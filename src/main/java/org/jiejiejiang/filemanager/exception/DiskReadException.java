package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘读取异常
 * 用于表示读取磁盘块时发生的错误（如IO错误、块ID无效、数据损坏等）
 */
public class DiskReadException extends DiskException {

    /**
     * 带错误信息的构造器
     * @param message 错误描述信息
     */
    public DiskReadException(String message) {
        super(message);
    }

    /**
     * 带错误信息和根源异常的构造器
     * @param message 错误描述信息
     * @param cause 根源异常（如IOException等）
     */
    public DiskReadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带块ID和错误信息的构造器（常用）
     * @param blockId 发生错误的块ID
     * @param message 错误描述信息
     */
    public DiskReadException(int blockId, String message) {
        super("块ID: " + blockId + "，" + message);
    }

    /**
     * 带块ID、错误信息和根源异常的构造器
     * @param blockId 发生错误的块ID
     * @param message 错误描述信息
     * @param cause 根源异常
     */
    public DiskReadException(int blockId, String message, Throwable cause) {
        super("块ID: " + blockId + "，" + message, cause);
    }
}
