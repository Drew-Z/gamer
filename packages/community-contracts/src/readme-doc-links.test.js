import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const readmePath = join(process.cwd(), "README.md");

test("README exposes contract docs and phase verification commands", () => {
  const readme = readFileSync(readmePath, "utf8");

  assert.ok(readme.includes("docs/api/community-api.md"));
  assert.ok(
    readme.includes(
      "docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md",
    ),
  );
  assert.ok(readme.includes("npm.cmd test"));
  assert.ok(readme.includes("testDebugUnitTest --console=plain"));
  assert.ok(readme.includes("docker compose config"));
  assert.ok(readme.includes("git diff --check"));
});
