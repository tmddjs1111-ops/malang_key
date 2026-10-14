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
adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton
sleep 2

open_route() {
  adb shell am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE \
    -d "ui://malangkey/$1" $PKG >/dev/null
  sleep 5
}

# 홈 화면의 테마 탭을 연다. 첫 실행은 느려서 '지금 키보드'가 보일 때까지 다시 연다.
open_theme_tab() {
  for attempt in 1 2 3 4; do
    open_route settings/home
    tap_text "테마" >/dev/null
    dump_ui
    grep -q '지금 키보드' /tmp/ui.xml && return 0
    log "theme tab not shown yet ($attempt)"
    sleep 4
  done
}

# 0) 처음 설치한 상태의 키보드 (기본 테마가 바닐라여야 한다)
show_keyboard
shot 00_default_keyboard

# 0) 지금 앱 화면 (디자인 비교용)
open_route settings/home
shot 00a_home
open_route settings/keyboard/input-feedback
shot 00b_sound
open_route settings/smartbar
shot 00c_smartbar_settings

# 1) 테마 탭
open_theme_tab
shot 01_theme_tab
adb shell input swipe 540 1700 540 700 300
shot 01b_theme_tab_more
adb shell input swipe 540 1700 540 700 300
shot 01c_theme_tab_end
adb shell input swipe 540 1700 540 700 300
shot 01d_theme_tab_bottom

# 2) 기본 블랙 → 키보드
open_theme_tab
tap_text "기본 블랙"
show_keyboard
shot 02_black_keyboard

# 3) 블랙에서 '꾸미기' → 꾸미기 화면 (블랙과 같아야 한다)
open_theme_tab
tap_text "✏️ 꾸미기"
sleep 2
shot 03_customize_colors
tap_text "배경 사진"
shot 03b_customize_photo
tap_text "스마트바"
shot 03c_customize_smartbar
show_keyboard
shot 04_custom_from_black_keyboard

# 4) 파스텔 핑크 → '꾸미기' → 키보드 (핑크와 같아야 한다)
open_theme_tab
tap_text "파스텔 핑크"
show_keyboard
shot 05_pink_keyboard
open_theme_tab
tap_text "✏️ 꾸미기"
sleep 2
shot 06_customize_from_pink
show_keyboard
shot 07_custom_from_pink_keyboard

# 5) 꾸민 모양을 내 테마로 저장 → 테마 탭 위쪽 이름과 내 테마 목록
open_route settings/theme
scroll_to '내 테마로 저장'
tap_text "내 테마로 저장"
shot 08_save_dialog
tap_text "저장"
sleep 2
open_theme_tab
shot 09_theme_tab_after_save
scroll_to '받은 테마 파일 가져오기'
shot 10_my_themes

# 6) 외곽선을 가장 굵게 하고 테마별 키보드 (바닐라에서 외곽선이 안 보이던 문제 확인)
bounds_of() {
  dump_ui
  python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse('/tmp/ui.xml').getroot()
for n in root.iter('node'):
    if n.get('text') == sys.argv[1]:
        print(*map(int, re.findall(r'\d+', n.get('bounds'))))
        break
PY
}
open_theme_tab
scroll_to '외곽선 두께'
read x1 y1 x2 y2 <<< "$(bounds_of '외곽선 두께')"
if [ -n "${y2:-}" ]; then
  # 제목 바로 아래 슬라이더의 오른쪽 끝을 눌러 가장 굵게 한다.
  adb shell input tap 985 $((y2 + 55))
  sleep 1
fi
shot 11_outline_max_setting
# 사진 파일 이름은 영어로 둔다. (릴리스에 올릴 때 한글이 지워져 이름이 겹친다)
for pair in "말랑키 바닐라:vanilla" "파스텔 핑크:pink" "기본 화이트:white" "기본 블랙:black"; do
  open_theme_tab
  tap_text "${pair%%:*}"
  show_keyboard
  shot "12_outline_${pair##*:}"
done

# 설정 파일 위치를 남겨 둔다 (다음 확인에 쓸 수 있게)
adb shell run-as $PKG find files shared_prefs -maxdepth 2 > "$OUT/app_files.txt" 2>&1
adb logcat -d -t 400 '*:E' > "$OUT/logcat_errors.txt" 2>&1
log "done"
