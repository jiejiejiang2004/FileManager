# GUI控制器类技术文档 / GUI Controllers Class Technical Documentation

## 概述 / Overview

**中文概述**：
GUI控制器类是文件管理系统中的用户界面控制层，负责处理用户交互、管理界面状态和协调业务逻辑。主要包括`MainController`（主窗口控制器）和`DiskViewerController`（磁盘查看器控制器）两个核心控制器。这些控制器基于JavaFX框架实现，采用FXML注入模式，提供了完整的文件管理界面功能，包括目录导航、文件操作、视图切换、FAT监控和磁盘分析等功能。

**English Overview**：
The GUI controller classes serve as the user interface control layer in the file management system, responsible for handling user interactions, managing interface states, and coordinating business logic. The main components include `MainController` (main window controller) and `DiskViewerController` (disk viewer controller). These controllers are implemented based on the JavaFX framework using FXML injection patterns, providing complete file management interface functionality including directory navigation, file operations, view switching, FAT monitoring, and disk analysis.

## 架构设计 / Architecture Design

### MVC模式实现 / MVC Pattern Implementation

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     View        │    │   Controller    │    │     Model       │
│   (FXML UI)     │◄──►│  (MainController│◄──►│  (FileSystem)   │
│                 │    │ DiskViewer...)  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 控制器层次结构 / Controller Hierarchy

```
AbstractController (概念基类)
├── MainController (主窗口控制器)
│   ├── 文件管理功能
│   ├── 目录导航功能
│   ├── 视图切换功能
│   └── FAT监控功能
└── DiskViewerController (磁盘查看器控制器)
    ├── 磁盘分析功能
    ├── 块内容查看功能
    └── 使用率统计功能
```

## MainController 主窗口控制器 / Main Window Controller

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.gui.controller;

@Controller
public class MainController {
    // 主窗口控制器实现
}
```

### 核心特性 / Core Features

#### 1. 多视图模式支持 / Multi-View Mode Support
- **列表视图**：基于TableView的详细文件列表
- **图标视图**：基于FlowPane的图标化文件展示
- **动态切换**：运行时无缝切换视图模式

#### 2. 目录树导航 / Directory Tree Navigation
- **层次结构展示**：TreeView展示完整目录结构
- **懒加载机制**：按需加载子目录，提升性能
- **路径同步**：目录树与文件列表保持同步

#### 3. 文件操作管理 / File Operation Management
- **CRUD操作**：创建、读取、更新、删除文件和目录
- **批量操作**：支持多选和批量处理
- **上下文菜单**：右键菜单提供快捷操作

#### 4. FAT实时监控 / Real-time FAT Monitoring
- **块状态监控**：实时显示FAT表状态
- **使用率统计**：动态计算和显示磁盘使用率
- **可视化展示**：图形化展示存储分配情况

### FXML组件注入 / FXML Component Injection

#### 主界面组件 / Main Interface Components

```java
public class MainController {
    // 目录导航组件
    @FXML private TreeView<String> dirTreeView;
    @FXML private TreeItem<String> computerRootItem;
    
    // 文件列表组件
    @FXML private TableView<FileEntry> fileTableView;
    @FXML private TableColumn<FileEntry, String> nameColumn;
    @FXML private TableColumn<FileEntry, String> typeColumn;
    @FXML private TableColumn<FileEntry, Long> sizeColumn;
    @FXML private TableColumn<FileEntry, String> modifyTimeColumn;
    
    // 状态显示组件
    @FXML private Label currentPathLabel;
    @FXML private Label fileCountLabel;
    @FXML private Button backButton;
    @FXML private TextField pathTextField;
}
```

#### FAT监控组件 / FAT Monitoring Components

```java
// FAT监视器组件
@FXML private TableView<FatRow> fatTableView;
@FXML private TableColumn<FatRow, Integer> fatBlockIdColumn;
@FXML private TableColumn<FatRow, String> fatValueColumn;
@FXML private TableColumn<FatRow, String> fatStatusColumn;
@FXML private Label fatFreeCountLabel;
@FXML private Label fatUsedCountLabel;
@FXML private Label fatBadCountLabel;
```

#### 菜单和工具栏组件 / Menu and Toolbar Components

```java
// 菜单组件
@FXML private MenuItem newFileItem;
@FXML private MenuItem newDirItem;
@FXML private MenuItem openItem;
@FXML private MenuItem deleteItem;
@FXML private MenuItem refreshItem;
@FXML private MenuItem listViewItem;
@FXML private MenuItem iconViewItem;
@FXML private MenuItem diskViewerItem;

// 视图切换组件
@FXML private Button toggleViewButton;
@FXML private ScrollPane iconViewScrollPane;
@FXML private FlowPane iconViewPane;
```

### 业务对象管理 / Business Object Management

```java
// 核心业务对象
private FileSystem fileSystem;           // 文件系统核心对象（外部注入）
private Directory currentDirectory;      // 当前选中的目录

// 状态管理
private long lastClickTime = 0L;
private int lastClickedRowIndex = -1;
private static final int DOUBLE_CLICK_THRESHOLD_MS = 350;

