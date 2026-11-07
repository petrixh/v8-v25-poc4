#!/bin/bash
# Add the seed additions to the committed snapshot of one test configuration and write it again:
# env/seed/snapshot-<C>.pgdump and snapshot-<C>.json (env/README.md "Seed additions").
# Usage: APP_ENV_CONFIG=C1|C2 [APP_ENV_PORT=... APP_ENV_PROJECT=...] env/seed/extend.sh
#   Needs a copy of that configuration started by env/up.sh from the committed snapshot (APP_ENV_SEED=snapshot) and not
#   changed since (run env/reset.sh first if it was used). Makes the parts of the additions the copy lacks: part 1
#   (C1 and C2) when it has no event group, part 2 (C2 only) when it has no user CfgUser. Then:
#   per part, part 1 first:
#   1. part 2 only: env/seed/campaign-form.sql with the app stopped (a campaign form definition, linked to the campaign)
#   2. extend.py: the REST additions (part 1: event group; C2: environment and environment sample, person and
#      immunization with a vaccination; part 2: campaign form data), as default users through the app's REST API
#   3. extend.cjs: the UI additions (part 1: default continents, subcontinents, countries; default point of entry
#      active; C2: campaign, survey with a token, self report, adverse event report with an investigation; part 2: an
#      area holding the default region, a user role and the user CfgUser)
#   4. stops the app; part 2: gives CfgUser NatUser's password (see below); pg_dump of sormas_db, updates the metadata
#      (snapshot date and the dates of the data unchanged)
# The copy stays up in the new snapshot's state (reset.sh returns to it). Commit the two files.
set -euo pipefail
ENV_DIR=$(cd "$(dirname "$0")/.." && pwd)
source "$ENV_DIR/lib.sh"
t0=$SECONDS
docker container inspect "$APP_ENV_CONTAINER" >/dev/null 2>&1 || die "copy is not running (no container $APP_ENV_CONTAINER): start it with env/up.sh"
[ -s "$COPY_DIR/seed-date" ] || die "no $COPY_DIR/seed-date: start the copy with env/up.sh"
SNAPSHOT_DATE=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["snapshotDate"])' "$SNAPSHOT_META")
[ "$(cat "$COPY_DIR/seed-date")" = "$SNAPSHOT_DATE" ] || die "the copy does not hold the committed snapshot of $APP_ENV_CONFIG (dated $SNAPSHOT_DATE)"
q() { db_psql -d sormas_db -c "$1"; }
parts=
[ "$(q "SELECT count(*) FROM eventgroups")" != 0 ] || parts=1
[ "$APP_ENV_CONFIG" != C2 ] || [ "$(q "SELECT count(*) FROM users WHERE username='CfgUser'")" != 0 ] || parts=${parts}2
[ -n "$parts" ] || die "the copy already holds all seed additions of $APP_ENV_CONFIG: they are made once, on the committed snapshot"
log "seed additions: part(s) $parts"

# Names of the default infrastructure and of the default user roles follow country.locale, so read them from the data
# (as generate.sh does)
settings=$COPY_DIR/extend-settings.json
python3 - "$settings" "$APP_ENV_CONFIG" "$SNAPSHOT_DATE" \
  "$(q "SELECT name FROM pointofentry WHERE uuid NOT LIKE 'SORMAS-CONST%'")" \
  "$(q "SELECT name FROM region WHERE uuid='00000000-0000-0000-0000-000000000003'")" \
  "$(q "SELECT name FROM district WHERE uuid='00000000-0000-0000-0000-000000000004'")" \
  "$(q "SELECT name FROM community WHERE uuid='00000000-0000-0000-0000-000000000005'")" "$parts" \
  "$(q "SELECT r.caption FROM userroles r JOIN users_userroles x ON x.userrole_id = r.id JOIN users u ON u.id = x.user_id
        WHERE u.username = 'NatUser'")" <<'EOF'
import json, sys
out, config, date, poe, region, district, community, parts, role = sys.argv[1:10]
assert poe and "\n" not in poe and region and district and community, "default point of entry, region, district or community not found"
assert role and "\n" not in role, "NatUser does not have exactly one user role"
json.dump({"config": config, "snapshotDate": date, "pointOfEntry": poe, "region": region, "district": district,
           "community": community, "parts": parts, "templateRole": role}, open(out, "w"), ensure_ascii=False)
EOF

