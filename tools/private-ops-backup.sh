#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
output_dir=${1:-"$repo_root/backups"}
timestamp=$(date -u +"%Y%m%dT%H%M%SZ")
output_file="$output_dir/community-db-$timestamp.sql"

mkdir -p "$output_dir"

cd "$repo_root"

: "${COMMUNITY_POSTGRES_DB:=gamer}"
: "${COMMUNITY_POSTGRES_USER:=gamer}"

set -- --env-file "${PRIVATE_OPS_ENV_FILE:-.env.private-ops}" \
  -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml \
  --profile private-ops
if [ -n "${PRIVATE_OPS_COMPOSE_OVERRIDE_FILE:-}" ]; then
  set -- "$@" -f "$PRIVATE_OPS_COMPOSE_OVERRIDE_FILE"
fi
if [ -n "${PRIVATE_OPS_COMPOSE_PROJECT_NAME:-}" ]; then
  set -- -p "$PRIVATE_OPS_COMPOSE_PROJECT_NAME" "$@"
fi

docker compose "$@" exec -T community-db \
  pg_dump \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    -U "$COMMUNITY_POSTGRES_USER" \
    "$COMMUNITY_POSTGRES_DB" > "$output_file"

printf '%s\n' "$output_file"
