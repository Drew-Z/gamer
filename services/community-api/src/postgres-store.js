import crypto from "node:crypto";
import { createDatabaseConfig } from "./database/config.js";
import { createPgClientOptions } from "./database/pg-options.js";
import { createScoreReportFromImportDraft } from "./scoring.js";
import {
  ALLOWED_REVIEW_STATUSES,
  TERMINAL_SUBMISSION_STATUSES,
  clone,
  createApprovedPetFromImport,
  createDefaultCommunityState,
  createFeedPostFromApprovedImport,
  createImportSummaryFromFantasyPetPackage,
  createImportSummaryFromPetPackageBundle,
  createPublicApprovedPet,
  createPublicSubmissionsSummary,
  draftStatusFromReadiness,
  isValidExplicitRewardAmount,
  normalizeCommunityState,
  submissionRewardLedgerEntries,
  sumPostedLedger,
  sumPostedSubmissionReward,
  validateFantasyPetPackageImport
} from "./store.js";

const jsonb = (value) => JSON.stringify(value ?? {});
const nowIso = () => new Date().toISOString();
const hasText = (value) => typeof value === "string" && value.trim() !== "";
const timestampIso = (value) =>
  value instanceof Date ? value.toISOString() : String(value ?? "");
const dateText = (value) => {
  if (value instanceof Date) {
    return value.toISOString().slice(0, 10);
  }
  return String(value ?? "").slice(0, 10);
};
const shortId = () => crypto.randomBytes(5).toString("hex");
const nextId = (prefix) =>
  `${prefix}-${new Date().toISOString().replace(/[-:.]/gu, "").replace("T", "-").replace("Z", "")}-${shortId()}`;

async function createPool(env, poolConfig) {
  if (poolConfig) {
    return poolConfig;
  }

  const config = createDatabaseConfig(env);
  if (config.mode !== "postgres") {
    throw new Error("DATABASE_URL is required for Postgres community store.");
  }
  const pg = await import("pg");
  const { Pool } = pg.default ?? pg;
  return new Pool(createPgClientOptions(config));
}

function rowJson(value, fallback = {}) {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value
    : fallback;
}

function mapUser(row) {
  return {
    id: row.id,
    displayName: row.display_name,
    handle: row.handle ?? "",
    equippedPetId: row.equipped_pet_id ?? ""
  };
}

function mapFeedPost(row) {
  return {
    id: row.id,
    authorId: row.author_id,
    petId: row.pet_id,
    title: row.title,
    body: row.body,
    reactionCount: Number(row.reaction_count ?? 0),
    createdAt: timestampIso(row.created_at),
    metadata: rowJson(row.metadata)
  };
}

function mapLedgerEntry(row) {
  return {
    schema: "gamer.currency-ledger-entry.v1",
    entryId: row.entry_id,
    userId: row.user_id,
    amount: Number(row.amount ?? 0),
    sourceType: row.source_type,
    sourceId: row.source_id,
    status: row.status,
    createdAt: timestampIso(row.created_at)
  };
}

function mapCheckIn(row) {
  return {
    userId: row.user_id,
    date: dateText(row.check_in_date),
    claimed: Boolean(row.claimed),
    rewardAmount: Number(row.reward_amount ?? 10),
    ledgerEntryId: row.ledger_entry_id ?? ""
  };
}

function mapImportDraft(row) {
  return {
    id: row.id,
    userId: row.user_id,
    status: row.status,
    readiness: rowJson(row.readiness),
    importSummary: rowJson(row.import_summary),
    petId: row.pet_id,
    ownershipClaimId: row.ownership_claim_id ?? "",
    scoreReportId: row.score_report_id ?? "",
    submissionId: row.submission_id ?? "",
    createdAt: timestampIso(row.created_at),
    submittedAt: row.submitted_at ? timestampIso(row.submitted_at) : undefined
  };
}

function mapScoreReport(row) {
  if (!row) {
    return null;
  }

  return {
    ...rowJson(row.report),
    reportId: row.report_id,
    petId: row.pet_id
  };
}

function mapSubmission(row) {
  return {
    id: row.id,
    petId: row.pet_id,
    userId: row.user_id,
    status: row.status,
    scoreReportId: row.score_report_id ?? "",
    ownershipClaimId: row.ownership_claim_id ?? "",
    importDraftId: row.import_draft_id ?? "",
    submittedAt: timestampIso(row.submitted_at)
  };
}

