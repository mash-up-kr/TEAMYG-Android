---
id: c103-error-use-original
title: C-103-Error 실패 화면 통합과 「편집 없이 사용」 구현 계획
status: done
type: work-order
created: 2026-09-05
updated: 2026-09-06
archived_reason: 구현 완료·develop 머지(2026-09-06, PR #457 5f860cb9c). 브랜치 feature/#348-segmentation-error-button, 4 Task 전량 수행, 신규 테스트 3건.
platforms: android
owner: Parfait 팀
related_adr: ADR-0012
related_spec: c103-error-use-original, segmentation-module-install
related_code:
  - SegmentationViewModel.kt#useOriginal
  - SegmentationErrorScreen.kt#SegmentationErrorScreen
  - SaveBitmapUseCase.kt
tags: [plan, parfait, segmentation, c103]
---

# C-103-Error 실패 화면 통합과 「편집 없이 사용」 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 누끼 실패 화면을 디자인 `C-103-Error` 확정본에 맞춰 한 벌 문구로 합치고, 원본 사진을 그대로 토핑 재료로 쓰는 「편집 없이 사용」 버튼을 더한다.

**Architecture:** 실패 원인 분기(`SegmentationErrorKind`)를 걷어 상태를 `isError: Boolean` 하나로 되돌린다. 「편집 없이 사용」은 새 경로를 만들지 않고 기존 후보 선택 경로(저장 → 초안 기록 → `GoToConfirm`)를 그대로 재사용하되, 원본은 잘린 판과 캔버스 판이 같은 그림이라 `saveEditedImage`로 **한 번만 저장해 같은 경로를 두 자리에 싣는다**. 원본을 못 읽은 경우는 실패 화면에 도달하지 않고 뒤로 보내, 실패 화면이 뜰 때는 원본이 반드시 살아 있게 만든다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3, Hilt, 자체 MVI(`BaseViewModel`), MockK, Turbine, `kotlinx-coroutines-test`, ktlint

**Spec:** [`parfait/specs/archive/2026-09-05-c103-error-use-original.md`](../../specs/archive/2026-09-05-c103-error-use-original.md)

> 📌 **실행 뒤 이름이 바뀌었다(2026-09-06, 코드 리뷰 반영).** 아래 코드 블록은 실제로 실행한 그대로
> 두되, `SaveEditedImageUseCase` → `SaveBitmapUseCase`, `saveEditedImage` → `saveBitmap` 으로 읽어라.
> Task 3 Step 7이 지시한 KDoc 확장도 이름이 역할을 말하게 되면서 필요 없어져 걷었다.
> 근거는 스펙의 「편집 없이 사용」절 📌 표기.

**작업 대상 저장소:** `TJYG-Android` (이 문서가 있는 저장소가 아니다). 브랜치 `feature/#348-segmentation-error-button`이 이미 있고 `develop`과 같은 커밋이다.

> ✅ **완료·develop 머지(2026-09-06, PR #457 `5f860cb9c`)** — 4 Task를 전부 수행했고 신규 테스트 3건이 붙었다. 머지본과 스펙을 대조한 결과 어긋난 자리가 없다.

## Global Constraints

- **커밋하지 않는다.** TJYG-Android는 기본이 미커밋이다. 각 Task의 마지막 단계는 커밋이 아니라 테스트 통과 확인이다. 사용자가 요청하면 그때 커밋한다.
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다. 낡는다.
  - 주석 분량은 그 코드의 **어려움**에 비례해야 한다. 중요하지만 단순한 코드에 긴 주석을 달지 않는다.
- **문구는 아래 값을 그대로 쓴다.** 임의로 다듬지 않는다.
  - `segmentation_error_title` = `사진 편집에 실패했어요`
  - `segmentation_error_description` = `다시 시도하거나 편집 없이 사용할 수 있어요`
  - `segmentation_error_retry` = `다시 시도`
  - `segmentation_error_use_original` = `편집 없이 사용`
- **버튼 타입은 디자인 시스템의 기존 것을 쓴다.** 「다시 시도」는 `YGButtonType.Medium.Primary`, 「편집 없이 사용」은 `YGButtonType.Medium.Secondary`. 새 컴포넌트를 만들지 않는다.
- **data 계층과 도메인 모델은 바뀌지 않는다.** `persistSubject`·`SegmentationCandidate`·모듈 설치기를 건드리지 않는다. 이 계획이 domain에서 손대는 것은 KDoc 두 곳뿐이다.
- **ktlint를 통과해야 한다.** 각 Task 끝에서 `./gradlew :feature:segmentation:impl:ktlintCheck`를 돌린다. 쓰지 않게 된 import를 남기면 여기서 걸린다.
- 테스트는 **Given / When / Then 주석**과 기존 파일의 명명 규칙(`대상_상황_기대`)을 따른다.

---

### Task 1: 실패 상태를 하나로 합친다

실패 원인이 화면에 닿지 않게 만든다. 이 Task가 끝나면 모듈 실패와 대상 못 찾음이 같은 문구를 쓴다.

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/SegmentationErrorScreen.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/SegmentationScreen.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `SegmentationState.isError: Boolean` — 이후 모든 Task가 이 이름을 쓴다. `SegmentationErrorKind`는 이 Task 이후 존재하지 않는다.

- [ ] **Step 1: 테스트의 단언을 `isError`로 바꾼다(먼저 깨뜨린다)**

`SegmentationViewModelTest.kt`에서 `errorKind`를 읽는 6곳을 아래처럼 바꾼다.

⚠️ 첫 번째 자리(`init_decodeFails_tellsTheUserWithoutSegmenting`)는 **Task 2가 이 테스트를 통째로 대체한다.** 여기서는 컴파일만 되게 최소로 치환하고 의미는 따지지 않는다.

```kotlin
// init_decodeFails_tellsTheUserWithoutSegmenting 안 — Task 2 가 대체할 자리다
assertTrue(viewModel.state.value.isError)

// init_segmentationFails_tellsTheUser 안
assertTrue(viewModel.state.value.isError)

// init_noSubjectDetected_tellsTheUser 안
assertTrue(viewModel.state.value.isError)

// retry_afterFailure_runsTheFlowAgainAndClearsTheError 안, 첫 시도 직후
assertTrue(viewModel.state.value.isError)

// 같은 테스트의 재시도 성공 직후
assertFalse(state.isError)
```

모듈 실패 테스트는 이름과 주석까지 바꾼다. 이 테스트가 **통합 결정을 잠그는 자리**라 지우지 않는다.

```kotlin
    @Test
    fun init_moduleNotReady_marksErrorLikeAnyOtherFailure() = runTest {
        // Given 모듈을 못 받아 실패한 상황
        coEvery { segmentImage(bitmapWrapper) } returns
            Result.failure(SegmentationException.ModuleNotReady(null))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 대상 못 찾음과 같은 실패로 접는다 — 디자인이 문구를 한 벌로 요구한다
        assertTrue(viewModel.state.value.isError)
    }
```

`assertNull`은 이 파일에서 쓰는 곳이 없어지므로 import를 지운다. `SegmentationErrorKind`는 테스트와 같은 패키지라 import가 없다.

```kotlin
// 삭제
import kotlin.test.assertNull
```

- [ ] **Step 2: 컴파일 실패를 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: 컴파일 FAIL. `Unresolved reference: isError`

- [ ] **Step 3: 상태에서 원인 분기를 걷는다**

`SegmentationViewModel.kt`에서 `SegmentationErrorKind` enum 선언 전체를 지운다. 상태를 바꾼다.

```kotlin
data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    /** 참이면 화면 전체가 `C-103-Error` 로 바뀐다 */
    val isError: Boolean = false,
) : UiState
```

`loadCandidates()`의 네 자리를 고친다.

```kotlin
            // 실패 표시를 걷지 않으면 재시도가 성공해도 에러 화면이 그대로 남는다
            updateState { copy(isLoading = true, isError = false, candidates = emptyList()) }
```

```kotlin
            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false, isError = true) }
                return@launch
            }
```

```kotlin
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        updateState { copy(isError = true) }
                        return@onSuccess
                    }

                    updateState { copy(candidates = candidates) }
                }.onFailure { throwable ->
                    // 원인을 삼키면 실기기 로그 말고는 모듈 미설치를 알아낼 수단이 없다
                    viewModelLogger.e(throwable) {
                        "세그멘테이션 실패 ${throwable::class.simpleName}, 원인 ${throwable.cause}"
                    }
                    updateState { copy(isError = true) }
                }
```

`SegmentationEffect.ShowError`의 KDoc이 지워진 프로퍼티를 가리키게 되므로 같이 고친다.

```kotlin
    /**
     * 고른 뒤의 실패에만 쓴다. 후보 목록이 그대로 남아 다른 대상을 고를 수 있으므로 화면을 덮지 않고
     * 토스트로 한 번 알린다. 대상을 아예 못 얻은 실패는 [SegmentationState.isError] 가 받는다.
     */
    data object ShowError : SegmentationEffect
```

`toErrorKind()` 확장 함수를 통째로 지운다. 함께 쓰이지 않게 된 import도 지운다.

```kotlin
// 삭제
import com.teamyg.parfait.domain.exception.SegmentationException
```

- [ ] **Step 4: Route에서 문구 분기를 걷는다**

`SegmentationRoute.kt` 하단의 `titleRes()`·`descriptionRes()` 두 함수를 통째로 지우고, 분기를 상태 하나로 바꾼다.

```kotlin
        // 대상을 아예 못 얻은 실패는 화면 전체를 C-103-Error 로 바꾼다.
        // 고른 뒤의 실패는 후보가 남아 있어 토스트로만 알린다(SegmentationEffect.ShowError)
        if (state.isError) {
            SegmentationErrorScreen(
                onClickRetry = { viewModel.processIntent(SegmentationIntent.Retry) },
                onClickClose = onClickClose,
                modifier = modifier.padding(innerPadding),
            )
        } else {
```

쓰지 않게 된 import 둘을 지운다.

```kotlin
// 삭제
import androidx.annotation.StringRes
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationErrorKind
```

- [ ] **Step 5: 화면이 문구를 스스로 쓰게 한다**

`SegmentationErrorScreen.kt`의 시그니처에서 `title`·`description`을 걷는다. 문구가 한 벌이라 밖에서 실어 보낼 것이 없다.

```kotlin
@Composable
internal fun SegmentationErrorScreen(
    onClickRetry: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

본문의 두 `Text`가 문자열을 직접 읽게 한다.

```kotlin
                    Text(
                        text = stringResource(R.string.segmentation_error_title),
                        style = YGTheme.typography.title.t03SB,
                        color = YGAtomicColors.Gray.Gray900,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(R.string.segmentation_error_description),
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray500,
                        textAlign = TextAlign.Center,
                    )
```

Preview도 파라미터를 줄인다.

```kotlin
@YGPreview
@Composable
private fun PreviewSegmentationErrorScreen() = PreviewBox {
    SegmentationErrorScreen(
        onClickRetry = {},
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

파일 상단 KDoc에서 재시도 버튼의 시안 경고를 걷는다. 검토가 끝나 디자인에 버튼이 들어왔으므로 그 경고는 이제 거짓이다.

```kotlin
/**
 * 대상을 잘라내지 못했을 때의 화면(Figma `C-103-Error`).
 */
```

- [ ] **Step 6: 죽은 KDoc 참조를 고친다**

`SegmentationScreen.kt` 상단 KDoc이 지워진 프로퍼티를 가리킨다. **컴파일도 ktlint도 이것을 잡지 못하고**, `SegmentationErrorKind`를 찾는 grep에도 안 걸린다.

```kotlin
/**
 * 대상을 하나 이상 얻은 뒤의 화면만 그린다 — 못 얻은 실패는 [SegmentationErrorScreen] 이
 * 받고, 둘 중 무엇을 띄울지는 상위 Route 가 [SegmentationState.isError] 로 고른다.
 */
```

- [ ] **Step 7: 문구를 통합한다**

`strings.xml`에서 설명을 바꾸고 모듈 실패 문구 2개를 지운다.

```xml
    <string name="segmentation_error_title">사진 편집에 실패했어요</string>
    <string name="segmentation_error_description">다시 시도하거나 편집 없이 사용할 수 있어요</string>
    <string name="segmentation_error_retry">다시 시도</string>
```

```xml
<!-- 아래 두 줄을 삭제한다 -->
    <string name="segmentation_module_error_title">사진 편집 기능을 준비하지 못했어요</string>
    <string name="segmentation_module_error_description">네트워크 상태를 확인하고 잠시 후 다시 시도해 주세요</string>
```

모듈 문구 두 줄을 지우면 `segmentation_error_retry`가 앞 블록과 빈 줄로 갈린 채 남는다. 위 목표 블록처럼 제목·설명·재시도를 한 블록으로 붙이고 남는 빈 줄을 정리한다.

`segmentation_error_message`는 그대로 둔다. 화면을 덮지 않는 실패(고른 뒤 저장 실패)에 쓰는 별개 문구다. ⚠️ 그 문구가 "사진 편집에 실패했어요. 잠시 후 다시 시도해 주세요."라 실패 화면 제목과 첫 문장이 겹치는데, **의도한 것이라 건드리지 않는다**(스펙 확인 완료).

- [ ] **Step 8: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: PASS

- [ ] **Step 9: ktlint를 돌린다**

Run: `./gradlew :feature:segmentation:impl:ktlintCheck`
Expected: PASS. 실패하면 남은 미사용 import를 지운다.

---

### Task 2: 원본을 못 읽으면 뒤로 보낸다

디코드 실패를 실패 화면에서 떼어 낸다. 이 Task가 끝나면 **실패 화면은 원본 비트맵이 반드시 살아 있을 때만 뜬다.** Task 3의 「편집 없이 사용」이 비활성 분기 없이 성립하는 근거가 여기서 만들어진다.

⚠️ **뒤로 가면 사진 확인 화면이 아니라 카메라 화면 또는 갤러리 피커가 나온다.** `PictureConfirmRoute`가 `goToAndPopCurrent`로 이동해 사진 확인 화면을 백스택에서 치환하기 때문이다. 다시 찍거나 다른 사진을 고를 수 있는 자리라 설계 목적은 그대로다. **이 사실을 코드 주석으로 옮겨 적지 않는다** — 다른 화면의 현재 상태라 낡는다.

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `SegmentationState.isError` (Task 1)
- Produces: `SegmentationEffect.GoBack` — 인자 없는 `data object`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

기존 `init_decodeFails_tellsTheUserWithoutSegmenting`를 아래 테스트로 **대체한다.** 이름·주석·단언이 모두 바뀐다.

```kotlin
    @Test
    fun init_decodeFails_goesBackWithoutSegmenting() = runTest {
        // Given URI 가 만료돼 디코드가 실패를 돌려주는 상황
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IllegalStateException("broken uri"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 실패 화면 대신 뒤로 보낸다 — 원본이 없으면 이 화면에서 할 수 있는 일이 없다
        assertFalse(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 0) { segmentImage(any()) }
        viewModel.effect.test { assertEquals(SegmentationEffect.GoBack, awaitItem()) }
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: 컴파일 FAIL. `Unresolved reference: GoBack`

- [ ] **Step 3: 이펙트를 더하고 분기를 바꾼다**

`SegmentationViewModel.kt`의 `SegmentationEffect`에 항목을 더한다.

```kotlin
sealed interface SegmentationEffect : UiSideEffect {
    /**
     * 고른 뒤의 실패에만 쓴다. 후보 목록이 그대로 남아 다른 대상을 고를 수 있으므로 화면을 덮지 않고
     * 토스트로 한 번 알린다. 대상을 아예 못 얻은 실패는 [SegmentationState.isError] 가 받는다.
     */
    data object ShowError : SegmentationEffect

    /**
     * 원본을 못 읽어 이 화면에서 할 수 있는 일이 없다. 실패 화면이 원본이 살아 있을 때만 뜨게
     * 만드는 장치이기도 하다 — 그래야 「편집 없이 사용」에 비활성 분기가 필요 없다.
     */
    data object GoBack : SegmentationEffect

    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
}
```

`loadCandidates()`의 디코드 실패 분기를 바꾼다.

```kotlin
            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false) }
                postSideEffect(SegmentationEffect.GoBack)
                return@launch
            }
```

- [ ] **Step 4: Route가 뒤로 가기를 받게 한다**

`SegmentationRoute.kt`의 이펙트 수집 `when`에 분기를 더한다. 토스트는 띄우지 않는다. 주석을 달지 않는다 — `GoBack`의 KDoc이 이미 이유를 말한다.

```kotlin
            when (effect) {
                is SegmentationEffect.ShowError -> toastPolicy.showError(errorMessage)

                is SegmentationEffect.GoBack -> navigator.onBack()

                // 백스택에 쌓아 올려서 뒤로가기 하면 객체 인식이 끝난 이 화면으로 그대로 돌아온다
                is SegmentationEffect.GoToConfirm -> navigator.goTo(
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: PASS

- [ ] **Step 6: ktlint를 돌린다**

Run: `./gradlew :feature:segmentation:impl:ktlintCheck`
Expected: PASS

---

### Task 3: 「편집 없이 사용」 동작을 만든다

원본 사진을 토핑 재료로 삼아 확인 화면까지 보낸다. 화면에 버튼을 다는 것은 Task 4다.

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/SaveEditedImageUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `SegmentationState.isError` (Task 1). Task 2가 만든 **불변식**에 기댄다 — 실패 화면이 뜨는 순간 원본은 반드시 살아 있다. 심볼을 쓰지는 않는다.
- Produces: `SegmentationIntent.UseOriginal` — 인자 없는 `data object`. Task 4의 `onClickUseOriginal`이 이것을 보낸다.

- [ ] **Step 1: 테스트 하니스에 유스케이스를 더한다**

`SegmentationViewModelTest.kt`에 목과 상수를 더하고 생성자 호출을 고친다.

```kotlin
private const val ORIGIN_PATH = "/cache/segmentation/origin.png"
```

```kotlin
    private val saveEditedImage: SaveEditedImageUseCase = mockk()
```

```kotlin
    @Before
    fun stubTheHappyPath() {
        coEvery { decodeImage(SOURCE_URI) } returns Result.success(bitmapWrapper)
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate))
        coEvery { persistSubject(candidate) } returns Result.success(success)
        coEvery { saveEditedImage(bitmapWrapper) } returns Result.success(ORIGIN_PATH)
    }

    private fun viewModel() = SegmentationViewModel(
        sourceImageUri = SOURCE_URI,
        addRecentImageUseCase = addRecentImage,
        clearSegmentationCacheUseCase = clearSegmentationCache,
        decodeImageUseCase = decodeImage,
        segmentImageUseCase = segmentImage,
        persistSubjectUseCase = persistSubject,
        saveEditedImageUseCase = saveEditedImage,
        toppingDraftRepository = toppingDraftRepository,
    )
```

import를 더한다. ktlint가 알파벳순을 요구하므로 `PersistSubjectUseCase`와 `SegmentImageUseCase` **사이**에 넣는다.

```kotlin
import com.teamyg.parfait.domain.usecase.image.SaveEditedImageUseCase
```

- [ ] **Step 2: 실패하는 테스트 3건을 쓴다**

파일 끝의 모듈 실패 테스트 뒤에 이어 붙인다.

```kotlin
    @Test
    fun useOriginal_savesOnceAndGoesToConfirm() = runTest {
        // Given 세그멘테이션이 실패해 실패 화면이 떠 있다
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 편집 없이 사용을 누른다
        viewModel.processIntent(SegmentationIntent.UseOriginal)
        advanceUntilIdle()

        // Then 원본은 잘린 판과 캔버스 판이 같은 그림이라 한 번만 저장하고 같은 경로를 두 자리에 싣는다
        coVerify(exactly = 1) { saveEditedImage(bitmapWrapper) }
        coVerify(exactly = 0) { persistSubject(any()) }
        coVerify(exactly = 1) {
            toppingDraftRepository.record(
                subjectImagePath = ORIGIN_PATH,
                cutoutImagePath = ORIGIN_PATH,
                borderColorArgb = null,
                borderWidthDp = null,
            )
        }
        viewModel.effect.test {
            assertEquals(
                SegmentationEffect.GoToConfirm(
                    subjectImagePath = ORIGIN_PATH,
                    trimmedSubjectImagePath = ORIGIN_PATH,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun useOriginal_saveFails_showsToastAndStaysOnErrorScreen() = runTest {
        // Given 실패 화면이 떠 있고 원본 저장이 실패하는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { saveEditedImage(bitmapWrapper) } returns Result.failure(IllegalStateException("disk full"))
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 편집 없이 사용을 누른다
        viewModel.processIntent(SegmentationIntent.UseOriginal)
        advanceUntilIdle()

        // Then 토스트로 알리고 실패 화면에 머문다 — 로딩에 갇히지도 않는다
        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }

    @Test
    fun useOriginal_pressedTwiceWhileRunning_runsOnce() = runTest {
        // Given 실패 화면이 떠 있고 원본 저장이 오래 걸리는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { saveEditedImage(bitmapWrapper) } coAnswers {
            delay(1_000)
            Result.success(ORIGIN_PATH)
        }
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 연달아 두 번 누른다
        viewModel.processIntent(SegmentationIntent.UseOriginal)
        runCurrent()
        viewModel.processIntent(SegmentationIntent.UseOriginal)
        advanceUntilIdle()

        // Then 두 번째 누름은 버려진다 — 같은 원본을 두 벌 떨구지 않는다
        coVerify(exactly = 1) { saveEditedImage(bitmapWrapper) }
    }
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: 컴파일 FAIL. `Unresolved reference: UseOriginal`, `No value passed for parameter 'saveEditedImageUseCase'`

- [ ] **Step 4: 인텐트와 의존성을 더한다**

`SegmentationViewModel.kt`의 인텐트에 항목을 더한다.

```kotlin
sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent

    data object Retry : SegmentationIntent

    data object UseOriginal : SegmentationIntent
}
```

생성자에 유스케이스를 더한다.

```kotlin
    private val persistSubjectUseCase: PersistSubjectUseCase,
    private val saveEditedImageUseCase: SaveEditedImageUseCase,
    private val toppingDraftRepository: ToppingDraftRepository,
```

import를 더한다. ktlint 정렬 때문에 `BitmapWrapper`는 `runSuspendCatching` 바로 뒤에, `SaveEditedImageUseCase`는 `PersistSubjectUseCase`와 `SegmentImageUseCase` 사이에 넣는다.

```kotlin
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.usecase.image.SaveEditedImageUseCase
```

- [ ] **Step 5: 원본 래퍼를 들고 있게 한다**

`AndroidBitmap`의 생성자가 `core:util:android` 모듈 내부로 막혀 있어 `state.originBitmap`을 여기서 다시 감쌀 수 없다. 디코드가 준 래퍼를 그대로 붙든다.

```kotlin
    /** `AndroidBitmap` 생성자가 모듈 내부라 `state.originBitmap` 으로는 다시 만들 수 없다 */
    private var originBitmapWrapper: BitmapWrapper? = null
```

`loadCandidates()`의 디코드 성공 직후에 채운다. 재시도가 이 함수를 다시 타므로 값도 함께 갱신된다.

진입부 리셋에서 이 필드를 비우지 않아도 낡은 값이 쓰일 경로가 없다. 재시도의 디코드가 실패하면 Task 2의 `GoBack`으로 화면을 떠나기 때문이다.

```kotlin
            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            originBitmapWrapper = bitmapWrapper
            updateState { copy(originBitmap = originBitmap) }
```

- [ ] **Step 6: 동작을 구현한다**

`processIntent`에 분기를 더한다.

```kotlin
    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)
            SegmentationIntent.Retry -> loadCandidates()
            SegmentationIntent.UseOriginal -> useOriginal()
        }
    }
