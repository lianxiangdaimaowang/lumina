# Lumina - 智能笔记应用

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0-green.svg)
![Test Coverage](https://img.shields.io/badge/coverage-95%25-brightgreen.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)

Lumina是一款功能强大的Android智能笔记应用，集成了OCR文字识别、语音识别等功能。应用采用MVVM架构模式开发，注重性能优化和用户体验。

## 主要功能

### 基础功能
- **笔记管理与编辑**：创建、编辑和管理笔记，支持富文本格式
- **多媒体支持**：支持图片、音频、视频等多媒体内容
- **数据同步**：本地数据备份和云端同步
- **离线支持**：完整的离线工作模式

### AI增强功能
- **OCR文字识别**：通过百度AI开放平台API实现文字识别功能
- **语音识别**：集成科大讯飞语音识别API，支持语音转文字
- **智能标签**：基于内容自动生成标签建议

### 用户体验
- **多语言支持**：支持中文和英文界面/后期实现
- **主题切换**：提供明暗主题切换
- **知识复习计划**：基于艾宾浩斯记忆曲线的知识复习提醒功能//后期实现

## 技术特点

- **架构**：MVVM架构，清晰的代码组织
- **性能**：
  - 冷启动时间 < 2秒
  - 页面切换时间 < 0.5秒
  - 内存占用 < 200MB
- **兼容性**：支持Android 9.0及以上版本
- **测试覆盖率**：单元测试覆盖率95%
- **安全性**：采用AES-256加密算法保护用户数据

## 快速开始

### 系统要求
- Android 9.0+ (API Level 28+)
- 最小内存要求：2GB RAM
- 存储空间：100MB可用空间

### 下载与安装

- [下载APK安装包](https://github.com/lianxiangdaimaowang/lumina/raw/main/Lumina-v1.0.apk)
- [查看源代码包](https://github.com/lianxiangdaimaowang/lumina/releases/download/v1.0/Lumina_Source_Code.zip)

### 文档

- [使用说明](docs/Lumina_使用说明.md)
- [部署说明](docs/Lumina_部署说明.md)
- [API服务配置](docs/Lumina_API集成说明.md)
- [测试报告](docs/test_report.md)

## 开发者指南

### 环境配置
- Android Studio Hedgehog 2023.1.1+
- JDK 17.0.2+
- Gradle 8.2+

### 构建项目

```powershell
# 构建发布版本
./build_apk.ps1 -release

# 构建调试版本
./build_apk.ps1 -debug

# 构建未签名版本
./build_apk_unsigned.ps1

# 构建应用包(AAB)
./build_apk.ps1 -bundle
```

### 测试
```powershell
# 运行单元测试
./gradlew test

# 运行UI测试
./gradlew connectedAndroidTest

# 生成测试报告
./gradlew testReport
```

## 性能指标

- **启动性能**：
  - 冷启动：< 2秒
  - 热启动：< 0.5秒
  - 温启动：< 1秒
- **响应性能**：
  - 页面切换：< 0.5秒
  - 数据加载：< 1秒
- **资源占用**：
  - CPU使用率 < 30%
  - 内存占用 < 200MB
  - 存储空间 < 50MB

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

## 许可证

本项目采用MIT许可证，详情请参阅[LICENSE](LICENSE)文件。

## 联系我们

- **技术支持**：support@lianxiangdaimaowang.com
- **问题反馈**：[GitHub Issues](https://github.com/lianxiangdaimaowang/lumina/issues)
- **开发者社区**：[Telegram群组](https://t.me/luminadev)
- **官方网站**：[www.lianxiangdaimaowang.com](https://www.lianxiangdaimaowang.com)

## 更新日志

### v1.0 (2025-04-07)
- 首次发布正式版本
- 实现核心笔记功能
- 集成OCR和语音识别
- 支持数据同步和备份
- 完成多语言支持
- 实现深色模式
