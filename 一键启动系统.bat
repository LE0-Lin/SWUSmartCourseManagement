@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "ROOT=%~dp0"
set "SERVER_DIR=%ROOT%SmartCourse-Server"
set "APP_DIR=%ROOT%SmartCourse-App"
set "TEACHER_DIR=%ROOT%SmartCourse-Teacher"
set "ADMIN_DIR=%ROOT%SmartCourse-Admin"
set "JAR=%SERVER_DIR%\target\online-edu-0.0.1-SNAPSHOT.jar"

title 智能课程管理系统一键启动

echo ==================================================
echo      智能课程管理系统 - 一键启动脚本
echo ==================================================
echo.

echo [1/7] 检查 Java...
where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未检测到 Java。请先安装 JDK 8 或 JDK 11，并配置 PATH。
  pause
  exit /b 1
)

echo [2/7] 检查 Node.js / npm...
where npm >nul 2>nul
if errorlevel 1 (
  echo [错误] 未检测到 Node.js。请先安装 Node.js 14/16，并配置 PATH。
  pause
  exit /b 1
)

echo [3/7] 启动 Redis...
if exist "%ROOT%Redis\redis-server.exe" (
  start "SmartCourse-Redis" cmd /k "cd /d "%ROOT%Redis" && redis-server.exe redis.windows.conf"
) else (
  echo [提示] 未找到内置 Redis，若本机已有 Redis 可忽略。
)
timeout /t 2 >nul

echo.
set /p MYSQL_PWD=请输入本机 MySQL root 密码（无密码直接回车）：

echo.
set /p INIT_DB=是否导入/重置演示数据库 online_edu？首次运行请输入 Y，已导入请输入 N：
if /i "%INIT_DB%"=="Y" (
  call :init_db
)

echo.
echo [4/7] 检查后端 JAR...
if not exist "%JAR%" (
  echo [提示] 未找到后端 JAR，尝试自动打包...
  cd /d "%SERVER_DIR%"
  call mvn -q -DskipTests package
  if errorlevel 1 (
    echo [错误] 后端打包失败。请检查 Maven/JDK 环境。
    pause
    exit /b 1
  )
)

echo [5/7] 启动 SpringBoot 后端...
start "SmartCourse-Backend" cmd /k "cd /d "%SERVER_DIR%" && java -jar "%JAR%" --spring.datasource.password=%MYSQL_PWD%"
timeout /t 5 >nul

echo [6/7] 启动三端前端...
call :start_vue "%APP_DIR%" "SmartCourse-Student"
call :start_vue "%TEACHER_DIR%" "SmartCourse-Teacher"
call :start_vue "%ADMIN_DIR%" "SmartCourse-Admin"

echo.
echo [7/7] 启动完成，请等待前端窗口编译完成后访问：
echo 学生端：http://localhost:9530
echo 教师端：http://localhost:9529
echo 管理端：http://localhost:9528
echo.
echo 常用账号见：TEST_ACCOUNTS.txt
echo 推荐学生演示账号：222023321102103 / 123456
echo 推荐教师演示账号：13800138002 / 123456
echo 管理员账号：admin / 123456
echo.
pause
exit /b 0

:start_vue
set "DIR=%~1"
set "TITLE=%~2"
if not exist "%DIR%\node_modules" (
  start "%TITLE%" cmd /k "cd /d "%DIR%" && npm install --registry=https://registry.npmmirror.com --legacy-peer-deps && npm run dev"
) else (
  start "%TITLE%" cmd /k "cd /d "%DIR%" && npm run dev"
)
timeout /t 1 >nul
exit /b 0

:init_db
echo [数据库] 正在查找 mysql.exe...
set "MYSQL_CMD=mysql"
where mysql >nul 2>nul
if errorlevel 1 (
  set "MYSQL_CMD="
  for %%p in ("C:\Program Files\MySQL\MySQL Server 8.0\bin" "C:\Program Files\MySQL\MySQL Server 5.7\bin" "D:\Program Files\MySQL\MySQL Server 8.0\bin" "C:\phpstudy_pro\Extensions\MySQL5.7.26\bin" "D:\phpstudy_pro\Extensions\MySQL5.7.26\bin") do (
    if exist "%%~p\mysql.exe" set "MYSQL_CMD=%%~p\mysql.exe"
  )
)
if "%MYSQL_CMD%"=="" (
  echo [错误] 未找到 mysql.exe。请把 MySQL bin 目录加入 PATH，或手动导入 SmartCourse-Server\schema.sql。
  pause
  exit /b 1
)

"%MYSQL_CMD%" -uroot -p%MYSQL_PWD% -e "DROP DATABASE IF EXISTS online_edu; CREATE DATABASE online_edu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 (
  echo [错误] MySQL 连接失败，请检查 root 密码或 MySQL 服务是否启动。
  pause
  exit /b 1
)
"%MYSQL_CMD%" -uroot -p%MYSQL_PWD% online_edu < "%SERVER_DIR%\schema.sql"
if errorlevel 1 (
  echo [错误] 数据库导入失败。
  pause
  exit /b 1
)
echo [数据库] 演示数据库导入完成。
exit /b 0
