@echo off
echo Registering Lumina as a Windows service...

REM 设置变量
set APP_DIR=C:\app
set JAR_FILE=%APP_DIR%\lumina-server-1.0-SNAPSHOT.jar
set SERVICE_NAME=LuminaService
set NSSM_PATH=C:\app\nssm.exe

REM 检查NSSM是否存在，如果不存在则下载
if not exist %NSSM_PATH% (
    echo NSSM not found, downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://nssm.cc/release/nssm-2.24.zip' -OutFile '%APP_DIR%\nssm.zip'"
    powershell -Command "Expand-Archive -Path '%APP_DIR%\nssm.zip' -DestinationPath '%APP_DIR%\nssm-temp'"
    copy "%APP_DIR%\nssm-temp\nssm-2.24\win64\nssm.exe" %NSSM_PATH%
    rmdir /s /q "%APP_DIR%\nssm-temp"
    del "%APP_DIR%\nssm.zip"
)

REM 检查服务是否已存在，如果存在则先移除
%NSSM_PATH% status %SERVICE_NAME% > nul
if %ERRORLEVEL% EQU 0 (
    echo Service already exists, removing...
    %NSSM_PATH% stop %SERVICE_NAME%
    %NSSM_PATH% remove %SERVICE_NAME% confirm
)

REM 注册服务
echo Registering service...
%NSSM_PATH% install %SERVICE_NAME% java
%NSSM_PATH% set %SERVICE_NAME% AppParameters -jar %JAR_FILE%
%NSSM_PATH% set %SERVICE_NAME% AppDirectory %APP_DIR%
%NSSM_PATH% set %SERVICE_NAME% DisplayName "Lumina Application Server"
%NSSM_PATH% set %SERVICE_NAME% Description "Lumina Spring Boot Application Server"
%NSSM_PATH% set %SERVICE_NAME% Start SERVICE_AUTO_START
%NSSM_PATH% set %SERVICE_NAME% AppStdout %APP_DIR%\logs\service-stdout.log
%NSSM_PATH% set %SERVICE_NAME% AppStderr %APP_DIR%\logs\service-stderr.log
%NSSM_PATH% set %SERVICE_NAME% AppRotateFiles 1
%NSSM_PATH% set %SERVICE_NAME% AppRotateOnline 1
%NSSM_PATH% set %SERVICE_NAME% AppRotateSeconds 86400
%NSSM_PATH% set %SERVICE_NAME% AppRotateBytes 10485760

REM 启动服务
echo Starting service...
%NSSM_PATH% start %SERVICE_NAME%

echo Service registration completed!
echo You can manage the service in the Windows Services panel or use:
echo   - %NSSM_PATH% status %SERVICE_NAME%  (查看状态)
echo   - %NSSM_PATH% start %SERVICE_NAME%   (启动服务)
echo   - %NSSM_PATH% stop %SERVICE_NAME%    (停止服务)
echo   - %NSSM_PATH% restart %SERVICE_NAME% (重启服务) 