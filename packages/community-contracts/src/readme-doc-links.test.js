import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const readmePath = join(process.cwd(), "README.md");
const publicLifecycleSmokePath = join(
  process.cwd(),
  "tools",
  "smoke-fantasy-pet-public-lifecycle.ps1",
);
const publicLifecycleSmokeCmdPath = join(
  process.cwd(),
  "tools",
  "smoke-fantasy-pet-public-lifecycle.cmd",
);
const communityImportSmokePath = join(
  process.cwd(),
  "tools",
  "smoke-fantasy-pet-community-import.ps1",
);
const communityImportSmokeCmdPath = join(
  process.cwd(),
  "tools",
  "smoke-fantasy-pet-community-import.cmd",
);
const verifyFantasyPetIntegrationPath = join(
  process.cwd(),
  "tools",
  "verify-fantasy-pet-integration.ps1",
);
const verifyFantasyPetIntegrationCmdPath = join(
  process.cwd(),
  "tools",
  "verify-fantasy-pet-integration.cmd",
);
const androidUiSmokePath = join(
  process.cwd(),
  "tools",
  "launch-fantasy-pet-android-ui-smoke.ps1",
);
const androidUiSmokeAssertionsPath = join(
  process.cwd(),
  "tools",
  "fantasy-pet-android-ui-smoke-assertions.ps1",
);
const androidUiSmokeCmdPath = join(
  process.cwd(),
  "tools",
  "launch-fantasy-pet-android-ui-smoke.cmd",
);

