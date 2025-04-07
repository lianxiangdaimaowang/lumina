# Lumina APK构建脚本
# 此脚本会根据参数构建调试版本或发布版本的APK

param (
    [switch]$debug = $false,
    [switch]$release = $false,
    [switch]$bundle = $false,
    [switch]$help = $false
)

# 显示帮助信息
function Show-Help {
    Write-Host "Lumina APK构建脚本"
    Write-Host "用法: .\build_apk.ps1 [-debug] [-release] [-bundle] [-help]"
    Write-Host ""
    Write-Host "参数:"
    Write-Host "  -debug    构建调试版本APK"
    Write-Host "  -release  构建发布版本APK"
    Write-Host "  -bundle   构建AAB格式的应用包"
    Write-Host "  -help     显示帮助信息"
    Write-Host ""
    Write-Host "示例:"
    Write-Host "  .\build_apk.ps1 -debug    # 构建调试版本"
    Write-Host "  .\build_apk.ps1 -release  # 构建发布版本"
    Write-Host ""
}

# 如果没有提供参数或者指定了帮助，显示帮助信息
if ($help -or (-not $debug -and -not $release -and -not $bundle)) {
    Show-Help
    exit 0
}

# 检查Gradle是否存在
if (-not (Test-Path "./gradlew")) {
    Write-Host "错误: 在当前目录找不到gradlew文件。请确保您在Lumina项目的根目录中。" -ForegroundColor Red
    exit 1
}

# 添加执行权限给gradlew文件
Write-Host "正在添加执行权限给gradlew文件..."
try {
    chmod +x ./gradlew 2>$null
} catch {
    # Windows下chmod可能不可用，可以忽略这个错误
}

# 构建调试版本
if ($debug) {
    Write-Host "正在构建调试版本APK..." -ForegroundColor Cyan
    Write-Host "执行命令: ./gradlew assembleDebug" -ForegroundColor DarkGray
    
    try {
        & ./gradlew assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            $apkPath = "./app/build/outputs/apk/debug/app-debug.apk"
            if (Test-Path $apkPath) {
                Write-Host "调试版本APK构建成功!" -ForegroundColor Green
                Write-Host "APK位置: $((Get-Item $apkPath).FullName)" -ForegroundColor Green
            } else {
                Write-Host "找不到生成的APK文件。构建可能失败。" -ForegroundColor Yellow
            }
        } else {
            Write-Host "构建调试版本APK失败。" -ForegroundColor Red
        }
    } catch {
        Write-Host "执行Gradle命令时出错: $_" -ForegroundColor Red
    }
}

# 构建发布版本
if ($release) {
    Write-Host "正在构建发布版本APK..." -ForegroundColor Cyan
    Write-Host "执行命令: ./gradlew assembleRelease" -ForegroundColor DarkGray
    
    try {
        & ./gradlew assembleRelease
        
        if ($LASTEXITCODE -eq 0) {
            $apkPath = "./app/build/outputs/apk/release/app-release.apk"
            if (Test-Path $apkPath) {
                Write-Host "发布版本APK构建成功!" -ForegroundColor Green
                Write-Host "APK位置: $((Get-Item $apkPath).FullName)" -ForegroundColor Green
            } else {
                Write-Host "找不到生成的APK文件。构建可能失败。" -ForegroundColor Yellow
            }
        } else {
            Write-Host "构建发布版本APK失败。" -ForegroundColor Red
        }
    } catch {
        Write-Host "执行Gradle命令时出错: $_" -ForegroundColor Red
    }
}

# 构建应用包
if ($bundle) {
    Write-Host "正在构建AAB应用包..." -ForegroundColor Cyan
    Write-Host "执行命令: ./gradlew bundleRelease" -ForegroundColor DarkGray
    
    try {
        & ./gradlew bundleRelease
        
        if ($LASTEXITCODE -eq 0) {
            $aabPath = "./app/build/outputs/bundle/release/app-release.aab"
            if (Test-Path $aabPath) {
                Write-Host "AAB应用包构建成功!" -ForegroundColor Green
                Write-Host "AAB位置: $((Get-Item $aabPath).FullName)" -ForegroundColor Green
            } else {
                Write-Host "找不到生成的AAB文件。构建可能失败。" -ForegroundColor Yellow
            }
        } else {
            Write-Host "构建AAB应用包失败。" -ForegroundColor Red
        }
    } catch {
        Write-Host "执行Gradle命令时出错: $_" -ForegroundColor Red
    }
} 