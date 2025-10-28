# 多线程任务类技术文档 / Multithreading Tasks Class Technical Documentation

## 概述 / Overview

**中文概述**：
多线程任务类是文件管理系统中的异步处理组件，负责处理耗时的文件系统操作、后台数据处理和UI响应优化。主要包括`FileReadTask`（文件读取任务）、`FileWriteTask`（文件写入任务）和`BufferFlushTask`（缓冲区刷新任务）等核心任务类。这些任务类基于JavaFX的Task框架实现，提供了进度监控、取消机制、异常处理和UI线程安全的异步操作能力，确保用户界面的流畅性和系统的响应性。

**English Overview**：
The multithreading task classes serve as asynchronous processing components in the file management system, responsible for handling time-consuming file system operations, background data processing, and UI responsiveness optimization. The main components include `FileReadTask` (file reading task), `FileWriteTask` (file writing task), and `BufferFlushTask` (buffer flush task). These task classes are implemented based on the JavaFX Task framework, providing progress monitoring, cancellation mechanisms, exception handling, and UI thread-safe asynchronous operation capabilities to ensure UI fluidity and system responsiveness.

## 架构设计 / Architecture Design

### 任务层次结构 / Task Hierarchy

```
AbstractTask (概念基类)
├── FileReadTask (文件读取任务)
│   ├── 异步文件读取
│   ├── 进度监控
│   └── 结果回调
├── FileWriteTask (文件写入任务)
│   ├── 异步文件写入
│   ├── 批量写入优化
│   └── 写入验证
└── BufferFlushTask (缓冲区刷新任务)
    ├── 定时刷新
    ├── 脏数据检测
    └── 异步持久化
```

### 线程模型 / Threading Model

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Thread     │    │ Background      │    │   Worker        │
│   (JavaFX)      │    │ Thread Pool     │    │   Threads       │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ Progress    │◄┼────┼─│ Task        │◄┼────┼─│ File I/O    │ │
│ │ Updates     │ │    │ │ Execution   │ │    │ │ Operations  │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ Result      │◄┼────┼─│ Callback    │ │    │ │ Buffer      │ │
│ │ Handling    │ │    │ │ Management  │ │    │ │ Management  │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## FileReadTask 文件读取任务 / File Reading Task

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.thread;

/**
 * 文件读取任务
 * 异步读取文件内容，支持大文件分块读取和进度监控
 */
public class FileReadTask extends Task<String> {
    // 文件读取任务实现
}
```

### 核心特性 / Core Features

#### 1. 异步文件读取 / Asynchronous File Reading
- **非阻塞操作**：在后台线程执行文件读取，不阻塞UI
- **分块读取**：支持大文件的分块读取，避免内存溢出
- **进度监控**：实时报告读取进度，提供用户反馈

#### 2. 错误处理和恢复 / Error Handling and Recovery
- **异常捕获**：捕获并处理文件读取异常
- **重试机制**：支持读取失败时的自动重试
- **优雅降级**：读取失败时提供备选方案

#### 3. 取消支持 / Cancellation Support
- **中断检测**：定期检查任务取消状态
- **资源清理**：任务取消时及时释放资源
- **状态同步**：与UI线程同步取消状态

### 字段定义 / Field Definitions

```java
public class FileReadTask extends Task<String> {
    // 任务参数
    private final FileSystem fileSystem;
    private final String filePath;
    private final boolean enableProgress;
    
    // 读取配置
    private final int bufferSize;
    private final int maxRetries;
    private final long retryDelay;
    
    // 状态管理
    private volatile boolean cancelled = false;
    private long totalBytes = 0;
    private long readBytes = 0;
    
    // 性能统计
    private long startTime;
    private long endTime;
}
```

### 构造方法 / Constructor Methods

```java
// 基本构造器
public FileReadTask(FileSystem fileSystem, String filePath) {
    this(fileSystem, filePath, true, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_RETRIES);
}

