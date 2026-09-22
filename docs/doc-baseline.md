# 문서-코드 검증 기준선 (Doc Baseline)

> parfait 문서(spec/plan/architecture/adr/open-questions)를 **어느 `develop` 커밋 기준으로 마지막 검증했는지** 기록하는 단일 출처(SoT).
> 사용자가 "develop 기준 문서 점검"을 요청하면, 아래 기준선부터 현재 `origin/develop`까지의 **delta(신규 머지)만** 감사하고, 끝나면 기준선을 갱신한다.

> 이 기준선과 축이 다른 또 하나의 기준선이 있다 — 이관 자체의 기준점(원본 저장소
> 커밋), [`docs/index.md`](index.md) 머리말 인용문에 있다. 이쪽은 "코드 대비 문서가
> 최신인가", 저쪽은 "이 트리를 언제 어디서 복사해 왔는가"를 잰다. 서로 다른 것을
> 재므로 하나로 합치지 않는다.

## 현재 기준선
- **repo**: `TJYG-Android` (`mash-up-kr/TEAMYG-Android`) `develop`
- **커밋**: `143cda87b` (`Merge pull request #511 from mash-up-kr/feature/ai/llm-wiki-document`)
- **요약**: **Android 소스가 한 줄도 안 움직인 첫 라운드이고, 들어온 것은 이 저장소 밖에서
  운영하던 정책 위키의 분기본이다** (delta 1건, **158파일 · 삽입 27237줄 · 삭제 0줄**, 커밋 67개).
  **머지 트리가 브랜치 팁과 같다**(충돌 해소 편집 0건). 전체 머지 목록과 첫 부모 선이 둘 다 하나라
  스택 PR 문제는 이번에 없다. **`.kt`·`.kts`·`gradle`·`xml`·`toml` 변경이 0건**이므로 유닛 **1298건**·
  계측 **46건**은 셈할 것도 없이 그대로다. 아카이브 이동 **0건**(코드가 안 바뀌었으니 대응 스펙·계획이
  없다), **parfait 문서 드리프트 0건**, 미결 **신설 1건**(OQ-P-405, `oq-next` 405 → 406).

  **#511 — 저장소 안에 위키가 생겼다.** `wiki/` 아래로 스크립트 다섯(`route`·`lint`·`check_status`·
  `ingest_cache`·`graph_signals`)과 `wikilib`, pytest 테스트 열다섯, `routing.json`, `references/`,
  `templates/`, 원본 `raw/` 45건, 콘텐츠 `pages/`(sources 39 · concepts 8 · entities 1 · synthesis 1),
  graphify 산출물 `graphify-out/`이 들어왔다. 저장소 루트에서는 `CLAUDE.md`에 「위키」 절이 붙었고
  (규칙은 담지 않고 `wiki/CLAUDE.md`로 넘긴다), `.gitignore`에 graphify 로컬 산출물 항목이,
  `.claudeignore`가 새로 생겼다. 이식 설계 문서는 `docs/superpowers/specs/2026-09-17-wiki-port-design.md`와
  그 계획이다. **Gradle 빌드·CI와는 분리돼 있다** — 빌드 스크립트를 한 줄도 건드리지 않았다.

  **드러난 것 — 같은 정책 위키가 두 벌이 됐다.** 이 문서가 사는 저장소(`team-yg-pesonal-agent`)의
  `wiki/`와 이번에 들어온 TJYG-Android 쪽 `wiki/`가 **같은 원본을 각각 ingest한 결과**다.
  소스 층은 사실상 같다 — 39건이 파일명까지 일치하고 차이는 한글 자모 정규화(NFC/NFD)뿐이다.
  갈라진 것은 **그 위의 개념 층**이다. 이쪽 `concepts/`는 19건이고 화면 단위로 잘게 쪼갠 한글 이름
  (`카메라-뷰파인더`·`무한-파르페-그리드`·`nametag-chip`)인데, 저쪽은 8건이고 굵게 묶은 영문 이름
  (`account`·`canvas`·`topping-placement-pipeline`·`mvp-scope-limits`)이다. `synthesis/`도 다르다 —
  이쪽은 `open-questions`와 lint 보고서 아홉이고, 저쪽은 `spec-version-history` 하나다. 뼈대 자체도
  다르다 — 저쪽은 `pages/`로 한 겹 내린 구조에 `routing.json`·`references/`·graphify를 갖췄고,
  이쪽은 평탄 구조에 `personal-private` 서브모듈이 붙어 있다. **어느 쪽이 정책 정본인지 정한 문서가
  없다** → OQ-P-405 신설.

  ⚠️ **이식 설계가 스스로 적은 전제가 지켜지지 않았다.** 그 문서는 "콘텐츠는 옮기지 않는다, 원본
  위키가 계속 정본이고 여기는 빈 스캐폴드로 시작해 새로 ingest한다"를 결정 표에 못 박았는데,
  실제로 들어온 것은 **같은 원본 39건을 다시 ingest한 결과물**이다. 결정 표의 문장과 머지된 트리가
  가리키는 방향이 다르고, 그 간극이 위의 두 벌 문제를 그대로 만든다.

  **이 회차가 확인한 것 — 코드 0건 라운드에도 감사할 것이 남는다.** `--stat`이 `.kt`를 하나도
  들고 있지 않으면 이 감사 체계는 대조할 심볼이 없어 그대로 통과한다. 그런데 이번 delta가 바꾼 것은
  **문서가 서 있는 땅**이다 — parfait 문서가 근거로 삼아 온 정책 위키가 이제 두 군데에 있다.
  그러므로 delta가 코드 밖이라도 ① **이 저장소가 근거로 쓰는 문서 체계를 건드렸는지** 먼저 보고,
  ② 건드렸으면 **어느 쪽이 정본인지**를 묻는다. 심볼 대조가 통과했다는 사실은 그 물음의 답이 아니다.

  📌 **배포 계보 쪽은 이번에도 develop 안이다** — 경량 태그 `1.1.0`·`1.1.1`·`1.1.2`·`1.1.3` 넷이 전부
  `origin/develop`의 조상이고, develop의 `appVersionCode`(`11`)·`appVersionName`(`1.1.3`)이 최신 태그와
  같다. develop 밖에 남은 태그는 `1.0.0` 하나뿐이다. `origin/release/*`는 계속 0개다(OQ-P-311 ①).

  📌 **원격 브랜치가 일곱에서 다섯으로 줄었다** — `feature/ai/llm-wiki`와
  `feature/ai/llm-wiki-document`가 머지 뒤 지워졌다. 남은 다섯은 `develop`·`main`·
  `chore/bump-version-1.1.4-12`·`feature/ai/set-up`·`feature/debug-mode`이고, 이 중 parfait 문서가
  걸린 것은 마지막 하나뿐이다.

  **조치**: doc-baseline 기준선·검증일·이력 · index 기준선 줄 · OQ-P-405 신설. 아카이브 이동 0건,
  spec·plan·architecture·ADR 전부 불변, **`api/` 다섯 표면 전부 불변**(원격 연동 코드 0건,
  `verified`는 서버 계약 대조일이라 건드리지 않았다). 미머지 하나(`feature/debug-mode`) 유지.

  직전 회차 요약(82회차, `e10ead2ca`): **직전 회차가 「사람이 따로 결정해야 지워진다」고 적은 잔해를
  바로 다음 라운드가 지웠고, 그 김에 한 달 넘게 끌던 스캐폴드 이관도 끝났다**(delta 2건, 40파일
  124/695 — 순감 571줄). #514가 도달 불가 캔버스 화면 셋을 Route·Screen·엔트리까지 지웠고, #513이
  A-004 초대 코드를 `YGScaffoldV2`로 옮겨 V1 호출을 0건으로 만들었다. 미결 해소 2건(OQ-P-053·
  OQ-P-239) · 신설 1건(OQ-P-404). 아래가 그 회차의 상세다.

  **직전 회차가 「사람이 따로 결정해야 지워진다」고 적은 잔해를 바로 다음 라운드가 지웠고,
  그 김에 한 달 넘게 끌던 스캐폴드 이관도 끝났다** (delta 2건, **40파일 · 삽입 124줄 · 삭제 695줄 —
  순감 571줄**, 커밋 7개). **머지 둘 다 트리가 브랜치 팁과 같다**(충돌 해소 편집 0건).
  유닛 **1298건**·계측 **46건** 둘 다 그대로다 — 청소 라운드가 두 번 연속 테스트 수를 안 움직였다.
  아카이브 이동 **0건**(선작성 스펙·계획이 없다 — 둘 다 청소·이관 티켓이다), 미결 **해소 2건**
  (OQ-P-053·OQ-P-239), **부분 해소 7건**(OQ-P-052·089·101·123·156·204·260), 마커만 **3건**
  (OQ-P-129·215·259), **신설 1건**(OQ-P-404, `oq-next` 405).

  **#514 — 청소 2차의 알맹이는 셋이다.** ① **도달 불가 캔버스 화면 셋이 삭제됐다** —
  `NavKeyCanvasEdit`·`NavKeyCanvasImageSelect`·`NavKeyCanvasMove` 와 각 Route·Screen, 엔트리 등록까지.
  커밋 제목이 `refactor: remove the three unreachable canvas entries` 다. 딸려 걷힌 것 셋:
  `NavTransition.Fade.metadata` 예외 · `LocalSharedTransitionScope` 와 **두 루트(`MainRoute`·
  `RootRoute`)의 `SharedTransitionLayout` 껍질** · `toAnalyticsScreenOrNull()` 의 화면 ID 셋
  (`C-001-edit`·`C-001-image-select`·`C-001-move`)과 그 테스트 기대값.
  ② **사용처 0 심볼 다섯 묶음이 걷혔다** — `animateToppingPlacement`·`ToppingLayoutDefaults`(OQ-P-101 ①) ·
  `AppleDesignGuideColors`(OQ-P-123 ②) · `LoadAllGalleryImageGroupsUseCase` 와
  `GalleryRepository.loadAllGalleryImages`(OQ-P-089 ②) · `BitmapUtils` 의 `mapViewToBitmap`·
  `mapBitmapToViewFloat` 와 `BitmapViewMapping.fitCenter(IntSize)` 오버로드(OQ-P-156 ③) · `String.sha256`.
  ③ **받기만 하던 파라미터·인텐트가 시그니처에서 빠졌다** — 권한 두 컴포넌트의
  `onClickGrantPermission`·`permanentlyDenied` 와 두 VM 의 `OnRequestPermission`(OQ-P-053 ③ 해소,
  항목 전체가 닫혔다) · `CameraControlComponent` 의 `zoomRatio`·`zoomRange`·`onClickZoomLevel` 과
  `OnClickZoomLevel`(OQ-P-052 ② 절반). 그 밖에 카메라 화면 프리뷰 셋이 `PreviewParameterProvider`
  하나로 합쳐지고, `businessFailure<T>`·`mapOf<NavKey, String>` 의 불필요한 타입 인자, 초대코드
  자리표시자의 이스케이프(`"%1\$s"` → `$$"%1$s"` 멀티달러 보간)가 정리됐다. `.kotlin/` 이 gitignore 에 붙었다.
  **원격 연동 코드는 계약에 닿지 않는다** — `ParfaitRemoteDataSourceImplTest` 의 타입 인자 정리뿐이라
  `api/` 다섯 표면은 전부 불변이다.

  **#513 — 마지막 V1 호출부가 옮겼다.** A-004 초대 코드가 EntryBuilder 의 `YGScaffold` 를 벗고
  Route 에서 `YGScaffoldV2` 를 쥔다. 인셋 관용구는 그대로 옮겨 왔고(`contentWindowInsets =
  WindowInsets(0.dp)` 는 스캐폴드가, `statusBarsPadding()` + `navigationBarsAndImePadding()` 은 Screen
  `modifier` 가 문다), **옮기면서 `isLoading = uiState.isSubmitting` 이 붙어 미리보기 조회 중 공통
  로딩 덮개가 처음 뜬다**(그전에는 다음 버튼 비활성만 있었다). develop 의 `YGScaffold` 호출은 이제
  **0건**이고 스캐폴드를 쥔 Route 는 **23파일**이다(`GroupListRoute` 만 호출 둘 — 본문 + 그룹 추가 오버레이).

  **드러난 것 하나 — 미결에 적어 둔 것이 다음 라운드의 작업 목록이 됐다.** 직전 회차(81)는 OQ-P-239 에
  ⚠️ 를 달며 "청소 라운드가 와도 이 문장이 참이었다 · 지우려면 사람이 따로 결정해야 한다"고 적었다.
  **같은 이슈(#502)의 2차 라운드가 바로 그것을 지웠다.** 문서 → 코드 방향이 실제로 도는 것을 본
  드문 사례이고, 그래서 이 저장소의 미결 목록은 기록이기만 한 것이 아니다. 다만 **인과를 단정할
  근거는 없다** — PR 본문에 이 문서를 가리키는 말은 없다.

  **드러난 것 둘 — 삭제가 결정을 대신했다.** OQ-P-101 ①·OQ-P-123 ②·OQ-P-215·OQ-P-260 ③ 은 전부
  "정하면 닫힌다"였는데, 판정 없이 대상이 사라져 닫혔다. **그 대가가 하나 남는다** — G-001 정책
  대조표의 "그룹 나가기 시 뒤 토핑이 당겨지며 좌/우·타입 변경" 행은 이동 프리미티브가 결선 없이
  삭제돼 **불일치로 굳었다**(이제 순간이동한다). 사용처 0 심볼의 존폐를 미루면 **닫히는 방식을
  고를 수 없다**는 것이 이번 사례다.

  **드러난 것 셋 — 지운 자리가 새 사용처 0을 만들었다.** `NavTransition.Fade` 는 유일한 소비처를
  잃고 프리셋만 남았다. `NavTransitionTest` 가 잠그는 것이 "프리셋마다 세 키가 다 있는가" 하나라
  **아무도 안 써도 테스트는 통과한다** → OQ-P-404 신설. 청소가 만든 잔해라 다음 청소 라운드의
  후보이기도 하다.

  **조치**: [navigation-flow](architecture/navigation-flow.md) NavKey·엔트리 빌더 목록 · 「화면 전환」
  예외 문단 🔁 · OQ-P-259 📌 · 토핑 생성 플로우 도달 불가 ⚠️ → ✅ · 「인자 있는 목적지」 목록 ·
  체크리스트 2번 공존 종료 · `verified` · [design-system](architecture/design-system.md) 색 트리 ·
  `screen/` 인벤토리 · 「화면 컨테이너」 이관 완료 ✅ 둘 · clickableYG 후보 목록 · `verified` ·
  [open-questions](synthesis/open-questions.md) OQ-P-052·053·089·101·123·129·156·204·215·239·259·260
  갱신 + **OQ-P-404 신설** + `verified` · [specs/README](superpowers/specs/README.md) a004 7라운드 · c106 ✅ ·
  segmentation-pipeline-hardening ✅ · [c101](superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)·
  [c102](superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) 권한 as-built ✅ ·
  [a004](superpowers/specs/archive/2026-08-12-a004-group-invite-code.md) 엔트리 절 🔁 ·
  [g001](superpowers/specs/archive/2026-08-01-g001-group-list.md) 심볼 인벤토리·토핑 배치 절·정책 대조표 ·
  [release-analytics](superpowers/specs/archive/2026-09-09-release-analytics-screen-tracking.md) 도달 불가 ID 📌 ·
  doc-baseline 기준선·검증일·이력 · index 기준선 줄. 아카이브 이동 0건, ADR 불변,
  **`api/` 다섯 표면 전부 불변**(`verified` 는 서버 계약 대조일이라 건드리지 않았다).
  미머지 하나(`feature/debug-mode`) 유지.

  ⚠️ **원격 브랜치가 둘에서 일곱으로 늘었다**(`git ls-remote`) — `chore/bump-version-1.1.4-12` 와
  `feature/ai/llm-wiki`·`feature/ai/llm-wiki-document`·`feature/ai/set-up` 넷이 새로 올라왔다.
  **전부 parfait 문서가 걸리지 않아 미머지 추적 줄에는 오르지 않는다.** 다만 앞의 하나는
  develop 의 `11`/`1.1.3` 보다 앞선 버전 표식이라 [ADR-0003](adr/0003-convention-plugins-version-catalog.md)
  as-built 가 다음 회차에 따라갈 후보다.

  직전 회차 요약(81회차, `924cb5802`): **한 줄도 동작을 바꾸지 않은 청소 라운드가, 예전부터 알던
  잔해는 그대로 둔 채 상태 노출 관용구만 두 갈래로 갈랐다**(delta 1건, 56파일 179/208 — 순감 29줄).
  ktx Bitmap 확장 전환·`delay(Duration)`·Compose 규약 정리가 알맹이였고, 유닛 1298·계측 46이 그대로라
  순수 정리임이 드러났다. 미결 신설 1건(OQ-P-403 — `Navigator` 만 Kotlin 명시적 backing field 로 갔다).
  ⚠️ 그 회차가 "청소 라운드가 와도 안 지워졌다"고 적은 OQ-P-239 는 **이번 회차에 지워졌다**(위 참고).
  아래가 그 회차의 상세다.

  **한 줄도 동작을 바꾸지 않은 청소 라운드가, 예전부터 알던 잔해는 그대로 둔 채 상태 노출
  관용구만 두 갈래로 갈랐다** (delta 1건, **56파일 · 삽입 179줄 · 삭제 208줄 — 순감 29줄**, 커밋 12개).
  **머지 트리가 브랜치 팁과 같다**(`924cb5802` 의 트리 = 두 번째 부모의 트리, 충돌 해소 편집 0건).
  유닛 **1298건**·계측 **46건** 둘 다 그대로다 — `.kt` 를 40개 가까이 만지고도 테스트 수가 안 움직인
  첫 라운드이고, 그것이 이 델타가 순수 정리라는 가장 단단한 증거다. 아카이브 이동 **0건**(선작성
  스펙·계획이 없다 — 이슈 #502 는 청소 티켓이라 설계 문서를 남기지 않았다), 미결 **신설 1건**
  (OQ-P-403, `oq-next` 404), 기존 미결 마커 **2건**, 활성 계획 스니펫 정정 **1건**.

  **#504 — 청소의 알맹이는 셋이다.** ① **Bitmap 호출이 androidx 확장으로 옮겼다** —
  `ImageSegmentationRepositoryImpl`·`UploadImagePreprocessorImpl` 의 `Bitmap.createBitmap(w, h, ARGB_8888)` 가
  `androidx.core.graphics.createBitmap(w, h)` 로, `SegmentationRecoveryNormalizer`·`UploadImagePreprocessorImpl` 의
  `Bitmap.createScaledBitmap(src, w, h, true)` 가 `Bitmap.scale(w, h)` 로 갔다. **빠뜨린 자리는 없다** —
  develop 에 남은 `Bitmap.createBitmap` 일곱은 전부 ktx 대응이 없는 오버로드다(행렬 판 셋 ·
  `IntArray` 픽셀 판 셋 · 부분 비트맵 판 하나). ② **`delay(Long)` 이 `delay(Duration)` 으로 바뀌었다**
  (`YGAlertPolicy`·`YGToastPolicy`·`StaggeredRevealState`·`SegmentationModuleInstaller`·`GroupListViewModel`·
  `GroupNickNameViewModel`·`GroupSettingViewModel`·`ImageSegmentationRepositoryImpl`) — 상수는 `_MS` 이름과
  `Long` 타입 그대로고 호출부에서 `.milliseconds` 를 붙인다. ③ **Compose 규약 정리** —
  `Arrangement.spacedBy(-1.dp)` → `(-1).dp`, `mutableStateOf(0f)` → `mutableFloatStateOf(0f)`,
  `CanvasBGEditRoute` 의 `modifier` 가 선택 인자 앞으로 갔다. 그 밖에 빈 매니페스트 넷의 안 쓰는
  `xmlns:android`, `app`·`app-preview` `colors.xml` 의 템플릿 색 일곱, `AndroidManifest` 의
  `android:label`, 고아 드로어블 `splash_icon*.xml` 둘, 안 쓰는 import 여럿이 걷혔다.
  **원격 연동 코드는 계약에 닿지 않는다** — `TokenAuthenticatorTest` 한 곳뿐이고 그것도 테스트 정리라
  `api/` 다섯 표면은 전부 불변이다.

  **드러난 것 하나 — 청소 라운드도 잔해를 못 지운다.** OQ-P-239 가 2026-08-19 부터 추적해 온
  `NavKeyCanvasMove`·`CanvasMoveRoute`·`CanvasMoveScreen` 셋은 **한 줄도 건드려지지 않았다**.
  목적 자체가 정리인 라운드가 왔는데도 그렇다. 이유가 분명하다: **엔트리 등록(`entry<NavKeyCanvasMove>`)이
  참조라서 자동 검사에 미사용 심볼로 안 잡힌다.** "지울 라운드가 정해지지 않았다"는 그 항목의 문장이
  청소 라운드 앞에서도 참이었다는 것이 이번의 확인이고, 그래서 이 잔해는 **사람이 따로 결정해야 지워진다.**

  **드러난 것 둘 — `Navigator` 만 새 관용구로 갔다.** `private val _backStack` + `get() = _backStack` 쌍이
  Kotlin 명시적 backing field(`val backStack: List<NavKey>` 아래 `field: SnapshotStateList<NavKey>`)로 바뀌었다.
  **develop 전체에서 이 형태는 여기 하나뿐**이고, 같은 일을 하는 `private val _x` 쌍은 넷이 남았다 —
  그중 `BaseViewModel` 의 `_state`/`state` 는 **MVI 베이스라 모든 ViewModel 의 상태 노출 형태를 정한다.**
  Kotlin 은 `2.4.10` 이고 이 기능을 켜는 컴파일러 인자는 `build-logic`·`gradle.properties` 어디에도 없다.
  동작은 한 톨도 안 바뀌었지만 관용구가 갈렸고 정한 문서가 없어 **OQ-P-403** 으로 열었다.

  **문서 쪽 일은 되살아날 뻔한 것을 막는 일이었다.** 활성 계획 `segmentation-preprocessing` 의 Task 10 은
  아직 미구현인데(`upscaledForSegmentation`·`computeUpscaleTarget` 둘 다 develop 에 없다) 스니펫이
  `Bitmap.createScaledBitmap` 을 적고 있었다 — 그대로 실행하면 이 라운드가 걷어낸 관용구가 되살아난다.
  스니펫을 `scale` 로 고치고 정정 사유를 달았다.

  **조치**: 계획 `2026-08-23-segmentation-preprocessing` Task 10 Step 2 스니펫 `scale` 정정 + 정정 문단 ·
  `updated` 갱신 · **OQ-P-403 신설**(backing field 관용구 두 갈래) · OQ-P-239 ⚠️(청소 라운드가 지나갔는데도
  잔존, 자동 검사가 못 잡는 이유) · OQ-P-187 📌(고아 드로어블 삭제 확인, 출처의 `app/` 경로를 `core/ui/` 로
  정정, 항목 ①②③ 은 잔존) · open-questions `verified` 갱신 · doc-baseline 기준선·이력 · index 기준선 줄.
  아카이브 이동 0건, architecture·ADR 불변(모듈 경계·의존 방향·공개 시그니처가 안 바뀌었다),
  **`api/` 다섯 표면 전부 불변**(원격 연동 코드 0건, `verified` 는 서버 계약 대조일이라 건드리지 않았다).
  미머지 하나(`feature/debug-mode`, `ed8e1ec8a`) 유지 — `git ls-remote` 로 확인했다.

  ⚠️ **코드 쪽에 잔재 둘이 남았다**(이 저장소의 일이 아니라 기록만 한다): `UploadImagePreprocessorImpl`
  의 KDoc 이 아직 "뒤따르는 `createScaledBitmap`" 이라고 적는데 코드는 `scale` 을 부른다 ·
  `NotionWebView` 에 `@SuppressLint("SetJavaScriptEnabled")` 가 근거 주석 없이 붙었다(JS 활성 자체는
  전부터 참이었고 이번에 억제만 명시됐다).

  직전 회차 요약(80회차, `f37a76540`): **선작성 문서 두 벌이 같은 날 들어왔는데, 한 벌은 한 줄도 어긋나지
  않았고 다른 한 벌은 구현이 스펙보다 두 자리 넓었다**(delta 2건, 16파일 1649/29, 커밋 24개).
  #497 로 G-001 목록 토핑이 테두리를 그리기 시작했고(`YGToppingImage.Remote.border`), 목록과 캔버스가
  열쇠를 공유하는 탓에 `ToppingBorderPlateCache` 가 열쇠당 **크기별 최대 3장의 선반**이 됐다 — 스펙과
  어긋난 조항이 0건이라 점검은 대조가 아니라 「머지 후 문서 반영」 절을 닫는 일이었다. #499 의 빌드 캐시
  측정 하니스는 `tools/build-cache-bench/` 에만 있어 앱 코드에 닿지 않지만 구현이 스펙보다 넓어
  (`check-relocatability.sh`·`report.py`/`report.html`, `--pair <A>:<B>`) 스펙 본문 다섯 절을 머지본 기준으로
  고쳤다. 유닛 1292 → 1298건, 계측 39 → 46건, 아카이브 이동 4건(스펙 2 · 계획 2), 미결 신설 1건(OQ-P-402).
  ⚠️ 하니스만 머지됐고 측정·게이트 G0 는 아직 아무도 돌리지 않았다.

  직전 회차 요약(79회차, `1b21725ba`): **로컬 커밋 단계에서 먼저 적은 문서가 머지 코드와 맞았는데 사본 수를
  하나 잘못 셌고, 직전 회차가 적은 릴리즈 브랜치는 그 문장이 커밋되기 전에 이미 원격에서 지워져 있었다**
  (delta 1건, 14파일 151/28). 그룹 목록 응답의 테두리 세 필드를 데이터 계층이 받고, 두 매퍼의 `private` 사본이
  `ToppingBorderMapper.kt#toToppingBorder` 하나로 모였다. 유닛 1288 → 1292건, 아카이브 이동 0건, 미결 신설 0건.
  ⚠️ **원격 브랜치가 있는지는 `git ls-remote` 로 확인한다** — 원격 추적 브랜치는 `fetch --prune` 없이는 지워진
  뒤에도 로컬에 남는다(OQ-P-310·311).

  직전 회차 요약(78회차, `c37dc2b4c`): **선작성 as-built 가 머지 코드와 한 줄도 어긋나지 않았고, 새로 드러난 것은
  코드가 아니라 버전 표식이다**(delta 3건, 8파일 133/3). 유닛 1282 → 1288건, 아카이브 이동 0건, 미결 신설 0건.
  아래가 그 회차의 상세다. ⚠️ 그 상세 중 원격 `release/version-1.1.3-10`·`-11` 에 대한 문장은 위 79회차 요약이 정정한다.

  **선작성 as-built 가 머지 코드와 한 줄도 어긋나지 않았고, 새로 드러난 것은 코드가 아니라 버전 표식이다**
  (delta 3건, **8파일 · 삽입 133줄 · 삭제 3줄**, 누적 diff). **머지 셋 전부 트리가 브랜치 팁과 같다**(충돌 해소
  편집 0건). 유닛 1282 → **1288건**(+6: `CustomCameraViewModelTest` 3 · `CustomGalleryPickerViewModelTest` 3),
  계측 **39건** 그대로다. 아카이브 이동 **0건**(선작성분이 이미 아카이브된 사후 스펙 둘의 as-built 절이었다),
  미결 **신설 0건**(`oq-next` 402 유지). **원격 연동 코드가 0건**이라 `api/` 는 손대지 않았다.

  **#489 — 권한을 한 번도 묻지 않던 두 화면이 진입할 때 묻는다.** 문서 저장소 PR #406 이 머지 직전에
  [c101](superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)·[c102](superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)
  스펙에 as-built 절을 먼저 붙여 두었고, 머지 코드와 다시 대조한 결과 **어긋난 자리가 0건**이다. 두 VM 의
  `requestPermissionOnce`·`hasRequestedPermission`, 카메라 요청 결과 인텐트가 요청을 보내지 않는 것, 갤러리
  PARTIAL 비요청, `CameraPreviewViewComponent(hasPermission)` + `DisposableEffect(lensFacing, hasPermission)`,
  커밋별 파일 분담(`1d25a4bb9` 는 Route 무변경, `98286f73e` 가 Route 한 줄), 테스트 수(카메라 0 → 3 · 갤러리
  8 → 11)까지 문서와 같다. 남은 일은 **미머지 표기를 걷는 것**뿐이었다 — OQ-P-053 해소 메모가 이 회차에 걷으라고
  스스로 적어 두었다. `:feature:camera:impl` 이 결선 여섯 주 만에 첫 유닛 테스트 소스셋을 얻어
  [module-structure](architecture/module-structure.md) 에 📌 를 달았다. ⚠️ OQ-P-053 ③(쓰이지 않는 파라미터)은
  그대로이고, 영구 거부 경로와 갤러리 경로는 실기기 확인 기록이 없다(두 스펙 「검증」 절에 적혀 있다).

  **#488·#490 — 1.1.3 이 코드 10 과 11 두 벌이다.** #488 이 `9 → 10`·`1.1.2 → 1.1.3` 을 올렸고, 한 시간 반 뒤
  #489 가 머지된 다음 #490 이 **이름은 두고 코드만** `10 → 11` 로 올렸다. 버전 이력에서 코드만 오른 첫 커밋이다.
  `origin/release/version-1.1.3-10` 은 #488 머지 커밋(권한 수정 전 트리)이고 `-11` 은 develop HEAD 다. 둘 다
  develop 커밋이라 **OQ-P-311 ①은 이번에도 성립하지 않는다**(release 만 가진 커밋 0). 경량 태그 `1.1.3` 은
  11 만 가리키므로 **코드 10 을 되짚는 표식은 브랜치 이름 하나뿐이다**(OQ-P-310). 다시 뗀 이유는 PR 본문 셋
  어디에도 없다 — #489 를 담으려 한 것으로 읽히지만 확인된 사실은 아니다. 직전 회차가 "로컬에만 있다"며 범위
  밖에 둔 `-10` 은 원격에 올라왔다.

  **조치**: c101·c102 스펙 미머지 표기 걷기 + `verified` 2026-09-11 · [specs/README](superpowers/specs/README.md) 두 행 ·
  [open-questions](synthesis/open-questions.md) OQ-P-053 상태·📌·해소 메모 · module-structure 📌 ·
  [ADR-0003](adr/0003-convention-plugins-version-catalog.md) 📌 버전 as-built(#490 11/1.1.3) ·
  [adr/README](adr/README.md) 0003 행 · OQ-P-310·311 📌 각 1덩이 · doc-baseline 검증일 줄(76 → 78회차, 한
  회차치 누락 복구)·미머지 줄 재확인·이력·index 기준선 갱신. 미머지 하나(`feature/debug-mode`) 유지.

  직전 회차 요약(77회차, `95b7fc4d5`): **선작성 문서가 브랜치 팁 그대로 들어왔는데, 어긋난 자리는 본문이
  아니라 문서 둘레에 있었다**(delta 1건, 29파일 2109/458). 유닛 1229 → 1282건, 스펙 1·계획 1 아카이브 이동,
  미결 신설 3건(OQ-P-399~401). 아래가 그 회차의 상세다.

  **선작성 문서가 브랜치 팁 그대로 들어왔는데, 어긋난 자리는 본문이 아니라 문서 둘레에 있었다**
  (delta 1건, **29파일 · 삽입 2109줄 · 삭제 458줄**, 커밋 15개). **머지 트리가 브랜치 팁 `3217e62f7`과 같다**
  (충돌 해소 편집 0건). 유닛 1229 → **1282건**(+53), 계측 **39건** 그대로다. **선작성 스펙 1·계획 1을
  아카이브로 옮겼고**, 미결은 **신설 3건**(OQ-P-399~401, `oq-next` 399 → 402)이다. **원격 연동 코드가 0건**이라
  `api/` 는 손대지 않았다.

  **본문은 맞았다.** 스펙 API 절의 선언(`harvestSubjects`·`harvestForeground`·`PlateSource`·
  `normalizeForDetection`·`isLongSideCapped`·`projectAlpha` 등)이 develop 코드와 맞고, as-built 배너가 적은
  모듈별 테스트 수(`:domain` 133 · `:data` 534 · `:feature:segmentation:impl` 75)도 다시 세어 보니 같았다.
  브랜치 단계에서 문서를 as-built 로 먼저 고쳐 둔 덕이다(문서 저장소 PR #404).

  **어긋난 자리는 넷이었다.** ① 스펙 frontmatter `related_code` 가 수확 코드를 옮기기 전의 이름 다섯을 들고
  있었다(`toCandidatePairs`·`buildCandidatePair`·`postProcess`·`toForegroundCandidate`·`originalCandidate`, develop 에서
  0건). 본문은 옮긴 뒤를 적는데 목록만 설계 시점에 멈춰 있었다. ② 같은 PR 의 마지막 커밋 `3217e62f7` 이 실패 화면
  「편집 없이 사용」을 「직접 편집」(C-104 직행)으로 바꿨는데, **내비게이션 문서에 그 갈래가 없었다.** 세그멘테이션
  Route 가 `TOPPING_EDIT_RESULT_KEY` 를 받는 쪽으로 새로 섰다. ③ OQ-P-153 ④와 OQ-P-344 ①이 「편집 없이 사용」을
  우회로로 적고 있었다. ④ 선행 스펙 `segmentation-preprocessing` 이 512 확대를 "미착수"로만 적고, 재시도 경로에
  들어온 사실을 담지 못했다.

  **이 회차가 새로 드러낸 것은 판정 수단 쪽이다.** 스펙은 조건부 세 항목의 철회 조건을 "실사용 로그"로 두었는데,
  `repositoryLogger` 의 출력처가 Kermit `platformLogWriter` 하나라 **로그가 logcat 밖으로 나가지 않는다**(OQ-P-399).
  회복 경로를 강제로 태우는 수단도 넣지 않았으므로 철회 근거가 들어올 길이 둘 다 막혀 있다. 실기기 확인은
  회복 경로와 1차 경로 회귀 모두 0회다(OQ-P-400). 「직접 편집」은 디자인 확정본과 다른 이름인데 근거가 문서에
  없다(OQ-P-401).

  계보는 이 회차에 달라지지 않았다. 원격에는 `release/*` 브랜치가 남아 있지 않고 태그는 직전 기준선의 `1.1.2`
  까지다. 로컬에만 버전 1.1.3·코드 10 범프 커밋을 얹은 `release/version-1.1.3-10` 이 있는데, 푸시 전이라 이번
  범위 밖이다.

  **조치**: 스펙·계획 각 1건 아카이브 이동(링크 보정) + 두 README 행 이동 · 스펙 `related_code` 정정과 as-built
  배너에 머지 사실 추가 · 계획 as-built 배너 신설 ·
  [c103-error-use-original](superpowers/specs/archive/2026-09-05-c103-error-use-original.md) 🔁 배너에 머지·심볼·문구 반영 ·
  [navigation-flow](architecture/navigation-flow.md) 「직접 편집」 갈래 · [data-layer](architecture/data-layer.md) 링크 ·
  [ADR-0012](adr/0012-mlkit-subject-segmentation.md) As-built 절 신설 · segmentation-preprocessing 📌 둘과 심볼 이동
  각주 · [open-questions](synthesis/open-questions.md) 📌 다섯(OQ-P-150·153·278·282·344) + 신설 셋 · doc-baseline·index
  기준선 갱신. 미머지 하나(`feature/debug-mode`) 유지.

  직전 회차 요약(76회차, `69bbbe68`): **버전만 오른 회차인데, 새로 알아낸 것은 버전이 아니라 계보다**(delta 1건,
  1파일 2/2). `release/version-1.1.2-9` 가 develop HEAD 그 커밋이어서 OQ-P-311 ①이 최신 계보에서 성립하지
  않았다. 아래가 그 회차의 상세다.

  **버전만 오른 회차인데, 이 회차가 새로 알아낸 것은 버전이 아니라 계보다**
  (delta 1건, **1파일 · 삽입 2줄 · 삭제 2줄**, `.kt` 0건). `appVersionCode` 8 → **9**,
  `appVersionName` 1.1.1 → **1.1.2**, 프리뷰 두 값은 그대로다. **머지 트리가 브랜치 팁과 같다**
  (충돌 해소 편집 0건). 테스트는 유닛 **1229건**·계측 **39건** 그대로고, 아카이브 이동 **0건**,
  미결 **신설 0건**(`oq-next` 399 유지)이다. **원격 연동 코드가 0건**이라 `api/` 는 계약 절도
  Android 표면도 손대지 않았다.

  **`origin/release/version-1.1.2-9` 가 `origin/develop` HEAD 그 커밋이다**(양방향 0커밋).
  직전 `release/version-1.1.1-8` 도 그랬다. **OQ-P-311 ①(문서가 검증한 트리와 배포된 트리가
  다르다)이 최신 계보에서는 성립하지 않는다** — 그 항목이 43·50커밋씩 벌어진 갈림을 세던 시절과
  지금의 관행이 다르다. 지금은 develop 에서 릴리즈 브랜치를 떼고, **버전을 올린 그 커밋을
  develop 이 PR 로 되받는다.** 그래서 이 회차의 delta 가 곧 릴리즈 브랜치의 내용이다.
  ⚠️ **②(계보를 둘로 둘 것인가)는 관행으로만 답했다** — 어디에도 적힌 규칙이 아니고,
  `feature/debug-mode` 는 여전히 release 쪽에만 있다.

  경량 태그 `1.1.2` 가 새로 붙었다. **여전히 경량이지만 가리키는 곳이 develop 커밋이다** —
  태그가 release 쪽 커밋을 가리켜 develop 에서 되짚을 수 없던 성질(OQ-P-311)이 이 축에서도 사라졌다.
  다만 **OQ-P-310 ②는 그대로다** — 다음 올림을 강제하는 것은 아무것도 없고, 값은 손으로 고친다.

  **버전 축을 지키는 테스트는 0건이다.** `DeviceInfoTest` 가 `1.1.1` 을 들고 있으나
  `buildDeviceInfo` 인자로 직접 넣는 **픽스처**라 카탈로그와 무관하다 — 그래서 이 범프로 깨지지
  않았고, 같은 이유로 **카탈로그가 잘못 오르는 것도 아무도 잡지 않는다**(OQ-P-233 이 묻는 접미사·
  플레이버 갈림도 이 회차에서 조건이 생기지 않았다).

  **조치**: [ADR-0003](adr/0003-convention-plugins-version-catalog.md) 📌 버전 as-built 갱신
  (#470 8/1.1.1 → #483 9/1.1.2) · [adr/README](adr/README.md) 0003 행 같은 갱신 ·
  [open-questions](synthesis/open-questions.md) OQ-P-311 📌 1덩이(계보 수렴·태그 지시처) ·
  doc-baseline·index 기준선 갱신. 미머지 하나(`feature/debug-mode`) 유지.

  직전 회차 요약(75회차, `efa77150`): **선작성 문서가 하루 만에 넉 벌 들어왔고, 그중 둘이 같은
  자리를 순서대로 밟았다**(delta 5건, 78파일 2573/305). 유닛 1164 → 1229건, 계측 37 → 39건.
  선작성 스펙 4·계획 4 아카이브 이동 + 사후 스펙 2건 신규, 미결 신설 6건(OQ-P-392~397)·해소 1건
  (OQ-P-389). #477 은 새 상태 없이 `lastClosedDate` 의 변화만 보고 알럿을 띄우고, #478 은 앱 최초의
  Analytics 소비처이며, #482 는 로딩 덮개 최소 노출을 `YGScaffoldV2` 의 계약으로 만들었다.
  아래가 그 회차의 상세다.

  **선작성 문서가 하루 만에 넉 벌 들어왔고, 그중 둘이 같은 자리를 순서대로 밟았다**
  (delta 5건, **78파일 · 삽입 2573줄 · 삭제 305줄**). 유닛 1164 → **1229건**(+65), 계측 37 → **39건**
  (`YGScaffoldV2Test` 2건). **머지 다섯 전부 트리가 브랜치 팁과 같다**(충돌 해소 편집 0건).
  **선작성 스펙 4·계획 4가 아카이브로 갔고**(어느 계획도 체크박스를 갱신하지 않아 거의 전부 미체크로
  남았다 — 진행의 정본은 `git log` 다), **사후 스펙 2건을 새로 썼다**(#477 지난 캔버스 알럿,
  #482 의 화면 수정 다섯). 미결은 **신설 6건**(OQ-P-392~397, `oq-next` 392 → 398)이고 **해소 1건**
  (OQ-P-389 — 주석 셋의 스펙 경로가 실제로 고쳐졌다). **원격 연동 코드는 0건**이라 `api/` 계약 절은
  불변이고 `api/notification.md` 의 Android 매핑에만 갈래 하나가 붙었다.

  **#477 — 알림 하나를 새 상태 없이 만들었다.** 03시 마감을 처음 확인하는 순간 알럿이 뜬다. 판정
  재료가 **이미 있는 값**이다 — 오늘 캔버스 응답의 `lastClosedDate` 는 마감될 때만 새 값이 되므로 그
  변화가 곧 「새 마감」이고, 폴링이 같은 값을 다시 실어 와도 저장값과 같아 조용하다. 저장은 평문
  DataStore 에 **그룹마다 별도 키**다. 판단 셋이 서로 얽혀 있다: **처음 확인은 기준선만 세우고 알리지
  않으며**(배포 직후 이미 알던 마감이 뜨는 것을 막는다), **`markSeen` 은 띄우기로 확정된 뒤에만**
  부르고(인원 수 조회 실패까지 「봤다」로 남기면 그 마감을 영영 못 본다), 인원 수는 오늘 멤버가 아니라
  **그 마감 당시 참여자**다. 「보러가기」가 달력 탭과 다른 경로를 쓰는 이유는 새해 첫날의 마감일이
  작년 12월 31일일 수 있어서다. ⚠️ **정책 소스가 없다** — 노출 조건도 문구도 코드가 확정했다(OQ-P-392).

  **#478 — 앱이 처음으로 자기 사용을 관측하기 시작했다.** `firebase-analytics` 는 ADR-0013 으로 이미
  들어와 있었으나 부르는 코드가 없어 이벤트가 한 건도 나가지 않았다. 화면 이름은 기획의 화면 ID
  그대로이고 매핑 전체가 순수 함수 하나(`NavKey` 26개)다. **`simpleName` 을 쓰지 않는 것이 이 라운드의
  핵심 방어다** — release R8 이 이름을 뭉개 **운영 집계에서만** 값이 깨진다. 판정 기준도 최상단 키가
  아니라 **크기와의 짝**이다(`goTo` 가 조건 없이 `add` 라 같은 값 키가 겹쳐 쌓이는데 그때가 실제
  전환이다). 트래커는 `@ActivityRetainedScoped` 이고 `@Singleton` 이면 `Navigator` 보다 오래 살아
  **그 실행의 A-001 이 통째로 빠진다**. debug 는 수집 자체를 끄고 Gradle 프로퍼티로 덮어쓴다.
  ⚠️ 그래서 `IS_DEBUG` 속성은 도착 데이터에서 언제나 `false` 다.

  **#479 — 계층 위반 넷을 UseCase 다섯으로 갈랐다.** ViewModel 21개 전수조사에서 Repository 직접
  주입이 넷이었고 전부 같은 타입이었다. **앞의 넷은 위임이고, 위임이라는 사실 자체가 결정이다** —
  도메인 규칙이 없는 자리에 규칙을 지어내지 않는다. 다섯째만 조합이 있고, 그 판정 기준(「비었는가」가
  아니라 「이 알맹이를 가리키는가」)은 c106 스펙이 정한 도메인 규칙이라 화면이 아니라 UseCase 가 든다.
  ⚠️ **컴파일러가 이 규율을 검사하지 않는다** — `feature/*/impl` 은 `:domain` 전체를 본다.

  **#480 — 축소 기준이 잘린 판에서 원본 사진으로 옮겼다.** iOS 팀이 준 실측 3건이 전부 현행 상한
  1500 아래여서 옛 규칙이 **아무 일도 하지 않고 있었다** — 잘린 판은 알파 bbox 와 같아 피사체가 프레임
  일부만 차지하면 원본이 아무리 커도 상한에 안 닿는다. 새 규칙은 `1280 ÷ 원본 긴 변` 을 잘린 판에
  곱한다. **하한 256 을 입력이 아니라 결과에 거는 것**이 이 스펙의 판단이다 — 입력에 걸면 640 은
  그대로, 641 은 204 로 올라가 **더 큰 알맹이가 더 작게** 올라간다. 값의 근거는 앱 자신이다
  (`ToppingOutlineCache` 의 거리판 해상도 256). **#479 와 같은 자리를 밟는데 순서가 예고대로 풀렸다** —
  스펙이 「그 PR 이 먼저 머지되면 인자 추가 지점이 한 겹 늘어난다」고 적었고 실제로 그렇게 됐다.
  iOS 와 값을 맞추지 않는 근거는 ADR-0032 다.

  **#482 — 폴링 스펙 하나에 화면 수정 다섯이 얹혀 왔다.** 폴링은 고정 5초를 10/15/20 적응형으로
  바꾼다(주기 계산은 순수 클래스로 떼고, 변화 판정은 `refresh` 가 이미 읽어 둔 캐시와의 구조적
  동등성이며, **실패는 램프를 건드리지 않는다** — 「변화 없음」으로 세면 회복이 가장 필요할 때 가장
  늦어진다). 나머지 다섯은 별도 사후 스펙으로 적었다 — 로딩 덮개 최소 노출 500ms 가 **`YGScaffoldV2`
  의 계약이 됐고**(화면 안에 있던 것이 디자인시스템으로 올라갔다), 캔버스가 **첫 페인트 뒤에는 덮개를
  다시 띄우지 않으며**(그 판정이 두 번 무너졌다가 잡혔다 — 첫 컴포지션의 `Loaded` 오인과, 토핑도
  배경도 없는 캔버스에서 판정이 굳는 것), 정원 1 그룹은 알림 권한을 묻지 않는다(토핑 알림은 서버가
  작성자를 빼고 보내 혼자면 영영 안 온다). ⚠️ **첫 페인트 판정에는 자기 테스트가 없다**(OQ-P-395).

  **이 회차의 발견은 절차 쪽이다** — 직전 회차가 연 OQ-P-389(코드 주석이 아카이브 이동을 못 따라감)가
  **고쳐지자마자 같은 모양으로 재발했다**(OQ-P-396, 여덟 자리). 스펙 이동은 문서 저장소에서 일어나고
  주석은 코드 저장소에 있어, 옮기는 라운드가 그 주석을 건드릴 계기가 없다. 주석이 경로 대신 스펙 `id`
  를 가리키게 바꾸는 것이 반복을 끝낸다. 미머지 하나(`feature/debug-mode`, login-debug-mode 스펙)는
  여전히 develop 에 없다.

  직전 회차 요약(74회차, `acbc4b45`): **하루를 묵힌 선작성 문서 두 벌이 나란히 들어왔고, 둘이 같은
  그림의 크기를 반대 방향에서 건드린다**(delta 2건, 21파일 941/68). 유닛 1139 → 1164건, 계측 37건
  유지. 선작성 스펙 2·계획 2 아카이브 이동, 미결 신설 5건(OQ-P-387~391). #472 가 커버리지 하한을
  `domain` 의 `SubjectCoverage` 로 올렸고, #473 이 업로드 경계에 전처리를 세워 **없던 디코드가
  생겼다**. 아래가 그 회차의 상세다.

  **하루를 묵힌 선작성 문서 두 벌이 나란히 들어왔고, 둘이 같은 그림의 크기를 반대
  방향에서 건드린다**(delta 2건, **21파일 · 삽입 941줄 · 삭제 68줄**). 유닛 1139 → **1164건**(+25:
  커버리지 하한 8 · 한 번 스캔 측정 5 · 축소 판정 8 · 업로드 전처리 순증 4), 계측 **37건** 유지.
  머지 둘 다 트리가 브랜치 팁과 같다(충돌 해소 편집 0건). **선작성 스펙 2건·계획 2건이 아카이브로
  갔고**(각 계획의 자동 Task 는 전량 수행, 실기기 수동 검증 Task 만 미체크로 남았다), 미결은
  **신설 5건**(OQ-P-387~391, `oq-next` 386 → 392)이다.

  **#472 — 자동 경로에 있던 하한이 수동 편집에도 닿았다.** 편집 화면에서 영역을 전부 지우고 완료해도
  완전 투명한 이미지가 서버까지 올라가던 구멍을 막았다. 새 기준을 만들지 않은 것이 이 라운드의 핵심이다
  — 하한 상수와 판정이 `data` 의 `internal` 에서 `domain` 의 **`SubjectCoverage`** 로 올라가, 자동 후보
  필터·저장소 알파 정제·편집 화면 셋이 같은 함수를 본다. 알파 합 비교까지 정책 안에 넣어 호출부가 255를
  각자 곱하지 않는다. 측정은 **한 번의 스캔** — `trimTransparentBounds` 가 `measureSubject`(경계 + 알파 합)와
  `trimTo(SubjectMeasure)` 로 갈렸고, `IntArray` 만 받는 순수 함수라 Robolectric 없이 덮인다. 차단은
  파일을 쓰기 **전**이다(뒤로 미루면 쓸모없는 캐시 파일 두 장이 남는다). `borderOnly` 진입은 판정에서
  빠졌다 — 그 문이 막으려는 것은 방금 비운 알맹이인데 그 진입에는 알맹이를 비울 수단도, 되돌려 늘릴
  수단도 없어 걸리면 남는 길이 화면을 벗어나는 것뿐이다. **그 예외는 구현이 먼저 드러냈고 스펙이 뒤따라
  적었다.**

  **#473 — 업로드 경계에 전처리가 서면서, 없던 디코드가 생겼다.** `ImageUploadRepositoryImpl#upload` 이
  발급 직전에 `UploadImagePreprocessor` 를 부르고 파일·포맷을 **쌍으로** 받아 발급과 PUT 양쪽에 같은 값을
  넘긴다. 판정과 실행이 갈려 있다 — `UploadImagePlan.of` 가 `Passthrough`/`Reencode` 를 순수하게 내고
  (긴 변 상한은 imageType 마다 다르며 배경은 포맷까지 JPEG 로 접는다), `UploadImagePreprocessorImpl` 이
  실행한다. 그래서 결정 표가 JVM 유닛 8건으로 덮인다. 메모리는 `inSampleSize` 만으로는 부족해
  (`4032/2 = 2016 < 2048` 이라 배경 2049~4095 구간이 sampleSize 1에 걸린다) 밀도 비로 디코드 단계에서
  목표까지 내려받는다. ⚠️ **그래도 이 경로는 이제 비트맵을 만든다** — 변경 전 업로드는 바이트 복사뿐이었다.

  **이 회차가 새로 알아낸 것은 코드 주석 안에 있었다.** 구현이 `density` 오염과 로그용 재디코드를
  스스로 잡아 커밋 제목으로 남겼는데(`b58f22e6`·`d3606c97`), 그중 하나는 문서가 예측한 적 없는 함정이다 —
  `BitmapFactory` 가 `inTargetDensity` 를 결과 비트맵의 density 로 남기고, 흰 판 합성의 기본 density 와
  다르면 `Canvas.drawBitmap` 이 두 비율로 그림을 자동 축소한다. `DENSITY_NONE` 으로 되돌리는 한 줄이
  그것을 막는다.

  **반대로 주석 하나는 스펙이 철회한 근거를 그대로 들고 있다**(OQ-P-388) — 저장소 호출부가 "축소본이
  원본보다 메모리를 덜 쓰므로 폴백이 더 위험하다"고 적었는데, 스펙 「실패 처리」가 그 비교를 명시적으로
  거두었다. 변경 전 경로에 디코드가 없었으므로 비교 대상 자체가 없다. 폴백하지 않는 결정은 양쪽이 같고
  갈린 것은 근거뿐이라 동작 영향은 없다.

  **두 라운드가 만나는 자리가 이번 회차의 진짜 발견이다**(OQ-P-391) — 빈 알맹이 차단은 **편집 화면의
  원본 해상도**에서 커버리지를 재는데, 같은 알맹이가 업로드될 때는 긴 변 1500까지 줄어든다. 하한을
  통과한 알맹이가 축소 뒤에는 그 하한에 못 미치는 구간이 생기고, `borderOnly` 를 판정에서 뺀 결정이
  바로 그 영향을 받는 자리다. **하한을 어느 좌표계에서 재는지가 정해진 적이 없다.**

  **조치**: 스펙 2건·계획 2건 아카이브 이동(+ 각 README 행 이동, 링크 `../` → `../../` 보정 후
  resolve 검증, 계획은 `status: done`·`archived_reason`·자동 Task 체크박스 반영), 스펙 2건
  `status: implemented`·`related_code` as-built 정정(사라진 `coverageFloorPixels`, 쓰이지 않은
  `rotatedToUpright`), 다운스케일 스펙에 as-built 각주 둘(철회된 근거를 든 주석 · 각도 판독이
  `File#readExifDegrees` 로 선 것), architecture 둘 갱신(data-layer 에 `UtilsModule` 행·업로드 경계
  전처리 문단·`SubjectCoverage` 승격 / module-structure 에 `File.readExifDegrees`), 미결 5건 신설.
  `api/` 는 이번 delta 에 원격 연동 코드가 없어 손대지 않았다. 미머지 하나(`feature/debug-mode`) 유지.

  직전 회차 요약(73회차, `b7674e88`): **어제 벌어진 간극을 오늘 닫았는데, 닫힌 것은 계약 표이지
  호출 수가 아니다**(delta 2건, 12파일 57/6). 유닛 1137 → 1139건, 계측 37건 유지. 선작성 문서가 없어
  아카이브 이동 0건, 미결 신설 0건. `api/conventions.md` 「Android 불일치」가 5 → 4건으로 내려갔고,
  #469 가 `groupName` 을 받되 정본은 그룹 목록 캐시로 남겼다. 아래가 그 회차의 상세다.

  **어제 벌어진 간극을 오늘 닫았는데, 닫힌 것은 계약 표이지 호출 수가 아니다**
  (delta 2건, **12파일 · 삽입 57줄 · 삭제 6줄**). 유닛 1137 → **1139건**(+2: 캔버스·목록이 다른 이름을
  들 때 목록을 고르는 것 1 · 목록 캐시가 빌 때 캔버스로 상단 바를 채우는 것 1), 계측 **37건** 유지.
  머지 둘 다 트리가 브랜치 팁과 같다(충돌 해소 편집 0건). **선작성 스펙·계획이 없어 아카이브 이동은
  0건**이고, 미결도 **신설 0건**(`oq-next` 386 유지)이다. 대신 **`api/conventions.md` 「Android 불일치」가
  5 → 4건으로 내려갔다** — 직전 회차가 4번째 행을 만들었고 서버 sync 회차가 5번째를 얹었는데, 그
  다섯째가 하루 만에 걷혔다.

  **#469 — 서버가 준 `groupName` 을 앱이 이튿날 받았다.** `GetTodayParfaitResponse`(`:data`)·
  `CanvasVO`(`:domain`)에 자리가 서고 `VOMapper.toCanvasVO` 가 `GroupName` 으로 감싼다. 오늘·상세 두
  조회가 같은 매퍼를 타므로 두 경로 모두 값을 얻는다. **서버 delta 가 벌린 간극 중 가장 짧게 열려
  있던 것**이다(2026-09-07 서버 `9c13852` → 2026-09-08 앱 머지).

  **그런데 서버가 없애 주려던 왕복은 남았다.** C-001 상단 바가 읽는 그룹명의 정본을 **그룹 목록
  캐시로 두기로 했고**(ADR-0023), 캔버스가 준 이름은 그 캐시가 **아직 비어 있을 때만** 상태를 채운다
  (`groupName.ifEmpty { … }`). 근거는 캔버스 SSoT(ADR-0029)의 "값을 얻는 길은 하나"이다 — 두 저장소가
  같은 값을 정본으로 들면 그 규칙이 저장소 사이에서 깨진다. 그래서 이 필드가 실제로 값을 내는 자리는
  **목록 캐시가 빈 진입**(푸시 딥링크·프로세스 재시작 복귀)이고, 그것이 서버가 이 필드를 만든 이유와
  정확히 겹친다. **계약 문서의 불일치는 닫혔고 조회 한 번은 그대로 남는다.**

  **같은 라운드가 이름을 지우던 자리도 고쳤다** — 목록에 그 그룹이 없을 때 `orEmpty()` 로 접어 빈
  문자열을 쓰던 것이 `return@collect` 가 됐다. 안 고치면 캔버스가 채운 이름이 다음 목록 방출에 곧바로
  날아가 부트스트랩이 무효가 된다. ⚠️ 뒤집으면 **목록에서 사라진 그룹의 옛 이름이 남지만**, 그 경로는
  화면을 떠나는 흐름이라 지금은 드러나지 않는다.

  **OQ-P-383 은 부분 해소다** — ①②③(필드 미독·값 폐기·왕복)은 닫혔고, **④ 404 `GROUP_NOT_FOUND` 는
  잔존**한다. 그 조회가 만든 실패 경로를 앱이 구별하지 않는다(코드에 그 상수가 0건). 값이 아니라
  실패 경로라 「Android 불일치」 표의 대상은 아니다.

  **#470 — 버전만 오른 커밋이다**(`appVersionCode` 7 → 8, `appVersionName` 1.1.0 → 1.1.1, 프리뷰 두 값
  불변). `.kt` 0건이라 테스트도 안 변했다. 손으로 올리는 관행은 그대로이고, **어떤 커밋이 실제로
  배포됐는지를 잇는 표식은 여전히 없다**(OQ-P-310).

  **조치**: `api/parfait.md`(필드 문단을 ⚠️ → ✅ 로 뒤집고 Android 매핑에 결선·정본·지우던 자리
  3문단), `api/conventions.md`(불일치 표에서 `groupName` 행 제거 + 리드 5건 → 4건·해소 문단),
  `api/README.md`(도메인 표 parfait 행 불일치 1건 → 0건, 회차 요약 ② 정정), `open-questions`
  OQ-P-383 부분 해소, `adr/0029` 📌 신설(**소유 경계는 응답에 무엇이 실렸는가가 아니라 어느 저장소가
  그 값을 책임지는가로 긋는다**), `architecture/data-layer.md` 📌, 아카이브 스펙 2건(group-ssot —
  as-built 2조항 / c001-canvas-main — "캔버스 응답에 그룹명이 없다" 취소선), `adr/0003`·`adr/README`
  버전 as-built. **`verified`는 `parfait/api/` 문서에서 건드리지 않았다** — 그 필드는 서버 계약
  대조일이고 이번 delta 는 앱 쪽 소비다.

  **이번 회차가 확인한 것** — **계약의 간극이 닫히는 것과 그 간극이 만든 비용이 사라지는 것은 다른
  일이다.** 「Android 불일치」는 "앱이 계약을 그대로 읽는가"만 묻기 때문에, 필드를 받아 두고 쓰지
  않기로 한 결정도 표에서는 해소로 보인다. 남는 비용(목록 조회 한 번)은 표가 아니라 **소유 경계를
  적은 ADR**에만 남는다.

  직전 회차 요약(72회차, `23675cc1`): **선작성 문서 한 벌이 통째로 들어왔고, 버그픽스 둘이 문서가
  단정한 적 없는 자리를 건드렸다**(delta 3건, 39파일 2102/782). 유닛 1106 → 1137건, 계측 35 → 37건.
  선작성 스펙 1·계획 1 아카이브 이동(`topping-border-distance-field`), 미결 신설 0건. #464 가 거리판
  코어를 세워 네 화면을 같은 거리장으로 모았고, 그 굵기 클램프가 **앱이 스스로 만든 첫 간극**을 냈다.
  아래가 그 회차의 상세다.

  **선작성 문서 한 벌이 통째로 들어왔고, 버그픽스 둘이 문서가 단정한 적 없는 자리를
  건드렸다**(delta 3건, **39파일 · 삽입 2102줄 · 삭제 782줄**). 유닛 1106 → **1137건**(+31: 거리판
  코어 14 · 판정 3 · 초대코드 뷰모델 18 · `InviteCode` 2 · 매핑 클램프 2 · 삭제된 알파 마스크 −8),
  계측 35 → **37건**(`ModifierCenteredAtTest` 2, `core:util:android` 에 `parfait.test.compose` 신설).
  **선작성 스펙 1건·계획 1건 아카이브 이동**(`topping-border-distance-field`). 미결은 **신설 0건**
  (`oq-next` 383 유지)이고 OQ-P-102에 📌 한 덩이가 붙었다. **미머지 표기 스무 자리를 걷었다.**

  **이 회차의 성격은 "예고가 전부 들어왔고, 새로 알아낸 것은 문서 밖에 있었다"이다.** #464는 스펙과
  계획을 하루 앞서 써 두고 실행한 라운드라 감사가 확인할 것이 거의 남지 않았다 — 머지 트리가 셋 다
  브랜치 팁과 같고, 스펙은 이미 as-built 로 고쳐져 있었다. 그래서 이번 감사가 한 일의 대부분은
  **꼬리표를 걷는 것**(미머지 → PR 번호)과 **버그픽스 둘이 문서에 없던 사실을 새로 적는 것**이다.

  **#464 — 거리장 통일이 8 Task 전량 수행으로 들어왔다.** `ToppingOutline`(`core:util:jvm`)·
  `Bitmap.toToppingOutline`(`core:util:android`)·`ToppingOutlineCache`(`core:ui`)가 서고, 네 화면이
  같은 거리판으로 그리고 판정한다. **계획과 갈린 자리는 넷**이고 스펙과는 하나다(스펙이 먼저 고쳐져
  있었다). ① 값 타입 셋과 상수가 `core:util:jvm` 의 `model/` 로 갈라지고 담기 규격이
  `ToppingOutlineSpec` 한 곳에 모였다. ② 예정에 없던 `ToppingBorderPlateCache`(전역 LRU 32칸)가
  생겼다 — 컴포저블이 다시 만들어질 때 판까지 다시 만들면 그동안 테두리를 안 그려 깜빡였다.
  ③ **지연 재생성(`BORDER_REBUILD_DELAY_MS`) 갈래가 통째로 사라졌다** — 옛 판을 늘려 그리면 굵기가
  배율만큼 틀어져 보이다가 손을 떼는 순간 스냅한다. 굵기 dp 고정이 눈에 보이는 성질이라 되돌렸고,
  그것을 조정하던 Task 8 Step 3도 함께 무효가 됐다. ④ 굵기 범위가 2~30dp 로 좁혀져 `domain` 한 곳
  (`ToppingBorder.WIDTH_RANGE_DP`)으로 모였다.

  **그 마지막 하나가 계약 문서에 새 행을 만들었다.** 읽는 쪽 `VOMapper` 둘이 `solidClamped` 를 지나
  서버가 준 `borderWidth` 를 2~30 으로 가둔다. 서버는 범위를 검증하지 않으므로 **50 으로 저장된 행을
  서버와 앱이 다른 값으로 본다** — `api/conventions.md` 「Android 불일치」가 3건에서 **4건**이 됐고,
  넷째는 서버 delta가 벌린 것이 아니라 **앱이 스스로 만든 첫 간극**이다(OQ-P-381 ②가 걷을 조건을 쥔다).

  **#465 — 문서가 충분하다고 적어 둔 규칙에 반례가 나왔다.** c106 스펙은 「같은 배율을 세 번
  표현하지 않는다」로 이미지·스트로크·핸들에 **같은 `center`·`sizeAfterScale` 을 한 번만 계산해
  넘기면 된다**고 적었는데, 토핑을 캔버스보다 크게 키우면 셋이 같은 값을 보고도 어긋났다. 값이 아니라
  **놓는 방식**이 갈렸기 때문이다 — 버튼만 `centeredAt` 이고 이미지·스트로크는 좌상단을 직접 계산해
  `offset` 으로 넘겼다. `requiredSize` 가 부모 제약을 무시하는 것은 자기 자식을 잴 때뿐이고, 그 노드가
  부모에게 보고하는 겉크기는 잘린다. Compose 는 그 잘린 겉크기 안에 내용을 가운데 정렬하므로
  (`Placeable.apparentToRealOffset`) 직접 계산한 쪽만 **넘친 양의 절반**만큼 밀렸다. 셋 다
  `centeredAt` 으로 통일했고, 계약은 계측 테스트가 두 방식을 나란히 재서 고정한다.

  **그 계약이 CI에서 실행되지는 않는다.** `core:util:android` 는 `test.yml` 의
  `assembleDebugAndroidTest` 두 줄 중 하나라 **컴파일까지는 간다** — OQ-P-102 ②가 세는 계측이 파일
  11·35건에서 **12·37건**이 됐고, 이번에 들어온 계약은 눈으로 보기 전에는 드러나지 않는 배치 규칙이라
  ②의 값어치가 또 커졌다.

  **#466 — 초대코드 입력이 텍스트 필드 하나로 합쳐졌다.** 칸 여섯이 각각 `BasicTextField` 이던 구조가
  단일 필드가 되면서 `InputMode` ADD/EDIT 분기가 통째로 사라지고 `focusedIndex` 가 `Int?` 에서 `Int` 로
  좁혀졌다. 이 라운드는 **브랜치 상태로 이미 문서에 반영돼 있었고**(PR 전이라 라운드 번호만 적었다),
  이번 감사는 그 꼬리표를 PR 번호로 바꾸는 일만 했다. 유닛 18건이 붙어 이번 delta에서 가장 많이 늘었다.

  **조치**: 스펙 1건·계획 1건 아카이브 이동(+ 각 README 행 이동, 계획은 `status: done`·
  `archived_reason`·체크박스 34개 반영, 조건부 Step 둘은 **수행하지 않은 채로 각주**를 달았다),
  계획 머리말 as-built 각주 1덩이, 아카이브 스펙 2건 갱신(c106 — 배치 방식 통일 / c301 — 스트로크·
  이미지도 `centeredAt`), a004 스펙·specs README 꼬리표 정정, architecture 2건(module-structure —
  `centeredAt` 계약·`parfait.test.compose` / navigation-flow — 지난 회차가 남긴 미머지 표기),
  api 4건(parfait-image Android 매핑에 클램프 · parfait 폴백 절 · conventions 불일치 표 4번째 행 ·
  README 도메인 표와 conventions 요약의 낡은 셈 1건 → 4건), open-questions 1항목(OQ-P-102 📌).
  **`verified`는 `parfait/api/` 문서에서 건드리지 않았다** — 그 필드는 서버 계약 대조일이고, 이번
  delta의 원격 연동 변경은 앱 쪽 매핑 클램프뿐이다.

  **이번 회차가 확인한 것** — **"같은 값을 넘긴다"는 문서의 단정은 값에 대해서만 참이다.** c106
  스펙은 어긋남의 원인을 배율 표현 방식으로 지목하고 값을 한 번만 계산하는 것으로 닫았는데, 남아 있던
  것은 **자리를 잡는 방식**이었다. 같은 문장이 두 라운드를 버틴 이유는 그 조건(자식이 부모보다 커진다)이
  평소에는 성립하지 않아서다 — **문서가 틀린 것이 아니라 조건을 안 적었다.**

  직전 회차 요약(71회차, `2285d09d`): **버그픽스 둘이 들어왔고, 하루 전에 쓴 선작성 문서 둘 중 하나만
  낡았다**(delta 2건, 14파일 302/28). 유닛 1096 → 1106건, 계측 35건 유지. 선작성 스펙 1·계획 1 아카이브
  이동, 미결 하나 신설·둘 정정(`oq-next` 377 → 378). 머지본이 스펙과는 어긋난 자리가 없고 계획과는 세
  자리에서 갈려, 감사의 절반이 계획에 as-built 각주를 다는 일이었다. 아래가 그 회차의 상세다.

  **버그픽스 둘이 들어왔고, 하루 전에 쓴 선작성 문서 둘 중 하나만 낡았다**(delta 2건,
  **14파일 · 삽입 302줄 · 삭제 28줄**). 유닛 1096 → **1106건**(+10: 캡처 홀더 4 · 캡처 캐시 2 ·
  A-004 2 · S-102 2), 계측 **35건** 유지. **선작성 스펙 1건·계획 1건 아카이브 이동**
  (`canvas-save-preview-capture-holder` — 하루 만이다). 미결은 **하나가 신설되고 둘이 정정됐다**
  (OQ-P-377 신설 / 364·365 정정, `oq-next` 377 → 378). 미머지 표기 셋을 걷었다.

  **이 회차의 성격은 "실행이 뒤집은 결정이 계획에는 안 적혔다"이다.** 캡처 홀더 라운드는 스펙과
  계획을 하루 전에 써 두고 실행했는데, **머지본은 스펙과 한 줄도 어긋나지 않고 계획과는 세 자리에서
  갈렸다.** 앞 회차가 코드리뷰 결과를 스펙에만 실어 정정했기 때문이다 — 스펙은 정본이라 고쳐졌고,
  계획은 실행 기록이라 그대로 남아 낡았다. 그래서 이번 감사가 한 일의 절반은 **계획 문서에 as-built
  정정 각주를 다는 것**이었다.

  **#463 — 캡처 홀더가 스펙대로 들어왔다.** `CanvasCaptureHolder`(`put`/`peek`/`clear`, `@Volatile`
  필드)가 캡처 비트맵을 캔버스 메인에서 저장 미리보기로 나르고, 미리보기는 `remember { peek() }`로
  받아 `Image`로 곧바로 그린다. 파일을 열어 전체 해상도 PNG를 디코드하고 크로스페이드하던 자리가
  사라졌다. **`NavKeyCanvasImageSave`는 그대로다** — 키가 나르는 것은 여전히 경로와 날짜이고, 저장
  확정은 지금도 그 파일을 읽는다.

  **계획이 뒤집힌 세 자리는 이렇다.** ① **홀더를 비우는 자리가 생겼다** — 계획의 Global Constraints는
  "앱 코드에 홀더를 비우는 호출을 넣지 않는다"였는데, 코드리뷰가 마지막 캡처 한 장이 다음 캡처까지
  사는 것을 지적해 미리보기의 `onDispose`에서 **자기 `NavKey`가 `Navigator.backStack`에 없을 때만**
  비우는 조건이 들어왔다. 비파괴라는 성질 자체는 지켜진다 — 키가 남아 있다는 것은 화면이 잠깐 내려간
  것이고(Activity 재생성·백스택 하강 후 복귀), 그때는 비우지 않는다. 닫기·저장 확정·시스템 백이
  전부 이 조건 하나로 모인다(`NavDisplay`가 `onBack = navigator::onBack`으로 시스템 백을 직접 받아
  화면의 닫기 콜백만으로는 그 경로가 안 걸린다). ② **압축 실패 분기에 유닛이 붙었다** — 계획은
  "파일 IO라 JVM 유닛으로 감싸기 어렵다"고 보고 컴파일에 맡겼는데, `Context`를 `mockk`로 세우고
  `cacheDir`에 `TemporaryFolder`를 물리면 **실제 파일 IO가 도는 채로 `compress`만 스텁할 수 있다.**
  `check`를 지우는 뮤테이션으로 실패 케이스가 깨지는 것까지 확인했다. ③ 그래서 산출이 신규 2가
  아니라 **신규 3**이고 유닛이 4건이 아니라 **6건**이다.

  **#461 — 참여 갈래의 닉네임 입력칸이 비어 있던 것을 채웠다.** 그룹 내 닉네임의 초기값은 계정 공통
  앱 닉네임을 재사용해야 하는데(위키 [[S-102-그룹-닉네임-생성-정책-v0.1]]), **참여 갈래에만 그 경로가
  없었다.** 생성 갈래는 #312부터 G-001이 `GetMyAccountFlowUseCase`를 구독해 값을 넘기고 있었고, 이번에
  A-004가 같은 모양을 갖는다 — `init`에서 구독하고 `NavKeyGroupNickName`의 세 번째 인자로 실어 보내며,
  S-102가 `@Assisted`로 받아 초기 상태에 넣는다. **새 축을 만들지 않고 있는 경로를 복제한 것**이 이
  수정의 성격이다.

  **그런데 두 갈래가 "값이 아직 없을 때"에 반대로 답한다.** 생성 갈래는
  `handleClickCreateNewGroup`이 **이동 자체를 접고** 로그만 남기고(다시 누르면 열린다), 참여 갈래는
  막지 않고 **빈 문자열로 넘어간다**(초대코드 조회를 이미 마친 뒤라 되돌리면 아무 반응 없는 실패로
  보인다는 것이 코드 주석의 근거다). 어느 쪽이 이 앱의 답인지 정한 적이 없고, 도달 조건을 세어 본
  적도 없다 — 두 갈래 다 로그만 남으므로 재현되어도 드러나지 않는다 → **OQ-P-377 신설.**

  **정정한 미결 둘은 같은 자리를 가리킨다.** OQ-P-364 ①("프로세스 사망 뒤 복원" 전제가 사실이
  아니다)과 OQ-P-365 ②(고정 파일명 덮어쓰기가 홀더에도 옮겨 갔다)는 앞 회차가 **브랜치를 보고 미리
  적어 둔 것**이라 "develop 미머지"라는 꼬리표를 달고 있었다. 그 꼬리표 셋(미결 둘 + navigation-flow
  하나)을 걷었다. OQ-P-365 ③("테스트가 없다")은 **한 칸 좁아졌다** — 새 유닛 여섯 중 둘이 쓰기의
  압축 실패·성공 갈래를 잠근다. 읽기·결과 왕복·미리보기 화면은 여전히 한 줄도 잠기지 않는다.

  **조치**: 스펙 1건·계획 1건 아카이브 이동(+ 각 README 1행씩 아카이브 표로), 아카이브 스펙 2건
  as-built 갱신(a004 — `nickName` 인자·`GetMyAccountFlowUseCase` 구독·`verified` / s102 — `@Assisted`
  셋·초기값 채움·확인 버튼 즉시 활성), architecture 1건(navigation-flow — 그룹 플로우 도식과 참여
  갈래 각주, 「인자 있는 목적지」의 낡은 `NavKeyGroupNickName(groupId)` 정정, 저장 왕복 절의 미머지
  표기), api 1건(parfait-group Android 매핑 — 참여 갈래도 같아졌다는 것과 서버로 나가는 요청 형태는
  그대로라는 것), open-questions 3항목(377 신설 / 364·365 정정). **`verified`는 `parfait/api/` 문서에서
  건드리지 않았다** — 그 필드는 서버 계약 대조일이고 이번 delta에 원격 연동 코드 변경이 0건이다.

  **이번 회차가 확인한 것** — **선작성 문서가 둘이면 정정도 둘 다에 실어야 한다.** 스펙만 고치고
  계획을 두면, 그 계획을 다음에 읽는 사람은 뒤집힌 결정을 그대로 다시 채택한다. 계획은 실행 기록이라
  본문을 되쓰지 않지만, **머리말 각주 한 덩이는 실행 중에 달 수 있었다.**

  미머지 브랜치는 **하나(`feature/debug-mode`)로 그대로다** — 이번 delta의 두 브랜치는 머지 후
  지워졌고 새로 올라온 것은 없다.

  직전 회차 요약(70회차, `5907e286`): **선작성 스펙이 예고한 화면 하나가 들어왔고, 푸시 딥링크가 두
  엣지 케이스를 닫으며 버전이 1.1.0(코드 7)이 됐다**(delta 4건, 24파일 399/130). 유닛 1091 → 1096건,
  계측 35건 유지. 선작성 스펙 1·계획 1 아카이브 이동, 미결 하나 해소(OQ-P-153)·둘 신설·다섯 정정
  (`oq-next` 374 → 376). 「편집 없이 사용」이 **새 경로를 만들지 않고** 후보 선택 경로를 재사용했고,
  `SaveEditedImageUseCase`가 `SaveBitmapUseCase`로 개명됐다. 아래가 그 회차의 상세다.

  **선작성 스펙이 예고한 화면 하나가 들어왔고, 푸시 딥링크가 두 엣지 케이스를 닫으며
  버전이 1.1.0(코드 7)이 됐다**(delta 4건, **24파일 · 삽입 399줄 · 삭제 130줄**). 유닛 1091 →
  **1096건**(+5: 「편집 없이 사용」 셋 · 되살린 태스크 판정 둘), 계측 **35건** 유지.
  **선작성 스펙 1건·계획 1건 아카이브 이동**(`c103-error-use-original` — 하루 만이고, **머지본을 줄
  단위로 대조한 결과 설계와 어긋난 자리가 없다**). 미결은 **하나가 닫히고 둘이 생겼으며 다섯이
  정정·갱신됐다**(OQ-P-153 해소 / 374·375 신설 / 344 완화 · 351·352·359·360 정정, `oq-next` 374 → 376).

  **이 회차의 성격은 "앞 회차가 남긴 숙제를 걷는 일"이다.** 직전 두 회차가 브랜치를 보고 문서에
  미리 적어 둔 것 — 「편집 없이 사용」 설계 전문과 딥링크 게이트 둘 — 이 **한 줄도 어긋나지 않고
  머지본이 됐다.** 그래서 새로 알아낸 것보다 **낡은 문장을 걷은 것이 많다**: 미머지 표기 셋,
  개명된 심볼 하나, 그리고 앞 회차가 "다음 감사 몫"이라고 이름까지 적어 둔 문구 셋이다.

  **OQ-P-153 ④가 닫혔다 — 2026-08-15에 열려 스물 몇 회차를 산 미결이다.** 누끼 실패 화면이
  「다시 시도」와 「편집 없이 사용」 두 버튼을 갖고, 뒤엣것은 원본 사진을 그대로 토핑 재료로 보낸다.
  **핵심 결정은 새 경로를 만들지 않은 것**이다 — 후보 선택 경로(저장 → 초안 기록 → `GoToConfirm`)를
  그대로 재사용하되, 원본은 잘린 판과 캔버스 판이 같은 그림이라 `saveBitmap` **한 번**으로 떨군
  경로를 두 자리에 싣는다. `persistSubject`로 보내면 원본 크기 비트맵을 한 벌 더 만들어 카메라 사진
  한 장에 수십 MB가 두 배로 뛴다.

  **같은 PR이 실패 화면의 도달 조건을 좁혔다.** 디코드 실패는 이제 실패 화면이 아니라 **뒤로 가기**로
  간다(`SegmentationEffect.GoBack`, 토스트 없음). 그 덕분에 **실패 화면은 원본 비트맵이 반드시 살아
  있는 상태에서만 뜨고**, 「편집 없이 사용」 버튼에 비활성 분기가 필요 없어진다. 실패 원인 분기
  (`SegmentationErrorKind`)는 통째로 걷혔다 — 디자인이 문구를 한 벌로 요구했고, 원인은 화면이 아니라
  로그가 든다.

  **`SaveEditedImageUseCase`가 `SaveBitmapUseCase`가 됐다**(`saveEditedImage` → `saveBitmap`).
  「편집 없이 사용」이 원본을 같은 자리로 보내면서 이름이 역할보다 좁아졌기 때문이고, 기계적 치환
  여섯 파일이다. `data-layer`의 메서드 목록과 ADR-0011·0012의 기록 자리에 개명 각주를 달았다 —
  **ADR 본문은 결정 시점의 기록이라 이름을 되쓰지 않고 각주만 얹는다.**

  **푸시 딥링크는 게이트 둘과 판정 하나를 더 얻었다.** 게이트 둘(스플래시 이탈 대기 · 세션 확인)은
  직전 회차에 브랜치 코드로 이미 문서에 적어 둔 그대로다. 새로 온 것은 **되살린 태스크 판정**이다 —
  알림이 만든 인텐트가 **태스크의 base intent**로 남아, 뒤로가기로 나간 뒤 최근 앱·런처로 되살리면
  같은 extras가 다시 온다. `setIntent(Intent())`는 액티비티 인스턴스의 필드만 비우므로 그 경로에
  닿지 못하고, `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`가 가른다(유닛 둘이 잠근다 — `app` 모듈의 두 번째
  유닛 테스트 파일이다).

  **딥링크 파싱의 문자열 축이 enum으로 옮겨갔다** — `route`는 `PushNotificationRouteType`,
  `type`은 `PushNotificationType`이 각자 `key`로 물고, `groupId`를 양수로 읽는 규칙은
  `PushDeepLink.AddTopping.parse`가 든다. **복제면이 줄어든 것이지 사라진 것은 아니다**: 서버가 값을
  바꾸면 여전히 조용히 `null`이 되어 평범한 실행과 구분되지 않는다(OQ-P-351 ③).

  **알림 상태바 아이콘이 전용 에셋으로 갈렸다.** 런처 아이콘 `ic_launcher_monochrome`은 어댑티브
  세이프존 여백을 안고 있어 24dp 규격에서 실루엣이 작게 찍힌다. 신설한 `ic_notification`은 벡터
  안에서 `group` 변환으로 여백을 조여 라이브 영역을 채우는데, **그 보정이 임시라는 것을 파일 주석이
  적는다** — 디자인이 조인 에셋을 주면 걷어낼 자리다 → OQ-P-374.

  **크림 하한이 3칸이 됐다**(`MIN_MIDDLE_COUNT` 1 → 2, 크림은 `topSection` 한 칸 + `middleSection`).
  위키 [[무한-파르페-그리드]]의 "토핑 0~3개 → 크림 3개"와 **저개수 구간이 처음으로 맞는다.** 그런데
  그 의도를 적은 것은 커밋 제목 한 줄뿐이고, **KDoc은 아직 "middle 1개"라고 적으며** 증가 규칙은
  여전히 높이 기반이라 정책의 "토핑 1개당 1칸"과 갈린다 → OQ-P-375.

  **앞 회차가 이름까지 적어 둔 숙제를 걷었다** — "등록 호출부가 0건이라 도달 불가"라는 문구가
  OQ-P-351·352·359 셋에 남아 있었고 셋 다 정정했다. PR #450으로 등록이 세션 축 넷에 걸린 뒤로
  **그 문구가 가리키던 안전지대는 없다**: 서버 문구도, 채널 id 어긋남도, `date` 미사용도 이제 곧바로
  사용자에게 닿는다.

  **이번 회차가 확인한 것** — **미결에 "다음 감사 몫"이라고 적어 두면 실제로 걷힌다.** 직전 회차가
  OQ-P-360 안에 "같은 문구가 351·359에도 남아 있다"고 **번호까지** 적어 둔 덕분에, 이번 감사는 그
  자리를 찾는 데 시간을 쓰지 않았다(352는 같은 문구를 찾다가 함께 걸렸다). 반대로 **브랜치 한 줄
  메모로는 부족하다**는 앞 회차의 관찰도 그대로다 — #455는 커밋 제목 하나뿐이라 무엇이 왜 바뀌었는지
  코드를 열어 크림 구조를 세어 보고서야 위키 정책과 이어졌다.

  직전 회차 요약(69회차, `489b14cc`): **여덟 회차를 끌어온 푸시 축이 닫혔다 — 기기 토큰 등록과 알림
  권한 안내가 develop에 섰다**(delta 1건, 42파일 944/77). 유닛 1072 → 1091건, 계측 35건 유지.
  선작성 스펙 1건 아카이브 이동, 미결 둘 해소·넷 신설·하나 갱신(`oq-next` 370 → 374). 등록과 권한이
  **별개 축**이라는 결정이 핵심이고, `register()`는 `suspend`가 아니라 `@ApplicationScope`에서 돈다.
  아래가 그 회차의 상세다.

  **여덟 회차를 끌어온 푸시 축이 닫혔다 — 기기 토큰 등록과 알림 권한 안내가 develop에
  섰다**(delta 1건, **42파일 · 삽입 944줄 · 삭제 77줄**). 유닛 1072 → **1091건**(+19: 등록구 · 등록
  UseCase · 세션 트리거 셋 · 권한 판정 · Saver 둘), 계측 **35건** 유지. **선작성 스펙 1건 아카이브
  이동**(`push-notification-permission-and-device-token` — 두 회차 만이고, **대조 결과 설계와 코드가
  어긋난 자리가 없다**). 미결은 **둘이 해소되고 넷이 생겼으며 하나가 갱신됐다**(OQ-P-341·358 해소 /
  370·371·372·373 신설 / 362 갱신, `oq-next` 370 → 374).

  **이 회차의 성격은 "예고가 그대로 들어왔다"이다.** 직전 두 회차가 미머지 브랜치를 보고 미결에 미리
  적어 둔 답(②는 세션 축 넷, ③은 A-004·A-005 완료 직후, ④는 거부 상태에서도 등록)이 **한 줄도
  어긋나지 않고 머지본이 됐다.** 지난 회차에 "브랜치 한 줄 메모는 존재만 말해 준다"고 적었는데,
  **미결이 걸린 축은 예외다** — 커밋 제목이 아니라 브랜치 코드를 직접 읽어 결정 여섯으로 스펙을
  세워 둔 자리였고, 그래서 이번 문서 일은 **대조와 상태 전환**이 대부분이었다.

  **OQ-P-341이 닫혔다 — 2026-09-02에 열려 여덟 회차를 산 미결이다.** ①(FCM을 되살릴지)은 직전 회차의
  #446·#447이 답했고, 남은 ②③④를 이번 #450이 답했다. **②(등록 호출 시점)은 세션 축 넷**이다 —
  `LoginWithKakaoUseCase`·`SignUpUseCase`의 `refreshMyAccount` 뒤, `BootstrapSessionUseCase`의 성공
  분기, `onNewToken`. **핵심 결정은 등록과 권한이 별개 축이라는 것**이다: FCM 토큰은 권한과 무관하게
  SDK가 설치 시점에 발급하므로, 등록을 권한에 매달면 재로그인·기기교체·재설치 사용자가 등록 경로에
  닿지 못한다. ④는 **거부한 사용자의 토큰도 등록한다**로 답했다(서버가 권한 상태를 모르니 발송이
  나가고 OS가 버리지만, 사용자가 나중에 설정에서 켜면 앱이 아무것도 안 해도 동작한다).

  **`register()`는 `suspend`가 아니다.** 걸어만 두고 곧장 돌아오며 실행은 `:data`의
  `DeviceTokenRegistrarImpl`이 `@ApplicationScope`에서 한다 — 부르는 자리가 로그인·가입·앱 진입이라
  호출자 스코프에 매달면 곧바로 갈아 끼워지는 화면과 함께 등록이 취소된다(브랜치 초판은 직렬
  await라 **로그인 스피너와 스플래시가 FCM `getToken()` + 등록 POST 두 왕복을 떠안았다**). 재시도는
  3회에서 멈추고 백오프는 3초·6초다 — 서버가 `token`을 유일 키로 upsert 하도록 설계해 **반복 호출이
  곧 실패 복구 수단**이라 다음 세션 트리거에 맡긴다. `Mutex`의 근거는 앱이 아니라 서버에 있다:
  같은 신규 토큰의 동시 요청은 두 번째가 유니크 제약 위반으로 500이다.

  **OQ-P-358도 닫혔고, 그 과정에서 이미 있던 결함 하나가 함께 고쳐졌다.** 권한을 묻는 자리는
  `NotificationPermissionGate`(A-004·A-005 완료 직후, 캔버스 진입 전)이고, **API 33 미만은 허용으로
  본다** — `POST_NOTIFICATIONS`가 그 아래 플랫폼에 정의돼 있지 않아 `checkSelfPermission`이 항상 거부를
  답하는데 알림은 기본으로 켜져 있다. 직전 회차에 되살아난 `ParfaitFirebaseMessagingService`가 정확히
  그 함정을 밟고 있어 **그 기기군의 포그라운드 알림을 전부 버리던 것**이 이 라운드에 사라졌다.
  ⚠️ 영구 거부 판정은 `shouldShowRequestPermissionRationale`을 **두 번 읽어 비교**하는데(한 번만 읽으면
  "아직 안 물어봤다"와 "두 번 거부됐다"가 같은 `false`라 처음 온 사용자까지 설정으로 보낸다), 그 대가로
  **이전 세션에서 이미 두 번 거부한 사용자는 버튼이 무반응으로 남는다** → **OQ-P-371 신설**.

  **이동이 안내 하나를 거쳐 가면서 새 관용구가 생겼다 — OQ-P-372 신설.** 두 Route의 `NavigateToNext`
  이펙트가 곧바로 실행되지 않고 **목적지를 들고 대기**한다. 이 저장소에서 **이펙트가 화면 상태를 거쳐
  지연되는 첫 자리**이고, 대기 값이 `rememberSaveable`인 이유는 이펙트가 `Channel`이라 구성 변경으로
  값을 잃으면 **서버에서는 그룹 생성이 끝났는데 사용자만 이전 화면에 갇히기** 때문이다. 배선은 두
  Route에 복제됐다(이펙트 타입이 달라 공용화 비용이 이득보다 크다는 판단). ⚠️ 프로세스 사망은 이
  구조가 막지 못한다.

  ⚠️ **안내를 몇 번 보여줄지는 정한 적이 없다 — OQ-P-370 신설.** 거부·"나중에"를 영속하지 않아
  허용 전까지 그룹 생성·참여 흐름마다 다시 뜬다. 영속할 자리는 이미 있다(직전 회차에 들어온
  `UserConfigRepository`). ⚠️ **게이트 자체에 테스트가 없다 — OQ-P-373 신설**(모듈에 `androidTest`
  소스셋이 없다). JVM으로 덮인 것은 버전 갈림과 두 Saver 왕복뿐이라, **정확히 OQ-P-371이 가리키는
  갈래가 한 줄도 안 잠겼다.**

  **알림과 무관한 축 하나가 같은 브랜치에 실려 왔다 — 이벤트 버스 개명.** `domain/event`·`data/event`가
  신설돼 세션 종료·푸시 딥링크 통로가 `repository/`에서 나왔고(둘 다 Repository가 아니다), 이름 규칙이
  **인터페이스 `~EventBus` · 구현 `~Impl`**로 통일됐다(전에는 세션만 반대였다 — `Source`가 이 저장소에서
  DataSource 계열 이름이라 오독을 부른다). **동작 변경은 없다**(Hilt 그래프 동일). 직전 회차가
  "머지 회차에 심볼명을 갱신해야 한다"고 적어 둔 자리를 이번에 갱신했다.

  ⚠️ **`:app`이 Hilt 모듈을 갖는 첫 자리가 생겼다** — `DeviceTokenModule`이다. 토큰을 읽는
  `FirebaseDeviceTokenProvider`가 Firebase SDK를 쓰는데 ADR-0013이 그 의존을 `:app`에 가둬 `:data`로
  내릴 수 없다. **갈린 기준은 도메인이 아니라 SDK 의존이 어느 모듈에 갇혀 있는가**이고, 같은 축의
  `DeviceTokenRegistrarModule`은 규칙대로 `:data`에 있다 → ADR-0004에 예외로 적었다.

  **미머지 브랜치는 둘에서 하나가 됐다**(`feature/debug-mode`). 이 저장소가 미결에 브랜치를 적어 두고
  추적해 온 축은 이제 없다.

  **이번 회차가 확인한 것** — **선작성 스펙이 있는 브랜치의 머지 회차는 감사가 아니라 대조다.**
  결정 여섯이 각각 무엇을 약속했는지 적혀 있었으므로 할 일은 코드에서 그 여섯을 찾아 맞추는 것이었고,
  틀린 곳은 표의 비고 한 칸뿐이었다(등록을 거는 자리가 `saveSession` 직후가 아니라 `refreshMyAccount`
  뒤다). **스펙이 미리 적어 둔 미결 넷이 그대로 OQ-P-370~373이 됐다는 점이 더 중요하다** — 선작성
  스펙은 무엇을 만들지만이 아니라 **무엇을 안 정했는지**도 넘겨주므로, 머지 회차의 미결 신설이
  발견이 아니라 이관 작업이 된다.

  직전 회차 요약(68회차, `bc216632`): **첫 진입 안내가 컴포넌트로 들어오면서 기기에만 남는 설정
  저장소가 처음 생겼고, 갤러리 저장이 화면 하나를 더 거치게 됐다**(delta 3건, 42파일 1535/98).
  유닛 1060 → 1072건, 계측 35건 유지. 미결 일곱 신설·하나 갱신(`oq-next` 363 → 370).
  **직전 회차가 미머지로 세어 둔 넷 중 둘이 들어왔고, 그 둘은 알림과 무관하다**
  (#449 `feature/#420-canvas-tutorial` · #445 `feature/#423-canvas-save-preview`). 문서가 두 브랜치에
  대해 미리 적어 둔 것은 **한 줄뿐이었다** — OQ-P-341의 미머지 셈에 "C-001 최초 진입 튜토리얼
  오버레이·표시 여부를 사용자 설정에 저장"이라고만 남겼다. 그 한 줄은 맞았지만 **범위를 절반도
  못 짚었다**: 튜토리얼은 C-001 하나가 아니라 **세 화면**에 붙었고, "사용자 설정"은 플래그 하나가
  아니라 **`:domain`·`:data`를 관통하는 새 저장소 축**이며, 디자인시스템에 컴포넌트 4종이 함께 섰다.
  세 번째 머지(#453)는 재시도 버튼 문구를 `다시 시도하기` → **`다시 시도`**로 맞춘 한 줄이고, 이로써
  버튼 라벨은 카메라·약관·캔버스·누끼 네 곳이 같은 문구가 됐다.

  **디자인시스템이 "화면 첫 진입 안내"를 갖게 됐다** — `ygtutorial/`의 `YGTutorialOverlay`·
  `YGTutorialBox`·`YGTutorialProgress`·`YGTutorialBoxPlacement` 넷이다. **강조할 자리만 뚫은
  오버레이가 아니라, 딤까지 구워진 알파 없는 풀스크린 목업 PNG 한 장이 실제 화면을 통째로 덮는다.**
  그래서 목업은 시스템바를 뺀 자리에만 그리고 딤은 인셋을 안 받아 화면 끝까지 이어지며, 소비 화면
  셋이 모두 오버레이를 **스캐폴드 밖 형제**로 놓는다(안에 넣으면 컨텐츠 인셋을 받아 딤이 상태바
  밑에서 끊긴다). 버튼 라벨은 호출부가 정하지 않는다 — 마지막 장이면 "시작하기", 아니면 "다음"이고
  그 판단이 `YGTutorialProgress.isLast` 하나에서 나온다(진행 표시 `3/3`인데 "다음"인 조합이 생길
  자리를 없앤 것이다). ⚠️ **목업이 실제 화면과 어긋날 길과 정책 근거 부재는 그대로 남는다** —
  버튼이 옮겨 가도 PNG는 안 따라오고, 위키에 튜토리얼 조항 자체가 없다 → **OQ-P-363 신설**.

  **기기 축 저장소가 처음 생겼다.** `UserConfigLocalDataSource`(평문 JSON 한 키 + `Flow`) ·
  `UserConfigRepository` · `UserConfigVO`(`seenTutorials` 집합) · `TutorialKind` · UseCase 둘이고,
  프록시도 함께 났다(`DataStorePreferences` — 암호화 판에서 암호화만 뺀 것). **계정 정보와 갈리는
  자리가 둘이다**: ① 암호화하지 않는다(담기는 것이 "튜토리얼을 봤는가"뿐이라 지킬 것이 없고,
  키 회전 한 번에 설정이 통째로 폐기될 위험만 남는다), ② **저장 형태가 도메인 enum이 아니라 이름
  문자열**이다(구버전이 최신 값을 읽다 모르는 항목에서 터지면 그 폐기가 설정을 다 날린다 — 문자열이면
  모르는 항목만 조용히 버린다). 즉 `TutorialKind`의 이름이 곧 저장 키라 개명이 곧 기록 소실이다.
  ⚠️ **`clearConfig()`는 계약·구현만 있고 호출부가 0건**이라 같은 기기에서 계정을 바꾼 사람이
  앞사람의 "봤다"를 물려받는다 → **OQ-P-366 신설**. ⚠️ 두 프록시가 암호화 두 줄만 빼면 KDoc까지
  같은 복제이고 `read`는 평문 쪽에서 호출부가 0건이다 → **OQ-P-367 신설**.

  **같은 기능이 화면 수에 따라 두 형태로 갈렸다 — OQ-P-369 신설.** 여러 장짜리인 C-001만
  `CanvasMainUiState.tutorialStep: CanvasTutorialStep?`을 담는데 **그 enum이 `@DrawableRes`·
  `@StringRes`를 프로퍼티로 든다**(State가 `Int`를 직접 담지 않을 뿐 표시 리소스가 State를 타고
  흐른다 — [state-management](architecture/state-management.md) 규약과
  [ADR-0016](adr/0016-domain-result-presentation-string-mapping.md)에 걸린다).
  한 장짜리 둘은 `isTutorialVisible: Boolean`만 담고 리소스는 화면이 고른다. **완료를 남기는 시점도
  갈린다** — 캔버스는 마지막 장을 닫을 때만 남기고(중간에 접으면 다음 진입에서 처음부터 다시 본다)
  나머지 둘은 누르는 즉시 남긴다. 넷째 화면이 어느 쪽을 따를지는 정해진 것이 없다.

  ⚠️ **문서의 기준과 코드의 관행이 갈렸다 — OQ-P-368 신설.** 튜토리얼 구독 셋이 전부
  `launchWhileSubscribed`로 열리는데, 이 저장소가 적어 둔 선택 기준은 **"이 구독이 서버를 계속
  부르는가"**([ADR-0029](adr/0029-canvas-today-ssot-polling.md))이고 이것은 DataStore 한 키를 읽는다.
  쓰는 곳이 셋에서 **여섯**이 됐고 그중 셋이 기준 밖이다. 딸려 오는 비용도 있다 — 이 구독은 화면이
  `state`를 보는 동안에만 열려서 **ViewModel 테스트가 `backgroundScope`에서 `state`를 수집**해야 하고,
  세 테스트가 각자 `shownViewModel()` 헬퍼로 같은 준비를 적는다.

  **갤러리 저장 사이에 화면이 하나 끼었다**(#445). `RequestCanvasCapture`가
  **`RequestCanvasCaptureForPreview`로 개명**되고, 캡처한 비트맵이 곧바로 저장으로 가지 않는다 —
  화면이 캐시에 PNG로 굽고 `NavKeyCanvasImageSave(imagePath, date)`로 미리보기를 연 뒤, 확정하고
  돌아온 결과를 받아 **같은 파일을 다시 읽어** 저장한다(보고 확정한 그림과 갤러리에 남는 그림이
  같아야 하므로 다시 캡처하지 않는다). **미리보기는 저장하지 않는다** — 결과 토스트가 뜨는 자리가
  캔버스 메인이고, 미리보기가 저장까지 하면 알림만 남기고 사라지는 화면이 되어 실패했을 때 알릴
  곳이 없다. 그래서 ViewModel이 없는 두 번째 화면이 됐다(`NavKeyWebView` 이후).
  ⚠️ **NavKey가 캐시 파일 경로를 나른다** — 비트맵을 키에 실을 수 없어 생긴 형태이고, 복원 시점에
  그 파일이 있으리라는 보장이 없다(`date`도 `:api`가 kotlinx-datetime을 안 써 문자열이다)
  → **OQ-P-364 신설**. ⚠️ **캡처 파일 이름이 고정이라 지우는 자리가 없고**, 왕복·캐시 입출력에
  테스트가 한 줄도 없다 → **OQ-P-365 신설**.

  **미머지 브랜치는 넷에서 둘이 됐다** — 들어온 것이 둘이고 새로 올라온 것은 없다
  (`feature/debug-mode` · `feature/push-notification-permission`). 남은 푸시 브랜치가 OQ-P-341 ②③④와
  OQ-P-358을 한꺼번에 답한다.

  **이번 회차가 확인한 것** — **미머지 메모의 한 줄은 범위가 아니라 존재만 말해 준다.**
  이 문서는 브랜치를 볼 때 커밋 제목에서 읽히는 것만 적어 왔는데, 그 한 줄이 맞았는데도 머지본은
  세 배 넓었다(화면 하나 → 셋, 플래그 하나 → 저장소 축, 예고에 없던 컴포넌트 4종). 알림 브랜치처럼
  **미결이 걸린 축**은 커밋 제목을 한 줄씩 대조할 값이 있지만, 그렇지 않은 브랜치의 한 줄 메모는
  **"들어올 것이 있다"는 표시**로만 읽고 머지 회차에 범위를 처음부터 훑는 편이 맞다.

  직전 회차 요약(67회차, `29c2f050`): **2026-08-22에 걷어냈던 FCM 축이 돌아왔고, 서버가 요구한
  다섯 중 셋만 맞췄다**(delta 2건, 17파일 462/0). 유닛 1047 → 1060건, 계측 35건 유지.
  미결 넷 신설·셋 부분 해소·둘 갱신(`oq-next` 358 → 362).
  **직전 회차가 "다음 회차의 후보"로 꼽은 푸시 브랜치 셋 중 둘이 들어왔다**(#446
  `feature/push-notification-deeplink` · #447 `feature/push-fcm-service`). 이번에는 **예고와 머지본이
  어긋나지 않았다** — 미결에 미리 적어 둔 "부를 수단이 브랜치에 있다"가 그대로 사실이 됐고, 대신
  **예고에 없던 축이 하나 붙었다**(딥링크). 남은 하나(`feature/push-notification-permission`)가
  안 들어온 것이 이번 회차의 성격을 정한다.

  **[ADR-0013](adr/0013-firebase-fcm-crashlytics.md)에 되살림 정정을 더했다**(철회 정정은 지우지
  않는다 — 두 결정이 다 이력이다). **되살린 근거가 걷어낸 근거와 정확히 짝을 이룬다** — 철회 사유는
  "보낼 서버가 없다"였고 서버가 실제로 보내기 시작하면서 그 전제가 사라졌다. 다만 **돌아온 것의
  이름과 자리가 달라졌다**: `fcm/YGFirebaseMessagingService` → `push/ParfaitFirebaseMessagingService`,
  채널 id는 앱이 정하던 `fcm_default_channel` → **서버가 못 박은 `parfait_default`를 따랐고**
  (OQ-P-352 ① 해소), 채널 생성 자리는 `MainActivity` → `BaseApplication`이다.

  **서버가 요구한 다섯 중 셋만 맞았다.** ✅ 채널 id · ✅ `data` 키 셋 · ✅ 딥링크 도달은 됐고,
  ⚠️ **`date`를 안 읽어 항상 최신 캔버스로 열며**(`AddTopping` KDoc이 그렇게 못 박는다),
  ⚠️ **중복 수신을 알림 두 개로 쌓고**(알림 id가 `messageId.hashCode()`),
  ❌ **토큰 등록을 한 번도 안 부른다.** 앞의 둘은 [api/conventions.md](api/conventions.md)
  "Android 불일치"에 행 둘로 올렸다(1건 → **3건**, 그중 둘은 HTTP 왕복이 아니라 **단방향 푸시의
  간극**이라 엔드포인트 셈에 안 잡힌다) → OQ-P-359 신설.

  ⚠️ **가장 큰 것은 안 들어온 것이다 — OQ-P-358 신설.** `POST_NOTIFICATIONS`가 **매니페스트 선언만**
  돌아왔고 **런타임 요청 코드는 develop에 0건**이다. 그래서 Android 13+에서는 수신부·채널·딥링크가
  다 서 있어도 **표시 단계에서 막힌다.** ADR-0013이 "되살릴 때 다시 정하라"고 남긴 물음 둘 중
  권한 쪽은 답이 아니라 **공백으로 돌아온 셈**이다. 등록 호출도 같은 부류다 — `onNewToken`이 돌아왔는데
  주석이 "등록 API 스펙이 아직 배포되지 않았다"고 적고, **그 전제는 PR #437이 이미 무너뜨렸다.**
  판정은 "부를 수단이 없다"에서 **"부를 수 있는데 안 부른다"**로 옮겼다(OQ-P-341 ②).

  **딥링크 축은 세션 종료 이동을 그대로 본떴다.** `:domain`의 `PushDeepLinkEventBus` + `:data`의
  `PushDeepLinkEventBusImpl`(`Channel(CONFLATED)`) + **앱 루트 `MainRoute` 단일 수집**이고, 접히는
  규칙의 뜻만 다르다(401 여러 건 → 로그아웃 한 번 / 알림 연달아 탭 → 마지막 한 곳). 구조가 같으므로
  **새 ADR을 만들지 않고** ADR-0013 정정과 [navigation-flow](architecture/navigation-flow.md)
  "푸시 딥링크 이동"에 적었다. 발행은 `MainActivity`의 `onCreate`·`onNewIntent` 둘이고
  (`launchMode="singleTop"`가 이번에 붙었다), 소비한 `Intent`는 `setIntent(Intent())`로 비운다.

  ⚠️ **콜드 스타트 경합이 미검증이다 — OQ-P-360 신설.** 코드 주석은 딥링크 수집이 "로그인·부트스트랩이
  끝난 뒤" 시작한다고 적지만 `LaunchedEffect(Unit)`은 **앱 루트 첫 컴포지션에 곧바로** 시작하고,
  그 시점의 백스택은 `NavKeySplash` 하나다. 스플래시가 끝나며 `replaceAll(...)`로 백스택을 갈아
  끼우므로 **딥링크가 먼저 쌓이고 리셋이 뒤따르는 순서**가 가능하다. 이동 수단이 `goTo`인 것도
  이 저장소의 다른 경계와 다르다.

  ⚠️ **앱이 서버보다 앞서 갔다 — OQ-P-361 신설.** `PushNotificationType`이 셋
  (`TOPPING`·`REMIND_AM`·`REMIND_PM`)이고 `route=group`이 그룹 목록으로 떨어지는데, **서버에 그것을
  보내는 코드가 없다**(트리거 1종). 근거로 코드가 가리키는 "FCM 페이로드 스펙 v1"은 **parfait에도
  위키에도 서버 저장소에도 없다.** 채널 설명 문구(`새 토핑, 캔버스 마감 안내 알림`)도 서버가 안 보내는
  알림을 약속한다.

  **`app` 모듈이 유닛 테스트 소스셋을 처음 가졌다.** `parfait.test.unit`이 **진입 모듈**에 붙은 것도
  처음이고(그전까지 core·`data`·`domain`과 feature `impl`에만 있었다), 그것을 가능하게 한 것은 **파싱을 `Intent`에서 떼어 둔 설계**다 — `PushDeepLinkParser`가
  원시 문자열만 받으므로 프레임워크 타입 없이 JVM에서 돈다. 잠근 것은 `groupId` 경계값 넷
  (0·음수·숫자 아님·없음) · 모르는 `type`·`route`의 폴백 · 평범한 실행이다.

  **이번 회차가 확인한 것** — **되살림은 걷어냄의 역연산이 아니다.** 이 문서는 철회 정정에 "되살릴 때
  다시 정해야 하는 것" 둘을 적어 두었는데, 되살아난 코드는 **하나를 답하고 하나를 비운 채** 왔다.
  그러므로 되살림 회차에 할 일은 "복원됐는지" 확인이 아니라 **걷어낼 때 적어 둔 물음 목록을 한 줄씩
  대조하는 것**이고, 답이 없는 자리는 "아직 안 왔다"가 아니라 **새 미결로 세워야** 다음 회차가 그것을
  본다(OQ-P-358이 그 형태다). 미머지 브랜치가 그 답을 갖고 있다는 사실은 문서가 기다릴 이유가 되지
  않는다 — develop이 지금 그 상태이기 때문이다.

  **미머지 브랜치는 여섯에서 넷이 됐다** — 들어온 것이 둘이고 새로 올라온 것은 없다
  (`feature/debug-mode` · `feature/#420-canvas-tutorial` · `feature/#423-canvas-save-preview` ·
  `feature/push-notification-permission`). 마지막 하나가 OQ-P-341 ②③④와 OQ-P-358을 한꺼번에 답한다.

  직전 회차 요약(66회차, `e6ce42b1`): **원격 이미지를 기다리는 방식이 두 화면에서 각각 정해졌고, 그 방식이 반대다**
  (delta 1건, **41파일 · 삽입 1697줄 · 삭제 224줄**). 유닛 1029 → **1047건**(+18: `reveal/` 순수 함수 11 ·
  `CanvasLoadState` 7), 계측 17 → **35건**(+18, 파일 6 → 11 — `core:ui`가 계측 소스셋을 처음 가졌다).
  **선작성 스펙·계획 없음 → 아카이브 이동 0건**, 미결은 **셋이 생기고 하나가 해소, 하나가 부분 해소,
  여섯이 갱신됐다**(OQ-P-355·356·357 신설 / 346 해소 · 348 부분 해소 / 102·112·113·167·330·349 갱신,
  `oq-next` 355 → 358).

  **#440이 세 라운드째 "미머지"로 세어 온 `feature/image-loading-placeholder`다.** 문서는 그 브랜치를
  보고 OQ-P-346·348에 미리 적어 두었는데, **머지본이 브랜치와 셋 달랐다** — `YGToppingGroup`은 한 줄도
  안 바뀌었고(결말 콜백이 안 들어왔다), 두 화면이 공유하는 것은 패키지이되 **구현이 갈리며**,
  `YGSkeleton`의 소비처는 `YGCanvas` 배경 하나다. 예고에 없던 것도 둘 붙었다(`YGDimOverlay` 분리 ·
  이미지 **다시 받기**).

  **같은 문제에 반대 답이 둘 나왔다.** C-001 캔버스는 **다 모일 때까지 아무것도 안 낸다**
  (`rememberBatchRevealState` — 성공·실패를 모두 결말로 세고, **빈 목록은 완료가 아니다**). G-001 목록은
  **아예 안 기다린다** — 자리가 먼저 열리고 그림이 뒤따르며, 대신 400ms 간격으로 아래에서 위로 쌓인다
  (`rememberStaggeredRevealState`). 둘을 잇는 것은 `core:ui` `reveal/`의 인터페이스 하나와
  `Modifier.revealed`(알파 0 + 시맨틱 제거, **측정·배치는 살려 둔다** — 그래야 감춘 자리의 이미지 요청이
  이어진다)뿐이다. 실패 처리도 반대다 — 캔버스는 **한 장만 실패해도 전체를 막고**, 목록은 실패한 것만
  폴백으로 남긴다. 위키가 부분 실패를 정한 자리는 G-001뿐이고 캔버스 쪽 조항은 없다 → **OQ-P-355 신설**.

  **`YGScaffoldV2`의 덮개가 슬롯이 됐다.** `loadingOverlay` 기본값이 종전 동작이라 **호출부는 한 곳도
  안 바뀌었고**, C-001이 같은 `isLoading` 하나로 로딩 덮개와 **다시 시도 버튼이 있는 실패 덮개**를 갈아
  끼운다. **터치를 삼키는 책임이 슬롯으로 내려간 것**이 이 변경이 옮긴 계약이라, `YGLoadingOverlay`에서
  Dim 판·터치 삼킴·병합 시맨틱을 뽑아낸 `YGDimOverlay`가 그것을 지키는 공용 판이 됐다. ⚠️ 함께
  **접근성 차단 수단이 정정됐다** — `hideFromAccessibility()`는 **그 노드 하나만** 감추고 자식 시맨틱은
  트리에 남아, 덮개 아래 버튼이 그대로 읽혔다(`clearAndSetSemantics { }`로 교체. 규칙 자체는 ygscaffold-v2
  스펙 as-built ①이 세운 그대로다 — 틀렸던 것은 수단이다).

  **G-001은 실패의 갈림이 세 번째로 뒤집혔다** — "목록이 남아 있는가"에서 **"사용자가 당겼는가"**로.
  당기는 동안 화면이 목록을 비우므로(디자인의 새로고침 프레임이 빈 컵이다) 실패를 받아 줄 자리가 에러
  화면뿐이 됐고, `ShowRefreshError` 토스트·문구·Route의 토스트 호스트가 함께 걷혔다. `isError`에 규칙이
  하나 붙는다 — **켜기만 하고 끄는 것은 성공한 조회뿐**이다. 세 번 뒤집히는 동안 **세 필드 독립은 한 번도
  안 바뀌었다**(OQ-P-112 ②). 새로고침 인디케이터는 로띠 + 안내 문구 2줄이 되어 **플랫폼 기본을 벗어났다**
  — OQ-P-113 ①이 닫혔다. ⚠️ 다만 **②(파르페 메타포 전용 에셋)는 어긋난 방향으로 채워졌다**: 화면 전용
  로띠가 이번에 처음 생겼는데 그것은 G-001이 아니라 **C-001 몫**(`YGLoadingArt.Topping`)이고, 위키가
  전용 그래픽을 요구한 화면은 여전히 공통 애셋을 쓴다.

  ⚠️ **다시 시도가 절반만 되살린다 — OQ-P-356 신설.** `retryKey`는 표시용 요청의 캐시를 건너뛰지만
  알파 마스크는 url 목록만 보는 자리라 재요청되지 않는다. 마스크가 없으면 판정이 사각형으로 떨어지도록
  설계돼 있어 못 누르게 되지는 않지만, **그림이 돌아온 뒤에도 투명한 자리가 눌린다.**
  ⚠️ **연출의 근거도 코드뿐이다 — OQ-P-357 신설**(400ms 간격 · 재진입 생략 조건 · 총 대기 상한 미정).

  **계측이 두 배가 됐는데 실행은 여전히 0회다.** 파일 6 → 11 · `@Test` 17 → 35이고 `core:ui`가 계측
  소스셋을 처음 가졌는데, CI `test.yml`은 그대로 `:core:util:android`·`:core:designsystem` 두 줄이라
  **새 모듈의 계측은 컴파일조차 안 된다.** 이번에 들어온 것들이 잠그려는 규칙(덮개 아래 접근성 차단 ·
  드러나기 전 클릭 차단 · 재시도 시 캐시 우회)은 전부 **눈으로 확인이 안 되는 종류**다(OQ-P-102 ②).

  **이번 회차가 확인한 것** — **미머지 브랜치를 보고 미리 적어 둔 것은 예고이지 사실이 아니다.**
  이 문서는 [2026-07-13] 규율대로 머지 전에 스펙을 고치지 않고 미결에만 메모를 달아 왔는데, 그 메모가
  머지 회차의 **대조표**로 쓰인다는 점이 이번에 드러났다. 세 항목이 어긋났고 셋 다 브랜치가 마지막에
  방향을 바꾼 자리였다(한 장씩 내던 것을 한 번에 내는 쪽으로, 이미지를 기다리던 것을 안 기다리는 쪽으로).
  그러므로 머지 회차에 할 일은 "예고한 항목을 지우는 것"이 아니라 **예고와 머지본을 한 줄씩 맞춰 보고
  다른 자리를 적는 것**이다 — 이번에 OQ-P-346의 해소 메모가 그 형태를 취했다.

  **미머지 브랜치는 일곱에서 여섯이 됐다** — 들어온 것이 `feature/image-loading-placeholder` 하나이고
  새로 올라온 것은 없다(`feature/debug-mode` · `feature/#420-canvas-tutorial` ·
  `feature/#423-canvas-save-preview` · `feature/push-fcm-service` · `feature/push-notification-deeplink` ·
  `feature/push-notification-permission`). 셋이 푸시 축이라 OQ-P-341·351·352가 다음 회차의 후보다.

  직전 회차 요약(65회차, `2b1dce3a`): **앱 코드는 한 줄도 안 바뀌었는데 서버 계약을 손으로 확인할 자리가 둘 생겼다**
  (delta 1건, **5파일 · 삽입 550줄 · 삭제 3줄** — 전부 `http/` 아래의 요청 모음과 사용법이다).
  유닛 **1029건**·계측 **17건** 유지(테스트 파일 무변경). **선작성 스펙·계획 없음 → 아카이브 이동
  0건**, 미결은 **하나가 생기고 넷이 갱신됐다**(OQ-P-354 신설 / 092·108·341·352 갱신,
  `oq-next` 354 → 355).

  **#451이 `notifications.http` 와 `fcm-test.http` 를 들여왔다.** 앞의 것은 우리 서버로 나가고
  (기기 토큰 등록 — 204·본문 없음, upsert 재호출, 400 네 갈래와 401 대조군), 뒤의 것은 **서버를
  거치지 않고 FCM v1 API 로 직접** 나간다(서버가 만드는 것과 같은 모양의 푸시를 손으로 쏴서 앱
  수신을 확인하는 자리다). `http-client.env.json`·`_reset.http`·`http/README.md` 도 같은 PR 이
  함께 맞췄다 — `fcm_project_id`·`fcm_access_token`·`fcm_device_token` 셋은 응답에서 뽑는 값이
  아니라 **손으로 채우는 값**이라 `_reset.http` 의 비우기 목록에는 일부러 안 넣었고, 그 사실을
  두 파일이 각자 적는다.

  **`http/` 요청 모음의 커버가 25/29 → 26/29 가 됐다 — 일곱 번째 왕복이 닫혔다.** 다만 이번
  왕복은 **방향이 반대다.** 앞선 여섯 번은 서버가 엔드포인트를 늘리면 요청 모음이 뒤처지는
  모양이었는데, 이번에는 **요청 모음이 앱 코드보다 앞서 나갔다** — 등록 엔드포인트를 부를 수단
  (FCM 토큰 취득)이 develop 에 여전히 0건이라 앱은 못 부르지만, 이 파일은 토큰을 손으로 넣으므로
  **앱 없이 지금 돌릴 수 있다.** 일곱 번째도 사람이 손으로 메운 것이라 OQ-P-092 의 "갱신 경로가
  둘"은 그대로다.

  **코드 드리프트는 0건이다.** `.kt`·gradle·리소스 어느 것도 안 바뀌었으므로 `android_status`·
  Android 매핑의 판정(등록 표면만 있고 호출부 0, FCM 수신·채널 심볼 0건)은 그대로 옳다 — 실제로
  `origin/develop` 전체에서 `firebase-messaging`·`FirebaseMessaging`·`onNewToken`·`parfait_default`
  를 찾으면 **`http/` 아래 세 파일만 걸린다.** 그래서 이 회차가 고친 것은 **코드에 대한 서술이
  아니라 "무엇으로 확인할 수 있는가"에 대한 서술**이다.

  ⚠️ **대신 복제면이 하나 늘었다 — OQ-P-354 신설.** `fcm-test.http` 는 엔드포인트 미러가 아니라
  **서버→앱 단방향 푸시의 미러**다. 문구 2종·`data` 키 4종·채널 id `parfait_default`·TTL 6시간·
  APNs 헤더가 상수로 박혀 있고, 서버가 그 값을 바꿔도 **세는 축이 없다.** 지금까지 `api/` ↔ `http/`
  갈라짐을 드러내 준 것은 엔드포인트 커버 셈(`N/29`)이었는데 푸시에는 셀 엔드포인트가 없다.
  ⚠️ **그리고 둘 다 실행 기록이 0건이다** — `fcm_access_token`(1시간 만료)과 Firebase 서비스 계정
  키가 있어야 돌아가고, 이 회차에 그것을 확보해 쏴 본 적이 없다. `fcm-test.http` 는 받을 쪽이
  없어 지금은 절반만 돌아간다(발송은 200, 기기에는 아무것도 안 뜬다).

  **미머지 브랜치가 다섯에서 일곱이 됐다** — `feature/push-notification-permission` 과
  `feature/#420-canvas-tutorial` 이 새로 올라왔다. 앞의 것이 OQ-P-341 을 정면으로 답한다: 커밋
  제목이 도메인 계약 신설 → `NotificationRepository` 결선 → `onNewToken` 등록 → **권한 허용 직후
  즉시 등록** → **그룹 생성·참여 직후 권한 안내**로 이어져, 미결의 ②(등록 호출 시점)와 ③(권한을
  언제 묻는지)이 브랜치에서는 이미 정해져 있다. 직전 회차가 `feature/push-fcm-service` 에서 본
  "등록을 안 부르는 자리"도 이 브랜치가 잇는다. 머지 전에는 스펙을 고치지 않는 규율([2026-07-13])
  대로 미결에 메모만 달았다.

  **이번 회차가 확인한 것** — **코드가 안 바뀐 라운드에도 계약 표면은 움직인다.** 이 문서의 감사
  루틴은 `.kt` 심볼 대조에 무게가 실려 있어서 `http/` 만 바뀐 delta 는 "드리프트 0건"으로 지나가기
  쉽다. 그런데 `http/` 는 **계약 서술을 복제해 두는 자리**라, 여기가 늘면 `api/` 문서의 커버 셈과
  "확인하는 법" 절이 곧바로 낡는다. 그러므로 delta 가 `http/` 만 건드렸을 때 물을 것은 "코드가
  바뀌었나"가 아니라 **① 커버 셈이 움직였나 ② 새로 복제된 계약 값이 무엇인가 ③ 그 복제를 세는
  축이 있나** 셋이다. 이번엔 ①이 26/29 로 움직였고 ②가 푸시 페이로드였으며 ③이 없어서 미결이 됐다.

  직전 회차 요약(64회차, `c74f40eb`): **앱을 여는 첫 그림이 갈렸는데 코드는 한 줄도 안 움직였다** (delta 1건, **1파일 · 삽입
  0줄 · 삭제 0줄** — 바이너리 교체라 diff 가 세어 줄 것이 없다). 유닛 **1029건**·계측 **17건** 유지
  (테스트 파일 무변경). **선작성 스펙·계획 없음 → 아카이브 이동 0건**, 미결은 **하나가 생기고 둘이
  갱신됐다**(OQ-P-350 신설 / 229·341 갱신, `oq-next` 350 → 351).
  **스플래시 로띠가 새 로고 애니메이션으로 통째로 교체됐다**(#444) — `feature/intro/impl` `res/raw/`
  의 애셋 하나가 전부다. 커밋 메시지가 "구조는 그대로이고 애니메이션 JSON 만 바뀌었다"고 주장하는데
  **그 주장을 열어서 확인했다**: dotLottie 를 풀면 manifest 판본·애니메이션 id(`Lottie-Logo`)·
  프레임률·재생 길이·화면 크기·레이어 여덟(`Parfait` 일곱 글자 + `Stroke`)이 교체 전과 같고, 다른 것은
  각 레이어의 패스와 키프레임 값뿐이다. 그래서 `R.raw.splash`·`SplashScreen` 은 손댈 자리가 없고,
  **60fps·3.5초**라는 위키 [[스플래시-애니메이션]] A-001 정본의 타임라인도 그대로다 — 진입이 재생
  종료를 기다리는 구조(OQ-P-229)의 대기 길이가 **한 프레임도 안 움직였다**는 뜻이다. 그러므로
  spec·plan·architecture·ADR·api 어디에도 고칠 문장이 없었다(**드리프트 0건**). ⚠️ 대신 남는 것이
  그림이다 — 바뀐 결과를 실기기로도 프리뷰로도 **본 기록이 0건**이고, 파일명도 구조값도 그대로라
  옛 판을 도로 넣어도 아무것도 빨갛게 되지 않는다 → **OQ-P-350 신설**.

  **미머지 브랜치가 둘에서 다섯이 됐다** — `feature/#423-canvas-save-preview`(캔버스 저장 미리보기
  화면) · `feature/push-fcm-service`(FCM 수신부) · `feature/push-notification-deeplink`(푸시 딥링크)
  셋이 같은 날 새로 올라왔다. 그중 푸시 브랜치가 OQ-P-341 을 정면으로 건드린다 — `firebase-messaging`
  의존과 `onNewToken` 이 되살아나 있는데 **그 자리가 아직 등록을 부르지 않고**, 안 부르는 근거로 단
  주석("등록 API 스펙이 아직 배포되지 않았다")은 **직전 회차의 #437 이 이미 무너뜨린 전제**다. 머지
  전에는 스펙을 고치지 않는 규율([2026-07-13])대로 미결에 메모만 달았다.

  **이번 회차가 확인한 것** — **바이너리 교체라고 다 같은 것이 아니다.** 이 문서는 폰트 회차에서
  "바이너리만 바뀐 라운드는 이 감사 체계가 가장 약한 자리"라고 적었고 그때 문서가 붙잡을 수 있는
  근거는 함께 들어온 텍스트 파일뿐이었다. 로띠는 다르다 — **컨테이너가 zip 이고 안이 JSON 이라
  열어서 구조를 대조할 수 있다.** 이번 회차가 "드리프트 0건"이라고 말할 수 있는 근거는 커밋 메시지의
  주장이 아니라 그 대조다. 그러므로 delta 에 바이너리가 있으면 먼저 물을 것은 "diff 가 안 보인다"가
  아니라 **"이 형식은 열리는가"**이고, 열리면 구조값을 정본과 맞춰 본다. 열어도 남는 것(그림·글자체
  같은 렌더 결과)이 미결이 될 자리이며, 열 수 있는 형식은 **회귀 검사를 붙일 수 있는 형식**이기도
  하다(OQ-P-350 ②가 폰트 쪽 OQ-P-307 보다 먼저인 이유다).

  직전 회차 요약: **서버가 먼저 연 표면에 앱이 하루 만에 붙었고, 조용히 실패하던 모듈 설치가 기다릴 줄
  알게 됐다** (delta 4건, 52파일 **삽입 940줄·삭제 101줄**). 유닛은 1015 → **1029건**(+14, 설치기 7 ·
  세그멘테이션 ViewModel 3 · 알림 DataSource 4), 계측은 **17건** 그대로다. **선작성 스펙 1·계획 1이
  아카이브로 갔고**, 미결은 **하나가 생기고 셋이 갱신됐다**(OQ-P-347 신설 / 341·342·153 갱신,
  `oq-next` 347 → 348).
  **① ML Kit 모듈 설치가 종료까지 기다린다**(#438) — `installModules` 의 Task 반환은 Play 서비스
  계약상 **요청 접수**일 뿐인데 그것을 설치 완료로 읽고 있었다. 그래서 모듈이 없는 기기의 첫
  사용자는 예외 없이 실패했다. `SegmentationModuleInstaller` 가 `InstallStatusListener` 의 종료
  신호까지 기다리고, `Mutex` + `CompletableDeferred` 로 진행 중인 설치를 여러 호출자가 나눠 쓴다.
  준비는 사용 직전이 아니라 **사진 확인 화면 진입**에 미리 건다(촬영·갤러리 두 경로의 유일한
  합류점이다). 실패는 `SegmentationErrorKind` 로 갈려 모듈 실패에 전용 문구와 **재시도 버튼**이
  붙었다 — OQ-P-153 ③ 의 근거였던 "재시도할 수단이 없다"가 그렇게 닫혔다. 같은 라운드가
  `data/repository/image` 를 셋으로 갈랐다(`installer/image` · `utils/image`, `data/util` 은
  `data/utils` 로 합쳐졌다).
  **② 기기 FCM 토큰 등록에 `:data` 표면이 생겼다**(#437) — `NotificationService` ·
  `NotificationRemoteDataSource`(+`Impl`) · `DeviceToken`. **서버 표면이 먼저 열린 첫 도메인**이었는데
  결국 여기서도 앱이 하루 만에 따라붙었다. ⚠️ **부를 수단은 없다** — FCM 토큰을 얻는 심볼이 develop
  에 여전히 0건이고(2026-08-22 #325 가 걷어냈다) 호출부도 0건이라, 되살릴지를 묻는 OQ-P-341 은
  그대로 열려 있다.
  **③ 누끼 영역 편집에 빨간 틴트가 들어왔다**(#442) — 남는 영역에 `Cherry500` 을 `SrcAtop` 으로 한 겹
  얹어 마스크 경계를 눈으로 잡게 한다. 위키 [[누끼-편집]]에 대응 조항이 없고 근거는 커밋 메시지뿐이라
  **OQ-P-347 을 세웠다**. **④ README 에 소개·스크린샷이 붙었다**(#439) — 코드·계약 무변경이다.

  **이번 회차가 확인한 것** — **"표면이 생겼다"와 "미결이 닫혔다"는 서로 다른 사건이다.** #437 은
  OQ-P-341 이 지목한 자리를 정확히 채웠는데도 그 미결은 한 줄도 닫히지 않았다. 그 미결이 묻는 것이
  표면의 유무가 아니라 **축을 되살릴지**였고, 부를 수단(FCM 토큰 취득)은 여전히 0건이기 때문이다.
  직전 회차가 "미결은 결정이 아니라 코드가 옮겨 와서 닫힌다"를 적었다면, 이번 회차는 그 뒤집힌 면을
  적는다 — **코드가 와도 그 코드가 미결의 질문에 답하지 않으면 닫히지 않는다.** 그래서 delta 를 볼 때
  확인할 것은 심볼이 생겼는가가 아니라 **그 심볼로 물음에 답할 수 있게 됐는가**다.
  반대편에서는 #438 이 OQ-P-153 ③ 을 **부수적으로** 닫았다. 그 미결은 재시도 버튼을 요구했는데,
  이번 라운드는 버튼을 목적으로 넣은 것이 아니라 모듈 실패를 정직하게 알리다 보니 놓게 된 것이다.
  ⚠️ 다만 그 버튼은 **디자인 검토 대기 시안**이고, 모듈 없는 상태의 화면 경로는 실기기에 모듈이
  도착해 **사람이 눈으로 본 적이 없는 채로** 머지됐다.

  직전 회차 요약: **위키가 1년 가까이 적어 둔 목적지로 코드가 옮겨 왔고, 앱 버전이 1.0.0 이 됐다**
  (delta 5건, 25파일 **삽입 449줄·삭제 85줄**). 유닛은 1012 → **1015건**(+3), 계측은 **17건**
  그대로다. 선작성 스펙·계획이 없어 **아카이브 이동은 0건**이고, **미결은 하나가 닫히고 둘이 생겼다**
  (OQ-P-135 해소 / OQ-P-339·340 신설, `oq-next` 339 → 341).
  **① 그룹을 만들거나 참여하면 그 그룹의 캔버스로 바로 들어간다**(#411) — 종착지가 G-001 목록에서
  C-001 캔버스로 옮겨 가며 [[기능정의서-v6]]이 적어 둔 배선과 **처음으로 맞았다**(OQ-P-135). 복귀는
  `goToSingleClearTop` 한 줄에서 **`replaceAll(목록)` + `goTo(캔버스)` 두 줄**이 됐고(캔버스만 남기면
  뒤로가기가 앱 종료가 된다), 그 결과 `goToSingleClearTop` 은 **소비처가 0건**이 됐다. 캔버스는
  `NavKeyCanvasMain` 에 붙은 `welcomeGroupName`·`welcomeInviteCode` 로 **진입 사유**를 받아 환영
  배너를 1회 띄우고, 생성 갈래에는 초대코드 복사가 붙는다(`YGAlert` 의 첫 프로덕션 소비처이고,
  그 참에 칩 버튼 끝 아이콘이 파라미터로 열렸다).
  **② 갤러리 저장이 메뉴에서 날짜바로 올라갔다**(#413·#414) — `ic_save` 아이콘이 날짜 버튼 오른쪽에
  서고 캘린더 그림은 텍스트 앞으로 내려와 날짜와 한 클릭 영역이 됐다. 그래서 **오늘 캔버스에서도
  저장할 수 있고**(전에는 지난 캔버스 전용이었다), 보일지는 날짜가 아니라 **저장할 내용이 있는지**로
  갈린다(OQ-P-340). 빈 자리를 메우려 `YGCanvasMenu.addAction` 이 nullable 이 되고 `YGMenuItem` 이
  아이콘·비활성을 얻었다.
  **③ 토핑 편집 핸들 둘이 자리를 맞바꿨다**(#412) — 우상단이 회전, 우하단이 크기조절이다. 환산이
  핸들 좌표와 중심만 보므로 계산식·상하한은 그대로다. **④ 버전이 5/0.1.1 → 6/1.0.0**(#434)이고,
  정식 판 번호로 올린 근거는 어디에도 없다(OQ-P-310).

  **이번 회차가 확인한 것** — **미결이 닫히는 방식이 "결정"이 아니라 "코드가 옮겨 오는 것"이었다.**
  OQ-P-135 는 열린 지 20일 동안 ①(코드가 맞다)과 ②(정본이 맞다) 중 무엇으로 닫힐지 물었는데,
  답을 준 것은 회의가 아니라 머지였다 — 그리고 그 미결이 선행 조건으로 적어 둔 것(인자 있는
  `NavKeyCanvasMain`)은 이미 2주 전에 다른 사유로 끝나 있었다. **미결이 적어 둔 선행 조건은 그 미결과
  무관하게 먼저 갖춰질 수 있다**는 뜻이고, 그래서 닫을 때 확인할 것은 조건의 이행이 아니라 **지금
  코드가 어느 쪽인가**다.
  또 하나는 **닫힌 자리에서 곧바로 다음 미결이 나왔다는 것**이다 — 목적지는 정본과 맞았지만 그
  화면이 새로 띄우는 배너에는 정책 소스가 없다(OQ-P-339). 저장도 같은 모양이다: 자리를 옮기며
  기능이 넓어졌는데 넓힌 근거가 커밋에 없다(OQ-P-340). **정본과 맞추는 변경이 정본에 없는 것을
  함께 들여온다**는 것이 이 회차가 남긴 형태다.

  직전 회차 요약: **한 줄짜리 색 교정 하나가 들어왔고, 그것을 적어 둔 문서는 아카이브에 있었다**
  (delta 1건, 1파일 **삽입 1줄·삭제 1줄** — 이 스킬이 세어 온 회차 중 가장 작다). 트리가 브랜치
  팁과 같아 **충돌 해소 편집이 0건**이고, 유닛 **1012건**·계측 **17건**이 그대로다(테스트 파일이
  안 바뀌었다). 선작성 스펙·계획이 없어 **아카이브 이동은 0건**이고, **신규 미결도 0건**이다.
  **그룹 추가 오버레이의 판 배경이 `Cherry50` 에서 `Gray.White` 로 바뀌었다**(#430) — 커밋이 밝힌
  근거는 "항목을 얹는 바탕이지 색을 띠는 면이 아니다"이고, 항목·구분선·모서리는 건드리지 않았다.
  화면 구조도 계약도 그대로라 코드 쪽 파장은 이 한 줄에서 끝난다.

  **이번 회차가 확인한 것** — **아카이브가 보관소가 아니라 여전히 대조 대상이라는 것을 가장 작은
  델타가 보여 줬다.** 유일한 드리프트가 `status: implemented` 로 archive 에 들어간 g001 스펙
  한 줄이었다 — 구현이 끝나 옮긴 문서도 그 화면이 다시 손질되면 곧바로 거짓이 된다. 그래서 이번
  점검의 일은 active 문서를 훑는 것이 아니라 **아카이브 본문의 as-built 한 줄을 고치는 것**이었다.
  다만 이것은 규모의 문제이지 성질의 문제는 아니다 — 아카이브가 낡는 속도는 그 화면을 누가
  얼마나 자주 만지느냐에 달렸고, 이번에는 그 화면이 하필 만져졌을 뿐이다.
  미결을 새로 세우지 않은 것도 판단이다 — 배경색이 위키 정책에 없다는 사실은 이미 그 오버레이의
  구조를 묻는 미결(엔트리 `Box` + 스캐폴드 중첩 항목)이 덮고 있어, 색 하나로 항목을 늘리지 않았다.

  직전 회차 요약: **브랜치에만 있던 하루짜리 문서가 develop 사실이 됐고, 잠가 두었던 버튼 하나가 열렸다**
  (delta 2건, 37파일 **삽입 997줄·삭제 437줄**). 두 머지 다 트리가 브랜치 팁과 같아 **충돌 해소
  편집이 0건**이고, 유닛은 996 → **1012건**(+16, 테스트 파일 +1), 계측은 **17건** 그대로다.
  **선작성 스펙 1·계획 1이 아카이브로 갔다** — 직전 회차에 이어 두 회차 연속이다.
  **① 토핑 변형 저장이 일괄 PATCH 한 번으로 접혔다**(#428) — 확인 버튼이 단건 PATCH 를
  `async` + `awaitAll` 로 N번 부르던 자리를 `UpdateToppingsUseCase` → `ToppingRepository.updateAll`
  한 번이 대신한다. 단건 경로는 Service 메서드·DataSource·Repository·UseCase·wire DTO 까지 **통째로
  걷혔고**(서버 엔드포인트는 그대로다 → OQ-P-335 가 develop 사실이 됐다), 부분 수정을 나를
  `ToppingTransformUpdate` 가 domain 에 생겼다. **저장을 가르는 축이 토핑에서 축으로 바뀐 것이 이
  라운드의 실질**이다 — dirty 토핑을 변형·테두리로 갈라 변형은 일괄 1회, 테두리는 여전히 토핑마다
  나간다(계약에 테두리 필드가 없다). 대가는 **부분 성공의 소멸**이다: 하나가 걸리면 전부 롤백되고
  응답이 어느 항목인지 안 알려주므로 변형을 보낸 토핑 전부가 dirty 로 남는다(OQ-P-334 ① 해소·⑤ 잔존).
  겸해 과거 캔버스 목록의 `status` 가 `PastCanvasVO` 까지 올라왔고 **달력 점 기준은 토핑 개수 그대로**
  두었다 — 위키 [[C-201-캘린더-정책-v0.1]] 이 정본이고 서버 `EMPTY` 는 뜻이 좁다(OQ-P-333 ②③ 해소).
  그룹 목록 `recentImageUrl` 의 바뀐 뜻도 KDoc 두 곳에 앉았다(OQ-P-336 ③ 해소, ①② 잔존).
  **② 되살린 알맹이도 테두리는 고칠 수 있게 됐다**(#425) — 최근 목록에서 되살린 알맹이는 원본이
  없어 "사진 편집"이 잠겨 있었는데, 잠그는 대신 `NavKeyToppingEdit(borderOnly = true)` 로 **테두리
  편집만** 연다. 그래서 **플래그의 뜻이 넓어졌다** — "이미 캔버스에 놓인 토핑"에서 **"되살릴 원본이
  없는 진입"**으로 바뀌었고, 원본 자리에는 알맹이를 같이 넣는다(⚠️ 그 전제가 진입마다 다른 뜻이 된다
  → **OQ-P-338 신설**). 함께 초안 재기록 가드가 `SavedStateHandle` 표시로 옮겨 프로세스 사망 복원이
  편집 결과를 덮어쓰던 길을 막았고, 테두리 미리보기가 알맹이를 꽉 채우는 대신 **사방에 여백을 둔 판**
  가운데에 앉아 굵은 테두리가 가장자리에서 깎이지 않는다. ⚠️ 그 여백 상수와 굵기 상한이 서로를 모른
  채 같은 값이고, 여백을 둔 화면은 편집 미리보기 하나뿐이다 → **OQ-P-337 신설**.

  **이번 회차가 확인한 것** — **"브랜치에만 있다"고 적어 두는 규율이 하루 만에 값을 치렀다.**
  직전 회차가 선작성 문서 넷 중 셋을 나흘 만에 닫았고, 이번에는 전날 브랜치에 있다고 적어 둔 것이
  이튿날 아침 develop 이 됐다 — 그 사이 문서가 한 일은 **as-built 대조가 아니라 표기 갱신뿐**이었고
  설계와 갈린 자리는 0건이었다. 반대 사례가 같은 날 나란히 들어온 것이 이 회차의 대비다 — **#425 는
  선작성 스펙도 계획도 없이 들어와** 문서가 사후에 따라 적었고, 신규 미결 둘은 전부 그쪽에서 나왔다.
  선작성이 미결을 줄인다는 증거는 아니다(표본이 하루치다). 다만 **문서가 먼저 있으면 이 스킬의 일이
  표기 갱신으로 끝나고, 없으면 코드를 읽어 사실을 다시 세워야 한다**는 비용 차이는 그대로 보였다.

  직전 회차 요약: **release 에만 있던 스택 둘이 develop 으로 건너왔다** (delta 2건, 51파일
  **삽입 3045줄·삭제 871줄**). 두 머지 다 트리가 브랜치 팁과 같아 **충돌 해소 편집이 0건**이고,
  유닛은 949 → **996건**(+47, 파일 100 → 101), 계측은 **17건** 불변이다. **이 회차의 일은 대부분
  "이미 풀린 것을 문서에 앉히는 일"이었다** — 선작성 스펙 1·계획 3이 한꺼번에 아카이브로 갔고
  (직전 회차까지 0건이던 자리다), ADR-0029 가 `proposed` → `accepted` 가 됐다.
  **① 캔버스 폴링 스택 3단이 한 머지로 들어왔다**(#404) — PR1 배경 탭 토핑 렌더링 · PR2 오늘 캔버스
  인메모리 SSoT · PR3 주기 폴링이다. 오늘 캔버스가 그룹 SSoT(ADR-0023)와 같은 형태로 갈려
  `ParfaitRepository.getTodayCanvas` 하나가 **구독·갱신 둘·정리·실패 축** 다섯이 되고
  `GetTodayParfaitUseCase` 가 사라졌다. 새것은 `:data` 의 `CanvasLocalDataSource`(인메모리 두 번째)와
  `CanvasPoller`(그룹별 참조 계수 + 5초 루프), `:core:ui` 의 `launchWhileSubscribed`(구독 수 기반
  수명), DI 모듈 둘(`ApplicationScopeModule`·`ClockModule`)이다. **부르는 주체가 화면에서 저장소
  층으로 내려간 것이 이 라운드의 실질**이다 — 계약은 한 줄도 안 바뀌었고 `today` 를 억제하던
  `launch(key)` 하나가 폴러의 그룹별 가드로 모였다. **문서가 두 회차째 "계획이 델타를 모른다"고
  세던 것(OQ-P-326 ①②⑤⑥)이 develop 사실이 됐다.**
  **② 최근 목록 정원이 종류별로 갈렸다**(#408) — `MAX_SIZE` → `MAX_SIZE_PER_KIND` 로
  원본(`SOURCE`)과 알맹이(`CUTOUT`)가 각자 상한을 든다. **OQ-P-258 이 ②로 닫혔고, PR6 가 ②를
  기각한 근거는 실제로 발생하지 않았다** — 저장 목록을 가르지 않고 자르는 판정만 종류별로 두면
  시간순 정렬도 데이 윈도우 정리도 이중이 되지 않는다. 겸해 최근 줄에 무엇을 싣는지가
  `returnResultOnly` 에서 신설 `RecentImagePick` 으로 갈렸고, 그것이 `NavKeyCustomGalleryPicker` 의
  **기본값 없는 첫 동작 인자**다. ⚠️ 총 상한이 두 배가 됐는데 저장소 사용량을 잰 사람이 없다 →
  **OQ-P-332 신설**.

  **이번 회차가 확인한 것** — **기준선을 develop 에 두는 규율이 이번에도 스스로 회복했다.**
  직전 회차가 "선작성 문서 넷이 구현이 끝난 채 `draft` 에 남는다"며 아카이브 판정 기준을 다시
  물었는데(OQ-P-311 ③), 넷 중 셋이 나흘 만에 develop 으로 들어와 **기준을 바꿀 필요 없이** 닫혔다.
  세그멘테이션 넷 때(2026-08-27)와 같은 결말이고, 이로써 같은 형태가 두 번 반복됐다 — release 가
  먼저 받고 develop 이 뒤따르되 **뒤따르는 것은 같은 커밋이 아니라 리베이스된 다른 커밋**이다
  (release 만 50커밋은 이번에도 줄지 않았다). 남은 것은 `feature/debug-mode` 하나다.
  다만 이 회복이 규율의 정당성을 증명하지는 않는다 — **셋이 들어온 것은 판정 기준 덕이 아니라
  누군가 머지했기 때문**이고, 다음에 안 들어오면 문서는 다시 어긋난 채로 기다린다.

  직전 회차 요약: **배포 계보가 develop 을 앞질렀다** (delta 4건, 14파일 **삽입 383줄·삭제 59줄**).
  네 머지 모두 트리가 브랜치 팁과 같아 **충돌 해소 편집이 0건**이고, 유닛은 942 → **949건**
  (+7, 파일 100 그대로), 계측은 14 → **17건**(+3, 파일 5 → 6 — `YGToastHostTest` 신설)이다.
  **선작성 스펙·계획이 없어 아카이브 이동은 0건**이다.
  **① 토스트 스택에 교체 갈래가 생겼다**(#405) — Dim 으로 Spotlight 를 껐다가 자동 소멸 전에 다른
  토핑을 탭하면 작성자 안내 둘이 **같은 자리에 포개져** 둘 다 못 읽었다. `YGToastPolicy.show` 가
  `replaceTag` 를 받아 같은 태그를 걷어낸다. 가르는 축을 `YGToastType` 으로 두지 않은 이유는 **같은
  타입이면서 쌓여야 하는 토스트**가 있어서다. 태그를 주는 곳은 캔버스 하나이고 나머지는 종전대로
  쌓인다. ⚠️ 태그 문자열이 화면 소유라 같은 값을 고른 발행자끼리 서로 지운다 → **OQ-P-329 신설**.
  **② 문서가 "미머지"로 세던 상단바가 들어왔다**(#406) — Figma `Floating Bar` 의 `Status=Title` 이
  `YGFloatingBarTitle` 로 신설돼 변형이 **5종**이 되고, C-102 갤러리 두 화면이 손으로 조립하던
  `Row` + `YGCircleButton` 을 그것으로 바꿨다. 수제 버튼의 `contentDescription` 이 `null` 이었으므로
  **닫기 버튼에 접근성 레이블이 따라왔다.** ⚠️ 빈 상태에만 제목을 두지 않는 근거가 작업자 지시
  뿐이고 육안 대조도 없다 → **OQ-P-331 신설**.
  **③ 로딩 오버레이가 "누른 작업"에서 "기다리는 작업"으로 넓어졌다**(#407) — G-001 그룹 목록과
  C-001 오늘 캔버스가 **화면에 들어오자마자 나가는 첫 조회**에 `YGScaffoldV2(isLoading)` 을 켠다.
  둘 다 이미 V2 를 쥐고 `isLoading` 만 안 넘기던 자리라 **스캐폴드 시그니처는 한 줄도 안 바뀌었다.**
  조건이 "조회 중"이 아니라 **"아직 한 번도 못 받은 조회"**인 것이 이 라운드가 세운 규칙이다 —
  두 화면 다 재진입마다 조회가 나가므로 조건이 없으면 이미 그려진 화면 위로 덮개가 번쩍인다.
  켜고 내리는 것을 둘 다 `launch` 블록 **안**에서 하는 것도 같은 이유다(키 가드에 막히면 블록이
  아예 안 돌아 `finally` 가 따라오지 않는다). ⚠️ 그 판정이 두 ViewModel 에 복제돼 있고 위키
  [[무한-파르페-그리드]]가 요구한 "자체 로딩 그래픽"과도 갈린다 → **OQ-P-330 신설**(OQ-P-205 ②).
  **④ 앱 버전이 한 PR 에서 두 칸 올랐다**(#409) — `3 → 4 → 5`(`0.0.3 → 0.1.0 → 0.1.1`). develop 이
  이 축에서 처음으로 배포본과 같은 값을 든다(OQ-P-310).

  **이번 회차가 확인한 것** — **감사 기준선이 배포된 코드를 더는 가리키지 않는다.** OQ-P-311 이
  "release 만 43커밋"으로 세던 갈림은 이번에 **방향이 뒤집혔다**: release 계보가 둘 더 생겼고
  (`0.1.0-4`·`0.1.1-5`), 최신 것을 기준으로 세면 **release 만 50커밋 · develop 만 8커밋**이며 그
  여덟은 이번 delta 그 자체다. release 는 이번 세 feature 브랜치를 develop 과 **별개로 직접 받았고**,
  그 위에 develop 에 없는 셋(`debug-mode`·`cache-image`·`canvas-polling`)을 더 받았다. 그래서
  **선작성 문서 넷이 구현이 끝난 채로 `draft` 에 남는다** — 로그인 디버그 모드 스펙·계획과 캔버스
  PR2·PR3 계획이다. 아카이브 판정을 `develop` 머지로 두는 규율이 이번에 처음으로 **문서 상태를 실제와
  어긋나게** 만들었다(OQ-P-311 ③ 재개).
  같은 자리에서 하나 더 드러난다 — **선작성 계획은 코드가 바뀔 때마다 낡는데, 그 낡음을 먼저 푸는
  것이 문서가 아니라 브랜치일 수 있다.** PR2 계획의 `loadTodayCanvas()` 교체 블록은 이번에 붙은
  첫 조회 덮개를 모르지만, release 계보는 두 의도를 이미 합쳐 두었고 **그 답이 계획과 다르다**
  (덮개를 구독 안에서 파생시킨다). 그러니 계획을 고칠 때 새로 설계할 것이 아니라 **이미 풀린 자리를
  베껴 온다** → OQ-P-326 ⑥.


  직전 회차 요약: **버그를 고치는 김에 미결이 닫혔다** (delta 5건, 28파일 **삽입 706줄·삭제 418줄**).
  다섯 머지 모두 트리가 브랜치 팁과 같아 **충돌 해소 편집이 0건**이고, 유닛은 931 → **942건**
  (+11, 파일 98 그대로), 계측은 **14건** 불변이다. **선작성 스펙·계획이 없어 아카이브 이동은
  0건**이다 — 다섯 PR 이 전부 버그픽스이고, 이 회차가 한 일은 그 수정이 문서를 어디까지
  뒤집었는지 세는 것이다.
  **① 그룹 진입 두 화면이 하나의 관용구로 수렴했다**(#393·#394) — A-005 생성과 S-102 참여가 둘 다
  **요청 직전에 확인 팝업을 닫고**, 진행은 `YGScaffoldV2` 로딩 오버레이가, 실패는 토스트가 말한다.
  Danger Zone 3종이 PR #287 에서 고른 것과 같은 형태다. 그 결과 **`YGModalPopup.isEnabledButton` 을
  주는 호출자가 0곳**이 되고(OQ-P-137 ④ 해소), 두 화면이 Route 로 `YGScaffoldV2` 를 쥐면서
  **V1 `YGScaffold` 잔여가 1파일 1호출**(A-004 초대 코드)로 줄었다(OQ-P-204). 실패를 로그로만 남기던
  마지막 화면이던 A-005 도 `GroupCreateError` 2종 + 토스트를 얻어 **OQ-P-167 ④ 가 그룹 진입
  흐름에서 비었다.**
  **② 읽기 전용이던 닉네임 필드가 열렸다**(#393) — `enabled = false` + no-op `onValueChange` 로
  넘겨받은 값을 보여 주기만 하던 자리가 입력을 받고 그룹명과 같은 검사를 거친다. 그래서
  NavKey 인자가 **표시값에서 초기값으로** 바뀌었고, "닉네임이 없으면 그룹 만들기를 열지 않는다"는
  가드(OQ-P-253)를 세우던 근거가 흔들렸다 — 빈 채로 열어도 사용자가 채울 수 있다. 가드 자체는
  그대로다.
  **③ 미뤄 둔 `TODO` 가 이번에도 하나 닫혔는데, 닫는 방식이 새 미결을 만들었다**(#394) —
  참여는 됐고 닉네임만 못 붙은 경우에 `NICKNAME_NOT_APPLIED` 안내가 붙었다. 토스트 호스트가
  화면에 매여 있어 곧바로 넘기면 안내가 뜨자마자 사라지므로 **이동을 그만큼 미룬다**. 실패 안내가
  화면 전환을 지연시키는 첫 사례이고, 기다리는 시간이 `YGToastPolicy` 와 **별개 상수**라
  한쪽을 바꾸면 조용히 어긋난다 → **OQ-P-328 신설**.
  **④ 목록 토핑이 잘리지 않게 됐다**(#396) — `YGToppingGroup` 의 `Remote` 갈래가 `ContentScale.Crop`
  에서 `Fit` 으로 바뀌었다. OQ-P-316 이 ②로 적어 둔 항목인데 **선행이라던 ①(서버가 테두리 필드를
  주는 것)보다 먼저 일어났다** — 테두리와 무관하게 그 자체로 버그였기 때문이다. `clip` 은 남되
  역할이 "비정사각을 프레임에 가둔다"에서 "인접 셀을 덮는 것을 막는 방어선"으로 바뀌었다.
  **⑤ 시스템바가 라이트로 고정됐다**(#395) — [ADR-0028](adr/0028-system-bar-light-fixed.md) 의 결정이
  그대로 들어왔다. 드리프트가 아니라 **문서가 먼저 있고 코드가 따라온** 이번 회차의 유일한 사례다.
  **⑥ 토핑 회전·크기조절이 상수에서 기하로 옮겼다**(#397) — 회전 핸들이 우측 하단인데 각도가 드래그의
  가로 성분만 받아 **시계방향으로 끌면 반시계로 돌던** 결함, 배율에 고정량을 더해 **큰 원본 사진에서
  같은 손동작이 훨씬 크게 먹던** 결함을 함께 고쳤다. 환산이 ViewModel 에서 **핸들 위치를 아는 화면**
  으로 올라가 인텐트가 픽셀 대신 배율·각도를 싣고(`OnToppingResize`·`OnToppingRotate`),
  `TOPPING_DRAG_PX_PER_SCALE`·`TOPPING_DRAG_DEGREES_PER_PX` 가 사라졌다 — OQ-P-241 이 "앱이 정했다"고
  세던 넷이 **둘(하한·상한)로 줄었다.**

  **이번 회차가 확인한 것** — **문서가 "결선 라운드가 오면 닫힌다"고 적어 둔 미결이, 실제로는
  버그를 고치는 김에 닫힌다.** 이번 다섯 PR 은 어느 스펙도 구현하지 않았는데 미결 넷을 건드렸고,
  그중 둘(OQ-P-167 ④·OQ-P-316 ②)은 문서가 **선행 조건을 잘못 걸어 둔** 것이었다 — A-005 의 실패
  표현은 "문구 정책이 확정되면"을 기다릴 필요가 없었고, 목록 토핑의 `Fit` 은 서버 테두리 필드를
  기다릴 필요가 없었다. 그래서 미결을 적을 때 **선행 조건이 진짜 선행인지**를 한 번 더 물어야 한다.
  반대 방향도 같은 회차에 드러났다 — ③처럼 미결을 닫는 방식 자체가 새 상수를 하나 낳으면,
  그 자리를 같은 라운드에서 미결로 옮겨 실어야 한다.


  직전 회차 요약: **미뤄 두었던 `TODO` 셋이 하루에 함께 닫혔다** (delta 3건, 18파일 **삽입 441줄·삭제
  31줄**). 세 머지 모두 트리가 브랜치 팁과 같아 **충돌 해소 편집이 0건**이고, 유닛은 926 →
  **931건**(+5, 파일 수 불변), 계측은 **14건** 그대로다. **선작성 스펙·계획이 없어 아카이브 이동은
  0건**이다 — 세 PR 다 문서보다 코드가 먼저였고, 이 회차가 한 일은 사후 대조다.
  **① 본인 토핑 탭이 갈 곳을 얻었다**(#400) — `handleOnClickMyTopping` 이
  `NavKeyCanvasBGEdit(initialToppingId)` 로 탭한 토핑 id 를 실어 보내고, 편집 화면의 `withCanvas` 가
  첫 조회 결과에 `selectedTab = TOPPING`·`selectedToppingId` 를 얹는다. 그래서 **OQ-P-250 이 셋 다
  닫혔다**. ⚠️ **다만 예고했던 방식이 아니다** — 그 미결은 "C-305 화면 라운드가 오면 닫힌다"고
  적어 두었는데, 실제로는 **새 화면이 생기지 않고 기존 C-301 편집 화면의 토핑 탭이 그 역할을
  받았다**. 위키 [[화면-ID-체계]]에서 C-305 가 독립 화면인 것과 구현의 화면 경계가 갈렸고, 그
  갈림이 `isViewingToday` 가드로 드러난다 — **지난 캔버스에서는 본인 토핑 탭이 여전히 무반응**이다
  (정책에 없는 조건) → **OQ-P-326 신설 ③**.
  **② 계약의 마지막 미소비 갈래가 닫혔다**(#369) — 확인 버튼이 테두리 PATCH 까지 부르면서
  `updateToppingIfChanged` 의 변경 판정이 **둘로 갈렸다**(위치 `update` / 테두리 `updateBorder`).
  서버 API 가 두 엔드포인트로 갈라져 있는 것을 그대로 미러링한 결과이고,
  [api/parfait-image.md](api/parfait-image.md) 의 `android_status` 가 **`done`**(4/4 소비)이 되며
  소비처를 얻은 엔드포인트는 **27건**이 됐다(OQ-P-276 ①③ 해소, ② 잔존).
  ⚠️ **접는 규칙이 그리는 규칙과 어긋난다** — 저장은 `lastOrNull()`(가장 바깥 겹), 렌더링은
  `firstOrNull()` 이다. 겹은 `UndoRedoStack` 이라 실제로 둘 이상 쌓인다 → **OQ-P-324 신설**.
  **③ 서버 토핑을 다시 편집하지 못하게 막던 `TODO(#274)` 도 같은 PR 이 닫았다** —
  `ImageSegmentationRepositoryImpl.decodeImage` 가 **스킴을 갈라** `https://` 면
  `RemoteImageDownloadDataSource` 로 바이트를 받아 디코드한다. Retrofit 밖 raw OkHttp 를 쓰는
  **둘째 자리**이고, 전용 `@DownloadClient` 의 이유는 업로드 쪽과 달리 기능 전제가 아니라
  **커넥션 풀·`Dispatcher` 격리**다(타임아웃 프로필은 메인과 같다).
  ⚠️ 이웃과 대칭이 깨진 자리가 하나 있다 — 업로드는 스트리밍인데 이쪽은 응답 본문을 `bytes()` 로
  통째 힙에 올리고 상한이 없다 → **OQ-P-327 신설**.
  **④ 배율 하한이 열 배 낮아졌다**(#398) — `TOPPING_MIN_SCALE` 이 0.5 → **0.05**. 커밋 메시지와
  KDoc 이 "배율 하한 수정"이라고만 적어 **무엇이 문제였는지가 남아 있지 않고**, 같은 저장소의 배치
  화면이 짧은 변 48dp 에서 하한을 역산하는 것과 갈렸다. 줄인 값은 그대로 PATCH 로 나가므로 **다시
  잡을 수 없는 토핑이 서버에 남을 수 있다** → **OQ-P-325 신설**.
  **⑤ 선작성 문서 셋이 하루 만에 낡았다** — [캔버스 오늘 SSoT·폴링 스펙](superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  과 [PR3 계획](superpowers/plans/archive/2026-08-27-canvas-polling.md)이 전제한 코드를 이번 delta 가 바꿨다. 계획의
  `updateDirtyToppings` 는 위치 PATCH 하나만 부르므로 **적힌 그대로 구현하면 방금 붙은 테두리
  저장이 되돌아간다**. 시딩 목록에도 `selectedTab`·`selectedToppingId` 가 빠져 있다.
  [세그멘테이션 입력 전처리 스펙](superpowers/specs/2026-08-23-segmentation-preprocessing.md)도 같은 부류다 —
  방향 보정과 하한 확대를 `decodeUriToBitmap` 안에 두면 된다고 적는데, 원격 갈래가 그 함수를
  타지 않게 됐으므로 **서버 토핑 재편집만 정규화 밖에 남는다.** 세 문서에 ⚠️ 를 박아 두었다
  → **OQ-P-326 신설 ①②④**.

  **이번 회차가 확인한 것** — **문서가 미결을 닫는 방식을 예고해 두면, 다른 방식으로 닫혔을 때
  그 예고가 미결을 하나 더 만든다.** OQ-P-250 은 "C-305 화면 라운드가 오면 닫힌다"고 적었고
  실제로는 기존 화면이 그 역할을 받았다. 그때 점검이 할 일은 미결을 그냥 해소로 넘기는 것이
  아니라 **예고와 실제가 갈린 지점을 새 미결로 옮겨 싣는 것**이다 — 여기서는 위키 화면 ID 체계와
  구현의 화면 경계, 그리고 정책에 없는 `isViewingToday` 조건이다. 같은 이유로 **선작성 문서가
  있는 영역에서는 delta 를 문서 방향으로도 읽는다** — 이번 세 PR 은 어느 스펙도 구현하지 않았지만
  아직 구현되지 않은 계획 하나를 **회귀로 만들어 버렸다**.


  직전 회차 요약: **세 라운드째 "미머지"로 세어 온 브랜치들이 마침내 develop 으로 들어왔다 — 다만
  release 가 받은 그 커밋들이 아니다** (delta 1건, 커밋 44개·20파일 **삽입 3,685줄·삭제 223줄**).
  `--merges` 는 **한 줄**만 세지만 그 안에 선작성 스펙 셋이 들어 있다:
  [마스크 후처리](superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md) ·
  [알파 정련](superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md) ·
  [커널 취소 확인 전환](superpowers/specs/archive/2026-08-27-alpha-kernel-suspend-cancellation.md).
  머지 트리가 정련 브랜치 팁과 같아 **충돌 해소 편집이 0건**이고, 유닛은 819 → **926건**(+107,
  파일 94 → 100), 계측은 **14건** 그대로다.
  **① 누끼가 처음으로 모델 출력을 고치기 시작했다** — ML Kit 이 준 알파를 그대로 쓰던 경로에
  순수 커널이 붙어 **이진화 축소 → area opening → 팽창 → keep 적용 → 1차 측정 → 가이드 필터 정련 →
  침식 → 2차 측정**을 돈다. 골격은 "판정은 축소판, 경계 모양은 원본 알파"이고, 커널은 `Bitmap` 도
  ML Kit 타입도 모른다 — 정련이 쓸 원본 휘도는 사각형을 받아 픽셀을 돌려주는 `GuidanceProvider`
  로만 들어온다. 그 대가로 [ADR-0012](adr/0012-mlkit-subject-segmentation.md) 가 열어 둔 질문
  하나가 닫혔다: **온디바이스 모델 하나로는 충분하지 않다.**
  **② 취소가 시그니처에서 읽히게 됐다** — 커널이 받던 `checkCancelled` 콜백이 전부 걷히고
  `suspend` + `currentCoroutineContext().job.ensureActive()` 로 옮겼다. `get(Job)?` 계열을 안 쓰는
  것이 계약인데, 이유가 성능이 아니라 **`Job` 이 없으면 조용히 no-op 이 되어 확인이 통째로 사라져도
  테스트가 초록으로 남기 때문**이다. ⚠️ **`suspend` 가 CPU 루프의 취소를 만들어 주지는 않는다** —
  확인을 넣는 부담은 콜백 때와 같고, 실제로 **확인 없이 두 패스를 도는 자리가 그대로 남아 있다**
  (`applyAreaOpening` 의 `countRuns`·`fillRuns`) → **OQ-P-318 신설**.
  **③ 스택 셋이 rebase 로 하나가 됐다** — 그래서 이 회차의 delta 는 머지 하나인데 스펙은 셋이다.
  각 스펙의 본문을 그대로 읽으면 develop 의 형태가 나오지 않는다(뒤 라운드가 앞 라운드의 시그니처를
  덮었다). 후처리 스펙에 그 겹침을 as-built 로 적었다 — `toCandidates` 가 `toCandidatePairs` +
  `buildCandidatePair` 로 갈린 것, `maskSubjectPixels` 가 `maskSubjectAlpha` 가 된 것,
  `postProcessAlpha` 가 `suspend` 가 되며 `checkCancelled` 를 잃고 안내자 공급자를 얻은 것,
  리뷰가 알파 합성을 `AlphaComposite.kt` 로 뺀 것 넷이다.
  **④ 릴리즈 계보 문제는 닫히지 않았고 한 겹 나빠졌다** — 세그멘테이션 넷이 develop 에 들어왔으니
  OQ-P-311 ③(아카이브 판정 기준을 바꿀 것인가)은 닫혔다. 그런데 **develop 이 받은 것은 release 가
  받은 그 커밋들이 아니다** — rebase 된 다른 커밋들이라 release-only 커밋 수는 **43 그대로**이고,
  rebase 중에 커널이 `suspend` 로 바뀌었으므로 **두 계보는 이제 SHA 만이 아니라 내용이 다르다.**
  배포된 `0.0.3` 은 콜백 방식 커널을 담고 있다. 반대 방향은 **85커밋**으로 벌어졌다.
  **⑤ 코드가 다 들어왔다는 것과 값이 옳다는 것은 다르다** — 임계·반경·정칙화·축소 하한은 전부
  **측정 없이 정한 값**이고, 판정 주체는 실기기 사진 세트다(OQ-P-287~300, 열넷 그대로 열려 있다).
  후처리 계획의 Task 14 도 Step 4~5(문서 이동)만 이번에 수행했고 Step 1~3(판정)은 사람 몫으로 남았다.

  **그 회차가 확인한 것** — **`--merges` 한 줄이 스펙 하나라는 보장이 없다.** 직전 회차는 머지
  셋이 스펙 하나였고, 이번에는 그 반대로 머지 하나가 스펙 셋이다. 세는 단위를 머지에 두면 어느
  쪽이든 틀린다. 갈래를 가르는 것은 **브랜치 이름이 아니라 커밋 메시지의 계열**이었다 — 44개를
  훑으면 후보 커버리지·커널·결선·정련·취소 전환이 순서대로 보이고, 그것이 곧 선작성 문서 셋의
  경계다. 그러므로 delta 가 한 줄이어도 **커밋 목록을 먼저 펼치고**, 그 계열이 문서 몇 건에
  걸리는지부터 센다.

  직전 회차 요약: **한 스펙이 다섯 갈래로 나뉘어 들어왔고, 그 사이 스택의 머지 순서가 계획과 뒤집혔다**
  (delta 3건, 전부 [토핑 알파 판정](superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md) 한 스펙).
  전체로 **17파일 삽입 1,208줄·삭제 129줄**이고, 세 머지 **전부 트리가 브랜치 팁과 같아** 충돌 해소
  편집이 0건이다. 신규 유닛 테스트로 **789 → 819**(+30), 계측은 14 그대로다.
  **① 스택이 계획과 다른 순서로 접혔다** — 계획은 PR 1~5를 앞 브랜치 위에 쌓고 앞이 머지되면 뒤의
  베이스를 `develop`으로 바꾸라고 적었다. 실제로는 **#390(PR 5)이 `develop`이 아니라 #389(PR 4)의
  브랜치로 머지**됐고, 그것을 품은 #389가 `develop`으로 들어왔다. 그래서 `--merges`는 셋을 세지만
  **develop 첫 부모 선에 붙은 것은 #388과 #389 둘**이다(#390은 #389 안에 있다). PR 1·2는 애초에
  #388 브랜치가 품고 있었다. 결과 트리는 계획이 의도한 것과 같고, 달라진 것은 **CI가 어디서
  돌았는가**뿐이다 — 계획이 "스택 PR에는 검사가 안 돈다"고 적어 둔 그 조건이 #390에 그대로 걸렸다.
  **② 판정이 토핑에서 레이어로 올라갔다** — 토핑마다 걸려 있던 `clickableYGNoRipple`과 딤의 클릭이
  전부 걷히고, 캔버스 영역 전면을 덮는 `pointerInput` 하나가 겹침 순서대로 알파 마스크를 찍어
  대상을 고른다. 그 대가로 **레이어가 캔버스의 포인터를 독점한다**가 새 계약이 됐다 — 앞으로 캔버스
  아래쪽에 제스처를 붙이면 조용히 죽는다. 클릭이 사라지면서 접근성이 함께 사라지는 자리가 둘 있어
  (토핑·스포트라이트 딤) 시맨틱스로 다시 만들었고, 판정을 끄는 배치 화면에는 붙이지 않는다.
  **③ 문서가 코드보다 먼저 as-built를 담고 있었다** — 계획의 "실행 뒤 달라진 것" 표가 리뷰 반영
  열넷을 이미 적어 둔 채 머지를 맞았다. 그래서 이 회차가 고친 드리프트는 **그 표가 못 덮은 자리
  셋**뿐이다: 스펙이 `OUTLINE_STAMP_COUNT`라 부른 상수가 공개되며 `TOPPING_OUTLINE_STAMP_COUNT`로
  개명된 것, "콜백이 없으면 레이어가 스스로 판단한다"가 `hitTestEnabled` 파라미터가 된 것,
  배경 편집이 마스크를 **내 토핑만** 요청하는 것.
  **④ 옆 문서 둘이 닫혔다** — 배경 편집이 `YGToppingCutoutImage`로 갈아타며 **OQ-P-254**(테두리를
  받아 두고 안 그린다)가 해소됐고, **OQ-P-203 ②**는 대상 자체가 사라졌다(토핑·딤에 클릭 모디파이어가
  남지 않았다). 판정과 무관해 보이는 렌더링 수정이 이 라운드에 들어온 이유는 **판정 모양을 외형과
  맞추려면 두 화면이 같은 그림을 그려야 하기 때문**이다. 저장 쪽(OQ-P-276)은 그대로다.
  **⑤ 새 미결 다섯** — 해상도·임계값이 측정 없이 굳었고(OQ-P-313), 토핑·딤을 읽어 줄 문구가
  임시이며(OQ-P-314), 새로 보이는 테두리와 코너 스트로크의 관계를 아무도 안 봤고(OQ-P-315),
  그룹 목록만 판정·테두리 밖에 남았으며(OQ-P-316), 마스크 캐시가 프로세스 전역인데 비우는 호출부가
  0건이다(OQ-P-317). 앞 넷은 스펙이 미결로 적어 둔 것이고, 마지막 하나가 실행 중에 새로 드러났다.

  직전 회차 요약: **여섯 날 전의 결정이 되돌려졌고, 그 결정을 적어 둔 문서 한 줄이 그동안 거짓이었다**
  (delta 1건).
  #371이 **4파일 삽입 37줄·삭제 3줄**로 들어왔고, **머지 트리가 브랜치 팁과 같아** 충돌 해소 편집이
  0건이다(브랜치가 develop을 한 번 따라잡은 병합을 자기 안에 품고 있다). 선작성 스펙·플랜이 없어
  **아카이브 이동 0건**이고, **신규 테스트 0건**이라 유닛 789·계측 14건이 그대로다.
  **① 되돌린 것** — 카메라·갤러리 두 화면이 `YGToastHost`를 **다시 자기 레이아웃 안**으로 가져갔다.
  Route가 `rememberYGToastPolicy()`를 만들어 **Screen에 넘기고**, Screen이 자기 프레임 `Box`(카메라는
  뷰파인더 자리, 갤러리는 사진 그리드 자리) 안에 `TopCenter`로 심는다. 2026-08-20 #309가 걷어
  스캐폴드로 올렸던 바로 그 형태다. 이유는 **위치**다 — 스캐폴드 호스트는 상태바 인셋 바로 아래에
  떠서 두 화면이 그 자리에 그리는 헤더 행(날짜·닫기 버튼)을 **덮었다.**
  **② 그래서 문서 한 줄이 여섯 날 동안 거짓이었다** — [c102 스펙](superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)이
  #309를 적으며 "갤러리 토스트는 이미 컨텐츠 영역 상단 정렬이라 **보이는 위치는 사실상 그대로다**"라고
  단정했는데, 두 "상단"이 같은 상자가 아니었다(상태바 인셋 아래 vs 닫기 행 아래의 그리드 프레임
  윗변). [c101 스펙](superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)의 "위키 Toast 공통
  정책에는 이쪽이 맞는다"도 이 화면에는 안 맞았다 — 위키 [[toast]]는 노출 **방향**만 정하고
  **기준 프레임**을 정하지 않는다. **이번 회차가 고친 것이 그 두 문장이다.**
  **③ 처방이 브랜치 안에서 한 번 갈렸다** — 첫 커밋은 `YGScaffoldV2`에 `toastTopPadding`을 더하고
  Route가 `padding6 + gap5 + 닫기 버튼 지름 + 뷰파인더 앞 간격`을 **직접 더해** 넘겼다(화면
  레이아웃이 이미 쓰는 상수를 Route에 복제하는 형태다). 두 번째 커밋이 그것을 **통째로 되돌리고**
  호스트를 프레임 안에 심는 쪽으로 갔다 — 코드 주석이 근거를 남긴다: **위치 계산 없이** 프레임
  윗변에 뜬다. 그래서 `YGScaffoldV2`는 이 라운드에서 **한 줄도 안 바뀌었다.**
  **④ 남긴 것 셋** — 관용구가 셋이 됐고(스캐폴드 기본 호스트 / `YGCanvas.overlayContent` 슬롯 /
  Screen이 직접 심기) 무엇을 언제 쓰는지 규칙이 없다. Route가 정책을 안 넘기므로 스캐폴드가
  **발행할 수 없는 호스트**를 화면마다 하나씩 만든다(끄는 수단이 없다). 그리고 호스트가
  **권한 허용 갈래 안에만** 있어 권한 거부 화면에는 호스트가 없다 — 지금은 두 화면 다 발행 조건이
  권한을 요구해 증상이 없지만 **그 결합이 코드에 안 적혀 있다** → OQ-P-312.
  **⑤ 릴리즈 계보는 한 칸 좁혀졌다** — 이 브랜치는 직전 회차에 "문서에 이름조차 없다"고 적은
  다섯째이고, release 브랜치에 이미 머지돼 있었다. 이번에 develop에도 들어와 **양쪽에 다 있고**,
  갈라진 폭은 45커밋에서 **43커밋**으로 줄었다. **세그멘테이션 넷은 그대로 release에만 있다**
  → OQ-P-311.

  직전 회차 요약: **문서가 세 라운드째 "미머지"로 세어 온 넷이 develop이 아닌 곳에서 이미 배포됐다**
  (delta 3건).
  #372·#374·#376이 **13파일 삽입 144줄·삭제 54줄**로 들어왔고, **세 머지 전부 트리가 브랜치 팁과
  같아** 충돌 해소 편집이 0건이다. 선작성 스펙·플랜이 없어 **아카이브 이동은 0건**이고, 유닛은
  785 → **789건**(전부 #376), 계측은 **14건** 그대로다. 겉보기에는 작은 라운드인데, 그 옆에서
  드러난 사실이 이 회차의 알맹이다.
  **① 이 감사가 보는 트리가 배포된 트리가 아니다** — #374가 앱 버전을 **0.0.1/1 → 0.0.3/3**으로
  올렸는데 **2는 어느 브랜치에도 없다.** 그 실마리를 따라가면 경량 태그 `0.0.3`이 나오고, 그것이
  가리키는 `origin/release/version-0.0.3-3`은 **develop에 없는 커밋 45개**를 담고 있다. 받아들인
  머지 여덟 중 **넷이 이 문서가 세 회차 연속 "미머지 추적 항목"으로 세어 온 세그멘테이션
  브랜치들**이고, 다섯째(`feature/toast-position-fix`)는 이 문서에 이름조차 없다. 즉 그 넷은
  방치된 것이 아니라 **다른 계보로 나갔고**, "문서가 검증한 코드"와 "사용자에게 나간 코드"가
  갈렸다 → OQ-P-311. **이번 회차는 그 브랜치를 감사하지 않았다** — 기준선 규율이 develop만 보게
  돼 있어서다. 감사 대상을 어떻게 둘지가 다음 회차의 첫 결정이다.
  **② 릴리즈 산출물이 처음으로 실제로 조립됐다** — #372의 브랜치 이름이 `#283-check-release-build`이고,
  커밋 하나가 **릴리즈에서만 터지는 lint 실패**를 고친다(`Instantiatable` — 매니페스트가 직접 선언한
  카카오 `AuthCodeHandlerActivity`의 상위 타입 `AppCompatActivity`를 컴파일 클래스패스에 `appcompat`이
  없어 못 풀었다). 그 실패를 만나려면 릴리즈를 돌려야 하므로 **"아무도 release를 만들어 본 적
  없다"가 깨졌다**(OQ-P-074 ② 마커). ⚠️ 그런데 `appcompat` 선언을 **AppCompat 도입으로 읽으면 안
  된다** — 런타임 의존과 그 액티비티의 테마(`TransparentCompat`, `Theme.AppCompat.NoActionBar` 파생)는
  카카오 SDK AAR이 매니페스트 병합으로 이미 넣고 있었고, 이번에 더한 것은 **버전을 우리가 못 박는
  컴파일 의존**뿐이다.
  같은 PR이 **리소스 축소를 처음 켰다**(`isShrinkResources = true`) — 그전까지는 `isMinifyEnabled`만
  켜져 있어 **리소스는 축소된 적이 없었다**(OQ-P-123 ③의 괄호가 이제야 참이 됐다). ⚠️ 축소의 실패는
  조립이 아니라 **실행에서** 드러나는데 그 산출물을 설치해 본 사람이 없고 `keep.xml`도 없다
  (지금은 `Resources.getIdentifier` 사용처가 0건이라 안전하다) → OQ-P-308. debug 블록에 함께 들어간
  `proguardFiles`는 minify가 꺼져 있어 **아무 일도 안 한다**. `firebase-crashlytics-ndk`도 붙었는데
  겨냥 대상이 우리 코드가 아니라 **CameraX·DataStore의 `.so`**이고, 심볼 업로드를 껐으므로 스택에
  **주소만** 남는다 → OQ-P-309.
  **③ 계약 delta와 앱 반영이 같은 날 붙은 첫 사례** — #376이 하루 전 서버가 더한
  `placedBy.ownerType`을 곧바로 읽는다. `PlacedByResponse`에 필드가 생기고 매퍼가 `"ME"` 여부를
  **`CanvasToppingVO.isMine`(비널 `Boolean`)으로 접어** 문자열이 `:data` 경계를 못 넘게 한다.
  그 값이 두 자리를 정리한다 — `CanvasMainViewModel`의 상수 `false` 확장 함수가 사라지고,
  `CanvasBGEditViewModel`은 `GetMyAccountFlowUseCase` 의존과 진입 시 `first()` 대기를 통째로 버렸다
  (**계정 id와 그룹 멤버십 행 id를 견주던 축이 다른 비교**가 그 자리에 있었고, 이 화면에서 그
  판정은 표현이 아니라 **게이트**였다). OQ-P-250은 ①②가 닫히고 **③만 남는다** — 본인 토핑 탭이
  "Spotlight로 잘못 들어간다"에서 **"아무 일도 안 한다"**로 바뀌었을 뿐, C-305 목적지는 그대로 없다.
  모르는 값·`null`을 **거짓으로 접은 근거**는 매퍼 KDoc이 들고 있다 — 여는 쪽으로 틀리면 남의
  토핑을 만지게 된다. 같은 PR이 `http/README.md`의 `base_url` 예시를 **HTTPS 도메인**으로 옮기고
  `network_security_config.xml`에 "호스트를 안 가린다"는 경고를 주석으로 박았다(OQ-P-302 마커 —
  실제 빌드 주소는 `local.properties`라 여전히 안 보인다).

  직전 회차 요약: **바이너리 넷이 바뀐 라운드 — 그래서 무엇이 바뀌었는지를 커밋 메시지 말고는 아무도
  말해 주지 않는다**(delta 1건).
  #366이 커밋 하나·5파일 **삽입 97줄**(전부 `OFL.txt`)과 **`.ttf` 넷의 바이너리 교체**로 들어왔고,
  **머지 트리가 브랜치 팁과 같아** 충돌 해소 편집이 0건이다. **`.kt`가 한 줄도 안 바뀌어**
  유닛 785건·계측 14건이 그대로이고, 선작성 스펙·플랜이 없어 **아카이브 이동도 0건**이다.
  **① 고친 것** — SUIT 원본은 **빈 글리프(zero-contour)로 향하는 cmap 매핑**을 갖고 있어 해당
  문자가 화면에 **투명하게** 찍혔다(#365). 그 매핑만 걷어낸 수정본으로 갈아 플랫폼이 **시스템
  폰트로 fallback** 하게 했다. 글리프 아웃라인·폰트 버전이 그대로이고 파일명도 유지해
  `R.font` 참조와 `YGFontFamily`는 손대지 않았다 — **코드 심볼 드리프트가 0건인 이유가 이것이다.**
  **② 그 대가가 둘이다** — fallback 문자는 기기 시스템 폰트로 그려져 **한 화면에 두 글자체가
  섞이고**, 그 결과를 **실기기로 본 사람이 없다**(#365가 보고한 것은 투명하게 찍히는 쪽이다).
  그리고 OFL 1.1이 수정본 배포 시 라이선스 사본 동봉을 요구해 모듈 루트에 `OFL.txt`가 생겼는데,
  **그 파일은 APK에 실리지 않고 앱에 오픈소스 고지 화면도 없다**(S-001 항목은 계정·약관·개인정보·
  버전 넷이고 `oss-licenses` 계열 플러그인도 안 쓴다) → OQ-P-306.
  **③ 이 라운드가 문서에 남길 값어치는 회귀 감지선의 부재다** — 교체 대상이 바이너리라 diff로
  읽을 수 없고, 파일명·버전·아웃라인이 그대로라 **원본을 다시 넣어도 아무것도 빨갛게 되지
  않는다.** 무엇을 고쳤는지가 `OFL.txt` 고지 문단과 커밋 메시지에만 있으므로, 다음에 SUIT를
  올리는 사람이 그 둘을 안 읽으면 조용히 되돌아간다 → OQ-P-307.

  직전 회차 요약: **문서가 미결로 적어 둔 자리 넷이 하루에 닫혔고, 그중 하나는 문서가 못 박은
  순서를 뒤집고 닫혔다**(delta 7건).
  #351·#352·#353·#354·#357·#358·#368이 **17파일 삽입 383줄·삭제 153줄**로 들어왔고, **일곱 머지가
  전부 트리가 브랜치 팁과 같아** 충돌 해소 편집이 0건이다. **선작성 스펙·플랜이 하나도 없는
  라운드라 아카이브 이동은 0건**이고, 유닛은 781 → **785건**(늘어난 넷이 전부 #352), 계측은
  12 → **14건**(#351)이다. 이 라운드의 성격은 신규 기능이 아니라 **`open-questions`에 오래 걸려
  있던 항목의 회수**다 — 닫힌 것이 넷(OQ-P-076·OQ-P-047 ③·OQ-P-240 그리고 死필드 하나)이고,
  같은 손이 새로 연 것이 셋(OQ-P-303·304·305)이다.
  **① 순서를 뒤집고도 안 끊긴 해소** — #358이 `usesCleartextTraffic="true"`를 지우고
  `network_security_config.xml`을 놓았다(`base-config`는 평문 차단 + 시스템 인증서, `debug-overrides`는
  평문 허용 + 사용자 인증서). OQ-P-076은 **"① 앱 `YG_BASE_URL`을 HTTPS로 옮기고 ② 그다음 플래그를
  지운다"**를 못 박아 두었고 순서를 뒤집으면 앱이 즉시 끊긴다고 적었는데, 코드는 ②를 먼저 했고
  끊기지 않았다. 문서가 못 본 것은 **`debug-overrides`가 디버그 빌드를 예전 그대로 두는 세 번째
  선택지**였다 — 릴리즈만 HTTPS를 강제하므로 ①이 안 끝나도 지금 붙어 있는 개발 경로가 산다.
  CI도 `YG_BASE_URL`을 주입하지 않아 `BASE_URL_FALLBACK`(이미 `https`)으로 조립된다. ⚠️ 다만 **좁히기가
  이 문서의 권고보다 넓다** — "개발 서버 도메인만"이 아니라 디버그 빌드의 **모든 호스트**가 열렸고
  사용자 설치 인증서까지 신뢰한다. ① 자체는 `local.properties`라 커밋 delta로 볼 수 없어 OQ-P-302가
  계속 쥔다.
  **② 세 주 넘게 "미해결"이던 툴팁은 결정이 없어서가 아니라 근거가 없어서 걸려 있었다** — #352가
  `isTooltipVisible`을 `groups?.isEmpty() == true`로 세우면서 위키 [[g-001-empty-툴팁]]의 노출 조건이
  처음으로 코드와 맞았다. 여기서 값어치는 조건 자체가 아니라 **미조회(`null`)를 0건으로 안 세는
  것**이다 — ADR-0023이 `null`과 `emptyList()`를 가른 이유("섞이면 조회 전에 0건 툴팁이 뜬다")가
  그 결정 8일 만에 **실제로 소비된 첫 자리**다. 死필드로 두 달 가까이 남아 있던
  `GroupListUiState.isTooltipVisible`이 같은 커밋에서 노출 조건의 정본이 됐고, 유닛 4건이 네 갈래를
  잠근다.
  **③ 화면에 복제돼 있던 기본값이 컴포넌트로 내려갔고, 내려가면서 값이 바뀌었다** — #351이
  `YGCanvas.background`를 nullable로 바꾸고 기본값을 `Solid(Gray100)`에서 `null`로 내렸다. 화면의
  `DEFAULT_CANVAS_BACKGROUND`가 사라져 미설정·미지 type·색 파싱 실패가 전부 `null` 하나로 모이고,
  **폴백 그림은 `Gray100`이 아니라 흰 바탕**이 됐다(2026-08-17에 "컴포넌트가 배경 폴백을 갖지
  않는다"고 적어 둔 문장이 뒤집힌다). ⚠️ 같은 커밋이 **빈 안내판을 배경에 종속**시켰다 —
  `isEmpty && background == null`일 때만 뜨므로 배경을 고른 빈 캔버스에는 안내가 없다. 이 컴포넌트가
  Figma 5상태를 **직교 불리언**으로 표현하기로 한 원칙에서 한 칸 벗어난 자리이고, 조건 자체는 위키에
  근거가 없다 → OQ-P-304.
  **④ 배치 화면이 처음으로 진짜 캔버스를 보여 주는데, 그 경로에 테스트가 한 줄도 없다** — #357이
  초안의 `groupId`로 `GetTodayParfaitUseCase`를 다시 불러 배경(색·이미지)과 기존 토핑을 그린다.
  OQ-P-240이 물어 둔 셋에 코드가 전부 답했다(① 재조회 · ② 남의 토핑도 딤 아래 · ③ `positionZ`
  오름차순). ⚠️ 그런데 같은 라운드가 테스트에 더한 것은 **새 의존을 생성자에 끼우고 조회를 조용히
  실패시키는 스텁뿐**이다 — 기존 테스트가 초록인 이유가 **새 경로가 한 번도 성공하지 않기
  때문**이라는 뜻이고, 매핑·정렬·중복 조회 가드 어느 것도 안 잠겨 있다 → OQ-P-303.
  **⑤ 가짜 경로를 지우자 안내가 생겼는데, 그 안내가 실제로 나오는지는 아무도 안 봤다** — #354가
  `PropertySettingManager`의 `./error.jks` 폴백을 걷고 `Key?`를 돌려준다. 없는 경로를 채우면 AGP가
  그것을 그대로 믿어 **어떤 프로퍼티가 비었는지를 아무도 말해 주지 않았다**는 것이 근거다. 대신
  `validateSigningRelease`·`validateSigningDebug`에 `doFirst`를 얹어 **서명이 실제로 필요한 순간에만**
  실패시킨다(설정 단계에서 막으면 키를 못 받은 사람과 CI가 ktlint·테스트조차 못 돈다 — CI는 키를
  주입하지 않는다). ⚠️ 이 안내는 **태스크 이름 일치**에 걸려 있어 그 이름의 태스크가 없으면 조용히
  아무 일도 하지 않는다 → OQ-P-305.
  **⑥ 나머지 둘은 한 줄씩이다** — #353이 약관 모두동의 행의 `shape`와 `clip`을 함께 걷어 **각짐**으로
  바꿨고(둥근 모서리를 만들던 두 자리가 한 번에 사라졌다), #368은 `.gitignore`에
  `parfait-release.keystore`를 더했다. 커밋 제목이 "release keystore 추가"라 오해하기 쉬우나
  **키 파일 자체는 트리에 없다**(`ls-tree`로 확인). 키를 어디서 받는지가 저장소 어디에도 안 적혀
  있다는 사실은 OQ-P-305에 함께 적었다.

  직전 회차 요약: **세 주 전에 "사라졌다"고 적어 둔 결함이 다른 갈래에 살아 있었다**(delta 1건).
  #350이 `fix` 커밋 다섯(+브랜치 안 develop 병합 하나)·3파일 **삽입 59줄·삭제 48줄**로 들어왔고,
  **머지 트리가 브랜치 팁 `5ddfdf77`과 같아** 충돌 해소 편집이 0건이다. **선작성 스펙·플랜이 없는
  버그 라운드라 아카이브 이동도 0건**이고, `.kt` 셋 말고는 아무것도 안 변해 **테스트가 781건 그대로**다.
  이 라운드가 문서에 남길 값어치는 고쳐진 픽셀이 아니라 **문서가 참이라고 적어 둔 문장이 어떻게
  틀리는가**에 있다.
  **① 이슈가 먼저 봤다** — 갤러리 권한 거부 화면의 닫기 버튼이 설계보다 **상태바 높이만큼** 내려앉아
  있었다(#345). 원인은 `Scaffold`가 `innerPadding`을 **넘겨줄 뿐 소비하지 않는다**는 성질이다.
  Route가 그 `innerPadding`을 화면에 물린 위에서 `GalleryPermissionRequestComponent`가
  `windowInsetsPadding(systemBars)`을 한 번 더 걸어, 상단은 상태바만큼 밀리고 하단은 내비게이션 바
  인셋이 **닫기 줄 아래의 빈칸**이 되어 세로 가운데 정렬인 안내 블록까지 밀려 내려갔다.
  **② 문서가 이미 틀려 있었다** — [c102 스펙](superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)은
  2026-08-04에 "이번 PR이 화면 안의 `windowInsetsPadding(systemBars)`을 걷어내 **인셋 이중 적용이
  사라졌다**"고 적었다. 걷힌 것은 **목록 갈래**였고 권한 갈래는 그대로였다. **인셋 소유가 화면
  단위가 아니라 갈래(권한 / 콘텐츠) 단위로 갈릴 수 있다**는 결이 그 문장에 없었고, 그래서 안 걷힌
  쪽이 세 주를 살아남았다. 스펙 문장을 "목록 갈래에서는"으로 좁히고 as-built 재정정 절을 붙였다.
  **③ 카메라는 기전이 반대다** — 카메라 entry는 피드를 시스템 바 밑까지 깔려고 `innerPadding`을
  **일부러 안 준다**(의도적 예외). 그래서 `CameraPermissionRequestComponent`가 인셋을 직접 무는 것
  자체는 맞고, 틀린 것은 **무는 자리**였다. 닫기 `Row`에 물려 있어 하단 인셋이 버튼 아래 빈칸이
  됐고, 이번에 바깥 `Box`로 올려 각 변을 한 번씩만 물게 했다. **"화면이 직접 문다"는 형태는 무는
  주체까지 정해야 재현 가능한 규약이 된다** — 같은 화면에서도 피드 갈래는 컨트롤 `Column`이,
  권한 갈래는 컴포넌트 최외곽이 문다.
  **④ PR 요약 밖에 동작 변경이 하나 있었다** — `CustomCameraRoute`의 가이드 토스트 게이트가
  `LaunchedEffect(Unit)` → `LaunchedEffect(state.hasPermission)`이 되고 조건에 `hasPermission`이
  들어갔다. 촬영 요령을 말하는 토스트가 **권한 거부 화면 위에** 뜨던 것이 멈추고, 설정에서 권한을
  켜고 돌아온 시점에 처음 뜬다. 갤러리 Route는 진작 같은 게이트를 걸고 있었으므로 **카메라가
  갤러리 쪽에 맞춰진 것**이고, 두 모듈에 각각 정의된 같은 문구의 토스트가 이제 같은 조건으로 뜬다.
  PR 본문은 인셋만 말한다.
  ⚠️ **고친 결과를 실기기에서 본 사람이 없다**(OQ-P-301 신설) — PR 본문이 스스로 밝히듯 권한 거부
  상태를 로컬에서 재현하지 못해 비교 이미지의 "After"가 실측이 아니라 이슈에 붙은 디자인 이미지다.
  인셋은 기기의 실제 시스템 바 높이에 붙는 값이라 유닛 테스트로도 안 잠기고 스크린샷 테스트도 없어,
  이 화면의 세로 정렬은 지금 **사람 눈 말고 검증 수단이 없다**. 카메라 안내 블록에 붙은 하단
  `padding3` 보정은 대응 디자인이 아예 없다(코드 주석의 논증이 유일한 근거다).
  📌 **컴포넌트를 통째로 다시 짜고도 안 열린 문이 있다** — `permanentlyDenied`·`onClickGrantPermission`은
  여전히 두 컴포넌트 본문에서 쓰이지 않는다(2026-08-01 항목). 손이 그 자리를 지나갔는데도 안 열렸다는
  것은, 이 항목이 **잊혀서가 아니라 결정이 없어서** 남아 있다는 뜻이다.

  직전 회차 요약: **촬영이 최대 품질로 들어오기 시작했고, 누운 사진이 세워졌다**(delta 1건).
  #349가 커밋 다섯·9파일 **삽입 177줄·삭제 6줄**로 들어왔고, **머지 트리가 브랜치 팁
  `7f74458c`와 같아** 충돌 해소 편집이 0건이다. **선작성 스펙·플랜이 있는 라운드가 두 회차 연속으로
  왔으나, 이번에는 스펙이 통째로 들어오지 않았다** — [전처리 스펙](superpowers/specs/2026-08-23-segmentation-preprocessing.md)이
  항목마다 **근거 등급과 철회 조건**을 매겨 두었고, 그중 **근거가 확정된 1단계(Task 1~4)만** 들어왔다.
  그래서 **아카이브 이동이 0건**이고 스펙은 `draft` → **`in-progress`**, 계획은 active에 남는다.
  **이 라운드의 성격이 그 자체로 결과다** — 조건부 항목은 사람이 고정 사진 세트로 판정해야 하고,
  판정 전까지는 "확인 못 한 것을 근거로 위험한 쪽에 기울지 않는다"가 기본값이다.
  들어온 것은 셋이다. **① 손실이 처음 생기는 자리를 고쳤다** — `ImageCapture.Builder()`가 포맷·품질·
  capture mode를 아무것도 지정하지 않아 **기본값 JPEG로 촬영**되고 있었고(저장 시점의 압축은 두 번째
  세대였다), 이제 `CAPTURE_MODE_MAXIMIZE_QUALITY` + `setJpegQuality(100)`을 명시한다. **초판이 유일한
  무조건 항목으로 두었던 PNG 저장 전환은 들어오지 않았다** — 근거가 가장 약한 항목이 비용은 가장 컸다.
  **② API 28 미만 갈래가 누운 사진을 세운다** — `MediaStore.Images.Media.getBitmap`은 EXIF를 적용하지
  않고 `minSdk`가 26이라 그 갈래가 살아 있었다. `rotatedToUpright`가 회전을 픽셀에 적용하고 **회전 전
  판을 즉시 회수한다**(전체 해상도 판 둘이 함께 살지 않게). `ImageDecoder` 갈래는 그대로다 —
  적용 여부를 문서로 확인하지 못했고(OQ-P-280), 이미 정립된 판을 또 돌리면 두 번 돌아간다.
  **③ 계획이 스펙보다 앞당긴 항목이 하나 있다** — 최근 이미지 `SOURCE`의 확장자 판정이 `"jpg"`
  하드코딩에서 **바이트 판정**(`RecentImageRepositoryImpl#extensionOf`)으로 바뀌었다. 스펙은 이것을
  "PNG 채택 시"로 분류했으나 **PNG 없이도 지금 참인 결함**이었다 — 갤러리에서 고른 PNG가 `.jpg`
  이름으로 앉고, 나중에 배경으로 고르면 `ImageFileLocalDataSourceImpl#formatOf`가 확장자에서 유도된
  MIME을 바이트 스니핑보다 먼저 믿어 `image/jpeg`로 올라간다.
  **as-built 이탈 둘**은 후속 커밋 `be657892`가 스스로 고친 것이라 계약을 넓히거나 좁히지 않는다 —
  `readExifDegrees`의 KDoc 근거가 `ImageDecoder.createSource`에서 **실제 호출 경로**(`getBitmap`)로
  정정됐고, 입력 스트림이 `null`인 갈래에 경고 로그가 붙었다(재개방 실패가 상시 참이 되면 보정이
  조용히 무효가 되는 자리다).
  **테스트는 775 → 781건**(파일 89 → 90)이고 새로 덮인 것은 **판단이 든 순수 함수 하나**
  (`exifOrientationToDegrees`, 미러링 4종이 0인 것까지 고정)와 확장자 판정 2건뿐이다.
  `Bitmap`·`ImageDecoder`·`ExifInterface`·CameraX 빌더는 이 저장소에 Robolectric이 없어 JVM으로 못 덮고,
  **그 자리를 대신할 사진 세트 측정이 아직 미착수다.**
  문서 쪽 결과는 **상시 문서 넷**이다 — [module-structure](architecture/module-structure.md)의
  `core:util:android` 행(EXIF 보정·`androidx.exifinterface`·모듈 로거), [data-layer](architecture/data-layer.md)의
  `decodeUriToBitmap` 계약, [ADR-0014](adr/0014-logging-abstraction-kermit.md)의 모듈별 로거 목록,
  그리고 [api/image.md](api/image.md) Android 매핑(형식 판정이 업로드 경로 밖으로 한 자리 더 갔다).
  미결은 **신규 0건**이고 둘을 갱신했다 — OQ-P-280은 "보정하지 않음"이 실제 코드가 됐고,
  OQ-P-283은 감수하던 두 이름 공존이 **가정에서 사실**이 됐다.
  직전 회차 요약: **사진에 피사체가 여럿이면 이제 고를 수 있다 — 그리고 실패가 다시 화면이 됐다**(delta 1건).
  #342가 커밋 열셋·17파일 **삽입 1074줄·삭제 211줄**로 들어왔고, **머지 트리가 브랜치 팁
  `8fe67476`과 같아** 충돌 해소 편집이 0건이다. **선작성 스펙·플랜이 있는 첫 라운드가 여섯 회차
  만에 돌아왔다** — 스택 둘(PR1 data·domain / PR2 UI)이 **한 PR로 합쳐져** 들어와, develop 이력에는
  스택 경계가 남지 않았고 문서 쪽에서는 **스펙 하나와 플랜 둘이 함께 아카이브로 갔다**.
  정책에는 처음부터 있었다 — 위키가 기능정의서 v5를 근거로 "다중이면 C-103-select, 단일이면
  C-103"을 정의했고 구현이 그 갈래를 만들지 않았을 뿐이다. **후보를 하나로 접던 자리는 ML Kit
  옵션 한 곳**이었고(전경 전체를 합친 마스크 1장을 받아 bounding box를 하나만 누적했다), 이번에
  `enableMultipleSubjects` + `enableSubjectBitmap`으로 옮겨 가며 그 위에 선 경계들을 함께 넓혔다.
  **화면 ID는 쪼개지 않았다** — `NavKeySegmentation` 하나가 후보 수에 따라 점선 박스를 1개 또는
  N개 그린다(후보 1개면 전과 픽셀 단위로 같다).
  설계에서 값진 부분은 셋이다. **① 탐지와 저장이 갈렸다** — `segmentImage`가 디스크를 아예 안
  건드리고 신설 `persistSubject`가 **탭한 하나만** 저장한다. 진입 즉시 후보 수만큼 PNG를 떨구지
  않으므로 후보가 늘어도 진입 비용이 그대로다. **② 판단이 드는 자리를 순수 함수로 뺐다** —
  후보 필터(`filterCandidates`)와 좌표 변환·탭 판정(`scaledRectOrNull`·`pickCandidateIndex`)이
  Compose·`Bitmap` 비의존이라 JVM 유닛 15건이 덮는다. **이 화면에는 UI 테스트가 한 건도 없어서**
  탭 판정은 깨져도 조용한 자리였다. 탭은 맞는 후보 중 **면적 최소**를 고른다 — 목록이 면적
  내림차순이라 "뒤에서부터 첫 히트"와 결과가 같지만, 그렇게 쓰면 컴포넌트의 올바름이 필터의
  정렬 기준에 몰래 의존하고, 앞에서부터면 **큰 후보 안의 작은 대상을 아예 못 고른다**.
  **③ 선택 시점의 순서가 계약이 됐다** — 저장 → 초안 기록 **완료** → 로딩 해제 → 이동. 확인
  화면이 정상 진입에서 초안을 구독만 하므로 **이 화면이 유일한 writer**이고, 기록보다 이동이
  앞서면 다음 화면이 "다음"을 잠근 채 뜬다. 이동이 `goTo`라 화면이 백스택에 남아 `isLoading`은
  성공·실패·예외 세 갈래에서 각각 내린다(켠 채 나가면 돌아왔을 때 오버레이에 갇힌다).
  ⚠️ **실기기가 네이티브 크래시 하나를 드러냈다**(OQ-P-268 해소) — 전경 마스크 옵션과 다중 후보
  옵션을 **한 요청에 함께 켜면 ML Kit 다이나마이트 모듈이 `SIGSEGV`로 죽는다**(Galaxy A35 /
  Android 16). 스택이 전부 모듈 네이티브라 **`try/catch`도 Crashlytics도 못 잡고 `logcat -b crash`에만
  남는다.** 그래서 전경 마스크 경로는 지우지 않고 **후보가 0건일 때의 2차 요청**으로 남겼다 —
  대가는 세그멘터를 한 흐름에서 두 번 열 수 있다는 것이고, 정상 경로는 쓰지도 않던 원본 해상도
  `FloatBuffer`가 사라져 **오히려 가벼워졌다**.
  ✅ **실기기 확인이 0회가 아닌 드문 라운드다** — 점선 박스가 둘 이상 뜨는 것과 `C-103-Error`
  화면이 실제로 뜨는 것을 봤다. 계획 밖에서 들어온 것이 그 실패 화면이고, 디자인이 나오면서
  **PR #311이 삭제했던 `SegmentationErrorScreen`이 되살아났다**(OQ-P-153 ①②③ 해소). 토스트가
  통째로 대체되지는 않았다 — **대상을 아예 못 얻은 실패만 화면**이고(1회성 효과가 아니라
  `isError` 상태여야 재구성에서 살아남는다), 고른 뒤의 저장 실패는 후보 목록이 살아 있으므로
  토스트다.
  ⚠️ 남은 위험 넷은 전부 실기기 항목이다 — **후보 비트맵을 명시적으로 해제하지 않는다**(OQ-P-266,
  화면이 최대 5장을 원본과 겹쳐 든다) · **필터 상수 둘이 실측이 아니다**(OQ-P-267) ·
  **`segmenter.close()` 이후 비트맵 수명이 문서에 없다**(OQ-P-269 — 하이라이트가 그려지는 것으로
  ①은 한 번 통과했고 확인 화면 왕복이 남았다) · **같은 후보를 다시 고르면 초안에 적힌 테두리가
  조용히 덮인다**(OQ-P-277, 저장이 탭 시점으로 옮겨 오며 생긴 새 동작).
  문서 쪽 결과는 **아카이브 셋 + architecture 넷 + ADR 둘**이다. 스펙은 `implemented`로 아카이브
  이동, 플랜 둘은 `done`이다. [data-layer](architecture/data-layer.md)는 계약이 넷에서 **다섯**이
  되고 `SegmentationResult`가 `subjectBounds`를 잃은 것을,
  [design-system](architecture/design-system.md)은 **#311 판정이 이틀 만에 절반 뒤집힌 것**을
  (실패 화면만 돌아왔고 로딩 화면은 여전히 0개), [state-management](architecture/state-management.md)는
  **순서가 계약이 되는 사례**를, [navigation-flow](architecture/navigation-flow.md)는 **정책이 가른
  화면 ID를 목적지로 쪼개지 않은 판단**을 받았다. [ADR-0012](adr/0012-mlkit-subject-segmentation.md)는
  옵션 전환과 위 크래시 제약을, [ADR-0011](adr/0011-cross-module-bitmap-abstraction.md)은
  **도메인 모델이 `BitmapWrapper`를 다시 물었다**는 사실을 받았다(`SegmentationCandidate.bitmap` —
  파일이 아직 없는 구간을 비트맵으로 나를 수밖에 없다. 2026-08-14의 "적용 범위가 줄었다"가
  되돌아온 자리다). 계약 문서(`api/`)는 원격 연동 코드가 delta에 없어 **손대지 않았다.**
  겸해 [전처리 계획](superpowers/plans/2026-08-23-segmentation-preprocessing.md)의 베이스를 `develop`으로
  정정했다 — 그 계획이 스택 위에 쌓게 한 근거가 이번 머지로 사라졌다. 유닛 751 → **775건**(+24),
  테스트 클래스 87 → **89개**.
  직전 회차 요약: **편집 결과가 처음으로 남는다 — 그리고 남지 않은 것만 조용해졌다**(delta 1건,
  기준선 `d634efd3`).
  #336이 커밋 하나(+브랜치 안 develop 병합 하나)·6파일 **삽입 318줄·삭제 32줄**로 들어왔고,
  **머지 트리가 브랜치의 develop 병합 커밋 `dd29dce5`와 같아** 충돌 해소 편집이 0건이다. 선작성
  스펙·플랜이 없어 **아카이브 이동도 0건**이다. C-301 편집 탭은 2026-08-16에 생긴 뒤 일주일 동안
  **고친 것을 버리는 화면**이었고, 하루 전 PR #335가 삭제 하나만 열어 둔 상태였다. 이번에
  **이동·크기·회전이 확인 버튼에서 서버로 나간다** — `ToppingRepository.update`·`UpdateToppingUseCase`
  신설로 `parfait-image` 도메인의 미소비가 둘에서 **하나**(테두리 수정)로 줄고, OQ-P-199가 세 항목
  전부 답을 얻어 **해소됨**이 됐다.
  설계에서 값진 부분은 셋이다. **① 바뀐 것만 보낸다** — ViewModel이 조회 응답 스냅샷
  (`confirmedToppings`)을 화면 렌더링과 별도로 들고 있다가 확인 시점에 대조해, 위치·배율·각도 중
  하나라도 달라진 토핑만 PATCH 한다(안 건드린 토핑은 요청 0건). **② 순서에 근거가 있다** — 토핑
  들끼리는 `async` + `awaitAll`로 병렬이지만(순차면 확인 버튼이 바뀐 토핑 수만큼 느려진다) 배경
  저장보다는 **완전히 앞**이다. 둘을 진짜 병렬로 얽으면 한쪽만 실패한 경우를 갈라 다뤄야 하는데
  그 설계가 이번 범위 밖이라, 코드 주석이 그 판단을 그대로 적어 두었다. **③ `positionZ`를 안
  보낸다** — 계약이 부분 병합(`null`이면 유지)이라 서버 겹침 순서가 남고, 앱에는 z 조작 경로가
  없다. 확인 버튼 전체가 한 단위가 되면서 코루틴 키도 `SAVE_BACKGROUND_KEY` → `CONFIRM_KEY`다.
  ⚠️ **그런데 실패가 화면에 닿지 않는데 확인은 성공한다**(OQ-P-275) — 토핑 PATCH 실패가
  `viewModelLogger.e` 한 줄이고 그다음 배경 저장이 이어져, 배경이 성공하면 화면이 넘어간다.
  사용자는 옮긴 자리가 저장됐다고 믿지만 캔버스 메인은 재조회로 **옛 좌표**를 그린다. 같은 버튼
  안에서 **배경은 토스트 + 화면 잔류, 토핑은 무반응 + 화면 이동**으로 갈렸고, 마감된 캔버스에서는
  **같은 409가 한 번의 확인에서 두 처분**으로 나간다(OQ-P-261 — 이로써 그 코드의 처분은 넷이다).
  ⚠️ **다섯 중 테두리 재편집만 혼자 남았다**(OQ-P-276) — 변경 판정이 넷만 비교하고
  `borderLayers`·`editedImagePath`는 요청에도 없다. 테두리 PATCH는 여전히 소비처 0건이라, 다른
  넷이 저장되기 시작한 지금 **"확인했는데 이것만 사라지는" 갈래**가 됐다. 표시 쪽(OQ-P-254)과
  같은 값을 두고 갈라져 있어 한 라운드에서 함께 닫는 편이 낫다.
  ⚠️ **어제 근거 없이 지운 상한이 오늘 요청 값이 됐고, 회귀 테스트로 굳었다**(OQ-P-271) —
  `scale`·`rotation` 둘 다 앱에도 서버에도 막는 자리가 없는 채로 PATCH 본문에 실린다. 게다가 같은
  PR이 "예전 상한 2.5에 걸리지 않고 커진다"를 단언하는 테스트를 넣어, **되살리려면 그 테스트를
  함께 지워야 한다.** 회전 무제한도 같은 이유로 화면 안 문제가 아니게 됐다(OQ-P-241 ③).
  문서 쪽 결과는 **as-built 둘**이다 — 선작성 스펙·플랜이 없어 아카이브 이동은 0건이고,
  [c301-topping-edit-tab](superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)에 재정정 절이 붙어
  **드리프트 2가 거의 닫혔고**(넷 저장·하나 잔존) 드리프트 4에 "저장 계약이 생기면"이 현실이 됐다는
  마커가 들어갔다. [c301 배경 스펙](superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)에는
  확인 버튼이 더는 배경만 다루지 않는다는 짧은 재정정 절이 붙었다(배경 세 갈래 자체는 불변이고
  `saveBackground()`로 이름만 얻었다). 계약 문서는 delta가 없지만 소비가 늘어
  `api/parfait-image.md` Android 매핑·`api/README.md` 도메인 표를 고쳤다(**`verified`는 서버 계약
  대조일이라 미변경**, `android_status`도 `partial` 유지 — 테두리 하나가 남았다). 유닛 745 →
  **751건**(+6), 테스트 클래스 **87개** 유지. ⚠️ **실기기·실서버 확인은 여전히 0회다** — 이번
  라운드는 특히 **부분 실패**(토핑 셋 중 하나만 403)가 유닛으로 잠기지 않은 자리다.
  직전 회차 요약: **앱이 캔버스를 밖으로 내보내고, 처음으로 서버의 것을 지운다**(delta 2건,
  기준선 `96dc215c`). 두 머지 다
  **트리가 브랜치 팁과 같아**(#324 = `20295ba8`, #335 = `122d950b`) 충돌 해소 편집이 0건이고,
  둘 다 선작성 스펙·플랜이 없어 **아카이브 이동은 0건**이다. 공통점은 **오래 걸려 있던 `TODO`
  두 개가 같은 날 닫힌 것**이고, 차이는 닫는 방식이다 — 하나는 없던 계층을 새로 쌓았고 하나는
  이미 있던 표면에 소비자를 붙였다.
  **① 갤러리 저장**(PR #324, 12파일 **삽입 416줄·삭제 26줄** → 사후 스펙 신설
  [c001-canvas-gallery-save](superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)). 지난 캔버스
  메뉴의 "갤러리에 저장"이 **로그 한 줄에서 실제 저장**이 됐다(OQ-P-211 해소, 넷 다 답이 나왔다).
  설계에서 값진 부분은 둘이다 — **한 동작이 MVI 왕복 두 번으로 갈린다**(비트맵은 컴포지션만 만들 수
  있어 ViewModel이 `RequestCanvasCapture`로 요청만 보내고 화면이 `toImageBitmap()` 결과를
  `SaveCapturedCanvas`로 되돌린다), 그리고 **캡처 레이어를 어디에 거는가**다 — 바깥이 아니라
  **배경·토핑만 담는 안쪽 `Box`**에 걸어 테두리·컷 도형·빈 캔버스 문구·날짜 버튼을 뺐다. 그래서
  `YGCanvas`가 "무엇이 그림이고 무엇이 프레임인가"를 처음으로 갈랐고, 그 경계가 곧 저장되는 이미지의
  경계다(정책 소스가 없어 코드가 확정). 쓰기는 `IS_PENDING` 등록 → 바이트 → 내림이고 중간 실패는
  등록 자체를 지운다. 권한은 `GalleryWritePermissionManager`가 API 29 미만에서만 보고 매니페스트도
  `maxSdkVersion="28"`로 좁혔다(`minSdk` 26이라 살아 있는 갈래이고, 요청에 Activity가 필요해 **Route가
  캡처한 비트맵을 들고 기다린다**).
  **② 토핑 삭제**(PR #335, 6파일 **삽입 163줄·삭제 9줄**). C-301 편집 탭의 삭제 확인 모달이 곧
  `DELETE`이고 **성공해야 목록에서 뺀다** — `ToppingRepository`의 둘째 갈래가 소비 화면과 함께 열려
  `parfait-image` 도메인의 미소비가 셋에서 **둘**(위치·테두리 수정)로 줄었다. **앱이 서버 데이터를
  지우는 첫 경로**이고 OQ-P-199 ③이 "확인 모달 시점에 즉시"로 닫혔다.
  ⚠️ **그런데 삭제만 즉시 영구이고 실패는 화면에 닿지 않는다**(OQ-P-270) — 이동·크기·회전은 여전히
  나가면 사라지는데 삭제는 "그만두기"로 나가도 안 돌아오고, 403·409·404가 전부 `viewModelLogger.e`
  한 줄로 접힌다. 같은 화면의 배경 저장은 같은 실패를 토스트로 보여 주므로 **한 화면 안에서 처분이
  갈렸고**, 409는 같은 서버 코드에 **세 번째 처분**을 더했다(OQ-P-261).
  ⚠️ **같은 커밋이 근거 없이 크기 상한을 지웠다**(OQ-P-271) — `TOPPING_MAX_SCALE = 2.5f`가 사라져
  이제 앱에도 서버에도 배율을 막는 자리가 없고, 상·하한 없는 축이 둘이 됐다.
  ⚠️ **캡처는 지금 그려진 것을 복사한다**(OQ-P-272) — 배경 `AsyncImage`가 늦으면 그대로 담기고
  해상도도 기기 종속인데 코드가 다루지 않는다. ⚠️ **얼럿 호스트와 문자열 셋이 트리거 없이 들어왔다**
  (OQ-P-273 — 프리뷰만 띄운다). ⚠️ **저장이 API 29를 경계로 위치도 보호도 갈린다**(OQ-P-274).
  문서 쪽 결과는 **사후 스펙 하나와 as-built 하나**다 — 갤러리 저장은 새 스펙이 갖고, 토핑 삭제는
  [c301-topping-edit-tab](superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)에 재정정 절로 들어갔다
  (드리프트 2 부분 해소, **드리프트 4의 "크기는 클램프한다"는 전제 정정**). 계약 문서는 delta가
  없지만 소비가 늘어 `api/parfait-image.md` Android 매핑·`api/README.md` 도메인 표를 고쳤다
  (**`verified`는 서버 계약 대조일이라 미변경**, `android_status`도 `partial` 유지). 유닛 737 →
  **745건**(+8), 테스트 클래스 **87개** 유지. ⚠️ **실기기·실서버 확인은 여전히 0회이고, 이번 라운드는
  유닛이 닿지 않는 자리가 특히 넓다** — 캡처 결과물·권한 다이얼로그·`MediaStore` 쓰기가 한 줄도 안
  잠겨 있다.
  직전 회차 요약: **앱이 처음으로 화면 방향을 정했고, 그 결정에는 시한이 붙어 있다**(delta 1건,
  기준선 `96dc215c`). #339가
  커밋 일곱·6파일 **삽입 42줄·삭제 17줄**로 들어왔고, **머지 트리가 브랜치 팁 `cab38993`과 같아**
  충돌 해소 편집이 0건이다. **`.kt` 파일이 0건인 첫 라운드**다 — 매니페스트 둘·문자열 둘·버전
  카탈로그·gradle wrapper가 전부이고, 그래서 테스트도 변하지 않았다(유닛 737건·클래스 87개 유지).
  들어온 것은 세 갈래다.
  **① 세로 고정**([ADR-0027](adr/0027-portrait-orientation-lock.md) 신설). 그전까지 방향은 아무도
  정하지 않아 기기 설정을 따랐는데, 이 앱의 화면 규격은 세로 폭 전제다(목록 지그재그 좌표·C-101
  뷰파인더 여백·달력 그리드가 실측이다). 결정에서 값진 부분은 **어디에 붙였는가** 둘이다 —
  카카오 리다이렉트 액티비티에도 붙였고(빼면 하필 **로그인 구간에서만** 회전이 살아 있는데, 그
  구간이 A-002 리뷰가 잡은 로그인 유실 경로다 — OQ-P-146 ⑨의 재현 경로가 이번에 닫혔다),
  `<application>`에 대화면 opt-out 속성을 얹었다(`targetSdk 36`은 sw600dp 이상에서
  `screenOrientation`을 무시해, 안 붙이면 폰은 고정되고 태블릿·폴더블만 눕는다 — 그쪽 레이아웃은
  아무도 만든 적이 없다).
  **② 표기·버전**: 앱 표시명이 소문자 `parfait`에서 `Parfait`으로 올라가고(프리뷰도 함께),
  `appVersionName`이 **내려갔다**(`versionCode`와 프리뷰 버전은 그대로다 — 스토어에 올린 적이
  없어 되돌릴 수 있는 자리다).
  **③ 의존성 일괄 상향**: AGP·Kotlin·KSP·Compose BOM·OkHttp·Kakao SDK·Navigation3(alpha 계열
  안에서)·Lottie·Hilt Navigation Compose·Kermit·Firebase BOM·Crashlytics 플러그인·Gradle
  wrapper. 코드 수정이 0건인 채로 **CI `lint`·`unit-test`가 둘 다 통과**해, 이 상향이 컴파일·유닛
  수준에서는 무해하다는 것까지는 확인됐다.
  ⚠️ **이 결정은 시한부다**(OQ-P-264) — 대화면 opt-out 속성은 `targetSdk 37`부터 제거돼
  무력화되는데, 그 사실이 지금 **매니페스트 주석의 TODO 한 줄에만** 있고 대화면 방침은 없다.
  ⚠️ **세로 고정이 카메라의 기준을 함께 고정했다**(OQ-P-265) — `CustomCameraRoute`는
  `targetRotation`을 주지 않고 `ImageProxy`의 회전값으로 보정하는데, 그 기준이 표시 방향이라
  **가로로 들고 찍었을 때의 결과가 이번 라운드에서 바뀌었다.** 촬영 결과는 누끼·배치·캔버스까지
  그대로 흘러가므로 한 번 누우면 흐름 끝까지 눕는다.
  문서 쪽 결과는 **ADR 하나와 미결 둘**이다 — 선작성 스펙·플랜이 없고 화면 변경도 없어 **아카이브
  이동 0건**이고, 매니페스트 결정은 스펙이 아니라 ADR 자리라 [ADR-0027](adr/0027-portrait-orientation-lock.md)을
  신설했다. 겸해 [ADR-0006](adr/0006-navigation3-custom-navigator.md)이 채택 당시 alpha 버전을
  현행 핀처럼 적어 두고 있어 문구를 고쳤다(이번 상향으로 실제로 어긋났다).
  계약 문서(`api/`)는 원격 연동 코드가 delta에 없어 **손대지 않았다.**
  ⚠️ **실기기 확인은 여전히 0회이고, 이번 라운드는 유닛으로 덮을 수 없는 종류다**(매니페스트 속성).
  직전 회차 요약: **고른 배경이 처음으로 남는다 — 그리고 편집 화면이 mock을 버렸다**(delta 1건,
  기준선 `8eb2af7d`). #329가
  커밋 열셋·44파일 **삽입 1677줄·삭제 218줄**로 들어왔고, **머지 트리가 브랜치 팁과 같아** 충돌
  해소 편집이 0건이다. C-301은 2026-08-15에 생긴 뒤 일주일 동안 **고른 값을 버리는 화면**이었다
  (확인 이펙트에 배경을 실어 놓고 Route가 `// TODO` 주석과 함께 그냥 물러났다). 이번에 확인이
  세 갈래로 갈린다 — 색이면 `#RRGGBB`로 바로 PATCH, 기기에서 고른 사진이면 **캐시 복사 → 발급 →
  S3 PUT → confirm**으로 `imageId`를 얻어 PATCH, 서버에 이미 있던 배경이면 **요청 0건**이다
  (https 주소는 기기가 읽을 수 없어 다시 올릴 수도 없다). 그래서 **앱이 서버에 쓰는 두 번째
  경로**이고, `api/parfait.md`가 `done`이 됐다(회전 제외 5/5 소비). 순서에 근거가 있다 —
  **저장이 끝나야 화면을 넘긴다**(먼저 넘기면 캔버스 메인이 저장 안 된 배경을 그린 채 서 있다가
  다음 조회에서 슬그머니 되돌아간다). 저장 성공인데 앱이 모르는 `type`이 와서 그릴 값이 없으면
  **실패로 다루지 않고 고른 값으로 그린다**(OQ-P-193 ① 답). 다만 그 폴백은 **아직 아무것도 바꾸지
  않는다** — Route가 실린 배경을 쓰지 않고 돌아간 캔버스 메인이 재조회로 그리기 때문이다
  (OQ-P-194 ③ 답).
  **두 번째 변화가 더 크다** — 같은 화면의 토핑 탭이 `loadMockToppings()`를 버리고 **서버 캔버스를
  그린다**(OQ-P-199 ①). 좌표계가 Dp 오프셋에서 **Canvas-Area 대비 0~1 중심점**으로 바뀌고 배치
  규칙 셋이 `util/ToppingGeometry.kt`로 올라가 **캔버스 메인·편집 탭·배치 화면이 같은 값을 본다** —
  같은 캔버스가 두 화면에서 다르게 보이던 상태가 끝났다. 진입도 함께 정리됐다:
  `NavKeyCanvasBGEdit`가 `data class(groupId, parfaitId)`가 되고, C-001이 **오늘 캔버스를 못 받았으면
  편집을 아예 열지 않는다**(없는 id를 지어내면 남의 날 캔버스를 고친다).
  ⚠️ **그런데 소유 판정이 축이 다른 두 id를 견준다**(OQ-P-250) — `isMine`이 계정 id와 그룹 멤버십
  행 id를 비교하고 코드 KDoc이 그 사실을 스스로 적어 두었다. 캔버스 메인에서는 Spotlight 갈래가
  갈릴 뿐이었지만 **편집 화면에서는 그 판정이 곧 게이트**다: 참으로 새면 남의 토핑을 만지고,
  거짓으로 접히면 내 토핑도 못 만진다. 실기기 1회로 어느 쪽인지 바로 드러난다.
  ⚠️ **마감된 캔버스의 409가 "잠시 후 다시 시도해 주세요"로 접힌다**(OQ-P-261) — 다시 눌러도 영원히
  실패하는데, 같은 상수를 C-106 배치는 되감기 판정에 쓴다. **한 서버 코드에 두 처분이 생겼다.**
  ⚠️ **업로드 캐시가 쌓이기만 한다**(OQ-P-262) — `cacheDir/upload`에 UUID 이름으로 떨구고 지우는
  코드가 없다(세그멘테이션·최근 이미지에는 정리 정책이 있다). ⚠️ **색을 6자리로 적는 함수가 둘이
  됐다**(OQ-P-263) — 새 `Color.toRgbHex()`는 알파를 조용히 버리고 `Locale.US`를 고정하는데, 기존
  `Int.toRgbHexString()`은 알파에 `require()`로 던지고 **로케일을 안 고정한다**(아라비아 숫자 기기에서
  서버가 못 읽는 문자열이 된다).
  문서 쪽 결과는 **as-built 둘**이다 — 선작성 스펙·플랜이 없어 아카이브 이동은 0건이고, C-301
  스펙 둘(배경·토핑 탭)의 드리프트 1이 각각 닫히며 재정정 절이 붙었다. `AppError`에 네 번째
  갈래(`UnsupportedImage`)가 생긴 것도 이 라운드다 — **서버가 아니라 기기에서 오는 실패**의 첫
  사례이고, 그 갈래만 재시도가 무의미해서 문구가 갈린다. 유닛 696 → **737건**(+41), 테스트 클래스
  81 → **87개**. ⚠️ **실기기·실서버 확인은 여전히 0회.**
  직전 회차 요약: **화면이 어떻게 바뀌는지가 처음으로 앱의 결정이 됐다**(delta 1건, 기준선
  `a0d584ef`). #326이 커밋 다섯·
  5파일 **삽입 156줄·삭제 1줄**로 들어왔고, **머지 트리가 브랜치 팁 `07f0a6a2`와 같아** 충돌 해소
  편집이 0건이다. 그전까지 전환은 아무도 정하지 않아 Navigation3 기본(페이드 + 축소)이었고, 이제
  **새 화면이 오른쪽에서 들어와 지금 화면을 덮고 뒤로 가면 오른쪽으로 빠진다.**
  설계의 핵심은 **셋을 한 값으로 묶은 것**이다 — `NavTransition`(`core:navigation`)이
  `push`·`pop`·`predictivePop`을 함께 들고, 따로 넘기지 않는 이유는 방향이 짝을 이뤄야 "덮였다
  걷힌다" 하나의 동작으로 읽히기 때문이다. 화면별 예외는 `metadata` 한 줄로 앱 기본을 덮는데,
  **붙이는 대상이 위에 놓이는 화면**이라는 것이 이 API의 함정이고(A → B의 모양은 B가 정하고 되돌아올
  때도 B의 것이다) 그 사실이 KDoc에 남았다.
  ⚠️ **그런데 이 라운드가 만든 것을 아무도 본 적이 없다**(OQ-P-260). `NavTransitionTest`가 잠그는
  것은 **세 슬롯이 비지 않았다**는 것뿐이고(하나라도 비면 그 방향만 라이브러리 기본으로 튄다) 모양·
  시간·방향은 실기기 몫인데 실기기 확인이 0회다. 더 나쁜 것은 **유일한 예외가 도달 불가 화면에
  붙었다**는 점이다 — `NavTransition.Fade`는 `NavKeyCanvasEdit`에 붙었고 근거는 그 화면과
  `NavKeyCanvasImageSelect`가 사진 하나를 공유 요소로 잇는다는 것인데, **그 짝은 `goTo` 호출부가
  0건**이고(C-001이 그 둘로 가지 않는다) 진입하는 코드는 mock uri를 넘기는 한 줄뿐이다(OQ-P-129 ②). 즉 **예외의 근거가 되는 전환 자체가 실행되지 않는다.**
  ⚠️ 복제도 하나 늘었다 — 앱 기본을 `NavDisplay`에 물리는 세 줄이 `app`의 `MainRoute`와
  `app-preview`의 `RootRoute` **두 곳에 똑같이** 있고, 한쪽만 고치면 갤러리와 본 앱의 전환이 갈린다
  (OQ-P-259). 데코레이터 셋도 이미 같은 형태로 복제돼 있어 **이 항목은 전환만의 문제가 아니다.**
  **문서 쪽 결과는 지워진 주석을 받아 낸 것**이다 — 최종 커밋이 `Fade`의 "언제 쓰나" KDoc(방향을 말할
  수 없는 전환: 앞뒤 관계가 없는 경계이거나 공유 요소가 자리를 옮기는 전환)을 지웠는데, 그 갈래는
  코드가 말하지 않고 호출부 주석에도 절반만 남았다. [parfait/CLAUDE.md](code-conventions.md)의 최소 보존선대로
  [navigation-flow](architecture/navigation-flow.md)에 「화면 전환」절을 신설해 옮겼다. 선작성
  스펙·플랜은 없어 **아카이브 이동 0건**이다. 유닛 694 → **696건**, 테스트 클래스 80 → **81개**.
  계약 문서(`api/`)는 원격 연동 코드가 delta에 없어 **손대지 않았다.**
  직전 회차 요약: **머지 하나에 PR 넷이 들어와, 앱이 처음으로 서버에 무언가를 만든다**(delta 1건, 기준선 `19cb5299`).
  머지는 하나지만 실린 것은 C-106 결선 스택 **PR3~PR6 넷**이다 — 커밋 **43개**, **77파일 삽입
  3602줄·삭제 433줄**. **머지 커밋의 트리가 PR6 브랜치 팁 `656cbf2e`와 같아** 충돌 해소 편집이
  0건이고, 그래서 이 회차가 한 일의 대부분은 새 감사가 아니라 **네 브랜치에 미리 붙여 둔 as-built를
  재측정 없이 develop 사실로 승격하는 것**이었다(선작성 스펙 1·플랜 4가 전부 있었다).
  **가장 큰 변화는 방향이다** — 지금까지 검증 안 된 실서버 경로는 전부 읽기였고, 이번에 확인 버튼
  하나가 **발급 → S3 PUT → confirm → 배치** 네 단계를 태운다. 실패하면 서버에 흔적이 남는다(고아
  `PENDING` 이미지·S3 객체). "저장 없이 화면만 바뀐다"던 상태가 끝났다(OQ-P-238 ② 해소).
  **PR3**(초안 SSOT + C-001 정비): 흐름 상태가 `:data` DataStore의 **초안 한 벌**로 모이고
  ([ADR-0026](adr/0026-topping-draft-datastore-ssot.md), 이번에 `accepted`) `NavKeyCanvasToppingPlace`가
  **인자를 잃어 `data object`가 됐다** — `camera`·`segmentation`이 캔버스 개념을 떠안지 않는 것이 이
  배치의 실익이다. C-001이 `YGScaffoldV2`로 옮겨 오늘 캔버스 조회 실패가 표현을 얻었고(V1 잔여
  2파일 그대로, 호출 곳 5 → 4), 토핑 추가 버튼에 가드가 붙었다(`YGCanvasMenuAction.isEnabled` 신설).
  **PR4**(테두리 계약 전환): 테두리를 픽셀에 굽는 것을 멈추고 서버 필드로 보낸다
  ([ADR-0025](adr/0025-topping-border-as-server-field.md), 이번에 `accepted`). 8방향 스탬프가
  `:core:designsystem`의 `YGToppingCutoutImage`로 올라가 화면 셋이 나눠 쓰고, 캔버스 종횡비 상수가
  `domain`에서 지워져 **정본이 하나가 됐다**(OQ-P-177 ① 해소 — 상수가 하나뿐이라 갈라짐을 막을 단언이
  필요 없어졌다). ⚠️ 대가는 **편집 화면에서 본 테두리가 그다음 화면들보다 굵어 보이는 것**이고,
  어느 쪽이 정책인지는 여전히 근거가 없다(OQ-P-245).
  **PR5**(결선): 좌표 변환·업로드 호출·로딩 오버레이·실패 판정이 붙었다. ⚠️ **이 라운드의 진짜
  결정은 되감지 않기로 한 것**이다 — 애초 설계는 영구 실패에서 캔버스까지 되감는 것이었는데, 최종
  브랜치 리뷰가 `CanvasToppingPlaceRoute`의 `toastPolicy`가 그 Route 컴포지션에 매달려 있어
  **되감기가 안내를 같은 프레임에 함께 폐기한다**는 것을 Critical로 잡았다. 같은 파일이 `DraftMissing`을
  안 되감는 이유로 이미 그 함정을 주석에 적어 두고 바로 아래에서 같은 실수를 반복하고 있었다. 처방은
  알린 뒤 화면에 남기는 것이고, **진짜 처방(안내를 캔버스 쪽 토스트 호스트로 보내기)은 또 미뤄졌다**
  (OQ-P-167). 영구 실패 코드도 셋에서 **다섯**으로 늘었다(400 둘 — 재시도가 4단계를 다시 태워도 결과가
  같은 400이라 참조되지 않는 이미지만 쌓인다). 소비자가 붙으며 잠들어 있던 결함 둘도 함께 닫혔다
  (OQ-P-109 발급 응답 본문 로깅 → `@NoBodyLog` + `SelectiveLoggingInterceptor`, OQ-P-246 블로킹
  `execute()` → `enqueue` + `suspendCancellableCoroutine`).
  **PR6**(누끼 알맹이 재사용): 배치에 성공한 **테두리 없는 알맹이**가 갤러리 "최근"에 남고, 그것을
  고르면 촬영·세그멘테이션을 건너뛰어 확인 화면으로 직행한다 — **흐름에 두 번째 입구가 생겼다.**
  선행 결함 넷을 함께 닫았고(OQ-P-255) 그중 값진 것은 **2단 폴백 디코드**다: 목록 스키마를 넓히면서
  폴백이 없으면 구 `List<String>` 디코드 실패를 `getOrDefault(emptyList())`가 삼켜 **기존 목록이 통째로
  날아가고 파일만 고아로 남는다**(정리가 목록 기준이라 지우지도 못한다). ⚠️ 감수한 대가는 상한
  `MAX_SIZE = 9`를 토핑 흐름 하나가 **두 칸씩** 먹는 것이다(OQ-P-258).
  테스트: 유닛 602 → **694건**(+92), 테스트 클래스 70 → **80개**.
  계약 문서 쪽 결과는 **`api/image.md`가 `done`이 된 것**이다 — 두 엔드포인트가 전부 화면까지 이어져
  **표면만 있고 소비처가 0인 도메인이 사라졌다**. `parfait-image.md`는 배치 하나만 소비돼 `partial`
  그대로다(나머지 셋은 C-301 라운드). "Android 불일치"는 **0건 유지**다.
  ⚠️ **실기기·실서버 확인 없음** — 스택이 남긴 실기기 항목은 이월 13 + 신규 9인데 하나도 돌지
  않았다. 실기기 1회로만 갈릴 것이 셋이다: presigned PUT에 `Authorization`이 안 붙는가(붙으면 업로드가
  아예 안 된다) · 배치 화면에서 본 그림이 캔버스에 같게 그려지는가(두 화면의 클립 모양이 다르다) ·
  테두리 색 `#RRGGBB` 왕복이 실제 응답에서도 성립하는가(OQ-P-146).
  직전 회차 요약: **두 라운드가 각각 코드를 지웠고, 지운 근거가 같다 — 지키고 있던 조건이 실은 값이
  없었다**(delta 2건, 기준선 `ef55a58c`). #311이 세그멘테이션의 로딩·에러 전용 화면 둘을 지워 공통
  오버레이·공통 토스트로 넘겼고(ygscaffold-v2 스펙의 제외 두 항목 철회, 화면 고유 로딩 화면 0개),
  #325가 FCM을 통째로 걷었다(결선된 적 없는 기능 때문에 첫 실행마다 알림 권한을 묻고 있었다.
  ADR-0013은 폐기가 아니라 FCM 축만 철회로 정정). 유닛 602건·클래스 70개 유지.
  직전 회차 요약: **한 화면이 처음으로 사용자에게 되묻기 시작했고, 그 아래층은 아직 아무도 부르지 않는다**(delta 5건, 기준선 `da03c9b0`).
  다섯 머지 **전부 트리가 브랜치 팁과 같아** 충돌 해소 편집이 0건이고, 브랜치 사실이 그대로
  develop 사실이다. 라운드를 관통하는 것은 **"보이는 것"과 "쌓이는 것"이 갈렸다**는 점이다 —
  #298·#312·#319·#320은 전부 화면에서 눈에 보이는 변화이고, 가장 큰 #322는 화면에서 아무것도
  바뀌지 않는다.
  **#298**(`feature/spotlight-topping`): **캔버스 토핑이 처음으로 탭을 받는다.** 위키 C-202가 정의한
  Default ↔ Spotlighted가 상태 필드 하나(`spotlightedToppingId`)로 들어왔고, 그리기 순서가 곧 정책의
  z 우선순위다(나머지 토핑 → Dim `Black50` → 강조 토핑). **정책 대조에서 여덟이 맞고 둘이 빈다** —
  본인 토핑 갈래(C-305 진입)는 **내 멤버십 행 id를 알 길이 없어 `isMine()`이 상수 `false`**라
  본인 토핑도 Spotlight로 들어가고(OQ-P-250), 탈퇴 사용자 문안은 서버가 넣어 준 `(알수없음)`이
  그대로 문장이 된다(OQ-P-251 ②). ⚠️ **이 라운드의 진짜 결정은 토스트 자리다** — `YGCanvas`에
  다섯 번째 슬롯 `overlayContent`가 뚫려 토스트가 스캐폴드가 아니라 **캔버스 프레임 상단**에 서고,
  그 결정이 아직 머지도 안 된 PR3의 조회 실패 토스트 자리까지 정했다(OQ-P-167). 즉 **"공통 실패는
  `YGScaffoldV2`가 자리를 준다"가 전 화면 규칙이 아니게 된 것**이 이 라운드에서 굳었다.
  작성자 칩은 서버 `placedBy.nameTagChip`이 아니라 화면 `memberChips` 조인인데, **탈퇴 멤버에서도
  서버 값과 결과가 같아** 임시라는 사실이 화면에 안 드러난다(OQ-P-251 ①). ⚠️ **신규 유닛 0건** —
  판단이 몰린 순수 함수 둘(`toElapsedTimeBucket`·`toSpotlightToastNameColor`)이 안 덮였다(OQ-P-252).
  **#322**(`feature/#270-topping-place-domain`): C-106 결선 스택 **1/5·2/5가 한 PR로** 들어왔다
  (PR2 브랜치가 PR1 커밋 여섯을 업고 올라갔다). `@UploadClient`·`PresignedUploadDataSource`·
  `ImageUploadRepository`·`ToppingRepository`·`AddToppingUseCase`가 생기면서 **Repository가 0건인
  도메인이 사라졌다**(image·parfait-image가 마지막 둘이었다). 저장소에서 **Retrofit 밖 raw OkHttp를
  쓰는 첫 자리**이고, 그래서 `@NoAuth` 판정이 안 걸려 전용 클라이언트가 성능이 아니라 **기능 전제**다.
  ⚠️ **소비자가 0이라 Dagger가 도달할 수 없고**(`dagger.fullBindingGraphValidation` 미설정)
  리플렉션 바인딩 테스트가 유일한 감지선이라는 계획의 전제가 develop에서도 그대로다.
  **#312**(`feature/group-list-my-nickname`): G-001의 마지막 mock이 사라졌다. 닉네임이 계정 SSoT
  구독이 되며 **전역 닉네임으로 확정**됐고(OQ-P-197 해소), 이펙트가 값을 실어 Route가 상태를 다시
  읽지 않는다. ⚠️ 대가는 **값이 아직 없으면 그룹 만들기가 조용히 안 열리는 것**이다(OQ-P-253) —
  같은 오버레이의 두 버튼이 서로 다르게 반응한다.
  **#319·#320**: 잔버그 둘. 달력 항목 사이 여백을 누르면 뒤의 Dim이 받아 달력이 닫히던 것을
  `YGCanvas`가 탭을 소비해 막고(막는 자리를 슬롯 주입자가 아니라 컴포넌트에 뒀다), 그룹 추가
  오버레이를 켜 둔 채 나가면 돌아왔을 때 누른 적 없는 오버레이가 떠 있던 것을 나가는 길에 접는다.
  테스트: 유닛 561 → **602건**(+41 전부 #322·#312·#320), 테스트 클래스 64 → **70개**.
  ⚠️ **실기기·실서버 확인 없음** — 이번에 처음 토핑을 탭할 수 있게 된 화면인데 그 토스트를 본 적이 없고,
  업로드 경로는 여전히 실서버 요청 0건이다.
  직전 회차 요약: **문서가 "임시"라고 적어 둔 자리 셋이 결정으로 바뀌었다**(delta 2건, 기준선 `36719e8e`).
  #315는 온보딩 약관의 실패 표현 둘을 확정했고(재시도 동선이 화면 안에 있으면 화면에 남기고 없으면
  토스트 — OQ-P-167 ①의 답) 스캐폴드 V1 잔여를 2파일로 줄였다. #318은 하루 전 서버 409 가드로
  거짓이 된 앱 주석 일곱을 지우지 않고 고쳤고, `ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED`를
  **소비처 0건인 채로** 신설하며 "처분이 이미 정해진 코드는 미리 둔다"는 예외를 함께 적었다.
  ⚠️ 그 상수 KDoc이 남긴 경고가 그 라운드의 발견이다 — 다섯 경로 전부 권한 검사가 마감 검사보다
  앞이라 마감된 캔버스라도 남의 토핑·비멤버면 409가 아니라 403이 먼저 온다. 유닛 560 → 561건.
  직전 회차 요약: **문서가 조건부로 적어 둔 문장 여섯이 한 커밋에 조건을 잃었다**(delta 1건).
  마지막 남은 미머지 브랜치(`refactor/segmentation-develop`)가 들어왔고, **머지 커밋의 트리가 브랜치
  팁과 같다** — 충돌 해소 편집이 0건이라 [스펙 as-built 재정정 절](superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md#as-built-재정정-2026-08-20-두-번째-리베이스)의
  수치를 **재측정 없이 그대로 develop 사실로 승격**할 수 있었다(유닛 538 → **560건**, 클래스 61 → 64).
  그래서 이 라운드가 한 일도 새 감사가 아니라 **"브랜치에만 있다"고 단서를 달아 둔 서술에서 그 단서를
  떼는 것**이었다. 조건부로 적혀 있던 것이 실제로 그 조건을 만난 사례라 **문서가 예측한 대로 닫혔는지**가
  이번의 관전 포인트다.
  **가장 잘 맞은 예측은 캐시 정리의 안전 근거다** — OQ-P-003 ③은 "세그멘테이션 진입 시 캐시를 통째로
  비워도 안전하다. 새 흐름은 캔버스에서만 시작하고, 그러려면 이전 흐름 화면이 이미 백스택에서 걷혀
  있으니까"라고 적고 곧바로 **"단 `refactor/segmentation-develop` 머지 시점부터"**라는 단서를 달았다.
  그 단서가 필요했던 이유는 근거를 참으로 만드는 수정(`CanvasToppingPlaceRoute`의 배치 완료 이펙트를
  `goTo(NavKeyCanvasMain(groupId = 0L))`에서 `popUpTo<NavKeyCanvasMain>()`으로)이 develop이 아니라 그
  브랜치에 있었기 때문이고, **정리와 그 정리를 안전하게 만드는 수정이 같은 커밋에 들어와야만 참**이었다.
  이번 머지가 정확히 그렇게 됐다(OQ-P-238 ① 해소).
  **닫힌 미결이 여섯이다** — OQ-P-003 ③(캐시 정리) · OQ-P-004 ②(마스크 null raw throw) ·
  OQ-P-055 ②(닫기 목적지) · OQ-P-087(죽은 `ResultEffect`) · OQ-P-152(토핑 생성 플로우 출구) ·
  OQ-P-238 ①. 이 중 둘은 **"고칠 도구가 없어서" 열려 있던 것**이고 도구가 이번에 생겼다:
  `Navigator.popUpTo<T>()`는 목적지 키의 인자를 모르는 호출부를 위한 되감기이고(`goToSingleClearTop`은
  키 동등성 비교라 `NavKeyCanvasMain`의 `groupId`를 알아야 한다), 그 하나로 세 Route의 닫기가 한꺼번에
  출구를 얻었다 — 세그멘테이션 쪽은 로딩·에러·본문 세 화면이 콜백 하나를 공유해 **한 자리를 채우자
  셋이 함께 열렸다.** **OQ-P-087은 반대로 "걷어내는" 쪽으로 닫혔다** — `CanvasMainRoute`의 `ResultEffect<String>`은
  기능이 죽고 크래시만 살아 있던 통로였고(카메라 취소가 흘린 `null`이 버퍼에 남아 있다가 캔버스로
  돌아오는 순간 인텐트로 들어갔다), `CustomCameraEffect.ReturnResult(uri: String?)`를 인자 없는 `Cancel`로
  좁혀 **`null`을 흘리는 자리 자체**를 없앴다.
  **부분 해소가 셋**이다. OQ-P-204는 **V1 잔여가 처음으로 줄었다**(6 → **3파일**, 전부 EntryBuilder) —
  8엔트리를 한 라운드에 옮긴 결과이고 이관 계기는 로딩·실패가 아니라 **어차피 그 파일을 다 여는
  라운드**였다(여덟 중 `isLoading`을 넘기는 곳이 하나도 없다). OQ-P-178은 ②가 닫혔다 — 배경 편집 복귀가
  `onBack()` 2회에서 `popUpTo<NavKeyCanvasBGEdit>()`가 됐고, **흐름 깊이를 가정한다는 것을 주석이
  설명하고 있었다는 것 자체가 신호**였다(PR 리뷰가 짚었다). OQ-P-155는 `feature/segmentation/impl`이
  첫 `src/test` 소스셋을 얻었지만 **판단이 몰린 순수 로직**(`UndoRedoStack`·`BitmapViewMapping`)은
  여전히 안 덮였다.
  **스펙 본문을 뒤집은 결정 둘도 그대로 develop에 들어왔다** — `decodeImage` 계약을 `Result`로 넓힌 것
  (스펙이 명시적으로 기각했던 대안인데, 이미 방어하던 호출부도 stdlib `runCatching`으로
  `CancellationException`을 삼켜 **떠난 화면이 자기를 "디코드 실패"로 보고**하던 것이 뒤집은 근거다)과,
  최근 이미지 공급자를 만든 것(스펙이 "C-106 몫"이라며 제외했으나 기록 대상이 결과물이 아니라
  **사용자가 고른 원본 uri**라 제외했던 설계와는 다르다 — 갤러리의 "최근"은 다시 고를 사진 목록이다).
  ⚠️ **열린 채로 남은 것도 분명히 한다** — 재시도 동선 부재(OQ-P-003 ①, `ModuleNotReady`는 일시적
  실패인데 다시 시도할 수단이 없다) · 다운샘플 없음(OQ-P-228, **현재 develop 기준 피크는 세그멘테이션·
  토핑 편집 둘 다 미측정이다** — 옛 실측치는 전부 다른 트리에서 잰 값이라 되살리지 않았다) ·
  `NavKeyCanvasMove` 계열 도달 불가(OQ-P-239, `goTo` 호출부 0건인데 엔트리 등록만 남았다. **이 라운드가
  만든 잔해가 아니라 #290이 남긴 것**이라 리뷰 diff를 넓히지 않기로 한 판단이 그대로다) · 카메라 캐시
  정리 경로 없음. 배치 확정이 서버로 가지 않는 것(OQ-P-238 ②③)도 그대로다 — **되돌아가는 방식만
  고쳐졌지 저장은 여전히 없다.**
  ⚠️ **실기기·실서버 확인 없음** — 크래시 셋을 닫은 라운드인데 그 크래시들을 실제로 밟아 본 적이 없다.
  직전 회차 요약: **미머지 스택이 한 번에 비었고, 계약과 앱이 처음으로 어긋난 데 없이 만났다**(delta 3건).
  세 PR(#307 → #308 → #310)이 쓰인 순서대로 들어갔고 **머지 커밋 셋 모두 충돌 해소 편집이 0건**이라
  브랜치 팁이 그대로 develop 사실이다. 그래서 이 라운드가 한 일의 대부분은 새 감사가 아니라
  **"미머지"라고 적힌 문서를 사실로 바꾸는 것**이었다 — 셋 다 선작성 스펙·플랜이 있었고 as-built 기록도
  이미 붙어 있었다(스펙 3건·플랜 3건 아카이브 이동, ADR-0023 `proposed` → `accepted`).
  **가장 큰 결과는 문서 한 줄이 아니라 표 하나가 비었다는 것이다** — [api/conventions.md](api/conventions.md)의
  "Android 불일치"가 **2건에서 0건**이 됐고 이 저장소에서 처음이다. 두 건이 닫힌 방식이 서로 다르다:
  업로드 시각 파싱은 **앱이 서버 포맷 변경을 기다리지 않고** `LocalDateTime` + 고정 KST로 읽는 쪽을
  골랐고(근거는 서버 DB 커넥션 세 환경이 `serverTimezone=Asia/Seoul`이라는 계약 사실이다 — "서버 쪽이
  자연스럽다"던 OQ-P-165 ①의 판단을 뒤집었다), 하루 경계는 반대로 **정책상 옳은 쪽이 서버라 앱을
  옮겼다**(03시). 후자는 `parfaitToday()` 한 함수를 고쳤을 뿐인데 재시도 조건·달력 활성 조건·`syncToday()`
  트리거가 저절로 따라왔다 — 값을 읽는 자리를 하나로 모아 둔 것의 값이 여기서 드러났다.
  **#307**(`refactor/#294-group-data-using-ssot`): 그룹 목록·상세가 `:data`의 인메모리 캐시 한 벌로
  모이고 세 화면(G-001·C-001·S-101)이 그것을 구독한다. 계정 정보(ADR-0022)와 같은 형태이고 **다른 점은
  영속하지 않는다는 것 하나**다([ADR-0023](adr/0023-group-in-memory-ssot.md), 이번에 `accepted`).
  화면이 조회 결과를 State에 넣을 길 자체를 없앤 것이 이 설계의 방법이다 — 갱신 함수가 `Result<Unit>`이라
  **값을 얻는 두 번째 경로가 애초에 없다**. 세션 정리를 부르는 경로는 셋이 됐고(로그아웃·강제 로그아웃·
  탈퇴 위임) 캐시 clear를 계정 정보 clear **앞에** 둔다(뒤에 두면 DataStore IO가 던질 때 그룹 캐시가
  안 지워진다). C-001의 하드코딩 그룹명도 이 캐시에서 왔다.
  **#308**(`feature/#300-sync-backend-api-250818`): 2026-08-18 서버 delta 반영. **칩 배정 주체가 서버로
  정해진 것이 이 라운드의 축**이고(그룹 안 활동 멤버 사이 유일·나가면 반납·재추첨 없음), 그래서 앱은
  `:domain`에 중립 enum `NametagChipType`을 두고 feature가 디자인시스템 타입으로 옮긴다(`:domain`은
  순수 JVM이라 `YGColorChipType`을 모른다). **`GroupDetailVO`가 삭제됐다** — 상세 응답에 `groupName`이
  실리면서 "서버 응답 하나에 대응하지 않는 유일한 그룹 VO"의 존재 이유가 사라졌고, 합성 자리를 `:data`가
  아니라 UseCase에 둔 판단이 그 소멸을 한 줄 삭제로 끝나게 했다. S-101의 "N명 남음"도 mock 1에서 실값이
  됐다(`memberLimit - members.size`).
  **#310**(`feature/#300-sync-backend-api-250819`): 2026-08-19 서버 delta 반영. 서버가 HTTP DTO 경계에서만
  키를 `nameTagChip` 계열로 바꿔 **직전 라운드가 붙인 칩 결선을 조용히 무력화한 것**을 되살렸고, 같은
  라운드가 C-001 상단 멤버 칩을 서버 값으로 결선해 `NAMETAG_CHIP_PALETTE`를 걷었다 — 팔레트 인덱스 순환이
  개념째 사라지면서 **같은 사람이 S-101과 C-001에서 같은 색**이 됐다. 머지 전 코드리뷰가 결론 하나를 더
  뒤집었다: 모르는 칩 문자열과 값 없음을 **모두 `DEFAULT`로 접어** 이 축의 널 허용을 없앴고
  ([ADR-0024](adr/0024-nametag-chip-unknown-fold.md)), 대가는 "서버가 늘린 새 타입"과 "반납된 자리"가
  앱에서 구분되지 않는 것이다(재검토 트리거를 ADR에 명시했다). 같은 리뷰가 `:data`의 칩 매퍼 두 사본을
  `source/common/mapper`의 `internal` 하나로 합쳤다 — feature 쪽 색 변환 셋을 복제로 남긴 것과 결론이
  갈리는 이유는 **가시성 미결이 여기엔 안 걸려서**다(입출력이 `:data`·`:domain` 안에서 닫혀 새 모듈
  간선이 0개다).
  **`MyParfaitGroupVOMapperTest`가 삭제돼 저장소의 `XxxVOMapperTest`가 0개가 됐다** — 규약 예외로 살아
  있던 그 파일이 한 일이 삭제 사유다. 오프셋 붙은 입력을 **스스로 지어 넣어** 파싱 버그를 초록으로 지켜
  왔고(Given 주석이 매퍼 주석과 같은 허구였다), 매퍼를 단독으로 두면 입력의 현실성을 아무도 검사하지
  않는다는 규약의 근거가 그대로 사례가 됐다. 옮길 대상이던 `ParfaitGroupRemoteDataSourceImplTest`도
  이번에 신설돼 `XxxRemoteDataSourceImplTest`가 여섯이 됐다(OQ-P-168 해소).
  ⚠️ **재발 방지 수단은 이번에도 안 생겼고, 그 사이 위험 반경이 넓어졌다** — 키 어긋남을 잡은 것은 두 번
  모두 계약 문서 감사였다(앱 테스트는 자기 DTO를 자기가 만들어 넣어 `@SerialName` 문자열을 검증하지
  않는다). 그전까지 이 키를 읽는 코드는 브랜치에만 있었으므로 사고도 브랜치에 갇혀 있었는데, 지금은
  **develop이 네 자리에서 이 키로 색을 정한다**(S-101 멤버 칩·G-001 그룹 칩·C-001 상단 칩·목록 시각) —
  다음 키 변경은 출시 가능한 develop을 조용히 폴백 색으로 만든다(OQ-P-234 ③, 최종 리뷰가 다음 라운드
  최우선으로 권한 것이 이것이다).
  ⚠️ **코드 변경 없이 화면 값이 바뀐 자리도 이 라운드에 실물이 됐다** — 서버가 `COALESCE`로 채운 값
  때문에 토핑 0건 그룹이 **생성 시각을 활동 시각처럼, 생성자 칩을 마지막 토퍼 칩처럼** 보여 준다
  (OQ-P-235는 그대로 열려 있고 사정거리만 넓어졌다).
  미결 정리: **해소 8건**(OQ-P-165·168·216·222·224·234 ①②·236 ①) · **소멸 1건**(OQ-P-210 ② — 팔레트
  개념 자체가 사라졌다) · **신설 1건**(OQ-P-243 — 하루 경계 상수 하나가 뜻이 다른 두 하루를 동시에
  정한다). 스캐폴드 이관 수치는 그대로다(8화면·V1 잔여 6파일) — 이 라운드는 UI 컨테이너를 안 건드렸다.
  테스트: 유닛 490 → **538건**(#307 +21 · #308 +21 · #310 +6), 테스트 파일 56 → 61개.
  ⚠️ **실기기·실서버 확인 없음** — 계약 정합이 처음으로 0건이 됐지만 그것을 확인한 것은 여전히 코드 대조뿐이다.
  직전 회차 요약: **되돌릴 수 없는 문 셋이 다 열렸고, 마지막 문만 닫는 방식이 다르다**(delta 1건).
  **#306**(`feature/#236-withdraw-api`): S-001 앱 설정의 회원 탈퇴가 **로그 한 줄에서 실제 요청으로**
  바뀌었다. 표면은 2026-08-15부터 있었고 화면도 팝업까지 있었으므로 이번에 들어온 것은 그 사이를
  잇는 셋뿐이다 — `MemberRepository.withdraw`·`WithdrawUseCase`·ViewModel 분기. 그래서 이 라운드가
  말하는 것은 분량이 아니라 **순서**다. `WithdrawUseCase`는 서버가 탈퇴를 받아 준 **뒤에야** 기기를
  정리하고, 거절당하면 아무것도 지우지 않는다 — 로그아웃과 정확히 반대이고(그쪽은 서버 호출이
  실패해도 로컬을 지운다), 이유는 서버가 거절했는데 로컬만 지우면 **계정이 살아 있는 채로 사용자만
  탈퇴했다고 믿기** 때문이다. 지우는 일 자체는 새로 쓰지 않고 `LogoutUseCase`에 위임했다("무엇을
  지우는가"의 단일 자리를 깨지 않으려는 선택이고, 그 UseCase의 호출자가 셋이 됐다).
  화면 쪽은 **S-101 나가기·신고가 확정한 형태를 그대로 복제**했다 — 팝업을 먼저 닫고 `YGScaffoldV2`
  로딩 오버레이가 덮으며, 실패는 공통 토스트다(OQ-P-141의 ①만 채우고 ②·③은 이번에도 불필요했다).
  갈린 것은 목적지와 잠금이다: 성공은 `replaceAll(NavKeyLogin)`이고(탈퇴는 세션 자체가 끝나 그룹
  목록으로 갈 자리가 없다), `launch(key)` 가드에 **"요청 중 다시 눌러도 API 1회"를 잠그는 테스트**가
  붙었다 — 나가기·신고에는 없던 무게다. in-flight도 `isWithdrawing`으로 따로 들었는데 `isLoggingOut`과
  합치지 않은 이유는 덮개가 아니라 **항목 비활성**이다(`YGActionItem(enabled = !isLoggingOut)`이 로그아웃
  줄 하나만 가리킨다). S-001은 이 라운드에 **토스트 호스트를 처음 얻었지만** 이미 V2를 쓰고 있어
  컨테이너는 손대지 않았고, 스캐폴드 이관 수치(8화면·V1 잔여 6파일)도 그대로다.
  ⚠️ **끝난 뒤가 깨끗하지 않다** — 위임받은 `LogoutUseCase`가 **방금 지워진 계정의 토큰으로** 서버
  로그아웃을 부른다. `logout`은 화이트리스트 밖이라 401이 `TokenAuthenticator`를 깨우고, refresh token은
  탈퇴가 서버에서 이미 지운 것이라 재발급도 거절돼 `ForcedLogout`까지 발행된다 — `MainRoute`와
  ViewModel이 **같은 목적지로 이동을 두 번** 일으키고 로그에는 "재발급 거절"이 결함처럼 남는다
  (OQ-P-242 신설). 계약 쪽 물음 하나는 **서지 않은 채로 닫혔다** — 서버가 회원 부재에도 204를 주지만
  화면이 할 일이 같아 앱은 "이미 탈퇴됨"을 성공과 구분하지 않는다(OQ-P-162 ③).
  테스트: 유닛 484 → **490건**(`WithdrawUseCaseTest` 2 · `AppSettingViewModelTest` +4).
  ⚠️ **실기기·실서버 확인 없음** — 되돌릴 수 없는 동작인데 한 번도 실제로 눌러 본 적이 없다.
  직전 회차 요약: **경로의 끝이 채워졌는데, 끝에서 아무것도 저장되지 않는다**(delta 1건).
  **#290**(`feature/topping-add-screen`): 토핑 생성 플로우의 마지막 자리채움
  (`NavKeyCanvasMove` → 아무것도 안 하는 `CanvasMoveScreen`)이 **C-106 배치 화면**으로 바뀌었다.
  목적지 `NavKeyCanvasToppingPlace(imageUri)` 한 벌이 생기고, 위키 [[C-106-토핑-배치-정책-v0.1]]의
  초기 배치 규칙 **넷이 처음으로 코드에 들어왔다** — 긴 변 = 캔버스 너비 40%·정중앙·짧은 변 48 하한·
  이탈 허용 + 클리핑. OQ-P-200이 "코드 어디에도 없다"고 적던 자리가 닫혔다. 들어온 방식이 두 가지를
  말한다: 40% 상수를 `internal`로 열어 **읽기(서버 배치를 그리는 `CanvasToppingLayer`)와 쓰기(신규
  배치)가 같은 값을 공유**하게 했고(③의 답이 "양쪽 다"로 났다), 48은 **dp로 굳었다**(②의 답, 정책은
  px — C-104 브러시 굵기가 이미 같은 방식으로 갈렸다). 계산 자리는 ViewModel이다 — 캔버스 실측과
  토핑 원본 크기가 서로 다른 시점에 오므로 화면이 인텐트 둘로 올려 주고 어느 쪽이 먼저 오든 다시
  시도하되 **사용자가 한 번 손대면 멈춘다**.
  **한계값도 고정 배율이 아니라 역산이라는 것이 이 화면의 판단**이다 — 리사이즈 하한은 48dp 최소 터치
  영역에서, 상한은 캔버스 긴 변의 1.5배에서 각각 원본 크기로 되돌려 계산한다(고정값이면 큰 원본
  사진이 "처음 크기로 못 줄고" "아무리 키워도 캔버스를 못 벗어난다"). 회전은 무제한이고, **정책이
  회전을 아예 다루지 않아** 감도·상한이 전부 코드 판단이다(OQ-P-241 신설). 그리기는 `center`와
  `sizeAfterScale`을 **한 번만 계산해 이미지·스트로크·핸들 셋에 그대로 넘긴다** — 같은 배율을 서로
  다른 modifier로 표현하면 그 둘이 실제로 같은 값을 내는지가 Compose 내부 구현에 달리기 때문이고,
  클리핑만 셋이 갈린다(스트로크는 안 잘린다).
  겸해 **세그멘테이션 결과가 두 벌이 됐다** — 수동 편집은 원본과 픽셀로 겹쳐야 해 원본 크기를
  유지하고, 배치·미리보기는 여백이 붙으면 40%·48dp 계산이 어긋나 실제 객체 크기가 필요하다.
  `trimmedSubjectImagePath`가 신설되고 `NavKeySegmentationConfirm`도 두 경로를 다 싣는다. **대가는
  캐시 파일과 메모리 버퍼가 각각 하나씩 는 것**이고, "테두리가 없으면 같은 파일을 두 번 떨구지
  않는다" 최적화가 사라져 편집 경로도 항상 두 번 저장한다(OQ-P-228 재도출 무효).
  `CanvasBGEditScreen`의 private 컴포저블 3종은 모듈 `component/`로 올라가 두 화면이 공유하고
  (`toppingId: Long` → `key: Any?`), 그 과정의 `size` → `requiredSize` 이관이 **C-301 편집 탭 토핑에도
  적용됐다**(기존 화면에 남긴 유일한 동작 변경). `YGScaffoldV2`는 Route가 소유해 **이관 8화면째**
  이지만 이건 이관이 아니라 신규 화면이 규약을 지킨 것이고, V1 잔여는 6파일 그대로다.
  ⚠️ **끝이 흐름을 닫지 못한다** — 확인 버튼은 위치·크기·각도 넷을 다 만들어 놓고 이펙트로만
  흘리고(서버 계약에 좌표·배율·회전이 없다), Route는 `goTo(NavKeyCanvasMain(groupId = 0L))`로
  이동한다. 그룹 id가 하드코딩이고 `goTo`라 흐름 화면이 백스택에 그대로 쌓인다(OQ-P-238 신설) —
  **이것이 미머지 세그멘테이션 스펙의 캐시 정리 안전 근거를 다시 거짓으로 만든다**(그 스펙은 #290
  머지를 예상하고 처방까지 적어 뒀다). 호출자를 잃은 `NavKeyCanvasMove` 계열은 도달 불가로
  남았고(OQ-P-239 신설), 배치 화면이 보여 주는 캔버스는 **흰 바탕에 자기 토핑 하나뿐**이라 겹침을
  못 보고 자리를 고른다(OQ-P-240 신설). 규약 이탈도 복사됐다 — `60.dp`·`14.dp` 리터럴과 "공통에
  없음" 주석이 그대로 옮겨졌고(OQ-P-203 ③), 드래그 핸들 접근성 공백은 공용화되면서 **두 화면**으로
  번졌다(OQ-P-202 ③).
  테스트: 유닛 474 → **484건**(`ToppingGeometryTest` 4 · `CanvasToppingPlaceViewModelTest` 6).
  ⚠️ **실기기 확인 없음.**
  직전 회차 요약: **화면 둘이 하나로 줄었고, 자리채움 둘이 실물로 바뀌었다**(delta 3건).
  **#296**(`feature/#281-policy-detail-webview`): 약관 종류마다 있던 목적지가 사라졌다.
  `NavKeyServiceTerms`·`NavKeyPrivacyPolicy` 두 `data object`와 Route/Screen/ViewModel 2벌이
  **`NavKeyWebView(title, url)` 한 벌**로 합쳐지고 **ViewModel은 아예 없어졌다** — 이 화면의 상태는
  실려 온 두 값이 전부이고 부를 API가 없어 감쌀 것이 없다. 합칠 수 있게 된 이유는 **두 화면을 가르던
  제목·주소가 이제 서버 값**이기 때문이다(`GET /api/v1/policies`). 앞선 라운드들이 "같은 화면을
  재사용하려고 인자를 붙인" 것이라면 이번은 **인자가 생겨서 화면이 줄어든** 반대 방향이고,
  그래서 출처 인자도 두지 않았다(설정·온보딩 어느 쪽에서 왔든 그릴 것이 같다). 동시에 온보딩 약관
  화면의 마지막 stub(`/* navigate to url */`)이 닫혔고 — caret을 탭하면 실제로 전문이 열린다 —
  설정 화면은 진입 시 약관 목록을 받아 두는 **두 번째 소비처**가 됐다(policy 도메인은 이미 `done`).
  엔트리에서 머티리얼 `Scaffold`를 직접 부르던 규약 이탈도 이때 `YGScaffoldV2`로 메워졌다.
  ⚠️ **다만 실패가 조용하다** — 설정에서 목록 조회가 실패하면 줄은 그대로 있는데 **눌러도 아무 일이
  없다**(로그만, OQ-P-231). 온보딩 쪽은 재시도 문구가 있어 같은 API의 실패 표현이 화면마다 갈린다.
  그리고 계약 공백이 처음으로 화면까지 내려왔다 — 서버 `url`이 URL 전용 컬럼이 아니라 본문 컬럼
  재사용이라(OQ-P-068) 전문이 오면 웹뷰가 로드에 실패한다. 목적지가 임의 주소를 받는 범용 화면이 된
  것에 출처 검증도 없다(OQ-P-232 신설).
  **#295**(`feature/#282-app-setting-version-info`): `"1.0v"` 리터럴이던 버전이 실값이 됐다.
  `versionName`이 애플리케이션 모듈 속성이라 라이브러리에서 읽을 수 없어, `core:util:android`가
  **`:app`과 같은 카탈로그 항목을 자기 `BuildConfig`에 다시 심고** `APP_VERSION_NAME` 상수로 낸다
  (저장소에서 `buildConfig`를 켜는 첫 라이브러리 모듈이다). 출처가 하나라 지금은 안 갈리지만
  접미사·플레이버가 생기면 설정 화면만 옛 값이 된다(OQ-P-233 신설). 표시의 `v`는 포맷 리소스가
  붙이고 상태에는 숫자만 둔다. 겸해 **`YGListItem`이 두 오버로드의 줄 높이를 스스로 맞췄다** —
  화면이 먼저 맞췄다가 컴포넌트로 내려간 것이고, 리터럴이 아니라 아이콘 프리셋 높이를 계산해 깐다.
  **#305**(`feature/#254-group-list-refresh-lottie`): 로딩 인디케이터가 **디자인 로띠로 확정**됐다.
  `YGLoadingLottie`(+`YGLoadingTone`)가 신설되고 `YGLoadingOverlay`의 `CircularProgressIndicator`가
  교체되며 Dim이 `Black25` → `Black75`로 짙어졌다 — **교체가 한 파일에서 끝난 것은 스펙이 그 파일을
  나눠 둔 이유가 그대로 맞았다는 뜻**이다(OQ-P-205 ① 절반 해소). 색을 화면 테마가 아니라 **얹히는
  바탕**으로 고르게 한 것이 이 컴포넌트의 결정이고, 그래서 Dim 위는 `Light`·흰 목록은 `Dark`다.
  G-001 당겨서 새로고침은 머티리얼 기본 인디케이터 컨테이너를 버리고(디자인에 없는 흰 원이 하나 더
  생긴다) 콘텐츠가 비켜 준 틈에 로띠를 직접 놓으며, 당기는 동안은 **손가락을 progress로 넘겨** 시계를
  하나만 돌린다. 스플래시도 로띠를 얻었는데 여기서 **진입 규칙이 바뀌었다** — 부트스트랩 응답과 재생
  종료가 **둘 다** 끝나야 이동하고, 순서가 정해져 있지 않아 나중에 끝난 쪽이 이동을 일으킨다.
  화면은 여전히 목적지를 모르고 "내 애니메이션이 끝났다"만 올리며, **파싱 실패도 '끝'으로 넘긴다**
  (그 신호가 없으면 스플래시를 벗어날 방법이 없다). ⚠️ 그러나 이것은
  [user-info-ssot 스펙](superpowers/specs/archive/2026-08-15-user-info-ssot.md)이 "최소 노출 시간 요구가 없다"며
  `SplashInitialUseCase`를 지운 자리에 **다른 이유로 최소 노출이 돌아온 것**이고, 상한이 없다
  (OQ-P-229 신설). 로띠 호출 관용구도 갈렸다 — 스플래시는 공용 표면을 안 쓰고 `LottieAnimation`을
  직접 부르며 모듈이 의존을 따로 문다(OQ-P-230 신설).
  스캐폴드 이관은 **7화면**이 됐지만(스플래시·약관 웹뷰 추가) **V1 잔여는 6파일 그대로**다 —
  `feature/intro/impl` EntryBuilder에 약관 동의 엔트리가 남는다. 이번 둘은 로딩·실패를 채우려 옮긴
  것이 아니라 **어차피 그 파일을 여는 라운드**여서 옮긴 것이라, OQ-P-204 ①의 답이 한 번 더 넓어졌다.
  테스트: 유닛 467 → **474건**(`AppSettingViewModelTest`·`SplashViewModelTest`·`TermAgreeViewModelTest` 보강).
  ⚠️ **실기기·실서버 확인 없음.**
  ⚠️ **문서 전제 오류 1건 정정**: [design-system](architecture/design-system.md)이
  `refactor/segmentation-logic`의 8엔트리 일괄 이관을 **"develop 기준 13화면·V1 잔여 3파일"**로 적고
  있었으나 그 브랜치는 **로컬에만 있고 develop에 없다**(리모트에도 없다). 수치를 브랜치 기준으로
  표기하고 develop 값(6파일)을 병기했다. 아래 "미머지 제외 항목"에도 추가한다.
  직전 회차 요약: **화면이 앞에 설 때마다 다시 묻기 시작했고, 그러자 실패 규칙이 뒤집혔다**(delta 1건).
  **#297**(`feature/#288-group-list-refresh`): G-001 목록·C-001 캔버스에 **`Enter` 인텐트**가 생기고
  Route의 `LifecycleResumeEffect`가 그것을 보낸다. `init` 조회는 화면 수명이 아니라 **ViewModel 수명**에
  걸린 것이라, 백스택 아래 엔트리가 컴포지션에서 빠져도 살아남아 돌아온 화면이 낡은 값을 그대로 보여
  주던 것이 닫혔다(**OQ-P-169**). 고른 답이 `ON_RESUME` 관측인 이유는 **재조회가 필요한 진짜 이유가
  복귀가 아니라 남이 바꾸기 때문**이라는 것이다 — 그래서 복귀 관용구(`goToSingleClearTop`)는 손대지
  않았고, 백스택 리셋 관용구 결정(OQ-P-136)과도 떨어졌다.
  **뒤집힌 것은 실패 규칙이다** — 재진입마다 조회가 나가게 되자 "목록이 남아 있어도 전면 에러 화면"이
  **뒤로 온 것만으로 보던 목록이 사라진다**가 됐다. 그래서 보여 줄 목록이 있으면 화면을 유지하고,
  **사용자가 직접 당긴 새로고침 실패만** 토스트로 알린다(`ShowRefreshError`) — 목록이 그대로인 것은
  "새 소식이 없다"와 구분되지 않기 때문이고, 목록이 비면 종전대로 에러 화면이라 토스트를 겹치지 않는다.
  토스트 호스트가 필요해 G-001 Route가 `YGScaffold` → **`YGScaffoldV2`**로 옮겨졌다 — **API 결선이 아닌
  라운드가 이관을 끌어온 첫 사례**이고(V1 잔여 7 → **6파일**), 이관을 부르는 것은 결선이 아니라
  **채울 것이 생기는 시점**임을 보였다(OQ-P-204 ①).
  C-001은 `syncToday()` 뒤 **오늘을 보고 있을 때만** 오늘 캔버스와 **올해** 달력 기록을 다시 받는다
  (지난 날은 마감돼 안 바뀌고, 연도 목록은 해가 바뀔 때만 늘어 `init`에 남는다). 부작용 있는
  `/parfaits/today`를 재진입마다 부르지만 첫 진입에서 만들어진 것을 받을 뿐이고, 반대로 **ViewModel이
  만들어지는 것만으로는 캔버스가 생기지 않게** 됐다. `syncToday()`는 화면을 열어 둔 채 자정을 넘긴
  경우를 맡는다 — 오늘을 보던 중이면 두 캔버스를 비우고 새 날로 옮기고(어제 것 위에 토핑을 얹는 일을
  막는다), 지난 날을 보던 중이면 `today`만 고친다.
  ⚠️ **남은 것은 규약 공백과 한쪽만 생긴 표면이다** — 재진입 재조회는 규약이 아니라 두 화면의 관용구고
  (key도 `Unit`/`viewModel`로 갈렸다), C-001은 조회 실패가 여전히 로그뿐인데 **실패할 기회만 늘었다**
  (OQ-P-221 신설). G-001도 재진입 조회 실패는 조용하다. `loadParfaitHistories` KDoc은 아직 "연 단위로
  한 번만"이라 동작과 어긋난다. 자정 경계는 처리 자리가 둘로 늘었지만 **둘 다 KST 자정**이라 03:00은
  그대로 미적용이다(OQ-P-127).
  테스트: 유닛 456 → **467건**(`CanvasMainViewModelTest` 5 신설 — `feature/groups/canvas/impl`에
  `parfait.test.unit`이 처음 붙었다, `GroupListViewModelTest` 8 → 14).
  ⚠️ **실기기·실서버 확인 없음.**
  직전 회차 요약: **그룹 설정이 mock을 다 버렸고, 나가는 문도 열렸다**(delta 2건, 둘 다 결선).
  **#285**(`feature/#275-group-setting-api`): S-101이 상세 조회로 채워진다. `ParfaitGroupRepository`에
  `getGroupDetail`이 올라오고 mock 기본값 5종이 사라졌으며, **진입도 이때 열렸다** —
  `NavKeyGroupSetting`이 `data class(groupId)`가 되고 C-001 상단 메뉴가 호출자가 돼 **4일간의 도달
  불가가 닫혔다**(OQ-P-138). 화면 컨테이너도 `YGScaffold`(엔트리) → **`YGScaffoldV2`(Route)** 로
  옮겨져 **API 결선 라운드에 스캐폴드 이관이 딸려 온 첫 사례**가 됐다(V1 잔여 8파일 → 7파일).
  핵심은 **상세 조회가 요청 둘이라는 것**이다 — 계약에 그룹명이 없어 `GetGroupDetailUseCase`가
  `getMyGroups()`를 한 번 더 부르고 이름만 집어 붙이며, **이름 조회 실패는 실패로 치지 않는다**
  (제목 한 줄 때문에 멤버·초대코드까지 못 보여 주는 편이 나쁘다). `isMe`는 닉네임이 아니라
  **`memberId`** 로 판별한다 — 서버가 그룹 내 닉네임 중복을 허용한 뒤라 이름으로 찾으면 남을 나로
  표시한다.
  **#287**(`feature/#277-group-leave-report-api`): Danger Zone의 확인 버튼이 **실제로 파괴적이 됐다.**
  `leaveGroup`·`reportGroup`이 Repository에 올라와 **DataSource 8함수 전량이 도메인에 열렸고**,
  이로써 parfait-group 도메인이 **`android_status: done`**이 됐다(8 엔드포인트 전부 호출부 보유).
  OQ-P-141이 요구한 셋 중 **①만 채우고 나머지 둘은 필요 없게 만들었다** — in-flight 필드를 신설하되
  "닫고 나서 요청" 순서는 **뒤집지 않고** 스캐폴드 오버레이가 덮게 했고(팝업을 띄운 채 두면 그
  덮개 아래 가려진다), 그래서 `YGModalPopup` 좌우 플래그 분리도 손대지 않았다. 나가기와 신고는
  결과가 같아(서버가 신고를 같은 트랜잭션에서 탈퇴로 잇는다) 한 함수에 모였고, 성공하면
  **`replaceAll(NavKeyGroupList)`** 다 — 백스택이 전부 방금 떠난 그룹의 것이라 되돌아가면 403뿐이다.
  ⚠️ **남은 것은 계약 공백과 임시 상수다** — `remainingCount`는 정원이 생성 응답에만 있어 여전히
  mock 1이고(나머지가 다 실데이터라 **이제야 그럴듯하게 틀린 값**으로 보인다), 컬러칩은 목록 인덱스
  순환이라 멤버가 들고 나면 남은 사람 색이 바뀌며, **신고 사유는 하드코딩 상수 하나**라 모든 신고가
  같은 문자열로 저장된다. 실패 문구도 3갈래뿐이라 **403/404가 일시 장애와 같은 문구**다.
  **회원 탈퇴(S-003)는 그대로 stub** — 되돌릴 수 없는 확인 셋 중 둘만 동작한다.
  테스트: 유닛 436 → **456건**(VM 24→35, `GetGroupDetailUseCaseTest` 5 신설, Repository 11→15).
  ⚠️ **실기기·실서버 확인 없음** — 화면이 도달 가능해져 육안 확인 항목 17건이 막혀 있지는 않다.
  직전 회차 요약: **선반영해 둔 문서 둘이 코드로 확정됐다 — 그 회차는 대조가 전부였다**(delta 2건, 둘 다 리팩터).
  기능 변화 0건이고 문서 콘텐츠도 이미 [2026-08-17 선반영 커밋 2건](index.md)이 반영해 둔 상태라,
  이번 라운드가 한 일은 **선반영이 머지본과 어긋나지 않았는지 확인**하는 것이었다. **어긋난 곳 0건.**
  **#291**(`refactor/#278-canvas-main`): C-001 화면 계열이 `CanvasImageAdd*` → **`CanvasMain*`**로 개명됐다
  (`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`, `strings.xml` 키
  `canvas_main_*`). diff를 이름 치환 후 대조하면 **짝이 안 맞는 라인이 0줄**이라 시그니처·동작 불변이
  기계적으로 확인된다. develop에 `CanvasImageAdd` 잔존 참조 0건.
  **#292**(`refactor/#284-clickable-to-clickable-yg`): 프로덕션 Foundation `Modifier.clickable` **28곳이 전량
  `clickableYGNoRipple`로** 옮겨졌다. develop에 남은 `clickable`은 `YGClickable.kt` 내부 구현 1곳과
  `androidTest` 픽스처 2건(`YGLoadingOverlayTest`·`YGThemeSmokeTest`)뿐 — 문서가 적은 "픽스처 2건뿐"이
  그대로 맞다. `clickableYGNoRipple`에 `interactionSource: MutableInteractionSource? = null`이 첫 파라미터로
  붙어 다른 네 변형과 자리가 같아졌고, 이로써 **300ms 스로틀이 디자인시스템을 넘어 feature 화면 클릭
  전반에** 걸린다. 사용처 0으로 남아 있던 API가 프로젝트 표준 클릭 유틸이 된 것이라 존치 판단이 결과로
  갚아졌다(OQ-P-077). 미결 3건 해소는 선반영 때 기록된 그대로다.
  ⚠️ **직전 회차가 남긴 것은 하나도 안 닫혔다** — 상세 조회 `launch(key)` 가드가 뒤집은 "마지막 선택이
  이긴다", 연도 캐시 `orEmpty()`가 뭉개는 빈 해/안 받은 해, 로그 한 줄뿐인 갤러리 저장, 토핑 배치·좌표
  저장 경로 부재가 그대로다. 테스트 건수도 **436건 불변**(리팩터라 신규 0). ⚠️ **실기기·실서버 확인 없음.**
  직전 회차 요약: **달력이 mock을 버렸고, 그 대가로 지난 날이 진짜 열렸다**(delta 1건).
  **#279**: 하루 전 라운드가 "같은 ViewModel 안에서 캔버스 조회는 계약을 타고 달력 조회는 mock을
  만든다"로 남긴 자리가 닫혔다(OQ-P-183). `ParfaitRepository`에 **`getYears`가 올라와 다섯 갈래 중
  넷**이 열렸고(남은 하나는 배경 변경), 두 UseCase에서 mock 생성 로직이 통째로 사라졌다.
  **`ParfaitHistory`는 삭제됐다** — 달력이 계약 VO `PastCanvasVO`를 그대로 쓰면서 "점을 찍는 기준"이
  응답 필드 `imageCount` → `toppingCount`가 됐고, `domain/model/parfait/` 패키지가 소멸해 하위 패키지가
  열에서 **아홉**으로 줄었다. 조회 경로도 갈렸다 — `GetCanvasByDateUseCase`(목록→상세 2단)가 하루 만에
  **삭제**되고 `GetParfaitDetailUseCase`가 들어왔다. 달력이 이미 그 해 목록을 캐시로 들고 있어
  **앞 단이 UseCase에서 화면으로 옮겨 간 것**이고, 날짜로 캔버스를 찾는 엔드포인트가 없다는 계약 사실은
  그대로다. 상태가 **캔버스 두 개**(`todayCanvas`·`viewedCanvas`)로 갈린 이유는 서버가 마감 캔버스의
  편집을 막지 않기 때문이다 — 토핑 추가·배경 편집이 언제나 오늘 것을 향하게 하려는 것이고, 부수 효과로
  **오늘로 돌아갈 때 부작용 있는 `today` 재조회가 필요 없다**(받아 둔 것을 갈아 끼운다). 화면에서도
  지난 캔버스의 메뉴 액션 둘이 **갤러리에 저장·오늘의 파르페 가기**로 바뀌어 편집 진입점 자체가
  사라졌다(OQ-P-189 ②에 앱 쪽 첫 답 — 다만 **가드가 아니라 길 치우기**라 다른 경로가 생기면 다시
  뚫린다). 달력 셀은 `date <= today`에서 **기록 있는 날 + 오늘**로 좁아졌다 — 누를 수는 있는데 열
  캔버스가 없는 날을 없앤 것이지만, 위키 정책의 Disabled 정의("미래 날짜")를 **과거로 넘어섰다**.
  **그런데 이번 라운드는 직전 라운드의 결정 하나를 근거 없이 뒤집었다** — 상세 조회에 `launch(key)`
  가드가 붙어 **앞선 조회가 이긴다**. 이전 날 그림을 비우지 않게 된 것과 겹쳐, 날짜를 빠르게 두 번
  고르면 **머리말은 B인데 토핑은 A**인 상태가 남는다(#268은 정확히 이 이유로 가드를 안 걸었다).
  연도별 캐시(`parfaitHistoriesByYear`)는 "빈 해"와 "안 받은 해"를 가르려고 `Map`인데 정작 화면이 읽는
  파생이 `orEmpty()`로 **둘을 다시 뭉갠다**. 갤러리 저장은 **버튼만 있고 핸들러는 로그 한 줄**이다.
  테스트: 유닛 434 → **436건**(`GetCanvasByDateUseCaseTest` 6 삭제, 신규 둘 4+4 · `GetParfaitDetailUseCase`는
  무테스트). ⚠️ **실기기·실서버 확인 없음.**
  직전 회차 요약: **캔버스가 열렸고, 열자마자 읽기만 된다는 것이 보였다**(delta 1건).
  **#268**: C-001이 mock을 버리고 **서버 캔버스**를 그린다 — parfait 도메인 첫 Repository
  (`ParfaitRepository`, 다섯 갈래 중 **오늘·목록·상세 셋만** 노출) → UseCase 둘 → 화면. 핵심은
  **같은 캔버스를 얻는 두 경로를 용도로 갈랐다**는 것이다: 진입은 `/parfaits/today`(조회인데 행을
  만드는 부작용을 감수한다 — 토핑을 얹으려면 `parfaitId`가 있어야 하고 부작용 없는 경로는 없는 날을
  만들어 주지 않는다, 그래서 `launch(key)`로 1회만)이고, 달력에서 고른 날은 **목록→상세 2단**
  (훑는 것만으로 빈 캔버스가 쌓이면 안 된다). 그 대가로 **없는 날은 실패가 아니라 `null`**이고
  상세 실패만 실패로 남는다. `today` 응답의 날짜는 앱이 검증해 **자정 경계 재시도 1회**를 하는데,
  그 "오늘"이 기기 시간대면 해외 기기에서 재시도가 **로드마다** 돌기 때문에 `PARFAIT_TIME_ZONE`(KST)이
  생겼다 — **03:00 경계는 여전히 미적용**이고 시간대만 맞았다. 렌더에서는 **계약이 말하지 않는 단위를
  앱이 정했다**: 좌표는 Canvas-Area 대비 **0~1 정규화 중심점**(절대 px이면 기기마다 배치가 어긋난다),
  `scale` 1.0은 **긴 변이 너비 40%**(위키 C-106을 끌어다 썼다), `borderWidth`는 **화면 dp**. 누끼라
  사각 테두리를 못 둘러 같은 그림을 색으로 물들여 **8방향으로 밀어 찍고** 원본을 덮는다(= `SOLID`
  토핑 하나가 이미지 9장, 미측정). **진입이 열린 것도 이번**이다 — `NavKeyCanvasImageAdd`가
  `data class(groupId)`가 되며 6일간의 도달 불가가 닫혔고(호출자 없는 화면은 자기가 무엇을 인자로
  받아야 하는지도 모른 채 머지된다), G-001 토핑에 첫 클릭 경로가 `clickableYGScaleRipple`로 붙었다.
  **그런데 열자마자 드러난 것은 읽기 전용이라는 사실이다** — 배치 확정·좌표 수정 소비처가 0건이라
  화면은 남이 올린 토핑을 보기만 하고, 조회 실패도 로그만 남아 **빈 캔버스와 실패가 같은 화면**이다.
  같은 ViewModel 안에서 **캔버스 조회는 계약을 타고 달력 조회는 여전히 mock**이며(막고 있던 이유
  둘 — Repository 부재·`groupId` 부재 — 은 이번에 다 사라졌다), C-301 편집 탭도 mock이라 **한 앱에서
  토핑의 출처가 둘**이다. 그룹명·칩 색도 서버가 안 줘서 mock으로 남았다. 테스트: 유닛 417 → **434건**
  (신규 3파일 16 + 그룹 목록 1). ⚠️ **실기기·실서버 확인 없음.**
  직전 회차 요약: **로딩과 실패에 공통 자리가 생겼다 — 다만 세 화면만 그 자리에 들어갔다**(delta 1건).
  **#267**(추적하던 미머지 스펙·플랜 한 쌍): 화면마다 손으로 짓던 로딩·실패 표현이 `YGScaffoldV2`
  **한 컴포저블의 세 층**(content → 로딩 오버레이 → 토스트 호스트)으로 모였다. 신규 파라미터는
  `isLoading`·`toastPolicy` 둘뿐이고 **전부 기본값**인데, 이건 편의가 아니라 V1에 붙인
  `@Deprecated(ReplaceWith)`의 치환 코드가 컴파일되게 하는 계약이다. 핵심은 **이관이 이름 교체가
  아니라 소유 위치 이동**이라는 점 — `hiltViewModel()`이 Route 안에 있어 `EntryBuilder`는 `isLoading`도
  실패 이펙트도 볼 수 없으므로, 스캐폴드가 nav에서 Route로 내려와야 비로소 채울 것이 생긴다. 그래서
  `ERROR` 승급 기준도 "V1 호출처 0"이 아니라 **"각 화면이 Route에서 스캐폴드를 소유하고 배선했는가"**로
  정정됐다(IDE 일괄 치환은 호출처만 0으로 만들고 어느 화면도 로딩·에러를 얻지 못한다). 구현이 설계에
  더한 것 중 큰 것은 **접근성**이다 — 오버레이의 `pointerInput` 소비로는 부족했다. **TalkBack 더블탭은
  포인터 이벤트가 아니라 `SemanticsActions.OnClick`을 직접 부르기 때문**에 로딩 중에도 뒤 버튼이
  눌렸고, 스캐폴드가 `content`를 `semantics { hideFromAccessibility() }`로 감싸 막는다(그 기전은
  `assertDoesNotExist`로 못 잠근다 — 플랫폼 트리에만 작용해서 `SemanticsMatcher`로 속성 보유만 단언한다).
  실패 문구는 **호출부 소유**로 확정돼 A-002가 첫 사례를 만들었다 — `LoginError` 4갈래(로그는 여전히
  8갈래, 사용자에겐 502·503·SDK 실패가 다 "잠시 후 다시"다)를 `ShowError`가 **문구가 아니라 사유로**
  실어 보내고, Route가 컴포지션에서 문구를 미리 뽑아 둔다(이펙트 수집은 코루틴이라 `stringResource`를
  못 부르고, `LocalContext.getString` 우회는 로케일 변경 때 안 갱신된다). **그러나 이관은 3화면
  (A-002 로그인 · S-003 앱 설정 · S-002 계정 정보)에서 멈췄고 V1이 8파일 22곳에 살아 있다** — 두 스캐폴드가
  공존하고 삭제 시점은 미정이다. 로딩 오버레이 자체도 **디자인 미확정 임시 구현**이고, 켜는 기준
  ("네트워크 왕복인가")은 규칙이 아니라 세 사례에서 귀납한 것이다(S-002는 처음엔 안 걸었다가 정정했다).
  테스트: 유닛 415 → **417건**(`LoginViewModelTest` 9 → 11), 계측 5 → **12건**(신규 7건). `core:designsystem`에
  **모듈 최초의 `strings.xml`**이 생겼다(오버레이 접근성 문구). ⚠️ **실기기 계측은 통과했으나 이관 3화면의
  실사용 확인은 없다.**
  직전 회차 요약: **편집 모드의 비어 있던 절반이 채워졌다 — 다만 고치는 대상이 아직 mock이다**(delta 1건).
  **#264**: C-301 토핑 탭이 상태만 바꾸던 자리에서 **놓인 토핑을 고르고 옮기고 키우고 돌리고 지우는
  화면**이 됐다. 구성은 **딤 한 장을 사이에 낀 4층 스택** — 남의 토핑을 아래, 딤, 내 토핑, 선택
  오버레이 순으로 그려 "내 것만 밝다"를 레이어 순서로 표현하고, 남의 토핑 탭은 딤이 먼저 받아
  선택 해제가 된다. 조작 셋 중 크기조절이 까다로운데, 핸들이 우상단 고정이라 **드래그 벡터를 회전된
  바깥 방향 단위벡터에 투영**해 증감을 낸다(거꾸로 선 토핑도 바깥으로 끌면 커진다). 선택 표시는
  토핑과 함께 돌면 아이콘이 뒤집히므로 스트로크·버튼 좌표를 `ToppingGeometry`가 따로 계산해
  **버튼은 위치만 따라가고 돌지 않는다.** 편집 버튼은 `NavKeyToppingEdit(borderOnly = true)`로 기존
  C-104/C-105 화면을 **탭 없이**(`YGFloatingBarEdit` 첫 실화면 소비) 열어, 위키 표의 C-306이 새
  목적지 없이 성립했다 — 결과의 알맹이 경로로 `segmentationImageUri`를 갈아 끼워 재편집이 누적된다.
  확장 2종(`Modifier.centeredAt`·`dragBy`)은 `core:util:android`로 승격, 아이콘 2종(`ic_edit`·
  `ic_scale`) 신설, release 빌드 타입에 release 서명 결선이 동승했다. **그러나 고치는 대상이
  `loadMockToppings()`가 만든 템플릿 이미지 4개·하드코딩 좌표이고, 이동·크기·회전·삭제 결과는 전부
  `UiState` 안에서 끝난다** — 서버에는 상세 조회·테두리 수정·삭제 표면이 이미 있는데 소비처가 0건이고,
  좌표·배율·회전을 저장할 계약 자체가 없다. C-106 배치 규격(40%·정중앙·48px)도 코드에 없다(신규 배치
  경로가 없어 아직 어긋나지는 않는다). 테스트 변경 0건. ⚠️ **실기기 확인 없음.**
  직전 회차 요약: **"참여하기"가 약속하는 시점으로 참여가 되돌아왔다**(delta 1건). **#261**: 하루 전까지 A-004
  초대코드 화면이 쥐고 있던 **참여 확인 모달이 통째로 S-102 닉네임 화면으로 내려갔다** — 모달의
  "참여하기"가 `POST join` → `PATCH nickname`을 연달아 부르므로, 앞 화면은 미리보기(`GET join-preview`)까지만
  하고 넘기는 것도 참여 결과(`groupId`)가 아니라 **참여 재료(초대코드·그룹명)**다. 그래서 닉네임 화면에서
  이탈하면 **참여 자체가 없다** — #244가 만든 "닉네임 없는 참여"(OQ-P-166)가 닫혔고, 확인 모달 문구와
  코드의 시점이 어긋나던 OQ-P-137 ③, dismiss 가드 비대칭 ④도 함께 닫혔다. `GroupNickNameError`는
  닉네임 400 갈래를 버리고 **참여 실패 3종**(404 초대코드·409 이미참여·409 정원)으로 바뀌어 A-004의
  `invite_code_error_*` 문구를 재사용한다 — 미리보기를 통과한 뒤에야 도달하므로 **미리보기와 참여 사이
  그룹 상태가 바뀐 경우**만 남기 때문이다. 반대로 **닉네임 적용 실패는 참여를 되돌리지 않고 표시도 없다**
  (로그 + 안내 토스트 `TODO`) — 두 요청이 원자적이지 않다는 사실은 그대로다. 테스트는 총량 415건 불변
  (초대코드 17 → 14, 닉네임 6 → 9로 케이스가 화면을 따라 이동). ⚠️ **실기기 확인 없음.**
  직전 회차 요약: **계정 정보가 화면의 소유물이길 그만두고, 앱이 자기 세션을 기억하기 시작했다**(delta 1건).
  **#263**(추적하던 미머지 스펙·플랜 한 쌍): `users/me`가 주는 계정 정보가 **암호화 로컬 SSoT 한 벌**로
  모이고 S-001·S-002는 mock 문자열을 버리고 `Flow`를 구독만 한다 — 서버 조회는 **로그인·가입 직후 /
  앱 진입 / 닉네임 변경 성공** 세 시점뿐이다. 스플래시가 `BootstrapSessionUseCase`로 갈림길이 돼
  **자동로그인이 성립**했고(`SplashInitialUseCase`는 삭제), 그 판정은 도메인 타입(`SessionBootstrap`)으로
  나와 화면이 "토큰이 있나"를 모른다. 실패 목적지는 하나(`ToLogin`)이고 **갈리는 것은 정리 범위뿐**
  — 세션을 파기하는 것은 서버가 자격증명을 거절했을 때(401·`MEMBER_NOT_FOUND`)뿐이라 5xx나 로컬
  저장 실패로는 로그아웃되지 않는다. 정리 자체는 `LogoutUseCase`에 위임해 **"무엇을 지우는가"가
  한 자리**다. **as-built 5건**: `ObserveMyAccountUseCase` → **`GetMyAccountFlowUseCase`** 개명, 암호화
  DataStore 접근이 **`EncryptedPreferences` 프록시**로 뽑히며 `EncryptedTokenStore`까지 옮겨 타고
  **읽기 IO 실패는 더는 저장분을 폐기하지 않는다**(ADR-0019 as-built 정정), 프록시가 **암호문 상태에서
  중복 방출을 끊어** 토큰 재발급 저장이 편집 중 입력 필드를 흔들던 경로를 막고, 닉네임 변경 시 로컬이
  비어 있으면 **재조회 폴백**, 부트스트랩 정리 위임. member 도메인은 **첫 소비처**를 얻어 Repository가
  0건이던 도메인이 셋으로 줄었고(탈퇴만 미소비), 테스트는 358 → **415건**(파일 40 → 47)이다.
  ⚠️ **수동 확인 7항목 미수행** — 자동로그인 왕복·첫 프레임·계정 전환·오프라인 진입 전부 실기기로 본
  적이 없다. **G-001 목록 닉네임만 mock으로 남았다.** 직전 회차 요약: **배경이 쓰기 경로를 얻은 다음 날
  앱 표면도 붙었다**(delta 1건). **#266**: 2026-08-16 서버 delta(`22717fe`)가 더한 엔드포인트 둘 —
  캔버스 상세 조회 `GET .../parfaits/{parfaitId}`와 배경 변경 `PATCH .../parfaits/{parfaitId}/background` —
  의 `:data` 표면이 들어와 **Android 표면이 27/27, 공백 0**이 됐다. 상세 조회는 서버가 오늘 조회와
  **같은 응답 클래스**를 재사용해 DTO·VO·매퍼를 그대로 쓰고, 그 대가로 `TodayCanvasVO`가 **`CanvasVO`로
  개명**됐다. 배경 변경은 그 도메인 **첫 쓰기 경로·첫 요청 DTO**이고 쓰기 전용 sealed
  `CanvasBackgroundEdit`로 서버의 조건부 필수를 컴파일에서 막는다. **소비처는 여전히 0건**이고 C-301
  배경 편집은 계속 고른 값을 버린다.
- **검증일**: 2026-09-21 (83회차)

  📌 **같은 날 두 번째 회차다** — 82회차(`e10ead2ca`)와 날짜가 같고, 그 회차가 「원격 브랜치가 일곱으로 늘었다」며 이름만 적어 둔 `feature/ai/llm-wiki-document`가 몇 시간 뒤 develop에 들어왔다. 번호는 요약 안의 「직전 회차 요약(82회차)」 포인터와 이 줄 + 1이 같다.

  📌 **이 줄이 또 한 회차치 낡아 있었다**(81회차 `924cb5802`). **같은 종류의 누락이 일곱 번째**다 —
  그 회차의 이력 표 비고는 "검증일 줄 81회차"라고 적었는데 이 줄은 `2026-09-16 (80회차)` 에
  멈춰 있었다. **비고에 적는 것과 이 줄을 고치는 것은 다른 일이다.** 번호는 요약 안의
  「직전 회차 요약(81회차)」 포인터와 이 줄 + 1 이 같다.

  📌 **닷새 만의 회차다** — 79·78회차가 둘 다 2026-09-11 이었다. 번호는 요약 안의 「직전 회차 요약(78회차)」
  포인터와 이 줄 + 1 이 같다.

  📌 **이 줄이 또 한 회차치 낡아 있었다**(77회차 `95b7fc4d5`). **같은 종류의 누락이 여섯 번째**다.
  번호는 요약 안의 「직전 회차 요약(76회차)」 포인터에서 77 → 78 로 이었다.

  📌 **이 줄이 또 낡아 있었다 — 이번엔 한 회차치다**(75회차 `efa77150`). **같은 종류의 누락이
  다섯 번째**다. 번호의 근거는 이력 표가 아니라 **요약 안의 「직전 회차 요약(N회차)」 사슬**이다 —
  74회차 포인터가 살아 있어 75 → 76 으로 이었다.

  📌 **이 줄이 또 세 회차 동안 낡아 있었다** — 71~73회차(`2285d09d`·`23675cc1`·`b7674e88`)가 기준선
  해시와 이력 표는 갱신하면서 이 줄만 건너뛰어 `2026-09-06 (70회차)` 에 멈춰 있었다. **같은 종류의
  누락이 네 번째**다. 이번 번호의 근거는 이력 표가 아니라 **요약 안의 「직전 회차 요약(N회차)」 사슬**
  이다 — 72회차 포인터가 살아 있어 73 → 74 로 이었다.

  📌 **이 줄이 여섯 회차 동안 낡아 있었다** — 58~63회차(`27e85d0d`·`afde8c4c`·`6a1da1b0`·`fa46e5cf`·
  `0173e454`·`40e1fca6`)가 기준선 해시와 이력 표는 갱신하면서 이 줄만 건너뛰어 `2026-08-28 (57회차)`
  에 멈춰 있었다. **같은 종류의 누락이 세 번째**다. 직전 회차의 이 줄이 낡았을 때 번호를 되찾는
  수단은 이력 표를 세는 것뿐인데, 표는 한 회차에 여러 행이 붙은 적이 있어 **정확한 복구를 보장하지
  않는다** — 그러니 이 줄은 4번 단계에서 함께 고친다.

  📌 **하루에 두 회차가 돌았다** — 56회차도 2026-08-28 이다. 회차 번호의 근거는 여전히
  **직전 회차의 이 줄 + 1**이다.

  📌 **하루에 세 회차가 돌았다** — 53~55회차가 전부 2026-08-27 이다. 회차 번호의 근거는 여전히
  **직전 회차의 이 줄 + 1**이다.

  📌 **하루에 다섯 회차가 돌았다** — 50~53회차가 전부 2026-08-26이다. 날짜만으로는 구분되지 않으니
  회차 번호의 근거는 **직전 회차의 이 줄 + 1**이다.

  📌 **직전 회차에 두 회차치가 낡아 있었다** — 50회차(`df3f4cbe`, #368)와 51회차(`cbb48cd8`, #366)가
  기준선 해시와 이력 표는 갱신하면서 이 줄만 건너뛰어 `2026-08-25 (49회차)`에 멈춰 있었다.
  같은 종류의 누락이 **두 번째**였으므로, 절차의 4번(기준선 갱신)에 이 줄이 포함된다는 것을 여기
  적어 둔다.

  📌 **48회차와 같은 날이다** — 하루에 두 회차가 돈 것이라 날짜만으로는 구분되지 않는다. 회차
  번호의 근거는 여전히 **직전 회차의 이 줄 + 1**이다.

  📌 **이 줄 자체가 다섯 회차 동안 낡아 있었다** — 43~46회차(`8eb2af7d`·`96dc215c`·`f31b8c30`·
  `d634efd3`)가 기준선 해시와 이력 표는 갱신하면서 이 두 줄만 건너뛰어 `2026-08-22 (42회차)`로
  멈춰 있었다. 이번에 맞췄다. 회차 번호의 근거는 이력 표가 아니라(표는 한 회차에 여러 행이 붙은
  적이 있다) **직전 회차의 이 줄 + 1**이다.
- **미머지 추적 항목**: **하나**(`feature/debug-mode`, OQ-P-311 계보).
  📌 **83회차 재확인(2026-09-21)** — `origin/develop`에 `DebugMode*` 심볼이 여전히 0건이다.
  **원격 브랜치는 일곱에서 다섯으로 줄었다** — `feature/ai/llm-wiki`와 `feature/ai/llm-wiki-document` 둘이 머지 뒤 지워졌다. 남은 다섯 중 parfait 문서가 걸린 것은 이 줄의 하나뿐이다. `origin/release/*`는 계속 0개이고, 경량 태그 `1.1.0`~`1.1.3` 넷은 전부 `origin/develop`의 조상이다(develop 밖에 남은 태그는 `1.0.0` 하나).
  📌 **82회차 재확인(2026-09-21)** — `origin/develop` 에 `DebugMode*` 심볼이 여전히 0건이다.
  ⚠️ **원격 브랜치가 둘에서 일곱으로 늘었다** — `chore/bump-version-1.1.4-12` 와
  `feature/ai/llm-wiki`·`feature/ai/llm-wiki-document`·`feature/ai/set-up` 넷이 새로 올라왔다.
  **넷 다 parfait 문서가 걸리지 않아 이 줄에 오르지 않는다** — 이 줄은 여전히 **parfait 문서가
  걸린 브랜치 중 회차를 넘겨 남는 것**만 센다. `origin/release/*` 는 계속 0개다.
  📌 **79회차 재확인(2026-09-11)** — `origin/develop` 에 `DebugMode*` 심볼이 여전히 0건이다(develop 이
  350커밋, 브랜치가 6커밋 앞선다). 이번 delta 의 `feature/sync-backend-api-260911` 은 선작성 스펙·계획이 없어
  이 줄에 오른 적이 없다. ⚠️ **원격 `release/*` 브랜치가 0개가 되어**, 이 브랜치의 커밋을 품은 다른 ref 는
  경량 태그 `0.1.0`·`0.1.1`·`1.0.0` 셋뿐이다(`git ls-remote` · `merge-base --is-ancestor` 로 확인).
  📌 **78회차 재확인(2026-09-11)** — `origin/develop` 에 `DebugMode*` 심볼이 여전히 0건이다(develop 이
  346커밋, 브랜치가 6커밋 앞선다). 직전 회차의 선작성 문서가 "미머지 브랜치"로 적어 둔
  `bugfix/permission-not-required` 는 이 줄에 오르지 않은 채 같은 날 #489로 들어왔다. 이 줄은 여전히
  **parfait 문서가 걸린 브랜치 중 회차를 넘겨 남는 것**만 센다.
  📌 **76회차 재확인(2026-09-10)** — `origin/develop` 에 `DebugMode*` 심볼이 여전히 0건이라
  [로그인 디버그 모드 스펙](superpowers/specs/2026-08-28-login-debug-mode.md)과
  [그 계획](superpowers/plans/2026-08-28-login-debug-mode.md)은 `draft` 로 남는다. 이번 회차가 확인한
  **release 계보 수렴은 최신 브랜치에 대해서만 참이고 이 브랜치는 그 밖이다.**
  📌 **70회차 재확인(2026-09-06)** — 이번 delta 넷은 전부 `origin/develop` 안이고, 69회차가 세어 둔
  `feature/debug-mode` 하나가 그대로 남았다. `origin/release/*` 넷은 릴리스 계보라 이 셈에 넣지 않는다
  (이번 회차의 `release/version-1.1.0-7`은 #458로 develop에 들어왔다).
  📌 **65회차에 일곱으로 늘었다(2026-09-04)** — 아래 다섯에 `feature/push-notification-permission`
  (도메인 계약 → `NotificationRepository` 결선 → `onNewToken` 등록 → 권한 허용 직후 등록 → 그룹
  생성·참여 직후 권한 안내, OQ-P-341 ②③④) 과 `feature/#420-canvas-tutorial`(C-001 최초 진입
  튜토리얼 오버레이, 표시 여부를 사용자 설정에 저장) 이 더해졌다. 푸시 계열이 이제 셋이고
  (`push-fcm-service` · `push-notification-deeplink` · `push-notification-permission`) 셋이 합쳐져야
  OQ-P-341 이 실제로 닫힌다 — 머지 회차에 **세 브랜치의 머지 순서부터** 확인한다.
  📌 **64회차에 다섯으로 늘었다(2026-09-03)** — `feature/debug-mode`(OQ-P-311 계보) ·
  `feature/image-loading-placeholder`(OQ-P-346·348) 에 더해 `feature/#423-canvas-save-preview`
  (캔버스 저장을 미리보기 화면으로 돌리고 스토리 비율로 잡는다) · `feature/push-fcm-service`
  (FCM 수신 서비스·알림 채널·`onNewToken` 복귀) · `feature/push-notification-deeplink`
  (푸시 딥링크 버스, 앞 브랜치와 커밋 둘을 공유한다) 셋이 같은 날 올라왔다. 푸시 둘은 OQ-P-341 에,
  저장 미리보기는 OQ-P-330 계열(캔버스 저장)에 닿는다 — 머지 회차에 그 미결부터 본다.
  📌 **57회차 재확인(2026-08-28)** — 이번 delta 다섯(#393·#394·#395·#396·#397)은 전부
  `origin/develop` 안이라 이 줄에 더할 것이 없다.
  📌 **56회차 재확인(2026-08-28)** — 이번 delta 셋(#369·#398·#400)은 전부 `origin/develop` 안이라
  이 줄에 더할 것이 없다. release 계보와의 격차는 이 회차가 다시 세지 않았다 — OQ-P-311 ①②는
  55회차 기록 그대로 열려 있다.
  📌 **55회차에 넷이 다 들어왔다(2026-08-27, PR #363)** — 네 브랜치의 내용이 정련 브랜치 하나로
  접혀 `develop` 에 머지됐다. 그래서 세 회차 연속 세어 온 이 줄이 이번에 비었고, 그 넷에 걸려
  있던 선작성 스펙 셋·계획 셋도 아카이브로 갔다.
  ⚠️ **그렇다고 두 계보가 만난 것은 아니다** — develop 이 받은 것은 release 가 받은 그 커밋들이
  아니라 **rebase 된 다른 커밋들**이라 `origin/release/version-0.0.3-3` 이 develop 보다 앞선 폭은
  **43커밋 그대로**다. 게다가 rebase 중에 커널 전체가 `suspend` 로 바뀌었으므로 **같은 기능이
  양쪽에 있다는 말이 이 넷에는 성립하지 않는다**(배포된 `0.0.3` 은 콜백 방식 커널이다).
  반대 방향은 develop 이 release 보다 **85커밋** 앞선다 → OQ-P-311 ①② 그대로 열려 있다.
  📌 **54회차 재확인(2026-08-27)** — 네 브랜치 다 여전히 `origin/develop`의 조상이 아니고,
  release 브랜치가 develop보다 앞선 폭도 **43커밋 그대로**다. 반대 방향은 이번 라운드만큼
  벌어져 develop이 release보다 41커밋 앞선다. 아래는 53회차의 기록이다.
  여전히 `origin/develop`에는 없고 `origin/release/version-0.0.3-3`
  에만 머지돼 있으며, 그 브랜치가 태그 `0.0.3`으로 나갔다. 즉 "아직 아무 데도 안 들어간 브랜치"가
  아니라 **"develop을 건너뛴 브랜치"**다 → OQ-P-311. 다음 회차의 첫 결정은 이 넷을 develop으로
  되돌릴지, 아니면 감사 대상을 바꿀지다. 아래 표의 성격 열은 그대로 유효하다.
  📌 **release 브랜치가 develop보다 43커밋 앞선다**(직전 회차 45에서 줄었다) — 줄어든 둘은
  `feature/toast-position-fix`이고 이번 #371로 develop에도 들어와 **양쪽에 다 있다.** 한 브랜치가
  두 계보에 각각 머지될 수 있다는 뜻이고, 그러면 "어느 트리가 배포됐나"는 더 흐려진다 —
  같은 변경이 양쪽에 있어도 **주변 커밋이 달라 결과 트리가 같다는 보장은 없다.**

  | 브랜치 | 대응 문서 | 성격 |
  |---|---|---|
  | `feature/segmentation-candidate-coverage` | [마스크 후처리 계획](superpowers/plans/archive/2026-08-24-segmentation-mask-postprocessing.md) 1단계 | 후보 면적 판정을 사각형에서 커버리지로 |
  | `feature/segmentation-alpha-kernel` | 위 계획 2단계 | 잡티 제거·keep 마스크·침식 커널 |
  | `feature/segmentation-postprocess-wiring` | 위 계획 3단계 | 두 경로를 커널에 태우는 결선(커널 위에 쌓였다) |
  | `feature/segmentation-alpha-refinement` | [알파 정련 계획](superpowers/plans/archive/2026-08-25-segmentation-alpha-refinement.md) | 가이드 필터 정련(결선 브랜치 위에 쌓였다) |

  ⚠️ **PR 순서가 계약인 스택이다** — 후처리 계획이 "1단계를 먼저 넣지 않고 커널을 배선하면 지금
  되던 사진이 실패로 바뀐다"를 명시한다. 다음 회차가 이 넷을 볼 때는 **머지된 순서**를 먼저 확인한다.
  📌 release 브랜치가 받아들인 순서는 `candidate-coverage` → `alpha-kernel` → `postprocess-wiring` →
  `alpha-refinement`로 **계획이 요구한 순서 그대로**다(2026-08-26 확인).

  📌 **선작성 스펙·계획 넷의 `status`는 이번에 안 건드렸다** — 아카이브 판정 기준이 "`develop` 머지"라
  release 머지는 그 기준을 충족하지 않는다. 기준 자체를 바꿀지가 OQ-P-311 ③이다.

  📌 **미착수 계획은 여전히 미머지 항목이 아니다** — [전처리 계획](superpowers/plans/2026-08-23-segmentation-preprocessing.md)은
  이번에 1단계가 머지됐지만 **2단계 이후(Task 5~14)는 대응 브랜치가 없다.** 스파이크와 사진 세트
  측정이 사람 손을 필요로 하고, 4단계는 그 판정을 통과한 것만 한다.

  ⚠️ **"추적 항목"이 "미머지 브랜치 전부"가 아니라는 단서는 그대로다.** `origin`에는 develop보다
  앞선 브랜치가 이 넷 말고도 여럿 남아 있다. 이 줄이 세는 것은 **parfait 문서가 걸려 있는 브랜치**뿐이다.

  **이번 회차가 확인한 것 하나** — **스택 PR은 머지 순서가 계획과 달라질 수 있고, 그 사실은
  `--merges` 목록만 봐서는 안 보인다.** 이번 셋 중 하나(#390)는 `develop`이 아니라 다른 PR의
  브랜치로 들어왔는데, 머지 커밋은 `develop`에서 도달 가능하므로 목록에는 나란히 셋이 뜬다.
  갈래를 가르는 것은 **첫 부모**다 — `git log --merges --first-parent`로 한 번 더 세면 develop
  선에 실제로 붙은 것이 둘이라는 사실이 바로 드러난다. 그러지 않으면 같은 변경을 두 번 세거나,
  `show --stat`이 보여 주는 결합 diff를 각 PR의 몫으로 오해한다(이번에도 #389의 stat이 #390의
  파일까지 함께 들고 있었다). 그러므로 스택 PR 라운드에서는 **첫 부모 선과 전체 머지 목록을 함께
  보고**, 어느 것이 어느 것을 품는지 적는다.

  직전 회차가 확인한 것: **문서가 "달라지지 않았다"고 단정한 자리는 그 자체로 감사
  대상이다.** 이 회차가 고친 것은 코드 드리프트가 아니라 **문서가 여섯 날 전에 스스로 적은 판정**
  둘이다("보이는 위치는 사실상 그대로다", "위키 공통 정책에는 이쪽이 맞는다"). 둘 다 이관 라운드가
  **부수 효과를 안 재고 지나가며** 적은 문장이고, 틀린 것을 드러낸 것은 다음 감사가 아니라
  **그 화면을 쓴 사람**이었다. 그러므로 이관·교체 라운드를 적을 때 "보이는 것은 그대로"라고 쓰려면
  근거를 함께 적고, 근거가 "이미 상단 정렬이니까" 같은 **어휘의 일치**뿐이면 단정하지 말고
  미확인으로 남긴다 — 이번 경우 두 "상단"은 서로 다른 상자였다.

  그 앞 회차가 확인한 것: **"미머지"는 "안 나갔다"가 아니다.** 이 문서는 브랜치가
  `origin/develop`에 들어왔는지만 보고 그렇지 않으면 "미머지 추적 항목"으로 세어 왔는데, 그 셈이
  암묵적으로 깔고 있던 전제는 **develop이 배포로 가는 유일한 길**이라는 것이었다. 이번에 그 전제가
  깨졌다 — 넷은 release 브랜치로 나갔고 태그까지 붙었다. 그러므로 다음부터 미머지 항목을 셀 때는
  `origin/develop`만이 아니라 **`git branch -r --contains`로 다른 계보를 함께 확인**하고, 브랜치가
  살아 있다는 것과 **어디에도 안 들어갔다**는 것을 구분해 적는다. 실마리는 이번처럼 사소한 데서
  나온다 — 결번 하나(`versionCode` 2)가 그 계보의 존재를 말해 줬다.

  직전 회차가 확인한 것: **바이너리만 바뀐 라운드는 이 감사 체계가 가장 약한 자리다.**
  `git show --stat`이 `Bin 589892 -> 599540 bytes`라고만 말하고, 코드 심볼이 하나도 안 움직여
  `related_code` 대조도 걸리지 않는다. 그런 라운드에서 문서가 붙잡을 수 있는 근거는 **커밋 메시지와
  함께 들어온 텍스트 파일**뿐이다(이번엔 `OFL.txt`의 고지 문단이 그 역할을 했다). 그러므로 delta에
  바이너리 교체가 있으면 ① 무엇이 바뀌었는지가 **저장소 안 어딘가에 글로 남아 있는지** 확인하고
  ② 없으면 그 자체를 미결로 등록한다 — 남아 있어도 **되돌아가는 것을 잡을 수단이 있는지**는
  따로 묻는다(이번엔 없어서 OQ-P-307이 됐다).

  직전 회차가 확인한 것: **해소는 문서가 정해 둔 순서를 지키지 않고도 성립할 수 있다.**
  OQ-P-076은 "①을 먼저, ②를 나중"을 두 번이나 못 박았고 순서를 뒤집으면 앱이 끊긴다고 적었는데,
  코드는 뒤집었고 끊기지 않았다. 문서가 틀린 것은 **결론이 아니라 선택지의 개수**였다 — ①·② 둘만
  놓고 순서를 다투는 동안 `debug-overrides`라는 셋째 자리가 시야 밖에 있었다. 그러므로 미결 항목이
  예상과 다른 방식으로 닫혔을 때 점검이 할 일은 **순서를 우기는 것이 아니라 무엇이 위험을
  흡수했는지 적는 것**이고, 그 흡수 장치가 문서의 권고보다 넓거나 좁으면 그 차이를 함께 적는다
  (이번엔 넓었다 — 도메인 한정이 아니라 디버그 빌드 전체다).

  직전 회차가 확인한 것: **스펙이 통째로 들어오지 않는 라운드가 있다.** 근거 등급으로
  항목을 가른 스펙은 **부분 머지가 정상 경로**이고, 그때 점검이 하는 일은 아카이브 이동이 아니라
  ① 스펙 `status`를 `in-progress`로 내리고 ② 본문에서 **현재 코드를 단정하던 문장 중 이번에 고쳐진
  자리만** 표시하고 ③ 계획의 해당 단계 체크박스를 닫는 것이다. 남은 조건부 항목의 서술은 **근거로서
  그대로 살아 있어야** 다음 사람이 판정을 이어받는다.

## 점검 절차 (다음 요청 시)
로컬 경로는 개인정보라 `wiki/personal-private/project-paths.md` 참고(아래 `<TJYG-Android>`).

1. **최신화**: `git -C <TJYG-Android> fetch origin develop`
2. **신규 머지 나열**: `git -C <TJYG-Android> log --oneline --merges <기준선>..origin/develop`
   - 각 머지 PR/브랜치가 어떤 컴포넌트·모듈을 건드렸는지 확인:
     `git -C <TJYG-Android> show --stat <merge-hash>`
3. **문서 대조**: 변경된 심볼(컴포넌트/토큰/시그니처)이 parfait 문서와 어긋나는지 검사.
   - 관련 spec/plan `status`·`related_code`, `architecture/*` 인벤토리, `synthesis/open-questions.md` "미머지" 항목.
   - 드리프트 발견 → 문서 수정. 구현 완료분(develop 머지) spec→`implemented`·`specs/archive/`, plan→`done`·`plans/archive/`.
4. **기준선 갱신**: 위 "현재 기준선"을 새 `origin/develop` HEAD로 교체하고 아래 이력에 한 줄 추가.
5. **미머지 항목 재확인**: `git -C <TJYG-Android> ls-tree -r --name-only origin/develop | grep <심볼>` 로 존재 여부 확정.

> 드리프트는 대개 **문서 검증일 이후 머지된 PR**에서 발생(예: #140 fix/ygbutton). merge 날짜와 문서 `verified` 날짜를 비교하면 후보를 빨리 좁힐 수 있다.

## 기준선 이력
| 검증일 | develop 커밋 | 요약 | 비고 |
|--------|-------------|------|------|
| 2026-09-21 | `143cda87b` | Merge #511(LLM 위키 체계를 TJYG-Android 저장소로 이식 — `wiki/` 스크립트·테스트·`raw/`·`pages/`·graphify 산출물, 루트 `CLAUDE.md` 「위키」 절) | delta 1건, **158파일 27237/0 — 삭제 0줄**, 커밋 67개, **머지 트리 = 브랜치 팁**(충돌 해소 편집 0건). **`.kt`·`.kts`·gradle·xml·toml 변경 0건 — Android 소스가 한 줄도 안 움직인 첫 라운드**라 유닛 **1298건**·계측 **46건**은 셈할 것도 없이 유지. 아카이브 이동 **0건**, **parfait 문서 드리프트 0건**(spec·plan·architecture·ADR·`api/` 다섯 표면 전부 불변), 미결 **신설 1건**(OQ-P-405, `oq-next` 405 → 406). **① 같은 정책 위키가 두 벌이 됐다** — 이 저장소의 `wiki/`와 새로 들어온 TJYG-Android `wiki/`가 같은 원본을 각각 ingest했다. 소스 39건은 파일명까지 같고(NFC/NFD 차이뿐) **개념 층이 갈라졌다**(이쪽 concepts 19건 한글 세분 / 저쪽 8건 영문 묶음, synthesis도 `open-questions`+lint 아홉 대 `spec-version-history` 하나). **어느 쪽이 정본인지 정한 문서가 없다**(OQ-P-405). **② 이식 설계가 스스로 적은 전제가 안 지켜졌다** — 「콘텐츠는 옮기지 않는다, 빈 스캐폴드로 시작해 새로 ingest한다」고 결정 표에 못 박았는데 머지된 트리에는 같은 원본 39건이 그대로 있다. **③ 코드 0건 라운드에도 감사할 것이 남는다** — `--stat`에 `.kt`가 없으면 심볼 대조는 통과하지만, 이번 delta가 바꾼 것은 **문서가 서 있는 땅**이다. Gradle·CI 미변경. 검증일 줄 83회차(같은 날 두 번째 회차). 미머지 하나(`feature/debug-mode`) 유지 — 원격 브랜치는 일곱에서 다섯으로 줄었다 |
| 2026-09-21 | `e10ead2ca` | Merge #514(이슈 #502 코드 정리 2차 — 도달 불가 캔버스 화면 셋 삭제 · 사용처 0 심볼 청소 · 안 쓰는 파라미터 제거) · #513(A-004 초대 코드 `YGScaffoldV2` 이관) | delta 2건, **40파일 124/695 — 순감 571줄**, 커밋 7개, **머지 둘 다 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 **1298건**·계측 **46건** 둘 다 유지(청소 라운드 두 번 연속). 아카이브 이동 **0건**(선작성 스펙·계획 없음), 미결 **해소 2건**(OQ-P-053·**OQ-P-239**) · **부분 해소 7건**(052·089·101·123·156·204·260) · 마커 **3건**(129·215·259) · **신설 1건**(OQ-P-404, `oq-next` 404 → 405). **① 직전 회차가 "사람이 따로 결정해야 지워진다"고 적은 잔해를 다음 라운드가 지웠다** — `NavKeyCanvasMove` 계열에 더해 `NavKeyCanvasEdit`·`NavKeyCanvasImageSelect` 계열까지. 딸려 `NavTransition.Fade` 예외·`LocalSharedTransitionScope`·두 루트의 `SharedTransitionLayout`·analytics ID 셋이 걷혔다. **② 삭제가 결정을 대신했다** — OQ-P-101 ①·123 ②·215·260 ③ 이 판정 없이 닫혔고, 대가로 G-001 정책 대조표의 토핑 당김 행이 **불일치로 굳었다**. **③ 지운 자리가 새 사용처 0을 만들었다** — `NavTransition.Fade` 프리셋(OQ-P-404 신설). **④ `YGScaffold`(V1) 호출 0건** — #513 이 마지막 호출부(A-004)를 옮겨 OQ-P-204 ① 이 닫혔다(V2 Route 23파일). `api/` 다섯 표면 전부 불변(원격 연동 코드는 테스트 타입 인자 정리뿐). ADR 불변. 검증일 줄 82회차(81회차치 누락 복구, 일곱 번째). 미머지 하나(`feature/debug-mode`) 유지 — ⚠️ 원격 브랜치는 둘에서 일곱으로 늘었으나 넷 다 parfait 문서 밖이다 |
| 2026-09-17 | `924cb5802` | Merge #504(이슈 #502 코드 정리 1차 — ktx Bitmap 확장 · `delay(Duration)` · Compose 규약 · 미사용 리소스 삭제) | delta 1건, **56파일 179/208 — 순감 29줄**, 커밋 12개, **머지 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 **1298건**·계측 **46건** 둘 다 유지 — `.kt` 를 40개 가까이 만지고도 테스트 수가 안 움직인 첫 라운드이고 그것이 순수 정리의 증거다. 아카이브 이동 **0건**(청소 티켓이라 선작성 스펙·계획이 없다), 미결 **신설 1건**(OQ-P-403, `oq-next` 403 → 404), 마커 **2건**. **① ktx 전환에 빠뜨린 자리 0건** — 남은 `Bitmap.createBitmap` 일곱은 전부 ktx 대응이 없는 오버로드(행렬·`IntArray`·부분 비트맵)다. **② 청소 라운드가 와도 잔해는 안 지워졌다** — OQ-P-239 의 `NavKeyCanvasMove` 계열 셋은 엔트리 등록이 참조라 미사용 심볼로 안 잡힌다(⚠️ 추가). **③ `Navigator` 만 Kotlin 명시적 backing field 로 갈아타** 상태 노출 관용구가 두 갈래가 됐다 — `BaseViewModel._state` 등 옛 쌍 넷이 남았고 정한 문서가 없다(OQ-P-403 신설, Kotlin `2.4.10`·컴파일러 인자 없음). **④ 되살아날 뻔한 것을 막았다** — 미구현 Task 10 스니펫이 `Bitmap.createScaledBitmap` 을 적고 있어 `scale` 로 정정. 고아 드로어블 `splash_icon*.xml` 삭제 확인 → OQ-P-187 📌(출처의 `app/` 경로를 `core/ui/` 로 정정, 항목 ①②③ 잔존). `api/` 다섯 표면 전부 불변(원격 연동 코드 0건, `TokenAuthenticatorTest` 만 테스트 정리). architecture·ADR 불변. 검증일 줄 81회차. 미머지 하나(`feature/debug-mode`, `ed8e1ec8a`) 유지 |
| 2026-09-16 | `f37a76540` | Merge #497(G-001 목록 토핑 테두리 렌더 + 띠 판 캐시 선반화) · #499(로컬 빌드 캐시 측정 하니스) | delta 2건, **16파일 1649/29**, 커밋 24개, **머지 둘 다 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 1292 → **1298건**, 계측 39 → **46건**. 아카이브 이동 **4건**(스펙 2 · 계획 2), 미결 **신설 1건**(OQ-P-402 — 리모트 캐시 도입 문턱값·커밋 쌍·미실행 측정). #497은 선작성 스펙과 **어긋난 조항 0건**, #499는 구현이 스펙보다 두 자리 넓어(`check-relocatability.sh`·`report.py`/`report.html`) 스펙 본문을 머지본 기준으로 고쳤다 |
| 2026-09-11 | `1b21725ba` | Merge #496(그룹 목록 응답 테두리 세 필드 수용 + 테두리 변환 공용 매퍼화) | delta 1건, **14파일 151/28**, 커밋 3개, **머지 트리 = 브랜치 팁 `307241337`**(충돌 해소 편집 0건). 유닛 1288 → **1292건**(+4: `ParfaitGroupRemoteDataSourceImplTest`), 계측 **39건** 유지. 아카이브 이동 **0건**(선작성 스펙·계획 없음), 미결 **신설 0건**(`oq-next` 402 유지). 서버 `82e6edc` 의 `recentImageBorderType`·`Color`·`Width` 를 `MyParfaitGroupVO.recentImageBorder` 가 받고, 캔버스·토핑 매퍼의 `private` 사본 둘이 `data/source/common/mapper/ToppingBorderMapper.kt#toToppingBorder` 로 모였다(그룹 매퍼가 세 번째 호출부 → 두께 클램프도 목록까지). **로컬 커밋 단계에 먼저 적은 문서(`09207d4`·`73a0661`)는 머지 코드와 맞았고 틀린 문장은 하나** — OQ-P-316 의 "세 매퍼에 똑같이 있던"(사본은 둘). 조치: parfait-group 「미결」 테두리 항목·OQ-P-316 로컬 표기 → PR #496 · conventions `borderWidth` 행 · parfait-image 매핑 📌 · api/README 📌 · module-structure·data-layer 두 번째 공용 매퍼. `CheckNameValidUseCase` 는 KDoc 한 줄만 걷혀 문서 불변. 렌더는 별도 티켓이라 design-system·OQ-P-316 열림 유지, `api/` `android_status`·엔드포인트 표·README 도메인 표 불변. **원격 `release/*` 0개** — 78회차가 적은 `release/version-1.1.3-10` 은 그 문서 커밋 전(00:19 KST)에 이미 삭제됐고 `-11` 도 없다, 태그 `1.1.3` 은 한 번 지워졌다가 `c37dc2b4c` → 코드 10 ref 0개(OQ-P-310 📌), 옛 여섯은 2026-09-10 삭제 · `feature/debug-mode` 는 태그 `0.1.0`·`0.1.1`·`1.0.0` 에만(OQ-P-311 📌). 검증일 줄 79회차(누락 없음). 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-11 | `c37dc2b4c` | Merge #488(버전 1.1.3 코드 10) · #489(카메라·갤러리 진입 시 권한 요청 + CameraX 바인딩 권한 대기) · #490(코드 11) | delta 3건, **8파일 133/3**, **머지 셋 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 1282 → **1288건**(+6: 카메라 3 · 갤러리 3), 계측 **39건** 유지. 아카이브 이동 **0건**, 미결 **신설 0건**(`oq-next` 402 유지). **선작성 as-built 절(문서 PR #406)이 머지 코드와 어긋난 자리 0건** — 조치는 미머지 표기 걷기(c101·c102 스펙, specs/README 두 행, OQ-P-053)와 두 스펙 `verified` 갱신. `:feature:camera:impl` 첫 유닛 테스트 소스셋 → module-structure 📌. **1.1.3 이 코드 10·11 두 벌** — #490 은 이름을 두고 코드만 올린 첫 커밋, `release/version-1.1.3-10` 은 권한 수정 전 트리, 경량 태그 `1.1.3` 은 11 만 가리킨다 → ADR-0003·adr/README 버전 as-built, OQ-P-310·311 📌. 다시 뗀 이유는 PR 본문에 없다. `api/` 불변(원격 연동 코드 0건). 검증일 줄 한 회차치 누락 복구(76 → 78회차). 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-10 | `95b7fc4d5` | Merge #487(세그멘테이션 재시도 회복 + 실패 화면 「직접 편집」) | delta 1건, **29파일 2109/458**, 커밋 15개, **머지 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 1229 → **1282건**(+53: 스펙 52 + 「직접 편집」 1), 계측 **39건** 유지. **선작성 스펙 1·계획 1 아카이브 이동**(계획 체크박스 43개 전부 미체크 — 진행의 정본은 `git log`). 미결 **신설 3건**(OQ-P-399~401, `oq-next` 399 → 402), 📌 다섯(OQ-P-150·153·278·282·344). **본문은 맞았다** — 스펙 API 절 선언과 모듈별 테스트 수(133·534·75)가 develop 과 같다(문서 저장소 PR #404 가 브랜치 단계에서 as-built 로 먼저 고쳤다). **어긋난 자리는 문서 둘레였다**: ① 스펙 `related_code` 가 옮기기 전 이름 다섯(develop 0건) ② 마지막 커밋 `3217e62f7` 의 「직접 편집」 갈래(세그멘테이션 Route 가 `TOPPING_EDIT_RESULT_KEY` 를 받는다)가 navigation-flow 에 없음 ③ OQ-P-153 ④·OQ-P-344 ①이 옛 버튼을 우회로로 적음 ④ segmentation-preprocessing 이 512 확대를 "미착수"로만 적음. **새로 드러난 것은 판정 수단이다** — 조건부 세 항목의 철회 조건인 단계 로그가 Kermit `platformLogWriter` 하나라 logcat 밖으로 나가지 않고, 회복 경로 강제 수단도 없다(OQ-P-399). 실기기 확인 0회(회복·1차 회귀, OQ-P-400), 「직접 편집」 이름의 디자인 근거 없음(OQ-P-401). 조치: 아카이브 2 · README 2 · c103-error-use-original 🔁 배너 · navigation-flow · data-layer 링크 · ADR-0012 As-built 절 · segmentation-preprocessing 📌 · open-questions. `api/` 는 원격 연동 코드 0건이라 불변. 원격 `release/*` 브랜치 없음, 태그는 `1.1.2` 까지(로컬 `release/version-1.1.3-10` 은 미푸시라 범위 밖). 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-10 | `69bbbe68` | Merge #483(버전 1.1.2 코드 9) | delta 1건, **1파일 2/2**, `.kt` **0건**, **머지 트리 = 브랜치 팁**. 유닛 **1229건**·계측 **39건** 그대로, 아카이브 이동 **0건**, 미결 **신설 0건**(`oq-next` 399 유지). `appVersionCode` 8 → **9** · `appVersionName` 1.1.1 → **1.1.2**(프리뷰 두 값 불변). **이 회차의 발견은 버전이 아니라 계보다** — `origin/release/version-1.1.2-9` 가 `origin/develop` HEAD **그 커밋**이고(양방향 0커밋) 직전 `1.1.1-8` 도 그랬다. **OQ-P-311 ①(검증한 트리 ≠ 배포된 트리)이 최신 계보에서는 성립하지 않는다** — 지금 관행은 develop 에서 릴리즈 브랜치를 떼고 **버전 커밋을 develop 이 PR 로 되받는** 것이라 delta 가 곧 릴리즈 브랜치의 내용이다. ⚠️ 그 관행은 어디에도 규칙으로 안 적혔고 `feature/debug-mode` 는 여전히 release 쪽에만 있다(②). 경량 태그 `1.1.2` 신설 — **경량인 채로 가리키는 곳만 develop 커밋이 됐다**(OQ-P-310 ② 불변: 다음 올림을 강제하는 것이 없다). **버전 축을 지키는 테스트는 0건** — `DeviceInfoTest` 의 `1.1.1` 은 `buildDeviceInfo` 인자로 직접 넣는 픽스처라 카탈로그와 무관하다(그래서 안 깨졌고, 같은 이유로 잘못 오른 값도 못 잡는다). 조치: ADR-0003 📌 · adr/README 0003 행 · OQ-P-311 📌 1덩이 · doc-baseline·index. `api/` 는 원격 연동 코드 0건이라 불변. 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-10 | `efa77150` | Merge #477(지난 캔버스 알럿) · #478(화면 진입 계측) · #479(토핑 초안 UseCase 분리) · #480(토핑 업로드 원본 기준 축소) · #482(적응형 폴링 + 화면 수정 다섯) | delta 5건, **78파일 2573/305**, **머지 다섯 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 1164 → **1229건**(+65), 계측 37 → **39건**. **선작성 스펙 4·계획 4 아카이브 이동**(계획 체크박스는 실행 세션이 남기지 않아 거의 전부 미체크 — 진행의 정본은 `git log`), **사후 스펙 2건 신규 작성**(past-canvas-alert · canvas-feedback-fixes). 미결 **신설 6건**(OQ-P-392~397, `oq-next` 392 → 398)·**해소 1건**(OQ-P-389). **#477**: 새 상태를 만들지 않고 `CanvasVO.lastClosedDate` 의 변화만 본다. 처음 확인은 기준선만 세우고, `markSeen` 은 띄우기로 확정된 뒤에만 부르며(실패까지 「봤다」로 남기면 그 마감을 영영 못 본다), 인원 수는 **그 마감 당시 참여자**다. ⚠️ 정책 소스 없음(OQ-P-392). **#478**: 앱 최초의 Analytics 소비처. `simpleName` 을 안 쓰는 이유는 release R8 이 **운영 집계에서만** 이름을 뭉개서이고, 판정 기준은 최상단 키가 아니라 **크기와의 짝**이며, 트래커가 `@Singleton` 이면 **그 실행의 A-001 이 통째로 빠진다**. **#479**: 위임 UseCase 넷 + 판정 하나. **위임이라는 사실 자체가 결정**(규칙 없는 자리에 규칙을 지어내지 않는다). ⚠️ 컴파일러가 검사하지 않는다. **#480**: 하한 256 을 **입력이 아니라 결과**에 건다 — 입력에 걸면 641 이 204 로 올라가 더 큰 알맹이가 더 작게 올라간다. #479 와 같은 자리를 밟았고 순서가 **예고대로** 풀렸다. **#482**: 적응형 주기(실패는 램프 불변) + 화면 수정 다섯 — 로딩 덮개 최소 노출이 **`YGScaffoldV2` 의 계약**이 되고, 첫 페인트 뒤 덮개 억제 판정이 **두 번 무너졌다가 잡혔다**. ⚠️ 그 판정에 자기 테스트가 없다(OQ-P-395). **이 회차의 발견은 절차 쪽** — OQ-P-389 가 고쳐지자마자 같은 모양으로 재발했다(OQ-P-396). `api/` 는 원격 연동 코드 0건이라 계약 절 불변, `notification.md` Android 매핑에만 갈래 추가. 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-09 | `acbc4b45` | Merge #472(토핑 편집 빈 알맹이 차단) · #473(업로드 이미지 다운스케일) | delta 2건, **21파일 941/68**, **머지 둘 다 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 1139 → **1164건**(+25), 계측 **37건** 유지. **선작성 스펙 2·계획 2 아카이브 이동**(자동 Task 전량 수행, 실기기 수동 검증 Task 만 미체크). 미결 **신설 5건**(OQ-P-387~391, `oq-next` 386 → 392). **#472**: 하한 상수·판정이 `data` 의 `internal` 에서 `domain` 의 `SubjectCoverage` 로 올라가 자동 후보 필터·저장소 알파 정제·편집 화면이 같은 함수를 본다. `trimTransparentBounds` 가 `measureSubject`(경계 + 알파 합, 순수 함수)와 `trimTo(SubjectMeasure)` 로 갈렸고 차단은 파일을 쓰기 전이다. `borderOnly` 진입은 판정에서 빠졌다 — **그 예외는 구현이 먼저 드러내고 스펙이 뒤따라 적었다**. **#473**: `upload` 이 발급 직전 `UploadImagePreprocessor` 를 부르고 파일·포맷을 쌍으로 받는다. 판정(`UploadImagePlan.of`, 순수)과 실행(`UploadImagePreprocessorImpl`)이 갈려 결정 표가 JVM 유닛으로 덮인다. `inSampleSize` 만으로는 배경 2049~4095 구간이 sampleSize 1에 걸려 밀도 비로 디코드 단계까지 내려받는다. ⚠️ **없던 디코드가 생겼다** — 변경 전 업로드는 바이트 복사뿐이었다. 구현이 스스로 잡은 함정 하나(`inTargetDensity` 가 결과 density 로 남아 흰 판 합성이 자동 축소되는 것)는 문서가 예측한 적 없다. 반대로 저장소 주석은 **스펙이 철회한 메모리 비교**를 그대로 들고 있다(OQ-P-388, 동작 영향 없음). **두 라운드가 만나는 자리가 이번의 발견**(OQ-P-391) — 하한은 편집본 원본 해상도에서 재는데 업로드본은 긴 변 1500까지 줄어, **하한을 어느 좌표계에서 재는지가 정해진 적이 없다**. 조치: 아카이브 4건 · architecture 2건 · 미결 5건. `api/` 는 원격 연동 코드 0건이라 불변. 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-08 | `b7674e88` | Merge #469(캔버스 응답 `groupName` 수신) · #470(버전 1.1.1 코드 8) | delta 2건, **12파일 57/6**, **머지 둘 다 트리 = 브랜치 팁**. 유닛 1137 → **1139건**(+2), 계측 **37건** 유지. 선작성 문서가 없어 **아카이브 이동 0건**, 미결 **신설 0건**(`oq-next` 386 유지). **`api/conventions.md` 「Android 불일치」가 5 → 4건**이 됐다 — 직전 회차의 서버 delta(`9c13852`)가 벌린 다섯째 행이 **하루 만에** 걷혔다. **#469**: `GetTodayParfaitResponse`·`CanvasVO` 에 `groupName` 이 서고 `toCanvasVO` 가 `GroupName` 으로 감싼다(오늘·상세 두 조회 공통). ⚠️ **서버가 없애 주려던 왕복은 남는다** — C-001 상단 바의 정본을 그룹 목록 캐시(ADR-0023)로 두고 캔버스가 준 이름은 **그 캐시가 빌 때만** 채우기로 했다(캔버스 SSoT 가 그룹의 값을 정본으로 들면 ADR-0029 의 "값을 얻는 길은 하나"가 저장소 사이에서 깨진다). 그래서 이 필드가 값을 내는 자리는 **푸시 딥링크·프로세스 재시작 복귀**이고, 그것이 서버가 필드를 만든 이유와 겹친다. 같은 라운드가 **목록에 없는 그룹이면 이름을 지우던 자리**도 고쳤다(`orEmpty()` → `return@collect`). OQ-P-383 **부분 해소**(①②③ 닫힘 / **④ 404 `GROUP_NOT_FOUND` 미분기 잔존**). **#470**: `appVersionCode` 7 → 8 · `appVersionName` 1.1.0 → 1.1.1, `.kt` 0건. 조치: api 3건 · open-questions 1항목 · **ADR-0029 📌 소유 경계 신설** · data-layer 📌 · 아카이브 스펙 2건 · adr/0003·adr/README 버전 as-built. `api/` `verified` 불변(서버 대조일). 미머지 하나(`feature/debug-mode`) 유지 |
| 2026-09-08 | `23675cc1` | Merge #464(토핑 테두리 거리장 통일) · #465(캔버스 넘은 토핑 박스) · #466(초대코드 단일 필드) | delta 3건, **39파일 2102/782**, **머지 셋 다 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 1106 → **1137건**(+31), 계측 35 → **37건**. **선작성 스펙 1·계획 1 아카이브 이동**(`topping-border-distance-field`, 8 Task 전량 수행). 미결 **신설 0건**(`oq-next` 383 유지), OQ-P-102에 📌 1덩이. **미머지 표기 스무 자리를 걷었다.** **#464**: 거리판 코어가 `core:util:{jvm,android}`·`core:ui` 로 서고 네 화면이 같은 거리장으로 그리고 판정한다. 스펙과 어긋난 자리 0건(스펙이 먼저 as-built 로 고쳐져 있었다), **계획과는 넷** — 값 타입·상수가 `model/`·`ToppingOutlineSpec` 으로 갈라졌고, 예정에 없던 `ToppingBorderPlateCache`(전역 LRU 32칸)가 깜빡임 때문에 생겼고, **지연 재생성 갈래가 통째로 폐기**됐고(굵기 dp 고정이 눈에 보이는 성질이라 늘려 그릴 수 없다 → Task 8 Step 3도 무효), 굵기 범위가 2~30dp 로 좁혀져 `domain` 한 곳으로 모였다. 그 마지막 하나가 읽는 쪽 `VOMapper` 둘에 클램프를 만들어 **`api/conventions.md` 「Android 불일치」가 3 → 4건**이 됐다 — 서버 delta가 벌린 것이 아니라 **앱이 스스로 만든 첫 간극**이다(OQ-P-381 ②). **#465**: 토핑을 캔버스보다 크게 키우면 박스·이미지만 제자리에 서고 버튼만 밖으로 가던 것을 고쳤다. 셋이 같은 `center`·`sizeAfterScale` 을 보는데도 갈린 이유는 **놓는 방식**이었다 — `requiredSize` 노드가 부모에게 보고하는 겉크기는 잘리고 Compose 가 그 안에 내용을 가운데 정렬하므로(`Placeable.apparentToRealOffset`) 좌상단을 직접 계산한 쪽만 **넘침의 절반**만큼 밀린다. 셋 다 `Modifier.centeredAt` 으로 통일하고 계측 테스트가 두 방식을 나란히 재서 계약을 고정한다. c106 스펙의 「같은 배율을 세 번 표현하지 않는다」가 **값에 대해서만 참이었다** — 조건(자식이 부모보다 커진다)을 안 적어 두 라운드를 버텼다. **#466**: 초대코드 칸 여섯이 텍스트 필드 하나로 합쳐져 `InputMode` ADD/EDIT 분기가 사라지고 `focusedIndex` 가 `Int?` → `Int` 로 좁혀졌다. 브랜치 상태로 이미 문서화돼 있어 이번엔 꼬리표만 PR 번호로 바꿨다(유닛 +18로 이번 delta 최대). 실기기 확인 기록은 #464 육안 6항목과 #466 한 줄(a004 스펙 각주)뿐이고 **#465는 계측 테스트만 있고 실기기 기록이 없다** |
| 2026-09-07 | `2285d09d` | Merge #461(그룹 참여 닉네임 기본값) · #463(캔버스 저장 미리보기 캡처 홀더) | delta 2건, **14파일 302/28**. 유닛 1096 → **1106건**(+10: 홀더 4 · 캡처 캐시 2 · A-004 2 · S-102 2), 계측 **35건** 유지. **선작성 스펙 1·계획 1 아카이브 이동**(`canvas-save-preview-capture-holder`, 하루 만). **머지본이 스펙과는 0건, 계획과는 세 자리에서 갈렸다** — 앞 회차가 코드리뷰 결과를 스펙에만 실어 정정했기 때문이고, 이번 감사의 절반이 계획에 as-built 각주를 다는 일이었다. **#463**: `CanvasCaptureHolder`가 캡처 비트맵을 캔버스 메인 → 미리보기로 날라 정상 경로에서 디스크 왕복이 사라졌다(`NavKeyCanvasImageSave`는 그대로, 저장 확정은 지금도 파일을 읽는다). 계획과 갈린 셋 — ① `onDispose`에서 자기 `NavKey`가 `Navigator.backStack`에 **없을 때만** 비우는 조건 신설(계획은 "앱 코드에 비우는 호출을 넣지 않는다"였다. 비파괴 성질은 유지) ② `compress` 반환값 검사에 유닛 2건(`Context` `mockk` + `cacheDir`에 `TemporaryFolder` — "파일 IO라 감싸기 어렵다"가 틀렸다) ③ 신규 2 → **3**, 유닛 4 → **6**. **#461**: 참여 갈래 S-102 입력칸이 빈 채로 서던 것을 채웠다 — A-004가 `GetMyAccountFlowUseCase`를 `init`에서 구독해 `NavKeyGroupNickName`의 세 번째 인자로 넘기고, **생성 갈래(#312)의 경로를 그대로 복제**했다. 다만 값이 아직 없을 때 **두 갈래가 반대로 답한다**(생성은 이동을 접고, 참여는 빈 값으로 넘어간다) → OQ-P-377 신설. 미머지 표기 셋(navigation-flow 1 · open-questions 2)을 걷었고 OQ-P-365 ③이 한 칸 좁아졌다. 조치: 스펙·계획 각 1건 아카이브 + 각 README 1행, 아카이브 스펙 2건 as-built(a004 · s102), architecture 1건(navigation-flow — 도식 · 참여 갈래 각주 · 낡은 `NavKeyGroupNickName(groupId)` 정정 · 미머지 표기), api 1건(parfait-group Android 매핑, `verified` 미변경 — 원격 연동 코드 변경 0건), open-questions 3항목(377 신설 / 364 · 365 정정), doc-baseline · index 기준선 갱신. ⚠️ **실기기 확인 0회** — 캡처 홀더의 유일한 증상이 "느리다"인데 그것을 보는 자동 검증이 없다. 미머지 **하나 유지**(`feature/debug-mode`, 신규 0) |
| 2026-09-06 | `5907e286` | Merge #455(크림 하한 3칸) · #456(푸시 딥링크 엣지 케이스) · #457(C-103-Error 통합·「편집 없이 사용」) · #458(버전 1.1.0 코드 7) | delta 4건, **24파일 399/130**. 유닛 1091 → **1096건**(+5), 계측 **35건** 유지. **선작성 스펙 1·계획 1 아카이브 이동**(`c103-error-use-original`, 하루 만 — 대조 결과 **설계와 코드가 어긋난 자리 0건**). **#457**: 실패 원인 분기(`SegmentationErrorKind`)를 걷어 문구를 한 벌로 합치고, 원본을 그대로 토핑 재료로 보내는 「편집 없이 사용」을 더했다 — 후보 선택 경로를 재사용하되 `saveBitmap` **한 번**으로 떨군 경로를 두 자리에 싣는다. 디코드 실패는 실패 화면이 아니라 **뒤로 가기**라 실패 화면은 원본이 사는 상태에서만 뜬다. `SaveEditedImageUseCase` → **`SaveBitmapUseCase`** 개명(6파일). OQ-P-153 ④ 종결·OQ-P-344 완화. **#456**: 되살린 태스크가 같은 딥링크를 다시 발행하던 경로를 `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`로 막고(유닛 2), 게이트 둘(스플래시 이탈 대기·`HasActiveSessionUseCase`)이 develop에 섰다. `route`·`type` 파싱이 enum `key`로 옮겨갔고 알림 아이콘이 전용 에셋 `ic_notification`으로 갈렸다(임시 여백 보정 → OQ-P-374). **#455**: `MIN_MIDDLE_COUNT` 1 → 2로 크림 하한이 3칸이 돼 위키 저개수 규칙과 처음 맞았다(증가 규칙은 여전히 높이 기반 → OQ-P-375). 앞 회차가 이름까지 적어 둔 "등록 호출부 0건" 낡은 문구를 OQ-P-351·352·359에서 걷었다. `oq-next` 374 → 376 |
| 2026-09-05 | `489b14cc` | Merge #450(알림 권한 안내 · 기기 토큰 등록 · 이벤트 버스 재배치) | delta 1건, **42파일 944/77**. 유닛 1072 → **1091건**(+19), 계측 **35건** 유지. **선작성 스펙 1건 아카이브 이동** — 대조 결과 **설계와 코드가 어긋난 자리 0건**(정정은 결정 2 표의 비고 한 칸: 등록을 거는 자리가 `saveSession` 직후가 아니라 `refreshMyAccount` 뒤다). **OQ-P-341이 닫혔다**(2026-09-02 개설, 여덟 회차) — ②는 **세션 축 넷**(`LoginWithKakaoUseCase`·`SignUpUseCase`·`BootstrapSessionUseCase` 성공 분기 + `onNewToken`), ③은 **A-004·A-005 완료 직후**, ④는 **거부해도 등록한다**. 핵심은 **등록과 권한이 별개 축**이라는 것 — 토큰은 권한과 무관하게 발급되므로 등록을 권한에 매달면 재로그인·기기교체·재설치 사용자가 등록 경로에 닿지 못한다. `register()`는 **`suspend`가 아니고** 실행은 `:data` 구현이 `@ApplicationScope`에서 하며 재시도 3회(3초·6초)·`Mutex` 겹침 방지가 그 안에 있다. **OQ-P-358도 닫혔고** API 33 미만 정책(`NotificationPermissionManager` — 그 아래는 **허용으로 본다**)이 되살아난 `showNotification`의 같은 결함까지 고쳤다. **이벤트 버스 개명**(`domain/event`·`data/event`, 인터페이스 `~EventBus`·구현 `~Impl`, 동작 무변경)과 **`:app` 최초 Hilt 모듈**(`DeviceTokenModule` — Firebase 경계 때문에 `:data`로 못 내린다)이 함께 왔다. 조치: 스펙 1건 아카이브 + specs/README 1행, ADR 3건(0013 되살림 완결 · 0004 평면 배치 예외 · 0021 개명 · README 3행), api 3문서(notification.md — 엔드포인트 표·요구 표 2행·`기기 토큰 등록 결선` 절 신설·미결 절 / conventions.md 관측 가능 메모 / README 도메인 표·본문), architecture 4건(data-layer — 개명·등록구·DI 2행·Repository 1행 / module-structure — `event/`·`notification/`·`:app` di·권한 판정 / navigation-flow — 그룹 플로우에 게이트 / state-management — 심볼), open-questions 7항목(341·358 해소 / 370·371·372·373 신설 / 362 갱신). ⚠️ **실서버·실기기 확인 0회** — 등록이 204를 받는지, 발송이 `NO_DEVICE_TOKEN`을 벗어나는지 아무도 보지 않았다. 미머지 **둘 → 하나**(신규 0) |
| 2026-09-05 | `bc216632` | Merge #445(캔버스 저장 미리보기) · #453(재시도 문구 통일) · #449(첫 진입 튜토리얼) | delta 3건, **42파일 1535/98**. 유닛 1060 → **1072건**(+12: `UserConfigRepositoryImplTest` 2 · 튜토리얼 ViewModel 10), 계측 **35건** 유지. **선작성 스펙·계획 없음 → 아카이브 이동 0건**. **#449**: 디자인시스템에 `ygtutorial/` 4종(`YGTutorialOverlay`·`YGTutorialBox`·`YGTutorialProgress`·`YGTutorialBoxPlacement`) 신설 — **뚫린 오버레이가 아니라 딤까지 구워진 알파 없는 풀스크린 목업 PNG**가 화면을 통째로 덮고, 소비 셋이 모두 **스캐폴드 밖 형제**로 놓는다(안에 넣으면 딤이 상태바 밑에서 끊긴다). 버튼 라벨·진행 표시가 `isLast` 하나에서 나온다. 함께 **기기 축 저장소가 처음 생겼다** — `UserConfigLocalDataSource`(평문 JSON 한 키 + `Flow`) · `UserConfigRepository` · `UserConfigVO` · `TutorialKind` · UseCase 2 · 프록시 `DataStorePreferences`(암호화 판에서 암호화만 뺀 복제). 저장 형태는 enum 이 아니라 **이름 문자열**(구버전이 모르는 항목에서 터지면 설정을 통째로 날린다). 소비는 C-001(3장, `CanvasTutorialStep`)·갤러리 업로드(1장)·누끼 확인(1장). **#445**: `RequestCanvasCapture` → **`RequestCanvasCaptureForPreview`** 개명, 저장이 `NavKeyCanvasImageSave(imagePath, date)` 미리보기를 거쳐 **같은 캐시 파일을 다시 읽어** 저장한다(다시 캡처하지 않는다). 미리보기는 저장하지 않아 **ViewModel 없는 두 번째 화면**이다. **#453**: 재시도 버튼 문구 `다시 시도하기` → `다시 시도`(네 화면 동일). 조치: architecture 4건(design-system — `ygtutorial/` 트리·인벤토리·항목 / navigation-flow — `캔버스 저장 미리보기 왕복` 절 신설·인자 목적지 / data-layer — `UserConfig*`·평문 프록시·DI 2행 / state-management — `launchWhileSubscribed` 소비 확장·State 이탈), 아카이브 스펙 1건(c001-canvas-gallery-save as-built), open-questions 8항목(363~369 신설 / 341 갱신), doc-baseline·index 기준선 갱신. ⚠️ **실기기 확인 0회**이고 미리보기 왕복·캡처 캐시에 테스트가 0건이다. 미머지 **넷 → 둘**(신규 0) |
| 2026-09-05 | `29c2f050` | Merge #446(푸시 알림 딥링크) · #447(FCM 수신부) | delta 2건, **17파일 462/0**(삭제 0줄). 유닛 1047 → **1060건**(+13: `PushDeepLinkParserTest` 11 · `PushDeepLinkEventBusImplTest` 2), 계측 **35건** 유지. **`app` 모듈이 유닛 테스트 소스셋을 처음 가졌다**(`parfait.test.unit`이 **진입 모듈**에 처음 붙었다 — 그전까지 core·`data`·`domain`과 feature `impl`에만 있었다. 파싱을 `Intent`에서 뗀 덕에 가능했다). **선작성 스펙·계획 없음 → 아카이브 이동 0건**. **2026-08-22 PR #325가 걷어낸 FCM 축이 되살아났다** — `firebase-messaging` 의존 · `push/ParfaitFirebaseMessagingService` · `BaseApplication`의 채널 생성 · 매니페스트 서비스 등록이 돌아왔고 **채널 id는 앱이 정하지 않고 서버가 못 박은 `parfait_default`를 따랐다**(OQ-P-352 ① 해소). 예고에 없던 **딥링크 축**이 함께 왔다 — `:domain` `PushDeepLink`·`PushNotificationType`·`PushDeepLinkEventBus` + `:data` `PushDeepLinkEventBusImpl`(`Channel(CONFLATED)`) + **앱 루트 `MainRoute` 단일 수집**으로 **세션 종료 이동 구조를 그대로 복제**해 새 ADR을 만들지 않았다. **서버가 요구한 다섯 중 셋만 맞다** — ⚠️ `date`를 안 읽어 항상 최신 캔버스로 열고, ⚠️ 중복 수신이 알림 두 개로 쌓이며(`messageId.hashCode()`), ❌ **토큰 등록을 한 번도 안 부른다**(`onNewToken` 주석의 전제는 PR #437이 이미 무너뜨렸다 — OQ-P-341 ②). ⚠️ **`POST_NOTIFICATIONS`는 선언만 돌아오고 요청 코드가 0건**이라 Android 13+에서는 표시 자체가 막힌다(OQ-P-358). 조치: ADR-0013 되살림 정정(+`위험·방어` 정정), api 3문서(notification.md — 요구 표에 앱 열 신설·`푸시 수신·딥링크` 절·미결 4항목 / conventions.md Android 불일치 1건 → **3건** / README 도메인 표·주석), architecture 3건(navigation-flow — `푸시 딥링크 이동` 절 신설 / data-layer — `PushDeepLinkModule`·`domain/model` 하위 패키지 열 · 통로 복제 / module-structure — `app` `push/`·유닛 테스트 소스셋), open-questions 9항목(358·359·360·361 신설 / 341·351·352 부분 해소 / 343·354 갱신), doc-baseline·index 기준선 갱신. ⚠️ **실기기 확인 0회**이고 등록 호출부가 0건이라 발송은 여전히 전부 `NO_DEVICE_TOKEN` 취소다. 미머지 **여섯 → 넷**(신규 0) |
| 2026-09-04 | `e6ce42b1` | Merge #440(원격 이미지 로딩 표현 — 캔버스 일괄 드러내기 · G-001 순차 등장) | delta 1건, **41파일 1697/224**. 유닛 1029 → **1047건**(+18: `reveal/` 순수 함수 11 · `CanvasLoadState` 7), 계측 17 → **35건**(+18, 파일 6 → 11 — `core:ui` 첫 계측 소스셋). **선작성 스펙·계획 없음 → 아카이브 이동 0건**. **#440**: 세 라운드째 미머지로 세어 온 `feature/image-loading-placeholder`가 들어왔고 **머지본이 예고와 셋 달랐다**(`YGToppingGroup` 무변경 · 두 화면의 구현이 갈림 · `YGSkeleton` 소비처는 `YGCanvas` 배경 하나) → OQ-P-346 해소 메모가 그 대조를 적는다. **같은 문제에 반대 답 둘** — C-001은 `rememberBatchRevealState`로 다 모일 때까지 안 내고(빈 목록은 완료가 아니다, 리셋 키는 그리는 캔버스), G-001은 안 기다리는 대신 `rememberStaggeredRevealState`로 400ms씩 쌓는다. 잇는 것은 `core:ui` `reveal/`과 `Modifier.revealed`(알파 0 + 시맨틱 제거, 측정·배치는 유지)뿐이다. 실패도 반대다(캔버스는 한 장만 실패해도 전체 차단, 목록은 실패분만 폴백). **`YGScaffoldV2`에 `loadingOverlay` 슬롯**(기본값이 종전 동작이라 호출부 무변경, C-001이 실패 덮개를 끼운다) + **터치 삼킴이 슬롯 몫으로 내려가며 `YGDimOverlay` 분리**. ⚠️ 접근성 차단이 `hideFromAccessibility()` → `clearAndSetSemantics { }`로 **수단 정정**(앞의 것은 노드 하나만 감춘다). G-001 실패 갈림이 세 번째로 뒤집혀 **"당겼는가"** 기준이 되고 `ShowRefreshError`·문구·토스트 호스트 삭제, 새로고침 인디케이터가 로띠 + 문구 2줄로 플랫폼 기본을 벗어났다(OQ-P-113 ① 해소). 조치: architecture 3건(design-system — 신설 컴포넌트 2·`YGLoadingArt`·이미지 로더·덮개 슬롯 / module-structure — `core:ui` `reveal/` / state-management — 실패 갈림), 아카이브 스펙 5건(g001-group-list · ygscaffold-v2 · screen-resume-refetch · c001-canvas-today-detail · topping-alpha-hit-test), specs/README 5행, open-questions 9항목(355·356·357 신설 / 346 해소 · 348 부분 해소 / 102·112·113·167·330·349 갱신), doc-baseline·index 기준선 갱신. ⚠️ **실기기 확인 0회**이고 늘어난 계측 18건도 실행되지 않는다(CI가 `core:ui`를 컴파일 대상에 안 넣는다). 미머지 **일곱 → 여섯**(신규 0) |
| 2026-09-04 | `2b1dce3a` | Merge #451(API 현행화 260903 — `http/` 요청 모음) | delta 1건, **5파일 550/3**(전부 `http/`). 유닛 **1029건**·계측 **17건** 유지(테스트 파일 무변경). **선작성 스펙·계획 없음 → 아카이브 이동 0건**. **#451**: `notifications.http`(기기 토큰 등록 — 204·본문 없음·upsert 재호출·400 4종·401 대조군)와 `fcm-test.http`(**서버를 거치지 않고 FCM v1 API 로 직접** 발송해 앱 수신을 확인)가 신설되고 `http-client.env.json`·`_reset.http`·`http/README.md` 가 함께 맞춰졌다(`fcm_*` 세 변수는 손으로 채우는 값이라 `_reset.http` 비우기 목록에서 제외). `http/` 커버 **25/29 → 26/29, 일곱 번째 왕복**이고 **방향이 반대다** — 요청 모음이 앱 코드보다 앞서 나갔다(등록을 부를 수단이 develop 에 0건). `.kt`·gradle 무변경이라 **코드 드리프트 0건**이고 `android_status`·Android 매핑 판정은 그대로 옳다. 조치: api/README(파일 목록·커버 셈·`fcm-test.http` 성격), api/notification.md(Android 매핑에 확인 수단 표·미결 2항목), open-questions 5항목(OQ-P-354 신설 — 서버 발송 페이로드 복제를 세는 축이 없다 / OQ-P-092·108·341·352 갱신), doc-baseline·index 기준선 갱신. ⚠️ 두 파일 다 **실행 기록 0건**(`fcm_access_token` 1시간 만료·서비스 계정 키 필요, 받을 쪽도 없다). 미머지 **다섯 → 일곱**: `feature/push-notification-permission`(OQ-P-341 ②③④) · `feature/#420-canvas-tutorial` 추가 |
| 2026-09-03 | `c74f40eb` | Merge #444(스플래시 로띠 교체) | delta 1건, **1파일 0/0**(바이너리 교체). 유닛 **1029건**·계측 **17건** 유지(테스트 파일 무변경). **선작성 스펙·계획 없음 → 아카이브 이동 0건**. **#444**: `feature/intro/impl` `res/raw/splash.lottie` 가 새 로고 애니메이션으로 교체됐다. dotLottie 를 풀어 대조하니 manifest 판본·애니메이션 id(`Lottie-Logo`)·프레임률·재생 길이·화면 크기·레이어 여덟(`Parfait` 일곱 글자 + `Stroke`)이 교체 전과 동일하고 각 레이어의 패스·키프레임만 다르다 → `R.raw.splash`·`SplashScreen` 무변경, 위키 [[스플래시-애니메이션]] A-001 정본의 **60fps·3.5초** 타임라인 유지, 진입 대기 길이(OQ-P-229) 불변. **드리프트 0건**. 조치: open-questions 3항목(OQ-P-350 신설 — 바뀐 그림을 본 사람 0건·회귀 감지 수단 없음 / OQ-P-229·341 갱신), doc-baseline·index 기준선 갱신. ⚠️ 실기기 확인 0회. 미머지 **둘 → 다섯**: `feature/debug-mode` · `feature/image-loading-placeholder` · `feature/#423-canvas-save-preview` · `feature/push-fcm-service` · `feature/push-notification-deeplink` |
| 2026-09-03 | `40e1fca6` | Merge #439(README 소개·스크린샷) · #437(기기 FCM 토큰 등록 data 표면) · #438(ML Kit 모듈 설치 대기·실패 처리) · #442(누끼 영역 빨간 틴트) | delta 4건, 52파일 940/101. 유닛 1015 → **1029건**(+14: 설치기 7 · 세그멘테이션 ViewModel 3 · 알림 DataSource 4), 계측 **17건** 유지. **선작성 스펙 1·계획 1 아카이브 이동**(segmentation-module-install), 미결 1건 신설·3건 갱신(`oq-next` 347 → 348). **#438**: `installModules` 의 Task 반환을 설치 완료로 읽던 것을 고쳐 `InstallStatusListener` 의 종료 신호까지 기다린다 — `SegmentationModuleInstaller`(`Mutex` + `CompletableDeferred` 로 진행 중 설치 공유) · `ModuleInstallGateway`(GMS 이음매) · `PlayServicesModuleInstallGateway` · `ModuleInstallModule` · `PrepareSegmentationModuleUseCase` 신설, `ImageSegmentationRepository` 메서드 5 → **6**. 사전 설치는 **사진 확인 화면 진입**(촬영·갤러리 합류점). `isError` → `SegmentationErrorKind`(`SubjectNotFound`/`ModuleNotReady`) + `Retry` 인텐트 + `YGButton` 재시도(**디자인 검토 대기 시안**) → **OQ-P-153 ③ 해소, ④ 잔존**. 패키지 재배치: `repository/image` → `installer/image`(설치 3종)·`utils/image`(순수 커널 7), `data/util` → `data/utils` 통합. ⚠️ 모듈 없는 상태의 화면 경로는 재현 수단이 사라져 **미확인**. **#437**: `NotificationService`(`POST /api/v1/notifications/devices`, 204 본문 없음 → `safeApiCallNoContent`) · `NotificationRemoteDataSource`(+`Impl`, `platform` 을 `"ANDROID"` 상수로 고정) · `domain.model.notification.DeviceToken`. **표면만이고 호출부 0건**, FCM 토큰 취득 심볼도 여전히 0건 → **OQ-P-341·342 는 열린 채 갱신**. api 표면 27/29 → **28/29, 공백 1**. **#442**: `ToppingEditScreen` 이 남는 영역에 `Cherry500` 틴트 한 겹(`MASK_TINT_ALPHA`, `SrcAtop`), 영역 탭 전용. 위키 [[누끼-편집]]에 대응 조항 없음 → **OQ-P-347 신설**. **#439**: README 소개·스크린샷(코드·계약 무변경). 조치: 스펙 1·계획 1 아카이브 + README 2행, api 3문서(notification.md `android_status` `none` → `partial`·엔드포인트 표·Android 매핑 절, README 도메인 표·총계), architecture 1건(data-layer — 메서드 6·모듈 설치 절 재작성·`util/image` → `utils/image` 정정·여덟 번째 Service), ADR-0012 머지 표기, 아카이브 스펙 c103 정책 대조 표 1행, open-questions 4항목. ⚠️ 실기기 확인 0회. 미머지: `feature/debug-mode` · `feature/image-loading-placeholder`(OQ-P-346) |
| 2026-09-01 | `0173e454` | Merge #412(핸들 모서리) · #413(캔버스 상단바) · #414(지난 캔버스 메뉴) · #411(환영 배너) · #434(버전 1.0.0) | delta 5건, 25파일 449/85. 유닛 1012 → **1015건**(+3), 계측 **17건** 유지. **선작성 스펙·계획 없음 → 아카이브 이동 0건**, 미결 1건 해소·2건 신설(`oq-next` 339 → 341). **#411**: 그룹 생성·참여의 종착지가 G-001 목록 → **C-001 캔버스**로 옮겨 위키 [[기능정의서-v6]] 배선과 맞았다(OQ-P-135 해소). 복귀 관용구가 `goToSingleClearTop` → **`replaceAll(목록)` + `goTo(캔버스)`** 두 줄이 되고 `goToSingleClearTop` 소비처 0건. `NavKeyCanvasMain` 에 `welcomeGroupName`·`welcomeInviteCode` 추가(진입 사유를 나르는 첫 인자), `YGAlert` 첫 프로덕션 소비처 + `buttonIconResource` 신설, `GroupCreate`·`GroupNickName` 의 `NavigateToNext` 가 `data class` 로 승격. ⚠️ 배너 문구·복사 흐름·1회 보장에 근거 부재 → **OQ-P-339 신설**. **#413·#414**: 갤러리 저장이 하단 메뉴 액션 → **날짜바 `ic_save` 아이콘**(오늘 캔버스에서도 저장 가능, 노출은 `isCanvasSaveVisible` = 빈 안내판 조건의 부정 → **OQ-P-340 신설**), `YGCanvasMenu.addAction` nullable·`YGMenuItem` 아이콘/비활성 신설, 캘린더 그림이 날짜 텍스트와 한 클릭 영역. **#412**: 토핑 편집 핸들 우상단↔우하단 교환(회전/크기조절), 계산식 불변. **#434**: `5/0.1.1 → 6/1.0.0`, 정식 판 근거 미기재(OQ-P-310 갱신). 조치: architecture 3건(navigation-flow·design-system·state-management), 아카이브 스펙 as-built 8건(designsystem-canvas-components·ygalert·c001-canvas-gallery-save·c201-canvas-calendar-server·c106-topping-place·c301-topping-edit-tab·a005·s102), specs/README 8행, open-questions 6항목(135·136·273·310 갱신 + 339·340 신설). ⚠️ 실기기 확인 0회. 미머지: `feature/debug-mode` |
| 2026-09-01 | `fa46e5cf` | Merge #430(그룹 추가 팝업 배경색) | delta 1건, **1파일 1/1** — 세어 온 회차 중 가장 작다. 트리 = 브랜치 팁(충돌 해소 편집 0건), 유닛 **1012건**·계측 **17건** 유지(테스트 파일 무변경). **선작성 스펙·계획 없음 → 아카이브 이동 0건, 신규 미결 0건**. **#430**: G-001 그룹 추가 오버레이의 메뉴 판 배경이 `Cherry50` → `Gray.White`(근거는 커밋 서술 — 항목을 얹는 바탕은 중립색). 항목·구분선·모서리·화면 구조·계약 무변경. 조치: 아카이브 스펙 g001-group-list as-built 한 줄 정정(+ `verified`), doc-baseline·index 기준선 갱신. 색이 위키 정책에 없다는 점은 기존 오버레이 구조 미결이 덮고 있어 항목을 늘리지 않았다. ⚠️ 실기기 확인 0회. 미머지: `feature/debug-mode` |
| 2026-09-01 | `6a1da1b0` | Merge #428(API 현행화 260831) · #425(최근 알맹이 테두리 편집) | delta 2건, 37파일 997/437, **두 머지 다 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 996 → **1012건**(+16), 계측 **17건** 유지. **선작성 스펙 1·계획 1 아카이브 이동**(두 회차 연속) — 전날 "로컬 브랜치에만 있다"고 적어 둔 것이 그대로 develop 이 됐고 **as-built 이탈 0건**이라 문서 일은 표기 갱신으로 끝났다. **#428**: 확인 버튼의 변형 저장이 단건 PATCH N회에서 **일괄 PATCH 1회**로 접혔다 — `UpdateToppingsUseCase`·`ToppingRepository.updateAll`·`ToppingTransformUpdate` 신설, 단건 경로(Service·DataSource·Repository·UseCase·wire DTO) 통째로 걷힘(서버 엔드포인트는 잔존 → OQ-P-335 가 develop 사실이 됨). 저장을 가르는 축이 **토핑 단위에서 축 단위**로 바뀌어 변형 일괄 1회 + 테두리 병렬 N회다. ⚠️ 부분 성공이 사라져 하나가 걸리면 변형을 보낸 토핑 전부가 dirty 로 남는다(OQ-P-334 ① 해소·②③④⑤ 잔존). 과거 캔버스 목록 `status` 가 `PastCanvasVO` 까지 올라왔고 **달력 점 기준은 토핑 개수 유지**(위키 [[C-201-캘린더-정책-v0.1]] 이 정본 → OQ-P-333 ②③ 해소), 그룹 목록 `recentImageUrl` 의 바뀐 뜻이 KDoc 두 곳에 반영(OQ-P-336 ③ 해소). **#425**: 최근 알맹이 재사용 진입에서 잠겨 있던 "사진 편집"이 `borderOnly` 편집으로 열렸다 — 플래그 뜻이 "캔버스에 놓인 토핑"에서 **"되살릴 원본이 없는 진입"**으로 넓어지고 원본 자리에 알맹이를 같이 넣는다. 초안 재기록 가드가 `SavedStateHandle` 로 옮겨 프로세스 사망 복원이 편집 결과를 덮어쓰지 않고, 테두리 미리보기가 사방 여백을 둔 판에 앉아 굵은 테두리가 안 깎인다. **선작성 문서 없이 들어와 신규 미결 둘이 전부 이쪽에서 나왔다** — OQ-P-337(여백 상수와 굵기 상한이 서로를 모름·테두리 렌더 규칙이 화면마다 둘) · OQ-P-338(원본 자리의 알맹이가 재편집 좌표계 전제를 바꾼다), `oq-next` 337 → 339. 조치: 스펙 1·계획 1 아카이브 + README 2행, api 4문서(`parfait-image.md` Android 매핑·낡은 심볼 정정, `parfait.md`·`conventions.md`·`README.md` 머지 표기), architecture 2건(navigation-flow·data-layer), 스펙 as-built 3건(c106·c301·c103). ⚠️ **실기기·실서버 확인 0회** — 일괄 PATCH 는 `http/parfait-image.http` 에도 여전히 없다. 미머지: `feature/debug-mode` |
| 2026-07-15 | `9085bc7` | Merge #143 (#94 clickable) | #94 clickable 반영(PR #47) + #140 ygbutton 드리프트 수정(PR #48). 미머지: #135 modal·#136 etc |
| 2026-07-16 | `bd844a5` | Merge #141 (ygchipbutton) | 신규 컴포넌트 2건 스펙 작성(implemented·archive): #141 YGChipButton·#142 YGToggleButton. design-system 인벤토리·원자색 확산 노트 갱신, open-questions에 YGToggleButton 규약 이탈 등록. 미머지: #135 modal·#136 etc |
| 2026-07-18 | `8f63945` | Merge #157 (YGButton-fix) | delta 머지 1건(#157 fix/YGButton-fix): `YGButtonType.kt` 변형별 disabled/pressed foreground·background 색값 스왑 수정만. API·심볼·구조·변형 목록 불변 → 파르페 규율상 색값 미기재라 **문서 콘텐츠 변경 없음**. 기준선 해시만 갱신. 미머지: `refactor/design-system-preview`(프리뷰 통일·YGAtomicColors public) |
| 2026-07-18 | `8cdf942` | Merge #151 (#135 modal) | 신규 머지 6건 반영. modal(#151/#135)·invitecard(#148/#136) spec·plan `implemented`/`done`·archive 이동(드리프트 없음). 신규 컴포넌트 spec 5건 작성(implemented·archive): YGColorChip·YGDate/YGLabel·YGTopBar·YGDateButton·YGDangerZone(#150/#152/#147/#148). ADR-0013(Firebase FCM)·ADR-0014(로깅 추상화 backfill) 신규. design-system 인벤토리·원자색·프리뷰·미머지 마커 정리. open-questions: [2026-07-13] 해소 + 신규 5건(YGColorChip 패키지 불일치·nametag 12/14·YGDateButton clickableYG·FCM 토큰·analytics 패키지). 미머지: 없음 |
| 2026-07-19 | `ce4e9b8` | Merge #158 (design-system-preview) | delta 머지 1건(#158, 이전 미머지 추적 항목). 프리뷰 관용구 `@YGPreview`+`PreviewBox` 통일 + `YGAtomicColors` internal→public. preview-migration spec `implemented`·plan `done`·archive 이동(+README 등록). design-system(YGAtomicColors public·원자참조 원칙 이탈·프리뷰 통일 완료)·ADR-0010·open-questions(프리뷰 ② 해소, YGAtomicColors public 코드머지·원칙 ADR 잔존) 마커 해소. 미머지: `feature/sync-design-system-260719`(dashed·radius none, in-progress 스펙 2건) |
| 2026-07-20 | `7b954a8` | Merge #160 (#85 app-side-menu) | delta 머지 1건(#160). S-001 앱 설정 화면 구현(ProfileCard+List 4항목·MVI ViewModel·계정/약관/개인정보 NavKey+stub Route+entry). 선작성 spec/plan과 대조: VM·Route·EntryBuilder·Screen·stub·strings 전부 설계 일치. **드리프트 1건**: ProfileCard 각짐이 설계 `radius.none` → 실제 `RectangleShape` 직접 참조(토큰 미머지 우회). app-setting-s001 spec `implemented`·plan `done`·archive 이동(+README 등록), spec/plan 각짐 문구·코드 실제 반영, open-questions [2026-07-20] 등록(radius-none-sync 머지에 종속). 미머지: `feature/sync-design-system-260719`(dashed·radius none, in-progress 스펙 2건) 유지 |
| 2026-07-22 | `23ef432` | Merge #162 (yg-screen) | delta 머지 4건(#153·#154·#161·#162). **#162**(추적 스펙 in-progress): `core:designsystem screen/` YGScreen·YGScaffold·YGScreenScope + feature :impl EntryBuilder 리팩터 — 코드=설계 완전 일치, ygscreen-scaffold spec `implemented`·archive, open-q [2026-07-20] YGScreen ADR 항목 "코드 머지 확정"으로 갱신(ADR·통합 방향만 잔존). **#161**(draft 스펙/플랜 2건): `:feature:common:terms:{api,impl}` 신설(NavKey 2 + Route/Screen/VM 2 + NotionWebView + EntryBuilder), setting에서 이동 — s004-terms-privacy-webview·feature-common-terms-module spec `implemented`/plan `done`·archive(s004는 최종 위치 common:terms 노트 추가). ADR-0015·module-structure는 선작성 상태로 이미 정합. **#153 term-agree**·**#154 group-nick-name**: parfait 스펙 없던 신규 화면 → 사후 스펙 2건 작성(`implemented`·archive): intro-term-agree·s102-group-nickname. 코드 검증 정상(#154 길이 15·문자 규칙 위키 S-102 일치, term-agree는 저장·랜딩URL·다음nav TODO 잔존). 미머지: 없음 |
| 2026-07-23 | `01ba72e` | Merge #156 (invite-code) | 신규 delta 1건(#156 feature/invite-code). ⚠️ 직전 "현재 기준선" 블록이 `23ef432`로 스테일(step 5 미갱신) — 이력 표 최신 행은 `526f4c9`였음. 두 기준선 합집합(`develop ^23ef432 ^526f4c9`)으로 신규 머지를 재산정 → #156 단일 건만 미검증. #156 = G-002 그룹 초대코드 입력 화면 리팩터(`GroupInviteCode` Route/Screen/VM + `InviteCodeInputFieldElement` + `CheckInviteCodeValidUseCase`): codeLength 5→6, `FocusedFirstIndex` 인텐트(첫 필드 자동 포커스), 입력 시 errorText 리셋, InputFieldElement border/RoundedCornerShape 제거 재스타일. **화면은 두 직전 기준선에 이미 존재(신규 아님, 리팩터)**, parfait 스펙 없던 feature-local 화면. 변경 심볼이 어느 parfait 문서와도 무충돌 — `NavKeyGroupInviteCode`(navigation-flow)·`feature/groups/enter/{api,impl}`(module-structure) 불변, `YGInviteCard`(design-system, 별개 카드 컴포넌트) 미변경, codeLength는 매직넘버라 파르페 규율상 문서 미기재. **드리프트 0건 → 문서 콘텐츠 변경 없음, 기준선 해시만 갱신**(#157/#158 선례와 동일). 미검증 TODO 잔존(UseCase 검증·클립보드 자동복붙, 코드 주석). 미머지: 없음 |
| 2026-07-23 | `4266c58` | Merge #149 (design-system-component-text) | delta 1건(#149). 신규 노출-정책 컴포넌트 2건(spec `implemented`·archive): **YGAlert**(배너+YGAlertPolicy/Host 단일슬롯)·**YGToast**(YGToastType 3종+YGToastPolicy/Host 다중스택, 위키 [[Toast-공통-정책]] 일치). **드리프트 1건**: YGDate `YGDate(text)`→`YGDate(date, day)` 2텍스트 Row+테두리·패딩 하드코딩→토큰화 → ygtext-date-label spec as-built 갱신(패딩 하드코딩 열린질문 해소). YGLabel 불변. design-system 인벤토리·원자색·트리·프리뷰 마커 갱신. open-questions [2026-07-23] 신규(프리뷰 관용구 부분 회귀 — YGAlert/YGToast `@Preview`·YGDate PreviewBox 미사용). 미머지: 없음 |
| 2026-07-26 | `0adeed6` | Merge #166 (string-resource) | delta 1건(#166, chore). 이전 기준선 블록 `4266c58` 정상(이력 최신 `526f4c9`는 그 조상 — 스테일 없음). 하드코딩 정적 라벨 → `strings.xml` + `stringResource` 이동: `feature/intro/impl`(TermAgree)·`feature/groups/enter/impl`(GroupNickName·GroupInviteCode, 두 화면 문자열 파일 공용) `strings.xml` 신설. **문구·API·상태·구조 불변**(순수 리소스 추출). 조치: intro-term-agree·s102-group-nickname 아카이브 스펙 as-built 갱신(strings.xml 파일 구성·표시 규칙·related_code·verified), open-questions [2026-07-26] 신규(리소스화 부분 적용 — `TERM_CONTENT_LIST` title 리터럴 잔존·`InviteCodeResult.errorMessage`가 domain 표시문자열 보유로 ADR-0016 패턴 이탈·canvas 등 미착수 화면). G-002 초대코드 화면은 parfait 스펙 없음(#156 선례) → 문서 대상 없음. 미머지: 없음 |
| 2026-07-27 | `2225bb3` | Merge #175 (bump-claude-code-review) | delta 1건(#175, chore). 이전 기준선 블록 `0adeed6` 정상(스테일 없음). 변경 파일은 `.github/workflows/claude-code-review.yml` **단 1개** — Kotlin·gradle·모듈·심볼 변경 0. PR 리뷰 워크플로가 이 위키 repo(`citytexi/team-yg-pesonal-agent`)의 `wiki/`만 sparse-checkout(`.llm-wiki/`, submodule 미포함 → private 서브모듈 안전)해서 **정책 정합 대조 단계**를 리뷰 프롬프트에 추가. parfait에는 CI 워크플로 인벤토리 문서가 없어 대조 대상 자체가 없음 → **드리프트 0건, 문서 콘텐츠 변경 없음, 기준선 해시만 갱신**(#157/#158/#156 선례와 동일). 미머지: 없음 |
| 2026-07-29 | `dd29dda` | Merge #181 (design-system-text-component) | delta 2건. **#181**(추적 스펙/플랜 draft): 텍스트 영역 Figma sync — 코드=설계 완전 일치(D1~D4·T1~T4·A1·A3·P1·P2 + 갤러리 showcase 2화면), spec `implemented`·plan `done`·archive 이동(링크 `../`→`../../` 보정, Step 체크 완료), README 이동 등록. **#179**(parfait 스펙 없던 신규 화면 + 선행 리팩터): A-005 그룹 생성 화면 사후 스펙 작성(`implemented`·archive) — 그룹명 10자·인원 1~12·`VerticalGridLayout`, 위키 [[A-005-그룹명-정책-v0.1]]·[[그룹]] 일치. 함께 온 domain 리팩터가 **ADR-0016 원안과 형태 이탈**(`NicknameResult`→`NameValidResult`, `toStringResource` 확장 미머지·매핑이 feature VM `@StringRes`, `core:ui`→`:domain` 의존 없음) → ADR-0016 as-built 표 추가, s102 아카이브 스펙·s002 스펙/플랜(Task 1 skip) 갱신. module-structure(core:ui 역할·문자열 리소스 규약)·navigation-flow(인자 있는 NavKey + Assisted 주입, goTo 동반 규칙) 갱신. open-questions 신규 4건(ADR-0016 매핑 위치·A-005 진입 경로 부재·`GroupCreateConfig` UI 상수·`core:ui` 프리뷰 규약), [2026-07-26] ① 부분 해소. 미머지: 없음 |
| 2026-07-31 | `d538d0e` | Merge #165 (design-system-component-colorchip) | delta 1건(#165). `component/ygcolorchip/` 계열 개명·확장: `YGColorChip`→`YGNametagChip`(+`YGColorChipStyle`→`YGNametagChipStyle`, 파라미터 `text`→`userFirstName`, PreviewData/Provider 동반 개명), 신규 2종 `YGUserChip`(+`YGUserNameStyle` Medium/Bold, 칩+이름 Row) ·`YGChipColorIndicator`(`isChecked` Cherry↔투명 점). `YGColorChipType`은 변형 목록 불변(주석만 추가 — `NametagChipPlus`=5명 이상 "+" 칩). **드리프트 3건 처리**: ① ygcolorchip 아카이브 스펙 as-built 전면 갱신(심볼·시그니처·파일 구성·신규 2종 흡수), ② design-system 인벤토리·설명·과도기 마커 갱신, ③ 프리뷰 회귀 목록에 `YGUserChip` 추가(`@Preview`+`YGCustomTheme`, 같은 PR의 다른 2종은 표준 준수). **open-questions**: [2026-07-18] 패키지↔폴더 불일치 **해소됨**(폴더명으로 통일), [2026-07-18] 타입 12/14 항목은 `Plus` 용도 확정으로 쟁점이 "숫자 13 vs 정책 12"로 좁혀짐(잔존), 신규 2건 등록(`YGChipColorIndicator` 정책 근거·용도 불명 / 신규 2종 `:app-preview` 갤러리 미등록). 미머지: `feature/grouptag-topping-component` |
| 2026-08-01 | `1eff238` | Merge #186 (grouptag-topping-component) | delta 3건(#183·#185·#186) — 전부 **선작성 스펙/플랜이 있던 브랜치**가 한 번에 머지된 라운드. **#183**(버튼 sync + 미구현 5종, 스펙 2·플랜 2): 드리프트 9건 전량 반영 + `YGCircleButton`·`YGEditButton`·`YGEditTabButton`·`YGEditActionButton`·`YGCameraShutter` 신설 + `YGToggleButton` 삭제 + `SizeTokens.Size18`·`Size28`. **as-built 정정 3건**(`YGCircleButtonType`이 `@get:Composable` 아닌 `@Immutable`+평 `val`, `paintsOuterCircle` 속성 추가, `Default`·`Small` `iconTint` `Gray850`) — 나머지는 설계 일치. 스펙 범위 밖 아이콘 드로어블 현행화 11건 동반. **#185**(캔버스 5종 + `shape/canvasCutCornerShape()`, 스펙·플랜 1쌍): 코드=설계 완전 일치. **#186**(Grouptag-Chip·Topping-Group + `coil-network-okhttp` + `Size96`/`Size160` + 에셋 42개, 직전 기준선의 미머지 추적 항목): 코드=설계 완전 일치. 조치: 스펙 4건 `implemented`·플랜 4건 `done` + 양쪽 archive 이동(링크 `../`→`../../` 보정, canvas 플랜은 **frontmatter 자체가 없어 신규 작성**, Step 체크 완료), README 4행씩 아카이브 등록. design-system 인벤토리에 신규 12종 + `shape/` 패키지 추가·`YGToggleButton` 행 제거·토큰 스케일·Coil 페처·프리뷰 회귀 범위 갱신. open-questions: [2026-07-16] YGToggleButton·[2026-07-27] YGChipButton 세로 패딩 **해소됨**, 머지 반영 마커 7건, 캔버스 라운드 이월분 **신규 4건** 등록, 갤러리 미등록 항목을 `ygcolorchip` 계열로 축소. 위키에 그룹칩 그레이 타입 색 불일치 등록(#186 머지로 예약 해제). 미머지: bar/list-date 스펙·플랜(코드 미착수) |
| 2026-08-01 | `cb6fc1f` | Merge #180 (#169 group-list-background) | delta 3건(#173·#176·#180) — 전부 `feature/#169-group-list*` 브랜치, **G-001 그룹 목록 화면 골격**이 한 라운드에 들어왔다. parfait 스펙 없던 신규 화면 → **사후 스펙 1건 작성**(`implemented`·archive): g001-group-list. **드리프트 4건 처리**: ① `YGTopBarDefault` **삭제**·`YGTopBarEmpty(rightContent)` 슬롯 통합(#173) → ygtopbar 아카이브 스펙 as-built 정정 + design-system 인벤토리·노트 갱신, ② 미머지 bar-listdate 스펙/플랜의 "Default 드리프트 제거" Task가 **대상 소멸** → 프리뷰 칩 색 정정으로 축소 개정(칩 색·문구는 호출 화면 G-001이 이미 정본대로 씀, 남은 드리프트는 `YGTopBarPreview`의 `CherrySolid` 1건), ③ `core:util:android` `extension/`(`drawTooltipCornerTop`·`Builder.withStyle`)·`core:util:jvm` `model/DateFormat` 신설 → module-structure 행 갱신, ④ 엔트리 컨테이너 규약 이탈(`YGScaffold` 대신 `Box`+배경 이미지, `YGScaffold` 2겹 오버레이, `YGScreen` 미사용) → navigation-flow에 이탈 사례 마커. open-questions: 신규 3건(화면 컨테이너 규약 이탈·파르페/툴팁 정책 미결선·그리기 프리미티브 소유 갈림) + [2026-07-29] A-005 진입점·[2026-07-26] 문자열 리소스화 항목에 진행 마커. 화면은 **그룹 데이터 미결선**(`groupList` placeholder)이라 위키 [[무한-파르페-그리드]]·[[g-001-empty-툴팁]] 정책 대부분 미반영 상태를 스펙 "정책 대조" 표로 기록. 미머지: bar/list-date 스펙·플랜(코드 미착수) |
| 2026-08-01 | `39d0846` | Merge #174 (network-set-up) | delta 2건(#182·#174). **#182**(parfait 스펙 없던 신규 화면): C-101 커스텀 카메라 + C-101-confirm 사진 확인 화면 → **사후 스펙 1건 작성**(`implemented`·archive): c101-camera-picture-confirm. 단일 피드를 `GraphicsLayer`로 기록해 두 번 그리는 뷰파인더 블러(`CameraFeedLayer`)·촬영 이미지 역산 크롭(`CameraCrop`)·플래시·`NavKeyPictureConfirm`·권한 화면 재디자인·카메라/갤러리 `strings.xml`·`DateTextFormat`. **드리프트 다수**: ① `YGButtonType.radius` **삭제**·`YGButton` shape 인자 제거(화면 PR이 각짐 토큰 경유를 회귀시킴), ② `YGDate`에 `background` 중복 추가로 테두리 덮임, ③ 뷰파인더 상·하 간격이 위키 [[카메라-뷰파인더]] 정책과 역전(상단은 리터럴 `10.dp`), ④ 줌 UI 死코드화, ⑤ 권한 요청 경로 UI 부재, ⑥ 갤러리 빈 그래픽 상시 노출·문구 리터럴, ⑦ 확인 화면 이후 경로 TODO, ⑧ C-101 블러가 **ADR-0018이 기각한 자체 `GraphicsLayer`**로 머지(대상이 자기 자식이라 기각 근거와 다름, 극단값 검증 기록 없음). design-system(`YGButtonType`·`YGDate`·화면 적용)·navigation-flow(`NavKeyPictureConfirm` 인자 전달·카메라 innerPadding 예외)·module-structure(`DateTextFormat`)·ADR-0018(C-101 후속 박스) 갱신, open-questions **신규 8건** + [2026-07-30] 셔터 2구현 **해소됨** + [2026-07-26]·[2026-07-27] 진행 마커. **#174**(추적하던 스펙 `implemented`/플랜 `done`, 그간 미머지): 코드=문서 일치, 머지 시점 재대조에서 **패키지 배치만** 갈림(`service/model/{request,response}`·`data/model/{exception,qualifier}`·`EmptyTokenProvider` 파일 분리·타임아웃 3종) → 스펙/플랜 as-built 보강 후 양쪽 archive 이동(링크 `../`→`../../` 보정)·README 등록, data-layer "미머지" 블록 제거 + 패키지 절 추가, ADR-0017 응답 타입 경로 정정, open-questions 네트워크 3건 "미머지"→머지 표기. 미머지: bar/list-date(브랜치 미커밋)·s002 |
| 2026-08-04 | `63ed024` | Merge #188 (sync-component) | delta 2건(#189·#188). **#188**(직전 기준선의 미머지 추적 항목 — 브랜치 `feature/sync-component`가 커밋·머지됨): `YGListDate`·`YGFloatingBar` 4변형·`YGTopBarCanvas` 신설 + `YGTopBarContent` 확장(`contentPadding`·`trailingContent`) + 칩 프리셋 `CherrySubtle`→`GrayOutline` 교체·개명(소비처 6곳) + `YGTopBarEmpty` 날짜 파라미터 + Haze 배경 블러(`ygTopBarBackdrop`·`YGTopBarDefaults.BackdropBlurRadius`·`libs.versions.toml`·`ComposeConfig`) + 갤러리 2화면 등록. **코드=설계 일치(드리프트 0)**, 스펙이 시그니처를 안 적었던 자리에 as-built 3건만 확정(블러 분기 private modifier·`YGTopBarEmpty` 파라미터 순서·`Size44.getDp()`). 조치: 스펙 `implemented`·플랜 `done` + 양쪽 archive 이동(링크 `../`→`../../` 보정)·README 등록, design-system 인벤토리에 신규 2패키지 + `YGTopBar` 4변형·haze 의존 절 추가, ygtopbar 아카이브 스펙 as-built 정정(로고 placeholder·프리뷰 칩 색 **해소**), ADR-0018 머지 마커. **#189**(chore): G-001 라벨 3종을 `feature/groups/list/impl` `strings.xml`로 추출 — g001 스펙의 "문자열 리소스화 TODO" **해소**, open-q [2026-07-26] 진행 마커. open-questions: [2026-07-31] `YGChipColorIndicator` 용도 **부분 해소**(첫 사용처가 `YGListDate`로 확정, 위키 C-201 Chip-Indicator와 별개라던 판단이 틀렸음), 갤러리 미등록·블러 관용구 항목에 머지 마커, **신규 5건**(우측 슬롯 2종 측정 의미·`YGFloatingBar` 사용처 0건·날짜 영문 표기 로케일·블러 실화면 미배선/API31 폴백·`YGListDate` 접근성). 미머지: s002·clearfocusontap·network-envelope·data-api-service-layer |
| 2026-08-04 | `ada2774` | Merge #192 (#86 account-info) | delta 1건(#192, 직전 기준선의 미머지 추적 2건 `s002-account-info`·`clearfocusontap-modifier`를 한 번에 닫은 브랜치). S-002 계정 정보 화면(Route/Screen/VM + `strings.xml` 2종) + `Modifier.clearFocusOnTap()`(`core:util:android` `focus/` 신설) + S-001 Danger Zone 이관 실물화(`YGDangerZone`+`YGActionItem` 2개·Intent 2종 stub·`ProfileCard` `YGLabel` 교체). **코드=설계 일치**, as-built 3건만 확정(Screen 상태 파라미터명 `state`·Contents `Column`의 `spacedBy(gap8)`이 자식 1개라 실효 없음·프리뷰 에러 케이스가 `core:ui` 리소스 ID 직접 주입). **문서 전제 오류 1건 정정**: 여러 문서가 "2026-07-23 `YGScreen`에 포커스 결선 도입 → 2026-08-03 철회"로 적었으나 **그 결선은 develop에 도달한 적이 없다**(도입·철회 둘 다 이 브랜치 내부, PR 변경 파일에 `YGScreen.kt` 없음). 반대로 결선용 `clickableYGNoRipple` + `clickableYGThrottle(indications: List<Indication>?)` nullable화는 철회 후에도 남아 **사용처 0 공개 API로 develop 신설**. 또 문서가 선반영해 뒀던 `YGTextFieldImpl` clear 게이팅(`isFocused \|\| isError`)이 이제야 머지 — 공용 Impl이라 `GroupCreateScreen`·`GroupNickNameScreen`에도 전파(회귀 확인 기록 없음). 조치: s002 스펙 `implemented`·플랜 `done`(frontmatter 자체가 없어 신규 작성) + clearfocusontap 스펙 `implemented`, 3건 archive 이동(링크 `../`→`../../` 보정)·README 등록. s001·ygscreen-scaffold·ygtextfield·ygtextfield-clear-iconbutton·clickableyg-throttle·clickableyg-ripple-variants 아카이브 스펙 as-built 정정, module-structure(`focus/` 패키지)·design-system(clickable 변형 5종·결선 전제 정정) 갱신. open-questions: [2026-08-03] `clickableYGNoRipple`·미적용 3종·[2026-07-29] ADR-0016 3사례에 머지 마커, **신규 2건**(clear 게이팅 비참여 화면 2곳 미검증·S-002가 저장 경로 없이 머지). 미머지: network-envelope·data-api-service-layer |
| 2026-08-04 | `23e9357` | Merge #191 (#171 gallery-screen) | delta 1건(#191). parfait 스펙 없던 C-102 커스텀 갤러리 화면이 실물로 채워짐 → **사후 스펙 1건 작성**(`implemented`·archive): c102-custom-gallery-picker. 권한 5단계 분기·최근/날짜 그룹 3열 그리드·빈 상태·PARTIAL 하단 "사진 재선택"·가이드 토스트 1회. **구조 변경 2건**: ① 선택 결과가 `LocalResultEventBus` 반환 → **확인 화면으로 `goTo`**(카메라와 화면 공유, `NavKeyPictureConfirm`에 `PictureConfirmSource` 인자 신설, `feature/gallery/impl`→`feature/camera/api` 의존 추가), ② 날짜 키 `String`→`LocalDate`(`GalleryImageGroup`·`GalleryRepository`·UseCase 2종)로 표시 포맷이 data에서 화면(`DateTextFormat`)으로 이동. **정책 정합 개선**: 확인 화면 이미지 노출이 위키 [[이미지-렌더링-정책]] Case A/B 비율 분기로 정정(이전엔 항상 테두리). 03시 하루 경계는 기존대로 일치. 조치: c101 아카이브 스펙 as-built 갱신(확인 화면 절 신설·파일 구성·갤러리 지적 정리), navigation-flow(공유 화면 출처 인자 관용구 + 반환 경로 제거 시 호출자 `ResultEffect` 경고), data-layer(표시 포맷 비보유 노트). open-questions: [2026-08-01] 갤러리 빈 상태 **부분 해소**(그래픽 분기·문구 리소스화 — 인디케이터 흰색 잔존), 권한 경로·확인 화면 이후 경로·[2026-07-26] 문자열·[2026-08-04] 날짜 영문에 마커, **신규 4건**(호출 화면 `ResultEffect` 死경로·그리드 셀 `clickableYG` 미사용·死코드 2건·가이드 토스트 문구 모듈 중복). 미머지: network-envelope·data-api-service-layer |
| 2026-07-22 | `526f4c9` | Merge #159 (sync-design-system-260719) | delta 머지 2건(#163, #159). **#159**(추적하던 미머지 브랜치): YGDangerZone 점선 재설계(`dashedBorder()`·`YGHorizontalDashedDivider` 신규)·`radius.none` 토큰 신설·YGTextField(none/white)·YGInviteCard(none)·YGButtonType SmallSquare(none) 각짐 sync. 코드=설계 완전 일치, in-progress 스펙 2건([ygdangerzone-dashed]·[radius-none-sync]) `implemented`·archive 이동, 구 solid ygdangerzone `superseded`. **#163**: `:app-preview` 컴포넌트 갤러리(카탈로그 5카테고리·NavKey17·showcase17·@IntoSet 배선) — 선작성 spec/plan과 구조 일치, `implemented`/`done`·archive 이동. design-system(radius none·dashed 프리미티브·YGDangerZone 재설계·textfield/invitecard/button sync 마커) 갱신. open-questions: ProfileCard 종속 해소(코드 교체만 잔존)·YGDangerZone 피그마 델타(gap-5·고정폭) [2026-07-22] 신규. 미머지: 없음 |
| 2026-08-04 | `90c651e` | Merge #190 (set-up-backend-api) | delta 1건(#190, 직전 기준선의 미머지 추적 항목 `network-envelope-token-storage`가 커밋·머지됨). 서버 계약 정합 + 암호화 토큰 저장: `ApiResponse` 5필드 정합(`isSuccess`·`SUCCESS_CODE` 제거), `SafeApiCall.kt`→**`ApiCaller` 클래스 승격**(3진입점 + `HttpException` 바디 envelope 파싱 + 폴백), `ApiException.Business`에 `statusCode`·`errorDetail`, `CryptoManager`(Keystore AES/GCM)+`TokenStore`/`EncryptedTokenStore`(DataStore)+`TokenStoreTokenProvider`(`EmptyTokenProvider` 삭제), DI 2곳 배선. **코드=설계 일치**, 스펙이 예고한 as-built 2건 확정(`read()`의 `runCatching`이 DataStore 읽기까지 포함). **드리프트 3건 처리**: ① `@NoAuth` 스킵이 **코드 리뷰 반영으로 갈림 확정** — early return 제거, `getToken()`은 항상 호출하고 헤더 부착만 게이팅(결과 동일·비용만 증가) → 스펙·ADR-0017·data-layer 세 곳의 "토큰 조회 생략" 정정, ② **R8 keep 규칙이 무효 자리에 머지** — `data/proguard-rules.pro`에 넣었는데 develop의 `setConfigAndroidLibrary`가 `consumerProguardFiles`·`proguardFiles` 어느 쪽도 등록하지 않고(`consumerProguardFiles` develop 전체 0건) `data/consumer-rules.pro`는 빈 파일 → ADR-0017 R8 절 정정·open-q ② 재개, ③ data-layer의 "미머지" 블록이 이제 `data-api-service-layer` 범위만 가리키도록 재구성(`transform` 오버로드·`PolicyService`·`@SerialName` 규약은 여전히 미머지, develop 진입점은 셋). **스펙 범위 밖 동반 변경 2건**: 루트 `http/` IntelliJ HTTP Client 요청 모음 7파일 + `.gitignore`. 조치: 스펙 `implemented`·플랜 `done`(Step 7 실기기 왕복만 미수행·이월) + 양쪽 archive 이동(링크 `../`→`../../` 보정, 기존 깨진 ADR-0008 링크 1건 동반 수정)·README 등록, ADR-0017 as-built 4블록 "미머지"→머지 표기, ADR-0019 머지 마커 + `read()` 범위 as-built. **api 5표면**: `conventions.md` "Android 불일치" 표 **3행 전량 제거**(해소), `api/README.md` `http/` 파일 목록 정정(`policy.http` 없음·`_reset.http` 추가)·불일치 요약 갱신, 도메인 문서 4건 Android 매핑 절에 "인프라는 머지·도메인 표면은 미머지" 마커(`android_status`·엔드포인트 Android 열·`verified`는 불변 — 서버 계약 대조일이라 이 스킬이 안 건드림). open-questions: **해소 5건**(envelope 불일치·성공 코드 판정·TokenProvider 부재·`@NoAuth` 스킵 갈림·`runBlocking` 리뷰), 마커 2건(실기기 왕복·키 유실), **② 재개 1건**(R8), **신규 2건**(`http/`↔`api/` 계약 이중 관리·`@NoAuth` 사용처 0건 死코드). 미머지: data-api-service-layer |
| 2026-08-06 | `ebad098` | Merge #197 (sync-api-service) | delta 1건(#197, 직전 기준선의 미머지 추적 항목 `data-api-service-layer`가 머지됨 — 이로써 미머지 추적 항목 0). 14 엔드포인트 표면 전량 develop 진입: Service 4(`AuthService`·`PolicyService`·`ParfaitGroupService`·`ParfaitService`) + request/response DTO 21 + remote DataSource 4쌍 + `VOMapper` 3 + domain VO/value class 21(`auth`·`group`·`id`·`policy` 하위 패키지) + `ApiCaller.safeApiCall(block, transform)` 오버로드 + DI 8바인딩 + `Temp*` 6파일 삭제. **코드=스펙 일치**(Service 함수명 규칙 14/14·`@NoAuth` 4곳·`@SerialName` 전 프로퍼티·`logout`만 `safeApiCallNoContent`·VO 미생성 3곳). **드리프트 3건**: ① 스펙이 지시한 mapper 타임존 근거 주석(`recentImageUploadedAt` Asia/Seoul)이 코드에 없음, ② `KakaoLoginVO`↔`KakaoLoginResult` 상호 참조 KDoc 미작성, ③ `domain/model/`이 신규만 하위 패키지·기존 8선언은 루트 평면으로 혼재. **R8 해소**: keep 규칙이 `data/consumer-rules.pro`로 이관되고 `setConfigAndroidLibrary`가 `consumerProguardFiles("consumer-rules.pro")`를 등록 — develop에서 무효였던 배치가 정상화(전 라이브러리 모듈에 `consumer-rules.pro` 실재 확인). 스펙 범위 밖 동반 변경: `http/policy.http`·`_reset.http` 신설 등 요청 모음 현행화. 조치: 아카이브 스펙 as-built 3건 추가·`verified` 갱신, data-layer "미머지" 블록 제거 + 진입점 4개 마커 정리, ADR-0017 R8·Temp 예시·검증 서술 정정. **api 5표면**: `README.md` 도메인 표 Android 열 4행 `미구현`→`구현됨` + `http/` 파일 목록 정정(`policy.http` 존재), 도메인 문서 4건 Android 매핑 절 "브랜치까지·미머지"→머지 표기, `conventions.md` 불일치 표 잔여 0건 유지(요청 0건 사유를 "Service 미머지"→"소비처 없음"으로 정정), `android_status`·`verified`는 불변. open-questions: **해소 3건**(R8 ② 재개분·`@SerialName` 필요·`@NoAuth` 사용처 0건), 마커 3건, **신규 3건**(표면 소비처 0건·근거 주석/KDoc 미이행·`domain/model` 배치 혼재). 미머지: 없음 |
| 2026-08-07 | `6892503` | Merge #194 (#169 group-list-item-reapply) | delta 1건(#194 — #187이 revert 후 reapply된 브랜치). G-001 **토핑 배치 라운드**: `route/component/ToppingLayout.kt` 신설(지그재그 커스텀 `Layout` — `index % 2` 좌/우, `overlap`·`alternateOffsetY`·`contentPadding` 파라미터) + `GroupListParfaitLayout`을 `route/`→`route/component/`로 옮기며 전면 재작성(크림 개수 `Animatable` 애니메이션·`placeRelativeWithLayer` 알파·첫 프레임 `snapTo`·덮음 판정 기준을 접시 제외 `creamTop + middleHeight`로 변경) + `GroupListUiState.groupList`가 `List<String>`→`List<MockToppingGroup>`(mock 4건이 **기본값**) + `YGToppingGroup` 첫 소비 + `YGTopBarEmpty(windowInsets)`·`YGTopBarDefaults.windowInsets` 신설(상단 인셋을 탑바가 흡수, Route는 `YGScaffold(contentWindowInsets = systemBars.only(Horizontal + Bottom))`). **정책 대조 결과 좌표는 일치** — 좌·우 인셋 4(`padding2`)·같은 side 갭 -12(`overlap`)·`Right = Left + 86`(`alternateOffsetY`)·저개수 N≤3 +12(`SPECIAL_RULE_THRESHOLD`)가 위키 [[G-001-무한파르페-간격-정책-v0.3]]과 전부 맞는다. **드리프트 5건**: ① 변형 타입이 정책의 랜덤 재부여 아닌 index 순환(코드에 Todo), ② mock 4건이 UiState 기본값(URL 1건 스킴 중복으로 상시 실패 폴백), ③ 토핑이 그려지는데 `ClickTopping`·`onClickTopping`·`NavigateToCanvas`가 死경로(호출자가 `clickableYG` 미적용), ④ 인셋 관용구 3형태 공존 + `windowInsets`가 `Empty` 변형에만, ⑤ `animateToppingPlacement` 사용처 0건 + 호출부 key 미이행(같은 파일 3심볼이 `public`). 조치: g001 아카이브 스펙 as-built 전면 갱신(범위·API·인셋 절 신설·파르페 레이아웃 재작성·토핑 배치 절 신설·정책 대조 표 전면 교체·파일 구성)·README 행 갱신, design-system(ygtopbar 인셋·인벤토리·`YGToppingGroup` 첫 소비처)·navigation-flow(인셋 소유 사례) 갱신, ygtopbar·bar-listdate 아카이브 스펙 as-built(`windowInsets`). open-questions: [2026-08-01] 파르페·툴팁 ① **부분 해소**(②③④ 잔존), **신규 5건**. 미머지: 없음 |
| 2026-08-09 | `25f7c17` | Merge #219 (#215 test-environment) | delta 1건(#219). **테스트 기반 구조가 처음 develop에 진입** — 컨벤션 플러그인 3종(`parfait-test-unit`·`-android`·`-compose`) + `TestConfig.kt`의 `setConfigTestXxx()` 3함수 + `:core:testing`(`MainDispatcherRule` 단일 파일) + 유닛 테스트 8파일(`CharExtension`·`DateFormat`·`CheckNameValidUseCase`·`DayWindow`·`PolicyVOMapper`·`PolicyRemoteDataSourceImpl`·`AuthInterceptor`·`ApiCaller`) + 계측 스모크 2건 + CI 워크플로 2종. 프로덕션 변경은 `DayWindow.current(timeZone, clock)` 파라미터 1건. **CI 구조 개편**: `test.yml` 신설 + `ktlint.yml`에서 `--info test` 스텝 제거, 두 워크플로가 공유하던 셋업을 composite action 2개(`setup-android-build`·`restore-app-secrets`)로 분리하고 `gradle/actions/setup-gradle@v4`로 전환 — 수동 `actions/cache`·`Cleanup Gradle Cache`·`chmod +x gradlew` 3스텝이 사라졌다. **드리프트 1건**: 스펙 "적용 대상" 표가 `parfait-test-android`를 `core:util:android`만으로 적었는데 `core:designsystem`에도 적용된다(`parfait-test-compose`가 계측 러너를 대체하지 않으므로 필연). 조치: 표 정정 + as-built 절 신설(일치 항목·규약 grep 3건 0건·`lintDebug` 경고 없음 확인), spec `implemented`·plan 완료 표기 후 양쪽 `archive/` 이동(상대링크 `../`→`../../` 보정, README 표 이동). open-questions **신규 2건**(검증 안 된 표면 3건 — `MainDispatcherRule` 사용처 0·계측 CI 미실행·`core:util:android` unit 0개 / Repository Fake 위치 미결 선택지 3), **부분 해소 1건**([2026-08-02] Keystore 항목의 "코드베이스에 test가 없다" 전제 해소 — 재현 수단 없음은 유지). module-structure에 `:core:testing` 등재(테스트 소스셋 전용이라 의존 방향 그래프 밖) + 프로덕션 참조 금지 규칙. 미머지: 없음 |
| 2026-08-10 | `0a1f688` | Merge #220 (login-term-agree-navigation) | delta 1건(#220). **앱 진입 체인이 처음 끝까지 이어짐** — `LoginRoute`가 `NavKeyGroupHome` 대신 `NavKeyTermAgree`로 가고, `TermAgreeRoute`의 `NavigateToNext` stub이 `clearBackStack()` + `goTo(NavKeyGroupList)`로 결선(`Splash → Login → TermAgree → GroupList`). 이로써 **도달 불가였던 화면 2건**(TermAgree·G-001 그룹 목록)이 진입 경로를 얻었다. 동시에 `feature/groups/home/{api,impl}` **모듈 2개 삭제**(`NavKeyGroupHome`·`GroupHomeRoute` — `ResultEventBus` 왕복 시연용 임시 화면)와 짝이던 `LoginRoute`의 `ResultEffect<String>` Toast 제거, `settings.gradle.kts`·`app`·`core:navigation` 의존 정리. 의존은 규약대로 `:api`만(`login/impl`→`intro/api`, `intro/impl`→`groups/list/api`). Kotlin 로직 변경은 Route 2개뿐. **드리프트 4건 처리**: ① module-structure 그룹 모듈 목록에서 `home` 제거 + 삭제 사유 노트, ② navigation-flow의 `NavKeyGroupHome` 언급 2곳(NavKey 예시·인자 있는 목적지 예시) 제거, ③ ADR-0002의 feature 간 의존 예시가 삭제된 모듈을 가리켜 현행 2쌍으로 교체, ④ a005 아카이브 스펙의 인자 있는 NavKey 선례 주석에서 `NavKeyGroupHome` 교체. 조치: navigation-flow에 **"앱 진입 체인" 절 신설**(시작 목적지 `NavKeySplash`·`clearBackStack()`+`goTo` 리셋 관용구 2곳·그룹 목록에서 뒤로가기 no-op), intro-term-agree 아카이브 스펙 as-built(다음 네비게이션 TODO **해소**, 진입/이탈 경로·파일 구성)·specs README 행 갱신, g001 아카이브 스펙에 진입 경로 결선 마커. open-questions: [2026-08-04] 死 `ResultEffect`·[2026-08-06] 표면 소비처 0건 ①·[2026-08-02] 토큰 왕복 미검증에 마커, **신규 2건**(온보딩 체인이 화면 전이만 결선 — 서버 인증·`newUser` 분기·동의 저장 전무 / `ResultEventBus` 데코레이터 존치 여부). 미머지: 없음 |
| 2026-08-11 | `7f38c90` | Merge #227 (build-action-cache) | delta 2건(#222·#227). **#222**(parfait 스펙 없던 feature 라운드): G-001에 실패 화면·pull-to-refresh·A-005 이동이 들어왔다 — `GroupListErrorScreen` 신설(`isError` 분기, Route가 같은 `YGScaffold` 안에서 화면을 가름), 목록·에러가 `GroupListTopBar`(칩·툴팁 노출을 `onClickAddGroup` null 여부로 가름)와 `GroupListPullToRefreshBox`(M3 `PullToRefreshBox` 래퍼 + `graphicsLayer`로 콘텐츠도 함께 내림)를 공유, 본문 중복 날짜 헤더(`YGDate`) 제거 → **G-001은 더 이상 `YGDate` 소비처가 아니다**, 추가 오버레이 칩을 `YGIconButtonSize.SIZE_44` 높이 `Box`로 감싸 탑바 칩과 정렬, `goTo(NavKeyGroupCreate(nickName = uiState.nickName))` 결선. **드리프트 4건 처리**: ① g001 아카이브 스펙 as-built 전면 갱신(범위·UiState 3필드·에러/새로고침 절 신설·본문·파일 구성·정책 대조 표 3행 교체), ② navigation-flow 체크리스트 6번 선례를 "#222에서 약 2주 만에 호출자 확보"로 갱신 + 인자 값 출처 문단 신설, ③ specs README g001 행 갱신, ④ index 상태 1줄 갱신. **정책 대조**: 위키 [[무한-파르페-그리드]]의 "실패 = Error + 새로고침"은 방향 일치이나 **`isError`를 세우는 코드가 프리뷰 밖에 없어 도달 불가**이고, "자체 로딩 그래픽" 요구는 M3 기본 인디케이터라 불일치. 툴팁 0건 조건은 여전히 미이행(에러 화면에서만 빠짐). **#227**(선작성 스펙·플랜이 있던 브랜치, 직전 라운드에서 push 보류였던 건): `gradle-cache-seed.yml` 신설 + `gradle.properties`에 `org.gradle.caching=true` 한 줄 — 코드=설계 일치. 머지 후 관측을 런 ID로 고정(seed `31402546861` `cache-read-only: false`·`Caching Gradle state`, 후속 PR `31403327032`에서 `Gradle User Home cache not found` 소멸·`368 from cache`·6m16s → 2m45s)해 스펙에 표로 남기고, spec `implemented`·plan 전 Step 완료 + 양쪽 archive 이동(링크 `../`→`../../` 보정)·README 2건 이동 등록. open-questions: [2026-07-29] A-005 진입 **부분 해소**(② 결선, ① 진입 관계·인자 값 잔존), [2026-08-07] mock 항목에 `nickName`·mock 새로고침 마커, **신규 4건**(실패 화면 도달 불가·로딩 그래픽 정책 불일치·CI 빌드 성능 후속 2축·Actions Node 20 deprecation). 미머지: 없음 |
| 2026-08-11 | `963ffed` | Merge #218 (login-a002) | delta 1건(#218). **A-002 로그인 화면이 placeholder에서 실물이 됐다** — 온보딩 자리의 회색 박스 + "[ 일러스트 ]" 문구가 밀도별 PNG 3장(`image_onboarding_1`~`_3`, **`core:designsystem` `res/drawable*`**)으로 교체되고, 페이저 구성이 뒤집혀 **인디케이터가 페이저 위**로 올라갔다. `OnboardingPage`에서 `title`이 사라지고 `painterResourceId`가 nullable→non-null. `feature/login/impl` `strings.xml` 신설로 온보딩 설명 3·카카오 라벨/접근성 2가 `stringResource`로 갔고, `KakaoSignInButton`이 `RoundedCornerShape` 리터럴→`radius.none`·리터럴 패딩→`padding.*`·라벨→`body.b01SB`로 토큰화됐다(버튼 자체는 외부 가이드 색 때문에 여전히 feature 로컬 Material3 `Button`). Kotlin 로직 변경은 없다 — 서버 인증·토큰 저장은 그대로 빠져 있고 카카오 실패·취소는 로그만 남긴다. parfait 스펙 없던 화면 → **사후 스펙 1건 작성**(`implemented`·archive): a002-login-onboarding. **드리프트 7건 기록**: ① 애플 로그인 잔여 심볼 3종(`AppleDesignGuideColors`·`icon_logo_apple.xml`·strings 2건)이 **사용처 0**으로 머지 — 전날 확정한 "Android 애플 미사용"(OQ-P-117 ②)과 어긋나는 잔여물, ② 치수 리터럴 4종(코드 주석이 "gap 없음"으로 스케일 공백 자인), ③ 에셋 소유가 DS(일러스트)와 feature(로고)로 갈리고 밀도 버킷도 이미지마다 다름, ④ 프리뷰 provider가 세 번째 장에서 실화면과 다른 이미지·줄바꿈, ⑤ 인디케이터 색 리터럴(기존), ⑥ 주입한 `contentColor`가 `Text` 명시 색에 덮여 死, ⑦ `painterResourceId`에 `@DrawableRes` 없음. **정책 대조는 대상 부재** — 위키에 A-002 온보딩 슬라이드 정책이 없어 문구·구성을 코드가 먼저 확정(G-001 에러 문구 선례). 조치: design-system(색 트리 `AppleDesignGuideColors`·`res/drawable*` 에셋 소유 노트·A-002 화면 적용 절)·navigation-flow(앱 진입 체인 마커) 갱신, specs README 아카이브 등록, index 상태·기준선 줄 갱신. open-questions: [2026-07-26] 문자열 리소스화 **A-002 준수 마커**, [2026-08-10] 온보딩 체인·[2026-08-11] 서버 delta ②에 마커, **신규 4건**(애플 잔여 심볼·치수 리터럴·에셋 소유·문구 정책 부재+프리뷰 드리프트). 미머지: 없음 |
| 2026-08-12 | `4af64ba` | Merge #199 (#193 canvas-default-screen) | delta 1건(#199). **C-001 캔버스 메인이 placeholder에서 실물이 됐다** — 본문이 `Text` + Material3 `Button` 2개였던 `CanvasImageAddScreen`이 `YGTopBarCanvas` + `YGCanvas`(+`YGCanvasMenu`) 조립으로 교체되고, `feature/groups/canvas/impl` `strings.xml`이 신설됐다. 화면 라운드가 **디자인시스템 3파일을 함께 바꿨다**: ① `YGCanvas`가 **반응형 배치를 흡수**(`BoxWithConstraints` + private `calculateCanvasLayoutMetrics` — 좌우 `padding7`·상하 최소 `padding5`·세로 중앙·세로 부족 시 축소, 전제가 `fillMaxWidth`→`fillMaxSize`), ② `onDimClick` 신설(Dim 구현도 소비 전용 `pointerInput`→`clickable(indication = null)`), ③ 인접 테두리 `spacedBy(-1.dp)` 겹침(접합선 2dp→1dp, 총높이 `-1.dp` 보정), ④ `Modifier.ygBackgroundDotGrid()` 신설(`component/ygbackgrounddotgrid/`). `YGTopBarCanvas`의 첫 소비처이기도 하다(멤버 칩 5 + `NametagChipPlus` `+N`). parfait 스펙 없던 화면 → **사후 스펙 1건 작성**(`implemented`·archive): c001-canvas-main. **정책 대조: 좌표·비율·gap·점 스펙 전량 일치**(좌우 20·상하 최소 12·Area만 9:16·메뉴 `Size44` 가산·점 지름 2/`Gray100`/간격 20), **불일치 2건** — 날짜가 `todayIn`이라 [[캔버스-마감-스케줄]] 03시 KST 경계 미적용(`DayWindow`가 이미 있고 C-102가 쓴다), Dot Grid가 `YGScaffold` `innerPadding` 안쪽이라 "화면 전체 뒤"·앵커 (0,0) 미이행. **드리프트 7건 기록**: 진입 경로 0건(도달 불가 — G-001 `NavigateToCanvas`가 `// Todo`)·화면 내 콜백 3종 빈 람다·mock을 VM 로드 함수에 박음 + 프리뷰 provider와 값 불일치·`isEmpty = true` 상수(토핑 슬롯 미사용)·`MAX_VISIBLE_MEMBER_CHIPS` 死상수·`OnClickCanvas`/`NavigateToCanvas`가 갤러리로 감·`YGCanvas`가 화면 배치까지 앎. 조치: 캔버스 컴포넌트 아카이브 스펙 as-built 3조항 정정(+`onDimClick`·치수 표·접합선), design-system(트리·인벤토리·캔버스 5종 절·`YGTopBar` Canvas 첫 소비처·프리미티브 소유 4번째 자리)·navigation-flow(체크리스트 6번 사례·인셋 형태 선택) 갱신, specs README 아카이브 등록, index 상태·기준선 줄 갱신. open-questions: **해소 1건**([2026-08-01] 임시 구현체 잔존), **부분 해소 1건**([2026-08-01] Dim 탭 닫기 ① 해소·② 잔존), 마커 4건([2026-07-26] 문자열 캔버스 준수·[2026-08-01] 프리미티브 소유·[2026-08-04] 날짜 영문 3번째 소비처·[2026-08-07] 인셋), **신규 5건**. 미머지: 없음 |
| 2026-08-12 | `cadab433` | Merge #230 (sync-backend-api-260810) | delta 1건(#230). **서버 계약 표면이 닫혔다** — 추적하던 선작성 스펙·플랜 2쌍(`image-api-service-layer`·`member-parfait-image-api-service-layer`)이 **한 브랜치·한 PR로 합쳐져** 머지됐다(#229는 별도로 나가지 않음). image 2 + member 2 + parfait-image 2 = 6 엔드포인트가 develop에 진입해 Service 4→**7**, remote DataSource 4→**7쌍**, 표면 14→**20**이 됐고 **Android가 쓰기로 한 전량(서버 21 − 애플 1)을 덮는다**. 신규: Service 3 + request/response DTO 9(+`PlacedByResponse`) + DataSource 3쌍 + `VOMapper` 3 + domain 16(`id/` 4·`image/` 4·`member/` 3·`topping/` 5) + DI 6바인딩 + 유닛 테스트 3건. **코드=설계 일치(드리프트 0)** — Service 함수명 6/6·`@NoAuth` 미부착 6/6·전 프로퍼티 `@SerialName`·POST/PATCH 비대칭·sealed `ToppingBorder`가 본문 그대로. **as-built 확정 4건**: `ToppingPlacerVO`가 `PlacedToppingVO.kt` 안·요청 방향 변환 `toPlaceRequest`도 같은 `VOMapper.kt`·`ImageVOMapperTest`는 develop에 **없음**(매퍼 단독 테스트 금지 규약으로 케이스 이관 후 삭제, 브랜치 안에서 생겼다 사라져 머지 diff에 안 보임)·PATCH의 `null` 전송 원인이 `explicitNulls`가 아니라 **`encodeDefaults = true`**(5필드가 전부 `= null` 기본값이라서). `PolicyVOMapperTest`도 함께 삭제돼 develop `*VOMapperTest` **0건**. **드리프트 1건**: `http/README.md`의 판별자 키 서술만 `newUser`→`isNewUser`로 정정되고 **같은 디렉토리 `http/auth.http`는 여전히 `newUser`를 가르치고 응답 핸들러도 그 키로 분기**한다(앱 DTO도 그대로) — 이중 관리가 `api/`↔`http/` 사이가 아니라 `http/` **안에서** 갈린 첫 사례. 조치: 스펙 2건 `implemented`·플랜 2건 완료 표기 + 양쪽 archive 이동(링크 `../`→`../../` 보정, 플랜은 frontmatter가 없는 최근 관례대로 상단 머지 블록 신설)·README 4행 아카이브 등록. **api 5표면**: `README.md` 도메인 표 Android 열 3행 `미구현`→`구현됨` + 표면 개수 14→20 + `http/` 파일 목록·커버리지 문단, 세 도메인 문서 `android_status: none`→`partial`·엔드포인트 표 Android 열 6칸·**Android 매핑 절 전면 신설**(심볼 표·도메인별 결정·소비처 0 명시), `conventions.md` 불일치 표 1행 유지 + `http/` 내부 모순 마커 + 개수 문단 재작성(`verified`·서버 계약 절은 불변 — 서버 대조일이라 이 스킬이 안 건드림). `data-layer` 반영 범위·`domain/model` 혼재 서술·요청 방향 매퍼 규약 신설. open-questions: **해소 2건**(서버가 7 엔드포인트 앞섰다 · `http/` 요청 모음 공백), 마커 **9건**(소비처 0건 표면 14→20·`domain/model` 배치·`@NoAuth` `Invocation` 태그가 S3 PUT 선행 조건으로 승격·이중 관리·판별자 키 ④ 신설·S-002 저장 경로·`MainDispatcherRule` 여전히 0·서버 소관 4건), **신규 2건**(`safeApiCallWithoutData` 死코드 확정·`Business.statusCode`를 KDoc이 약속하는데 테스트가 그 경로를 안 탐). 미머지: 없음 |
| 2026-08-12 | `45de9413` | Merge #224 (group-create-enter-flow) | delta 1건(#224). **그룹 생성·참여 두 갈래가 목록으로 되돌아오며 닫혔다** — A-005 생성 확인과 A-004 초대코드 확인이 각각 `YGModalPopup`을 게이트로 두고, 확인 시 `CreateGroupUseCase`/`EnterGroupUseCase`(둘 다 신설 mock)를 거쳐 `Navigator.goToSingleClearTop(NavKeyGroupList)`(신설)로 복귀한다. 참여는 A-004 모달 → S-102 닉네임 → 목록 순. `InviteCodeResult`에 `groupName` 추가(모달 제목용, UseCase 안 리터럴 mock), `feature/groups/enter/impl` → `feature/groups/list/api` 의존 추가(규약 준수), 모달 문구 7종 `strings.xml`. Kotlin 로직 변경은 Route 2·VM 3·Navigator 1. **`YGModalPopup`의 첫 실화면 소비처**(그전엔 `:app-preview` 갤러리뿐). **사후 스펙 1건 작성**(`implemented`·archive): a004-group-invite-code — 화면은 #156부터 있었으나 이번에 플로우 첫 화면이 되며 스펙 대상이 됐고, 그 과정에서 **parfait 전반의 화면 ID 오류를 정정**했다(초대코드 화면은 위키 정본상 G-002가 아니라 **A-004**, G-002는 [[기능정의서-v6]]에서 삭제된 별개 화면). **드리프트 6건 처리**: ① navigation-flow의 Navigator 함수 목록에 `goToSingleClearTop` 추가 + **"그룹 생성·참여 플로우" 절 신설**(백스택 리셋 관용구가 2형태로 갈린 점 포함), ② a005 스펙 as-built(모달·`isCreating`·복귀 결선, "다음 화면 네비게이션"·"진입 경로" TODO 해소), ③ s102 스펙 as-built(`EnterGroupUseCase`·`isEntering`·복귀, 후보였던 A-005가 목적지가 **아님**을 명시), ④ design-system `YGModalPopup` 노트(첫 실화면 소비처 + 좌우 배치가 미머지 Danger Zone 스펙과 갈림 + `isEnabledButton` 단일 플래그 실사례), ⑤ 그 Danger Zone 스펙에 반대 배치 마커, ⑥ G-002 호칭 정정 4곳(g001·s102·a005 스펙, open-q 해소 메모). **정책 대조**: 좌표·문구가 아니라 **흐름이 어긋난다** — 위키 [[기능정의서-v6]]은 A-004·A-005 다음 단계를 **C-001 직접 진입**으로 재배선했는데 코드는 그룹 목록으로 돌아온다. 초대 코드 6자리는 정책 문서 자체가 없다. open-questions: 마커 5건([2026-07-29] A-005 진입 ① 후보 흐름 기각·[2026-07-26] ② domain 표시문자열 확대·[2026-08-06] 첫 소비처가 mock으로 채워짐·[2026-08-07] mock이 왕복을 닫음·[2026-08-10] 같은 패턴 반복), **신규 4건**(mock UseCase 3종·복귀 목적지 정책 불일치·백스택 리셋 관용구 2형태·모달 문구/좌우 배치 근거 부재). 미머지: 없음 |
| 2026-08-13 | `0521e0bc` | Merge #225 (setting pop-up) | delta 2건(#223·#225) — **둘 다 선작성 스펙·플랜이 있던 브랜치**가 같은 날 연이어 머지된 라운드. **#223**(S-101 그룹 사이드 메뉴, 이슈 #211): 닉네임 조회 + `isEditing` 인라인 편집(포커스 진입·입력 시점 검증·`확인`/키보드 엔터 확정·배경 탭 취소) · 그룹원 목록 · 초대 코드 카드(클립보드에 **초대 문구 2줄**, `복사됨` 2초 자동 복귀 + 연타 시 타이머 리셋) · Danger Zone 2항목 stub. State 3필드를 **도메인 VO**(`GroupName`·`GroupNickname`·`InviteCode`)로 들인 저장소 첫 사례이고, 확인 버튼은 오버레이가 아니라 스크롤 영역의 **형제**(`weight(1f)`)이며 entry가 `consumeWindowInsets(innerPadding)`로 인셋 이중 계산을 막는다. **동반 변경 3건**: `YGColorChipType` 14종 → **12종 + Plus** 정렬(11번이 3번과 완전 중복이라 뒤가 밀려 있었고 9번 글자색이 테두리색과 동일) · `YGTextFormField`/`YGTextFieldImpl`에 `keyboardOptions`·`keyboardActions` 노출 + **최소 높이 48 고정** · **ADR-0016 원안 수렴**(유효성 표시 매핑을 VM 4곳에서 `core:ui` `text/NameValidResultUiText.kt`의 `NameFieldType`+`toStringResource`로 이관, `core:ui`→`:domain` 의존 추가, State 필드 `errorMessageResId`/`groupNameErrorTextResId` → `nicknameError`/`groupNameError`). **#225**(Danger Zone 확인 팝업 3종): `AppSettingState.isWithdrawDialogVisible` Boolean + `GroupSettingUiState.visibleDialog: GroupSettingDialog?` 단일 nullable enum, 세 확인 핸들러에 **멱등 가드**(교차 확인까지 차단), 파괴적 액션=좌 Secondary / 취소=우 Primary. **드리프트 처리**: #225는 **0건**(본문이 그대로 as-built), #223은 as-built 3건(`GroupMemberUiModel`에 `id: Long` 추가로 `key(member.id)` · State 계산 프로퍼티 `isNicknameValid` 추가 · 테스트 16개는 스펙 범위이고 #225가 8개 더해 24개)과 **스펙 서술 오류 2건**(`YGColorChipType` 사용처가 프리뷰뿐이 아니라 `app-preview` `YGTopBarPreviewScreen`도 포함 / `GroupInviteCodeRoute` 인셋 지적의 기전이 `innerPadding` 미소비가 아니라 **수동 IME 패딩 이중 적용**). 조치: 스펙 2건 `implemented`·플랜 2건 완료 표기 + 양쪽 archive 이동(링크 `../`→`../../` 보정, 팝업 플랜은 frontmatter가 없어 상단 머지 블록 신설)·README 4행 아카이브 등록. 아카이브 스펙 4건 as-built 정정(ygcolorchip 개수·s002·s102·a005의 "VM에서 매핑"), design-system(컬러칩 12종·텍스트필드 확장·`YGModalPopup` 좌우 배치 공존)·state-management·module-structure(`core:ui`→`:domain`)·navigation-flow(`consumeWindowInsets` 4형태)·ADR-0016 머지 표기. open-questions: **해소 2건**([2026-07-18] 컬러칩 12/14 · [2026-07-29] ADR-0016 원안 수렴), 마커 3건([2026-08-07] 인셋 관용구 5형태 · [2026-08-12] 모달 좌우 배치가 사실로 확정 · [2026-07-26] 문자열 규약 준수), **신규 6건**(화면 도달 불가 · mock 전량 + 서버 계약에 `groupName`·`memberLimit` 부재 · 컬러칩 배정 주체 미정 · 확인 3종이 in-flight/error 자리 없음 · `core:ui` 숨은 `:domain` 의존 · `GroupInviteCodeRoute` IME 이중). 미머지: 없음 |
| 2026-08-15 | `4daa6419` | Merge #237 (invite-code-clipboard-paste) | delta 2건(#221·#237) — 둘 다 **parfait 스펙 없던 라운드**. **#221**(`feature/segmentation-edit-stroke`, 이슈 #202): 비어 있던 `feature/segmentation`이 로딩·에러·대상 하이라이트·확인·수동 편집·테두리 편집으로 한 번에 채워져 **토핑 생성 경로가 C-101-confirm부터 C-106 직전까지 이어졌다** — 사후 스펙 1건 작성(`implemented`·archive): c103-segmentation-topping-edit. 목적지 3개가 위키 서브 화면 5개를 묶는다(C-103-loading·select → `NavKeySegmentation` / C-103 → `…Confirm` / **C-104·C-105 → `NavKeyToppingEdit`의 두 탭**). `Navigator.goToAndPopCurrent` 신설(확인 화면을 치환해 걷어낸다), `SegmentationResult`가 `BitmapWrapper`를 버리고 `subjectImagePath`+`subjectBounds`로 재편, ML Kit optional module을 `ModuleInstall`로 사용 직전 확인(`ModuleNotReady`·`Process` 예외 신설), `saveEditedImage`+UseCase 신설. 편집은 알파 마스크 3단계 합성 + **거리장 기반 테두리**이고 픽셀 유틸 3종이 `core:util`로 승격되며 유닛 테스트 2파일이 붙었다. 결과는 `ToppingEditResult`를 `ResultEventBus`로 돌려준다 — **왕복 관용구가 실사용 소비처를 되찾은 첫 사례**. `YGFloatingBar`·`YGEditButton`·`YGEditActionButton`의 **첫 실화면 소비처**이기도 하다. **정책 대조**: [[누끼-편집]] Aspect Fit·중앙·Scale 1.0 축소 차단·Pan clamping·배경 50%/복구 100%·굵기 2~50 전량 일치(단위만 px↔dp 갈림), 불일치 3건 — Safe Margin +20% 미이행·C-103-select 부재·실패 재시도 부재. **#237**: A-004에 클립보드 **붙여넣기 제안 바**(자동 채움 아님) — `InviteCode.parseOrNull`을 domain에 두고(부분 매치 금지·200자 상한), 초대 메시지 템플릿을 `core:ui`로 올려 S-101 복사와 공유, 읽기는 윈도우 포커스 시점에 `ClipDescription`(MIME·민감 표시) 선필터 후에만. `codeLength`가 `InviteCode.LENGTH`로 이관돼 매직넘버 소멸, 검증 실패 시 UiState 통째 교체가 `copy`로 정정, 유닛 테스트 2파일 신설. 조치: 신규 스펙 1 + a004·c101 아카이브 스펙 as-built, specs README 3행, module-structure(`core:util` 확장 2종·픽셀 연산 승격 근거)·navigation-flow(`goToAndPopCurrent`·**토핑 생성 플로우 절 신설**·`ResultEffect` 되살아난 사례·`adjustResize` 앱 전역 영향)·design-system(`YGFloatingBar`·편집 버튼 3종 첫 소비처, 탭 문자열 화면 소유 확정)·data-layer(결과 모델 재편·메서드 3개·모듈 설치 확인·캐시 경고) 갱신, ADR-0011·ADR-0012에 **As-built 절 신설**. open-questions: **해소 1건**(IME 이중 적용), **부분 해소 4건**(`YGFloatingBar` 사용처 0건 ①③ / `ResultEventBus` 존치 ① / C-101-confirm 이후 경로 ① / 세그멘테이션 예외 ①), 마커 1건(ML Kit ②③), **신규 7건**(Safe Margin 미이행·굵기 단위 px↔dp·플로우 출구 부재·다중 검출/재시도 부재·테두리 팔레트 정책 부재·편집 로직 테스트 0·raw `Bitmap` UiState+死코드 2건). 미머지: 없음 |
| 2026-08-15 | `80895eb1` | Merge #241 (mvi-error-infra-a002-login) | delta 1건(#241) — **선작성 스펙 2·플랜 2가 있던 브랜치**이고 as-built 반영도 머지 전날(2026-08-14) 끝나 있었다. 그래서 이번 회차의 일은 둘이다: 상태 전환과, **문서화 이후 브랜치에 더 얹힌 커밋 하나를 잡는 것**. **#241 본체**: 앱 최초의 실서버 결선 — A-002 카카오 로그인이 SDK `idToken`+`nonce` 취득부터 `AuthRepository`·`LoginWithKakaoUseCase`·`isNewUser` 분기·세션 저장까지 이어지고, 그 아래 MVI 공통 에러 인프라(`AppError` 3갈래·`Channel(BUFFERED)` 이펙트·`launch(key, onError)` 가드·`AppErrorMapper`·`runSuspendCatching`)가 깔렸다. 설계에서 뒤집힌 결정 1건(공용 `error` 채널·`postError`·`CollectAppError` 철회)은 ADR-0020 번복 절에 이미 적혀 있었고 **develop 코드에도 `CollectAppError`가 없음을 확인**했다. **as-built 정정 2건**: 스펙 파일 구성 절이 `LoginRoute` 변경 사유로 철회된 `CollectAppError`를 계속 적고 있었고, `NonceGenerator` 바인딩 위치가 계획의 `SingletonInjectModule`이 아니라 **`RepositoryModule`**이다(`@Binds`는 `interface` 모듈에만 된다 — 본문 DI 문단은 처음부터 맞았고 파일 구성 절만 갈렸다). **문서화 이후 델타 1건(`58ddf525`, 머지 4분 전)**: `ParfaitGroupRepository`/`Impl` 5메서드 + DI 바인딩 + 테스트 + `ServerErrorCode.ParfaitGroup` 8종·`Common` 1종이 **스펙 범위 밖으로** 들어왔다 — 그룹 화면 브랜치 셋(#233·#239·#240)이 각자 같은 4파일을 만들고 있어 충돌을 먼저 막은 것이고, UseCase·ViewModel이 없어 **소비처가 0**이다. 조치: 스펙 2건 `implemented`·플랜 2건 `done` + 양쪽 archive 이동(링크 `../`→`../../` 보정, 플랜의 spec 링크는 `specs/archive/`로 재지정, 체크박스는 실기기 Step 6 두 자리만 비워 둠)·README 4행 아카이브 등록. data-layer에 **원격 Repository 인벤토리 표 신설**(auth 2메서드·parfaitGroup 5메서드·소비 여부) + `RepositoryModule` 바인딩 목록·`ServerErrorCode` 중첩 구조(`Auth` 3만 사용, 9종 미사용) 갱신 + "미머지" 블록 제거, ADR-0020 머지 마커. **api 5표면**: `auth.md` Android 매핑·판별자 키 각주 "미머지"→머지 + `ServerErrorCode.Auth` 실체 언급, `parfait-group.md` Android 매핑에 **Repository 함수 표 신설**(5/8, 나머지 3은 화면 요구 시), `android_status`·`verified`·`conventions.md` 불일치 표·`README.md` 도메인 표는 불변(엔드포인트 구현 상태가 안 바뀜). open-questions: 마커 5건(OQ-P-076 평문 HTTP가 develop 도달·OQ-P-144 이관 표면 축소·OQ-P-146 **검증 안 된 로그인 경로가 develop에 있다**·OQ-P-147 과도기가 develop 상태가 됨·OQ-P-149 `runSuspendCatching` 2건 머지), **신규 1건**(OQ-P-157 그룹 경계·에러 코드 9종이 소비처 없이 선반영 — 나머지 3메서드 추가 주체 미정·`ServerErrorCode` 자기 KDoc 규칙 위반·선반영을 관례로 삼을지). 미머지: 없음 |
| 2026-08-15 | `ca9e4581` | Merge #248 (#245 group-list-api) | delta 5건(#242·#247·#243·#244·#248) — **결선 라운드**. 기준선 잡던 중 #248이 추가 머지돼 재산정했다(로컬 체크아웃은 그 이전이라 대조는 전부 `origin/develop` 기준). **#242**(약관): `GET /api/v1/policies` + `POST /auth/signup` 결선 — `PolicyRepository`/`Impl`·`GetPoliciesUseCase`·`SignUpUseCase`(성공 시 세션 저장)·`SignUpException` 신설, `TermContent`·`TERM_CONTENT_LIST` **삭제**, State가 `policies`/`agreedTermsIds`/`isLoading`/`isLoadFailed`/`isSigningUp`, VM Assisted 주입, 조회 실패 자리 + 재시도(코드에 공통 에러화면 TODO), 가입 실패는 로그만. **#243**(생성): `CreateGroupUseCase`가 실서버 호출 + `groupId > 0` 가드, 실패를 `ServerErrorCode.ParfaitGroup` 코드별 로그로 갈랐고, 유효성 문자 집합을 서버 정규식으로 좁히며 `Char.isKorean()` **삭제**. **#244**(참여): mock 2종 삭제 → `GetGroupJoinPreviewUseCase`·`JoinGroupUseCase`·`ChangeGroupNicknameUseCase`, **합류 시점이 A-004로 앞당겨지고** `NavKeyGroupNickName`이 `data class(groupId)`로, 실패는 feature 로컬 enum 2종(`InviteCodeError`·`GroupNickNameError`) + 화면 매핑 → **domain 표시 문자열(`InviteCodeResult`) 소멸**. **#248**(목록): `GetMyGroupsUseCase` 결선으로 mock 4건·mock 새로고침 삭제, `isError`가 실제로 서고, 상대시간 `GroupTimestamp` + 템플릿 이미지 분기 신설, `recentImageUploadedAt`이 `LocalDateTime` → `Instant`. **#247**: 캔버스 이동 3파일 패키지 선언 정정(동작 불변). 조치: 스펙 6건 as-built(intro-term-agree·a005·a004·s102·g001·c001) + unit-test-infra 표 노트, architecture 3건(data-layer Repository 인벤토리·`ServerErrorCode` 전량 소비·시각 타입 / navigation-flow 진입 체인·그룹 플로우·인자 있는 NavKey / module-structure 테스트 플러그인·`isKorean` 삭제), ADR-0016 as-built(feature 로컬 실패 enum), **api 5표면**(policy `android_status: done` + 매핑 절 재작성, auth signup 결선, parfait-group Repository 표에 UseCase·화면 열 + GET 목록 행 **`⚠️불일치`** + 각주, conventions 불일치 표 **1건 재개**, README 도메인 표·소비처 문단). open-questions: **해소 5건**(OQ-P-019 문자열·OQ-P-104 온보딩 체인·OQ-P-112 실패 화면 도달 불가·OQ-P-134 mock UseCase·OQ-P-147 세션 없는 도달), **부분 해소 3건**(OQ-P-094·OQ-P-098·OQ-P-157), 마커 5건, **신규 7건**(OQ-P-165~171: 시각 파싱 불일치·참여/닉네임 분리·실패 표현 4형태·매퍼 테스트 규약 위반·복귀 시 재조회 부재·상대시간 정책 갈림·자모 입력 정책 공백). 미머지: 없음 |
| 2026-08-15 | `bf72292a` | Merge #231 (background-edit-screen) | delta 1건(#231). **C-301 캔버스 배경 편집 화면**이 신설됐다 — parfait 스펙 없던 화면이라 **사후 스펙 1건 작성**(`implemented`·archive): c301-canvas-background-edit. 위키 [[기능정의서-v3]]이 C-301을 "파르페 편집 모드 진입"(배경 변경 + 토핑 편집 통합)으로 개편해 둔 자리이고, 코드 심볼은 배경만 가리키는 `CanvasBGEdit*`다. 구성은 배경 미리보기(9:16) + 팔레트(갤러리·카메라 원 + 색 8종, 가로 스크롤) + `YGFloatingBarEditTab`(배경/토핑) + 그만두기 `YGModalPopup`. **재사용 진입 플래그가 처음 들어왔다** — `NavKeyCameraCustom`·`NavKeyCustomGalleryPicker`가 `data object` → `data class(showGuideToast, returnResultOnly)`로 승격되고 `NavKeyPictureConfirm`에 `returnResultOnly`가 붙어, C-101-confirm이 세그멘테이션으로 전진(false)하거나 신설 `PictureConfirmResult`를 `ResultEventBus`로 반환(true)한다. 복귀는 `onBack()` 2회. C-001의 `onClickEditCanvasBG`도 이 PR에서 결선됐다. **드리프트 7건**: ① 확인 이펙트가 TODO라 **선택한 배경이 어디에도 반영되지 않음**(브랜치 중간 반영 커밋이 되돌려짐), ② 미리보기가 `YGCanvas` 재사용이 아니라 화면 자작이라 컷 도형·Dot Grid 없음 + 좌우 여백이 20이 아닌 **21dp 리터럴**(코드 주석이 토큰 부재 자인), ③ "토핑" 탭이 상태만 바꾸고 내용 없음, ④ State가 Compose `Color`·이펙트가 `YGCanvasBackground`를 들고 팔레트 8종 중 5종이 hex 리터럴, ⑤ `domain`에 `CANVAS_ASPECT_RATIO` 신설로 `YGCanvas` private 상수와 이중 정의, ⑥ NavKey 인자가 데이터 아닌 동작 플래그 + 복귀가 스택 깊이 가정 + 카메라 실패 반환 타입(`String?`)을 새 수신부가 못 받음, ⑦ 팔레트 원 3종이 `clickableYG` 미사용. **정책 대조**: 비율 9:16 일치, 좌우 여백·컷 도형 불일치, 팔레트 색·모달 문구는 정책 소스 부재. 조치: 신규 스펙 1 + c001·c101·c102 아카이브 스펙 as-built, specs README 등록, navigation-flow(**"캔버스 배경 편집 플로우" 절 신설** + NavKey 목록·엔트리 빌더·체크리스트 5번·인자 있는 목적지)·design-system(`YGFloatingBarEditTab` 두 번째 소비처·`YGModalPopup` 7번째·캔버스 컴포넌트 미재사용)·state-management(UI 타입 보유 사례)·module-structure(`domain` 표시 규격) 갱신. open-questions: 마커 5건(OQ-P-087·088·105·129·136·137), **신규 6건**(OQ-P-173~178). 미머지: 없음 |
| 2026-08-15 | `60df07a4` | Merge #250 (canvas-topping-member-api) | delta 1건(#250, **선작성 스펙·플랜 한 쌍**). **API 표면이 25/25로 닫혔다** — 서버 delta가 벌린 5 엔드포인트(파르페 `today`·과거 목록 · 토핑 테두리 PATCH·DELETE · 회원 탈퇴 DELETE)가 Service 5함수 + wire DTO 9 + domain VO 7(`domain/model/canvas/` 6 + `UpdatedToppingBorderVO`) + `source/parfait/mapper/VOMapper.kt` 신설 + DataSource 5함수로 들어왔다. **코드=설계 일치(드리프트 0)** — 함수명·시그니처·`@Query` 기본값·전 프로퍼티 `@SerialName`·매퍼의 네 폴백(`images` null→빈 목록 / 미지 배경 type→null / 미지 status→`UNKNOWN` / `SOLID` 불완전→`None`)·진입점 갈림이 본문 그대로다. **as-built 2건**(테두리 평탄화가 두 곳에서 필요해져 `ToppingBorder.flatten()`으로 추출 / `http-client.env.json`·`_reset.http`에 `parfait_id` 등재). **DI 바인딩 0줄 증가** — Service·DataSource가 안 늘고 함수만 는 첫 라운드다. **범위 밖 동반 변경 2건**(2차 서버 delta `e4ff23f` 반영): `CheckNameValidUseCase`에 자모 범위 추가(이번엔 **앱이 좁던 쪽**), `GROUP_NICKNAME_ALREADY_USED` 계열(상수·`GroupNickNameError.ALREADY_USED`·문구·매핑 분기) 제거 — 테스트 2건은 삭제가 아니라 `INVALID_GROUP_NICKNAME`으로 바꿔 살렸다. 조치: 스펙 `implemented`·플랜 완료 + 양쪽 archive 이동(링크 `../`→`../../` 보정, 플랜은 frontmatter가 없어 상단 머지 블록 신설)·README 2행 등록, 스펙에 "서버 규칙 delta 반영" 절 신설. **api 5표면**: `README.md` 도메인 표 Android 열 3행 `부분`→`구현됨` + 개수 20/25→**25/25**, `conventions.md` 개수 문단(불일치 표는 1건 유지 — 시각 파싱은 이 델타가 안 건드림), parfait·parfait-image·member 세 문서의 Android 매핑 절 전면 재작성 + 엔드포인트 표 Android 열 5칸 + `related_spec` 채움(`android_status`·`verified`는 불변 — 소비처 0이라 `partial` 유지). `data-layer`(반영 범위 25 엔드포인트·진입점 표에 두 DELETE 소비처·중첩 DTO가 "선언당 파일 하나"의 명시적 예외·`flatten()`·`domain/model` 하위 패키지 여덟·`ServerErrorCode.ParfaitGroup` 8→7종·DataSource 테스트 다섯). s102·a005 아카이브 스펙 as-built(자모 재개정·중복 허용으로 #244 서술 뒤집힘). open-questions: **해소 3건**(OQ-P-108 `http/` 커버 25/25 회복 · OQ-P-132 `safeApiCallWithoutData` 첫 소비처 · OQ-P-158 서버 5 앞섬), **부분 해소 4건**(OQ-P-094 ③만 남음 · OQ-P-162 ② · OQ-P-171 자모 구현 · OQ-P-179 ①), 마커 3건(OQ-P-160 앱 방어가 KDoc뿐 · OQ-P-173 배경 쓰기 API가 서버에 없음 확정 · `domain/model` 배치), **서술 오류 정정 1건**(OQ-P-168의 "`ParfaitGroupRemoteDataSourceImplTest`는 이미 있다"가 **사실이 아님** — group만 DataSource 테스트가 없다), **신규 2건**(OQ-P-181 매퍼 조용한 폴백 3종에 관측 수단 없음 · OQ-P-182 `http/` 파괴적 요청이 순서 실행으로 계정·데이터 삭제). 미머지: `session-token-refresh-infra`·`user-info-ssot`(둘 다 코드 미착수) |
| 2026-08-16 | `2d0f6a5d` | Merge #259 (#207 canvas-calendar) | delta 3건(#260·#262·#259). **#260**(추적하던 미머지 스펙·플랜 한 쌍, 이번 회차에 해소): 세션 인프라 — `TokenAuthenticator`(401 가로채 `@NoAuth` 가드 → 루프 가드 → `Mutex` → 선점 확인 → 재발급) · `SessionEventBus`(`Channel(CONFLATED)`, `:domain` 인터페이스 + `:data` 구현) · `MainRoute` 단일 수집 · `AuthRepository.logout()` + `LogoutUseCase` + S-001 결선. **코드=설계 대체로 일치, as-built 3건**: 한정자가 `@AuthClient`→**`@UnauthenticatedClient`**(사용처가 아니라 표면의 성질을 가리키는 이름, `AuthService` provider는 `ServiceModule` 소관) · `postForcedLogout`이 `trySend` 실패를 로그로 남긴다 · 바인딩은 신설 `SessionModule`. **계획서와는 3건 갈림**(계획이 낡은 판본): `Provider<AuthService>` 지연 주입 폐기(전용 클라이언트로 순환 자체가 소멸) · `clearBackStack()`+`goTo` → **`replaceAll()`**(그 함수를 **제거**하고 기존 호출부 3곳까지 이동, `NavigatorTest` 3케이스 신설) · `isLoggingOut` + `YGActionItem(enabled)`(클릭만 차단, 색 불변) 추가. **스펙 서술 오류 1건 정정**: "회원 탈퇴는 서버 계약이 없다"가 사실이 아니다(#250에 계약·표면 다 있고 stub인 이유는 범위 밖). 테스트 파일 5개 26케이스. **#259**(parfait 스펙 없던 라운드): C-201 캘린더 **사후 스펙 1건 작성**(`implemented`·archive) — `YGCanvas`의 `calendarContent` 슬롯 첫 충전(OQ-P-045 ② 해소), Dim이 메뉴·캘린더 겸용, `YGListDate` 첫 실화면 소비처, `YGStrokeButton.borderWidth`(`Dp.Hairline`) 신설, 공용 유틸 3종(`verticalScrollbar`·`toFirstDayOfMonth`·월 포맷 2종). 정책([[캘린더-컴포넌트]]) 4상태·인디케이터·Disabled 예외 일치, **불일치 2건**(03시 경계 미적용이 미래 잠금·오늘 강조로 확대 / 앞뒤 달까지 Disabled). **드리프트**: mock UseCase 2종이 `domain`에 생성 로직까지 들고 표면을 우회(OQ-P-134 해소 하루 만의 재발) · 고른 날짜가 아무것도 안 바꿈 · State 계산 프로퍼티 · 그리기 확장 소유 4곳. **#262**: 런처 아이콘 적응형 3종 + monochrome 교체, `windowSplashScreenAnimatedIcon`·`windowBackground` 제거(Android 12 미만 콜드 스타트 미검증) — Kotlin·gradle 변경 0. 조치: 스펙 `implemented`·플랜 `done` + archive 이동(링크 `../`→`../../` 보정, 플랜은 frontmatter가 없어 상단 머지 블록 신설)·README 4행, 신규 스펙 1 + c001 아카이브 스펙 as-built, ADR-0021 `proposed`→`accepted`, architecture 4건(data-layer 401 재발급 절·DI 표·Repository 인벤토리 / navigation-flow `replaceAll`·**"세션 종료 이동" 절 신설** / design-system `YGActionItem enabled`·`borderWidth`·`YGListDate` 소비처 / module-structure·state-management), **api 5표면**(auth `android_status: done`·엔드포인트 표 3행·Android 매핑 재작성, parfait Android 매핑에 표면 우회 마커, README 도메인 표·소비처 문단 — `verified`·서버 계약 절은 불변). open-questions: **부분 해소 2건**(OQ-P-045 ② · OQ-P-136 ②), 마커 5건(OQ-P-073 실제 노출로 승격 · OQ-P-094 auth 닫힘 · OQ-P-127 범위 확대 · OQ-P-129 ② 잔여 1 · OQ-P-146 검증 4항목 추가), **신규 6건**(OQ-P-183~188). 미머지: `user-info-ssot`(선행 조건 해소됨) |
| 2026-08-16 | `1873a8f0` | Merge #266 (#265 sync-backend-api-260816) | delta 1건(#266, 커밋 하나). **표면 왕복이 하루 만에 닫혔다** — 전날 서버 `22717fe`가 벌린 공백 2(캔버스 상세 조회 `GET .../parfaits/{parfaitId}` · 배경 변경 `PATCH .../parfaits/{parfaitId}/background`)에 `:data` 표면이 붙어 **Android 27/27, 공백 0**. 직전 라운드들(서버 `36ecd1c` → PR #250)보다 왕복이 짧다. **상세 조회는 비용이 거의 0** — 서버가 오늘 조회와 같은 응답 클래스를 재사용해 DTO·VO·매퍼가 그대로 쓰이고 Service·DataSource 함수만 늘었다. 대신 `TodayCanvasVO`가 **`CanvasVO`로 개명**(한 타입이 두 조회를 담는다)되며 **"이 GET이 캔버스 행을 만든다"는 경고의 소유가 타입 KDoc에서 함수 KDoc으로 내려갔다** — 반환 타입만 봐선 부작용 유무를 모른다. **배경 변경은 이 도메인 첫 쓰기 경로·첫 요청 DTO**이고 결정 둘이 핵심이다: 읽기 모델 `CanvasBackground`를 재사용하지 않고 **쓰기 전용 sealed `CanvasBackgroundEdit`**(`Color(hex)`/`Image(imageId)`)를 세워 ① 서버 계약의 비대칭(이미지 배경이 **쓸 때 `imageId`·읽을 때 URL**)과 ② **조건부 필수**(Bean Validation이 없어 OpenAPI `required`에도 안 드러나는 제약)를 컴파일에서 막는다. wire DTO는 평면·널 허용 그대로(서버의 거울). 반환은 `CanvasBackground?` — 미지 type을 널로 접는 규칙을 조회와 통일한 것이고, 이미지 echo URL은 앱이 모르는 값이라 버리지 않는다. **마감 가드는 의도적으로 범위 밖**(서버가 상태를 안 보지만 막는 것은 화면 책임 — KDoc ⚠️만). **DI 줄 증가 0**(PR #250에 이은 두 번째), DataSource 테스트 15 → **25 케이스**(요청 바디를 `coVerify` 인자 비교로 잠가 조건부 필수 두 갈래 검증). **선작성 스펙이 없던 첫 표면 라운드** → 사후 스펙 1건 작성(`implemented`·archive): canvas-detail-background-api-service-layer. 조치: 신규 스펙 1 + specs README 등록, #250 아카이브 스펙에 개명 as-built 블록, **api 5표면**(parfait 엔드포인트 표 Android 열 2칸 `미구현`→`구현됨` · Android 매핑 절 재작성(표 2행·요청 DTO 절 신설·domain VO 일곱·개명 경고·반환 널 사유) · README 도메인 표·개수 25/27→**27/27**·`http/` 절 · conventions 개수 문단 + 조건부 필수 절에 "앱은 도메인 타입으로 표현" 추가 — `android_status`는 `partial` 유지(소비처 0), `verified`·서버 계약 절 불변), architecture data-layer(표면 27·요청 방향 변환 세 번째 사례·DataSource 테스트 케이스 수). open-questions: **부분 해소 1건**(OQ-P-191 ② 조건부 필수를 sealed로 확정), 마커 6건(OQ-P-094 표면 25→27 · OQ-P-108 **왕복이 반만 닫힌 첫 사례** · OQ-P-173 앱 표면 도착 · OQ-P-181 폴백 자리 넷 · OQ-P-189/190 경고만 실림 · OQ-P-192 경고 소유 이동), **신규 2건**(OQ-P-193 저장 성공이 널로 접힘 · OQ-P-194 배경 표현 셋 사이 변환 주체 부재). 미머지: `user-info-ssot`·`ygscaffold-v2-common-loading-error`(둘 다 미머지, 후자는 구현 완료) |
| 2026-08-16 | `2143c229` | Merge #263 (user-info-ssot) | delta 1건(#263, **추적하던 미머지 스펙·플랜 한 쌍**). 계정 정보가 **암호화 로컬 SSoT**로 모이고 화면은 구독만 한다 — `UserInfoLocalDataSource`(DataStore+암호화·`Flow`) → `MemberRepositoryImpl`(remote↔local 조율) → UseCase 3종 → S-001·S-002 mock 제거(`null`=로딩, S-002는 입력 필드만 비활성). 스플래시가 `BootstrapSessionUseCase`로 갈라져 **자동로그인 성립**(`SplashInitialUseCase` 삭제, 진입은 `SplashIntent.Init` 하나), 판정은 `SessionBootstrap` 도메인 타입으로 나온다. 실패 목적지는 `ToLogin` 하나이고 **정리 범위만 갈린다**(401·`MEMBER_NOT_FOUND`만 파기 — 5xx·네트워크·로컬 저장 실패는 아무것도 안 지운다). **as-built 5건**: `ObserveMyAccountUseCase`→**`GetMyAccountFlowUseCase`**(구독하지 않고 `Flow`만 넘기므로 `Get…`) / 암호화 접근을 **`EncryptedPreferences`(`data/datastore/`)로 추출**하고 `EncryptedTokenStore`까지 이관 — **폐기 조건이 좁아져 읽기 IO 실패는 더는 지우지 않고**(ADR-0019 이전 as-built 정정) **암호문 상태 `distinctUntilChanged`**가 공유 DataStore 재방출로부터 편집 중 입력 버퍼를 지킨다 / 닉네임 변경 시 로컬 공백이면 **재조회 폴백**(`memberId`·provider를 몰라 VO를 못 세운다) / 부트스트랩 정리를 **`LogoutUseCase`에 위임**("무엇을 지우는가"의 단일 자리, 호출자 둘) / feature 로컬 `GlobalNicknameError` 4종 + `ServerErrorCode.Member` 2종 신설. DI 줄 증가 2(`RepositoryModule`·`LocalDataSourceModule`), 테스트 358 → **415건**(파일 40 → 47). 조치: 스펙 `implemented`·플랜 `done` + 양쪽 archive 이동(링크 보정, 플랜은 frontmatter가 없어 상단 머지 블록 신설)·README 2행, ADR-0022 `proposed`→**`accepted`** + as-built 3블록, ADR-0019 폐기 조건 정정·프록시 위임, ADR-0009 `Flow`는 `Get…` 명명 규약 + `SplashInitialUseCase` 삭제 반영, **ADR README status 드리프트 1건 정정**(ADR-0021이 `proposed`로 남아 있었다), architecture 3건(data-layer 프록시 저장 경로·`MemberRepository` 인벤토리 행·`ServerErrorCode.Member`·member Repository 획득 / state-management **SSoT 구독 절 신설**(`null`=로딩·저장값↔버퍼 분리·낙관적 갱신 금지·feature 로컬 실패 enum) / navigation-flow 앱 진입 체인이 갈림길이 됨), **api 3표면**(member.md Android 매핑 재작성 + 엔드포인트 표 2칸 `구현됨·결선됨` + 같은 코드에 처분이 갈리는 이유, README 도메인 표·소비처 문단 12건 — `android_status`는 `partial` 유지(탈퇴 미소비), `verified`·서버 계약 절 불변). open-questions: 마커 3건(OQ-P-072 저장 호출부는 생겼는데 검증은 그대로·OQ-P-172 S-002만 적용 확정·OQ-P-185 ① 피해 범위만 축소), **신규 4건**(OQ-P-195 오프라인 진입이 자동로그인 포기 · OQ-P-196 낡거나 빈 SSoT를 알릴 수단 없음 · OQ-P-197 G-001만 mock 잔존 · OQ-P-198 같은 서버 코드에 처분이 갈림), **oq-next 카운터 스테일 정정**(193 → 199). ⚠️ **수동 확인 7항목 미수행**. 미머지: `ygscaffold-v2-common-loading-error`(구현 완료·미머지) |
| 2026-08-16 | `0e2643cf` | Merge #261 (group-join-modal-after-nickname) | delta 1건(#261). **참여 확인 모달이 A-004 초대코드 → S-102 닉네임 화면으로 이관**되고 합류가 모달 뒤로 묶였다: 모달 "참여하기"가 `POST join` → `PATCH nickname`을 연달아 부르고, A-004는 미리보기(`GET join-preview`)까지만 하며 `NavKeyGroupNickName(inviteCode, groupName)`(참여 결과 아닌 참여 재료)로 넘긴다. A-004에서 `groupName`·`isConfirmPopupVisible`·모달 인텐트 2종·`JoinGroupUseCase` 주입 삭제, S-102는 이름 있는 `@Assisted` 2개 + `isConfirmPopupVisible`·`isEntering` 가드. `GroupNickNameError`가 닉네임 400 갈래 대신 **참여 실패 3종**으로 교체돼 `invite_code_error_*` 문구 재사용(`group_nickname_error_invalid` 삭제), `INVALID_GROUP_NICKNAME` 분기는 A-005만 남았다. **닉네임 PATCH 실패는 로그만**(안내 토스트 TODO). 문서 조치: a004·s102 스펙 as-built 갱신 + specs/README 행, navigation-flow 다이어그램·모달 게이트 서술, data-layer Repository 소비 열, design-system `YGModalPopup` 소비처, api/parfait-group Android 매핑·에러코드·두 요청 메모. open-questions: **OQ-P-166 해소됨**, OQ-P-137 ③④ 해소(①② 잔존), OQ-P-167에 "표현 없음" 사례 1건 추가. 테스트 415건 불변(초대코드 17→14 / 닉네임 6→9). 미머지: ① `ygscaffold-v2-common-loading-error` 유지 |
| 2026-08-16 | `46ed38fb` | Merge #264 (canvas-topping-screen) | delta 1건(#264). **C-301 편집 모드의 비어 있던 절반이 채워졌다** — 토핑 탭이 선택·이동·크기·회전·삭제와 테두리 재편집 왕복을 갖췄다. 딤 한 장을 사이에 낀 4층 스택으로 "내 것만 밝다"를 표현하고, 크기조절은 드래그 벡터를 **회전된 바깥 방향에 투영**해 0.5~2.5로 클램프, 선택 스트로크·버튼은 `ToppingGeometry`가 좌표를 따로 내 **버튼은 돌지 않는다**. 편집 버튼은 `NavKeyToppingEdit(borderOnly = true)`로 C-104/C-105 화면을 탭 없이 열어(`YGFloatingBarEdit` 첫 실화면 소비) 위키 C-306이 새 목적지 없이 성립. 확장 2종(`centeredAt`·`dragBy`) `core:util:android` 승격, 아이콘 2종 신설, release 서명 결선 동승. **사후 스펙 1건 작성**(`implemented`·archive): c301-topping-edit-tab(+README 등록). **as-built 3건**: c301 배경 스펙 드리프트 3 해소 표기, c103 스펙 `borderOnly`(Assisted 4인자·`isBorderOnly`), ADR-0003 release signingConfig. architecture 3건 갱신(module-structure 확장 목록 · design-system 아이콘·`YGCircleButton` 핸들 소비·`YGFloatingBarEdit` 첫 소비·점선 재사용 실패 · navigation-flow 재편집 왕복 절 신설). open-questions: **OQ-P-175 부분 해소**(② 답 나옴, ①③ 잔존), 신규 5건 OQ-P-199~203(mock 목록·미저장 / C-106 규격 부재 / 편집 대상 id가 Route에 삶 / C-202 정책 갈림·핸들 접근성 / 규약 이탈 셋). 테스트 변경 0건. 미머지: ① `ygscaffold-v2-common-loading-error` 유지 |
| 2026-08-17 | `955c4636` | Merge #267 (common-error-loading-scaffold) | delta 1건(#267, **추적하던 미머지 스펙·플랜 한 쌍**). **공통 로딩·실패에 자리가 생겼다.** `YGScaffoldV2`가 content → 로딩 오버레이 → 토스트 호스트 **세 층**을 겹치고(토스트가 오버레이 위 — 로딩 중 실패도 보여야 한다), 신규 파라미터 `isLoading`·`toastPolicy` 둘 다 **기본값**인 것은 V1 `@Deprecated(ReplaceWith)` 치환이 컴파일되어야 한다는 **계약**이다. 핵심 발견은 **이관이 이름 교체가 아니라 소유 위치 이동**이라는 것 — `hiltViewModel()`이 Route 안이라 `EntryBuilder`는 `isLoading`도 실패 이펙트도 못 본다. 그래서 `ERROR` 승급 기준이 "V1 호출처 0"에서 **"각 화면이 Route에서 스캐폴드를 소유하고 배선함"**으로 정정됐다(IDE 일괄 치환은 호출처만 0으로 만든다). **as-built로 더해진 것**: ① 오버레이의 `pointerInput` 소비만으로 부족 — **TalkBack 더블탭은 `SemanticsActions.OnClick`을 직접 불러** 통과하므로 스캐폴드가 `content`를 `semantics { hideFromAccessibility() }`로 감싼다, ② 그 기전은 `assertDoesNotExist`로 못 잠금(플랫폼 트리 전용) → `SemanticsMatcher`로 속성 보유만 단언, ③ `YGCustomTheme` 조상이 전제(`YGToast`가 `YGTheme.layout`을 읽어 **첫 토스트에서야 죽는다**), ④ 실패 문구는 호출부 소유 — A-002가 `LoginError` 4갈래(로그는 8갈래 유지)를 세우고 `ShowError`가 **문구가 아니라 사유**를 실으며 Route가 컴포지션에서 문구를 미리 뽑아 둔다(`LocalContextResourcesRead` 회피), ⑤ `launch(onError = …)` 동반 필수. 이관은 **3화면**(A-002 로그인 · S-003 앱 설정 · S-002 계정 정보)이고 **V1 잔여 8파일 22곳**. 조치: 스펙 `implemented`·플랜 `done` + 양쪽 archive 이동(링크 보정, 플랜은 frontmatter가 없어 상단 머지 블록 신설)·README 2행, **드리프트 2건 정정**(as-built의 "계측 9건"은 신규 7건 + 기존 smoke 2건 · "로그인 유닛 6건"은 `LoginViewModelTest` 9 → 11건), architecture 3건(design-system 구조 트리 `ygloading/`·`YGScaffoldV2.kt`·모듈 최초 `strings.xml` + 인벤토리 2행 + 화면 컨테이너 이관 현황·오버레이 적용 기준 / navigation-flow 체크리스트에 두 형태 공존 명시 / state-management **feature 로컬 실패 enum 두 번째 사례**), ADR-0020 "남는 것"에 **예상한 비용이 실제로 청구됨** 블록. open-questions: OQ-P-167 마커 갱신(머지 확정·잔여 이관은 OQ-P-204로 분리), **신규 3건**(OQ-P-204 스캐폴드 둘로 갈린 채 잔여 8파일·삭제 시점 미정 · OQ-P-205 오버레이 임시 구현·적용 기준이 귀납뿐 · OQ-P-206 토스트 2초 동안 상단 띠 탭 삼킴이 전 화면 공통으로 승격). 테스트 유닛 415 → **417**, 계측 5 → **12**(신규 7). ⚠️ **이관 3화면 실사용 확인 없음.** 미머지: 없음 |
| 2026-08-17 | `977f44f2` | Merge #268 (canvas-today-parfait-detail) | delta 1건(#268). **C-001 캔버스가 서버 캔버스를 그린다.** parfait 도메인 **첫 Repository**(`ParfaitRepository`/`Impl` — DataSource의 다섯 갈래 중 **오늘·목록·상세 셋만** 노출, 쓰지 않는 갈래를 미리 열면 실패 처리를 안 정한 채 계약이 굳는다) → UseCase 둘 → 화면. **같은 캔버스를 얻는 두 경로를 용도로 갈랐다** — 진입은 `/parfaits/today`(조회인데 행을 만드는 부작용을 감수: 토핑을 얹으려면 `parfaitId`가 필요한데 부작용 없는 경로는 없는 날을 만들어 주지 않는다 → `launch(key)`로 1회만), 달력 선택은 **목록→상세 2단**(훑는 것만으로 빈 캔버스가 쌓이면 안 된다 → 없는 날은 실패가 아니라 `null`, 상세 실패만 실패). `today` 응답 날짜를 앱이 검증해 **자정 경계 재시도 1회**, 그 "오늘"은 **KST 고정**(`PARFAIT_TIME_ZONE`·`parfaitToday()`) — 기기 시간대면 해외 기기에서 재시도가 **로드마다** 돌기 때문이고 **03:00 경계는 여전히 미적용**(OQ-P-127 ② 확정·① 잔존). 날짜 선택은 즉시 닫고 이전 그림을 비운 뒤 채우며, 응답 경합은 중복 가드가 아니라 **반영 직전 `selectedDate` 재확인**으로 막는다(마지막에 고른 것이 이겨야 한다). 렌더는 **계약이 말하지 않는 단위를 앱이 정했다** — 좌표 0~1 정규화 중심점 · `scale` 1.0 = 긴 변이 너비 **40%**(C-106) · `borderWidth` = **화면 dp** · 색 `#RRGGBB`(신규 OQ-P-207). 누끼라 사각 테두리를 못 둘러 **8방향 스탬프 + 원본 덮기**로 실루엣을 딴다(토핑 1개 = 이미지 9장, 미측정 — 신규 OQ-P-208). **진입도 이번에 열렸다** — `NavKeyCanvasImageAdd`가 `data class(groupId)`가 되며 6일 도달 불가가 닫히고 G-001 토핑에 첫 클릭 경로(`clickableYGScaleRipple`). `String.toColorOrNull()` `core:util:android` 승격. **드러난 것은 읽기 전용이라는 사실** — 배치·좌표 수정 소비처 0건, 조회 실패는 로그만이라 빈 캔버스와 실패가 같은 화면(신규 OQ-P-209). 달력 UseCase 둘·C-301 편집 탭·그룹명·칩 색은 여전히 mock(신규 OQ-P-210). 조치: **사후 스펙 1건 작성**(`implemented`·archive, +README 등록), c001·c201 스펙 as-built 갱신(c001 드리프트 3 해소·1·4 정정·정책 대조 2행 / c201 드리프트 2·4 해소·3 정정), architecture 4건(data-layer Repository 인벤토리 행 + parfait Repository 획득 노트 · module-structure `toColorOrNull` · navigation-flow 도달 불가 해소·인자 목적지·체크리스트 6 사례 종결 · design-system `YGCanvas` 배경/토핑 슬롯 첫 소비·`YGToppingGroup` 클릭 결선·`YGNametagChip` 두 번째 mock 사례), api 2건(parfait.md Android 매핑 — `verified` 불변 / README 도메인 표·소비처 15건). open-questions: **OQ-P-184 해소**, OQ-P-129 ①·OQ-P-130 ③ 해소, OQ-P-127 ②·OQ-P-183 ②③·OQ-P-200 부분 해소, OQ-P-167 ④·OQ-P-181·OQ-P-199 사례 추가, **신규 4건 OQ-P-207~210**. 유닛 417 → **434건**. 미머지: 없음 |
| 2026-08-17 | `fa7d79d6` | Merge #279 (canvas-calendar-api) | delta 1건(#279). **달력이 mock을 버렸고, 그 대가로 지난 날이 진짜 열렸다.** `ParfaitRepository`에 `getYears`가 올라와 **다섯 갈래 중 넷**이 열리고(남은 하나는 배경 변경) 두 UseCase의 mock 생성 로직이 통째로 사라졌다 → **OQ-P-183 해소**(하루 전 라운드가 남긴 "한 ViewModel 안에서 층이 갈린다"가 닫혔다). `ParfaitHistory` **삭제** — 달력이 계약 VO `PastCanvasVO`를 그대로 써 점 찍는 기준이 `imageCount` → `toppingCount`가 됐고 `domain/model/parfait/` 패키지가 소멸해 하위 패키지가 열 → **아홉**. `GetCanvasByDateUseCase`(목록→상세 2단)도 하루 만에 **삭제**되고 `GetParfaitDetailUseCase`로 대체 — 달력이 그 해 목록을 캐시로 들면서 **앞 단이 UseCase에서 화면으로 옮겨 갔다**. 상태가 `todayCanvas`/`viewedCanvas` **두 갈래**로 갈린 이유는 서버가 마감 캔버스 편집을 안 막기 때문이고, 부수 효과로 **오늘로 돌아갈 때 부작용 있는 `today` 재조회가 없다**. 지난 캔버스는 메뉴 액션이 **갤러리에 저장·오늘의 파르페 가기**로 바뀌어 편집 진입점이 사라졌다(OQ-P-189 ②에 앱 쪽 첫 답 — 가드가 아니라 길 치우기). 달력 셀은 **기록 있는 날 + 오늘**만 활성. 조치: 사후 스펙 1건 작성(`implemented`·archive) c201-canvas-calendar-server + README 등록, c201(#259)·c001-canvas-today-detail(#268) 아카이브 스펙에 대체·해소 마커, `api/parfait.md` Android 매핑(우회 소비자 해소·연 단위 조회·`android_status` partial 유지, **`verified`는 서버 계약 대조일이라 미변경**)·`api/README.md` 도메인 표 Android 열, `data-layer` Repository 인벤토리·하위 패키지 수·네트워킹 절. open-questions: OQ-P-183 **해소됨**, OQ-P-184 ②에 뒤집힘 마커, OQ-P-189에 앱 쪽 답 마커, OQ-P-199 심볼 정정, **신규 4건**(OQ-P-211 갤러리 저장이 로그 한 줄 / OQ-P-212 상세 조회 `launch(key)` 가드로 **연속 선택 시 머리말과 그림이 어긋난 채 남는다** — #268이 일부러 안 걸었던 것이 근거 없이 뒤집힘 / OQ-P-213 기록 없는 과거 날짜 잠금이 위키 Disabled 정의를 넘어섬 / OQ-P-214 연도 캐시 무효화 부재·파생이 캐시가 가른 둘을 뭉갬), `oq-next` 스테일(207 → 215) 정정. 유닛 434 → **436건**. 실기기·실서버 확인 없음. 미머지: 없음 |
| 2026-08-17 | `ede719f0` | Merge #292 (#284 clickable-to-clickable-yg) | delta 2건(#291·#292) — **둘 다 리팩터이고 둘 다 문서 선반영이 먼저 끝나 있던 브랜치**라, 이번 회차는 대조가 전부다(어긋난 곳 0건). **#291**(`refactor/#278-canvas-main`): C-001 화면 계열 `CanvasImageAdd*` → `CanvasMain*` 개명(NavKey·Route·Screen·ViewModel·UiState·Intent·Effect·`strings.xml` `canvas_main_*`). diff를 이름 치환 후 대조하면 짝 안 맞는 라인 0줄 = 시그니처·동작 불변이 기계 확인되고, develop 잔존 참조 0건. **#292**(`refactor/#284-clickable-to-clickable-yg`): 프로덕션 Foundation `Modifier.clickable` 28곳 전량 `clickableYGNoRipple` 이관 — develop 잔존은 `YGClickable.kt` 내부 구현 1곳 + `androidTest` 픽스처 2건(`YGLoadingOverlayTest`·`YGThemeSmokeTest`)뿐이라 design-system 서술 그대로다. `clickableYGNoRipple`에 `interactionSource` 첫 파라미터 추가(다른 네 변형과 동일 자리), 300ms 스로틀이 feature 화면 클릭 전반으로 확장. 문서 조치는 **선반영 문구의 미머지 마커 해제만** — `index.md` "지금 상태" 두 문단(⚠️ 미머지 선반영 → develop 머지 확정)과 doc-baseline 라인 갱신. `specs/README.md`·아카이브 스펙 7건 각주와 open-questions 해소 메모는 미머지 표현이 없어 그대로 유효. 신규 스펙·플랜·ADR 0건, 신규 미결 0건, 테스트 436건 불변. 실기기·실서버 확인 없음. 미머지: 없음 |
| 2026-08-17 | `2d15cd9f` | Merge #287 (#277 group-leave-report-api) | delta 2건(#285·#287) — **S-101 그룹 설정 결선 라운드**. **#285**: 상세 조회로 mock 기본값 5종이 사라지고 화면이 서버를 본다. 계약에 그룹명이 없어 `GetGroupDetailUseCase`가 `getMyGroups()`를 **한 번 더 불러 이름만 붙이고**(그 실패는 실패로 치지 않는다 — 빈 제목 + 나머지 표시), 조합 결과가 서버 응답에 1:1 대응하지 않는 유일한 그룹 VO `GroupDetailVO`다. `isMe`는 닉네임 중복 허용 이후라 **`memberId`** 로 판별(계정 SSoT 구독). **진입도 이때 열렸다** — `NavKeyGroupSetting`이 `data class(groupId)`가 되고 C-001 상단 메뉴가 호출자가 돼 OQ-P-138(4일간 도달 불가) 해소. 컨테이너도 `YGScaffold`(엔트리) → `YGScaffoldV2`(Route)로 이관돼 **결선 라운드에 스캐폴드 이관이 딸려 온 첫 사례**(V1 잔여 8→7파일, OQ-P-204 ①에 사례로 답). **#287**: `leaveGroup`·`reportGroup`이 올라와 **DataSource 8함수 전량 = Repository 8함수**가 됐고 parfait-group이 **`android_status: done`**(8 엔드포인트 전부 호출부). OQ-P-141의 셋 중 **①만 채우고 둘은 필요 없게 만들었다** — in-flight 필드 신설, 순서는 뒤집지 않고 스캐폴드 오버레이가 덮음, 그래서 `YGModalPopup` 좌우 플래그 분리 불필요. 나가기·신고는 결과가 같아 한 함수로 모으고 성공 시 **`replaceAll(NavKeyGroupList)`**(백스택이 전부 떠난 그룹 것이라 되돌아가면 403). 조치: 사후 스펙 1건 신규(`2026-08-17-s101-group-setting-api`, implemented·archive + README 등록), s101·Danger Zone 아카이브 스펙에 결선 노트, api/parfait-group(`android_status: done`·엔드포인트 표 결선됨·Repository 표 3행·Android 매핑 5건)·api/README(도메인 표·라운드 노트, 소비 19건), data-layer(Repository 인벤토리·방침 종결·UseCase 조합 첫 사례)·navigation-flow(진입·이탈 절 신설·리셋 관용구 4번째·Assisted 목록·구 형태 7파일)·module-structure(feature 간 `:api` 의존 2건)·state-management(in-flight 분리 규약)·design-system(V2 4화면·잔여 7파일) 갱신. open-questions: **OQ-P-138 해소됨**, OQ-P-139·140·141·167·186·204 갱신, **신규 3건**(OQ-P-216 상세 2회 호출·217 신고 사유 상수·218 403/404가 일시 장애와 같은 문구). 유닛 436 → **456건**. 미머지: 없음 |
| 2026-08-18 | `8730ffa3` | Merge #297 (#288 group-list-refresh) | delta 1건(#297). **화면이 앞에 설 때마다 다시 묻기 시작했다** — G-001·C-001에 `Enter` 인텐트 + Route `LifecycleResumeEffect`. `init` 조회는 화면 수명이 아니라 ViewModel 수명에 걸린 것이라 백스택 아래에서 살아남아 낡았고, 그것이 닫혔다(**OQ-P-169 해소**). 복귀 관용구(`goToSingleClearTop`)는 손대지 않았다 — 재조회가 필요한 이유가 복귀가 아니라 **남이 바꾸기 때문**이라서다(OQ-P-136과 분리). **실패 규칙이 뒤집혔다**: 목록이 남아 있으면 화면 유지 + 당긴 새로고침 실패만 토스트(`ShowRefreshError`), 목록이 비면 종전대로 에러 화면. 토스트 호스트 때문에 G-001 Route가 **`YGScaffoldV2`**로 이관 — **결선 아닌 라운드가 이관을 끌어온 첫 사례**(V1 잔여 7 → **6파일**, OQ-P-204 ①). C-001은 `syncToday()` 뒤 **오늘을 볼 때만** 오늘 캔버스 + **올해** 달력 기록을 재조회(연도 목록·지난 캔버스는 그대로), 부작용 GET을 재진입마다 부르되 ViewModel 생성만으로는 캔버스가 안 생기게 됐다. `syncToday()`가 열어 둔 채 자정을 넘긴 경우를 맡는다. 문서: 사후 스펙 1건 신규(`2026-08-17-screen-resume-refetch`, implemented·archive + README 등록), g001·c001-today-detail·c201-calendar-server 아카이브 스펙에 갱신 노트, state-management(재진입 재조회 절 신설)·navigation-flow(`goToSingleClearTop`·생성 참여 복귀)·design-system(V2 5화면·잔여 6파일) 갱신. open-questions: **OQ-P-169 해소됨**, OQ-P-046·204 갱신, **신규 1건**(OQ-P-221 재진입 재조회가 규약 아님 + 실패 표현 편차). 범위 밖 정정 1건: g001 스펙의 토핑 클릭 미결선 서술이 #268 이후 stale이라 함께 고쳤다. 유닛 456 → **467건**. 미머지: `feature/#294-group-ssot`(스펙·플랜 등록됨) |
| 2026-08-18 | `86f0f6b0` | Merge #305 (#254 group-list-refresh-lottie) | delta 3건(#296·#295·#305). **#296**(약관 웹뷰): `NavKeyServiceTerms`·`NavKeyPrivacyPolicy` + Route/Screen/ViewModel 2벌 → **`NavKeyWebView(title, url)` 1벌**, **ViewModel 삭제**(상태가 인자뿐·부를 API 없음), 엔트리 머티리얼 `Scaffold` → Route `YGScaffoldV2`. 온보딩 약관의 마지막 stub(`NavigateToUrl`) 해소 — `ClickTermDetail(policy)`·`NavigateToPolicyDetail(title, url)`로 개명. 설정 화면이 `GetPoliciesUseCase` **두 번째 소비처**(policy 도메인 `done` 유지). **#295**(버전 정보): `core:util:android`에 `AppInfo#APP_VERSION_NAME`(모듈 `buildConfig` 첫 활성 + `:app`과 같은 카탈로그 항목 재기입) → S-001 버전 placeholder 해소, 표시 `v` 접두는 포맷 리소스. `YGListItem` 두 오버로드 줄 높이를 컴포넌트가 `heightIn`으로 정렬. **#305**(로띠): `YGLoadingLottie`(+`YGLoadingTone`) 신설·`YGLoadingOverlay` 인디케이터 교체(Dim `Black25`→`Black75`), G-001 당겨서 새로고침 커스텀 인디케이터, 스플래시 로띠 + **부트스트랩·재생 종료 둘 다 대기**(`SplashState` 2필드). 조치: architecture 3건(design-system 트리·컴포넌트 표·화면 컨테이너·로띠 의존·`YGListItem` 노트 / module-structure terms·`core:util:android` 행 / navigation-flow NavKey 통합·출처 인자 비적용·스플래시 진입 조건) + api 2건(policy.md Android 매핑·앱 동작 메모·미결 경고, README 도메인 표 주석 — `android_status`·`verified` 불변) + 아카이브 스펙 6건 as-built(s004·feature-common-terms-module·intro-term-agree·app-setting-s001·ygscaffold-v2·user-info-ssot). **문서 전제 오류 1건 정정**: design-system이 `refactor/segmentation-logic`(로컬 전용 브랜치) 수치를 "develop 기준 13화면·V1 3파일"로 적고 있어 브랜치 기준으로 표기 + develop 값(6파일) 병기, 미머지 추적에 추가. open-questions: **부분 해소 1건**(OQ-P-205 ① 인디케이터), 마커 3건(OQ-P-204 이관 계기 확장·OQ-P-068 계약 공백이 화면으로·OQ-P-125 로띠 애셋 소유), **신규 5건**(OQ-P-229 스플래시 대기 상한·OQ-P-230 로띠 호출 관용구 갈림·OQ-P-231 약관 탭 무반응·OQ-P-232 웹뷰 출처 검증·OQ-P-233 버전 상수 이중 출처). 신규 spec 작성 0건(신규 화면 없음, `YGLoadingLottie`는 ygscaffold-v2 스펙 as-built로 흡수). 테스트 467 → **474건**. 미머지: `refactor/segmentation-logic`·`feature/#294-group-ssot` |
| 2026-08-19 | `f12870a8` | Merge #290 (feature/topping-add-screen) | delta 1건(#290). **C-106 토핑 배치 화면 신설** — 자리채움 `NavKeyCanvasMove`(아무것도 안 하는 `CanvasMoveScreen`)를 대신해 `NavKeyCanvasToppingPlace(imageUri)` + Route/Screen/ViewModel 한 벌이 들어와 토핑 생성 플로우의 마지막 자리가 채워졌다. 위키 [[C-106-토핑-배치-정책-v0.1]]의 초기 배치 규칙 **넷이 처음으로 코드에 들어왔다**(긴 변 = 캔버스 너비 40%·정중앙·짧은 변 48 하한·이탈 허용 + 클리핑) → **OQ-P-200 종결**. 40% 상수를 `internal`로 열어 읽기·쓰기가 공유(③ = 양쪽 다), 48은 **dp로 굳음**(② 확정, 정책은 px). 계산은 ViewModel 소유 — 캔버스 실측·토핑 원본 크기가 서로 다른 시점에 오므로 인텐트 둘로 받아 매번 재시도하되 사용자가 손대면 멈춘다. **리사이즈 한계는 고정 배율이 아니라 역산**(하한 = 48dp 최소 터치, 상한 = 캔버스 긴 변 1.5배). 그리기는 `center`·`sizeAfterScale`을 한 번만 계산해 이미지·스트로크·핸들에 공유하고 클리핑만 셋이 갈린다. 세그멘테이션 결과가 **두 벌**로 갈림(`trimmedSubjectImagePath` 신설 — 편집은 원본 크기 유지, 배치는 여백 없는 실제 크기) → 캐시 PNG·메모리 버퍼 각 1 증가, "테두리 없으면 한 번만 떨군다" 최적화 소멸. `CanvasBGEditScreen` private 컴포저블 3종이 모듈 `component/`로 승격돼 두 화면 공유(`toppingId: Long` → `key: Any?`), 그 과정의 `size` → `requiredSize`가 C-301 토핑에도 적용. `YGScaffoldV2` 이관 **8화면째**(V1 잔여 6파일 불변). 조치: 신규 as-built 스펙 1건(c106-topping-place, archive) + specs README 등록·c103 아카이브 스펙 목적지/모델 정정, navigation-flow(NavKey 목록·토핑 생성 플로우 다이어그램·도달 불가 표기)·design-system(이관 8화면·`YGFloatingBarEdit` 두 번째 화면·핸들 관용구 공용화)·data-layer(`SegmentationResult` 3필드)·module-structure(화면 간 공유 컴포저블은 모듈 `component/`) 갱신, open-questions: **OQ-P-200 해소** · OQ-P-209 부분 해소(①) · OQ-P-202/203/204/207/228 사례 갱신 · **신규 4건**(OQ-P-238 `groupId = 0L` 하드코딩 + 백스택 누적 / OQ-P-239 `NavKeyCanvasMove` 도달 불가 / OQ-P-240 배치 화면이 실제 캔버스가 아님 / OQ-P-241 회전·리사이즈 한계에 정책 근거 없음). 미머지 세그멘테이션 스펙의 캐시 정리 안전 근거가 **다시 거짓**이 됨을 그 스펙에 기록. 유닛 474 → **484건**. 미머지: `refactor/segmentation-develop`·`feature/#294-group-ssot`·`feature/#300-sync-backend-api-250819`(셋 다 리베이스 필요) |
| 2026-08-20 | `c36cad49` | Merge #306 (#236 withdraw-api) | delta 1건(#306). **되돌릴 수 없는 문 셋이 다 열렸다** — S-001 회원 탈퇴가 로그 한 줄에서 실제 요청이 됐다. 표면(#250)도 팝업(#225)도 이미 있어 이번에 들어온 것은 사이를 잇는 셋뿐이다(`MemberRepository.withdraw` · `WithdrawUseCase` · ViewModel 분기). **핵심은 순서다** — 서버가 받아 준 뒤에야 기기를 정리하고 거절당하면 아무것도 지우지 않는다(로그아웃과 반대. 서버가 거절했는데 로컬만 지우면 계정이 살아 있는 채로 사용자만 탈퇴했다고 믿는다). 정리는 새로 쓰지 않고 `LogoutUseCase`에 위임해 "무엇을 지우는가"의 단일 자리를 지켰다(호출자 셋). 화면은 **S-101 나가기·신고 형태를 그대로 복제**(팝업 먼저 닫고 로딩 오버레이 + 실패 토스트)했고 갈린 것은 목적지(`replaceAll(NavKeyLogin)` — 세션이 끝나 그룹 목록으로 갈 자리가 없다)와 **연타 잠금 테스트**뿐이다. `isWithdrawing`을 `isLoggingOut`과 합치지 않은 이유는 덮개가 아니라 `YGActionItem(enabled = !isLoggingOut)`이 로그아웃 줄 하나만 가리키기 때문. S-001이 토스트 호스트를 처음 얻었지만 이미 V2라 컨테이너는 불변(이관 8화면·V1 잔여 6파일 그대로). ⚠️ **끝난 뒤가 깨끗하지 않다** — 위임받은 로그아웃이 방금 지워진 계정의 토큰으로 나가 401 → 재발급 거절 → `ForcedLogout`까지 이어져 이동을 두 곳이 일으킨다(**OQ-P-242 신설**). 조치: 신규 스펙·플랜 없음(선작성 문서가 없던 소규모 결선 라운드), **api 4표면**(member.md `android_status: partial`→**`done`** · 엔드포인트 표 DELETE 열 `구현됨·결선됨` · Android 매핑 2블록 재작성 · README 도메인 표 member 행 **결선됨** + 소비처 20건 문단. `verified`·서버 계약 절 불변, conventions.md 불일치 표는 이 delta가 안 건드림), architecture 3건(data-layer `MemberRepository` 인벤토리 + UseCase 규칙 넷째 + 도메인 공백 문단 / state-management in-flight 분리 사례 둘째 / design-system 토스트 호스트 사례). open-questions: **해소 1건**(OQ-P-141 Danger Zone 확인 3종 — ②·③은 채택이 아니라 불필요해져서 닫힘), **부분 해소 2건**(OQ-P-162 ③이 물음이 서지 않은 채 닫힘 · OQ-P-186 ② 닫힘, ① 비활성 색은 잔존), **신규 1건**(OQ-P-242). 테스트 484 → **490건**. ⚠️ 실기기·실서버 확인 없음. 미머지 3건 재확인 — **`refactor/segmentation-develop`이 리모트에 올라와 있어 직전 회차의 "리모트에도 없다"를 정정**했고, 셋 다 base가 develop 뒤라 리베이스 필요 |
| 2026-08-20 | `750cc2dd` | Merge #310 (#300 sync-backend-api-250819) | delta 3건(#307·#308·#310, 전부 선작성 스펙·플랜 보유). **미머지 스택이 한 번에 비었고 계약 정합이 처음으로 0건이 됐다** — [api/conventions.md](api/conventions.md) "Android 불일치"가 2건 → **0건**. 닫힌 방식이 서로 다르다: 업로드 시각 파싱은 **앱이 서버 포맷 변경을 기다리지 않고** `LocalDateTime` + 고정 KST로 읽는 쪽(OQ-P-165 ①의 "서버 쪽이 자연스럽다"를 뒤집었고 근거는 서버 DB 커넥션 세 환경의 `serverTimezone=Asia/Seoul`), 하루 경계는 **정책상 옳은 쪽이 서버라 앱을 03시로** 옮겼다(`parfaitToday()` 한 함수만 고쳐 재시도 조건·달력 활성 조건·`syncToday()` 트리거가 저절로 따라왔다). **#307**: 그룹 목록·상세가 `:data` 인메모리 캐시 한 벌로 모이고 세 화면이 구독한다 — 갱신 함수가 `Result<Unit>`이라 **값을 얻는 두 번째 경로가 없다**([ADR-0023](adr/0023-group-in-memory-ssot.md) `proposed` → `accepted`). 세션 정리 경로 셋(로그아웃·강제 로그아웃·탈퇴 위임), 캐시 clear를 계정 정보 clear **앞에**. **#308**: 칩 배정 주체가 **서버**로 정해져 `:domain` 중립 enum `NametagChipType` 신설 + feature가 디자인시스템 타입으로 옮긴다. **`GroupDetailVO` 삭제** — 상세에 `groupName`이 실려 존재 이유가 사라졌고, 합성 자리를 `:data`가 아니라 UseCase에 둔 판단이 소멸을 한 줄 삭제로 끝나게 했다. S-101 "N명 남음"도 실값. **#310**: 서버가 HTTP DTO 경계에서만 키를 `nameTagChip` 계열로 바꿔 직전 라운드의 칩 결선을 조용히 무력화한 것을 되살리고, C-001 상단 칩을 서버 값으로 결선해 `NAMETAG_CHIP_PALETTE`를 걷었다(팔레트 개념째 소멸 → **같은 사람이 S-101·C-001에서 같은 색**). 머지 전 리뷰가 결론 하나를 뒤집어 **모르는 값·값 없음을 모두 `DEFAULT`로 접고 널 허용을 없앴다**([ADR-0024](adr/0024-nametag-chip-unknown-fold.md), 대가는 "새 타입"과 "반납된 자리"의 구분 상실). `:data` 칩 매퍼 두 사본은 `source/common/mapper` `internal` 하나로 합쳤다(feature 색 변환 셋과 결론이 갈리는 이유는 가시성 미결이 여기엔 안 걸려서다). **`MyParfaitGroupVOMapperTest` 삭제로 `XxxVOMapperTest`가 0개** — 오프셋 붙은 입력을 스스로 지어 넣어 파싱 버그를 초록으로 지켜 온 파일이고, 옮길 대상 `ParfaitGroupRemoteDataSourceImplTest`도 이번에 신설(OQ-P-168 해소). ⚠️ **재발 방지 수단은 이번에도 없고 위험 반경은 넓어졌다** — 키 어긋남을 잡은 것은 두 번 다 계약 문서 감사였고, 그전까지 브랜치에 갇혀 있던 사고가 지금은 **develop 네 자리에서 색을 정한다**(OQ-P-234 ③). ⚠️ 서버 `COALESCE` 폴백 탓에 토핑 0건 그룹이 **생성 시각을 활동 시각처럼·생성자 칩을 마지막 토퍼 칩처럼** 보여 준다(OQ-P-235 사정거리 확대). 미결: **해소 8건**(OQ-P-165·168·216·222·224·234 ①②·236 ①) · **소멸 1건**(OQ-P-210 ②) · **신설 1건**(OQ-P-243 — 하루 경계 상수 하나가 뜻이 다른 두 하루를 정한다). 스펙 3건·플랜 3건 `archive/` 이동, 스캐폴드 이관 수치 불변(8화면·V1 잔여 6파일). 유닛 490 → **538건**(파일 56 → 61). ⚠️ 실기기·실서버 확인 없음. 미머지: `refactor/segmentation-develop`(다섯 세대 뒤처짐, 커밋 11 → 13개로 늘어 스펙이 자기 브랜치보다 뒤처졌다) |
| 2026-08-20 | `cf357937` | Merge #309 (refactor/segmentation-develop) | delta 1건(#309, 선작성 스펙·플랜 보유). **추적하던 마지막 미머지 브랜치가 들어와 미머지 항목이 0건이 됐다** — 머지 커밋 트리가 브랜치 팁(`63ec2989`)과 같아 스펙 as-built 수치를 재측정 없이 승격했다(유닛 538 → **560건**, 클래스 61 → 64). 이 라운드가 한 일은 새 감사가 아니라 **"브랜치에만 있다"는 단서를 문서에서 떼는 것**이었다. **닫힌 미결 여섯**: OQ-P-003 ③(캐시 정리 — 진입 시 전용 디렉토리를 통째로 비운다. 그 안전 근거를 참으로 만드는 되감기 수정이 **같은 커밋에 있어야만 참**이었고 이번에 그렇게 됐다) · OQ-P-004 ②(마스크 null raw throw → `Result.failure`) · OQ-P-055 ②(닫기 목적지) · OQ-P-087(죽은 `ResultEffect` — 기능은 죽고 크래시만 살아 있던 통로라 걷어냈고, `ReturnResult(uri: String?)`를 인자 없는 `Cancel`로 좁혀 `null`을 흘리는 자리 자체를 없앴다) · OQ-P-152(플로우 출구 — `Navigator.popUpTo<T>()` 하나로 세 Route의 닫기가 함께 열렸다) · OQ-P-238 ①(배치 완료가 캔버스를 새로 쌓던 것 → 되감기, 하드코딩 `groupId = 0L` 소멸). **부분 해소 셋**: OQ-P-204(8엔트리 일괄 이관으로 V1 잔여가 처음 줄어 6 → **3파일**, 이관 화면 8 → **16개**. 계기는 로딩·실패가 아니라 어차피 그 파일을 다 여는 라운드였고 여덟 중 `isLoading`을 넘기는 곳이 0이다) · OQ-P-178 ②(배경 편집 복귀가 `onBack()` 2회 → `popUpTo<NavKeyCanvasBGEdit>()`, PR 리뷰가 짚었다) · OQ-P-155(`feature/segmentation/impl`이 첫 `src/test`를 얻었으나 `UndoRedoStack`·`BitmapViewMapping`은 여전히 미검증). **스펙을 뒤집은 결정 둘**(`decodeImage` 계약 → `Result` / 최근 이미지 공급자 신설)도 그대로 들어왔다. 열린 채: OQ-P-239(도달 불가 잔해) · OQ-P-228(다운샘플, 현재 트리 피크 미측정) · OQ-P-003 ①(재시도 동선) · 카메라 캐시 정리. ⚠️ 실기기 확인 없음 |
| 2026-08-20 | `36719e8e` | Merge #315(term-agree scaffold v2) · #318(API 현행화 260820) | delta 2건, **둘 다 머지 커밋 트리 = 브랜치 팁**(충돌 해소 편집 0건). 공통점은 **"임시"라고 적어 둔 자리가 결정으로 바뀐 것**이다. **#315** — 이름은 스캐폴드 이관인데 실은 실패 표현 라운드다. 가입 실패가 갈래 넷을 열거만 하고 로그로 흘리던 것(`TODO(에러 UX 미정)`)이 `TermAgreeError` **2갈래** + 공통 토스트가 됐고, `RequiredPolicyNotAgreed`처럼 **로그에는 결함으로 남기는 갈래까지 `UNKNOWN`으로 접었다**(사용자 처분이 같다). 같은 화면의 조회 실패는 **반대로** — `TODO(공통 에러화면)`이 예고하던 공용 화면으로 **가지 않기로** 정해 목록 자리에 남겼고, 가른 기준이 OQ-P-167 ①의 답이다(**재시도 동선이 화면 안에 있으면 화면에 남기고 없으면 토스트**). 컨테이너는 Route의 `YGScaffoldV2`로 옮기며 `isLoading`에 **두 플래그를 겹쳐** 넘기는 첫 사례(`isLoading \|\| isSigningUp`). **V1 잔여 3 → 2파일**(둘 다 EntryBuilder), 이관 화면 16 → **17개** — OQ-P-204 ①의 "이관만 하는 라운드" 첫 사례인데 **결선을 데려왔다**(채울 것이 밀려 있던 자리였다). **#318** — 하루 전 서버 409 가드로 거짓이 된 앱 주석 일곱을 **지우지 않고 고쳤다**(`parfait/CLAUDE.md` "기준 2와 3이 겹칠 때는 남긴다", 단정 대신 `api/parfait.md` 포인터). 화면 방어는 남는다 — **실패를 보여 주기 전에 길을 없애는 일**이라 서버 가드와 목적이 다르다. `ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED`를 **소비처 0건인 채로** 신설하며 "쓰는 코드만 둔다" 규약에 **"처분이 정해진 코드는 미리 둔다"** 예외를 함께 적었다(브랜치 PR2 파일과 바이트 동일). ⚠️ 상수 KDoc이 남긴 발견 — 다섯 경로 전부 **권한 검사가 마감 검사보다 앞**이라 마감된 캔버스라도 남의 토핑·비멤버면 403이 먼저 온다. 조치: 스펙 1건 as-built(intro-term-agree, `verified` 2026-08-20)·architecture 3건(design-system 이관 수치 + 📌 · navigation-flow **구 형태 7파일이라는 낡은 수치 정정** · state-management 실패 enum 사례) · api 2건(parfait·parfait-image Android 매핑을 ⚠️ → ✅, `verified`는 서버 대조일이라 불변) · index 3곳. open-questions: **해소 3**(OQ-P-244 ① · OQ-P-167 ③④) · **신설 1**(OQ-P-249 가입 중 플래그 KDoc이 없는 버튼을 가리킨다) · **악화 1**(OQ-P-167 ② "알 수 없는 오류" 문구를 든 화면이 셋). 테스트 유닛 560 → **561건**, 파일 64개 유지. ⚠️ 실기기·실서버 확인 없음 |
| 2026-08-21 | `da03c9b0` | Merge #320(overlay reset) · #319(calendar dim guard) · #312(내 닉네임) · #298(Spotlight) · #322(C-106 업로드·배치 계층) | delta 5건, **다섯 머지 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 라운드를 가르는 축은 **보이는 변화와 쌓이는 변화**다 — 넷은 화면에서 눈에 보이고, 가장 큰 #322는 화면에서 아무것도 바꾸지 않는다. **#298**: 캔버스 토핑이 처음 탭을 받는다(C-202 Spotlight). 사후 스펙 신규 작성(`implemented`·archive). 상태는 `spotlightedToppingId` 하나이고 그리기 순서가 곧 정책 z 우선순위(나머지 → Dim `Black50` → 강조). 정책 대조 8일치·2공백 — **본인 갈래가 통째로 비었고**(`isMine()`이 상수 `false`, 내 멤버십 행 id를 알 길이 없다 → OQ-P-250 신설), 탈퇴 문안은 서버가 준 `(알수없음)`이 그대로 문장이 된다. ⚠️ 진짜 결정은 **토스트 자리** — `YGCanvas`에 `overlayContent` 슬롯이 뚫려 토스트가 스캐폴드가 아니라 캔버스 프레임 상단에 서고, 미머지 PR3의 조회 실패 토스트까지 그 자리로 정해졌다(OQ-P-167). 작성자 칩은 서버 필드가 아니라 화면 조인인데 **탈퇴 멤버에서도 결과가 우연히 같아** 임시가 안 드러난다(OQ-P-251 신설). **신규 유닛 0건**(OQ-P-252 신설). **#322**: C-106 스택 1/5·2/5가 한 PR로(PR2 브랜치가 PR1 커밋 여섯을 업었다). **Repository 0건 도메인이 사라졌다**(image·parfait-image). Retrofit 밖 raw OkHttp 첫 자리라 `@NoAuth`가 안 걸리고 전용 `@UploadClient`가 기능 전제다. 소비자 0이라 Dagger 도달 불가 — 리플렉션 바인딩 테스트가 유일한 감지선. **#312**: G-001 마지막 mock 소멸, 닉네임이 계정 SSoT 구독이 되며 **전역 닉네임으로 확정**(OQ-P-197 해소). ⚠️ 값이 없으면 그룹 만들기가 조용히 안 열린다(OQ-P-253 신설). **#319·#320**: 달력 빈자리 탭이 달력을 닫던 것을 컴포넌트가 소비해 막고, 그룹 추가 오버레이를 나가는 길에 접는다. 조치: 스펙 신규 1(c202)·아카이브 스펙 갱신 3(g001·c201·c106 active)·plan README 및 PR1/PR2 `archived_reason` 머지 표기·architecture 3건(data-layer Repository 인벤토리 2행 추가·DI 3항목·design-system 슬롯/토스트·state-management 세 번째 구독자)·api 5건(image·parfait-image·parfait·member·README, `verified`·`android_status` 불변)·index 상태 3문단. open-questions: **해소 1건**(OQ-P-197) · **신규 4건**(250~253) · 마커 2건(OQ-P-224·OQ-P-167 관련). **범위 밖 스테일 3건도 정정**(develop 기준으로 이미 거짓이던 서술) — data-layer·api/parfait-group의 `Instant::parse` 경고, g001 스펙 UiState 코드 블록의 `groupList` 타입. 유닛 561 → **602건**, 테스트 클래스 64 → 70개. 미머지: `feature/#270-topping-draft-ssot`(PR3) |
| 2026-08-22 | `ef55a58c` | Merge #311(segmentation 공통 로딩·토스트) · #325(FCM·알림 권한 철거) | delta 2건, **둘 다 머지 커밋 트리 = 브랜치 팁**(충돌 해소 편집 0건). 삽입 53줄·삭제 365줄에 신규 심볼 하나(`SegmentationEffect.ShowError`)뿐인 **삭제 라운드**이고, 두 머지가 지운 근거가 같다 — **지키고 있던 조건이 실은 값이 없었다.** **#311**: `SegmentationLoadingScreen`·`SegmentationErrorScreen`이 삭제되고 로딩은 `YGScaffoldV2(isLoading = …)`, 실패는 공통 토스트가 받는다. [ygscaffold-v2 스펙](superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)의 제외 두 항목을 **철회**한 것이고, 제외를 정당화하던 문구는 안내문 두 줄·닫기 버튼은 오버레이가 삼켜 로딩 중 눌리지도 않던 것이었다(게다가 그 시기엔 갈 곳 없는 빈 람다였다 — OQ-P-152). 실패를 상태에서 1회성 이펙트로 옮긴 근거는 OQ-P-003 ①(재시도 없는 실패를 상태로 붙들면 영영 안 걷힌다). 화면 고유 로딩 화면 **0개**(OQ-P-205 ①③ 해소), 전면 에러 화면은 `GroupListErrorScreen` 하나만 잔존. `isLoading` 귀납 기준의 첫 반례이기도 하다(온디바이스 추론이라 네트워크 왕복이 아닌데 켠다 → OQ-P-205 ② 미결). 대가: 로딩 중 닫기 도달 불가·안내 문구 소멸. **#325**: FCM 전면 철거(서비스·`app/Logger.kt`·토큰 조회·알림 채널/권한·매니페스트 등록·`firebase-messaging` 의존), Analytics·Crashlytics 유지. 근거는 결선 부재 하나 — **결선된 적 없는 기능 때문에 첫 실행마다 알림 권한을 묻고 있었다.** 발견은 방치된 방식이다: OQ-P-012가 "원격 연동 이후"로 **보류**였는데 원격 연동은 그 사이 다 붙었고 아무도 토큰을 결선하지 않았다(보류 조건이 충족돼도 스스로 열리지 않는 항목이 있다). ADR-0013은 폐기가 아니라 **FCM 축만 철회**로 정정. 유닛 **602건·클래스 70개 유지**(신규 0 — Turbine 이펙트 단언으로 바뀌었을 뿐). 실기기·실서버 확인 없음 |
| 2026-08-22 | `19cb5299` | Merge #334 (C-106 결선 스택 PR3~PR6) | delta 1건인데 **PR 넷이 실렸다** — 머지는 하나고 커밋 43개·77파일 3602/433이다. **머지 트리 = PR6 브랜치 팁 `656cbf2e`**(충돌 해소 편집 0건)라 네 브랜치의 as-built를 **재측정 없이 승격**했다(선작성 스펙 1·플랜 4 전부 보유). **앱이 처음으로 서버에 무언가를 만든다** — 확인 버튼이 발급 → S3 PUT → confirm → 배치 네 단계를 태워 "저장 없이 화면만 바뀐다"가 끝났다(OQ-P-238 ② 해소). 테두리는 서버 필드로(ADR-0025 `accepted`), 흐름 상태는 DataStore 초안 한 벌로(ADR-0026 `accepted`) 가면서 `NavKeyCanvasToppingPlace`가 인자를 잃었다. 종횡비 상수 정본 하나로 통일(OQ-P-177 ① 해소), C-001이 V2 스캐폴드로(V1 잔여 2파일 유지·호출 5→4), 배치 성공 알맹이가 갤러리 "최근"에 남아 **흐름의 두 번째 입구**가 생겼다. 되감기는 최종 리뷰가 걷었다 — Route에 매달린 토스트까지 같은 프레임에 폐기돼 실패를 못 듣는다(OQ-P-167 그대로 열림). 유닛 602 → **694건**, 클래스 70 → **80개**. `api/image.md` `done` — 표면만 있고 소비처 0인 도메인 소멸. 스펙 1건 아카이브 이동, ADR 둘 `accepted`, 미머지 추적 항목 **0**. ⚠️ 실기기·실서버 0회(이월 13 + 신규 9) |
| 2026-08-22 | `a0d584ef` | Merge #326 (nav screen transition) | delta 1건, 커밋 5·5파일 156/1, **머지 트리 = 브랜치 팁 `07f0a6a2`**(충돌 해소 편집 0건). **전환이 처음으로 앱의 결정이 됐다** — 라이브러리 기본(페이드+축소)에서 **오른쪽에서 덮고 오른쪽으로 빠지는** 밀기로 바뀌었다. `NavTransition`(`core:navigation`)이 `push`·`pop`·`predictivePop`을 한 값으로 묶고(방향이 짝을 이뤄야 하나의 동작으로 읽힌다) `metadata`로 화면별 override를 연다 — 붙는 대상이 **위에 놓이는 화면**이라는 것이 이 API의 함정이다. ⚠️ **산출물을 아무도 본 적이 없다**(OQ-P-260): 테스트가 잠그는 것은 세 슬롯이 비지 않았다는 것뿐이고, 유일한 예외 `Fade`가 붙은 `NavKeyCanvasEdit`은 짝인 `NavKeyCanvasImageSelect`와 함께 **도달 불가**라(OQ-P-129 ②) 그 근거인 공유 요소 전환도 실행되지 않는다. ⚠️ 앱 기본을 무는 세 줄이 `MainRoute`·`RootRoute`(app-preview) **두 곳에 복제**됐다(OQ-P-259 — 데코레이터 셋도 이미 같은 형태다). 문서 결과는 **최종 커밋이 지운 `Fade` KDoc을 받아 낸 것** — [navigation-flow](architecture/navigation-flow.md) 「화면 전환」 신설 + 등록 체크리스트 8번. 선작성 스펙·플랜 없어 **아카이브 이동 0건**, `api/` 무변경. 유닛 694 → **696건**, 클래스 80 → **81개**. 미머지 추적 항목 **0** |
| 2026-08-22 | `8eb2af7d` | Merge #329 (canvas bg edit api + test) | delta 1건, 커밋 13·44파일 1677/218, **머지 트리 = 브랜치 팁**(충돌 해소 편집 0건). **고른 배경이 처음으로 남고, 편집 화면이 mock을 버렸다.** 확인이 세 갈래로 갈린다 — 색은 `#RRGGBB` PATCH, 기기 사진은 **캐시 복사 → 발급 → S3 PUT → confirm** 뒤 `imageId` PATCH, 서버에 이미 있던 배경은 **요청 0건**(https는 기기가 못 읽어 다시 못 올린다). **저장이 끝나야 화면을 넘긴다**(먼저 넘기면 저장 안 된 배경을 그린 채 서 있다가 다음 조회에서 되돌아간다). 앱이 서버에 쓰는 **두 번째 경로**이고 `api/parfait.md`가 **`done`**(회전 제외 5/5). OQ-P-173 **해소**, OQ-P-193(성공 널 → 실패로 안 다루고 고른 값으로 그린다)·OQ-P-194(②는 UseCase 둘로 조율, ③은 재조회) **부분 해소** — 다만 널 폴백은 Route가 이펙트 값을 안 써서 **아직 아무것도 안 바꾼다**. **토핑 탭이 서버 캔버스를 그린다**(OQ-P-199 ① 해소) — 좌표가 Dp 오프셋 → **0~1 중심점**, 배치 규칙 셋이 `util/ToppingGeometry.kt`로 올라가 캔버스 메인·편집 탭·배치 화면이 같은 값을 본다. `NavKeyCanvasBGEdit`가 `data class(groupId, parfaitId)`로 승격되고 C-001은 **오늘 캔버스를 못 받았으면 편집을 안 연다**. entry의 `YGScaffold` 껍질을 걷고 Route가 `YGScaffoldV2`를 직접 든다(토스트 자리·인셋 이중 적용 회피). `AppError` **네 번째 갈래 `UnsupportedImage`** — 서버가 아니라 **기기에서 오는 실패**의 첫 사례이고 그 갈래만 재시도가 무의미하다. 형식 판정이 `UploadImageFormat`(확장자·contentType·시그니처) 한 자리로 모이고, 캐시 복사가 **시스템 MIME → 바이트 앞머리** 순으로 판정한다. ⚠️ **소유 판정이 축이 다른 두 id를 견준다**(OQ-P-250 — 편집 화면에서는 그 판정이 곧 게이트다). ⚠️ 마감 409가 "잠시 후 다시"로 접힌다(**OQ-P-261 신설**, C-106 배치와 처분이 갈렸다). 조치: 스펙 as-built 2건(c301 배경·c301 토핑 탭, 둘 다 드리프트 1 닫힘·`verified` 2026-08-22, **아카이브 이동 0건**), api 3표면(parfait `android_status: partial`→**`done`**·엔드포인트 표 5칸·Android 매핑 4블록 / image Android 매핑에 `BACKGROUND` 첫 소비·형식 판정·캐시 / README 도메인 표 + 소비 24건 문단. `verified`·conventions 불일치 표 불변 — 0건 유지), architecture 3건(data-layer `UnsupportedImage`·`ImageFileLocalDataSource`·Repository 인벤토리 2행·DI 2줄·`UploadImageFormat` / navigation-flow 인자 승격·entry 예외·Assisted 목록 / state-management 실패 enum 네 번째 사례·UI 타입 근거 변경). open-questions: **해소 1건**(OQ-P-173) · **부분 해소 3건**(OQ-P-193·194·199) · 마커 5건(OQ-P-146 실기기 신규 4항목·OQ-P-190 첫 실사례·OQ-P-250·OQ-P-254·OQ-P-256) · **신규 3건**(OQ-P-261 마감 409 처분 갈림 · OQ-P-262 업로드 캐시 정리 없음 · OQ-P-263 6자리 HEX 변환 함수 둘·알파 처리 정반대·로케일 결함). 유닛 696 → **737건**(+41), 클래스 81 → **87개**. 미머지 추적 항목 **0**. ⚠️ 실기기·실서버 확인 0회 |
| 2026-08-22 | `96dc215c` | Merge #339 (portrait lock + version/dependency bump) | delta 1건, 커밋 7·6파일 42/17, **머지 트리 = 브랜치 팁 `cab38993`**(충돌 해소 편집 0건). **`.kt` 0건인 첫 라운드** — 매니페스트 둘·문자열 둘·버전 카탈로그·wrapper뿐이라 테스트도 안 변했다(유닛 737·클래스 87 유지). **① 세로 고정**([ADR-0027](adr/0027-portrait-orientation-lock.md) 신설): `MainActivity`(`app`·`app-preview`)와 **카카오 `AuthCodeHandlerActivity`**에 `screenOrientation="portrait"`, `<application>`에 `PROPERTY_COMPAT_ALLOW_RESTRICTED_ORIENTATION_AND_ASPECT_RATIO_OPT_OUT`. 리다이렉트까지 붙인 이유가 알맹이다 — 빼면 **로그인 구간에서만** 회전이 살아 있고 그 구간이 A-002 리뷰의 로그인 유실 경로다(OQ-P-146 ⑨ **재현 경로 닫힘**, 다른 구성 변경은 남아 항목 자체는 유지). opt-out을 붙인 이유는 `targetSdk 36`이 sw600dp 이상에서 `screenOrientation`을 무시해 폰과 대화면 거동이 갈리기 때문. **② 표기·버전**: `app_name` 대문자화(프리뷰 포함), `appVersionName` **하향**(`versionCode`·프리뷰 버전 불변 — 스토어 미배포라 되돌릴 수 있는 자리). **③ 의존성 일괄 상향**: AGP·Kotlin·KSP·Compose BOM·OkHttp·Kakao SDK·Navigation3(alpha 계열 내)·Lottie·Hilt Navigation Compose·Kermit·Firebase BOM·Crashlytics·Gradle wrapper — 코드 수정 0건인 채 **CI `lint`·`unit-test` 통과**. 조치: **ADR-0027 신설 + README 등록**, [ADR-0006](adr/0006-navigation3-custom-navigator.md) alpha 버전 핀 문구 정정(이번 상향으로 실제로 어긋났다), open-questions **신규 2건**(OQ-P-264 opt-out이 `targetSdk 37`에 사라지는데 대화면 방침 없음·추적처가 매니페스트 TODO 한 줄뿐 / OQ-P-265 세로 고정으로 카메라 촬영이 기기 방향을 안 따른다 — `targetRotation`·`OrientationEventListener` 없음, 정책 근거도 없음) + 마커 1건(OQ-P-146 ⑨). 선작성 스펙·플랜 없고 화면 변경도 없어 **아카이브 이동 0건**, 계약 문서(`api/`)는 원격 연동 코드가 delta에 없어 무변경. 미머지 추적 항목 **0**. ⚠️ 실기기 0회 — 이번 라운드는 **유닛으로 덮을 수 없는 종류**(매니페스트 속성)이고 대화면 opt-out은 폰에서 드러나지 않는다 |
| 2026-08-23 | `f31b8c30` | Merge #324(gallery-store) · #335(topping-delete) | delta 2건, **둘 다 머지 트리 = 브랜치 팁**(#324 = `20295ba8`, #335 = `122d950b`, 충돌 해소 편집 0건)이고 **선작성 스펙·플랜 없음 → 아카이브 이동 0건**. 라운드를 묶는 축은 **오래 걸려 있던 `TODO` 둘이 같은 날 닫힌 것**이고, 닫는 방식이 갈린다 — 하나는 없던 계층을 새로 쌓았고 하나는 이미 있던 표면에 소비자를 붙였다. **#324**(12파일 416/26): 지난 캔버스의 "갤러리에 저장"이 로그 한 줄에서 **실제 저장**이 됐다(OQ-P-211 **해소**, 네 항목 전부 답). 설계의 핵심 둘 — **한 동작이 MVI 왕복 두 번**으로 갈린다(비트맵은 컴포지션만 만들 수 있어 ViewModel이 `RequestCanvasCapture`로 요청만 보내고 화면이 `toImageBitmap()`을 `SaveCapturedCanvas`로 되돌린다), 그리고 **캡처 레이어를 배경·토핑만 담는 안쪽 `Box`에 건다**(테두리·컷 도형·빈 캔버스 문구·날짜 버튼 제외) — `YGCanvas`가 "그림 vs 프레임"을 처음 갈랐고 그 경계가 곧 저장 이미지의 경계다. 쓰기는 `IS_PENDING` 등록 → 바이트 → 내림이고 중간 실패는 등록을 지운다. 권한은 API 29 미만 전용(`GalleryWritePermissionManager`, 매니페스트 `maxSdkVersion="28"`, `minSdk` 26이라 살아 있는 갈래)이고 Activity가 필요해 **Route가 캡처 비트맵을 들고 기다린다**. **#335**(6파일 163/9): 삭제 확인 모달이 곧 `DELETE`이고 **성공해야 목록에서 뺀다** — `ToppingRepository` 둘째 갈래가 소비 화면과 함께 열려 `parfait-image` 미소비가 셋 → **둘**(위치·테두리 수정), **앱이 서버 데이터를 지우는 첫 경로**다(OQ-P-199 ③ 해소 = "확인 모달 시점에 즉시"). ⚠️ **삭제만 즉시 영구이고 실패가 화면에 안 닿는다**(OQ-P-270 — "그만두기"로 나가도 안 돌아오는데 403·409·404가 전부 로그 한 줄, 같은 화면 배경 저장은 토스트라 처분이 갈렸고 409는 같은 코드에 **세 번째 처분**). ⚠️ **같은 커밋이 `TOPPING_MAX_SCALE`을 근거 없이 삭제**해 상·하한 없는 축이 둘이 됐다(OQ-P-271). ⚠️ 캡처가 **지금 그려진 것**을 복사해 배경 로딩 전이면 그대로 담기고 해상도도 기기 종속(OQ-P-272) · **얼럿 호스트·문자열 셋이 트리거 없이 유입**(OQ-P-273) · **저장이 API 29를 경계로 위치도 보호도 갈림**(OQ-P-274). 조치: 사후 스펙 1건 작성(`implemented`·archive) [c001-canvas-gallery-save](superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md) + README 등록, [c301-topping-edit-tab](superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)에 as-built 재정정 절(드리프트 2 부분 해소 · **드리프트 4의 "크기는 클램프한다" 전제 정정**) + README 행 갱신, [c201-canvas-calendar-server](superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md) 드리프트 1 해소 마커, `api/parfait-image.md` Android 매핑(소비처 배치+삭제 둘 · 실패 무반응 경고 · 배율 상한 소멸 마커)·`api/README.md` 도메인 표 + 소비 25건 문단(**`verified`·`android_status` 불변**), architecture 2건(data-layer 시스템 미디어가 **읽기 전용이 아니게 됨** + `ToppingRepository` 인벤토리 / design-system `captureGraphicsLayer`·`overlayContent` 호스트 둘). open-questions: **OQ-P-211 해소됨**, OQ-P-199 **③ 해소 + ② 전제 정정**(좌표 PATCH는 2026-08-15부터 계약에 있다), **신규 5건**(OQ-P-270~274), `oq-next` 270 → 275. 유닛 737 → **745건**(+8), 클래스 87 유지. ⚠️ **실기기·실서버 확인 0회 — 캡처 결과물·권한 다이얼로그·`MediaStore` 쓰기가 한 줄도 안 잠겼다.** 미머지: 없음(as-built 대기 브랜치 0건 — 활성 계획 [c103 다중 후보 PR1·PR2](superpowers/plans/archive/2026-08-23-c103-pr1-multi-subject-domain.md)는 아직 브랜치가 없다) |
| 2026-08-23 | `d634efd3` | Merge #336 (topping-edit-c305 api) | delta 1건, 커밋 1(+브랜치 안 develop 병합 1)·6파일 318/32, **머지 트리 = 브랜치의 develop 병합 커밋 `dd29dce5`**(충돌 해소 편집 0건)이고 **선작성 스펙·플랜 없음 → 아카이브 이동 0건**. **편집 결과가 처음으로 남는다 — 그리고 남지 않은 것만 조용해졌다.** C-301 편집 탭이 2026-08-16 이후 일주일 동안 고친 것을 버리던 화면이었고, 하루 전 #335가 삭제 하나만 열어 둔 상태에서 이번에 **이동·크기·회전이 확인 버튼에서 PATCH로 나간다** — `ToppingRepository.update`·`UpdateToppingUseCase` 신설로 `parfait-image` 미소비가 둘 → **하나**(테두리 수정)가 되고 **OQ-P-199가 세 항목 전부 답을 얻어 해소됨**. 설계 핵심 셋: **① 바뀐 것만 보낸다**(조회 응답 스냅샷 `confirmedToppings`를 렌더링과 별도로 들고 확인 시점에 대조 — 안 건드린 토핑은 요청 0건), **② 토핑끼리는 `async`+`awaitAll` 병렬이되 배경 저장보다 완전히 앞**(둘을 얽으면 한쪽만 실패한 경우를 갈라 다뤄야 해서다 — 코드 주석이 그 판단을 적어 두었다), **③ `positionZ`를 안 보낸다**(부분 병합이라 서버 겹침 순서 유지, 앱에 z 조작 경로 없음). 확인 버튼이 한 단위가 되며 코루틴 키 `SAVE_BACKGROUND_KEY` → `CONFIRM_KEY`. ⚠️ **실패가 화면에 안 닿는데 확인은 성공한다**(OQ-P-275 — 로그 한 줄 뒤 배경 저장이 이어져 성공하면 화면이 넘어가고, 캔버스 메인은 재조회로 옛 좌표를 그린다. 같은 버튼 안에서 배경은 토스트+잔류, 토핑은 무반응+이동이고 마감 캔버스에서는 **같은 409가 한 번의 확인에서 두 처분** — OQ-P-261의 처분이 넷이 됐다). ⚠️ **다섯 중 테두리 재편집만 혼자 남았다**(OQ-P-276 — 변경 판정이 넷만 비교하고 `borderLayers`·`editedImagePath`는 요청에도 없다. 표시 쪽 OQ-P-254와 같은 값을 두고 갈라져 있다). ⚠️ **어제 근거 없이 지운 상한이 오늘 요청 값이 되고 회귀 테스트로 굳었다**(OQ-P-271 — 되살리려면 그 테스트를 함께 지워야 한다). 회전 무제한도 화면 안 문제가 아니게 됐다(OQ-P-241 ③). 조치: 스펙 as-built 2건([c301-topping-edit-tab](superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) 재정정 절 — 드리프트 2 거의 해소·드리프트 4 마커, related_code 6줄 추가 / [c301 배경 스펙](superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) 재정정 절 — 확인 버튼이 더는 배경만 다루지 않음, `verified` 2026-08-23) + specs README 2행, api 2표면(`parfait-image.md` Android 매핑 — 소비처 셋·미소비 하나·실패 무반응 경고·요청 값이 된 두 축 / `api/README.md` 도메인 표 + 소비 26건 문단. **`verified`·`android_status` 불변**), architecture 1건(data-layer `ToppingRepository` 인벤토리에 `update` + 셋째 갈래 노트). open-questions: **OQ-P-199 해소됨**, 마커 5건(OQ-P-241·OQ-P-254·OQ-P-261·OQ-P-270·OQ-P-271), **신규 2건**(OQ-P-275 저장 실패가 성공처럼 보임 · OQ-P-276 테두리만 안 나감), `oq-next` 275 → 277. 유닛 745 → **751건**(+6), 클래스 87 유지. ⚠️ **실기기·실서버 확인 0회 — 부분 실패(토핑 셋 중 하나만 403)가 유닛으로 안 잠겼다.** 미머지: 없음 |
| 2026-08-24 | `34bf1939` | Merge #342 (c103-multi-subject-ui) | delta 1건, 커밋 13·17파일 1074/211, **머지 트리 = 브랜치 팁 `8fe67476`**(충돌 해소 편집 0건). **선작성 스펙·플랜이 있는 첫 라운드가 여섯 회차 만에 돌아왔고, 스택 둘(PR1 data·domain / PR2 UI)이 한 PR로 합쳐져 들어와** develop 이력에 스택 경계가 남지 않았다. **사진에 피사체가 여럿이면 이제 고를 수 있다** — 정책에는 처음부터 있던(위키 [[누끼-따기]], 기능정의서 v5의 C-103-select) 갈래를 열었다. 후보를 하나로 접던 자리는 **ML Kit 옵션 한 곳**이었고 `enableMultipleSubjects` + `enableSubjectBitmap`으로 옮기며 그 위에 선 경계들을 함께 넓혔다. **화면 ID는 쪼개지 않았다**(`NavKeySegmentation` 하나가 후보 수에 따라 박스를 1개 또는 N개, 후보 1개면 전과 픽셀 단위로 같다). 설계 핵심 셋: **① 탐지와 저장이 갈렸다**(`segmentImage`는 디스크를 안 건드리고 신설 `persistSubject`가 **탭한 하나만** 저장 — 진입 즉시 후보 수만큼 PNG를 떨구지 않는다. `SegmentationResult`는 `subjectBounds`를 잃고 경로 둘만 남고, 좌표는 새 도메인 모델 `SegmentationCandidate`가 나른다) · **② 판단이 드는 자리를 순수 함수로 뺐다**(`filterCandidates` · `scaledRectOrNull`·`pickCandidateIndex`, JVM 유닛 15건. **이 화면에는 UI 테스트가 0건**이라 탭 판정은 깨져도 조용한 자리였다. 탭은 **면적 최소**를 고른다 — "뒤에서부터 첫 히트"와 결과가 같지만 그러면 컴포넌트의 올바름이 필터 정렬에 매달리고, 앞에서부터면 큰 후보 안의 작은 대상을 아예 못 고른다) · **③ 선택 시점의 순서가 계약이다**(저장 → 초안 기록 **완료** → 로딩 해제 → 이동. 확인 화면이 구독만 하므로 이 화면이 초안의 유일한 writer이고, 어기면 다음 화면이 "다음"을 잠근 채 뜬다. 이동이 `goTo`라 `isLoading`은 성공·실패·예외 세 갈래에서 각각 내린다). ⚠️ **실기기가 네이티브 크래시를 드러냈다**(OQ-P-268 해소) — 전경 마스크 옵션과 다중 후보 옵션을 **한 요청에 함께 켜면 `SIGSEGV`**(Galaxy A35 / Android 16). 스택이 전부 모듈 네이티브라 `try/catch`도 Crashlytics도 못 잡고 `logcat -b crash`에만 남는다. 전경 마스크는 **후보 0건일 때의 2차 요청**으로 남겼고(세그멘터를 한 흐름에서 두 번 열 수 있다), 정상 경로는 쓰지도 않던 `FloatBuffer`가 사라져 **오히려 가벼워졌다.** ✅ **실기기 확인이 0회가 아닌 드문 라운드**(다중 박스·`C-103-Error` 화면). 계획 밖에서 들어온 실패 화면이 **PR #311이 삭제했던 `SegmentationErrorScreen`을 되살려**(OQ-P-153 ①②③ 해소) 이틀 만에 그 판정이 절반 뒤집혔다 — **대상을 아예 못 얻은 실패만 화면**(1회성 효과가 아니라 `isError` 상태여야 재구성에서 산다)이고 고른 뒤의 저장 실패는 토스트 그대로다. 조치: **아카이브 3건**(스펙 `implemented` + 플랜 둘 `done`), **architecture 4건**(data-layer 계약 4→**5**·`subjectBounds` 제거·필터 계층 / design-system #311 판정 절반 뒤집힘, **로딩 화면 0개는 유지** / state-management 순서 계약 사례 / navigation-flow 화면 ID를 목적지로 안 쪼갠 판단 + 이동이 이펙트 수신으로), **ADR 2건**(0012 옵션 전환·크래시 제약 / 0011 **도메인 모델이 `BitmapWrapper`를 다시 물었다** — 2026-08-14의 "적용 범위가 줄었다"가 되돌아왔다), 전처리 계획 베이스 `develop` 정정. `api/`는 원격 연동 코드가 delta에 없어 **미변경**. open-questions: **해소 0건**(OQ-P-269 ①만 부분 확인), 신규 0건 — 이 라운드의 미결 다섯은 스펙 작성 시점에 이미 등재됐다(OQ-P-266·267·268·269·277). 유닛 751 → **775건**(+24), 클래스 87 → **89개**. ⚠️ 남은 넷은 전부 실기기 항목이다. |
| 2026-08-25 | `a5e8a760` | Merge #349 (segmentation-preprocessing) | delta 1건, 커밋 5·9파일 177/6, **머지 트리 = 브랜치 팁 `7f74458c`**(충돌 해소 편집 0건). **선작성 스펙이 부분만 머지돼 아카이브 이동이 0건인 라운드** — [전처리 스펙](superpowers/specs/2026-08-23-segmentation-preprocessing.md)이 항목마다 근거 등급과 철회 조건을 매겨 두었고 **근거가 확정된 1단계(Task 1~4)만** 들어왔다. 스펙 `draft` → **`in-progress`**, 계획은 1단계 체크박스만 닫고 active 유지. **촬영이 최대 품질로 들어오기 시작했고**(`CAPTURE_MODE_MAXIMIZE_QUALITY` + `setJpegQuality(100)` — 기본 빌더가 JPEG로 찍어 손실의 첫 세대가 이미 있었다), **API 28 미만 갈래가 누운 사진을 세운다**(`rotatedToUpright`, 회전 전 판 즉시 회수. `ImageDecoder` 갈래는 OQ-P-280이 미판정이라 그대로), **최근 이미지 `SOURCE` 확장자가 바이트 판정으로**(`extensionOf` — 스펙은 "PNG 채택 시"였으나 PNG 없이도 참인 결함이라 계획이 앞당겼다). **PNG 저장 전환·512 확대·API 28 이상 회전은 미착수**(사진 세트 측정이 게이트). as-built 이탈 둘은 후속 커밋이 스스로 고쳤다(KDoc 근거를 실제 호출 경로로, `null` 스트림 갈래에 경고 로그). 테스트 775 → **781건**(파일 89 → 90). 상시 문서 넷 갱신(module-structure·data-layer·ADR-0014·api/image.md), 미결 신규 0·갱신 2(OQ-P-280·283). **미머지 추적 항목이 넷 생겼다** — 후처리 스택 셋 + 알파 정련 하나 |
| 2026-08-25 | `37ea970b` | Merge #350 (#345 permission-screen-inset) | delta 1건, `fix` 커밋 5(+브랜치 안 develop 병합 1)·3파일 59/48, **머지 트리 = 브랜치 팁 `5ddfdf77`**(충돌 해소 편집 0건). **선작성 스펙·플랜 없는 버그 라운드 → 아카이브 이동 0건**, 테스트 781건 유지. **세 주 전에 "사라졌다"고 적어 둔 결함이 다른 갈래에 살아 있었다** — [c102 스펙](superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)이 2026-08-04에 인셋 이중 적용 해소를 적었으나 걷힌 것은 **목록 갈래**였고 `GalleryPermissionRequestComponent`는 Route가 준 `innerPadding` 위에 `windowInsetsPadding(systemBars)`을 계속 걸어 닫기 버튼이 상태바 높이만큼 내려앉아 있었다(#345). **카메라는 기전이 반대** — entry가 `innerPadding`을 일부러 안 주므로 컴포넌트가 무는 것은 맞고 **무는 자리**(닫기 `Row` → 바깥 `Box`)가 틀렸다. **PR 요약 밖 동작 변경 1건**: 가이드 토스트가 `LaunchedEffect(state.hasPermission)` 게이트를 얻어 권한 거부 화면 위에 뜨지 않는다(갤러리는 진작 그랬다 → 카메라가 맞춰졌다). ⚠️ **실기기 확인 0회**(비교 이미지의 After가 이슈 디자인 이미지, OQ-P-301 신설). `permanentlyDenied`·`onClickGrantPermission` 미사용은 컴포넌트를 다시 짜고도 그대로. 문서: c101·c102 스펙 as-built + [navigation-flow](architecture/navigation-flow.md) 인셋 사례 2건 + open-questions 2건 갱신·1건 신설 |
| 2026-08-26 | `df3f4cbe` | Merge #368 (ignore-release-keystore) | delta 7건(#351·#352·#353·#354·#357·#358·#368), 17파일 383/153, **일곱 머지 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). **선작성 스펙·플랜 없는 라운드 → 아카이브 이동 0건**, 유닛 781 → **785건**(전부 #352), 계측 12 → **14건**(#351, CI는 컴파일만). **오래 걸려 있던 미결 넷이 하루에 닫혔다** — **#358**이 `usesCleartextTraffic`을 걷고 `network_security_config.xml`을 놓아 OQ-P-076을 닫았는데 **문서가 못 박은 순서(base URL 먼저)를 뒤집고도 안 끊겼다**(`debug-overrides`가 디버그 빌드를 예전 그대로 둔다. ⚠️ 좁히기는 권고보다 넓다 — 도메인 한정이 아니라 디버그 전체 + 사용자 인증서). **#352**가 G-001 온보딩 툴팁을 `groups?.isEmpty() == true`로 결선해 死필드 `isTooltipVisible`을 정본으로 만들고 **미조회(`null`)를 0건으로 안 센다**(ADR-0023이 `null`/`emptyList()`를 가른 근거가 처음 소비된 자리, 유닛 4건). **#351**이 `YGCanvas.background`를 nullable로 내려 화면의 `DEFAULT_CANVAS_BACKGROUND` 복제를 없애고 폴백 그림을 `Gray100` → **흰 바탕**으로 바꿨다(⚠️ 빈 안내판이 `isEmpty && background == null`로 좁아져 직교 플래그 원칙에서 이탈 + 정책 근거 없음 → OQ-P-304). **#357**이 배치 화면에서 `GetTodayParfaitUseCase`를 재조회해 실제 배경·기존 토핑을 그려 OQ-P-240 셋에 다 답했으나 ⚠️ **새 경로에 테스트 0건**(스텁이 조회를 조용히 실패시켜 기존 테스트가 초록) → OQ-P-303. **#354**가 `./error.jks` 가짜 폴백을 걷고 `validateSigning*`에 `doFirst`로 안내를 얹었으나 ⚠️ **태스크 이름 일치라 발화 미확인** → OQ-P-305. **#353** 약관 모두동의 행 각짐(`shape`+`clip` 동시 제거), **#368** `.gitignore`에 keystore 추가(**키 파일은 트리에 없다**). 문서 조치: open-questions 해소 4 + 갱신 4(OQ-P-079·102·181·272·302) + 신규 3(OQ-P-303·304·305), spec 4건 as-built(g001-group-list·c001-canvas-today-detail·c106-topping-place·intro-term-agree), design-system 캔버스 절, ADR-0003 as-built, api conventions·auth 전송 절, specs README 4행. 미머지 추적 항목 **넷 그대로**(네 브랜치 `origin`에 살아 있음을 재확인) |
| 2026-08-26 | `cbb48cd8` | Merge #366 (#365 font) | delta 1건, 커밋 1·5파일 97/0 + **`.ttf` 넷 바이너리 교체**, **머지 트리 = 브랜치 팁**(충돌 해소 편집 0건). **`.kt` 0줄 변경 → 유닛 785·계측 14건 유지, 선작성 스펙·플랜 없어 아카이브 이동 0건, 코드 심볼 드리프트 0건**(파일명 유지로 `R.font`·`YGFontFamily` 무변경). SUIT 원본의 **빈 글리프(zero-contour) cmap 매핑** 때문에 해당 문자가 **투명하게** 찍히던 것을(#365) 그 매핑만 걷어낸 수정본으로 갈아 **시스템 폰트 fallback**이 걸리게 했다. ⚠️ 대가 둘 — fallback 문자는 기기 폰트로 그려져 **한 화면에 두 글자체가 섞이는데 실기기 확인이 없고**, OFL 1.1 조건으로 생긴 `core/designsystem/OFL.txt`가 **APK에 안 실리고 앱에 오픈소스 고지 화면도 없다**. ⚠️ **회귀 감지선 0건** — 바이너리라 diff로 못 읽고 파일명·버전·아웃라인이 같아 원본을 되넣어도 빨갛게 되지 않는다. 문서 조치: design-system 폰트 절(수정본 표기·모듈 루트 `OFL.txt` 등재·대가 둘) + open-questions 신규 2건(OQ-P-306 라이선스 고지 · OQ-P-307 회귀 감지선·fallback 미관측). 미머지 추적 항목 **넷 그대로** |
| 2026-08-26 | `c55e10bc` | Merge #372(release build) · #374(version bump) · #376(sync backend api) | delta 3건, 13파일 144/54, **세 머지 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 선작성 스펙·플랜 없어 **아카이브 이동 0건**, 유닛 785 → **789건**(전부 #376), 계측 **14건** 유지. **작은 라운드인데 그 옆에서 이 감사의 전제가 깨졌다.** **#374**가 앱 버전을 0.0.1/1 → **0.0.3/3**으로 올렸는데 **2가 어느 브랜치에도 없다** — 그 결번을 따라가니 경량 태그 `0.0.3`이 나오고, 그것이 가리키는 `origin/release/version-0.0.3-3`이 **develop에 없는 45커밋**을 담고 있다. 받은 머지 여덟 중 **넷이 이 문서가 세 회차 연속 "미머지"로 세어 온 세그멘테이션 브랜치들**(계획이 요구한 순서 그대로 들어갔다)이고 다섯째 `feature/toast-position-fix`는 문서에 이름조차 없다 → **OQ-P-311 신설**(감사 대상을 develop 하나로 둘지가 다음 회차의 첫 결정. 이번 회차는 그 브랜치를 감사하지 않았고, 넷의 스펙·플랜 `status`도 안 건드렸다 — 아카이브 기준이 develop 머지라서다). **#372**: 브랜치가 `#283-check-release-build`이고 커밋 하나가 **릴리즈에서만 터지는 lint 실패**(`Instantiatable` — 매니페스트가 직접 선언한 카카오 `AuthCodeHandlerActivity`의 상속 체인)를 고쳐 **"아무도 release를 만들어 본 적 없다"가 깨졌다**(OQ-P-074 ② 마커). ⚠️ `appcompat` 선언은 **AppCompat 도입이 아니다** — 런타임 의존과 테마(`TransparentCompat`)는 카카오 AAR이 이미 병합으로 넣고 있었고 더한 것은 컴파일 의존뿐. 같은 PR이 **리소스 축소를 처음 켰다**(`isShrinkResources`, 그전까지 minify만 → OQ-P-123 ③의 괄호가 이제야 참) — ⚠️ 축소는 조립이 아니라 **실행에서** 실패하는데 산출물 실행 기록 0건·`keep.xml` 없음(**OQ-P-308 신설**), debug의 `proguardFiles`는 무동작. `firebase-crashlytics-ndk`도 붙었으나 겨냥 대상이 CameraX·DataStore의 `.so`이고 심볼 업로드를 꺼 **주소만 남는다**(**OQ-P-309 신설**). **#376**: 하루 전 서버가 더한 `placedBy.ownerType`을 **같은 날 읽었다 — 계약 delta와 앱 반영이 같은 날 붙은 첫 사례**. 매퍼가 `"ME"` 여부를 `CanvasToppingVO.isMine`(비널 `Boolean`)으로 접어 **문자열이 `:data` 경계를 못 넘는다**(모르는 값·`null`은 거짓 — 여는 쪽으로 틀리면 남의 토핑을 만지게 된다). `CanvasMainViewModel`의 상수 `false` 확장 함수가 사라지고 `CanvasBGEditViewModel`은 `GetMyAccountFlowUseCase` 의존과 진입 `first()` 대기를 버렸다(**축이 다른 비교**가 그 자리였고 이 화면에서 판정은 게이트였다). OQ-P-250 **①② 해소·③ 잔존** — 본인 토핑 탭이 "잘못 Spotlight로 간다"에서 **"아무 일도 안 한다"**가 됐다(C-305 부재). 같은 PR이 `http/README.md` `base_url`을 HTTPS 도메인으로 옮기고 `network_security_config.xml`에 "호스트를 안 가린다" 경고를 주석으로 박았다(OQ-P-302 마커). 조치: 스펙 as-built 3건(c202·c301 토핑 탭·c301 배경, `verified` 2026-08-26) + specs README 3행, api 3표면(parfait Android 매핑·미지 값 폴백 셋·DataSource 29 케이스·미결 절 / README 도메인 문단 / conventions 필드 소비 간격 0일 + 전송 절 HTTPS 안내. **`verified`·`android_status`·불일치 표 0건 전부 불변**), ADR 2건 as-built(0003 릴리즈 빌드·appcompat·버전 / 0013 crashlytics-ndk) + ADR README 2행, architecture 1건(data-layer 매퍼 규약 실사례·DataSource 케이스 수). open-questions: **부분 해소 1건**(OQ-P-250 ②) · 마커 3건(OQ-P-074·123·302) · **신규 4건**(OQ-P-308~311), `oq-next` 308 → 312. ⚠️ **실기기·실서버 확인 0회 그대로** — 이번 라운드가 켠 축소·NDK 수집은 **유닛으로 덮을 수 없는 종류**다 |
| 2026-08-26 | `bf06c830` | Merge #371 (toast position fix) | delta 1건, 커밋 2·4파일 37/3, **머지 트리 = 브랜치 팁**(브랜치가 develop 따라잡기 병합을 자기 안에 품는다, 충돌 해소 편집 0건). 선작성 스펙·플랜 없어 **아카이브 이동 0건**, **신규 테스트 0건**이라 유닛 789·계측 14건 유지. **여섯 날 전의 결정이 되돌려졌고, 그 결정을 적어 둔 문서 한 줄이 그동안 거짓이었다.** 카메라·갤러리가 `YGToastHost`를 **다시 자기 레이아웃 안**으로 가져갔다 — Route가 `rememberYGToastPolicy()`를 만들어 **Screen에 넘기고** Screen이 자기 프레임 `Box`(뷰파인더 자리·그리드 자리) 안에 `TopCenter`로 심는다. 2026-08-20 #309가 걷어 스캐폴드로 올렸던 그 형태이고, 되돌린 이유는 **스캐폴드 호스트가 상태바 인셋 바로 아래에 떠서 헤더 행(날짜·닫기)을 덮었기** 때문이다. ⚠️ **그래서 c102 스펙의 "보이는 위치는 사실상 그대로다"와 c101 스펙의 "위키 Toast 공통 정책에는 이쪽이 맞는다"가 둘 다 틀렸다** — 두 "상단"이 다른 상자였고(상태바 아래 vs 그리드 프레임 윗변), 위키 [[toast]]는 노출 방향만 정하지 기준 프레임을 정하지 않는다. **이번 회차가 고친 것이 그 두 문장이다.** ⚠️ **처방이 브랜치 안에서 갈렸다** — 첫 커밋은 `YGScaffoldV2`에 `toastTopPadding`을 더해 Route가 헤더 높이를 dp로 복제 계산했고, 두 번째 커밋이 통째로 되돌려 위치 계산 없이 프레임 안에 심었다(**스캐폴드는 결국 무변경**). 남긴 것 셋 → **OQ-P-312 신설**: 관용구가 셋이 됐고(스캐폴드 기본 호스트 / `YGCanvas.overlayContent` / Screen이 직접 심기) 규칙이 없다 · Route가 정책을 안 넘겨 스캐폴드가 **발행 불가능한 호스트**를 화면마다 하나씩 만든다(끄는 수단 없음) · 호스트가 **권한 허용 갈래 안에만** 있어 권한 거부 화면엔 없다(지금은 발행 조건이 권한을 요구해 증상 0, 그 결합이 코드에 안 적혀 있다). **미머지 넷은 그대로**이고 release 브랜치와의 격차는 45 → **43커밋**(줄어든 둘이 이 브랜치 — 두 계보에 각각 머지돼 **양쪽에 다 있다**, OQ-P-311 마커). 조치: 스펙 as-built 2건(c101·c102, `verified` 2026-08-26, **둘 다 낡은 단정 정정 포함**) + specs README 2행, architecture 1건(design-system `YGScaffoldV2` 절에 🔁 되돌림 블록), open-questions 마커 2건(OQ-P-167 ①의 전제가 두 화면에서 깨짐 · OQ-P-311) + **신규 1건**(OQ-P-312), `oq-next` 312 → 313. `api/` 무변경(원격 연동 코드 0건). ⚠️ **확인 수단이 실기기 눈뿐이다** — 배치는 유닛으로 못 덮고 CI는 계측을 컴파일만 한다(OQ-P-102 ②) |
| 2026-08-27 | `5dc9fbec` | Merge #389 (canvas-main-alpha-hit) | delta 3건(#388·#390·#389) — 전부 [토핑 알파 판정](superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md) 한 스펙의 스택 PR이고, **#390은 develop이 아니라 #389 브랜치로 머지**돼 develop 첫 부모 선에 붙은 것은 둘이다. 17파일 1,208/129, 세 머지 다 트리 = 브랜치 팁, 유닛 789→819(+30). 토핑마다 걸린 `clickableYGNoRipple`과 딤 클릭을 걷고 **레이어 전면 `pointerInput` 하나**가 알파 마스크로 겹침 순서를 판정한다 — "레이어가 캔버스 포인터를 독점한다"가 새 계약. 사라진 클릭 시맨틱스는 토핑·딤에 다시 붙이고 판정을 끈 배치 화면(`hitTestEnabled = false`)에는 안 붙인다. **계획의 "실행 뒤 달라진 것" 표가 리뷰 반영 열넷을 이미 담고 있어 드리프트는 셋**(상수 개명 `TOPPING_OUTLINE_STAMP_COUNT` · 콜백 유무 판단 → `hitTestEnabled` 파라미터 · 배경 편집 마스크를 내 토핑만 요청) → 스펙 as-built 표 신설. 스펙 `implemented`·계획 `done`(frontmatter 자체가 없어 신규 작성) + 양쪽 archive 이동(링크 `../`→`../../` 보정)·README 등록. 배경 편집이 `YGToppingCutoutImage`로 갈아타 **OQ-P-254 해소**, OQ-P-203 ②는 대상 소멸. c202·c301 아카이브 스펙 as-built 정정(딤·토핑 클릭 → 판정 오버레이, 테두리 그림, 접근성 부분 해소), design-system(상수 공개·소비처 넷째)·module-structure(판정을 domain으로 안 올린 근거·`model/` 신설)·ADR-0025 머지 마커. open-questions 신규 5건(OQ-P-313~317). 미머지: 세그멘테이션 넷 그대로(release에만, 43커밋 차) |
| 2026-08-27 | `4da18230` | Merge #363 (segmentation-alpha-refinement) | delta 1건인데 **선작성 스펙 셋이 실렸다** — 커밋 44개·20파일 3685/223, **머지 트리 = 정련 브랜치 팁**(충돌 해소 편집 0건). 세 회차 연속 "미머지 추적 항목"이던 세그멘테이션 넷이 rebase 로 하나가 되어 develop 에 들어왔다. **누끼가 처음으로 모델 출력을 고친다** — `AlphaPostProcessor.kt`·`AlphaComponents.kt`·`AlphaRefine.kt`·`AlphaComposite.kt` 신설, 이진화 축소 → area opening → 팽창 → keep 적용 → 1차 측정 → 가이드 필터 정련 → 침식 → 2차 측정. 커널은 `Bitmap`·ML Kit 타입을 모르고 원본 휘도는 `GuidanceProvider` 로만 들어온다(주·폴백 둘 다 `origin` 에서 읽는다 — 도려낸 판을 주면 `I ≡ p` 라 정련이 틀린 경계를 강화한다). 후보 필터가 사각형 면적 → **알파 커버리지**, 동일 bounds → **IoU 병합**(`SegmentationCandidate.coverageAlphaSum` 신설). 취소 확인이 `checkCancelled` 콜백 → `suspend` + `currentCoroutineContext().job.ensureActive()`(⚠️ `get(Job)?` 계열은 `Job` 부재를 조용히 통과시켜 확인이 no-op 이 되고도 초록이라 배제). 조치: **스펙 3건 `implemented`·계획 3건 `done` + 양쪽 archive 이동**(링크 `../`→`../../` 보정, 알파 커널 계획은 **frontmatter 자체가 없어 신규 작성**, 전처리 계획도 같은 이유로 신규 작성하고 active 유지)·README 3행씩 아카이브 등록, 후처리 스펙에 **as-built 4건**(`toCandidates`→`toCandidatePairs`+`buildCandidatePair` · `maskSubjectPixels`→`maskSubjectAlpha` · API 절 시그니처를 뒤 두 라운드가 덮음 · `AlphaComposite.kt` 신설), data-layer 세그멘테이션 절에 커널 문단 + `related_code` 5심볼, ADR-0012 에 "모델 하나로 충분하지 않다" 절 + `related_spec` 3건. open-questions: **신규 2건**(OQ-P-318 확인 없이 두 패스 도는 루프 · OQ-P-319 콜백↔`suspend` 차이를 잴 하니스 없음), OQ-P-311 ③ **해소**·①② 유지(release-only 43커밋 그대로이고 **내용까지 갈렸다**), 출처 링크 16건 archive 경로 보정. 유닛 819 → **926건**(+107, 파일 94 → 100), 계측 14 그대로. `api/` 무변경(원격 연동 코드 0건). ⚠️ **값의 근거는 여전히 없다** — 임계·반경·정칙화·축소 하한을 판정할 실기기 사진 세트가 0회(OQ-P-287~300 열넷 그대로). 미머지 추적 항목 **0** |
| 2026-08-28 | `84a89728` | Merge #369(토핑 편집 재구현) · #400(본인 토핑 탭) · #398(배율 하한) | delta 3건, 18파일 441/31, **세 머지 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 선작성 스펙·계획 없어 **아카이브 이동 0건**, 유닛 926 → **931건**(+5)·계측 **14건** 유지. **미뤄 두었던 `TODO` 셋이 하루에 함께 닫혔다.** **#400**: 본인 토핑 탭이 `NavKeyCanvasBGEdit(initialToppingId)`로 이어져 **C-301 편집 화면의 토핑 탭**이 그 토핑을 선택한 채 열린다 → OQ-P-250 ③ 해소(셋 다 닫힘). ⚠️ **예고했던 "C-305 화면 라운드"가 아니다** — 새 화면 없이 기존 화면이 역할을 받았고, `isViewingToday` 가드 때문에 **지난 캔버스에서는 여전히 무반응**이다(정책에 없는 조건) → **OQ-P-326 신설 ③**. **#369**: 확인 버튼이 테두리 PATCH까지 부르며 `updateToppingIfChanged`의 판정이 위치·테두리 **둘로 갈렸다**(서버 API가 두 엔드포인트라서다) → `parfait-image.md` `android_status` **`done`**(4/4 소비), 소비처 27건, OQ-P-276 ①③ 해소·② 잔존. 같은 PR이 `TODO(#274)`도 닫았다 — `decodeImage`가 스킴을 갈라 `https://`면 신설 `RemoteImageDownloadDataSource`(전용 `@DownloadClient`, Retrofit 밖 raw OkHttp **둘째 자리**)로 받아 디코드한다. ⚠️ **테두리를 접는 규칙이 그리는 규칙과 어긋난다**(저장 `lastOrNull()` vs 렌더 `firstOrNull()`, 겹은 `UndoRedoStack`이라 둘 이상 쌓인다) → **OQ-P-324 신설**. ⚠️ 다운로드가 본문을 `bytes()`로 통째 힙에 올리고 상한이 없다(업로드 스트리밍과 대칭 깨짐) → **OQ-P-327 신설**. **#398**: `TOPPING_MIN_SCALE` 0.5 → **0.05**인데 근거가 커밋 메시지에도 KDoc에도 없고, 짧은 변 48dp에서 역산하는 배치 화면과 갈렸다 → **OQ-P-325 신설**. ⚠️ **선작성 문서 셋이 하루 만에 낡았다** — 폴링 스펙·PR3 계획의 `updateDirtyToppings`가 위치 PATCH 하나만 불러 **그대로 구현하면 테두리 저장이 회귀**하고, 시딩 목록에 `selectedTab`·`selectedToppingId`가 빠졌다. 전처리 스펙은 정규화를 `decodeUriToBitmap` 안에 두라고 적는데 원격 갈래가 그 함수를 안 탄다(세 문서에 ⚠️ 삽입) → **OQ-P-326 신설 ①②④**. 신규 미결 4건(OQ-P-324~327) |
| 2026-08-28 | `627e1867` | Merge #393·#394(그룹 생성·참여 수정) · #396(목록 토핑 Fit) · #395(시스템바) · #397(토핑 회전·크기조절) | delta 5건, 28파일 706/418, **다섯 머지 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 선작성 스펙·계획 없어 **아카이브 이동 0건**, 유닛 931 → **942건**(+11, 파일 98 유지)·계측 **14건** 유지. **버그를 고치는 김에 미결이 닫힌 회차다.** **#393·#394**: A-005·S-102 가 요청 직전에 확인 팝업을 닫고 로딩 오버레이 + 토스트로 옮겨 `isEnabledButton` 호출자 **0곳**(OQ-P-137 ④ 해소), Route 가 `YGScaffoldV2` 를 쥐며 V1 잔여 **1파일 1호출**(OQ-P-204), A-005 가 `GroupCreateError` 2종을 얻어 OQ-P-167 ④ 가 그룹 진입에서 비었다. A-005 닉네임 필드가 열려 NavKey 인자가 표시값 → **초기값**이 됐고 OQ-P-253 의 근거가 흔들렸다. S-102 의 `TODO(닉네임 적용 실패 안내)`는 `NICKNAME_NOT_APPLIED` 로 닫혔으나 안내가 **이동을 지연**시키고 대기 상수가 `YGToastPolicy` 와 갈려 **OQ-P-328 신설**. **#396**: `YGToppingGroup.Remote` 가 `Crop` → `Fit`, `clip` 은 방어선으로 남는다 — OQ-P-316 ②가 선행이라던 ① 없이 먼저 일어났다. **#395**: [ADR-0028](adr/0028-system-bar-light-fixed.md) 대로 두 `MainActivity` 가 `SystemBarStyle.light` 를 명시 — 문서가 먼저 있고 코드가 따라온 유일한 사례. **#397**: 회전이 접선 투영, 크기조절이 핸들 거리 비율로 바뀌며 감도 상수 둘 소멸 · 환산이 화면으로 이동(`OnToppingResize`·`OnToppingRotate`) → OQ-P-241 부분 해소(넷 → 둘), 스펙 셋의 인텐트 이름·`resizeOutwardDirection` 정정 |
| 2026-08-30 | `27e85d0d` | Merge #405(스포트라이트 토스트 교체) · #406(갤러리 상단바 Title) · #407(첫 조회 로딩) · #409(앱 버전 0.1.1) | delta 4건, 14파일 383/59, **네 머지 전부 트리 = 브랜치 팁**(충돌 해소 편집 0건). 선작성 스펙·계획 없어 **아카이브 이동 0건**, 유닛 942 → **949건**(+7, 파일 100 유지)·계측 14 → **17건**(+3, `YGToastHostTest` 신설). **배포 계보가 develop 을 앞지른 회차다.** **#405**: `YGToastPolicy.show(type, replaceTag)` + `YGToastItem.tag` — 같은 태그면 걷어내고 새것만 남긴다(태그 없으면 종전대로 스택). Spotlight 작성자 토스트가 Dim 을 거쳐 다시 탭할 때 포개지던 것을 고쳤고, 태그 문자열이 화면 소유라 서로 지울 수 있다 → **OQ-P-329 신설**. **#406**: `YGFloatingBarTitle` 신설로 변형 **5종**, C-102 갤러리 두 화면이 수제 `Row` + `YGCircleButton` 을 버려 닫기 버튼에 접근성 레이블이 따라왔다. 문서가 두 회차째 "develop 미머지"로 세던 항목이 닫혔다. 빈 상태에만 제목이 없는 근거가 작업자 지시뿐 → **OQ-P-331 신설**. **#407**: G-001·C-001 의 **첫 조회**에 `YGScaffoldV2(isLoading)` — 오버레이 기준이 "누른 작업"에서 "기다리는 작업"으로 넓어졌고, 조건은 "조회 중"이 아니라 **"아직 한 번도 못 받은 조회"**다(재진입마다 조회가 나가 덮개가 번쩍이므로). 켜고 내리기를 둘 다 `launch` 블록 안에서 한다(키 가드에 막히면 `finally` 가 안 돈다). 판정이 두 ViewModel 에 복제 → **OQ-P-330 신설**(OQ-P-205 ②). **#409**: 한 PR 이 `3 → 4 → 5`(`0.0.3 → 0.1.0 → 0.1.1`)를 연달아 올려 develop 이 처음으로 배포본과 같은 값을 든다(OQ-P-310). ⚠️ **OQ-P-311 의 갈림이 뒤집혔다** — release 계보가 둘 늘어(`0.1.0-4`·`0.1.1-5`) 최신 기준 **release 만 50커밋 · develop 만 8커밋**이고, release 는 develop 에 없는 `debug-mode`·`cache-image`·`canvas-polling`(PR2 스택 포함)을 더 받았다. 그래서 **선작성 문서 넷이 구현이 끝난 채 `draft` 로 남는다**(OQ-P-311 ③ 재개). PR2 계획의 `loadTodayCanvas()` 교체 블록이 첫 조회 덮개를 모르는 것도 함께 등록 → **OQ-P-326 ⑥** |
| 2026-08-31 | `afde8c4c` | Merge #404(캔버스 폴링 스택 3단) · #408(최근 목록 종류별 정원) | delta 2건, 51파일 3045/871, **두 머지 다 트리 = 브랜치 팁**(충돌 해소 편집 0건). 유닛 949 → **996건**(+47, 파일 100 → 101)·계측 **17건** 유지. **아카이브 이동이 넉 달 만에 다시 생긴 회차** — 선작성 스펙 1·계획 3이 한꺼번에 갔다(spec `implemented`·plan `done`, 링크 `../`→`../../` 보정, 두 README 표 이동, Task 체크박스 완료 표시. 「수동 확인」 절은 실기기 기록이 없어 미체크 유지). **#404**: PR1 배경 탭 토핑 렌더링 · PR2 오늘 캔버스 인메모리 SSoT · PR3 주기 폴링이 한 머지로. `ParfaitRepository.getTodayCanvas` 하나가 **구독·갱신 둘·정리·실패 축** 다섯이 되고 `GetTodayParfaitUseCase` 소멸. 신설 `CanvasLocalDataSource`(인메모리 두 번째)·`CanvasPoller`(참조 계수 + 5초 루프)·`BaseViewModel.launchWhileSubscribed`·DI 모듈 둘(`ApplicationScopeModule`·`ClockModule`). 서버 계약은 불변이고 **부르는 주체가 화면에서 저장소 층으로 내려간 것**이 실질 — `today` 억제가 화면 `launch(key)` 하나에서 폴러 가드로 모였다. ADR-0029 `proposed` → **`accepted`**. **#408**: `MAX_SIZE` → `MAX_SIZE_PER_KIND`(원본·알맹이 각자 정원) + 신설 `RecentImagePick` 이 `NavKeyCustomGalleryPicker` 의 **기본값 없는 첫 동작 인자**. 문서 조치: data-layer(인메모리 (2) 절 신설·DI 모듈 둘·`ParfaitRepository` 행 재작성·최근 이미지 절)·state-management(`launchWhileSubscribed` 머지 확정·`viewedCanvas`→`pastCanvas` 정정)·navigation-flow(최근 줄 가르는 축 변경·다이어그램 인자·기본값 없는 동작 인자 사례)·api/parfait.md Android 매핑(호출 주체 이동, `verified` 불변 — 서버 대조일이라 이 스킬이 안 건드린다). open-questions: **OQ-P-258 해소됨**(②로), OQ-P-321 부분 해소(①② 닫힘·③ 잔존), OQ-P-320·322·323 "구현 전" → "구현됨"(값·서버 확인은 잔존), OQ-P-326 ①②⑤⑥ develop 확정, **OQ-P-311 ③ 넷 중 셋 닫힘**(`debug-mode` 만 잔존, release 만 50 · develop 만 43), **OQ-P-332 신설**(정원 두 배의 저장소 사용량 미측정). 미머지 추적 항목: **`feature/debug-mode` 1건** |
