---
id: g001-empty-animation
title: 그룹 목록 Empty 안내 애니메이션 (G-001-Empty)
status: in-progress
type: work-order
created: 2026-10-03
updated: 2026-10-03
platforms: android
owner:
related_adr:
related_spec: g001-empty-animation
related_code:
  - feature/groups/list/impl/.../route/GroupListScreen.kt#GroupListScreen
  - feature/groups/list/impl/.../route/GroupListViewModel.kt#GroupListUiState
  - feature/groups/list/impl/.../route/component/GroupListTopBar.kt#GroupListTopBar
  - feature/groups/list/impl/.../route/component/GroupListTooltip.kt#GroupListTooltip
  - core/designsystem/.../component/ygtoppinggroup/YGToppingImage.kt#YGToppingImage
  - core/designsystem/.../component/ygtopbar/YGTopBar.kt#YGTopBarEmpty
archived_reason:
tags: [plan, parfait, G-001, animation]
---

# G-001-Empty 안내 애니메이션 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 그룹 0건으로 G-001에 설 때마다 더미 그룹 3개와 안내 툴팁이 순서대로 등장하고, 3초 뒤부터 화면을 누르면 함께 사라지게 한다. 상단 바는 날짜·요일 대신 "내 그룹 N"을 보여 준다.

**Architecture:** 인트로 상태는 컴포지션이 든다(`remember`). 단계(`Entering → Shown → Dismissing → Dismissed`)와 `Animatable` 값을 한 상태 객체가 갖고, 단계 전이·값 해석은 순수 함수로 떼어 단위 테스트한다. 더미는 기존 `ToppingLayout` 안에 `YGToppingGroup`으로 그리고, 툴팁은 Material 팝업 대신 상단 바 아래에 겹치는 일반 컴포저블이다. ViewModel은 날짜·툴팁 상태를 잃고 "0건 확정" 파생값 하나만 얻는다.

**Tech Stack:** Kotlin, Jetpack Compose(`Animatable`, `graphicsLayer`, `pointerInput`), `androidx.lifecycle.compose.LifecycleStartEffect`, `BaseViewModel`(MVI), kotlin-test + MockK + Turbine, Figma MCP.

**Spec:** [`docs/superpowers/specs/2026-10-03-g001-empty-animation-design.md`](../specs/2026-10-03-g001-empty-animation-design.md) — 타임라인 표·단계 정의·문구·알려진 한계는 스펙이 정본이다.

## Global Constraints

- 코드 주석·KDoc 규약: [`docs/code-conventions.md`](../../code-conventions.md)
- 커밋 메시지는 `type: 한국어 설명` 꼴. `Co-Authored-By` 줄을 붙이지 않는다
- data class는 `impl/model`에 둔다(`impl/viewmodel`·`route/component`에 두지 않는다)
- 경로 약어: `P` = `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl`, `T` = `feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl`, `DS` = `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem`
- 타임라인(밀리초): 초기 대기 `500`, 더미 간격 `500`, 더미 지속 `1000`, 툴팁 시작 `2500`, 툴팁 지속 `500`, 등장 전체 `3000`, 종료 지속 `300`
- 더미 이동량: Y `-60.dp → 0`
- 이징: 더미 등장 `CubicBezierEasing(0f, 0f, 0f, 1f)`, 종료 `CubicBezierEasing(0f, 0f, 0.58f, 1f)`, 툴팁 등장은 Task 3의 샘플 곡선
- 문구(정확히):
  - 상단 바 제목 `내 그룹`
  - 더미 그룹명 `예카수집가`, `파르페`, `일상`. 시간은 기존 `group_list_timestamp_minutes`에 `1`, `2`, `3`
  - 툴팁 1행 `새 그룹을 만들거나 그룹에 참여하면`, 2행 `내 그룹 목록을 파르페로 쌓을 수 있어요.` 강조 구절은 `새 그룹`, `그룹에 참여`, `파르페`
- 툴팁 강조·테두리 색: `YGAtomicColors.Soda.Soda500`
- 터치는 소비하지 않는다. 상단 바 버튼은 어느 단계에서든 동작한다
- 빈 파르페 그래픽(크림·컵 이미지)은 바꾸지 않는다

## Review Focus

- **그룹이 있는 사용자의 당겨서 새로고침** — `GroupListScreen`은 새로고침 동안 그릴 목록을 빈 목록으로 바꾼다. 이때 더미·툴팁이 뜨면 안 된다. → Task 1 `isEmptyConfirmed` 테스트, Task 4가 화면 지역값이 아니라 이 값만 본다
- **등장 중 탭 뒤 방치** — 3초 이전 탭이 "예약"되어 3초에 저절로 닫히면 안 된다. → Task 3 `onTouchDown_whileEntering_staysEntering`
- **종료 중·종료 후의 추가 탭** — 다시 등장하거나 종료 애니메이션이 처음부터 돌면 안 된다. → Task 3 `onTouchDown_whileDismissingOrDismissed_keepsPhase`
- **등장 도중 백그라운드 복귀** — 반쯤 나온 더미가 그대로 멈춰 있거나 툴팁이 빠진 채 보이면 안 된다. 전부 드러난 상태여야 한다. → Task 3 `onStop_*`, `enterValue_*`
- **화면에 선 채 0건 ↔ 1건 이상이 바뀜** — 첫 그룹이 생기면 더미는 즉시 사라지고, 다시 0건이 되면 처음부터 재생된다. 단위 테스트로 못 잡는다. → Task 5 에뮬레이터 확인 항목

