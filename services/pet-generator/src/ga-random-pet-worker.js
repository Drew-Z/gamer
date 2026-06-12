import crypto from "node:crypto";
import {
  appendFile,
  mkdir,
  readFile,
  readdir,
  writeFile
} from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
const DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-image";
const DEFAULT_VIDEO_MODEL = "veo-3.1-generate-preview";
const API_REVISION = "2026-05-20";
const DEFAULT_RUN_ROOT = path.resolve(
  "services",
  "pet-generator",
  "data",
  "ga-random-pets"
);

const SPECIES = [
  "cloud-tailed fox",
  "lantern mouse",
  "dewdrop dragon",
  "pocket comet cat",
  "mint shell turtle",
  "stardust rabbit",
  "copper moth sprite",
  "snowglass ferret"
];

const ELEMENTS = [
  "electric teal",
  "sunlit orange",
  "moon blue",
  "mint wind",
  "warm ember",
  "crystal water",
  "violet nebula",
  "soft brass"
];

const TEMPERAMENTS = [
  "curious and loyal",
  "sleepy but brave",
  "playful and gentle",
  "alert and helpful",
  "shy then affectionate",
  "mischievous but harmless",
  "calm and wise",
  "energetic and social"
];

const MOTION_IDEAS = [
  "idle bob with tiny ear twitches",
  "tail swish with a soft sparkle trail",
  "two-step hop and settle",
  "happy click reaction with a small bounce",
  "review waiting pose with focused eyes",
  "sniffing curiosity loop",
  "reward nibble pose",
  "signature wave"
];

const PALETTES = [
  "bright teal, cream, and citrus accents",
  "sky blue, pearl white, and gold accents",
  "coral, mint, and warm gray",
  "lavender, moon white, and cyan",
  "honey yellow, aqua, and soft brown",
  "rose pink, cloud white, and tiny green highlights"
];

export function createWorkerConfig(env = process.env) {
  return {
    apiKey: env.GEMINI_API_KEY || env.GOOGLE_API_KEY || "",
    apiBaseUrl: env.GA_PET_API_BASE_URL || DEFAULT_API_BASE_URL,
    imageModel: env.GA_PET_IMAGE_MODEL || DEFAULT_IMAGE_MODEL,
    imageSize: env.GA_PET_IMAGE_SIZE || "1K",
    imageAspectRatio: env.GA_PET_IMAGE_ASPECT_RATIO || "1:1",
    videoModel: env.GA_PET_VIDEO_MODEL || DEFAULT_VIDEO_MODEL,
    enableVideo: parseBoolean(env.GA_PET_ENABLE_VIDEO, false),
    videoDurationSeconds: parsePositiveInteger(env.GA_PET_VIDEO_DURATION_SECONDS, 5),
    videoPollSeconds: parsePositiveInteger(env.GA_PET_VIDEO_POLL_SECONDS, 12),
    videoMaxPolls: parsePositiveInteger(env.GA_PET_VIDEO_MAX_POLLS, 30),
    runRoot: path.resolve(env.GA_PET_RUN_ROOT || DEFAULT_RUN_ROOT),
    batchSize: parsePositiveInteger(env.GA_PET_BATCH_SIZE, 1),
    loop: parseBoolean(env.GA_PET_LOOP, false),
    intervalSeconds: parsePositiveInteger(env.GA_PET_INTERVAL_SECONDS, 90),
    maxRuns: parseNonNegativeInteger(env.GA_PET_MAX_RUNS, 1),
    requestTimeoutMs: parsePositiveInteger(env.GA_PET_REQUEST_TIMEOUT_MS, 180000),
    ownerUserId: env.GA_PET_OWNER_USER_ID || "user-demo-001"
  };
}

