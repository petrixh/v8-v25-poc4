# Shared settings and helpers for up.sh, reset.sh, down.sh and build.sh. Sourced, not run. See env/README.md.
# shellcheck shell=bash

ENV_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
APP_ROOT=$(cd "$ENV_DIR/.." && pwd)

# One app copy = one compose project: app (Payara), db (PostgreSQL), mail (SMTP sink). Everything is named from the project.
export APP_ENV_PORT=${APP_ENV_PORT:-18080}
export APP_ENV_PROJECT=${APP_ENV_PROJECT:-sormas-new-env}
export APP_ENV_CONTAINER=${APP_ENV_CONTAINER:-$APP_ENV_PROJECT-app}
# Mail sink web UI and API on 127.0.0.1; empty = a free port chosen by Docker (up.sh prints it)
export APP_ENV_MAIL_PORT=${APP_ENV_MAIL_PORT:-}
# Vaadin mode of the app: prod (default; production build, as CI and releases) or dev (Flow development mode, for the
# dev-mode check: sormas-ui built with -Pdev, the build clone mounted into the app container; env/README.md "Development mode")
export APP_ENV_MODE=${APP_ENV_MODE:-prod}
[[ "$APP_ENV_MODE" =~ ^(prod|dev)$ ]] || { echo "APP_ENV_MODE must be prod or dev: $APP_ENV_MODE" >&2; exit 2; }
# Generated files: build output shared by all copies, per-copy secrets. Git-ignored (env/.gitignore). A dev build has its
# own default state dir, so a production copy never deploys a dev war and a build for other copies never cleans the
# clone a running dev copy uses
if [ "$APP_ENV_MODE" = dev ]; then export APP_ENV_STATE_DIR=${APP_ENV_STATE_DIR:-$ENV_DIR/.state-dev}
else export APP_ENV_STATE_DIR=${APP_ENV_STATE_DIR:-$ENV_DIR/.state}; fi
APP_ENV_M2_REPO=${APP_ENV_M2_REPO:-$HOME/.m2/repository}
APP_ENV_WAIT_S=${APP_ENV_WAIT_S:-900}
# reset.sh: restart (default; DB from the snapshot and a fresh app JVM) or db (DB only, app keeps running)
APP_ENV_RESET_MODE=${APP_ENV_RESET_MODE:-restart}
# Test configuration (env/config/<C>.properties and <C>.sql): C1 default country and features, C2 Germany with optional features
export APP_ENV_CONFIG=${APP_ENV_CONFIG:-C1}
# on: the "Security Alert: default password" dialog after a default-user login (skipDefaultPasswordCheck=false)
APP_ENV_PWCHECK=${APP_ENV_PWCHECK:-off}
# up.sh: ui.url of the copy, the link in notification mails. The app only accepts https URLs for it (ConfigFacadeEjb
# validateConfigUrls), so the link names this copy's UI with https although the copy serves http
export APP_ENV_UI_URL=${APP_ENV_UI_URL:-https://localhost:$APP_ENV_PORT/sormas-ui/}
# up.sh data: snapshot (the committed env/seed/snapshot-<C>.pgdump) or rest (the small REST seed only, as env/seed/generate.sh needs)
APP_ENV_SEED=${APP_ENV_SEED:-snapshot}
# Login: sormas (default, the app's own login) or keycloak (opt-in: a local Keycloak next to the app, env/compose.keycloak.yml,
# Keycloak on 127.0.0.1:$APP_ENV_KC_PORT). Matters to up.sh only: reset.sh and down.sh read it from the copy's state dir.
if [ -z "${APP_ENV_AUTH:-}" ] && [ -s "$APP_ENV_STATE_DIR/copies/$APP_ENV_PROJECT/auth" ] && docker container inspect "${APP_ENV_CONTAINER}" >/dev/null 2>&1; then
  APP_ENV_AUTH=$(cat "$APP_ENV_STATE_DIR/copies/$APP_ENV_PROJECT/auth")   # a running copy keeps the login up.sh gave it
fi
export APP_ENV_AUTH=${APP_ENV_AUTH:-sormas}
export APP_ENV_KC_PORT=${APP_ENV_KC_PORT:-$((APP_ENV_PORT + 100))}

[[ "$APP_ENV_PROJECT" =~ ^[a-z0-9][a-z0-9_.-]*$ ]] || { echo "APP_ENV_PROJECT must match ^[a-z0-9][a-z0-9_.-]*\$: $APP_ENV_PROJECT" >&2; exit 2; }
[[ "$APP_ENV_PORT" =~ ^[0-9]+$ ]] || { echo "APP_ENV_PORT must be a number: $APP_ENV_PORT" >&2; exit 2; }
[[ "$APP_ENV_CONFIG" =~ ^C[12]$ ]] || { echo "APP_ENV_CONFIG must be C1 or C2: $APP_ENV_CONFIG" >&2; exit 2; }
[[ "$APP_ENV_PWCHECK" =~ ^(on|off)$ ]] || { echo "APP_ENV_PWCHECK must be on or off: $APP_ENV_PWCHECK" >&2; exit 2; }
[[ "$APP_ENV_SEED" =~ ^(snapshot|rest)$ ]] || { echo "APP_ENV_SEED must be snapshot or rest: $APP_ENV_SEED" >&2; exit 2; }
[[ "$APP_ENV_AUTH" =~ ^(sormas|keycloak)$ ]] || { echo "APP_ENV_AUTH must be sormas or keycloak: $APP_ENV_AUTH" >&2; exit 2; }
[[ "$APP_ENV_KC_PORT" =~ ^[0-9]+$ ]] || { echo "APP_ENV_KC_PORT must be a number: $APP_ENV_KC_PORT" >&2; exit 2; }

PAYARA_VERSION=7.2026.10
PAYARA_ZIP_URL=https://repo1.maven.org/maven2/fish/payara/distributions/payara/$PAYARA_VERSION/payara-$PAYARA_VERSION.zip
export APP_ENV_IMAGE=sormas-new-env-payara:$PAYARA_VERSION
MVN_IMAGE=maven:3.9-eclipse-temurin-21

BUILD_DIR=$APP_ENV_STATE_DIR/build
export APP_ENV_DEPLOY_DIR=$BUILD_DIR/deploy
# Clean clone of HEAD that build.sh builds; APP_ENV_MODE=dev mounts it into the app container at the same path
export APP_ENV_SRC_DIR=$BUILD_DIR/src
COPY_DIR=$APP_ENV_STATE_DIR/copies/$APP_ENV_PROJECT
export APP_ENV_COPY_DIR=$COPY_DIR
# 1: build and deploy the app (default). 0: the Payara domain, database and mail sink only, no app build and no deploy
# (while the app does not build or start, e.g. during migration stage 1a). A running copy keeps the mode up.sh started
# it with (in its state dir), so reset.sh and down.sh need not repeat it. Only while its app container exists: a mode
# file left by an interrupted up.sh does not carry over to the next up.sh.
if [ -z "${APP_ENV_DEPLOY:-}" ] && [ -s "$COPY_DIR/deploy" ] && docker container inspect "$APP_ENV_CONTAINER" >/dev/null 2>&1; then
  APP_ENV_DEPLOY=$(cat "$COPY_DIR/deploy")
fi
export APP_ENV_DEPLOY=${APP_ENV_DEPLOY:-1}
[[ "$APP_ENV_DEPLOY" =~ ^[01]$ ]] || { echo "APP_ENV_DEPLOY must be 1 or 0: $APP_ENV_DEPLOY" >&2; exit 2; }
BASE_URL=http://localhost:$APP_ENV_PORT
# What answers 200 once the copy is up: the app's login page, or with APP_ENV_DEPLOY=0 the domain's default page
if [ "$APP_ENV_DEPLOY" = 1 ]; then READY_URL=$BASE_URL/sormas-ui/login; else READY_URL=$BASE_URL/; fi
DB_SVC=db
# Committed seed snapshot of the configuration (pg_dump custom format) and its metadata (env/seed/generate.sh writes both)
SNAPSHOT_DUMP=$ENV_DIR/seed/snapshot-$APP_ENV_CONFIG.pgdump
SNAPSHOT_META=$ENV_DIR/seed/snapshot-$APP_ENV_CONFIG.json

log() { echo "[$(date -u +%H:%M:%S)] $APP_ENV_PROJECT: $*" >&2; }
die() { log "ERROR: $*"; exit 1; }

# APP_ENV_MODE=dev adds env/compose.dev.yml (project folder mount, dev-tools host rule, Vaadin key by name),
# APP_ENV_AUTH=keycloak env/compose.keycloak.yml (Keycloak; the app in its network)
compose() {
  local files=(-f "$ENV_DIR/compose.yml")
  [ "$APP_ENV_MODE" = prod ] || files+=(-f "$ENV_DIR/compose.dev.yml")
  [ "$APP_ENV_AUTH" = sormas ] || files+=(-f "$ENV_DIR/compose.keycloak.yml")
  docker compose -p "$APP_ENV_PROJECT" "${files[@]}" "$@"
}

# psql as the postgres superuser inside this copy's db container, on the maintenance database
db_psql() { compose exec -T -e PGOPTIONS=--client-min-messages=warning "$DB_SVC" psql -v ON_ERROR_STOP=1 -qAt -U postgres -d postgres "$@"; }

http_code() { curl -s -o /dev/null -m 10 -w '%{http_code}' "$1" 2>/dev/null || true; }

# wait_ui <seconds>: until GET $READY_URL (/sormas-ui/login, or / with APP_ENV_DEPLOY=0) answers 200; fails early
# when the app container stops
wait_ui() {
  local deadline=$((SECONDS + $1))
  while [ $SECONDS -lt $deadline ]; do
    [ "$(http_code "$READY_URL")" = 200 ] && return 0
    [ "$(docker inspect -f '{{.State.Running}}' "$APP_ENV_CONTAINER" 2>/dev/null)" = true ] || { log "app container is not running"; return 1; }
    sleep 3
  done
  log "no 200 from $READY_URL after $1 s"; return 1
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

# Row counts of the seed data, "name=count" per line, for the snapshot metadata (env/seed/generate.sh, extend.sh)
snapshot_counts() {
  db_psql -d sormas_db -F= -c "SELECT 'person', count(*) FROM person UNION ALL SELECT 'cases', count(*) FROM cases
  UNION ALL SELECT 'contact', count(*) FROM contact UNION ALL SELECT 'events', count(*) FROM events
  UNION ALL SELECT 'eventparticipant', count(*) FROM eventparticipant UNION ALL SELECT 'samples', count(*) FROM samples
  UNION ALL SELECT 'pathogentest', count(*) FROM pathogentest UNION ALL SELECT 'task', count(*) FROM task
  UNION ALL SELECT 'visit', count(*) FROM visit UNION ALL SELECT 'immunization', count(*) FROM immunization
  UNION ALL SELECT 'travelentry', count(*) FROM travelentry
  UNION ALL SELECT 'casesInvestigationPending', count(*) FROM cases WHERE investigationstatus='PENDING' AND NOT deleted
  UNION ALL SELECT 'samplesAtLabResultPending', count(*) FROM samples s JOIN facility f ON f.id=s.lab_id
    WHERE f.type='LABORATORY' AND s.pathogentestresult='PENDING' AND NOT s.deleted
  UNION ALL SELECT 'eventgroups', count(*) FROM eventgroups UNION ALL SELECT 'environments', count(*) FROM environments
  UNION ALL SELECT 'environmentsamples', count(*) FROM environmentsamples UNION ALL SELECT 'campaigns', count(*) FROM campaigns
  UNION ALL SELECT 'surveys', count(*) FROM surveys UNION ALL SELECT 'surveytokens', count(*) FROM surveytokens
  UNION ALL SELECT 'selfreports', count(*) FROM selfreports
  UNION ALL SELECT 'adverseevents', count(*) FROM adverseeventsfollowingimmunization
  UNION ALL SELECT 'adverseeventinvestigations', count(*) FROM adverseeventsfollowingimmunizationinvestigation
  UNION ALL SELECT 'continents', count(*) FROM continent UNION ALL SELECT 'subcontinents', count(*) FROM subcontinent
  UNION ALL SELECT 'countries', count(*) FROM country
  UNION ALL SELECT 'campaignforms', count(*) FROM campaignformmeta UNION ALL SELECT 'campaignformdata', count(*) FROM campaignformdata
  UNION ALL SELECT 'areas', count(*) FROM areas UNION ALL SELECT 'userroles', count(*) FROM userroles
  UNION ALL SELECT 'users', count(*) FROM users
  UNION ALL SELECT 'tasksFor' || u.username, count(*) FROM task t JOIN users u ON u.id=t.assigneeuser_id GROUP BY u.username"
}
