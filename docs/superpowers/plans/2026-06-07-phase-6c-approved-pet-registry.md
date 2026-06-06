# Phase 6c Approved Pet Registry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Register approved imported pets as queryable community pet assets.

**Architecture:** Keep registry state inside the current in-memory community store. Approval of an import submission creates one registry record keyed by `petId`; revocation removes it. A public API route exposes the approved pet list for future Android detail pages and showcase systems.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, current community API in-memory store, existing Docker Compose services.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED coverage for approved pet registry creation and revocation.
- Modify `services/community-api/src/store.js`: add `approvedPets` state, registry record creation, registry list method, and revocation removal.
- Modify `services/community-api/src/routes.test.js`: add RED route flow coverage for `GET /v1/pets/approved`.
- Modify `services/community-api/src/routes.js`: expose `GET /v1/pets/approved`.
- Modify `services/community-api/src/server.test.js`: add HTTP coverage for the approved pet registry route.

## Task 1: Store Registry

- [x] **Step 1: Write failing store test**

Add this test to `services/community-api/src/store.test.js`:

```js
test("approved imported submission registers approved pet asset", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const submitted = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submitted.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const registry = store.listApprovedPets();
  const pet = registry.items.find((item) => item.petId === "pet-stardust-001");

  assert.equal(pet.displayName, "Stardust Dragon");
  assert.equal(pet.ownerUserId, "user-demo-001");
  assert.equal(pet.source.kind, "fantasy-pet-rule");
  assert.equal(pet.assets.previewPath, "previews/overall-showcase.png");
  assert.equal(pet.assets.motionSheetCount, 2);
  assert.equal(pet.submissionId, submitted.submission.id);
  assert.equal(pet.importDraftId, draft.id);
});
```

- [x] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because `store.listApprovedPets` does not exist.

- [x] **Step 3: Implement store registry**

In `services/community-api/src/store.js`:

1. Add `approvedPets: []` to `defaultSeed`.
2. Add helper:

```js
const createApprovedPetFromImport = (submission, draft, scoreReport) => ({
  petId: submission.petId,
  displayName: draft?.importSummary?.source?.displayName ?? submission.petId,
  ownerUserId: submission.userId,
  source: {
    kind: draft?.importSummary?.source?.kind ?? "",
    runId: draft?.importSummary?.source?.runId ?? "",
    statePath: draft?.importSummary?.source?.statePath ?? ""
  },
  assets: {
    previewPath: draft?.importSummary?.assets?.previewPath ?? "",
    motionSheetCount: Array.isArray(draft?.importSummary?.assets?.motionSheets)
      ? draft.importSummary.assets.motionSheets.length
      : 0
  },
  submissionId: submission.id,
  importDraftId: submission.importDraftId,
  scoreReportId: submission.scoreReportId,
  totalScore: Number(scoreReport?.totalScore ?? 0),
  approvedAt: nowIso()
});
```

3. In `createImportSummaryFromPetPackageBundle()`, include `displayName: bundle.manifest.displayName`.
4. Add `listApprovedPets()` returning `{ items: clone(state.approvedPets) }`.
5. On approval of an import submission, upsert one approved pet record if absent.
6. On revocation of an import submission, remove the matching approved pet record.

- [x] **Step 4: Run store test to verify GREEN**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: PASS.

## Task 2: API Route

- [x] **Step 1: Write failing route test**

Add this test to `services/community-api/src/routes.test.js`:

```js
test("approved pets route returns registered imported pet assets", () => {
  const store = createCommunityStore();
  const draft = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
  const submitted = handleCommunityRequest("POST", "/v1/import-drafts/submit", {
    store,
    body: {
      draftId: draft.body.id
    }
  });
  handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: submitted.body.submission.id,
      status: "approved",
      reviewer: "admin-demo"
    }
  });

  const response = handleCommunityRequest("GET", "/v1/pets/approved", { store });

  assert.equal(response.status, 200);
  assert.equal(response.body.items.length, 1);
  assert.equal(response.body.items[0].petId, "pet-stardust-001");
  assert.equal(response.body.items[0].displayName, "Stardust Dragon");
});
```

- [x] **Step 2: Run routes test to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because the route returns 404.

- [x] **Step 3: Implement route**

In `services/community-api/src/routes.js`, add before `/v1/feed` or near public GET routes:

```js
if (method === "GET" && url.pathname === "/v1/pets/approved") {
  return json(200, store.listApprovedPets());
}
```

- [x] **Step 4: Run routes test to verify GREEN**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: PASS.

## Task 3: HTTP Coverage

- [x] **Step 1: Write failing HTTP test**

Add a test to `services/community-api/src/server.test.js` that creates, submits, approves a bundle import, then requests `GET /v1/pets/approved` and asserts:

```js
assert.equal(response.status, 200);
assert.equal(response.body.items[0].petId, "pet-stardust-001");
assert.equal(response.body.items[0].assets.motionSheetCount, 2);
```

- [x] **Step 2: Run HTTP server test**

Run:

```powershell
node --test services/community-api/src/server.test.js
```

Expected: PASS if route wiring is complete.

## Task 4: Verification

- [x] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

- [x] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [x] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

- [x] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

- [x] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6c-approved-pet-registry.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js services/community-api/src/server.test.js
git commit -m "Register approved imported pets"
```

## Self-Review

- Spec coverage: Adds a queryable registry for approved pet package imports, supporting community display beyond feed posts.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Registry fields derive from existing import draft, submission, and score report names.
