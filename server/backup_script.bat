@echo off
setlocal EnableDelayedExpansion

REM 设置日志文件
set LOG_FILE=C:\app\logs\backup.log

echo ======================================== >> %LOG_FILE%
echo Backup started: %date% %time% >> %LOG_FILE%
echo ======================================== >> %LOG_FILE%

echo Backing up Lumina application...

REM 设置变量
set BACKUP_DIR=C:\app\backups
set DATE_FORMAT=%date:~10,4%%date:~4,2%%date:~7,2%
set MYSQL_USER=lumina
set MYSQL_PASS=Yihe041016!
set MYSQL_HOST=rm-2vc942bk6jz9173r7io.rwlb.cn-chengdu.rds.aliyuncs.com
set MYSQL_DB=lumina
set APP_DIR=C:\app

REM 创建日志目录（如果不存在）
if not exist C:\app\logs mkdir C:\app\logs

REM 创建备份目录（如果不存在）
if not exist %BACKUP_DIR% (
    echo Creating backup directory %BACKUP_DIR%... >> %LOG_FILE%
    mkdir %BACKUP_DIR%
    if !ERRORLEVEL! NEQ 0 (
        echo [ERROR] Failed to create backup directory! >> %LOG_FILE%
        goto :ERROR
    )
)

REM 备份MySQL数据库
echo Backing up database... >> %LOG_FILE%
mysqldump -h %MYSQL_HOST% -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% > %BACKUP_DIR%\lumina_db_%DATE_FORMAT%.sql
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Database backup failed! >> %LOG_FILE%
    goto :ERROR
) else (
    echo Database backup successful. >> %LOG_FILE%
)

REM 压缩SQL文件
echo Compressing database backup... >> %LOG_FILE%
powershell -Command "Compress-Archive -Path '%BACKUP_DIR%\lumina_db_%DATE_FORMAT%.sql' -DestinationPath '%BACKUP_DIR%\lumina_db_%DATE_FORMAT%.zip' -Force"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Database compression failed! >> %LOG_FILE%
    goto :ERROR
) else (
    del %BACKUP_DIR%\lumina_db_%DATE_FORMAT%.sql
    echo Database compression successful. >> %LOG_FILE%
)

REM 检查日志目录是否存在日志文件
dir /b %APP_DIR%\logs\*.log >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo No log files found to backup. >> %LOG_FILE%
) else (
    REM 备份应用日志
    echo Backing up application logs... >> %LOG_FILE%
    powershell -Command "Compress-Archive -Path '%APP_DIR%\logs\*.log' -DestinationPath '%BACKUP_DIR%\lumina_logs_%DATE_FORMAT%.zip' -Force"
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Log backup failed! >> %LOG_FILE%
        goto :ERROR
    ) else (
        echo Log backup successful. >> %LOG_FILE%
    )
)

REM 备份应用文件
echo Backing up application files... >> %LOG_FILE%
if exist %APP_DIR%\lumina-server-*.jar (
    powershell -Command "Compress-Archive -Path '%APP_DIR%\lumina-server-*.jar' -DestinationPath '%BACKUP_DIR%\lumina_app_%DATE_FORMAT%.zip' -Force"
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Application backup failed! >> %LOG_FILE%
        goto :ERROR
    ) else (
        echo Application backup successful. >> %LOG_FILE%
    )
) else (
    echo [WARNING] No application JAR file found! >> %LOG_FILE%
)

REM 清理旧备份文件（保留最近14天）
echo Cleaning old backups... >> %LOG_FILE%
powershell -Command "$limit = (Get-Date).AddDays(-14); Get-ChildItem -Path '%BACKUP_DIR%' -Filter *.zip | Where-Object {$_.LastWriteTime -lt $limit} | ForEach-Object { Remove-Item -Path $_.FullName -Force; Write-Output ('Removed: ' + $_.Name) }" >> %LOG_FILE%

echo Backup completed successfully! >> %LOG_FILE%
echo Files saved to %BACKUP_DIR% >> %LOG_FILE%
goto :END

:ERROR
echo Backup process encountered errors! Check log for details. >> %LOG_FILE%

:END
echo --------------------------------------- >> %LOG_FILE%
exit /b %ERRORLEVEL% 