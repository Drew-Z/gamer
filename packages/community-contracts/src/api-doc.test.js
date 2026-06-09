import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const apiDocPath = join(process.cwd(), "docs", "api", "community-api.md");

test("community API docs include export artifact response examples", () => {
  const doc = readFileSync(apiDocPath, "utf8");

  assert.ok(doc.includes("GET /v1/feed"));
  assert.ok(doc.includes("GET /v1/community-home"));
  assert.ok(doc.includes("gamer.community-home.v1"));
  assert.ok(doc.includes("dailyCheckIn"));
  assert.ok(doc.includes("submissionsSummary.pendingCount"));
  assert.ok(doc.includes("feed.items[].metadata.exportArtifactPath"));
  assert.ok(doc.includes('"exportArtifactPath": "exports/stardust-package.zip"'));
  assert.ok(doc.includes("GET /v1/pets/approved"));
  assert.ok(doc.includes("approvedPets.items[].assets.exportArtifactPath"));
  assert.ok(doc.includes('"assets": {'));
  assert.ok(doc.includes("GET /v1/pets/approved/:petId/package"));
  assert.ok(doc.includes("approvedPetPackage.package.exportArtifactPath"));
  assert.ok(doc.includes("approved_pet_package_not_found"));
  assert.ok(doc.includes("GET /v1/submissions/:submissionId"));
  assert.ok(doc.includes("submission_not_found"));
  assert.ok(doc.includes('"status": "pending"'));
  assert.ok(doc.includes("POST /v1/import-drafts/from-fantasy-pet-package"));
  assert.ok(doc.includes("fantasy-pet.package-manifest.v1"));
  assert.ok(doc.includes("invalid_fantasy_pet_package"));
  assert.ok(doc.includes("safe package-relative path"));
  assert.ok(doc.includes("tools\\smoke-fantasy-pet-community-import.cmd"));
  assert.ok(doc.includes("/v1/import-drafts/submit"));
  assert.ok(doc.includes("public app and community endpoints"));
});