---

### Task 1: 상단 바 "내 그룹 N"과 ViewModel 상태 정리

**Files:**
- Modify: `DS/component/ygtopbar/YGTopBar.kt` (`YGTopBarEmpty`와 파일 안 프리뷰)
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTopBarPreviewScreen.kt`
- Modify: `P/route/component/GroupListTopBar.kt`
- Modify: `P/route/GroupListViewModel.kt`
- Modify: `P/route/GroupListScreen.kt`, `P/route/GroupListErrorScreen.kt` (호출부·프리뷰 인자)
- Modify: `feature/groups/list/impl/src/main/res/values/strings.xml`
- Test: `T/route/GroupListViewModelTest.kt`

**Interfaces:**
- Produces:
  - `fun YGTopBarEmpty(title: String, count: String?, onIconClick: () -> Unit, modifier: Modifier = Modifier, hazeState: HazeState? = null, windowInsets: WindowInsets = YGTopBarDefaults.windowInsets, rightContent: @Composable () -> Unit = {})`
  - `internal fun GroupListTopBar(count: Int?, onClickSideMenu: () -> Unit, modifier: Modifier = Modifier, onClickAddGroup: (() -> Unit)? = null, isTooltipVisible: Boolean = false)` — `isTooltipVisible`와 `TooltipBox`는 Task 4에서 걷는다. 이 태스크에서는 그대로 둔다
  - `GroupListUiState`의 멤버 프로퍼티 `val isEmptyConfirmed: Boolean`
  - `R.string.group_list_title`

- [x] **Step 1: 실패하는 테스트를 쓴다** — `GroupListViewModelTest.kt`에 추가

```kotlin
@Test
fun isEmptyConfirmed_onlyWhenTheListArrivedEmpty() {
    assertFalse(GroupListUiState(groupList = null).isEmptyConfirmed)
    assertTrue(GroupListUiState(groupList = emptyList()).isEmptyConfirmed)
    assertFalse(GroupListUiState(groupList = GROUPS).isEmptyConfirmed)
}

@Test
fun isEmptyConfirmed_whileRefreshingWithNoGroups_staysTrue() {
    // 0건에서 당겨도 켜진 채여야 인트로 상태가 버려지지 않아 다시 재생되지 않는다
    assertTrue(GroupListUiState(groupList = emptyList(), isRefreshing = true).isEmptyConfirmed)
}
```

같은 파일에서 `enter_fillsTodayInTheHeader`를 지운다(날짜 상태가 없어진다).

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*GroupListViewModelTest'`
Expected: 컴파일 실패 — `isEmptyConfirmed` 미정의

- [x] **Step 3: `GroupListUiState`를 고친다** (`GroupListViewModel.kt`)

- `dateString`, `dayOfWeekString` 필드와 `updateToday()`, 그 호출, 안 쓰게 된 import를 지운다
- 파생값을 더한다: `val isEmptyConfirmed: Boolean get() = groupList?.isEmpty() == true`
- `isTooltipVisible`은 이 태스크에서 건드리지 않는다
- `core/util/jvm`의 `DateFormat.FullMonthWithDay`·`AbbreviatedDayOfWeek`는 이 ViewModel이 유일한 사용처였다. `grep`으로 남은 사용처가 테스트뿐임을 확인한 뒤 두 상수와 `DateFormatTest`의 해당 케이스를 지운다. 다른 사용처가 나오면 남긴다

- [x] **Step 4: `YGTopBarEmpty` 시그니처를 바꾼다** (`YGTopBar.kt`)

`date`→`title`, `day`→`count: String?`. `count`는 괄호 없이 그대로 그리고(`Gray300`), `null`이면 그 `Text`를 그리지 않는다. 같은 파일 프리뷰 셋과 `YGTopBarPreviewScreen.kt` 호출 셋을 `title = "내 그룹"`, `count = "3"`(긴 제목 프리뷰는 긴 `title` 유지)로 맞춘다.

- [x] **Step 5: `GroupListTopBar`와 호출부를 맞춘다**

