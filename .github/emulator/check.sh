#!/usr/bin/env bash
# 에뮬레이터에서 디버그 APK를 설치하고, 테마를 바꿔 가며 키보드 화면을 찍는다.
# 결과 사진은 shots/ 에 남는다. (.github/workflows/build_apk.yml 의 emulator 작업에서 실행)
set -u
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
    tap_text "테마" nav >/dev/null
    dump_ui
    grep -q '지금 키보드' /tmp/ui.xml && return 0
    log "theme tab not shown yet ($attempt)"
    sleep 4
  done
}

# 0) 지금 앱 화면 (디자인 비교용). 앱을 한 번 열어야 키보드가 준비된다.
open_route settings/home
shot 00a_home

# 처음 설치한 상태의 키보드 (기본 테마 바닐라, 외곽선 '테마 기본')
show_keyboard
shot 00_default_keyboard
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

# 7) 전체 둘러보기: 설정 화면을 하나씩 열어 찍는다 (이상한 화면·크래시 찾기)
i=0
for route in settings/keyboard settings/keyboard/selection settings/keyboard/order settings/keyboard/test \
             settings/keyboard/input-feedback settings/smartbar settings/typing settings/dictionary \
             settings/gestures settings/clipboard settings/media settings/other/backup settings/other/restore \
             settings/about settings/about/project-license settings/about/third-party-licenses; do
  i=$((i + 1))
  open_route "$route"
  shot "20_$(printf %02d $i)_$(echo "$route" | tr '/' '_')"
done
open_route settings/home
tap_text "게임" nav
shot 21_game_tab
open_route settings/home
tap_text "빠른 설정"
shot 22_quick_settings

# 8) 키보드 직접 써 보기 (바닐라 기본 배열 기준 좌표)
open_theme_tab
tap_text "말랑키 바닐라"
show_keyboard
shot 30_keyboard_before_typing
# ㅇ ㅏ ㄴ ㄴ ㅕ ㅇ → 안녕
for xy in "326 1979" "864 1979" "217 1979" "217 1979" "700 1857" "326 1979"; do
  adb shell input tap $xy
  sleep 0.4
done
shot 31_typed_annyeong
adb shell input tap 86 2214      # ?123
shot 32_symbols
adb shell input tap 86 2214      # 가 (되돌아가기)
adb shell input tap 629 1750     # 스마트바 이모지
shot 33_emoji_panel
adb shell input keyevent KEYCODE_BACK
show_keyboard
adb shell input tap 89 1750      # 스마트바 붙여넣기 칸
shot 34_smartbar_first_button

# 9) 일본어: 로마자 자판만 켜고 nihon → 日本 후보가 뜨는지 (Mozc 엔진이 들어 있는지 확인)
tap_contains() {
  dump_ui
  pos=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse('/tmp/ui.xml').getroot()
for n in root.iter('node'):
    if sys.argv[1] in (n.get('text') or ''):
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -n "$pos" ]; then log "tap '*$1*' at $pos"; adb shell input tap $pos; sleep 2; else log "NOT FOUND: '*$1*'"; fi
}
open_route settings/keyboard/selection
tap_text "일본어"
shot 40_japanese_layouts
cp /tmp/ui.xml "$OUT/40_japanese_layouts.xml"
scroll_to 'ROMAJI'
read x1 y1 x2 y2 <<< "$(bounds_of 'QWERTY')"
if [ -n "${y2:-}" ]; then
  # 카드 오른쪽 위 체크 칸을 누른다.
  adb shell input tap 468 $(((y1 + y2) / 2))
  sleep 2
else
  log "NOT FOUND: QWERTY card"
fi
shot 41_japanese_romaji_selected
show_keyboard
shot 42a_keyboard_before_switch
# 한국어 두벌식을 꺼서 일본어 로마자만 남긴다.
open_route settings/keyboard/selection
tap_text "한국어"
read x1 y1 x2 y2 <<< "$(bounds_of '두벌식(쿼티)')"
if [ -n "${y2:-}" ]; then
  adb shell input tap 468 $(((y1 + y2) / 2))
  sleep 2
else
  log "NOT FOUND: 두벌식 card"
fi
shot 41b_korean_off
show_keyboard
shot 42_japanese_keyboard
# 쿼티 좌표: n(3줄 6번째) i(1줄 8번째) h(2줄 6번째) o(1줄 9번째) n
for xy in "756 2094" "808 1856" "646 1974" "914 1856" "756 2094"; do
  adb shell input tap $xy
  sleep 0.6
done
sleep 2
shot 43_japanese_nihon_candidates
adb shell input tap 160 1750     # 첫 후보
shot 44_japanese_committed
# きょうはいいてんき: kyouhaiitennki
for ch in k y o u h a i i t e n n k i; do
  case $ch in
    q) xy="58 1856";; w) xy="164 1856";; e) xy="272 1856";; r) xy="378 1856";; t) xy="486 1856";;
    y) xy="592 1856";; u) xy="700 1856";; i) xy="808 1856";; o) xy="914 1856";; p) xy="1022 1856";;
    a) xy="108 1974";; s) xy="216 1974";; d) xy="324 1974";; f) xy="432 1974";; g) xy="538 1974";;
    h) xy="646 1974";; j) xy="754 1974";; k) xy="862 1974";; l) xy="970 1974";;
    z) xy="220 2094";; x) xy="328 2094";; c) xy="434 2094";; v) xy="542 2094";; b) xy="648 2094";;
    n) xy="756 2094";; m) xy="862 2094";;
  esac
  adb shell input tap $xy
  sleep 0.5
done
sleep 2
shot 45_japanese_sentence_candidates
adb logcat -d | grep -i "mozc" | tail -40 > "$OUT/mozc_log.txt" 2>&1

# 앱이 죽은 적이 있는지 남긴다
adb logcat -d -b crash > "$OUT/crash_log.txt" 2>&1
adb logcat -d | grep -E "FATAL EXCEPTION|ANR in dev.malangkey" -A 25 > "$OUT/fatal_log.txt" 2>&1
log "crash lines: $(grep -c 'FATAL EXCEPTION' "$OUT/fatal_log.txt")"

# 설정 파일 위치를 남겨 둔다 (다음 확인에 쓸 수 있게)
adb shell run-as $PKG find files shared_prefs -maxdepth 2 > "$OUT/app_files.txt" 2>&1
adb logcat -d -t 400 '*:E' > "$OUT/logcat_errors.txt" 2>&1
log "done"
