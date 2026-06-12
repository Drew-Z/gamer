# GA Random Pet Worker

This worker is a temporary high-throughput path for the 24-hour Google AI image/video quota window. It creates random original desktop-pet resource candidates, records prompts and outcomes, and defaults to a full package mode with base identity art plus desktop-pet motion sheets.

It does not call admin APIs, does not publish to community, and does not mark any package as human-reviewed. Full candidates are tagged as `ga-auto-generated` / `auto-generated-unverified` so they can be accumulated first and reviewed or repaired later.

## Secret Handling

- Put the key only in the server environment as `GEMINI_API_KEY` or `GOOGLE_API_KEY`.
- Do not paste the key into code, Android, admin UI, docs, commits, or logs.
- The worker sends the key via the `x-goog-api-key` header and redacts API-key-looking strings from error logs.
- If the key comes from `image.docx`, extract it on the server or into the server panel environment only. Do not commit the document.

## Fast 24-Hour Server Run

Recommended server environment:

```powershell
GEMINI_API_KEY=<server-secret>
GA_PET_RUN_ROOT=/data/ga-random-pets
GA_PET_PACKAGE_MODE=full
GA_PET_QUALITY_PRESET=high
GA_PET_IMAGE_SIZE=2K
GA_PET_SPRITESHEET_IMAGE_SIZE=4K
GA_PET_BACKGROUND_MODE=transparent
GA_PET_OUTPUT_MIME_TYPE=image/png
GA_PET_LOOP=1
GA_PET_MAX_RUNS=0
GA_PET_BATCH_SIZE=1
GA_PET_INTERVAL_SECONDS=60
GA_PET_ACTION_INTERVAL_SECONDS=0
GA_PET_ENABLE_VIDEO=1
GA_PET_VIDEO_DURATION_SECONDS=5
GA_PET_VIDEO_MAX_POLLS=30
```

Start command:

```powershell
npm run start:ga-random-pet-worker
```

Before burning quota, run a no-network config check:

```powershell
npm run check:ga-random-pet-worker
```

Equivalent direct command:

```powershell
node services/pet-generator/src/ga-random-pet-worker.js --config-check
```

Confirm the output shows `apiKeyPresent: true`, `packageMode: full`, the intended image sizes, and `backgroundMode: transparent` for nano-style transparent PNG generation. This check does not call GA and does not print the key.

For a cautious first run, set `GA_PET_MAX_RUNS=1`. A full resource package calls image generation once for the identity image and then once per planned motion sheet, so one candidate is already a meaningful server test. After confirming output folders are being created, change it back to `0` for continuous generation during the quota window.

If the proxy documented in `image.docx` uses different size or output names, keep the key in the server panel and change only the environment variables. For example, nano-style transparent PNG generation should keep `GA_PET_BACKGROUND_MODE=transparent` and `GA_PET_OUTPUT_MIME_TYPE=image/png`. A channel that cannot generate transparency should use `GA_PET_BACKGROUND_MODE=chroma` or `light`, then post-process later.

## Output Layout

Each candidate is written under:

```text
GA_PET_RUN_ROOT/
  ga-<timestamp>-<ordinal>-<slug>/
    assets/base_identity.png
    artifacts/candidates/base-identity.png
    artifacts/video/motion-reference.mp4
    artifacts/video/operation.json
    meta/motion_map.json
    meta/runtime.json
    motion/sheets/<action>.png
    previews/preview.png
    source/generation/prompt-plan.json
    source/generation/api-trace.json
    source/generation/actions/<action>.json
    license.json
    manifest.json
    ownership-claim.json
    package-manifest.json
    score-report.json
    review-card.md
    exports/ga-<...>-full-resource-candidate.zip
  ga-experience.jsonl
```

Video files appear only when `GA_PET_ENABLE_VIDEO=1` and the Veo operation returns downloadable media.

## Human Review Console

The admin review app now exposes a GA random pet review panel. Start the existing admin console and point it at the same run root:

```powershell
GA_PET_RUN_ROOT=/data/ga-random-pets
npm run start:admin-review
```

Open the admin console and use the `GA Random Pets` section. It reads candidates directly from `GA_PET_RUN_ROOT`, displays previews and motion sheets, and writes:

```text
GA_PET_RUN_ROOT/
  ga-learning-notes.jsonl
  ga-rework-queue.jsonl
  ga-<run-id>/
    human-feedback.jsonl
    human-feedback-latest.json
    source/generation/rework-requests.jsonl
```

Each candidate card has an expandable `Evidence and history` section. Use it to open `prompt-plan.json`, `api-trace.json`, `motion_map.json`, `runtime.json`, manifests, `review-card.md`, feedback logs, rework requests, and video references when present. Motion sheets also expose an `Open original` link so the spritesheet can be inspected at source size before writing feedback.

