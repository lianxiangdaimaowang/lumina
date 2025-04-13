@echo off
echo Checking disk space...

REM 设置阈值（百分比）
set THRESHOLD=90

REM 获取C盘使用率
for /f "tokens=1,2,3,4,5,6" %%a in ('wmic logicaldisk where "DeviceID='C:'" get FreeSpace^,Size^,Caption /format:value') do (
    if "%%a"=="Caption=C:" set DRIVE=%%a
    if "%%c"=="FreeSpace=" set FREE=%%d
    if "%%e"=="Size=" set SIZE=%%f
)

REM 计算使用百分比
set /a USED=(%SIZE%-%FREE%)*100/%SIZE%

echo Drive C: usage: %USED%%%

REM 检查是否超过阈值
if %USED% GTR %THRESHOLD% (
    echo WARNING: Disk space critical! Usage exceeds %THRESHOLD%%%
    
    REM 发送邮件提醒 (需要配置SMTP服务器)
    REM powershell -command "Send-MailMessage -From 'alert@example.com' -To 'admin@example.com' -Subject 'Disk Space Alert' -Body 'Disk usage on server exceeds %THRESHOLD%%% (current: %USED%%%)' -SmtpServer 'smtp.example.com'"
    
    REM 清理旧备份文件（保留最近7天）
    echo Cleaning old backup files...
    forfiles /p "C:\app\backups" /s /m *.zip /d -7 /c "cmd /c del @path"
)

echo Disk check completed. 