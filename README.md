# Arrrr

Kotlin + Jetpack Compose 기반 Android 애플리케이션입니다.

## 개발 환경

- Android Studio (Android Gradle Plugin 9.4 지원 버전)
- JDK 17
- Android SDK 37 / Build Tools 36.0.0
- Gradle 9.6.1 (Wrapper)

## 프로젝트 설정

```sh
git clone https://github.com/hanana-kick/Arrrr.git
cd Arrrr
./gradlew :app:assembleDebug
```

Android Studio에서 저장소 디렉터리를 열고 Gradle 동기화 후 `app`을 실행할 수 있습니다.

첫 번째 GitHub Actions 실행에서 공식 `gradle-wrapper.jar`가 자동으로 추가됩니다. 첫 실행 이전에 로컬에서 작업한다면 설치된 Gradle로 `gradle wrapper --gradle-version 9.6.1`을 실행하여 Wrapper 파일을 생성할 수 있습니다.

## 앱 설정

| 항목 | 값 |
| --- | --- |
| 앱 이름 | Arrrr |
| Application ID | `com.hananakick.arrrr` |
| 최소 SDK | API 26 (Android 8.0) |
| 컴파일 / 타깃 SDK | API 37 (Android 17) |
| UI | Jetpack Compose + Material 3 |
| 빌드 | AGP 9.4.0 · Gradle 9.6.1 |

기본 실행 화면은 앱 이름만 표시합니다. 기능, 권한 및 서비스 연동은 아직 추가하지 않았습니다.
