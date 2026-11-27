# C-001 빈 캔버스 안내 애니메이션 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 토핑 0개인 오늘 캔버스에 진입하면 더미 이미지 3장과 글자별 강조 텍스트를 그룹·날짜당 1회 재생한다.

**Architecture:** 재생 기록은 `PastCanvasAlert`와 같은 모양(계정 별칭 키 + 마지막 재생 날짜, DataStore)으로 data·domain에 새로 둔다. 연출 단계(`CanvasEmptyIntroPhase`)는 `CanvasMainViewModel`의 UiState가 들고, 애니메이션 값은 컴포지션 상태 홀더 `CanvasEmptyIntroState`가 단계를 거쳐 읽는다(G-001 `GroupListEmptyIntroState` 선례). `YGCanvas`의 빈 안내판은 `emptyMessage: String`에서 슬롯으로 바꾼다.

**Tech Stack:** Kotlin, Jetpack Compose(`Animatable`, `graphicsLayer`), DataStore Preferences, Hilt, MockK·kotlin.test(JVM 단위 테스트)

**Spec:** [2026-11-23-c001-empty-intro-animation-design.md](../specs/2026-11-23-c001-empty-intro-animation-design.md)

## Global Constraints

- 사용자 응답·문서·주석은 한국어, 코드 식별자·커밋 메시지는 영어. 커밋 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- 주석은 "왜"만 쓴다. 이름·시그니처가 말하는 KDoc은 쓰지 않는다([code-conventions](../../code-conventions.md)).
- Composable 안에서 early return(guard clause)을 쓰지 않는다. `if { }`로 감싼다.
- Preview는 `private`, 2개 이상이면 `PreviewParameter`로 만든다.
- 시스템 글자 크기는 무시한다(ADR-0036). 새 타이포 토큰은 만들지 않고 `YGTheme.typography.caption.c01M`(= SUIT Medium 12sp)을 쓴다.
- 더미 이미지: 시작 Y `-60dp` → 0, 불투명도 0 → 1, 1,000ms, `CubicBezierEasing(0f, 0f, 0f, 1f)`, 시작 500/1,000/1,500ms.
- 강조: 대상은 "함께 남기고 싶은 순간"의 공백 제외 9글자, `Gray500 → Black` 150ms `(0, 0, 0.58, 1)` 후 `Black → Gray500` 300ms `(0.42, 0, 0.58, 1)`, 글자 시작 500ms + 150ms × 번호, 마지막 글자 2,150ms.
- 닫기: 300ms, `CubicBezierEasing(0f, 0f, 0.58f, 1f)`.
- 이미지 크기는 PNG 픽셀 수를 dp로(134×134, 180×193, 248×254), 중심 배치 비율은 (31.64%, 36.58%), (78.34%, 60.15%), (31.49%, 69.04%).
- 문구: `아직 캔버스가 비어 있어요\n친구들과 함께 남기고 싶은 순간을 캔버스에 쌓아 주세요`.
- 이 브랜치는 `feature/726-empty-canvas-intro-animation`, 작업 디렉터리는 `TEAMYG-Android-wt-726`.

## Review Focus

- 기록 읽기에서 별칭 생성이 실패한다 → 읽기는 `null`, 쓰기는 무시하고 화면은 그대로 뜬다(Task 1).
- 03:00 이후 캔버스 날짜가 바뀐다 → 같은 그룹도 다시 `Entering`(Task 3).
- 재생 도중 다른 멤버가 토핑을 올려 캔버스가 비지 않게 된다 → 연출이 `Dismissed`로 끝난다(Task 3).
- 재생 도중 지난 날로 이동했다 오늘로 돌아온다 → 같은 날 재재생하지 않는다(기록은 시작 때 썼다)(Task 3).
- `Dismissing` 중 앱이 백그라운드로 갔다 오면 `Dismissed`다(Task 2).

---

### Task 1: 재생 기록 저장 (data·domain)

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/parfait/CanvasIntroRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/GetCanvasIntroPlayedDateUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/MarkCanvasIntroPlayedUseCase.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasIntroLocalDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasIntroLocalDataSourceImpl.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/parfait/CanvasIntroRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`, `.../RepositoryModule.kt` (`@Binds @Singleton` 추가)
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasIntroLocalDataSourceImplTest.kt`

