# Arrrr · RSS Reader

Android RSS Reader UI, built **only with official Jetpack Compose Material 3 Expressive UI components**. Kotlin · AndroidX · Material You.

## 구현된 화면

- **피드:** 확장형 앱 바, 읽지 않은 글 배지, 카테고리 필터, 대표 기사 카드, 기사 목록, 검색
- **저장:** 북마크 추가·제거, 저장된 글 목록, 빈 화면 상태
- **구독:** 구독한 피드 목록, 피드 추가 대화상자, 구독 해제
- **읽기:** 본문 화면, 읽음 상태, 북마크, 뒤로 가기
- **테마:** `MaterialExpressiveTheme`, 시스템 다크 모드, Android 12+ 동적 색상

**현재는 UI 프로토타입입니다.** 기사와 초기 피드는 로컬 샘플 데이터입니다. 등록한 RSS 주소의 네트워크 다운로드·파싱, 데이터 영구 저장, 백그라운드 동기화는 아직 구현되지 않았습니다. 저장·검색·카테고리 필터·구독 추가/삭제는 앱 실행 중 작동합니다.

## 빌드

Android Studio에서 프로젝트를 열거나 JDK 17 + Android SDK Platform 37.0을 설치한 후:

```sh
git clone https://github.com/hanana-kick/Arrrr.git
cd Arrrr
./gradlew :app:assembleDebug
```

GitHub Actions의 [Android CI](https://github.com/hanana-kick/Arrrr/actions) 성공 실행에서 디버그 APK를 아티팩트로 다운로드할 수 있습니다.

## 기술 구성

| 구분 | 버전 |
| --- | --- |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.1 |
| Kotlin / Compose Compiler | 2.4.10 |
| Compose BOM | 2026.09.00 |
| Material 3 (Expressive) | 1.5.0-alpha29 |
| Activity Compose | 1.13.0 |
| Android SDK | min 26 · target 37 |

UI는 `androidx.compose.material3`의 공식 컴포넌트만 사용합니다. 아이콘은 Google 공식 Compose Material Icons를 사용하고, Compose Foundation은 레이아웃에만 사용합니다. 별도 서드파티 UI 라이브러리는 없습니다.

API 문서: [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) · [ShortNavigationBar](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)

디자인 설명: [docs/DESIGN.md](docs/DESIGN.md)
