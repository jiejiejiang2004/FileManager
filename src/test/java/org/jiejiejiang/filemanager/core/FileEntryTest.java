package org.jiejiejiang.filemanager.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileEntry 单元测试：验证元数据属性、路径生成、类型区分等核心逻辑
 */
class FileEntryTest {

    // 测试文件类型的元数据
    @Test
    void testFileEntry_File() {
        // 创建文件元数据
        FileEntry file = new FileEntry(
                "test.txt",
                FileEntry.EntryType.FILE,
                "/home",
                5  // 起始块ID
        );

        // 验证基本属性
        assertEquals("test.txt", file.getName());
        assertEquals(FileEntry.EntryType.FILE, file.getType());
        assertEquals("/home", file.getParentPath());
        assertEquals(5, file.getStartBlockId());
        assertEquals(0, file.getSize());  // 初始大小为0
        assertFalse(file.isDeleted());
        assertNotNull(file.getCreateTime());
        assertNotNull(file.getModifyTime());

        // 验证完整路径
        assertEquals("/home/test.txt", file.getFullPath());

        // 验证更新大小（文件允许更新）
        file.updateSize(1024);
        assertEquals(1024, file.getSize());
        assertTrue(file.getModifyTime().after(file.getCreateTime()));  // 修改时间晚于创建时间
    }

    // 测试目录类型的元数据
    @Test
    void testFileEntry_Directory() {
        // 创建目录元数据
        FileEntry dir = new FileEntry(
                "docs",
                FileEntry.EntryType.DIRECTORY,
                "/home",
                -1  // 目录初始无块
        );

        // 验证基本属性
        assertEquals("docs", dir.getName());
        assertEquals(FileEntry.EntryType.DIRECTORY, dir.getType());
        assertEquals("/home", dir.getParentPath());
        assertEquals(-1, dir.getStartBlockId());
        assertEquals(0, dir.getSize());  // 目录大小固定为0
        assertFalse(dir.isDeleted());

        // 验证完整路径
        assertEquals("/home/docs", dir.getFullPath());

        // 验证目录不允许更新大小（应抛出异常）
        assertThrows(UnsupportedOperationException.class, () -> {
            dir.updateSize(1024);
        });
    }

    // 测试根目录路径生成
    @Test
    void testRootDirectoryFullPath() {
        // 根目录：名称为空字符串，父路径为"/"，类型为DIRECTORY
        FileEntry root = new FileEntry(
                "",  // 空名称仅根目录允许
                FileEntry.EntryType.DIRECTORY,
                "/",  // 父路径必须是"/"
                -1
        );
        // 验证根目录完整路径（父路径"/" + 空名称 → 拼接为"/"）
        assertEquals("/", root.getFullPath());
    }

    // 测试路径拼接逻辑（父路径带/或不带/的情况）
    @Test
    void testFullPathConcatenation() {
        // 父路径不带末尾/
        FileEntry entry1 = new FileEntry("file1", FileEntry.EntryType.FILE, "/home", 1);
        assertEquals("/home/file1", entry1.getFullPath());

        // 父路径带末尾/
        FileEntry entry2 = new FileEntry("file2", FileEntry.EntryType.FILE, "/home/", 2);
        assertEquals("/home/file2", entry2.getFullPath());  // 避免重复//

        // 多级目录
        FileEntry entry3 = new FileEntry("data", FileEntry.EntryType.DIRECTORY, "/home/docs", 3);
        assertEquals("/home/docs/data", entry3.getFullPath());
    }

    // 测试标记删除功能
    @Test
    void testMarkAsDeleted() {
        // 创建一个普通文件
        FileEntry file = new FileEntry("delete.me", FileEntry.EntryType.FILE, "/", 10);

        // 初始状态应该是未删除
        assertFalse(file.isDeleted(), "初始状态应为未删除");

        // 调用标记删除方法
        file.markAsDeleted();

        // 验证删除状态（这里失败了）
        assertTrue(file.isDeleted(), "标记删除后应为已删除状态");

        // 额外验证修改时间是否更新
        assertFalse(file.getModifyTime().before(file.getCreateTime()), "修改时间应不早于创建时间");
    }

    // 测试构造函数的参数校验
    @Test
    void testConstructor_InvalidParameters() {
        // 1. 名称为空（非根目录场景）→ 应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new FileEntry("", FileEntry.EntryType.FILE, "/", 1);  // 空名称+文件类型
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new FileEntry("", FileEntry.EntryType.DIRECTORY, "/home", 1);  // 空名称+非根目录
        });

        // 2. 类型为null → 应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new FileEntry("test", null, "/", 1);
        });

        // 3. 父路径为空 → 应抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new FileEntry("test", FileEntry.EntryType.DIRECTORY, "", 1);
        });
    }

    // 测试equals和hashCode（基于完整路径）
    @Test
    void testEqualsAndHashCode() {
        FileEntry entry1 = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/home", 1);
        FileEntry entry2 = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/home", 2);  // 块ID不同但路径相同
        FileEntry entry3 = new FileEntry("other.txt", FileEntry.EntryType.FILE, "/home", 1);  // 名称不同

        assertEquals(entry1, entry2);  // 路径相同则相等
        assertNotEquals(entry1, entry3);
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }
}
