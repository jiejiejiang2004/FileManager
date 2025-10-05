package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskInitializeException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.jiejiejiang.filemanager.exception.DiskFullException;
import org.jiejiejiang.filemanager.util.LogUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件分配表（FAT）：管理磁盘块的分配、回收与链式存储
 * 核心功能：标记块状态、分配空闲块、释放已用块、查询块的后续块
 */
public class FAT {
    // ======================== FAT 特殊标记值 ========================
    /** 空闲块标记（表示该块未被使用） */
    public static final int FREE_BLOCK = 0;
    /** 文件结束标记（表示该块是文件的最后一个块） */
    public static final int END_OF_FILE = -1;
    /** 坏块标记（表示该块物理损坏，不可使用） */
    public static final int BAD_BLOCK = -2;

    // ======================== 核心属性 ========================
    /** 关联的磁盘实例（依赖 Disk 获取块大小、总块数） */
    private final Disk disk;
    /** FAT 表本体：索引 = 磁盘块ID，值 = 下一个块ID（或特殊标记） */
    private int[] fatTable;
    /** 磁盘总块数（从 Disk 同步） */
    private int totalBlocks;
    /** FAT 是否已初始化 */
    private boolean isInitialized;

    // ======================== 构造器 ========================
    /**
     * 初始化 FAT，关联磁盘实例
     * @param disk 已初始化的 Disk 实例（必须先调用 disk.initialize()）
     * @throws DiskInitializeException 磁盘未初始化时抛出
     */
    public FAT(Disk disk) throws DiskInitializeException {
        if (!disk.isInitialized()) {
            throw new DiskInitializeException("FAT 初始化失败：关联的磁盘未初始化");
        }
        this.disk = disk;
        this.totalBlocks = disk.getTotalBlocks();
        this.isInitialized = false;
    }

    // ======================== 核心方法 ========================
    /**
     * 初始化 FAT 表：
     * 1. 创建 FAT 表数组（大小 = 磁盘总块数）
     * 2. 初始化所有块为“空闲”状态（FREE_BLOCK）
     * 3. 标记预留块（如第0块用于引导，可选）
     */
    public void initialize() {
        // 1. 初始化 FAT 表数组
        this.fatTable = new int[totalBlocks];
        // 2. 所有块默认设为空闲（FREE_BLOCK）
        Arrays.fill(fatTable, FREE_BLOCK);
//        for (int i = 0; i < totalBlocks; i++) {
//            freeBlocks.add(i);
//        }
        // 3. 可选：标记第0块为预留（模拟真实FAT的引导块）
        if (totalBlocks > 0) {
            fatTable[0] = BAD_BLOCK; // 此处用 BAD_BLOCK 模拟预留，实际可自定义
            LogUtil.info("FAT 初始化：预留块 0 为引导区（标记为坏块）");
        }
        this.isInitialized = true;
        LogUtil.info("FAT 初始化完成：总块数=" + totalBlocks + "，FAT表大小=" + fatTable.length);
    }

    /**
     * 分配一个空闲块
     * @return 分配到的块ID（>=1，因为0块预留）
     * @throws DiskFullException 无空闲块时抛出
     * @throws InvalidBlockIdException FAT 未初始化时抛出
     */
    public synchronized int allocateBlock() throws DiskFullException, InvalidBlockIdException {
        checkInitialized();

        // 遍历 FAT 表，找第一个空闲块（从1开始，跳过0号预留块）
        for (int blockId = 1; blockId < totalBlocks; blockId++) {
            if (fatTable[blockId] == FREE_BLOCK) {
                // 标记块为“已占用”（初始无后续块，设为 END_OF_FILE）
                fatTable[blockId] = END_OF_FILE;
                LogUtil.info("FAT 分配块：blockId=" + blockId);
                return blockId;
            }
        }

        // 无空闲块时抛出异常
        throw new DiskFullException("磁盘无空闲块：总块数=" + totalBlocks + "，已用尽");
    }

    /**
     * 为指定块分配“下一个块”（用于文件扩展）
     * @param currentBlockId 当前块ID（已分配的块）
     * @return 新分配的下一个块ID
     * @throws InvalidBlockIdException currentBlockId 无效或未占用时抛出
     * @throws DiskFullException 无空闲块时抛出
     */
    public synchronized int allocateNextBlock(int currentBlockId) throws InvalidBlockIdException, DiskFullException {
        checkInitialized();
        validateAllocatedBlock(currentBlockId); // 校验当前块是否合法且已占用

        // 1. 分配一个新的空闲块
        int nextBlockId = allocateBlock();
        // 2. 将当前块的“下一块”指向新块
        fatTable[currentBlockId] = nextBlockId;
        // 3. 新块的“下一块”设为文件结束（END_OF_FILE）
        fatTable[nextBlockId] = END_OF_FILE;

        LogUtil.info("FAT 扩展块：currentBlockId=" + currentBlockId + " → nextBlockId=" + nextBlockId);
        return nextBlockId;
    }

