# Phase 6h Admin Approved Pet Registry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show approved imported pet assets in the admin review console so reviewers can see what has been published.

**Architecture:** Reuse the existing `/api` proxy in `apps/admin-review/server.js` and call `/api/v1/pets/approved` from the browser alongside drafts and review queue data. Add a presenter model that converts the approved pet registry into compact rows for the sidebar, then render a new panel without changing the community API contract.

**Tech Stack:** Node.js test runner, browser DOM JavaScript, static CSS, existing community API proxy.

---

### Task 1: Approved Pet Registry Presenter

**Files:**
- Modify: `apps/admin-review/src/reviewQueuePresenter.js`
- Test: `apps/admin-review/src/reviewQueuePresenter.test.js`

- [ ] **Step 1: Write the failing test**

Add a test for `createApprovedPetRegistryModel({ items })` that expects:
- `summary.total === 2`
- first row has `petId`, `displayName`, `ownerUserId`
- first row has `assetLabel === "fantasy-pet-rule / score 86 / 2 motion sheets"`
- first row has `previewPath === "previews/overall-showcase.png"`
- first row has `submissionLabel === "Submission submission-local-002"`
- empty input returns total 0 and no rows

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because `createApprovedPetRegistryModel` is not exported.

- [ ] **Step 3: Implement presenter**

Add:
- `export function createApprovedPetRegistryModel(response = { items: [] })`
- rows with safe defaults and compact display labels

- [ ] **Step 4: Run test to verify GREEN**

Run the same focused npm command. Expected: PASS.

### Task 2: Admin UI Rendering

**Files:**
- Modify: `apps/admin-review/public/app.js`
- Modify: `apps/admin-review/public/index.html`
- Modify: `apps/admin-review/public/styles.css`

- [ ] **Step 1: Wire state and API load**

Update browser state to include:
- `approvedPetModel: createApprovedPetRegistryModel({ items: [] })`

Update `loadDashboard()` to fetch:
- `/v1/import-drafts`
- `/v1/admin/review-queue`
- `/v1/pets/approved`

- [ ] **Step 2: Render the registry panel**

Add sidebar elements:
- `#approved-pet-summary`
- `#approved-pet-list`

Render:
- empty state: `"No approved pets yet."`
- each approved pet row: display name, asset label, preview path, submission label

- [ ] **Step 3: Run admin presenter tests**

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
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
git add docs/superpowers/plans/2026-06-07-phase-6h-admin-approved-pet-registry.md apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js apps/admin-review/public/app.js apps/admin-review/public/index.html apps/admin-review/public/styles.css
git commit -m "Show approved pet registry in admin review"
```
