import assert from "node:assert/strict";
import test from "node:test";
import {
  runCommunityMigrations,
  schemaMigrationsBootstrapSql
} from "./runner.js";

const migrations = [
  {
    id: "001_initial_community_schema",
    filename: "001_initial_community_schema.sql",
    sql: "create table if not exists users (id text primary key);"
  },
  {
    id: "002_add_assets",
    filename: "002_add_assets.sql",
    sql: "create table if not exists asset_objects (id text primary key);"
  }
];

class FakeMigrationClient {
  constructor({ appliedIds = [], failOnSql = "" } = {}) {
    this.appliedIds = appliedIds;
    this.failOnSql = failOnSql;
    this.queries = [];
  }

  async query(sql, params = []) {
    this.queries.push({ sql, params });
    if (this.failOnSql && sql.includes(this.failOnSql)) {
      throw new Error("migration failed");
    }
    if (/select id from schema_migrations/iu.test(sql)) {
      return {
        rows: this.appliedIds.map((id) => ({ id }))
      };
    }
    return { rows: [] };
  }
}

test("migration runner bootstraps table and applies pending migrations in transactions", async () => {
  const client = new FakeMigrationClient({
    appliedIds: ["001_initial_community_schema"]
  });

  const result = await runCommunityMigrations({ client, migrations });

  assert.deepEqual(result, {
    dryRun: false,
    applied: ["002_add_assets"],
    skipped: ["001_initial_community_schema"],
    pending: ["002_add_assets"]
  });
  assert.equal(client.queries[0].sql, schemaMigrationsBootstrapSql);
  assert.ok(client.queries.some((query) => query.sql === "BEGIN"));
  assert.ok(client.queries.some((query) => query.sql === migrations[1].sql));
  assert.ok(client.queries.some((query) => query.sql === "COMMIT"));
  assert.deepEqual(
    client.queries.find((query) => query.sql.includes("insert into schema_migrations"))?.params,
    ["002_add_assets"]
  );
});

test("migration runner dry-run reports pending migrations without applying SQL", async () => {
  const client = new FakeMigrationClient({
    appliedIds: ["001_initial_community_schema"]
  });

  const result = await runCommunityMigrations({
    client,
    migrations,
    dryRun: true
  });

  assert.deepEqual(result, {
    dryRun: true,
    applied: [],
    skipped: ["001_initial_community_schema"],
    pending: ["002_add_assets"]
  });
  assert.equal(client.queries.some((query) => query.sql === migrations[1].sql), false);
  assert.equal(client.queries.some((query) => query.sql === "BEGIN"), false);
});

test("migration runner rolls back failed migration", async () => {
  const client = new FakeMigrationClient({
    failOnSql: "users"
  });

  await assert.rejects(
    () => runCommunityMigrations({ client, migrations }),
    /migration failed/
  );

  assert.ok(client.queries.some((query) => query.sql === "BEGIN"));
  assert.ok(client.queries.some((query) => query.sql === "ROLLBACK"));
  assert.equal(client.queries.some((query) => query.sql === "COMMIT"), false);
});