export async function runGaRandomPetWorker(config = createWorkerConfig()) {
  if (!config.apiKey.trim()) {
    throw new Error("GEMINI_API_KEY or GOOGLE_API_KEY is required.");
  }

  await mkdir(config.runRoot, { recursive: true });
  const experienceLogPath = path.join(config.runRoot, "ga-experience.jsonl");
  const startedAt = new Date().toISOString();
  await appendExperience(experienceLogPath, {
    type: "worker-started",
    startedAt,
    imageModel: config.imageModel,
    videoModel: config.enableVideo ? config.videoModel : "",
    enableVideo: config.enableVideo,
    loop: config.loop,
    batchSize: config.batchSize,
    maxRuns: config.maxRuns
  });

  let completedRuns = 0;
  let batchIndex = 0;

  while (shouldContinue(config, completedRuns)) {
    batchIndex += 1;
    for (let itemIndex = 0; itemIndex < config.batchSize; itemIndex += 1) {
      if (!shouldContinue(config, completedRuns)) break;
      completedRuns += 1;
      const plan = buildRandomPetPromptPlan({
        runOrdinal: completedRuns,
        seed: crypto.randomBytes(8).toString("hex")
      });
      const runId = createRunId(plan, completedRuns);
      const runDir = path.join(config.runRoot, runId);

      try {
        const result = await generateCandidateRun({
          config,
          plan,
          runId,
          runDir
        });
        await appendExperience(experienceLogPath, {
          type: "candidate-ready",
          runId,
          batchIndex,
          completedRuns,
          createdAt: new Date().toISOString(),
          promptSummary: plan.summary,
          imageModel: config.imageModel,
          videoModel: result.video ? config.videoModel : "",
          packageZip: result.packageZip,
          status: "waiting-human-review"
        });
        console.log(
          `[ga-worker] candidate-ready ${runId} status=waiting-human-review zip=${result.packageZip}`
        );
      } catch (error) {
        const safeError = safeErrorMessage(error);
        await mkdir(runDir, { recursive: true });
        await writeJsonFile(path.join(runDir, "failure.json"), {
          runId,
          failedAt: new Date().toISOString(),
          status: "failed",
          error: safeError,
          promptPlan: plan
        });
        await appendExperience(experienceLogPath, {
          type: "candidate-failed",
          runId,
          batchIndex,
          completedRuns,
          failedAt: new Date().toISOString(),
          promptSummary: plan.summary,
          error: safeError
        });
        console.error(`[ga-worker] candidate-failed ${runId} error=${safeError}`);
      }
    }

    if (config.loop && shouldContinue(config, completedRuns)) {
      await sleep(config.intervalSeconds * 1000);
    } else {
      break;
    }
  }

  await appendExperience(experienceLogPath, {
    type: "worker-stopped",
    stoppedAt: new Date().toISOString(),
    completedRuns
  });
}

export function buildRandomPetPromptPlan({ runOrdinal, seed }) {
  const pick = (items, offset) => items[indexFromSeed(seed, offset, items.length)];
  const species = pick(SPECIES, 0);
  const element = pick(ELEMENTS, 1);
  const temperament = pick(TEMPERAMENTS, 2);
  const motionIdea = pick(MOTION_IDEAS, 3);
  const palette = pick(PALETTES, 4);
  const name = titleCase(`${element.split(" ")[0]} ${species}`);
  const summary = `${name}: ${temperament}, ${motionIdea}`;

  const identityPrompt = [
    "Create an original fantasy desktop pet character.",
    `Creature: ${species}.`,
    `Element and palette: ${element}; ${palette}.`,
    `Temperament: ${temperament}.`,
    `Animation direction: ${motionIdea}.`,
    "Full body, centered, transparent or clean solid light background, no cropping.",
    "Cute game companion, readable at 128px, strong silhouette, separated ears, paws, tail, and effects.",
    "No text, no watermark, no UI, no existing IP, no photorealistic human features.",
    "Design it as a source identity image for later sprite-sheet animation."
  ].join(" ");

  const videoPrompt = [
    "Create a short motion reference for the same original fantasy desktop pet.",
    `The pet is a ${species} with ${element} styling and ${palette}.`,
    `Show ${motionIdea}.`,
    "Keep the body centered with no camera zoom, no scene cuts, no text, no watermark.",
    "The result is only a motion-quality reference for later PNG sprite-sheet production."
  ].join(" ");

  return {
    schema: "gamer.ga-random-pet-prompt-plan.v1",
    runOrdinal,
    seed,
    name,
    summary,
    species,
    element,
    temperament,
    motionIdea,
    palette,
    imagePrompt: identityPrompt,
    videoPrompt,
    reviewChecklist: [
      "Original creature, not recognizable as existing IP",
      "Full body readable at Android small-avatar scale",
      "No text or watermark",
      "Body has separable parts for idle, reward, review, and click motion",
      "Effects support transparent PNG cleanup",
      "Video reference does not rely on camera movement"
    ]
  };
}

