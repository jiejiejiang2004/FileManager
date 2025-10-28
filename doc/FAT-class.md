# FAT 类技术文档 / FAT Class Technical Documentation

## 概述 / Overview

**中文概述：**
`FAT`（File Allocation Table，文件分配表）类是文件管理系统的核心存储管理组件，负责管理磁盘块的分配、回收与链式存储。它实现了类似于FAT文件系统的块分配算法，通过字节数组维护每个磁盘块的状态和链接关系，支持文件的动态扩展和高效的空间管理。

**English Overview:**
The `FAT` (File Allocation Table) class is the core storage management component of the file management system, responsible for managing disk block allocation, deallocation, and linked storage. It implements a block allocation algorithm similar to the FAT file system, maintaining the status and linking relationships of each disk block through a byte array, supporting dynamic file expansion and efficient space management.

## 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.core;

public class FAT {
    // Implementation details...
}
```

## 核心特性 / Core Features

### 1. 链式存储管理 / Linked Storage Management
- 基于字节数组的FAT表实现
- 支持文件的链式块分配
- 动态文件扩展和收缩

### 2. 块状态管理 / Block Status Management
- 空闲块、已用块、坏块的状态标记
- 文件结束标记（EOF）
- 块链完整性验证

### 3. 持久化存储 / Persistent Storage
- FAT表的磁盘持久化
- 从磁盘加载FAT表
- 数据一致性保障

### 4. 线程安全设计 / Thread-Safe Design
- 关键操作的同步保护
- 并发访问安全
- 状态一致性维护

## 常量定义 / Constants

### FAT特殊标记值 / FAT Special Markers
```java
public static final byte FREE_BLOCK = 0;           // 空闲块标记
public static final byte END_OF_FILE = (byte) 255; // 文件结束标记
public static final byte BAD_BLOCK = (byte) 254;   // 坏块标记
```

**标记说明 / Marker Description:**
- `FREE_BLOCK (0)`: 表示该块未被使用，可以分配
- `END_OF_FILE (-1)`: 表示该块是文件的最后一个块
- `BAD_BLOCK (-2)`: 表示该块物理损坏或被预留，不可使用

## 核心字段 / Core Fields

### 依赖组件 / Dependencies
```java
private final Disk disk;              // 关联的磁盘实例
```

### FAT表结构 / FAT Table Structure
```java
private byte[] fatTable;              // FAT表本体：索引=块ID，值=下一个块ID
private int totalBlocks;              // 磁盘总块数
private boolean isInitialized;       // FAT初始化状态
```

## 构造方法 / Constructor

### 主构造器 / Primary Constructor
```java
public FAT(Disk disk) throws DiskInitializeException
```

**功能说明 / Functionality:**
- 关联已初始化的磁盘实例
- 同步磁盘总块数
- 设置初始化状态为false

**参数 / Parameters:**
- `disk`: 已初始化的Disk实例

**异常 / Exceptions:**
- `DiskInitializeException`: 磁盘未初始化时抛出

## 核心方法 / Core Methods

### 1. 生命周期管理 / Lifecycle Management

#### initialize()
```java
public void initialize()
```

**功能 / Function:**
- 创建FAT表数组（大小=磁盘总块数）
- 初始化所有块为空闲状态
- 标记块0和块1为FAT存储区域
- 设置初始化状态为true

**实现细节 / Implementation Details:**
```java
// 1. 初始化FAT表数组
this.fatTable = new byte[totalBlocks];
// 2. 所有块默认设为空闲
Arrays.fill(fatTable, FREE_BLOCK);
// 3. 标记FAT占用的块0和块1
fatTable[0] = BAD_BLOCK; // 块0用于FAT存储
fatTable[1] = BAD_BLOCK; // 块1用于FAT存储
```

### 2. 块分配操作 / Block Allocation Operations

#### allocateBlock()
```java
public synchronized int allocateBlock() throws DiskFullException, InvalidBlockIdException
```

**功能 / Function:**
- 分配一个空闲块
- 从块2开始搜索（跳过FAT存储块）
- 标记块为已占用（END_OF_FILE）
- 提供详细的分配日志

**返回值 / Return Value:**
- `int`: 分配到的块ID（>=2）

**异常 / Exceptions:**
- `DiskFullException`: 无空闲块时抛出
- `InvalidBlockIdException`: FAT未初始化时抛出

**算法流程 / Algorithm Flow:**
```java
// 遍历FAT表，找第一个空闲块
for (int blockId = 2; blockId < totalBlocks; blockId++) {
    if (fatTable[blockId] == FREE_BLOCK) {
        fatTable[blockId] = END_OF_FILE;
        return blockId;
    }
}
```

#### allocateNextBlock()
```java
public synchronized int allocateNextBlock(int currentBlockId) 
    throws InvalidBlockIdException, DiskFullException
