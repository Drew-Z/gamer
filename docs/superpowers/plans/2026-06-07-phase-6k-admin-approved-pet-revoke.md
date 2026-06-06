# Phase 6k Admin Approved Pet Revoke Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let admins revoke a published imported pet directly from the approved pet registry panel.

**Architecture:** Reuse the existing `POST /v1/admin/reviews` endpoint with `status: "revoked"` instead of adding a new backend route. Extend approved pet registry presenter rows with a revoke action descriptor when `submissionId` exists, render a danger button in the admin sidebar, and refresh the full dashboard after revoke so review queue, approved pet registry, feed publication state, and ledger state stay in sync.

**Tech Stack:** Node.js test runner, browser DOM JavaScript, static CSS, existing community API review flow.

---

### Task 1: Registry Revoke Action Presenter

**Files:**
- Modify: `apps/admin-review/src/reviewQueuePresenter.js`
- Test: `apps/admin-review/src/reviewQueuePresenter.test.js`

- [ ] **Step 1: Write the failing test**

Extend `createApprovedPetRegistryModel summarizes approved pet assets` so the first approved pet row expects:

```js
assert.equal(model.rows[0].canRevokeSubmission, true);
assert.equal(model.rows[0].revokeSubmissionLabel, "Revoke publication");
```

Also extend the missing-submission row assertions:

```js
assert.equal(missingSubmission.rows[0].canRevokeSubmission, false);
assert.equal(missingSubmission.rows[0].revokeSubmissionLabel, "");
```

- [ ] **Step 2: Run test to verify RED**

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because `canRevokeSubmission` and `revokeSubmissionLabel` are missing.

- [ ] **Step 3: Implement presenter fields**

In `createApprovedPetRegistryModel`, add:

```js
canRevokeSubmission: Boolean(submissionId),
revokeSubmissionLabel: submissionId ? "Revoke publication" : "",
```

- [ ] **Step 4: Run focused test to verify GREEN**

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: PASS.

### Task 2: Admin DOM Revoke Behavior

**Files:**
- Modify: `apps/admin-review/public/app.js`
- Modify: `apps/admin-review/public/styles.css`

- [ ] **Step 1: Render revoke button**

In `renderApprovedPetList()`, when `row.canRevokeSubmission` is true, render a button:

```js
button.className = "approved-pet-revoke-button";
button.type = "button";
button.textContent = row.revokeSubmissionLabel;
button.addEventListener("click", () => revokeApprovedPet(row.submissionId));
```

- [ ] **Step 2: Implement revoke function**

Add:

```js
async function revokeApprovedPet(submissionId) {
  elements.statusLine.textContent = `Revoking ${submissionId}...`;
  await requestJson("/v1/admin/reviews", {
    method: "POST",
    body: JSON.stringify({
      submissionId,
      status: "revoked",
      reviewer: "admin-ui"
    })
  });
  await loadDashboard();
}
```

- [ ] **Step 3: Refresh full dashboard after queue review actions**

Change `reviewSubmission(...)` to call `await loadDashboard()` instead of `await loadQueue()` so approve and revoke actions update the approved pet panel immediately.

- [ ] **Step 4: Add danger button style**

Add `.approved-pet-revoke-button` beside `.approved-pet-focus-button` using the existing danger palette:

```css
.approved-pet-revoke-button {
  min-height: 32px;
  border: 1px solid var(--danger);
  border-radius: 8px;
  background: var(--surface);
  color: var(--danger);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  padding: 0 10px;
}
```

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

Start temporary community/admin services and verify the admin page returns 200, `/app.js` includes `revokeApprovedPet`, and `/api/v1/pets/approved` remains accessible.

- [ ] **Step 5: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 6: Commit**

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6k-admin-approved-pet-revoke.md apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js apps/admin-review/public/app.js apps/admin-review/public/styles.css
git commit -m "Revoke approved pets from admin registry"
```

---

## Self-Review

- Spec coverage: Adds direct admin registry revoke, reuses the existing audited review route, and refreshes dashboard state after publish-state changes.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: Presenter fields use `canRevokeSubmission` and `revokeSubmissionLabel`; browser code consumes the same names.
