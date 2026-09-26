# 토핑 초안 접근 UseCase 분리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ✅ **완료·develop 머지(2026-09-09, PR #479 `5f3aee286`).** 설계대로 들어왔다 — UseCase 다섯의
> 이름·시그니처·패키지가 스펙 표와 같고 `feature/` 아래 `ToppingDraftRepository` 참조는 0건이다.
> 구현하며 갈린 테스트 둘의 기록은
> [스펙 「as-built」](../../specs/archive/2026-09-09-topping-draft-usecase-extraction.md)에 있다.
> 📌 같은 날 PR #480이 `RecordToppingDraftUseCase`·`EnsureDraftSubjectRecordedUseCase`에
> `sourceLongSide` 인자를 얹었다 — 이 계획이 만든 UseCase 층이 그 인자가 앉은 자리다.
> ⚠️ **체크박스는 실행 세션이 남기지 않아 전부 미체크다**(32개). 진행의 정본은 `git log`다.

**Goal:** `feature/*/impl`의 ViewModel 넷이 `ToppingDraftRepository`를 직접 부르는 자리를 없애고, 그 사이에 UseCase 다섯을 놓는다.

**Architecture:** `:domain`의 `usecase/topping/`에 UseCase 5종을 만든다. 넷은 Repository로 그대로 넘기는 위임이고, 다섯 번째 `EnsureDraftSubjectRecordedUseCase`만 `draft.first()`와 `record`를 조합해 재사용 진입을 판정한다. ViewModel은 생성자 의존성과 호출부만 바뀌고 화면 동작·effect는 하나도 바뀌지 않는다.

**Tech Stack:** Kotlin, Hilt(`@Inject constructor`), kotlinx.coroutines Flow, JUnit4 + kotlin.test + MockK + Turbine.

**Spec:** [`parfait/specs/2026-09-09-topping-draft-usecase-extraction.md`](../../specs/archive/2026-09-09-topping-draft-usecase-extraction.md)

**저장소:** 코드는 `TJYG-Android`(브랜치 `refactor/using-usecase`, 이미 체크아웃되어 있고 `develop`과 차이 0). 마지막 Task만 이 문서 저장소에서 한다.

## Global Constraints

- **동작을 바꾸지 않는다.** 화면 상태·side effect·호출 순서가 전부 그대로여야 한다. 이 라운드는 의존 표면만 옮긴다.
- `ToppingDraftRepository` 인터페이스와 `:data`의 `ToppingDraftRepositoryImpl`은 **한 줄도 고치지 않는다.**
- `record`의 `Boolean` 반환 규약을 유지한다. `Result`로 바꾸지 않는다.
- `SegmentationViewModel`의 `runSuspendCatching { … }.getOrDefault(false)` 래퍼는 **호출부에 그대로 둔다.** UseCase 안으로 옮기면 실패가 `ShowError` 대신 `launch(onError = …)` 경로로 빠져 화면 동작이 달라진다.
- 새 UseCase는 전부 `com.teamyg.parfait.domain.usecase.topping` 패키지, `@Inject constructor`, `operator fun invoke`.
- 각 Task 끝에서 커밋한다. **푸시와 PR 생성은 하지 않는다.**
- 주석·KDoc 규약(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다). 근거는 문서 포인터로 남긴다.

## File Structure

**신규 (`:domain`)**

