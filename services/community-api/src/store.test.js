import assert from "node:assert/strict";
import test from "node:test";
import { validPetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
import { createCommunityStore } from "./store.js";

const validFantasyPetPackageManifest = {
  schema: "fantasy-pet.package-manifest.v1",
  runId: "run-public-lifecycle-smoke",
  appJobId: "public-lifecycle-smoke",
  acceptedBy: "human-review",
  sourceDownloadId: "artifact-1",
  sourceTaskId: "codex-worker-task",
  files: [
    {
      kind: "candidate",
      path: "artifacts/candidates/final-preview.png"
    },
    {
      kind: "metadata",
      path: "package/package-manifest.json"
    }
  ]
};

test("fresh store exposes initial wallet balance", () => {
  const store = createCommunityStore();

  assert.equal(store.getWallet("user-demo-001").balance, 90);
});

test("community home summary combines feed wallet check-in and submission counts", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-home-pending-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-home-pending-001",
    scoreReportId: "score-home-pending-001"
  });
  store.claimDailyCheckIn("user-demo-001", "2026-06-09");

  const home = store.getCommunityHome("user-demo-001", "2026-06-09");

  assert.equal(home.schema, "gamer.community-home.v1");
  assert.equal(home.userId, "user-demo-001");
  assert.ok(home.feed.items.length >= 2);
  assert.equal(home.wallet.balance, 100);
  assert.deepEqual(home.dailyCheckIn, {
    date: "2026-06-09",
    claimed: true,
    rewardAmount: 10,
    ledgerEntryId: "ledger-checkin-2026-06-09"
  });
  assert.equal(home.approvedPets.items.length, 0);
  assert.equal(home.submissionsSummary.pendingCount, 1);
  assert.ok(home.submissionsSummary.approvedCount >= 1);
  assert.equal(home.submissionsSummary.latest.id, submission.id);
});

test("first daily check-in posts ledger entry and increases balance", () => {
  const store = createCommunityStore();
  const result = store.claimDailyCheckIn("user-demo-001", "2026-06-05");

  assert.equal(result.checkIn.claimed, true);
  assert.equal(result.checkIn.date, "2026-06-05");
  assert.equal(result.wallet.balance, 100);
  assert.equal(result.ledgerEntry.amount, 10);
  assert.equal(result.ledgerEntry.sourceType, "daily-checkin");
});

test("second daily check-in returns existing claim without increasing balance", () => {
  const store = createCommunityStore();
  const first = store.claimDailyCheckIn("user-demo-001", "2026-06-05");
  const second = store.claimDailyCheckIn("user-demo-001", "2026-06-05");

  assert.equal(second.wallet.balance, 100);
  assert.equal(second.ledgerEntry.entryId, first.ledgerEntry.entryId);
  assert.equal(second.checkIn.claimed, true);
});

test("creating a submission adds pending submission", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-new-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-pet-new-001",
    scoreReportId: "score-pet-new-001"
  });

  assert.equal(submission.status, "pending");
  assert.equal(submission.petId, "pet-new-001");
  assert.ok(store.listSubmissions().submissions.some((item) => item.id === submission.id));
});

test("approving a submission posts reward ledger entry and marks review approved", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-new-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-pet-new-001",
    scoreReportId: "score-pet-new-001"
  });

  const review = store.reviewSubmission({
    submissionId: submission.id,
    status: "approved",
    reviewer: "admin-demo",
    rewardAmount: 55
  });

  assert.equal(review.status, "approved");
  assert.equal(review.rewardEntry.amount, 55);
  assert.equal(store.getWallet("user-demo-001").balance, 145);
});

test("blocked import draft is stored but cannot be submitted", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "blocked",
      reason: "state is missing"
    },
    importSummary: {
      source: {
        petId: "pet-blocked-001"
      }
    }
  });

  const result = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  assert.equal(draft.status, "blocked");
  assert.equal(result.error, "draft_not_ready");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});

test("community-ready import draft creates pending submission", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-ready-001"
      }
    },
    ownershipClaimId: "claim-pet-ready-001",
    scoreReportId: "score-pet-ready-001"
  });

  const result = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  assert.equal(result.draft.status, "submitted");
  assert.equal(result.submission.status, "pending");
  assert.equal(result.submission.petId, "pet-ready-001");
});

test("community-ready import draft receives generated score report", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-scored-001",
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
    },
    ownershipClaimId: "claim-pet-scored-001"
  });

  const report = store.getScoreReport(draft.scoreReportId);

  assert.equal(report.schema, "gamer.pet-score-report.v1");
  assert.equal(report.petId, "pet-scored-001");
  assert.equal(report.rewardRecommendation.grant, true);
});

