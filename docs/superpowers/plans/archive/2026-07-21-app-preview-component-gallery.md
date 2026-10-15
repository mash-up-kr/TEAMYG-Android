---
id: app-preview-component-gallery
title: 컴포넌트 프리뷰 갤러리 앱 (app-preview) Implementation Plan
status: done
type: work-order
created: 2026-07-21
updated: 2026-07-22
archived_reason: PR #163 develop 머지 완료(2026-07-22) — 카탈로그·NavKey17·showcase 17·DI 배선 코드 반영, 구조=설계 일치
platforms: android
owner: TJYG-Android 클라이언트
related_adr:
related_spec: app-preview-component-gallery
related_code:
  - MainScreen.kt
  - MainRoute.kt
  - MainEntryBuilder.kt
  - MainEntryModule.kt
  - NavKeyMain.kt#NavKeyMain
  - RootRoute.kt#RootRoute
tags: [plan, parfait, app-preview, designsystem]
---

# 컴포넌트 프리뷰 갤러리 앱 (app-preview) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `:app-preview` 앱을, 메인에서 컴포넌트명 버튼을 카테고리로 묶어 보여주고 탭하면 해당 `YG*` 컴포넌트의 전 변형을 실기기에서 렌더·조작하는 컴포넌트 갤러리로 확장한다.

**Architecture:** 기존 Navigation3 + Hilt multibinding 패턴(NavKey → EntryBuilder → `@IntoSet @Provides`) 그대로 확장. 컴포넌트별 NavKey(1파일에 모음) + 컴포넌트별 showcase 화면 + 카탈로그 데이터 모델(메인 목록 소스) + 공용 `PreviewSection` 헬퍼. 화면 배선은 `ComponentEntryModule`이 `RootRoute`의 entryBuilder set에 합류시켜 기존 `NavDisplay`가 자동 렌더.

**Tech Stack:** Kotlin, Jetpack Compose, androidx.navigation3, Hilt, `:core:designsystem`(YG* 컴포넌트 + YGTheme), `:core:navigation`(Navigator).

## Global Constraints

- **작업 대상 repo**: `TJYG-Android` (로컬 경로는 private submodule `wiki/personal-private/project-paths.md`). 모든 경로·gradle 명령은 그 repo 루트 기준.
- **모듈**: `:app-preview`. 패키지 루트 `com.teamyg.parfait.preview`. 소스 루트 `app-preview/src/main/kotlin/com/teamyg/parfait/preview/`.
- **의존 한계**: `:app-preview`는 `core:designsystem`/`core:navigation`/`core:ui`/`core:util:*`만 의존. **feature/data/domain 의존 추가 금지.**
- **테마 필수**: 최상위는 이미 `YGCustomTheme { RootRoute(...) }`로 래핑됨(MainActivity). 컴포넌트가 `YGTheme.*` 접근하려면 이 provider 필수 — 별도 래핑 불필요(entry는 그 안에서 렌더됨).
- **테스트 없음**: `:app-preview`는 실기기 육안 확인용 샌드박스. 유닛/스크린샷 테스트 작성 안 함. 검증 = **컴파일 성공**(`./gradlew :app-preview:compileDebugKotlin`) + 실기기 육안.
- **Navigator API**(정확): 이동 `navigator.goTo(navKey)`, 뒤로 `navigator.onBack()`. (navigate/push/back/pop 아님)
- **디자인시스템 정확 사실**(추측 금지):
  - `YGButtonType`: `SmallSquare` / `Medium.Primary` / `Medium.Secondary` / `Medium.Transparency` / `Large`. `Medium` 단독은 타입 아님.
  - ChipButton 색 프리셋 object명은 `YGChipButtonColorsDefaults` (멤버 2개: `CherryBorderPressed`, `CherryBackgroundPressed`). `YGChipButtonColors.Defaults` 없음.
  - `YGIconButtonSize.SIZE_44` / `SIZE_48`. `YGInviteCardStatus.Active` / `Invalid`.
  - drawable 접근: `com.teamyg.parfait.core.designsystem.R.drawable.ic_*` (실존: `ic_plus`, `ic_copy`, `ic_caret_left`, `ic_caret_right`, `ic_close_round`, `ic_warning_round`, `ic_hamburger` 등).
  - `YGLabel(text, modifier)` — 보조 라벨(회색). 카테고리/변형 소제목에 사용.
