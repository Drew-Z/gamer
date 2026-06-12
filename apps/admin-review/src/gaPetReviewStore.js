import { appendFile, mkdir, readFile, readdir, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { createDatabaseConfig } from "../../../services/community-api/src/database/config.js";
import { listCommunityMigrations } from "../../../services/community-api/src/database/migrations.js";
import { createPgClientOptions } from "../../../services/community-api/src/database/pg-options.js";
import { runCommunityMigrations } from "../../../services/community-api/src/database/runner.js";

const DEFAULT_RUN_ROOT = path.resolve(
  "services",
  "pet-generator",
  "data",
  "ga-random-pets"
);

const textDecoder = new TextDecoder();

const ISSUE_GUIDANCE = {
  "identity-drift": "Lock the base identity, head/body ratio, colors, markings, ears, tail, and signature effects across every sheet.",
  "static-frames": "Make frames visibly different with clear key poses and avoid repeated near-identical frames.",
  "scale-pop": "Keep body size, center, and ground anchor stable across frames and between idle handoff.",
  "bad-transparency": "Use clean true alpha or clean chroma edges with no background patches.",
  "white-matte": "Avoid white matte contamination around fur, muzzle, paws, ears, and effects.",
  "cropped-body": "Keep full body, tail, ears, wings, props, and effects inside every frame.",
  "wrong-action": "Make the motion clearly match the requested desktop-pet trigger and action semantics.",
  "too-noisy": "Reduce particles and decorative fragments so the pet silhouette stays readable.",
  "weak-silhouette": "Strengthen small-size readability with clear silhouette, separated limbs, and simple effects.",
  "style-mismatch": "Keep the same render style, line weight, lighting, and material treatment as the accepted identity."
};

const EVIDENCE_FILES = [
  ["Prompt plan", "source/generation/prompt-plan.json", "prompt"],
  ["API trace", "source/generation/api-trace.json", "trace"],
  ["Motion map", "meta/motion_map.json", "motion"],
  ["Runtime", "meta/runtime.json", "runtime"],
  ["Review card", "review-card.md", "review"],
  ["Manifest", "manifest.json", "manifest"],
  ["Package manifest", "package-manifest.json", "manifest"],
  ["Score report", "score-report.json", "score"],
  ["Ownership claim", "ownership-claim.json", "ownership"],
  ["Video reference", "artifacts/video/motion-reference.mp4", "video"],
  ["Video operation", "artifacts/video/operation.json", "video-meta"],
  ["Latest feedback", "human-feedback-latest.json", "feedback"],
  ["Feedback log", "human-feedback.jsonl", "feedback"],
  ["Rework requests", "source/generation/rework-requests.jsonl", "rework"]
];

export function createGaPetReviewStore(options = {}) {
  const runRoot = path.resolve(options.runRoot || process.env.GA_PET_RUN_ROOT || DEFAULT_RUN_ROOT);
  const fileStore = createFileGaPetReviewStore({ runRoot });
  const databaseStore = createDatabaseGaPetReviewStore({
    env: options.env || process.env
  });

  if (!databaseStore) {
    return fileStore;
  }

  return createHybridGaPetReviewStore({
    databaseStore,
    fileStore
  });
}

function createFileGaPetReviewStore({ runRoot }) {
  const root = path.resolve(runRoot || DEFAULT_RUN_ROOT);

  return {
    runRoot: root,
    listCandidates: (input) => listCandidates({ runRoot: root, ...input }),
    readAsset: (input) => readAsset({ runRoot: root, ...input }),
    writeFeedback: (input) => writeFeedback({ runRoot: root, ...input })
  };
}

function createHybridGaPetReviewStore({ databaseStore, fileStore }) {
  return {
    runRoot: fileStore.runRoot,

    async listCandidates(input) {
      try {
        const databaseList = await databaseStore.listCandidates(input);
        if (databaseList.count > 0) {
          return databaseList;
        }
      } catch {
        // Keep the admin panel usable if Supabase/Data API configuration is still settling.
      }

      return fileStore.listCandidates(input);
    },

    async readAsset(input) {
      try {
        const asset = await databaseStore.readAsset(input);
        if (asset) {
          return asset;
        }
      } catch {
        // Fall back to the local run root when the requested candidate is local-only.
      }

      return fileStore.readAsset(input);
    },

    async writeFeedback(input) {
      const databaseResult = await databaseStore.writeFeedback(input);
      if (databaseResult) {
        return databaseResult;
      }

      return fileStore.writeFeedback(input);
    }
  };
}

function createDatabaseGaPetReviewStore({ env }) {
  let config;
  try {
    config = createDatabaseConfig(env);
  } catch {
    return null;
  }

  const supabaseUrl = String(env.SUPABASE_URL || "").trim().replace(/\/+$/u, "");
  const serviceKey = String(
    env.SUPABASE_SERVICE_ROLE_KEY ||
    env.SUPABASE_SECRET_KEY ||
    ""
  ).trim();
  const bucket = String(env.SUPABASE_STORAGE_BUCKET || "pet-assets").trim();

  if (config.mode !== "postgres" || !supabaseUrl || !serviceKey || !bucket) {
    return null;
  }

  let poolPromise;
  let migratePromise;

  const getPool = async () => {
    if (!poolPromise) {
      poolPromise = import("pg").then((pgModule) => {
        const { Pool } = pgModule.default ?? pgModule;
        return new Pool(createPgClientOptions(config));
      });
    }
    return poolPromise;
  };

  const ensureMigrated = async () => {
    if (!migratePromise) {
      migratePromise = getPool().then(async (pool) => {
        const client = await pool.connect();
        try {
          await runCommunityMigrations({
            client,
            migrations: listCommunityMigrations()
          });
        } finally {
          client.release();
        }
      });
    }
    return migratePromise;
  };

  const query = async (sql, params = []) => {
    await ensureMigrated();
    const pool = await getPool();
    return pool.query(sql, params);
  };

  return {
    async listCandidates({ limit = 40 } = {}) {
      const candidateResult = await query(
        `select *
         from ga_pet_candidates
         order by updated_at desc, run_id asc
         limit $1`,
        [limit]
      );
      const candidates = candidateResult.rows;
      const runIds = candidates.map((candidate) => candidate.run_id);

      if (runIds.length === 0) {
        return {
          schema: "gamer.ga-pet-review-list.v1",
          runRoot: "supabase:pet-assets",
          count: 0,
          summary: createDatabaseReviewSummary({
            totalCandidates: 0,
            shownCandidates: 0,
            candidates: [],
            feedbackRows: [],
            reworkRows: [],
            reworkStatusRows: []
          }),
          candidates: []
        };
      }

      const [assetResult, feedbackResult, reworkResult, reworkStatusResult, totalResult] =
        await Promise.all([
          query(
            `select *
             from ga_pet_assets
             where run_id = any($1::text[])
             order by run_id asc, kind asc, id asc`,
            [runIds]
          ),
          query(
            `select *
             from ga_pet_feedback
             where run_id = any($1::text[])
             order by created_at desc`,
            [runIds]
          ),
          query(
            `select *
             from ga_pet_rework_requests
             where source_run_id = any($1::text[])
             order by created_at desc`,
            [runIds]
          ),
          query(
            `select *
             from ga_pet_rework_statuses
             where source_run_id = any($1::text[]) or target_run_id = any($1::text[])
             order by created_at desc, id desc`,
            [runIds]
          ),
          query("select count(*)::int as count from ga_pet_candidates")
        ]);

      const assetsByRun = groupRowsBy(assetResult.rows, "run_id");
      const feedbackByRun = groupRowsBy(feedbackResult.rows, "run_id");
      const reworksByRun = groupRowsBy(reworkResult.rows, "source_run_id");
      const statuses = reworkStatusResult.rows.map(mapDatabaseReworkStatus);
      const lineageIndex = createDatabaseLineageIndex({
        reworkRows: reworkResult.rows,
        statusRows: reworkStatusResult.rows
      });
      const mappedCandidates = candidates.map((candidate) =>
        createDatabaseCandidateSummary({
          candidate,
          assets: assetsByRun.get(candidate.run_id) || [],
          feedbackRows: feedbackByRun.get(candidate.run_id) || [],
          reworkRows: reworksByRun.get(candidate.run_id) || [],
          lineageIndex
        })
      );

      return {
        schema: "gamer.ga-pet-review-list.v1",
        runRoot: "supabase:pet-assets",
        count: mappedCandidates.length,
        summary: createDatabaseReviewSummary({
          totalCandidates: Number(totalResult.rows[0]?.count ?? mappedCandidates.length),
          shownCandidates: mappedCandidates.length,
          candidates: mappedCandidates,
          feedbackRows: feedbackResult.rows,
          reworkRows: reworkResult.rows,
          reworkStatusRows: statuses
        }),
        candidates: mappedCandidates
      };
    },

    async readAsset({ runId, relativePath }) {
      const normalizedPath = normalizeRelativePath(relativePath);
      if (!normalizedPath) {
        return null;
      }

      const result = await query(
        `select *
         from ga_pet_assets
         where run_id = $1 and relative_path = $2
         limit 1`,
        [runId, normalizedPath]
      );
      const asset = result.rows[0];
      if (!asset?.storage_key) {
        return null;
      }

      const response = await fetch(
        `${supabaseUrl}/storage/v1/object/authenticated/${encodeURIComponent(asset.storage_bucket || bucket)}/${encodeStoragePath(asset.storage_key)}`,
        {
          headers: {
            apikey: serviceKey,
            Authorization: `Bearer ${serviceKey}`
          }
        }
      );

      if (!response.ok) {
        throw new Error(`supabase_storage_read_${response.status}`);
      }

      return {
        file: Buffer.from(await response.arrayBuffer()),
        contentType:
          response.headers.get("content-type") ||
          asset.content_type ||
          contentTypeFor(asset.relative_path)
      };
    },

    async writeFeedback({ runId, body }) {
      const exists = await query(
        "select run_id from ga_pet_candidates where run_id = $1 limit 1",
        [runId]
      );

      if (exists.rows.length === 0) {
        return null;
      }

      const feedback = createFeedbackRecord({ runId, body });
      const learningNote = createLearningNote(feedback);
      let reworkRequest = null;

      await query(
        `insert into ga_pet_feedback
          (feedback_id, run_id, reviewer, decision, severity, action_id,
           tags, notes, prompt_patch, rework_mode, metadata, created_at)
         values ($1, $2, $3, $4, $5, $6, $7::text[], $8, $9, $10, $11::jsonb, $12)
         on conflict (feedback_id) do nothing`,
        [
          feedback.feedbackId,
          feedback.runId,
          feedback.reviewer,
          feedback.decision,
          feedback.severity,
          feedback.actionId,
          feedback.tags,
          feedback.notes,
          feedback.promptPatch,
          feedback.reworkMode,
          JSON.stringify({
            schema: "gamer.ga-pet-feedback-metadata.v1",
            learningNote
          }),
          feedback.createdAt
        ]
      );

      if (["rework", "regenerate"].includes(feedback.decision) || feedback.reworkMode) {
        reworkRequest = createReworkRequest(feedback);
        await query(
          `insert into ga_pet_rework_requests
            (request_id, source_run_id, source_feedback_id, status, mode,
             action_id, tags, notes, prompt_patch, metadata, created_at, updated_at)
           values ($1, $2, $3, $4, $5, $6, $7::text[], $8, $9, $10::jsonb, $11, $11)
           on conflict (request_id) do nothing`,
          [
            reworkRequest.requestId,
            reworkRequest.sourceRunId,
            reworkRequest.sourceFeedbackId,
            reworkRequest.status,
            normalizeReworkMode(reworkRequest.mode),
            reworkRequest.actionId,
            reworkRequest.tags,
            reworkRequest.notes,
            reworkRequest.promptPatch,
            JSON.stringify({
              schema: "gamer.ga-pet-rework-request-metadata.v1",
              source: "admin-review"
            }),
            reworkRequest.createdAt
          ]
        );
      }

      return {
        ok: true,
        feedback,
        learningNote,
        reworkRequest
      };
    }
  };
}

async function listCandidates({ runRoot, limit = 40 } = {}) {
  const root = path.resolve(runRoot || DEFAULT_RUN_ROOT);
  const entries = await safeReadDirectory(root);
  const reworkRecords = await readJsonLinesIfExists(path.join(root, "ga-rework-queue.jsonl"));
  const lineageIndex = createReworkLineageIndex(reworkRecords);
  const directories = [];

  for (const entry of entries) {
    if (!entry.isDirectory()) continue;
    const runDir = path.join(root, entry.name);
    const packageManifest = await readJsonIfExists(path.join(runDir, "package-manifest.json"));
    const promptPlan = await readJsonIfExists(path.join(runDir, "source", "generation", "prompt-plan.json"));
    if (!packageManifest && !promptPlan) continue;
    const info = await stat(runDir);
    directories.push({
      runId: entry.name,
      runDir,
      packageManifest,
      promptPlan,
      updatedAt: info.mtimeMs
    });
  }

  directories.sort((left, right) => right.updatedAt - left.updatedAt);

  const candidates = [];
  for (const item of directories.slice(0, limit)) {
    candidates.push(await createCandidateSummary({
      runRoot: root,
      lineageIndex,
      ...item
    }));
  }

  return {
    schema: "gamer.ga-pet-review-list.v1",
    runRoot: root,
    count: candidates.length,
    summary: await createReviewSummary({
      runRoot: root,
      totalCandidates: directories.length,
      shownCandidates: candidates.length,
      candidates,
      reworkRecords
    }),
    candidates
  };
}

async function createReviewSummary({
  runRoot,
  totalCandidates,
  shownCandidates,
  candidates,
  reworkRecords
}) {
  const learningNotes = await readJsonLinesIfExists(path.join(runRoot, "ga-learning-notes.jsonl"));
  const queueRecords = Array.isArray(reworkRecords)
    ? reworkRecords
    : await readJsonLinesIfExists(path.join(runRoot, "ga-rework-queue.jsonl"));
  const reworkRequests = queueRecords.filter(
    (record) => record?.schema === "gamer.ga-pet-rework-request.v1"
  );
  const reworkStatuses = queueRecords.filter(
    (record) => record?.schema === "gamer.ga-pet-rework-status.v1"
  );
  const completed = idsWithStatus(reworkStatuses, "completed");
  const failed = idsWithStatus(reworkStatuses, "failed");
  const started = idsWithStatus(reworkStatuses, "started");
  const terminal = new Set([...completed, ...failed]);
  const queued = reworkRequests.filter(
    (request) => !started.has(request.requestId) && !terminal.has(request.requestId)
  );
  const running = reworkRequests.filter(
    (request) => started.has(request.requestId) && !terminal.has(request.requestId)
  );

  return {
    schema: "gamer.ga-pet-review-summary.v1",
    totalCandidates,
    shownCandidates,
    feedbackCount: candidates.reduce(
      (total, candidate) => total + (candidate.feedback?.count || 0),
      0
    ),
    learningNoteCount: learningNotes.length,
    decisions: countBy(
      learningNotes
        .map((note) => note.decision)
        .filter(Boolean)
    ),
    topTags: countBy(
      learningNotes.flatMap((note) => Array.isArray(note.tags) ? note.tags : [])
    ).slice(0, 8),
    rework: {
      requested: reworkRequests.length,
      queued: queued.length,
      running: running.length,
      completed: completed.size,
      failed: failed.size
    }
  };
}

async function createCandidateSummary({
  runRoot,
  runId,
  runDir,
  packageManifest,
  promptPlan,
  updatedAt,
  lineageIndex
}) {
  const motionMap = await readJsonIfExists(path.join(runDir, "meta", "motion_map.json"));
  const latestFeedback = await readJsonIfExists(path.join(runDir, "human-feedback-latest.json"));
  const feedbackEntries = await readJsonLinesIfExists(path.join(runDir, "human-feedback.jsonl"));
  const reworkRequests = await readJsonLinesIfExists(path.join(runDir, "source", "generation", "rework-requests.jsonl"));
  const evidenceFiles = await summarizeEvidenceFiles({ runDir, runId });
  const previewPath = await firstExistingRelativePath(runDir, [
    "previews/preview.png",
    "assets/base_identity.png",
    "artifacts/candidates/base-identity.png"
  ]);
  const packageFile = await firstExistingRelativePath(runDir, [
    `exports/${runId}-full-resource-candidate.zip`,
    `${runId}-identity-candidate.zip`,
    `${runId}-candidate.zip`
  ]);

  return {
    schema: "gamer.ga-pet-review-candidate.v1",
    runId,
    displayName: promptPlan?.name || runId,
    summary: promptPlan?.summary || "",
    species: promptPlan?.species || "",
    element: promptPlan?.element || "",
    status:
      packageManifest?.resourceStatus ||
      packageManifest?.qualityGate ||
      packageManifest?.acceptedBy ||
      "unknown",
    packageMode: promptPlan?.packageMode || "",
    backgroundMode: promptPlan?.backgroundMode || "",
    createdAt: packageManifest?.generatedBy?.createdAt || "",
    updatedAt: new Date(updatedAt).toISOString(),
    previewPath,
    previewUrl: previewPath
      ? gaReviewFileUrl({ runId, relativePath: previewPath })
      : "",
    packagePath: packageFile,
    packageUrl: packageFile
      ? gaReviewFileUrl({ runId, relativePath: packageFile })
      : "",
    motionSheets: summarizeMotionSheets(motionMap, runId),
    evidenceFiles,
    videoReferenceUrl: evidenceFiles.find((file) => file.path.endsWith(".mp4"))?.url || "",
    lineage: summarizeLineage({
      runId,
      promptPlan,
      lineageIndex
    }),
    feedback: {
      latest: latestFeedback,
      count: feedbackEntries.length,
      history: feedbackEntries.slice(-8).reverse().map(summarizeFeedbackEntry)
    },
    rework: {
      count: reworkRequests.length,
      latest: reworkRequests.at(-1) || null,
      requests: reworkRequests.slice(-8).reverse().map(summarizeReworkRequest)
    }
  };
}

async function readAsset({ runRoot, runId, relativePath }) {
  const filePath = safeRunPath({ runRoot, runId, relativePath });
  const file = await readFile(filePath);
  return {
    file,
    contentType: contentTypeFor(filePath)
  };
}

function createFeedbackRecord({ runId, body }) {
  const now = new Date().toISOString();
  const feedbackId = `feedback-${now.replace(/[-:.]/gu, "")}-${shortId()}`;
  const feedback = {
    schema: "gamer.ga-pet-human-feedback.v1",
    feedbackId,
    runId,
    createdAt: now,
    reviewer: "admin-ui",
    decision: normalizeField(body?.decision, "hold"),
    severity: normalizeField(body?.severity, "medium"),
    actionId: normalizeField(body?.actionId, ""),
    tags: parseTags(body?.tags),
    notes: normalizeField(body?.notes, ""),
    promptPatch: normalizeField(body?.promptPatch, ""),
    reworkMode: normalizeField(body?.reworkMode, "")
  };

  if (!feedback.notes && feedback.tags.length === 0) {
    throw new Error("Feedback needs notes or tags.");
  }

  return feedback;
}

function createLearningNote(feedback) {
  return {
    schema: "gamer.ga-pet-learning-note.v1",
    noteId: `lesson-${feedback.feedbackId}`,
    sourceRunId: feedback.runId,
    sourceFeedbackId: feedback.feedbackId,
    createdAt: feedback.createdAt,
    decision: feedback.decision,
    severity: feedback.severity,
    actionId: feedback.actionId,
    tags: feedback.tags,
    guidance: guidanceForTags(feedback.tags),
    lesson: buildLearningLesson(feedback)
  };
}

function createReworkRequest(feedback) {
  return {
    schema: "gamer.ga-pet-rework-request.v1",
    requestId: `rework-${feedback.feedbackId}`,
    sourceRunId: feedback.runId,
    sourceFeedbackId: feedback.feedbackId,
    createdAt: feedback.createdAt,
    status: "requested",
    mode: feedback.reworkMode || feedback.decision,
    actionId: feedback.actionId,
    tags: feedback.tags,
    notes: feedback.notes,
    promptPatch: feedback.promptPatch
  };
}

async function writeFeedback({ runRoot, runId, body }) {
  const runDir = safeRunDirectory({ runRoot, runId });
  const runInfo = await stat(runDir);
  if (!runInfo.isDirectory()) {
    throw new Error("GA pet candidate is not a directory.");
  }

  const feedback = createFeedbackRecord({ runId, body });

  const feedbackLine = `${JSON.stringify(feedback)}\n`;
  await appendFile(path.join(runDir, "human-feedback.jsonl"), feedbackLine, "utf8");
  await writeFile(
    path.join(runDir, "human-feedback-latest.json"),
    `${JSON.stringify(feedback, null, 2)}\n`,
    "utf8"
  );

  const learningNote = createLearningNote(feedback);
  await appendFile(
    path.join(path.resolve(runRoot), "ga-learning-notes.jsonl"),
    `${JSON.stringify(learningNote)}\n`,
    "utf8"
  );

  let reworkRequest = null;
  if (["rework", "regenerate"].includes(feedback.decision) || feedback.reworkMode) {
    reworkRequest = createReworkRequest(feedback);
    await mkdir(path.join(runDir, "source", "generation"), { recursive: true });
    await appendFile(
      path.join(runDir, "source", "generation", "rework-requests.jsonl"),
      `${JSON.stringify(reworkRequest)}\n`,
      "utf8"
    );
    await appendFile(
      path.join(path.resolve(runRoot), "ga-rework-queue.jsonl"),
      `${JSON.stringify(reworkRequest)}\n`,
      "utf8"
    );
  }

  return {
    ok: true,
    feedback,
    learningNote,
    reworkRequest
  };
}

function summarizeMotionSheets(motionMap, runId = "") {
  const actions = motionMap?.actions && typeof motionMap.actions === "object"
    ? motionMap.actions
    : {};
  return Object.entries(actions).map(([actionId, action]) => ({
    actionId,
    sheet: action.sheet || "",
    failure: action.failure || "",
    status: action.status || "",
    frames: action.frames || 0,
    loop: Boolean(action.loop),
    category: action.category || "",
    trigger: action.trigger || "",
    imageUrl: action.sheet && runId
      ? gaReviewFileUrl({ runId, relativePath: action.sheet })
      : ""
  }));
}

async function summarizeEvidenceFiles({ runDir, runId }) {
  const files = [];
  for (const [label, relativePath, kind] of EVIDENCE_FILES) {
    try {
      const info = await stat(path.join(runDir, relativePath));
      if (!info.isFile()) continue;
      files.push({
        label,
        kind,
        path: relativePath,
        url: gaReviewFileUrl({ runId, relativePath }),
        sizeBytes: info.size,
        updatedAt: new Date(info.mtimeMs).toISOString()
      });
    } catch (error) {
      if (error?.code !== "ENOENT") throw error;
    }
  }
  return files;
}

function createDatabaseCandidateSummary({
  candidate,
  assets,
  feedbackRows,
  reworkRows,
  lineageIndex
}) {
  const runId = candidate.run_id;
  const promptPlan = candidate.prompt_plan || {};
  const packageManifest = candidate.package_manifest || {};
  const motionMap = candidate.motion_map || {};
  const previewAsset =
    findAssetByStorageKey(assets, candidate.preview_storage_key) ||
    assets.find((asset) => asset.kind === "preview") ||
    assets.find((asset) => asset.relative_path === "previews/preview.png");
  const packageAsset =
    findAssetByStorageKey(assets, candidate.package_storage_key) ||
    assets.find((asset) => asset.kind === "package");
  const feedbackEntries = feedbackRows.map(mapDatabaseFeedback).sort(compareCreatedAsc);
  const reworkRequests = reworkRows.map(mapDatabaseReworkRequest).sort(compareCreatedAsc);
  const evidenceFiles = assets.map((asset) => ({
    label: asset.label || labelFromPath(asset.relative_path),
    kind: asset.kind || "asset",
    path: asset.relative_path,
    url: gaReviewFileUrl({ runId, relativePath: asset.relative_path }),
    sizeBytes: Number(asset.byte_count || 0),
    updatedAt: dateIso(asset.created_at)
  }));

  return {
    schema: "gamer.ga-pet-review-candidate.v1",
    runId,
    displayName: candidate.display_name || promptPlan.name || runId,
    summary: candidate.summary || promptPlan.summary || "",
    species: candidate.species || promptPlan.species || "",
    element: candidate.element || promptPlan.element || "",
    status:
      candidate.status ||
      packageManifest.resourceStatus ||
      packageManifest.qualityGate ||
      "unknown",
    packageMode: candidate.package_mode || promptPlan.packageMode || "",
    backgroundMode: candidate.background_mode || promptPlan.backgroundMode || "",
    createdAt: dateIso(candidate.created_at || packageManifest.generatedBy?.createdAt),
    updatedAt: dateIso(candidate.updated_at),
    previewPath: previewAsset?.relative_path || "",
    previewUrl: previewAsset
      ? gaReviewFileUrl({ runId, relativePath: previewAsset.relative_path })
      : "",
    packagePath: packageAsset?.relative_path || "",
    packageUrl: packageAsset
      ? gaReviewFileUrl({ runId, relativePath: packageAsset.relative_path })
      : "",
    motionSheets: summarizeMotionSheets(motionMap, runId),
    evidenceFiles,
    videoReferenceUrl: evidenceFiles.find((file) => file.path.endsWith(".mp4"))?.url || "",
    lineage: summarizeLineage({
      runId,
      promptPlan: {
        ...promptPlan,
        sourceRunId: candidate.source_run_id || promptPlan.sourceRunId || "",
        reworkRequestId: candidate.rework_request_id || promptPlan.reworkRequestId || ""
      },
      lineageIndex
    }),
    feedback: {
      latest: feedbackEntries.at(-1) || null,
      count: feedbackEntries.length,
      history: feedbackEntries.slice(-8).reverse().map(summarizeFeedbackEntry)
    },
    rework: {
      count: reworkRequests.length,
      latest: reworkRequests.at(-1) || null,
      requests: reworkRequests.slice(-8).reverse().map(summarizeReworkRequest)
    }
  };
}

function createDatabaseReviewSummary({
  totalCandidates,
  shownCandidates,
  candidates,
  feedbackRows,
  reworkRows,
  reworkStatusRows
}) {
  const mappedFeedback = feedbackRows.map(mapDatabaseFeedback);
  const mappedReworks = reworkRows.map(mapDatabaseReworkRequest);
  const statuses = reworkStatusRows.map((row) =>
    row?.schema === "gamer.ga-pet-rework-status.v1"
      ? row
      : mapDatabaseReworkStatus(row)
  );
  const completed = idsWithStatus(statuses, "completed");
  const failed = idsWithStatus(statuses, "failed");
  const started = idsWithStatus(statuses, "started");
  const terminal = new Set([...completed, ...failed]);
  const queued = mappedReworks.filter(
    (request) => !started.has(request.requestId) && !terminal.has(request.requestId)
  );
  const running = mappedReworks.filter(
    (request) => started.has(request.requestId) && !terminal.has(request.requestId)
  );

  return {
    schema: "gamer.ga-pet-review-summary.v1",
    totalCandidates,
    shownCandidates,
    feedbackCount: mappedFeedback.length,
    learningNoteCount: mappedFeedback.length,
    decisions: countBy(mappedFeedback.map((feedback) => feedback.decision).filter(Boolean)),
    topTags: countBy(mappedFeedback.flatMap((feedback) => feedback.tags)).slice(0, 8),
    rework: {
      requested: mappedReworks.length,
      queued: queued.length,
      running: running.length,
      completed: completed.size,
      failed: failed.size
    }
  };
}

function createDatabaseLineageIndex({ reworkRows, statusRows }) {
  const requestsById = new Map();
  const requestsBySource = new Map();
  const statusesByRequest = new Map();

  for (const row of reworkRows) {
    const request = mapDatabaseReworkRequest(row);
    requestsById.set(request.requestId, request);
    appendMapList(requestsBySource, request.sourceRunId, request);
  }

  for (const row of statusRows) {
    const statusEntry = mapDatabaseReworkStatus(row);
    if (!statusesByRequest.has(statusEntry.requestId)) {
      statusesByRequest.set(statusEntry.requestId, statusEntry);
    }
  }

  return {
    requestsById,
    requestsBySource,
    statusesByRequest
  };
}

function mapDatabaseFeedback(row) {
  return {
    schema: "gamer.ga-pet-human-feedback.v1",
    feedbackId: row.feedback_id,
    runId: row.run_id,
    createdAt: dateIso(row.created_at),
    reviewer: row.reviewer || "admin-ui",
    decision: row.decision || "",
    severity: row.severity || "",
    actionId: row.action_id || "",
    tags: Array.isArray(row.tags) ? row.tags : [],
    notes: row.notes || "",
    promptPatch: row.prompt_patch || "",
    reworkMode: row.rework_mode || ""
  };
}

function mapDatabaseReworkRequest(row) {
  return {
    schema: "gamer.ga-pet-rework-request.v1",
    requestId: row.request_id,
    sourceRunId: row.source_run_id,
    sourceFeedbackId: row.source_feedback_id || "",
    targetRunId: row.target_run_id || "",
    createdAt: dateIso(row.created_at),
    status: row.status || "requested",
    mode: row.mode || "rework",
    actionId: row.action_id || "",
    tags: Array.isArray(row.tags) ? row.tags : [],
    notes: row.notes || "",
    promptPatch: row.prompt_patch || ""
  };
}

function mapDatabaseReworkStatus(row) {
  return {
    schema: "gamer.ga-pet-rework-status.v1",
    requestId: row.request_id,
    sourceRunId: row.source_run_id || "",
    targetRunId: row.target_run_id || "",
    status: row.status || "",
    error: row.error || "",
    createdAt: dateIso(row.created_at)
  };
}

function summarizeFeedbackEntry(entry) {
  return {
    feedbackId: entry?.feedbackId || "",
    createdAt: entry?.createdAt || "",
    decision: entry?.decision || "",
    severity: entry?.severity || "",
    actionId: entry?.actionId || "",
    tags: Array.isArray(entry?.tags) ? entry.tags : [],
    notes: entry?.notes || "",
    promptPatch: entry?.promptPatch || ""
  };
}

function summarizeReworkRequest(entry) {
  return {
    requestId: entry?.requestId || "",
    sourceRunId: entry?.sourceRunId || "",
    sourceFeedbackId: entry?.sourceFeedbackId || "",
    createdAt: entry?.createdAt || "",
    status: entry?.status || "",
    mode: entry?.mode || "",
    actionId: entry?.actionId || "",
    tags: Array.isArray(entry?.tags) ? entry.tags : [],
    notes: entry?.notes || "",
    promptPatch: entry?.promptPatch || ""
  };
}

function createReworkLineageIndex(records = []) {
  const requestsById = new Map();
  const requestsBySource = new Map();
  const statusesByRequest = new Map();

  for (const record of records) {
    if (record?.schema === "gamer.ga-pet-rework-request.v1" && record.requestId) {
      requestsById.set(record.requestId, record);
      appendMapList(requestsBySource, record.sourceRunId || "", record);
    }

    if (record?.schema === "gamer.ga-pet-rework-status.v1" && record.requestId) {
      statusesByRequest.set(record.requestId, record);
    }
  }

  return {
    requestsById,
    requestsBySource,
    statusesByRequest
  };
}

function summarizeLineage({ runId, promptPlan, lineageIndex }) {
  const sourceRunId = promptPlan?.sourceRunId || "";
  const reworkRequestId = promptPlan?.reworkRequestId || "";
  const sourceRequest = reworkRequestId
    ? lineageIndex?.requestsById?.get(reworkRequestId) || null
    : null;
  const workerStatus = reworkRequestId
    ? summarizeReworkStatus(lineageIndex?.statusesByRequest?.get(reworkRequestId))
    : null;
  const outgoingReworks = (lineageIndex?.requestsBySource?.get(runId) || [])
    .slice(-8)
    .reverse()
    .map((request) => {
      const latestStatus = lineageIndex?.statusesByRequest?.get(request.requestId);
      return {
        ...summarizeReworkRequest(request),
        workerStatus: summarizeReworkStatus(latestStatus),
        targetRunId: latestStatus?.targetRunId || "",
        error: latestStatus?.error || ""
      };
    });

  return {
    runKind: sourceRunId ? "rework" : "random",
    sourceRunId,
    reworkRequestId,
    sourceFeedbackId: sourceRequest?.sourceFeedbackId || "",
    workerStatus,
    outgoingReworks
  };
}

function summarizeReworkStatus(entry) {
  if (!entry) return null;
  return {
    requestId: entry?.requestId || "",
    sourceRunId: entry?.sourceRunId || "",
    targetRunId: entry?.targetRunId || "",
    status: entry?.status || "",
    error: entry?.error || "",
    createdAt: entry?.createdAt || ""
  };
}

function appendMapList(map, key, value) {
  if (!key) return;
  const list = map.get(key) || [];
  list.push(value);
  map.set(key, list);
}

function idsWithStatus(records, status) {
  return new Set(
    records
      .filter((record) => record.status === status && record.requestId)
      .map((record) => record.requestId)
  );
}

function countBy(values) {
  const counts = new Map();
  for (const value of values) {
    const key = normalizeField(value, "");
    if (!key) continue;
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  return [...counts.entries()]
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
    .map(([label, count]) => ({ label, count }));
}

function gaReviewFileUrl({ runId, relativePath }) {
  return `/ga-review/files/${encodeURIComponent(runId)}?path=${encodeURIComponent(relativePath)}`;
}

function safeRunDirectory({ runRoot, runId }) {
  const root = path.resolve(runRoot || DEFAULT_RUN_ROOT);
  const safeRunId = normalizeRunId(runId);
  const runDir = path.resolve(root, safeRunId);
  assertInside(runDir, root);
  return runDir;
}

function safeRunPath({ runRoot, runId, relativePath }) {
  const runDir = safeRunDirectory({ runRoot, runId });
  const cleanRelativePath = String(relativePath ?? "").replace(/\\/gu, "/");
  if (!cleanRelativePath || path.isAbsolute(cleanRelativePath)) {
    throw new Error("Invalid asset path.");
  }
  const filePath = path.resolve(runDir, cleanRelativePath);
  assertInside(filePath, runDir);
  return filePath;
}

function assertInside(target, parent) {
  const relative = path.relative(parent, target);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    throw new Error("Path escapes GA pet run root.");
  }
}

function normalizeRunId(value) {
  const runId = String(value ?? "").trim();
  if (!/^[a-zA-Z0-9._-]+$/u.test(runId)) {
    throw new Error("Invalid run id.");
  }
  return runId;
}

function normalizeRelativePath(value) {
  const clean = String(value ?? "")
    .replace(/\\/gu, "/")
    .replace(/^\/+/u, "");
  const parts = clean.split("/").filter(Boolean);
  if (parts.some((part) => part === "..")) {
    return "";
  }
  return parts.join("/");
}

function encodeStoragePath(value) {
  return normalizeRelativePath(value)
    .split("/")
    .map(encodeURIComponent)
    .join("/");
}

function normalizeField(value, fallback) {
  const text = String(value ?? "").trim();
  return text || fallback;
}

function normalizeReworkMode(value) {
  const mode = normalizeField(value, "rework");
  if (["rework", "regenerate", "action-only", "identity-lock"].includes(mode)) {
    return mode;
  }
  return "rework";
}

function parseTags(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeTag).filter(Boolean).slice(0, 16);
  }
  return String(value ?? "")
    .split(",")
    .map(normalizeTag)
    .filter(Boolean)
    .slice(0, 16);
}