    /**
     * 释放指定块及其后续所有块（用于删除文件）
     * @param startBlockId 文件的起始块ID
     * @throws InvalidBlockIdException startBlockId 无效或未占用时抛出
     */
    public synchronized void freeBlocks(int startBlockId) throws InvalidBlockIdException {
        checkInitialized();
        validateAllocatedBlock(startBlockId); // 校验起始块是否合法且已占用

        int currentBlockId = startBlockId;
        // 循环释放当前块及后续所有块
        while (true) {
            // 1. 记录下一个块ID（避免释放后丢失后续块引用）
            int nextBlockId = fatTable[currentBlockId];
            // 2. 将当前块标记为空闲
            fatTable[currentBlockId] = FREE_BLOCK;
            LogUtil.info("FAT 释放块：blockId=" + currentBlockId);

            // 3. 终止条件：当前块是文件最后一块（END_OF_FILE）
            if (nextBlockId == END_OF_FILE) {
                break;
            }

            // 4. 校验下一个块ID是否合法（防止循环引用或无效值）
            if (nextBlockId < 1 || nextBlockId >= totalBlocks || fatTable[nextBlockId] == FREE_BLOCK) {
                throw new InvalidBlockIdException("释放块失败：无效的后续块ID=" + nextBlockId + "（当前块ID=" + currentBlockId + "）");
            }

            // 5. 移动到下一个块
            currentBlockId = nextBlockId;
        }
    }

    /**
     * 查询指定块的下一个块ID
     * @param blockId 待查询的块ID
     * @return 下一个块ID（或 END_OF_FILE 表示文件结束）
     * @throws InvalidBlockIdException blockId 无效或未占用时抛出
     */
    public int getNextBlock(int blockId) throws InvalidBlockIdException {
        checkInitialized();
        validateAllocatedBlock(blockId); // 校验块是否合法且已占用

        int nextBlockId = fatTable[blockId];
        // 校验下一个块ID的合法性（只能是 END_OF_FILE 或有效块ID）
        if (nextBlockId != END_OF_FILE && (nextBlockId < 1 || nextBlockId >= totalBlocks)) {
            throw new InvalidBlockIdException("查询下一块失败：块ID=" + blockId + " 的后续块ID=" + nextBlockId + " 无效");
        }
        return nextBlockId;
    }

    /**
     * 计算文件占用的总块数（通过起始块遍历链式结构）
     * @param startBlockId 文件的起始块ID
     * @return 文件占用的块数
     * @throws InvalidBlockIdException startBlockId 无效或未占用时抛出
     */
    public int getFileBlockCount(int startBlockId) throws InvalidBlockIdException {
        checkInitialized();
        validateAllocatedBlock(startBlockId);

        int count = 0;
        int currentBlockId = startBlockId;
        while (true) {
            count++;
            // 终止条件：当前块是文件最后一块
            if (fatTable[currentBlockId] == END_OF_FILE) {
                break;
            }
            // 移动到下一个块
            currentBlockId = fatTable[currentBlockId];
            // 校验下一个块（防止死循环）
            if (currentBlockId < 1 || currentBlockId >= totalBlocks || fatTable[currentBlockId] == FREE_BLOCK) {
                throw new InvalidBlockIdException("计算文件块数失败：无效的后续块ID=" + currentBlockId);
            }
        }
        return count;
    }

    // ======================== 辅助校验方法 ========================
    /**
     * 校验 FAT 是否已初始化
     * @throws InvalidBlockIdException FAT 未初始化时抛出
     */
    private void checkInitialized() throws InvalidBlockIdException {
        if (!isInitialized) {
            throw new InvalidBlockIdException("FAT 未初始化，请先调用 initialize() 方法");
        }
    }

    /**
     * 校验块ID是否合法且已占用（非空闲、非坏块）
     * @param blockId 待校验的块ID
     * @throws InvalidBlockIdException 块ID无效或未占用时抛出
     */
    private void validateAllocatedBlock(int blockId) throws InvalidBlockIdException {
        // 1. 校验块ID范围
        if (blockId < 1 || blockId >= totalBlocks) {
            throw new InvalidBlockIdException("无效的块ID=" + blockId + "（合法范围：1~" + (totalBlocks - 1) + "）");
        }
        // 2. 校验块状态（必须是已占用状态：值为 END_OF_FILE 或有效块ID）
        int blockStatus = fatTable[blockId];
        if (blockStatus == FREE_BLOCK || blockStatus == BAD_BLOCK) {
            throw new InvalidBlockIdException("块ID=" + blockId + " 未被占用（状态：" + getBlockStatusDesc(blockStatus) + "）");
        }
    }

