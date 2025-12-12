@echo off
cls
echo.
echo ╔═══════════════════════════════════════╗
echo ║   LIMS Server Starting...             ║
echo ╚═══════════════════════════════════════╝
echo.

if not exist "LIMSWebSocketServer.class" (
    echo ERROR: Compiled files not found!
    echo Please run compile.bat first
    pause
    exit /b 1
)

echo Starting server...
echo.
java -cp ".;lib\*" LIMSWebSocketServer

pause