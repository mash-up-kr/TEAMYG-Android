---
id: designsystem-text-component-sync
title: 디자인시스템 텍스트 영역 컴포넌트 Figma 동기화 (Design System Text Components Figma Sync)
status: done
type: work-order
created: 2026-07-27
updated: 2026-07-29
platforms: android
owner:
related_adr: ADR-0010
related_spec: designsystem-text-component-sync
related_code:
  - YGDate.kt#YGDate
  - YGToast.kt#YGToastType
  - YGAlertPolicy.kt#YGAlertPolicy
  - ComponentCatalog.kt#componentCatalog
archived_reason: PR #181 develop 머지 완료(2026-07-29) — Task 1~6 전량 수행, 코드=설계 일치
tags: [plan, parfait, designsystem, figma-sync]
---

# 디자인시스템 텍스트 영역 컴포넌트 Figma 동기화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Figma `Components > Detail Type "텍스트"` 영역(Label·Date·Toast·Alert)의 코드 드리프트를 제거하고, `YGToast`·`YGAlert`을 `:app-preview` 컴포넌트 갤러리에 등록한다.

**Architecture:** `core:designsystem`의 기존 컴포넌트 4종을 제자리 수정한다. 새 모듈·새 추상화·새 ADR 없음. `YGToast`에 sealed 분기 1개(`Fail`)를 추가하고, `YGAlertPolicy`에 기본값 `null`인 파라미터 2개를 더해 기존 호출 형태를 깨지 않는다. `:app-preview`는 기존 Navigation3 + Hilt `@IntoSet` 갤러리 패턴을 그대로 확장한다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3, Hilt, Gradle(컨벤션 플러그인), ktlint

**Spec:** [`parfait/specs/2026-07-27-designsystem-text-component-sync.md`](../../specs/archive/2026-07-27-designsystem-text-component-sync.md)

## Global Constraints

