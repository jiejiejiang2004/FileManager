# 异常处理类技术文档 / Exception Handling Classes Technical Documentation

## 概述 / Overview

**中文概述**：
异常处理类是文件管理系统中的错误管理组件，提供了完整的异常体系结构和统一的错误处理机制。系统采用分层异常设计，包括磁盘操作异常（`DiskException`及其子类）、文件系统异常（`FileSystemException`）和运行时异常等。这些异常类不仅提供了详细的错误信息和上下文，还支持异常链传递，确保错误的可追溯性。通过统一的异常处理策略，系统能够优雅地处理各种错误情况，提供用户友好的错误反馈，并保证系统的稳定性和可靠性。

**English Overview**：
The exception handling classes serve as error management components in the file management system, providing a complete exception hierarchy and unified error handling mechanisms. The system adopts a layered exception design, including disk operation exceptions (`DiskException` and its subclasses), file system exceptions (`FileSystemException`), and runtime exceptions. These exception classes not only provide detailed error information and context but also support exception chaining to ensure error traceability. Through unified exception handling strategies, the system can gracefully handle various error conditions, provide user-friendly error feedback, and ensure system stability and reliability.

## 异常体系结构 / Exception Hierarchy

### 异常继承关系图 / Exception Inheritance Diagram

```
java.lang.Throwable
├── java.lang.Exception
│   ├── FileSystemException (文件系统异常)
│   └── DiskException (磁盘操作基础异常)
│       ├── DiskInitializeException (磁盘初始化异常)
│       ├── DiskReadException (磁盘读取异常)
│       ├── DiskWriteException (磁盘写入异常)
│       └── DiskFullException (磁盘空间不足异常)
└── java.lang.RuntimeException
    ├── InvalidBlockIdException (无效块ID异常)
    ├── InvalidPathException (无效路径异常)
    ├── FileNotFoundException (文件未找到异常)
    └── ReadOnlyException (只读文件操作异常)
```

### 异常分类 / Exception Classification

#### 1. 检查型异常 (Checked Exceptions)
- **FileSystemException**: 文件系统层面的通用异常
- **DiskException**: 磁盘操作相关的基础异常
- **DiskInitializeException**: 磁盘初始化失败异常
- **DiskReadException**: 磁盘读取操作异常
- **DiskWriteException**: 磁盘写入操作异常
- **DiskFullException**: 磁盘空间不足异常

#### 2. 运行时异常 (Runtime Exceptions)
- **InvalidBlockIdException**: 无效块ID异常
- **InvalidPathException**: 无效路径格式异常
- **FileNotFoundException**: 文件未找到异常
- **ReadOnlyException**: 只读文件修改异常

## 核心异常类详解 / Core Exception Classes

## DiskException 磁盘操作基础异常 / Disk Operation Base Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘操作基础异常
 * 所有磁盘相关异常（读、写、块管理等）的父类，用于统一捕获磁盘操作错误
 */
public class DiskException extends RuntimeException {
    // 磁盘异常基类实现
}
```

### 核心特性 / Core Features

#### 1. 异常层次管理 / Exception Hierarchy Management
- **统一基类**：作为所有磁盘操作异常的父类
- **异常分类**：支持按操作类型细分异常
- **链式传递**：支持异常原因的链式传递

#### 2. 错误信息封装 / Error Information Encapsulation
- **详细描述**：提供详细的错误描述信息
- **上下文信息**：包含操作上下文和环境信息
- **根源异常**：保留底层异常的完整信息

#### 3. 调试支持 / Debugging Support
- **堆栈跟踪**：完整的异常堆栈信息
- **错误定位**：精确的错误发生位置
- **诊断信息**：有助于问题诊断的详细信息

### 构造方法 / Constructor Methods

```java
/**
 * 无参构造器
 */
public DiskException() {
    super();
}

/**
 * 带错误信息的构造器
 * @param message 错误描述信息
 */
public DiskException(String message) {
    super(message);
}

/**
 * 带错误信息和根源异常的构造器
 * @param message 错误描述信息
 * @param cause 根源异常（如IOException等底层错误）
 */
public DiskException(String message, Throwable cause) {
    super(message, cause);
}

/**
 * 带根源异常的构造器
 * @param cause 根源异常
 */
public DiskException(Throwable cause) {
    super(cause);
}
```

### 使用场景 / Usage Scenarios

```java
// 1. 基础磁盘操作异常
try {
    disk.performOperation();
} catch (IOException e) {
    throw new DiskException("磁盘操作失败", e);
}

// 2. 作为其他磁盘异常的基类
public class DiskReadException extends DiskException {
    public DiskReadException(String message) {
        super(message);
    }
}

// 3. 统一异常捕获
try {
    performDiskOperations();
} catch (DiskException e) {
    // 统一处理所有磁盘相关异常
    handleDiskError(e);
}
```

## DiskReadException 磁盘读取异常 / Disk Read Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘读取异常
 * 用于表示读取磁盘块时发生的错误（如IO错误、块ID无效、数据损坏等）
 */
public class DiskReadException extends DiskException {
    // 磁盘读取异常实现
}
```

### 核心特性 / Core Features

#### 1. 读取错误分类 / Read Error Classification
- **IO错误**：底层文件系统读取失败
- **块ID无效**：访问超出范围的块ID
- **数据损坏**：读取到损坏或不一致的数据
- **权限错误**：缺乏读取权限

#### 2. 上下文信息 / Context Information
- **块ID信息**：包含发生错误的具体块ID
- **操作详情**：描述具体的读取操作
- **错误原因**：详细的错误原因分析

#### 3. 错误恢复支持 / Error Recovery Support
- **重试提示**：指示是否可以重试操作
- **替代方案**：提供可能的替代读取方案
- **数据恢复**：支持部分数据恢复机制

### 构造方法 / Constructor Methods

```java
/**
 * 带错误信息的构造器
 * @param message 错误描述信息
 */
public DiskReadException(String message) {
    super(message);
}

/**
 * 带错误信息和根源异常的构造器
 * @param message 错误描述信息
 * @param cause 根源异常（如IOException等）
 */
public DiskReadException(String message, Throwable cause) {
    super(message, cause);
}

/**
 * 带块ID和错误信息的构造器（常用）
 * @param blockId 发生错误的块ID
 * @param message 错误描述信息
 */
public DiskReadException(int blockId, String message) {
    super("块ID: " + blockId + "，" + message);
}

/**
 * 带块ID、错误信息和根源异常的构造器
 * @param blockId 发生错误的块ID
 * @param message 错误描述信息
 * @param cause 根源异常
 */
public DiskReadException(int blockId, String message, Throwable cause) {
    super("块ID: " + blockId + "，" + message, cause);
}
```

### 使用示例 / Usage Examples

```java
// 1. 基本读取错误
public byte[] readBlock(int blockId) throws DiskReadException {
    try {
        return performBlockRead(blockId);
    } catch (IOException e) {
        throw new DiskReadException(blockId, "IO读取失败", e);
    }
}

// 2. 块ID验证错误
public byte[] readBlock(int blockId) throws DiskReadException {
    if (blockId < 0 || blockId >= totalBlocks) {
        throw new DiskReadException(blockId, "块ID超出有效范围 [0, " + (totalBlocks - 1) + "]");
    }
    // 继续读取操作...
}

// 3. 数据完整性检查错误
public byte[] readBlock(int blockId) throws DiskReadException {
    byte[] data = performBlockRead(blockId);
    if (!validateChecksum(data)) {
        throw new DiskReadException(blockId, "数据校验失败，可能存在数据损坏");
    }
    return data;
}
```

## DiskWriteException 磁盘写入异常 / Disk Write Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘写入异常
 * 用于表示写入磁盘块时发生的错误（如IO错误、块ID无效等）
 */
