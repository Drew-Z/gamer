# Phase 5v Import Draft From Pet Package Bundle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Community API create a ready import draft from a validated pet package bundle.

**Architecture:** Keep bundle schema validation in `packages/pet-package-spec`, then add a `community-api` store method that converts a validated bundle into an import draft. The store method must preserve the uploaded bundle score report instead of replacing it with the local generated scoring heuristic.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-memory `community-api` store and route handler.

---

## Files

- Modify `services/community-api/src/store.test.js`: add RED test for creating an import draft from a valid pet package bundle.
- Modify `services/community-api/src/store.js`: add score report preservation support and `createImportDraftFromPetPackageBundle()`.
- Modify `services/community-api/src/routes.test.js`: add RED tests for the route.
- Modify `services/community-api/src/routes.js`: add `POST /v1/import-drafts/from-pet-package-bundle`.

## Task 1: Store Bundle Import Draft

- [x] **Step 1: Write failing store test**

Import:

```js
import { validPetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
```

Add:

```js
test("pet package bundle creates ready import draft with uploaded score report", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const report = store.getScoreReport(draft.scoreReportId);

  assert.equal(draft.status, "ready");
  assert.equal(draft.petId, "pet-stardust-001");
  assert.equal(draft.ownershipClaimId, "claim-pet-stardust-001");
  assert.equal(draft.importSummary.source.kind, "fantasy-pet-rule");
  assert.equal(draft.importSummary.assets.previewPath, "previews/overall-showcase.png");
  assert.deepEqual(draft.importSummary.assets.motionSheets, [
    "motion/sheets/idle.png",
    "motion/sheets/happy_click.png"
  ]);
  assert.equal(report.reportId, draft.scoreReportId);
  assert.equal(report.petId, "pet-stardust-001");
  assert.equal(report.totalScore, validPetPackageBundle.scoreReport.totalScore);
  assert.equal(
    report.rewardRecommendation.amount,
    validPetPackageBundle.scoreReport.rewardRecommendation.amount
  );
});
```

- [x] **Step 2: Run store test to verify RED**

Run:

```powershell
node --test services/community-api/src/store.test.js
```

Expected: FAIL because `createImportDraftFromPetPackageBundle` does not exist.

- [x] **Step 3: Implement store support**

Add helper:

```js
const createImportSummaryFromPetPackageBundle = (bundle) => ({
  source: {
    petId: bundle.manifest.petId,
    schema: bundle.manifest.schema,
    kind: bundle.manifest.source.kind,
    runId: bundle.manifest.source.runId,
    statePath: bundle.manifest.source.statePath
  },
  review: {
    blockers: [],
    previewDecision: "keep",
    exportStatus: "ready"
  },
  assets: {
    baseImage: bundle.manifest.assets.baseImage,
    previewPath: bundle.manifest.assets.previewImage,
    motionSheets: clone(bundle.manifest.assets.motionSheets)
  }
});
```

In `createImportDraft(input)`, before generated scoring:

```js
if (input.scoreReport) {
  const scoreReport = {
    ...clone(input.scoreReport),
    reportId: input.scoreReport.reportId ?? `score-${draft.id}`
  };
  state.scoreReports.push(scoreReport);
  draft.scoreReportId = scoreReport.reportId;
} else if (!draft.scoreReportId) {
  const scoreReport = createScoreReportFromImportDraft(draft);
  state.scoreReports.push(scoreReport);
  draft.scoreReportId = scoreReport.reportId;
}
```

Add store method:

```js
createImportDraftFromPetPackageBundle(input) {
  return this.createImportDraft({
    userId: input.userId,
    readiness: {
      status: "community-ready",
      reason: "validated pet package bundle"
    },
    importSummary: createImportSummaryFromPetPackageBundle(input.bundle),
    ownershipClaimId: input.bundle.ownershipClaim.claimId,
    scoreReport: input.bundle.scoreReport
  });
},
```

- [x] **Step 4: Run store test to verify GREEN**

Run the store test command again.

Expected: PASS.

## Task 2: Route Bundle Import Draft

- [x] **Step 1: Write failing route tests**

Add:

```js
test("pet package bundle route creates ready import draft", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
  const report = store.getScoreReport(response.body.scoreReportId);

  assert.equal(response.status, 201);
  assert.equal(response.body.status, "ready");
  assert.equal(response.body.petId, "pet-stardust-001");
  assert.equal(response.body.ownershipClaimId, "claim-pet-stardust-001");
  assert.equal(report.totalScore, validPetPackageBundle.scoreReport.totalScore);
});

test("pet package bundle route rejects invalid import draft bundle", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: {
          ...validPetPackageBundle,
          ownershipClaim: {
            ...validPetPackageBundle.ownershipClaim,
            petId: "pet-other-route-002"
          }
        }
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_pet_package_bundle");
  assert.equal(response.body.validation.ok, false);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});
```

- [x] **Step 2: Run route tests to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because the route does not exist.

- [x] **Step 3: Implement route**

Add before existing import draft routes:

```js
if (method === "POST" && url.pathname === "/v1/import-drafts/from-pet-package-bundle") {
  const validation = validatePetPackageBundle(body.bundle);

  if (!validation.ok) {
    return json(400, {
      error: "invalid_pet_package_bundle",
      validation
    });
  }

  const draft = store.createImportDraftFromPetPackageBundle({
    userId: currentUserId,
    bundle: body.bundle
  });

  return json(201, draft);
}
```

- [x] **Step 4: Run route tests to verify GREEN**

Run the route test command again.

Expected: PASS.

## Task 3: Verification

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
git add docs/superpowers/plans/2026-06-06-phase-5v-import-draft-from-pet-package-bundle.md services/community-api/src/store.js services/community-api/src/store.test.js services/community-api/src/routes.js services/community-api/src/routes.test.js
git commit -m "Create import drafts from pet package bundles"
```

Expected: commit created.

## Self-Review

- Spec coverage: Connects validated package evidence into the existing import draft, submit, review, reward, and feed flow.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses existing `validPetPackageBundle`, `validatePetPackageBundle()`, and import draft fields.