- **커밋 규칙**: 이 계획의 커밋 단계 실행 전 사용자 확인(프로젝트 Git 규칙). `main`/`develop` 직접 커밋 금지 — 브랜치에서 작업.
- **프리뷰 관용구**: MainScreen·PreviewSection·17개 showcase 화면 각 파일 하단에 `@YGPreview`+`PreviewBox`(FQN `...core.designsystem.utils.preview.YGPreview`/`PreviewBox`) 프리뷰 함수를 붙인다. showcase는 `private fun Preview<화면명>() = PreviewBox { <화면명>(onBack = {}) }` 형태.
- **컴포넌트 호출 스타일**: 인자 2개 이상인 `YG*` 호출은 각 인자를 개별 라인으로 펼치고 trailing comma. 1개면 단일 라인.

## File Structure

`app-preview/src/main/kotlin/com/teamyg/parfait/preview/` 이하:

| 파일 | 신규/개조 | 책임 |
|---|---|---|
| `model/ComponentCatalog.kt` | 신규 | `ComponentCategory` enum + `ComponentEntry` + `componentCatalog` 목록 |
| `navigation/key/NavKeyYG*.kt` (17개) | 신규 | 컴포넌트별 `@Serializable data object` NavKey, 파일당 1개(기존 `NavKeyMain.kt` 패턴) |
| `screen/component/PreviewSection.kt` | 신규 | 변형 소제목 + 여백 공용 래퍼 (+자체 `@YGPreview`) |
| `screen/component/YGButtonPreviewScreen.kt` 외 16개 | 신규 | 컴포넌트별 showcase 화면 (각자 하단에 `@YGPreview` 프리뷰 함수) |
| `navigation/entry/ComponentEntryBuilders.kt` | 신규 | 컴포넌트별 `entry<NavKeyXxx>{ Scaffold { XxxPreviewScreen(...) } }` |
| `navigation/di/ComponentEntryModule.kt` | 신규 | `@IntoSet @Provides`로 entry builder 등록 |
| `screen/MainScreen.kt` | 개조 | 플레이스홀더 → 카테고리 그룹 버튼 목록 |
| `route/MainRoute.kt` | 개조 | `navigator::goTo`를 MainScreen에 전달 |

---

## Task 1: 카탈로그 데이터 모델 + 컴포넌트 NavKey 17개

**Files:**
- Create: `app-preview/.../navigation/key/NavKeyYGButton.kt` … `NavKeyYGTopBar.kt` (17개, 파일당 1개)
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`

**Interfaces:**
- Produces: `ComponentCategory`(enum: BUTTON/INPUT/TEXT/CONTAINER/BAR, each `val label: String`), `ComponentEntry(category, label, navKey)`, `val componentCatalog: List<ComponentEntry>`, 그리고 17개 `NavKeyYG*` object. 이후 모든 task가 이 심볼들을 참조.

- [ ] **Step 1: NavKey 17개 작성 (파일당 1개, 기존 `NavKeyMain.kt` 패턴)**

`navigation/key/NavKeyYGButton.kt` (나머지 16개도 이름만 바꿔 동형으로):
```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGButton : NavKey
```
나머지 NavKey(파일당 1개): `NavKeyYGChipButton`, `NavKeyYGToggleButton`, `NavKeyYGIconButton`, `NavKeyYGDateButton`, `NavKeyYGInputNumber`, `NavKeyYGTextField`, `NavKeyYGTextFormField`, `NavKeyYGLabel`, `NavKeyYGDate`, `NavKeyYGActionItem`, `NavKeyYGModalPopup`, `NavKeyYGInviteCard`, `NavKeyYGDangerZone`, `NavKeyYGListItem`, `NavKeyYGHorizontalDivider`, `NavKeyYGTopBar`.

- [ ] **Step 2: 카탈로그 데이터 모델 작성**

`model/ComponentCatalog.kt`:
```kotlin
package com.teamyg.parfait.preview.model

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.preview.navigation.key.NavKeyYGActionItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGChipButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDangerZone
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDate
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDateButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGHorizontalDivider
import com.teamyg.parfait.preview.navigation.key.NavKeyYGIconButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInputNumber
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInviteCard
import com.teamyg.parfait.preview.navigation.key.NavKeyYGLabel
import com.teamyg.parfait.preview.navigation.key.NavKeyYGListItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGModalPopup
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextFormField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToggleButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTopBar

enum class ComponentCategory(val label: String) {
    BUTTON("Button"),
    INPUT("Input"),
    TEXT("Text"),
    CONTAINER("Container"),
    BAR("Bar"),
}

data class ComponentEntry(
    val category: ComponentCategory,
    val label: String,
    val navKey: NavKey,
)

