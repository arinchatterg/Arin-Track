# Arin Track — Android Architecture & Build Guide

Arin Track is an Android application engineered for mindful Instagram Reels & YouTube Shorts tracking with a real-time floating overlay HUD and modern Jetpack Glance home screen widgets.

## Key Features
1. **Active Floating HUD**: A discreet counter badge overlays on top of the video player *only* when Instagram Reels or YouTube Shorts is active. Vanishes the moment you return to feed or switch apps.
2. **Modern Glance Widgets**: 2x1, 2x2, and 4x2 home screen widgets showing reel count, screen time, and progress.
3. **Clean Architecture**: Room SQLite (100% offline, zero network permissions), Hilt DI, Coroutines & Flow, and WorkManager midnight rollups.

## Build Instructions
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```