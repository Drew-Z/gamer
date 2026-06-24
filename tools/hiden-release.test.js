import assert from "node:assert/strict";
import test from "node:test";
import { syncHidenRelease } from "./hiden-release.js";

test("hiden release sync pulls main when auto update is enabled", () => {
  const commands = [];

  syncHidenRelease({
    env: {
      AUTO_UPDATE: "1"
    },
    repoRoot: "/repo",
    existsSync: () => true,
    execFileSync(command, args, options) {
      commands.push({ command, args, cwd: options.cwd });
    },
    logger: {
      log() {},
      error() {}
    }
  });

  assert.deepEqual(commands, [
    {
      command: "git",
      args: ["pull", "--ff-only", "origin", "main"],
      cwd: "/repo"
    }
  ]);
});

test("hiden release sync checks out requested release ref instead of pulling main", () => {
  const commands = [];
  const logs = [];

  syncHidenRelease({
    env: {
      AUTO_UPDATE: "1",
      GAMER_RELEASE_REF: "private-ops-v0.16"
    },
    repoRoot: "/repo",
    existsSync: () => true,
    execFileSync(command, args, options) {
      commands.push({ command, args, cwd: options.cwd });
    },
    logger: {
      log(message) {
        logs.push(message);
      },
      error() {}
    }
  });

  assert.deepEqual(commands, [
    {
      command: "git",
      args: ["fetch", "--tags", "origin", "private-ops-v0.16"],
      cwd: "/repo"
    },
    {
      command: "git",
      args: ["checkout", "--force", "FETCH_HEAD"],
      cwd: "/repo"
    }
  ]);
  assert.deepEqual(logs, ["gamer release ref requested: private-ops-v0.16"]);
});

test("hiden release sync rejects unsafe requested release refs before git", () => {
  const commands = [];

  assert.throws(
    () =>
      syncHidenRelease({
        env: {
          GAMER_RELEASE_REF: "--upload-pack=bad"
        },
        repoRoot: "/repo",
        existsSync: () => true,
        execFileSync(command, args) {
          commands.push({ command, args });
        },
        logger: {
          log() {},
          error() {}
        }
      }),
    /invalid GAMER_RELEASE_REF/
  );
  assert.deepEqual(commands, []);
});

test("hiden release sync skips when git metadata is absent", () => {
  const commands = [];

  syncHidenRelease({
    env: {
      AUTO_UPDATE: "1",
      GAMER_RELEASE_REF: "private-ops-v0.16"
    },
    repoRoot: "/repo",
    existsSync: () => false,
    execFileSync(command, args) {
      commands.push({ command, args });
    },
    logger: {
      log() {},
      error() {}
    }
  });

  assert.deepEqual(commands, []);
});
