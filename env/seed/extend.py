#!/usr/bin/env python3
"""Seed additions, REST part: records the dev-mode generator and the small REST seed do not make, so that every detail
view of the configuration has one record to open (env/seed/extend.sh runs this, then extend.cjs, on a copy that holds
the committed snapshot).

Usage: extend.py <base-url> <DefaultEntityHelper.java> <C1|C2> <snapshot-date YYYY-MM-DD> [parts]
  parts: which additions to make, "1", "2" or "12" (default "1"); env/seed/extend.sh passes the parts the copy lacks.

Additions only: nothing seeded before is changed, except that one existing event is linked to the new event group (the
event the event directory lists first, so that its order does not change). Deterministic: fixed UUIDs, names and values.
Dates are whole days relative to the snapshot date at 10:00 UTC, as in seed.py. Through the app's REST API as default
users (Basic auth), so the app's own validation applies. Passwords are read at run time and never printed.

C1 and C2: event group SEGR01 "Seed event group" with one event.
C2 only (the features are off in C1): environment SENV01 with environment sample SESA01 at the default laboratory;
person India Seedperson (SPER09) with immunization SIMM02 and one vaccination (SVAC01), the base of the AEFI report
that extend.cjs adds.

Part 2 (C2 only, on top of part 1): campaign form data SCFD01 of the seed campaign with the seed campaign form
(env/seed/campaign-form.sql loads the form), as NatUser.
"""
import base64
import datetime
import json
import re
import sys
import urllib.error
import urllib.request

BASE, HELPER, CONFIG, SNAPSHOT_DATE = sys.argv[1].rstrip("/"), sys.argv[2], sys.argv[3], sys.argv[4]
PARTS = sys.argv[5] if len(sys.argv) > 5 else "1"
PASSWORDS = dict(re.findall(r'Pair<>\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)', open(HELPER, encoding="utf-8").read()))
BASE_DAY = datetime.datetime.strptime(SNAPSHOT_DATE, "%Y-%m-%d").replace(hour=10, tzinfo=datetime.timezone.utc)


def day(offset):
    """Epoch millis of the snapshot date + offset days, 10:00 UTC."""
    return int((BASE_DAY + datetime.timedelta(days=offset)).timestamp() * 1000)


def uid(kind, n):
    return f"S{kind}{n:02d}-SEED00-SEED00-SEED00"


def ref(u):
    return {"uuid": u}


def call(user, method, path, body=None):
    req = urllib.request.Request(BASE + "/sormas-rest" + path, method=method,
                                 data=None if body is None else json.dumps(body).encode())
    token = base64.b64encode(f"{user}:{PASSWORDS[user]}".encode()).decode()
    req.add_header("Authorization", "Basic " + token)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            raw = r.read()
            try:
                return json.loads(raw or b"null")
            except ValueError:
                return raw.decode(errors="replace")
    except urllib.error.HTTPError as e:
        sys.exit(f"extend: {method} {path} as {user} -> HTTP {e.code}: {e.read().decode(errors='replace')[:500]}")


def push(user, path, dtos):
    res = call(user, "POST", path, dtos)
    bad = [(d.get("uuid"), r) for d, r in zip(dtos, res or []) if r.get("statusCode") != 200]
    if bad or len(res or []) != len(dtos):
        sys.exit(f"extend: {path} failed: {bad or res}")
    print(f"extend: {path}: {len(dtos)} ok")


REGION = ref("00000000-0000-0000-0000-000000000003")
DISTRICT = ref("00000000-0000-0000-0000-000000000004")
COMMUNITY = ref("00000000-0000-0000-0000-000000000005")
JURISDICTION = {"responsibleRegion": REGION, "responsibleDistrict": DISTRICT, "responsibleCommunity": COMMUNITY}
users = {u["userName"]: ref(u["uuid"]) for u in call("SurvSup", "GET", "/users/all/0")}
SURVSUP, NATUSER = users["SurvSup"], users["NatUser"]
facilities = [f for f in call("SurvSup", "GET", f"/facilities/region/{REGION['uuid']}/0") if not f["uuid"].startswith("SORMAS-CONST")]
LAB = ref(next(f["uuid"] for f in facilities if f.get("type") == "LABORATORY"))

