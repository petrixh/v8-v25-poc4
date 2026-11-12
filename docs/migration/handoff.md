---
title: SORMAS on Vaadin 25: hand-off document
subtitle: What changed, how to deploy and run the migrated application, and what to know before go-live
kicker: Migration hand-off
version: Draft 1
date: 2026-11-12
status: Draft, written before the last round of fixes and before milestone M2 is passed
audience: Operators, administrators, developers and product owners of SORMAS
author: Migration team
header: SORMAS on Vaadin 25: hand-off
notice: Draft. Numbers and open points are as of 2026-11-12. This document is regenerated after the remaining fixes and tests. Open points are marked "TO CONFIRM".
---

[[TOC]]

# Executive summary {-}

<div class="exec">

**Outcome.** SORMAS v1.104.0 has been moved from Vaadin 8 on Java 11 / Payara 5 to Vaadin 25 on Java 21 / Payara 7 (Jakarta EE 11) with Hibernate 7.4, keeping what the application does. The migrated application was compared with the old one, step by step, on 62 user workflows in two configurations (default English, and Germany with optional features switched on): 61 behave the same and 1 differs in an agreed way (the link format in task notification e-mails). The data shown to each test user, list by list, is the same as in the old application (1,896 comparisons, 0 differences). The database schema is unchanged: no database migration script is needed. All 2,218 existing and added unit tests pass; no test was switched off.

**What changed.** The user interface is rebuilt on Vaadin 25. It keeps the SORMAS look and layout but is not pixel-identical: dialogs, drop-down menus, lists, date pickers, combo boxes and maps use the new components (section 1). Old `#!` links in bookmarks and earlier e-mails open the start page. The server platform, its libraries and several server settings change (sections 2 and 3). Three Vaadin commercial components are used (Rich Text Editor, Charts, Map): building the application needs a Vaadin subscription key.

**Risks to manage before go-live.**

1. Every Payara 7 server needs one new transport setting, or large pages stay blank with no error (section 3.3).
2. The stock setup script (`sormas-base/setup/server-setup.sh`) and the server documentation in `docs/` still install Payara 5 and ask for Java 11. They have not been updated yet (section 3.3).
3. Single sign-on through Keycloak is rebuilt but was never run against a Keycloak server (section 2.4).
4. Six database columns that may be empty would hide their records in the new application if any row has them empty. Check your database (section 4).
5. Rich text that uses formatting the new editor does not support loses it when edited and saved (section 1.3).
6. Chart image and PDF downloads send the chart to Highcharts' public export server, as before (section 2.5).

**Decisions needed.**

- Milestone M2 (functional parity) is waiting for the internal reviewer's approval.
- 43 deferred changes are waiting for review: accept each one, or ask for a fix (section 6.3).
- Where the migrated code ends up: merged into `main`, replacing it, or handed over as the `mig-dev` branch next to the old version on `main`.
- Who updates the production setup script and server documentation for Payara 7 and Java 21, and when.

</div>

## How to read this document {-}

Section 1 is for everyone who uses SORMAS. Sections 2 and 3 are for operators and administrators, section 7 for developers. Section 5 lists problems that the old application already had. They are still there, because the migration copies the old behaviour, defects included, unless a decision says otherwise. Section 6 lists the decisions behind the changes.

A separate document, the *final migration report*, describes how the migration was done and verified. It has the full verification results and the timeline.

Terms used here:

- **Old application:** SORMAS v1.104.0 as imported (branch `main`). **New application:** the migrated code (branch `mig-dev`).
- **C1** and **C2:** the two test configurations. C1 is the default country with English. C2 is Germany (German) with campaigns, adverse events following immunization (AEFI), environments, surveys, self reports, the GDPR notice and travel entries switched on.
- **Workflow:** one user task from the migration's catalogue of 62 (for example "create a new case for a new person"). Each was recorded on the old application and replayed on the new one.
- **Ref. B0nn:** the migration's reference number for a pre-existing problem (section 5). Quote it when you ask the migration team about it.

# What changed for users

This section lists the changes users can notice. It is written from the migration's register of material changes; changes that were later undone are left out. Where the new application deliberately behaves better than the old one, this is marked **Improvement**.

## Everyone: look and common controls

