# Reproducible environment

One command starts a copy of SORMAS in a known state: Payara 5.2022.5 on JDK 11, PostgreSQL 14, a small synthetic data
set and a local mail sink. Several copies can run side by side, each with its own port, containers and database.

```sh
env/up.sh          # build, start, seed: http://localhost:18080/sormas-ui/
env/reset.sh       # back to the seed state
env/down.sh        # stop and remove this copy
```

Log in with a default user (`admin`, `SurvSup`, `NatUser`, ...; passwords in
`sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java`). Test environment only: the default
users and their default passwords exist on purpose.

## Requirements

Docker with Compose v2, bash, git, curl, python3 (standard library only) and flock. About 4 GB of RAM per copy.
The first build downloads the Maven dependencies, the Docker images and the Payara zip from Maven Central.

## Settings

Each script reads these variables; the defaults start one copy.

| Variable | Default | What |
|---|---|---|
| `APP_ENV_PORT` | `18080` | Port of the app on 127.0.0.1 (`/sormas-ui`, `/sormas-rest`) |
| `APP_ENV_PROJECT` | `sormas-old-env` | Compose project: names the network `<project>_default`, the database volume `<project>_dbdata` and the containers `<project>-db`, `<project>-mail` |
| `APP_ENV_CONTAINER` | `<project>-app` | Name of the app container |
| `APP_ENV_MAIL_PORT` | free port | Port of the mail sink's web UI on 127.0.0.1 (`up.sh` prints it) |
| `APP_ENV_RESET_MODE` | `restart` | `reset.sh`: `restart` or `db` (see "Reset") |
| `APP_ENV_STATE_DIR` | `env/.state` | Build output and per-copy files (git-ignored) |
| `APP_ENV_M2_REPO` | `~/.m2/repository` | Maven repository used by the build container |
| `APP_ENV_WAIT_S` | `900` | How long `up.sh` waits for the app |

Use the same variables for `up.sh`, `reset.sh` and `down.sh` of one copy. Two copies:

```sh
APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-old-1 env/up.sh
APP_ENV_PORT=18082 APP_ENV_PROJECT=sormas-old-2 env/up.sh --no-build
APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-old-1 env/down.sh
```

These scripts follow the replay runner's env-script contract (`up.sh [--no-build]`, `reset.sh`, `down.sh`, configured by
`APP_ENV_PORT`, `APP_ENV_PROJECT`, `APP_ENV_CONTAINER`).

## What the scripts do

`up.sh [--no-build]`
1. Refuses to start when this copy's containers already exist (run `down.sh` first).
2. Builds (`build.sh`), unless `--no-build` is given and the image and the built artefacts exist:
   - `mvn clean install -DskipTests` with Maven 3.9 on JDK 11 in a container (`maven:3.9-eclipse-temurin-11`), default
     profiles, on a clean clone of this checkout's **committed HEAD** in `env/.state/build/src` (uncommitted changes
     are not built; `up.sh` warns). The files under `env/` are used from the working tree.
   - collects the ear, the two wars, the server libraries and `sormas-base/setup` files into `env/.state/build/deploy`;
     `sormas.properties` is the stock file plus `env/sormas.properties.env`.
   - builds the image `sormas-old-env-payara:5.2022.5`: `eclipse-temurin:11-jdk` plus the Payara zip (sha1 checked),
     domain set up as `sormas-base/setup/server-setup.sh` does (`env/payara/`). Works on arm64 and amd64.
   - A lock makes a second build wait for the first. Log: `env/.state/build/build.log`.
3. Generates a throwaway database password for this copy (`env/.state/copies/<project>/db.env`, mode 600; never in an
   image or a tracked file) and starts the compose project (`env/compose.yml`): `db` (postgres:14, fresh volume),
   `mail` (SMTP sink) and `app` (Payara; deploys the ear and the wars at container start).
4. Waits until `GET /sormas-ui/login` answers 200.
5. Seeds the data set (`env/seed/seed.py`), then waits for the app's case completeness job (every 2 minutes) so that
   it has written the seeded cases before the snapshot.
6. Stops the app's Payara domain, copies the database to the template database `sormas_seed` (the snapshot), starts
   the domain again and waits for 200. The copy is now in exactly the state `reset.sh` restores.

