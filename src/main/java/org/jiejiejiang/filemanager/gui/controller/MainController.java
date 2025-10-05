package org.jiejiejiang.filemanager.gui.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.jiejiejiang.filemanager.core.Directory;
import org.jiejiejiang.filemanager.core.FileEntry;
import org.jiejiejiang.filemanager.core.FileSystem;
import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.util.LogUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;

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

    // 菜单组件
    @FXML private MenuItem newFileItem;
    @FXML private MenuItem newDirItem;
    @FXML private MenuItem deleteItem;
    @FXML private MenuItem refreshItem;

    // ============================== 业务对象 ==============================
    private FileSystem fileSystem; // 文件系统核心对象（由外部注入）
    private Directory currentDirectory; // 当前选中的目录

    // ============================== 初始化 ==============================
    @FXML
    public void initialize() {
        // 1. 初始化表格列与FileEntry属性绑定
        initTableColumns();

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

        // 时间列：在setCellValueFactory中直接将Date转换为格式化的String
        modifyTimeColumn.setCellValueFactory(cellData -> {
            FileEntry entry = cellData.getValue();
            Date modifyTime = entry.getModifyTime();
            
            if (modifyTime == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            
            // 将Date转换为格式化的字符串
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = modifyTime.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            
            return new javafx.beans.property.SimpleStringProperty(dateTime.format(formatter));
        });
        
        // 使用默认的String类型的TableCell
        modifyTimeColumn.setCellFactory(column -> new TableCell<FileEntry, String>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                } else {
                    setText(text);
                }
            }
        });
    }

    /**
     * 初始化目录树（模拟磁盘加载）
     */
    private void initDirectoryTree() {
        // 清空示例节点
        computerRootItem.getChildren().clear();

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
                loadSubDirectories(path, expandedItem);
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
        // 先清空已有子节点（避免重复加载）
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
                    
                    // 添加展开事件监听
                     dirItem.addEventHandler(TreeItem.<String>branchExpandedEvent(), event -> {
                         TreeItem<String> expandedItem = event.getTreeItem();
                         // 移除临时子节点
                         expandedItem.getChildren().clear();
                          
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
            if (currentDirectory != null) {
                loadDirectory(currentDirectory.getDirEntry().getFullPath());
            }
        });
    }

    // ============================== 核心业务逻辑 ==============================
    /**
     * 加载指定路径的目录内容
     */
    private void loadDirectory(String path) {
        try {
            // 从文件系统获取目录对象
            currentDirectory = fileSystem.getDirectory(path);
            if (currentDirectory == null) {
                showError("错误", "目录不存在：" + path);
                return;
            }

            // 更新UI状态
            currentPathLabel.setText("当前路径：" + path);

            // 加载文件列表并显示
            List<FileEntry> entries = currentDirectory.getEntries();
            fileTableView.getItems().clear();
            fileTableView.getItems().addAll(entries);

            // 更新文件数量状态栏
            fileCountLabel.setText(String.format("文件数量：%d", entries.size()));

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
            try {
                // 调用业务层创建文件，确保路径格式正确
                String parentPath = currentDirectory.getDirEntry().getFullPath();
                String fullPath = parentPath.endsWith("/") ? parentPath + fileName : parentPath + "/" + fileName;
                
                fileSystem.createFile(fullPath);
                
                // 刷新列表
                loadDirectory(parentPath);

            } catch (FileSystemException e) {
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
        confirm.setContentText("确定要删除 " + selectedEntry.getName() + " 吗？");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    currentDirectory.removeEntry(selectedEntry.getName());
                    currentDirectory.syncToDisk();
                    loadDirectory(currentDirectory.getDirEntry().getFullPath()); // 刷新列表
                    initDirectoryTree(); // 刷新目录树
                } catch (FileSystemException e) {
                    showError("删除失败", e.getMessage());
                }
            }
        });
    }

    // ============================== 工具方法 ==============================
    /**
     * 获取目录树节点的完整路径
     */
    private String getFullPath(TreeItem<String> item) {
        if (item == computerRootItem) {
            return "";
        }
        StringBuilder path = new StringBuilder(item.getValue());
        TreeItem<String> parent = item.getParent();
        while (parent != null && parent != computerRootItem) {
            path.insert(0, parent.getValue() + "/");
            parent = parent.getParent();
        }
        return path.toString().replace("//", "/");
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
    }
}
