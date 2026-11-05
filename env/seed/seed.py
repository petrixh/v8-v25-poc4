#!/usr/bin/env python3
"""Seed one app copy with a small, fixed, obviously synthetic data set through the app's own REST API (sormas-rest).

Usage: seed.py <base-url> <DefaultEntityHelper.java>      e.g. seed.py http://localhost:18080 sormas-api/.../DefaultEntityHelper.java

Run by env/up.sh on a fresh database that holds only what createDefaultEntities makes (default users and the "Default"
region, district, community, facility, laboratory and point of entry). Everything goes through the API as a default user
(Basic auth), so the app's own validation and logic apply (e.g. epid numbers, jurisdiction, follow-up).

Deterministic: fixed UUIDs, names and values; no random data. Dates are whole days relative to the seed date (UTC) at
10:00 UTC, so lists sorted by date look the same on any day. Server-set values (creation/change dates, epid numbers,
the default users' and default facility's UUIDs) are made by the app. The password is read from the given file at run
time and never printed. Standard library only.
"""
import base64
import datetime
import json
import re
import sys
import urllib.error
import urllib.request

BASE, HELPER = sys.argv[1].rstrip("/"), sys.argv[2]
PUSH_USER = "SurvSup"  # surveillance supervisor of the Default region: has SORMAS_REST and sees all seeded data
PASSWORDS = dict(re.findall(r'Pair<>\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)', open(HELPER, encoding="utf-8").read()))
TODAY = datetime.datetime.now(datetime.timezone.utc).replace(hour=10, minute=0, second=0, microsecond=0)


def day(offset):
    """Epoch millis of the seed date + offset days, 10:00 UTC."""
    return int((TODAY + datetime.timedelta(days=offset)).timestamp() * 1000)


def birth(y, m, d):
    return {"birthdateYYYY": y, "birthdateMM": m, "birthdateDD": d}


def uid(kind, n):
    """Fixed UUID: the first block is what the UI shows as the short id (e.g. SCAS01)."""
    return f"S{kind}{n:02d}-SEED00-SEED00-SEED00"


def ref(u):
    return {"uuid": u}


def call(method, path, body=None):
    req = urllib.request.Request(BASE + "/sormas-rest" + path, method=method,
                                 data=None if body is None else json.dumps(body).encode())
    token = base64.b64encode(f"{PUSH_USER}:{PASSWORDS[PUSH_USER]}".encode()).decode()
    req.add_header("Authorization", "Basic " + token)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        sys.exit(f"seed: {method} {path} -> HTTP {e.code}: {e.read().decode(errors='replace')[:500]}")


def push(path, dtos):
    """POST a list to a /push endpoint; every entry must come back 200."""
    res = call("POST", path, dtos)
    bad = [(d.get("uuid"), r) for d, r in zip(dtos, res or []) if r.get("statusCode") != 200]
    if bad or len(res or []) != len(dtos):
        sys.exit(f"seed: {path} failed: {bad or res}")
    print(f"seed: {path}: {len(dtos)} ok")


# --- what createDefaultEntities made (UUIDs of users and the facility differ per database, so look them up) ------------
REGION = ref("00000000-0000-0000-0000-000000000003")
DISTRICT = ref("00000000-0000-0000-0000-000000000004")
COMMUNITY = ref("00000000-0000-0000-0000-000000000005")
users = {u["userName"]: ref(u["uuid"]) for u in call("GET", "/users/all/0")}
facilities = {f["name"]: f for f in call("GET", f"/facilities/region/{REGION['uuid']}/0")}
HOSPITAL = ref(facilities["Default Facility"]["uuid"])
LAB = ref(facilities["Default Laboratory"]["uuid"])
POE = ref(next(p["uuid"] for p in call("GET", "/pointsofentry/all/0") if p["name"] == "Default Point Of Entry"))
SURVSUP, SURVOFF, LABOFF = users["SurvSup"], users["SurvOff"], users["LabOff"]
JURISDICTION = {"responsibleRegion": REGION, "responsibleDistrict": DISTRICT, "responsibleCommunity": COMMUNITY}

if call("GET", "/cases/uuids"):
    sys.exit("seed: the database already holds cases; seed only a fresh copy (env/up.sh does)")

