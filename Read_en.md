# Simple File Manager (JavaFX)

A lightweight file system simulation application built with Java and JavaFX, featuring a graphical user interface (GUI), multi-threaded operations, and core file system functionalities like directory management, file I/O, and disk space simulation.


## Overview
This project simulates a basic file system to demonstrate core concepts including:
- **Disk Simulation**: Emulates physical disk using file blocks and I/O operations.
- **File Allocation Table (FAT)**: Manages disk block allocation and file block chaining.
- **Multi-level Directory**: Supports nested directory structures (create/delete/navigate directories).
- **Graphical Interface**: Intuitive JavaFX UI for file/directory operations (no command-line required).
- **Multi-threading**: Background tasks for file read/write and buffer flushing to avoid UI freezes.


## Tech Stack
| Category            | Tools/Libraries                                    |
|---------------------|----------------------------------------------------|
| Programming         | Java 11+ (compatible with JavaFX)                  |
| GUI Framework       | JavaFX (for interactive interface)                 |
| Build Tool          | Maven/Gradle (optional, for dependency management) |
| Resource Management | FXML (UI layout), CSS (styling), PNG (icons)       |


## Project Structure
The project follows a **MVC (Model-View-Controller)** pattern and standard Java resource directory conventions:

```
src/
├── main/
│   ├── java/org/jiejiejiang/filemanager/  # Core logic & controllers
│   │   ├── core/          # Model: File system business logic
│   │   │   ├── Disk.java          # Simulate disk block I/O
│   │   │   ├── Buffer.java        # Buffer for disk read/write optimization
│   │   │   ├── FAT.java           # File Allocation Table
│   │   │   ├── FileEntry.java     # File metadata (name, size, start block)
│   │   │   ├── Directory.java     # Multi-level directory management
│   │   │   └── FileSystem.java    # Core API (create/delete/read/write files)
│   │   │
│   │   ├── gui/           # View & Controller
│   │   │   ├── FileManagerApp.java  # JavaFX app entry point
│   │   │   ├── controller/          # Controllers (link FXML & logic)
│   │   │   │   ├── MainController.java       # Main window controller
│   │   │   │   ├── NewFileDialogController.java  # New file dialog controller
│   │   │   │   └── ...
│   │   │   └── view/               # Reusable UI components (optional)
│   │   │
│   │   ├── thread/        # Multi-threaded tasks (avoid UI lag)
│   │   │   ├── FileReadTask.java   # Background file reading
│   │   │   ├── FileWriteTask.java  # Background file writing
│   │   │   └── BufferFlushTask.java # Auto-flush buffer to disk
│   │   │
│   │   ├── exception/     # Custom business exceptions
│   │   └── util/          # Utility tools (path, log, file size)
│   │
│   └── resources/org/jiejiejiang/filemanager/  # Non-code resources
│       ├── fxml/          # FXML UI layouts (match controllers)
│       ├── css/           # UI styling (main.css, dialogs.css)
│       ├── images/        # Icons (new file, folder, delete, etc.)
│       └── config/        # Config files (disk block size, window size)
```


## Features
### 1. File Operations
- **Create New Files**: Support basic text content through dialog input for filename and content.
- **File Viewing**: Double-click files or right-click "View" to safely view file content in read-only mode, preventing accidental modifications.
- **File Editing**: Right-click "Edit" to modify file content in dedicated editing dialog with save functionality.
- **Delete Files**: Automatically checks for read-only status; read-only files cannot be deleted.
- **File Renaming**: Right-click "Rename" to modify file names.
- **File Properties**: Right-click "Properties" to view detailed file information (size, creation time, disk location, etc.).

#### New Feature Highlights
- **Separated View and Edit**: Double-clicking files defaults to safe read-only viewing mode; editing requires explicit right-click menu selection.
- **Enhanced User Experience**: Prevents accidental file modifications while maintaining full editing capabilities.
- **Multiple Operation Methods**: Supports double-click, right-click menu, and menu bar operations.

