import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const schema = "desktop-pet.ops.user-hooks-state.v1";

const trimString = (value) => (typeof value === "string" ? value.trim() : "");

const isEnabled = (value) => /^(1|true|yes|on)$/iu.test(trimString(value));

const isDisabled = (value) => /^(0|false|no|off)$/iu.test(trimString(value));

const parsePositiveInt = (value, fallback, max) => {
  const parsed = Number.parseInt(trimString(value), 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }
  return Math.min(parsed, max);
};

export function resolvePrivateOpsUserHooksConfig(env = process.env, repoRoot = process.cwd()) {
  const logDir = trimString(env.PRIVATE_OPS_LOG_DIR) || path.join(repoRoot, ".private-ops", "logs");
  const hooksMode = trimString(env.PRIVATE_OPS_HOOKS_MODE).toLowerCase();
  const explicitEnabled = trimString(env.PRIVATE_OPS_USER_HOOKS_ENABLED);
  const enabled = explicitEnabled ? isEnabled(explicitEnabled) : hooksMode === "user";
  const intervalMs = parsePositiveInt(
    env.PRIVATE_OPS_USER_HOOKS_INTERVAL_MS,
    5 * 60 * 1000,
    5 * 60 * 1000
  );

  return {
    enabled: enabled && !isDisabled(explicitEnabled),
    intervalMs,
    initialDelayMs: parsePositiveInt(
      env.PRIVATE_OPS_USER_HOOKS_INITIAL_DELAY_MS,
      30 * 1000,
      intervalMs
    ),
    rotate: Math.max(14, parsePositiveInt(env.PRIVATE_OPS_USER_HOOKS_ROTATE, 14, 365)),
    maxBytes: parsePositiveInt(
      env.PRIVATE_OPS_USER_HOOKS_MAX_LOG_BYTES,
      1024 * 1024,
      50 * 1024 * 1024
    ),
    logDir,
    smokeLogFile:
      trimString(env.PRIVATE_OPS_SMOKE_LOG_FILE) ||
      path.join(logDir, "private-ops-smoke.log"),
    stateFile:
      trimString(env.PRIVATE_OPS_USER_HOOKS_STATE_FILE) ||
      path.join(logDir, "private-ops-user-hooks.json"),
    runOnStart: !isDisabled(env.PRIVATE_OPS_USER_HOOKS_RUN_ON_START)
  };
}

