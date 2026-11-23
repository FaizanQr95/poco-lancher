#!/usr/bin/env bash
set -e

echo "Checking connected Poco X3 Pro via ADB..."
adb devices

echo "[1/3] Building Debug APK..."
./gradlew assembleDebug

echo "[2/3] Installing APK via USB Debugging..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "[3/3] Granting Overlay & Write Settings permissions..."
adb shell appops set com.pocoos.launcher SYSTEM_ALERT_WINDOW allow
adb shell appops set com.pocoos.launcher WRITE_SETTINGS allow

echo "Done! Launch PocoOS Launcher on your Poco X3 Pro."
