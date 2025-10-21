package org.jiejiejiang.filemanager.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 8字节目录项结构，符合题目规范
 * 结构：
 * - 文件名哈希 (4字节)
 * - 起始块ID (2字节) 
 * - 文件大小 (2字节)
 */
public class DirectoryEntry8Byte {
    public static final int ENTRY_SIZE = 8; // 8字节固定大小
    public static final int EMPTY_ENTRY = 0; // 空目录项标识
    
    private int nameHash;      // 文件名哈希值 (4字节)
    private short startBlock;  // 起始块ID (2字节, 0-65535)
    private short fileSize;    // 文件大小 (2字节, 0-65535字节)
    
    // 原始文件名，用于显示（不存储在8字节结构中）
    private String originalName;
    private boolean isDirectory;
    
    /**
     * 构造函数
     * @param name 文件名
     * @param startBlock 起始块ID
     * @param fileSize 文件大小
     * @param isDirectory 是否为目录
     */
    public DirectoryEntry8Byte(String name, int startBlock, int fileSize, boolean isDirectory) {
        this.originalName = name;
        this.nameHash = calculateNameHash(name);
        this.startBlock = (short) startBlock;
        this.fileSize = (short) fileSize;
        this.isDirectory = isDirectory;
    }
    
    /**
     * 从字节数组构造
     * @param data 8字节数据
     */
    public DirectoryEntry8Byte(byte[] data) {
        if (data.length != ENTRY_SIZE) {
            throw new IllegalArgumentException("目录项数据必须为8字节");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.nameHash = buffer.getInt();
        this.startBlock = buffer.getShort();
        this.fileSize = buffer.getShort();
        
        // 原始名称需要从其他地方获取，这里设为空
        this.originalName = "";
        this.isDirectory = false;
    }
    
    /**
     * 计算文件名哈希值
     * 使用简单的哈希算法确保在4字节范围内
     */
    private int calculateNameHash(String name) {
        if (name == null || name.isEmpty()) {
            return EMPTY_ENTRY;
        }
        
        int hash = 0;
        for (char c : name.toCharArray()) {
            hash = hash * 31 + c;
        }
        return hash;
    }
    
    /**
     * 转换为8字节数组
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(nameHash);
        buffer.putShort(startBlock);
        buffer.putShort(fileSize);
        return buffer.array();
    }
    
    /**
     * 检查是否为空目录项
     */
    public boolean isEmpty() {
        return nameHash == EMPTY_ENTRY && startBlock == 0 && fileSize == 0;
    }
    
    /**
     * 创建空目录项
     */
    public static DirectoryEntry8Byte createEmpty() {
        return new DirectoryEntry8Byte("", 0, 0, false);
    }
    
    // Getters
    public int getNameHash() {
        return nameHash;
    }
    
    public int getStartBlock() {
        return Short.toUnsignedInt(startBlock);
    }
    
    public int getFileSize() {
        return Short.toUnsignedInt(fileSize);
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public boolean isDirectory() {
        return isDirectory;
    }
    
    // Setters
    public void setOriginalName(String name) {
        this.originalName = name;
        this.nameHash = calculateNameHash(name);
    }
    
    public void setStartBlock(int startBlock) {
        this.startBlock = (short) startBlock;
    }
    
    public void setFileSize(int fileSize) {
        this.fileSize = (short) fileSize;
    }
    
    public void setDirectory(boolean directory) {
        this.isDirectory = directory;
    }
    
    @Override
    public String toString() {
        return String.format("DirectoryEntry8Byte{name='%s', hash=%d, startBlock=%d, size=%d, isDir=%b}", 
                originalName, nameHash, getStartBlock(), getFileSize(), isDirectory);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DirectoryEntry8Byte other = (DirectoryEntry8Byte) obj;
        return nameHash == other.nameHash && 
               startBlock == other.startBlock && 
               fileSize == other.fileSize;
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[]{nameHash, startBlock, fileSize});
    }
}