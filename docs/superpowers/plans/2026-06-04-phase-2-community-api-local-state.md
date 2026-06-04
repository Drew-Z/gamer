# Phase 2 Community API Local State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `community-api` from fixture-only responses into a local mutable API for feed, wallet, daily check-in, submissions, review decisions, and reward ledger entries.

**Architecture:** Keep the service database-free for this phase. Add a small in-memory store factory seeded from shared fixtures, expose pure route handlers that accept parsed JSON bodies, and keep the HTTP server as a thin body-parsing wrapper.

**Tech Stack:** Node.js 22, built-in `node:test`, built-in `node:http`, existing shared fixtures.

---

## File Structure

- Create `services/community-api/src/store.js`: in-memory state, mutations, and query helpers.
- Create `services/community-api/src/store.test.js`: store behavior tests.
- Modify `services/community-api/src/routes.js`: route through store and request bodies.
- Modify `services/community-api/src/routes.test.js`: route tests for mutable flows.
- Modify `services/community-api/src/server.js`: parse JSON request body and pass it to routes.
- Modify `README.md`: document local API endpoints.

## Task 1: In-Memory Store

**Files:**
- Create: `services/community-api/src/store.js`
- Create: `services/community-api/src/store.test.js`

- [ ] **Step 1: Write failing store tests**

Cover:

- Fresh store exposes initial wallet balance.
- First daily check-in posts a new ledger entry and increases balance.
- Second daily check-in returns existing claim without increasing balance.
- Creating a submission adds a pending submission.
- Approving a submission posts a reward ledger entry and marks review approved.

- [ ] **Step 2: Implement store**

Create `createCommunityStore(seed)` with methods:

- `getMe()`
- `getFeed()`
- `getWallet(userId)`
- `claimDailyCheckIn(userId, date)`
- `listSubmissions()`
- `createSubmission(input)`
- `reviewSubmission(input)`

- [ ] **Step 3: Run store tests**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: PASS.

## Task 2: Routes With Request Bodies

**Files:**
- Modify: `services/community-api/src/routes.js`
- Modify: `services/community-api/src/routes.test.js`

- [ ] **Step 1: Write failing route tests**

Cover:

- `POST /v1/check-in` can accept `{ "date": "2026-06-05" }`.
- `POST /v1/submissions` creates a pending submission.
- `POST /v1/admin/reviews` approves a submission and updates wallet.
- Unsupported route still returns 404.

- [ ] **Step 2: Implement route handling**

Change `handleCommunityRequest(method, requestUrl, options)` where `options` can include:

- `store`
- `body`

Default store should be a module-level singleton for the live server. Tests can inject a fresh store.

- [ ] **Step 3: Run route tests**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: PASS.

## Task 3: HTTP Server Body Parsing

**Files:**
- Modify: `services/community-api/src/server.js`

- [ ] **Step 1: Update server**

Read request body, parse JSON when present, return `400` for invalid JSON, and pass parsed body into `handleCommunityRequest`.

- [ ] **Step 2: Smoke-test server through route tests**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: PASS.

## Task 4: Docs And Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document endpoints**

Add examples for:

- `GET /v1/feed`
- `GET /v1/wallet/me`
- `POST /v1/check-in`
- `POST /v1/submissions`
- `POST /v1/admin/reviews`

- [ ] **Step 2: Run final verification**

Run:

```powershell
npm.cmd test
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
docker compose config
```

Expected: all commands pass.

- [ ] **Step 3: Commit**

Run:

```powershell
git add .
git commit -m "Add local state community API flows"
```

Expected: commit succeeds.

## Self-Review

Spec coverage:

- Feed remains available.
- Wallet, daily check-in, submission, review, and reward ledger flows become locally interactive.
- Docker compatibility is preserved by keeping the same service entrypoint.
- No database is introduced.

Placeholder scan:

- No route depends on a future database or external service.

Type consistency:

- Store methods return plain objects.
- Route handlers return `{ status, body }`.
- Ledger entries consistently use `entryId`, `userId`, `amount`, `sourceType`, `sourceId`, `status`, and `createdAt`.
