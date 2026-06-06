# Phase 5x Duplicate Pet Package Bundle Import Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent duplicate ready import drafts from the same pet package bundle for the same user.

**Architecture:** Keep duplicate detection in `community-api` store because it owns draft state. The route should map the store's stable duplicate error to HTTP 409 so API clients can distinguish duplicate draft attempts from validation or owner failures.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory `community-api` store, route handler, and HTTP handler tests.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED test for duplicate pet package bundle draft creation.
- Modify `services/community-api/src/store.js`: add duplicate guard in `createImportDraftFromPetPackageBundle()`.
- Modify `services/community-api/src/routes.test.js`: add route test for duplicate mapping.
- Modify `services/community-api/src/routes.js`: map duplicate result to HTTP 409.
- Modify `services/community-api/src/server.test.js`: add HTTP coverage for duplicate mapping.

## Task 1: Store Duplicate Guard

- [x] **Step 1: Write failing store test**

Add:

```js
test("pet package bundle cannot create duplicate active import draft", () => {
  const store = createCommunityStore();
  const first = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const second = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });

  assert.equal(first.status, "ready");
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

Expected: FAIL because the store currently creates two ready drafts for the same bundle.

- [x] **Step 3: Implement store duplicate guard**

At the start of `createImportDraftFromPetPackageBundle(input)`, after owner guard, add:

```js
const existingDraft = state.importDrafts.find(
  (draft) =>
    draft.userId === input.userId &&
    draft.petId === input.bundle.manifest.petId &&
    draft.status !== "submitted"
);
if (existingDraft) {
  return {
    error: "duplicate_import_draft",
    petId: input.bundle.manifest.petId,
    existingDraftId: existingDraft.id
  };
}
```

- [x] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Route Duplicate Mapping

- [x] **Step 1: Write failing route test**

Add:

```js
test("pet package bundle route rejects duplicate active import draft", () => {
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

- [x] **Step 2: Run route test to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because the route currently returns 201 for duplicate bundle draft attempts.

- [x] **Step 3: Implement route mapping**

After owner mismatch mapping, add:

```js
if (draft.error === "duplicate_import_draft") {
  return json(409, draft);
}
```

- [x] **Step 4: Run route test to verify GREEN**

Run the route test command again.

Expected: PASS.

## Task 3: HTTP Duplicate Coverage

- [x] **Step 1: Write HTTP test**

Add:

```js
test("HTTP server rejects duplicate pet package bundle import draft", async () => {
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

Expected: PASS after route mapping is implemented.

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
git add docs/superpowers/plans/2026-06-06-phase-5x-duplicate-pet-package-bundle-import-guard.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js services/community-api/src/server.test.js
git commit -m "Guard duplicate pet package bundle imports"
```

Expected: commit created.

## Self-Review

- Spec coverage: Reduces duplicate submission/reward abuse by preventing repeated active drafts for the same package.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `validPetPackageBundle`, `createImportDraftFromPetPackageBundle()`, and route paths.