val componentCatalog: List<ComponentEntry> = listOf(
    ComponentEntry(ComponentCategory.BUTTON, "YGButton", NavKeyYGButton),
    ComponentEntry(ComponentCategory.BUTTON, "YGChipButton", NavKeyYGChipButton),
    ComponentEntry(ComponentCategory.BUTTON, "YGToggleButton", NavKeyYGToggleButton),
    ComponentEntry(ComponentCategory.BUTTON, "YGIconButton", NavKeyYGIconButton),
    ComponentEntry(ComponentCategory.BUTTON, "YGDateButton", NavKeyYGDateButton),
    ComponentEntry(ComponentCategory.BUTTON, "YGInputNumber", NavKeyYGInputNumber),
    ComponentEntry(ComponentCategory.INPUT, "YGTextField", NavKeyYGTextField),
    ComponentEntry(ComponentCategory.INPUT, "YGTextFormField", NavKeyYGTextFormField),
    ComponentEntry(ComponentCategory.TEXT, "YGLabel", NavKeyYGLabel),
    ComponentEntry(ComponentCategory.TEXT, "YGDate", NavKeyYGDate),
    ComponentEntry(ComponentCategory.TEXT, "YGActionItem", NavKeyYGActionItem),
    ComponentEntry(ComponentCategory.CONTAINER, "YGModalPopup", NavKeyYGModalPopup),
    ComponentEntry(ComponentCategory.CONTAINER, "YGInviteCard", NavKeyYGInviteCard),
    ComponentEntry(ComponentCategory.CONTAINER, "YGDangerZone", NavKeyYGDangerZone),
    ComponentEntry(ComponentCategory.CONTAINER, "YGListItem", NavKeyYGListItem),
    ComponentEntry(ComponentCategory.CONTAINER, "YGHorizontalDivider", NavKeyYGHorizontalDivider),
    ComponentEntry(ComponentCategory.BAR, "YGTopBar", NavKeyYGTopBar),
)
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (신규 파일 컴파일 통과, 아직 미사용이라 경고만 가능).

- [ ] **Step 4: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYG*.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt
git commit -m "feat(app-preview): 컴포넌트 갤러리 카탈로그 모델·NavKey 17개 추가"
```

---

## Task 2: PreviewSection 공용 헬퍼

**Files:**
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/PreviewSection.kt`

**Interfaces:**
- Produces: `@Composable fun PreviewSection(title: String, content: @Composable () -> Unit)`. 이후 모든 컴포넌트 화면이 변형 래퍼로 사용.

- [ ] **Step 1: 헬퍼 작성**

`screen/component/PreviewSection.kt`:
```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel

@Composable
fun PreviewSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YGLabel(text = title)
        content()
    }
}
```

