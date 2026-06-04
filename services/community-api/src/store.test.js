import assert from "node:assert/strict";
import test from "node:test";
import { createCommunityStore } from "./store.js";

test("fresh store exposes initial wallet balance", () => {
  const store = createCommunityStore();

  assert.equal(store.getWallet("user-demo-001").balance, 90);
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
