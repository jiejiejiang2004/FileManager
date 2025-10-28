# 测试类技术文档 / Test Classes Technical Documentation

## 中文概述 / Chinese Overview

测试类系统为FileManager项目提供全面的单元测试和集成测试覆盖。测试框架基于JUnit 5，使用Mockito进行模拟测试，确保各个组件的功能正确性、边界条件处理和异常情况的健壮性。

## English Overview

The Test Classes System provides comprehensive unit and integration test coverage for the FileManager project. The testing framework is based on JUnit 5, uses Mockito for mock testing, and ensures functional correctness, boundary condition handling, and robustness in exception scenarios for all components.

---

## 测试架构设计 / Test Architecture Design

### 测试层次结构 / Test Hierarchy

```
测试系统 (Test System)
├── 核心组件测试 (Core Component Tests)
│   ├── DiskTest                    # 磁盘操作测试
│   ├── FATTest                     # 文件分配表测试
│   ├── FileSystemTest              # 文件系统测试
│   ├── DirectoryTest               # 目录管理测试
│   ├── FileEntryTest               # 文件条目测试
│   ├── OpenFileTableTest           # 打开文件表测试
│   └── DirectoryEntry8ByteTest     # 8字节目录项测试
├── 测试工具和框架 (Test Tools & Framework)
│   ├── JUnit 5                     # 测试框架
│   ├── Mockito                     # 模拟框架
│   ├── @TempDir                    # 临时目录管理
│   └── AssertJ                     # 断言库
└── 测试策略 (Test Strategies)
    ├── 单元测试 (Unit Tests)        # 组件独立测试
    ├── 集成测试 (Integration Tests) # 组件协作测试
    ├── 边界测试 (Boundary Tests)    # 边界条件测试
    └── 异常测试 (Exception Tests)   # 异常处理测试
```

### 测试设计原则 / Test Design Principles

1. **隔离性** (Isolation)：每个测试独立运行，不依赖其他测试
2. **可重复性** (Repeatability)：测试结果可重复且一致
3. **全面性** (Comprehensiveness)：覆盖正常流程、边界条件和异常情况
4. **可维护性** (Maintainability)：测试代码清晰易懂，易于维护

---

## 核心测试类详解 / Core Test Classes Details

### 1. DiskTest - 磁盘操作测试

#### 类声明 / Class Declaration
```java
package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskInitializeException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Disk类的单元测试，验证磁盘初始化、块读写、格式化等功能
 */
class DiskTest {
    @TempDir Path tempDir;
    private String testConfigPath;
    private Disk disk;
}
```

#### 核心特性 / Core Features

1. **配置文件测试** (Configuration File Testing)
   - 测试有效配置文件的加载
   - 测试无效配置参数的处理
   - 测试配置文件不存在的情况

2. **磁盘初始化测试** (Disk Initialization Testing)
   - 测试磁盘的正确初始化
   - 测试重复初始化的处理
   - 测试初始化失败的异常处理

3. **块读写测试** (Block Read/Write Testing)
   - 测试单块读写操作
   - 测试多块连续读写
   - 测试无效块ID的异常处理

4. **边界条件测试** (Boundary Condition Testing)
   - 测试块大小边界
   - 测试块数量边界
   - 测试文件路径边界

#### 测试方法示例 / Test Method Examples

```java
/**
 * 磁盘初始化测试示例
 */
public class DiskTestExample {
    
    @Test
    void testDiskInitialization_ValidConfig_ShouldSucceed() throws Exception {
        // 创建有效配置文件
        Properties config = new Properties();
        config.setProperty("disk.block.size", "512");
        config.setProperty("disk.total.blocks", "1024");
        config.setProperty("disk.file.path", tempDir.resolve("test.img").toString());
        
        String configPath = createConfigFile(config);
        
        // 测试磁盘初始化
        Disk disk = new Disk(configPath);
        assertDoesNotThrow(() -> disk.initialize());
        
        // 验证磁盘属性
        assertEquals(512, disk.getBlockSize());
        assertEquals(1024, disk.getTotalBlocks());
        assertTrue(disk.isInitialized());
    }
    
    @Test
    void testBlockReadWrite_ValidBlockId_ShouldSucceed() throws Exception {
        // 准备测试数据
        byte[] testData = "Hello, World!".getBytes();
        int blockId = 1;
        
        // 写入数据
        assertDoesNotThrow(() -> disk.writeBlock(blockId, testData));
        
        // 读取数据
        byte[] readData = disk.readBlock(blockId);
        
        // 验证数据一致性
        assertArrayEquals(testData, Arrays.copyOf(readData, testData.length));
    }
    
    @Test
    void testBlockReadWrite_InvalidBlockId_ShouldThrow() {
        // 测试无效块ID
        assertThrows(InvalidBlockIdException.class, () -> {
            disk.readBlock(-1); // 负数块ID
        });
        
        assertThrows(InvalidBlockIdException.class, () -> {
            disk.readBlock(9999); // 超出范围的块ID
        });
    }
}
```

### 2. FATTest - 文件分配表测试

#### 类声明 / Class Declaration
```java
/**
 * FAT 类的单元测试，覆盖初始化、块分配、块释放、链式扩展等核心功能
 */
class FATTest {
    @TempDir Path tempDir;
    private Disk disk;
    private FAT fat;
    private static final int BLOCK_SIZE = 512;
    private static final int TOTAL_BLOCKS = 10;
}
```