// 完整构造器
public FileReadTask(FileSystem fileSystem, String filePath, 
                   boolean enableProgress, int bufferSize, int maxRetries) {
    this.fileSystem = Objects.requireNonNull(fileSystem, "FileSystem不能为null");
    this.filePath = Objects.requireNonNull(filePath, "文件路径不能为null");
    this.enableProgress = enableProgress;
    this.bufferSize = Math.max(bufferSize, MIN_BUFFER_SIZE);
    this.maxRetries = Math.max(maxRetries, 0);
    this.retryDelay = DEFAULT_RETRY_DELAY;
    
    // 设置任务标题
    updateTitle("读取文件: " + filePath);
}
```

### 核心方法 / Core Methods

#### call() 主执行方法

```java
@Override
protected String call() throws Exception {
    startTime = System.currentTimeMillis();
    updateMessage("准备读取文件...");
    updateProgress(0, 100);
    
    try {
        // 1. 验证文件存在性
        if (!fileSystem.fileExists(filePath)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        // 2. 获取文件信息
        FileEntry fileEntry = fileSystem.getFileEntry(filePath);
        if (fileEntry.getType() != FileEntry.EntryType.FILE) {
            throw new IllegalArgumentException("路径不是文件: " + filePath);
        }
        
        totalBytes = fileEntry.getSize();
        updateMessage("开始读取文件，大小: " + formatFileSize(totalBytes));
        
        // 3. 执行读取操作
        String content = performRead();
        
        // 4. 完成统计
        endTime = System.currentTimeMillis();
        updateMessage("读取完成，耗时: " + (endTime - startTime) + "ms");
        updateProgress(100, 100);
        
        return content;
        
    } catch (Exception e) {
        updateMessage("读取失败: " + e.getMessage());
        throw e;
    }
}
```

#### performRead() 执行读取操作

```java
private String performRead() throws Exception {
    StringBuilder contentBuilder = new StringBuilder();
    int retryCount = 0;
    
    while (retryCount <= maxRetries) {
        try {
            // 检查取消状态
            if (isCancelled()) {
                updateMessage("任务已取消");
                return null;
            }
            
            // 分块读取文件内容
            if (totalBytes <= bufferSize) {
                // 小文件一次性读取
                String content = fileSystem.readFile(filePath);
                updateProgress(100, 100);
                return content;
            } else {
                // 大文件分块读取
                return performChunkedRead();
            }
            
        } catch (Exception e) {
            retryCount++;
            if (retryCount > maxRetries) {
                throw new FileReadException("读取失败，已重试 " + maxRetries + " 次", e);
            }
            
            updateMessage("读取失败，正在重试 (" + retryCount + "/" + maxRetries + ")...");
            Thread.sleep(retryDelay);
        }
    }
    
    throw new FileReadException("读取失败，超过最大重试次数");
}
```

#### performChunkedRead() 分块读取

```java
private String performChunkedRead() throws Exception {
    StringBuilder contentBuilder = new StringBuilder();
    byte[] buffer = new byte[bufferSize];
    readBytes = 0;
    
    // 获取文件的起始块ID
    FileEntry fileEntry = fileSystem.getFileEntry(filePath);
    int currentBlockId = fileEntry.getStartBlockId();
    
    while (currentBlockId != FAT.END_OF_FILE && !isCancelled()) {
        // 读取当前块
        byte[] blockData = fileSystem.getDisk().readBlock(currentBlockId);
        
        // 计算本次读取的字节数
        int bytesToRead = (int) Math.min(bufferSize, totalBytes - readBytes);
        
        // 转换为字符串并添加到结果中
        String blockContent = new String(blockData, 0, bytesToRead, StandardCharsets.UTF_8);
        contentBuilder.append(blockContent);
        
        // 更新进度
        readBytes += bytesToRead;
        if (enableProgress && totalBytes > 0) {
            double progress = (double) readBytes / totalBytes * 100;
            updateProgress(progress, 100);
            updateMessage(String.format("已读取: %s / %s (%.1f%%)", 
                formatFileSize(readBytes), formatFileSize(totalBytes), progress));
        }
        
        // 获取下一个块
        currentBlockId = fileSystem.getFat().getNextBlock(currentBlockId);
        
        // 避免过快更新UI
        if (readBytes % (bufferSize * 10) == 0) {
            Thread.sleep(10);
        }
    }
    
    return contentBuilder.toString();
}
```

### 回调方法 / Callback Methods

```java
@Override
protected void succeeded() {
    Platform.runLater(() -> {
        // 任务成功完成的处理
        LogUtil.info("文件读取成功: " + filePath + 
                    ", 大小: " + formatFileSize(totalBytes) + 
                    ", 耗时: " + (endTime - startTime) + "ms");
        
        // 触发成功回调
        if (onSuccessCallback != null) {
            onSuccessCallback.accept(getValue());
        }
    });
}

@Override
protected void failed() {
    Platform.runLater(() -> {
        Throwable exception = getException();
        LogUtil.error("文件读取失败: " + filePath, exception);
        
        // 触发失败回调
        if (onFailureCallback != null) {
            onFailureCallback.accept(exception);
        }
    });
}

@Override
protected void cancelled() {
    Platform.runLater(() -> {
        LogUtil.info("文件读取已取消: " + filePath);
        
        // 触发取消回调
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    });
}
```

## FileWriteTask 文件写入任务 / File Writing Task

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.thread;

/**
 * 文件写入任务
 * 异步写入文件内容，支持大文件分块写入和写入验证
 */
public class FileWriteTask extends Task<Boolean> {
    // 文件写入任务实现
}
```

### 核心特性 / Core Features

#### 1. 异步文件写入 / Asynchronous File Writing
- **非阻塞写入**：在后台线程执行文件写入操作
- **分块写入**：支持大文件的分块写入，优化内存使用
- **原子性保证**：确保写入操作的原子性，避免部分写入

#### 2. 写入验证 / Write Verification
- **数据校验**：写入后验证数据完整性
- **回滚机制**：写入失败时自动回滚到原始状态
- **备份策略**：重要文件写入前自动备份

#### 3. 性能优化 / Performance Optimization
- **批量写入**：合并多个小写入操作
- **缓冲优化**：使用适当的缓冲区大小
- **异步刷新**：延迟磁盘同步，提升写入性能

### 字段定义 / Field Definitions

```java
public class FileWriteTask extends Task<Boolean> {
    // 任务参数
    private final FileSystem fileSystem;
    private final String filePath;
    private final String content;
    private final WriteMode writeMode;
    
    // 写入配置
    private final boolean enableVerification;
    private final boolean createBackup;
    private final int bufferSize;
    
    // 状态管理
    private long totalBytes;
    private long writtenBytes = 0;
    private String backupPath;
    
    // 性能统计
    private long startTime;
    private long endTime;
}
```

### 写入模式枚举 / Write Mode Enumeration

```java
public enum WriteMode {
    OVERWRITE,  // 覆盖写入
    APPEND,     // 追加写入
    CREATE_NEW  // 创建新文件（文件存在时失败）
}
```

### 构造方法 / Constructor Methods

```java
// 基本构造器（覆盖模式）
public FileWriteTask(FileSystem fileSystem, String filePath, String content) {
    this(fileSystem, filePath, content, WriteMode.OVERWRITE, true, false);
}

// 完整构造器
public FileWriteTask(FileSystem fileSystem, String filePath, String content,
                    WriteMode writeMode, boolean enableVerification, boolean createBackup) {
    this.fileSystem = Objects.requireNonNull(fileSystem, "FileSystem不能为null");
    this.filePath = Objects.requireNonNull(filePath, "文件路径不能为null");
    this.content = Objects.requireNonNull(content, "文件内容不能为null");
    this.writeMode = writeMode;
    this.enableVerification = enableVerification;
    this.createBackup = createBackup;
    this.bufferSize = DEFAULT_BUFFER_SIZE;
    
    this.totalBytes = content.getBytes(StandardCharsets.UTF_8).length;
    
    // 设置任务标题
    updateTitle("写入文件: " + filePath);
}
```

### 核心方法 / Core Methods

#### call() 主执行方法

```java
@Override
protected Boolean call() throws Exception {
    startTime = System.currentTimeMillis();
    updateMessage("准备写入文件...");
    updateProgress(0, 100);
    
    try {
        // 1. 预检查
        performPreCheck();
        
        // 2. 创建备份（如果需要）
        if (createBackup && fileSystem.fileExists(filePath)) {
            createBackupFile();
        }
        
        // 3. 执行写入操作
        boolean success = performWrite();
        
        // 4. 验证写入结果（如果启用）
        if (success && enableVerification) {
            success = verifyWrittenContent();
        }
        
        // 5. 清理备份文件（如果写入成功）
        if (success && backupPath != null) {
            cleanupBackup();
        }
        
        // 6. 完成统计
        endTime = System.currentTimeMillis();
        updateMessage("写入完成，耗时: " + (endTime - startTime) + "ms");
        updateProgress(100, 100);
        
        return success;
        
    } catch (Exception e) {
        // 写入失败时恢复备份
        if (backupPath != null) {
            restoreFromBackup();
        }
        updateMessage("写入失败: " + e.getMessage());
        throw e;
    }
}
```

#### performWrite() 执行写入操作

```java
private boolean performWrite() throws Exception {
    updateMessage("开始写入文件，大小: " + formatFileSize(totalBytes));
    
    try {
        switch (writeMode) {
            case OVERWRITE:
                return performOverwrite();
            case APPEND:
                return performAppend();
            case CREATE_NEW:
                return performCreateNew();
            default:
                throw new IllegalArgumentException("不支持的写入模式: " + writeMode);
        }
    } catch (Exception e) {
        throw new FileWriteException("写入操作失败", e);
    }
}
```

#### performOverwrite() 覆盖写入

```java
private boolean performOverwrite() throws Exception {
    // 检查取消状态
    if (isCancelled()) {
        return false;
    }
    
    // 删除现有文件（如果存在）
    if (fileSystem.fileExists(filePath)) {
        fileSystem.deleteFile(filePath);
    }
    
    // 创建新文件并写入内容
    fileSystem.createFile(filePath, "");
    
    // 分块写入内容
    return writeContentInChunks();
}
```

#### writeContentInChunks() 分块写入内容

```java
private boolean writeContentInChunks() throws Exception {
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    writtenBytes = 0;
    
    // 获取文件条目
    FileEntry fileEntry = fileSystem.getFileEntry(filePath);
    int currentBlockId = fileEntry.getStartBlockId();
    
    // 分块写入
    int offset = 0;
    while (offset < contentBytes.length && !isCancelled()) {
        // 计算本次写入的字节数
        int chunkSize = Math.min(bufferSize, contentBytes.length - offset);
        
        // 准备写入数据
        byte[] chunkData = new byte[fileSystem.getBlockSize()];
        System.arraycopy(contentBytes, offset, chunkData, 0, chunkSize);
        
        // 写入当前块
        fileSystem.getDisk().writeBlock(currentBlockId, chunkData);
        
        // 更新进度
        writtenBytes += chunkSize;
        offset += chunkSize;
        
        if (totalBytes > 0) {
            double progress = (double) writtenBytes / totalBytes * 100;
            updateProgress(progress, 100);
            updateMessage(String.format("已写入: %s / %s (%.1f%%)", 
                formatFileSize(writtenBytes), formatFileSize(totalBytes), progress));
        }
        
        // 如果还有数据要写入，分配下一个块
        if (offset < contentBytes.length) {
            currentBlockId = fileSystem.getFat().allocateNextBlock(currentBlockId);
        }
        
        // 避免过快更新UI
        Thread.sleep(5);
    }
    
    // 更新文件大小
    fileEntry.updateSize(writtenBytes);
    
    return !isCancelled();
}
```

#### verifyWrittenContent() 验证写入内容

```java
private boolean verifyWrittenContent() throws Exception {
    updateMessage("验证写入内容...");
    
    try {
        // 读取刚写入的文件内容
        String readContent = fileSystem.readFile(filePath);
        
        // 比较内容
        boolean isValid = content.equals(readContent);
        
        if (isValid) {
            updateMessage("内容验证成功");
        } else {
            updateMessage("内容验证失败：写入内容与原始内容不匹配");
        }
        
        return isValid;
        
    } catch (Exception e) {
        updateMessage("内容验证失败：" + e.getMessage());
        return false;
    }
}
```

## BufferFlushTask 缓冲区刷新任务 / Buffer Flush Task

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.thread;

/**
 * 缓冲区刷新任务
 * 定期将缓冲区中的脏数据刷新到磁盘，确保数据持久性
 */
public class BufferFlushTask extends Task<Void> {
    // 缓冲区刷新任务实现
}
```

### 核心特性 / Core Features

#### 1. 定时刷新机制 / Scheduled Flush Mechanism
- **周期性执行**：按照配置的时间间隔定期执行刷新
- **智能调度**：根据系统负载动态调整刷新频率
- **优先级管理**：根据数据重要性确定刷新优先级

#### 2. 脏数据检测 / Dirty Data Detection
- **变更追踪**：跟踪缓冲区中的数据变更
- **批量处理**：将多个脏块合并为批量操作
- **依赖分析**：分析数据块之间的依赖关系

#### 3. 异常恢复 / Exception Recovery
- **故障检测**：检测刷新过程中的故障
- **重试机制**：失败时自动重试刷新操作
- **数据保护**：确保刷新失败时数据不丢失

### 字段定义 / Field Definitions

```java
public class BufferFlushTask extends Task<Void> {
    // 依赖组件
    private final Buffer buffer;
    private final Disk disk;
    private final FAT fat;
    
    // 刷新配置
    private final long flushInterval;
    private final int maxRetries;
    private final boolean enableBatchFlush;
    
    // 状态管理
    private volatile boolean running = true;
    private long lastFlushTime = 0;
    private int consecutiveFailures = 0;
    
    // 统计信息
    private long totalFlushCount = 0;
    private long totalBlocksFlushed = 0;
    private long totalFlushTime = 0;
}
```

### 构造方法 / Constructor Methods

```java
// 基本构造器
public BufferFlushTask(Buffer buffer) {
    this(buffer, DEFAULT_FLUSH_INTERVAL, DEFAULT_MAX_RETRIES, true);
}

// 完整构造器
public BufferFlushTask(Buffer buffer, long flushInterval, int maxRetries, boolean enableBatchFlush) {
    this.buffer = Objects.requireNonNull(buffer, "Buffer不能为null");
    this.disk = buffer.getDisk();
    this.fat = buffer.getFat();
    this.flushInterval = Math.max(flushInterval, MIN_FLUSH_INTERVAL);
    this.maxRetries = Math.max(maxRetries, 0);
    this.enableBatchFlush = enableBatchFlush;
    
    // 设置任务标题
    updateTitle("缓冲区刷新任务");
}
```

### 核心方法 / Core Methods

#### call() 主执行方法

```java
@Override
protected Void call() throws Exception {
    updateMessage("缓冲区刷新任务启动");
    
    while (running && !isCancelled()) {
        try {
            // 1. 检查是否需要刷新
            if (shouldFlush()) {
                // 2. 执行刷新操作
                performFlush();
                
                // 3. 更新统计信息
                updateStatistics();
            }
            
            // 4. 等待下一个刷新周期
            Thread.sleep(flushInterval);
            
        } catch (InterruptedException e) {
            // 任务被中断，正常退出
            Thread.currentThread().interrupt();
            break;
        } catch (Exception e) {
            // 处理刷新异常
            handleFlushException(e);
        }
    }
    
    updateMessage("缓冲区刷新任务已停止");
    return null;
}
```

#### shouldFlush() 检查是否需要刷新

```java
private boolean shouldFlush() {
    // 1. 检查是否有脏数据
    if (buffer.getDirtyBlockCount() == 0) {
        return false;
    }
    
    // 2. 检查时间间隔
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastFlushTime < flushInterval) {
        return false;
    }
    
    // 3. 检查系统负载（可选）
    if (isSystemBusy()) {
        return false;
    }
    
    // 4. 检查脏数据量是否达到阈值
    int dirtyCount = buffer.getDirtyBlockCount();
    int threshold = buffer.getCacheSize() / 4; // 25%阈值
    
    return dirtyCount >= threshold || 
           (currentTime - lastFlushTime) > (flushInterval * 2);
}
```

#### performFlush() 执行刷新操作

```java
private void performFlush() throws Exception {
    long startTime = System.currentTimeMillis();
    updateMessage("开始刷新缓冲区...");
    
    try {
        // 获取所有脏块
        Set<Integer> dirtyBlocks = buffer.getDirtyBlocks();
        int totalBlocks = dirtyBlocks.size();
        
        if (totalBlocks == 0) {
            return;
        }
        
        updateMessage("刷新 " + totalBlocks + " 个脏块到磁盘");
        
        if (enableBatchFlush) {
            // 批量刷新
            performBatchFlush(dirtyBlocks);
        } else {
            // 逐个刷新
            performSequentialFlush(dirtyBlocks);
        }
        
        // 更新最后刷新时间
        lastFlushTime = System.currentTimeMillis();
        consecutiveFailures = 0;
        
        long flushTime = lastFlushTime - startTime;
        totalFlushTime += flushTime;
        totalFlushCount++;
        totalBlocksFlushed += totalBlocks;
        
        updateMessage("刷新完成，耗时: " + flushTime + "ms");
        
    } catch (Exception e) {
        consecutiveFailures++;
        throw new BufferFlushException("缓冲区刷新失败", e);
    }
}
```

#### performBatchFlush() 批量刷新

```java
private void performBatchFlush(Set<Integer> dirtyBlocks) throws Exception {
    // 按块ID排序，优化磁盘访问
    List<Integer> sortedBlocks = dirtyBlocks.stream()
        .sorted()
        .collect(Collectors.toList());
    
    int processedBlocks = 0;
    
    // 分批处理，避免一次性处理过多块
    int batchSize = Math.min(32, sortedBlocks.size());
    
    for (int i = 0; i < sortedBlocks.size(); i += batchSize) {
        if (isCancelled()) {
            break;
        }
        
        int endIndex = Math.min(i + batchSize, sortedBlocks.size());
        List<Integer> batch = sortedBlocks.subList(i, endIndex);
        
        // 处理当前批次
        for (Integer blockId : batch) {
            flushSingleBlock(blockId);
            processedBlocks++;
            
            // 更新进度
            double progress = (double) processedBlocks / sortedBlocks.size() * 100;
            updateProgress(progress, 100);
        }
        
        // 批次间短暂休息，避免过度占用系统资源
        Thread.sleep(10);
    }
}
```

#### flushSingleBlock() 刷新单个块

```java
private void flushSingleBlock(Integer blockId) throws Exception {
    int retryCount = 0;
    
    while (retryCount <= maxRetries) {
        try {
            // 从缓冲区获取块数据
            byte[] blockData = buffer.getBlockData(blockId);
            if (blockData == null) {
                // 块已经不在缓冲区中，可能已被其他线程刷新
                return;
            }
            
            // 写入磁盘
            disk.writeBlock(blockId, blockData);
            
            // 标记块为干净
            buffer.markBlockClean(blockId);
            
            return; // 成功，退出重试循环
            
        } catch (Exception e) {
            retryCount++;
            if (retryCount > maxRetries) {
                throw new BufferFlushException(
                    "刷新块 " + blockId + " 失败，已重试 " + maxRetries + " 次", e);
            }
            
            // 重试前等待
            Thread.sleep(100 * retryCount);
        }
    }
}
```

### 监控和统计方法 / Monitoring and Statistics Methods

#### getFlushStatistics() 获取刷新统计

```java
public FlushStatistics getFlushStatistics() {
    return new FlushStatistics(
        totalFlushCount,
        totalBlocksFlushed,
        totalFlushTime,
        consecutiveFailures,
        calculateAverageFlushTime(),
        calculateAverageBlocksPerFlush()
    );
}

