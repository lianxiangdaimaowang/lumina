# Lumina项目清理脚本
# 此脚本用于清理项目中不必要的文件，使GitHub仓库更加整洁

Write-Host "开始清理Lumina项目..." -ForegroundColor Cyan

# 需要删除的目录
$dirsToRemove = @(
    ".gradle",     # Gradle缓存目录
    ".idea",       # IDE配置目录
    "build",       # 构建输出目录
    "app/build",   # 应用构建输出目录
    "debug",       # 调试文件目录
    "Logcat_new"   # 日志文件目录
)

# 需要删除的文件
$filesToRemove = @(
    "local.properties",      # 本地配置文件，包含敏感路径信息
    "dependencies.txt",      # 空文件
    "Lumina-debug.apk"       # 调试APK，保留正式版APK即可
)

# 删除目录
foreach ($dir in $dirsToRemove) {
    if (Test-Path $dir) {
        Write-Host "正在删除目录: $dir" -ForegroundColor Yellow
        try {
            Remove-Item -Path $dir -Recurse -Force -ErrorAction Stop
            Write-Host "已成功删除目录: $dir" -ForegroundColor Green
        } catch {
            Write-Host "删除目录失败: $dir - $_" -ForegroundColor Red
        }
    } else {
        Write-Host "目录不存在，无需删除: $dir" -ForegroundColor DarkGray
    }
}

# 删除文件
foreach ($file in $filesToRemove) {
    if (Test-Path $file) {
        Write-Host "正在删除文件: $file" -ForegroundColor Yellow
        try {
            Remove-Item -Path $file -Force -ErrorAction Stop
            Write-Host "已成功删除文件: $file" -ForegroundColor Green
        } catch {
            Write-Host "删除文件失败: $file - $_" -ForegroundColor Red
        }
    } else {
        Write-Host "文件不存在，无需删除: $file" -ForegroundColor DarkGray
    }
}

# 整理文档文件
Write-Host "整理文档文件..." -ForegroundColor Cyan
if (-not (Test-Path "docs")) {
    New-Item -Path "docs" -ItemType Directory | Out-Null
    Write-Host "已创建docs目录" -ForegroundColor Green
}

# 将Markdown文档移动到docs目录
$docsToMove = @(
    "Lumina_使用说明.md",
    "Lumina_部署说明.md",
    "软件部署链接.md"
)

foreach ($doc in $docsToMove) {
    if (Test-Path $doc) {
        Write-Host "正在移动文档到docs目录: $doc" -ForegroundColor Yellow
        try {
            Move-Item -Path $doc -Destination "docs/$doc" -Force -ErrorAction Stop
            Write-Host "已成功移动文档: $doc" -ForegroundColor Green
        } catch {
            Write-Host "移动文档失败: $doc - $_" -ForegroundColor Red
        }
    }
}

# 创建更好的README.md
$readmeContent = @"
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
"@

# 更新README.md
Write-Host "更新README.md..." -ForegroundColor Cyan
try {
    Set-Content -Path "README.md" -Value $readmeContent -Force
    Write-Host "已成功更新README.md" -ForegroundColor Green
} catch {
    Write-Host "更新README.md失败: $_" -ForegroundColor Red
}

# 清理完成
Write-Host "项目清理完成！项目结构现在更加整洁。" -ForegroundColor Green
Write-Host "你可以使用'git add .'然后'git commit -m \"整理项目结构\"'来提交更改。" -ForegroundColor Cyan 