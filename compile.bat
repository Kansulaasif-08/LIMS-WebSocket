@echo off
cls
echo.
echo ╔═══════════════════════════════════════╗
echo ║   LIMS Compilation Starting...        ║
echo ╚═══════════════════════════════════════╝
echo.

echo Checking Java installation...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java not found!
    pause
    exit /b 1
)
echo ✓ Java found
echo.

echo Checking JAR files...
if not exist "lib\gson-2.10.1.jar" (
    echo ERROR: gson JAR not found!
    pause
    exit /b 1
)
if not exist "lib\Java-WebSocket-1.5.3.jar" (
    echo ERROR: WebSocket JAR not found!
    pause
    exit /b 1
)
echo ✓ JAR files found
echo.

echo Compiling Java files...
javac -cp "lib\*;." -d . src\*.java

if %errorlevel% neq 0 (
    echo.
    echo ✗ COMPILATION FAILED!
    pause
    exit /b 1
)

echo.
echo ╔═══════════════════════════════════════╗
echo ║   ✓ COMPILATION SUCCESSFUL!           ║
echo ╚═══════════════════════════════════════╝
echo.
echo Run "run.bat" to start the server
pause