package org.jiejiejiang.filemanager.exception;

/**
 * 无效路径异常：当路径格式错误或无法解析时抛出
 */
public class InvalidPathException extends RuntimeException {

    public InvalidPathException() {
        super();
    }

    public InvalidPathException(String message) {
        super(message);
    }

    public InvalidPathException(String message, Throwable cause) {
        super(message, cause);
    }
}
