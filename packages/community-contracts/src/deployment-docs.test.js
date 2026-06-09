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
