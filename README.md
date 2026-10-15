# NoteSpace

NoteSpace is an offline-first Android notes app built with Kotlin and Jetpack Compose. Organize notes into categories, customize the home screen, and keep your data stored locally on your device.

## Features

- Create and manage note categories
- Write, read, and edit notes
- Filter notes by category
- Personalize the home greeting
- Toggle between light and dark themes
- Optionally show note previews
- Store notes and preferences locally with Android SharedPreferences
- No network permission is requested

## Requirements

- Android Studio with Android SDK Platform 35
- JDK 17
- Android device or emulator running Android 8.0 (API 26) or later

The project uses Android Gradle Plugin 8.7.3, Gradle 8.11.1, and Kotlin 2.0.21.

## Build and run

1. Clone or download this repository and open the project folder in Android Studio.
2. Allow Gradle to sync and install any prompted Android SDK components.
3. Select the `app` run configuration and run it on a device or emulator.

You can also build a debug APK from the project directory:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

- `app/src/main/java/` — application source code
- `app/src/main/res/` — Android resources and app icon
- `gradle/` — Gradle wrapper configuration
- `build.gradle.kts` — root plugin configuration
- `app/build.gradle.kts` — Android app configuration and dependencies

## Data and privacy

Notes, categories, theme preference, greeting name, and other app preferences are saved on the device. Uninstalling the app or clearing its storage removes this local data. NoteSpace does not request network access.
