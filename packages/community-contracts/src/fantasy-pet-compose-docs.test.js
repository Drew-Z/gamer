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
  assert.match(compose, /8765:8765/);
  assert.match(compose, /tools\/app_server\.py|tools\\app_server\.py/);
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

test("Android build supports configurable local API base URLs", () => {
  const buildGradle = readRepoFile("apps/android-community/app/build.gradle");
  const readme = readRepoFile("README.md");

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
