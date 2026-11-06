#!/bin/bash
# Add the seed additions to the committed snapshot of one test configuration and write it again:
# env/seed/snapshot-<C>.pgdump and snapshot-<C>.json (env/README.md "Seed additions").
# Usage: APP_ENV_CONFIG=C1|C2 [APP_ENV_PORT=... APP_ENV_PROJECT=...] env/seed/extend.sh
#   Needs a copy of that configuration started by env/up.sh from the committed snapshot (APP_ENV_SEED=snapshot) and not
#   changed since (run env/reset.sh first if it was used). Then:
#   1. extend.py: the REST additions (event group; C2: environment and environment sample, person and immunization with
#      a vaccination), as default users through the app's REST API
#   2. extend.cjs: the UI additions (default continents, subcontinents, countries; default point of entry active;
#      C2: campaign, survey with a token, self report, adverse event report with an investigation)
#   3. stops the app, pg_dump of sormas_db, updates the metadata (snapshot date and the dates of the data unchanged)
# The copy stays up in the new snapshot's state (reset.sh returns to it). Commit the two files.
set -euo pipefail
ENV_DIR=$(cd "$(dirname "$0")/.." && pwd)
source "$ENV_DIR/lib.sh"
t0=$SECONDS
docker container inspect "$APP_ENV_CONTAINER" >/dev/null 2>&1 || die "copy is not running (no container $APP_ENV_CONTAINER): start it with env/up.sh"
[ -s "$COPY_DIR/seed-date" ] || die "no $COPY_DIR/seed-date: start the copy with env/up.sh"
SNAPSHOT_DATE=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["snapshotDate"])' "$SNAPSHOT_META")
[ "$(cat "$COPY_DIR/seed-date")" = "$SNAPSHOT_DATE" ] || die "the copy does not hold the committed snapshot of $APP_ENV_CONFIG (dated $SNAPSHOT_DATE)"
[ "$(db_psql -d sormas_db -c "SELECT count(*) FROM eventgroups")" = 0 ] || die "the copy already holds event groups: the additions are made once, on the committed snapshot"

# Names of the default infrastructure follow country.locale, so read them from the data (as generate.sh does)
q() { db_psql -d sormas_db -c "$1"; }
settings=$COPY_DIR/extend-settings.json
python3 - "$settings" "$APP_ENV_CONFIG" "$SNAPSHOT_DATE" \
  "$(q "SELECT name FROM pointofentry WHERE uuid NOT LIKE 'SORMAS-CONST%'")" \
  "$(q "SELECT name FROM region WHERE uuid='00000000-0000-0000-0000-000000000003'")" \
  "$(q "SELECT name FROM district WHERE uuid='00000000-0000-0000-0000-000000000004'")" \
  "$(q "SELECT name FROM community WHERE uuid='00000000-0000-0000-0000-000000000005'")" <<'EOF'
import json, sys
out, config, date, poe, region, district, community = sys.argv[1:8]
assert poe and "\n" not in poe and region and district and community, "default point of entry, region, district or community not found"
json.dump({"config": config, "snapshotDate": date, "pointOfEntry": poe, "region": region, "district": district,
           "community": community}, open(out, "w"), ensure_ascii=False)
EOF

log "REST additions"
python3 "$ENV_DIR/seed/extend.py" "$BASE_URL" "$APP_ROOT/sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java" \
  "$APP_ENV_CONFIG" "$SNAPSHOT_DATE" || die "REST additions failed"
log "UI additions"
node "$ENV_DIR/seed/extend.cjs" "$BASE_URL/sormas-ui" "$settings" "$COPY_DIR/extend" || die "UI additions failed (screenshot in $COPY_DIR/extend)"

domain stop
counts=$(snapshot_counts)
c=$APP_ENV_PROJECT-db
docker exec "$c" pg_dump -U postgres -Fc -Z 9 --no-owner --no-acl -f /tmp/snapshot.pgdump sormas_db || die "pg_dump failed"
docker cp "$c:/tmp/snapshot.pgdump" "$SNAPSHOT_DUMP.part" >/dev/null && mv "$SNAPSHOT_DUMP.part" "$SNAPSHOT_DUMP"
docker exec "$c" rm -f /tmp/snapshot.pgdump
pgv=$(docker exec "$c" pg_dump --version)
python3 - "$SNAPSHOT_META" "$SNAPSHOT_DUMP" <<EOF
import hashlib, json, os, sys
meta_file, dump = sys.argv[1:3]
meta = json.load(open(meta_file))
meta.update({
    "extendedAt": "$(date -u +%FT%TZ)",
    "extendNote": "Seed additions (env/seed/extend.sh) made on the snapshot of createdAt: dates relative to snapshotDate as the REST seed's; creation and change dates of the added records are those of extendedAt.",
    "extendAppCommit": open("$APP_ENV_DEPLOY_DIR/commit").read().strip(),
    "pgDump": "$pgv",
    "bytes": os.path.getsize(dump),
    "sha256": hashlib.sha256(open(dump, "rb").read()).hexdigest(),
    "seedAdditions": "env/seed/extend.py, env/seed/extend.cjs",
    "rowCounts": {k: int(v) for k, v in (l.split("=") for l in """$counts""".split())},
})
json.dump(meta, open(meta_file, "w"), indent=2)
open(meta_file, "a").write("\n")
EOF
snapshot_take || die "snapshot failed"
domain start
wait_ui 300 || die "app did not come back after the dump"
log "EXTENDED $APP_ENV_CONFIG in $((SECONDS - t0)) s: $SNAPSHOT_DUMP ($(du -h "$SNAPSHOT_DUMP" | cut -f1)), $(echo $counts | tr ' ' ',')"
