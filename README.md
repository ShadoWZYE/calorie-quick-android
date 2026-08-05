# Calorie Quick

An Android calorie tracker designed around one product rule: logging food must be faster than deciding not to log it.

This repository currently contains a first vertical-slice prototype: a launch-to-Today flow, local food search, one-tap serving presets, live calories/macros, removal, and English/Romanian resources.

## Run locally

Prerequisites: Android Studio 2026.1.3 or newer, Android SDK 37, and an Android 6.0+ device/emulator.

1. Open this folder in Android Studio.
2. Allow Gradle sync to finish.
3. Create or select an emulator, then run the `app` configuration.

Command line checks:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

## Product principles

- Open directly to today's log; no dashboard detour.
- Local-first reads and writes; the core log works offline.
- Rank personal history before global catalogue results.
- Make the common case one tap and correction equally cheap.
- Treat calorie targets as estimates, explain how they were derived, and avoid medical claims.
- Ask for camera, health, and notification permissions only at the moment their feature is used.

See [docs/PRODUCT.md](docs/PRODUCT.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/ROADMAP.md](docs/ROADMAP.md), and [docs/RESEARCH.md](docs/RESEARCH.md).

