# 配置管理系统技术文档 / Configuration Management System Technical Documentation

## 中文概述 / Chinese Overview

配置管理系统负责处理应用程序的各种配置参数，包括磁盘配置、UI配置等。系统采用基于Properties文件的配置方式，支持配置验证、默认值设置和运行时配置加载。

## English Overview

The Configuration Management System handles various application configuration parameters, including disk configuration, UI configuration, etc. The system uses Properties file-based configuration with support for configuration validation, default value setting, and runtime configuration loading.

---

## 架构设计 / Architecture Design

### 配置系统层次结构 / Configuration System Hierarchy

```
配置管理系统 (Configuration Management System)
├── 配置文件 (Configuration Files)
│   ├── disk.properties          # 磁盘配置
│   └── ui-config.properties     # UI配置 (规划中)
├── 配置加载器 (Configuration Loaders)
│   ├── Disk.loadConfig()        # 磁盘配置加载
│   └── 其他配置加载器 (Other Loaders)
└── 配置验证器 (Configuration Validators)
    ├── 参数范围验证 (Parameter Range Validation)
    ├── 类型验证 (Type Validation)
    └── 依赖验证 (Dependency Validation)
```

### 设计原则 / Design Principles

1. **配置分离** (Configuration Separation)：不同模块的配置独立管理
2. **验证优先** (Validation First)：所有配置参数都需要验证
3. **默认值支持** (Default Value Support)：提供合理的默认配置
4. **异常处理** (Exception Handling)：配置错误时的优雅降级

---

## 核心配置文件 / Core Configuration Files

### 1. 磁盘配置 (disk.properties)

#### 文件位置 / File Location
```
src/main/resources/org/jiejiejiang/filemanager/config/disk.properties
```

#### 配置参数 / Configuration Parameters

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| `disk.block.size` | int | 512 | 磁盘块大小（字节） |
| `disk.total.blocks` | int | 1024 | 磁盘总块数 |
| `disk.file.path` | String | "./data/disk.img" | 磁盘文件路径 |

#### 配置文件示例 / Configuration File Example
```properties
# 磁盘块大小（字节）
disk.block.size=512

# 磁盘总块数
disk.total.blocks=1024

# 磁盘文件路径（可选，有默认值）
# disk.file.path=./data/disk.img
```

### 2. UI配置 (ui-config.properties) - 规划中

#### 预期配置参数 / Expected Configuration Parameters
```properties
# 窗口配置
window.width=1200
window.height=800
window.resizable=true

# 主题配置
theme.name=default
theme.dark.mode=false

# 视图配置
default.view.mode=list
show.hidden.files=false
```

---

## 配置加载机制 / Configuration Loading Mechanism

### 1. Disk配置加载器 / Disk Configuration Loader

#### 类声明 / Class Declaration
```java
// 位于 Disk.java 中的配置加载方法
private void loadConfig(String configPath) throws DiskInitializeException
```

#### 核心特性 / Core Features

1. **Properties文件解析** (Properties File Parsing)
   - 使用 `java.util.Properties` 解析配置文件
   - 支持注释和空行处理

2. **参数验证** (Parameter Validation)
   - 块大小必须为正数且为2的幂
   - 总块数必须为正数
   - 文件路径格式验证

3. **默认值处理** (Default Value Handling)
   - 提供合理的默认配置值
   - 缺失参数时使用默认值

4. **异常处理** (Exception Handling)
   - 配置文件不存在时的处理
   - 参数格式错误时的处理
   - 参数值超出范围时的处理

#### 配置加载流程 / Configuration Loading Process

```java
/**
 * 磁盘配置加载示例
 */
public class DiskConfigurationExample {
    
    public void loadDiskConfiguration() {
        try {
            // 1. 创建Properties对象
            Properties props = new Properties();
            
            // 2. 加载配置文件
            try (InputStream input = new FileInputStream(configPath)) {
                props.load(input);
            }
            
            // 3. 读取并验证配置参数
            String blockSizeStr = props.getProperty("disk.block.size", "512");
            int blockSize = Integer.parseInt(blockSizeStr);
            
            // 4. 参数验证
            if (blockSize <= 0 || (blockSize & (blockSize - 1)) != 0) {
                throw new DiskInitializeException("块大小必须为正数且为2的幂");
            }
            
            // 5. 应用配置
            this.blockSize = blockSize;
            
        } catch (IOException e) {
            throw new DiskInitializeException("配置文件加载失败: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new DiskInitializeException("配置参数格式错误: " + e.getMessage(), e);
        }
    }
}
```

