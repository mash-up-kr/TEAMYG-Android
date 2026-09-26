# 그룹 목록·상세 인메모리 SSoT Implementation Plan

> ✅ **완료·develop 머지(PR #307 `refactor/#294-group-data-using-ssot` → `8ca3329a`, 2026-08-20)** —
> 7 Task 전량이 develop에 있다. 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> 계획이 쓰인 브랜치(`feature/#294-group-ssot`)는 PR #299가 닫히면서 SSoT 몫만
> `refactor/#294-group-data-using-ssot`로 갈렸고, 그 브랜치가 develop `c36cad49` 위로 리베이스된 뒤
> 머지됐다. 머지 커밋에 충돌 해소 편집은 0건이라 브랜치 팁이 그대로 develop 사실이다.
> 리베이스가 Global Constraints 하나를 넓혔다 — **세션 정리를 부르는 경로가 셋이 됐다**(#306이
> 들여온 `WithdrawUseCase`가 `LogoutUseCase`에 정리를 위임한다). 유닛 490 → 511건.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 그룹 목록과 그룹 상세를 `:data`의 인메모리 저장소 한 벌에 두고, 세 화면(G-001 목록·C-001 캔버스·S-101 설정)이 그것을 `Flow`로 구독하게 만든다.

**Architecture:** `GroupLocalDataSource`(`@Singleton` + `MutableStateFlow`, IO 없음)를 신설하고 `ParfaitGroupRepositoryImpl`이 원격·로컬을 조율한다. 조회 UseCase는 구독용(`Flow`)과 갱신용(`suspend`)으로 갈린다. 서버 조회는 화면이 명시적으로 부를 때만 나가고, 생성·참여·닉네임 변경·나가기·신고 성공은 저장소가 캐시에 반영한다.

**Tech Stack:** Kotlin, Coroutines/Flow, Hilt, JUnit4 + kotlin-test + MockK + Turbine

**Spec:** [`parfait/specs/2026-08-17-group-ssot.md`](../../specs/archive/2026-08-17-group-ssot.md)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 계획을 쓸 당시 브랜치는 `feature/#294-group-ssot`이었고 `feature/#288-group-list-refresh` 위에 리베이스돼 있었다. 실제로 PR이 열린 브랜치는 `refactor/#294-group-data-using-ssot`(PR #307)이고 지금은 `develop` 위로 리베이스돼 있다. 이 문서(`parfait/`)가 있는 저장소가 아니다.
- **커밋하지 않는다.** 각 Task의 마지막 단계는 `git status`로 변경 파일을 확인하고 보고하는 것으로 갈음한다. 사용자가 명시적으로 요청할 때만 커밋한다.
- **`Enter` 인텐트는 이미 있다.** `GroupListViewModel`·`CanvasMainViewModel` 모두 `LifecycleResumeEffect`가 화면 진입마다 `Enter`를 보낸다(#288). 새로 만들지 말고 그 안의 조회 호출만 갈아 끼운다.
- **매퍼 단독 테스트를 만들지 않는다.** 변환 검증은 DataSource·Repository 테스트 케이스로 덮는다.
- 테스트 실행: `./gradlew :<module>:testDebugUnitTest --console=plain`. 컴파일 확인은 `./gradlew :app:assembleDebug --console=plain`.
- 린트: `./gradlew :<module>:ktlintCheck --console=plain`.
- 주석·문서는 한국어. 기존 파일의 KDoc 밀도와 어투를 따른다.

---

## File Structure

**신규 (`:data`)**
- `data/src/main/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSource.kt` — 캐시 인터페이스
- `data/src/main/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSourceImpl.kt` — `MutableStateFlow` 두 개
- `data/src/test/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSourceImplTest.kt`

**신규 (`:domain`)**
- `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/GetMyGroupsFlowUseCase.kt`
- `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/RefreshMyGroupsUseCase.kt`
- `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/RefreshGroupDetailUseCase.kt`
- `domain/src/test/java/com/teamyg/parfait/domain/usecase/group/GetMyGroupsFlowUseCaseTest.kt`

**수정**
- `domain/.../repository/group/ParfaitGroupRepository.kt` — 읽기 `Flow` 둘 + `refresh` 둘 + `clearGroups`, 기존 `getMyGroups`·`getGroupDetail` 제거
- `data/.../repository/group/ParfaitGroupRepositoryImpl.kt` — 로컬 주입 + write-through
- `data/.../di/LocalDataSourceModule.kt` — 바인딩 한 줄
- `domain/.../usecase/group/GetGroupDetailUseCase.kt` — `Flow` + `combine`
- `domain/.../usecase/group/GetMyGroupsUseCase.kt` — 삭제
- `domain/.../usecase/auth/LogoutUseCase.kt` — 그룹 캐시 정리 추가
- `data/.../network/TokenAuthenticator.kt` — 강제 로그아웃 시 그룹 캐시 정리
- `feature/groups/list/impl/.../GroupListViewModel.kt`, `GroupListScreen.kt`
- `feature/groups/setting/impl/.../GroupSettingViewModel.kt`
- `feature/groups/canvas/impl/.../CanvasMainViewModel.kt`
- 대응 테스트 5종(`ParfaitGroupRepositoryImplTest`·`GetGroupDetailUseCaseTest`·`GroupListViewModelTest`·`GroupSettingViewModelTest`·`CanvasMainViewModelTest`)

---

### Task 1: `GroupLocalDataSource` — 인메모리 캐시

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `MyParfaitGroupVO`, `ParfaitGroupDetailVO`, `GroupId` (모두 `:domain`에 이미 있음)
- Produces: `GroupLocalDataSource` — `val myGroups: StateFlow<List<MyParfaitGroupVO>?>`, `fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?>`, `fun saveMyGroups(groups: List<MyParfaitGroupVO>)`, `fun saveGroupDetail(detail: ParfaitGroupDetailVO)`, `fun removeGroup(groupId: GroupId)`, `fun clear()`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/source/group/local/GroupLocalDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.group.local

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupLocalDataSourceImplTest {
    private val dataSource = GroupLocalDataSourceImpl()

    @Test
    fun myGroups_beforeAnySave_isNull() {
        // Given/When 아무것도 저장하지 않았다
        // Then 미조회는 null 이다 — 0건과 구분돼야 빈 화면과 로딩이 갈린다
        assertNull(dataSource.myGroups.value)
    }

    @Test
    fun saveMyGroups_withEmptyList_isEmptyNotNull() {
        // Given/When 서버가 그룹 0건을 줬다
        dataSource.saveMyGroups(emptyList())

        // Then 미조회가 아니라 0건이다
        assertEquals(emptyList(), dataSource.myGroups.value)
    }

    @Test
    fun removeGroup_dropsFromListAndDetail() = runTest {
        // Given 목록과 상세가 모두 캐시에 있다
        dataSource.saveMyGroups(listOf(GROUP_A, GROUP_B))
        dataSource.saveGroupDetail(DETAIL_A)

        // When 그룹 A 를 지운다
        dataSource.removeGroup(GROUP_ID_A)

        // Then 목록에서도 상세에서도 사라진다 — 한쪽만 지우면 나간 그룹의 상세가 남는다
        assertEquals(listOf(GROUP_B), dataSource.myGroups.value)
        dataSource.groupDetail(GROUP_ID_A).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun groupDetail_otherGroupSaved_doesNotReemit() = runTest {
        // Given A 의 상세를 구독하고 있다
        dataSource.saveGroupDetail(DETAIL_A)

        dataSource.groupDetail(GROUP_ID_A).test {
            assertEquals(DETAIL_A, awaitItem())

            // When 다른 그룹의 상세가 저장된다
            dataSource.saveGroupDetail(DETAIL_B)

            // Then A 구독자는 흔들리지 않는다
            expectNoEvents()
        }
    }

    @Test
    fun clear_resetsToUnloaded() = runTest {
        // Given 목록과 상세가 차 있다
        dataSource.saveMyGroups(listOf(GROUP_A))
        dataSource.saveGroupDetail(DETAIL_A)

        // When 세션이 끝난다
        dataSource.clear()

        // Then 미조회로 되돌아간다 — 계정이 바뀌어도 이전 그룹이 남지 않는다
        assertNull(dataSource.myGroups.value)
        dataSource.groupDetail(GROUP_ID_A).test {
            assertNull(awaitItem())
        }
    }

    private companion object {
        val GROUP_ID_A = GroupId(1L)
        val GROUP_ID_B = GroupId(2L)

        val GROUP_A = MyParfaitGroupVO(
            groupId = GROUP_ID_A,
            groupName = GroupName("아메리카노"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
        )
        val GROUP_B = MyParfaitGroupVO(
            groupId = GROUP_ID_B,
            groupName = GroupName("라떼"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
        )

        val DETAIL_A = ParfaitGroupDetailVO(
            groupId = GROUP_ID_A,
            groupNickname = GroupNickname("모카"),
            inviteCode = InviteCode("ABC123"),
            members = listOf(
                ParfaitGroupMemberVO(memberId = MemberId(10L), groupNickname = GroupNickname("모카")),
            ),
        )
        val DETAIL_B = DETAIL_A.copy(groupId = GROUP_ID_B)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*GroupLocalDataSourceImplTest*' --console=plain`
Expected: 컴파일 실패 — `Unresolved reference: GroupLocalDataSourceImpl`

- [ ] **Step 3: 인터페이스를 만든다**

`GroupLocalDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.group.local

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 그룹 목록·상세의 인메모리 SSoT. 프로세스와 수명을 같이 하고 디스크에 남기지 않는다
 * (ADR-0023) — 그래서 모든 함수가 suspend 가 아니다.
 */
interface GroupLocalDataSource {
    /** `null` 은 **아직 한 번도 받지 못했다**는 뜻이다. 빈 목록(그룹 0건)과 구분한다 */
    val myGroups: StateFlow<List<MyParfaitGroupVO>?>

    /** 그 그룹의 상세. 캐시에 없으면 `null` 을 낸다 */
    fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?>

    fun saveMyGroups(groups: List<MyParfaitGroupVO>)

    fun saveGroupDetail(detail: ParfaitGroupDetailVO)

    /** 나간 그룹을 목록과 상세 **양쪽에서** 지운다 */
    fun removeGroup(groupId: GroupId)

    /** 세션이 끝났을 때. 미조회 상태로 되돌린다 */
    fun clear()
}
```

- [ ] **Step 4: 구현을 만든다**

`GroupLocalDataSourceImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.group.local

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupLocalDataSourceImpl @Inject constructor() : GroupLocalDataSource {
    private val _myGroups = MutableStateFlow<List<MyParfaitGroupVO>?>(null)
    override val myGroups: StateFlow<List<MyParfaitGroupVO>?> = _myGroups.asStateFlow()

    private val details = MutableStateFlow<Map<GroupId, ParfaitGroupDetailVO>>(emptyMap())

    /**
     * 맵 전체가 아니라 한 그룹으로 좁혀서 낸다 — [distinctUntilChanged] 가 없으면 남의 그룹
     * 상세가 저장될 때마다 이 구독자까지 재방출된다.
     */
    override fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?> = details
        .map { it[groupId] }
        .distinctUntilChanged()

    override fun saveMyGroups(groups: List<MyParfaitGroupVO>) {
        _myGroups.value = groups
    }

    override fun saveGroupDetail(detail: ParfaitGroupDetailVO) {
        details.update { it + (detail.groupId to detail) }
    }

    override fun removeGroup(groupId: GroupId) {
        _myGroups.update { current -> current?.filterNot { it.groupId == groupId } }
        details.update { it - groupId }
    }

    override fun clear() {
        _myGroups.value = null
        details.value = emptyMap()
    }
}
```

- [ ] **Step 5: DI 바인딩을 더한다**

`LocalDataSourceModule.kt`에 import 두 줄과 아래 바인딩을 더한다(기존 바인딩들과 같은 형식):

```kotlin
    @Binds
    @Singleton
    fun bindGroupLocalDataSource(groupLocalDataSourceImpl: GroupLocalDataSourceImpl): GroupLocalDataSource
```

- [ ] **Step 6: 테스트가 통과하는지 본다**

Run: `./gradlew :data:testDebugUnitTest --tests '*GroupLocalDataSourceImplTest*' --console=plain`
Expected: PASS (5건)

- [ ] **Step 7: 변경 파일을 확인해 보고한다**

Run: `git status --short`
Expected: 신규 3파일 + `LocalDataSourceModule.kt` 수정. **커밋하지 않는다.**

---

### Task 2: Repository — 읽기 `Flow`·갱신·write-through

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/group/ParfaitGroupRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/group/ParfaitGroupRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/group/ParfaitGroupRepositoryImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 `GroupLocalDataSource`
- Produces: `ParfaitGroupRepository` — `val myGroups: Flow<List<MyParfaitGroupVO>?>`, `fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?>`, `suspend fun refreshMyGroups(): Result<Unit>`, `suspend fun refreshGroupDetail(groupId: GroupId): Result<Unit>`, `fun clearGroups()`. 기존 `createGroup`·`joinGroup`·`changeMyNickname`·`leaveGroup`·`reportGroup`·`previewJoin`은 시그니처 그대로 유지하고, `getMyGroups()`·`getGroupDetail()`은 **삭제**한다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ParfaitGroupRepositoryImplTest.kt`의 기존 케이스 중 `getMyGroups`·`getGroupDetail`을 직접 부르는 것들을 아래로 갈아 끼우고, 새 케이스를 더한다. 로컬은 mock 이 아니라 **실물**을 넣는다 — 캐시 상태가 이 테스트의 관찰 대상이다.

```kotlin
    private val remoteDataSource: ParfaitGroupRemoteDataSource = mockk()
    private val localDataSource = GroupLocalDataSourceImpl()
    private val repository = ParfaitGroupRepositoryImpl(remoteDataSource, localDataSource)

    @Test
    fun refreshMyGroups_succeeds_fillsCache() = runTest {
        // Given 서버가 그룹 하나를 준다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))

        // When 갱신한다
        val result = repository.refreshMyGroups()

        // Then 성공이고 캐시가 찼다
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_A), repository.myGroups.first())
    }

    @Test
    fun refreshMyGroups_fails_keepsCacheAndMapsError() = runTest {
        // Given 캐시에 이미 목록이 있고, 다음 조회가 실패한다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))
        repository.refreshMyGroups()
        coEvery { remoteDataSource.getMyGroups() } returns
            Result.failure(ApiException.Network(cause = IOException("no network")))

        // When 다시 갱신한다
        val result = repository.refreshMyGroups()

        // Then 실패는 AppError 로 나오고 캐시는 그대로다
        assertIs<AppError.Network>(result.exceptionOrNull())
        assertEquals(listOf(GROUP_A), repository.myGroups.first())
    }

    @Test
    fun createGroup_succeeds_refreshesList() = runTest {
        // Given 생성이 성공하고 목록 재조회도 성공한다
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns Result.success(CREATED)
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))

        // When 그룹을 만든다
        val result = repository.createGroup(GroupName("아메리카노"), GroupNickname("모카"), 12)

        // Then 생성 결과가 돌아오고 목록 캐시가 갱신된다
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_A), repository.myGroups.first())
        coVerify(exactly = 1) { remoteDataSource.getMyGroups() }
    }

    @Test
    fun createGroup_refreshFails_stillSucceeds() = runTest {
        // Given 생성은 성공했는데 뒤이은 목록 재조회가 실패한다
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns Result.success(CREATED)
        coEvery { remoteDataSource.getMyGroups() } returns
            Result.failure(ApiException.Network(cause = IOException("no network")))

        // When 그룹을 만든다
        val result = repository.createGroup(GroupName("아메리카노"), GroupNickname("모카"), 12)

        // Then 이미 성공한 생성을 뒷정리 실패로 되돌리지 않는다
        assertTrue(result.isSuccess)
    }

    @Test
    fun changeMyNickname_succeeds_refreshesDetail() = runTest {
        // Given 닉네임 변경과 상세 재조회가 모두 성공한다
        coEvery { remoteDataSource.changeMyNickname(any(), any()) } returns Result.success(NICKNAME_VO)
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns Result.success(DETAIL_A)

        // When 닉네임을 바꾼다
        val result = repository.changeMyNickname(GROUP_ID_A, GroupNickname("모카"))

        // Then 상세 캐시가 서버 값으로 채워진다
        assertTrue(result.isSuccess)
        assertEquals(DETAIL_A, repository.groupDetail(GROUP_ID_A).first())
    }

    @Test
    fun leaveGroup_succeeds_removesFromCache() = runTest {
        // Given 목록·상세가 캐시에 있고 나가기가 성공한다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A, GROUP_B))
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns Result.success(DETAIL_A)
        repository.refreshMyGroups()
        repository.refreshGroupDetail(GROUP_ID_A)
        coEvery { remoteDataSource.leaveGroup(GROUP_ID_A) } returns Result.success(GROUP_ID_A)

        // When 그룹에서 나간다
        val result = repository.leaveGroup(GROUP_ID_A)

        // Then 목록에서 빠지고 상세도 폐기된다. 재조회는 하지 않는다(403 뿐이다)
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_B), repository.myGroups.first())
        assertNull(repository.groupDetail(GROUP_ID_A).first())
        coVerify(exactly = 1) { remoteDataSource.getMyGroups() }
    }

    @Test
    fun clearGroups_emptiesCache() = runTest {
        // Given 캐시가 차 있다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))
        repository.refreshMyGroups()

        // When 세션이 끝난다
        repository.clearGroups()

        // Then 미조회로 되돌아간다
        assertNull(repository.myGroups.first())
    }
