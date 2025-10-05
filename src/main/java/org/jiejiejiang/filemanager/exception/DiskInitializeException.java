package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘初始化异常：当磁盘初始化、打开或格式化失败时抛出
 */
public class DiskInitializeException extends DiskException {

    public DiskInitializeException() {
        super();
    }

    public DiskInitializeException(String message) {
        super(message);
    }

    public DiskInitializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