if "1" in PARTS:
    if call("SurvSup", "GET", f"/eventGroups/{uid('EGR', 1)}"):
        sys.exit("extend: the event group already exists; run on a copy with the committed snapshot")

    # --- event group with one event (C1 and C2) -------------------------------------------------------------------------
    # The event directory lists events by change date, newest first. Linking changes the linked event's change date, so link
    # the event the directory already shows first: the directory's order stays the same.
    events = call("SurvSup", "GET", "/events/all/0")
    first = max((e for e in events if not e.get("deleted")), key=lambda e: (e["changeDate"], e["uuid"]))
    push("SurvSup", "/eventGroups/push", [{"uuid": uid("EGR", 1), "name": "Seed event group"}])
    call("SurvSup", "POST", f"/eventGroups/{uid('EGR', 1)}/linkEvents", [ref(first["uuid"])])
    print(f"extend: event group {uid('EGR', 1)} linked to the first event of the directory")

    if CONFIG == "C2":
        # --- environment with one environment sample (NatUser: SurvSup has no environment rights in C2) ----------------
        push("NatUser", "/environments/push", [{
            "uuid": uid("ENV", 1), "environmentName": "Seed environment: village well", "description": "Synthetic seed environment",
            "reportDate": day(-4), "reportingUser": NATUSER, "responsibleUser": NATUSER, "investigationStatus": "PENDING",
            "environmentMedia": "WATER", "waterType": "GROUNDWATER",
            "location": {"uuid": uid("LOC", 71), "region": REGION, "district": DISTRICT, "community": COMMUNITY,
                         "city": "Seedville", "street": "Well Lane", "latitude": 50.0, "longitude": 10.0}}])
        push("NatUser", "/environmentsamples/push", [{
            "uuid": uid("ESA", 1), "environment": ref(uid("ENV", 1)), "reportDate": day(-3), "reportingUser": NATUSER,
            "sampleDateTime": day(-3), "sampleMaterial": "WATER", "laboratory": LAB, "fieldSampleId": "SEED-ESA-01",
            "dispatched": True, "dispatchDate": day(-3), "received": True, "receivalDate": day(-2),
            "specimenCondition": "ADEQUATE", "generalComment": "Synthetic seed environment sample",
            "location": {"uuid": uid("LOC", 72), "region": REGION, "district": DISTRICT, "community": COMMUNITY,
                         "city": "Seedville", "street": "Well Lane", "latitude": 50.0, "longitude": 10.0}}])

        # --- person with an immunization and one vaccination (the AEFI report needs a vaccination) --------------------
        push("SurvSup", "/persons/push", [{
            "uuid": uid("PER", 9), "firstName": "India", "lastName": "Seedperson", "sex": "FEMALE",
            "birthdateYYYY": 1988, "birthdateMM": 6, "birthdateDD": 21,
            "address": {"uuid": uid("ADR", 9), "region": REGION, "district": DISTRICT, "community": COMMUNITY,
                        "city": "Seedville", "street": "Example Street", "houseNumber": "9", "postalCode": "00000"}}])
        push("SurvSup", "/immunizations/push", [{
            "uuid": uid("IMM", 2), "person": ref(uid("PER", 9)), "disease": "CORONAVIRUS", "reportDate": day(-30),
            "reportingUser": SURVSUP, "meansOfImmunization": "VACCINATION", "immunizationStatus": "ACQUIRED",
            "immunizationManagementStatus": "COMPLETED", "startDate": day(-40), "endDate": day(-40), "numberOfDoses": 1,
            "additionalDetails": "Synthetic seed immunization with a vaccination", **JURISDICTION,
            "vaccinations": [{
                "uuid": uid("VAC", 1), "immunization": ref(uid("IMM", 2)), "reportDate": day(-30), "reportingUser": SURVSUP,
                "vaccinationDate": day(-40), "vaccineName": "COMIRNATY", "vaccineManufacturer": "BIONTECH_PFIZER",
                "healthConditions": {"uuid": uid("HEC", 91)}}]}])

if "2" in PARTS and CONFIG == "C2":
    # --- campaign form data of the seed campaign (NatUser: campaign form data edit right and REST access) ---------
    if call("NatUser", "POST", "/campaignFormData/query", [uid("CFD", 1)]):
        sys.exit("extend: the campaign form data already exists; run on a copy without the part 2 additions")
    campaigns = call("NatUser", "GET", "/campaigns/all/0")
    campaign = next(c for c in campaigns if c["name"] == "Seed campaign: vaccination drive")
    push("NatUser", "/campaignFormData/push", [{
        "uuid": uid("CFD", 1), "campaign": ref(campaign["uuid"]), "campaignFormMeta": ref(uid("CFM", 1)),
        "formDate": day(-2), "region": REGION, "district": DISTRICT, "community": COMMUNITY, "creatingUser": NATUSER,
        "formValues": [{"id": "childrenVaccinated", "value": 42}, {"id": "childrenMissed", "value": 3},
                       {"id": "stockSufficient", "value": False}, {"id": "remarks", "value": "Synthetic seed form data"}]}])

print("extend: REST additions done")
