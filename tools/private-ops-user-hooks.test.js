import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import {
  createPrivateOpsUserHooks,
  resolvePrivateOpsUserHooksConfig
} from "./private-ops-user-hooks.js";

test("private ops user hooks resolve hiden-safe defaults", () => {
  const repoRoot = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-user-hooks-"));
  const config = resolvePrivateOpsUserHooksConfig(
    {
      PRIVATE_OPS_HOOKS_MODE: "user"
    },
    repoRoot
  );

  assert.equal(config.enabled, true);
  assert.equal(config.intervalMs, 300000);
  assert.equal(config.rotate, 14);
  assert.equal(config.smokeLogFile, path.join(repoRoot, ".private-ops", "logs", "private-ops-smoke.log"));
  assert.equal(config.stateFile, path.join(repoRoot, ".private-ops", "logs", "private-ops-user-hooks.json"));
});

test("private ops user hooks write state and append smoke output", async () => {
  const repoRoot = fs.mkdtempSync(path.join(os.tmpdir(), "private-ops-user-hooks-"));
  const logDir = path.join(repoRoot, "logs");
  const smokeLogFile = path.join(logDir, "private-ops-smoke.log");
  const stateFile = path.join(logDir, "private-ops-user-hooks.json");
  const hooks = createPrivateOpsUserHooks({
    repoRoot,
    config: {
      enabled: true,
      intervalMs: 300000,
      initialDelayMs: 0,
      rotate: 14,
      maxBytes: 1024 * 1024,
      logDir,
      smokeLogFile,
      stateFile,
      runOnStart: false
    },
    spawnImpl: () => {
      const child = new EventEmitter();
      child.stdout = new EventEmitter();
      child.stderr = new EventEmitter();
      child.stdout.setEncoding = () => {};
      child.stderr.setEncoding = () => {};
      setImmediate(() => {
        child.stdout.emit(
          "data",
          `${JSON.stringify({
            ok: true,
            checks: [{ name: "community health is public-safe", status: "pass" }]
          })}\n`
        );
        child.emit("close", 0);
      });
      return child;
    }
  });

  const result = await hooks.runOnce();
  assert.equal(result.ok, true);

  const state = JSON.parse(fs.readFileSync(stateFile, "utf8"));
  assert.equal(state.schema, "desktop-pet.ops.user-hooks-state.v1");
  assert.equal(state.scheduler.mode, "user-process");
  assert.equal(state.logRotation.rotate, 14);
  assert.match(fs.readFileSync(smokeLogFile, "utf8"), /"ok":true/);
});
