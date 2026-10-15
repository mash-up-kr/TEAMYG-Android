---
id: designsystem-button-missing-components
title: 디자인시스템 버튼 영역 미구현 컴포넌트 5종 신설 구현 계획
status: done
type: work-order
created: 2026-07-30
updated: 2026-08-01
platforms: android
owner: TJYG-Android 디자인시스템
related_adr:
  - ADR-0010
related_spec:
  - designsystem-button-missing-components
related_code:
  - YGEditTabButton.kt#YGEditTabButton
  - YGEditButton.kt#YGEditButton
  - YGCircleButton.kt#YGCircleButton
  - YGCircleButtonType.kt#YGCircleButtonType
  - YGEditActionButton.kt#YGEditActionButton
  - YGCameraShutter.kt#YGCameraShutter
  - SizeTokens.kt#SizeTokens
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
archived_reason: PR #183 develop 머지 완료(2026-08-01) — 5종 신설·YGToggleButton 삭제 반영, 스펙 implemented 전환
tags: [plan, parfait, designsystem, figma-sync]
---

# 디자인시스템 버튼 영역 미구현 컴포넌트 5종 신설 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** Figma "버튼" 영역 14종 중 대응 구현체가 없던 5종을 `core:designsystem`에 신설하고, 대응 원본이 없는 `YGToggleButton`을 삭제한다.

**Architecture:** 크기 토큰(`Size18`·`Size28`)을 먼저 넣고, 컴포넌트를 하나씩 만들면서 각 컴포넌트의 프리뷰와 `:app-preview` 갤러리 화면까지 같은 Task에서 끝낸다. 그래야 Task마다 실기기에서 눈으로 볼 수 있는 결과가 남는다. 삭제는 신규 5종이 다 들어온 뒤 별도 Task로 처리한다.

**Tech Stack:** Kotlin, Jetpack Compose, `core:designsystem`(`YGTheme` + `YGAtomicColors` + `SizeTokens`), `:app-preview`(Navigation3 + Hilt multibinding 갤러리), Gradle, ktlint.

## 실행 기록 (2026-07-30)

Task 1~7 전량 수행. Task 8은 검증까지 수행하고 문서 처리 일부를 의도적으로 보류했다.

- **통과**: Task별 `compileDebugKotlin`·`ktlintMainSourceSetCheck`, 최종 `:core:designsystem`·`:app-preview` `assembleDebug`, repo 전체 `ktlintCheck`. 실기기 갤러리에서 5종 육안 대조 + `BUTTON` 카테고리에 신규 5종 등록·`YGToggleButton` 소멸 확인, 삭제 후 잔존 참조 0건
- **계획과 달라진 점 2건**(둘 다 갤러리 검증에서 드러난 결함 수정):
  - `YGCircleButtonType`에 **`iconTint` 추가** — 계획은 tint를 걸지 않기로 했으나 저장소 아이콘 드로어블이 전부 검정이라 `Secondary`(어두운 원)에서 아이콘이 묻혔다. Figma 스크린샷으로 Secondary 아이콘이 흰색임을 확인하고 `Default`·`Small` = `Gray.Gray900`, `Secondary` = `Gray.White`로 지정
  - `YGEditTabButton`에 **`width(IntrinsicSize.Max)` 추가** — 밑줄 `fillMaxWidth`가 부모 최대 폭을 채워 화면 전체로 늘어나고 나머지 탭이 밖으로 밀렸다
- **미검증**: pressed 상태 전반. `adb shell input`으로 누른 상태가 Compose `interactionSource`에 반영되지 않는다(선행 라운드와 같은 한계)
- **as-built 차이(계획 코드블록 대비)**: `YGCircleButtonType`의 변형별·속성별 KDoc은 최종 코드에 없다. 파일 상단 컴포넌트 KDoc이 Figma 대응을 밝히고 변형명이 Figma 변형명과 같아 중복이라는 판단이다. KDoc 병기 규약은 컴포넌트 단위로 적용된다. 그 외 인자 순서·주석 형식·`Spacer` 사용 등 스타일 차이는 문서가 기술하는 층위가 아니다
- **IDE 프리뷰 렌더**: 에이전트가 실행할 수 없어 갤러리 실기기 확인으로 대체했다
- **보류(계획 Step 5·6 일부)**: 코드가 TJYG-Android에 **미커밋**이라 `design-system.md` as-built 갱신, 스펙·계획 `implemented`/`done` 전환, 아카이브 이동, open-questions `해소됨` 전환은 머지 후로 옮긴다
- **커밋**: 에이전트는 TJYG-Android를 커밋하지 않았다(Global Constraints)

