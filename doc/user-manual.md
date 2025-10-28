# 用户操作手册 / User Manual

## 中文版 / Chinese Version

### 快速开始

#### 系统要求
- Java 11 或更高版本
- JavaFX 运行时环境
- 至少 100MB 可用磁盘空间

#### 启动应用程序
1. 确保已安装 Java 11+ 和 JavaFX
2. 运行以下命令启动应用程序：
   ```bash
   java -jar FileManager.jar
   ```
   或者使用 Maven：
   ```bash
   mvn javafx:run
   ```

### 界面介绍

#### 主界面布局
```
┌─────────────────────────────────────────────────────────┐
│ 菜单栏: [文件] [编辑] [查看] [工具] [帮助]                    │
├─────────────────────────────────────────────────────────┤
│ 工具栏: [新建] [删除] [刷新] [列表视图] [图标视图]             │
├─────────┬───────────────────────────────────────────────┤
│         │                                               │
│ 目录树   │              文件列表/图标区域                    │
│         │                                               │
│         │                                               │
├─────────┼───────────────────────────────────────────────┤
│ 状态栏: 当前路径 | 文件数量 | 磁盘使用情况                    │
└─────────────────────────────────────────────────────────┘
```

#### 界面组件说明

**1. 目录树（左侧）**
- 显示完整的目录结构
- 点击目录节点可展开/折叠子目录
- 单击目录可在右侧显示其内容

**2. 文件显示区域（右侧）**
- **列表视图**：以表格形式显示文件信息（名称、大小、类型、修改时间）
- **图标视图**：以图标形式显示文件和目录

**3. 状态栏（底部）**
- 显示当前路径
- 显示当前目录的文件数量
- 显示磁盘使用情况

### 基本操作

#### 文件操作

**查看文件**
1. **方法一**：双击文件
2. **方法二**：右键点击文件 → 选择"查看"
3. **方法三**：选中文件 → 菜单栏"文件" → "查看"

*注意：查看模式为只读，无法修改文件内容*

**编辑文件**
1. 右键点击文件 → 选择"编辑"
2. 在弹出的编辑对话框中修改内容
3. 点击"保存"按钮保存修改
4. 点击"取消"放弃修改

**新建文件**
1. **方法一**：在空白区域右键 → "新建文件"
2. **方法二**：工具栏点击"新建"按钮
3. **方法三**：菜单栏"文件" → "新建文件"
4. 在对话框中输入文件名和内容
5. 点击"创建"完成

**删除文件**
1. 右键点击文件 → "删除"
2. 在确认对话框中点击"确定"

**重命名文件**
1. 右键点击文件 → "重命名"
2. 在对话框中输入新名称
3. 点击"确定"完成重命名

#### 目录操作

**进入目录**
1. **方法一**：双击目录
2. **方法二**：右键点击目录 → "进入"
3. **方法三**：在目录树中点击目录节点

**新建目录**
1. **方法一**：在空白区域右键 → "新建文件夹"
2. **方法二**：菜单栏"文件" → "新建文件夹"
3. 输入目录名称
4. 点击"创建"完成

**删除目录**
1. 右键点击目录 → "删除"
2. 确认删除（注意：只能删除空目录）

#### 视图切换

**列表视图**
- 点击工具栏的"列表视图"按钮
- 或菜单栏"查看" → "列表视图"
- 显示详细的文件信息表格

**图标视图**
- 点击工具栏的"图标视图"按钮
- 或菜单栏"查看" → "图标视图"
- 以图标形式显示文件和目录

### 高级功能

#### 文件属性查看
1. 右键点击文件或目录 → "属性"
2. 查看详细信息：
   - 文件名
   - 文件大小
   - 创建时间
   - 文件类型
   - 磁盘位置

#### 磁盘管理
1. 菜单栏"工具" → "磁盘查看器"
2. 查看磁盘使用情况
3. 查看文件分配表（FAT）状态
4. 监控磁盘空间使用

#### 搜索功能
1. 使用 Ctrl+F 打开搜索框
2. 输入文件名或关键词
3. 在当前目录及子目录中搜索

### 快捷键

| 操作 | 快捷键 |
|------|--------|
| 新建文件 | Ctrl+N |
| 删除 | Delete |
| 重命名 | F2 |
| 刷新 | F5 |
| 搜索 | Ctrl+F |
| 复制 | Ctrl+C |
| 粘贴 | Ctrl+V |
| 全选 | Ctrl+A |

### 注意事项

#### 文件操作限制
- 文件名不能包含特殊字符：`\ / : * ? " < > |`
- 单个文件大小限制：10MB
- 文件名长度限制：255个字符
- 目录深度限制：10级

#### 安全提示
- 删除操作不可恢复，请谨慎操作
- 编辑大文件时请注意保存频率
- 建议定期备份重要文件

