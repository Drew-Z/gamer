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