#### 核心特性 / Core Features

1. **FAT初始化测试** (FAT Initialization Testing)
   - 测试FAT表的正确初始化
   - 测试未初始化磁盘的异常处理
   - 测试FAT表的持久化和恢复

2. **块分配测试** (Block Allocation Testing)
   - 测试单块分配
   - 测试连续块分配
   - 测试磁盘满时的异常处理

3. **块释放测试** (Block Deallocation Testing)
   - 测试单块释放
   - 测试链式块释放
   - 测试重复释放的处理

4. **链式操作测试** (Chain Operation Testing)
   - 测试块链的创建和扩展
   - 测试块链的遍历
   - 测试块链的完整性验证

#### 测试方法示例 / Test Method Examples

```java
/**
 * FAT测试示例
 */
public class FATTestExample {
    
    @Test
    void testAllocateBlock_AvailableBlocks_ShouldReturnValidBlockId() throws Exception {
        // 分配第一个块
        int blockId = fat.allocateBlock();
        
        // 验证分配结果
        assertTrue(blockId >= 1 && blockId < TOTAL_BLOCKS);
        assertEquals(FAT.END_OF_CHAIN, fat.getNextBlock(blockId));
        assertFalse(fat.isFreeBlock(blockId));
    }
    
    @Test
    void testAllocateBlock_DiskFull_ShouldThrow() throws Exception {
        // 分配所有可用块
        for (int i = 1; i < TOTAL_BLOCKS; i++) {
            fat.allocateBlock();
        }
        
        // 尝试分配额外的块应该抛出异常
        assertThrows(DiskFullException.class, () -> {
            fat.allocateBlock();
        });
    }
    
    @Test
    void testDeallocateChain_ValidChain_ShouldFreeAllBlocks() throws Exception {
        // 创建块链：1 -> 2 -> 3 -> END
        int block1 = fat.allocateBlock();
        int block2 = fat.allocateBlock();
        int block3 = fat.allocateBlock();
        
        fat.setNextBlock(block1, block2);
        fat.setNextBlock(block2, block3);
        fat.setNextBlock(block3, FAT.END_OF_CHAIN);
        
        // 释放整个链
        fat.deallocateChain(block1);
        
        // 验证所有块都被释放
        assertTrue(fat.isFreeBlock(block1));
        assertTrue(fat.isFreeBlock(block2));
        assertTrue(fat.isFreeBlock(block3));
    }
}
```

### 3. FileSystemTest - 文件系统测试

#### 类声明 / Class Declaration
```java
/**
 * FileSystem 单元测试：验证文件/目录的创建、删除、读写等核心操作
 */
class FileSystemTest {
    @TempDir Path tempDir;
    private Disk disk;
    private FAT fat;
    private FileSystem fs;
    private static final int BLOCK_SIZE = 512;
    private static final int TOTAL_BLOCKS = 20;
}
```

#### 核心特性 / Core Features

1. **文件操作测试** (File Operation Testing)
   - 测试文件的创建、读取、写入、删除
   - 测试文件内容的完整性
   - 测试文件大小的正确计算

2. **目录操作测试** (Directory Operation Testing)
   - 测试目录的创建和删除
   - 测试目录内容的列举
   - 测试嵌套目录的处理

3. **路径处理测试** (Path Handling Testing)
   - 测试绝对路径和相对路径
   - 测试路径解析和验证
   - 测试无效路径的异常处理

4. **文件系统挂载测试** (File System Mount Testing)
   - 测试文件系统的挂载和卸载
   - 测试挂载状态的验证
   - 测试重复挂载的处理

#### 测试方法示例 / Test Method Examples

```java
/**
 * 文件系统测试示例
 */
public class FileSystemTestExample {
    
    @Test
    void testCreateAndReadFile_ValidContent_ShouldSucceed() throws Exception {
        String filePath = "/test.txt";
        String content = "Hello, FileSystem!";
        
        // 创建文件
        FileEntry file = fs.createFile(filePath);
        assertNotNull(file);
        assertEquals(filePath, file.getFullPath());
        assertEquals(FileEntry.EntryType.FILE, file.getType());
        
        // 写入内容
        fs.writeFile(filePath, content.getBytes());
        
        // 读取内容
        byte[] readData = fs.readFile(filePath);
        String readContent = new String(readData);
        
        // 验证内容一致性
        assertEquals(content, readContent);
    }
    
    @Test
    void testCreateDirectory_ValidPath_ShouldSucceed() throws Exception {
        String dirPath = "/documents";
        
        // 创建目录
        FileEntry dir = fs.createDirectory(dirPath);
        
        // 验证目录属性
        assertNotNull(dir);
        assertEquals(dirPath, dir.getFullPath());
        assertEquals(FileEntry.EntryType.DIRECTORY, dir.getType());
        
        // 验证目录在父目录中可见
        List<FileEntry> rootChildren = fs.listDirectory("/");
        assertTrue(rootChildren.stream()
                  .anyMatch(entry -> entry.getFullPath().equals(dirPath)));
    }
    
    @Test
    void testDeleteFile_ExistingFile_ShouldSucceed() throws Exception {
        String filePath = "/temp.txt";
        
        // 创建文件
        fs.createFile(filePath);
        assertTrue(fs.exists(filePath));
        
        // 删除文件
        fs.deleteFile(filePath);
        assertFalse(fs.exists(filePath));
        
        // 验证文件不在父目录中
        List<FileEntry> rootChildren = fs.listDirectory("/");
        assertFalse(rootChildren.stream()
                   .anyMatch(entry -> entry.getFullPath().equals(filePath)));
    }
}
```

