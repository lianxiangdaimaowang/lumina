@echo off
chcp 65001 > nul
echo Executing security check...
echo Results will be saved to security_check.log

set LOG_FILE=C:\app\logs\security_check.log

echo ======================================== > %LOG_FILE%
echo Security Check - %date% %time% >> %LOG_FILE%
echo ======================================== >> %LOG_FILE%

REM Check firewall status
echo Checking firewall status... >> %LOG_FILE%
netsh advfirewall show allprofiles >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Check open ports
echo Checking open ports... >> %LOG_FILE%
netstat -ano | findstr LISTENING >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Check firewall rules
echo Checking inbound firewall rules... >> %LOG_FILE%
netsh advfirewall firewall show rule name=all dir=in | findstr "Rule Name:|Enabled:|Action:|LocalPort:" >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Check Windows update status
echo Checking Windows update status... >> %LOG_FILE%
wmic qfe list brief >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Check scheduled tasks
echo Checking scheduled tasks... >> %LOG_FILE%
schtasks /query >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Check running services
echo Checking Lumina service status... >> %LOG_FILE%
sc query LuminaService >> %LOG_FILE%
echo. >> %LOG_FILE%

REM Check environment variables
echo Checking environment variables... >> %LOG_FILE%
echo JWT_SECRET variable status: >> %LOG_FILE%
if defined JWT_SECRET (
    echo JWT_SECRET is set >> %LOG_FILE%
) else (
    echo JWT_SECRET is NOT set! >> %LOG_FILE%
)

echo LUMINA_DB_PASSWORD variable status: >> %LOG_FILE%
if defined LUMINA_DB_PASSWORD (
    echo LUMINA_DB_PASSWORD is set >> %LOG_FILE%
) else (
    echo LUMINA_DB_PASSWORD is NOT set! >> %LOG_FILE%
)
echo. >> %LOG_FILE%

REM Check file permissions
echo Checking critical file permissions... >> %LOG_FILE%
icacls C:\app\*.bat >> %LOG_FILE%
icacls C:\app\lumina-server-1.0-SNAPSHOT.jar >> %LOG_FILE%
echo. >> %LOG_FILE%

echo Security check completed. Results saved to %LOG_FILE% 