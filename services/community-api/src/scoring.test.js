import assert from "node:assert/strict";
import test from "node:test";
import { validateScoreReport } from "../../../packages/pet-package-spec/src/index.js";
import { createScoreReportFromImportDraft } from "./scoring.js";

test("community-ready import draft produces grant recommendation", () => {
  const report = createScoreReportFromImportDraft({
    id: "import-draft-local-001",
    petId: "pet-ready-001",
    ownershipClaimId: "claim-pet-ready-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-ready-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
      }
    }
  });

  assert.equal(validateScoreReport(report).ok, true);
  assert.equal(report.schema, "gamer.pet-score-report.v1");
  assert.equal(report.petId, "pet-ready-001");
  assert.equal(report.rewardRecommendation.grant, true);
  assert.equal(report.rewardRecommendation.amount, 80);
  assert.equal(report.risks.length, 0);
});

test("blocked import draft produces no grant and risk evidence", () => {
  const report = createScoreReportFromImportDraft({
    id: "import-draft-local-002",
    petId: "pet-blocked-001",
    readiness: {
      status: "blocked",
      reason: "state is missing"
    },
    importSummary: {
      source: {
        petId: "pet-blocked-001",
        baseIdentityStatus: ""
      },
      review: {
        blockers: [
          {
            stage: "base-review",
            reason: "base identity candidate has not been accepted by user"
          }
        ],
        previewDecision: "missing",
        exportStatus: "missing"
      },
      assets: {
        previewPath: "",
        exportArtifactPath: ""
      }
    }
  });

  assert.equal(validateScoreReport(report).ok, true);
  assert.equal(report.rewardRecommendation.grant, false);
  assert.equal(report.rewardRecommendation.amount, 0);
  assert.ok(report.risks.includes("fantasy-pet-rule state is blocked"));
});
