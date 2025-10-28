# 工具类技术文档 / Utility Classes Technical Documentation

## 概述 / Overview

### 中文概述

工具类模块为文件管理系统提供了一系列通用的辅助功能，包括路径处理、文件大小格式化和日志记录等核心工具。这些工具类遵循单一职责原则，提供静态方法接口，确保在整个系统中的一致性和可重用性。

**主要工具类：**
- **PathUtil**：路径处理工具类，提供路径标准化、验证、拆分等功能
- **FileSizeUtil**：文件大小格式化工具类，处理字节数与人类可读单位的转换
- **LogUtil**：日志工具类，提供多级别日志记录和输出控制

### English Overview

The utility classes module provides a series of common auxiliary functions for the file management system, including path processing, file size formatting, and logging. These utility classes follow the single responsibility principle and provide static method interfaces to ensure consistency and reusability throughout the system.

**Main Utility Classes:**
- **PathUtil**: Path processing utility class providing path normalization, validation, and splitting
- **FileSizeUtil**: File size formatting utility class handling conversion between bytes and human-readable units
- **LogUtil**: Logging utility class providing multi-level logging and output control

## 架构设计 / Architecture Design

### 工具类层次结构 / Utility Class Hierarchy

```
org.jiejiejiang.filemanager.util
├── PathUtil.java           # 路径处理工具类
├── FileSizeUtil.java       # 文件大小格式化工具类
└── LogUtil.java            # 日志工具类
```

### 设计原则 / Design Principles

#### 1. 静态工具类设计 / Static Utility Class Design
- **无状态设计**：所有工具类都是无状态的，只提供静态方法
- **线程安全**：所有方法都是线程安全的，可在多线程环境中使用
- **不可实例化**：工具类不应被实例化，所有功能通过静态方法提供

#### 2. 单一职责原则 / Single Responsibility Principle
- **PathUtil**：专注于路径相关操作
- **FileSizeUtil**：专注于文件大小格式化
- **LogUtil**：专注于日志记录功能

#### 3. 异常处理策略 / Exception Handling Strategy
- **输入验证**：对所有输入参数进行严格验证
- **明确异常**：抛出具体的异常类型，提供详细错误信息
- **容错设计**：在可能的情况下提供默认值或降级处理

## PathUtil 路径处理工具类 / Path Processing Utility Class

### 类声明 / Class Declaration

```java
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
    
    // 私有构造器，防止实例化
    private PathUtil() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
}
```

### 核心特性 / Core Features

#### 1. 路径验证 / Path Validation
- **非法字符检测**：检查路径中的非法字符
- **路径格式验证**：验证路径格式的合法性
- **空值处理**：处理null和空字符串输入

#### 2. 路径标准化 / Path Normalization
- **分隔符统一**：将所有路径分隔符统一为"/"
- **特殊符号处理**：处理"."和".."等特殊路径符号
- **重复分隔符清理**：清理连续的路径分隔符

#### 3. 路径解析 / Path Parsing
- **路径拆分**：将路径拆分为组件列表
- **文件名提取**：从完整路径中提取文件名
- **父目录获取**：获取指定路径的父目录

### 核心方法 / Core Methods

#### validatePath()
```java
/**
 * 验证路径的合法性
 * @param path 待验证的路径字符串
 * @throws InvalidPathException 路径包含非法字符时抛出
 */
public static void validatePath(String path) throws InvalidPathException
```

**功能 / Function:**
- 检查路径是否为null或空
- 验证路径中是否包含非法字符
- 确保路径格式符合系统要求

**使用示例 / Usage Example:**
```java
try {
    PathUtil.validatePath("/home/user/document.txt");
    // 路径验证通过
} catch (InvalidPathException e) {
    // 处理非法路径
    System.err.println("路径非法: " + e.getMessage());
}
```

#### normalizePath()
```java
/**
 * 标准化路径，处理相对路径符号和重复分隔符
 * @param path 原始路径字符串
 * @return 标准化后的路径
 * @throws InvalidPathException 路径非法时抛出
 */
public static String normalizePath(String path) throws InvalidPathException
```

**功能 / Function:**
- 将路径分隔符统一为"/"
- 处理"."（当前目录）和".."（父目录）符号
- 清理重复的路径分隔符
- 返回标准化的绝对路径

**使用示例 / Usage Example:**
```java
String normalized = PathUtil.normalizePath("/home/user/../user/./documents");
// 结果: "/home/user/documents"

String normalized2 = PathUtil.normalizePath("a//b///c");
// 结果: "/a/b/c"
```

#### splitPath()
```java
/**
 * 拆分路径为目录/文件名片段
 * @param path 标准化后的路径（如"/a/b/c.txt"）
 * @return 片段列表（如["a", "b", "c.txt"]）
 */
public static List<String> splitPath(String path)
```

**功能 / Function:**
- 将路径拆分为组件列表
- 自动处理绝对路径和相对路径
- 返回不包含分隔符的路径组件

