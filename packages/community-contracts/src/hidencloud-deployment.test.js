import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const root = process.cwd();
const indexPath = join(root, "index.js");
const readmePath = join(root, "deploy", "hidencloud", "README.md");

test("HidenCloud Node entrypoint starts the community API from root index.js", () => {
  assert.ok(existsSync(indexPath));

  const source = readFileSync(indexPath, "utf8");

  assert.ok(source.includes("startCommunityApiServer"));
  assert.ok(source.includes("./services/community-api/src/server.js"));
  assert.doesNotMatch(source, /admin-review|pet-generator|fantasy-pet-rule/);
});

test("HidenCloud runbook documents fixed startup fields", () => {
  assert.ok(existsSync(readmePath));

  const readme = readFileSync(readmePath, "utf8");

  assert.ok(readme.includes("Server Image: Nodejs 23"));
  assert.ok(readme.includes("Git Repo Address: https://github.com/Drew-Z/gamer"));
  assert.ok(readme.includes("Install Branch: main"));
  assert.ok(readme.includes("Main File: index.js"));
  assert.ok(readme.includes("Auto Update: 1"));
  assert.ok(readme.includes("User Uploaded Files: 0"));
  assert.ok(readme.includes("PORT"));
  assert.ok(readme.includes("SERVER_PORT"));
  assert.ok(readme.includes("FANTASY_PET_API_BASE_URL"));
  assert.ok(readme.includes("only starts `community-api`"));
});
