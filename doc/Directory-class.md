# Directory 类技术文档

## 概述 / Overview

### 中文概述
`Directory` 类是文件管理器系统中的目录内容管理器，负责管理目录下的子文件和子目录（目录项），处理目录项的持久化存储和读取。该类实现了目录结构的内存缓存机制，提供高效的目录操作接口，并确保数据的一致性和完整性。

### English Overview
The `Directory` class is a directory content manager in the file manager system, responsible for managing sub-files and subdirectories (directory entries) under a directory, handling persistent storage and reading of directory entries. This class implements an in-memory caching mechanism for directory structures, provides efficient directory operation interfaces, and ensures data consistency and integrity.

## 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.core;

public class Directory {
    // 目录内容管理器实现
}
```

## 核心特性 / Core Features

### 中文特性
- **目录项缓存管理**：内存缓存目录项，避免频繁磁盘读写
- **持久化存储**：支持目录项的磁盘存储和恢复
- **智能块管理**：自动分配和释放磁盘块，优化存储空间
- **数据一致性**：确保内存缓存与磁盘数据的同步
- **删除标记机制**：支持软删除，避免数据丢失
- **格式兼容性**：支持多种目录项存储格式的向后兼容

### English Features
- **Directory Entry Caching**: In-memory caching of directory entries to avoid frequent disk I/O
- **Persistent Storage**: Support for disk storage and recovery of directory entries
- **Smart Block Management**: Automatic allocation and deallocation of disk blocks for storage optimization
- **Data Consistency**: Ensure synchronization between memory cache and disk data
- **Deletion Marking**: Support for soft deletion to prevent data loss
- **Format Compatibility**: Backward compatibility with multiple directory entry storage formats

## 常量定义 / Constants

```java
// 目录项存储格式常量
private static final String ENTRY_SEPARATOR = "|";        // 字段分隔符
private static final String ENTRY_TERMINATOR = "\n";      // 条目结束符
private static final int MAX_ENTRY_NAME_LENGTH = 64;      // 最大名称长度
private static final int MAX_ENTRIES_PER_BLOCK = 8;       // 每块最多存储的目录项数
```

## 核心字段 / Core Fields

### 依赖组件 / Dependencies
```java
private final FileSystem fileSystem;  // 关联的文件系统
private final FileEntry dirEntry;     // 目录自身的元数据
private final Disk disk;              // 磁盘引用（用于读写块）
private final FAT fat;                // FAT引用（用于块管理）
private final int blockSize;          // 块大小
```

### 缓存和状态 / Cache and State
```java
private List<FileEntry> entriesCache; // 内存缓存的目录项
private boolean isDirty;              // 缓存是否已修改（需要同步到磁盘）
```

## 构造方法 / Constructor

### Directory(FileSystem fileSystem, FileEntry dirEntry)

```java
public Directory(FileSystem fileSystem, FileEntry dirEntry)
```

**参数 / Parameters**:
- `fileSystem` - 关联的文件系统实例
- `dirEntry` - 目录自身的元数据

**功能 / Function**:
- 初始化目录管理器
- 验证输入参数的有效性
- 获取文件系统的核心组件引用
- 初始化目录项缓存
- 从磁盘加载现有目录项（如果已分配块）

**异常 / Exceptions**:
- `IllegalArgumentException` - 参数为null或类型不匹配
- `IllegalStateException` - 无法获取必要的系统组件

## 核心方法 / Core Methods

### 目录项管理 / Directory Entry Management

#### addEntry(FileEntry entry)
```java
public void addEntry(FileEntry entry) throws FileSystemException
```
**参数 / Parameters**: `entry` - 要添加的子项
**功能 / Function**: 添加子目录项（文件或目录）
- 验证目录项的合法性
- 检查是否存在同名项
- 添加到内存缓存并标记为脏
- 更新FileSystem的全局缓存
- 记录操作日志

#### removeEntry(String entryName)
```java
public FileEntry removeEntry(String entryName) throws FileSystemException
```
**参数 / Parameters**: `entryName` - 要删除的子项名称
**返回 / Returns**: 被删除的子项，不存在则返回null
**功能 / Function**: 删除子目录项
- 查找要删除的目录项
- 标记为已删除状态
- 同步删除状态到磁盘
- 从内存缓存中移除
- 更新全局缓存

#### findEntryByName(String entryName)
```java
public FileEntry findEntryByName(String entryName)
```
**参数 / Parameters**: `entryName` - 子项名称
**返回 / Returns**: 找到的子项，不存在则返回null
**功能 / Function**: 按名称查找子目录项
- 遍历内存缓存
- 过滤已删除的条目
- 返回匹配的有效条目

#### listEntries()
```java
public List<FileEntry> listEntries()
```
**返回 / Returns**: 子项列表（返回副本，避免外部修改）
**功能 / Function**: 获取所有子目录项
- 过滤已删除的条目
- 返回有效条目的副本列表

#### isEmpty()
```java
public boolean isEmpty()
```
**返回 / Returns**: 空目录返回true，否则返回false
**功能 / Function**: 检查目录是否为空
- 检查是否有未删除的条目
- 用于目录删除前的验证

### 持久化操作 / Persistence Operations

#### loadEntriesFromDisk()
```java
public void loadEntriesFromDisk()
```
**功能 / Function**: 从磁盘加载目录项（覆盖当前缓存）
- 检查是否已分配磁盘块
- 遍历块链读取数据
- 解析目录项字符串
- 过滤已删除的条目
- 处理重复项检查
- 更新内存缓存

#### syncToDisk()
```java
public void syncToDisk() throws FileSystemException
```
**功能 / Function**: 将目录项同步到磁盘
- 检查缓存是否需要同步
- 处理空目录的特殊情况（根目录保持块分配）
- 智能块管理策略：
  - 就地更新（单块且已分配）
  - 重新分配块链（多块或未分配）
- 写入目录内容到磁盘
- 释放旧块链
- 更新目录元数据

#### writeDirectoryContent(int startBlockId)
```java
private void writeDirectoryContent(int startBlockId) throws DiskWriteException, InvalidBlockIdException
```
**参数 / Parameters**: `startBlockId` - 起始块ID
**功能 / Function**: 将目录内容写入指定的起始块
- 按块组织目录项
- 序列化目录项为字符串格式
- 填充块数据并写入磁盘
- 处理块链的遍历

### 序列化与反序列化 / Serialization and Deserialization

#### formatEntryString(FileEntry entry)
```java
private String formatEntryString(FileEntry entry)
```
**参数 / Parameters**: `entry` - 要格式化的目录项
**返回 / Returns**: 格式化的字符串
**功能 / Function**: 将FileEntry格式化为字符串（用于存储到磁盘）
**格式 / Format**: `名称|类型|起始块ID|大小|是否删除|只读|UUID`

#### parseEntryString(String entryStr)
```java
private FileEntry parseEntryString(String entryStr)
```
**参数 / Parameters**: `entryStr` - 要解析的字符串
**返回 / Returns**: 解析后的FileEntry对象，失败返回null
**功能 / Function**: 将字符串解析为FileEntry（从磁盘读取后恢复）
- 支持多种格式的向后兼容
- 处理新旧格式的差异
- 自动生成缺失的UUID
- 异常处理和日志记录

### 辅助方法 / Utility Methods

#### validateEntry(FileEntry entry)
```java
private void validateEntry(FileEntry entry) throws FileSystemException
```
**功能 / Function**: 验证目录项合法性
- 检查名称长度限制
- 验证父路径匹配

#### truncateName(String name)
```java
private String truncateName(String name)
```
**功能 / Function**: 截断过长的名称

#### updateDirEntryStartBlock(int startBlockId)
```java
private void updateDirEntryStartBlock(int startBlockId)
```
**功能 / Function**: 更新目录元数据的起始块ID

#### refreshEntries()
```java
public void refreshEntries()
```
**功能 / Function**: 刷新目录项缓存（重新从磁盘加载）

## 状态管理 / State Management

### 脏标记机制 / Dirty Flag Mechanism
```java
public boolean isDirty()           // 检查是否需要同步
public void markDirty()           // 标记为需要同步
public void setDirty(boolean dirty) // 设置脏标记状态
```

### 缓存管理 / Cache Management
```java
public List<FileEntry> getEntries()                    // 获取缓存副本
public void setEntriesCache(List<FileEntry> entriesCache) // 设置缓存内容
```

## 异常处理 / Exception Handling

### 主要异常类型 / Main Exception Types
- **FileSystemException** - 文件系统操作异常
- **DiskFullException** - 磁盘空间不足
- **DiskWriteException** - 磁盘写入错误
- **InvalidBlockIdException** - 无效的块ID

### 异常处理策略 / Exception Handling Strategy
- 参数验证异常：立即抛出，阻止无效操作
- 磁盘操作异常：包装为FileSystemException，提供详细错误信息
- 解析异常：记录警告日志，返回null，保证系统稳定性

## 线程安全 / Thread Safety

### 安全性考虑 / Safety Considerations
- **非线程安全**：Directory类本身不是线程安全的
- **上层同步**：需要在FileSystem层面进行同步控制
- **缓存一致性**：通过脏标记机制确保数据一致性

### 建议使用方式 / Recommended Usage
```java
// 在FileSystem层面进行同步
synchronized(fileSystem) {
    directory.addEntry(newEntry);
    directory.syncToDisk();
}
```

## 性能优化 / Performance Optimization

### 缓存策略 / Caching Strategy
- **延迟写入**：通过脏标记延迟磁盘同步
- **批量操作**：支持多个操作后统一同步
- **就地更新**：单块目录优先就地更新，避免重新分配

### 存储优化 / Storage Optimization
- **智能块管理**：根据目录大小动态分配块
- **空间回收**：及时释放不需要的磁盘块
- **格式压缩**：使用分隔符格式减少存储空间

## 使用示例 / Usage Examples

### 基本目录操作 / Basic Directory Operations
```java
// 创建目录管理器
FileEntry dirEntry = new FileEntry("mydir", FileEntry.EntryType.DIRECTORY, "/", -1, uuid);
Directory directory = new Directory(fileSystem, dirEntry);