## Global Constraints

- **TJYG-Android 저장소에 커밋하지 않는다.** 편집만 한다(작업자 지시, 2026-07-30). 브랜치는 `feature/sync-button-component`를 그대로 쓴다. parfait 문서 커밋은 사용자 승인 후.
- **테스트를 쓰지 않는다.** `core:designsystem`에 테스트 소스셋이 없고 이 계획에서 만들지 않는다. Task별 검증은 `compileDebugKotlin` → `ktlintMainSourceSetCheck` → (마지막 Task에서) 갤러리 육안.
- 테마 값은 `YGTheme.colorScheme`/`.typography`/`.shapes`/`.layout`으로 읽고, 크기만 `SizeTokens.SizeN.getDp()`로 직접 읽는다.
- 색은 기존 컴포넌트 관용구대로 `YGAtomicColors`를 직접 참조한다.
- 테두리 두께는 리터럴 dp를 쓴다(`1.dp`·`1.4.dp`·`1.5.dp`) — 두께용 토큰 스케일이 없다(`YGDate`의 `0.75.dp` 선례).
- 프리뷰는 `@YGPreview` + `PreviewBox`, 프리뷰 함수는 `private`.
- 신규 컴포넌트는 `*Colors` data class를 만들지 않는다(스펙 "Colors 분리 판단").
- 코드·식별자·파일명은 영어. KDoc으로 대응 Figma 변형명을 병기한다.
- 갤러리 화면은 기존 골격(`Column` → `YGTopBarBack` → `LazyColumn(contentPadding 16dp, spacedBy 8dp)` → `item { PreviewSection(label) { … } }`)을 따른다.

---

## File Structure

### `core:designsystem`

| 파일 | 책임 |
|---|---|
| `theme/size/SizeTokens.kt` (수정) | `Size18`·`Size28` 스케일 추가 |
| `component/ygcirclebutton/YGCircleButtonType.kt` (신규) | 3변형의 색·아이콘 크기·채움 위치 |
| `component/ygcirclebutton/YGCircleButton.kt` (신규) | 원형 아이콘 버튼 본체 + 프리뷰 |
| `component/ygeditactionbutton/YGEditActionButton.kt` (신규) | 반투명 원형 액션 버튼 + 프리뷰 |
| `component/ygcamerashutter/YGCameraShutter.kt` (신규) | 이중 원 셔터 + 프리뷰 |
| `component/ygeditbutton/YGEditButton.kt` (신규) | 각짐 선택 버튼 + 프리뷰 |
| `component/ygedittabbutton/YGEditTabButton.kt` (신규) | 밑줄 탭 + 프리뷰 |
| `component/ygtogglebutton/` (삭제) | `YGToggleButton.kt`·`YGToggleButtonPreviewData.kt` |

### `:app-preview`

| 파일 | 책임 |
|---|---|
| `navigation/key/NavKeyYG{CircleButton,EditActionButton,CameraShutter,EditButton,EditTabButton}.kt` (신규 5) | 화면 키 |
| `screen/component/YG{…}PreviewScreen.kt` (신규 5) | 변형·상태 showcase |
| `navigation/key/NavKeyYGToggleButton.kt` (삭제) | — |
| `screen/component/YGToggleButtonPreviewScreen.kt` (삭제) | — |
| `model/ComponentCatalog.kt` (수정) | `BUTTON` 카테고리 5줄 추가, 토글 1줄 제거 |
| `navigation/entry/ComponentEntryBuilders.kt` (수정) | `entry` 5블록 추가, 토글 블록 제거 |

---

## 갤러리 화면 골격 (공용 템플릿 G)

Task 2~6이 만드는 `:app-preview` 화면 5개는 모두 이 골격을 쓴다. **`{Name}`을 컴포넌트명으로 바꾸고
`{BODY}` 자리에 각 Task가 지정한 `item { … }` 블록을 넣는다.** 본문 코드가 쓰는 심볼
(`YGAtomicColors`·`Box`·`background`·`remember` 등)의 import는 그 Task 본문에 맞춰 추가한다.

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YG{Name}PreviewScreen(
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
            {BODY}
        }
    }
}

@YGPreview
@Composable
private fun PreviewYG{Name}PreviewScreen() = PreviewBox {
    YG{Name}PreviewScreen(
        onBack = {},
    )
}
```

## 갤러리 배선 골격 (공용 템플릿 W)

카탈로그·entry 등록도 5개 화면이 같은 형태다. `{Name}`을 컴포넌트명으로 바꾼다.

`ComponentCatalog.kt` — `BUTTON` 항목들 뒤에 추가하고 `NavKeyYG{Name}` import를 넣는다.

```kotlin
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YG{Name}",
        navKey = NavKeyYG{Name},
    ),
