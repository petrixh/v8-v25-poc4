# Reproducible environment

One command starts a copy of SORMAS in a known state: Payara 7.2026.10 on JDK 21, PostgreSQL 14, a synthetic data set
from a committed snapshot and a local mail sink, in one of two test configurations (C1 default, C2 Germany with optional
features). Several copies can run side by side, each with its own port, containers, database and configuration.

**Payara 7 runtime.** The image is Payara Server 7 Community 7.2026.10 (Jakarta EE 11) on JDK 21. Since migration
ticket 1a-91 the app builds and deploys on it (`APP_ENV_DEPLOY=1`, the default). `APP_ENV_DEPLOY=0` still starts the
domain, database (with the snapshot and the reset point) and mail sink without the app (see "Settings" and "Payara 7
domain").

```sh
env/up.sh                        # build, start C1 with its snapshot: http://localhost:18080/sormas-ui/
env/reset.sh                     # back to the snapshot state
env/down.sh                      # stop and remove this copy
APP_ENV_CONFIG=C2 APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-new-c2 env/up.sh --no-build   # a C2 copy next to it
APP_ENV_DEPLOY=0 env/up.sh       # the domain only (no app build, no deploy): http://localhost:18080/
```

Log in with a default user (`admin`, `SurvSup`, `NatUser`, ...; passwords in
`sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java`). Test environment only: the default
users and their default passwords exist on purpose. C2 also has the seed user `CfgUser`, who logs in with `NatUser`'s
default password ("Seed additions").

## Requirements

Docker with Compose v2, bash, git, curl, python3 (standard library only) and flock. About 4 GB of RAM per copy.
The first build downloads the Maven dependencies, the Docker images and the Payara zip from Maven Central.
An old-app copy (the Payara 5 environment of the old app, names `sormas-old-*`) can run next to new-app copies: names,
images (`sormas-old-env-payara:5.2022.5`, `sormas-new-env-payara:7.2026.10`) and state directories (`env/.state` of each
checkout) differ; give each copy its own `APP_ENV_PORT`.

## Settings

Each script reads these variables; the defaults start one copy.

| Variable | Default | What |
|---|---|---|
| `APP_ENV_PORT` | `18080` | Port of the app on 127.0.0.1 (`/sormas-ui`, `/sormas-rest`) |
| `APP_ENV_PROJECT` | `sormas-new-env` | Compose project: names the network `<project>_default`, the database volume `<project>_dbdata` and the containers `<project>-db`, `<project>-mail` |
| `APP_ENV_CONTAINER` | `<project>-app` | Name of the app container |
| `APP_ENV_MAIL_PORT` | free port | Port of the mail sink's web UI on 127.0.0.1 (`up.sh` prints it) |
| `APP_ENV_RESET_MODE` | `restart` | `reset.sh`: `restart` or `db` (see "Reset") |
| `APP_ENV_STATE_DIR` | `env/.state` | Build output and per-copy files (git-ignored) |
| `APP_ENV_M2_REPO` | `~/.m2/repository` | Maven repository used by the build container |
| `APP_ENV_WAIT_S` | `900` | How long `up.sh` waits for the app |
| `APP_ENV_CONFIG` | `C1` | Test configuration, `C1` or `C2` (see "Configurations") |
| `APP_ENV_UI_URL` | `https://localhost:<APP_ENV_PORT>/sormas-ui/` | `ui.url` of the copy: the link target in notification mails (see "Integrations") |
| `APP_ENV_PWCHECK` | `off` | `on`: the "Security Alert: default password" dialog after a default-user login (`skipDefaultPasswordCheck=false`) |
| `APP_ENV_SEED` | `snapshot` | `up.sh` data: `snapshot` (the committed `env/seed/snapshot-<C>.pgdump`) or `rest` (only the small REST seed; `env/seed/generate.sh` uses it) |
| `APP_ENV_DEPLOY` | `1` | `0`: the domain only. `build.sh` does not build the app, the container does not deploy it, `up.sh` waits for `GET /` (Payara's default page) instead of the login page and takes the reset point right after loading the snapshot. Needs `APP_ENV_SEED=snapshot`. Was used in migration stage 1a while the app did not deploy on Payara 7 (until ticket 1a-91) |

Use the same variables for `up.sh`, `reset.sh` and `down.sh` of one copy. `APP_ENV_CONFIG`, `APP_ENV_PWCHECK`,
`APP_ENV_UI_URL`, `APP_ENV_SEED` and `APP_ENV_DEPLOY` matter to `up.sh` only: the copy keeps them until `down.sh`
(`APP_ENV_CONFIG` also tells the smoke check which users the copy has; `up.sh` records `APP_ENV_DEPLOY` in the copy's
state directory for `reset.sh`; it counts only while the copy's app container exists, so a record left by an
interrupted `up.sh` does not change the next `up.sh`). Two copies:

```sh
APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-new-1 env/up.sh
APP_ENV_PORT=18082 APP_ENV_PROJECT=sormas-new-2 env/up.sh --no-build
APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-new-1 env/down.sh
```

These scripts follow the replay runner's env-script contract (`up.sh [--no-build]`, `reset.sh`, `down.sh`, configured by
`APP_ENV_PORT`, `APP_ENV_PROJECT`, `APP_ENV_CONTAINER`).

## What the scripts do

`up.sh [--no-build]`
1. Refuses to start when this copy's containers already exist (run `down.sh` first).
2. Builds (`build.sh`), unless `--no-build` is given and the image and the built artefacts exist (with
   `APP_ENV_DEPLOY=0`: the image, the server libraries and the setup files):
   - builds the image `sormas-new-env-payara:7.2026.10` first: `eclipse-temurin:21-jdk` plus the Payara 7 zip from Maven
     Central (sha1 checked), domain set up as `sormas-base/setup/server-setup.sh` does (`env/payara/`, see "Payara 7
     domain"). Works on arm64 and amd64. It does not depend on the app, so a failing app build cannot fail it.
   - on a clean clone of this checkout's **committed HEAD** in `env/.state/build/src` (uncommitted changes are not
     built; `up.sh` warns; the files under `env/` are used from the working tree): collects the `sormas-base/setup`
     files and the server libraries (`dependency:copy-dependencies` on `sormas-serverlibs`) into
     `env/.state/build/deploy`.
   - unless `APP_ENV_DEPLOY=0`: `mvn clean install -Dmaven.test.skip=true` (the tests are neither compiled nor run;
     CI does both) with Maven 3.9 on JDK 21 in a container (`maven:3.9-eclipse-temurin-21`), default profiles, and
     collects the ear and the two wars. The production frontend build of `sormas-ui` checks the Vaadin commercial
     licence (Rich Text Editor): `build.sh` passes `VAADIN_PRO_KEY` or `VAADIN_OFFLINE_KEY` to the container by name
     when set, else mounts `~/.vaadin/proKey` read-only. Without a key the build fails.
   - A lock makes a second build wait for the first. Log: `env/.state/build/build.log`.
3. Generates a throwaway database password for this copy (`env/.state/copies/<project>/db.env`, mode 600; never in an
   image or a tracked file) and writes the copy's `sormas.properties` (`env/.state/copies/<project>/sormas.properties`,
   mounted over the shared one): the stock `sormas-base/setup/sormas.properties`, then `env/sormas.properties.env`, then
   `env/config/<C>.properties`, then `ui.url=$APP_ENV_UI_URL`, then `skipDefaultPasswordCheck=false` if
   `APP_ENV_PWCHECK=on`. Later lines win.
