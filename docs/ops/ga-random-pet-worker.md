# GA Random Pet Worker

This worker is a temporary high-throughput path for the 24-hour Google AI image/video quota window. It creates random original desktop-pet candidates, records prompts and outcomes, and leaves every result in `waiting-human-review`.

It does not call admin APIs, does not publish to community, and does not mark any package as human-reviewed.

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
GA_PET_LOOP=1
GA_PET_MAX_RUNS=0
GA_PET_BATCH_SIZE=1
GA_PET_INTERVAL_SECONDS=90
GA_PET_ENABLE_VIDEO=1
GA_PET_VIDEO_DURATION_SECONDS=5
GA_PET_VIDEO_MAX_POLLS=30
```

Start command:

```powershell
npm run start:ga-random-pet-worker
```

For a cautious first run, set `GA_PET_MAX_RUNS=3`. After confirming output folders are being created, change it back to `0` for continuous generation during the quota window.

## Output Layout

Each candidate is written under:

```text
GA_PET_RUN_ROOT/
  ga-<timestamp>-<ordinal>-<slug>/
    artifacts/candidates/base-identity.png
    artifacts/video/motion-reference.mp4
    artifacts/video/operation.json
    source/generation/prompt-plan.json
    source/generation/api-trace.json
    package-manifest.json
    review-card.md
    ga-<...>-candidate.zip
  ga-experience.jsonl
```

Video files appear only when `GA_PET_ENABLE_VIDEO=1` and the Veo operation returns downloadable media.

## Review Flow

1. Open `review-card.md` and `base-identity.png`.
2. Check originality, small-size readability, no text/watermark, separable body parts, and motion potential.
3. Use `motion-reference.mp4` only as a motion-quality reference. The Android app still needs PNG motion sheets.
4. If accepted, generate/repair action sheets and then produce a human-reviewed package.
5. Only after human review should `acceptedBy` become `human-review` for community import.

## Important Environment Variables

- `GA_PET_IMAGE_MODEL`: defaults to `gemini-3.1-flash-image`.
- `GA_PET_IMAGE_SIZE`: defaults to `1K`.
- `GA_PET_IMAGE_ASPECT_RATIO`: defaults to `1:1`.
- `GA_PET_ENABLE_VIDEO`: defaults to off.
- `GA_PET_VIDEO_MODEL`: defaults to `veo-3.1-generate-preview`.
- `GA_PET_MAX_RUNS`: `0` means unlimited until the process is stopped.
- `GA_PET_INTERVAL_SECONDS`: pause between batches when loop mode is enabled.

## Why This Is Separate From Codex

Codex should not spend scarce quota on repeated image/video generation. This worker moves the expensive loop to GA while keeping the product gates intact: generated candidates accumulate quickly, then Codex or a human can review, repair, package, and wire the accepted assets into Android/community flows later.
