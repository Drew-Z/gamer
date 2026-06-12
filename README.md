# Gamer

Pet-first app and community ecosystem workspace.

`gamer` is primarily the app/community repository. It owns the Android app,
community API, admin review prototype, and shared app-facing packages. The
server-side generation rules, worker orchestration, private pipeline, QA gates,
and public app API contract live in the sibling repository:
`D:\workspace4Codex\pet\fantasy-pet-rule`.

## Structure

```text
gamer/
  apps/
    admin-review/           # Admin review prototype.
    android-community/      # Android app prototype.
  services/
    community-api/          # App/community backend and app gateway.
    pet-generator/          # Pet generation adapter/service shell.
  packages/
    community-contracts/    # Shared API contracts and drift guards.
    pet-package-spec/       # pet.zip package and manifest rules.
    pet-runtime/            # Shared pet runtime helpers.
  docs/                     # Project docs, API notes, specs, agent config.
  tools/                    # Local smoke, verification, and helper scripts.
```

## Docs

- Community API contract: `docs/api/community-api.md`
- Ecosystem design spec: `docs/superpowers/specs/2026-06-04-gamer-pet-community-ecosystem-design.md`
- Community PostgreSQL migrations: `services/community-api/db/migrations`

## Local

Run tests:

```powershell
npm.cmd test
```

Preview or apply community database migrations when `DATABASE_URL` points at
PostgreSQL:

```powershell
npm.cmd run migrate:community-db:dry-run
npm.cmd run migrate:community-db
```

For server deployments that cannot define custom environment variables in the
panel, place the same server-only values in `.env.local` at the repository root.
`index.js` and the migration CLI load `.env.local` automatically without adding
another dependency. The file is ignored by git and must not be committed.
When `DATABASE_URL` is configured, the community API also applies pending
migrations automatically before seeding or reading Postgres state.

The Community API runtime keeps state in a JSON snapshot by default at
`services/community-api/data/community-store.json`, so imported drafts,
submissions, review decisions, approved pets, feed posts, wallet entries, and
daily check-ins survive service restarts. Override the path with
`COMMUNITY_API_STORE_FILE`; set it to `memory` or `none` for a temporary
in-memory run. Docker Compose mounts this path on the `community-api-data`
volume.

`GET /health` includes `release.commit` when the deployment sets `GIT_COMMIT`,
`COMMIT_SHA`, `SOURCE_VERSION`, or `RENDER_GIT_COMMIT`.

## Verification

Run the standard verification set before committing a phase:

```powershell
npm.cmd test
node --test services/community-api/src/database/migrations.test.js services/community-api/src/database/config.test.js
node --test services/community-api/src/database/*.test.js
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community testDebugUnitTest --console=plain
docker compose config
docker compose -f compose.yaml -f compose.fantasy-pet.yaml --profile fantasy-pet config
git diff --check
```

Run services:

```powershell
node index.js
npm.cmd run start:admin-review
npm.cmd run start:community-api
npm.cmd run start:pet-generator
```

## Docker

Run the service skeletons with Docker Compose:

```powershell
docker compose up --build
```

Run the community services plus the public `fantasy-pet-rule` app API:

```powershell
docker compose -f compose.yaml -f compose.fantasy-pet.yaml --profile fantasy-pet up --build
```

The default ports are:

- Community API: `http://localhost:4000`
- Pet Generator Adapter: `http://localhost:4100`
- Admin Review Prototype: `http://localhost:4200`
- Fantasy Pet Public API: `http://127.0.0.1:8765`

## Community API

The Phase 2 API uses local in-memory state. It is useful for Android and admin
prototype integration before a database exists.

Read feed:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/feed
```

Read wallet:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/wallet/me
```

Claim daily check-in:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4000/v1/check-in -ContentType application/json -Body '{"date":"2026-06-05"}'
```

Create a pet submission:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4000/v1/submissions -ContentType application/json -Body '{"petId":"pet-new-001","ownershipClaimId":"claim-pet-new-001","scoreReportId":"score-pet-new-001"}'
```

Read one submission status:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/submissions/submission-local-002
```

Approve a submission and post a reward:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:4000/v1/admin/reviews -ContentType application/json -Body '{"submissionId":"submission-local-002","status":"approved","reviewer":"admin-demo","rewardAmount":55}'
```