function mapReview(row) {
  return {
    submissionId: row.submission_id,
    status: row.status,
    reviewer: row.reviewer,
    rewardEntryId: row.reward_entry_id ?? "",
    rewardReversalEntryId: row.reward_reversal_entry_id ?? "",
    reviewedAt: timestampIso(row.reviewed_at)
  };
}

function mapApprovedPet(row) {
  return {
    petId: row.pet_id,
    displayName: row.display_name,
    ownerUserId: row.owner_user_id,
    source: rowJson(row.source),
    assets: rowJson(row.assets),
    submissionId: row.submission_id,
    importDraftId: row.import_draft_id,
    scoreReportId: row.score_report_id,
    totalScore: Number(row.total_score ?? 0),
    approvedAt: timestampIso(row.approved_at)
  };
}

async function queryState(client) {
  const [
    usersResult,
    feedResult,
    ledgerResult,
    checkInResult,
    submissionResult,
    reviewResult,
    draftResult,
    scoreResult,
    approvedResult
  ] = await Promise.all([
    client.query("select * from users order by created_at asc, id asc"),
    client.query("select * from feed_posts order by created_at desc, id asc"),
    client.query("select * from wallet_ledger_entries order by created_at asc, entry_id asc"),
    client.query("select * from daily_check_ins order by check_in_date asc, user_id asc"),
    client.query("select * from submissions order by submitted_at asc, id asc"),
    client.query("select * from review_decisions order by reviewed_at asc, id asc"),
    client.query("select * from import_drafts order by created_at asc, id asc"),
    client.query("select * from score_reports order by created_at asc, report_id asc"),
    client.query("select * from approved_pets order by approved_at desc, pet_id asc")
  ]);

  return {
    users: usersResult.rows.map(mapUser),
    feedPosts: feedResult.rows.map(mapFeedPost),
    ledgerEntries: ledgerResult.rows.map(mapLedgerEntry),
    checkIns: checkInResult.rows.map(mapCheckIn),
    submissions: submissionResult.rows.map(mapSubmission),
    reviewQueue: reviewResult.rows.map(mapReview),
    importDrafts: draftResult.rows.map(mapImportDraft),
    scoreReports: scoreResult.rows.map(mapScoreReport),
    approvedPets: approvedResult.rows.map(mapApprovedPet)
  };
}

