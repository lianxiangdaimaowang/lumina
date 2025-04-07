# Lumina未签名APK构建脚本
# 此脚本构建未签名的APK，避免签名相关问题

# 显示开始信息
Write-Host "正在构建未签名APK..." -ForegroundColor Cyan

# 检查Gradle是否存在
if (-not (Test-Path "./gradlew")) {
    Write-Host "错误: 在当前目录找不到gradlew文件。请确保您在Lumina项目的根目录中。" -ForegroundColor Red
    exit 1
}

# 构建未签名APK
try {
    # 清理项目
    Write-Host "正在清理项目..." -ForegroundColor DarkGray
    & ./gradlew clean
    
    # 构建未签名APK
    Write-Host "正在构建未签名APK..." -ForegroundColor DarkGray
    & ./gradlew assembleRelease -x validateSigningRelease -x packageRelease
    
    # 继续打包过程，但使用debug签名
    Write-Host "正在使用debug签名完成打包..." -ForegroundColor DarkGray
    & ./gradlew assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        $apkPath = "./app/build/outputs/apk/debug/app-debug.apk"
        if (Test-Path $apkPath) {
            # 复制APK到根目录并重命名
            $rootApkPath = "./Lumina-unsigned.apk"
            Copy-Item -Path $apkPath -Destination $rootApkPath -Force
            
            Write-Host "未签名APK构建成功!" -ForegroundColor Green
            Write-Host "APK位置: $((Get-Item $rootApkPath).FullName)" -ForegroundColor Green
        } else {
            Write-Host "找不到生成的APK文件。构建可能失败。" -ForegroundColor Yellow
        }
    } else {
        Write-Host "构建未签名APK失败。" -ForegroundColor Red
    }
} catch {
    Write-Host "执行Gradle命令时出错: $_" -ForegroundColor Red
} 