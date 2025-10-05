package org.jiejiejiang.filemanager.util;

/**
 * 文件大小格式化工具类
 * 将字节数转换为人类可读的单位（B、KB、MB、GB）
 */
public class FileSizeUtil {

    // 单位换算常量
    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    /**
     * 将字节数格式化为带单位的字符串
     * @param bytes 字节数（如1500）
     * @return 格式化后的字符串（如"1.46 KB"）
     */
    public static String format(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("字节数不能为负数");
        }

        if (bytes >= GB) {
            return String.format("%.2f GB", (double) bytes / GB);
        } else if (bytes >= MB) {
            return String.format("%.2f MB", (double) bytes / MB);
        } else if (bytes >= KB) {
            return String.format("%.2f KB", (double) bytes / KB);
        } else {
            return bytes + " B";
        }
    }

    /**
     * 将带单位的字符串转换为字节数
     * @param sizeStr 带单位的字符串（如"1.5 KB"）
     * @return 对应的字节数（如1536）
     */
    public static long parse(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("大小字符串不能为空");
        }

        sizeStr = sizeStr.trim();
        String unit = sizeStr.replaceAll("[0-9.]+", "").trim().toUpperCase();
        double value;

        try {
            value = Double.parseDouble(sizeStr.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的大小格式：" + sizeStr, e);
        }

        switch (unit) {
            case "GB":
                return (long) (value * GB);
            case "MB":
                return (long) (value * MB);
            case "KB":
                return (long) (value * KB);
            case "B":
            default:
                return (long) value;
        }
    }
}
