# Phase 5e Admin Fantasy Pet Import Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin-review panel that creates community import drafts from a local `fantasy-pet-rule` `state.json` path.

**Architecture:** Keep the browser UI small and use a presenter helper for form payload/status behavior. The UI posts to the existing community API bridge route through the admin proxy, then refreshes the review queue.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, vanilla browser JavaScript, existing static admin-review app.

---

## Files

- Modify `apps/admin-review/src/reviewQueuePresenter.js`: add import form helpers.
- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: cover form payload and status label helpers.
- Modify `apps/admin-review/public/index.html`: add import panel form.
- Modify `apps/admin-review/public/app.js`: wire form submission to `/v1/import-drafts/from-fantasy-pet-rule`.
- Modify `apps/admin-review/public/styles.css`: style the import panel with existing dashboard patterns.

## Task 1: Presenter Helpers

- [ ] **Step 1: Add failing presenter tests**

Add tests for `createFantasyPetImportPayload({ statePath, ownershipClaimId })` and `formatImportDraftStatus(draft)`.

- [ ] **Step 2: Run presenter tests**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because helpers do not exist.

- [ ] **Step 3: Implement helpers**

Add the helpers to `reviewQueuePresenter.js`.

- [ ] **Step 4: Run presenter tests**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: PASS.

## Task 2: Browser UI

- [ ] **Step 1: Add form markup**

Add a sidebar form with `statePath`, `ownershipClaimId`, submit button, and status line.

- [ ] **Step 2: Wire form submit**

Post the helper payload to `/api/v1/import-drafts/from-fantasy-pet-rule`, show success or error, and reload the queue.

- [ ] **Step 3: Style panel**

Use compact full-width controls consistent with the existing admin console.

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

Run `community-api` and `admin-review`, open `http://localhost:4200`, and confirm the import panel renders without layout overlap.

- [ ] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5e-admin-fantasy-pet-import-panel.md apps/admin-review
git commit -m "Add admin fantasy pet import panel"
```

Expected: commit created.

## Self-Review

- Spec coverage: Gives admins a local UI entry point for the generator-to-community import bridge.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `statePath`, `ownershipClaimId`, and import draft response fields.
