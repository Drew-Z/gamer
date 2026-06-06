import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const specPath = join(
  process.cwd(),
  "docs",
  "superpowers",
  "specs",
  "2026-06-04-gamer-pet-community-ecosystem-design.md"
);

test("ecosystem spec documents export artifact field mapping", () => {
  const spec = readFileSync(specPath, "utf8");

  assert.ok(spec.includes('"exportArtifact": "exports/stardust-package.zip"'));
  assert.ok(spec.includes("assets.exportArtifact"));
  assert.ok(spec.includes("importSummary.assets.exportArtifactPath"));
  assert.ok(spec.includes("metadata.exportArtifactPath"));
  assert.ok(spec.includes("approvedPets.items[].assets.exportArtifactPath"));
});