```

`ComponentEntryBuilders.kt` — `componentEntryBuilders` 함수 안에 추가하고 import 2개
(`NavKeyYG{Name}`·`YG{Name}PreviewScreen`)를 넣는다.

```kotlin
    entry<NavKeyYG{Name}> {
        ScreenScaffold { modifier ->
            YG{Name}PreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

`navigation/di/ComponentEntryModule.kt`는 수정하지 않는다 — `@IntoSet` 바인딩이 함수 단위다.

---

## Tasks

### Task 1: `SizeTokens`에 18·28 추가

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/theme/size/SizeTokens.kt`

**Interfaces:**
- Produces: `SizeTokens.Size18`·`SizeTokens.Size28`(둘 다 `SizeToken`). Task 2~6이 쓴다.
- Consumes: 없음.

- [x] **Step 1: 두 스케일 추가**

기존 상수 사이에 오름차순으로 끼운다.

```kotlin
    val Size16: SizeToken = SizeToken(16)
    val Size18: SizeToken = SizeToken(18)
    val Size20: SizeToken = SizeToken(20)
    val Size24: SizeToken = SizeToken(24)
    val Size28: SizeToken = SizeToken(28)
    val Size32: SizeToken = SizeToken(32)
```

- [x] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL. `SizeTokens`는 `object`의 `val` 추가라 기존 사용처에 영향이 없다.

> 커밋하지 않는다(Global Constraints).

---

### Task 2: `YGCircleButton` + `YGCircleButtonType`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcirclebutton/YGCircleButtonType.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcirclebutton/YGCircleButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGCircleButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGCircleButtonPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: `SizeTokens.Size18`·`Size28`(Task 1).
- Produces: `YGCircleButton(iconResource: Int, type: YGCircleButtonType, contentDescription: String?, onClick: () -> Unit, modifier: Modifier, interactionSource: MutableInteractionSource)` + `YGCircleButtonType.{Default, Secondary, Small}`.

- [x] **Step 1: `YGCircleButtonType` 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcirclebutton

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

@Immutable
sealed interface YGCircleButtonType {
    val backgroundColor: Color

    val pressedBackgroundColor: Color

    val borderColor: Color

    val iconSize: Dp

    /**
     * 배경·테두리를 바깥 원에 그릴지 여부.
     *
     * `true`면 패딩으로 도출된 바깥 원(44)에 그리고, `false`면 안쪽 원(28)에 그린다.
     */
    val paintsOuterCircle: Boolean

    /** Figma Button-Circle Type=Default */
    data object Default : YGCircleButtonType {
        override val backgroundColor: Color = YGAtomicColors.Gray.White
        override val pressedBackgroundColor: Color = YGAtomicColors.Gray.Gray100
        override val borderColor: Color = YGAtomicColors.Transparency.Black5
        override val iconSize: Dp = SizeTokens.Size28.getDp()
        override val paintsOuterCircle: Boolean = true
    }

    /** Figma Button-Circle Type=Secondary */
    data object Secondary : YGCircleButtonType {
        override val backgroundColor: Color = YGAtomicColors.Gray.Gray900
        override val pressedBackgroundColor: Color = YGAtomicColors.Gray.Gray950
        override val borderColor: Color = YGAtomicColors.Transparency.White25
        override val iconSize: Dp = SizeTokens.Size28.getDp()
        override val paintsOuterCircle: Boolean = true
    }

    /** Figma Button-Circle Type=Small — 44 터치 영역 안에 28 원을 그린다 */
    data object Small : YGCircleButtonType {
        override val backgroundColor: Color = YGAtomicColors.Gray.White
        override val pressedBackgroundColor: Color = YGAtomicColors.Gray.Gray100
        override val borderColor: Color = YGAtomicColors.Transparency.Black5
        override val iconSize: Dp = SizeTokens.Size18.getDp()
        override val paintsOuterCircle: Boolean = false
    }
}
```

`YGButtonType`과 달리 `@get:Composable`을 쓰지 않는다 — 값이 전부 `YGAtomicColors`·`SizeTokens`(둘 다 평범한 `object`)에서 오고 `YGTheme`를 읽지 않는다. 모양(`radius.round`)만 컴포저블 본문에서 읽는다.

- [x] **Step 2: `YGCircleButton` 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcirclebutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGCircleButton(
    @DrawableRes iconResource: Int,
    type: YGCircleButtonType,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round
    val background = if (isPressed) type.pressedBackgroundColor else type.backgroundColor
    val fill = Modifier
        .background(color = background, shape = shape)
        .border(width = 1.dp, color = type.borderColor, shape = shape)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
            .semantics { role = Role.Button }
            .then(if (type.paintsOuterCircle) fill else Modifier)
            .padding(YGTheme.layout.padding.padding3),
    ) {
        if (type.paintsOuterCircle) {
            YGCircleButtonIcon(
                iconResource = iconResource,
                contentDescription = contentDescription,
                iconSize = type.iconSize,
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(SizeTokens.Size28.getDp())
                    .then(fill),
            ) {
                YGCircleButtonIcon(
                    iconResource = iconResource,
                    contentDescription = contentDescription,
                    iconSize = type.iconSize,
                )
            }
        }
    }
}

@Composable
private fun YGCircleButtonIcon(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    iconSize: Dp,
) {
    Image(
        painter = painterResource(id = iconResource),
        contentDescription = contentDescription,
        modifier = Modifier.size(iconSize),
    )
}
```

- `padding3`(8) + 아이콘 `Size28` → 바깥 원 44가 도출된다. `Small`은 같은 패딩 안에 28 원을 놓아 결과 지름이 같다.
- `ColorFilter.tint`를 걸지 않는다 — Figma가 아이콘 색을 에셋에 담고 있어 대조된 tint 값이 없다(스펙 열린 질문). 리소스 색을 그대로 쓴다.
- `clip`을 `clickable` 앞에 둬서 터치·리플 영역이 원을 넘지 않게 한다.

- [x] **Step 3: 프리뷰 추가**

같은 파일 끝에 붙인다.

```kotlin
@YGPreview
@Composable
private fun YGCircleButtonPreview() = PreviewBox {
    Row(horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4)) {
        YGCircleButton(
            iconResource = R.drawable.ic_caret_left,
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = {},
        )
        YGCircleButton(
            iconResource = R.drawable.ic_plus,
            type = YGCircleButtonType.Secondary,
            contentDescription = null,
            onClick = {},
        )
        YGCircleButton(
            iconResource = R.drawable.ic_rotate,
            type = YGCircleButtonType.Small,
            contentDescription = null,
            onClick = {},
        )
    }
}
```

- [x] **Step 4: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL.

- [x] **Step 5: `NavKey` 추가**

Create `app-preview/.../navigation/key/NavKeyYGCircleButton.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGCircleButton : NavKey
```

- [x] **Step 6: 갤러리 화면 추가**

Create `app-preview/.../screen/component/YGCircleButtonPreviewScreen.kt`:

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
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGCircleButtonPreviewScreen(
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
                PreviewSection("Default / Secondary / Small") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        YGCircleButton(
                            iconResource = R.drawable.ic_caret_left,
                            type = YGCircleButtonType.Default,
                            contentDescription = "뒤로",
                            onClick = {},
                        )
                        YGCircleButton(
                            iconResource = R.drawable.ic_plus,
                            type = YGCircleButtonType.Secondary,
                            contentDescription = "추가",
                            onClick = {},
                        )
                        YGCircleButton(
                            iconResource = R.drawable.ic_rotate,
                            type = YGCircleButtonType.Small,
                            contentDescription = "전환",
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
private fun PreviewYGCircleButtonPreviewScreen() = PreviewBox {
    YGCircleButtonPreviewScreen(
        onBack = {},
    )
}
```

