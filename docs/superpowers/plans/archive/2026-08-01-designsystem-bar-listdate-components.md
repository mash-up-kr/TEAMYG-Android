---
id: designsystem-bar-listdate-components
title: List-Date·Floating Bar 신설 + Top Bar Canvas 변형 구현 계획
status: done
type: work-order
created: 2026-08-01
updated: 2026-08-04
platforms: android
owner: TJYG-Android 디자인시스템
related_adr:
  - ADR-0018
related_spec:
  - designsystem-bar-listdate-components
related_code:
  - YGListDate.kt#YGListDate
  - YGFloatingBar.kt#YGFloatingBarBackClose
  - YGFloatingBar.kt#YGFloatingBarClose
  - YGFloatingBar.kt#YGFloatingBarEdit
  - YGFloatingBar.kt#YGFloatingBarEditTab
  - YGTopBar.kt#YGTopBarCanvas
  - YGTopBar.kt#YGTopBarEmpty
  - YGTopBar.kt#YGTopBarContent
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
archived_reason: PR #188로 develop 머지 완료(2026-08-04) — Task 1~7 산출물 전량 반영, 드리프트 0
tags: [plan, parfait, designsystem, figma-sync, top-bar, floating-bar, c-201]
---

# List-Date·Floating Bar·Top Bar Canvas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는
> superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