public class DiskWriteException extends DiskException {
    // 磁盘写入异常实现
}
```

### 核心特性 / Core Features

#### 1. 写入错误分类 / Write Error Classification
- **IO错误**：底层文件系统写入失败
- **空间不足**：磁盘空间不足导致写入失败
- **权限错误**：缺乏写入权限或文件只读
- **数据验证失败**：写入数据验证失败

#### 2. 事务支持 / Transaction Support
- **原子性保证**：确保写入操作的原子性
- **回滚机制**：写入失败时的数据回滚
- **一致性检查**：写入后的数据一致性验证

#### 3. 性能监控 / Performance Monitoring
- **写入统计**：记录写入操作的性能数据
- **错误频率**：监控写入错误的发生频率
- **性能分析**：提供写入性能分析信息

### 构造方法 / Constructor Methods

```java
/**
 * 带错误信息的构造器
 * @param message 错误描述信息
 */
public DiskWriteException(String message) {
    super(message);
}

/**
 * 带错误信息和根源异常的构造器
 * @param message 错误描述信息
 * @param cause 根源异常（如IOException等）
 */
public DiskWriteException(String message, Throwable cause) {
    super(message, cause);
}

/**
 * 带块ID和错误信息的构造器（新增，用于指定具体块写入失败）
 * @param blockId 发生错误的块ID
 * @param message 错误描述信息
 */
public DiskWriteException(int blockId, String message) {
    super("块ID: " + blockId + "，" + message);
}

/**
 * 带块ID、错误信息和根源异常的构造器（新增）
 * @param blockId 发生错误的块ID
 * @param message 错误描述信息
 * @param cause 根源异常
 */
public DiskWriteException(int blockId, String message, Throwable cause) {
    super("块ID: " + blockId + "，" + message, cause);
}
```

### 使用示例 / Usage Examples

```java
// 1. 基本写入错误处理
public boolean writeBlock(int blockId, byte[] data) throws DiskWriteException {
    try {
        return performBlockWrite(blockId, data);
    } catch (IOException e) {
        throw new DiskWriteException(blockId, "IO写入失败", e);
    }
}

// 2. 空间检查
public boolean writeBlock(int blockId, byte[] data) throws DiskWriteException {
    if (getFreeSpace() < data.length) {
        throw new DiskWriteException(blockId, "磁盘空间不足，需要 " + data.length + " 字节");
    }
    // 继续写入操作...
}

// 3. 写入验证
public boolean writeBlock(int blockId, byte[] data) throws DiskWriteException {
    performBlockWrite(blockId, data);
    
    // 验证写入结果
    byte[] readBack = readBlock(blockId);
    if (!Arrays.equals(data, readBack)) {
        throw new DiskWriteException(blockId, "写入验证失败，数据不匹配");
    }
    
    return true;
}
```

## DiskFullException 磁盘空间不足异常 / Disk Full Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘满异常：当磁盘没有足够空间存储数据时抛出
 */
public class DiskFullException extends DiskException {
    // 磁盘空间不足异常实现
}
```

### 核心特性 / Core Features

#### 1. 空间管理 / Space Management
- **空间检测**：实时检测磁盘可用空间
- **阈值监控**：监控空间使用阈值
- **预警机制**：空间不足时的预警提示

#### 2. 资源优化 / Resource Optimization
- **空间回收**：建议可回收的空间
- **清理策略**：提供空间清理建议
- **压缩选项**：数据压缩优化建议

#### 3. 用户指导 / User Guidance
- **友好提示**：用户友好的错误信息
- **解决方案**：提供具体的解决方案
- **操作建议**：给出下一步操作建议

### 构造方法 / Constructor Methods

```java
public DiskFullException() {
    super();
}

public DiskFullException(String message) {
    super(message);
}

public DiskFullException(String message, Throwable cause) {
    super(message, cause);
}
```

### 扩展构造方法建议 / Extended Constructor Recommendations

```java
/**
 * 带空间信息的构造器（建议扩展）
 * @param requiredSpace 需要的空间大小
 * @param availableSpace 可用空间大小
 * @param message 错误描述信息
 */
public DiskFullException(long requiredSpace, long availableSpace, String message) {
    super(String.format("%s (需要: %d 字节, 可用: %d 字节, 不足: %d 字节)", 
          message, requiredSpace, availableSpace, requiredSpace - availableSpace));
}

/**
 * 带详细空间统计的构造器（建议扩展）
 * @param spaceInfo 磁盘空间统计信息
 * @param operation 失败的操作描述
 */
public DiskFullException(DiskSpaceInfo spaceInfo, String operation) {
    super(String.format("操作 '%s' 失败：磁盘空间不足。总空间: %d, 已用: %d, 可用: %d", 
          operation, spaceInfo.getTotalSpace(), spaceInfo.getUsedSpace(), spaceInfo.getFreeSpace()));
}
```

### 使用示例 / Usage Examples

```java
// 1. 基本空间检查
public void allocateBlocks(int count) throws DiskFullException {
    if (getFreeBlockCount() < count) {
        throw new DiskFullException("磁盘空间不足，无法分配 " + count + " 个块");
    }
}

// 2. 详细空间信息
public void writeFile(String path, byte[] data) throws DiskFullException {
    long requiredSpace = data.length;
    long availableSpace = getFreeSpace();
    
    if (availableSpace < requiredSpace) {
        throw new DiskFullException(
            String.format("写入文件 '%s' 失败：空间不足。需要 %d 字节，可用 %d 字节", 
                         path, requiredSpace, availableSpace));
    }
}

// 3. 带解决方案的异常
public void createFile(String path, long size) throws DiskFullException {
    if (getFreeSpace() < size) {
        String suggestion = "建议：删除不需要的文件或清空回收站以释放空间";
        throw new DiskFullException("创建文件失败：磁盘空间不足。" + suggestion);
    }
}
```

## DiskInitializeException 磁盘初始化异常 / Disk Initialize Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘初始化异常：当磁盘初始化、打开或格式化失败时抛出
 */
public class DiskInitializeException extends DiskException {
    // 磁盘初始化异常实现
}
```

### 核心特性 / Core Features

#### 1. 初始化阶段管理 / Initialization Phase Management
- **阶段识别**：识别初始化失败的具体阶段
- **状态跟踪**：跟踪初始化过程的状态
- **回滚支持**：初始化失败时的状态回滚

#### 2. 配置验证 / Configuration Validation
- **参数检查**：验证初始化参数的有效性
- **环境检测**：检测运行环境的兼容性
- **依赖验证**：验证必要依赖的可用性

#### 3. 恢复机制 / Recovery Mechanism
- **自动恢复**：尝试自动恢复初始化
- **手动修复**：提供手动修复指导
- **备份恢复**：从备份恢复磁盘状态

### 使用示例 / Usage Examples

```java
// 1. 磁盘文件创建失败
public void initialize() throws DiskInitializeException {
    try {
        createDiskFile();
    } catch (IOException e) {
        throw new DiskInitializeException("无法创建磁盘文件", e);
    }
}

// 2. 配置参数无效
public void initialize(Properties config) throws DiskInitializeException {
    if (!validateConfiguration(config)) {
        throw new DiskInitializeException("磁盘配置参数无效：" + getConfigErrors(config));
    }
}

// 3. 磁盘格式化失败
public void format() throws DiskInitializeException {
    try {
        performFormat();
    } catch (Exception e) {
        throw new DiskInitializeException("磁盘格式化失败，可能需要手动修复", e);
    }
}
```

## FileSystemException 文件系统异常 / File System Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 文件系统通用异常类
 * 用于封装文件系统层面的错误（如未挂载、路径非法、操作失败等）
 */
public class FileSystemException extends Exception {
    // 文件系统异常实现
}
```

### 核心特性 / Core Features

#### 1. 高层抽象 / High-Level Abstraction
- **业务层异常**：封装业务逻辑层的错误
- **操作抽象**：抽象具体的底层操作错误
- **用户友好**：提供用户可理解的错误信息

#### 2. 异常聚合 / Exception Aggregation
- **多异常合并**：将多个底层异常合并为一个
- **错误分类**：按照业务逻辑对错误进行分类
- **上下文保持**：保持完整的错误上下文信息

#### 3. 系统集成 / System Integration
- **跨层传递**：在不同系统层之间传递异常
- **接口统一**：为外部系统提供统一的异常接口
- **日志集成**：与日志系统的深度集成

