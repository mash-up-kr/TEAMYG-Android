---
id: designsystem-grouptag-topping-components
title: Grouptag-Chip·Topping-Group 컴포넌트 구현 계획
status: done
type: work-order
created: 2026-07-31
updated: 2026-08-01
platforms: android
owner: TJYG-Android 디자인시스템
related_adr:
related_spec:
  - designsystem-grouptag-topping-components
related_code:
  - YGGrouptagChip.kt#YGGrouptagChip
  - YGGrouptagChipType.kt#YGGrouptagChipType
  - YGToppingGroup.kt#YGToppingGroup
  - YGToppingGroupType.kt#YGToppingGroupType
  - YGToppingImage.kt#YGToppingImage
  - YGToppingTemplate.kt#YGToppingTemplate
  - SizeTokens.kt#SizeTokens
  - ComposeConfig.kt#setComposeDependencies
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
archived_reason: PR #186 develop 머지 완료(2026-08-01) — 코드=설계 일치, 스펙 implemented 전환
tags: [plan, parfait, designsystem, figma-sync, g-001, topping]
---

# Grouptag-Chip·Topping-Group Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는
> superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** Figma `Grouptag-Chip`·`Topping-Group` 2종을 `:core:designsystem`에 신설하고
`:app-preview` 갤러리에서 전 변형을 실기기로 검증한다.

**Architecture:** 아래에서 위로 쌓는다. 빌드 기반(Coil 네트워크 페처·크기 토큰) → `YGGrouptagChip`
→ 토핑 모델 3종 + 에셋 → `YGToppingGroup`(칩을 합성) → 전체 검증. 각 Task는 앞 Task의 산출물만
소비하므로 순서를 바꾸면 컴파일이 깨진다.

**Tech Stack:** Kotlin, Jetpack Compose, Coil 3, Hilt, Navigation3, Gradle 컨벤션 플러그인.

> **실행 결과(2026-07-31)** — Task 1~5 전량 완료. subagent-driven-development로 Task마다 새
> 서브에이전트 + 리뷰를 돌렸고, Task 1~3은 리뷰 1회에 클린 통과했다. Task 4만 실기기 육안 검증에서
> 결함 2건이 나와 fix round 1회로 해소했다(둘 다 갤러리 화면 한정, `:core:designsystem` 무변경).
>
> **계획 자체의 결함 1건** — Task 4 Step 4가 지정한 샘플 이미지 URL이 **죽은 주소(404)** 였다.
> 계획을 쓸 때 실재 확인을 하지 않은 것이 원인이다. 이 때문에 "Remote 성공" 칸이 error 폴백으로
> 떨어져, 이 라운드가 검증하려던 **네트워크 페처 동작 자체를 확인할 수 없는 상태**로 처음 캡처가
> 나왔다. live 확인한 `https://picsum.photos/400`으로 교체한 뒤 실제 사진 렌더를 확인했다.
> 교훈: 계획서에 외부 URL을 상수로 박을 때는 작성 시점에 응답 코드를 확인해야 한다.
>
> **TJYG-Android는 커밋하지 않았다**(작업자 지시). 브랜치 `feature/grouptag-topping-component`에
> 작업 트리 변경만 남아 있다. 스펙 `archive/` 이동과 `architecture/design-system` as-built 갱신은
> develop 머지 후로 미룬다.

## Global Constraints

- 작업 대상 repo는 **`TJYG-Android`**(이 repo 아님). 로컬 절대경로는
  `wiki/personal-private/project-paths.md` 참고. 아래 모든 경로는 그 repo 루트 기준.
- **TJYG-Android에 커밋하지 않는다**(작업자 지시). 작업 트리 변경만 남기고 보고한다.
  각 Task 말미의 "커밋" 단계는 **의도적으로 없다.**
- 테스트를 작성하지 않는다. 선행 디자인시스템 라운드 5회에서 확립된 판단 — 상태 없는 순수 렌더
  컴포넌트라 단위 테스트가 잡을 회귀가 거의 없고, 실제 결함(색 대비·잘림·오프셋)은 육안 검증에서만
  드러난다. Task별 검증 사이클은 **`assembleDebug` + `ktlintCheck` + 프리뷰/갤러리 육안**이다.
- 색·치수는 반드시 **토큰 심볼**로 참조한다. hex 리터럴·raw dp 금지. 단 Figma가 소수로 준
  배치 오프셋(`-1.25`, `+49.23` 등)은 예외 — 토큰화하지 않고 상수로 그대로 쓴다.
