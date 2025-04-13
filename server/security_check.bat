@echo off
echo 执行安全检查...
echo 结果将输出到security_check.log

set LOG_FILE=C:\app\logs\security_check.log

echo ======================================== > %LOG_FILE%
echo 安全检查 - %date% %time% >> %LOG_FILE%
echo ======================================== >> %LOG_FILE%

REM 检查防火墙状态
echo 检查防火墙状态... >> %LOG_FILE%
netsh advfirewall show allprofiles >> %LOG_FILE%
echo. >> %LOG_FILE%

REM 检查开放端口
echo 检查开放端口... >> %LOG_FILE%
netstat -ano | findstr LISTENING >> %LOG_FILE%
echo. >> %LOG_FILE%

REM 检查防火墙规则
echo 检查入站防火墙规则... >> %LOG_FILE%
netsh advfirewall firewall show rule name=all dir=in | findstr "Rule Name:|Enabled:|Action:|LocalPort:" >> %LOG_FILE%
echo. >> %LOG_FILE%

REM 检查Windows更新状态
echo 检查Windows更新状态... >> %LOG_FILE%
wmic qfe list brief >> %LOG_FILE%
echo. >> %LOG_FILE%

REM 检查计划任务
echo 检查计划任务... >> %LOG_FILE%
schtasks /query >> %LOG_FILE%
echo. >> %LOG_FILE%

REM 检查运行中的服务
echo 检查Lumina服务状态... >> %LOG_FILE%
sc query LuminaService >> %LOG_FILE%
echo. >> %LOG_FILE%

REM 检查环境变量
echo 检查环境变量... >> %LOG_FILE%
echo JWT_SECRET 变量是否设置: >> %LOG_FILE%
if defined JWT_SECRET (
    echo JWT_SECRET 已设置 >> %LOG_FILE%
) else (
    echo JWT_SECRET 未设置! >> %LOG_FILE%
)

echo LUMINA_DB_PASSWORD 变量是否设置: >> %LOG_FILE%
if defined LUMINA_DB_PASSWORD (
    echo LUMINA_DB_PASSWORD 已设置 >> %LOG_FILE%
) else (
    echo LUMINA_DB_PASSWORD 未设置! >> %LOG_FILE%
)
echo. >> %LOG_FILE%

REM 检查文件权限
echo 检查关键文件权限... >> %LOG_FILE%
icacls C:\app\*.bat >> %LOG_FILE%
icacls C:\app\lumina-server-1.0-SNAPSHOT.jar >> %LOG_FILE%
echo. >> %LOG_FILE%

echo 安全检查完成，结果已保存到 %LOG_FILE% 