### 2. 配置验证器 / Configuration Validators

#### 块大小验证 / Block Size Validation
```java
/**
 * 验证块大小是否有效
 * @param blockSize 块大小
 * @return 是否有效
 */
private boolean isValidBlockSize(int blockSize) {
    // 必须为正数且为2的幂
    return blockSize > 0 && (blockSize & (blockSize - 1)) == 0;
}
```

#### 路径验证 / Path Validation
```java
/**
 * 验证文件路径是否有效
 * @param filePath 文件路径
 * @return 是否有效
 */
private boolean isValidFilePath(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
        return false;
    }
    
    try {
        Path path = Paths.get(filePath);
        // 检查父目录是否存在或可创建
        Path parent = path.getParent();
        return parent == null || Files.exists(parent) || parent.toFile().mkdirs();
    } catch (InvalidPathException e) {
        return false;
    }
}
```

---

## 配置管理最佳实践 / Configuration Management Best Practices

### 1. 配置文件组织 / Configuration File Organization

```
src/main/resources/org/jiejiejiang/filemanager/config/
├── disk.properties          # 磁盘相关配置
├── ui-config.properties     # UI相关配置
├── logging.properties       # 日志相关配置
└── application.properties   # 应用程序全局配置
```

### 2. 配置参数命名规范 / Configuration Parameter Naming Convention

- **模块前缀** (Module Prefix)：使用模块名作为前缀，如 `disk.`、`ui.`
- **层次结构** (Hierarchical Structure)：使用点号分隔层次，如 `disk.block.size`
- **描述性命名** (Descriptive Naming)：参数名应清晰描述其用途

### 3. 默认值策略 / Default Value Strategy

```java
/**
 * 配置默认值管理
 */
public class ConfigurationDefaults {
    
    // 磁盘配置默认值
    public static final int DEFAULT_BLOCK_SIZE = 512;
    public static final int DEFAULT_TOTAL_BLOCKS = 1024;
    public static final String DEFAULT_DISK_PATH = "./data/disk.img";
    
    // UI配置默认值
    public static final int DEFAULT_WINDOW_WIDTH = 1200;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    public static final boolean DEFAULT_RESIZABLE = true;
    
    /**
     * 获取配置值，如果不存在则返回默认值
     */
    public static int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LogUtil.warn("配置参数格式错误，使用默认值: " + key + " = " + defaultValue);
            return defaultValue;
        }
    }
}
```

### 4. 配置热重载 / Configuration Hot Reload

```java
/**
 * 配置热重载支持
 */
public class ConfigurationManager {
    
    private final Map<String, Properties> configCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastModified = new ConcurrentHashMap<>();
    
    /**
     * 检查配置文件是否需要重新加载
     */
    public boolean needsReload(String configPath) {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            return false;
        }
        
        long currentModified = configFile.lastModified();
        Long cachedModified = lastModified.get(configPath);
        
        return cachedModified == null || currentModified > cachedModified;
    }
    
    /**
     * 重新加载配置文件
     */
    public Properties reloadConfig(String configPath) throws IOException {
        Properties props = new Properties();
        
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        }
        
        configCache.put(configPath, props);
        lastModified.put(configPath, new File(configPath).lastModified());
        
        return props;
    }
}
```

---

## 配置异常处理 / Configuration Exception Handling

### 1. 配置异常类型 / Configuration Exception Types

```java
/**
 * 配置相关异常
 */
public class ConfigurationException extends Exception {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * 配置文件不存在异常
 */
public class ConfigurationFileNotFoundException extends ConfigurationException {
    
    public ConfigurationFileNotFoundException(String configPath) {
        super("配置文件不存在: " + configPath);
    }
}

/**
 * 配置参数无效异常
 */
public class InvalidConfigurationException extends ConfigurationException {
    
    public InvalidConfigurationException(String parameter, String value, String reason) {
        super(String.format("配置参数无效: %s = %s, 原因: %s", parameter, value, reason));
    }
}
```

### 2. 异常处理策略 / Exception Handling Strategy

