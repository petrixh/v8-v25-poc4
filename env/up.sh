#!/bin/bash
# Start one app copy (PostgreSQL 14, Payara 5.2022.5 with SORMAS, SMTP sink) in the seed state, on 127.0.0.1:$APP_ENV_PORT.
# Usage: env/up.sh [--no-build]     --no-build: reuse the image and the built artefacts when they exist (else build)
# Returns 0 once GET /sormas-ui/login answers 200 on the seeded copy. See env/README.md.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
NO_BUILD=0
case "${1:-}" in "") ;; --no-build) NO_BUILD=1 ;; *) echo "usage: $0 [--no-build]" >&2; exit 2 ;; esac
t0=$SECONDS

for c in "$APP_ENV_CONTAINER" "$APP_ENV_PROJECT-db" "$APP_ENV_PROJECT-mail"; do
  ! docker container inspect "$c" >/dev/null 2>&1 || die "container $c already exists: run env/down.sh with the same APP_ENV_* first"
done

if [ $NO_BUILD = 1 ] && docker image inspect "$APP_ENV_IMAGE" >/dev/null 2>&1 && [ -s "$APP_ENV_DEPLOY_DIR/apps/sormas-ui.war" ]; then
  log "using the existing image and artefacts (commit $(cut -c1-10 "$APP_ENV_DEPLOY_DIR/commit"))"
else
  "$ENV_DIR/build.sh"
fi

# Per-copy throwaway DB password: generated here, only in the state dir (mode 600) and the containers' env
mkdir -p "$COPY_DIR"
if [ ! -s "$COPY_DIR/db.env" ]; then
  (umask 077; pw=$(head -c 18 /dev/urandom | base64 | tr -dc A-Za-z0-9); printf 'SORMAS_DB_PW=%s\nPOSTGRES_PASSWORD=%s\n' "$pw" "$pw" > "$COPY_DIR/db.env")
fi

t1=$SECONDS
log "starting db, mail and app (app container $APP_ENV_CONTAINER, port $APP_ENV_PORT)"
compose up -d --quiet-pull >/dev/null 2>"$COPY_DIR/compose-up.log" || { cat "$COPY_DIR/compose-up.log" >&2; die "docker compose up failed"; }
wait_ui "$APP_ENV_WAIT_S" || { docker logs --tail 50 "$APP_ENV_CONTAINER" >&2 2>&1 || true; die "app did not come up"; }
docker logs "$APP_ENV_CONTAINER" 2>&1 | grep -q "DEPLOY FAILED" && die "a deployment failed (docker logs $APP_ENV_CONTAINER)"
t_start=$((SECONDS - t1))
log "app answers after ${t_start} s; seeding"

t1=$SECONDS
python3 "$ENV_DIR/seed/seed.py" "$BASE_URL" "$APP_ROOT/sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java" ||
  die "seeding failed"
# The app's cron job computes case completeness every 2 minutes and writes the seeded cases once. Wait for it, so the
# snapshot already holds that write and nothing changes the seeded rows behind a test's back after a reset.
log "waiting for the app's case completeness job (every 2 min) before the snapshot"
until [ "$(db_psql -d sormas_db -c "SELECT count(*) FROM cases WHERE completeness IS NULL")" = 0 ]; do
  [ $((SECONDS - t1)) -lt 300 ] || die "case completeness not computed after 300 s"
  sleep 5
done
date -u +%F > "$COPY_DIR/seed-date"
t_seed=$((SECONDS - t1))

# Snapshot the seeded database with the app stopped, then start the app on it: the copy is now in the state reset.sh restores
t1=$SECONDS
domain stop
snapshot_take || die "snapshot failed"
domain start
wait_ui 300 || die "app did not come back after the snapshot"
t_snap=$((SECONDS - t1))

mail=$(compose port mail 8025 2>/dev/null || true)
log "UP in $((SECONDS - t0)) s (start ${t_start} s, seed ${t_seed} s, snapshot ${t_snap} s): $BASE_URL/sormas-ui/ ; mail sink: http://${mail:-?}/"
