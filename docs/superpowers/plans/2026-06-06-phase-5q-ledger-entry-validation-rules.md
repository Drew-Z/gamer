# Phase 5q Ledger Entry Validation Rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen currency ledger validation for reward, reversal, and daily check-in entries.

**Architecture:** Keep the rules in `packages/pet-package-spec` so community API, admin tooling, and future persistence share one contract. Accept current MVP ledger source types and statuses while rejecting invalid amounts and mismatched reversal signs.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing package validators.

---

## Files

- Modify `packages/pet-package-spec/src/validators.test.js`: add invalid ledger entry cases.
- Modify `packages/pet-package-spec/src/validators.js`: add amount, source type, and status rules.

## Task 1: Ledger Validator Rules

- [ ] **Step 1: Write failing validator tests**

Add:

```js
test("ledger entry rejects invalid amount and status", () => {
  const entry = {
    ...validCurrencyLedgerEntry,
    amount: 1.5,
    status: "unknown"
  };

  const validation = validateCurrencyLedgerEntry(entry);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("amount must be an integer"));
  assert.ok(validation.errors.includes("status must be one of posted, pending, voided"));
});

test("ledger entry requires reversal amounts to be negative", () => {
  const entry = {
    ...validCurrencyLedgerEntry,
    sourceType: "submission-reward-reversal",
    amount: 80
  };

  const validation = validateCurrencyLedgerEntry(entry);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes(
      "amount must be negative for submission-reward-reversal"
    )
  );
});
```

- [ ] **Step 2: Run validator test to verify RED**

Run:

```powershell
node --test packages/pet-package-spec/src/validators.test.js
```

Expected: FAIL because current ledger validation allows those values.

- [ ] **Step 3: Implement ledger validation rules**

Add constants:

```js
const ALLOWED_LEDGER_SOURCE_TYPES = [
  "daily-checkin",
  "submission-reward",
  "submission-reward-reversal"
];
const ALLOWED_LEDGER_STATUSES = ["posted", "pending", "voided"];
```

In `validateCurrencyLedgerEntry()`:

```js
if (typeof entry.amount !== "number" || !Number.isInteger(entry.amount)) {
  errors.push("amount must be an integer");
}
if (!ALLOWED_LEDGER_SOURCE_TYPES.includes(entry.sourceType)) {
  errors.push("sourceType must be one of daily-checkin, submission-reward, submission-reward-reversal");
}
if (!ALLOWED_LEDGER_STATUSES.includes(entry.status)) {
  errors.push("status must be one of posted, pending, voided");
}
if (entry.sourceType === "submission-reward-reversal" && entry.amount >= 0) {
  errors.push("amount must be negative for submission-reward-reversal");
}
if (entry.sourceType !== "submission-reward-reversal" && entry.amount < 0) {
  errors.push("amount must be non-negative unless sourceType is submission-reward-reversal");
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
git add docs/superpowers/plans/2026-06-06-phase-5q-ledger-entry-validation-rules.md packages/pet-package-spec/src/validators.js packages/pet-package-spec/src/validators.test.js
git commit -m "Strengthen ledger entry validation"
```

Expected: commit created.

## Self-Review

- Spec coverage: Strengthens the shared currency ledger contract for daily rewards, submission rewards, and reward reversals.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Ledger source type, status, and amount sign rules match existing community API entries.
