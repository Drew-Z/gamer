# Phase 5w Pet Package Bundle Owner Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent users from creating import drafts from pet package bundles owned by another user.

**Architecture:** Keep bundle-internal schema validation in `packages/pet-package-spec`, and enforce request-user ownership in `community-api`. The store method should return a stable `bundle_owner_mismatch` error without mutating import drafts, and HTTP routes should map that result to HTTP 403.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory `community-api` store, route handler, and HTTP handler tests.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED store test for owner mismatch.
- Modify `services/community-api/src/store.js`: guard `createImportDraftFromPetPackageBundle()`.
- Modify `services/community-api/src/routes.test.js`: add RED route test for owner mismatch.
- Modify `services/community-api/src/routes.js`: map store owner mismatch to HTTP 403.
- Modify `services/community-api/src/server.test.js`: add HTTP coverage for the owner guard.

## Task 1: Store Owner Guard

- [x] **Step 1: Write failing store test**

Add:

```js
test("pet package bundle owned by another user does not create import draft", () => {
  const store = createCommunityStore();
  const result = store.createImportDraftFromPetPackageBundle({
    userId: "user-other-001",
    bundle: validPetPackageBundle
  });

  assert.equal(result.error, "bundle_owner_mismatch");
  assert.equal(result.userId, "user-other-001");
  assert.equal(result.ownerUserId, "user-demo-001");
  assert.equal(store.listImportDrafts("user-other-001").drafts.length, 0);
});
```

- [x] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because the store currently creates a draft even when `input.userId` differs from `bundle.manifest.ownerUserId`.

- [x] **Step 3: Implement store guard**

At the start of `createImportDraftFromPetPackageBundle(input)` add:

```js
const ownerUserId = input.bundle.manifest.ownerUserId;
if (input.userId !== ownerUserId) {
  return {
    error: "bundle_owner_mismatch",
    userId: input.userId,
    ownerUserId
  };
}
```

- [x] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Route Owner Guard

- [x] **Step 1: Write failing route test**

Add:

```js
test("pet package bundle route rejects bundle owned by another user", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        userId: "user-other-001",
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(response.status, 403);
  assert.equal(response.body.error, "bundle_owner_mismatch");
  assert.equal(response.body.ownerUserId, "user-demo-001");
  assert.equal(store.listImportDrafts("user-other-001").drafts.length, 0);
});
```

- [x] **Step 2: Run route test to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because the route currently returns 201 for this mismatch.

- [x] **Step 3: Implement route mapping**

After `store.createImportDraftFromPetPackageBundle(...)`, add:

```js
if (draft.error === "bundle_owner_mismatch") {
  return json(403, draft);
}
```

- [x] **Step 4: Run route test to verify GREEN**

Run the route test command again.

Expected: PASS.

## Task 3: HTTP Owner Guard

- [x] **Step 1: Write HTTP test**

Import:

```js
import { validPetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
```

Add:

```js
test("HTTP server rejects pet package bundle owned by another user", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        userId: "user-other-001",
        bundle: validPetPackageBundle
      }
    );

    assert.equal(response.status, 403);
    assert.equal(response.body.error, "bundle_owner_mismatch");
    assert.equal(response.body.ownerUserId, "user-demo-001");
    assert.equal(store.listImportDrafts("user-other-001").drafts.length, 0);
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
git add docs/superpowers/plans/2026-06-06-phase-5w-pet-package-bundle-owner-guard.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js services/community-api/src/server.test.js
git commit -m "Guard pet package bundle owners"
```

Expected: commit created.

## Self-Review

- Spec coverage: Strengthens ownership and reward integrity before package bundles enter the submission flow.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `validPetPackageBundle`, `createImportDraftFromPetPackageBundle()`, and `bundle_owner_mismatch`.