- **Look.** The application keeps the SORMAS colours (primary `#005A9C`, menu `#6691C4`), the Open Sans 12 px font, upper-case captions and buttons and the left menu. Controls have the shapes of the new Vaadin theme (Lumo): radio buttons, check boxes, lists, dialogs and the calendar look different. The left menu no longer collapses on narrow screens.
- **Pop-up windows** (forms, confirmations, warnings) are Vaadin 25 dialogs. As before, they are modal and movable, cannot be resized, have a close button and close with Escape, and do not close on a click outside. The header looks different. Keyboard focus stays inside the dialog while it is open.
- **Drop-down buttons** (Export, Import, New, dashboard period, map key and layers, epi-curve grouping, e-diary options) open a pop-over below the button. It closes on a click outside or Escape.
- **Lists** keep their columns, captions, order, sorting (click, shift-click for more columns), loading 100 rows at a time while scrolling, selection modes and double-click to open. Sort arrows now show in every sortable column header, not only the sorted one, so wide lists are a little wider. Tooltips on column headers are gone. Icons in list cells (edit, view, create case) are the new theme's icons.
- **Drop-down fields (combo boxes).** A value is cleared with the × button, not by picking an empty first entry. "Inaccessible" appears as grey placeholder text. Eight drop-downs that did not accept typing now accept typing to filter.
- **Date fields.** The display format is unchanged. A typed date must match that format; the old date field also accepted some other spellings. An unreadable date blocks Save with "Date format not recognized", as before. The calendar opens from its button. It shows month and weekday names and the first day of the week in the browser's language (German browser: Monday first, German names). It has new **Today** and **Cancel** buttons, captioned in German for German and in English for every other language until they are translated. Date filters without their own format show the short date (11/8/26 or 08.11.26) and read two-digit years from 80 years back to 20 years ahead, as before.
- **Field hints and messages.** Non-blocking hints (for example the Luxembourg national health id check, the follow-up-until hints, "admission date before symptom onset", the fever/temperature hint) appear as grey text under the field. In the old application they appeared on hover over an indicator. Error messages appear in red under the field. None of them blocks saving, as before.
- **Tooltips with formatted text** (automatic deletion date, "most relevant date" in list filters, previous case of a reinfection) show the same text with simpler formatting.
- **Notifications** keep the old timing: information notices close at the next user action, warnings 1.5 s after it, errors stay until clicked. Notifications have no close (×) icon. This is listed for review (section 6.3).
- **Refused Save.** The "Please check the input data" notification lists the missing fields in the order of the form. **Improvement:** the old application's order changed every time the form was opened.

## Links, addresses and logging in

- **Old links.** Addresses in the old format (`<server>/sormas-ui/#!cases/data/<id>`), in bookmarks and earlier notification e-mails, open the start page, not the record. New addresses are paths: `<server>/sormas-ui/cases/data/<id>`. Task notification e-mails sent by the new application use the new format.
- **Login page address.** When a page that needs a login is opened without one, the login page address keeps the requested page as `login?target=<page>` (old: `login#!<page>`). After login the user lands on that page.
- **After a logout**, a second user who logs in on the same login page lands on the first user's last page, as in the old application. The second user's own rights apply.
- **Addresses typed by hand.** A sub-page the user has no access to, or one that does not exist, shows "access denied" or "view not found". The old application silently opened the parent page instead.
- **Several tabs.** Opening the application in a new browser tab re-checks the pages the user may open, for all tabs of that login. A change of the user's rights then applies in every tab.
- **Rights changes.** A change to a user's roles applies at the user's next page change. In the old application it applied at the next login. Deactivating a logged-in user still applies at the next login.
- **A page that fails to open** shows the generic "An error has occurred" notification, as before, but the page area stays empty and the address bar shows the failed page. The old application kept showing the previous page.
- **Errors during page load.** An unexpected error from the server now always shows its notification. The old application sometimes showed nothing.

## Forms and data entry

- **Rich text fields** (action description and reply, disease configuration case definition text) use the Vaadin Rich Text Editor. The toolbar differs: it has undo/redo, bold/italic/underline/strike, text and background colour, headings H1 to H3, sub- and superscript, lists, indent, alignment, image, link, block quote, code block and clear formatting. Stored texts are unchanged until someone edits them. When a text is edited and saved, formatting the editor does not support is removed: font face, size and colour set with `<font>`, tables (the text stays), horizontal rules, headings H4 to H6, images linked by address (embedded images stay), `javascript:` links, scripts and event attributes. Links get `rel="nofollow"`. **Improvement:** scripts in rich text are removed.
- **Tables inside forms** (person addresses and contact details, previous hospitalisations, exposures, activities as case, vaccinations, AEFI vaccinations) use the new list component. Columns, the edit/view button, double-click to open and "new entry" are kept. Column widths follow the content.
- **Uploads.** CSV imports and document-template uploads use a new upload button that shows the file name and progress below it. Document uploads with several files save the files after all have uploaded, not one by one; the size limit, warnings and "file exists" prompts are unchanged.
- **Address map** in location forms (eye button next to the coordinates, only when geocoding is configured): the map opens in a pop-over below the button and closes on a click outside or Escape. Before, it closed when the mouse left it.
- **Custom date-and-time fields** (customizable fields of type "date and time", also their default value in configuration) show the date in the user's format and the time in the browser's format.
- **Statistics filters** (attribute values, jurisdiction, place of residence): values are picked in one drop-down and shown as removable chips. Picking a marked value again removes it. **Improvement:** a picked district, community or facility stays picked when the list is rebuilt. The old application could ignore it or show a district twice.
- **Campaign selector** (campaign data and statistics): "All campaigns" is chosen with the × button; it was an entry in the list.
- **Hospital informants creating a case.** Choosing a disease no longer shows "An unexpected error occurred". The rest of the disease handling now runs for them as for other users (for example influenza presets the classification to Confirmed). The preset facility is kept. **Improvement.**
- **Saved values that are no longer active** (archived region, district, facility and so on) show as "<name> (Inactive)", as before.
- **Values the application writes into locked (read-only) fields** are refused as in the old application: the action stops and "An unexpected error occurred" is shown. **Improvement:** two-part fields (user fields, date-and-time fields) showing a new value in their locked part no longer raise this error.

