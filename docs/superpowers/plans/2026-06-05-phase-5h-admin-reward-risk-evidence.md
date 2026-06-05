# Phase 5h Admin Reward Risk Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show reward recommendation reasoning and concrete risk evidence in the admin review queue.

**Architecture:** Keep evidence formatting in the existing admin-review presenter so the browser UI only renders rows. The static admin console adds a compact recommendation/risk evidence section to each submission card without changing review actions or backend APIs.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, vanilla browser JavaScript, static admin-review frontend.

---

## Files

- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: assert recommendation reason and risk details are present.
- Modify `apps/admin-review/src/reviewQueuePresenter.js`: add stable `recommendationReason` and `riskItems` row fields.
- Modify `apps/admin-review/public/index.html`: add recommendation and risk containers to the queue template.
- Modify `apps/admin-review/public/app.js`: render recommendation reason and risk list.
- Modify `apps/admin-review/public/styles.css`: style the evidence section.

## Task 1: Presenter Evidence Model

- [ ] **Step 1: Add failing presenter test**

Extend `createReviewDashboardModel` assertions to require:

```js
assert.equal(model.rows[0].recommendationReason, "Community-ready import has enough accepted evidence.");
assert.deepEqual(model.rows[1].riskItems, ["manual IP review requested"]);
```

- [ ] **Step 2: Run presenter tests**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because `riskItems` does not exist.

- [ ] **Step 3: Implement presenter fields**

Return:

```js
recommendationReason: rewardRecommendation.reason ?? "No recommendation reason.",
riskItems: risks
```

Keep the existing `risks` and `riskLabel` fields for compatibility.

- [ ] **Step 4: Run presenter tests**

Run the presenter test command again.

Expected: PASS.

## Task 2: Browser UI

- [ ] **Step 1: Add template markup**

Add an evidence section inside the queue item template:

```html
<section class="recommendation-block">
  <h4>Reward Recommendation</h4>
  <p class="recommendation-reason"></p>
  <ul class="risk-list"></ul>
</section>
```

- [ ] **Step 2: Render recommendation and risks**

In `renderList()`, set `.recommendation-reason` and render `row.riskItems`. If there are no risks, render `No risk evidence.`.

- [ ] **Step 3: Style evidence block**

Use compact styles matching the existing evidence grid, with muted recommendation text and risk items that wrap cleanly.

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
git add docs/superpowers/plans/2026-06-05-phase-5h-admin-reward-risk-evidence.md apps/admin-review
git commit -m "Show reward and risk evidence in admin review"
```

Expected: commit created.

## Self-Review

- Spec coverage: Improves the reward review workflow by exposing recommendation rationale and concrete risks to admins.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `recommendationReason`, `riskItems`, and existing `riskLabel` naming stays consistent across presenter and UI.
