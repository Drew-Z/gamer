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
const isObject = (value) =>
  value !== null && typeof value === "object" && !Array.isArray(value);
const hasText = (value) => typeof value === "string" && value.trim() !== "";

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
    exportArtifactPath: bundle.manifest.assets.exportArtifact,
    motionSheets: clone(bundle.manifest.assets.motionSheets)
  }
});

const INTERNAL_PACKAGE_MARKERS = [
  "server_run.json",
  "artifact-index.json",
  "resolution-map",
  "desktop-pet-casebook-audit.json",
  "desktop-pet-stage-gate-report.json",
  "desktop-pet-learning-memory.json",
  "human-feedback-context.json",
  "genericagent-orchestrator-task.json",
  "codex-worker-task.json",
  "codex-worker-task.output.json",
  "*.invocation.json",
  ".invocation.json",
  "*.execution.json",
  ".execution.json",
  "*.output.json.adapterprovenance",
  ".output.json.adapterprovenance",
  "adapterprovenance",
  "directcodexcli",
  "strategy-plan.json",
  "codex-generation-directives.json",
  "server-proof-summary.json",
  "server-proof-summary",
  "realadapterlaunch",
  "humanacceptance",
  "server-generation-learning-drill.json",
  "server-generation-regression-report.json",
  "learning-ledger.jsonl",
  "route-policy-decision.json",
  "genericagent-ledger-suggestions.json",
  "genericagent-ledger-import.json",
  "stage-gate-ledger-import.json",
  "learning-drill",
  "learningprogress",
  "learningmemory",
  "learningmemoryresponse",
  "codexgenerationdirectiveresponse",
  "codexgenerationdirectiveresponsepresentcount",
  "codexgenerationdirectiveresponsesummary",
  "codexqaevidence",
  "directivehistoryresponse",
  "narrowedrepairfocus",
  "gadirectivehistoryresponse",
  "gadirectivehistoryresponsepresentcount",
  "gadirectivehistoryaddressedgenerationdirectivetext",
  "gadirectivehistorynarrowedrepairfocus",
  "gadirectivehistorynarrowedrepairfocuscounts",
  "directivehistorynarrowedrepairfocuscountdeltas",
  "repeateddirectivehistorynarrowedrepairfocus",
  "casebookreferencesused",
  "repairstrategies",
  "repairstrategiesused",
  "desktoppetlearningmemorysummary",
  "servergenerationlearningprogresssummary",
  "qualitygatestatus",
  "qualitygatestatuscounts",
  "qualitygatetrend",
  "learningassessment",
  "nextrepairfocus",
  "memorycarryforward",
  "learningmemoryinput",
  "learningmemoryoutput",
  "priormemorypresent",
  "priormemoryqualitygatestatus",
  "priormemoryscenariocount",
  "repeatedneedsrevisionstages",
  "repeatedhardfailuresobserved",
  "missingneedsrevisioncoverage",
  "missinghardfailurecoverage",
  "repaircoverage",
  "repairstrategyusecounts",
  "codex-action-attempt-n-server-imagegen-001",
  "stagegatereport",
  "stagegaterepair",
  "stagegaterepairrequests",
  "stagegatestatus",
  "learningledgersuggestions",
  "routeswitchrequired",
  "disabledroutes",
  "caseid",
  "referencetype",
  "strengthstopreserve",
  "reviewlessons",
  "regression-report",
  "agent-review.json",
  "orchestration-review.json",
  "runs/",
  "runs\\",
  "secret/",
  "secret\\",
  "targetoutput",
  "prompt-pack",
  "adapter-config"
];

const isOpaquePublicToken = (value) => {
  if (!hasText(value)) {
    return false;
  }
  const trimmed = value.trim();
  const lower = trimmed.toLowerCase();
  return !lower.startsWith("file:") &&
    !/^[A-Za-z]:[\\/]/.test(trimmed) &&
    !trimmed.includes("/") &&
    !trimmed.includes("\\") &&
    !trimmed.includes(":") &&
    INTERNAL_PACKAGE_MARKERS.every((marker) => !lower.includes(marker));
};