function normalizeTag(value) {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s_]+/gu, "-")
    .replace(/[^a-z0-9-]+/gu, "")
    .replace(/-+/gu, "-")
    .replace(/^-|-$/gu, "");
}

function buildLearningLesson(feedback) {
  const pieces = [];
  if (feedback.actionId) pieces.push(`action=${feedback.actionId}`);
  if (feedback.tags.length > 0) pieces.push(`tags=${feedback.tags.join(",")}`);
  const guidance = guidanceForTags(feedback.tags);
  if (guidance) pieces.push(`guidance: ${guidance}`);
  if (feedback.notes) pieces.push(feedback.notes);
  if (feedback.promptPatch) pieces.push(`prompt patch: ${feedback.promptPatch}`);
  return pieces.join(" / ");
}

function guidanceForTags(tags) {
  return (Array.isArray(tags) ? tags : [])
    .map((tag) => ISSUE_GUIDANCE[tag])
    .filter(Boolean)
    .join(" ");
}

function shortId() {
  return Math.random().toString(36).slice(2, 8);
}

function groupRowsBy(rows, key) {
  const grouped = new Map();
  for (const row of rows) {
    const value = row[key];
    if (!value) continue;
    const group = grouped.get(value) || [];
    group.push(row);
    grouped.set(value, group);
  }
  return grouped;
}