```

**功能 / Function:**
- 为指定块分配下一个块（文件扩展）
- 建立块链连接关系
- 新块设置为文件结束标记

**参数 / Parameters:**
- `currentBlockId`: 当前块ID（已分配的块）

**返回值 / Return Value:**
- `int`: 新分配的下一个块ID

**实现逻辑 / Implementation Logic:**
```java
// 1. 分配一个新的空闲块
int nextBlockId = allocateBlock();
// 2. 将当前块的"下一块"指向新块
fatTable[currentBlockId] = (byte) nextBlockId;
// 3. 新块的"下一块"设为文件结束
fatTable[nextBlockId] = END_OF_FILE;
```

### 3. 块释放操作 / Block Deallocation Operations

#### freeBlocks()
```java
public synchronized void freeBlocks(int startBlockId) throws InvalidBlockIdException
```

**功能 / Function:**
- 释放指定块及其后续所有块
- 遍历块链进行批量释放
- 防止块链循环引用
- 提供详细的释放日志

**参数 / Parameters:**
- `startBlockId`: 文件的起始块ID

**算法实现 / Algorithm Implementation:**
```java
int currentBlockId = startBlockId;
while (true) {
    // 1. 记录下一个块ID
    int nextBlockId = fatTable[currentBlockId];
    // 2. 将当前块标记为空闲
    fatTable[currentBlockId] = FREE_BLOCK;
    // 3. 终止条件：当前块是文件最后一块
    if (nextBlockId == END_OF_FILE) {
        break;
    }
    // 4. 移动到下一个块
    currentBlockId = nextBlockId;
}
```

### 4. 块查询操作 / Block Query Operations

#### getNextBlock()
```java
public int getNextBlock(int blockId) throws InvalidBlockIdException
```

**功能 / Function:**
- 查询指定块的下一个块ID
- 处理特殊标记值
- 返回-1表示文件结束

**参数 / Parameters:**
- `blockId`: 待查询的块ID

**返回值 / Return Value:**
- `int`: 下一个块ID，或-1表示文件结束

#### getFileBlockCount()
```java
public int getFileBlockCount(int startBlockId) throws InvalidBlockIdException
```

**功能 / Function:**
- 计算文件占用的总块数
- 通过起始块遍历链式结构
- 防止死循环检测

**参数 / Parameters:**
- `startBlockId`: 文件的起始块ID

**返回值 / Return Value:**
- `int`: 文件占用的块数

### 5. 持久化操作 / Persistence Operations

#### saveToDisk()
```java
public void saveToDisk() throws DiskInitializeException
```

**功能 / Function:**
- 将FAT表写入磁盘块0和块1
- 支持大容量FAT表的分块存储
- 数据完整性验证

**实现细节 / Implementation Details:**
```java
// 计算需要的块数
int requiredBlocks = (fatBytes.length + blockSize - 1) / blockSize;
// 分块写入到块0和块1
for (int i = 0; i < requiredBlocks; i++) {
    int blockId = i;
    byte[] blockData = new byte[blockSize];
    System.arraycopy(fatBytes, offset, blockData, 0, length);
    disk.writeBlock(blockId, blockData);
}
```

#### loadFromDisk()
```java
public void loadFromDisk() throws DiskInitializeException
```

**功能 / Function:**
- 从磁盘块0和块1读取FAT表
- 恢复FAT表状态
- 修正FAT存储区标记

**恢复逻辑 / Recovery Logic:**
```java
// 从磁盘读取FAT数据
for (int i = 0; i < requiredBlocks; i++) {
    byte[] blockData = disk.readBlock(i);
    System.arraycopy(blockData, 0, fatBytes, offset, length);
}
// 修正FAT存储区标记
fatTable[0] = BAD_BLOCK;
fatTable[1] = BAD_BLOCK;
```

### 6. 状态管理方法 / Status Management Methods

#### setNextBlock()
```java
public void setNextBlock(int currentBlockId, int nextBlockId) throws InvalidBlockIdException
```

**功能 / Function:**
- 设置块的下一个块ID
- 构建块链关系
- 支持文件结束标记

#### markAsBadBlock() / markAsFreeBlock()
```java
public void markAsBadBlock(int blockId)
public void markAsFreeBlock(int blockId)
```

**功能 / Function:**
- 标记块为坏块或空闲块
- 块状态管理
- 磁盘维护支持

#### isBadBlock() / isFreeBlock()
```java
public boolean isBadBlock(int blockId)
public boolean isFreeBlock(int blockId)
```

**功能 / Function:**
- 检查块状态
- 状态查询接口
- 条件判断支持

### 7. 统计和监控方法 / Statistics and Monitoring Methods

#### getFreeBlockCount()
```java
public int getFreeBlockCount()
```

**功能 / Function:**
- 获取空闲块数量
- 磁盘空间统计
- 容量监控支持

**实现 / Implementation:**
```java
int count = 0;
for (int i = 2; i < totalBlocks; i++) { // 从块2开始
    if (fatTable[i] == FREE_BLOCK) {
        count++;
    }
}
return count;
```

### 8. 私有辅助方法 / Private Helper Methods

#### checkInitialized()
```java
private void checkInitialized() throws InvalidBlockIdException
```

**功能 / Function:**
- 检查FAT初始化状态
- 操作前置条件验证

#### validateAllocatedBlock()
```java
private void validateAllocatedBlock(int blockId) throws InvalidBlockIdException
```

**功能 / Function:**
- 校验块ID是否合法且已占用
- 块状态一致性检查

#### validateBlockId()
```java
private void validateBlockId(int blockId) throws InvalidBlockIdException
```

**功能 / Function:**
- 验证块ID合法性
- 范围和状态检查

#### getBlockStatusDesc()
```java
private String getBlockStatusDesc(int blockStatus)
```

**功能 / Function:**
- 获取块状态描述文本
- 日志和异常信息支持

## 异常处理 / Exception Handling

### 异常类型 / Exception Types
1. **DiskInitializeException**: 磁盘初始化相关异常
2. **InvalidBlockIdException**: 无效块ID异常
3. **DiskFullException**: 磁盘空间不足异常

### 异常处理策略 / Exception Handling Strategy
- 所有公共方法都有异常保护
- 详细的错误信息和上下文
- 状态一致性保障

## 线程安全 / Thread Safety

### 同步机制 / Synchronization Mechanism
- 关键方法使用`synchronized`修饰
- 块分配和释放操作的原子性
- 并发访问安全保障

### 线程安全方法 / Thread-Safe Methods
```java
public synchronized int allocateBlock()
public synchronized int allocateNextBlock()
public synchronized void freeBlocks()
```

## 性能特性 / Performance Characteristics

### 时间复杂度 / Time Complexity
- `allocateBlock()`: O(n) - 线性搜索空闲块
- `freeBlocks()`: O(k) - k为文件块数
- `getNextBlock()`: O(1) - 直接数组访问
- `getFileBlockCount()`: O(k) - k为文件块数

### 空间复杂度 / Space Complexity
- FAT表：O(n) - n为总块数
- 每块1字节存储开销
- 内存占用：totalBlocks字节

## 存储布局 / Storage Layout

### FAT表结构 / FAT Table Structure
```
索引(块ID) | 值(下一块ID或特殊标记)
---------|------------------------
0        | BAD_BLOCK (FAT存储区)
1        | BAD_BLOCK (FAT存储区)
2        | FREE_BLOCK 或 下一块ID
3        | FREE_BLOCK 或 下一块ID
...      | ...
n-1      | FREE_BLOCK 或 下一块ID
```

### 文件块链示例 / File Block Chain Example
```
文件A: 块2 → 块5 → 块8 → END_OF_FILE
FAT[2] = 5
FAT[5] = 8
FAT[8] = END_OF_FILE
```

## 使用示例 / Usage Examples

### 基本使用 / Basic Usage
```java
// 1. 创建FAT实例
Disk disk = new Disk("config/disk.properties");
disk.initialize();
FAT fat = new FAT(disk);