## Lists, exports and dashboards

- **Event directory for regional, district, community, facility and laboratory users.** Each event appears once and none is missing, and the count matches the rows. **Improvement:** the old application could list the same event several times and miss others (Ref. B064). The REST list endpoint for events pages the same way.
- **Self report basic export.** The "process" column is empty. The old application wrote internal object text into it. **Improvement.**
- **User roles export.** On the "User rights" sheet, the required rights are listed in a fixed order. **Improvement:** the old order changed with each server start.
- **Downloads** (exports, data dictionary, documents) keep their file names and types. Their internal addresses change, and the browser shows a link on hover.
- **Charts** (dashboards, statistics, campaigns, AEFI) are drawn by Vaadin Charts (Highcharts 12 inside; was Highcharts 8). Chart types, series, colours, labels, legends and export menu are kept. Fonts, tooltip and menu styling differ slightly. **Improvement:** keyboard navigation and screen-reader descriptions of charts.
- **Maps** (surveillance, contact, event, sample, AEFI and GIS dashboards, statistics map, address maps) are drawn by Vaadin Map (OpenLayers) with the same tile server, markers, clusters, polygons, clicks and full screen. Accepted differences: region and district names (and statistics values) do not show on hover; overlapping markers are not spread apart on click; markers of different kinds form separate clusters, and a cluster's number counts its markers; sample and AEFI markers are coloured squares; mouse-wheel zoom is finer (about 0.4 levels per notch); world maps open zoomed to the map's width.

## Who notices what, in short

| Who | Main changes |
|---|---|
| All users | New look of controls, dialogs, drop-down buttons, calendar; old `#!` links open the start page |
| Users typing dates | Strict date format; new Today/Cancel buttons |
| Users of rich text (actions, disease configuration) | New editor; unsupported formatting removed on save |
| Dashboard, statistics and map users | Charts and maps redrawn; no hover names on maps |
| Hospital informants | No error when choosing a disease for a new case |
| Regional and district users | Event directory without duplicates |
| Administrators | Role changes apply at the next page change |
| Users with bookmarks or old e-mails | Old links open the start page |

# What changed for operators and administrators

## Platform

| Part | Old | New |
|---|---|---|
| Java | 11 | 21 |
| Application server | Payara 5.2022.5 Full (Java EE 8, `javax.*`) | Payara Server 7 Community Full 7.2026.10 (Jakarta EE 11, `jakarta.*`) |
| Web UI framework | Vaadin 8.14.3 with the Vaadin 7 compatibility layer and a GWT widgetset | Vaadin Flow 25.3.5, production mode, CDI integration 16.1.2 |
| Persistence | Hibernate 5.6.15 | Hibernate ORM 7.4.14 |
| Second-level cache | Ehcache 2 | Ehcache 3.12 through JCache, same effective sizes (10,000 entries, 120 s) |
| Database | PostgreSQL 14 | PostgreSQL 14, schema unchanged |
| Topology | Backend EAR, REST war and UI war on one server | Unchanged |

Developers and integrators: code and plugins compiled against `sormas-api` must use `jakarta.*` imports. A CDI extension registered under the old `javax` service file name is no longer loaded.

The Android app was outside the migration: it was not built or tested. It uses the shared API module, which now uses `jakarta.*` packages, so it needs its own build and test.

## Server libraries and configuration in the application

- **Domain library folder.** The server libraries (`sormas-serverlibs`) change: Hibernate 7.4.14 with `hibernate-jcache` and Ehcache 3.12; `hibernate-types` removed; a `javax` XML Binding 2.3 set added for document generation (docx4j); `vaadin-themes` removed. An old domain library folder does not work with the new EAR and wars: replace it.
- **JSON columns** are written by Hibernate's Jackson mapper (set in `persistence.xml`). The stored JSON text is byte-identical to the old application's. The application fails at start if Jackson is not visible to Hibernate in the domain library folder, rather than writing a different format.
- **Bytecode enhancement off.** Hibernate 7's runtime enhancement is switched off in `persistence.xml`, so entities behave as with Hibernate 5. This logs four `HHH90000029` warnings at start (the settings are deprecated). A future Hibernate upgrade that removes the settings must keep enhancement off in another way.
- **Cache configuration** is in `sormas-backend` `META-INF/ehcache.xml` (Ehcache 3 format). The Ehcache provider is named in `persistence.xml`, because Payara 7 also ships another JCache provider.
- **Deprecation messages.** Payara 7 logs `glassfish-web.xml` as deprecated, at SEVERE level, on every deploy of the two wars. It is harmless today; a later Payara release may drop support.

