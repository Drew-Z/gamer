# Gamer

Pet-first app and community ecosystem workspace.

`gamer` is primarily the app/community repository. It owns the Android app,
community API, admin review prototype, and shared app-facing packages. The
server-side generation rules, worker orchestration, private pipeline, QA gates,
and public app API contract are provided by `fantasy-pet-rule` (deployed to
Baidu server).

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

Optional in-memory request limiting can be enabled for the public community API
with `COMMUNITY_RATE_LIMIT_ENABLED=1`. Tune the window and budgets with
`COMMUNITY_RATE_LIMIT_WINDOW_MS`, `COMMUNITY_RATE_LIMIT_WRITE_MAX`, and
`COMMUNITY_RATE_LIMIT_READ_MAX`. It is disabled by default so local and existing
deployments keep their current behavior until the server opts in.

For a private demo gateway, set `COMMUNITY_DEMO_TOKEN` on the Community API and
Android demo build. Android sends it as `X-Demo-Token`. Set
`FANTASY_PET_UPSTREAM_TOKEN` only on the Community API server so proxied
fantasy-pet requests can authenticate upstream without exposing that token to
the app.

## Verification

Run the standard verification set before committing a phase:

```powershell
npm.cmd test
node --test services/community-api/src/database/migrations.test.js services/community-api/src/database/config.test.js
node --test services/community-api/src/database/*.test.js
cd apps\android-community && .\gradlew testDebugUnitTest --console=plain && cd ..\..
docker compose config
docker compose -f compose.yaml -f compose.fantasy-pet.yaml --profile fantasy-pet config
docker compose -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml --profile fantasy-pet --profile private-ops config
npm.cmd run smoke:private-ops
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

Run the hiden private Community API stack with Postgres, migration gating,
healthchecks, Community demo auth, a protected Baidu fantasy-pet Agent upstream,
and a Caddy TLS/Basic Auth reverse proxy:

```powershell
Copy-Item .env.private-ops.example .env.private-ops
# Edit .env.private-ops with private values before continuing.
# Set PRIVATE_OPS_DEPLOYMENT_ROLE=community on hiden.
# Set FANTASY_PET_API_BASE_URL to the private Baidu Agent API URL.
# Generate CADDY_ADMIN_BASIC_AUTH_HASH with:
# docker run --rm caddy:2-alpine caddy hash-password --plaintext "REPLACE_WITH_PRIVATE_PASSWORD"
# When storing the generated bcrypt hash in .env.private-ops, escape each
# dollar sign as $$ so Docker Compose keeps the hash literal.

