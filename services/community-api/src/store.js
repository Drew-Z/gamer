import {
  checkInState,
  feedPosts,
  ledgerEntries,
  reviewQueue,
  submissions,
  users
} from "../../../packages/community-contracts/src/index.js";
import { createScoreReportFromImportDraft } from "./scoring.js";

const clone = (value) => JSON.parse(JSON.stringify(value));

const defaultSeed = {
  users,
  feedPosts,
  ledgerEntries,
  checkIns: [checkInState],
  submissions,
  reviewQueue,
  importDrafts: [],
  scoreReports: [],
  approvedPets: []
};

const nowIso = () => new Date().toISOString();

const sumPostedLedger = (entries, userId) =>
  entries
    .filter((entry) => entry.userId === userId && entry.status === "posted")
    .reduce((sum, entry) => sum + entry.amount, 0);

const sumPostedSubmissionReward = (entries, submissionId) =>
  entries
    .filter(
      (entry) =>
        entry.sourceId === submissionId &&
        entry.status === "posted" &&
        (entry.sourceType === "submission-reward" ||
          entry.sourceType === "submission-reward-reversal")
    )
    .reduce((sum, entry) => sum + entry.amount, 0);

const submissionRewardLedgerEntries = (entries, submissionId) =>
  entries.filter(
    (entry) =>
      entry.sourceId === submissionId &&
      (entry.sourceType === "submission-reward" ||
        entry.sourceType === "submission-reward-reversal")
  );

const draftStatusFromReadiness = (readiness) => {
  if (readiness?.status === "community-ready") {
    return "ready";
  }

  if (readiness?.status === "blocked") {
    return "blocked";
  }

  return "in-progress";
};

const createImportSummaryFromPetPackageBundle = (bundle) => ({
  source: {
    petId: bundle.manifest.petId,
    displayName: bundle.manifest.displayName,
    schema: bundle.manifest.schema,
    kind: bundle.manifest.source.kind,
    runId: bundle.manifest.source.runId,
    statePath: bundle.manifest.source.statePath
  },
  review: {
    blockers: [],
    previewDecision: "keep",
    exportStatus: "ready"
  },
  assets: {
    baseImage: bundle.manifest.assets.baseImage,
    previewPath: bundle.manifest.assets.previewImage,
    motionSheets: clone(bundle.manifest.assets.motionSheets)
  }
});

const TERMINAL_SUBMISSION_STATUSES = new Set(["rejected", "revoked"]);
const ALLOWED_REVIEW_STATUSES = ["approved", "held", "rejected", "revoked"];
const isValidExplicitRewardAmount = (amount) =>
  amount === undefined || (Number.isInteger(amount) && amount >= 0);

const createFeedPostFromApprovedImport = (submission, draft, scoreReport, rewardEntry) => ({
  id: `post-${submission.id}`,
  authorId: submission.userId,
  petId: submission.petId,
  title: `Approved pet import: ${submission.petId}`,
  body: draft?.readiness?.reason ?? "Approved community pet import.",
  reactionCount: 0,
  createdAt: nowIso(),
  metadata: {
    sourceType: "approved-import",
    importDraftId: submission.importDraftId,
    submissionId: submission.id,
    scoreReportId: submission.scoreReportId,
    rewardAmount: rewardEntry?.amount ?? scoreReport?.rewardRecommendation?.amount ?? 0,
    importSourceKind: draft?.importSummary?.source?.kind ?? "",
    importPreviewPath: draft?.importSummary?.assets?.previewPath ?? "",
    motionSheetCount: Array.isArray(draft?.importSummary?.assets?.motionSheets)
      ? draft.importSummary.assets.motionSheets.length
      : 0
  }
});

