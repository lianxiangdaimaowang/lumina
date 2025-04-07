# 清理Gradle缓存的PowerShell脚本，解决BouncyCastle依赖问题

# 显示开始信息
Write-Host "Cleaning Gradle caches..."

# 定义需要清理的路径
$bouncyCastlePath = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\org.bouncycastle"
$jarsDir = "$env:USERPROFILE\.gradle\caches\jars-9"
$problemJarDir = "$env:USERPROFILE\.gradle\caches\jars-9\18366b31678c0171857be093a3b8ec22"
$lockfilesPath = "$env:USERPROFILE\.gradle\caches"

# 尝试删除jars-9目录（这里包含可能有问题的jar文件）
if (Test-Path $jarsDir) {
    Remove-Item -Path $jarsDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "Deleted cache directory: $jarsDir"
}

# 尝试删除BouncyCastle缓存
if (Test-Path $bouncyCastlePath) {
    Remove-Item -Path $bouncyCastlePath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "Deleted BouncyCastle cache: $bouncyCastlePath"
}

# 删除Gradle锁文件
Get-ChildItem -Path $lockfilesPath -Include "*.lock" -Recurse -ErrorAction SilentlyContinue | 
    ForEach-Object {
        Remove-Item -Path $_.FullName -Force -ErrorAction SilentlyContinue
    }
Write-Host "Deleted Gradle lock files"

# 完成信息
Write-Host "Cache cleaning complete"
Write-Host "Please try running: ./gradlew clean --refresh-dependencies" 