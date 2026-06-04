# Phase 3a Pet Generator Readonly Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a read-only adapter in `pet-generator` that can summarize `fantasy-pet-rule` state and produce a community import summary without mutating upstream files.

**Architecture:** Keep the integration shallow while `fantasy-pet-rule` is still changing. The adapter only reads a small stable field set from either an inline `state` body or a local `statePath`, then returns `blocked`, `in-progress`, or `community-ready` plus an import-oriented summary.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, Docker Compose service already present.

---

### Files

- Modify: `services/pet-generator/src/adapter.js`
  - Keep `summarizeFantasyPetRuleState(state)`.
  - Add `createFantasyPetRuleImportSummary(state, options)`.
  - Extract only stable fields: `schema`, `petId`, `currentStage`, `baseIdentity.status`, `preview.userDecision`, `preview.urlOrPath`, `blockers`, `export.decision`, `export.status`, `export.artifactPath`.
- Create: `services/pet-generator/src/state-source.js`
  - Add `resolveFantasyPetRuleState(payload, options)` to accept inline `state` or read JSON from `statePath`.
  - Return a state object; throw typed request errors for missing, unreadable, or invalid JSON files.
- Modify: `services/pet-generator/src/server.js`
  - Export `createPetGeneratorHttpHandler(options = {})`.
  - Use shared JSON body parsing and explicit `invalid_json` responses.
  - Support `POST /v1/fantasy-pet-rule/summarize`.
  - Add `POST /v1/fantasy-pet-rule/import-summary`.
- Modify: `services/pet-generator/src/adapter.test.js`
  - Add readiness and import summary coverage.
- Create: `services/pet-generator/src/state-source.test.js`
  - Test inline state, readable `statePath`, invalid JSON, missing file, and empty request behavior.
- Create: `services/pet-generator/src/server.test.js`
  - Test HTTP `statePath` summarize, import summary, and invalid JSON behavior.

### Task 1: Adapter Behavior

- [ ] **Step 1: Write failing adapter tests**

Add tests in `services/pet-generator/src/adapter.test.js` for:

```js
test("import summary captures stable fantasy-pet-rule fields", () => {
  const result = createFantasyPetRuleImportSummary({
    schema: "fantasy-pet.codex-state.v1",
    petId: "demo-pet",
    currentStage: "preview-review",
    baseIdentity: { status: "accepted" },
    preview: {
      userDecision: "keep",
      urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
    },
    export: {
      decision: "asked",
      status: "ready",
      artifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
    },
    blockers: []
  });

  assert.equal(result.readiness.status, "community-ready");
  assert.equal(result.importSummary.source.schema, "fantasy-pet.codex-state.v1");
  assert.equal(result.importSummary.source.petId, "demo-pet");
  assert.equal(result.importSummary.assets.previewPath, "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html");
  assert.equal(result.importSummary.assets.exportArtifactPath, "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip");
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm.cmd test -- services/pet-generator/src/adapter.test.js`

Expected: FAIL because `createFantasyPetRuleImportSummary` is not exported.

- [ ] **Step 3: Implement minimal adapter**

Add the export in `services/pet-generator/src/adapter.js`. It must call `summarizeFantasyPetRuleState(state)` and return:

```js
{
  readiness,
  importSummary: {
    source: {
      schema,
      petId,
      currentStage,
      baseIdentityStatus
    },
    review: {
      blockers,
      previewDecision,
      exportDecision,
      exportStatus
    },
    assets: {
      previewPath,
      exportArtifactPath
    },
    notes: [
      "read-only summary; no fantasy-pet-rule files were mutated",
      "not a gamer.pet-package.v1 manifest"
    ]
  }
}
```

- [ ] **Step 4: Run adapter tests**

Run: `npm.cmd test -- services/pet-generator/src/adapter.test.js`

Expected: PASS.

### Task 2: State Source

- [ ] **Step 1: Write failing state source tests**

Create `services/pet-generator/src/state-source.test.js` with tests for inline `state`, temporary valid `statePath`, invalid JSON, missing file, and empty request.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm.cmd test -- services/pet-generator/src/state-source.test.js`

Expected: FAIL because `state-source.js` does not exist.

- [ ] **Step 3: Implement state source**

Create `services/pet-generator/src/state-source.js` with:

```js
export class StateSourceError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.name = "StateSourceError";
    this.code = code;
    this.status = status;
  }
}
```

and `resolveFantasyPetRuleState(payload, { readFile } = {})` that:

- returns `payload.state` when it is present;
- reads and parses `payload.statePath` when it is a non-empty string;
- throws `state_missing` when neither is present;
- throws `state_file_unreadable` for read failures;
- throws `state_file_invalid_json` for parse failures.

- [ ] **Step 4: Run state source tests**

Run: `npm.cmd test -- services/pet-generator/src/state-source.test.js`

Expected: PASS.

### Task 3: HTTP Routes

- [ ] **Step 1: Write failing server tests**

Create `services/pet-generator/src/server.test.js` that imports `createPetGeneratorHttpHandler`, spins up an ephemeral HTTP server, and verifies:

- `POST /v1/fantasy-pet-rule/summarize` accepts `statePath`;
- `POST /v1/fantasy-pet-rule/import-summary` returns readiness and import summary;
- invalid JSON returns `400 { error: "invalid_json" }`.

- [ ] **Step 2: Run test to verify it fails**

Run: `npm.cmd test -- services/pet-generator/src/server.test.js`

Expected: FAIL because `createPetGeneratorHttpHandler` is not exported and import route is missing.

- [ ] **Step 3: Refactor server**

Modify `services/pet-generator/src/server.js` to export `createPetGeneratorHttpHandler(options = {})`, preserve direct-run startup, and call:

```js
const state = await resolveFantasyPetRuleState(payload, options);
const summary = summarizeFantasyPetRuleState(state);
const result = createFantasyPetRuleImportSummary(state);
```

Handle `StateSourceError` with its status and `error` code.

- [ ] **Step 4: Run server tests**

Run: `npm.cmd test -- services/pet-generator/src/server.test.js`

Expected: PASS.

### Task 4: Verification and Commit

- [ ] **Step 1: Run all Node tests**

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
git add docs/superpowers/plans/2026-06-04-phase-3a-pet-generator-readonly-adapter.md services/pet-generator/src
git commit -m "Add readonly fantasy pet rule adapter"
```

Expected: new commit created.

### Self-Review

- Spec coverage: Covers read-only `state.json` ingestion, readiness judgment, import summary, HTTP routes, tests, and Docker config check.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Functions and error codes are named consistently across tasks.
