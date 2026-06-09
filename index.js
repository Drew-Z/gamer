import { startCommunityApiServer } from "./services/community-api/src/server.js";

startCommunityApiServer({
  env: {
    ...process.env,
    PORT: process.env.PORT ?? "24674",
    FANTASY_PET_API_BASE_URL:
      process.env.FANTASY_PET_API_BASE_URL ?? "http://120.48.67.110:8765"
  }
});
