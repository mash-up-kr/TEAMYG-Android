---
id: designsystem-button-component-sync
title: 디자인시스템 버튼 영역 컴포넌트 Figma 동기화 구현 계획
status: done
type: work-order
created: 2026-07-30
updated: 2026-08-01
platforms: android
owner: TJYG-Android 디자인시스템
related_adr:
  - ADR-0010
related_spec:
  - designsystem-button-component-sync
related_code:
  - YGButton.kt#YGButton
  - YGButtonType.kt#YGButtonType
  - YGButtonColors.kt#YGButtonColors
  - YGIconButtonSize.kt#YGIconButtonSize
  - YGActionItem.kt#YGActionItem
  - YGChipButton.kt#YGChipButton
  - YGChipButtonColorsDefaults.kt#YGChipButtonColorsDefaults
  - YGInputNumber.kt#YGInputNumber
archived_reason: PR #183 develop 머지 완료(2026-08-01) — 드리프트 9건 전량 반영 확인, 스펙 implemented 전환
tags: [plan, parfait, designsystem, figma-sync]
---

# 디자인시스템 버튼 영역 컴포넌트 Figma 동기화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** Figma "버튼" 영역 14종 중 이미 구현체가 있는 7종의 드리프트를 제거한다 — 아이콘 크기 파이프 복구, 테두리 색 경로 복구, 각짐 전환, 값 정정 5건, 칩 프리셋 재명명.

**Architecture:** 공용 결함(`YGButton`이 `iconSize`를 렌더에 안 씀 / `YGButtonColors`에 테두리 색 자리 없음)을 Task 1에서 먼저 닫고, Task 2부터 각 컴포넌트의 값을 Figma 정본에 맞춘다. 순서를 뒤집으면 같은 파일을 두 번 고친다. 컴포넌트 시그니처는 `YGActionItem`(기본값 인자 1개 추가) 하나만 바뀌므로 호출처 파괴가 없고, 칩 프리셋 재명명만 호출처 4파일을 동반한다.

**Tech Stack:** Kotlin, Jetpack Compose, `core:designsystem`(자체 CompositionLocal 테마 `YGTheme` + `YGAtomicColors` + `SizeTokens`), `:app-preview`(컴포넌트 갤러리 앱), Gradle, ktlint.

## 실행 기록 (2026-07-30)

Task 1~6 전량 수행. Task 7은 검증까지 수행하고 문서 처리 일부를 의도적으로 보류했다.

- **통과**: `:core:designsystem`·`:app-preview` `compileDebugKotlin`·`assembleDebug`, repo 전체 `ktlintCheck`, 실기기 갤러리 육안 대조(각짐 3종·`Medium.Secondary` 테두리 2단계·아이콘 크기 파이프·`SIZE_48` 아이콘·칩 높이·`CherrySubtle`/`CherrySolid` 라벨·`YGActionItem` 아이콘 변형·기존 항목 무변화)
- **미검증**: pressed 상태 전반. `adb shell input motionevent DOWN`으로 누른 상태가 Compose `interactionSource`에 반영되지 않아 default와 동일하게 캡처됐다. 손으로 눌러 확인해야 한다
- **IDE 프리뷰 렌더**: 에이전트가 실행할 수 없어 갤러리 실기기 확인으로 대체했다(같은 컴포넌트·같은 변형을 덮는다)
- **보류(계획 Step 5·6·8 수정)**: parfait 문서는 develop 상태를 기술하는데 이번 코드는 `feature/sync-button-component` 브랜치에만 있다(**develop 미머지**). `architecture/design-system.md` as-built 갱신, 스펙 `implemented` 전환·아카이브, 이 계획의 아카이브는 **머지 후**로 옮긴다. 지금 반영하면 문서가 develop보다 앞서 드리프트한다
- **커밋**: 에이전트는 TJYG-Android를 커밋하지 않았다(Global Constraints). 이후 작업자가 직접 `aee2378a`("refactor: 버튼 컴포넌트 현행화")로 커밋했다

