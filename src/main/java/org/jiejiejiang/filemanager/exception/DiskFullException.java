package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘满异常：当磁盘没有足够空间存储数据时抛出
 */
public class DiskFullException extends DiskException {

    public DiskFullException() {
        super();
    }

    public DiskFullException(String message) {
        super(message);
    }

    public DiskFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