| 파일 | 책임 |
|---|---|
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/GetToppingDraftFlowUseCase.kt` | 초안 흐름을 넘긴다 |
| `.../topping/StartToppingDraftUseCase.kt` | 흐름을 연다 |
| `.../topping/ClearToppingDraftUseCase.kt` | 초안을 비운다 |
| `.../topping/RecordToppingDraftUseCase.kt` | 알맹이·테두리를 적는다 |
| `.../topping/EnsureDraftSubjectRecordedUseCase.kt` | 초안이 이 알맹이를 가리키게 맞춘다(유일한 판정) |
| `domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/EnsureDraftSubjectRecordedUseCaseTest.kt` | 위 판정의 세 갈래 |

**수정 (`feature`)**

| 파일 | 바뀌는 것 |
|---|---|
| `feature/groups/canvas/impl/.../viewmodel/CanvasMainViewModel.kt` | `start` → `StartToppingDraftUseCase` |
| `feature/groups/canvas/impl/.../viewmodel/CanvasToppingPlaceViewModel.kt` | `draft`·`clear` → UseCase 둘 |
| `feature/segmentation/impl/.../viewmodel/SegmentationViewModel.kt` | `record` 2곳 → `RecordToppingDraftUseCase` |
| `feature/segmentation/impl/.../viewmodel/SegmentationConfirmViewModel.kt` | 진입 판정 → `EnsureDraftSubjectRecordedUseCase`, `draft`·`record` → UseCase 둘 |
| 위 넷의 테스트 4파일 | 더블을 Repository에서 UseCase로 교체 |

Gradle 스크립트는 손대지 않는다. `ModuleFeatureImplConventionPlugin`이 `:domain`을 이미 붙인다.

---

### Task 1: UseCase 5종 신설

먼저 판정 UseCase의 테스트를 쓰고, 그것을 통과시키며 다섯을 모두 만든다. 이 Task는 `:domain`만 건드리므로 ViewModel은 아직 Repository를 직접 본다.

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/GetToppingDraftFlowUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/StartToppingDraftUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/ClearToppingDraftUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/RecordToppingDraftUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/EnsureDraftSubjectRecordedUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/EnsureDraftSubjectRecordedUseCaseTest.kt`

**Interfaces:**
- Consumes: `ToppingDraftRepository`(`draft: Flow<ToppingDraft?>`, `start(GroupId, ParfaitId, Int)`, `clear()`, `record(String, String?, Int?, Float?): Boolean`), `ToppingDraft`(`subjectImagePath: String?`, `cutoutImagePath: String?`, `borderColorArgb: Int?`, `borderWidthDp: Float?`, `groupId`, `parfaitId`, `nextPositionZ`).
- Produces: 아래 다섯 클래스. Task 2~5가 이 이름과 시그니처를 그대로 쓴다.
  - `GetToppingDraftFlowUseCase.invoke(): Flow<ToppingDraft?>`
  - `StartToppingDraftUseCase.invoke(groupId: GroupId, parfaitId: ParfaitId, nextPositionZ: Int)` — suspend
  - `ClearToppingDraftUseCase.invoke()` — suspend
  - `RecordToppingDraftUseCase.invoke(subjectImagePath: String, cutoutImagePath: String?, borderColorArgb: Int?, borderWidthDp: Float?): Boolean` — suspend
  - `EnsureDraftSubjectRecordedUseCase.invoke(subjectImagePath: String): Boolean` — suspend

- [ ] **Step 1: 판정 UseCase의 실패 테스트를 쓴다**

`domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/EnsureDraftSubjectRecordedUseCaseTest.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SUBJECT_PATH = "/files/recent_images/a.png"
private const val OTHER_PATH = "/files/recent_images/b.png"

class EnsureDraftSubjectRecordedUseCaseTest {
    private val repository: ToppingDraftRepository = mockk()
    private val ensureDraftSubjectRecorded = EnsureDraftSubjectRecordedUseCase(repository)

    private fun givenDraft(subjectImagePath: String?) {
        every { repository.draft } returns flowOf(
            ToppingDraft(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(2L),
                nextPositionZ = 0,
                subjectImagePath = subjectImagePath,
                cutoutImagePath = null,
                borderColorArgb = null,
                borderWidthDp = null,
            ),
        )
    }

    @Test
    fun whenDraftAlreadyPointsToTheSubject_doesNotRecordAgain() = runTest {
        givenDraft(SUBJECT_PATH)

        assertTrue(ensureDraftSubjectRecorded(SUBJECT_PATH))

        coVerify(exactly = 0) { repository.record(any(), any(), any(), any()) }
    }

    @Test
    fun whenDraftPointsElsewhere_recordsTheSubjectOnly() = runTest {
        givenDraft(OTHER_PATH)
        coEvery { repository.record(any(), any(), any(), any()) } returns true

        assertTrue(ensureDraftSubjectRecorded(SUBJECT_PATH))

        // 테두리까지 비우는 것이 규약이다 — 알맹이가 바뀌면 그 전 테두리는 설 자리가 없다
        coVerify(exactly = 1) {
            repository.record(
                subjectImagePath = SUBJECT_PATH,
                cutoutImagePath = null,
                borderColorArgb = null,
                borderWidthDp = null,
            )
        }
    }

    @Test
    fun whenRecordFails_returnsFalse() = runTest {
        givenDraft(null)
        coEvery { repository.record(any(), any(), any(), any()) } returns false

        assertFalse(ensureDraftSubjectRecorded(SUBJECT_PATH))
    }
}
```

