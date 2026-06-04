import assert from "node:assert/strict";
import test from "node:test";
import {
  createFantasyPetRuleImportSummary,
  summarizeFantasyPetRuleState
} from "./adapter.js";

test("missing state is blocked", () => {
  const summary = summarizeFantasyPetRuleState(null);

  assert.equal(summary.status, "blocked");
  assert.equal(summary.reason, "state is missing");
});

test("accepted preview is community-ready", () => {
  const summary = summarizeFantasyPetRuleState({
    currentStage: "preview-review",
    blockers: [],
    preview: {
      userDecision: "keep",
      urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
    }
  });

  assert.equal(summary.status, "community-ready");
  assert.equal(summary.evidence.length, 1);
});

test("blockers keep state blocked", () => {
  const summary = summarizeFantasyPetRuleState({
    currentStage: "base-review",
    blockers: [
      {
        stage: "base-review",
        reason: "base identity candidate has not been accepted by user"
      }
    ]
  });

  assert.equal(summary.status, "blocked");
  assert.equal(summary.reason, "base identity candidate has not been accepted by user");
});

test("active state without blockers is in-progress", () => {
  const summary = summarizeFantasyPetRuleState({
    currentStage: "action-design",
    blockers: [],
    preview: {
      userDecision: "missing"
    }
  });

  assert.equal(summary.status, "in-progress");
  assert.equal(summary.reason, "current stage is action-design");
});

test("import summary captures stable fantasy-pet-rule fields", () => {
  const result = createFantasyPetRuleImportSummary({
    schema: "fantasy-pet.codex-state.v1",
    petId: "demo-pet",
    currentStage: "preview-review",
    baseIdentity: { status: "accepted" },
    preview: {
      userDecision: "keep",
      urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
    },
    export: {
      decision: "asked",
      status: "ready",
      artifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
    },
    blockers: []
  });

  assert.equal(result.readiness.status, "community-ready");
  assert.equal(result.importSummary.source.schema, "fantasy-pet.codex-state.v1");
  assert.equal(result.importSummary.source.petId, "demo-pet");
  assert.equal(result.importSummary.source.currentStage, "preview-review");
  assert.equal(result.importSummary.source.baseIdentityStatus, "accepted");
  assert.equal(result.importSummary.review.previewDecision, "keep");
  assert.equal(result.importSummary.review.exportDecision, "asked");
  assert.equal(result.importSummary.review.exportStatus, "ready");
  assert.equal(
    result.importSummary.assets.previewPath,
    "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
  );
  assert.equal(
    result.importSummary.assets.exportArtifactPath,
    "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
  );
});
