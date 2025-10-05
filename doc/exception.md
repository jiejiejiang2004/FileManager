# 自定义异常类文档

## 概述
`org.jiejiejiang.filemanager.exception` 包下的自定义异常类，用于明确区分文件系统运行中的不同错误场景，替代泛化的 `RuntimeException`，使错误定位更精准、上层代码处理更具针对性。

所有异常均继承自 `java.lang.RuntimeException`（运行时异常），避免强制要求调用者捕获，简化代码逻辑；同时提供多构造器支持，可灵活传递错误信息和异常根源。


## 异常类列表及说明

| 异常类名                         | 核心场景                          | 构造器说明                                                                                   |
|------------------------------|-------------------------------|-----------------------------------------------------------------------------------------|
| `DiskInitializeException`    | 磁盘初始化相关错误                     | 1. 无参构造<br>2. 带错误消息（`String message`）<br>3. 带消息+异常根源（`String message, Throwable cause`） |
| `InvalidBlockIdException`    | 访问的磁盘块ID非法（如小于0、大于等于总块数）      | 同上                                                                                      |
| `DiskWriteException`         | 磁盘块写入失败（如权限不足、文件损坏、IO错误）      | 同上                                                                                      |
| `FileNotFoundException`      | 尝试操作不存在的文件（如读取/删除未创建的文件）      | 同上                                                                                      |
| `DiskFullException`          | 磁盘空间不足（无法分配新的磁盘块）             | 同上                                                                                      |
| `ReadOnlyException`          | 尝试修改只读属性的文件（如写入/删除标记为只读的文件）   | 同上                                                                                      |
| `InvalidPathException`       | 路径格式无效（如空路径、包含非法字符、无法解析的相对路径） | 同上                                                                                      |
| `DirectoryNotEmptyException` | 删除非空目录（仅允许删除无文件/子目录的空目录）      | 同上                                                                                      |


## 异常使用规范

### 1. 抛出原则
- **精准匹配场景**：每个错误场景必须对应唯一的异常类，禁止用通用异常替代。  
  示例：磁盘块ID为 `-1` 时，必须抛 `InvalidBlockIdException`，而非 `RuntimeException`。
- **携带完整信息**：抛出时需包含具体错误细节（如非法块ID、目标路径、文件名称），便于调试。  
  示例：
  ```java
  // 错误：信息模糊
  throw new FileNotFoundException("文件不存在");
  
  // 正确：包含具体路径
  throw new FileNotFoundException("文件不存在：路径=" + parentDir.getAbsolutePath() + "，文件名=" + fileName);
  ```
- **传递异常根源**：若错误由底层异常（如 `IOException`）触发，需通过构造器传递 `cause`，保留完整异常链。  
  示例：
  ```java
  try {
      diskFile.write(writeData);
  } catch (IOException e) {
      // 传递IO异常根源
      throw new DiskWriteException("写入块失败：blockId=" + blockId, e);
  }
  ```

### 2. 捕获处理建议
- **分层处理**：底层（如 `Disk`、`FAT`）仅抛出异常，由上层（如 `FileSystem`、`Controller`）统一捕获并处理（如弹窗提示、日志记录）。  
  示例（`MainController` 中处理）：
  ```java
  try {
      fileSystem.deleteFile(currentPath, fileName);
  } catch (FileNotFoundException e) {
      // 弹窗提示用户
      ErrorDialog.show("删除失败", e.getMessage());
  } catch (ReadOnlyException e) {
      ErrorDialog.show("权限不足", e.getMessage());
  }
  ```
- **避免吞异常**：禁止捕获异常后不处理（如仅打印日志不提示用户），确保错误能被感知。


## 异常类代码实现

### 1. 基础模板（以 `DiskInitializeException` 为例）
所有异常类结构一致，仅类名和场景说明不同：
```java
package org.jiejiejiang.filemanager.exception;

/**
 * 磁盘初始化异常：当磁盘配置加载失败、物理文件创建失败等初始化相关错误时抛出
 */
public class DiskInitializeException extends RuntimeException {

    /**
     * 无参构造
     */
    public DiskInitializeException() {
        super();
    }

    /**
     * 带错误消息的构造器
     * @param message 错误详细信息
     */
    public DiskInitializeException(String message) {
        super(message);
    }

    /**
     * 带错误消息和异常根源的构造器
     * @param message 错误详细信息
     * @param cause 底层异常根源（如IOException）
     */
    public DiskInitializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 2. 其他异常类快速实现
只需复制上述模板，修改类名、类注释即可，以下为部分示例：

#### `InvalidBlockIdException.java`
```java
package org.jiejiejiang.filemanager.exception;

/**
 * 无效块ID异常：当访问的磁盘块ID小于0或大于等于磁盘总块数时抛出
 */
public class InvalidBlockIdException extends RuntimeException {

    public InvalidBlockIdException() {
        super();
    }

    public InvalidBlockIdException(String message) {
        super(message);
    }

    public InvalidBlockIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### `DirectoryNotEmptyException.java`
```java
package org.jiejiejiang.filemanager.exception;

/**
 * 目录非空异常：当尝试删除包含文件或子目录的非空目录时抛出
 */
public class DirectoryNotEmptyException extends RuntimeException {

    public DirectoryNotEmptyException() {
        super();
    }

    public DirectoryNotEmptyException(String message) {
        super(message);
    }

    public DirectoryNotEmptyException(String message, Throwable cause) {
        super(message, cause);
    }
}
```


## 注意事项
1. **异常命名规范**：所有异常类名以 `Exception` 结尾，前缀明确表达错误场景（如 `InvalidBlockId`、`DiskFull`）。
2. **避免重复异常**：新增错误场景时，先检查现有异常是否覆盖，不重复创建同类异常。
3. **文档同步**：若新增或修改异常类，需同步更新此文档，确保文档与代码一致。