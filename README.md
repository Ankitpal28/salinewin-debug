# salinewin Android

Screen color filter + crosshair overlay. Controls Windows overlay via IPC.

## Build options

### A — GitHub Actions (easiest, no setup)
1. Push this folder to a GitHub repo
2. Actions → Build APK → Run workflow
3. Download APK from Artifacts

### B — Android Studio
Open folder → Run ▶

### C — Termux (on-device)
```bash
pkg install openjdk-17 gradle
# Install Android SDK via sdkmanager
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### D — Command line (Linux/macOS with Android SDK)
```bash
export ANDROID_HOME=~/Android/Sdk
chmod +x gradlew
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Grant overlay permission when prompted
2. Tap START OVERLAY
3. Tap anywhere on overlay to toggle
4. Swipe left/right = hue ±15°
5. Swipe up = brightness +10%
6. Swipe down = reset

## Windows IPC (Remote Control tab)
```bash
# Run once per ADB session
adb forward tcp:9999 tcp:9999
```
Then tap REMOTE CONTROL → CONNECT
