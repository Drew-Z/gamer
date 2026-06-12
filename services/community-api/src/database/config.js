const postgresProtocols = new Set(["postgres:", "postgresql:"]);

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

  return {
    mode: "postgres",
    databaseUrl,
    sslMode: urlSslMode || envSslMode || inferredSslMode,
    caCertPath: (env.AIVEN_CA_CERT_PATH ?? "").trim()
  };
}
