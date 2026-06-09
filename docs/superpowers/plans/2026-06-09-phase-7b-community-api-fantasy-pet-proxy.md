# Phase 7b Community API Fantasy Pet Proxy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Android app use the community API as the single backend entry point for the fantasy-pet public app lifecycle.

**Architecture:** Add a narrow proxy in `services/community-api` that forwards only the fantasy-pet public app endpoints to `FANTASY_PET_API_BASE_URL`. The proxy returns JSON and zip responses unchanged, keeps `/admin/*` out of the app path, and leaves package import/review safety in the existing community store.

**Tech Stack:** Node.js `http`, Node.js `fetch`, Kotlin BuildConfig, Docker Compose, node:test.

---

### Task 1: Community API Public Proxy

**Files:**
- Modify: `services/community-api/src/server.test.js`
- Create: `services/community-api/src/fantasy-pet-proxy.js`
- Modify: `services/community-api/src/server.js`

- [x] **Step 1: Write failing HTTP proxy tests**

Add tests that prove `POST /pet-generation-jobs` forwards JSON to an upstream fantasy-pet API, `GET /pet-generation-jobs/{appJobId}/package` streams zip bytes without JSON wrapping, and `/admin/server-worker-cycle` is not proxied.

- [x] **Step 2: Run focused tests and verify RED**

Run `node --test services/community-api/src/server.test.js`. The new public proxy tests must fail before implementation because community-api currently returns `404`.

- [x] **Step 3: Implement the narrow proxy**

Create `fantasy-pet-proxy.js` with an explicit public endpoint allowlist, upstream URL resolution from `FANTASY_PET_API_BASE_URL`, and JSON error responses for missing/unreachable upstreams.

- [x] **Step 4: Wire proxy before normal JSON routing**

Update `createCommunityHttpHandler` to forward fantasy-pet public routes before parsing app JSON, then write proxied JSON/zip responses directly.

- [x] **Step 5: Run focused tests and verify GREEN**

Run `node --test services/community-api/src/server.test.js`.

### Task 2: App and Local Runtime Defaults

**Files:**
- Modify: `apps/android-community/app/build.gradle`
- Modify: `compose.fantasy-pet.yaml`
- Modify: `README.md`
- Modify: `docs/api/community-api.md`

- [x] **Step 1: Route Android default generation traffic through community-api**

Default `FANTASY_PET_API_BASE_URL` to `COMMUNITY_API_BASE_URL` unless an explicit generation base URL is provided.

- [x] **Step 2: Configure local compose proxy upstream**

Add `FANTASY_PET_API_BASE_URL=http://fantasy-pet-api:8765` to the `community-api` service in the fantasy-pet compose override.

- [x] **Step 3: Document the single-backend app path**

Update README/API docs to state that the Android app can use community-api for generation, while the backend proxy still requires a running fantasy-pet public app API and never exposes admin endpoints.

### Task 3: Verification and Commit

**Files:**
- Verify all files above

- [x] **Step 1: Run Node tests**

Run `npm.cmd test`.

- [x] **Step 2: Run Android unit tests**

Run `D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain`.

- [x] **Step 3: Validate compose config**

Run `docker compose -f compose.yaml -f compose.fantasy-pet.yaml --profile fantasy-pet config`.

- [x] **Step 4: Check whitespace and status**

Run `git diff --check` and `git status --short`.

- [ ] **Step 5: Commit**

Commit with message `Add community fantasy pet public proxy`.

---

## Self-Review

- Spec coverage: Covers single backend entry, public endpoint proxying, zip pass-through, admin exclusion, Android default routing, compose config, and docs.
- Placeholder scan: No placeholders or deferred implementation notes remain.
- Type consistency: Uses existing fantasy-pet public path names and existing `FANTASY_PET_API_BASE_URL` configuration.