```

상수는 기존 테스트의 companion 을 재사용하고, 없는 것은 아래 값으로 더한다(`GROUP_A`·`GROUP_B`·`DETAIL_A`는 Task 1 테스트와 같은 값).

```kotlin
        val NICKNAME_VO = GroupNicknameVO(groupId = GROUP_ID_A, groupNickname = GroupNickname("모카"))
        val CREATED = CreatedGroupVO(
            groupId = GROUP_ID_A,
            groupName = GroupName("아메리카노"),
            inviteCode = InviteCode("ABC123"),
            memberLimit = 12,
        )
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*ParfaitGroupRepositoryImplTest*' --console=plain`
Expected: 컴파일 실패 — `Unresolved reference: refreshMyGroups`

- [ ] **Step 3: 도메인 인터페이스를 고친다**

`ParfaitGroupRepository.kt`에서 `getMyGroups()`·`getGroupDetail()`을 지우고 아래를 넣는다(나머지 함수는 그대로).

```kotlin
    /**
     * 캐시된 내 그룹 목록. `null` 은 아직 한 번도 받지 못했다는 뜻이고 빈 목록과 다르다.
     * 값을 새로 받으려면 [refreshMyGroups] 를 부른다 — 이 흐름은 스스로 조회하지 않는다.
     */
    val myGroups: Flow<List<MyParfaitGroupVO>?>

    /** 캐시된 그룹 상세. 받아 둔 것이 없으면 `null` */
    fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?>

    /**
     * 서버에서 목록을 다시 받아 캐시를 덮는다. 실패하면 캐시는 그대로다.
     *
     * 값을 돌려주지 않는 이유: 읽는 길이 [myGroups] 하나여야 한다. 반환값으로도 줄 수 있으면
     * 화면이 그것을 쓰기 시작하고 캐시는 두 번째 출처가 된다.
     */
    suspend fun refreshMyGroups(): Result<Unit>

    /** 서버에서 그 그룹 상세를 다시 받아 캐시를 덮는다. 실패하면 캐시는 그대로다 */
    suspend fun refreshGroupDetail(groupId: GroupId): Result<Unit>

    /** 세션이 끝났을 때 캐시를 비운다. 인메모리라 suspend 가 아니다 */
    fun clearGroups()
```

