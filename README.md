# Lumina - 智能笔记应用

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0-green.svg)

Lumina是一款功能强大的Android智能笔记应用，集成了OCR文字识别、语音识别、知识复习计划等功能。

## 主要功能

- **笔记管理与编辑**：创建、编辑和管理笔记，支持富文本格式
- **OCR文字识别**：通过百度AI开放平台API实现文字识别功能
- **语音识别**：集成科大讯飞语音识别API，支持语音转文字
- **多语言支持**：支持中文和英文界面
- **主题切换**：提供明暗主题切换
- **知识复习计划**：基于艾宾浩斯记忆曲线的知识复习提醒功能

## 快速开始

### 下载与安装

- [下载APK安装包](https://github.com/lianxiangdaimaowang/lumina/raw/main/Lumina-v1.0.apk)
- [查看源代码包](https://github.com/lianxiangdaimaowang/lumina/releases/download/v1.0/Lumina_Source_Code.zip)

### 文档

- [使用说明](docs/Lumina_使用说明.md)
- [部署说明](docs/Lumina_部署说明.md)
- [API服务配置](docs/Lumina_API集成说明.md)

## 构建项目

```powershell
# 构建发布版本
./build_apk.ps1 -release

# 构建调试版本
./build_apk.ps1 -debug

# 构建未签名版本
./build_apk_unsigned.ps1
```

## 许可证

本项目采用MIT许可证，详情请参阅LICENSE文件。

## 联系我们

- **邮箱**: support@lianxiangdaimaowang.com
- **网站**: [www.lianxiangdaimaowang.com](https://www.lianxiangdaimaowang.com)
