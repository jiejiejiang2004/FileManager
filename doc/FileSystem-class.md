# FileSystem 类技术文档 / FileSystem Class Technical Documentation

## 中文版 / Chinese Version

### 类概述

`FileSystem` 是文件管理器的核心类，整合了 `Disk` 和 `FAT` 组件，提供完整的文件系统操作接口。该类实现了文件和目录的创建、删除、读写等所有基本操作，并管理文件系统的挂载状态和内存缓存。

### 类声明
```java
public class FileSystem
```

**包路径**: `org.jiejiejiang.filemanager.core`

### 核心特性

- **统一接口**: 提供文件和目录操作的统一入口点
- **内存缓存**: 使用 `ConcurrentHashMap` 缓存文件/目录元数据，支持多线程安全
- **已打开文件表**: 管理最多5个同时打开的文件
- **路径管理**: 支持完整的路径解析和验证
- **异常处理**: 完善的异常处理机制

### 常量定义

| 常量名 | 值 | 描述 |
|--------|-----|------|
| `ROOT_PATH` | `"/"` | 根目录路径 |
| `ROOT_NAME` | `""` | 根目录Entry名称（空字符串） |
| `DIR_ENTRY_SEPARATOR` | `"|"` | 目录项分隔符 |
| `MAX_DIR_ENTRY_LENGTH` | `128` | 单个目录项最大长度（字节） |

### 核心字段

#### 依赖组件
```java
private final Disk disk;          // 底层磁盘
private final FAT fat;            // 文件分配表
private final int blockSize;      // 磁盘块大小
```

#### 缓存和状态
```java
private final Map<String, FileEntry> entryCache;  // 元数据缓存
private FileEntry rootDir;                        // 根目录
private final OpenFileTable oft;                  // 已打开文件表
private boolean isMounted;                        // 挂载状态
```

### 构造方法

#### FileSystem(Disk disk, FAT fat)
```java
public FileSystem(Disk disk, FAT fat) throws FileSystemException
```

**参数**:
- `disk`: 已初始化的磁盘实例
- `fat`: 已初始化的FAT实例

**异常**:
- `FileSystemException`: 磁盘或FAT未初始化时抛出

**功能**: 初始化文件系统，关联磁盘和FAT组件

### 核心方法

#### 文件系统管理

##### mount()
```java
public void mount() throws FileSystemException
```
**功能**: 挂载文件系统，初始化根目录，加载元数据
- 清空缓存确保干净状态
- 为根目录分配磁盘块（通常使用块2）
- 创建根目录Entry并同步到磁盘
- 保存FAT表到磁盘
- 标记文件系统为已挂载状态

##### unmount()
```java
public void unmount() throws FileSystemException
```
**功能**: 卸载文件系统，清理资源
- 关闭所有打开的文件
- 持久化FAT表
- 关闭磁盘
- 清理缓存并标记为未挂载

#### 文件操作