```

`selectCandidate` 아래에 함수를 더한다.

```kotlin
    /**
     * 누끼 없이 원본을 그대로 토핑 재료로 쓴다. 저장 → 초안 기록 → 이동 순서는 후보 선택과 같다.
     *
     * ⚠️ **원본은 `persistSubject` 로 보내지 않는다.** 그 경로는 원본 크기 빈 비트맵을 하나 더
     * 만들어 그 위에 원본을 그리므로 순간 메모리가 두 배로 뛰는데, 잘린 판과 캔버스 판이 같은
     * 그림이라 얻는 것이 없다. 한 번 저장한 경로를 두 자리에 싣는다.
     */
    private fun useOriginal() {
        val originBitmapWrapper = originBitmapWrapper ?: return

        launch(
            key = USE_ORIGINAL_KEY,
            onError = {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
            },
        ) {
            updateState { copy(isLoading = true) }

            val path = saveEditedImageUseCase(originBitmapWrapper).getOrElse {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
                return@launch
            }

            val recorded = runSuspendCatching {
                toppingDraftRepository.record(
                    subjectImagePath = path,
                    cutoutImagePath = path,
                    borderColorArgb = null,
                    borderWidthDp = null,
                )
            }.getOrDefault(false)

            // 이동이 goTo 라 이 화면이 백스택에 남는다. 켠 채 나가면 돌아왔을 때 갇힌다
            releaseLoading()

            if (recorded) {
                postSideEffect(
                    SegmentationEffect.GoToConfirm(
                        subjectImagePath = path,
                        trimmedSubjectImagePath = path,
                    ),
                )
            } else {
                postSideEffect(SegmentationEffect.ShowError)
            }
        }
    }
