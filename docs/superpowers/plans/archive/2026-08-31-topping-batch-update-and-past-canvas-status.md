# 토핑 일괄 수정 전환 및 과거 캔버스 status 수용 Implementation Plan

> ✅ **완료·develop 머지(2026-09-01, PR #428 `e870fb87` — 브랜치 `feature/#427-sync-backend-api-260831`
> 팁 `a8d2efd5`와 트리가 같아 충돌 해소 편집이 0건)** — 5 Task 전량이 develop에 있다. 체크박스는
> 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> 계획과 갈린 것은 설계가 아니라 표기 정정뿐이다 — 브리프가 준 검증 명령
> (`:domain:testDebugUnitTest`·`:domain:compileDebugKotlin`)이 실제로 없는 태스크라
> `:domain:test`·`:domain:compileKotlin`으로 대체 실행했고, ktlint 120자 규칙에 걸린 줄을 값 변경 없이
> 재포맷했다. 머지 뒤 문서 커밋 둘(`850a5be5`·`a8d2efd5`)이 주석 군더더기를 걷고 그룹 목록
> `recentImageUrl`의 바뀐 뜻을 KDoc에 반영했다(OQ-P-336 ③). 유닛 996 → **1012건**.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** C-301 편집 탭 확인 버튼이 토핑 위치 수정을 단건 PATCH N회 대신 일괄 PATCH 1회로 보내게 하고, 과거 캔버스 목록 응답의 `status`를 도메인 VO까지 받는다.

**Architecture:** 서버가 신설한 `PATCH .../parfaits/{parfaitId}/images`를 `:data`(Service·DTO·매퍼·DataSource) → `:domain`(Repository·UseCase) → `:feature`(ViewModel) 순으로 관통시키고, 소비처가 하나뿐인 단건 위치 수정 경로는 같은 라운드에서 걷어낸다. 확인 버튼의 저장 흐름은 토핑 단위 병렬에서 **축 단위**(변형 일괄 1회 + 테두리 병렬 N회)로 바뀐다. 과거 목록 `status`는 기존 `toCanvasStatus()` 매퍼를 재사용해 DTO·VO에만 더하고 화면 판정은 건드리지 않는다.

**Tech Stack:** Kotlin, Retrofit, kotlinx.serialization, Hilt, kotlinx-coroutines-test, MockK, kotlin.test, Turbine

**Spec:** [`parfait/specs/archive/2026-08-31-topping-batch-update-and-past-canvas-status.md`](../../specs/archive/2026-08-31-topping-batch-update-and-past-canvas-status.md)

**대상 저장소·브랜치:** `TJYG-Android`, 브랜치 `feature/#427-sync-backend-api-260831`(이미 생성돼 있고 `origin/develop`과 같은 지점이다). 경로는 `wiki/personal-private/project-paths.md`의 `TJYG-Android` 값.

## Global Constraints

- **커밋은 하되 push·PR은 하지 않는다.** 각 Task 끝에서 커밋한다. `git push`·`gh pr create`는 사용자 승인 없이 실행하지 않는다.
- **기존 파일을 전문으로 덮어쓰지 않는다.** 수정은 해당 부분만 바꾼다.
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 써야 하면 근거 문서를 가리킨다.
- **매퍼 단독 테스트를 만들지 않는다.** 판단이 든 변환은 DataSource 테스트의 케이스로 잠근다.
- **라인번호·색 hex·변동 수치를 문서에 적지 않는다.** 근거는 파일명 + 심볼명으로 적는다.
- 서버 계약의 정본은 [`parfait/api/parfait-image.md`](../../../api/parfait-image.md)와 [`parfait/api/parfait.md`](../../../api/parfait.md)다.
- 검증 명령: `./gradlew :data:testDebugUnitTest :domain:test :feature:groups:canvas:impl:testDebugUnitTest` 및 `./gradlew ktlintCheck`.

---

### Task 1: 과거 캔버스 목록 `status` 수용

일괄 PATCH와 독립적이라 먼저 끝낸다. `:data`와 `:domain`만 건드리고 화면 판정은 안 바꾼다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/PastParfaitsResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/PastCanvasVO.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: 없음(이 Task가 첫 Task다).
- Produces: `PastCanvasVO(parfaitId: ParfaitId, date: LocalDate, status: CanvasStatus, thumbnailUrl: String?, toppingCount: Int)` — 뒤 Task는 이 타입을 쓰지 않지만 기존 테스트(`CanvasMainViewModelTest`·`GetParfaitHistoriesUseCaseTest`·`GetParfaitYearsUseCaseTest`·`GetTodayParfaitFlowUseCaseTest`)가 이 생성자를 부르므로 인자 추가에 맞춰 함께 고쳐야 컴파일된다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ParfaitRemoteDataSourceImplTest.kt`의 `getPastCanvases_serviceReturnsList_mapsCountAndThumbnail` 아래에 두 케이스를 더한다. 기존 케이스의 `PastParfaitResponse(...)` 호출에도 `status` 인자를 넣어야 컴파일된다 — 기존 두 원소에 각각 `status = "CLOSED"`, `status = "EMPTY"`를 준다.

```kotlin
    @Test
    fun getPastCanvases_mapsStatusOfEachElement() = runTest {
        // Given 서버가 상태가 다른 캔버스 셋을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(
            listOf(
                PastParfaitResponse(parfaitId = 3L, date = "2026-08-14", status = "ACTIVE", thumbnailUrl = null, imageCount = 0),
                PastParfaitResponse(parfaitId = 2L, date = "2026-08-13", status = "CLOSED", thumbnailUrl = null, imageCount = 4),
                PastParfaitResponse(parfaitId = 1L, date = "2026-08-12", status = "EMPTY", thumbnailUrl = null, imageCount = 0),
            ),
        )

        // When 과거 목록 조회
        val canvases = dataSource.getPastCanvases(GroupId(1L)).getOrThrow()

        // Then 오늘 조회·상세와 같은 enum 으로 온다
        assertEquals(
            listOf(CanvasStatus.ACTIVE, CanvasStatus.CLOSED, CanvasStatus.EMPTY),
            canvases.map { it.status },
        )
    }

    @Test
    fun getPastCanvases_unknownStatus_fallsBackToUnknown() = runTest {
        // Given 서버가 앱이 모르는 상태를 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(
            listOf(
                PastParfaitResponse(parfaitId = 3L, date = "2026-08-14", status = "ARCHIVED", thumbnailUrl = null, imageCount = 1),
            ),
        )

        // When 과거 목록 조회
        val canvases = dataSource.getPastCanvases(GroupId(1L)).getOrThrow()

        // Then 목록을 버리지 않고 UNKNOWN 으로 접는다
        assertEquals(CanvasStatus.UNKNOWN, canvases.first().status)
    }

    @Test
    fun getPastCanvases_activeCanvasWithNoToppings_isNotEmptyStatusButIsEmptyCount() = runTest {
        // Given 오늘 캔버스가 아직 비어 있다 — 마감된 EMPTY 와 다른 상태다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(
            listOf(
                PastParfaitResponse(parfaitId = 3L, date = "2026-08-14", status = "ACTIVE", thumbnailUrl = null, imageCount = 0),
            ),
        )

        // When 과거 목록 조회
        val canvas = dataSource.getPastCanvases(GroupId(1L)).getOrThrow().first()

        // Then 두 축이 갈린다 — 달력 점은 개수 축을 쓴다(api/parfait.md)
        assertEquals(CanvasStatus.ACTIVE, canvas.status)
        assertTrue(canvas.isEmpty)
    }
