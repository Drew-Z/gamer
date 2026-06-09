import assert from "node:assert/strict";
import test from "node:test";
import {
  listCommunityMigrations,
  readInitialCommunitySchema,
  requiredCommunityTables
} from "./migrations.js";

test("initial community schema covers public app and admin review data", () => {
  const sql = readInitialCommunitySchema();

  for (const tableName of requiredCommunityTables) {
    assert.match(sql, new RegExp(`create table if not exists ${tableName}\\b`, "i"));
  }

  assert.match(sql, /submissions_status_check/i);
  assert.match(sql, /pending/i);
  assert.match(sql, /approved/i);
  assert.match(sql, /revoked/i);
  assert.match(sql, /r2_bucket/i);
  assert.match(sql, /r2_key/i);
  assert.match(sql, /download_id/i);
  assert.match(sql, /create table if not exists schema_migrations\b/i);
  assert.match(sql, /applied_at timestamptz/i);
  assert.doesNotMatch(sql, /file:\/\//i);
  assert.doesNotMatch(sql, /server-worker-cycle|agent-outputs|targetoutput/i);
});

test("migration loader returns ordered migrations with stable ids", () => {
  const migrations = listCommunityMigrations();

  assert.ok(migrations.length >= 1);
  assert.equal(migrations[0].id, "001_initial_community_schema");
  assert.equal(migrations[0].filename, "001_initial_community_schema.sql");
  assert.ok(migrations[0].sql.includes("create table if not exists users"));
});
