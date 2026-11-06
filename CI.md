# Continuous integration

CI is one GitHub Actions workflow, [`.github/workflows/ci.yml`](.github/workflows/ci.yml). The workflows and Dependabot configuration inherited from the upstream SORMAS project were removed (commits `2b5b8b8` and `a56aef2`); they are not run here.

## What CI runs

On every push to any branch (there is no pull request trigger: a pushed commit's checks show on its PR), one job on `ubuntu-latest`:

1. **Skip if already green.** If this commit already has a successful run of this workflow (for example a branch created at a commit that was already checked, or a re-run of a green run), the job ends after this step.
2. Checkout, JDK 21 (Temurin), Maven dependency cache (Actions cache, keyed on the `pom.xml` files).
3. `mvn -B -ntp clean install` in `sormas-base` with the default profiles (the Android app profile `with-app` is not built). This compiles all modules and runs the unit tests of `sormas-api`, `sormas-backend`, `sormas-rest` and `sormas-ui`, and builds the `sormas-ui` frontend in production mode, which needs a Vaadin licence key (see "Vaadin licence key" below).
4. **Test report:** test counts per module go in the job summary. The job fails if `HistoryTablesTest` (the Testcontainers test that starts PostgreSQL in Docker) did not run and pass: without a usable Docker it is skipped silently. The workflow sets the Docker API version for Testcontainers (see below): the Ubuntu 24.04 runner (Docker 28) does not need it, the Ubuntu 26.04 runner (Docker 29) does, and `ubuntu-latest` moves to 26.04 in November 2026.
5. **Smoke**, on the artefacts this build just made (see "Smoke" below): start one app copy with `env/`, log in as each web test user, stop the copy. **Non-blocking until migration ticket 1a-91** (see below).
6. On failure only: the surefire reports, the smoke screenshots and results and the app's server log are uploaded as a diagnostics artifact kept for 3 days. Nothing else is uploaded; the built war and ear files are not kept.

## Vaadin licence key

`sormas-ui` uses a Vaadin commercial component, the Rich Text Editor (see the notice at the top of [`README.md`](README.md)). Vaadin's prebuilt frontend bundle does not contain it, so the production build of `sormas-ui` (`vaadin-maven-plugin` goal `build-frontend`) builds the bundle itself (it downloads Node to `~/.vaadin` and the npm packages) and checks the commercial licence. Without a valid key it stops with `Commercial features require a subscription` and the build fails.

- **CI:** the build step gets the key from the repository secret `VAADIN_PRODMODE_CI_SERVER_KEY` as the environment variable `VAADIN_OFFLINE_KEY`. It is a Vaadin CI server key, valid for production-mode builds only. The workflow never prints it; when the secret is missing (for example in a fork) the step logs a warning and the build then fails at the licence check.
- **Locally:** the build reads your Vaadin pro key from `~/.vaadin/proKey` or the environment variable `VAADIN_PRO_KEY`. When Maven runs in a container, pass the variable by name only (`docker run -e VAADIN_PRO_KEY ...`) or mount the key file read-only; never write the key into an image, a file in the repository or a log.
- Vaadin also offers a watermarked build without a key (`-Dvaadin.commercialWithBanner`); this project does not use it: the banner it shows can get in the way of clicks.

## Smoke

**Non-blocking during migration stage 1a, until ticket 1a-91.** Since ticket 1a-22, `env/` runs Payara 7.2026.10 (Jakarta EE 11) on JDK 21. The app is still being ported to Jakarta EE during stage 1a and does not deploy on Payara 7 until ticket 1a-91: the app does not come up, so the smoke fails. Every smoke step has `continue-on-error: true`, the start waits 5 minutes instead of 15 (`APP_ENV_WAIT_S=300`), the logins run only when the start succeeded, and the step "Smoke: result" reports the outcome as a warning and in the job summary. A green job therefore says nothing about the smoke until then. Ticket 1a-91 (app built and deployed on Payara 7) makes the smoke blocking again.

The smoke steps use the reproducible environment ([`env/README.md`](env/README.md)) with its defaults (port 18080, project `sormas-new-env`):

1. **Artefacts and image from this build.** `env/up.sh --no-build` starts from the image `sormas-new-env-payara:7.2026.10` and the files in `env/.state/build/deploy`, which `env/build.sh` normally makes from its own Maven build. So that the app is not built a second time, this step copies the ear, the two wars and the setup files of this build there, collects the server libraries (`mvn dependency:copy-dependencies` on `sormas-serverlibs`), downloads the Payara zip from Maven Central (sha1 checked) and builds the image from `env/payara/`, as `env/build.sh` does.
2. Node 22, Playwright for Node 1.63.0 (npm) and its Chromium (`playwright install --with-deps chromium`).
3. `env/up.sh --no-build`: PostgreSQL 14, the mail sink and Payara with the app, in the default test configuration C1; restores the committed C1 seed snapshot (`env/seed/snapshot-C1.pgdump`) and takes the reset point (about 2 minutes, 105 to 121 s measured in CI).
4. `node env/smoke.cjs env/.state/smoke`: `admin`, `SurvSup`, `NatUser`, `SurvOff`, `LabOff`, `HospInf`, `PoeSup` and `NatClin` each log in to the web UI; the first view must load with no console or page error, no HTTP response of 400 or more, and no collapsed layout. `PoeInf` must be refused (its role has no web UI right). The passwords are read from `DefaultEntityHelper.java` at run time and are not written to any file, log or uploaded diagnostic.
5. Always, also after a failure or a cancel once the app was started: the app's server log is saved to `env/.state/smoke/app.log`, then `env/down.sh` removes the copy.

The smoke uses a real-browser user agent (`HeadlessChrome` replaced with `Chrome`): Vaadin 8 serves the old ES5 client bundle to `HeadlessChrome`.

## Run the same locally

You need JDK 21, Maven 3.9, a running Docker (for `HistoryTablesTest`) and a Vaadin licence key (see "Vaadin licence key").

```sh
# Docker 29 or later only accepts API 1.44 or later; Testcontainers 1.18.3 asks for 1.32 unless told:
echo "api.version=1.44" > ~/.docker-java.properties
cd sormas-base
mvn -B -ntp clean install
```

Then check that `HistoryTablesTest` ran and was not skipped: `sormas-backend/target/surefire-reports/TEST-de.symeda.sormas.backend.common.HistoryTablesTest.xml` shows `tests="1"` and `skipped="0"`.

For the smoke, start a copy and run the script against it (Node 22 and Playwright 1.63 with Chromium; see `env/README.md`, "Smoke check"):

```sh
env/up.sh --no-build
node env/smoke.cjs
env/down.sh
```

`InfoProviderTest` reads the repository's git `origin`: it fails when `origin` is a local path, so run the build in a clone whose `origin` is the GitHub URL.
