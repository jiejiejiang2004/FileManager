package org.jiejiejiang.filemanager.exception;

/**
 * 文件系统通用异常类
 * 用于封装 文件系统层面的错误（如未挂载、路径非法、操作失败等）
 */
public class FileSystemException extends Exception {

    /**
     * 无参构造器
     */
    public FileSystemException() {
        super();
    }

    /**
     * 带错误信息的构造器
     * @param message 错误描述信息
     */
    public FileSystemException(String message) {
        super(message);
    }

    /**
     * 带错误信息和根源异常的构造器
     * @param message 错误描述信息
     * @param cause 根源异常（如磁盘操作异常、FAT块管理异常等）
     */
    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带根源异常的构造器
     * @param cause 根源异常
     */
    public FileSystemException(Throwable cause) {
        super(cause);
    }
}