### 构造方法 / Constructor Methods

```java
/**
 * 无参构造器
 */
public FileSystemException() {
    super();
}

/**
 * 带错误信息的构造器
 * @param message 错误描述信息
 */
public FileSystemException(String message) {
    super(message);
}

/**
 * 带错误信息和根源异常的构造器
 * @param message 错误描述信息
 * @param cause 根源异常（如磁盘操作异常、FAT块管理异常等）
 */
public FileSystemException(String message, Throwable cause) {
    super(message, cause);
}

/**
 * 带根源异常的构造器
 * @param cause 根源异常
 */
public FileSystemException(Throwable cause) {
    super(cause);
}
```

### 使用示例 / Usage Examples

```java
// 1. 包装底层异常
public void createFile(String path) throws FileSystemException {
    try {
        performLowLevelFileCreation(path);
    } catch (DiskFullException e) {
        throw new FileSystemException("创建文件失败：磁盘空间不足", e);
    } catch (InvalidPathException e) {
        throw new FileSystemException("创建文件失败：路径格式无效", e);
    }
}

// 2. 业务逻辑异常
public void mountFileSystem() throws FileSystemException {
    if (isAlreadyMounted()) {
        throw new FileSystemException("文件系统已经挂载，无法重复挂载");
    }
    
    if (!isDiskAvailable()) {
        throw new FileSystemException("磁盘不可用，无法挂载文件系统");
    }
}

// 3. 复合操作异常
public void copyFile(String source, String destination) throws FileSystemException {
    try {
        validatePath(source);
        validatePath(destination);
        performFileCopy(source, destination);
    } catch (Exception e) {
        throw new FileSystemException("文件复制操作失败：从 '" + source + "' 到 '" + destination + "'", e);
    }
}
```

## 运行时异常类 / Runtime Exception Classes

## InvalidBlockIdException 无效块ID异常 / Invalid Block ID Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 无效块ID异常：当访问的磁盘块ID超出有效范围时抛出
 */
public class InvalidBlockIdException extends RuntimeException {
    // 无效块ID异常实现
}
```

### 核心特性 / Core Features

#### 1. 参数验证 / Parameter Validation
- **范围检查**：验证块ID是否在有效范围内
- **状态验证**：检查块的分配状态
- **类型验证**：验证块ID的数据类型

#### 2. 快速失败 / Fail-Fast
- **即时检测**：在参数传入时立即检测
- **早期发现**：在操作执行前发现问题
- **资源保护**：避免无效操作消耗资源

#### 3. 调试辅助 / Debugging Assistance
- **详细信息**：提供详细的块ID信息
- **上下文描述**：描述操作的上下文
- **修复建议**：提供可能的修复建议

### 使用示例 / Usage Examples

```java
// 1. 基本范围检查
public byte[] readBlock(int blockId) {
    if (blockId < 0 || blockId >= totalBlocks) {
        throw new InvalidBlockIdException("块ID " + blockId + " 超出有效范围 [0, " + (totalBlocks - 1) + "]");
    }
    return performRead(blockId);
}

// 2. 状态检查
public void freeBlock(int blockId) {
    validateBlockId(blockId);
    if (!isBlockAllocated(blockId)) {
        throw new InvalidBlockIdException("块ID " + blockId + " 未分配，无法释放");
    }
    performFree(blockId);
}

// 3. 批量验证
public void validateBlockIds(int[] blockIds) {
    for (int i = 0; i < blockIds.length; i++) {
        if (blockIds[i] < 0 || blockIds[i] >= totalBlocks) {
            throw new InvalidBlockIdException("第 " + i + " 个块ID " + blockIds[i] + " 无效");
        }
    }
}
```

## InvalidPathException 无效路径异常 / Invalid Path Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 无效路径异常：当路径格式错误或无法解析时抛出
 */
public class InvalidPathException extends RuntimeException {
    // 无效路径异常实现
}
```

### 核心特性 / Core Features

#### 1. 路径格式验证 / Path Format Validation
- **语法检查**：验证路径的语法正确性
- **字符验证**：检查路径中的非法字符
- **长度限制**：验证路径长度限制

#### 2. 路径解析 / Path Resolution
- **相对路径**：处理相对路径的解析
- **绝对路径**：验证绝对路径的格式
- **路径规范化**：标准化路径格式

#### 3. 安全检查 / Security Check
- **路径遍历**：防止路径遍历攻击
- **权限检查**：验证路径访问权限
- **安全边界**：确保路径在安全边界内

### 使用示例 / Usage Examples

```java
// 1. 基本路径验证
public void validatePath(String path) {
    if (path == null || path.isEmpty()) {
        throw new InvalidPathException("路径不能为空");
    }
    
    if (!path.startsWith("/")) {
        throw new InvalidPathException("路径必须以 '/' 开头：" + path);
    }
    
    if (path.contains("//")) {
        throw new InvalidPathException("路径不能包含连续的 '/'：" + path);
    }
}

// 2. 字符验证
public void validatePathCharacters(String path) {
    char[] invalidChars = {'<', '>', ':', '"', '|', '?', '*'};
    for (char invalidChar : invalidChars) {
        if (path.indexOf(invalidChar) != -1) {
            throw new InvalidPathException("路径包含非法字符 '" + invalidChar + "'：" + path);
        }
    }
}

// 3. 路径长度检查
public void validatePathLength(String path) {
    if (path.length() > MAX_PATH_LENGTH) {
        throw new InvalidPathException("路径长度超过限制 " + MAX_PATH_LENGTH + " 字符：" + path);
    }
}
```

## FileNotFoundException 文件未找到异常 / File Not Found Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 文件未找到异常：当尝试访问不存在的文件时抛出
 */
public class FileNotFoundException extends RuntimeException {
    // 文件未找到异常实现
}
```

### 核心特性 / Core Features

#### 1. 存在性检查 / Existence Check
- **文件检查**：验证文件是否存在
- **目录检查**：验证目录是否存在
- **路径解析**：解析并验证完整路径

#### 2. 用户指导 / User Guidance
- **友好提示**：提供用户友好的错误信息
- **建议操作**：建议可能的解决方案
- **相似文件**：提示可能的相似文件

#### 3. 操作上下文 / Operation Context
- **操作描述**：描述尝试执行的操作
- **路径信息**：提供完整的路径信息
- **时间戳**：记录操作尝试的时间

### 使用示例 / Usage Examples

```java
// 1. 基本文件检查
public FileEntry getFile(String path) {
    FileEntry file = findFile(path);
    if (file == null) {
        throw new FileNotFoundException("文件不存在：" + path);
    }
    return file;
}

// 2. 带操作上下文的异常
public String readFile(String path) {
    if (!fileExists(path)) {
        throw new FileNotFoundException("读取文件失败，文件不存在：" + path);
    }
    return performRead(path);
}

// 3. 带建议的异常
public void deleteFile(String path) {
    if (!fileExists(path)) {
        String suggestion = "请检查文件路径是否正确，或文件是否已被删除";
        throw new FileNotFoundException("删除文件失败，文件不存在：" + path + "。" + suggestion);
    }
    performDelete(path);
}
```

## ReadOnlyException 只读文件操作异常 / Read Only Exception

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 只读文件操作异常：当尝试修改只读文件时抛出
 */
public class ReadOnlyException extends RuntimeException {
    // 只读文件操作异常实现
}
```

### 核心特性 / Core Features

#### 1. 权限检查 / Permission Check
- **只读属性**：检查文件的只读属性
- **系统权限**：验证系统级别的权限
- **用户权限**：检查用户级别的权限

#### 2. 操作限制 / Operation Restriction
- **写入限制**：限制对只读文件的写入操作
- **修改限制**：限制对只读文件的修改操作
- **删除限制**：限制对只读文件的删除操作

#### 3. 权限管理 / Permission Management
- **权限提升**：提示权限提升的方法
- **临时权限**：支持临时权限获取
- **权限恢复**：操作后恢复原始权限

### 使用示例 / Usage Examples