// 刷新统计信息类
public static class FlushStatistics {
    private final long totalFlushCount;
    private final long totalBlocksFlushed;
    private final long totalFlushTime;
    private final int consecutiveFailures;
    private final double averageFlushTime;
    private final double averageBlocksPerFlush;
    
    // 构造器和getter方法...
}
```

#### printFlushStatus() 打印刷新状态

```java
public void printFlushStatus() {
    FlushStatistics stats = getFlushStatistics();
    System.out.printf("刷新统计: 总次数=%d, 总块数=%d, 总耗时=%dms, 平均耗时=%.2fms, 连续失败=%d%n",
        stats.getTotalFlushCount(),
        stats.getTotalBlocksFlushed(),
        stats.getTotalFlushTime(),
        stats.getAverageFlushTime(),
        stats.getConsecutiveFailures()
    );
}
```

## 任务管理器 / Task Manager

### TaskManager 任务管理器类

```java
package org.jiejiejiang.filemanager.thread;

/**
 * 任务管理器
 * 统一管理所有后台任务的创建、执行和监控
 */
public class TaskManager {
    // 线程池
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    
    // 任务跟踪
    private final Map<String, Task<?>> activeTasks;
    private final AtomicLong taskIdCounter;
    
    // 配置参数
    private final int maxConcurrentTasks;
    private final long taskTimeoutMs;
}
```

### 核心功能 / Core Functionality

#### 任务提交方法 / Task Submission Methods

```java
// 提交文件读取任务
public Future<String> submitFileReadTask(FileSystem fileSystem, String filePath) {
    FileReadTask task = new FileReadTask(fileSystem, filePath);
    String taskId = generateTaskId("FileRead");
    
    activeTasks.put(taskId, task);
    
    return executorService.submit(task);
}

