# MAD Launcher — native Android (Kotlin + Jetpack Compose)

Open source under the [Apache License 2.0](LICENSE).

Source: https://github.com/madx131/MadLauncher

Clean rebuild of the MAD Launcher UI **without Expo / React Native**.

## Features

- Nordic dark home screen (clock + greeting)
- Dock: Phone · Browser · Action Box · Apps · Settings
- App drawer powered by real installed apps (`PackageManager`)
- Notifications panel (sample data)
- Quick settings toggles (UI state)
- Action Box commands: `#call`, `@msg`, `/n note`, `/h help`, or web search
- Registered as a **HOME / launcher** candidate

## Build

```bat
cd /d S:\projects\MadLauncher
gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

Release:

```bat
gradlew.bat assembleRelease
```

## Install on emulator / device

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.madlauncher/.MainActivity
```

Package ID: `com.madlauncher`
