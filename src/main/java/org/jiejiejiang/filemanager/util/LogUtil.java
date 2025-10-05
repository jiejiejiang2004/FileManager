package org.jiejiejiang.filemanager.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志工具类，提供简单的日志记录功能
 * 支持控制台输出和文件记录，区分不同日志级别
 */
public class LogUtil {

    // 扩展日志级别，增加DEBUG
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    // 日志文件路径
    private static String logFilePath = "./logs/filemanager.log";
    // 日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // 是否输出到控制台
    private static boolean consoleOutput = true;
    // 是否写入文件
    private static boolean fileOutput = true;
    // 调试日志开关
    private static boolean debugEnabled = true;

    /**
     * 设置日志文件路径
     * @param path 日志文件路径
     */
    public static void setLogFilePath(String path) {
        logFilePath = path;
    }

    /**
     * 设置是否输出到控制台
     * @param enable  true-启用控制台输出，false-禁用
     */
    public static void setConsoleOutput(boolean enable) {
        consoleOutput = enable;
    }

    /**
     * 设置是否写入日志文件
     * @param enable  true-启用文件写入，false-禁用
     */
    public static void setFileOutput(boolean enable) {
        fileOutput = enable;
    }

    /**
     * 设置是否启用调试日志
     * @param enabled true-启用DEBUG级别日志，false-禁用
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * 记录DEBUG级别日志
     * @param message 日志消息
     */
    public static void debug(String message) {
        if (debugEnabled) {
            log(Level.DEBUG, message, null);
        }
    }

    /**
     * 记录INFO级别日志
     * @param message 日志消息
     */
    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    /**
     * 记录WARN级别日志
     * @param message 日志消息
     */
    public static void warn(String message) {
        log(Level.WARN, message, null);
    }

    /**
     * 记录WARN级别日志（带异常）
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void warn(String message, Throwable throwable) {
        log(Level.WARN, message, throwable);
    }

    /**
     * 记录ERROR级别日志
     * @param message 日志消息
     */
    public static void error(String message) {
        log(Level.ERROR, message, null);
    }

    /**
     * 记录ERROR级别日志（带异常）
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    /**
     * 核心日志记录方法
     * @param level 日志级别
     * @param message 日志消息
     * @param throwable 异常对象（可为null）
     */
    private static synchronized void log(Level level, String message, Throwable throwable) {
        // 构建日志消息
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // 输出到控制台
        if (consoleOutput) {
            PrintWriter consoleWriter = new PrintWriter(System.out);
            consoleWriter.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(consoleWriter);
            }
            consoleWriter.flush();
        }

        // 写入日志文件
        if (fileOutput) {
            try {
                // 创建日志目录（如果不存在）
                File logFile = new File(logFilePath);
                File parentDir = logFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // 追加写入日志
                try (PrintWriter fileWriter = new PrintWriter(new FileWriter(logFile, true))) {
                    fileWriter.println(logMessage);
                    if (throwable != null) {
                        throwable.printStackTrace(fileWriter);
                    }
                }
            } catch (IOException e) {
                System.err.println("日志写入失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
