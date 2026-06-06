# Phase 6g Android Approved Pet Showcase Assets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android approved pet showcase communicate the selected pet position and preview asset reference.

**Architecture:** Keep the existing approved pet registry model unchanged and add UI-only helper functions that derive compact display text from `approvedPets`, `approvedPetIndex`, and `previewPath`. Render the new text inside the existing showcase surface so the current selection behaves like a real imported resource display slot.

**Tech Stack:** Kotlin, Android Compose, JUnit, existing Gradle wrapper from `floating-pet-android`.

---

### Task 1: Showcase Display Text

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Add tests for two helper functions:
- `approvedPetShowcasePosition(pets, selectedIndex)` returns `"Pet 2 of 3"` for a selected index of 1 and three pets.
- `approvedPetShowcasePosition(emptyList(), selectedIndex)` returns `"No showcase selection"`.
- `approvedPetShowcaseAsset(pets, selectedIndex)` returns `"Preview previews/moonfox.png"` for the selected pet.
- `approvedPetShowcaseAsset(emptyList(), selectedIndex)` returns `"Preview asset pending"`.

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.ui.PetShellUiModelTest --console=plain
```

Expected: FAIL because the new helper functions do not exist.

- [ ] **Step 3: Implement helper functions**

Add:
- `internal fun approvedPetShowcasePosition(pets: List<ApprovedPet>, selectedIndex: Int): String`
- `internal fun approvedPetShowcaseAsset(pets: List<ApprovedPet>, selectedIndex: Int): String`

Use the same selected-pet fallback as the existing title/detail helpers.

- [ ] **Step 4: Run test to verify GREEN**

Run the same focused Gradle command. Expected: PASS.

### Task 2: Showcase Surface Rendering

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`

- [ ] **Step 1: Render the new text**

Add two compact `Text` rows inside the existing approved pet showcase surface:
- position text from `approvedPetShowcasePosition(...)`
- asset text from `approvedPetShowcaseAsset(...)`

- [ ] **Step 2: Run Android unit tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: PASS.

### Task 3: Full Verification and Commit

**Files:**
- Verify all files above

- [ ] **Step 1: Run Node tests**

```powershell
npm.cmd test
```

- [ ] **Step 2: Validate Docker compose**

```powershell
docker compose config
```

- [ ] **Step 3: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 4: Commit**

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6g-android-approved-pet-showcase-assets.md apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt
git commit -m "Show approved pet showcase asset context"
```
