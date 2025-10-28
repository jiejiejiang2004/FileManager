# 开发指南 / Development Guide

## 中文版 / Chinese Version

### 开发环境搭建

#### 必需工具
- **JDK**: Java 11 或更高版本
- **IDE**: IntelliJ IDEA 或 Eclipse（推荐 IntelliJ IDEA）
- **构建工具**: Maven 3.6+
- **版本控制**: Git

#### 环境配置

**1. 克隆项目**
```bash
git clone <repository-url>
cd FileManager
```

**2. 导入IDE**
- 使用 IntelliJ IDEA 打开项目根目录
- IDE 会自动识别 Maven 项目并下载依赖

**3. 配置 JavaFX**
```bash
# 如果使用 OpenJDK，需要单独安装 JavaFX
# 下载 JavaFX SDK 并配置模块路径
--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
```

**4. 运行项目**
```bash
mvn clean compile
mvn javafx:run
```

### 项目结构详解

#### 源码组织
```
src/main/java/org/jiejiejiang/filemanager/
├── core/                    # 核心业务逻辑
│   ├── Disk.java           # 磁盘模拟
│   ├── Buffer.java         # 缓冲区管理
│   ├── FAT.java            # 文件分配表
│   ├── Directory.java      # 目录管理
│   ├── FileEntry.java      # 文件项
│   ├── FileSystem.java     # 文件系统API
│   └── OpenFileTable.java  # 打开文件表
├── gui/                     # 图形界面
│   ├── FileManagerApp.java # 应用程序入口
│   └── controller/         # 控制器
│       ├── MainController.java
│       ├── NewFileDialogController.java
│       └── ...
├── exception/              # 异常定义
│   ├── FileSystemException.java
│   ├── DiskException.java
│   └── ...
├── thread/                 # 多线程任务
│   ├── FileReadTask.java
│   ├── FileWriteTask.java
│   └── BufferFlushTask.java
└── util/                   # 工具类
    ├── PathUtil.java
    ├── FileSizeUtil.java
    └── LogUtil.java
```

#### 资源文件组织
```
src/main/resources/org/jiejiejiang/filemanager/
├── fxml/                   # FXML 界面文件
│   ├── MainView.fxml
│   ├── NewFileDialog.fxml
│   └── ...
├── css/                    # 样式文件
│   └── MainViewCss.css
├── images/                 # 图标资源
│   ├── file.png
│   ├── folder.png
│   └── ...
└── config/                 # 配置文件
    └── disk.properties
```

### 核心模块开发

#### 1. 文件系统核心 (core 包)

**扩展文件系统功能**
```java
// 在 FileSystem.java 中添加新方法
public void copyFile(String sourcePath, String destPath) throws FileSystemException {
    // 1. 读取源文件内容
    String content = readFile(sourcePath);
    
    // 2. 创建目标文件
    createFile(destPath, content);
}
```

**添加新的文件类型支持**
```java
// 在 FileEntry.java 中扩展文件类型
public enum FileType {
    TEXT, IMAGE, BINARY, EXECUTABLE
}

public FileType getFileType() {
    String extension = getExtension(name);
    switch (extension.toLowerCase()) {
        case "txt": case "md": return FileType.TEXT;
        case "png": case "jpg": return FileType.IMAGE;
        case "exe": return FileType.EXECUTABLE;
        default: return FileType.BINARY;
    }
}
```

#### 2. 用户界面开发 (gui 包)

**添加新的对话框**
1. 创建 FXML 文件：
```xml
<!-- src/main/resources/org/jiejiejiang/filemanager/fxml/CustomDialog.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<DialogPane xmlns="http://javafx.com/javafx/11.0.1" xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="org.jiejiejiang.filemanager.gui.controller.CustomDialogController">
    <content>
        <VBox spacing="10">
            <Label text="自定义对话框" />
            <TextField fx:id="inputField" />
        </VBox>
    </content>
    <buttonTypes>
        <ButtonType fx:constant="OK" />
        <ButtonType fx:constant="CANCEL" />
    </buttonTypes>
</DialogPane>
```

2. 创建控制器：
```java
public class CustomDialogController {
    @FXML private TextField inputField;
    
    @FXML
    private void initialize() {
        // 初始化逻辑
    }
    
    public String getInputValue() {
        return inputField.getText();
    }
}
```

3. 在主控制器中使用：
```java
private void showCustomDialog() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/jiejiejiang/filemanager/fxml/CustomDialog.fxml"));
        DialogPane dialogPane = loader.load();
        CustomDialogController controller = loader.getController();
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        dialog.setTitle("自定义对话框");
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String value = controller.getInputValue();
            // 处理输入值
        }
    } catch (IOException e) {
        LogUtil.error("Failed to load custom dialog", e);
    }
}
```

