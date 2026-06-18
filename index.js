import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.dirname(fileURLToPath(import.meta.url));

function isEnabled(value) {
  return ["1", "true", "yes", "on"].includes(
    String(value ?? "").trim().toLowerCase()
  );
}

function maybePullLatest() {
  const enabled =
    isEnabled(process.env.GAMER_AUTO_UPDATE) || isEnabled(process.env.AUTO_UPDATE);
  if (!enabled || !existsSync(path.join(repoRoot, ".git"))) {
    return;
  }

  try {
    execFileSync("git", ["pull", "--ff-only", "origin", "main"], {
      cwd: repoRoot,
      stdio: "inherit",
      timeout: 120_000
    });
  } catch (error) {
    console.error("gamer auto-update failed; starting current checkout", error);
  }
}

maybePullLatest();

const { startAdminReviewServer } = await import("./apps/admin-review/server.js");
const { loadEnvFiles } = await import("./services/community-api/src/env-file.js");
const { startCommunityApiServer } = await import(
  "./services/community-api/src/server.js"
);

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
