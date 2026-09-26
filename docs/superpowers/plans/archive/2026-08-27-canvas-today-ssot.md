---
id: canvas-today-ssot
title: PR2 — 오늘 캔버스 인메모리 SSoT
status: done
type: work-order
created: 2026-08-27
updated: 2026-08-31
platforms: android
owner: Parfait 팀
related_adr: ADR-0029, ADR-0023, ADR-0026
related_spec: canvas-today-ssot-polling
related_code: CanvasLocalDataSource, CanvasLocalDataSourceImpl, ParfaitRepository, ParfaitRepositoryImpl, ParfaitRemoteDataSource, GetTodayParfaitUseCase, GetTodayParfaitFlowUseCase, RefreshTodayParfaitUseCase, GetParfaitDetailUseCase, CanvasVO, CanvasMainViewModel, CanvasMainUiState, CanvasBGEditViewModel, CanvasToppingPlaceViewModel, LogoutUseCase, TokenAuthenticator, LocalDataSourceModule
archived_reason: PR #404 로 develop 머지(2026-08-31) — 인메모리 SSoT 가 스택 3단의 둘째 단으로 함께 들어왔다
tags: [plan, parfait, canvas, state, cache]
---

# PR2 — 오늘 캔버스 인메모리 SSoT Implementation Plan

