# YGScaffoldV2 공통 로딩·에러 토스트 Implementation Plan

> ✅ **완료·develop 머지(PR #267 `955c4636`, 2026-08-16)** — 3 Task 전량이 develop에 있다.
> 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> **계획이 코드와 갈린 곳 2건**:
> ① **계측 테스트가 5건이 아니라 7건이다.** 리뷰 라운드에서 접근성 차단 대조 2건
> (`ygScaffoldV2_isLoadingTrue_marksContentHiddenFromAccessibility` ·
> `…_isLoadingFalse_keepsContentVisibleToAccessibility`)이 추가됐다 — 오버레이의 `pointerInput`
> 소비만으로는 TalkBack 더블탭(`SemanticsActions.OnClick` 직접 호출)을 막지 못한다는 사실이
> 구현 중에 드러났기 때문이다. 계획의 테스트 코드에도 결함 2건이 있었다(`assertDoesNotExist`
> import 불필요 · `setContent`를 `YGCustomTheme`으로 감싸야 함) — 둘 다 테스트 파일 안에서 끝났다.
> ② **이 계획의 "범위 밖"이던 화면 이관 3건이 같은 브랜치에 들어왔다**(A-002 로그인 `dbbed12e` ·
> S-003·S-002 `d63ed5b0` · S-002 제출 중 차단 `8e0662b5`). 일괄 이관은 여전히 하지 않았고, 계약이
> 실제로 성립하는지 확인하려고 세 화면만 옮긴 것이다. 그 과정에서 나온 발견은 스펙 as-built ⑤에 있다.
>
> **설계 계약은 무변경** — `YGScaffoldV2`·`YGLoadingOverlay`·`showError` 시그니처, `@Deprecated`
> `ReplaceWith` 문자열, Task 순서 전부 계획대로다. 상세 → [스펙 as-built](../../specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md).

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `YGScaffold`에 공통 로딩 오버레이와 공통 에러 토스트 자리를 더한 `YGScaffoldV2`를 만들고, V1에 `@Deprecated`를 붙여 점진 이관 경로를 연다.

**Architecture:** `core:designsystem` 안에서만 끝난다. `YGScaffoldV2`는 M3 `Scaffold` 위에 `Box`를 얹어 세 층(content → 로딩 오버레이 → 토스트 호스트)을 쌓는 무상태 컴포저블이고, 상태는 전부 호출부가 파라미터로 넘긴다. 토스트는 이미 있는 `YGToastPolicy`·`YGToastHost`를 그대로 재사용하고 `showError` 확장 하나만 더한다. 로딩 오버레이는 디자인 미확정 상태의 임시 구현이다.

**Tech Stack:** Kotlin, Jetpack Compose (Material3 `Scaffold`), Compose UI Test (`ui-test-junit4` 계측), ktlint

**Spec:** [`parfait/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md`](../../specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 위키 저장소가 아니다. 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다. 아래 모든 경로는 `TJYG-Android` 저장소 루트 기준이다.
- **커밋은 하지 않는다.** 저장소 규약상 TJYG-Android는 기본적으로 미커밋이다. 각 Task의 마지막 단계는 커밋이 아니라 검증이다. 사용자가 명시적으로 요청하면 그때 커밋한다.
- **모든 신규 파라미터는 기본값을 갖는다.** V1의 `ReplaceWith`가 생성하는 치환 코드가 V1 인자만으로 컴파일되어야 한다. 이것은 편의가 아니라 계약이다.
- **새 모듈 의존을 추가하지 않는다.** `core:designsystem`은 `:domain`·`core:ui`를 몰라야 한다. 에러 문구가 `String`인 이유가 이것이다.
- **패키지 루트**: `com.teamyg.parfait.core.designsystem`
- **Kotlin 소스 디렉토리**: `core/designsystem/src/main/kotlin/`, 테스트는 `core/designsystem/src/androidTest/kotlin/`
- **KDoc·주석은 한국어**로 쓴다(저장소 전체 관례).
- **테스트 룰은 무인자 `createComposeRule()`**를 쓴다. 이 모듈의 유일한 선례인 `YGThemeSmokeTest`가 `androidx.compose.ui.test.junit4.v2.createComposeRule`를 무인자로 쓰고 있고, `StandardTestDispatcher`를 넘기려면 `kotlinx-coroutines-test`를 androidTest 클래스패스에 새로 올려야 한다. 의존 추가는 이 라운드 범위 밖이다.
- **검증 명령 3종**:
  - 컴파일: `./gradlew :core:designsystem:compileDebugKotlin`
  - 린트: `./gradlew :core:designsystem:ktlintCheck`
  - 계측 테스트(**에뮬레이터 또는 실기기 연결 필요**): `./gradlew :core:designsystem:connectedDebugAndroidTest`
    - 단일 클래스만: `./gradlew :core:designsystem:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<FQCN>`
- **기기가 없으면 계측 테스트를 "통과"로 보고하지 않는다.** 실행하지 못했으면 못했다고 적는다.

---

### Task 1: YGLoadingOverlay

로딩 중 화면 위에 덮는 오버레이. Dim + 인디케이터 + 터치 삼킴. 이 컴포넌트의 유일한 비자명 동작은 **터치 삼킴**이고 테스트도 그것을 겨눈다.

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygloading/YGLoadingOverlay.kt`
- Create: `core/designsystem/src/main/res/values/strings.xml`
- Test: `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygloading/YGLoadingOverlayTest.kt`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces:
  - `fun YGLoadingOverlay(modifier: Modifier = Modifier)` — `com.teamyg.parfait.core.designsystem.component.ygloading`
  - `const val YG_LOADING_OVERLAY_TEST_TAG: String` — 같은 파일, Task 2 테스트가 이 태그로 오버레이 유무를 판정한다

> **주의 — 이 모듈 최초의 `strings.xml`이다.** `core/designsystem/src/main/res/`에는 지금 `drawable*/`과 `font/`만 있고 `values/`가 없다. 즉 디자인시스템이 사용자 노출 문자열을 소유하는 첫 사례가 된다. 그럼에도 문자열을 두는 이유는 접근성이다 — 터치를 통째로 삼키는 오버레이가 TalkBack에 아무것도 아닌 것으로 보이면 스크린리더 사용자는 화면이 멈춘 이유를 알 수 없다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygloading/YGLoadingOverlayTest.kt`

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGLoadingOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygLoadingOverlay_composed_isDisplayed() {
        // Given · When 오버레이만 컴포지션
        composeTestRule.setContent {
            YGLoadingOverlay(modifier = Modifier.fillMaxSize())
        }

        // Then 오버레이가 그려진다
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun ygLoadingOverlay_overContent_swallowsClick() {
        // Given 클릭 가능한 컨텐츠 위에 오버레이를 덮는다
        var clickCount = 0
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(CONTENT_TAG)
                        .clickable { clickCount += 1 },
                )
                YGLoadingOverlay(modifier = Modifier.fillMaxSize())
            }
        }

        // When 가려진 컨텐츠를 클릭
        composeTestRule.onNodeWithTag(CONTENT_TAG).performClick()

        // Then 클릭이 오버레이에서 멎어 콜백이 불리지 않는다
        composeTestRule.runOnIdle { assertEquals(0, clickCount) }
    }

    private companion object {
        const val CONTENT_TAG = "content"
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```
./gradlew :core:designsystem:compileDebugAndroidTestKotlin
```

Expected: FAIL — `Unresolved reference: YGLoadingOverlay`, `Unresolved reference: YG_LOADING_OVERLAY_TEST_TAG`

(계측 테스트라 실행 전 컴파일에서 이미 실패한다. 이 단계에서는 컴파일 실패가 RED다.)

- [ ] **Step 3: 문자열 리소스를 만든다**

`core/designsystem/src/main/res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="yg_loading_overlay_description">로딩 중</string>
</resources>
```

- [ ] **Step 4: 오버레이를 구현한다**

`core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygloading/YGLoadingOverlay.kt`

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

const val YG_LOADING_OVERLAY_TEST_TAG = "yg_loading_overlay"

/**
 * 로딩 중 화면 위에 덮는 오버레이. Dim 과 인디케이터를 그리고 그 아래 컨텐츠의
 * 터치를 삼킨다.
 *
 * ⚠️ 임시 구현이다 — 로딩 UI 디자인이 아직 정해지지 않았다. Dim 농도·인디케이터 모양·
 * 문구 유무 전부 확정 전 자리 채움이고, 디자인이 나오면 이 파일만 고친다.
 * 다른 곳에 로딩 UI 를 복제하지 마라 — 그러면 고칠 곳이 늘어난다.
 *
 * 터치 차단에 `clickable` 이 아니라 [pointerInput] 을 쓰는 이유: `clickable` 은 클릭
 * 시맨틱과 접근성 액션을 붙여 TalkBack 이 이 오버레이를 버튼으로 읽는다. 여기서 필요한
 * 것은 "누를 수 있는 것"이 아니라 "지나갈 수 없는 것"이다.
 */
@Composable
fun YGLoadingOverlay(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.yg_loading_overlay_description)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(YG_LOADING_OVERLAY_TEST_TAG)
            .background(YGAtomicColors.Transparency.Black25)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .semantics { contentDescription = description },
    ) {
        CircularProgressIndicator(color = YGAtomicColors.Cherry.Cherry100)
    }
}
```

> 인디케이터 색·타입은 새로 고른 값이 아니라 `SegmentationLoadingScreen`이 이미 쓰는 값(`CircularProgressIndicator` + `Cherry.Cherry100`)을 그대로 따른 것이다. Dim은 `Transparency.Black25` — `GroupListRoute`가 모달 배경에 쓰는 것과 같은 토큰이다.

- [ ] **Step 5: 프리뷰를 추가한다**

같은 파일 하단에 붙인다. 이 모듈의 프리뷰 관례(`@YGPreview` + `PreviewBox`)를 따른다.

```kotlin
@YGPreview
@Composable
private fun YGLoadingOverlayPreview() = PreviewBox {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "가려질 컨텐츠",
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray900,
        )
        YGLoadingOverlay(modifier = Modifier.fillMaxSize())
    }
}
```

추가 import:

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

```
./gradlew :core:designsystem:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingOverlayTest
```

Expected: PASS 2건

기기가 연결돼 있지 않으면 `./gradlew :core:designsystem:compileDebugAndroidTestKotlin`까지만 확인하고 **"계측 테스트 미실행"으로 보고한다.**

- [ ] **Step 7: 린트를 통과시킨다**

```
./gradlew :core:designsystem:ktlintCheck
```

위반이 있으면 `./gradlew :core:designsystem:ktlintFormat` 후 재확인.

---

### Task 2: showError 확장과 YGScaffoldV2

스캐폴드 본체. 세 층을 쌓고 인셋을 나눠 준다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoast/YGToastPolicy.kt` (`showError` 확장 추가)
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/screen/YGScaffoldV2.kt`
- Test: `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/screen/YGScaffoldV2Test.kt`

**Interfaces:**
- Consumes: Task 1의 `YGLoadingOverlay(modifier)`, `YG_LOADING_OVERLAY_TEST_TAG`
- Produces:
  - `fun YGScaffoldV2(modifier: Modifier = Modifier, containerColor: Color = YGAtomicColors.Gray.White, contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets, isLoading: Boolean = false, toastPolicy: YGToastPolicy = rememberYGToastPolicy(), content: @Composable (PaddingValues) -> Unit)` — `com.teamyg.parfait.core.designsystem.screen`
  - `fun YGToastPolicy.showError(text: String)` — `com.teamyg.parfait.core.designsystem.component.ygtoast`. Task 3의 `ReplaceWith` 문자열은 이 시그니처에 의존하지 않는다

> **함정 — 테스트 클럭이 토스트를 지운다.** `YGToastHost`는 토스트마다 `LaunchedEffect { delay(2000) }`로 자동 소멸시킨다. Compose 테스트 룰은 기본이 `mainClock.autoAdvance = true`라 `assertIsDisplayed()`가 부르는 `waitForIdle()`이 가상 시간을 진행시키고, 그 과정에서 2초가 지나 **토스트가 사라진 뒤에 단언이 실행될 수 있다.** 아래 테스트는 `autoAdvance = false`로 끄고 진입 애니메이션(`ANIMATION_DURATION` 300ms)만큼만 손으로 진행시킨다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/screen/YGScaffoldV2Test.kt`

