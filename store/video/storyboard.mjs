// <이름>.json으로 스토리보드 한 장(output/<이름>_storyboard.png)을 만든다. 영상 만들기 전에 이걸로 확인한다.
// 실행: node store/video/storyboard.mjs quick_phrase   (playwright 필요)
import { chromium } from 'playwright';
import { fileURLToPath, pathToFileURL } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(fileURLToPath(import.meta.url));
const name = process.argv[2] || 'quick_phrase';
const plan = JSON.parse(fs.readFileSync(path.join(dir, `${name}.json`), 'utf8'));
const outDir = path.join(dir, 'output');
fs.mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1240, height: 800 } });
await page.goto(pathToFileURL(path.join(dir, 'storyboard.html')).href);
await page.evaluate(p => window.render(p), plan);
await page.evaluate(() => document.fonts.ready);
const file = path.join(outDir, `${name}_storyboard.png`);
await page.screenshot({ path: file, fullPage: true });
await browser.close();
console.log(file);