- 프리뷰 관용구는 `@YGPreview` + `PreviewBox` 고정(`@Preview` + `YGCustomTheme` 금지).
- 패키지명은 소문자 컴포넌트명(`component/yggrouptagchip/`), 파일명은 PascalCase.
- ktlint는 **repo 전체**(`./gradlew ktlintCheck`)로 돌린다. 모듈 단위로만 돌리면 갤러리 모듈
  위반을 놓친다.

---

## Task 1: 빌드 기반 — Coil 네트워크 페처 + 크기 토큰

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/ComposeConfig.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/theme/size/SizeTokens.kt`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `SizeTokens.Size96`·`SizeTokens.Size160`(둘 다 `SizeToken`, `.getDp()`로 `Dp` 취득).
  전 모듈에서 `coil3.network` 페처 사용 가능.

**배경:** 현재 `coil-compose`만 있고 네트워크 페처가 없어 원격 URL이 로드되지 않는다. 캔버스 라운드에서
`YGCanvasBackground.Image`가 미검증으로 남은 원인이 이것이다. Task 4의 `YGToppingImage.Remote`가 같은
문제를 물려받으므로 여기서 먼저 해소한다.

- [x] **Step 1: 버전 카탈로그에 네트워크 페처 추가**

`gradle/libs.versions.toml`의 `#Coil` 섹션에서 `coil-compose` 줄 **바로 아래**에 추가한다.
`coil` 버전 참조를 재사용하므로 `[versions]` 블록은 건드리지 않는다.

```toml
#Coil
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
```

- [x] **Step 2: 컴포즈 컨벤션 플러그인에 의존 추가**

`ComposeConfig.kt`의 `setComposeDependencies()` 안, 기존 `implementation(libs.coil.compose)` 바로
아래에 한 줄 추가한다. `coil-compose`가 이미 여기 있으므로 같은 자리에 둔다.

```kotlin
        implementation(libs.coil.compose)
        implementation(libs.coil.network.okhttp)
```

- [x] **Step 3: 크기 토큰 2종 추가**

`SizeTokens.kt`의 `object SizeTokens` 안, 숫자 오름차순 자리에 넣는다. `Size80` 다음이 마지막이므로
그 뒤에 이어 붙인다.

```kotlin
    val Size80: SizeToken = SizeToken(80)
    val Size96: SizeToken = SizeToken(96)
    val Size160: SizeToken = SizeToken(160)
```

둘 다 **Figma가 고정한 치수**다(토핑 이미지 96, 토핑 프레임 160). 버튼 라운드에서 세운 원칙 —
"패딩으로 도출되는 치수는 하드코딩하지 않고, Figma가 고정한 곳만 토큰으로 못박는다" — 에 해당한다.

- [x] **Step 4: 빌드 확인**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL. 컨벤션 플러그인을 고쳤으므로 build-logic이 먼저 재컴파일된다.

- [x] **Step 5: ktlint 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

---

## Task 2: `YGGrouptagChip` + 타입 6종 + 프리뷰 + 갤러리

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yggrouptagchip/YGGrouptagChipType.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yggrouptagchip/YGGrouptagChip.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGGrouptagChip.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGGrouptagChipPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: `SizeTokens.Size80`(기존)
- Produces:
  - `enum class YGGrouptagChipType(val timestampColor: Color)` — 엔트리
    `TYPE_1_2`, `TYPE_3_4`, `TYPE_5_6`, `TYPE_7_8`, `TYPE_9_10`, `TYPE_11_12`
  - `@Composable fun YGGrouptagChip(name: String, timestamp: String, type: YGGrouptagChipType, modifier: Modifier = Modifier)`
  - Task 4가 이 둘을 그대로 소비한다.

- [x] **Step 1: 타입 enum 작성**

타임스탬프 텍스트 컬러만 결정한다. 기준은 해당 그룹에서 **마지막으로 변화를 가한 유저의 타입**이다.
Nametag 원형칩(`YGColorChipType`)과 매핑 표가 별개라 타입도 별개로 둔다 — 정책 문서가 이미 두 표로
분리돼 있고, `YGColorChipType`은 13종 + Plus로 정책 12종과 이미 어긋나 있다.