> ✅ **develop 에 머지됐다(2026-08-31, PR #404 `2c7bb31b`)** — 스택 3단이 한 머지로 들어왔고
> 머지 커밋의 트리가 브랜치 팁(`6bd21fb7`)과 같아 **충돌 해소 편집이 0건**이다. 아래 머리말이
> "release 계보에만 있다"고 적던 상태는 끝났다(OQ-P-311 ③). Task 의 체크는 완료 표시이고,
> 「수동 확인」은 실기기 확인 기록이 없어 그대로 둔다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ⚠️ **이 계획은 2026-08-30 델타(PR #407)를 모른다.** 아래 Task 에서 `loadTodayCanvas()` 를 통째로
> 갈아 끼우는 코드블록에 그날 붙은 `isInitialLoading` 이 없다. **적힌 그대로 구현하면 오늘 캔버스
> 첫 조회의 화면 덮개가 조용히 사라진다** → [open-questions](../../../synthesis/open-questions.md) OQ-P-326 ⑥.
>
> ✅ **코드는 이미 그 자리를 풀었다(2026-08-30, 스택 3단을 `27e85d0d` 위로 리베이스).** 이 단계의
> `loadTodayCanvas()` 는 갱신 호출을 `try`/`finally` 로 감싸 덮개를 들고, 갱신 실패는 캐시가 비어
> 있을 때만 `ShowTodayCanvasError` 를 낸다. 아래 코드블록은 그 감싸기가 빠진 판본이다.
>
> 📌 **`develop` 에도 들어왔다(2026-08-31, PR #404)** — 이 단은 PR3 스택에 실려 한 머지로 갔다.
> 구현이 끝난 채 `draft` 로 남아 있던 상태가 풀려 `status: done` 이 됐다(OQ-P-311 ③).

**Goal:** 오늘 캔버스를 `:data`의 인메모리 저장소 한 벌에 두고, 캔버스 메인·배경 편집·토핑 배치 세 화면이 `Flow`로 구독하게 한다.

**Architecture:** ADR-0023이 그룹에 세운 구조를 캔버스로 확장한다. `CanvasLocalDataSource`(`@Singleton` + `MutableStateFlow<Map<GroupId, CanvasVO>>`)를 신설하고, `ParfaitRepository`의 `getTodayCanvas`를 **구독·오늘 갱신·상세 갱신·정리·날짜 peek**으로 가른다. 값을 얻는 길이 둘이면 캐시가 곧 두 번째 출처가 되므로 갱신 함수는 `Result<Unit>`만 돌려준다. 세 화면은 조회 대신 구독한다.

**Tech Stack:** Kotlin, Coroutines/Flow, Hilt, MockK, Turbine, kotlinx-coroutines-test

**Spec:** [`parfait/specs/2026-08-27-canvas-today-ssot-polling.md`](../../specs/archive/2026-08-27-canvas-today-ssot-polling.md) 「PR2 — 오늘 캔버스 인메모리 SSoT」
**대응 ADR:** [`parfait/adr/0029-canvas-today-ssot-polling.md`](../../../adr/0029-canvas-today-ssot-polling.md)

**작업 저장소:** `TJYG-Android`. 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다. **PR1 위에 쌓는다.**

## Global Constraints

- **커밋은 하되 push·PR은 사용자 확인 후에만.**
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다).** 서버 동작을 써야 하면 반드시 `api/parfait.md` 같은 근거 문서를 괄호로 가리킨다.
  - 아키텍처 결정은 코드가 아니라 `parfait/adr/`·`parfait/architecture/`에 쓰고 코드에는 포인터 한 줄만 둔다.
- **주석·KDoc은 한국어로 쓴다.**
- **테스트 함수 이름은 기존 관례를 따른다** — 이 저장소에는 백틱 이름이 **한 건도 없다.** `invoke_condition_expectation` / `enter_...` 형태의 카멜케이스로 쓴다.
- **기존 테스트 헬퍼 이름을 지어내지 않는다.** 세 ViewModel 테스트의 팩토리는 `viewModel()`이고 목 필드는 `getTodayParfait`(Main·BGEdit)·`getTodayParfaitUseCase`(ToppingPlace)다. 캔버스 헬퍼는 각 파일의 기존 `canvas(...)`를 쓴다.
- **매퍼 단독 테스트를 만들지 않는다.** 판단이 든 변환은 DataSource 테스트의 케이스로 덮는다.
- **ViewModel 테스트만** `runTest(mainDispatcherRule.dispatcher)`로 스케줄러를 묶는다. `:data`·`:domain`에는 `Dispatchers.Main`이 없어 맨 `runTest`가 맞다.
- **hot flow 수집은 `backgroundScope`나 Turbine의 `flow.test { }`로 한다.**
- **이 PR은 폴링을 넣지 않는다.** 갱신 계기는 기존 진입 재조회뿐이다.
- **⚠️ `:domain`은 순수 Kotlin JVM 모듈이다**(`parfait.module.domain` → `parfait.kotlin.jvm`). `compileDebugKotlin`·`testDebugUnitTest` 같은 Android 변형 태스크가 **없다.** 전체 테스트는 반드시 `./gradlew test`로 돌린다 — `testDebugUnitTest`는 `:domain`을 조용히 건너뛴다.
- **CI가 게이트하는 것은 `ktlintCheck`와 `test`다.** `lintDebug`를 도는 CI는 없고, `:feature:groups:canvas:impl`의 lint는 기준선에서 이미 실패한다.

---

## File Structure

**신설**

| 파일 | 책임 |
|------|------|
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasLocalDataSource.kt` | 인터페이스 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasLocalDataSourceImpl.kt` | `@Singleton` 인메모리 구현 |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitFlowUseCase.kt` | 구독 + 날짜 낡음 필터 |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/RefreshTodayParfaitUseCase.kt` | 기존 `GetTodayParfaitUseCase` 이관 |
| `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasLocalDataSourceImplTest.kt` | 저장소 테스트 |
| `domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitFlowUseCaseTest.kt` | 날짜 필터 테스트 |

**변경**

| 파일 | 변경 |
|------|------|
| `domain/.../repository/parfait/ParfaitRepository.kt` | `getTodayCanvas` 제거, 다섯으로 분리, `getCanvasDetail` KDoc 링크 수정 |
| `data/.../repository/parfait/ParfaitRepositoryImpl.kt` | 로컬 데이터소스 조율 |
| `data/.../di/LocalDataSourceModule.kt` | `CanvasLocalDataSource` 바인딩 |
| `domain/.../usecase/parfait/GetParfaitDetailUseCase.kt` | KDoc의 깨진 링크 수정 |
| `domain/.../usecase/auth/LogoutUseCase.kt` | 캔버스 캐시 정리 추가 |
| `data/.../network/TokenAuthenticator.kt` | 캔버스 캐시 정리 추가 |
| `feature/.../viewmodel/CanvasMainViewModel.kt` | 구독 이관, `viewedCanvas` → `pastCanvas` + `displayedCanvas` |
| `feature/.../viewmodel/CanvasBGEditViewModel.kt` | 구독 이관(최초 방출 시딩) |
| `feature/.../viewmodel/CanvasToppingPlaceViewModel.kt` | 구독 이관 |
| 도메인 테스트 페이크 3종 | `GetTodayParfaitUseCaseTest`(rename)·`GetParfaitHistoriesUseCaseTest`·`GetParfaitYearsUseCaseTest` |
| `feature/.../viewmodel/CanvasMainViewModelTest.kt` | **일괄 개조**(Task 4 참고) |

**삭제**

`domain/.../usecase/parfait/GetTodayParfaitUseCase.kt`는 `RefreshTodayParfaitUseCase.kt`로 옮겨 가며 사라진다.

---

### Task 1: `CanvasLocalDataSource`를 만든다

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasLocalDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasLocalDataSourceImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasLocalDataSourceImplTest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`

**Interfaces:**
- Consumes: `CanvasVO`, `GroupId`
- Produces:
  - `fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>`
  - `fun cachedTodayCanvas(groupId: GroupId): CanvasVO?`
  - `fun saveTodayCanvas(groupId: GroupId, canvas: CanvasVO)`
  - `fun clear()`

- [x] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val GROUP_A = GroupId(1L)
private val GROUP_B = GroupId(2L)

class CanvasLocalDataSourceImplTest {
    private fun canvas(parfaitId: Long) = CanvasVO(
        parfaitId = ParfaitId(parfaitId),
        date = parfaitToday(),
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = emptyList(),
    )

    @Test
    fun todayCanvas_beforeAnySave_isNull() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()

        dataSource.todayCanvas(GROUP_A).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun saveTodayCanvas_emitsToThatGroupsSubscriber() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()

        dataSource.todayCanvas(GROUP_A).test {
            assertNull(awaitItem())

            dataSource.saveTodayCanvas(GROUP_A, canvas(100L))

            assertEquals(ParfaitId(100L), awaitItem()?.parfaitId)
        }
    }

    @Test
    fun saveTodayCanvas_forAnotherGroup_doesNotDisturbThisSubscriber() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()

        dataSource.todayCanvas(GROUP_A).test {
            assertNull(awaitItem())

            dataSource.saveTodayCanvas(GROUP_B, canvas(200L))

            expectNoEvents()
        }
    }

    @Test
    fun cachedTodayCanvas_readsWithoutSubscribing() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()
        assertNull(dataSource.cachedTodayCanvas(GROUP_A))

        dataSource.saveTodayCanvas(GROUP_A, canvas(100L))

        assertEquals(ParfaitId(100L), dataSource.cachedTodayCanvas(GROUP_A)?.parfaitId)
    }

    @Test
    fun clear_emptiesEveryGroup() = runTest {
        val dataSource = CanvasLocalDataSourceImpl()
        dataSource.saveTodayCanvas(GROUP_A, canvas(100L))

        dataSource.todayCanvas(GROUP_A).test {
            assertEquals(ParfaitId(100L), awaitItem()?.parfaitId)

            dataSource.clear()

            assertNull(awaitItem())
        }
    }
}
```

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:test --tests "*CanvasLocalDataSourceImplTest*"`
Expected: 컴파일 실패 — `CanvasLocalDataSourceImpl` 없음

- [x] **Step 3: 인터페이스와 구현을 쓴다**

`CanvasLocalDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow

/**
 * 오늘 캔버스의 인메모리 SSoT (`adr/0029-canvas-today-ssot-polling.md`).
 *
 * 지난 날 캔버스는 여기 두지 않는다 — 마감돼 바뀌지 않으므로 공유해 얻을 것이 없고,
 * 날짜 축을 들이면 무효화 규칙이 그만큼 늘어난다.
 *
 * IO 가 없어 모든 함수가 non-suspend 다.
 */
interface CanvasLocalDataSource {
    /** 아직 한 번도 못 받았으면 `null` (`api/parfait.md` — 서버가 오늘 캔버스를 만들어 주므로 "0건"이 없다) */
    fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

    /** 구독하지 않고 현재 값만 본다 — [todayCanvas] 를 구독하면 그 자체가 부수 효과를 갖는다 */
    fun cachedTodayCanvas(groupId: GroupId): CanvasVO?

    /** [CanvasVO] 에 그룹이 실려 오지 않아 따로 받는다 */
    fun saveTodayCanvas(groupId: GroupId, canvas: CanvasVO)

    fun clear()
}
```

`CanvasLocalDataSourceImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanvasLocalDataSourceImpl @Inject constructor() : CanvasLocalDataSource {
    private val canvases = MutableStateFlow<Map<GroupId, CanvasVO>>(emptyMap())

    /**
     * 맵 전체가 아니라 한 그룹으로 좁혀서 낸다 — [distinctUntilChanged] 가 없으면 남의 그룹
     * 캔버스가 저장될 때마다 이 구독자까지 재방출된다.
     */
    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvases
        .map { it[groupId] }
        .distinctUntilChanged()

    override fun cachedTodayCanvas(groupId: GroupId): CanvasVO? = canvases.value[groupId]

    override fun saveTodayCanvas(groupId: GroupId, canvas: CanvasVO) {
        canvases.update { it + (groupId to canvas) }
    }

    override fun clear() {
        canvases.value = emptyMap()
    }
}
```

- [x] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:test --tests "*CanvasLocalDataSourceImplTest*"`
Expected: PASS (5건)

- [x] **Step 5: DI 바인딩을 더한다**

`LocalDataSourceModule`의 `bindGroupLocalDataSource` 아래에 넣고 import 두 줄을 알파벳 순서에 맞게 더한다.

```kotlin
    @Binds
    @Singleton
    fun bindCanvasLocalDataSource(canvasLocalDataSourceImpl: CanvasLocalDataSourceImpl): CanvasLocalDataSource
```

- [x] **Step 6: 컴파일과 ktlint를 확인한다**

Run: `./gradlew :data:compileDebugKotlin :data:ktlintCheck`
Expected: 둘 다 BUILD SUCCESSFUL

- [x] **Step 7: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/local/ data/src/test/java/com/teamyg/parfait/data/source/parfait/local/ data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt
git commit -m "feat: 오늘 캔버스 인메모리 저장소를 만든다"
```

---

### Task 2: 저장소 표면을 가르고 갱신 UseCase를 옮긴다

⚠️ **이 둘은 반드시 한 커밋이다.** `ParfaitRepository.getTodayCanvas`를 지우는 순간 같은 모듈의 `GetTodayParfaitUseCase.kt`가 컴파일되지 않는다(그 파일이 그 함수를 두 번 부른다). 인터페이스 변경과 UseCase 이관을 나누면 중간에 `:domain`이 통째로 깨진다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/parfait/ParfaitRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/parfait/ParfaitRepositoryImpl.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/RefreshTodayParfaitUseCase.kt`
- Delete: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitUseCase.kt`
- Rename: `domain/src/test/.../GetTodayParfaitUseCaseTest.kt` → `RefreshTodayParfaitUseCaseTest.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/GetParfaitDetailUseCase.kt` (KDoc만)
- Modify: `domain/src/test/.../GetParfaitHistoriesUseCaseTest.kt`, `.../GetParfaitYearsUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 1의 `CanvasLocalDataSource`
- Produces:
  - `fun ParfaitRepository.todayCanvas(groupId: GroupId): Flow<CanvasVO?>`
  - `suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit>`
  - `suspend fun refreshTodayCanvasDetail(groupId: GroupId, parfaitId: ParfaitId): Result<Unit>`
  - `fun cachedTodayCanvasDate(groupId: GroupId): LocalDate?`
  - `fun clearTodayCanvas()`
  - `suspend operator fun RefreshTodayParfaitUseCase.invoke(groupId: GroupId, clock: Clock = Clock.System): Result<Unit>`

- [x] **Step 1: 인터페이스를 고친다**

`getTodayCanvas`를 지우고 다섯을 넣는다. 원래 그 함수에 있던 ⚠️ 부작용 경고는 `refreshTodayCanvas`로 옮기되 **근거 문서 포인터를 함께 옮긴다.**

```kotlin
    /**
     * 오늘 캔버스 구독. 아직 한 번도 못 받았으면 `null` 이다.
     *
     * 값을 얻는 길은 이것 하나뿐이다 — 갱신 함수가 값을 돌려주면 캐시가 곧 두 번째 출처가 된다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     */
    fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

    /**
     * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
     * (`api/parfait.md`). 부를 지점을 아껴야 한다.
     *
     * 오늘 날짜가 이미 마감돼 있으면 그것을 그대로 싣는다 — 그 캔버스에 쓰기를 보내면
     * 409 PARFAIT_ALREADY_CLOSED 로 돌아온다(`api/parfait.md`).
     */
    suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit>

    /**
     * 상세 조회로 오늘 캔버스 캐시를 갱신한다. [refreshTodayCanvas] 와 달리 부작용이 없어
     * 주기 갱신은 이쪽을 쓴다.
     *
     * [getCanvasDetail] 과 같은 엔드포인트지만 캐시에 싣는다는 점이 다르다 — 지난 날 조회가
     * 오늘 캔버스를 덮지 않도록 표면을 갈라 둔다.
     */
    suspend fun refreshTodayCanvasDetail(groupId: GroupId, parfaitId: ParfaitId): Result<Unit>

    /**
     * 캐시에 실린 오늘 캔버스의 날짜. 미조회면 `null`.
     *
     * 구독([todayCanvas])이 아닌 별도 표면인 이유: 그 [Flow] 는 나중에 폴링 수명을 나르게 되어,
     * 한 번 구독하는 것만으로 조회가 나간다(`adr/0029-canvas-today-ssot-polling.md`).
     * 날짜만 내므로 "값을 얻는 길은 하나"는 그대로다.
     */
    fun cachedTodayCanvasDate(groupId: GroupId): LocalDate?

    /** 세션 종료 정리. `:domain` 이 `:data` 를 볼 수 없어 저장소 표면으로 낸다 */
    fun clearTodayCanvas()
```

`kotlinx.coroutines.flow.Flow` import를 더한다. 같은 파일 `getCanvasDetail`의 KDoc에 있는 `[getTodayCanvas]` 링크를 `[refreshTodayCanvas]`로 고친다.

- [x] **Step 2: 구현을 고친다**

```kotlin
class ParfaitRepositoryImpl @Inject constructor(
    private val parfaitRemoteDataSource: ParfaitRemoteDataSource,
    private val canvasLocalDataSource: CanvasLocalDataSource,
) : ParfaitRepository {
    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvasLocalDataSource.todayCanvas(groupId)

    override fun cachedTodayCanvasDate(groupId: GroupId): LocalDate? =
        canvasLocalDataSource.cachedTodayCanvas(groupId)?.date

    override suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit> = parfaitRemoteDataSource
        .getTodayCanvas(groupId)
        .onSuccess { canvas -> canvasLocalDataSource.saveTodayCanvas(groupId, canvas) }
        .map { }
        .mapErrorToAppError()

    override suspend fun refreshTodayCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit> = parfaitRemoteDataSource
        .getCanvasDetail(groupId = groupId, parfaitId = parfaitId)
        .onSuccess { canvas -> canvasLocalDataSource.saveTodayCanvas(groupId, canvas) }
        .map { }
        .mapErrorToAppError()

    override fun clearTodayCanvas() = canvasLocalDataSource.clear()
    // 나머지 함수는 그대로
}
```

클래스 KDoc에 한 문단을 더한다.

```kotlin
 * 오늘 캔버스는 [CanvasLocalDataSource] 인메모리 캐시가 SSoT 다
 * (`adr/0029-canvas-today-ssot-polling.md`) — 조회는 캐시를 읽는 [Flow] 하나, 서버 재조회는
 * [refreshTodayCanvas]·[refreshTodayCanvasDetail] 로 갈라 둔다.
```

- [x] **Step 3: `RefreshTodayParfaitUseCase`를 만들고 옛 파일을 지운다**

```bash
git rm domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitUseCase.kt
```

```kotlin
package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘의 캔버스를 받아 캐시에 싣는다. 파르페 하루 경계(새벽 3시)를 지나며 요청이 나가
 * 어제 캔버스를 받으면 한 번만 다시 부른다.
 *
 * 값은 돌려주지 않는다 — 읽는 길은 [GetTodayParfaitFlowUseCase] 하나다.
 */
class RefreshTodayParfaitUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    /**
     * @param clock 파르페 하루 경계 판정에 쓰는 시계. 테스트에서 경계 앞뒤 시각을 고정한다.
     */
    suspend operator fun invoke(
        groupId: GroupId,
        clock: Clock = Clock.System,
    ): Result<Unit> {
        val first = parfaitRepository.refreshTodayCanvas(groupId)
        if (first.isFailure) return first

        // 오늘을 응답 뒤에 읽는다 — 요청이 도는 사이 하루 경계를 넘겼다면 어제 것이 실려 있다.
        // 구독이 아니라 peek 을 쓰는 이유는 cachedTodayCanvasDate KDoc 에 있다
        if (parfaitRepository.cachedTodayCanvasDate(groupId) == parfaitToday(clock)) return first

        // 두 번째도 어긋나면 기기와 서버의 시계가 어긋난 것이라 더 불러도 같은 답이 온다
        return parfaitRepository.refreshTodayCanvas(groupId)
    }
}
```

- [x] **Step 4: `GetParfaitDetailUseCase`의 KDoc 링크를 고친다**

그 파일 KDoc의 `[GetTodayParfaitUseCase] 를 쓰면 안 된다`를 `[RefreshTodayParfaitUseCase] 가 부르는 오늘 조회를 쓰면 안 된다`로 바꾼다.

- [x] **Step 5: 옛 테스트를 옮기고 페이크를 다시 세운다**

```bash
git mv domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitUseCaseTest.kt domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/RefreshTodayParfaitUseCaseTest.kt
```

지금 페이크는 `results: List<Result<CanvasVO>>`를 순서대로 돌려주고 `callCount`를 세며, 테스트 7건이 **반환된 `CanvasVO`의 `parfaitId`**로 단언한다. 페이크를 **상태를 가진 형태**로 바꾼다.

```kotlin
    private class FakeParfaitRepository(
        private val results: List<Result<CanvasVO>>,
    ) : ParfaitRepository {
        var callCount = 0
            private set

        /** 갱신이 실제로 실은 값. 옛 테스트가 반환값으로 보던 것을 여기서 본다 */
        var cached: CanvasVO? = null
            private set

        override suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit> {
            val result = results[minOf(callCount, results.lastIndex)]
            callCount++
            result.onSuccess { cached = it }
            return result.map { }
        }

        override fun cachedTodayCanvasDate(groupId: GroupId): LocalDate? = cached?.date

        override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = error("이 유스케이스는 구독하지 않는다")

        override suspend fun refreshTodayCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<Unit> = error("이 유스케이스는 상세를 갱신하지 않는다")

        override fun clearTodayCanvas() = error("이 유스케이스는 캐시를 지우지 않는다")

        // getYears·getPastCanvases·getCanvasDetail·changeCanvasBackground 는 기존 그대로 둔다
    }
```

단언 이관 규칙은 둘이다.

- 성공 경로 5건: `result.getOrNull()?.parfaitId` → `repository.cached?.parfaitId`. `callCount` 단언은 그대로.
- 실패 전파 2건(`invoke_firstCallFails_*`·`invoke_retryFails_*`): `result.isFailure`·`assertIs<IOException>`을 그대로 둔다.

- [x] **Step 6: 나머지 두 페이크를 채운다**

`GetParfaitHistoriesUseCaseTest`·`GetParfaitYearsUseCaseTest`의 페이크는 `getTodayCanvas`를 override하고 있다. 그 함수를 지우고 새 표면 다섯을 전부 `error("…")`로 채운다 — `error()`는 `Nothing`을 돌려주므로 어떤 반환 타입에도 대입된다. 두 테스트는 이 함수들을 부르지 않아 깨지지 않는다.

- [x] **Step 7: 컴파일과 테스트를 확인한다**

Run: `./gradlew :domain:compileKotlin :domain:compileTestKotlin :domain:test :data:compileDebugKotlin`
Expected: `:domain`·`:data` 모두 통과. 세 ViewModel이 아직 `GetTodayParfaitUseCase`를 주입받으므로 `:feature:groups:canvas:impl`만 깨진 채로 남는다 — Task 6까지 그렇다.

- [x] **Step 8: 커밋**

```bash
git add domain/ data/src/main/java/com/teamyg/parfait/data/repository/parfait/ParfaitRepositoryImpl.kt
git commit -m "refactor: 오늘 캔버스 조회를 구독과 갱신으로 가른다"
```

---

### Task 3: 구독 UseCase를 만든다

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitFlowUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitFlowUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 2의 `ParfaitRepository.todayCanvas`
- Produces: `operator fun invoke(groupId: GroupId, clock: Clock = Clock.System): Flow<CanvasVO?>`

- [x] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.domain.usecase.parfait

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val GROUP = GroupId(1L)

class GetTodayParfaitFlowUseCaseTest {
    private fun canvas(date: LocalDate) = CanvasVO(
        parfaitId = ParfaitId(100L),
        date = date,
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = emptyList(),
    )

    private fun useCaseWith(canvas: CanvasVO?) =
        GetTodayParfaitFlowUseCase(FakeParfaitRepository(flowOf(canvas)))

    @Test
    fun invoke_todaysCanvas_passesThrough() = runTest {
        useCaseWith(canvas(parfaitToday())).invoke(GROUP).test {
            assertEquals(ParfaitId(100L), awaitItem()?.parfaitId)
            awaitComplete()
        }
    }

    @Test
    fun invoke_yesterdaysCanvas_isFilteredToNull() = runTest {
        val yesterday = parfaitToday().minus(DatePeriod(days = 1))

        useCaseWith(canvas(yesterday)).invoke(GROUP).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_notFetchedYet_staysNull() = runTest {
        useCaseWith(null).invoke(GROUP).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
```

`FakeParfaitRepository`는 이 파일 안에 `private class`로 두고 `todayCanvas`만 값을 내며 나머지는 Task 2와 같은 방식으로 `error("…")`로 채운다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :domain:test --tests "*GetTodayParfaitFlowUseCaseTest*"`
Expected: 컴파일 실패

- [x] **Step 3: 구현을 쓴다**

```kotlin
package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘 캔버스 구독. 캐시에 실린 것이 오늘 날짜가 아니면 `null` 로 낸다.
 *
 * ⚠️ 이 필터는 업스트림이 방출할 때만 평가된다. 캐시에 `distinctUntilChanged` 가 걸려 있어
 * 값이 안 바뀌면 재방출이 없다 — 화면을 열어 둔 채 하루 경계를 넘기는 경우는 이 필터가 아니라
 * 별도 시간 축이 닫는다(`specs/2026-08-27-canvas-today-ssot-polling.md` 「하루 경계」).
 */
class GetTodayParfaitFlowUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    /**
     * @param clock 하루 경계 판정에 쓰는 시계. 주입하지 않으면 낡음 필터를 테스트로 고정할 수 없다.
     */
    operator fun invoke(
        groupId: GroupId,
        clock: Clock = Clock.System,
    ): Flow<CanvasVO?> = parfaitRepository
        .todayCanvas(groupId)
        .map { canvas -> canvas?.takeIf { it.date == parfaitToday(clock) } }
}
```

- [x] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :domain:test`
Expected: PASS

- [x] **Step 5: 커밋**

```bash
git add domain/
git commit -m "feat: 오늘 캔버스 구독 유스케이스를 만든다"
```

---

### Task 4: `CanvasMainViewModel`을 구독으로 옮긴다

**Files:**
- Modify: `feature/.../viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/.../viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2·3의 `RefreshTodayParfaitUseCase`·`GetTodayParfaitFlowUseCase`
- Produces:
  - `CanvasMainUiState.pastCanvas: CanvasVO?` (지난 날 전용)
  - `CanvasMainUiState.displayedCanvas: CanvasVO?` (파생)

- [x] **Step 1: 테스트 파일을 먼저 일괄 개조한다**

**이것을 건너뛰면 뒤 단계가 전부 빨갛게 나온다.** 이 파일은 조회 목킹으로 **상태를 채우는** 테스트가 14곳이고, `viewedCanvas`를 직접 읽는 단언이 3건이다.

1. 목 필드를 바꾼다 — `getTodayParfait: GetTodayParfaitUseCase`를 `getTodayParfaitFlow: GetTodayParfaitFlowUseCase`와 `refreshTodayParfait: RefreshTodayParfaitUseCase` 둘로.
2. 클래스에 구독 소스를 둔다.
   ```kotlin
   private val todayCanvases = MutableStateFlow<CanvasVO?>(null)
   ```
3. `@Before`의 `stubTheHappyPath()`에서 조회 스텁을 구독 스텁으로 바꾼다.
   ```kotlin
   every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
   coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
   todayCanvases.value = canvas(TODAY_PARFAIT_ID, today)
   ```
4. `coEvery { getTodayParfait(any()) } returns Result.success(canvas(...))`로 상태를 채우던 14곳을 전부 `todayCanvases.value = canvas(...)`로 옮긴다. 실패를 흘리던 자리는 `coEvery { refreshTodayParfait(any(), any()) } returns Result.failure(...)` + `todayCanvases.value = null`로 나눈다.
5. `viewedCanvas`를 읽는 단언 3건(`enter_loadsTodayCanvasAndThisYearsHistories`·`enter_beforeTheScreenIsShown_doesNotLoadTheCanvas`·`enter_whileViewingAPastDate_keepsThatDayAsIs`)을 `displayedCanvas`로 바꾼다.
6. `enter_whileViewingAPastDate_dayChanges_clearsTodayCanvas`는 **그대로 둔다** — Step 4가 그 동작을 유지한다.

🔁 **4번의 "14곳"은 이 계획이 쓰인 시점의 수다.** 2026-08-27 PR #400 이 같은 파일에 본인 토핑 탭
테스트 둘을 더해, 2026-08-28 스택 리베이스 시점에는 옮길 자리가 그보다 많았다. 그중 하나
(`clickTopping_placedByMe_navigatesToCanvasBGEditInsteadOfSpotlighting`)는 오늘 캔버스를 보는
테스트라 지시대로 `todayCanvases.value = …` 로 옮겼고, 다른 하나
(`clickTopping_placedByMe_whileViewingAPastDate_doesNothing`)는 **지난 날을 보는 테스트라 옮기지
않았다** — 지난 날 조회는 이 라운드 뒤에도 `getParfaitDetail` 이 맡는다. 4번을 다시 적용할 때는
"오늘 캔버스를 채우던 자리"만 대상임을 기준으로 삼는다.

- [x] **Step 2: 실패하는 테스트 셋을 더한다**

```kotlin
    @Test
    fun observeTodayCanvas_emission_landsOnTheScreen() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val canvas = canvas(TODAY_PARFAIT_ID, today)
        todayCanvases.value = canvas
        advanceUntilIdle()

        assertEquals(canvas, viewModel.state.value.todayCanvas)
        assertEquals(canvas, viewModel.state.value.displayedCanvas)
    }

    @Test
    fun observeTodayCanvas_whileViewingAPastDate_doesNotCoverThatDay() = runTest(mainDispatcherRule.dispatcher) {
        val pastDate = today.minus(DatePeriod(days = 1))
        val pastCanvas = canvas(PAST_PARFAIT_ID, pastDate)
        coEvery { getParfaitDetail(any(), ParfaitId(PAST_PARFAIT_ID)) } returns Result.success(pastCanvas)

        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(CanvasMainIntent.ClickDate(pastDate))
        advanceUntilIdle()

        todayCanvases.value = canvas(TODAY_PARFAIT_ID + 1, today)
        advanceUntilIdle()

        assertEquals(pastCanvas, viewModel.state.value.displayedCanvas)
    }

    @Test
    fun clickDate_pastDate_beforeTheDetailArrives_doesNotLookEmpty() = runTest(mainDispatcherRule.dispatcher) {
        val pastDate = today.minus(DatePeriod(days = 1))
        coEvery { getParfaitDetail(any(), any()) } coAnswers { awaitCancellation() }

        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(CanvasMainIntent.ClickDate(pastDate))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCanvasEmpty)
    }
```

`PAST_PARFAIT_ID`·달력 기록 스텁은 기존 `enter_whileViewingAPastDate_keepsThatDayAsIs`가 쓰는 것을 그대로 재사용한다.

- [x] **Step 3: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasMainViewModelTest*"`
Expected: 컴파일 실패 — `getTodayParfaitFlow`·`displayedCanvas` 없음

- [x] **Step 4: UiState를 고친다**

```kotlin
    /**
     * 오늘 캔버스. 저장소 구독 값이다 — 아직 못 받았거나 조회가 실패했으면 null 이다.
     *
     * [pastCanvas] 와 나눠 두는 이유는 토핑 추가·배경 편집이 언제나 오늘 것을 대상으로 해야
     * 해서다. 지난 날을 보다가 그 캔버스를 고치면 서버가 409 로 되돌려준다(`api/parfait.md`).
     */
    val todayCanvas: CanvasVO? = null,
    /** 달력에서 고른 **지난** 날의 캔버스 */
    val pastCanvas: CanvasVO? = null,
```

파생값과 그것을 보는 getter들. **폴백이 중요하다** — 지난 날을 처음 고르면 상세 응답이 올 때까지 `pastCanvas`가 null인데, 그때 빈 캔버스를 보여 주면 안 된다(달력이 기록 있는 날만 열어 주므로 "비어 있다"는 늘 거짓이다).

```kotlin
    /** 지난 날 상세를 기다리는 동안에는 직전에 보던 것을 그대로 둔다 */
    val displayedCanvas: CanvasVO?
        get() = if (isViewingToday) todayCanvas else (pastCanvas ?: todayCanvas)

    val canvasBackground: CanvasBackground?
        get() = displayedCanvas?.background

    val toppings: List<CanvasToppingVO>
        get() = displayedCanvas?.toppings.orEmpty().sortedBy { it.transform.positionZ }
```

이름을 바꾸는 이유를 `pastCanvas` KDoc 아래에 한 줄 남긴다 — `viewedCanvas`는 "지금 그려지는 캔버스"였고 그 역할은 `displayedCanvas`가 가져간다.

- [x] **Step 5: ViewModel을 구독으로 옮긴다**

생성자에서 `getTodayParfaitUseCase`를 둘로 바꾸고 `init`에 구독을 연다.

```kotlin
    init {
        viewModelLogger.i { "CanvasMainViewModel::init" }
        observeTodayCanvas()
        loadCanvasMainInfo()
        // 연도 목록은 해가 바뀔 때만 늘어나 재진입마다 물어볼 값이 아니다
        loadParfaitYears()
    }

    private fun observeTodayCanvas() {
        launch {
            getTodayParfaitFlowUseCase(groupId).collect { canvas ->
                updateState {
                    copy(
                        todayCanvas = canvas,
                        memberChips = canvas?.members?.toMemberChips() ?: memberChips,
                    )
                }
            }
        }
    }
```

`loadTodayCanvas()`는 갱신만 남긴다.

```kotlin
    private fun loadTodayCanvas() {
        launch(key = LOAD_TODAY_CANVAS_KEY) {
            refreshTodayParfaitUseCase(groupId).onFailure { throwable ->
                viewModelLogger.e(throwable) { "오늘 캔버스를 불러오지 못했다 - groupId: ${groupId.value}" }
                if (state.value.todayCanvas == null) {
                    postSideEffect(CanvasMainEffect.ShowTodayCanvasError)
                }
            }
        }
    }
```

**`syncToday()`에서는 `viewedCanvas` 대입 두 줄만 걷고 `todayCanvas = null`은 남긴다.** 날짜 필터는 방출이 있을 때만 돌아 이 자리를 대신하지 못한다 — 그것이 이 단계의 유일한 안전장치다.

```kotlin
    private fun syncToday() {
        val today = parfaitToday()
        if (today == state.value.today) return

        updateState {
            // 캐시에 distinctUntilChanged 가 걸려 있어 경계를 넘겨도 재방출이 없다 —
            // 어제 것을 오늘로 착각해 그 위에 토핑을 올리는 일이 없도록 여기서 비운다.
            // 갱신이 오면 구독이 다시 채운다
            if (isViewingToday) {
                copy(
                    today = today,
                    selectedDate = today,
                    displayedMonth = today.toFirstDayOfMonth(),
                    todayCanvas = null,
                )
            } else {
                copy(today = today, todayCanvas = null)
            }
        }
    }
```

`handleClickDate`·`handleClickGoToToday`의 `viewedCanvas = todayCanvas` 대입을 지운다 — 오늘로 돌아가면 파생값이 저절로 오늘 것을 고른다. `loadCanvasDetail`의 성공 처리는 `copy(pastCanvas = canvas)`로 바꾼다.

- [x] **Step 6: 화면은 확인만 한다**

`viewedCanvas` 참조는 `CanvasMainViewModel.kt`와 그 테스트에만 있다. `CanvasMainScreen`은 `canvasBackground`·`toppings` 파생값만 읽으므로 **바꿀 것이 없다.** grep으로 한 번 확인만 한다.

Run: `grep -rn "viewedCanvas" --include=*.kt feature/ | grep -v "/build/"`
Expected: 결과 없음

- [x] **Step 7: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasMainViewModelTest*"`
Expected: PASS

- [x] **Step 8: 커밋**

```bash
git add feature/groups/canvas/impl/
git commit -m "refactor: 캔버스 메인이 오늘 캔버스를 구독하게 한다"
```

---

### Task 5: `CanvasBGEditViewModel`을 구독으로 옮긴다

**Files:**
- Modify: `feature/.../viewmodel/CanvasBGEditViewModel.kt`
- Modify: `feature/.../viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2·3의 두 UseCase
- Produces: 없음(화면 내부)

- [x] **Step 1: 목킹을 구독으로 바꾸고 실패하는 테스트 둘을 더한다**

Task 4 Step 1과 같은 방식으로 목 필드를 바꾸고 `private val todayCanvases = MutableStateFlow<CanvasVO?>(null)`을 둔다.

```kotlin
    @Test
    fun observeCanvas_seedsBackgroundSelectionOnlyOnTheFirstEmission() = runTest(mainDispatcherRule.dispatcher) {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
        todayCanvases.value = canvas(background = CanvasBackground.Image(SAVED_IMAGE_URL))

        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(SAVED_IMAGE_URL, viewModel.state.value.selectedImageUri)

        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(
                uri = LOCAL_IMAGE_URI,
                source = PictureConfirmSource.GALLERY,
            ),
        )
        advanceUntilIdle()

        todayCanvases.value = canvas(background = CanvasBackground.Image("https://cdn.example.com/other.png"))
        advanceUntilIdle()

        assertEquals(LOCAL_IMAGE_URI, viewModel.state.value.selectedImageUri)
        assertEquals(PictureConfirmSource.GALLERY, viewModel.state.value.selectedImageSource)
    }

    @Test
    fun observeCanvas_movesTheEditTargetOnlyOnTheFirstEmission() = runTest(mainDispatcherRule.dispatcher) {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
        coEvery { changeCanvasBackground(any(), any(), any()) } returns Result.success(null)
        todayCanvases.value = canvas(parfaitId = OTHER_PARFAIT_ID)

        val viewModel = viewModel()
        advanceUntilIdle()

        // 최초 방출로 편집 대상이 옮겨간 뒤, 다음 방출은 그것을 다시 옮기지 않는다
        todayCanvases.value = canvas(parfaitId = THIRD_PARFAIT_ID)
        advanceUntilIdle()

        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify { changeCanvasBackground(any(), ParfaitId(OTHER_PARFAIT_ID), any()) }
    }
```

`canvas(parfaitId =, background =)` 헬퍼는 이 파일의 기존 `canvas(...)`를 필요한 만큼 넓혀 쓴다. `OTHER_PARFAIT_ID`·`THIRD_PARFAIT_ID`는 파일 상단 상수로 둔다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasBGEditViewModelTest*"`
Expected: 컴파일 실패

- [x] **Step 3: 구독으로 옮긴다**

```kotlin
    /** 최초 방출에만 서버 값을 시딩한다 — 이후 방출이 사용자의 선택을 덮으면 안 된다 */
    private var hasSeededFromCanvas = false

    init {
        viewModelLogger.i { "CanvasBGEditViewModel::init" }
        observeCanvas()
        refreshCanvas()
    }

    private fun observeCanvas() {
        launch {
            getTodayParfaitFlowUseCase(groupId).collect { canvas ->
                if (canvas == null) return@collect

                if (hasSeededFromCanvas.not() && canvas.parfaitId != parfaitId) {
                    viewModelLogger.e {
                        "편집을 연 캔버스와 조회 결과가 다르다 — 조회 쪽으로 옮긴다" +
                            " (열린 것: ${parfaitId.value}, 받은 것: ${canvas.parfaitId.value})"
                    }
                    parfaitId = canvas.parfaitId
                }

                confirmedToppings = canvas.toppings
                    .sortedBy { topping -> topping.transform.positionZ }
                    .map { topping -> topping.toToppingItem() }

                updateState { withCanvas(canvas = canvas, toppings = confirmedToppings) }
                hasSeededFromCanvas = true
            }
        }
    }

    /**
     * 편집을 여는 사이 다른 멤버가 올린 토핑까지 그려야 해서 진입할 때 한 번 더 받는다.
     *
     * ⚠️ 오늘 조회는 캔버스가 없으면 서버가 만들어 저장한다(`api/parfait.md`). 여기서 불러도
     * 캔버스 메인이 이미 만든 것을 다시 받을 뿐이라 늘어나지 않는다 — 이 화면은 그 메인을
     * 거쳐야만 열린다.
     */
    private fun refreshCanvas() {
        launch(key = LOAD_CANVAS_KEY) {
            refreshTodayParfaitUseCase(groupId).onFailure { throwable ->
                viewModelLogger.e(throwable) { "캔버스를 불러오지 못했다 - parfaitId: ${parfaitId.value}" }
                postSideEffect(effect = CanvasBGEditEffect.ShowError(throwable.toCanvasBGEditError()))
            }
        }
    }
```

기존 `loadCanvas()`의 KDoc에 있던 두 문장("왜 또 부르는가", "생성 부작용은 여기서 발동하지 않는다")을 위처럼 `refreshCanvas()`로 옮긴다. 그 정보를 잃으면 다음 사람이 이 호출을 중복으로 보고 지운다.

- [x] **Step 4: `withCanvas`가 배경만 최초 시딩하게 고친다**

```kotlin
    /**
     * 배경 선택은 최초 방출에만 서버 값에서 시딩한다 — 이후 방출까지 대입하면 사용자가 방금 고른
     * 배경이 되돌아간다.
     *
     * 저장된 배경 색을 못 읽으면 기본 색으로 두는데, 그때 확인을 누르면 배경이 팔레트 첫 색으로
     * 바뀐다 — 못 읽는 색을 그대로 되돌려 보내는 것보다 낫다.
     */
    private fun CanvasBGEditUiState.withCanvas(
        canvas: CanvasVO,
        toppings: List<CanvasToppingItem>,
    ): CanvasBGEditUiState {
        val withToppings = copy(toppings = toppings)
        if (hasSeededFromCanvas) return withToppings

        return withToppings.copy(
            selectedColor = (canvas.background as? CanvasBackground.Color)
                ?.value
                ?.toColorOrNull()
                ?: selectedColor,
            selectedImageUri = (canvas.background as? CanvasBackground.Image)?.url,
            selectedImageSource = null,
        )
    }
```

> **토핑은 이 단계에서 통째 대입이다.** 재방출 계기가 최초 로드뿐이라 지켜야 할 로컬 편집이 없다. 다만 캐시는 세 화면 공유이므로 다른 화면의 갱신이 방출을 일으킬 창이 원리적으로 열려 있고, 삭제한 토핑의 툼스톤도 없어 그때 지운 토핑이 되살아난다. 지금 그 경로는 없지만 PR3이 dirty 집합과 툼스톤으로 이 자리를 대체할 때까지 창은 열려 있다.

- [x] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasBGEditViewModelTest*"`
Expected: PASS

- [x] **Step 6: 커밋**

```bash
git add feature/groups/canvas/impl/
git commit -m "refactor: 배경 편집이 오늘 캔버스를 구독하게 한다"
```

---

### Task 6: `CanvasToppingPlaceViewModel`을 구독으로 옮긴다

**Files:**
- Modify: `feature/.../viewmodel/CanvasToppingPlaceViewModel.kt`
- Modify: `feature/.../viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2·3의 두 UseCase
- Produces: 없음(화면 내부)

- [x] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun observeCanvas_nullEmission_keepsTheLastBackground() = runTest(mainDispatcherRule.dispatcher) {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
        todayCanvases.value = canvas(background = CanvasBackground.Color("#FF0000"))

        val viewModel = viewModel()
        advanceUntilIdle()
        val seeded = viewModel.state.value.backgroundColor

        todayCanvases.value = null
        advanceUntilIdle()

        assertEquals(seeded, viewModel.state.value.backgroundColor)
    }
```

이 파일에는 캔버스 헬퍼가 없으므로 `canvas(...)`를 새로 만든다. 초안 방출 스텁은 기존 것을 그대로 쓴다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasToppingPlaceViewModelTest*"`
Expected: 컴파일 실패

- [x] **Step 3: 구독으로 옮긴다**

`canvasLoadedForGroupId` 가드와 `loadCanvasIfNeeded`를 지우고, 초안이 알려 준 `groupId`로 한 번만 구독을 연다. `BaseViewModel.launch`는 `key` 없이 부르면 non-null `Job`을 돌려주므로 그것으로 가드를 세운다. 초안은 여러 번 방출된다(확정 성공 뒤 `clear()`가 `null`을 흘린다).

```kotlin
    /** 초안이 그룹을 알려 준 뒤에야 구독을 열 수 있다. 그룹은 흐름 내내 바뀌지 않는다 */
    private var canvasObserveJob: Job? = null

    /**
     * ⚠️ 오늘 조회는 서버에서 그 날짜 파르페가 없으면 만들어 저장하는 부작용을 갖고 있다
     * (`api/parfait.md`). 이 흐름에 들어왔다는 것 자체가 오늘 캔버스가 있다는 뜻이라 여기서
     * 그 부작용이 발동할 일은 없다 — 호출 자체가 안전하다는 뜻은 아니다.
     */
    private fun observeCanvasOnce(groupId: GroupId) {
        if (canvasObserveJob != null) return

        canvasObserveJob = launch {
            getTodayParfaitFlowUseCase(groupId).collect { canvas ->
                // null 은 무시한다 — 비우면 배경이 흰색으로 튄다
                if (canvas == null) return@collect
                updateState { withCanvas(canvas) }
            }
        }

        launch(key = LOAD_CANVAS_KEY) {
            refreshTodayParfaitUseCase(groupId).onFailure { throwable ->
                // 조회 실패는 토핑 배치 자체를 막지 않는다 — 기본 배경·빈 토핑 목록으로 그대로 둔다
                viewModelLogger.e(throwable) { "캔버스를 불러오지 못했다 - groupId: ${groupId.value}" }
            }
        }
    }
```

`observeDraft()`의 `draft?.groupId?.let { groupId -> loadCanvasIfNeeded(groupId) }`를 `observeCanvasOnce(groupId)`로 바꾼다.

- [x] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test`
Expected: PASS

- [x] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/
git commit -m "refactor: 토핑 배치가 오늘 캔버스를 구독하게 한다"
```

---

### Task 7: 세션 종료 시 캔버스 캐시를 지운다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Modify: `domain/src/test/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCaseTest.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt`

**Interfaces:**
- Consumes: Task 2의 `ParfaitRepository.clearTodayCanvas`, Task 1의 `CanvasLocalDataSource.clear`
- Produces: 없음

- [x] **Step 1: 테스트 파일을 먼저 맞춘다**

`LogoutUseCaseTest`에는 `useCase()` 헬퍼가 **없다.** 필드 이름이 `logout`이고, 두 번째 기존 테스트는 `LogoutUseCase(authRepository, memberRepository, parfaitGroupRepository)`를 인라인으로 세운다.

1. `private val parfaitRepository: ParfaitRepository = mockk(relaxed = true)`를 더한다.
2. 필드 `logout`의 생성과 두 번째 테스트의 인라인 생성에 그 인자를 넣는다.
3. 아래 두 건을 더한다.

```kotlin
    @Test
    fun invoke_clearsTheCanvasCacheToo() = runTest {
        logout()

        verify { parfaitRepository.clearTodayCanvas() }
    }

    @Test
    fun invoke_clearsInMemoryCachesBeforeTheAccountStore() = runTest {
        logout()

        coVerifyOrder {
            parfaitGroupRepository.clearGroups()
            parfaitRepository.clearTodayCanvas()
            memberRepository.clearMyAccount()
        }
    }
```

`TokenAuthenticatorTest`에는 이미 `groupLocalDataSource.clear()`가 `userInfoLocalDataSource.clear()`보다 먼저인 것을 단언하는 테스트가 있다. 그 자리에 `canvasLocalDataSource.clear()`를 끼워 같은 방식으로 순서를 고정한다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :domain:test --tests "*LogoutUseCaseTest*"`
Expected: 컴파일 실패

- [x] **Step 3: `LogoutUseCase`를 고친다**

```kotlin
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val parfaitGroupRepository: ParfaitGroupRepository,
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val result = authRepository.logout()
        parfaitGroupRepository.clearGroups()
        parfaitRepository.clearTodayCanvas()
        runSuspendCatching { memberRepository.clearMyAccount() }
        return result
    }
}
```

기존 KDoc에서 "`ParfaitGroupRepository.clearGroups` 는 인메모리라 IO 실패 경로가 없어…"라고 적은 문단에 캔버스 캐시도 같은 성질이라는 구절을 더한다. **새 문단을 만들지 않는다.**

- [x] **Step 4: `TokenAuthenticator`를 고친다**

생성자에 `CanvasLocalDataSource`를 주입하고, `groupLocalDataSource.clear()` 바로 뒤·`userInfoLocalDataSource.clear()` 앞에 `canvasLocalDataSource.clear()`를 넣는다.

- [x] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :domain:test :data:test`
Expected: PASS

- [x] **Step 6: 커밋**

```bash
git add domain/ data/
git commit -m "fix: 세션이 끝날 때 캔버스 캐시도 지운다"
```

---

### Task 8: 전체 빌드와 테스트를 확인한다

**Files:** 없음(검증만)

- [x] **Step 1: 전체 컴파일**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 2: 전체 유닛 테스트**

Run: `./gradlew test`
Expected: PASS. **`testDebugUnitTest`를 쓰지 않는다** — `:domain`이 순수 JVM 모듈이라 그 태스크가 없고, 이 PR의 새 테스트 상당수가 거기 있다.

- [x] **Step 3: ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL. CI가 게이트하는 것이 이것이다.

- [x] **Step 4: 남은 호출자와 깨진 링크를 확인한다**

Run: `grep -rn "getTodayParfaitUseCase\|GetTodayParfaitUseCase\|getTodayCanvas" --include=*.kt . | grep -v "/build/" | grep -v "ParfaitRemoteDataSource"`
Expected: 결과 없음(원격 데이터소스의 동명 함수만 남는다)

- [x] **Step 5: 고칠 것이 있었다면 커밋**

```bash
git add -A
git commit -m "fix: 오늘 캔버스 구독 이관에서 남은 호출자를 고친다"
```

---

## 수동 확인 (구현자가 직접)

이 PR에는 폴링이 없으므로 "다른 기기에서 올린 토핑이 저절로 나타나는지"는 확인 대상이 아니다.

- [ ] 캔버스를 열면 오늘 캔버스가 그려지고 그룹명·멤버 칩이 뜬다
- [ ] 배경 편집으로 들어가면 같은 토핑이 보인다
- [ ] 배경 편집에서 갤러리로 배경을 고르면 그 선택이 유지된다
- [ ] 배경을 저장하고 되감으면 캔버스 메인에 그 배경이 반영된다
- [ ] 토핑을 추가하고 되감으면 캔버스 메인에 그 토핑이 나타난다
- [ ] 달력에서 지난 날을 고르는 순간 빈 캔버스가 깜빡이지 않는다
- [ ] "오늘의 파르페 가기"로 돌아오면 오늘 것이 보인다
- [ ] 로그아웃 후 다른 계정으로 들어가면 이전 계정의 캔버스가 보이지 않는다

## 검증 못 한 것

- 하루 경계(새벽 3시)를 넘겼을 때의 동작. 이 단계의 안전장치는 `syncToday()`의 무효화뿐이고 그것은 진입 시점에만 돈다. 실제 보장은 PR3의 티커가 만든다.

---

## Self-Review 결과

**스펙 커버리지** — 「저장소 구조」(Task 1), 「Repository」(Task 2), 「갱신·무효화 규칙」의 PR2 해당 행(Task 2·4), 「UseCase」(Task 2·3), 「화면 이관」 셋(Task 4·5·6), 「실패 표현」(Task 4의 `loadTodayCanvas`), 「세션 종료 정리」(Task 7), 「검증」(각 태스크 + Task 8).

**스펙과의 차이 1건** — 스펙의 Repository 표면은 넷인데 여기서는 **다섯**이다. `cachedTodayCanvasDate`를 더한 이유는 `RefreshTodayParfaitUseCase`가 하루 경계 재시도를 판정할 때 구독 표면을 쓰면 안 되기 때문이다. PR3에서 그 `Flow`가 폴링 수명을 나르게 되어, `first()` 한 번이 폴링을 기동했다 끄면서 부작용 있는 조회를 한 번 더 태운다. PR3 마지막 태스크에서 스펙에 반영한다.

**타입 일관성** — `CanvasLocalDataSource`의 함수 넷은 Task 1에서 정의하고 Task 2·7에서 쓴다. `ParfaitRepository`의 새 표면 다섯은 Task 2에서 정의하고 Task 3~7이 같은 이름을 쓴다. `displayedCanvas`·`pastCanvas`는 Task 4에서 정의한다. `hasSeededFromCanvas`는 Task 5에서 정의하고 PR3이 그대로 쓴다.

**중간 커밋의 컴파일 상태** — Task 2 커밋에서 `:feature:groups:canvas:impl` 하나만 깨지고 Task 6에서 복구된다. `:domain`·`:data`는 모든 커밋에서 통과한다.
