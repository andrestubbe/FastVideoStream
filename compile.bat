@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo ===========================================
echo FastVideoStream Builder (v0.1.0)
echo ===========================================
echo.

if not defined JAVA_HOME (
    for /d %%i in ("C:\Program Files\Java\jdk-*") do set "JAVA_HOME=%%i"
)
if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME is not set and no JDK was found.
    exit /b 1
)

call mvn clean install -DskipTests -q
if errorlevel 1 (
    echo ERROR: FastVideoStream build failed.
    exit /b %errorlevel%
)

echo BUILD SUCCESSFUL!