> `Secondary`는 어두운 배경 위에 놓이는 변형이라 흰 갤러리 배경에서는 테두리(`Transparency.White25`)가 보이지 않는다. 눈으로 확인할 대상은 배경색·지름·아이콘 크기다.

- [x] **Step 7: 카탈로그 등록**

`ComponentCatalog.kt`의 `BUTTON` 항목 뒤에 추가하고 import도 넣는다.

```kotlin
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGCircleButton",
        navKey = NavKeyYGCircleButton,
    ),
```

- [x] **Step 8: entry 등록**

`ComponentEntryBuilders.kt`의 `componentEntryBuilders` 안에 추가하고 import 2개(`NavKeyYGCircleButton`, `YGCircleButtonPreviewScreen`)를 넣는다.

```kotlin
    entry<NavKeyYGCircleButton> {
        ScreenScaffold { modifier ->
            YGCircleButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [x] **Step 9: 컴파일 + ktlint (app-preview 포함)**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

> 커밋하지 않는다.

---

### Task 3: `YGEditActionButton`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygeditactionbutton/YGEditActionButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGEditActionButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGEditActionButtonPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: 없음(`SizeTokens.Size24`는 기존 값).
- Produces: `YGEditActionButton(iconResource: Int, contentDescription: String?, onClick: () -> Unit, modifier: Modifier, isEnabled: Boolean, interactionSource: MutableInteractionSource)`.

- [x] **Step 1: 컴포넌트 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygeditactionbutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/** Figma Button-Edit-Action — 어두운 화면 위에 얹는 반투명 원형 액션 버튼 */
@Composable
fun YGEditActionButton(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round
    val background = when {
        isEnabled.not() -> YGAtomicColors.Transparency.Black5
        isPressed -> YGAtomicColors.Transparency.Black75
        else -> YGAtomicColors.Transparency.Black50
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                enabled = isEnabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button }
            .padding(YGTheme.layout.padding.padding1),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(color = background, shape = shape)
                .border(width = 1.5.dp, color = YGAtomicColors.Transparency.White25, shape = shape)
                .padding(YGTheme.layout.padding.padding3),
        ) {
            Image(
                painter = painterResource(id = iconResource),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(color = YGAtomicColors.Gray.White),
                modifier = Modifier.size(SizeTokens.Size24.getDp()),
            )
        }
    }
}
```

