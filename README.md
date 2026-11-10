# ModernSafe

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![minSdk](https://img.shields.io/badge/minSdk-33%20%28Android%2013%2B%29-green.svg)](Safe/build.gradle.kts)
[![targetSdk](https://img.shields.io/badge/targetSdk-35-orange.svg)](Safe/build.gradle.kts)

ModernSafe is an offline, privacy-first password manager for Android. It is the modern successor to the classic open-source [OI Safe](https://www.openintents.org/safe) by OpenIntents, rebuilt from the ground up in 100% Kotlin with Jetpack Compose and state-of-the-art cryptography.

---

## Key Features

* **Zero Network Permissions**: ModernSafe does not request `android.permission.INTERNET`. Your vault stays strictly on your device—no accounts, no telemetry, no analytics, and no cloud synchronization.
* **Modern Cryptography**: Vaults are encrypted using authenticated **AES-256-GCM** with **PBKDF2** key derivation.
* **Android Autofill**: System-level integration via `AutofillService` allows quick and secure filling of usernames and passwords into apps and browsers.
* **Biometric Unlock**: Secure and fast authentication using fingerprint or face unlock via `BiometricPrompt`.
* **Jetpack Compose & Material 3**: Single-Activity architecture with edge-to-edge rendering, dark mode support, and customizable dynamic color palettes.
* **Auto-Lock Security**: Instantly locks on device screen off, system lock, or after a configurable inactivity timeout.
* **Clipboard Protection**: Masks copied passwords in system clipboard previews and provides timed clipboard clearing.
* **Backup & Migration**:
  * Full support for encrypted XML backups.
  * Transparent, automatic upgrade of legacy OI Safe backups to the new AES-256-GCM vault format.
  * Plaintext CSV import and export capabilities.

---

## Desktop Decryption Utility

If you are a legacy OI Safe user who needs to recover or extract your password vault on a computer without running the Android application, ModernSafe provides a standalone Python GUI tool:

* See [README_DECRYPT.md](README_DECRYPT.md) for requirements and usage instructions.
* Run [`decrypt_gui.py`](decrypt_gui.py) on Windows, macOS, or Linux to decrypt and export `.xml` backups directly to CSV.

---

## Building from Source

### Prerequisites
* **JDK 17 or higher** (JDK 17 or JDK 21 recommended)
* **Android SDK** with platform tools (minSdk 33, targetSdk 35, compileSdk 37)

### Build Commands

To build the debug APK:
```bash
./gradlew assembleDebug
```
The resulting APK will be placed in `Safe/build/outputs/apk/debug/`.

To build the release APK (with R8 minification and resource shrinking):
```bash
./gradlew assembleRelease
```
The output will be in `Safe/build/outputs/apk/release/`.

### Testing

Run local unit tests:
```bash
./gradlew testDebugUnitTest
```

Run connected instrumentation tests (requires a connected device or emulator):
```bash
./gradlew connectedAndroidTest
```

---

## License & Credits

ModernSafe is open-source software licensed under the [Apache License, Version 2.0](LICENSE).

ModernSafe builds upon the original work of **OI Safe**:
* Copyright 2007–2012 Steven Osborn, Randy McEoin, and OpenIntents contributors.
* Copyright 2026 ModernSafe contributors.
* Key icon artwork by [David Vignoni](http://www.icon-king.com/) licensed under GNU LGPL v2.1.
* See [NOTICE](NOTICE) for full attribution details.
