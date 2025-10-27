package org.jiejiejiang.filemanager.gui.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jiejiejiang.filemanager.core.Directory;
import org.jiejiejiang.filemanager.core.FAT;
import org.jiejiejiang.filemanager.core.FileEntry;
import org.jiejiejiang.filemanager.core.FileSystem;
import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.util.LogUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

public class MainController {

    // ============================== FXML组件注入 ==============================
    @FXML private TreeView<String> dirTreeView;
    @FXML private TreeItem<String> computerRootItem;
    @FXML private TableView<FileEntry> fileTableView;
    @FXML private TableColumn<FileEntry, String> nameColumn;
    @FXML private TableColumn<FileEntry, String> typeColumn;
    @FXML private TableColumn<FileEntry, Long> sizeColumn;
    @FXML private TableColumn<FileEntry, String> modifyTimeColumn;
    @FXML private Label currentPathLabel;
    @FXML private Label fileCountLabel;
    @FXML private Button backButton;
    @FXML private TextField pathTextField;

    // FAT监视器组件
    @FXML private TableView<FatRow> fatTableView;
    @FXML private TableColumn<FatRow, Integer> fatBlockIdColumn;
    @FXML private TableColumn<FatRow, String> fatValueColumn;
    @FXML private TableColumn<FatRow, String> fatStatusColumn;
    @FXML private Label fatFreeCountLabel;
    @FXML private Label fatUsedCountLabel;
    @FXML private Label fatBadCountLabel;

    // 菜单组件
    @FXML private MenuItem newFileItem;
    @FXML private MenuItem newDirItem;
    @FXML private MenuItem deleteItem;
    @FXML private MenuItem refreshItem;
    @FXML private MenuItem listViewItem;
    @FXML private MenuItem iconViewItem;
    
    // 视图切换组件
    @FXML private Button toggleViewButton;
    @FXML private javafx.scene.control.ScrollPane iconViewScrollPane;
    @FXML private javafx.scene.layout.FlowPane iconViewPane;

    // ============================== 业务对象 ==============================
    private FileSystem fileSystem; // 文件系统核心对象（由外部注入）
    private Directory currentDirectory; // 当前选中的目录

    // ============================== 点击行为状态 ==============================
    private long lastClickTime = 0L;
    private int lastClickedRowIndex = -1;
    private static final int DOUBLE_CLICK_THRESHOLD_MS = 350;
    
    // ============================== 视图模式状态 ==============================
    public enum ViewMode {
        LIST,   // 列表模式（表格）
        ICON    // 图标模式
    }
    
    private ViewMode currentViewMode = ViewMode.LIST; // 默认列表模式

    // ============================== 初始化 ==============================
    @FXML
    public void initialize() {
        // 1. 初始化表格列与FileEntry属性绑定
        initTableColumns();
        // 初始化 FAT 监视器表格列
        initFatTableColumns();
        
        // 2. 初始化目录树（模拟加载磁盘，实际应从fileSystem获取）
        initDirectoryTree();
        
        // 3. 绑定事件监听器
        bindEvents();
    }

    /**
     * 初始化表格列，关联FileEntry的属性
     */
    private void initTableColumns() {
        // 移除重复的设置
        nameColumn.setCellValueFactory(new PropertyValueFactory<> ("name"));
        
        // 类型列：转换为"文件"或"文件夹"显示
        typeColumn.setCellValueFactory(cellData -> {
            FileEntry entry = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    entry.getType() == FileEntry.EntryType.FILE ? "文件" : "文件夹"
            );
        });
    
