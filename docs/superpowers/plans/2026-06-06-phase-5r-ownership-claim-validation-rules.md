# Phase 5r Ownership Claim Validation Rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen ownership claim validation for pet package submissions.

**Architecture:** Keep ownership claim contract rules inside `packages/pet-package-spec`, alongside manifest, score report, and ledger validators. Add narrow enum and reference validation without changing the claim schema.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing package validators.

---

## Files

- Modify `packages/pet-package-spec/src/validators.test.js`: add invalid ownership claim cases.
- Modify `packages/pet-package-spec/src/validators.js`: add claim type, review status, and source reference rules.

## Task 1: Ownership Claim Validator Rules

- [ ] **Step 1: Write failing validator test**

Add:

```js
test("ownership claim rejects invalid claim type status and source references", () => {
  const claim = {
    ...validOwnershipClaim,
    claimType: "unknown",
    reviewStatus: "maybe",
    sourceReferences: ["", 42]
  };

  const validation = validateOwnershipClaim(claim);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes(
      "claimType must be one of original-created, licensed, derivative-permitted"
    )
  );
  assert.ok(
    validation.errors.includes(
      "reviewStatus must be one of pending, approved, rejected, revoked"
    )
  );
  assert.ok(
    validation.errors.includes("sourceReferences[0] must be a non-empty string")
  );
  assert.ok(
    validation.errors.includes("sourceReferences[1] must be a non-empty string")
  );
});
```

- [ ] **Step 2: Run validator test to verify RED**

Run:

```powershell
node --test packages/pet-package-spec/src/validators.test.js
```

Expected: FAIL because current ownership claim validation only checks string presence and array shape.

- [ ] **Step 3: Implement ownership claim rules**

Add constants:

```js
const ALLOWED_CLAIM_TYPES = [
  "original-created",
  "licensed",
  "derivative-permitted"
];
const ALLOWED_CLAIM_REVIEW_STATUSES = ["pending", "approved", "rejected", "revoked"];
```

In `validateOwnershipClaim()`:

```js
if (!ALLOWED_CLAIM_TYPES.includes(claim.claimType)) {
  errors.push("claimType must be one of original-created, licensed, derivative-permitted");
}
if (!ALLOWED_CLAIM_REVIEW_STATUSES.includes(claim.reviewStatus)) {
  errors.push("reviewStatus must be one of pending, approved, rejected, revoked");
}
if (Array.isArray(claim.sourceReferences)) {
  claim.sourceReferences.forEach((reference, index) => {
    requireString(errors, reference, `sourceReferences[${index}]`);
  });
}
```

- [ ] **Step 4: Run validator test to verify GREEN**

Run the validator test command again.

Expected: PASS.

## Task 2: Verification

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

- [ ] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only this phase's files are modified.

- [ ] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-06-phase-5r-ownership-claim-validation-rules.md packages/pet-package-spec/src/validators.js packages/pet-package-spec/src/validators.test.js
git commit -m "Strengthen ownership claim validation"
```

Expected: commit created.

## Self-Review

- Spec coverage: Strengthens ownership declaration data before it can support submission rewards and moderation.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Claim type and review status enums match current product language and existing fixture values.
