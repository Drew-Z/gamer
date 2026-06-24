import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";
import {
  loadFantasyPetAppHandoffRecord,
  missingInternalAuditPolicyFieldMarkers
} from "./fantasy-pet-public-api-coverage.js";

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../..",
);

function readRepoFile(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

test("fantasy pet public API has a Docker Compose overlay", () => {
  const compose = readRepoFile("compose.fantasy-pet.yaml");

  assert.match(compose, /fantasy-pet-api:/);
  assert.match(compose, /community-api:/);
  assert.match(compose, /FANTASY_PET_API_BASE_URL:\s+"http:\/\/fantasy-pet-api:8765"/);
  assert.match(compose, /build:\s*\n\s+context: \.\.\/fantasy-pet-rule\s*\n\s+dockerfile: Dockerfile/);
  assert.match(compose, /fantasy_pet_adapter_config:/);
  assert.match(compose, /8765:8765/);
  assert.match(compose, /tools\/app_server\.py|tools\\app_server\.py/);
  assert.doesNotMatch(compose, /\.\.\/fantasy-pet-rule:\/workspace/);
  assert.doesNotMatch(compose, /enable-admin|\/admin|server-worker-cycle|agent-outputs/);
});

test("README documents Docker startup for the fantasy pet public API", () => {
  const readme = readRepoFile("README.md");

  assert.match(
    readme,
    /docker compose -f compose\.yaml -f compose\.fantasy-pet\.yaml --profile fantasy-pet up --build/,
  );
  assert.match(
    readme,
    /With the Docker overlay running[\s\S]*http:\/\/127\.0\.0\.1:8765\/app-api-contract/,
  );
  assert.match(readme, /FANTASY_PET_API_BASE_URL/);
});

test("private ops Compose overlay gates startup through Postgres migrations", () => {
  const compose = readRepoFile("compose.private-ops.yaml");

  assert.match(compose, /community-db:/);
  assert.match(compose, /postgres:17-alpine/);
  assert.match(compose, /POSTGRES_PASSWORD:\s+"\$\{COMMUNITY_POSTGRES_PASSWORD:\?set COMMUNITY_POSTGRES_PASSWORD\}"/);
  assert.match(compose, /community-migrate:/);
  assert.match(compose, /migrate:community-db/);
  assert.match(compose, /service_completed_successfully/);
  assert.match(compose, /COMMUNITY_DEMO_TOKEN:\s+"\$\{COMMUNITY_DEMO_TOKEN:\?set COMMUNITY_DEMO_TOKEN\}"/);
  assert.match(compose, /FANTASY_PET_UPSTREAM_TOKEN:\s+"\$\{FANTASY_PET_UPSTREAM_TOKEN:\?set FANTASY_PET_UPSTREAM_TOKEN\}"/);
  assert.match(compose, /private-ops-proxy:/);
  assert.match(compose, /CADDY_ADMIN_BASIC_AUTH_HASH:\s+"\$\{CADDY_ADMIN_BASIC_AUTH_HASH:\?set CADDY_ADMIN_BASIC_AUTH_HASH\}"/);
  assert.match(compose, /deploy\/Caddyfile\.private-ops/);
  assert.match(compose, /driver: json-file/);
  assert.match(compose, /mem_limit:/);
  assert.match(compose, /cpus:/);
  assert.match(compose, /env_file:/);
  assert.match(compose, /\/health/);
  assert.match(compose, /\/worker-readiness/);
  assert.doesNotMatch(compose, /5432:5432/);
});

test("private ops smoke verifies auth readiness and leak boundaries", () => {
  const script = readRepoFile("tools/private-ops-smoke.js");
  const preflight = readRepoFile("tools/private-ops-preflight.js");
  const packageJson = readRepoFile("package.json");
  const readme = readRepoFile("README.md");

  assert.match(script, /COMMUNITY_DEMO_TOKEN/);
  assert.match(script, /FANTASY_PET_UPSTREAM_TOKEN/);
  assert.match(script, /PRIVATE_OPS_BASIC_AUTH_USER/);
  assert.match(script, /\/health/);
  assert.match(script, /\/v1\/sla/);
  assert.match(script, /\/worker-readiness/);
  assert.match(script, /\/app-api-contract/);
  assert.match(script, /unauthorized_demo_request/);
  assert.match(script, /PRIVATE_OPS_CREATE_JOB/);
  assert.match(script, /PRIVATE_OPS_KNOWN_APP_JOB_ID/);
  assert.match(script, /known job package gate is observable/);
  assert.match(script, /fantasy-pet\.package-download-response\.v1/);
  assert.match(script, /assertNoLeaks/);
  assert.match(preflight, /PRIVATE_OPS_ENV_FILE/);
  assert.match(preflight, /COMMUNITY_POSTGRES_PASSWORD/);
  assert.match(preflight, /FANTASY_PET_ADAPTER_CONFIG_FILE/);
  assert.match(preflight, /CADDY_ADMIN_BASIC_AUTH_HASH/);
  assert.match(preflight, /COMMUNITY_CORS_ALLOWED_ORIGINS/);
  assert.match(packageJson, /"smoke:private-ops": "node tools\/private-ops-smoke\.js"/);
  assert.match(packageJson, /"preflight:private-ops": "node tools\/private-ops-preflight\.js"/);
  assert.match(readme, /compose\.private-ops\.yaml/);
  assert.match(readme, /npm\.cmd run preflight:private-ops/);
  assert.match(readme, /npm\.cmd run smoke:private-ops/);
  assert.match(readme, /PRIVATE_OPS_KNOWN_APP_JOB_ID/);
});

test("private ops deployment assets document TLS monitoring and backup hooks", () => {
  const caddyfile = readRepoFile("deploy/Caddyfile.private-ops");
  const cron = readRepoFile("deploy/private-ops-cron.example");
  const logrotate = readRepoFile("deploy/private-ops-logrotate.conf");
  const backup = readRepoFile("tools/private-ops-backup.sh");
  const restore = readRepoFile("tools/private-ops-restore.sh");
  const prune = readRepoFile("tools/private-ops-prune-agent-runs.sh");
  const rollback = readRepoFile("tools/private-ops-rollback.sh");

  assert.match(caddyfile, /tls \{\$PRIVATE_OPS_TLS_MODE:internal\}/);
  assert.match(caddyfile, /basic_auth/);
  assert.match(caddyfile, /reverse_proxy admin-review:4200/);
  assert.match(cron, /\*\/5 \* \* \* \*/);
  assert.match(cron, /npm run smoke:private-ops/);
  assert.match(logrotate, /rotate 14/);
  assert.match(backup, /pg_dump/);
  assert.match(restore, /psql/);
  assert.match(restore, /migrate:community-db:dry-run/);
  assert.match(prune, /FANTASY_PET_RUN_RETENTION_DAYS:-14/);
  assert.match(prune, /find \/data\/runs/);
  assert.match(rollback, /PRIVATE_OPS_ROLLBACK_APPLY/);
  assert.match(rollback, /GAMER_IMAGE_TAG/);
  assert.match(rollback, /FANTASY_PET_IMAGE_TAG/);
  assert.match(rollback, /COMMUNITY_API_IMAGE=desktop-pet-community-api:\$target_release/);
  assert.match(rollback, /ADMIN_REVIEW_IMAGE=desktop-pet-admin-review:\$target_release/);
  assert.match(rollback, /FANTASY_PET_IMAGE=desktop-pet-agent:\$target_release/);
  assert.match(rollback, /migrate:community-db:dry-run/);
  assert.match(rollback, /--no-build/);
  assert.match(rollback, /--force-recreate/);
});

test("Android build supports configurable local API base URLs", () => {
  const buildGradle = readRepoFile("apps/android-community/app/build.gradle");
  const readme = readRepoFile("README.md");

  assert.match(
    buildGradle,
    /System\.getenv\('FANTASY_PET_API_BASE_URL'\)\s*\?:\s*communityApiBaseUrl/,
  );
  assert.match(buildGradle, /System\.getenv\('FANTASY_PET_API_BASE_URL'\)/);
  assert.match(buildGradle, /System\.getenv\('COMMUNITY_API_BASE_URL'\)/);
  assert.match(readme, /FANTASY_PET_API_BASE_URL/);
  assert.match(readme, /COMMUNITY_API_BASE_URL/);
});

test("fantasy pet public lifecycle smoke blocks current handoff internals", () => {
  const handoff = loadFantasyPetAppHandoffRecord();
  const smokeScript = readRepoFile("tools/smoke-fantasy-pet-public-lifecycle.ps1");
  const lowerSmokeScript = smokeScript.toLowerCase();
  const missingInternalArtifacts = handoff.internalArtifactsNotForApp
    .map((artifact) => artifact.split(/[\\/]/u).at(-1)?.toLowerCase() ?? "")
    .filter((marker) => /[a-z0-9]/u.test(marker))
    .filter((marker) => !lowerSmokeScript.includes(marker));

  assert.deepEqual(missingInternalArtifacts, []);
  assert.deepEqual(
    missingInternalAuditPolicyFieldMarkers(smokeScript, handoff.internalAuditPolicy ?? {}),
    [],
  );
  assert.match(smokeScript, /function\s+Get-SmokeInternalMarkers/);
  assert.match(smokeScript, /\$forbidden\s*=\s*@\(Get-SmokeInternalMarkers\)/);
});

test("fantasy pet public lifecycle smoke validates the runtime app API contract boundary", () => {
  const smokeScript = readRepoFile("tools/smoke-fantasy-pet-public-lifecycle.ps1");

  assert.match(smokeScript, /function\s+Assert-SmokeAppApiContract/);
  assert.match(smokeScript, /\$requiredPublicPaths\s*=\s*@\(/);
  assert.match(smokeScript, /\/pet-generation-jobs\/\{appJobId\}\/review-decisions/);
  assert.match(smokeScript, /\/pet-generation-jobs\/\{appJobId\}\/package/);
  assert.match(smokeScript, /\/worker-readiness/);
  assert.match(smokeScript, /exposesInternalPaths/);
  assert.match(smokeScript, /appMayInvokeAgentsDirectly/);
  assert.match(smokeScript, /requiresHumanReview/);
  assert.match(smokeScript, /adminEndpointsDisabledByDefault/);
});

test("fantasy pet public lifecycle smoke validates worker readiness stays app-safe", () => {
  const smokeScript = readRepoFile("tools/smoke-fantasy-pet-public-lifecycle.ps1");

  assert.match(smokeScript, /function\s+Assert-SmokeWorkerReadiness/);
  assert.match(smokeScript, /\/worker-readiness/);
  assert.match(smokeScript, /fantasy-pet\.worker-readiness-public\.v1/);
  assert.match(smokeScript, /secretsInReport/);
  assert.match(smokeScript, /executesAgentProcesses/);
  assert.match(smokeScript, /appMayInvokeAgentsDirectly/);
  assert.match(smokeScript, /executesReadinessProbe/);
  assert.match(smokeScript, /workerReadinessStatus/);
});

test("fantasy pet public lifecycle smoke exercises revise and reject human review states", () => {
  const smokeScript = readRepoFile("tools/smoke-fantasy-pet-public-lifecycle.ps1");

  assert.match(smokeScript, /-Decision\s+"revise"/);
  assert.match(smokeScript, /-Decision\s+"reject"/);
  assert.match(smokeScript, /targetDownloadId/);
  assert.match(smokeScript, /idle action jumps vertically|running-right is nearly static/);
  assert.match(smokeScript, /revision-requested/);
  assert.match(smokeScript, /candidate-rejected/);
  assert.match(smokeScript, /packageBlockedAfterRevise/);
  assert.match(smokeScript, /packageBlockedAfterReject/);
  const requestLines = smokeScript
    .split("\n")
    .filter((line) => /Invoke-RestMethod|DownloadData/.test(line))
    .join("\n");
  assert.doesNotMatch(requestLines, /\/admin|server-worker-cycle|agent-outputs|targetOutput/);
});

test("fantasy pet community import smoke uses only public app and community routes", () => {
  const script = readRepoFile("tools/smoke-fantasy-pet-community-import.ps1");
  const launcher = readRepoFile("tools/smoke-fantasy-pet-community-import.cmd");

  assert.match(script, /smoke-fantasy-pet-public-lifecycle\.ps1/);
  assert.match(script, /\/v1\/import-drafts\/from-fantasy-pet-package/);
  assert.match(script, /\/v1\/import-drafts\/submit/);
  assert.match(script, /targetDownloadId/);
  assert.doesNotMatch(script, /\/admin|server-worker-cycle|agent-outputs|targetOutput/);
  assert.match(launcher, /smoke-fantasy-pet-community-import\.ps1/);
});
