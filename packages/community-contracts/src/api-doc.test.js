import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const apiDocPath = join(process.cwd(), "docs", "api", "community-api.md");

test("community API docs include export artifact response examples", () => {
  const doc = readFileSync(apiDocPath, "utf8");

  assert.ok(doc.includes("GET /v1/feed"));
  assert.ok(doc.includes("feed.items[].metadata.exportArtifactPath"));
  assert.ok(doc.includes('"exportArtifactPath": "exports/stardust-package.zip"'));
  assert.ok(doc.includes("GET /v1/pets/approved"));
  assert.ok(doc.includes("approvedPets.items[].assets.exportArtifactPath"));
  assert.ok(doc.includes('"assets": {'));
});
