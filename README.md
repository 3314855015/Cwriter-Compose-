# CWriter - 创作助手

一款专为中文写作设计的 Android 应用，帮助作者管理作品、章节和伏笔。

## 项目截图

<!-- 用户自行添加 -->

## 技术栈

| 技术 | 说明 |
|------|------|
| **Kotlin** | 主要开发语言 |
| **Jetpack Compose** | 现代 Android UI 工具包 |
| **MVVM** | 架构模式，分离视图与业务逻辑 |
| **Kotlin Coroutines** | 协程，用于异步数据处理（待集成） |
| **本地存储** | 基于文件系统的本地数据持久化 |

### 依赖库

- **Jetpack Compose** - UI 框架
- **Material 3** - 设计系统
- **Navigation Compose** - 页面导航
- **Lifecycle ViewModel** - 状态管理
- **Material Icons Extended** - 扩展图标库

## 核心功能

<!-- 用户自行添加 -->

## 如何运行

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11 或更高版本
- Android SDK (minSdk 24, targetSdk 36)
- Gradle 8.x

### 运行步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd Cwriter
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目根目录
   - 等待 Gradle 同步完成

3. **连接设备**
   - 连接 Android 手机（开启 USB 调试）
   - 或启动 Android 模拟器

4. **运行应用**
   - 点击 Android Studio 工具栏的运行按钮
   - 或使用快捷键 `Shift + F10`

### 构建 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/` 目录。

## 项目结构

```
app/src/main/java/com/cwriter/
├── CwriterApp.kt          # Application 类
├── data/                   # 数据层
│   └── model/             # 数据模型
├── ui/                     # UI 层
│   ├── components/        # 可复用组件
│   ├── screen/            # 页面
│   └── theme/             # 主题配置
└── navigation/            # 导航配置
```

## 许可证

MIT License