    /**
     * 验证块ID是否合法
     * @param blockId 要验证的块ID
     * @throws InvalidBlockIdException 块ID无效时抛出
     */
    private void validateBlockId(int blockId) throws InvalidBlockIdException {
        if (blockId < 1 || blockId >= totalBlocks) {
            throw new InvalidBlockIdException("无效的块ID：" + blockId + "（有效范围：1-" + (totalBlocks - 1) + "）");
        }
        if (fatTable[blockId] == BAD_BLOCK) {
            throw new InvalidBlockIdException("块已标记为坏块：" + blockId);
        }
    }

    /**
     * 获取块状态的描述文本（用于日志和异常信息）
     * @param blockStatus 块状态值（FAT.FREE_BLOCK / END_OF_FILE 等）
     * @return 描述文本
     */
    private String getBlockStatusDesc(int blockStatus) {
        return switch (blockStatus) {
            case FREE_BLOCK -> "空闲";
            case END_OF_FILE -> "文件结束块";
            case BAD_BLOCK -> "坏块/预留块";
            default -> "已占用（后续块ID=" + blockStatus + "）";
        };
    }

    // ======================== 持久化方法（可选，用于保存FAT到磁盘） ========================
    /**
     * 将 FAT 表写入磁盘（写入连续的多个块）
     * @throws DiskInitializeException 磁盘写入失败时抛出
     */
    public void saveToDisk() throws DiskInitializeException {
        checkInitialized();
        try {
            // 1. 验证FAT表大小是否与总块数匹配
            if (fatTable.length != totalBlocks) {
                throw new IllegalStateException("FAT表大小不匹配：当前表大小=" + fatTable.length + "，总块数=" + totalBlocks);
            }

            // 2. 将 int[] FAT 表转为字节数组（每个 int 占4字节）
            byte[] fatBytes = new byte[totalBlocks * 4];
            for (int i = 0; i < totalBlocks; i++) {
                // int 转 4字节（小端序，适配多数系统）
                fatBytes[i * 4] = (byte) (fatTable[i] & 0xFF);
                fatBytes[i * 4 + 1] = (byte) ((fatTable[i] >> 8) & 0xFF);
                fatBytes[i * 4 + 2] = (byte) ((fatTable[i] >> 16) & 0xFF);
                fatBytes[i * 4 + 3] = (byte) ((fatTable[i] >> 24) & 0xFF);
            }

            // 3. 写入磁盘从第1块开始的连续块
            int blockSize = disk.getBlockSize();
            int requiredBlocks = (fatBytes.length + blockSize - 1) / blockSize; // 向上取整计算需要的块数
            
            LogUtil.info("开始保存FAT表到磁盘，总大小=" + fatBytes.length + "字节，需要" + requiredBlocks + "个块");
            
            // 分块写入
            for (int i = 0; i < requiredBlocks; i++) {
                int blockId = i + 1; // 从第1块开始写入
                int offset = i * blockSize;
                int length = Math.min(blockSize, fatBytes.length - offset);
                
                byte[] blockData = new byte[blockSize];
                System.arraycopy(fatBytes, offset, blockData, 0, length);
                
                disk.writeBlock(blockId, blockData);
                LogUtil.debug("FAT表块 " + blockId + " 已写入磁盘");
            }
            
            LogUtil.info("FAT 表已持久化到磁盘，共使用" + requiredBlocks + "个块");
        } catch (Exception e) {
            // 添加更详细的错误信息
            String errorMsg = "FAT 持久化失败：" + e.getMessage() + 
                              "（块大小=" + disk.getBlockSize() + "，FAT表大小=" + (totalBlocks * 4) + "，总块数=" + totalBlocks + "）";
            LogUtil.error(errorMsg, e);
            throw new DiskInitializeException(errorMsg, e);
        }
    }

