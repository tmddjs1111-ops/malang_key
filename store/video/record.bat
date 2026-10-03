@echo off
rem 말랑키 쇼츠 녹화: 폰(또는 에뮬레이터)을 연결하고 더블클릭.
rem 장면마다 안내가 나오고, 녹화한 파일은 store\video\raw\ 에 저장된다.
chcp 65001 > nul
setlocal

where adb > nul 2> nul
if errorlevel 1 set "PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%PATH%"
where adb > nul 2> nul
if errorlevel 1 (
  echo adb를 찾을 수 없어요. 안드로이드 스튜디오 SDK Manager에서 Platform-Tools를 설치해 주세요.
  pause
  exit /b 1
)

set "OUT=%~dp0raw"
if not exist "%OUT%" mkdir "%OUT%"

echo 연결된 기기:
adb devices
echo.
echo 녹화 전에 확인해 주세요
echo  - 말랑키 테마: 파스텔 핑크 / 스마트바: 말랑 슬롯
echo  - 상용구에 예시만 넣기 (집 주소: 서울시 마포구 말랑로 12, 계좌번호: 말랑은행 123-456-789012, 이메일: malang@example.com)
echo  - 에뮬레이터라면 "물리 키보드 사용 중에도 화면 키보드 표시"를 켜고, 화면 키보드를 마우스로 눌러서 치기
echo.
pause

rem 터치 위치 표시, 상태바 정리 (시간 12:00, 배터리 100%%, 알림 숨김)
adb shell settings put system show_touches 1
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter > nul
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200 > nul
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false > nul
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 > nul
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false > nul

call :rec 01_typing 12 "메모나 문자 앱에서 주소를 한 글자씩 천천히 치다가, 오타를 내고 지우기"
call :rec 02_quick_phrase 10 "빈 입력창에서 마침표 꾹 - 상용구 표가 뜨면 1초 멈춤 - 집 주소 칸으로 슥 - 손 떼기"
call :rec 03_more 12 "같은 방법으로 계좌번호, 이메일을 차례로 입력"
call :rec 04_settings 10 "말랑키 앱 - 클립보드 - 상용구 칸에 글자를 입력하는 모습"

rem 원래대로 되돌리기
adb shell am broadcast -a com.android.systemui.demo -e command exit > nul
adb shell settings put system show_touches 0

echo.
echo 끝! store\video\raw 폴더의 mp4 4개를 커밋해서 올려 주세요.
pause
exit /b 0

:rec
echo.
echo ============================================
echo [%1] %~3
echo 준비되면 아무 키나 누르세요. 누르는 순간부터 %2초 동안 녹화돼요.
echo (조금 길게 찍혀도 괜찮아요. 편집할 때 잘라요.)
pause > nul
echo 녹화 중... %2초
adb shell screenrecord --time-limit %2 /sdcard/%1.mp4
adb pull /sdcard/%1.mp4 "%OUT%\%1.mp4" > nul
adb shell rm /sdcard/%1.mp4
echo 저장됨: raw\%1.mp4
choice /c YN /m "다시 찍을까요? (Y = 다시 찍기, N = 다음 장면)"
if errorlevel 2 exit /b 0
goto :rec