> ⚠️ **개정(2026-08-01, PR #173 develop 머지 반영)** — `YGTopBarDefault`가 develop에서 삭제되고
> `YGTopBarEmpty(rightContent)`로 통합됐다. Task 3의 "Default 드리프트 제거"는 대상이 사라져
> **프리뷰 칩 색 정정**으로 축소했고, 기준 시그니처를 #173 이후 코드로 갱신했다. 이 개정 시점에는
> 착수 전이었고, 실행은 그 뒤에 이뤄졌다(아래 실행 결과).

> **실행 결과(2026-08-01)** — Task 1~4 전량 완료. subagent-driven-development로 Task마다 새
> 서브에이전트 + 리뷰를 돌렸다. Task 1·3은 리뷰 1회에 클린 통과, Task 2만 fix round 1회.
> 실행 시점에는 **TJYG-Android를 커밋하지 않았다**(작업자 지시). 브랜치 **`feature/sync-component`**에
> 작업 트리 변경만 남아 있었고, 이후 작업자가 커밋해 **PR #188로 develop 머지**됐다(2026-08-04).
> 머지 코드와 설계의 갈림은 없다 — as-built 3건은 [스펙](../../specs/archive/2026-08-01-designsystem-bar-listdate-components.md) 머지 블록 참고.
> 본문 체크박스는 실행 기록을 이 블록에 모으는 관례를 따라 그대로 둔다.
>
> **실행 도중 베이스가 바뀌었다.** 작업을 중단했다 재개하는 사이 `develop`이 나아갔고(#173 머지),
> 브랜치도 `feature/bar-listdate-component` → `feature/sync-component`로 옮겨졌다. Task 1·2 산출물은
> 새 베이스 위에 그대로 살아남았다. 위 ⚠️ 개정이 그 대응이다.
> 교훈: 며칠에 걸치는 계획은 Task 착수 시점마다 대상 파일의 현재 상태를 다시 읽어야 한다.
>
> **계획 자체의 결함 2건 — 둘 다 최종 전체 리뷰가 잡았다.** Task 단위 리뷰 3회는 전부 통과했다.
> Task 3 Step 2가 지정한 `Text` → `Spacer(weight(1f))` → `memberContent()` 배치와, Task 2 Step 1의
> `YGFloatingBarEdit` 중앙 `Text`가 **같은 종류의 측정 버그**를 갖고 있었다 — 가중치 없는 `Text`가
> 먼저 측정돼 긴 문자열이 잔여 폭을 다 먹으면 옆 요소(멤버 칩 / 확인 버튼)가 0dp로 밀린다.
> 계획서가 코드를 그대로 명시했으므로 plan-mandated 충돌로 작업자에게 질의 후 수정했다.
> **프리뷰·실기기 육안이 이걸 놓친 이유는 모든 샘플 제목이 4자였기 때문**이고, 재발 방지로 두
> 컴포넌트 프리뷰에 긴 제목 변형을 상설했다. 계획서에 UI 코드를 박을 때는 **사용자 입력값을 받는
> 텍스트마다 긴 문자열 케이스를 함께 지정**해야 한다.
>
> **Task 2 리뷰가 잡은 것** — 닫기 블록 4회·확인 블록 2회의 문자 그대로 중복. 계획서가 그 코드를
> 명시했으므로 질의 후 `YGFloatingBarCloseButton`·`YGFloatingBarConfirmButton` 추출로 개정했다.
> 1회 사용인 뒤로가기 버튼은 추출하지 않았다.
>
> **재리뷰 오판 1건** — Task 2 fix의 import 정렬을 재리뷰가 NOT ADDRESSED로 판정했으나, 컨트롤러가
> 세 파일을 직접 확인해 **정렬 수열 기준으로 올바른 자리**임을 확인하고 ADDRESSED로 뒤집었다.
> 재리뷰가 defect로 본 것은 손대지 말라고 지시한 **기존 파일의 정렬 결함**이었다. 최종 리뷰도 이
> 판정을 확인했다(`.editorconfig`가 `ktlint_standard_import-ordering`을 끈 상태라 위반 자체가 불가).
>
> **검증은 컨트롤러가 직접 수행했다** — repo 전체 `assembleDebug`·`ktlintCheck` 통과,
> `:app-preview:installDebug` 후 실기기(SM-A356N)에서 3개 화면 스크린샷 대조. Task 1 구현자 리포트의
> gradle 출력이 재포맷돼 신뢰할 수 없던 이월 건도 이로써 닫혔다.
>
> **블러가 한 번 더 뒤집혔다(Task 6 이후)** — 자체 `GraphicsLayer` 구현을 "동작 확인"으로 보고했으나
> **오검증이었다.** 40dp 극단값으로 대조하자 블러가 전혀 걸리지 않는 것이 드러났고(틴트만으로도 대비가
> 낮아져 흐린 것처럼 보였다), 세 형태를 모두 시도한 뒤 **Haze로 되돌려 즉시 동작을 확인**했다.
> 아래 Task 6 본문의 `GraphicsLayer` 코드는 **폐기된 형태**다 — 실제 구현은 `hazeSource`/`hazeEffect`이고
> 경위는 [ADR-0018](../../../adr/0018-backdrop-blur-haze.md).
> 교훈: **블러는 어긋나도 눈에 잘 띄지 않는다. 반드시 극단값 대조로 검증한다.**
>
> **미검증**: pressed 상태, 긴 제목 케이스의 실기기 렌더(갤러리에 긴 제목 섹션 없음 — 후속 과제).
> API 31 미만 폴백(검증 기기가 API 36).

**Goal:** Figma `List-Date`·`Floating Bar`를 `:core:designsystem`에 신설하고, `Top Bar`에 `Canvas`
변형을 더한 뒤 `:app-preview` 갤러리에서 실기기로 검증한다.

**Architecture:** 세 컴포넌트가 서로를 소비하지 않으므로 독립 Task 3개 + 통합 검증 Task 1개다.
전부 기존 부품(`YGDateButton`·`YGChipColorIndicator`·`YGCircleButton`·`YGEditTabButton`·`YGIconButton`)
위에 얹는 합성이라 **부품 쪽 파일은 한 줄도 고치지 않는다.** Task 3만 기존 파일(`YGTopBar.kt`)을 수정한다.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Navigation3, Gradle 컨벤션 플러그인.

## Global Constraints

- 작업 대상 repo는 **`TJYG-Android`**(이 repo 아님). 로컬 절대경로는
  `wiki/personal-private/project-paths.md` 참고. 아래 모든 경로는 그 repo 루트 기준.
- **TJYG-Android에 커밋하지 않는다**(작업자 지시). 작업 트리 변경만 남기고 보고한다.
  각 Task 말미의 "커밋" 단계는 **의도적으로 없다.**
- 테스트를 작성하지 않는다. 선행 디자인시스템 라운드 6회에서 확립된 판단 — 상태 없는 순수 렌더
  컴포넌트라 단위 테스트가 잡을 회귀가 거의 없고, 실제 결함(정렬·잘림·색 대비)은 육안 검증에서만
  드러난다. Task별 검증 사이클은 **`assembleDebug` + `ktlintCheck` + 프리뷰/갤러리 육안**이다.
- 색·치수는 반드시 **토큰 심볼**로 참조한다. hex 리터럴·raw dp 금지. 예외는 갤러리 화면의
  레이아웃 여백뿐이며, 이는 기존 프리뷰 화면들이 이미 `16.dp`·`8.dp`를 직접 쓰는 관행을 따른다.
- 프리뷰 관용구는 `@YGPreview` + `PreviewBox` 고정(`@Preview` + `YGCustomTheme` 금지).
- 패키지명은 소문자 컴포넌트명(`component/yglistdate/`), 파일명은 PascalCase.
- ktlint는 **repo 전체**(`./gradlew ktlintCheck`)로 돌린다. 모듈 단위로만 돌리면 갤러리 모듈
  위반을 놓친다.
- **신규 토큰·에셋을 만들지 않는다.** 이번 라운드에 필요한 `SizeTokens.Size44`, `gap.gap1`,
  `padding.padding3`/`padding6`/`padding7`, 드로어블 `ic_caret_left`·`ic_close`·`ic_check`·
  `ic_hamburger`가 모두 실재한다(2026-08-01 확인).

---

## Task 1: `YGListDate` 신설

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yglistdate/YGListDate.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGListDate.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGListDatePreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: 기존 `YGDateButton(text: String, isSelected: Boolean, isToday: Boolean, isEnabled: Boolean, onClick: () -> Unit, modifier: Modifier)`,
  기존 `YGChipColorIndicator(modifier: Modifier = Modifier, isChecked: Boolean)`
- Produces: `YGListDate(text: String, isSelected: Boolean, isToday: Boolean, isEnabled: Boolean, isUploaded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`
  — 반환값 없는 `@Composable`. C-201 캘린더 패널이 나중에 격자로 배치한다.

**배경:** Figma `List-Date`는 44dp 날짜 버튼 아래 2dp 띄우고 4dp 업로드 점을 붙인 44×50 셀이다.
두 부품이 이미 있으므로 합성만 한다. `Upload=False`는 Figma에서 `opacity-0`이고
`YGChipColorIndicator`가 미체크 시 `Color.Transparent`를 그리므로 **자리를 유지한 채 비노출**된다 —
선택이 바뀌어도 셀 높이가 흔들리지 않는다.

> ⚠️ **계획서 결함(2026-08-01 코드리뷰) — 아래 코드블록은 역사 스냅샷이다.** Step 1·3의
> `YGChipColorIndicator(isChecked = isUploaded)`는 C-201 정책의 예외 조항("Button-Date가 Disabled면
> 항상 False")을 빠뜨렸고, 그 결과 Step 3 갤러리 코드가 `isEnabled = false, isUploaded = true` 위반
> 조합을 그대로 노출했다. **현행은 `isChecked = isEnabled && isUploaded`** — 규칙을 컴포넌트가 강제한다
> ([spec](../../specs/archive/2026-08-01-designsystem-bar-listdate-components.md#yglistdate)). 원인은 계획서가
> 부품 시그니처만 옮기고 정책 문서의 "예외" 줄을 옮기지 않은 것.

- [ ] **Step 1: `YGListDate.kt` 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.yglistdate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGChipColorIndicator
import com.teamyg.parfait.core.designsystem.component.ygdatebutton.YGDateButton
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma List-Date
 */
@Composable
fun YGListDate(
    text: String,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    isUploaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
        modifier = modifier,
    ) {
        YGDateButton(
            text = text,
            isSelected = isSelected,
            isToday = isToday,
            isEnabled = isEnabled,
            onClick = onClick,
            modifier = Modifier.size(SizeTokens.Size44.getDp()),
        )
        YGChipColorIndicator(isChecked = isUploaded)
    }
}

@YGPreview
@Composable
private fun YGListDatePreview() = PreviewBox {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        modifier = Modifier.background(color = Color.White),
    ) {
        YGListDate(
            text = "31",
            isSelected = false,
            isToday = false,
            isEnabled = true,
            isUploaded = true,
            onClick = {},
        )
        YGListDate(
            text = "31",
            isSelected = true,
            isToday = false,
            isEnabled = true,
            isUploaded = true,
            onClick = {},
        )
        YGListDate(
            text = "31",
            isSelected = false,
            isToday = true,
            isEnabled = true,
            isUploaded = false,
            onClick = {},
        )
        YGListDate(
            text = "31",
            isSelected = false,
            isToday = false,
            isEnabled = false,
            isUploaded = false,
            onClick = {},
        )
    }
}
```

- [ ] **Step 2: 갤러리 NavKey 작성**

Create `app-preview/.../navigation/key/NavKeyYGListDate.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGListDate : NavKey
```

- [ ] **Step 3: 갤러리 화면 작성**

Create `app-preview/.../screen/component/YGListDatePreviewScreen.kt`:

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.yglistdate.YGListDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGListDatePreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PreviewSection("upload = true") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = true,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = true,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = true,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = true,
                            isEnabled = true,
                            isUploaded = true,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = false,
                            isUploaded = true,
                            onClick = {},
                        )
                    }
                }
            }
            item {
                PreviewSection("upload = false") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = false,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = true,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = false,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = true,
                            isEnabled = true,
                            isUploaded = false,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = false,
                            isUploaded = false,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGListDatePreviewScreen() = PreviewBox {
    YGListDatePreviewScreen(onBack = {})
}
```

- [ ] **Step 4: 카탈로그 등록**

`ComponentCatalog.kt` — import 블록에 알파벳 순으로
`import com.teamyg.parfait.preview.navigation.key.NavKeyYGListDate` 추가하고,
`componentCatalog` 리스트에서 `label = "YGDateButton"` 항목 **바로 뒤에** 아래를 넣는다:

```kotlin
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGListDate",
        navKey = NavKeyYGListDate,
    ),
```

- [ ] **Step 5: 엔트리 배선**

`ComponentEntryBuilders.kt` — import 2줄 추가
(`...navigation.key.NavKeyYGListDate`, `...screen.component.YGListDatePreviewScreen`),
그리고 `entry<NavKeyYGDateButton> { ... }` 블록 **바로 뒤에** 아래를 넣는다:

```kotlin
    entry<NavKeyYGListDate> {
        ScreenScaffold { modifier ->
            YGListDatePreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: ktlint 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew ktlintFormat` 후 재실행.

- [ ] **Step 8: 프리뷰 육안 확인**

Android Studio에서 `YGListDate.kt`의 `YGListDatePreview`를 연다.
Expected: 4개 셀이 나란히 보이고, 앞의 둘만 날짜 아래 빨간 점이 있으며 **네 셀의 높이가 모두 같다.**

---

## Task 2: `YGFloatingBar` 4변형 신설

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygfloatingbar/YGFloatingBar.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGFloatingBar.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGFloatingBarPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: 기존 `YGCircleButton(iconResource: Int, type: YGCircleButtonType, contentDescription: String?, onClick: () -> Unit, modifier: Modifier, interactionSource: MutableInteractionSource)`,
  기존 `YGEditTabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier, interactionSource: MutableInteractionSource)`
- Produces:
  - `YGFloatingBarBackClose(onBackClick: () -> Unit, onCloseClick: () -> Unit, modifier: Modifier = Modifier)`
  - `YGFloatingBarClose(onCloseClick: () -> Unit, modifier: Modifier = Modifier)`
  - `YGFloatingBarEdit(title: String, onCloseClick: () -> Unit, onConfirmClick: () -> Unit, modifier: Modifier = Modifier)`
  - `YGFloatingBarEditTab(tabs: List<String>, selectedIndex: Int, onTabSelect: (Int) -> Unit, onCloseClick: () -> Unit, onConfirmClick: () -> Unit, modifier: Modifier = Modifier)`

**배경:** Figma `Floating Bar` 4변형은 상단 16dp·좌우 20dp 패딩의 한 `Row`를 공유하고, 그 안에
`Button-Circle`(`Type=Default`)을 좌·우로 배치한다. 폭 375는 Figma 프레임 폭일 뿐이라 컴포넌트에
박지 않고 호출자가 `modifier`로 정한다.

`Close` 변형만 `Arrangement.End`인 이유: `SpaceBetween`에 자식이 하나면 좌측으로 붙어 Figma의
`justify-end`와 어긋난다.

`Edit`의 중앙 텍스트는 좌우 버튼 폭이 44dp로 같아 `SpaceBetween`에서 실질 중앙에 온다. Figma도 같은
구조라 중앙 정렬을 별도로 강제하지 않는다.

- [ ] **Step 1: `YGFloatingBar.kt` 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygfloatingbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygedittabbutton.YGEditTabButton
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Floating Bar / Status=Back-Close
 */
@Composable
fun YGFloatingBarBackClose(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(modifier = modifier) {
        YGCircleButton(
            iconResource = R.drawable.ic_caret_left,
            type = YGCircleButtonType.Default,
            contentDescription = "뒤로가기",
            onClick = onBackClick,
        )
        YGFloatingBarCloseButton(onClick = onCloseClick)
    }
}

/**
 * Figma Floating Bar / Status=Close
 */
@Composable
fun YGFloatingBarClose(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        YGFloatingBarCloseButton(onClick = onCloseClick)
    }
}

/**
 * Figma Floating Bar / Status=Edit
 */
@Composable
fun YGFloatingBarEdit(
    title: String,
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(modifier = modifier) {
        YGFloatingBarCloseButton(onClick = onCloseClick)
        Text(
            text = title,
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray800,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        YGFloatingBarConfirmButton(onClick = onConfirmClick)
    }
}

/**
 * Figma Floating Bar / Status=Edit-Tab
 */
@Composable
fun YGFloatingBarEditTab(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGFloatingBarContent(modifier = modifier) {
        YGFloatingBarCloseButton(onClick = onCloseClick)
        Row(verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, label ->
                YGEditTabButton(
                    text = label,
                    isSelected = index == selectedIndex,
                    onClick = { onTabSelect(index) },
                )
            }
        }
        YGFloatingBarConfirmButton(onClick = onConfirmClick)
    }
}

@Composable
private fun YGFloatingBarCloseButton(onClick: () -> Unit) {
    YGCircleButton(
        iconResource = R.drawable.ic_close,
        type = YGCircleButtonType.Default,
        contentDescription = "닫기",
        onClick = onClick,
    )
}

@Composable
private fun YGFloatingBarConfirmButton(onClick: () -> Unit) {
    YGCircleButton(
        iconResource = R.drawable.ic_check,
        type = YGCircleButtonType.Default,
        contentDescription = "확인",
        onClick = onClick,
    )
}

@Composable
private fun YGFloatingBarContent(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            start = YGTheme.layout.padding.padding7,
            top = YGTheme.layout.padding.padding6,
            end = YGTheme.layout.padding.padding7,
        ),
        content = content,
    )
}

