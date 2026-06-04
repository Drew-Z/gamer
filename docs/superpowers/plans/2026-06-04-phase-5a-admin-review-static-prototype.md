# Phase 5a Admin Review Static Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a runnable `apps/admin-review` prototype that consumes the community API admin review queue and exposes approve, hold, reject, and revoke controls.

**Architecture:** Use a static web app with a tiny Node static server. Keep display mapping in a tested ESM presenter module so the prototype can evolve into React/Vite later without losing API semantics.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, vanilla HTML/CSS/JS, Docker Compose.

---

### Files

- Create: `apps/admin-review/package.json`
- Create: `apps/admin-review/server.js`
- Create: `apps/admin-review/Dockerfile`
- Create: `apps/admin-review/src/reviewQueuePresenter.js`
- Create: `apps/admin-review/src/reviewQueuePresenter.test.js`
- Create: `apps/admin-review/public/index.html`
- Create: `apps/admin-review/public/styles.css`
- Create: `apps/admin-review/public/app.js`
- Modify: `package.json`
  - Include `apps/**/*.test.js` in `npm.cmd test`.
  - Add `start:admin-review`.
- Modify: `compose.yaml`
  - Add `admin-review` on port `4200`.

### Task 1: Presenter Logic

- [ ] **Step 1: Write failing presenter tests**

Create tests for status counts, reward formatting, and action availability.

- [ ] **Step 2: Run tests to verify failure**

Run: `npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js`

Expected: FAIL because presenter module does not exist.

- [ ] **Step 3: Implement presenter**

Create `reviewQueuePresenter.js` with:

- `createReviewDashboardModel(queue)`
- `formatReward(amount)`
- `actionsForStatus(status)`

- [ ] **Step 4: Run presenter tests**

Run: `npm.cmd test -- apps/admin-review/src/reviewQueuePresenter.test.js`

Expected: PASS.

### Task 2: Static App

- [ ] **Step 1: Create app shell**

Add HTML, CSS, browser JS, and static server. UI must show:

- queue summary counts;
- submission rows with score, status, outstanding reward;
- score breakdown;
- review history;
- approve, hold, reject, revoke buttons.

- [ ] **Step 2: Add root scripts and Docker service**

Update `package.json` and `compose.yaml`.

- [ ] **Step 3: Run app tests and config**

Run:

```bash
npm.cmd test
docker compose config
```

Expected: both pass.

### Task 3: Browser Smoke

- [ ] **Step 1: Start backend and admin app**

Run community API and admin review server locally.

- [ ] **Step 2: Open admin app**

Open `http://127.0.0.1:4200` and verify the UI renders.

- [ ] **Step 3: Exercise core action**

Click at least one review action and verify the queue refreshes.

### Task 4: Verification and Commit

- [ ] **Step 1: Run Node tests**

Run: `npm.cmd test`

Expected: all Node tests pass.

- [ ] **Step 2: Run Android unit tests**

Run: `D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Check Docker config**

Run: `docker compose config`

Expected: config prints successfully and includes `admin-review`.

- [ ] **Step 4: Commit**

Run:

```bash
git add docs/superpowers/plans/2026-06-04-phase-5a-admin-review-static-prototype.md package.json compose.yaml apps/admin-review
git commit -m "Add admin review static prototype"
```

Expected: new commit created.

### Self-Review

- Spec coverage: Adds the first runnable admin-review app and keeps it connected to the existing review queue contract.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Presenter and browser code use `items`, `submission`, `scoreReport`, `reviews`, `rewardLedgerEntries`, and `outstandingReward` consistently.
