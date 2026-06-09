# HidenCloud Node.js Deployment

This target is for HidenCloud / Pterodactyl-style Node.js hosting where the
startup command is fixed and the panel runs:

```bash
npm install
node /home/container/${MAIN_FILE}
```

The root `index.js` only starts `community-api`. It does not start
`admin-review`, `pet-generator`, or `fantasy-pet-rule`.

## Server Type

- Server Image: Nodejs 23

Node.js 23 satisfies the repository engine requirement of Node.js 22 or newer.

## Startup Variables

Use these values in the HidenCloud startup panel:

```text
Git Repo Address: https://github.com/Drew-Z/gamer
Install Branch: main
User Uploaded Files: 0
Auto Update: 1
Additional Node Packages:
Git Username:
Git Access Token:
Uninstall Node Packages:
Main File: index.js
Additional Arguments:
```

If the GitHub repository is private, either make it accessible to the panel or
fill `Git Username` and `Git Access Token` with a GitHub token that can read the
repository. Do not paste that token into this repository or chat history.

## Ports

Use the primary allocation for the community API:

```text
Primary / P1: 24674
Secondary / P2: 25483
```

Set one of these environment variables if the panel lets you add custom
variables:

```text
PORT=24674
```

If the panel injects a Pterodactyl-style `SERVER_PORT`, the API can also read
that. Port priority is:

```text
PORT -> SERVER_PORT -> 4000
```

Keep `25483` unused for now. It can later host `admin-review` or another
separate service if HidenCloud allows a second server process.

## Runtime Environment

For the first HidenCloud deployment, the community API can start with these
values empty while the external services are still being prepared:

```text
DATABASE_URL=
FANTASY_PET_API_BASE_URL=
R2_ACCOUNT_ID=
R2_BUCKET_NAME=gamer-pet-assets
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_PUBLIC_BASE_URL=
```

When Aiven PostgreSQL, Cloudflare R2, and the fantasy-pet public app API are
ready, fill them in the panel's environment-variable area rather than committing
secrets to git.

## Health Check

After startup, open:

```text
http://<hidencloud-host>:24674/health
```

Expected response:

```json
{"ok":true,"service":"community-api"}
```
