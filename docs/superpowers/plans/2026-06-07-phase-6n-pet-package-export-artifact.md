# Phase 6n Pet Package Export Artifact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let uploaded pet package bundles declare the exported package artifact that later flows into import drafts, approved pet registry, Android, and admin review.

**Architecture:** Add `assets.exportArtifact` to the pet package manifest contract and validator. Keep existing community API import summary naming as `assets.exportArtifactPath`, mapping from `manifest.assets.exportArtifact` during bundle import so the downstream Phase 6l/6m asset path remains stable.

**Tech Stack:** Node.js test runner, shared package spec validators, in-memory community API store.

---

### Task 1: Pet Package Manifest Contract

**Files:**
- Modify: `packages/pet-package-spec/src/fixtures.js`
- Modify: `packages/pet-package-spec/src/validators.test.js`
- Modify: `packages/pet-package-spec/src/validators.js`

- [ ] **Step 1: Write failing validator test**

Add `exportArtifact: "exports/stardust-package.zip"` to `validPetPackageManifest.assets`.

In `manifest rejects unsupported source kind and invalid motion sheets`, set:

```js
exportArtifact: ""
```

and assert:

```js
assert.ok(validation.errors.includes("assets.exportArtifact must be a non-empty string"));
```

- [ ] **Step 2: Run focused validator tests to verify RED**

```powershell
npm.cmd test -- packages/pet-package-spec/src/validators.test.js
```

Expected: FAIL because `validatePetPackageManifest()` does not yet require `assets.exportArtifact`.

- [ ] **Step 3: Implement validator rule**

In `validatePetPackageManifest()`, require:

```js
requireString(errors, manifest.assets.exportArtifact, "assets.exportArtifact");
```

- [ ] **Step 4: Run focused validator tests to verify GREEN**

```powershell
npm.cmd test -- packages/pet-package-spec/src/validators.test.js
```

Expected: PASS.

### Task 2: Bundle Import Summary Mapping

**Files:**
- Modify: `services/community-api/src/store.test.js`
- Modify: `services/community-api/src/store.js`

- [ ] **Step 1: Write failing store assertion**

In `pet package bundle creates ready import draft with uploaded score report`, assert:

```js
assert.equal(
  draft.importSummary.assets.exportArtifactPath,
  "exports/stardust-package.zip"
);
```

- [ ] **Step 2: Run focused store test to verify RED**

```powershell
npm.cmd test -- services/community-api/src/store.test.js
```

Expected: FAIL because bundle import summary does not yet map `exportArtifact`.

- [ ] **Step 3: Implement mapping**

In `createImportSummaryFromPetPackageBundle()`, add:

```js
exportArtifactPath: bundle.manifest.assets.exportArtifact,
```

inside `assets`.

- [ ] **Step 4: Run focused store test to verify GREEN**

```powershell
npm.cmd test -- services/community-api/src/store.test.js
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
git add docs/superpowers/plans/2026-06-07-phase-6n-pet-package-export-artifact.md packages/pet-package-spec/src/fixtures.js packages/pet-package-spec/src/validators.test.js packages/pet-package-spec/src/validators.js services/community-api/src/store.test.js services/community-api/src/store.js
git commit -m "Require export artifacts in pet packages"
```

---

## Self-Review

- Spec coverage: Gives uploaded pet packages an explicit exported artifact contract and maps it into the already-consumed import summary field.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: Manifest uses `assets.exportArtifact`; import summary continues using `assets.exportArtifactPath`.