- `strings.xml`: `<string name="group_list_title">내 그룹</string>`
- `GroupListTopBar`: `date`, `day` 대신 `count: Int?`. `YGTopBarEmpty(title = stringResource(R.string.group_list_title), count = count?.toString(), …)`
- `GroupListScreen`, `GroupListErrorScreen`: `count = uiState.groupList?.size`. 두 파일의 프리뷰 상태에서 `dateString`·`dayOfWeekString` 인자를 지운다

- [x] **Step 6: 통과를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest :core:util:jvm:test :app-preview:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL, `GroupListViewModelTest` 전부 PASS

- [x] **Step 7: 커밋**

```bash
git add core/designsystem core/util/jvm app-preview feature/groups/list/impl
git commit -m "feat: 그룹 목록 상단 바를 날짜 대신 내 그룹 개수로 바꾼다"
```

---

### Task 2: 더미 그룹 이미지와 `YGToppingImage.Local`

**Files:**
- Modify: `DS/component/ygtoppinggroup/YGToppingImage.kt`, `DS/component/ygtoppinggroup/YGToppingGroup.kt`
- Create: `feature/groups/list/impl/src/main/res/drawable-xxxhdpi/img_group_list_dummy_matcha.png`, `img_group_list_dummy_cap.png`, `img_group_list_dummy_camera.png`
- Create: `P/model/GroupListEmptyDummyGroup.kt`
- Modify: `feature/groups/list/impl/src/main/res/values/strings.xml`
- Test: `T/model/GroupListEmptyDummyGroupTest.kt`

**Interfaces:**
- Produces:
  - `data class YGToppingImage.Local(@DrawableRes val drawableRes: Int) : YGToppingImage`
  - `internal data class GroupListEmptyDummyGroup(@DrawableRes val imageRes: Int, @StringRes val nameRes: Int, val minutesAgo: Int, val chipType: YGGrouptagChipType, val type: YGToppingGroupType)`
  - `internal val GROUP_LIST_EMPTY_DUMMY_GROUPS: List<GroupListEmptyDummyGroup>` — 순서가 곧 `ToppingLayout` 자리(0 왼쪽 상단, 1 오른쪽 중단, 2 왼쪽 하단)이자 등장 순서
  - `R.string.group_list_empty_dummy_name_collector`, `_parfait`, `_daily`

- [x] **Step 1: 실패하는 테스트를 쓴다** — `GroupListEmptyDummyGroupTest.kt`

```kotlin
@Test
fun dummyGroups_followTheFigmaOrderAndVariants() {
    val dummies = GROUP_LIST_EMPTY_DUMMY_GROUPS

    assertEquals(3, dummies.size)
    assertEquals(
        listOf(
            R.string.group_list_empty_dummy_name_collector,
            R.string.group_list_empty_dummy_name_parfait,
            R.string.group_list_empty_dummy_name_daily,
        ),
        dummies.map { it.nameRes },
    )
    assertEquals(listOf(1, 2, 3), dummies.map { it.minutesAgo })
    assertEquals(
        listOf(YGGrouptagChipType.TYPE_5_6, YGGrouptagChipType.TYPE_1_2, YGGrouptagChipType.TYPE_3_4),
        dummies.map { it.chipType },
    )
    assertEquals(
        listOf(YGToppingGroupType.TYPE_2_LEFT, YGToppingGroupType.TYPE_1_RIGHT, YGToppingGroupType.TYPE_2_LEFT),
        dummies.map { it.type },
    )
}
```

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*GroupListEmptyDummyGroupTest'`
Expected: 컴파일 실패 — `GROUP_LIST_EMPTY_DUMMY_GROUPS` 미정의

- [x] **Step 3: 이미지 3장을 Figma에서 받아 프레임에 맞춰 자른다**

`figma:figma-design-to-code` 스킬을 먼저 불러오고, `get_design_context(fileKey = "QPoxqbNMNktsi8ktua3gMN", nodeId = "5417:6332")`로 에셋 URL을 새로 받는다(URL은 수명이 짧다). 반환 코드에서 `data-node-id`가 아래 노드인 `Topping-Group` 안 `<img>`의 `src`가 원본이다.

원본은 잘리지 않은 채 온다. 96dp 정사각 프레임 안에서의 배치가 아래 비율(프레임 한 변 대비 %)이다. 384×384 투명 캔버스에 원본을 `(w%, h%)` 크기로 줄여 `(left%, top%)`에 얹고 캔버스 밖은 버린다. 회전은 굽지 않는다.

| 파일 | 노드 | left | top | w | h |
|---|---|---|---|---|---|
| `img_group_list_dummy_matcha.png` | `5417:6414` | -12.94 | -14.08 | 111.04 | 138.8 |
| `img_group_list_dummy_cap.png` | `5417:6428` | -16.19 | -33.79 | 133.06 | 167.13 |
| `img_group_list_dummy_camera.png` | `5417:6421` | 19.47 | -17.89 | 91.26 | 121.69 |

합성 도구는 저장소에 없다. 스크래치패드에 `python3 -m venv`를 만들고 Pillow를 깔아 쓴다(저장소에 스크립트를 남기지 않는다).

