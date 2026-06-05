# Phase 5c Approved Import Feed Publishing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish an approved imported pet into the community feed so Android live API consumers can see reviewed generator output.

**Architecture:** Keep publishing inside `community-api` store state for the local MVP. Import drafts remain the source of generator evidence, submitted drafts keep a link to their submission, and approval creates one stable feed post per submission.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory `community-api` store and routes.

---

## Files

- Modify `services/community-api/src/store.test.js`: cover feed publication after approving a submitted import draft and duplicate-approval idempotency.
- Modify `services/community-api/src/routes.test.js`: cover the HTTP-style route flow from import draft creation through approval to feed visibility.
- Modify `services/community-api/src/store.js`: preserve import draft linkage on submission, publish one feed post when an imported submission is approved, and expose it through `getFeed()`.

## Task 1: Store Feed Publishing Behavior

- [ ] **Step 1: Add failing store tests**

Add tests that create a community-ready import draft, submit it, approve it, and assert `getFeed()` contains a new post for the imported pet. Add a second test that approving the same submission twice does not duplicate the feed post.

- [ ] **Step 2: Run store tests to verify failure**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because approval does not publish imported submissions into `feedPosts`.

- [ ] **Step 3: Implement minimal store behavior**

Update `submitImportDraft()` to attach the draft id to the created submission. Update `reviewSubmission()` so the first `approved` review publishes one feed post for the imported submission.

- [ ] **Step 4: Run store tests**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: PASS.

## Task 2: Route Flow Coverage

- [ ] **Step 1: Add failing route test**

Add a route-level test that calls `/v1/import-drafts`, `/v1/import-drafts/submit`, `/v1/admin/reviews`, and then `/v1/feed`.

- [ ] **Step 2: Run route tests**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: PASS after Task 1 because routes already use the store.

## Task 3: Full Verification

- [ ] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [ ] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

Expected: config renders `community-api`, `pet-generator`, and `admin-review`.

- [ ] **Step 4: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5c-approved-import-feed-publishing.md services/community-api/src
git commit -m "Publish approved imports to community feed"
```

Expected: commit created.

## Self-Review

- Spec coverage: Connects reviewed generator imports to the live feed consumed by Android.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing import draft, submission, review, and feed post shapes.
