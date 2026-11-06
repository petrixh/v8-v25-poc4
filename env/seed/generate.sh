#!/bin/bash
# Make the committed seed snapshot of one test configuration: env/seed/snapshot-<C>.pgdump and snapshot-<C>.json.
# Usage: APP_ENV_CONFIG=C1|C2 [APP_ENV_PORT=... APP_ENV_PROJECT=...] env/seed/generate.sh [--no-build]
#   1. env/up.sh with APP_ENV_SEED=rest: a fresh database, the small REST seed (seed.py), the configuration's features
#   2. SORMAS's dev-mode generator through the UI (devmode.cjs), with the fixed seed and counts below, run as SurvSup:
#      the generator picks source cases for contacts and samples within the user's jurisdiction, and admin (the only
#      default user with the DEV_MODE right) has none, so SurvSup's role gets DEV_MODE for the run and loses it again
#   3. waits for the case completeness job, stops the app, pg_dump of sormas_db, writes the metadata
# The copy stays up afterwards in the state of the new snapshot (reset.sh returns to it); env/down.sh removes it.
# Data dates are relative to the day this runs (the snapshot date in the metadata). Commit the two files.
set -euo pipefail
ENV_DIR=$(cd "$(dirname "$0")/.." && pwd)
export APP_ENV_SEED=rest
source "$ENV_DIR/lib.sh"

# Generator settings. Change them only together with a new snapshot (they are recorded in the metadata).
SEED=sormasenv # base 36, the generator's "seed" field
DISEASE=COVID-19 # region, district, laboratory: the default ones (their names follow country.locale, so read below)
CASES=80 CONTACTS=60 EVENTS=12 SAMPLES=40
GEN_USER=SurvSup

t0=$SECONDS
"$ENV_DIR/up.sh" "$@"

REGION=$(db_psql -d sormas_db -c "SELECT name FROM region WHERE uuid='00000000-0000-0000-0000-000000000003'")
DISTRICT=$(db_psql -d sormas_db -c "SELECT name FROM district WHERE uuid='00000000-0000-0000-0000-000000000004'")
LAB=$(db_psql -d sormas_db -c "SELECT name FROM facility WHERE type='LABORATORY' AND uuid NOT LIKE 'SORMAS-CONST%' AND region_id=(SELECT id FROM region WHERE uuid='00000000-0000-0000-0000-000000000003')")
[ -n "$REGION" ] && [ -n "$DISTRICT" ] && [ -n "$LAB" ] || die "default region, district or laboratory not found"
settings=$COPY_DIR/devmode-settings.json
python3 - "$settings" <<EOF
import json, sys
json.dump({"user": "$GEN_USER", "seed": "$SEED", "disease": "$DISEASE", "region": "$REGION", "district": "$DISTRICT",
           "laboratory": "$LAB", "cases": $CASES, "contacts": $CONTACTS, "events": $EVENTS, "samples": $SAMPLES},
          open(sys.argv[1], "w"), ensure_ascii=False)
EOF
# DEV_MODE for the generator user's role, for this run only (the app reads role rights at start)
# (the user's one role; role captions follow country.locale, so it is found through the user)
ROLE_ID="(SELECT ur.userrole_id FROM users_userroles ur JOIN users u ON u.id = ur.user_id WHERE u.username = '$GEN_USER')"
role_right() { # role_right grant|revoke
  local sql="INSERT INTO userroles_userrights (userrole_id, userright, sys_period) SELECT $ROLE_ID, 'DEV_MODE', tstzrange(now(), NULL)"
  [ "$1" = grant ] || sql="DELETE FROM userroles_userrights WHERE userright = 'DEV_MODE' AND userrole_id = $ROLE_ID"
  have() { db_psql -d sormas_db -c "SELECT count(*) FROM userroles_userrights WHERE userright = 'DEV_MODE' AND userrole_id = $ROLE_ID"; }
  [ "$(have)" = "$([ "$1" = grant ] && echo 0 || echo 1)" ] || die "role of $GEN_USER: unexpected DEV_MODE right before $1"
  db_psql -d sormas_db -c "$sql" >/dev/null
  [ "$(have)" = "$([ "$1" = grant ] && echo 1 || echo 0)" ] || die "role of $GEN_USER: DEV_MODE $1 failed"
}
domain stop
role_right grant
domain start
wait_ui 300 || die "app did not come back"
t1=$SECONDS
log "dev-mode generator as $GEN_USER: $CASES cases, $CONTACTS contacts, $EVENTS events, $SAMPLES samples ($DISEASE, $DISTRICT, $LAB), seed $SEED"
node "$ENV_DIR/seed/devmode.cjs" "$BASE_URL/sormas-ui" "$settings" "$COPY_DIR/devmode" || die "dev-mode generator failed (screenshot in $COPY_DIR/devmode)"
t_gen=$((SECONDS - t1))

log "waiting for the app's case completeness job (every 2 min) before the dump"
until [ "$(db_psql -d sormas_db -c "SELECT count(*) FROM cases WHERE completeness IS NULL")" = 0 ]; do
  [ $((SECONDS - t1)) -lt 600 ] || die "case completeness not computed after 600 s"
  sleep 5
done

domain stop
role_right revoke
counts=$(snapshot_counts)
c=$APP_ENV_PROJECT-db
docker exec "$c" pg_dump -U postgres -Fc -Z 9 --no-owner --no-acl -f /tmp/snapshot.pgdump sormas_db || die "pg_dump failed"
docker cp "$c:/tmp/snapshot.pgdump" "$SNAPSHOT_DUMP.part" >/dev/null && mv "$SNAPSHOT_DUMP.part" "$SNAPSHOT_DUMP"
docker exec "$c" rm -f /tmp/snapshot.pgdump
pgv=$(docker exec "$c" pg_dump --version)
date -u +%F > "$COPY_DIR/seed-date"
python3 - "$SNAPSHOT_META" "$SNAPSHOT_DUMP" "$settings" <<EOF
import hashlib, json, os, sys
meta, dump, settings = sys.argv[1:4]
counts = {k: int(v) for k, v in (l.split("=") for l in """$counts""".split())}
json.dump({
    "config": "$APP_ENV_CONFIG",
    "snapshotDate": "$(cat "$COPY_DIR/seed-date")",
    "createdAt": "$(date -u +%FT%TZ)",
    "dateNote": "Data dates are relative to snapshotDate (REST seed: whole days at 10:00 UTC; dev-mode generator: the 90 days up to snapshotDate). They do not move on reset.",
    "appCommit": open("$APP_ENV_DEPLOY_DIR/commit").read().strip(),
    "pgDump": "$pgv",
    "file": os.path.basename(dump),
    "bytes": os.path.getsize(dump),
    "sha256": hashlib.sha256(open(dump, "rb").read()).hexdigest(),
    "restSeed": "env/seed/seed.py",
    "devModeGenerator": dict(json.load(open(settings)), dateRange="the generator's default: 90 days before snapshotDate to snapshotDate"),
    "rowCounts": counts,
}, open(meta, "w"), indent=2)
open(meta, "a").write("\n")
EOF
# The copy now holds the new snapshot's data: make it the state reset.sh restores
snapshot_take || die "snapshot failed"
domain start
wait_ui 300 || die "app did not come back after the dump"
log "SNAPSHOT $APP_ENV_CONFIG in $((SECONDS - t0)) s (generator ${t_gen} s): $SNAPSHOT_DUMP ($(du -h "$SNAPSHOT_DUMP" | cut -f1)), $(echo $counts | tr ' ' ',')"
