# Phase 4a Community Import Drafts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a community-side import draft workflow that can accept `pet-generator` read-only import summaries while `fantasy-pet-rule` remains unstable.

**Architecture:** Keep import drafts inside `community-api` local store state. A draft records readiness, source/import summary, and user ownership intent; only `community-ready` drafts can be promoted into existing submissions.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory community store and HTTP router.

---

### Files

- Modify: `services/community-api/src/store.js`
  - Add `importDrafts` state.
  - Add `listImportDrafts()`.
  - Add `createImportDraft(input)`.
  - Add `submitImportDraft(input)`.
- Modify: `services/community-api/src/store.test.js`
  - Test blocked drafts are stored but cannot submit.
  - Test community-ready drafts can become pending submissions.
- Modify: `services/community-api/src/routes.js`
  - Add `GET /v1/import-drafts`.
  - Add `POST /v1/import-drafts`.
  - Add `POST /v1/import-drafts/submit`.
- Modify: `services/community-api/src/server.test.js`
  - Test draft creation over HTTP.
  - Test ready draft submission over HTTP.

### Task 1: Store Draft State

- [ ] **Step 1: Write failing store tests**

Add tests to `services/community-api/src/store.test.js`:

```js
test("blocked import draft is stored but cannot be submitted", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "blocked",
      reason: "state is missing"
    },
    importSummary: {
      source: {
        petId: "pet-blocked-001"
      }
    }
  });

  const result = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  assert.equal(draft.status, "blocked");
  assert.equal(result.error, "draft_not_ready");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});
```

```js
test("community-ready import draft creates pending submission", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-ready-001"
      }
    },
    ownershipClaimId: "claim-pet-ready-001",
    scoreReportId: "score-pet-ready-001"
  });

  const result = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  assert.equal(result.draft.status, "submitted");
  assert.equal(result.submission.status, "pending");
  assert.equal(result.submission.petId, "pet-ready-001");
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: FAIL because import draft methods do not exist.

- [ ] **Step 3: Implement store methods**

Modify `services/community-api/src/store.js`:

- Add `importDrafts: []` to `defaultSeed`.
- Add draft status mapping:
  - `community-ready` -> `ready`
  - `blocked` -> `blocked`
  - everything else -> `in-progress`
- Add `createImportDraft(input)` that stores:
  - `id`
  - `userId`
  - `status`
  - `readiness`
  - `importSummary`
  - `petId`
  - `ownershipClaimId`
  - `scoreReportId`
  - `createdAt`
- Add `submitImportDraft(input)` that:
  - returns `draft_not_found` if missing or wrong user;
  - returns `draft_not_ready` unless `draft.status === "ready"`;
  - creates a submission from draft ids;
  - marks draft `submitted`;
  - returns `{ draft, submission }`.

- [ ] **Step 4: Run store tests**

Run: `npm.cmd test -- services/community-api/src/store.test.js`

Expected: PASS.

### Task 2: HTTP Routes

- [ ] **Step 1: Write failing server tests**

Add tests to `services/community-api/src/server.test.js` for:

- `POST /v1/import-drafts` creating an `in-progress` or `ready` draft.
- `POST /v1/import-drafts/submit` converting a ready draft to a pending submission.

- [ ] **Step 2: Run tests to verify they fail**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: FAIL because routes do not exist.

- [ ] **Step 3: Implement routes**

Modify `services/community-api/src/routes.js`:

- `GET /v1/import-drafts` returns `store.listImportDrafts(currentUserId)`.
- `POST /v1/import-drafts` calls `store.createImportDraft(...)` and returns `201`.
- `POST /v1/import-drafts/submit` calls `store.submitImportDraft(...)`.
- Return `404` for `draft_not_found`.
- Return `409` for `draft_not_ready`.

- [ ] **Step 4: Run server tests**

Run: `npm.cmd test -- services/community-api/src/server.test.js`

Expected: PASS.

### Task 3: Verification and Commit

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
git add docs/superpowers/plans/2026-06-04-phase-4a-community-import-drafts.md services/community-api/src
git commit -m "Add community import draft workflow"
```

Expected: new commit created.

### Self-Review

- Spec coverage: Keeps `fantasy-pet-rule` integration buffered while enabling community progress.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: `importDraft`, `draft_not_ready`, and `draft_not_found` are consistent across store and routes.