## Global Constraints

- **TJYG-Android 저장소에 커밋하지 않는다.** 편집만 하고 커밋·푸시·PR을 만들지 않는다(작업자 지시, 2026-07-30). 현재 브랜치 `feature/sync-button-component`를 그대로 쓴다.
- **테스트를 쓰지 않는다.** `core:designsystem`에 테스트 소스셋이 없고 이 계획에서 신설하지 않는다(스펙 결정). 각 Task의 검증은 컴파일 → ktlint → IDE 프리뷰 → 갤러리 육안 순이다.
- 테마 값은 `YGTheme.colorScheme` / `.typography` / `.shapes` / `.layout`으로 읽고, 크기만 `SizeTokens.SizeN.getDp()`로 직접 읽는다.
- 색은 기존 컴포넌트 관용구대로 `YGAtomicColors`를 직접 참조한다(시맨틱 경유는 이 라운드 범위 밖 — 기존 open-questions 항목).
- 프리뷰는 `@YGPreview` + `PreviewBox` 규약을 쓴다. 신규 프리뷰 함수는 `private`로 만들고, 기존 함수의 가시성은 건드리지 않는다(`YGActionItemPreview`가 `public`이지만 이 라운드의 범위가 아니다).
- 코드·식별자·파일명은 영어(TJYG-Android 저장소 규약).
- 테두리 두께는 `1.dp` 리터럴을 쓴다(`YGChipButton`·`YGInputNumber`와 동일 관용구).
- 작업 대상 저장소 경로는 `wiki/personal-private/project-paths.md`의 `TJYG-Android` 항목.

---

## File Structure

### `core:designsystem` (수정 11)

| 파일 | 책임 | 이 계획에서의 변경 |
|---|---|---|
| `component/ygbutton/YGButtonColors.kt` | 버튼 상태별 색 묶음 | 테두리 3필드 + `borderColor()` |
| `component/ygbutton/YGButton.kt` | 버튼 컴포저블 본체 | `border` 체이닝, 아이콘 `size` 적용 |
| `component/ygbutton/YGButtonType.kt` | 변형별 토큰(패딩·radius·타이포·아이콘·색) | `iconSize`·`radius`·`Medium.Secondary` 테두리·`Transparency` 배경 |
| `component/yginputnumber/YGInputNumber.kt` | 숫자 선택 셀 | `shape` 3곳 |
| `component/ygiconbutton/YGIconButtonSize.kt` | 아이콘 버튼 크기 프리셋 | `SIZE_48` 아이콘 크기 |
| `component/ygchipbutton/YGChipButton.kt` | pill 칩 버튼 | 세로 패딩 |
| `component/ygchipbutton/YGChipButtonColorsDefaults.kt` | 칩 색 프리셋 | 값 교정 + 재명명 + KDoc |
| `component/ygchipbutton/YGChipButtonPreviewData.kt` | 칩 프리뷰 데이터 | 프리셋 이름 |
| `component/ygalert/YGAlert.kt` | 상단 배너 | 프리셋 이름(색 동일) |
| `component/ygtopbar/YGTopBar.kt` | 상단 바 | 프리셋 이름(색 동일) |
| `component/ygactionitem/YGActionItem.kt` | 텍스트 액션 항목 | `iconResource` 신설, `Row` 전환 |

### `:app-preview` (수정 2)

| 파일 | 변경 |
|---|---|
| `screen/component/YGChipButtonPreviewScreen.kt` | 프리셋 이름(`PreviewSection` 라벨 포함) |
| `screen/component/YGActionItemPreviewScreen.kt` | 아이콘 변형 섹션 |

### `parfait` 문서 (수정 4)

`architecture/design-system.md`(as-built) · `specs/2026-07-30-designsystem-button-component-sync.md`(status) · `specs/README.md` · `synthesis/open-questions.md`(칩 패딩 항목 해소).

---

## Tasks

### Task 1: `YGButtonColors` 테두리 경로 + `YGButton` 배선