test("pet package bundle creates ready import draft with uploaded score report", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const report = store.getScoreReport(draft.scoreReportId);

  assert.equal(draft.status, "ready");
  assert.equal(draft.petId, "pet-stardust-001");
  assert.equal(draft.ownershipClaimId, "claim-pet-stardust-001");
  assert.equal(draft.importSummary.source.kind, "fantasy-pet-rule");
  assert.equal(draft.importSummary.assets.previewPath, "previews/overall-showcase.png");
  assert.deepEqual(draft.importSummary.assets.motionSheets, [
    "motion/sheets/idle.png",
    "motion/sheets/happy_click.png"
  ]);
  assert.equal(
    draft.importSummary.assets.exportArtifactPath,
    "exports/stardust-package.zip"
  );
  assert.equal(report.reportId, draft.scoreReportId);
  assert.equal(report.petId, "pet-stardust-001");
  assert.equal(report.totalScore, validPetPackageBundle.scoreReport.totalScore);
  assert.equal(
    report.rewardRecommendation.amount,
    validPetPackageBundle.scoreReport.rewardRecommendation.amount
  );
});

test("fantasy pet package manifest creates ready import draft without internal paths", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraftFromFantasyPetPackage({
    userId: "user-demo-001",
    packageManifest: validFantasyPetPackageManifest,
    packageFileName: "pet-public-lifecycle-smoke.zip",
    packageByteCount: 664,
    targetDownloadId: "artifact-1",
    ownershipClaimId: "claim-public-lifecycle-smoke"
  });
  const report = store.getScoreReport(draft.scoreReportId);

  assert.equal(draft.status, "ready");
  assert.equal(draft.petId, "public-lifecycle-smoke");
  assert.equal(draft.importSummary.source.kind, "fantasy-pet-rule");
  assert.equal(draft.importSummary.source.schema, "fantasy-pet.package-manifest.v1");
  assert.equal(draft.importSummary.review.previewDecision, "keep");
  assert.equal(draft.importSummary.review.exportStatus, "ready");
  assert.equal(draft.importSummary.review.targetDownloadId, "artifact-1");
  assert.equal(
    draft.importSummary.assets.exportArtifactPath,
    "pet-public-lifecycle-smoke.zip"
  );
  assert.equal(draft.importSummary.assets.packageByteCount, 664);
  assert.deepEqual(draft.importSummary.assets.motionSheets, [
    "artifacts/candidates/final-preview.png"
  ]);
  assert.equal(report.petId, "public-lifecycle-smoke");
  assert.equal(report.rewardRecommendation.amount, 80);

  const submitted = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });
  store.reviewSubmission({
    submissionId: submitted.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const approvedPet = store
    .listApprovedPets()
    .items.find((item) => item.petId === "public-lifecycle-smoke");

  assert.equal(approvedPet.source.appJobId, "public-lifecycle-smoke");
  assert.equal(approvedPet.assets.targetDownloadId, "artifact-1");
  assert.equal(
    approvedPet.assets.previewUrl,
    "/pet-generation-jobs/public-lifecycle-smoke/artifacts/artifact-1"
  );
});

test("fantasy pet package manifest rejects internal file paths", () => {
  const store = createCommunityStore();
  const result = store.createImportDraftFromFantasyPetPackage({
    userId: "user-demo-001",
    packageManifest: {
      ...validFantasyPetPackageManifest,
      files: [
        {
          kind: "candidate",
          path: "C:/secret/runs/public-lifecycle-smoke/output.png"
        }
      ]
    },
    packageFileName: "pet-public-lifecycle-smoke.zip",
    packageByteCount: 664,
    targetDownloadId: "artifact-1",
    ownershipClaimId: "claim-public-lifecycle-smoke"
  });

  assert.equal(result.error, "invalid_fantasy_pet_package");
  assert.ok(result.validation.errors.includes("files[0].path must be a safe package-relative path"));
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});