async function generateCandidateRun({ config, plan, runId, runDir }) {
  const candidatePath = "artifacts/candidates/base-identity.png";
  const promptPlanPath = "source/generation/prompt-plan.json";
  const reviewCardPath = "review-card.md";
  const manifestPath = "package-manifest.json";
  const apiTracePath = "source/generation/api-trace.json";

  await mkdir(path.join(runDir, "artifacts", "candidates"), { recursive: true });
  await mkdir(path.join(runDir, "artifacts", "video"), { recursive: true });
  await mkdir(path.join(runDir, "source", "generation"), { recursive: true });

  await writeJsonFile(path.join(runDir, promptPlanPath), plan);

  const imageResult = await generateImage({
    config,
    prompt: plan.imagePrompt
  });
  await writeFile(path.join(runDir, candidatePath), imageResult.bytes);

  let videoResult = null;
  if (config.enableVideo) {
    videoResult = await generateVideoReference({
      config,
      prompt: plan.videoPrompt,
      runDir
    });
  }

  const files = [
    {
      kind: "candidate",
      path: candidatePath,
      role: "base-identity"
    },
    {
      kind: "generation-plan",
      path: promptPlanPath
    }
  ];
  if (videoResult?.filePath) {
    files.push({
      kind: "video-reference",
      path: videoResult.filePath
    });
  }
  if (videoResult?.operationPath) {
    files.push({
      kind: "video-operation",
      path: videoResult.operationPath
    });
  }

  const packageManifest = {
    schema: "fantasy-pet.package-manifest.v1",
    runId,
    appJobId: runId,
    acceptedBy: "pending-human-review",
    sourceTaskId: `${runId}:ga-random-pet-worker`,
    sourceDownloadId: "ga-base-identity",
    generatedBy: {
      provider: "google-genai",
      imageModel: config.imageModel,
      videoModel: config.enableVideo ? config.videoModel : "",
      createdAt: new Date().toISOString()
    },
    files
  };
  await writeJsonFile(path.join(runDir, manifestPath), packageManifest);

  await writeJsonFile(path.join(runDir, apiTracePath), {
    image: {
      model: config.imageModel,
      mimeType: imageResult.mimeType,
      responseShape: imageResult.responseShape
    },
    video: videoResult
      ? {
          model: config.videoModel,
          operationDone: videoResult.done,
          filePath: videoResult.filePath,
          operationPath: videoResult.operationPath
        }
      : null
  });

  await writeFile(
    path.join(runDir, reviewCardPath),
    buildReviewCard({ runId, plan, imageResult, videoResult }),
    "utf8"
  );

  const packageZip = `${runId}-candidate.zip`;
  await createStoredZipFromDirectory(runDir, path.join(runDir, packageZip));

  return {
    packageZip: path.join(runDir, packageZip),
    video: videoResult
  };
}

async function generateImage({ config, prompt }) {
  const responseJson = await postJson({
    apiKey: config.apiKey,
    url: `${config.apiBaseUrl}/interactions`,
    timeoutMs: config.requestTimeoutMs,
    body: {
      model: config.imageModel,
      input: [
        {
          type: "text",
          text: prompt
        }
      ],
      response_format: {
        type: "image",
        aspect_ratio: config.imageAspectRatio,
        image_size: config.imageSize
      }
    },
    headers: {
      "Api-Revision": API_REVISION
    }
  });

  const image = extractGeneratedImage(responseJson);
  if (!image) {
    throw new Error(
      `image_response_missing_base64 shape=${summarizeResponseShape(responseJson)}`
    );
  }

  return {
    bytes: Buffer.from(stripDataUrl(image.base64), "base64"),
    mimeType: image.mimeType || "image/png",
    responseShape: summarizeResponseShape(responseJson)
  };
}

