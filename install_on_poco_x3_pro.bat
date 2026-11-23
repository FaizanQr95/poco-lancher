@echo off
echo ===================================================
echo   PocoOS Launcher - Direct USB Installer (POCO X3 Pro)
echo ===================================================
echo.
adb devices
echo.
echo [1/3] Building Debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo Build failed. Please open this folder in Android Studio or check JDK 17.
  pause
  exit /b 1
)
echo [2/3] Installing APK via USB Debugging...
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo [3/3] Granting Overlay & Write Settings permissions...
adb shell appops set com.pocoos.launcher SYSTEM_ALERT_WINDOW allow
adb shell appops set com.pocoos.launcher WRITE_SETTINGS allow
echo.
echo Done! PocoOS Launcher is installed on your Poco X3 Pro.
pause