function findAssetByStorageKey(assets, storageKey) {
  if (!storageKey) return null;
  return assets.find((asset) => asset.storage_key === storageKey) || null;
}

function compareCreatedAsc(left, right) {
  return Date.parse(left.createdAt || "") - Date.parse(right.createdAt || "");
}

function dateIso(value) {
  if (!value) return "";
  if (value instanceof Date) return value.toISOString();
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : String(value);
}

function labelFromPath(value) {
  return path.basename(String(value || ""));
}

async function safeReadDirectory(directory) {
  try {
    return await readdir(directory, { withFileTypes: true });
  } catch (error) {
    if (error?.code === "ENOENT") return [];
    throw error;
  }
}

async function firstExistingRelativePath(root, candidates) {
  for (const candidate of candidates) {
    try {
      await stat(path.join(root, candidate));
      return candidate;
    } catch (error) {
      if (error?.code !== "ENOENT") throw error;
    }
  }
  return "";
}

async function readJsonIfExists(filePath) {
  try {
    return JSON.parse(await readFile(filePath, "utf8"));
  } catch (error) {
    if (error?.code === "ENOENT") return null;
    return null;
  }
}

async function readJsonLinesIfExists(filePath) {
  try {
    const text = await readFile(filePath, "utf8");
    return text
      .split(/\r?\n/u)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => JSON.parse(line));
  } catch (error) {
    if (error?.code === "ENOENT") return [];
    return [];
  }
}

function contentTypeFor(filePath) {
  const extension = path.extname(filePath).toLowerCase();
  if (extension === ".png") return "image/png";
  if (extension === ".jpg" || extension === ".jpeg") return "image/jpeg";
  if (extension === ".webp") return "image/webp";
  if (extension === ".mp4") return "video/mp4";
  if (extension === ".json" || extension === ".jsonl") return "application/json; charset=utf-8";
  if (extension === ".md") return "text/markdown; charset=utf-8";
  if (extension === ".zip") return "application/zip";
  return "application/octet-stream";
}

export function decodeBody(buffer) {
  if (!buffer || buffer.byteLength === 0) return {};
  return JSON.parse(textDecoder.decode(buffer));
}
