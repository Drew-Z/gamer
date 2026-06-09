import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../..",
);
const helperPath = path.join(
  repoRoot,
  "tools",
  "fantasy-pet-android-ui-smoke-assertions.ps1",
);
const launcherPath = path.join(
  repoRoot,
  "tools",
  "launch-fantasy-pet-android-ui-smoke.ps1",
);

function quotePowerShellLiteral(value) {
  return `'${value.replaceAll("'", "''")}'`;
}

function runContractDemoAssertion(uiXml) {
  const script = `
$ErrorActionPreference = "Stop"
. ${quotePowerShellLiteral(helperPath)}
$uiXml = ${quotePowerShellLiteral(uiXml)}
$result = Assert-ContractDemoAndroidUiState -UiXml $uiXml
$result | ConvertTo-Json -Depth 4
`;

  return spawnSync(
    "powershell.exe",
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script],
    { encoding: "utf8" },
  );
}

function runPowerShellHelper(scriptBody) {
  const script = `
$ErrorActionPreference = "Stop"
. ${quotePowerShellLiteral(helperPath)}
${scriptBody}
`;

  return spawnSync(
    "powershell.exe",
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script],
    { encoding: "utf8" },
  );
}

test("Android UI smoke assertion accepts contract demo warning with disabled review and download", () => {
  const result = runContractDemoAssertion(`
<hierarchy>
  <node text="" enabled="true" content-desc="generation-public-api-boundary-notice" bounds="[0,0][500,40]" />
  <node text="" enabled="true" content-desc="generation-contract-demo-notice" bounds="[0,0][100,40]" />
  <node text="" enabled="true" content-desc="generation-contract-demo-no-live-worker" bounds="[0,50][500,90]" />
  <node enabled="false" content-desc="" bounds="[79,1697][373,1823]">
    <node enabled="true" content-desc="generation-review-accept-button" bounds="[79,1707][373,1812]" />
    <node text="Accept" enabled="true" content-desc="" bounds="[120,1720][180,1780]" />
  </node>
  <node enabled="false" content-desc="" bounds="[79,1917][1001,2043]">
    <node enabled="true" content-desc="generation-package-download-button" bounds="[79,1927][1001,2032]" />
    <node text="Download pet.zip" enabled="true" content-desc="" bounds="[200,1940][360,2000]" />
  </node>
</hierarchy>`);

  assert.equal(result.status, 0, result.stderr);
  const decoded = JSON.parse(result.stdout);
  assert.equal(decoded.passed, true);
  assert.equal(decoded.publicApiBoundaryNoticeVisible, true);
  assert.equal(decoded.contractDemoWarningVisible, true);
  assert.equal(decoded.contractDemoNoLiveWorkerVisible, true);
  assert.equal(decoded.reviewAcceptDisabled, true);
  assert.equal(decoded.packageDownloadDisabled, true);
});

test("Android UI smoke helper extracts hierarchy XML from uiautomator stdout noise", () => {
  const rawDump = `UI hierchary dumped to: /dev/tty
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<hierarchy rotation="0">
  <node text="进入应用" enabled="true" content-desc="launch-bubble-enter" bounds="[315,1270][765,1396]" />
</hierarchy>
`;
  const result = runPowerShellHelper(`
$rawDump = ${quotePowerShellLiteral(rawDump)}
ConvertTo-AndroidUiHierarchyXml -RawDumpLines @($rawDump)
`);

  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /^<\?xml/);
  assert.match(result.stdout, /content-desc="launch-bubble-enter"/);
  assert.match(result.stdout, /<\/hierarchy>/);
});

test("Android UI smoke helper rejects dumps without a hierarchy XML payload", () => {
  const result = runPowerShellHelper(`
ConvertTo-AndroidUiHierarchyXml -RawDumpLines @("UI hierchary dumped to: /dev/tty")
`);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /hierarchy XML payload/);
});

test("Android UI smoke launcher keeps an on-device UI dump fallback", () => {
  const source = fs.readFileSync(launcherPath, "utf8");

  assert.match(source, /\/sdcard\/window\.xml/);
  assert.match(source, /uiautomator/);
  assert.match(source, /cat/);
});

