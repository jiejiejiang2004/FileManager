package org.jiejiejiang.filemanager.gui.controller;

import java.util.ArrayList;
import java.util.List;

import org.jiejiejiang.filemanager.core.FAT;
import org.jiejiejiang.filemanager.core.FileSystem;
import org.jiejiejiang.filemanager.util.LogUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * 磁盘块查看器控制器
 * 用于展示磁盘的每个块和每个字节的内容，以及图形化展示磁盘使用率
 */
public class DiskViewerController {
    
    // FXML组件
    @FXML private ProgressBar diskUsageProgressBar;
    @FXML private Label diskUsageLabel;
    @FXML private TableView<BlockInfo> blockTableView;
    @FXML private TableColumn<BlockInfo, Integer> blockIdColumn;
    @FXML private TableColumn<BlockInfo, String> blockStatusColumn;
    @FXML private TableColumn<BlockInfo, Integer> blockSizeColumn;
    @FXML private TableColumn<BlockInfo, String> blockContentColumn;
    @FXML private Label selectedBlockLabel;
    @FXML private TextArea hexTextArea;
    @FXML private TextArea asciiTextArea;
    @FXML private Label totalBlocksLabel;
    @FXML private Label usedBlocksLabel;
    @FXML private Label freeBlocksLabel;
    @FXML private Label badBlocksLabel;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
    
    // 数据
    private FileSystem fileSystem;
    private ObservableList<BlockInfo> blockData = FXCollections.observableArrayList();
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        initTableColumns();
        bindEvents();
    }
    
    /**
     * 初始化表格列
     */
    private void initTableColumns() {
        blockIdColumn.setCellValueFactory(new PropertyValueFactory<>("blockId"));
        blockStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        blockSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        blockContentColumn.setCellValueFactory(new PropertyValueFactory<>("contentPreview"));
        
        blockTableView.setItems(blockData);
    }
    
    /**
     * 绑定事件
     */
    private void bindEvents() {
        // 表格选择事件
        blockTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showBlockDetails(newSelection);
            }
        });
        
        // 刷新按钮
        refreshButton.setOnAction(e -> refreshData());
        
        // 关闭按钮
        closeButton.setOnAction(e -> {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        });
    }
    
    /**
     * 设置文件系统
     */
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        refreshData();
    }
    
    /**
     * 刷新数据
     */
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
    
    /**
     * 获取块内容预览
     */
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
    
    /**
     * 显示块详细信息
     */
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
    
    /**
     * 块信息数据类
     */
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
        
        public int getBlockId() {
            return blockId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public int getSize() {
            return size;
        }
        
        public String getContentPreview() {
            return contentPreview;
        }
    }
}