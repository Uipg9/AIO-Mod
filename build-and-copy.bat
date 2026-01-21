@echo off
REM Auto-copy AIO Mod JAR to Complete mods folder after build

echo.
echo ========================================
echo   AIO Mod - Build and Copy Script
echo ========================================
echo.

cd /d "C:\Users\baesp\Desktop\MCMODS\AIO\aio-mod"

echo [1/3] Running Gradle build...
call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed! Check errors above.
    pause
    exit /b 1
)

echo.
echo [2/3] Copying JAR to Complete mods folder...
copy /Y "build\libs\aio-mod-1.0.0.jar" "C:\Users\baesp\Desktop\MCMODS\Complete mods\AIO Mod\aio-mod-1.0.1.jar"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Failed to copy JAR file!
    pause
    exit /b 1
)

echo.
echo [3/3] Done!
echo.
echo JAR Location: C:\Users\baesp\Desktop\MCMODS\Complete mods\AIO Mod\aio-mod-1.0.1.jar
echo.
echo ========================================
echo   Build Complete!
echo ========================================
echo.
pause
