# Phase 5t Pet Package Bundle Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared validator for a complete pet package submission bundle.

**Architecture:** Keep individual schema validation in `packages/pet-package-spec/src/validators.js`, then add a bundle-level validator that composes manifest, ownership claim, and score report checks. The bundle validator also verifies cross-document identity consistency so later API/admin import flows do not accept mismatched package evidence.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing `@gamer/pet-package-spec` fixtures and validators.

---

## Files

- Modify `packages/pet-package-spec/src/validators.test.js`: add RED tests for valid bundle and mismatched bundle identities.
- Modify `packages/pet-package-spec/src/validators.js`: add `validatePetPackageBundle()`.
- Modify `packages/pet-package-spec/src/fixtures.js`: add `validPetPackageBundle`.
- Modify `packages/pet-package-spec/src/index.js`: export the new fixture and validator.

## Task 1: Bundle Validator

- [x] **Step 1: Write failing bundle validator tests**

Add tests:

```js
test("valid pet package bundle passes", () => {
  const validation = validatePetPackageBundle(validPetPackageBundle);

  assert.deepEqual(validation, {
    ok: true,
    errors: []
  });
});

test("pet package bundle rejects mismatched pet and owner identities", () => {
  const bundle = {
    ...validPetPackageBundle,
    ownershipClaim: {
      ...validPetPackageBundle.ownershipClaim,
      userId: "user-other-001",
      petId: "pet-other-001"
    },
    scoreReport: {
      ...validPetPackageBundle.scoreReport,
      petId: "pet-score-other-001"
    }
  };

  const validation = validatePetPackageBundle(bundle);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes("manifest.petId must match ownershipClaim.petId")
  );
  assert.ok(
    validation.errors.includes("manifest.ownerUserId must match ownershipClaim.userId")
  );
  assert.ok(validation.errors.includes("manifest.petId must match scoreReport.petId"));
});
```

- [x] **Step 2: Run validator test to verify RED**

Run:

```powershell
node --test packages/pet-package-spec/src/validators.test.js
```

Expected: FAIL because `validatePetPackageBundle` and `validPetPackageBundle` do not exist yet.

- [x] **Step 3: Implement bundle fixture and exports**

Add to `packages/pet-package-spec/src/fixtures.js`:

```js
export const validPetPackageBundle = {
  manifest: validPetPackageManifest,
  ownershipClaim: validOwnershipClaim,
  scoreReport: validScoreReport
};
```

Export `validPetPackageBundle` and `validatePetPackageBundle` from `packages/pet-package-spec/src/index.js`.

- [x] **Step 4: Implement bundle validator**

Add to `packages/pet-package-spec/src/validators.js`:

```js
const prefixErrors = (prefix, validation) =>
  validation.errors.map((error) => `${prefix}.${error}`);

export function validatePetPackageBundle(bundle) {
  const errors = [];

  if (!isObject(bundle)) {
    return result(["bundle must be an object"]);
  }

  errors.push(...prefixErrors("manifest", validatePetPackageManifest(bundle.manifest)));
  errors.push(
    ...prefixErrors("ownershipClaim", validateOwnershipClaim(bundle.ownershipClaim))
  );
  errors.push(...prefixErrors("scoreReport", validateScoreReport(bundle.scoreReport)));

  if (
    isObject(bundle.manifest) &&
    isObject(bundle.ownershipClaim) &&
    bundle.manifest.petId !== bundle.ownershipClaim.petId
  ) {
    errors.push("manifest.petId must match ownershipClaim.petId");
  }

  if (
    isObject(bundle.manifest) &&
    isObject(bundle.ownershipClaim) &&
    bundle.manifest.ownerUserId !== bundle.ownershipClaim.userId
  ) {
    errors.push("manifest.ownerUserId must match ownershipClaim.userId");
  }

  if (
    isObject(bundle.manifest) &&
    isObject(bundle.scoreReport) &&
    bundle.manifest.petId !== bundle.scoreReport.petId
  ) {
    errors.push("manifest.petId must match scoreReport.petId");
  }

  return result(errors);
}
```

- [x] **Step 5: Run validator test to verify GREEN**

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
git add docs/superpowers/plans/2026-06-06-phase-5t-pet-package-bundle-validation.md packages/pet-package-spec/src/fixtures.js packages/pet-package-spec/src/index.js packages/pet-package-spec/src/validators.js packages/pet-package-spec/src/validators.test.js
git commit -m "Add pet package bundle validation"
```

Expected: commit created.

## Self-Review

- Spec coverage: Adds the shared validation boundary needed before API/admin flows accept complete package evidence.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses the existing manifest, ownership claim, and score report fixture names and validator names.