##### createFile(String fullPath)
```java
public FileEntry createFile(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 文件完整路径
**返回**: 创建的文件Entry
**功能**: 创建新文件
- 解析路径，验证父目录存在
- 检查文件是否已存在
- 分配磁盘块
- 创建文件元数据并缓存
- 更新父目录

##### deleteFile(String fullPath)
```java
public void deleteFile(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 文件完整路径
**功能**: 删除文件
- 验证文件存在且为文件类型
- 释放文件占用的所有磁盘块
- 标记文件为已删除
- 更新父目录修改时间

##### readFile(String fullPath)
```java
public byte[] readFile(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 文件完整路径
**返回**: 文件内容字节数组
**功能**: 读取文件完整内容
- 验证文件存在且为文件类型
- 遍历文件的块链读取数据
- 返回完整文件内容

##### writeFile(String fullPath, byte[] content)
```java
public void writeFile(String fullPath, byte[] content) throws FileSystemException
```
**参数**: 
- `fullPath` - 文件完整路径
- `content` - 要写入的内容
**功能**: 写入文件内容
- 验证文件存在且为文件类型
- 根据内容大小分配或释放磁盘块
- 将内容写入磁盘块
- 更新文件大小和修改时间

#### 目录操作

##### createDirectory(String fullPath)
```java
public FileEntry createDirectory(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 目录完整路径
**返回**: 创建的目录Entry
**功能**: 创建新目录
- 解析路径，验证父目录存在
- 检查目录是否已存在
- 分配磁盘块
- 创建目录元数据并初始化空目录
- 更新父目录

##### deleteDirectory(String fullPath)
```java
public void deleteDirectory(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 目录完整路径
**功能**: 删除空目录
- 验证目录存在且为目录类型
- 检查目录是否为空
- 释放目录占用的磁盘块
- 从父目录中移除

##### listDirectory(String fullPath)
```java
public List<FileEntry> listDirectory(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 目录完整路径
**返回**: 目录下的FileEntry列表
**功能**: 列出目录内容
- 验证目录存在且为目录类型
- 从缓存中筛选子条目
- 过滤已删除的条目
- 返回有效的子条目列表

#### 已打开文件表操作

##### openFile(String fullPath, String mode)
```java
public int openFile(String fullPath, String mode) throws FileSystemException
```
**参数**: 
- `fullPath` - 文件完整路径
- `mode` - 打开模式（READ, WRITE, READ_WRITE）
**返回**: OFT索引（0-4），失败返回-1
**功能**: 打开文件并加入OFT管理

##### closeFile(int oftIndex)
```java
public void closeFile(int oftIndex) throws FileSystemException
```
**参数**: `oftIndex` - OFT索引
**功能**: 关闭指定索引的文件

##### closeFile(String fullPath)
```java
public void closeFile(String fullPath) throws FileSystemException
```
**参数**: `fullPath` - 文件完整路径
**功能**: 根据路径关闭文件

#### 工具方法

##### getEntry(String fullPath)
```java
public FileEntry getEntry(String fullPath)
```
**参数**: `fullPath` - 完整路径
**返回**: 对应的FileEntry，不存在返回null
**功能**: 从缓存中获取文件/目录Entry

##### isMounted()
```java
public boolean isMounted()
```
**返回**: 文件系统是否已挂载
**功能**: 检查文件系统挂载状态

### 异常处理

该类主要抛出以下异常：
- `FileSystemException`: 通用文件系统异常
- `DiskFullException`: 磁盘空间不足
- `InvalidPathException`: 路径格式无效
- `DiskWriteException`: 磁盘写入失败

### 线程安全

- 使用 `ConcurrentHashMap` 作为元数据缓存，支持多线程并发访问
- 已打开文件表（OFT）内部实现线程安全
- 磁盘和FAT操作通过底层组件保证线程安全

### 使用示例

```java
// 初始化文件系统
Disk disk = new Disk("disk.img", 1024, 1000);
FAT fat = new FAT(disk);
FileSystem fs = new FileSystem(disk, fat);

// 挂载文件系统
fs.mount();

// 创建文件
FileEntry file = fs.createFile("/test.txt");
fs.writeFile("/test.txt", "Hello World".getBytes());

// 读取文件
byte[] content = fs.readFile("/test.txt");

// 创建目录
fs.createDirectory("/docs");

// 列出根目录
List<FileEntry> entries = fs.listDirectory("/");

// 卸载文件系统
fs.unmount();
```

---

## English Version

### Class Overview

`FileSystem` is the core class of the file manager, integrating `Disk` and `FAT` components to provide a complete file system operation interface. This class implements all basic operations for files and directories including creation, deletion, reading, and writing, while managing the mount status and memory cache of the file system.

### Class Declaration
```java
public class FileSystem
```

**Package**: `org.jiejiejiang.filemanager.core`

### Core Features

- **Unified Interface**: Provides a unified entry point for file and directory operations
- **Memory Cache**: Uses `ConcurrentHashMap` to cache file/directory metadata with thread-safe support
- **Open File Table**: Manages up to 5 simultaneously opened files
- **Path Management**: Supports complete path parsing and validation
- **Exception Handling**: Comprehensive exception handling mechanism

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `ROOT_PATH` | `"/"` | Root directory path |
| `ROOT_NAME` | `""` | Root directory entry name (empty string) |
| `DIR_ENTRY_SEPARATOR` | `"|"` | Directory entry separator |
| `MAX_DIR_ENTRY_LENGTH` | `128` | Maximum length of single directory entry (bytes) |

### Core Fields

#### Dependency Components
```java
private final Disk disk;          // Underlying disk
private final FAT fat;            // File allocation table
private final int blockSize;      // Disk block size
```

#### Cache and State
```java
private final Map<String, FileEntry> entryCache;  // Metadata cache
private FileEntry rootDir;                        // Root directory
private final OpenFileTable oft;                  // Open file table
private boolean isMounted;                        // Mount status
```

### Constructor

#### FileSystem(Disk disk, FAT fat)
```java
public FileSystem(Disk disk, FAT fat) throws FileSystemException
```

**Parameters**:
- `disk`: Initialized disk instance
- `fat`: Initialized FAT instance

**Exceptions**:
- `FileSystemException`: Thrown when disk or FAT is not initialized

**Function**: Initialize file system and associate disk and FAT components

### Core Methods

#### File System Management

##### mount()
```java
public void mount() throws FileSystemException
```
**Function**: Mount file system, initialize root directory, load metadata
- Clear cache to ensure clean state
- Allocate disk block for root directory (typically block 2)
- Create root directory entry and sync to disk
- Save FAT table to disk
- Mark file system as mounted

##### unmount()
```java
public void unmount() throws FileSystemException
```
**Function**: Unmount file system and clean up resources
- Close all open files
- Persist FAT table
- Close disk
- Clear cache and mark as unmounted

#### File Operations

##### createFile(String fullPath)
```java
public FileEntry createFile(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete file path
**Returns**: Created file entry
**Function**: Create new file
- Parse path and verify parent directory exists
- Check if file already exists
- Allocate disk block
- Create file metadata and cache
- Update parent directory

##### deleteFile(String fullPath)
```java
public void deleteFile(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete file path
**Function**: Delete file
- Verify file exists and is a file type
- Free all disk blocks occupied by the file
- Mark file as deleted
- Update parent directory modification time

##### readFile(String fullPath)
```java
public byte[] readFile(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete file path
**Returns**: File content byte array
**Function**: Read complete file content
- Verify file exists and is a file type
- Traverse file's block chain to read data
- Return complete file content

##### writeFile(String fullPath, byte[] content)
```java
public void writeFile(String fullPath, byte[] content) throws FileSystemException
```
**Parameters**: 
- `fullPath` - Complete file path
- `content` - Content to write
**Function**: Write file content
- Verify file exists and is a file type
- Allocate or free disk blocks based on content size
- Write content to disk blocks
- Update file size and modification time

#### Directory Operations

##### createDirectory(String fullPath)
```java
public FileEntry createDirectory(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete directory path
**Returns**: Created directory entry
**Function**: Create new directory
- Parse path and verify parent directory exists
- Check if directory already exists
- Allocate disk block
- Create directory metadata and initialize empty directory
- Update parent directory

##### deleteDirectory(String fullPath)
```java
public void deleteDirectory(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete directory path
**Function**: Delete empty directory
- Verify directory exists and is a directory type
- Check if directory is empty
- Free disk blocks occupied by directory
- Remove from parent directory

##### listDirectory(String fullPath)
```java
public List<FileEntry> listDirectory(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete directory path
**Returns**: List of FileEntry in the directory
**Function**: List directory contents
- Verify directory exists and is a directory type
- Filter child entries from cache
- Filter out deleted entries
- Return valid child entries

#### Open File Table Operations

##### openFile(String fullPath, String mode)
```java
public int openFile(String fullPath, String mode) throws FileSystemException
```
**Parameters**: 
- `fullPath` - Complete file path
- `mode` - Open mode (READ, WRITE, READ_WRITE)
**Returns**: OFT index (0-4), returns -1 on failure
**Function**: Open file and add to OFT management

##### closeFile(int oftIndex)
```java
public void closeFile(int oftIndex) throws FileSystemException
```
**Parameters**: `oftIndex` - OFT index
**Function**: Close file at specified index

##### closeFile(String fullPath)
```java
public void closeFile(String fullPath) throws FileSystemException
```
**Parameters**: `fullPath` - Complete file path
**Function**: Close file by path

#### Utility Methods

##### getEntry(String fullPath)
```java
public FileEntry getEntry(String fullPath)
```
**Parameters**: `fullPath` - Complete path
**Returns**: Corresponding FileEntry, null if not exists
**Function**: Get file/directory entry from cache

##### isMounted()
```java
public boolean isMounted()
```
**Returns**: Whether file system is mounted
**Function**: Check file system mount status

### Exception Handling

This class primarily throws the following exceptions:
- `FileSystemException`: General file system exception
- `DiskFullException`: Insufficient disk space
- `InvalidPathException`: Invalid path format
- `DiskWriteException`: Disk write failure

### Thread Safety

- Uses `ConcurrentHashMap` as metadata cache, supporting multi-threaded concurrent access
- Open File Table (OFT) implements thread safety internally
- Disk and FAT operations ensure thread safety through underlying components

### Usage Example

```java
// Initialize file system
Disk disk = new Disk("disk.img", 1024, 1000);
FAT fat = new FAT(disk);
FileSystem fs = new FileSystem(disk, fat);

// Mount file system
fs.mount();

// Create file
FileEntry file = fs.createFile("/test.txt");
fs.writeFile("/test.txt", "Hello World".getBytes());

// Read file
byte[] content = fs.readFile("/test.txt");

// Create directory
fs.createDirectory("/docs");

// List root directory
List<FileEntry> entries = fs.listDirectory("/");

// Unmount file system
fs.unmount();
```