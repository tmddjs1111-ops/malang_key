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

// 1. 자막 띠·카드: cards.html의 등장 모션을 프레임마다 PNG로 찍는다 (마지막 프레임은 영상에서 그대로 유지)
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: W, height: H } });
async function card(i, kind, text, height, seconds) {
  const url = pathToFileURL(path.join(dir, 'cards.html')).href +
    `?kind=${kind}&text=${encodeURIComponent(text)}`;
  await page.goto(url);
  await page.evaluate(() => document.fonts.ready);
  await page.evaluate(() => window.fit());
  const frames = path.join(work, `${String(i).padStart(2, '0')}_${kind}`);
  fs.rmSync(frames, { recursive: true, force: true });
  fs.mkdirSync(frames);
  const count = Math.round(seconds * FPS);
  for (let f = 0; f < count; f++) {
    await page.evaluate(ms => window.seek(ms), (f * 1000) / FPS);
    await page.screenshot({ path: path.join(frames, `${String(f).padStart(3, '0')}.png`), clip: { x: 0, y: 0, width: W, height } });
  }
  return path.join(frames, '%03d.png');
}

// 화면 줌: cut.zoom = { at, dur, from: [배율, 중심x, 중심y], to: [...] } (좌표는 잘라낸 화면 기준)
// at초부터 dur초 동안 from → to로 부드럽게 바뀐다.
function zoomFilter(zoom, pw, ph) {
  if (!zoom) return '';
  const { at = 0, dur = 0.5, from, to } = zoom;
  const e = `(clip((t-${at})/${dur},0,1)*clip((t-${at})/${dur},0,1)*(3-2*clip((t-${at})/${dur},0,1)))`;
  const lerp = k => `(${from[k]}+(${to[k] - from[k]})*${e})`;
  const z = lerp(0);
  return `,scale=w='ceil(${pw}*${z}/2)*2':h='ceil(${ph}*${z}/2)*2':eval=frame` +
    `,crop=${pw}:${ph}:x='clip(${lerp(1)}*iw/${pw}-${pw}/2,0,iw-${pw})':y='clip(${lerp(2)}*ih/${ph}-${ph}/2,0,ih-${ph})'`;
}

// 2. 장면별 영상
const parts = [];
for (const [i, cut] of plan.cuts.entries()) {
  const out = path.join(work, `${String(i).padStart(2, '0')}.mp4`);
  if (cut.card) {
    const frames = await card(i, cut.card, cut.caption, H, cut.duration);
    ffmpeg(['-framerate', String(FPS), '-i', frames,
      '-vf', `format=yuv420p,fade=in:st=0:d=0.15`,
      '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', out]);
    cut.length = cut.duration;
  } else {
    const src = path.join(rawDir, `${cut.clip}.mp4`);
    if (!fs.existsSync(src)) throw new Error(`녹화 파일이 없어요: ${src}`);
    const speed = cut.speed || 1;
    cut.length = (cut.to - cut.from) / speed;
    // 자막이 바뀔 때만 자막 등장 모션, 녹화 파일이 바뀔 때만 화면 슬라이드인 (cut.enter로 직접 켜고 끔)
    const prev = plan.cuts[i - 1] || {};
    const frames = await card(i, 'caption', cut.caption, BAND, prev.caption === cut.caption ? 1 / FPS : 0.5);
    const enter = cut.enter ?? prev.clip !== cut.clip;
    const vh = H - BAND;
    // 기본은 화면 아래쪽 vh만큼. cut.crop = { y, h }이면 녹화 원본에서 그 구간을 잘라 vh 높이에 맞춘다
    // (입력창이 화면 위쪽에 있는 장면용).
    const pw = cut.crop ? Math.round((W * vh) / cut.crop.h / 2) * 2 : W;
    const panel = cut.crop
      ? `crop=iw:${cut.crop.h}:0:${cut.crop.y},scale=${pw}:${vh}`
      : `scale=${W}:-2,crop=${W}:${vh}:0:ih-${vh}`;
    const slide = enter ? `,format=rgba,fade=in:st=0:d=0.25:alpha=1` : '';
    const y = enter ? `${BAND}+90*pow(max(0,1-t/0.3),3)` : String(BAND);
    ffmpeg(['-ss', String(cut.from), '-t', String(cut.to - cut.from), '-i', src,
      '-framerate', String(FPS), '-i', frames,
      '-f', 'lavfi', '-i', `color=c=0xFBF1D4:s=${W}x${H}:r=${FPS}:d=${cut.length}`,
      '-filter_complex',
      `[0:v]setpts=(PTS-STARTPTS)/${speed},fps=${FPS},${panel}${zoomFilter(cut.zoom, pw, vh)}${slide}[p];` +
      `[2:v][p]overlay=x=(${W}-w)/2:y='${y}':eof_action=repeat[v];` +
      `[v][1:v]overlay=0:0:eof_action=repeat,format=yuv420p`,
      '-t', String(cut.length), '-an', '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', out]);
  }
  parts.push(out);
}
await browser.close();

// 3. 이어 붙이기
const list = path.join(work, 'list.txt');
fs.writeFileSync(list, parts.map(p => `file '${p}'`).join('\n'));
const final = path.join(outDir, `${name}.mp4`);
if (plan.bgm) {
  // 배경음악: plan.bgm = { file, start, volume, fadeOut } — file은 이 폴더 기준 경로
  const { file, start = 0, volume = 0.4, fadeOut = 1.5 } = plan.bgm;
  const bgm = path.join(dir, file);
  if (!fs.existsSync(bgm)) throw new Error(`배경음악 파일이 없어요: ${bgm}`);
  const video = path.join(work, 'video.mp4');
  ffmpeg(['-f', 'concat', '-safe', '0', '-i', list, '-c', 'copy', video]);
  const total = Number(execFileSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration',
    '-of', 'csv=p=0', video], { encoding: 'utf8' }));
  ffmpeg(['-i', video, '-ss', String(start), '-i', bgm,
    '-filter_complex', `[1:a]volume=${volume},afade=in:st=0:d=0.3,afade=out:st=${(total - fadeOut).toFixed(2)}:d=${fadeOut}[a]`,
    '-map', '0:v', '-map', '[a]', '-c:v', 'copy', '-c:a', 'aac', '-b:a', '192k', '-shortest',
    '-movflags', '+faststart', final]);
} else {
  ffmpeg(['-f', 'concat', '-safe', '0', '-i', list, '-c', 'copy', '-movflags', '+faststart', final]);
}

// 4. TTS 대사 타이밍 (CapCut에서 목소리를 얹을 때)
let t = 0;
const fmt = s => `${Math.floor(s / 60)}:${(s % 60).toFixed(1).padStart(4, '0')}`;
const lines = plan.cuts.map(c => { const l = `${fmt(t)}  ${c.tts}`; t += c.length; return l; });
fs.writeFileSync(path.join(outDir, `${name}_tts.txt`),
  `${lines.join('\n')}\n\n전체 ${t.toFixed(1)}초\n\n이어 읽기:\n${plan.cuts.map(c => c.tts).join(' ')}\n`);
console.log(`${final}  (${t.toFixed(1)}초)`);