**使用示例 / Usage Example:**
```java
List<String> components = PathUtil.splitPath("/home/user/documents/file.txt");
// 结果: ["home", "user", "documents", "file.txt"]

List<String> rootComponents = PathUtil.splitPath("/");
// 结果: [] (空列表，表示根目录)
```

#### getFileNameFromPath()
```java
/**
 * 从路径中提取文件名
 * @param path 完整路径（如"/a/b/c.txt"）
 * @return 文件名（如"c.txt"）
 */
public static String getFileNameFromPath(String path)
```

**功能 / Function:**
- 从完整路径中提取最后一个组件作为文件名
- 处理各种路径格式
- 自动标准化输入路径

**使用示例 / Usage Example:**
```java
String fileName = PathUtil.getFileNameFromPath("/home/user/document.txt");
// 结果: "document.txt"

String dirName = PathUtil.getFileNameFromPath("/home/user/documents");
// 结果: "documents"
```

#### getParentPath()
```java
/**
 * 获取父目录路径
 * @param path 原始路径（如"/a/b/c.txt"）
 * @return 父目录路径（如"/a/b"）
 */
public static String getParentPath(String path)
```

**功能 / Function:**
- 获取指定路径的父目录
- 处理根目录的特殊情况
- 返回标准化的父目录路径

**使用示例 / Usage Example:**
```java
String parent = PathUtil.getParentPath("/home/user/documents/file.txt");
// 结果: "/home/user/documents"

String rootParent = PathUtil.getParentPath("/");
// 结果: "/" (根目录的父目录还是根目录)
```

#### isAbsolutePath()
```java
/**
 * 判断路径是否为绝对路径
 * @param path 路径字符串
 * @return 是绝对路径返回true（以/开头或包含盘符）
 */
public static boolean isAbsolutePath(String path)
```

**功能 / Function:**
- 判断路径是否为绝对路径
- 支持Unix风格（以/开头）和Windows风格（包含:）
- 处理各种边界情况

**使用示例 / Usage Example:**
```java
boolean isAbsolute1 = PathUtil.isAbsolutePath("/home/user");
// 结果: true

boolean isAbsolute2 = PathUtil.isAbsolutePath("C:\\Users\\user");
// 结果: true

boolean isAbsolute3 = PathUtil.isAbsolutePath("relative/path");
// 结果: false
```

#### getAbsolutePath()
```java
/**
 * 将相对路径转换为绝对路径
 * @param relativePath 相对路径
 * @return 绝对路径
 */
public static String getAbsolutePath(String relativePath)
```

**功能 / Function:**
- 将相对路径转换为绝对路径
- 基于当前工作目录进行转换
- 处理路径转换异常

**使用示例 / Usage Example:**
```java
String absolutePath = PathUtil.getAbsolutePath("documents/file.txt");
// 结果: "/current/working/directory/documents/file.txt"
```

## FileSizeUtil 文件大小格式化工具类 / File Size Formatting Utility Class

### 类声明 / Class Declaration

```java
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
    
    // 私有构造器，防止实例化
    private FileSizeUtil() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
}
```

### 核心特性 / Core Features

#### 1. 大小格式化 / Size Formatting
- **自动单位选择**：根据大小自动选择合适的单位（B、KB、MB、GB）
- **精度控制**：保留两位小数，确保显示精度
- **负数处理**：对负数输入进行验证和异常处理

#### 2. 大小解析 / Size Parsing
- **多单位支持**：支持B、KB、MB、GB等单位的解析
- **格式容错**：支持多种输入格式的解析
- **异常处理**：对无效格式进行详细的异常处理

### 核心方法 / Core Methods

#### format()
```java
/**
 * 将字节数格式化为带单位的字符串
 * @param bytes 字节数（如1500）
 * @return 格式化后的字符串（如"1.46 KB"）
 */
public static String format(long bytes)
```

**功能 / Function:**
- 将字节数转换为人类可读的格式
- 自动选择最合适的单位
- 保留两位小数精度

**单位选择逻辑 / Unit Selection Logic:**
- `bytes >= GB`: 使用GB单位
- `bytes >= MB`: 使用MB单位  
- `bytes >= KB`: 使用KB单位
- `bytes < KB`: 使用B单位

**使用示例 / Usage Example:**
```java
String size1 = FileSizeUtil.format(1024);
// 结果: "1.00 KB"

String size2 = FileSizeUtil.format(1536);
// 结果: "1.50 KB"

String size3 = FileSizeUtil.format(1048576);
// 结果: "1.00 MB"

String size4 = FileSizeUtil.format(1073741824);
// 结果: "1.00 GB"

String size5 = FileSizeUtil.format(500);
// 结果: "500 B"
```

#### parse()
```java
/**
 * 将带单位的字符串转换为字节数
 * @param sizeStr 带单位的字符串（如"1.5 KB"）
 * @return 对应的字节数（如1536）
 */
public static long parse(String sizeStr)
```

**功能 / Function:**
- 解析带单位的大小字符串
- 支持多种单位格式
- 返回对应的字节数

