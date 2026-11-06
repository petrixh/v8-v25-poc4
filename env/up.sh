#!/bin/bash
# Start one app copy (PostgreSQL 14, Payara 5.2022.5 with SORMAS, SMTP sink) in the seed state, on 127.0.0.1:$APP_ENV_PORT.
# Usage: env/up.sh [--no-build]     --no-build: reuse the image and the built artefacts when they exist (else build)
# APP_ENV_CONFIG=C1|C2 selects the test configuration, APP_ENV_PWCHECK=on the default-password dialog, APP_ENV_SEED=rest
# the small REST seed instead of the committed snapshot. Returns 0 once GET /sormas-ui/login answers 200 on the seeded
# copy. See env/README.md.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
NO_BUILD=0
case "${1:-}" in "") ;; --no-build) NO_BUILD=1 ;; *) echo "usage: $0 [--no-build]" >&2; exit 2 ;; esac
t0=$SECONDS

for c in "$APP_ENV_CONTAINER" "$APP_ENV_PROJECT-db" "$APP_ENV_PROJECT-mail"; do
  ! docker container inspect "$c" >/dev/null 2>&1 || die "container $c already exists: run env/down.sh with the same APP_ENV_* first"
done
[ "$APP_ENV_SEED" = rest ] || [ -s "$SNAPSHOT_DUMP" ] || die "no snapshot $SNAPSHOT_DUMP: make it with env/seed/generate.sh, or use APP_ENV_SEED=rest"

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
# This copy's sormas.properties (mounted over the shared one, compose.yml): stock file, env overlay, configuration overlay,
# this copy's UI URL, and the default-password check when asked for. Later lines win.
{
  cat "$APP_ENV_DEPLOY_DIR/setup/sormas.properties" "$ENV_DIR/sormas.properties.env" "$ENV_DIR/config/$APP_ENV_CONFIG.properties"
  # Link target of notification mails (task mails): this copy's UI. The app accepts only https here (validateConfigUrls)
  printf '%s\n' '# --- env/up.sh: this copy ---' "ui.url=$APP_ENV_UI_URL"
  if [ "$APP_ENV_PWCHECK" = on ]; then
    printf '%s\n' '# --- env/up.sh: APP_ENV_PWCHECK=on ---' 'skipDefaultPasswordCheck=false'
  fi
} > "$COPY_DIR/sormas.properties"

t1=$SECONDS
log "configuration $APP_ENV_CONFIG (default-password check $APP_ENV_PWCHECK, data: $APP_ENV_SEED)"
log "starting db, mail and app (app container $APP_ENV_CONTAINER, port $APP_ENV_PORT)"
if [ "$APP_ENV_SEED" = snapshot ]; then
  # The database first, loaded from the committed snapshot, so that the app's first start already finds the data
  compose up -d --wait --quiet-pull db mail >/dev/null 2>"$COPY_DIR/compose-up.log" || { cat "$COPY_DIR/compose-up.log" >&2; die "docker compose up (db, mail) failed"; }
  snapshot_load || die "loading $SNAPSHOT_DUMP failed"
  log "loaded $(basename "$SNAPSHOT_DUMP") in $((SECONDS - t1)) s"
fi
compose up -d --quiet-pull >/dev/null 2>"$COPY_DIR/compose-up.log" || { cat "$COPY_DIR/compose-up.log" >&2; die "docker compose up failed"; }
wait_ui "$APP_ENV_WAIT_S" || { docker logs --tail 50 "$APP_ENV_CONTAINER" >&2 2>&1 || true; die "app did not come up"; }
docker logs "$APP_ENV_CONTAINER" 2>&1 | grep -q "DEPLOY FAILED" && die "a deployment failed (docker logs $APP_ENV_CONTAINER)"
t_start=$((SECONDS - t1))

t1=$SECONDS
if [ "$APP_ENV_SEED" = rest ]; then
  log "app answers after ${t_start} s; seeding"
  python3 "$ENV_DIR/seed/seed.py" "$BASE_URL" "$APP_ROOT/sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java" ||
    die "seeding failed"
  date -u +%F > "$COPY_DIR/seed-date"
else
  log "app answers after ${t_start} s"
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["snapshotDate"])' "$SNAPSHOT_META" > "$COPY_DIR/seed-date"
fi
# The app's cron job computes case completeness every 2 minutes and writes each case once. Wait for it, so the
# snapshot already holds that write and nothing changes the seeded rows behind a test's back after a reset.
log "waiting for the app's case completeness job (every 2 min) before the snapshot"
until [ "$(db_psql -d sormas_db -c "SELECT count(*) FROM cases WHERE completeness IS NULL")" = 0 ]; do
  [ $((SECONDS - t1)) -lt 300 ] || die "case completeness not computed after 300 s"
  sleep 5
done
t_seed=$((SECONDS - t1))

# Stop the app, switch the configuration's features, snapshot, then start the app on it: the copy is now in the state
# reset.sh restores
t1=$SECONDS
domain stop
config_sql || die "feature switches of $APP_ENV_CONFIG failed (env/config/$APP_ENV_CONFIG.sql)"
snapshot_take || die "snapshot failed"
domain start
wait_ui 300 || die "app did not come back after the snapshot"
t_snap=$((SECONDS - t1))

mail=$(compose port mail 8025 2>/dev/null || true)
log "UP $APP_ENV_CONFIG in $((SECONDS - t0)) s (start ${t_start} s, seed ${t_seed} s, snapshot ${t_snap} s; data dated $(cat "$COPY_DIR/seed-date")): $BASE_URL/sormas-ui/ ; mail sink: http://${mail:-?}/"
