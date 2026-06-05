# Phase 5d Fantasy Pet Rule Import Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `community-api` create import drafts directly from `fantasy-pet-rule` state or `statePath` without manually copying `pet-generator` responses.

**Architecture:** Keep `pet-generator` as the source of readiness/import-summary semantics. `community-api` imports the read-only state resolver and summary creator, then stores the resulting draft through the existing import draft workflow.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory `community-api` and `pet-generator` modules.

---

## Files

- Modify `services/community-api/src/routes.js`: add `POST /v1/import-drafts/from-fantasy-pet-rule`.
- Modify `services/community-api/src/routes.test.js`: cover inline state bridge behavior.
- Modify `services/community-api/src/server.js`: await route results so `statePath` reads can complete.
- Modify `services/community-api/src/server.test.js`: cover HTTP `statePath` bridge behavior.

## Task 1: Route Bridge

- [ ] **Step 1: Add failing route test**

Add a test that posts inline `state` to `/v1/import-drafts/from-fantasy-pet-rule` and expects a ready import draft with a generated score report.

- [ ] **Step 2: Run route tests**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because the bridge route does not exist.

- [ ] **Step 3: Implement bridge route**

Import `createFantasyPetRuleImportSummary`, `resolveFantasyPetRuleState`, and `StateSourceError`. Add the route and convert state-source errors into API errors.

- [ ] **Step 4: Run route tests**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: PASS.

## Task 2: HTTP StatePath Bridge

- [ ] **Step 1: Add failing HTTP test**

Add a server test that posts `statePath` to `/v1/import-drafts/from-fantasy-pet-rule` with an injected `readFile`.

- [ ] **Step 2: Run server tests**

Run:

```powershell
node --test services/community-api/src/server.test.js
```

Expected: FAIL until `createCommunityHttpHandler` awaits route results.

- [ ] **Step 3: Await route results**

Update `createCommunityHttpHandler()` to `await handleCommunityRequest(...)`.

- [ ] **Step 4: Run server tests**

Run:

```powershell
node --test services/community-api/src/server.test.js
```

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

- [ ] **Step 4: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-05-phase-5d-fantasy-pet-rule-import-bridge.md services/community-api/src
git commit -m "Bridge fantasy pet state to import drafts"
```

Expected: commit created.

## Self-Review

- Spec coverage: Connects `fantasy-pet-rule` state ingestion to community import drafts while preserving the read-only adapter gate.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `readiness`, `importSummary`, and import draft shapes.
