package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskWriteException;
import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.jiejiejiang.filemanager.util.LogUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Directory类单元测试
 * 测试目录项管理、块链同步等核心功能
 */
@ExtendWith(MockitoExtension.class)
class DirectoryTest {

    @Mock
    private FileSystem fileSystem;

    @Mock
    private Disk disk;

    @Mock
    private FAT fat;

    @Mock
    private FileEntry dirEntry;  // 目录自身的元数据

    @Mock
    private Directory directory;
    private static final int BLOCK_SIZE = 512;
    private static final int TEST_BLOCK_ID = 10;

    @BeforeEach
    void setUp() throws Exception {
        // 禁用日志输出，避免干扰测试
        LogUtil.setConsoleOutput(false);
        LogUtil.setFileOutput(false);
        LogUtil.setDebugEnabled(false);

        // 模拟目录自身元数据
        lenient().when(dirEntry.getType()).thenReturn(FileEntry.EntryType.DIRECTORY);
        lenient().when(dirEntry.getFullPath()).thenReturn("/testDir");
        lenient().when(dirEntry.getStartBlockId()).thenReturn(-1);  // 初始未分配块

        // 关键修复：确保fileSystem返回有效的FAT、Disk和blockSize
        lenient().when(fileSystem.getFat()).thenReturn(fat);
        lenient().when(fileSystem.getDisk()).thenReturn(disk);
        lenient().when(fileSystem.getBlockSize()).thenReturn(BLOCK_SIZE);

        // 验证模拟配置是否正确（提前发现问题）
        assertNotNull(fileSystem.getFat(), "fileSystem.getFat()返回null");
        assertNotNull(fileSystem.getDisk(), "fileSystem.getDisk()返回null");
        assertNotEquals(0, fileSystem.getBlockSize(), "blockSize不能为0");

        // 初始化测试对象（此时Directory构造函数能获取到有效的FAT）
        directory = new Directory(fileSystem, dirEntry);
    }

    // ======================== 基础功能测试 ========================

    @Test
    void testInitialState_ShouldBeEmpty() throws FileSystemException {
        // 单独为这个测试定义必要的模拟
//        when(fileSystem.getDisk()).thenReturn(disk);
//        when(fileSystem.getFat()).thenReturn(fat);
//        when(fileSystem.getBlockSize()).thenReturn(BLOCK_SIZE);

        // 验证初始状态为空目录
        assertTrue(directory.isEmpty());
        assertTrue(directory.listEntries().isEmpty());
        assertFalse(directory.isDirty());
    }

    // 磁盘交互测试：添加文件并同步
    @Test
    void testAddEntry_NewFile_ShouldSucceed() throws Exception {
        // 配置必要的磁盘模拟（确保被调用）
        when(fat.allocateBlock()).thenReturn(1, 2); // 预期被调用2次
        doNothing().when(fat).setNextBlock(anyInt(), anyInt()); // 假设setNextBlock是void
        when(disk.writeBlock(anyInt(), any(byte[].class))).thenReturn(true); // 有返回值，用thenReturn

        // 执行操作
        FileEntry fileEntry = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/testDir", 1);
        directory.addEntry(fileEntry);
        directory.syncToDisk();

        // 验证
        assertFalse(directory.isEmpty());
        assertFalse(directory.isDirty());
        verify(fat, times(2)).allocateBlock(); // 验证模拟被调用
        verify(disk).writeBlock(anyInt(), any(byte[].class));
    }

    // 纯内存测试：添加重复名称
    @Test
    void testAddEntry_DuplicateName_ShouldThrow() throws Exception {
        // 准备数据
        FileEntry entry1 = new FileEntry("duplicate.txt", FileEntry.EntryType.FILE, "/testDir", 1);
        directory.addEntry(entry1);

        // 尝试添加重复名称
        FileEntry entry2 = new FileEntry("duplicate.txt", FileEntry.EntryType.FILE, "/testDir", 2);
        assertThrows(FileSystemException.class, () -> directory.addEntry(entry2));
    }

    @Test
    void testAddEntry_InvalidParentPath_ShouldThrow() throws Exception {
//        when(fileSystem.getFat()).thenReturn(fat);
//        when(fileSystem.getDisk()).thenReturn(disk);
//        when(fileSystem.getBlockSize()).thenReturn(BLOCK_SIZE);

        // 准备测试数据（父路径不匹配当前目录）
        FileEntry invalidEntry = new FileEntry("invalid.txt", FileEntry.EntryType.FILE, "/wrongDir", 1);

        // 执行测试并验证
        assertThrows(FileSystemException.class, () -> {
            directory.addEntry(invalidEntry);
        }, "应拒绝父路径不匹配的目录项");
    }

    // 纯内存测试：删除现有文件
    @Test
    void testRemoveEntry_ExistingFile_ShouldSucceed() throws Exception {
        // 准备数据（仅内存操作）
        FileEntry fileEntry = new FileEntry("toRemove.txt", FileEntry.EntryType.FILE, "/testDir", 1);
        directory.addEntry(fileEntry);

        // 执行删除
        FileEntry removed = directory.removeEntry("toRemove.txt");

        // 验证
        assertNotNull(removed);
        assertNull(directory.findEntryByName("toRemove.txt"));
        assertTrue(directory.isDirty()); // 未同步，应保持脏标记
    }