**支持的单位 / Supported Units:**
- **GB**: 1024³ 字节
- **MB**: 1024² 字节
- **KB**: 1024 字节
- **B**: 1 字节（默认）

**使用示例 / Usage Example:**
```java
long bytes1 = FileSizeUtil.parse("1.5 KB");
// 结果: 1536

long bytes2 = FileSizeUtil.parse("2.5 MB");
// 结果: 2621440

long bytes3 = FileSizeUtil.parse("1 GB");
// 结果: 1073741824

long bytes4 = FileSizeUtil.parse("500 B");
// 结果: 500

// 异常情况
try {
    FileSizeUtil.parse("invalid size");
} catch (IllegalArgumentException e) {
    System.err.println("无效的大小格式: " + e.getMessage());
}
```

## LogUtil 日志工具类 / Logging Utility Class

### 类声明 / Class Declaration

```java
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
    
    // 日志级别枚举
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }
    
    // 配置字段
    private static String logFilePath = "./logs/filemanager.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static boolean consoleOutput = true;
    private static boolean fileOutput = true;
    private static boolean debugEnabled = true;
    
    // 私有构造器，防止实例化
    private LogUtil() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
}
```

### 核心特性 / Core Features

#### 1. 多级别日志 / Multi-Level Logging
- **DEBUG**：调试信息，可通过开关控制
- **INFO**：一般信息记录
- **WARN**：警告信息，支持异常对象
- **ERROR**：错误信息，支持异常对象

#### 2. 多输出目标 / Multiple Output Targets
- **控制台输出**：实时显示日志信息
- **文件输出**：持久化保存日志记录
- **输出控制**：可独立控制各输出目标的开关

#### 3. 格式化输出 / Formatted Output
- **时间戳**：精确到秒的时间记录
- **级别标识**：清晰的日志级别标识
- **异常堆栈**：完整的异常堆栈跟踪

### 配置方法 / Configuration Methods

#### setLogFilePath()
```java
/**
 * 设置日志文件路径
 * @param path 日志文件路径
 */
public static void setLogFilePath(String path)
```

**使用示例 / Usage Example:**
```java
LogUtil.setLogFilePath("./logs/custom.log");
```

#### setConsoleOutput()
```java
/**
 * 设置是否输出到控制台
 * @param enable  true-启用控制台输出，false-禁用
 */
public static void setConsoleOutput(boolean enable)
```

#### setFileOutput()
```java
/**
 * 设置是否写入日志文件
 * @param enable  true-启用文件写入，false-禁用
 */
public static void setFileOutput(boolean enable)
```

#### setDebugEnabled()
```java
/**
 * 设置是否启用调试日志
 * @param enabled true-启用DEBUG级别日志，false-禁用
 */
public static void setDebugEnabled(boolean enabled)
```

### 日志记录方法 / Logging Methods

#### debug()
```java
/**
 * 记录DEBUG级别日志
 * @param message 日志消息
 */
public static void debug(String message)
```

**功能 / Function:**
- 记录调试级别的日志信息
- 受debugEnabled开关控制
- 主要用于开发和调试阶段

**使用示例 / Usage Example:**
```java
LogUtil.debug("进入方法: processFile()");
LogUtil.debug("处理文件: " + fileName);
```

#### info()
```java
/**
 * 记录INFO级别日志
 * @param message 日志消息
 */
public static void info(String message)
```

**功能 / Function:**
- 记录一般信息级别的日志
- 用于记录系统的正常运行信息
- 始终启用，不受调试开关影响

**使用示例 / Usage Example:**
```java
LogUtil.info("文件系统初始化完成");
LogUtil.info("用户登录: " + username);
```

#### warn()
```java
/**
 * 记录WARN级别日志
 * @param message 日志消息
 */
public static void warn(String message)

/**
 * 记录WARN级别日志（带异常）
 * @param message 日志消息
 * @param throwable 异常对象
 */
public static void warn(String message, Throwable throwable)
```

**功能 / Function:**
- 记录警告级别的日志信息
- 支持附加异常对象和堆栈跟踪
- 用于记录可能的问题或异常情况

**使用示例 / Usage Example:**
```java
LogUtil.warn("磁盘空间不足，剩余: " + freeSpace);

try {
    // 一些操作
} catch (IOException e) {
    LogUtil.warn("文件读取警告", e);
}
```

#### error()
```java
/**
 * 记录ERROR级别日志
 * @param message 日志消息
 */
public static void error(String message)

/**
 * 记录ERROR级别日志（带异常）
 * @param message 日志消息
 * @param throwable 异常对象
 */
public static void error(String message, Throwable throwable)
```

**功能 / Function:**
- 记录错误级别的日志信息
- 支持附加异常对象和完整堆栈跟踪
- 用于记录系统错误和异常情况

**使用示例 / Usage Example:**
```java
LogUtil.error("文件系统初始化失败");

try {
    // 关键操作
} catch (Exception e) {
    LogUtil.error("系统发生严重错误", e);
}
```

### 日志格式 / Log Format

#### 标准日志格式 / Standard Log Format
```
[时间戳] [级别] 消息内容
异常堆栈（如果有）
```