```java
// 1. 基本只读检查
public void writeFile(String path, String content) {
    FileEntry file = getFile(path);
    if (file.isReadOnly()) {
        throw new ReadOnlyException("无法写入只读文件：" + path);
    }
    performWrite(path, content);
}

// 2. 带权限提示的异常
public void modifyFile(String path) {
    if (isReadOnly(path)) {
        throw new ReadOnlyException("文件为只读状态，无法修改：" + path + 
                                   "。请先移除只读属性或以管理员身份运行");
    }
    performModify(path);
}

// 3. 批量操作检查
public void modifyFiles(List<String> paths) {
    List<String> readOnlyFiles = new ArrayList<>();
    for (String path : paths) {
        if (isReadOnly(path)) {
            readOnlyFiles.add(path);
        }
    }
    
    if (!readOnlyFiles.isEmpty()) {
        throw new ReadOnlyException("以下文件为只读状态，无法修改：" + 
                                   String.join(", ", readOnlyFiles));
    }
}
```

## 异常处理策略 / Exception Handling Strategies

### 统一异常处理器 / Unified Exception Handler

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 统一异常处理器
 * 提供系统级别的异常处理策略和错误恢复机制
 */
public class ExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    
    /**
     * 处理磁盘操作异常
     */
    public static void handleDiskException(DiskException e, String operation) {
        // 1. 记录错误日志
        logger.error("磁盘操作失败: {}, 操作: {}", e.getMessage(), operation, e);
        
        // 2. 错误分类处理
        if (e instanceof DiskFullException) {
            handleDiskFullException((DiskFullException) e);
        } else if (e instanceof DiskReadException) {
            handleDiskReadException((DiskReadException) e);
        } else if (e instanceof DiskWriteException) {
            handleDiskWriteException((DiskWriteException) e);
        } else {
            handleGenericDiskException(e);
        }
        
        // 3. 通知用户
        notifyUser("磁盘操作失败", e.getMessage());
    }
    
    /**
     * 处理文件系统异常
     */
    public static void handleFileSystemException(FileSystemException e, String context) {
        logger.error("文件系统操作失败: {}, 上下文: {}", e.getMessage(), context, e);
        
        // 尝试自动恢复
        if (attemptAutoRecovery(e)) {
            logger.info("文件系统异常自动恢复成功");
            return;
        }
        
        // 提供用户指导
        String guidance = generateUserGuidance(e);
        notifyUser("文件系统错误", e.getMessage() + "\n\n建议：" + guidance);
    }
    
    /**
     * 处理运行时异常
     */
    public static void handleRuntimeException(RuntimeException e, String operation) {
        logger.error("运行时异常: {}, 操作: {}", e.getMessage(), operation, e);
        
        // 快速失败处理
        if (e instanceof InvalidBlockIdException || e instanceof InvalidPathException) {
            // 参数错误，立即返回
            notifyUser("参数错误", e.getMessage());
            return;
        }
        
        // 其他运行时异常
        handleGenericRuntimeException(e, operation);
    }
    
    // 私有辅助方法
    private static void handleDiskFullException(DiskFullException e) {
        // 尝试清理临时文件
        cleanupTemporaryFiles();
        
        // 建议用户操作
        String suggestion = "磁盘空间不足，建议：\n" +
                           "1. 删除不需要的文件\n" +
                           "2. 清空回收站\n" +
                           "3. 运行磁盘清理工具";
        notifyUser("磁盘空间不足", suggestion);
    }
    
    private static void handleDiskReadException(DiskReadException e) {
        // 尝试重新读取
        if (attemptRetryRead(e)) {
            logger.info("磁盘读取重试成功");
            return;
        }
        
        // 检查磁盘健康状态
        checkDiskHealth();
        
        notifyUser("磁盘读取错误", "磁盘可能存在硬件问题，建议运行磁盘检查工具");
    }
    
    private static void handleDiskWriteException(DiskWriteException e) {
        // 检查磁盘权限
        if (!checkDiskPermissions()) {
            notifyUser("权限错误", "磁盘写入权限不足，请以管理员身份运行");
            return;
        }
        
        // 检查磁盘空间
        if (getDiskFreeSpace() < getMinimumRequiredSpace()) {
            notifyUser("空间不足", "磁盘剩余空间不足，无法完成写入操作");
            return;
        }
        
        notifyUser("磁盘写入错误", "磁盘写入失败，请检查磁盘状态");
    }
    
    private static boolean attemptAutoRecovery(FileSystemException e) {
        // 实现自动恢复逻辑
        try {
            // 尝试重新挂载文件系统
            if (e.getMessage().contains("未挂载")) {
                remountFileSystem();
                return true;
            }
            
            // 尝试修复文件系统
            if (e.getMessage().contains("损坏")) {
                repairFileSystem();
                return true;
            }
            
        } catch (Exception recoveryException) {
            logger.warn("自动恢复失败", recoveryException);
        }
        
        return false;
    }
    
    private static String generateUserGuidance(FileSystemException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("未挂载")) {
            return "请先挂载文件系统后再进行操作";
        } else if (message.contains("路径")) {
            return "请检查文件路径是否正确";
        } else if (message.contains("权限")) {
            return "请检查文件访问权限或以管理员身份运行";
        } else {
            return "请重试操作，如问题持续存在请联系技术支持";
        }
    }
    
    // 工具方法
    private static void notifyUser(String title, String message) {
        // 实现用户通知逻辑（GUI对话框、控制台输出等）
        System.err.println("[" + title + "] " + message);
    }
    
    private static void cleanupTemporaryFiles() {
        // 实现临时文件清理逻辑
    }
    
    private static boolean attemptRetryRead(DiskReadException e) {
        // 实现读取重试逻辑
        return false;
    }
    
    private static void checkDiskHealth() {
        // 实现磁盘健康检查逻辑
    }
    
    private static boolean checkDiskPermissions() {
        // 实现磁盘权限检查逻辑
        return true;
    }
    
    private static long getDiskFreeSpace() {
        // 实现磁盘空间检查逻辑
        return 0;
    }
    
    private static long getMinimumRequiredSpace() {
        // 返回最小所需空间
        return 1024 * 1024; // 1MB
    }
    
    private static void remountFileSystem() throws Exception {
        // 实现文件系统重新挂载逻辑
    }
    
    private static void repairFileSystem() throws Exception {
        // 实现文件系统修复逻辑
    }
    
    private static void handleGenericDiskException(DiskException e) {
        // 处理通用磁盘异常
        notifyUser("磁盘错误", "磁盘操作失败：" + e.getMessage());
    }
    
    private static void handleGenericRuntimeException(RuntimeException e, String operation) {
        // 处理通用运行时异常
        notifyUser("系统错误", "操作 '" + operation + "' 失败：" + e.getMessage());
    }
}
```

### 异常处理最佳实践 / Exception Handling Best Practices

#### 1. 异常捕获原则 / Exception Catching Principles

```java
// 好的做法：具体异常优先捕获
try {
    performFileOperation();
} catch (DiskFullException e) {
    // 处理磁盘空间不足
    handleDiskFull(e);
} catch (FileNotFoundException e) {
    // 处理文件不存在
    handleFileNotFound(e);
} catch (DiskException e) {
    // 处理其他磁盘异常
    handleDiskError(e);
} catch (Exception e) {
    // 处理未预期的异常
    handleUnexpectedError(e);
}

// 避免的做法：过于宽泛的异常捕获
try {
    performFileOperation();
} catch (Exception e) {
    // 这样会掩盖具体的错误类型
    System.out.println("操作失败");
}
```

#### 2. 异常信息设计 / Exception Message Design

```java
// 好的异常信息：详细、有用、可操作
throw new DiskFullException(
    "磁盘空间不足：需要 " + requiredSpace + " 字节，" +
    "可用 " + availableSpace + " 字节。" +
    "建议删除不需要的文件或扩展磁盘容量。"
);

// 避免的异常信息：模糊、无用
throw new DiskFullException("错误");
```

#### 3. 异常链传递 / Exception Chaining

```java
// 好的做法：保持异常链
try {
    lowLevelOperation();
} catch (IOException e) {
    throw new DiskException("高级操作失败", e);
}

