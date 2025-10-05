package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.exception.DiskInitializeException;
import org.jiejiejiang.filemanager.exception.InvalidBlockIdException;
import org.jiejiejiang.filemanager.exception.DiskWriteException;
import org.jiejiejiang.filemanager.util.PathUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLOutput;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 磁盘模拟类，负责管理磁盘块的读写、初始化和格式化
 * 模拟物理磁盘的行为，所有文件系统数据最终都存储在该类管理的磁盘块中
 */
public class Disk {
    // 磁盘块大小（字节），默认1024 Bytes
    private int blockSize = 1024;
    // 磁盘总块数，默认1024 Blocks
    private int totalBlocks = 1024;
    // 模拟磁盘的物理文件路径（操作系统中的真实文件）
    private String diskFilePath;
    // 随机访问文件对象，用于实现磁盘块的随机读写
    private RandomAccessFile diskFile;
    // 磁盘是否已初始化
    private boolean isInitialized;
    // 磁盘总容量（字节）= blockSize * totalBlocks
    private final long diskTotalSize;

    /**
     * 构造器：通过配置文件初始化磁盘参数
     * @param configPath 磁盘配置文件路径（如config/disk.properties）
     * @throws DiskInitializeException 初始化失败时抛出
     */
    public Disk(String configPath) throws DiskInitializeException {
        // 加载配置文件并解析参数
        loadConfig(configPath);
        // 计算磁盘总容量
        this.diskTotalSize = (long) blockSize * totalBlocks;
        // 标记为未初始化（需要调用initialize()方法完成初始化）
        this.isInitialized = false;
        this.diskFilePath = this.diskFilePath.replace("\\", "\\\\");
    }