```java
/**
 * 配置加载异常处理示例
 */
public class ConfigurationLoader {
    
    public Properties loadConfigurationSafely(String configPath) {
        try {
            return loadConfiguration(configPath);
        } catch (ConfigurationFileNotFoundException e) {
            LogUtil.warn("配置文件不存在，使用默认配置: " + configPath);
            return createDefaultConfiguration();
        } catch (InvalidConfigurationException e) {
            LogUtil.error("配置参数无效: " + e.getMessage());
            return createDefaultConfiguration();
        } catch (Exception e) {
            LogUtil.error("配置加载失败，使用默认配置: " + e.getMessage());
            return createDefaultConfiguration();
        }
    }
    
    private Properties createDefaultConfiguration() {
        Properties defaults = new Properties();
        defaults.setProperty("disk.block.size", "512");
        defaults.setProperty("disk.total.blocks", "1024");
        defaults.setProperty("disk.file.path", "./data/disk.img");
        return defaults;
    }
}
```

---

## 使用示例 / Usage Examples

### 1. 基本配置加载 / Basic Configuration Loading

```java
/**
 * 基本配置加载示例
 */
public class BasicConfigurationExample {
    
    public static void main(String[] args) {
        try {
            // 加载磁盘配置
            String configPath = "src/main/resources/org/jiejiejiang/filemanager/config/disk.properties";
            Properties diskConfig = loadDiskConfiguration(configPath);
            
            // 读取配置参数
            int blockSize = Integer.parseInt(diskConfig.getProperty("disk.block.size", "512"));
            int totalBlocks = Integer.parseInt(diskConfig.getProperty("disk.total.blocks", "1024"));
            String diskPath = diskConfig.getProperty("disk.file.path", "./data/disk.img");
            
            System.out.println("磁盘配置加载成功:");
            System.out.println("  块大小: " + blockSize + " 字节");
            System.out.println("  总块数: " + totalBlocks);
            System.out.println("  磁盘路径: " + diskPath);
            
        } catch (Exception e) {
            System.err.println("配置加载失败: " + e.getMessage());
        }
    }
    
    private static Properties loadDiskConfiguration(String configPath) throws IOException {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        }
        return props;
    }
}
```

### 2. 配置验证示例 / Configuration Validation Example

```java
/**
 * 配置验证示例
 */
public class ConfigurationValidationExample {
    
    public void validateDiskConfiguration(Properties config) throws InvalidConfigurationException {
        // 验证块大小
        String blockSizeStr = config.getProperty("disk.block.size");
        if (blockSizeStr != null) {
            try {
                int blockSize = Integer.parseInt(blockSizeStr);
                if (blockSize <= 0) {
                    throw new InvalidConfigurationException("disk.block.size", blockSizeStr, "必须为正数");
                }
                if ((blockSize & (blockSize - 1)) != 0) {
                    throw new InvalidConfigurationException("disk.block.size", blockSizeStr, "必须为2的幂");
                }
            } catch (NumberFormatException e) {
                throw new InvalidConfigurationException("disk.block.size", blockSizeStr, "必须为整数");
            }
        }
        
        // 验证总块数
        String totalBlocksStr = config.getProperty("disk.total.blocks");
        if (totalBlocksStr != null) {
            try {
                int totalBlocks = Integer.parseInt(totalBlocksStr);
                if (totalBlocks <= 0) {
                    throw new InvalidConfigurationException("disk.total.blocks", totalBlocksStr, "必须为正数");
                }
                if (totalBlocks > 100000) {
                    throw new InvalidConfigurationException("disk.total.blocks", totalBlocksStr, "不能超过100000");
                }
            } catch (NumberFormatException e) {
                throw new InvalidConfigurationException("disk.total.blocks", totalBlocksStr, "必须为整数");
            }
        }
        
        // 验证磁盘路径
        String diskPath = config.getProperty("disk.file.path");
        if (diskPath != null && !isValidPath(diskPath)) {
            throw new InvalidConfigurationException("disk.file.path", diskPath, "路径格式无效");
        }
    }
    
    private boolean isValidPath(String path) {
        try {
            Paths.get(path);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
```

### 3. 配置管理器示例 / Configuration Manager Example