// 视图模式管理
public enum ViewMode {
    LIST,   // 列表模式（表格）
    ICON    // 图标模式
}
private ViewMode currentViewMode = ViewMode.LIST;
```

### 初始化方法 / Initialization Methods

#### initialize() 主初始化方法

```java
@FXML
public void initialize() {
    // 1. 初始化表格列与FileEntry属性绑定
    initTableColumns();
    
    // 2. 初始化FAT监视器表格列
    initFatTableColumns();
    
    // 3. 初始化目录树（模拟加载磁盘，实际应从fileSystem获取）
    initDirectoryTree();
    
    // 4. 绑定事件监听器
    bindEvents();
}
```

#### initTableColumns() 表格列初始化

```java
private void initTableColumns() {
    // 文件名列
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    
    // 类型列：转换为"文件"或"文件夹"显示
    typeColumn.setCellValueFactory(cellData -> {
        FileEntry entry = cellData.getValue();
        return new SimpleStringProperty(
            entry.getType() == FileEntry.EntryType.FILE ? "文件" : "文件夹"
        );
    });

    // 大小列：文件显示大小，文件夹显示"-"
    sizeColumn.setCellValueFactory(cellData -> {
        FileEntry entry = cellData.getValue();
        long size = (entry.getType() == FileEntry.EntryType.FILE) ? entry.getSize() : -1;
        return new SimpleLongProperty(size).asObject();
    });
    
    // 自定义大小列显示格式
    sizeColumn.setCellFactory(column -> new TableCell<>() {
        @Override
        protected void updateItem(Long size, boolean empty) {
            super.updateItem(size, empty);
            if (empty || size == -1) {
                setText("-");
            } else {
                setText(String.format("%.2f KB", size / 1024.0));
            }
        }
    });

    // 修改时间列：格式化显示
    modifyTimeColumn.setCellValueFactory(cellData -> {
        FileEntry entry = cellData.getValue();
        Date modifyTime = entry.getModifyTime();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
            modifyTime.toInstant(), ZoneId.systemDefault()
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new SimpleStringProperty(localDateTime.format(formatter));
    });
}
```

#### initDirectoryTree() 目录树初始化

```java
private void initDirectoryTree() {
    // 清空示例节点
    computerRootItem.getChildren().clear();

    // 设置自定义TreeCell以防止文本闪烁
    dirTreeView.setCellFactory(tv -> {
        TreeCell<String> cell = new TreeCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
        };
        cell.getStyleClass().add("tree-cell");
        return cell;
    });

    // 从fileSystem获取根目录
    if (fileSystem != null) {
        String root = "/";
        TreeItem<String> rootItem = new TreeItem<>(root);
        rootItem.setExpanded(true);
        computerRootItem.getChildren().add(rootItem);
        
        // 加载根目录的子目录
        loadSubDirectories(root, rootItem);
        
        // 添加展开事件监听，动态加载子目录
        rootItem.addEventHandler(TreeItem.<String>branchExpandedEvent(), event -> {
            TreeItem<String> expandedItem = event.getTreeItem();
            String path = getFullPath(expandedItem);
            if (expandedItem.getChildren().size() == 1 && 
                expandedItem.getChildren().get(0).getValue().isEmpty()) {
                loadSubDirectories(path, expandedItem);
            }
        });
    }

    dirTreeView.setRoot(computerRootItem);
    dirTreeView.setShowRoot(true);
}
```

### 事件绑定方法 / Event Binding Methods

#### bindEvents() 主事件绑定

```java
private void bindEvents() {
    // 1. 目录树点击事件：切换当前目录并加载文件列表
    dirTreeView.setOnMouseClicked(event -> {
        if (event.getClickCount() == 1) {
            TreeItem<String> selectedItem = dirTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem != computerRootItem) {
                String path = getFullPath(selectedItem);
                loadDirectory(path);
            }
        }
    });

    // 2. 新建文件菜单
    newFileItem.setOnAction(e -> showNewFileDialog());

    // 3. 新建文件夹菜单
    newDirItem.setOnAction(e -> showNewDirDialog());

    // 4. 查看菜单
    openItem.setOnAction(e -> {
        FileEntry selectedEntry = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedEntry != null) {
            if (selectedEntry.getType() == FileEntry.EntryType.FILE) {
                showViewFileContentDialog(selectedEntry);
            } else if (selectedEntry.getType() == FileEntry.EntryType.DIRECTORY) {
                String parentPath = currentDirectory.getDirEntry().getFullPath();
                String path = parentPath.endsWith("/") ? 
                    parentPath + selectedEntry.getName() : 
                    parentPath + "/" + selectedEntry.getName();
                loadDirectory(path);
                selectTreeItemByPath(path);
            }
        } else {
            showWarning("提示", "请先选择一个文件或文件夹");
        }
    });

    // 5. 删除菜单
    deleteItem.setOnAction(e -> deleteSelectedEntry());

    // 6. 刷新菜单
    refreshItem.setOnAction(e -> {
        String currentPath = (currentDirectory != null) ? 
            currentDirectory.getDirEntry().getFullPath() : "/";
        loadDirectory(currentPath);
        initDirectoryTree();
        selectTreeItemByPath(currentPath);
        refreshFatView();
    });
    
    // 7. 返回按钮
    backButton.setOnAction(e -> navigateBack());
    
    // 8. 视图切换按钮
    toggleViewButton.setOnAction(e -> toggleViewMode());
    
    // 9. 视图菜单项
    listViewItem.setOnAction(e -> switchToListView());
    iconViewItem.setOnAction(e -> switchToIconView());
    
    // 10. 磁盘查看器菜单项
    diskViewerItem.setOnAction(e -> openDiskViewer());
    
    // 11. 地址栏回车跳转路径
    if (pathTextField != null) {
        pathTextField.setOnAction(e -> handlePathEnter());
    }
    
    // 12. 设置右键菜单
    setupContextMenus();
}
```

### 核心业务方法 / Core Business Methods

#### loadDirectory() 目录加载

```java
public void loadDirectory(String path) {
    try {
        // 从文件系统获取目录对象
        currentDirectory = fileSystem.getDirectory(path);
        if (currentDirectory == null) {
            showError("错误", "目录不存在：" + path);
            return;
        }

        // 更新UI状态
        currentPathLabel.setText("当前路径：" + path);
        pathTextField.setText(path);
        
        // 更新返回按钮状态（根目录时禁用）
        backButton.setDisable(path.equals("/"));

        // 刷新目录缓存，确保获取到最新数据
        currentDirectory.refreshEntries();
        
        // 通过fileSystem.listDirectory重新加载，确保获取最新状态
        List<FileEntry> entries = new ArrayList<>(fileSystem.listDirectory(path));
        
        // 清空现有项并添加新项
        fileTableView.getItems().clear();
        fileTableView.getItems().addAll(entries);
        
        // 强制刷新TableView的UI显示
        Platform.runLater(() -> {
            fileTableView.refresh();
            fileTableView.requestFocus();
            
            // 更新文件计数
            long fileCount = entries.stream()
                .filter(entry -> entry.getType() == FileEntry.EntryType.FILE)
                .count();
            long dirCount = entries.stream()
                .filter(entry -> entry.getType() == FileEntry.EntryType.DIRECTORY)
                .count();
            
            fileCountLabel.setText(String.format("文件: %d, 文件夹: %d", fileCount, dirCount));
            
            // 刷新当前视图（如果是图标视图）
            refreshCurrentView();
        });
        
        LogUtil.info("已加载目录: " + path + ", 包含 " + entries.size() + " 个项目");
        
    } catch (FileSystemException e) {
        LogUtil.error("加载目录失败: " + e.getMessage(), e);
        showError("错误", "加载目录失败: " + e.getMessage());
    }
}
```

#### 文件操作方法 / File Operation Methods

```java
// 新建文件对话框
private void showNewFileDialog() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("新建文件");
    dialog.setHeaderText("请输入文件名：");
    dialog.setContentText("文件名:");

    dialog.showAndWait().ifPresent(fileName -> {
        if (!fileName.trim().isEmpty()) {
            try {
                String currentPath = currentDirectory.getDirEntry().getFullPath();
                String filePath = currentPath.endsWith("/") ? 
                    currentPath + fileName : currentPath + "/" + fileName;
                
                // 创建文件
                fileSystem.createFile(filePath, "");
                
                // 刷新当前目录
                loadDirectory(currentPath);
                
                LogUtil.info("文件创建成功: " + filePath);
            } catch (FileSystemException e) {
                LogUtil.error("创建文件失败: " + e.getMessage(), e);
                showError("错误", "创建文件失败: " + e.getMessage());
            }
        }
    });
}

