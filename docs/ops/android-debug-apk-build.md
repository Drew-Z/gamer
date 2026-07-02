# Android Debug APK Build Record

This document records the latest local debug APK build for the Gamer Pet Android
community prototype. It is not a public release note and does not approve APK
publication.

## 2026-07-02 Local Debug Build

- Project: `apps/android-community`
- Command: `.\gradlew.bat assembleDebug --console=plain --rerun-tasks`
- Result: build succeeded
- Artifact: `apps/android-community/app/build/outputs/apk/debug/app-debug.apk`
- Size: `19,118,482` bytes (`18.23` MiB)
- SHA-256: `0887407AC4E8561196F36D2D98690E0ACA6DDFF7FF9B8D0B167A995C27E61EC5`
- Build note: Gradle reported that `libandroidx.graphics.path.so` could not be
  stripped and was packaged as-is.

## Publication Gate

Do not copy this debug APK into a public static site or enable a public download
button from it. A public APK still requires:

- release build policy and version name/code;
- signing policy and checksum disclosure;
- home, hatchery, community, profile, and package gate regression evidence;
- release notes describing current limitations;
- explicit human approval for public distribution.