- [ ] **Step 2: 테스트가 컴파일 실패로 떨어지는지 본다**

Run: `./gradlew :domain:test --tests "*EnsureDraftSubjectRecordedUseCaseTest*"`
Expected: FAIL — `Unresolved reference: EnsureDraftSubjectRecordedUseCase`

- [ ] **Step 3: 위임 UseCase 넷을 만든다**

`GetToppingDraftFlowUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 토핑 만들기 흐름의 초안 구독. 호출 자체는 구독하지 않고 `Flow` 만 넘긴다 */
class GetToppingDraftFlowUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    operator fun invoke(): Flow<ToppingDraft?> = toppingDraftRepository.draft
}
```

`StartToppingDraftUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/**
 * 토핑 만들기 흐름을 연다. 이전 초안은 통째로 덮인다
 * (`adr/0026-topping-draft-datastore-ssot.md`).
 */
class StartToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    ) = toppingDraftRepository.start(
        groupId = groupId,
        parfaitId = parfaitId,
        nextPositionZ = nextPositionZ,
    )
}
```

`ClearToppingDraftUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/** 토핑 만들기 흐름을 닫는다 */
class ClearToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    suspend operator fun invoke() = toppingDraftRepository.clear()
}
```

`RecordToppingDraftUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/** 흐름이 만들어 낸 알맹이·테두리를 초안에 적는다 */
class RecordToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    /** @return 흐름이 열려 있지 않으면 `false`. 호출부가 이 값으로 실패를 알린다 */
    suspend operator fun invoke(
        subjectImagePath: String,
        cutoutImagePath: String?,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
    ): Boolean = toppingDraftRepository.record(
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )
}
```

- [ ] **Step 4: 판정 UseCase를 만든다**

`EnsureDraftSubjectRecordedUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 초안이 이 알맹이를 가리키게 맞춘다.
 *
 * 판정 기준은 "초안이 비었는가"가 아니라 "이 알맹이를 가리키는가"다
 * (`specs/archive/2026-08-20-c106-topping-place-api.md`).
 */
class EnsureDraftSubjectRecordedUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    /** @return 초안이 이 알맹이를 가리키게 되었으면 `true`. 이미 가리키던 경우도 포함한다 */
    suspend operator fun invoke(subjectImagePath: String): Boolean {
        val draftSubjectPath = toppingDraftRepository.draft.first()?.subjectImagePath
        if (draftSubjectPath == subjectImagePath) return true

        return toppingDraftRepository.record(
            subjectImagePath = subjectImagePath,
            cutoutImagePath = null,
            borderColorArgb = null,
            borderWidthDp = null,
        )
    }
}
```

- [ ] **Step 5: 테스트가 통과하는지 본다**

Run: `./gradlew :domain:test --tests "*EnsureDraftSubjectRecordedUseCaseTest*"`
Expected: PASS (3건)

- [ ] **Step 6: 린트와 함께 커밋한다**

```bash
./gradlew :domain:ktlintFormat :domain:ktlintCheck
git add domain/src/main/java/com/teamyg/parfait/domain/usecase/topping domain/src/test/java/com/teamyg/parfait/domain/usecase/topping
git commit -m "feat: 토핑 초안 접근 UseCase 다섯을 만든다"
```

---