### 4. DirectoryTest - 目录管理测试

#### 类声明 / Class Declaration
```java
/**
 * Directory类单元测试
 * 测试目录项管理、块链同步等核心功能
 */
@ExtendWith(MockitoExtension.class)
class DirectoryTest {
    @Mock private FileSystem fileSystem;
    @Mock private Disk disk;
    @Mock private FAT fat;
    @Mock private FileEntry dirEntry;
    private Directory directory;
    private static final int BLOCK_SIZE = 512;
}
```

#### 核心特性 / Core Features

1. **目录项管理测试** (Directory Entry Management Testing)
   - 测试目录项的添加和删除
   - 测试目录项的查找和更新
   - 测试目录项的序列化和反序列化

2. **块链同步测试** (Block Chain Synchronization Testing)
   - 测试目录内容与磁盘的同步
   - 测试块链的扩展和收缩
   - 测试同步失败的异常处理

3. **模拟测试** (Mock Testing)
   - 使用Mockito模拟依赖组件
   - 测试组件间的交互
   - 验证方法调用的正确性

#### 测试方法示例 / Test Method Examples

```java
/**
 * 目录测试示例
 */
public class DirectoryTestExample {
    
    @Test
    void testAddEntry_ValidEntry_ShouldSucceed() throws Exception {
        // 创建测试文件条目
        FileEntry fileEntry = new FileEntry(
            "test.txt", 
            FileEntry.EntryType.FILE, 
            "/testDir", 
            1
        );
        
        // 添加条目到目录
        assertDoesNotThrow(() -> directory.addEntry(fileEntry));
        
        // 验证条目已添加
        FileEntry found = directory.findEntryByName("test.txt");
        assertNotNull(found);
        assertEquals("test.txt", found.getName());
        assertEquals(FileEntry.EntryType.FILE, found.getType());
    }
    
    @Test
    void testRemoveEntry_ExistingEntry_ShouldSucceed() throws Exception {
        // 添加测试条目
        FileEntry fileEntry = new FileEntry(
            "remove.txt", 
            FileEntry.EntryType.FILE, 
            "/testDir", 
            2
        );
        directory.addEntry(fileEntry);
        
        // 删除条目
        boolean removed = directory.removeEntry("remove.txt");
        
        // 验证删除成功
        assertTrue(removed);
        assertNull(directory.findEntryByName("remove.txt"));
    }
    
    @Test
    void testSyncToDisk_ModifiedDirectory_ShouldPersist() throws Exception {
        // 模拟磁盘写入操作
        when(disk.writeBlock(anyInt(), any(byte[].class)))
            .thenReturn(true);
        
        // 添加条目并同步
        FileEntry entry = new FileEntry("sync.txt", FileEntry.EntryType.FILE, "/testDir", 3);
        directory.addEntry(entry);
        
        // 执行同步
        assertDoesNotThrow(() -> directory.syncToDisk());
        
        // 验证磁盘写入被调用
        verify(disk, atLeastOnce()).writeBlock(anyInt(), any(byte[].class));
    }
}
```

### 5. FileEntryTest - 文件条目测试

#### 类声明 / Class Declaration
```java
/**
 * FileEntry 单元测试：验证元数据属性、路径生成、类型区分等核心逻辑
 */
class FileEntryTest {
    // 测试方法直接在类中定义，无需特殊设置
}
```

#### 核心特性 / Core Features

1. **元数据测试** (Metadata Testing)
   - 测试文件和目录的基本属性
   - 测试时间戳的正确设置
   - 测试UUID的唯一性

2. **路径处理测试** (Path Processing Testing)
   - 测试完整路径的生成
   - 测试路径组件的解析
   - 测试路径验证逻辑

3. **类型区分测试** (Type Distinction Testing)
   - 测试文件和目录的不同行为
   - 测试类型安全的操作限制
   - 测试类型转换的异常处理

#### 测试方法示例 / Test Method Examples

```java
/**
 * 文件条目测试示例
 */
public class FileEntryTestExample {
    
    @Test
    void testFileEntry_FileType_ShouldHaveCorrectProperties() {
        // 创建文件条目
        FileEntry file = new FileEntry(
            "document.txt",
            FileEntry.EntryType.FILE,
            "/home/user",
            5
        );
        
        // 验证基本属性
        assertEquals("document.txt", file.getName());
        assertEquals(FileEntry.EntryType.FILE, file.getType());
        assertEquals("/home/user", file.getParentPath());
        assertEquals("/home/user/document.txt", file.getFullPath());
        assertEquals(5, file.getStartBlockId());
        assertEquals(0, file.getSize());
        assertFalse(file.isDeleted());
        
        // 验证时间戳
        assertNotNull(file.getCreateTime());
        assertNotNull(file.getModifyTime());
        assertTrue(file.getModifyTime().compareTo(file.getCreateTime()) >= 0);
    }
    
    @Test
    void testFileEntry_DirectoryType_ShouldRestrictSizeUpdate() {
        // 创建目录条目
        FileEntry directory = new FileEntry(
            "documents",
            FileEntry.EntryType.DIRECTORY,
            "/home/user",
            -1
        );
        
        // 验证目录属性
        assertEquals(FileEntry.EntryType.DIRECTORY, directory.getType());
        assertEquals(0, directory.getSize());
        
        // 验证目录不允许更新大小
        assertThrows(UnsupportedOperationException.class, () -> {
            directory.updateSize(1024);
        });
    }
    
    @Test
    void testFileEntry_UpdateSize_ShouldUpdateModifyTime() throws InterruptedException {
        // 创建文件条目
        FileEntry file = new FileEntry(
            "test.txt",
            FileEntry.EntryType.FILE,
            "/tmp",
            1
        );
        
        Date originalModifyTime = file.getModifyTime();
        
        // 等待一毫秒确保时间差异
        Thread.sleep(1);
        
        // 更新文件大小
        file.updateSize(2048);
        
        // 验证大小和修改时间都已更新
        assertEquals(2048, file.getSize());
        assertTrue(file.getModifyTime().after(originalModifyTime));
    }
}
```