```kotlin
package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.teamyg.parfait.core.designsystem.component.ygloading.YG_LOADING_OVERLAY_TEST_TAG
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class YGScaffoldV2Test {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygScaffoldV2_isLoadingTrue_showsOverlay() {
        // Given · When 로딩을 켠 채 컴포지션
        composeTestRule.setContent {
            YGScaffoldV2(isLoading = true) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        }

        // Then 오버레이가 보인다
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun ygScaffoldV2_isLoadingFalse_hidesOverlay() {
        // Given · When 로딩을 끈 채 컴포지션
        composeTestRule.setContent {
            YGScaffoldV2(isLoading = false) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        }

        // Then 오버레이가 없다
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun ygScaffoldV2_showErrorWhileLoading_displaysFailToast() {
        // Given 토스트 정책을 테스트가 쥐고, 로딩을 켠 채 컴포지션한다
        val toastPolicy = YGToastPolicy()
        // 토스트는 2초 뒤 스스로 사라진다 — 가상 시간이 저절로 흐르면 단언 전에 없어진다
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            YGScaffoldV2(isLoading = true, toastPolicy = toastPolicy) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
        }

        // When 실패 토스트를 띄우고 진입 애니메이션만큼만 시간을 진행시킨다
        composeTestRule.runOnUiThread { toastPolicy.showError(ERROR_TEXT) }
        composeTestRule.mainClock.advanceTimeBy(TOAST_ENTER_ANIMATION_MILLIS)

        // Then 로딩 오버레이 위로 문구가 보인다
        composeTestRule.onNodeWithText(ERROR_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(YG_LOADING_OVERLAY_TEST_TAG).assertIsDisplayed()
    }

    private companion object {
        const val ERROR_TEXT = "실패했어요"

        /** `YGToastPolicy` 의 진입 애니메이션(300ms)보다 크고 자동 소멸(2000ms)보다 작아야 한다 */
        const val TOAST_ENTER_ANIMATION_MILLIS = 500L
    }
}
```

