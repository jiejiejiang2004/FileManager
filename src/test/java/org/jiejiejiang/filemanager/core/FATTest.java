package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskFullException;
import org.jiejiejiang.filemanager.exception.DiskInitializeException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FAT 类的单元测试，覆盖初始化、块分配、块释放、链式扩展等核心功能
 */
class FATTest {

    // 临时目录（自动创建和清理）
    @TempDir
    Path tempDir;

    // 测试依赖的组件
    private Disk disk;
    private FAT fat;

    // 磁盘配置参数（小容量便于测试）
    private static final int BLOCK_SIZE = 512;   // 块大小 512 字节
    private static final int TOTAL_BLOCKS = 10;  // 总块数10（0块预留，实际可用9块）

    @BeforeEach
    void setUp() throws Exception {
        // 1. 创建临时配置文件
        Path configFile = tempDir.resolve("test-fat.properties");
        try (var writer = new java.io.FileWriter(configFile.toFile())) {
            writer.write("disk.block.size=" + BLOCK_SIZE + "\n");
            writer.write("disk.total.blocks=" + TOTAL_BLOCKS + "\n");
            writer.write("disk.file.path=" + tempDir.resolve("test-fat.img").toString().replace("\\", "\\\\"));
        }

        // 2. 初始化磁盘
        disk = new Disk(configFile.toString());
        disk.initialize();

        // 3. 初始化FAT
        fat = new FAT(disk);
        fat.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        // 关闭磁盘资源
        if (disk != null && disk.isInitialized()) {
            disk.close();
        }
    }

    // ======================== 测试初始化功能 ========================

    @Test
    void testInitialize_Success() {
        // 验证初始化状态
        assertTrue(fat.isInitialized());
        // 验证总块数正确
        assertEquals(TOTAL_BLOCKS, fat.getTotalBlocks());
        // 验证FAT表大小正确
        assertEquals(TOTAL_BLOCKS, fat.getFatTable().length);
        // 验证0块被标记为坏块（预留）
        assertEquals(FAT.BAD_BLOCK, fat.getFatTable()[0]);
        // 验证其他块初始为空闲
        for (int i = 1; i < TOTAL_BLOCKS; i++) {
            assertEquals(FAT.FREE_BLOCK, fat.getFatTable()[i]);
        }
    }

    @Test
    void testInitialize_WithUninitializedDisk_ShouldThrow() throws Exception {
        // 1. 先创建一个合法的配置文件（复用 setUp 中的配置逻辑，确保 Disk 能加载配置）
        Path validConfigFile = tempDir.resolve("valid-for-uninit.properties");
        try (var writer = new java.io.FileWriter(validConfigFile.toFile())) {
            writer.write("disk.block.size=512\n");
            writer.write("disk.total.blocks=10\n");
            writer.write("disk.file.path=" + tempDir.resolve("uninit-disk.img").toString().replace("\\", "\\\\"));
        }

        // 2. 创建 Disk 实例（此时配置已加载，但未调用 initialize() → 磁盘未初始化）
        Disk uninitializedDisk = new Disk(validConfigFile.toString());
        // 关键：不调用 uninitializedDisk.initialize()，模拟“未初始化磁盘”

        // 3. 用未初始化的磁盘创建 FAT → 预期抛出 DiskInitializeException
        assertThrows(DiskInitializeException.class, () -> {
            new FAT(uninitializedDisk);  // FAT 构造器会检查磁盘是否初始化，此处应抛出异常
        });
    }

    // ======================== 测试块分配功能 ========================

    @Test
    void testAllocateBlock_FirstFreeBlock() throws Exception {
        // 首次分配应得到1号块（0块预留）
        int blockId = fat.allocateBlock();
        assertEquals(1, blockId);
        // 验证块状态为文件结束（无后续块）
        assertEquals(FAT.END_OF_FILE, fat.getFatTable()[blockId]);
    }

    @Test
    void testAllocateBlock_MultipleBlocks() throws Exception {
        // 连续分配3块
        int block1 = fat.allocateBlock();
        int block2 = fat.allocateBlock();
        int block3 = fat.allocateBlock();

        // 验证分配的块ID递增
        assertEquals(1, block1);
        assertEquals(2, block2);
        assertEquals(3, block3);

        // 验证这些块已被标记为占用
        assertEquals(FAT.END_OF_FILE, fat.getFatTable()[block1]);
        assertEquals(FAT.END_OF_FILE, fat.getFatTable()[block2]);
        assertEquals(FAT.END_OF_FILE, fat.getFatTable()[block3]);
    }

    @Test
    void testAllocateBlock_DiskFull_ShouldThrow() throws Exception {
        // 填满所有可用块（1-9号，共9块）
        for (int i = 1; i < TOTAL_BLOCKS; i++) {
            fat.allocateBlock();
        }

        // 再次分配应抛出磁盘满异常
        assertThrows(DiskFullException.class, () -> fat.allocateBlock());
    }

    // ======================== 测试块扩展功能 ========================

