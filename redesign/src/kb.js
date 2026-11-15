// 두벌식 키보드 마크업을 만든다. <div class="kb" data-sb="..." data-mode="keys|panel|edit"></div>
const ic = (name) => `<i data-lucide="${name}"></i>`;

const MARK = `<svg viewBox="0 0 100 100" width="22" height="22" fill="none" stroke="currentColor" stroke-width="11" stroke-linejoin="round" stroke-linecap="round">
  <path d="M31 29 C43 26 57 26 69 29 Q85 29 85 45 V55 Q85 71 69 71 C57 74 43 74 31 71 Q15 71 15 55 V45 Q15 29 31 29 Z"/></svg>`;

function keysHtml(opts = {}) {
  const r1 = "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ".split(""), h1 = "1234567890".split("");
  const r2 = "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ".split("");
  const r3 = "ㅋㅌㅊㅍㅠㅜㅡ".split("");
  const k = (c, sup = "") => `<div class="k">${c}${sup ? `<sup>${sup}</sup>` : ""}</div>`;
  return `<div class="keys">
    <div class="kr">${r1.map((c, i) => k(c, h1[i])).join("")}</div>
    <div class="kr" style="padding:0 16px">${r2.map(c => k(c)).join("")}</div>
    <div class="kr"><div class="k f w15">${ic("arrow-big-up")}</div>${r3.map(c => k(c)).join("")}<div class="k f w15">${ic("delete")}</div></div>
    <div class="kr"><div class="k f w15">?123</div>${k(",")}<div class="k space">두벌식</div><div class="k">.<span class="dots"><i></i><i></i><i></i></span></div><div class="k ent w15">${ic("corner-down-left")}</div></div>
  </div>`;
}

function symHtml() {
  const k = (c) => `<div class="k">${c}</div>`;
  return `<div class="keys">
    <div class="kr">${"1234567890".split("").map(k).join("")}</div>
    <div class="kr">${"@#₩%&*-+()".split("").map(k).join("")}</div>
    <div class="kr"><div class="k f w15">1/2</div>${["!", "\"", "'", ":", ";", "/", "?"].map(k).join("")}<div class="k f w15">${ic("delete")}</div></div>
    <div class="kr"><div class="k f w15" style="font-size:16px">가</div>${k(",")}<div class="k space">기호</div>${k(".")}<div class="k ent w15">${ic("corner-down-left")}</div></div>
  </div>`;
}

function smartbar(active) {
  return `<div class="sb">
    <div class="ic mk ${active ? "on" : ""}">${MARK}</div>
    <div class="ic">${ic("clipboard-paste")}</div>
    <div class="ic">${ic("smile")}</div>
    <div class="ic">${ic("quote")}</div>
    <div class="ic">${ic("move")}</div>
    <div class="sp"></div>
    <div class="ic">${ic("chevron-down")}</div>
  </div>`;
}

const TILES = [
  ["volume-2", "소리", true], ["vibrate", "진동", true], ["hand", "한손 모드", false], ["moon", "다크 모드", false],
  ["languages", "자판 전환", false], ["square-arrow-up", "키 팝업", true], ["quote", "상용구", false], ["move", "커서 이동", false],
];

function panelHtml() {
  return `<div class="qp">
    <div class="hd"><b>빠른 설정</b><div class="pill">${ic("pencil")}편집</div><div class="x">${ic("x")}</div></div>
    <div class="grid">${TILES.map(([i, l, on]) => `<div class="tile ${on ? "on" : ""}">${ic(i)}${l}</div>`).join("")}</div>
    <div class="slider">${ic("move-vertical")}<span>높이</span><div class="tr"><i style="width:55%"></i><em style="left:55%"></em></div><span class="v">105%</span></div>
    <div class="slider">${ic("volume-1")}<span>소리</span><div class="tr"><i style="width:35%"></i><em style="left:35%"></em></div><span class="v">35%</span></div>
  </div>`;
}

function editHtml() {
  const t = TILES.slice(0, 7);
  return `<div class="qp" style="gap:8px">
    <div class="hd"><b>빠른 설정 편집</b><div class="pill" style="background:var(--ink);color:#fff">완료</div></div>
    <div class="grid">${t.map(([i, l], idx) => {
      const cls = idx === 2 ? "tile lift" : "tile wig";
      return `<div class="${cls}">${ic(i)}${l}<span class="minus"></span></div>`;
    }).join("")}<div class="tile" style="background:transparent;outline:1.5px dashed var(--accent);outline-offset:-1.5px;color:var(--accent)">${ic("plus").replace("<i ", "<i style='color:var(--accent)' ")}빈 칸</div></div>
    <div class="tray"><small>더 넣을 수 있어요 · 끌어서 위로</small>
      <div class="chips">
        <div class="chip">${ic("plus")}테마 바꾸기</div><div class="chip">${ic("plus")}숫자 줄</div>
        <div class="chip">${ic("plus")}클립보드</div>
      </div>
    </div>
  </div>`;
}

function renderKeyboards() {
  document.querySelectorAll(".kb").forEach(el => {
    const mode = el.dataset.mode || "keys";
    let body = mode === "panel" ? panelHtml() : mode === "edit" ? editHtml() : mode === "sym" ? symHtml() : keysHtml();
    el.innerHTML = smartbar(mode === "panel" || mode === "edit") + body +
      `<div class="nav">${ic("chevron-down")}${ic("globe")}</div>`;
  });
  lucide.createIcons();
}

const STATUS = `<span>9:41</span><span class="r"><i data-lucide="signal"></i><i data-lucide="wifi"></i><i data-lucide="battery-full"></i></span>`;
document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".status").forEach(s => s.innerHTML = STATUS);
  document.querySelectorAll(".gesture").forEach(g => g.innerHTML = "<i></i>");
  renderKeyboards();
});
