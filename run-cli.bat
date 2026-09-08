@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

echo [FastVideoStream] Building CLI...
call mvn clean package -DskipTests -q
if errorlevel 1 exit /b 1

if not exist "target\cp.txt" call mvn dependency:build-classpath "-Dmdep.outputFile=target/cp.txt" -q
if errorlevel 1 exit /b 1
set /p APP_CP=<target\cp.txt

echo [FastVideoStream] Starting headless CLI.
java -cp "target\classes;%APP_CP%" fastvideostream.FastVideoStreamCli %*