추가 import: `androidx.compose.ui.test.assertDoesNotExist`

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```
./gradlew :core:designsystem:compileDebugAndroidTestKotlin
```

Expected: FAIL — `Unresolved reference: YGScaffoldV2`, `Unresolved reference: showError`

- [ ] **Step 3: showError 확장을 추가한다**

`YGToastPolicy.kt` 파일 안, `rememberYGToastPolicy` 선언 바로 아래에 붙인다.

```kotlin
/**
 * 실패를 알리는 토스트를 띄운다. 공통 실패 표현은 [YGToastType.Fail] 하나로 고정이고,
 * 문구는 호출부가 만든다 — 실패의 어휘가 화면마다 다르기 때문이다(로그인은 카카오 로그인
 * 실패, 갤러리는 저장 실패).
 *
 * 재시도 동선이 필요한 실패는 이걸 쓰지 않는다. 그런 실패는 화면이 자기 UI 로 표현한다.
 */
fun YGToastPolicy.showError(text: String) {
    show(YGToastType.Fail(text))
}
```

- [ ] **Step 4: YGScaffoldV2를 구현한다**

`core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/screen/YGScaffoldV2.kt`

```kotlin
package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingOverlay
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * [YGScaffold] 에 공통 로딩 오버레이와 공통 에러 토스트 자리를 더한 신판.
 *
 * 무상태다 — 로딩 여부도 토스트 큐도 호출부가 넘긴다. 스캐폴드는 **어디에 무엇을 겹칠지**만
 * 안다. 화면이 실패를 어떤 문구로 말할지는 화면의 어휘라 여기서 정하지 않는다.
 *
 * 세 층을 이 순서로 겹친다.
 * 1. [content] — [Scaffold] 가 계산한 인셋 패딩을 그대로 받는다
 * 2. 로딩 오버레이 — 인셋을 받지 않는다. Dim 이 시스템바 밑에서 끊기면 어설프다
 * 3. 토스트 호스트 — 상태바 인셋만 받는다. Toast 정책이 위에서 아래로 내려오는 노출이다
 *
 * 토스트가 로딩보다 위인 이유: 로딩 중에 일어난 실패도 보여야 한다.
 *
 * @param isLoading `true` 면 [content] 위에 [YGLoadingOverlay] 를 덮고 그 아래 터치를 삼킨다
 * @param toastPolicy 토스트 큐. 화면이 실패를 알리려면 이 정책을 직접 만들어 넘기고
 *   `showError` 로 띄운다. 넘기지 않으면 스캐폴드가 자기 것을 만들어 쓴다
 */
@Composable
fun YGScaffoldV2(
    modifier: Modifier = Modifier,
    containerColor: Color = YGAtomicColors.Gray.White,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    isLoading: Boolean = false,
    toastPolicy: YGToastPolicy = rememberYGToastPolicy(),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(innerPadding)

            if (isLoading) {
                YGLoadingOverlay(modifier = Modifier.fillMaxSize())
            }

            YGToastHost(
                policy = toastPolicy,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars),
            )
        }
    }
}
```

