# Gamer

Pet-first community ecosystem workspace.

## Structure

```text
gamer/
  apps/
  services/
    community-api/
    pet-generator/
  packages/
    community-contracts/
    pet-package-spec/
    pet-runtime/
  docs/
```

## Local

Run tests:

```powershell
npm.cmd test
```

Run services:

```powershell
npm.cmd run start:community-api
npm.cmd run start:pet-generator
```

## Docker

Run the service skeletons with Docker Compose:

```powershell
docker compose up --build
```

The default ports are:

- Community API: `http://localhost:4000`
- Pet Generator Adapter: `http://localhost:4100`

## Android Community Prototype

The Android prototype lives in `apps/android-community`. It uses the local verified
baseline from `D:\workspace4Codex\floating-pet-android`:

- Android Gradle Plugin `9.2.0`
- Kotlin `2.2.10`
- Compose BOM `2025.12.00`
- Compile SDK `36`

Versions are centralized in:

```text
apps/android-community/gradle/libs.versions.toml
```

This workspace currently reuses the known-good local Gradle wrapper from
`D:\workspace4Codex\floating-pet-android`.

List Android projects:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community projects
```

Run Android unit tests:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Build the debug APK:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
```
