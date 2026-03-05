# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All commands use the Gradle wrapper (`gradlew.bat` on Windows):

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device/emulator
./gradlew installDebug

# Run lint checks
./gradlew lint

# Run unit tests (JVM, no device needed)
./gradlew test

# Run instrumented tests (requires emulator or physical device)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.example.photofriend.ExampleUnitTest"
```

## Architecture

This is a single-module Android app using **Jetpack Compose** and **Material Design 3**.

**Package**: `com.example.photofriend`
**Min SDK**: 24 (Android 7.0) | **Target SDK**: 36 (Android 15)
**Language**: Kotlin 2.0.21 | **JVM target**: Java 11

**Entry point**: `MainActivity` (extends `ComponentActivity`) sets up the Compose content tree under `PhotofriendTheme`.

**UI layer** lives in `app/src/main/java/com/example/photofriend/`:
- `MainActivity.kt` — single activity, hosts all Compose UI
- `ui/theme/` — Material 3 theme: `Theme.kt` (dynamic color + dark/light), `Color.kt` (palette), `Type.kt` (typography)

**Dependency management** uses a version catalog at `gradle/libs.versions.toml`. Add new dependencies there rather than hardcoding versions in `build.gradle.kts`.

## Key Stack Details

- Compose BOM `2024.09.00` pins all `androidx.compose.*` versions
- Dynamic color (Material You) is enabled on Android 12+ via `DynamicColorScheme`; falls back to static `Purple40/Purple80` palette on older devices
- ProGuard is currently disabled for release builds (`isMinifyEnabled = false`)
