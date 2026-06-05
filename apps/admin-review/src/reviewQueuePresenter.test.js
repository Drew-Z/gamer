import assert from "node:assert/strict";
import test from "node:test";
import {
  actionsForStatus,
  createReviewDashboardModel,
  createFantasyPetImportPayload,
  formatImportDraftStatus,
  formatReward
} from "./reviewQueuePresenter.js";

const queueFixture = {
  items: [
    {
      submission: {
        id: "submission-local-002",
        petId: "pet-stardust-002",
        userId: "user-demo-001",
        status: "pending",
        submittedAt: "2026-06-04T08:00:00.000Z"
      },
      scoreReport: {
        totalScore: 80,
        rewardRecommendation: {
          grant: true,
          amount: 80,
          reason: "Community-ready import has enough accepted evidence."
        },
        breakdown: {
          packageCompleteness: 20,
          visualQuality: 18,
          actionCoverage: 12,
          identityConsistency: 16,
          previewEvidence: 10,
          licenseReadiness: 8
        },
        risks: []
      },
      reviews: [],
      rewardLedgerEntries: [],
      outstandingReward: 0
    },
    {
      submission: {
        id: "submission-local-003",
        petId: "pet-moon-003",
        userId: "user-demo-001",
        status: "approved",
        submittedAt: "2026-06-04T08:05:00.000Z"
      },
      scoreReport: {
        totalScore: 74,
        rewardRecommendation: {
          grant: true,
          amount: 74,
          reason: "Enough evidence."
        },
        breakdown: {
          packageCompleteness: 12,
          visualQuality: 18,
          actionCoverage: 12,
          identityConsistency: 16,
          previewEvidence: 8,
          licenseReadiness: 8
        },
        risks: ["manual IP review requested"]
      },
      reviews: [
        {
          status: "approved",
          reviewer: "admin-demo",
          reviewedAt: "2026-06-04T08:10:00.000Z"
        }
      ],
      rewardLedgerEntries: [
        {
          amount: 74,
          sourceType: "submission-reward"
        }
      ],
      outstandingReward: 74
    }
  ]
};

test("formatReward renders positive, zero, and negative petcoin amounts", () => {
  assert.equal(formatReward(80), "+80 PC");
  assert.equal(formatReward(0), "0 PC");
  assert.equal(formatReward(-80), "-80 PC");
});

test("actionsForStatus exposes review actions by submission state", () => {
  assert.deepEqual(actionsForStatus("pending"), ["approve", "held", "reject"]);
  assert.deepEqual(actionsForStatus("held"), ["approve", "reject"]);
  assert.deepEqual(actionsForStatus("approved"), ["revoke"]);
  assert.deepEqual(actionsForStatus("revoked"), []);
});

test("createReviewDashboardModel summarizes queue counts and rows", () => {
  const model = createReviewDashboardModel(queueFixture);

  assert.equal(model.summary.total, 2);
  assert.equal(model.summary.pending, 1);
  assert.equal(model.summary.approved, 1);
  assert.equal(model.summary.outstandingReward, 74);
  assert.equal(model.rows[0].petId, "pet-stardust-002");
  assert.equal(model.rows[0].recommendedRewardLabel, "+80 PC");
  assert.equal(model.rows[0].actions.length, 3);
  assert.equal(model.rows[1].riskLabel, "1 risk");
  assert.equal(model.rows[1].latestReview.status, "approved");
});

test("createFantasyPetImportPayload trims state path and ownership claim", () => {
  const payload = createFantasyPetImportPayload({
    statePath: "  D:/workspace4Codex/fantasy-pet-rule/runs/demo/state.json  ",
    ownershipClaimId: "  claim-pet-demo-001  "
  });

  assert.deepEqual(payload, {
    statePath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/state.json",
    ownershipClaimId: "claim-pet-demo-001"
  });
});

test("formatImportDraftStatus summarizes created draft", () => {
  const message = formatImportDraftStatus({
    id: "import-draft-local-003",
    status: "ready",
    petId: "pet-demo-003",
    scoreReportId: "score-import-draft-local-003"
  });

  assert.equal(
    message,
    "Created ready draft import-draft-local-003 for pet-demo-003."
  );
});