### Task 2: CanvasMainViewModel을 UseCase로 돌린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: `StartToppingDraftUseCase.invoke(groupId, parfaitId, nextPositionZ)` (Task 1).
- Produces: 없음. 이 Task는 화면 하나로 닫힌다.

- [ ] **Step 1: 테스트의 더블을 갈아끼운다**

`CanvasMainViewModelTest.kt`에서 import와 필드, 그리고 `toppingDraftRepository`를 쓰던 스텁·검증을 전부 바꾼다.

```kotlin
// import 교체
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.StartToppingDraftUseCase

// 필드 교체
- private val toppingDraftRepository: ToppingDraftRepository = mockk(relaxUnitFun = true)
+ private val startToppingDraft: StartToppingDraftUseCase = mockk(relaxUnitFun = true)

// ViewModel 생성 인자 교체
- toppingDraftRepository = toppingDraftRepository,
+ startToppingDraft = startToppingDraft,
```

⚠️ `relaxUnitFun = true`를 **반드시 유지한다.** 이걸 빼면 `startToppingDraft` 호출을 스텁 없이
통과시키던 테스트 셋(`clickCamera_…`·`clickGallery_opensTheFlowToo`·`clickCamera_stacksTheNewToppingOnTop`)에서
MockK 예외가 `launch(onError = …)`에 잡혀 `ShowToppingFlowStartError`가 나가고 Navigate 이펙트가 안 나온다.

`coEvery { toppingDraftRepository.start(any(), any(), any()) }` 는
`coEvery { startToppingDraft(any(), any(), any()) }` 로, `coVerify { toppingDraftRepository.start(...) }` 는
`coVerify { startToppingDraft(...) }` 로 바꾼다. **인자 값과 `exactly` 수치, 단언 대상은 그대로 둔다.**

- [ ] **Step 2: 테스트가 컴파일 실패로 떨어지는지 본다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasMainViewModelTest*"`
Expected: FAIL — `No value passed for parameter 'toppingDraftRepository'` 또는 `Unresolved reference: startToppingDraft`

- [ ] **Step 3: ViewModel 생성자와 호출부를 바꾼다**

```kotlin
// import
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.StartToppingDraftUseCase

// 생성자 마지막 파라미터
- private val toppingDraftRepository: ToppingDraftRepository,
+ private val startToppingDraft: StartToppingDraftUseCase,
```

`startToppingFlow` 안:

```kotlin
- toppingDraftRepository.start(
+ startToppingDraft(
      groupId = groupId,
      parfaitId = canvas.parfaitId,
      nextPositionZ = canvas.nextPositionZ(),
  )
```

`launch(key = START_TOPPING_FLOW_KEY, onError = …)` 가드와 그 안의 로그·`postSideEffect(effect)`는 손대지 않는다.

- [ ] **Step 4: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasMainViewModelTest*"`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
./gradlew :feature:groups:canvas:impl:ktlintFormat :feature:groups:canvas:impl:ktlintCheck
git add feature/groups/canvas/impl
git commit -m "refactor: CanvasMainViewModel 이 초안 저장소 대신 UseCase 를 본다"
```

---

### Task 3: CanvasToppingPlaceViewModel을 UseCase로 돌린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Consumes: `GetToppingDraftFlowUseCase.invoke(): Flow<ToppingDraft?>`, `ClearToppingDraftUseCase.invoke()` (Task 1).
- Produces: 없음.

- [ ] **Step 1: 테스트의 더블을 갈아끼운다**

```kotlin
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
+ import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase

- private val toppingDraftRepository: ToppingDraftRepository = mockk()
+ private val getToppingDraftFlow: GetToppingDraftFlowUseCase = mockk()
+ private val clearToppingDraft: ClearToppingDraftUseCase = mockk(relaxed = true)
```

초안 흐름을 세우던 자리(`every { toppingDraftRepository.draft } returns …`)는
`every { getToppingDraftFlow() } returns …` 로 바꾼다. `clear` 스텁·검증은
`clearToppingDraft` 로 바꾼다.

⚠️ 이 파일은 **ViewModel을 세 자리에서 만든다.** 셋 다 인자를 둘로 나눠 넘겨야 한다.
`clear` 스텁은 여덟 자리, 검증은 세 자리에 흩어져 있으니 `toppingDraftRepository` 가
파일에서 완전히 사라졌는지 마지막에 확인한다.

- [ ] **Step 2: 테스트가 컴파일 실패로 떨어지는지 본다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingPlaceViewModelTest*"`
Expected: FAIL — `Unresolved reference: getToppingDraftFlow`