The panel also shows a live review summary: total candidates, shown candidates, learning notes, feedback count, queued/running/completed/failed reworks, and the most frequent issue tags.

Feedback decisions:

- `Looks good`: records a positive note for later triage.
- `Hold`: records an observation without queueing a rework.
- `Rework`: queues a repair-style request for the same pet concept.
- `Regenerate pet`: queues a stronger regeneration request while preserving useful concept notes.
- `Reject`: records a negative lesson so future prompts avoid similar failures.

Use issue tags consistently so the worker can translate them into prompt guidance:

- `identity-drift`: different face/body/colors between sheets.
- `static-frames`: frames barely change.
- `scale-pop`: body size, center, or ground anchor jumps.
- `bad-transparency` / `white-matte`: alpha, matte, or edge contamination problems.
- `cropped-body`: ears, tail, wings, props, or effects are cut off.
- `wrong-action`: motion does not match the desktop-pet trigger.
- `too-noisy`: particles or effects overpower the character.
- `weak-silhouette`: small-size readability is poor.
- `style-mismatch`: render style differs from the identity image.

The worker reads recent `ga-learning-notes.jsonl` entries and injects them into later identity and motion-sheet prompts. When `GA_PET_REWORK_QUEUE=1` is enabled, it also consumes `ga-rework-queue.jsonl` before starting a random pet, producing a new rework run linked to the source candidate.

## Motion Plan

Full mode uses two layers of actions:

- Core desktop-pet interactions: idle, tap reaction, drag hold, drag release, feed, sleep, wake, roam, waiting/review, attention, and failed.
- Adaptive habit actions: selected from the generated species and element, plus one signature action. A fox may get scent/tail motions; a mouse may get whisker or quick-dash motions; a dragon may get wing or breath motions.

Each generated action is requested as a single horizontal spritesheet. `meta/motion_map.json` records frame count, loop behavior, trigger, status, and sheet path.

## Review Flow

1. Let the worker accumulate full resource candidates during the quota window.
2. Later open `review-card.md`, `previews/preview.png`, and the files under `motion/sheets/`.
3. Check originality, small-size readability, transparency or clean chroma key, frame count, identity drift, and action semantics.
4. Use `motion-reference.mp4` only as a motion-quality reference. The app still consumes PNG motion sheets.
5. Only after a separate acceptance step should `acceptedBy` become `human-review` for community import.

## Important Environment Variables

- `GA_PET_IMAGE_MODEL`: defaults to `gemini-3.1-flash-image`.
- `GA_PET_PACKAGE_MODE`: `full` creates base art plus motion sheets; `identity` creates only the base identity package.
- `GA_PET_QUALITY_PRESET`: `high` defaults identity art to `2K` and spritesheets to `4K`; `balanced` uses `2K`/`2K`; `fast` uses `1K`/`2K`.
- `GA_PET_IMAGE_SIZE`: defaults to the quality preset's identity size.
- `GA_PET_IMAGE_ASPECT_RATIO`: defaults to `1:1`.
- `GA_PET_SPRITESHEET_IMAGE_SIZE`: defaults to the quality preset's spritesheet size.
- `GA_PET_SPRITESHEET_ASPECT_RATIO`: defaults to `16:9`.
- `GA_PET_BACKGROUND_MODE`: `transparent`, `chroma`, `light`, or `auto`.
- `GA_PET_OUTPUT_MIME_TYPE`: defaults to `image/png`; set to `default` to omit the field if the proxy rejects it.
- `GA_PET_IMAGE_DELIVERY`: optional; set to the proxy's URI/inline value if needed.
- `GA_PET_CUSTOM_ACTION_COUNT`: defaults to `3` species-habit actions in addition to core actions and signature.
- `GA_PET_REQUIRE_ALL_ACTIONS`: set to `1` only if a missing action should fail the whole package.
- `GA_PET_LEARNING_NOTE_LIMIT`: defaults to `12`; recent human notes injected into future prompts.
- `GA_PET_REWORK_QUEUE`: defaults to on; set to `0` to ignore queued rework requests.
- `GA_PET_CONFIG_CHECK`: set to `1` to print a safe config summary and exit without generation.
- `GA_PET_ENABLE_VIDEO`: defaults to off.
- `GA_PET_VIDEO_MODEL`: defaults to `veo-3.1-generate-preview`.
- `GA_PET_MAX_RUNS`: `0` means unlimited until the process is stopped.
- `GA_PET_INTERVAL_SECONDS`: pause between batches when loop mode is enabled.
- `GA_PET_ACTION_INTERVAL_SECONDS`: optional pause between per-action image calls.

## Why This Is Separate From Codex

Codex should not spend scarce quota on repeated image/video generation. This worker moves the expensive loop to GA while keeping the product gates intact: generated full resource candidates accumulate quickly, then Codex or a human can review, repair, package, and wire the accepted assets into Android/community flows later.