# --- persons ---------------------------------------------------------------------------------------------------------
PERSONS = [  # n, first name, last name, sex, birth date
    (1, "Alpha", "Seedperson", "FEMALE", birth(1980, 3, 14)),
    (2, "Bravo", "Seedperson", "MALE", birth(1975, 7, 2)),
    (3, "Charlie", "Seedperson", "MALE", birth(1990, 11, 23)),
    (4, "Delta", "Seedperson", "FEMALE", birth(2001, 5, 9)),
    (5, "Echo", "Seedperson", "FEMALE", birth(1968, 1, 30)),
    (6, "Foxtrot", "Seedperson", "MALE", birth(1985, 9, 17)),
    (7, "Golf", "Seedperson", "FEMALE", birth(1995, 12, 1)),
    (8, "Hotel", "Seedperson", "MALE", birth(1972, 4, 8)),
]


def person(n):
    _, first, last, sex, bd = next(p for p in PERSONS if p[0] == n)
    return {"uuid": uid("PER", n), "firstName": first, "lastName": last, "sex": sex, **bd,
            "address": {"uuid": uid("ADR", n), "region": REGION, "district": DISTRICT, "community": COMMUNITY,
                        "city": "Seedville", "street": "Example Street", "houseNumber": str(n), "postalCode": "00000"}}


push("/persons/push", [person(n) for n, *_ in PERSONS])

# --- cases: three diseases, four classifications (the app keeps these; its automatic classification changed PROBABLE) ---
CASES = [  # n, person, disease, classification, report day, investigation status
    (1, 1, "CORONAVIRUS", "CONFIRMED", -10, "DONE"),
    (2, 2, "CORONAVIRUS", "NO_CASE", -7, "PENDING"),
    (3, 3, "MEASLES", "SUSPECT", -5, "PENDING"),
    (4, 4, "CHOLERA", "NOT_CLASSIFIED", -2, "PENDING"),
]


def case(n, p, disease, classification, report, investigation):
    embedded = {k: {"uuid": uid(code, n)} for k, code in [
        ("hospitalization", "HOS"), ("symptoms", "SYM"), ("epiData", "EPI"), ("therapy", "THE"),
        ("healthConditions", "HEC"), ("clinicalCourse", "CLC"), ("maternalHistory", "MAT"), ("portHealthInfo", "PHI")]}
    embedded["symptoms"].update({"onsetDate": day(report - 2), "symptomatic": True, "fever": "YES", "cough": "YES"}
                                if disease == "CORONAVIRUS" else {"symptomatic": False})
    return {"uuid": uid("CAS", n), "disease": disease, "person": ref(uid("PER", p)), "reportDate": day(report),
            "reportingUser": SURVSUP, "surveillanceOfficer": SURVOFF, "caseClassification": classification,
            "investigationStatus": investigation, "outcome": "NO_OUTCOME", "caseOrigin": "IN_COUNTRY",
            "facilityType": "HOSPITAL", "healthFacility": HOSPITAL, "followUpStatus": "NO_FOLLOW_UP",
            "additionalDetails": "Synthetic seed case", **JURISDICTION, **embedded}


push("/cases/push", [case(*c) for c in CASES])

# --- contact of case 1 -----------------------------------------------------------------------------------------------
push("/contacts/push", [{
    "uuid": uid("CON", 1), "person": ref(uid("PER", 5)), "caze": ref(uid("CAS", 1)), "disease": "CORONAVIRUS",
    "reportDateTime": day(-9), "reportingUser": SURVSUP, "lastContactDate": day(-11), "contactProximities": ["FACE_TO_FACE_LONG"],
    "contactClassification": "CONFIRMED", "contactStatus": "ACTIVE", "contactCategory": "HIGH_RISK",
    "relationToCase": "SAME_HOUSEHOLD", "contactOfficer": SURVOFF, "description": "Synthetic seed contact",
    "region": REGION, "district": DISTRICT, "community": COMMUNITY,
    "epiData": {"uuid": uid("EPI", 51)}, "healthConditions": {"uuid": uid("HEC", 51)}}])

