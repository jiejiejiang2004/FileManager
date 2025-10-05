package org.jiejiejiang.filemanager.util;

import org.jiejiejiang.filemanager.exception.InvalidPathException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 路径处理工具类，提供路径标准化、拆分、解析等功能
 * 统一处理绝对路径、相对路径、特殊符号（.和..）等场景
 */
public class PathUtil {

    // 定义非法字符（Windows和Linux通用禁止的字符）
    private static final String ILLEGAL_CHARS = "*?\"<>| ";  // 包含空格

    /**
     * 验证路径是否合法（不包含非法字符）
     * @param path 待验证的路径
     * @throws InvalidPathException 路径包含非法字符时抛出
     */
    public static void validatePath(String path) throws InvalidPathException {
        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }

        // 检查路径中是否包含非法字符
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (ILLEGAL_CHARS.indexOf(c) != -1) {
                throw new InvalidPathException("路径包含非法字符：" + c + "，路径：" + path);
            }
        }
    }

    /**
     * 将相对路径转换为绝对路径
     * @param relativePath 相对路径（如"./data/disk.img"）
     * @return 绝对路径（如"/home/project/data/disk.img"）
     */
    public static String getAbsolutePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }

        try {
            // 关键：使用 File 类自动处理系统相关的路径分隔符
            File file = new File(relativePath);
            // 获取规范路径（自动补全分隔符、处理 .. 等，且兼容 Windows/Linux）
            // 确保 Windows 路径的盘符后有分隔符（如 C:\ 而非 C:）
            return file.getCanonicalPath();
        } catch (IOException e) {
            // 捕获路径解析错误，返回更清晰的错误信息
            throw new InvalidPathException("无法转换为绝对路径：" + relativePath + "，原因：" + e.getMessage());
        }
    }

    /**
     * 标准化路径（处理连续斜杠、.和..等）
     * @param path 原始路径
     * @return 标准化后的路径
     * @throws InvalidPathException 路径非法法（如越界）时抛出
     */
    public static String normalizePath(String path) throws InvalidPathException {
        // 先验证路径包含非法字符
        validatePath(path);

        // 空路径直接抛出异常（已在validatePath中处理）
        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }

        // 分割路径为组件（按/分割，过滤空字符串）
        String[] components = path.split("/");
        List<String> normalizedComponents = new ArrayList<>();

        for (String component : components) {
            if (component.isEmpty() || component.equals(".")) {
                // 忽略空组件（连续/导致）和当前目录.
                continue;
            } else if (component.equals("..")) {
                // 处理上级目录..：移除最后一个非根组件
                if (normalizedComponents.isEmpty()) {
                    // 已经果已经在根目录，再使用..则越界
                    throw new InvalidPathException("路径越界：不能超出根目录 → " + path);
                }
                normalizedComponents.remove(normalizedComponents.size() - 1);
            } else {
                // 普通目录/文件名，添加到组件列表
                normalizedComponents.add(component);
            }
        }

        // 拼接标准化后的路径（根目录以/开头）
        if (normalizedComponents.isEmpty()) {
            return "/";
        }

        StringBuilder sb = new StringBuilder();
        for (String comp : normalizedComponents) {
            sb.append("/").append(comp);
        }
        return sb.toString();
    }

    /**
     * 拆分路径为目录/文件名片段
     * @param path 标准化后的路径（如"/a/b/c.txt"）
     * @return 片段列表（如["a", "b", "c.txt"]）
     */
    public static List<String> splitPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }

        String normalized = normalizePath(path);
        // 移除开头的斜杠（如果是绝对路径）
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.isEmpty()) {
            return new ArrayList<>(); // 根路径返回空列表
        }

        return Arrays.asList(normalized.split("/"));
    }

    /**
     * 判断路径是否为绝对路径
     * @param path 路径字符串
     * @return 是绝对路径返回true（以/开头或包含盘符）
     */
    public static boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // 以/开头视为绝对路径，或Windows系统下包含:（如C:）
        return path.startsWith("/") || path.contains(":");
    }

    /**
     * 从路径中提取文件名
     * @param path 完整路径（如"/a/b/c.txt"）
     * @return 文件名（如"c.txt"）
     */
    public static String getFileNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }

        String normalized = normalizePath(path);
        int lastSlashIndex = normalized.lastIndexOf("/");

        if (lastSlashIndex == -1) {
            return normalized; // 没有斜杠，整个路径就是文件名
        }

        return normalized.substring(lastSlashIndex + 1);
    }

    /**
     * 获取父目录路径
     * @param path 原始路径（如"/a/b/c.txt"）
     * @return 父目录路径（如"/a/b"）
     */
    public static String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }

        String normalized = normalizePath(path);
        int lastSlashIndex = normalized.lastIndexOf("/");

        if (lastSlashIndex == -1) {
            return "."; // 没有父目录，返回当前目录
        }

        if (lastSlashIndex == 0) {
            return "/"; // 根目录的父目录还是根目录
        }

        return normalized.substring(0, lastSlashIndex);
    }
}