# Part 2 builds on part 1 (the campaign), so the parts run one after the other
for part in 1 2; do
  [[ $parts == *$part* ]] || continue
  python3 -c 'import json,sys; s=json.load(open(sys.argv[1])); s["parts"]=sys.argv[2]; json.dump(s, open(sys.argv[1], "w"), ensure_ascii=False)' "$settings" "$part"
  if [ $part = 2 ]; then
    # The app has no UI or REST endpoint that creates campaign form definitions: loaded into the table, app stopped
    log "part 2: campaign form definition"
    domain stop
    db_psql -d sormas_db < "$ENV_DIR/seed/campaign-form.sql" || die "campaign form definition failed (env/seed/campaign-form.sql)"
    domain start
    wait_ui 300 || die "app did not come back after the campaign form definition"
  fi
  log "part $part: REST additions"
  python3 "$ENV_DIR/seed/extend.py" "$BASE_URL" "$APP_ROOT/sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java" \
    "$APP_ENV_CONFIG" "$SNAPSHOT_DATE" "$part" || die "REST additions failed"
  log "part $part: UI additions"
  node "$ENV_DIR/seed/extend.cjs" "$BASE_URL/sormas-ui" "$settings" "$COPY_DIR/extend" || die "UI additions failed (screenshot in $COPY_DIR/extend)"
done

domain stop
if [[ $parts == *2* ]]; then
  # CfgUser logs in with NatUser's default password: its password hash and seed are copied from NatUser (PasswordHelper:
  # SHA-256 of password and seed). The app only generates passwords (shown once, not read by extend.cjs), so no
  # password is handled here. Then the role's rights are checked by name (extend.cjs ticks them by caption).
  db_psql -d sormas_db -c "UPDATE users u SET password = n.password, seed = n.seed FROM users n
    WHERE n.username = 'NatUser' AND u.username = 'CfgUser'" || die "setting CfgUser's password failed"
  rights=$(q "SELECT string_agg(x.userright, ' ' ORDER BY x.userright) FROM userroles r
    JOIN userroles_userrights x ON x.userrole_id = r.id JOIN users_userroles ur ON ur.userrole_id = r.id
    JOIN users u ON u.id = ur.user_id WHERE u.username = 'CfgUser' AND x.userright IN ('DISEASE_MANAGEMENT',
    'EMAIL_TEMPLATE_MANAGEMENT', 'EPIPULSE_EXPORT_VIEW', 'EPIPULSE_EXPORT_CREATE', 'EPIPULSE_EXPORT_DOWNLOAD', 'EPIPULSE_EXPORT_DELETE')")
  [ "$rights" = "DISEASE_MANAGEMENT EMAIL_TEMPLATE_MANAGEMENT EPIPULSE_EXPORT_CREATE EPIPULSE_EXPORT_DELETE EPIPULSE_EXPORT_DOWNLOAD EPIPULSE_EXPORT_VIEW" ] ||
    die "CfgUser's role lacks rights: has only $rights"
fi
counts=$(snapshot_counts)
c=$APP_ENV_PROJECT-db
docker exec "$c" pg_dump -U postgres -Fc -Z 9 --no-owner --no-acl -f /tmp/snapshot.pgdump sormas_db || die "pg_dump failed"
docker cp "$c:/tmp/snapshot.pgdump" "$SNAPSHOT_DUMP.part" >/dev/null && mv "$SNAPSHOT_DUMP.part" "$SNAPSHOT_DUMP"
docker exec "$c" rm -f /tmp/snapshot.pgdump
pgv=$(docker exec "$c" pg_dump --version)
python3 - "$SNAPSHOT_META" "$SNAPSHOT_DUMP" "$parts" <<EOF
import hashlib, json, os, sys
meta_file, dump, parts = sys.argv[1:4]
meta = json.load(open(meta_file))
now = "$(date -u +%FT%TZ)"
if "1" in parts:
    meta["extendedAt"] = now
if "2" in parts:
    meta["extendedPart2At"] = now
meta.update({
    "extendNote": "Seed additions (env/seed/extend.sh) made on the snapshot of createdAt: dates relative to snapshotDate as the REST seed's; creation and change dates of the added records are those of extendedAt (part 1) and extendedPart2At (part 2, C2).",
    "extendAppCommit": open("$APP_ENV_DEPLOY_DIR/commit").read().strip(),
    "pgDump": "$pgv",
    "bytes": os.path.getsize(dump),
    "sha256": hashlib.sha256(open(dump, "rb").read()).hexdigest(),
    "seedAdditions": "env/seed/extend.py, env/seed/extend.cjs" + (", env/seed/campaign-form.sql" if "$APP_ENV_CONFIG" == "C2" else ""),
    "rowCounts": {k: int(v) for k, v in (l.split("=") for l in """$counts""".split())},
})
json.dump(meta, open(meta_file, "w"), indent=2)
open(meta_file, "a").write("\n")
EOF
snapshot_take || die "snapshot failed"
domain start
wait_ui 300 || die "app did not come back after the dump"
log "EXTENDED $APP_ENV_CONFIG (part(s) $parts) in $((SECONDS - t0)) s: $SNAPSHOT_DUMP ($(du -h "$SNAPSHOT_DUMP" | cut -f1)), $(echo $counts | tr ' ' ',')"
