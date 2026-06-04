import {
  checkInState,
  feedPosts,
  ledgerEntries,
  reviewQueue,
  submissions,
  users
} from "../../../packages/community-contracts/src/index.js";

const clone = (value) => JSON.parse(JSON.stringify(value));

const defaultSeed = {
  users,
  feedPosts,
  ledgerEntries,
  checkIns: [checkInState],
  submissions,
  reviewQueue
};

const nowIso = () => new Date().toISOString();

const sumPostedLedger = (entries, userId) =>
  entries
    .filter((entry) => entry.userId === userId && entry.status === "posted")
    .reduce((sum, entry) => sum + entry.amount, 0);

export function createCommunityStore(seed = defaultSeed) {
  const state = clone(seed);

  const nextId = (prefix, collection) =>
    `${prefix}-${String(collection.length + 1).padStart(3, "0")}`;

  return {
    getMe() {
      return clone(state.users[0]);
    },

    getFeed() {
      return {
        items: clone(state.feedPosts),
        nextCursor: "fixture-page-2"
      };
    },

    getWallet(userId) {
      const userLedger = state.ledgerEntries.filter((entry) => entry.userId === userId);
      return {
        userId,
        balance: sumPostedLedger(state.ledgerEntries, userId),
        currencyCode: "petcoin",
        ledgerEntries: clone(userLedger)
      };
    },

    claimDailyCheckIn(userId, date) {
      const existing = state.checkIns.find(
        (checkIn) => checkIn.userId === userId && checkIn.date === date
      );

      if (existing) {
        const existingEntry = state.ledgerEntries.find(
          (entry) => entry.entryId === existing.ledgerEntryId
        );
        return {
          checkIn: clone(existing),
          wallet: this.getWallet(userId),
          ledgerEntry: clone(existingEntry)
        };
      }

      const ledgerEntry = {
        schema: "gamer.currency-ledger-entry.v1",
        entryId: `ledger-checkin-${date}`,
        userId,
        amount: 10,
        sourceType: "daily-checkin",
        sourceId: `checkin-${date}`,
        status: "posted",
        createdAt: nowIso()
      };
      const checkIn = {
        userId,
        date,
        claimed: true,
        rewardAmount: 10,
        ledgerEntryId: ledgerEntry.entryId
      };

      state.ledgerEntries.push(ledgerEntry);
      state.checkIns.push(checkIn);

      return {
        checkIn: clone(checkIn),
        wallet: this.getWallet(userId),
        ledgerEntry: clone(ledgerEntry)
      };
    },

    listSubmissions() {
      return {
        submissions: clone(state.submissions),
        reviewQueue: clone(state.reviewQueue)
      };
    },

    createSubmission(input) {
      const submission = {
        id: nextId("submission-local", state.submissions),
        petId: input.petId,
        userId: input.userId,
        status: "pending",
        scoreReportId: input.scoreReportId,
        ownershipClaimId: input.ownershipClaimId,
        submittedAt: nowIso()
      };

      state.submissions.push(submission);
      return clone(submission);
    },

    reviewSubmission(input) {
      const submission = state.submissions.find((item) => item.id === input.submissionId);
      if (!submission) {
        return {
          error: "submission_not_found",
          submissionId: input.submissionId
        };
      }

      submission.status = input.status;

      let rewardEntry = null;
      if (input.status === "approved" && input.rewardAmount > 0) {
        rewardEntry = {
          schema: "gamer.currency-ledger-entry.v1",
          entryId: nextId("ledger-review", state.ledgerEntries),
          userId: submission.userId,
          amount: input.rewardAmount,
          sourceType: "submission-reward",
          sourceId: submission.id,
          status: "posted",
          createdAt: nowIso()
        };
        state.ledgerEntries.push(rewardEntry);
      }

      const review = {
        submissionId: submission.id,
        status: input.status,
        reviewer: input.reviewer,
        rewardEntryId: rewardEntry?.entryId ?? "",
        reviewedAt: nowIso()
      };
      state.reviewQueue.push(review);

      return {
        ...clone(review),
        rewardEntry: clone(rewardEntry)
      };
    }
  };
}