- [ ] **Step 3: ViewModel 생성자와 호출부 둘을 바꾼다**

```kotlin
// import
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
+ import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase

// 생성자 첫 파라미터
- private val toppingDraftRepository: ToppingDraftRepository,
+ private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
+ private val clearToppingDraft: ClearToppingDraftUseCase,
```

`observeDraft` 안:

```kotlin
- toppingDraftRepository.draft.collect { draft ->
+ getToppingDraftFlow().collect { draft ->
```

`handleOnClickConfirm` 안:

```kotlin
- toppingDraftRepository.clear()
+ clearToppingDraft()
```

⚠️ `clear()` 호출은 `postSideEffect(CanvasToppingPlaceEffect.PlaceSucceeded)` **뒤에 그대로 둔다.**
순서를 바꾸면 오버레이가 내려간 화면에 빈 캔버스가 잠깐 조작 가능한 상태로 남는다(그 자리 주석 참고).

- [ ] **Step 4: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingPlaceViewModelTest*"`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
./gradlew :feature:groups:canvas:impl:ktlintFormat :feature:groups:canvas:impl:ktlintCheck
git add feature/groups/canvas/impl
git commit -m "refactor: CanvasToppingPlaceViewModel 이 초안 저장소 대신 UseCase 를 본다"
```

---

### Task 4: SegmentationViewModel을 UseCase로 돌린다

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `RecordToppingDraftUseCase.invoke(subjectImagePath, cutoutImagePath, borderColorArgb, borderWidthDp): Boolean` (Task 1).
- Produces: 없음.

- [ ] **Step 1: 테스트의 더블을 갈아끼운다**

```kotlin
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase

- private val toppingDraftRepository: ToppingDraftRepository = mockk(relaxed = true)
+ private val recordToppingDraft: RecordToppingDraftUseCase = mockk(relaxed = true)
```

`coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns …` 는
`coEvery { recordToppingDraft(any(), any(), any(), any()) } returns …` 로 바꾼다.
`GoToConfirm`·`ShowError` 를 가르는 단언은 그대로 둔다 — 이 화면의 분기가 이 Task의 회귀 방어다.

- [ ] **Step 2: 테스트가 컴파일 실패로 떨어지는지 본다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: FAIL — `Unresolved reference: recordToppingDraft`

- [ ] **Step 3: ViewModel 생성자와 호출부 둘을 바꾼다**

```kotlin
// import
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase

