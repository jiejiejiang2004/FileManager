# API 参考文档 / API Reference

## 中文版 / Chinese Version

### 核心API接口

#### FileSystem 类

文件系统的主要接口类，提供所有文件和目录操作的统一入口。

##### 构造方法
```java
public FileSystem(String diskPath)
```
- **参数**: `diskPath` - 磁盘镜像文件路径
- **功能**: 初始化文件系统，加载或创建磁盘镜像

##### 文件操作方法

**创建文件**
```java
public void createFile(String path, String content) throws FileSystemException
```
- **参数**: 
  - `path` - 文件路径
  - `content` - 文件内容
- **异常**: `FileSystemException` - 文件创建失败
- **功能**: 在指定路径创建文件并写入内容

**读取文件**
```java
public String readFile(String path) throws FileNotFoundException
```
- **参数**: `path` - 文件路径
- **返回**: 文件内容字符串
- **异常**: `FileNotFoundException` - 文件不存在
- **功能**: 读取指定文件的全部内容

**删除文件**
```java
public void deleteFile(String path) throws FileNotFoundException
```
- **参数**: `path` - 文件路径
- **异常**: `FileNotFoundException` - 文件不存在
- **功能**: 删除指定文件

**文件是否存在**
```java
public boolean fileExists(String path)
```
- **参数**: `path` - 文件路径
- **返回**: 文件是否存在
- **功能**: 检查文件是否存在

##### 目录操作方法

**创建目录**
```java
public void createDirectory(String path) throws FileSystemException
```
- **参数**: `path` - 目录路径
- **异常**: `FileSystemException` - 目录创建失败
- **功能**: 创建指定路径的目录

**列出目录内容**
```java
public List<FileEntry> listDirectory(String path) throws FileNotFoundException
```
- **参数**: `path` - 目录路径
- **返回**: 目录项列表
- **异常**: `FileNotFoundException` - 目录不存在
- **功能**: 获取目录下所有文件和子目录

**删除目录**
```java
public void deleteDirectory(String path) throws FileSystemException
```
- **参数**: `path` - 目录路径
- **异常**: `FileSystemException` - 目录删除失败
- **功能**: 删除指定目录（必须为空目录）

#### Directory 类

目录管理类，处理目录结构和目录项操作。

##### 主要方法

**添加目录项**
```java
public void addEntry(DirectoryEntry entry) throws DiskFullException
```
- **参数**: `entry` - 目录项对象
- **异常**: `DiskFullException` - 磁盘空间不足
- **功能**: 向目录中添加新的文件或子目录项

**删除目录项**
```java
public boolean removeEntry(String name)
```
- **参数**: `name` - 文件或目录名
- **返回**: 是否删除成功
- **功能**: 从目录中删除指定名称的项

**查找目录项**
```java
public DirectoryEntry findEntry(String name)
```
- **参数**: `name` - 文件或目录名
- **返回**: 目录项对象，未找到返回null
- **功能**: 在目录中查找指定名称的项

#### Disk 类

磁盘模拟类，提供底层的块级读写操作。

##### 主要方法

**读取块**
```java
public byte[] readBlock(int blockId) throws DiskReadException
```
- **参数**: `blockId` - 块号
- **返回**: 块数据字节数组
- **异常**: `DiskReadException` - 读取失败
- **功能**: 读取指定块的数据

**写入块**
```java
public void writeBlock(int blockId, byte[] data) throws DiskWriteException
```
- **参数**: 
  - `blockId` - 块号
  - `data` - 要写入的数据
- **异常**: `DiskWriteException` - 写入失败
- **功能**: 向指定块写入数据

#### FAT 类

文件分配表类，管理磁盘块的分配和链接。

##### 主要方法

**分配块**
```java
public int allocateBlock() throws DiskFullException
```
- **返回**: 分配的块号
- **异常**: `DiskFullException` - 磁盘已满
- **功能**: 分配一个空闲块

**释放块**
```java
public void freeBlock(int blockId)
```
- **参数**: `blockId` - 要释放的块号
- **功能**: 释放指定块，标记为空闲

**获取下一块**
```java
public int getNextBlock(int blockId)
```
- **参数**: `blockId` - 当前块号
- **返回**: 下一块号，-1表示文件结束
- **功能**: 获取文件链中的下一个块

### 异常类型

#### FileSystemException
文件系统通用异常，所有文件系统相关异常的基类。

#### FileNotFoundException
文件未找到异常，当访问不存在的文件时抛出。

#### DiskFullException
磁盘已满异常，当磁盘空间不足时抛出。