- [ ] **Step 2: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/PreviewSection.kt
git commit -m "feat(app-preview): 컴포넌트 변형 래퍼 PreviewSection 추가"
```

---

## Task 3: Button 카테고리 화면 6개

**Files:**
- Create: `screen/component/YGButtonPreviewScreen.kt`, `YGChipButtonPreviewScreen.kt`, `YGToggleButtonPreviewScreen.kt`, `YGIconButtonPreviewScreen.kt`, `YGDateButtonPreviewScreen.kt`, `YGInputNumberPreviewScreen.kt` (모두 `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/`)

**Interfaces:**
- Consumes: `PreviewSection`(Task 2).
- Produces: `@Composable internal fun YGButtonPreviewScreen(onBack: () -> Unit, modifier: Modifier = Modifier)` 및 나머지 5개 동형 시그니처. Task 9(EntryBuilders)가 호출.

공통 화면 골격(모든 화면 동일): `Column(modifier) { YGTopBarBack(onIconClick=onBack); LazyColumn(Modifier.weight(1f), contentPadding=16dp, spacedBy=8dp) { item { PreviewSection(...) } ... } }`.

- [ ] **Step 1: YGButtonPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGButtonPreviewScreen(
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
                PreviewSection("SmallSquare") {
                    YGButton(text = "버튼", buttonType = YGButtonType.SmallSquare, isEnabled = true, onClick = {})
                }
            }
            item {
                PreviewSection("Medium.Primary (enabled / disabled)") {
                    YGButton(text = "확인", buttonType = YGButtonType.Medium.Primary, isEnabled = true, onClick = {})
                    YGButton(text = "확인", buttonType = YGButtonType.Medium.Primary, isEnabled = false, onClick = {})
                }
            }
            item {
                PreviewSection("Medium.Secondary (enabled / disabled)") {
                    YGButton(text = "취소", buttonType = YGButtonType.Medium.Secondary, isEnabled = true, onClick = {})
                    YGButton(text = "취소", buttonType = YGButtonType.Medium.Secondary, isEnabled = false, onClick = {})
                }
            }
            item {
                PreviewSection("Medium.Transparency") {
                    YGButton(text = "투명", buttonType = YGButtonType.Medium.Transparency, isEnabled = true, onClick = {})
                }
            }
            item {
                PreviewSection("Large (enabled / disabled)") {
                    YGButton(text = "다음", buttonType = YGButtonType.Large, isEnabled = true, onClick = {})
                    YGButton(text = "다음", buttonType = YGButtonType.Large, isEnabled = false, onClick = {})
                }
            }
            item {
                PreviewSection("with icons (start / end)") {
                    YGButton(
                        text = "아이콘",
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = {},
                        startIconResource = R.drawable.ic_plus,
                    )
                    YGButton(
                        text = "복사",
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = {},
                        endIconResource = R.drawable.ic_copy,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: YGChipButtonPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGChipButtonPreviewScreen(
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
                PreviewSection("CherryBorderPressed") {
                    YGChipButton(text = "칩", colors = YGChipButtonColorsDefaults.CherryBorderPressed, onClick = {})
                }
            }
            item {
                PreviewSection("CherryBackgroundPressed") {
                    YGChipButton(text = "칩", colors = YGChipButtonColorsDefaults.CherryBackgroundPressed, onClick = {})
                }
            }
            item {
                PreviewSection("with start icon") {
                    YGChipButton(
                        text = "추가",
                        colors = YGChipButtonColorsDefaults.CherryBackgroundPressed,
                        onClick = {},
                        startIconResource = R.drawable.ic_plus,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: YGToggleButtonPreviewScreen 작성** (remember 상태로 토글 인터랙션)

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygtogglebutton.YGToggleButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGToggleButtonPreviewScreen(
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
                PreviewSection("interactive (tap to toggle)") {
                    YGToggleButton(text = "토글", isSelected = selected, onClick = { selected = !selected })
                }
            }
            item {
                PreviewSection("selected = false / true (static)") {
                    YGToggleButton(text = "토글", isSelected = false, onClick = {})
                    YGToggleButton(text = "토글", isSelected = true, onClick = {})
                }
            }
            item {
                var selected by remember { mutableStateOf(true) }
                PreviewSection("with icon") {
                    YGToggleButton(
                        text = "토글",
                        isSelected = selected,
                        onClick = { selected = !selected },
                        iconResource = R.drawable.ic_plus,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: YGIconButtonPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGIconButtonPreviewScreen(
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
                PreviewSection("SIZE_44 (enabled / disabled)") {
                    YGIconButton(iconResource = R.drawable.ic_close_round, size = YGIconButtonSize.SIZE_44, contentDescription = "닫기", onClick = {})
                    YGIconButton(iconResource = R.drawable.ic_close_round, size = YGIconButtonSize.SIZE_44, contentDescription = "닫기", onClick = {}, isEnabled = false)
                }
            }
            item {
                PreviewSection("SIZE_48 (enabled / disabled)") {
                    YGIconButton(iconResource = R.drawable.ic_caret_right, size = YGIconButtonSize.SIZE_48, contentDescription = "다음", onClick = {})
                    YGIconButton(iconResource = R.drawable.ic_caret_right, size = YGIconButtonSize.SIZE_48, contentDescription = "다음", onClick = {}, isEnabled = false)
                }
            }
        }
    }
}
```

- [ ] **Step 5: YGDateButtonPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.component.ygdatebutton.YGDateButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGDateButtonPreviewScreen(
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
                PreviewSection("default") {
                    YGDateButton(text = "15", isSelected = false, isToday = false, isEnabled = true, onClick = {})
                }
            }
            item {
                PreviewSection("selected") {
                    YGDateButton(text = "15", isSelected = true, isToday = false, isEnabled = true, onClick = {})
                }
            }
            item {
                PreviewSection("today") {
                    YGDateButton(text = "15", isSelected = false, isToday = true, isEnabled = true, onClick = {})
                }
            }
            item {
                PreviewSection("disabled") {
                    YGDateButton(text = "15", isSelected = false, isToday = false, isEnabled = false, onClick = {})
                }
            }
        }
    }
}
```

- [ ] **Step 6: YGInputNumberPreviewScreen 작성** (remember 선택 상태)

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.yginputnumber.YGInputNumber
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGInputNumberPreviewScreen(
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
                var selected by remember { mutableIntStateOf(1) }
                PreviewSection("interactive (tap to select)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { n ->
                            YGInputNumber(number = n, isSelected = selected == n, onClick = { selected = n })
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 7: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (6개 화면 컴파일, 아직 미배선이라 미사용 경고 가능).

- [ ] **Step 8: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGButtonPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGChipButtonPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToggleButtonPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGIconButtonPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGDateButtonPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGInputNumberPreviewScreen.kt
git commit -m "feat(app-preview): Button 카테고리 프리뷰 화면 6개 추가"
```

