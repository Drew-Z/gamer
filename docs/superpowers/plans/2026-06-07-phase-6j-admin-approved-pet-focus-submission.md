# Phase 6j Admin Approved Pet Focus Submission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let admins jump from an approved pet registry row to the corresponding submission card in the review queue.

**Architecture:** Extend approved pet registry presenter rows with a small action descriptor when `submissionId` exists. Render a `View submission` button in the admin sidebar and implement browser-side focus behavior that switches to the All queue filter, rerenders the queue, scrolls the target card into view, and briefly highlights it.

**Tech Stack:** Node.js test runner, browser DOM JavaScript, static CSS.

---

### Task 1: Registry Focus Action Presenter

**Files:**
- Modify: `apps/admin-review/src/reviewQueuePresenter.js`
- Test: `apps/admin-review/src/reviewQueuePresenter.test.js`

- [ ] **Step 1: Write the failing test**

Extend the approved pet registry model test so the first row expects:
- `canFocusSubmission === true`
- `focusSubmissionLabel === "View submission"`

Also assert a missing-submission registry row has:
- `canFocusSubmission === false`
- `focusSubmissionLabel === ""`

- [ ] **Step 2: Run test to verify RED**

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because the new row fields are missing.

- [ ] **Step 3: Implement presenter fields**

Set:
- `canFocusSubmission` to `Boolean(submissionId)`
- `focusSubmissionLabel` to `"View submission"` when focus is possible, otherwise `""`

- [ ] **Step 4: Run focused test to verify GREEN**

Run the same focused command. Expected: PASS.

### Task 2: Admin DOM Focus Behavior

**Files:**
- Modify: `apps/admin-review/public/app.js`
- Modify: `apps/admin-review/public/styles.css`

- [ ] **Step 1: Render focus button**

In each approved pet item with `canFocusSubmission`, render a `.approved-pet-focus-button` button labeled by `focusSubmissionLabel`.

- [ ] **Step 2: Implement focus function**

Add `focusSubmission(submissionId)`:
- sets `state.filter = "all"`
- updates filter button active state
- calls `renderList()`
- finds `[data-submission-id="<submissionId>"]`
- if found, adds `is-focused`, scrolls into view, and updates status line to `Focused ${submissionId}.`
- if missing, updates status line to `Submission ${submissionId} is not in the loaded queue.`

- [ ] **Step 3: Add highlight style**

Style `.queue-item.is-focused` with a clear border/outline that fits the existing admin palette.

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

Start temporary community/admin services and verify the admin page returns 200 and `/app.js` includes `focusSubmission`.

- [ ] **Step 5: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 6: Commit**

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6j-admin-approved-pet-focus-submission.md apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js apps/admin-review/public/app.js apps/admin-review/public/styles.css
git commit -m "Focus submissions from approved pet registry"
```
