@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

echo [FastStream] Building CLI...
call mvn clean package -DskipTests -q
if errorlevel 1 exit /b 1

echo [FastStream] Starting CLI. Stream keys must be in FAST_YOUTUBE_KEY and/or FAST_TWITCH_KEY.
java -jar target\FastVideoStream-0.1.0.jar %*