    /**
     * 从磁盘读取 FAT 表（对应 saveToDisk() 的逆操作）
     * @throws DiskInitializeException 磁盘读取失败时抛出
     */
    public void loadFromDisk() throws DiskInitializeException {
        try {
            // 1. 验证总块数是否合理
            if (totalBlocks <= 0) {
                throw new IllegalStateException("无效的总块数：" + totalBlocks);
            }
            
            // 2. 计算需要读取的块数
            int blockSize = disk.getBlockSize();
            int requiredBytes = totalBlocks * 4;
            int requiredBlocks = (requiredBytes + blockSize - 1) / blockSize;
            
            // 3. 创建足够大的字节数组
            byte[] fatBytes = new byte[requiredBytes];
            
            // 4. 从磁盘读取连续块
            LogUtil.info("开始从磁盘加载FAT表，总大小=" + requiredBytes + "字节，需要" + requiredBlocks + "个块");
            
            for (int i = 0; i < requiredBlocks; i++) {
                int blockId = i + 1; // 从第1块开始读取
                int offset = i * blockSize;
                int length = Math.min(blockSize, requiredBytes - offset);
                
                byte[] blockData = disk.readBlock(blockId);
                System.arraycopy(blockData, 0, fatBytes, offset, length);
                
                LogUtil.debug("已读取FAT表块 " + blockId);
            }
            
            // 5. 将字节数组转为 int[] FAT 表
            this.fatTable = new int[totalBlocks];
            for (int i = 0; i < totalBlocks; i++) {
                // 4字节转 int（小端序）
                fatTable[i] = (fatBytes[i * 4] & 0xFF)
                        | ((fatBytes[i * 4 + 1] & 0xFF) << 8)
                        | ((fatBytes[i * 4 + 2] & 0xFF) << 16)
                        | ((fatBytes[i * 4 + 3] & 0xFF) << 24);
            }
            
            this.isInitialized = true;
            LogUtil.info("FAT 表已从磁盘加载完成，表大小=" + fatTable.length + "，总块数=" + totalBlocks);
        } catch (Exception e) {
            // 添加更详细的错误信息
            String errorMsg = "FAT 加载失败：" + e.getMessage() + 
                              "（块大小=" + disk.getBlockSize() + "，FAT表大小=" + (totalBlocks * 4) + "，总块数=" + totalBlocks + "）";
            LogUtil.error(errorMsg, e);
            throw new DiskInitializeException(errorMsg, e);
        }
    }

    // ======================== Getter 方法 ========================
    public int[] getFatTable() {
        return fatTable.clone(); // 返回克隆数组，防止外部修改FAT表
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public Disk getDisk() {
        return disk;
    }

    // ======================== Setter 方法 ========================

    /**
     * 设置块的下一个块ID（构建块链）
     * @param currentBlockId 当前块ID
     * @param nextBlockId 下一个块ID（使用END_OF_FILE表示结束）
     * @throws InvalidBlockIdException 块ID无效时抛出
     */
    public void setNextBlock(int currentBlockId, int nextBlockId) throws InvalidBlockIdException {
        // 验证当前块ID合法性
        validateBlockId(currentBlockId);

        // 验证下一个块ID合法性（允许为END_OF_FILE）
        if (nextBlockId != END_OF_FILE) {
            validateBlockId(nextBlockId);
        }

        // 不允许指向空闲块（避免块链混乱）
        if (nextBlockId != END_OF_FILE && fatTable[nextBlockId] == FREE_BLOCK) {
            throw new InvalidBlockIdException("无法指向空闲块：nextBlockId=" + nextBlockId);
        }

        // 更新FAT表
        fatTable[currentBlockId] = nextBlockId;
        LogUtil.debug("FAT更新块链：" + currentBlockId + " → " + nextBlockId);
    }

    /**
     * 将指定块标记为坏块
     * @param blockId 块ID
     */
    public void markAsBadBlock(int blockId) {
        if (blockId < 0) {
            throw new IllegalArgumentException("块ID不能为负数: " + blockId);
        }
        fatTable[blockId] = BAD_BLOCK;
    }

    /**
     * 检查块是否为坏块
     * @param blockId 块ID
     * @return 若为坏块则返回true
     */
    public boolean isBadBlock(int blockId) {
        if (blockId < 0) {
            throw new IllegalArgumentException("块ID不能为负数: " + blockId);
        }
        return (fatTable[blockId] == BAD_BLOCK);
    }

    /**
     * 将指定块标记为空闲块
     * @param blockId 块ID
     */
    public void markAsFreeBlock(int blockId) {
        if (blockId <= 0) {
            throw new IllegalArgumentException("块ID不能为负数: " + blockId);
        }
        fatTable[blockId] = FREE_BLOCK;
    }

    /**
     * 检查块是否为空闲块
     * @param blockId 块ID
     * @return 若为空闲块则返回true
     */
    public boolean isFreeBlock(int blockId) {
        if (blockId <= 0) {
            throw new IllegalArgumentException("块ID不能为负数: " + blockId);
        }
        return (fatTable[blockId] == FREE_BLOCK);
    }
}