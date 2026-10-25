@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo   FastVideoStream Showcase Demo
echo ===================================================
echo.

echo [1/2] Building FastVideoStream...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastVideoStream build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Running Demo...
cd examples\Demo
call mvn compile exec:java -DskipTests -q

cd ..\..
pause
