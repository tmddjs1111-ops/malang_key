const { chromium } = require('/opt/node-tools/node_modules/playwright');
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 411, height: 891 }, deviceScaleFactor: 2.25 });
  await p.goto('file://' + __dirname + '/settings.html');
  await p.evaluate(() => document.fonts.ready);
  const variants = [['0', 24, 14, 16], ['5', 20, 14 + 37*0.05, 16.8], ['10', 16, 14 + 37*0.10, 17.6], ['15', 12, 14 + 37*0.15, 18.4], ['20', 8, 14 + 37*0.20, 19.2]];
  for (const [n, m, v, h] of variants) {
    await p.evaluate(([m, v, h]) => { const s = document.documentElement.style; s.setProperty('--margin', m + 'px'); s.setProperty('--rowv', v + 'px'); s.setProperty('--rowh', h + 'px'); }, [m, v, h]);
    await p.screenshot({ path: `card_${n}.png` });
  }
  await b.close();
})();
