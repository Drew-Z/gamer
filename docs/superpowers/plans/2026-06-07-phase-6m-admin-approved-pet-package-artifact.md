# Phase 6m Admin Approved Pet Package Artifact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show approved pet export package artifact paths in the admin approved pet registry panel.

**Architecture:** Keep the existing `/v1/pets/approved` API contract added in Phase 6l. Extend the admin presenter row with `exportArtifactPath` and a derived `packageArtifactLabel`, then render that label as a compact code row under the preview path in the approved pet registry list.

**Tech Stack:** Node.js test runner, browser DOM JavaScript, static CSS.

---

### Task 1: Admin Presenter Package Artifact Fields

**Files:**
- Modify: `apps/admin-review/src/reviewQueuePresenter.test.js`
- Modify: `apps/admin-review/src/reviewQueuePresenter.js`

- [ ] **Step 1: Write the failing test**

In `approvedPetsFixture`, add:

```js
exportArtifactPath: "exports/stardust.zip"
```

under the first approved pet's `assets`.

Extend `createApprovedPetRegistryModel summarizes approved pet assets` with:

```js
assert.equal(model.rows[0].exportArtifactPath, "exports/stardust.zip");
assert.equal(model.rows[0].packageArtifactLabel, "Package exports/stardust.zip");
```

Also assert the missing-submission fallback row:

```js
assert.equal(missingSubmission.rows[0].exportArtifactPath, "");
assert.equal(missingSubmission.rows[0].packageArtifactLabel, "Package artifact pending");
```

- [ ] **Step 2: Run focused test to verify RED**

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because the new presenter fields are missing.

- [ ] **Step 3: Implement presenter fields**

In `createApprovedPetRegistryModel(...)`, add:

```js
const exportArtifactPath = item.assets?.exportArtifactPath ?? "";
```

Then include:

```js
exportArtifactPath,
packageArtifactLabel: exportArtifactPath
  ? `Package ${exportArtifactPath}`
  : "Package artifact pending",
```

- [ ] **Step 4: Run focused test to verify GREEN**

```powershell
npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: PASS.

### Task 2: Admin Registry DOM Rendering

**Files:**
- Modify: `apps/admin-review/public/app.js`
- Modify: `apps/admin-review/public/styles.css`

- [ ] **Step 1: Render package artifact label**

In `renderApprovedPetList()`, after `previewPath`, create:

```js
const packageArtifact = document.createElement("code");
packageArtifact.className = "approved-pet-package-path";
packageArtifact.textContent = row.packageArtifactLabel;
```

Then append it after `previewPath`.

- [ ] **Step 2: Style package path row**

Reuse the approved pet code styling by changing:

```css
.approved-pet-item code {
```

to keep applying to the new row, and add:

```css
.approved-pet-package-path {
  color: var(--accent-strong);
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

Start temporary community/admin services and verify the admin page returns 200, `/app.js` contains `approved-pet-package-path`, and `/api/v1/pets/approved` remains accessible.

- [ ] **Step 5: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 6: Commit**

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6m-admin-approved-pet-package-artifact.md apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js apps/admin-review/public/app.js apps/admin-review/public/styles.css
git commit -m "Show approved pet package artifacts in admin"
```

---

## Self-Review

- Spec coverage: Makes the Phase 6l export artifact visible in the admin review console, improving reviewer traceability for published resources.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: Uses `exportArtifactPath` from approved registry assets and `packageArtifactLabel` in the admin presenter/UI.
