package org.jiejiejiang.filemanager.exception;

/**
 * 只读文件操作异常：当尝试修改只读文件时抛出
 */
public class ReadOnlyException extends RuntimeException {

    public ReadOnlyException() {
        super();
    }

    public ReadOnlyException(String message) {
        super(message);
    }

    public ReadOnlyException(String message, Throwable cause) {
        super(message, cause);
    }
}
