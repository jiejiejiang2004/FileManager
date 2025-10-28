# Disk 类技术文档 / Disk Class Technical Documentation

## 概述 / Overview

**中文概述：**
`Disk` 类是文件管理系统的底层存储抽象层，负责模拟物理磁盘的行为。它提供了块级别的读写操作，是整个文件系统的存储基础。该类将磁盘抽象为固定大小的块集合，通过随机访问文件实现持久化存储。

**English Overview:**
The `Disk` class serves as the low-level storage abstraction layer for the file management system, responsible for simulating physical disk behavior. It provides block-level read/write operations and forms the storage foundation of the entire file system. This class abstracts the disk as a collection of fixed-size blocks and implements persistent storage through random access files.

## 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.core;

public class Disk {
    // Implementation details...
}
```

## 核心特性 / Core Features

### 1. 块级存储管理 / Block-Level Storage Management
- 固定大小的块结构（默认1024字节）
- 支持随机访问任意块
- 块ID范围校验和边界检查

### 2. 配置驱动初始化 / Configuration-Driven Initialization
- 通过配置文件加载磁盘参数
- 支持自定义块大小和总块数
- 灵活的磁盘文件路径配置

### 3. 异常安全设计 / Exception-Safe Design
- 完整的异常处理机制
- 资源自动释放保障
- 状态一致性维护

### 4. 持久化存储 / Persistent Storage
- 基于RandomAccessFile的高效I/O
- 自动文件扩展和目录创建
- 数据完整性保障

## 常量定义 / Constants

```java
// 默认配置值
private int blockSize = 1024;        // 默认块大小：1024字节
private int totalBlocks = 1024;      // 默认总块数：1024块
```

## 核心字段 / Core Fields

### 配置参数 / Configuration Parameters
```java
private int blockSize;               // 磁盘块大小（字节）
private int totalBlocks;             // 磁盘总块数
private final long diskTotalSize;    // 磁盘总容量（字节）
```

### 存储管理 / Storage Management
```java
private String diskFilePath;         // 模拟磁盘的物理文件路径
private RandomAccessFile diskFile;   // 随机访问文件对象
private boolean isInitialized;       // 磁盘初始化状态
```

## 构造方法 / Constructor

### 主构造器 / Primary Constructor
```java
public Disk(String configPath) throws DiskInitializeException
```

**功能说明 / Functionality:**
- 通过配置文件路径初始化磁盘参数
- 计算磁盘总容量
- 设置初始化状态为false

**参数 / Parameters:**
- `configPath`: 磁盘配置文件路径

**异常 / Exceptions:**
- `DiskInitializeException`: 配置文件加载失败或参数无效

## 核心方法 / Core Methods

### 1. 磁盘生命周期管理 / Disk Lifecycle Management

#### initialize()
```java
public boolean initialize() throws DiskInitializeException
```

**功能 / Function:**
- 创建或打开物理磁盘文件
- 自动创建父目录
- 扩展文件到指定大小
- 设置初始化状态

**返回值 / Return Value:**
- `boolean`: 初始化成功返回true

#### close()
```java
public void close()
```

**功能 / Function:**
- 关闭磁盘文件流
- 释放系统资源
- 重置初始化状态

#### format()
```java
public void format() throws DiskInitializeException
```

**功能 / Function:**
- 清空磁盘所有数据
- 重置为初始状态
- 保持磁盘大小不变

### 2. 块级I/O操作 / Block-Level I/O Operations

#### readBlock()
```java
public byte[] readBlock(int blockId) throws InvalidBlockIdException, DiskInitializeException
```

**功能 / Function:**
- 读取指定块的完整数据
- 自动处理不足数据的补零
- 提供块ID合法性校验

**参数 / Parameters:**
- `blockId`: 块ID（0 <= blockId < totalBlocks）

**返回值 / Return Value:**
- `byte[]`: 块数据（长度为blockSize）

**异常 / Exceptions:**
- `InvalidBlockIdException`: 块ID超出范围
- `DiskInitializeException`: 磁盘未初始化或读取失败

#### writeBlock()
```java
public boolean writeBlock(int blockId, byte[] data) 
    throws InvalidBlockIdException, DiskWriteException, DiskInitializeException
