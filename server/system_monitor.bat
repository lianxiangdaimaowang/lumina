@echo off
echo Starting system monitoring...

REM 设置变量
set SERVICE_NAME=LuminaService
set NSSM_PATH=C:\app\nssm.exe
set LOG_FILE=C:\app\logs\system_monitor.log
set ALERT_THRESHOLD_CPU=80
set ALERT_THRESHOLD_MEMORY=80
set ALERT_THRESHOLD_DISK=85

REM 创建日志目录（如果不存在）
if not exist C:\app\logs mkdir C:\app\logs

REM 添加时间戳到日志
echo ======================================== >> %LOG_FILE%
echo System Status Check: %date% %time% >> %LOG_FILE%
echo ======================================== >> %LOG_FILE%

REM 检查服务状态
echo Checking service status... >> %LOG_FILE%
if exist %NSSM_PATH% (
    %NSSM_PATH% status %SERVICE_NAME% >> %LOG_FILE% 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo [ALERT] Service %SERVICE_NAME% is not running! >> %LOG_FILE%
        echo Attempting to restart service... >> %LOG_FILE%
        %NSSM_PATH% start %SERVICE_NAME% >> %LOG_FILE% 2>&1
    ) else (
        echo Service %SERVICE_NAME% is running correctly. >> %LOG_FILE%
    )
) else (
    echo Checking Java process... >> %LOG_FILE%
    tasklist /FI "IMAGENAME eq java.exe" | find "java.exe" > nul
    if %ERRORLEVEL% NEQ 0 (
        echo [ALERT] Java process not found! App may not be running. >> %LOG_FILE%
    ) else (
        echo Java process is running. >> %LOG_FILE%
    )
)

REM 检查CPU使用率
echo Checking CPU usage... >> %LOG_FILE%
for /f "skip=1" %%p in ('wmic cpu get loadpercentage') do (
    if not "%%p"=="" (
        set CPU_USAGE=%%p
        goto :CPU_CHECKED
    )
)
:CPU_CHECKED

if %CPU_USAGE% GTR %ALERT_THRESHOLD_CPU% (
    echo [ALERT] High CPU usage: %CPU_USAGE%%% >> %LOG_FILE%
) else (
    echo CPU usage: %CPU_USAGE%%% >> %LOG_FILE%
)

REM 检查内存使用情况
echo Checking memory usage... >> %LOG_FILE%
for /f "tokens=4" %%m in ('systeminfo ^| find "Physical Memory"') do (
    set MEM_AVAIL=%%m
)
set MEM_AVAIL=%MEM_AVAIL:,=%
set /a MEM_USAGE=100-%MEM_AVAIL%

if %MEM_USAGE% GTR %ALERT_THRESHOLD_MEMORY% (
    echo [ALERT] High memory usage: %MEM_USAGE%%% >> %LOG_FILE%
) else (
    echo Memory usage: %MEM_USAGE%%% >> %LOG_FILE%
)

REM 检查磁盘空间
echo Checking disk space... >> %LOG_FILE%
for /f "tokens=1,2,3,4,5,6" %%a in ('wmic logicaldisk where "DeviceID='C:'" get FreeSpace^,Size^,Caption /format:value') do (
    if "%%a"=="Caption=C:" set DRIVE=%%a
    if "%%c"=="FreeSpace=" set FREE=%%d
    if "%%e"=="Size=" set SIZE=%%f
)

REM 计算使用百分比
set /a DISK_USAGE=(%SIZE%-%FREE%)*100/%SIZE%

if %DISK_USAGE% GTR %ALERT_THRESHOLD_DISK% (
    echo [ALERT] High disk usage on C: %DISK_USAGE%%% >> %LOG_FILE%
    
    REM 清理临时文件
    echo Cleaning temporary files... >> %LOG_FILE%
    del /q C:\Windows\Temp\*.* >> %LOG_FILE% 2>&1
    
    REM 清理旧的日志文件
    echo Cleaning old log files... >> %LOG_FILE%
    forfiles /p C:\app\logs /s /m *.log /d -30 /c "cmd /c del @path" >> %LOG_FILE% 2>&1
) else (
    echo Disk usage on C: %DISK_USAGE%%% >> %LOG_FILE%
)

REM 检查应用程序日志中的错误
echo Checking application logs for errors... >> %LOG_FILE%
findstr /i /C:"error" /C:"exception" /C:"fatal" C:\app\logs\*.log > C:\app\logs\error_summary.log
for /f %%i in ("C:\app\logs\error_summary.log") do set ERROR_SIZE=%%~zi
if %ERROR_SIZE% GTR 0 (
    echo [ALERT] Found errors in application logs. See error_summary.log for details. >> %LOG_FILE%
) else (
    echo No critical errors found in application logs. >> %LOG_FILE%
)

echo System monitoring completed. >> %LOG_FILE%
echo --------------------------------------- >> %LOG_FILE% 