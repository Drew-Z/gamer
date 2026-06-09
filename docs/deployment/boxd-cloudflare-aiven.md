# Boxd + Cloudflare + Aiven Deployment

This is the production-shaped deployment skeleton for Gamer community services.
It keeps the app server, object storage, relational data, and fantasy-pet
generation service in separate trust boundaries.

## Target Topology

```text
Android app
  -> Cloudflare DNS / WAF / HTTPS
  -> Cloudflare Tunnel
  -> Boxd Docker services
       - community-api
       - admin-review
  -> Aiven PostgreSQL
  -> Cloudflare R2
  -> fantasy-pet generation server
```

Boxd runs the Node services and `cloudflared`. Cloudflare Tunnel is the public
entry point, so the Boxd VM does not need public inbound ports for the API.
Aiven PostgreSQL is the future primary relational database. Cloudflare R2 is the
future object store for candidate previews, generated showcase assets, motion
sheets, and final `pet.zip` packages.

## Service Responsibilities

- `community-api`: public mobile/community API, import drafts, submissions,
  wallet state, feed, daily check-in, and approved pet registry.
- `admin-review`: protected review console. It calls `community-api` over the
  internal Docker network.
- `cloudflared`: outbound-only Cloudflare Tunnel connector.
- `Aiven PostgreSQL`: users, wallets, ledger entries, feed posts, submissions,
  review decisions, approved pet registry, and asset metadata.
- `Cloudflare R2`: binary objects only. Store object keys, byte counts, hashes,
  content types, and public download ids in PostgreSQL.
- `fantasy-pet generation server`: separate public app API for job creation,
  polling, candidate artifacts, human review, and package download.

Do not commit real secrets. Keep production values in
`deploy/boxd/.env.production` on the Boxd VM or in the Boxd host environment.

## Environment

Copy the template on the Boxd VM:

```bash
cp deploy/boxd/.env.production.example deploy/boxd/.env.production
```

Required values:

- `DATABASE_URL`: Aiven PostgreSQL primary URI or PgBouncer URI. Prefer the
  PgBouncer URI once the API has real database access.
- `AIVEN_CA_CERT_PATH`: local path for the Aiven CA certificate when the runtime
  uses CA verification.
- `CLOUDFLARED_TOKEN`: Cloudflare Tunnel token.
- `FANTASY_PET_API_BASE_URL`: public fantasy-pet app API URL.
- `R2_ACCOUNT_ID`: Cloudflare account id for R2.
- `R2_BUCKET_NAME`: R2 bucket name, for example `gamer-pet-assets`.
- `R2_ACCESS_KEY_ID`: R2 access key id.
- `R2_SECRET_ACCESS_KEY`: R2 access key value.
- `R2_PUBLIC_BASE_URL`: optional public or worker-mediated asset base URL.

The Android production build should use Cloudflare hostnames:

```powershell
$env:COMMUNITY_API_BASE_URL = "https://api.example.com"
$env:FANTASY_PET_API_BASE_URL = "https://petgen.example.com"
```

## Cloudflare Tunnel

Create a Tunnel in Cloudflare Zero Trust and point public hostnames to Docker
services:

- `api.example.com` -> `http://community-api:4000`
- `review.example.com` -> `http://admin-review:4200`

Then set `CLOUDFLARED_TOKEN` on the Boxd VM. The compose file runs:

```bash
cloudflared tunnel --no-autoupdate run --token "$CLOUDFLARED_TOKEN"
```

This follows the Cloudflare Tunnel model where `cloudflared` creates outbound
connections to Cloudflare and proxies requests back to private services.

## Cloudflare R2

Use Cloudflare R2 for generated assets:

- candidate preview images
- accepted candidate images
- motion sheets
- final `pet.zip`
- community showcase images

The app and public API should never expose server filesystem paths. Store only
public ids and R2 object keys in PostgreSQL. Return app-safe `downloadId`,
`downloadUrl`, object metadata, or a server-mediated signed URL.

## Aiven PostgreSQL

Aiven PostgreSQL should become the source of truth for:

- users and profile state
- wallet ledger entries
- daily check-in records
- import drafts and submissions
- admin review decisions
- approved pet registry
- feed posts and reactions
- R2 asset metadata

The first production schema migration lives in:

```text
services/community-api/db/migrations/001_initial_community_schema.sql
```

It defines the data model for the public app, admin review flow, wallet ledger,
approved pet registry, and R2 asset metadata. Runtime traffic still defaults to
the in-memory store until a Postgres-backed store is implemented and explicitly
enabled. Filling `DATABASE_URL` prepares the container environment; it does not
by itself switch persistence yet.

For the API container, prefer a pooled PgBouncer URI when concurrent mobile
traffic starts increasing.

Dry-run pending migrations before applying them:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production run --rm community-api npm run migrate:community-db:dry-run
```

Apply migrations after reviewing the dry-run result:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production run --rm community-api npm run migrate:community-db
```

Both commands use `DATABASE_URL` from `deploy/boxd/.env.production`. They
initialize or update the database schema only; they do not switch the running
community API from memory-backed state to PostgreSQL-backed state.

## Fantasy-Pet Boundary

The fantasy-pet generation server remains separate. The app and `community-api`
may call only public generation endpoints. They must not call admin worker,
shell, GenericAgent, Codex, or internal file-path endpoints.

Keep these rules:

- no `/admin/server-worker-cycle`
- no `/admin/pet-generation-jobs/{appJobId}/agent-outputs`
- no automatic human review accept
- no final package download before `downloadReady=true` or
  `nextAction=download-package`
- no internal paths, leases, prompt packs, adapter configs, SSH data, or keys in
  app responses

## Commands

For direct VM setup, follow the Boxd runbook:

```text
deploy/boxd/README.md
```

Render the production-shaped compose config locally:

```powershell
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production.example config
```

Run on the Boxd VM after filling `deploy/boxd/.env.production`:

```bash
deploy/boxd/deploy.sh
```

The helper validates Compose, runs migration dry-run by default, optionally
applies migrations when `RUN_MIGRATIONS=1`, then starts services:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production up -d --build
```

Run local contract checks before deploying:

```powershell
npm.cmd test
node --test services/community-api/src/database/migrations.test.js services/community-api/src/database/config.test.js
node --test services/community-api/src/database/*.test.js
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production.example config
git diff --check
```
