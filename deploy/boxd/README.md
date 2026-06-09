# Boxd Direct Deployment Runbook

This runbook is for deploying Gamer community services directly on a Boxd VM.
Boxd provides the Ubuntu host and Docker runtime. Cloudflare Tunnel remains the
public ingress, Aiven PostgreSQL remains the relational database, Cloudflare R2
remains object storage, and the fantasy-pet generation server remains a
separate public API.

## 1. Create and enter the Boxd VM

Create the VM from your local machine:

```bash
ssh boxd.sh new --name=gamer-prod
```

Connect to it:

```bash
ssh gamer-prod.boxd.sh
```

Boxd VMs are full Linux machines, so the deployment runs from a normal git
checkout with Docker Compose.

## 2. Clone the repository

On the Boxd VM:

```bash
git clone <repo-url> gamer
cd gamer
```

Use your real repository URL in place of `<repo-url>`.

## 3. Fill production environment values

Create the local production env file:

```bash
cp deploy/boxd/.env.production.example deploy/boxd/.env.production
```

Do not commit `deploy/boxd/.env.production`. It must stay only on the Boxd VM
or in a protected secret store.

Fill at least:

- `CLOUDFLARED_TOKEN`: Cloudflare Tunnel token for this deployment.
- `DATABASE_URL`: Aiven PostgreSQL URI, preferably PgBouncer for app traffic.
- `FANTASY_PET_API_BASE_URL`: public fantasy-pet app API base URL.
- `R2_ACCOUNT_ID`: Cloudflare account id.
- `R2_BUCKET_NAME`: R2 bucket for previews, accepted assets, and packages.
- `R2_ACCESS_KEY_ID`: R2 access key id.
- `R2_SECRET_ACCESS_KEY`: R2 secret access key.

The production file must not contain local `file://` references for
fantasy-pet artifacts and must not expose worker, admin, SSH, prompt pack, or
server path information to the Android app.

## 4. Configure Cloudflare Tunnel

In Cloudflare Zero Trust, create a Tunnel and map public hostnames to the Docker
service names:

```text
api.example.com -> http://community-api:4000
review.example.com -> http://admin-review:4200
```

Use your real domain names in place of `example.com`. Keep the review hostname
behind Cloudflare Access or another admin-only control before using it outside
a private test.

The fantasy-pet generation server is not deployed in this compose file. Point
`FANTASY_PET_API_BASE_URL` at that separate public app API.

## 5. Validate compose and database migrations

Render the production compose config:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production config
```

Dry-run pending database migrations:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production run --rm community-api npm run migrate:community-db:dry-run
```

Apply migrations only after reviewing the dry-run output:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production run --rm community-api npm run migrate:community-db
```

The current API runtime still uses in-memory community state until the
PostgreSQL-backed store is implemented and enabled. These migrations prepare
the Aiven schema; they do not switch runtime persistence by themselves.

## 6. Deploy services

Use the helper from the Boxd VM:

```bash
deploy/boxd/deploy.sh
```

By default it validates compose, runs a migration dry-run, builds images, starts
containers, and prints service status.

To apply migrations before starting services:

```bash
RUN_MIGRATIONS=1 deploy/boxd/deploy.sh
```

To skip the migration dry-run for an emergency redeploy:

```bash
RUN_MIGRATION_DRY_RUN=0 deploy/boxd/deploy.sh
```

You can also run Docker Compose directly:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production up -d --build
```

## 7. Check status

On the Boxd VM:

```bash
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production ps
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production logs --tail=100 community-api
docker compose -f compose.boxd.yaml --env-file deploy/boxd/.env.production logs --tail=100 cloudflared
```

From your machine, verify the public API hostname through Cloudflare:

```bash
curl https://api.example.com/health
```

Replace `api.example.com` with your production API hostname.
