// screenshots.html의 섹션 5개를 Play 스토어용 PNG(1080×1920)로 저장한다.
// 실행: node store/screenshots/render.mjs   (playwright 필요: npm i -D playwright 후 npx playwright install chromium)
import { chromium } from 'playwright';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';

const dir = path.dirname(fileURLToPath(import.meta.url));
const names = ['01_theme', '02_layouts', '03_sound', '04_insta_font', '05_quick_phrase'];

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1200, height: 2000 } });
await page.goto(pathToFileURL(path.join(dir, 'screenshots.html')).href);
await page.evaluate(() => document.fonts.ready);

for (let i = 0; i < names.length; i++) {
  await page.locator(`#s${i + 1}`).screenshot({ path: path.join(dir, `${names[i]}.png`) });
  console.log(`${names[i]}.png`);
}
await browser.close();
