export const users = [
  {
    id: "user-demo-001",
    displayName: "Demo Keeper",
    handle: "demo_keeper",
    equippedPetId: "pet-stardust-001"
  }
];

export const feedPosts = [
  {
    id: "post-demo-001",
    authorId: "user-demo-001",
    petId: "pet-stardust-001",
    title: "Stardust dragon launch pose",
    body: "First preview package imported from a gated fantasy-pet-rule run.",
    reactionCount: 12,
    createdAt: "2026-06-04T04:00:00.000Z"
  },
  {
    id: "post-demo-002",
    authorId: "user-demo-001",
    petId: "pet-moonfox-001",
    title: "Moon fox sleepy loop",
    body: "A fixture post for testing pet-first feed navigation.",
    reactionCount: 7,
    createdAt: "2026-06-04T04:05:00.000Z"
  }
];

export const ledgerEntries = [
  {
    entryId: "ledger-checkin-001",
    userId: "user-demo-001",
    amount: 10,
    sourceType: "daily-checkin",
    sourceId: "checkin-2026-06-04",
    status: "posted",
    createdAt: "2026-06-04T04:10:00.000Z"
  },
  {
    entryId: "ledger-submission-001",
    userId: "user-demo-001",
    amount: 80,
    sourceType: "submission-reward",
    sourceId: "submission-demo-001",
    status: "posted",
    createdAt: "2026-06-04T04:20:00.000Z"
  }
];

export const wallet = {
  userId: "user-demo-001",
  balance: 90,
  currencyCode: "petcoin",
  ledgerEntries
};

export const checkInState = {
  userId: "user-demo-001",
  date: "2026-06-04",
  claimed: true,
  rewardAmount: 10,
  ledgerEntryId: "ledger-checkin-001"
};

export const importDrafts = [
  {
    id: "draft-demo-001",
    userId: "user-demo-001",
    status: "submitted",
    readiness: {
      status: "community-ready",
      reason: "Approved HidenCloud package seed."
    },
    importSummary: {
      source: {
        petId: "pet-stardust-001",
        displayName: "Stardust Dragon",
        schema: "fantasy-pet.package-manifest.v1",
        kind: "fantasy-pet-rule",
        runId: "issue-1-fresh-timeout3600-20260610-1",
        appJobId: "issue-1-fresh-timeout3600-20260610-1",
        statePath: "",
        baseIdentityStatus: "accepted"
      },
      review: {
        blockers: [],
        previewDecision: "keep",
        exportStatus: "ready",
        acceptedBy: "human-review",
        targetDownloadId: "artifact-34"
      },
      assets: {
        previewPath: "artifact-34",
        exportArtifactPath: "issue-1-fresh-timeout3600-20260610-1-package.zip",
        packageByteCount: 138651,
        motionSheets: ["artifact-34"]
      }
    },
    petId: "pet-stardust-001",
    ownershipClaimId: "claim-pet-stardust-001",
    scoreReportId: "score-pet-stardust-001",
    submissionId: "submission-demo-001",
    createdAt: "2026-06-04T04:12:00.000Z",
    submittedAt: "2026-06-04T04:15:00.000Z"
  }
];

export const scoreReports = [
  {
    reportId: "score-pet-stardust-001",
    petId: "pet-stardust-001",
    totalScore: 86,
    breakdown: {
      packageCompleteness: 18,
      visualQuality: 18,
      actionCoverage: 15,
      identityConsistency: 17,
      previewEvidence: 10,
      licenseReadiness: 8
    },
    rewardRecommendation: {
      grant: true,
      amount: 80,
      reason: "Accepted package with complete preview evidence."
    },
    risks: []
  }
];

export const submissions = [
  {
    id: "submission-demo-001",
    petId: "pet-stardust-001",
    userId: "user-demo-001",
    status: "approved",
    scoreReportId: "score-pet-stardust-001",
    ownershipClaimId: "claim-pet-stardust-001",
    importDraftId: "draft-demo-001",
    submittedAt: "2026-06-04T04:15:00.000Z"
  }
];

export const reviewQueue = [
  {
    submissionId: "submission-demo-001",
    status: "approved",
    reviewer: "system-fixture",
    rewardEntryId: "ledger-submission-001",
    reviewedAt: "2026-06-04T04:20:00.000Z"
  }
];

export const approvedPets = [
  {
    petId: "pet-stardust-001",
    displayName: "Stardust Dragon",
    ownerUserId: "user-demo-001",
    source: {
      kind: "fantasy-pet-rule",
      runId: "issue-1-fresh-timeout3600-20260610-1",
      appJobId: "issue-1-fresh-timeout3600-20260610-1",
      statePath: ""
    },
    assets: {
      previewPath: "artifact-34",
      exportArtifactPath: "issue-1-fresh-timeout3600-20260610-1-package.zip",
      motionSheetCount: 1
    },
    submissionId: "submission-demo-001",
    importDraftId: "draft-demo-001",
    scoreReportId: "score-pet-stardust-001",
    totalScore: 86,
    approvedAt: "2026-06-04T04:20:00.000Z"
  }
];
