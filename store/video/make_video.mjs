// 녹화한 화면(raw/*.mp4)을 쇼츠 영상(1080×1920)으로 편집한다.
// 실행: node store/video/make_video.mjs quick_phrase   (playwright, ffmpeg 필요)
//
// 장면 순서·자르는 구간·배속·자막은 <이름>.json에 있다. 화면 아래쪽(키보드와 입력창)을 잘라
// 자막 띠 아래에 놓는다. 결과: output/<이름>.mp4, output/<이름>_tts.txt (TTS 대사 타이밍)
import { chromium } from 'playwright';
import { execFileSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const dir = path.dirname(fileURLToPath(import.meta.url));
const name = process.argv[2] || 'quick_phrase';
const plan = JSON.parse(fs.readFileSync(path.join(dir, `${name}.json`), 'utf8'));
const rawDir = process.env.RAW_DIR || path.join(dir, 'raw');
const outDir = path.join(dir, 'output');
const work = path.join(outDir, `${name}_parts`);
fs.mkdirSync(work, { recursive: true });

const W = 1080, H = 1920, BAND = 420, FPS = 30;
const ffmpeg = args => execFileSync('ffmpeg', ['-hide_banner', '-loglevel', 'error', '-y', ...args], { stdio: 'inherit' });

// 1. 자막 띠·카드 PNG
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: W, height: H } });
async function card(i, kind, text, height) {
  const url = pathToFileURL(path.join(dir, 'cards.html')).href +
    `?kind=${kind}&text=${encodeURIComponent(text)}`;
  await page.goto(url);
  await page.evaluate(() => document.fonts.ready);
  const file = path.join(work, `${String(i).padStart(2, '0')}_${kind}.png`);
  await page.screenshot({ path: file, clip: { x: 0, y: 0, width: W, height } });
  return file;
}

// 2. 장면별 영상
const parts = [];
for (const [i, cut] of plan.cuts.entries()) {
  const out = path.join(work, `${String(i).padStart(2, '0')}.mp4`);
  if (cut.card) {
    const png = await card(i, cut.card, cut.caption, H);
    ffmpeg(['-loop', '1', '-t', String(cut.duration), '-i', png,
      '-vf', `fps=${FPS},format=yuv420p,fade=in:st=0:d=0.2`,
      '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', out]);
    cut.length = cut.duration;
  } else {
    const src = path.join(rawDir, `${cut.clip}.mp4`);
    if (!fs.existsSync(src)) throw new Error(`녹화 파일이 없어요: ${src}`);
    const png = await card(i, 'caption', cut.caption, BAND);
    const speed = cut.speed || 1;
    const vh = H - BAND;
    ffmpeg(['-ss', String(cut.from), '-t', String(cut.to - cut.from), '-i', src, '-i', png,
      '-filter_complex',
      `[0:v]setpts=(PTS-STARTPTS)/${speed},fps=${FPS},scale=${W}:-2,` +
      `crop=${W}:'min(ih,${vh})':0:'ih-min(ih,${vh})',` +
      `pad=${W}:${H}:0:${BAND}:color=0xFBF1D4[v];[v][1:v]overlay=0:0,format=yuv420p`,
      '-an', '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', out]);
    cut.length = (cut.to - cut.from) / speed;
  }
  parts.push(out);
}
await browser.close();

// 3. 이어 붙이기
const list = path.join(work, 'list.txt');
fs.writeFileSync(list, parts.map(p => `file '${p}'`).join('\n'));
const final = path.join(outDir, `${name}.mp4`);
ffmpeg(['-f', 'concat', '-safe', '0', '-i', list, '-c', 'copy', '-movflags', '+faststart', final]);

// 4. TTS 대사 타이밍 (CapCut에서 목소리를 얹을 때)
let t = 0;
const fmt = s => `${Math.floor(s / 60)}:${(s % 60).toFixed(1).padStart(4, '0')}`;
const lines = plan.cuts.map(c => { const l = `${fmt(t)}  ${c.tts}`; t += c.length; return l; });
fs.writeFileSync(path.join(outDir, `${name}_tts.txt`),
  `${lines.join('\n')}\n\n전체 ${t.toFixed(1)}초\n\n이어 읽기:\n${plan.cuts.map(c => c.tts).join(' ')}\n`);
console.log(`${final}  (${t.toFixed(1)}초)`);
