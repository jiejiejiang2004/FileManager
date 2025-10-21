package org.jiejiejiang.filemanager.core;

//import org.jiejiejiang.filemanager.exception.*;
import org.jiejiejiang.filemanager.exception.*;
import org.jiejiejiang.filemanager.util.FileSizeUtil;
import org.jiejiejiang.filemanager.util.LogUtil;
import org.jiejiejiang.filemanager.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
//import java.nio.file.FileSystemException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统核心类：整合 Disk、FAT 实现文件/目录的完整操作
 * 支持创建/删除/读取/写入文件，创建/删除目录，列出目录内容等
 */
public class FileSystem {
    // ======================== 常量定义 ========================
    /** 根目录路径 */
    public static final String ROOT_PATH = "/";
    /** 根目录 Entry 名称（空字符串，避免路径重复） */
    private static final String ROOT_NAME = "";
    /** 目录项分隔符（存储目录项时使用，如 "test.txt|FILE|1"） */
    private static final String DIR_ENTRY_SEPARATOR = "|";
    /** 单个目录项最大长度（字节，适配块大小） */
    private static final int MAX_DIR_ENTRY_LENGTH = 128;

    // ======================== 核心依赖 ========================
    private final Disk disk;          // 底层磁盘
    private final FAT fat;            // 文件分配表（块管理）
    private final int blockSize;      // 磁盘块大小（从 Disk 同步）

    // ======================== 内存缓存（模拟目录项和元数据） ========================
    /** 缓存所有文件/目录的元数据（key：完整路径，value：FileEntry） */
    private final Map<String, FileEntry> entryCache;
    /** 根目录（文件系统初始化时创建） */
    private FileEntry rootDir;

    // ======================== 已打开文件表 ========================
    /** 已打开文件表（OFT），管理最多5个同时打开的文件 */
    private final OpenFileTable oft;

    // ======================== 状态标记 ========================
    private boolean isMounted;        // 文件系统是否已挂载（初始化完成）

    // ======================== 构造器 ========================
    /**
     * 初始化文件系统，关联磁盘和FAT
     * @param disk 已初始化的 Disk 实例
     * @param fat 已初始化的 FAT 实例
     * @throws FileSystemException 磁盘/FAT 未初始化时抛出
     */
    public FileSystem(Disk disk, FAT fat) throws FileSystemException {
        // 校验依赖是否初始化
        if (!disk.isInitialized()) {
            throw new FileSystemException("文件系统初始化失败：磁盘未初始化");
        }
        if (!fat.isInitialized()) {
            throw new FileSystemException("文件系统初始化失败：FAT 未初始化");
        }

        this.disk = disk;
        this.fat = fat;
        this.blockSize = disk.getBlockSize();
        this.entryCache = new ConcurrentHashMap<>(); // 支持多线程安全操作
        this.oft = new OpenFileTable(); // 初始化已打开文件表
        this.isMounted = false;
    }

    // ======================== 核心初始化（挂载文件系统） ========================
    /**
     * 挂载文件系统：初始化根目录，加载元数据（模拟从磁盘加载）
     * @throws FileSystemException 初始化失败时抛出
     */
    public void mount() throws FileSystemException {
        if (isMounted) {
            LogUtil.warn("文件系统已挂载，无需重复操作");
            return;
        }

        try {
            // 重要：在挂载前清空全局缓存，确保每次程序启动时缓存都是空的
            entryCache.clear();
            LogUtil.debug("文件系统挂载前已清空全局缓存");
            
            // 1. 创建根目录 Entry（父路径为自身，起始块ID=-1 表示空目录）
            rootDir = new FileEntry(ROOT_NAME, FileEntry.EntryType.DIRECTORY, ROOT_PATH, -1);
            // 2. 缓存根目录（完整路径为 "/"）
            entryCache.put(rootDir.getFullPath(), rootDir);
            // 3. 标记文件系统已挂载
            this.isMounted = true;
            LogUtil.info("文件系统挂载成功：块大小=" + blockSize + "字节，总块数=" + fat.getTotalBlocks());
        } catch (Exception e) {
            throw new FileSystemException("文件系统挂载失败：" + e.getMessage());
        }
    }

    /**
     * 卸载文件系统：清理缓存，关闭磁盘
     * @throws FileSystemException 卸载失败时抛出
     */
    public void unmount() throws FileSystemException {
        if (!isMounted) {
            LogUtil.warn("文件系统未挂载，无需卸载");
            return;
        }

        try {
            // 1. 关闭所有打开的文件
            oft.closeAllFiles();
            // 2. 持久化FAT表（可选，确保块分配状态不丢失）
            fat.saveToDisk();
            // 3. 关闭磁盘
            disk.close();
            // 4. 清理缓存，标记未挂载
            entryCache.clear();
            this.isMounted = false;
            LogUtil.info("文件系统卸载成功");
        } catch (Exception e) {
            throw new FileSystemException("文件系统卸载失败：" + e.getMessage());
        }
    }
    
    /**
     * 从缓存中移除指定的文件/目录条目
     * @param fullPath 文件/目录的完整路径
     */
    public void removeEntryFromCache(String fullPath) {
        if (fullPath != null) {
            entryCache.remove(fullPath);
            LogUtil.debug("从FileSystem缓存中移除条目：" + fullPath);
        }
    }