---

## Task 4: Input 카테고리 화면 2개

**Files:**
- Create: `screen/component/YGTextFieldPreviewScreen.kt`, `YGTextFormFieldPreviewScreen.kt`

**Interfaces:**
- Consumes: `PreviewSection`(Task 2).
- Produces: `YGTextFieldPreviewScreen(onBack, modifier)`, `YGTextFormFieldPreviewScreen(onBack, modifier)`.

- [ ] **Step 1: YGTextFieldPreviewScreen 작성** (remember 텍스트 상태)

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextField
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGTextFieldPreviewScreen(
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
                var text by remember { mutableStateOf("") }
                PreviewSection("default (placeholder)") {
                    YGTextField(value = text, onValueChange = { text = it }, placeholder = "입력하세요")
                }
            }
            item {
                var text by remember { mutableStateOf("입력값") }
                PreviewSection("filled + maxLength 10") {
                    YGTextField(value = text, onValueChange = { text = it }, maxLength = 10)
                }
            }
            item {
                var text by remember { mutableStateOf("잘못된 값") }
                PreviewSection("error") {
                    YGTextField(value = text, onValueChange = { text = it }, isError = true)
                }
            }
            item {
                PreviewSection("disabled") {
                    YGTextField(value = "비활성", onValueChange = {}, enabled = false)
                }
            }
        }
    }
}
```

- [ ] **Step 2: YGTextFormFieldPreviewScreen 작성**

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGTextFormFieldPreviewScreen(
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
                var text by remember { mutableStateOf("") }
                PreviewSection("default (placeholder)") {
                    YGTextFormField(value = text, onValueChange = { text = it }, placeholder = "닉네임")
                }
            }
            item {
                var text by remember { mutableStateOf("bad") }
                PreviewSection("error + errorDescription") {
                    YGTextFormField(
                        value = text,
                        onValueChange = { text = it },
                        isError = true,
                        errorDescription = "사용할 수 없는 값입니다",
                    )
                }
            }
            item {
                PreviewSection("disabled") {
                    YGTextFormField(value = "비활성", onValueChange = {}, enabled = false)
                }
            }
        }
    }
}
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTextFieldPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTextFormFieldPreviewScreen.kt
git commit -m "feat(app-preview): Input 카테고리 프리뷰 화면 2개 추가"
```

---

## Task 5: Text 카테고리 화면 3개

**Files:**
- Create: `screen/component/YGLabelPreviewScreen.kt`, `YGDatePreviewScreen.kt`, `YGActionItemPreviewScreen.kt`

**Interfaces:**
- Consumes: `PreviewSection`(Task 2).
- Produces: `YGLabelPreviewScreen(onBack, modifier)`, `YGDatePreviewScreen(onBack, modifier)`, `YGActionItemPreviewScreen(onBack, modifier)`.

- [ ] **Step 1: YGLabelPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGLabelPreviewScreen(
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
                PreviewSection("label") {
                    YGLabel(text = "레이블 텍스트")
                }
            }
            item {
                PreviewSection("long label") {
                    YGLabel(text = "조금 더 긴 보조 설명 레이블 텍스트입니다")
                }
            }
        }
    }
}
```

- [ ] **Step 2: YGDatePreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGDatePreviewScreen(
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
                PreviewSection("date") {
                    YGDate(text = "2026.07.21")
                }
            }
        }
    }
}
```

- [ ] **Step 3: YGActionItemPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGActionItemPreviewScreen(
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
                PreviewSection("action item") {
                    YGActionItem(text = "로그아웃", onClick = {})
                }
            }
            item {
                PreviewSection("another action") {
                    YGActionItem(text = "회원 탈퇴", onClick = {})
                }
            }
        }
    }
}
```

- [ ] **Step 4: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGLabelPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGDatePreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGActionItemPreviewScreen.kt
git commit -m "feat(app-preview): Text 카테고리 프리뷰 화면 3개 추가"
```

---

## Task 6: Container 카테고리 화면 5개

**Files:**
- Create: `screen/component/YGModalPopupPreviewScreen.kt`, `YGInviteCardPreviewScreen.kt`, `YGDangerZonePreviewScreen.kt`, `YGListItemPreviewScreen.kt`, `YGHorizontalDividerPreviewScreen.kt`

**Interfaces:**
- Consumes: `PreviewSection`(Task 2).
- Produces: 위 5개 화면 `(onBack, modifier)`.

