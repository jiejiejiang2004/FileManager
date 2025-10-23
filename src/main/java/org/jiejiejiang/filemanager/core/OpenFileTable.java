package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.util.LogUtil;

/**
 * 已打开文件表（Open File Table, OFT）
 * 管理最多5个同时打开的文件，每个文件维护独立的读写指针
 */
public class OpenFileTable {
    
    // ======================== 常量定义 ========================
    /** OFT最大容量 */
    public static final int MAX_OPEN_FILES = 5;
    
    // ======================== 打开文件条目结构 ========================
    /**
     * 打开文件条目，包含文件信息和读写指针
     */
    public static class OpenFileEntry {
        private final String filePath;          // 文件完整路径
        private final FileEntry fileEntry;     // 文件元数据
        private final String mode;             // 打开模式：READ, WRITE, READ_WRITE
        private long readPointer;              // 读指针位置（字节偏移）
        private long writePointer;             // 写指针位置（字节偏移）
        private boolean isActive;              // 是否活跃状态
        
        public OpenFileEntry(String filePath, FileEntry fileEntry, String mode) {
            this.filePath = filePath;
            this.fileEntry = fileEntry;
            this.mode = mode;
            this.readPointer = 0;
            this.writePointer = 0;
            this.isActive = true;
        }
        
        // Getters and Setters
        public String getFilePath() { return filePath; }
        public FileEntry getFileEntry() { return fileEntry; }
        public String getMode() { return mode; }
        public long getReadPointer() { return readPointer; }
        public void setReadPointer(long readPointer) { this.readPointer = readPointer; }
        public long getWritePointer() { return writePointer; }
        public void setWritePointer(long writePointer) { this.writePointer = writePointer; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { this.isActive = active; }
        
        @Override
        public String toString() {
            return String.format("OpenFileEntry{path='%s', mode='%s', readPtr=%d, writePtr=%d, active=%s}", 
                    filePath, mode, readPointer, writePointer, isActive);
        }
    }
    
    // ======================== OFT数据结构 ========================
    /** 打开文件表数组，固定容量5 */
    private final OpenFileEntry[] openFiles;
    
    // ======================== 构造器 ========================
    public OpenFileTable() {
        this.openFiles = new OpenFileEntry[MAX_OPEN_FILES];
        LogUtil.info("已打开文件表（OFT）初始化完成，容量：" + MAX_OPEN_FILES);
    }
    
    // ======================== 文件打开操作 ========================
    /**
     * 打开文件，分配OFT条目
     * @param filePath 文件完整路径
     * @param fileEntry 文件元数据
     * @param mode 打开模式（READ, WRITE, READ_WRITE）
     * @return OFT索引（0-4），如果OFT已满则返回-1
     * @throws FileSystemException 参数无效时抛出
     */
    public int openFile(String filePath, FileEntry fileEntry, String mode) throws FileSystemException {
        if (filePath == null || fileEntry == null || mode == null) {
            throw new FileSystemException("打开文件失败：参数不能为空");
        }
        
        // 检查文件是否已经打开
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            if (openFiles[i] != null && openFiles[i].isActive() && 
                openFiles[i].getFilePath().equals(filePath)) {
                LogUtil.warn("文件已在OFT中打开：" + filePath + "，索引：" + i);
                return i; // 返回已存在的索引
            }
        }
        
        // 查找空闲的OFT条目
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            if (openFiles[i] == null || !openFiles[i].isActive()) {
                openFiles[i] = new OpenFileEntry(filePath, fileEntry, mode);
                LogUtil.info("文件已打开并加入OFT：" + filePath + "，索引：" + i + "，模式：" + mode);
                return i;
            }
        }
        
