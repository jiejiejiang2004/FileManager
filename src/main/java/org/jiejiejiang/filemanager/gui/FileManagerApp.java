package org.jiejiejiang.filemanager.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jiejiejiang.filemanager.core.Disk;
import org.jiejiejiang.filemanager.core.FileSystem;
import org.jiejiejiang.filemanager.core.FAT;
import org.jiejiejiang.filemanager.exception.FileSystemException;
import org.jiejiejiang.filemanager.exception.DiskInitializeException;
import org.jiejiejiang.filemanager.util.LogUtil;

import java.io.IOException;
import java.net.URL;

/**
 * JavaFX 应用入口类，负责初始化应用并加载主界面
 */
public class FileManagerApp extends Application {

    // 磁盘配置文件路径（位于resources/config目录下，符合.gitignore对资源文件的保留规则）
    private static final String DISK_CONFIG_PATH = "D:\\Project\\FileManager\\src\\main\\resources\\org\\jiejiejiang\\filemanager\\config\\disk.properties";

    private FileSystem fileSystem; // 文件系统核心实例

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. 初始化文件系统（磁盘 + FAT）
            initFileSystem();

            // 2. 加载主界面 FXML
            URL fxmlUrl = getClass().getResource("/org/jiejiejiang/filemanager/fxml/MainView.fxml");
            if (fxmlUrl == null) {
                throw new IOException("未找到主界面布局文件: MainView.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // 3. 向主控制器注入文件系统实例
            org.jiejiejiang.filemanager.gui.controller.MainController mainController = loader.getController();
            mainController.setFileSystem(fileSystem);

            // 4. 配置主窗口
            primaryStage.setTitle("Simple File Manager");
            primaryStage.setScene(new Scene(root, 800, 600)); // 初始窗口大小
            primaryStage.show();

            LogUtil.info("应用启动成功");

        } catch (Exception e) {
            LogUtil.error("应用启动失败", e);
            showErrorDialog(primaryStage, "启动失败", "无法初始化应用: " + e.getMessage());
        }
    }

    /**
     * 初始化文件系统（磁盘 + FAT + 挂载）
     */
    private void initFileSystem() throws FileSystemException, DiskInitializeException {
        // 1. 从配置文件初始化磁盘（使用类路径下的config资源）
        Disk disk = new Disk(DISK_CONFIG_PATH);
        disk.initialize(); // 完成磁盘初始化（创建或加载磁盘文件）

        // 2. 初始化FAT表（基于磁盘配置）
        FAT fat = new FAT(disk);
        fat.initialize(); // 从磁盘加载或创建新FAT表

        // 3. 创建并挂载文件系统
        fileSystem = new FileSystem(disk, fat);
        fileSystem.mount(); // 挂载文件系统（初始化根目录）
    }

    /**
     * 显示错误对话框
     */
    private void showErrorDialog(Stage stage, String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        // 应用关闭时卸载文件系统，释放资源
        if (fileSystem != null && fileSystem.isMounted()) {
            try {
                fileSystem.unmount();
                LogUtil.info("应用关闭，文件系统已卸载");
            } catch (FileSystemException e) {
                LogUtil.error("文件系统卸载失败", e);
            }
        }
    }

    public static void main(String[] args) {
        launch(args); // 启动JavaFX应用
    }
}