const createApprovedPetFromImport = (submission, draft, scoreReport) => ({
  petId: submission.petId,
  displayName: draft?.importSummary?.source?.displayName ?? submission.petId,
  ownerUserId: submission.userId,
  source: {
    kind: draft?.importSummary?.source?.kind ?? "",
    runId: draft?.importSummary?.source?.runId ?? "",
    statePath: draft?.importSummary?.source?.statePath ?? ""
  },
  assets: {
    previewPath: draft?.importSummary?.assets?.previewPath ?? "",
    motionSheetCount: Array.isArray(draft?.importSummary?.assets?.motionSheets)
      ? draft.importSummary.assets.motionSheets.length
      : 0
  },
  submissionId: submission.id,
  importDraftId: submission.importDraftId,
  scoreReportId: submission.scoreReportId,
  totalScore: Number(scoreReport?.totalScore ?? 0),
  approvedAt: nowIso()
});

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

    listApprovedPets() {
      return {
        items: clone(state.approvedPets)
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

    listAdminReviewQueue() {
      return {
        items: state.submissions.map((submission) => {
          const scoreReport = state.scoreReports.find(
            (report) => report.reportId === submission.scoreReportId
          );
          const reviews = state.reviewQueue.filter(
            (review) => review.submissionId === submission.id
          );
          const rewardLedgerEntries = submissionRewardLedgerEntries(
            state.ledgerEntries,
            submission.id
          );
          const importDraft = state.importDrafts.find(
            (draft) => draft.id === submission.importDraftId
          );
          const publishedFeedPost = state.feedPosts.find(
            (post) => post.id === `post-${submission.id}`
          );

          return {
            submission: clone(submission),
            importDraft: clone(importDraft ?? null),
            scoreReport: clone(scoreReport ?? null),
            reviews: clone(reviews),
            rewardLedgerEntries: clone(rewardLedgerEntries),
            publishedFeedPost: clone(publishedFeedPost ?? null),
            outstandingReward: sumPostedSubmissionReward(
              state.ledgerEntries,
              submission.id
            )
          };
        })
      };
    },

    listImportDrafts(userId) {
      return {
        drafts: clone(state.importDrafts.filter((draft) => draft.userId === userId))
      };
    },

    getScoreReport(scoreReportId) {
      const report = state.scoreReports.find((item) => item.reportId === scoreReportId);
      return clone(report ?? null);
    },

    createImportDraft(input) {
      const draft = {
        id: nextId("import-draft-local", state.importDrafts),
        userId: input.userId,
        status: draftStatusFromReadiness(input.readiness),
        readiness: clone(input.readiness ?? {}),
        importSummary: clone(input.importSummary ?? {}),
        petId: input.importSummary?.source?.petId ?? input.petId ?? "",
        ownershipClaimId: input.ownershipClaimId ?? "",
        scoreReportId: input.scoreReportId ?? "",
        createdAt: nowIso()
      };

      if (input.scoreReport) {
        const scoreReport = {
          ...clone(input.scoreReport),
          reportId: input.scoreReport.reportId ?? `score-${draft.id}`
        };
        state.scoreReports.push(scoreReport);
        draft.scoreReportId = scoreReport.reportId;
      } else if (!draft.scoreReportId) {
        const scoreReport = createScoreReportFromImportDraft(draft);
        state.scoreReports.push(scoreReport);
        draft.scoreReportId = scoreReport.reportId;
      }

      state.importDrafts.push(draft);
      return clone(draft);
    },

    createImportDraftFromPetPackageBundle(input) {
      const ownerUserId = input.bundle.manifest.ownerUserId;
      if (input.userId !== ownerUserId) {
        return {
          error: "bundle_owner_mismatch",
          userId: input.userId,
          ownerUserId
        };
      }

      const existingDraft = state.importDrafts.find(
        (draft) =>
          draft.userId === input.userId &&
          draft.petId === input.bundle.manifest.petId
      );
      if (existingDraft) {
        return {
          error: "duplicate_import_draft",
          petId: input.bundle.manifest.petId,
          existingDraftId: existingDraft.id
        };
      }

      return this.createImportDraft({
        userId: input.userId,
        readiness: {
          status: "community-ready",
          reason: "validated pet package bundle"
        },
        importSummary: createImportSummaryFromPetPackageBundle(input.bundle),
        ownershipClaimId: input.bundle.ownershipClaim.claimId,
        scoreReport: input.bundle.scoreReport
      });
    },

    submitImportDraft(input) {
      const draft = state.importDrafts.find(
        (item) => item.id === input.draftId && item.userId === input.userId
      );

      if (!draft) {
        return {
          error: "draft_not_found",
          draftId: input.draftId
        };
      }

      if (draft.status !== "ready") {
        return {
          error: "draft_not_ready",
          draft: clone(draft)
        };
      }

      const submission = this.createSubmission({
        petId: draft.petId,
        userId: draft.userId,
        ownershipClaimId: draft.ownershipClaimId,
        scoreReportId: draft.scoreReportId,
        importDraftId: draft.id
      });

      draft.status = "submitted";
      draft.submissionId = submission.id;
      draft.submittedAt = nowIso();

      return {
        draft: clone(draft),
        submission
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
        importDraftId: input.importDraftId ?? "",
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

      if (!ALLOWED_REVIEW_STATUSES.includes(input.status)) {
        return {
          error: "invalid_review_status",
          submissionId: submission.id,
          status: input.status,
          allowedStatuses: [...ALLOWED_REVIEW_STATUSES]
        };
      }

      if (TERMINAL_SUBMISSION_STATUSES.has(submission.status)) {
        return {
          error: "submission_terminal",
          submissionId: submission.id,
          status: submission.status
        };
      }

      if (!isValidExplicitRewardAmount(input.rewardAmount)) {
        return {
          error: "invalid_reward_amount",
          submissionId: submission.id,
          rewardAmount: input.rewardAmount
        };
      }

      submission.status = input.status;

      let rewardEntry = null;
      let rewardReversalEntry = null;
      const scoreReport = state.scoreReports.find(
        (item) => item.reportId === submission.scoreReportId
      );
      const recommendedAmount =
        scoreReport?.rewardRecommendation?.grant === true
          ? scoreReport.rewardRecommendation.amount
          : 0;
      const rewardAmount =
        typeof input.rewardAmount === "number" ? input.rewardAmount : recommendedAmount;

      if (input.status === "approved" && rewardAmount > 0) {
        rewardEntry = {
          schema: "gamer.currency-ledger-entry.v1",
          entryId: nextId("ledger-review", state.ledgerEntries),
          userId: submission.userId,
          amount: rewardAmount,
          sourceType: "submission-reward",
          sourceId: submission.id,
          status: "posted",
          createdAt: nowIso()
        };
        state.ledgerEntries.push(rewardEntry);
      }

      if (input.status === "approved" && submission.importDraftId) {
        const feedPostId = `post-${submission.id}`;
        const alreadyPublished = state.feedPosts.some((post) => post.id === feedPostId);

        if (!alreadyPublished) {
          const draft = state.importDrafts.find(
            (item) => item.id === submission.importDraftId
          );
          state.feedPosts.unshift(
            createFeedPostFromApprovedImport(submission, draft, scoreReport, rewardEntry)
          );
        }

        const alreadyRegistered = state.approvedPets.some(
          (pet) => pet.petId === submission.petId
        );
        if (!alreadyRegistered) {
          const draft = state.importDrafts.find(
            (item) => item.id === submission.importDraftId
          );
          state.approvedPets.unshift(
            createApprovedPetFromImport(submission, draft, scoreReport)
          );
        }
      }

      if (input.status === "revoked") {
        const outstandingReward = sumPostedSubmissionReward(
          state.ledgerEntries,
          submission.id
        );

        if (outstandingReward > 0) {
          rewardReversalEntry = {
            schema: "gamer.currency-ledger-entry.v1",
            entryId: nextId("ledger-reversal", state.ledgerEntries),
            userId: submission.userId,
            amount: -outstandingReward,
            sourceType: "submission-reward-reversal",
            sourceId: submission.id,
            status: "posted",
            createdAt: nowIso()
          };
          state.ledgerEntries.push(rewardReversalEntry);
        }

        if (submission.importDraftId) {
          const feedPostId = `post-${submission.id}`;
          state.feedPosts = state.feedPosts.filter((post) => post.id !== feedPostId);
          state.approvedPets = state.approvedPets.filter(
            (pet) => pet.submissionId !== submission.id
          );
        }
      }

      const review = {
        submissionId: submission.id,
        status: input.status,
        reviewer: input.reviewer,
        rewardEntryId: rewardEntry?.entryId ?? "",
        rewardReversalEntryId: rewardReversalEntry?.entryId ?? "",
        reviewedAt: nowIso()
      };
      state.reviewQueue.push(review);

      return {
        ...clone(review),
        rewardEntry: clone(rewardEntry),
        rewardReversalEntry: clone(rewardReversalEntry)
      };
    }
  };
}