```

`CanvasStatus`와 `assertTrue`는 이 파일이 이미 import하고 있다(오늘 조회·상세 케이스가 쓴다). **더 넣지 마라** — 중복 import는 ktlint `no-duplicate-imports`에서 걸린다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*ParfaitRemoteDataSourceImplTest*'`
Expected: 컴파일 실패 — `PastParfaitResponse`에 `status` 파라미터가 없고 `PastCanvasVO`에 `status` 프로퍼티가 없다.

- [ ] **Step 3: 응답 DTO에 필드를 더한다**

`PastParfaitsResponse.kt`의 `PastParfaitResponse`를 이렇게 만든다(파일의 나머지는 그대로 둔다).

```kotlin
/**
 * @param status 오늘 조회·상세와 같은 값 집합이다. EMPTY 는 "비어 있음"이 아니라
 * "빈 채로 마감됨"이라 imageCount == 0 과 뜻이 다르다(`api/parfait.md`).
 * @param thumbnailUrl 서버가 항상 null 을 넣는다. 채우는 코드가 없다(`api/parfait.md`).
 */
@Serializable
data class PastParfaitResponse(
    @SerialName("parfaitId")
    val parfaitId: Long,
    @SerialName("date")
    val date: String,
    @SerialName("status")
    val status: String,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    @SerialName("imageCount")
    val imageCount: Int,
)
```

- [ ] **Step 4: VO에 필드를 더한다**

`PastCanvasVO.kt` 전체를 이렇게 바꾼다.

```kotlin
package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

/**
 * 과거 캔버스 목록의 한 칸.
 *
 * thumbnailUrl 은 서버가 항상 null 을 넣는다 — 필드만 있고 채우는 코드가 없다
 * (`api/parfait.md`). 빼지도 지어내지도 않고 그대로 노출한다.
 *
 * 서버 응답 필드명은 imageCount 인데 domain 은 제품 언어를 쓰므로 toppingCount 다.
 */
data class PastCanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val status: CanvasStatus,
    val thumbnailUrl: String?,
    val toppingCount: Int,
) {
    /**
     * 캔버스만 열어 보고 토핑은 안 올린 날. 달력이 점을 찍으면 안 되는 날이다.
     *
     * [status] 의 EMPTY 와 뜻이 다르니 갈아타지 말 것 — 그쪽은 "0건으로 마감된 날"이라
     * 아직 진행 중인 오늘 캔버스가 빠진다. 점 기준을 토핑 개수로 두는 것은 C-201 정책이다.
     */
    val isEmpty: Boolean get() = toppingCount == 0
}
```

- [ ] **Step 5: 매퍼가 값을 채우게 한다**

`data/source/parfait/mapper/VOMapper.kt`의 `toPastCanvasVOList`에 한 줄을 더한다.

```kotlin
internal fun PastParfaitsResponse.toPastCanvasVOList(): List<PastCanvasVO> = parfaits.map {
    PastCanvasVO(
        parfaitId = ParfaitId(it.parfaitId),
        date = LocalDate.parse(it.date),
        status = it.status.toCanvasStatus(),
        thumbnailUrl = it.thumbnailUrl,
        toppingCount = it.imageCount,
    )
}
```

`toCanvasStatus()`는 같은 파일에 이미 있는 private 확장이다(미지 값은 `CanvasStatus.UNKNOWN`). 새로 만들지 않는다.

- [ ] **Step 6: `PastCanvasVO`를 부르는 기존 테스트를 고친다**

생성자 인자가 늘어 아래 **두 파일**이 컴파일되지 않는다. 각 호출에 `status = CanvasStatus.CLOSED`를 넣고(과거 목록의 일반적인 상태다) 그 파일에 `CanvasStatus` import가 없으면 더한다. 값이 판정에 쓰이는 곳은 없다.

- `feature/groups/canvas/impl/src/test/kotlin/.../CanvasMainViewModelTest.kt` — **생성자 호출이 세 곳이다.** 하나만 고치면 나머지 둘이 깨진다.
- `domain/src/test/java/.../GetParfaitHistoriesUseCaseTest.kt` — `private fun canvas(date: LocalDate)` 헬퍼 한 곳.

⚠️ **`GetParfaitYearsUseCaseTest.kt`와 `GetTodayParfaitFlowUseCaseTest.kt`는 건드리지 마라.** 두 파일은 페이크의 반환 타입 `Result<List<PastCanvasVO>>`만 쓰고 생성자를 부르지 않는다 — `CanvasStatus` import를 넣으면 미사용 import가 되어 Step 8의 `ktlintCheck`가 깨진다.

각 파일에서 `PastCanvasVO(`를 찾아 인자를 더한다. 예:

```kotlin
    private fun canvas(date: LocalDate) = PastCanvasVO(
        parfaitId = ParfaitId(1L),
        date = date,
        status = CanvasStatus.CLOSED,
        thumbnailUrl = null,
        toppingCount = 0,
    )
```

- [ ] **Step 7: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :data:testDebugUnitTest :domain:test :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 8: ktlint를 돌린다**

Run: `./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 9: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/PastParfaitsResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/canvas/PastCanvasVO.kt \
        data/src/test domain/src/test feature/groups/canvas/impl/src/test
git commit -m "feat: 과거 캔버스 목록 응답의 status 를 도메인까지 받는다"
```

---

### Task 2: 일괄 수정 wire 계약과 DataSource

