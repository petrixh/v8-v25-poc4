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
# Test configuration (env/config/<C>.properties and <C>.sql): C1 default country and features, C2 Germany with optional features
export APP_ENV_CONFIG=${APP_ENV_CONFIG:-C1}
# on: the "Security Alert: default password" dialog after a default-user login (skipDefaultPasswordCheck=false)
APP_ENV_PWCHECK=${APP_ENV_PWCHECK:-off}
# up.sh data: snapshot (the committed env/seed/snapshot-<C>.pgdump) or rest (the small REST seed only, as env/seed/generate.sh needs)
APP_ENV_SEED=${APP_ENV_SEED:-snapshot}

[[ "$APP_ENV_PROJECT" =~ ^[a-z0-9][a-z0-9_.-]*$ ]] || { echo "APP_ENV_PROJECT must match ^[a-z0-9][a-z0-9_.-]*\$: $APP_ENV_PROJECT" >&2; exit 2; }
[[ "$APP_ENV_PORT" =~ ^[0-9]+$ ]] || { echo "APP_ENV_PORT must be a number: $APP_ENV_PORT" >&2; exit 2; }
[[ "$APP_ENV_CONFIG" =~ ^C[12]$ ]] || { echo "APP_ENV_CONFIG must be C1 or C2: $APP_ENV_CONFIG" >&2; exit 2; }
[[ "$APP_ENV_PWCHECK" =~ ^(on|off)$ ]] || { echo "APP_ENV_PWCHECK must be on or off: $APP_ENV_PWCHECK" >&2; exit 2; }
[[ "$APP_ENV_SEED" =~ ^(snapshot|rest)$ ]] || { echo "APP_ENV_SEED must be snapshot or rest: $APP_ENV_SEED" >&2; exit 2; }

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
# Committed seed snapshot of the configuration (pg_dump custom format) and its metadata (env/seed/generate.sh writes both)
SNAPSHOT_DUMP=$ENV_DIR/seed/snapshot-$APP_ENV_CONFIG.pgdump
SNAPSHOT_META=$ENV_DIR/seed/snapshot-$APP_ENV_CONFIG.json

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

# The configuration's feature switches (env/config/<C>.sql) on sormas_db, with the app's domain stopped: the app reads the
# featureconfiguration table at start (docs/SERVER_CUSTOMIZATION.md: the table is the supported place to switch features)
config_sql() {
  local f=$ENV_DIR/config/$APP_ENV_CONFIG.sql
  [ -s "$f" ] || return 0
  db_psql -d sormas_db < "$f"
}

# Load the committed snapshot into the empty sormas_db (before the app first starts on it). Objects are created as
# sormas_user; the extensions and the public schema are made by env/db/db-init.sh, so their entries are skipped.
snapshot_load() {
  local c=$APP_ENV_PROJECT-db
  docker cp "$SNAPSHOT_DUMP" "$c:/tmp/snapshot.pgdump" >/dev/null &&
  docker exec "$c" sh -c 'pg_restore -l /tmp/snapshot.pgdump | grep -v -e " EXTENSION " -e " SCHEMA - public " > /tmp/snapshot.list &&
    pg_restore -U postgres -d sormas_db --no-owner --no-acl --role=sormas_user --exit-on-error --single-transaction -L /tmp/snapshot.list /tmp/snapshot.pgdump &&
    rm -f /tmp/snapshot.pgdump /tmp/snapshot.list'
}