npm.cmd run preflight:private-ops
docker compose -f compose.yaml -f compose.private-ops.yaml --profile private-ops config
docker compose -f compose.yaml -f compose.private-ops.yaml --profile private-ops up --build -d community-db community-migrate community-api admin-review private-ops-proxy
docker compose -f compose.yaml -f compose.private-ops.yaml --profile private-ops run --rm community-migrate npm run migrate:community-db:dry-run
npm.cmd run smoke:private-ops
```

For a single-host local or fallback drill where `gamer` also starts the
`fantasy-pet-rule` Agent containers, keep `PRIVATE_OPS_DEPLOYMENT_ROLE=combined`
and include `compose.fantasy-pet.yaml` plus the `fantasy-pet` profile:

```powershell
docker compose -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml --profile fantasy-pet --profile private-ops config
docker compose -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml --profile fantasy-pet --profile private-ops up --build -d community-db community-migrate fantasy-pet-api fantasy-pet-worker-daemon community-api admin-review private-ops-proxy
docker compose -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml --profile fantasy-pet --profile private-ops run --rm community-migrate npm run migrate:community-db:dry-run
npm.cmd run smoke:private-ops
```

`tools/private-ops-preflight.js` reads `${PRIVATE_OPS_ENV_FILE}` or
`.env.private-ops` before deployment and checks that required private values are
present and no longer look like placeholders. In `community` role it verifies
the hiden Community API has a remote `FANTASY_PET_API_BASE_URL` for the Baidu
Agent. In `combined` role it also verifies
`FANTASY_PET_ADAPTER_CONFIG_FILE` points at an existing private adapter config.
The failure output names missing or placeholder variables without printing the
configured secret values.

`tools/private-ops-smoke.js` checks `/health`, `/v1/sla`,
`/worker-readiness`, `/app-api-contract`, missing-token rejection, and one
token-authenticated Community write without printing configured secret values.
It also verifies `FANTASY_PET_UPSTREAM_TOKEN` is present before running. When
the smoke goes through Caddy Basic Auth, set `PRIVATE_OPS_BASIC_AUTH_USER` and
`PRIVATE_OPS_BASIC_AUTH_PASSWORD` in `.env.private-ops`. Set
`PRIVATE_OPS_KNOWN_APP_JOB_ID` to monitor a human-reviewed known job package
gate; the smoke accepts either a ready ZIP or a stable gated JSON response. It
does not create a live generation job unless `PRIVATE_OPS_CREATE_JOB=1` is set
for a quota-approved window.

For hiden-style direct Node deployments that expose `admin-review` without the
private Caddy proxy, set `PRIVATE_OPS_BASIC_AUTH_USER` and
`PRIVATE_OPS_BASIC_AUTH_PASSWORD` in `.env.local`; `admin-review` will require
Basic Auth before serving the Web UI or proxying Community/Agent requests,
while keeping `/health` public for platform health checks. When
`COMMUNITY_BASE_URL` points at this public admin-review surface, run the smoke
with `PRIVATE_OPS_SMOKE_SURFACE=admin-review`; the smoke verifies the public
Basic Auth gate and calls the protected
`/ops/internal-community-auth-check` endpoint so `admin-review` can verify raw
Community API missing-token behavior from inside the target host. Set
`PRIVATE_OPS_REQUIRE_POSTGRES=1` during the target database gate; the smoke then
also calls `/ops/community-db-readiness`, which verifies Postgres is configured
and a migration dry-run reports zero pending migrations without printing
`DATABASE_URL`. Set `PRIVATE_OPS_REQUIRE_DB_BACKUP_DRILL=1` for the target
backup gate; the smoke then calls `/ops/community-db-backup-drill`, which reads
the current Postgres tables, restores bounded row samples into temporary tables,
and reports only table/row counts.

For hiden/direct `admin-review` private ops deployments,
`PRIVATE_OPS_DEPLOYMENT_ROLE=community` enables the in-process synthetic
monitor by default. Set `PRIVATE_OPS_MONITOR_ENABLED=0` to disable it, or tune
`PRIVATE_OPS_MONITOR_INTERVAL_MS`, `PRIVATE_OPS_MONITOR_STALE_AFTER_MS`, and
`PRIVATE_OPS_MONITOR_HISTORY_LIMIT` when the platform needs different
retention windows. The monitor writes one token-safe JSON summary line to
stdout per run and keeps bounded recent history at
`/ops/private-ops-monitor-status`. During deployment verification, set
`PRIVATE_OPS_REQUIRE_MONITOR=1`; the smoke calls
`/ops/private-ops-monitor-status?run=1` and requires the latest synthetic probe
to pass.

For hiden/direct `admin-review` deployments that cannot install system
cron/logrotate, set `PRIVATE_OPS_REQUIRE_HOOKS=1` during the final recurring
operations gate. The smoke calls `/ops/private-ops-hooks-audit` through the same
Basic Auth protected admin-review surface and requires
`npm run audit:private-ops-hooks` to pass without exposing configured secret
fragments.
For a protected remote audit without shell access, call
`/ops/private-ops-hooks-audit?fresh=1` through the Basic Auth protected
admin-review entry; `fresh=1` makes the server require the recurring smoke log
to be recent and successful.

After the private entry is placed behind TLS, set `PRIVATE_OPS_REQUIRE_TLS=1`
in the deployment smoke environment. This makes `tools/private-ops-smoke.js`
fail before sending requests unless `COMMUNITY_BASE_URL` uses `https://`.

Operational helpers:

```powershell
tools\private-ops-backup.sh
tools\private-ops-restore.sh backups\community-db-YYYYMMDDTHHMMSSZ.sql
node tools\private-ops-target-hooks-audit.js
npm.cmd run audit:private-ops-hooks
tools\private-ops-prune-agent-runs.sh
tools\private-ops-rollback.sh private-ops-v0.2
```