검증: `sips -g pixelWidth -g pixelHeight -g hasAlpha <파일>` 이 셋 다 `384`, `384`, `yes`. 그리고 각 파일을 `Read`로 열어 Figma 스크린샷의 피사체와 같은 구도인지 본다.

- [x] **Step 4: `YGToppingImage.Local`을 더한다**

`YGToppingImage.kt`에 `@Immutable data class Local(@DrawableRes val drawableRes: Int)`(이웃 케이스와 같은 어노테이션). `YGToppingGroup.kt`의 `when (image)`에 분기를 더해 `Template`과 같은 방식(`painterResource`, `ContentScale.Fit`, `imageModifier`)으로 그린다. `Template` KDoc 옆에 한 줄 — 디자인 시스템 밖 모듈이 가진 이미지를 넘기는 길이라는 것.

- [x] **Step 5: 더미 정의를 쓴다** (`P/model/GroupListEmptyDummyGroup.kt`)

`strings.xml`에 `group_list_empty_dummy_name_collector`=`예카수집가`, `_parfait`=`파르페`, `_daily`=`일상`. `GROUP_LIST_EMPTY_DUMMY_GROUPS`는 Step 1 테스트의 순서·값 그대로, 이미지는 순서대로 matcha, cap, camera.

- [x] **Step 6: 통과를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest :core:designsystem:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 7: 커밋**

```bash
git add core/designsystem feature/groups/list/impl
git commit -m "feat: 그룹 목록 Empty 더미 그룹 이미지와 정의를 추가한다"
```

---

### Task 3: 인트로 단계·타임라인·이징 (순수 로직)

**Files:**
- Create: `P/route/component/GroupListEmptyIntroPhase.kt`
- Test: `T/route/component/GroupListEmptyIntroPhaseTest.kt`

**Interfaces:**
- Produces:
  - `internal enum class GroupListEmptyIntroPhase { Entering, Shown, Dismissing, Dismissed }`
  - `internal fun GroupListEmptyIntroPhase.onTouchDown(): GroupListEmptyIntroPhase`
  - `internal fun GroupListEmptyIntroPhase.onStop(): GroupListEmptyIntroPhase`
  - `internal fun GroupListEmptyIntroPhase.enterValue(animated: Float): Float` — 등장 값(더미 진행도·툴팁 알파)을 단계로 해석
  - `internal fun GroupListEmptyIntroPhase.exitValue(animated: Float): Float` — 종료 배율을 단계로 해석
  - `internal object GroupListEmptyIntroTimeline` — `INITIAL_DELAY_MILLIS`, `DUMMY_STAGGER_MILLIS`, `DUMMY_DURATION_MILLIS`, `TOOLTIP_DELAY_MILLIS`, `TOOLTIP_DURATION_MILLIS`, `ENTER_TOTAL_MILLIS`, `EXIT_DURATION_MILLIS`(전부 `Int`), `fun dummyDelayMillis(index: Int): Int`, `val DummyEnterEasing: Easing`, `val ExitEasing: Easing`, `val TooltipEnterEasing: Easing`

- [x] **Step 1: 실패하는 테스트를 쓴다** — `GroupListEmptyIntroPhaseTest.kt`