4. Starts the compose project (`env/compose.yml`): `db` (postgres:14, fresh volume), `mail` (SMTP sink) and `app`
   (Payara; deploys the ear and the wars at container start, unless `APP_ENV_DEPLOY=0`). With `APP_ENV_SEED=snapshot` it starts `db` first and
   loads `env/seed/snapshot-<C>.pgdump` into it (`pg_restore`, objects owned by the app's database user), so that the
   app's first start finds the data.
5. Waits until `GET /sormas-ui/login` answers 200 (`APP_ENV_DEPLOY=0`: `GET /`). With `APP_ENV_SEED=rest` it seeds the
   small data set (`env/seed/seed.py`). Then it waits for the app's case completeness job (every 2 minutes) until
   every case has been written by it, so that the job does not change cases after a reset (not with
   `APP_ENV_DEPLOY=0`: no app, no job).
6. Stops the app's Payara domain, sets the configuration's feature switches (`env/config/<C>.sql`, C2 only), copies the
   database to the template database `sormas_seed` (the reset point), starts the domain again and waits for 200. The
   copy is now in exactly the state `reset.sh` restores.

`reset.sh` restores the database from `sormas_seed`, empties the mail sink and removes uploaded document templates and
documents (`/opt/sormas/custom/docgeneration` and `/opt/sormas/documents` in the app container: their database rows go
with the restore, the files would stay). Modes (`APP_ENV_RESET_MODE`):
- `restart` (default): stops the Payara domain, restores, starts it. Nothing in memory survives: no sessions, no cached
  users, roles or infrastructure (Hibernate second-level cache).
- `db`: restores the database while the app runs. Logged-in sessions and the cache survive; use it only when tests
  do not change users, roles or infrastructure.

`down.sh` removes this copy's containers, network and database volume (`docker compose down --volumes`) and its
state directory. It is safe after a half-finished `up.sh` and when the copy is already gone. Other copies, the build
output and the images stay.

Measured on a 10-CPU arm64 host on the Payara 5 environment of the old app (2026-11-05, three copies at once for the
snapshot rows); Payara 7 with the app is in the second table below:

| Step | Time |
|---|---|
| Build (warm Maven repository) | about 1 min |
| `up.sh --no-build` (snapshot, C1 or C2) | about 70 s: loading the snapshot 5 s, start about 38 s, reset point about 30 s |
| `up.sh --no-build` with `APP_ENV_SEED=rest` | 1.5 to 3 min: start about 35 s, seed 5 s, waiting for the completeness job 0 to 2 min, reset point about 25 s |
| `env/seed/generate.sh --no-build` | about 6 min: the `rest` start above, the generator about 80 s, the completeness job up to 2 min, dump and reset point about 30 s |
| `reset.sh` (`restart`) | about 25 s |
| `reset.sh` (`db`) | about 1 s |
| Memory per copy | app about 2.7 to 3.1 GB (heap limit 4 GB), database about 0.1 GB, mail sink about 10 MB |

Payara 7, `APP_ENV_DEPLOY=0` (same host, 2026-11-06, two copies):

| Step | Time |
|---|---|
| Image build (Payara zip already downloaded) | about 30 s; image 1.73 GB |
| Server libraries, cold Maven repository | about 2.5 min |
| `up.sh --no-build` (snapshot, C1 or C2) | about 20 s: loading the snapshot 4 s, domain answers after 8 to 9 s, reset point about 10 s |
| `reset.sh` (`restart`) | about 6 s |
| Memory per copy | domain about 0.9 to 1.0 GB, database about 0.06 GB |

Payara 7 with the app, `APP_ENV_DEPLOY=1` (same host, 2026-11-06, ticket 1a-91, one copy, C1):

| Step | Time |
|---|---|
| Build (warm Maven repository, tests not compiled; frontend production bundle) | about 50 to 60 s |
| `up.sh` after the build (snapshot, C1) | about 55 s: loading the snapshot 4 s, the app answers after 30 s (deploy of the ear about 15 s), reset point about 25 s; 107 s including the build |
| Memory per copy | app about 2.3 GB after the first logins (heap limit 4 GB), database about 0.09 GB |

## Configurations

`APP_ENV_CONFIG` selects one of two test configurations. Both run with `createDefaultEntities=true`,
`skipDefaultPasswordCheck=true` (unless `APP_ENV_PWCHECK=on`), `devmode=true` and mail to the local sink
(`env/sormas.properties.env`), each with its own snapshot.

| | C1 (default) | C2 Germany |
|---|---|---|
| `sormas.properties` | stock (`country.locale=en`); `env/config/C1.properties` adds nothing | `country.locale=de-DE` (`env/config/C2.properties`): German texts and date formats for users without a language of their own, and the DE-specific UI (`isConfiguredCountry("de")`) |
| Features (`featureconfiguration` table) | stock | `env/config/C2.sql` switches on campaigns, AEFI, environments, surveys, self reports, the GDPR consent popup at login, travel entries, case follow-up (the case's visits tab), the area infrastructure type, external e-mails (e-mail templates) and documents; external messages and SORMAS-to-SORMAS stay off |
| Default infrastructure names | "Default Region", "Default Facility", ... | German: "Voreingestelltes Bundesland", "Voreingestellte Einrichtung", ... (made by the app in `country.locale`) |

The feature switches are set in the database because that is where SORMAS keeps them (`docs/SERVER_CUSTOMIZATION.md`;
the admin UI's Features page offers only two other switches). `up.sh` runs `env/config/<C>.sql` with the app stopped,
before the reset point, so the app reads them at start. In C2 the GDPR popup opens at every login of a user who has not
consented; closing it consents to nothing. In C2, `NatUser` has every switched-on menu (Umgebungen, Adverse Events,
Einreisen, Kampagnen, Self Reports, Surveys); `SurvSup` has all but environments and AEFI (role rights). The seed user
`CfgUser` (C2) has `NatUser`'s rights plus those of the disease configuration, the e-mail templates and the Epipulse
export (`statistics/epipulse-export`; the statistics view shows its tab only with the `EPIPULSE_EXPORT` feature, which
stays off, so the view is opened by its address).
`APP_ENV_PWCHECK=on` works with either configuration.

## Seed data

`up.sh` loads the configuration's committed snapshot, `env/seed/snapshot-<C>.pgdump` (`pg_dump` custom format of
`sormas_db`), with its metadata in `env/seed/snapshot-<C>.json`: snapshot date, app commit, generator settings, row
counts and the dump's sha256. Each snapshot holds the small REST seed, on top of it data from SORMAS's own dev-mode
generator, and on top of that the seed additions (one record for each detail view the other two leave empty, and in
C2 a campaign form, an area and a test user for the views no default user reaches). The new
app restores the same files (its schema must match the old app's).

| Snapshot | Snapshot date | Size | Cases (investigation pending) | Contacts | Events (participants) | Samples (at the lab, result pending) | Tasks (SurvOff / SurvSup) | Persons |
|---|---|---|---|---|---|---|---|---|
| C1 | 2026-11-05 | 0.97 MB | 84 (57) | 151 | 13 (85) | 41 (40) | 60 (6 / 54) | 232 |
| C2 | 2026-11-05 | 0.98 MB | 84 (50) | 131 | 13 (76) | 41 (40) | 53 (6 / 47) | 224 |

All records are in the default region and district (the only ones). `SurvSup` and `NatUser` see all of them, `SurvOff`
those of the district, `LabOff` the samples at the default laboratory.

**Dates.** All dates in a snapshot are relative to its snapshot date and do not move: not on `reset.sh`, not on `up.sh`.
The REST seed's dates are whole days before or after the snapshot date at 10:00 UTC; the generated records fall in the
90 days up to the snapshot date. Workflows that need "today" compute it from `snapshotDate` in the metadata (also in
`env/.state/copies/<project>/seed-date`). The longer a snapshot is in use, the older its data looks to the app (e.g.
follow-up periods end, tasks become overdue): make a new snapshot when that matters, for both configurations at once.

**REST seed** (`env/seed/seed.py`): created through the app's REST API (`/sormas-rest/.../push`, Basic auth as
`SurvSup`), so the app's own rules apply. Names are obviously synthetic (`Alpha Seedperson`, ...). Fixed UUIDs: the short
ids the UI shows are `SPER01`, `SCAS01`, `SCON01` and so on.

| Record | Values |
|---|---|
| Persons | 8: Alpha ... Hotel `Seedperson`, fixed sex and birth dates |
| Cases | 4: SCAS01 COVID-19 confirmed (investigation done), SCAS02 COVID-19 not a case, SCAS03 measles suspect, SCAS04 cholera not yet classified; at the default facility |
| Contact | SCON01: Echo, confirmed contact of SCAS01 |
| Event | SEVT01 "Seed event: community gathering", COVID-19, moderate risk, with participant Foxtrot (SEVP01) |
| Sample | SSAM01 of SCAS01 at the default laboratory, with a positive PCR test (SPAT01) |
| Tasks | STSK01 case investigation of SCAS02, STSK02 contact follow-up of SCON01, both for SurvOff |
| Immunization | SIMM01: Golf, COVID-19 vaccination, completed |
| Travel entry | STRV01: Hotel, via the default point of entry (the travel entry views are in the menu in C2 only) |

**Generated data** (Configuration > Developer, `DevModeView`, driven by `env/seed/devmode.cjs`): COVID-19 cases,
contacts of those cases, events with participants (some of them cases or contacts) and samples of cases sent to the
default laboratory, with the generator's seed `sormasenv` and its default date range (90 days up to the snapshot date).
The app adds tasks (e.g. case investigation, contact follow-up) for SurvOff and SurvSup by its own rules. Names are
random combinations from the generator's built-in lists of fictitious first and last names. The generator does not make
event group, campaign, AEFI, environment, survey or self-report data: the seed additions below add one of each.

The seed makes the generator's choices repeatable, but not the whole data set: UUIDs, creation times and a few values
(additional test results) are random in the app, and generating again on another day moves all dates. The committed
snapshot is therefore the fixed data set, not the generator run.

**Seed additions** (`env/seed/extend.sh`): records neither the REST seed nor the generator makes, so that every detail
view has a record to open, and the default infrastructure the app's own buttons load. Made on the snapshot above
(same snapshot date) through the app: its REST API where it has an endpoint (`env/seed/extend.py`, fixed UUIDs), else
its UI as a user would (`env/seed/extend.cjs`; the app sets the UUIDs). Dates are fixed days before the snapshot date.

| Record | Values | In |
|---|---|---|
| Continents, subcontinents, countries | the app's default lists (6, 27, 201), loaded with "Import default continents / subcontinents / countries" as `admin` | C1, C2 |
| Default point of entry | set active (the app creates it inactive) as `admin`, so it is offered in forms and shown without "(Inactive)" | C1, C2 |
| Event group | SEGR01 "Seed event group", linked to the event the event directory lists first (newest change), so the directory's order stays the same | C1, C2 |
| Environment | SENV01 "Seed environment: village well" (water, groundwater, investigation pending), with environment sample SESA01 (water, at the default laboratory, received) | C2 |
| Immunization | SIMM02: India `Seedperson` (SPER09), COVID-19, one vaccination (SVAC01, COMIRNATY) | C2 |
| Adverse event | report SEED-AEFI-01 on SIMM02 (serious, hospitalization, fever, recovered) with investigation SEED-INV-01 | C2 |
| Campaign | "Seed campaign: vaccination drive", 30 days before to 30 days after the snapshot date, no form | C2 |
| Survey | "Seed survey: symptom follow-up" (COVID-19) with one imported token `SEEDTOKEN01` | C2 |
| Self report | Juliett `Seedperson`, case, COVID-19, imported from CSV | C2 |
| Campaign form | SCFM01 "Seed form: vaccination round" (`env/seed/campaign-form.sql`: section, label, two numbers, yes/no, a text shown on "no"; German captions for the German UI), linked to the seed campaign | C2 (part 2) |
| Campaign form data | SCFD01 of that form and campaign, in the default community, 2 days before the snapshot date, as NatUser | C2 (part 2) |
| Area | "Seed area", holding the default region: with the area infrastructure type on, the campaign form data form needs an area and offers only the regions of the chosen one. The region's area is the one change to an existing row | C2 (part 2) |
| User role and user | role "Seed role: configuration and Epipulse" (from NatUser's role as template, plus `DISEASE_MANAGEMENT`, `EMAIL_TEMPLATE_MANAGEMENT` and the four `EPIPULSE_EXPORT_*` rights); user `CfgUser` (Config Seeduser) with that role and `NatUser`'s password | C2 (part 2) |

The features of the C2-only records are off in C1. Part 2 is the C2 configuration's second set of additions (made
later, on top of part 1). SORMAS has no UI or REST endpoint that creates campaign form definitions, so `extend.sh`
loads the form into its table with the app stopped, as deployments do. The app only generates passwords for new users
(it shows them once); `extend.sh` does not read it and copies `NatUser`'s password hash and seed to `CfgUser`, so no
password is in any file.

**Make a new snapshot** (for example after a schema change, or to move the dates), per configuration:

```sh
APP_ENV_CONFIG=C1 APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-new-gen-c1 env/seed/generate.sh --no-build
APP_ENV_CONFIG=C2 APP_ENV_PORT=18082 APP_ENV_PROJECT=sormas-new-gen-c2 env/seed/generate.sh --no-build
APP_ENV_CONFIG=C1 APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-new-gen-c1 env/seed/extend.sh
APP_ENV_CONFIG=C2 APP_ENV_PORT=18082 APP_ENV_PROJECT=sormas-new-gen-c2 env/seed/extend.sh
# check the counts they print, look at the data, then commit env/seed/snapshot-C1.* and snapshot-C2.*
```

`generate.sh` starts a copy with `APP_ENV_SEED=rest` (fresh database, REST seed, the configuration's feature switches),
then runs the dev-mode generator through the UI as `SurvSup` with the settings at the top of `generate.sh`, waits for the
case completeness job, stops the app and writes the dump and the metadata. The generator picks source cases for contacts
and samples within the user's jurisdiction, and `admin`, the only default user with the `DEV_MODE` right, has none; so
for the run `generate.sh` gives SurvSup's role the `DEV_MODE` right and removes it again before the dump (the role's
history table `userroles_userrights_history` keeps one row of it). It needs Node and Playwright as the smoke check does. The copy stays up in the new
snapshot's state; remove it with `down.sh`.

`extend.sh` adds the seed additions to a copy that holds the committed snapshot (started with `up.sh`, unchanged): the
parts the copy lacks (part 1 when it has no event group, part 2 in C2 when it has no `CfgUser`; it refuses a copy that
has both). It stops the app and writes the dump and the metadata again: the snapshot date stays, `extendedAt` (part 1)
and `extendedPart2At` (part 2) record when. To add them to the current snapshot without regenerating it, start a copy with
`up.sh --no-build` and run `extend.sh` on it (about 1 min for C1, 2 min for C2). It needs Node and Playwright as the
smoke check does. Running `extend.sh` again does not reproduce the UUIDs or the creation and change times of the records
made through the UI (campaign, survey and token, self report, adverse event and investigation, area, user role, user):
the committed snapshot is the fixed seed, not the script run.

Set by the app, not by the seed: creation and change dates, epid numbers and the UUIDs of the default users and of the
default facility, laboratory and point of entry. They are in the snapshot, so every copy started from the same
snapshot has the same row counts and UUIDs (checked for two C1 copies), and `reset.sh` returns to them.

## Integrations

- Mail: the app's mail session (`mail/MailSession`) sends to the copy's own SMTP sink (Mailpit, service `mail`, SMTP
  port 1025). Nothing leaves the host. Task mails link to the task's case, contact or event under `ui.url`
  (`APP_ENV_UI_URL`). The app refuses to start with an `ui.url` that is not https (`ConfigFacadeEjb.validateConfigUrls`),
  so the default names the copy's own port with https although the copy serves http: the link shows the right view
  path; to open it, change the scheme to http. Read the mails in the sink's web UI (URL printed by `up.sh`, or
  `docker compose -p <project> -f env/compose.yml port mail 8025`) or its API (`/api/v1/messages`).
- Everything else stays at the stock values of `sormas-base/setup/sormas.properties`, which leave all external
  systems off (SORMAS-to-SORMAS, SurvNet, geocoding, SMS, patient diary, external messages).
- Login: the built-in SORMAS login. No Keycloak.
- Vaadin runs in production mode (the stock setting).

## Smoke check

`env/smoke.cjs` checks a running copy: each web test user in `env/smoke-users.json` (`admin`, `SurvSup`, `NatUser`,
`SurvOff`, `LabOff`, `HospInf`, `PoeSup`, `NatClin`; on a C2 copy, with `APP_ENV_CONFIG=C2`, also `CfgUser`) logs in, and the first view must load with no console or page error, no HTTP
response of 400 or more and no collapsed layout (a scroll height over 4 window heights). `PoeInf` must be refused by the
web login ("Your user account does not have access to the web application"): its role has no web UI right. CI runs it
after every build (`CI.md`).

```sh
APP_ENV_PORT=18091 APP_ENV_PROJECT=sormas-new-smoke env/up.sh --no-build
APP_ENV_PORT=18091 APP_ENV_PROJECT=sormas-new-smoke node env/smoke.cjs   # exit 0: all pass
APP_ENV_PORT=18091 APP_ENV_PROJECT=sormas-new-smoke env/down.sh
```

- Base URL: `http://localhost:$APP_ENV_PORT/sormas-ui`, or `APP_ENV_SMOKE_URL`. Output: one screenshot per user (the
  window is made as tall as the view's scrolling content, up to 4 window heights) and
  `smoke.json` (results) in the directory given as the first argument, default `env/.state/smoke/<project>`.
- Needs Node (22 tested) and Playwright for Node 1.63 with Chromium (`npm install -g playwright@1.63.0`,
  `playwright install chromium`); a local install, a global one or `PW=<path to the playwright module>`.
- Passwords are read at run time from `DefaultEntityHelper.java`, as the seeding does; the script never prints or
  writes them.
- The browser sends a real Chrome user agent (`HeadlessChrome` replaced with `Chrome`): Vaadin 8 gives `HeadlessChrome`
  the ES5 client bundle, which real browsers do not get.

## Logs and troubleshooting

- App: `docker logs <APP_ENV_CONTAINER>` (the Payara server log).
- Build: `env/.state/build/build.log`.
- Known, from the app (not changed here): on the Payara 5 environment of the old app, deployment logs `SEVERE` lines
  (e.g. `@Lock is only permitted for singleton session beans`, annotation processing of `sormas-backend`) that do not
  stop the app. Payara 7 logs the same ones (checked in ticket 1a-91), plus a deprecation warning for each
  `glassfish-web.xml` and, when the domain stops, a `SEVERE` about a ThreadLocal of Payara's ORB left in `/sormas-ui`.
- `server.log` in a fresh container starts with the image build's own domain start (`WARNING` lines "cannot take effect
  without server restart" for the JVM options set then); they are from the build, not from this start.

## Payara 7 domain

`env/payara/payara-setup.sh` sets up the domain `sormas` at image build time, as `sormas-base/setup/server-setup.sh`
does and as the Payara 5 environment of the old app did. No secrets in the image: the JDBC pool reads the database host
and password from the container env at run time (`${ENV=SORMAS_DB_HOST}`, `${ENV=SORMAS_DB_PW}`).

| Setting | Value |
|---|---|
| Server | Payara Server 7 Community (Full) 7.2026.10 (Jakarta EE 11), zip from Maven Central, on `eclipse-temurin:21-jdk` (Temurin 21) |
| Ports | port base 4800: HTTP 4880 (published on 127.0.0.1:`APP_ENV_PORT`), admin 4848 (inside the container only) |
| JDBC | pool `sormasDataPool` (`org.postgresql.ds.PGConnectionPoolDataSource`, PostgreSQL connection validation, max 128, database `sormas_db`, user `sormas_user`), resource `jdbc/sormasDataPool` (the name in `sormas-backend`'s `persistence.xml`) |
| Mail | `mail/MailSession`: host `mail` (the copy's SMTP sink), port 1025, user `sormas`, from `noreply@sormas.local` |
| Properties | custom resource `sormas/Properties` reading `${com.sun.aas.instanceRoot}/sormas.properties` (this copy's file, copied in at start) |
| JVM | `-Xmx4096m`, `-XX:+IgnoreUnrecognizedVMOptions`, logback configuration `config/logback.xml`, `-DlogbackDisableServletContainerInitializer=true`; time zone Europe/Berlin from the container's `TZ` (the JVM reports `user.timezone = Europe/Berlin`) |
| Class loading | system property `fish.payara.classloading.delegate=false` |
| Pools | EJB `max-pool-size` 128, HTTP thread pool max 50 |
| Domain lib | the server libraries (`sormas-serverlibs`, collected by `build.sh`) copied to `domains/sormas/lib/` at container start; also holds the PostgreSQL driver the JDBC pool needs |
| Login | none in the domain: SORMAS authenticates in the app (Jakarta Security: `SormasIdentityStore` and `MultiAuthenticationMechanism` in the two wars) and maps its user rights to roles through `glassfish-ejb-jar.xml` / `glassfish-web.xml`; the setup scripts create no realm, as before. No Keycloak |
| Directories | `/opt/sormas/{custom,documents,temp,generated}`; login pages from `sormas-base/setup` into `custom` at start |

Differences to the Payara 5 environment of the old app (`sormas-old-env-payara:5.2022.5`):

- Payara 5.2022.5 (Jakarta EE 8, `javax.*`) on JDK 11 → Payara 7.2026.10 (Jakarta EE 11, `jakarta.*`) on JDK 21; the
  mail resource uses Eclipse Angus Mail.
- EJB pool: 128 as `server-setup.sh` intends. The old environment's `set` of it failed (no `get` before it) and the
  pool stayed at Payara's default of 32.
- A failed setup command fails the image build, the EJB pool `set` included. The old environment only warned when that
  `set` failed (`WARN: ejb-container max-pool-size not set`) and built the image anyway.
- The domain lib holds today's server library list (`sormas-serverlibs/pom.xml` at HEAD). Migration ticket 1a-02
  changes that list to Jakarta EE 11 artefacts; `build.sh` collects whatever the pom says, so nothing changes here.
  Ticket 1a-91 deployed the app with it: no missing class at deploy or on the screens it opened.
- `APP_ENV_DEPLOY=0` (new): the domain without the app.
- Payara 7's own defaults differ from Payara 5's (e.g. its default JVM options with `--add-opens` for JDK 21); the
  setup does not change them.
