---
id: designsystem-canvas-components
title: 디자인시스템 캔버스 영역 컴포넌트 신설 구현 계획
status: done
type: work-order
created: 2026-07-31
updated: 2026-08-01
platforms: android
owner: TJYG-Android 디자인시스템
related_adr:
  - ADR-0010
related_spec:
  - designsystem-canvas-components
related_code:
  - CanvasCutCornerShape.kt#canvasCutCornerShape
  - YGStrokeButton.kt#YGStrokeButton
  - YGMenuItem.kt#YGMenuItem
  - YGCanvasMenu.kt#YGCanvasMenu
  - YGCanvasMenuAction.kt#YGCanvasMenuAction
  - YGCanvasDateSelectButton.kt#YGCanvasDateSelectButton
  - YGCanvas.kt#YGCanvas
  - YGCanvasBackground.kt#YGCanvasBackground
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
archived_reason: PR #185 develop 머지 완료(2026-08-01) — 코드=설계 일치, 스펙 implemented 전환
tags: [plan, parfait, designsystem, figma-sync, canvas]
---

# 디자인시스템 캔버스 영역 컴포넌트 신설 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Figma "캔버스" 영역 5종(`Canvas`·`Canvas-Menu`·`Menu-Item`·`Button-Stroke`·`Canvas/Button-Date-Select`)을 `core:designsystem`에 신설하고 `:app-preview` 갤러리에 등록한다.

**Architecture:** 아래에서 위로 쌓는다 — 공용 컷 `Shape` → 잎 컴포넌트 2종(`YGStrokeButton`·`YGMenuItem`) → 합성 컴포넌트 2종(`YGCanvasMenu`·`YGCanvasDateSelectButton`) → 최상위 컨테이너 `YGCanvas` → 갤러리 등록. 각 단계는 앞 단계 산출물만 소비하므로 순서를 바꿀 수 없다.

**Tech Stack:** Kotlin, Jetpack Compose, `core:designsystem` 자체 테마(`YGTheme`·`YGAtomicColors`·`SizeTokens`), Coil 3(`AsyncImage`), Navigation3 + Hilt(`:app-preview`).

**설계 스펙:** [2026-07-31-designsystem-canvas-components](../../specs/archive/2026-07-31-designsystem-canvas-components.md) — 값·상태 표의 단일 출처. 이 계획과 어긋나면 스펙이 정답.

> **실행 결과(2026-07-31, subagent-driven)** — Task 1~8 전량 수행. 태스크마다 구현 → 스펙·품질 리뷰를
> 돌렸고, 마지막에 14파일 통합 리뷰를 한 번 더 돌렸다. 빌드·repo 전체 ktlint 통과, 실기기
> 갤러리에서 5종 + `YGCanvas` 5상태 육안 대조 완료. **TJYG-Android 미커밋**(작업자 지시).
>
> **계획 코드에서 고친 것 2건**
> - **Task 6 — Expanded 총높이 붕괴**: 계획의 `if (isMenuRaised.not()) { YGCanvasMenu(...) }`가 승격 시
>   하단 행을 아예 안 그려 컨테이너가 44dp 짧아졌다(Figma는 모든 상태가 같은 높이, 확장 메뉴는
>   캔버스 하단을 덮고 바닥은 그대로). `Spacer(SizeTokens.Size44)`로 행을 예약해 복구.
> - **Task 6 — Dim이 터치를 안 막음**(통합 리뷰 Critical): 흐려진 메뉴·날짜바·토핑이 그대로 눌렸다.
>   Dim `Box`에 소비 전용 `pointerInput`을 추가. `onDimClick`은 추가하지 않았다.
>
> **계획에 없던 보완 1건** — Task 7의 `Image` 배경 showcase가 렌더되지 않아
> `:app-preview` 매니페스트에 `INTERNET` 권한을 추가했다. 다만 진짜 원인은 따로 있었다 —
> Coil 3는 네트워크 페처를 별도 아티팩트(`coil-network-okhttp`)로 분리하는데 이 프로젝트는
> `coil-compose`만 물려 있어 원격 URL이 로드되지 않는다. 의존 추가는 `build-logic` 전역 변경이라
> 다음 라운드로 미뤘다(스펙 open-questions).
> **✅ [2026-07-31] 해소** — 후속 Grouptag·Topping 라운드가 `coil-network-okhttp`를 추가하고
> 실기기에서 원격 URL 로딩을 확인했다. `YGCanvasBackground.Image` 화면 자체의 렌더 검증은 별개로 남는다.
>
> **실행 후 API 변경(작업자 요청)** — 상태 조건을 "값의 유무"에서 불리언 플래그로 통일했다.
> `YGCanvasMenu(… isExpanded: Boolean = false, expandedItems)`,
> `YGCanvas(… isMenuExpanded, isEmpty, isCalendarVisible: Boolean = false, expandedItems,
> emptyMessage: String = "", calendarContent: @Composable () -> Unit = {})`.
> `YGCanvas`의 조건이 전부 플래그로 바뀌었다 — 메뉴 승격 `expandedItems.isNotEmpty()` → `isMenuExpanded`,
> 안내문 `emptyMessage != null` → `isEmpty`, 캘린더 승격 `calendarContent != null` → `isCalendarVisible`.
> `emptyMessage`·`calendarContent`는 논널이 됐다. 갤러리 두 화면의 호출부도 따라 고쳤다.
> **아래 Task 4·6·7의 코드 블록은 계획 당시 시그니처 그대로다** — 현행 API는
> [스펙](../../specs/archive/2026-07-31-designsystem-canvas-components.md)이 정본이다.
>
> **미검증 2건** — pressed 상태(자동 입력이 `interactionSource`에 안 잡힘, 선행 라운드와 같은 한계),
> `YGCanvasBackground.Image`의 원격 이미지 실렌더(위 Coil 네트워크 페처 부재로 로드 자체가 안 됨).
>
> **통합 리뷰에서 결함 아님으로 판정** — 컴포넌트 접합부 2dp 테두리(Figma도 인접 인스턴스마다 1px
> stroke), `isSelected`가 `YGCanvasMenu`까지 안 이어짐(Figma Expanded 하단 버튼이 `Base/White`라
> 강조 없음이 원본).