공용 결함 2건(B1 파이프·B2 경로)을 닫는다. 값은 Task 2에서 채운다 — 이 Task 후 시각 변화는 없다(테두리 기본값이 투명, `iconSize` 현행값이 `Size24`).

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygbutton/YGButtonColors.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygbutton/YGButton.kt`

**Interfaces:**
- Produces: `YGButtonColors`에 `enabledBorderColor`/`disabledBorderColor`/`pressedBorderColor: Color`(기본 `Color.Transparent`) + `fun borderColor(isEnabled: Boolean, isPressed: Boolean): Color`. Task 2가 `Medium.Secondary`에서 이 세 인자를 채운다.
- Consumes: 없음.

- [x] **Step 1: `YGButtonColors`에 테두리 3필드 + `borderColor()` 추가**

`YGButtonColors.kt` 전문을 아래로 바꾼다. 기존 6필드 순서·`foregroundColor`/`backgroundColor` 분기는 그대로 두고 뒤에 덧붙인다.

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygbutton

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGButtonColors(
    val enabledForegroundColor: Color,
    val disabledForegroundColor: Color,
    val pressedForegroundColor: Color,
    val enabledBackgroundColor: Color,
    val disabledBackgroundColor: Color,
    val pressedBackgroundColor: Color,
    val enabledBorderColor: Color = Color.Transparent,
    val disabledBorderColor: Color = Color.Transparent,
    val pressedBorderColor: Color = Color.Transparent,
) {
    fun foregroundColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledForegroundColor
        isPressed -> pressedForegroundColor
        else -> enabledForegroundColor
    }

    fun backgroundColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledBackgroundColor
        isPressed -> pressedBackgroundColor
        else -> enabledBackgroundColor
    }

    fun borderColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledBorderColor
        isPressed -> pressedBorderColor
        else -> enabledBorderColor
    }
}
```

- [x] **Step 2: `YGButton`에 `border` 체이닝**

`YGButton.kt`의 `Row` modifier 체인에서 `.clip(shape = buttonType.radius)` **바로 뒤**, `.clickable(` **앞**에 아래를 끼운다.

```kotlin
            ).border(
                width = 1.dp,
                color = buttonType.colors.borderColor(
                    isEnabled = isEnabled,
                    isPressed = isPressed,
                ),
                shape = buttonType.radius,
            ).clickable(
```

`import androidx.compose.foundation.border`를 추가한다(`androidx.compose.ui.unit.dp`는 이미 있다).

- [x] **Step 3: 두 아이콘 슬롯에 `size` 적용**

`YGButton.kt`의 `startIconResource?.let { … }` 안 `Image`에 `modifier`를 추가한다.

```kotlin
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = buttonType.colors.foregroundColor(
                        isEnabled = isEnabled,
                        isPressed = isPressed,
                    ),
                ),
                modifier = Modifier.size(buttonType.iconSize),
            )
```

`endIconResource?.let { … }` 안 `Image`에도 같은 `modifier` 줄을 추가한다. `import androidx.compose.foundation.layout.size`를 추가한다.

- [x] **Step 4: 컴파일 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. 기존 `YGButtonType` 변형 4개는 `YGButtonColors(...)`를 명명 인자로 호출하고 신규 3필드에 기본값이 있어 수정 없이 통과해야 한다. 실패하면 기본값 누락을 먼저 확인한다.

- [x] **Step 5: ktlint 확인**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew :core:designsystem:ktlintMainSourceSetFormat` 후 재확인한다.

- [x] **Step 6: 프리뷰로 무변화 확인**

IDE에서 `YGButton.kt`의 `YGButtonPreview`를 렌더한다.
Expected: 이 Task 전과 **똑같이** 보인다(테두리 안 보임, 아이콘 크기 그대로). 달라 보이면 Step 2의 `border` 위치나 Step 3의 `iconSize` 참조를 확인한다.

> 커밋하지 않는다(Global Constraints).

---

### Task 2: `YGButtonType` 값 정정 — 아이콘 크기·테두리 색·각짐·Transparency 배경

B1 값 + B2 값 + R1 + V4를 한 파일에서 처리한다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygbutton/YGButtonType.kt`

