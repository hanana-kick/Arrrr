package com.hananakick.arrrr.reader

/** In-memory editorial examples for the RSS reader UI preview. */
enum class Topic(val label: String) {
    ALL("전체"),
    TECH("기술"),
    DESIGN("디자인"),
    PRODUCT("비즈니스"),
    LIFE("라이프"),
}

data class FeedSource(
    val id: String,
    val title: String,
    val url: String,
    val description: String,
    val topic: Topic,
)

data class Story(
    val id: String,
    val sourceId: String,
    val title: String,
    val excerpt: String,
    val time: String,
    val minutes: Int,
    val topic: Topic,
    val paragraphs: List<String>,
)

val previewSources = listOf(
    FeedSource(
        id = "tech",
        title = "기술의 장면",
        url = "https://example.org/rss/technology.xml",
        description = "소프트웨어와 기술의 새로운 장면",
        topic = Topic.TECH,
    ),
    FeedSource(
        id = "design",
        title = "디자인 아카이브",
        url = "https://example.org/rss/design.xml",
        description = "일상의 디자인을 발견하는 시선",
        topic = Topic.DESIGN,
    ),
    FeedSource(
        id = "product",
        title = "프로덕트 저널",
        url = "https://example.org/rss/product.xml",
        description = "제품, 팀, 그리고 더 나은 경험",
        topic = Topic.PRODUCT,
    ),
    FeedSource(
        id = "life",
        title = "느린 편지",
        url = "https://example.org/rss/life.xml",
        description = "조금 더 천천히 읽을 이야기",
        topic = Topic.LIFE,
    ),
)

