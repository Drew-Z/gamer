# Phase 6s Approved Pet Package Descriptor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a stable Community API descriptor endpoint for the package artifact behind one approved pet.

**Architecture:** Extend the in-memory community store with a read-only approved pet package descriptor lookup. Add a route at `GET /v1/pets/approved/:petId/package` that returns package artifact metadata for an approved pet or a 404 error for unknown pet IDs. Document the new response shape in the Community API docs.

**Tech Stack:** Node.js test runner, in-memory Community API store, Markdown API documentation.

---

### Task 1: Route-Level RED Test

**Files:**
- Modify: `services/community-api/src/routes.test.js`

- [ ] **Step 1: Write the failing route test**

Add a test named `approved pet package route returns export artifact descriptor`.

The test should:

```js
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

const response = handleCommunityRequest(
  "GET",
  "/v1/pets/approved/pet-stardust-001/package",
  { store }
);

assert.equal(response.status, 200);
assert.equal(response.body.petId, "pet-stardust-001");
assert.equal(response.body.displayName, "Stardust Dragon");
assert.equal(response.body.package.exportArtifactPath, "exports/stardust-package.zip");
assert.equal(response.body.package.status, "available");
assert.equal(response.body.submissionId, submitted.body.submission.id);
```

- [ ] **Step 2: Add the missing route test**

Add a test named `approved pet package route returns 404 for unknown pet`.

```js
const response = handleCommunityRequest(
  "GET",
  "/v1/pets/approved/pet-missing-001/package",
  {
    store: createCommunityStore()
  }
);

assert.equal(response.status, 404);
assert.equal(response.body.error, "approved_pet_package_not_found");
assert.equal(response.body.petId, "pet-missing-001");
```

- [ ] **Step 3: Run focused route tests to verify RED**

```powershell
npm.cmd test -- services/community-api/src/routes.test.js
```

Expected: FAIL because the new route currently returns `not_found`.

### Task 2: Documentation RED Test

**Files:**
- Modify: `packages/community-contracts/src/api-doc.test.js`

- [ ] **Step 1: Extend the API docs test**

Add assertions that `docs/api/community-api.md` contains:

```js
"GET /v1/pets/approved/:petId/package"
"approvedPetPackage.package.exportArtifactPath"
"approved_pet_package_not_found"
```

- [ ] **Step 2: Run focused docs test to verify RED**

```powershell
npm.cmd test -- packages/community-contracts/src/api-doc.test.js
```

Expected: FAIL because the docs do not describe the package descriptor route yet.

### Task 3: Store and Route Implementation

**Files:**
- Modify: `services/community-api/src/store.js`
- Modify: `services/community-api/src/routes.js`

- [ ] **Step 1: Add a store descriptor lookup**

Add this method inside the object returned by `createCommunityStore()`:

```js
getApprovedPetPackage(petId) {
  const pet = state.approvedPets.find((item) => item.petId === petId);

  if (!pet) {
    return null;
  }

  const exportArtifactPath = pet.assets?.exportArtifactPath ?? "";

  return {
    petId: pet.petId,
    displayName: pet.displayName,
    ownerUserId: pet.ownerUserId,
    package: {
      exportArtifactPath,
      status: exportArtifactPath ? "available" : "missing"
    },
    assets: {
      previewPath: pet.assets?.previewPath ?? "",
      motionSheetCount: Number(pet.assets?.motionSheetCount ?? 0)
    },
    source: clone(pet.source ?? {}),
    submissionId: pet.submissionId,
    importDraftId: pet.importDraftId,
    scoreReportId: pet.scoreReportId
  };
}
```

- [ ] **Step 2: Add the route**

In `handleCommunityRequest`, before the exact `/v1/wallet/me` route, add:

```js
if (
  method === "GET" &&
  url.pathname.startsWith("/v1/pets/approved/") &&
  url.pathname.endsWith("/package")
) {
  const petId = decodeURIComponent(
    url.pathname.slice("/v1/pets/approved/".length, -"/package".length)
  );
  const descriptor = store.getApprovedPetPackage(petId);

  if (!descriptor) {
    return json(404, {
      error: "approved_pet_package_not_found",
      petId
    });
  }

  return json(200, descriptor);
}
```

- [ ] **Step 3: Run focused route tests to verify GREEN**

```powershell
npm.cmd test -- services/community-api/src/routes.test.js
```

Expected: PASS.

### Task 4: API Documentation Implementation

**Files:**
- Modify: `docs/api/community-api.md`

- [ ] **Step 1: Add package descriptor docs**

Add a section after `GET /v1/pets/approved`:

````markdown
## GET /v1/pets/approved/:petId/package

Returns the package descriptor for one approved pet. This endpoint does not
stream or copy the package archive yet; it exposes the approved export artifact
path and trace IDs that future download or import flows can use.

`approvedPetPackage.package.exportArtifactPath` is the package archive path
registered during review approval.

Example response:

```json
{
  "petId": "pet-stardust-001",
  "displayName": "Stardust Dragon",
  "ownerUserId": "user-demo-001",
  "package": {
    "exportArtifactPath": "exports/stardust-package.zip",
    "status": "available"
  },
  "assets": {
    "previewPath": "previews/overall-showcase.png",
    "motionSheetCount": 2
  },
  "source": {
    "kind": "fantasy-pet-rule",
    "runId": "stardust-chinese-dragon-codex-02",
    "statePath": "D:/workspace4Codex/fantasy-pet-rule/runs/stardust-chinese-dragon-codex-02/state.json"
  },
  "submissionId": "submission-local-001",
  "importDraftId": "import-draft-local-001",
  "scoreReportId": "score-import-draft-local-001"
}
```

Unknown approved pets return:

```json
{
  "error": "approved_pet_package_not_found",
  "petId": "pet-missing-001"
}
```
````

- [ ] **Step 2: Run focused docs test to verify GREEN**

```powershell
npm.cmd test -- packages/community-contracts/src/api-doc.test.js
```

Expected: PASS.

### Task 5: Full Verification and Commit

**Files:**
- Verify all files above

- [ ] **Step 1: Run Node tests**

```powershell
npm.cmd test
```

- [ ] **Step 2: Run Android tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [ ] **Step 3: Validate Docker compose**

```powershell
docker compose config
```

- [ ] **Step 4: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 5: Commit**

```powershell
git add docs/superpowers/plans/2026-06-08-phase-6s-approved-pet-package-descriptor.md docs/api/community-api.md packages/community-contracts/src/api-doc.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js services/community-api/src/store.js
git commit -m "Add approved pet package descriptor route"
```

---

## Self-Review

- Spec coverage: Supports the project goal of approved pet package import and preview by exposing a stable per-pet package descriptor after review approval.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: The endpoint uses `exportArtifactPath`, matching approved registry assets and feed metadata naming.