// 新建目录对话框
private void showNewDirDialog() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("新建文件夹");
    dialog.setHeaderText("请输入文件夹名：");
    dialog.setContentText("文件夹名:");

    dialog.showAndWait().ifPresent(dirName -> {
        if (!dirName.trim().isEmpty()) {
            try {
                String currentPath = currentDirectory.getDirEntry().getFullPath();
                String dirPath = currentPath.endsWith("/") ? 
                    currentPath + dirName : currentPath + "/" + dirName;
                
                // 创建目录
                fileSystem.createDirectory(dirPath);
                
                // 刷新当前目录和目录树
                loadDirectory(currentPath);
                initDirectoryTree();
                selectTreeItemByPath(currentPath);
                
                LogUtil.info("目录创建成功: " + dirPath);
            } catch (FileSystemException e) {
                LogUtil.error("创建目录失败: " + e.getMessage(), e);
                showError("错误", "创建目录失败: " + e.getMessage());
            }
        }
    });
}

// 删除选中条目
private void deleteSelectedEntry() {
    FileEntry selectedEntry = fileTableView.getSelectionModel().getSelectedItem();
    if (selectedEntry == null) {
        showWarning("提示", "请先选择要删除的文件或文件夹");
        return;
    }

    // 确认删除
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("确认删除");
    confirmAlert.setHeaderText("您确定要删除以下项目吗？");
    confirmAlert.setContentText(selectedEntry.getName() + 
        " (" + (selectedEntry.getType() == FileEntry.EntryType.FILE ? "文件" : "文件夹") + ")");

    confirmAlert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK) {
            deleteFileEntry(selectedEntry);
        }
    });
}
```

### 视图切换功能 / View Switching Functionality

#### 视图模式枚举 / View Mode Enumeration

```java
public enum ViewMode {
    LIST,   // 列表模式（表格）
    ICON    // 图标模式
}
```

#### 视图切换方法 / View Switching Methods

```java
// 切换视图模式
private void toggleViewMode() {
    if (currentViewMode == ViewMode.LIST) {
        switchToIconView();
    } else {
        switchToListView();
    }
}

