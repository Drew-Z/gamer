# Phase 5j Android Approved Import Audit Summary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show compact approved-import audit references in the Android community feed.

**Architecture:** Keep backend feed metadata unchanged from Phase 5g. Android maps optional `importDraftId`, `submissionId`, and `scoreReportId` metadata into display labels on `FeedPost`, then renders those labels below the existing approved-import and reward pills.

**Tech Stack:** Kotlin 2.2.10, kotlinx.serialization, Jetpack Compose Material 3, Android unit tests.

---

## Files

- Modify `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt`: assert approved import metadata maps to audit labels.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`: add optional audit label fields to `FeedPost`.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`: derive compact audit labels from feed metadata.
- Modify `apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt`: assert UI detail labels omit missing metadata and include present audit labels.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`: render audit labels in the feed card.

## Task 1: Android Mapper Audit Labels

- [ ] **Step 1: Write failing mapper test**

Add assertions to `mapsApprovedImportMetadataToDisplayLabels()`:

```kotlin
assertEquals("Draft import-draft-local-001", posts[0].importDraftLabel)
assertEquals("Submission submission-local-002", posts[0].submissionLabel)
assertEquals("Score score-import-draft-local-001", posts[0].scoreReportLabel)
```

- [ ] **Step 2: Run mapper test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityApiMappersTest --console=plain
```

Expected: FAIL because `FeedPost` does not expose the new audit label fields.

- [ ] **Step 3: Implement mapper fields**

Add nullable fields to `FeedPost`:

```kotlin
val importDraftLabel: String? = null,
val submissionLabel: String? = null,
val scoreReportLabel: String? = null
```

Map metadata:

```kotlin
importDraftLabel = item.metadata.importDraftLabel()
submissionLabel = item.metadata.submissionLabel()
scoreReportLabel = item.metadata.scoreReportLabel()
```

- [ ] **Step 4: Run mapper test to verify GREEN**

Run the mapper test command again.

Expected: PASS.

## Task 2: Android UI Detail Labels

- [ ] **Step 1: Write failing UI model test**

Add a test requiring:

```kotlin
assertEquals(
    listOf("Draft import-draft-local-001", "Submission submission-local-002", "Score score-import-draft-local-001"),
    feedPostAuditLabels(post)
)
```

- [ ] **Step 2: Run UI model test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.ui.PetShellUiModelTest --console=plain
```

Expected: FAIL because `feedPostAuditLabels` does not exist.

- [ ] **Step 3: Implement UI helper and rendering**

Add:

```kotlin
internal fun feedPostAuditLabels(post: FeedPost): List<String> =
    listOfNotNull(post.importDraftLabel, post.submissionLabel, post.scoreReportLabel)
```

Render those labels below the body as compact secondary text.

- [ ] **Step 4: Run UI model test to verify GREEN**

Run the UI model test command again.

Expected: PASS.

## Task 3: Verification

- [ ] **Step 1: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run Node tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [ ] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

Expected: config renders all services.

- [ ] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only this phase's files are modified.

- [ ] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5j-android-approved-import-audit-summary.md apps/android-community
git commit -m "Show approved import audit summary on Android"
```

Expected: commit created.

## Self-Review

- Spec coverage: Extends the approved import feed surface so Android users can see the draft, submission, and score report chain for generated imports.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `importDraftLabel`, `submissionLabel`, `scoreReportLabel`, and `feedPostAuditLabels` are used consistently across tests, mapper, model, and UI.