- [ ] **Step 5: 프리뷰를 추가한다**

같은 파일 하단.

```kotlin
@YGPreview
@Composable
private fun YGScaffoldV2LoadingPreview() = PreviewBox {
    YGScaffoldV2(isLoading = true) { innerPadding ->
        Text(
            text = "컨텐츠",
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray900,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
```

추가 import:

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

```
./gradlew :core:designsystem:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2Test
```

Expected: PASS 3건

세 번째 테스트가 "토스트를 못 찾음"으로 실패하면 시간 진행이 원인이다. `autoAdvance = false`가 `setContent` **앞에** 있는지, `advanceTimeBy` 값이 300보다 크고 2000보다 작은지 확인한다.

- [ ] **Step 7: 린트를 통과시킨다**

```
./gradlew :core:designsystem:ktlintCheck
```

---

### Task 3: V1에 Deprecated 부착

기존 `YGScaffold`가 스스로 후속본을 가리키게 만든다. 삭제는 하지 않는다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/screen/YGScaffold.kt`

**Interfaces:**
- Consumes: Task 2의 `YGScaffoldV2` 시그니처. `ReplaceWith` 문자열이 **V1 인자만으로 V2를 부를 수 있다는 사실에 의존**한다 — V2의 `isLoading`·`toastPolicy`에 기본값이 없으면 이 치환은 컴파일되지 않는다
- Produces: 없음(마지막 Task)

- [ ] **Step 1: Deprecated를 붙인다**

`YGScaffold.kt`의 `@Composable fun YGScaffold(` 선언 바로 위에 붙인다. 함수 본문은 건드리지 않는다.

```kotlin
@Deprecated(
    message = "공통 로딩·에러 토스트 처리가 없는 구판이다. YGScaffoldV2 로 이관한다.",
    replaceWith = ReplaceWith(
        "YGScaffoldV2(modifier = modifier, containerColor = containerColor, " +
            "contentWindowInsets = contentWindowInsets, content = content)",
    ),
    level = DeprecationLevel.WARNING,
)
@Composable
fun YGScaffold(
```

`DeprecationLevel.ERROR`가 아니라 `WARNING`인 이유는 호출처 11곳이 아직 살아 있어서다. `ERROR`면 그 자리가 전부 컴파일 에러가 된다. 승급(`ERROR`)은 호출처가 0이 된 뒤, 삭제는 그다음 라운드다.

- [ ] **Step 2: 전체 모듈이 여전히 컴파일되는지 확인한다**

```
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. `YGScaffold` 호출처 11곳에서 deprecation **경고**가 나오지만 빌드는 통과한다(`allWarningsAsErrors` 설정이 저장소에 없다).

경고가 하나도 안 보이면 `@Deprecated`가 실제로 붙지 않았거나 캐시된 빌드다 — `./gradlew :app:assembleDebug --rerun-tasks`로 재확인한다.

- [ ] **Step 3: `ReplaceWith` 치환이 실제로 컴파일되는지 검증한다**

기계 검사가 없는 항목이라 손으로 한 번 확인한다. 아무 호출처(예: `feature/login/impl/.../navigation/EntryBuilder.kt`)에서 IDE의 "Replace with YGScaffoldV2" 퀵픽스를 적용해 보고, 컴파일되는 것을 확인한 뒤 **되돌린다**(이 라운드는 이관하지 않는다).

IDE 없이 확인하려면 임시로 호출부 한 곳을 손으로 V2로 바꿔 `./gradlew :app:assembleDebug`를 돌리고 되돌린다.

Expected: 치환 코드가 인자 추가 없이 컴파일된다.

- [ ] **Step 4: 린트·전체 테스트를 통과시킨다**

```
./gradlew :core:designsystem:ktlintCheck
./gradlew :core:designsystem:connectedDebugAndroidTest
```

Expected: ktlint 위반 0, 계측 테스트 5건 PASS(Task 1의 2건 + Task 2의 3건 + 기존 `YGThemeSmokeTest` 2건은 별도 클래스)

- [ ] **Step 5: 변경 요약을 보고한다**

커밋하지 않는다. 대신 `git -C <TJYG-Android 경로> status --short`와 `git diff --stat`을 보여주고, 계측 테스트를 실제로 실행했는지(기기 연결 여부)를 명시해 보고한다.

---

## 범위 밖 — 하지 않는 것

이 계획을 실행하는 사람이 "빠뜨린 것"으로 오해하지 않도록 명시한다.

- **기존 호출처 11곳 이관.** 스펙이 점진 이관으로 정했다. 이관은 스캐폴드를 `EntryBuilder`에서 Route 안으로 내리는 작업이라(호출처 9곳이 `EntryBuilder`에 있고 `hiltViewModel()`은 Route 안에 있다) 화면별 API 결선 라운드에 묶인다.
- **`app-preview` 컴포넌트 갤러리 등록.** 이 저장소는 새 디자인시스템 컴포넌트를 `NavKey` + `PreviewScreen` + `ComponentCatalog` + `EntryBuilders` 4파일로 갤러리에 등록하는 관례가 있지만, `YGLoadingOverlay`는 디자인 미확정 임시 구현이라 등록하지 않는다. 디자인이 확정되는 라운드에 함께 한다.
- **`AppError` → 문구 공통 매핑.** `core:ui` 소관이고 이 라운드는 `core:designsystem` 안에서 끝난다.
- **`SegmentationLoadingScreen`·`GroupListErrorScreen` 정리.** 전자는 화면 고유 로딩 표현, 후자는 차단성 에러 UI로 둘 다 V2가 다루는 갈래가 아니다.
- **camera·gallery의 수동 토스트 배선 제거.** 그 화면들이 V2로 이관될 때 함께 걷어낸다.
