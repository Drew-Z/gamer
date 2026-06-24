import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);

test("private ops preflight fails on missing and placeholder values without leaking secrets", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-preflight-"));
  const envFile = path.join(tempDir, ".env.private-ops");
  fs.writeFileSync(
    envFile,
    [
      "COMMUNITY_POSTGRES_PASSWORD=REPLACE_WITH_PRIVATE_PASSWORD",
      "COMMUNITY_DEMO_TOKEN=community-super-secret-token",
      "FANTASY_PET_UPSTREAM_TOKEN=agent-super-secret-token",
      "FANTASY_PET_ADAPTER_CONFIG_FILE=missing-adapter-config.json",
      "PRIVATE_OPS_HOST=desktop-pet.example.internal",
      "COMMUNITY_CORS_ALLOWED_ORIGINS=https://desktop-pet.example.internal",
      ""
    ].join("\n"),
  );

  const result = await runPreflight({
    PRIVATE_OPS_ENV_FILE: envFile
  });

  assert.equal(result.exitCode, 1);
  assert.doesNotMatch(result.stdout, /community-super-secret-token|agent-super-secret-token/);
  assert.doesNotMatch(result.stderr, /community-super-secret-token|agent-super-secret-token/);
  assert.match(result.stderr, /COMMUNITY_POSTGRES_PASSWORD/);
  assert.match(result.stderr, /CADDY_ADMIN_BASIC_AUTH_HASH/);
  assert.match(result.stderr, /PRIVATE_OPS_HOST/);
  assert.match(result.stderr, /FANTASY_PET_ADAPTER_CONFIG_FILE/);
});

test("private ops preflight passes with a complete env file and existing adapter config", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-preflight-"));
  const adapterConfig = path.join(tempDir, "adapter-config.server.json");
  const envFile = path.join(tempDir, ".env.private-ops");
  fs.writeFileSync(adapterConfig, JSON.stringify({ ok: true }));
  fs.writeFileSync(
    envFile,
    [
      "COMMUNITY_POSTGRES_PASSWORD=postgres-private-password-123",
      "COMMUNITY_DEMO_TOKEN=community-private-token-123",
      "FANTASY_PET_UPSTREAM_TOKEN=agent-private-token-123",
      "FANTASY_PET_API_BASE_URL=http://fantasy-pet-api:8765",
      `FANTASY_PET_ADAPTER_CONFIG_FILE=${adapterConfig}`,
      "PRIVATE_OPS_HOST=desktop-pet.internal",
      "CADDY_ADMIN_BASIC_AUTH_HASH=$$2a$$14$$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuuabcdefgh",
      "COMMUNITY_CORS_ALLOWED_ORIGINS=https://desktop-pet.internal",
      ""
    ].join("\n"),
  );

  const result = await runPreflight({
    PRIVATE_OPS_ENV_FILE: envFile
  });

  assert.equal(result.exitCode, 0, result.stderr);
  const output = JSON.parse(result.stdout);
  assert.equal(output.ok, true);
  assert.equal(output.checkedEnvFile.endsWith(".env.private-ops"), true);
  assert.equal(output.checkedAdapterConfig, true);
  assert.match(
    output.checks.join("\n"),
    /COMMUNITY_DEMO_TOKEN|FANTASY_PET_ADAPTER_CONFIG_FILE/,
  );
  assert.doesNotMatch(result.stdout, /community-private-token-123|agent-private-token-123/);
});

test("private ops preflight supports hiden community role with a remote Baidu agent URL", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-preflight-"));
  const envFile = path.join(tempDir, ".env.private-ops");
  fs.writeFileSync(
    envFile,
    [
      "PRIVATE_OPS_DEPLOYMENT_ROLE=community",
      "COMMUNITY_POSTGRES_PASSWORD=postgres-private-password-123",
      "COMMUNITY_DEMO_TOKEN=community-private-token-123",
      "FANTASY_PET_UPSTREAM_TOKEN=agent-private-token-123",
      "FANTASY_PET_API_BASE_URL=https://agent.baidu-private.example",
      "PRIVATE_OPS_HOST=hiden-community.internal",
      "CADDY_ADMIN_BASIC_AUTH_HASH=$$2a$$14$$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuuabcdefgh",
      "COMMUNITY_CORS_ALLOWED_ORIGINS=https://hiden-community.internal",
      ""
    ].join("\n"),
  );

  const result = await runPreflight({
    PRIVATE_OPS_ENV_FILE: envFile
  });

  assert.equal(result.exitCode, 0, result.stderr);
  const output = JSON.parse(result.stdout);
  assert.equal(output.ok, true);
  assert.equal(output.deploymentRole, "community");
  assert.equal(output.checkedAdapterConfig, false);
  assert.match(output.checks.join("\n"), /FANTASY_PET_API_BASE_URL/);
  assert.doesNotMatch(result.stdout, /community-private-token-123|agent-private-token-123/);
});

function runPreflight(env) {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      [path.join(repoRoot, "tools/private-ops-preflight.js")],
      {
        cwd: repoRoot,
        env: {
          ...process.env,
          ...env
        },
        stdio: ["ignore", "pipe", "pipe"]
      },
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
      resolve({
        exitCode,
        stdout,
        stderr
      });
    });
  });
}
