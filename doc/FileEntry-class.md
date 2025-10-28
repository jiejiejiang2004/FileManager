# FileEntry 类技术文档 / FileEntry Class Technical Documentation

## 概述 / Overview

**中文概述**：
`FileEntry` 类是文件管理系统中的核心元数据实体，类似于Unix系统中的inode概念。它封装了文件和目录的所有基本属性，包括名称、类型、大小、时间戳、存储位置等信息。该类支持文件和目录的统一管理，提供了完整的元数据操作接口，并通过UUID确保每个文件/目录的唯一性。

**English Overview**：
The `FileEntry` class is a core metadata entity in the file management system, similar to the inode concept in Unix systems. It encapsulates all basic attributes of files and directories, including name, type, size, timestamps, storage location, and other information. This class supports unified management of files and directories, provides a complete metadata operation interface, and ensures uniqueness of each file/directory through UUID.

## 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.core;

public class FileEntry {
    // 文件/目录元数据实体（类比 inode）
    // 存储文件/目录的核心属性，区分文件和目录类型
}
```

## 核心特性 / Core Features

### 1. 统一元数据管理 / Unified Metadata Management
- **文件和目录统一抽象**：通过EntryType枚举区分文件和目录类型
- **完整属性封装**：包含名称、大小、时间戳、存储位置等所有必要信息
- **UUID唯一标识**：每个文件/目录都有唯一的UUID标识符

### 2. 智能路径处理 / Intelligent Path Handling
- **根目录特殊处理**：支持根目录的特殊命名规则（空名称）
- **完整路径生成**：自动组合父路径和名称生成完整路径
- **路径分隔符处理**：智能处理路径分隔符，避免重复

### 3. 类型安全操作 / Type-Safe Operations
- **文件大小管理**：只有文件类型可以修改大小，目录大小固定
- **操作权限控制**：通过只读属性控制文件/目录的修改权限
- **删除标记机制**：支持软删除，避免立即释放存储空间

### 4. 时间戳管理 / Timestamp Management
- **创建时间记录**：自动记录文件/目录的创建时间
- **修改时间更新**：操作时自动更新修改时间
- **时间副本保护**：返回时间戳副本，防止外部修改

## 内部枚举 / Inner Enums

### EntryType 枚举 / EntryType Enum

```java
public enum EntryType {
    FILE,       // 文件类型
    DIRECTORY   // 目录类型
}
```

**用途**：区分文件和目录类型，确保类型安全的操作。

## 核心字段 / Core Fields

### 不可变属性 / Immutable Properties

```java
private final String name;          // 名称（不含路径）
private final EntryType type;       // 类型（文件/目录）
private final String parentPath;    // 父目录路径
private final Date createTime;      // 创建时间
private final String uuid;          // 唯一标识符
```

### 可变属性 / Mutable Properties

```java
private long size;                  // 大小（字节）
private int startBlockId;          // 起始块ID
private Date modifyTime;           // 修改时间
private boolean isDeleted;         // 删除标记
private boolean readOnly;          // 只读属性
```

## 构造方法 / Constructors

### 1. 标准构造器 / Standard Constructor

```java
public FileEntry(String name, EntryType type, String parentPath, int startBlockId)
```

**参数说明**：
- `name`: 文件/目录名称（根目录可为空）
- `type`: 文件类型（FILE或DIRECTORY）
- `parentPath`: 父目录路径
- `startBlockId`: 起始块ID（-1表示未分配）

**特殊处理**：
- 根目录名称可以为空
- 自动生成UUID和时间戳
- 目录大小初始化为0

### 2. UUID恢复构造器 / UUID Recovery Constructor

```java
public FileEntry(String name, EntryType type, String parentPath, int startBlockId, String uuid)
```

**用途**：从磁盘加载时恢复已有的UUID，保持文件/目录的唯一性。

## 核心方法 / Core Methods

### 1. 路径操作方法 / Path Operation Methods

#### getFullPath()
```java
public String getFullPath()
```
- **功能**：生成完整路径（父路径 + 名称）
- **特殊处理**：正确处理根目录和路径分隔符
- **返回值**：完整的文件/目录路径

### 2. 大小管理方法 / Size Management Methods

#### updateSize(long newSize)
```java
public void updateSize(long newSize)
```
- **功能**：更新文件大小（仅限文件类型）
- **限制**：目录类型调用会抛出UnsupportedOperationException
- **副作用**：自动更新修改时间

#### setSize(long newSize)
```java
public void setSize(long newSize)
```
- **功能**：直接设置文件大小
- **验证**：检查大小不能为负数
- **同步**：同时更新修改时间

### 3. 状态管理方法 / State Management Methods

#### markAsDeleted()
```java
public void markAsDeleted()
```
- **功能**：标记文件/目录为已删除
- **机制**：软删除，不立即释放存储空间
- **副作用**：更新修改时间

#### setReadOnly(boolean readOnly)
```java
public void setReadOnly(boolean readOnly)
```
- **功能**：设置只读属性
- **权限控制**：控制文件/目录的修改权限
- **时间同步**：自动更新修改时间

### 4. 存储位置方法 / Storage Location Methods

#### setStartBlockId(int newBlockId)
```java
public void setStartBlockId(int newBlockId)
```
- **功能**：设置起始块ID
- **验证**：块ID不能小于-1
- **用途**：更新文件/目录的存储位置

### 5. 格式化方法 / Formatting Methods

#### getFormattedSize()
```java
public String getFormattedSize()
```
- **功能**：获取人类可读的大小格式
- **目录处理**：目录返回"-"
- **文件处理**：使用FileSizeUtil格式化大小

## Getter方法 / Getter Methods

### 基本属性获取 / Basic Property Access

```java
public String getName()              // 获取名称
public EntryType getType()           // 获取类型
public String getParentPath()        // 获取父路径
public long getSize()                // 获取大小
public int getStartBlockId()         // 获取起始块ID
public String getUuid()              // 获取UUID
public boolean isDeleted()           // 检查是否已删除
public boolean isReadOnly()          // 检查是否只读
```

### 时间戳获取 / Timestamp Access

```java
public Date getCreateTime()          // 获取创建时间（副本）
public Date getModifyTime()          // 获取修改时间（副本）
```

**安全机制**：返回Date对象的副本，防止外部修改内部状态。

## 重写方法 / Overridden Methods

### equals(Object o)
```java
@Override
public boolean equals(Object o)
```
- **比较依据**：基于UUID进行比较
- **唯一性保证**：确保每个文件/目录的唯一性
- **性能优化**：避免复杂的路径比较

### hashCode()
```java
@Override
public int hashCode()
```
- **哈希依据**：基于UUID计算哈希值
- **一致性保证**：与equals方法保持一致

### toString()
```java
@Override
public String toString()
```
- **格式化输出**：包含类型、路径、大小、时间等关键信息
- **调试友好**：便于开发和调试时查看对象状态

## 异常处理 / Exception Handling

### 构造器异常 / Constructor Exceptions

```java
IllegalArgumentException:
- 文件/目录名称不能为空（根目录除外）
- 文件类型不能为空
- 父目录路径不能为空
- UUID不能为空（UUID恢复构造器）
```

### 操作异常 / Operation Exceptions

```java
UnsupportedOperationException:
- 目录类型不支持大小修改操作