내부 원 = `padding3`(8) + 아이콘 `Size24` → 40, 바깥 = `padding1`(2) 래핑 → 44.
Figma는 38/42이며 2dp 차이는 의도된 절충이다(스펙 열린 질문).

- [x] **Step 2: 프리뷰 추가**

배경이 반투명이라 어두운 판 위에 올려야 보인다.

```kotlin
@YGPreview
@Composable
private fun YGEditActionButtonPreview() = PreviewBox {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = Modifier
            .background(YGAtomicColors.Gray.Gray900)
            .padding(YGTheme.layout.padding.padding6),
    ) {
        YGEditActionButton(
            iconResource = R.drawable.ic_arrow_left,
            contentDescription = null,
            onClick = {},
        )
        YGEditActionButton(
            iconResource = R.drawable.ic_arrow_left,
            contentDescription = null,
            onClick = {},
            isEnabled = false,
        )
    }
}
```

import 추가: `androidx.compose.foundation.layout.Arrangement`, `androidx.compose.foundation.layout.Row`.

- [x] **Step 3: `NavKey` 추가**

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGEditActionButton : NavKey
```

- [x] **Step 4: 갤러리 화면 추가**

위 **공용 템플릿 G**를 그대로 쓰고 `{BODY}`에 아래를 넣는다(`{Name}` = `EditActionButton`). 반투명 배경이라 섹션 안을 어둡게 깐다.

```kotlin
            item {
                PreviewSection("enabled / disabled (dark backdrop)") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .background(YGAtomicColors.Gray.Gray900)
                            .padding(16.dp),
                    ) {
                        YGEditActionButton(
                            iconResource = R.drawable.ic_arrow_left,
                            contentDescription = "이전",
                            onClick = {},
                        )
                        YGEditActionButton(
                            iconResource = R.drawable.ic_arrow_left,
                            contentDescription = "이전",
                            onClick = {},
                            isEnabled = false,
                        )
                    }
                }
            }
```

- [x] **Step 5: 카탈로그 + entry 등록**

`ComponentCatalog.kt`에 `label = "YGEditActionButton"`, `navKey = NavKeyYGEditActionButton` 항목을 추가하고,
`ComponentEntryBuilders.kt`에 `entry<NavKeyYGEditActionButton>` 블록을 추가한다(import 2개 포함).

- [x] **Step 6: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

> 커밋하지 않는다.

---

### Task 4: `YGCameraShutter`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcamerashutter/YGCameraShutter.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGCameraShutter.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGCameraShutterPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: 없음.
- Produces: `YGCameraShutter(onClick: () -> Unit, modifier: Modifier, interactionSource: MutableInteractionSource)`.

- [x] **Step 1: 컴포넌트 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygcamerashutter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/** Figma Camera-Shutter — 흰 외곽 링 + 어두운 내부 원 */
@Composable
fun YGCameraShutter(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White, shape = shape)
            .clip(shape)
            .clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
            .semantics { role = Role.Button }
            .padding(YGTheme.layout.padding.padding2),
    ) {
        Box(
            modifier = Modifier
                .size(SizeTokens.Size48.getDp())
                .background(
                    color = if (isPressed) YGAtomicColors.Gray.Gray950 else YGAtomicColors.Gray.Gray900,
                    shape = shape,
                ),
        )
    }
}

@YGPreview
@Composable
private fun YGCameraShutterPreview() = PreviewBox {
    Box(
        modifier = Modifier
            .background(YGAtomicColors.Gray.Black)
            .padding(YGTheme.layout.padding.padding6),
    ) {
        YGCameraShutter(onClick = {})
    }
}
```

