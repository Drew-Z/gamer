# Phase 5y Submitted Pet Package Bundle Duplicate Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent duplicate pet package bundle imports even after the first draft has been submitted.

**Architecture:** Keep duplicate detection in `community-api` store because it owns all draft states. Reuse the existing `duplicate_import_draft` error and HTTP 409 mapping so clients have one duplicate import behavior to handle.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory store, route handler, and HTTP tests.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED test for duplicate bundle import after submit.
- Modify `services/community-api/src/store.js`: remove the active-only condition from duplicate detection.
- Modify `services/community-api/src/routes.test.js`: add route coverage for duplicate after submit.
- Modify `services/community-api/src/server.test.js`: add HTTP coverage for duplicate after submit.

## Task 1: Store Submitted Duplicate Guard

- [x] **Step 1: Write failing store test**

Add:

```js
test("pet package bundle cannot create duplicate import draft after submission", () => {
  const store = createCommunityStore();
  const first = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const submitted = store.submitImportDraft({
    draftId: first.id,
    userId: "user-demo-001"
  });
  const second = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });

  assert.equal(submitted.submission.petId, "pet-stardust-001");
  assert.equal(second.error, "duplicate_import_draft");
  assert.equal(second.petId, "pet-stardust-001");
  assert.equal(second.existingDraftId, first.id);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});
```

- [x] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because duplicate detection currently ignores submitted drafts.

- [x] **Step 3: Implement store guard**

Change the duplicate lookup from:

```js
draft.userId === input.userId &&
draft.petId === input.bundle.manifest.petId &&
draft.status !== "submitted"
```

to:

```js
draft.userId === input.userId && draft.petId === input.bundle.manifest.petId
```

- [x] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Route Submitted Duplicate Guard

- [x] **Step 1: Write route test**

Add:

```js
test("pet package bundle route rejects duplicate import after submission", () => {
  const store = createCommunityStore();
  const first = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
  handleCommunityRequest("POST", "/v1/import-drafts/submit", {
    store,
    body: {
      draftId: first.body.id
    }
  });
  const second = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(first.status, 201);
  assert.equal(second.status, 409);
  assert.equal(second.body.error, "duplicate_import_draft");
  assert.equal(second.body.existingDraftId, first.body.id);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});
```

- [x] **Step 2: Run route test**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: PASS after store guard changes.

## Task 3: HTTP Submitted Duplicate Guard

- [x] **Step 1: Write HTTP test**

Add:

```js
test("HTTP server rejects duplicate pet package bundle import after submission", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const first = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        bundle: validPetPackageBundle
      }
    );
    await requestJson(server, "POST", "/v1/import-drafts/submit", {
      draftId: first.body.id
    });
    const second = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        bundle: validPetPackageBundle
      }
    );

    assert.equal(first.status, 201);
    assert.equal(second.status, 409);
    assert.equal(second.body.error, "duplicate_import_draft");
    assert.equal(second.body.existingDraftId, first.body.id);
    assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});
```

- [x] **Step 2: Run HTTP test**

Run:

```powershell
node --test services/community-api/src/server.test.js
```

Expected: PASS.

## Task 4: Verification

- [x] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [x] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

Expected: config renders all services.

- [x] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only this phase's files are modified.

- [ ] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-06-phase-5y-submitted-pet-package-bundle-duplicate-guard.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.test.js services/community-api/src/server.test.js
git commit -m "Guard submitted pet package bundle duplicates"
```

Expected: commit created.

## Self-Review

- Spec coverage: Closes a reward abuse gap by preventing re-import after a submitted draft.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Reuses existing `duplicate_import_draft` behavior and route mappings.