IllegalArgumentException:
- 文件大小不能为负数
- 块ID不能小于-1
```

## 线程安全 / Thread Safety

**注意事项**：
- `FileEntry` 类**不是线程安全的**
- 多线程环境下需要外部同步机制
- 建议在文件系统层面实现并发控制

## 性能特性 / Performance Characteristics

### 内存使用 / Memory Usage
- **轻量级对象**：每个实例占用内存较小
- **不可变字段**：减少意外修改的风险
- **UUID缓存**：避免重复生成UUID

### 操作复杂度 / Operation Complexity
- **路径生成**：O(1) - 简单字符串拼接
- **大小更新**：O(1) - 直接字段赋值
- **比较操作**：O(1) - 基于UUID比较

## 使用示例 / Usage Examples

### 创建文件条目 / Creating File Entry

```java
// 创建文件元数据
FileEntry file = new FileEntry(
    "document.txt",                    // 文件名
    FileEntry.EntryType.FILE,          // 文件类型
    "/home/user",                      // 父目录路径
    100                                // 起始块ID
);

// 设置文件大小
file.updateSize(2048);

// 设置只读属性
file.setReadOnly(true);

// 获取完整路径
String fullPath = file.getFullPath(); // "/home/user/document.txt"
```

### 创建目录条目 / Creating Directory Entry

```java
// 创建目录元数据
FileEntry directory = new FileEntry(
    "projects",                        // 目录名
    FileEntry.EntryType.DIRECTORY,     // 目录类型
    "/home/user",                      // 父目录路径
    -1                                 // 未分配块
);

