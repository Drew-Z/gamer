import { readFileSync } from "node:fs";

export function createPgClientOptions(config, { readFile = readFileSync } = {}) {
  const sslMode = String(config.sslMode ?? "").trim();
  const caCertPath = String(config.caCertPath ?? "").trim();
  const clientConfig = {
    connectionString: config.databaseUrl
  };

  const url = new URL(config.databaseUrl);
  if (sslMode !== "" || caCertPath !== "") {
    url.searchParams.delete("sslmode");
    clientConfig.connectionString = url.toString();
  }

  if (caCertPath !== "") {
    clientConfig.ssl = {
      ca: readFile(caCertPath, "utf8"),
      rejectUnauthorized: true
    };
    return clientConfig;
  }

  if (["require", "verify-ca", "verify-full"].includes(sslMode)) {
    clientConfig.ssl = {
      rejectUnauthorized: true
    };
  } else if (sslMode === "disable") {
    clientConfig.ssl = false;
  }

  return clientConfig;
}