```

파일 끝의 키 상수에 한 줄을 더한다.

```kotlin
private const val SELECT_CANDIDATE_KEY = "select-candidate"
private const val LOAD_CANDIDATES_KEY = "loadCandidates"
private const val USE_ORIGINAL_KEY = "use-original"
```

- [ ] **Step 7: 도메인 KDoc을 넓힌다**

`SaveEditedImageUseCase`는 원본도 지나가게 되므로 "손으로 다듬은 결과"만 가리키는 서술을 넓힌다. **이름은 바꾸지 않는다** — 편집 저장 호출부까지 건드리게 되어 이 작업의 범위를 넘는다.

`ImageSegmentationRepository.kt`:

```kotlin
    /**
     * 비트맵 한 장을 캐시에 PNG 로 저장한다. 손으로 다듬은 결과와, 누끼 없이 그대로 쓰는 원본이
     * 이 함수를 함께 쓴다.
     *
     * 화면 사이에서는 비트맵 대신 경로를 주고받아야 해서 한 번 파일로 떨군다.
     *
     * @return 저장된 파일의 절대 경로
     */
    suspend fun saveEditedImage(bitmapWrapper: BitmapWrapper): Result<String>
```

`SaveEditedImageUseCase.kt`의 클래스 선언 위에 한 줄을 둔다.

```kotlin
/** 손으로 다듬은 결과와 누끼 없이 쓰는 원본이 함께 쓴다 */
class SaveEditedImageUseCase
```

- [ ] **Step 8: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: PASS

- [ ] **Step 9: ktlint를 돌린다**

Run: `./gradlew :feature:segmentation:impl:ktlintCheck`
Expected: PASS

---

### Task 4: 실패 화면에 버튼 둘을 놓는다

디자인 `C-103-Error`의 버튼 두 개를 화면에 올리고 Route에 결선한다. 이 Task가 끝나면 기능이 손으로 만져진다.

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/SegmentationErrorScreen.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `SegmentationIntent.UseOriginal` (Task 3). Task 1이 만든 화면 시그니처(`title`·`description` 제거본)와 `strings.xml` 상태 위에 얹는다.
- Produces: `SegmentationErrorScreen(onClickRetry, onClickUseOriginal, onClickClose, modifier)`

- [ ] **Step 1: 버튼 라벨을 더한다**

`strings.xml`의 재시도 라벨 아래에 한 줄을 더한다.

```xml
    <string name="segmentation_error_retry">다시 시도</string>
    <string name="segmentation_error_use_original">편집 없이 사용</string>
