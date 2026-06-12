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
import { loadEnvFiles } from "../../community-api/src/env-file.js";
import { createGaPetDatabaseStore } from "./ga-pet-database.js";
import {
  createSupabasePetSyncConfig,
  isSupabasePetSyncReady,
  syncGaPetCandidateToSupabase
} from "./supabase-pet-sync.js";

const DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
const DEFAULT_L0VEYOU_API_BASE_URL = "https://l0veyou.com";
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

const CORE_DESKTOP_ACTIONS = [
  {
    id: "idle",
    fileName: "idle.png",
    displayName: "Idle",
    category: "core-desktop",
    trigger: "app open and desktop standby",
    frameCount: 16,
    loop: true,
    prompt:
      "gentle breathing idle loop, tiny ear or tail motion, calm eyes, no locomotion"
  },
  {
    id: "tap-reaction",
    fileName: "tap_reaction.png",
    displayName: "Tap reaction",
    category: "core-desktop",
    trigger: "user taps or clicks the pet",
    frameCount: 14,
    loop: false,
    prompt:
      "happy click reaction, small bounce, bright expression, quick settle back to idle"
  },
  {
    id: "drag-hold",
    fileName: "drag_hold.png",
    displayName: "Drag hold",
    category: "core-desktop",
    trigger: "user drags the pet around the desktop",
    frameCount: 12,
    loop: true,
    prompt:
      "being gently dragged, body tucked upward, surprised but comfortable expression, dangling paws or tail"
  },
  {
    id: "drag-release",
    fileName: "drag_release.png",
    displayName: "Drag release",
    category: "core-desktop",
    trigger: "user releases the dragged pet",
    frameCount: 14,
    loop: false,
    prompt:
      "soft landing after drag, squash and stretch, regains balance, returns to idle stance"
  },
  {
    id: "feed",
    fileName: "feed.png",
    displayName: "Feed",
    category: "core-desktop",
    trigger: "user feeds or rewards the pet",
    frameCount: 18,
    loop: false,
    prompt:
      "accepts a tiny snack, cheerful nibble, satisfied sparkle, no human hand visible"
  },
  {
    id: "sleep",
    fileName: "sleep.png",
    displayName: "Sleep",
    category: "core-desktop",
    trigger: "pet rests while idle for a long time",
    frameCount: 18,
    loop: true,
    prompt:
      "sleeping loop, curled or settled posture, slow breathing, tiny dream effect"
  },
  {
    id: "wake",
    fileName: "wake.png",
    displayName: "Wake",
    category: "core-desktop",
    trigger: "pet wakes up from sleep",
    frameCount: 14,
    loop: false,
    prompt:
      "wakes from sleep, stretches, blinks, returns to alert desktop companion posture"
  },
  {
    id: "roam",
    fileName: "roam.png",
    displayName: "Roam",
    category: "core-desktop",
    trigger: "pet moves across or around the desktop",
    frameCount: 16,
    loop: true,
    prompt:
      "small in-place roam or walk cycle, readable foot rhythm, stable body size"
  },
  {
    id: "waiting-review",
    fileName: "waiting_review.png",
    displayName: "Waiting review",
    category: "core-desktop",
    trigger: "pet waits during generation, review, or loading",
    frameCount: 16,
    loop: true,
    prompt:
      "patient waiting pose, focused eyes, tiny ambient motion, does not imply automatic approval"
  },
  {
    id: "attention",
    fileName: "attention.png",
    displayName: "Attention",
    category: "core-desktop",
    trigger: "pet notifies the user",
    frameCount: 14,
    loop: false,
    prompt:
      "friendly notification reaction, perk up, small glow or chime effect, not alarming"
  },
  {
    id: "failed",
    fileName: "failed.png",
    displayName: "Failed",
    category: "core-desktop",
    trigger: "generation or action fails",
    frameCount: 14,
    loop: false,
    prompt:
      "gentle failure reaction, droops briefly, recovers with hopeful expression"
  }
];

const SPECIES_ACTIONS = {
  fox: [
    habitAction("scent-sniff", "scent_sniff.png", "Scent sniff", "follows an invisible scent trail, nose twitch, tail balances the body"),
    habitAction("tail-fan", "tail_fan.png", "Tail fan", "expressive tail fan flourish, soft curl and settle"),
    habitAction("clever-peek", "clever_peek.png", "Clever peek", "leans forward with clever curiosity, ears perk, quick retreat")
  ],
  mouse: [
    habitAction("whisker-sniff", "whisker_sniff.png", "Whisker sniff", "whiskers twitch, tiny cautious sniff, quick paw adjustment"),
    habitAction("crumb-dash", "crumb_dash.png", "Crumb dash", "short playful dash toward a tiny crumb, stops cleanly in place"),
    habitAction("cheek-spark", "cheek_spark.png", "Cheek spark", "cheeks glow with a cute charge, tiny paws brace, no harsh lightning")
  ],
  dragon: [
    habitAction("wing-flutter", "wing_flutter.png", "Wing flutter", "tiny wing flutter while staying centered, tail counterbalance"),
    habitAction("breath-spark", "breath_spark.png", "Breath spark", "small harmless magical breath puff, returns to neutral"),
    habitAction("treasure-guard", "treasure_guard.png", "Treasure guard", "protects a tiny shiny pebble, proud but cute stance")
  ],
  cat: [
    habitAction("paw-bat", "paw_bat.png", "Paw bat", "bats at a tiny floating mote, quick paw motion, playful focus"),
    habitAction("pounce", "pounce.png", "Pounce", "small playful pounce in place, crouch then spring then settle"),
    habitAction("loaf-curl", "loaf_curl.png", "Loaf curl", "curls into a compact loaf-like rest and peeks up")
  ],
  turtle: [
    habitAction("shell-tuck", "shell_tuck.png", "Shell tuck", "brief shell tuck and peek, gentle and readable"),
    habitAction("bubble-peek", "bubble_peek.png", "Bubble peek", "peeks through soft bubbles, slow charming motion"),
    habitAction("slow-spin", "slow_spin.png", "Slow spin", "tiny slow turn in place, shell remains readable")
  ],
  rabbit: [
    habitAction("ear-radar", "ear_radar.png", "Ear radar", "ears scan like radar, alert but cute expression"),
    habitAction("binky-hop", "binky_hop.png", "Binky hop", "joyful small hop twist, lands softly"),
    habitAction("nose-wiggle", "nose_wiggle.png", "Nose wiggle", "nose wiggle and tiny paw lift, compact idle-like loop")
  ],
  moth: [
    habitAction("wing-shimmer", "wing_shimmer.png", "Wing shimmer", "gentle wing shimmer with tiny dust sparkle, body centered"),
    habitAction("lantern-orbit", "lantern_orbit.png", "Lantern orbit", "orbits a tiny harmless light mote, soft wing beats"),
    habitAction("dust-twirl", "dust_twirl.png", "Dust twirl", "brief powdery sparkle twirl, no dense particles")
  ],
  ferret: [
    habitAction("slinky-scamper", "slinky_scamper.png", "Slinky scamper", "long playful scamper in place, flexible body arc"),
    habitAction("peekaboo", "peekaboo.png", "Peekaboo", "peeks out, ducks, and pops back up cheerfully"),
    habitAction("tail-loop", "tail_loop.png", "Tail loop", "tail makes a playful loop, body remains stable")
  ]
};

const ELEMENT_ACTIONS = {
  electric: [
    habitAction("static-charge", "static_charge.png", "Static charge", "soft static charge gathers then pops into tiny harmless sparks")
  ],
  sunlit: [
    habitAction("sun-glow", "sun_glow.png", "Sun glow", "warm sun glow pulse, cheerful stretch, no harsh flare")
  ],
  moon: [
    habitAction("moon-ring", "moon_ring.png", "Moon ring", "small moon ring orbits once, dreamy expression, stable body")
  ],
  mint: [
    habitAction("wind-swirl", "wind_swirl.png", "Wind swirl", "soft mint wind swirl lifts ears or fur gently")
  ],
  warm: [
    habitAction("ember-puff", "ember_puff.png", "Ember puff", "tiny warm ember puff, cozy expression, no fire danger")
  ],
  crystal: [
    habitAction("water-prism", "water_prism.png", "Water prism", "small water-prism shimmer, droplets stay close to body")
  ],
  violet: [
    habitAction("nebula-blink", "nebula_blink.png", "Nebula blink", "tiny nebula blink effect, star specks fade quickly")
  ],
  soft: [
    habitAction("brass-chime", "brass_chime.png", "Brass chime", "small chime-like shimmer, polite notification flourish")
  ]
};