**Interfaces:**
- Consumes: Task 1이 만든 `YGButtonColors`의 `enabledBorderColor`/`disabledBorderColor`/`pressedBorderColor` + `YGButton`의 `border`·`size` 배선.
- Produces: 없음(내부 값 변경).

- [x] **Step 1: `Medium.Primary`·`Medium.Secondary`·`Medium.Transparency`·`Large`의 `radius`를 각짐으로**

네 변형의 `radius` getter를 바꾼다. `SmallSquare`는 이미 `none`이므로 건드리지 않는다.

```kotlin
        override val radius: Shape
            @Composable
            get() = YGTheme.shapes.radius.none
```

- [x] **Step 2: 같은 네 변형의 `iconSize`를 `Size20`으로**

```kotlin
        override val iconSize: Dp
            get() = SizeTokens.Size20.getDp()
```

`SmallSquare`의 `iconSize`는 `SizeTokens.Size24` 그대로 둔다(Figma `Button-SmallSquare` 아이콘과 일치).

- [x] **Step 3: `Medium.Secondary`에 테두리 색 3개 지정**

`Medium.Secondary`의 `colors` getter를 아래로 바꾼다.

```kotlin
            override val colors: YGButtonColors
                @Composable
                get() = YGButtonColors(
                    enabledForegroundColor = YGAtomicColors.Gray.Gray900,
                    disabledForegroundColor = YGAtomicColors.Gray.Gray500,
                    pressedForegroundColor = YGAtomicColors.Gray.Gray900,
                    enabledBackgroundColor = YGAtomicColors.Gray.Gray100,
                    disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                    pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                    enabledBorderColor = YGAtomicColors.Gray.Gray500,
                    disabledBorderColor = YGAtomicColors.Gray.Gray300,
                    pressedBorderColor = YGAtomicColors.Gray.Gray500,
                )
```

다른 세 변형의 `colors`에는 테두리 인자를 추가하지 않는다(기본값 투명).

- [x] **Step 4: `Medium.Transparency` 배경을 `Transparency.White50` 토큰으로**

`Medium.Transparency`의 `colors` getter에서 배경 3개를 바꾼다. pressed만 현행 리터럴 유지(Figma가 변수 미바인딩 — open-questions 등록됨).

```kotlin
                    enabledBackgroundColor = YGAtomicColors.Transparency.White50,
                    disabledBackgroundColor = YGAtomicColors.Transparency.White50,
                    pressedBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.9f),
```

- [x] **Step 5: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL.

- [x] **Step 6: 프리뷰 육안 확인**

IDE에서 `YGButtonPreview`를 4변형 모두 렌더한다.
Expected:
- `Medium.*`·`Large`가 pill이 아니라 **직각**
- `Medium.Secondary`에 회색 테두리가 보이고, disabled에서 테두리가 더 밝다
- 아이콘 있는 프리뷰(`Button Start`·`Button End`)의 아이콘이 이전보다 작다

- [x] **Step 7: 각짐 전파 지점 확인**

IDE에서 `YGModalPopup.kt`의 프리뷰를 렌더한다.
Expected: 하단 두 버튼(`Medium.Secondary` 좌 / `Medium.Primary` 우)이 직각이고 좌측에만 테두리가 있다.

> 커밋하지 않는다.

---

### Task 3: `YGInputNumber` 각짐

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yginputnumber/YGInputNumber.kt`

**Interfaces:**
- Consumes: 없음. Produces: 없음.

- [x] **Step 1: `shape` 3곳을 `radius.none`으로**

`background`·`clip`·`border`가 각각 `YGTheme.shapes.radius.xSmall`을 참조한다. 세 곳 모두 바꾼다 — 한 곳만 바꾸면 채움과 테두리 모양이 어긋난다.

```kotlin
            .background(
                color = when {
                    isSelected -> YGAtomicColors.Gray.Gray900
                    else -> YGAtomicColors.Gray.White
                },
                shape = YGTheme.shapes.radius.none,
            ).clip(
                shape = YGTheme.shapes.radius.none,
            ).clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.Gray100,
                shape = YGTheme.shapes.radius.none,
            ).semantics { role = Role.Button },