### 6. OpenFileTableTest - 打开文件表测试

#### 类声明 / Class Declaration
```java
/**
 * 已打开文件表（OFT）测试类
 */
class OpenFileTableTest {
    private FileSystem fs;
    private Disk disk;
    private FAT fat;
    @TempDir Path tempDir;
}
```

#### 核心特性 / Core Features

1. **文件打开关闭测试** (File Open/Close Testing)
   - 测试文件的打开和关闭
   - 测试不同访问模式的处理
   - 测试最大打开文件数限制

2. **读写指针测试** (Read/Write Pointer Testing)
   - 测试读写指针的移动
   - 测试指针边界检查
   - 测试指针同步机制

3. **权限控制测试** (Permission Control Testing)
   - 测试读写权限的验证
   - 测试权限违规的异常处理
   - 测试权限模式的切换

#### 测试方法示例 / Test Method Examples

```java
/**
 * 打开文件表测试示例
 */
public class OpenFileTableTestExample {
    
    @Test
    void testOpenFile_ValidPath_ShouldReturnIndex() throws Exception {
        // 创建测试文件
        fs.createFile("/test.txt");
        fs.writeFile("/test.txt", "Hello World".getBytes());
        
        // 打开文件
        int oftIndex = fs.openFile("/test.txt", "READ");
        
        // 验证返回有效索引
        assertTrue(oftIndex >= 0);
        assertEquals(0, fs.isFileOpen("/test.txt"));
        
        // 关闭文件
        fs.closeFile(oftIndex);
        assertEquals(-1, fs.isFileOpen("/test.txt"));
    }
    
    @Test
    void testOpenFile_DifferentModes_ShouldSucceed() throws Exception {
        fs.createFile("/test.txt");
        
        // 测试不同访问模式
        int readIndex = fs.openFile("/test.txt", "READ");
        fs.closeFile(readIndex);
        
        int writeIndex = fs.openFile("/test.txt", "WRITE");
        fs.closeFile(writeIndex);
        
        int readWriteIndex = fs.openFile("/test.txt", "READ_WRITE");
        fs.closeFile(readWriteIndex);
        
        // 测试无效模式
        assertThrows(FileSystemException.class, () -> {
            fs.openFile("/test.txt", "INVALID_MODE");
        });
    }
    
    @Test
    void testMaxOpenFiles_ExceedLimit_ShouldThrow() throws Exception {
        // 创建多个测试文件
        for (int i = 0; i < 10; i++) {
            fs.createFile("/test" + i + ".txt");
        }
        
        // 打开文件直到达到限制
        List<Integer> openFiles = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // 假设最大打开文件数为5
            int index = fs.openFile("/test" + i + ".txt", "READ");
            openFiles.add(index);
        }
        
        // 尝试打开超出限制的文件
        assertThrows(FileSystemException.class, () -> {
            fs.openFile("/test5.txt", "READ");
        });
        
        // 清理：关闭所有打开的文件
        for (int index : openFiles) {
            fs.closeFile(index);
        }
    }
}
```

### 7. DirectoryEntry8ByteTest - 8字节目录项测试

#### 类声明 / Class Declaration
```java
/**
 * 8字节目录项测试类
 */
public class DirectoryEntry8ByteTest {
    @TempDir Path tempDir;
    private Disk disk;
    private FAT fat;
}
```

#### 核心特性 / Core Features

1. **紧凑存储测试** (Compact Storage Testing)
   - 测试8字节目录项的创建
   - 测试数据的序列化和反序列化
   - 测试存储空间的优化

2. **哈希冲突测试** (Hash Collision Testing)
   - 测试文件名哈希的计算
   - 测试哈希冲突的处理
   - 测试哈希表的性能

3. **大文件支持测试** (Large File Support Testing)
   - 测试大文件大小的存储
   - 测试大块ID的处理
   - 测试数据范围的边界

#### 测试方法示例 / Test Method Examples

