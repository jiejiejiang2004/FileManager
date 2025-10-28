# 文件管理器技术架构文档 / File Manager Technical Architecture

## 中文版 / Chinese Version

### 系统架构概述

本文件管理器采用分层架构设计，遵循MVC（Model-View-Controller）设计模式，确保代码的可维护性和可扩展性。

#### 核心架构层次

```
┌─────────────────────────────────────┐
│         表示层 (Presentation)        │
│  ┌─────────────┐  ┌─────────────┐   │
│  │   FXML UI   │  │ Controllers │   │
│  └─────────────┘  └─────────────┘   │
├─────────────────────────────────────┤
│        业务逻辑层 (Business)      	   │
│  ┌─────────────┐  ┌─────────────┐   │
│  │ FileSystem  │  │  Directory  │   │
│  └─────────────┘  └─────────────┘   │
├─────────────────────────────────────┤
│        数据访问层 (Data Access)      │
│  ┌─────────────┐  ┌─────────────┐   │
│  │    Disk     │  │     FAT     │   │
│  └─────────────┘  └─────────────┘   │
├─────────────────────────────────────┤
│        基础设施层 (Infrastructure)   │
│  ┌─────────────┐  ┌─────────────┐   │
│  │   Buffer    │  │   Thread    │   │
│  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────┘
```

### 核心组件详解

#### 1. 磁盘模拟层 (Disk Simulation Layer)

**Disk.java**
- 功能：模拟物理磁盘的块级读写操作
- 特性：
  - 固定块大小（默认512字节）
  - 支持随机访问
  - 异常处理机制
- 关键方法：
  ```java
  public byte[] readBlock(int blockId)
  public void writeBlock(int blockId, byte[] data)
  ```

**Buffer.java**
- 功能：提供磁盘I/O缓冲机制
- 特性：
  - LRU缓存策略
  - 异步刷新机制
  - 提高读写性能

#### 2. 文件分配表 (File Allocation Table)

**FAT.java**
- 功能：管理磁盘块的分配和链接关系
- 特性：
  - 链式存储结构
  - 空闲块管理
  - 文件块链维护
- 数据结构：
  ```
  FAT表项：[下一块ID | 文件结束标记 | 空闲标记]
  ```

#### 3. 目录管理系统

**Directory.java**
- 功能：实现多级目录结构
- 特性：
  - 树形目录结构
  - 目录项管理
  - 路径解析
- 目录项结构：
  ```java
  class DirectoryEntry {
      String name;        // 文件/目录名
      boolean isDirectory; // 是否为目录
      int startBlock;     // 起始块号
      int size;          // 文件大小
      Date createTime;   // 创建时间
  }
  ```

#### 4. 文件系统核心

**FileSystem.java**
- 功能：提供统一的文件系统API
- 主要接口：
  ```java
  public void createFile(String path, String content)
  public String readFile(String path)
  public void deleteFile(String path)
  public void createDirectory(String path)
  public List<FileEntry> listDirectory(String path)
  ```

### 用户界面架构

#### 1. 视图模式支持
- **列表视图**：传统的文件列表显示
- **图标视图**：图标化的文件显示

#### 2. 交互设计
- **双击行为**：默认查看文件内容（只读模式）
- **右键菜单**：
  - 文件：查看、编辑、删除、属性
  - 目录：进入、删除、属性
- **菜单栏**：文件操作、视图切换、工具选项

#### 3. 对话框系统
- **文件查看对话框**：只读模式，安全查看文件内容
- **文件编辑对话框**：完整编辑功能，支持保存
- **新建文件对话框**：创建新文件
- **重命名对话框**：文件/目录重命名

### 多线程架构

#### 1. 后台任务
- **FileReadTask**：异步文件读取
- **FileWriteTask**：异步文件写入
- **BufferFlushTask**：定期缓冲区刷新

#### 2. 线程安全
- 使用JavaFX Platform.runLater()更新UI
- 文件系统操作的同步控制
- 缓冲区的线程安全访问

---

## English Version

### System Architecture Overview

This file manager adopts a layered architecture design following the MVC (Model-View-Controller) pattern to ensure code maintainability and extensibility.

#### Core Architecture Layers

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  ┌─────────────┐  ┌─────────────┐   │
│  │   FXML UI   │  │ Controllers │   │
│  └─────────────┘  └─────────────┘   │
├─────────────────────────────────────┤
│          Business Layer             │
│  ┌─────────────┐  ┌─────────────┐   │
│  │ FileSystem  │  │  Directory  │   │
│  └─────────────┘  └─────────────┘   │
├─────────────────────────────────────┤
│         Data Access Layer           │
│  ┌─────────────┐  ┌─────────────┐   │
│  │    Disk     │  │     FAT     │   │
│  └─────────────┘  └─────────────┘   │
├─────────────────────────────────────┤
│       Infrastructure Layer          │
│  ┌─────────────┐  ┌─────────────┐   │
│  │   Buffer    │  │   Thread    │   │
│  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────┘
```

### Core Components

#### 1. Disk Simulation Layer

**Disk.java**
- Function: Simulates physical disk block-level read/write operations
- Features:
  - Fixed block size (default 512 bytes)
  - Random access support
  - Exception handling mechanism
- Key methods:
  ```java
  public byte[] readBlock(int blockId)
  public void writeBlock(int blockId, byte[] data)
  ```

**Buffer.java**
- Function: Provides disk I/O buffering mechanism
- Features:
  - LRU cache strategy
  - Asynchronous flush mechanism
  - Improved read/write performance

#### 2. File Allocation Table

**FAT.java**
- Function: Manages disk block allocation and linking relationships
- Features:
  - Linked storage structure
  - Free block management
  - File block chain maintenance
- Data structure:
  ```
  FAT Entry: [Next Block ID | End of File Mark | Free Mark]
  ```

#### 3. Directory Management System

**Directory.java**
- Function: Implements multi-level directory structure
- Features:
  - Tree-like directory structure
  - Directory entry management
  - Path resolution
- Directory entry structure:
  ```java
  class DirectoryEntry {
      String name;        // File/directory name
      boolean isDirectory; // Is directory flag
      int startBlock;     // Starting block number
      int size;          // File size
      Date createTime;   // Creation time
  }
  ```

#### 4. File System Core

**FileSystem.java**
- Function: Provides unified file system API
- Main interfaces:
  ```java
  public void createFile(String path, String content)
  public String readFile(String path)
  public void deleteFile(String path)
  public void createDirectory(String path)
  public List<FileEntry> listDirectory(String path)
  ```

### User Interface Architecture

#### 1. View Mode Support
- **List View**: Traditional file list display
- **Icon View**: Iconified file display

#### 2. Interaction Design
- **Double-click behavior**: Default to view file content (read-only mode)
- **Context menu**:
  - Files: View, Edit, Delete, Properties
  - Directories: Enter, Delete, Properties
- **Menu bar**: File operations, view switching, tool options

#### 3. Dialog System
- **File View Dialog**: Read-only mode for safe file content viewing
- **File Edit Dialog**: Full editing functionality with save support
- **New File Dialog**: Create new files
- **Rename Dialog**: File/directory renaming

### Multi-threading Architecture

#### 1. Background Tasks
- **FileReadTask**: Asynchronous file reading
- **FileWriteTask**: Asynchronous file writing
- **BufferFlushTask**: Periodic buffer flushing

#### 2. Thread Safety
- Use JavaFX Platform.runLater() for UI updates
- Synchronization control for file system operations
- Thread-safe buffer access