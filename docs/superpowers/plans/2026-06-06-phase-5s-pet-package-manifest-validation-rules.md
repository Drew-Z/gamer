# Phase 5s Pet Package Manifest Validation Rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen pet package manifest validation for source and asset fields.

**Architecture:** Keep manifest validation in `packages/pet-package-spec`. The current MVP only accepts `fantasy-pet-rule` as a package source, and every motion sheet path must be a non-empty string so downstream runtime/import tools do not receive unusable asset references.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing package validators.

---

## Files

- Modify `packages/pet-package-spec/src/validators.test.js`: add invalid manifest source and asset cases.
- Modify `packages/pet-package-spec/src/validators.js`: add source kind and motion sheet entry rules.

## Task 1: Manifest Validator Rules

- [x] **Step 1: Write failing validator test**

Add:

```js
test("manifest rejects unsupported source kind and invalid motion sheets", () => {
  const manifest = {
    ...validPetPackageManifest,
    source: {
      ...validPetPackageManifest.source,
      kind: "manual-upload"
    },
    assets: {
      ...validPetPackageManifest.assets,
      motionSheets: ["motion/sheets/idle.png", "", 42]
    }
  };

  const validation = validatePetPackageManifest(manifest);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("source.kind must be one of fantasy-pet-rule"));
  assert.ok(
    validation.errors.includes("assets.motionSheets[1] must be a non-empty string")
  );
  assert.ok(
    validation.errors.includes("assets.motionSheets[2] must be a non-empty string")
  );
});
```

- [x] **Step 2: Run validator test to verify RED**

Run:

```powershell
node --test packages/pet-package-spec/src/validators.test.js
```

Expected: FAIL because current manifest validation accepts any non-empty source kind and does not validate motion sheet entries.

- [x] **Step 3: Implement manifest rules**

Add:

```js
const ALLOWED_MANIFEST_SOURCE_KINDS = ["fantasy-pet-rule"];
```

In `validatePetPackageManifest()`:

```js
if (!ALLOWED_MANIFEST_SOURCE_KINDS.includes(manifest.source.kind)) {
  errors.push("source.kind must be one of fantasy-pet-rule");
}
```

For motion sheets:

```js
manifest.assets.motionSheets.forEach((sheet, index) => {
  requireString(errors, sheet, `assets.motionSheets[${index}]`);
});
```

- [x] **Step 4: Run validator test to verify GREEN**

Run the validator test command again.

Expected: PASS.

## Task 2: Verification

- [x] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [x] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

Expected: config renders all services.

- [x] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only this phase's files are modified.

- [ ] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-06-phase-5s-pet-package-manifest-validation-rules.md packages/pet-package-spec/src/validators.js packages/pet-package-spec/src/validators.test.js
git commit -m "Strengthen pet package manifest validation"
```

Expected: commit created.

## Self-Review

- Spec coverage: Strengthens package source and motion asset references for generated pet imports.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: The manifest source kind matches the current `fantasy-pet-rule` integration path.
