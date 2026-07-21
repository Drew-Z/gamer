import path from "node:path";
import { fileURLToPath } from "node:url";
import { runMigrationCli } from "./database/migrate.js";
import { loadEnvFiles } from "./env-file.js";
import { startCommunityApiServer } from "./server.js";

const writeLine = (stream, text) => {
  stream.write(`${text}\n`);
};

export async function bootRenderCommunityApi({
  env = process.env,
  stdout = process.stdout,
  stderr = process.stderr,
  runMigrations = runMigrationCli,
  startServer = startCommunityApiServer
} = {}) {
  if (!String(env.DATABASE_URL ?? "").trim()) {
    writeLine(stderr, "DATABASE_URL is required for the Render Community API deployment.");
    return { exitCode: 1, server: null };
  }

  if (!String(env.COMMUNITY_DEMO_TOKEN ?? "").trim()) {
    writeLine(stderr, "COMMUNITY_DEMO_TOKEN is required for the Render Community API deployment.");
    return { exitCode: 1, server: null };
  }

  let migrationExitCode;
  try {
    migrationExitCode = await runMigrations({ env, stdout, stderr });
  } catch {
    writeLine(stderr, "Community database migration failed during startup.");
    return { exitCode: 1, server: null };
  }

  if (migrationExitCode !== 0) {
    return { exitCode: migrationExitCode, server: null };
  }

  return {
    exitCode: 0,
    server: startServer({ env })
  };
}

export function installGracefulShutdown(server, {
  processRef = process,
  timeoutMs = 25_000
} = {}) {
  let shuttingDown = false;

  const shutdown = () => {
    if (shuttingDown) {
      return;
    }
    shuttingDown = true;

    const timer = setTimeout(() => {
      server.closeAllConnections?.();
      processRef.exit(1);
    }, timeoutMs);
    timer.unref?.();

    server.close((error) => {
      clearTimeout(timer);
      processRef.exit(error ? 1 : 0);
    });
  };

  processRef.once("SIGINT", shutdown);
  processRef.once("SIGTERM", shutdown);
}

const isDirectRun =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  loadEnvFiles();
  const result = await bootRenderCommunityApi();
  if (result.exitCode !== 0) {
    process.exitCode = result.exitCode;
  } else {
    installGracefulShutdown(result.server);
  }
}
