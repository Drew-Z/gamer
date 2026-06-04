# Phase 4b Local Scoring and Rewards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local scoring policy for community import drafts and use its reward recommendation during admin approval.

**Architecture:** Keep scoring deterministic and community-local while `fantasy-pet-rule` continues to change. The scorer consumes the stable Phase 3a/4a import draft shape and produces `gamer.pet-score-report.v1`; store state keeps reports by ID and submissions reference the generated report.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing `community-api` in-memory store, existing `pet-package-spec` score report contract.

---

### Files

- Create: `services/community-api/src/scoring.js`
  - Add `createScoreReportFromImportDraft(draft)`.
  - Score only stable fields from `draft.readiness`, `draft.importSummary`, and `draft.ownershipClaimId`.
- Create: `services/community-api/src/scoring.test.js`
  - Test high-confidence community-ready draft produces a grant recommendation.
  - Test blocked draft produces no grant and a risk.
- Modify: `services/community-api/src/store.js`
  - Add `scoreReports` state.
  - Add `getScoreReport(scoreReportId)`.
  - Generate a score report when creating an import draft unless caller provided `scoreReportId`.
  - Use score report recommendation when admin approves without explicit `rewardAmount`.
- Modify: `services/community-api/src/store.test.js`
  - Test ready import draft receives generated score report.
  - Test admin approval can grant recommended reward without explicit amount.
- Modify: `services/community-api/src/routes.js`
  - Add `GET /v1/score-reports/:id`.
  - Pass optional explicit `rewardAmount` through without breaking existing behavior.
- Modify: `services/community-api/src/server.test.js`
  - Test score report lookup by HTTP.
  - Test admin approval uses score recommendation over HTTP.

### Task 1: Scoring Policy

- [ ] **Step 1: Write failing scorer tests**

Create `services/community-api/src/scoring.test.js` with tests that call `createScoreReportFromImportDraft`.

- [ ] **Step 2: Run tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/scoring.test.js`

Expected: FAIL because `scoring.js` does not exist.

- [ ] **Step 3: Implement scorer**

Create `services/community-api/src/scoring.js` with a deterministic score report:

- `packageCompleteness`: 20 with export artifact, 12 with preview, otherwise 4.
- `visualQuality`: 18 when `readiness.status === "community-ready"`, 8 when in progress, 0 when blocked.
- `actionCoverage`: 12 when export status is ready, otherwise 6.
- `identityConsistency`: 16 when base identity is accepted, otherwise 8.
- `previewEvidence`: 10 when preview is kept and preview path exists, otherwise 0.
- `licenseReadiness`: 8 when ownership claim id exists, otherwise 0.
- Grant when total score is at least 70 and there are no blockers; reward amount is capped at 80.

- [ ] **Step 4: Run scorer tests**

Run: `npm.cmd test -- services/community-api/src/scoring.test.js`

Expected: PASS.

### Task 2: Store Integration

- [ ] **Step 1: Write failing store tests**

Add tests that import drafts get generated score reports and approval uses recommended reward.

- [ ] **Step 2: Run tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: FAIL because store has no `getScoreReport` and review does not read recommendations.

- [ ] **Step 3: Implement store integration**

Modify `services/community-api/src/store.js` to keep `scoreReports: []`, generate score reports from drafts, expose `getScoreReport`, and use recommendation amount when `rewardAmount` is omitted.

- [ ] **Step 4: Run store tests**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: PASS.

### Task 3: HTTP Integration

- [ ] **Step 1: Write failing server tests**

Add HTTP tests for `GET /v1/score-reports/:id` and admin approval using recommended reward.

- [ ] **Step 2: Run tests to verify failure**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: FAIL because score report route does not exist.

- [ ] **Step 3: Implement routes**

Modify `services/community-api/src/routes.js` to return score reports and to distinguish omitted `rewardAmount` from explicit `0`.

- [ ] **Step 4: Run server tests**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: PASS.

### Task 4: Verification and Commit

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
git add docs/superpowers/plans/2026-06-04-phase-4b-local-scoring-and-rewards.md services/community-api/src
git commit -m "Add local scoring and recommended rewards"
```

Expected: new commit created.

### Self-Review

- Spec coverage: Implements score reports and recommended rewards without binding to unstable generator internals.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `scoreReportId`, `getScoreReport`, and reward recommendation names are consistent.
