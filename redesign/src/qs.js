// 빠른 설정 시안 공통 조각
const I = (n, extra = "") => `<i data-lucide="${n}" ${extra}></i>`;

function statusBar() {
  return `<div class="status"><span>9:41</span><span style="display:flex;gap:4px">${I("signal", 'style="width:14px;height:14px"')}${I("wifi", 'style="width:14px;height:14px"')}${I("battery-full", 'style="width:14px;height:14px"')}</span></div>`;
}

// step: 1~5, done: 테스트까지 끝낸 단계 번호 목록
function wizTop(step, title, one, opts = {}) {
  const bars = [1, 2, 3, 4, 5].map(n => {
    const cls = [n <= step ? "on" : "", (opts.done || []).includes(n) ? "done" : ""].join(" ");
    return `<i class="${cls}"></i>`;
  }).join("");
  const more = opts.detail
    ? `<div class="more">자세히 접기 ${I("chevron-up")}</div><div class="detail">${opts.detail.map(([ic, t]) => `<div>${I(ic)}<span>${t}</span></div>`).join("")}</div>`
    : (opts.noMore ? "" : `<div class="more">자세히 ${I("chevron-down")}</div>`);
  const alert = opts.alert ? `<div class="alert">${I("bell-off")}<span>${opts.alert}</span></div>` : "";
  return `<div class="top">
    <div class="meta"><span>${step} / 5</span><em>건너뛰기</em></div>
    <div class="prog">${bars}</div>
    <h2>${title}</h2>
    <div class="one">${one}</div>
    ${more}${alert}
  </div>`;
}

function mission(text, sub, cnt, done = false) {
  return `<div class="mission ${done ? "done" : ""}">
    <div class="tgt">${I(done ? "check" : "target")}</div>
    <div class="tx">${text}${sub ? `<small>${sub}</small>` : ""}</div>
    ${cnt ? `<span class="cnt">${cnt}</span>` : ""}
  </div>`;
}

function testbar(text, ph = false) {
  return `<div class="testbar"><div class="in ${ph ? "ph" : ""}">${text}${ph ? "" : '<span class="cur"></span>'}</div></div>`;
}

function strip(label, items) {
  return `<div class="strip"><span class="lab" style="background:none;border:none;padding:0">${label}</span>${items.map(([t, on, ic]) => `<span class="${on ? "on" : ""}">${ic ? I(ic) : ""}${t}</span>`).join("")}</div>`;
}

const SB_DEFAULT = ["clipboard-paste", "copy", "square-dashed", "smile", "chevron-left", "chevron-right"];

function smartbar(icons = SB_DEFAULT, newIdx = -1) {
  return `<div class="sb">${icons.map((n, i) => `<div class="${i === newIdx ? "new" : ""}">${I(n)}</div>`).join("")}</div>`;
}

// o: { sb, sbNew, space, glow, font }
function keyboard(o = {}) {
  const k = (c, sup = "", cls = "") => `<div class="k ${cls}" ${o.font ? `style="font-family:${o.font}"` : ""}>${c}${sup ? `<sup>${sup}</sup>` : ""}</div>`;
  const r1 = "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ".split(""), h = "1234567890".split("");
  return `<div class="kb">
    ${o.noSb ? "" : smartbar(o.sb, o.sbNew)}
    <div class="keys">
      <div class="kr">${r1.map((c, i) => k(c, h[i])).join("")}</div>
      <div class="kr" style="padding:0 15px">${"ㅁㄴㅇㄹㅎㅗㅓㅏㅣ".split("").map(c => k(c)).join("")}</div>
      <div class="kr"><div class="k f w15">${I("chevron-up")}</div>${"ㅋㅌㅊㅍㅠㅜㅡ".split("").map(c => k(c)).join("")}<div class="k f w15">${I("delete")}</div></div>
      <div class="kr"><div class="k f w15">?123</div>${k(",", "", "f")}<div class="k sp ${o.glow === "space" ? "glow" : ""}">${o.space || "Korean"}</div><div class="k f ${o.glow === "." ? "glow" : ""}">.</div><div class="k ent w15">${I("corner-down-left")}</div></div>
    </div>
    <div class="nav"></div>
  </div>`;
}

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-tpl]").forEach(el => { el.innerHTML = window[el.dataset.tpl](); });
  lucide.createIcons();
});