#### 示例输出 / Example Output
```
[2024-12-19 14:30:25] [INFO] 文件系统初始化完成
[2024-12-19 14:30:26] [DEBUG] 进入方法: loadDirectory()
[2024-12-19 14:30:27] [WARN] 磁盘空间不足，剩余: 100MB
[2024-12-19 14:30:28] [ERROR] 文件读取失败
java.io.IOException: 文件不存在
    at org.jiejiejiang.filemanager.core.FileSystem.readFile(FileSystem.java:123)
    at org.jiejiejiang.filemanager.gui.controller.MainController.openFile(MainController.java:456)
```

## 使用示例 / Usage Examples

### 综合使用示例 / Comprehensive Usage Examples

```java
package org.jiejiejiang.filemanager.example;

import org.jiejiejiang.filemanager.util.PathUtil;
import org.jiejiejiang.filemanager.util.FileSizeUtil;
import org.jiejiejiang.filemanager.util.LogUtil;
import org.jiejiejiang.filemanager.exception.InvalidPathException;

/**
 * 工具类使用示例
 */
public class UtilityClassesExample {
    
    public static void main(String[] args) {
        // 配置日志
        setupLogging();
        
        // 路径处理示例
        pathProcessingExample();
        
        // 文件大小格式化示例
        fileSizeFormattingExample();
        
        // 日志记录示例
        loggingExample();
    }
    
    /**
     * 配置日志设置
     */
    private static void setupLogging() {
        LogUtil.setLogFilePath("./logs/example.log");
        LogUtil.setConsoleOutput(true);
        LogUtil.setFileOutput(true);
        LogUtil.setDebugEnabled(true);
        
        LogUtil.info("日志系统初始化完成");
    }
    
    /**
     * 路径处理示例
     */
    private static void pathProcessingExample() {
        LogUtil.info("开始路径处理示例");
        
        try {
            // 路径验证
            String testPath = "/home/user/../user/./documents/file.txt";
            LogUtil.debug("原始路径: " + testPath);
            
            PathUtil.validatePath(testPath);
            LogUtil.debug("路径验证通过");
            
            // 路径标准化
            String normalizedPath = PathUtil.normalizePath(testPath);
            LogUtil.info("标准化路径: " + normalizedPath);
            
            // 路径拆分
            List<String> pathComponents = PathUtil.splitPath(normalizedPath);
            LogUtil.info("路径组件: " + pathComponents);
            
            // 提取文件名
            String fileName = PathUtil.getFileNameFromPath(normalizedPath);
            LogUtil.info("文件名: " + fileName);
            
            // 获取父目录
            String parentPath = PathUtil.getParentPath(normalizedPath);
            LogUtil.info("父目录: " + parentPath);
            
            // 判断绝对路径
            boolean isAbsolute = PathUtil.isAbsolutePath(normalizedPath);
            LogUtil.info("是否为绝对路径: " + isAbsolute);
            
        } catch (InvalidPathException e) {
            LogUtil.error("路径处理失败", e);
        }
    }
    
    /**
     * 文件大小格式化示例
     */
    private static void fileSizeFormattingExample() {
        LogUtil.info("开始文件大小格式化示例");
        
        // 格式化不同大小的文件
        long[] fileSizes = {500, 1024, 1536, 1048576, 2621440, 1073741824};
        
        for (long size : fileSizes) {
            String formattedSize = FileSizeUtil.format(size);
            LogUtil.info(String.format("字节数: %d -> 格式化: %s", size, formattedSize));
        }
        
        // 解析大小字符串
        String[] sizeStrings = {"1.5 KB", "2.5 MB", "1 GB", "500 B"};
        
        for (String sizeStr : sizeStrings) {
            try {
                long bytes = FileSizeUtil.parse(sizeStr);
                LogUtil.info(String.format("大小字符串: %s -> 字节数: %d", sizeStr, bytes));
            } catch (IllegalArgumentException e) {
                LogUtil.warn("无效的大小格式: " + sizeStr, e);
            }
        }
    }
    
    /**
     * 日志记录示例
     */
    private static void loggingExample() {
        LogUtil.info("开始日志记录示例");
        
        // 不同级别的日志
        LogUtil.debug("这是一条调试信息");
        LogUtil.info("这是一条信息记录");
        LogUtil.warn("这是一条警告信息");
        LogUtil.error("这是一条错误信息");
        
        // 带异常的日志
        try {
            // 模拟异常
            throw new RuntimeException("模拟异常");
        } catch (Exception e) {
            LogUtil.error("捕获到异常", e);
        }
        
        // 测试调试开关
        LogUtil.setDebugEnabled(false);
        LogUtil.debug("这条调试信息不会显示");
        
        LogUtil.setDebugEnabled(true);
        LogUtil.debug("这条调试信息会显示");
        
        LogUtil.info("日志记录示例完成");
    }
}
```

### 文件操作中的工具类使用 / Utility Classes in File Operations

