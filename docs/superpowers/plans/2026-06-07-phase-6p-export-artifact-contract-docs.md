# Phase 6p Export Artifact Contract Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Freeze the export artifact naming contract in project docs so pet package, import summary, approved registry, feed metadata, and Android display fields do not drift while `fantasy-pet-rule` keeps evolving.

**Architecture:** Add a lightweight documentation contract test that reads the main ecosystem spec and checks for the exact export artifact fields used by code. Then update the spec's data contract examples and notes to document the intended mapping from manifest `assets.exportArtifact` to downstream `exportArtifactPath` fields.

**Tech Stack:** Node.js test runner, filesystem reads, Markdown project specification.

---

### Task 1: Documentation Contract Test

**Files:**
- Create: `packages/pet-package-spec/src/contract-doc.test.js`

- [ ] **Step 1: Write the failing documentation test**

Create a test that reads `docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md` and asserts it contains:

```js
'"exportArtifact": "exports/stardust-package.zip"'
"assets.exportArtifact"
"importSummary.assets.exportArtifactPath"
"metadata.exportArtifactPath"
"approvedPets.items[].assets.exportArtifactPath"
```

- [ ] **Step 2: Run the focused package tests to verify RED**

```powershell
npm.cmd test -- packages/pet-package-spec/src/contract-doc.test.js
```

Expected: FAIL because the spec still has the old manifest example and no downstream export artifact mapping notes.

### Task 2: Update Main Contract Spec

**Files:**
- Modify: `docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md`

- [ ] **Step 1: Add export artifact to Pet Package Manifest example**

In the `assets` object, add:

```json
"exportArtifact": "exports/stardust-package.zip",
```

between `previewImage` and `motionSheets`.

- [ ] **Step 2: Add downstream export artifact mapping notes**

After the Pet Package Manifest example, add a short `Export Artifact Field Mapping` subsection:

```markdown
### Export Artifact Field Mapping

- `manifest.assets.exportArtifact` is the package-relative archive path declared by uploaded `gamer.pet-package.v1` bundles.
- `importSummary.assets.exportArtifactPath` is the community import summary field produced from `manifest.assets.exportArtifact` or `fantasy-pet-rule` state `export.artifactPath`.
- `approvedPets.items[].assets.exportArtifactPath` is the approved pet registry field exposed by `/v1/pets/approved`.
- `feed.items[].metadata.exportArtifactPath` is the approved-import feed metadata field exposed by `/v1/feed`; Android renders it as `Package <path>` in feed audit labels.
```

- [ ] **Step 3: Run focused package tests to verify GREEN**

```powershell
npm.cmd test -- packages/pet-package-spec/src/contract-doc.test.js
```

Expected: PASS.

### Task 3: Full Verification and Commit

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
git add docs/superpowers/plans/2026-06-07-phase-6p-export-artifact-contract-docs.md docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md packages/pet-package-spec/src/contract-doc.test.js
git commit -m "Document export artifact contract mapping"
```

---

## Self-Review

- Spec coverage: Documents the full field path from uploaded package manifest to import summary, approved pet registry, feed metadata, and Android display.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: Manifest keeps `assets.exportArtifact`; downstream API shapes use `exportArtifactPath`.