async function insertSeedState(client, seed) {
  for (const user of seed.users) {
    await client.query(
      `insert into users (id, display_name, handle, equipped_pet_id)
       values ($1, $2, $3, $4)
       on conflict (id) do nothing`,
      [user.id, user.displayName, user.handle ?? "", user.equippedPetId ?? ""]
    );
  }

  for (const entry of seed.ledgerEntries) {
    await client.query(
      `insert into wallet_ledger_entries
        (entry_id, user_id, amount, source_type, source_id, status, created_at)
       values ($1, $2, $3, $4, $5, $6, $7)
       on conflict (entry_id) do nothing`,
      [
        entry.entryId,
        entry.userId,
        entry.amount,
        entry.sourceType,
        entry.sourceId,
        entry.status,
        entry.createdAt
      ]
    );
  }

  for (const checkIn of seed.checkIns) {
    await client.query(
      `insert into daily_check_ins
        (user_id, check_in_date, claimed, reward_amount, ledger_entry_id)
       values ($1, $2, $3, $4, $5)
       on conflict (user_id, check_in_date) do nothing`,
      [
        checkIn.userId,
        checkIn.date,
        Boolean(checkIn.claimed),
        Number(checkIn.rewardAmount ?? 10),
        checkIn.ledgerEntryId ?? ""
      ]
    );
  }

  for (const post of seed.feedPosts) {
    await client.query(
      `insert into feed_posts
        (id, author_id, pet_id, title, body, reaction_count, created_at, metadata)
       values ($1, $2, $3, $4, $5, $6, $7, $8::jsonb)
       on conflict (id) do nothing`,
      [
        post.id,
        post.authorId,
        post.petId,
        post.title,
        post.body,
        Number(post.reactionCount ?? 0),
        post.createdAt,
        jsonb(post.metadata ?? {})
      ]
    );
  }

  for (const report of seed.scoreReports) {
    await client.query(
      `insert into score_reports (report_id, pet_id, report)
       values ($1, $2, $3::jsonb)
       on conflict (report_id) do nothing`,
      [report.reportId, report.petId ?? "", jsonb(report)]
    );
  }

  for (const draft of seed.importDrafts) {
    await client.query(
      `insert into import_drafts
        (id, user_id, status, readiness, import_summary, pet_id, ownership_claim_id,
         score_report_id, submission_id, created_at, submitted_at)
       values ($1, $2, $3, $4::jsonb, $5::jsonb, $6, $7, $8, $9, $10, $11)
       on conflict (id) do nothing`,
      [
        draft.id,
        draft.userId,
        draft.status,
        jsonb(draft.readiness ?? {}),
        jsonb(draft.importSummary ?? {}),
        draft.petId ?? "",
        draft.ownershipClaimId ?? "",
        draft.scoreReportId ?? "",
        draft.submissionId ?? "",
        draft.createdAt ?? nowIso(),
        draft.submittedAt ?? null
      ]
    );
  }

  for (const submission of seed.submissions) {
    await client.query(
      `insert into submissions
        (id, pet_id, user_id, status, score_report_id, ownership_claim_id,
         import_draft_id, submitted_at)
       values ($1, $2, $3, $4, $5, $6, $7, $8)
       on conflict (id) do nothing`,
      [
        submission.id,
        submission.petId,
        submission.userId,
        submission.status,
        submission.scoreReportId ?? "",
        submission.ownershipClaimId ?? "",
        submission.importDraftId ?? "",
        submission.submittedAt
      ]
    );
  }

  for (const review of seed.reviewQueue) {
    await client.query(
      `insert into review_decisions
        (submission_id, status, reviewer, reward_entry_id, reward_reversal_entry_id, reviewed_at)
       values ($1, $2, $3, $4, $5, $6)`,
      [
        review.submissionId,
        review.status,
        review.reviewer,
        review.rewardEntryId ?? "",
        review.rewardReversalEntryId ?? "",
        review.reviewedAt
      ]
    );
  }

  for (const pet of seed.approvedPets) {
    await client.query(
      `insert into approved_pets
        (pet_id, display_name, owner_user_id, source, assets, submission_id,
         import_draft_id, score_report_id, total_score, approved_at)
       values ($1, $2, $3, $4::jsonb, $5::jsonb, $6, $7, $8, $9, $10)
       on conflict (pet_id) do nothing`,
      [
        pet.petId,
        pet.displayName,
        pet.ownerUserId,
        jsonb(pet.source ?? {}),
        jsonb(pet.assets ?? {}),
        pet.submissionId,
        pet.importDraftId,
        pet.scoreReportId,
        Number(pet.totalScore ?? 0),
        pet.approvedAt ?? nowIso()
      ]
    );
  }
}