```java
/**
 * 文件操作中使用工具类的示例
 */
public class FileOperationExample {
    
    /**
     * 安全的文件路径处理
     */
    public boolean processFile(String inputPath, String content) {
        try {
            // 1. 验证和标准化路径
            LogUtil.debug("开始处理文件: " + inputPath);
            PathUtil.validatePath(inputPath);
            String normalizedPath = PathUtil.normalizePath(inputPath);
            LogUtil.info("标准化路径: " + normalizedPath);
            
            // 2. 检查文件大小
            long contentSize = content.getBytes().length;
            String formattedSize = FileSizeUtil.format(contentSize);
            LogUtil.info("文件内容大小: " + formattedSize);
            
            // 3. 提取文件信息
            String fileName = PathUtil.getFileNameFromPath(normalizedPath);
            String parentDir = PathUtil.getParentPath(normalizedPath);
            
            LogUtil.info("文件名: " + fileName);
            LogUtil.info("父目录: " + parentDir);
            
            // 4. 执行文件操作
            boolean success = performFileWrite(normalizedPath, content);
            
            if (success) {
                LogUtil.info("文件写入成功: " + fileName);
            } else {
                LogUtil.warn("文件写入失败: " + fileName);
            }
            
            return success;
            
        } catch (InvalidPathException e) {
            LogUtil.error("路径验证失败: " + inputPath, e);
            return false;
        } catch (Exception e) {
            LogUtil.error("文件处理过程中发生异常", e);
            return false;
        }
    }
    
    /**
     * 批量文件大小统计
     */
    public void analyzeDirectorySize(List<FileInfo> files) {
        LogUtil.info("开始分析目录大小");
        
        long totalSize = 0;
        Map<String, Long> sizeByType = new HashMap<>();
        
        for (FileInfo file : files) {
            try {
                // 标准化文件路径
                String normalizedPath = PathUtil.normalizePath(file.getPath());
                String fileName = PathUtil.getFileNameFromPath(normalizedPath);
                
                // 累计大小
                long fileSize = file.getSize();
                totalSize += fileSize;
                
                // 按类型统计
                String extension = getFileExtension(fileName);
                sizeByType.merge(extension, fileSize, Long::sum);
                
                // 记录文件信息
                String formattedSize = FileSizeUtil.format(fileSize);
                LogUtil.debug(String.format("文件: %s, 大小: %s", fileName, formattedSize));
                
            } catch (InvalidPathException e) {
                LogUtil.warn("跳过无效路径的文件: " + file.getPath(), e);
            }
        }
        
        // 输出统计结果
        String totalFormattedSize = FileSizeUtil.format(totalSize);
        LogUtil.info("目录总大小: " + totalFormattedSize);
        
        for (Map.Entry<String, Long> entry : sizeByType.entrySet()) {
            String typeSize = FileSizeUtil.format(entry.getValue());
            LogUtil.info(String.format("类型 %s: %s", entry.getKey(), typeSize));
        }
    }
    
    // 辅助方法
    private boolean performFileWrite(String path, String content) {
        // 模拟文件写入操作
        return true;
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "无扩展名";
    }
    
    // 文件信息类
    public static class FileInfo {
        private String path;
        private long size;
        
        public FileInfo(String path, long size) {
            this.path = path;
            this.size = size;
        }
        
        public String getPath() { return path; }
        public long getSize() { return size; }
    }
}
```

## 性能优化 / Performance Optimization

### 工具类性能优化策略 / Utility Class Performance Optimization

#### 1. 字符串处理优化 / String Processing Optimization

```java
/**
 * 优化的路径处理实现
 */
public class OptimizedPathUtil {
    
    // 使用StringBuilder减少字符串拼接开销
    public static String optimizedNormalizePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }
        
        // 预估容量，减少StringBuilder扩容
        StringBuilder sb = new StringBuilder(path.length());
        String[] components = path.split("/");
        
        for (String component : components) {
            if (!component.isEmpty() && !component.equals(".")) {
                if (component.equals("..")) {
                    // 回退到上一级目录
                    int lastSlash = sb.lastIndexOf("/");
                    if (lastSlash > 0) {
                        sb.setLength(lastSlash);
                    }
                } else {
                    sb.append("/").append(component);
                }
            }
        }
        
        return sb.length() == 0 ? "/" : sb.toString();
    }
    
    // 缓存常用路径组件
    private static final Map<String, List<String>> PATH_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    public static List<String> cachedSplitPath(String path) {
        return PATH_CACHE.computeIfAbsent(path, k -> {
            if (PATH_CACHE.size() >= MAX_CACHE_SIZE) {
                PATH_CACHE.clear(); // 简单的缓存清理策略
            }
            return splitPath(k);
        });
    }
}
```

#### 2. 日志性能优化 / Logging Performance Optimization

