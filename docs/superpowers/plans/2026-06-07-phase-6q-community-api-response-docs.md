# Phase 6q Community API Response Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document `/v1/feed` and `/v1/pets/approved` response examples so client and admin work can depend on stable export artifact fields.

**Architecture:** Add a focused API Markdown document under `docs/api/community-api.md`. Protect the response examples with a lightweight documentation contract test in `packages/community-contracts`, checking endpoint headings and exact `exportArtifactPath` locations.

**Tech Stack:** Node.js test runner, filesystem reads, Markdown API documentation.

---

### Task 1: API Documentation Contract Test

**Files:**
- Create: `packages/community-contracts/src/api-doc.test.js`

- [ ] **Step 1: Write the failing API docs test**

Create a test that reads `docs/api/community-api.md` and asserts it contains:

```js
"GET /v1/feed"
"feed.items[].metadata.exportArtifactPath"
'"exportArtifactPath": "exports/stardust-package.zip"'
"GET /v1/pets/approved"
"approvedPets.items[].assets.exportArtifactPath"
'"assets": {'
```

- [ ] **Step 2: Run focused docs test to verify RED**

```powershell
npm.cmd test -- packages/community-contracts/src/api-doc.test.js
```

Expected: FAIL because `docs/api/community-api.md` does not exist yet.

### Task 2: Add Community API Response Examples

**Files:**
- Create: `docs/api/community-api.md`

- [ ] **Step 1: Create the API document**

Add a `Community API` document with sections for:

```markdown
# Community API

## GET /v1/feed

...

## GET /v1/pets/approved

...
```

- [ ] **Step 2: Document feed response metadata**

Include a response example where `items[0].metadata.exportArtifactPath` is:

```json
"exportArtifactPath": "exports/stardust-package.zip"
```

Also include the literal note:

```markdown
`feed.items[].metadata.exportArtifactPath`
```

- [ ] **Step 3: Document approved pets response assets**

Include a response example where `items[0].assets.exportArtifactPath` is:

```json
"exportArtifactPath": "exports/stardust-package.zip"
```

Also include the literal note:

```markdown
`approvedPets.items[].assets.exportArtifactPath`
```

- [ ] **Step 4: Run focused docs test to verify GREEN**

```powershell
npm.cmd test -- packages/community-contracts/src/api-doc.test.js
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
git add docs/superpowers/plans/2026-06-07-phase-6q-community-api-response-docs.md docs/api/community-api.md packages/community-contracts/src/api-doc.test.js
git commit -m "Document community API export artifact responses"
```

---

## Self-Review

- Spec coverage: Documents the public feed and approved pet registry response shapes that Android and admin users consume.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: Feed metadata and approved registry assets both expose `exportArtifactPath`; the docs use full path references for each response.