Read the admin review queue:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/v1/admin/review-queue
```

## Admin Review Prototype

The static admin review prototype lives in `apps/admin-review`. It reads the
community API review queue and can approve, hold, reject, or revoke submissions.

Run it with the community API:

```powershell
npm.cmd run start:community-api
npm.cmd run start:admin-review
```

Then open:

```text
http://localhost:4200
```

## Android Community Prototype

The Android prototype lives in `apps/android-community`. It uses the local verified
baseline from `D:\workspace4Codex\pet\floating-pet-android`:

- Android Gradle Plugin `9.2.0`
- Kotlin `2.2.10`
- Compose BOM `2025.12.00`
- Compile SDK `36`

Versions are centralized in:

```text
apps/android-community/gradle/libs.versions.toml
```

This workspace currently reuses the known-good local Gradle wrapper from
`D:\workspace4Codex\pet\floating-pet-android`.

List Android projects:

```powershell
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community projects
```

Run Android unit tests:

```powershell
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community testDebugUnitTest
```

Build the debug APK:

```powershell
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community assembleDebug
```

### Fantasy Pet Generation API

The Android app should normally use the community API as its single backend
entry point. When `FANTASY_PET_API_BASE_URL` is set on the community API, the
community API proxies only the public `fantasy-pet-rule` app endpoints for the
desktop-pet generation loop. The `fantasy-pet-rule` service lives beside this
workspace:

```text
D:\workspace4Codex\pet\fantasy-pet-rule
```

Start the public app API without admin endpoints:

```powershell
Set-Location D:\workspace4Codex\pet\fantasy-pet-rule
uv run --with-requirements requirements-server.txt python tools\app_server.py --run-root runs --host 127.0.0.1 --port 8765
```

To run the community API as the local app gateway, point it at the public
fantasy-pet API before starting it:

```powershell
$env:FANTASY_PET_API_BASE_URL = "http://127.0.0.1:8765"
npm.cmd run start:community-api
```

The Android build reads `COMMUNITY_API_BASE_URL` and defaults to:

```text
http://10.0.2.2:4000
```

The Android build also reads `FANTASY_PET_API_BASE_URL`. If it is not set, it
defaults to `COMMUNITY_API_BASE_URL`, so create, poll, review, and package
download calls go through the community API proxy.

Use this default single-backend setup for emulator builds:

```powershell
$env:COMMUNITY_API_BASE_URL = "http://10.0.2.2:4000"
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community assembleDebug
```

Override both local API targets only when you intentionally want Android to
connect directly to the public `fantasy-pet-rule` server:

```powershell
$env:COMMUNITY_API_BASE_URL = "http://10.0.2.2:4000"
$env:FANTASY_PET_API_BASE_URL = "http://10.0.2.2:8765"
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community assembleDebug
```

Use `http://10.0.2.2:8765` for the Android emulator to reach the host machine's
`fantasy-pet-rule` server directly. Use `http://127.0.0.1:8765` only when the
app process and the server share the same network namespace.

Keep `npm.cmd run start:community-api` running when testing the generated
`pet.zip` import, community review submission, and submission status refresh.
The Android app uses public community endpoints such as
`/v1/import-drafts/submit` and `/v1/submissions/{submissionId}`; admin review
or approval remains a separate protected surface and is not called by the app.
The community API proxy also does not expose `/admin/*`, `server-worker-cycle`,
worker command routes, Codex routes, GenericAgent routes, or direct
image-generation controls to the app.

Public contract and smoke checks:

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:8765/app-api-contract
```

With the Docker overlay running, the same public contract is available at:

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:8765/app-api-contract
```

The Docker overlay also configures the community API proxy, so the app gateway
contract is available at:

```powershell
Invoke-RestMethod -Uri http://localhost:4000/app-api-contract
```

Run the app-side public lifecycle smoke from this repo:

```powershell
tools\smoke-fantasy-pet-public-lifecycle.cmd
```

The `.cmd` wrapper runs `tools\smoke-fantasy-pet-public-lifecycle.ps1` with a
local PowerShell execution-policy bypass. The smoke uses `fantasy-pet-rule`
server-side demo data to publish a candidate, then exercises only public app
endpoints: poll job, download candidate preview, confirm package download is
blocked before review, submit a human `accept` with `targetDownloadId`, and
download the final `pet.zip`. It does not enable or call admin endpoints.
This is an API contract smoke, not proof that the live generation worker stack
is running.

Run the fantasy-pet to community import smoke when you want to verify the
downloaded package can become a community import draft and submission:

```powershell
tools\smoke-fantasy-pet-community-import.cmd
```

The `.cmd` wrapper runs `tools\smoke-fantasy-pet-community-import.ps1`. It
reuses the public lifecycle smoke, reads the generated `pet.zip` manifest,
starts the local community API on a temporary port, then posts only to
`/v1/import-drafts/from-fantasy-pet-package` and `/v1/import-drafts/submit`.

After deploying or restarting HidenCloud, verify the public server state and
approved pet preview route:

```powershell
tools\verify-hidencloud-community.cmd
```

The script checks `/health`, `/v1/pets/approved`, the explicit
`assets.previewUrl` contract, the direct public artifact route, and the admin
`/api` proxy route. It calls the configured remote server only.

Run the full fantasy-pet integration verification before handing off a larger
change:

```powershell
tools\verify-fantasy-pet-integration.cmd
```

When an emulator is already running and you want the handoff check to include
the Compose connected tests plus the contract-demo Android UI smoke:

```powershell
tools\verify-fantasy-pet-integration.cmd -IncludeAndroidUi
```

