# 导出源码脚本
# 这个脚本会将项目的主要源码目录压缩成zip文件

# 设置输出路径和文件名
$outputPath = "Lumina_Source_Code.zip"

Write-Host "开始导出Lumina项目源码..."
Write-Host "正在创建压缩文件: $outputPath"

# 创建临时导出目录
$tempDir = "temp_export"
if (Test-Path $tempDir) {
    Remove-Item -Path $tempDir -Recurse -Force
}
New-Item -Path $tempDir -ItemType Directory | Out-Null

# 复制主要源码目录到临时目录
Write-Host "正在复制源代码文件..."

# 复制核心源码
Copy-Item -Path "app/src/main/java" -Destination "$tempDir/java" -Recurse
Copy-Item -Path "app/src/main/res" -Destination "$tempDir/res" -Recurse
Copy-Item -Path "app/src/main/AndroidManifest.xml" -Destination "$tempDir/AndroidManifest.xml"

# 复制构建文件
Copy-Item -Path "app/build.gradle.kts" -Destination "$tempDir/app_build.gradle.kts"
Copy-Item -Path "build.gradle.kts" -Destination "$tempDir/build.gradle.kts"
Copy-Item -Path "settings.gradle.kts" -Destination "$tempDir/settings.gradle.kts"
Copy-Item -Path "gradle.properties" -Destination "$tempDir/gradle.properties"

# 创建README文件
@"
# Lumina 项目源码

此压缩包包含Lumina项目的主要源代码文件：

- java/ - Java/Kotlin源代码
- res/ - 资源文件
- AndroidManifest.xml - 应用清单文件
- 构建相关文件 - Gradle配置文件

## 项目结构

主要功能模块位于 java/com/lianxiangdaimaowang/lumina/ 目录下

## 导入说明

要导入此项目，请使用Android Studio并按照以下步骤操作：
1. 创建新项目
2. 将源文件复制到相应位置
3. 同步Gradle文件
"@ | Out-File -FilePath "$tempDir/README.md"

# 压缩临时目录
Write-Host "正在压缩文件..."
Compress-Archive -Path "$tempDir/*" -DestinationPath $outputPath -Force

# 清理临时文件
Remove-Item -Path $tempDir -Recurse -Force

Write-Host "源码导出完成！"
Write-Host "压缩文件位置: $PWD\$outputPath" 