#### DiskReadException / DiskWriteException
磁盘读写异常，当磁盘I/O操作失败时抛出。

---

## English Version

### Core API Interfaces

#### FileSystem Class

The main interface class for the file system, providing unified entry points for all file and directory operations.

##### Constructor
```java
public FileSystem(String diskPath)
```
- **Parameter**: `diskPath` - Path to disk image file
- **Function**: Initialize file system, load or create disk image

##### File Operation Methods

**Create File**
```java
public void createFile(String path, String content) throws FileSystemException
```
- **Parameters**: 
  - `path` - File path
  - `content` - File content
- **Exception**: `FileSystemException` - File creation failed
- **Function**: Create file at specified path and write content

**Read File**
```java
public String readFile(String path) throws FileNotFoundException
```
- **Parameter**: `path` - File path
- **Return**: File content string
- **Exception**: `FileNotFoundException` - File does not exist
- **Function**: Read entire content of specified file

**Delete File**
```java
public void deleteFile(String path) throws FileNotFoundException
```
- **Parameter**: `path` - File path
- **Exception**: `FileNotFoundException` - File does not exist
- **Function**: Delete specified file

**File Exists**
```java
public boolean fileExists(String path)
```
- **Parameter**: `path` - File path
- **Return**: Whether file exists
- **Function**: Check if file exists

##### Directory Operation Methods

**Create Directory**
```java
public void createDirectory(String path) throws FileSystemException
```
- **Parameter**: `path` - Directory path
- **Exception**: `FileSystemException` - Directory creation failed
- **Function**: Create directory at specified path

**List Directory**
```java
public List<FileEntry> listDirectory(String path) throws FileNotFoundException
```
- **Parameter**: `path` - Directory path
- **Return**: List of directory entries
- **Exception**: `FileNotFoundException` - Directory does not exist
- **Function**: Get all files and subdirectories in directory

**Delete Directory**
```java
public void deleteDirectory(String path) throws FileSystemException
```
- **Parameter**: `path` - Directory path
- **Exception**: `FileSystemException` - Directory deletion failed
- **Function**: Delete specified directory (must be empty)

#### Directory Class

Directory management class handling directory structure and entry operations.

##### Main Methods

**Add Entry**
```java
public void addEntry(DirectoryEntry entry) throws DiskFullException
```
- **Parameter**: `entry` - Directory entry object
- **Exception**: `DiskFullException` - Insufficient disk space
- **Function**: Add new file or subdirectory entry to directory

**Remove Entry**
```java
public boolean removeEntry(String name)
```
- **Parameter**: `name` - File or directory name
- **Return**: Whether removal was successful
- **Function**: Remove entry with specified name from directory

**Find Entry**
```java
public DirectoryEntry findEntry(String name)
```
- **Parameter**: `name` - File or directory name
- **Return**: Directory entry object, null if not found
- **Function**: Find entry with specified name in directory

#### Disk Class

Disk simulation class providing low-level block read/write operations.

##### Main Methods

**Read Block**
```java
public byte[] readBlock(int blockId) throws DiskReadException
```
- **Parameter**: `blockId` - Block number
- **Return**: Block data byte array
- **Exception**: `DiskReadException` - Read failed
- **Function**: Read data from specified block

**Write Block**
```java
public void writeBlock(int blockId, byte[] data) throws DiskWriteException
```
- **Parameters**: 
  - `blockId` - Block number
  - `data` - Data to write
- **Exception**: `DiskWriteException` - Write failed
- **Function**: Write data to specified block

#### FAT Class

File Allocation Table class managing disk block allocation and linking.

##### Main Methods

**Allocate Block**
```java
public int allocateBlock() throws DiskFullException
```
- **Return**: Allocated block number
- **Exception**: `DiskFullException` - Disk is full
- **Function**: Allocate a free block

**Free Block**
```java
public void freeBlock(int blockId)
```
- **Parameter**: `blockId` - Block number to free
- **Function**: Free specified block, mark as available

**Get Next Block**
```java
public int getNextBlock(int blockId)
```
- **Parameter**: `blockId` - Current block number
- **Return**: Next block number, -1 indicates end of file
- **Function**: Get next block in file chain

### Exception Types

#### FileSystemException
General file system exception, base class for all file system related exceptions.

#### FileNotFoundException
File not found exception, thrown when accessing non-existent files.

#### DiskFullException
Disk full exception, thrown when disk space is insufficient.

#### DiskReadException / DiskWriteException
Disk I/O exceptions, thrown when disk read/write operations fail.