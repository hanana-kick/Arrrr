# Arrrr · Android RSS Reader

Kotlin / Jetpack Compose Material 3 Expressive 기반의 RSS·Atom 리더. 승인된 Doop 시안([캔버스](https://doop.design/c/6DyH2NjWm7))을 Android 네이티브 화면으로 옮겼습니다. 웹뷰나 HTML 목업을 앱에 넣지 않습니다.

## 기능

- 좁은 화면: 피드 · 저장 · 구독 · 설정의 하단 내비게이션. 본문은 별도 화면.
- 넓은 화면/폴더블: 내비게이션 레일, 적응형 목록·본문 동시 표시, 구독 카드 2열, 설정 분류·상세 분할. 기기명이나 물리 해상도 하드코딩 없음.
- Material 3 Card / Switch / 연결 토글 / 바텀 시트. 북마크는 Google Material Symbols Rounded의 outline ↔ fill이며 별도 배경 강조 없음.
- RSS 2.0, RSS 1/RDF, Atom 수집. 사이트의 RSS 자동 발견, ETag/Last-Modified 조건부 요청, 오류 표시와 재시도.
- SQLite에 구독·본문·읽음·북마크 저장. 피드별 최근 1,000개 글을 캐시하고 북마크는 자동 정리에서 제외. 구독 해제 시 저장한 글 유지.
- 피드 제공 이미지/사이트 파비콘 우선. 실패 시 RSS 아이콘. 이모지 또는 번들된 Phosphor 4종 및 색상으로 사용자 지정.
- DataStore 설정: 15분~24시간 백그라운드 폴링, 앱 열 때 확인, 전경 1분/5분 확인, Wi-Fi 제한. 앱/기기 재시작 후에도 WorkManager 작업 유지.
- 시스템·라이트·다크, Android 12+ 동적 운영체제 색상, 수동 프리셋/직접 색상, 검정 배경. 본문 글꼴·배율·줄 간격·목록 밀도·요약 표시.
- 외부 브라우저 원문 열기와 Android 공유 시트.

**범위:** 실시간 수신 선호는 저장되지만 WebSub→기기 push를 전달하는 서버는 아직 연결하지 않았습니다. 현재 실제 업데이트는 폴링입니다. 설정 화면에도 미연결 상태를 표시합니다. RSS에 요약만 있으면 제공된 요약까지만 표시하며 유료/로그인 본문을 우회 수집하지 않습니다. HTTP 피드 지원을 위해 cleartext를 허용하지만, HTTPS→HTTP 자동 리디렉션과 URL에 포함된 계정정보는 허용하지 않습니다. 아이콘은 해당 사이트에서 직접 요청하며 외부 favicon 프록시에 구독 주소를 보내지 않습니다.

## 빌드와 확인

JDK 17, Android SDK Platform 37.0, Build Tools 36.0.0. 저장소 루트에서:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

GitHub Actions는 단위 테스트·lint·debug/release 빌드 후 API 35 에뮬레이터에서 compact/expanded UI 및 SQLite 저장 상태를 확인합니다. 네이티브 스크린샷은 `native-ui-tests` 아티팩트에 포함합니다. 이는 실제 Fold8 하드웨어 검증을 대신하지 않습니다.

`main` CI가 모두 성공하면 [Releases](https://github.com/hanana-kick/Arrrr/releases)에 설치 가능한 **debug 서명 프리뷰 APK**와 SHA-256을 올립니다. Play 배포용 production 서명은 구성하지 않았습니다. debug 키는 CI 캐시에 보관하며 캐시가 삭제되면 기존 프리뷰를 지우고 재설치해야 할 수 있습니다. 사용자 데이터 백업 없이 앱을 제거하지 마세요.

첫 실행은 빈 구독 목록입니다. 새 피드를 추가하면 실제 글을 가져옵니다. 테스트용 가상 기사는 `androidTest`에만 있으며 제품에 자동으로 넣지 않습니다.

## 구조

`data/` — SQLite와 DataStore, 저장 모델 / `network/` — 제한된 HTTP 요청과 RSS·Atom 파서 / `sync/` — WorkManager / `ui/` — 내비게이션, 적응형 본문, 구독과 설정 / `ui/theme/` — Material Expressive 동적·고정 색상.

도입 라이브러리는 `app/build.gradle.kts`에 고정되어 있습니다. Material Java 색상 유틸리티는 테마 어댑터 한 곳에서만 사용하며 View UI는 만들지 않습니다. 아이콘 라이선스는 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)를 참고하세요.
