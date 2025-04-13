# Lumina 软件部署说明

## 系统要求

### 客户端要求
- Android 9.0+ (API Level 24+)
- 2GB RAM以上
- 100MB可用存储空间
- 网络连接（支持离线使用部分功能）

### 服务器要求
- Java 11或更高版本
- MySQL 8.0或更高版本
- 4GB RAM以上（推荐）
- 20GB可用存储空间

## 客户端部署

### 下载渠道

| 内容 | 链接 | 备注 |
|------|------|------|
| 源码包 | [GitHub](https://github.com/lianxiangdaimaowang/lumina/releases/download/v1.0/Lumina_Source_Code.zip) | 完整源代码 |
| APK安装包 | [GitHub](https://github.com/lianxiangdaimaowang/lumina/releases/download/v1.0/Lumina-v1.0.apk) | Android应用安装包 |

### 安装步骤

1. **直接安装APK**：
   - 下载APK到Android设备
   - 点击APK文件安装（需要允许安装未知来源应用）
   - 安装完成后点击图标打开应用

2. **使用ADB安装**：
   ```bash
   adb install Lumina-v1.0.apk
   ```

## 开发环境配置

### Android客户端开发环境

1. **Android Studio**：
   - 版本：Hedgehog 2023.1.1+
   - [下载链接](https://developer.android.com/studio)
   - 推荐配置：
     - 8GB RAM以上
     - SSD存储
     - Intel i5/AMD Ryzen 5或更高处理器

2. **JDK要求**：
   - 版本：JDK 11
   - [下载链接](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)
   - 环境变量配置：
     ```
     JAVA_HOME=<JDK安装路径>
     PATH=%JAVA_HOME%\bin;%PATH%
     ```

3. **Gradle配置**：
   - 版本：Gradle 8.0+
   - Android Studio会自动处理
   - 配置文件：app/build.gradle.kts

### 服务器开发环境

1. **Java环境**：
   - JDK 11+
   - Maven 3.8+
   - 配置文件：server/pom.xml

2. **数据库**：
   - MySQL 8.0+
   - 创建数据库：lumina
   - 字符集：utf8mb4
   - 排序规则：utf8mb4_unicode_ci

3. **环境变量**：
   ```
   LUMINA_DB_PASSWORD=<数据库密码>
   JWT_SECRET=<JWT密钥>
   ```

## 项目构建

### Android客户端构建

1. **克隆代码**：
   ```bash
   git clone https://github.com/lianxiangdaimaowang/lumina.git
   cd lumina
   ```

2. **构建命令**：
   ```powershell
   # 构建调试版本
   ./build_apk.ps1 -debug

   # 构建发布版本
   ./build_apk.ps1 -release

   # 构建应用包(AAB)
   ./build_apk.ps1 -bundle
   ```

### 服务器构建

1. **构建项目**：
   ```bash
   cd server
   mvn clean package
   ```

2. **运行服务器**：
   ```bash
   java -jar target/lumina-server-1.0-SNAPSHOT.jar
   ```

## API服务配置

### 百度OCR API

1. **申请步骤**：
   - 注册百度开发者账号：[百度AI开放平台](https://ai.baidu.com/)
   - 创建OCR应用并获取API密钥
   - 更新 `BaiduOcrManager.java` 中的API密钥
   - 每日免费额度：500次

2. **配置文件位置**：
   ```
   app/src/main/java/com/lianxiangdaimaowang/lumina/ocr/BaiduOcrManager.java
   ```

### 讯飞语音识别API

1. **申请步骤**：
   - 注册讯飞开发者账号：[讯飞开放平台](https://www.xfyun.cn/)
   - 创建语音识别应用并获取API密钥
   - 更新 `IflytekVoiceManager.java` 中的API密钥
   - 每日免费额度：500次

2. **配置文件位置**：
   ```
   app/src/main/java/com/lianxiangdaimaowang/lumina/voice/IflytekVoiceManager.java
   ```

## 服务器部署

### 基础部署

1. **环境准备**：
   - 安装JDK 11+
   - 安装MySQL 8.0+
   - 配置环境变量

2. **数据库配置**：
   - 创建数据库和用户
   - 导入初始数据
   - 配置数据库连接

3. **应用部署**：
   - 上传jar包
   - 配置application.yml
   - 启动应用

### 监控配置

1. **系统监控**：
   - 运行 `setup_monitoring.bat`
   - 配置Prometheus
   - 设置告警规则

2. **日志管理**：
   - 运行 `rotate_logs.bat`
   - 配置日志轮转
   - 设置日志保留策略

3. **备份策略**：
   - 运行 `backup_script.bat`
   - 配置自动备份
   - 设置备份保留策略

## 安全配置

1. **客户端安全**：
   - 启用ProGuard混淆
   - 实现SSL Pinning
   - 加密本地存储

2. **服务器安全**：
   - 配置防火墙规则
   - 启用HTTPS
   - 实施访问控制

3. **数据安全**：
   - 加密敏感数据
   - 实现数据备份
   - 配置数据清理策略

## 技术支持

- **邮箱**：support@lianxiangdaimaowang.com
- **问题跟踪**：[GitHub Issues](https://github.com/lianxiangdaimaowang/lumina/issues)
- **开发者社区**：[Telegram群组](https://t.me/luminadev)

## 更新历史

### v1.0 (2025-04-07)
- 首次发布版本
- 支持Android 9.0+
- 完整的笔记管理功能
- OCR和语音识别集成
- 服务器端基础功能实现 
