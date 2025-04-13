@echo off
echo Starting log rotation...

REM 设置变量
set LOG_DIR=C:\app\logs
set BACKUP_DIR=C:\app\logs\archive
set MAX_DAYS=7

REM 使用更可靠的日期格式
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (
    set DAY=%%a
    set MONTH=%%b
    set YEAR=%%c
)
set DATE_FORMAT=%YEAR%%MONTH%%DAY%

REM 创建日志目录（如果不存在）
if not exist %LOG_DIR% (
    echo Creating log directory...
    mkdir %LOG_DIR%
)

REM 创建归档目录（如果不存在）
if not exist %BACKUP_DIR% (
    echo Creating archive directory...
    mkdir %BACKUP_DIR%
)

REM 检查是否有日志文件存在
dir /b %LOG_DIR%\*.log >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo No log files found in %LOG_DIR%
) else (
    REM 轮转日志文件
    for %%f in (%LOG_DIR%\*.log) do (
        echo Rotating %%f
        
        REM 创建带时间戳的备份
        copy "%%f" "%BACKUP_DIR%\%%~nf_%DATE_FORMAT%%%~xf"
        
        REM 清空原始日志文件
        type nul > "%%f"
        
        echo Rotated %%f to %BACKUP_DIR%\%%~nf_%DATE_FORMAT%%%~xf
    )
)

REM 检查归档目录中是否有日志文件
dir /b %BACKUP_DIR%\*.log >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo No log files found in %BACKUP_DIR% for compression
) else (
    REM 压缩超过一天的日志文件
    echo Compressing log files...
    for %%f in (%BACKUP_DIR%\*.log) do (
        powershell -Command "Compress-Archive -Path '%%f' -DestinationPath '%%~dpnf.zip' -Force"
        del "%%f"
        echo Compressed and deleted %%f
    )
)

REM 检查归档目录中是否有压缩文件
dir /b %BACKUP_DIR%\*.zip >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo No archive files found in %BACKUP_DIR% for cleanup
) else (
    REM 删除超过指定天数的旧日志压缩文件
    echo Cleaning old archives...
    powershell -Command "$limit = (Get-Date).AddDays(-%MAX_DAYS%); Get-ChildItem -Path '%BACKUP_DIR%' -Filter *.zip | Where-Object {$_.LastWriteTime -lt $limit} | Remove-Item -Force"
    echo Old archives cleaned
)

echo Log rotation completed! 