After installing the target scheduler and log retention hooks, run
`npm.cmd run audit:private-ops-hooks`. For the final target gate, set
`PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG=1` so the audit also requires the recurring
smoke log to be recent, successful, and free of configured secret fragments.
Override `PRIVATE_OPS_CRON_FILE`, `PRIVATE_OPS_LOGROTATE_FILE`,
`PRIVATE_OPS_LOG_DIR`, and `PRIVATE_OPS_SMOKE_LOG_FILE` when the platform uses
non-default paths.

On hiden-style hosts that allow only user-level access and a fixed Node startup
file, system cron/logrotate cannot be installed. Use the built-in user-level
hook instead: set `PRIVATE_OPS_HOOKS_MODE=user`,
`PRIVATE_OPS_USER_HOOKS_ENABLED=1`, `COMMUNITY_BASE_URL` to the HTTPS
admin-review URL, `PRIVATE_OPS_SMOKE_SURFACE=admin-review`, and the same smoke
requirements used by the live gate, including `PRIVATE_OPS_REQUIRE_HOOKS=1` for
the final hook audit. `index.js` starts
`tools/private-ops-user-hooks.js`, waits briefly after boot, runs
`tools/private-ops-smoke.js` every five minutes, writes
`.private-ops/logs/private-ops-smoke.log`, rotates it in user space, and writes
`.private-ops/logs/private-ops-user-hooks.json` for audit. Verify this mode
with:

```powershell
$env:PRIVATE_OPS_HOOKS_MODE = "user"
$env:PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG = "1"
npm.cmd run audit:private-ops-hooks
```

If shell access is not available, verify through the protected Web entry by
running smoke with `PRIVATE_OPS_REQUIRE_HOOKS=1` and
`PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG=1`; it calls
`/ops/private-ops-hooks-audit?fresh=1` and returns only redacted audit status.

The helpers default to `.env.private-ops` and the default compose project. For
operator drills that use a custom project name or temporary override file, set
`PRIVATE_OPS_ENV_FILE`, `PRIVATE_OPS_COMPOSE_PROJECT_NAME`, or
`PRIVATE_OPS_COMPOSE_OVERRIDE_FILE`.

`tools/private-ops-rollback.sh` is plan-only by default. To apply a rollback to
already-built release images, set `PRIVATE_OPS_ROLLBACK_APPLY=1`; then run
`npm.cmd run smoke:private-ops` and inspect logs before reopening live
generation.

For hiden-style Git deployments, use `GAMER_RELEASE_REF` as the direct runtime
rollback pin. Set it to a release tag such as `private-ops-v0.16` and restart
hiden; `index.js` will fetch that ref, checkout `FETCH_HEAD`, and start the
checked-out code instead of pulling `origin/main`. Remove `GAMER_RELEASE_REF`
and restart to resume the normal `AUTO_UPDATE`/`GAMER_AUTO_UPDATE` mainline
path.

Use `deploy/private-ops-cron.example` for a 5-minute synthetic smoke probe and
`deploy/private-ops-logrotate.conf` for local smoke/ops log rotation. The
target hook audit turns those installed files plus the latest smoke log into a
token-safe verification artifact.

The default ports are:

- Community API: `http://localhost:4000`
- Pet Generator Adapter: `http://localhost:4100`
- Admin Review Prototype: `http://localhost:4200`
- Fantasy Pet Public API: `http://127.0.0.1:8765`
- Private Ops Proxy: `https://localhost`

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
In private ops, the browser talks to admin-review through the private proxy;
admin-review injects the server-side `COMMUNITY_DEMO_TOKEN` when it proxies to
Community API, so the token is not shipped to browser JavaScript. When
`COMMUNITY_CORS_ALLOWED_ORIGINS` or
`COMMUNITY_ADMIN_REVIEW_TRUSTED_ORIGINS` is configured, Community admin review
writes require a matching browser `Origin` or `Referer`.

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

The Android prototype lives in `apps/android-community`. The project now keeps
its own Gradle wrapper in-repo so Android work stays inside `gamer/`:

- Android Gradle Plugin `9.2.0`
- Kotlin `2.2.10`
- Compose BOM `2025.12.00`
- Compile SDK `36`

Versions are centralized in:

```text
apps/android-community/gradle/libs.versions.toml
```

List Android projects:

```powershell
cd apps/android-community
.\gradlew.bat projects
```

Run Android unit tests:

```powershell
cd apps/android-community
.\gradlew.bat testDebugUnitTest
```

Build the debug APK:

```powershell
cd apps/android-community
.\gradlew.bat assembleDebug
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