```java
/**
 * 8字节目录项测试示例
 */
public class DirectoryEntry8ByteTestExample {
    
    @Test
    void testDirectoryEntry8Byte_Creation_ShouldStoreCorrectData() {
        // 创建8字节目录项
        DirectoryEntry8Byte entry = new DirectoryEntry8Byte(
            "document.pdf", 
            100, 
            2048576, // 2MB
            false
        );
        
        // 验证基本属性
        assertEquals("document.pdf", entry.getOriginalName());
        assertEquals(100, entry.getStartBlock());
        assertEquals(2048576, entry.getFileSize());
        assertFalse(entry.isDirectory());
        assertFalse(entry.isEmpty());
    }
    
    @Test
    void testDirectoryEntry8Byte_Serialization_ShouldPreserveData() {
        // 创建原始条目
        DirectoryEntry8Byte original = new DirectoryEntry8Byte(
            "test.dat", 
            50, 
            1024000, 
            true
        );
        
        // 序列化
        byte[] data = original.toBytes();
        assertEquals(DirectoryEntry8Byte.ENTRY_SIZE, data.length);
        
        // 反序列化
        DirectoryEntry8Byte restored = new DirectoryEntry8Byte(data);
        
        // 验证数据一致性
        assertEquals(original.getNameHash(), restored.getNameHash());
        assertEquals(original.getStartBlock(), restored.getStartBlock());
        assertEquals(original.getFileSize(), restored.getFileSize());
        assertEquals(original.isDirectory(), restored.isDirectory());
    }
    
    @Test
    void testDirectory8Byte_Operations_ShouldManageEntries() {
        // 创建8字节目录管理器
        Directory8Byte directory = new Directory8Byte(disk, fat, -1);
        
        // 添加多个条目
        directory.addEntry("file1.txt", 10, 1000, false);
        directory.addEntry("subdir", 20, 0, true);
        directory.addEntry("file2.dat", 30, 5000, false);
        
        // 验证条目数量
        assertEquals(3, directory.getEntryCount());
        
        // 验证条目查找
        DirectoryEntry8Byte found = directory.findEntry("file1.txt");
        assertNotNull(found);
        assertEquals(10, found.getStartBlock());
        assertEquals(1000, found.getFileSize());
    }
}
```

---

## 测试工具和框架 / Testing Tools and Frameworks

### 1. JUnit 5 测试框架 / JUnit 5 Testing Framework

#### 核心注解 / Core Annotations

```java
/**
 * JUnit 5 注解使用示例
 */
public class JUnit5AnnotationExample {
    
    @BeforeEach
    void setUp() {
        // 每个测试方法执行前的初始化
    }
    
    @AfterEach
    void tearDown() {
        // 每个测试方法执行后的清理
    }
    
    @BeforeAll
    static void setUpClass() {
        // 所有测试方法执行前的一次性初始化
    }
    
    @AfterAll
    static void tearDownClass() {
        // 所有测试方法执行后的一次性清理
    }
    
    @Test
    void testBasicFunctionality() {
        // 基本测试方法
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"file1.txt", "file2.dat", "document.pdf"})
    void testWithParameters(String fileName) {
        // 参数化测试
    }
    
    @RepeatedTest(5)
    void testRepeated() {
        // 重复测试
    }
    
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Test
    void testWithTimeout() {
        // 超时测试
    }
    
    @Disabled("暂时禁用此测试")
    @Test
    void testDisabled() {
        // 禁用的测试
    }
}
```

#### 断言方法 / Assertion Methods

```java
/**
 * JUnit 5 断言示例
 */
public class JUnit5AssertionExample {
    
    @Test
    void testAssertions() {
        // 基本断言
        assertEquals(expected, actual);
        assertNotEquals(unexpected, actual);
        assertTrue(condition);
        assertFalse(condition);
        assertNull(object);
        assertNotNull(object);
        
        // 数组断言
        assertArrayEquals(expectedArray, actualArray);
        
        // 异常断言
        assertThrows(ExpectedException.class, () -> {
            // 可能抛出异常的代码
        });
        
        assertDoesNotThrow(() -> {
            // 不应该抛出异常的代码
        });
        
        // 超时断言
        assertTimeout(Duration.ofSeconds(2), () -> {
            // 应该在2秒内完成的代码
        });
        
        // 组合断言
        assertAll("文件属性验证",
            () -> assertEquals("test.txt", file.getName()),
            () -> assertEquals(1024, file.getSize()),
            () -> assertTrue(file.exists())
        );
    }
}
```

### 2. Mockito 模拟框架 / Mockito Mocking Framework

#### 基本模拟 / Basic Mocking

```java
/**
 * Mockito 模拟示例
 */
@ExtendWith(MockitoExtension.class)
public class MockitoExample {
    
    @Mock
    private FileSystem fileSystem;
    
    @Mock
    private Disk disk;
    
    @InjectMocks
    private Directory directory;
    
    @Test
    void testWithMocks() {
        // 设置模拟行为
        when(fileSystem.getBlockSize()).thenReturn(512);
        when(disk.readBlock(1)).thenReturn(new byte[512]);
        
        // 执行测试
        int blockSize = fileSystem.getBlockSize();
        byte[] data = disk.readBlock(1);
        
        // 验证结果
        assertEquals(512, blockSize);
        assertNotNull(data);
        assertEquals(512, data.length);
        
        // 验证方法调用
        verify(fileSystem).getBlockSize();
        verify(disk).readBlock(1);
        verify(disk, never()).writeBlock(anyInt(), any());
    }
    
    @Test
    void testArgumentMatchers() {
        // 参数匹配器
        when(disk.readBlock(anyInt())).thenReturn(new byte[512]);
        when(fileSystem.createFile(startsWith("/tmp"))).thenReturn(mock(FileEntry.class));
        
        // 参数捕获
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        fileSystem.createFile("/tmp/test.txt");
        verify(fileSystem).createFile(pathCaptor.capture());
        assertEquals("/tmp/test.txt", pathCaptor.getValue());
    }
}
```

### 3. 临时目录管理 / Temporary Directory Management

#### @TempDir 使用 / @TempDir Usage

