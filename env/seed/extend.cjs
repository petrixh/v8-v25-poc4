// Seed additions, UI part: what has no REST endpoint in the app is entered through its own UI, as a user would (the
// dev-mode generator is driven the same way, devmode.cjs). Run by env/seed/extend.sh after extend.py, not alone.
// Usage: node env/seed/extend.cjs <base-url> <settings-json> [screenshot-dir]
//   base-url e.g. http://localhost:18080/sormas-ui; settings-json as written by extend.sh:
//   {"config": "C1|C2", "snapshotDate": "YYYY-MM-DD", "pointOfEntry": "<name of the default point of entry>",
//    "region": "<default region>", "district": "<default district>", "community": "<default community>"}
// C1 and C2 (admin): "Import default continents / subcontinents / countries" (the app's own default lists, as the
//   buttons of the infrastructure views load them); the default point of entry set active.
// C2 only (features off in C1): campaign (admin); survey with one imported token, self report (CSV import), adverse
//   event report on immunization SIMM02 with an investigation (NatUser).
// Dates are fixed days relative to the snapshot date. Passwords are read at run time from DefaultEntityHelper.java and
// never printed or written. Needs Playwright for Node (1.63) with Chromium, found as env/smoke.cjs finds it.
'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');

const APP_ROOT = path.resolve(__dirname, '../..');
const HELPER = path.join(APP_ROOT, 'sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java');

function loadPlaywright() {
  if (process.env.PW) return require(process.env.PW);
  try { return require('playwright'); } catch (e) { if (e.code !== 'MODULE_NOT_FOUND') throw e; }
  const root = require('child_process').execSync('npm root -g', { encoding: 'utf8' }).trim();
  return require(path.join(root, 'playwright'));
}

const log = m => console.log(`extend: ${m}`);
const firstLine = e => String(e && e.message || e).split('\n')[0];