// 切换到列表视图
private void switchToListView() {
    currentViewMode = ViewMode.LIST;
    fileTableView.setVisible(true);
    iconViewScrollPane.setVisible(false);
    toggleViewButton.setText("图标视图");
    LogUtil.info("已切换到列表视图");
}

// 切换到图标视图
private void switchToIconView() {
    currentViewMode = ViewMode.ICON;
    fileTableView.setVisible(false);
    iconViewScrollPane.setVisible(true);
    toggleViewButton.setText("列表视图");
    refreshIconView();
    LogUtil.info("已切换到图标视图");
}

// 刷新图标视图
private void refreshIconView() {
    if (iconViewPane == null) {
        return;
    }
    
    iconViewPane.getChildren().clear();
    
    if (currentDirectory == null) {
        return;
    }
    
    try {
        List<FileEntry> entries = fileSystem.listDirectory(
            currentDirectory.getDirEntry().getFullPath()
        );
        
        for (FileEntry entry : entries) {
            if (!entry.isDeleted()) {
                VBox iconItem = createIconItem(entry);
                iconViewPane.getChildren().add(iconItem);
            }
        }
    } catch (FileSystemException e) {
        LogUtil.error("刷新图标视图失败: " + e.getMessage(), e);
    }
}

// 创建图标项
private VBox createIconItem(FileEntry entry) {
    VBox iconItem = new VBox(5);
    iconItem.setAlignment(javafx.geometry.Pos.CENTER);
    iconItem.setPrefWidth(100);
    iconItem.setPrefHeight(120);
    iconItem.getStyleClass().add("icon-item");
    
    // 创建图标
    ImageView iconView = null;
    try {
        String iconPath = (entry.getType() == FileEntry.EntryType.DIRECTORY) ? 
            "/org/jiejiejiang/filemanager/images/folder.png" : 
            "/org/jiejiejiang/filemanager/images/file.png";
        
        Image icon = new Image(getClass().getResourceAsStream(iconPath));
        iconView = new ImageView(icon);
        iconView.setFitWidth(64);
        iconView.setFitHeight(64);
        iconView.setPreserveRatio(true);
    } catch (Exception e) {
        iconView = null;
    }
    
    // 创建文件名标签
    Label nameLabel = new Label(entry.getName());
    nameLabel.setWrapText(true);
    nameLabel.setMaxWidth(90);
    nameLabel.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
    
    // 添加组件到容器
    if (iconView != null) {
        iconItem.getChildren().addAll(iconView, nameLabel);
    } else {
        // 如果图标加载失败，显示文件类型标识
        Label typeLabel = new Label(
            entry.getType() == FileEntry.EntryType.DIRECTORY ? "📁" : "📄"
        );
        typeLabel.setStyle("-fx-font-size: 48px;");
        iconItem.getChildren().addAll(typeLabel, nameLabel);
    }
    
    // 添加点击事件
    iconItem.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2) {
            // 双击事件
            if (entry.getType() == FileEntry.EntryType.DIRECTORY) {
                // 进入目录
                String parentPath = currentDirectory.getDirEntry().getFullPath();
                String fullPath = parentPath.endsWith("/") ? 
                    parentPath + entry.getName() : parentPath + "/" + entry.getName();
                loadDirectory(fullPath);
                selectTreeItemByPath(fullPath);
            } else {
                // 查看文件内容
                showViewFileContentDialog(entry);
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            // 右键菜单
            showIconContextMenu(entry, event.getScreenX(), event.getScreenY());
        }
    });
    
    return iconItem;
}
```

### FAT监控功能 / FAT Monitoring Functionality

#### FAT表格初始化 / FAT Table Initialization

```java
private void initFatTableColumns() {
    fatBlockIdColumn.setCellValueFactory(new PropertyValueFactory<>("blockId"));
    fatValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
    fatStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    
    fatTableView.setItems(FXCollections.observableArrayList());
}

// FAT行数据类
public static class FatRow {
    private final int blockId;
    private final int value;
    
    public FatRow(int blockId, int value) {
        this.blockId = blockId;
        this.value = value;
    }
    
    public int getBlockId() { return blockId; }
    public int getValue() { return value; }
    