```java
/**
 * 高性能日志实现
 */
public class HighPerformanceLogUtil {
    
    // 异步日志队列
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(10000);
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    
    // 批量写入缓冲区
    private static final List<LogEntry> batchBuffer = new ArrayList<>(100);
    private static final int BATCH_SIZE = 100;
    private static final long BATCH_TIMEOUT_MS = 1000;
    
    static {
        // 启动异步日志处理线程
        logExecutor.submit(new LogProcessor());
    }
    
    /**
     * 异步日志记录
     */
    public static void asyncLog(Level level, String message, Throwable throwable) {
        LogEntry entry = new LogEntry(level, message, throwable, System.currentTimeMillis());
        
        if (!logQueue.offer(entry)) {
            // 队列满时的降级处理
            System.err.println("日志队列已满，丢弃日志: " + message);
        }
    }
    
    /**
     * 日志处理器
     */
    private static class LogProcessor implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 批量收集日志条目
                    collectLogEntries();
                    
                    // 批量写入
                    if (!batchBuffer.isEmpty()) {
                        writeBatchLogs();
                        batchBuffer.clear();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("日志处理异常: " + e.getMessage());
                }
            }
        }
        
        private void collectLogEntries() throws InterruptedException {
            // 等待第一个日志条目
            LogEntry firstEntry = logQueue.take();
            batchBuffer.add(firstEntry);
            
            // 收集更多条目直到达到批量大小或超时
            long startTime = System.currentTimeMillis();
            while (batchBuffer.size() < BATCH_SIZE && 
                   System.currentTimeMillis() - startTime < BATCH_TIMEOUT_MS) {
                
                LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    batchBuffer.add(entry);
                }
            }
        }
        
        private void writeBatchLogs() {
            // 批量写入日志文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
                for (LogEntry entry : batchBuffer) {
                    writer.println(entry.format());
                    if (entry.getThrowable() != null) {
                        entry.getThrowable().printStackTrace(writer);
                    }
                }
            } catch (IOException e) {
                System.err.println("批量日志写入失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 日志条目类
     */
    private static class LogEntry {
        private final Level level;
        private final String message;
        private final Throwable throwable;
        private final long timestamp;
        
        public LogEntry(Level level, String message, Throwable throwable, long timestamp) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = timestamp;
        }
        
        public String format() {
            return String.format("[%s] [%s] %s", 
                DATE_FORMAT.format(new Date(timestamp)), level, message);
        }
        
        // Getters
        public Throwable getThrowable() { return throwable; }
    }
}
```

#### 3. 文件大小格式化优化 / File Size Formatting Optimization

```java
/**
 * 优化的文件大小格式化
 */
public class OptimizedFileSizeUtil {
    
    // 预计算的单位数组
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};
    private static final long[] UNIT_VALUES = {1L, 1024L, 1024L * 1024L, 1024L * 1024L * 1024L, 1024L * 1024L * 1024L * 1024L};
    
    // 格式化器缓存
    private static final ThreadLocal<DecimalFormat> FORMATTER = ThreadLocal.withInitial(() -> 
        new DecimalFormat("#,##0.##"));
    
    /**
     * 高性能格式化方法
     */
    public static String fastFormat(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("字节数不能为负数");
        }
        
        // 找到合适的单位
        int unitIndex = 0;
        double value = bytes;
        
        while (unitIndex < UNIT_VALUES.length - 1 && bytes >= UNIT_VALUES[unitIndex + 1]) {
            unitIndex++;
            value = (double) bytes / UNIT_VALUES[unitIndex];
        }
        
        // 使用线程本地的格式化器
        DecimalFormat formatter = FORMATTER.get();
        
        if (unitIndex == 0) {
            return bytes + " " + UNITS[unitIndex];
        } else {
            return formatter.format(value) + " " + UNITS[unitIndex];
        }
    }
    
    // 解析缓存
    private static final Map<String, Long> PARSE_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 带缓存的解析方法
     */
    public static long cachedParse(String sizeStr) {
        return PARSE_CACHE.computeIfAbsent(sizeStr, k -> {
            if (PARSE_CACHE.size() >= 1000) {
                PARSE_CACHE.clear();
            }
            return parse(k);
        });
    }
}
```

## 测试建议 / Testing Recommendations

### 工具类单元测试 / Utility Classes Unit Tests