- 작업 디렉토리는 **`TJYG-Android` repo**(별도 repo). 브랜치는 이미 `feature/sync-design-system-text-component`, 기준 커밋 `2225bb33`.
- 코드·식별자·파일명·커밋 메시지는 **영어**. 문서·주석 내용은 한국어. (`TJYG-Android/CLAUDE.md`)
- 커밋 메시지 컨벤션: 드리프트 교정은 `refactor: sync <Component>`, 신규 추가는 `feat: ...`. (선례 PR #159)
- **이 repo에는 테스트 인프라가 없다** — `test/`·`androidTest/` 소스셋이 한 모듈에도 존재하지 않는다.
  따라서 각 Task의 검증은 **컴파일 + ktlint + `@YGPreview` 렌더 + 갤러리 앱 실행**으로 한다.
  이는 기존 컴포넌트 plan들과 동일한 방식이다(`2026-07-19-app-setting-s001` 등 "테스트 없음(compile+ktlint+프리뷰 검증)").
  테스트 인프라 도입은 이 작업의 범위가 아니다.
- 색은 `YGAtomicColors`, 치수는 `YGTheme.layout.padding` / `YGTheme.layout.gap`, 타이포는 `YGTheme.typography`를 쓴다. **hex 리터럴·raw dp 신규 도입 금지** (기존 `0.75.dp` 테두리 두께는 토큰이 없어 그대로 둔다).
- 토큰 값(참조용, 코드에 숫자로 쓰지 말 것): `padding3`=8, `padding4`=10, `padding5`=12, `padding6`=16, `padding7`=20, `gap2`=4, `gap3`=8.
- 베이스라인은 green이다 — `:core:designsystem:compileDebugKotlin`, `:app-preview:compileDebugKotlin`, 양 모듈 `ktlintCheck` 모두 통과 확인됨(2026-07-27).
- **`YGLabel.kt`는 수정하지 않는다** — Figma와 일치한다.
- **`YGChipButton`은 수정하지 않는다** — 세로 패딩 드리프트는 칩 영역 sync로 이월(open-questions 등록됨).

## File Structure

### `core:designsystem` (수정만, 신규 파일 없음)

| 파일 | 책임 | 변경 |
|---|---|---|
| `component/ygtext/YGDate.kt` | 날짜 라벨 프리셋 | 배경·`modifier` 배선·간격·프리뷰 |
| `component/ygtext/YGLabel.kt` | 보조 라벨 프리셋 | **변경 없음** |
| `component/ygtoast/YGToast.kt` | 토스트 렌더 + `YGToastType` | 패딩·`Fail` 타입·KDoc·프리뷰 |
| `component/ygtoast/YGToastPolicy.kt` | 토스트 노출 정책 | 호스트 프리뷰(Task 5b) — 런타임 무변경 (`YGToastType`을 그대로 전달) |
| `component/ygalert/YGAlert.kt` | 알럿 배너 렌더 | 프리뷰만 (런타임 무변경) |
| `component/ygalert/YGAlertPolicy.kt` | 알럿 노출 정책 | 버튼 변형 전달 경로 + 호스트 프리뷰(Task 5b) |

### `:app-preview`

| 파일 | 책임 | 변경 |
|---|---|---|
| `navigation/key/NavKeyYGToast.kt` | 토스트 showcase 목적지 키 | **신규** |
| `navigation/key/NavKeyYGAlert.kt` | 알럿 showcase 목적지 키 | **신규** |
| `screen/component/YGToastPreviewScreen.kt` | 토스트 정적 4변형 + 정책 트리거 | **신규** |
| `screen/component/YGAlertPreviewScreen.kt` | 알럿 정적 2변형 + 정책 트리거 | **신규** |
| `model/ComponentCatalog.kt` | 카테고리별 컴포넌트 목록 | `TEXT`에 2줄 추가 |
| `navigation/entry/ComponentEntryBuilders.kt` | NavKey → 화면 배선 | `entry` 2블록 추가 |
| `navigation/di/ComponentEntryModule.kt` | `@IntoSet` 바인딩 | **변경 없음** (함수 단위 바인딩) |

NavKey는 파일당 1개 — 기존 17개 키 파일의 관용구를 따른다.

---

### Task 1: YGDate — 배경·modifier 배선·간격·프리뷰

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGDate.kt`

**Interfaces:**
- Consumes: 없음 (첫 Task)
- Produces: `YGDate(date: String, day: String, modifier: Modifier = Modifier)` — **공개 시그니처 무변경**. Task 6의 갤러리 검증이 이 컴포저블을 렌더한다.

수정 근거 4건:
- **D1** Figma `Date` 노드에 `color/base/white` 채움이 있는데 코드에 배경이 없다 → `YGAtomicColors.Gray.White`(`#FAFAFA`).
- **D2** 호출자 `modifier`가 `Row`가 아니라 **두 번째 `Text`**에 붙어 있다. 호출자가 `Modifier.padding()`을 주면 날짜 박스가 아니라 `(Wed)` 텍스트만 밀린다.
- **D3** 텍스트 간격이 두 번째 `Text`의 `start` 패딩으로 표현돼 있다. 값(8)은 Figma `gap-3`과 같지만 `Arrangement`가 맞다.
- **D4** 프리뷰가 `@YGPreview` + `YGCustomTheme`이다. 규약은 `@YGPreview` + `PreviewBox`(`YGLabel.kt` 참고).

modifier 체인 순서는 `background` → `border` → `padding`이다. `background`를 `border` 앞에 둬야 테두리가 배경 위에 그려지고, 둘 다 `padding` 위에 있어야 패딩이 안쪽 여백으로 동작한다.

- [x] **Step 1: `YGDate.kt` 전체를 아래 내용으로 교체**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGDate(
    date: String,
    day: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White)
            .border(width = 0.75.dp, color = YGAtomicColors.Gray.Gray800)
            .padding(
                vertical = YGTheme.layout.padding.padding3,
                horizontal = YGTheme.layout.padding.padding4,
            ),
    ) {
        Text(
            text = date,
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray800,
        )
        Text(
            text = "($day)",
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray300,
        )
    }
}

@YGPreview
@Composable
private fun YGDatePreview() = PreviewBox {
    YGDate(
        date = "December 31",
        day = "Wed",
    )
}
```

바뀐 점: `background` 추가, `Modifier` 리터럴 → `modifier` 파라미터, `Arrangement.spacedBy`, 두 `Text`에서 `modifier` 인자 제거, `"(" + day + ")"` → `"($day)"` 문자열 템플릿, `YGCustomTheme` → `PreviewBox`. `YGCustomTheme` import는 제거된다.

- [x] **Step 2: 컴파일 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 3: ktlint 확인**

Run: `./gradlew :core:designsystem:ktlintCheck --console=plain`
Expected: `BUILD SUCCESSFUL`. 실패하면 `./gradlew :core:designsystem:ktlintFormat` 후 재실행.

- [x] **Step 4: 프리뷰 육안 확인**

Android Studio에서 `YGDate.kt`의 `YGDatePreview`를 연다.
Expected: 흰(`#FAFAFA`) 배경 위에 회색 테두리 박스, `December 31` (진회색) + `(Wed)` (연회색), 두 텍스트 사이 8dp.
night 프리뷰에서도 배경이 흰색이어야 한다 — Figma가 고정 흰 채움이므로 테마 반전은 없다.

