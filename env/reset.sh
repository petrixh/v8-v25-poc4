#!/bin/bash
# Return this copy to the seed state: the database from the seed snapshot taken by up.sh, the mail sink emptied.
# APP_ENV_RESET_MODE=restart (default): the app's domain is stopped, the database restored, the domain started again,
#   so no in-memory state survives (sessions, the Hibernate cache of users, roles and infrastructure).
# APP_ENV_RESET_MODE=db: the database only, the app keeps running (seconds instead of about a minute). Logged-in
#   sessions and cached users, roles and infrastructure survive: use it only when tests do not change those.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
t0=$SECONDS
docker container inspect "$APP_ENV_CONTAINER" >/dev/null 2>&1 || die "copy is not running (no container $APP_ENV_CONTAINER)"
snapshot_exists || die "no seed snapshot in this copy: run env/up.sh"
case "$APP_ENV_RESET_MODE" in
  restart) domain stop; snapshot_restore || die "restore failed"; domain start ;;
  db) snapshot_restore || die "restore failed" ;;
  *) die "APP_ENV_RESET_MODE must be restart or db: $APP_ENV_RESET_MODE" ;;
esac
mail=$(compose port mail 8025 2>/dev/null || true)
[ -z "$mail" ] || curl -sf -m 10 -X DELETE "http://$mail/api/v1/messages" >/dev/null || die "could not empty the mail sink"
wait_ui 300 || die "app did not come back after the reset"
log "RESET ($APP_ENV_RESET_MODE) in $((SECONDS - t0)) s (seeded $(cat "$COPY_DIR/seed-date" 2>/dev/null || echo ?))"
