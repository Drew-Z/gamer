#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const schema = "desktop-pet.ops.target-hooks-audit.v1";

const trimString = (value) => (typeof value === "string" ? value.trim() : "");

const isEnabled = (value) => /^(1|true|yes|on)$/iu.test(trimString(value));

const parsePositiveInt = (value, fallback, max) => {
  const parsed = Number.parseInt(trimString(value), 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }
  return Math.min(parsed, max);
};

const fileExists = (filePath) => {
  try {
    return fs.statSync(filePath).isFile();
  } catch {
    return false;
  }
};

const directoryExists = (directoryPath) => {
  try {
    return fs.statSync(directoryPath).isDirectory();
  } catch {
    return false;
  }
};

const readTextFile = (filePath, maxBytes = 1024 * 1024) => {
  const file = fs.openSync(filePath, "r");
  try {
    const stat = fs.fstatSync(file);
    const length = Math.min(stat.size, maxBytes);
    const offset = Math.max(0, stat.size - length);
    const buffer = Buffer.alloc(length);
    fs.readSync(file, buffer, 0, length, offset);
    return buffer.toString("utf8");
  } finally {
    fs.closeSync(file);
  }
};

const addCheck = (checks, name, ok, details = {}) => {
  checks.push({
    name,
    status: ok ? "pass" : "fail",
    ...details
  });
};

const redactPaths = (paths) => ({
  hooksMode: paths.hooksMode,
  cronConfigured: Boolean(paths.cronFile),
  logrotateConfigured: Boolean(paths.logrotateFile),
  userSchedulerConfigured: Boolean(paths.userHooksStateFile),
  logDirConfigured: Boolean(paths.logDir),
  smokeLogConfigured: Boolean(paths.smokeLogFile)
});

const resolvePaths = (env, cwd = process.cwd()) => {
  const hooksMode =
    trimString(env.PRIVATE_OPS_HOOKS_MODE).toLowerCase() === "user" ? "user" : "system";
  const logDir =
    trimString(env.PRIVATE_OPS_LOG_DIR) ||
    (hooksMode === "user" ? path.join(cwd, ".private-ops", "logs") : "/var/log/desktop-pet");
  return {
    hooksMode,
    cronFile:
      trimString(env.PRIVATE_OPS_CRON_FILE) ||
      "/opt/desktop-pet/gamer/deploy/private-ops-cron.example",
    logrotateFile:
      trimString(env.PRIVATE_OPS_LOGROTATE_FILE) ||
      "/etc/logrotate.d/desktop-pet-private-ops",
    userHooksStateFile:
      trimString(env.PRIVATE_OPS_USER_HOOKS_STATE_FILE) ||
      path.join(logDir, "private-ops-user-hooks.json"),
    logDir,
    smokeLogFile:
      trimString(env.PRIVATE_OPS_SMOKE_LOG_FILE) ||
      path.join(logDir, "private-ops-smoke.log")
  };
};

const auditCron = (checks, cronText) => {
  const lines = cronText
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith("#"));
  const hasFiveMinuteSchedule = lines.some((line) => /^\*\/5\s+\*\s+\*\s+\*\s+\*/u.test(line));
  const hasSmokeCommand = lines.some((line) => /npm(?:\.cmd)?\s+run\s+smoke:private-ops/u.test(line));
  const loadsPrivateEnv = lines.some((line) => /\.env\.private-ops|PRIVATE_OPS_ENV_FILE/u.test(line));
  const writesSmokeLog = lines.some((line) => /private-ops-smoke\.log/u.test(line));
  const redirectsStderr = lines.some((line) => /2>&1/u.test(line));

  addCheck(checks, "external scheduler has 5-minute cadence", hasFiveMinuteSchedule);
  addCheck(checks, "external scheduler runs private ops smoke", hasSmokeCommand);
  addCheck(checks, "external scheduler loads private env outside git", loadsPrivateEnv);
  addCheck(checks, "external scheduler writes smoke log", writesSmokeLog && redirectsStderr);
};

const auditLogrotate = (checks, logrotateText, logDir) => {
  const escapedLogDir = escapeRegExp(logDir.replace(/\/+$/u, ""));
  const rotateMatch = /\brotate\s+(\d+)\b/iu.exec(logrotateText);
  const rotateCount = rotateMatch ? Number.parseInt(rotateMatch[1], 10) : 0;

  addCheck(
    checks,
    "logrotate covers private ops log directory",
    new RegExp(`${escapedLogDir}/\\*\\.log`, "u").test(logrotateText)
  );
  addCheck(checks, "logrotate keeps at least 14 rotations", rotateCount >= 14, {
    rotateCount
  });
  addCheck(checks, "logrotate compresses old logs", /\bcompress\b/iu.test(logrotateText));
  addCheck(
    checks,
    "logrotate handles active log writers",
    /\bcopytruncate\b/iu.test(logrotateText) || /\bcreate\s+0?[0-7]{3}\b/iu.test(logrotateText)
  );
};