```java
/**
 * 临时目录使用示例
 */
public class TempDirExample {
    
    @TempDir
    Path tempDir; // JUnit 自动创建和清理
    
    @Test
    void testWithTempDir() throws IOException {
        // 创建临时文件
        Path configFile = tempDir.resolve("test.properties");
        Files.write(configFile, Arrays.asList(
            "disk.block.size=512",
            "disk.total.blocks=100"
        ));
        
        // 验证文件存在
        assertTrue(Files.exists(configFile));
        
        // 读取文件内容
        List<String> lines = Files.readAllLines(configFile);
        assertEquals(2, lines.size());
        
        // 测试结束后，tempDir 会被自动删除
    }
    
    @Test
    void testCreateTestDisk() throws Exception {
        // 创建测试磁盘文件
        Path diskFile = tempDir.resolve("test.img");
        
        // 创建配置文件
        Properties config = new Properties();
        config.setProperty("disk.block.size", "512");
        config.setProperty("disk.total.blocks", "50");
        config.setProperty("disk.file.path", diskFile.toString());
        
        Path configFile = tempDir.resolve("config.properties");
        try (FileOutputStream out = Files.newOutputStream(configFile)) {
            config.store(out, "Test configuration");
        }
        
        // 使用配置初始化磁盘
        Disk disk = new Disk(configFile.toString());
        disk.initialize();
        
        // 验证磁盘文件已创建
        assertTrue(Files.exists(diskFile));
        assertTrue(Files.size(diskFile) > 0);
    }
}
```

---

## 测试策略和最佳实践 / Testing Strategies and Best Practices

### 1. 测试分层策略 / Test Layering Strategy

```java
/**
 * 测试分层示例
 */
public class TestLayeringExample {
    
    // 单元测试：测试单个组件的功能
    @Test
    void unitTest_DiskBlockRead_ShouldReturnCorrectData() {
        // 只测试 Disk.readBlock() 方法
        byte[] expected = "test data".getBytes();
        disk.writeBlock(1, expected);
        
        byte[] actual = disk.readBlock(1);
        assertArrayEquals(expected, actual);
    }
    
    // 集成测试：测试组件间的协作
    @Test
    void integrationTest_FileSystemCreateFile_ShouldUpdateFATAndDisk() {
        // 测试 FileSystem、FAT、Disk 的协作
        FileEntry file = fs.createFile("/test.txt");
        fs.writeFile("/test.txt", "content".getBytes());
        
        // 验证 FAT 中有分配的块
        assertTrue(fat.isAllocated(file.getStartBlockId()));
        
        // 验证磁盘中有数据
        byte[] diskData = disk.readBlock(file.getStartBlockId());
        assertNotNull(diskData);
    }
    
    // 端到端测试：测试完整的用户场景
    @Test
    void endToEndTest_CreateEditDeleteFile_ShouldWorkCorrectly() {
        // 模拟完整的用户操作流程
        String filePath = "/document.txt";
        String originalContent = "Original content";
        String updatedContent = "Updated content";
        
        // 创建文件
        fs.createFile(filePath);
        fs.writeFile(filePath, originalContent.getBytes());
        
        // 编辑文件
        fs.writeFile(filePath, updatedContent.getBytes());
        byte[] readContent = fs.readFile(filePath);
        assertEquals(updatedContent, new String(readContent));
        
        // 删除文件
        fs.deleteFile(filePath);
        assertFalse(fs.exists(filePath));
    }
}
```

### 2. 边界条件测试 / Boundary Condition Testing

```java
/**
 * 边界条件测试示例
 */
public class BoundaryTestingExample {
    
    @Test
    void testBlockSize_BoundaryValues() {
        // 测试最小块大小
        assertThrows(DiskInitializeException.class, () -> {
            createDiskWithBlockSize(0);
        });
        
        // 测试非2的幂块大小
        assertThrows(DiskInitializeException.class, () -> {
            createDiskWithBlockSize(100);
        });
        
        // 测试有效的最小块大小
        assertDoesNotThrow(() -> {
            createDiskWithBlockSize(64);
        });
        
        // 测试有效的最大块大小
        assertDoesNotThrow(() -> {
            createDiskWithBlockSize(4096);
        });
    }
    
    @Test
    void testFileSize_BoundaryValues() throws Exception {
        String filePath = "/test.txt";
        fs.createFile(filePath);
        
        // 测试空文件
        assertEquals(0, fs.getFileSize(filePath));
        
        // 测试单字节文件
        fs.writeFile(filePath, new byte[]{1});
        assertEquals(1, fs.getFileSize(filePath));
        
        // 测试块大小边界
        int blockSize = fs.getBlockSize();
        fs.writeFile(filePath, new byte[blockSize - 1]);
        assertEquals(blockSize - 1, fs.getFileSize(filePath));
        
        fs.writeFile(filePath, new byte[blockSize]);
        assertEquals(blockSize, fs.getFileSize(filePath));
        
        fs.writeFile(filePath, new byte[blockSize + 1]);
        assertEquals(blockSize + 1, fs.getFileSize(filePath));
    }
    
    @Test
    void testPathLength_BoundaryValues() {
        // 测试空路径
        assertThrows(InvalidPathException.class, () -> {
            fs.createFile("");
        });
        
        // 测试最长有效路径
        String longPath = "/" + "a".repeat(254); // 255字符限制
        assertDoesNotThrow(() -> {
            fs.createFile(longPath);
        });
        
        // 测试超长路径
        String tooLongPath = "/" + "a".repeat(255);
        assertThrows(InvalidPathException.class, () -> {
            fs.createFile(tooLongPath);
        });
    }
}
```

