#!/usr/bin/env bash
# 에뮬레이터 확인 스크립트들이 같이 쓰는 함수. (check.sh, fold_check.sh 에서 source 한다)
PKG=dev.malangkey.debug
IME=$PKG/dev.malangkey.FlorisImeService
OUT=shots
mkdir -p "$OUT"
log() { echo "== $*" | tee -a "$OUT/log.txt"; }

shot() { sleep 2; dump_ui; adb exec-out screencap -p > "$OUT/$1.png"; log "shot $1"; }

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  # 에뮬레이터의 다른 앱(런처 등)이 '응답 없음' 창을 띄우면 화면이 가려진다. '기다리기'로 닫고 다시 읽는다.
  if grep -q "isn't responding\|응답하지 않습니다" /tmp/ui.xml 2>/dev/null; then
    log "dismiss ANR dialog"
    adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1
    pos=$(python3 - <<'PY'
import re, xml.etree.ElementTree as ET
root = ET.parse('/tmp/ui.xml').getroot()
for n in root.iter('node'):
    if n.get('text') in ('Wait', '기다리기'):
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
    [ -n "$pos" ] && adb shell input tap $pos
    sleep 2
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  fi
}

# 화면에서 글자(또는 설명)가 [1]인 요소를 찾아 누른다. 없으면 아래로 밀어 가며 다시 찾는다.
# 하단 탭 막대의 탭을 누를 때는 [2]에 nav 를 준다.
tap_text() {
  for attempt in 1 2 3 4 5; do
    dump_ui
    pos=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want = sys.argv[1]
try:
    root = ET.parse('/tmp/ui.xml').getroot()
except Exception:
    sys.exit()
for n in root.iter('node'):
    if n.get('text') == want or n.get('content-desc') == want:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
    # 화면 아래쪽은 떠 있는 하단 탭 막대에 가려진다. 거기 있으면 조금 올린 뒤 다시 찾는다.
    if [ -n "$pos" ] && [ "${2:-}" != nav ] && [ "${pos#* }" -gt 1950 ]; then
      adb shell input swipe 540 1500 540 1000 300
      sleep 1
      continue
    fi
    if [ -n "$pos" ]; then
      log "tap '$1' at $pos"
      adb shell input tap $pos
      sleep 2
      return 0
    fi
    adb shell input swipe 540 1600 540 900 300
    sleep 1
  done
  log "NOT FOUND: '$1'"
  return 1
}

# 화면에 [1] 글자가 보일 때까지 아래로 민다.
scroll_to() {
  for i in 1 2 3 4 5 6; do
    dump_ui
    grep -q "$1" /tmp/ui.xml && return 0
    adb shell input swipe 540 1700 540 900 300
    sleep 1
  done
  log "NOT FOUND while scrolling: '$1'"
}

# 연락처 추가 화면의 입력칸을 눌러 키보드를 띄운다.
show_keyboard() {
  adb shell am force-stop com.android.contacts
  adb shell am start -W -a android.intent.action.INSERT -t vnd.android.cursor.dir/contact >/dev/null
  # 처음 여는 연락처 앱은 늦게 떠서, 입력칸이 보일 때까지 잠깐씩 기다린다.
  for wait in 1 2 3 4 5; do
    sleep 4
    dump_ui
    grep -q 'android.widget.EditText' /tmp/ui.xml && break
  done
  pos=$(python3 - <<'PY'
import re, xml.etree.ElementTree as ET
try:
    root = ET.parse('/tmp/ui.xml').getroot()
except Exception:
    raise SystemExit
for n in root.iter('node'):
    if n.get('class') == 'android.widget.EditText':
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -n "$pos" ]; then adb shell input tap $pos; else log "no text field"; fi
  sleep 3
}

install_and_enable() {
  log "install"
  adb install -r apk/malang-key-debug.apk | tee -a "$OUT/log.txt"
  adb shell ime enable "$IME" 2>&1 | tee -a "$OUT/log.txt"
  adb shell ime set "$IME" 2>&1 | tee -a "$OUT/log.txt"
  # ime set 이 먹지 않을 때가 있어서 시스템 설정에도 직접 넣는다.
  adb shell settings put secure enabled_input_methods "$IME"
  adb shell settings put secure default_input_method "$IME"
  adb shell settings get secure default_input_method 2>&1 | tee -a "$OUT/log.txt"
  adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton
  sleep 2
}

open_route() {
  adb shell am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE \
    -d "ui://malangkey/$1" $PKG >/dev/null
  sleep 5
}

# 글자(text)에 [1]이 들어 있는 요소의 bounds 를 출력한다.
bounds_contains() {
  dump_ui
  python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse('/tmp/ui.xml').getroot()
for n in root.iter('node'):
    if sys.argv[1] in (n.get('text') or ''):
        print(*map(int, re.findall(r'\d+', n.get('bounds'))))
        break
PY
}