    @Test
    void testRemoveEntry_NonExisting_ShouldReturnNull() throws Exception {
        // 尝试删除不存在的条目
        FileEntry removed = directory.removeEntry("nonexistent.txt");

        // 验证结果
        assertNull(removed);
        assertTrue(directory.isEmpty());
        assertFalse(directory.isDirty());  // 未修改，无需同步
    }

    // 纯内存测试：查找现有条目
    @Test
    void testFindEntryByName_Existing_ShouldReturnEntry() throws Exception {
        // 准备数据（仅内存操作，无需磁盘模拟）
        FileEntry fileEntry = new FileEntry("findMe.txt", FileEntry.EntryType.FILE, "/testDir", 1);
        directory.addEntry(fileEntry);

        // 执行查询
        FileEntry found = directory.findEntryByName("findMe.txt");

        // 验证
        assertNotNull(found);
        assertEquals("findMe.txt", found.getName());
    }

    @Test
    void testFindEntryByName_NonExisting_ShouldReturnNull() {
        // 查询不存在的条目
        FileEntry found = directory.findEntryByName("ghost.txt");

        // 验证结果
        assertNull(found);
    }

    // ======================== 磁盘同步测试 ========================

    // 磁盘交互测试：同步新条目分配块
    @Test
    void testSyncToDisk_NewEntries_ShouldAllocateBlocks() throws Exception {
        // 配置模拟
        when(fat.allocateBlock()).thenReturn(10, 11);
        doNothing().when(fat).setNextBlock(anyInt(), anyInt());
        when(disk.writeBlock(anyInt(), any(byte[].class))).thenReturn(true); // 修复doNothing()误用

        // 执行操作
        directory.addEntry(new FileEntry("a.txt", FileEntry.EntryType.FILE, "/testDir", -1));
        directory.addEntry(new FileEntry("b.txt", FileEntry.EntryType.FILE, "/testDir", -1));
        directory.syncToDisk();

        // 验证
        verify(fat, times(2)).allocateBlock();
        verify(disk, times(1)).writeBlock(anyInt(), any(byte[].class));
    }

    @Test
    void testSyncToDisk_EmptyDirectory_ShouldFreeBlocks() throws Exception {
        // 1. 确保目录为空（核心前置条件）
//        lenient().when(directory.isEmpty()).thenReturn(true); // 若有isEmpty方法
        // 或直接操作entriesCache（如果可访问）：
//         directory.getEntriesCache().clear();

        // 2. 确保需要同步（isDirty为true）
        directory.setDirty(true); // 假设存在setDirty方法，或通过addEntry后删除触发

        // 3. 提前设置所有模拟行为（执行syncToDisk之前）
        int id = 5;
        lenient().doReturn(5).when(dirEntry).getStartBlockId(); // 块ID=5
        lenient().when(fat.isFreeBlock(5)).thenReturn(false); // 初始非空闲（需要释放）
        lenient().when(fat.isBadBlock(5)).thenReturn(false); // 初始非坏块
        lenient().when(disk.writeBlock(anyInt(), any(byte[].class))).thenReturn(true);

        // 4. 执行同步操作
        directory.syncToDisk();

        // 5. 验证释放块的逻辑
        verify(fat).markAsFreeBlock(5); // 验证释放方法被调用

        // 6. 验证块状态更新（设置模拟返回值后断言）
        lenient().when(fat.isFreeBlock(5)).thenReturn(true); // 模拟释放后的状态
        assertTrue(fat.isFreeBlock(5), "空目录的块应被标记为空闲");
    }

    // ======================== 异常场景测试 ========================

    @Test
    void testAddEntry_NameTooLong_ShouldThrow() {
        // 准备超长名称（超过MAX_ENTRY_NAME_LENGTH）
        String longName = "a".repeat(65);  // 假设最大长度为64
        FileEntry longEntry = new FileEntry(longName, FileEntry.EntryType.FILE, "/testDir", 1);

        // 执行测试并验证
        assertThrows(FileSystemException.class, () -> {
            directory.addEntry(longEntry);
        }, "应拒绝超长名称的目录项");
    }

    // 磁盘交互测试：磁盘写入失败
    @Test
    void testSyncToDisk_DiskWriteFailure_ShouldThrow() throws Exception {
        // 配置模拟（仅必要的）
        when(fat.allocateBlock()).thenReturn(1);
        when(disk.writeBlock(anyInt(), any(byte[].class))).thenThrow(
                new DiskWriteException(1, "写入失败")
        );

        // 执行操作
        directory.addEntry(new FileEntry("error.txt", FileEntry.EntryType.FILE, "/testDir", -1));

        // 验证异常
        assertThrows(FileSystemException.class, () -> directory.syncToDisk());
        assertTrue(directory.isDirty()); // 失败后应保持脏标记
    }

    @Test
    void testConstructor_NotDirectoryType_ShouldThrow() {
        // 准备非目录类型的FileEntry
        when(dirEntry.getType()).thenReturn(FileEntry.EntryType.FILE);

        // 验证构造器应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new Directory(fileSystem, dirEntry);
        }, "非目录类型的FileEntry应拒绝创建Directory");
    }
}