### 3. 异常处理测试 / Exception Handling Testing

```java
/**
 * 异常处理测试示例
 */
public class ExceptionHandlingTestExample {
    
    @Test
    void testDiskFull_ShouldThrowDiskFullException() throws Exception {
        // 填满磁盘
        int totalBlocks = disk.getTotalBlocks();
        for (int i = 1; i < totalBlocks; i++) {
            fat.allocateBlock();
        }
        
        // 尝试分配额外的块
        assertThrows(DiskFullException.class, () -> {
            fat.allocateBlock();
        });
    }
    
    @Test
    void testInvalidBlockId_ShouldThrowInvalidBlockIdException() {
        // 测试负数块ID
        assertThrows(InvalidBlockIdException.class, () -> {
            disk.readBlock(-1);
        });
        
        // 测试超出范围的块ID
        assertThrows(InvalidBlockIdException.class, () -> {
            disk.readBlock(9999);
        });
        
        // 测试零块ID（通常保留）
        assertThrows(InvalidBlockIdException.class, () -> {
            disk.readBlock(0);
        });
    }
    
    @Test
    void testFileNotFound_ShouldThrowFileNotFoundException() {
        // 测试读取不存在的文件
        assertThrows(org.jiejiejiang.filemanager.exception.FileNotFoundException.class, () -> {
            fs.readFile("/nonexistent.txt");
        });
        
        // 测试删除不存在的文件
        assertThrows(org.jiejiejiang.filemanager.exception.FileNotFoundException.class, () -> {
            fs.deleteFile("/nonexistent.txt");
        });
    }
    
    @Test
    void testExceptionMessage_ShouldBeDescriptive() {
        try {
            fs.readFile("/missing.txt");
            fail("应该抛出异常");
        } catch (org.jiejiejiang.filemanager.exception.FileNotFoundException e) {
            // 验证异常消息包含有用信息
            assertTrue(e.getMessage().contains("/missing.txt"));
            assertTrue(e.getMessage().contains("不存在") || e.getMessage().contains("not found"));
        }
    }
}
```

### 4. 性能测试 / Performance Testing

```java
/**
 * 性能测试示例
 */
public class PerformanceTestExample {
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testLargeFileCreation_ShouldCompleteInTime() throws Exception {
        // 创建大文件应该在合理时间内完成
        String filePath = "/large.dat";
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        Arrays.fill(largeData, (byte) 'A');
        
        long startTime = System.currentTimeMillis();
        
        fs.createFile(filePath);
        fs.writeFile(filePath, largeData);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证性能要求
        assertTrue(duration < 1000, "大文件创建耗时过长: " + duration + "ms");
    }
    
    @Test
    void testManySmallFiles_ShouldMaintainPerformance() throws Exception {
        int fileCount = 100;
        long startTime = System.currentTimeMillis();
        
        // 创建多个小文件
        for (int i = 0; i < fileCount; i++) {
            String filePath = "/file" + i + ".txt";
            fs.createFile(filePath);
            fs.writeFile(filePath, ("Content " + i).getBytes());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证平均性能
        double avgTimePerFile = (double) duration / fileCount;
        assertTrue(avgTimePerFile < 50, "平均文件创建时间过长: " + avgTimePerFile + "ms");
    }
    
    @RepeatedTest(10)
    void testRepeatedOperations_ShouldBeConsistent() throws Exception {
        // 重复操作的性能应该一致
        String filePath = "/repeat.txt";
        byte[] data = "test data".getBytes();
        
        long startTime = System.nanoTime();
        
        fs.createFile(filePath);
        fs.writeFile(filePath, data);
        byte[] readData = fs.readFile(filePath);
        fs.deleteFile(filePath);
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        // 记录性能数据用于分析
        System.out.println("操作耗时: " + duration / 1_000_000.0 + "ms");
        
        // 验证数据正确性
        assertArrayEquals(data, readData);
    }
}
```

---

## 测试配置和环境 / Test Configuration and Environment

### 1. Maven 测试配置 / Maven Test Configuration

```xml
<!-- pom.xml 测试配置 -->
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.17.0</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.17.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Surefire 插件用于运行测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Failsafe 插件用于集成测试 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.0.0</version>
            <configuration>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. 测试资源配置 / Test Resource Configuration

```
src/test/resources/
├── test-configs/
│   ├── small-disk.properties      # 小容量磁盘配置
│   ├── large-disk.properties      # 大容量磁盘配置
│   └── invalid-disk.properties    # 无效配置（用于异常测试）
├── test-data/
│   ├── sample.txt                 # 测试文件样本
│   ├── large-file.dat            # 大文件样本
│   └── binary-data.bin           # 二进制数据样本
└── logback-test.xml              # 测试日志配置
```

### 3. 测试工具类 / Test Utility Classes

```java
/**
 * 测试工具类
 */
public class TestUtils {
    
    /**
     * 创建测试配置文件
     */
    public static String createTestConfig(Path tempDir, int blockSize, int totalBlocks) 
            throws IOException {
        Path configFile = tempDir.resolve("test.properties");
        Path diskFile = tempDir.resolve("test.img");
        
        Properties config = new Properties();
        config.setProperty("disk.block.size", String.valueOf(blockSize));
        config.setProperty("disk.total.blocks", String.valueOf(totalBlocks));
        config.setProperty("disk.file.path", diskFile.toString());
        
        try (FileOutputStream out = Files.newOutputStream(configFile)) {
            config.store(out, "Test configuration");
        }
        
        return configFile.toString();
    }
    
