package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘操作基础异常
 * 所有磁盘相关异常（读、写、块管理等）的父类，用于统一捕获磁盘操作错误
 */
public class DiskException extends RuntimeException {

    /**
     * 无参构造器
     */
    public DiskException() {
        super();
    }

    /**
     * 带错误信息的构造器
     * @param message 错误描述信息
     */
    public DiskException(String message) {
        super(message);
    }

    /**
     * 带错误信息和根源异常的构造器
     * @param message 错误描述信息
     * @param cause 根源异常（如IOException等底层错误）
     */
    public DiskException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带根源异常的构造器
     * @param cause 根源异常
     */
    public DiskException(Throwable cause) {
        super(cause);
    }
}
