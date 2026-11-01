# 말랑키 작업 규칙

## 업데이트 흐름 (항상 이 순서로)
업데이트(사용자에게 보이는 변경)를 마치면 따로 묻지 않고 Git 과 Google Play 에 올린다.

1. 버전을 올리기 전에 `git fetch origin` 후 `release-*`, `desktop-*` 같은 다른 브랜치에 `main` 보다 높은
   `projectVersionCode` 가 있는지 본다. 다른 대화에서 따로 올린 버전이 있으면 그 브랜치를 먼저 합친다.
   (Play 는 이미 쓴 versionCode 를 다시 받지 않는다: "Version code N has already been used")
   그다음 `gradle.properties` 의 `projectVersionCode`(+1)와 `projectVersionName`(+0.01)을 올린다.
2. `distribution/whatsnew/whatsnew-ko-KR` 출시 노트를 이번 변경에 맞게 쓴다. (500자 이내)
3. 커밋하고 작업 브랜치에 푸시한다. `Build debug APK` 가 초록인지 확인한다.
4. `main` 으로 PR 을 만들고 squash 로 합친다.
5. `main` 에 versionCode 가 올라간 커밋이 들어오면 `Release to Google Play` 가 자동으로 돌아
   비공개 테스트(alpha) 트랙에 올린다. 끝나면 결과를 확인하고 사용자에게 알린다.

사용자가 "올리지 말고" 처럼 이번엔 올리지 말라고 하면 3단계까지만 한다.