`:data` 표면을 통째로 갈아 끼운다. 단건 경로를 이 Task에서 걷어내므로 이 Task가 끝나면 `:domain`·`:feature`가 컴파일되지 않는다 — Task 3·4가 이어서 닫는다. 그래서 **Task 2·3·4는 한 덩이로 진행하고 Task 4 끝에서 전체 빌드가 다시 초록이 된다.**

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImagesRequest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImagesResponse.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingTransformUpdate.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageRequest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 산출물은 쓰지 않는다.
- Produces:
  - `ToppingTransformUpdate(parfaitImageId: ParfaitImageId, positionX: Double? = null, positionY: Double? = null, positionZ: Int? = null, scale: Double? = null, rotation: Double? = null)` — `domain/model/topping/`
  - `ParfaitImageRemoteDataSource.updateToppings(groupId: GroupId, parfaitId: ParfaitId, updates: List<ToppingTransformUpdate>): Result<List<UpdatedToppingVO>>`
  - `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(groupId: Long, parfaitId: Long, request: UpdateParfaitImagesRequest): ApiResponse<UpdateParfaitImagesResponse>`
  - 삭제되는 심볼: `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId`, `ParfaitImageRemoteDataSource.updateTopping`, `UpdateParfaitImageRequest`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ParfaitImageRemoteDataSourceImplTest.kt`에서 기존 `updateTopping_` 케이스 다섯(`serviceReturnsSuccess_returnsMergedTransform`·`omittedFieldsAreSentAsNull`·`unwrapsIdsForPathVariables`·`notOwned_returnsBusinessException`·`ioException_returnsNetworkException`)과 헬퍼 `updateSuccess()`를 아래로 **대체**한다. `updateToppingBorder_` 케이스와 `placeTopping_` 케이스는 건드리지 않는다.

import를 바꾼다 — `UpdateParfaitImageRequest`를 빼고 `UpdateParfaitImagesRequest`·`UpdateParfaitImagesResponse`·`ToppingTransformUpdate`를 더한다.

```kotlin
    private fun updateResponse(
        parfaitImageId: Long,
        positionX: Double = 200.0,
        positionY: Double = 400.0,
        positionZ: Int = 1,
        scale: Double = 1.5,
        rotation: Double = 45.0,
    ) = UpdateParfaitImageResponse(
        parfaitImageId = parfaitImageId,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    )

    private fun updateSuccess(vararg images: UpdateParfaitImageResponse) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = UpdateParfaitImagesResponse(images = images.toList()),
    )

    @Test
    fun updateToppings_serviceReturnsSuccess_returnsMergedTransforms() = runTest {
        // Given 서버가 병합된 값 둘을 준다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns updateSuccess(
            updateResponse(parfaitImageId = 201L),
            updateResponse(parfaitImageId = 202L, positionX = 10.0, positionY = 20.0, positionZ = 2, scale = 1.0, rotation = 0.0),
        )

        // When 토핑 둘을 한 번에 수정
        val vos = dataSource
            .updateToppings(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(5L),
                updates = listOf(
                    ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0, positionY = 400.0, scale = 1.5, rotation = 45.0),
                    ToppingTransformUpdate(parfaitImageId = ParfaitImageId(202L), positionX = 10.0, positionY = 20.0),
                ),
            ).getOrThrow()

        // Then 응답은 부분이 아니라 전체 transform 이고 원소 순서를 유지한다
        assertEquals(listOf(ParfaitImageId(201L), ParfaitImageId(202L)), vos.map { it.parfaitImageId })
        assertEquals(
            ToppingTransform(positionX = 200.0, positionY = 400.0, positionZ = 1, scale = 1.5, rotation = 45.0),
            vos.first().transform,
        )
    }

    @Test
    fun updateToppings_buildsOneItemPerUpdate() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitImagesRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns updateSuccess(updateResponse(parfaitImageId = 201L), updateResponse(parfaitImageId = 202L))

        // When 토핑 둘을 수정
        dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(
                ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0),
                ToppingTransformUpdate(parfaitImageId = ParfaitImageId(202L), scale = 2.0),
            ),
        )

        // Then 요청 한 번에 항목 둘이 실린다 — 호출도 한 번뿐이다
        assertEquals(listOf(201L, 202L), request.captured.items.map { it.parfaitImageId })
        coVerify(exactly = 1) {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        }
    }

    @Test
    fun updateToppings_omittedFieldsAreSentAsNull() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitImagesRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns updateSuccess(updateResponse(parfaitImageId = 201L))

        // When z-order 만 바꾼다
        dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionZ = 3)),
        )

        // Then 지정한 필드만 값이 있고 나머지는 null 이다 (서버가 null 을 미변경으로 읽는다)
        val item = request.captured.items.single()
        assertEquals(3, item.positionZ)
        assertNull(item.positionX)
        assertNull(item.positionY)
        assertNull(item.scale)
        assertNull(item.rotation)
    }

    @Test
    fun updateToppings_unwrapsIdsForPathVariablesAndItems() = runTest {
        // Given 성공 응답
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns updateSuccess(updateResponse(parfaitImageId = 201L))

        // When value class 로 감싼 id 로 수정
        dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then 경로 변수 둘에 raw Long 이 들어간다 — parfaitImageId 는 경로가 아니라 바디로 간다
        coVerify(exactly = 1) {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(1L, 5L, any())
        }
    }

    @Test
    fun updateToppings_notOwned_returnsBusinessException() = runTest {
        // Given 항목 중 하나가 본인 배치가 아니다 (그룹 미참여도 같은 코드로 온다.
        // HTTP status 축은 여기서 잡지 않는다 - 실제 서버의 403 은 Retrofit 이 HttpException 을
        // 던지는 별도 경로를 탄다)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_OWNED",
            message = "본인이 배치한 토핑이 아닙니다",
            data = null,
        )

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then Business 예외로 실패한다 — 서버가 전부 롤백했으므로 부분 성공이 없다
        assertTrue(result.isFailure)
        assertEquals(
            "PARFAIT_IMAGE_NOT_OWNED",
            assertIs<ApiException.Business>(result.exceptionOrNull()).code,
        )
    }

    @Test
    fun updateToppings_alreadyClosed_returnsBusinessException() = runTest {
        // Given 마감된 캔버스다 — 일괄은 마감 검사가 항목별 소유권보다 앞이라 단건과 다른 코드가 온다
        // (`api/parfait-image.md` 검사 순서)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_ALREADY_CLOSED",
            message = "이미 마감된 파르페입니다",
            data = null,
        )

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        assertEquals(
            "PARFAIT_ALREADY_CLOSED",
            assertIs<ApiException.Business>(result.exceptionOrNull()).code,
        )
    }

    @Test
    fun updateToppings_successButNullData_returnsEmptyBodyException() = runTest {
        // Given 성공인데 본문이 비었다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(success = true, code = "SUCCESS", message = "성공", data = null)

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        assertEquals("SUCCESS", assertIs<ApiException.EmptyBody>(result.exceptionOrNull()).code)
    }

    @Test
    fun updateToppings_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } throws IOException("connection reset")

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*ParfaitImageRemoteDataSourceImplTest*'`
Expected: 컴파일 실패 — `patchGroupsByGroupIdParfaitsByParfaitIdImages`·`UpdateParfaitImagesRequest`·`ToppingTransformUpdate`·`updateToppings`가 없다.

- [ ] **Step 3: 도메인 항목 타입을 만든다**

Create `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingTransformUpdate.kt`:

```kotlin
package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 배치된 토핑 하나의 부분 수정. null 인 축은 그대로 두라는 뜻이다.
 *
 * [ToppingTransform] 과 달리 축이 전부 널 허용이라 "안 바꾼다"를 표현할 수 있다.
 */
data class ToppingTransformUpdate(
    val parfaitImageId: ParfaitImageId,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
)
```

- [ ] **Step 4: wire DTO를 만들고 단건 요청 DTO를 지운다**

Create `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImagesRequest.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 여러 토핑의 위치·크기·각도를 한 요청으로 수정한다.
 *
 * 부분 성공이 없다 — 서버가 트랜잭션 하나로 묶어 항목 하나가 걸리면 전부 롤백하고,
 * 어느 항목이 걸렸는지는 응답에 없다(`api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImagesRequest(
    @SerialName("items")
    val items: List<UpdateParfaitImageItemRequest>,
)

/**
 * null 인 필드는 서버가 기존 값을 유지한다(ParfaitImage.update 의 ?: 병합).
 *
 * @RemoteJson Json 이 explicitNulls 기본값(true)을 쓰므로 안 바꾸는 필드도 "positionX": null 로
 * 실려 나간다. 실제 결정 인자는 encodeDefaults 다 — @RemoteJson 은 encodeDefaults = true 라서,
 * 다섯 축이 전부 `= null` 기본값이어도 프로퍼티가 생략되지 않고 그대로 실린다
 * (`JsonModule.provideRemoteJson`). encodeDefaults = false 였다면 explicitNulls 와 무관하게
 * 기본값과 같은 필드는 통째로 생략됐을 것이다. 서버에게 키 부재와 명시적 null 이 같은 뜻이라
 * 동작은 정확하다. 이 API 하나 때문에 전역 Json 설정을 바꾸지 않는다.
 */