## Database queries

The "changed since last shared with the reporting tool" and "last share date" filters (case and event directories) use a different subquery. The rows are the same; the query plan differs. Database administrators may see different plans for these filters.

## Security, login and REST

- **Web login check moved into the application.** The UI no longer relies on the server's URL security constraint. The application checks the web-access right on every page change and sends visitors without it to the login page. Background UI requests without a session are refused. Data access stays protected by the backend's right checks, unchanged. Reason: with the old layout, Payara 7 refused logged-in users on the application's root address.
- **REST rights.** The right to use the REST API, and the extra rights of the external visits, BAG export and SORMAS-to-SORMAS endpoints, are checked by SORMAS against the user's role rights, not by the server. The answers are the same (401 / 200 / 500 "Caller not in requested role", checked against the old application). A right removed by editing the database directly applies only after an application restart; changes made in the application apply at once.
- **Keycloak (single sign-on).** The login through Keycloak is rebuilt and unit-tested, but it was **never run against a Keycloak server** in the migration. The login page sends the browser to a new address `<ui>/oidc-login`, which starts the Keycloak login; `/Callback` and `/logout` are unchanged. A Keycloak user without the web UI right gets HTTP 403, as before. After login the start page opens. **If you use `authentication.provider=KEYCLOAK`, test login, logout and refusal before go-live.**
- **Unchanged weaknesses** of the old application are kept (section 5): `/metrics` answers without a login; no Content-Security-Policy, no HSTS, and the session cookie has no `Secure`/`SameSite` flags; the REST catch-all error answer contains the exception message.

## Outside services and network

- **Chart export server.** The chart menu's image and PDF downloads send the chart to Highcharts' public export server, now `export-svg.highcharts.com` (was `export.highcharts.com`). Print, CSV and XLS stay in the browser. If your firewall blocks the server, those two downloads fail; if your data must not leave your network, tell users not to use them, or remove the two menu entries in a follow-up.
- **Map tiles** come from the same tile server as before (OSM France HOT, or `map.tilersUrl`).
- **Build-time downloads** (build machines only): Node.js from nodejs.org and npm packages from registry.npmjs.org, pinned by the lock file, for the frontend build. The running application needs no Node.

## Monitoring and HTTP behaviour

| Request | Old | New |
|---|---|---|
| A UI address without a login | 302 to `/sormas-ui/login` | 200 (the start page, which shows the login at `/login`); `GET /sormas-ui/login` still answers 200 |
| An address that matches no page of the user | 200 (the page name was after `#!`) | 404 with the not-found or access-denied page |
| Opening an address whose page fails | 200 | 500 (no exception text shown) |
| Download addresses | Vaadin 8 connector addresses | Vaadin 25 dynamic resource addresses |

Uptime checks on `/sormas-ui/login` are not affected. Proxies or filters that match download or login addresses need checking.

## Performance and capacity

- **First page responses are larger**: about 4 to 12 times larger than the old application's for the same page (for example 556 KB against 45 to 61 KB for the symptoms form). Watch page load times on slow connections in the pilot.
- **Memory.** A short soak test (160 logins per application, 8 roles) showed no abnormal growth: the new application's heap grew less per login than the old one's. Its process memory ended 0.45 GB above its start (2.36 GB). Watch it in the pilot.
- **Sessions after logout.** Both applications keep the server session after logout until it times out (161 sessions after 161 logins). The session timeout limits it. This is old behaviour.
- **EJB pool.** The new test environment sets the EJB pool to 128, as `server-setup.sh` intends. The old test environment ran with Payara's default of 32, because that setting failed there without notice (Ref. B001). Check the setting on your server after setup.

# Environment and deployment

## Requirements