// 避免的做法：丢失原始异常信息
try {
    lowLevelOperation();
} catch (IOException e) {
    throw new DiskException("高级操作失败");
}
```

#### 4. 资源清理 / Resource Cleanup

```java
// 使用try-with-resources确保资源清理
try (FileInputStream fis = new FileInputStream(file)) {
    // 文件操作
} catch (IOException e) {
    throw new DiskReadException("文件读取失败", e);
}

// 或者使用finally块
FileInputStream fis = null;
try {
    fis = new FileInputStream(file);
    // 文件操作
} catch (IOException e) {
    throw new DiskReadException("文件读取失败", e);
} finally {
    if (fis != null) {
        try {
            fis.close();
        } catch (IOException e) {
            logger.warn("关闭文件流失败", e);
        }
    }
}
```

## 异常监控和诊断 / Exception Monitoring and Diagnostics

### 异常统计器 / Exception Statistics

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 异常统计器
 * 收集和分析系统中的异常发生情况
 */
public class ExceptionStatistics {
    
    private final Map<Class<? extends Throwable>, AtomicLong> exceptionCounts = new ConcurrentHashMap<>();
    private final Map<Class<? extends Throwable>, AtomicLong> exceptionTimes = new ConcurrentHashMap<>();
    private final Queue<ExceptionRecord> recentExceptions = new ConcurrentLinkedQueue<>();
    
    private static final int MAX_RECENT_EXCEPTIONS = 100;
    
    /**
     * 记录异常发生
     */
    public void recordException(Throwable exception) {
        Class<? extends Throwable> exceptionClass = exception.getClass();
        
        // 更新计数
        exceptionCounts.computeIfAbsent(exceptionClass, k -> new AtomicLong(0)).incrementAndGet();
        
        // 记录时间
        exceptionTimes.computeIfAbsent(exceptionClass, k -> new AtomicLong(0))
                     .set(System.currentTimeMillis());
        
        // 添加到最近异常列表
        ExceptionRecord record = new ExceptionRecord(exception, System.currentTimeMillis());
        recentExceptions.offer(record);
        
        // 保持队列大小
        while (recentExceptions.size() > MAX_RECENT_EXCEPTIONS) {
            recentExceptions.poll();
        }
    }
    
    /**
     * 获取异常统计报告
     */
    public ExceptionReport generateReport() {
        Map<String, Long> counts = new HashMap<>();
        Map<String, Long> lastOccurrence = new HashMap<>();
        
        for (Map.Entry<Class<? extends Throwable>, AtomicLong> entry : exceptionCounts.entrySet()) {
            String className = entry.getKey().getSimpleName();
            counts.put(className, entry.getValue().get());
            lastOccurrence.put(className, exceptionTimes.get(entry.getKey()).get());
        }
        
        return new ExceptionReport(counts, lastOccurrence, new ArrayList<>(recentExceptions));
    }
    
    /**
     * 获取最频繁的异常
     */
    public List<String> getMostFrequentExceptions(int limit) {
        return exceptionCounts.entrySet().stream()
                .sorted(Map.Entry.<Class<? extends Throwable>, AtomicLong>comparingByValue(
                    (a, b) -> Long.compare(b.get(), a.get())))
                .limit(limit)
                .map(entry -> entry.getKey().getSimpleName() + " (" + entry.getValue().get() + ")")
                .collect(Collectors.toList());
    }
    
    /**
     * 清除统计数据
     */
    public void clearStatistics() {
        exceptionCounts.clear();
        exceptionTimes.clear();
        recentExceptions.clear();
    }
    
    // 内部类
    public static class ExceptionRecord {
        private final String exceptionType;
        private final String message;
        private final long timestamp;
        private final String stackTrace;
        
        public ExceptionRecord(Throwable exception, long timestamp) {
            this.exceptionType = exception.getClass().getSimpleName();
            this.message = exception.getMessage();
            this.timestamp = timestamp;
            this.stackTrace = getStackTraceString(exception);
        }
        
        private String getStackTraceString(Throwable exception) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            return sw.toString();
        }
        
        // Getters...
        public String getExceptionType() { return exceptionType; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public String getStackTrace() { return stackTrace; }
    }
    
    public static class ExceptionReport {
        private final Map<String, Long> exceptionCounts;
        private final Map<String, Long> lastOccurrences;
        private final List<ExceptionRecord> recentExceptions;
        
        public ExceptionReport(Map<String, Long> exceptionCounts, 
                             Map<String, Long> lastOccurrences,
                             List<ExceptionRecord> recentExceptions) {
            this.exceptionCounts = exceptionCounts;
            this.lastOccurrences = lastOccurrences;
            this.recentExceptions = recentExceptions;
        }
        
        // Getters...
        public Map<String, Long> getExceptionCounts() { return exceptionCounts; }
        public Map<String, Long> getLastOccurrences() { return lastOccurrences; }
        public List<ExceptionRecord> getRecentExceptions() { return recentExceptions; }
    }
}
```

### 异常诊断工具 / Exception Diagnostic Tools

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 异常诊断工具
 * 提供异常分析和诊断功能
 */
public class ExceptionDiagnostics {
    
    /**
     * 分析异常模式
     */
    public static DiagnosticResult analyzeExceptionPattern(List<ExceptionRecord> exceptions) {
        DiagnosticResult result = new DiagnosticResult();
        
        // 分析异常频率
        Map<String, Long> frequency = exceptions.stream()
                .collect(Collectors.groupingBy(
                    ExceptionRecord::getExceptionType,
                    Collectors.counting()));
        
        // 检测异常爆发
        detectExceptionBursts(exceptions, result);
        
        // 分析异常趋势
        analyzeTrends(exceptions, result);
        
        // 识别根本原因
        identifyRootCauses(exceptions, result);
        
        return result;
    }
    
    /**
     * 检测异常爆发
     */
    private static void detectExceptionBursts(List<ExceptionRecord> exceptions, DiagnosticResult result) {
        // 按时间窗口分组
        long timeWindow = 60000; // 1分钟
        Map<Long, Long> timeGroups = exceptions.stream()
                .collect(Collectors.groupingBy(
                    record -> record.getTimestamp() / timeWindow,
                    Collectors.counting()));
        
        // 检测异常数量突增
        long averageCount = timeGroups.values().stream()
                .mapToLong(Long::longValue)
                .sum() / Math.max(timeGroups.size(), 1);
        
        for (Map.Entry<Long, Long> entry : timeGroups.entrySet()) {
            if (entry.getValue() > averageCount * 3) { // 3倍于平均值
                result.addWarning("检测到异常爆发：时间窗口 " + 
                                new Date(entry.getKey() * timeWindow) + 
                                " 内发生 " + entry.getValue() + " 个异常");
            }
        }
    }
    
    /**
     * 分析异常趋势
     */
    private static void analyzeTrends(List<ExceptionRecord> exceptions, DiagnosticResult result) {
        if (exceptions.size() < 10) {
            return; // 数据不足
        }
        
        // 按时间排序
        List<ExceptionRecord> sorted = exceptions.stream()
                .sorted(Comparator.comparing(ExceptionRecord::getTimestamp))
                .collect(Collectors.toList());
        
        // 计算趋势
        int recentCount = 0;
        int olderCount = 0;
        long midpoint = sorted.get(sorted.size() / 2).getTimestamp();
        
        for (ExceptionRecord record : sorted) {
            if (record.getTimestamp() > midpoint) {
                recentCount++;
            } else {
                olderCount++;
            }
        }
        
        if (recentCount > olderCount * 1.5) {
            result.addWarning("异常发生频率呈上升趋势");
        } else if (recentCount < olderCount * 0.5) {
            result.addInfo("异常发生频率呈下降趋势");
        }
    }
    