@YGPreview
@Composable
private fun YGFloatingBarPreview() = PreviewBox {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White),
    ) {
        YGFloatingBarBackClose(
            onBackClick = {},
            onCloseClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarClose(
            onCloseClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarEdit(
            title = "토핑 편집",
            onCloseClick = {},
            onConfirmClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGFloatingBarEditTab(
            tabs = listOf("영역", "테두리"),
            selectedIndex = 0,
            onTabSelect = {},
            onCloseClick = {},
            onConfirmClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 2: 갤러리 NavKey 작성**

Create `app-preview/.../navigation/key/NavKeyYGFloatingBar.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGFloatingBar : NavKey
```

- [ ] **Step 3: 갤러리 화면 작성**

`Edit-Tab`은 상호작용이 있으므로 `remember`로 선택 상태를 들고 실제로 탭이 전환되는지 확인한다.

Create `app-preview/.../screen/component/YGFloatingBarPreviewScreen.kt`:

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackClose
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarClose
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEdit
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarEditTab
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGFloatingBarPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewSection("Back-Close") {
                    YGFloatingBarBackClose(
                        onBackClick = {},
                        onCloseClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("Close") {
                    YGFloatingBarClose(
                        onCloseClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("Edit") {
                    YGFloatingBarEdit(
                        title = "토핑 편집",
                        onCloseClick = {},
                        onConfirmClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("Edit-Tab") {
                    var selectedIndex by remember { mutableIntStateOf(0) }
                    YGFloatingBarEditTab(
                        tabs = listOf("영역", "테두리"),
                        selectedIndex = selectedIndex,
                        onTabSelect = { selectedIndex = it },
                        onCloseClick = {},
                        onConfirmClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGFloatingBarPreviewScreen() = PreviewBox {
    YGFloatingBarPreviewScreen(onBack = {})
}
```

- [ ] **Step 4: 카탈로그 등록**

`ComponentCatalog.kt` — import 블록에 알파벳 순으로
`import com.teamyg.parfait.preview.navigation.key.NavKeyYGFloatingBar` 추가하고,
`category = ComponentCategory.BAR`인 `label = "YGTopBar"` 항목 **바로 뒤에** 아래를 넣는다:

```kotlin
    ComponentEntry(
        category = ComponentCategory.BAR,
        label = "YGFloatingBar",
        navKey = NavKeyYGFloatingBar,
    ),
```

- [ ] **Step 5: 엔트리 배선**

`ComponentEntryBuilders.kt` — import 2줄 추가
(`...navigation.key.NavKeyYGFloatingBar`, `...screen.component.YGFloatingBarPreviewScreen`),
그리고 `entry<NavKeyYGTopBar> { ... }` 블록 **바로 뒤에** 아래를 넣는다:

```kotlin
    entry<NavKeyYGFloatingBar> {
        ScreenScaffold { modifier ->
            YGFloatingBarPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: ktlint 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew ktlintFormat` 후 재실행.

- [ ] **Step 8: 프리뷰 육안 확인**

Android Studio에서 `YGFloatingBar.kt`의 `YGFloatingBarPreview`를 연다.
Expected: 4행. 1행 좌 `<`·우 `×`, 2행 **우측에만** `×`, 3행 좌 `×`·중앙 "토핑 편집"·우 `✓`,
4행 좌 `×`·중앙 "영역"(밑줄 있음)/"테두리"(밑줄 없음)·우 `✓`.

---

## Task 3: `YGTopBar` — Canvas 변형 신설 + 프리뷰 칩 색 정정

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtopbar/YGTopBar.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTopBarPreviewScreen.kt`

**Interfaces:**
- Consumes: 기존 `YGIconButton`, `YGChipButton`, `YGChipButtonColorsDefaults.CherrySubtle`,
  기존 `YGNametagChip(colorChipType: YGColorChipType, userFirstName: String, chip: YGNametagChipStyle, modifier: Modifier)`,
  기존 `YGColorChipType.NametagChipPlus`
- Produces:
  - `YGTopBarCanvas(title: String, onBackClick: () -> Unit, onMenuClick: () -> Unit, modifier: Modifier = Modifier, memberContent: @Composable RowScope.() -> Unit = { })`

**배경:** private `YGTopBarContent`에 파라미터 2개(`contentPadding`·`trailingContent`)만 더해
Canvas를 수용한다. 기본값이 현재 동작과 같아 기존 3변형(`Back`·`Detail`·`Empty`) 호출부는 바뀌지 않는다.
`trailingContent`를 `weight(1f)` Row **바깥**의 형제로 두는 것이 Figma가 Info-Group을 flex-1로 두고
우측 아이콘을 형제로 두는 구조와 같다. `Empty`의 `rightContent`(#173 신설)는 그 Row **안쪽** 형제라
역할이 겹치지 않는다.

Canvas의 제목/멤버 우측 정렬은 안쪽 Row의 `Arrangement`를 바꾸지 않고 `titleContent` 안에서
`Spacer(Modifier.weight(1f))`로 만든다 — 안쪽 Row의 arrangement를 건드리면 나머지 4변형에 영향이 간다.

- [ ] **Step 1: `YGTopBarContent` 확장**

`YGTopBar.kt`의 `YGTopBarContent`를 아래로 교체한다.

```kotlin
@Composable
private fun YGTopBarContent(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = YGTheme.layout.padding.padding3,
        top = YGTheme.layout.padding.padding3,
        end = YGTheme.layout.padding.padding7,
        bottom = YGTheme.layout.padding.padding3,
    ),
    titleContent: @Composable RowScope.() -> Unit = { },
    trailingContent: @Composable () -> Unit = { },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(contentPadding),
    ) {
        YGIconButton(
            iconResource = iconResource,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = contentDescription,
            onClick = onIconClick,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            content = titleContent,
        )
        trailingContent()
    }
}
```

import 추가: `androidx.compose.foundation.layout.PaddingValues`.
`androidx.compose.foundation.layout.padding`은 이미 import돼 있다.

- [ ] **Step 2: `YGTopBarCanvas` 추가**

`YGTopBarEmpty` **뒤**, `YGTopBarContent` **앞**에 넣는다(공개 함수들이 모여 있는 구역).

```kotlin
@Composable
fun YGTopBarCanvas(
    title: String,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    memberContent: @Composable RowScope.() -> Unit = { },
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_caret_left,
        contentDescription = "뒤로가기",
        onIconClick = onBackClick,
        modifier = modifier,
        contentPadding = PaddingValues(YGTheme.layout.padding.padding3),
        titleContent = {
            Text(
                text = title,
                style = YGTheme.typography.body.b01R,
                color = YGAtomicColors.Gray.Gray800,
            )
            Spacer(modifier = Modifier.weight(1f))
            memberContent()
        },
        trailingContent = {
            YGIconButton(
                iconResource = R.drawable.ic_hamburger,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = "메뉴",
                onClick = onMenuClick,
            )
        },
    )
}
```

import 추가: `androidx.compose.foundation.layout.Spacer`.

- [ ] **Step 3: 프리뷰 칩 색 정정**

`YGTopBar.kt` 맨 아래 `YGTopBarPreview`의 칩 슬롯 예시에서 `colors`만 바꾼다. 컴포넌트 API는 손대지 않는다.

```kotlin
        YGTopBarEmpty(
            onIconClick = {},
            rightContent = {
                YGChipButton(
                    text = "그룹 추가하기",
                    colors = YGChipButtonColorsDefaults.CherrySubtle, // was: CherrySolid
                    onClick = {},
                    startIconResource = R.drawable.ic_plus,
                )
            },
        )
```

Figma 정본이 `CherrySubtle`(`Cherry50` 배경 / `Gray600` 전경)이고 실제 호출부(G-001 `GroupListScreen`)도
이미 `CherrySubtle`이라, 프리뷰만 어긋나 있다.

- [ ] **Step 4: 컴포넌트 프리뷰에 Canvas 추가**

`YGTopBar.kt` 맨 아래 `YGTopBarPreview`의 `Column` 안, 칩 슬롯 예시(Step 3) 뒤에 추가한다.

```kotlin
        YGTopBarCanvas(
            title = "그룹이름",
            onBackClick = { },
            onMenuClick = { },
            modifier = Modifier.fillMaxWidth(),
            memberContent = {
                YGNametagChip(
                    colorChipType = YGColorChipType.NametagChip5,
                    userFirstName = "김",
                    chip = YGNametagChipStyle.Style28,
                )
            },
        )
```

import 추가:
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType`,
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip`,
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle`.

- [ ] **Step 5: 갤러리 화면에 Canvas 섹션 + List-Member 조립 예시 추가**

`YGTopBarPreviewScreen.kt`의 `LazyColumn` 안, `"YGTopBarDefault"` 라벨 item(내용은 #173 이후
`YGTopBarEmpty` + 칩 슬롯) 뒤에 아래 item을 추가하고,
파일 하단(`PreviewYGTopBarPreviewScreen` 앞)에 `MemberListSample`을 정의한다.

겹침은 `Arrangement.spacedBy((-12).dp)`로 만든다 — Figma의 `mr-[-12px]`와 같고, 나중에 오는 칩이
위에 그려지는 순서까지 Figma와 일치한다. 이 조립 코드가 **호출자 책임의 참조 구현**이다.

```kotlin
            item {
                PreviewSection("YGTopBarCanvas") {
                    YGTopBarCanvas(
                        title = "그룹이름",
                        onBackClick = {},
                        onMenuClick = {},
                        memberContent = { MemberListSample() },
                    )
                }
            }
```

```kotlin
@Composable
private fun MemberListSample() {
    val members = listOf(
        YGColorChipType.NametagChip5 to "김",
        YGColorChipType.NametagChip4 to "이",
        YGColorChipType.NametagChip1 to "박",
        YGColorChipType.NametagChip2 to "최",
        YGColorChipType.NametagChip12 to "정",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        members.forEach { (type, name) ->
            YGNametagChip(
                colorChipType = type,
                userFirstName = name,
                chip = YGNametagChipStyle.Style28,
            )
        }
        YGNametagChip(
            colorChipType = YGColorChipType.NametagChipPlus,
            userFirstName = "+7",
            chip = YGNametagChipStyle.Style28,
        )
    }
}
```

import 추가:
`androidx.compose.foundation.layout.Row`,
`androidx.compose.ui.Alignment`,
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType`,
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip`,
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle`,
`com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarCanvas`.

- [ ] **Step 6: 기존 호출부 회귀 확인**

Run: `rg -n "YGTopBar(Empty|Back|Detail)\(" --glob '!**/build/**'`
Expected: 세 변형 호출부(`app-preview` 프리뷰 화면들, G-001 `GroupListScreen`)가 기존 파라미터만
넘기고 있어 `YGTopBarContent` 확장이 컴파일에 영향을 주지 않음을 눈으로 확인.
`YGTopBarDefault` 호출부는 **0건이어야 한다**(#173에서 삭제됨).

- [ ] **Step 7: 빌드 확인**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL. `:app`까지 도는 이유는 `YGTopBar*`가 feature 모듈에서 실제로 쓰이고
있어 시그니처 변경 회귀를 여기서 잡아야 하기 때문이다.

- [ ] **Step 8: ktlint 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew ktlintFormat` 후 재실행.

- [ ] **Step 9: 프리뷰 육안 확인**

Android Studio에서 `YGTopBar.kt`의 `YGTopBarPreview`를 연다.
Expected: 5행(`Back`·`Detail`·`Empty`·`Empty`+칩·`Canvas`). 칩 행이 **연분홍 배경 + 회색 글씨
"그룹 추가하기"**로 바뀌어 있고, 마지막 `Canvas` 행은 좌 `<`·"그룹이름"·우측에 네임태그 1개와
햄버거 아이콘이 보인다.

---

## Task 4: 통합 검증 + 문서 갱신

**Files:**
- Modify: `parfait/specs/2026-08-01-designsystem-bar-listdate-components.md` (이 repo)
- Modify: `parfait/specs/README.md` (이 repo)
- Modify: `parfait/plans/2026-08-01-designsystem-bar-listdate-components.md` (이 repo)
- Modify: `parfait/plans/README.md` (이 repo)

**Interfaces:**
- Consumes: Task 1~3의 산출물 전부
- Produces: 없음(검증·기록 Task)

- [ ] **Step 1: repo 전체 빌드**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: repo 전체 ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 갤러리 설치**

Run: `./gradlew :app-preview:installDebug`
Expected: 실기기(Galaxy A35)에 설치 성공

- [ ] **Step 4: 실기기 육안 대조 — YGListDate**

갤러리 → `Button` → `YGListDate`.
Expected: `upload = true` 4셀 전부 날짜 아래 빨간 점, `upload = false` 4셀은 점이 없으나
**두 섹션의 셀 높이가 동일**. 각 섹션 안에서 default/selected/today/disabled가 Figma
`Button-Date` 4상태와 일치.

- [ ] **Step 5: 실기기 육안 대조 — YGFloatingBar**

갤러리 → `Bar` → `YGFloatingBar`.
Expected: `Close` 행의 버튼이 **우측 끝**에 붙어 있고(좌측 아님), `Edit` 행의 텍스트가 중앙,
`Edit-Tab` 행에서 "테두리"를 탭하면 밑줄이 그쪽으로 옮겨간다.

- [ ] **Step 6: 실기기 육안 대조 — YGTopBar**

갤러리 → `Bar` → `YGTopBar`.
Expected: 칩 슬롯 섹션의 칩이 연분홍 배경 + 회색 "그룹 추가하기". `YGTopBarCanvas` 섹션에서
네임태그 6개가 12dp씩 겹쳐 있고 마지막이 `+7` 흰 칩이며, 우측 끝에 햄버거 아이콘이 있다.
Back/Detail/Empty 3섹션은 이전과 동일.

- [ ] **Step 7: 결함 발견 시 처리**

육안에서 어긋난 것이 있으면 **원인이 컴포넌트인지 갤러리 화면인지 먼저 가른다.** 선행 라운드에서
결함 4건 중 3건이 갤러리 화면 한정이었다. 컴포넌트 결함이면 Task 1~3 해당 파일을 고치고 Step 1~6을
다시 돈다. 갤러리 결함이면 `app-preview`만 고친다. 어느 쪽이든 **무엇이 왜 틀렸는지**를 Step 8의
기록 대상에 넣는다.

- [ ] **Step 8: parfait 문서 갱신 (이 repo)**

- 스펙 파일 머리말에 "구현 상태(2026-08-01)" 블록 추가 — 완료 범위, 육안 검증 결과, 발견·수정한
  결함, 미검증으로 남은 것(pressed 상태 등), TJYG-Android 미커밋 사실과 브랜치명
- 스펙 `status`를 `draft` → `in-progress`로 변경
- 이 계획서 `status`를 `todo` → `done`으로 변경하고 머리말에 실행 결과 블록 추가
- `parfait/specs/README.md`·`parfait/plans/README.md`의 해당 행에 실행 결과 요약 반영
- **이 repo는 브랜치 → 커밋까지만.** push·PR은 작업자 확인 후

- [ ] **Step 9: 최종 보고**

TJYG-Android 작업 트리 변경 목록(`git status --short`)과 미커밋 사실, 육안 검증 결과, 남은
열린 질문을 작업자에게 보고한다.

---

# 2차 라운드 (2026-08-01 Figma 재조회)

> Task 1~4를 끝낸 뒤 Figma를 다시 확인하니 `Top Bar`의 `Default`·`Empty` 두 변형과 공유 컴포넌트
> `Button-Chip-Left`가 바뀌어 있었다. `Back`·`Detail`·`Canvas`는 무변경이라 1차 산출물은 그대로 살아
> 있다. **Task 3 Step 3에서 한 "프리뷰 칩을 `CherrySubtle`로 정정"은 이 재조회로 무효**가 됐다 —
> Task 5가 그 자리를 덮어쓴다.
>
> 배경 블러 관용은 [ADR-0018](../../../adr/0018-backdrop-blur-haze.md)이 정한다.

> **2차 라운드 실행 결과(2026-08-01)** — Task 5~7 전량 완료. Task 5·6 모두 **리뷰 1회에 클린 통과**해
> fix round가 없었다. TJYG-Android는 여전히 미커밋, 브랜치 `feature/sync-component`.
>
> **블러 방식이 착수 직전에 뒤집혔다.** 계획 초안은 haze 도입이었는데, 작업자가 `BlurEffect`로 안
> 되느냐고 물어 재검토한 결과 (a) `RenderEffect` 기반이라 **haze도 API 하한 31로 동일**하고,
> (b) C-101이 이미 같은 `GraphicsLayer` 관용으로 확정돼 있어 라이브러리를 넣으면 **블러 구현이
> 이원화**된다는 것이 드러났다. 자체 구현으로 뒤집고 ADR-0018을 다시 썼다.
> 교훈: 라이브러리를 후보로 올리기 전에 **같은 문제의 선행 결정이 repo에 있는지 먼저 확인**해야 한다.
> 이번엔 작업자의 질문이 그 확인을 대신했다.
>
> **계획서 결함 1건** — Task 6 Step 1의 import 목록에 `androidx.compose.ui.graphics.layer.drawLayer`가
> 빠져 있었다. `GraphicsLayer`와 별개의 top-level 확장 함수라 명시 import가 필요한데, 코드 본문은
> 그 함수를 두 번 호출한다. 구현자가 발견해 한 줄 보충했고 리뷰가 확인했다.
>
> **검증은 컨트롤러가 직접 수행했다** — 구현자는 IDE를 열 수 없어 육안 검증을 못 했고 리뷰가 그것을
> 유일한 Important로 지적했는데, 컨트롤러가 실기기(SM-A356N, **API 36**)에서 이미 마친 상태였다.
> 칩 색·날짜 표시·블러 동작·좌표 정합을 스크린샷으로 대조했고, **스크롤 후에도 정합이 유지**되는 것을
> 확인해 ADR-0018이 경고한 회귀가 없음을 확정했다.
>
> **미검증**: pressed 상태. API 31 미만 폴백 경로(검증 기기가 API 36이라 실행되지 않음).
> 실제 화면(G-001)에서의 블러 — 배경 record 배선이 범위 밖이라 갤러리 데모로만 확인.

---

## Task 5: `Button-Chip-Left` 프리셋 교체 + 개명

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygchipbutton/YGChipButtonColorsDefaults.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygchipbutton/YGChipButtonPreviewData.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtopbar/YGTopBar.kt`
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListScreen.kt`
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListAddGroupScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTopBarPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGChipButtonPreviewScreen.kt`

**Interfaces:**
- Consumes: 기존 `YGChipButtonColors(defaultForegroundColor, pressedForegroundColor, defaultBackgroundColor, pressedBackgroundColor, defaultBorderColor, pressedBorderColor)`
- Produces: `YGChipButtonColorsDefaults.GrayOutline` — `CherrySubtle`을 **대체**한다. `CherrySubtle`이라는
  이름은 이 Task 이후 존재하지 않는다. `CherrySolid`는 무변경.

**배경:** Figma `Button-Chip-Left`가 Cherry 계열을 버리고 흰 배경 + 회색 테두리로 바뀌었다. 프리셋 하나를
고치면 소비처 6곳이 전부 따라온다. 이름에 Cherry가 남으면 내용과 어긋나므로 함께 개명한다
(선행 라운드의 `CherryBorderPressed`→`CherrySubtle` 개명과 같은 이유).

- [ ] **Step 1: 프리셋 교체**

`YGChipButtonColorsDefaults.kt`의 `CherrySubtle` 블록을 아래로 **교체**한다(`CherrySolid`는 건드리지 않는다).

```kotlin
    /**
     * Figma Button-Chip-Left
     */
    val GrayOutline: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray900,
        pressedForegroundColor = YGAtomicColors.Gray.Gray950,
        defaultBackgroundColor = YGAtomicColors.Gray.White,
        pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
        defaultBorderColor = YGAtomicColors.Gray.Gray500,
        pressedBorderColor = YGAtomicColors.Gray.Gray500,
    )
```

- [ ] **Step 2: 소비처 6곳 참조 갱신**

`CherrySubtle` → `GrayOutline`으로 바꾼다. **참조 심볼만** 바꾸고 다른 인자는 손대지 않는다.

Run: `rg -n "CherrySubtle" --glob '!**/build/**'`
Expected: 위 Files 목록의 6곳(정의 1 + 참조 5)이 나온다. 전부 교체한 뒤 다시 돌려 0건을 확인한다.

`YGChipButtonPreviewScreen.kt`는 `PreviewSection("CherrySubtle")` **라벨 문자열**도 `"GrayOutline"`으로
바꾼다. 프리셋 이름을 보여주는 라벨이므로 함께 가야 한다.

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. feature 모듈까지 참조가 걸려 있으므로 repo 전체로 돌린다.

- [ ] **Step 4: ktlint 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew ktlintFormat` 후 재실행.

- [ ] **Step 5: 잔여 참조 확인**

Run: `rg -n "CherrySubtle" --glob '!**/build/**'`
Expected: **0건.**

---

## Task 6: `YGTopBarEmpty` — 날짜 + 반투명 배경 + 배경 블러

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtopbar/YGTopBar.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTopBarPreviewScreen.kt`

**Interfaces:**
- Consumes: Task 5의 `YGChipButtonColorsDefaults.GrayOutline`, 기존 `YGTopBarContent(iconResource, contentDescription, onIconClick, modifier, contentPadding, titleContent, trailingContent)`
- Produces:
  - `YGTopBarEmpty(date: String, day: String, onIconClick: () -> Unit, modifier: Modifier = Modifier, backdropLayer: GraphicsLayer? = null, rightContent: @Composable () -> Unit = {})`
    — `date`·`day`가 **앞에 필수로 붙으므로 기존 호출부가 깨진다.** 아래 Step 4에서 함께 고친다.

**배경:** Figma `Default`·`Empty`가 로고 자리에 날짜를 넣고 컨테이너에 `White75` 배경 + **배경 블러 4**를
붙였다. (MCP가 내주는 `backdrop-blur-[2px]`는 CSS 환산값이다 — Compose에는 Figma 저작값 4를 쓴다.) 블러 관용은 ADR-0018 — 레이어는 호출 화면이 소유하고 컴포넌트는 받아서 자기 영역만
흐린다. `backdropLayer`가 `null`이면 틴트만 그린다.

**API 31 미만에서는 블러가 없다.** `RenderEffect`가 31+라 26~30에서는 틴트만 남는다.
틴트는 블러와 **독립적으로 항상** 그린다.

- [ ] **Step 1: 배경 그리기 모디파이어 추가**

`YGTopBar.kt` 안에 private 확장으로 둔다. 소비처가 이 파일뿐이라 공용으로 빼지 않는다(ADR-0018).

```kotlin
@Composable
private fun Modifier.ygTopBarBackdrop(backdropLayer: GraphicsLayer?): Modifier {
    val blurLayer = rememberGraphicsLayer()
    val isBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return this
        .onGloballyPositioned { blurOriginInRoot = it.positionInRoot() }
        .drawBehind {
            if (backdropLayer != null && isBlurSupported) {
                blurLayer.renderEffect = BlurEffect(
                    radiusX = BlurRadius.toPx(),
                    radiusY = BlurRadius.toPx(),
                    edgeTreatment = TileMode.Clamp,
                )
                blurLayer.record(size = size.toIntSize()) {
                    translate(left = -blurOriginInRoot.x, top = -blurOriginInRoot.y) {
                        drawLayer(backdropLayer)
                    }
                }
                drawLayer(blurLayer)
            }
            drawRect(color = YGAtomicColors.Transparency.White75)
        }
}

private val BlurRadius = 4.dp
```

`blurOriginInRoot`는 `remember { mutableStateOf(Offset.Zero) }`로 이 모디파이어 안에서 들고, draw
단계에서만 읽는다. 위치를 모르면 배경 레이어의 엉뚱한 부분을 흐리게 된다.

> 원본 `backdropLayer`에 `renderEffect`를 **직접 걸지 마라.** 같은 레이어가 화면 배경으로도 그려지므로
> 블러가 화면 전체로 번진다. 반드시 별도 레이어에 복사한 뒤 건다(ADR-0018).

import 추가: `android.os.Build`, `androidx.compose.ui.draw.drawBehind`,
`androidx.compose.ui.graphics.BlurEffect`, `androidx.compose.ui.graphics.TileMode`,
`androidx.compose.ui.graphics.layer.GraphicsLayer`, `androidx.compose.ui.graphics.rememberGraphicsLayer`,
`androidx.compose.ui.graphics.drawscope.translate`, `androidx.compose.ui.layout.onGloballyPositioned`,
`androidx.compose.ui.layout.positionInRoot`, `androidx.compose.ui.unit.toIntSize`,
`androidx.compose.ui.geometry.Offset`, `androidx.compose.runtime.mutableStateOf`,
`androidx.compose.runtime.remember`, `androidx.compose.runtime.getValue`,
`androidx.compose.runtime.setValue`, `androidx.compose.ui.unit.dp`.

- [ ] **Step 2: `YGTopBarEmpty` 재작성**

```kotlin
@Composable
fun YGTopBarEmpty(
    date: String,
    day: String,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdropLayer: GraphicsLayer? = null,
    rightContent: @Composable () -> Unit = {},
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_hamburger,
        contentDescription = "메뉴",
        onIconClick = onIconClick,
        modifier = modifier.ygTopBarBackdrop(backdropLayer = backdropLayer),
        titleContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = date,
                    style = YGTheme.typography.body.b01R,
                    color = YGAtomicColors.Gray.Gray800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "($day)",
                    style = YGTheme.typography.body.b01R,
                    color = YGAtomicColors.Gray.Gray300,
                    maxLines = 1,
                )
            }
            rightContent()
        },
    )
}
```

- 기존 로고 `Image(painterResource(R.drawable.ic_plus))`와 그 `// todo : parfait logo 로 변경 예정`
  주석을 **삭제**한다. Figma가 그 자리를 날짜로 확정했으므로 todo가 닫힌다.
- `Row`에 `weight(1f)`를 줘 날짜가 길어져도 `rightContent`를 밀어내지 않게 한다
  (1차 라운드에서 같은 종류의 결함을 두 번 겪었다).
- import 추가: `androidx.compose.ui.text.style.TextOverflow`,
  `androidx.compose.foundation.layout.Arrangement`.
  `Image`·`painterResource`가 이 파일에서 더 쓰이지 않으면 import를 지운다 — 먼저 확인할 것.

- [ ] **Step 3: 컴포넌트 프리뷰 갱신**

`YGTopBarPreview`의 `YGTopBarEmpty` 호출 2곳에 `date = "December 31"`, `day = "Wed"`를 넣는다.
칩을 쓰는 쪽은 Task 5에서 이미 `GrayOutline`으로 바뀌어 있다.

긴 날짜 회귀 케이스를 하나 더 둔다:

```kotlin
        YGTopBarEmpty(
            date = "December 31, 2026 (아주 긴 날짜 문자열)",
            day = "Wed",
            onIconClick = {},
            modifier = Modifier.fillMaxWidth(),
            rightContent = {
                YGChipButton(
                    text = "그룹 추가하기",
                    colors = YGChipButtonColorsDefaults.GrayOutline,
                    onClick = {},
                    startIconResource = R.drawable.ic_plus,
                )
            },
        )
```

- [ ] **Step 4: 기존 호출부 수리**

Run: `rg -n "YGTopBarEmpty\(" --glob '!**/build/**'`
Expected: `GroupListScreen.kt`, `YGTopBarPreviewScreen.kt`, `YGTopBar.kt` 프리뷰가 나온다.
`date`·`day`가 필수 파라미터로 늘었으므로 **전부 인자를 추가**해야 컴파일된다.

- `GroupListScreen.kt` — 화면이 아직 날짜 데이터를 갖고 있지 않다. **하드코딩 placeholder를 넣지 말고**
  `date = ""`, `day = ""`처럼 빈 문자열로도 두지 마라. 대신 이 Task에서는
  `date = "December 31"`, `day = "Wed"` 샘플을 넣고 **`// TODO: 실제 날짜 데이터 결선` 주석을 단다.**
  날짜 포맷·로케일 규칙이 미정이라(스펙 열린 질문 5) 여기서 정할 수 없다.
  > ⚠️ **계획서 결함(2026-08-01 코드리뷰) — 이 지시의 전제가 틀렸다.** 작성 시점에 이미
  > `GroupListViewModel`이 `init`에서 `dateString`·`dayOfWeekString`을 계산했고 같은 화면의 `YGDate`가
  > 그 값을 쓰고 있었다. 지시대로 넣은 결과 상단바만 "December 31 (Wed)" 고정 노출이 됐다.
  > **현행은 `date = uiState.dateString`, `day = uiState.dayOfWeekString`.** 미정으로 남는 건 포맷·로케일
  > 규칙뿐이다. 교훈: 호출부를 고치는 Step은 **그 호출부의 상태 보유 여부를 계획 단계에서 실제로 읽고**
  > 쓴다.
- `backdropLayer`는 이 Task에서 **아무 호출부도 넘기지 않는다.** G-001 화면의 배경 record 배선은
  범위 밖이다(스펙 열린 질문 6). 기본값 `null`이라 컴파일된다.

- [ ] **Step 5: 갤러리 화면 갱신**

`YGTopBarPreviewScreen.kt`의 `YGTopBarEmpty` 호출 2곳에 `date`·`day`를 넣는다.
`PreviewSection("YGTopBarEmpty")`와 `PreviewSection("YGTopBarDefault")` 라벨은 Figma 변형명이므로 유지.

블러를 갤러리에서 실제로 보려면 배경이 필요하다. `PreviewSection("YGTopBarDefault")` 아래에
**블러 확인용 섹션**을 하나 추가한다:

```kotlin
            item {
                PreviewSection("YGTopBarEmpty + backdrop blur") {
                    val backdrop = rememberGraphicsLayer()
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .drawWithContent {
                                    backdrop.record { this@drawWithContent.drawContent() }
                                    drawLayer(backdrop)
                                },
                        ) {
                            repeat(6) { index ->
                                Text(
                                    text = "배경 콘텐츠 줄 $index — 블러가 걸리면 흐려진다",
                                    style = YGTheme.typography.body.b02R,
                                    color = YGAtomicColors.Gray.Gray800,
                                )
                            }
                        }
                        YGTopBarEmpty(
                            date = "December 31",
                            day = "Wed",
                            onIconClick = {},
                            backdropLayer = backdrop,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
```

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. `YGTopBarEmpty` 시그니처가 바뀌어 feature 모듈이 영향을 받으므로 repo
전체로 돌린다.

- [ ] **Step 7: ktlint 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew ktlintFormat` 후 재실행.

---

## Task 7: 2차 라운드 검증 + 문서 갱신

**Files:**
- Modify: `parfait/specs/2026-08-01-designsystem-bar-listdate-components.md` (이 repo)
- Modify: `parfait/plans/2026-08-01-designsystem-bar-listdate-components.md` (이 repo)
- Modify: `parfait/adr/0018-backdrop-blur-haze.md` (이 repo)
- Modify: `parfait/specs/README.md`·`parfait/plans/README.md`·`parfait/adr/README.md` (이 repo)

- [ ] **Step 1: 갤러리 설치**

Run: `./gradlew :app-preview:installDebug`
Expected: 실기기 설치 성공.

- [ ] **Step 2: 실기기 육안 — 칩 프리셋**

갤러리 → `Button` → `YGChipButton`.
Expected: `GrayOutline` 섹션의 칩이 **흰 배경 + 회색 테두리 + 진한 글씨**. `CherrySolid` 섹션은 이전과
동일한 연분홍.

- [ ] **Step 3: 실기기 육안 — Top Bar 날짜**

갤러리 → `Bar` → `YGTopBar`.
Expected: `YGTopBarEmpty`·`YGTopBarDefault` 섹션에 **"December 31 (Wed)"** — 날짜는 진한 회색,
요일은 옅은 회색. 로고 자리의 `+` 아이콘이 사라졌다. `Default` 섹션 칩은 흰 배경 + 회색 테두리.
긴 날짜 케이스에서 칩이 밀려나지 않고 날짜가 말줄임된다.

- [ ] **Step 4: 실기기 육안 — 배경 블러**

같은 화면의 `YGTopBarEmpty + backdrop blur` 섹션.
Expected(API 31+ 기기): 바 뒤의 텍스트가 **흐리게** 비친다. API 30 이하 기기라면 흐림 없이 흰 반투명만.
**기기 API 레벨을 먼저 확인하고 기대치를 정한다** — `adb shell getprop ro.build.version.sdk`.

블러가 엉뚱한 위치를 흐리는지도 본다. 좌표 정합이 틀리면 바가 아니라 화면 위쪽이 흐려진다.

- [ ] **Step 5: 결함 발견 시 처리**

원인이 컴포넌트인지 갤러리 화면인지 먼저 가른다. 좌표 정합 결함은 **정지 화면에서 안 보일 수 있으니**
스크롤 가능한 배경으로도 확인한다.

- [ ] **Step 6: 문서 갱신 (이 repo)**

- ADR-0018 `status`를 `proposed` → `accepted`로 (실기기에서 관용이 동작함을 확인한 뒤)
- 스펙 구현 상태 블록에 2차 라운드 결과 추가
- 이 계획서 2차 라운드 실행 결과 기록
- specs/plans/adr README 3곳 반영
- **이 repo는 브랜치 → 커밋까지만.** push·PR은 작업자 확인 후

- [ ] **Step 7: 최종 보고**

TJYG-Android 작업 트리 변경 목록과 미커밋 사실, 실기기 검증 결과(특히 블러가 동작한 API 레벨),
남은 열린 질문을 작업자에게 보고한다.

---

## 열린 질문 (스펙에서 이월)

1. **`+N` 카운트 칩 타입** — `YGColorChipType`이 13종 + `NametagChipPlus`라 정책 12종과 어긋난
   상태가 이어진다. 갤러리 예시는 `NametagChipPlus`로 그리되 정리는 Nametag 라운드로 넘긴다.
2. **`YGFloatingBarEdit`의 중앙 문구 출처** — Figma가 `Text` placeholder만 두어 실제 문구를 알 수
   없다. 프리뷰·갤러리에서는 `"토핑 편집"`을 샘플로 쓰고, 실제 값은 호출 화면 구현 때 확정한다.
3. **Floating Bar의 배치 책임** — Figma가 상단 패딩 16dp만 주고 화면 어디에 떠 있는지는 컴포넌트
   밖 정보다. 호출 화면이 정한다.
4. **API 31 미만의 배경 블러 부재** — `minSdk` 26이라 26~30에서는 틴트만 남는다. 플랫폼 제약이므로
   해결이 아니라 **수용 여부**의 문제다. 디자인 확인 필요.
5. **날짜 포맷·로케일** — Figma가 `December 31 (Wed)` 영문 표기인데 앱은 한국어 UI다. 컴포넌트는
   완성된 문자열 2개를 받기만 하고, 포맷 책임은 호출 화면/도메인이다. 규칙 미정이라 Task 6은
   `GroupListScreen`에 샘플 + TODO만 남긴다.
6. **블러 대상 화면 배선** — G-001이 배경을 record하도록 배선하는 것은 범위 밖이다. 그 전까지
   실사용 화면에서는 블러가 꺼진 상태(틴트만)로 동작한다.
