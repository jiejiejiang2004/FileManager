package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.exception.InvalidPathException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileSystem 单元测试：验证文件/目录的创建、删除、读写等核心操作
 */
class FileSystemTest {

    @TempDir
    Path tempDir;  // 临时目录，自动创建和清理

    // 测试依赖组件
    private Disk disk;
    private FAT fat;
    private FileSystem fs;

    // 磁盘配置（小容量便于测试）
    private static final int BLOCK_SIZE = 512;
    private static final int TOTAL_BLOCKS = 20;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 创建临时磁盘配置文件
        Path configFile = tempDir.resolve("fs-test.properties");
        try (var writer = new java.io.FileWriter(configFile.toFile())) {
            writer.write("disk.block.size=" + BLOCK_SIZE + "\n");
            writer.write("disk.total.blocks=" + TOTAL_BLOCKS + "\n");
            writer.write("disk.file.path=" + tempDir.resolve("fs-test.img").toString().replace("\\", "\\\\"));
        }

        // 2. 初始化磁盘和FAT
        disk = new Disk(configFile.toString());
        disk.initialize();
        fat = new FAT(disk);
        fat.initialize();

        // 3. 初始化文件系统并挂载
        fs = new FileSystem(disk, fat);
        fs.mount();
    }

    @AfterEach
    void tearDown() throws Exception {
        // 卸载文件系统，清理资源
        if (fs != null && fs.isMounted()) {
            fs.unmount();
        }
    }

    // ======================== 测试挂载/卸载 ========================

    @Test
    void testMountAndUnmount() {
        // 验证挂载状态
        assertTrue(fs.isMounted());
        assertNotNull(fs.getRootDir());
        assertEquals("/", fs.getRootDir().getFullPath());

        // 测试卸载
        assertDoesNotThrow(() -> fs.unmount());
        assertFalse(fs.isMounted());
    }

    @Test
    void testOperationBeforeMount_ShouldThrow() throws java.nio.file.FileSystemException, FileSystemException {
        // 卸载后操作应失败
        fs.unmount();
        assertThrows(FileSystemException.class, () -> fs.createFile("/test.txt"));
    }

    // ======================== 测试目录操作 ========================

    @Test
    void testCreateAndListDirectory() throws Exception {
        // 创建一级目录
        FileEntry dir1 = fs.createDirectory("/docs");
        assertEquals("/docs", dir1.getFullPath());
        assertEquals(FileEntry.EntryType.DIRECTORY, dir1.getType());

        // 创建二级目录
        FileEntry dir2 = fs.createDirectory("/docs/java");
        assertEquals("/docs/java", dir2.getFullPath());

        // 列出根目录内容（应包含docs）
        List<FileEntry> rootChildren = fs.listDirectory("/");
        assertEquals(1, rootChildren.size());
        assertEquals("/docs", rootChildren.get(0).getFullPath());

        // 列出/docs目录内容（应包含java）
        List<FileEntry> docsChildren = fs.listDirectory("/docs");
        assertEquals(1, docsChildren.size());
        assertEquals("/docs/java", docsChildren.get(0).getFullPath());
    }

    @Test
    void testCreateDirectory_ParentNotExists_ShouldThrow() {
        // 父目录不存在时创建目录应失败
        assertThrows(FileSystemException.class, () -> {
            fs.createDirectory("/a/b/c");  // /a 不存在
        });
    }

    @Test
    void testCreateExistingDirectory_ShouldThrow() throws Exception {
        fs.createDirectory("/tmp");
        // 创建已存在的目录应失败
        assertThrows(FileSystemException.class, () -> fs.createDirectory("/tmp"));
    }

    @Test
    void testDeleteEmptyDirectory() throws Exception {
        fs.createDirectory("/empty-dir");
        // 删除空目录
        assertDoesNotThrow(() -> fs.deleteDirectory("/empty-dir"));
        // 验证目录已删除（无法通过getEntry获取）
        assertNull(fs.getEntry("/empty-dir"));
    }

    @Test
    void testDeleteNonEmptyDirectory_ShouldThrow() throws Exception {
        // 创建非空目录（包含文件）
        fs.createDirectory("/non-empty");
        fs.createFile("/non-empty/file.txt");
        // 删除非空目录应失败
        assertThrows(FileSystemException.class, () -> fs.deleteDirectory("/non-empty"));
    }

    @Test
    void testDeleteRootDirectory_ShouldThrow() {
        // 根目录不允许删除
        assertThrows(FileSystemException.class, () -> fs.deleteDirectory("/"));
    }

    // ======================== 测试文件操作 ========================

    @Test
    void testCreateFileAndWriteRead() throws Exception {
        // 创建文件
        FileEntry file = fs.createFile("/test.txt");
        assertEquals("/test.txt", file.getFullPath());
        assertEquals(0, file.getSize());  // 初始大小为0

        // 写入内容
        String content = "Hello, File System!";
        fs.writeFile("/test.txt", content.getBytes());

        // 验证文件大小更新
        FileEntry updatedFile = fs.getEntry("/test.txt");
        assertEquals(content.getBytes().length, updatedFile.getSize());

        // 读取内容并验证
        byte[] readContent = fs.readFile("/test.txt");
        assertEquals(content, new String(readContent));
    }

    @Test
    void testWriteFile_LargerThanOneBlock() throws Exception {
        // 创建超过1块大小的内容（BLOCK_SIZE=512，创建600字节内容）
        byte[] largeContent = new byte[600];
        for (int i = 0; i < 600; i++) {
            largeContent[i] = (byte) (i % 256);  // 填充测试数据
        }

        // 创建文件并写入
        fs.createFile("/large.bin");
        fs.writeFile("/large.bin", largeContent);

        // 验证读取内容一致
        byte[] readContent = fs.readFile("/large.bin");
        assertArrayEquals(largeContent, readContent);
    }

    @Test
    void testDeleteFile() throws Exception {
        fs.createFile("/to-delete.txt");
        // 删除文件
        fs.deleteFile("/to-delete.txt");
        // 验证文件已删除
        assertNull(fs.getEntry("/to-delete.txt"));
        // 验证文件占用的块已释放（通过FAT检查）
        FileEntry deletedFile = fs.getEntry("/to-delete.txt");  // 已删除，返回null
        // 注：实际应通过FAT的getFatTable()验证块状态为FREE_BLOCK
    }

    @Test
    void testReadNonExistentFile_ShouldThrow() {
        assertThrows(FileSystemException.class, () -> fs.readFile("/nonexistent.txt"));
    }

    // ======================== 测试路径校验 ========================

    @Test
    void testInvalidPathOperations_ShouldThrow() {
        // 空路径
        assertThrows(FileSystemException.class, () -> fs.createFile(""));

        // 非法路径格式（如包含空格、特殊字符）
        assertThrows(FileSystemException.class, () -> fs.createFile("/invalid path.txt"));

        // 上级目录超出根目录（如/../a.txt）
        assertThrows(FileSystemException.class, () -> fs.createFile("/../escape.txt"));
    }

    // ======================== 测试文件系统边界场景 ========================

    @Test
    void testDiskFull_ShouldThrow() throws Exception {
        // 填满磁盘所有块（总块数20，0块预留，可用19块）
        for (int i = 0; i < 19; i++) {
            // 每个文件占用1块（创建19个文件）
            fs.createFile("/file" + i + ".txt");
        }

        // 第20个文件应因磁盘满而创建失败
        assertThrows(FileSystemException.class, () -> fs.createFile("/disk-full.txt"));
    }

    @Test
    void testWriteToDeletedFile_ShouldThrow() throws Exception {
        fs.createFile("/deleted.txt");
        fs.deleteFile("/deleted.txt");
        // 向已删除文件写入应失败
        assertThrows(FileSystemException.class, () -> {
            fs.writeFile("/deleted.txt", "oops".getBytes());
        });
    }
}
