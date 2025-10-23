package org.jiejiejiang.filemanager.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 8字节目录项测试类
 */
public class DirectoryEntry8ByteTest {
    
    @TempDir
    Path tempDir;
    
    private Disk disk;
    private FAT fat;
    
    @BeforeEach
    void setUp() throws IOException {
        // 创建临时配置文件
        File configFile = tempDir.resolve("test_disk.properties").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("blockSize=512\n");
            writer.write("totalBlocks=100\n");
            writer.write("diskFilePath=" + tempDir.resolve("test_disk.dat").toString() + "\n");
        }
        
        // 初始化磁盘和FAT
        disk = new Disk(configFile.getAbsolutePath());
        disk.initialize(); // 初始化磁盘
        fat = new FAT(disk);
        fat.initialize(); // 初始化FAT表
    }
    
    @AfterEach
    void tearDown() {
        if (disk != null) {
            disk.close();
        }
    }
    
    @Test
    void testDirectoryEntry8ByteCreation() {
        // 测试创建8字节目录项
        DirectoryEntry8Byte entry = new DirectoryEntry8Byte("test.txt", 10, 1024, false);
        
        assertEquals("test.txt", entry.getOriginalName());
        assertEquals(10, entry.getStartBlock());
        assertEquals(1024, entry.getFileSize());
        assertFalse(entry.isDirectory());
        assertFalse(entry.isEmpty());
    }
    
    @Test
    void testDirectoryEntry8ByteSerialization() {
        // 测试序列化和反序列化
        DirectoryEntry8Byte original = new DirectoryEntry8Byte("file.dat", 25, 2048, true);
        
        byte[] data = original.toBytes();
        assertEquals(DirectoryEntry8Byte.ENTRY_SIZE, data.length);
        
        DirectoryEntry8Byte restored = new DirectoryEntry8Byte(data);
        assertEquals(original.getNameHash(), restored.getNameHash());
        assertEquals(original.getStartBlock(), restored.getStartBlock());
        assertEquals(original.getFileSize(), restored.getFileSize());
    }
    
    @Test
    void testEmptyDirectoryEntry() {
        // 测试空目录项
        DirectoryEntry8Byte empty = DirectoryEntry8Byte.createEmpty();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.getStartBlock());
        assertEquals(0, empty.getFileSize());
        
        byte[] data = empty.toBytes();
        for (byte b : data) {
            assertEquals(0, b);
        }
    }
    
    @Test
    void testDirectory8ByteOperations() {
        // 测试8字节目录管理器
        Directory8Byte directory = new Directory8Byte(disk, fat, -1);
        
        // 添加目录项
        directory.addEntry("file1.txt", 5, 100, false);
        directory.addEntry("dir1", 10, 0, true);
        directory.addEntry("file2.dat", 15, 500, false);
        
        assertEquals(3, directory.getEntryCount());
        
        // 查找目录项
        DirectoryEntry8Byte entry = directory.findEntry("file1.txt");
        assertNotNull(entry);
        assertEquals("file1.txt", entry.getOriginalName());
        assertEquals(5, entry.getStartBlock());
        assertEquals(100, entry.getFileSize());
        
        // 删除目录项
        assertTrue(directory.removeEntry("file2.dat"));
        assertEquals(2, directory.getEntryCount());
        assertNull(directory.findEntry("file2.dat"));
        
        // 删除不存在的目录项
        assertFalse(directory.removeEntry("nonexistent.txt"));
    }
    
    @Test
    void testDirectory8BytePersistence() {
        // 测试目录项持久化
        Directory8Byte directory = new Directory8Byte(disk, fat, -1);
        
        // 添加一些目录项
        directory.addEntry("persistent1.txt", 20, 256, false);
        directory.addEntry("persistent2.dir", 25, 0, true);
        
        // 同步到磁盘
        directory.syncToDisk();
        int startBlockId = directory.getStartBlockId();
        assertTrue(startBlockId > 0);
        
        // 创建新的目录管理器，从磁盘加载
        Directory8Byte loadedDirectory = new Directory8Byte(disk, fat, startBlockId);
        assertEquals(2, loadedDirectory.getEntryCount());
        
        // 验证加载的数据（注意：由于只有哈希值，无法直接验证文件名）
        // 但可以验证目录项数量和基本结构
        assertFalse(loadedDirectory.isDirty());
    }
    
    @Test
    void testLargeFileSize() {
        // 测试大文件大小（2字节限制：0-65535）
        DirectoryEntry8Byte entry = new DirectoryEntry8Byte("large.file", 100, 65535, false);
        assertEquals(65535, entry.getFileSize());
        
        byte[] data = entry.toBytes();
        DirectoryEntry8Byte restored = new DirectoryEntry8Byte(data);
        assertEquals(65535, restored.getFileSize());
    }
    
    @Test
    void testLargeBlockId() {
        // 测试大块ID（2字节限制：0-65535）
        DirectoryEntry8Byte entry = new DirectoryEntry8Byte("test.file", 65535, 1000, false);
        assertEquals(65535, entry.getStartBlock());
        
        byte[] data = entry.toBytes();
        DirectoryEntry8Byte restored = new DirectoryEntry8Byte(data);
        assertEquals(65535, restored.getStartBlock());
    }
}