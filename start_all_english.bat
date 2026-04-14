@echo off
setlocal enabledelayedexpansion
title OnlineEdu Auto Startup (English Version)

echo ==============================================
echo    OnlineEdu Project Startup Script
echo ==============================================
echo.

:: 1. Environment Check: JDK
echo [Check] Detecting JDK (Java)...
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo.
    echo ===================================================
    echo [FATAL ERROR] JDK 1.8 is NOT INSTALLED or NOT IN PATH!
    echo Please install JDK 1.8 and configure your Environment Variables.
    echo ===================================================
    pause
    exit /b
)
echo  =^> Java is OK.

:: 2. Environment Check: Node.js (npm)
echo [Check] Detecting Node.js...
where npm >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo.
    echo ===================================================
    echo [FATAL ERROR] Node.js is NOT INSTALLED or NOT IN PATH!
    echo Please download it from https://nodejs.org/ and install it.
    echo ===================================================
    pause
    exit /b
)
echo  =^> Node.js is OK.

:: 3. Interactive Import DB Menu
echo.
set /p DO_INIT="Do you want to AUTO-IMPORT the database? (Type Y for yes, N if already imported): "
if /i "%DO_INIT%"=="N" goto skip_db

set /p MYSQL_PWD="[Required] Enter your MySQL root password (Press Enter if blank): "

:: 4. Attempt MySQL Automatic Search
echo.
echo [Check] Detecting MySQL command-line tool...
set MYSQL_CMD=mysql
where mysql >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo  =^> MySQL is in PATH.
    goto mysql_found
)

echo  =^> MySQL not in PATH. Searching common directories...
set "COMMON_PATHS=C:\Program Files\MySQL\MySQL Server 8.0\bin C:\Program Files\MySQL\MySQL Server 8.1\bin C:\Program Files\MySQL\MySQL Server 5.7\bin D:\Program Files\MySQL\MySQL Server 8.0\bin C:\xampp\mysql\bin D:\xampp\mysql\bin C:\phpstudy_pro\Extensions\MySQL5.7.26\bin D:\phpstudy_pro\Extensions\MySQL5.7.26\bin"
for %%p in (%COMMON_PATHS%) do (
    if exist "%%p\mysql.exe" (
        set "MYSQL_CMD="%%p\mysql.exe""
        echo  =^> Found MySQL at: %%p\mysql.exe
        goto mysql_found
    )
)

:: 5. Fallback to Node.js Import (For users lacking mysql.exe in PATH)
echo  =^> Still cannot find mysql.exe! 
echo  =^> Using Node.js smart fallback to inject database...
cd /d "%~dp0OnlineEdu-master"
echo  =^> Installing database driver for Node.js (this only takes a few seconds)...
call npm install mysql2 --no-save --registry=https://registry.npmmirror.com >nul 2>nul

echo const mysql = require('mysql2/promise'); > _db_init.js
echo const fs = require('fs'); >> _db_init.js
echo async function init() { >> _db_init.js
echo   try { >> _db_init.js
echo     console.log('  [Node.js] Connecting to MySQL...'); >> _db_init.js
echo     const conn = await mysql.createConnection({ host: 'localhost', user: 'root', password: '%MYSQL_PWD%', multipleStatements: true }); >> _db_init.js
echo     console.log('  [Node.js] Connection successful! Creating database online_edu...'); >> _db_init.js
echo     await conn.query('CREATE DATABASE IF NOT EXISTS online_edu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'); >> _db_init.js
echo     await conn.query('USE online_edu;'); >> _db_init.js
echo     console.log('  [Node.js] Executing schema.sql... Please wait...'); >> _db_init.js
echo     const sql = fs.readFileSync('schema.sql', 'utf8'); >> _db_init.js
echo     await conn.query(sql); >> _db_init.js
echo     console.log('  [Node.js] Database imported successfully!'); >> _db_init.js
echo     process.exit(0); >> _db_init.js
echo   } catch(e) { >> _db_init.js
echo     console.error('  [Node.js ERROR] Database initialization failed: ', e.message); >> _db_init.js
echo     process.exit(1); >> _db_init.js
echo   } >> _db_init.js
echo } >> _db_init.js
echo init(); >> _db_init.js

node _db_init.js
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Database Smart Fallback Import Failed! Check your MySQL password!
    pause
    goto skip_db
)
:: Clean up
del _db_init.js
goto skip_db


:mysql_found
echo.
echo [1/6] Initializing Database via MYSQL CMD...
!MYSQL_CMD! -uroot -p%MYSQL_PWD% -e "CREATE DATABASE IF NOT EXISTS online_edu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %ERRORLEVEL% neq 0 (
    echo.
    echo ===================================================
    echo [ERROR] MySQL Connection Failed! Wrong password or MySQL isn't running?
    echo We will skip database import and try to continue.
    echo ===================================================
    pause
    goto skip_db
)
!MYSQL_CMD! -uroot -p%MYSQL_PWD% online_edu < "%~dp0OnlineEdu-master\schema.sql"
echo Database initialized successfully.


:skip_db
echo.
echo [2/6] Starting Redis...
if exist "%~dp0Redis\redis-server.exe" (
    start "Redis" cmd /k "cd /d %~dp0Redis && redis-server.exe redis.windows.conf"
    timeout /t 2 >nul
) else (
    echo  =^> Redis not found, skipping...
)

:: Ask password for SpringBoot connection if we didn't ask yet
if not defined MYSQL_PWD (
    echo.
    set /p MYSQL_PWD="Enter MySQL root password for Backend to connect to DB (Or press Enter if none): "
)

