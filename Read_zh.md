# Simple File Manager（JavaFX 实现）

一款基于 Java 和 JavaFX 开发的轻量级文件系统模拟应用，具备图形用户界面（GUI）、多线程操作能力，支持目录管理、文件读写、磁盘空间模拟等核心文件系统功能。

## 项目概述

本项目通过模拟基础文件系统，演示以下核心技术概念：

- **磁盘模拟**：使用文件块和 I/O 操作模拟物理磁盘。
- **文件分配表（FAT）**：管理磁盘块分配与文件块链接关系。
- **多级目录结构**：支持嵌套目录（创建/删除/导航目录）。
- **图形化界面**：基于 JavaFX 开发直观 UI，无需命令行即可完成文件操作。
- **多线程处理**：文件读写、缓冲区刷新等操作在后台线程执行，避免界面卡顿。

## 技术栈

| 分类     | 工具 / 库                      |
|--------|-----------------------------|
| 开发语言   | Java 11+（需兼容 JavaFX）        |
| GUI 框架 | JavaFX（用于构建交互式界面）           |
| 构建工具   | Maven/Gradle（可选，用于依赖管理）     |
| 资源管理   | FXML（UI 布局）、CSS（样式）、PNG（图标） |

## 项目结构

项目遵循 **MVC（模型-视图-控制器）** 设计模式，且符合 Java 标准资源目录规范：

```
src/
├── main/
│   ├── java/org/jiejiejiang/filemanager/  # 核心逻辑与控制器
│   │   ├── core/          # 模型层：文件系统核心业务逻辑
│   │   │   ├── Disk.java          # 模拟磁盘块读写
│   │   │   ├── Buffer.java        # 缓冲区（优化磁盘读写效率）
│   │   │   ├── FAT.java           # 文件分配表
│   │   │   ├── FileEntry.java     # 文件元数据（名称、大小、起始块等）
│   │   │   ├── Directory.java     # 多级目录管理
│   │   │   └── FileSystem.java    # 核心 API（创建/删除/读写文件）
│   │   │
│   │   ├── gui/           # 视图与控制器层
│   │   │   ├── FileManagerApp.java  # JavaFX 应用入口
│   │   │   ├── controller/          # 控制器（关联 FXML 与业务逻辑）
│   │   │   │   ├── MainController.java       # 主窗口控制器
│   │   │   │   ├── NewFileDialogController.java  # 新建文件对话框控制器
│   │   │   │   └── ...（其他对话框控制器）
│   │   │   └── view/               # 可复用 UI 组件（可选）
│   │   │
│   │   ├── thread/        # 多线程任务（避免界面卡顿）
│   │   │   ├── FileReadTask.java   # 后台文件读取任务
│   │   │   ├── FileWriteTask.java  # 后台文件写入任务
│   │   │   └── BufferFlushTask.java # 缓冲区自动刷盘任务
│   │   │
│   │   ├── exception/     # 自定义业务异常
│   │   └── util/          # 工具类（路径处理、日志、文件大小转换等）
│   │
│   └── resources/org/jiejiejiang/filemanager/  # 非代码资源
│       ├── fxml/          # FXML UI 布局文件（与控制器一一对应）
│       ├── css/           # UI 样式文件（main.css、dialogs.css 等）
│       ├── images/        # 图标资源（新建文件、文件夹、删除等）
│       └── config/        # 配置文件（磁盘块大小、窗口尺寸等）
```

## 核心功能

### 1. 文件操作

- 创建新文件（支持基础文本内容）。
- 删除文件（自动校验只读属性，只读文件禁止删除）。
- 读写文件内容（大文件操作通过后台线程执行，不阻塞界面）。
- 修改文件属性（如设置为只读状态）。

### 2. 目录操作

- 创建嵌套目录（支持多级目录层级）。
- 删除空目录（非空目录需先删除内部文件/子目录）。
- 目录树导航（通过界面左侧 TreeView 组件快速切换目录）。
- 目录内容列表（右侧面板展示当前目录下的文件与子目录）。

### 3. 磁盘与缓冲区管理

- 可配置磁盘参数（块大小、总块数可通过配置文件修改）。
- 缓冲区缓存（减少直接磁盘 I/O 次数，提升操作效率）。
- 缓冲区自动刷盘（后台线程定时将缓冲区数据写入磁盘，防止数据丢失）。

### 4. 用户界面

- 直观布局：左侧目录树 + 右侧文件列表 + 顶部操作工具栏。
- 交互式对话框：新建文件、重命名、错误提示等场景弹出引导式对话框。
- 响应式设计：支持窗口大小调整，组件自动适配布局。
- 文件类型图标：文本文件、图片文件、文件夹等显示不同图标，提升辨识度。

## 运行步骤

### 前置条件

- 安装 Java 11 及以上版本（JDK 11+ 已内置 JavaFX；若使用低版本 JDK，需手动导入 JavaFX SDK）。
- 可选：安装 Maven/Gradle（用于构建项目；也可直接通过 IDE 运行）。

### 具体步骤

1. **克隆/下载项目**

   将项目源码保存到本地电脑。

2. **配置 JavaFX（如需）**

   若使用 JDK 11 以下版本，需先添加 JavaFX SDK 依赖，并配置 VM 参数：

   ```
   --module-path /你的路径/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```

3. **启动应用**

    - **通过 IDE（IntelliJ/Eclipse）**：

      找到 `gui` 包下的 `FileManagerApp.java`，右键点击 →「运行」（确保 FXML 及资源文件已加入类路径）。

    - **通过 Maven**：

      先在 pom.xml 中配置 `javafx-maven-plugin` 插件，再执行命令：

      ```
      mvn clean javafx:run
      ```

## 配置说明

可通过 `src/main/resources/org/jiejiejiang/filemanager/config/` 目录下的配置文件调整项目行为：

- **disk.properties（磁盘配置）**：
    - `disk.block.size`：单个磁盘块大小（默认 1024 字节）。
    - `disk.total.blocks`：模拟磁盘的总块数（默认 1024 块）。

- **ui-config.properties（界面配置）**：
    - `window.width`：主窗口默认宽度（默认 800 像素）。
    - `window.height`：主窗口默认高度（默认 600 像素）。

## 开发注意事项

- **控制器与 FXML 绑定**：每个 FXML 文件需通过 `fx:controller` 指定对应控制器，示例：

  ```xml
  <BorderPane xmlns:fx="http://javafx.com/fxml"
              fx:controller="org.jiejiejiang.filemanager.gui.controller.MainController">
  ```

- **线程安全**：从后台线程更新 UI 时，需使用 `Platform.runLater()` 或 JavaFX `Task` 类（JavaFX UI 为单线程模型）。

- **测试建议**：先独立测试核心逻辑（如 `FileSystem`、`FAT` 类），再测试 UI 交互功能。

## 许可说明

本项目仅供学习使用（如课程设计、技术练习），可自由修改与扩展。