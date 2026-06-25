import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const root = process.cwd();

test("repository keeps runtime entrypoint separate from private ops assets", () => {
  const indexPath = join(root, "index.js");

  assert.ok(existsSync(indexPath));
  const indexSource = readFileSync(indexPath, "utf8");
  assert.ok(indexSource.includes("startCommunityApiServer"));
  assert.ok(indexSource.includes("startPrivateOpsUserHooks"));
  assert.ok(existsSync(join(root, "tools", "private-ops-user-hooks.js")));
  assert.ok(existsSync(join(root, "deploy", "Caddyfile.private-ops")));
  assert.ok(existsSync(join(root, "deploy", "private-ops-cron.example")));
  assert.equal(existsSync(join(root, "docs", "deployment")), false);
});
