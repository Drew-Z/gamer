export const validPetPackageManifest = {
  schema: "gamer.pet-package.v1",
  petId: "pet-stardust-001",
  displayName: "Stardust Dragon",
  ownerUserId: "user-demo-001",
  source: {
    kind: "fantasy-pet-rule",
    runId: "stardust-chinese-dragon-codex-02",
    statePath: "D:/workspace4Codex/fantasy-pet-rule/runs/stardust-chinese-dragon-codex-02/state.json"
  },
  assets: {
    baseImage: "assets/base_identity_accepted_clean.png",
    previewImage: "previews/overall-showcase.png",
    motionSheets: ["motion/sheets/idle.png", "motion/sheets/happy_click.png"]
  },
  license: "license.json",
  scoreReport: "score-report.json"
};

export const validOwnershipClaim = {
  schema: "gamer.ownership-claim.v1",
  claimId: "claim-pet-stardust-001",
  userId: "user-demo-001",
  petId: "pet-stardust-001",
  claimType: "original-created",
  attestation: "I created or control the submitted pet package assets.",
  sourceReferences: [],
  submittedAt: "2026-06-04T04:15:00.000Z",
  reviewStatus: "pending"
};

export const validScoreReport = {
  schema: "gamer.pet-score-report.v1",
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
};

export const validPetPackageBundle = {
  manifest: validPetPackageManifest,
  ownershipClaim: validOwnershipClaim,
  scoreReport: validScoreReport
};

export const validCurrencyLedgerEntry = {
  schema: "gamer.currency-ledger-entry.v1",
  entryId: "ledger-submission-001",
  userId: "user-demo-001",
  amount: 80,
  sourceType: "submission-reward",
  sourceId: "submission-demo-001",
  status: "posted",
  createdAt: "2026-06-04T04:20:00.000Z"
};