- [ ] **Step 1: YGModalPopupPreviewScreen 작성** (Dialog → remember show 상태로 열기/닫기)

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGModalPopupPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showModal by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PreviewSection("tap to open modal") {
                    YGButton(text = "모달 열기", buttonType = YGButtonType.Medium.Primary, isEnabled = true, onClick = { showModal = true })
                }
            }
        }
    }
    if (showModal) {
        YGModalPopup(
            title = "정말 삭제할까요?",
            body = "삭제하면 되돌릴 수 없어요.",
            iconRes = R.drawable.ic_warning_round,
            secondaryText = "취소",
            onSecondaryClick = { showModal = false },
            primaryText = "삭제",
            onPrimaryClick = { showModal = false },
            onDismissRequest = { showModal = false },
        )
    }
}
```

- [ ] **Step 2: YGInviteCardPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCard
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCardStatus
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGInviteCardPreviewScreen(
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
                PreviewSection("Active") {
                    YGInviteCard(
                        label = "초대 코드",
                        inviteCode = "ABC123",
                        subText = "유효한 코드입니다",
                        status = YGInviteCardStatus.Active,
                        copyButtonText = "복사",
                        onCopyClick = {},
                    )
                }
            }
            item {
                PreviewSection("Invalid") {
                    YGInviteCard(
                        label = "초대 코드",
                        inviteCode = "------",
                        subText = "만료된 코드입니다",
                        status = YGInviteCardStatus.Invalid,
                        copyButtonText = "복사",
                        onCopyClick = {},
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: YGDangerZonePreviewScreen 작성** (top/bottom 슬롯에 YGActionItem)

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
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGDangerZonePreviewScreen(
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
                PreviewSection("top + bottom zones") {
                    YGDangerZone(
                        topZone = { YGActionItem(text = "로그아웃", onClick = {}) },
                        bottomZone = { YGActionItem(text = "회원 탈퇴", onClick = {}) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: YGListItemPreviewScreen 작성** (오버로드 2개 모두)

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
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.etc.YGListItem
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGListItemPreviewScreen(
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
                PreviewSection("trailing icon overload") {
                    YGListItem(
                        text = "계정 정보",
                        trailingIcon = R.drawable.ic_caret_right,
                        onClickTrailingIcon = {},
                    )
                }
            }
            item {
                PreviewSection("subText overload") {
                    YGListItem(
                        text = "버전 정보",
                        subText = "1.0.0",
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: YGHorizontalDividerPreviewScreen 작성**

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
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack

@Composable
internal fun YGHorizontalDividerPreviewScreen(
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
                PreviewSection("default (1dp)") {
                    YGHorizontalDivider()
                }
            }
            item {
                PreviewSection("thick 4dp") {
                    YGHorizontalDivider(thickness = 4.dp)
                }
            }
        }
    }
}
```

- [ ] **Step 6: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGModalPopupPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGInviteCardPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGDangerZonePreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGListItemPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGHorizontalDividerPreviewScreen.kt
git commit -m "feat(app-preview): Container 카테고리 프리뷰 화면 5개 추가"
```

---

## Task 7: Bar 카테고리 화면 1개 (YGTopBar 4변형)

**Files:**
- Create: `screen/component/YGTopBarPreviewScreen.kt`

**Interfaces:**
- Consumes: `PreviewSection`(Task 2).
- Produces: `YGTopBarPreviewScreen(onBack, modifier)`.

- [ ] **Step 1: YGTopBarPreviewScreen 작성** (Back/Detail/Empty/Default 4변형 나열)

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
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDefault
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty

@Composable
internal fun YGTopBarPreviewScreen(
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
                PreviewSection("YGTopBarBack") {
                    YGTopBarBack(onIconClick = {})
                }
            }
            item {
                PreviewSection("YGTopBarDetail") {
                    YGTopBarDetail(title = "상세 화면", onIconClick = {})
                }
            }
            item {
                PreviewSection("YGTopBarEmpty") {
                    YGTopBarEmpty(onIconClick = {})
                }
            }
            item {
                PreviewSection("YGTopBarDefault") {
                    YGTopBarDefault(onIconClick = {}, onChipClick = {})
                }
            }
        }
    }
}
```

> 주의: `PreviewSection` 내부 `padding(vertical)`만 있으므로 TopBar는 전체폭 렌더. contentPadding에 horizontal 0으로 둔 것은 TopBar가 좌우 여백 없이 폭을 쓰도록 하기 위함.