    public String getStatus() {
        if (value == FAT.FREE_BLOCK) return "空闲";
        if (value == FAT.BAD_BLOCK) return "坏块";
        if (value == FAT.END_OF_FILE) return "文件末尾";
        return "已使用";
    }
}
```

#### FAT视图刷新 / FAT View Refresh

```java
private void refreshFatView() {
    if (fileSystem == null) return;
    
    try {
        FAT fat = fileSystem.getFat();
        if (fat == null) return;
        
        // 清空现有数据
        fatTableView.getItems().clear();
        
        // 统计计数器
        int freeCount = 0, usedCount = 0, badCount = 0;
        
        // 获取FAT表数据
        byte[] fatTable = fat.getFatTable();
        
        // 遍历FAT表并创建行数据
        for (int i = 0; i < fat.getTotalBlocks(); i++) {
            byte fatValue = fatTable[i];
            FatRow row = new FatRow(i, fatValue);
            fatTableView.getItems().add(row);
            
            // 统计各种状态的块数
            if (fatValue == FAT.FREE_BLOCK) {
                freeCount++;
            } else if (fatValue == FAT.BAD_BLOCK) {
                badCount++;
            } else {
                usedCount++;
            }
        }
        
        // 更新统计标签
        fatFreeCountLabel.setText("空闲: " + freeCount);
        fatUsedCountLabel.setText("已使用: " + usedCount);
        fatBadCountLabel.setText("坏块: " + badCount);
        
        LogUtil.info("FAT视图已刷新，总块数: " + fat.getTotalBlocks());
        
    } catch (Exception e) {
        LogUtil.error("刷新FAT视图失败: " + e.getMessage(), e);
    }
}
```

### 辅助方法 / Helper Methods

#### 路径处理方法 / Path Processing Methods

```java
// 获取TreeItem的完整路径
private String getFullPath(TreeItem<String> item) {
    if (item == null || item == computerRootItem) {
        return "/";
    }
    
    List<String> pathParts = new ArrayList<>();
    TreeItem<String> current = item;
    
    while (current != null && current != computerRootItem) {
        pathParts.add(0, current.getValue());
        current = current.getParent();
    }
    
    if (pathParts.isEmpty()) {
        return "/";
    }
    
    // 如果第一个部分是根目录"/"，直接返回
    if (pathParts.get(0).equals("/")) {
        if (pathParts.size() == 1) {
            return "/";
        } else {
            return "/" + String.join("/", pathParts.subList(1, pathParts.size()));
        }
    }
    
    return "/" + String.join("/", pathParts);
}

// 路径规范化
private String normalizePath(String path) {
    if (path == null || path.trim().isEmpty()) {
        return "/";
    }
    
    path = path.trim().replace("\\", "/");
    if (!path.startsWith("/")) {
        path = "/" + path;
    }
    
    return path;
}

// 根据路径选择TreeItem
private void selectTreeItemByPath(String path) {
    path = normalizePath(path);
    
    if (path.equals("/")) {
        // 选择根目录
        TreeItem<String> rootItem = computerRootItem.getChildren().isEmpty() ? 
            null : computerRootItem.getChildren().get(0);
        if (rootItem != null) {
            dirTreeView.getSelectionModel().select(rootItem);
        }
        return;
    }
    
    // 分割路径并逐级查找
    String[] pathParts = path.substring(1).split("/");
    TreeItem<String> currentItem = computerRootItem.getChildren().isEmpty() ? 
        null : computerRootItem.getChildren().get(0);
    
    for (String part : pathParts) {
        if (currentItem == null) break;
        
        // 确保当前节点已展开
        if (!currentItem.isExpanded()) {
            currentItem.setExpanded(true);
            loadSubDirectories(getFullPath(currentItem), currentItem);
        }
        
        // 查找匹配的子节点
        TreeItem<String> foundChild = null;
        for (TreeItem<String> child : currentItem.getChildren()) {
            if (part.equals(child.getValue())) {
                foundChild = child;
                break;
            }
        }
        
        currentItem = foundChild;
    }
    
    // 选择找到的节点
    if (currentItem != null) {
        dirTreeView.getSelectionModel().select(currentItem);
        
        // 确保选中的项可见
        int selectedIndex = dirTreeView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            dirTreeView.scrollTo(selectedIndex);
        }
    }
}
```

#### UI辅助方法 / UI Helper Methods

```java
// 显示错误对话框
private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}

// 显示警告对话框
private void showWarning(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}

// 显示信息对话框
private void showInfo(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}
```

### 依赖注入方法 / Dependency Injection Methods

```java
// 设置文件系统（外部注入）
public void setFileSystem(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    
    // 初始化完成后加载根目录
    Platform.runLater(() -> {
        initDirectoryTree();
        loadDirectory("/");
        refreshFatView();
    });
}
```

## DiskViewerController 磁盘查看器控制器 / Disk Viewer Controller

### 类声明 / Class Declaration

```java
package org.jiejiejiang.filemanager.gui.controller;

/**
 * 磁盘块查看器控制器
 * 用于展示磁盘的每个块和每个字节的内容，以及图形化展示磁盘使用率
 */
public class DiskViewerController {
    // 磁盘查看器控制器实现
}
```

### 核心特性 / Core Features

#### 1. 磁盘使用率可视化 / Disk Usage Visualization
- **进度条显示**：直观展示磁盘使用率
- **统计信息**：显示总块数、已使用、空闲和坏块数量
- **实时更新**：支持数据刷新和实时监控

#### 2. 块级别分析 / Block-Level Analysis
- **块状态监控**：显示每个块的状态（空闲、已使用、坏块等）
- **内容预览**：提供块内容的简要预览
- **详细查看**：支持查看块的完整内容

#### 3. 十六进制查看器 / Hexadecimal Viewer
- **十六进制显示**：以十六进制格式显示块内容
- **ASCII对照**：同时提供ASCII字符对照
- **格式化输出**：规范的十六进制编辑器格式

### FXML组件注入 / FXML Component Injection

```java
public class DiskViewerController {
    // 磁盘使用率组件
    @FXML private ProgressBar diskUsageProgressBar;
    @FXML private Label diskUsageLabel;
    
    // 块信息表格组件
    @FXML private TableView<BlockInfo> blockTableView;
    @FXML private TableColumn<BlockInfo, Integer> blockIdColumn;
    @FXML private TableColumn<BlockInfo, String> blockStatusColumn;
    @FXML private TableColumn<BlockInfo, Integer> blockSizeColumn;
    @FXML private TableColumn<BlockInfo, String> blockContentColumn;
    