```java
package org.jiejiejiang.filemanager.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具类单元测试
 */
public class UtilityClassesTest {
    
    @BeforeEach
    void setUp() {
        // 重置日志配置
        LogUtil.setConsoleOutput(false);
        LogUtil.setFileOutput(false);
        LogUtil.setDebugEnabled(true);
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试环境
    }
    
    // PathUtil 测试
    @Test
    void testPathValidation() {
        // 测试有效路径
        assertDoesNotThrow(() -> PathUtil.validatePath("/valid/path"));
        assertDoesNotThrow(() -> PathUtil.validatePath("relative/path"));
        
        // 测试无效路径
        assertThrows(InvalidPathException.class, () -> PathUtil.validatePath(null));
        assertThrows(InvalidPathException.class, () -> PathUtil.validatePath(""));
        assertThrows(InvalidPathException.class, () -> PathUtil.validatePath("path*with*invalid"));
    }
    
    @Test
    void testPathNormalization() {
        // 测试基本标准化
        assertEquals("/a/b/c", PathUtil.normalizePath("/a/b/c"));
        assertEquals("/a/b/c", PathUtil.normalizePath("/a//b///c"));
        
        // 测试相对路径符号
        assertEquals("/a/c", PathUtil.normalizePath("/a/b/../c"));
        assertEquals("/a/b", PathUtil.normalizePath("/a/b/./"));
        assertEquals("/", PathUtil.normalizePath("/a/.."));
        
        // 测试复杂情况
        assertEquals("/home/user/documents", 
                    PathUtil.normalizePath("/home/user/../user/./documents"));
    }
    
    @Test
    void testPathSplitting() {
        // 测试路径拆分
        List<String> components = PathUtil.splitPath("/home/user/documents");
        assertEquals(Arrays.asList("home", "user", "documents"), components);
        
        // 测试根路径
        List<String> rootComponents = PathUtil.splitPath("/");
        assertTrue(rootComponents.isEmpty());
        
        // 测试相对路径
        List<String> relativeComponents = PathUtil.splitPath("a/b/c");
        assertEquals(Arrays.asList("a", "b", "c"), relativeComponents);
    }
    
    @Test
    void testFileNameExtraction() {
        assertEquals("file.txt", PathUtil.getFileNameFromPath("/home/user/file.txt"));
        assertEquals("documents", PathUtil.getFileNameFromPath("/home/user/documents"));
        assertEquals("file.txt", PathUtil.getFileNameFromPath("file.txt"));
    }
    
    @Test
    void testParentPathExtraction() {
        assertEquals("/home/user", PathUtil.getParentPath("/home/user/file.txt"));
        assertEquals("/", PathUtil.getParentPath("/file.txt"));
        assertEquals("/", PathUtil.getParentPath("/"));
    }
    
    @Test
    void testAbsolutePathDetection() {
        assertTrue(PathUtil.isAbsolutePath("/home/user"));
        assertTrue(PathUtil.isAbsolutePath("C:\\Users\\user"));
        assertFalse(PathUtil.isAbsolutePath("relative/path"));
        assertFalse(PathUtil.isAbsolutePath(""));
    }
    
    // FileSizeUtil 测试
    @Test
    void testFileSizeFormatting() {
        assertEquals("500 B", FileSizeUtil.format(500));
        assertEquals("1.00 KB", FileSizeUtil.format(1024));
        assertEquals("1.50 KB", FileSizeUtil.format(1536));
        assertEquals("1.00 MB", FileSizeUtil.format(1048576));
        assertEquals("1.00 GB", FileSizeUtil.format(1073741824));
        
        // 测试边界情况
        assertEquals("0 B", FileSizeUtil.format(0));
        assertEquals("1023 B", FileSizeUtil.format(1023));
    }
    
    @Test
    void testFileSizeParsing() {
        assertEquals(500, FileSizeUtil.parse("500 B"));
        assertEquals(1024, FileSizeUtil.parse("1 KB"));
        assertEquals(1536, FileSizeUtil.parse("1.5 KB"));
        assertEquals(1048576, FileSizeUtil.parse("1 MB"));
        assertEquals(1073741824, FileSizeUtil.parse("1 GB"));
        
        // 测试格式容错
        assertEquals(1024, FileSizeUtil.parse("1KB"));
        assertEquals(1024, FileSizeUtil.parse("1.0 KB"));
    }
    
    @Test
    void testFileSizeParsingExceptions() {
        assertThrows(IllegalArgumentException.class, () -> FileSizeUtil.parse(null));
        assertThrows(IllegalArgumentException.class, () -> FileSizeUtil.parse(""));
        assertThrows(IllegalArgumentException.class, () -> FileSizeUtil.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> FileSizeUtil.parse("1.5.5 KB"));
    }
    
    @Test
    void testFileSizeFormattingExceptions() {
        assertThrows(IllegalArgumentException.class, () -> FileSizeUtil.format(-1));
    }
    
    // LogUtil 测试
    @Test
    void testLogLevels() {
        // 启用文件输出进行测试
        LogUtil.setFileOutput(true);
        LogUtil.setLogFilePath("./test-logs/test.log");
        
        assertDoesNotThrow(() -> LogUtil.debug("Debug message"));
        assertDoesNotThrow(() -> LogUtil.info("Info message"));
        assertDoesNotThrow(() -> LogUtil.warn("Warning message"));
        assertDoesNotThrow(() -> LogUtil.error("Error message"));
        
        // 测试带异常的日志
        Exception testException = new RuntimeException("Test exception");
        assertDoesNotThrow(() -> LogUtil.warn("Warning with exception", testException));
        assertDoesNotThrow(() -> LogUtil.error("Error with exception", testException));
    }
    
    @Test
    void testLogConfiguration() {
        // 测试配置方法
        assertDoesNotThrow(() -> LogUtil.setLogFilePath("./test.log"));
        assertDoesNotThrow(() -> LogUtil.setConsoleOutput(false));
        assertDoesNotThrow(() -> LogUtil.setFileOutput(false));
        assertDoesNotThrow(() -> LogUtil.setDebugEnabled(false));
        
        // 验证调试开关
        LogUtil.setDebugEnabled(false);
        assertDoesNotThrow(() -> LogUtil.debug("This should not appear"));
        
        LogUtil.setDebugEnabled(true);
        assertDoesNotThrow(() -> LogUtil.debug("This should appear"));
    }
}
```

