@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

echo [FastVideoStream] Building CLI...
call mvn clean package -DskipTests -q
if errorlevel 1 exit /b 1

call mvn dependency:build-classpath "-Dmdep.outputFile=target/cp.txt" -q
if errorlevel 1 exit /b 1
powershell -NoProfile -Command "$cp = 'target\classes;' + (Get-Content -Raw 'target\cp.txt').Trim(); @('-cp', $cp) | Set-Content -Encoding ASCII 'target\run_cp_args.txt'"
echo [FastVideoStream] Starting headless CLI.
java @target\run_cp_args.txt fastvideostream.FastVideoStream %*
