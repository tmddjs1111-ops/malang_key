#!/usr/bin/env bash
# 입력칸 실험실(앱 정보 > 입력칸 실험실)의 실험용 칸마다 두벌식으로 '손', '간'을 쳐 보고 찍는다.
# 커밋 메시지에 [emu-lab] 가 있으면 build_apk.yml 이 돌린다. 진단 기록도 함께 남긴다.
set -u
source "$(dirname "$0")/lib.sh"

# 내용 설명이 [1]인 요소를 찾는다. 화면 아래쪽(키보드에 가릴 자리)에 있으면 위로 밀고 다시 찾는다.
tap_desc_scroll() {
  for attempt in 1 2 3 4 5 6; do
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
    if [ -n "$pos" ] && [ "${pos#* }" -lt 1200 ]; then
      log "tap '$1' at $pos"
      adb shell input tap $pos
      sleep 2
      return 0
    fi
    # 기록 글(선택 가능한 글)이 아닌 아래쪽을 밀어야 화면이 스크롤된다.
    adb shell input swipe 540 1900 540 1000 400
    sleep 1
  done
  log "NOT FOUND desc: '$1'"
  return 1
}

# 두벌식 쿼티 좌표 (1080x2400, 기본 키 높이)
type_jamo() {
  for k in "$@"; do
    case $k in
      ㅅ) xy="486 1856";; ㅗ) xy="646 1974";; ㄴ) xy="216 1974";;
      ㄱ) xy="378 1856";; ㅏ) xy="862 1974";;
    esac
    adb shell input tap $xy
    sleep 0.5
  done
}

install_and_enable
open_route settings/home
open_route settings/about/input-lab
tap_text "진단 기록 켜기"
shot l00_lab_top

for field in normal blocked_read password blocked_password raw; do
  open_route settings/about/input-lab
  if tap_desc_scroll "lab_$field"; then
    type_jamo ㅅ ㅗ ㄴ
    adb shell input tap 540 2214      # 스페이스
    type_jamo ㄱ ㅏ ㄴ
    sleep 1
    shot "l_${field}"
  fi
  adb shell input keyevent KEYCODE_BACK
done

# 진단 기록 화면
open_route settings/about/input-lab
shot l99_diagnostics
adb logcat -d | grep -E "FATAL EXCEPTION|ANR in dev.malangkey" -A 25 > "$OUT/fatal_log.txt" 2>&1
log "crash lines: $(grep -c 'FATAL EXCEPTION' "$OUT/fatal_log.txt")"
log "done"
