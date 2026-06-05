# Phase 5k Revoke Approved Import Feed Post Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove published approved-import feed posts when their submission is revoked.

**Architecture:** Keep feed publishing and unpublishing inside the local `community-api` store. Approval still publishes one stable feed post per imported submission; revoke now reverses the reward ledger and removes the matching `post-<submissionId>` feed item so Android no longer presents revoked imports as approved community content.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory community store.

---

## Files

- Modify `services/community-api/src/store.test.js`: add coverage for revoke removing an approved import feed post.
- Modify `services/community-api/src/store.js`: remove the matching approved-import feed post when an imported submission is revoked.

## Task 1: Store Revoke Feed Unpublish

- [ ] **Step 1: Write failing store test**

Add a test to `services/community-api/src/store.test.js`:

```js
test("revoking imported submission removes approved import feed post", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-feed-revoked-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-revoked/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-revoked/export.zip"
      }
    },
    ownershipClaimId: "claim-pet-feed-revoked-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });
  assert.ok(
    store.getFeed().items.some((post) => post.petId === "pet-feed-revoked-001")
  );

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "revoked",
    reviewer: "admin-demo"
  });

  assert.equal(
    store.getFeed().items.some((post) => post.petId === "pet-feed-revoked-001"),
    false
  );
});
```

- [ ] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because revoke does not remove the previously published feed post.

- [ ] **Step 3: Implement feed unpublish**

In `reviewSubmission()`, inside the `input.status === "revoked"` branch, remove the matching feed post when `submission.importDraftId` exists:

```js
if (submission.importDraftId) {
  const feedPostId = `post-${submission.id}`;
  state.feedPosts = state.feedPosts.filter((post) => post.id !== feedPostId);
}
```

- [ ] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Verification

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
git add docs/superpowers/plans/2026-06-06-phase-5k-revoke-approved-import-feed-post.md services/community-api/src/store.js services/community-api/src/store.test.js
git commit -m "Remove revoked import posts from feed"
```

Expected: commit created.

## Self-Review

- Spec coverage: Keeps community feed state consistent with moderation revocation for generated imports.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses the existing `post-<submissionId>` feed id convention introduced for approved imports.