```kotlin
@Test
fun onTouchDown_whileEntering_staysEntering() {
    assertEquals(Entering, Entering.onTouchDown())
}

@Test
fun onTouchDown_whileShown_startsDismissing() {
    assertEquals(Dismissing, Shown.onTouchDown())
}

@Test
fun onTouchDown_whileDismissingOrDismissed_keepsPhase() {
    assertEquals(Dismissing, Dismissing.onTouchDown())
    assertEquals(Dismissed, Dismissed.onTouchDown())
}

@Test
fun onStop_skipsTheRunningAnimationToItsEnd() {
    assertEquals(Shown, Entering.onStop())
    assertEquals(Dismissed, Dismissing.onStop())
}

@Test
fun onStop_whileSettled_keepsPhase() {
    assertEquals(Shown, Shown.onStop())
    assertEquals(Dismissed, Dismissed.onStop())
}

@Test
fun enterValue_followsTheAnimationOnlyWhileEntering() {
    assertEquals(0.3f, Entering.enterValue(0.3f))
    assertEquals(1f, Shown.enterValue(0.3f))
    assertEquals(1f, Dismissing.enterValue(0.3f))
    assertEquals(1f, Dismissed.enterValue(0.3f))
}

@Test
fun exitValue_followsTheAnimationOnlyWhileDismissing() {
    assertEquals(1f, Entering.exitValue(0.4f))
    assertEquals(1f, Shown.exitValue(0.4f))
    assertEquals(0.4f, Dismissing.exitValue(0.4f))
    assertEquals(0f, Dismissed.exitValue(0.4f))
}

@Test
fun timeline_matchesThePolicy() {
    assertEquals(listOf(500, 1000, 1500), (0..2).map(GroupListEmptyIntroTimeline::dummyDelayMillis))
    assertEquals(1000, GroupListEmptyIntroTimeline.DUMMY_DURATION_MILLIS)
    assertEquals(2500, GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS)
    assertEquals(500, GroupListEmptyIntroTimeline.TOOLTIP_DURATION_MILLIS)
    assertEquals(3000, GroupListEmptyIntroTimeline.ENTER_TOTAL_MILLIS)
    assertEquals(300, GroupListEmptyIntroTimeline.EXIT_DURATION_MILLIS)
}

@Test
fun timeline_tooltipStartsWhenTheLastDummyLands_andEndsTheEntrance() {
    val lastDummyEnd = GroupListEmptyIntroTimeline.dummyDelayMillis(2) +
        GroupListEmptyIntroTimeline.DUMMY_DURATION_MILLIS

    assertEquals(GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS, lastDummyEnd)
    assertEquals(
        GroupListEmptyIntroTimeline.ENTER_TOTAL_MILLIS,
        GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS + GroupListEmptyIntroTimeline.TOOLTIP_DURATION_MILLIS,
    )
}

@Test
fun tooltipEnterEasing_followsTheFigmaSpringSamples() {
    val easing = GroupListEmptyIntroTimeline.TooltipEnterEasing

    assertEquals(0f, easing.transform(0f))
    assertEquals(1f, easing.transform(1f))
    assertEquals(0.3076f, easing.transform(0.1f), 0.0005f)
    assertEquals(0.9754f, easing.transform(0.5f), 0.0005f)
    // 샘플 사이는 선형으로 잇는다
    assertEquals((0.0216f + 0.0747f) / 2, easing.transform(0.03f), 0.0005f)
}
```

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*GroupListEmptyIntroPhaseTest'`
Expected: 컴파일 실패 — 심볼 미정의

- [x] **Step 3: `GroupListEmptyIntroPhase.kt`를 구현한다**

전이·해석 함수는 테스트가 정한 그대로다. `DummyEnterEasing`·`ExitEasing`은 Global Constraints의 `CubicBezierEasing`.

`TooltipEnterEasing`은 아래 51개 샘플을 0..1에 균등 간격으로 놓고 사이를 선형 보간한다. `fraction >= 1f`이면 `1f`를 돌려준다(마지막 샘플이 1에 못 미친다). 샘플은 Figma 노드 `5417:6432`가 스프링(mass 1, stiffness 80, damping 20)을 500ms에 맞춰 내보낸 곡선이고, 그 사실을 KDoc 한 줄로 남긴다.

```
0, 0.0216, 0.0747, 0.1458, 0.2255, 0.3076, 0.3879, 0.4638, 0.5339, 0.5974,
0.6542, 0.7044, 0.7484, 0.7866, 0.8196, 0.8479, 0.8722, 0.8928, 0.9103, 0.9251,
0.9375, 0.948, 0.9568, 0.9642, 0.9703, 0.9754, 0.9797, 0.9832, 0.9862, 0.9886,
0.9906, 0.9923, 0.9936, 0.9948, 0.9957, 0.9965, 0.9971, 0.9976, 0.9981, 0.9984,
0.9987, 0.9989, 0.9991, 0.9993, 0.9994, 0.9995, 0.9996, 0.9997, 0.9997, 0.9998,
0.9998
```

- [x] **Step 4: 통과를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*GroupListEmptyIntroPhaseTest' ktlintCheck`
Expected: PASS

- [x] **Step 5: 커밋**

```bash
git add feature/groups/list/impl
git commit -m "feat: 그룹 목록 Empty 인트로의 단계 전이와 타임라인을 추가한다"
```

---

### Task 4: 인트로 상태 홀더, 툴팁, 화면 연결

**Files:**
- Create: `P/route/component/GroupListEmptyIntroState.kt`
- Create: `P/route/component/GroupListEmptyDummyGroups.kt`
- Modify: `P/route/component/GroupListTooltip.kt`
- Modify: `P/route/component/GroupListTopBar.kt`
- Modify: `P/route/GroupListScreen.kt`
- Modify: `P/route/GroupListErrorScreen.kt` (주석만)
- Modify: `P/route/GroupListViewModel.kt`
- Test: `T/route/GroupListViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 `isEmptyConfirmed`, `GroupListTopBar(count, …)`. Task 2 `GROUP_LIST_EMPTY_DUMMY_GROUPS`, `YGToppingImage.Local`. Task 3 전부
- Produces:

```kotlin
@Stable
internal class GroupListEmptyIntroState {
    var phase: GroupListEmptyIntroPhase   // private set, 초기 Entering
    fun dummyProgress(index: Int): Float  // phase.enterValue(더미 Animatable 값)
    val tooltipAlpha: Float               // phase.enterValue(툴팁 Animatable 값)
    val exitAlpha: Float                  // phase.exitValue(종료 Animatable 값)

