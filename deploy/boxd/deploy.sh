#!/usr/bin/env sh
set -eu

ENV_FILE="${ENV_FILE:-deploy/boxd/.env.production}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.boxd.yaml}"
RUN_MIGRATION_DRY_RUN="${RUN_MIGRATION_DRY_RUN:-1}"
RUN_MIGRATIONS="${RUN_MIGRATIONS:-0}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE" >&2
  echo "Copy deploy/boxd/.env.production.example and fill production values on the Boxd VM." >&2
  exit 1
fi

echo "Validating Docker Compose config..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" config

if [ "$RUN_MIGRATION_DRY_RUN" = "1" ]; then
  echo "Running community database migration dry-run..."
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" run --rm community-api npm run migrate:community-db:dry-run
else
  echo "Skipping migration dry-run because RUN_MIGRATION_DRY_RUN=$RUN_MIGRATION_DRY_RUN"
fi

if [ "$RUN_MIGRATIONS" = "1" ]; then
  echo "Applying community database migrations..."
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" run --rm community-api npm run migrate:community-db
else
  echo "Skipping migration apply. Set RUN_MIGRATIONS=1 to apply migrations."
fi

echo "Building and starting services..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build

echo "Service status:"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
