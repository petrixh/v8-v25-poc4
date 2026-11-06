// Drive SORMAS's own dev-mode generator (Configuration > Developer, DevModeView; needs devmode=true and a user with the
// DEV_MODE right) on one app copy: set the generator's seed, then generate cases, contacts, events and samples, in order.
// Usage: node env/seed/devmode.cjs <base-url> <settings-json> [screenshot-dir]
//   base-url e.g. http://localhost:18080/sormas-ui; settings-json as written by env/seed/generate.sh:
//   {"user": "SurvSup", "seed": "<base 36, max 11 chars>", "disease": "COVID-19", "region": "<default region>",
//    "district": "<default district>", "laboratory": "<default laboratory>", "cases": 80, "contacts": 60, ...}
// The date ranges are the generator's own defaults: the 90 days up to today. Samples go to the laboratory, for external
// testing, shipped and received. Run by env/seed/generate.sh, not alone.
// The user's password is read at run time from DefaultEntityHelper.java and never printed or written.
// Needs Playwright for Node (1.63) with Chromium, found as env/smoke.cjs finds it (local, global, or PW=<module path>).
'use strict';
const fs = require('fs');
const path = require('path');

const APP_ROOT = path.resolve(__dirname, '../..');
const HELPER = path.join(APP_ROOT, 'sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java');

function loadPlaywright() {
  if (process.env.PW) return require(process.env.PW);
  try { return require('playwright'); } catch (e) { if (e.code !== 'MODULE_NOT_FOUND') throw e; }
  const root = require('child_process').execSync('npm root -g', { encoding: 'utf8' }).trim();
  return require(path.join(root, 'playwright'));
}

const log = m => console.log(`devmode: ${m}`);
const firstLine = e => String(e && e.message || e).split('\n')[0];