const isSafePackageFileName = (value) => {
  if (!hasText(value)) {
    return false;
  }
  const trimmed = value.trim();
  const lower = trimmed.toLowerCase();
  return lower.endsWith(".zip") &&
    !lower.startsWith("file:") &&
    !/^[A-Za-z]:[\\/]/.test(trimmed) &&
    !trimmed.includes("/") &&
    !trimmed.includes("\\") &&
    !trimmed.includes(":") &&
    INTERNAL_PACKAGE_MARKERS.every((marker) => !lower.includes(marker));
};

const isSafePackageRelativePath = (value) => {
  if (!hasText(value)) {
    return false;
  }
  const trimmed = value.trim();
  const lower = trimmed.toLowerCase();
  const segments = trimmed.split("/");
  return !lower.startsWith("file:") &&
    !/^[A-Za-z]:[\\/]/.test(trimmed) &&
    !trimmed.startsWith("/") &&
    !trimmed.includes("\\") &&
    !trimmed.includes(":") &&
    !segments.includes("..") &&
    INTERNAL_PACKAGE_MARKERS.every((marker) => !lower.includes(marker));
};

const validateFantasyPetPackageImport = (input) => {
  const errors = [];
  const manifest = input.packageManifest;

  if (!isObject(manifest)) {
    errors.push("packageManifest must be an object");
    return { ok: false, errors };
  }
  if (manifest.schema !== "fantasy-pet.package-manifest.v1") {
    errors.push("packageManifest.schema must be fantasy-pet.package-manifest.v1");
  }
  if (!hasText(manifest.appJobId)) {
    errors.push("packageManifest.appJobId must be a non-empty string");
  }
  if (!hasText(manifest.runId)) {
    errors.push("packageManifest.runId must be a non-empty string");
  }
  if (manifest.acceptedBy !== "human-review") {
    errors.push("packageManifest.acceptedBy must be human-review");
  }
  if (!isSafePackageFileName(input.packageFileName)) {
    errors.push("packageFileName must be a safe zip file name");
  }
  if (
    typeof input.packageByteCount !== "number" ||
    !Number.isFinite(input.packageByteCount) ||
    input.packageByteCount <= 0
  ) {
    errors.push("packageByteCount must be a positive number");
  }
  if (!isOpaquePublicToken(input.targetDownloadId)) {
    errors.push("targetDownloadId must be an opaque public artifact id");
  }
  if (
    hasText(manifest.sourceDownloadId) &&
    hasText(input.targetDownloadId) &&
    manifest.sourceDownloadId !== input.targetDownloadId
  ) {
    errors.push("targetDownloadId must match packageManifest.sourceDownloadId");
  }
  if (!Array.isArray(manifest.files) || manifest.files.length === 0) {
    errors.push("packageManifest.files must be a non-empty array");
  } else {
    manifest.files.forEach((file, index) => {
      if (!isObject(file)) {
        errors.push(`files[${index}] must be an object`);
        return;
      }
      if (!hasText(file.kind)) {
        errors.push(`files[${index}].kind must be a non-empty string`);
      }
      if (!isSafePackageRelativePath(file.path)) {
        errors.push(`files[${index}].path must be a safe package-relative path`);
      }
    });
  }

  return { ok: errors.length === 0, errors };
};

