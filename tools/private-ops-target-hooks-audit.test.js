import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

test("private ops target hooks audit verifies scheduler logrotate and fresh smoke log", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-hooks-"));
  const logDir = path.join(tempDir, "logs");
  const cronFile = path.join(tempDir, "private-ops.cron");
  const logrotateFile = path.join(tempDir, "private-ops.logrotate");
  const smokeLogFile = path.join(logDir, "private-ops-smoke.log");
  fs.mkdirSync(logDir);
  fs.writeFileSync(
    cronFile,
    [
      "# private desktop pet monitor",
      `*/5 * * * * cd /opt/desktop-pet/gamer && set -a && . ./.env.private-ops && set +a && npm run smoke:private-ops >> ${smokeLogFile} 2>&1`,
      ""
    ].join("\n")
  );
  fs.writeFileSync(
    logrotateFile,
    [
      `${logDir}/*.log {`,
      "    daily",
      "    rotate 14",
      "    compress",
      "    missingok",
      "    notifempty",
      "    copytruncate",
      "}",
      ""
    ].join("\n")
  );
  fs.writeFileSync(
    smokeLogFile,
    JSON.stringify({
      ok: true,
      checks: [{ name: "community health is public-safe", status: "pass" }],
      requiredTls: true
    })
  );

  const result = await runAudit({
    PRIVATE_OPS_CRON_FILE: cronFile,
    PRIVATE_OPS_LOGROTATE_FILE: logrotateFile,
    PRIVATE_OPS_LOG_DIR: logDir,
    PRIVATE_OPS_SMOKE_LOG_FILE: smokeLogFile,
    PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG: "1",
    COMMUNITY_DEMO_TOKEN: "community-private-token-123",
    FANTASY_PET_UPSTREAM_TOKEN: "agent-private-token-123",
    PRIVATE_OPS_BASIC_AUTH_PASSWORD: "basic-auth-password-123"
  });

  assert.equal(result.exitCode, 0, result.stderr);
  const output = JSON.parse(result.stdout);
  assert.equal(output.schema, "desktop-pet.ops.target-hooks-audit.v1");
  assert.equal(output.ok, true);
  assert.equal(output.freshSmokeLogRequired, true);
  assert.equal(output.targetHooks.cronConfigured, true);
  assert.equal(output.targetHooks.logrotateConfigured, true);
  assert.match(JSON.stringify(output.checks), /external scheduler runs private ops smoke/);
  assert.match(JSON.stringify(output.checks), /recurring smoke log records successful smoke/);
  assert.doesNotMatch(result.stdout, /community-private-token-123|agent-private-token-123|basic-auth-password-123/);
});

test("private ops target hooks audit fails stale logs and secret leaks safely", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-hooks-"));
  const logDir = path.join(tempDir, "logs");
  const cronFile = path.join(tempDir, "private-ops.cron");
  const logrotateFile = path.join(tempDir, "private-ops.logrotate");
  const smokeLogFile = path.join(logDir, "private-ops-smoke.log");
  fs.mkdirSync(logDir);
  fs.writeFileSync(
    cronFile,
    `*/5 * * * * cd /opt/desktop-pet/gamer && set -a && . ./.env.private-ops && set +a && npm run smoke:private-ops >> ${smokeLogFile} 2>&1\n`
  );
  fs.writeFileSync(
    logrotateFile,
    [
      `${logDir}/*.log {`,
      "    daily",
      "    rotate 14",
      "    compress",
      "    copytruncate",
      "}",
      ""
    ].join("\n")
  );
  fs.writeFileSync(
    smokeLogFile,
    JSON.stringify({
      ok: true,
      checks: [],
      leaked: "community-private-token-123"
    })
  );
  const staleTime = new Date(Date.now() - 60 * 60 * 1000);
  fs.utimesSync(smokeLogFile, staleTime, staleTime);

  const result = await runAudit({
    PRIVATE_OPS_CRON_FILE: cronFile,
    PRIVATE_OPS_LOGROTATE_FILE: logrotateFile,
    PRIVATE_OPS_LOG_DIR: logDir,
    PRIVATE_OPS_SMOKE_LOG_FILE: smokeLogFile,
    PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG: "1",
    PRIVATE_OPS_SMOKE_LOG_MAX_AGE_MS: "1000",
    COMMUNITY_DEMO_TOKEN: "community-private-token-123"
  });

  assert.equal(result.exitCode, 1);
  const output = JSON.parse(result.stderr);
  assert.equal(output.ok, false);
  assert(
    output.checks.some(
      (check) => check.name === "recurring smoke log is fresh" && check.status === "fail"
    )
  );
  assert(
    output.checks.some(
      (check) => check.name === "recurring smoke log does not contain configured secret fragments" && check.status === "fail"
    )
  );
  assert.doesNotMatch(result.stderr, /community-private-token-123/);
});

function runAudit(env) {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      [path.join(repoRoot, "tools/private-ops-target-hooks-audit.js")],
      {
        cwd: repoRoot,
        env: {
          ...process.env,
          ...env
        },
        stdio: ["ignore", "pipe", "pipe"]
      }
    );
    let stdout = "";
    let stderr = "";

    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk) => {
      stdout += chunk;
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk;
    });
    child.on("error", reject);
    child.on("close", (exitCode) => {
      resolve({ exitCode, stdout, stderr });
    });
  });
}
