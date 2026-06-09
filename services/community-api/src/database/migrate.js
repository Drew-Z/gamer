import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createDatabaseConfig } from "./config.js";
import { listCommunityMigrations } from "./migrations.js";
import { runCommunityMigrations } from "./runner.js";

export function createPgClientOptions(config, { readFile = readFileSync } = {}) {
  const clientConfig = {
    connectionString: config.databaseUrl
  };

  if (config.caCertPath !== "") {
    const url = new URL(config.databaseUrl);
    url.searchParams.delete("sslmode");
    clientConfig.connectionString = url.toString();
    clientConfig.ssl = {
      ca: readFile(config.caCertPath, "utf8")
    };
  }

  return clientConfig;
}

async function createPgClient(config) {
  let pg;
  try {
    pg = await import("pg");
  } catch {
    throw new Error("The pg package is required to run database migrations.");
  }

  return new pg.Client(createPgClientOptions(config));
}

const writeLine = (stream, text) => {
  stream.write(`${text}\n`);
};

export async function runMigrationCli({
  env = process.env,
  argv = process.argv.slice(2),
  stdout = process.stdout,
  stderr = process.stderr,
  createClient = createPgClient
} = {}) {
  let config;
  try {
    config = createDatabaseConfig(env);
  } catch (error) {
    writeLine(stderr, error instanceof Error ? error.message : "Invalid database config");
    return 1;
  }

  if (config.mode !== "postgres") {
    writeLine(stderr, "DATABASE_URL is required to run community database migrations.");
    return 1;
  }

  const dryRun = argv.includes("--dry-run");
  const client = await createClient(config);

  try {
    await client.connect();
    const result = await runCommunityMigrations({
      client,
      migrations: listCommunityMigrations(),
      dryRun
    });
    writeLine(
      stdout,
      [
        "community migrations",
        `dryRun=${result.dryRun}`,
        `pending=${result.pending.length}`,
        `applied=${result.applied.length}`,
        `skipped=${result.skipped.length}`
      ].join(" ")
    );
    return 0;
  } catch (error) {
    writeLine(stderr, error instanceof Error ? error.message : "Migration failed");
    return 1;
  } finally {
    await client.end();
  }
}

const isDirectRun =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  const exitCode = await runMigrationCli();
  process.exitCode = exitCode;
}