test("Android UI smoke launcher waits for post-tap navigation state", () => {
  const source = fs.readFileSync(launcherPath, "utf8");

  assert.match(source, /function Wait-AndroidUiContentDescription/);
  assert.match(source, /function Invoke-AndroidTapContentDescriptionUntil/);
  assert.match(source, /function Dismiss-AndroidKeyboardIfVisible/);
  assert.match(source, /function Dismiss-AndroidKeyboardBestEffort/);
  assert.match(source, /function Test-AndroidKeyboardLikelyVisible/);
  assert.match(source, /function Test-AndroidUiHasFocusedTextInput/);
  assert.match(source, /function Test-AndroidInputMethodWindowVisible/);
  assert.match(source, /function Get-AndroidUiMaximumBottom/);
  assert.match(source, /function Get-AndroidDisplayWidth/);
  assert.match(source, /function Enter-AndroidText/);
  assert.match(source, /function Enter-AndroidTextCharacters/);
  assert.match(source, /function Set-AndroidClipboardText/);
  assert.match(source, /function Set-AndroidGenerationJobPreference/);
  assert.match(source, /\[string\]\$AppJobId = "publicdemo1"/);
  assert.match(source, /fantasy-pet-generation\.xml/);
  assert.match(source, /shared_prefs/);
  assert.match(source, /run-as[\s\S]*com\.gamer\.community/);
  assert.match(source, /Set-AndroidGenerationJobPreference -AppJobId \$AppJobId/);
  assert.match(source, /cmd[\s\S]*clipboard[\s\S]*set/);
  assert.match(source, /-not \$Text\.Contains\("-"\)/);
  assert.match(source, /\) 2>&1/);
  assert.match(source, /No shell command implementation/);
  assert.match(source, /keyevent[\s\S]*"279"/);
  assert.match(source, /keyevent[\s\S]*"69"/);
  assert.match(source, /Enter-AndroidTextCharacters -Name \$Name -Text \$Text/);
  assert.match(source, /Start-Sleep -Milliseconds 60/);
  assert.match(source, /input[\s\S]*keyevent[\s\S]*"111"/);
  assert.match(source, /input[\s\S]*keyevent[\s\S]*"66"/);
  assert.match(source, /Tap Android keyboard action key/);
  assert.match(source, /Dismiss Android keyboard with Back/);
  assert.match(
    source,
    /function Invoke-AndroidTapContentDescriptionUntil[\s\S]*Test-AndroidUiHasContentDescription[\s\S]*TargetContentDescription/,
  );
  assert.match(
    source,
    /function Invoke-AndroidTapContentDescriptionUntil[\s\S]*Test-AndroidUiHasContentDescription[\s\S]*ContentDescription/,
  );
  assert.match(
    source,
    /Invoke-AndroidTapContentDescriptionUntil `\s+-ContentDescription "launch-bubble-enter" `\s+-TargetContentDescription "gamer-tab-generate"/,
  );
  assert.match(
    source,
    /Invoke-AndroidTapCenter -Name "Select contract demo candidate"[\s\S]*Dismiss-AndroidKeyboardBestEffort/,
  );
});

test("Android UI smoke launcher captures contract demo warning snapshot explicitly", () => {
  const source = fs.readFileSync(launcherPath, "utf8");

  assert.match(
    source,
    /Find-AndroidUiWithContentDescription `\s+-ContentDescription "generation-contract-demo-notice"/,
  );
});

test("Android UI smoke launcher resets to the top before contract snapshots", () => {
  const source = fs.readFileSync(launcherPath, "utf8");

  assert.match(source, /function Invoke-AndroidScrollToTop/);
  assert.match(
    source,
    /Invoke-AndroidScrollToTop[\s\S]*Find-AndroidUiWithContentDescription `\s+-ContentDescription "generation-public-api-boundary-notice"/,
  );
});

