package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.*;
import org.jiejiejiang.filemanager.util.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 目录内容管理器
 * 负责管理目录下的子文件/子目录（目录项），处理目录项的持久化存储和读取
 */
public class Directory {
    // 目录项存储格式常量
    private static final String ENTRY_SEPARATOR = "|";  // 字段分隔符
    private static final String ENTRY_TERMINATOR = "\n"; // 条目结束符
    private static final int MAX_ENTRY_NAME_LENGTH = 64; // 最大名称长度（含扩展名）
    private static final int MAX_ENTRIES_PER_BLOCK = 8;  // 每块最多存储的目录项数（基于块大小计算）

    private final FileSystem fileSystem;  // 关联的文件系统
    private final FileEntry dirEntry;     // 目录自身的元数据
    private final Disk disk;              // 磁盘引用（用于读写块）
    private final FAT fat;                // FAT引用（用于块管理）
    private final int blockSize;          // 块大小

    // 内存缓存的目录项（避免频繁读写磁盘）
    private List<FileEntry> entriesCache;

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    private boolean isDirty;              // 缓存是否已修改（需要同步到磁盘）



    /**
     * 初始化目录管理器
     * @param fileSystem 关联的文件系统
     * @param dirEntry 目录自身的元数据
     */
    public Directory(FileSystem fileSystem, FileEntry dirEntry) {
        // 验证入参不为null
        if (fileSystem == null) {
            throw new IllegalArgumentException("FileSystem不能为null");
        }
        if (dirEntry == null) {
            throw new IllegalArgumentException("目录元数据不能为null");
        }
        if (dirEntry.getType() != FileEntry.EntryType.DIRECTORY) {
            throw new IllegalArgumentException("dirEntry必须是目录类型");
        }

        this.fileSystem = fileSystem;
        this.dirEntry = dirEntry;
        this.disk = fileSystem.getDisk();
        this.fat = fileSystem.getFat();
        this.blockSize = fileSystem.getBlockSize();
        this.entriesCache = new ArrayList<>();
        this.isDirty = false;

        // 验证依赖获取成功
        if (this.fat == null) {
            throw new IllegalStateException("无法从FileSystem获取FAT实例");
        }
        if (this.disk == null) {
            throw new IllegalStateException("无法从FileSystem获取Disk实例");
        }
        if (this.blockSize <= 0) {
            throw new IllegalStateException("无效的块大小: " + this.blockSize);
        }

        // 从磁盘加载目录项（如果已分配块）
        if (dirEntry.getStartBlockId() != -1) {
            loadEntriesFromDisk();
        }
    }

    // ======================== 目录项管理核心方法 ========================

    /* 添加子目录项（文件或目录）
            * @param entry 要添加的子项
     * @throws FileSystemException 名称过长、已存在同名项或磁盘操作失败时抛出
     */
    public void addEntry(FileEntry entry) throws FileSystemException {
        validateEntry(entry);

        // 检查是否已存在同名项（检查本地缓存）
        if (findEntryByName(entry.getName()) != null) {
            throw new FileSystemException("目录项已存在：" + entry.getName() + "（目录：" + dirEntry.getFullPath() + "）");
        }

        // 添加到缓存并标记为脏
        entriesCache.add(entry);
        isDirty = true;

        // 移除自动同步，由上层调用者决定何时同步到磁盘
        // syncToDisk();  // 注释掉自动同步

        LogUtil.debug("目录添加项成功：" + entry.getName() + " → " + dirEntry.getFullPath());
    }

    /**
     * 删除子目录项
     * @param entryName 要删除的子项名称
     * @return 被删除的子项，不存在则返回null
     * @throws FileSystemException 磁盘操作失败时抛出
     */
    public FileEntry removeEntry(String entryName) throws FileSystemException {
        LogUtil.debug("尝试删除目录项：" + entryName + "，目录：" + dirEntry.getFullPath());
        
        FileEntry toRemove = findEntryByName(entryName);
        if (toRemove == null) {
            LogUtil.warn("未找到要删除的目录项：" + entryName + "，目录：" + dirEntry.getFullPath());
            return null;
        }

        // 关键修复：先标记文件为已删除，确保同步到磁盘时能正确过滤
        toRemove.markAsDeleted();
        LogUtil.debug("已标记目录项为已删除：" + entryName);

        // 从缓存移除并标记为脏
        entriesCache.remove(toRemove);
        isDirty = true;
        LogUtil.debug("已从目录本地缓存移除条目：" + entryName);

        // 同时从FileSystem的全局缓存中移除对应的条目
        String fullPath = toRemove.getFullPath();
        fileSystem.removeEntryFromCache(fullPath);
        LogUtil.debug("已请求从FileSystem全局缓存移除条目：" + fullPath);
        
        // 移除自动同步，由上层调用者决定何时同步到磁盘
        // syncToDisk();  // 注释掉自动同步

        LogUtil.info("目录删除项成功：" + entryName + " → " + dirEntry.getFullPath());
        return toRemove;
    }

