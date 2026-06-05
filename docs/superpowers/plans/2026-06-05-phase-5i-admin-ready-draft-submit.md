# Phase 5i Admin Ready Draft Submit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let admins submit existing ready import drafts from the admin-review draft list.

**Architecture:** Keep draft action eligibility in the admin-review presenter. The browser UI renders a submit button only for ready drafts, calls the existing `/v1/import-drafts/submit` API, and reloads both import drafts and the review queue after success.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, vanilla browser JavaScript, static admin-review frontend.

---

## Files

- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: assert ready drafts expose a submit action.
- Modify `apps/admin-review/src/reviewQueuePresenter.js`: add `canSubmit` and `actions` fields to draft rows.
- Modify `apps/admin-review/public/app.js`: render submit buttons for ready drafts and call the submit endpoint.
- Modify `apps/admin-review/public/styles.css`: style draft action rows.

## Task 1: Presenter Draft Actions

- [ ] **Step 1: Add failing presenter test**

Extend the `createImportDraftListModel` test to require:

```js
assert.equal(model.rows[0].canSubmit, true);
assert.deepEqual(model.rows[0].actions, ["submit"]);
assert.equal(model.rows[1].canSubmit, false);
assert.deepEqual(model.rows[1].actions, []);
```

- [ ] **Step 2: Run presenter tests**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because `canSubmit` and `actions` are missing.

- [ ] **Step 3: Implement presenter fields**

For each draft row:

```js
const canSubmit = status === "ready";
actions: canSubmit ? ["submit"] : []
```

- [ ] **Step 4: Run presenter tests**

Run the presenter test command again.

Expected: PASS.

## Task 2: Browser UI

- [ ] **Step 1: Render draft actions**

In `renderDraftList()`, append an action row to each draft item. For `submit`, create a button that calls `submitImportDraft(row.id)`.

- [ ] **Step 2: Submit ready draft**

Add:

```js
async function submitImportDraft(draftId) {
  elements.importStatus.textContent = `Submitting ${draftId}...`;
  await requestJson("/v1/import-drafts/submit", {
    method: "POST",
    body: JSON.stringify({ draftId })
  });
  elements.importStatus.textContent = `Submitted ${draftId}.`;
  await loadDashboard();
}
```

- [ ] **Step 3: Style draft actions**

Add `.draft-actions` styling and reuse the existing button colors.

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

Create a ready draft via `POST /v1/import-drafts`, open `http://127.0.0.1:4200`, click the draft list `Submit` button, and confirm the queue count increases without console errors.

- [ ] **Step 5: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only this phase's files are modified.

- [ ] **Step 6: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5i-admin-ready-draft-submit.md apps/admin-review
git commit -m "Submit ready import drafts from admin review"
```

Expected: commit created.

## Self-Review

- Spec coverage: Lets admins move ready imports into the review queue even when the draft was created before or outside the import form auto-submit path.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `canSubmit`, `actions`, `submitImportDraft`, and `/v1/import-drafts/submit` are used consistently.