async function generateVideoReference({ config, prompt, runDir }) {
  const operation = await postJson({
    apiKey: config.apiKey,
    url: `${config.apiBaseUrl}/models/${encodeURIComponent(config.videoModel)}:predictLongRunning`,
    timeoutMs: config.requestTimeoutMs,
    body: {
      instances: [
        {
          prompt
        }
      ],
      parameters: {
        durationSeconds: config.videoDurationSeconds,
        sampleCount: 1
      }
    }
  });

  if (!operation?.name) {
    throw new Error(`video_operation_missing_name shape=${summarizeResponseShape(operation)}`);
  }

  let latestOperation = operation;
  for (let pollIndex = 0; pollIndex < config.videoMaxPolls; pollIndex += 1) {
    if (latestOperation.done) break;
    await sleep(config.videoPollSeconds * 1000);
    latestOperation = await getJson({
      apiKey: config.apiKey,
      url: resolveApiUrl(config.apiBaseUrl, latestOperation.name),
      timeoutMs: config.requestTimeoutMs
    });
  }

  const operationPath = "artifacts/video/operation.json";
  await writeJsonFile(path.join(runDir, operationPath), redactOperationForLog(latestOperation));

  const video = extractGeneratedVideo(latestOperation);
  if (!video) {
    return {
      done: Boolean(latestOperation.done),
      filePath: "",
      operationPath
    };
  }

  const filePath = "artifacts/video/motion-reference.mp4";
  if (video.base64) {
    await writeFile(path.join(runDir, filePath), Buffer.from(stripDataUrl(video.base64), "base64"));
  } else if (video.uri) {
    const videoBytes = await downloadBinary({
      apiKey: config.apiKey,
      url: resolveApiUrl(config.apiBaseUrl, video.uri),
      timeoutMs: config.requestTimeoutMs
    });
    await writeFile(path.join(runDir, filePath), videoBytes);
  }

  return {
    done: Boolean(latestOperation.done),
    filePath,
    operationPath
  };
}

async function postJson({ apiKey, url, body, timeoutMs, headers = {} }) {
  return requestJson({
    apiKey,
    url,
    timeoutMs,
    init: {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...headers
      },
      body: JSON.stringify(body)
    }
  });
}

async function getJson({ apiKey, url, timeoutMs }) {
  return requestJson({
    apiKey,
    url,
    timeoutMs,
    init: {
      method: "GET"
    }
  });
}

async function requestJson({ apiKey, url, init, timeoutMs }) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      ...init,
      headers: {
        ...(init.headers || {}),
        "x-goog-api-key": apiKey
      },
      signal: controller.signal
    });
    const text = await response.text();
    const json = text.trim() ? JSON.parse(text) : {};
    if (!response.ok) {
      throw new Error(`ga_api_${response.status}:${safeApiError(json)}`);
    }
    return json;
  } finally {
    clearTimeout(timeout);
  }
}

async function downloadBinary({ apiKey, url, timeoutMs }) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      headers: {
        "x-goog-api-key": apiKey
      },
      signal: controller.signal
    });
    if (!response.ok) {
      throw new Error(`ga_video_download_${response.status}`);
    }
    return Buffer.from(await response.arrayBuffer());
  } finally {
    clearTimeout(timeout);
  }
}

function extractGeneratedImage(responseJson) {
  const candidates = [
    responseJson?.output_image,
    responseJson?.outputImage,
    responseJson?.generatedImages?.[0]?.image,
    responseJson?.predictions?.[0],
    responseJson?.data?.[0]
  ];

  for (const candidate of candidates) {
    const image = imageFromCandidate(candidate);
    if (image) return image;
  }

  const timeline = responseJson?.timeline || responseJson?.steps || [];
  for (const step of timeline) {
    const content = step?.content || step?.outputs || step?.output || [];
    const items = Array.isArray(content) ? content : [content];
    for (const item of items) {
      const image = imageFromCandidate(item);
      if (image) return image;
    }
  }

  const outputs = responseJson?.outputs || responseJson?.content || [];
  for (const item of Array.isArray(outputs) ? outputs : [outputs]) {
    const image = imageFromCandidate(item);
    if (image) return image;
  }

  return null;
}