- [ ] **Step 2: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGTopBarPreviewScreen.kt
git commit -m "feat(app-preview): Bar 카테고리 프리뷰 화면(YGTopBar 4변형) 추가"
```

---

## Task 8: 네비게이션 배선 (EntryBuilders + DI 모듈)

**Files:**
- Create: `navigation/entry/ComponentEntryBuilders.kt`
- Create: `navigation/di/ComponentEntryModule.kt`

**Interfaces:**
- Consumes: 17개 `NavKeyYG*`(Task 1), 17개 `YG*PreviewScreen`(Task 3~7), `Navigator`(`goTo`/`onBack`).
- Produces: `EntryProviderScope<NavKey>.componentEntryBuilders(navigator: Navigator)` + Hilt `@IntoSet @Provides`. `RootRoute`의 entryBuilder set에 자동 합류 → 각 NavKey 도달 시 해당 화면 렌더.

- [ ] **Step 1: ComponentEntryBuilders 작성**

```kotlin
package com.teamyg.parfait.preview.navigation.entry

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.navigation.key.NavKeyYGActionItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGChipButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDangerZone
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDate
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDateButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGHorizontalDivider
import com.teamyg.parfait.preview.navigation.key.NavKeyYGIconButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInputNumber
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInviteCard
import com.teamyg.parfait.preview.navigation.key.NavKeyYGLabel
import com.teamyg.parfait.preview.navigation.key.NavKeyYGListItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGModalPopup
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextFormField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToggleButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTopBar
import com.teamyg.parfait.preview.screen.component.YGActionItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGChipButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDangerZonePreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDatePreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDateButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGHorizontalDividerPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGIconButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGInputNumberPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGInviteCardPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGLabelPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGListItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGModalPopupPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTextFieldPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTextFormFieldPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGToggleButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTopBarPreviewScreen

