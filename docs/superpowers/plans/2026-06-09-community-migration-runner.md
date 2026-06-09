# Community Migration Runner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real migration runner for the community API so Boxd can initialize Aiven PostgreSQL through `DATABASE_URL` without switching runtime reads/writes away from the in-memory store.

**Architecture:** Keep migration execution separate from HTTP serving. A pure runner accepts an injected query client for tests, bootstraps `schema_migrations`, applies pending SQL files in transactions, and supports dry-run. A CLI creates a `pg` Client from existing database config and calls the runner.

**Tech Stack:** Node.js `node:test`, node-postgres `pg`, PostgreSQL SQL migrations, Docker.

---

### Task 1: Pure Migration Runner

**Files:**
- Create: `services/community-api/src/database/runner.test.js`
- Create: `services/community-api/src/database/runner.js`

- [ ] **Step 1: Write RED tests**

Test that the runner bootstraps `schema_migrations`, skips already-applied migrations, applies pending migrations inside `BEGIN`/`COMMIT`, records applied ids, supports dry-run without applying SQL, and rolls back on failure.

- [ ] **Step 2: Run tests**

Run: `node --test services/community-api/src/database/runner.test.js`
Expected: FAIL because `runner.js` does not exist.

- [ ] **Step 3: Implement runner**

Implement `runCommunityMigrations({ client, migrations, dryRun })` and `schemaMigrationsBootstrapSql`.

- [ ] **Step 4: Run tests**

Run: `node --test services/community-api/src/database/runner.test.js`
Expected: PASS.

### Task 2: CLI and Dependency

**Files:**
- Create: `services/community-api/src/database/migrate.js`
- Create: `services/community-api/src/database/migrate.test.js`
- Modify: `services/community-api/package.json`
- Modify: `services/community-api/Dockerfile`

- [ ] **Step 1: Write RED tests**

Test CLI behavior with injected client factory: blank `DATABASE_URL` fails safely, `--dry-run` reports pending migrations, and a normal run closes the client.

- [ ] **Step 2: Run tests**

Run: `node --test services/community-api/src/database/migrate.test.js`
Expected: FAIL because `migrate.js` does not exist.

- [ ] **Step 3: Implement CLI**

Implement `runMigrationCli()` and direct-run entrypoint. Use dynamic `import("pg")` only inside the default client factory.

- [ ] **Step 4: Add dependency and Docker install**

Add `pg` to `@gamer/community-api` dependencies. Update Dockerfile to run `npm install --omit=dev --ignore-scripts --workspace @gamer/community-api`.

- [ ] **Step 5: Run tests**

Run: `node --test services/community-api/src/database/migrate.test.js services/community-api/src/database/runner.test.js`
Expected: PASS.

### Task 3: Docs and Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/deployment/boxd-cloudflare-aiven.md`

- [ ] **Step 1: Document commands**

Document dry-run and apply commands using `node services/community-api/src/database/migrate.js --dry-run` and `node services/community-api/src/database/migrate.js`.

- [ ] **Step 2: Verify**

Run:

```powershell
node --test services/community-api/src/database/*.test.js
npm.cmd test
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production.example config
git diff --check
```

Expected: all pass.
