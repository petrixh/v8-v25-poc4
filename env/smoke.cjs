// Login smoke for one app copy started with env/up.sh: each web user from env/smoke-users.json logs in and the first
// view loads with no console or page errors, no HTTP response >= 400 and no collapsed layout; users marked
// expectWebLoginRefused must be refused by the web login (no menu, the "no access to the web application" message).
// Usage: node env/smoke.cjs [outDir]
//   Base URL: APP_ENV_SMOKE_URL, else http://localhost:$APP_ENV_PORT/sormas-ui (APP_ENV_PORT default 18080, as env/).
//   outDir default: $APP_ENV_STATE_DIR/smoke/$APP_ENV_PROJECT (env/.state/smoke/sormas-old-env). Writes one screenshot
//   per user and smoke.json (results, no passwords). Exit 0 all pass, 1 a check failed, 2 setup error.
// Passwords are read at run time from DefaultEntityHelper.java and never printed or written.
// Needs Playwright for Node (1.63) with Chromium: a local or global npm install, or PW=<path to the module>.
'use strict';
const fs = require('fs');
const path = require('path');

const ENV_DIR = __dirname;
const APP_ROOT = path.dirname(ENV_DIR);
const HELPER = path.join(APP_ROOT, 'sormas-api/src/main/java/de/symeda/sormas/api/utils/DefaultEntityHelper.java');

function loadPlaywright() {
  if (process.env.PW) return require(process.env.PW);
  try { return require('playwright'); } catch (e) { if (e.code !== 'MODULE_NOT_FOUND') throw e; }
  const root = require('child_process').execSync('npm root -g', { encoding: 'utf8' }).trim();
  return require(path.join(root, 'playwright'));
}

(async () => {
  const cfg = JSON.parse(fs.readFileSync(path.join(ENV_DIR, 'smoke-users.json'), 'utf8'));
  const base = (process.env.APP_ENV_SMOKE_URL || `http://localhost:${process.env.APP_ENV_PORT || 18080}/sormas-ui`).replace(/\/+$/, '');
  const out = path.resolve(process.argv[2] || path.join(process.env.APP_ENV_STATE_DIR || path.join(ENV_DIR, '.state'),
    'smoke', process.env.APP_ENV_PROJECT || 'sormas-old-env'));
  fs.mkdirSync(out, { recursive: true });

  // Passwords: in memory only, passed to the password field and nowhere else. Messages are cut to their first line,
  // which never holds a typed value (Playwright puts call details on later lines).
  const pw = {};
  for (const m of fs.readFileSync(HELPER, 'utf8').matchAll(/Pair<>\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*\)/g)) pw[m[1]] = m[2];
  const missing = cfg.users.filter(u => !(u.username in pw)).map(u => u.username);
  if (missing.length) { console.error('SMOKE ERROR no default password in DefaultEntityHelper.java for ' + missing.join(', ')); process.exit(2); }

  const { chromium } = loadPlaywright();
  const browser = await chromium.launch();
  // Real-browser UA: Vaadin 8 serves the ES5 bundle to "HeadlessChrome" (migration quirk); use what Chrome would send.
  const probe = await browser.newPage();
  const userAgent = (await probe.evaluate(() => navigator.userAgent)).replace('HeadlessChrome', 'Chrome');
  await probe.close();
  const viewport = { width: 1400, height: 900 };
  const slug = s => String(s).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'x';
  const firstLine = e => String(e && e.message || e).split('\n')[0];
  const results = [];
  console.log(`smoke: ${base} (${cfg.users.length} users, Chromium ${browser.version()}, out: ${out})`);

  try {
    for (const user of cfg.users) {
      const refusedExpected = !!user.expectWebLoginRefused;
      // Fresh context per user: a shared one keeps the previous user's session cookie.
      const context = await browser.newContext({ viewport, userAgent });
      const page = await context.newPage();
      const errors = [], badResponses = [];
      page.on('pageerror', e => errors.push('pageerror: ' + firstLine(e)));
      page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text().split('\n')[0]); });
      page.on('response', r => { if (r.status() >= 400) badResponses.push(r.status() + ' ' + r.url()); });
      const r = { user: user.username, expect: refusedExpected ? 'refused' : 'first view', screenshot: path.join(out, slug(user.username) + '.png') };
      try {
        await page.goto(base + (cfg.loginPath || '/'));
        await page.fill(cfg.login.user, user.username);
        await page.fill(cfg.login.password, pw[user.username]);
        await page.click(cfg.login.submit);
        if (refusedExpected) {
          await page.getByText(cfg.refusedText).first().waitFor({ state: 'visible', timeout: 30000 });
          r.refusedMessage = true;
          r.menu = await page.locator(cfg.loggedInSelector).count();
          await page.screenshot({ path: r.screenshot });
          r.pass = r.menu === 0;
        } else {
          await page.waitForSelector(cfg.loggedInSelector, { state: 'visible', timeout: 60000 });
          await page.waitForTimeout(cfg.settleMs || 3000);
          r.view = new URL(page.url()).hash || '/';
          // Tallest scroll container (virtual-scrolling grids and tables excluded): catches collapsed layouts.
          r.maxScrollHeight = await page.evaluate(() => Math.max(document.body.scrollHeight, ...[...document.querySelectorAll('*')]
            .filter(e => !e.closest('.v-grid-scroller,.v-grid-tablewrapper,.v-grid-body,.v-table-body')).map(e => e.scrollHeight)));
          // The view scrolls inside its own container, so a full-page screenshot shows only the window: make the window
          // as tall as the content (at most the collapse limit) for the screenshot.
          await page.setViewportSize({ width: viewport.width, height: Math.min(Math.max(r.maxScrollHeight, viewport.height), (cfg.maxScrollFactor || 4) * viewport.height) });
          await page.waitForTimeout(1000);
          await page.screenshot({ path: r.screenshot, fullPage: true });
          r.pass = errors.length === 0 && badResponses.length === 0 && r.maxScrollHeight <= (cfg.maxScrollFactor || 4) * viewport.height;
        }
      } catch (e) {
        r.exception = firstLine(e);
        r.pass = false;
        await page.screenshot({ path: r.screenshot }).catch(() => {});
      }
      r.errors = errors;
      r.badResponses = badResponses;
      results.push(r);
      console.log(`${r.pass ? 'PASS' : 'FAIL'} ${user.username}: ${refusedExpected
        ? `web login refused=${!!r.refusedMessage} menu=${r.menu === undefined ? '?' : r.menu}`
        : `first view ${r.view || '?'} scroll=${r.maxScrollHeight || '?'}`} errors=${errors.length} http>=400=${badResponses.length}${r.exception ? ' exception: ' + r.exception : ''}`);
      for (const x of errors.concat(badResponses)) console.log('    ' + x);
      await context.close();
    }
  } finally {
    fs.writeFileSync(path.join(out, 'smoke.json'), JSON.stringify({ base, userAgent, results }, null, 2));
    await browser.close();
  }
  const failed = results.filter(r => !r.pass).length;
  console.log(`smoke: ${results.length - failed} of ${results.length} checks pass`);
  process.exit(failed ? 1 : 0);
})().catch(e => { console.error('SMOKE ERROR ' + String(e && e.message || e).split('\n')[0]); process.exit(2); });