`getGroupDetail`에 달려 있던 "서버 응답 확장 대기" TODO 는 `refreshGroupDetail` KDoc 으로 옮긴다.

- [ ] **Step 4: 구현을 고친다**

`ParfaitGroupRepositoryImpl.kt`:

```kotlin
class ParfaitGroupRepositoryImpl @Inject constructor(
    private val parfaitGroupRemoteDataSource: ParfaitGroupRemoteDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
) : ParfaitGroupRepository {
    override val myGroups: Flow<List<MyParfaitGroupVO>?> = groupLocalDataSource.myGroups

    override fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?> =
        groupLocalDataSource.groupDetail(groupId)

    override suspend fun refreshMyGroups(): Result<Unit> = parfaitGroupRemoteDataSource
        .getMyGroups()
        .onSuccess(groupLocalDataSource::saveMyGroups)
        .map { }
        .mapErrorToAppError()

    override suspend fun refreshGroupDetail(groupId: GroupId): Result<Unit> = parfaitGroupRemoteDataSource
        .getGroupDetail(groupId)
        .onSuccess(groupLocalDataSource::saveGroupDetail)
        .map { }
        .mapErrorToAppError()

    override fun clearGroups() = groupLocalDataSource.clear()

    /**
     * 생성 응답에는 최근 사진·시각이 없어 [MyParfaitGroupVO] 를 세울 수 없다 — 빈 값으로 끼워
     * 넣으면 활동순 정렬이 어긋나므로 목록을 다시 받는다. 그 재조회가 실패해도 생성은 성공이다.
     */
    override suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO> = parfaitGroupRemoteDataSource
        .createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = memberLimit,
        ).onSuccess { refreshMyGroups() }
        .mapErrorToAppError()

    /** 참여 응답도 목록 항목을 세울 수 없어 다시 받는다([createGroup] 과 같은 이유) */
    override suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO> = parfaitGroupRemoteDataSource
        .joinGroup(inviteCode)
        .onSuccess { refreshMyGroups() }
        .mapErrorToAppError()

    /**
     * 응답은 바뀐 닉네임뿐이라 캐시의 멤버 목록에서 "나"를 짚으려면 계정 id 가 필요하다.
     * 그것을 알려면 계정 저장소를 끌어와야 해서, 대신 상세를 서버에서 다시 받는다.
     */
    override suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO> = parfaitGroupRemoteDataSource
        .changeMyNickname(
            groupId = groupId,
            groupNickname = groupNickname,
        ).onSuccess { refreshGroupDetail(groupId) }
        .mapErrorToAppError()

    /** 나간 그룹은 이후 모든 호출이 403 이라 재조회하지 않고 캐시에서 지운다 */
    override suspend fun leaveGroup(groupId: GroupId): Result<GroupId> = parfaitGroupRemoteDataSource
        .leaveGroup(groupId)
        .onSuccess { groupLocalDataSource.removeGroup(groupId) }
        .mapErrorToAppError()

    /** 신고는 서버가 같은 트랜잭션에서 탈퇴로 잇는다 — [leaveGroup] 과 같이 캐시에서 지운다 */
    override suspend fun reportGroup(
        groupId: GroupId,
        reason: String,
    ): Result<ReportedGroupVO> = parfaitGroupRemoteDataSource
        .reportGroup(
            groupId = groupId,
            reason = reason,
        ).onSuccess { groupLocalDataSource.removeGroup(groupId) }
        .mapErrorToAppError()
```