```java
/**
 * 配置管理器示例
 */
public class ConfigurationManagerExample {
    
    private final Map<String, Properties> configurations = new HashMap<>();
    
    /**
     * 初始化所有配置
     */
    public void initializeConfigurations() {
        try {
            // 加载磁盘配置
            Properties diskConfig = loadConfiguration("disk.properties");
            configurations.put("disk", diskConfig);
            
            // 加载UI配置
            Properties uiConfig = loadConfiguration("ui-config.properties");
            configurations.put("ui", uiConfig);
            
            LogUtil.info("所有配置加载完成");
            
        } catch (Exception e) {
            LogUtil.error("配置初始化失败: " + e.getMessage());
            throw new RuntimeException("配置初始化失败", e);
        }
    }
    
    /**
     * 获取配置值
     */
    public String getConfigValue(String module, String key, String defaultValue) {
        Properties config = configurations.get(module);
        if (config == null) {
            LogUtil.warn("配置模块不存在: " + module);
            return defaultValue;
        }
        
        return config.getProperty(key, defaultValue);
    }
    
    /**
     * 获取整数配置值
     */
    public int getIntConfigValue(String module, String key, int defaultValue) {
        String value = getConfigValue(module, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LogUtil.warn("配置值格式错误，使用默认值: " + module + "." + key + " = " + defaultValue);
            return defaultValue;
        }
    }
    
    private Properties loadConfiguration(String fileName) throws IOException {
        String configPath = "src/main/resources/org/jiejiejiang/filemanager/config/" + fileName;
        Properties props = new Properties();
        
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        } catch (FileNotFoundException e) {
            LogUtil.warn("配置文件不存在，使用默认配置: " + fileName);
            return new Properties(); // 返回空配置，使用默认值
        }
        
        return props;
    }
}
```

---

## 性能优化 / Performance Optimizations

### 1. 配置缓存 / Configuration Caching

```java
/**
 * 配置缓存机制
 */
public class ConfigurationCache {
    
    private static final Map<String, Properties> cache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5分钟过期
    
    /**
     * 获取缓存的配置
     */
    public static Properties getCachedConfiguration(String configPath) {
        Long timestamp = cacheTimestamps.get(configPath);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY_MS) {
            return cache.get(configPath);
        }
        return null;
    }
    
    /**
     * 缓存配置
     */
    public static void cacheConfiguration(String configPath, Properties config) {
        cache.put(configPath, config);
        cacheTimestamps.put(configPath, System.currentTimeMillis());
    }
    
    /**
     * 清除过期缓存
     */
    public static void clearExpiredCache() {
        long currentTime = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() >= CACHE_EXPIRY_MS) {
                cache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
```

### 2. 懒加载配置 / Lazy Configuration Loading

```java
/**
 * 懒加载配置管理器
 */
public class LazyConfigurationManager {
    
    private volatile Properties diskConfig;
    private volatile Properties uiConfig;
    
    /**
     * 懒加载磁盘配置
     */
    public Properties getDiskConfiguration() {
        if (diskConfig == null) {
            synchronized (this) {
                if (diskConfig == null) {
                    diskConfig = loadDiskConfiguration();
                }
            }
        }
        return diskConfig;
    }
    
    /**
     * 懒加载UI配置
     */
    public Properties getUIConfiguration() {
        if (uiConfig == null) {
            synchronized (this) {
                if (uiConfig == null) {
                    uiConfig = loadUIConfiguration();
                }
            }
        }
        return uiConfig;
    }
    
    private Properties loadDiskConfiguration() {
        try {
            return loadConfiguration("disk.properties");
        } catch (Exception e) {
            LogUtil.error("磁盘配置加载失败: " + e.getMessage());
            return createDefaultDiskConfiguration();
        }
    }
    
    private Properties loadUIConfiguration() {
        try {
            return loadConfiguration("ui-config.properties");
        } catch (Exception e) {
            LogUtil.error("UI配置加载失败: " + e.getMessage());
            return createDefaultUIConfiguration();
        }
    }
}
```

---

## 测试建议 / Testing Recommendations

### 1. 单元测试 / Unit Testing

