import assert from "node:assert/strict";
import test from "node:test";
import { createDatabaseConfig } from "./config.js";

test("database config stays in memory mode when DATABASE_URL is blank", () => {
  assert.deepEqual(createDatabaseConfig({ DATABASE_URL: "" }), {
    mode: "memory",
    databaseUrl: "",
    sslMode: "",
    caCertPath: ""
  });
});

test("database config accepts Aiven PostgreSQL URLs with SSL metadata", () => {
  const config = createDatabaseConfig({
    DATABASE_URL: "postgres://avnadmin:example@example.aivencloud.com:13040/pgbouncer?sslmode=require",
    AIVEN_CA_CERT_PATH: "/run/secrets/aiven-ca.pem"
  });

  assert.equal(config.mode, "postgres");
  assert.equal(config.sslMode, "require");
  assert.equal(config.caCertPath, "/run/secrets/aiven-ca.pem");
});

test("database config accepts postgresql URL scheme", () => {
  const config = createDatabaseConfig({
    DATABASE_URL: "postgresql://avnadmin:example@example.aivencloud.com:13040/defaultdb?sslmode=verify-ca"
  });

  assert.equal(config.mode, "postgres");
  assert.equal(config.sslMode, "verify-ca");
});

test("explicit SSL mode overrides provider URL defaults", () => {
  const config = createDatabaseConfig({
    DATABASE_URL: "postgresql://example.invalid/community?sslmode=require",
    POSTGRES_SSLMODE: "verify-full"
  });

  assert.equal(config.sslMode, "verify-full");
});

test("database config infers SSL for Supabase pooler hosts", () => {
  const config = createDatabaseConfig({
    DATABASE_URL: "postgresql://postgres.example:example@aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres"
  });

  assert.equal(config.mode, "postgres");
  assert.equal(config.sslMode, "require");
});

test("database config rejects non-PostgreSQL URLs", () => {
  assert.throws(
    () => createDatabaseConfig({ DATABASE_URL: "mysql://example" }),
    /DATABASE_URL must use postgres/
  );
});

test("database config rejects unknown SSL modes instead of disabling TLS", () => {
  assert.throws(
    () =>
      createDatabaseConfig({
        DATABASE_URL: "postgresql://example.invalid/community?sslmode=require",
        POSTGRES_SSLMODE: "verify-fll"
      }),
    /POSTGRES_SSLMODE must be/u
  );
});
