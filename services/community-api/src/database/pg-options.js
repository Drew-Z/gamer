import { readFileSync } from "node:fs";

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
    return clientConfig;
  }

  if (config.sslMode === "require") {
    clientConfig.ssl = {
      rejectUnauthorized: false
    };
  }

  return clientConfig;
}