## Global Constraints

- **TJYG-Android에 `git commit`·`git push`를 실행하지 않는다.** 작업 트리 변경으로만 남긴다(작업자 지시). 각 태스크의 마지막 단계는 커밋이 아니라 빌드·린트 통과 확인이다.
- 작업 브랜치는 이미 `feature/sync-canvas-component`다. 새 브랜치를 만들지 않는다.
- 테스트를 쓰지 않는다 — `core:designsystem`·`:app-preview`에 테스트 소스셋이 없다. 검증은 `assembleDebug` + `ktlintCheck` + 프리뷰/실기기 육안 대조다.
- 색은 `YGAtomicColors.*`를 직접 읽는다(모듈 as-built 관용구). 시맨틱(`YGTheme.colorScheme`)으로 우회하지 않는다.
- 크기는 `SizeTokens.SizeN.getDp()`, 그 외 토큰은 `YGTheme.layout.*` / `YGTheme.typography.*` / `YGTheme.shapes.*`로 읽는다. **`SizeTokens`에 값을 추가하지 않는다**(20·44 모두 존재).
- 클릭은 표준 `clickable(indication = null)` + `semantics { role = Role.Button }`, 선택형은 `selectable(indication = null)`. 이 모듈은 `clickableYG`를 쓰지 않는다.
- 프리뷰는 `@YGPreview` + `PreviewBox`, 프리뷰 함수는 `private`.
- 컴포넌트 파일 상단 KDoc에 대응 Figma 컴포넌트명을 적는다(`/** Figma Button-Stroke */` 형식).
- 새 `ComponentCategory`를 만들지 않는다. `BUTTON`·`CONTAINER`에 넣는다.

**모듈 경로 약어** (이 문서에서 반복):
- `DS/` = `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/`
- `PV/` = `app-preview/src/main/kotlin/com/teamyg/parfait/preview/`

---

### Task 1: 컷 도형 `canvasCutCornerShape()`

캔버스 배경·날짜바·Dim이 공유하는 좌상단 45° 컷 사각형. 나머지 모든 태스크가 이걸 쓴다.

**Files:**
- Create: `DS/shape/CanvasCutCornerShape.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `fun canvasCutCornerShape(cutSize: Dp = 17.dp): Shape` (패키지 `com.teamyg.parfait.core.designsystem.shape`)

- [x] **Step 1: 파일 생성**

`DS/shape/CanvasCutCornerShape.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Figma 캔버스 영역 공용 실루엣 — 좌상단 모서리만 45°로 잘린 사각형.
 *
 * 캔버스 배경 / Canvas/Button-Date-Select / Dim 이 같은 실루엣을 쓴다.
 * 컷 다리 길이는 캔버스 크기에 비례하지 않고 고정이다.
 */
fun canvasCutCornerShape(cutSize: Dp = DefaultCutSize): Shape = CanvasCutCornerShape(cutSize)

private val DefaultCutSize: Dp = 17.dp

@Immutable
private data class CanvasCutCornerShape(private val cutSize: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cut = with(density) { cutSize.toPx() }
            .coerceAtMost(minOf(size.width, size.height))
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}
```

- [x] **Step 2: 컴파일 확인**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: 린트 확인**

Run: `./gradlew :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 2: `YGStrokeButton`

Figma `Button-Stroke`. 테두리 버튼, 텍스트 + 선택 아이콘. 폭은 호출자가 준다.

**Files:**
- Create: `DS/component/ygstrokebutton/YGStrokeButton.kt`