```

**功能 / Function:**
- 向指定块写入数据
- 自动处理数据长度（截断或补零）
- 确保写入完整块大小

**参数 / Parameters:**
- `blockId`: 目标块ID
- `data`: 要写入的数据

**返回值 / Return Value:**
- `boolean`: 写入成功返回true

**异常 / Exceptions:**
- `InvalidBlockIdException`: 块ID无效
- `DiskWriteException`: 写入操作失败
- `DiskInitializeException`: 磁盘未初始化

### 3. 工具方法 / Utility Methods

#### isDiskFull()
```java
public boolean isDiskFull(int requiredBlocks)
```

**功能 / Function:**
- 检查磁盘是否有足够空间
- 基于总容量的简单判断

**参数 / Parameters:**
- `requiredBlocks`: 需要的块数

**返回值 / Return Value:**
- `boolean`: 空间不足返回true

### 4. 私有辅助方法 / Private Helper Methods

#### loadConfig()
```java
private void loadConfig(String configPath) throws DiskInitializeException
```

**功能 / Function:**
- 加载配置文件
- 解析磁盘参数
- 参数合法性校验

#### calculateOffset()
```java
private long calculateOffset(int blockId)
```

**功能 / Function:**
- 计算块ID对应的文件偏移量
- 公式：blockId * blockSize

#### validateBlockId()
```java
private boolean validateBlockId(int blockId)
```

**功能 / Function:**
- 校验块ID合法性
- 范围检查：0 <= blockId < totalBlocks

#### checkInitialized()
```java
private void checkInitialized() throws DiskInitializeException
```

**功能 / Function:**
- 检查磁盘初始化状态
- 未初始化时抛出异常

## 配置文件格式 / Configuration File Format

```properties
# 磁盘配置文件示例 (disk.properties)
disk.block.size=1024
disk.total.blocks=1024
disk.file.path=./data/disk.img
```

**配置项说明 / Configuration Items:**
- `disk.block.size`: 块大小（字节），默认1024
- `disk.total.blocks`: 总块数，默认1024
- `disk.file.path`: 磁盘文件路径，默认"./data/disk.img"

## 异常处理 / Exception Handling

### 异常类型 / Exception Types
1. **DiskInitializeException**: 磁盘初始化相关异常
2. **InvalidBlockIdException**: 无效块ID异常
3. **DiskWriteException**: 磁盘写入异常

### 异常处理策略 / Exception Handling Strategy
- 所有I/O操作都有异常保护
- 资源泄漏防护（finalize方法）
- 详细的错误信息提供

## 线程安全 / Thread Safety

**注意事项 / Important Notes:**
- `Disk` 类不是线程安全的
- 多线程环境下需要外部同步
- 建议在FileSystem层实现并发控制

## 性能特性 / Performance Characteristics

### 时间复杂度 / Time Complexity
- `readBlock()`: O(1) - 随机访问
- `writeBlock()`: O(1) - 随机访问
- `initialize()`: O(1) - 文件操作

### 空间复杂度 / Space Complexity
- 内存占用：O(1) - 仅存储配置参数
- 磁盘占用：O(totalBlocks * blockSize)

## 使用示例 / Usage Examples

### 基本使用 / Basic Usage
```java
// 1. 创建磁盘实例
Disk disk = new Disk("config/disk.properties");

// 2. 初始化磁盘
disk.initialize();

// 3. 写入数据
byte[] data = "Hello, World!".getBytes();
disk.writeBlock(0, data);

// 4. 读取数据
byte[] readData = disk.readBlock(0);
String content = new String(readData).trim();

// 5. 关闭磁盘
disk.close();
```

### 错误处理示例 / Error Handling Example
```java
try {
    Disk disk = new Disk("config/disk.properties");
    disk.initialize();
    
    // 尝试读取无效块
    byte[] data = disk.readBlock(9999);
} catch (InvalidBlockIdException e) {
    System.err.println("块ID无效: " + e.getMessage());
} catch (DiskInitializeException e) {
    System.err.println("磁盘初始化失败: " + e.getMessage());
}
```

### 批量操作示例 / Batch Operations Example
```java
Disk disk = new Disk("config/disk.properties");
disk.initialize();

// 批量写入数据
for (int i = 0; i < 10; i++) {
    String content = "Block " + i + " content";
    disk.writeBlock(i, content.getBytes());
}

// 批量读取验证
for (int i = 0; i < 10; i++) {
    byte[] data = disk.readBlock(i);
    System.out.println("Block " + i + ": " + new String(data).trim());
}

disk.close();
```

## 设计模式 / Design Patterns

### 1. 资源管理模式 / Resource Management Pattern
- 通过`initialize()`和`close()`管理资源生命周期
- `finalize()`方法提供资源泄漏保护

### 2. 配置模式 / Configuration Pattern
- 通过外部配置文件驱动初始化
- 支持默认值和参数校验

### 3. 异常安全模式 / Exception Safety Pattern
- 完整的异常层次结构
- 操作失败时保持对象状态一致性

## 扩展建议 / Extension Recommendations

### 1. 性能优化 / Performance Optimization
- 添加块缓存机制
- 实现异步I/O操作
- 支持批量读写操作

### 2. 功能增强 / Feature Enhancement
- 添加磁盘碎片整理功能
- 支持磁盘快照和备份
- 实现磁盘使用统计

### 3. 可靠性提升 / Reliability Improvement
- 添加数据校验和机制
- 实现坏块检测和标记
- 支持磁盘镜像和RAID

## 依赖关系 / Dependencies

### 内部依赖 / Internal Dependencies
- `org.jiejiejiang.filemanager.exception.*`: 异常类
- `org.jiejiejiang.filemanager.util.PathUtil`: 路径工具类

### 外部依赖 / External Dependencies
- `java.io.*`: 文件I/O操作
- `java.util.Properties`: 配置文件解析

## 测试建议 / Testing Recommendations

### 单元测试覆盖 / Unit Test Coverage
- 配置文件加载测试
- 块读写操作测试
- 异常情况测试
- 边界条件测试

### 集成测试 / Integration Testing
- 与FileSystem的集成测试
- 大容量数据测试
- 并发访问测试

---

**文档版本 / Document Version:** 1.0  
**最后更新 / Last Updated:** 2024-12-19  
**维护者 / Maintainer:** FileManager Development Team