        LogUtil.error("OFT已满，无法打开更多文件：" + filePath);
        return -1; // OFT已满
    }
    
    // ======================== 文件关闭操作 ========================
    /**
     * 关闭文件，释放OFT条目
     * @param oftIndex OFT索引
     * @throws FileSystemException 索引无效时抛出
     */
    public void closeFile(int oftIndex) throws FileSystemException {
        validateOftIndex(oftIndex);
        
        if (openFiles[oftIndex] == null || !openFiles[oftIndex].isActive()) {
            throw new FileSystemException("关闭文件失败：OFT索引 " + oftIndex + " 处无活跃文件");
        }
        
        String filePath = openFiles[oftIndex].getFilePath();
        openFiles[oftIndex].setActive(false);
        openFiles[oftIndex] = null; // 清空条目
        LogUtil.info("文件已关闭并从OFT移除：" + filePath + "，索引：" + oftIndex);
    }
    
    /**
     * 根据文件路径关闭文件
     * @param filePath 文件完整路径
     * @throws FileSystemException 文件未在OFT中打开时抛出
     */
    public void closeFile(String filePath) throws FileSystemException {
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            if (openFiles[i] != null && openFiles[i].isActive() && 
                openFiles[i].getFilePath().equals(filePath)) {
                closeFile(i);
                return;
            }
        }
        throw new FileSystemException("关闭文件失败：文件未在OFT中打开 → " + filePath);
    }
    
    // ======================== 指针操作 ========================
    /**
     * 设置读指针位置
     * @param oftIndex OFT索引
     * @param position 新的读指针位置
     * @throws FileSystemException 索引无效或位置超出文件大小时抛出
     */
    public void setReadPointer(int oftIndex, long position) throws FileSystemException {
        validateOftIndex(oftIndex);
        OpenFileEntry entry = getActiveEntry(oftIndex);
        
        if (position < 0 || position > entry.getFileEntry().getSize()) {
            throw new FileSystemException("设置读指针失败：位置超出范围 [0, " + entry.getFileEntry().getSize() + "]");
        }
        
        entry.setReadPointer(position);
        LogUtil.debug("读指针已设置：" + entry.getFilePath() + "，位置：" + position);
    }
    
    /**
     * 设置写指针位置
     * @param oftIndex OFT索引
     * @param position 新的写指针位置
     * @throws FileSystemException 索引无效或位置超出文件大小时抛出
     */
    public void setWritePointer(int oftIndex, long position) throws FileSystemException {
        validateOftIndex(oftIndex);
        OpenFileEntry entry = getActiveEntry(oftIndex);
        
        if (position < 0 || position > entry.getFileEntry().getSize()) {
            throw new FileSystemException("设置写指针失败：位置超出范围 [0, " + entry.getFileEntry().getSize() + "]");
        }
        
        entry.setWritePointer(position);
        LogUtil.debug("写指针已设置：" + entry.getFilePath() + "，位置：" + position);
    }
    
    // ======================== 查询操作 ========================
    /**
     * 获取OFT条目
     * @param oftIndex OFT索引
     * @return 打开文件条目，如果索引无效或文件未打开则返回null
     */
    public OpenFileEntry getOpenFileEntry(int oftIndex) {
        if (oftIndex < 0 || oftIndex >= MAX_OPEN_FILES) {
            return null;
        }
        return openFiles[oftIndex];
    }
    
    /**
     * 检查文件是否已打开
     * @param filePath 文件完整路径
     * @return 如果文件已打开则返回OFT索引，否则返回-1
     */
    public int findOpenFile(String filePath) {
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            if (openFiles[i] != null && openFiles[i].isActive() && 
                openFiles[i].getFilePath().equals(filePath)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 获取当前打开的文件数量
     * @return 活跃文件数量
     */
    public int getOpenFileCount() {
        int count = 0;
        for (OpenFileEntry entry : openFiles) {
            if (entry != null && entry.isActive()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取所有打开的文件路径
     * @return 打开文件路径列表
     */
    public java.util.List<String> getOpenFilePaths() {
        java.util.List<String> paths = new java.util.ArrayList<>();
        for (OpenFileEntry entry : openFiles) {
            if (entry != null && entry.isActive()) {
                paths.add(entry.getFilePath());
            }
        }
        return paths;
    }
    
    // ======================== 工具方法 ========================
    /**
     * 验证OFT索引有效性
     * @param oftIndex OFT索引
     * @throws FileSystemException 索引无效时抛出
     */
    private void validateOftIndex(int oftIndex) throws FileSystemException {
        if (oftIndex < 0 || oftIndex >= MAX_OPEN_FILES) {
            throw new FileSystemException("OFT索引无效：" + oftIndex + "，有效范围：[0, " + (MAX_OPEN_FILES - 1) + "]");
        }
    }
    
    /**
     * 获取活跃的OFT条目
     * @param oftIndex OFT索引
     * @return 活跃的打开文件条目
     * @throws FileSystemException 条目不存在或非活跃时抛出
     */
    private OpenFileEntry getActiveEntry(int oftIndex) throws FileSystemException {
        if (openFiles[oftIndex] == null || !openFiles[oftIndex].isActive()) {
            throw new FileSystemException("OFT索引 " + oftIndex + " 处无活跃文件");
        }
        return openFiles[oftIndex];
    }
    
    /**
     * 关闭所有打开的文件（用于系统关闭时清理）
     */
    public void closeAllFiles() {
        int closedCount = 0;
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            if (openFiles[i] != null && openFiles[i].isActive()) {
                openFiles[i].setActive(false);
                openFiles[i] = null;
                closedCount++;
            }
        }
        LogUtil.info("OFT清理完成，关闭了 " + closedCount + " 个文件");
    }
    
    /**
     * 获取OFT状态信息（用于调试）
     * @return OFT状态字符串
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("OFT状态 [").append(getOpenFileCount()).append("/").append(MAX_OPEN_FILES).append("]:\n");
        for (int i = 0; i < MAX_OPEN_FILES; i++) {
            sb.append("  [").append(i).append("] ");
            if (openFiles[i] != null && openFiles[i].isActive()) {
                sb.append(openFiles[i].toString());
            } else {
                sb.append("空闲");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}