- [x] **Step 5: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGDate.kt
git commit -m "refactor: sync YGDate with Figma text components"
```

---

### Task 2: YGToast — 패딩 교정 + Fail 타입 + 프리뷰

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoast/YGToast.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `YGToastType.Fail(text: String)` — Task 5의 갤러리 화면과 Task 3의 정책 경로가 참조한다.
  - 기존 `YGToastType.InviteCode(text)`, `YGToastType.Edit(text)`, `YGToastType.Record(userName, time)` 유지.
  - `YGToast(type: YGToastType, modifier: Modifier = Modifier)` — 시그니처 무변경.

수정 근거 3건:
- **T1** Figma는 가로 `padding-6`(16) / 세로 `padding-5`(12)인데 코드는 가로 `padding4`(10) / 세로 `padding6`(16)로 방향이 뒤바뀌어 있다. 그 결과 토스트 높이가 Figma 변형(45)보다 커져 있다.
- **T2** Figma `Toast / Type=Error` 변형에 대응하는 타입이 없다. `Cherry500` 단색 + 완성 문장 주입형으로 추가한다(`InviteCode`·`Edit`과 동일 규약).
- **T3** 타입명은 리네임하지 않는다 — Figma의 `Type=Alert`을 그대로 쓰면 `YGAlert` 컴포넌트와 충돌한다. 대신 KDoc으로 Figma 변형명을 병기해 추적한다.
- **T4** 프리뷰가 `@Preview` + `YGCustomTheme`이다 → `@YGPreview` + `PreviewBox`.

`YGToastType`은 sealed interface이므로 `Fail` 추가 시 `when`이 non-exhaustive가 되어 컴파일러가 분기 누락을 잡아준다.

- [x] **Step 1: `YGToast.kt` 전체를 아래 내용으로 교체**

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

sealed interface YGToastType {
    /** Figma `Toast / Type=Success` */
    data class InviteCode(val text: String) : YGToastType

    /** Figma `Toast / Type=Warning` */
    data class Edit(val text: String) : YGToastType

    /** Figma `Toast / Type=Alert` */
    data class Record(val userName: String, val time: String) : YGToastType

    /** Figma `Toast / Type=Error` */
    data class Fail(val text: String) : YGToastType
}

@Composable
fun YGToast(
    type: YGToastType,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .background(color = YGAtomicColors.Transparency.Black75)
            .padding(
                vertical = YGTheme.layout.padding.padding5,
                horizontal = YGTheme.layout.padding.padding6,
            ),
    ) {
        when (type) {
            is YGToastType.InviteCode -> Text(
                text = type.text,
                style = YGTheme.typography.body.b02SB,
                color = YGAtomicColors.Melon.Melon600,
            )

            is YGToastType.Edit -> Text(
                text = type.text,
                style = YGTheme.typography.body.b02SB,
                color = YGAtomicColors.Pudding.Pudding600,
            )

            is YGToastType.Fail -> Text(
                text = type.text,
                style = YGTheme.typography.body.b02SB,
                color = YGAtomicColors.Cherry.Cherry500,
            )

            is YGToastType.Record -> {
                val userStyle = YGTheme.typography.body.b02SB
                val timeStyle = YGTheme.typography.body.b02R
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            userStyle.toSpanStyle().copy(color = YGAtomicColors.Pudding.Pudding500),
                        ) { append(type.userName) }
                        withStyle(
                            timeStyle.toSpanStyle().copy(color = YGAtomicColors.Gray.Gray100),
                        ) { append("님이 ${type.time} 전에 쌓았어요") }
                    },
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun YGToastPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        YGToast(type = YGToastType.Record(userName = "WWWWWWWWWW", time = "59분"))
        YGToast(type = YGToastType.Edit("내 토핑만 편집할 수 있어요"))
        YGToast(type = YGToastType.InviteCode("초대 코드를 복사했어요"))
        YGToast(type = YGToastType.Fail("갤러리 저장에 실패했어요. 나중에 다시 시도해 주세요."))
    }
}
```

`Record` 분기는 손대지 않는다 — 한국어 문구 하드코딩은 open-questions로 이월된 별건이다.
`androidx.compose.ui.tooling.preview.Preview`와 `YGCustomTheme` import는 제거된다.

- [x] **Step 2: 컴파일 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`. `YGToastPolicy.kt`는 `YGToastType`을 그대로 전달만 하므로 수정 없이 통과해야 한다 — 여기서 에러가 나면 정책 쪽에 숨은 `when` 분기가 있다는 뜻이니 그 분기에도 `Fail`을 추가한다.

- [x] **Step 3: ktlint 확인**

Run: `./gradlew :core:designsystem:ktlintCheck --console=plain`
Expected: `BUILD SUCCESSFUL`. 실패 시 `ktlintFormat` 후 재실행.

- [x] **Step 4: 프리뷰 육안 확인**

Android Studio에서 `YGToastPreview`를 연다.
Expected: 검정 반투명 배너 4개. 위에서부터 노랑 이름 + 연회색 본문 / 형광노랑 / 민트 / **빨강(Cherry500)**.
높이가 이전보다 **줄고** 좌우 여백은 **늘었어야** 한다 — 이게 T1 교정의 눈에 보이는 증거다.

- [x] **Step 5: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoast/YGToast.kt
git commit -m "refactor: sync YGToast padding and add Fail type"
```