function imageFromCandidate(candidate) {
  if (!candidate || typeof candidate !== "object") return null;
  const base64 =
    candidate.data ||
    candidate.imageBytes ||
    candidate.image_bytes ||
    candidate.bytesBase64Encoded ||
    candidate.b64_json ||
    candidate.b64Json ||
    candidate.inlineData?.data ||
    candidate.inline_data?.data;
  if (typeof base64 !== "string" || base64.trim() === "") return null;
  return {
    base64,
    mimeType:
      candidate.mime_type ||
      candidate.mimeType ||
      candidate.inlineData?.mimeType ||
      candidate.inline_data?.mime_type ||
      "image/png"
  };
}

function extractGeneratedVideo(operation) {
  const videos = [
    operation?.response?.generatedVideos?.[0]?.video,
    operation?.response?.generated_videos?.[0]?.video,
    operation?.response?.videos?.[0],
    operation?.response?.predictions?.[0]?.video
  ];
  for (const video of videos) {
    if (!video || typeof video !== "object") continue;
    const base64 =
      video.bytesBase64Encoded ||
      video.bytes_base64_encoded ||
      video.data ||
      video.inlineData?.data ||
      video.inline_data?.data;
    const uri = video.uri || video.gcsUri || video.gcs_uri || video.url;
    if (base64 || uri) {
      return { base64, uri };
    }
  }
  return null;
}

function stripDataUrl(value) {
  const text = String(value ?? "");
  const commaIndex = text.indexOf(",");
  if (text.startsWith("data:") && commaIndex >= 0) {
    return text.slice(commaIndex + 1);
  }
  return text;
}

function resolveApiUrl(baseUrl, maybeUrl) {
  const text = String(maybeUrl ?? "").trim();
  if (/^https?:\/\//iu.test(text)) return text;
  const base = String(baseUrl ?? "").replace(/\/+$/u, "");
  const pathPart = text.replace(/^\/+/u, "");
  return `${base}/${pathPart}`;
}

function buildReviewCard({ runId, plan, imageResult, videoResult }) {
  return [
    `# GA random pet candidate ${runId}`,
    "",
    `Status: waiting-human-review`,
    `Name: ${plan.name}`,
    `Summary: ${plan.summary}`,
    `Image mime: ${imageResult.mimeType}`,
    `Video reference: ${videoResult?.filePath || "not generated"}`,
    "",
    "## Image Prompt",
    "",
    plan.imagePrompt,
    "",
    "## Video Prompt",
    "",
    plan.videoPrompt,
    "",
    "## Review Checklist",
    "",
    ...plan.reviewChecklist.map((item) => `- ${item}`),
    "",
    "## Next",
    "",
    "- Review the base identity image at Android small-avatar size.",
    "- If accepted, generate or repair action sheets before community import.",
    "- Do not mark this package as human-reviewed until a person accepts it."
  ].join("\n");
}

async function createStoredZipFromDirectory(sourceDir, destinationPath) {
  const filePaths = await listFiles(sourceDir);
  const entries = [];
  for (const absolutePath of filePaths) {
    if (path.resolve(absolutePath) === path.resolve(destinationPath)) continue;
    const relativePath = path.relative(sourceDir, absolutePath).replaceAll("\\", "/");
    const data = await readFile(absolutePath);
    entries.push({ relativePath, data });
  }
  const zipBuffer = createStoredZip(entries);
  await writeFile(destinationPath, zipBuffer);
}

async function listFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await listFiles(absolutePath));
    } else if (entry.isFile()) {
      files.push(absolutePath);
    }
  }
  return files;
}

