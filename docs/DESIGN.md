# 승인된 디자인 → 네이티브 구현

원본: https://doop.design/c/6DyH2NjWm7 (01–15)

- 앱명은 화면 제목에 반복하지 않는다. 전역 목적지는 피드/저장/구독/설정.
- compact에서는 ShortNavigationBar 4항목; medium 이상에서는 NavigationRail.
- 피드·저장은 AndroidX NavigableListDetailPaneScaffold / AnimatedPane으로 창 크기와 접힘 변화에 적응한다. 선택된 글 키와 목적지 상태를 rememberSaveable로 유지.
- 구독은 가용 폭에 따른 1/2열 Card, 설정은 작은 화면의 탭과 넓은 화면의 분류/상세 패널.
- UI는 공식 AndroidX Material 3 컴포넌트, 레이아웃은 Compose Foundation. 피드 아이콘만 콘텐츠 슬롯(자동 이미지/이모지/Phosphor)으로 취급.
- 실제 Material Switch와 IconToggleButton을 사용한다. 이전 HTML 시안의 switch CSS나 수동 thumb 배치를 옮기지 않는다.
- Card radius 20dp, 간격 10dp. OS 폰트 배율을 유지하고 본문에만 사용자의 추가 배율 적용.
- 빈 목록·네트워크 실패·구독 해제 후 저장 글·동적색상 미지원 OS 상태를 포함.
- 파비콘/본문 콘텐츠를 자체 리드로 재배포하지 않으며 JS 실행이나 임의 HTML WebView를 사용하지 않는다.

프리뷰 버전의 제한: 네이티브 pane 너비 드래그 핸들, WebSub push 서버, 원문 자동 추출, OPML 가져오기/내보내기는 이번 버전에 포함하지 않았다. 기본 목록/상세 분할은 창 크기와 접힘 자세에 의해 자동 계산된다.