    // 详细信息显示组件
    @FXML private Label selectedBlockLabel;
    @FXML private TextArea hexTextArea;
    @FXML private TextArea asciiTextArea;
    
    // 统计信息组件
    @FXML private Label totalBlocksLabel;
    @FXML private Label usedBlocksLabel;
    @FXML private Label freeBlocksLabel;
    @FXML private Label badBlocksLabel;
    
    // 控制按钮
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
}
```

### 数据模型 / Data Models

#### BlockInfo 块信息类 / Block Information Class

```java
public static class BlockInfo {
    private final int blockId;
    private final String status;
    private final int size;
    private final String contentPreview;
    
    public BlockInfo(int blockId, String status, int size, String contentPreview) {
        this.blockId = blockId;
        this.status = status;
        this.size = size;
        this.contentPreview = contentPreview;
    }
    
    // Getter方法
    public int getBlockId() { return blockId; }
    public String getStatus() { return status; }
    public int getSize() { return size; }
    public String getContentPreview() { return contentPreview; }
}
```

### 初始化方法 / Initialization Methods

```java
@FXML
public void initialize() {
    initTableColumns();
    bindEvents();
}

// 初始化表格列
private void initTableColumns() {
    blockIdColumn.setCellValueFactory(new PropertyValueFactory<>("blockId"));
    blockStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    blockSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
    blockContentColumn.setCellValueFactory(new PropertyValueFactory<>("contentPreview"));
    
    blockTableView.setItems(blockData);
}

