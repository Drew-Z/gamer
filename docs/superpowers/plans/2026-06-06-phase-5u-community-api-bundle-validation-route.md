# Phase 5u Community API Bundle Validation Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose a Community API route that validates complete pet package bundles before submission flows consume them.

**Architecture:** Keep validation logic in `packages/pet-package-spec` and call it from a narrow `community-api` route. The route should be stateless: valid bundles return the validation result with HTTP 200, invalid bundles return HTTP 400 with a stable error code and the validator errors.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, existing in-process route handler tests.

---

## Files

- Modify `services/community-api/src/routes.test.js`: add RED route tests for valid and invalid bundle validation.
- Modify `services/community-api/src/routes.js`: import `validatePetPackageBundle()` and add `POST /v1/pet-package-bundles/validate`.

## Task 1: Bundle Validation Route

- [x] **Step 1: Write failing route tests**

Add imports:

```js
import { validPetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
```

Add tests:

```js
test("pet package bundle validation route accepts valid bundle", () => {
  const response = handleCommunityRequest(
    "POST",
    "/v1/pet-package-bundles/validate",
    {
      body: {
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(response.status, 200);
  assert.deepEqual(response.body.validation, {
    ok: true,
    errors: []
  });
});

test("pet package bundle validation route rejects invalid bundle", () => {
  const response = handleCommunityRequest(
    "POST",
    "/v1/pet-package-bundles/validate",
    {
      body: {
        bundle: {
          ...validPetPackageBundle,
          scoreReport: {
            ...validPetPackageBundle.scoreReport,
            petId: "pet-other-route-001"
          }
        }
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_pet_package_bundle");
  assert.equal(response.body.validation.ok, false);
  assert.ok(
    response.body.validation.errors.includes("manifest.petId must match scoreReport.petId")
  );
});
```

- [x] **Step 2: Run route tests to verify RED**

Run:

```powershell
node --test services/community-api/src/routes.test.js
```

Expected: FAIL because the route does not exist yet and returns 404.

- [x] **Step 3: Implement route**

In `services/community-api/src/routes.js`, import:

```js
import { validatePetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
```

Add before the submission routes:

```js
if (method === "POST" && url.pathname === "/v1/pet-package-bundles/validate") {
  const validation = validatePetPackageBundle(body.bundle);

  if (!validation.ok) {
    return json(400, {
      error: "invalid_pet_package_bundle",
      validation
    });
  }

  return json(200, {
    validation
  });
}
```

- [x] **Step 4: Run route tests to verify GREEN**

Run the route test command again.

Expected: PASS.

## Task 2: Verification

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
git add docs/superpowers/plans/2026-06-06-phase-5u-community-api-bundle-validation-route.md services/community-api/src/routes.js services/community-api/src/routes.test.js
git commit -m "Add community bundle validation route"
```

Expected: commit created.

## Self-Review

- Spec coverage: Adds a backend validation boundary for complete package evidence before upload/submission flows.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Route body uses `bundle`, matching `validatePetPackageBundle(bundle)`.
