# Phase 5p Score Report Validation Rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strengthen score report validation so invalid score and reward values cannot pass shared contracts.

**Architecture:** Keep validation inside `packages/pet-package-spec` because score reports are shared by `community-api`, generator adapters, admin tooling, and Android metadata mapping. Add narrow numeric rules without changing score report shape.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing package validators.

---

## Files

- Modify `packages/pet-package-spec/src/validators.test.js`: add invalid score report cases.
- Modify `packages/pet-package-spec/src/validators.js`: add finite non-negative number and non-negative integer reward rules.

## Task 1: Score Report Validator Rules

- [ ] **Step 1: Write failing validator tests**

Add:

```js
test("score report rejects negative score fields", () => {
  const report = {
    ...validScoreReport,
    totalScore: -1,
    breakdown: {
      ...validScoreReport.breakdown,
      visualQuality: -2
    }
  };

  const validation = validateScoreReport(report);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("totalScore must be a non-negative number"));
  assert.ok(validation.errors.includes("breakdown.visualQuality must be a non-negative number"));
});

test("score report rejects invalid reward recommendation amount", () => {
  const report = {
    ...validScoreReport,
    rewardRecommendation: {
      ...validScoreReport.rewardRecommendation,
      grant: false,
      amount: 10
    }
  };

  const validation = validateScoreReport(report);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("rewardRecommendation.amount must be 0 when grant is false"));
});
```

- [ ] **Step 2: Run validator test to verify RED**

Run:

```powershell
node --test packages/pet-package-spec/src/validators.test.js
```

Expected: FAIL because current validation allows those values.

- [ ] **Step 3: Implement numeric validation**

Add helper:

```js
const requireNonNegativeNumber = (errors, value, field) => {
  if (typeof value !== "number" || !Number.isFinite(value) || value < 0) {
    errors.push(`${field} must be a non-negative number`);
  }
};
```

Use it for `totalScore` and score `breakdown` fields.

Add reward checks:

```js
if (
  typeof report.rewardRecommendation.amount !== "number" ||
  !Number.isInteger(report.rewardRecommendation.amount) ||
  report.rewardRecommendation.amount < 0
) {
  errors.push("rewardRecommendation.amount must be a non-negative integer");
}
if (
  report.rewardRecommendation.grant === false &&
  report.rewardRecommendation.amount !== 0
) {
  errors.push("rewardRecommendation.amount must be 0 when grant is false");
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
git add docs/superpowers/plans/2026-06-06-phase-5p-score-report-validation-rules.md packages/pet-package-spec/src/validators.js packages/pet-package-spec/src/validators.test.js
git commit -m "Strengthen score report validation"
```

Expected: commit created.

## Self-Review

- Spec coverage: Strengthens shared score report validation for completion/quality scores and reward recommendation integrity.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `non-negative number`, `non-negative integer`, and grant/amount rules are used consistently in tests and implementation.
