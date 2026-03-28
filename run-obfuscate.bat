@echo off
title Falcon - Build and Obfuscate
setlocal EnableDelayedExpansion

REM Move to the project root (where pom.xml is), regardless of where this was double-clicked
cd /d "%~dp0"

echo ============================================================
echo  Falcon Plugin - Build + Obfuscate
echo ============================================================
echo.

REM --- Check for Maven ---
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 'mvn' not found in PATH. Please install Maven or add it to your PATH.
    pause
    exit /b 1
)

REM --- Check for Java ---
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    echo [INFO] Using JAVA_HOME: %JAVA_HOME%
) else (
    set "JAVA_EXE=java"
    echo [WARN] JAVA_HOME is not set. Falling back to 'java' in PATH.
)

REM --- Step 1: Maven Build ---
echo.
echo [STEP 1/2] Building plugin with Maven (clean package)...
echo ------------------------------------------------------------
call mvn clean package -q
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Maven build failed! Check the output above for errors.
    pause
    exit /b 1
)
echo [OK] Maven build succeeded.

REM --- Check output JAR exists ---
set "INPUT_JAR=%~dp0target\Falcon-5.3.jar"
if not exist "%INPUT_JAR%" (
    echo [ERROR] Expected JAR not found: %INPUT_JAR%
    echo         Check that the finalName in pom.xml matches.
    pause
    exit /b 1
)

REM --- Step 2: Obfuscate ---
echo.
echo [STEP 2/2] Running Skidfuscator obfuscation...
echo ------------------------------------------------------------
echo [INFO] Input:  %INPUT_JAR%
echo [INFO] This may take 5-15 minutes depending on your PC.
echo.
call "%~dp0build_scripts\obfuscate.bat" "%INPUT_JAR%"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Obfuscation failed! Check the output above.
    echo         Error logs are in: build_scripts\skidfuscator-error-*.txt
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  SUCCESS! Obfuscated JAR is ready:
echo  %INPUT_JAR%
echo ============================================================
echo.
pause
exit /b 0