// 2. 初始化FAT
fat.initialize();

// 3. 分配块
int blockId = fat.allocateBlock();
System.out.println("分配到块: " + blockId);

// 4. 扩展文件
int nextBlockId = fat.allocateNextBlock(blockId);
System.out.println("扩展块: " + nextBlockId);

// 5. 释放文件
fat.freeBlocks(blockId);

// 6. 持久化FAT
fat.saveToDisk();
```

### 文件操作示例 / File Operations Example
```java
// 创建一个3块的文件
int startBlock = fat.allocateBlock();
int block2 = fat.allocateNextBlock(startBlock);
int block3 = fat.allocateNextBlock(block2);

// 查询文件大小
int fileSize = fat.getFileBlockCount(startBlock);
System.out.println("文件占用块数: " + fileSize);

// 遍历文件块
int currentBlock = startBlock;
while (currentBlock != -1) {
    System.out.println("文件块: " + currentBlock);
    currentBlock = fat.getNextBlock(currentBlock);
}

// 删除文件
fat.freeBlocks(startBlock);
```

### 状态监控示例 / Status Monitoring Example
```java
// 检查磁盘使用情况
int freeBlocks = fat.getFreeBlockCount();
int totalBlocks = fat.getTotalBlocks();
int usedBlocks = totalBlocks - freeBlocks - 2; // 减去FAT存储块