    /**
     * 初始化磁盘：创建/打开物理文件，准备读写操作
     * @return 初始化成功返回true，失败返回false
     * @throws DiskInitializeException 初始化失败时抛出
     */
    public boolean initialize() throws DiskInitializeException {
        if (isInitialized) {
            return true; // 已初始化，直接返回
        }

        try {
            // 将相对路径转为绝对路径，避免运行目录变化导致的问题
            String absolutePath = PathUtil.getAbsolutePath(diskFilePath);
            File diskFile = new File(absolutePath);

            // 如果父目录不存在，创建目录
            File parentDir = diskFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 以"读写"模式打开文件（不存在则创建）
            this.diskFile = new RandomAccessFile(diskFile, "rw");

            // 如果是新文件，扩展到指定大小（totalBlocks * blockSize）
            if (this.diskFile.length() < diskTotalSize) {
                this.diskFile.setLength(diskTotalSize);
            }

            // 标记为已初始化
            isInitialized = true;
            return true;
        } catch (IOException e) {
            throw new DiskInitializeException("磁盘初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取指定块的数据
     * @param blockId 块ID（0 <= blockId < totalBlocks）
     * @return 块数据（字节数组，长度为blockSize）
     * @throws InvalidBlockIdException 块ID无效时抛出
     * @throws DiskInitializeException 磁盘未初始化时抛出
     */
    public byte[] readBlock(int blockId) throws InvalidBlockIdException, DiskInitializeException {
        // 校验磁盘是否已初始化
        checkInitialized();
        // 校验块ID合法性
        if (!validateBlockId(blockId)) {
            throw new InvalidBlockIdException("无效的块ID：" + blockId + "，总块数：" + totalBlocks);
        }

        try {
            // 计算块在文件中的偏移量
            long offset = calculateOffset(blockId);
            // 移动文件指针到指定位置
            diskFile.seek(offset);
            // 读取块数据
            byte[] data = new byte[blockSize];
            int bytesRead = diskFile.read(data);

            // 如果实际读取字节数不足（文件损坏等情况），用0填充
            if (bytesRead < blockSize) {
                for (int i = bytesRead; i < blockSize; i++) {
                    data[i] = 0;
                }
            }
            return data;
        } catch (IOException e) {
            throw new DiskInitializeException("读取块失败：" + blockId, e);
        }
    }

    /**
     * 向指定块写入数据
     * @param blockId 块ID（0 <= blockId < totalBlocks）
     * @param data 要写入的数据（长度超过blockSize会被截断，不足则补0）
     * @return 写入成功返回true
     * @throws InvalidBlockIdException 块ID无效时抛出
     * @throws DiskWriteException 写入失败时抛出
     * @throws DiskInitializeException 磁盘未初始化时抛出
     */
    public boolean writeBlock(int blockId, byte[] data)
            throws InvalidBlockIdException, DiskWriteException, DiskInitializeException {
        // 校验磁盘是否已初始化
        checkInitialized();
        // 校验块ID合法性
        if (!validateBlockId(blockId)) {
            throw new InvalidBlockIdException("无效的块ID：" + blockId + "，总块数：" + totalBlocks);
        }

        try {
            // 计算块在文件中的偏移量
            long offset = calculateOffset(blockId);
            // 移动文件指针到指定位置
            diskFile.seek(offset);

            // 处理数据长度（确保写入的是完整块）
            byte[] writeData = new byte[blockSize];
            if (data != null) {
                // 复制数据（长度取最小值，避免数组越界）
                int copyLength = Math.min(data.length, blockSize);
                System.arraycopy(data, 0, writeData, 0, copyLength);
            }
            // 写入数据
            diskFile.write(writeData);
            return true;
        } catch (IOException e) {
            throw new DiskWriteException("写入块失败：" + blockId, e);
        }
    }

    /**
     * 格式化磁盘：清空所有数据，重置为初始状态
     * @throws DiskInitializeException 磁盘未初始化时抛出
     */
    public void format() throws DiskInitializeException {
        checkInitialized();
        try {
            // 截断文件为0，再扩展到原始大小（清空所有数据）
            diskFile.setLength(0);
            diskFile.setLength(diskTotalSize);
        } catch (IOException e) {
            throw new DiskInitializeException("格式化磁盘失败", e);
        }
    }

    /**
     * 关闭磁盘，释放资源
     */
    public void close() {
        if (diskFile != null) {
            try {
                diskFile.close();
            } catch (IOException e) {
                System.err.println("关闭磁盘文件失败：" + e.getMessage());
            } finally {
                diskFile = null;
                isInitialized = false;
            }
        }
    }

    /**
     * 校验磁盘是否有足够的空闲块（仅从总容量判断，实际需结合FAT）
     * @param requiredBlocks 需要的块数
     * @return 足够返回true，否则返回false
     */
    public boolean isDiskFull(int requiredBlocks) {
        return requiredBlocks > totalBlocks;
    }

    /**
     * 加载配置文件中的磁盘参数
     * @param configPath 配置文件路径
     * @throws DiskInitializeException 配置文件加载失败时抛出
     */
    private void loadConfig(String configPath) throws DiskInitializeException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(configPath)) {
            props.load(in);


            // 读取配置参数，使用默认值兜底
            this.blockSize = Integer.parseInt(props.getProperty("disk.block.size", "1024"));
            this.totalBlocks = Integer.parseInt(props.getProperty("disk.total.blocks", "1024"));
            this.diskFilePath = props.getProperty("disk.file.path", "./data/disk.img");

            // 校验配置参数合法性
            if (blockSize <= 0 || totalBlocks <= 0) {
                throw new DiskInitializeException("无效的磁盘配置：块大小或总块数必须为正数");
            }
        } catch (IOException e) {
            throw new DiskInitializeException("无法加载配置文件：" + configPath, e);
        } catch (NumberFormatException e) {
            throw new DiskInitializeException("配置文件格式错误：" + e.getMessage(), e);
        }
    }

    /**
     * 计算块ID对应的文件偏移量
     * @param blockId 块ID
     * @return 偏移量（字节）
     */
    private long calculateOffset(int blockId) {
        return (long) blockId * blockSize;
    }

    /**
     * 校验块ID是否合法（0 <= blockId < totalBlocks）
     * @param blockId 块ID
     * @return 合法返回true，否则返回false
     */
    private boolean validateBlockId(int blockId) {
        return blockId >= 0 && blockId < totalBlocks;
    }

    /**
     * 检查磁盘是否已初始化，未初始化则抛出异常
     * @throws DiskInitializeException 磁盘未初始化时抛出
     */
    private void checkInitialized() throws DiskInitializeException {
        if (!isInitialized) {
            throw new DiskInitializeException("磁盘未初始化，请先调用initialize()方法");
        }
    }

    /**
     * 析构方法：确保资源释放（防止忘记调用close()）
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close(); // 确保关闭文件流
    }

    // Getter方法
    public int getBlockSize() {
        return blockSize;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public long getDiskTotalSize() {
        return diskTotalSize;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public String getDiskFilePath() {
        return diskFilePath;
    }
}
