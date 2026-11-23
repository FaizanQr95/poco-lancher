# PocoOS Launcher — Xiaomi Poco X3 Pro (vayu)

## Build the APK
1. Extract this zip and open the folder in **Android Studio** (Jellyfish or newer), OR push to GitHub and download the `PocoOS-Launcher-PocoX3Pro` artifact from the **Actions** tab.
2. Connect your Poco X3 Pro with USB Debugging enabled and run:
   `./gradlew installDebug`

## Required MIUI / HyperOS Permissions
1. **Default Launcher**: Settings -> Home screen -> Default launcher -> PocoOS Launcher
2. **Display Over Other Apps**: Enable for Dynamic Island, Control Center & Status Bar
3. **Xiaomi Background Pop-up**: App info -> Other permissions -> Display pop-up windows while running in the background -> Allow
4. **Autostart & Battery**: Enable Autostart and set Battery Saver to **No restrictions**, then lock the app in Recents
5. **Notification Access**: Settings -> Privacy -> Special permissions -> Notification access -> PocoOS
6. **Accessibility Service**: Settings -> Additional settings -> Accessibility -> Downloaded apps -> PocoOS Fluid Gesture Navigation