    /**
     * 识别根本原因
     */
    private static void identifyRootCauses(List<ExceptionRecord> exceptions, DiagnosticResult result) {
        // 分析异常消息模式
        Map<String, Long> messagePatterns = exceptions.stream()
                .filter(record -> record.getMessage() != null)
                .collect(Collectors.groupingBy(
                    record -> extractPattern(record.getMessage()),
                    Collectors.counting()));
        
        // 识别常见问题
        for (Map.Entry<String, Long> entry : messagePatterns.entrySet()) {
            if (entry.getValue() > 5) { // 出现5次以上
                String pattern = entry.getKey();
                String suggestion = generateSuggestion(pattern);
                result.addSuggestion("常见问题：" + pattern + " (出现 " + entry.getValue() + " 次)。建议：" + suggestion);
            }
        }
    }
    
    private static String extractPattern(String message) {
        // 简化消息，提取模式
        return message.replaceAll("\\d+", "N")
                      .replaceAll("'[^']*'", "'...'")
                      .replaceAll("\"[^\"]*\"", "\"...\"");
    }
    
    private static String generateSuggestion(String pattern) {
        if (pattern.contains("磁盘空间不足")) {
            return "清理磁盘空间或扩展存储容量";
        } else if (pattern.contains("文件不存在")) {
            return "检查文件路径是否正确";
        } else if (pattern.contains("权限")) {
            return "检查文件访问权限";
        } else if (pattern.contains("块ID")) {
            return "检查块ID范围和分配状态";
        } else {
            return "查看详细日志以获取更多信息";
        }
    }
    
    // 诊断结果类
    public static class DiagnosticResult {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();
        private final List<String> suggestions = new ArrayList<>();
        
        public void addWarning(String warning) { warnings.add(warning); }
        public void addInfo(String info) { infos.add(info); }
        public void addSuggestion(String suggestion) { suggestions.add(suggestion); }
        
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfos() { return infos; }
        public List<String> getSuggestions() { return suggestions; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 异常诊断报告 ===\n");
            
            if (!warnings.isEmpty()) {
                sb.append("\n警告：\n");
                warnings.forEach(w -> sb.append("- ").append(w).append("\n"));
            }
            
            if (!infos.isEmpty()) {
                sb.append("\n信息：\n");
                infos.forEach(i -> sb.append("- ").append(i).append("\n"));
            }
            
            if (!suggestions.isEmpty()) {
                sb.append("\n建议：\n");
                suggestions.forEach(s -> sb.append("- ").append(s).append("\n"));
            }
            
            return sb.toString();
        }
    }
}
```

## 异常处理配置 / Exception Handling Configuration

### 异常处理配置类 / Exception Handling Configuration

```java
package org.jiejiejiang.filemanager.exception;

/**
 * 异常处理配置
 * 管理异常处理的各种配置参数
 */
public class ExceptionHandlingConfig {
    
    // 重试配置
    private int maxRetryAttempts = 3;
    private long retryDelayMs = 1000;
    private double retryBackoffMultiplier = 2.0;
    
    // 日志配置
    private boolean enableDetailedLogging = true;
    private boolean enableStackTraceLogging = true;
    private String logLevel = "ERROR";
    
    // 用户通知配置
    private boolean enableUserNotification = true;
    private boolean enablePopupDialogs = true;
    private boolean enableConsoleOutput = false;
    
    // 统计配置
    private boolean enableExceptionStatistics = true;
    private int maxRecentExceptions = 100;
    private long statisticsRetentionMs = 24 * 60 * 60 * 1000; // 24小时
    
    // 自动恢复配置
    private boolean enableAutoRecovery = true;
    private int maxAutoRecoveryAttempts = 2;
    private long autoRecoveryDelayMs = 5000;
    
    // 构造器
    public ExceptionHandlingConfig() {
        // 使用默认配置
    }
    
    public ExceptionHandlingConfig(Properties properties) {
        loadFromProperties(properties);
    }
    
    /**
     * 从配置文件加载
     */
    private void loadFromProperties(Properties properties) {
        maxRetryAttempts = Integer.parseInt(
            properties.getProperty("exception.retry.maxAttempts", "3"));
        retryDelayMs = Long.parseLong(
            properties.getProperty("exception.retry.delayMs", "1000"));
        retryBackoffMultiplier = Double.parseDouble(
            properties.getProperty("exception.retry.backoffMultiplier", "2.0"));
        
        enableDetailedLogging = Boolean.parseBoolean(
            properties.getProperty("exception.logging.detailed", "true"));
        enableStackTraceLogging = Boolean.parseBoolean(
            properties.getProperty("exception.logging.stackTrace", "true"));
        logLevel = properties.getProperty("exception.logging.level", "ERROR");
        
        enableUserNotification = Boolean.parseBoolean(
            properties.getProperty("exception.notification.enabled", "true"));
        enablePopupDialogs = Boolean.parseBoolean(
            properties.getProperty("exception.notification.popup", "true"));
        enableConsoleOutput = Boolean.parseBoolean(
            properties.getProperty("exception.notification.console", "false"));
        
        enableExceptionStatistics = Boolean.parseBoolean(
            properties.getProperty("exception.statistics.enabled", "true"));
        maxRecentExceptions = Integer.parseInt(
            properties.getProperty("exception.statistics.maxRecent", "100"));
        statisticsRetentionMs = Long.parseLong(
            properties.getProperty("exception.statistics.retentionMs", "86400000"));
        
        enableAutoRecovery = Boolean.parseBoolean(
            properties.getProperty("exception.recovery.enabled", "true"));
        maxAutoRecoveryAttempts = Integer.parseInt(
            properties.getProperty("exception.recovery.maxAttempts", "2"));
        autoRecoveryDelayMs = Long.parseLong(
            properties.getProperty("exception.recovery.delayMs", "5000"));
    }
    
    // Getters and Setters
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    
    public double getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }
    
    public boolean isEnableDetailedLogging() { return enableDetailedLogging; }
    public void setEnableDetailedLogging(boolean enableDetailedLogging) { this.enableDetailedLogging = enableDetailedLogging; }
    
    public boolean isEnableStackTraceLogging() { return enableStackTraceLogging; }
    public void setEnableStackTraceLogging(boolean enableStackTraceLogging) { this.enableStackTraceLogging = enableStackTraceLogging; }
    
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    
    public boolean isEnableUserNotification() { return enableUserNotification; }
    public void setEnableUserNotification(boolean enableUserNotification) { this.enableUserNotification = enableUserNotification; }
    
    public boolean isEnablePopupDialogs() { return enablePopupDialogs; }
    public void setEnablePopupDialogs(boolean enablePopupDialogs) { this.enablePopupDialogs = enablePopupDialogs; }
    
    public boolean isEnableConsoleOutput() { return enableConsoleOutput; }
    public void setEnableConsoleOutput(boolean enableConsoleOutput) { this.enableConsoleOutput = enableConsoleOutput; }
    
    public boolean isEnableExceptionStatistics() { return enableExceptionStatistics; }
    public void setEnableExceptionStatistics(boolean enableExceptionStatistics) { this.enableExceptionStatistics = enableExceptionStatistics; }
    
    public int getMaxRecentExceptions() { return maxRecentExceptions; }
    public void setMaxRecentExceptions(int maxRecentExceptions) { this.maxRecentExceptions = maxRecentExceptions; }
    
    public long getStatisticsRetentionMs() { return statisticsRetentionMs; }
    public void setStatisticsRetentionMs(long statisticsRetentionMs) { this.statisticsRetentionMs = statisticsRetentionMs; }
    
    public boolean isEnableAutoRecovery() { return enableAutoRecovery; }
    public void setEnableAutoRecovery(boolean enableAutoRecovery) { this.enableAutoRecovery = enableAutoRecovery; }
    
    public int getMaxAutoRecoveryAttempts() { return maxAutoRecoveryAttempts; }
    public void setMaxAutoRecoveryAttempts(int maxAutoRecoveryAttempts) { this.maxAutoRecoveryAttempts = maxAutoRecoveryAttempts; }
    
    public long getAutoRecoveryDelayMs() { return autoRecoveryDelayMs; }
    public void setAutoRecoveryDelayMs(long autoRecoveryDelayMs) { this.autoRecoveryDelayMs = autoRecoveryDelayMs; }
}
```

## 使用示例 / Usage Examples

### 基本异常处理示例 / Basic Exception Handling Examples

```java
package org.jiejiejiang.filemanager.example;

