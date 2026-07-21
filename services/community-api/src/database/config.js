const postgresProtocols = new Set(["postgres:", "postgresql:"]);
const postgresSslModes = new Set(["", "disable", "require", "verify-ca", "verify-full"]);

export function createDatabaseConfig(env = process.env) {
  const databaseUrl = (env.DATABASE_URL ?? "").trim();
  const envSslMode = (env.POSTGRES_SSLMODE ?? env.PGSSLMODE ?? "").trim();

  if (databaseUrl === "") {
    return {
      mode: "memory",
      databaseUrl: "",
      sslMode: "",
      caCertPath: ""
    };
  }

  let parsed;
  try {
    parsed = new URL(databaseUrl);
  } catch {
    throw new Error("DATABASE_URL must be a valid URL");
  }

  if (!postgresProtocols.has(parsed.protocol)) {
    throw new Error("DATABASE_URL must use postgres or postgresql protocol");
  }

  const urlSslMode = parsed.searchParams.get("sslmode") ?? "";
  const inferredSslMode =
    urlSslMode === "" &&
    envSslMode === "" &&
    parsed.hostname.endsWith(".supabase.com")
      ? "require"
      : "";
  const sslMode = envSslMode || urlSslMode || inferredSslMode;

  if (!postgresSslModes.has(sslMode)) {
    throw new Error(
      "POSTGRES_SSLMODE must be disable, require, verify-ca, or verify-full"
    );
  }

  return {
    mode: "postgres",
    databaseUrl,
    sslMode,
    caCertPath: (env.AIVEN_CA_CERT_PATH ?? "").trim()
  };
}
