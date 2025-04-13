@echo off
echo Backing up Lumina application...

REM Set variables
set BACKUP_DIR=C:\app\backups
set DATE_FORMAT=%date:~10,4%%date:~4,2%%date:~7,2%
set MYSQL_USER=lumina
set MYSQL_PASS=Yihe041016!
set MYSQL_HOST=rm-2vc942bk6jz9173r7io.rwlb.cn-chengdu.rds.aliyuncs.com
set MYSQL_DB=lumina
set APP_DIR=C:\app

REM Create backup directory if not exists
if not exist %BACKUP_DIR% mkdir %BACKUP_DIR%

REM Backup MySQL database
echo Backing up database...
mysqldump -h %MYSQL_HOST% -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% > %BACKUP_DIR%\lumina_db_%DATE_FORMAT%.sql

REM Compress SQL file
echo Compressing database backup...
powershell Compress-Archive -Path %BACKUP_DIR%\lumina_db_%DATE_FORMAT%.sql -DestinationPath %BACKUP_DIR%\lumina_db_%DATE_FORMAT%.zip
del %BACKUP_DIR%\lumina_db_%DATE_FORMAT%.sql

REM Backup application logs
echo Backing up application logs...
powershell Compress-Archive -Path %APP_DIR%\logs\* -DestinationPath %BACKUP_DIR%\lumina_logs_%DATE_FORMAT%.zip

REM Backup application files
echo Backing up application files...
powershell Compress-Archive -Path %APP_DIR%\lumina-server-*.jar -DestinationPath %BACKUP_DIR%\lumina_app_%DATE_FORMAT%.zip

echo Backup completed successfully!
echo Files saved to %BACKUP_DIR% 