---

### Task 3: YGAlert — 프리뷰 규약 + 정책 버튼 변형 전달

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygalert/YGAlert.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygalert/YGAlertPolicy.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `YGAlertPolicy.show(title: String, sub: String, buttonText: String? = null, onButtonClick: (() -> Unit)? = null)` — Task 6의 갤러리 트리거가 호출한다.
  - `YGAlertItem(id: String, title: String, sub: String, buttonText: String? = null, onButtonClick: (() -> Unit)? = null, visible: Boolean = true)`
  - `YGAlertHost(policy: YGAlertPolicy, modifier: Modifier = Modifier)` — 시그니처 무변경.
  - `YGAlert(title, sub, modifier, buttonText, onButtonClick)` — **시그니처·구현 모두 무변경**.

수정 근거 2건:
- **A1** 프리뷰가 `@Preview` + `YGCustomTheme`이다 → `@YGPreview` + `PreviewBox`.
- **A3** Figma Alert은 버튼 유/무 2변형인데 `YGAlertPolicy.show(title, sub)`가 버튼 인자를 안 받아 호스트로는 버튼 변형을 띄울 수 없다. 기본값 `null` 파라미터 2개를 더해 기존 호출 형태를 유지한 채 경로를 연다.

`YGAlert` 컴포저블 본문은 Figma와 대조 결과 일치하므로 건드리지 않는다. 버튼 노출 분기(`buttonText != null`)는 이미 `YGAlert` 안에 있으므로 호스트에 조건 분기를 두지 않고 값만 흘린다.

- [x] **Step 1: `YGAlert.kt`의 프리뷰 블록 교체**

파일 하단의 프리뷰를 아래로 바꾼다.

```kotlin
@YGPreview
@Composable
private fun YGAlertPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        YGAlert(
            title = "Title",
            sub = "Sub",
            buttonText = "Text",
            onButtonClick = {},
        )
        YGAlert(
            title = "Title",
            sub = "Sub",
        )
    }
}
```

import 조정: `androidx.compose.ui.tooling.preview.Preview`와 `com.teamyg.parfait.core.designsystem.theme.YGCustomTheme`를 제거하고 아래 둘을 추가한다.

```kotlin
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
```

버튼 텍스트를 기존 `"확인"`에서 Figma 노드와 같은 `"Text"`로 맞춘다. `Column`·`Arrangement`·`dp` import는 이미 있으므로 유지한다.

- [x] **Step 2: `YGAlertPolicy.kt`의 `YGAlertItem`에 필드 2개 추가**

```kotlin
data class YGAlertItem(
    val id: String,
    val title: String,
    val sub: String,
    val buttonText: String? = null,
    val onButtonClick: (() -> Unit)? = null,
    val visible: Boolean = true,
)
```

- [x] **Step 3: `YGAlertPolicy.show`가 두 값을 받아 전달하도록 수정**

```kotlin
fun show(
    title: String,
    sub: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    alert = YGAlertItem(
        id = UUID.randomUUID().toString(),
        title = title,
        sub = sub,
        buttonText = buttonText,
        onButtonClick = onButtonClick,
    )
}
```

- [x] **Step 4: `YGAlertHost`가 두 값을 `YGAlert`에 넘기도록 수정**

`YGAlertHost` 안의 `YGAlert(...)` 호출을 아래로 바꾼다. `modifier` 인자(draggable + offset 체인)는 기존 그대로 둔다.

```kotlin
YGAlert(
    title = alert.title,
    sub = alert.sub,
    buttonText = alert.buttonText,
    onButtonClick = alert.onButtonClick,
    modifier = Modifier
        .draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                if (delta < 0) dragOffsetY += delta
            },
            onDragStopped = { velocity ->
                if (dragOffsetY < SWIPE_DISMISS_THRESHOLD || velocity < FLING_DISMISS_VELOCITY) {
                    dragOffsetY = 0f
                    scope.launch {
                        policy.clearAlert()
                    }
                } else {
                    dragOffsetY = 0f
                }
            },
        ).offset { IntOffset(0, dragOffsetY.toInt()) },
)
```

- [x] **Step 5: 컴파일 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 6: ktlint 확인**

Run: `./gradlew :core:designsystem:ktlintCheck --console=plain`
Expected: `BUILD SUCCESSFUL`. 실패 시 `ktlintFormat` 후 재실행.

