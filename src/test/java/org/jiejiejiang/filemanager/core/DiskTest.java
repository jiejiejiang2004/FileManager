package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskInitializeException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Disk类的单元测试，验证磁盘初始化、块读写、格式化等功能
 */
class DiskTest {

    // 临时目录（JUnit自动管理，测试后自动删除）
    @TempDir
    Path tempDir;

    // 测试用的配置文件路径
    private String testConfigPath;
    // 测试用的磁盘实例
    private Disk disk;

    /**
     * 每个测试方法执行前：创建临时配置文件，初始化Disk实例
     */
    @BeforeEach
    void setUp() throws IOException {
        // 1. 创建临时配置文件
        Path configFile = tempDir.resolve("test-disk.properties");
        testConfigPath = configFile.toString();

        // 写入测试配置（小容量磁盘，便于测试）
        try (FileWriter writer = new FileWriter(testConfigPath)) {
            writer.write("disk.block.size=512\n");       // 块大小512字节
            writer.write("disk.total.blocks=10\n");      // 总块数10块
            String res = tempDir.resolve("test-disk.img").toString();
            String in = "disk.file.path=" + tempDir.resolve("test-disk.img").toString().replace("\\","\\\\");
            writer.write(in); // 临时磁盘文件
        }
        // 2. 初始化Disk实例（尚未调用initialize()）
        disk = new Disk(testConfigPath);
    }

    /**
     * 每个测试方法执行后：关闭磁盘，释放资源
     */
    @AfterEach
    void tearDown() {
        if (disk != null) {
            disk.close();
        }
    }

    /**
     * 测试磁盘初始化：验证初始化后状态正确，磁盘文件被创建
     */
    @Test
    void testInitialize() throws DiskInitializeException, IOException {

        // 初始化前状态
        assertFalse(disk.isInitialized());

//        setUp();
        // 执行初始化
        boolean result = disk.initialize();

        // 验证结果
        assertTrue(result);
        assertTrue(disk.isInitialized());
        assertEquals(512, disk.getBlockSize());
        assertEquals(10, disk.getTotalBlocks());
        assertEquals(512 * 10, disk.getDiskTotalSize());

        // 验证磁盘文件被创建且大小正确
        File diskFile = new File(disk.getDiskFilePath());
        assertTrue(diskFile.exists());
        assertEquals(512 * 10, diskFile.length());

        tearDown();
    }

    /**
     * 测试块写入和读取：写入数据后读取，验证内容一致
     */
    @Test
    void testWriteAndReadBlock() throws Exception {
        // 初始化磁盘
        disk.initialize();

        // 测试数据
        int blockId = 2; // 写入块2
        String testData = "Hello, Disk Block!";
        byte[] writeData = testData.getBytes(StandardCharsets.UTF_8);

        // 写入块
        boolean writeResult = disk.writeBlock(blockId, writeData);
        assertTrue(writeResult);

        // 读取块
        byte[] readData = disk.readBlock(blockId);
        assertNotNull(readData);
        assertEquals(disk.getBlockSize(), readData.length); // 确保返回完整块大小

        // 验证内容（前n字节应与写入数据一致，剩余字节为0）
        byte[] expectedData = new byte[disk.getBlockSize()];
        System.arraycopy(writeData, 0, expectedData, 0, writeData.length);
        assertArrayEquals(expectedData, readData);
    }

    /**
     * 测试块写入超出大小：数据被自动截断
     */
    @Test
    void testWriteBlock_TruncateData() throws Exception {
        disk.initialize();

        int blockId = 0;
        int blockSize = disk.getBlockSize();
        // 创建超出块大小的数据（blockSize + 100字节）
        byte[] largeData = new byte[blockSize + 100];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // 写入
        disk.writeBlock(blockId, largeData);

        // 读取并验证（仅前blockSize字节被保留）
        byte[] readData = disk.readBlock(blockId);
        byte[] expectedData = new byte[blockSize];
        System.arraycopy(largeData, 0, expectedData, 0, blockSize);
        assertArrayEquals(expectedData, readData);
    }

    /**
     * 测试块写入数据不足：自动补0
     */
    @Test
    void testWriteBlock_PadWithZero() throws Exception {
        disk.initialize();

        int blockId = 1;
        byte[] smallData = new byte[100]; // 远小于块大小512
        for (int i = 0; i < smallData.length; i++) {
            smallData[i] = (byte) (i + 1);
        }

        disk.writeBlock(blockId, smallData);

        // 读取并验证（前100字节为写入数据，剩余412字节为0）
        byte[] readData = disk.readBlock(blockId);
        assertEquals(512, readData.length);

        // 验证前100字节
        for (int i = 0; i < 100; i++) {
            assertEquals(smallData[i], readData[i]);
        }

        // 验证剩余字节为0
        for (int i = 100; i < 512; i++) {
            assertEquals(0, readData[i]);
        }
    }

    /**
     * 测试格式化磁盘：所有块数据被清空
     */
    @Test
    void testFormat() throws Exception {
        disk.initialize();

        // 先写入一些数据
        disk.writeBlock(0, "Test data".getBytes());
        disk.writeBlock(5, "Another block".getBytes());

        // 格式化
        disk.format();

        // 验证所有块数据被清空（全0）
        byte[] emptyBlock = new byte[disk.getBlockSize()];
        for (int i = 0; i < disk.getTotalBlocks(); i++) {
            assertArrayEquals(emptyBlock, disk.readBlock(i));
        }
    }

    /**
     * 测试读取未初始化的磁盘：抛出异常
     */
    @Test
    void testReadBlock_NotInitialized() {
        // 未调用initialize()，直接读取
        assertThrows(DiskInitializeException.class, () -> {
            disk.readBlock(0);
        });
    }

    /**
     * 测试写入无效块ID（负数）：抛出异常
     */
    @Test
    void testWriteBlock_NegativeBlockId() throws DiskInitializeException {
        disk.initialize();

        assertThrows(InvalidBlockIdException.class, () -> {
            disk.writeBlock(-1, new byte[10]);
        });
    }

    /**
     * 测试写入超出范围的块ID（>=总块数）：抛出异常
     */
    @Test
    void testWriteBlock_BlockIdExceedsTotal() throws DiskInitializeException {
        disk.initialize();
        int invalidBlockId = disk.getTotalBlocks(); // 总块数10，ID=10无效

        assertThrows(InvalidBlockIdException.class, () -> {
            disk.writeBlock(invalidBlockId, new byte[10]);
        });
    }

    /**
     * 测试磁盘满判断：当需要的块数超过总块数时返回true
     */
    @Test
    void testIsDiskFull() throws DiskInitializeException {
        disk.initialize();

        // 需要的块数 <= 总块数：不满
        assertFalse(disk.isDiskFull(5));
        assertFalse(disk.isDiskFull(10));

        // 需要的块数 > 总块数：满
        assertTrue(disk.isDiskFull(11));
    }

    /**
     * 测试加载无效配置文件：构造器抛出异常
     */
    @Test
    void testInvalidConfig() {
        // 创建一个无效配置文件（块大小为负数）
        Path badConfig = tempDir.resolve("bad-config.properties");
        try (FileWriter writer = new FileWriter(badConfig.toFile())) {
            writer.write("disk.block.size=-100\n"); // 无效值
            writer.write("disk.total.blocks=10\n");
        } catch (IOException e) {
            fail("创建测试配置文件失败：" + e.getMessage());
        }

        // 验证构造器抛出异常
        assertThrows(DiskInitializeException.class, () -> {
            new Disk(badConfig.toString());
        });
    }
}