```

- [x] **Step 2: `xSmall` 잔존 참조 없는지 확인**

Run: `grep -n "xSmall" core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yginputnumber/YGInputNumber.kt`
Expected: 출력 없음.

- [x] **Step 3: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL.

- [x] **Step 4: 프리뷰 확인**

IDE에서 `YGInputNumberPreview`를 렌더한다.
Expected: selected/default 둘 다 모서리가 직각.

> 커밋하지 않는다.

---

### Task 4: `YGIconButtonSize.SIZE_48` 아이콘 크기 교정

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygiconbutton/YGIconButtonSize.kt`

**Interfaces:**
- Consumes: 없음. Produces: 없음(enum 값만 변경).

- [x] **Step 1: `SIZE_48`의 `iconSize` 교정**

```kotlin
@Immutable
enum class YGIconButtonSize(val containerSize: Dp, val iconSize: Dp) {
    SIZE_44(containerSize = 44.dp, iconSize = 24.dp),
    SIZE_48(containerSize = 48.dp, iconSize = 32.dp),
}
```

`SIZE_44`는 Figma `Button-Icon` `Size=44`와 일치하므로 그대로 둔다.

- [x] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL.

- [x] **Step 3: 파급 지점 프리뷰 확인**

`SIZE_48`을 쓰는 곳이 있는지 먼저 확인한다.

Run: `grep -rn "SIZE_48" --include="*.kt" core feature app-preview | grep -v build`

찾은 각 지점의 프리뷰(없으면 `YGIconButton.kt`의 `YGIconButtonPreview`)를 IDE에서 렌더한다.
Expected: 48 컨테이너 안 아이콘이 이전보다 커졌고 컨테이너 밖으로 넘치지 않는다.

> 커밋하지 않는다.

---

### Task 5: `YGChipButton` 세로 패딩 + 프리셋 값 교정·재명명

