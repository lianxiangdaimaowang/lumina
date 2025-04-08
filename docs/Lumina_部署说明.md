# Lumina 软件部署说明

## 快速部署链接

### 下载渠道

| 内容 | 链接 | 备注 |
|------|------|------|
| 源码包 | [GitHub](https://github.com/lianxiangdaimaowang/lumina/releases/download/v1.0/Lumina_Source_Code.zip) | 完整源代码 |
| APK安装包 | [GitHub](https://github.com/lianxiangdaimaowang/lumina/releases/download/v1.0/Lumina-v1.0.apk) | Android应用安装包 |

## 项目部署

### Android设备部署

1. **直接安装APK**：
   - 下载APK到Android设备
   - 点击APK文件安装（需要允许安装未知来源应用）
   - 安装完成后点击图标打开应用

2. **使用ADB安装**：
   ```bash
   adb install Lumina-v1.0.apk
   ```

### 开发环境配置

要开发和构建Lumina项目，您需要：

1. **Android Studio**：[下载最新版本](https://developer.android.com/studio)
2. **JDK 11**：[下载链接](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)
3. **Gradle 8.0+**：Android Studio会自动处理

### 代码仓库部署

```bash
# 克隆代码仓库
git clone https://github.com/lianxiangdaimaowang/lumina.git

# 进入项目目录
cd lumina

# 构建项目
./gradlew build
```

## 快速构建

项目提供了PowerShell脚本用于快速构建APK：

```powershell
# 查看帮助
.\build_apk.ps1 -help

# 构建调试版本
.\build_apk.ps1 -debug

# 构建发布版本
.\build_apk.ps1 -release

# 构建应用包(AAB)
.\build_apk.ps1 -bundle
```

## API服务配置

项目使用的API服务需要单独申请并配置：

1. **百度OCR API**：
   - 注册百度开发者账号：[百度AI开放平台](https://ai.baidu.com/)
   - 创建OCR应用并获取API密钥
   - 更新 `BaiduOcrManager.java` 中的API密钥

2. **讯飞语音识别API**：
   - 注册讯飞开发者账号：[讯飞开放平台](https://www.xfyun.cn/)
   - 创建语音识别应用并获取API密钥
   - 更新 `IflytekVoiceManager.java` 中的API密钥

## 服务器部署//未来实现

对于企业版本，需要配置以下服务器组件：

1. **API网关**：管理API密钥和请求路由
2. **数据同步服务**：处理云端数据同步
3. **用户认证服务**：处理用户登录和权限

详细的服务器部署文档请参阅：[服务器部署指南](https://github.com/lianxiangdaimaowang/lumina/wiki/server-deployment)

## 技术支持与联系方式

如有任何问题或需要技术支持，请通过以下方式联系：

- **邮箱**：support@lianxiangdaimaowang.com
- **问题跟踪**：[GitHub Issues](https://github.com/lianxiangdaimaowang/lumina/issues)
- **开发者社区**：[Telegram群组](https://t.me/luminadev)

## 更新历史

### v1.0 (2025-04-07)
- 首次发布版本
- 支持OCR文字识别
- 支持语音识别
- 支持笔记编辑与管理 
