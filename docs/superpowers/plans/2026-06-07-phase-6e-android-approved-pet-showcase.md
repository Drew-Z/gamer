# Phase 6e Android Approved Pet Showcase Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android approved pet registry visible as a compact showcase, not only a count.

**Architecture:** Keep the Android screen simple. Add tested UI text helpers that summarize the first approved pet with score and motion sheet coverage, then render those lines in the existing community screen below the registry count.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose Material3, Android/JUnit unit tests.

---

## Files

- Modify `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`: add RED tests for showcase title and detail helpers.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`: add helpers and render showcase lines.

## Task 1: Showcase Text Helpers

- [x] **Step 1: Write failing UI helper tests**

Add tests:

```kotlin
@Test
fun approvedPetShowcaseTitleUsesFirstApprovedPet() {
    val pet = ApprovedPet(
        petId = "pet-stardust-001",
        displayName = "Stardust Dragon",
        sourceKind = "fantasy-pet-rule",
        previewPath = "previews/overall-showcase.png",
        motionSheetCount = 2,
        totalScore = 86
    )

    assertEquals("Stardust Dragon", approvedPetShowcaseTitle(listOf(pet)))
    assertEquals("Awaiting approved pet", approvedPetShowcaseTitle(emptyList()))
}
```

and:

```kotlin
@Test
fun approvedPetShowcaseDetailSummarizesScoreAndMotionSheets() {
    val pet = ApprovedPet(
        petId = "pet-stardust-001",
        displayName = "Stardust Dragon",
        sourceKind = "fantasy-pet-rule",
        previewPath = "previews/overall-showcase.png",
        motionSheetCount = 2,
        totalScore = 86
    )

    assertEquals(
        "fantasy-pet-rule / score 86 / 2 motion sheets",
        approvedPetShowcaseDetail(listOf(pet))
    )
    assertEquals(
        "Approved imports will appear here.",
        approvedPetShowcaseDetail(emptyList())
    )
}
```

- [x] **Step 2: Run UI model test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.ui.PetShellUiModelTest --console=plain
```

Expected: FAIL because helper functions do not exist.

- [x] **Step 3: Implement helpers**

In `PetShellApp.kt`, add:

```kotlin
internal fun approvedPetShowcaseTitle(pets: List<ApprovedPet>): String =
    pets.firstOrNull()?.displayName ?: "Awaiting approved pet"

internal fun approvedPetShowcaseDetail(pets: List<ApprovedPet>): String {
    val pet = pets.firstOrNull() ?: return "Approved imports will appear here."
    return "${pet.sourceKind} / score ${pet.totalScore} / ${pet.motionSheetCount} motion sheets"
}
```

- [x] **Step 4: Run UI model test to verify GREEN**

Run the same UI model test command.

Expected: PASS.

## Task 2: Render Showcase

- [x] **Step 1: Add compact showcase UI**

In `CommunityScreen()`, below the registry summary text, render:

```kotlin
Surface(
    color = Color.White,
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 1.dp,
    shadowElevation = 1.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = approvedPetShowcaseTitle(state.approvedPets),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = approvedPetShowcaseDetail(state.approvedPets),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF667085)
        )
    }
}
```

- [x] **Step 2: Run UI model test**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.ui.PetShellUiModelTest --console=plain
```

Expected: PASS.

## Task 3: Verification

- [x] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

- [x] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [x] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

- [x] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

- [x] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6e-android-approved-pet-showcase.md apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt
git commit -m "Show approved pet showcase in Android shell"
```

## Self-Review

- Spec coverage: Moves Android from approved-pet count to visible pet showcase information.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `ApprovedPet` fields.