(async () => {
  const [base0, settingsFile, shots] = process.argv.slice(2);
  if (!base0 || !settingsFile) { console.error('usage: devmode.cjs <base-url> <settings-json> [screenshot-dir]'); process.exit(2); }
  const base = base0.replace(/\/+$/, '');
  const s = JSON.parse(fs.readFileSync(settingsFile, 'utf8'));
  const USER = s.user;
  const pw = {};
  for (const m of fs.readFileSync(HELPER, 'utf8').matchAll(/Pair<>\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)/g)) pw[m[1]] = m[2];
  if (!(USER in pw)) { console.error(`devmode: no default password for ${USER} in DefaultEntityHelper.java`); process.exit(2); }

  const { chromium } = loadPlaywright();
  const browser = await chromium.launch();
  const probe = await browser.newPage();
  // Real-browser UA: Vaadin 8 serves the ES5 bundle to "HeadlessChrome"
  const userAgent = (await probe.evaluate(() => navigator.userAgent)).replace('HeadlessChrome', 'Chrome');
  await probe.close();
  const page = await (await browser.newContext({ viewport: { width: 1600, height: 1000 }, userAgent })).newPage();
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + firstLine(e)));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text().split('\n')[0]); });

  // Vaadin is idle when its loading indicator is hidden (it shows during every server round trip). Vaadin hides it with
  // an inline display:none; the theme's CSS makes the computed display "block", so read the inline style.
  const idle = async (timeout = 60000) => {
    await page.waitForTimeout(300);
    await page.waitForFunction(() => {
      const e = document.querySelector('.v-loading-indicator');
      return !e || e.style.display === 'none';
    }, null, { timeout, polling: 250 });
  };
  const failIfError = async step => {
    const n = page.locator('.v-Notification-error, .v-Notification.error, .v-Notification-warning, .v-errorindicator:visible');
    if (await n.count()) throw new Error(`${step}: the app shows an error: ${(await n.first().innerText().catch(() => '')).split('\n')[0]}`);
  };
  // Pick a combo box item by its exact text: type it (the suggestion popup opens on key strokes, not on fill), click it
  const select = async (combo, text) => {
    const input = combo.locator('input.v-filterselect-input');
    if ((await input.inputValue()).trim() === text) return; // preset by the view (e.g. the region)
    await input.click();
    await input.press('Control+a');
    await input.press('Backspace');
    await input.pressSequentially(text, { delay: 30 });
    const item = page.locator('.v-filterselect-suggestpopup .gwt-MenuItem').filter({ hasText: new RegExp(`^\\s*${text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`) });
    await item.first().waitFor({ state: 'visible', timeout: 30000 });
    await item.first().click();
    await idle();
    if ((await input.inputValue()).trim() !== text) throw new Error(`could not select "${text}"`);
  };

  // Set a check box (Vaadin: click its label) and wait for the round trip
  const check = async (box, value) => {
    const input = box.locator('input[type=checkbox]');
    if ((await input.isChecked()) !== value) { await box.locator('label').click(); await idle(); }
    if ((await input.isChecked()) !== value) throw new Error('could not set a check box');
  };

  try {
    await page.goto(base + '/login');
    await page.fill('#username', USER);
    await page.fill('#password', pw[USER]);
    await page.click("[id='Login.doLogIn']");
    await page.waitForSelector('.valo-menu', { state: 'visible', timeout: 60000 });
    await idle();
    await page.goto(base + '/#!configuration/devMode');
    await page.waitForTimeout(2000);
    await idle();
    // The GDPR consent popup (C2) opens on every page load until a user consents: close it without consenting
    for (const box of await page.locator('.v-window .v-window-closebox').all()) { await box.click(); await idle(); }

    // The four generator rows (count, start date, end date, disease, region, district, Generate): cases, contacts,
    // events, samples. Found by structure, not by caption, so that the same script works in every language.
    const rows = page.locator('.v-horizontallayout:has(.v-datefield):not(:has(.v-horizontallayout .v-datefield))');
    await rows.first().waitFor({ state: 'visible', timeout: 60000 });
    if (await rows.count() !== 4) throw new Error(`expected 4 generator rows in DevModeView, found ${await rows.count()}`);

    // Seed: the text field and "use seed" check box, the first check box of the view (the seed is static in the app)
    const seedRow = page.locator('.v-horizontallayout:has(.v-checkbox):not(:has(.v-datefield))').first();
    await seedRow.locator('input.v-textfield').first().fill(s.seed);
    await seedRow.locator('input.v-textfield').first().press('Tab');
    await idle();
    const useSeed = seedRow.locator('.v-checkbox input[type=checkbox]').first();
    if (!(await useSeed.isChecked())) { await seedRow.locator('.v-checkbox label').first().click(); await idle(); }
    if (!(await useSeed.isChecked())) throw new Error('could not switch on the generator seed');
    log(`seed ${s.seed} (base 36) on`);

    const kinds = [['cases', s.cases], ['contacts', s.contacts], ['events', s.events], ['samples', s.samples]];
    for (let i = 0; i < kinds.length; i++) {
      const [kind, count] = kinds[i];
      const row = rows.nth(i);
      if (kind === 'samples') {
        // Third line: "external lab testing" on (switched off and on, so that the view sets the purpose to external and
        // shows the laboratory and the other check boxes: tests requested, additional tests, dispatched, received).
        // Second line: material (default blood), laboratory.
        const lines = row.locator('xpath=ancestor::div[contains(@class,"v-verticallayout")][1]').locator('.v-horizontallayout');
        const boxes = lines.nth(2).locator('.v-checkbox');
        await check(boxes.nth(0), true);
        await check(boxes.nth(0), false);
        await check(boxes.nth(0), true);
        await check(boxes.nth(3), true);
        await check(boxes.nth(4), true);
        await select(lines.nth(1).locator('.v-filterselect').nth(1), s.laboratory);
      }
      await row.scrollIntoViewIfNeeded();
      const countField = row.locator('input.v-textfield:not(.v-datefield-textfield)').first();
      await countField.fill(String(count));
      await countField.press('Tab');
      await idle();
      const combos = row.locator('.v-filterselect');
      await select(combos.nth(0), s.disease);
      await select(combos.nth(1), s.region);
      await select(combos.nth(2), s.district);
      const t = Date.now();
      await row.locator('.v-button').first().click();
      await idle(20 * 60000);
      await page.waitForTimeout(1000);
      await failIfError(kind);
      log(`${kind}: ${count} requested, done in ${Math.round((Date.now() - t) / 1000)} s`);
      // Close a notification so that the next row is not hidden behind it
      await page.locator('.v-Notification').click({ timeout: 2000 }).catch(() => {});
    }
    if (shots) { fs.mkdirSync(shots, { recursive: true }); await page.screenshot({ path: path.join(shots, 'devmode.png'), fullPage: true }); }
  } catch (e) {
    if (shots) { fs.mkdirSync(shots, { recursive: true }); await page.screenshot({ path: path.join(shots, 'devmode-error.png'), fullPage: true }).catch(() => {}); }
    console.error('devmode: FAILED ' + firstLine(e));
    for (const x of errors) console.error('    ' + x);
    await browser.close();
    process.exit(1);
  }
  for (const x of errors) log('browser ' + x);
  await browser.close();
  log('done');
})().catch(e => { console.error('devmode: ERROR ' + firstLine(e)); process.exit(2); });