@Serializable
data class UpdateParfaitImageItemRequest(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("positionX")
    val positionX: Double? = null,
    @SerialName("positionY")
    val positionY: Double? = null,
    @SerialName("positionZ")
    val positionZ: Int? = null,
    @SerialName("scale")
    val scale: Double? = null,
    @SerialName("rotation")
    val rotation: Double? = null,
)
```

Create `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImagesResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 원소는 단건 수정 응답과 같은 타입이다 — 서버가 그 DTO 를 그대로 재사용한다.
 *
 * 순서를 계약이 보장하지 않으므로 소비 측은 parfaitImageId 로 맞춘다(`api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImagesResponse(
    @SerialName("images")
    val images: List<UpdateParfaitImageResponse>,
)
```

Delete `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageRequest.kt`:

```bash
git rm data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageRequest.kt
```

`UpdateParfaitImageResponse.kt`는 **지우지 않는다** — 일괄 응답의 원소로 계속 쓴다.

같은 디렉토리의 `UpdateParfaitImageBorderRequest.kt` KDoc이 "위치 수정(`UpdateParfaitImageRequest`)과 달리…"로 사라질 타입을 이름으로 가리킨다. 그 한 자리를 `UpdateParfaitImagesRequest`로 고친다(컴파일에는 영향이 없지만 낡은 채로 남는다).

- [ ] **Step 5: Service를 바꾼다**

`ParfaitImageService.kt`에서 단건 PATCH 메서드를 지우고 컬렉션 PATCH를 넣는다. import도 `UpdateParfaitImageRequest` → `UpdateParfaitImagesRequest`, `UpdateParfaitImageResponse` → `UpdateParfaitImagesResponse`로 바꾼다.

```kotlin
    /**
     * POST 와 경로가 같고 메서드만 다르다 — 서버에서도 두 컨트롤러가 이 URL 을 나눠 갖는다.
     */
    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImages(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Body request: UpdateParfaitImagesRequest,
    ): ApiResponse<UpdateParfaitImagesResponse>
```

- [ ] **Step 6: 매퍼를 바꾼다**

`data/source/parfaitimage/mapper/VOMapper.kt`에 둘을 더한다. 기존 `toUpdatedToppingVO()`는 그대로 두고 원소 매핑으로 재사용한다.

```kotlin
internal fun List<ToppingTransformUpdate>.toUpdateRequest(): UpdateParfaitImagesRequest =
    UpdateParfaitImagesRequest(
        items = map {
            UpdateParfaitImageItemRequest(
                parfaitImageId = it.parfaitImageId.value,
                positionX = it.positionX,
                positionY = it.positionY,
                positionZ = it.positionZ,
                scale = it.scale,
                rotation = it.rotation,
            )
        },
    )

internal fun UpdateParfaitImagesResponse.toUpdatedToppingVOList(): List<UpdatedToppingVO> =
    images.map { it.toUpdatedToppingVO() }
```

import를 더한다 — `UpdateParfaitImageItemRequest`, `UpdateParfaitImagesRequest`, `UpdateParfaitImagesResponse`, `com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate`.

- [ ] **Step 7: DataSource 인터페이스를 바꾼다**

`ParfaitImageRemoteDataSource.kt`의 `updateTopping`을 아래로 대체한다. `ParfaitImageId` import는 `updateToppingBorder`·`deleteTopping`이 계속 쓰므로 남긴다.

```kotlin
    /**
     * 배치된 토핑 여럿의 위치·크기·각도를 한 요청으로 부분 수정한다. 넘기지 않은 축은 서버가 유지한다.
     *
     * 부분 성공이 없다 — 항목 하나가 걸리면 전부 롤백되고 어느 항목이었는지는 응답에 없다.
     * 테두리는 이 API 로 바꿀 수 없다(요청에 필드가 없다) — [updateToppingBorder] 가 맡는다.
     *
     * 그룹에 참여하지 않았을 때도 본인 배치가 아닐 때와 같은 코드(PARFAIT_IMAGE_NOT_OWNED,
     * 403)가 온다. 그룹 멤버라면 마감된 캔버스가 항목별 소유권보다 먼저 걸려 409
     * PARFAIT_ALREADY_CLOSED 다 — 그 둘의 순서가 단건 수정과 반대다(`api/parfait-image.md`).
     */
    suspend fun updateToppings(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>>
```

import를 더한다 — `com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate`.

- [ ] **Step 8: DataSource 구현을 바꾼다**

`ParfaitImageRemoteDataSourceImpl.kt`의 `updateTopping` 구현을 아래로 대체하고, import에서 `UpdateParfaitImageRequest`·`toUpdatedToppingVO`를 빼고 `toUpdateRequest`·`toUpdatedToppingVOList`·`ToppingTransformUpdate`를 더한다.

```kotlin
    override suspend fun updateToppings(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                request = updates.toUpdateRequest(),
            )
        },
        transform = { it.toUpdatedToppingVOList() },
    )
```

- [ ] **Step 9: 검증하지 않고 넘어간다 (의도된 것)**

⚠️ **이 Task는 자체 검증이 없다.** `ToppingRepositoryImpl.update`가 `:data` main에서 방금 지운 `updateTopping`을 직접 부르므로 `:data:compileDebugKotlin`이 **확정적으로 실패한다.** 여기서 gradle을 돌리지 마라 — 실패가 정상이고, 고치려 들면 Task 3의 일을 앞당겨 하게 된다.

`:data`가 다시 컴파일되는 것은 **Task 3 Step 5**에서다. 이 Task의 산출물은 그때 함께 검증된다.

- [ ] **Step 10: 커밋한다**

```bash
git add data/src domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingTransformUpdate.kt
git commit -m "feat: 토핑 일괄 수정 wire 계약과 DataSource 를 신설하고 단건 경로를 걷는다"
```

---

### Task 3: Repository·UseCase 교체

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/UpdateToppingsUseCase.kt`
- Delete: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/UpdateToppingUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImplTest.kt`

⚠️ **`ToppingRepositoryImplTest`를 함께 고쳐야 `:data` 테스트 소스셋이 컴파일된다.** 그 파일의 `update_` 케이스 둘이 `repository.update(...)`와 `parfaitImageRemoteDataSource.updateTopping(any() 8개)`를 직접 부른다. **Task 2가 끝난 시점부터 `:data:testDebugUnitTest`가 막혀 있고, 이 Task의 Step 3이 그것을 푼다.**

**Interfaces:**
- Consumes: Task 2의 `ToppingTransformUpdate`, `ParfaitImageRemoteDataSource.updateToppings`.
- Produces:
  - `ToppingRepository.updateAll(groupId: GroupId, parfaitId: ParfaitId, updates: List<ToppingTransformUpdate>): Result<List<UpdatedToppingVO>>`
  - `UpdateToppingsUseCase.invoke(groupId: GroupId, parfaitId: ParfaitId, updates: List<ToppingTransformUpdate>): Result<List<UpdatedToppingVO>>` — Task 4의 ViewModel이 이 시그니처로 부른다.
  - 삭제되는 심볼: `ToppingRepository.update`, `UpdateToppingUseCase`

- [ ] **Step 1: Repository 인터페이스를 바꾼다**

`ToppingRepository.kt`의 `update`를 아래로 대체한다. `ParfaitImageId` import는 `delete`·`updateBorder`가 계속 쓰므로 남기고, `ToppingTransformUpdate` import를 더한다.

```kotlin
    /**
     * 배치된 토핑 여럿의 위치·크기·각도를 한 요청으로 부분 수정한다. 넘기지 않은 축은 서버가 유지한다.
     *
     * 부분 성공이 없다 — 하나라도 걸리면 전부 롤백되고 실패한 항목이 무엇인지는 알 수 없다.
     * [updates] 가 비면 요청을 보내지 않고 빈 목록으로 성공한다.
     */
    suspend fun updateAll(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>>
```

- [ ] **Step 2: Repository 구현을 바꾼다**

`ToppingRepositoryImpl.kt`의 `update` 구현을 아래로 대체하고 import를 조정한다(`ToppingTransformUpdate` 추가).

```kotlin
    override suspend fun updateAll(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>> {
        // 서버가 빈 items 를 200 으로 받아 주지만 보낼 이유가 없다(`api/parfait-image.md`)
        if (updates.isEmpty()) return Result.success(emptyList())

        return parfaitImageRemoteDataSource
            .updateToppings(groupId = groupId, parfaitId = parfaitId, updates = updates)
            .mapErrorToAppError()
    }
```

- [ ] **Step 3: `ToppingRepositoryImplTest`의 단건 케이스를 일괄 케이스로 바꾼다**

`update_dataSourceSucceeds_returnsSameValue`와 `update_dataSourceFailsWithBusiness_convertsToAppErrorServer` 둘을 아래 셋으로 대체한다. `updateBorder_`·`place_`·`delete_` 케이스는 건드리지 않는다.

**빈 목록 단축은 이 파일이 잠그는 유일한 자리다** — 매퍼 단독 테스트를 만들지 않는 규약상 Repository 테스트가 그 판단의 서식지다.

```kotlin
    @Test
    fun updateAll_dataSourceSucceeds_returnsSameValue() = runTest {
        // Given 서버가 수정된 배치 둘을 준다
        val updated = listOf(
            UpdatedToppingVO(parfaitImageId = PARFAIT_IMAGE_ID, transform = transform),
            UpdatedToppingVO(parfaitImageId = ParfaitImageId(43L), transform = transform),
        )
        val updates = listOf(
            ToppingTransformUpdate(
                parfaitImageId = PARFAIT_IMAGE_ID,
                positionX = transform.positionX,
                positionY = transform.positionY,
                scale = transform.scale,
                rotation = transform.rotation,
            ),
            ToppingTransformUpdate(parfaitImageId = ParfaitImageId(43L), scale = 2.0),
        )
        coEvery {
            parfaitImageRemoteDataSource.updateToppings(GROUP_ID, PARFAIT_ID, updates)
        } returns Result.success(updated)

        // When 둘을 한 번에 수정한다
        val result = repository.updateAll(groupId = GROUP_ID, parfaitId = PARFAIT_ID, updates = updates)

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(updated, result.getOrThrow())
    }

    @Test
    fun updateAll_emptyUpdates_shortCircuitsWithoutCallingDataSource() = runTest {
        // Given 보낼 것이 없다

        // When 빈 목록으로 부른다
        val result = repository.updateAll(groupId = GROUP_ID, parfaitId = PARFAIT_ID, updates = emptyList())

        // Then 서버가 빈 items 를 200 으로 받아 주더라도 요청 자체를 안 만든다
        assertEquals(emptyList(), result.getOrThrow())
        coVerify(exactly = 0) { parfaitImageRemoteDataSource.updateToppings(any(), any(), any()) }
    }

    @Test
    fun updateAll_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 항목 중 하나가 본인이 배치한 토핑이 아니다 — 서버가 전부 롤백한다
        coEvery {
            parfaitImageRemoteDataSource.updateToppings(any(), any(), any())
        } returns Result.failure(
            ApiException.Business(
                code = "PARFAIT_IMAGE_NOT_OWNED",
                serverMessage = "본인이 배치한 토핑이 아닙니다",
                statusCode = 403,
                errorDetail = null,
            ),
        )

        // When 수정한다
        val result = repository.updateAll(
            groupId = GROUP_ID,
            parfaitId = PARFAIT_ID,
            updates = listOf(ToppingTransformUpdate(parfaitImageId = PARFAIT_IMAGE_ID, positionX = 100.0)),
        )

        // Then 코드와 상태 코드가 함께 살아 있다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_IMAGE_NOT_OWNED", error.code)
        assertEquals(403, error.statusCode)
    }
```

import를 더한다 — `com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate`. `coVerify`·`assertIs`·`AppError`·`ApiException`·`UpdatedToppingVO`·`ParfaitImageId`는 이미 있다.

- [ ] **Step 4: UseCase를 교체한다**

Create `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/UpdateToppingsUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

class UpdateToppingsUseCase @Inject constructor(
    private val toppingRepository: ToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>> = toppingRepository.updateAll(
        groupId = groupId,
        parfaitId = parfaitId,
        updates = updates,
    )
}
```

```bash
git rm domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/UpdateToppingUseCase.kt
```

- [ ] **Step 5: `:data`·`:domain` 컴파일과 `:data` 테스트를 확인한다**

여기가 **Task 2와 Task 3을 함께 검증하는 첫 게이트**다. Task 2에서 미뤄 둔 것이 여기서 초록이 된다.

Run: `./gradlew :domain:compileKotlin :data:testDebugUnitTest`
Expected: PASS (`:feature:groups:canvas:impl`은 아직 깨져 있다 — Task 4가 닫는다)

- [ ] **Step 6: 커밋한다**

```bash
git add domain/src/main \
        data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImpl.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImplTest.kt
git commit -m "feat: 토핑 수정 Repository·UseCase 를 일괄 계약으로 바꾼다"
```

---

### Task 4: 확인 버튼을 축 단위 저장으로 바꾼다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasBGEditViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3의 `UpdateToppingsUseCase`, Task 2의 `ToppingTransformUpdate`.
- Produces: 없음(마지막 코드 Task다). `handleOnClickConfirm`의 계약은 안 바뀐다 — `updateDirtyToppings(): Set<Long>`이 실패한 토핑 id를 돌려주고 그 뒤 배경 저장·토스트·화면 닫기 판정이 지금 그대로다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`CanvasBGEditViewModelTest.kt`에서 다음을 바꾼다.

먼저 mock 선언과 생성자 인자:

```kotlin
    private val updateToppings: UpdateToppingsUseCase = mockk()
```

```kotlin
        updateToppingsUseCase = updateToppings,
```

import를 `UpdateToppingUseCase` → `UpdateToppingsUseCase`로 바꾸고 `com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate`를 더한다.

`updateTopping` 목을 참조하는 기존 케이스는 **여섯**이고 전부 처리해야 한다. 하나라도 남기면 미해결 참조로 `:feature:groups:canvas:impl` 테스트 소스셋 전체가 컴파일되지 않는다.

| 기존 케이스 | 처분 |
|---|---|
| `onClickConfirm_toppingMoved_updatesOnlyThatTopping` | 아래 새 케이스로 대체 |
| `onClickConfirm_toppingUpdateFails_keepsTheScreenAndTellsWhy` | 아래 새 케이스로 대체 |
| `onClickConfirm_toppingUpdateFails_keepsDirtyForRetry` | 아래 새 케이스로 대체 |
| `onClickConfirm_everythingSaved_clearsDirtyAndConfirms` | 아래 새 케이스로 대체 |
| `confirm_patchesOnlyDirtyToppings` | 아래 `onClickConfirm_multipleToppingsMoved_...`가 대신한다 — **삭제** |
| `onClickConfirm_noToppingChanges_doesNotCallUpdate` | 아래 `onClickConfirm_nothingDirty_sendsNoUpdateRequest`가 대신한다 — **삭제** |

`onClickConfirm_toppingBorderEdited_savesOnlyTheBorder`는 **남긴다** — 마지막 `coVerify` 한 줄만 아래에서 고친다.

```kotlin
    @Test
    fun onClickConfirm_toppingMoved_sendsOnlyThatToppingInOneRequest() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 옮긴다. 배경은 안 건드려 기본 팔레트 색 그대로다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        val topping = viewModel.selectMyTopping()
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = -0.05f))
        val moved = viewModel.state.value.toppings
            .first { it.parfaitImageId == topping.parfaitImageId }

        val updates = slot<List<ToppingTransformUpdate>>()
        coEvery {
            updateToppings(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), capture(updates))
        } returns Result.success(emptyList<UpdatedToppingVO>())

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 요청은 한 번이고 옮긴 토핑만 항목으로 실린다
        coVerify(exactly = 1) { updateToppings(any(), any(), any()) }
        assertEquals(listOf(ParfaitImageId(topping.parfaitImageId)), updates.captured.map { it.parfaitImageId })
        val item = updates.captured.single()
        assertEquals(moved.positionX.toDouble(), item.positionX)
        assertEquals(moved.positionY.toDouble(), item.positionY)
        assertEquals(moved.scale.toDouble(), item.scale)
        assertEquals(moved.rotationDegrees.toDouble(), item.rotation)
    }

    @Test
    fun onClickConfirm_toppingMoved_doesNotSendPositionZ() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 옮긴다 — 앱에 z 조작 경로가 없다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        viewModel.selectMyTopping()
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = 0f))

        val updates = slot<List<ToppingTransformUpdate>>()
        coEvery { updateToppings(any(), any(), capture(updates)) } returns Result.success(emptyList<UpdatedToppingVO>())

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 겹침 순서는 null 로 남아 서버 값이 유지된다
        assertNull(updates.captured.single().positionZ)
    }

    @Test
    fun onClickConfirm_multipleToppingsMoved_sendsOneRequestWithAllItems() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑 둘을 각각 옮긴다
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID), myTopping(SECOND_IMAGE_ID))
        stubBackgroundChange(
            background = CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()),
            result = Result.success(null),
        )
        val viewModel = viewModel()

        listOf(MY_IMAGE_ID, SECOND_IMAGE_ID).forEach { id ->
            viewModel.processIntent(
                CanvasBGEditIntent.OnClickTopping(
                    viewModel.state.value.toppings.first { it.parfaitImageId == id },
                ),
            )
            viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.2f, deltaY = 0f))
        }

        val updates = slot<List<ToppingTransformUpdate>>()
        coEvery { updateToppings(any(), any(), capture(updates)) } returns Result.success(emptyList<UpdatedToppingVO>())

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 토핑 수만큼 요청하지 않고 한 번에 보낸다
        coVerify(exactly = 1) { updateToppings(any(), any(), any()) }
        assertEquals(
            setOf(ParfaitImageId(MY_IMAGE_ID), ParfaitImageId(SECOND_IMAGE_ID)),
            updates.captured.map { it.parfaitImageId }.toSet(),
        )
    }

    @Test
    fun onClickConfirm_nothingDirty_sendsNoUpdateRequest() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑을 하나도 안 건드린다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 빈 요청조차 보내지 않는다
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_batchFails_keepsTheScreenAndTellsWhy() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 옮겼는데 저장은 실패한다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        viewModel.selectMyTopping()
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = 0f))
        coEvery { updateToppings(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        // When 확인 버튼을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 저장하지 못한 편집을 안은 채 화면을 닫지 않고, 실패만 알린다
            assertEquals(CanvasBGEditEffect.ShowError(CanvasBGEditError.TOPPING_SAVE_UNKNOWN), awaitItem())
            expectNoEvents()
        }
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun onClickConfirm_batchFails_keepsEveryTransformToppingDirty() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑 둘을 옮겼는데 일괄 저장이 실패한다
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID), myTopping(SECOND_IMAGE_ID))
        stubBackgroundChange(
            background = CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()),
            result = Result.success(null),
        )
        val viewModel = viewModel()

        listOf(MY_IMAGE_ID, SECOND_IMAGE_ID).forEach { id ->
            viewModel.processIntent(
                CanvasBGEditIntent.OnClickTopping(
                    viewModel.state.value.toppings.first { it.parfaitImageId == id },
                ),
            )
            viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.2f, deltaY = 0f))
        }
        coEvery { updateToppings(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 서버가 전부 롤백했으므로 보낸 토핑 전부를 대상으로 남긴다
        assertEquals(setOf(MY_IMAGE_ID, SECOND_IMAGE_ID), viewModel.state.value.dirtyToppingIds)
    }

    @Test
    fun onClickConfirm_everythingSaved_clearsDirtyAndConfirms() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 옮겼고 저장도 성공한다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        viewModel.selectMyTopping()
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = 0f))
        coEvery { updateToppings(any(), any(), any()) } returns Result.success(emptyList<UpdatedToppingVO>())

        // When 확인 버튼을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 화면을 넘긴다
            assertIs<CanvasBGEditEffect.ConfirmBackground>(awaitItem())
        }
        assertTrue(
            viewModel.state.value.dirtyToppingIds
                .isEmpty(),
        )
        assertEquals(false, viewModel.state.value.isLoading)
    }
```

`onClickConfirm_toppingBorderEdited_savesOnlyTheBorder`의 마지막 줄은 이렇게 바꾼다 — 테두리만 바뀐 토핑은 일괄에 실리면 안 된다.

```kotlin
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
```

`private fun TestScope.viewModel(...)` 헬퍼가 이미 상태 구독과 `advanceUntilIdle()`을 해 주므로 그 뒤에 `backgroundScope.launch { ... }`를 다시 넣지 않는다(기존 `confirm_patchesOnlyDirtyToppings`가 그렇게 하고 있었다 — 물려받지 마라).

`private fun updatedTopping(): UpdatedToppingVO = mockk()` 헬퍼는 더 이상 쓰이지 않으면 지운다. `assertNull`·`slot` import가 없으면 더한다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasBGEditViewModelTest*'`
Expected: 컴파일 실패 — `UpdateToppingsUseCase`·`updateToppingsUseCase` 생성자 인자가 없다.

- [ ] **Step 3: ViewModel 생성자를 바꾼다**

import를 `UpdateToppingUseCase` → `UpdateToppingsUseCase`로 바꾸고 `com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate`를 더한다. 생성자 파라미터도 바꾼다.

```kotlin
    private val updateToppingsUseCase: UpdateToppingsUseCase,
```

- [ ] **Step 4: 저장 흐름을 축 단위로 다시 짠다**

`updateDirtyToppings`와 `updateToppingIfChanged`를 아래 셋으로 대체한다. `handleOnClickConfirm`은 건드리지 않는다 — `updateDirtyToppings(): Set<Long>` 계약이 그대로다.

```kotlin
    /**
     * PATCH 대상은 지금 목록에 있으면서 손댄 토핑뿐이다. 대조를 dirty 안에서만 하는 것이 요점이다:
     * 목록 전체를 스냅샷과 견주면 갱신이 들여온 남의 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡힌다.
     *
     * 축으로 가르는 이유는 서버 API 가 갈라져 있어서다 — 변형은 한 요청에 접히지만
     * (`ToppingRepository.updateAll`) 테두리는 토핑마다 따로 나간다(`updateBorder`).
     *
     * @return 저장하지 못한 토핑의 id.
     */
    private suspend fun updateDirtyToppings(): Set<Long> = coroutineScope {
        val current = state.value
        val dirty = current.toppings.filter { it.parfaitImageId in current.dirtyToppingIds }

        val transformChanged = dirty.filter { it.hasTransformChange() }
        val borderChanged = dirty.filter { it.hasBorderChange() }

        val transformFailures = saveTransforms(transformChanged)
        val borderFailures = borderChanged
            .map { topping -> async { topping.parfaitImageId.takeIf { saveBorder(topping).not() } } }
            .awaitAll()
            .filterNotNull()

        transformFailures + borderFailures
    }

    /**
     * 일괄이라 부분 성공이 없다 — 하나가 걸리면 서버가 전부 롤백하고 실패한 항목이 무엇인지
     * 응답에 없다(`api/parfait-image.md`). 그래서 실패하면 보낸 토핑 전부를 대상으로 남긴다.
     *
     * 되풀이되는 실패가 섞이면 나머지 토핑까지 계속 막히는 것을 감수한 설계다 — 근거는
     * 스펙의 「주의」 절에 있다.
     *
     * @return 저장하지 못한 토핑의 id.
     */
    private suspend fun saveTransforms(toppings: List<CanvasToppingItem>): Set<Long> {
        if (toppings.isEmpty()) return emptySet()

        return updateToppingsUseCase(
            groupId = groupId,
            parfaitId = parfaitId,
            updates = toppings.map { it.toTransformUpdate() },
        ).fold(
            onSuccess = { emptySet() },
            onFailure = { throwable ->
                viewModelLogger.e(throwable) { "토핑 변형을 저장하지 못했다 - ${toppings.map { it.parfaitImageId }}" }
                toppings.mapTo(mutableSetOf()) { it.parfaitImageId }
            },
        )
    }

    /** @return 보냈고 성공했으면 `true`. */
    private suspend fun saveBorder(topping: CanvasToppingItem): Boolean = updateToppingBorderUseCase(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = ParfaitImageId(topping.parfaitImageId),
        border = topping.borderLayers.toToppingBorder(),
    ).onFailure { throwable ->
        viewModelLogger.e(throwable) { "토핑 테두리를 저장하지 못했다 - ${topping.parfaitImageId}" }
    }.isSuccess

    /** 스냅샷에 없으면 바뀐 것으로 본다 — 갱신이 들여온 토핑은 dirty 에 안 들어 여기 오지 않는다. */
    private fun CanvasToppingItem.hasTransformChange(): Boolean {
        val original = serverToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
        return positionX != original.positionX ||
            positionY != original.positionY ||
            scale != original.scale ||
            rotationDegrees != original.rotationDegrees
    }

    private fun CanvasToppingItem.hasBorderChange(): Boolean {
        val original = serverToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
        return borderLayers != original.borderLayers
    }

    /** 겹침 순서는 안 보낸다 — 앱에 z 조작 경로가 없어 서버 값을 그대로 둔다. */
    private fun CanvasToppingItem.toTransformUpdate(): ToppingTransformUpdate = ToppingTransformUpdate(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        positionX = positionX.toDouble(),
        positionY = positionY.toDouble(),
        scale = scale.toDouble(),
        rotation = rotationDegrees.toDouble(),
    )
```

기존 `updateToppingIfChanged`가 쓰던 `rotationDegrees` 비교는 원본 코드에 있던 조건을 그대로 옮긴 것이다. `toToppingBorder()`는 같은 파일에 이미 있는 확장이라 새로 만들지 않는다.

- [ ] **Step 5: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: 전체 유닛 테스트와 ktlint를 돌린다**

Run: `./gradlew :data:testDebugUnitTest :domain:test :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS

Run: `./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 7: 커밋한다**

```bash
git add feature/groups/canvas/impl/src
git commit -m "feat: 확인 버튼이 토핑 변형을 일괄 요청 한 번으로 저장한다"
```

---

### Task 5: 계약 문서와 미결 갱신

코드가 아니라 이 저장소(`team-yg-pesonal-agent`)의 문서를 고친다. **작업 디렉토리가 다르다** — TJYG-Android가 아니라 위키·parfait 저장소이고, 브랜치는 `docs/sync-server-api-de3a99a`다(이미 체크아웃돼 있고 서버 라운드 커밋 둘이 올라가 있다).

**Files:**
- Modify: `parfait/api/parfait-image.md`
- Modify: `parfait/api/parfait.md`
- Modify: `parfait/api/README.md`
- Modify: `parfait/api/conventions.md`
- Modify: `parfait/architecture/data-layer.md`
- Modify: `parfait/synthesis/open-questions.md`

**Interfaces:**
- Consumes: Task 1~4가 만든 심볼 이름들 — `UpdateToppingsUseCase`, `ToppingRepository.updateAll`, `ParfaitImageRemoteDataSource.updateToppings`, `ToppingTransformUpdate`, `PastCanvasVO.status`.
- Produces: 없음(마지막 Task다).

- [ ] **Step 1: `parfait-image.md`의 Android 열을 뒤집는다**

엔드포인트 표에서 단건 위치 PATCH 행의 Android 열을 `구현됨` → `표면 없음`(2026-08-31 걷어냄)으로, 일괄 PATCH 행을 `표면 없음`(2026-08-31 신설) → `구현됨`으로 바꾼다.

Android 매핑 절의 표에서도 단건 행의 심볼을 `**없음**(2026-08-31 걷어냄 — 소비처가 없어졌다)`으로, 일괄 행을 `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages` → `ParfaitImageRemoteDataSource.updateToppings(groupId, parfaitId, updates)`로 바꾼다.

- [ ] **Step 2: `parfait-image.md`의 낡은 실패 처분 서술을 고친다**

두 서술이 현재 `develop`에 대해 틀렸다. **다만 정정문이 서로 다르다 — 같은 문장을 양쪽에 붙이지 마라.**

**PR #336(위치 PATCH) 항목** — "⚠️ **그런데 실패가 화면에 닿지 않고, 확인은 그대로 성공한다**"를 걷고 이렇게 적는다. `CanvasBGEditViewModel.handleOnClickConfirm`이 실패한 토핑 id를 `dirtyToppingIds`에 남겨 다음 확인에서 재시도하고, `CanvasBGEditError.TOPPING_SAVE_UNKNOWN` 토스트를 내며 화면을 닫지 않는다.

**PR #335(삭제) 항목** — "⚠️ **그런데 실패가 화면에 닿지 않는다**"를 걷고 이렇게 적는다. `failToDeleteTopping`이 `CanvasBGEditError.TOPPING_DELETE_UNKNOWN` 토스트를 내고 로딩만 내린다. **dirty 집합과는 무관하다**(삭제는 dirty 축을 안 쓴다) — 위치 PATCH와 처분이 다르다.

같은 절의 PR #336 항목에 있는 **`confirmedToppings`는 실제 이름이 아니다** — `serverToppings`로 고친다.

그 절에 일괄 전환을 새 항목으로 더한다 — 변형은 요청 하나로 접혔고, 부분 성공이 사라져 실패 시 변형을 보낸 토핑 전부가 dirty로 남는다는 것, 테두리는 여전히 토핑마다 나간다는 것.

- [ ] **Step 3: `parfait.md`에 `status` 수용을 반영한다**

Android 매핑 절 끝의 "⚠️ **2026-08-31 서버 delta가 과거 목록에 공백 하나를 냈다**"로 시작하는 블록을 아래로 대체한다.

```markdown
✅ **2026-08-31 서버 delta가 낸 공백을 같은 날 닫았다** — 과거 목록 원소의 `status`를 앱 DTO
`PastParfaitResponse`와 `PastCanvasVO`가 받는다. 매핑은 `today`·상세가 이미 쓰던
`toCanvasStatus()` 재사용이라 미지 값 폴백(`CanvasStatus.UNKNOWN`)도 그대로다.

⚠️ **달력 점 기준은 옮기지 않았다.** `CanvasMainViewModel.uploadedDates`는 계속
`PastCanvasVO.isEmpty`(토핑 개수)를 쓴다 — 위키 [[C-201-캘린더-정책-v0.1]]이 인디케이터를
"토핑 1개 이상 = True, 0개 = False"로 규정하고 그 판정이 정본과 일치한다. 서버 `EMPTY`는
"0건으로 마감된 날"이라 뜻이 좁아, 옮기면 진행 중인 오늘의 빈 캔버스에 점이 찍힌다. 두 값이
같지 않다는 것을 `PastCanvasVO.isEmpty` KDoc이 담는다. **읽는 화면은 아직 0건이다**
→ [open-questions](../../synthesis/open-questions.md) OQ-P-333.
```

`## 미결`의 `OQ-P-333` 줄은 이렇게 바꾼다.

```markdown
- 과거 목록 원소의 `status`를 VO까지 받았으나 읽는 화면이 0건이다(달력 점은 개수 축을 그대로 쓴다)
  → [open-questions](../../synthesis/open-questions.md) OQ-P-333
```

- [ ] **Step 4: `README.md`와 `conventions.md`의 표면 셈을 고친다**

`README.md`:
- `parfait-image.md` 행의 Android 열 — 일괄 수정이 결선됐고 단건이 표면 없음이 됐다.
- `parfait.md` 행의 "다만 2026-08-31 과거 목록에 붙은 `status`를 앱 DTO가 안 받는다" 단서를 걷는다.
- 총계 문단의 Android 표면 셈을 **27/28 유지**로 적는다 — 일괄이 채워지고 단건이 비어 값이 그대로다. 공백 1의 정체가 바뀐 것(일괄 → 단건)을 적는다.
- `http/` 커버는 여전히 25/28이다(요청 모음에 일괄 요청이 없다).

`conventions.md`의 2026-08-31 블록도 같은 취지로 고친다.

- [ ] **Step 5: `architecture/data-layer.md`를 갱신한다**

Repository 표의 `ToppingRepository` 행이 지워지는 시그니처를 철자 그대로 싣고 있다.

- `**`update(groupId, parfaitId, parfaitImageId, positionX?, positionY?, positionZ?, scale?, rotation?): Result<UpdatedToppingVO>`**(#336)`를 `**`updateAll(groupId, parfaitId, updates): Result<List<UpdatedToppingVO>>`**`로 바꾼다.
- 소비 UseCase 열의 `UpdateToppingUseCase`를 `UpdateToppingsUseCase`로 바꾼다.
- frontmatter `related_code`에 `UpdateToppingBorderUseCase`는 있고 단건은 없으므로 손댈 것이 없다. `ToppingTransformUpdate`를 더할지는 그 목록의 기준(도메인 모델을 다 싣지는 않는다)에 맞춰 판단한다.

`architecture/state-management.md`는 저장 흐름을 적지 않으므로 손대지 않는다.

- [ ] **Step 6: 미결을 갱신한다**

`open-questions.md`:
- **OQ-P-334**를 "부분 해소"로 바꾼다 — ① 옮겨 탔고 ②③④는 남는다(실패 항목 미식별·검사 순서 차이·`items` 상한 없음). 상태 줄에 "지금 그 실패는 로그 한 줄로 접히고 있어(OQ-P-275)"라는 **틀린 전제를 걷고**, 지금 처분(실패 id 유지 + `TOPPING_SAVE_UNKNOWN` 토스트)으로 바꾼다. **새 항목 하나를 더한다** — 되풀이되는 실패가 섞이면 그 화면에서 위치 저장이 통째로 막힌다는 것과, 폴백 없이 가기로 한 근거(선택이 `isMine`으로 막혀 발생 조건이 좁다).
- **OQ-P-333**을 "부분 해소"로 바꾼다 — `status`를 VO까지 받았고, 달력 점 기준을 개수로 유지한 결정과 그 근거(위키 정본)를 적는다. 남는 것은 `status`의 화면 소비처가 0건이라는 사실뿐이다.
- **신규 1건**: 단건 위치 PATCH가 서버에 살아 있는데 앱 표면이 사라졌다는 것. 소비처가 생기면 되살려야 하고, 그때 검사 순서 차이(403 vs 409)를 다시 봐야 한다. `<!-- oq-next: -->` 값을 하나 올린다.

- [ ] **Step 7: 스펙·계획 문서의 상태를 갱신한다**

`parfait/specs/2026-08-31-topping-batch-update-and-past-canvas-status.md`의 frontmatter `status`를 `draft` → `implemented`로 바꾸고, `parfait/specs/README.md`·`parfait/plans/README.md`의 해당 행에 as-built 한 줄을 더한다(어느 브랜치에 들어갔는지, 계획과 달라진 점이 있으면 그것).

- [ ] **Step 8: 커밋한다**

```bash
git add parfait/
git commit -m "docs: 토핑 일괄 수정 전환을 계약 문서와 미결에 반영한다"
```

---

## 검증 요약

| 항목 | 명령 |
|---|---|
| `:data` 유닛 테스트 | `./gradlew :data:testDebugUnitTest` |
| `:domain` 유닛 테스트 | `./gradlew :domain:test` |
| 캔버스 feature 유닛 테스트 | `./gradlew :feature:groups:canvas:impl:testDebugUnitTest` |
| 정적 검사 | `./gradlew ktlintCheck` |

실기기 검증은 이 계획의 범위 밖이다.

**`http/parfait-image.http`에 일괄 요청을 넣지 않는다**(이 라운드 결정). 그 모음은 손으로 쏴서 계약을
확인하는 자리인데, 이번 라운드에는 실서버 요청 계획 자체가 없어 넣어도 아무도 돌리지 않는다. 커버가
25/28로 벌어진 채 남는 것을 [api/README.md](../../../api/README.md)가 기록한다 — 채우는 것은 실서버 검증을
하는 라운드의 일이다.