- [x] **Step 7: 프리뷰 육안 확인**

Android Studio에서 `YGAlertPreview`를 연다.
Expected: 검정 반투명 배너 2개. 위쪽은 분홍 `Title` + 반투명 흰 `Sub` + 우측에 연분홍 pill 칩(`Text` + 오른쪽 캐럿), 아래쪽은 칩 없음.

- [x] **Step 8: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygalert/
git commit -m "feat: pass alert button variant through YGAlertPolicy"
```

---

### Task 4: 갤러리 — YGToast showcase

**Files:**
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGToast.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToastPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: Task 2의 `YGToastType.Fail(text)` 및 기존 3타입, `YGToast(type, modifier)`, 기존 `rememberYGToastPolicy()` / `YGToastHost(policy, modifier)` / `YGToastPolicy.show(type)`
- Produces: `NavKeyYGToast` (`data object`, `NavKey` 구현) — Task 6의 갤러리 실행 검증에서 진입 대상

화면은 `Box` 루트에 본문 `Column`을 깔고 `YGToastHost`를 상단에 오버레이한다. 정적 4변형은 색을 천천히 검수하는 용도이고, 트리거 버튼은 스와이프·2초 자동 소멸을 확인하는 용도다. (가로 패딩은 정적 섹션의 `contentPadding` 16dp에 가려지므로 트리거로 띄운 배너에서 봐야 한다.)

- [x] **Step 1: NavKey 생성**

`NavKeyYGToast.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGToast : NavKey
```

- [x] **Step 2: showcase 화면 생성**

`YGToastPreviewScreen.kt`:

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToast
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private data class YGToastSample(
    val label: String,
    val type: YGToastType,
)

private val ygToastSamples: List<YGToastSample> = listOf(
    YGToastSample(
        label = "Record (Figma Type=Alert)",
        type = YGToastType.Record(userName = "WWWWWWWWWW", time = "59분"),
    ),
    YGToastSample(
        label = "Edit (Figma Type=Warning)",
        type = YGToastType.Edit("내 토핑만 편집할 수 있어요"),
    ),
    YGToastSample(
        label = "InviteCode (Figma Type=Success)",
        type = YGToastType.InviteCode("초대 코드를 복사했어요"),
    ),
    YGToastSample(
        label = "Fail (Figma Type=Error)",
        type = YGToastType.Fail("갤러리 저장에 실패했어요. 나중에 다시 시도해 주세요."),
    ),
)

@Composable
internal fun YGToastPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toastPolicy = rememberYGToastPolicy()

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarBack(onIconClick = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ygToastSamples) { sample ->
                    PreviewSection(sample.label) {
                        YGToast(type = sample.type)
                    }
                }
                items(ygToastSamples) { sample ->
                    PreviewSection("show: ${sample.label}") {
                        YGButton(
                            text = "띄우기",
                            buttonType = YGButtonType.Medium.Primary,
                            isEnabled = true,
                            onClick = { toastPolicy.show(sample.type) },
                        )
                    }
                }
            }
        }
        YGToastHost(
            policy = toastPolicy,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGToastPreviewScreen() = PreviewBox {
    YGToastPreviewScreen(
        onBack = {},
    )
}
```

`YGToastHost`의 `modifier`는 기본값이 없으므로 반드시 넘긴다.

- [x] **Step 3: 카탈로그에 등록**

`ComponentCatalog.kt`의 import 목록에 알파벳 순서에 맞춰 추가:

```kotlin
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToast
```

`componentCatalog` 리스트에서 `YGActionItem` 항목 **뒤**, `YGModalPopup`(CONTAINER 첫 항목) **앞**에 삽입:

```kotlin
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGToast",
        navKey = NavKeyYGToast,
    ),
```

- [x] **Step 4: EntryBuilder에 배선**

`ComponentEntryBuilders.kt`의 import 목록에 2줄 추가:

```kotlin
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToast
import com.teamyg.parfait.preview.screen.component.YGToastPreviewScreen
```

`componentEntryBuilders` 함수 안, `entry<NavKeyYGActionItem> { … }` 블록 **뒤**에 추가:

```kotlin
    entry<NavKeyYGToast> {
        ScreenScaffold { modifier ->
            YGToastPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

`ComponentEntryModule.kt`는 건드리지 않는다 — `@IntoSet` 바인딩이 `componentEntryBuilders` 함수 단위다.

- [x] **Step 5: 컴파일 확인**

Run: `./gradlew :app-preview:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 6: ktlint 확인**

Run: `./gradlew :app-preview:ktlintCheck --console=plain`
Expected: `BUILD SUCCESSFUL`. 실패 시 `ktlintFormat` 후 재실행.

