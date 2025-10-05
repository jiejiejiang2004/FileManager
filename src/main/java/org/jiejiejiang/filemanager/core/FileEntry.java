package org.jiejiejiang.filemanager.core;

import org.jiejiejiang.filemanager.util.FileSizeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * 文件/目录元数据实体（类比 inode）
 * 存储文件/目录的核心属性，区分文件和目录类型
 */
public class FileEntry {
    // ======================== 文件类型枚举 ========================
    public enum EntryType {
        FILE,   // 文件
        DIRECTORY  // 目录
    }

    // ======================== 核心属性 ========================
    private final String name;          // 名称（不含路径，如 "test.txt" 或 "docs"）
    private final EntryType type;       // 类型（文件/目录）
    private final String parentPath;    // 父目录路径（如 "/home"，根目录的父路径为 "/"）
    private long size;                  // 大小（字节，目录大小固定为0或块大小）
    private int startBlockId;     // 起始块ID（文件：数据存储起始块；目录：目录项存储起始块）
    private final Date createTime;      // 创建时间
    private Date modifyTime;            // 修改时间
    private boolean isDeleted;          // 是否被删除（标记删除，避免立即释放空间）

    // ======================== 构造器 ========================
    /**
     * 创建文件/目录元数据
     * @param name 名称（不可为空或空白）
     * @param type 类型（文件/目录）
     * @param parentPath 父目录路径（不可为空，根目录父路径为 "/"）
     * @param startBlockId 起始块ID（-1 表示未分配块，如空目录）
     */
    public FileEntry(String name, EntryType type, String parentPath, int startBlockId) {
        // 1. 处理名称为空的特殊情况（仅允许根目录）
        boolean isNameEmpty = (name == null || name.trim().isEmpty());
        if (isNameEmpty) {
            // 根目录判定条件：名称为空 + 父路径为"/" + 类型为目录
            boolean isRootDir = "/".equals(parentPath.trim()) && type == EntryType.DIRECTORY;
            if (!isRootDir) {
                throw new IllegalArgumentException("文件/目录名称不能为空");
            }
        }

        // 2. 校验其他必填参数
        if (type == null) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        if (parentPath == null || parentPath.trim().isEmpty()) {
            throw new IllegalArgumentException("父目录路径不能为空");
        }

        this.name = isNameEmpty ? "" : name.trim();  // 根目录保留空名称
        this.type = type;
        this.parentPath = parentPath.trim();
        this.startBlockId = startBlockId;
        this.createTime = new Date();
        this.modifyTime = new Date();
        this.isDeleted = false;

        this.size = (type == EntryType.DIRECTORY) ? 0 : 0;
    }

    // ======================== 核心方法 ========================
    /**
     * 获取完整路径（父路径 + 名称，处理根目录特殊情况）
     * @return 完整路径（如 "/home/docs" 或 "/test.txt"）
     */
    public String getFullPath() {
        // 根目录的父路径是 "/"，名称为空（若有根目录Entry），此处简化处理非根目录场景
        if (parentPath.equals("/")) {
            return "/" + name;
        }
        // 非根目录：父路径 + "/" + 名称（避免重复分隔符，如 "/home/" + "docs" → "/home/docs"）
        return parentPath.endsWith("/") ? parentPath + name : parentPath + "/" + name;
    }

    /**
     * 标记文件/目录为已删除
     */
    public void markAsDeleted() {
        this.isDeleted = true;  // 明确设置为true
        this.modifyTime = new Date();  // 更新修改时间
    }

    /**
     * 更新文件大小（仅文件可用，目录大小不可修改）
     * @param newSize 新大小（字节）
     * @throws UnsupportedOperationException 目录调用此方法时抛出
     */
    public void updateSize(long newSize) {
        if (this.type == EntryType.DIRECTORY) {
            throw new UnsupportedOperationException("目录大小不可修改");
        }
        if (newSize < 0) {
            throw new IllegalArgumentException("文件大小不能为负数");
        }
        this.size = newSize;
        this.modifyTime = new Date();
    }

    // ======================== 辅助方法（格式化输出） ========================
    /**
     * 获取格式化后的文件大小（如 "1.50 KB"）
     * @return 人类可读的大小字符串
     */
    public String getFormattedSize() {
        return FileSizeUtil.format(size);
    }

    // ======================== 重写方法 ========================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntry fileEntry = (FileEntry) o;
        // 完整路径唯一标识一个文件/目录
        return Objects.equals(getFullPath(), fileEntry.getFullPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullPath());
    }

    @Override
    public String toString() {
        return String.format(
                "[%s] %s | 大小：%s | 创建时间：%s | 起始块：%s | 状态：%s",  // 起始块用 %s
                type == EntryType.FILE ? "文件" : "目录",
                getFullPath(),
                getFormattedSize(),
                createTime,
                startBlockId == -1 ? "未分配" : String.valueOf(startBlockId),  // 转为字符串
                isDeleted ? "已删除" : "正常"
        );
    }

    // ======================== Getter 方法（部分属性不可修改，无 Setter） ========================
    public String getName() {
        return name;
    }

    public EntryType getType() {
        return type;
    }

    public String getParentPath() {
        return parentPath;
    }

    public long getSize() {
        return size;
    }

    public int getStartBlockId() {
//        System.out.println(startBlockId);
        return startBlockId;
    }

    public Date getCreateTime() {
        return new Date(createTime.getTime()); // 返回副本，避免外部修改
    }

    public Date getModifyTime() {
        return new Date(modifyTime.getTime()); // 返回副本，避免外部修改
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setStartBlockId(int newBlockId) {
        // 可以添加验证逻辑（如块ID不能为负数，除非表示未分配）
        if (newBlockId < -1) {
            throw new IllegalArgumentException("无效的块ID: " + newBlockId + "，块ID不能小于-1");
        }
        this.startBlockId = newBlockId;
    }

    // FileEntry.java
    /**
     * 更新文件大小（仅文件可用，目录大小不可修改）
     * @param newSize 新大小（字节）
     * @throws UnsupportedOperationException 目录调用此方法时抛出
     */
    public void setSize(long newSize) {
        if (this.type == EntryType.DIRECTORY) {
            throw new UnsupportedOperationException("目录大小不可修改");
        }
        if (newSize < 0) {
            throw new IllegalArgumentException("文件大小不能为负数");
        }
        this.size = newSize;
        this.modifyTime = new Date(); // 同步更新修改时间
    }

    // FileEntry.java
    /**
     * 更新修改时间
     * @param time 新的修改时间
     */
    public void setModifyTime(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("修改时间不能为null");
        }
        // 转换LocalDateTime为Date
        this.modifyTime = Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}