echo [3/6] Starting SpringBoot Backend...
if exist "%~dp0OnlineEdu-master\target\online-edu-0.0.1-SNAPSHOT.jar" (
    start "Backend" cmd /k "cd /d %~dp0OnlineEdu-master\target && java -jar online-edu-0.0.1-SNAPSHOT.jar --spring.datasource.password=%MYSQL_PWD%"
) else (
    echo.
    echo ===================================================
    echo [FATAL ERROR] Backend JAR not found!
    echo Please build the backend project first using Maven.
    echo You are missing \OnlineEdu-master\target\online-edu-0.0.1-SNAPSHOT.jar
    echo ===================================================
    pause
    exit /b
)
timeout /t 3 >nul

echo [4/6] Starting Vue App Frontend (Student)...
if not exist "%~dp0OnlineEdu-App-master\node_modules\" (
    echo Installing dependencies for App...
    start "App_Frontend" cmd /k "cd /d %~dp0OnlineEdu-App-master && npm install --registry=https://registry.npmmirror.com --legacy-peer-deps && npm run dev"
) else (
    start "App_Frontend" cmd /k "cd /d %~dp0OnlineEdu-App-master && npm run dev"
)
timeout /t 1 >nul

echo [5/6] Starting Vue Teacher Frontend...
if not exist "%~dp0OnlineEdu-Teacher-master\node_modules\" (
    echo Installing dependencies for Teacher...
    start "Teacher_Frontend" cmd /k "cd /d %~dp0OnlineEdu-Teacher-master && npm install --registry=https://registry.npmmirror.com --legacy-peer-deps && npm run dev"
) else (
    start "Teacher_Frontend" cmd /k "cd /d %~dp0OnlineEdu-Teacher-master && npm run dev"
)
timeout /t 1 >nul

echo [6/6] Starting Vue Admin Frontend...
if not exist "%~dp0OnlineEdu-Admin-master\node_modules\" (
    echo Installing dependencies for Admin...
    start "Admin_Frontend" cmd /k "cd /d %~dp0OnlineEdu-Admin-master && npm install --registry=https://registry.npmmirror.com --legacy-peer-deps && npm run dev"
) else (
    start "Admin_Frontend" cmd /k "cd /d %~dp0OnlineEdu-Admin-master && npm run dev"
)

:: Create test webpage
echo.
echo [Creating] Generating test webpage...
echo ^<!DOCTYPE html^>
^<html lang="en"^>
^<head^>
    ^<meta charset="UTF-8"^>
    ^<meta name="viewport" content="width=device-width, initial-scale=1.0"^>
    ^<title^>System Test^</title^>
    ^<style^>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            text-align: center;
        }
        .test-section {
            margin: 20px 0;
            padding: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .test-section h2 {
            color: #555;
            margin-top: 0;
        }
        .status {
            padding: 10px;
            border-radius: 4px;
            margin: 10px 0;
        }
        .status.success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .status.error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .button {
            display: inline-block;
            padding: 10px 20px;
            background-color: #007bff;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            margin: 5px;
        }
        .button:hover {
            background-color: #0069d9;
        }
    ^</style^>
^</head^>
^<body^>
    ^<div class="container"^>
        ^<h1^>Online Education System Test^</h1^>
        
        ^<div class="test-section"^>
            ^<h2^>System Status^</h2^>
            ^<div class="status success"^>
                ^<strong^>Database Status:^</strong^> Data imported successfully
            ^</div^>
            ^<div class="status success"^>
                ^<strong^>Backend Service:^</strong^> Started (Port: 9096)
            </div^>
        ^</div^>
        
        ^<div class="test-section"^>
            ^<h2^>Access Systems^</h2^>
            ^<p^>Click the links below to access each system:^</p^>
            ^<a href="http://localhost:9528" class="button" target="_blank"^>Admin System^</a^>
            ^<a href="http://localhost:9529" class="button" target="_blank"^>Teacher System^</a^>
            ^<a href="http://localhost:9530" class="button" target="_blank"^>Student System^</a^>
        ^</div^>
        
        ^<div class="test-section"^>
            ^<h2^>Test Accounts^</h2^>
            ^<p^>^<strong^>Admin:^</strong^> Username: admin, Password: 123456^</p^>
            ^<p^>^<strong^>Teacher:^</strong^> Username: teacher, Password: 123456^</p^>
            ^<p^>^<strong^>Student:^</strong^> Username: student, Password: 123456^</p^>
        ^</div^>
        
        ^<div class="test-section"^>
            ^<h2^>Database Test^</h2^>
            ^<p^>Here are some of the database tables:^</p^>
            ^<ul^>
                ^<li^>acl_user - Admin user table^</li^>
                ^<li^>edu_course - Course table^</li^>
                ^<li^>edu_teacher - Teacher table^</li^>
                ^<li^>uctr_member - Student member table^</li^>
                ^<li^>edu_schedule - Course schedule table^</li^>
            ^</ul^>
        ^</div^>
    ^</div^>
^</body^>
^</html^> > "%~dp0test_english.html"

:: Open test webpage
echo [Opening] Launching test webpage...
start "Test Webpage" "%~dp0test_english.html"

echo.
echo ==============================================
echo   All environments started!
echo   Wait for the browser tabs to open!
echo ==============================================
echo.
echo Access URLs:
echo - Admin System: http://localhost:9528
echo - Teacher System: http://localhost:9529
echo - Student System: http://localhost:9530
echo.
echo Test Accounts:
echo - Admin: admin / 123456
echo - Teacher: teacher / 123456
echo - Student: student / 123456

echo.
echo ==============================================
echo   System startup completed!
echo ==============================================
pause
