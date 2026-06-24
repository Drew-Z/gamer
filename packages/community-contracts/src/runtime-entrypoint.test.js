import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const root = process.cwd();

test("repository keeps runtime entrypoint separate from private ops assets", () => {
  const indexPath = join(root, "index.js");

  assert.ok(existsSync(indexPath));
  assert.ok(readFileSync(indexPath, "utf8").includes("startCommunityApiServer"));
  assert.ok(existsSync(join(root, "deploy", "Caddyfile.private-ops")));
  assert.ok(existsSync(join(root, "deploy", "private-ops-cron.example")));
  assert.equal(existsSync(join(root, "docs", "deployment")), false);
});
