# Phase 4c Review Decisions and Revocation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add auditable admin review decisions for hold, reject, and reward revocation.

**Architecture:** Keep all money movement in the existing ledger. Approval can create positive reward entries, hold/reject create no reward, and revocation creates a negative reversal entry for any outstanding posted submission reward.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing `community-api` in-memory store and HTTP router.

---

### Files

- Modify: `services/community-api/src/store.js`
  - Treat `held`, `rejected`, and `revoked` as explicit review outcomes.
  - Add reward reversal calculation for `revoked`.
  - Return `rewardReversalEntry` from revoke reviews.
- Modify: `services/community-api/src/store.test.js`
  - Test held submissions do not grant reward.
  - Test revoked approved submissions create a negative ledger reversal and restore wallet balance.
- Modify: `services/community-api/src/server.test.js`
  - Test the HTTP admin review endpoint can revoke a previously approved reward.

### Task 1: Store Review Decisions

- [ ] **Step 1: Write failing store tests**

Add tests in `services/community-api/src/store.test.js` for:

- `held` review does not create a reward entry.
- `revoked` review creates a negative `submission-reward-reversal` ledger entry after approval.

- [ ] **Step 2: Run store tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: FAIL because `revoked` does not create a reversal entry.

- [ ] **Step 3: Implement store reversal**

Modify `services/community-api/src/store.js`:

- Calculate outstanding submission reward as posted `submission-reward` sum plus posted `submission-reward-reversal` sum for the submission.
- For `revoked`, create a negative ledger entry only when outstanding reward is greater than zero.
- Return `rewardReversalEntry` along with existing review response.

- [ ] **Step 4: Run store tests**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: PASS.

### Task 2: HTTP Revocation

- [ ] **Step 1: Write failing HTTP test**

Add a test in `services/community-api/src/server.test.js` that approves a scored submission, then revokes it through `POST /v1/admin/reviews`, and verifies wallet balance returns to the starting value.

- [ ] **Step 2: Run server tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: FAIL until store reversal is wired through.

- [ ] **Step 3: Confirm route behavior**

The existing route can keep using `store.reviewSubmission`; no new endpoint is required if the response exposes `rewardReversalEntry`.

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
git add docs/superpowers/plans/2026-06-04-phase-4c-review-decisions-and-revocation.md services/community-api/src
git commit -m "Add review revocation ledger flow"
```

Expected: new commit created.

### Self-Review

- Spec coverage: Implements hold/reject/revoke review behavior and auditable reward reversal.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `rewardReversalEntry` and `submission-reward-reversal` are named consistently.
