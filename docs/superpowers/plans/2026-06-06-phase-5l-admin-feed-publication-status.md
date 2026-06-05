# Phase 5l Admin Feed Publication Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show admins whether an imported submission is currently published in the community feed.

**Architecture:** Extend `community-api` admin review queue items with an optional `publishedFeedPost` summary found by the existing `post-<submissionId>` feed id. The admin-review presenter maps that optional summary into compact publication labels, and the static UI renders them alongside submission metadata.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, vanilla browser JavaScript, static admin-review frontend.

---

## Files

- Modify `services/community-api/src/store.test.js`: assert admin queue exposes published feed post before revoke and removes it after revoke.
- Modify `services/community-api/src/store.js`: include `publishedFeedPost` in `listAdminReviewQueue()` items.
- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: assert queue rows expose feed publication labels.
- Modify `apps/admin-review/src/reviewQueuePresenter.js`: derive `feedPublicationStatus`, `feedPublicationLabel`, and `publishedFeedPostId`.
- Modify `apps/admin-review/public/index.html`: add a feed publication slot to the review item template.
- Modify `apps/admin-review/public/app.js`: render feed publication status.
- Modify `apps/admin-review/public/styles.css`: style the compact feed status line.

## Task 1: Community API Queue Publication Summary

- [ ] **Step 1: Write failing store test**

Extend the existing `revoking imported submission removes approved import feed post` test:

```js
const approvedQueueItem = store
  .listAdminReviewQueue()
  .items.find((item) => item.submission.id === submissionResult.submission.id);
assert.equal(approvedQueueItem.publishedFeedPost.id, `post-${submissionResult.submission.id}`);

store.reviewSubmission({
  submissionId: submissionResult.submission.id,
  status: "revoked",
  reviewer: "admin-demo"
});

const revokedQueueItem = store
  .listAdminReviewQueue()
  .items.find((item) => item.submission.id === submissionResult.submission.id);
assert.equal(revokedQueueItem.publishedFeedPost, null);
```

- [ ] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because queue items do not expose `publishedFeedPost`.

- [ ] **Step 3: Implement queue summary**

In `listAdminReviewQueue()`, find the matching feed post:

```js
const publishedFeedPost = state.feedPosts.find(
  (post) => post.id === `post-${submission.id}`
);
```

Return:

```js
publishedFeedPost: clone(publishedFeedPost ?? null)
```

- [ ] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Admin Presenter Publication Labels

- [ ] **Step 1: Write failing presenter test**

In `queueFixture`, give the approved row:

```js
publishedFeedPost: {
  id: "post-submission-local-003",
  petId: "pet-moon-003"
}
```

Then assert:

```js
assert.equal(model.rows[0].feedPublicationStatus, "unpublished");
assert.equal(model.rows[0].feedPublicationLabel, "Feed: unpublished");
assert.equal(model.rows[1].feedPublicationStatus, "published");
assert.equal(model.rows[1].feedPublicationLabel, "Feed: post-submission-local-003");
assert.equal(model.rows[1].publishedFeedPostId, "post-submission-local-003");
```

- [ ] **Step 2: Run presenter test to verify RED**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because publication fields are missing.

- [ ] **Step 3: Implement presenter fields**

For each row:

```js
const publishedFeedPost = item.publishedFeedPost ?? null;
const publishedFeedPostId = publishedFeedPost?.id ?? "";
```

Return:

```js
publishedFeedPostId,
feedPublicationStatus: publishedFeedPostId ? "published" : "unpublished",
feedPublicationLabel: publishedFeedPostId
  ? `Feed: ${publishedFeedPostId}`
  : "Feed: unpublished"
```

- [ ] **Step 4: Run presenter test to verify GREEN**

Run the presenter test command again.

Expected: PASS.

## Task 3: Admin UI Rendering

- [ ] **Step 1: Add template slot**

Add a paragraph under `.item-meta`:

```html
<p class="feed-publication"></p>
```

- [ ] **Step 2: Render publication label**

In `renderList()`, set:

```js
const feedPublication = node.querySelector(".feed-publication");
feedPublication.textContent = row.feedPublicationLabel;
feedPublication.dataset.status = row.feedPublicationStatus;
```

- [ ] **Step 3: Style publication line**

Add CSS for `.feed-publication` and its `published` / `unpublished` data statuses.

## Task 4: Verification

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
git add docs/superpowers/plans/2026-06-06-phase-5l-admin-feed-publication-status.md services/community-api/src/store.js services/community-api/src/store.test.js apps/admin-review
git commit -m "Show admin feed publication status"
```

Expected: commit created.

## Self-Review

- Spec coverage: Gives admins visibility into whether approved generated imports are still live in the community feed after approval or revoke.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `publishedFeedPost`, `publishedFeedPostId`, `feedPublicationStatus`, and `feedPublicationLabel` are used consistently.
