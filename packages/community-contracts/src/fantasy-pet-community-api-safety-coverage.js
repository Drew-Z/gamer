import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import {
  loadFantasyPetAppHandoffRecord,
  missingInternalAuditPolicyFieldMarkers
} from "./fantasy-pet-public-api-coverage.js";

const DEFAULT_COMMUNITY_API_STORE = join(
  process.cwd(),
  "services",
  "community-api",
  "src",
  "store.js"
);

export function buildFantasyPetCommunityApiSafetyCoverageReport(options = {}) {
  const handoff = options.handoff ?? loadFantasyPetAppHandoffRecord(options);
  const storeSource = readRequiredFile(options.communityApiStorePath ?? DEFAULT_COMMUNITY_API_STORE);

  return {
    handoffSchema: handoff.schema,
    internalHandoffArtifactsWithoutCommunityApiMarkers: missingInternalArtifactMarkers(
      storeSource,
      handoff.internalArtifactsNotForApp ?? []
    ),
    internalAuditPolicyFieldsWithoutCommunityApiMarkers: missingInternalAuditPolicyFieldMarkers(
      storeSource,
      handoff.internalAuditPolicy ?? {}
    ),
    blocksUnsafePackagePaths:
      storeSource.includes("INTERNAL_PACKAGE_MARKERS") &&
      storeSource.includes("isSafePackageRelativePath") &&
      storeSource.includes("invalid_fantasy_pet_package")
  };
}

function readRequiredFile(path) {
  if (!existsSync(path)) {
    throw new Error(`community_api_store_missing:${path}`);
  }
  return readFileSync(path, "utf8");
}

function missingInternalArtifactMarkers(source, internalArtifacts) {
  const lowerSource = source.toLowerCase();
  return internalArtifacts
    .map((artifact) => artifact.split(/[\\/]/u).at(-1)?.toLowerCase() ?? "")
    .filter((marker) => /[a-z0-9]/u.test(marker))
    .filter((marker) => !lowerSource.includes(marker));
}