const createImportSummaryFromFantasyPetPackage = (input) => {
  const manifest = input.packageManifest;
  const candidateFiles = manifest.files
    .filter((file) => file.kind === "candidate")
    .map((file) => file.path);
  const petId = manifest.appJobId.trim();

  return {
    source: {
      petId,
      displayName: `Generated pet ${petId}`,
      schema: manifest.schema,
      kind: "fantasy-pet-rule",
      runId: manifest.runId,
      appJobId: manifest.appJobId,
      statePath: "",
      baseIdentityStatus: "accepted"
    },
    review: {
      blockers: [],
      previewDecision: "keep",
      exportStatus: "ready",
      acceptedBy: "human-review",
      targetDownloadId: input.targetDownloadId
    },
    assets: {
      previewPath: input.targetDownloadId,
      exportArtifactPath: input.packageFileName,
      packageByteCount: input.packageByteCount,
      motionSheets: clone(candidateFiles)
    },
    notes: [
      "imported from fantasy-pet public package manifest",
      "no server run paths were accepted from app input"
    ]
  };
};

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
    exportArtifactPath: draft?.importSummary?.assets?.exportArtifactPath ?? "",
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
    exportArtifactPath: draft?.importSummary?.assets?.exportArtifactPath ?? "",
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

const createPublicSubmissionsSummary = (submissionsForUser) => {
  const countStatus = (status) =>
    submissionsForUser.filter((submission) => submission.status === status).length;

  return {
    pendingCount: countStatus("pending"),
    approvedCount: countStatus("approved"),
    heldCount: countStatus("held"),
    rejectedCount: countStatus("rejected"),
    revokedCount: countStatus("revoked"),
    latest: clone(submissionsForUser.at(-1) ?? null)
  };
};

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

    getApprovedPetPackage(petId) {
      const pet = state.approvedPets.find((item) => item.petId === petId);

      if (!pet) {
        return null;
      }

      const exportArtifactPath = pet.assets?.exportArtifactPath ?? "";

      return {
        petId: pet.petId,
        displayName: pet.displayName,
        ownerUserId: pet.ownerUserId,
        package: {
          exportArtifactPath,
          status: exportArtifactPath ? "available" : "missing"
        },
        assets: {
          previewPath: pet.assets?.previewPath ?? "",
          motionSheetCount: Number(pet.assets?.motionSheetCount ?? 0)
        },
        source: clone(pet.source ?? {}),
        submissionId: pet.submissionId,
        importDraftId: pet.importDraftId,
        scoreReportId: pet.scoreReportId
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

    getCommunityHome(userId, date = new Date().toISOString().slice(0, 10)) {
      const existingCheckIn = state.checkIns.find(
        (checkIn) => checkIn.userId === userId && checkIn.date === date
      );
      const submissionsForUser = state.submissions.filter(
        (submission) => submission.userId === userId
      );

      return {
        schema: "gamer.community-home.v1",
        userId,
        feed: this.getFeed(),
        wallet: this.getWallet(userId),
        approvedPets: this.listApprovedPets(),
        dailyCheckIn: {
          date,
          claimed: Boolean(existingCheckIn?.claimed),
          rewardAmount: Number(existingCheckIn?.rewardAmount ?? 10),
          ledgerEntryId: existingCheckIn?.ledgerEntryId ?? ""
        },
        submissionsSummary: createPublicSubmissionsSummary(submissionsForUser)
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

    getSubmission(submissionId) {
      const submission = state.submissions.find((item) => item.id === submissionId);
      return clone(submission ?? null);
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

    createImportDraftFromFantasyPetPackage(input) {
      const validation = validateFantasyPetPackageImport(input);
      if (!validation.ok) {
        return {
          error: "invalid_fantasy_pet_package",
          validation
        };
      }

      const petId = input.packageManifest.appJobId.trim();
      const existingDraft = state.importDrafts.find(
        (draft) =>
          draft.userId === input.userId &&
          draft.petId === petId
      );
      if (existingDraft) {
        return {
          error: "duplicate_import_draft",
          petId,
          existingDraftId: existingDraft.id
        };
      }

      return this.createImportDraft({
        userId: input.userId,
        readiness: {
          status: "community-ready",
          reason: "human-reviewed fantasy pet package downloaded"
        },
        importSummary: createImportSummaryFromFantasyPetPackage(input),
        ownershipClaimId: input.ownershipClaimId ?? ""
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
