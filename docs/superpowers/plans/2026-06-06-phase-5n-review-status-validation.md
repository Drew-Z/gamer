# Phase 5n Review Status Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reject invalid admin review status values before they mutate submissions.

**Architecture:** Add a small review-status whitelist inside `community-api` store. The store returns a structured `invalid_review_status` error for anything outside `approved`, `held`, `rejected`, or `revoked`; the HTTP route maps that error to 400 while keeping terminal-state conflicts at 409 and missing submissions at 404.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory community store and routes.

---

## Files

- Modify `services/community-api/src/store.test.js`: assert invalid review status does not mutate a submission.
- Modify `services/community-api/src/store.js`: add review status whitelist and error return.
- Modify `services/community-api/src/routes.test.js`: assert invalid review status maps to HTTP 400.
- Modify `services/community-api/src/routes.js`: return 400 for `invalid_review_status`.

## Task 1: Store Status Validation

- [ ] **Step 1: Write failing store test**

Add:

```js
test("invalid review status is rejected without mutating submission", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-invalid-review-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-invalid-review-001",
    scoreReportId: "score-invalid-review-001"
  });

  const result = store.reviewSubmission({
    submissionId: submission.id,
    status: "published",
    reviewer: "admin-demo",
    rewardAmount: 40
  });
  const queueItem = store
    .listAdminReviewQueue()
    .items.find((item) => item.submission.id === submission.id);

  assert.equal(result.error, "invalid_review_status");
  assert.equal(result.status, "published");
  assert.deepEqual(result.allowedStatuses, ["approved", "held", "rejected", "revoked"]);
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

Expected: FAIL because invalid status currently mutates the submission.

- [ ] **Step 3: Implement whitelist**

Add:

```js
const ALLOWED_REVIEW_STATUSES = ["approved", "held", "rejected", "revoked"];
```

Before terminal guard:

```js
if (!ALLOWED_REVIEW_STATUSES.includes(input.status)) {
  return {
    error: "invalid_review_status",
    submissionId: submission.id,
    status: input.status,
    allowedStatuses: [...ALLOWED_REVIEW_STATUSES]
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
test("admin review route rejects invalid review status", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-invalid-route-001",
      ownershipClaimId: "claim-invalid-route-001",
      scoreReportId: "score-invalid-route-001"
    }
  });

  const response = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "published",
      reviewer: "admin-demo"
    }
  });

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_review_status");
  assert.deepEqual(response.body.allowedStatuses, ["approved", "held", "rejected", "revoked"]);
});
```

- [ ] **Step 2: Run route test to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because `invalid_review_status` is not mapped to 400.

- [ ] **Step 3: Implement 400 mapping**

In the admin review route:

```js
if (review.error === "invalid_review_status") {
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
git add docs/superpowers/plans/2026-06-06-phase-5n-review-status-validation.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js
git commit -m "Validate review statuses"
```

Expected: commit created.

## Self-Review

- Spec coverage: Prevents unknown review statuses from corrupting moderation state or creating unmodeled admin queue states.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `invalid_review_status` and `allowedStatuses` are used consistently in store, routes, and tests.
