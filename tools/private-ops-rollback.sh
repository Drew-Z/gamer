#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  printf '%s\n' "usage: tools/private-ops-rollback.sh <target-release-tag>" >&2
  exit 2
fi

target_release=$1
if [ -z "$target_release" ]; then
  printf '%s\n' "target release tag must not be blank" >&2
  exit 2
fi

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_root"

env_file=${PRIVATE_OPS_ENV_FILE:-.env.private-ops}
set -- --env-file "$env_file" \
  -f compose.yaml -f compose.fantasy-pet.yaml -f compose.private-ops.yaml
if [ -n "${PRIVATE_OPS_COMPOSE_OVERRIDE_FILE:-}" ]; then
  set -- "$@" -f "$PRIVATE_OPS_COMPOSE_OVERRIDE_FILE"
fi
if [ -n "${PRIVATE_OPS_COMPOSE_PROJECT_NAME:-}" ]; then
  set -- -p "$PRIVATE_OPS_COMPOSE_PROJECT_NAME" "$@"
fi

if [ "${PRIVATE_OPS_ROLLBACK_APPLY:-0}" != "1" ]; then
  printf '%s\n' "private ops rollback plan"
  printf 'targetRelease=%s\n' "$target_release"
  printf 'apply=false\n'
  printf '%s\n' "Set PRIVATE_OPS_ROLLBACK_APPLY=1 to stop the worker, run migration dry-run, and recreate services with release image tags."
  printf '%s\n' "After apply, run npm run smoke:private-ops and inspect logs before reopening live generation."
  exit 0
fi

export GAMER_IMAGE_TAG=${GAMER_IMAGE_TAG:-$target_release}
export FANTASY_PET_IMAGE_TAG=${FANTASY_PET_IMAGE_TAG:-$target_release}

docker compose "$@" --profile fantasy-pet --profile private-ops \
  stop fantasy-pet-worker-daemon || true

docker compose "$@" --profile fantasy-pet --profile private-ops \
  run --rm community-migrate npm run migrate:community-db:dry-run

docker compose "$@" --profile fantasy-pet --profile private-ops \
  up -d --no-build --wait \
  community-api admin-review private-ops-proxy \
  fantasy-pet-api fantasy-pet-worker-daemon

printf 'private ops rollback applied targetRelease=%s gamerImageTag=%s fantasyPetImageTag=%s\n' \
  "$target_release" "$GAMER_IMAGE_TAG" "$FANTASY_PET_IMAGE_TAG"