`YGGrouptagChipType.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.yggrouptagchip

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * Figma `Grouptag-Chip`의 `Type` 변형 6종.
 *
 * 한 엔트리가 Nametag 타입 2개(연핑크 = Type 1·2 …)를 묶는다. 타임스탬프 텍스트 컬러만 결정하며,
 * Nametag 원형칩([com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType])과는
 * 매핑이 별개다.
 */
enum class YGGrouptagChipType(val timestampColor: Color) {
    /** 연핑크 */
    TYPE_1_2(YGAtomicColors.Cherry.Cherry100),

    /** 진핑크 */
    TYPE_3_4(YGAtomicColors.Cherry.Cherry200),

    /** 체리 */
    TYPE_5_6(YGAtomicColors.Cherry.Cherry300),

    /** 그레이 */
    TYPE_7_8(YGAtomicColors.Gray.Gray200),

    /** 멜론 */
    TYPE_9_10(YGAtomicColors.Melon.Melon500),

    /** 푸딩 */
    TYPE_11_12(YGAtomicColors.Pudding.Pudding500),
}
```

- [x] **Step 2: 컴포넌트 작성**

3요소 고정 `Row`(슬롯 없음). 이름은 80dp를 넘으면 말줄임한다 — 그룹 목록 라벨 정책이 "이름 80px 초과 시
초과분부터 `…`"(픽셀 기준, 문자수 기준은 폐기)이고, Figma도 `팀장은 진짜 연...`으로 잘려 있다.
타임스탬프는 "항상 전체 노출"이라 잘리지 않는다.

`YGGrouptagChip.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.yggrouptagchip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private val DividerSize = 1.25.dp

/**
 * Figma `Grouptag-Chip`. 그룹 대표 토핑에 붙는 이름·상대시간 라벨.
 *
 * @param name 그룹명. 80dp를 넘으면 말줄임된다.
 * @param timestamp 상대 시간 문구("3분전"). 항상 전체 노출된다.
 * @param type 타임스탬프 텍스트 컬러를 결정하는 유저 타입.
 */
@Composable
fun YGGrouptagChip(
    name: String,
    timestamp: String,
    type: YGGrouptagChipType,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        modifier = modifier
            .background(
                color = YGAtomicColors.Transparency.Black75,
                shape = YGTheme.shapes.radius.round,
            ).padding(
                horizontal = YGTheme.layout.padding.padding5,
                vertical = YGTheme.layout.padding.padding2,
            ),
    ) {
        Text(
            text = name,
            color = YGAtomicColors.Gray.White,
            style = YGTheme.typography.body.b02SB,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = SizeTokens.Size80.getDp()),
        )
        Box(
            modifier = Modifier
                .size(DividerSize)
                .background(
                    color = YGAtomicColors.Transparency.White50,
                    shape = YGTheme.shapes.radius.round,
                ),
        )
        Text(
            text = timestamp,
            color = type.timestampColor,
            style = YGTheme.typography.caption.c01R,
        )
    }
}

@YGPreview
@Composable
private fun YGGrouptagChipPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        YGGrouptagChipType.entries.forEach { type ->
            YGGrouptagChip(
                name = "잠탈감금",
                timestamp = "3분전",
                type = type,
            )
        }
        YGGrouptagChip(
            name = "팀장은 진짜 연경이야",
            timestamp = "3분전",
            type = YGGrouptagChipType.TYPE_7_8,
        )
    }
}
```

- [x] **Step 3: 프리뷰 렌더 확인**

Android Studio에서 `YGGrouptagChip.kt`를 열고 `YGGrouptagChipPreview`를 렌더한다.
Expected: pill 7개. 위 6개는 타임스탬프 색만 다르고, 마지막 1개는 이름이 `팀장은 진짜 연…`으로 잘린다.

- [x] **Step 4: 갤러리 NavKey 작성**

`NavKeyYGGrouptagChip.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGGrouptagChip : NavKey
```

- [x] **Step 5: 갤러리 화면 작성**

