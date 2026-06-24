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

docker compose \
  -f compose.yaml \
  -f compose.fantasy-pet.yaml \
  -f compose.private-ops.yaml \
  --profile private-ops \
  exec -T community-db \
  psql -U "$COMMUNITY_POSTGRES_USER" "$COMMUNITY_POSTGRES_DB" < "$backup_file"

docker compose \
  -f compose.yaml \
  -f compose.fantasy-pet.yaml \
  -f compose.private-ops.yaml \
  --profile fantasy-pet \
  --profile private-ops \
  run --rm community-migrate npm run migrate:community-db:dry-run