#### 3. 多线程任务开发

**创建新的后台任务**
```java
public class CustomTask extends Task<Void> {
    private final String parameter;
    
    public CustomTask(String parameter) {
        this.parameter = parameter;
    }
    
    @Override
    protected Void call() throws Exception {
        // 后台任务逻辑
        updateProgress(0, 100);
        
        for (int i = 0; i <= 100; i++) {
            if (isCancelled()) {
                break;
            }
            
            // 执行任务
            Thread.sleep(50);
            updateProgress(i, 100);
            updateMessage("处理中... " + i + "%");
        }
        
        return null;
    }
    
    @Override
    protected void succeeded() {
        Platform.runLater(() -> {
            // 任务成功完成后的UI更新
        });
    }
    
    @Override
    protected void failed() {
        Platform.runLater(() -> {
            // 任务失败后的错误处理
            LogUtil.error("Task failed", getException());
        });
    }
}
```

### 测试开发

#### 单元测试示例
```java
@Test
public void testFileCreation() {
    FileSystem fs = new FileSystem("test-disk.img");
    
    // 测试文件创建
    assertDoesNotThrow(() -> {
        fs.createFile("/test.txt", "Hello World");
    });
    
    // 测试文件读取
    assertEquals("Hello World", fs.readFile("/test.txt"));
    
    // 测试文件存在性
    assertTrue(fs.fileExists("/test.txt"));
    
    // 清理
    fs.deleteFile("/test.txt");
}
```

#### 集成测试
```java
@Test
public void testDirectoryOperations() {
    FileSystem fs = new FileSystem("test-disk.img");
    
    // 创建目录结构
    fs.createDirectory("/folder1");
    fs.createDirectory("/folder1/subfolder");
    fs.createFile("/folder1/file1.txt", "content");
    
    // 验证目录内容
    List<FileEntry> entries = fs.listDirectory("/folder1");
    assertEquals(2, entries.size());
    
    // 清理
    fs.deleteFile("/folder1/file1.txt");
    fs.deleteDirectory("/folder1/subfolder");
    fs.deleteDirectory("/folder1");
}
```

### 性能优化

#### 1. 缓冲区优化
```java
// 调整缓冲区大小
public class Buffer {
    private static final int DEFAULT_CACHE_SIZE = 64; // 增加缓存大小
    private static final int FLUSH_INTERVAL = 5000;   // 调整刷新间隔
}
```

#### 2. 异步操作优化
```java
// 使用 CompletableFuture 进行异步操作
public CompletableFuture<String> readFileAsync(String path) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            return readFile(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
}
```

#### 3. UI 响应性优化
```java
// 使用 Platform.runLater 更新 UI
private void updateFileList() {
    Task<List<FileEntry>> task = new Task<List<FileEntry>>() {
        @Override
        protected List<FileEntry> call() throws Exception {
            return fileSystem.listDirectory(currentPath);
        }
    };
    
    task.setOnSucceeded(e -> {
        Platform.runLater(() -> {
            fileTableView.getItems().setAll(task.getValue());
        });
    });
    
    new Thread(task).start();
}
```

### 代码规范

#### 命名规范
- **类名**: 使用 PascalCase，如 `FileSystem`
- **方法名**: 使用 camelCase，如 `createFile`
- **常量**: 使用 UPPER_SNAKE_CASE，如 `MAX_FILE_SIZE`
- **包名**: 使用小写，如 `org.jiejiejiang.filemanager.core`

#### 注释规范
```java
/**
 * 创建新文件
 * 
 * @param path 文件路径，必须是绝对路径
 * @param content 文件内容，不能为null
 * @throws FileSystemException 当文件创建失败时抛出
 * @throws IllegalArgumentException 当路径格式不正确时抛出
 */
public void createFile(String path, String content) throws FileSystemException {
    // 实现逻辑
}
```

#### 异常处理
```java
public void riskyOperation() {
    try {
        // 可能抛出异常的操作
        performOperation();
    } catch (SpecificException e) {
        // 处理特定异常
        LogUtil.error("Specific error occurred", e);
        throw new FileSystemException("Operation failed", e);
    } catch (Exception e) {
        // 处理通用异常
        LogUtil.error("Unexpected error", e);
        throw new FileSystemException("Unexpected error occurred", e);
    }
}
```

### 调试技巧

#### 1. 日志记录
```java
// 使用 LogUtil 记录日志
LogUtil.info("File created: " + path);
LogUtil.debug("Block allocated: " + blockId);
LogUtil.error("Failed to read file: " + path, exception);
```

#### 2. 断点调试
- 在关键方法入口设置断点
- 检查变量状态和调用栈
- 使用条件断点过滤特定情况