export function createPostgresCommunityStore(options = {}) {
  const env = options.env ?? process.env;
  const seed = normalizeCommunityState(options.seed ?? createDefaultCommunityState());
  let poolPromise;
  let seedPromise;

  const getPool = () => {
    poolPromise ??= createPool(env, options.pool);
    return poolPromise;
  };

  const ensureSeeded = () => {
    seedPromise ??= seedDatabase();
    return seedPromise;
  };

  const seedDatabase = async () => {
    const pool = await getPool();
    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      const result = await client.query("select count(*)::int as count from users");
      if (Number(result.rows[0]?.count ?? 0) === 0) {
        await insertSeedState(client, seed);
      }
      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  };

  const withTransaction = async (callback) => {
    await ensureSeeded();
    const pool = await getPool();
    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      const result = await callback(client);
      await client.query("COMMIT");
      return result;
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  };

  const query = async (sql, params = []) => {
    await ensureSeeded();
    const pool = await getPool();
    return pool.query(sql, params);
  };

  const store = {
    async close() {
      const pool = await getPool();
      await pool.end();
    },

    async getStateSnapshot() {
      await ensureSeeded();
      const pool = await getPool();
      return clone(await queryState(pool));
    },

    async getMe() {
      const result = await query(
        "select * from users order by created_at asc, id asc limit 1"
      );
      return clone(mapUser(result.rows[0]));
    },

    async getFeed() {
      const result = await query(
        "select * from feed_posts order by created_at desc, id asc"
      );
      return {
        items: result.rows.map(mapFeedPost),
        nextCursor: "fixture-page-2"
      };
    },

    async listApprovedPets() {
      const result = await query(
        "select * from approved_pets order by approved_at desc, pet_id asc"
      );
      return {
        items: result.rows.map(mapApprovedPet).map(createPublicApprovedPet)
      };
    },

    async getApprovedPetPackage(petId) {
      const result = await query(
        "select * from approved_pets where pet_id = $1",
        [petId]
      );
      const pet = result.rows[0] ? mapApprovedPet(result.rows[0]) : null;
      if (!pet) {
        return null;
      }

      const publicPet = createPublicApprovedPet(pet);
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
          targetDownloadId: publicPet.assets.targetDownloadId,
          previewUrl: publicPet.assets.previewUrl,
          motionSheetCount: Number(pet.assets?.motionSheetCount ?? 0)
        },
        source: clone(publicPet.source ?? {}),
        submissionId: pet.submissionId,
        importDraftId: pet.importDraftId,
        scoreReportId: pet.scoreReportId
      };
    },

    async getWallet(userId) {
      const result = await query(
        `select * from wallet_ledger_entries
         where user_id = $1
         order by created_at asc, entry_id asc`,
        [userId]
      );
      const ledgerEntries = result.rows.map(mapLedgerEntry);
      return {
        userId,
        balance: sumPostedLedger(ledgerEntries, userId),
        currencyCode: "petcoin",
        ledgerEntries
      };
    },

    async getCommunityHome(userId, date = new Date().toISOString().slice(0, 10)) {
      await ensureSeeded();
      const pool = await getPool();
      const [
        feed,
        wallet,
        approvedPets,
        checkInResult,
        submissionResult
      ] = await Promise.all([
        store.getFeed(),
        store.getWallet(userId),
        store.listApprovedPets(),
        pool.query(
          `select * from daily_check_ins
           where user_id = $1 and check_in_date = $2`,
          [userId, date]
        ),
        pool.query(
          `select * from submissions
           where user_id = $1
           order by submitted_at asc, id asc`,
          [userId]
        )
      ]);
      const existingCheckIn = checkInResult.rows[0]
        ? mapCheckIn(checkInResult.rows[0])
        : null;

      return {
        schema: "gamer.community-home.v1",
        userId,
        feed,
        wallet,
        approvedPets,
        dailyCheckIn: {
          date,
          claimed: Boolean(existingCheckIn?.claimed),
          rewardAmount: Number(existingCheckIn?.rewardAmount ?? 10),
          ledgerEntryId: existingCheckIn?.ledgerEntryId ?? ""
        },
        submissionsSummary: createPublicSubmissionsSummary(
          submissionResult.rows.map(mapSubmission)
        )
      };
    },

    async claimDailyCheckIn(userId, date) {
      const result = await withTransaction(async (client) => {
        const existingResult = await client.query(
          `select * from daily_check_ins
           where user_id = $1 and check_in_date = $2
           for update`,
          [userId, date]
        );
        if (existingResult.rows[0]) {
          const existing = mapCheckIn(existingResult.rows[0]);
          const entryResult = existing.ledgerEntryId
            ? await client.query(
                "select * from wallet_ledger_entries where entry_id = $1",
                [existing.ledgerEntryId]
              )
            : { rows: [] };
          return {
            checkIn: existing,
            ledgerEntry: entryResult.rows[0] ? mapLedgerEntry(entryResult.rows[0]) : null
          };
        }

        const ledgerEntry = {
          schema: "gamer.currency-ledger-entry.v1",
          entryId: `ledger-checkin-${userId}-${date}`,
          userId,
          amount: 10,
          sourceType: "daily-checkin",
          sourceId: `checkin-${userId}-${date}`,
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

        await client.query(
          `insert into wallet_ledger_entries
            (entry_id, user_id, amount, source_type, source_id, status, created_at)
           values ($1, $2, $3, $4, $5, $6, $7)
           on conflict (entry_id) do nothing`,
          [
            ledgerEntry.entryId,
            ledgerEntry.userId,
            ledgerEntry.amount,
            ledgerEntry.sourceType,
            ledgerEntry.sourceId,
            ledgerEntry.status,
            ledgerEntry.createdAt
          ]
        );
        await client.query(
          `insert into daily_check_ins
            (user_id, check_in_date, claimed, reward_amount, ledger_entry_id)
           values ($1, $2, $3, $4, $5)
           on conflict (user_id, check_in_date) do nothing`,
          [userId, date, true, 10, ledgerEntry.entryId]
        );

        return {
          checkIn,
          ledgerEntry
        };
      });

      return {
        checkIn: clone(result.checkIn),
        wallet: await store.getWallet(userId),
        ledgerEntry: clone(result.ledgerEntry)
      };
    },

    async listSubmissions() {
      const [submissionResult, reviewResult] = await Promise.all([
        query("select * from submissions order by submitted_at asc, id asc"),
        query("select * from review_decisions order by reviewed_at asc, id asc")
      ]);
      return {
        submissions: submissionResult.rows.map(mapSubmission),
        reviewQueue: reviewResult.rows.map(mapReview)
      };
    },

    async getSubmission(submissionId) {
      const result = await query("select * from submissions where id = $1", [
        submissionId
      ]);
      return result.rows[0] ? clone(mapSubmission(result.rows[0])) : null;
    },

    async listAdminReviewQueue() {
      await ensureSeeded();
      const pool = await getPool();
      const state = await queryState(pool);

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

    async listImportDrafts(userId) {
      const result = await query(
        `select * from import_drafts
         where user_id = $1
         order by created_at asc, id asc`,
        [userId]
      );
      return {
        drafts: result.rows.map(mapImportDraft)
      };
    },

    async getScoreReport(scoreReportId) {
      const result = await query("select * from score_reports where report_id = $1", [
        scoreReportId
      ]);
      return clone(mapScoreReport(result.rows[0]));
    },

    async createImportDraft(input) {
      return withTransaction(async (client) => {
        const draft = {
          id: nextId("import-draft-db"),
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
          draft.scoreReportId = scoreReport.reportId;
          await client.query(
            `insert into score_reports (report_id, pet_id, report)
             values ($1, $2, $3::jsonb)
             on conflict (report_id) do nothing`,
            [scoreReport.reportId, scoreReport.petId ?? draft.petId, jsonb(scoreReport)]
          );
        } else if (!draft.scoreReportId) {
          const scoreReport = createScoreReportFromImportDraft(draft);
          await client.query(
            `insert into score_reports (report_id, pet_id, report)
             values ($1, $2, $3::jsonb)
             on conflict (report_id) do nothing`,
            [scoreReport.reportId, scoreReport.petId, jsonb(scoreReport)]
          );
          draft.scoreReportId = scoreReport.reportId;
        }

        await client.query(
          `insert into import_drafts
            (id, user_id, status, readiness, import_summary, pet_id,
             ownership_claim_id, score_report_id, created_at)
           values ($1, $2, $3, $4::jsonb, $5::jsonb, $6, $7, $8, $9)`,
          [
            draft.id,
            draft.userId,
            draft.status,
            jsonb(draft.readiness),
            jsonb(draft.importSummary),
            draft.petId,
            draft.ownershipClaimId,
            draft.scoreReportId,
            draft.createdAt
          ]
        );

        return clone(draft);
      });
    },

    async createImportDraftFromPetPackageBundle(input) {
      const ownerUserId = input.bundle.manifest.ownerUserId;
      if (input.userId !== ownerUserId) {
        return {
          error: "bundle_owner_mismatch",
          userId: input.userId,
          ownerUserId
        };
      }

      const existing = await query(
        `select id from import_drafts
         where user_id = $1 and pet_id = $2
         order by created_at asc
         limit 1`,
        [input.userId, input.bundle.manifest.petId]
      );
      if (existing.rows[0]) {
        return {
          error: "duplicate_import_draft",
          petId: input.bundle.manifest.petId,
          existingDraftId: existing.rows[0].id
        };
      }

      return store.createImportDraft({
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

    async createImportDraftFromFantasyPetPackage(input) {
      const validation = validateFantasyPetPackageImport(input);
      if (!validation.ok) {
        return {
          error: "invalid_fantasy_pet_package",
          validation
        };
      }

      const petId = input.packageManifest.appJobId.trim();
      const existing = await query(
        `select id from import_drafts
         where user_id = $1 and pet_id = $2
         order by created_at asc
         limit 1`,
        [input.userId, petId]
      );
      if (existing.rows[0]) {
        return {
          error: "duplicate_import_draft",
          petId,
          existingDraftId: existing.rows[0].id
        };
      }

      return store.createImportDraft({
        userId: input.userId,
        readiness: {
          status: "community-ready",
          reason: "human-reviewed fantasy pet package downloaded"
        },
        importSummary: createImportSummaryFromFantasyPetPackage(input),
        ownershipClaimId: input.ownershipClaimId ?? ""
      });
    },

    async submitImportDraft(input) {
      return withTransaction(async (client) => {
        const result = await client.query(
          `select * from import_drafts
           where id = $1 and user_id = $2
           for update`,
          [input.draftId, input.userId]
        );
        if (!result.rows[0]) {
          return {
            error: "draft_not_found",
            draftId: input.draftId
          };
        }

        const draft = mapImportDraft(result.rows[0]);
        if (draft.status !== "ready") {
          return {
            error: "draft_not_ready",
            draft: clone(draft)
          };
        }

        const submission = {
          id: nextId("submission-db"),
          petId: draft.petId,
          userId: draft.userId,
          status: "pending",
          scoreReportId: draft.scoreReportId,
          ownershipClaimId: draft.ownershipClaimId,
          importDraftId: draft.id,
          submittedAt: nowIso()
        };

        await client.query(
          `insert into submissions
            (id, pet_id, user_id, status, score_report_id,
             ownership_claim_id, import_draft_id, submitted_at)
           values ($1, $2, $3, $4, $5, $6, $7, $8)`,
          [
            submission.id,
            submission.petId,
            submission.userId,
            submission.status,
            submission.scoreReportId,
            submission.ownershipClaimId,
            submission.importDraftId,
            submission.submittedAt
          ]
        );
        await client.query(
          `update import_drafts
           set status = 'submitted', submission_id = $1, submitted_at = $2
           where id = $3`,
          [submission.id, submission.submittedAt, draft.id]
        );

        return {
          draft: {
            ...draft,
            status: "submitted",
            submissionId: submission.id,
            submittedAt: submission.submittedAt
          },
          submission
        };
      });
    },

    async createSubmission(input) {
      await ensureSeeded();
      const pool = await getPool();
      const submission = {
        id: nextId("submission-db"),
        petId: input.petId,
        userId: input.userId,
        status: "pending",
        scoreReportId: input.scoreReportId,
        ownershipClaimId: input.ownershipClaimId,
        importDraftId: input.importDraftId ?? "",
        submittedAt: nowIso()
      };

      await pool.query(
        `insert into submissions
          (id, pet_id, user_id, status, score_report_id,
           ownership_claim_id, import_draft_id, submitted_at)
         values ($1, $2, $3, $4, $5, $6, $7, $8)`,
        [
          submission.id,
          submission.petId,
          submission.userId,
          submission.status,
          submission.scoreReportId,
          submission.ownershipClaimId,
          submission.importDraftId,
          submission.submittedAt
        ]
      );
      return clone(submission);
    },

    async reviewSubmission(input) {
      return withTransaction(async (client) => {
        const submissionResult = await client.query(
          "select * from submissions where id = $1 for update",
          [input.submissionId]
        );
        if (!submissionResult.rows[0]) {
          return {
            error: "submission_not_found",
            submissionId: input.submissionId
          };
        }

        const submission = mapSubmission(submissionResult.rows[0]);
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

        await client.query("update submissions set status = $1 where id = $2", [
          input.status,
          submission.id
        ]);
        submission.status = input.status;

        const scoreResult = submission.scoreReportId
          ? await client.query("select * from score_reports where report_id = $1", [
              submission.scoreReportId
            ])
          : { rows: [] };
        const scoreReport = mapScoreReport(scoreResult.rows[0]);
        const recommendedAmount =
          scoreReport?.rewardRecommendation?.grant === true
            ? scoreReport.rewardRecommendation.amount
            : 0;
        const rewardAmount =
          typeof input.rewardAmount === "number" ? input.rewardAmount : recommendedAmount;

        let rewardEntry = null;
        let rewardReversalEntry = null;

        if (input.status === "approved" && rewardAmount > 0) {
          rewardEntry = {
            schema: "gamer.currency-ledger-entry.v1",
            entryId: nextId("ledger-review-db"),
            userId: submission.userId,
            amount: rewardAmount,
            sourceType: "submission-reward",
            sourceId: submission.id,
            status: "posted",
            createdAt: nowIso()
          };
          await client.query(
            `insert into wallet_ledger_entries
              (entry_id, user_id, amount, source_type, source_id, status, created_at)
             values ($1, $2, $3, $4, $5, $6, $7)`,
            [
              rewardEntry.entryId,
              rewardEntry.userId,
              rewardEntry.amount,
              rewardEntry.sourceType,
              rewardEntry.sourceId,
              rewardEntry.status,
              rewardEntry.createdAt
            ]
          );
        }

        if (input.status === "approved" && hasText(submission.importDraftId)) {
          const draftResult = await client.query(
            "select * from import_drafts where id = $1",
            [submission.importDraftId]
          );
          const draft = draftResult.rows[0] ? mapImportDraft(draftResult.rows[0]) : null;

          if (draft) {
            const feedPost = createFeedPostFromApprovedImport(
              submission,
              draft,
              scoreReport,
              rewardEntry
            );
            await client.query(
              `insert into feed_posts
                (id, author_id, pet_id, title, body, reaction_count, created_at, metadata)
               values ($1, $2, $3, $4, $5, $6, $7, $8::jsonb)
               on conflict (id) do nothing`,
              [
                feedPost.id,
                feedPost.authorId,
                feedPost.petId,
                feedPost.title,
                feedPost.body,
                feedPost.reactionCount,
                feedPost.createdAt,
                jsonb(feedPost.metadata ?? {})
              ]
            );

            const approvedPet = createApprovedPetFromImport(
              submission,
              draft,
              scoreReport
            );
            await client.query(
              `insert into approved_pets
                (pet_id, display_name, owner_user_id, source, assets, submission_id,
                 import_draft_id, score_report_id, total_score, approved_at)
               values ($1, $2, $3, $4::jsonb, $5::jsonb, $6, $7, $8, $9, $10)
               on conflict (pet_id) do nothing`,
              [
                approvedPet.petId,
                approvedPet.displayName,
                approvedPet.ownerUserId,
                jsonb(approvedPet.source ?? {}),
                jsonb(approvedPet.assets ?? {}),
                approvedPet.submissionId,
                approvedPet.importDraftId,
                approvedPet.scoreReportId,
                approvedPet.totalScore,
                approvedPet.approvedAt
              ]
            );
          }
        }

        if (input.status === "revoked") {
          const outstandingResult = await client.query(
            `select coalesce(sum(amount), 0)::int as total
             from wallet_ledger_entries
             where source_id = $1
               and status = 'posted'
               and source_type in ('submission-reward', 'submission-reward-reversal')`,
            [submission.id]
          );
          const outstandingReward = Number(outstandingResult.rows[0]?.total ?? 0);

          if (outstandingReward > 0) {
            rewardReversalEntry = {
              schema: "gamer.currency-ledger-entry.v1",
              entryId: nextId("ledger-reversal-db"),
              userId: submission.userId,
              amount: -outstandingReward,
              sourceType: "submission-reward-reversal",
              sourceId: submission.id,
              status: "posted",
              createdAt: nowIso()
            };
            await client.query(
              `insert into wallet_ledger_entries
                (entry_id, user_id, amount, source_type, source_id, status, created_at)
               values ($1, $2, $3, $4, $5, $6, $7)`,
              [
                rewardReversalEntry.entryId,
                rewardReversalEntry.userId,
                rewardReversalEntry.amount,
                rewardReversalEntry.sourceType,
                rewardReversalEntry.sourceId,
                rewardReversalEntry.status,
                rewardReversalEntry.createdAt
              ]
            );
          }

          if (hasText(submission.importDraftId)) {
            await client.query("delete from feed_posts where id = $1", [
              `post-${submission.id}`
            ]);
            await client.query("delete from approved_pets where submission_id = $1", [
              submission.id
            ]);
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
        await client.query(
          `insert into review_decisions
            (submission_id, status, reviewer, reward_entry_id,
             reward_reversal_entry_id, reviewed_at)
           values ($1, $2, $3, $4, $5, $6)`,
          [
            review.submissionId,
            review.status,
            review.reviewer,
            review.rewardEntryId,
            review.rewardReversalEntryId,
            review.reviewedAt
          ]
        );

        return {
          ...clone(review),
          rewardEntry: clone(rewardEntry),
          rewardReversalEntry: clone(rewardReversalEntry)
        };
      });
    }
  };

  return store;
}