(async () => {
  const [base0, settingsFile, shots] = process.argv.slice(2);
  if (!base0 || !settingsFile) { console.error('usage: extend.cjs <base-url> <settings-json> [screenshot-dir]'); process.exit(2); }
  const base = base0.replace(/\/+$/, '');
  const s = JSON.parse(fs.readFileSync(settingsFile, 'utf8'));
  const pw = {};
  for (const m of fs.readFileSync(HELPER, 'utf8').matchAll(/Pair<>\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)/g)) pw[m[1]] = m[2];
  for (const u of ['admin', 'NatUser']) if (!(u in pw)) { console.error(`extend: no default password for ${u}`); process.exit(2); }

  // Day relative to the snapshot date, as dd.MM.yyyy (C2's date format, de-DE) or dd/MM/yyyy (the CSV import format)
  const day = (offset, sep = '.') => {
    const d = new Date(s.snapshotDate + 'T12:00:00Z'); d.setUTCDate(d.getUTCDate() + offset);
    return [String(d.getUTCDate()).padStart(2, '0'), String(d.getUTCMonth() + 1).padStart(2, '0'), d.getUTCFullYear()].join(sep);
  };
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'sormas-extend-'));

  const { chromium } = loadPlaywright();
  const browser = await chromium.launch();
  const probe = await browser.newPage();
  // Real-browser UA: Vaadin 8 serves the ES5 bundle to "HeadlessChrome"
  const userAgent = (await probe.evaluate(() => navigator.userAgent)).replace('HeadlessChrome', 'Chrome');
  await probe.close();
  let page;
  const errors = [];

  // Vaadin is idle when its loading indicator is hidden (inline display:none; see devmode.cjs)
  const idle = async (timeout = 60000) => {
    await page.waitForTimeout(300);
    await page.waitForFunction(() => {
      const e = document.querySelector('.v-loading-indicator');
      return !e || e.style.display === 'none';
    }, null, { timeout, polling: 250 });
  };
  const settle = async (ms = 600) => { await page.waitForTimeout(ms); await idle(); };
  const failIfError = async step => {
    const n = page.locator('.v-Notification-error, .v-Notification.error');
    if (await n.count()) throw new Error(`${step}: the app shows an error: ${(await n.first().innerText().catch(() => '')).split('\n')[0]}`);
  };
  const notification = () => page.locator('.v-Notification').first().innerText({ timeout: 10000 }).then(t => t.replace(/\s+/g, ' ').trim()).catch(() => '');
  const closeWindows = async () => { while (await page.locator('.v-window-closebox').count()) { await page.locator('.v-window-closebox').last().click(); await settle(300); } };
  const open = async route => { await page.goto(`${base}/#!${route}`); await settle(1500); await closeWindows(); };
  // Pick a combo box item by its exact text: type it (the suggestion popup opens on key strokes), click it
  const select = async (combo, text) => {
    const input = combo.locator('input.v-filterselect-input');
    await input.click(); await input.press('Control+a'); await input.press('Backspace');
    await input.pressSequentially(text, { delay: 30 });
    const item = page.locator('.v-filterselect-suggestpopup .gwt-MenuItem').filter({ hasText: new RegExp(`^\\s*${text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`) });
    await item.first().waitFor({ state: 'visible', timeout: 30000 });
    await item.first().click(); await idle();
    if ((await input.inputValue()).trim() !== text) throw new Error(`could not select "${text}"`);
  };
  const login = async user => {
    if (page) await page.context().close();
    page = await (await browser.newContext({ viewport: { width: 1600, height: 1000 }, userAgent })).newPage();
    page.on('pageerror', e => errors.push('pageerror: ' + firstLine(e)));
    page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text().split('\n')[0]); });
    await page.goto(base + '/login');
    await page.fill('#username', user);
    await page.fill('#password', pw[user]);
    await page.click("[id='Login.doLogIn']");
    await page.waitForSelector('.valo-menu', { state: 'visible', timeout: 60000 });
    await settle(1000);
    await closeWindows(); // C2: the GDPR consent popup, closed without consenting
  };
  // Upload a CSV in the open import window (Vaadin Upload: file input, then its "start import" button) and wait for the
  // result window; every row must be imported
  const importCsv = async (name, rows, expected) => {
    const file = path.join(tmp, name);
    fs.writeFileSync(file, rows.map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n') + '\n');
    await page.locator('.v-window .v-upload input[type=file]').setInputFiles(file);
    await page.locator('.v-window .v-upload .v-button').click();
    await resultWindow(name, expected);
  };
  const resultWindow = async (what, expected) => {
    const result = page.locator('.v-window').filter({ hasText: /\d+\/\d+/ }).last();
    await result.waitFor({ state: 'visible', timeout: 300000 });
    // "<processed>/<total> ..." counts up while the import runs
    let t = '', m = null;
    for (const t0 = Date.now(); Date.now() - t0 < 300000; await settle(1000)) {
      t = (await result.innerText()).replace(/\s+/g, ' ');
      m = t.match(/(\d+)\/(\d+)/);
      if (m && m[1] === m[2]) break;
    }
    await settle(1000);
    t = (await result.innerText()).replace(/\s+/g, ' ');
    m = t.match(/(\d+)\/(\d+)/);
    if (!m || m[1] !== m[2] || (expected && Number(m[2]) !== expected) || !/(^|\D)0 (Fehler|errors?)/i.test(t)) throw new Error(`${what}: import incomplete: ${t.slice(0, 200)}`);
    log(`${what}: ${m[2]} rows imported`);
    await closeWindows();
  };
  const shot = async n => { if (shots) { fs.mkdirSync(shots, { recursive: true }); await page.screenshot({ path: path.join(shots, n + '.png'), fullPage: true }).catch(() => {}); } };

  try {
    // --- C1 and C2: default continents, subcontinents and countries; the default point of entry active (admin) --------
    await login('admin');
    for (const [route, button] of [['continents', 'actionImportAllContinents'], ['subcontinents', 'actionImportAllSubcontinents'], ['countries', 'actionImportAllCountries']]) {
      await open('configuration/' + route);
      await page.click('#' + button); await settle();
      await page.click('.v-window #import-step-1');
      await resultWindow('default ' + route);
    }
    await open('configuration/pointsofentry');
    const poeRow = page.locator('.v-grid-body .v-grid-row').filter({ hasText: s.pointOfEntry });
    if (await poeRow.count() !== 1) throw new Error(`default point of entry "${s.pointOfEntry}" not found once in the list`);
    await poeRow.locator('.v-grid-cell').first().click(); await settle();
    const active = page.locator('.v-window #active input[type=checkbox]');
    if (await active.isChecked()) throw new Error('the default point of entry is already active');
    await page.locator('.v-window #active label').click(); await settle();
    if (!(await active.isChecked())) throw new Error('could not tick "active" of the default point of entry');
    await page.click('.v-window #commit'); await settle(1000);
    await failIfError('point of entry');
    log('default point of entry active');

    if (s.config === 'C2') {
      // --- campaign (admin: the only default user with the campaign edit right) ----------------------------------
      await open('campaign/campaigns');
      await page.click('#campaignNewCampaign'); await settle();
      const win = page.locator('.v-window').last();
      for (const [id, v] of [['startDate', day(-30)], ['endDate', day(30)]]) {
        const input = win.locator(`#${id} input`); await input.fill(v); await input.press('Tab'); await settle(300);
      }
      await win.locator('#name').fill('Seed campaign: vaccination drive');
      await win.locator('#description').fill('Synthetic seed campaign');
      await win.locator('#commit').click(); await settle(1500);
      await failIfError('campaign');
      log('campaign saved');

      // --- survey with one token, self report, adverse event with an investigation (NatUser) ---------------------
      await login('NatUser');
      await open('surveys');
      await page.click('#surveyNewSurvey'); await settle();
      await page.locator('.v-window #name').fill('Seed survey: symptom follow-up');
      await select(page.locator('.v-window #disease'), 'COVID-19');
      await page.click('.v-window #commit'); await settle(1500);
      await failIfError('survey');
      if (!/#!surveys\/data\//.test(page.url())) throw new Error('survey: its view did not open after saving');
      await page.click('#tab-surveys-surveytokens'); await settle(1500);
      await page.click('#actionImportSurveyTokens'); await settle();
      await importCsv('survey_tokens.csv', [['token'], ['SEEDTOKEN01']], 1);
      log('survey and token saved');

      await open('selfreports');
      await page.click('#actionImport'); await settle();
      const cols = [['SelfReport', 'type', 'CASE'], ['SelfReport', 'reportDate', day(-2, '/')], ['SelfReport', 'disease', 'CORONAVIRUS'],
        ['SelfReport', 'firstName', 'Juliett'], ['SelfReport', 'lastName', 'Seedperson'], ['SelfReport', 'sex', 'FEMALE'],
        ['SelfReport', 'birthdateDD', '12'], ['SelfReport', 'birthdateMM', '2'], ['SelfReport', 'birthdateYYYY', '1992'],
        ['Location', 'address.region', s.region], ['Location', 'address.district', s.district], ['Location', 'address.community', s.community],
        ['Location', 'address.city', 'Seedville'], ['Location', 'address.street', 'Example Street'], ['Location', 'address.houseNumber', '10'],
        ['Location', 'address.postalCode', '00000'], ['SelfReport', 'dateOfSymptoms', day(-4, '/')], ['SelfReport', 'comment', 'Synthetic seed self report']];
      await importCsv('self_report.csv', [0, 1, 2].map(i => cols.map(c => c[i])), 1);
      log('self report saved');

      // Adverse event report on SIMM02 (made by extend.py with one vaccination) and an investigation of it. Most AEFI
      // captions are English under the German UI; buttons and yes/no options are German.
      const tb = re => page.getByRole('textbox', { name: re }).first();
      const cap = re => page.locator('.v-caption').filter({ hasText: re }).first();
      const U = 'translate(normalize-space(),"abcdefghijklmnopqrstuvwxyz","ABCDEFGHIJKLMNOPQRSTUVWXYZ")';
      const opt = async (re, val) => { await cap(re).locator(`xpath=following::label[${U}="${val}"][1]`).click(); await settle(); };
      const fillDate = async (input, v) => { await input.fill(v); await input.press('Tab'); await settle(); };
      const choose = async (input, text) => {
        await input.locator('xpath=following::div[contains(@class,"v-filterselect-button")][1]').click(); await settle(800);
        await page.locator('.v-filterselect-suggestpopup').getByText(text, { exact: true }).first().click(); await settle(800);
      };
      const save = () => page.getByRole('button').filter({ hasText: /^Speichern$/ }).locator('visible=true').first();
      const suspectVaccine = async () => {
        await page.getByText('Select Suspect Vaccination', { exact: true }).click(); await settle(1500);
        const sw = page.locator('.v-window').last();
        await sw.getByText('COMIRNATY', { exact: true }).click(); await settle();
        await sw.getByRole('button').filter({ hasText: /^Speichern$/ }).click(); await settle(1200);
      };
      const VACC = -40; // vaccination date of SIMM02 in extend.py
      await open('immunizations/data/SIMM02-SEED00-SEED00-SEED00');
      await page.getByRole('button', { name: /new adverse event/i }).first().click(); await settle(3000);
      await fillDate(tb(/^date of report/i), day(-4));
      await tb(/^reporting id number/i).fill('SEED-AEFI-01');
      await suspectVaccine();
      await opt(/^Fever >38degC$/, 'YES');
      await fillDate(cap(/^date and time aefi started/i).locator('xpath=following::input[1]'), day(VACC + 1));
      await tb(/^describe aefi/i).fill('Synthetic seed adverse event: fever the day after vaccination');
      await opt(/^serious\*?$/i, 'JA');
      await choose(tb(/^reason for serious/i), 'Hospitalization');
      await opt(/^outcome\*?$/i, 'RECOVERED');
      await save().click();
      if (!/Adverse event saved/.test(await notification())) throw new Error('adverse event: not saved');
      await settle(2500);
      log('adverse event saved');
      await page.getByRole('button', { name: /new investigation/i }).first().click(); await settle(3000);
      await fillDate(tb(/^date of report/i), day(-3));
      await tb(/^investigation case id/i).fill('SEED-INV-01');
      await opt(/^place of vaccination/i, 'GOVERNMENT HEALTH FACILITY');
      await opt(/^vaccination in/i, 'ROUTINE');
      await fillDate(tb(/^date of investigation/i), day(-3));
      await fillDate(tb(/^date of filling this form/i), day(-3));
      await opt(/^this report is/i, 'FIRST');
      await suspectVaccine();
      await opt(/^type of site/i, 'FIXED');
      await fillDate(cap(/^date & time of first\/key symptom/i).locator('xpath=following::input[1]'), day(VACC + 1));
      await opt(/^status on the date of investigation/i, 'RECOVERED COMPLETELY');
      await opt(/^investigation status\*?$/i, 'DONE');
      await opt(/^classification of aefi\*?$/i, 'UNDETERMINED');
      await save().click();
      if (!/Adverse event investigation saved/.test(await notification())) throw new Error('adverse event investigation: not saved');
      await settle(2000);
      log('adverse event investigation saved');
    }
    await shot('extend');
  } catch (e) {
    await shot('extend-error');
    console.error('extend: FAILED ' + firstLine(e));
    for (const x of errors) console.error('    ' + x);
    await browser.close();
    fs.rmSync(tmp, { recursive: true, force: true });
    process.exit(1);
  }
  for (const x of errors) log('browser ' + x);
  await browser.close();
  fs.rmSync(tmp, { recursive: true, force: true });
  log('UI additions done');
})().catch(e => { console.error('extend: ERROR ' + firstLine(e)); process.exit(2); });