test("fantasy pet package manifest rejects internal ledger and route artifacts", () => {
  const store = createCommunityStore();
  const result = store.createImportDraftFromFantasyPetPackage({
    userId: "user-demo-001",
    packageManifest: {
      ...validFantasyPetPackageManifest,
      files: [
        {
          kind: "candidate",
          path: "artifacts/candidates/final-preview.png"
        },
        {
          kind: "metadata",
          path: "learning-ledger.jsonl"
        },
        {
          kind: "metadata",
          path: "route-policy-decision.json"
        },
        {
          kind: "metadata",
          path: "ledger-suggestions/genericagent-ledger-suggestions.json"
        },
        {
          kind: "metadata",
          path: "ledger-suggestions/genericagent-ledger-import.json"
        },
        {
          kind: "metadata",
          path: "review/stage-gate-ledger-import.json"
        }
      ]
    },
    packageFileName: "pet-public-lifecycle-smoke.zip",
    packageByteCount: 664,
    targetDownloadId: "artifact-1",
    ownershipClaimId: "claim-public-lifecycle-smoke"
  });

  assert.equal(result.error, "invalid_fantasy_pet_package");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});

test("fantasy pet package manifest rejects stage gate ledger import artifact", () => {
  const store = createCommunityStore();
  const result = store.createImportDraftFromFantasyPetPackage({
    userId: "user-demo-001",
    packageManifest: {
      ...validFantasyPetPackageManifest,
      files: [
        {
          kind: "candidate",
          path: "artifacts/candidates/final-preview.png"
        },
        {
          kind: "metadata",
          path: "review/stage-gate-ledger-import.json"
        }
      ]
    },
    packageFileName: "pet-public-lifecycle-smoke.zip",
    packageByteCount: 664,
    targetDownloadId: "artifact-1",
    ownershipClaimId: "claim-public-lifecycle-smoke"
  });

  assert.equal(result.error, "invalid_fantasy_pet_package");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});

test("pet package bundle owned by another user does not create import draft", () => {
  const store = createCommunityStore();
  const result = store.createImportDraftFromPetPackageBundle({
    userId: "user-other-001",
    bundle: validPetPackageBundle
  });

  assert.equal(result.error, "bundle_owner_mismatch");
  assert.equal(result.userId, "user-other-001");
  assert.equal(result.ownerUserId, "user-demo-001");
  assert.equal(store.listImportDrafts("user-other-001").drafts.length, 0);
});

test("pet package bundle cannot create duplicate active import draft", () => {
  const store = createCommunityStore();
  const first = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const second = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });

  assert.equal(first.status, "ready");
  assert.equal(second.error, "duplicate_import_draft");
  assert.equal(second.petId, "pet-stardust-001");
  assert.equal(second.existingDraftId, first.id);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});

test("pet package bundle cannot create duplicate import draft after submission", () => {
  const store = createCommunityStore();
  const first = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const submitted = store.submitImportDraft({
    draftId: first.id,
    userId: "user-demo-001"
  });
  const second = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });

  assert.equal(submitted.submission.petId, "pet-stardust-001");
  assert.equal(second.error, "duplicate_import_draft");
  assert.equal(second.petId, "pet-stardust-001");
  assert.equal(second.existingDraftId, first.id);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});

test("approving scored submission uses recommended reward when amount is omitted", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-rewarded-001",
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
    },
    ownershipClaimId: "claim-pet-rewarded-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  const review = store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  assert.equal(review.rewardEntry.amount, 80);
  assert.equal(store.getWallet("user-demo-001").balance, 170);
});

test("holding scored submission does not post reward", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-held-001",
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
    },
    ownershipClaimId: "claim-pet-held-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  const review = store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "held",
    reviewer: "admin-demo"
  });

  assert.equal(review.status, "held");
  assert.equal(review.rewardEntry, null);
  assert.equal(store.getWallet("user-demo-001").balance, 90);
});

test("rejecting scored submission does not post reward", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-rejected-001",
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
    },
    ownershipClaimId: "claim-pet-rejected-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  const review = store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "rejected",
    reviewer: "admin-demo"
  });

  assert.equal(review.status, "rejected");
  assert.equal(review.rewardEntry, null);
  assert.equal(store.getWallet("user-demo-001").balance, 90);
});

test("revoking approved submission posts reversal ledger entry", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-revoked-001",
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
    },
    ownershipClaimId: "claim-pet-revoked-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });
  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const revoke = store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "revoked",
    reviewer: "admin-demo"
  });

  assert.equal(revoke.status, "revoked");
  assert.equal(revoke.rewardReversalEntry.amount, -80);
  assert.equal(revoke.rewardReversalEntry.sourceType, "submission-reward-reversal");
  assert.equal(store.getWallet("user-demo-001").balance, 90);
});

test("admin review queue aggregates submission evidence and reward status", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-queue-001",
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
    },
    ownershipClaimId: "claim-pet-queue-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });
  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });
  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "revoked",
    reviewer: "admin-demo"
  });

  const queue = store.listAdminReviewQueue();
  const item = queue.items.find(
    (entry) => entry.submission.id === submissionResult.submission.id
  );

  assert.equal(item.submission.status, "revoked");
  assert.equal(item.scoreReport.petId, "pet-queue-001");
  assert.equal(item.reviews.length, 2);
  assert.equal(item.rewardLedgerEntries.length, 2);
  assert.equal(item.outstandingReward, 0);
});

