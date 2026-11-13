#!/bin/bash
# Stop and remove this copy only: its compose project's containers, network and database volume, and its state dir.
# Safe after a half-finished up.sh and when the copy is already gone. Other copies and the shared build stay.
set -uo pipefail
source "$(dirname "$0")/lib.sh"
compose down --volumes --remove-orphans --timeout 20 >/dev/null 2>&1 || log "WARNING: docker compose down reported an error"
# A container left without compose labels (e.g. compose interrupted mid-create) is removed only if it carries this copy's label
for c in "$APP_ENV_CONTAINER" "$APP_ENV_PROJECT-db" "$APP_ENV_PROJECT-mail" "$APP_ENV_PROJECT-keycloak"; do
  [ "$(docker inspect -f '{{index .Config.Labels "sormas-env.project"}}' "$c" 2>/dev/null)" = "$APP_ENV_PROJECT" ] && docker rm -f -v "$c" >/dev/null 2>&1
done
rm -rf "$COPY_DIR"
log "DOWN"
exit 0
