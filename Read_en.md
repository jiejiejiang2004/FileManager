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
- Create new files (supports basic text content).
- Delete files (checks for read-only status).
- Read/write file content (background threads for large files).
- Modify file attributes (e.g., set read-only).

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
- Intuitive layout: Directory tree (left) + file list (right) + operation toolbar.
- Interactive dialogs (new file, rename, error prompts).
- Responsive design (supports window resizing).
- File type icons (text, image, folder) for better visibility.


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


## License
This project is for educational purposes (e.g., course design). Feel free to modify and extend it.