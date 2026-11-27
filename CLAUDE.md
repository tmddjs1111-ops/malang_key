# 말랑키 작업 규칙

## 업데이트 흐름 (항상 이 순서로)
업데이트(사용자에게 보이는 변경)를 마치면 따로 묻지 않고 Git 과 Google Play 에 올린다.

1. 버전을 올리기 전에 `git fetch origin` 후 `origin/main` 을 작업 브랜치에 먼저 합친다(다른 대화가 `main` 에
   바로 올렸을 수 있다). 그리고 `release-*`, `desktop-*` 같은 다른 브랜치에 `main` 보다 높은
   `projectVersionCode` 가 있는지 본다. 다른 대화에서 따로 올린 버전이 있으면 그 브랜치를 먼저 합친다.
   (Play 는 이미 쓴 versionCode 를 다시 받지 않는다: "Version code N has already been used")
   그다음 `gradle.properties` 의 `projectVersionCode`(+1)와 `projectVersionName`(+0.01)을 올린다.
2. `distribution/whatsnew/whatsnew-ko-KR` 출시 노트를 이번 변경에 맞게 쓴다. (500자 이내)
3. 커밋하고 작업 브랜치에 푸시한다. `Build debug APK` 가 초록인지 확인한다.
4. `main` 으로 PR 을 만들고 squash 로 합친다. 합친 직후 `main` 을 작업 브랜치에 다시 합쳐 둔다
   (`git fetch origin main && git merge -s ours origin/main`, 내용은 이미 같다). 안 하면 다음 PR 이
   같은 줄을 두고 충돌한다.
5. `main` 에 versionCode 가 올라간 커밋이 들어오면 `Release to Google Play` 가 자동으로 돌아
   비공개 테스트(alpha) 트랙에 올린다. 끝나면 결과를 확인하고 사용자에게 알린다.

사용자가 "올리지 말고" 처럼 이번엔 올리지 말라고 하면 3단계까지만 한다.

## 원칙
- 키보드로 입력한 내용은 절대 기기 밖으로 나가지 않는다. 키보드 기능은 완전히 오프라인으로 동작하며,
  사전·번역·맞춤법 같은 온라인 연동은 넣지 않는다.
- 인터넷은 설정 앱 화면의 광고(Google AdMob)에만 쓴다. (1.70 부터 Android 앱에 인터넷 권한이 있다.)
  - 광고는 설정 앱 화면에만 둔다. 키보드(IME) 화면에는 절대 넣지 않는다. (다른 앱 위에 뜨므로 Play 정책 위반)
  - 키보드·입력 처리 코드에서 광고 SDK 나 네트워크를 쓰지 않는다. 입력 내용을 광고 요청에 넣지 않는다.
  - 광고 ID 는 `app/src/main/res/values/admob.xml` 에 있다. debug 빌드는 `src/debug` 의 테스트 ID 를 쓴다.
  - 광고를 새로 넣거나 옮기면 `docs/privacy.html` 과 Play Console 데이터 보안 항목도 같이 맞춘다.
- iPhone 앱에는 광고가 없고 인터넷을 쓰지 않는다.