```java
/**
 * 配置管理单元测试
 */
public class ConfigurationManagerTest {
    
    @Test
    public void testLoadValidConfiguration() {
        // 测试加载有效配置
        Properties config = new Properties();
        config.setProperty("disk.block.size", "512");
        config.setProperty("disk.total.blocks", "1024");
        
        // 验证配置加载正确
        assertEquals("512", config.getProperty("disk.block.size"));
        assertEquals("1024", config.getProperty("disk.total.blocks"));
    }
    
    @Test
    public void testConfigurationValidation() {
        // 测试配置验证
        Properties invalidConfig = new Properties();
        invalidConfig.setProperty("disk.block.size", "0"); // 无效值
        
        assertThrows(InvalidConfigurationException.class, () -> {
            validateConfiguration(invalidConfig);
        });
    }
    
    @Test
    public void testDefaultValues() {
        // 测试默认值
        Properties emptyConfig = new Properties();
        
        int blockSize = getIntProperty(emptyConfig, "disk.block.size", 512);
        assertEquals(512, blockSize);
    }
    
    @Test
    public void testConfigurationFileNotFound() {
        // 测试配置文件不存在的情况
        assertThrows(ConfigurationFileNotFoundException.class, () -> {
            loadConfiguration("nonexistent.properties");
        });
    }
}
```

### 2. 集成测试 / Integration Testing

```java
/**
 * 配置管理集成测试
 */
public class ConfigurationIntegrationTest {
    
    @Test
    public void testDiskInitializationWithConfiguration() {
        // 测试使用配置初始化磁盘
        String configPath = createTestConfigFile();
        
        try {
            Disk disk = new Disk(configPath);
            
            // 验证磁盘使用了正确的配置
            assertEquals(512, disk.getBlockSize());
            assertEquals(1024, disk.getTotalBlocks());
            
        } finally {
            deleteTestConfigFile(configPath);
        }
    }
    
    @Test
    public void testConfigurationReload() {
        // 测试配置重新加载
        String configPath = createTestConfigFile();
        
        try {
            // 初始加载
            Properties config1 = loadConfiguration(configPath);
            assertEquals("512", config1.getProperty("disk.block.size"));
            
            // 修改配置文件
            updateTestConfigFile(configPath, "disk.block.size", "1024");
            
            // 重新加载
            Properties config2 = loadConfiguration(configPath);
            assertEquals("1024", config2.getProperty("disk.block.size"));
            
        } finally {
            deleteTestConfigFile(configPath);
        }
    }
    
    private String createTestConfigFile() {
        // 创建测试配置文件的实现
        return "test-config.properties";
    }
    
    private void updateTestConfigFile(String path, String key, String value) {
        // 更新测试配置文件的实现
    }
    
    private void deleteTestConfigFile(String path) {
        // 删除测试配置文件的实现
    }
}
```

---

## 扩展建议 / Extension Recommendations

### 1. 功能扩展 / Feature Extensions

1. **配置加密** (Configuration Encryption)
   - 支持敏感配置的加密存储
   - 提供配置解密机制

2. **配置版本管理** (Configuration Versioning)
   - 支持配置文件版本控制
   - 提供配置迁移机制

3. **远程配置** (Remote Configuration)
   - 支持从远程服务器加载配置
   - 提供配置同步机制

4. **配置UI** (Configuration UI)
   - 提供图形化配置编辑界面
   - 支持配置实时预览

### 2. 性能优化 / Performance Optimizations

1. **配置预编译** (Configuration Precompilation)
   - 将配置编译为Java类
   - 提高配置访问性能

2. **配置分片** (Configuration Sharding)
   - 将大配置文件分片存储
   - 支持按需加载配置片段

3. **配置压缩** (Configuration Compression)
   - 压缩配置文件存储
   - 减少内存占用

---

## 依赖关系 / Dependencies

### 内部依赖 / Internal Dependencies

- `org.jiejiejiang.filemanager.exception.DiskInitializeException`: 磁盘初始化异常
- `org.jiejiejiang.filemanager.util.LogUtil`: 日志工具类
- `org.jiejiejiang.filemanager.util.PathUtil`: 路径工具类

### 外部依赖 / External Dependencies

- `java.util.Properties`: 配置文件解析
- `java.io.*`: 文件I/O操作
- `java.nio.file.*`: 现代文件操作API
- `java.util.concurrent.*`: 并发工具类

### 被依赖关系 / Dependent Classes

- `Disk`: 磁盘类依赖配置管理
- `FileManagerApp`: 应用程序入口依赖配置
- 其他需要配置的组件

---

**文档版本 / Document Version:** 1.0  
**最后更新 / Last Updated:** 2024-12-19  
**维护者 / Maintainer:** FileManager Development Team