# --- event with one participant ----------------------------------------------------------------------------------------
push("/events/push", [{
    "uuid": uid("EVT", 1), "eventTitle": "Seed event: community gathering", "eventDesc": "Synthetic seed event",
    "eventStatus": "EVENT", "riskLevel": "MODERATE", "eventInvestigationStatus": "PENDING", "disease": "CORONAVIRUS",
    "startDate": day(-8), "reportDateTime": day(-6), "reportingUser": SURVSUP, "responsibleUser": SURVSUP,
    "srcType": "NOT_APPLICABLE", "typeOfPlace": "PUBLIC_PLACE",
    "eventLocation": {"uuid": uid("LOC", 61), "region": REGION, "district": DISTRICT, "community": COMMUNITY,
                      "city": "Seedville", "street": "Market Square"}}])
push("/eventparticipants/push", [{
    "uuid": uid("EVP", 1), "event": ref(uid("EVT", 1)), "person": call("GET", f"/persons/{uid('PER', 6)}"),
    "reportingUser": SURVSUP, "involvementDescription": "Attended the gathering", "region": REGION, "district": DISTRICT}])

# --- sample of case 1 with a positive PCR result ---------------------------------------------------------------------------
push("/samples/push", [{
    "uuid": uid("SAM", 1), "associatedCase": ref(uid("CAS", 1)), "sampleDateTime": day(-9), "reportDateTime": day(-9),
    "reportingUser": SURVSUP, "sampleMaterial": "NP_SWAB", "samplePurpose": "EXTERNAL", "lab": LAB,
    "shipped": True, "shipmentDate": day(-9), "received": True, "receivedDate": day(-8), "specimenCondition": "ADEQUATE",
    "pathogenTestResult": "POSITIVE", "comment": "Synthetic seed sample"}])
push("/pathogentests/push", [{
    "uuid": uid("PAT", 1), "sample": ref(uid("SAM", 1)), "testType": "PCR_RT_PCR", "testedDisease": "CORONAVIRUS",
    "testDateTime": day(-8), "lab": LAB, "labUser": LABOFF, "testResult": "POSITIVE", "testResultVerified": True,
    "testResultText": "Synthetic seed result"}])

# --- tasks ---------------------------------------------------------------------------------------------------------------
push("/tasks/push", [
    {"uuid": uid("TSK", 1), "taskContext": "CASE", "caze": ref(uid("CAS", 2)), "taskType": "CASE_INVESTIGATION",
     "priority": "NORMAL", "taskStatus": "PENDING", "suggestedStart": day(-1), "dueDate": day(2),
     "assigneeUser": SURVOFF, "assignedByUser": SURVSUP, "creatorUser": SURVSUP, "creatorComment": "Synthetic seed task"},
    {"uuid": uid("TSK", 2), "taskContext": "CONTACT", "contact": ref(uid("CON", 1)), "taskType": "CONTACT_FOLLOW_UP",
     "priority": "HIGH", "taskStatus": "PENDING", "suggestedStart": day(0), "dueDate": day(1),
     "assigneeUser": SURVOFF, "assignedByUser": SURVSUP, "creatorUser": SURVSUP, "creatorComment": "Synthetic seed task"}])

# --- immunization --------------------------------------------------------------------------------------------------------
push("/immunizations/push", [{
    "uuid": uid("IMM", 1), "person": ref(uid("PER", 7)), "disease": "CORONAVIRUS", "reportDate": day(-20),
    "reportingUser": SURVSUP, "meansOfImmunization": "VACCINATION", "immunizationStatus": "ACQUIRED",
    "immunizationManagementStatus": "COMPLETED", "startDate": day(-60), "endDate": day(-30), "numberOfDoses": 2,
    "additionalDetails": "Synthetic seed immunization", **JURISDICTION}])

# --- travel entry --------------------------------------------------------------------------------------------------------
push("/travelentries/push", [{
    "uuid": uid("TRV", 1), "person": ref(uid("PER", 8)), "disease": "CORONAVIRUS", "reportDate": day(-3),
    "reportingUser": SURVSUP, "pointOfEntry": POE, "dateOfArrival": day(-4), "responsibleRegion": REGION,
    "responsibleDistrict": DISTRICT, "responsibleCommunity": COMMUNITY, "pointOfEntryRegion": REGION,
    "pointOfEntryDistrict": DISTRICT, "pointOfEntryDetails": "Synthetic seed arrival"}])

print("seed: done")