internal fun EntryProviderScope<NavKey>.componentEntryBuilders(navigator: Navigator) {
    entry<NavKeyYGButton> { ScreenScaffold { m -> YGButtonPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGChipButton> { ScreenScaffold { m -> YGChipButtonPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGToggleButton> { ScreenScaffold { m -> YGToggleButtonPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGIconButton> { ScreenScaffold { m -> YGIconButtonPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGDateButton> { ScreenScaffold { m -> YGDateButtonPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGInputNumber> { ScreenScaffold { m -> YGInputNumberPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGTextField> { ScreenScaffold { m -> YGTextFieldPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGTextFormField> { ScreenScaffold { m -> YGTextFormFieldPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGLabel> { ScreenScaffold { m -> YGLabelPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGDate> { ScreenScaffold { m -> YGDatePreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGActionItem> { ScreenScaffold { m -> YGActionItemPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGModalPopup> { ScreenScaffold { m -> YGModalPopupPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGInviteCard> { ScreenScaffold { m -> YGInviteCardPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGDangerZone> { ScreenScaffold { m -> YGDangerZonePreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGListItem> { ScreenScaffold { m -> YGListItemPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGHorizontalDivider> { ScreenScaffold { m -> YGHorizontalDividerPreviewScreen(onBack = navigator::onBack, modifier = m) } }
    entry<NavKeyYGTopBar> { ScreenScaffold { m -> YGTopBarPreviewScreen(onBack = navigator::onBack, modifier = m) } }
}

@androidx.compose.runtime.Composable
private fun ScreenScaffold(content: @androidx.compose.runtime.Composable (Modifier) -> Unit) {
    Scaffold { innerPadding ->
        content(Modifier.fillMaxSize().padding(innerPadding))
    }
}
```

> `ScreenScaffold`는 17개 entry의 `Scaffold { padding }` 보일러플레이트를 1곳으로 모은 화면 파일-로컬 헬퍼. 기존 `MainEntryBuilder`의 `Scaffold { innerPadding -> ...(padding(innerPadding)) }` 패턴과 동일.

- [ ] **Step 2: ComponentEntryModule 작성**

```kotlin
package com.teamyg.parfait.preview.navigation.di

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.navigation.entry.componentEntryBuilders
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
object ComponentEntryModule {
    @IntoSet
    @Provides
    fun provideComponentEntryBuilders(): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        componentEntryBuilders(navigator = it)
    }
}
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (이제 모든 화면이 배선됨. 미사용 경고 사라짐.)

- [ ] **Step 4: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/di/ComponentEntryModule.kt
git commit -m "feat(app-preview): 컴포넌트 화면 17개 Navigation3 배선(EntryBuilders+DI)"
```

---

## Task 9: MainScreen 카테고리 그룹 목록 + Route 배선

**Files:**
- Modify: `screen/MainScreen.kt`
- Modify: `route/MainRoute.kt`

**Interfaces:**
- Consumes: `componentCatalog`, `ComponentCategory`(Task 1), `Navigator.goTo`.
- Produces: 메인에서 카테고리 그룹 버튼 → 탭 시 해당 NavKey로 이동. 최종 동작 완성.

- [ ] **Step 1: MainScreen 개조** (플레이스홀더 → 카탈로그 그룹 목록)

`screen/MainScreen.kt` 전체 교체:
```kotlin
package com.teamyg.parfait.preview.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.preview.model.ComponentCategory
import com.teamyg.parfait.preview.model.componentCatalog

@Composable
internal fun MainScreen(
    onComponentClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Component Preview",
                style = YGTheme.typography.title.t03SB,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        ComponentCategory.entries.forEach { category ->
            val entries = componentCatalog.filter { it.category == category }
            if (entries.isNotEmpty()) {
                item(key = "header_${category.name}") {
                    YGLabel(
                        text = category.label,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(
                    items = entries,
                    key = { it.label },
                ) { entry ->
                    YGButton(
                        text = entry.label,
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = { onComponentClick(entry.navKey) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
```

> 메인 상단은 `YGTopBarDetail` 대신 일반 제목 `Text`를 쓴다 — 루트 화면엔 뒤로가기 대상이 없어 back 아이콘이 오작동으로 보이기 때문(spec의 "상단 YGTopBarDetail" 문구는 이 이유로 조정). 제목 타이포는 `YGTheme.typography.title.t03SB`.

- [ ] **Step 2: MainRoute 개조** (navigator.goTo 전달)

`route/MainRoute.kt` 전체 교체:
```kotlin
package com.teamyg.parfait.preview.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.screen.MainScreen

@Composable
internal fun MainRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    MainScreen(
        onComponentClick = navigator::goTo,
        modifier = modifier,
    )
}
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 전체 빌드 검증**

Run: `./gradlew :app-preview:assembleDebug`
Expected: BUILD SUCCESSFUL (APK 생성).

- [ ] **Step 5: 커밋** *(사용자 확인 후)*

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/MainScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/route/MainRoute.kt
git commit -m "feat(app-preview): 메인 화면 카테고리 그룹 컴포넌트 목록 + 이동 배선"
```

---

## Task 10: 실기기 육안 검증

**Files:** 없음(검증 전용).

- [ ] **Step 1: 앱 설치·실행**

Run: `./gradlew :app-preview:installDebug` (에뮬레이터/실기기 연결 상태)
또는 Android Studio에서 `app-preview` 런 구성 실행.

- [ ] **Step 2: 체크리스트 육안 확인**

- [ ] 메인에 5개 카테고리(Button/Input/Text/Container/Bar) 헤더 + 17개 버튼 표시.
- [ ] 각 버튼 탭 → 해당 컴포넌트 화면 진입, 상단 back으로 복귀.
- [ ] 상태형 컴포넌트 인터랙션: YGToggleButton 토글, YGInputNumber 선택, YGTextField/FormField 입력, YGModalPopup 열기/닫기.
- [ ] 17개 화면 전부 도달 가능(카탈로그↔NavKey↔Entry 매핑 누락 없음).

- [ ] **Step 3: 완료 처리**

- 계획 문서 `status: done` + `archived_reason` 기입 후 `archive/`로 이동, README 갱신.
- 스펙 `status: implemented` + `specs/archive/` 이동, README 갱신.

---

## Self-Review

**Spec coverage:** spec의 5개 카테고리·17화면·카탈로그 모델·컴포넌트별 NavKey/EntryBuilder/화면·PreviewSection 헬퍼·상태형 remember 인터랙션·`goTo`/`onBack` 배선 → Task 1~9로 전부 매핑. feature 로컬 컴포넌트·기존 @YGPreview 재사용·테스트는 spec에서 제외 → 계획에도 없음. ✅

**Placeholder scan:** 모든 코드 스텝에 실제 컴파일 가능한 전체 코드 수록. "TODO/TBD/적절히 처리" 없음. ✅

**Type consistency:** 화면 시그니처 `(onBack: () -> Unit, modifier: Modifier = Modifier)` 전 화면 통일. NavKey명 ↔ 카탈로그 ↔ EntryBuilder import 17개 일치 확인. `YGButtonType.Medium.Secondary`, `YGChipButtonColorsDefaults.*`, `YGIconButtonSize.SIZE_*`, `YGInviteCardStatus.*`, `navigator.goTo`/`navigator.onBack` 실제 API명 사용. ✅

**조정 사항(spec 대비):** 메인 상단을 `YGTopBarDetail` → 일반 `Text` 제목으로 변경(루트에 back 대상 없음). 기능 영향 없음, spec 본문의 근거와 일치.
