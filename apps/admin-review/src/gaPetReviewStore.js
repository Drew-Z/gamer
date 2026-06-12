import { appendFile, mkdir, readFile, readdir, stat, writeFile } from "node:fs/promises";
import path from "node:path";

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

  return {
    runRoot,
    listCandidates: (input) => listCandidates({ runRoot, ...input }),
    readAsset: (input) => readAsset({ runRoot, ...input }),
    writeFeedback: (input) => writeFeedback({ runRoot, ...input })
  };
}

async function listCandidates({ runRoot, limit = 40 } = {}) {
  const root = path.resolve(runRoot || DEFAULT_RUN_ROOT);
  const entries = await safeReadDirectory(root);
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
    candidates.push(await createCandidateSummary({ runRoot: root, ...item }));
  }

  return {
    schema: "gamer.ga-pet-review-list.v1",
    runRoot: root,
    count: candidates.length,
    summary: await createReviewSummary({
      runRoot: root,
      totalCandidates: directories.length,
      shownCandidates: candidates.length,
      candidates
    }),
    candidates
  };
}

async function createReviewSummary({
  runRoot,
  totalCandidates,
  shownCandidates,
  candidates
}) {
  const learningNotes = await readJsonLinesIfExists(path.join(runRoot, "ga-learning-notes.jsonl"));
  const reworkRecords = await readJsonLinesIfExists(path.join(runRoot, "ga-rework-queue.jsonl"));
  const reworkRequests = reworkRecords.filter(
    (record) => record?.schema === "gamer.ga-pet-rework-request.v1"
  );
  const reworkStatuses = reworkRecords.filter(
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
  updatedAt
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

async function writeFeedback({ runRoot, runId, body }) {
  const runDir = safeRunDirectory({ runRoot, runId });
  const runInfo = await stat(runDir);
  if (!runInfo.isDirectory()) {
    throw new Error("GA pet candidate is not a directory.");
  }

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

  const feedbackLine = `${JSON.stringify(feedback)}\n`;
  await appendFile(path.join(runDir, "human-feedback.jsonl"), feedbackLine, "utf8");
  await writeFile(
    path.join(runDir, "human-feedback-latest.json"),
    `${JSON.stringify(feedback, null, 2)}\n`,
    "utf8"
  );

  const learningNote = {
    schema: "gamer.ga-pet-learning-note.v1",
    noteId: `lesson-${feedbackId}`,
    sourceRunId: runId,
    sourceFeedbackId: feedbackId,
    createdAt: now,
    decision: feedback.decision,
    severity: feedback.severity,
    actionId: feedback.actionId,
    tags: feedback.tags,
    guidance: guidanceForTags(feedback.tags),
    lesson: buildLearningLesson(feedback)
  };
  await appendFile(
    path.join(path.resolve(runRoot), "ga-learning-notes.jsonl"),
    `${JSON.stringify(learningNote)}\n`,
    "utf8"
  );

  let reworkRequest = null;
  if (["rework", "regenerate"].includes(feedback.decision) || feedback.reworkMode) {
    reworkRequest = {
      schema: "gamer.ga-pet-rework-request.v1",
      requestId: `rework-${feedbackId}`,
      sourceRunId: runId,
      sourceFeedbackId: feedbackId,
      createdAt: now,
      status: "requested",
      mode: feedback.reworkMode || feedback.decision,
      actionId: feedback.actionId,
      tags: feedback.tags,
      notes: feedback.notes,
      promptPatch: feedback.promptPatch
    };
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
    createdAt: entry?.createdAt || "",
    status: entry?.status || "",
    mode: entry?.mode || "",
    actionId: entry?.actionId || "",
    tags: Array.isArray(entry?.tags) ? entry.tags : [],
    notes: entry?.notes || "",
    promptPatch: entry?.promptPatch || ""
  };
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

function normalizeField(value, fallback) {
  const text = String(value ?? "").trim();
  return text || fallback;
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
