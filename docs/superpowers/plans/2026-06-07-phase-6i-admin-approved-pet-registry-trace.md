# Phase 6i Admin Approved Pet Registry Trace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add traceability fields to the admin approved pet registry panel so reviewers can connect a published pet back to approval time, score report, and import draft.

**Architecture:** Keep the existing `/v1/pets/approved` contract and admin API proxy unchanged. Extend the admin presenter rows with derived labels for `approvedAt`, `scoreReportId`, and `importDraftId`, then render those labels in each approved pet registry row.

**Tech Stack:** Node.js test runner, browser DOM JavaScript, static CSS.

---

### Task 1: Registry Trace Presenter

**Files:**
- Modify: `apps/admin-review/src/reviewQueuePresenter.js`
- Test: `apps/admin-review/src/reviewQueuePresenter.test.js`

- [ ] **Step 1: Write the failing test**

Extend `createApprovedPetRegistryModel summarizes approved pet assets` so the first row expects:
- `approvedAt === "2026-06-07T02:30:00.000Z"`
- `approvedAtLabel === "Approved 2026-06-07T02:30:00.000Z"`
- `scoreReportLabel === "Score score-import-draft-local-001"`
- `importDraftLabel === "Draft import-draft-local-001"`

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because the trace labels are missing.

- [ ] **Step 3: Implement presenter labels**

Add safe defaults:
- missing `approvedAt` -> `"Approved time unknown"`
- missing `scoreReportId` -> `"No score report"`
- missing `importDraftId` -> `"No import draft"`

- [ ] **Step 4: Run test to verify GREEN**

Run the same focused test command. Expected: PASS.

### Task 2: Registry Trace Rendering

**Files:**
- Modify: `apps/admin-review/public/app.js`
- Modify: `apps/admin-review/public/styles.css`

- [ ] **Step 1: Render trace labels**

Inside each `.approved-pet-item`, append a compact trace list containing:
- approved time label
- score report label
- import draft label
- existing submission label

- [ ] **Step 2: Run focused tests**

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

- [ ] **Step 4: Run admin smoke test**

Start temporary community/admin services and verify the admin page still returns 200 and includes the approved pet list.

- [ ] **Step 5: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 6: Commit**

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6i-admin-approved-pet-registry-trace.md apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js apps/admin-review/public/app.js apps/admin-review/public/styles.css
git commit -m "Show approved pet registry trace details"
```
