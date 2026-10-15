# Arrrr · Android RSS Reader

Kotlin / Jetpack Compose Material 3 Expressive 기반 RSS·Atom 리더. [승인된 Doop 디자인](https://doop.design/c/6DyH2NjWm7)을 네이티브 Compose로 구현합니다. HTML 목업이나 WebView를 앱 UI로 넣지 않습니다.

## 0.3.2 읽기 흐름

- 읽을거리·구독·설정의 세 목적지. 저장한 글은 읽을거리 필터로 봅니다.
- 좁은 창은 전체 기사 페이지로 이동하며, 넓은 창은 목록 옆에 기사 페이지를 표시합니다. 읽기 시트와 탄성 넘김은 사용하지 않습니다.
- 기사 본문은 RSS 내용 또는 원문 웹페이지에서 추출한 공개 본문·이미지를 표시하고, 추출 성공 시 기기에 저장합니다. 추출 불가 시 RSS 내용과 원문 열기를 제공합니다.
- 저장·읽기 테마·공유 플로팅 툴바, 큰/작은 제목 전환, 명시적 이전/다음 이동과 읽던 위치 복원.

## 수집과 저장

RSS 2.0, RSS 1/RDF, Atom을 지원합니다. 사이트에서 피드 주소를 찾고 ETag/Last-Modified 조건부 요청으로 새 글을 확인합니다. 구독·본문·읽음·북마크는 SQLite에 저장합니다. 피드별 최근 1,000개를 캐시하며 저장한 글은 정리에서 제외하고 구독 해제 후에도 유지합니다.

피드 제공 이미지와 사이트 파비콘을 우선 사용합니다. 실패하면 기본 RSS 심볼로 표시하고, 사용자가 이모지나 번들 Phosphor 아이콘과 색상으로 변경할 수 있습니다. 파비콘은 해당 사이트에서 직접 요청하며 외부 favicon 프록시에 구독 주소를 전송하지 않습니다.

DataStore와 WorkManager로 15분~24시간 백그라운드 폴링, 앱 열 때 확인, 전경 1분/5분 확인, Wi-Fi 제한을 유지합니다. 운영체제 절전·네트워크 상태에 따라 실제 확인은 늦어질 수 있습니다. 시스템/라이트/다크 및 Android 12 이상 동적 운영체제 색상을 지원합니다.

**현재 범위:** 실시간 수신 서버는 미연결이며 실제 갱신은 폴링입니다. RSS가 요약만 제공하면 그 내용까지만 표시합니다. 목록은 기존 SQLite cache snapshot 위의 점진적 표시이며 DB cursor/PagingSource 방식의 대규모 페이징은 아직 아닙니다. 직접 당김과 기본 터치 입력은 네이티브 테스트 대상으로 두며 실기기 촉감·햅틱과 세부 접근성 검증은 별도입니다.

HTTP 피드를 위해 cleartext를 허용하지만 HTTPS→HTTP 자동 리디렉션과 URL 안의 계정정보는 허용하지 않습니다. 글 안의 JavaScript나 외부 XML 엔티티는 실행하지 않습니다.

## 빌드·테스트·릴리즈

JDK 17, Android SDK Platform 37.0, Build Tools 36.0.0:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest
```

`main`의 GitHub Actions가 단위 테스트·lint·빌드와 API 35의 compact/wide/short UI·SQLite 테스트를 실행합니다. 성공 후 [Releases](https://github.com/hanana-kick/Arrrr/releases)에 **설치 가능한 debug 서명 프리뷰 APK**, SHA-256, 네이티브 스크린샷 ZIP을 올립니다. production/Play 서명은 구성하지 않았습니다.

0.3.2는 동일한 applicationId를 유지하고 DB 스키마만 v1에서 v2로 마이그레이션하며 versionCode는 5입니다. CI는 이전 0.2.0 APK와 서명 인증서가 같은지 검사하여 업데이트 호환성이 끊긴 APK를 배포하지 않습니다. 첫 실행은 빈 구독 목록이며 테스트 기사는 androidTest에만 포함됩니다.

## 구조

`data/`: SQLite·DataStore / `network/`: HTTP·안전한 RSS·Atom 파서 / `sync/`: WorkManager / `ui/`: 통합 필터·목록·기사 페이지·구독·설정 / `ui/theme/`: Material Expressive ColorScheme.

[디자인 대응](docs/DESIGN.md) · [릴리즈 노트](docs/RELEASE_NOTES.md) · [아이콘 라이선스](THIRD_PARTY_NOTICES.md)