**Interfaces:**
- Consumes: 없음(Task 1과 무관 — 컷 도형을 쓰지 않는 각진 사각형이다)
- Produces:
  ```kotlin
  fun YGStrokeButton(
      text: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      @DrawableRes iconResource: Int? = null,
      isSelected: Boolean = false,
      isEnabled: Boolean = true,
      interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  )
  ```

- [x] **Step 1: 컴포저블 + 프리뷰 작성**

`DS/component/ygstrokebutton/YGStrokeButton.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygstrokebutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Button-Stroke
 *
 * 폭을 스스로 정하지 않는다 — 호출자가 `Modifier.weight`/`fillMaxWidth`로 준다.
 */
@Composable
fun YGStrokeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.none
    val isHighlighted = isEnabled && (isSelected || isPressed)
    val backgroundColor = if (isHighlighted) {
        YGAtomicColors.Gray.Gray100
    } else {
        YGAtomicColors.Gray.White
    }
    val borderColor = if (isEnabled) {
        YGAtomicColors.Gray.Gray500
    } else {
        YGAtomicColors.Gray.Gray200
    }
    val contentColor = if (isEnabled) {
        YGAtomicColors.Gray.Gray700
    } else {
        YGAtomicColors.Gray.Gray300
    }

    Row(
        modifier = modifier
            .height(SizeTokens.Size44.getDp())
            .background(
                color = backgroundColor,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            ).selectable(
                selected = isSelected,
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = YGTheme.layout.gap.gap1,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        iconResource?.let { resource ->
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = contentColor),
                modifier = Modifier.size(SizeTokens.Size20.getDp()),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGStrokeButtonPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGStrokeButton(
            text = "토핑 추가",
            onClick = {},
            iconResource = R.drawable.ic_plus,
        )
        YGStrokeButton(
            text = "토핑 추가",
            onClick = {},
            iconResource = R.drawable.ic_plus,
            isSelected = true,
        )
        YGStrokeButton(
            text = "토핑 추가",
            onClick = {},
            iconResource = R.drawable.ic_plus,
            isEnabled = false,
        )
        YGStrokeButton(
            text = "아이콘 없음",
            onClick = {},
        )
    }
}
```

- [x] **Step 2: 빌드 확인**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: 타이포 심볼 실재 확인**

Run: `grep -n "b02R" core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/theme/typography/YGTypography.kt`
Expected: `b02R` 필드가 나온다. 안 나오면 같은 파일에서 14sp Regular에 해당하는 실제 필드명을 찾아 교체하고, 이후 태스크에도 같은 이름을 쓴다.

- [x] **Step 4: 린트 확인**

Run: `./gradlew :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 3: `YGMenuItem`

Figma `Menu-Item`. 전폭 반투명 메뉴 항목. `YGStrokeButton`과 색 방향이 반대다(기본 반투명 → 눌리면 불투명).

**Files:**
- Create: `DS/component/ygmenuitem/YGMenuItem.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  ```kotlin
  fun YGMenuItem(
      text: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  )
  ```

- [x] **Step 1: 컴포저블 + 프리뷰 작성**

`DS/component/ygmenuitem/YGMenuItem.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygmenuitem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Menu-Item
 *
 * Canvas-Menu 확장 시 쌓이는 항목. 기본이 반투명이고 눌리면 불투명해진다.
 */
@Composable
fun YGMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.none
    val backgroundColor = if (isPressed) {
        YGAtomicColors.Gray.White
    } else {
        YGAtomicColors.Transparency.White75
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(SizeTokens.Size44.getDp())
            .background(
                color = backgroundColor,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ).clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button },
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray700,
            textAlign = TextAlign.Center,
        )
    }
}

@YGPreview
@Composable
private fun YGMenuItemPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGMenuItem(
            text = "카메라로 촬영",
            onClick = {},
        )
        YGMenuItem(
            text = "갤러리에서 선택",
            onClick = {},
        )
    }
}
```

- [x] **Step 2: `Transparency.White75` 심볼 실재 확인**

Run: `grep -n "White75\|White50" core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/theme/colors/YGAtomicColors.kt`
Expected: `White75`가 나온다. 없으면 파일에서 흰색 75% 항목의 실제 이름을 찾아 교체하고, Task 5·6에도 같은 이름을 쓴다.

- [x] **Step 3: 빌드 + 린트 확인**