#### 3. 性能分析
```java
// 使用简单的性能计时
long startTime = System.currentTimeMillis();
performOperation();
long endTime = System.currentTimeMillis();
LogUtil.debug("Operation took: " + (endTime - startTime) + "ms");
```

---

## English Version

### Development Environment Setup

#### Required Tools
- **JDK**: Java 11 or higher
- **IDE**: IntelliJ IDEA or Eclipse (IntelliJ IDEA recommended)
- **Build Tool**: Maven 3.6+
- **Version Control**: Git

#### Environment Configuration

**1. Clone Project**
```bash
git clone <repository-url>
cd FileManager
```

**2. Import to IDE**
- Open project root directory with IntelliJ IDEA
- IDE will automatically recognize Maven project and download dependencies

**3. Configure JavaFX**
```bash
# If using OpenJDK, JavaFX needs to be installed separately
# Download JavaFX SDK and configure module path
--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
```

**4. Run Project**
```bash
mvn clean compile
mvn javafx:run
```

### Project Structure Details

#### Source Code Organization
```
src/main/java/org/jiejiejiang/filemanager/
├── core/                    # Core business logic
│   ├── Disk.java           # Disk simulation
│   ├── Buffer.java         # Buffer management
│   ├── FAT.java            # File allocation table
│   ├── Directory.java      # Directory management
│   ├── FileEntry.java      # File entry
│   ├── FileSystem.java     # File system API
│   └── OpenFileTable.java  # Open file table
├── gui/                     # Graphical interface
│   ├── FileManagerApp.java # Application entry point
│   └── controller/         # Controllers
│       ├── MainController.java
│       ├── NewFileDialogController.java
│       └── ...
├── exception/              # Exception definitions
│   ├── FileSystemException.java
│   ├── DiskException.java
│   └── ...
├── thread/                 # Multi-threading tasks
│   ├── FileReadTask.java
│   ├── FileWriteTask.java
│   └── BufferFlushTask.java
└── util/                   # Utility classes
    ├── PathUtil.java
    ├── FileSizeUtil.java
    └── LogUtil.java
```

#### Resource File Organization
```
src/main/resources/org/jiejiejiang/filemanager/
├── fxml/                   # FXML interface files
│   ├── MainView.fxml
│   ├── NewFileDialog.fxml
│   └── ...
├── css/                    # Style files
│   └── MainViewCss.css
├── images/                 # Icon resources
│   ├── file.png
│   ├── folder.png
│   └── ...
└── config/                 # Configuration files
    └── disk.properties
```

### Core Module Development

#### 1. File System Core (core package)

**Extending File System Functionality**
```java
// Add new method in FileSystem.java
public void copyFile(String sourcePath, String destPath) throws FileSystemException {
    // 1. Read source file content
    String content = readFile(sourcePath);
    
    // 2. Create destination file
    createFile(destPath, content);
}
```

**Adding New File Type Support**
```java
// Extend file types in FileEntry.java
public enum FileType {
    TEXT, IMAGE, BINARY, EXECUTABLE
}

public FileType getFileType() {
    String extension = getExtension(name);
    switch (extension.toLowerCase()) {
        case "txt": case "md": return FileType.TEXT;
        case "png": case "jpg": return FileType.IMAGE;
        case "exe": return FileType.EXECUTABLE;
        default: return FileType.BINARY;
    }
}
```

#### 2. User Interface Development (gui package)

**Adding New Dialogs**
1. Create FXML file:
```xml
<!-- src/main/resources/org/jiejiejiang/filemanager/fxml/CustomDialog.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<DialogPane xmlns="http://javafx.com/javafx/11.0.1" xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="org.jiejiejiang.filemanager.gui.controller.CustomDialogController">
    <content>
        <VBox spacing="10">
            <Label text="Custom Dialog" />
            <TextField fx:id="inputField" />
        </VBox>
    </content>
    <buttonTypes>
        <ButtonType fx:constant="OK" />
        <ButtonType fx:constant="CANCEL" />
    </buttonTypes>
</DialogPane>
```

2. Create controller:
```java
public class CustomDialogController {
    @FXML private TextField inputField;
    
    @FXML
    private void initialize() {
        // Initialization logic
    }
    
    public String getInputValue() {
        return inputField.getText();
    }
}
```

3. Use in main controller:
```java
private void showCustomDialog() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/jiejiejiang/filemanager/fxml/CustomDialog.fxml"));
        DialogPane dialogPane = loader.load();
        CustomDialogController controller = loader.getController();
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        dialog.setTitle("Custom Dialog");
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String value = controller.getInputValue();
            // Process input value
        }
    } catch (IOException e) {
        LogUtil.error("Failed to load custom dialog", e);
    }
}
```

#### 3. Multi-threading Task Development

