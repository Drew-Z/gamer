# Phase 6l Approved Pet Export Artifact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Carry the approved pet export artifact path from import evidence into the approved pet registry and Android showcase.

**Architecture:** Keep the existing `/v1/pets/approved` API route and approved pet registry shape. Add `assets.exportArtifactPath` as an optional asset field sourced from import draft evidence, map it through Android DTOs into the `ApprovedPet` shell model, and display a compact package reference in the Android approved pet showcase.

**Tech Stack:** Node.js test runner, Kotlin serialization DTOs, Android unit tests, Jetpack Compose text helpers.

---

### Task 1: Backend Approved Registry Export Artifact

**Files:**
- Modify: `services/community-api/src/store.test.js`
- Modify: `services/community-api/src/server.test.js`
- Modify: `services/community-api/src/store.js`

- [ ] **Step 1: Write failing store test**

Add a test in `services/community-api/src/store.test.js` that creates a community-ready import draft with:

```js
assets: {
  previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/preview.html",
  exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/export.zip"
}
```

Submit and approve it, then assert:

```js
assert.equal(
  pet.assets.exportArtifactPath,
  "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/export.zip"
);
```

- [ ] **Step 2: Run store test to verify RED**

```powershell
npm.cmd test -- services/community-api/src/store.test.js
```

Expected: FAIL because approved pet assets do not yet include `exportArtifactPath`.

- [ ] **Step 3: Implement store field**

In `createApprovedPetFromImport(...)`, add:

```js
exportArtifactPath: draft?.importSummary?.assets?.exportArtifactPath ?? "",
```

inside the returned `assets` object.

- [ ] **Step 4: Add HTTP route assertion**

In `services/community-api/src/server.test.js`, extend the approved pet registry HTTP test to assert:

```js
assert.equal(
  response.body.items[0].assets.exportArtifactPath,
  "D:/workspace4Codex/fantasy-pet-rule/runs/approved-registry/export.zip"
);
```

Use an import draft payload that includes that `exportArtifactPath`, then submit and approve it before calling `GET /v1/pets/approved`.

- [ ] **Step 5: Run Node focused tests**

```powershell
npm.cmd test -- services/community-api/src/store.test.js services/community-api/src/server.test.js
```

Expected: PASS.

### Task 2: Android DTO and Model Mapping

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/api/HttpCommunityApiClientTest.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt`

- [ ] **Step 1: Write failing Android decode and mapper tests**

Update approved pets JSON in `HttpCommunityApiClientTest.decodesApprovedPetsJson`:

```json
"assets": {
  "previewPath": "previews/overall-showcase.png",
  "motionSheetCount": 2,
  "exportArtifactPath": "exports/stardust.zip"
}
```

Assert:

```kotlin
assertEquals("exports/stardust.zip", response.items[0].assets.exportArtifactPath)
```

Update `CommunityApiMappersTest.mapsApprovedPetRegistryToShellModels` with the same field and assert:

```kotlin
assertEquals("exports/stardust.zip", pets[0].exportArtifactPath)
```

- [ ] **Step 2: Run Android tests to verify RED**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: FAIL because DTO/model fields are missing.

- [ ] **Step 3: Implement Android fields**

Add to `ApprovedPetAssetsDto`:

```kotlin
val exportArtifactPath: String = ""
```

Add to `ApprovedPet`:

```kotlin
val exportArtifactPath: String
```

Map it in `toApprovedPets()`:

```kotlin
exportArtifactPath = item.assets.exportArtifactPath,
```

Update test helper constructors to pass `exportArtifactPath = ""` by default.

- [ ] **Step 4: Run Android tests to verify GREEN**

Run the same Android test command. Expected: PASS.

### Task 3: Android Showcase Package Reference

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`

- [ ] **Step 1: Write failing UI helper test**

Add a test:

```kotlin
@Test
fun approvedPetShowcasePackageShowsSelectedExportArtifactPath() {
    val pets = listOf(
        approvedPet("pet-stardust-001", "Stardust Dragon"),
        approvedPet(
            petId = "pet-moonfox-001",
            displayName = "Moon Fox",
            exportArtifactPath = "exports/moonfox.zip"
        )
    )

    assertEquals("Package exports/moonfox.zip", approvedPetShowcasePackage(pets, selectedIndex = 1))
    assertEquals("Package artifact pending", approvedPetShowcasePackage(emptyList(), selectedIndex = 1))
}
```

- [ ] **Step 2: Run Android tests to verify RED**

Run Android unit tests. Expected: FAIL because `approvedPetShowcasePackage` does not exist.

- [ ] **Step 3: Implement helper and render row**

Add:

```kotlin
internal fun approvedPetShowcasePackage(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Package artifact pending"
    return if (pet.exportArtifactPath.isBlank()) {
        "Package artifact pending"
    } else {
        "Package ${pet.exportArtifactPath}"
    }
}
```

Render a `Text` row below `approvedPetShowcaseAsset(...)`.

- [ ] **Step 4: Run Android tests to verify GREEN**

Run Android unit tests. Expected: PASS.

### Task 4: Full Verification and Commit

**Files:**
- Verify all files above

- [ ] **Step 1: Run Node tests**

```powershell
npm.cmd test
```

- [ ] **Step 2: Run Android tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
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
git add docs/superpowers/plans/2026-06-07-phase-6l-approved-pet-export-artifact.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/server.test.js apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/api/HttpCommunityApiClientTest.kt apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt
git commit -m "Carry approved pet export artifacts to Android"
```

---

## Self-Review

- Spec coverage: Moves approved pet registry from preview-only asset evidence toward importable/export artifact evidence, supporting future desktop pet package loading.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: `exportArtifactPath` is used consistently across backend `assets`, Android DTOs, shell model, mapper, and UI helper.
