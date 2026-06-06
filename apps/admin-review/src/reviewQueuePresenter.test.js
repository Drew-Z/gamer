import assert from "node:assert/strict";
import test from "node:test";
import {
  actionsForStatus,
  createApprovedPetRegistryModel,
  createImportDraftListModel,
  createReviewDashboardModel,
  createFantasyPetImportPayload,
  formatImportEvidenceDetails,
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
      importDraft: {
        importSummary: {
          source: {
            kind: "fantasy-pet-rule"
          },
          assets: {
            previewPath: "previews/overall-showcase.png",
            motionSheets: ["motion/sheets/idle.png", "motion/sheets/happy_click.png"]
          }
        }
      },
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
      publishedFeedPost: {
        id: "post-submission-local-003",
        petId: "pet-moon-003"
      },
      outstandingReward: 74
    }
  ]
};

const approvedPetsFixture = {
  items: [
    {
      petId: "pet-stardust-001",
      displayName: "Stardust Dragon",
      ownerUserId: "user-demo-001",
      source: {
        kind: "fantasy-pet-rule"
      },
      assets: {
        previewPath: "previews/overall-showcase.png",
        motionSheetCount: 2
      },
      submissionId: "submission-local-002",
      importDraftId: "import-draft-local-001",
      scoreReportId: "score-import-draft-local-001",
      approvedAt: "2026-06-07T02:30:00.000Z",
      totalScore: 86
    },
    {
      petId: "pet-moonfox-001",
      displayName: "Moon Fox",
      ownerUserId: "user-demo-001",
      source: {
        kind: "pet-package-bundle"
      },
      assets: {
        previewPath: "previews/moonfox.png",
        motionSheetCount: 3
      },
      submissionId: "submission-local-003",
      totalScore: 91
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
  assert.equal(
    model.rows[0].recommendationReason,
    "Community-ready import has enough accepted evidence."
  );
  assert.equal(model.rows[0].actions.length, 3);
  assert.equal(model.rows[0].importSourceKind, "fantasy-pet-rule");
  assert.equal(model.rows[0].importPreviewPath, "previews/overall-showcase.png");
  assert.equal(model.rows[0].motionSheetCount, 2);
  assert.equal(
    model.rows[0].importEvidenceLabel,
    "fantasy-pet-rule / 2 motion sheets"
  );
  assert.equal(model.rows[0].feedPublicationStatus, "unpublished");
  assert.equal(model.rows[0].feedPublicationLabel, "Feed: unpublished");
  assert.equal(model.rows[1].riskLabel, "1 risk");
  assert.deepEqual(model.rows[1].riskItems, ["manual IP review requested"]);
  assert.equal(model.rows[1].latestReview.status, "approved");
  assert.equal(model.rows[1].feedPublicationStatus, "published");
  assert.equal(model.rows[1].feedPublicationLabel, "Feed: post-submission-local-003");
  assert.equal(model.rows[1].publishedFeedPostId, "post-submission-local-003");
});

test("formatImportEvidenceDetails summarizes import evidence fields", () => {
  assert.deepEqual(
    formatImportEvidenceDetails({
      importEvidenceLabel: "fantasy-pet-rule / 2 motion sheets",
      importPreviewPath: "previews/overall-showcase.png"
    }),
    {
      label: "fantasy-pet-rule / 2 motion sheets",
      previewPath: "previews/overall-showcase.png",
      hasPreviewPath: true
    }
  );

  assert.deepEqual(formatImportEvidenceDetails({}), {
    label: "No import evidence",
    previewPath: "No preview path",
    hasPreviewPath: false
  });
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

test("createImportDraftListModel summarizes draft statuses and rows", () => {
  const model = createImportDraftListModel({
    drafts: [
      {
        id: "import-draft-local-001",
        petId: "pet-ready-001",
        status: "ready",
        readiness: {
          reason: "preview accepted by user"
        },
        scoreReportId: "score-import-draft-local-001"
      },
      {
        id: "import-draft-local-002",
        petId: "pet-blocked-002",
        status: "blocked",
        readiness: {
          reason: "state has blockers"
        }
      },
      {
        id: "import-draft-local-003",
        petId: "pet-progress-003",
        status: "in-progress",
        readiness: {
          reason: "current stage is base-review"
        }
      }
    ]
  });

  assert.equal(model.summary.total, 3);
  assert.equal(model.summary.ready, 1);
  assert.equal(model.summary.blocked, 1);
  assert.equal(model.summary.inProgress, 1);
  assert.equal(model.rows[0].statusLabel, "ready");
  assert.equal(model.rows[0].canSubmit, true);
  assert.deepEqual(model.rows[0].actions, ["submit"]);
  assert.equal(model.rows[1].reason, "state has blockers");
  assert.equal(model.rows[1].canSubmit, false);
  assert.deepEqual(model.rows[1].actions, []);
  assert.equal(model.rows[2].scoreReportId, "");
  assert.equal(model.rows[2].canSubmit, false);
  assert.deepEqual(model.rows[2].actions, []);
});

test("createApprovedPetRegistryModel summarizes approved pet assets", () => {
  const model = createApprovedPetRegistryModel(approvedPetsFixture);

  assert.equal(model.summary.total, 2);
  assert.equal(model.rows[0].petId, "pet-stardust-001");
  assert.equal(model.rows[0].displayName, "Stardust Dragon");
  assert.equal(model.rows[0].ownerUserId, "user-demo-001");
  assert.equal(
    model.rows[0].assetLabel,
    "fantasy-pet-rule / score 86 / 2 motion sheets"
  );
  assert.equal(model.rows[0].previewPath, "previews/overall-showcase.png");
  assert.equal(model.rows[0].submissionLabel, "Submission submission-local-002");
  assert.equal(model.rows[0].approvedAt, "2026-06-07T02:30:00.000Z");
  assert.equal(
    model.rows[0].approvedAtLabel,
    "Approved 2026-06-07T02:30:00.000Z"
  );
  assert.equal(
    model.rows[0].scoreReportLabel,
    "Score score-import-draft-local-001"
  );
  assert.equal(model.rows[0].importDraftLabel, "Draft import-draft-local-001");
  assert.equal(model.rows[0].canFocusSubmission, true);
  assert.equal(model.rows[0].focusSubmissionLabel, "View submission");

  const missingSubmission = createApprovedPetRegistryModel({
    items: [
      {
        petId: "pet-unlinked-001",
        displayName: "Unlinked Pet"
      }
    ]
  });
  assert.equal(missingSubmission.rows[0].canFocusSubmission, false);
  assert.equal(missingSubmission.rows[0].focusSubmissionLabel, "");

  const empty = createApprovedPetRegistryModel({});
  assert.equal(empty.summary.total, 0);
  assert.deepEqual(empty.rows, []);
});