    suspend fun play()
    suspend fun dismiss()
    fun onTouchDown()
    fun onStop()

    companion object {
        /** 프리뷰용. 애니메이션 없이 그 단계로 시작한다 */
        fun settled(phase: GroupListEmptyIntroPhase): GroupListEmptyIntroState
    }
}

@Composable
internal fun rememberGroupListEmptyIntroState(enabled: Boolean): GroupListEmptyIntroState

@Composable
internal fun GroupListEmptyDummyGroups(intro: GroupListEmptyIntroState)
```

- [x] **Step 1: ViewModel에서 툴팁 상태를 걷는다**

`GroupListViewModelTest.kt`에서 `noGroups_showsTheTooltip`, `withGroups_hidesTheTooltip`, `beforeTheFirstEmission_hidesTheTooltip`, `firstGroupIsCreated_dismissesTheTooltip`을 지운다 — 같은 조건을 Task 1의 `isEmptyConfirmed` 테스트가 잡는다. `GroupListUiState.isTooltipVisible`과 `observeGroups()`의 대입, 그 KDoc의 툴팁 문단을 지운다.

- [x] **Step 2: `GroupListEmptyIntroState`를 구현한다**

- 더미마다 `Animatable(0f)` 하나, 툴팁 `Animatable(0f)`, 종료 `Animatable(1f)`
- `play()`: `coroutineScope` 안에서 더미 3개와 툴팁을 **각각 `launch`로 동시에** 띄운다. 더미 `index`는 `launch { delay(dummyDelayMillis(index)); animateTo(1f, tween(DUMMY_DURATION_MILLIS, easing = DummyEnterEasing)) }`, 툴팁은 `launch { delay(TOOLTIP_DELAY_MILLIS); animateTo(1f, tween(TOOLTIP_DURATION_MILLIS, easing = TooltipEnterEasing)) }`. `coroutineScope`가 전부 끝난 뒤 `phase`가 아직 `Entering`일 때만 `Shown`으로 올린다 — 3초 탭 게이트가 이 전이다. 순차로 돌리면 8초가 되는데 단위 테스트는 이를 못 잡는다(Task 5 에뮬레이터 확인이 유일한 검증)
- `dismiss()`: 종료 `Animatable`을 `animateTo(0f, tween(EXIT_DURATION_MILLIS, easing = ExitEasing))`, 끝나면 `phase`가 `Dismissing`일 때만 `Dismissed`
- `onTouchDown()`, `onStop()`: `phase = phase.onTouchDown()` / `phase.onStop()`
- 값 getter는 Task 3의 `enterValue`·`exitValue`를 거친다. 그래서 `onStop()`은 `Animatable`을 건드리지 않고 단계만 바꿔도 화면이 완료 상태가 된다 — 이 이유를 KDoc에 남긴다

- [x] **Step 3: `rememberGroupListEmptyIntroState(enabled)`를 구현한다**

- `remember(enabled) { GroupListEmptyIntroState() }` — 0건이 풀렸다 다시 0건이 되면 새 상태로 처음부터 재생된다
- `LaunchedEffect(state, enabled)`: `enabled`이고 `phase == Entering`이면 `state.play()`
- `LaunchedEffect(state, state.phase)`: `phase == Dismissing`이면 `state.dismiss()`
- `LifecycleStartEffect(state) { onStopOrDispose { state.onStop() } }`

- [x] **Step 4: `GroupListEmptyDummyGroups`를 구현한다**

`GROUP_LIST_EMPTY_DUMMY_GROUPS`를 돌며 `YGToppingGroup`을 **감싸는 레이아웃 없이** 바로 낸다 — `ToppingLayout`은 직계 자식을 자리 단위로 센다. 각 항목 modifier:

```kotlin
Modifier
    .graphicsLayer {
        val progress = intro.dummyProgress(index)
        translationY = (1f - progress) * DUMMY_ENTER_OFFSET_Y.toPx()   // -60.dp
        alpha = progress * intro.exitAlpha
    }
    .clearAndSetSemantics { }
