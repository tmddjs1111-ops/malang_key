# 말랑키 아이폰 버전

안드로이드 말랑키(`app/`)와 따로 Swift 로 만든 iOS 키보드입니다.

## 구성

| 폴더 | 내용 |
| --- | --- |
| `MalangCore/` | 한글 조합기(`HangulComposer`, 안드로이드 `HangulUnicode.kt` 를 옮김), 자판 배치, 공유 설정. UIKit 없이 `swift test` 로 시험 |
| `Keyboard/` | 키보드 확장. `KeyboardViewController` 가 입력창에 글자를 넣고, `KeyboardView`(SwiftUI)가 키를 그림 |
| `App/` | 키보드 켜는 방법 안내, 한글 자판 고르기, 시험 입력창 |
| `project.yml` | XcodeGen 설정. `.xcodeproj` 는 이걸로 만들어 쓰고 커밋하지 않음 |

지금 되는 것: 두벌식·천지인, 영어 쿼티, 숫자·기호 2쪽, 길게 눌러 숫자·기호(키 오른쪽 위에 표시), 쉬프트(두 번 누르면 고정), 지우기 길게 누르기, 한/영 전환, 지구본 키, 다크 모드, 앱 안 미리보기(키보드를 켜지 않고 쳐 보기).

CI 가 시뮬레이터에서 찍은 화면: Releases 의 `ios-shots`.

## 빌드

Windows 에서는 iOS 앱을 빌드할 수 없어서 GitHub Actions 의 Mac(`Build iOS`)이 대신 빌드하고 시험을 돌립니다. `ios/` 를 바꿔 푸시하면 자동으로 돕니다.

Mac 이 있으면:

```bash
brew install xcodegen
cd ios && xcodegen generate && open MalangKey.xcodeproj
```

## 아이폰에 설치하려면

1. Apple Developer Program 가입 (연 99달러)
2. App Store Connect 에 앱 등록, 번들 ID `dev.malangkey.ios` / `dev.malangkey.ios.keyboard`, App Group `group.dev.malangkey`
3. 서명 인증서·프로비저닝을 GitHub Secrets 에 넣고 TestFlight 업로드 워크플로 추가

## iOS 키보드의 제약

- 비밀번호 칸에서는 iOS 가 무조건 시스템 키보드로 바꿉니다. (안드로이드의 PASS 비밀번호 칸 문제는 생기지 않음)
- 전화번호·숫자 전용 칸도 시스템 키보드가 뜰 수 있습니다.
- 키보드 확장은 메모리를 수십 MB 밖에 못 써서 일본어 Mozc 사전 같은 큰 엔진은 그대로 넣기 어렵습니다.
- 진동(햅틱)·클립보드 기록은 "전체 접근 허용"이 있어야 합니다. 지금은 요구하지 않습니다.