// 提交文件写入任务
public Future<Boolean> submitFileWriteTask(FileSystem fileSystem, String filePath, 
                                          String content, WriteMode mode) {
    FileWriteTask task = new FileWriteTask(fileSystem, filePath, content, mode, true, false);
    String taskId = generateTaskId("FileWrite");
    
    activeTasks.put(taskId, task);
    
    return executorService.submit(task);
}

// 启动缓冲区刷新任务
public void startBufferFlushTask(Buffer buffer) {
    BufferFlushTask task = new BufferFlushTask(buffer);
    String taskId = generateTaskId("BufferFlush");
    
    activeTasks.put(taskId, task);
    
    scheduledExecutorService.submit(task);
}
```

#### 任务监控方法 / Task Monitoring Methods

```java
// 获取活跃任务列表
public List<TaskInfo> getActiveTasks() {
    return activeTasks.entrySet().stream()
        .map(entry -> new TaskInfo(
            entry.getKey(),
            entry.getValue().getTitle(),
            entry.getValue().getProgress(),
            entry.getValue().getMessage(),
            entry.getValue().getState()
        ))
        .collect(Collectors.toList());
}

// 取消指定任务
public boolean cancelTask(String taskId) {
    Task<?> task = activeTasks.get(taskId);
    if (task != null) {
        boolean cancelled = task.cancel(true);
        if (cancelled) {
            activeTasks.remove(taskId);
        }
        return cancelled;
    }
    return false;
}

