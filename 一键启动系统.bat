@echo off
setlocal EnableExtensions

set "ROOT=%~dp0"
set "SERVER_DIR=%ROOT%SmartCourse-Server"
set "APP_DIR=%ROOT%SmartCourse-App"
set "TEACHER_DIR=%ROOT%SmartCourse-Teacher"
set "ADMIN_DIR=%ROOT%SmartCourse-Admin"
set "PORTAL_DIR=%ROOT%SmartCourse-Portal"
set "JAR=%SERVER_DIR%\target\online-edu-0.0.1-SNAPSHOT.jar"

title SmartCourse One Click Startup

echo ==================================================
echo   SmartCourse One Click Startup
echo ==================================================
echo.

echo [1/8] Checking Java...
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java was not found. Please install JDK 8/11 and add java to PATH.
  pause
  exit /b 1
)

echo [2/8] Checking Node.js and npm...
where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm was not found. Please install Node.js 14/16 and add npm to PATH.
  pause
  exit /b 1
)

echo [3/8] Starting Redis...
if exist "%ROOT%Redis\redis-server.exe" (
  start "SmartCourse-Redis" cmd /k "pushd ""%ROOT%Redis"" && redis-server.exe redis.windows.conf"
) else (
  echo [WARN] Built-in Redis was not found. Skip Redis startup.
)
timeout /t 2 /nobreak >nul

echo.
set /p MYSQL_PWD=Enter MySQL root password (press Enter if blank):

echo.
set /p INIT_DB=Import/reset demo database online_edu? First run type Y, otherwise type N:
if /i "%INIT_DB%"=="Y" (
  call :init_db
)

echo.
echo [4/8] Checking backend jar...
if not exist "%JAR%" (
  echo [INFO] Backend jar not found. Building with Maven...
  pushd "%SERVER_DIR%"
  call mvn -q -DskipTests package
  popd
  if errorlevel 1 (
    echo [ERROR] Backend build failed. Please check Maven and JDK.
    pause
    exit /b 1
  )
)

echo [5/8] Starting SpringBoot backend...
start "SmartCourse-Backend" cmd /k "pushd ""%SERVER_DIR%"" && java -jar ""%JAR%"""
timeout /t 5 /nobreak >nul

echo [6/8] Starting Vue frontends...
call :start_vue "%APP_DIR%" "SmartCourse-Student"
call :start_vue "%TEACHER_DIR%" "SmartCourse-Teacher"
call :start_vue "%ADMIN_DIR%" "SmartCourse-Admin"

echo [7/8] Starting unified portal...
start "SmartCourse-Portal" cmd /k "pushd ""%PORTAL_DIR%"" && node server.cjs"

echo.
echo [8/8] Startup commands have been launched.
echo Please wait until all frontend windows show "Compiled successfully".
echo.
echo Portal : http://localhost:9527
echo Student: http://localhost:9530
echo Teacher: http://localhost:9529
echo Admin  : http://localhost:9528
echo.
echo Accounts are listed in TEST_ACCOUNTS.txt
echo Recommended student: 222023321102103 / 123456
echo Recommended teacher: 13800138002 / 123456
echo Admin: admin / 123456
echo.
pause
exit /b 0

:start_vue
set "DIR=%~1"
set "WIN_TITLE=%~2"
if not exist "%DIR%\node_modules" (
  start "%WIN_TITLE%" cmd /k "pushd ""%DIR%"" && npm install --registry=https://registry.npmmirror.com --legacy-peer-deps && npm run dev"
) else (
  start "%WIN_TITLE%" cmd /k "pushd ""%DIR%"" && npm run dev"
)
timeout /t 1 /nobreak >nul
exit /b 0

:init_db
echo [DB] Searching mysql.exe...
set "MYSQL_CMD=mysql"
where mysql >nul 2>nul
if errorlevel 1 (
  set "MYSQL_CMD="
  for %%p in ("C:\Program Files\MySQL\MySQL Server 8.0\bin" "C:\Program Files\MySQL\MySQL Server 5.7\bin" "D:\Program Files\MySQL\MySQL Server 8.0\bin" "C:\phpstudy_pro\Extensions\MySQL5.7.26\bin" "D:\phpstudy_pro\Extensions\MySQL5.7.26\bin") do (
    if exist "%%~p\mysql.exe" set "MYSQL_CMD=%%~p\mysql.exe"
  )
)
if "%MYSQL_CMD%"=="" (
  echo [ERROR] mysql.exe was not found. Add MySQL bin to PATH or import SmartCourse-Server\schema.sql manually.
  pause
  exit /b 1
)

"%MYSQL_CMD%" -uroot -e "DROP DATABASE IF EXISTS online_edu; CREATE DATABASE online_edu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 (
  echo [ERROR] MySQL connection failed. Check root password and MySQL service.
  pause
  exit /b 1
)
"%MYSQL_CMD%" -uroot online_edu < "%SERVER_DIR%\schema.sql"
if errorlevel 1 (
  echo [ERROR] Database import failed.
  pause
  exit /b 1
)
echo [DB] Demo database imported successfully.
exit /b 0
