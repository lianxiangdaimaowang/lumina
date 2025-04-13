@echo off
echo Setting up Lumina monitoring and backup tasks...

REM 设置变量
set APP_DIR=C:\app
set SCRIPTS_DIR=%APP_DIR%

REM 创建必要的目录
if not exist %APP_DIR%\logs mkdir %APP_DIR%\logs
if not exist %APP_DIR%\backups mkdir %APP_DIR%\backups

REM 复制脚本到应用目录（如果需要）
if not "%CD%"=="%APP_DIR%" (
    echo Copying scripts to application directory...
    copy rotate_logs.bat %APP_DIR%\
    copy backup_script.bat %APP_DIR%\
    copy system_monitor.bat %APP_DIR%\
    copy register_service.bat %APP_DIR%\
)

REM 注册Windows服务
echo Registering Lumina as a Windows service...
call %APP_DIR%\register_service.bat

REM 创建或更新计划任务
echo Setting up scheduled tasks...

REM 删除现有任务（如果存在）
schtasks /query /tn "LuminaLogRotate" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Removing existing log rotation task...
    schtasks /delete /tn "LuminaLogRotate" /f
)

schtasks /query /tn "LuminaBackup" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Removing existing backup task...
    schtasks /delete /tn "LuminaBackup" /f
)

schtasks /query /tn "LuminaSystemMonitor" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Removing existing system monitor task...
    schtasks /delete /tn "LuminaSystemMonitor" /f
)

REM 创建新任务
echo Creating new scheduled tasks...

REM 日志轮转 - 每天午夜执行
schtasks /create /tn "LuminaLogRotate" /tr "%APP_DIR%\rotate_logs.bat" /sc daily /st 00:00 /ru System
if %ERRORLEVEL% NEQ 0 (
    echo Failed to create log rotation task!
) else (
    echo Created log rotation task.
)

REM 系统备份 - 每天凌晨1点执行
schtasks /create /tn "LuminaBackup" /tr "%APP_DIR%\backup_script.bat" /sc daily /st 01:00 /ru System
if %ERRORLEVEL% NEQ 0 (
    echo Failed to create backup task!
) else (
    echo Created backup task.
)

REM 系统监控 - 每4小时执行一次
schtasks /create /tn "LuminaSystemMonitor" /tr "%APP_DIR%\system_monitor.bat" /sc hourly /mo 4 /ru System
if %ERRORLEVEL% NEQ 0 (
    echo Failed to create system monitoring task!
) else (
    echo Created system monitoring task.
)

echo.
echo Monitoring setup completed!
echo.
echo The following tasks have been scheduled:
echo  - Log Rotation: Daily at midnight
echo  - System Backup: Daily at 1:00 AM
echo  - System Monitoring: Every 4 hours
echo.
echo Logs will be stored in: %APP_DIR%\logs
echo Backups will be stored in: %APP_DIR%\backups
echo. 