| Item | Requirement |
|---|---|
| Server runtime | Payara Server 7 Community Full 7.2026.10 on a Java 21 JDK |
| Database | PostgreSQL 14 (the old application's database is used as it is). Note: PostgreSQL 14 reaches end of support on 2026-11-12 (Ref. B006); an upgrade of the database server is a separate decision |
| Build | JDK 21, Maven 3.9, network access to Maven repositories, nodejs.org and registry.npmjs.org, and a Vaadin licence key |
| Memory | The test copies run with `-Xmx4096m` and use about 2.4 to 3 GB of process memory each |

## Commercial components and the licence key

The UI uses three Vaadin commercial components: Rich Text Editor, Charts and Map. They are licensed under the Vaadin Commercial License, not under SORMAS's GPL v3; the `README.md` starts with this notice. The production build of `sormas-ui` builds the frontend bundle and checks the licence. **Without a valid Vaadin key the build fails** ("Commercial features require a subscription").

- **Local and release builds:** the key is read from `~/.vaadin/proKey` or the environment variable `VAADIN_PRO_KEY`. In a container, pass the variable by name only (`docker run -e VAADIN_PRO_KEY ...`) or mount the key file read-only. Never write the key into an image, a repository file or a log.
- **CI builds** use Vaadin's "commercial with banner" option instead of a key. That build shows a banner and **must never be shipped or deployed**. CI keeps no build artefacts.
- Two further Vaadin libraries arrive with the UI framework (`collaboration-engine`, `vaadin-ai-core-flow`). Their licence terms were not checked: check them, or exclude them if unused [TO CONFIRM: listed for review].

## Setting up a Payara 7 server

**Open gap.** The stock setup script `sormas-base/setup/server-setup.sh` still downloads and sets up **Payara 5.2022.5**, and `docs/SERVER_SETUP.md`, `docs/SERVER_UPDATE.md` and `docs/DEVELOPMENT_ENVIRONMENT.md` still ask for Java 11. The only Payara 7 change in the script so far is the transport setting below. Until the script and documents are updated [TO CONFIRM: owner and date], use the settings of the test environment's Payara 7 domain. That domain is the one the migration verified (`env/payara/payara-setup.sh`, described in `env/README.md`, "Payara 7 domain"). If you deploy with the SORMAS Docker images, a separate project, they need the same changes.

Domain settings used and verified in the migration:

| Setting | Value |
|---|---|
| Server | Payara Server 7 Community Full 7.2026.10 on Temurin JDK 21 |
| **Transport write buffer** | `asadmin set configs.config.server-config.network-config.transports.transport.tcp.socket-write-buffer-size=1048576`, then restart. **Required.** Without it Payara 7 closes the connection on responses above the operating system's send buffer (about 680 KB on the test host): large pages (case data, symptoms, long lists) stay blank with no error shown, and the server log shows only `Connection is closed`. |
| Class loading | system property `fish.payara.classloading.delegate=false` (as before) |
| Pools | EJB `max-pool-size` 128; HTTP thread pool maximum 50 |
| JDBC | pool with `org.postgresql.ds.PGConnectionPoolDataSource` and PostgreSQL connection validation; resource `jdbc/sormasDataPool` |
| Mail | `mail/MailSession` (Payara 7 uses Eclipse Angus Mail) |
| Properties | custom resource `sormas/Properties` pointing to `sormas.properties` (the properties file itself is unchanged by the migration) |
| JVM | `-Xmx4096m`; logback configuration as before |
| Domain library folder | the new `sormas-serverlibs` jars and the PostgreSQL driver |
| Login realm | none, as before: SORMAS authenticates in the application |

Side effects of the transport setting: sockets ask for a 1 MiB send buffer (capped by the kernel's `net.core.wmem_max`), and up to 1.5 MiB of direct memory per HTTP thread is used while large responses are written.

## Upgrading an existing installation

An in-place upgrade of a production server was **not rehearsed** in the migration [TO CONFIRM: rehearse on a copy of production]. What the migration's checks support:

1. The database schema is identical (compared old against new on a fresh database), so the existing database is used as it is. Take a backup first, as for any release.
2. Before go-live, count rows with an empty value in the six columns listed in section 4.
3. Install Java 21 and Payara 7, create the domain with the settings above, and put the new server libraries in the domain library folder. Do not reuse the Payara 5 domain or its library folder.
4. Keep `sormas.properties`. Check that `ui.url` is still the UI's base address: task e-mails build their links from it.
5. Deploy the new EAR and the two wars, then check that the start page loads for a few users and that a large page (a case's symptoms) shows.

## Continuous integration

CI is one GitHub Actions workflow (`.github/workflows/ci.yml`, described in `CI.md`). It runs on every push to any branch, once per commit; there is no pull request trigger. It has two jobs side by side:

- **Build and test** (about 18 minutes): `mvn clean install` with JDK 21 runs all unit tests. It fails if the Testcontainers database test did not run. Then a smoke test starts one app copy and logs in as each web test user, checks the first page and the `/about` page by its address, and checks that a user without web rights is refused.
- **End-to-end workflow tests (Playwright)** (about 32 minutes for the current 21 workflows): builds the app without tests and runs the browser workflow suite (section 7.4) on two app copies.

Both jobs build with the commercial banner, never with a key. On failure only, diagnostics are kept for 3 days. The upstream SORMAS workflows and Dependabot were removed from this repository.

## Test environment scripts

`env/` starts reproducible test copies: Payara 7 on JDK 21, PostgreSQL 14, a local mail sink, and a synthetic data set from a committed snapshot, in configuration C1 or C2 (`env/up.sh`, `env/reset.sh`, `env/down.sh`; details in `env/README.md`). Several copies can run side by side. The copies use test-only settings: default users with default passwords, development data generation, the Europe/Berlin time zone. **These must never reach production.** The environment's own app build skips tests (CI runs them) and needs the Vaadin key by name.

# Database migration scripts

**None.** The migration does not change the database schema, so the upgrade needs no migration script beyond what SORMAS v1.104.0 already has.

How this was checked:

- The schema files (`sormas-backend/src/main/resources/sql/`, schema version 649) are unchanged. The old and new code have no other `.sql` change outside the test environment's own seed files (`git diff` of the migrated code against `main`).
- The schema created at start on a fresh database was dumped (`pg_dump -s`) for the old and the new application: identical, with equal object counts.
- Stored JSON text is byte-identical to the old application's.

What to check in your data before go-live. Hibernate 7 loads some required links with an inner join where Hibernate 5 used an outer join. If a record has an empty value in one of these six columns, it would no longer open in the new application. The test data has none:

`environments.reportingUser_id`, `selfreports.address_id`, `EventParticipant.person_id`, `campaigns.creatingUser_id`, `sormastosormassharerequest.originInfo_id`, `sharerequestinfo.sender_id`.

Count the rows where each is `NULL`. Where any exist, fix the data or ask for the mapping to be relaxed.

# Known pre-existing issues

These problems exist in the old application too. The migration keeps them, so that the new application behaves like the old one; fixing them is a separate decision. The Ref. column is the migration's reference number.

## To act on

| Ref. | Problem | What to know |
|---|---|---|
| B065 | Date-and-time fields (event start, sample collection, task, visit, treatment): a mistyped date part saves the record with the date-time empty, losing the previous value | Data loss. **To be fixed right after the migration** (decision 2026-11-12); a one-line fix exists |
| B050 | Case import template: the template says dates are `dd/MM/yyyy` but they are read as `M/D/YYYY` (05/11/2026 is imported as 11 May 2026) | Silent wrong dates on import; tell users or fix the template |
| B045 | A disease variant created for "All diseases" and used on a case breaks the case directory ("An unexpected error occurred") | Avoid such variants, or fix |
| B007 | Survey question and answer texts from outside data are shown as HTML without escaping (stored script injection) | Security; fix after the migration |
| B008 | Error messages, which can contain user input, are shown as HTML without escaping | Security; fix after the migration |
| B060, B061 | `/metrics` answers without login; no Content-Security-Policy, no HSTS, session cookie without `Secure`/`SameSite` | Close `/metrics` on public listeners; add the headers and cookie flags behind TLS |
| B018 | The REST catch-all error answer contains the exception message | Information disclosure |
| B056 | Chart options are built by joining captions and region or campaign names into script code: a quote in a name breaks the chart and could inject script | Security (minor); fix after the migration |
| B006 | Payara 5, free Vaadin 8 and Hibernate 5.6 were out of maintenance (resolved by the migration); PostgreSQL 14 support ends 2026-11-12 | Plan the database server upgrade |

## Errors users may see

| Ref. | Where | Problem |
|---|---|---|
| B025 | Case therapy and port health pages (some users) | Error notification on opening |
| B026 | Travel entry pages, C2 (administrator, lab officer, hospital informant) | Error notification on opening |
| B027 | Sample page and person page with a tested sample, hospital informants and other facility users | "Facility is not supported for environment samples" |
| B038 | Surveillance dashboard map, "Hide other countries" | "An error has occurred"; the same code path is kept (checked by reading the code, not re-run) |
| B046 | Hospital informant choosing a disease for a new case | Fixed by the migration (section 1.3) |
| B054, B055 | Participants of an archived event; pathogen test form when switching from Shigellosis | Error (found by reading code, not reproduced) |
| B057 | Travel entry create after the similar-persons warning | The dialog reopens titled "Create new immunization" |
| B064 | Event directory for jurisdiction users | Duplicates and missing events. Fixed by the migration (section 1.4) |

## Display and wording

- Successful saves of epidemiological and person data show a warning-styled "Warning: ... saved" (B029).
- The line listing window shows dates as M/D/YY (B031); later lines' inputs have no accessible names (B033).
- The case directory does not keep its sort order after navigating away and back (B032).
- Under German: untranslated or mixed captions, raw vaccine constants, a "?" in the AEFI header, "EXTERNALRESPONDENTID", M/D/YY on the dashboard custom period (B035, B036, B041, B042, B044).
- The case created from a COVID-19 travel entry has no disease (B034); a case from a self report shows "Case reference number: null" (B043).
- The samples dashboard counters read 0 after a period change until "Apply filters" (B037).
- Count labels show 0 on the areas and disease configuration lists (B063).
- After a merge the view still reads "1 potential duplicates detected" (B049).
- The quarantine order window stays open after the download (B051).
- A hospital informant's preset facility shows "(Inactive)" although it is active (B052).
- Reopening an exposure of type "Other" shows the activity details empty and required although stored (B048).
- The bulk progress window can show 0/N for several seconds (B030).

## Operations and tests

- About 37 SEVERE log lines from annotation scanning at each deploy (B002), a Log4j2 "could not find a logging implementation" line on each data dictionary export (B040), and Vaadin event-router warnings on dashboards (B047): log noise only.
- Hibernate schema validation would fail on one AEFI column (text against an integer mapping) in old and new alike; validation is off (B062).
- One backend test fails when it runs between 00:00 and 06:00 in the build machine's time zone (B059). Another fails rarely when a random id contains "555" (B066). Run CI builds outside 00:00 to 06:00 UTC or re-run. One test needs the git `origin` to be a URL (B003). The Testcontainers test is skipped silently without the Docker API version setting (B004; CI enforces it).
- Every event save also refreshes all its participants (change date, vaccination status, one history row) because of how the old date-time field worked; this is kept on purpose and could be cleaned up later.

# Accepted behaviour changes and their decisions

## Decided

"M0 D1" to "M0 D10" and "M0 P10a" to "P10c" are the numbers of the questions answered at the planning milestone (M0); "option A, B, C, K" are the answers chosen on the decision pages. The internal reviewer answered as the customer's representative.

| Change | Decision | By, when |
|---|---|---|
| Old `#!` links open the start page; no redirect | Accepted | Customer, 2026-11-05 |
| Task e-mail links use the new address format | Accepted (M0 D5) | Customer, 2026-11-06 |
| Unknown or forbidden sub-pages show "not found"/"access denied" instead of the parent page | Accepted (M0 D4) | Customer, 2026-11-06 |
| Survey-window and error-message HTML (B007, B008) kept as before | Kept for parity, fix later (M0 D1) | Customer, 2026-11-06 |
| Commercial components: Rich Text Editor, Charts, Map; licence notice in the README | Accepted (M0 D8, D10) | Customer, 2026-11-06 |
| Node.js and npm downloads for frontend builds | Allowed (M0 D9) | Customer, 2026-11-06 |
| Stored JSON stays byte-identical | Decided (M0 P10a) | Customer, 2026-11-06 |
| EJB pool 128 as SORMAS intends; fall back to 32 with a note if it cannot be set | Decided (M0 P10b). It was set: no fallback needed | Customer, 2026-11-06 |
| Notifications keep the Vaadin 8 timing where possible | Decided (M0 P10c). Kept; no notification type needed the exception | Customer, 2026-11-06 |
| Web login check in the application instead of the server constraint | Accepted (option B) | Customer, 2026-11-06 |
| CI builds with the commercial banner instead of a CI key | Accepted (option C) | Customer, 2026-11-07 |
| Global theme settings (palette, font, sizes, menu) | Accepted (option A) | Customer, 2026-11-12 |
| Map differences (hover names, marker spreading, clusters, square markers, finer zoom, world zoom); chart export menu kept, with the export server note | Accepted (A, A, K) | Customer, 2026-11-12 |
| Date-and-time fields losing a mistyped value (B065) | Kept now, fixed right after the migration | Customer, 2026-11-12 |
| Next user after a logout lands on the previous user's last page | Kept as in the old application | Migration team 2026-11-08; confirmed by customer 2026-11-12 |
| Code-set values that are not among a drop-down's options are ignored | Kept as in the old application | Migration team 2026-11-08; confirmed 2026-11-12 |
| Refused Save lists fields in form order | Improvement accepted | Migration team 2026-11-08; confirmed 2026-11-12 |
| Hospital informant person-page error (B027) | Kept as in the old application; fix offered after the migration | Migration team 2026-11-08; confirmed 2026-11-12 |

## Accepted as low impact during the migration

The look changes of dialogs, drop-down buttons, lists, table fields, uploads, the address map pop-over, statistics filter chips and the campaign selector (section 1) were recorded as low-impact changes when they were made. They are part of the review in section 6.3.

## Waiting for review

43 changes that earlier checkpoints deferred are listed for the customer's review, each with a suggestion: accept, check something before go-live, or ask for a fix. The review covers, among others:

- Hospital staff disease handling, the event directory fix, and the self report and user roles exports.
- The rich text formatting loss (suggestion: count affected texts before go-live) and the six empty-link columns.
- Two-digit years typed into date fields with their own format: they may be read into another century (suggestion: fix).
- Keycloak testing, rights changes at the next page change, and REST rights edited in the database.
- The `..` in the login target address (suggestion: small hardening fix), and the missing close icon on notifications (suggestion: fix).
- Read-only values shown as grey fields rather than plain text, and sort arrows in every column header (suggestion: fix both).
- No unsaved-changes warning on browser Back after typing an address (suggestion: fix).
- Larger pages and session memory; the Payara setting; library licences and security advisories (suggestion: check).

Status on 2026-11-12: all 43 are undecided [TO CONFIRM: regenerate with the answers].

# How to run and test

## Build

```sh
# JDK 21, Maven 3.9, a Vaadin key in ~/.vaadin/proKey or VAADIN_PRO_KEY
cd sormas-base
mvn clean install                  # all modules and all unit tests
mvn clean install -DskipTests      # without tests
```

The build compiles the backend EAR (`sormas-ear`), the REST and UI wars and the server libraries. The Android app profile is not part of it.

Unit tests need a running Docker for the Testcontainers test. With Docker 29 or later, set `api.version=1.44` in `~/.docker-java.properties` (see `CI.md`). One test reads the repository's git `origin` and fails when it is a local path.

## Start a test copy

```sh
env/up.sh                          # builds, starts C1: http://localhost:18080/sormas-ui/
APP_ENV_CONFIG=C2 APP_ENV_PORT=18081 APP_ENV_PROJECT=sormas-new-c2 env/up.sh --no-build
env/reset.sh                       # back to the seed data
env/down.sh                        # remove the copy
```

Test users are the application's default users (`admin`, `SurvSup`, `NatUser`, `SurvOff`, `LabOff`, `HospInf`, `PoeSup`, `NatClin`; in C2 also `CfgUser`). Their passwords are the built-in defaults in `sormas-api/.../DefaultEntityHelper.java`, for test copies only. `APP_ENV_PWCHECK=on` shows the default-password alert. `APP_ENV_MODE=dev` starts Vaadin development mode, for checks only.

## Unit tests

| Module | Old application | New application |
|---|---|---|
| `sormas-api` | 200 | 200 |
| `sormas-backend` | 1,730 (7 skipped) | 1,740 (7 skipped) |
| `sormas-rest` | 2 | 2 |
| `sormas-ui` | 136 (4 skipped) | 276 (4 skipped) |
| **Total** | **2,068, 11 skipped** | **2,218, 0 failed, 11 skipped** |

The 11 skipped tests are the same in both: they were already disabled in the old code. The migration added 150 tests and disabled, skipped, removed or commented out none.

## End-to-end workflow tests

The module `sormas-e2e-playwright` (Playwright for Java and JUnit 5) holds the migration's user workflows as browser tests. There is one test class per workflow and one ordered test per step, with database checks in the last step. Each class gets a freshly reset app copy started with `env/`. The module is off the default build. Status on 2026-11-12: the 21 critical workflows (107 steps) pass locally and in CI; the other 41 workflows are being added [TO CONFIRM: final count 62 classes].

```sh
env/build.sh                                            # once, and after app changes
mvn -f sormas-e2e-playwright/pom.xml verify             # all workflows, 2 app copies
mvn -f sormas-e2e-playwright/pom.xml verify -De2e.ports=18720-18723   # 4 copies at once
mvn -f sormas-e2e-playwright/pom.xml verify -Dit.test='WF03*'         # one workflow
```

Each copy needs about 3 GB of memory. All 21 workflows took 8 minutes with five copies on a 10-CPU machine. The module's `README.md` explains settings, how it works and how to add a workflow.

The older Selenium/Cucumber suite `sormas-e2e-tests` was left as it is. It was written for the old user interface and was neither run nor updated in the migration.

# Follow-up candidates

The migration kept a list of improvements it was not allowed to make, because they go beyond parity. These are the most valuable for a follow-up:

1. **Fix B065** (date-and-time fields losing a value): agreed for right after the migration.
2. **Update the server setup script and documentation** for Payara 7 and Java 21, including the transport setting (section 3.3).
3. **Security hardening:** close `/metrics` on public listeners; add Content-Security-Policy, HSTS and `Secure`/`SameSite` cookies behind TLS; escape the survey and error-message HTML (B007, B008); invalidate the session after a refused login and rotate the session id at login.
4. **Test Keycloak login** with a real Keycloak before go-live, if used.
5. **Report two Payara 7 defects upstream** (role checks fail after a remote EJB call; the root-address permission). Reproducers and issue drafts exist. Once Payara fixes them, the server-side role checks can return.
6. **Translations** of the new date-picker "Today" and "Cancel" buttons for the 53 languages other than English and German.
7. **Page size:** first responses are 4 to 12 times larger than before. Reducing them (fewer client calls per form) would help slow links.
8. **Look items:** hover names on map regions; the responsive (collapsing) menu under 1100 px; captions cut with an ellipsis in filter rows; the side panel moving under the form on narrow screens.
9. **Lenient date input:** accept extra date spellings in date fields, as the old date field did.
10. **CI:** shorten the backend tests (11 to 13 minutes of each run), add a concurrency group, pin image and action versions, and share the duplicated artefact steps of the two jobs.
11. **Logout:** send the browser to the plain login page after a logout, so the next user lands on their own start page (one line).
12. **Clean-ups:** a deprecated `glassfish-web.xml` (move to `payara-web.xml`), Vaadin and Hibernate APIs deprecated for removal, unused Valo theme files, and the README's upstream CI badge.

# Side-by-side recordings

A side-by-side walkthrough of every workflow, old and new application, comes with the final hand-off. It is built from a same-day run of both applications on the final code. It is not part of this draft.

# Appendix A. Open points in this draft

- Milestone M2 is not yet passed; this draft is written from the code at the M2 checkpoint plus the hand-off work so far.
- The 43 deferred changes (section 6.3) are undecided; fixes that come from them will change sections 1 and 6.
- The remaining 41 workflow tests are being added; section 7.4 gives the current count.
- The production setup script and the server documentation are not yet updated for Payara 7 and Java 21 (section 3.3).
- An in-place upgrade of an existing installation has not been rehearsed (section 3.4).
- The licence terms of two bundled Vaadin libraries are unchecked (section 3.2).
- Where the migrated code ends up (`main` or the `mig-dev` branch) is undecided.