test("admin review queue includes import draft evidence", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  const queue = store.listAdminReviewQueue();
  const item = queue.items.find(
    (entry) => entry.submission.id === submissionResult.submission.id
  );

  assert.equal(item.importDraft.id, draft.id);
  assert.equal(item.importDraft.importSummary.source.kind, "fantasy-pet-rule");
  assert.equal(
    item.importDraft.importSummary.assets.previewPath,
    "previews/overall-showcase.png"
  );
});

test("approving imported submission publishes one community feed post", () => {
  const store = createCommunityStore();
  const initialFeed = store.getFeed();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-feed-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed/export.zip"
      }
    },
    ownershipClaimId: "claim-pet-feed-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const feed = store.getFeed();
  const published = feed.items.find((post) => post.petId === "pet-feed-001");

  assert.equal(feed.items.length, initialFeed.items.length + 1);
  assert.equal(published.title, "Approved pet import: pet-feed-001");
  assert.equal(published.body, "preview accepted by user");
  assert.equal(published.authorId, "user-demo-001");
});

test("approved import feed post includes import metadata", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-feed-metadata-001",
        kind: "fantasy-pet-rule",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "previews/overall-showcase.png",
        motionSheets: ["motion/sheets/idle.png", "motion/sheets/happy_click.png"],
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-metadata/export.zip"
      }
    },
    ownershipClaimId: "claim-pet-feed-metadata-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const feed = store.getFeed();
  const published = feed.items.find((post) => post.petId === "pet-feed-metadata-001");

  assert.deepEqual(published.metadata, {
    sourceType: "approved-import",
    importDraftId: draft.id,
    submissionId: submissionResult.submission.id,
    scoreReportId: draft.scoreReportId,
    rewardAmount: 80,
    importSourceKind: "fantasy-pet-rule",
    importPreviewPath: "previews/overall-showcase.png",
    exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-metadata/export.zip",
    motionSheetCount: 2
  });
  assert.equal(published.metadata.importSourceKind, "fantasy-pet-rule");
  assert.equal(published.metadata.importPreviewPath, "previews/overall-showcase.png");
  assert.equal(published.metadata.motionSheetCount, 2);
});

test("approved imported submission registers approved pet asset", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraftFromPetPackageBundle({
    userId: "user-demo-001",
    bundle: validPetPackageBundle
  });
  const submitted = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submitted.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const registry = store.listApprovedPets();
  const pet = registry.items.find((item) => item.petId === "pet-stardust-001");

  assert.equal(pet.displayName, "Stardust Dragon");
  assert.equal(pet.ownerUserId, "user-demo-001");
  assert.equal(pet.source.kind, "fantasy-pet-rule");
  assert.equal(pet.assets.previewPath, "previews/overall-showcase.png");
  assert.equal(pet.assets.motionSheetCount, 2);
  assert.equal(pet.submissionId, submitted.submission.id);
  assert.equal(pet.importDraftId, draft.id);
});

test("approved imported submission registers export artifact asset", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-export-registry-001",
        displayName: "Export Registry Pet",
        kind: "fantasy-pet-rule",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/export.zip"
      }
    },
    ownershipClaimId: "claim-pet-export-registry-001"
  });
  const submitted = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submitted.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const registry = store.listApprovedPets();
  const pet = registry.items.find((item) => item.petId === "pet-export-registry-001");

  assert.equal(
    pet.assets.exportArtifactPath,
    "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/export.zip"
  );
});

test("approving imported submission twice does not duplicate feed post", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-feed-idempotent-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-idempotent/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-idempotent/export.zip"
      }
    },
    ownershipClaimId: "claim-pet-feed-idempotent-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });
  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  const matchingPosts = store
    .getFeed()
    .items.filter((post) => post.petId === "pet-feed-idempotent-001");

  assert.equal(matchingPosts.length, 1);
});