// 绑定事件
private void bindEvents() {
    // 表格选择事件
    blockTableView.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showBlockDetails(newSelection);
            }
        }
    );
    
    // 刷新按钮
    refreshButton.setOnAction(e -> refreshData());
    
    // 关闭按钮
    closeButton.setOnAction(e -> {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    });
}
```

### 核心功能方法 / Core Functionality Methods

#### 数据刷新方法 / Data Refresh Methods

```java
private void refreshData() {
    if (fileSystem == null) {
        LogUtil.warn("文件系统未初始化，无法刷新磁盘数据");
        return;
    }
    
    try {
        // 获取FAT表
        FAT fat = fileSystem.getFat();
        if (fat == null) {
            LogUtil.warn("FAT表未初始化");
            return;
        }
        
        // 清空现有数据
        blockData.clear();
        
        // 统计信息
        int totalBlocks = fat.getTotalBlocks();
        int usedBlocks = 0;
        int freeBlocks = 0;
        int badBlocks = 0;
        
        // 获取FAT表数据
        byte[] fatTable = fat.getFatTable();
        
        // 遍历所有块
        for (int i = 0; i < totalBlocks; i++) {
            byte fatValue = fatTable[i];
            String status;
            
            if (fatValue == FAT.FREE_BLOCK) {
                status = "空闲";
                freeBlocks++;
            } else if (fatValue == FAT.BAD_BLOCK) {
                status = "坏块";
                badBlocks++;
            } else if (fatValue == FAT.END_OF_FILE) {
                status = "文件末尾";
                usedBlocks++;
            } else {
                status = "已使用";
                usedBlocks++;
            }
            
            // 读取块内容预览
            String contentPreview = getBlockContentPreview(i);
            
            BlockInfo blockInfo = new BlockInfo(i, status, fileSystem.getBlockSize(), contentPreview);
            blockData.add(blockInfo);
        }
        
        // 更新统计标签
        totalBlocksLabel.setText(String.valueOf(totalBlocks));
        usedBlocksLabel.setText(String.valueOf(usedBlocks));
        freeBlocksLabel.setText(String.valueOf(freeBlocks));
        badBlocksLabel.setText(String.valueOf(badBlocks));
        
        // 更新使用率
        double usageRate = totalBlocks > 0 ? (double) usedBlocks / totalBlocks : 0;
        diskUsageProgressBar.setProgress(usageRate);
        diskUsageLabel.setText(String.format("%.1f%%", usageRate * 100));
        
        LogUtil.info("磁盘数据刷新完成，总块数: " + totalBlocks + ", 已使用: " + usedBlocks);
        
    } catch (Exception e) {
        LogUtil.error("刷新磁盘数据时发生错误: " + e.getMessage(), e);
    }
}
```

#### 内容预览方法 / Content Preview Methods

```java
// 获取块内容预览
private String getBlockContentPreview(int blockId) {
    try {
        byte[] blockData = fileSystem.getDisk().readBlock(blockId);
        if (blockData == null || blockData.length == 0) {
            return "空";
        }
        
        // 取前16个字节作为预览
        StringBuilder preview = new StringBuilder();
        int previewLength = Math.min(16, blockData.length);
        
        for (int i = 0; i < previewLength; i++) {
            byte b = blockData[i];
            if (b >= 32 && b <= 126) { // 可打印ASCII字符
                preview.append((char) b);
            } else {
                preview.append('.');
            }
        }
        
        if (blockData.length > 16) {
            preview.append("...");
        }
        
        return preview.toString();
        
    } catch (Exception e) {
        LogUtil.error("读取块 " + blockId + " 内容时发生错误: " + e.getMessage(), e);
        return "错误";
    }
}
```

#### 详细信息显示方法 / Detailed Information Display Methods

```java
// 显示块详细信息
private void showBlockDetails(BlockInfo blockInfo) {
    selectedBlockLabel.setText("块 " + blockInfo.getBlockId());
    
    try {
        byte[] blockData = fileSystem.getDisk().readBlock(blockInfo.getBlockId());
        if (blockData == null || blockData.length == 0) {
            hexTextArea.setText("块为空");
            asciiTextArea.setText("块为空");
            return;
        }
        
        // 生成十六进制视图
        StringBuilder hexView = new StringBuilder();
        StringBuilder asciiView = new StringBuilder();
        
        for (int i = 0; i < blockData.length; i++) {
            if (i % 16 == 0) {
                if (i > 0) {
                    hexView.append("\n");
                    asciiView.append("\n");
                }
                hexView.append(String.format("%04X: ", i));
            }
            
            byte b = blockData[i];
            hexView.append(String.format("%02X ", b & 0xFF));
            
            // ASCII视图
            if (b >= 32 && b <= 126) {
                asciiView.append((char) b);
            } else {
                asciiView.append('.');
            }
            
            if (i % 16 == 15) {
                asciiView.append(" ");
            }
        }
        
        hexTextArea.setText(hexView.toString());
        asciiTextArea.setText(asciiView.toString());
        
    } catch (Exception e) {
        LogUtil.error("显示块详细信息时发生错误: " + e.getMessage(), e);
        hexTextArea.setText("读取错误: " + e.getMessage());
        asciiTextArea.setText("读取错误: " + e.getMessage());
    }
}
```

### 依赖注入方法 / Dependency Injection Methods

```java
// 设置文件系统
public void setFileSystem(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    refreshData();
}
```

## 异常处理 / Exception Handling

### 异常处理策略 / Exception Handling Strategy

```java
// 统一异常处理模式
try {
    // 业务逻辑操作
    performFileSystemOperation();
} catch (FileSystemException e) {
    // 记录错误日志
    LogUtil.error("文件系统操作失败: " + e.getMessage(), e);
    
    // 显示用户友好的错误信息
    showError("操作失败", "文件系统操作失败: " + e.getMessage());
    
    // 可选：回滚操作或恢复状态
    rollbackOperation();
} catch (Exception e) {
    // 处理未预期的异常
    LogUtil.error("未预期的错误: " + e.getMessage(), e);
    showError("系统错误", "发生未预期的错误，请重试");
}
```

### 常见异常场景 / Common Exception Scenarios

1. **文件系统未初始化**：在操作前检查fileSystem是否为null
2. **路径不存在**：处理目录或文件不存在的情况
3. **权限不足**：处理只读文件或目录的操作限制
4. **磁盘空间不足**：处理磁盘满的情况
5. **并发访问冲突**：处理多线程访问的同步问题

## 线程安全 / Thread Safety

### UI线程管理 / UI Thread Management

```java
// 在后台线程执行耗时操作，在UI线程更新界面
CompletableFuture.supplyAsync(() -> {
    // 后台线程执行文件系统操作
    try {
        return fileSystem.listDirectory(path);
    } catch (FileSystemException e) {
        throw new RuntimeException(e);
    }
}).thenAcceptAsync(entries -> {
    // UI线程更新界面
    Platform.runLater(() -> {
        fileTableView.getItems().clear();
        fileTableView.getItems().addAll(entries);
        fileTableView.refresh();
    });
}, Platform::runLater).exceptionally(throwable -> {
    // 异常处理
    Platform.runLater(() -> {
        showError("错误", "加载目录失败: " + throwable.getMessage());
    });
    return null;
});
```

### 数据同步策略 / Data Synchronization Strategy

```java
// 使用Platform.runLater确保UI更新在JavaFX应用线程中执行
private void updateUIOnFXThread(Runnable updateAction) {
    if (Platform.isFxApplicationThread()) {
        updateAction.run();
    } else {
        Platform.runLater(updateAction);
    }
}

// 示例使用
updateUIOnFXThread(() -> {
    fileTableView.refresh();
    refreshFatView();
});
```

## 性能优化 / Performance Optimization

### 懒加载策略 / Lazy Loading Strategy

```java
// 目录树懒加载实现
private void loadSubDirectories(String path, TreeItem<String> parentItem) {
    // 检查是否已经加载过（避免重复加载）
    boolean hasRealChildren = parentItem.getChildren().stream()
        .anyMatch(child -> !child.getValue().isEmpty());
    
    if (hasRealChildren) {
        return; // 已经加载过，直接返回
    }
    
    // 异步加载子目录
    CompletableFuture.supplyAsync(() -> {
        try {
            return fileSystem.listDirectory(path);
        } catch (FileSystemException e) {
            LogUtil.error("加载子目录失败：" + e.getMessage());
            return new ArrayList<FileEntry>();
        }
    }).thenAcceptAsync(entries -> {
        Platform.runLater(() -> {
            parentItem.getChildren().clear();
            
            for (FileEntry entry : entries) {
                if (entry.getType() == FileEntry.EntryType.DIRECTORY && !entry.isDeleted()) {
                    TreeItem<String> dirItem = new TreeItem<>(entry.getName());
                    parentItem.getChildren().add(dirItem);
                    
                    // 添加临时子节点以显示展开图标
                    dirItem.getChildren().add(new TreeItem<>(""));
                }
            }
        });
    });
}
```

### 缓存策略 / Caching Strategy

```java
// 图标缓存
private final Map<String, Image> iconCache = new ConcurrentHashMap<>();