const auditUserHooks = (checks, stateText) => {
  let state;
  try {
    state = JSON.parse(stateText);
  } catch {
    addCheck(checks, "user-level hook state is valid json", false);
    return;
  }

  const intervalMs = Number.parseInt(String(state.scheduler?.intervalMs ?? ""), 10);
  const rotateCount = Number.parseInt(String(state.logRotation?.rotate ?? ""), 10);
  const maxBytes = Number.parseInt(String(state.logRotation?.maxBytes ?? ""), 10);
  const smokeCommand = trimString(state.scheduler?.smokeCommand);

  addCheck(
    checks,
    "user-level hook state uses expected schema",
    state.schema === "desktop-pet.ops.user-hooks-state.v1"
  );
  addCheck(checks, "user-level scheduler is enabled", state.enabled === true);
  addCheck(
    checks,
    "user-level scheduler has 5-minute cadence",
    Number.isFinite(intervalMs) && intervalMs > 0 && intervalMs <= 5 * 60 * 1000,
    { intervalMs: Number.isFinite(intervalMs) ? intervalMs : 0 }
  );
  addCheck(
    checks,
    "user-level scheduler runs private ops smoke",
    /npm(?:\.cmd)?\s+run\s+smoke:private-ops/u.test(smokeCommand) ||
      /private-ops-smoke\.js/u.test(smokeCommand)
  );
  addCheck(checks, "user-level scheduler writes smoke log", state.smokeLogConfigured === true);
  addCheck(
    checks,
    "user-level log rotation keeps at least 14 rotations",
    Number.isFinite(rotateCount) && rotateCount >= 14,
    { rotateCount: Number.isFinite(rotateCount) ? rotateCount : 0 }
  );
  addCheck(
    checks,
    "user-level log rotation has bounded file size",
    Number.isFinite(maxBytes) && maxBytes > 0,
    { maxBytes: Number.isFinite(maxBytes) ? maxBytes : 0 }
  );
};

const auditFreshSmokeLog = (checks, paths, env, now) => {
  const maxAgeMs = parsePositiveInt(
    env.PRIVATE_OPS_SMOKE_LOG_MAX_AGE_MS,
    15 * 60 * 1000,
    24 * 60 * 60 * 1000
  );
  if (!fileExists(paths.smokeLogFile)) {
    addCheck(checks, "fresh recurring smoke log exists", false);
    return;
  }

  const stat = fs.statSync(paths.smokeLogFile);
  const ageMs = Math.max(0, now.getTime() - stat.mtime.getTime());
  const smokeLogText = readTextFile(paths.smokeLogFile);
  const hasSuccessfulSmoke = /"ok"\s*:\s*true/u.test(smokeLogText);
  const hasChecks = /"checks"\s*:\s*\[/u.test(smokeLogText);

  addCheck(checks, "fresh recurring smoke log exists", stat.size > 0, {
    bytes: stat.size
  });
  addCheck(checks, "recurring smoke log is fresh", ageMs <= maxAgeMs, {
    ageMs,
    maxAgeMs
  });
  addCheck(checks, "recurring smoke log records successful smoke", hasSuccessfulSmoke && hasChecks);
  auditForbiddenFragments(checks, "recurring smoke log", smokeLogText, env);
};

const auditForbiddenFragments = (checks, label, text, env) => {
  const fragments = [
    env.COMMUNITY_DEMO_TOKEN,
    env.FANTASY_PET_UPSTREAM_TOKEN,
    env.FANTASY_PET_DEMO_TOKEN,
    env.PRIVATE_OPS_BASIC_AUTH_PASSWORD,
    env.DATABASE_URL,
    env.COMMUNITY_POSTGRES_PASSWORD,
    env.SUPABASE_SERVICE_ROLE_KEY
  ]
    .map(trimString)
    .filter((value) => value.length >= 8);
  const leaked = fragments.some((fragment) => text.includes(fragment));
  addCheck(checks, `${label} does not contain configured secret fragments`, !leaked);
};

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");

export function runPrivateOpsTargetHooksAudit(options = {}) {
  const env = options.env ?? process.env;
  const now = options.now ?? new Date();
  const cwd = options.cwd ?? process.cwd();
  const paths = resolvePaths(env, cwd);
  const checks = [];

  addCheck(checks, "log directory exists", directoryExists(paths.logDir));
  if (paths.hooksMode === "user") {
    addCheck(checks, "user-level hook state file exists", fileExists(paths.userHooksStateFile));
    if (fileExists(paths.userHooksStateFile)) {
      const stateText = readTextFile(paths.userHooksStateFile);
      auditUserHooks(checks, stateText);
      auditForbiddenFragments(checks, "user-level hook state", stateText, env);
    }
  } else {
    addCheck(checks, "external scheduler file exists", fileExists(paths.cronFile));
    if (fileExists(paths.cronFile)) {
      const cronText = readTextFile(paths.cronFile);
      auditCron(checks, cronText);
      auditForbiddenFragments(checks, "external scheduler file", cronText, env);
    }

    addCheck(checks, "logrotate config file exists", fileExists(paths.logrotateFile));
    if (fileExists(paths.logrotateFile)) {
      const logrotateText = readTextFile(paths.logrotateFile);
      auditLogrotate(checks, logrotateText, paths.logDir);
      auditForbiddenFragments(checks, "logrotate config", logrotateText, env);
    }
  }

  if (isEnabled(env.PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG)) {
    auditFreshSmokeLog(checks, paths, env, now);
  }

  const ok = checks.every((check) => check.status === "pass");
  return {
    schema,
    ok,
    auditedAt: now.toISOString(),
    targetHooks: redactPaths(paths),
    freshSmokeLogRequired: isEnabled(env.PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG),
    checks
  };
}

const main = () => {
  const output = runPrivateOpsTargetHooksAudit();
  const ok = output.ok;

  const rendered = `${JSON.stringify(output, null, 2)}\n`;
  if (ok) {
    process.stdout.write(rendered);
  } else {
    process.stderr.write(rendered);
  }
  process.exitCode = ok ? 0 : 1;
};

const isDirectRun =
  process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  main();
}
