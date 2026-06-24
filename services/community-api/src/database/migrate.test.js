import assert from "node:assert/strict";
import test from "node:test";
import { listCommunityMigrations } from "./migrations.js";
import { createPgClientOptions, runMigrationCli } from "./migrate.js";

class FakeCliClient {
  constructor({ appliedIds = [] } = {}) {
    this.appliedIds = appliedIds;
    this.queries = [];
    this.connected = false;
    this.ended = false;
  }

  async connect() {
    this.connected = true;
  }

  async end() {
    this.ended = true;
  }

  async query(sql, params = []) {
    this.queries.push({ sql, params });
    if (/select id from schema_migrations/iu.test(sql)) {
      return {
        rows: this.appliedIds.map((id) => ({ id }))
      };
    }
    return { rows: [] };
  }
}

test("migration CLI fails safely when DATABASE_URL is blank", async () => {
  const stderr = [];
  const exitCode = await runMigrationCli({
    env: { DATABASE_URL: "" },
    argv: [],
    stderr: { write: (text) => stderr.push(text) },
    stdout: { write: () => {} },
    createClient: () => {
      throw new Error("client should not be created");
    }
  });

  assert.equal(exitCode, 1);
  assert.match(stderr.join(""), /DATABASE_URL is required/);
});

test("migration CLI supports dry-run with injected client", async () => {
  const stdout = [];
  const migrations = listCommunityMigrations();
  const client = new FakeCliClient({
    appliedIds: [migrations[0].id]
  });

  const exitCode = await runMigrationCli({
    env: {
      DATABASE_URL: "postgres://avnadmin:example@example.aivencloud.com:13040/pgbouncer?sslmode=require"
    },
    argv: ["--dry-run"],
    stdout: { write: (text) => stdout.push(text) },
    stderr: { write: () => {} },
    createClient: () => client
  });

  assert.equal(exitCode, 0);
  assert.equal(client.connected, true);
  assert.equal(client.ended, true);
  assert.match(stdout.join(""), /dryRun=true/);
  assert.match(stdout.join(""), new RegExp(`pending=${migrations.length - 1}\\b`));
});

test("migration CLI applies migrations and closes client", async () => {
  const stdout = [];
  const migrations = listCommunityMigrations();
  const client = new FakeCliClient();

  const exitCode = await runMigrationCli({
    env: {
      DATABASE_URL: "postgresql://avnadmin:example@example.aivencloud.com:13040/defaultdb?sslmode=require"
    },
    argv: [],
    stdout: { write: (text) => stdout.push(text) },
    stderr: { write: () => {} },
    createClient: () => client
  });

  assert.equal(exitCode, 0);
  assert.equal(client.connected, true);
  assert.equal(client.ended, true);
  assert.match(stdout.join(""), /dryRun=false/);
  assert.match(stdout.join(""), new RegExp(`applied=${migrations.length}\\b`));
  assert.ok(client.queries.some((query) => query.sql === "BEGIN"));
});

test("pg client options move Aiven CA verification out of the connection string", () => {
  const options = createPgClientOptions(
    {
      databaseUrl: "postgres://avnadmin:example@example.aivencloud.com:13040/pgbouncer?sslmode=verify-ca",
      caCertPath: "/run/secrets/aiven-ca.pem"
    },
    {
      readFile: (path) => `ca from ${path}`
    }
  );

  assert.equal(options.connectionString.includes("sslmode"), false);
  assert.deepEqual(options.ssl, {
    ca: "ca from /run/secrets/aiven-ca.pem"
  });
});
