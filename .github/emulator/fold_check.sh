#!/usr/bin/env bash
# 폴더블·가로 화면 관련 화면만 짧게 찍는다. 커밋 메시지에 [emu-fold] 가 있으면 build_apk.yml 이 돌린다.
# 기호 자판 힌트, 천지인(폰 화면), 폴드 안쪽 화면 크기의 천지인(좁힌 자판·버튼), 가로 화면을 찍는다.
set -u
source "$(dirname "$0")/lib.sh"

# 내용 설명(content-desc)이 [1]인 요소를 누른다. 키보드 위 버튼처럼 화면 아래쪽에 있어도 밀지 않는다.
tap_desc() {
  dump_ui
  pos=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse('/tmp/ui.xml').getroot()
for n in root.iter('node'):
    if n.get('content-desc') == sys.argv[1]:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -n "$pos" ]; then log "tap desc '$1' at $pos"; adb shell input tap $pos; sleep 2; else log "NOT FOUND desc: '$1'"; fi
}

# 자판 선택 화면에서 이름에 [1]이 들어 있는 카드의 체크 칸을 누른다. (두 줄 배치: 왼쪽 카드 468, 오른쪽 카드 1008)
toggle_card() {
  read x1 y1 x2 y2 <<< "$(bounds_contains "$1")"
  if [ -n "${y2:-}" ]; then
    if [ "$x1" -lt 540 ]; then x=468; else x=1008; fi
    log "toggle card '$1' at $x $(((y1 + y2) / 2))"
    adb shell input tap $x $(((y1 + y2) / 2))
    sleep 2
  else
    log "NOT FOUND card: '$1'"
  fi
}

# 저장된 자판 목록과 활성 자판 id 를 남긴다. (자판 켜고 끄기가 제대로 저장되는지 확인용)
dump_prefs() {
  adb shell run-as $PKG sh -c 'for f in $(grep -rl localization__subtypes files shared_prefs 2>/dev/null); do echo "## $f"; cat "$f"; echo; done' \
    | grep -o 'localization__\(active_subtype_id\|subtypes\).\{0,700\}' > "$OUT/prefs_$1.txt" 2>&1
  log "prefs $1: $(tr '\n' ' ' < "$OUT/prefs_$1.txt" | head -c 400)"
}

keep_ui() { cp /tmp/ui.xml "$OUT/$1.xml" 2>/dev/null; }

install_and_enable
# 화면 크기를 바꾸면 에뮬레이터의 런처가 죽으며 '계속 중지됨' 창을 띄워 화면을 가린다. 그 창을 끈다.
adb shell settings put global hide_error_dialogs 1
open_route settings/home

# 1) 폰 화면 쿼티 → 기호 자판 힌트
show_keyboard
shot f01_qwerty_phone
adb shell input tap 86 2214      # ?123
shot f02_symbols_hints
adb shell input tap 86 2214      # 가 (되돌아가기)

# 2) 키보드 설정 맨 아래 폴더블·가로 화면 섹션
open_route settings/keyboard
scroll_to '좁힌 자판 위치'
shot f03_settings_fold_section

# 3) 천지인만 켠다
open_route settings/keyboard/selection
tap_text "한국어"
shot f04_korean_layouts
keep_ui f04_korean_layouts
toggle_card "천지인"
sleep 3
# 일본어 확인(check.sh)처럼 화면을 새로 연 뒤 두벌식을 끈다. 같은 화면에서 이어 누르면 꺼지지 않았다.
show_keyboard
open_route settings/keyboard/selection
tap_text "한국어"
dump_prefs before_off
toggle_card "두벌식"
sleep 3
dump_prefs after_off
shot f05_cheonjiin_only
show_keyboard
shot f06_cheonjiin_phone

# 4) 폴드 안쪽 화면 크기 (약 775dp 너비) → 좁힌 천지인이 오른쪽에
adb shell wm size 1812x2176
adb shell wm density 374
sleep 4
show_keyboard
shot f07_cheonjiin_unfolded_right
keep_ui f07_cheonjiin_unfolded_right
tap_desc "가운데로"
shot f08_cheonjiin_unfolded_center
tap_desc "왼쪽으로"
shot f09_cheonjiin_unfolded_left

# 5) 폰 화면을 가로로 돌렸을 때
adb shell wm size reset
adb shell wm density reset
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
sleep 4
show_keyboard
shot f10_cheonjiin_landscape
tap_desc "넓게"
shot f11_cheonjiin_landscape_expanded
adb shell settings put system user_rotation 0

adb logcat -d | grep -E "FATAL EXCEPTION|ANR in dev.malangkey" -A 25 > "$OUT/fatal_log.txt" 2>&1
log "crash lines: $(grep -c 'FATAL EXCEPTION' "$OUT/fatal_log.txt")"
log "done"
