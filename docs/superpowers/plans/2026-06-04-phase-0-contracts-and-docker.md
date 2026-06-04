# Phase 0 Contracts And Docker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the initial `gamer` workspace with shared contracts, fixtures, minimal service skeletons, tests, and Docker Compose support.

**Architecture:** Use a lightweight Node.js ESM monorepo with npm workspaces. Shared packages define contracts and fixtures; services consume those packages and expose minimal HTTP endpoints so the project can run locally or in Docker without a database.

**Tech Stack:** Node.js 22, npm workspaces, built-in `node:test`, built-in `node:http`, Docker Compose.

---

## File Structure

- Create `package.json`: root npm workspace scripts.
- Create `.dockerignore`: keep Docker build contexts small.
- Create `compose.yaml`: local multi-service Docker entry point.
- Create `README.md`: quickstart and architecture overview.
- Create `packages/community-contracts`: user, feed, wallet, submission, review DTO fixtures.
- Create `packages/pet-package-spec`: pet package, ownership claim, score report, ledger validators.
- Create `packages/pet-runtime`: action, speech bubble, app launch, feed navigation runtime model.
- Create `services/community-api`: minimal HTTP API serving fixture-backed endpoints.
- Create `services/pet-generator`: minimal adapter API that reports known `fantasy-pet-rule` run states from fixture input.

## Task 1: Root Workspace

**Files:**
- Create: `package.json`
- Create: `.dockerignore`
- Create: `README.md`

- [ ] **Step 1: Write root workspace files**

Create npm workspace scripts:

```json
{
  "name": "gamer",
  "private": true,
  "type": "module",
  "scripts": {
    "test": "node --test \"packages/**/*.test.js\" \"services/**/*.test.js\"",
    "start:community-api": "node services/community-api/src/server.js",
    "start:pet-generator": "node services/pet-generator/src/server.js"
  },
  "workspaces": [
    "packages/*",
    "services/*"
  ],
  "engines": {
    "node": ">=22"
  }
}
```

Create `.dockerignore` with:

```text
.git
.superpowers
node_modules
**/node_modules
npm-debug.log
Dockerfile
compose.yaml
```

Create `README.md` with quickstart commands:

```markdown
# Gamer

Pet-first community ecosystem workspace.

## Local

Run tests:

```powershell
npm test
```

Run services:

```powershell
npm run start:community-api
npm run start:pet-generator
```

## Docker

```powershell
docker compose up --build
```
```

- [ ] **Step 2: Verify root scripts parse**

Run: `npm pkg get scripts`

Expected: JSON output includes `test`, `start:community-api`, and `start:pet-generator`.

## Task 2: Shared Community Contracts

**Files:**
- Create: `packages/community-contracts/package.json`
- Create: `packages/community-contracts/src/index.js`
- Create: `packages/community-contracts/src/fixtures.js`
- Create: `packages/community-contracts/src/index.test.js`

- [ ] **Step 1: Write fixtures and exports**

Create fixture users, feed posts, wallet, check-in, submission, and review records. Export them from `src/index.js`.

- [ ] **Step 2: Write fixture tests**

Test that fixture IDs are stable, feed posts have pet references, and wallet balance equals posted ledger sum.

- [ ] **Step 3: Run community contract tests**

Run: `node --test packages/community-contracts/src/index.test.js`

Expected: PASS.

## Task 3: Pet Package Spec Validators

**Files:**
- Create: `packages/pet-package-spec/package.json`
- Create: `packages/pet-package-spec/src/index.js`
- Create: `packages/pet-package-spec/src/validators.js`
- Create: `packages/pet-package-spec/src/fixtures.js`
- Create: `packages/pet-package-spec/src/validators.test.js`

- [ ] **Step 1: Write validators**

Implement validators for:

- `validatePetPackageManifest`
- `validateOwnershipClaim`
- `validateScoreReport`
- `validateCurrencyLedgerEntry`

Each validator returns `{ ok: true, errors: [] }` or `{ ok: false, errors: ["field message"] }`.

- [ ] **Step 2: Write valid fixtures**

Create one valid pet package manifest, ownership claim, score report, and ledger entry.

- [ ] **Step 3: Write validator tests**

Test valid fixtures pass and a manifest without `petId` fails.

- [ ] **Step 4: Run validator tests**

Run: `node --test packages/pet-package-spec/src/validators.test.js`

Expected: PASS.

## Task 4: Pet Runtime Model