export function createWorkerConfig(env = process.env) {
  const qualityPreset = parseQualityPreset(env.GA_PET_QUALITY_PRESET, "high");
  const qualityDefaults = qualityDefaultsFor(qualityPreset);
  const apiProvider = parseApiProvider(env.GA_PET_API_PROVIDER, env.GA_PET_API_BASE_URL);
  return {
    apiProvider,
    apiKey: env.GA_PET_API_KEY || env.GEMINI_API_KEY || env.GOOGLE_API_KEY || "",
    apiBaseUrl:
      env.GA_PET_API_BASE_URL ||
      (apiProvider === "l0veyou" ? DEFAULT_L0VEYOU_API_BASE_URL : DEFAULT_API_BASE_URL),
    packageMode: parsePackageMode(env.GA_PET_PACKAGE_MODE, "full"),
    qualityPreset,
    backgroundMode: parseBackgroundMode(env.GA_PET_BACKGROUND_MODE, "transparent"),
    outputMimeType: parseOptionalString(env.GA_PET_OUTPUT_MIME_TYPE, "image/png"),
    imageDelivery: parseOptionalString(env.GA_PET_IMAGE_DELIVERY, ""),
    imageModel: env.GA_PET_IMAGE_MODEL || DEFAULT_IMAGE_MODEL,
    imageSize: parseImageSize(env.GA_PET_IMAGE_SIZE, qualityDefaults.identityImageSize),
    imageAspectRatio: env.GA_PET_IMAGE_ASPECT_RATIO || "1:1",
    spriteSheetImageSize: parseImageSize(
      env.GA_PET_SPRITESHEET_IMAGE_SIZE,
      qualityDefaults.spriteSheetImageSize
    ),
    spriteSheetAspectRatio: env.GA_PET_SPRITESHEET_ASPECT_RATIO || "16:9",
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
    actionIntervalSeconds: parseNonNegativeInteger(env.GA_PET_ACTION_INTERVAL_SECONDS, 0),
    customActionCount: parseNonNegativeInteger(env.GA_PET_CUSTOM_ACTION_COUNT, 3),
    requireAllActions: parseBoolean(env.GA_PET_REQUIRE_ALL_ACTIONS, false),
    learningNoteLimit: parseNonNegativeInteger(env.GA_PET_LEARNING_NOTE_LIMIT, 12),
    reworkQueue: parseBoolean(env.GA_PET_REWORK_QUEUE, true),
    reworkStartedTimeoutMinutes: parseNonNegativeInteger(
      env.GA_PET_REWORK_STARTED_TIMEOUT_MINUTES,
      180
    ),
    supabaseSync: createSupabasePetSyncConfig(env),
    databaseStore: createGaPetDatabaseStore(env),
    configCheck:
      parseBoolean(env.GA_PET_CONFIG_CHECK, false) ||
      process.argv.includes("--config-check"),
    requestTimeoutMs: parsePositiveInteger(env.GA_PET_REQUEST_TIMEOUT_MS, 180000),
    ownerUserId: env.GA_PET_OWNER_USER_ID || "user-demo-001"
  };
}