/**
 * 异常处理使用示例
 */
public class ExceptionHandlingExamples {
    
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();
    private final ExceptionStatistics statistics = new ExceptionStatistics();
    
    /**
     * 示例1：文件读取操作的异常处理
     */
    public String readFileWithExceptionHandling(String path) {
        try {
            // 验证路径
            if (path == null || path.isEmpty()) {
                throw new InvalidPathException("文件路径不能为空");
            }
            
            // 检查文件是否存在
            if (!fileExists(path)) {
                throw new FileNotFoundException("文件不存在：" + path);
            }
            
            // 执行读取操作
            return performFileRead(path);
            
        } catch (InvalidPathException e) {
            statistics.recordException(e);
            exceptionHandler.handleRuntimeException(e, "文件读取");
            return null;
            
        } catch (FileNotFoundException e) {
            statistics.recordException(e);
            exceptionHandler.handleRuntimeException(e, "文件读取");
            return null;
            
        } catch (DiskReadException e) {
            statistics.recordException(e);
            exceptionHandler.handleDiskException(e, "文件读取");
            return null;
            
        } catch (Exception e) {
            statistics.recordException(e);
            logger.error("文件读取时发生未预期的异常", e);
            return null;
        }
    }
    
    /**
     * 示例2：文件写入操作的异常处理
     */
    public boolean writeFileWithExceptionHandling(String path, String content) {
        try {
            // 验证参数
            validateWriteParameters(path, content);
            
            // 检查磁盘空间
            checkDiskSpace(content.length());
            
            // 检查文件权限
            checkFilePermissions(path);
            
            // 执行写入操作
            performFileWrite(path, content);
            return true;
            
        } catch (InvalidPathException | ReadOnlyException e) {
            statistics.recordException(e);
            exceptionHandler.handleRuntimeException(e, "文件写入");
            return false;
            
        } catch (DiskFullException e) {
            statistics.recordException(e);
            exceptionHandler.handleDiskException(e, "文件写入");
            return false;
            
        } catch (DiskWriteException e) {
            statistics.recordException(e);
            exceptionHandler.handleDiskException(e, "文件写入");
            return false;
            
        } catch (FileSystemException e) {
            statistics.recordException(e);
            exceptionHandler.handleFileSystemException(e, "文件写入操作");
            return false;
        }
    }
    
    /**
     * 示例3：批量文件操作的异常处理
     */
    public List<String> processBatchFiles(List<String> filePaths) {
        List<String> results = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        for (String path : filePaths) {
            try {
                String result = processFile(path);
                results.add(result);
                
            } catch (FileNotFoundException e) {
                statistics.recordException(e);
                failedFiles.add(path + " (文件不存在)");
                
            } catch (ReadOnlyException e) {
                statistics.recordException(e);
                failedFiles.add(path + " (文件只读)");
                
            } catch (DiskException e) {
                statistics.recordException(e);
                failedFiles.add(path + " (磁盘错误: " + e.getMessage() + ")");
                
            } catch (Exception e) {
                statistics.recordException(e);
                failedFiles.add(path + " (未知错误: " + e.getMessage() + ")");
            }
        }
        
        // 报告处理结果
        if (!failedFiles.isEmpty()) {
            String errorReport = "以下文件处理失败：\n" + String.join("\n", failedFiles);
            exceptionHandler.notifyUser("批量处理完成", errorReport);
        }
        
        return results;
    }
    
    /**
     * 示例4：带重试机制的异常处理
     */
    public boolean performOperationWithRetry(String operation) {
        ExceptionHandlingConfig config = new ExceptionHandlingConfig();
        int maxAttempts = config.getMaxRetryAttempts();
        long delay = config.getRetryDelayMs();
        double backoffMultiplier = config.getRetryBackoffMultiplier();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                executeOperation(operation);
                return true; // 操作成功
                
            } catch (DiskReadException | DiskWriteException e) {
                statistics.recordException(e);
                
                if (attempt == maxAttempts) {
                    // 最后一次尝试失败
                    exceptionHandler.handleDiskException(e, operation);
                    return false;
                } else {
                    // 等待后重试
                    logger.warn("操作失败，第 {} 次重试 (共 {} 次): {}", 
                               attempt, maxAttempts, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                        delay = (long) (delay * backoffMultiplier);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                
            } catch (DiskFullException | InvalidBlockIdException e) {
                // 这些异常不适合重试
                statistics.recordException(e);
                exceptionHandler.handleDiskException(e, operation);
                return false;
            }
        }
        
        return false;
    }
    
    // 辅助方法
    private void validateWriteParameters(String path, String content) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("文件路径不能为空");
        }
        if (content == null) {
            throw new IllegalArgumentException("文件内容不能为null");
        }
    }
    
    private void checkDiskSpace(long requiredSpace) throws DiskFullException {
        if (getDiskFreeSpace() < requiredSpace) {
            throw new DiskFullException("磁盘空间不足，需要 " + requiredSpace + " 字节");
        }
    }
    
    private void checkFilePermissions(String path) throws ReadOnlyException {
        if (isReadOnly(path)) {
            throw new ReadOnlyException("文件为只读状态：" + path);
        }
    }
    
    // 模拟方法
    private boolean fileExists(String path) { return true; }
    private String performFileRead(String path) throws DiskReadException { return "content"; }
    private void performFileWrite(String path, String content) throws DiskWriteException {}
    private String processFile(String path) throws Exception { return "processed"; }
    private void executeOperation(String operation) throws DiskException {}
    private long getDiskFreeSpace() { return 1024 * 1024; }
    private boolean isReadOnly(String path) { return false; }
}
```

## 性能优化 / Performance Optimization

### 异常处理性能优化策略 / Exception Handling Performance Optimization

#### 1. 异常对象池 / Exception Object Pool

```java
/**
 * 异常对象池
 * 重用常见异常对象以减少内存分配开销
 */
public class ExceptionPool {
    
    private final Queue<DiskReadException> diskReadExceptions = new ConcurrentLinkedQueue<>();
    private final Queue<DiskWriteException> diskWriteExceptions = new ConcurrentLinkedQueue<>();
    private final Queue<InvalidBlockIdException> invalidBlockIdExceptions = new ConcurrentLinkedQueue<>();
    
    private static final int MAX_POOL_SIZE = 50;
    
    /**
     * 获取磁盘读取异常
     */
    public DiskReadException getDiskReadException(String message) {
        DiskReadException exception = diskReadExceptions.poll();
        if (exception == null) {
            return new DiskReadException(message);
        }
        
        // 重置异常信息
        resetException(exception, message);
        return exception;
    }
    
    /**
     * 归还异常对象到池中
     */
    public void returnException(DiskReadException exception) {
        if (diskReadExceptions.size() < MAX_POOL_SIZE) {
            diskReadExceptions.offer(exception);
        }
    }
    
    private void resetException(Exception exception, String message) {
        // 通过反射重置异常信息（实际实现中可能需要更复杂的逻辑）
        try {
            Field messageField = Throwable.class.getDeclaredField("detailMessage");
            messageField.setAccessible(true);
            messageField.set(exception, message);
        } catch (Exception e) {
            // 重置失败，创建新异常
        }
    }
}
```

#### 2. 异常处理缓存 / Exception Handling Cache

```java
/**
 * 异常处理结果缓存
 * 缓存异常处理的结果以避免重复处理
 */
public class ExceptionHandlingCache {
    
    private final Map<String, ExceptionHandlingResult> cache = new ConcurrentHashMap<>();
    private final long cacheExpirationMs = 60000; // 1分钟过期
    
    /**
     * 获取缓存的处理结果
     */
    public ExceptionHandlingResult getCachedResult(String exceptionKey) {
        ExceptionHandlingResult result = cache.get(exceptionKey);
        if (result != null && !result.isExpired()) {
            return result;
        }
        
        // 清理过期缓存
        cache.remove(exceptionKey);
        return null;
    }
    
