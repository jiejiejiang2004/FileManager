package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;

/**
 * 已打开文件表（OFT）测试类
 */
class OpenFileTableTest {

    private FileSystem fs;
    private Disk disk;
    private FAT fat;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时磁盘文件与配置文件
        File diskFile = tempDir.resolve("test_disk.dat").toFile();
        File configFile = tempDir.resolve("test_disk.properties").toFile();
        
        // 写入磁盘配置（64字节块，128块）
        Properties props = new Properties();
        props.setProperty("disk.block.size", "64");
        props.setProperty("disk.total.blocks", "128");
        props.setProperty("disk.file.path", diskFile.getAbsolutePath());
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Test disk config");
        }
        
        // 使用配置文件初始化磁盘
        disk = new Disk(configFile.getAbsolutePath());
        disk.initialize();
        
        // 初始化FAT
        fat = new FAT(disk);
        fat.initialize();
        
        // 初始化文件系统
        fs = new FileSystem(disk, fat);
        fs.mount();
    }

    @Test
    void testOpenAndCloseFile() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        fs.writeFile("/test.txt", "Hello World".getBytes());
        
        // 测试打开文件
        int oftIndex = fs.openFile("/test.txt", "READ");
        assertEquals(0, oftIndex); // 第一个打开的文件应该在索引0
        
        // 验证文件已打开
        assertEquals(0, fs.isFileOpen("/test.txt"));
        
        // 测试关闭文件
        fs.closeFile(oftIndex);
        assertEquals(-1, fs.isFileOpen("/test.txt")); // 文件应该已关闭
    }

    @Test
    void testOpenFileWithDifferentModes() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        
        // 测试不同的打开模式
        int readIndex = fs.openFile("/test.txt", "READ");
        fs.closeFile(readIndex);
        
        int writeIndex = fs.openFile("/test.txt", "WRITE");
        fs.closeFile(writeIndex);
        
        int readWriteIndex = fs.openFile("/test.txt", "READ_WRITE");
        fs.closeFile(readWriteIndex);
        
        // 测试无效模式
        assertThrows(FileSystemException.class, () -> {
            fs.openFile("/test.txt", "INVALID");
        });
    }

    @Test
    void testMaxOpenFiles() throws Exception {
        // 创建5个测试文件
        for (int i = 0; i < 5; i++) {
            fs.createFile("/test" + i + ".txt");
        }
        
        // 打开5个文件（达到OFT容量上限）
        int[] indices = new int[5];
        for (int i = 0; i < 5; i++) {
            indices[i] = fs.openFile("/test" + i + ".txt", "READ");
            assertEquals(i, indices[i]);
        }
        
        // 尝试打开第6个文件应该失败
        fs.createFile("/test5.txt");
        assertThrows(FileSystemException.class, () -> {
            fs.openFile("/test5.txt", "READ");
        });
        
        // 关闭一个文件后应该能打开新文件
        fs.closeFile(indices[0]);
        int newIndex = fs.openFile("/test5.txt", "READ");
        assertEquals(0, newIndex); // 应该复用索引0
    }

    @Test
    void testReadWritePointers() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        fs.writeFile("/test.txt", "Hello World!".getBytes());
        
        // 以读写模式打开文件
        int oftIndex = fs.openFile("/test.txt", "READ_WRITE");
        
        // 测试设置读指针
        fs.setReadPointer(oftIndex, 6);
        
        // 测试设置写指针
        fs.setWritePointer(oftIndex, 12);
        
        // 测试无效指针位置
        assertThrows(FileSystemException.class, () -> {
            fs.setReadPointer(oftIndex, -1);
        });
        
        assertThrows(FileSystemException.class, () -> {
            fs.setReadPointer(oftIndex, 1000); // 超出文件大小
        });
        
        fs.closeFile(oftIndex);
    }

    @Test
    void testOftReadWrite() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        String originalContent = "Hello World! This is a test file.";
        fs.writeFile("/test.txt", originalContent.getBytes());
        
        // 以读写模式打开文件
        int oftIndex = fs.openFile("/test.txt", "READ_WRITE");
        
        // 测试从头读取
        byte[] readContent = fs.readFileFromOft(oftIndex, 5);
        assertEquals("Hello", new String(readContent));
        
        // 读指针应该已移动到位置5
        // 继续读取
        readContent = fs.readFileFromOft(oftIndex, 6);
        assertEquals(" World", new String(readContent));
        
        // 测试读取到文件末尾
        readContent = fs.readFileFromOft(oftIndex, -1);
        assertEquals("! This is a test file.", new String(readContent));
        
        // 测试写入（从当前写指针位置）
        fs.setWritePointer(oftIndex, 0); // 重置写指针到开头
        fs.writeFileToOft(oftIndex, "Hi".getBytes());
        
        // 验证写入结果
        fs.setReadPointer(oftIndex, 0); // 重置读指针
        readContent = fs.readFileFromOft(oftIndex, 2);
        assertEquals("Hi", new String(readContent));
        
        fs.closeFile(oftIndex);
    }

    @Test
    void testReadWritePermissions() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        fs.writeFile("/test.txt", "Hello World".getBytes());
        
        // 以只读模式打开
        int readOnlyIndex = fs.openFile("/test.txt", "READ");
        
        // 应该能读取
        byte[] content = fs.readFileFromOft(readOnlyIndex, 5);
        assertEquals("Hello", new String(content));
        
        // 不应该能写入
        assertThrows(FileSystemException.class, () -> {
            fs.writeFileToOft(readOnlyIndex, "Hi".getBytes());
        });
        
        fs.closeFile(readOnlyIndex);
        
        // 以只写模式打开
        int writeOnlyIndex = fs.openFile("/test.txt", "WRITE");
        
        // 应该能写入
        fs.writeFileToOft(writeOnlyIndex, "Hi".getBytes());
        
        // 不应该能读取
        assertThrows(FileSystemException.class, () -> {
            fs.readFileFromOft(writeOnlyIndex, 5);
        });
        
        fs.closeFile(writeOnlyIndex);
    }

    @Test
    void testOftStatus() throws Exception {
        // 创建测试文件
        fs.createFile("/test1.txt");
        fs.createFile("/test2.txt");
        
        // 打开文件
        int index1 = fs.openFile("/test1.txt", "READ");
        int index2 = fs.openFile("/test2.txt", "WRITE");
        
        // 获取OFT状态
        String status = fs.getOftStatus();
        assertNotNull(status);
        assertTrue(status.contains("/test1.txt"));
        assertTrue(status.contains("/test2.txt"));
        assertTrue(status.contains("READ"));
        assertTrue(status.contains("WRITE"));
        
        // 关闭文件
        fs.closeFile(index1);
        fs.closeFile(index2);
    }

    @Test
    void testOpenNonExistentFile() {
        // 尝试打开不存在的文件
        assertThrows(FileSystemException.class, () -> {
            fs.openFile("/nonexistent.txt", "READ");
        });
    }

    @Test
    void testOpenDirectory() throws Exception {
        // 创建目录
        fs.createDirectory("/testdir");
        
        // 尝试打开目录应该失败
        assertThrows(FileSystemException.class, () -> {
            fs.openFile("/testdir", "READ");
        });
    }

    @Test
    void testCloseFileByPath() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        
        // 打开文件
        fs.openFile("/test.txt", "READ");
        assertEquals(0, fs.isFileOpen("/test.txt"));
        
        // 通过路径关闭文件
        fs.closeFile("/test.txt");
        assertEquals(-1, fs.isFileOpen("/test.txt"));
    }
    
    @AfterEach
    void tearDown() {
        try {
            if (fs != null) {
                fs.unmount();
            } else if (disk != null) {
                disk.close();
            }
        } catch (Exception ignored) {
        }
    }
}