    /**
     * 创建测试数据
     */
    public static byte[] createTestData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }
    
    /**
     * 验证文件系统状态
     */
    public static void assertFileSystemState(FileSystem fs, String expectedRoot) 
            throws Exception {
        assertTrue(fs.isMounted());
        assertTrue(fs.exists("/"));
        
        List<FileEntry> rootEntries = fs.listDirectory("/");
        assertNotNull(rootEntries);
    }
    
    /**
     * 清理测试环境
     */
    public static void cleanupFileSystem(FileSystem fs) {
        if (fs != null && fs.isMounted()) {
            try {
                fs.unmount();
            } catch (Exception e) {
                // 忽略清理异常
            }
        }
    }
}
```

---

## 测试覆盖率和质量 / Test Coverage and Quality

### 1. 覆盖率目标 / Coverage Goals

| 组件 | 行覆盖率目标 | 分支覆盖率目标 | 方法覆盖率目标 |
|------|-------------|---------------|---------------|
| 核心组件 (Core) | ≥ 90% | ≥ 85% | ≥ 95% |
| 工具类 (Util) | ≥ 85% | ≥ 80% | ≥ 90% |
| 异常类 (Exception) | ≥ 80% | ≥ 75% | ≥ 85% |
| GUI组件 (GUI) | ≥ 70% | ≥ 65% | ≥ 75% |

### 2. 质量指标 / Quality Metrics

```java
/**
 * 测试质量指标示例
 */
public class TestQualityMetrics {
    
    // 测试方法命名规范
    @Test
    void should_ThrowException_When_InvalidBlockIdProvided() {
        // Given: 无效的块ID
        int invalidBlockId = -1;
        
        // When & Then: 应该抛出异常
        assertThrows(InvalidBlockIdException.class, () -> {
            disk.readBlock(invalidBlockId);
        });
    }
    
    // 测试数据驱动
    @ParameterizedTest
    @CsvSource({
        "1, true",
        "512, true", 
        "1024, true",
        "0, false",
        "100, false",
        "-1, false"
    })
    void should_ValidateBlockSize_Correctly(int blockSize, boolean expected) {
        assertEquals(expected, isValidBlockSize(blockSize));
    }
    
    // 测试断言清晰性
    @Test
    void should_CreateFile_WithCorrectAttributes() {
        // Given
        String fileName = "test.txt";
        String parentPath = "/home/user";
        
        // When
        FileEntry file = new FileEntry(fileName, FileEntry.EntryType.FILE, parentPath, 1);
        
        // Then
        assertAll("文件属性验证",
            () -> assertEquals(fileName, file.getName(), "文件名应该正确"),
            () -> assertEquals(FileEntry.EntryType.FILE, file.getType(), "类型应该是文件"),
            () -> assertEquals(parentPath + "/" + fileName, file.getFullPath(), "完整路径应该正确"),
            () -> assertFalse(file.isDeleted(), "新文件不应该被标记为删除")
        );
    }
}
```

### 3. 持续集成测试 / Continuous Integration Testing

```yaml
# .github/workflows/test.yml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run unit tests
      run: mvn test
    
    - name: Run integration tests
      run: mvn failsafe:integration-test
    
    - name: Generate test report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
```

---

## 扩展建议 / Extension Recommendations

### 1. 测试自动化 / Test Automation

1. **GUI自动化测试** (GUI Automation Testing)
   - 使用TestFX进行JavaFX界面测试
   - 实现用户操作流程的自动化验证

2. **性能基准测试** (Performance Benchmark Testing)
   - 使用JMH进行微基准测试
   - 建立性能回归检测机制

3. **压力测试** (Stress Testing)
   - 实现高并发场景测试
   - 验证系统在极限条件下的稳定性

### 2. 测试工具增强 / Test Tool Enhancement

1. **自定义断言** (Custom Assertions)
   - 开发领域特定的断言方法
   - 提高测试代码的可读性

2. **测试数据生成器** (Test Data Generators)
   - 实现随机测试数据生成
   - 支持属性基础测试(Property-based Testing)

3. **测试报告增强** (Enhanced Test Reporting)
   - 生成详细的测试覆盖率报告
   - 实现测试结果的可视化展示

---

## 依赖关系 / Dependencies

### 外部依赖 / External Dependencies

- `org.junit.jupiter.*`: JUnit 5测试框架
- `org.mockito.*`: Mockito模拟框架
- `org.assertj.core.*`: AssertJ断言库
- `java.nio.file.*`: 文件系统操作
- `java.util.concurrent.*`: 并发测试工具

### 内部依赖 / Internal Dependencies

- `org.jiejiejiang.filemanager.core.*`: 被测试的核心组件
- `org.jiejiejiang.filemanager.exception.*`: 异常类测试
- `org.jiejiejiang.filemanager.util.*`: 工具类测试

### 测试资源依赖 / Test Resource Dependencies

- 测试配置文件：`test-configs/*.properties`
- 测试数据文件：`test-data/*`
- 临时目录：JUnit `@TempDir` 管理

---

**文档版本 / Document Version:** 1.0  
**最后更新 / Last Updated:** 2024-12-19  
**维护者 / Maintainer:** FileManager Development Team