# sormas-e2e-playwright: end-to-end workflow tests

Browser tests of whole user workflows (log in, open a view, fill a form, save, check the result and the database),
written with [Playwright for Java](https://playwright.dev/java/) and JUnit 5. Each test class is one workflow of the
migration's workflow catalogue (`WF01` ... `WF62`); its test methods are the workflow's steps, in order. The tests drive
the built app in app copies started with the reproducible environment (`env/`, see [`env/README.md`](../env/README.md)),
one fresh copy per test class.

This module is **not part of the default build**: a plain `mvn install` in `sormas-base` neither builds nor runs it.
The Selenium/Cucumber suite `sormas-e2e-tests` is separate and unchanged.

## What is covered

The 21 critical workflows: WF01, WF03, WF04, WF08, WF11, WF12, WF13, WF16, WF17, WF19, WF20, WF21, WF28, WF31, WF38,
WF39, WF40, WF48, WF49 in configuration C1, and WF50, WF51 in C2 (Germany). The class name says which
(`WF03CreateCaseIT`), its display name gives the workflow's title, user and configuration.

## Requirements

- JDK 21 and Maven 3.9.
- Docker with Compose v2 and what `env/` needs ([`env/README.md`](../env/README.md), "Requirements"); about 3 GB of RAM
  per app copy.
- The app built for `env/`: run `env/build.sh` once (or `env/up.sh` for any copy). It needs your Vaadin licence key
  (`~/.vaadin/proKey` or `VAADIN_PRO_KEY`, see [`CI.md`](../CI.md)). The tests start their copies with
  `env/up.sh --no-build`, so they test what that build made; build again after changing the app.
- Chromium for Playwright 1.63.0, installed once: `npx playwright@1.63.0 install --with-deps chromium` (or
  `PLAYWRIGHT_BROWSERS_PATH` pointing at an existing install of that version). The tests never download a browser
  (`PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`).

## Run

```sh
env/build.sh                                         # once, and after each change to the app
mvn -f sormas-e2e-playwright/pom.xml verify          # all workflow tests, 2 app copies (ports 18720-18721)
mvn -f sormas-e2e-playwright/pom.xml verify -De2e.ports=18720-18723      # 4 copies, 4 classes at once
mvn -f sormas-e2e-playwright/pom.xml verify -Dit.test='WF03*'           # one workflow
cd sormas-base && mvn -Pe2e-playwright -pl ../sormas-e2e-playwright verify   # the same through the profile
```

Results: `target/failsafe-reports/` (one report per workflow); `target/e2e/<class>/` holds the downloads a workflow
checks (CSV exports, Word documents) and, for a failed step, a full-page screenshot of each open browser page;
`target/e2e/copies/<project>.log` the output of the env scripts per copy.

A failed step ends its workflow: the later steps of that class are skipped ("not reached"), as a user could not go on
either. After each step the test also fails on any console error, page error or HTTP response of 400 or more in the
browser.

Measured on a 10-CPU arm64 host (2026-11-12): see the PR that added this module; about 2 minutes per workflow on one
copy, mostly the copy's reset (about 25 s) and the workflow's own waits.

### Settings

System properties (`-D...`):

| Property | Default | What |
|---|---|---|
| `e2e.ports` | `18720-18721` | Ports of the app copies (`18720-18724` or `18720,18722`); one copy per port, as many test classes at once as copies |
| `e2e.project` | `sormas-e2e` | Name prefix of the copies: compose project `<prefix>-<port>`, containers `<prefix>-<port>-app`, `-db`, `-mail` |
| `e2e.keepCopies` | `false` | `true`: leave the copies running at the end (remove them with `env/down.sh` and the copy's `APP_ENV_PORT` and `APP_ENV_PROJECT`) |
| `e2e.reuseCopies` | `false` | `true`: take a running copy of the right kind (kept by an earlier run) with a reset instead of starting a new one. For writing tests: `-De2e.keepCopies=true -De2e.reuseCopies=true -Dit.test='WF03*'` reruns one workflow in about a minute |
| `e2e.headless` | `true` | `false`: show the browser |
| `e2e.envDir`, `e2e.appSrc` | `../env`, `..` | The env scripts and the source tree the test-user passwords are read from |

## How it works

- **App copies** (`support/AppCopyPool`): one slot per port. A test class asks for a copy of its kind (`@AppCopy`: C1
  or C2, and whether the default-password check is on, as WF01 needs). It gets an idle copy of that kind after
  `env/reset.sh` (restart reset: database back to the seed snapshot, fresh app JVM, mail sink emptied, uploaded
  documents removed), else a new copy (`env/up.sh --no-build`, already in the seed state), else an idle copy of another
  kind is replaced. Every class therefore starts on the seed data with a fresh database. Classes needing a rare kind run
  first (`support/KindOrder`).
- **Parallel:** as many classes at once as copies (`junit-platform.properties`, `support/CopiesParallelism`); the steps of
  a class run in order on one thread.
- **Browser** (`support/BrowserSession`): Chromium, window 2300 x 1300 (the case grid is wide), a real Chrome user agent
  (`HeadlessChrome` replaced). Each `newUi()` is its own browser context, so a workflow can act as two users.
- **Test users:** the default users of the test environment. Their passwords are read at run time from
  `sormas-api/.../DefaultEntityHelper.java` (`support/Credentials`) and never written into a test, a log or a report.
  `CfgUser` (C2) logs in with `NatUser`'s password.
- **Dates:** the app's "today" is the date in `Europe/Berlin` (the copies' time zone), taken when the class gets its
  copy. Workflows enter dates relative to it (`en(-3)`: three days ago as `M/D/YYYY`; `de(...)`: `DD.MM.YYYY` for C2).
  Seed records keep the dates of the snapshot (`env/seed/snapshot-<C>.json`), so checks of seed data use fixed dates.
- **Mail:** each copy's local mail sink (Mailpit, `support/MailSink`); nothing leaves the host. No integration is
  contacted.
- **Database checks:** `copy.sql("SELECT ...")` runs a query as the database superuser in the copy's database
  container (`docker exec ... psql`). Each workflow's last step checks the data the workflow's "Data checks" name.
- **Locators** (`support/Ui`): what a user sees first: buttons, links, tabs and menu items by role and name, fields by
  their label, texts by visible text. Vaadin component tags only where Flow gives no role or label (login form,
  `vaadin-dialog`, combo-box items, notifications, grid rows and cells, which are in the grid's shadow DOM and read with
  a script); a few CSS class names of the app on the dashboard (tile numbers, selected disease tab, map). No XPath, no
  generated ids; the directories' search boxes are found by the app's own ids (`caseLike`, `personLike`, ...).

## Add a workflow

1. Read the workflow text (`migration/workflows/WFxx-*.md` in the migration workspace): its steps, expected outcomes and
   data checks. A passing replay script of it, where there is one, shows how the app is driven.
2. Copy a test class of the same configuration, e.g. `WF03CreateCaseIT` (C1). Name it `WFxx<Title>IT`; set
   `@DisplayName` to "WFxx <title> (<users>, <configuration>)" and `@AppCopy`.
3. One `@Test @Order(n)` method per step, `@DisplayName("n. <step and expected outcome>")`. Log in in `@BeforeAll`
   (`ui = newUi(); ui.login("SurvSup");`) unless a step is about logging in.
4. Assert what the step expects, with a message naming the check. Use the helpers of `Ui`; add a helper there when a
   new kind of element is needed, not a selector in the test. Never assert generated ids (case ids, epid numbers) or
   the time of day.
5. Put the data checks in the last step (`copy.sql`), relative to the seed data (fixed short ids such as `SCAS01`).
6. Run it alone a few times (`-De2e.keepCopies=true -De2e.reuseCopies=true -Dit.test='WFxx*'`), then with the others.

Prepared files a workflow uploads go in `src/test/resources/workflows/` (synthetic data only).

## CI

The job "End-to-end workflow tests (Playwright)" of [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs this
suite on every pushed commit, next to the build job: it builds the app without tests and with Vaadin's
commercial-with-banner option (as the build job, decision E002 C; the artefacts are never kept), puts the artefacts
where `env/` takes them, installs Chromium, and runs `mvn -f sormas-e2e-playwright/pom.xml verify` with two copies. The
job summary lists the steps per workflow; on failure, the test reports, screenshots, downloads and the copies' server
logs are uploaded for 3 days. See [`CI.md`](../CI.md).