// 添加文件
FileEntry fileEntry = new FileEntry("test.txt", FileEntry.EntryType.FILE, "/mydir", -1, uuid);
directory.addEntry(fileEntry);

// 查找文件
FileEntry found = directory.findEntryByName("test.txt");

// 列出所有条目
List<FileEntry> entries = directory.listEntries();

// 同步到磁盘
directory.syncToDisk();
```

### 目录删除操作 / Directory Deletion Operations
```java
// 删除文件
FileEntry removed = directory.removeEntry("test.txt");

// 检查目录是否为空
if (directory.isEmpty()) {
    // 可以安全删除目录
    parentDirectory.removeEntry(directory.getDirEntry().getName());
}
```

### 缓存刷新操作 / Cache Refresh Operations
```java
// 刷新缓存（重新从磁盘加载）
directory.refreshEntries();

// 检查是否需要同步
if (directory.isDirty()) {
    directory.syncToDisk();
}
```

## 设计模式 / Design Patterns

### 使用的设计模式 / Applied Design Patterns
- **缓存模式 (Cache Pattern)**: 内存缓存目录项，提高访问性能
- **延迟写入模式 (Lazy Write Pattern)**: 通过脏标记延迟磁盘同步
- **策略模式 (Strategy Pattern)**: 不同情况下的块分配策略

### 架构优势 / Architectural Advantages
- **性能优化**: 减少磁盘I/O操作
- **数据一致性**: 确保内存和磁盘数据同步
- **可扩展性**: 支持不同的存储格式和策略
- **容错性**: 完善的异常处理和恢复机制