`padding2`(4) + 내부 `Size48` → 외곽 56이 도출된다. Figma가 두 원을 래스터로 내보내므로 에셋을 쓰지 않고 도형 2개로 그린다.

- [x] **Step 2: `NavKey` 추가**

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGCameraShutter : NavKey
```

- [x] **Step 3: 갤러리 화면 추가**

위 **공용 템플릿 G**(`{Name}` = `CameraShutter`)의 `{BODY}`에 아래를 넣는다.

```kotlin
            item {
                PreviewSection("shutter (dark backdrop, tap to see pressed)") {
                    Box(
                        modifier = Modifier
                            .background(YGAtomicColors.Gray.Black)
                            .padding(24.dp),
                    ) {
                        YGCameraShutter(onClick = {})
                    }
                }
            }
```

- [x] **Step 4: 카탈로그 + entry 등록**

위 **공용 템플릿 W**(`{Name}` = `CameraShutter`)대로 등록한다.

- [x] **Step 5: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

> 커밋하지 않는다.

---

### Task 5: `YGEditButton`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygeditbutton/YGEditButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGEditButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGEditButtonPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: 없음.
- Produces: `YGEditButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier, iconResource: Int?, interactionSource: MutableInteractionSource)`.

- [x] **Step 1: 컴포넌트 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygeditbutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Figma Button-Edit — 각진 선택형 버튼. 아이콘은 텍스트 뒤에 온다 */
@Composable
fun YGEditButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val shape = YGTheme.shapes.radius.none
    val backgroundColor = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.White
    val borderColor = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.Gray100
    val contentColor = if (isSelected) YGAtomicColors.Gray.White else YGAtomicColors.Gray.Gray900

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = backgroundColor, shape = shape)
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ).padding(vertical = YGTheme.layout.padding.padding3),
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02SB,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        iconResource?.let { resource ->
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = contentColor),
                modifier = Modifier.size(SizeTokens.Size24.getDp()),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGEditButtonPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGEditButton(
            text = "편집",
            isSelected = false,
            onClick = {},
            iconResource = R.drawable.ic_minus_round,
        )
        YGEditButton(
            text = "편집",
            isSelected = true,
            onClick = {},
            iconResource = R.drawable.ic_minus_round,
        )
        YGEditButton(
            text = "아이콘 없음",
            isSelected = false,
            onClick = {},
        )
    }
}
```

- 텍스트와 아이콘 사이 간격을 두지 않는다 — Figma에 간격 토큰이 없다(`Arrangement.Center`만).
- 폭은 호출자가 정한다. Figma 162는 캔버스 기준폭이다.
- 선택 상태를 접근성에 알리려고 `selectable`을 쓴다(`YGToggleButton` 선례). 이 관용구의 표준화 여부는 open-questions에 남아 있다.

- [x] **Step 2: `NavKey` 추가**

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGEditButton : NavKey
```

- [x] **Step 3: 갤러리 화면 추가**

위 **공용 템플릿 G**(`{Name}` = `EditButton`)의 `{BODY}`에 아래 2섹션을 넣는다. 선택 상태가 있으므로 `remember` 인터랙션을 포함한다.

```kotlin
            item {
                var selected by remember { mutableStateOf(false) }
                PreviewSection("interactive (tap to toggle)") {
                    YGEditButton(
                        text = "편집",
                        isSelected = selected,
                        onClick = { selected = !selected },
                        iconResource = R.drawable.ic_minus_round,
                    )
                }
            }
            item {
                PreviewSection("default / selected (static) + no icon") {
                    YGEditButton(
                        text = "편집",
                        isSelected = false,
                        onClick = {},
                        iconResource = R.drawable.ic_minus_round,
                    )
                    YGEditButton(
                        text = "편집",
                        isSelected = true,
                        onClick = {},
                        iconResource = R.drawable.ic_minus_round,
                    )
                    YGEditButton(
                        text = "아이콘 없음",
                        isSelected = false,
                        onClick = {},
                    )
                }
            }