Run: `./gradlew :core:designsystem:assembleDebug :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 4: `YGCanvasMenu` + 메뉴 모델

Figma `Canvas-Menu`. 하단 2버튼 행 고정 + 확장 시 그 위로 `YGMenuItem` 스택.

**Files:**
- Create: `DS/component/ygcanvasmenu/YGCanvasMenuAction.kt`
- Create: `DS/component/ygcanvasmenu/YGCanvasMenu.kt`

**Interfaces:**
- Consumes: `YGStrokeButton`(Task 2), `YGMenuItem`(Task 3)
- Produces:
  ```kotlin
  @Immutable data class YGCanvasMenuAction(val text: String, @DrawableRes val iconResource: Int?, val onClick: () -> Unit)
  @Immutable data class YGCanvasMenuItem(val text: String, val onClick: () -> Unit)

  fun YGCanvasMenu(
      addAction: YGCanvasMenuAction,
      editAction: YGCanvasMenuAction,
      modifier: Modifier = Modifier,
      expandedItems: List<YGCanvasMenuItem> = emptyList(),
  )
  ```

- [x] **Step 1: 모델 파일 작성**

`DS/component/ygcanvasmenu/YGCanvasMenuAction.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcanvasmenu

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/** Canvas-Menu 하단 행의 버튼 1개(Figma Button-Stroke) */
@Immutable
data class YGCanvasMenuAction(
    val text: String,
    @DrawableRes val iconResource: Int?,
    val onClick: () -> Unit,
)

/** Canvas-Menu 확장 시 쌓이는 항목 1개(Figma Menu-Item) */
@Immutable
data class YGCanvasMenuItem(
    val text: String,
    val onClick: () -> Unit,
)
```

- [x] **Step 2: 컴포저블 + 프리뷰 작성**

`DS/component/ygcanvasmenu/YGCanvasMenu.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcanvasmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygmenuitem.YGMenuItem
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Canvas-Menu
 *
 * 구조는 고정이고 문구만 주입받는다. [expandedItems] 가 비면 Figma `Status=Default`,
 * 차 있으면 `Status=Expanded` 다.
 */