System.out.println("总块数: " + totalBlocks);
System.out.println("空闲块数: " + freeBlocks);
System.out.println("已用块数: " + usedBlocks);
System.out.println("使用率: " + (usedBlocks * 100.0 / (totalBlocks - 2)) + "%");

// 检查特定块状态
for (int i = 0; i < 10; i++) {
    if (fat.isFreeBlock(i)) {
        System.out.println("块" + i + ": 空闲");
    } else if (fat.isBadBlock(i)) {
        System.out.println("块" + i + ": 坏块/预留");
    } else {
        System.out.println("块" + i + ": 已占用");
    }
}
```

## 设计模式 / Design Patterns

### 1. 单例模式变体 / Singleton Pattern Variant
- 每个磁盘实例对应一个FAT实例
- 通过构造器依赖注入确保唯一性

### 2. 状态模式 / State Pattern
- 块的不同状态（空闲、已用、坏块）
- 状态转换的规则和约束

### 3. 观察者模式潜力 / Observer Pattern Potential
- 可扩展为支持块状态变化监听
- 磁盘空间监控和告警

## 扩展建议 / Extension Recommendations

### 1. 性能优化 / Performance Optimization
- 实现空闲块位图索引
- 添加块分配策略（首次适应、最佳适应）
- 支持块预分配和批量操作

### 2. 功能增强 / Feature Enhancement
- 添加块碎片整理功能
- 支持坏块重映射
- 实现FAT表备份和恢复

### 3. 监控和诊断 / Monitoring and Diagnostics
- 添加详细的使用统计
- 实现块访问热度分析
- 支持FAT表一致性检查

## 依赖关系 / Dependencies

### 内部依赖 / Internal Dependencies
- `org.jiejiejiang.filemanager.core.Disk`: 磁盘操作
- `org.jiejiejiang.filemanager.exception.*`: 异常处理
- `org.jiejiejiang.filemanager.util.LogUtil`: 日志记录

### 外部依赖 / External Dependencies
- `java.util.Arrays`: 数组操作
- `java.util.HashSet`: 集合操作
- `java.util.Set`: 集合接口

## 测试建议 / Testing Recommendations

### 单元测试覆盖 / Unit Test Coverage
- 块分配和释放测试
- 块链操作测试
- 异常情况测试
- 边界条件测试
- 持久化操作测试

### 集成测试 / Integration Testing
- 与Disk类的集成测试
- 与FileSystem的集成测试
- 并发访问测试
- 大容量数据测试

### 性能测试 / Performance Testing
- 大量块分配性能测试
- 长块链遍历性能测试
- 并发操作性能测试

---

**文档版本 / Document Version:** 1.0  
**最后更新 / Last Updated:** 2024-12-19  
**维护者 / Maintainer:** FileManager Development Team