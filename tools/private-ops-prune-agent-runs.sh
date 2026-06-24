#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
days=${FANTASY_PET_RUN_RETENTION_DAYS:-14}
apply=${PRIVATE_OPS_PRUNE_APPLY:-0}

cd "$repo_root"

if [ "$apply" = "1" ]; then
  docker compose \
    -f compose.yaml \
    -f compose.fantasy-pet.yaml \
    -f compose.private-ops.yaml \
    --profile fantasy-pet \
    --profile private-ops \
    exec -T fantasy-pet-api \
    find /data/runs -mindepth 1 -maxdepth 1 -type d -mtime "+$days" -print -exec rm -rf {} +
else
  docker compose \
    -f compose.yaml \
    -f compose.fantasy-pet.yaml \
    -f compose.private-ops.yaml \
    --profile fantasy-pet \
    --profile private-ops \
    exec -T fantasy-pet-api \
    find /data/runs -mindepth 1 -maxdepth 1 -type d -mtime "+$days" -print
fi