@Composable
fun YGCanvasMenu(
    addAction: YGCanvasMenuAction,
    editAction: YGCanvasMenuAction,
    modifier: Modifier = Modifier,
    expandedItems: List<YGCanvasMenuItem> = emptyList(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        expandedItems.forEach { item ->
            YGMenuItem(
                text = item.text,
                onClick = item.onClick,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            YGStrokeButton(
                text = addAction.text,
                onClick = addAction.onClick,
                iconResource = addAction.iconResource,
                modifier = Modifier.weight(1f),
            )
            YGStrokeButton(
                text = editAction.text,
                onClick = editAction.onClick,
                iconResource = editAction.iconResource,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGCanvasMenuPreview() = PreviewBox {
    Column {
        YGCanvasMenu(
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = R.drawable.ic_plus,
                onClick = {},
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = R.drawable.ic_caret_right,
                onClick = {},
            ),
        )
        YGCanvasMenu(
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = R.drawable.ic_plus,
                onClick = {},
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = R.drawable.ic_caret_right,
                onClick = {},
            ),
            expandedItems = listOf(
                YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
            ),
        )
    }
}
```

- [x] **Step 3: 빌드 + 린트 확인**

Run: `./gradlew :core:designsystem:assembleDebug :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: 프리뷰 육안 확인**

Android Studio에서 `YGCanvasMenuPreview` 렌더. 확인 사항: 두 버튼이 정확히 반반으로 나뉜다 / 확장 항목이 버튼 행 **위**에 쌓인다 / 항목 사이 간격이 없다.

---

### Task 5: `YGCanvasDateSelectButton`

Figma `Canvas/Button-Date-Select`. 컷 도형 + 날짜·요일 + 우측 캘린더 아이콘 버튼.

**Files:**
- Create: `DS/component/ygcanvasdateselect/YGCanvasDateSelectButton.kt`

**Interfaces:**
- Consumes: `canvasCutCornerShape()`(Task 1), 기존 `YGIconButton`·`YGIconButtonSize`
- Produces:
  ```kotlin
  fun YGCanvasDateSelectButton(
      date: String,
      day: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
  )
  ```

- [x] **Step 1: `YGIconButtonSize` 심볼명 확인**

Run: `grep -n "SIZE_" core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygiconbutton/YGIconButtonSize.kt`
Expected: `SIZE_44` 상수가 나온다. 이름이 다르면 44 컨테이너에 해당하는 실제 상수를 쓴다.

- [x] **Step 2: 컴포저블 + 프리뷰 작성**

`DS/component/ygcanvasdateselect/YGCanvasDateSelectButton.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcanvasdateselect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.shape.canvasCutCornerShape
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Canvas/Button-Date-Select
 *
 * 캔버스 좌상단 컷 위에 얹히는 날짜 라벨. [date]·[day] 는 이미 포맷된 문자열이다.
 */
@Composable
fun YGCanvasDateSelectButton(
    date: String,
    day: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = canvasCutCornerShape()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SizeTokens.Size44.getDp())
            .background(
                color = YGAtomicColors.Transparency.White75,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ).padding(start = YGTheme.layout.padding.padding6),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray800,
            )
            Text(
                text = day,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray300,
            )
        }
        YGIconButton(
            iconResource = R.drawable.ic_calender,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClick,
        )
    }
}

@YGPreview
@Composable
private fun YGCanvasDateSelectButtonPreview() = PreviewBox {
    YGCanvasDateSelectButton(
        date = "May 20",
        day = "(Wed)",
        onClick = {},
    )
}
```

- [x] **Step 3: 빌드 + 린트 확인**

Run: `./gradlew :core:designsystem:assembleDebug :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: 프리뷰 육안 확인**

`YGCanvasDateSelectButtonPreview` 렌더. 확인 사항: 좌상단만 사선으로 잘려 있다 / 테두리가 사선을 따라간다 / 우측 아이콘이 44 영역 안 중앙이다.

---

### Task 6: `YGCanvasBackground` + `YGCanvas`

Figma `Canvas`. 배경 + 토핑 슬롯 + 날짜바 + 메뉴 + Dim 합성.

**Files:**
- Create: `DS/component/ygcanvas/YGCanvasBackground.kt`
- Create: `DS/component/ygcanvas/YGCanvas.kt`

**Interfaces:**
- Consumes: `canvasCutCornerShape()`(Task 1), `YGCanvasDateSelectButton`(Task 5), `YGCanvasMenu`·`YGCanvasMenuAction`·`YGCanvasMenuItem`(Task 4)
- Produces:
  ```kotlin
  sealed interface YGCanvasBackground {
      data class Solid(val color: Color) : YGCanvasBackground
      data class Image(val url: String) : YGCanvasBackground
  }

  fun YGCanvas(
      date: String,
      day: String,
      onDateSelectClick: () -> Unit,
      addAction: YGCanvasMenuAction,
      editAction: YGCanvasMenuAction,
      modifier: Modifier = Modifier,
      background: YGCanvasBackground = YGCanvasBackground.Solid(YGAtomicColors.Gray.Gray100),
      isDimmed: Boolean = false,
      expandedItems: List<YGCanvasMenuItem> = emptyList(),
      emptyMessage: String? = null,
      calendarContent: (@Composable () -> Unit)? = null,
      content: @Composable BoxScope.() -> Unit = {},
  )
  ```

- [x] **Step 1: 배경 타입 작성**

`DS/component/ygcanvas/YGCanvasBackground.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 캔버스 배경 — 사용자가 올린 이미지(URL) 또는 제시된 단색 중 하나다.
 */
@Immutable
sealed interface YGCanvasBackground {
    @Immutable
    data class Solid(val color: Color) : YGCanvasBackground

    @Immutable
    data class Image(val url: String) : YGCanvasBackground
}
```

- [x] **Step 2: Coil 의존 확인**

Run: `grep -rn "coil" core/designsystem/build.gradle.kts build-logic --include="*.kts" | head`
Expected: `:core:designsystem`이 coil-compose를 (직접 또는 컨벤션 플러그인으로) 받고 있다. `YGTheme.kt`가 이미 `AsyncImagePreviewHandler`를 import하므로 받고 있어야 한다. 못 찾으면 `core/designsystem/build.gradle.kts`의 `dependencies`에 `implementation(libs.coil.compose)`를 추가한다.

- [x] **Step 3: 컴포저블 + 프리뷰 작성**

`DS/component/ygcanvas/YGCanvas.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenu
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygcanvasdateselect.YGCanvasDateSelectButton
import com.teamyg.parfait.core.designsystem.shape.canvasCutCornerShape
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val CANVAS_AREA_ASPECT_RATIO = 9f / 16f

/**
 * Figma Canvas
 *
 * Figma 의 5개 `Status` 를 직교 파라미터 조합으로 표현한다.
 * `Empty`=[emptyMessage], `Expanded`=[isDimmed]+[expandedItems],
 * `Spotlighted`=[isDimmed], `Calendar`=[isDimmed]+[calendarContent].
 *
 * Dim 은 항상 최상단에 깔리고, 확장 메뉴와 캘린더만 그 위로 올라간다.
 */
@Composable
fun YGCanvas(
    date: String,
    day: String,
    onDateSelectClick: () -> Unit,
    addAction: YGCanvasMenuAction,
    editAction: YGCanvasMenuAction,
    modifier: Modifier = Modifier,
    background: YGCanvasBackground = YGCanvasBackground.Solid(YGAtomicColors.Gray.Gray100),
    isDimmed: Boolean = false,
    expandedItems: List<YGCanvasMenuItem> = emptyList(),
    emptyMessage: String? = null,
    calendarContent: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = canvasCutCornerShape()
    val isMenuRaised = expandedItems.isNotEmpty()
    val isCalendarRaised = calendarContent != null

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CanvasArea(
                shape = shape,
                background = background,
                emptyMessage = emptyMessage,
                content = content,
                dateSelect = {
                    if (isCalendarRaised.not()) {
                        YGCanvasDateSelectButton(
                            date = date,
                            day = day,
                            onClick = onDateSelectClick,
                        )
                    }
                },
            )
            if (isMenuRaised.not()) {
                YGCanvasMenu(
                    addAction = addAction,
                    editAction = editAction,
                )
            }
        }

        if (isDimmed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(color = YGAtomicColors.Transparency.Black25),
            )
        }

        if (isCalendarRaised) {
            Column(modifier = Modifier.fillMaxWidth()) {
                YGCanvasDateSelectButton(
                    date = date,
                    day = day,
                    onClick = onDateSelectClick,
                )
                calendarContent?.invoke()
            }
        }

        if (isMenuRaised) {
            YGCanvasMenu(
                addAction = addAction,
                editAction = editAction,
                expandedItems = expandedItems,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CanvasArea(
    shape: Shape,
    background: YGCanvasBackground,
    emptyMessage: String?,
    dateSelect: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
            .clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ),
    ) {
        when (background) {
            is YGCanvasBackground.Solid -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = background.color),
            )

            is YGCanvasBackground.Image -> AsyncImage(
                model = background.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        content()

        emptyMessage?.let { message ->
            Text(
                text = message,
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.Gray500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = YGTheme.layout.padding.padding6),
            )
        }

        dateSelect()
    }
}

@YGPreview
@Composable
private fun YGCanvasPreview() = PreviewBox {
    YGCanvas(
        date = "May 20",
        day = "(Wed)",
        onDateSelectClick = {},
        addAction = YGCanvasMenuAction(
            text = "토핑 추가",
            iconResource = R.drawable.ic_plus,
            onClick = {},
        ),
        editAction = YGCanvasMenuAction(
            text = "캔버스 편집",
            iconResource = R.drawable.ic_caret_right,
            onClick = {},
        ),
        emptyMessage = "아직 캔버스가 비어 있어요\n첫번째 토핑을 올려 캔버스를 채워보세요",
    )
}
```

- [x] **Step 4: 타이포 심볼 확인**

Run: `grep -n "c01M" core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/theme/typography/YGTypography.kt`
Expected: `c01M`이 나온다. 없으면 12sp Medium에 해당하는 실제 필드명으로 교체한다.

- [x] **Step 5: `Transparency.Black25` 심볼 확인**

Run: `grep -n "Black25" core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/theme/colors/YGAtomicColors.kt`
Expected: `Black25`가 나온다. 없으면 검정 25%에 해당하는 실제 이름으로 교체한다.

- [x] **Step 6: 빌드 + 린트 확인**

Run: `./gradlew :core:designsystem:assembleDebug :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 7: 프리뷰 육안 확인**

`YGCanvasPreview` 렌더. 확인 사항: 캔버스 영역이 9:16이고 그 아래 메뉴가 간격 없이 붙는다 / 좌상단 컷이 배경·날짜바 둘 다에 적용된다 / 안내문이 캔버스 영역 중앙이다.

---

### Task 7: `:app-preview` 갤러리 등록

5종을 컴포넌트 갤러리에서 실기기로 볼 수 있게 만든다.

**Files:**
- Create: `PV/navigation/key/NavKeyYGStrokeButton.kt`
- Create: `PV/navigation/key/NavKeyYGMenuItem.kt`
- Create: `PV/navigation/key/NavKeyYGCanvasMenu.kt`
- Create: `PV/navigation/key/NavKeyYGCanvasDateSelectButton.kt`
- Create: `PV/navigation/key/NavKeyYGCanvas.kt`
- Create: `PV/screen/component/YGStrokeButtonPreviewScreen.kt`
- Create: `PV/screen/component/YGMenuItemPreviewScreen.kt`
- Create: `PV/screen/component/YGCanvasMenuPreviewScreen.kt`
- Create: `PV/screen/component/YGCanvasDateSelectButtonPreviewScreen.kt`
- Create: `PV/screen/component/YGCanvasPreviewScreen.kt`
- Modify: `PV/model/ComponentCatalog.kt`
- Modify: `PV/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: Task 2~6의 모든 public 심볼
- Produces: 없음(최종 소비자)

- [x] **Step 1: NavKey 5개 작성**

각 파일은 이름만 다르고 내용이 같다. `PV/navigation/key/NavKeyYGStrokeButton.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGStrokeButton : NavKey
```

같은 방식으로 `NavKeyYGMenuItem`·`NavKeyYGCanvasMenu`·`NavKeyYGCanvasDateSelectButton`·`NavKeyYGCanvas`를 만든다(각각 `data object` 이름만 파일명과 맞춘다).

- [x] **Step 2: `YGStrokeButtonPreviewScreen` 작성**

`PV/screen/component/YGStrokeButtonPreviewScreen.kt`:

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGStrokeButtonPreviewScreen(
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
                var selected by remember { mutableStateOf(false) }
                PreviewSection("interactive (tap to toggle selected)") {
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = { selected = !selected },
                        iconResource = R.drawable.ic_plus,
                        isSelected = selected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("default / selected / disabled") {
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                        isSelected = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                        isEnabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("no icon / half width pair") {
                    YGStrokeButton(
                        text = "아이콘 없음",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        YGStrokeButton(
                            text = "토핑 추가",
                            onClick = {},
                            iconResource = R.drawable.ic_plus,
                            modifier = Modifier.weight(1f),
                        )
                        YGStrokeButton(
                            text = "캔버스 편집",
                            onClick = {},
                            iconResource = R.drawable.ic_caret_right,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGStrokeButtonPreviewScreen() = PreviewBox {
    YGStrokeButtonPreviewScreen(
        onBack = {},
    )
}
```

- [x] **Step 3: `YGMenuItemPreviewScreen` 작성**

`PV/screen/component/YGMenuItemPreviewScreen.kt` — 위 화면과 같은 골격(`Column` + `YGTopBarBack` + `LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp))`)에 본문만 아래로 채운다:

```kotlin
            item {
                PreviewSection("default (pressed는 실기기에서 눌러 확인)") {
                    YGMenuItem(
                        text = "카메라로 촬영",
                        onClick = {},
                    )
                    YGMenuItem(
                        text = "갤러리에서 선택",
                        onClick = {},
                    )
                }
            }
```

import는 `com.teamyg.parfait.core.designsystem.component.ygmenuitem.YGMenuItem`을 쓰고, 함수명은 `YGMenuItemPreviewScreen`, 프리뷰 함수는 `PreviewYGMenuItemPreviewScreen`으로 한다.

- [x] **Step 4: `YGCanvasMenuPreviewScreen` 작성**

같은 골격에 본문:

```kotlin
            item {
                var expanded by remember { mutableStateOf(false) }
                PreviewSection("interactive (tap 캔버스 편집 to toggle expand)") {
                    YGCanvasMenu(
                        addAction = YGCanvasMenuAction(
                            text = "토핑 추가",
                            iconResource = R.drawable.ic_plus,
                            onClick = {},
                        ),
                        editAction = YGCanvasMenuAction(
                            text = "캔버스 편집",
                            iconResource = R.drawable.ic_caret_right,
                            onClick = { expanded = !expanded },
                        ),
                        expandedItems = if (expanded) {
                            listOf(
                                YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                                YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
                            )
                        } else {
                            emptyList()
                        },
                    )
                }
            }
            item {
                PreviewSection("static: default / expanded") {
                    YGCanvasMenu(
                        addAction = YGCanvasMenuAction(
                            text = "토핑 추가",
                            iconResource = R.drawable.ic_plus,
                            onClick = {},
                        ),
                        editAction = YGCanvasMenuAction(
                            text = "캔버스 편집",
                            iconResource = R.drawable.ic_caret_right,
                            onClick = {},
                        ),
                    )
                    YGCanvasMenu(
                        addAction = YGCanvasMenuAction(
                            text = "토핑 추가",
                            iconResource = R.drawable.ic_plus,
                            onClick = {},
                        ),
                        editAction = YGCanvasMenuAction(
                            text = "캔버스 편집",
                            iconResource = R.drawable.ic_caret_right,
                            onClick = {},
                        ),
                        expandedItems = listOf(
                            YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                            YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
                        ),
                    )
                }
            }
```

- [x] **Step 5: `YGCanvasDateSelectButtonPreviewScreen` 작성**

같은 골격에 본문:

```kotlin
            item {
                PreviewSection("default") {
                    YGCanvasDateSelectButton(
                        date = "May 20",
                        day = "(Wed)",
                        onClick = {},
                    )
                }
            }
```

- [x] **Step 6: `YGCanvasPreviewScreen` 작성**

Figma 5상태 대응 조합 + 배경 2종. 같은 골격에 본문(공통 인자는 화면 파일 최상단 `private val`로 뽑는다):

```kotlin
private val PreviewAddAction = YGCanvasMenuAction(
    text = "토핑 추가",
    iconResource = R.drawable.ic_plus,
    onClick = {},
)

private val PreviewEditAction = YGCanvasMenuAction(
    text = "캔버스 편집",
    iconResource = R.drawable.ic_caret_right,
    onClick = {},
)
```

```kotlin
            item {
                PreviewSection("Status=Default (Solid 배경)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                    )
                }
            }
            item {
                PreviewSection("Status=Empty") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        emptyMessage = "아직 캔버스가 비어 있어요\n첫번째 토핑을 올려 캔버스를 채워보세요",
                    )
                }
            }
            item {
                PreviewSection("Status=Expanded (dim + 메뉴가 dim 위)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isDimmed = true,
                        expandedItems = listOf(
                            YGCanvasMenuItem(text = "카메라로 촬영", onClick = {}),
                            YGCanvasMenuItem(text = "갤러리에서 선택", onClick = {}),
                        ),
                    )
                }
            }
            item {
                PreviewSection("Status=Spotlighted (dim이 전부 덮음)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isDimmed = true,
                    )
                }
            }
            item {
                PreviewSection("Status=Calendar (슬롯 placeholder)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        isDimmed = true,
                        calendarContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(423.dp)
                                    .background(color = Color.White),
                            )
                        },
                    )
                }
            }
            item {
                PreviewSection("배경 Image(URL)") {
                    YGCanvas(
                        date = "May 20",
                        day = "(Wed)",
                        onDateSelectClick = {},
                        addAction = PreviewAddAction,
                        editAction = PreviewEditAction,
                        background = YGCanvasBackground.Image(
                            url = "https://picsum.photos/seed/parfait/720/1280",
                        ),
                    )
                }
            }
```

`Status=Calendar` placeholder는 캘린더 실물이 없기 때문이며(스펙의 명시적 제외 범위), 423dp는 Figma 패널 높이다.

- [x] **Step 7: 카탈로그 등록**

`PV/model/ComponentCatalog.kt` — import 5줄 추가(`NavKeyYGStrokeButton`·`NavKeyYGMenuItem`·`NavKeyYGCanvasDateSelectButton`·`NavKeyYGCanvasMenu`·`NavKeyYGCanvas`)하고, `BUTTON` 그룹 끝에:

```kotlin
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGStrokeButton",
        navKey = NavKeyYGStrokeButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGMenuItem",
        navKey = NavKeyYGMenuItem,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGCanvasDateSelectButton",
        navKey = NavKeyYGCanvasDateSelectButton,
    ),
```

`CONTAINER` 그룹 끝에:

```kotlin
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGCanvasMenu",
        navKey = NavKeyYGCanvasMenu,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGCanvas",
        navKey = NavKeyYGCanvas,
    ),
```

- [x] **Step 8: 엔트리 등록**

`PV/navigation/entry/ComponentEntryBuilders.kt` — NavKey·PreviewScreen import 각 5줄 추가 후, `componentEntryBuilders` 안에 블록 5개 추가:

```kotlin
    entry<NavKeyYGStrokeButton> {
        ScreenScaffold { modifier ->
            YGStrokeButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGMenuItem> {
        ScreenScaffold { modifier ->
            YGMenuItemPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCanvasMenu> {
        ScreenScaffold { modifier ->
            YGCanvasMenuPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCanvasDateSelectButton> {
        ScreenScaffold { modifier ->
            YGCanvasDateSelectButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCanvas> {
        ScreenScaffold { modifier ->
            YGCanvasPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

`PV/navigation/di/ComponentEntryModule.kt`는 수정하지 않는다(`@IntoSet` 바인딩이 함수 단위).

- [x] **Step 9: 빌드 + 전체 린트 확인**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 8: 실기기 육안 대조

**Files:** 없음(검증 전용)

**Interfaces:**
- Consumes: Task 1~7 전부
- Produces: 대조 결과 보고

- [x] **Step 1: 설치·실행**

Run: `./gradlew :app-preview:installDebug`
Expected: 갤러리 앱 설치. 실행 후 `Button` 카테고리에서 `YGStrokeButton`·`YGMenuItem`·`YGCanvasDateSelectButton`, `Container` 카테고리에서 `YGCanvasMenu`·`YGCanvas` 항목이 보인다.

- [x] **Step 2: Figma와 나란히 대조**

각 화면을 Figma 노드와 대조한다. 확인 항목:

| 화면 | 확인 |
|---|---|
| `YGStrokeButton` | 높이 44 / 아이콘이 텍스트 뒤 / disabled 테두리·텍스트가 옅어짐 / selected 배경이 회색 |
| `YGMenuItem` | 전폭 / 반투명 배경이 뒤를 비침 / 눌렀을 때 불투명해짐 |
| `YGCanvasMenu` | 두 버튼 정확히 반반 / 확장 항목이 버튼 행 **위** / 항목 사이 간격 0 |
| `YGCanvasDateSelectButton` | 좌상단 45° 컷 / 테두리가 사선을 따라감 / 날짜·요일 색 대비 / 우측 아이콘 44 중앙 |
| `YGCanvas` | 캔버스 영역 9:16 / 메뉴가 간격 0으로 붙음 / Dim이 컷 실루엣을 따름 / Expanded는 메뉴가 Dim 위 / Spotlighted는 메뉴까지 덮임 / Calendar는 날짜바+패널이 Dim 위 / Image 배경이 Crop으로 채워짐 |

- [x] **Step 3: 상호작용 확인**

`YGStrokeButton` selected 토글, `YGCanvasMenu` 확장 토글, 각 컴포넌트 pressed를 손으로 눌러 확인한다(pressed는 정적 렌더로 재현되지 않는다).

- [x] **Step 4: 결과 반영**

발견된 차이를 스펙 [2026-07-31-designsystem-canvas-components](../../specs/archive/2026-07-31-designsystem-canvas-components.md)의 "설계에서 달라진 점"으로 기록하고, 미해결 항목은 [parfait open-questions](../../../synthesis/open-questions.md)에 등록한다.

- [x] **Step 5: 커밋하지 않는다**

Run: `git -C <TJYG-Android> status --short`
Expected: 변경 파일이 스테이지되지 않은 채 남아 있다. **`git commit`을 실행하지 않는다**(작업자 지시). 사용자에게 변경 파일 목록과 검증 결과를 보고하고 종료한다.