test("revoking imported submission removes approved import feed post", () => {
  const store = createCommunityStore();
  const draft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-feed-revoked-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-revoked/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/feed-revoked/export.zip"
      }
    },
    ownershipClaimId: "claim-pet-feed-revoked-001"
  });
  const submissionResult = store.submitImportDraft({
    draftId: draft.id,
    userId: "user-demo-001"
  });

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "approved",
    reviewer: "admin-demo"
  });
  assert.ok(
    store.getFeed().items.some((post) => post.petId === "pet-feed-revoked-001")
  );
  const approvedQueueItem = store
    .listAdminReviewQueue()
    .items.find((item) => item.submission.id === submissionResult.submission.id);
  assert.equal(
    approvedQueueItem.publishedFeedPost.id,
    `post-${submissionResult.submission.id}`
  );

  store.reviewSubmission({
    submissionId: submissionResult.submission.id,
    status: "revoked",
    reviewer: "admin-demo"
  });

  assert.equal(
    store.getFeed().items.some((post) => post.petId === "pet-feed-revoked-001"),
    false
  );
  const revokedQueueItem = store
    .listAdminReviewQueue()
    .items.find((item) => item.submission.id === submissionResult.submission.id);
  assert.equal(revokedQueueItem.publishedFeedPost, null);
});

test("terminal submissions cannot be reviewed again", () => {
  const store = createCommunityStore();
  const rejected = store.createSubmission({
    petId: "pet-terminal-rejected-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-terminal-rejected-001",
    scoreReportId: "score-terminal-rejected-001"
  });
  store.reviewSubmission({
    submissionId: rejected.id,
    status: "rejected",
    reviewer: "admin-demo"
  });

  const rejectedResult = store.reviewSubmission({
    submissionId: rejected.id,
    status: "approved",
    reviewer: "admin-demo",
    rewardAmount: 40
  });

  assert.equal(rejectedResult.error, "submission_terminal");
  assert.equal(rejectedResult.status, "rejected");
  assert.equal(store.getWallet("user-demo-001").balance, 90);

  const revokedDraft = store.createImportDraft({
    userId: "user-demo-001",
    readiness: {
      status: "community-ready",
      reason: "preview accepted by user"
    },
    importSummary: {
      source: {
        petId: "pet-terminal-revoked-001",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready"
      },
      assets: {
        previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/terminal-revoked/preview.html",
        exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/terminal-revoked/export.zip"
      }
    },
    ownershipClaimId: "claim-terminal-revoked-001"
  });
  const revokedSubmission = store.submitImportDraft({
    draftId: revokedDraft.id,
    userId: "user-demo-001"
  }).submission;
  store.reviewSubmission({
    submissionId: revokedSubmission.id,
    status: "approved",
    reviewer: "admin-demo"
  });
  store.reviewSubmission({
    submissionId: revokedSubmission.id,
    status: "revoked",
    reviewer: "admin-demo"
  });

  const revokedResult = store.reviewSubmission({
    submissionId: revokedSubmission.id,
    status: "approved",
    reviewer: "admin-demo"
  });

  assert.equal(revokedResult.error, "submission_terminal");
  assert.equal(revokedResult.status, "revoked");
  assert.equal(store.getWallet("user-demo-001").balance, 90);
  assert.equal(
    store.getFeed().items.some((post) => post.petId === "pet-terminal-revoked-001"),
    false
  );
});

test("invalid review status is rejected without mutating submission", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-invalid-review-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-invalid-review-001",
    scoreReportId: "score-invalid-review-001"
  });

  const result = store.reviewSubmission({
    submissionId: submission.id,
    status: "published",
    reviewer: "admin-demo",
    rewardAmount: 40
  });
  const queueItem = store
    .listAdminReviewQueue()
    .items.find((item) => item.submission.id === submission.id);

  assert.equal(result.error, "invalid_review_status");
  assert.equal(result.status, "published");
  assert.deepEqual(result.allowedStatuses, ["approved", "held", "rejected", "revoked"]);
  assert.equal(queueItem.submission.status, "pending");
  assert.equal(queueItem.reviews.length, 0);
  assert.equal(store.getWallet("user-demo-001").balance, 90);
});

test("invalid explicit reward amount is rejected without mutating submission", () => {
  const store = createCommunityStore();
  const submission = store.createSubmission({
    petId: "pet-invalid-reward-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-invalid-reward-001",
    scoreReportId: "score-invalid-reward-001"
  });

  const result = store.reviewSubmission({
    submissionId: submission.id,
    status: "approved",
    reviewer: "admin-demo",
    rewardAmount: -5
  });
  const queueItem = store
    .listAdminReviewQueue()
    .items.find((item) => item.submission.id === submission.id);

  assert.equal(result.error, "invalid_reward_amount");
  assert.equal(result.rewardAmount, -5);
  assert.equal(queueItem.submission.status, "pending");
  assert.equal(queueItem.reviews.length, 0);
  assert.equal(store.getWallet("user-demo-001").balance, 90);
});