// 取消所有任务
public void cancelAllTasks() {
    activeTasks.values().forEach(task -> task.cancel(true));
    activeTasks.clear();
}
```

## 异常处理 / Exception Handling

### 自定义异常类 / Custom Exception Classes

```java
// 文件读取异常
public class FileReadException extends Exception {
    public FileReadException(String message) {
        super(message);
    }
    
    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 文件写入异常
public class FileWriteException extends Exception {
    public FileWriteException(String message) {
        super(message);
    }
    
    public FileWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 缓冲区刷新异常
public class BufferFlushException extends Exception {
    public BufferFlushException(String message) {
        super(message);
    }
    
    public BufferFlushException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 异常处理策略 / Exception Handling Strategy

```java
// 统一异常处理模式
private void handleTaskException(Task<?> task, Throwable exception) {
    // 1. 记录错误日志
    LogUtil.error("任务执行失败: " + task.getTitle(), exception);
    
    // 2. 更新任务状态
    Platform.runLater(() -> {
        task.updateMessage("任务失败: " + exception.getMessage());
    });
    
    // 3. 通知错误处理器
    if (errorHandler != null) {
        errorHandler.handleError(task, exception);
    }
    
    // 4. 清理资源
    cleanupTaskResources(task);
}
```

## 线程安全 / Thread Safety

### 同步策略 / Synchronization Strategy

```java
// 使用并发集合确保线程安全
private final ConcurrentHashMap<String, Task<?>> activeTasks = new ConcurrentHashMap<>();
private final AtomicLong taskIdCounter = new AtomicLong(0);

// 使用同步块保护关键操作
public synchronized void updateTaskProgress(String taskId, double progress) {
    Task<?> task = activeTasks.get(taskId);
    if (task != null) {
        Platform.runLater(() -> task.updateProgress(progress, 100));
    }
}
```

### UI线程安全 / UI Thread Safety

```java
// 确保UI更新在JavaFX应用线程中执行
private void updateUI(Runnable updateAction) {
    if (Platform.isFxApplicationThread()) {
        updateAction.run();
    } else {
        Platform.runLater(updateAction);
    }
}

// 示例：安全更新进度条
private void updateProgressSafely(ProgressBar progressBar, double progress) {
    updateUI(() -> progressBar.setProgress(progress));
}
```

## 性能优化 / Performance Optimization

### 线程池配置 / Thread Pool Configuration

```java
// 优化的线程池配置
private ExecutorService createOptimizedExecutorService() {
    int corePoolSize = Runtime.getRuntime().availableProcessors();
    int maximumPoolSize = corePoolSize * 2;
    long keepAliveTime = 60L;
    
    return new ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "FileManager-Task-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
}
```

### 内存优化 / Memory Optimization

```java
// 大文件处理的内存优化
private static final int LARGE_FILE_THRESHOLD = 10 * 1024 * 1024; // 10MB
private static final int SMALL_BUFFER_SIZE = 8 * 1024;            // 8KB
private static final int LARGE_BUFFER_SIZE = 64 * 1024;           // 64KB

private int getOptimalBufferSize(long fileSize) {
    return fileSize > LARGE_FILE_THRESHOLD ? LARGE_BUFFER_SIZE : SMALL_BUFFER_SIZE;
}
```

### 批量操作优化 / Batch Operation Optimization

```java
// 批量文件操作
public Future<List<Boolean>> submitBatchFileWriteTasks(
        List<FileWriteRequest> requests) {
    
    return CompletableFuture.supplyAsync(() -> {
        return requests.parallelStream()
            .map(request -> {
                try {
                    FileWriteTask task = new FileWriteTask(
                        request.getFileSystem(),
                        request.getFilePath(),
                        request.getContent()
                    );
                    return task.call();
                } catch (Exception e) {
                    LogUtil.error("批量写入失败: " + request.getFilePath(), e);
                    return false;
                }
            })
            .collect(Collectors.toList());
    }, executorService);
}
```

## 使用示例 / Usage Examples

### 基本任务使用 / Basic Task Usage

```java
// 创建任务管理器
TaskManager taskManager = new TaskManager();

// 异步读取文件
Future<String> readFuture = taskManager.submitFileReadTask(fileSystem, "/example.txt");
readFuture.thenAccept(content -> {
    System.out.println("文件内容: " + content);
});

// 异步写入文件
Future<Boolean> writeFuture = taskManager.submitFileWriteTask(
    fileSystem, "/output.txt", "Hello World", WriteMode.OVERWRITE);
writeFuture.thenAccept(success -> {
    System.out.println("写入" + (success ? "成功" : "失败"));
});

// 启动缓冲区刷新任务
taskManager.startBufferFlushTask(buffer);
```

### 进度监控示例 / Progress Monitoring Example

```java
// 创建带进度监控的文件读取任务
FileReadTask readTask = new FileReadTask(fileSystem, "/large-file.txt");

// 绑定进度更新
readTask.progressProperty().addListener((obs, oldProgress, newProgress) -> {
    Platform.runLater(() -> {
        progressBar.setProgress(newProgress.doubleValue());
    });
});

// 绑定消息更新
readTask.messageProperty().addListener((obs, oldMessage, newMessage) -> {
    Platform.runLater(() -> {
        statusLabel.setText(newMessage);
    });
});

// 设置完成回调
readTask.setOnSucceeded(e -> {
    String content = readTask.getValue();
    Platform.runLater(() -> {
        textArea.setText(content);
    });
});

// 提交任务
executorService.submit(readTask);
```

### 错误处理示例 / Error Handling Example

```java
// 创建带错误处理的写入任务
FileWriteTask writeTask = new FileWriteTask(fileSystem, "/protected.txt", content);

// 设置失败回调
writeTask.setOnFailed(e -> {
    Throwable exception = writeTask.getException();
    Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("写入失败");
        alert.setContentText("文件写入失败: " + exception.getMessage());
        alert.showAndWait();
    });
});

// 设置取消回调
writeTask.setOnCancelled(e -> {
    Platform.runLater(() -> {
        statusLabel.setText("写入操作已取消");
    });
});

// 提交任务
Future<?> future = executorService.submit(writeTask);

// 可以取消任务
cancelButton.setOnAction(e -> {
    writeTask.cancel(true);
});
```

## 扩展建议 / Extension Recommendations

### 功能扩展 / Feature Extensions

1. **任务优先级**：实现任务优先级队列，重要任务优先执行
2. **任务依赖**：支持任务之间的依赖关系管理
3. **任务持久化**：支持任务状态的持久化和恢复
4. **分布式任务**：支持跨节点的分布式任务执行
5. **任务调度**：实现基于时间和条件的任务调度

### 性能优化 / Performance Optimizations

1. **自适应线程池**：根据系统负载动态调整线程池大小
2. **智能缓存**：实现任务结果的智能缓存机制
3. **负载均衡**：在多个工作线程间均衡分配任务
4. **资源预分配**：预分配常用资源，减少运行时开销

### 监控和诊断 / Monitoring and Diagnostics

1. **性能指标**：收集和分析任务执行的性能指标
2. **健康检查**：定期检查任务和线程池的健康状态
3. **可视化监控**：提供任务执行状态的可视化界面
4. **告警机制**：在任务异常或性能下降时发送告警

## 依赖关系 / Dependencies

### 外部依赖 / External Dependencies

- `javafx.concurrent.*`: JavaFX并发框架
- `java.util.concurrent.*`: Java并发工具包
- `java.util.concurrent.atomic.*`: 原子操作类
- `java.util.stream.*`: Stream API
- `java.nio.charset.*`: 字符编码支持

### 内部依赖 / Internal Dependencies

- `org.jiejiejiang.filemanager.core.*`: 核心业务逻辑
- `org.jiejiejiang.filemanager.exception.*`: 异常定义
- `org.jiejiejiang.filemanager.util.*`: 工具类

### 被依赖关系 / Dependent Classes

- `MainController`: 主控制器使用任务进行异步操作
- `FileSystem`: 文件系统可能使用任务进行后台处理
- `Buffer`: 缓冲区使用刷新任务进行数据持久化

## 测试建议 / Testing Recommendations

### 单元测试覆盖 / Unit Test Coverage

1. **任务执行测试**：验证各种任务的正确执行
2. **进度监控测试**：验证进度更新的准确性
3. **取消机制测试**：验证任务取消的正确性
4. **异常处理测试**：验证各种异常情况的处理
5. **回调机制测试**：验证成功、失败、取消回调的执行

### 并发测试建议 / Concurrency Test Recommendations

1. **多线程安全测试**：验证多线程环境下的安全性
2. **竞态条件测试**：检测和防止竞态条件
3. **死锁检测测试**：验证不会发生死锁
4. **性能压力测试**：测试高并发场景下的性能

### 集成测试建议 / Integration Test Recommendations

1. **文件系统集成**：与FileSystem的集成测试
2. **UI集成测试**：与JavaFX界面的集成测试
3. **端到端测试**：完整的用户操作流程测试
4. **长期运行测试**：验证长期运行的稳定性

### 测试工具推荐 / Recommended Testing Tools

1. **JUnit 5**：单元测试框架
2. **TestFX**：JavaFX应用程序测试
3. **Mockito**：模拟对象框架
4. **JMH**：Java微基准测试框架
5. **VisualVM**：性能分析工具