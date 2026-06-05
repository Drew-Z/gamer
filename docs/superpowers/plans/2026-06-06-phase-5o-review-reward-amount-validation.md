# Phase 5o Review Reward Amount Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reject invalid explicit admin review reward amounts before they mutate submissions or ledger entries.

**Architecture:** Validate explicit `rewardAmount` inside `community-api` store so every caller shares the same guard. Omitted reward amounts still use the score recommendation; explicit values must be non-negative integers. The HTTP route maps the store error to 400.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory community store and routes.

---

## Files

- Modify `services/community-api/src/store.test.js`: assert invalid explicit reward amounts do not mutate submissions or wallet state.
- Modify `services/community-api/src/store.js`: add explicit reward amount validation.
- Modify `services/community-api/src/routes.test.js`: assert invalid reward amount maps to HTTP 400.
- Modify `services/community-api/src/routes.js`: return 400 for `invalid_reward_amount`.

## Task 1: Store Reward Amount Validation

- [ ] **Step 1: Write failing store test**

Add:

```js
test("invalid explicit reward amount is rejected without mutating submission", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-invalid-reward-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-invalid-reward-001",
    scoreReportId: "score-invalid-reward-001"
  });

  const result = store.reviewSubmission({
    submissionId: submission.id,
    status: "approved",
    reviewer: "admin-demo",
    rewardAmount: -5
  });
  const queueItem = store
    .listAdminReviewQueue()
    .items.find((item) => item.submission.id === submission.id);

  assert.equal(result.error, "invalid_reward_amount");
  assert.equal(result.rewardAmount, -5);
  assert.equal(queueItem.submission.status, "pending");
  assert.equal(queueItem.reviews.length, 0);
  assert.equal(store.getWallet("user-demo-001").balance, 90);
});
```

- [ ] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because invalid explicit reward amounts are not rejected.

- [ ] **Step 3: Implement store validation**

Add helper:

```js
const isValidExplicitRewardAmount = (amount) =>
  amount === undefined || (Number.isInteger(amount) && amount >= 0);
```

Before `submission.status = input.status`:

```js
if (!isValidExplicitRewardAmount(input.rewardAmount)) {
  return {
    error: "invalid_reward_amount",
    submissionId: submission.id,
    rewardAmount: input.rewardAmount
  };
}
```

- [ ] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: HTTP 400 Mapping

- [ ] **Step 1: Write failing route test**

Add:

```js
test("admin review route rejects invalid reward amount", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-invalid-reward-route-001",
      ownershipClaimId: "claim-invalid-reward-route-001",
      scoreReportId: "score-invalid-reward-route-001"
    }
  });

  const response = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "approved",
      reviewer: "admin-demo",
      rewardAmount: -5
    }
  });

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_reward_amount");
  assert.equal(response.body.rewardAmount, -5);
});
```

- [ ] **Step 2: Run route test to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because `invalid_reward_amount` is not mapped to 400.

- [ ] **Step 3: Implement route mapping**

In the admin review route error branch:

```js
if (review.error === "invalid_reward_amount") {
  return json(400, review);
}
```

- [ ] **Step 4: Run route test to verify GREEN**

Run the route test command again.

Expected: PASS.

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
git add docs/superpowers/plans/2026-06-06-phase-5o-review-reward-amount-validation.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js
git commit -m "Validate review reward amounts"
```

Expected: commit created.

## Self-Review

- Spec coverage: Prevents explicit invalid reward amounts from corrupting wallet ledger state or producing unexpected approved submissions.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `invalid_reward_amount` and `rewardAmount` are used consistently in store, routes, and tests.