**Interfaces:**
- Produces:
  - `interface CanvasIntroLocalDataSource { suspend fun playedDate(memberId: MemberId, groupId: GroupId): LocalDate?; suspend fun markPlayed(memberId: MemberId, groupId: GroupId, date: LocalDate) }`
  - `interface CanvasIntroRepository`: 같은 두 메서드(`playedDate`, `markPlayed`)
  - `GetCanvasIntroPlayedDateUseCase.invoke(groupId: GroupId): LocalDate?` — 계정을 모르면 `null`
  - `MarkCanvasIntroPlayedUseCase.invoke(groupId: GroupId, date: LocalDate)` — 계정을 모르면 기록하지 않는다

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `PastCanvasAlertLocalDataSourceImplTest`와 같은 방식(`FakePreferencesDataStore`, `AccountKeyAlias` mockk)으로 아래 이름의 테스트를 쓴다. 별칭 스코프 문자열은 `"canvas-intro:<groupId>"`, 저장 키 이름은 `"canvas_intro_played_<alias>"`.
  - `markPlayed_thenRead_roundTrips`
  - `playedDate_nothingSaved_isNull`
  - `markPlayed_calledAgain_replacesThePreviousDate` (같은 키가 덮인다 — 날마다 키가 늘지 않는다)
  - `markPlayed_differentGroups_doNotOverwriteEachOther`
  - `playedDate_otherAccountSameGroup_isIsolated`
  - `markPlayed_storesUnderAliasKey_withoutRawIds` (키 이름에 계정·그룹 id가 없다)
  - `playedDate_aliasFails_isNull`, `markPlayed_aliasFails_doesNotThrow`
  - `playedDate_unparsableValue_isNull`
- [ ] **Step 2: 실패를 확인한다.** Run: `./gradlew :data:testDebugUnitTest --tests '*CanvasIntroLocalDataSourceImplTest'` — Expected: 컴파일 실패(클래스 없음).
- [ ] **Step 3: 데이터 소스를 구현한다.** `CanvasIntroLocalDataSourceImpl @Inject constructor(dataStore: DataStore<Preferences>, accountKeyAlias: AccountKeyAlias)`, `@Singleton`. `PastCanvasAlertLocalDataSourceImpl`의 `keyOrNull`(`runCatching`으로 별칭 실패 → `null`)을 따르되 **레거시 키 이관은 없다**. 읽기는 `dataStore.data.first()[key]`를 `LocalDate.parse`(`runCatching`)로, 쓰기는 `dataStore.edit { it[key] = date.toString() }`로 한다. 읽기 경로에서 `edit`을 타지 않는 이유(쓰기 큐 직렬화)를 한 줄 주석으로 남긴다.
- [ ] **Step 4: 리포지토리·유스케이스·DI를 만든다.** 리포지토리 구현은 데이터 소스에 위임만 한다. 유스케이스는 `GetLastSeenClosedDateUseCase`·`MarkPastCanvasAlertSeenUseCase`와 같은 모양(`MemberRepository.myAccount.first()?.memberId`)이다. 두 모듈에 `bindCanvasIntroLocalDataSource`, `bindCanvasIntroRepository`를 추가한다.
- [ ] **Step 5: 통과를 확인한다.** Run: 같은 명령 — Expected: PASS. 이어서 `./gradlew :domain:compileKotlin :data:compileDebugKotlin` 성공.
- [ ] **Step 6: Commit** — `feat: add canvas intro played-date storage`

---

