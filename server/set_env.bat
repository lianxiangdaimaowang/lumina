@echo off
REM 设置环境变量
setx LUMINA_DB_PASSWORD "Yihe041016!" /M
setx JWT_SECRET "production_secure_jwt_key_789!@#$%^&*()" /M

echo 环境变量已设置，请重启服务以使用新的环境变量：
echo.
echo net stop LuminaService
echo net start LuminaService 