- [x] **Step 7: 커밋**

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGToast.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToastPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt
git commit -m "feat: add YGToast showcase to component gallery"
```

---

### Task 5: 갤러리 — YGAlert showcase

**Files:**
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGAlert.kt`
- Create: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGAlertPreviewScreen.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt`
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt`

**Interfaces:**
- Consumes: Task 3의 `YGAlertPolicy.show(title, sub, buttonText, onButtonClick)`, 기존 `YGAlert(title, sub, modifier, buttonText, onButtonClick)` / `rememberYGAlertPolicy()` / `YGAlertHost(policy, modifier)`
- Produces: `NavKeyYGAlert` (`data object`, `NavKey` 구현) — Task 6의 갤러리 실행 검증에서 진입 대상

Task 4와 같은 골격이되 변형이 2개(버튼 유/무)라 리스트 대신 `item` 블록 4개를 쓴다. 트리거 두 개가 각각 Task 3에서 연 버튼 전달 경로와 기존 경로를 실제로 태운다.

- [x] **Step 1: NavKey 생성**

`NavKeyYGAlert.kt`:

```kotlin
package com.teamyg.parfait.preview.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NavKeyYGAlert : NavKey
```

- [x] **Step 2: showcase 화면 생성**

`YGAlertPreviewScreen.kt`:

```kotlin
package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlert
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlertHost
import com.teamyg.parfait.core.designsystem.component.ygalert.rememberYGAlertPolicy
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val YG_ALERT_SAMPLE_TITLE = "Title"
private const val YG_ALERT_SAMPLE_SUB = "Sub"
private const val YG_ALERT_SAMPLE_BUTTON = "Text"

@Composable
internal fun YGAlertPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alertPolicy = rememberYGAlertPolicy()

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarBack(onIconClick = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    PreviewSection("with button") {
                        YGAlert(
                            title = YG_ALERT_SAMPLE_TITLE,
                            sub = YG_ALERT_SAMPLE_SUB,
                            buttonText = YG_ALERT_SAMPLE_BUTTON,
                            onButtonClick = {},
                        )
                    }
                }
                item {
                    PreviewSection("without button") {
                        YGAlert(
                            title = YG_ALERT_SAMPLE_TITLE,
                            sub = YG_ALERT_SAMPLE_SUB,
                        )
                    }
                }
                item {
                    PreviewSection("show: with button") {
                        YGButton(
                            text = "띄우기",
                            buttonType = YGButtonType.Medium.Primary,
                            isEnabled = true,
                            onClick = {
                                alertPolicy.show(
                                    title = YG_ALERT_SAMPLE_TITLE,
                                    sub = YG_ALERT_SAMPLE_SUB,
                                    buttonText = YG_ALERT_SAMPLE_BUTTON,
                                    onButtonClick = {},
                                )
                            },
                        )
                    }
                }
                item {
                    PreviewSection("show: without button") {
                        YGButton(
                            text = "띄우기",
                            buttonType = YGButtonType.Medium.Primary,
                            isEnabled = true,
                            onClick = {
                                alertPolicy.show(
                                    title = YG_ALERT_SAMPLE_TITLE,
                                    sub = YG_ALERT_SAMPLE_SUB,
                                )
                            },
                        )
                    }
                }
            }
        }
        YGAlertHost(
            policy = alertPolicy,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGAlertPreviewScreen() = PreviewBox {
    YGAlertPreviewScreen(
        onBack = {},
    )
}
```

- [x] **Step 3: 카탈로그에 등록**

`ComponentCatalog.kt`의 import 목록에 알파벳 순서에 맞춰 추가:

```kotlin
import com.teamyg.parfait.preview.navigation.key.NavKeyYGAlert
```

`componentCatalog` 리스트에서 Task 4가 넣은 `YGToast` 항목 **뒤**에 삽입:

```kotlin
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGAlert",
        navKey = NavKeyYGAlert,
    ),
```

- [x] **Step 4: EntryBuilder에 배선**

`ComponentEntryBuilders.kt`의 import 목록에 2줄 추가:

```kotlin
import com.teamyg.parfait.preview.navigation.key.NavKeyYGAlert
import com.teamyg.parfait.preview.screen.component.YGAlertPreviewScreen
```

Task 4가 넣은 `entry<NavKeyYGToast> { … }` 블록 **뒤**에 추가:

```kotlin
    entry<NavKeyYGAlert> {
        ScreenScaffold { modifier ->
            YGAlertPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
```

- [x] **Step 5: 컴파일 확인**

Run: `./gradlew :app-preview:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 6: ktlint 확인**

Run: `./gradlew :app-preview:ktlintCheck --console=plain`
Expected: `BUILD SUCCESSFUL`. 실패 시 `ktlintFormat` 후 재실행.

- [x] **Step 7: 커밋**

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/key/NavKeyYGAlert.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGAlertPreviewScreen.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/model/ComponentCatalog.kt \
        app-preview/src/main/kotlin/com/teamyg/parfait/preview/navigation/entry/ComponentEntryBuilders.kt
git commit -m "feat: add YGAlert showcase to component gallery"
```

