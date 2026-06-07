# Phase 6r README Contract Doc Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the workspace README expose the key community contract documents and verification commands for a new or restarted worker.

**Architecture:** Add a small README documentation contract test under `packages/community-contracts`. Update the root README with a `Docs` section that links the community API contract and ecosystem spec, and with a `Verification` section that lists the standard Node, Android, Docker, and whitespace checks.

**Tech Stack:** Node.js test runner, filesystem reads, Markdown documentation.

---

### Task 1: README Documentation Contract Test

**Files:**
- Create: `packages/community-contracts/src/readme-doc-links.test.js`

- [ ] **Step 1: Write the failing README docs test**

Create a test that reads `README.md` and asserts it contains:

```js
"docs/api/community-api.md"
"docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md"
"npm.cmd test"
"testDebugUnitTest --console=plain"
"docker compose config"
"git diff --check"
```

- [ ] **Step 2: Run focused test to verify RED**

```powershell
npm.cmd test -- packages/community-contracts/src/readme-doc-links.test.js
```

Expected: FAIL because the README does not yet expose all required links and verification commands.

### Task 2: Add README Docs and Verification Sections

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add the Docs section**

Add this section after the structure block:

```markdown
## Docs

- Community API contract: `docs/api/community-api.md`
- Ecosystem design spec: `docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md`
```

- [ ] **Step 2: Add the Verification section**

Add this section near the local test instructions:

````markdown
## Verification

Run the standard verification set before committing a phase:

```powershell
npm.cmd test
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
docker compose config
git diff --check
```
````

- [ ] **Step 3: Run focused test to verify GREEN**

```powershell
npm.cmd test -- packages/community-contracts/src/readme-doc-links.test.js
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
git add README.md docs/superpowers/plans/2026-06-08-phase-6r-readme-contract-doc-links.md packages/community-contracts/src/readme-doc-links.test.js
git commit -m "Document project contract entry points"
```

---

## Self-Review

- Spec coverage: New workers can find the Community API response contract and the main desktop-pet community ecosystem spec from the root README.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: The test strings match the README links and verification commands exactly.