The `.cmd` wrapper runs `tools\verify-fantasy-pet-integration.ps1`, which
serially runs JS tests, Android unit tests, Android debug build, both
fantasy-pet smoke scripts, the Android public-app forbidden surface scan, and
`git diff --check`. With `-IncludeAndroidUi`, it also runs
`connectedDebugAndroidTest` and
`tools\launch-fantasy-pet-android-ui-smoke.cmd -StartPublicApi -AssertContractDemoUi`,
then stops the temporary public API process started for that UI smoke. The
Android UI public API port defaults to `18765` in this aggregate verifier so an
already-running local `fantasy-pet-rule` service on `8765` can stay untouched.

Android emulator generation UI smoke:

Scripted setup and launch:

```powershell
tools\launch-fantasy-pet-android-ui-smoke.cmd -StartPublicApi
```

To also save a launch screenshot for the manual review record:

```powershell
tools\launch-fantasy-pet-android-ui-smoke.cmd -StartPublicApi -CaptureScreenshot
```

To drive the seeded contract-demo task through the Android UI and assert the
warning plus disabled Accept / Download pet.zip controls:

```powershell
tools\launch-fantasy-pet-android-ui-smoke.cmd -StartPublicApi -AssertContractDemoUi
```

The `.cmd` wrapper runs `tools\launch-fantasy-pet-android-ui-smoke.ps1`. It
seeds a public demo job in a temp run root, optionally starts
`tools\app_server.py` without admin flags, builds and installs the Android app
with `FANTASY_PET_API_BASE_URL=http://10.0.2.2:8765` and
`COMMUNITY_API_BASE_URL=http://10.0.2.2:4000`, clears the emulator app state,
and launches `com.gamer.community/.MainActivity`. Keep
`npm.cmd run start:community-api` running separately when testing import draft
creation and submission refresh.
Use `-SkipLaunch` when you only want to seed/build without requiring a running
emulator. Use `-CaptureScreenshot` to pull a PNG from the emulator into the
smoke run root after launch. Use `-AssertContractDemoUi` when the emulator is
available and you want the script to tap the launch bubble, poll
`public-lifecycle-smoke`, select the candidate, and verify that the contract
demo warning and no-live-worker copy are visible while Accept and Download
pet.zip stay disabled. The demo candidate seeded by
`run_server_job_lifecycle_demo.py` is a 1x1 transparent placeholder, not a real
generated desktop pet image. The script reports `screenshotLikelyBlank=true`
when the captured PNG is mostly
black; treat that as an emulator display/capture problem and restart or repair
the emulator before doing visual QA.

```powershell
# Terminal A: seed a public demo job and keep the public API running.
Set-Location D:\workspace4Codex\pet\fantasy-pet-rule
$runRoot = Join-Path $env:TEMP "fantasy-pet-android-ui"
Remove-Item -LiteralPath $runRoot -Recurse -Force -ErrorAction SilentlyContinue
uv run --with-requirements requirements-server.txt python tools\run_server_job_lifecycle_demo.py --run-dir "$runRoot\public-lifecycle-smoke" --app-job-id public-lifecycle-smoke --run-id public-lifecycle-smoke --description "A tiny stardust dragon desktop pet with smooth idle motion." --body-shape wide-tail
uv run --with-requirements requirements-server.txt python tools\app_server.py --run-root $runRoot --host 127.0.0.1 --port 8765
```

```powershell
# Terminal B: run the community API for package import and submission.
npm.cmd run start:community-api
```

```powershell
# Terminal C: install an emulator build pointed at the host service.
$env:FANTASY_PET_API_BASE_URL = "http://10.0.2.2:8765"
D:\workspace4Codex\pet\floating-pet-android\gradlew.bat -p D:\workspace4Codex\pet\gamer\apps\android-community installDebug --console=plain --rerun-tasks
adb devices
adb -s emulator-5554 shell pm clear com.gamer.community
adb -s emulator-5554 shell am start -n com.gamer.community/.MainActivity
```

In the app, tap the launch bubble, enter `public-lifecycle-smoke` in App job id,
tap Poll job, scroll to Candidate gallery, confirm the candidate preview renders,
and confirm the app shows both the contract-demo warning and the
`no live generation worker has run` message. The Android UI treats
`public-lifecycle-smoke` as pre-seeded public API validation data, so the
placeholder candidate does not mean the real generation worker stack has run,
and human review submission plus final package download stay disabled for that
job. Use a real non-demo generation job after the `fantasy-pet-rule` worker
stack is available when validating the full accept, package download, and
community import path from the app, including the Submit to community review and
Refresh community submission buttons.

Run the app-side public API drift guard:

```powershell
node --test packages/community-contracts/src/fantasy-pet-public-api-coverage.test.js
```

This guard builds the current `fantasy-pet-rule` app API contract and verifies
that the Android generation client only uses public endpoints. It allows
`/app-api-contract` as a documented non-runtime endpoint; any other public
endpoint that is not represented by the app appears in
`unexpectedUnhandledPublicEndpointPaths` and should be reviewed before the app
integration is considered current.

Run the community API package-import safety drift guard:

```powershell
node --test packages/community-contracts/src/fantasy-pet-community-api-safety-coverage.test.js
```

This guard reads the current `fantasy-pet-rule` handoff record and verifies that
the community API rejects every internal artifact basename when building import
drafts from downloaded `pet.zip` package manifests.
