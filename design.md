### **阶段1：核心文件系统逻辑（无界面，优先保证数据层正确）**
**目标**：实现磁盘、文件、目录的基础操作逻辑，可通过单元测试验证。  
**开发内容**：
1. **`core/Disk.java`**
    - 实现磁盘块的初始化（从配置文件读取块大小、总块数）。
    - 完成 `readBlock(int blockId)` 和 `writeBlock(int blockId, byte[] data)` 核心方法。
    - 简单测试：写入数据到某块，再读取验证是否一致。

2. **`core/FAT.java`**
    - 初始化FAT表（数组存储，-1表示块末尾，0表示空闲）。
    - 实现 `findFreeBlock()`（找空闲块）、`setNextBlock()`（设置块链接）、`releaseBlocks()`（释放文件占用的块）。
    - 测试：分配3个连续块，验证FAT表链接是否正确；删除后验证块是否被标记为空闲。

3. **`core/FileEntry.java` 与 `core/Directory.java`**
    - `FileEntry`：存储文件名、大小、起始块、只读属性等，提供 `getSize()`、`isReadOnly()` 等方法。
    - `Directory`：实现多级目录（父目录引用、子目录列表、文件列表），完成 `addFile()`、`removeFile()`、`addSubDirectory()` 等基础操作。
    - 测试：创建 `/a/b/c` 三级目录，在 `c` 中创建文件 `test.txt`，验证目录结构是否正确。

4. **`core/FileSystem.java`**
    - 整合上述类，提供对外接口：`createFile(String path, String name)`、`deleteFile(String path, String name)`、`createDirectory(String path, String name)` 等。
    - 处理路径解析（依赖后续 `util/PathUtil`，可先简单实现）。
    - 测试：通过 `FileSystem` 接口完成文件/目录的创建、删除、路径导航，验证底层FAT和磁盘块是否正确更新。


### **阶段2：工具类与异常（支撑核心逻辑）**
**目标**：解决通用问题（路径处理、异常提示），避免核心代码冗余。  
**开发内容**：
1. **`util/PathUtil.java`**
    - 实现路径解析：`splitPath(String path)`（拆分 `/a/b/c` 为列表 `[a, b, c]`）、`getAbsolutePath(Directory currentDir, String relativePath)` 等。
    - 处理特殊路径（`.` 表示当前目录、`..` 表示父目录）。

2. **`exception/` 包**
    - 定义 `FileNotFoundException`、`DiskFullException`、`ReadOnlyException` 等自定义异常，在 `FileSystem` 操作中抛出。

3. **`util/FileSizeUtil.java`**
    - 实现字节与KB/MB的转换（如 `format(1500)` 返回 `1.46 KB`），后续UI展示用。


### **阶段3：JavaFX界面基础框架（搭建UI骨架）**
**目标**：实现界面布局，完成控制器与视图的绑定，暂不关联业务逻辑。  
**开发内容**：
1. **`resources/fxml/MainView.fxml`**
    - 设计主界面布局：左侧 `TreeView`（目录树）、右侧 `ListView` 或 `TableView`（文件列表）、顶部工具栏（按钮：新建文件/目录、删除、刷新）。

2. **`gui/FileManagerApp.java`**
    - JavaFX入口类：加载 `MainView.fxml`，设置窗口标题、尺寸（从配置文件读取）。

3. **`gui/controller/MainController.java`**
    - 绑定FXML组件（`@FXML TreeView<Directory> directoryTree;` 等）。
    - 初始化界面：暂时显示静态目录树和文件列表（不关联真实 `FileSystem`）。


### **阶段4：界面与核心逻辑关联（实现基础交互）**
**目标**：让UI操作触发实际的文件系统逻辑，完成核心功能闭环。  
**开发内容**：
1. **目录树与文件列表刷新**
    - 在 `MainController` 中初始化 `FileSystem` 实例。
    - 实现 `refreshDirectoryTree()`：从根目录加载目录结构到 `TreeView`。
    - 实现 `refreshFileList(Directory currentDir)`：将目录下的文件/子目录显示到右侧列表。

2. **基础操作按钮绑定**
    - “新建目录”：点击后弹出输入框（后续可优化为 `NewDirectoryDialog`），调用 `fileSystem.createDirectory(...)`，然后刷新界面。
    - “新建文件”：类似新建目录，调用 `fileSystem.createFile(...)`。
    - “删除”：获取选中的文件/目录，调用 `fileSystem.delete(...)`，处理异常（如删除只读文件时弹窗提示）。

3. **路径导航**
    - 点击目录树节点时，自动更新右侧文件列表为该目录内容。


### **阶段5：多线程与高级功能（提升用户体验）**
**目标**：解决UI卡顿问题，实现文件内容读写等复杂功能。  
**开发内容**：
1. **`thread/` 包**
    - `FileReadTask.java`：继承 `javafx.concurrent.Task`，在后台读取文件内容，完成后通过 `Platform.runLater()` 更新UI（如显示到文本框）。
    - `FileWriteTask.java`：后台写入文件内容，避免大文件写入时界面冻结。
    - 绑定到UI：“打开文件”按钮触发 `FileReadTask`，“保存文件”触发 `FileWriteTask`。

2. **`core/Buffer.java`**
    - 实现缓冲区逻辑：读取时先查缓存，写入时先写缓存，定时通过 `BufferFlushTask`（后台线程）刷新到磁盘。
    - 在 `FileSystem` 中集成缓冲区，提升读写效率。

3. **对话框完善**
    - 开发 `NewFileDialog.fxml` + `NewFileDialogController`：支持输入文件名和初始内容。
    - 开发 `ErrorDialog.fxml`：统一展示异常信息（如磁盘满、文件不存在）。


### **阶段6：UI美化与优化（细节打磨）**
**目标**：提升界面美观度和易用性，修复潜在问题。  
**开发内容**：
1. **CSS样式**
    - 为 `TreeView`、`ListView`、按钮添加样式（如选中项高亮、悬停效果）。
    - 统一对话框样式，确保风格一致。

2. **图标与交互细节**
    - 在文件列表中根据类型显示图标（文件夹、文本文件等，使用 `images/` 资源）。
    - 添加操作成功提示（如“文件创建成功”），优化用户反馈。

3. **配置文件生效**
    - 从 `config/disk.properties` 加载磁盘参数，`config/ui-config.properties` 加载窗口尺寸。


### **阶段7：测试与文档（确保稳定性）**
**目标**：全面验证功能，补充说明文档。  
**开发内容**：
1. **功能测试**
    - 模拟各种场景：磁盘满时创建文件、删除非空目录、并发读写同一文件等。
    - 验证异常处理是否正确（如只读文件不可写、路径不存在时提示）。

2. **性能优化**
    - 测试大文件读写时的UI流畅度（依赖多线程任务）。
    - 检查缓冲区是否有效减少了磁盘IO次数。

3. **文档完善**
    - 补充 `Readme_zh.md` 中的操作说明、常见问题。
    - 在代码中添加关键注释（如复杂算法、核心逻辑步骤）。


### **开发顺序的优势**
1. **先核心后界面**：确保文件系统逻辑正确后再对接UI，减少“界面能用但逻辑错误”的调试成本。
2. **分步验证**：每个阶段都可独立测试（如阶段1用单元测试，阶段3用静态UI），早期发现问题。
3. **依赖合理**：工具类、异常类在核心逻辑稳定后开发，避免反复修改；多线程等高级功能在基础交互跑通后添加，降低复杂度。

按照这个顺序，大约可分为2-3周的开发周期（视功能复杂度调整），每完成一个阶段都能看到可运行的成果，便于把控进度。