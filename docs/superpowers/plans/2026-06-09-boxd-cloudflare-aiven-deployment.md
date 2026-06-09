# Boxd Cloudflare Aiven Deployment Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a production-shaped deployment skeleton for running Gamer community services on Boxd behind Cloudflare, with Aiven PostgreSQL as the future relational store and Cloudflare R2 as the future object store.

**Architecture:** Boxd runs Docker services for `community-api`, `admin-review`, and `cloudflared`. Cloudflare Tunnel terminates public hostnames and forwards to private Docker service names. Aiven PostgreSQL and Cloudflare R2 remain external managed services configured through environment variables, not local containers or committed secrets.

**Tech Stack:** Docker Compose, Cloudflare Tunnel, Cloudflare R2, Aiven PostgreSQL, Node.js service tests.

---

### Task 1: Deployment Contract Test

**Files:**
- Create: `packages/community-contracts/src/deployment-docs.test.js`

- [ ] **Step 1: Write the failing test**

```js
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const root = process.cwd();
const deploymentDocPath = join(root, "docs", "deployment", "boxd-cloudflare-aiven.md");
const composePath = join(root, "compose.boxd.yaml");
const envExamplePath = join(root, "deploy", "boxd", ".env.production.example");

test("Boxd Cloudflare Aiven deployment skeleton documents managed service boundaries", () => {
  assert.ok(existsSync(deploymentDocPath));
  assert.ok(existsSync(composePath));
  assert.ok(existsSync(envExamplePath));

  const doc = readFileSync(deploymentDocPath, "utf8");
  const compose = readFileSync(composePath, "utf8");
  const envExample = readFileSync(envExamplePath, "utf8");

  assert.ok(doc.includes("Boxd"));
  assert.ok(doc.includes("Cloudflare Tunnel"));
  assert.ok(doc.includes("Cloudflare R2"));
  assert.ok(doc.includes("Aiven PostgreSQL"));
  assert.ok(doc.includes("fantasy-pet generation server"));
  assert.ok(doc.includes("Do not commit real secrets"));
  assert.ok(doc.includes("DATABASE_URL"));
  assert.ok(doc.includes("FANTASY_PET_API_BASE_URL"));
  assert.ok(doc.includes("R2_BUCKET_NAME"));

  assert.ok(compose.includes("cloudflare/cloudflared"));
  assert.ok(compose.includes("CLOUDFLARED_TOKEN"));
  assert.ok(compose.includes("community-api"));
  assert.ok(compose.includes("admin-review"));
  assert.ok(!compose.includes("postgres:"));
  assert.ok(!compose.includes("minio"));

  assert.ok(envExample.includes("DATABASE_URL="));
  assert.ok(envExample.includes("AIVEN_CA_CERT_PATH="));
  assert.ok(envExample.includes("CLOUDFLARED_TOKEN="));
  assert.ok(envExample.includes("R2_BUCKET_NAME="));
  assert.ok(envExample.includes("R2_ACCESS_KEY_ID="));
  assert.ok(envExample.includes("R2_SECRET_ACCESS_KEY="));
  assert.doesNotMatch(envExample, /password|secret-token|sk_live/i);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test packages/community-contracts/src/deployment-docs.test.js`
Expected: FAIL because the deployment skeleton files do not exist yet.

### Task 2: Deployment Skeleton Files

**Files:**
- Create: `compose.boxd.yaml`
- Create: `deploy/boxd/.env.production.example`
- Create: `docs/deployment/boxd-cloudflare-aiven.md`
- Modify: `README.md`

- [ ] **Step 1: Add Compose services**

Create `compose.boxd.yaml` with `community-api`, `admin-review`, and `cloudflared`. Use external env vars for `DATABASE_URL`, Aiven CA path, R2 values, and `FANTASY_PET_API_BASE_URL`. Do not define local `postgres` or `minio` services.

- [ ] **Step 2: Add env template**

Create `deploy/boxd/.env.production.example` with placeholder values only. It must document `DATABASE_URL`, `AIVEN_CA_CERT_PATH`, `CLOUDFLARED_TOKEN`, `R2_BUCKET_NAME`, `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, and public API URLs.

- [ ] **Step 3: Add deployment guide**

Create `docs/deployment/boxd-cloudflare-aiven.md` describing service responsibilities, Cloudflare Tunnel ingress, Aiven PostgreSQL role, R2 role, Android base URLs, safety boundaries, and verification commands.

- [ ] **Step 4: Link README**

Add a docs bullet and production skeleton command to `README.md`.

- [ ] **Step 5: Run test to verify it passes**

Run: `node --test packages/community-contracts/src/deployment-docs.test.js`
Expected: PASS.

### Task 3: Verification

**Files:**
- No additional files.

- [ ] **Step 1: Validate Compose syntax**

Run: `docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production.example config`
Expected: Compose config renders without defining local Postgres or MinIO.

- [ ] **Step 2: Run repository tests**

Run: `npm.cmd test`
Expected: all Node tests pass.

- [ ] **Step 3: Check formatting**

Run: `git diff --check`
Expected: no whitespace errors.