```

- [ ] **Step 2: 화면에 두 번째 버튼을 놓는다**

`SegmentationErrorScreen.kt`의 시그니처에 콜백을 더한다.

```kotlin
@Composable
internal fun SegmentationErrorScreen(
    onClickRetry: () -> Unit,
    onClickUseOriginal: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

기존 `YGButton` 하나를 `Column` 으로 감싸 둘을 세로로 쌓는다. 두 번째 버튼은 `Medium.Secondary` 를 쓴다 — Gray100 채움에 Gray500 테두리라 디자인의 두 번째 버튼과 그대로 맞는다.

버튼 사이 간격은 `gap3`(8dp)다. ⚠️ `gap1` 은 2dp라 버튼 둘이 거의 붙는다.

설명 문구와 버튼 블록 사이는 `gap7`(24dp)다. **바깥 `Column` 의 `Arrangement.spacedBy` 를 걷고 명시적 `Spacer` 로 바꾼다** — 균일 배치를 남기면 스페이서 위아래로 8dp가 한 번 더 붙어 32dp가 된다.

```kotlin
            // 아이콘·문구·버튼의 간격이 서로 달라 균일 배치를 쓰지 않는다
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image( … )

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

                Column( … 제목·설명 … )

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))

                Column( … 버튼 둘 … )
            }
```

폭은 디자인 실측값 `161.5.dp` 로 둘을 같게 고정한다. `YGButton` 은 `modifier` 를 안 주면 자기 텍스트 폭으로 감싸서 글자 수가 다른 두 버튼이 어긋난다. 값은 감싸는 `Column` 에 한 번만 주고 버튼은 `fillMaxWidth()` 로 채운다 — 숫자가 한 곳에만 있어야 나중에 한 줄로 고친다.

```kotlin
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                    modifier = Modifier.width(BUTTON_WIDTH),
                ) {
                    YGButton(
                        text = stringResource(R.string.segmentation_error_retry),
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = onClickRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    YGButton(
                        text = stringResource(R.string.segmentation_error_use_original),
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = onClickUseOriginal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
```

폭 상수는 프리뷰 함수 위에 둔다.

```kotlin
/** 디자인 `C-103-Error` 실측값. 두 버튼이 같은 폭이라 감싸는 Column 이 한 번만 든다 */
private val BUTTON_WIDTH = 161.5.dp
```

import 둘을 더한다. 파일이 이미 `Column`·`Arrangement`·`Alignment`·`fillMaxWidth` 는 가지고 있다.

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
```

Preview에 콜백을 더한다.

```kotlin
@YGPreview
@Composable
private fun PreviewSegmentationErrorScreen() = PreviewBox {
    SegmentationErrorScreen(
        onClickRetry = {},
        onClickUseOriginal = {},
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

- [ ] **Step 3: Route에 결선한다**

```kotlin
        if (state.isError) {
            SegmentationErrorScreen(
                onClickRetry = { viewModel.processIntent(SegmentationIntent.Retry) },
                onClickUseOriginal = { viewModel.processIntent(SegmentationIntent.UseOriginal) },
                onClickClose = onClickClose,
                modifier = modifier.padding(innerPadding),
            )
```

- [ ] **Step 4: 모듈 전체가 빌드되고 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: ktlint를 돌린다**

Run: `./gradlew :feature:segmentation:impl:ktlintCheck`
Expected: PASS

- [ ] **Step 6: 앱을 빌드한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 화면을 눈으로 확인한다**

`PreviewSegmentationErrorScreen`을 IDE 프리뷰로 열어 디자인 `C-103-Error`와 대조한다. 아이콘, 제목, 설명, 버튼 둘의 순서와 색을 본다. **두 버튼의 폭이 같은지, 간격이 디자인과 맞는지를 특히 본다** — 이 둘은 테스트가 잡지 못하는 자리다.

⚠️ **실기기 확인은 재현 수단이 없을 수 있다.** 모듈이 이미 도착한 기기에서는 모듈 실패 경로를 만들 수 없다. 후보 0건 경로(피사체가 없는 사진)로는 실패 화면에 도달할 수 있으므로, 그 경로로 「편집 없이 사용」이 확인 화면까지 가는지 본다.

---

## 검증 체크리스트

구현이 끝나면 아래를 확인한다.

- [ ] `SegmentationErrorKind`가 저장소 어디에도 남아 있지 않다 — `grep -rn "SegmentationErrorKind" --exclude-dir=build .`가 0건이다.
- [ ] `errorKind`라는 이름이 남아 있지 않다 — `grep -rn "errorKind" --exclude-dir=build .`가 0건이다. **KDoc 안의 죽은 참조는 위 grep에 안 걸리므로 이 검사가 따로 필요하다.**
- [ ] `segmentation_module_error_`로 시작하는 문자열이 남아 있지 않다.
- [ ] 실패 화면이 뜨는 세 경로(모듈 실패·처리 실패·후보 0건)에서 문구가 같다.
- [ ] 디코드 실패는 실패 화면을 띄우지 않고 뒤로 간다.
- [ ] 「편집 없이 사용」이 파일을 한 개만 만든다.
- [ ] 커밋하지 않았다.