```

import 추가: `androidx.compose.runtime.getValue`·`mutableStateOf`·`remember`·`setValue`(`YGToggleButtonPreviewScreen`과 동일 세트).

- [x] **Step 4: 카탈로그 + entry 등록**

위 **공용 템플릿 W**(`{Name}` = `EditButton`)대로 등록한다.

- [x] **Step 5: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

> 커밋하지 않는다.

---

### Task 6: `YGEditTabButton`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygedittabbutton/YGEditTabButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGEditTabButton.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGEditTabButtonPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: 없음.
- Produces: `YGEditTabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier, interactionSource: MutableInteractionSource)`.

- [x] **Step 1: 컴포넌트 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygedittabbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/** Figma Button-Edit-Tab — 선택 시 텍스트 폭만큼 밑줄이 그려지는 탭 */
@Composable
fun YGEditTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ).padding(
                horizontal = YGTheme.layout.padding.padding4,
                vertical = YGTheme.layout.padding.padding3,
            ),
    ) {
        Text(
            text = text,
            style = if (isSelected) YGTheme.typography.body.b01SB else YGTheme.typography.body.b01R,
            color = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.Gray500,
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding2))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.4.dp)
                    .background(color = YGAtomicColors.Gray.Gray900),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGEditTabButtonPreview() = PreviewBox {
    Row(horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2)) {
        YGEditTabButton(
            text = "토핑",
            isSelected = true,
            onClick = {},
        )
        YGEditTabButton(
            text = "사진",
            isSelected = false,
            onClick = {},
        )
    }
}
```

- 밑줄은 `Column`의 폭(= 텍스트 폭)을 채운다. 바깥 `padding4`/`padding3`은 터치 영역이라 밑줄이 거기까지 늘어나지 않는다.
- 텍스트와 밑줄 사이 `padding2`(4) 간격은 **선택 여부와 무관하게** 유지한다 — Figma도 컨테이너 `pb`가 두 상태에 다 걸려 있고, 없으면 선택 시 텍스트가 위로 튄다.
- 두께 `1.4.dp`는 리터럴이다(두께 토큰 없음).
- 탭이므로 `Role.Tab`을 쓴다.

- [x] **Step 2: `NavKey` 추가**

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGEditTabButton : NavKey
```

- [x] **Step 3: 갤러리 화면 추가**

위 **공용 템플릿 G**(`{Name}` = `EditTabButton`)의 `{BODY}`에 아래를 넣는다. 탭은 여러 개가 나란히 놓이는 컴포넌트라 실제 선택 이동을 재현한다.

```kotlin
            item {
                var selectedIndex by remember { mutableIntStateOf(0) }
                val labels = listOf("토핑", "사진", "설정")
                PreviewSection("tab row (tap to move selection)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.forEachIndexed { index, label ->
                            YGEditTabButton(
                                text = label,
                                isSelected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                            )
                        }
                    }
                }
            }
```

import 추가: `androidx.compose.runtime.getValue`·`mutableIntStateOf`·`remember`·`setValue`.

- [x] **Step 4: 카탈로그 + entry 등록**

위 **공용 템플릿 W**(`{Name}` = `EditTabButton`)대로 등록한다.

- [x] **Step 5: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

> 커밋하지 않는다.

---

### Task 7: `YGToggleButton` 삭제

신규 5종이 다 들어온 뒤에 지운다 — 먼저 지우면 갤러리 `BUTTON` 카테고리에 빈 자리가 생긴다.

**Files:**
- Delete: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtogglebutton/YGToggleButton.kt`
- Delete: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtogglebutton/YGToggleButtonPreviewData.kt`
- Delete: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGToggleButton.kt`
- Delete: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToggleButtonPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: Task 2~6(대체물 `YGEditButton`이 이미 있어야 한다).
- Produces: 없음.

- [x] **Step 1: 사용처 확인**

Run:
```bash
grep -rn "YGToggleButton" --include="*.kt" core feature app-preview data domain app | grep -v build
```
Expected: `component/ygtogglebutton/` 2파일 + `:app-preview` 4곳(NavKey·PreviewScreen·Catalog·EntryBuilders)만. **다른 결과가 나오면 멈추고 보고한다** — 실화면 사용처가 생겼다는 뜻이다.

- [x] **Step 2: 파일 4개 삭제**

Run:
```bash
rm core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtogglebutton/YGToggleButton.kt
rm core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtogglebutton/YGToggleButtonPreviewData.kt
rm app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGToggleButton.kt
rm app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToggleButtonPreviewScreen.kt
```

빈 디렉토리 `component/ygtogglebutton/`도 지운다.

- [x] **Step 3: 카탈로그 항목 제거**

`ComponentCatalog.kt`에서 아래 블록과 `NavKeyYGToggleButton` import를 지운다.

```kotlin
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGToggleButton",
        navKey = NavKeyYGToggleButton,
    ),
```

- [x] **Step 4: entry 제거**

`ComponentEntryBuilders.kt`에서 아래 블록과 import 2개(`NavKeyYGToggleButton`·`YGToggleButtonPreviewScreen`)를 지운다.

