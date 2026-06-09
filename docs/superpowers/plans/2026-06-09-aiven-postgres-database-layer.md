# Aiven Postgres Database Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first production database layer for Gamer by defining the PostgreSQL schema, migration loader, and Aiven connection configuration without switching runtime traffic away from the current in-memory store.

**Architecture:** Keep `community-api` behavior unchanged while adding SQL migrations under `services/community-api/db/migrations`. Add small JavaScript helpers that can load and validate migrations and parse database environment variables. Tests lock the table coverage and managed-service boundaries before later work adds a real Postgres-backed store.

**Tech Stack:** Node.js `node:test`, PostgreSQL SQL migrations, Aiven PostgreSQL environment configuration.

---

### Task 1: Migration Contract

**Files:**
- Create: `services/community-api/src/database/migrations.test.js`
- Create: `services/community-api/src/database/migrations.js`
- Create: `services/community-api/db/migrations/001_initial_community_schema.sql`

- [ ] **Step 1: Write the failing test**

```js
import assert from "node:assert/strict";
import test from "node:test";
import {
  listCommunityMigrations,
  readInitialCommunitySchema,
  requiredCommunityTables
} from "./migrations.js";

test("initial community schema covers public app and admin review data", () => {
  const sql = readInitialCommunitySchema();

  for (const tableName of requiredCommunityTables) {
    assert.match(sql, new RegExp(`create table if not exists ${tableName}\\b`, "i"));
  }

  assert.match(sql, /submissions_status_check/i);
  assert.match(sql, /pending/i);
  assert.match(sql, /approved/i);
  assert.match(sql, /revoked/i);
  assert.match(sql, /r2_bucket/i);
  assert.match(sql, /r2_key/i);
  assert.match(sql, /download_id/i);
  assert.doesNotMatch(sql, /file:\/\//i);
  assert.doesNotMatch(sql, /server-worker-cycle|agent-outputs|targetoutput/i);
});

test("migration loader returns ordered migrations with stable ids", () => {
  const migrations = listCommunityMigrations();

  assert.ok(migrations.length >= 1);
  assert.equal(migrations[0].id, "001_initial_community_schema");
  assert.equal(migrations[0].filename, "001_initial_community_schema.sql");
  assert.ok(migrations[0].sql.includes("create table if not exists users"));
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test services/community-api/src/database/migrations.test.js`
Expected: FAIL because `migrations.js` and SQL migrations do not exist.

- [ ] **Step 3: Implement minimal migration loader and SQL**

Create `migrations.js` with `requiredCommunityTables`, `listCommunityMigrations()`, and `readInitialCommunitySchema()`. Create `001_initial_community_schema.sql` with tables for users, wallet ledger, daily check-ins, feed posts, import drafts, score reports, submissions, review decisions, approved pets, asset objects, and post reactions.

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test services/community-api/src/database/migrations.test.js`
Expected: PASS.

### Task 2: Aiven Configuration Parser

**Files:**
- Create: `services/community-api/src/database/config.test.js`
- Create: `services/community-api/src/database/config.js`

- [ ] **Step 1: Write the failing test**

```js
import assert from "node:assert/strict";
import test from "node:test";
import { createDatabaseConfig } from "./config.js";

test("database config stays in memory mode when DATABASE_URL is blank", () => {
  assert.deepEqual(createDatabaseConfig({ DATABASE_URL: "" }), {
    mode: "memory",
    databaseUrl: "",
    sslMode: "",
    caCertPath: ""
  });
});

test("database config accepts Aiven PostgreSQL URLs with SSL metadata", () => {
  const config = createDatabaseConfig({
    DATABASE_URL: "postgres://avnadmin:example@example.aivencloud.com:13040/pgbouncer?sslmode=require",
    AIVEN_CA_CERT_PATH: "/run/secrets/aiven-ca.pem"
  });

  assert.equal(config.mode, "postgres");
  assert.equal(config.sslMode, "require");
  assert.equal(config.caCertPath, "/run/secrets/aiven-ca.pem");
});

test("database config rejects non-PostgreSQL URLs", () => {
  assert.throws(
    () => createDatabaseConfig({ DATABASE_URL: "mysql://example" }),
    /DATABASE_URL must use postgres/
  );
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test services/community-api/src/database/config.test.js`
Expected: FAIL because `config.js` does not exist.

- [ ] **Step 3: Implement config parser**

Create `config.js` with `createDatabaseConfig(env = process.env)`. Return memory mode for blank URLs, accept `postgres:` and `postgresql:` URLs, and expose `sslMode` plus `AIVEN_CA_CERT_PATH`.

- [ ] **Step 4: Run test to verify it passes**

Run: `node --test services/community-api/src/database/config.test.js`
Expected: PASS.

### Task 3: Documentation and Verification

**Files:**
- Modify: `docs/deployment/boxd-cloudflare-aiven.md`
- Modify: `README.md`

- [ ] **Step 1: Document migration scope**

Add a note that the first migration defines the production schema but runtime still defaults to in-memory state until the Postgres-backed store is implemented.

- [ ] **Step 2: Run focused tests**

Run: `node --test services/community-api/src/database/migrations.test.js services/community-api/src/database/config.test.js`
Expected: PASS.

- [ ] **Step 3: Run repository tests**

Run: `npm.cmd test`
Expected: all Node tests pass.

- [ ] **Step 4: Check formatting**

Run: `git diff --check`
Expected: no whitespace errors.
