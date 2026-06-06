# Phase 6o Feed Export Artifact Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Carry approved pet package export artifact paths into community feed metadata and Android feed audit labels.

**Architecture:** The community API already maps imported package exports into import drafts and approved pet registry assets. Extend the approved-import feed post metadata with `exportArtifactPath`, then teach Android to decode that metadata and render it as an audit label alongside preview and motion sheet evidence.

**Tech Stack:** Node.js test runner, Kotlin serialization DTOs, Android JVM unit tests, Jetpack Compose UI model helpers.

---

### Task 1: Community API Feed Metadata

**Files:**
- Modify: `services/community-api/src/store.test.js`
- Modify: `services/community-api/src/store.js`

- [ ] **Step 1: Write the failing feed metadata test**

In `approved import feed post includes import metadata`, extend the expected metadata object with:

```js
exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-metadata/export.zip"
```

- [ ] **Step 2: Run focused store tests to verify RED**

```powershell
npm.cmd test -- services/community-api/src/store.test.js
```

Expected: FAIL because `createFeedPostFromApprovedImport()` does not yet include `metadata.exportArtifactPath`.

- [ ] **Step 3: Implement feed metadata mapping**

In `createFeedPostFromApprovedImport()`, add:

```js
exportArtifactPath: draft?.importSummary?.assets?.exportArtifactPath ?? "",
```

beside `importPreviewPath`.

- [ ] **Step 4: Run focused store tests to verify GREEN**

```powershell
npm.cmd test -- services/community-api/src/store.test.js
```

Expected: PASS.

### Task 2: Android Feed Audit Label

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`

- [ ] **Step 1: Write failing Android mapper and UI tests**

In `mapsApprovedImportMetadataToDisplayLabels`, set:

```kotlin
exportArtifactPath = "exports/stardust-package.zip"
```

and assert:

```kotlin
assertEquals("Package exports/stardust-package.zip", posts[0].exportArtifactLabel)
```

In `feedPostAuditLabelsReturnsImportReferences`, set:

```kotlin
exportArtifactLabel = "Package exports/stardust-package.zip"
```

and include that label in the expected list after the preview label.

- [ ] **Step 2: Run Android tests to verify RED**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: FAIL at compile/test time because feed post metadata has no export artifact DTO/model/label yet.

- [ ] **Step 3: Implement Android metadata decoding and UI label**

Add nullable `exportArtifactPath` to `FeedPostMetadataDto`, add nullable `exportArtifactLabel` to `FeedPost`, map it through `toFeedPosts()`, and include it in `feedPostAuditLabels()`.

- [ ] **Step 4: Run Android tests to verify GREEN**

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
git add docs/superpowers/plans/2026-06-07-phase-6o-feed-export-artifact-metadata.md services/community-api/src/store.test.js services/community-api/src/store.js apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt
git commit -m "Show export artifacts in feed metadata"
```

---

## Self-Review

- Spec coverage: Extends the approved import feed path and Android feed display path with package export artifact evidence.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: API metadata and Android DTO use `exportArtifactPath`; Android shell model uses display-oriented `exportArtifactLabel`.