`reset.sh` restores the database from `sormas_seed` and empties the mail sink. Modes (`APP_ENV_RESET_MODE`):
- `restart` (default): stops the Payara domain, restores, starts it. Nothing in memory survives: no sessions, no cached
  users, roles or infrastructure (Hibernate second-level cache).
- `db`: restores the database while the app runs. Logged-in sessions and the cache survive; use it only when tests
  do not change users, roles or infrastructure.

`down.sh` removes this copy's containers, network and database volume (`docker compose down --volumes`) and its
state directory. It is safe after a half-finished `up.sh` and when the copy is already gone. Other copies, the build
output and the images stay.

Measured on a 10-CPU arm64 host (2026-11-05):

| Step | Time |
|---|---|
| Build (warm Maven repository) | about 1 min |
| `up.sh --no-build` | 1.5 to 3 min: start about 35 s, seed 5 s, waiting for the completeness job 0 to 2 min, snapshot about 25 s |
| `reset.sh` (`restart`) | about 25 s |
| `reset.sh` (`db`) | about 1 s |
| Memory per copy | app about 2.7 to 3.1 GB (heap limit 4 GB), database about 0.1 GB, mail sink about 10 MB |

## Seed data

Created through the app's REST API (`/sormas-rest/.../push`, Basic auth as `SurvSup`), so the app's own rules apply. All
records are in the Default region, district and community. Names are obviously synthetic (`Alpha Seedperson`, ...).
Fixed UUIDs: the short ids the UI shows are `SPER01`, `SCAS01`, `SCON01` and so on.

| Record | Values |
|---|---|
| Persons | 8: Alpha ... Hotel `Seedperson`, fixed sex and birth dates |
| Cases | 4: SCAS01 COVID-19 confirmed (investigation done), SCAS02 COVID-19 not a case, SCAS03 measles suspect, SCAS04 cholera not yet classified; at the Default Facility |
| Contact | SCON01: Echo, confirmed contact of SCAS01 |
| Event | SEVT01 "Seed event: community gathering", COVID-19, moderate risk, with participant Foxtrot (SEVP01) |
| Sample | SSAM01 of SCAS01 at the Default Laboratory, with a positive PCR test (SPAT01) |
| Tasks | STSK01 case investigation of SCAS02, STSK02 contact follow-up of SCON01, both for SurvOff |
| Immunization | SIMM01: Golf, COVID-19 vaccination, completed |
| Travel entry | STRV01: Hotel, via the Default Point Of Entry (the travel entry views are not in SurvSup's menu) |

Dates are whole days relative to the seed date (the UTC date `up.sh` ran, in `env/.state/copies/<project>/seed-date`)
at 10:00 UTC. A copy keeps its seed date until it is started again with `up.sh`; `reset.sh` does not move it.

Set by the app, not by the seed: creation and change dates, epid numbers, the UUIDs of the default users and of the
Default Facility, Laboratory and Point Of Entry, and four tasks the app creates by itself when cases and contacts are
saved (random UUIDs, times of the seeding). Two copies seeded on the same day therefore have the same row counts and
the same seeded records, but not byte-identical databases.

## Integrations

- Mail: the app's mail session (`mail/MailSession`) sends to the copy's own SMTP sink (Mailpit, service `mail`, SMTP
  port 1025). Nothing leaves the host. Read the mails in the sink's web UI (URL printed by `up.sh`, or
  `docker compose -p <project> -f env/compose.yml port mail 8025`) or its API (`/api/v1/messages`).
- Everything else stays at the stock values of `sormas-base/setup/sormas.properties`, which leave all external
  systems off (SORMAS-to-SORMAS, SurvNet, geocoding, SMS, patient diary, external messages).
- Login: the built-in SORMAS login. No Keycloak.
- Vaadin runs in production mode (the stock setting).

## Logs and troubleshooting

- App: `docker logs <APP_ENV_CONTAINER>` (the Payara server log).
- Build: `env/.state/build/build.log`.
- Known, from the app and its stock setup (not changed here): deployment logs `SEVERE` lines (e.g. `@Lock is only
  permitted for singleton session beans`, annotation processing of `sormas-backend`), and
  `set server-config.ejb-container.max-pool-size=128` fails during the image build, so the EJB pool stays at Payara's
  default of 32 (`env/payara/payara-setup.sh` keeps the line as `server-setup.sh` has it). Neither stops the app.