    @Test
    void testAllocateNextBlock_Success() throws Exception {
        // 1. 分配起始块
        int startBlock = fat.allocateBlock();  // 1号块

        // 2. 扩展第一个后续块
        int nextBlock1 = fat.allocateNextBlock(startBlock);  // 2号块
        assertEquals(2, nextBlock1);
        // 验证起始块的下一块指向2号块
        assertEquals(nextBlock1, fat.getNextBlock(startBlock));
        // 验证2号块的下一块为文件结束
        assertEquals(FAT.END_OF_FILE, fat.getNextBlock(nextBlock1));

        // 3. 继续扩展第二个后续块
        int nextBlock2 = fat.allocateNextBlock(nextBlock1);  // 3号块
        assertEquals(3, nextBlock2);
        // 验证2号块的下一块指向3号块
        assertEquals(nextBlock2, fat.getNextBlock(nextBlock1));
        // 验证3号块的下一块为文件结束
        assertEquals(FAT.END_OF_FILE, fat.getNextBlock(nextBlock2));
    }

    @Test
    void testAllocateNextBlock_WithInvalidCurrentBlock_ShouldThrow() throws Exception {
        // 测试用例：使用未分配的块扩展
        assertThrows(InvalidBlockIdException.class, () -> {
            fat.allocateNextBlock(1);  // 1号块未分配，应失败
        });

        // 测试用例：使用0号块（预留块）扩展
        assertThrows(InvalidBlockIdException.class, () -> {
            fat.allocateNextBlock(0);  // 0号是坏块，不允许扩展
        });

        // 测试用例：使用超出范围的块ID
        assertThrows(InvalidBlockIdException.class, () -> {
            fat.allocateNextBlock(TOTAL_BLOCKS);  // 块ID超出范围
        });
    }

    // ======================== 测试块释放功能 ========================

    @Test
    void testFreeBlocks_SingleBlock() throws Exception {
        // 1. 分配一个块
        int blockId = fat.allocateBlock();  // 1号块

        // 2. 释放该块
        fat.freeBlocks(blockId);

        // 3. 验证块已被标记为空闲
        assertEquals(FAT.FREE_BLOCK, fat.getFatTable()[blockId]);
    }

    @Test
    void testFreeBlocks_ChainOfBlocks() throws Exception {
        // 1. 创建块链：1 → 2 → 3 → END
        int block1 = fat.allocateBlock();
        int block2 = fat.allocateNextBlock(block1);
        int block3 = fat.allocateNextBlock(block2);

        // 2. 从起始块释放整个链
        fat.freeBlocks(block1);

        // 3. 验证所有块都被标记为空闲
        assertEquals(FAT.FREE_BLOCK, fat.getFatTable()[block1]);
        assertEquals(FAT.FREE_BLOCK, fat.getFatTable()[block2]);
        assertEquals(FAT.FREE_BLOCK, fat.getFatTable()[block3]);
    }

    @Test
    void testFreeBlocks_WithInvalidStartBlock_ShouldThrow() throws Exception {
        // 测试用例：释放未分配的块
        assertThrows(InvalidBlockIdException.class, () -> {
            fat.freeBlocks(1);  // 1号块未分配
        });

        // 测试用例：释放0号块（预留块）
        assertThrows(InvalidBlockIdException.class, () -> {
            fat.freeBlocks(0);  // 0号是坏块
        });

        // 测试用例：释放超出范围的块
        assertThrows(InvalidBlockIdException.class, () -> {
            fat.freeBlocks(TOTAL_BLOCKS + 1);  // 块ID无效
        });
    }

    // ======================== 测试文件块数计算 ========================

    @Test
    void testGetFileBlockCount() throws Exception {
        // 1. 创建块链：1 → 2 → 3 → END（共3块）
        int block1 = fat.allocateBlock();
        int block2 = fat.allocateNextBlock(block1);
        int block3 = fat.allocateNextBlock(block2);

        // 2. 计算文件块数
        int count = fat.getFileBlockCount(block1);
        assertEquals(3, count);
    }

    @Test
    void testGetFileBlockCount_SingleBlock() throws Exception {
        // 1. 分配单个块
        int block1 = fat.allocateBlock();

        // 2. 计算文件块数（应为1）
        int count = fat.getFileBlockCount(block1);
        assertEquals(1, count);
    }

    // ======================== 测试持久化功能 ========================

    @Test
    void testSaveAndLoadFromDisk() throws Exception {
        // 1. 先分配一些块，修改FAT状态
        int block1 = fat.allocateBlock();
        int block2 = fat.allocateNextBlock(block1);

        // 2. 保存FAT到磁盘
        fat.saveToDisk();

        // 3. 创建新的FAT实例，从磁盘加载
        FAT loadedFat = new FAT(disk);
        loadedFat.loadFromDisk();

        // 4. 验证加载后的FAT状态与原状态一致
        assertArrayEquals(fat.getFatTable(), loadedFat.getFatTable());
        assertEquals(fat.getTotalBlocks(), loadedFat.getTotalBlocks());
        assertEquals(fat.isInitialized(), loadedFat.isInitialized());
    }

    // ======================== 测试异常场景 ========================

    @Test
    void testOperationBeforeInitialize_ShouldThrow() {
        // 创建未初始化的FAT
        FAT uninitializedFat = null;
        try {
            uninitializedFat = new FAT(disk);  // 此时未调用initialize()
        } catch (DiskInitializeException e) {
            fail("创建FAT失败：" + e.getMessage());
        }

        // 未初始化时调用任何操作都应失败
        FAT finalUninitializedFat = uninitializedFat;
        assertThrows(InvalidBlockIdException.class, () -> finalUninitializedFat.allocateBlock());
        assertThrows(InvalidBlockIdException.class, () -> finalUninitializedFat.getNextBlock(1));
        assertThrows(InvalidBlockIdException.class, () -> finalUninitializedFat.freeBlocks(1));
    }
}