    /**
     * 缓存处理结果
     */
    public void cacheResult(String exceptionKey, ExceptionHandlingResult result) {
        if (cache.size() < 1000) { // 限制缓存大小
            cache.put(exceptionKey, result);
        }
    }
    
    /**
     * 生成异常键
     */
    public String generateExceptionKey(Throwable exception) {
        return exception.getClass().getSimpleName() + ":" + 
               (exception.getMessage() != null ? exception.getMessage().hashCode() : 0);
    }
    
    // 缓存结果类
    public static class ExceptionHandlingResult {
        private final String action;
        private final long timestamp;
        private final long expirationMs;
        
        public ExceptionHandlingResult(String action, long expirationMs) {
            this.action = action;
            this.timestamp = System.currentTimeMillis();
            this.expirationMs = expirationMs;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expirationMs;
        }
        
        public String getAction() { return action; }
    }
}
```

## 测试建议 / Testing Recommendations

### 异常处理单元测试 / Exception Handling Unit Tests

```java
package org.jiejiejiang.filemanager.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常处理类单元测试
 */
public class ExceptionHandlingTest {
    
    private ExceptionHandler exceptionHandler;
    private ExceptionStatistics statistics;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new ExceptionHandler();
        statistics = new ExceptionStatistics();
    }
    
    @Test
    void testDiskExceptionHierarchy() {
        // 测试异常继承关系
        DiskReadException readException = new DiskReadException("读取失败");
        assertTrue(readException instanceof DiskException);
        assertTrue(readException instanceof RuntimeException);
        
        DiskWriteException writeException = new DiskWriteException("写入失败");
        assertTrue(writeException instanceof DiskException);
        
        DiskFullException fullException = new DiskFullException("磁盘已满");
        assertTrue(fullException instanceof DiskException);
    }
    
    @Test
    void testExceptionMessageFormatting() {
        // 测试异常信息格式化
        DiskReadException exception = new DiskReadException(123, "IO错误");
        assertTrue(exception.getMessage().contains("块ID: 123"));
        assertTrue(exception.getMessage().contains("IO错误"));
        
        DiskWriteException writeException = new DiskWriteException(456, "写入失败");
        assertTrue(writeException.getMessage().contains("块ID: 456"));
        assertTrue(writeException.getMessage().contains("写入失败"));
    }
    
    @Test
    void testExceptionChaining() {
        // 测试异常链
        IOException ioException = new IOException("底层IO错误");
        DiskException diskException = new DiskException("磁盘操作失败", ioException);
        
        assertEquals(ioException, diskException.getCause());
        assertTrue(diskException.getMessage().contains("磁盘操作失败"));
    }
    
    @Test
    void testExceptionStatistics() {
        // 测试异常统计
        DiskReadException readException = new DiskReadException("读取错误");
        FileNotFoundException notFoundException = new FileNotFoundException("文件不存在");
        
        statistics.recordException(readException);
        statistics.recordException(notFoundException);
        statistics.recordException(readException); // 重复异常
        
        ExceptionStatistics.ExceptionReport report = statistics.generateReport();
        assertEquals(2, report.getExceptionCounts().get("DiskReadException").longValue());
        assertEquals(1, report.getExceptionCounts().get("FileNotFoundException").longValue());
    }
    
    @Test
    void testInvalidBlockIdException() {
        // 测试无效块ID异常
        assertThrows(InvalidBlockIdException.class, () -> {
            validateBlockId(-1, 100);
        });
        
        assertThrows(InvalidBlockIdException.class, () -> {
            validateBlockId(100, 100);
        });
        
        // 有效块ID不应抛出异常
        assertDoesNotThrow(() -> {
            validateBlockId(50, 100);
        });
    }
    
    @Test
    void testInvalidPathException() {
        // 测试无效路径异常
        assertThrows(InvalidPathException.class, () -> {
            validatePath(null);
        });
        
        assertThrows(InvalidPathException.class, () -> {
            validatePath("");
        });
        
        assertThrows(InvalidPathException.class, () -> {
            validatePath("invalid//path");
        });
        
        // 有效路径不应抛出异常
        assertDoesNotThrow(() -> {
            validatePath("/valid/path");
        });
    }
    
    @Test
    void testReadOnlyException() {
        // 测试只读异常
        ReadOnlyException exception = new ReadOnlyException("文件只读");
        assertEquals("文件只读", exception.getMessage());
        
        IOException cause = new IOException("权限拒绝");
        ReadOnlyException exceptionWithCause = new ReadOnlyException("文件只读", cause);
        assertEquals(cause, exceptionWithCause.getCause());
    }
    
    @Test
    void testFileSystemException() {
        // 测试文件系统异常
        DiskException diskException = new DiskException("磁盘错误");
        FileSystemException fsException = new FileSystemException("文件系统错误", diskException);
        
        assertEquals(diskException, fsException.getCause());
        assertTrue(fsException instanceof Exception);
        assertFalse(fsException instanceof RuntimeException);
    }
    
    // 辅助测试方法
    private void validateBlockId(int blockId, int totalBlocks) {
        if (blockId < 0 || blockId >= totalBlocks) {
            throw new InvalidBlockIdException("块ID " + blockId + " 超出范围 [0, " + (totalBlocks - 1) + "]");
        }
    }
    
    private void validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("路径不能为空");
        }
        if (path.contains("//")) {
            throw new InvalidPathException("路径不能包含连续的斜杠");
        }
    }
}
```

### 集成测试建议 / Integration Testing Recommendations

```java
/**
 * 异常处理集成测试
 */
public class ExceptionHandlingIntegrationTest {
    
    @Test
    void testEndToEndExceptionHandling() {
        // 测试端到端异常处理流程
        // 1. 模拟磁盘操作失败
        // 2. 验证异常正确传播
        // 3. 验证异常处理器正确处理
        // 4. 验证用户通知正确发送
        // 5. 验证统计信息正确记录
    }
    
    @Test
    void testExceptionRecoveryMechanisms() {
        // 测试异常恢复机制
        // 1. 模拟可恢复的异常
        // 2. 验证自动恢复逻辑
        // 3. 验证恢复后系统状态
    }
    
    @Test
    void testExceptionHandlingPerformance() {
        // 测试异常处理性能
        // 1. 大量异常处理的性能测试
        // 2. 内存使用情况测试
        // 3. 异常处理延迟测试
    }
}
```

## 扩展建议 / Extension Recommendations

### 1. 自定义异常扩展 / Custom Exception Extensions

- **业务特定异常**：根据具体业务需求添加更多专用异常类
- **异常分级**：实现异常严重程度分级机制
- **国际化支持**：为异常消息添加多语言支持

### 2. 异常处理增强 / Exception Handling Enhancements

- **智能重试**：基于异常类型和历史数据的智能重试策略
- **异常预测**：基于模式识别的异常预测机制
- **自适应处理**：根据系统状态自适应调整异常处理策略

### 3. 监控和分析 / Monitoring and Analysis

- **实时监控**：实时异常监控和告警系统
- **趋势分析**：异常趋势分析和预警
- **性能影响分析**：异常对系统性能影响的分析

## 依赖关系 / Dependencies

### 内部依赖 / Internal Dependencies

- **磁盘管理模块**：`Disk` 类及相关组件
- **文件系统模块**：`FileSystem`、`Directory`、`FileEntry` 类
- **FAT管理模块**：`FAT` 类和块管理组件
- **GUI控制器**：`MainController`、`DiskViewerController` 类
- **多线程任务**：各种任务类和任务管理器

### 外部依赖 / External Dependencies

- **Java标准库**：`java.lang.Exception`、`java.lang.RuntimeException`
- **日志框架**：SLF4J、Logback 或类似日志框架
- **并发工具**：`java.util.concurrent` 包
- **反射API**：用于异常对象池的实现

### 配置依赖 / Configuration Dependencies

- **配置文件**：`exception-handling.properties`
- **系统属性**：JVM系统属性配置
- **环境变量**：运行时环境配置

---

**文档版本**：1.0  
**最后更新**：2024年12月  
**维护者**：文件管理系统开发团队