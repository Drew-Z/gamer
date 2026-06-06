# Phase 6f Android Approved Pet Showcase Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Android pet shell browse multiple approved imported pets in the showcase instead of always rendering the first approved pet.

**Architecture:** Add a showcase index to `PetShellState`, clamp/reset it when community data loads, and expose a controller method that wraps previous/next navigation across approved pets. Update UI helper functions to render the selected approved pet and add compact controls in the existing showcase surface.

**Tech Stack:** Kotlin, Android Compose, JUnit, existing Gradle wrapper from `floating-pet-android`.

---

### Task 1: State Navigation

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt`

- [ ] **Step 1: Write the failing tests**

Add tests that expect:
- `initialState()` starts with `approvedPetIndex == 0`
- `navigateApprovedPet(..., FeedDirection.Next)` moves from index 0 to 1
- `navigateApprovedPet(..., FeedDirection.Previous)` wraps from index 0 to the last pet
- empty approved pet lists keep index 0 and explain that no approved pets are ready
- `applyCommunityLoad(...)` resets `approvedPetIndex` to 0

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.petshell.PetShellControllerTest --console=plain
```

Expected: FAIL because `approvedPetIndex` and `navigateApprovedPet` do not exist yet.

- [ ] **Step 3: Implement state navigation**

Add:
- `approvedPetIndex: Int` to `PetShellState`
- `PetAction.ShowcaseNext` and `PetAction.ShowcasePrevious`
- `PetShellController.navigateApprovedPet(state, direction)`
- reset `approvedPetIndex = 0` in `initialState()` and `applyCommunityLoad(...)`

- [ ] **Step 4: Run test to verify GREEN**

Run the same focused Gradle command. Expected: PASS.

### Task 2: UI Showcase Selection

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Update UI model tests to expect:
- `approvedPetShowcaseTitle(pets, selectedIndex)` uses the selected pet
- `approvedPetShowcaseDetail(pets, selectedIndex)` uses the selected pet
- out-of-range selected indexes coerce to a valid pet
- empty lists keep the existing empty title/detail

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.ui.PetShellUiModelTest --console=plain
```

Expected: FAIL because helper signatures still only accept `List<ApprovedPet>`.

- [ ] **Step 3: Implement UI selection and controls**

Update:
- `CommunityScreen` accepts an `onShowcaseNavigate` callback
- showcase title/detail call helper functions with `state.approvedPetIndex`
- add Prev/Next buttons inside the showcase surface
- `PetShellApp` wires buttons to `PetShellController.navigateApprovedPet`

- [ ] **Step 4: Run test to verify GREEN**

Run the same focused Gradle command. Expected: PASS.

### Task 3: Full Verification and Commit

**Files:**
- Verify all files above

- [ ] **Step 1: Run Android unit tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [ ] **Step 2: Run Node tests**

```powershell
npm.cmd test
```

- [ ] **Step 3: Validate Docker compose**

```powershell
docker compose config
```

- [ ] **Step 4: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 5: Commit**

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6f-android-approved-pet-showcase-navigation.md apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt
git commit -m "Add approved pet showcase navigation"
```