private Image getIcon(FileEntry entry) {
    String iconKey = entry.getType() == FileEntry.EntryType.DIRECTORY ? "folder" : "file";
    
    return iconCache.computeIfAbsent(iconKey, key -> {
        String iconPath = key.equals("folder") ? 
            "/org/jiejiejiang/filemanager/images/folder.png" : 
            "/org/jiejiejiang/filemanager/images/file.png";
        
        try {
            return new Image(getClass().getResourceAsStream(iconPath));
        } catch (Exception e) {
            LogUtil.error("加载图标失败: " + iconPath, e);
            return null;
        }
    });
}
```

### 批量操作优化 / Batch Operation Optimization

```java
// 批量刷新UI
private void batchUpdateUI(List<FileEntry> entries) {
    // 暂停UI更新
    fileTableView.setDisable(true);
    
    try {
        // 批量更新数据
        ObservableList<FileEntry> items = fileTableView.getItems();
        items.clear();
        items.addAll(entries);
        
        // 强制刷新
        fileTableView.refresh();
        
    } finally {
        // 恢复UI更新
        fileTableView.setDisable(false);
    }
}
```

## 使用示例 / Usage Examples

### 基本使用 / Basic Usage

```java
// 创建主控制器并注入文件系统
MainController mainController = new MainController();
FileSystem fileSystem = new FileSystem();
mainController.setFileSystem(fileSystem);

// 加载指定目录
mainController.loadDirectory("/home/user/documents");

// 切换视图模式
mainController.switchToIconView();
```

### 磁盘查看器使用 / Disk Viewer Usage

```java
// 创建磁盘查看器控制器
DiskViewerController diskViewer = new DiskViewerController();
diskViewer.setFileSystem(fileSystem);

// 刷新磁盘数据
diskViewer.refreshData();
```

### 事件处理示例 / Event Handling Examples

```java
// 自定义文件双击处理
fileTableView.setRowFactory(tv -> {
    TableRow<FileEntry> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && !row.isEmpty()) {
            FileEntry entry = row.getItem();
            if (entry.getType() == FileEntry.EntryType.DIRECTORY) {
                // 进入目录
                String fullPath = getCurrentPath() + "/" + entry.getName();
                loadDirectory(fullPath);
            } else {
                // 打开文件
                showViewFileContentDialog(entry);
            }
        }
    });
    return row;
});
```

## 扩展建议 / Extension Recommendations

### 功能扩展 / Feature Extensions

1. **多标签页支持**：实现多个目录的同时浏览
2. **搜索功能**：添加文件和目录的搜索功能
3. **书签管理**：支持常用目录的书签功能
4. **文件预览**：支持图片、文本等文件的预览
5. **拖拽操作**：支持文件的拖拽移动和复制

### 性能优化 / Performance Optimizations

1. **虚拟化列表**：对大量文件的列表进行虚拟化
2. **异步加载**：所有文件系统操作异步化
3. **智能缓存**：实现更智能的缓存策略
4. **增量更新**：支持目录内容的增量更新

### 用户体验改进 / User Experience Improvements

1. **主题支持**：支持多种UI主题
2. **快捷键**：添加常用操作的快捷键
3. **状态栏**：显示更多状态信息
4. **进度指示**：长时间操作的进度显示
5. **撤销重做**：支持操作的撤销和重做

## 依赖关系 / Dependencies

### 外部依赖 / External Dependencies

- `javafx.fxml.*`: JavaFX FXML支持
- `javafx.scene.control.*`: JavaFX控件库
- `javafx.application.Platform`: JavaFX平台工具
- `java.util.concurrent.*`: 并发工具类
- `java.time.*`: 时间处理工具

### 内部依赖 / Internal Dependencies

- `org.jiejiejiang.filemanager.core.*`: 核心业务逻辑
- `org.jiejiejiang.filemanager.exception.*`: 异常定义
- `org.jiejiejiang.filemanager.util.*`: 工具类

### 被依赖关系 / Dependent Classes

- `FileManagerApp`: 应用程序入口类
- `*.fxml`: FXML界面定义文件
- 其他对话框控制器类

## 测试建议 / Testing Recommendations

### 单元测试覆盖 / Unit Test Coverage

1. **控制器初始化测试**：验证FXML注入和初始化的正确性
2. **事件处理测试**：验证各种用户交互的正确处理
3. **数据绑定测试**：验证UI组件与数据模型的绑定
4. **异常处理测试**：验证各种异常情况的处理
5. **视图切换测试**：验证不同视图模式的切换

### 集成测试建议 / Integration Test Recommendations

1. **文件系统集成测试**：验证与FileSystem的集成
2. **UI自动化测试**：使用TestFX进行UI自动化测试
3. **性能测试**：测试大量文件时的性能表现
4. **用户体验测试**：验证用户操作流程的完整性

### 测试工具推荐 / Recommended Testing Tools

1. **JUnit 5**：单元测试框架
2. **TestFX**：JavaFX应用程序测试框架
3. **Mockito**：模拟对象框架
4. **AssertJ**：流畅的断言库