**Files:**
- Create: `packages/pet-runtime/package.json`
- Create: `packages/pet-runtime/src/index.js`
- Create: `packages/pet-runtime/src/actions.js`
- Create: `packages/pet-runtime/src/runtime.test.js`

- [ ] **Step 1: Write runtime model**

Define actions:

- `idle`
- `app-loading`
- `bubble-open`
- `feed-next`
- `feed-previous`
- `feed-skip`
- `reward`
- `review`

Export helpers:

- `getActionForLaunchStage(stage)`
- `getActionForFeedNavigation(direction)`

- [ ] **Step 2: Write runtime tests**

Test launch loading maps to `app-loading`, launch ready maps to `bubble-open`, and feed directions map to the three feed actions.

- [ ] **Step 3: Run runtime tests**

Run: `node --test packages/pet-runtime/src/runtime.test.js`

Expected: PASS.

## Task 5: Minimal Community API

**Files:**
- Create: `services/community-api/package.json`
- Create: `services/community-api/Dockerfile`
- Create: `services/community-api/src/server.js`
- Create: `services/community-api/src/routes.js`
- Create: `services/community-api/src/routes.test.js`

- [ ] **Step 1: Write route handler**

Implement `handleCommunityRequest(method, url)` returning `{ status, body }` for:

- `GET /health`
- `GET /v1/feed`
- `GET /v1/wallet/me`
- `POST /v1/check-in`
- `GET /v1/submissions`

- [ ] **Step 2: Write HTTP server**

Use Node `http.createServer` and `process.env.PORT || 4000`.

- [ ] **Step 3: Write route tests**

Test `/health`, `/v1/feed`, and unsupported routes.

- [ ] **Step 4: Run API tests**

Run: `node --test services/community-api/src/routes.test.js`

Expected: PASS.

## Task 6: Minimal Pet Generator Adapter

**Files:**
- Create: `services/pet-generator/package.json`
- Create: `services/pet-generator/Dockerfile`
- Create: `services/pet-generator/src/server.js`
- Create: `services/pet-generator/src/adapter.js`
- Create: `services/pet-generator/src/adapter.test.js`

- [ ] **Step 1: Write adapter**

Implement `summarizeFantasyPetRuleState(state)` with these rules:

- Missing state returns `blocked`.
- `preview.userDecision === "keep"` returns `community-ready`.
- Any blocker returns `blocked`.
- Otherwise return `in-progress`.

- [ ] **Step 2: Write HTTP server**

Expose:

- `GET /health`
- `POST /v1/fantasy-pet-rule/summarize`

- [ ] **Step 3: Write adapter tests**

Test ready, blocked, and in-progress states.

- [ ] **Step 4: Run adapter tests**

Run: `node --test services/pet-generator/src/adapter.test.js`

Expected: PASS.

## Task 7: Docker Compose

**Files:**
- Create: `compose.yaml`
- Modify: `services/community-api/Dockerfile`
- Modify: `services/pet-generator/Dockerfile`

- [ ] **Step 1: Write Compose services**

Create `compose.yaml` with services:

- `community-api`, build context `.`, dockerfile `services/community-api/Dockerfile`, port `4000:4000`
- `pet-generator`, build context `.`, dockerfile `services/pet-generator/Dockerfile`, port `4100:4100`

- [ ] **Step 2: Verify compose config**

Run: `docker compose config`

Expected: Compose prints normalized service config.

## Task 8: Full Verification And Commit

**Files:**
- All Phase 0 files.

- [ ] **Step 1: Run all tests**

Run: `npm test`

Expected: all tests pass.

- [ ] **Step 2: Run git status**

Run: `git status --short`

Expected: Phase 0 files are uncommitted.

- [ ] **Step 3: Commit**

Run:

```powershell
git add .
git commit -m "Add phase 0 contracts and docker skeleton"
```

Expected: commit succeeds.

## Self-Review

Spec coverage:

- Workspace split is covered by package and service directories.
- Shared package contracts are covered by Tasks 2, 3, and 4.
- Docker runtime is covered by Task 7.
- Generator integration boundary is covered by Task 6.
- Tests are covered by Tasks 2 through 8.

Placeholder scan:

- No implementation step depends on unspecified future behavior.

Type consistency:

- Route handlers consistently return `{ status, body }`.
- Validators consistently return `{ ok, errors }`.
- Generator summaries consistently return `{ status, reason, evidence }`.