    /**
     * 查找子目录项（按名称）
     * @param entryName 子项名称
     * @return 找到的子项，不存在则返回null
     */
    public FileEntry findEntryByName(String entryName) {
        for (FileEntry entry : entriesCache) {
            if (entry.getName().equals(entryName) && !entry.isDeleted()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 刷新目录项缓存（重新从磁盘加载）
     */
    public void refreshEntries() {
        loadEntriesFromDisk();
    }
    
    /**
     * 获取所有子目录项
     * @return 子项列表（返回副本，避免外部修改）
     */
    public List<FileEntry> listEntries() {
        return new ArrayList<>(entriesCache);
    }

    /**
     * 检查目录是否为空
     * @return 空目录返回true，否则返回false
     */
    public boolean isEmpty() {
        return entriesCache.isEmpty();
    }

    // ======================== 持久化操作（磁盘读写） ========================

    /**
     * 从磁盘加载目录项（覆盖当前缓存）
     */
    public void loadEntriesFromDisk() {
        if (dirEntry.getStartBlockId() == -1) {
            // 未分配块，清空缓存
            entriesCache.clear();
            isDirty = false;
            return;
        }

        try {
            List<FileEntry> loadedEntries = new ArrayList<>();
            int currentBlockId = dirEntry.getStartBlockId();

            // 遍历所有块（如果目录不为空）
            while (currentBlockId != FAT.END_OF_FILE) {
                // 读取块数据
                byte[] blockData = disk.readBlock(currentBlockId);
                String blockContent = new String(blockData).trim();
                
                if (!blockContent.isEmpty()) {
                    // 按条目结束符分割
                    String[] entryStrings = blockContent.split(ENTRY_TERMINATOR);
                    for (String entryStr : entryStrings) {
                        if (!entryStr.trim().isEmpty()) {
                            // 解析条目
                            FileEntry entry = parseEntryString(entryStr);
                            if (entry != null) {
                                // 重要：只加载未被删除的条目，避免已删除文件重新出现
                                if (!entry.isDeleted()) {
                                    // 检查是否有重复项（同名且未删除）
                                    boolean isDuplicate = false;
                                    for (FileEntry loadedEntry : loadedEntries) {
                                        if (loadedEntry.getName().equals(entry.getName())) {
                                            LogUtil.warn("发现重复的目录项（已跳过）：" + entry.getName() + "，目录：" + dirEntry.getFullPath());
                                            isDuplicate = true;
                                            break;
                                        }
                                    }
                                    if (!isDuplicate) {
                                        loadedEntries.add(entry);
                                    }
                                } else {
                                    LogUtil.debug("跳过已删除的目录项：" + entry.getName() + "，目录：" + dirEntry.getFullPath());
                                }
                            }
                        }
                    }
                }

                // 移动到下一块
                currentBlockId = fat.getNextBlock(currentBlockId);
            }
            
            // 替换缓存并标记为未修改
            entriesCache = loadedEntries;
            isDirty = false;
            LogUtil.debug("从磁盘加载目录项完成：" + dirEntry.getFullPath() + "，共" + entriesCache.size() + "项");
        } catch (Exception e) {
            LogUtil.error("加载目录项失败：" + dirEntry.getFullPath(), e);
            entriesCache.clear(); // 加载失败时清空缓存
        }
    }

    /* 将目录项同步到磁盘（覆盖写入块链，自动管理块分配）
            * @throws FileSystemException 块分配失败或写入失败时抛出
     */
    public void syncToDisk() throws FileSystemException {
        if (!isDirty) {
            return; // 缓存未修改，无需同步
        }

        try {
            if (entriesCache.isEmpty()) {
                // 空目录：释放原块并更新元数据
                int currentBlockId = dirEntry.getStartBlockId();
                if (currentBlockId != -1 && !fat.isFreeBlock(currentBlockId)) {
                    fat.markAsFreeBlock(currentBlockId); // 释放块为空闲
                }
                // 关键修复1：更新目录项的起始块ID为-1（未分配）
                updateDirEntryStartBlock(-1); // 建议用setter或重新创建实例，避免反射
            } else {
                // 非空目录：先分配新块链，成功后再释放旧链
                int requiredBlocks = (entriesCache.size() + MAX_ENTRIES_PER_BLOCK - 1) / MAX_ENTRIES_PER_BLOCK;
                int startBlockId = -1;
                int prevBlockId = -1;

                // 1. 分配新块链（避免先释放旧链导致中间状态错误）
                if (requiredBlocks > 0) {
                    startBlockId = fat.allocateBlock(); // 起始块
                    prevBlockId = startBlockId;

                    // 分配剩余块
                    for (int i = 1; i <= requiredBlocks; i++) {
                        int nextBlockId = fat.allocateBlock();
                        fat.setNextBlock(prevBlockId, nextBlockId);
                        prevBlockId = nextBlockId;
                    }
                    fat.setNextBlock(prevBlockId, FAT.END_OF_FILE); // 标记链结束
                }

                // 2. 写入新块链数据
                if (startBlockId != -1) {
                    int entryIndex = 0;
                    int currentBlockId = startBlockId;
                    while (currentBlockId != FAT.END_OF_FILE && entryIndex < entriesCache.size()) {
                        StringBuilder blockContent = new StringBuilder();
                        int entriesInBlock = 0;

                        while (entriesInBlock < MAX_ENTRIES_PER_BLOCK && entryIndex < entriesCache.size()) {
                            FileEntry entry = entriesCache.get(entryIndex);
                            // 只写入未被删除的条目，防止已删除文件在同步后重新出现
                            if (!entry.isDeleted()) {
                                blockContent.append(formatEntryString(entry))
                                        .append(ENTRY_TERMINATOR);
                                entriesInBlock++;
                            }
                            entryIndex++;
                        }

                        // 填充块数据并写入
                        byte[] blockData = blockContent.toString().getBytes();
                        byte[] fullBlock = new byte[blockSize];
                        System.arraycopy(blockData, 0, fullBlock, 0, Math.min(blockData.length, blockSize));
                        disk.writeBlock(currentBlockId, fullBlock);

                        currentBlockId = fat.getNextBlock(currentBlockId);
                    }
                }

                // 3. 释放旧块链（确保新块链已成功分配和写入）
                int oldStartBlockId = dirEntry.getStartBlockId();
                if (oldStartBlockId != -1) {
                    fat.freeBlocks(oldStartBlockId);
                }

                // 4. 更新目录项的起始块ID（新块链的起始块）
                updateDirEntryStartBlock(startBlockId); // 优先用setter：dirEntry.setStartBlockId(startBlockId)

                LogUtil.debug("目录项同步到磁盘完成：" + dirEntry.getFullPath() + "，块数：" + requiredBlocks);
            }
            isDirty = false;

        } catch (InvalidBlockIdException | DiskFullException e) {
            throw new FileSystemException("目录项同步失败：块分配异常", e);
        } catch (DiskWriteException e) {
            throw new FileSystemException("目录项同步失败：磁盘写入错误", e);
        }
    }


    // ======================== 目录项序列化与反序列化 ========================

    /**
     * 将FileEntry格式化为字符串（用于存储到磁盘）
     * 格式：名称|类型|起始块ID|大小|是否删除|UUID
     */
    private String formatEntryString(FileEntry entry) {
        return String.join(ENTRY_SEPARATOR,
                truncateName(entry.getName()),  // 名称（截断过长名称）
                entry.getType().name(),         // 类型（FILE/DIRECTORY）
                String.valueOf(entry.getStartBlockId()),
                String.valueOf(entry.getSize()),
                String.valueOf(entry.isDeleted()),
                entry.getUuid()  // 添加UUID字段
        );
    }

    /**
     * 将字符串解析为FileEntry（从磁盘读取后恢复）
     */
    private FileEntry parseEntryString(String entryStr) {
        try {
            // 添加调试信息，显示完整的entryStr和其长度
            LogUtil.debug("解析目录项：长度=" + entryStr.length() + ", 内容=" + entryStr + ", 分隔符=" + ENTRY_SEPARATOR);
            
            // 修复：对特殊字符进行转义，避免split将|当作正则表达式的或操作符
            String[] parts = entryStr.split(Pattern.quote(ENTRY_SEPARATOR));
            
            // 调试parts数组内容
            StringBuilder partsDebug = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                partsDebug.append("[").append(i).append("]=").append(parts[i]);
                if (i < parts.length - 1) partsDebug.append(", ");
            }
            LogUtil.debug("分割后数组：长度=" + parts.length + ", 内容=[" + partsDebug + "]");
            
            if (parts.length != 6) { // 现在需要6个部分，因为添加了UUID
                // 兼容旧格式（没有UUID的情况）
                if (parts.length == 5) {
                    LogUtil.debug("检测到旧格式目录项，将使用新的UUID");
                    // 创建一个新的UUID
                    String uuid = UUID.randomUUID().toString();
                    
                    // 创建临时FileEntry，使用新UUID构造器
                    FileEntry entry = new FileEntry(parts[0], 
                                                  FileEntry.EntryType.valueOf(parts[1]), 
                                                  dirEntry.getFullPath(), 
                                                  Integer.parseInt(parts[2]),
                                                  uuid);
                    
                    // 恢复大小和删除状态
                    if (entry.getType() == FileEntry.EntryType.FILE) {
                        setEntrySize(entry, Long.parseLong(parts[3]));
                    }
                    if (Boolean.parseBoolean(parts[4])) {
                        entry.markAsDeleted();
                    }
                    
                    return entry;
                }
                
                LogUtil.warn("无效的目录项格式：" + entryStr + ", 分割后长度=" + parts.length);
                return null;
            }

            String name = parts[0];
            FileEntry.EntryType type = FileEntry.EntryType.valueOf(parts[1]);
            int startBlockId = Integer.parseInt(parts[2]);
            long size = Long.parseLong(parts[3]);
            boolean isDeleted = Boolean.parseBoolean(parts[4]);
            String uuid = parts[5]; // 获取UUID字段

            // 创建FileEntry，使用包含UUID的构造器
            FileEntry entry = new FileEntry(name, type, dirEntry.getFullPath(), startBlockId, uuid);

            // 恢复大小和删除状态
            if (type == FileEntry.EntryType.FILE) {
                setEntrySize(entry, size);
            }
            if (isDeleted) {
                entry.markAsDeleted();
            }

            return entry;
        } catch (Exception e) {
            LogUtil.warn("解析目录项失败：" + entryStr, e);
            return null;
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 验证目录项合法性
     */
    private void validateEntry(FileEntry entry) throws FileSystemException {
        // 校验名称长度
        if (entry.getName().length() > MAX_ENTRY_NAME_LENGTH) {
            throw new FileSystemException("目录项名称过长（最大" + MAX_ENTRY_NAME_LENGTH + "字符）：" + entry.getName());
        }

        // 校验父路径是否匹配当前目录
        if (!entry.getParentPath().equals(dirEntry.getFullPath())) {
            throw new FileSystemException("目录项父路径不匹配：" + entry.getParentPath() + " != " + dirEntry.getFullPath());
        }
    }

    /**
     * 截断过长的名称
     */
    private String truncateName(String name) {
        if (name.length() <= MAX_ENTRY_NAME_LENGTH) {
            return name;
        }
        return name.substring(0, MAX_ENTRY_NAME_LENGTH);
    }

    /**
     * 更新目录元数据的起始块ID（使用setter方法，安全且符合封装原则）
     */
    private void updateDirEntryStartBlock(int startBlockId) {
        try {
            dirEntry.setStartBlockId(startBlockId); // 直接调用setter
        } catch (IllegalArgumentException e) {
            LogUtil.error("更新目录起始块ID失败：无效的块ID", e);
            throw e; // 抛出异常让上层处理，避免无效状态
        }
    }

    /**
     * 设置文件大小（反射实现，实际项目建议在FileEntry添加setter）
     */
    private void setEntrySize(FileEntry entry, long size) {
        try {
            var field = FileEntry.class.getDeclaredField("size");
            field.setAccessible(true);
            field.set(entry, size);
        } catch (Exception e) {
            LogUtil.error("设置文件大小失败", e);
        }
    }

    // ======================== Getter方法 ========================

    public FileEntry getDirEntry() {
        return dirEntry;
    }

    public boolean isDirty() {
        return isDirty;
    }

    // Directory.java
    /**
     * 获取目录下所有条目（返回缓存的副本）
     * @return 目录条目列表
     */
    public List<FileEntry> getEntries() {
        return new ArrayList<>(entriesCache);
    }
}
