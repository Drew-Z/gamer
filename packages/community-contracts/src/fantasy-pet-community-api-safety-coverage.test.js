import assert from "node:assert/strict";
import test from "node:test";
import { buildFantasyPetCommunityApiSafetyCoverageReport } from "./fantasy-pet-community-api-safety-coverage.js";

test("community API fantasy pet package import blocks current handoff internals", () => {
  const report = buildFantasyPetCommunityApiSafetyCoverageReport();

  assert.equal(report.handoffSchema, "fantasy-pet.app-handoff-record.v1");
  assert.deepEqual(report.internalHandoffArtifactsWithoutCommunityApiMarkers, []);
  assert.deepEqual(report.internalAuditPolicyFieldsWithoutCommunityApiMarkers, []);
  assert.equal(report.blocksUnsafePackagePaths, true);
});