function createStoredZip(entries) {
  const localParts = [];
  const centralParts = [];
  let offset = 0;

  for (const entry of entries) {
    const name = Buffer.from(entry.relativePath, "utf8");
    const data = entry.data;
    const crc = crc32(data);
    const localHeader = Buffer.alloc(30);
    localHeader.writeUInt32LE(0x04034b50, 0);
    localHeader.writeUInt16LE(20, 4);
    localHeader.writeUInt16LE(0x0800, 6);
    localHeader.writeUInt16LE(0, 8);
    localHeader.writeUInt16LE(0, 10);
    localHeader.writeUInt16LE(0, 12);
    localHeader.writeUInt32LE(crc, 14);
    localHeader.writeUInt32LE(data.length, 18);
    localHeader.writeUInt32LE(data.length, 22);
    localHeader.writeUInt16LE(name.length, 26);
    localHeader.writeUInt16LE(0, 28);
    localParts.push(localHeader, name, data);

    const centralHeader = Buffer.alloc(46);
    centralHeader.writeUInt32LE(0x02014b50, 0);
    centralHeader.writeUInt16LE(20, 4);
    centralHeader.writeUInt16LE(20, 6);
    centralHeader.writeUInt16LE(0x0800, 8);
    centralHeader.writeUInt16LE(0, 10);
    centralHeader.writeUInt16LE(0, 12);
    centralHeader.writeUInt16LE(0, 14);
    centralHeader.writeUInt32LE(crc, 16);
    centralHeader.writeUInt32LE(data.length, 20);
    centralHeader.writeUInt32LE(data.length, 24);
    centralHeader.writeUInt16LE(name.length, 28);
    centralHeader.writeUInt16LE(0, 30);
    centralHeader.writeUInt16LE(0, 32);
    centralHeader.writeUInt16LE(0, 34);
    centralHeader.writeUInt16LE(0, 36);
    centralHeader.writeUInt32LE(0, 38);
    centralHeader.writeUInt32LE(offset, 42);
    centralParts.push(centralHeader, name);

    offset += localHeader.length + name.length + data.length;
  }

  const centralDirectory = Buffer.concat(centralParts);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(entries.length, 8);
  end.writeUInt16LE(entries.length, 10);
  end.writeUInt32LE(centralDirectory.length, 12);
  end.writeUInt32LE(offset, 16);
  end.writeUInt16LE(0, 20);

  return Buffer.concat([...localParts, centralDirectory, end]);
}

const CRC_TABLE = new Uint32Array(256).map((_, index) => {
  let crc = index;
  for (let bit = 0; bit < 8; bit += 1) {
    crc = crc & 1 ? 0xedb88320 ^ (crc >>> 1) : crc >>> 1;
  }
  return crc >>> 0;
});

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc = CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function shouldContinue(config, completedRuns) {
  return config.maxRuns === 0 || completedRuns < config.maxRuns;
}

function parseBoolean(value, fallback) {
  if (value === undefined) return fallback;
  return ["1", "true", "yes", "on"].includes(String(value).trim().toLowerCase());
}

function parsePositiveInteger(value, fallback) {
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseNonNegativeInteger(value, fallback) {
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}

function indexFromSeed(seed, offset, length) {
  const hash = crypto
    .createHash("sha256")
    .update(`${seed}:${offset}`)
    .digest();
  return hash.readUInt32BE(0) % length;
}

function createRunId(plan, ordinal) {
  const slug = plan.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/gu, "-")
    .replace(/^-|-$/gu, "")
    .slice(0, 48);
  const stamp = new Date().toISOString().replace(/[-:]/gu, "").replace(/\..+$/u, "Z");
  return `ga-${stamp}-${String(ordinal).padStart(3, "0")}-${slug}`;
}

function titleCase(value) {
  return value
    .split(/\s+/u)
    .filter(Boolean)
    .map((part) => `${part.slice(0, 1).toUpperCase()}${part.slice(1)}`)
    .join(" ");
}

async function writeJsonFile(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

async function appendExperience(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await appendFile(filePath, `${JSON.stringify(value)}\n`, "utf8");
}

function safeApiError(json) {
  const message = json?.error?.message || json?.error || "request_failed";
  return String(message).replace(/AIza[0-9A-Za-z_-]+/gu, "[REDACTED_API_KEY]");
}

function safeErrorMessage(error) {
  const message = error instanceof Error ? error.message : String(error);
  return message.replace(/AIza[0-9A-Za-z_-]+/gu, "[REDACTED_API_KEY]");
}

function summarizeResponseShape(value) {
  if (!value || typeof value !== "object") return typeof value;
  const keys = Object.keys(value).slice(0, 12);
  return keys.join(",");
}

function redactOperationForLog(operation) {
  if (!operation || typeof operation !== "object") return operation;
  return JSON.parse(
    JSON.stringify(operation).replace(/AIza[0-9A-Za-z_-]+/gu, "[REDACTED_API_KEY]")
  );
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const isDirectRun =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  runGaRandomPetWorker().catch((error) => {
    console.error(`[ga-worker] fatal ${safeErrorMessage(error)}`);
    process.exitCode = 1;
  });
}
