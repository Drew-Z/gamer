# Phase 5g Android Approved Import Feed Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make approved fantasy-pet imports visibly distinct in the Android community feed.

**Architecture:** The community API adds compact metadata to feed posts published from approved import drafts. Android decodes optional feed metadata, maps it into the pet shell model, and renders a small approved-import signal without changing the existing live-feed fallback flow.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, Kotlin 2.2.10, kotlinx.serialization, Jetpack Compose Material 3.

---

## Files

- Modify `services/community-api/src/store.test.js`: assert approved import feed posts include metadata.
- Modify `services/community-api/src/store.js`: attach import metadata when publishing approved import posts.
- Modify `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt`: assert metadata maps into Android feed posts.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`: add optional feed metadata DTO.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`: map metadata to display fields.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`: add optional feed display metadata.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`: render import badge and reward text.

## Task 1: Backend Feed Metadata

- [ ] **Step 1: Add failing store test**

Add a test that approves a community-ready import draft and asserts the published feed post includes:

```js
{
  metadata: {
    sourceType: "approved-import",
    importDraftId: draft.id,
    submissionId: submissionResult.submission.id,
    scoreReportId: draft.scoreReportId,
    rewardAmount: 80
  }
}
```

- [ ] **Step 2: Run store tests**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because `metadata` is missing.

- [ ] **Step 3: Implement feed metadata**

Update `createFeedPostFromApprovedImport(submission, draft, scoreReport)` to attach the metadata above. Keep existing feed fields unchanged.

- [ ] **Step 4: Run store tests**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: PASS.

## Task 2: Android Mapping

- [ ] **Step 1: Add failing mapper test**

Add a mapper test with `FeedPostDto(metadata = FeedPostMetadataDto(...))` and assert:

```kotlin
assertEquals("Approved import", posts[0].sourceLabel)
assertEquals("+80 petcoin", posts[0].rewardLabel)
```

- [ ] **Step 2: Run mapper tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityApiMappersTest --console=plain
```

Expected: FAIL because metadata DTO/model fields are missing.

- [ ] **Step 3: Implement Android DTO and mapper**

Add `FeedPostMetadataDto` with nullable fields and add nullable `sourceLabel` / `rewardLabel` fields to `FeedPost`.

Mapping rules:

- `sourceType == "approved-import"` -> `sourceLabel = "Approved import"`.
- `rewardAmount > 0` -> `rewardLabel = "+<amount> petcoin"`.
- Missing metadata keeps both labels `null`.

- [ ] **Step 4: Run mapper tests**

Run the mapper test command again.

Expected: PASS.

## Task 3: Android UI

- [ ] **Step 1: Render optional metadata**

In the feed card, render the source label and reward label only when non-null.

- [ ] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

## Task 4: Verification

- [ ] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [ ] **Step 2: Run Docker config validation**

Run:

```powershell
docker compose config
```

Expected: config renders all services.

- [ ] **Step 3: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only this phase's files are modified.

- [ ] **Step 4: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5g-android-approved-import-feed-metadata.md services/community-api/src/store.js services/community-api/src/store.test.js apps/android-community/app/src/main/java/com/gamer/community/api apps/android-community/app/src/main/java/com/gamer/community/petshell apps/android-community/app/src/main/java/com/gamer/community/ui apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt
git commit -m "Surface approved import metadata in Android feed"
```

Expected: commit created.

## Self-Review

- Spec coverage: Extends the integrated MVP path so approved generated pets are identifiable in the Android feed.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `metadata`, `sourceType`, `importDraftId`, `submissionId`, `scoreReportId`, and `rewardAmount` are used consistently across backend and Android DTOs.