V2·V3을 처리하고, 이름 변경에 딸린 호출처 4파일을 같은 Task에서 고친다 — 쪼개면 중간 상태가 컴파일되지 않는다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygchipbutton/YGChipButton.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygchipbutton/YGChipButtonColorsDefaults.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygchipbutton/YGChipButtonPreviewData.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygalert/YGAlert.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtopbar/YGTopBar.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGChipButtonPreviewScreen.kt`

**Interfaces:**
- Produces: `YGChipButtonColorsDefaults.CherrySubtle`(Figma `Button-Chip-Left`)·`CherrySolid`(Figma `Button-Chip-Right`). 구 `CherryBorderPressed`·`CherryBackgroundPressed`는 사라진다.
- Consumes: 없음.

- [x] **Step 1: 세로 패딩을 `padding2`로**

`YGChipButton.kt`의 `padding(` 블록에서 `top`·`bottom`만 바꾼다. 가로 비대칭 로직은 Figma와 일치하므로 그대로 둔다.

```kotlin
            .padding(
                top = YGTheme.layout.padding.padding2,
                end = if (endIconResource != null) YGTheme.layout.padding.padding3 else YGTheme.layout.padding.padding5,
                bottom = YGTheme.layout.padding.padding2,
                start = if (startIconResource !=
                    null
                ) {
                    YGTheme.layout.padding.padding3
                } else {
                    YGTheme.layout.padding.padding5
                },
            ),
```

- [x] **Step 2: 프리셋 값 교정 + 재명명 + KDoc**

`YGChipButtonColorsDefaults.kt` 전문을 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGChipButtonColorsDefaults {
    /** Figma `Button-Chip-Left` — 옅은 채움, 눌리면 한 단계 진해진다. 테두리 없음. */
    val CherrySubtle: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray600,
        pressedForegroundColor = YGAtomicColors.Gray.Gray700,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry50,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry100,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Gray.Transparent,
    )

    /** Figma `Button-Chip-Right` — 진한 채움, 전경색 고정. 테두리 없음. */
    val CherrySolid: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray950,
        pressedForegroundColor = YGAtomicColors.Gray.Gray950,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry100,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry200,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Gray.Transparent,
    )
}
```

`CherrySubtle`에서 바뀐 것은 `pressedBackgroundColor`(`Cherry50` → `Cherry100`)와 `pressedBorderColor`(`Cherry100` → `Transparent`) 두 개다. `CherrySolid`는 이름만 바뀌고 값은 구 `CherryBackgroundPressed`와 같다.

- [x] **Step 3: 호출처 이름 일괄 교체**

Run:
```bash
grep -rln "CherryBorderPressed\|CherryBackgroundPressed" --include="*.kt" core feature app-preview | grep -v build
```
Expected: `YGChipButtonPreviewData.kt`, `YGAlert.kt`, `YGTopBar.kt`, `YGChipButtonPreviewScreen.kt` 4개(방금 고친 `YGChipButtonColorsDefaults.kt`는 목록에서 빠져야 한다).

각 파일에서 `CherryBorderPressed` → `CherrySubtle`, `CherryBackgroundPressed` → `CherrySolid`로 바꾼다.
`YGChipButtonPreviewScreen.kt`는 `PreviewSection("CherryBorderPressed")`·`PreviewSection("CherryBackgroundPressed")` **라벨 문자열도** 새 이름으로 바꾼다.

- [x] **Step 4: 구 이름 잔존 확인**

Run:
```bash
grep -rn "CherryBorderPressed\|CherryBackgroundPressed" --include="*.kt" core feature app-preview | grep -v build
```
Expected: 출력 없음.

- [x] **Step 5: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

- [x] **Step 6: 프리뷰 확인**

IDE에서 `YGChipButton.kt`의 `YGChipButtonPreview`, `YGAlert.kt`의 프리뷰, `YGTopBar.kt`의 프리뷰를 렌더한다.
Expected: 칩 높이가 낮아졌고, `YGAlert`·`YGTopBar` 안 칩도 함께 낮아졌다. 배너·상단 바 레이아웃이 깨지지 않았다.

> 커밋하지 않는다.

---

### Task 6: `YGActionItem` 아이콘 변형 신설

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygactionitem/YGActionItem.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGActionItemPreviewScreen.kt`

**Interfaces:**
- Produces: `YGActionItem(text, onClick, modifier, iconResource: Int? = null, interactionSource)` — `iconResource`는 `@DrawableRes`, 기본값 `null`.
- Consumes: 없음.

- [x] **Step 1: `YGActionItem`을 `Row`로 바꾸고 아이콘 슬롯 추가**

컴포저블 본체를 아래로 바꾼다. 패딩·타이포·색은 Figma와 이미 일치하므로 값을 유지한다.

```kotlin
@Composable
fun YGActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val contentColor = if (isPressed) YGAtomicColors.Gray.Gray700 else YGAtomicColors.Gray.Gray500

    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
            .semantics { role = Role.Button }
            .padding(
                vertical = YGTheme.layout.padding.padding5,
                horizontal = YGTheme.layout.padding.padding6,
            ),
    ) {
        iconResource?.let { resource ->
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = contentColor),
                modifier = Modifier.size(SizeTokens.Size24.getDp()),
            )
        }
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = contentColor,
        )
    }
}
```

import를 추가한다: `androidx.annotation.DrawableRes`, `androidx.compose.foundation.Image`, `androidx.compose.foundation.layout.Arrangement`, `androidx.compose.foundation.layout.Row`, `androidx.compose.foundation.layout.size`, `androidx.compose.ui.Alignment`, `androidx.compose.ui.graphics.ColorFilter`, `androidx.compose.ui.res.painterResource`, `com.teamyg.parfait.core.designsystem.theme.size.SizeTokens`.
`androidx.compose.foundation.layout.Box`는 프리뷰가 계속 쓰므로 남긴다.

- [x] **Step 2: 프리뷰에 아이콘 변형 추가**

같은 파일의 `YGActionItemPreview`를 아이콘 유/무 2개가 보이게 바꾼다.

```kotlin
@YGPreview
@Composable
fun YGActionItemPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        YGActionItem(
            text = "그룹 나가기",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGActionItem(
            text = "새 그룹 만들기",
            onClick = {},
            iconResource = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

import를 추가한다: `androidx.compose.foundation.layout.Column`, `com.teamyg.parfait.core.designsystem.R`. 쓰지 않게 된 `Box` import는 지운다.

> `ic_plus`는 `core:designsystem`의 `res/drawable`에 있는 것이 확인된 리소스다(`YGTopBar`가 placeholder로 쓴다). 아이콘 글리프가 Figma `Action-Item`의 `Ic_Newgroup`과 다르지만, 이 Task는 슬롯 배선 확인이 목적이고 실제 아이콘은 사용처가 주입한다.

- [x] **Step 3: 갤러리 화면에 아이콘 섹션 추가**

`YGActionItemPreviewScreen.kt`의 `LazyColumn` 안, 기존 `item { PreviewSection("another action") { … } }` **뒤**에 아래 블록을 추가한다.

```kotlin
            item {
                PreviewSection("show icon") {
                    YGActionItem(
                        text = "새 그룹 만들기",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                    )
                }
            }
```

`import com.teamyg.parfait.core.designsystem.R`를 추가한다. 기존 두 섹션(`"action item"`·`"another action"`)은 그대로 둔다 — 아이콘 없는 렌더가 회귀하지 않았는지 나란히 보는 용도다.

- [x] **Step 4: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck :app-preview:ktlintMainSourceSetCheck`
Expected: 전부 BUILD SUCCESSFUL.

- [x] **Step 5: 기존 호출처 무영향 확인**

Run: `grep -rn "YGActionItem(" --include="*.kt" core feature app-preview | grep -v build`
Expected: `YGDangerZone` 등 기존 호출이 `iconResource` 없이 그대로 있고, Step 4가 통과했으므로 기본값으로 컴파일된다.

- [x] **Step 6: 프리뷰 확인**

IDE에서 `YGActionItemPreview`와 `YGDangerZone.kt` 프리뷰를 렌더한다.
Expected: 아이콘 없는 항목은 이전과 동일(텍스트 위치 변화 없음), 아이콘 있는 항목은 텍스트 앞에 아이콘 + 좁은 간격. `YGDangerZone` 안 항목은 변화 없다.

> 커밋하지 않는다.

---

### Task 7: 전체 검증 + parfait 문서 갱신

**Files:**
- Modify: `parfait/architecture/design-system.md`
- Modify: `parfait/specs/2026-07-30-designsystem-button-component-sync.md`
- Modify: `parfait/specs/README.md`
- Modify: `parfait/synthesis/open-questions.md`

**Interfaces:**
- Consumes: Task 1~6 전체.
- Produces: 없음.

- [x] **Step 1: 전체 빌드**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug`
Expected: 둘 다 BUILD SUCCESSFUL.

- [x] **Step 2: 전체 ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. CI(`.github/workflows/ktlint.yml`)와 같은 게이트다.

- [x] **Step 3: 각짐 전파 화면 프리뷰 확인**

IDE에서 아래 프리뷰를 렌더한다: `TermAgreeScreen`·`GroupCreateScreen`·`GroupInviteCodeScreen`·`GroupNickNameScreen`(모두 `YGButtonType.Large`), `YGModalPopup`(`Medium` 2종).
Expected: 하단 버튼이 pill → 직각으로 바뀌었고 레이아웃이 깨지지 않았다.

- [x] **Step 4: 갤러리 실행 육안 대조**

`:app-preview`를 기기/에뮬레이터에 올리고 `YGButton`·`YGIconButton`·`YGChipButton`·`YGActionItem`·`YGInputNumber` 화면을 Figma와 나란히 본다. **pressed 상태는 여기서만 확인 가능**하다 — 각 버튼을 눌러 배경·전경·테두리 변화를 확인한다.
Expected 체크리스트:
- `YGButton` `Medium.Secondary`: 눌러도 테두리가 유지되고 배경만 진해진다
- `YGButton` `Medium.Transparency`: 눌렀을 때 더 불투명해진다
- `YGChipButton` `CherrySubtle`: 눌렀을 때 배경이 진해지고 테두리는 나타나지 않는다
- `YGChipButton` `CherrySolid`: 눌렀을 때 배경만 진해진다
- `YGActionItem`: 눌렀을 때 텍스트와 아이콘이 함께 진해진다
- `YGIconButton` `SIZE_48`: 아이콘이 컨테이너에 알맞게 크다

- [x] **Step 5: `architecture/design-system.md` as-built 갱신**

parfait 저장소에서 아래를 반영한다.
- 컴포넌트 작성 규약의 `YGButtonType` 설명에서 "`#140`에서 `borderColor` 제거" 뒤에 테두리 3상태 복원(이번 라운드)을 덧붙인다
- 컴포넌트 인벤토리 `YGChipButton` 행의 프리셋 이름을 `CherrySubtle`·`CherrySolid`로 바꾼다
- `YGButton` 설명에 아이콘 크기가 `YGButtonType.iconSize`로 적용된다는 사실을 적는다(이전엔 死필드였음)
- `YGInputNumber` 행에 각짐(`radius.none`)을 반영한다

수치·hex는 적지 않는다(parfait 규칙).

- [x] **Step 6: 스펙 status 갱신 + 아카이브 이동**

- `parfait/specs/2026-07-30-designsystem-button-component-sync.md`의 `status`를 `implemented`, `verified`를 검증 수행일로 바꾸고 상단에 구현 완료 노트를 단다(코드=설계 일치 여부, 계획과 달라진 점).
- 파일을 `parfait/specs/archive/`로 옮긴다.
- `parfait/specs/README.md`에서 해당 행을 활성 표에서 아카이브 표로 옮기고 링크 경로를 `archive/`로 고친다.

- [x] **Step 7: open-questions 칩 패딩 항목 해소 처리**

`parfait/synthesis/open-questions.md`의 `[2026-07-27] YGChipButton 세로 패딩 Figma 불일치` 항목 `상태`를 `해소됨`으로 바꾸고, 해소 메모에 이 라운드에서 `padding2`로 내렸고 `YGAlert`·`YGTopBar` 높이 변화를 갤러리에서 확인했다는 사실을 적는다.

- [x] **Step 8: 계획 문서 아카이브**

이 계획 파일의 `status`를 `done`, `archived_reason`을 채우고 `parfait/plans/archive/`로 옮긴 뒤 `parfait/plans/README.md`의 활성 행을 아카이브 표로 옮긴다.

- [x] **Step 9: parfait 저장소 커밋 확인 요청**

TJYG-Android는 커밋하지 않는다(Global Constraints). parfait 문서 변경은 커밋 대상이지만 **사용자 확인 후**에만 커밋한다(저장소 규약). 무엇을 커밋할지 목록으로 보고하고 승인을 받는다.

---

## 잔여 작업 (이 계획 밖)

후속 스펙 `designsystem-button-missing-components`가 다룬다: `Button-Edit-Tab`·`Button-Edit`·`Button-Circle`(3타입)·`Button-Edit-Action` 신설, `Camera-Shutter`를 `core:designsystem`으로 이관(`feature/camera`의 `ShutterButton` 삭제 + `CameraControlComponent` 치환), `YGToggleButton` 삭제 및 `:app-preview` 카탈로그·`NavKey`·프리뷰 화면 정리.