### Task 2: 연출 단계와 타임라인 상수

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/CanvasEmptyIntroPhase.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/CanvasEmptyIntroTimeline.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/CanvasEmptyIntroPhaseTest.kt`, `.../CanvasEmptyIntroTimelineTest.kt`

**Interfaces:**
- Produces:
  - `enum class CanvasEmptyIntroPhase { Idle, Entering, Shown, Dismissing, Dismissed }` 와 `fun onAnimationFinished()`, `fun onTap()`, `fun onInteraction()`, `fun onStop()`, `fun onDismissFinished()` — 각각 `CanvasEmptyIntroPhase`를 돌려준다. 값 getter: `fun enterValue(raw: Float): Float`(`Entering`이면 `raw`, `Shown`·`Dismissing`이면 1, 그 외 0), `fun exitValue(raw: Float): Float`(`Dismissing`이면 `raw`, `Dismissed`면 0, 그 외 1), `val isPlaying: Boolean`(`Entering`·`Shown`·`Dismissing`).
  - `object CanvasEmptyIntroTimeline`: `IMAGE_COUNT = 3`, `imageDelayMillis(index: Int): Int`, `IMAGE_DURATION_MILLIS = 1000`, `IMAGE_OFFSET_DP = 60`, `HIGHLIGHT_TEXT = "함께 남기고 싶은 순간"`, `highlightDelayMillis(charIndex: Int): Int`, `HIGHLIGHT_UP_MILLIS = 150`, `HIGHLIGHT_DOWN_MILLIS = 300`, `EXIT_DURATION_MILLIS = 300`, `TOTAL_MILLIS`(= 2,500), `ImageEnterEasing`, `HighlightUpEasing`, `HighlightDownEasing`, `ExitEasing`(`CubicBezierEasing`).

- [ ] **Step 1: 실패하는 테스트를 쓴다.**
  - `CanvasEmptyIntroPhaseTest`: 스펙 전이표 전부를 한 케이스씩. 예) `Entering.onInteraction() == Shown`, `Entering.onTap() == Dismissing`, `Entering.onStop() == Shown`, `Shown.onInteraction() == Dismissing`, `Shown.onStop() == Shown`, `Dismissing.onStop() == Dismissed`, `Dismissing.onDismissFinished() == Dismissed`, `Idle`·`Dismissed`는 모든 사건에서 그대로, `Entering.onAnimationFinished() == Shown`. `enterValue`·`exitValue`가 단계별로 위 규칙대로인지.
  - `CanvasEmptyIntroTimelineTest`: `imageDelayMillis`가 500/1000/1500, 마지막 이미지 끝(`imageDelayMillis(2) + IMAGE_DURATION_MILLIS`)이 `TOTAL_MILLIS`(2,500), `HIGHLIGHT_TEXT`에서 공백을 뺀 글자 수가 9, 마지막 글자 끝(`highlightDelayMillis(8) + HIGHLIGHT_UP_MILLIS + HIGHLIGHT_DOWN_MILLIS`)이 2,150.
- [ ] **Step 2: 실패를 확인한다.** Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasEmptyIntro*'` — Expected: 컴파일 실패.
- [ ] **Step 3: 위 시그니처대로 구현한다.** `highlightDelayMillis(i) = 500 + 150 * i`. `HIGHLIGHT_TEXT`의 글자 번호 `i`는 공백을 뺀 순서다.
- [ ] **Step 4: 통과를 확인한다.** Run: 같은 명령 — Expected: PASS.
- [ ] **Step 5: Commit** — `feat: add canvas empty intro phase and timeline`

---

### Task 3: ViewModel 통합

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `GetCanvasIntroPlayedDateUseCase`, `MarkCanvasIntroPlayedUseCase`; Task 2의 `CanvasEmptyIntroPhase`.
- Produces:
  - `CanvasMainUiState.emptyIntroPhase: CanvasEmptyIntroPhase = Idle`
  - `CanvasMainIntent`: `data object OnEmptyIntroAnimationFinished`, `data object OnEmptyIntroTap`, `data object OnEmptyIntroInteraction`, `data object OnEmptyIntroDismissFinished`
  - 생성자 파라미터 `getCanvasIntroPlayedDate: GetCanvasIntroPlayedDateUseCase`, `markCanvasIntroPlayed: MarkCanvasIntroPlayedUseCase`
  - 기존 `OnAppReturnedFromBackground`는 스포트라이트 해제에 더해 `emptyIntroPhase.onStop()`을 적용한다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** 기존 테스트 하네스(`enteredViewModel()`, `todayCanvases`, mockk 유스케이스)에 `getCanvasIntroPlayedDate`(기본 `null`)와 `markCanvasIntroPlayed`(`relaxUnitFun`)를 더하고 생성 호출에 넘긴다. 이름과 단언:
  - `emptyTodayCanvas_noPlayedRecord_entersIntroAndMarksPlayedWithCanvasDate` → `phase == Entering`, `coVerify(exactly = 1) { markCanvasIntroPlayed(GroupId(GROUP_ID), today) }`
  - `emptyTodayCanvas_playedRecordIsToday_goesStraightToDismissedWithoutMarking` → `Dismissed`, mark 0회
  - `emptyTodayCanvas_playedRecordIsYesterday_entersIntroAgain` (03:00 이후 새 캔버스)
  - `todayCanvasWithToppings_doesNotStartIntro` → `Idle`, mark 0회
  - `emptyCanvasWhileViewingPastDay_doesNotStartIntro` → 지난 날을 보는 동안 `Idle`
  - `toppingArrivesDuringIntro_endsIntroAsDismissed` → `Entering`/`Shown`에서 캔버스가 비지 않게 되면 `Dismissed`
  - 전이 인텐트: `OnEmptyIntroTap`·`OnEmptyIntroInteraction`·`OnEmptyIntroAnimationFinished`·`OnEmptyIntroDismissFinished`가 `Entering`/`Shown`/`Dismissing`에서 Task 2 전이표대로 상태를 바꾸는지, `OnAppReturnedFromBackground`가 `Entering → Shown`, `Dismissing → Dismissed`로 바꾸는지
  - `returningFromAnotherScreen_keepsShownPhase` (같은 ViewModel에서 `Enter` 재호출 후에도 `Shown` 유지)
  - `mark` 호출은 단계가 `Entering`이 된 경우에만이고, 같은 날 폴링 재방출로 두 번 호출되지 않는다(`exactly = 1`)