### 2. Directory Operations
- Create nested directories.
- Delete empty directories.
- Navigate directory tree (via TreeView in UI).
- List directory contents (files + subdirectories).

### 3. Disk & Buffer Management
- Simulate disk with configurable block size and total blocks.
- Buffer cache to reduce direct disk I/O (improves performance).
- Auto-flush buffer to disk (via background thread).

### 4. User Interface
- **Intuitive Layout**: Directory tree (left) + file list (right) + operation toolbar.
- **Multi-View Modes**: Support both list view and icon view to accommodate different user preferences.
- **Smart Interaction Design**:
  - Double-click files: Default to read-only viewing mode
  - Double-click directories: Enter directory
  - Right-click menu: Complete operation options including view, edit, delete, properties
  - Menu bar: File operations, view switching, tool options
- **Dialog System**:
  - File viewing dialog: Read-only mode for safe content viewing
  - File editing dialog: Full editing functionality with save and cancel options
  - New file dialog: Create new files
  - Rename dialog: File/directory renaming
  - Properties dialog: View detailed information
- **Responsive Design**: Supports window resizing with automatic component layout adaptation.
- **File Type Icons**: Different icons for text files, image files, folders, etc., for better visibility.


## How to Run
### Prerequisites
- Java 11 or higher (JavaFX is included in JDK 11+; for older versions, add JavaFX SDK manually).
- Maven/Gradle (optional, for building; alternatively, use IDE run configurations).

### Steps
1. **Clone/Download the Project**  
   Save the project to your local machine.

2. **Configure JavaFX (if needed)**  
   If using JDK < 11, add the JavaFX SDK to your project dependencies and set the `--module-path` and `--add-modules` VM options:
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```

3. **Run the Application**
    - **IDE (IntelliJ/Eclipse)**:  
      Locate `FileManagerApp.java` (under `gui/`), right-click → "Run" (ensure FXML/resources are in the classpath).
    - **Maven**:  
      Add a Maven plugin for JavaFX (e.g., `javafx-maven-plugin`) and run:
      ```bash
      mvn clean javafx:run
      ```


## Configuration
Adjust project behavior via config files in `src/main/resources/org/jiejiejiang/filemanager/config/`:
- **disk.properties**:
    - `disk.block.size`: Size of each disk block (default: 1024 bytes).
    - `disk.total.blocks`: Total number of blocks in the simulated disk (default: 1024).
- **ui-config.properties**:
    - `window.width`: Default width of the main window (default: 800).
    - `window.height`: Default height of the main window (default: 600).


## Notes for Development
- **Controller-FXML Binding**: Each FXML file must specify its controller via `fx:controller` (e.g., `fx:controller="org.jiejiejiang.filemanager.gui.controller.MainController"`).
- **Thread Safety**: Use JavaFX `Task` or `Platform.runLater()` when updating UI from background threads (JavaFX UI is single-threaded).
- **Testing**: Test core logic (e.g., `FileSystem`, `FAT`) independently first, then test UI interactions.


## Documentation Resources

### Detailed Documentation
- **[Architecture Documentation](./doc/architecture.md)**: System design and architecture details
- **[API Reference](./doc/api-reference.md)**: Detailed description of core classes and methods
- **[User Manual](./doc/user-manual.md)**: Complete user guide
- **[Development Guide](./doc/development-guide.md)**: Development environment setup and extension guide

### Quick Links
- 🚀 [Quick Start](./doc/user-manual.md#quick-start-快速开始)
- 📖 [User Interface Guide](./doc/user-manual.md#interface-overview-界面介绍)
- 🔧 [Development Setup](./doc/development-guide.md#development-environment-开发环境搭建)
- 📚 [API Reference](./doc/api-reference.md#core-api-interfaces-核心api接口)

## License
This project is for educational purposes (e.g., course design). Feel free to modify and extend it.