```

`DUMMY_ENTER_OFFSET_Y`는 이 파일의 `private val`(`(-60).dp`)이다. 클릭 modifier는 붙이지 않는다. `image = YGToppingImage.Local(imageRes)`, `timestamp = stringResource(R.string.group_list_timestamp_minutes, minutesAgo)`.

- [x] **Step 5: 툴팁 문구·색을 바꾼다** (`GroupListTooltip.kt`)

문구는 Global Constraints 그대로, 두 행 사이는 `\n`. 강조 구절 셋의 색과 테두리·화살표 색을 `YGAtomicColors.Soda.Soda500`으로 바꾼다. 나머지(패딩·화살표 치수·기존 `buildAnnotatedString` 방식)는 그대로 둔다.

- [x] **Step 6: `GroupListTopBar`에서 팝업을 걷는다**

`TooltipBox`·`rememberTooltipState`·`LaunchedEffect(showTooltip)`와 `isTooltipVisible` 파라미터를 지우고 `rightContent`에는 `YGChipButton`만 남긴다. 필요 없어진 `@OptIn(ExperimentalMaterial3Api::class)`와 import도 지운다. KDoc의 "칩과 툴팁을 함께 감춘다"와 `GroupListErrorScreen.kt`의 같은 취지 주석을 칩만으로 고친다.

- [x] **Step 7: `GroupListScreen`에 연결한다**

- `GroupListScreen`에 파라미터 `intro: GroupListEmptyIntroState = rememberGroupListEmptyIntroState(enabled = uiState.isEmptyConfirmed)`를 더한다(`modifier` 뒤). 화면 지역값 `groupList`(새로고침 중 비워진다)를 보지 않는다. `GroupListRoute`는 이 인자를 넘기지 않는다
- 루트 `Box`의 modifier 끝에 터치 다운 관찰을 붙인다. 소비하지 않는다:

```kotlin
.pointerInput(intro) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        intro.onTouchDown()
    }
}
```

- `GroupListContent`에 `emptyIntro: GroupListEmptyIntroState? = null` 파라미터를 더한다. `ToppingLayout` 내용에서 `emptyIntro != null`이면 `GroupListEmptyDummyGroups(emptyIntro)`를, 아니면 기존 그룹 루프를 낸다. 호출부는 `emptyIntro = intro.takeIf { uiState.isEmptyConfirmed }`
- 더미를 낼 때는 `ToppingLayout`에 `reveal = RevealState.AllRevealed`를 넘긴다 — 기존 순차 드러내기 상태는 총 0개라 더미 자리를 높이에서 빼 버리고, 그러면 파르페가 더미를 덮을 만큼 자라지 않는다
- `GroupListPullToRefreshBox`를 `Box`로 감싸고, `uiState.isEmptyConfirmed && isTooltipVisible`일 때 그 위에 툴팁을 겹친다. `isTooltipVisible`은 `remember(intro) { derivedStateOf { intro.tooltipAlpha * intro.exitAlpha > 0f } }` — 보이지 않는 동안 컴포지션에서 빼야 TalkBack이 안 보이는 문구를 읽지 않는다:

```kotlin
GroupListTooltip(
    modifier = Modifier
        .align(Alignment.TopEnd)
        .graphicsLayer { alpha = intro.tooltipAlpha * intro.exitAlpha }
        .padding(top = 16.dp, end = YGTheme.layout.padding.padding7),
)
```

  `graphicsLayer`가 `padding`보다 **바깥**이어야 한다. `drawTooltipCornerTop`은 화살표를 본체 경계 위쪽 밖에 그리는데, 알파가 1 미만이면 레이어가 자기 경계로 잘라 화살표가 페이드 내내 사라진다. `top = 16.dp`는 팝업 시절과 같은 값으로 화살표가 그려질 자리다
- `groupList` 주석의 "둘을 가르는 일은 툴팁 쪽(isTooltipVisible)이 맡는다"를 `isEmptyConfirmed`로 고친다
- 프리뷰: `GroupListScreenPreviewParameterProvider`에서 0건 케이스를 빼고 `isTooltipVisible` 인자를 지운다. 0건은 `@YGPreview` 함수 둘을 따로 둔다 — `GroupListScreenEmptyShownPreview`(`intro = GroupListEmptyIntroState.settled(Shown)`), `GroupListScreenEmptyDismissedPreview`(`settled(Dismissed)`)

- [x] **Step 8: 통과를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest :feature:groups:list:impl:assembleDebug ktlintCheck`
Expected: BUILD SUCCESSFUL, 테스트 전부 PASS

- [x] **Step 9: 커밋**

```bash
git add feature/groups/list/impl
git commit -m "feat: 그룹 목록이 비어 있을 때 더미 그룹과 툴팁 등장·종료 애니메이션을 추가한다"
```

---

### Task 5: 에뮬레이터 확인과 문서

**Files:**
- Modify: `docs/status.md` (그룹 목록 G-001 블록)
- Modify: `docs/superpowers/specs/2026-10-03-g001-empty-animation-design.md`, `docs/superpowers/specs/README.md` (`status`)
- Modify: `docs/superpowers/plans/2026-10-03-g001-empty-animation.md`, `docs/superpowers/plans/README.md` (`status`)
- Modify: `docs/synthesis/open-questions.md`, `docs/architecture/design-system.md`

**Interfaces:**
- Consumes: Task 1~4 전부

- [ ] **Step 1: 그룹 0건 계정으로 앱을 띄워 확인한다**

`android-emulator-skill`로 빌드·설치·실행한다. 0건 계정 로그인이 필요하다 — 없으면 멈추고 사용자에게 요청한다. 화면 녹화(`adb shell screenrecord`)로 타이밍을 본다.

