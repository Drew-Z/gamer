import { appendFile, mkdir, readFile, readdir, stat, writeFile } from "node:fs/promises";
import path from "node:path";

const DEFAULT_RUN_ROOT = path.resolve(
  "services",
  "pet-generator",
  "data",
  "ga-random-pets"
);

const textDecoder = new TextDecoder();

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
    candidates
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
    feedback: {
      latest: latestFeedback,
      count: feedbackEntries.length
    },
    rework: {
      count: reworkRequests.length,
      latest: reworkRequests.at(-1) || null
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
    return value.map((item) => normalizeField(item, "")).filter(Boolean).slice(0, 16);
  }
  return String(value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 16);
}

function buildLearningLesson(feedback) {
  const pieces = [];
  if (feedback.actionId) pieces.push(`action=${feedback.actionId}`);
  if (feedback.tags.length > 0) pieces.push(`tags=${feedback.tags.join(",")}`);
  if (feedback.notes) pieces.push(feedback.notes);
  if (feedback.promptPatch) pieces.push(`prompt patch: ${feedback.promptPatch}`);
  return pieces.join(" / ");
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