    // ======================== 已打开文件表（OFT）操作 ========================
    /**
     * 打开文件，加入OFT管理
     * @param fullPath 文件完整路径
     * @param mode 打开模式：READ, WRITE, READ_WRITE
     * @return OFT索引（0-4），如果OFT已满则返回-1
     * @throws FileSystemException 文件不存在、不是文件或OFT操作失败时抛出
     */
    public int openFile(String fullPath, String mode) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);
        
        // 1. 校验文件是否存在且为文件
        FileEntry file = getEntry(fullPath);
        if (file == null) {
            throw new FileSystemException("打开文件失败：文件不存在 → " + fullPath);
        }
        if (file.getType() != FileEntry.EntryType.FILE) {
            throw new FileSystemException("打开文件失败：" + fullPath + " 不是文件");
        }
        if (file.isDeleted()) {
            throw new FileSystemException("打开文件失败：文件已删除 → " + fullPath);
        }
        
        // 2. 验证打开模式
        if (!mode.equals("READ") && !mode.equals("WRITE") && !mode.equals("READ_WRITE")) {
            throw new FileSystemException("打开文件失败：无效的打开模式 → " + mode);
        }
        
        // 3. 将文件加入OFT
        int oftIndex = oft.openFile(fullPath, file, mode.toUpperCase());
        if (oftIndex == -1) {
            throw new FileSystemException("打开文件失败：OFT已满，无法打开更多文件");
        }
        
        LogUtil.info("文件已打开：" + fullPath + "，模式：" + mode + "，OFT索引：" + oftIndex);
        return oftIndex;
    }
    
    /**
     * 关闭文件，从OFT中移除
     * @param oftIndex OFT索引
     * @throws FileSystemException OFT操作失败时抛出
     */
    public void closeFile(int oftIndex) throws FileSystemException {
        checkMounted();
        oft.closeFile(oftIndex);
    }
    
    /**
     * 根据文件路径关闭文件
     * @param fullPath 文件完整路径
     * @throws FileSystemException 文件未打开或OFT操作失败时抛出
     */
    public void closeFile(String fullPath) throws FileSystemException {
        checkMounted();
        oft.closeFile(fullPath);
    }
    
    /**
     * 检查文件是否已打开
     * @param fullPath 文件完整路径
     * @return 如果文件已打开则返回OFT索引，否则返回-1
     */
    public int isFileOpen(String fullPath) {
        if (!isMounted) {
            return -1;
        }
        return oft.findOpenFile(fullPath);
    }
    
    /**
     * 获取OFT状态信息
     * @return OFT状态字符串
     */
    public String getOftStatus() {
        return oft.getStatus();
    }
    
    /**
     * 设置文件读指针位置
     * @param oftIndex OFT索引
     * @param position 新的读指针位置
     * @throws FileSystemException OFT操作失败时抛出
     */
    public void setReadPointer(int oftIndex, long position) throws FileSystemException {
        checkMounted();
        oft.setReadPointer(oftIndex, position);
    }
    
    /**
     * 设置文件写指针位置
     * @param oftIndex OFT索引
     * @param position 新的写指针位置
     * @throws FileSystemException OFT操作失败时抛出
     */
    public void setWritePointer(int oftIndex, long position) throws FileSystemException {
        checkMounted();
        oft.setWritePointer(oftIndex, position);
    }

    // ======================== 文件操作：创建/删除/读取/写入 ========================
    /**
     * 创建文件
     * @param fullPath 文件完整路径（如 "/home/test.txt"）
     * @return 创建成功的 FileEntry
     * @throws FileSystemException 路径非法、文件已存在、磁盘满时抛出
     */
    /**
     * 创建文件（创建元数据 + 分配块）
     * @param fullPath 文件完整路径
     * @return 创建的文件元数据
     * @throws FileSystemException 路径无效、父目录不存在、文件已存在时抛出
     */
    public FileEntry createFile(String fullPath) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);
        LogUtil.debug("尝试创建文件：" + fullPath);

        // 1. 解析路径：拆分父目录路径和文件名
        String parentPath = PathUtil.getParentPath(fullPath);
        String fileName = PathUtil.getFileNameFromPath(fullPath);
        LogUtil.debug("解析路径 - 父目录：" + parentPath + ", 文件名：" + fileName);

        // 2. 校验父目录是否存在且为目录
        FileEntry parentDir = getEntry(parentPath);
        if (parentDir == null) {
            LogUtil.error("创建文件失败：父目录不存在 → " + parentPath);
            throw new FileSystemException("创建文件失败：父目录不存在 → " + parentPath);
        }
        if (parentDir.getType() != FileEntry.EntryType.DIRECTORY) {
            LogUtil.error("创建文件失败：" + parentPath + " 不是目录");
            throw new FileSystemException("创建文件失败：" + parentPath + " 不是目录");
        }

        // 3. 校验文件是否已存在（考虑已删除的文件）
        FileEntry existingEntry = entryCache.get(fullPath);
        if (existingEntry != null) {
            LogUtil.debug("文件已在缓存中存在，检查删除状态：" + existingEntry.isDeleted());
        }
        if (existingEntry != null && !existingEntry.isDeleted()) {
            LogUtil.error("创建文件失败：文件已存在且未删除 → " + fullPath);
            throw new FileSystemException("创建文件失败：文件已存在 → " + fullPath);
        }
        // 如果文件已被标记为删除，则从缓存中移除它，允许重新创建
        if (existingEntry != null && existingEntry.isDeleted()) {
            LogUtil.debug("文件已被标记为删除，从缓存中移除：" + fullPath);
            entryCache.remove(fullPath);
            
            // 重要修复：同时从父目录的entriesCache中移除已删除的文件
            try {
                Directory parentDirectory = getDirectory(parentPath);
                if (parentDirectory != null) {
                    // 从父目录的缓存中查找并移除已删除的文件
                    FileEntry entryToRemove = null;
                    for (FileEntry entry : parentDirectory.getEntries()) {
                        if (entry.getName().equals(fileName) && entry.isDeleted()) {
                            entryToRemove = entry;
                            break;
                        }
                    }
                    if (entryToRemove != null) {
                        parentDirectory.getEntries().remove(entryToRemove);
                        LogUtil.debug("从父目录缓存中移除已删除的文件：" + fileName);
                    }
                }
            } catch (FileSystemException e) {
                LogUtil.warn("清理父目录缓存失败，但继续创建文件：" + e.getMessage());
            }
        }

        try {
            // 4. 分配文件的起始块（FAT 分配空闲块）
            int startBlockId = fat.allocateBlock();
            LogUtil.debug("为文件分配起始块：" + startBlockId);
            // 5. 创建文件元数据
            FileEntry newFile = new FileEntry(fileName, FileEntry.EntryType.FILE, parentPath, startBlockId);
            LogUtil.debug("创建文件元数据：" + newFile);
            // 6. 缓存文件元数据
            entryCache.put(fullPath, newFile);
            LogUtil.debug("文件元数据已缓存：" + fullPath);
            // 7. 更新父目录的修改时间
            updateDirModifyTime(parentDir);
            LogUtil.debug("父目录修改时间已更新：" + parentPath);
            
            // 8. 将新文件添加到父目录并同步到磁盘
            Directory parentDirectory = new Directory(this, parentDir);
            LogUtil.debug("创建父目录对象：" + parentPath);
            parentDirectory.addEntry(newFile);
            LogUtil.debug("文件已添加到父目录：" + fileName + " → " + parentPath);
            parentDirectory.syncToDisk();
            LogUtil.debug("父目录已同步到磁盘：" + parentPath);

            LogUtil.info("创建文件成功：" + newFile);
            return newFile;
        } catch (DiskFullException e) {
            LogUtil.error("创建文件失败：磁盘空间不足", e);
            throw new FileSystemException("创建文件失败：磁盘空间不足");
        } catch (InvalidBlockIdException e) {
            LogUtil.error("创建文件失败：块分配异常", e);
            throw new FileSystemException("创建文件失败：块分配异常");
        } catch (Exception e) {
            LogUtil.error("创建文件失败：未知异常", e);
            throw new FileSystemException("创建文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除文件（标记删除 + 释放FAT块）
     * @param fullPath 文件完整路径
     * @throws FileSystemException 文件不存在、不是文件时抛出
     */
    public void deleteFile(String fullPath) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);

        // 1. 校验文件是否存在且为文件
        FileEntry file = getEntry(fullPath);
        if (file == null) {
            throw new FileSystemException("删除文件失败：文件不存在 → " + fullPath);
        }
        if (file.getType() != FileEntry.EntryType.FILE) {
            throw new FileSystemException("删除文件失败：" + fullPath + " 不是文件");
        }
        if (file.isDeleted()) {
            LogUtil.warn("删除文件失败：文件已删除 → " + fullPath);
            return;
        }

        try {
            // 2. 释放文件占用的所有块（FAT 释放块链）
            fat.freeBlocks(file.getStartBlockId());
            // 3. 标记文件为已删除
            file.markAsDeleted();
            // 4. 更新父目录的修改时间
            FileEntry parentDir = getEntry(file.getParentPath());
            updateDirModifyTime(parentDir);
            // 5. 从entryCache中完全移除已删除的文件，确保后续创建同名文件时不会受到影响
            entryCache.remove(fullPath);

            LogUtil.info("删除文件成功：" + fullPath);
        } catch (InvalidBlockIdException e) {
            throw new FileSystemException("删除文件失败：块释放异常");
        }
    }

    /**
     * 写入文件内容（覆盖写入，自动扩展块）
     * @param fullPath 文件完整路径
     * @param content 待写入的字节内容
     * @throws FileSystemException 文件不存在、写入磁盘失败时抛出
     */
    public void writeFile(String fullPath, byte[] content) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);
        if (content == null) {
            content = new byte[0];
        }

        FileEntry file = getEntry(fullPath);
        if (file == null) {
            // 抛出自定义的FileSystemException，而非JDK自带的
            throw new FileSystemException("写入文件失败：文件不存在 → " + fullPath);
        }
        if (file.getType() != FileEntry.EntryType.FILE) {
            throw new FileSystemException("写入文件失败：" + fullPath + " 不是文件");
        }
        if (file.isDeleted()) {
            // 这是测试用例触发的场景，确保抛出自定义异常
            throw new FileSystemException("写入文件失败：文件已删除 → " + fullPath);
        }

        try {
            // 2. 计算需要的块数（向上取整，如 1025字节 / 512字节/块 = 3块）
            int requiredBlocks = (content.length + blockSize - 1) / blockSize;
            // 3. 计算当前文件占用的块数
            int currentBlocks = fat.getFileBlockCount(file.getStartBlockId());

            // 4. 扩展块（若需要的块数大于当前块数）
            if (requiredBlocks > currentBlocks) {
                int currentBlockId = file.getStartBlockId();
                // 遍历到当前最后一块
                while (fat.getNextBlock(currentBlockId) != FAT.END_OF_FILE) {
                    currentBlockId = fat.getNextBlock(currentBlockId);
                }
                // 分配剩余需要的块
                for (int i = 0; i < requiredBlocks - currentBlocks; i++) {
                    currentBlockId = fat.allocateNextBlock(currentBlockId);
                }
            }

            // 5. 分块写入磁盘（每块写入 blockSize 字节）
            int offset = 0; // 内容偏移量
            int currentBlockId = file.getStartBlockId();
            while (offset < content.length) {
                // 计算当前块需要写入的字节数（最后一块可能不足 blockSize）
                int writeLength = Math.min(blockSize, content.length - offset);
                // 截取当前块的内容（不足 blockSize 时自动补0）
                byte[] blockContent = new byte[blockSize];
                System.arraycopy(content, offset, blockContent, 0, writeLength);
                // 写入磁盘块
                disk.writeBlock(currentBlockId, blockContent);

                // 更新偏移量和下一块
                offset += writeLength;
                int nextBlockId = fat.getNextBlock(currentBlockId);
                if (nextBlockId == FAT.END_OF_FILE && offset < content.length) {
                    throw new FileSystemException("写入文件失败：块链中断，无法继续写入");
                }
                currentBlockId = nextBlockId;
            }

            // 6. 更新文件大小和修改时间
            file.updateSize(content.length);
            LogUtil.info("写入文件成功：" + fullPath + "，大小：" + file.getFormattedSize());
        } catch (DiskFullException e) {
            throw new FileSystemException("写入文件失败：磁盘空间不足");
        } catch (InvalidBlockIdException | DiskWriteException e) {
            throw new FileSystemException("写入文件失败：磁盘操作异常");
        }
    }

    /**
     * 读取文件内容
     * @param fullPath 文件完整路径
     * @return 文件内容（字节数组）
     * @throws FileSystemException 文件不存在、读取磁盘失败时抛出
     */
    public byte[] readFile(String fullPath) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);

        // 1. 校验文件是否存在且为文件
        FileEntry file = getEntry(fullPath);
        if (file == null) {
            throw new FileSystemException("读取文件失败：文件不存在 → " + fullPath);
        }
        if (file.getType() != FileEntry.EntryType.FILE) {
            throw new FileSystemException("读取文件失败：" + fullPath + " 不是文件");
        }
        if (file.isDeleted()) {
            throw new FileSystemException("读取文件失败：文件已删除 → " + fullPath);
        }
        if (file.getSize() == 0) {
            return new byte[0]; // 空文件返回空字节数组
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 2. 遍历块链，读取所有块内容
            int currentBlockId = file.getStartBlockId();
            while (true) {
                // 读取当前块的字节内容
                byte[] blockContent = disk.readBlock(currentBlockId);
                // 写入输出流（注意：最后一块可能有多余的0，需按文件实际大小截取）
                if (outputStream.size() + blockSize >= file.getSize()) {
                    // 最后一块：仅写入剩余需要的字节
                    int remaining = (int) (file.getSize() - outputStream.size());
                    outputStream.write(blockContent, 0, remaining);
                    break;
                } else {
                    // 非最后一块：写入完整块
                    outputStream.write(blockContent);
                }

                // 移动到下一块，若为文件结束则终止
                int nextBlockId = fat.getNextBlock(currentBlockId);
                if (nextBlockId == FAT.END_OF_FILE) {
                    break;
                }
                currentBlockId = nextBlockId;
            }

            byte[] content = outputStream.toByteArray();
            LogUtil.info("读取文件成功：" + fullPath + "，大小：" + FileSizeUtil.format(content.length));
            return content;
        } catch (InvalidBlockIdException e) {
            throw new FileSystemException("读取文件失败：块查询异常");
        } catch (IOException e) {
            throw new FileSystemException("读取文件失败：流操作异常");
        }
    }

    /**
     * 基于OFT读取文件内容（从当前读指针位置开始）
     * @param oftIndex OFT索引
     * @param length 要读取的字节数，-1表示读取到文件末尾
     * @return 读取的内容（字节数组）
     * @throws FileSystemException OFT操作失败或读取失败时抛出
     */
    public byte[] readFileFromOft(int oftIndex, int length) throws FileSystemException {
        checkMounted();
        
        // 1. 获取OFT条目
        OpenFileTable.OpenFileEntry oftEntry = oft.getOpenFileEntry(oftIndex);
        if (oftEntry == null || !oftEntry.isActive()) {
            throw new FileSystemException("读取文件失败：OFT索引 " + oftIndex + " 处无活跃文件");
        }
        
        // 2. 检查读权限
        String mode = oftEntry.getMode();
        if (!mode.equals("READ") && !mode.equals("READ_WRITE")) {
            throw new FileSystemException("读取文件失败：文件未以读模式打开");
        }
        
        FileEntry file = oftEntry.getFileEntry();
        long readPointer = oftEntry.getReadPointer();
        
        // 3. 检查读指针位置
        if (readPointer >= file.getSize()) {
            return new byte[0]; // 已到文件末尾
        }
        
        // 4. 计算实际读取长度
        long remainingBytes = file.getSize() - readPointer;
        int actualLength = (length == -1) ? (int) remainingBytes : Math.min(length, (int) remainingBytes);
        
        if (actualLength <= 0) {
            return new byte[0];
        }
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 5. 计算起始块和块内偏移
            int startBlockIndex = (int) (readPointer / blockSize);
            int blockOffset = (int) (readPointer % blockSize);
            int bytesRead = 0;
            
            // 6. 遍历块链找到起始块
            int currentBlockId = file.getStartBlockId();
            for (int i = 0; i < startBlockIndex; i++) {
                currentBlockId = fat.getNextBlock(currentBlockId);
                if (currentBlockId == FAT.END_OF_FILE) {
                    throw new FileSystemException("读取文件失败：块链中断");
                }
            }
            
            // 7. 从起始块开始读取
            while (bytesRead < actualLength && currentBlockId != FAT.END_OF_FILE) {
                byte[] blockContent = disk.readBlock(currentBlockId);
                
                // 计算当前块要读取的字节数
                int startPos = (bytesRead == 0) ? blockOffset : 0;
                int endPos = Math.min(blockSize, startPos + (actualLength - bytesRead));
                int blockReadLength = endPos - startPos;
                
                outputStream.write(blockContent, startPos, blockReadLength);
                bytesRead += blockReadLength;
                
                // 移动到下一块
                if (bytesRead < actualLength) {
                    currentBlockId = fat.getNextBlock(currentBlockId);
                }
            }
            
            // 8. 更新读指针
            oft.setReadPointer(oftIndex, readPointer + bytesRead);
            
            byte[] result = outputStream.toByteArray();
            LogUtil.debug("从OFT读取文件：" + oftEntry.getFilePath() + "，读取字节数：" + bytesRead + "，新读指针：" + (readPointer + bytesRead));
            return result;
            
        } catch (InvalidBlockIdException e) {
            throw new FileSystemException("读取文件失败：块查询异常");
        } catch (IOException e) {
            throw new FileSystemException("读取文件失败：流操作异常");
        }
    }
    
    /**
     * 基于OFT写入文件内容（从当前写指针位置开始）
     * @param oftIndex OFT索引
     * @param content 要写入的内容
     * @throws FileSystemException OFT操作失败或写入失败时抛出
     */
    public void writeFileToOft(int oftIndex, byte[] content) throws FileSystemException {
        checkMounted();
        
        if (content == null) {
            content = new byte[0];
        }
        
        // 1. 获取OFT条目
        OpenFileTable.OpenFileEntry oftEntry = oft.getOpenFileEntry(oftIndex);
        if (oftEntry == null || !oftEntry.isActive()) {
            throw new FileSystemException("写入文件失败：OFT索引 " + oftIndex + " 处无活跃文件");
        }
        
        // 2. 检查写权限
        String mode = oftEntry.getMode();
        if (!mode.equals("WRITE") && !mode.equals("READ_WRITE")) {
            throw new FileSystemException("写入文件失败：文件未以写模式打开");
        }
        
        FileEntry file = oftEntry.getFileEntry();
        long writePointer = oftEntry.getWritePointer();
        
        if (content.length == 0) {
            return; // 无内容写入
        }
        
        try {
            // 3. 计算写入后的文件大小
            long newSize = Math.max(file.getSize(), writePointer + content.length);
            
            // 4. 检查是否需要扩展文件
            int currentBlocks = (int) Math.ceil((double) file.getSize() / blockSize);
            int requiredBlocks = (int) Math.ceil((double) newSize / blockSize);
            
            // 5. 扩展块（若需要）
            if (requiredBlocks > currentBlocks) {
                int currentBlockId = file.getStartBlockId();
                // 遍历到当前最后一块
                while (fat.getNextBlock(currentBlockId) != FAT.END_OF_FILE) {
                    currentBlockId = fat.getNextBlock(currentBlockId);
                }
                // 分配剩余需要的块
                for (int i = 0; i < requiredBlocks - currentBlocks; i++) {
                    currentBlockId = fat.allocateNextBlock(currentBlockId);
                }
            }
            
            // 6. 计算起始块和块内偏移
            int startBlockIndex = (int) (writePointer / blockSize);
            int blockOffset = (int) (writePointer % blockSize);
            int bytesWritten = 0;
            
            // 7. 遍历块链找到起始块
            int currentBlockId = file.getStartBlockId();
            for (int i = 0; i < startBlockIndex; i++) {
                currentBlockId = fat.getNextBlock(currentBlockId);
                if (currentBlockId == FAT.END_OF_FILE) {
                    throw new FileSystemException("写入文件失败：块链中断");
                }
            }
            
            // 8. 从起始块开始写入
            while (bytesWritten < content.length && currentBlockId != FAT.END_OF_FILE) {
                // 读取当前块内容（用于部分写入）
                byte[] blockContent = disk.readBlock(currentBlockId);
                
                // 计算当前块要写入的字节数
                int startPos = (bytesWritten == 0) ? blockOffset : 0;
                int writeLength = Math.min(blockSize - startPos, content.length - bytesWritten);
                
                // 将新内容复制到块中
                System.arraycopy(content, bytesWritten, blockContent, startPos, writeLength);
                
                // 写回磁盘
                disk.writeBlock(currentBlockId, blockContent);
                
                bytesWritten += writeLength;
                
                // 移动到下一块
                if (bytesWritten < content.length) {
                    currentBlockId = fat.getNextBlock(currentBlockId);
                }
            }
            
            // 9. 更新文件大小和写指针
            if (newSize > file.getSize()) {
                file.updateSize(newSize);
            }
            oft.setWritePointer(oftIndex, writePointer + bytesWritten);
            
            LogUtil.debug("向OFT写入文件：" + oftEntry.getFilePath() + "，写入字节数：" + bytesWritten + "，新写指针：" + (writePointer + bytesWritten));
            
        } catch (DiskFullException e) {
            throw new FileSystemException("写入文件失败：磁盘空间不足");
        } catch (InvalidBlockIdException | DiskWriteException e) {
            throw new FileSystemException("写入文件失败：磁盘操作异常");
        }
    }

    /**
     * 修改文件大小
     * @param fullPath 文件完整路径
     * @param newSize 新的文件大小（字节）
     * @throws FileSystemException 文件不存在、不是文件、大小无效、操作失败时抛出
     */
    public void resizeFile(String fullPath, long newSize) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);

        // 1. 检查文件是否存在且为文件
        FileEntry file = getEntry(fullPath);
        if (file == null) {
            throw new FileSystemException("修改文件大小失败：文件不存在 → " + fullPath);
        }
        if (file.getType() != FileEntry.EntryType.FILE) {
            throw new FileSystemException("修改文件大小失败：" + fullPath + " 不是文件");
        }
        if (file.isDeleted()) {
            throw new FileSystemException("修改文件大小失败：文件已删除 → " + fullPath);
        }
        
        // 2. 验证新大小是否有效
        if (newSize < 0) {
            throw new FileSystemException("修改文件大小失败：文件大小不能为负数");
        }
        
        // 3. 如果新大小与原大小相同，直接返回
        if (newSize == file.getSize()) {
            return;
        }
        
        try {
            // 4. 计算新旧大小需要的块数
            int currentBlocks = fat.getFileBlockCount(file.getStartBlockId());
            int newBlocks = (int) Math.ceil((double) newSize / blockSize);
            
            // 5. 如果新大小需要更多的块，扩展块链
            if (newBlocks > currentBlocks) {
                int currentBlockId = file.getStartBlockId();
                // 遍历到当前最后一块
                while (fat.getNextBlock(currentBlockId) != FAT.END_OF_FILE) {
                    currentBlockId = fat.getNextBlock(currentBlockId);
                }
                // 分配剩余需要的块
                for (int i = 0; i < newBlocks - currentBlocks; i++) {
                    currentBlockId = fat.allocateNextBlock(currentBlockId);
                }
            }
            
            // 6. 如果新大小需要更少的块，截断块链（但不释放块，避免性能问题）
            // 注意：这里只是更新文件大小，不实际释放磁盘块，实际项目中可能需要考虑释放块
            
            // 7. 更新文件大小和修改时间
            file.updateSize(newSize);
            
            // 8. 更新父目录的修改时间
            FileEntry parentDir = getEntry(file.getParentPath());
            updateDirModifyTime(parentDir);
            
            // 9. 同步更新父目录缓存中的文件大小
            if (parentDir != null) {
                Directory parentDirectory = getDirectory(file.getParentPath());
                if (parentDirectory != null) {
                    // 关键修复：更新父目录缓存中的FileEntry对象
                    List<FileEntry> entriesCache = parentDirectory.getEntries();
                    // 创建一个新的List，避免在遍历时修改原List
                    List<FileEntry> updatedEntries = new ArrayList<>(entriesCache);
                    // 查找并替换缓存中的FileEntry对象
                    for (int i = 0; i < updatedEntries.size(); i++) {
                        FileEntry cachedEntry = updatedEntries.get(i);
                        if (cachedEntry.getName().equals(file.getName()) && cachedEntry.getType() == FileEntry.EntryType.FILE) {
                            // 创建一个新的FileEntry对象，确保使用最新的大小
                            FileEntry updatedEntry = new FileEntry(
                                file.getName(),
                                file.getType(),
                                file.getParentPath(),
                                file.getStartBlockId()
                            );
                            // 设置正确的大小
                            updatedEntry.setSize(file.getSize());
                            // 如果原文件已删除，也要更新这个状态
                            if (file.isDeleted()) {
                                updatedEntry.markAsDeleted();
                            }
                            // 替换缓存中的对象
                            updatedEntries.set(i, updatedEntry);
                            break;
                        }
                    }
                    
                    // 使用反射更新parentDirectory的entriesCache字段
                    try {
                        var field = Directory.class.getDeclaredField("entriesCache");
                        field.setAccessible(true);
                        field.set(parentDirectory, updatedEntries);
                        // 标记为脏，确保会同步到磁盘
                        var dirtyField = Directory.class.getDeclaredField("isDirty");
                        dirtyField.setAccessible(true);
                        dirtyField.setBoolean(parentDirectory, true);
                    } catch (Exception e) {
                        LogUtil.error("更新父目录缓存失败", e);
                    }
                    
                    // 直接同步父目录到磁盘，确保文件大小更新被持久化
                    parentDirectory.syncToDisk();
                    // 同步后再刷新，确保内存中的数据是最新的
                    parentDirectory.refreshEntries();
                }
            }
            
            LogUtil.info("修改文件大小成功：" + fullPath + "，新大小：" + FileSizeUtil.format(newSize));
        } catch (DiskFullException e) {
            throw new FileSystemException("修改文件大小失败：磁盘空间不足");
        } catch (InvalidBlockIdException e) {
            throw new FileSystemException("修改文件大小失败：块查询异常");
        }
    }

    /**
     * 复制文件
     * @param sourcePath 源文件完整路径
     * @param targetPath 目标文件完整路径
     * @throws FileSystemException 文件不存在、路径无效、复制失败时抛出
     */
    public void copyFile(String sourcePath, String targetPath) throws FileSystemException {
        checkMounted();
        validateFullPath(sourcePath);
        validateFullPath(targetPath);

        // 1. 检查源文件是否存在且为文件
        FileEntry sourceFile = getEntry(sourcePath);
        if (sourceFile == null) {
            throw new FileSystemException("复制文件失败：源文件不存在 → " + sourcePath);
        }
        if (sourceFile.getType() != FileEntry.EntryType.FILE) {
            throw new FileSystemException("复制文件失败：" + sourcePath + " 不是文件");
        }
        if (sourceFile.isDeleted()) {
            throw new FileSystemException("复制文件失败：源文件已删除 → " + sourcePath);
        }

        // 2. 检查目标路径是否合法（不能与源文件路径相同）
        if (sourcePath.equals(targetPath)) {
            throw new FileSystemException("复制文件失败：源文件和目标文件路径不能相同");
        }

        // 3. 解析目标路径：拆分父目录路径和文件名
        String targetParentPath = PathUtil.getParentPath(targetPath);
        String targetFileName = PathUtil.getFileNameFromPath(targetPath);
        
        // 4. 检查目标父目录是否存在且为目录
        FileEntry targetParentDir = getEntry(targetParentPath);
        if (targetParentDir == null) {
            throw new FileSystemException("复制文件失败：目标父目录不存在 → " + targetParentPath);
        }
        if (targetParentDir.getType() != FileEntry.EntryType.DIRECTORY) {
            throw new FileSystemException("复制文件失败：" + targetParentPath + " 不是目录");
        }
        
        // 5. 检查目标文件是否已存在
        FileEntry existingFile = getEntry(targetPath);
        if (existingFile != null) {
            throw new FileSystemException("复制文件失败：目标文件已存在 → " + targetPath);
        }
        
        try {
            // 6. 读取源文件内容
            byte[] fileContent = readFile(sourcePath);
            
            // 7. 在目标路径创建新文件
            FileEntry newFile = createFile(targetPath);
            
            // 8. 将源文件内容写入目标文件
            writeFile(targetPath, fileContent);
            
            LogUtil.info("复制文件成功：" + sourcePath + " → " + targetPath);
        } catch (Exception e) {
            throw new FileSystemException("复制文件失败：" + e.getMessage(), e);
        }
    }

    // ======================== 目录操作：创建/删除/列出内容 ========================
    /**
     * 创建目录
     * @param fullPath 目录完整路径（如 "/home/docs"）
     * @return 创建成功的目录 FileEntry
     * @throws FileSystemException 路径非法、父目录不存在、目录已存在时抛出
     */
    public FileEntry createDirectory(String fullPath) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);

        // 1. 解析路径：拆分父目录路径和目录名
        String parentPath = PathUtil.getParentPath(fullPath);
        String dirName = PathUtil.getFileNameFromPath(fullPath);

        // 2. 校验父目录是否存在且为目录（根目录无需校验父目录）
        if (!fullPath.equals(ROOT_PATH)) {
            FileEntry parentDir = getEntry(parentPath);
            if (parentDir == null) {
                throw new FileSystemException("创建目录失败：父目录不存在 → " + parentPath);
            }
            if (parentDir.getType() != FileEntry.EntryType.DIRECTORY) {
                throw new FileSystemException("创建目录失败：" + parentPath + " 不是目录");
            }
        }

        // 3. 校验目录是否已存在
        if (entryCache.containsKey(fullPath)) {
            throw new FileSystemException("创建目录失败：目录已存在 → " + fullPath);
        }

        // 4. 创建目录元数据（目录初始无块，startBlockId=-1）
        FileEntry newDir = new FileEntry(dirName, FileEntry.EntryType.DIRECTORY, parentPath, -1);
        // 5. 缓存目录元数据
        entryCache.put(fullPath, newDir);
        // 6. 若不是根目录，更新父目录的修改时间并将新目录添加到父目录
        if (!fullPath.equals(ROOT_PATH)) {
            FileEntry parentDir = getEntry(parentPath);
            updateDirModifyTime(parentDir);
            
            // 将新目录添加到父目录并同步到磁盘
            Directory parentDirectory = new Directory(this, parentDir);
            parentDirectory.addEntry(newDir);
            parentDirectory.syncToDisk();
        }

        LogUtil.info("创建目录成功：" + newDir);
        return newDir;
    }

    /**
     * 删除目录（仅支持空目录删除）
     * @param fullPath 目录完整路径
     * @throws FileSystemException 目录不存在、非空目录、不是目录时抛出
     */
    public void deleteDirectory(String fullPath) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);

        // 1. 禁止删除根目录
        if (fullPath.equals(ROOT_PATH)) {
            throw new FileSystemException("删除目录失败：根目录不允许删除");
        }

        // 2. 校验目录是否存在且为目录
        FileEntry dir = getEntry(fullPath);
        if (dir == null) {
            throw new FileSystemException("删除目录失败：目录不存在 → " + fullPath);
        }
        if (dir.getType() != FileEntry.EntryType.DIRECTORY) {
            throw new FileSystemException("删除目录失败：" + fullPath + " 不是目录");
        }
        if (dir.isDeleted()) {
            LogUtil.warn("删除目录失败：目录已删除 → " + fullPath);
            return;
        }

        // 3. 校验目录是否为空（遍历缓存，判断是否有子条目）
        boolean isEmpty = entryCache.keySet().stream()
                .filter(path -> !path.equals(fullPath)) // 排除目录自身
                .noneMatch(path -> path.startsWith(fullPath + "/")); // 无子条目
        if (!isEmpty) {
            throw new FileSystemException("删除目录失败：目录非空 → " + fullPath);
        }

        // 4. 标记目录为已删除
        dir.markAsDeleted();
        // 5. 更新父目录的修改时间
        FileEntry parentDir = getEntry(dir.getParentPath());
        updateDirModifyTime(parentDir);

        LogUtil.info("删除目录成功：" + fullPath);
    }

    /**
     * 列出目录下的所有文件/目录
     * @param fullPath 目录完整路径
     * @return 目录下的 FileEntry 列表（空列表表示目录为空）
     * @throws FileSystemException 目录不存在、不是目录时抛出
     */
    public List<FileEntry> listDirectory(String fullPath) throws FileSystemException {
        checkMounted();
        validateFullPath(fullPath);

        // 1. 校验目录是否存在且为目录
        FileEntry dir = getEntry(fullPath);
        if (dir == null) {
            throw new FileSystemException("列出目录失败：目录不存在 → " + fullPath);
        }
        if (dir.getType() != FileEntry.EntryType.DIRECTORY) {
            throw new FileSystemException("列出目录失败：" + fullPath + " 不是目录");
        }

        // 2. 遍历缓存，筛选当前目录的子条目（路径匹配：父目录路径 + "/" + 子条目名称）
        List<FileEntry> children = new ArrayList<>();
        String dirPrefix = fullPath.equals(ROOT_PATH) ? ROOT_PATH : fullPath + "/";
        for (Map.Entry<String, FileEntry> entry : entryCache.entrySet()) {
            String childPath = entry.getKey();
            FileEntry childEntry = entry.getValue();
            // 条件：子条目路径以目录前缀开头，且不是目录自身，且未删除
            if (childPath.startsWith(dirPrefix)
                    && !childPath.equals(fullPath)
                    && !childEntry.isDeleted()) {
                // 排除子目录的子条目（仅保留直接子条目）
                String childParentPath = childEntry.getParentPath();
                if (childParentPath.equals(fullPath)) {
                    children.add(childEntry);
                }
            }
        }

        LogUtil.info("列出目录成功：" + fullPath + "，子条目数：" + children.size());
        return children;
    }

    // ======================== 辅助方法 ========================
    /**
     * 校验文件系统是否已挂载
     * @throws FileSystemException 未挂载时抛出
     */
    private void checkMounted() throws FileSystemException {
        if (!isMounted) {
            throw new FileSystemException("文件系统未挂载，请先调用 mount()");
        }
    }

    /**
     * 校验完整路径是否合法（非空、符合路径格式）
     * @param fullPath 完整路径
     * @throws FileSystemException 路径非法时抛出
     */
    private void validateFullPath(String fullPath) throws FileSystemException {
        if (fullPath == null || fullPath.trim().isEmpty()) {
            throw new FileSystemException("路径不能为空");
        }
        try {
            // 调用PathUtil的标准化方法，触发非法字符校验
            PathUtil.normalizePath(fullPath);
        } catch (InvalidPathException e) {
            throw new FileSystemException("非法路径：" + fullPath + "，原因：" + e.getMessage(), e);
        }
    }

    /**
     * 根据完整路径获取 FileEntry（从缓存中读取）
     * @param fullPath 完整路径
     * @return FileEntry 或 null（不存在或已删除）
     */
    public FileEntry getEntry(String fullPath) {
        if (!isMounted || fullPath == null) {
            return null;
        }
        FileEntry entry = entryCache.get(fullPath);
        // 返回未删除的条目
        return (entry != null && !entry.isDeleted()) ? entry : null;
    }

    /**
     * 更新目录的修改时间
     * @param dir 目录 FileEntry
     */
    private void updateDirModifyTime(FileEntry dir) {
        if (dir != null && dir.getType() == FileEntry.EntryType.DIRECTORY) {
            // 通过反射更新 modifyTime（因 modifyTime 无 Setter，实际项目建议添加 Setter）
            try {
                var field = FileEntry.class.getDeclaredField("modifyTime");
                field.setAccessible(true);
                field.set(dir, new Date());
            } catch (Exception e) {
                LogUtil.error("更新目录修改时间失败", e);
            }
        }
    }

    // ======================== Getter 方法 ========================
    public boolean isMounted() {
        return isMounted;
    }

    public Disk getDisk() {
        return disk;
    }

    public FAT getFat() {
        return fat;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public FileEntry getRootDir() {
        return rootDir;
    }

    // FileSystem.java
    /**
     * 根据获取指定路径对应的目录管理器
     * @param path 目录路径
     * @return 目录管理器
     * @throws FileSystemException 路径不存在或不是目录时抛出
     */
    public Directory getDirectory(String path) throws FileSystemException {
        checkMounted();
        validateFullPath(path);

        FileEntry entry = getEntry(path);
        if (entry == null) {
            throw new FileSystemException("目录不存在：" + path);
        }
        if (entry.getType() != FileEntry.EntryType.DIRECTORY) {
            throw new FileSystemException(path + " 不是目录");
        }

        return new Directory(this, entry);
    }
}