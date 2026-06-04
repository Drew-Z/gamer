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

export const submissions = [
  {
    id: "submission-demo-001",
    petId: "pet-stardust-001",
    userId: "user-demo-001",
    status: "approved",
    scoreReportId: "score-pet-stardust-001",
    ownershipClaimId: "claim-pet-stardust-001",
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
