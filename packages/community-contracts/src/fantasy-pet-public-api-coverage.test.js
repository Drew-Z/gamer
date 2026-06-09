import assert from "node:assert/strict";
import test from "node:test";
import { buildFantasyPetPublicApiCoverageReport } from "./fantasy-pet-public-api-coverage.js";

test("Android fantasy pet client covers the current public app API contract", () => {
  const report = buildFantasyPetPublicApiCoverageReport();

  assert.equal(report.contractSchema, "fantasy-pet.app-api-contract.v1");
  assert.deepEqual(report.missingSchemas, []);
  assert.deepEqual(report.missingBodyShapes, []);
  assert.deepEqual(report.missingReviewDecisions, []);
  assert.deepEqual(report.missingProgressStatuses, []);
  assert.deepEqual(report.missingNextActions, []);
  assert.deepEqual(report.missingGenerationStages, []);
  assert.deepEqual(report.missingPublicEndpointPaths, []);
  assert.deepEqual(report.unhandledPublicEndpointPaths, []);
  assert.deepEqual(report.unexpectedUnhandledPublicEndpointPaths, []);
  assert.deepEqual(report.internalHandoffArtifactsWithoutAndroidMarkers, []);
  assert.deepEqual(report.internalAuditPolicyFieldsWithoutAndroidMarkers, []);
  assert.deepEqual(report.adminEndpointReferences, []);
  assert.equal(report.candidateArtifactReviewGate, true);
  assert.equal(report.packageDownloadIsGated, true);
  assert.equal(report.communityImportUiControlsReachable, true);
  assert.equal(report.serverWorkerWaitNoticeReachable, true);
  assert.equal(report.usesTargetDownloadId, true);
  assert.deepEqual(report.securityBoundary, {
    exposesInternalPaths: false,
    exposesRawPrompt: false,
    exposesWorkerCommands: false,
    exposesSecrets: false,
    appMayInvokeAgentsDirectly: false,
    requiresHumanReview: true,
    adminEndpointsDisabledByDefault: true,
  });
});