#### 性能优化建议
- 避免在单个目录中存放过多文件（建议少于1000个）
- 大文件操作时请耐心等待
- 定期清理不需要的文件以释放磁盘空间

---

## English Version

### Quick Start

#### System Requirements
- Java 11 or higher
- JavaFX runtime environment
- At least 100MB available disk space

#### Starting the Application
1. Ensure Java 11+ and JavaFX are installed
2. Run the following command to start the application:
   ```bash
   java -jar FileManager.jar
   ```
   Or using Maven:
   ```bash
   mvn javafx:run
   ```

### Interface Overview

#### Main Interface Layout
```
┌─────────────────────────────────────────────────────────┐
│ Menu Bar: [File] [Edit] [View] [Tools] [Help]            │
├─────────────────────────────────────────────────────────┤
│ Toolbar: [New] [Delete] [Refresh] [List View] [Icon View]│
├─────────┬───────────────────────────────────────────────┤
│         │                                               │
│Directory│              File List/Icon Area              │
│  Tree   │                                               │
│         │                                               │
├─────────┼───────────────────────────────────────────────┤
│ Status Bar: Current Path | File Count | Disk Usage       │
└─────────────────────────────────────────────────────────┘
```

#### Interface Components

**1. Directory Tree (Left Panel)**
- Displays complete directory structure
- Click directory nodes to expand/collapse subdirectories
- Single-click directory to display its contents on the right

**2. File Display Area (Right Panel)**
- **List View**: Displays file information in table format (name, size, type, modified time)
- **Icon View**: Displays files and directories as icons

**3. Status Bar (Bottom)**
- Shows current path
- Shows file count in current directory
- Shows disk usage information

### Basic Operations

#### File Operations

**View File**
1. **Method 1**: Double-click file
2. **Method 2**: Right-click file → Select "View"
3. **Method 3**: Select file → Menu bar "File" → "View"

*Note: View mode is read-only, file content cannot be modified*

**Edit File**
1. Right-click file → Select "Edit"
2. Modify content in the edit dialog
3. Click "Save" button to save changes
4. Click "Cancel" to discard changes

**Create New File**
1. **Method 1**: Right-click in empty area → "New File"
2. **Method 2**: Click "New" button in toolbar
3. **Method 3**: Menu bar "File" → "New File"
4. Enter filename and content in dialog
5. Click "Create" to complete

**Delete File**
1. Right-click file → "Delete"
2. Click "OK" in confirmation dialog

**Rename File**
1. Right-click file → "Rename"
2. Enter new name in dialog
3. Click "OK" to complete rename

#### Directory Operations

**Enter Directory**
1. **Method 1**: Double-click directory
2. **Method 2**: Right-click directory → "Enter"
3. **Method 3**: Click directory node in directory tree

**Create New Directory**
1. **Method 1**: Right-click in empty area → "New Folder"
2. **Method 2**: Menu bar "File" → "New Folder"
3. Enter directory name
4. Click "Create" to complete

**Delete Directory**
1. Right-click directory → "Delete"
2. Confirm deletion (Note: only empty directories can be deleted)

#### View Switching

**List View**
- Click "List View" button in toolbar
- Or menu bar "View" → "List View"
- Displays detailed file information table

**Icon View**
- Click "Icon View" button in toolbar
- Or menu bar "View" → "Icon View"
- Displays files and directories as icons

### Advanced Features

#### File Properties
1. Right-click file or directory → "Properties"
2. View detailed information:
   - Filename
   - File size
   - Creation time
   - File type
   - Disk location

#### Disk Management
1. Menu bar "Tools" → "Disk Viewer"
2. View disk usage
3. View File Allocation Table (FAT) status
4. Monitor disk space usage

#### Search Function
1. Use Ctrl+F to open search box
2. Enter filename or keywords
3. Search in current directory and subdirectories

### Keyboard Shortcuts

| Operation | Shortcut |
|-----------|----------|
| New File | Ctrl+N |
| Delete | Delete |
| Rename | F2 |
| Refresh | F5 |
| Search | Ctrl+F |
| Copy | Ctrl+C |
| Paste | Ctrl+V |
| Select All | Ctrl+A |

### Important Notes

#### File Operation Limitations
- Filenames cannot contain special characters: `\ / : * ? " < > |`
- Single file size limit: 10MB
- Filename length limit: 255 characters
- Directory depth limit: 10 levels

#### Security Tips
- Delete operations are irreversible, please operate carefully
- Save frequently when editing large files
- Regularly backup important files

#### Performance Optimization Tips
- Avoid storing too many files in a single directory (recommended less than 1000)
- Be patient when operating on large files
- Regularly clean up unnecessary files to free disk space