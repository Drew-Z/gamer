const postgresProtocols = new Set(["postgres:", "postgresql:"]);

export function createDatabaseConfig(env = process.env) {
  const databaseUrl = (env.DATABASE_URL ?? "").trim();

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

  return {
    mode: "postgres",
    databaseUrl,
    sslMode: parsed.searchParams.get("sslmode") ?? "",
    caCertPath: (env.AIVEN_CA_CERT_PATH ?? "").trim()
  };
}