---

### Task 5b: 호스트 프리뷰 신설 (계획 후 추가, 2026-07-27)

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygalert/YGAlertPolicy.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoast/YGToastPolicy.kt`

**Interfaces:**
- Consumes: Task 2·3의 산출물(`Fail` 타입, `show`의 버튼 인자)
- Produces: `YGAlertHostPreview`·`YGToastHostPreview` (둘 다 `private`) — 갤러리 없이 IDE에서 호스트 렌더 확인용

원 계획에는 없던 Task다. 정책 파일 2개에 프리뷰가 아예 없어(스펙 P1·P2) 갤러리를 띄우지 않으면
호스트 렌더를 볼 수 없었다. 다른 컴포넌트 파일과 같은 `@YGPreview` + `PreviewBox` 규약으로 채웠다.

- [x] **Step 1: `YGAlertPolicy.kt`에 `YGAlertHostPreview` 추가**

`remember { YGAlertPolicy().apply { show(…) } }`로 정책 인스턴스 2개(버튼 유/무)를 만들어
`Column(verticalArrangement = Arrangement.spacedBy(10.dp))`에 호스트 2개로 나열한다.

`rememberYGAlertPolicy()` + `LaunchedEffect { show(…) }`로 쓰지 말 것 — **정적 프리뷰는
`LaunchedEffect`를 실행하지 않아** `alert`이 `null`인 채 빈 화면이 렌더된다. `apply`로 최초
컴포지션 시점에 상태를 채워야 한다. 같은 이유로 호스트의 자동 소멸 `LaunchedEffect`도 돌지 않아
프리뷰에 배너가 계속 남는다(의도).

- [x] **Step 2: `YGToastPolicy.kt`에 `YGToastHostPreview` 추가**

`YGToastType` 4종을 `listOf`로 두고 `forEach`로 **타입마다 정책 1개 + 호스트 1개**를 세로로 나열한다.
호스트 1개에 4건을 `show`하면 컨테이너가 `Box`라 같은 원점에 겹쳐 그려져 맨 위 1건만 보인다
(스택 결함 — [open-questions](../../../synthesis/open-questions.md) [2026-07-27] 등록분).

- [x] **Step 3: 컴파일 + ktlint 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: `BUILD SUCCESSFUL` (2026-07-27 확인).

- [x] **Step 4: 커밋** — `feat: 프리뷰 앱 변경점 현행화`(`03f00c1f`)에 포함.

---

### Task 6: 전체 검증 + 스펙 상태 갱신

**Files:**
- Modify: `parfait/specs/2026-07-27-designsystem-text-component-sync.md` (**위키 repo**)
- Modify: `parfait/specs/README.md` (**위키 repo**)
- Modify: `parfait/plans/2026-07-27-designsystem-text-component-sync.md` (**위키 repo**)

**Interfaces:**
- Consumes: Task 1~5·5b의 모든 산출물
- Produces: 없음 (종결 Task)

여기부터는 `TJYG-Android`가 아니라 **위키 repo**(`team-yg-pesonal-agent`) 작업이다. 두 repo의 커밋을 섞지 않는다.

- [x] **Step 1: 전체 빌드 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin :app-preview:assembleDebug ktlintCheck --console=plain`
Expected: `BUILD SUCCESSFUL`. `:app` 모듈도 `core:designsystem`에 의존하므로 `ktlintCheck`(루트 태스크)가 전 모듈을 훑는다.

- [x] **Step 2: 갤러리 앱 실기기/에뮬레이터 실행 검증**

`:app-preview`를 설치·실행하고 메인 목록의 **Text** 그룹을 연다.

Expected:
1. Text 그룹에 `YGLabel`·`YGDate`·`YGActionItem`·`YGToast`·`YGAlert` 5개가 보인다.
2. `YGDate` — 흰 배경 + 회색 테두리 박스.
3. `YGToast` — 정적 4변형(노랑이름/형광노랑/민트/빨강)이 보이고, "띄우기" 4개가 각각 배너를 띄운다 → **2초 후 자동 소멸**. 위로 스와이프하면 즉시 닫힌다.
4. `YGAlert` — 정적 2변형(칩 유/무)이 보이고, "show: with button" 트리거가 **칩이 있는** 배너를, "show: without button"이 **칩이 없는** 배너를 띄운다. 칩을 탭하면 `clicked` 배너로 교체된다(A3 콜백 전달 확인). **2.5초 후 자동 소멸**, 위로 스와이프 시 즉시 닫힘.