**Creating New Background Tasks**
```java
public class CustomTask extends Task<Void> {
    private final String parameter;
    
    public CustomTask(String parameter) {
        this.parameter = parameter;
    }
    
    @Override
    protected Void call() throws Exception {
        // Background task logic
        updateProgress(0, 100);
        
        for (int i = 0; i <= 100; i++) {
            if (isCancelled()) {
                break;
            }
            
            // Execute task
            Thread.sleep(50);
            updateProgress(i, 100);
            updateMessage("Processing... " + i + "%");
        }
        
        return null;
    }
    
    @Override
    protected void succeeded() {
        Platform.runLater(() -> {
            // UI update after successful completion
        });
    }
    
    @Override
    protected void failed() {
        Platform.runLater(() -> {
            // Error handling after task failure
            LogUtil.error("Task failed", getException());
        });
    }
}
```

### Test Development

#### Unit Test Example
```java
@Test
public void testFileCreation() {
    FileSystem fs = new FileSystem("test-disk.img");
    
    // Test file creation
    assertDoesNotThrow(() -> {
        fs.createFile("/test.txt", "Hello World");
    });
    
    // Test file reading
    assertEquals("Hello World", fs.readFile("/test.txt"));
    
    // Test file existence
    assertTrue(fs.fileExists("/test.txt"));
    
    // Cleanup
    fs.deleteFile("/test.txt");
}
```

#### Integration Test
```java
@Test
public void testDirectoryOperations() {
    FileSystem fs = new FileSystem("test-disk.img");
    
    // Create directory structure
    fs.createDirectory("/folder1");
    fs.createDirectory("/folder1/subfolder");
    fs.createFile("/folder1/file1.txt", "content");
    
    // Verify directory content
    List<FileEntry> entries = fs.listDirectory("/folder1");
    assertEquals(2, entries.size());
    
    // Cleanup
    fs.deleteFile("/folder1/file1.txt");
    fs.deleteDirectory("/folder1/subfolder");
    fs.deleteDirectory("/folder1");
}
```

### Performance Optimization

#### 1. Buffer Optimization
```java
// Adjust buffer size
public class Buffer {
    private static final int DEFAULT_CACHE_SIZE = 64; // Increase cache size
    private static final int FLUSH_INTERVAL = 5000;   // Adjust flush interval
}
```

#### 2. Asynchronous Operation Optimization
```java
// Use CompletableFuture for asynchronous operations
public CompletableFuture<String> readFileAsync(String path) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            return readFile(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
}
```

#### 3. UI Responsiveness Optimization
```java
// Use Platform.runLater to update UI
private void updateFileList() {
    Task<List<FileEntry>> task = new Task<List<FileEntry>>() {
        @Override
        protected List<FileEntry> call() throws Exception {
            return fileSystem.listDirectory(currentPath);
        }
    };
    
    task.setOnSucceeded(e -> {
        Platform.runLater(() -> {
            fileTableView.getItems().setAll(task.getValue());
        });
    });
    
    new Thread(task).start();
}
```

### Code Standards

#### Naming Conventions
- **Class names**: Use PascalCase, e.g., `FileSystem`
- **Method names**: Use camelCase, e.g., `createFile`
- **Constants**: Use UPPER_SNAKE_CASE, e.g., `MAX_FILE_SIZE`
- **Package names**: Use lowercase, e.g., `org.jiejiejiang.filemanager.core`

#### Comment Standards
```java
/**
 * Creates a new file
 * 
 * @param path File path, must be absolute path
 * @param content File content, cannot be null
 * @throws FileSystemException When file creation fails
 * @throws IllegalArgumentException When path format is incorrect
 */
public void createFile(String path, String content) throws FileSystemException {
    // Implementation logic
}
```

#### Exception Handling
```java
public void riskyOperation() {
    try {
        // Operations that may throw exceptions
        performOperation();
    } catch (SpecificException e) {
        // Handle specific exceptions
        LogUtil.error("Specific error occurred", e);
        throw new FileSystemException("Operation failed", e);
    } catch (Exception e) {
        // Handle general exceptions
        LogUtil.error("Unexpected error", e);
        throw new FileSystemException("Unexpected error occurred", e);
    }
}
```

### Debugging Tips

#### 1. Logging
```java
// Use LogUtil for logging
LogUtil.info("File created: " + path);
LogUtil.debug("Block allocated: " + blockId);
LogUtil.error("Failed to read file: " + path, exception);
```

#### 2. Breakpoint Debugging
- Set breakpoints at key method entry points
- Check variable states and call stack
- Use conditional breakpoints to filter specific cases

#### 3. Performance Analysis
```java
// Use simple performance timing
long startTime = System.currentTimeMillis();
performOperation();
long endTime = System.currentTimeMillis();
LogUtil.debug("Operation took: " + (endTime - startTime) + "ms");
```