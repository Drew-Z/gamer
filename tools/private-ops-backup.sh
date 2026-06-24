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

docker compose \
  -f compose.yaml \
  -f compose.fantasy-pet.yaml \
  -f compose.private-ops.yaml \
  --profile private-ops \
  exec -T community-db \
  pg_dump -U "$COMMUNITY_POSTGRES_USER" "$COMMUNITY_POSTGRES_DB" > "$output_file"

printf '%s\n' "$output_file"