> **기존 결함 2건 — 이번 브랜치 소관 아님, 발견해도 재보고 불필요** (최종 리뷰에서 확인, [open-questions](../../../synthesis/open-questions.md) [2026-07-27] 등록):
> - **슬라이드 인/아웃이 안 난다.** 토스트·알럿 모두 그냥 나타났다 사라진다. 호스트의 `AnimatedVisibility` 배선 결함.
> - **토스트를 2초 안에 연달아 띄우면 쌓이지 않고 겹친다.** `Black75`가 중첩돼 어두워지고 최신 것이 아래 깔린다. 호스트가 `Box`라서 그렇다.
>
> **T1(가로 패딩) 검수는 반드시 트리거로 띄운 배너에서 할 것** — 정적 섹션은 `contentPadding` 16dp 안에 들어가 좌우 여백이 가려진다.

4번의 "칩이 있는 배너가 실제로 뜬다"가 Task 3(A3)의 회귀 검증 포인트다 — 수정 전에는 호스트로 칩 변형을 띄울 방법이 없었다.

- [x] **Step 3: Figma 최종 대조**

Figma 텍스트 영역(`Label`·`Date`·`Toast` 4변형·`Alert`)과 갤러리 화면을 나란히 놓고 색·패딩·문구를 확인한다.
남은 차이가 있으면 이 계획에 Task를 추가하거나, 범위 밖이면 [parfait open-questions](../../../synthesis/open-questions.md)에 등록한다.

- [x] **Step 4: 스펙 상태를 `implemented`로 바꾸고 아카이브**

위키 repo에서:
1. `parfait/specs/2026-07-27-designsystem-text-component-sync.md`의 frontmatter를 `status: implemented`로 바꾸고 `verified`를 실제 검증일로 갱신한다.
2. 파일을 `parfait/specs/archive/`로 옮긴다.
3. `parfait/specs/README.md`에서 해당 줄을 활성 테이블 → 아카이브 테이블로 옮기고 링크 경로에 `archive/`를 넣는다. 머지된 PR 번호를 한 줄 요약에 덧붙인다.

- [x] **Step 5: 계획 상태를 `done`으로 바꾸고 아카이브**

위키 repo에서:
1. 이 계획 파일의 frontmatter를 `status: done`으로 바꾸고 `updated`를 갱신한다.
2. `parfait/plans/archive/`로 옮긴다.
3. `parfait/plans/README.md`의 아카이브 테이블에 한 줄 등록한다(활성 테이블에는 애초에 이 줄이 있어야 하므로 이동시킨다).

- [x] **Step 6: 두 repo 각각 커밋**

`TJYG-Android`에 잔여 변경이 있으면 먼저 커밋한다. 그 다음 위키 repo에서:

```bash
git add parfait/specs parfait/plans
git commit -m "docs(parfait): text component Figma sync 스펙·계획 아카이브"
```

**커밋·푸시·PR 생성 전에는 사용자 확인을 받는다** (루트 `CLAUDE.md` Git 워크플로).

---

## 검증 요약

| 단계 | 명령 | 기대 |
|---|---|---|
| 컴파일 | `./gradlew :core:designsystem:compileDebugKotlin :app-preview:compileDebugKotlin` | BUILD SUCCESSFUL |
| 린트 | `./gradlew ktlintCheck` | BUILD SUCCESSFUL |
| 패키징 | `./gradlew :app-preview:assembleDebug` | BUILD SUCCESSFUL |
| 프리뷰 | Android Studio `@YGPreview` 8종(YGDate·YGToast·YGAlert + 호스트 2 + 갤러리 화면 2) | 위 각 Task의 Expected |
| 실행 | `:app-preview` 설치 후 Text 그룹 | Task 6 Step 2의 4항목 |

## 주의

- **테스트 코드는 작성하지 않는다.** 이 repo에 테스트 소스셋·러너·의존이 전혀 없다. 도입은 별도 작업이다.
- `Fail` 타입 추가로 `when(type)`이 non-exhaustive가 되는 곳이 있으면 컴파일러가 잡는다. 현재 `YGToastPolicy`는 `YGToastType`을 전달만 하므로 영향이 없어야 한다.
- `YGAlertItem`에 함수 타입 필드가 생기면서 이 data class는 Compose 기준 unstable이 된다. `mutableStateOf`에 담겨 호스트 내부에서만 읽히고 컴포저블 파라미터로 넘어가지 않으므로 실질 영향은 없다. 갤러리에서 알럿이 뜨는데 리컴포지션이 튀는 증상이 보이면 그때 `@Immutable`을 검토한다.
- 갤러리 화면의 `YGToastHost`/`YGAlertHost`는 `YGTopBarBack` 위를 덮는다. 실제 사용과 같은 배치이므로 의도된 동작이다.
