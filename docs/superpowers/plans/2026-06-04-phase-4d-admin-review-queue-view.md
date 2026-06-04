# Phase 4d Admin Review Queue View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin review queue view that aggregates submissions, score reports, review history, and reward ledger status for future admin-review UI work.

**Architecture:** Keep aggregation inside `community-api` store while state is in-memory. The HTTP route returns one ready-to-render queue item per submission so an admin client does not have to stitch together multiple endpoints.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing `community-api` in-memory store and HTTP router.

---

### Files

- Modify: `services/community-api/src/store.js`
  - Add `listAdminReviewQueue()`.
  - Aggregate submission, score report, reviews, reward ledger entries, and outstanding reward.
- Modify: `services/community-api/src/store.test.js`
  - Test queue item contains score report, review history, reward ledger entries, and outstanding reward after approve/revoke.
- Modify: `services/community-api/src/routes.js`
  - Add `GET /v1/admin/review-queue`.
- Modify: `services/community-api/src/server.test.js`
  - Test HTTP route returns the aggregated review queue.

### Task 1: Store Aggregation

- [ ] **Step 1: Write failing store test**

Add a test in `services/community-api/src/store.test.js` that creates a scored import draft, submits it, approves it, revokes it, then calls `store.listAdminReviewQueue()`.

Expected item shape:

```js
{
  submission,
  scoreReport,
  reviews,
  rewardLedgerEntries,
  outstandingReward
}
```

- [ ] **Step 2: Run store tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: FAIL because `listAdminReviewQueue` does not exist.

- [ ] **Step 3: Implement store aggregation**

Modify `services/community-api/src/store.js`:

- Add a helper for submission ledger entries.
- Add `listAdminReviewQueue()` that maps each submission to the aggregate shape.
- `outstandingReward` should equal the sum of posted `submission-reward` and `submission-reward-reversal` entries for that submission.

- [ ] **Step 4: Run store tests**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: PASS.

### Task 2: HTTP Route

- [ ] **Step 1: Write failing HTTP test**

Add a test in `services/community-api/src/server.test.js` that creates, submits, approves, and fetches `GET /v1/admin/review-queue`.

- [ ] **Step 2: Run server tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: FAIL because route is missing.

- [ ] **Step 3: Implement route**

Modify `services/community-api/src/routes.js`:

```js
if (method === "GET" && url.pathname === "/v1/admin/review-queue") {
  return json(200, store.listAdminReviewQueue());
}
```

- [ ] **Step 4: Run server tests**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: PASS.

### Task 3: Verification and Commit

- [ ] **Step 1: Run Node tests**

Run: `npm.cmd test`

Expected: all Node tests pass.

- [ ] **Step 2: Run Android unit tests**

Run: `D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Check Docker config**

Run: `docker compose config`

Expected: config prints successfully.

- [ ] **Step 4: Commit**

Run:

```bash
git add docs/superpowers/plans/2026-06-04-phase-4d-admin-review-queue-view.md services/community-api/src
git commit -m "Add admin review queue aggregate"
```

Expected: new commit created.

### Self-Review

- Spec coverage: Supports future admin-review UI by exposing review, scoring, and reward evidence together.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `listAdminReviewQueue`, `rewardLedgerEntries`, and `outstandingReward` are consistent across store and route tests.
