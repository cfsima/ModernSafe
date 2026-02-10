# OI Safe

Password manager for Android.
Please see https://www.openintents.org/safe for description using Trivium streaming encryption.

## Build Instructions

### Prerequisites
*   **JDK 17 or higher**: Android Gradle Plugin 8.2 requires JDK 17 to run.
*   **Android SDK**: Ensure you have the Android SDK installed. This project targets SDK 35.

### Building
To build the project, run:

```bash
./gradlew assembleDebug
```

The output APK will be located in `Safe/build/outputs/apk/debug/`.

### Testing
To run unit tests:

```bash
./gradlew test
```

To run instrumentation tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

### Installation
To install the debug APK on a connected device:

```bash
./gradlew installDebug
```

## Release to Google Play alpha channel
Just push to master.
