package org.jiejiejiang.filemanager.exception;

/**
 * 无效块ID异常：当访问的磁盘块ID超出有效范围时抛出
 */
public class InvalidBlockIdException extends RuntimeException {

    public InvalidBlockIdException() {
        super();
    }

    public InvalidBlockIdException(String message) {
        super(message);
    }

    public InvalidBlockIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
