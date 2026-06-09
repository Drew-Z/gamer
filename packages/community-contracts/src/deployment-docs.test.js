import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const root = process.cwd();
const deploymentDocPath = join(root, "docs", "deployment", "boxd-cloudflare-aiven.md");
const composePath = join(root, "compose.boxd.yaml");
const envExamplePath = join(root, "deploy", "boxd", ".env.production.example");
const gitignorePath = join(root, ".gitignore");
const gitattributesPath = join(root, ".gitattributes");
const boxdReadmePath = join(root, "deploy", "boxd", "README.md");
const boxdDeployScriptPath = join(root, "deploy", "boxd", "deploy.sh");

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

test("Boxd runbook documents direct VM deployment steps and tunnel targets", () => {
  assert.ok(existsSync(boxdReadmePath));
  assert.ok(existsSync(gitignorePath));

  const readme = readFileSync(boxdReadmePath, "utf8");
  const gitignore = readFileSync(gitignorePath, "utf8");

  assert.ok(readme.includes("ssh boxd.sh new --name=gamer-prod"));
  assert.ok(readme.includes("ssh gamer-prod.boxd.sh"));
  assert.ok(readme.includes("git clone"));
  assert.ok(readme.includes("cp deploy/boxd/.env.production.example deploy/boxd/.env.production"));
  assert.ok(readme.includes("Do not commit `deploy/boxd/.env.production`"));
  assert.ok(readme.includes("docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production config"));
  assert.ok(readme.includes("docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production run --rm community-api npm run migrate:community-db:dry-run"));
  assert.ok(readme.includes("docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production run --rm community-api npm run migrate:community-db"));
  assert.ok(readme.includes("deploy/boxd/deploy.sh"));
  assert.ok(readme.includes("api.example.com -> http://community-api:4000"));
  assert.ok(readme.includes("review.example.com -> http://admin-review:4200"));
  assert.ok(readme.includes("FANTASY_PET_API_BASE_URL"));
  assert.doesNotMatch(readme, /sk_live|secret-token|postgres:\/\/[^\\s]*:[^\\s]*@/i);
  assert.ok(gitignore.includes("deploy/boxd/.env.production"));
});

test("Boxd deploy script validates compose and keeps migrations explicit", () => {
  assert.ok(existsSync(boxdDeployScriptPath));

  const script = readFileSync(boxdDeployScriptPath, "utf8");

  assert.ok(script.startsWith("#!/usr/bin/env sh"));
  assert.ok(script.includes("set -eu"));
  assert.ok(script.includes('ENV_FILE="${ENV_FILE:-deploy/boxd/.env.production}"'));
  assert.ok(script.includes('COMPOSE_FILE="${COMPOSE_FILE:-compose.boxd.yaml}"'));
  assert.ok(script.includes("docker compose -f \"$COMPOSE_FILE\" --env-file \"$ENV_FILE\" config"));
  assert.ok(script.includes("migrate:community-db:dry-run"));
  assert.ok(script.includes("migrate:community-db"));
  assert.ok(script.includes("RUN_MIGRATIONS"));
  assert.ok(script.includes("docker compose -f \"$COMPOSE_FILE\" --env-file \"$ENV_FILE\" up -d --build"));
  assert.ok(script.includes("docker compose -f \"$COMPOSE_FILE\" --env-file \"$ENV_FILE\" ps"));
  assert.doesNotMatch(script, /CLOUDFLARED_TOKEN=[A-Za-z0-9_-]+/);
  assert.doesNotMatch(script, /R2_SECRET_ACCESS_KEY=[A-Za-z0-9_-]+/);
  assert.doesNotMatch(script, /DATABASE_URL=postgres/i);
});

test("Boxd shell scripts keep Linux-friendly line endings", () => {
  assert.ok(existsSync(gitattributesPath));

  const gitattributes = readFileSync(gitattributesPath, "utf8");

  assert.ok(gitattributes.includes("deploy/boxd/*.sh text eol=lf"));
});