`YGGrouptagChipPreviewScreen.kt`:

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
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChip
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGGrouptagChipPreviewScreen(
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
                PreviewSection("Type 6종 (타임스탬프 색만 다름)") {
                    YGGrouptagChipType.entries.forEach { type ->
                        YGGrouptagChip(
                            name = "잠탈감금",
                            timestamp = "3분전",
                            type = type,
                        )
                    }
                }
            }
            item {
                PreviewSection("이름 말줄임 (80dp 초과)") {
                    YGGrouptagChip(
                        name = "팀장은 진짜 연경이야",
                        timestamp = "3분전",
                        type = YGGrouptagChipType.TYPE_7_8,
                    )
                    YGGrouptagChip(
                        name = "다섯글자임",
                        timestamp = "오래 전",
                        type = YGGrouptagChipType.TYPE_9_10,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGGrouptagChipPreviewScreen() = PreviewBox {
    YGGrouptagChipPreviewScreen(
        onBack = {},
    )
}
```

- [x] **Step 6: 카탈로그 등록**

`ComponentCatalog.kt` — import를 알파벳 순 자리에 추가하고, `ComponentCategory.TEXT` 블록의
마지막(`YGAlert` 뒤)에 엔트리를 넣는다. 카테고리를 `TEXT`로 두는 이유: 이 칩은 라벨 텍스트 표시가
전부이고 상호작용이 없다.

```kotlin
import com.teamyg.parfait.preview.navigation.key.NavKeyYGGrouptagChip
```

```kotlin
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGGrouptagChip",
        navKey = NavKeyYGGrouptagChip,
    ),
```

- [x] **Step 7: 엔트리 배선**

`ComponentEntryBuilders.kt` — import 추가 후, `componentEntryBuilders` 함수 본문 마지막
`entry<NavKeyYGCanvas>` 블록 **뒤에** 추가한다(`private fun ScreenScaffold` 앞).

```kotlin
    entry<NavKeyYGGrouptagChip> {
        ScreenScaffold { modifier ->
            YGGrouptagChipPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [x] **Step 8: 빌드·ktlint 확인**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug ktlintCheck`
Expected: BUILD SUCCESSFUL

---

## Task 3: 토핑 에셋 반입 + 모델 3종

**Files:**
- Create: `core/designsystem/src/main/res/drawable-{ldpi,mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/img_topping_template_01.png` … `_06.png`, `img_topping_template_error.png` (총 42개 파일)
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingTemplate.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingImage.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingGroupType.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `enum class YGToppingTemplate(@DrawableRes val drawableRes: Int)` — `TEMPLATE_01` … `TEMPLATE_06`
  - `sealed interface YGToppingImage` — `Remote(url: String)` / `Template(type: YGToppingTemplate)` / `Error`
  - `enum class YGToppingGroupType(val rotation: Float, val imageOffset: DpOffset, val chipOffset: DpOffset)`
    — `TYPE_1_LEFT`, `TYPE_1_RIGHT`, `TYPE_2_LEFT`, `TYPE_2_RIGHT`, `TYPE_3_LEFT`, `TYPE_3_RIGHT`, `TEMPLATE`
  - `internal val TOPPING_ERROR_DRAWABLE: Int`(= `R.drawable.img_topping_template_error`) — Task 4가 쓴다.

- [x] **Step 1: 에셋 복사**

원본은 작업자가 제공한 `~/Downloads/Topping-Template/Android/`이고, 안에 density 폴더 6개
(`ldpi`, `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`)가 있으며 각 폴더에 `Template01.png` ~
`Template06.png` + `Template-Error.png` 7개가 들어 있다.

안드로이드 리소스 파일명은 소문자·숫자·언더스코어만 허용하므로 이름을 바꿔 복사한다.

```bash
SRC=~/Downloads/Topping-Template/Android
DST=core/designsystem/src/main/res

for d in ldpi mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  mkdir -p "$DST/drawable-$d"
  for n in 01 02 03 04 05 06; do
    cp "$SRC/$d/Template$n.png" "$DST/drawable-$d/img_topping_template_$n.png"
  done
  cp "$SRC/$d/Template-Error.png" "$DST/drawable-$d/img_topping_template_error.png"
done
```

- [x] **Step 2: 복사 결과 확인**

Run: `find core/designsystem/src/main/res -name "img_topping_template_*" | wc -l`
Expected: `42` (7개 × 6 density)

- [x] **Step 3: 템플릿 enum 작성**

에셋 6종의 그림 내용은 별×2·음표×2·소용돌이×2이며 전부 회색 반투명 아웃라인이다. 어떤 종을 부여할지는
**이 컴포넌트가 정하지 않는다** — 호출자가 결정해 넘긴다.

`YGToppingTemplate.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.annotation.DrawableRes
import com.teamyg.parfait.core.designsystem.R

/**
 * 그룹에 아직 첫 토핑이 없을 때 토핑 영역을 채우는 템플릿 그래픽 6종.
 *
 * 6종 중 어느 것을 부여할지, 언제까지 고정할지는 제품 정책이며 이 컴포넌트의 책임이 아니다.
 * 호출자가 결정해 [YGToppingImage.Template]로 넘긴다.
 */
enum class YGToppingTemplate(@DrawableRes val drawableRes: Int) {
    TEMPLATE_01(R.drawable.img_topping_template_01),
    TEMPLATE_02(R.drawable.img_topping_template_02),
    TEMPLATE_03(R.drawable.img_topping_template_03),
    TEMPLATE_04(R.drawable.img_topping_template_04),
    TEMPLATE_05(R.drawable.img_topping_template_05),
    TEMPLATE_06(R.drawable.img_topping_template_06),
}
```

- [x] **Step 4: 콘텐츠 상태 sealed 작성**

3상태를 **명시적으로 주입받는다.** URL이 null인지로 상태를 추론하면 "조회 실패"와 "이미지 없음"이
뭉개진다 — 원인이 다른 별개 상태다.

`YGToppingImage.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import com.teamyg.parfait.core.designsystem.R

internal val TOPPING_ERROR_DRAWABLE: Int = R.drawable.img_topping_template_error

/**
 * [YGToppingGroup]의 토핑 영역에 무엇을 그릴지 나타내는 3상태.
 *
 * 어느 상태인지 판정하는 것은 호출자(feature/domain) 책임이다.
 */
sealed interface YGToppingImage {
    /** 정상 대표 토핑. 이미지 로드에 실패하면 [Error]와 같은 그래픽으로 떨어진다. */
    data class Remote(val url: String) : YGToppingImage

    /** 그룹에 아직 첫 토핑이 없음. 6종 중 [type]을 그린다. */
    data class Template(val type: YGToppingTemplate) : YGToppingImage

    /** 특정 토핑 조회 실패. 물음표 그래픽 1종을 그린다. */
    data object Error : YGToppingImage
}
```

- [x] **Step 5: 배치 변형 enum 작성**

프레임 중심(0, 0) 기준 오프셋이다. 회전은 시계방향 양수(Compose `Modifier.rotate` 규약).
Figma 소수값을 **반올림하지 않는다** — 반올림하면 나중에 Figma와 대조할 때 "의도적 차이"인지
"드리프트"인지 구분할 수 없다.

`YGToppingGroupType.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Figma `Topping-Group`의 배치 변형 7종.
 *
 * Left/Right 선택(그리드 index % 2)과 변형 번호(1/2/3) 재부여는 호출자 책임이다.
 * [TEMPLATE]은 회전 0°인 배치 변형일 뿐이고, 안에 무엇을 그릴지는 [YGToppingImage]가 정한다.
 *
 * @param rotation 토핑 이미지 회전각(도, 시계방향 양수).
 * @param imageOffset 프레임 중심 기준 이미지 오프셋.
 * @param chipOffset 프레임 중심 기준 그룹 칩 오프셋.
 */
enum class YGToppingGroupType(
    val rotation: Float,
    val imageOffset: DpOffset,
    val chipOffset: DpOffset,
) {
    TYPE_1_LEFT(
        rotation = -6f,
        imageOffset = DpOffset(x = (-1.25).dp, y = (-11.25).dp),
        chipOffset = DpOffset(x = (-0.5).dp, y = 49.23.dp),
    ),
    TYPE_1_RIGHT(
        rotation = 6f,
        imageOffset = DpOffset(x = (-1.25).dp, y = (-11.25).dp),
        chipOffset = DpOffset(x = (-0.5).dp, y = 49.23.dp),
    ),
    TYPE_2_LEFT(
        rotation = -12f,
        imageOffset = DpOffset(x = 1.06.dp, y = (-12.07).dp),
        chipOffset = DpOffset(x = 0.13.dp, y = 54.69.dp),
    ),
    TYPE_2_RIGHT(
        rotation = 16f,
        imageOffset = DpOffset(x = 1.5.dp, y = (-12.63).dp),
        chipOffset = DpOffset(x = 0.13.dp, y = 58.13.dp),
    ),

    // Figma에서 TYPE_3_LEFT와 TYPE_3_RIGHT는 회전·오프셋이 완전히 동일하다.
    // 다른 Left 변형은 전부 음수 회전인데 3번만 Left도 양수(+8°)다. Figma 그대로 옮긴 값이며
    // 디자인 의도인지 누락인지는 확인 대기 중이다(스펙 열린 질문 2번).
    TYPE_3_LEFT(
        rotation = 8f,
        imageOffset = DpOffset(x = (-0.79).dp, y = (-10.79).dp),
        chipOffset = DpOffset(x = (-0.5).dp, y = 49.5.dp),
    ),
    TYPE_3_RIGHT(
        rotation = 8f,
        imageOffset = DpOffset(x = (-0.79).dp, y = (-10.79).dp),
        chipOffset = DpOffset(x = (-0.5).dp, y = 49.5.dp),
    ),
    TEMPLATE(
        rotation = 0f,
        imageOffset = DpOffset(x = (-0.79).dp, y = (-10.79).dp),
        chipOffset = DpOffset(x = (-0.5).dp, y = 49.5.dp),
    ),
}
```

- [x] **Step 6: 빌드·ktlint 확인**

Run: `./gradlew :core:designsystem:assembleDebug ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 대개 리소스명 오타이므로 Step 2의 `find` 결과와
`R.drawable.img_topping_template_*` 참조를 대조한다.

---

## Task 4: `YGToppingGroup` + 프리뷰 + 갤러리

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingGroup.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGToppingGroup.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToppingGroupPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: Task 2의 `YGGrouptagChip`·`YGGrouptagChipType`, Task 3의 `YGToppingImage`·
  `YGToppingTemplate`·`YGToppingGroupType`·`TOPPING_ERROR_DRAWABLE`, Task 1의
  `SizeTokens.Size96`·`Size160`
- Produces:
  `@Composable fun YGToppingGroup(image: YGToppingImage, name: String, timestamp: String, chipType: YGGrouptagChipType, type: YGToppingGroupType, modifier: Modifier = Modifier)`

- [x] **Step 1: 컴포넌트 작성**

설계 요점 4가지:

1. **클리핑 없음.** 회전·오프셋으로 프레임을 넘어가는 픽셀과 칩을 자르지 않는다(G-001 오버플로우 조항).
   `Modifier.rotate`는 `clip = false`인 graphicsLayer라 그대로 삐져나간다. `Box`에 `clipToBounds`를
   **걸지 않는다.**
2. **`onClick` 없음.** C-001 이동은 호출자가 `clickableYG`로 감싼다 — 그리드 셀의 터치 범위를 셀 쪽이
   결정해야 하고, 컴포넌트가 프레임 밖 칩까지 터치 범위에 넣을지 판단할 근거가 없다.
3. 이미지·칩 모두 `Alignment.Center` 정렬 후 `Modifier.offset`으로 중심 기준 배치한다(Figma가 center
   기준 좌표를 준다). `offset`은 레이아웃 배치를, `rotate`는 드로우를 담당하므로 `.offset().rotate()`
   순서로 건다.
4. `AsyncImage`의 `error` 파라미터에 물음표 드로어블을 지정하면 **Coil 로드 실패가 자동으로 Error
   그래픽으로 떨어진다.** 별도 상태 분기가 필요 없다.

`YGToppingGroup.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChip
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma `Topping-Group`. G-001 무한 파르페 그리드의 셀 1개.
 *
 * 160dp 정사각 프레임에 96dp 토핑을 [type]의 각도로 기울여 얹고, 우측 하단에 [YGGrouptagChip]을
 * 겹친다. 프레임을 넘어가는 픽셀은 자르지 않는다.
 *
 * 탭 처리는 하지 않는다 — 호출자가 `clickableYG`로 감싼다.
 *
 * @param image 토핑 영역에 그릴 콘텐츠 3상태. 어느 상태인지 판정은 호출자 책임이다.
 * @param type 배치 변형. Left/Right 선택과 번호 재부여는 호출자 책임이다.
 */
@Composable
fun YGToppingGroup(
    image: YGToppingImage,
    name: String,
    timestamp: String,
    chipType: YGGrouptagChipType,
    type: YGToppingGroupType,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(SizeTokens.Size160.getDp()),
    ) {
        val imageModifier = Modifier
            .size(SizeTokens.Size96.getDp())
            .offset(x = type.imageOffset.x, y = type.imageOffset.y)
            .rotate(type.rotation)

        when (image) {
            is YGToppingImage.Remote -> AsyncImage(
                model = image.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(TOPPING_ERROR_DRAWABLE),
                modifier = imageModifier,
            )

            is YGToppingImage.Template -> Image(
                painter = painterResource(image.type.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )

            YGToppingImage.Error -> Image(
                painter = painterResource(TOPPING_ERROR_DRAWABLE),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )
        }

        YGGrouptagChip(
            name = name,
            timestamp = timestamp,
            type = chipType,
            modifier = Modifier.offset(x = type.chipOffset.x, y = type.chipOffset.y),
        )
    }
}

@YGPreview
@Composable
private fun YGToppingGroupPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        YGToppingGroup(
            image = YGToppingImage.Template(YGToppingTemplate.TEMPLATE_01),
            name = "잠탈감금",
            timestamp = "3분전",
            chipType = YGGrouptagChipType.TYPE_1_2,
            type = YGToppingGroupType.TYPE_1_LEFT,
        )
        YGToppingGroup(
            image = YGToppingImage.Error,
            name = "팀장은연경이",
            timestamp = "3분전",
            chipType = YGGrouptagChipType.TYPE_5_6,
            type = YGToppingGroupType.TYPE_2_RIGHT,
        )
    }
}
```

- [x] **Step 2: 프리뷰 렌더 확인**

Android Studio에서 `YGToppingGroup.kt`의 `YGToppingGroupPreview`를 렌더한다.
Expected: 160dp 칸 2개. 위는 템플릿 그래픽이 −6° 기울고 칩이 아래쪽에 겹친다. 아래는 물음표
그래픽이 +16° 기운다. 칩이 프레임 밖으로 나가도 잘리지 않는다.

- [x] **Step 3: 갤러리 NavKey 작성**

`NavKeyYGToppingGroup.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGToppingGroup : NavKey
```

- [x] **Step 4: 갤러리 화면 작성**

배치 7변형과 콘텐츠 3상태를 모두 찍는다. `Remote` 성공 상태 확인이 이번 라운드에서 Coil 네트워크
페처 도입 효과를 검증하는 유일한 지점이므로 실제 접근 가능한 URL을 쓴다.

`YGToppingGroupPreviewScreen.kt`:

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
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingTemplate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val SAMPLE_TOPPING_URL =
    "https://raw.githubusercontent.com/coil-kt/coil/main/samples/shared/src/commonMain/composeResources/drawable/sample.jpg"

@Composable
internal fun YGToppingGroupPreviewScreen(
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
                PreviewSection("배치 7변형 (Template 그래픽 고정)") {
                    YGToppingGroupType.entries.forEach { type ->
                        YGToppingGroup(
                            image = YGToppingImage.Template(YGToppingTemplate.TEMPLATE_01),
                            name = type.name,
                            timestamp = "3분전",
                            chipType = YGGrouptagChipType.TYPE_1_2,
                            type = type,
                        )
                    }
                }
            }
            item {
                PreviewSection("Template 6종") {
                    YGToppingTemplate.entries.forEach { template ->
                        YGToppingGroup(
                            image = YGToppingImage.Template(template),
                            name = "잠탈감금",
                            timestamp = "3분전",
                            chipType = YGGrouptagChipType.TYPE_9_10,
                            type = YGToppingGroupType.TEMPLATE,
                        )
                    }
                }
            }
            item {
                PreviewSection("Remote 성공 / Remote 실패 / Error") {
                    YGToppingGroup(
                        image = YGToppingImage.Remote(SAMPLE_TOPPING_URL),
                        name = "정상 로딩",
                        timestamp = "3분전",
                        chipType = YGGrouptagChipType.TYPE_3_4,
                        type = YGToppingGroupType.TYPE_1_LEFT,
                    )
                    YGToppingGroup(
                        image = YGToppingImage.Remote("https://invalid.example/none.png"),
                        name = "로드 실패",
                        timestamp = "3분전",
                        chipType = YGGrouptagChipType.TYPE_7_8,
                        type = YGToppingGroupType.TYPE_1_RIGHT,
                    )
                    YGToppingGroup(
                        image = YGToppingImage.Error,
                        name = "조회 실패",
                        timestamp = "오래 전",
                        chipType = YGGrouptagChipType.TYPE_11_12,
                        type = YGToppingGroupType.TYPE_2_LEFT,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGToppingGroupPreviewScreen() = PreviewBox {
    YGToppingGroupPreviewScreen(
        onBack = {},
    )
}
```

- [x] **Step 5: 카탈로그 등록**

`ComponentCatalog.kt` — import를 알파벳 순 자리에 추가하고, `ComponentCategory.CONTAINER` 블록의
마지막(`YGCanvas` 뒤)에 엔트리를 넣는다. `CONTAINER`인 이유: 이미지와 칩을 합성하는 컨테이너로
`YGCanvas`와 성격이 같다.

```kotlin
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToppingGroup
```

```kotlin
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGToppingGroup",
        navKey = NavKeyYGToppingGroup,
    ),
```

- [x] **Step 6: 엔트리 배선**

`ComponentEntryBuilders.kt` — import 추가 후, Task 2에서 넣은 `entry<NavKeyYGGrouptagChip>` 블록
뒤에 추가한다.

```kotlin
    entry<NavKeyYGToppingGroup> {
        ScreenScaffold { modifier ->
            YGToppingGroupPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [x] **Step 7: 빌드·ktlint 확인**

Run: `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug ktlintCheck`
Expected: BUILD SUCCESSFUL

---

## Task 5: 전체 검증 + 문서 갱신

**Files:**
- Modify: (이 repo) `parfait/specs/2026-07-31-designsystem-grouptag-topping-components.md`
- Modify: (이 repo) `parfait/plans/2026-07-31-designsystem-grouptag-topping-components.md`
- Modify: (이 repo) `parfait/specs/README.md`, `parfait/plans/README.md`

**Interfaces:**
- Consumes: Task 1~4 전량
- Produces: 검증 결과와 as-built 차이가 반영된 스펙/계획 문서

- [x] **Step 1: repo 전체 빌드**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. 컨벤션 플러그인을 고쳤으므로 다른 모듈도 함께 확인한다.

- [x] **Step 2: repo 전체 ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: 실기기 갤러리 육안 검증**

`:app-preview`를 설치·실행하고 아래를 Figma와 대조한다. **네트워크 연결이 있는 상태**로 확인한다
(`Remote` 성공 검증에 필요).

`Text > YGGrouptagChip`:
- 타입 6종의 타임스탬프 색이 서로 다르고, 순서대로 연핑크 → 진핑크 → 체리 → 그레이 → 멜론 → 푸딩
- 이름 `팀장은 진짜 연경이야`가 `팀장은 진짜 연…`으로 잘림
- pill 배경이 반투명 검정, 이름과 시간 사이에 아주 작은 점 하나

`Container > YGToppingGroup`:
- 배치 7변형의 기울기가 각각 다름. **`TYPE_3_LEFT`와 `TYPE_3_RIGHT`는 같아 보이는 게 정상**
  (Figma가 동일값 — 스펙 열린 질문 2번)
- Template 6종이 각각 다른 그래픽(별·음표·소용돌이)
- `Remote 성공` 칸에 실제 사진이 뜸 → **네트워크 페처 도입이 동작한다는 증거**
- `Remote 실패`·`조회 실패` 칸에 동일한 물음표 그래픽
- 칩이 프레임 밖으로 나가도 잘리지 않음

- [x] **Step 4: 발견한 차이를 스펙에 반영**

Step 3에서 Figma와 어긋나는 점이 나오면 코드를 고치고, **설계와 달라진 부분은 스펙 문서 상단에
"구현 상태"·"설계에서 달라진 점" 절로 기록한다**(선행 캔버스 라운드와 같은 형식). 무엇을 고쳤는지가
아니라 **왜 설계대로 하면 안 됐는지**를 적는다.

- [x] **Step 5: 문서 상태 갱신**

- 스펙 frontmatter `status: draft` → `in-progress`, `verified`를 검증일로 갱신
- 계획 frontmatter `status: draft` → `in-progress`(전 Task 완료 시 `done`), `updated` 갱신
- `parfait/specs/README.md`·`parfait/plans/README.md`의 해당 행에 실행 결과 한 줄 추가

`archive/` 이동과 `architecture/design-system` as-built 갱신은 **develop 머지 후**로 미룬다.
코드가 커밋되지 않은 상태에서 문서만 완료로 넘기면 다음 라운드가 없는 코드를 전제하게 된다.

- [x] **Step 6: 미커밋 상태 보고**

Run: `git -C <TJYG-Android 경로> status --short`
Expected: 신규/수정 파일 목록이 나오고 커밋은 없음. 이 목록을 작업자에게 보고한다.

---

## 검증 요약

| Task | 산출물 | 검증 |
|---|---|---|
| 1 | Coil 네트워크 페처, `Size96`·`Size160` | `:core:designsystem:assembleDebug` + `ktlintCheck` |
| 2 | `YGGrouptagChip` + 타입 6종 + 갤러리 | 프리뷰 렌더 + 2모듈 빌드 |
| 3 | 에셋 42개, 모델 3종 | `find` 개수 42 + 빌드 |
| 4 | `YGToppingGroup` + 갤러리 | 프리뷰 렌더 + 2모듈 빌드 |
| 5 | 전체 통합 | repo 전체 빌드·ktlint + 실기기 육안 |

## 범위 밖 (건드리지 않는다)

- `Chip-Indicator`·`List-Date` — 다른 브랜치에서 작업 중
- `feature/groups/list` G-001 화면 실구현
- 템플릿 6종 랜덤 부여·영속 로직(feature/domain 책임)
- `YGColorChipType` 13종 + Plus ↔ 정책 12종 드리프트
- `YGCanvasBackground.Image` 검증 — Task 1의 부수 효과로 살아나지만 이 라운드의 검증 대상은 아니다