| 확인 | 기대 |
|---|---|
| 진입 직후 | 0.5초 동안 빈 파르페만. 앱 첫 실행에서는 이 구간에 크림이 토핑 3개 분량으로 자란다(스펙 "주의") |
| 등장 | 왼쪽 상단 → 오른쪽 중단 → 왼쪽 하단 순, 0.5초 간격, 위에서 내려오며 나타남. 2.5초에 툴팁 |
| 3초 이전 탭 | 아무 일도 없다. 3초가 지나도 저절로 닫히지 않는다 |
| 3초 이후 탭 | 더미 3개와 툴팁이 함께 0.3초에 사라진다. 파르페 높이는 그대로 |
| 등장 중 `그룹 추가하기` | 추가 오버레이가 바로 열린다 |
| 3초 이후 `그룹 추가하기` | 오버레이가 열리고 더미·툴팁도 사라진다 |
| 설정 화면 갔다 복귀 | 처음부터 다시 재생 |
| 등장 1.5초쯤 홈으로 내렸다 복귀 | 더미 3개와 툴팁이 전부 드러난 상태. 탭하면 닫힌다 |
| 닫은 뒤 홈으로 내렸다 복귀 | 닫힌 채 유지 |
| 닫은 뒤 당겨서 새로고침 | 다시 재생하지 않는다 |
| 그룹을 하나 만든 뒤 목록 | 더미·툴팁 없음, 상단 바 `내 그룹 1` |
| 그룹이 있는 계정에서 당겨서 새로고침 | 더미·툴팁이 스치지 않는다 |
| 그 그룹을 나간 뒤 목록 | 다시 재생, 상단 바 `내 그룹 0` |
| 툴팁 모양 | Figma 스크린샷(`get_screenshot` nodeId `5417:6332`)과 문구·색·화살표 위치가 맞는다. 화살표가 `그룹 추가하기` 칩을 가리킨다 |
| 툴팁 페이드 | 등장·종료 내내 화살표가 본체와 함께 보인다 |
| 등장 1초쯤 설정 화면으로 이동 | 전환 중에 더미가 한꺼번에 튀어나오지 않는다 |
| 3초 이후 추가 오버레이를 연 뒤 오버레이 바깥 탭으로 닫기 | 목록으로 돌아왔을 때 더미·툴팁이 닫혀 있다(오버레이를 여는 탭이 이미 닫는다) |
| TalkBack 켜고 진입 직후 | 툴팁 문구와 더미 그룹명을 읽지 않는다. 툴팁이 뜬 뒤에는 문구를 읽는다 |

어긋나는 항목은 Task 4 파일에서 고치고 이 표를 다시 돈다.

- [x] **Step 2: 문서를 현재 상태로 맞춘다**

- `docs/status.md` G-001 블록의 "상태" 문단을 덮어쓴다 — 0건일 때의 인트로(더미 3개·툴팁, 재진입마다 재생, 탭 종료)와 상단 바 표기. 앵커에 `GroupListEmptyIntroState`, `GROUP_LIST_EMPTY_DUMMY_GROUPS`를 더하고 설계 링크에 이 스펙을 더한다. 변경 서술("~로 바꿨다")을 쓰지 않는다
- `docs/synthesis/open-questions.md`에서 이 변경으로 풀린 것을 정리한다. `OQ-P-082`(Top Bar 날짜 표기)는 날짜가 사라져 통째로 지운다. `OQ-P-047`은 ③ 해소 메모가 `isTooltipVisible`을 현재 사실로 적고 있으니 현재 상태(노출은 `isEmptyConfirmed`와 인트로 단계가 정한다)로 고쳐 쓰고, ④는 문구가 Figma 정책으로 정해졌음을 반영해 남은 미결만 남긴다
- `docs/architecture/design-system.md`의 `YGTopBar`·`YGToppingImage` 서술이 `date`/`day`나 케이스 목록을 적고 있으면 현재 시그니처로 고친다
- `docs/synthesis/open-questions.md`에 한 항목 — `G-001-Empty` 애니메이션 정책 원본이 `wiki/raw`에 없고, 위키의 툴팁 정책("0건이면 항상 뜬다")과 구현(등장 타임라인 끝에 뜨고 탭하면 재진입 전까지 안 뜬다)이 어긋난다. 그 파일의 기존 항목 형식과 번호 규칙을 따른다
- 스펙 frontmatter `status: implemented`, 계획 frontmatter `status: done`·`archived_reason`, 두 README 표의 상태 칸. `archive/`로의 이동은 develop 머지 뒤에 한다

- [x] **Step 3: 링크를 확인한다**

Run: `python3 docs/script/check_links.py docs`
Expected: `깨진 링크 0건`

- [x] **Step 4: 커밋**

```bash
git add docs
git commit -m "docs: 그룹 목록 Empty 안내 애니메이션 구현 상태를 반영한다"
```
