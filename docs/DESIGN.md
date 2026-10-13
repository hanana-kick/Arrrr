# RSS Reader — Material 3 Expressive 화면 설계

## 원칙
- 완전한 Compose 네이티브 UI. 화면 구성 요소는 Google 공식 Material 3 컴포넌트만 사용.
- MaterialExpressiveTheme가 색상·모양·모션의 기본 기준. Android 12+에서는 사용자 동적 색상 적용.
- 외부 UI 프레임워크나 커스텀 Canvas/컴포넌트 드로잉 없음.
- 편안한 읽기 경험을 최우선: 넓은 간격, 명확한 제목 계층, 높은 텍스트 대비.

## 정보 구조

| 화면 | 위젯 | 행동 |
| --- | --- | --- |
| 피드 | MediumFlexibleTopAppBar, FilterChip, ElevatedCard, OutlinedCard | 검색, 주제별 탐색, 기사 선택, 북마크 |
| 저장 | ShortNavigationBar, OutlinedCard, ElevatedCard | 저장한 글만 보기, 저장 취소, 빈 상태에서 피드로 이동 |
| 구독 | ShortNavigationBar, OutlinedCard, ExtendedFloatingActionButton, AlertDialog | RSS 주소 등록, 삭제 |
| 읽기 | TopAppBar, FilledTonalButton, HorizontalDivider | 기사 읽기, 저장/해제, 뒤로 |

## 주요 디자인 상태
- 처음 진입: 대표 기사 1개 + 피드 목록 + 읽지 않은 글 배지.
- 카테고리 선택: 한 번에 하나의 필터만 적용.
- 글 선택: 읽음 상태로 전환, 읽기 화면 표시.
- 저장: 북마크 및 스낵바로 시각적 피드백.
- 데이터 없음: 디자인 시스템의 ElevatedCard 빈 상태 안내.
- 구독 추가: 최소한의 이름/HTTP(S) 주소 검증, 세션 중 목록 반영.
- 다크 모드: 시스템 설정에 따라 자동 적용.

## 범위
프리뷰용 기사와 피드 데이터는 `ReaderModels.kt`에 있고 네트워크 연결은 없습니다. 실제 리더 개발 시 RSS 2.0/Atom 파서, 지속 저장소, 동기화·에러 상태를 별도 계층으로 구현해야 합니다.
