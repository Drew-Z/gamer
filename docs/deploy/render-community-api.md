# Render Community API Deployment

The Render deployment is defined by the repository-root `render.yaml`. It creates
one Docker Web Service from `main` and waits for GitHub Actions checks before an
automatic deploy.

## Storage decision

Render Free web services have an ephemeral filesystem and cannot attach a
persistent disk. The Render start command therefore requires `DATABASE_URL` and
refuses to fall back to `services/community-api/data/community-store.json`.

Use a dedicated PostgreSQL database for Community API data. Do not reuse the ERP
database or its schema. A free Render Postgres database expires after 30 days, so
use an external durable PostgreSQL provider for a longer-lived deployment.

## Create the Blueprint

1. Merge the deployment branch into GitHub `main` and wait for `Community API CI`.
2. In Render, select **New > Blueprint** and connect `Drew-Z/gamer`.
3. Keep the Blueprint path as `render.yaml`.
4. Enter every value prompted with `sync: false`; never commit those values.
5. Confirm the service is in the same practical region as the database. The
   default Blueprint region is Singapore.

Required values:

```text
DATABASE_URL
COMMUNITY_DEMO_TOKEN
FANTASY_PET_API_BASE_URL
FANTASY_PET_UPSTREAM_TOKEN
```

Generate a dedicated Community API token with Node.js:

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
```

Set these when browser-based admin review is enabled:

```text
COMMUNITY_CORS_ALLOWED_ORIGINS
COMMUNITY_ADMIN_REVIEW_TRUSTED_ORIGINS
```

`POSTGRES_SSLMODE=verify-full`, production error redaction, and bounded request
rate limiting are enabled by the Blueprint. The current Aiven deployment uses a
project CA: upload it as a Render secret file named `aiven-project-ca.pem` before
the first deploy. The Blueprint sets `AIVEN_CA_CERT_PATH` to
`/etc/secrets/aiven-project-ca.pem`.

Render Free rejects `maxShutdownDelaySeconds`, even though standalone Blueprint
validation accepts the field. Keep the platform default shutdown delay on the
Free plan.

## Deployment behavior

Render Free does not support a pre-deploy command. The Blueprint overrides the
image command with `services/community-api/src/render-start.js`, which:

1. rejects a missing `DATABASE_URL` or `COMMUNITY_DEMO_TOKEN`;
2. applies all pending Community API migrations;
3. starts the HTTP server only after migrations succeed;
4. closes the server on `SIGINT` or `SIGTERM`.

Render checks `GET /readyz`. Unlike `GET /health`, this endpoint reads from the
configured store, so a broken database connection or migration prevents the
deployment from becoming healthy.

## Verification

Run locally before merging:

```powershell
npm.cmd ci --ignore-scripts
npm.cmd test
docker build -f services/community-api/Dockerfile -t gamer-community-api:render .
```

GitHub's `Community API CI` runs `npm run test:community-api:ci`, the subset that
is self-contained in this repository checkout. The full `npm test` command also
checks Windows tooling and integration contracts that use the sibling
`fantasy-pet-rule` workspace, so keep running it from the local pet workspace.

After Render deploys, replace the host and run:

```powershell
curl.exe -fsS https://gamer-community-api.onrender.com/health
curl.exe -fsS https://gamer-community-api.onrender.com/readyz
```

`/health` proves that the process is alive. `/readyz` proves that the configured
store can serve a query. Test one token-protected read and write before changing
the Android API base URL or shutting down the old service.

## Cutover and rollback

1. Keep the old Community API stopped but recoverable during a seven-day window.
2. Change the Android Community API base URL only after Render health, readiness,
   authentication, and one write/read round trip pass.
3. Preserve the old environment file and Community API snapshot outside Git.
4. Roll back from Render's deploy history or redeploy the previous Git commit.

References:

- <https://render.com/docs/blueprint-spec>
- <https://render.com/docs/deploys#pre-deploy-command>
- <https://render.com/docs/free>