        // 大小列：文件显示大小，文件夹显示"-"
        sizeColumn.setCellValueFactory(cellData -> {
            FileEntry entry = cellData.getValue();
            long size = (entry.getType() == FileEntry.EntryType.FILE) ? entry.getSize() : -1;
            return new javafx.beans.property.SimpleLongProperty(size).asObject();
        });
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
            LocalDateTime localDateTime = LocalDateTime.ofInstant(modifyTime.toInstant(), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return new javafx.beans.property.SimpleStringProperty(localDateTime.format(formatter));
        });
    }

    /**
     * 初始化目录树（模拟磁盘加载）
     */
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
            // 添加CSS样式类
            cell.getStyleClass().add("tree-cell");
            return cell;
        });

        // 实际应从fileSystem获取所有磁盘/根目录
        if (fileSystem != null) {
            String root = "/";
            TreeItem<String> rootItem = new TreeItem<>(root);
            rootItem.setExpanded(true); // 默认展开根目录
            computerRootItem.getChildren().add(rootItem);
            
            // 加载根目录的子目录
            loadSubDirectories(root, rootItem);
            
            // 添加展开事件监听，动态加载子目录
            rootItem.addEventHandler(TreeItem.<String>branchExpandedEvent(), event -> {
                TreeItem<String> expandedItem = event.getTreeItem();
                String path = getFullPath(expandedItem);
                // 避免重复加载
                if (expandedItem.getChildren().size() == 1 && 
                    expandedItem.getChildren().get(0).getValue().isEmpty()) {
                    loadSubDirectories(path, expandedItem);
                }
            });
        } else {
            // 模拟数据（开发阶段用）
            TreeItem<String> cDrive = new TreeItem<>("C:");
            TreeItem<String> dDrive = new TreeItem<>("D:");
            cDrive.getChildren().add(new TreeItem<>("Users"));
            cDrive.getChildren().add(new TreeItem<>("Program Files"));
            dDrive.getChildren().add(new TreeItem<>("Documents"));
            computerRootItem.getChildren().addAll(cDrive, dDrive);
        }

        dirTreeView.setRoot(computerRootItem);
        dirTreeView.setShowRoot(true);
    }
    
    /**
     * 加载指定目录的子目录到目录树中
     * @param path 目录路径
     * @param parentItem 父节点
     */
    private void loadSubDirectories(String path, TreeItem<String> parentItem) {
        // 检查是否已经加载过（避免重复加载）
        boolean hasRealChildren = parentItem.getChildren().stream()
            .anyMatch(child -> !child.getValue().isEmpty());
        
        if (hasRealChildren) {
            return; // 已经加载过，直接返回
        }
        
        // 清空临时子节点
        parentItem.getChildren().clear();
        
        try {
            // 列出目录下的所有条目
            List<FileEntry> entries = fileSystem.listDirectory(path);
            
            // 筛选出目录并添加到父节点
            for (FileEntry entry : entries) {
                if (entry.getType() == FileEntry.EntryType.DIRECTORY && !entry.isDeleted()) {
                    TreeItem<String> dirItem = new TreeItem<>(entry.getName());
                    parentItem.getChildren().add(dirItem);
                    
                    // 为每个目录添加一个临时子节点，以显示展开图标
                    dirItem.getChildren().add(new TreeItem<>(""));
                    
                    // 添加展开事件监听（只添加一次）
                    dirItem.addEventHandler(TreeItem.<String>branchExpandedEvent(), event -> {
                        TreeItem<String> expandedItem = event.getTreeItem();
                        String dirPath = getFullPath(expandedItem);
                        loadSubDirectories(dirPath, expandedItem);
                    });
                }
            }
        } catch (FileSystemException e) {
            LogUtil.error("加载子目录失败：" + e.getMessage());
        }
    }

    /**
     * 绑定UI事件监听器
     */
    private void bindEvents() {
        // 1. 目录树点击事件：切换当前目录并加载文件列表
        dirTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // 单击即可切换
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

        // 4. 删除菜单
        deleteItem.setOnAction(e -> deleteSelectedEntry());

        // 5. 刷新菜单
        refreshItem.setOnAction(e -> {
            String currentPath = (currentDirectory != null) ? currentDirectory.getDirEntry().getFullPath() : "/";
            loadDirectory(currentPath);
            initDirectoryTree();
            selectTreeItemByPath(currentPath);
            // 刷新FAT视图
            refreshFatView();
        });
        
        // 6. 返回按钮
        backButton.setOnAction(e -> navigateBack());
        
        // 7. 视图切换按钮
        toggleViewButton.setOnAction(e -> toggleViewMode());
        
        // 8. 视图菜单项
        listViewItem.setOnAction(e -> switchToListView());
        iconViewItem.setOnAction(e -> switchToIconView());
        
        // 地址栏回车跳转路径
        if (pathTextField != null) {
            pathTextField.setOnAction(e -> handlePathEnter());
        }
        
        // 9. 设置右键菜单
        setupContextMenus();
        
        // 10. 文件表格点击事件统一由 setupContextMenus() 中注册的处理器负责
        // （包含自定义双击阈值与取消选中逻辑，避免事件处理器覆盖问题）
    }
    
    /**
     * 显示文本编辑器对话框，允许修改文件内容（大小由内容长度决定）
     */
    private void showEditFileContentDialog(FileEntry fileEntry) {
        // 仅允许编辑文件
        if (fileEntry.getType() != FileEntry.EntryType.FILE) {
            showWarning("提示", "只能编辑文件内容");
            return;
        }
    
        // 只读文件禁止打开与保存
        if (fileEntry.isReadOnly()) {
            showWarning("提示", "该文件为只读，不能打开或保存内容");
            return;
        }
    
        // 计算完整路径
        String parentPath = currentDirectory.getDirEntry().getFullPath();
        String fullPath = parentPath.endsWith("/") ? parentPath + fileEntry.getName() : parentPath + "/" + fileEntry.getName();
    
        // 创建对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("编辑文件内容");
        dialog.setHeaderText("文件：" + fileEntry.getName());
    
        // 仅需要保存功能（加上取消以便关闭）
        ButtonType SAVE_BUTTON = new ButtonType("保存", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(SAVE_BUTTON, ButtonType.CANCEL);
    
        // 文本编辑器
        javafx.scene.control.TextArea editor = new javafx.scene.control.TextArea();
        editor.setWrapText(true);
        editor.setPrefRowCount(20);
        editor.setPrefColumnCount(60);
    
        // 读取现有内容
        try {
            byte[] content = fileSystem.readFile(fullPath);
            String text = new String(content, java.nio.charset.StandardCharsets.UTF_8);
            editor.setText(text);
        } catch (FileSystemException e) {
            showError("读取文件失败", e.getMessage());
            editor.setText("");
        }
    
        dialog.getDialogPane().setContent(editor);
    
        // 绑定 Ctrl+S 触发保存按钮
        dialog.setOnShown(ev -> {
            var scene = dialog.getDialogPane().getScene();
            if (scene != null) {
                var saveNode = dialog.getDialogPane().lookupButton(SAVE_BUTTON);
                scene.getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.S,
                        javafx.scene.input.KeyCombination.CONTROL_DOWN
                    ),
                    () -> { if (saveNode != null && !saveNode.isDisabled()) ((javafx.scene.control.Button) saveNode).fire(); }
                );
            }
        });
    
        // 显示并处理保存
        dialog.showAndWait().ifPresent(result -> {
            if (result == SAVE_BUTTON) {
                try {
                    // 保存前存在性检测
                    FileEntry current = fileSystem.getEntry(fullPath);
                    if (current == null || current.isDeleted() || current.getType() != FileEntry.EntryType.FILE) {
                        showError("保存失败", "文件不存在或类型错误：" + fullPath);
                        return;
                    }
    
                    // 写入内容（大小由文本长度决定）
                    byte[] newContent = editor.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    fileSystem.writeFile(fullPath, newContent);
    
                    // 刷新 UI 与 FAT
                    Platform.runLater(() -> {
                        loadDirectory(currentDirectory.getDirEntry().getFullPath());
                        refreshFatView();
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("成功");
                        success.setHeaderText(null);
                        success.setContentText("保存成功！");
                        success.showAndWait();
                    });
                } catch (FileSystemException e) {
                    Platform.runLater(() -> {
                        loadDirectory(currentDirectory.getDirEntry().getFullPath());
                        refreshFatView();
                        showError("保存失败", e.getMessage());
                    });
                }
            }
        });
    }
    
    /**
     * 将指定大小和单位转换为字节数
     */
    private long convertToBytes(long size, String unit) {
        switch (unit) {
            case "KB":
                return size * 1024;
            case "MB":
                return size * 1024 * 1024;
            case "GB":
                return size * 1024 * 1024 * 1024;
            case "TB":
                return size * 1024 * 1024 * 1024 * 1024;
            case "B":
            default:
                return size;
        }
    }

    // ============================== 核心业务逻辑 ==============================
    /**
     * 加载指定路径的目录内容
     */
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

            // 先刷新目录缓存，确保获取到最新数据
            currentDirectory.refreshEntries();
            
            // 关键修复：不从currentDirectory直接获取条目，而是通过fileSystem.listDirectory重新加载
            // 这确保了获取到的是文件系统的最新状态，而仅仅是内存缓存
            List<FileEntry> entries = new ArrayList<>(fileSystem.listDirectory(path));
            
            // 清空现有项并添加新项
            fileTableView.getItems().clear();
            fileTableView.getItems().addAll(entries);
            
            // 强制刷新TableView的UI显示
            Platform.runLater(() -> {
                fileTableView.refresh();
                // 刷新UI的其他部分
                fileTableView.requestFocus();
                fileTableView.getSelectionModel().clearSelection();
            });

            // 更新文件数量状态栏
            fileCountLabel.setText(String.format("文件数量：%d", entries.size()));
            
            // 如果当前是图标视图模式，也刷新图标视图
            refreshCurrentView();

        } catch (FileSystemException e) {
            showError("加载目录失败", e.getMessage());
        }
    }

    /**
     * 显示新建文件对话框
     */
    private void showNewFileDialog() {
        if (currentDirectory == null) {
            showWarning("提示", "请先选择一个目录");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("新建文件.txt");
        dialog.setTitle("新建文件");
        dialog.setHeaderText("请输入文件名：");
        dialog.setContentText("文件名：");

        dialog.showAndWait().ifPresent(fileName -> {
            String fullPath = null;
            try {
                // 调用业务层创建文件，确保路径格式正确
                String parentPath = currentDirectory.getDirEntry().getFullPath();
                fullPath = parentPath.endsWith("/") ? parentPath + fileName : parentPath + "/" + fileName;
                
                LogUtil.debug("准备创建文件：" + fullPath);
                fileSystem.createFile(fullPath);
                LogUtil.info("文件创建成功：" + fullPath);
                
                // 刷新列表
                LogUtil.debug("创建文件后刷新目录：" + parentPath);
                loadDirectory(parentPath);
                // 刷新 FAT 视图
                refreshFatView();

            } catch (FileSystemException e) {
                String pathInfo = (fullPath != null) ? "，路径：" + fullPath : "";
                LogUtil.error("创建文件失败：" + e.getMessage() + pathInfo);
                showError("创建文件失败", e.getMessage());
            }
        });
    }

    /**
     * 显示新建文件夹对话框
     */
    private void showNewDirDialog() {
        if (currentDirectory == null) {
            showWarning("提示", "请先选择一个目录");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("新建文件夹");
        dialog.setTitle("新建文件夹");
        dialog.setHeaderText("请输入文件夹名：");
        dialog.setContentText("文件夹名：");

        dialog.showAndWait().ifPresent(dirName -> {
            try {
                // 调用业务层创建文件夹，确保路径格式正确
                String parentPath = currentDirectory.getDirEntry().getFullPath();
                String fullPath = parentPath.endsWith("/") ? parentPath + dirName : parentPath + "/" + dirName;
                
                fileSystem.createDirectory(fullPath);
                
                // 刷新列表和目录树
                loadDirectory(parentPath);
                initDirectoryTree(); // 重新加载目录树
                // 刷新 FAT 视图
                refreshFatView();

            } catch (FileSystemException e) {
                showError("创建文件夹失败", e.getMessage());
            }
        });
    }

    /**
     * 删除选中的文件/文件夹
     */
    private void deleteSelectedEntry() {
        FileEntry selectedEntry = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showWarning("提示", "请先选择要删除的文件或文件夹");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText(null);
        String confirmMessage = selectedEntry.getType() == FileEntry.EntryType.DIRECTORY 
            ? "确定要删除文件夹 " + selectedEntry.getName() + " 及其所有内容吗？" 
            : "确定要删除文件 " + selectedEntry.getName() + " 吗？";
        confirm.setContentText(confirmMessage);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String fullPath = selectedEntry.getFullPath();
                    
                    if (selectedEntry.getType() == FileEntry.EntryType.DIRECTORY) {
                        // 对于目录，使用递归删除方法
                        fileSystem.deleteDirectoryRecursively(fullPath);
                    } else {
                        // 对于文件，使用普通删除方法
                        fileSystem.deleteFile(fullPath);
                    }
                    
                    loadDirectory(currentDirectory.getDirEntry().getFullPath()); // 刷新列表
                    initDirectoryTree(); // 刷新目录树
                    // 删除后刷新 FAT 视图
                    refreshFatView();
                } catch (FileSystemException e) {
                    showError("删除失败", e.getMessage());
                }
            }
        });
    }

    /**
     * 设置右键菜单
     */
    private void setupContextMenus() {
        // 创建空白区域右键菜单（新建文件、新建文件夹）
        ContextMenu emptyAreaContextMenu = new ContextMenu();
        MenuItem newFileMenuItem = new MenuItem("新建文件");
        MenuItem newDirMenuItem = new MenuItem("新建文件夹");
        
        newFileMenuItem.setOnAction(e -> showNewFileDialog());
        newDirMenuItem.setOnAction(e -> showNewDirDialog());
        
        emptyAreaContextMenu.getItems().addAll(newFileMenuItem, newDirMenuItem);
        
        // 创建选中项右键菜单（删除、属性）
        ContextMenu selectedItemContextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("删除");
        MenuItem propertiesMenuItem = new MenuItem("属性");
        
        deleteMenuItem.setOnAction(e -> deleteSelectedEntry());
        propertiesMenuItem.setOnAction(e -> showFilePropertiesDialog());
        
        selectedItemContextMenu.getItems().addAll(deleteMenuItem, propertiesMenuItem);
        
        // 为文件表格设置右键菜单
        fileTableView.setOnContextMenuRequested(event -> {
            // 获取点击位置的表格行
            Node node = event.getPickResult().getIntersectedNode();
            // 检查是否点击在空白区域（没有选中任何行）
            boolean clickedOnEmpty = node == fileTableView || (node != null && node.getParent() == fileTableView);
            
            // 如果点击在空白区域，先清除选中
            if (clickedOnEmpty) {
                fileTableView.getSelectionModel().clearSelection();
            }
            
            FileEntry selectedEntry = fileTableView.getSelectionModel().getSelectedItem();
            if (selectedEntry != null) {
                // 有选中项时显示删除菜单
                selectedItemContextMenu.show(fileTableView, event.getScreenX(), event.getScreenY());
            } else {
                // 空白区域显示新建菜单
                emptyAreaContextMenu.show(fileTableView, event.getScreenX(), event.getScreenY());
            }
        });
        
        // 点击其他地方时隐藏菜单并处理空白区域点击（自定义双击阈值）
        fileTableView.setOnMouseClicked(event -> {
            // 获取点击位置的节点并尝试找到对应的TableRow
            Node node = event.getPickResult().getIntersectedNode();
            TableRow<FileEntry> row = null;
            Node cur = node;
            while (cur != null && !(cur instanceof TableRow)) {
                cur = cur.getParent();
            }
            if (cur instanceof TableRow) {
                row = (TableRow<FileEntry>) cur;
            }
            int clickedRowIndex = (row != null) ? row.getIndex() : -1;
            FileEntry clickedEntry = (row != null) ? row.getItem() : null;
            boolean clickedOnEmpty = (row == null) || (clickedEntry == null);
            
            // 隐藏右键菜单
            if (event.getButton() == MouseButton.PRIMARY) {
                emptyAreaContextMenu.hide();
                selectedItemContextMenu.hide();
            }
            
            long now = System.currentTimeMillis();
            boolean isDoubleClick = event.getButton() == MouseButton.PRIMARY
                    && clickedRowIndex >= 0
                    && clickedRowIndex == lastClickedRowIndex
                    && (now - lastClickTime) <= DOUBLE_CLICK_THRESHOLD_MS;
            
            // 标记是否在本次点击中执行了取消选中，用于正确更新点击状态
            boolean performedCancel = false;
            
            if (event.getButton() == MouseButton.PRIMARY) {
                if (isDoubleClick && !clickedOnEmpty && clickedEntry != null) {
                    // 快速双击：执行打开逻辑
                    if (clickedEntry.getType() == FileEntry.EntryType.FILE) {
                        showEditFileContentDialog(clickedEntry);
                    } else if (clickedEntry.getType() == FileEntry.EntryType.DIRECTORY) {
                        String parentPath = currentDirectory.getDirEntry().getFullPath();
                        String path = parentPath.endsWith("/") ? parentPath + clickedEntry.getName() : parentPath + "/" + clickedEntry.getName();
                        loadDirectory(path);
                    }
                } else {
                    // 单击：空白区域取消选中；分开两次单击同一项才取消选中
                    if (clickedOnEmpty) {
                        fileTableView.getSelectionModel().clearSelection();
                        fileTableView.refresh();
                        try { fileTableView.getFocusModel().focus(-1); } catch (Exception ignore) {}
                        performedCancel = true;
                    } else {
                        boolean isRepeatSingleClick = clickedRowIndex >= 0
                                && clickedRowIndex == lastClickedRowIndex
                                && (now - lastClickTime) > DOUBLE_CLICK_THRESHOLD_MS;
                        if (isRepeatSingleClick) {
                            fileTableView.getSelectionModel().clearSelection();
                            fileTableView.refresh();
                            try { fileTableView.getFocusModel().focus(-1); } catch (Exception ignore) {}
                            performedCancel = true;
                        } // 否则保持选中，无需处理
                    }
                }
            }
            
            // 更新最近点击状态（若本次点击执行了取消选中，则重置为-1，避免后续单击立即再次被视为重复单击）
            lastClickedRowIndex = performedCancel ? -1 : clickedRowIndex;
            lastClickTime = now;
        });
        
        // 为图标视图的ScrollPane和FlowPane设置右键菜单
        if (iconViewScrollPane != null && iconViewPane != null) {
            // 为ScrollPane设置右键菜单（处理ScrollPane空白区域）
            iconViewScrollPane.setOnContextMenuRequested(event -> {
                // 获取点击位置的节点
                Node node = event.getPickResult().getIntersectedNode();
                
                // 检查是否点击在空白区域（没有点击在任何图标项上）
                boolean clickedOnEmpty = true;
                Node cur = node;
                while (cur != null) {
                    if (cur.getStyleClass().contains("icon-item")) {
                        clickedOnEmpty = false;
                        break;
                    }
                    cur = cur.getParent();
                }
                
                // 如果点击在空白区域，显示新建菜单
                if (clickedOnEmpty) {
                    emptyAreaContextMenu.show(iconViewScrollPane, event.getScreenX(), event.getScreenY());
                }
            });
            
            // 为FlowPane设置右键菜单（处理FlowPane空白区域）
            iconViewPane.setOnContextMenuRequested(event -> {
                // 获取点击位置的节点
                Node node = event.getPickResult().getIntersectedNode();
                
                // 检查是否点击在空白区域（没有点击在任何图标项上）
                boolean clickedOnEmpty = true;
                Node cur = node;
                while (cur != null) {
                    if (cur.getStyleClass().contains("icon-item")) {
                        clickedOnEmpty = false;
                        break;
                    }
                    cur = cur.getParent();
                }
                
                // 如果点击在空白区域，显示新建菜单
                if (clickedOnEmpty) {
                    emptyAreaContextMenu.show(iconViewPane, event.getScreenX(), event.getScreenY());
                }
            });
            
            // 为ScrollPane添加鼠标点击事件，用于隐藏右键菜单
            iconViewScrollPane.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    emptyAreaContextMenu.hide();
                    selectedItemContextMenu.hide();
                }
            });
            
            // 为FlowPane添加鼠标点击事件，用于隐藏右键菜单
            iconViewPane.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    emptyAreaContextMenu.hide();
                    selectedItemContextMenu.hide();
                }
            });
        }
    }

    /**
     * 返回上级目录
     */
    private void navigateBack() {
        if (currentDirectory == null) {
            return;
        }
        
        String currentPath = currentDirectory.getDirEntry().getFullPath();
        
        // 如果已经在根目录，不能再返回
        if (currentPath.equals("/")) {
            return;
        }
        
        // 计算父目录路径
        String parentPath;
        if (currentPath.endsWith("/")) {
            currentPath = currentPath.substring(0, currentPath.length() - 1);
        }
        
        int lastSlashIndex = currentPath.lastIndexOf('/');
        if (lastSlashIndex <= 0) {
            parentPath = "/";
        } else {
            parentPath = currentPath.substring(0, lastSlashIndex);
        }
        
        // 加载父目录
        loadDirectory(parentPath);
    }

    // ============================== 工具方法 ==============================
    /**
     * 获取目录树节点的完整路径
     */
    private String getFullPath(TreeItem<String> item) {
        if (item == computerRootItem) {
            return "/";
        }
        StringBuilder path = new StringBuilder(item.getValue());
        TreeItem<String> parent = item.getParent();
        while (parent != null && parent != computerRootItem) {
            path.insert(0, parent.getValue() + "/");
            parent = parent.getParent();
        }
        // 确保路径以/开头
        if (!path.toString().startsWith("/")) {
            path.insert(0, "/");
        }
        // 移除多余的斜杠
        return path.toString().replaceAll("//+", "/");
    }

    /**
     * 显示错误对话框
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示警告对话框
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============================== 外部注入 ==============================
    /**
     * 注入FileSystem实例（由应用启动类调用）
     */
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        initDirectoryTree(); // 重新初始化目录树
        // 初次注入后刷新 FAT 视图
        refreshFatView();
    }

    // 初始化 FAT监视器的列
    private void initFatTableColumns() {
        if (fatBlockIdColumn != null) {
            fatBlockIdColumn.setCellValueFactory(new PropertyValueFactory<>("blockId"));
        }
        if (fatValueColumn != null) {
            fatValueColumn.setCellValueFactory(new PropertyValueFactory<>("fatValue"));
        }
        if (fatStatusColumn != null) {
            fatStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
    }

    // FAT行模型
    public static class FatRow {
        private final int blockId;
        private final int value;
        public FatRow(int blockId, int value) {
            this.blockId = blockId;
            this.value = value;
        }
        public int getBlockId() { return blockId; }
        public String getFatValue() { return String.valueOf(value); }
        public String getStatus() {
            if (value == FAT.FREE_BLOCK) return "空闲";
            if (value == FAT.END_OF_FILE) return "文件结束";
            if (value == FAT.BAD_BLOCK) return "坏块";
            return "指向 " + value;
        }
    }

    // 刷新 FAT 视图
    private void refreshFatView() {
        if (fileSystem == null || fileSystem.getFat() == null || fatTableView == null) {
            return;
        }
        FAT fat = fileSystem.getFat();
        byte[] table = fat.getFatTable();
        List<FatRow> rows = new ArrayList<>(table.length);
        int free = 0, used = 0, bad = 0;
        for (int i = 0; i < table.length; i++) {
            byte v = table[i];
            int intValue = v & 0xFF; // 转换为无符号整数显示
            rows.add(new FatRow(i, intValue));
            if (v == FAT.FREE_BLOCK) free++;
            else if (v == FAT.BAD_BLOCK) bad++;
            else used++;
        }
        fatTableView.getItems().setAll(rows);
        if (fatFreeCountLabel != null) fatFreeCountLabel.setText("空闲：" + free);
        if (fatUsedCountLabel != null) fatUsedCountLabel.setText("已用：" + used);
        if (fatBadCountLabel != null) fatBadCountLabel.setText("坏块：" + bad);
    }


    // 地址栏回车事件处理
    private void handlePathEnter() {
        String input = pathTextField.getText();
        if (input == null) return;
        String targetPath = normalizePath(input.trim());
        try {
            Directory dir = fileSystem.getDirectory(targetPath);
            if (dir == null) {
                showWarning("提示", "目录不存在：" + targetPath);
                // 回退到当前目录
                if (currentDirectory != null) {
                    pathTextField.setText(currentDirectory.getDirEntry().getFullPath());
                } else {
                    pathTextField.setText("/");
                }
                return;
            }
            loadDirectory(targetPath);
            // 同步目录树选中
            selectTreeItemByPath(targetPath);
        } catch (FileSystemException e) {
            showError("跳转目录失败", e.getMessage());
        }
    }

    // 规范化路径：确保以/开头并清理重复斜杠
    private String normalizePath(String path) {
        if (path.isEmpty()) return "/";
        String p = path;
        if (!p.startsWith("/")) p = "/" + p;
        return p.replaceAll("/+", "/");
    }

    // 在目录树中选中指定路径
    private void selectTreeItemByPath(String path) {
        if (dirTreeView == null || computerRootItem == null) return;
        // 找到根'/'节点
        TreeItem<String> rootItem = null;
        for (TreeItem<String> child : computerRootItem.getChildren()) {
            if ("/".equals(child.getValue())) {
                rootItem = child;
                break;
            }
        }
        if (rootItem == null) return;
        rootItem.setExpanded(true);
        dirTreeView.getSelectionModel().select(rootItem);
        
        String[] parts = path.split("/");
        TreeItem<String> current = rootItem;
        String built = "/";
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            built = built.endsWith("/") ? built + part : built + "/" + part;
            // 确保当前节点的子目录已加载
            loadSubDirectories(built, current);
            // 在子节点中查找匹配项
            TreeItem<String> next = null;
            for (TreeItem<String> child : current.getChildren()) {
                if (part.equals(child.getValue())) {
                    next = child;
                    break;
                }
            }
            if (next == null) break;
            next.setExpanded(true);
            current = next;
        }
        // 选中最后定位到的节点
        dirTreeView.getSelectionModel().select(current);
        try {
            int row = dirTreeView.getRow(current);
            if (row >= 0) dirTreeView.scrollTo(row);
        } catch (Exception ignore) {}
    }
    
    /**
     * 显示文件或文件夹属性对话框（根据类型自动选择）
     */
    private void showFilePropertiesDialog() {
        FileEntry selectedEntry = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showWarning("警告", "请先选择一个文件或文件夹");
            return;
        }
        
        // 根据类型调用不同的属性对话框
        if (selectedEntry.getType() == FileEntry.EntryType.FILE) {
            showFilePropertiesDialogInternal(selectedEntry);
        } else {
            showFolderPropertiesDialogInternal(selectedEntry);
        }
    }
    
    /**
     * 显示文件属性对话框（包含只读选项）
     */
    private void showFilePropertiesDialogInternal(FileEntry selectedEntry) {
        String currentPath = (currentDirectory != null) ? currentDirectory.getDirEntry().getFullPath() : "/";
        String fullPath = currentPath.endsWith("/") ? currentPath + selectedEntry.getName() : currentPath + "/" + selectedEntry.getName();
    
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("文件属性");
        dialog.setHeaderText(selectedEntry.getName() + " 的属性");
    
        ButtonType SAVE_BUTTON = new ButtonType("保存", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(SAVE_BUTTON, ButtonType.CANCEL);
    
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(10));
    
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("文件名:");
        javafx.scene.control.TextField nameValue = new javafx.scene.control.TextField(selectedEntry.getName());
        final String originalName = selectedEntry.getName();

        javafx.scene.control.Label pathLabel = new javafx.scene.control.Label("路径:");
        javafx.scene.control.Label pathValue = new javafx.scene.control.Label(fullPath);
    
        javafx.scene.control.Label typeLabel = new javafx.scene.control.Label("类型:");
        javafx.scene.control.Label typeValue = new javafx.scene.control.Label("文件");
    
        javafx.scene.control.Label sizeLabel = new javafx.scene.control.Label("大小:");
        String sizeText = String.format("%d 字节 (%.2f KB)", selectedEntry.getSize(), selectedEntry.getSize() / 1024.0);
        javafx.scene.control.Label sizeValue = new javafx.scene.control.Label(sizeText);
    
        javafx.scene.control.Label modifyLabel = new javafx.scene.control.Label("修改时间:");
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.ofInstant(selectedEntry.getModifyTime().toInstant(), java.time.ZoneId.systemDefault());
        String modifyText = localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        javafx.scene.control.Label modifyValue = new javafx.scene.control.Label(modifyText);
    
        javafx.scene.control.CheckBox readOnlyCheck = new javafx.scene.control.CheckBox("只读");
        readOnlyCheck.setSelected(selectedEntry.isReadOnly());
    
        grid.addRow(0, nameLabel, nameValue);
        grid.addRow(1, pathLabel, pathValue);
        grid.addRow(2, typeLabel, typeValue);
        grid.addRow(3, sizeLabel, sizeValue);
        grid.addRow(4, modifyLabel, modifyValue);
        grid.addRow(5, new javafx.scene.control.Label("属性:"), readOnlyCheck);
    
        dialog.getDialogPane().setContent(grid);
    
        dialog.showAndWait().ifPresent(result -> {
            if (result == SAVE_BUTTON) {
                try {
                    String newName = nameValue.getText().trim();
                    boolean nameChanged = !newName.equals(originalName);
                    
                    if (newName.isEmpty()) {
                        showError("重命名失败", "文件名不能为空");
                        return;
                    }
                    
                    boolean newReadOnly = readOnlyCheck.isSelected();
                    
                    if (nameChanged) {
                        boolean fileExists = currentDirectory.getEntries().stream()
                                .filter(entry -> !entry.isDeleted())
                                .anyMatch(entry -> entry.getName().equals(newName) && !entry.equals(selectedEntry));
                        
                        if (fileExists) {
                            showError("重命名失败", "当前目录下已存在同名文件：" + newName);
                            return;
                        }
                        
                        FileEntry newEntry = new FileEntry(
                                newName,
                                selectedEntry.getType(),
                                selectedEntry.getParentPath(),
                                selectedEntry.getStartBlockId(),
                                selectedEntry.getUuid()
                        );
                        newEntry.setSize(selectedEntry.getSize());
                        newEntry.setReadOnly(newReadOnly);
                        
                        currentDirectory.removeEntry(originalName);
                        currentDirectory.addEntry(newEntry);
                    } else {
                        selectedEntry.setReadOnly(newReadOnly);
                    }
                    
                    currentDirectory.markDirty();
                    currentDirectory.syncToDisk();
                    loadDirectory(currentDirectory.getDirEntry().getFullPath());
                    refreshFatView();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("成功");
                    success.setHeaderText(null);
                    success.setContentText(nameChanged ? "文件已重命名并保存属性" : "属性已保存");
                    success.showAndWait();
                } catch (org.jiejiejiang.filemanager.exception.FileSystemException e) {
                    showError("保存属性失败", e.getMessage());
                }
            }
        });
    }
    
    /**
     * 显示文件夹属性对话框（不包含只读选项，支持重命名）
     */
    private void showFolderPropertiesDialogInternal(FileEntry selectedEntry) {
        String currentPath = (currentDirectory != null) ? currentDirectory.getDirEntry().getFullPath() : "/";
        String fullPath = currentPath.endsWith("/") ? currentPath + selectedEntry.getName() : currentPath + "/" + selectedEntry.getName();
    
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("文件夹属性");
        dialog.setHeaderText(selectedEntry.getName() + " 的属性");
    
        ButtonType SAVE_BUTTON = new ButtonType("保存", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(SAVE_BUTTON, ButtonType.CANCEL);
    
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(10));
    
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("文件夹名:");
        javafx.scene.control.TextField nameValue = new javafx.scene.control.TextField(selectedEntry.getName());
        final String originalName = selectedEntry.getName();

        javafx.scene.control.Label pathLabel = new javafx.scene.control.Label("路径:");
        javafx.scene.control.Label pathValue = new javafx.scene.control.Label(fullPath);
    
        javafx.scene.control.Label typeLabel = new javafx.scene.control.Label("类型:");
        javafx.scene.control.Label typeValue = new javafx.scene.control.Label("文件夹");
    
        javafx.scene.control.Label modifyLabel = new javafx.scene.control.Label("修改时间:");
        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.ofInstant(selectedEntry.getModifyTime().toInstant(), java.time.ZoneId.systemDefault());
        String modifyText = localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        javafx.scene.control.Label modifyValue = new javafx.scene.control.Label(modifyText);
        
        // 计算文件夹包含的项目数量
        javafx.scene.control.Label itemsLabel = new javafx.scene.control.Label("包含项目:");
        String itemsText;
        try {
            // 尝试加载文件夹内容来计算项目数量
            Directory folderDir = fileSystem.getDirectory(fullPath);
            if (folderDir != null) {
                long fileCount = folderDir.getEntries().stream().filter(entry -> !entry.isDeleted() && entry.getType() == FileEntry.EntryType.FILE).count();
                long folderCount = folderDir.getEntries().stream().filter(entry -> !entry.isDeleted() && entry.getType() == FileEntry.EntryType.DIRECTORY).count();
                itemsText = String.format("%d 个文件，%d 个文件夹", fileCount, folderCount);
            } else {
                itemsText = "无法访问";
            }
        } catch (Exception e) {
            itemsText = "无法访问";
        }
        javafx.scene.control.Label itemsValue = new javafx.scene.control.Label(itemsText);
    
        grid.addRow(0, nameLabel, nameValue);
        grid.addRow(1, pathLabel, pathValue);
        grid.addRow(2, typeLabel, typeValue);
        grid.addRow(3, itemsLabel, itemsValue);
        grid.addRow(4, modifyLabel, modifyValue);
    
        dialog.getDialogPane().setContent(grid);
    
        dialog.showAndWait().ifPresent(result -> {
            if (result == SAVE_BUTTON) {
                try {
                    String newName = nameValue.getText().trim();
                    boolean nameChanged = !newName.equals(originalName);
                    
                    if (newName.isEmpty()) {
                        showError("重命名失败", "文件夹名不能为空");
                        return;
                    }
                    
                    if (nameChanged) {
                        boolean folderExists = currentDirectory.getEntries().stream()
                                .filter(entry -> !entry.isDeleted())
                                .anyMatch(entry -> entry.getName().equals(newName) && !entry.equals(selectedEntry));
                        
                        if (folderExists) {
                            showError("重命名失败", "当前目录下已存在同名文件夹：" + newName);
                            return;
                        }
                        
                        // 使用FileSystem的重命名方法，正确处理子文件和子文件夹的路径更新
                        String oldFullPath = selectedEntry.getFullPath();
                        fileSystem.renameDirectory(oldFullPath, newName);
                        
                        // 重新加载当前目录以反映更改
                        loadDirectory(currentDirectory.getDirEntry().getFullPath());
                        
                        // 更新目录树以反映文件夹重命名
                        initDirectoryTree();
                        
                        // 刷新FAT视图
                        refreshFatView();

                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("成功");
                        success.setHeaderText(null);
                        success.setContentText("文件夹已重命名");
                        success.showAndWait();
                    } else {
                        // 文件夹名没有改变，无需操作
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("提示");
                        info.setHeaderText(null);
                        info.setContentText("文件夹名称未更改");
                        info.showAndWait();
                    }
                } catch (org.jiejiejiang.filemanager.exception.FileSystemException e) {
                    showError("重命名失败", e.getMessage());
                }
            }
        });
    }
    
    // ============================== 视图切换相关方法 ==============================
    
    /**
     * 切换视图模式（在列表和图标之间切换）
     */
    private void toggleViewMode() {
        if (currentViewMode == ViewMode.LIST) {
            switchToIconView();
        } else {
            switchToListView();
        }
    }
    
    /**
     * 切换到列表视图
     */
    private void switchToListView() {
        currentViewMode = ViewMode.LIST;
        fileTableView.setVisible(true);
        iconViewScrollPane.setVisible(false);
        toggleViewButton.setText("图标视图");
        LogUtil.info("切换到列表视图");
    }
    
    /**
     * 切换到图标视图
     */
    private void switchToIconView() {
        currentViewMode = ViewMode.ICON;
        fileTableView.setVisible(false);
        iconViewScrollPane.setVisible(true);
        toggleViewButton.setText("列表视图");
        refreshIconView();
        LogUtil.info("切换到图标视图");
    }
    
    /**
     * 刷新图标视图
     */
    private void refreshIconView() {
        if (iconViewPane == null) return;
        
        iconViewPane.getChildren().clear();
        
        if (currentDirectory == null) return;
        
        try {
            List<FileEntry> entries = fileSystem.listDirectory(currentDirectory.getDirEntry().getFullPath());
            
            for (FileEntry entry : entries) {
                javafx.scene.layout.VBox iconItem = createIconItem(entry);
                iconViewPane.getChildren().add(iconItem);
            }
        } catch (FileSystemException e) {
            LogUtil.error("刷新图标视图失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建单个图标项
     */
    private javafx.scene.layout.VBox createIconItem(FileEntry entry) {
        javafx.scene.layout.VBox iconItem = new javafx.scene.layout.VBox();
        iconItem.setAlignment(javafx.geometry.Pos.CENTER);
        iconItem.setSpacing(5);
        iconItem.setPrefWidth(80);
        iconItem.setStyle("-fx-cursor: hand; -fx-padding: 5;");
        
        // 添加样式类标识，用于右键菜单识别
        iconItem.getStyleClass().add("icon-item");
        
        // 创建图标
        javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView();
        iconView.setFitWidth(64);  // 放大图标尺寸从48到64
        iconView.setFitHeight(64);
        iconView.setPreserveRatio(true);
        iconView.getStyleClass().add("file-icon");
        
        // 根据文件类型加载图标
        String iconPath;
        if (entry.getType() == FileEntry.EntryType.DIRECTORY) {
            iconPath = "/org/jiejiejiang/filemanager/images/folder.png";
        } else {
            iconPath = "/org/jiejiejiang/filemanager/images/file.png";
        }
        
        try {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath));
            iconView.setImage(icon);
        } catch (Exception e) {
            LogUtil.error("加载图标失败：" + iconPath + " - " + e.getMessage());
            // 如果图标加载失败，创建一个简单的文本标识
            iconView = null;
        }
        
        // 创建文件名标签
        Label nameLabel = new Label(entry.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(90);  // 增加最大宽度以适应放大的图标
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");  // 放大字体从10px到13px
        
        // 添加组件到容器
        if (iconView != null) {
            iconItem.getChildren().addAll(iconView, nameLabel);
        } else {
            // 如果图标加载失败，显示文件类型标识
            Label typeLabel = new Label(entry.getType() == FileEntry.EntryType.DIRECTORY ? "📁" : "📄");
            typeLabel.setStyle("-fx-font-size: 48px;");  // 放大备用图标字体从32px到48px
            iconItem.getChildren().addAll(typeLabel, nameLabel);
        }
        
        // 添加点击事件
        iconItem.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // 双击事件
                if (entry.getType() == FileEntry.EntryType.DIRECTORY) {
                    // 进入目录
                    String parentPath = currentDirectory.getDirEntry().getFullPath();
                    String fullPath = parentPath.endsWith("/") ? parentPath + entry.getName() : parentPath + "/" + entry.getName();
                    loadDirectory(fullPath);
                    selectTreeItemByPath(fullPath);
                } else {
                    // 打开文件
                    showEditFileContentDialog(entry);
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                // 右键点击 - 显示上下文菜单
                showIconContextMenu(entry, event.getScreenX(), event.getScreenY());
            }
        });
        
        // 添加悬停效果
        iconItem.setOnMouseEntered(e -> iconItem.setStyle("-fx-cursor: hand; -fx-padding: 5; -fx-background-color: #e3f2fd;"));
        iconItem.setOnMouseExited(e -> iconItem.setStyle("-fx-cursor: hand; -fx-padding: 5;"));
        
        return iconItem;
    }
    
    /**
     * 显示图标项的右键菜单
     */
    private void showIconContextMenu(FileEntry entry, double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();
        
        // 打开/进入
        MenuItem openItem = new MenuItem(entry.getType() == FileEntry.EntryType.DIRECTORY ? "进入" : "打开");
        openItem.setOnAction(e -> {
            if (entry.getType() == FileEntry.EntryType.DIRECTORY) {
                String parentPath = currentDirectory.getDirEntry().getFullPath();
                String fullPath = parentPath.endsWith("/") ? parentPath + entry.getName() : parentPath + "/" + entry.getName();
                loadDirectory(fullPath);
                selectTreeItemByPath(fullPath);
            } else {
                showEditFileContentDialog(entry);
            }
        });
        
        // 属性
        MenuItem propertiesItem = new MenuItem("属性");
        propertiesItem.setOnAction(e -> {
            // 先在表格中选中该项，然后显示属性对话框
            fileTableView.getSelectionModel().clearSelection();
            for (int i = 0; i < fileTableView.getItems().size(); i++) {
                if (fileTableView.getItems().get(i).getName().equals(entry.getName())) {
                    fileTableView.getSelectionModel().select(i);
                    break;
                }
            }
            showFilePropertiesDialog();
        });
        
        // 删除
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> {
            // 模拟选中该项并删除
            fileTableView.getSelectionModel().clearSelection();
            // 创建一个临时选择来删除
            deleteFileEntry(entry);
        });
        
        contextMenu.getItems().addAll(openItem, propertiesItem, deleteItem);
        contextMenu.show(iconViewPane, screenX, screenY);
    }
    
    /**
     * 删除指定的文件条目
     */
    private void deleteFileEntry(FileEntry entry) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要删除 \"" + entry.getName() + "\" 吗？");
        
        confirmAlert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String parentPath = currentDirectory.getDirEntry().getFullPath();
                    String fullPath = parentPath.endsWith("/") ? parentPath + entry.getName() : parentPath + "/" + entry.getName();
                    
                    if (entry.getType() == FileEntry.EntryType.DIRECTORY) {
                        fileSystem.deleteDirectoryRecursively(fullPath);
                    } else {
                        fileSystem.deleteFile(fullPath);
                    }
                    
                    // 刷新视图
                    loadDirectory(currentDirectory.getDirEntry().getFullPath());
                    initDirectoryTree();
                    refreshFatView();
                } catch (FileSystemException e) {
                    showError("删除失败", e.getMessage());
                }
            }
        });
    }
    
    /**
     * 重写loadDirectory方法以支持视图切换
     */
    private void refreshCurrentView() {
        if (currentViewMode == ViewMode.ICON) {
            refreshIconView();
        }
        // 列表视图的刷新已经在原有的loadDirectory方法中处理
    }
}