export function createPrivateOpsUserHooks(options = {}) {
  const env = options.env ?? process.env;
  const repoRoot = options.repoRoot ?? process.cwd();
  const config = options.config ?? resolvePrivateOpsUserHooksConfig(env, repoRoot);
  const spawnImpl = options.spawnImpl ?? spawn;
  const now = options.now ?? (() => new Date());
  const setIntervalImpl = options.setIntervalImpl ?? setInterval;
  const clearIntervalImpl = options.clearIntervalImpl ?? clearInterval;
  const setTimeoutImpl = options.setTimeoutImpl ?? setTimeout;
  const clearTimeoutImpl = options.clearTimeoutImpl ?? clearTimeout;
  let intervalTimer = null;
  let startupTimer = null;
  let running = false;

  const ensureLogDir = () => {
    fs.mkdirSync(config.logDir, { recursive: true });
  };

  const writeState = () => {
    ensureLogDir();
    fs.writeFileSync(
      config.stateFile,
      `${JSON.stringify(
        {
          schema,
          enabled: config.enabled,
          updatedAt: now().toISOString(),
          smokeLogConfigured: true,
          scheduler: {
            mode: "user-process",
            intervalMs: config.intervalMs,
            initialDelayMs: config.initialDelayMs,
            smokeCommand: "node tools/private-ops-smoke.js"
          },
          logRotation: {
            rotate: config.rotate,
            maxBytes: config.maxBytes,
            compress: false
          }
        },
        null,
        2
      )}\n`
    );
  };

  const rotateLogIfNeeded = () => {
    if (!fs.existsSync(config.smokeLogFile)) {
      return;
    }
    const stat = fs.statSync(config.smokeLogFile);
    if (!stat.isFile() || stat.size < config.maxBytes) {
      return;
    }

    const lastRotated = `${config.smokeLogFile}.${config.rotate}`;
    if (fs.existsSync(lastRotated)) {
      fs.rmSync(lastRotated, { force: true });
    }
    for (let index = config.rotate - 1; index >= 1; index -= 1) {
      const source = `${config.smokeLogFile}.${index}`;
      const target = `${config.smokeLogFile}.${index + 1}`;
      if (fs.existsSync(source)) {
        fs.renameSync(source, target);
      }
    }
    fs.renameSync(config.smokeLogFile, `${config.smokeLogFile}.1`);
  };

  const appendLog = (text) => {
    if (!text) {
      return;
    }
    fs.appendFileSync(config.smokeLogFile, text.endsWith("\n") ? text : `${text}\n`);
  };

  const runOnce = () =>
    new Promise((resolve) => {
      if (!config.enabled || running) {
        resolve({ skipped: true });
        return;
      }

      running = true;
      ensureLogDir();
      writeState();
      rotateLogIfNeeded();

      const child = spawnImpl(process.execPath, [path.join(repoRoot, "tools", "private-ops-smoke.js")], {
        cwd: repoRoot,
        env,
        stdio: ["ignore", "pipe", "pipe"]
      });
      let stdout = "";
      let stderr = "";

      child.stdout?.setEncoding?.("utf8");
      child.stderr?.setEncoding?.("utf8");
      child.stdout?.on?.("data", (chunk) => {
        stdout += chunk;
      });
      child.stderr?.on?.("data", (chunk) => {
        stderr += chunk;
      });
      child.on("error", (error) => {
        appendLog(
          JSON.stringify({
            ok: false,
            at: now().toISOString(),
            error: "private_ops_smoke_spawn_failed",
            message: String(error?.message ?? "unknown")
          })
        );
        running = false;
        resolve({ ok: false });
      });
      child.on("close", (exitCode) => {
        appendLog(stdout);
        appendLog(stderr);
        if (exitCode !== 0 && !stdout && !stderr) {
          appendLog(
            JSON.stringify({
              ok: false,
              at: now().toISOString(),
              error: "private_ops_smoke_failed",
              exitCode
            })
          );
        }
        running = false;
        resolve({ ok: exitCode === 0, exitCode });
      });
    });

  return {
    runOnce,
    start() {
      if (!config.enabled || intervalTimer) {
        return;
      }
      writeState();
      if (config.runOnStart) {
        startupTimer = setTimeoutImpl(() => {
          void runOnce();
        }, config.initialDelayMs);
        startupTimer?.unref?.();
      }
      intervalTimer = setIntervalImpl(() => {
        void runOnce();
      }, config.intervalMs);
      intervalTimer?.unref?.();
    },
    stop() {
      if (startupTimer) {
        clearTimeoutImpl(startupTimer);
        startupTimer = null;
      }
      if (intervalTimer) {
        clearIntervalImpl(intervalTimer);
        intervalTimer = null;
      }
    },
    getState() {
      return {
        schema,
        enabled: config.enabled,
        scheduler: {
          mode: "user-process",
          intervalMs: config.intervalMs,
          initialDelayMs: config.initialDelayMs,
          smokeCommand: "node tools/private-ops-smoke.js"
        },
        logRotation: {
          rotate: config.rotate,
          maxBytes: config.maxBytes,
          compress: false
        }
      };
    }
  };
}

export function startPrivateOpsUserHooks(options = {}) {
  const hooks = createPrivateOpsUserHooks(options);
  hooks.start();
  return hooks;
}

const isDirectRun =
  process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  startPrivateOpsUserHooks({
    repoRoot: path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..")
  });
}
