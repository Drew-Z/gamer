# Phase 5z Admin Review Import Draft Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show import draft package evidence in the admin review queue.

**Architecture:** Keep raw import draft evidence in `community-api` review queue items, then let `admin-review` presenter derive concise display fields. This keeps API evidence available for future UI while preserving the existing admin model shape.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory store and admin presenter tests.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED test that review queue items include the source import draft.
- Modify `services/community-api/src/store.js`: include `importDraft` in `listAdminReviewQueue()`.
- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: add RED assertions for import evidence display fields.
- Modify `apps/admin-review/src/reviewQueuePresenter.js`: map import draft evidence into row fields.

## Task 1: API Review Queue Evidence

- [x] **Step 1: Write failing store test**

Add a test that creates a pet package bundle draft, submits it, reads the review queue, and asserts:

```js
assert.equal(item.importDraft.id, draft.id);
assert.equal(item.importDraft.importSummary.source.kind, "fantasy-pet-rule");
assert.equal(item.importDraft.importSummary.assets.previewPath, "previews/overall-showcase.png");
```

- [x] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because queue items do not include `importDraft`.

- [x] **Step 3: Implement queue evidence**

In `listAdminReviewQueue()`, find the matching import draft with `submission.importDraftId` and include:

```js
importDraft: clone(importDraft ?? null)
```

- [x] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Admin Presenter Evidence

- [x] **Step 1: Write failing presenter assertions**

Update `queueFixture.items[0]` with an `importDraft.importSummary` object and assert row fields:

```js
assert.equal(model.rows[0].importSourceKind, "fantasy-pet-rule");
assert.equal(model.rows[0].importPreviewPath, "previews/overall-showcase.png");
assert.equal(model.rows[0].motionSheetCount, 2);
assert.equal(model.rows[0].importEvidenceLabel, "fantasy-pet-rule / 2 motion sheets");
```

- [x] **Step 2: Run presenter test to verify RED**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because presenter rows do not expose import evidence fields.

- [x] **Step 3: Implement presenter fields**

In `createReviewDashboardModel()`, read `item.importDraft.importSummary` and add row fields:

```js
importSourceKind,
importPreviewPath,
motionSheetCount,
importEvidenceLabel
```

- [x] **Step 4: Run presenter test to verify GREEN**

Run the presenter test command again.

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
git add docs/superpowers/plans/2026-06-06-phase-5z-admin-review-import-draft-evidence.md services/community-api/src/store.js services/community-api/src/store.test.js apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js
git commit -m "Show import draft evidence in admin review"
```

## Self-Review

- Spec coverage: Gives admin review visibility into package/import evidence for bundle submissions.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `importDraft`, `importSummary`, and admin row model naming.