function quotePowerShellLiteral(value) {
  return `'${value.replaceAll("'", "''")}'`;
}

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
  assert.ok(readme.includes("FANTASY_PET_API_BASE_URL"));
  assert.ok(readme.includes("D:\\workspace4Codex\\pet\\fantasy-pet-rule"));
  assert.ok(readme.includes("http://10.0.2.2:8765"));
  assert.ok(readme.includes("http://10.0.2.2:4000"));
  assert.ok(readme.includes("tools\\app_server.py --run-root runs --host 127.0.0.1 --port 8765"));
  assert.ok(readme.includes("/v1/import-drafts/submit"));
  assert.ok(readme.includes("/v1/submissions/{submissionId}"));
  assert.ok(readme.includes("http://localhost:4000/v1/submissions/submission-local-002"));
  assert.ok(readme.includes("fantasy-pet-public-api-coverage.test.js"));
  assert.ok(readme.includes("fantasy-pet-community-api-safety-coverage.test.js"));
  assert.ok(readme.includes("tools\\smoke-fantasy-pet-public-lifecycle.cmd"));
  assert.ok(readme.includes("tools\\smoke-fantasy-pet-public-lifecycle.ps1"));
  assert.ok(readme.includes("tools\\smoke-fantasy-pet-community-import.cmd"));
  assert.ok(readme.includes("tools\\smoke-fantasy-pet-community-import.ps1"));
  assert.ok(readme.includes("tools\\verify-fantasy-pet-integration.cmd"));
  assert.ok(readme.includes("tools\\verify-fantasy-pet-integration.ps1"));
  assert.ok(readme.includes("tools\\launch-fantasy-pet-android-ui-smoke.cmd"));
  assert.ok(readme.includes("tools\\launch-fantasy-pet-android-ui-smoke.ps1"));
  assert.ok(readme.includes("Android emulator generation UI smoke"));
  assert.ok(readme.includes("tools\\verify-fantasy-pet-integration.cmd -IncludeAndroidUi"));
  assert.ok(readme.includes("Android UI public API port defaults to `18765`"));
  assert.ok(readme.includes("-CaptureScreenshot"));
  assert.ok(readme.includes("-AssertContractDemoUi"));
  assert.ok(readme.includes("Submit to community review"));
  assert.ok(readme.includes("Refresh community submission"));
  assert.ok(readme.includes("installDebug --console=plain --rerun-tasks"));
  assert.ok(readme.includes("adb -s emulator-5554 shell am start -n com.gamer.community/.MainActivity"));
  assert.ok(readme.includes("unexpectedUnhandledPublicEndpointPaths"));
  assert.ok(existsSync(publicLifecycleSmokeCmdPath));
  assert.ok(existsSync(publicLifecycleSmokePath));
  assert.ok(existsSync(communityImportSmokeCmdPath));
  assert.ok(existsSync(communityImportSmokePath));
  assert.ok(existsSync(verifyFantasyPetIntegrationCmdPath));
  assert.ok(existsSync(verifyFantasyPetIntegrationPath));
  assert.ok(existsSync(androidUiSmokeCmdPath));
  assert.ok(existsSync(androidUiSmokePath));
  assert.ok(existsSync(androidUiSmokeAssertionsPath));

  const verifyScript = readFileSync(verifyFantasyPetIntegrationPath, "utf8");
  assert.ok(verifyScript.includes("npm.cmd test"));
  assert.ok(verifyScript.includes("testDebugUnitTest --console=plain"));
  assert.ok(verifyScript.includes("assembleDebug --console=plain"));
  assert.ok(verifyScript.includes("[switch]$IncludeAndroidUi"));
  assert.ok(verifyScript.includes("[int]$AndroidUiPublicApiPort = 18765"));
  assert.ok(verifyScript.includes("connectedDebugAndroidTest"));
  assert.ok(verifyScript.includes("launch-fantasy-pet-android-ui-smoke.cmd"));
  assert.ok(verifyScript.includes("-PublicApiPort"));
  assert.ok(verifyScript.includes("-FantasyPetApiBaseUrl"));
  assert.ok(verifyScript.includes("http://10.0.2.2:$AndroidUiPublicApiPort"));
  assert.ok(verifyScript.includes("-AssertContractDemoUi"));
  assert.ok(verifyScript.includes("smoke-fantasy-pet-public-lifecycle.cmd"));
  assert.ok(verifyScript.includes("smoke-fantasy-pet-community-import.cmd"));
  assert.ok(verifyScript.includes("git diff --check"));
  assert.ok(verifyScript.includes("rg -n -e /admin"));

  const androidUiSmokeScript = readFileSync(androidUiSmokePath, "utf8");
  const androidUiSmokeAssertions = readFileSync(androidUiSmokeAssertionsPath, "utf8");
  const androidUiSmokeLauncher = readFileSync(androidUiSmokeCmdPath, "utf8");
  assert.ok(androidUiSmokeLauncher.includes("launch-fantasy-pet-android-ui-smoke.ps1"));
  assert.ok(androidUiSmokeScript.includes("run_server_job_lifecycle_demo.py"));
  assert.ok(androidUiSmokeScript.includes("tools\\app_server.py"));
  assert.ok(androidUiSmokeScript.includes("FANTASY_PET_API_BASE_URL"));
  assert.ok(androidUiSmokeScript.includes("COMMUNITY_API_BASE_URL"));
  assert.ok(androidUiSmokeScript.includes("[switch]$SkipLaunch"));
  assert.ok(androidUiSmokeScript.includes("[switch]$CaptureScreenshot"));
  assert.ok(androidUiSmokeScript.includes("[switch]$AssertContractDemoUi"));
  assert.ok(androidUiSmokeScript.includes("fantasy-pet-android-ui-smoke-assertions.ps1"));
  assert.ok(androidUiSmokeScript.includes("launch-bubble-enter"));
  assert.ok(androidUiSmokeScript.includes("Assert-ContractDemoAndroidUiState"));
  assert.ok(androidUiSmokeScript.includes("Find-AndroidUiWithTextFragment"));
  assert.ok(androidUiSmokeScript.includes("generation-contract-demo-no-live-worker"));
  assert.ok(androidUiSmokeScript.includes("function Stop-PublicFantasyPetApi"));
  assert.ok(androidUiSmokeScript.includes("Get-NetTCPConnection"));
  assert.ok(androidUiSmokeScript.includes("MaxAttempts = 5"));
  assert.ok(androidUiSmokeAssertions.includes("generation-contract-demo-notice"));
  assert.ok(androidUiSmokeScript.includes("installDebug"));
  assert.ok(androidUiSmokeScript.includes("adb"));
  assert.ok(androidUiSmokeScript.includes("screencap"));
  assert.ok(androidUiSmokeScript.includes("pull"));
  assert.match(androidUiSmokeScript, /"pm"[\s\S]*"clear"[\s\S]*"com\.gamer\.community"/);
  assert.match(
    androidUiSmokeScript,
    /"am"[\s\S]*"start"[\s\S]*"-n"[\s\S]*"com\.gamer\.community\/\.MainActivity"/,
  );
  assert.doesNotMatch(androidUiSmokeScript, /Invoke-AndroidTapTextFragment\s+-TextFragment/);
  assert.doesNotMatch(androidUiSmokeScript, /\/admin|server-worker-cycle|agent-outputs|targetOutput/);
});

test("fantasy pet PowerShell verification scripts parse before runtime", { skip: resolvePowerShell() === "" ? "PowerShell is not available in this test environment" : false }, () => {
  const scripts = [verifyFantasyPetIntegrationPath, androidUiSmokePath];
  const powershellCommand = resolvePowerShell();
  const powershell = `
$ErrorActionPreference = "Stop"
$paths = @(${scripts.map(quotePowerShellLiteral).join(", ")})
foreach ($path in $paths) {
  $tokens = $null
  $errors = $null
  [System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$errors) | Out-Null
  if ($errors.Count -gt 0) {
    throw (($errors | ForEach-Object { "$($path): $($_.Message)" }) -join [Environment]::NewLine)
  }
}
`;

  const result = spawnSync(
    powershellCommand,
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", powershell],
    { encoding: "utf8" },
  );

  assert.equal(result.status, 0, result.stderr);
});
