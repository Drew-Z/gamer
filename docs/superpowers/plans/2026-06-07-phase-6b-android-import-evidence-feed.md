# Phase 6b Android Import Evidence Feed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Carry approved pet import evidence from the community feed API into the Android feed display.

**Architecture:** Extend approved-import feed metadata with stable evidence fields derived from the import draft summary. Android keeps its current simple DTO and mapper layer, converts those fields into audit labels, and reuses the existing feed card audit label rendering.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, Kotlin 2.2.10, kotlinx.serialization, Android/JUnit unit tests.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED assertions for import evidence metadata on approved import feed posts.
- Modify `services/community-api/src/store.js`: include source kind, preview path, and motion sheet count in `createFeedPostFromApprovedImport()`.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`: add nullable metadata DTO fields.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`: map evidence metadata into `FeedPost` audit labels.
- Modify `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt`: add RED assertions for the new labels.

## Task 1: Feed Metadata Evidence

- [x] **Step 1: Write failing store test assertions**

In `services/community-api/src/store.test.js`, update `approved import feed post includes import metadata` so the draft import summary has package-like evidence:

```js
source: {
  petId: "pet-feed-metadata-001",
  kind: "fantasy-pet-rule",
  baseIdentityStatus: "accepted"
}
```

and:

```js
assets: {
  previewPath: "previews/overall-showcase.png",
  motionSheets: ["motion/sheets/idle.png", "motion/sheets/happy_click.png"],
  exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-metadata/export.zip"
}
```

Then expect metadata:

```js
assert.equal(published.metadata.importSourceKind, "fantasy-pet-rule");
assert.equal(published.metadata.importPreviewPath, "previews/overall-showcase.png");
assert.equal(published.metadata.motionSheetCount, 2);
```

- [x] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because approved import feed metadata does not include these fields.

- [x] **Step 3: Implement feed metadata evidence**

In `services/community-api/src/store.js`, update `createFeedPostFromApprovedImport()` metadata:

```js
importSourceKind: draft?.importSummary?.source?.kind ?? "",
importPreviewPath: draft?.importSummary?.assets?.previewPath ?? "",
motionSheetCount: Array.isArray(draft?.importSummary?.assets?.motionSheets)
  ? draft.importSummary.assets.motionSheets.length
  : 0
```

- [x] **Step 4: Run store test to verify GREEN**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: PASS.

## Task 2: Android Mapper Evidence Labels

- [x] **Step 1: Write failing Android mapper assertions**

In `CommunityApiMappersTest.kt`, update `mapsApprovedImportMetadataToDisplayLabels()` metadata:

```kotlin
importSourceKind = "fantasy-pet-rule",
importPreviewPath = "previews/overall-showcase.png",
motionSheetCount = 2
```

Then assert:

```kotlin
assertEquals("Source fantasy-pet-rule", posts[0].importSourceLabel)
assertEquals("Preview previews/overall-showcase.png", posts[0].importPreviewLabel)
assertEquals("2 motion sheets", posts[0].motionSheetLabel)
```

- [x] **Step 2: Run Android mapper test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityApiMappersTest --console=plain
```

Expected: FAIL because DTO and mapper fields do not exist.

- [x] **Step 3: Add feed model fields**

In `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`, add nullable labels to `FeedPost`:

```kotlin
val importSourceLabel: String? = null,
val importPreviewLabel: String? = null,
val motionSheetLabel: String? = null
```

- [x] **Step 4: Add DTO metadata fields**

In `CommunityApiDtos.kt`, add:

```kotlin
val importSourceKind: String? = null,
val importPreviewPath: String? = null,
val motionSheetCount: Int? = null
```

- [x] **Step 5: Map metadata labels**

In `CommunityApiMappers.kt`, pass:

```kotlin
importSourceLabel = item.metadata.importSourceLabel(),
importPreviewLabel = item.metadata.importPreviewLabel(),
motionSheetLabel = item.metadata.motionSheetLabel()
```

and add helpers:

```kotlin
private fun FeedPostMetadataDto?.importSourceLabel(): String? {
    val kind = this?.importSourceKind ?: return null
    return if (kind.isNotBlank()) "Source $kind" else null
}

private fun FeedPostMetadataDto?.importPreviewLabel(): String? {
    val path = this?.importPreviewPath ?: return null
    return if (path.isNotBlank()) "Preview $path" else null
}

private fun FeedPostMetadataDto?.motionSheetLabel(): String? {
    val count = this?.motionSheetCount ?: return null
    return if (count > 0) "$count motion sheets" else null
}
```

- [x] **Step 6: Render evidence labels through existing audit area**

In `PetShellApp.kt`, update `feedPostAuditLabels(post)`:

```kotlin
listOfNotNull(
    post.importDraftLabel,
    post.submissionLabel,
    post.scoreReportLabel,
    post.importSourceLabel,
    post.importPreviewLabel,
    post.motionSheetLabel
)
```

- [x] **Step 7: Run Android mapper test to verify GREEN**

Run the Android mapper test command again.

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
git add docs/superpowers/plans/2026-06-07-phase-6b-android-import-evidence-feed.md services/community-api/src/store.js services/community-api/src/store.test.js apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt
git commit -m "Show import evidence in Android feed"
```

## Self-Review

- Spec coverage: Moves approved pet import evidence from backend publication into Android feed display.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Metadata names match API JSON, Kotlin DTOs, and `FeedPost` label fields.