```kotlin
    entry<NavKeyYGToggleButton> {
        ScreenScaffold { modifier ->
            YGToggleButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [x] **Step 5: 잔존 참조 0건 확인**

Run:
```bash
grep -rn "YGToggleButton\|ygtogglebutton" --include="*.kt" core feature app-preview data domain app | grep -v build
```
Expected: 출력 없음.

- [x] **Step 6: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

> 커밋하지 않는다.

---

### Task 8: 전체 검증 + parfait 문서 처리

**Files:**
- Modify: `parfait/specs/2026-07-30-designsystem-button-missing-components.md`
- Modify: `parfait/specs/README.md`
- Modify: `parfait/plans/2026-07-30-designsystem-button-missing-components.md`
- Modify: `parfait/plans/README.md`
- Modify: `parfait/synthesis/open-questions.md`

**Interfaces:**
- Consumes: Task 1~7 전체.
- Produces: 없음.

- [x] **Step 1: 전체 빌드**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug`
Expected: 둘 다 BUILD SUCCESSFUL.

- [x] **Step 2: 전체 ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL(CI `ktlint.yml`과 같은 게이트).

- [x] **Step 3: 갤러리 설치·실행**

Run:
```bash
adb install -r app-preview/build/outputs/apk/debug/app-preview-debug.apk
adb shell monkey -p com.teamyg.parfait.preview -c android.intent.category.LAUNCHER 1
```
`BUTTON` 카테고리에 신규 5종이 보이고 `YGToggleButton`이 사라졌는지 확인한다.

- [x] **Step 4: 5종 육안 대조**

각 화면을 Figma와 나란히 본다. 확인 항목:
- `YGCircleButton` — 3변형 지름이 같고(44) `Small`만 안쪽 원이 작다(28). 아이콘 크기 28/28/18
- `YGEditActionButton` — 어두운 판 위 반투명 채움, 흰 테두리, disabled가 거의 투명
- `YGCameraShutter` — 흰 링 + 어두운 내부 원, 눌렀을 때 내부 원이 더 어두워진다
- `YGEditButton` — 각짐 + 테두리, 선택 시 반전, 아이콘이 텍스트 뒤
- `YGEditTabButton` — 선택 항목만 밑줄, 밑줄 폭이 텍스트 폭과 같고 선택 이동 시 텍스트가 위로 튀지 않는다

pressed·selected는 실제로 눌러 확인한다.

- [x] **Step 5: 스펙·계획 상태 갱신**

- 스펙 `status`를 `in-progress`(develop 미머지) 또는 `implemented`(머지 완료)로 맞추고 상단에 구현 결과 노트를 단다: 통과 항목, 미검증 항목, 계획과 달라진 점.
- 이 계획 `status`를 갱신하고 실행 기록 섹션을 추가한다.
- `parfait/specs/README.md`·`parfait/plans/README.md`의 해당 행을 상태에 맞게 고친다.
- **develop 미머지 상태면 `archive/` 이동과 `architecture/design-system.md` as-built 갱신을 하지 않는다** — parfait 문서는 develop 상태를 기술하므로 먼저 반영하면 드리프트한다(선행 라운드에서 같은 판단을 했다).

- [x] **Step 6: open-questions 갱신**

- `[2026-07-16] YGToggleButton 규약 이탈` — 삭제가 develop에 머지되면 `해소됨`으로 바꾼다. 미머지면 "삭제 구현 완료·미머지"로 메모만 갱신한다.
- `[2026-07-30] Button-Edit-Action … 2dp` — 구현값(내부 원·바깥 프레임)을 확정 기록한다.
- `[2026-07-30] Camera-Shutter … Black5` — 두 원만 그렸다는 사실을 기록한다.
- `[2026-07-30] 신규 버튼군 Colors 미분리` — 구현이 그대로 갔음을 기록한다.

- [x] **Step 7: parfait 커밋 확인 요청**

TJYG-Android는 커밋하지 않는다(Global Constraints). parfait 문서 변경은 목록으로 보고하고 **사용자 승인 후** 커밋한다.

---

## 잔여 작업 (이 계획 밖)

- `feature/camera` 임시 구현체(`ShutterButton`·`FlipCameraButton`·취소 `TextButton`) 치환 — C-101 카메라 화면 라운드
- Phase 1에서 `YGActionItem` 프리뷰·갤러리에 placeholder로 넣은 `ic_plus`를 실제 글리프 `ic_newgroup`으로 교체
- `Button-Stroke`(Figma) 대응 컴포넌트 — 이번 14종 목록 밖
