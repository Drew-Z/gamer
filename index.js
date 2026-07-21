import path from "node:path";
import { fileURLToPath } from "node:url";
import { syncHidenRelease } from "./tools/hiden-release.js";
import { startPrivateOpsUserHooks } from "./tools/private-ops-user-hooks.js";

const repoRoot = path.dirname(fileURLToPath(import.meta.url));

const { loadEnvFiles } = await import("./services/community-api/src/env-file.js");
loadEnvFiles();
syncHidenRelease({
  env: process.env,
  repoRoot
});
const { startAdminReviewServer } = await import("./apps/admin-review/server.js");
const { startCommunityApiServer } = await import(
  "./services/community-api/src/server.js"
);

const publicPort = process.env.PORT ?? "24674";
const communityApiPort = process.env.COMMUNITY_API_PORT ?? "4000";
const communityApiUrl = `http://127.0.0.1:${communityApiPort}`;
// Keep the upstream explicit so local starts never call a private host by default.
const fantasyPetApiBaseUrl = process.env.FANTASY_PET_API_BASE_URL ?? "";

startCommunityApiServer({
  env: {
    ...process.env,
    PORT: communityApiPort,
    FANTASY_PET_API_BASE_URL: fantasyPetApiBaseUrl
  }
});

startAdminReviewServer({
  env: {
    ...process.env,
    PORT: publicPort,
    COMMUNITY_API_URL: communityApiUrl
  },
  repoRoot
});

startPrivateOpsUserHooks({
  env: process.env,
  repoRoot
});
