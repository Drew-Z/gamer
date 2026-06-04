# Phase 1 Android Pet Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android-first pet shell prototype where a pet appears during app launch, shows loading inside a speech bubble, opens the community surface on tap, and drives fixture feed navigation.

**Architecture:** Create an independent Android Compose project under `apps/android-community`. Keep behavior in small Kotlin model/controller files with unit tests, then render the prototype with a single Activity and Compose UI using local fixture data.

**Tech Stack:** Local verified Android baseline from `D:\workspace4Codex\floating-pet-android`: Android Gradle Plugin 9.2.0, Kotlin 2.2.10, Jetpack Compose BOM 2025.12.00, JUnit 4, local fixtures. Versions must be centralized in `gradle/libs.versions.toml`.

---

## File Structure

- Create `apps/android-community/settings.gradle`: Android project settings and repositories.
- Create `apps/android-community/build.gradle`: plugin versions matching the existing local Android baseline.
- Create `apps/android-community/gradle/libs.versions.toml`: centralized local verified Android baseline.
- Create `apps/android-community/gradle.properties`: AndroidX and Gradle JVM settings.
- Create `apps/android-community/local.properties`: local SDK path for this machine.
- Create `apps/android-community/app/build.gradle`: Compose app module.
- Create `apps/android-community/app/src/main/AndroidManifest.xml`: single Activity manifest.
- Create `apps/android-community/app/src/main/java/com/gamer/community/MainActivity.kt`: Android entry point.
- Create `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`: state and fixture models.
- Create `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt`: pure Kotlin state transitions.
- Create `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`: Compose UI.
- Create `apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt`: unit tests.
- Modify `README.md`: add Android prototype commands.
- Modify `.gitignore`: ignore Android build outputs.

## Task 1: Android Project Skeleton

**Files:**
- Create: `apps/android-community/settings.gradle`
- Create: `apps/android-community/build.gradle`
- Create: `apps/android-community/gradle.properties`
- Create: `apps/android-community/local.properties`
- Create: `apps/android-community/app/build.gradle`
- Create: `apps/android-community/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create Gradle files**

Use the same Android baseline as `D:\workspace4Codex\floating-pet-android`: AGP `9.2.0`, Kotlin `2.2.10`, Compose BOM `2025.12.00`, compile SDK `36`, min SDK `26`. Store those values in `gradle/libs.versions.toml`, not scattered through module build files.

- [ ] **Step 2: Verify Gradle project loads**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community projects
```

Expected: Gradle lists root project `GamerAndroidCommunity` and project `:app`.

## Task 2: Pet Shell State Model

**Files:**
- Create: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt`
- Create: `apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt`

- [ ] **Step 1: Write controller tests first**

Tests cover:

- Initial state starts on launch bubble.
- Tapping bubble opens community.
- Next/previous/skip navigation changes feed index and pet action.
- Check-in marks reward claimed and changes pet action to reward.

- [ ] **Step 2: Implement models and controller**

Create immutable state with:

- `phase`
- `petAction`
- `speechBubble`
- `feedIndex`
- `walletBalance`
- `checkInClaimed`
- `posts`

- [ ] **Step 3: Run unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Expected: controller tests pass.

## Task 3: Compose Pet Shell UI

**Files:**
- Create: `apps/android-community/app/src/main/java/com/gamer/community/MainActivity.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`

- [ ] **Step 1: Render launch bubble**

Use `ComponentActivity.setContent`, a remembered `PetShellState`, and a tappable speech bubble. Initial screen shows only the pet stage and bubble loading message.

- [ ] **Step 2: Render community shell**

After tapping the bubble, show:

- Current feed post.
- Wallet balance.
- Check-in button.
- Previous, next, and skip feed controls.
- Pet action label and speech bubble.

- [ ] **Step 3: Build debug APK**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
```

Expected: debug APK builds.

## Task 4: Docs And Verification

**Files:**
- Modify: `README.md`
- Modify: `.gitignore`

- [ ] **Step 1: Add Android commands to README**

Document:

- Gradle project listing command.
- Unit test command.
- Debug APK build command.

- [ ] **Step 2: Run final verification**

Run:

```powershell
npm.cmd test
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
docker compose config
```

Expected: all commands pass.

- [ ] **Step 3: Commit**

Run:

```powershell
git add .
git commit -m "Add Android pet shell prototype"
```

Expected: commit succeeds.

## Self-Review

Spec coverage:

- App launch pet and bubble are covered by Task 3.
- Bubble tap opens community shell in Task 2 and Task 3.
- Pet-led previous, next, and skip navigation are covered by Task 2 and Task 3.
- Wallet and daily check-in fixture behavior are covered by Task 2 and Task 3.
- Android-first local fixture prototype is covered by the independent Android project.

Placeholder scan:

- No task depends on unspecified backend behavior.

Type consistency:

- `PetShellState`, `PetAction`, `ShellPhase`, and controller method names are used consistently across tests and UI.