- [ ] **Step 2: 실패를 확인한다.** Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasMainViewModelTest'` — Expected: 컴파일 실패.
- [ ] **Step 3: 구현한다.** `observeTodayCanvas` 수집기 끝에 `canvas?.let(::syncEmptyIntro)`를 더한다. `syncEmptyIntro(canvas: CanvasVO)`: 단계가 `Entering`·`Shown`이고 `canvas`가 비지 않았으면 `Dismissed`로 만든다. 단계가 `Idle`이고 오늘을 보는 중이며 캔버스가 비었으면 `launch(key = EMPTY_INTRO_KEY)`에서 `getCanvasIntroPlayedDate(groupId) == canvas.date`면 `Dismissed`, 아니면 단계를 `Entering`으로 바꾼 뒤 `markCanvasIntroPlayed(groupId, canvas.date)`를 부른다(재생을 **시작하는 시점**에 기록한다는 규칙). 단계 변경은 `updateState`로 `Idle`일 때만 적용해 중복 시작을 막는다. 네 인텐트는 `emptyIntroPhase`에 Task 2 전이 함수를 적용하고, `OnAppReturnedFromBackground`는 `resetSpotlight()`에 `onStop()` 적용을 더한다. "비었다"는 기존 `CanvasMainUiState.isCanvasEmpty`와 같은 `toppings.isEmpty() && texts.isEmpty()`이므로 `CanvasVO`용 확장 하나를 두어 공유한다.
- [ ] **Step 4: 통과를 확인한다.** Run: 같은 명령 — Expected: 전체 PASS(기존 테스트 포함).
- [ ] **Step 5: Commit** — `feat: drive canvas empty intro phase from CanvasMainViewModel`

---

### Task 4: 화면 (YGCanvas 슬롯, 연출 상태, 레이어, 배선)

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcanvas/YGCanvas.kt` — `emptyMessage: String`을 `emptyContent: @Composable BoxScope.() -> Unit = {}`로 교체. 회색 면(`Gray100`)은 `isEmpty && background == null`일 때만 깔고, `emptyContent`는 `isEmpty`면 항상 그린다(캡처 Box 바깥, 날짜 버튼 아래). 파일에 `YGCanvasEmptyMessage(text: AnnotatedString, modifier)`와 `String` 오버로드를 공개 컴포저블로 추가한다(기존 안내판의 `c01M`·`Gray500`·가운데 정렬·가로 패딩).
- Modify: `core/designsystem/src/androidTest/.../YGCanvasTest.kt`, `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGCanvasPreviewScreen.kt`, `YGCanvas.kt`의 프리뷰 — `emptyContent = { YGCanvasEmptyMessage(...) }`로 호출부를 맞춘다.
- Create: `feature/groups/canvas/impl/src/main/res/drawable-nodpi/canvas_empty_intro_1.png`, `_2.png`, `_3.png` (각각 `~/Desktop/Group 1.png`, `Group 2.png`, `Group 3.png`를 복사)
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml` — `canvas_main_empty_message`를 "아직 캔버스가 비어 있어요\n친구들과 함께 남기고 싶은 순간을 캔버스에 쌓아 주세요"로 교체
- Create: `.../canvas/impl/model/CanvasEmptyIntroImage.kt` (`data class CanvasEmptyIntroImage(@DrawableRes val res: Int, val width: Dp, val height: Dp, val centerXFraction: Float, val centerYFraction: Float, val rotationDegrees: Float = 0f)`와 `CANVAS_EMPTY_INTRO_IMAGES: List<CanvasEmptyIntroImage>` 상수, Global Constraints의 크기·비율 값)
- Create: `.../canvas/impl/model/CanvasEmptyIntroState.kt` — `@Stable internal class CanvasEmptyIntroState`와 `@Composable internal fun rememberCanvasEmptyIntroState(phase: CanvasEmptyIntroPhase, onAnimationFinished: () -> Unit, onDismissFinished: () -> Unit): CanvasEmptyIntroState`
- Create: `.../canvas/impl/component/CanvasEmptyIntroLayer.kt` — `@Composable internal fun CanvasEmptyIntroLayer(state: CanvasEmptyIntroState, message: String, modifier: Modifier = Modifier)`
- Modify: `.../canvas/impl/screen/CanvasMainScreen.kt`, `.../canvas/impl/route/CanvasMainRoute.kt` (배선)