### 性能测试建议 / Performance Testing Recommendations

```java
/**
 * 工具类性能测试
 */
public class UtilityClassesPerformanceTest {
    
    @Test
    void testPathProcessingPerformance() {
        int iterations = 100000;
        String[] testPaths = {
            "/home/user/documents/file.txt",
            "/a/b/../c/./d",
            "relative/path/to/file",
            "/very/long/path/with/many/components/file.txt"
        };
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            for (String path : testPaths) {
                PathUtil.normalizePath(path);
                PathUtil.splitPath(path);
                PathUtil.getFileNameFromPath(path);
                PathUtil.getParentPath(path);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("路径处理性能测试: " + iterations + " 次迭代耗时 " + duration + " ms");
        assertTrue(duration < 5000, "路径处理性能应在5秒内完成");
    }
    
    @Test
    void testFileSizeFormattingPerformance() {
        int iterations = 1000000;
        long[] testSizes = {500, 1024, 1536, 1048576, 1073741824};
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            for (long size : testSizes) {
                FileSizeUtil.format(size);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("文件大小格式化性能测试: " + iterations + " 次迭代耗时 " + duration + " ms");
        assertTrue(duration < 3000, "文件大小格式化性能应在3秒内完成");
    }
    
    @Test
    void testLoggingPerformance() {
        int iterations = 10000;
        
        // 禁用控制台输出，只测试文件写入
        LogUtil.setConsoleOutput(false);
        LogUtil.setFileOutput(true);
        LogUtil.setLogFilePath("./performance-test.log");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            LogUtil.info("Performance test message " + i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("日志记录性能测试: " + iterations + " 次迭代耗时 " + duration + " ms");
        assertTrue(duration < 10000, "日志记录性能应在10秒内完成");
    }
}
```

## 扩展建议 / Extension Recommendations

### 1. 工具类功能扩展 / Utility Class Feature Extensions

#### PathUtil 扩展 / PathUtil Extensions
- **通配符支持**：添加路径通配符匹配功能
- **路径比较**：实现路径相似度比较算法
- **路径压缩**：实现路径的压缩和展开功能

#### FileSizeUtil 扩展 / FileSizeUtil Extensions
- **更多单位**：支持TB、PB等更大单位
- **本地化支持**：支持不同语言的单位显示
- **精度控制**：允许用户自定义显示精度

#### LogUtil 扩展 / LogUtil Extensions
- **日志轮转**：实现日志文件的自动轮转
- **远程日志**：支持远程日志服务器
- **结构化日志**：支持JSON等结构化日志格式

### 2. 新工具类建议 / New Utility Class Suggestions

#### DateTimeUtil 日期时间工具类
```java
public class DateTimeUtil {
    public static String formatTimestamp(long timestamp);
    public static long parseTimestamp(String dateStr);
    public static String getRelativeTime(long timestamp);
    public static boolean isValidDate(String dateStr);
}
```

#### ValidationUtil 验证工具类
```java
public class ValidationUtil {
    public static boolean isValidFileName(String fileName);
    public static boolean isValidFileSize(long size);
    public static boolean isValidPath(String path);
    public static String sanitizeInput(String input);
}
```

#### CompressionUtil 压缩工具类
```java
public class CompressionUtil {
    public static byte[] compress(byte[] data);
    public static byte[] decompress(byte[] compressedData);
    public static String compressString(String text);
    public static String decompressString(String compressedText);
}
```

### 3. 性能优化建议 / Performance Optimization Suggestions

- **缓存机制**：为常用操作添加缓存支持
- **异步处理**：对耗时操作实现异步处理
- **批量操作**：支持批量处理以提高效率
- **内存优化**：减少不必要的对象创建

## 依赖关系 / Dependencies

### 内部依赖 / Internal Dependencies

- **异常处理模块**：`InvalidPathException` 等自定义异常
- **核心模块**：被文件系统、GUI控制器等模块使用
- **配置模块**：日志配置和路径配置

### 外部依赖 / External Dependencies

- **Java标准库**：`java.io.*`、`java.util.*`、`java.text.*`
- **并发工具**：`java.util.concurrent.*`（用于性能优化）
- **文件系统API**：`java.nio.file.*`（用于路径处理）

### 被依赖关系 / Dependent Modules

- **文件系统模块**：使用PathUtil进行路径处理
- **GUI控制器**：使用LogUtil记录操作日志
- **磁盘管理模块**：使用FileSizeUtil格式化大小显示
- **异常处理模块**：使用LogUtil记录异常信息

---

**文档版本**：1.0  
**最后更新**：2024年12月  
**维护者**：文件管理系统开发团队