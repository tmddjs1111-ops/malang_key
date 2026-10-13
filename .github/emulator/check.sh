#!/usr/bin/env bash
# 에뮬레이터에서 디버그 APK를 설치하고, 테마를 바꿔 가며 키보드 화면을 찍는다.
# 결과 사진은 shots/ 에 남는다. (.github/workflows/build_apk.yml 의 emulator 작업에서 실행)
set -u
PKG=dev.malangkey.debug
IME=$PKG/dev.malangkey.FlorisImeService
OUT=shots
mkdir -p "$OUT"
log() { echo "== $*" | tee -a "$OUT/log.txt"; }

shot() { sleep 2; adb exec-out screencap -p > "$OUT/$1.png"; log "shot $1"; }

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
}

# 화면에서 글자(또는 설명)가 [1]인 요소를 찾아 누른다. 없으면 아래로 밀어 가며 다시 찾는다.
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

# 테마 화면을 연다. 첫 실행은 느려서 화면 제목이 보일 때까지 다시 연다.
open_theme_screen() {
  for attempt in 1 2 3 4; do
    adb shell am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE \
      -d ui://malangkey/settings/theme $PKG >/dev/null
    sleep 4
    dump_ui
    grep -q '키보드 테마' /tmp/ui.xml && return 0
    log "theme screen not shown yet ($attempt)"
    sleep 4
  done
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
  sleep 4
  dump_ui
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

log "install"
adb install -r apk/malang-key-debug.apk | tee -a "$OUT/log.txt"
adb shell ime enable "$IME" 2>&1 | tee -a "$OUT/log.txt"
adb shell ime set "$IME" 2>&1 | tee -a "$OUT/log.txt"
# ime set 이 먹지 않을 때가 있어서 시스템 설정에도 직접 넣는다.
adb shell settings put secure enabled_input_methods "$IME"
adb shell settings put secure default_input_method "$IME"
adb shell settings get secure default_input_method 2>&1 | tee -a "$OUT/log.txt"
# 3버튼 소프트바로 맞춘다. (사진이 소프트바 위에서 끝나는지 보기 쉽게)
adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton
sleep 2

open_theme_screen
shot 01_theme_screen

# 1) 기본 블랙 → 키보드
tap_text "기본 블랙"
show_keyboard
shot 02_black_keyboard

# 2) 블랙에서 커스텀 → 키보드 (블랙과 같아야 한다)
open_theme_screen
tap_text "커스텀"
shot 03_custom_selected
adb shell input swipe 540 1700 540 700 300
shot 04_custom_colors_from_black
show_keyboard
shot 05_custom_from_black_keyboard

# 3) 파스텔 핑크 → 커스텀 → 키보드 (핑크와 같아야 한다)
open_theme_screen
tap_text "파스텔 핑크"
show_keyboard
shot 06_pink_keyboard
open_theme_screen
tap_text "커스텀"
show_keyboard
shot 07_custom_from_pink_keyboard

# 4) 다시 커스텀을 눌러도(이미 커스텀) 그대로여야 한다
open_theme_screen
tap_text "커스텀"
show_keyboard
shot 08_custom_again_keyboard

# 5) 커스텀 화면의 색상 표와 스마트바 아이콘 칸
open_theme_screen
tap_text "커스텀"
scroll_to '커스텀 색상'
adb shell input swipe 540 1500 540 1100 300
shot 09_custom_colors
scroll_to '사진 아이콘을 넣어'
shot 10_smartbar_icons

# 설정 파일 위치를 남겨 둔다 (다음 확인에 쓸 수 있게)
adb shell run-as $PKG find files shared_prefs -maxdepth 2 > "$OUT/app_files.txt" 2>&1
adb logcat -d -t 400 '*:E' > "$OUT/logcat_errors.txt" 2>&1
log "done"