val previewStories = listOf(
    Story(
        id = "design-01",
        sourceId = "design",
        title = "좋은 인터페이스는 무엇을 덜어내는가",
        excerpt = "선택지를 줄이는 것이 아니라, 필요한 것들이 자연스럽게 보이게 하는 디자인에 관하여.",
        time = "25분 전",
        minutes = 6,
        topic = Topic.DESIGN,
        paragraphs = listOf(
            "우리가 매일 사용하는 서비스는 많은 기능을 품고 있습니다. 하지만 훌륭한 인터페이스는 그 모든 기능을 동시에 보여주지 않습니다.",
            "좋은 디자인은 정보의 우선순위를 세웁니다. 지금 해야 할 일에 집중할 수 있도록 화면의 무게를 조절하고, 사용자가 다음 행동을 예상할 수 있게 만듭니다.",
            "읽기 경험도 마찬가지입니다. 필요한 도구는 가까이에 두되 콘텐츠를 가리지 않아야 합니다. 중요한 것은 시선을 빼앗는 효과보다 편안한 리듬입니다.",
        ),
    ),
    Story(
        id = "tech-01",
        sourceId = "tech",
        title = "작고 빠른 앱을 만드는 생각의 순서",
        excerpt = "기술을 추가하기 전에 사용자가 실제로 기다리는 순간을 먼저 살펴보기.",
        time = "1시간 전",
        minutes = 4,
        topic = Topic.TECH,
        paragraphs = listOf(
            "빠른 소프트웨어는 많은 최적화 기법만으로 완성되지 않습니다. 어떤 작업을 굳이 하지 않아도 되는지 묻는 것으로 시작합니다.",
            "처음 화면에 필요한 정보와 나중에 불러와도 되는 정보를 구분하면 복잡한 구조 없이도 체감 속도를 높일 수 있습니다.",
            "사용자가 느끼는 속도는 응답 시간만이 아니라 예측 가능성과도 연결됩니다. 명확한 상태, 단순한 이동, 일관된 동작이 중요합니다.",
        ),
    ),
    Story(
        id = "product-01",
        sourceId = "product",
        title = "사람들이 다시 돌아오는 제품의 공통점",
        excerpt = "강렬한 첫인상보다 매일의 작은 불편을 없애는 일이 중요한 이유.",
        time = "2시간 전",
        minutes = 8,
        topic = Topic.PRODUCT,
        paragraphs = listOf(
            "오래 사용되는 제품은 꼭 가장 많은 기능을 제공하지는 않습니다. 반복해서 마주치는 문제를 자연스럽게 해결할 때 습관이 만들어집니다.",
            "기능을 추가할 때마다 사용자는 새로운 개념을 배워야 합니다. 새로운 기능의 가치뿐 아니라 그것이 요구하는 주의력의 비용도 생각할 필요가 있습니다.",
        ),
    ),
    Story(
        id = "life-01",
        sourceId = "life",
        title = "아침에 읽는 다섯 분의 가치",
        excerpt = "알림 대신 내가 고른 문장으로 하루를 시작하는 작은 습관.",
        time = "3시간 전",
        minutes = 5,
        topic = Topic.LIFE,
        paragraphs = listOf(
            "많은 정보가 동시에 도착하는 하루에서, 내가 선택한 글을 읽는 시간은 작지만 분명한 쉼표가 됩니다.",
            "읽을거리를 모아두는 목적은 더 많은 정보를 소비하는 것이 아닙니다. 중요한 것을 놓치지 않으면서 나머지는 잠시 미룰 수 있게 되는 것입니다.",
        ),
    ),
    Story(
        id = "design-02",
        sourceId = "design",
        title = "여백이 콘텐츠에 주는 목소리",
        excerpt = "타이포그래피, 간격, 그리고 단정한 정보 구조가 만드는 읽기 경험.",
        time = "5시간 전",
        minutes = 7,
        topic = Topic.DESIGN,
        paragraphs = listOf(
            "여백은 남는 공간이 아니라 정보의 관계를 설명하는 도구입니다.",
            "제목과 요약 사이에 놓인 간격, 서로 다른 카드의 높이, 콘텐츠와 조작 영역 사이의 거리는 모두 읽는 속도에 영향을 미칩니다.",
        ),
    ),
    Story(
        id = "tech-02",
        sourceId = "tech",
        title = "오픈 웹을 다시 구독한다는 것",
        excerpt = "알고리즘 대신 직접 선택한 소스에서 정보를 받아보는 RSS의 매력.",
        time = "어제",
        minutes = 5,
        topic = Topic.TECH,
        paragraphs = listOf(
            "RSS는 간단한 약속에 기반합니다. 글을 발행하는 곳은 피드 주소를 제공하고, 읽는 사람은 원하는 소스만 구독합니다.",
            "목록에 무엇이 나타날지 결정하는 것은 추천 알고리즘이 아니라 나의 선택입니다. 이 단순한 구조가 개인적인 읽기 공간을 만듭니다.",
        ),
    ),
    Story(
        id = "product-02",
        sourceId = "product",
        title = "좋은 제품의 기본값을 고르는 방법",
        excerpt = "설정을 늘리기보다 대부분의 사람에게 편안한 시작점을 설계하기.",
        time = "어제",
        minutes = 6,
        topic = Topic.PRODUCT,
        paragraphs = listOf(
            "기본값은 제품이 사용자를 대신해 내리는 첫 번째 결정입니다.",
            "명확한 초기 경험을 제공하면서도 필요할 때 쉽게 바꿀 수 있어야 합니다. 자유로움과 단순함은 서로 모순되는 목표가 아닙니다.",
        ),
    ),
    Story(
        id = "life-02",
        sourceId = "life",
        title = "나만의 읽기 목록을 가꾸는 일",
        excerpt = "모으는 데 집중하지 않고 다시 펼쳐 볼 이야기를 남기는 습관.",
        time = "2일 전",
        minutes = 4,
        topic = Topic.LIFE,
        paragraphs = listOf(
            "보관함은 언젠가 읽어야 할 숙제 목록이 아닙니다. 다시 만나고 싶은 생각들을 모아두는 작은 서가에 가깝습니다.",
            "저장한 글을 가볍게 정리하고 읽은 글을 보내줄 수 있을 때, 정보는 비로소 나만의 속도로 흐르기 시작합니다.",
        ),
    ),
)
