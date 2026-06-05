# Phase 5m Terminal Review State Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent terminal rejected or revoked submissions from being reviewed again.

**Architecture:** Add a small server-side guard in `community-api` before mutating review state. `pending`, `held`, and `approved` keep their existing transitions; `rejected` and `revoked` become terminal in the local MVP, matching the admin UI action model and preventing duplicate rewards or republished feed posts after revoke.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory community store and HTTP routes.

---

## Files

- Modify `services/community-api/src/store.test.js`: assert rejected and revoked submissions reject later review attempts.
- Modify `services/community-api/src/store.js`: return a `submission_terminal` error before mutating terminal submissions.
- Modify `services/community-api/src/routes.test.js`: assert the route maps terminal review attempts to HTTP 409.
- Modify `services/community-api/src/routes.js`: return 409 for `submission_terminal`.

## Task 1: Store Terminal Guard

- [ ] **Step 1: Write failing store test**

Add a test:

```js
test("terminal submissions cannot be reviewed again", () => {
  const store = createCommunityStore();
  const rejected = store.createSubmission({
    petId: "pet-terminal-rejected-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-terminal-rejected-001",
    scoreReportId: "score-terminal-rejected-001"
  });
  store.reviewSubmission({
    submissionId: rejected.id,
    status: "rejected",
    reviewer: "admin-demo"
  });

  const rejectedResult = store.reviewSubmission({
    submissionId: rejected.id,
    status: "approved",
    reviewer: "admin-demo",
    rewardAmount: 40
  });

  assert.equal(rejectedResult.error, "submission_terminal");
  assert.equal(rejectedResult.status, "rejected");
  assert.equal(store.getWallet("user-demo-001").balance, 90);

  const revokedDraft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-terminal-revoked-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/terminal-revoked/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/terminal-revoked/export.zip"
      }
    },
    ownershipClaimId: "claim-terminal-revoked-001"
  });
  const revokedSubmission = store.submitImportDraft({
    draftId: revokedDraft.id,
    userId: "user-demo-001"
  }).submission;
  store.reviewSubmission({
    submissionId: revokedSubmission.id,
    status: "approved",
    reviewer: "admin-demo"
  });
  store.reviewSubmission({
    submissionId: revokedSubmission.id,
    status: "revoked",
    reviewer: "admin-demo"
  });

  const revokedResult = store.reviewSubmission({
    submissionId: revokedSubmission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  assert.equal(revokedResult.error, "submission_terminal");
  assert.equal(revokedResult.status, "revoked");
  assert.equal(store.getWallet("user-demo-001").balance, 90);
  assert.equal(
    store.getFeed().items.some((post) => post.petId === "pet-terminal-revoked-001"),
    false
  );
});
```

- [ ] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because terminal submissions can still be reviewed.

- [ ] **Step 3: Implement terminal guard**

Add:

```js
const TERMINAL_SUBMISSION_STATUSES = new Set(["rejected", "revoked"]);
```

Before `submission.status = input.status`:

```js
if (TERMINAL_SUBMISSION_STATUSES.has(submission.status)) {
  return {
    error: "submission_terminal",
    submissionId: submission.id,
    status: submission.status
  };
}
```

- [ ] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: HTTP 409 Mapping

- [ ] **Step 1: Write failing route test**

Add a route-level test:

```js
test("admin review route rejects terminal submissions", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-terminal-route-001",
      ownershipClaimId: "claim-terminal-route-001",
      scoreReportId: "score-terminal-route-001"
    }
  });
  handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "rejected",
      reviewer: "admin-demo"
    }
  });

  const response = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "approved",
      reviewer: "admin-demo",
      rewardAmount: 40
    }
  });

  assert.equal(response.status, 409);
  assert.equal(response.body.error, "submission_terminal");
  assert.equal(response.body.status, "rejected");
});
```

- [ ] **Step 2: Run route test to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because `submission_terminal` maps to 404 or 200.

- [ ] **Step 3: Implement 409 mapping**

In the admin review route:

```js
if (review.error === "submission_terminal") {
  return json(409, review);
}
```

Keep `submission_not_found` as 404.

- [ ] **Step 4: Run route test to verify GREEN**

Run the route test command again.

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
git add docs/superpowers/plans/2026-06-06-phase-5m-terminal-review-state-guard.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js
git commit -m "Guard terminal review states"
```

Expected: commit created.

## Self-Review

- Spec coverage: Prevents rejected or revoked submissions from being accidentally rewarded or republished through API calls that bypass the admin UI.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `submission_terminal`, terminal status names, and HTTP 409 mapping are used consistently.
