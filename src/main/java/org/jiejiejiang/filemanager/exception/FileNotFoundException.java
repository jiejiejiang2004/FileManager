package org.jiejiejiang.filemanager.exception;

/**
 * 文件未找到异常：当尝试访问不存在的文件时抛出
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException() {
        super();
    }

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
