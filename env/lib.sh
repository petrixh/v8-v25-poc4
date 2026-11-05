# Shared settings and helpers for up.sh, reset.sh, down.sh and build.sh. Sourced, not run. See env/README.md.
# shellcheck shell=bash

ENV_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
APP_ROOT=$(cd "$ENV_DIR/.." && pwd)

# One app copy = one compose project: app (Payara), db (PostgreSQL), mail (SMTP sink). Everything is named from the project.
export APP_ENV_PORT=${APP_ENV_PORT:-18080}
export APP_ENV_PROJECT=${APP_ENV_PROJECT:-sormas-old-env}
export APP_ENV_CONTAINER=${APP_ENV_CONTAINER:-$APP_ENV_PROJECT-app}
# Mail sink web UI and API on 127.0.0.1; empty = a free port chosen by Docker (up.sh prints it)
export APP_ENV_MAIL_PORT=${APP_ENV_MAIL_PORT:-}
# Generated files: build output shared by all copies, per-copy secrets. Git-ignored (env/.gitignore).
export APP_ENV_STATE_DIR=${APP_ENV_STATE_DIR:-$ENV_DIR/.state}
APP_ENV_M2_REPO=${APP_ENV_M2_REPO:-$HOME/.m2/repository}
APP_ENV_WAIT_S=${APP_ENV_WAIT_S:-900}
# reset.sh: restart (default; DB from the snapshot and a fresh app JVM) or db (DB only, app keeps running)
APP_ENV_RESET_MODE=${APP_ENV_RESET_MODE:-restart}

[[ "$APP_ENV_PROJECT" =~ ^[a-z0-9][a-z0-9_.-]*$ ]] || { echo "APP_ENV_PROJECT must match ^[a-z0-9][a-z0-9_.-]*\$: $APP_ENV_PROJECT" >&2; exit 2; }
[[ "$APP_ENV_PORT" =~ ^[0-9]+$ ]] || { echo "APP_ENV_PORT must be a number: $APP_ENV_PORT" >&2; exit 2; }

PAYARA_VERSION=5.2022.5
PAYARA_ZIP_URL=https://repo1.maven.org/maven2/fish/payara/distributions/payara/$PAYARA_VERSION/payara-$PAYARA_VERSION.zip
export APP_ENV_IMAGE=sormas-old-env-payara:$PAYARA_VERSION
MVN_IMAGE=maven:3.9-eclipse-temurin-11

BUILD_DIR=$APP_ENV_STATE_DIR/build
export APP_ENV_DEPLOY_DIR=$BUILD_DIR/deploy
COPY_DIR=$APP_ENV_STATE_DIR/copies/$APP_ENV_PROJECT
export APP_ENV_COPY_DIR=$COPY_DIR
BASE_URL=http://localhost:$APP_ENV_PORT
DB_SVC=db

log() { echo "[$(date -u +%H:%M:%S)] $APP_ENV_PROJECT: $*" >&2; }
die() { log "ERROR: $*"; exit 1; }

compose() { docker compose -p "$APP_ENV_PROJECT" -f "$ENV_DIR/compose.yml" "$@"; }

# psql as the postgres superuser inside this copy's db container, on the maintenance database
db_psql() { compose exec -T -e PGOPTIONS=--client-min-messages=warning "$DB_SVC" psql -v ON_ERROR_STOP=1 -qAt -U postgres -d postgres "$@"; }

http_code() { curl -s -o /dev/null -m 10 -w '%{http_code}' "$1" 2>/dev/null || true; }

# wait_ui <seconds>: until GET /sormas-ui/login answers 200; fails early when the app container stops
wait_ui() {
  local deadline=$((SECONDS + $1))
  while [ $SECONDS -lt $deadline ]; do
    [ "$(http_code "$BASE_URL/sormas-ui/login")" = 200 ] && return 0
    [ "$(docker inspect -f '{{.State.Running}}' "$APP_ENV_CONTAINER" 2>/dev/null)" = true ] || { log "app container is not running"; return 1; }
    sleep 3
  done
  log "no 200 from $BASE_URL/sormas-ui/login after $1 s"; return 1
}

# Payara domain inside the running app container (deployed apps stay deployed across a domain restart)
domain() { docker exec "$APP_ENV_CONTAINER" asadmin "$1"-domain sormas >/dev/null; }

# Seed snapshot: the template database sormas_seed, taken after seeding, inside this copy's db container
snapshot_exists() { [ "$(db_psql -c "SELECT 1 FROM pg_database WHERE datname='sormas_seed'")" = 1 ]; }
snapshot_take() {
  db_psql -c "ALTER DATABASE sormas_db ALLOW_CONNECTIONS false" -c "SELECT count(pg_terminate_backend(pid)) FROM pg_stat_activity WHERE datname='sormas_db'" >/dev/null &&
  db_psql -c "DROP DATABASE IF EXISTS sormas_seed" -c "CREATE DATABASE sormas_seed TEMPLATE sormas_db OWNER sormas_user" &&
  db_psql -c "ALTER DATABASE sormas_seed ALLOW_CONNECTIONS false" -c "ALTER DATABASE sormas_db ALLOW_CONNECTIONS true"
}
snapshot_restore() {
  db_psql -c "DROP DATABASE IF EXISTS sormas_db WITH (FORCE)" &&
  db_psql -c "CREATE DATABASE sormas_db TEMPLATE sormas_seed OWNER sormas_user"
}
