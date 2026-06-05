# Phase 5f Admin Import Draft Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show import drafts in the admin-review UI so blocked and in-progress generator imports remain visible.

**Architecture:** Keep import drafts separate from the submission review queue. The browser loads `/v1/import-drafts`, maps drafts through a presenter helper, and renders a compact draft list below the import form.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, vanilla browser JavaScript, existing static admin-review app.

---

## Files

- Modify `apps/admin-review/src/reviewQueuePresenter.js`: add `createImportDraftListModel`.
- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: cover draft list status counts and row labels.
- Modify `apps/admin-review/public/index.html`: add import draft list container.
- Modify `apps/admin-review/public/app.js`: load and render `/v1/import-drafts`.
- Modify `apps/admin-review/public/styles.css`: style draft list rows.

## Task 1: Presenter Model

- [ ] **Step 1: Add failing presenter test**

Add a test for `createImportDraftListModel({ drafts })` with ready, blocked, and in-progress drafts.

- [ ] **Step 2: Run presenter tests**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because `createImportDraftListModel` does not exist.

- [ ] **Step 3: Implement model**

Return summary counts and rows with `id`, `petId`, `status`, `reason`, and `scoreReportId`.

- [ ] **Step 4: Run presenter tests**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: PASS.

## Task 2: Browser UI

- [ ] **Step 1: Add draft list markup**

Add a compact container below the import form.

- [ ] **Step 2: Load drafts**

Fetch `/v1/import-drafts` alongside the queue and render rows.

- [ ] **Step 3: Refresh after import**

After creating/submitting a draft, reload both draft list and review queue.

## Task 3: Verification

- [ ] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [ ] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

Expected: config renders all services.

- [ ] **Step 4: Browser smoke check**

Open `http://localhost:4200` and confirm the draft list renders without console errors.

- [ ] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5f-admin-import-draft-visibility.md apps/admin-review
git commit -m "Show import drafts in admin review"
```

Expected: commit created.

## Self-Review

- Spec coverage: Makes non-submitted generator imports visible to admins.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `drafts`, `readiness`, and `scoreReportId` shapes.
