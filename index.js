import { startAdminReviewServer } from "./apps/admin-review/server.js";
import { loadEnvFiles } from "./services/community-api/src/env-file.js";
import { startCommunityApiServer } from "./services/community-api/src/server.js";

loadEnvFiles();

const publicPort = process.env.PORT ?? "24674";
const communityApiPort = process.env.COMMUNITY_API_PORT ?? "4000";
const communityApiUrl = `http://127.0.0.1:${communityApiPort}`;
const fantasyPetApiBaseUrl =
  process.env.FANTASY_PET_API_BASE_URL ?? "http://120.48.67.110:8765";

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
  }
});
