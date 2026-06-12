import { createDatabaseConfig } from "./database/config.js";
import { createFileBackedCommunityStore } from "./file-store.js";
import { createPostgresCommunityStore } from "./postgres-store.js";

export function createConfiguredCommunityStore(options = {}) {
  const env = options.env ?? process.env;
  const config = createDatabaseConfig(env);

  if (config.mode === "postgres") {
    return createPostgresCommunityStore({
      ...options,
      env
    });
  }

  return createFileBackedCommunityStore(options);
}
