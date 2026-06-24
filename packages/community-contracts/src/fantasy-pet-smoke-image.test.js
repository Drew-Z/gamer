import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../..",
);
const helperPath = path.join(repoRoot, "tools", "fantasy-pet-smoke-image.ps1");
const lifecycleSmokePath = path.join(
  repoRoot,
  "tools",
  "smoke-fantasy-pet-public-lifecycle.ps1",
);
const validPng1x1 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR4nGNgAAIAAAUAAXpeqz8AAAAASUVORK5CYII=";
const invalidPngWithBadCrc =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

function resolvePowerShell() {
  for (const executable of ["powershell.exe", "pwsh", "powershell"]) {
    const probe = spawnSync(
      executable,
      ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "$PSVersionTable.PSVersion.ToString()"],
      { encoding: "utf8" },
    );
    if (!probe.error) {
      return executable;
    }
  }

  return "";
}

const powershell = resolvePowerShell();
const powershellSkip = powershell === "" ? "PowerShell is not available in this test environment" : false;

function testWithPowerShell(name, fn) {
  test(name, { skip: powershellSkip }, fn);
}

function quotePowerShellLiteral(value) {
  return `'${value.replaceAll("'", "''")}'`;
}

function runImageDecode(base64Image) {
  const script = `
$ErrorActionPreference = "Stop"
. ${quotePowerShellLiteral(helperPath)}
[byte[]]$bytes = [Convert]::FromBase64String(${quotePowerShellLiteral(base64Image)})
$result = Assert-SmokeImageDecodes -Bytes $bytes -Label "candidate preview"
$result | ConvertTo-Json -Depth 4
`;

  return spawnSync(
    powershell,
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script],
    { encoding: "utf8" },
  );
}

testWithPowerShell("fantasy pet smoke image helper accepts decodable preview images", () => {
  const result = runImageDecode(validPng1x1);

  assert.equal(result.status, 0, result.stderr);
  const decoded = JSON.parse(result.stdout);
  assert.equal(decoded.width, 1);
  assert.equal(decoded.height, 1);
});

testWithPowerShell("fantasy pet smoke image helper rejects corrupt preview images", () => {
  const result = runImageDecode(invalidPngWithBadCrc);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /candidate preview/);
});

test("fantasy pet public lifecycle smoke validates package manifest fields", () => {
  const script = readFileSync(lifecycleSmokePath, "utf8");

  assert.match(script, /package-manifest\.json/);
  assert.match(script, /acceptedBy/);
  assert.match(script, /sourceDownloadId/);
  assert.match(script, /candidate/);
});
