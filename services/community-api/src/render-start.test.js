import assert from "node:assert/strict";
import test from "node:test";
import { bootRenderCommunityApi } from "./render-start.js";

const writer = (lines) => ({
  write(value) {
    lines.push(String(value));
  }
});

test("Render startup refuses the ephemeral fallback when DATABASE_URL is missing", async () => {
  const errors = [];
  let migrationCalls = 0;
  let serverCalls = 0;

  const result = await bootRenderCommunityApi({
    env: {},
    stderr: writer(errors),
    runMigrations: async () => {
      migrationCalls += 1;
      return 0;
    },
    startServer: () => {
      serverCalls += 1;
    }
  });

  assert.equal(result.exitCode, 1);
  assert.equal(result.server, null);
  assert.equal(migrationCalls, 0);
  assert.equal(serverCalls, 0);
  assert.match(errors.join(""), /DATABASE_URL is required/u);
});

test("Render startup refuses to disable Community API authentication", async () => {
  const errors = [];
  let migrationCalls = 0;
  let serverCalls = 0;

  const result = await bootRenderCommunityApi({
    env: { DATABASE_URL: "postgresql://example.invalid/community" },
    stderr: writer(errors),
    runMigrations: async () => {
      migrationCalls += 1;
      return 0;
    },
    startServer: () => {
      serverCalls += 1;
    }
  });

  assert.equal(result.exitCode, 1);
  assert.equal(result.server, null);
  assert.equal(migrationCalls, 0);
  assert.equal(serverCalls, 0);
  assert.match(errors.join(""), /COMMUNITY_DEMO_TOKEN is required/u);
});

test("Render startup does not listen when migrations fail", async () => {
  let serverCalls = 0;
  const result = await bootRenderCommunityApi({
    env: {
      DATABASE_URL: "postgresql://example.invalid/community",
      COMMUNITY_DEMO_TOKEN: "test-community-token"
    },
    runMigrations: async () => 1,
    startServer: () => {
      serverCalls += 1;
    }
  });

  assert.equal(result.exitCode, 1);
  assert.equal(result.server, null);
  assert.equal(serverCalls, 0);
});

test("Render startup runs migrations before starting the HTTP server", async () => {
  const calls = [];
  const fakeServer = { close() {} };
  const env = {
    DATABASE_URL: "postgresql://example.invalid/community",
    COMMUNITY_DEMO_TOKEN: "test-community-token"
  };

  const result = await bootRenderCommunityApi({
    env,
    runMigrations: async (options) => {
      calls.push(["migrate", options.env]);
      return 0;
    },
    startServer: (options) => {
      calls.push(["listen", options.env]);
      return fakeServer;
    }
  });

  assert.deepEqual(calls, [
    ["migrate", env],
    ["listen", env]
  ]);
  assert.equal(result.exitCode, 0);
  assert.equal(result.server, fakeServer);
});
