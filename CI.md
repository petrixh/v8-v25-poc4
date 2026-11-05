# Continuous integration

CI is one GitHub Actions workflow, [`.github/workflows/ci.yml`](.github/workflows/ci.yml). The workflows and Dependabot configuration inherited from the upstream SORMAS project were removed (commits `2b5b8b8` and `a56aef2`); they are not run here.

## What CI runs

On every push to any branch (there is no pull request trigger: a pushed commit's checks show on its PR), one job on `ubuntu-latest`:

1. **Skip if already green.** If this commit already has a successful run of this workflow (for example a branch created at a commit that was already checked, or a re-run of a green run), the job ends after this step.
2. Checkout, JDK 11 (Temurin), Maven dependency cache (Actions cache, keyed on the `pom.xml` files).
3. `mvn -B -ntp clean install` in `sormas-base` with the default profiles (the Android app profile `with-app` is not built). This compiles all modules and runs the unit tests of `sormas-api`, `sormas-backend`, `sormas-rest` and `sormas-ui`.
4. **Test report:** test counts per module go in the job summary. The job fails if `HistoryTablesTest` (the Testcontainers test that starts PostgreSQL in Docker) did not run and pass: without a usable Docker it is skipped silently. The workflow sets the Docker API version for Testcontainers (see below): the Ubuntu 24.04 runner (Docker 28) does not need it, the Ubuntu 26.04 runner (Docker 29) does, and `ubuntu-latest` moves to 26.04 in November 2026.
5. On failure only: the surefire reports are uploaded as a diagnostics artifact kept for 3 days. Nothing else is uploaded; the built war and ear files are not kept.

A smoke step (start the app and log in) will be added to the same job.

## Run the same locally

You need JDK 11, Maven 3.9 and a running Docker (for `HistoryTablesTest`).

```sh
# Docker 29 or later only accepts API 1.44 or later; Testcontainers 1.18.3 asks for 1.32 unless told:
echo "api.version=1.44" > ~/.docker-java.properties
cd sormas-base
mvn -B -ntp clean install
```

Then check that `HistoryTablesTest` ran and was not skipped: `sormas-backend/target/surefire-reports/TEST-de.symeda.sormas.backend.common.HistoryTablesTest.xml` shows `tests="1"` and `skipped="0"`.

`InfoProviderTest` reads the repository's git `origin`: it fails when `origin` is a local path, so run the build in a clone whose `origin` is the GitHub URL.
