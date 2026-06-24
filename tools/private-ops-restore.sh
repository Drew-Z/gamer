#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  printf '%s\n' "usage: tools/private-ops-restore.sh <community-db-backup.sql>" >&2
  exit 2
fi

backup_file=$1
if [ ! -f "$backup_file" ]; then
  printf '%s\n' "backup file not found: $backup_file" >&2
  exit 2
fi

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_root"

: "${COMMUNITY_POSTGRES_DB:=gamer}"
: "${COMMUNITY_POSTGRES_USER:=gamer}"

set -- --env-file "${PRIVATE_OPS_ENV_FILE:-.env.private-ops}" \
  -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml
if [ -n "${PRIVATE_OPS_COMPOSE_OVERRIDE_FILE:-}" ]; then
  set -- "$@" -f "$PRIVATE_OPS_COMPOSE_OVERRIDE_FILE"
fi
if [ -n "${PRIVATE_OPS_COMPOSE_PROJECT_NAME:-}" ]; then
  set -- -p "$PRIVATE_OPS_COMPOSE_PROJECT_NAME" "$@"
fi

docker compose "$@" --profile private-ops exec -T community-db \
  psql -v ON_ERROR_STOP=1 -U "$COMMUNITY_POSTGRES_USER" "$COMMUNITY_POSTGRES_DB" < "$backup_file"

docker compose "$@" --profile fantasy-pet --profile private-ops \
  run --rm community-migrate npm run migrate:community-db:dry-run
