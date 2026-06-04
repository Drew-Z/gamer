# Gamer

Pet-first community ecosystem workspace.

## Structure

```text
gamer/
  apps/
  services/
    community-api/
    pet-generator/
  packages/
    community-contracts/
    pet-package-spec/
    pet-runtime/
  docs/
```

## Local

Run tests:

```powershell
npm.cmd test
```

Run services:

```powershell
npm.cmd run start:community-api
npm.cmd run start:pet-generator
```

## Docker

Run the service skeletons with Docker Compose:

```powershell
docker compose up --build
```

The default ports are:

- Community API: `http://localhost:4000`
- Pet Generator Adapter: `http://localhost:4100`