test("Android UI smoke assertion rejects contract demo UI without public API boundary notice", () => {
  const result = runContractDemoAssertion(`
<hierarchy>
  <node text="" enabled="true" content-desc="generation-contract-demo-notice" bounds="[0,0][100,40]" />
  <node text="" enabled="true" content-desc="generation-contract-demo-no-live-worker" bounds="[0,50][500,90]" />
  <node enabled="false" content-desc="" bounds="[79,1697][373,1823]">
    <node enabled="true" content-desc="generation-review-accept-button" bounds="[79,1707][373,1812]" />
    <node text="Accept" enabled="true" content-desc="" bounds="[120,1720][180,1780]" />
  </node>
  <node enabled="false" content-desc="" bounds="[79,1917][1001,2043]">
    <node enabled="true" content-desc="generation-package-download-button" bounds="[79,1927][1001,2032]" />
    <node text="Download pet.zip" enabled="true" content-desc="" bounds="[200,1940][360,2000]" />
  </node>
</hierarchy>`);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /public API boundary/);
});

test("Android UI smoke assertion rejects contract demo UI without no-live-worker copy", () => {
  const result = runContractDemoAssertion(`
<hierarchy>
  <node text="" enabled="true" content-desc="generation-public-api-boundary-notice" bounds="[0,0][500,40]" />
  <node text="Contract demo task: this candidate is pre-seeded for public API validation; it is not a live pet generation run." enabled="true" content-desc="" bounds="[0,0][100,40]" />
  <node enabled="false" content-desc="" bounds="[79,1697][373,1823]">
    <node enabled="true" content-desc="generation-review-accept-button" bounds="[79,1707][373,1812]" />
    <node text="Accept" enabled="true" content-desc="" bounds="[120,1720][180,1780]" />
  </node>
  <node enabled="false" content-desc="" bounds="[79,1917][1001,2043]">
    <node enabled="true" content-desc="generation-package-download-button" bounds="[79,1927][1001,2032]" />
    <node text="Download pet.zip" enabled="true" content-desc="" bounds="[200,1940][360,2000]" />
  </node>
</hierarchy>`);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /no live generation worker/);
});

test("Android UI smoke assertion rejects enabled accept button for contract demo job", () => {
  const result = runContractDemoAssertion(`
<hierarchy>
  <node text="" enabled="true" content-desc="generation-public-api-boundary-notice" bounds="[0,0][500,40]" />
  <node text="Contract demo task: this candidate is pre-seeded for public API validation; it is not a live pet generation run." enabled="true" content-desc="" bounds="[0,0][100,40]" />
  <node text="" enabled="true" content-desc="generation-contract-demo-no-live-worker" bounds="[0,50][500,90]" />
  <node enabled="true" content-desc="" bounds="[79,1697][373,1823]">
    <node enabled="true" content-desc="generation-review-accept-button" bounds="[79,1707][373,1812]" />
    <node text="Accept" enabled="true" content-desc="" bounds="[120,1720][180,1780]" />
  </node>
  <node enabled="false" content-desc="" bounds="[79,1917][1001,2043]">
    <node enabled="true" content-desc="generation-package-download-button" bounds="[79,1927][1001,2032]" />
    <node text="Download pet.zip" enabled="true" content-desc="" bounds="[200,1940][360,2000]" />
  </node>
</hierarchy>`);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /generation-review-accept-button/);
});

test("Android UI smoke helper returns centers for text and content description bounds", () => {
  const script = `
$ErrorActionPreference = "Stop"
. ${quotePowerShellLiteral(helperPath)}
$uiXml = '<hierarchy><node text="Tap bubble" enabled="true" content-desc="" bounds="[315,1270][765,1396]" /><node text="" enabled="true" content-desc="generation-poll-job-button" bounds="[79,1488][1001,1593]" /></hierarchy>'
$bubble = Get-AndroidUiCenterByTextFragment -UiXml $uiXml -TextFragment "Tap bubble"
$poll = Get-AndroidUiCenterByContentDescription -UiXml $uiXml -ContentDescription "generation-poll-job-button"
[pscustomobject]@{ bubble = $bubble; poll = $poll } | ConvertTo-Json -Depth 4
`;
  const result = spawnSync(
    "powershell.exe",
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script],
    { encoding: "utf8" },
  );

  assert.equal(result.status, 0, result.stderr);
  const decoded = JSON.parse(result.stdout);
  assert.deepEqual(decoded.bubble, { x: 540, y: 1333 });
  assert.deepEqual(decoded.poll, { x: 540, y: 1540 });
});