// 检查类型
if (directory.getType() == FileEntry.EntryType.DIRECTORY) {
    System.out.println("这是一个目录");
}

// 获取格式化大小
String size = directory.getFormattedSize(); // "-"
```

### 根目录处理 / Root Directory Handling

```java
// 创建根目录（特殊情况：名称为空）
FileEntry rootDir = new FileEntry(
    "",                                // 根目录名称为空
    FileEntry.EntryType.DIRECTORY,     // 目录类型
    "/",                               // 父路径为"/"
    0                                  // 根目录块ID
);

String rootPath = rootDir.getFullPath(); // "/"
```

### 文件操作示例 / File Operation Examples

```java
// 文件大小管理
FileEntry file = new FileEntry("data.bin", FileEntry.EntryType.FILE, "/tmp", 50);

// 更新文件大小
file.setSize(4096);
System.out.println("文件大小: " + file.getFormattedSize());

// 标记删除
file.markAsDeleted();
System.out.println("是否已删除: " + file.isDeleted());

// 设置存储位置
file.setStartBlockId(200);
System.out.println("起始块ID: " + file.getStartBlockId());
```

### UUID和比较操作 / UUID and Comparison Operations

```java
FileEntry file1 = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/", 10);
FileEntry file2 = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/", 20);

// UUID唯一性
System.out.println("文件1 UUID: " + file1.getUuid());
System.out.println("文件2 UUID: " + file2.getUuid());

// 比较操作（基于UUID）
boolean isEqual = file1.equals(file2); // false，不同的UUID

// 从磁盘恢复时保持UUID
String savedUuid = file1.getUuid();
FileEntry restoredFile = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/", 10, savedUuid);
boolean isSame = file1.equals(restoredFile); // true，相同的UUID
```

## 设计模式 / Design Patterns

### 使用的设计模式 / Applied Design Patterns

1. **值对象模式 (Value Object Pattern)**
   - 封装文件/目录的元数据
   - 提供不可变的核心属性

2. **工厂模式 (Factory Pattern)**
   - 通过构造器创建不同类型的条目
   - 支持从磁盘恢复的特殊构造

3. **策略模式 (Strategy Pattern)**
   - 文件和目录的不同行为策略
   - 类型安全的操作限制

### 架构优势 / Architectural Advantages

- **类型安全**：编译时检查文件/目录操作的合法性
- **数据完整性**：通过不可变字段保护关键属性
- **扩展性**：易于添加新的元数据属性
- **可维护性**：清晰的职责分离和接口设计

## 扩展建议 / Extension Recommendations

### 功能扩展 / Feature Extensions

1. **权限系统**：添加更细粒度的权限控制
2. **版本管理**：支持文件版本历史
3. **标签系统**：支持文件/目录标签分类
4. **压缩属性**：支持文件压缩状态管理

### 性能优化 / Performance Optimizations

1. **字段懒加载**：对于不常用的属性实现懒加载
2. **缓存机制**：缓存格式化字符串等计算结果
3. **序列化优化**：优化磁盘存储格式

## 依赖关系 / Dependencies

### 外部依赖 / External Dependencies

- `java.util.Date`: 时间戳管理
- `java.util.Objects`: 对象比较和哈希
- `java.util.UUID`: 唯一标识符生成
- `org.jiejiejiang.filemanager.util.FileSizeUtil`: 文件大小格式化

### 被依赖关系 / Dependent Classes

- `Directory`: 目录管理中的条目存储
- `FileSystem`: 文件系统中的元数据管理
- `Buffer`: 缓冲区中的文件信息

## 测试建议 / Testing Recommendations

### 单元测试覆盖 / Unit Test Coverage

1. **构造器测试**：验证各种参数组合的正确性
2. **类型安全测试**：验证文件/目录操作的限制
3. **路径处理测试**：验证各种路径场景的正确性
4. **异常处理测试**：验证异常情况的处理
5. **UUID唯一性测试**：验证UUID的唯一性和持久性

### 集成测试建议 / Integration Test Recommendations

1. **文件系统集成**：与FileSystem类的集成测试
2. **持久化测试**：验证磁盘存储和恢复的正确性
3. **并发测试**：验证多线程环境下的行为