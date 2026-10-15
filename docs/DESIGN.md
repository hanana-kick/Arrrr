# Doop → Android 0.3

원본 캔버스: https://doop.design/c/6DyH2NjWm7
최신 기준: unified-reading-sheet (부분 시트 확장과 두 장 전환).

## 구성

전역 목적지는 읽을거리/구독/설정. 저장은 필터. 필터는 저장·읽음상태·출처·정렬·검색의 교집합이며, 열려 있는 읽기 세션의 순서는 필터 결과에서 복사하여 유지한다. 읽음/저장을 바꾸어도 본문은 닫지 않는다.

배경과 목록 viewport는 시스템바 뒤까지 이어지고 콘텐츠 시작/끝 여백과 조작 위치에만 WindowInsets를 적용한다. 작은 창의 큰 목록 제목은 목록 안에 있고, 스크롤하면 앱바에 작은 제목을 남긴다. 낮은 창에는 큰 제목을 넣지 않는다.

넓고 충분히 높은 창에서는 AndroidX adaptive list-detail scaffold가 좌우 pane과 접힘 정보에 따른 배치를 담당한다. 낮은 창은 레일과 단일 읽기영역을 사용한다. 기기 모델명을 레이아웃 분기에 사용하지 않는다.

## 읽기 Surface

기본 MaterialExpressiveTheme의 Surface/shape/typography/Button/Switch/Card/HorizontalFloatingToolbar를 사용한다. 이전 OutlinedCard 일괄 적용과 장식용 구분선을 복원하지 않는다. 두 장의 이동·초기 시트 높이·임계값은 사용자 요청을 위한 제품 인터랙션으로, 공식 Material token인 것처럼 부르지 않는다.

작은 화면에서 초기 top=min(height*0.22,200dp), 낮은 창에서는 최대72dp. 위쪽 직접 스크롤은 먼저 시트를 확장하고 남은 delta를 본문에 전달한다. 본문 경계에 도달한 뒤의 직접 touch delta로만 rubber-band를 누적한다. 관성·프로그램 스크롤·휠은 글 넘김을 시작하지 않는다. 모션축소에서는 버튼으로 넘긴다.

현재 Surface와 다음 Surface는12dp 간격을 두고 함께 움직인다. 전환 확정 전에는 목록 선택을 바꾸지 않는다. 미달은 복귀, 확정은 다음 글, 이전 방향은 저장한 LazyList index/offset 복원. 첫/마지막에서는 순환하지 않는다. 툴바는 본문 위에 있고 분리 중에만 감춘다.

큰 제목은 본문에, 한 줄 축소 제목은 앱바에 표시한다. 폰트 배율은 시스템 배율을 유지하며 본문 설정을 추가한다. 읽기 색상은 앱 ColorScheme과 별도로 저장된다.

## 데이터/테스트 경계

기존 DB schema1을 유지하여 구독/글/북마크를 지우지 않는다. 목록은 기존 캐시 snapshot 위의 점진적 표시와 LazyColumn 가상화다. 이것을 DB keyset pagination이나 네트워크 PagingSource라고 부르지 않는다. 대형 캐시 최적화와 push 연결은 후속 작업이다.

단위테스트는 조건 교집합·저장 유지·고정 읽기순서·첫/마지막 경계·rubber-band/임계/입력 제한을 검사한다. 계측테스트는 실제 터치 스크롤·시트 확장·다음 전환·이전 위치·테마 및 DataStore 복원을 검사한다. CI 화면 크기는360×800/1020×770/900×400 작업용dp에 대응하는 에뮬레이터이며 실기기 인증이 아니다.