`previewJoin`은 캐시와 무관하므로 기존 코드 그대로 둔다.

- [ ] **Step 5: 테스트가 통과하는지 본다**

Run: `./gradlew :data:testDebugUnitTest --tests '*ParfaitGroupRepositoryImplTest*' --console=plain`
Expected: PASS. `:domain`·`:feature` 는 아직 옛 API 를 부르므로 이 시점에 전체 빌드는 깨져 있다 — Task 3 에서 고친다.

- [ ] **Step 6: 변경 파일을 확인해 보고한다**

Run: `git status --short`

---

### Task 3: UseCase — 구독과 갱신 분리

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/GetMyGroupsFlowUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/RefreshMyGroupsUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/RefreshGroupDetailUseCase.kt`
- Delete: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/GetMyGroupsUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/GetGroupDetailUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/group/GetGroupDetailUseCaseTest.kt` (기존 파일 교체)

**Interfaces:**
- Consumes: Task 2의 `ParfaitGroupRepository`
- Produces:
  - `GetMyGroupsFlowUseCase` — `operator fun invoke(): Flow<List<MyParfaitGroupVO>?>`
  - `RefreshMyGroupsUseCase` — `suspend operator fun invoke(): Result<Unit>`
  - `RefreshGroupDetailUseCase` — `suspend operator fun invoke(groupId: GroupId): Result<Unit>`
  - `GetGroupDetailUseCase` — `operator fun invoke(groupId: GroupId): Flow<GroupDetailVO?>`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`GetGroupDetailUseCaseTest.kt`를 통째로 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.domain.usecase.group

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetGroupDetailUseCaseTest {
    private val repository: ParfaitGroupRepository = mockk()

    @Test
    fun invoke_listCacheEmpty_stillEmitsDetailWithBlankName() = runTest {
        // Given 상세는 있는데 목록 캐시가 비어 있다
        every { repository.groupDetail(GROUP_ID) } returns flowOf(DETAIL)
        every { repository.myGroups } returns flowOf(null)

        // When 상세를 구독한다
        GetGroupDetailUseCase(repository).invoke(GROUP_ID).test {
            // Then 이름만 비고 나머지는 보인다 — 이름 한 줄 때문에 멤버·초대코드를 가리지 않는다
            val detail = awaitItem()
            assertEquals(GroupName(""), detail?.groupName)
            assertEquals(DETAIL.inviteCode, detail?.inviteCode)
            assertEquals(DETAIL.members, detail?.members)
            awaitComplete()
        }
    }

    @Test
    fun invoke_listCacheArrivesLater_emitsNameOnly() = runTest {
        // Given 상세는 이미 있고 목록은 나중에 채워진다
        val groups = MutableStateFlow<List<MyParfaitGroupVO>?>(null)
        every { repository.groupDetail(GROUP_ID) } returns flowOf(DETAIL)
        every { repository.myGroups } returns groups

        GetGroupDetailUseCase(repository).invoke(GROUP_ID).test {
            assertEquals(GroupName(""), awaitItem()?.groupName)

            // When 목록 캐시가 채워진다
            groups.value = listOf(GROUP)

            // Then 이름이 붙어 다시 나온다
            assertEquals(GroupName("아메리카노"), awaitItem()?.groupName)
        }
    }

    @Test
    fun invoke_detailNotCached_emitsNull() = runTest {
        // Given 상세를 아직 받지 못했다
        every { repository.groupDetail(GROUP_ID) } returns flowOf(null)
        every { repository.myGroups } returns flowOf(listOf(GROUP))

        // When 구독한다
        GetGroupDetailUseCase(repository).invoke(GROUP_ID).test {
            // Then 보여 줄 것이 없다
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    private companion object {
        val GROUP_ID = GroupId(1L)

        val GROUP = MyParfaitGroupVO(
            groupId = GROUP_ID,
            groupName = GroupName("아메리카노"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
        )

        val DETAIL = ParfaitGroupDetailVO(
            groupId = GROUP_ID,
            groupNickname = GroupNickname("모카"),
            inviteCode = InviteCode("ABC123"),
            members = listOf(
                ParfaitGroupMemberVO(memberId = MemberId(10L), groupNickname = GroupNickname("모카")),
            ),
        )
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :domain:test --tests '*GetGroupDetailUseCaseTest*' --console=plain`
Expected: 컴파일 실패 — `invoke` 가 `Flow` 를 돌려주지 않는다

- [ ] **Step 3: UseCase 셋을 만들고 하나를 고친다**

`GetMyGroupsFlowUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 캐시된 내 그룹 목록을 구독한다. 조회는 [RefreshMyGroupsUseCase] 가 따로 부른다 */
class GetMyGroupsFlowUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    operator fun invoke(): Flow<List<MyParfaitGroupVO>?> = parfaitGroupRepository.myGroups
}
```

`RefreshMyGroupsUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class RefreshMyGroupsUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(): Result<Unit> = parfaitGroupRepository.refreshMyGroups()
}
```

`RefreshGroupDetailUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class RefreshGroupDetailUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(groupId: GroupId): Result<Unit> =
        parfaitGroupRepository.refreshGroupDetail(groupId)
}
```

`GetGroupDetailUseCase.kt` 전체 교체:

```kotlin
package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupDetailVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetGroupDetailUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    /**
     * TODO(서버 응답 확장 대기): 그룹 상세에 그룹명이 없어 목록 캐시에서 이름만 집어 붙인다.
     *  서버가 상세에 groupName 을 실어 주면 이 [combine] 을 걷어낸다.
     *
     * 이름을 못 찾아도 상세를 접지 않는다 — 이름 한 줄 때문에 멤버·초대코드까지 못 보여 주는
     * 것보다, 이름을 비우고 나머지를 띄우는 편이 낫다.
     */
    operator fun invoke(groupId: GroupId): Flow<GroupDetailVO?> = combine(
        parfaitGroupRepository.groupDetail(groupId),
        parfaitGroupRepository.myGroups,
    ) { detail, groups ->
        detail?.let {
            GroupDetailVO(
                groupId = it.groupId,
                groupName = groups
                    ?.firstOrNull { group -> group.groupId == groupId }
                    ?.groupName
                    ?: GroupName(""),
                myNickname = it.groupNickname,
                inviteCode = it.inviteCode,
                members = it.members,
            )
        }
    }
}
```

`GetMyGroupsUseCase.kt`는 삭제한다(대체는 `GetMyGroupsFlowUseCase` + `RefreshMyGroupsUseCase`).

- [ ] **Step 4: 테스트가 통과하는지 본다**

Run: `./gradlew :domain:test --tests '*GetGroupDetailUseCaseTest*' --console=plain`
Expected: PASS (3건)

- [ ] **Step 5: 변경 파일을 확인해 보고한다**

Run: `git status --short`
Expected: 신규 3 + 수정 1 + 삭제 1 + 테스트 1. `:feature` 는 아직 옛 UseCase 를 부르므로 앱 빌드는 Task 4~6 이 끝나야 통과한다.

---

### Task 4: G-001 목록 화면 이관

**Files:**
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListViewModel.kt`
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListScreen.kt`
- Test: `feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3의 `GetMyGroupsFlowUseCase`·`RefreshMyGroupsUseCase`
- Produces: `GroupListUiState.groupList: List<MyParfaitGroupVO>?`(기본 `null`)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

기존 `GroupListViewModelTest`에서 `getMyGroups` mock 을 아래로 바꾸고, 케이스 셋을 더한다.

```kotlin
    private val getMyGroupsFlow: GetMyGroupsFlowUseCase = mockk()
    private val refreshMyGroups: RefreshMyGroupsUseCase = mockk()

    private fun viewModel() = GroupListViewModel(getMyGroupsFlow, refreshMyGroups)

    @Test
    fun enter_cacheEmits_showsGroups() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 그룹 둘이 있고 갱신은 성공한다
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When 화면이 앞에 선다
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 캐시가 준 순서 그대로 들고 있다
        assertEquals(GROUPS, viewModel.state.value.groupList)
        assertFalse(viewModel.state.value.isError)
        coVerify(exactly = 1) { refreshMyGroups() }
    }

    @Test
    fun enter_refreshFailsWithEmptyCache_showsErrorScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비었고 갱신이 실패한다
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 앞에 선다
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 보여 줄 것이 없으므로 에러 화면으로 넘어간다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun enter_refreshFailsWithCachedGroups_keepsList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 목록이 있는데 갱신이 실패한다
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 앞에 선다
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 낡아도 목록을 남긴다 — 뒤로 온 것만으로 화면이 사라지지 않는다
        assertFalse(viewModel.state.value.isError)
        assertEquals(GROUPS, viewModel.state.value.groupList)
    }

    @Test
    fun cacheUpdatesAfterEnter_reflectsWithoutNewRefresh() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시를 구독 중이다
        val cache = MutableStateFlow<List<MyParfaitGroupVO>?>(emptyList())
        every { getMyGroupsFlow() } returns cache
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // When 다른 화면이 그룹을 만들어 캐시가 바뀐다
        cache.value = GROUPS
        advanceUntilIdle()

        // Then 이 화면이 다시 조회하지 않고도 반영한다
        assertEquals(GROUPS, viewModel.state.value.groupList)
        coVerify(exactly = 1) { refreshMyGroups() }
    }
```

기존의 pull-to-refresh·중복 가드 케이스는 `getMyGroups()` 를 `refreshMyGroups()` 로만 바꿔 살린다.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --console=plain`
Expected: 컴파일 실패 — 생성자 인자가 맞지 않는다

- [ ] **Step 3: ViewModel 을 고친다**

`GroupListViewModel.kt`:

```kotlin
data class GroupListUiState(
    /** `null` 은 아직 한 번도 받지 못했다는 뜻. 0건과 구분한다 */
    val groupList: List<MyParfaitGroupVO>? = null,
    // ... 나머지 필드는 그대로
) : UiState
```

```kotlin
@HiltViewModel
class GroupListViewModel
@Inject
constructor(
    private val getMyGroupsFlow: GetMyGroupsFlowUseCase,
    private val refreshMyGroups: RefreshMyGroupsUseCase,
) : BaseViewModel<GroupListUiState, GroupListIntent, GroupListSideEffect>(
    initialState = GroupListUiState(),
) {
    init {
        observeGroups()
    }

    /**
     * 표시는 캐시가 맡는다 — 다른 화면이 그룹을 만들거나 나가면 이 화면이 다시 조회하지 않아도
     * 그 자리에서 반영된다.
     */
    private fun observeGroups() {
        viewModelScope.launch {
            getMyGroupsFlow().collect { groups -> updateState { copy(groupList = groups) } }
        }
    }
    // ... processIntent 는 그대로. Enter 가 updateToday() + loadGroups(isRefresh = false) 를 부른다
```

`loadGroups`는 갱신만 부르도록 바꾼다:

```kotlin
    private fun loadGroups(isRefresh: Boolean) {
        if (isRefresh) {
            updateState { copy(isRefreshing = true) }
        }

        launch(key = KEY_LOAD_GROUPS) {
            try {
                refreshMyGroups()
                    .onSuccess { updateState { copy(isError = false) } }
                    .onFailure(::handleLoadFailure)
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }
```

실패 판정은 nullable 을 반영한다(#288 규칙 유지):

```kotlin
        updateState { copy(isError = groupList.isNullOrEmpty()) }
```

`viewModelScope`·`launch`·`MutableStateFlow` import 를 정리한다.

- [ ] **Step 4: 화면 호출부를 고친다**

`GroupListScreen.kt`에서 `groupList = uiState.groupList` 를 아래로 바꾼다.

```kotlin
                            // 0건 온보딩 툴팁이 결선되면 여기서 null(미조회)과 0건을 갈라 분기한다
                            groupList = uiState.groupList.orEmpty(),
```

- [ ] **Step 5: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --console=plain`
Expected: PASS

- [ ] **Step 6: 변경 파일을 확인해 보고한다**

Run: `git status --short`

---

### Task 5: S-101 그룹 설정 화면 이관

**Files:**
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModel.kt`
- Test: `feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3의 `GetGroupDetailUseCase`(Flow)·`RefreshGroupDetailUseCase`
- Produces: 없음(화면 종단)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

기존 테스트의 `getGroupDetail` 스텁을 Flow 로 바꾸고 아래 셋을 더한다.

```kotlin
    @Test
    fun init_cacheHasDetail_showsWithoutLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 상세가 이미 있다
        every { getGroupDetail(GROUP_ID) } returns flowOf(DETAIL)
        coEvery { refreshGroupDetail(GROUP_ID) } returns Result.success(Unit)
        every { getMyAccountFlow() } returns flowOf(ACCOUNT)

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 첫 값이 이미 있으므로 화면을 덮지 않는다
        val state = viewModel.state.value
        assertFalse(state.isLoadingDetail)
        assertEquals(DETAIL.groupName, state.groupName)
        assertEquals(DETAIL.myNickname, state.myNickname)
    }

    @Test
    fun init_refreshFails_showsErrorEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비었고 갱신이 실패한다
        every { getGroupDetail(GROUP_ID) } returns flowOf(null)
        coEvery { refreshGroupDetail(GROUP_ID) } returns Result.failure(AppError.Network(cause = null))
        every { getMyAccountFlow() } returns flowOf(ACCOUNT)

        val viewModel = viewModel()

        // When/Then 실패가 토스트로 나간다
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertIs<GroupSettingSideEffect.ShowError>(effect)
        }
    }

    @Test
    fun confirmNickname_succeeds_takesNewValueFromCache() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 변경 후 새 닉네임을 낸다
        val detail = MutableStateFlow<GroupDetailVO?>(DETAIL)
        every { getGroupDetail(GROUP_ID) } returns detail
        coEvery { refreshGroupDetail(GROUP_ID) } returns Result.success(Unit)
        every { getMyAccountFlow() } returns flowOf(ACCOUNT)
        coEvery { changeGroupNickname(GROUP_ID, GroupNickname("라떼")) } coAnswers {
            detail.value = DETAIL.copy(myNickname = GroupNickname("라떼"))
            Result.success(GroupNicknameVO(groupId = GROUP_ID, groupNickname = GroupNickname("라떼")))
        }

        val viewModel = viewModel()
        advanceUntilIdle()

        // When 닉네임을 바꾼다
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("라떼"))
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)
        advanceUntilIdle()

        // Then 화면 값은 캐시 방출로 바뀐다 — ViewModel 이 손으로 고치지 않는다
        val state = viewModel.state.value
        assertEquals(GroupNickname("라떼"), state.myNickname)
        assertEquals("라떼", state.nicknameInput)
        assertFalse(state.isEditing)
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest --console=plain`
Expected: 컴파일 실패 — `refreshGroupDetail` 이 없다

- [ ] **Step 3: ViewModel 을 고친다**

생성자에 `RefreshGroupDetailUseCase`를 더하고 `init`·조회부를 바꾼다.

```kotlin
    init {
        viewModelLogger.i { "GroupSettingViewModel::init" }
        observeGroupDetail()
        loadGroupDetail()
    }

    /**
     * 상세는 캐시가 낸다 — 닉네임을 바꾸면 저장소가 서버에서 다시 받아 캐시에 넣고, 그 값이
     * 여기로 내려온다. 화면이 자기 상태를 손으로 고치지 않는다.
     */
    private fun observeGroupDetail() {
        viewModelScope.launch {
            val myMemberId = getMyAccountFlow().first()?.memberId

            getGroupDetail(groupId).collect { detail ->
                if (detail == null) return@collect
                updateState { withDetail(detail, myMemberId).copy(isLoadingDetail = false) }
            }
        }
    }

    private fun loadGroupDetail() {
        launch(key = KEY_LOAD_GROUP_DETAIL) {
            try {
                refreshGroupDetail(groupId).onFailure { throwable ->
                    viewModelLogger.e(throwable) { "그룹 상세를 불러오지 못했다 - groupId: ${groupId.value}" }
                    postSideEffect(GroupSettingSideEffect.ShowError(throwable.toGroupSettingError()))
                }
            } finally {
                // 예외·취소 어느 경로로 빠져나가도 로딩이 걸린 채 남지 않게 한다
                updateState { copy(isLoadingDetail = false) }
            }
        }
    }
```

`handleConfirmNickname`의 성공 처리에서 `withMyNickname` 호출을 걷어낸다:

```kotlin
                changeGroupNickname(groupId = groupId, groupNickname = nickname)
                    .onSuccess { updateState { copy(isEditing = false, nicknameError = null) } }
                    .onFailure { throwable ->
                        viewModelLogger.e(throwable) { "그룹 닉네임을 바꾸지 못했다 - groupId: ${groupId.value}" }
                        postSideEffect(GroupSettingSideEffect.ShowError(throwable.toGroupSettingError()))
                    }
```

`withMyNickname` 함수는 지운다 — 캐시 방출이 그 일을 한다. `withDetail`은 그대로 둔다(편집 중이면 입력값을 보존하는 규칙이 여전히 필요하다).

- [ ] **Step 4: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest --console=plain`
Expected: PASS

- [ ] **Step 5: 변경 파일을 확인해 보고한다**

Run: `git status --short`

---

### Task 6: C-001 캔버스 그룹명 결선

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3의 `GetMyGroupsFlowUseCase`·`RefreshMyGroupsUseCase`
- Produces: 없음(화면 종단)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`CanvasMainViewModelTest`(#288이 만든 파일)에 아래 둘을 더한다. 기존 mock 목록에 `getMyGroupsFlow`·`refreshMyGroups`를 더하고 `viewModel()` 헬퍼의 인자도 늘린다.

```kotlin
    @Test
    fun init_cacheHasGroup_showsGroupName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록 캐시에 이 그룹이 있다
        every { getMyGroupsFlow() } returns flowOf(listOf(GROUP))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 캐시의 이름이 화면에 온다
        assertEquals("아메리카노", viewModel.state.value.groupName)
        coVerify(exactly = 0) { refreshMyGroups() }
    }

    @Test
    fun init_cacheEmpty_refreshesListOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비어 있다(프로세스 재시작 후 캔버스로 복귀)
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 목록을 한 번 받아 온다. 실패해도 캔버스는 계속 그린다
        coVerify(exactly = 1) { refreshMyGroups() }
    }
```

`GROUP` 상수는 Task 1 테스트와 같은 값(`groupId = GroupId(GROUP_ID_VALUE)`, `groupName = GroupName("아메리카노")`)으로 더한다. `GROUP_ID_VALUE`는 기존 테스트가 `viewModel()`에 넘기는 `groupIdValue`와 같아야 한다.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --console=plain`
Expected: 컴파일 실패 — 생성자 인자가 맞지 않는다

- [ ] **Step 3: ViewModel 을 고친다**

생성자에 두 UseCase 를 더하고 `loadCanvasMainInfo()`를 바꾼다.

```kotlin
    /**
     * 캔버스 응답에는 그룹명이 없어 그룹 목록 캐시에서 가져온다. 캐시가 비어 있는 진입
     * (프로세스 재시작 후 캔버스로 복귀)에서만 목록을 한 번 받아 온다 — 이름 한 줄 때문에
     * 캔버스를 막지 않으므로 그 조회의 실패는 로그로만 남긴다.
     */
    private fun loadCanvasMainInfo() {
        viewModelScope.launch {
            getMyGroupsFlow().collect { groups ->
                if (groups == null) return@collect

                val groupName = groups.firstOrNull { it.groupId == groupId }?.groupName?.value.orEmpty()
                updateState { copy(groupName = groupName) }
            }
        }

        launch(key = LOAD_GROUP_NAME_KEY) {
            if (getMyGroupsFlow().first() != null) return@launch

            refreshMyGroups().onFailure { throwable ->
                viewModelLogger.e(throwable) { "그룹명을 불러오지 못했다 - groupId: ${groupId.value}" }
            }
        }
    }
```

`LOAD_GROUP_NAME_KEY` 상수를 기존 companion 에 더한다(`private const val LOAD_GROUP_NAME_KEY = "loadGroupName"`). `kotlinx.coroutines.flow.first` import 를 더한다.

- [ ] **Step 4: 테스트가 통과하는지 본다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --console=plain`
Expected: PASS

- [ ] **Step 5: 앱 전체가 컴파일되는지 본다**

Run: `./gradlew :app:assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL — 여기서 옛 `GetMyGroupsUseCase` 를 부르는 잔여 호출부가 있으면 드러난다

- [ ] **Step 6: 변경 파일을 확인해 보고한다**

Run: `git status --short`

---

### Task 7: 세션 종료 시 캐시 정리

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCaseTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt`

**Interfaces:**
- Consumes: Task 2의 `ParfaitGroupRepository.clearGroups()`, Task 1의 `GroupLocalDataSource.clear()`
- Produces: 없음

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`LogoutUseCaseTest`에 mock 하나를 더하고 케이스를 더한다.

```kotlin
    private val parfaitGroupRepository: ParfaitGroupRepository = mockk(relaxed = true)

    @Test
    fun invoke_clearsTokenAccountAndGroups() = runTest {
        // Given 로그아웃이 성공한다
        coEvery { authRepository.logout() } returns Result.success(Unit)

        // When 로그아웃한다
        LogoutUseCase(authRepository, memberRepository, parfaitGroupRepository).invoke()

        // Then 세 가지를 모두 지운다 — 하나만 남으면 계정 전환 때 이전 사용자 흔적이 남는다
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
        verify(exactly = 1) { parfaitGroupRepository.clearGroups() }
    }
```

`TokenAuthenticatorTest`에는 강제 로그아웃 케이스에 아래 단언을 더한다.

```kotlin
        // Then 토큰·계정 정보와 함께 그룹 캐시도 지운다
        verify(exactly = 1) { groupLocalDataSource.clear() }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :domain:test --tests '*LogoutUseCaseTest*' :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*' --console=plain`
Expected: 컴파일 실패 — 생성자 인자가 맞지 않는다

- [ ] **Step 3: `LogoutUseCase` 를 고친다**

```kotlin
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val result = authRepository.logout()
        runSuspendCatching { memberRepository.clearMyAccount() }
        parfaitGroupRepository.clearGroups()
        return result
    }
}
```

KDoc 의 "토큰과 계정 정보를 함께 정리한다"를 "토큰·계정 정보·그룹 캐시를 함께 정리한다"로 고치고, 그룹 캐시는 인메모리라 `runSuspendCatching` 이 필요 없다는 것을 한 줄로 적는다.

- [ ] **Step 4: `TokenAuthenticator` 를 고친다**

생성자에 `private val groupLocalDataSource: GroupLocalDataSource` 를 더하고, `tokenStore.clear()`·`userInfoLocalDataSource.clear()` 옆에 `groupLocalDataSource.clear()` 를 더한다. 인메모리라 IO 실패 경로가 없다.

- [ ] **Step 5: 테스트가 통과하는지 본다**

Run: `./gradlew :domain:test :data:testDebugUnitTest --console=plain`
Expected: PASS

- [ ] **Step 6: 전체 검증**

Run: `./gradlew :app:assembleDebug --console=plain`
Run: `./gradlew :data:testDebugUnitTest :domain:test :feature:groups:list:impl:testDebugUnitTest :feature:groups:setting:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest --console=plain`
Run: `./gradlew :data:ktlintCheck :domain:ktlintCheck --console=plain`
Expected: 모두 통과

- [ ] **Step 7: 변경 파일 전체를 확인해 보고한다**

Run: `git status --short`
Expected: Task 1~7의 변경이 모두 미커밋 상태로 남아 있다. **커밋하지 않는다.**

---

## 검증 못 하는 것

아래는 유닛 테스트로 덮이지 않으므로 실기기 확인이 필요하다. 보고할 때 "미검증"으로 명시한다.

- 그룹 생성·참여 후 목록에 새 그룹이 실제로 뜨는지(캐시 갱신 + `Enter` 재조회가 함께 도는 경로)
- 설정에서 닉네임을 바꾼 뒤 캔버스·목록으로 나갔을 때 값이 따라오는지
- 나가기·신고 후 목록에서 그 그룹이 사라지는지
- 계정 전환 시 이전 계정 그룹이 남지 않는지(로그아웃 → 다른 계정 로그인)
- 캔버스 그룹명이 실제 그룹 이름으로 뜨는지