export async function runGaRandomPetWorker(config = createWorkerConfig()) {
  if (config.configCheck) {
    printConfigCheck(config);
    return;
  }

  if (!config.apiKey.trim()) {
    throw new Error("GA_PET_API_KEY, GEMINI_API_KEY, or GOOGLE_API_KEY is required.");
  }

  await mkdir(config.runRoot, { recursive: true });
  const experienceLogPath = path.join(config.runRoot, "ga-experience.jsonl");
  const startedAt = new Date().toISOString();
  await appendExperience(experienceLogPath, {
    type: "worker-started",
    startedAt,
    packageMode: config.packageMode,
    apiProvider: config.apiProvider,
    qualityPreset: config.qualityPreset,
    backgroundMode: config.backgroundMode,
    outputMimeType: config.outputMimeType,
    imageDelivery: config.imageDelivery,
    imageModel: config.imageModel,
    imageSize: config.imageSize,
    spriteSheetImageSize: config.spriteSheetImageSize,
    spriteSheetAspectRatio: config.spriteSheetAspectRatio,
    videoModel: config.enableVideo ? config.videoModel : "",
    enableVideo: config.enableVideo,
    loop: config.loop,
    batchSize: config.batchSize,
    maxRuns: config.maxRuns,
    learningNoteLimit: config.learningNoteLimit,
    reworkQueue: config.reworkQueue,
    reworkStartedTimeoutMinutes: config.reworkStartedTimeoutMinutes,
    supabaseSyncEnabled: isSupabasePetSyncReady(config.supabaseSync),
    databaseReworkQueueEnabled: Boolean(config.databaseStore)
  });

  let completedRuns = 0;
  let batchIndex = 0;

  while (shouldContinue(config, completedRuns)) {
    batchIndex += 1;
    for (let itemIndex = 0; itemIndex < config.batchSize; itemIndex += 1) {
      if (!shouldContinue(config, completedRuns)) break;
      completedRuns += 1;
      const seed = crypto.randomBytes(8).toString("hex");
      const learningContext = await loadLearningContext({
        runRoot: config.runRoot,
        limit: config.learningNoteLimit
      });
      const reworkRequest = config.reworkQueue
        ? await findNextReworkRequest(config, {
            startedTimeoutMinutes: config.reworkStartedTimeoutMinutes
          })
        : null;
      const plan = reworkRequest
        ? await buildReworkPetPromptPlan({
            config,
            runOrdinal: completedRuns,
            seed,
            request: reworkRequest,
            learningInstruction: learningContext.instruction
          })
        : buildRandomPetPromptPlan({
            runOrdinal: completedRuns,
            seed,
            packageMode: config.packageMode,
            customActionCount: config.customActionCount,
            backgroundMode: config.backgroundMode,
            learningInstruction: learningContext.instruction
          });
      const runId = createRunId(plan, completedRuns);
      const runDir = path.join(config.runRoot, runId);

      try {
        if (reworkRequest) {
          await appendReworkStatus(config, {
            requestId: reworkRequest.requestId,
            sourceRunId: reworkRequest.sourceRunId,
            targetRunId: runId,
            status: "started"
          });
        }
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
          packageMode: config.packageMode,
          qualityPreset: config.qualityPreset,
          backgroundMode: config.backgroundMode,
          runKind: reworkRequest ? "rework" : "random",
          reworkRequestId: reworkRequest?.requestId || "",
          learningNoteCount: learningContext.count,
          imageModel: config.imageModel,
          imageSize: config.imageSize,
          spriteSheetImageSize: config.spriteSheetImageSize,
          videoModel: result.video ? config.videoModel : "",
          packageZip: result.packageZip,
          supabaseSync: result.supabaseSync,
          generatedActionCount: result.generatedActionCount,
          expectedActionCount: result.expectedActionCount,
          status: result.status
        });
        if (reworkRequest) {
          await appendReworkStatus(config, {
            requestId: reworkRequest.requestId,
            sourceRunId: reworkRequest.sourceRunId,
            targetRunId: runId,
            status: "completed"
          });
        }
        console.log(
          `[ga-worker] candidate-ready ${runId} status=${result.status} actions=${result.generatedActionCount}/${result.expectedActionCount} zip=${result.packageZip}`
        );
      } catch (error) {
        const safeError = safeErrorMessage(error);
        if (reworkRequest) {
          await appendReworkStatus(config, {
            requestId: reworkRequest.requestId,
            sourceRunId: reworkRequest.sourceRunId,
            targetRunId: runId,
            status: "failed",
            error: safeError
          });
        }
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

export function buildRandomPetPromptPlan({
  runOrdinal,
  seed,
  packageMode = "full",
  customActionCount = 3,
  backgroundMode = "transparent",
  learningInstruction = ""
}) {
  const pick = (items, offset) => items[indexFromSeed(seed, offset, items.length)];
  const species = pick(SPECIES, 0);
  const element = pick(ELEMENTS, 1);
  const temperament = pick(TEMPERAMENTS, 2);
  const motionIdea = pick(MOTION_IDEAS, 3);
  const palette = pick(PALETTES, 4);
  const name = titleCase(`${element.split(" ")[0]} ${species}`);
  const summary = `${name}: ${temperament}, ${motionIdea}`;
  const backgroundInstruction = backgroundInstructionFor(backgroundMode);

  const identityPrompt = [
    "Create an original fantasy desktop pet character.",
    `Creature: ${species}.`,
    `Element and palette: ${element}; ${palette}.`,
    `Temperament: ${temperament}.`,
    `Animation direction: ${motionIdea}.`,
    `Full body, centered, no cropping. ${backgroundInstruction}`,
    learningInstruction ? `Human review lessons to apply: ${learningInstruction}` : "",
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

  const actions =
    packageMode === "full"
      ? buildAdaptiveActionPlan({ species, element, temperament, motionIdea, seed, customActionCount })
      : [];

  return {
    schema: "gamer.ga-random-pet-prompt-plan.v1",
    packageMode,
    backgroundMode,
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
    backgroundInstruction,
    learningInstruction,
    actions,
    reviewChecklist: [
      "Original creature, not recognizable as existing IP",
      "Full body readable at Android small-avatar scale",
      "No text or watermark",
      "Body has separable parts for desktop interaction motion",
      "Core desktop actions include idle, tap, drag, feed, sleep, attention, waiting, and failure states",
      "Species and element actions match the animal habits instead of copying a fixed template",
      "Effects support transparent PNG cleanup",
      "Video reference does not rely on camera movement"
    ]
  };
}

function buildAdaptiveActionPlan({
  species,
  element,
  temperament,
  motionIdea,
  seed,
  customActionCount
}) {
  const speciesKey = detectSpeciesKey(species);
  const elementKey = detectElementKey(element);
  const speciesPool = SPECIES_ACTIONS[speciesKey] || [];
  const elementPool = ELEMENT_ACTIONS[elementKey] || [];
  const selectedSpeciesActions = pickManyFromSeed(
    speciesPool,
    seed,
    10,
    Math.min(customActionCount, speciesPool.length)
  );
  const selectedElementActions = pickManyFromSeed(
    elementPool,
    seed,
    20,
    elementPool.length > 0 ? 1 : 0
  );
  const signatureAction = habitAction(
    "signature",
    "signature.png",
    "Signature",
    `${motionIdea}, customized for a ${temperament} ${species} with ${element} effects`
  );

  return dedupeActions([
    ...CORE_DESKTOP_ACTIONS,
    ...selectedSpeciesActions,
    ...selectedElementActions,
    signatureAction
  ]);
}

function habitAction(id, fileName, displayName, prompt) {
  return {
    id,
    fileName,
    displayName,
    category: "adaptive-habit",
    trigger: "pet-specific habit or signature animation",
    frameCount: 16,
    loop: false,
    prompt
  };
}

function detectSpeciesKey(species) {
  const text = String(species).toLowerCase();
  for (const key of Object.keys(SPECIES_ACTIONS)) {
    if (text.includes(key)) return key;
  }
  return "fox";
}

function detectElementKey(element) {
  const text = String(element).toLowerCase();
  for (const key of Object.keys(ELEMENT_ACTIONS)) {
    if (text.includes(key)) return key;
  }
  return "soft";
}

function pickManyFromSeed(items, seed, offset, count) {
  const remaining = [...items];
  const selected = [];
  while (selected.length < count && remaining.length > 0) {
    const index = indexFromSeed(seed, offset + selected.length, remaining.length);
    selected.push(remaining.splice(index, 1)[0]);
  }
  return selected;
}

function dedupeActions(actions) {
  const seen = new Set();
  const result = [];
  for (const action of actions) {
    if (seen.has(action.id)) continue;
    seen.add(action.id);
    result.push(action);
  }
  return result;
}

async function buildReworkPetPromptPlan({
  config,
  runOrdinal,
  seed,
  request,
  learningInstruction
}) {
  const originalPlan = await readPromptPlanForRework(config, request);
  const species = originalPlan?.species || "fantasy desktop pet";
  const element = originalPlan?.element || "bright original";
  const temperament = originalPlan?.temperament || "friendly";
  const motionIdea = originalPlan?.motionIdea || "polished desktop-pet motion";
  const palette = originalPlan?.palette || "bright readable companion colors";
  const name = `${originalPlan?.name || titleCase(species)} Rework`;
  const summary = `Rework ${request.sourceRunId}: ${request.notes || request.promptPatch || motionIdea}`;
  const backgroundMode = config.backgroundMode || originalPlan?.backgroundMode || "transparent";
  const backgroundInstruction = backgroundInstructionFor(backgroundMode);
  const feedbackInstruction = buildReworkInstruction(request);
  const combinedLearningInstruction = [learningInstruction, feedbackInstruction]
    .filter(Boolean)
    .join(" ");
  const actions = Array.isArray(originalPlan?.actions) && originalPlan.actions.length > 0
    ? originalPlan.actions
    : buildAdaptiveActionPlan({
        species,
        element,
        temperament,
        motionIdea,
        seed,
        customActionCount: config.customActionCount
      });

  const imagePrompt = [
    "Create a revised original fantasy desktop pet character from a prior generated candidate.",
    `Source run: ${request.sourceRunId}.`,
    `Creature: ${species}.`,
    `Element and palette: ${element}; ${palette}.`,
    `Temperament: ${temperament}.`,
    `Human feedback to fix: ${feedbackInstruction}`,
    `Full body, centered, no cropping. ${backgroundInstruction}`,
    combinedLearningInstruction
      ? `Also apply accumulated review lessons: ${combinedLearningInstruction}`
      : "",
    "Preserve the strongest parts of the original concept while correcting the reviewed problems.",
    "No text, no watermark, no UI, no existing IP, no photorealistic human features.",
    "Design it as a source identity image for later sprite-sheet animation."
  ].join(" ");

  const videoPrompt = [
    "Create a short motion reference for the revised original fantasy desktop pet.",
    `Pet: ${name}, based on ${species} with ${element} styling.`,
    `Human feedback to fix: ${feedbackInstruction}`,
    "Keep the body centered with no camera zoom, no scene cuts, no text, no watermark."
  ].join(" ");

  return {
    schema: "gamer.ga-random-pet-rework-prompt-plan.v1",
    packageMode: config.packageMode,
    backgroundMode,
    runOrdinal,
    seed,
    name,
    summary,
    sourceRunId: request.sourceRunId,
    reworkRequestId: request.requestId,
    species,
    element,
    temperament,
    motionIdea,
    palette,
    imagePrompt,
    videoPrompt,
    backgroundInstruction,
    learningInstruction: combinedLearningInstruction,
    actions,
    reviewChecklist: [
      "Rework directly addresses the human feedback",
      "Original creature, not recognizable as existing IP",
      "Full body readable at Android small-avatar scale",
      "No text or watermark",
      "Core and adaptive action sheets preserve identity",
      "Effects support transparent PNG cleanup"
    ]
  };
}

function buildReworkInstruction(request) {
  const pieces = [];
  if (request.actionId) pieces.push(`focus action ${request.actionId}`);
  if (Array.isArray(request.tags) && request.tags.length > 0) {
    pieces.push(`problem tags: ${request.tags.join(", ")}`);
    pieces.push(`tag guidance: ${guidanceForTags(request.tags)}`);
  }
  if (request.notes) pieces.push(request.notes);
  if (request.promptPatch) pieces.push(`specific prompt patch: ${request.promptPatch}`);
  if (request.mode) pieces.push(`requested mode: ${request.mode}`);
  return pieces.join("; ") || "improve identity consistency, transparency, and motion readability";
}

function guidanceForTags(tags) {
  return (Array.isArray(tags) ? tags : [])
    .map((tag) => ISSUE_GUIDANCE[normalizeIssueTag(tag)])
    .filter(Boolean)
    .join(" ");
}

function normalizeIssueTag(value) {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s_]+/gu, "-")
    .replace(/[^a-z0-9-]+/gu, "")
    .replace(/-+/gu, "-")
    .replace(/^-|-$/gu, "");
}

function backgroundInstructionFor(backgroundMode) {
  if (backgroundMode === "transparent") {
    return [
      "Use true transparent alpha background.",
      "Output PNG with no matte, no checkerboard, no paper texture, no cast-shadow rectangle, and no background scene."
    ].join(" ");
  }
  if (backgroundMode === "chroma") {
    return [
      "Use a single flat bright green chroma-key background only.",
      "Keep the pet edges clean and separated for later background removal."
    ].join(" ");
  }
  if (backgroundMode === "light") {
    return "Use a clean solid light background only, with no scene details or text.";
  }
  return "Use the cleanest available cutout-friendly background for later transparent desktop-pet packaging.";
}

async function generateCandidateRun({ config, plan, runId, runDir }) {
  const candidatePath = "artifacts/candidates/base-identity.png";
  const baseAssetPath = "assets/base_identity.png";
  const previewPath = "previews/preview.png";
  const promptPlanPath = "source/generation/prompt-plan.json";
  const reviewCardPath = "review-card.md";
  const manifestPath = "package-manifest.json";
  const apiTracePath = "source/generation/api-trace.json";
  const motionMapPath = "meta/motion_map.json";
  const runtimePath = "meta/runtime.json";
  const licensePath = "license.json";
  const scoreReportPath = "score-report.json";
  const ownershipClaimPath = "ownership-claim.json";
  const gamerManifestPath = "manifest.json";

  await mkdir(path.join(runDir, "artifacts", "candidates"), { recursive: true });
  await mkdir(path.join(runDir, "artifacts", "video"), { recursive: true });
  await mkdir(path.join(runDir, "assets"), { recursive: true });
  await mkdir(path.join(runDir, "meta"), { recursive: true });
  await mkdir(path.join(runDir, "motion", "sheets"), { recursive: true });
  await mkdir(path.join(runDir, "previews"), { recursive: true });
  await mkdir(path.join(runDir, "exports"), { recursive: true });
  await mkdir(path.join(runDir, "source", "generation"), { recursive: true });
  await mkdir(path.join(runDir, "source", "generation", "actions"), { recursive: true });

  await writeJsonFile(path.join(runDir, promptPlanPath), plan);

  const imageResult = await generateImage({
    config,
    prompt: plan.imagePrompt
  });
  await writeFile(path.join(runDir, candidatePath), imageResult.bytes);
  await writeFile(path.join(runDir, baseAssetPath), imageResult.bytes);
  await writeFile(path.join(runDir, previewPath), imageResult.bytes);

  const motionResults =
    plan.packageMode === "full"
      ? await generateMotionSheets({
          config,
          plan,
          runDir,
          identityImage: imageResult
        })
      : [];
  const generatedActionCount = motionResults.filter((result) => result.status === "generated")
    .length;
  const expectedActionCount = plan.actions.length;

  if (
    config.requireAllActions &&
    expectedActionCount > 0 &&
    generatedActionCount !== expectedActionCount
  ) {
    throw new Error(
      `required_full_actions_missing generated=${generatedActionCount} expected=${expectedActionCount}`
    );
  }

  let videoResult = null;
  if (config.enableVideo) {
    videoResult = await generateVideoReference({
      config,
      prompt: plan.videoPrompt,
      runDir
    });
  }

  let motionMap = {};
  if (plan.packageMode === "full") {
    motionMap = buildMotionMap({ motionResults });
    await writeJsonFile(
      path.join(runDir, motionMapPath),
      motionMap
    );
    await writeJsonFile(
      path.join(runDir, runtimePath),
      buildRuntimeMetadata({ motionResults })
    );
    await writeJsonFile(
      path.join(runDir, licensePath),
      buildLicenseMetadata({ runId, plan })
    );
    await writeJsonFile(
      path.join(runDir, scoreReportPath),
      buildScoreReport({ runId, plan, generatedActionCount, expectedActionCount })
    );
    await writeJsonFile(
      path.join(runDir, ownershipClaimPath),
      buildOwnershipClaim({ runId, config })
    );
  }

  const status = resourceStatusFor({
    packageMode: plan.packageMode,
    generatedActionCount,
    expectedActionCount
  });
  const packageZip = `${runId}-${plan.packageMode === "full" ? "full-resource" : "identity"}-candidate.zip`;
  const exportArtifactPath = plan.packageMode === "full" ? `exports/${packageZip}` : packageZip;

  if (plan.packageMode === "full") {
    await writeJsonFile(
      path.join(runDir, gamerManifestPath),
      buildGamerPetManifest({
        runId,
        config,
        plan,
        baseAssetPath,
        previewPath,
        exportArtifactPath,
        motionResults,
        licensePath,
        scoreReportPath
      })
    );
  }

  const files = [
    {
      kind: "candidate",
      path: candidatePath,
      role: "base-identity"
    },
    {
      kind: "base-image",
      path: baseAssetPath
    },
    {
      kind: "preview-image",
      path: previewPath
    },
    {
      kind: "generation-plan",
      path: promptPlanPath
    }
  ];
  for (const result of motionResults) {
    if (result.status === "generated") {
      files.push({
        kind: "motion-sheet",
        path: result.path,
        action: result.action.id,
        status: result.status
      });
    } else if (result.failurePath) {
      files.push({
        kind: "motion-sheet-failure",
        path: result.failurePath,
        action: result.action.id,
        status: result.status
      });
    }
  }
  if (plan.packageMode === "full") {
    files.push(
      {
        kind: "motion-map",
        path: motionMapPath
      },
      {
        kind: "runtime",
        path: runtimePath
      },
      {
        kind: "license",
        path: licensePath
      },
      {
        kind: "score-report",
        path: scoreReportPath
      },
      {
        kind: "ownership-claim",
        path: ownershipClaimPath
      },
      {
        kind: "gamer-pet-manifest",
        path: gamerManifestPath
      }
    );
  }
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
    acceptedBy: "ga-auto-generated",
    qualityGate: "auto-generated-unverified",
    resourceStatus: status,
    sourceTaskId: `${runId}:ga-random-pet-worker`,
    sourceDownloadId: plan.packageMode === "full" ? "ga-full-resource-package" : "ga-base-identity",
    generatedBy: {
      provider: config.apiProvider,
      imageModel: config.imageModel,
      videoModel: config.enableVideo ? config.videoModel : "",
      qualityPreset: config.qualityPreset,
      backgroundMode: config.backgroundMode,
      outputMimeType: config.outputMimeType,
      createdAt: new Date().toISOString()
    },
    files
  };
  await writeJsonFile(path.join(runDir, manifestPath), packageManifest);

  await writeJsonFile(path.join(runDir, apiTracePath), {
    provider: config.apiProvider,
    apiBaseUrl: config.apiBaseUrl,
    image: {
      model: config.imageModel,
      imageSize: config.imageSize,
      aspectRatio: config.imageAspectRatio,
      mimeType: imageResult.mimeType,
      responseShape: imageResult.responseShape
    },
    quality: {
      preset: config.qualityPreset,
      spriteSheetImageSize: config.spriteSheetImageSize,
      spriteSheetAspectRatio: config.spriteSheetAspectRatio,
      backgroundMode: config.backgroundMode,
      outputMimeType: config.outputMimeType,
      imageDelivery: config.imageDelivery
    },
    motionSheets: motionResults.map((result) => ({
      actionId: result.action.id,
      path: result.path,
      status: result.status,
      mimeType: result.mimeType,
      responseShape: result.responseShape,
      error: result.error || ""
    })),
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
    buildReviewCard({
      runId,
      plan,
      imageResult,
      videoResult,
      motionResults,
      status,
      generatedActionCount,
      expectedActionCount
    }),
    "utf8"
  );

  const packageZipPath = path.join(runDir, exportArtifactPath);
  await createStoredZipFromDirectory(runDir, packageZipPath);

  const supabaseSync = await syncCandidateIfConfigured({
    config,
    runId,
    runDir,
    plan,
    packageManifest,
    motionMap,
    previewPath,
    packagePath: exportArtifactPath,
    videoPath: videoResult?.filePath || ""
  });

  return {
    packageZip: packageZipPath,
    video: videoResult,
    supabaseSync,
    status,
    generatedActionCount,
    expectedActionCount
  };
}

async function syncCandidateIfConfigured(input) {
  if (!isSupabasePetSyncReady(input.config.supabaseSync)) {
    return {
      enabled: false,
      uploadedAssets: 0
    };
  }

  try {
    return await syncGaPetCandidateToSupabase({
      config: input.config.supabaseSync,
      ownerUserId: input.config.ownerUserId,
      runId: input.runId,
      runDir: input.runDir,
      plan: input.plan,
      packageManifest: input.packageManifest,
      motionMap: input.motionMap,
      previewPath: input.previewPath,
      packagePath: input.packagePath,
      videoPath: input.videoPath
    });
  } catch (error) {
    const safeError = safeErrorMessage(error);
    await writeJsonFile(path.join(input.runDir, "meta", "supabase-sync-error.json"), {
      schema: "gamer.ga-pet-supabase-sync-error.v1",
      runId: input.runId,
      failedAt: new Date().toISOString(),
      error: safeError
    });
    console.error(`[ga-worker] supabase-sync-failed ${input.runId} error=${safeError}`);
    return {
      enabled: true,
      uploadedAssets: 0,
      error: safeError
    };
  }
}

async function generateMotionSheets({ config, plan, runDir, identityImage }) {
  const results = [];
  for (const action of plan.actions) {
    const sheetPath = `motion/sheets/${action.fileName}`;
    const actionPrompt = buildMotionSheetPrompt({ plan, action });
    const actionPromptPath = `source/generation/actions/${action.id}.json`;

    await writeJsonFile(path.join(runDir, actionPromptPath), {
      schema: "gamer.ga-random-pet-action-prompt.v1",
      action,
      prompt: actionPrompt,
      identityReference: "assets/base_identity.png"
    });

    try {
      const sheetResult = await generateImage({
        config,
        prompt: actionPrompt,
        inputImage: identityImage,
        aspectRatio: config.spriteSheetAspectRatio,
        imageSize: config.spriteSheetImageSize
      });
      await writeFile(path.join(runDir, sheetPath), sheetResult.bytes);
      results.push({
        action,
        path: sheetPath,
        promptPath: actionPromptPath,
        status: "generated",
        mimeType: sheetResult.mimeType,
        responseShape: sheetResult.responseShape
      });
    } catch (error) {
      const safeError = safeErrorMessage(error);
      const failurePath = `source/generation/actions/${action.id}.failure.json`;
      await writeJsonFile(path.join(runDir, failurePath), {
        schema: "gamer.ga-random-pet-action-failure.v1",
        action,
        failedAt: new Date().toISOString(),
        error: safeError
      });
      results.push({
        action,
        path: sheetPath,
        promptPath: actionPromptPath,
        status: "failed",
        mimeType: "",
        responseShape: "",
        failurePath,
        error: safeError
      });
    }

    if (config.actionIntervalSeconds > 0) {
      await sleep(config.actionIntervalSeconds * 1000);
    }
  }
  return results;
}

function buildMotionSheetPrompt({ plan, action }) {
  const loopText = action.loop ? "This action should loop cleanly." : "This action should play once and settle cleanly.";
  return [
    "Use the attached identity image as the strict character reference.",
    "Generate one PNG horizontal spritesheet for an original desktop pet action.",
    `Pet: ${plan.name}, a ${plan.species} with ${plan.element} styling, ${plan.palette}.`,
    `Personality: ${plan.temperament}.`,
    `Action id: ${action.id}.`,
    `Desktop trigger: ${action.trigger}.`,
    `Motion: ${action.prompt}.`,
    plan.learningInstruction
      ? `Human review lessons to apply before drawing this sheet: ${plan.learningInstruction}`
      : "",
    `Create exactly ${action.frameCount} animation frames in one single horizontal row.`,
    "Every frame must have the same canvas size and the same body scale.",
    "Keep the ground anchor and body center stable unless the action clearly jumps or lands.",
    plan.backgroundInstruction,
    "No grid lines, no labels, no numbers, no UI, no watermark, no text.",
    "Avoid camera zoom, scene cuts, duplicate static frames, cropped body parts, and identity drift.",
    loopText,
    "Output only the spritesheet image."
  ].join(" ");
}

async function generateImage({
  config,
  prompt,
  inputImage = null,
  aspectRatio = config.imageAspectRatio,
  imageSize = config.imageSize
}) {
  if (config.apiProvider === "l0veyou") {
    return generateL0veYouImage({
      config,
      prompt,
      inputImage,
      aspectRatio,
      imageSize
    });
  }

  const input = [
    {
      type: "text",
      text: prompt
    }
  ];
  if (inputImage?.bytes) {
    input.push({
      type: "image",
      data: inputImage.bytes.toString("base64"),
      mime_type: inputImage.mimeType || "image/png"
    });
  }
  const responseFormat = {
    type: "image",
    aspect_ratio: aspectRatio,
    image_size: imageSize
  };
  if (config.outputMimeType) {
    responseFormat.mime_type = config.outputMimeType;
  }
  if (config.imageDelivery) {
    responseFormat.delivery = config.imageDelivery;
  }

  const responseJson = await postJson({
    apiProvider: config.apiProvider,
    apiKey: config.apiKey,
    url: `${config.apiBaseUrl}/interactions`,
    timeoutMs: config.requestTimeoutMs,
    body: {
      model: config.imageModel,
      input,
      response_format: responseFormat
    },
    headers: {
      "Api-Revision": API_REVISION
    }
  });

  const image = extractGeneratedImage(responseJson);
  if (!image) {
    throw new Error(
      `image_response_missing_data shape=${summarizeResponseShape(responseJson)}`
    );
  }
  const bytes = image.base64
    ? Buffer.from(stripDataUrl(image.base64), "base64")
    : await downloadBinary({
        apiProvider: config.apiProvider,
        apiKey: config.apiKey,
        url: resolveApiUrl(config.apiBaseUrl, image.uri),
        timeoutMs: config.requestTimeoutMs
      });

  return {
    bytes,
    mimeType: image.mimeType || "image/png",
    responseShape: summarizeResponseShape(responseJson)
  };
}

async function generateL0veYouImage({
  config,
  prompt,
  inputImage = null,
  aspectRatio = config.imageAspectRatio,
  imageSize = config.imageSize
}) {
  const model = String(config.imageModel || "nano-banana-2").trim();
  const hasInputImage = Boolean(inputImage?.bytes);
  const url = `${config.apiBaseUrl.replace(/\/+$/u, "")}/api/generate/${encodeURIComponent(model)}${hasInputImage ? "/image" : ""}`;
  const body = buildL0veYouImageBody({
    config,
    prompt,
    inputImage,
    aspectRatio,
    imageSize,
    model
  });

  const responseJson = await postJson({
    apiProvider: config.apiProvider,
    apiKey: config.apiKey,
    url,
    timeoutMs: config.requestTimeoutMs,
    body
  });

  const image = extractGeneratedImage(responseJson);
  if (!image) {
    throw new Error(
      `image_response_missing_data shape=${summarizeResponseShape(responseJson)}`
    );
  }
  const bytes = image.base64
    ? Buffer.from(stripDataUrl(image.base64), "base64")
    : await downloadBinary({
        apiProvider: config.apiProvider,
        apiKey: config.apiKey,
        url: resolveApiUrl(config.apiBaseUrl, image.uri),
        timeoutMs: config.requestTimeoutMs
      });

  return {
    bytes,
    mimeType: image.mimeType || "image/png",
    responseShape: summarizeResponseShape(responseJson)
  };
}

function buildL0veYouImageBody({
  config,
  prompt,
  inputImage = null,
  aspectRatio,
  imageSize,
  model
}) {
  const baseBody = {
    client_task_id: createClientTaskId("gamer-image"),
    prompt
  };
  if (inputImage?.bytes) {
    baseBody.image_data = toDataUrl(
      inputImage.bytes,
      inputImage.mimeType || "image/png"
    );
  }

  if (String(model).toLowerCase().includes("gpt-image")) {
    return {
      ...baseBody,
      n: 1,
      size: l0veYouSquareSize(imageSize),
      output_format: mimeTypeToImageFormat(config.outputMimeType)
    };
  }

  return {
    ...baseBody,
    resolution: l0veYouResolution(imageSize),
    aspect_ratio: aspectRatio
  };
}

function buildMotionMap({ motionResults }) {
  const actions = {};
  for (const result of motionResults) {
    actions[result.action.id] = {
      sheet: result.status === "generated" ? result.path : "",
      failure: result.failurePath || "",
      frames: result.action.frameCount,
      loop: result.action.loop,
      category: result.action.category,
      trigger: result.action.trigger,
      status: result.status
    };
  }
  return {
    schema: "fantasy-pet.motion-map.v1",
    format: "hd-independent-horizontal-action-sheets",
    generatedBy: "ga-random-pet-worker",
    actions,
    note:
      "Core desktop-pet interactions are fixed; species and element actions are selected from the generated pet's habits."
  };
}

function buildRuntimeMetadata({ motionResults }) {
  const motions = {};
  for (const result of motionResults) {
    motions[result.action.id] = {
      baselineY: 0.88,
      shadowScale: 1,
      blendMs: result.action.loop ? 180 : 120,
      fx: fxForAction(result.action),
      qa: {
        status: result.status === "generated" ? "ga-generated-needs-runtime-qa" : "generation-failed",
        sourceCandidate: result.status === "generated" ? result.path : "",
        failurePath: result.failurePath || "",
        note:
          "Generated automatically from GA during the quota window. Runtime QA and visual acceptance are still separate gates."
      }
    };
  }
  return {
    schema: "fantasy-pet.runtime.v1",
    targetFps: 60,
    packageProfile: "ga-full-desktop-pet-v1",
    previewOnly: true,
    motions
  };
}

function buildLicenseMetadata({ runId, plan }) {
  return {
    schema: "gamer.pet-license.v1",
    runId,
    claim: "original-ai-generated-candidate",
    provider: "google-genai",
    reviewStatus: "unreviewed",
    usage:
      "Prototype candidate for internal desktop-pet generation review and later packaging.",
    restrictions: [
      "Do not publish as human-reviewed without a separate review record.",
      "Do not use if it resembles existing IP, contains text, or contains watermark artifacts."
    ],
    promptSummary: plan.summary
  };
}

function buildScoreReport({ runId, plan, generatedActionCount, expectedActionCount }) {
  const completeness =
    expectedActionCount === 0 ? 0 : Math.round((generatedActionCount / expectedActionCount) * 20);
  const complete = generatedActionCount === expectedActionCount && expectedActionCount > 0;
  return {
    schema: "gamer.pet-score-report.v1",
    petId: runId,
    totalScore: complete ? 70 : Math.max(25, completeness + 20),
    breakdown: {
      packageCompleteness: completeness,
      visualQuality: complete ? 12 : 6,
      actionCoverage: completeness,
      identityConsistency: 8,
      previewEvidence: 6,
      licenseReadiness: 4
    },
    rewardRecommendation: {
      grant: false,
      amount: 0,
      reason:
        "GA auto-generated resource candidate. Reward and community approval require a later review gate."
    },
    risks: [
      "Automated generation may still contain identity drift between action sheets.",
      "Spritesheets may need transparency cleanup, frame extraction repair, or runtime anchor QA.",
      `Prompt summary: ${plan.summary}`
    ]
  };
}

function buildOwnershipClaim({ runId, config }) {
  return {
    schema: "gamer.ownership-claim.v1",
    claimId: `claim-${runId}`,
    userId: config.ownerUserId,
    petId: runId,
    claimType: "original-created",
    attestation:
      "Auto-generated candidate controlled by the project owner; not yet reviewed for publication.",
    sourceReferences: ["source/generation/prompt-plan.json"],
    submittedAt: new Date().toISOString(),
    reviewStatus: "pending"
  };
}

function buildGamerPetManifest({
  runId,
  config,
  plan,
  baseAssetPath,
  previewPath,
  exportArtifactPath,
  motionResults,
  licensePath,
  scoreReportPath
}) {
  return {
    schema: "gamer.pet-package.v1",
    petId: runId,
    displayName: plan.name,
    ownerUserId: config.ownerUserId,
    source: {
      kind: "fantasy-pet-rule",
      runId,
      statePath: "source/generation/prompt-plan.json"
    },
    assets: {
      baseImage: baseAssetPath,
      previewImage: previewPath,
      exportArtifact: exportArtifactPath,
      motionSheets: motionResults
        .filter((result) => result.status === "generated")
        .map((result) => result.path)
    },
    license: licensePath,
    scoreReport: scoreReportPath
  };
}

function resourceStatusFor({ packageMode, generatedActionCount, expectedActionCount }) {
  if (packageMode !== "full") return "identity-candidate-ready";
  if (expectedActionCount > 0 && generatedActionCount === expectedActionCount) {
    return "full-resource-candidate-ready";
  }
  return "partial-resource-candidate";
}

function printConfigCheck(config) {
  const samplePlan = buildRandomPetPromptPlan({
    runOrdinal: 1,
    seed: "config-check",
    packageMode: config.packageMode,
    customActionCount: config.customActionCount,
    backgroundMode: config.backgroundMode
  });
  const expectedImageCalls = 1 + samplePlan.actions.length;
  const expectedVideoCalls = config.enableVideo ? 1 : 0;
  const safeConfig = {
    schema: "gamer.ga-random-pet-worker-config-check.v1",
    ok: true,
    apiKeyPresent: Boolean(config.apiKey.trim()),
    apiProvider: config.apiProvider,
    apiBaseUrl: config.apiBaseUrl,
    packageMode: config.packageMode,
    qualityPreset: config.qualityPreset,
    imageModel: config.imageModel,
    imageSize: config.imageSize,
    imageAspectRatio: config.imageAspectRatio,
    spriteSheetImageSize: config.spriteSheetImageSize,
    spriteSheetAspectRatio: config.spriteSheetAspectRatio,
    backgroundMode: config.backgroundMode,
    outputMimeType: config.outputMimeType || "(omitted)",
    imageDelivery: config.imageDelivery || "(omitted)",
    enableVideo: config.enableVideo,
    videoModel: config.enableVideo ? config.videoModel : "",
    runRoot: config.runRoot,
    loop: config.loop,
    maxRuns: config.maxRuns,
    batchSize: config.batchSize,
    intervalSeconds: config.intervalSeconds,
    actionIntervalSeconds: config.actionIntervalSeconds,
    customActionCount: config.customActionCount,
    requireAllActions: config.requireAllActions,
    learningNoteLimit: config.learningNoteLimit,
    reworkQueue: config.reworkQueue,
    reworkStartedTimeoutMinutes: config.reworkStartedTimeoutMinutes,
    supabaseSync: {
      enabled: Boolean(config.supabaseSync.enabled),
      ready: isSupabasePetSyncReady(config.supabaseSync),
      urlConfigured: Boolean(config.supabaseSync.supabaseUrl),
      serviceKeyPresent: Boolean(config.supabaseSync.serviceKey),
      bucket: config.supabaseSync.bucket || ""
    },
    databaseReworkQueueEnabled: Boolean(config.databaseStore),
    estimatedCallsPerCandidate: {
      image: expectedImageCalls,
      video: expectedVideoCalls
    },
    samplePlan: {
      name: samplePlan.name,
      species: samplePlan.species,
      element: samplePlan.element,
      actionCount: samplePlan.actions.length,
      actionIds: samplePlan.actions.map((action) => action.id)
    },
    note:
      "Config check does not call GA and does not read or print API keys."
  };
  console.log(JSON.stringify(safeConfig, null, 2));
}

function fxForAction(action) {
  if (action.id.includes("electric") || action.id.includes("static")) return "soft_static";
  if (action.id.includes("moon")) return "moon_glow";
  if (action.id.includes("feed")) return "snack_sparkle";
  if (action.id.includes("attention")) return "friendly_ping";
  if (action.category === "adaptive-habit") return "species_signature";
  return "none";
}

async function loadLearningContext({ runRoot, limit }) {
  const notes = await readJsonLinesIfExists(path.join(runRoot, "ga-learning-notes.jsonl"));
  const recent = notes
    .filter((note) => typeof note?.lesson === "string" && note.lesson.trim())
    .slice(-limit);
  const instruction = recent
    .map((note) => {
      const action = note.actionId ? `action ${note.actionId}: ` : "";
      const tags = Array.isArray(note.tags) && note.tags.length > 0
        ? `[${note.tags.join(", ")}] `
        : "";
      const guidance = note.guidance || guidanceForTags(note.tags);
      return `${action}${tags}${guidance ? `${guidance} ` : ""}${note.lesson}`;
    })
    .join(" | ")
    .slice(0, 2400);
  return {
    count: recent.length,
    instruction
  };
}

async function findNextReworkRequest(config, options = {}) {
  if (config.databaseStore) {
    try {
      const databaseRecords = await config.databaseStore.readReworkRecords();
      const databaseRequest = selectNextReworkRequest(databaseRecords, options);
      if (databaseRequest) {
        return databaseRequest;
      }
    } catch (error) {
      console.error(`[ga-worker] database-rework-read-failed error=${safeErrorMessage(error)}`);
    }
  }

  const records = await readJsonLinesIfExists(path.join(config.runRoot, "ga-rework-queue.jsonl"));
  return selectNextReworkRequest(records, options);
}

export function selectNextReworkRequest(records = [], options = {}) {
  const startedTimeoutMinutes = Number.isFinite(options.startedTimeoutMinutes)
    ? options.startedTimeoutMinutes
    : 180;
  const nowMs = Number.isFinite(options.nowMs) ? options.nowMs : Date.now();
  const latestStatusByRequest = new Map();

  for (const record of records) {
    if (record?.schema === "gamer.ga-pet-rework-status.v1" && record.requestId) {
      latestStatusByRequest.set(record.requestId, record);
    }
  }

  return records.find(
    (record) => {
      if (
        record?.schema !== "gamer.ga-pet-rework-request.v1" ||
        record.status !== "requested" ||
        !record.requestId
      ) {
        return false;
      }

      return canStartReworkRequest({
        latestStatus: latestStatusByRequest.get(record.requestId),
        startedTimeoutMinutes,
        nowMs
      });
    }
  ) || null;
}

function canStartReworkRequest({ latestStatus, startedTimeoutMinutes, nowMs }) {
  if (!latestStatus) return true;
  if (latestStatus.status === "completed" || latestStatus.status === "failed") {
    return false;
  }
  if (latestStatus.status !== "started") return true;
  return isStartedReworkStale({ latestStatus, startedTimeoutMinutes, nowMs });
}

function isStartedReworkStale({ latestStatus, startedTimeoutMinutes, nowMs }) {
  if (startedTimeoutMinutes <= 0) return false;
  const startedAtMs = Date.parse(latestStatus.createdAt || "");
  if (!Number.isFinite(startedAtMs)) return true;
  return nowMs - startedAtMs >= startedTimeoutMinutes * 60 * 1000;
}

async function appendReworkStatus(config, status) {
  await appendFile(
    path.join(config.runRoot, "ga-rework-queue.jsonl"),
    `${JSON.stringify({
      schema: "gamer.ga-pet-rework-status.v1",
      requestId: status.requestId,
      sourceRunId: status.sourceRunId || "",
      targetRunId: status.targetRunId || "",
      status: status.status,
      error: status.error || "",
      createdAt: new Date().toISOString()
    })}\n`,
    "utf8"
  );

  if (config.databaseStore) {
    try {
      await config.databaseStore.appendReworkStatus(status);
    } catch (error) {
      console.error(`[ga-worker] database-rework-status-failed error=${safeErrorMessage(error)}`);
    }
  }
}

async function readPromptPlanForRework(config, request) {
  if (!request?.sourceRunId) return null;
  const localPlan = await readJsonIfExists(
    path.join(config.runRoot, request.sourceRunId, "source", "generation", "prompt-plan.json")
  );
  if (localPlan) return localPlan;

  if (config.databaseStore) {
    try {
      return await config.databaseStore.readPromptPlan(request.sourceRunId);
    } catch (error) {
      console.error(`[ga-worker] database-rework-prompt-read-failed error=${safeErrorMessage(error)}`);
    }
  }

  return null;
}

async function generateVideoReference({ config, prompt, runDir }) {
  if (config.apiProvider === "l0veyou") {
    return generateL0veYouVideoReference({ config, prompt, runDir });
  }

  const operation = await postJson({
    apiProvider: config.apiProvider,
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
      apiProvider: config.apiProvider,
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
      apiProvider: config.apiProvider,
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

async function generateL0veYouVideoReference({ config, prompt, runDir }) {
  const responseJson = await postJson({
    apiProvider: config.apiProvider,
    apiKey: config.apiKey,
    url: `${config.apiBaseUrl.replace(/\/+$/u, "")}/api/generate/${encodeURIComponent(config.videoModel)}`,
    timeoutMs: config.requestTimeoutMs,
    body: {
      client_task_id: createClientTaskId("gamer-video"),
      prompt,
      duration: config.videoDurationSeconds,
      aspect_ratio: config.spriteSheetAspectRatio || config.imageAspectRatio
    }
  });

  const operationPath = "artifacts/video/operation.json";
  await writeJsonFile(path.join(runDir, operationPath), redactOperationForLog(responseJson));

  const video = extractGeneratedVideo(responseJson);
  if (!video) {
    return {
      done: Boolean(responseJson.done || responseJson.completed || responseJson.status === "completed"),
      filePath: "",
      operationPath
    };
  }

  const filePath = "artifacts/video/motion-reference.mp4";
  if (video.base64) {
    await writeFile(path.join(runDir, filePath), Buffer.from(stripDataUrl(video.base64), "base64"));
  } else if (video.uri) {
    const videoBytes = await downloadBinary({
      apiProvider: config.apiProvider,
      apiKey: config.apiKey,
      url: resolveApiUrl(config.apiBaseUrl, video.uri),
      timeoutMs: config.requestTimeoutMs
    });
    await writeFile(path.join(runDir, filePath), videoBytes);
  }

  return {
    done: true,
    filePath,
    operationPath
  };
}

async function postJson({ apiProvider, apiKey, url, body, timeoutMs, headers = {} }) {
  return requestJson({
    apiProvider,
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

async function getJson({ apiProvider, apiKey, url, timeoutMs }) {
  return requestJson({
    apiProvider,
    apiKey,
    url,
    timeoutMs,
    init: {
      method: "GET"
    }
  });
}

async function requestJson({ apiProvider, apiKey, url, init, timeoutMs }) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      ...init,
      headers: {
        ...(init.headers || {}),
        ...authHeadersForProvider(apiProvider, apiKey)
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

async function downloadBinary({ apiProvider, apiKey, url, timeoutMs }) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      headers: authHeadersForProvider(apiProvider, apiKey),
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
    responseJson?.image,
    responseJson?.image_data,
    responseJson?.imageData,
    responseJson?.image_url,
    responseJson?.imageUrl,
    responseJson?.url,
    responseJson?.result,
    responseJson?.output,
    responseJson?.output_image,
    responseJson?.outputImage,
    responseJson?.images?.[0],
    responseJson?.files?.[0],
    responseJson?.artifacts?.[0],
    responseJson?.data?.image,
    responseJson?.data?.image_url,
    responseJson?.data?.imageUrl,
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
  if (typeof candidate === "string") {
    if (candidate.startsWith("data:image/")) {
      return {
        base64: candidate,
        uri: "",
        mimeType: mimeTypeFromDataUrl(candidate) || "image/png"
      };
    }
    if (/^https?:\/\//iu.test(candidate) || candidate.startsWith("/")) {
      return {
        base64: "",
        uri: candidate,
        mimeType: "image/png"
      };
    }
    return null;
  }
  if (!candidate || typeof candidate !== "object") return null;
  const base64 =
    candidate.data ||
    candidate.image ||
    candidate.imageData ||
    candidate.image_data ||
    candidate.imageBase64 ||
    candidate.image_base64 ||
    candidate.base64 ||
    candidate.imageBytes ||
    candidate.image_bytes ||
    candidate.bytesBase64Encoded ||
    candidate.b64_json ||
    candidate.b64Json ||
    candidate.inlineData?.data ||
    candidate.inline_data?.data;
  const uri =
    candidate.uri ||
    candidate.url ||
    candidate.imageUrl ||
    candidate.image_url ||
    candidate.outputUrl ||
    candidate.output_url ||
    candidate.gcsUri ||
    candidate.gcs_uri;
  if (
    (typeof base64 !== "string" || base64.trim() === "") &&
    (typeof uri !== "string" || uri.trim() === "")
  ) {
    return null;
  }
  return {
    base64,
    uri,
    mimeType:
      candidate.mime_type ||
      candidate.mimeType ||
      mimeTypeFromDataUrl(base64) ||
      candidate.inlineData?.mimeType ||
      candidate.inline_data?.mime_type ||
      "image/png"
  };
}

function extractGeneratedVideo(operation) {
  const videos = [
    operation?.video,
    operation?.video_url,
    operation?.videoUrl,
    operation?.output_video,
    operation?.outputVideo,
    operation?.result,
    operation?.output,
    operation?.data,
    operation?.response?.generatedVideos?.[0]?.video,
    operation?.response?.generated_videos?.[0]?.video,
    operation?.response?.videos?.[0],
    operation?.response?.predictions?.[0]?.video
  ];
  for (const video of videos) {
    if (typeof video === "string") {
      if (video.startsWith("data:video/")) return { base64: video, uri: "" };
      if (/^https?:\/\//iu.test(video) || video.startsWith("/")) {
        return { base64: "", uri: video };
      }
    }
    if (!video || typeof video !== "object") continue;
    const base64 =
      video.video_data ||
      video.videoData ||
      video.video_base64 ||
      video.videoBase64 ||
      video.base64 ||
      video.bytesBase64Encoded ||
      video.bytes_base64_encoded ||
      video.data ||
      video.inlineData?.data ||
      video.inline_data?.data;
    const uri =
      video.uri ||
      video.video_url ||
      video.videoUrl ||
      video.output_url ||
      video.outputUrl ||
      video.gcsUri ||
      video.gcs_uri ||
      video.url;
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

function mimeTypeFromDataUrl(value) {
  const match = /^data:([^;,]+)[;,]/iu.exec(String(value ?? ""));
  return match?.[1] || "";
}

function toDataUrl(bytes, mimeType) {
  return `data:${mimeType || "image/png"};base64,${Buffer.from(bytes).toString("base64")}`;
}

function resolveApiUrl(baseUrl, maybeUrl) {
  const text = String(maybeUrl ?? "").trim();
  if (/^https?:\/\//iu.test(text)) return text;
  const base = String(baseUrl ?? "").replace(/\/+$/u, "");
  const pathPart = text.replace(/^\/+/u, "");
  return `${base}/${pathPart}`;
}

function buildReviewCard({
  runId,
  plan,
  imageResult,
  videoResult,
  motionResults,
  status,
  generatedActionCount,
  expectedActionCount
}) {
  const actionLines =
    motionResults.length > 0
      ? motionResults.map((result) => {
          const marker = result.status === "generated" ? "ok" : "failed";
          return `- ${marker}: ${result.action.id} / ${result.action.displayName} / ${result.path}`;
        })
      : ["- identity-only mode: no motion sheets generated"];

  return [
    `# GA random pet candidate ${runId}`,
    "",
    `Status: ${status}`,
    `Name: ${plan.name}`,
    `Summary: ${plan.summary}`,
    `Image mime: ${imageResult.mimeType}`,
    `Motion sheets: ${generatedActionCount}/${expectedActionCount}`,
    `Video reference: ${videoResult?.filePath || "not generated"}`,
    "",
    "## Image Prompt",
    "",
    plan.imagePrompt,
    "",
    "## Motion Sheets",
    "",
    ...actionLines,
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
    "- Keep running the worker during the quota window to accumulate full resource candidates.",
    "- Later, batch-triage identity drift, transparency, frame count, and runtime anchor quality.",
    "- Do not mark this package as human-reviewed or publish it until a separate acceptance step exists."
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

function parseOptionalString(value, fallback) {
  if (value === undefined) return fallback;
  const text = String(value).trim();
  if (["", "default", "none", "auto"].includes(text.toLowerCase())) return "";
  return text;
}

function parseApiProvider(value, apiBaseUrl = "") {
  const provider = String(value || "").trim().toLowerCase();
  if (["l0veyou", "l0ve-you", "love-you", "love-you-proxy"].includes(provider)) {
    return "l0veyou";
  }
  if (["google", "google-genai", "gemini"].includes(provider)) {
    return "google-genai";
  }
  if (String(apiBaseUrl || "").toLowerCase().includes("l0veyou.com")) {
    return "l0veyou";
  }
  return "google-genai";
}

function parsePackageMode(value, fallback) {
  const mode = String(value || fallback || "full").trim().toLowerCase();
  return ["identity", "full"].includes(mode) ? mode : fallback;
}

function parseBackgroundMode(value, fallback) {
  const mode = String(value || fallback || "transparent").trim().toLowerCase();
  return ["transparent", "chroma", "light", "auto"].includes(mode) ? mode : fallback;
}

function parseQualityPreset(value, fallback) {
  const preset = String(value || fallback || "high").trim().toLowerCase();
  return ["fast", "balanced", "high"].includes(preset) ? preset : fallback;
}

function qualityDefaultsFor(preset) {
  if (preset === "fast") {
    return {
      identityImageSize: "1K",
      spriteSheetImageSize: "2K"
    };
  }
  if (preset === "balanced") {
    return {
      identityImageSize: "2K",
      spriteSheetImageSize: "2K"
    };
  }
  return {
    identityImageSize: "2K",
    spriteSheetImageSize: "4K"
  };
}

function parseImageSize(value, fallback) {
  const size = String(value || fallback || "2K").trim();
  return size || fallback;
}

function l0veYouResolution(imageSize) {
  const size = String(imageSize || "2K").trim().toLowerCase();
  if (size === "4k") return "4k";
  if (size === "2k") return "2k";
  if (size === "1k") return "1k";
  return size || "2k";
}

function l0veYouSquareSize(imageSize) {
  const resolution = l0veYouResolution(imageSize);
  if (resolution === "4k") return "4096x4096";
  if (resolution === "1k") return "1024x1024";
  return "2048x2048";
}

function mimeTypeToImageFormat(mimeType) {
  const text = String(mimeType || "image/png").toLowerCase();
  if (text.includes("jpeg") || text.includes("jpg")) return "jpeg";
  if (text.includes("webp")) return "webp";
  return "png";
}

function authHeadersForProvider(apiProvider, apiKey) {
  if (apiProvider === "l0veyou") {
    return {
      Authorization: `Bearer ${apiKey}`
    };
  }
  return {
    "x-goog-api-key": apiKey
  };
}

function createClientTaskId(prefix) {
  return `${prefix}-${Date.now()}-${crypto.randomBytes(4).toString("hex")}`;
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

function safeApiError(json) {
  const message = json?.error?.message || json?.error || "request_failed";
  return String(message).replace(/AIza[0-9A-Za-z_-]+/gu, "[REDACTED_API_KEY]");
}

function safeErrorMessage(error) {
  const message = error instanceof Error ? error.message : String(error);
  return message
    .replace(/AIza[0-9A-Za-z_-]+/gu, "[REDACTED_API_KEY]")
    .replace(/sb_secret_[A-Za-z0-9_-]+/gu, "[REDACTED_SUPABASE_SECRET]")
    .replace(/postgres(?:ql)?:\/\/[^@\s]+@/giu, "postgresql://[REDACTED]@");
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
  loadEnvFiles();
  runGaRandomPetWorker().catch((error) => {
    console.error(`[ga-worker] fatal ${safeErrorMessage(error)}`);
    process.exitCode = 1;
  });
}
