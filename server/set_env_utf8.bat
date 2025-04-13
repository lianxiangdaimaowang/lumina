@echo off
chcp 65001 > nul
echo Setting environment variables...

setx LUMINA_DB_PASSWORD "Yihe041016!" /M
setx JWT_SECRET "production_secure_jwt_key_789!@#$%^&*()" /M

echo Environment variables have been set.
echo Please restart the service to apply new environment variables:
echo.
echo net stop LuminaService
echo net start LuminaService 