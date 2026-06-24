#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
days=${FANTASY_PET_RUN_RETENTION_DAYS:-14}
apply=${PRIVATE_OPS_PRUNE_APPLY:-0}

cd "$repo_root"

set -- --env-file "${PRIVATE_OPS_ENV_FILE:-.env.private-ops}" \
  -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml \
  --profile fantasy-pet --profile private-ops
if [ -n "${PRIVATE_OPS_COMPOSE_OVERRIDE_FILE:-}" ]; then
  set -- "$@" -f "$PRIVATE_OPS_COMPOSE_OVERRIDE_FILE"
fi
if [ -n "${PRIVATE_OPS_COMPOSE_PROJECT_NAME:-}" ]; then
  set -- -p "$PRIVATE_OPS_COMPOSE_PROJECT_NAME" "$@"
fi

if [ "$apply" = "1" ]; then
  docker compose "$@" exec -T fantasy-pet-api \
    find /data/runs -mindepth 1 -maxdepth 1 -type d -mtime "+$days" -print -exec rm -rf {} +
else
  docker compose "$@" exec -T fantasy-pet-api \
    find /data/runs -mindepth 1 -maxdepth 1 -type d -mtime "+$days" -print
fi