// 생성자 마지막 파라미터
- private val toppingDraftRepository: ToppingDraftRepository,
+ private val recordToppingDraft: RecordToppingDraftUseCase,
```

`selectCandidate` 안 — **`runSuspendCatching` 래퍼는 그대로 둔다**:

```kotlin
  val recorded = runSuspendCatching {
-     toppingDraftRepository.record(
+     recordToppingDraft(
          subjectImagePath = result.trimmedSubjectImagePath,
          cutoutImagePath = result.subjectImagePath,
          borderColorArgb = null,
          borderWidthDp = null,
      )
  }.getOrDefault(false)
```

`useOriginal` 안도 같은 모양으로 바꾼다. 그쪽은 `subjectImagePath`·`cutoutImagePath` 둘 다 `path` 다.

```kotlin
  val recorded = runSuspendCatching {
-     toppingDraftRepository.record(
+     recordToppingDraft(
          subjectImagePath = path,
          cutoutImagePath = path,
          borderColorArgb = null,
          borderWidthDp = null,
      )
  }.getOrDefault(false)
```

- [ ] **Step 4: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
./gradlew :feature:segmentation:impl:ktlintFormat :feature:segmentation:impl:ktlintCheck
git add feature/segmentation/impl
git commit -m "refactor: SegmentationViewModel 이 초안 저장소 대신 UseCase 를 본다"
```

---

### Task 5: SegmentationConfirmViewModel의 판정을 UseCase로 올린다

이 Task만 테스트 단언이 바뀐다. 판정의 갈래를 가르던 커버리지는 Task 1의 `EnsureDraftSubjectRecordedUseCaseTest`가 이미 들고 있다.

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModelTest.kt`

**Interfaces:**
- Consumes: `EnsureDraftSubjectRecordedUseCase.invoke(subjectImagePath): Boolean`, `GetToppingDraftFlowUseCase.invoke(): Flow<ToppingDraft?>`, `RecordToppingDraftUseCase.invoke(…): Boolean` (Task 1).
- Produces: 없음.

- [ ] **Step 1: 테스트의 더블과 판정 테스트를 다시 쓴다**

```kotlin
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.EnsureDraftSubjectRecordedUseCase
+ import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
+ import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase

- private val toppingDraftRepository: ToppingDraftRepository = mockk()
+ private val ensureDraftSubjectRecorded: EnsureDraftSubjectRecordedUseCase = mockk()
+ private val getToppingDraftFlow: GetToppingDraftFlowUseCase = mockk()
+ private val recordToppingDraft: RecordToppingDraftUseCase = mockk()
```

ViewModel 생성 헬퍼 둘(`viewModel()`·`reuseViewModel(handle)`)의 `toppingDraftRepository = …`
인자도 세 UseCase 인자로 바꾼다.

⚠️ 이 파일은 `givenDraft` **밖에서도** 초안 흐름을 직접 세운다. 네 자리를 함께 바꿔야 한다 —
`draft_turnsEmptyMidSession_…`(여러 값 방출), `draft_throws_…`(`flow { throw … }`),
`reuseEntry_withEmptyDraft_recordsBeforeObserving`(끝나지 않는 `MutableStateFlow`),
`reuseEntry_afterBorderEdit_survivesProcessDeath_…`(`draftFlow`). 전부
`every { getToppingDraftFlow() } returns …` 로 바꾼다.

`givenDraft(draft)` 헬퍼는 다음으로 바꾼다.

```kotlin
private fun givenDraft(draft: ToppingDraft?) {
    every { getToppingDraftFlow() } returns flowOf(draft)
    coEvery { recordToppingDraft(any(), any(), any(), any()) } returns true
    coEvery { ensureDraftSubjectRecorded(any()) } returns true
}
```

테스트 본문은 이렇게 정리한다.

- `onEnter_writesNothing` — 재사용 진입이 아닌 경로다. 단언을
  `coVerify(exactly = 0) { ensureDraftSubjectRecorded(any()) }` 로 바꾼다. 진입 종류 가드가
  화면에 남는다는 것을 이 테스트가 지킨다.
- `reuseEntry_whenDraftAlreadyHasSubject_doesNotRecordAgain` 과
  `reuseEntry_whenDraftHasDifferentSubject_recordsTheNewOne` — **둘을 지운다.** 두 갈래를 가르던
  것은 판정이고 그것은 이제 `EnsureDraftSubjectRecordedUseCaseTest`가 덮는다. 대신 아래 하나를
  넣는다.

`reuseViewModel(handle)` 헬퍼가 이미 `subjectImagePath = REUSED_PATH`·`cutoutImagePath = null`
로 재사용 진입을 세운다. 그것을 그대로 쓴다.

```kotlin
@Test
fun reuseEntry_ensuresTheDraftPointsToTheSubject() = runTest(mainDispatcherRule.dispatcher) {
    givenDraft(draft(subjectImagePath = REUSED_PATH, cutoutImagePath = null))

    reuseViewModel()
    advanceUntilIdle()

    coVerify(exactly = 1) { ensureDraftSubjectRecorded(REUSED_PATH) }
}
```

- `reuseEntry_withEmptyDraft_recordsBeforeObserving` — 판정이 구독보다 먼저 돈다는 순서를 지키는
  테스트다. 단언 대상만 `ensureDraftSubjectRecorded` 로 바꾸고 순서 검증은 남긴다.

```kotlin
@Test
fun reuseEntry_ensuresBeforeObserving() = runTest(mainDispatcherRule.dispatcher) {
    givenDraft(draft(subjectImagePath = null, cutoutImagePath = null))

    reuseViewModel()
    advanceUntilIdle()

    coVerifyOrder {
        ensureDraftSubjectRecorded(REUSED_PATH)
        getToppingDraftFlow()
    }
}
```

`io.mockk.coVerifyOrder` import를 더한다.

원 테스트는 "끝나지 않는 `MutableStateFlow`"를 쓰고 "`flowOf` 는 곧장 완결돼 순서가 뒤집혀도
테스트를 속인다"는 주석을 달고 있다. 여기서는 그 장치를 버려도 된다 — 순서를 지키는 것이
완결 시점이 아니라 `coVerifyOrder` 이기 때문이다.

- `reuseEntry_afterBorderEdit_survivesProcessDeath_withoutOverwritingTheEdit` — `SavedStateHandle`
  플래그가 화면에 남는다는 것을 지키는 테스트다. 단언을
  `coVerify(exactly = 1) { ensureDraftSubjectRecorded(REUSED_PATH) }` 로 바꾼다. 프로세스 사망
  복원 뒤에도 판정이 **한 번만** 도는 것이 이 테스트의 요지다.
  ⚠️ 이 테스트는 `givenDraft`를 쓰지 않으므로 **스텁을 직접 넣어야 한다** —
  `coEvery { ensureDraftSubjectRecorded(any()) } returns true` 와
  `every { getToppingDraftFlow() } returns draftFlow`. 빠뜨리면 첫 ViewModel에서 예외가 나
  플래그가 안 서고, 복원된 ViewModel이 한 번 더 불러 `exactly = 1` 이 깨진다.
- 판정이 실패하면 `DraftMissing`이 나가는 것을 지키는 테스트를 더한다.

```kotlin
@Test
fun reuseEntry_whenEnsureFails_reportsMissingDraft() = runTest(mainDispatcherRule.dispatcher) {
    givenDraft(draft(subjectImagePath = null, cutoutImagePath = null))
    coEvery { ensureDraftSubjectRecorded(any()) } returns false

    val viewModel = reuseViewModel()

    viewModel.effect.test {
        advanceUntilIdle()
        assertEquals(SegmentationConfirmEffect.DraftMissing, awaitItem())
    }
}
```

`assertEquals`는 이미 import되어 있다. `assertIs`는 이 파일에 없으므로 쓰지 않는다.

`onEditResult_recordsBorderValues` 는 더블 이름만 `recordToppingDraft` 로 바꾸고 단언은 그대로 둔다.

⚠️ 지우는 테스트 하나(`reuseEntry_whenDraftAlreadyHasSubject_doesNotRecordAgain`)가 **화면 상태도
단언한다** — 재사용 진입에서 기존 테두리 색이 화면에 살아남는 것이다. UseCase 테스트는 화면을
보지 않으므로 이 단언이 사라진다. 새로 넣는
`reuseEntry_ensuresTheDraftPointsToTheSubject` 에 그 단언을 함께 옮긴다.

```kotlin
assertEquals(0xFF00FF00.toInt(), viewModel.state.value.borderColorArgb)
```

(`reuseViewModel()` 의 반환을 변수로 받아 쓴다. 초안 `draft(...)` 인자에 그 테두리 색을 실어야
한다 — 지우는 테스트가 세우던 값을 그대로 가져온다.)

- [ ] **Step 2: 테스트가 컴파일 실패로 떨어지는지 본다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationConfirmViewModelTest*"`
Expected: FAIL — `Unresolved reference: ensureDraftSubjectRecorded`

- [ ] **Step 3: ViewModel 생성자를 바꾼다**

```kotlin
// import
- import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
+ import com.teamyg.parfait.domain.usecase.topping.EnsureDraftSubjectRecordedUseCase
+ import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
+ import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
- import kotlinx.coroutines.flow.first

// 생성자
- private val toppingDraftRepository: ToppingDraftRepository,
+ private val ensureDraftSubjectRecorded: EnsureDraftSubjectRecordedUseCase,
+ private val getToppingDraftFlow: GetToppingDraftFlowUseCase,
+ private val recordToppingDraft: RecordToppingDraftUseCase,
```

- [ ] **Step 4: `init` 판정을 UseCase 호출로 줄인다**

`init` 블록의 재사용 진입 갈래를 다음으로 바꾼다. 진입 종류 판정과 `hasRecordedEntrySubject`
플래그는 화면에 그대로 남는다.

```kotlin
init {
    launch(onError = { reportMissingDraft() }) {
        val isReuseEntry = cutoutImagePath == null
        if (isReuseEntry && !hasRecordedEntrySubject) {
            // 못 적었으면 표시를 남기지 않아 복원된 화면이 다시 적어 본다
            if (ensureDraftSubjectRecorded(subjectImagePath)) {
                hasRecordedEntrySubject = true
            } else {
                reportMissingDraft()
            }
        }

        collectDraft()
    }

    observeTutorial()
}
```

`subjectImagePath`는 `val` 없는 생성자 파라미터라 `init` 블록에서 그대로 보인다. 지금 코드가
참조하는 방식 그대로이므로 새로 붙일 것이 없다.

- [ ] **Step 5: 나머지 두 호출부를 바꾼다**

`collectDraft` 안:

```kotlin
- toppingDraftRepository.draft.collect { draft ->
+ getToppingDraftFlow().collect { draft ->
```

`record(result)` 안:

```kotlin
- val recorded = toppingDraftRepository.record(
+ val recorded = recordToppingDraft(
      subjectImagePath = result.subjectImagePath,
      cutoutImagePath = result.cutoutImagePath,
      borderColorArgb = border?.colorArgb,
      borderWidthDp = border?.widthDp,
  )
```

`launch(onError = { postSideEffect(SegmentationConfirmEffect.DraftWriteFailed) })` 가드와
`if (!recorded) postSideEffect(…)` 분기는 그대로 둔다.

- [ ] **Step 6: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationConfirmViewModelTest*"`
Expected: PASS

- [ ] **Step 7: 저장소 전체에서 직접 의존이 사라졌는지 확인한다**

```bash
grep -rn "ToppingDraftRepository" --include='*.kt' feature/
```

Expected: 출력 0줄. 남아 있으면 그 파일을 마저 고친다.

- [ ] **Step 8: 전체 검증 후 커밋한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: 전부 통과

```bash
git add feature/segmentation/impl
git commit -m "refactor: SegmentationConfirmViewModel 의 재사용 진입 판정을 UseCase 로 올린다"
```

---

### Task 6: 문서를 as-built로 맞춘다

이 Task만 문서 저장소(`team-yg-pesonal-agent`, 브랜치 `docs/spec-topping-draft-usecase-extraction`)에서 한다.

**Files:**
- Modify: `parfait/specs/2026-09-09-topping-draft-usecase-extraction.md`
- Modify: `parfait/specs/README.md`
- Modify: `parfait/plans/README.md`

- [ ] **Step 1: 스펙 프론트매터를 올린다**

`status: draft` → `status: implemented`, `verified:` 에 확인 날짜를 적는다. 구현하며 설계와
달라진 것이 있으면 본문 끝에 **as-built** 절로 남긴다.

- [ ] **Step 2: 인덱스 두 곳을 갱신한다**

`parfait/specs/README.md`의 해당 행 상태를 바꾸고, `parfait/plans/README.md` 표에 이 계획서
행을 더한다(Task 수, 신규·수정 파일 수, 결과 요약).

- [ ] **Step 3: 커밋한다**

```bash
git add parfait/specs parfait/plans
git commit -m "docs: 토핑 초안 UseCase 분리 결과를 스펙과 인덱스에 반영한다"
```