**Interfaces:**
- Consumes: Task 2의 `CanvasEmptyIntroPhase`·`CanvasEmptyIntroTimeline`, Task 3의 `emptyIntroPhase`와 네 인텐트.
- Produces:
  - `CanvasEmptyIntroState`: `fun imageProgress(index: Int): Float`(0..1, 단계 반영), `fun highlight(charIndex: Int): Float`(0..1, 단계 반영), `val exitAlpha: Float`, `suspend fun play()`, `suspend fun dismiss()`. 값 getter는 `phase.enterValue`·`exitValue`를 거쳐서 `Shown`이면 이미지 1·강조 0, `Dismissed`면 이미지 0·강조 0이 되게 한다.
  - `CanvasMainScreen`에 `onEmptyIntroTap: () -> Unit`, `onEmptyIntroInteraction: () -> Unit`, `onEmptyIntroAnimationFinished: () -> Unit`, `onEmptyIntroDismissFinished: () -> Unit` 파라미터 추가.

- [ ] **Step 1: YGCanvas 슬롯을 바꾸고 컴파일한다.** 위 Files대로 `emptyMessage`를 `emptyContent`로 교체하고 호출부 3곳(androidTest, app-preview, 프리뷰)을 맞춘다. 배경이 있는 빈 캔버스에는 회색 면을 깔지 않는 이유(고른 배경을 가리면 안 됨)를 한 줄 주석으로 남긴다. Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:compileDebugAndroidTestKotlin :app-preview:compileDebugKotlin` — Expected: 성공.
- [ ] **Step 2: 에셋·문구·이미지 목록을 추가한다.** PNG 복사, `strings.xml` 교체, `CanvasEmptyIntroImage.kt` 작성.
- [ ] **Step 3: `CanvasEmptyIntroState`를 구현한다.** 이미지 3개의 `Animatable(0f)`, 강조 9글자의 `Animatable(0f)`, 닫기 `Animatable(1f)`을 들고, `play()`는 `coroutineScope`에서 각 이미지를 `delay(imageDelayMillis(i))` 후 `tween(1000, ImageEnterEasing)`으로 1까지, 각 글자를 `delay(highlightDelayMillis(i))` 후 `tween(150, HighlightUpEasing)`으로 1, 이어서 `tween(300, HighlightDownEasing)`으로 0까지 올린다. `dismiss()`는 닫기 값을 `tween(300, ExitEasing)`으로 0까지 내린다. 값 getter는 단계 getter를 거치게 하고(`enterValue`로 이미지·강조를, `exitValue`로 닫기), 강조는 `Entering`에서만 raw 값, 그 밖에는 0이다. `rememberCanvasEmptyIntroState`는 `LaunchedEffect(phase == Entering)`에서 `play()` 후 `onAnimationFinished()`, `LaunchedEffect(phase == Dismissing)`에서 `dismiss()` 후 `onDismissFinished()`를 부른다.
- [ ] **Step 4: `CanvasEmptyIntroLayer`를 구현한다.** `BoxWithConstraints` 안에서 `CANVAS_EMPTY_INTRO_IMAGES`를 그린다. 위치는 `Modifier.offset { IntOffset(x, y) }`(중심 = 비율 × 캔버스 크기, 이미지 크기의 절반을 뺀 좌상단)로 정하고, 그 **바깥** `graphicsLayer { translationY = -IMAGE_OFFSET_DP.dp.toPx() * (1 - progress); alpha = progress * exitAlpha }`, **안쪽** `graphicsLayer { rotationZ = rotationDegrees }`로 둬서 회전이 있어도 이동이 화면 기준 수직이게 한다(이유를 한 줄 주석으로). 이미지는 `contentDescription = null`이고 `clearAndSetSemantics {}`로 접근성에서 뺀다. 이미지를 먼저, 그 위에 가운데 정렬된 `YGCanvasEmptyMessage`를 그린다. 메시지는 `message`에서 `HIGHLIGHT_TEXT`를 찾아 공백 아닌 글자마다 `SpanStyle(color = lerp(Gray500, Black, state.highlight(i)))`를 입힌 `AnnotatedString`이다. 이미지 시작 오프셋·닫기 알파는 `state` 값만 쓴다.
- [ ] **Step 5: 화면을 배선한다.** `CanvasMainScreen`의 `YGCanvas(...)`에서 `isEmpty = canvasState.isCanvasEmpty`는 유지하고 `emptyContent`에서 `canvasState.isEmptyIntroVisible`(= `isViewingToday && emptyIntroPhase.isPlaying`)이면 `CanvasEmptyIntroLayer`를, 아니면 `YGCanvasEmptyMessage(message)`를 그린다. `CanvasMainUiState`에 `isEmptyIntroVisible`을 추가한다. 상태 홀더는 `CanvasMainScreen` 최상단에서 `rememberCanvasEmptyIntroState(canvasState.emptyIntroPhase, ...)`로 만들어 레이어에 넘긴다. 루트 `Column`에 `isEmptyIntroVisible`일 때만 `Modifier.pointerInput(Unit) { awaitEachGesture { val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final); if (!down.isConsumed) onEmptyIntroTap() } }`를 건다(G-001과 같은 방식: 버튼이 소비한 다운은 닫기로 세지 않는다, 터치는 소비하지 않는다). 뒤로가기(`onClickBack`)·메뉴(`onClickMenu`)·`addAction.onClick`(`openMenu`)은 각각 먼저 `onEmptyIntroInteraction()`을 부른 뒤 기존 동작을 한다. `CanvasMainRoute`에서 네 콜백을 `viewModel.processIntent(...)`로 잇는다. 이유가 필요한 곳(소비 안 된 다운만 보는 이유)에만 주석을 단다.
- [ ] **Step 6: Preview를 추가한다.** `CanvasEmptyIntroLayer`를 `Shown`·`Entering 중간`·`Dismissed` 세 단계로 보는 프리뷰 하나를 `PreviewParameter`(단계 목록)로 만든다. 모두 `private`.
- [ ] **Step 7: 빌드·린트를 확인한다.** Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :app:assembleDebug` — Expected: 성공. 린트/포맷 태스크가 이 저장소에 있으면(`./gradlew tasks --all | grep -i -E "ktlint|spotless|detekt"`) 함께 돌린다.
- [ ] **Step 8: Commit** — `feat: play empty canvas intro animation on C-001`

