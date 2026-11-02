# 말랑키 아이폰 버전

안드로이드 말랑키(`app/`)와 따로 Swift 로 만든 iOS 키보드입니다.

## 구성

| 폴더 | 내용 |
| --- | --- |
| `MalangCore/` | 한글 조합기(`HangulComposer`, 안드로이드 `HangulUnicode.kt` 를 옮김), 자판 배치, 공유 설정. UIKit 없이 `swift test` 로 시험 |
| `Keyboard/` | 키보드 확장. `KeyboardViewController` 가 입력창에 글자를 넣고, `KeyboardView`(SwiftUI)가 키를 그림 |
| `App/` | 키보드 켜는 방법 안내, 한글 자판 고르기, 시험 입력창 |
| `project.yml` | XcodeGen 설정. `.xcodeproj` 는 이걸로 만들어 쓰고 커밋하지 않음 |

## 지금 되는 것

- **자판**: 두벌식, 천지인, 스카이, 단모음, 획추가형(나랏글), 영어 쿼티, 숫자·기호 2쪽
- **입력**: 쉬프트(두 번 누르면 고정), 지우기 길게 누르기, 길게 눌러 숫자·기호(키 오른쪽 위 표시), 한/영 전환, 지구본 키, 키 소리
- **위쪽 툴바**: 이모지(한국어 CLDR 목록, 최근 쓴 이모지), 상용구, 테마, 설정(자판·높이·숫자 힌트), 키보드 내리기
- **이모지 추천**: "사랑", "행복"처럼 친 단어에 맞는 이모지를 툴바에 띄움
- **테마**: 안드로이드와 같은 색 10종 + 말랑 핑크, "자동"은 밝을 때 바닐라·어두울 때 블랙
- **앱**: 키보드 켜기 안내, 앱 안에서 쳐 보기, 테마·자판·높이·소리 설정, 상용구 고치기
- 전체 접근 허용을 요구하지 않음

CI 가 시뮬레이터에서 찍은 화면: Releases 의 `ios-shots`.

## 빌드

Windows 에서는 iOS 앱을 빌드할 수 없어서 GitHub Actions 의 Mac(`Build iOS`)이 대신 빌드하고 시험을 돌립니다. `ios/` 를 바꿔 푸시하면 자동으로 돕니다.

Mac 이 있으면:

```bash
brew install xcodegen
cd ios && xcodegen generate && open MalangKey.xcodeproj
```

## 아이폰에 설치하려면 (TestFlight)

1. Apple Developer Program 가입 (연 99달러)
2. `.github/workflows/release_ios.yml` 맨 위 "준비"대로 App Store Connect 에 앱을 만들고 API 키 4개를 GitHub Secrets 에 넣기
3. Actions → **Release iOS to TestFlight** → Run workflow
4. 아이폰에 TestFlight 앱을 깔고 초대 메일로 설치

## iOS 키보드의 제약

- 비밀번호 칸에서는 iOS 가 무조건 시스템 키보드로 바꿉니다. (안드로이드의 PASS 비밀번호 칸 문제는 생기지 않음)
- 전화번호·숫자 전용 칸도 시스템 키보드가 뜰 수 있습니다.
- 키보드 확장은 메모리를 수십 MB 밖에 못 써서 일본어 Mozc 사전 같은 큰 엔진은 그대로 넣기 어렵습니다.
- 진동(햅틱)·클립보드 기록은 "전체 접근 허용"이 있어야 합니다. 지금은 요구하지 않습니다.
- 전체 접근이 꺼져 있으면 키보드가 앱의 설정(App Group)을 못 읽을 수 있습니다. 그래서 키보드 툴바의 ⚙︎ 와 🎨 에서도 자판·테마·높이를 바꿀 수 있게 했습니다.
