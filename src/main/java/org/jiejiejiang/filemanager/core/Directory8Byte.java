package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskFullException;
import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.jiejiejiang.filemanager.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 8字节目录项管理器
 * 管理固定8字节结构的目录项，符合题目规范
 */
public class Directory8Byte {
    private static final int ENTRIES_PER_BLOCK = 64; // 512字节块 / 8字节目录项 = 64个目录项
    
    private final Disk disk;
    private final FAT fat;
    private final int blockSize;
    private int startBlockId;
    
    // 目录项缓存
    private List<DirectoryEntry8Byte> entries;
    private Map<String, DirectoryEntry8Byte> nameToEntryMap; // 文件名到目录项的映射
    private boolean isDirty = false;
    
    /**
     * 构造函数
     * @param disk 磁盘对象
     * @param fat FAT表
     * @param startBlockId 目录起始块ID
     */
    public Directory8Byte(Disk disk, FAT fat, int startBlockId) {
        this.disk = disk;
        this.fat = fat;
        this.blockSize = disk.getBlockSize();
        this.startBlockId = startBlockId;
        this.entries = new ArrayList<>();
        this.nameToEntryMap = new HashMap<>();
        
        if (startBlockId != -1) {
            loadFromDisk();
        }
    }
    
    /**
     * 从磁盘加载目录项
     */
    private void loadFromDisk() {
        try {
            entries.clear();
            nameToEntryMap.clear();
            
            int currentBlockId = startBlockId;
            while (currentBlockId != FAT.END_OF_FILE) {
                byte[] blockData = disk.readBlock(currentBlockId);
                
                // 解析块中的目录项
                for (int i = 0; i < ENTRIES_PER_BLOCK; i++) {
                    int offset = i * DirectoryEntry8Byte.ENTRY_SIZE;
                    if (offset + DirectoryEntry8Byte.ENTRY_SIZE <= blockData.length) {
                        byte[] entryData = new byte[DirectoryEntry8Byte.ENTRY_SIZE];
                        System.arraycopy(blockData, offset, entryData, 0, DirectoryEntry8Byte.ENTRY_SIZE);
                        
                        DirectoryEntry8Byte entry = new DirectoryEntry8Byte(entryData);
                        if (!entry.isEmpty()) {
                            entries.add(entry);
                            // 注意：由于只有哈希值，无法直接建立文件名映射
                            // 需要额外的文件名存储机制
                        }
                    }
                }
                
                currentBlockId = fat.getNextBlock(currentBlockId);
            }
            
            LogUtil.debug("从磁盘加载目录项完成，共" + entries.size() + "个条目");
        } catch (Exception e) {
            LogUtil.error("加载目录项失败: " + e.getMessage());
            // 如果加载失败，初始化为空目录
            entries.clear();
            nameToEntryMap.clear();
        }
    }
    
    /**
     * 添加目录项
     * @param name 文件名
     * @param startBlock 起始块ID
     * @param fileSize 文件大小
     * @param isDirectory 是否为目录
     */
    public void addEntry(String name, int startBlock, int fileSize, boolean isDirectory) {
        DirectoryEntry8Byte entry = new DirectoryEntry8Byte(name, startBlock, fileSize, isDirectory);
        entries.add(entry);
        nameToEntryMap.put(name, entry);
        isDirty = true;
        
        LogUtil.debug("添加目录项：" + entry);
    }
    
    /**
     * 删除目录项
     * @param name 文件名
     * @return 是否删除成功
     */
    public boolean removeEntry(String name) {
        DirectoryEntry8Byte entry = nameToEntryMap.get(name);
        if (entry != null) {
            entries.remove(entry);
            nameToEntryMap.remove(name);
            isDirty = true;
            LogUtil.debug("删除目录项：" + name);
            return true;
        }
        return false;
    }
    
    /**
     * 查找目录项
     * @param name 文件名
     * @return 目录项，如果不存在返回null
     */
    public DirectoryEntry8Byte findEntry(String name) {
        return nameToEntryMap.get(name);
    }
    
    /**
     * 获取所有目录项
     * @return 目录项列表
     */
    public List<DirectoryEntry8Byte> getAllEntries() {
        return new ArrayList<>(entries);
    }
    
    /**
     * 同步到磁盘
     */
    public void syncToDisk() {
        if (!isDirty) {
            return;
        }
        
        try {
            // 计算需要的块数
            int requiredBlocks = (entries.size() + ENTRIES_PER_BLOCK - 1) / ENTRIES_PER_BLOCK;
            if (requiredBlocks == 0) {
                requiredBlocks = 1; // 至少需要一个块
            }
            
            // 分配新的块链
            int newStartBlockId = -1;
            if (requiredBlocks > 0) {
                newStartBlockId = fat.allocateBlock();
                int currentBlockId = newStartBlockId;
                
                for (int i = 1; i < requiredBlocks; i++) {
                    int nextBlockId = fat.allocateBlock();
                    fat.setNextBlock(currentBlockId, nextBlockId);
                    currentBlockId = nextBlockId;
                }
                fat.setNextBlock(currentBlockId, -1);
            }
            
            // 写入数据
            if (newStartBlockId != -1) {
                int entryIndex = 0;
                int currentBlockId = newStartBlockId;
                
                while (currentBlockId != FAT.END_OF_FILE && entryIndex < entries.size()) {
                    byte[] blockData = new byte[blockSize];
                    
                    // 填充块数据
                    for (int i = 0; i < ENTRIES_PER_BLOCK && entryIndex < entries.size(); i++) {
                        DirectoryEntry8Byte entry = entries.get(entryIndex);
                        byte[] entryData = entry.toBytes();
                        System.arraycopy(entryData, 0, blockData, i * DirectoryEntry8Byte.ENTRY_SIZE, DirectoryEntry8Byte.ENTRY_SIZE);
                        entryIndex++;
                    }
                    
                    disk.writeBlock(currentBlockId, blockData);
                    currentBlockId = fat.getNextBlock(currentBlockId);
                }
            }
            
            // 释放旧块链
            if (startBlockId != -1) {
                fat.freeBlocks(startBlockId);
            }
            
            // 更新起始块ID
            startBlockId = newStartBlockId;
            isDirty = false;
            
            LogUtil.debug("目录项同步到磁盘完成，块数：" + requiredBlocks);
        } catch (InvalidBlockIdException | DiskFullException e) {
            LogUtil.error("目录项同步失败: " + e.getMessage());
            // 同步失败时保持dirty状态，允许重试
        } catch (Exception e) {
            LogUtil.error("目录项同步时发生未知错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取起始块ID
     */
    public int getStartBlockId() {
        return startBlockId;
    }
    
    /**
     * 设置起始块ID
     */
    public void setStartBlockId(int startBlockId) {
        this.startBlockId = startBlockId;
    }
    
    /**
     * 获取目录项数量
     */
    public int getEntryCount() {
        return entries.size();
    }
    
    /**
     * 检查是否有未保存的更改
     */
    public boolean isDirty() {
        return isDirty;
    }
}