---

### Task 5: 문서와 에뮬레이터 검증

**Files:**
- Modify: `docs/status.md` (캔버스 메인 C-001 절: 안내판 조건 `isEmpty`로 변경, 재생 규칙·앵커 `CanvasEmptyIntroPhase`·`CanvasEmptyIntroState`·`CanvasEmptyIntroLayer`·`CanvasIntroLocalDataSourceImpl`, OQ-P-304 해소 반영, 설계 링크에 새 스펙 추가)
- Modify: `docs/log.md` (한 줄 append)
- Modify: `docs/synthesis/open-questions.md` (OQ-P-304 삭제, 새 열린 질문 추가: 에뮬레이터·실기기 타이밍/TalkBack 미확인, 배경 위 텍스트 가독성)

- [ ] **Step 1: 문서를 고친다.** 위 세 파일을 현재 상태 서술로 덮어쓴다(변경 서술·이력은 쓰지 않는다). Run: `python3 docs/script/check_links.py docs` — Expected: 깨진 링크 0건.
- [ ] **Step 2: 앱을 빌드해 에뮬레이터에 설치한다.** `Pixel_9` AVD를 띄우고 `./gradlew :app:installDebug`. 브랜치 전환 빌드 캐시 함정은 메모리의 에뮬레이터 레시피를 따른다.
- [ ] **Step 3: 시나리오를 확인하며 화면을 녹화한다.** `adb shell screenrecord`로 (a) 빈 캔버스 첫 진입 재생, (b) 재생 도중 탭으로 디졸브, (c) 재진입 시 텍스트만, (d) 재생 도중 뒤로가기·메뉴, (e) 백그라운드 복귀를 순서대로 찍고 `ffmpeg` 타일 시트로 타이밍을 확인한다. 서버 값을 바꾸는 조작은 하지 않는다.
- [ ] **Step 4: Commit** — `docs: reflect C-001 empty intro animation in status and log`
