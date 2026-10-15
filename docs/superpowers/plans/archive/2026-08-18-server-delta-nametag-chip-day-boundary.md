# 서버 delta 08df1bf 반영 Implementation Plan

> ✅ **완료·develop 머지(PR #308 `feature/#300-sync-backend-api-250818` → `412991ea`, 2026-08-20)** —
> 8 Task 전량이 develop에 있다. 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> 선행 PR #307이 먼저 들어간 직후 같은 순서로 머지됐고 머지 커밋에 충돌 해소 편집이 0건이다.
> Task 4가 자체 신설한 `YGColorChipType.Default`는 이 머지로 develop 소유가 됐다 — PR #298과
> 글자를 맞춰 둔 이유(충돌 해소를 한 블록 삭제로 끝내려는 것)는 이제 #298 쪽 몫이다.
> 유닛 511 → 532건.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 `08df1bf`가 들여온 응답 필드 넷(Nametag-Chip 3종 + 그룹 상세 `groupName`·`memberLimit`)을 앱이 읽게 하고, "오늘"의 경계를 자정에서 03시로 옮긴다.

**Architecture:** 칩 타입은 `:domain`의 중립 enum `NametagChipType`으로 받고 feature impl이 `:core:designsystem` 타입으로 옮긴다(`:domain`은 순수 JVM이라 designsystem을 모른다). 값이 없거나 `RELEASED`면 새 `Default` 변형으로 접는다. 그룹 상세에 `groupName`이 실리면서 `GroupDetailVO` 조합이 필요 없어져 삭제한다. 하루 경계는 `parfaitToday()` 한 함수만 고치면 재시도 조건·달력 기준이 저절로 따라온다.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, kotlinx-serialization, kotlinx-datetime, MockK, Turbine, kotlin.test, JUnit4

**Spec:** [`parfait/specs/2026-08-18-server-delta-nametag-chip-day-boundary.md`](../../specs/archive/2026-08-18-server-delta-nametag-chip-day-boundary.md)

**작업 저장소·브랜치:** `TJYG-Android`(경로는 `wiki/personal-private/project-paths.md`). 계획을 쓸 당시 브랜치는 **`feature/#294-group-ssot`**(PR #299) 위였으나, 그 PR이 닫히면서 산출물은 **`feature/#300-sync-backend-api-250818`**(PR #308)로 갈렸고 **`refactor/#294-group-data-using-ssot`**(PR #307) 위에 얹혀 있다. 워크트리를 새로 만들지 않는다.

## Global Constraints

- 커밋 메시지는 영어 본문, 타입 접두사(`feat:`·`fix:`·`refactor:`·`test:`)를 쓴다. 저장소 기존 로그와 같은 형식.
- **매퍼 단독 테스트 파일(`XxxVOMapperTest`)을 새로 만들지 않는다.** 판단이 든 변환은 DataSource 테스트 케이스로 잠근다.
- `:domain`은 `:core:util:jvm`과 `javax.inject`만 의존한다. Compose·designsystem 타입을 절대 import 하지 않는다.
- 응답 DTO는 서버의 거울이다 — 필드명·널 허용을 서버와 같게 두고 sealed·도메인 타입을 DTO에 넣지 않는다.
- 닫힌 도메인(enum·sealed·nullable enum)을 분류하는 `when` **표현식에는 `else`를 쓰지 않는다.** 컴파일러가 빠짐을 잡게 둔다. `else`는 서버 문자열처럼 열린 입력에만 쓴다.
- 하루 경계 상수는 `DayWindow.DAY_BOUNDARY_HOUR`(= 3) 하나뿐이다. 03을 새로 적지 않는다.
- 라인번호·색 hex를 문서에 적지 않는다(파르페 규율). 코드에는 디자인 토큰(`YGAtomicColors.*`)만 쓴다.
- 빌드·테스트는 저장소 루트에서 `./gradlew`로 돈다.

---

### Task 1: 하루 경계를 03시로 옮긴다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/ParfaitDay.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/model/ParfaitDayTest.kt` (create)
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitUseCaseTest.kt` (modify)

**Interfaces:**
- Consumes: `DayWindow.DAY_BOUNDARY_HOUR`(`domain/model/DayWindow.kt`, `Int` = 3)
- Produces: `parfaitToday(clock: Clock = Clock.System): LocalDate` — 시그니처 불변, 03시 이전이면 전날을 돌려준다. `GetTodayParfaitUseCase`·`GetParfaitYearsUseCase`·`CanvasMainViewModel`이 그대로 쓴다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`domain/src/test/java/com/teamyg/parfait/domain/model/ParfaitDayTest.kt` 새로 만든다. 고정 시계 관용구는 같은 디렉토리 `DayWindowTest`의 것을 그대로 따른다.

```kotlin
package com.teamyg.parfait.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class ParfaitDayTest {
    private fun fixedClock(iso: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(iso)
    }

    @Test
    fun parfaitToday_justBeforeBoundary_isStillYesterday() {
        // Given 한국 시간 8월 18일 02:59 (UTC 로는 8월 17일 17:59)
        val clock = fixedClock("2026-08-17T17:59:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 아직 전날 캔버스가 진행 중이다 — 서버 ParfaitDay 와 같은 기준
        assertEquals(LocalDate(2026, 8, 17), today)
    }

    @Test
    fun parfaitToday_atBoundary_rollsOver() {
        // Given 한국 시간 8월 18일 03:00 정각
        val clock = fixedClock("2026-08-17T18:00:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 경계 정각부터 새 날이다
        assertEquals(LocalDate(2026, 8, 18), today)
    }

    @Test
    fun parfaitToday_justAfterMidnight_isYesterday() {
        // Given 한국 시간 8월 18일 00:00 정각 (UTC 로는 8월 17일 15:00)
        val clock = fixedClock("2026-08-17T15:00:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 자정은 경계가 아니다 — 달력이 넘어가도 캔버스는 안 넘어간다
        assertEquals(LocalDate(2026, 8, 17), today)
    }

    @Test
    fun parfaitToday_lateEvening_isSameCalendarDay() {
        // Given 한국 시간 8월 18일 23:59 (UTC 로는 8월 18일 14:59)
        val clock = fixedClock("2026-08-18T14:59:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 경계 뒤라 달력 날짜와 같다
        assertEquals(LocalDate(2026, 8, 18), today)
    }

    @Test
    fun parfaitToday_usesSeoulNotDeviceZone() {
        // Given 한국 시간 8월 18일 11:00 인 순간 (UTC 로는 8월 18일 02:00)
        val clock = fixedClock("2026-08-18T02:00:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 기기 시간대와 무관하게 KST 로 센다 — CI 가 UTC 여도 같은 답이 나온다
        assertEquals(LocalDate(2026, 8, 18), today)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :domain:testDebugUnitTest --tests "com.teamyg.parfait.domain.model.ParfaitDayTest"`
Expected: FAIL — `parfaitToday_justBeforeBoundary_isStillYesterday`·`parfaitToday_justAfterMidnight_isYesterday`가 하루 뒤 날짜를 돌려준다(`expected <2026-08-17> but was <2026-08-18>`).

- [ ] **Step 3: 최소 구현을 쓴다**

`domain/src/main/java/com/teamyg/parfait/domain/model/ParfaitDay.kt`의 `parfaitToday`만 바꾼다. `PARFAIT_TIME_ZONE` 선언과 그 KDoc은 그대로 둔다.

```kotlin
package com.teamyg.parfait.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 파르페의 하루가 놓이는 시간대.
 *
 * 하루를 가르는 것은 기기가 아니라 서버다 — 캔버스 행이 KST 날짜를 키로 저장되고
 * (`TZ=Asia/Seoul`), 오늘 조회도 서버가 그 날짜로 캔버스를 찾는다. 기기 시간대로 오늘을 세면
 * 해외에 있는 기기에서 서버와 하루가 어긋나, 서버가 준 캔버스를 어제 것으로 오해하거나
 * 달력이 오늘을 미래로 보고 잠근다.
 */
val PARFAIT_TIME_ZONE: TimeZone = TimeZone.of("Asia/Seoul")

/**
 * 파르페 기준의 오늘. 기기 시간대를 따르지 않는 이유는 [PARFAIT_TIME_ZONE] 에 있다.
 *
 * 하루는 자정이 아니라 **새벽 3시**에 넘어간다 — 캔버스 마감 배치가 도는 시각이라
 * 자정~03시 사이에는 아직 전날 캔버스가 진행 중이다. 서버 `ParfaitDay.current()` 의 거울이고,
 * 계약이 그 값을 내려주지 않아 앱이 복제하고 있다. **서버가 배치 시각을 바꾸면 여기도 바꾼다.**
 * 경계 값은 [DayWindow.DAY_BOUNDARY_HOUR] 하나만 쓴다 — 두 곳에 적으면 한쪽만 고쳐진다.
 */
fun parfaitToday(clock: Clock = Clock.System): LocalDate {
    val now = clock.now().toLocalDateTime(PARFAIT_TIME_ZONE)

    return if (now.time < LocalTime(DayWindow.DAY_BOUNDARY_HOUR, 0)) {
        now.date.minus(1, DateTimeUnit.DAY)
    } else {
        now.date
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :domain:testDebugUnitTest --tests "com.teamyg.parfait.domain.model.ParfaitDayTest"`
Expected: PASS (5건)

- [ ] **Step 5: `GetTodayParfaitUseCaseTest`에 경계 구간 케이스를 더한다**

기존 테스트는 `parfaitToday()`를 실행 시각으로 읽어 오늘/어제를 만든다. 그 관용구를 유지하되, **03시 이전 구간에서 서버가 준 날짜가 곧 오늘이라 재호출이 안 일어난다**는 것을 잠근다. 파일 맨 아래 `private companion object` 바로 앞에 테스트를 더한다.

```kotlin
    @Test
    fun invoke_beforeRolloverHour_treatsPreviousCalendarDayAsToday() = runTest {
        // Given 서버가 파르페 기준의 오늘 날짜로 캔버스를 준다.
        //  03시 이전이면 그 날짜는 달력상 어제이고, 그때도 어긋난 응답이 아니다
        val repository = FakeParfaitRepository(listOf(Result.success(canvas(PARFAIT_ID, today))))

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 한 번만 부른다 — 경계 기준이 서버와 같으면 재요청이 사라진다
        assertEquals(1, repository.callCount)
        assertEquals(ParfaitId(PARFAIT_ID), result.getOrNull()?.parfaitId)
    }
```

- [ ] **Step 6: 도메인 테스트 전체를 돌린다**

Run: `./gradlew :domain:testDebugUnitTest`
Expected: PASS. `GetParfaitYearsUseCaseTest`·`GetParfaitHistoriesUseCaseTest`가 깨지면 `parfaitToday()`를 실행 시각으로 읽는 관용구를 쓰는 자리이므로, 기대값을 리터럴 날짜로 바꾸지 말고 그 관용구를 유지한 채 고친다.

- [ ] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/ParfaitDay.kt \
        domain/src/test/java/com/teamyg/parfait/domain/model/ParfaitDayTest.kt \
        domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/GetTodayParfaitUseCaseTest.kt
git commit -m "fix(parfait): roll the parfait day over at 3am instead of midnight"
```

---

### Task 2: 칩 타입을 도메인에 세우고 그룹 계약을 받는다

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/group/NametagChipType.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/MyParfaitGroupVO.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupDetailVO.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupMemberVO.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/MyParfaitGroupResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/MyParfaitGroupDetailResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/ParfaitGroupMemberResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt` (create)

**Interfaces:**
- Produces:
  - `enum class NametagChipType { TYPE1 … TYPE12, RELEASED }` (`com.teamyg.parfait.domain.model.group`)
  - `MyParfaitGroupVO.lastPlacedByNametagChip: NametagChipType?`
  - `ParfaitGroupDetailVO.groupName: GroupName` · `ParfaitGroupDetailVO.memberLimit: Int`
  - `ParfaitGroupMemberVO.nametagChip: NametagChipType?`
- Consumes: 기존 `GroupName`·`GroupNickname`·`InviteCode`·`GroupId`·`MemberId` value class

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ParfaitGroupRemoteDataSourceImplTest`를 새로 만든다. 이 도메인에 DataSource 테스트가 없어 신설이고, 관용구는 `data/src/test/.../member/remote/MemberRemoteDataSourceImplTest`를 따른다. 매퍼 단독 파일을 만들지 않는다는 규약대로 **칩 문자열 판정을 여기서 잠근다.**

```kotlin
package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupDetailResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.ParfaitGroupMemberResponse
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParfaitGroupRemoteDataSourceImplTest {
    private val parfaitGroupService: ParfaitGroupService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitGroupRemoteDataSourceImpl(
        parfaitGroupService = parfaitGroupService,
        apiCaller = apiCaller,
    )

    private fun <T> success(data: T) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = data,
    )

    private fun groupResponse(lastPlacedByNametagChip: String?) = MyParfaitGroupResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        recentImageUrl = null,
        recentImageUploadedAt = null,
        lastPlacedByNametagChip = lastPlacedByNametagChip,
    )

    private fun detailResponse(memberChip: String?) = MyParfaitGroupDetailResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        groupNickname = "모카",
        inviteCode = "ABCDEF",
        memberLimit = 12,
        members = listOf(
            ParfaitGroupMemberResponse(
                memberId = 42L,
                groupNickname = "모카",
                nametagChip = memberChip,
            ),
        ),
    )

    @Test
    fun getMyGroups_knownChipString_becomesThatType() = runTest {
        // Given 서버가 마지막 토퍼의 칩을 enum 이름 문자열로 준다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("TYPE7")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then 도메인 enum 으로 바뀐다
        assertEquals(NametagChipType.TYPE7, result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_releasedChip_isKeptNotFolded() = runTest {
        // Given 마지막 토퍼가 그룹을 나가 서버가 반납 표식을 준다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("RELEASED")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then null 로 접지 않는다 — "나간 사람"과 "값이 없다"는 뜻이 다르다
        assertEquals(NametagChipType.RELEASED, result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_missingChip_isNull() = runTest {
        // Given 아직 아무도 토핑을 올리지 않아 칩이 없다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse(null)))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then null 그대로 둔다
        assertNull(result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_unknownChipString_foldsToNull() = runTest {
        // Given 서버가 앱이 모르는 값을 준다 — 열린 입력이다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("TYPE99")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then 던지지 않고 null 로 접는다 — 모르는 색은 그리지 못할 뿐이다
        assertNull(result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getGroupDetail_carriesNameLimitAndMemberChip() = runTest {
        // Given 서버가 그룹명·정원·멤버 칩을 함께 준다
        coEvery { parfaitGroupService.getParfaitGroupsByGroupId(1L) } returns success(detailResponse("TYPE3"))

        // When 상세를 받는다
        val detail = dataSource.getGroupDetail(GroupId(1L)).getOrNull()

        // Then 셋 다 VO 로 넘어온다 — 목록을 한 번 더 읽을 이유가 사라진다
        assertEquals("모카의 파르페", detail?.groupName?.value)
        assertEquals(12, detail?.memberLimit)
        assertEquals(NametagChipType.TYPE3, detail?.members?.single()?.nametagChip)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSourceImplTest"`
Expected: FAIL — 컴파일 에러. `NametagChipType` 미해결, `MyParfaitGroupResponse`에 `lastPlacedByNametagChip` 없음, `MyParfaitGroupDetailResponse`에 `groupName`·`memberLimit` 없음, `ParfaitGroupMemberResponse`에 `nametagChip` 없음.

- [ ] **Step 3: 도메인 enum을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/group/NametagChipType.kt`:

```kotlin
package com.teamyg.parfait.domain.model.group

/**
 * 그룹 안에서 사람을 가리키는 칩 타입. 배정 주체는 **서버**다 — 참여·생성 시 그 그룹의 활동
 * 멤버가 안 쓰는 값 중 하나를 받고, 그룹을 나가면 [RELEASED] 로 반납된다. 다시 뽑는 경로는 없다.
 *
 * 유일성은 **그룹 안에서만** 성립한다 — 같은 사람이 그룹마다 다른 타입을 받는다(닉네임 초기값이
 * 계정 공통인 것과 반대다).
 *
 * 화면 색으로 옮기는 일은 feature 가 한다. `:domain` 은 `:core:designsystem` 을 모른다.
 */
enum class NametagChipType {
    TYPE1,
    TYPE2,
    TYPE3,
    TYPE4,
    TYPE5,
    TYPE6,
    TYPE7,
    TYPE8,
    TYPE9,
    TYPE10,
    TYPE11,
    TYPE12,

    /**
     * 그룹을 나간 사람이 반납한 자리. 12종과 달리 여럿이 동시에 가질 수 있다.
     *
     * "값이 없다"(`null`)와 뜻이 다르다 — 지금은 화면 표현이 같지만 계약이 갈라 주는 것을
     * 매퍼가 뭉개면 되돌릴 수 없다.
     */
    RELEASED,
}
```

- [ ] **Step 4: VO에 필드를 더한다**

`MyParfaitGroupVO.kt` — `lastPlacedByNametagChip`을 더한다.

```kotlin
package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId
import kotlin.time.Instant

data class MyParfaitGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val recentImageUrl: String?,
    /** 오프셋이 붙은 절대 시점 — 기기 타임존과 무관하게 같은 순간을 가리킨다 */
    val recentImageUploadedAt: Instant?,
    /**
     * 마지막으로 토핑을 올린 사람의 칩. 그 사람이 이미 그룹을 나갔으면
     * [NametagChipType.RELEASED] 이고, 토핑이 하나도 없으면 `null` 이다.
     */
    val lastPlacedByNametagChip: NametagChipType?,
)
```

`ParfaitGroupMemberVO.kt` — `nametagChip`을 더한다.

```kotlin
package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.MemberId

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
    /**
     * 서버가 이 그룹 안에서 배정한 칩. 상세 응답은 탈퇴자를 빼고 주므로 실제로는
     * [NametagChipType.RELEASED] 도 `null` 도 오지 않지만, 계약 타입이 널 허용이라 그대로 받는다.
     */
    val nametagChip: NametagChipType?,
)
```

`ParfaitGroupDetailVO.kt` — `groupName`·`memberLimit`을 더하고, `groupNickname`이 "나"의 것임을 KDoc에 박는다(Task 5에서 `GroupDetailVO`가 사라지면서 `myNickname`이라는 이름이 없어진다).

```kotlin
package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

/**
 * 그룹 상세. 서버 응답 하나에 1:1 로 대응한다.
 *
 * @param groupNickname **인증 회원 본인**이 이 그룹에서 쓰는 이름이다. 전역 닉네임과 별개이고,
 *  [members] 안의 내 항목과 같은 값이다
 * @param memberLimit 그룹 정원(1~12). 생성 이후 바뀌지 않는다
 * @param members 탈퇴하지 않은 멤버만, 참여 순
 */
data class ParfaitGroupDetailVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val groupNickname: GroupNickname,
    val inviteCode: InviteCode,
    val memberLimit: Int,
    val members: List<ParfaitGroupMemberVO>,
)
```

- [ ] **Step 5: 응답 DTO에 필드를 더한다**

`MyParfaitGroupResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyParfaitGroupResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("recentImageUrl")
    val recentImageUrl: String? = null,
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    /** 마지막 토퍼가 이미 그룹을 나갔으면 `"RELEASED"` 가 온다 */
    @SerialName("lastPlacedByNametagChip")
    val lastPlacedByNametagChip: String? = null,
)
```

`MyParfaitGroupDetailResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyParfaitGroupDetailResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("inviteCode")
    val inviteCode: String,
    @SerialName("memberLimit")
    val memberLimit: Int,
    @SerialName("members")
    val members: List<ParfaitGroupMemberResponse>,
)
```

`ParfaitGroupMemberResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParfaitGroupMemberResponse(
    @SerialName("memberId")
    val memberId: Long,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("nametagChip")
    val nametagChip: String? = null,
)
```

- [ ] **Step 6: 매퍼를 고친다**

`data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt`에서 세 함수를 고치고 문자열 판정 함수를 더한다. import에 `com.teamyg.parfait.domain.model.group.NametagChipType`을 추가한다.

```kotlin
/**
 * 서버가 주는 칩 이름을 도메인 값으로 바꾼다.
 *
 * 열린 입력이라 모르는 문자열은 `null` 로 접는다 — 새 타입이 서버에 먼저 들어와도 목록 조회가
 * 통째로 실패하지 않아야 한다. `"RELEASED"` 는 접지 않고 그대로 남긴다.
 */
private fun String?.toNametagChipType(): NametagChipType? =
    this?.let { raw -> NametagChipType.entries.firstOrNull { it.name == raw } }

internal fun MyParfaitGroupResponse.toMyParfaitGroupVO(): MyParfaitGroupVO = MyParfaitGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    recentImageUrl = recentImageUrl,
    // 오프셋(`Z`)째로 읽는다 — 벽시계 숫자로 받으면 기기 타임존에 따라 다른 시점이 된다
    recentImageUploadedAt = recentImageUploadedAt?.let(Instant::parse),
    lastPlacedByNametagChip = lastPlacedByNametagChip.toNametagChipType(),
)

internal fun MyParfaitGroupDetailResponse.toParfaitGroupDetailVO(): ParfaitGroupDetailVO = ParfaitGroupDetailVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    groupNickname = GroupNickname(groupNickname),
    inviteCode = InviteCode(inviteCode),
    memberLimit = memberLimit,
    members = members.map { it.toParfaitGroupMemberVO() },
)

internal fun ParfaitGroupMemberResponse.toParfaitGroupMemberVO(): ParfaitGroupMemberVO = ParfaitGroupMemberVO(
    memberId = MemberId(memberId),
    groupNickname = GroupNickname(groupNickname),
    nametagChip = nametagChip.toNametagChipType(),
)
```

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSourceImplTest"`
Expected: PASS (5건)

- [ ] **Step 8: 새 필드 때문에 깨진 기존 테스트를 고친다**

Run: `./gradlew :data:testDebugUnitTest :domain:testDebugUnitTest`
Expected: 처음엔 컴파일 에러. 아래 두 곳이 `ParfaitGroupDetailVO`·`ParfaitGroupMemberVO`를 세우므로 새 인자를 채운다.
- `data/src/test/java/com/teamyg/parfait/data/repository/group/ParfaitGroupRepositoryImplTest.kt` — 상세 픽스처에 `groupName = GroupName("모카의 파르페")`·`memberLimit = 12`, 멤버에 `nametagChip = NametagChipType.TYPE1`
- `data/src/test/java/com/teamyg/parfait/data/source/group/mapper/MyParfaitGroupVOMapperTest.kt` — `response(...)` 헬퍼는 DTO 기본값 덕에 그대로 컴파일된다. 손대지 않는다.

고친 뒤 다시 돌려 PASS를 확인한다.

- [ ] **Step 9: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/group/ \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/group/ \
        data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt \
        data/src/test/java/com/teamyg/parfait/data/
git commit -m "feat(group): read server-assigned nametag chips, group name and member limit"
```

---

### Task 3: 캔버스 `placedBy`에 칩 필드를 미러한다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `PlacedByResponse.nametagChip: String?` — **도메인 VO로는 올리지 않는다.** 뒤 태스크가 이 값을 읽지 않는다.

이 태스크에는 테스트가 없다. 관측 가능한 동작이 하나도 안 바뀌기 때문이다 — DTO는 서버의 거울이라는 규약을 지키려고 필드만 받아 두고, 도메인으로 올리는 결정은 소비자가 생길 때(C-202 작성자 표시, PR #298) 함께 한다. `ToppingPlacerVO`는 토핑 배치 확정 응답과 공유하는데 서버가 그쪽엔 칩을 주지 않아, 지금 올리면 타입을 가르거나 nullable로 "없다"와 "모른다"를 뭉개야 한다.

- [ ] **Step 1: DTO에 필드를 더한다**

`GetTodayParfaitResponse.kt`의 `PlacedByResponse`만 고친다.

```kotlin
/**
 * 배치자. 같은 이름의 DTO 가 response/parfaitimage 에도 있다 — 서버가 두 응답에 같은 이름을
 * 썼고 wire DTO 는 서버의 거울이라 이름을 바꾸지 않는다.
 *
 * @param nickname 그룹 닉네임이다. 탈퇴·이탈한 멤버면 "(알수없음)"이 온다.
 * @param nametagChip 그 사람의 칩. 탈퇴했으면 `"RELEASED"` 다. **아직 도메인으로 올리지 않는다** —
 *  읽는 화면이 없고, [com.teamyg.parfait.domain.model.topping.ToppingPlacerVO] 를 배치 확정
 *  응답과 공유하는데 서버가 그쪽엔 이 값을 주지 않아서다. C-202 작성자 표시가 붙을 때 정한다.
 */
@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nametagChip")
    val nametagChip: String? = null,
)
```

- [ ] **Step 2: 컴파일과 기존 테스트를 확인한다**

Run: `./gradlew :data:testDebugUnitTest`
Expected: PASS. `ParfaitRemoteDataSourceImplTest`가 `PlacedByResponse`를 세우더라도 새 필드에 기본값이 있어 그대로 컴파일된다.

- [ ] **Step 3: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt
git commit -m "feat(parfait): mirror the placedBy nametag chip field in the canvas response"
```

---

### Task 4: 디자인시스템에 `Default` 변형을 더한다

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcolorchip/YGColorChipType.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yggrouptagchip/YGGrouptagChipType.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcolorchip/YGNametagChipPreviewData.kt`

**Interfaces:**
- Produces: `YGColorChipType.Default`(`data object`) · `YGGrouptagChipType.DEFAULT`(enum entry). Task 6·7이 폴백으로 쓴다.

> ⚠️ 열린 PR #298(스포트라이트 토핑)도 `YGColorChipType.Default`를 **같은 이름·같은 토큰·같은 위치**로 추가한다. 아래 코드는 그 PR의 것과 글자까지 같으므로, 머지 충돌이 나면 한쪽을 지우면 끝난다. 정의를 임의로 바꾸지 말 것.

테스트가 없다. 디자인 토큰 선언이라 단언할 동작이 없고, 이 저장소는 designsystem 타입에 유닛 테스트를 두지 않는다. 검증은 프리뷰와 뒤 태스크의 ViewModel 테스트가 한다.

- [ ] **Step 1: `YGColorChipType`에 `Default`를 더한다**

파일 맨 아래 `NametagChipPlus` 블록 뒤, 닫는 중괄호 앞에 넣는다.

```kotlin
    /**
     * 실제 컬러를 배정할 수 없을 때 쓰는 중립 상태 — 탈퇴한 그룹원의 칩을 보여줘야 하거나,
     * 칩 정보를 불러오지 못했을 때 등.
     */
    data object Default : YGColorChipType {
        override val fillColor = YGAtomicColors.Gray.White
        override val strokeColor = YGAtomicColors.Gray.Gray100
        override val textColor = YGAtomicColors.Gray.Gray300
    }
```

- [ ] **Step 2: `YGGrouptagChipType`에 `DEFAULT`를 더한다**

```kotlin
package com.teamyg.parfait.core.designsystem.component.yggrouptagchip

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * Figma Grouptag-Chip Type
 */
enum class YGGrouptagChipType(val timestampColor: Color) {
    TYPE_1_2(YGAtomicColors.Cherry.Cherry100),
    TYPE_3_4(YGAtomicColors.Cherry.Cherry200),
    TYPE_5_6(YGAtomicColors.Cherry.Cherry300),
    TYPE_7_8(YGAtomicColors.Gray.Gray200),
    TYPE_9_10(YGAtomicColors.Melon.Melon500),
    TYPE_11_12(YGAtomicColors.Pudding.Pudding500),

    /** 마지막으로 바꾼 사람을 가리킬 수 없을 때 — 그 사람이 나갔거나, 아직 아무도 안 올렸다 */
    DEFAULT(YGAtomicColors.Gray.Gray300),
}
```

- [ ] **Step 3: 프리뷰 데이터에 `Default`를 더한다**

`YGNametagChipPreviewData.kt`의 `values` 시퀀스 맨 끝(`NametagChipPlus` 항목 뒤)에 한 항목을 더한다.

```kotlin
        YGChipPreviewData(
            name = "Default",
            colorChipType = YGColorChipType.Default,
        ),
```

- [ ] **Step 4: 컴파일을 확인한다**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/
git commit -m "feat(designsystem): add Default variants to nametag and grouptag chips"
```

---

### Task 5: `GroupDetailVO`를 지우고 상세 조합을 걷는다

**Files:**
- Delete: `domain/src/main/java/com/teamyg/parfait/domain/model/group/GroupDetailVO.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/GetGroupDetailUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/group/ParfaitGroupRepository.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/group/GetGroupDetailUseCaseTest.kt`

**Interfaces:**
- Consumes: `ParfaitGroupDetailVO`(Task 2에서 `groupName`·`memberLimit`을 얻음)
- Produces: `GetGroupDetailUseCase.invoke(groupId: GroupId): Flow<ParfaitGroupDetailVO?>` — **반환 타입이 `GroupDetailVO?`에서 `ParfaitGroupDetailVO?`로 바뀐다.** Task 6이 이 타입을 받는다. 필드명도 `myNickname` → `groupNickname`으로 바뀐다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`GetGroupDetailUseCaseTest.kt`를 아래로 바꾼다. 기존 테스트가 `myGroups`를 채워야 이름이 나온다고 단언하고 있으므로 그 전제 자체를 뒤집는다.

```kotlin
package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetGroupDetailUseCaseTest {
    private val repository: ParfaitGroupRepository = mockk()

    private val groupId = GroupId(1L)

    private val detail = ParfaitGroupDetailVO(
        groupId = groupId,
        groupName = GroupName("모카의 파르페"),
        groupNickname = GroupNickname("모카"),
        inviteCode = InviteCode("ABCDEF"),
        memberLimit = 12,
        members = listOf(
            ParfaitGroupMemberVO(
                memberId = MemberId(42L),
                groupNickname = GroupNickname("모카"),
                nametagChip = NametagChipType.TYPE3,
            ),
        ),
    )

    @Test
    fun invoke_detailCached_emitsItWithoutReadingTheGroupList() = runTest {
        // Given 상세 캐시에만 값이 있고 목록 캐시는 비어 있다
        every { repository.groupDetail(groupId) } returns MutableStateFlow(detail)

        // When 상세를 구독한다
        val emitted = GetGroupDetailUseCase(repository)(groupId).first()

        // Then 그룹명까지 상세 하나에서 나온다 — 목록을 한 번 더 읽지 않는다
        assertEquals(GroupName("모카의 파르페"), emitted?.groupName)
        assertEquals(12, emitted?.memberLimit)
        assertEquals(NametagChipType.TYPE3, emitted?.members?.single()?.nametagChip)
    }

    @Test
    fun invoke_detailNotCachedYet_emitsNull() = runTest {
        // Given 아직 상세를 한 번도 받지 못했다
        every { repository.groupDetail(groupId) } returns MutableStateFlow(null)

        // When 상세를 구독한다
        val emitted = GetGroupDetailUseCase(repository)(groupId).first()

        // Then 미조회는 null 로 나온다 — 화면이 로딩과 빈 값을 가른다
        assertNull(emitted)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :domain:testDebugUnitTest --tests "com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCaseTest"`
Expected: FAIL — 컴파일 에러. UseCase가 아직 `GroupDetailVO?`를 내보내고 `repository.myGroups` 스텁이 없어 mockk가 던진다.

- [ ] **Step 3: UseCase에서 `combine`을 걷는다**

```kotlin
package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 캐시된 그룹 상세를 구독한다. 값을 새로 받는 것은 [RefreshGroupDetailUseCase] 의 일이다.
 *
 * 서버가 상세 응답에 그룹명·정원을 실어 주기 전에는 목록 캐시에서 이름만 집어 붙였는데
 * (서버 `08df1bf`), 지금은 상세 하나로 화면이 채워진다.
 */
class GetGroupDetailUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    operator fun invoke(groupId: GroupId): Flow<ParfaitGroupDetailVO?> =
        parfaitGroupRepository.groupDetail(groupId)
}
```

- [ ] **Step 4: `GroupDetailVO`를 지운다**

```bash
git rm domain/src/main/java/com/teamyg/parfait/domain/model/group/GroupDetailVO.kt
```

- [ ] **Step 5: Repository KDoc의 TODO를 걷는다**

`ParfaitGroupRepository.kt`의 `refreshGroupDetail` KDoc에서 `TODO(서버 응답 확장 대기)` 문단을 지우고 아래로 바꾼다.

```kotlin
    /**
     * 서버에서 그 그룹 상세를 다시 받아 캐시를 덮는다. 실패하면 캐시는 그대로다.
     *
     * 응답에 그룹명·정원·멤버 칩이 함께 온다(서버 `08df1bf`) — 그전에는 이름을
     * [refreshMyGroups] 에서 따로 집어 왔고 정원은 얻을 길이 없었다.
     */
    suspend fun refreshGroupDetail(groupId: GroupId): Result<Unit>
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

Run: `./gradlew :domain:testDebugUnitTest --tests "com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCaseTest"`
Expected: PASS (2건)

- [ ] **Step 7: 커밋**

Task 6에서 `GroupSettingViewModel`을 고칠 때까지 `:feature:groups:setting:impl`은 컴파일되지 않는다. `:domain` 테스트만 확인하고 커밋한다.

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/ \
        domain/src/test/java/com/teamyg/parfait/domain/usecase/group/GetGroupDetailUseCaseTest.kt
git commit -m "refactor(group): drop GroupDetailVO now that the detail response carries the name"
```

---

### Task 6: S-101 그룹 설정이 서버 칩과 정원을 쓴다

**Files:**
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModel.kt`
- Test: `feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModelTest.kt`

**Interfaces:**
- Consumes: `NametagChipType`(Task 2) · `ParfaitGroupDetailVO`(Task 2·5) · `YGColorChipType.Default`(Task 4) · `GetGroupDetailUseCase(groupId): Flow<ParfaitGroupDetailVO?>`(Task 5)
- Produces: 없음(화면 종단)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

먼저 companion의 `DETAIL` 픽스처를 새 타입으로 바꾼다(`GroupDetailVO` → `ParfaitGroupDetailVO`, `myNickname` → `groupNickname`, `memberLimit`·`nametagChip` 추가). 파일의 다른 테스트가 전부 이 픽스처를 쓰므로 이것만 고치면 나머지는 그대로 산다.

```kotlin
        val DETAIL = ParfaitGroupDetailVO(
            groupId = GroupId(GROUP_ID),
            groupName = GroupName("그룹이름"),
            groupNickname = GroupNickname(MY_NICKNAME),
            inviteCode = InviteCode("WDIDCJ"),
            memberLimit = 12,
            members = listOf(
                ParfaitGroupMemberVO(MemberId(1L), GroupNickname(MY_NICKNAME), NametagChipType.TYPE8),
                ParfaitGroupMemberVO(MemberId(2L), GroupNickname("체리마루"), NametagChipType.TYPE3),
                ParfaitGroupMemberVO(MemberId(3L), GroupNickname("푸딩왕자"), NametagChipType.TYPE11),
            ),
        )
```

픽스처를 변형해 쓸 헬퍼를 클래스 본문의 `newViewModel()` 위에 둔다.

```kotlin
    /** [DETAIL] 에서 정원·멤버만 바꾼 상세. 나머지 필드는 기본 픽스처 그대로다 */
    private fun detailOf(
        memberLimit: Int = 12,
        members: List<ParfaitGroupMemberVO> = DETAIL.members,
    ): ParfaitGroupDetailVO = DETAIL.copy(memberLimit = memberLimit, members = members)
```

그리고 테스트 넷을 더한다. `@Before`가 `getGroupDetail`을 `DETAIL`로 스텁해 두므로, 다른 값이 필요한 테스트는 `every { ... }`로 덮어쓴 뒤 `viewModel()`을 만든다(`viewModel()`이 `advanceUntilIdle()`까지 한다).

```kotlin
    @Test
    fun detailArrives_memberChipsFollowTheServerAssignment() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 멤버 셋에게 목록 순서와 무관한 칩을 배정했다(기본 픽스처가 그렇다)

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 인덱스가 아니라 배정된 값을 쓴다 — 멤버가 빠져도 남은 사람 색이 안 밀린다
        assertEquals(
            listOf(
                YGColorChipType.NametagChip8,
                YGColorChipType.NametagChip3,
                YGColorChipType.NametagChip11,
            ),
            viewModel.state.value.members.map { it.colorChipType },
        )
    }

    @Test
    fun detailArrives_missingChipFallsBackToDefault() = runTest(mainDispatcherRule.dispatcher) {
        // Given 칩을 알 수 없는 멤버가 섞여 있다 — 계약상 오지 않지만 타입은 널 허용이다
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(
            detailOf(members = listOf(ParfaitGroupMemberVO(MemberId(1L), GroupNickname(MY_NICKNAME), null))),
        )

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 아무 색이나 돌리지 않고 중립 칩으로 그린다
        assertEquals(
            YGColorChipType.Default,
            viewModel.state.value.members.single().colorChipType,
        )
    }

    @Test
    fun detailArrives_remainingCountIsLimitMinusMembers() = runTest(mainDispatcherRule.dispatcher) {
        // Given 정원 12 인 그룹에 멤버가 셋 있다(기본 픽스처)

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 남은 자리를 실제로 센다 — 그전엔 고정 1 이었다
        assertEquals(9, viewModel.state.value.remainingCount)
    }

    @Test
    fun detailArrives_moreMembersThanLimit_clampsRemainingToZero() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시와 서버가 어긋나 멤버가 정원보다 많다
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(detailOf(memberLimit = 1))

        // When 상세가 도착한 화면
        val viewModel = viewModel()

        // Then 음수를 내보내지 않는다 — "-1명 남음"은 읽는 사람에게 뜻이 없다
        assertEquals(0, viewModel.state.value.remainingCount)
    }
```

import를 바꾼다 — `com.teamyg.parfait.domain.model.group.GroupDetailVO`를 지우고
`com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType`,
`com.teamyg.parfait.domain.model.group.NametagChipType`,
`com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO`를 더한다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest`
Expected: FAIL — 컴파일 에러. `GroupDetailVO` 미해결, `withDetail`이 `GroupDetailVO`를 받고, `remainingCount`가 상수다.

- [ ] **Step 3: ViewModel을 고친다**

`withDetail`의 파라미터 타입과 필드명을 바꾸고 `remainingCount`를 계산으로 만든다.

```kotlin
    private fun GroupSettingUiState.withDetail(
        detail: ParfaitGroupDetailVO,
        myMemberId: MemberId?,
    ): GroupSettingUiState = copy(
        groupName = detail.groupName,
        myNickname = detail.groupNickname,
        nicknameInput = if (isEditing) nicknameInput else detail.groupNickname.value,
        inviteCode = detail.inviteCode,
        // 캐시와 서버가 어긋나 멤버가 정원을 넘으면 음수가 난다 — 0 아래로는 뜻이 없다
        remainingCount = (detail.memberLimit - detail.members.size).coerceAtLeast(0),
        members = detail.members.toUiModels(myMemberId),
    )
```

칩 배정을 서버 값으로 바꾼다.

```kotlin
    /**
     * [myMemberId] 를 모르면 아무도 나로 표시되지 않는다 — 그룹 닉네임은 중복될 수 있어
     * 이름으로 나를 찾으면 남을 나로 표시할 수 있다.
     */
    private fun List<ParfaitGroupMemberVO>.toUiModels(myMemberId: MemberId?): List<GroupMemberUiModel> =
        map { member ->
            GroupMemberUiModel(
                id = member.memberId.value,
                nickname = member.groupNickname.value,
                colorChipType = member.nametagChip.toColorChipType(),
                isMe = member.memberId == myMemberId,
            )
        }
```

파일 아래쪽 `NAMETAG_CHIP_TYPES` 선언을 지우고 변환 함수로 바꾼다. `MOCK_REMAINING_COUNT` 선언도 지운다. `GroupSettingUiState.remainingCount`의 기본값은 `0`으로, 위의 TODO 주석은 지운다.

```kotlin
/**
 * 서버가 배정한 칩을 화면 색으로 옮긴다.
 *
 * 값이 없거나 반납된 자리는 [YGColorChipType.Default] 다 — 색이 "그룹 안의 이 사람"을 가리키는
 * 신호라, 가리킬 사람이 없을 때 아무 색이나 돌리면 신호가 거짓이 된다.
 */
private fun NametagChipType?.toColorChipType(): YGColorChipType = when (this) {
    NametagChipType.TYPE1 -> YGColorChipType.NametagChip1
    NametagChipType.TYPE2 -> YGColorChipType.NametagChip2
    NametagChipType.TYPE3 -> YGColorChipType.NametagChip3
    NametagChipType.TYPE4 -> YGColorChipType.NametagChip4
    NametagChipType.TYPE5 -> YGColorChipType.NametagChip5
    NametagChipType.TYPE6 -> YGColorChipType.NametagChip6
    NametagChipType.TYPE7 -> YGColorChipType.NametagChip7
    NametagChipType.TYPE8 -> YGColorChipType.NametagChip8
    NametagChipType.TYPE9 -> YGColorChipType.NametagChip9
    NametagChipType.TYPE10 -> YGColorChipType.NametagChip10
    NametagChipType.TYPE11 -> YGColorChipType.NametagChip11
    NametagChipType.TYPE12 -> YGColorChipType.NametagChip12
    NametagChipType.RELEASED, null -> YGColorChipType.Default
}
```

import를 정리한다 — `GroupDetailVO` 제거, `ParfaitGroupDetailVO`·`NametagChipType` 추가. `YGColorChipType` import는 그대로 쓴다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest`
Expected: PASS. 기존 테스트가 `GroupDetailVO(...)`를 세우던 자리는 Step 1에서 `detailOf(...)`로 옮겼으므로 함께 통과한다.

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/setting/impl/src/
git commit -m "feat(group-setting): use the server nametag chip and real remaining seats"
```

---

### Task 7: G-001 그룹 목록이 마지막 토퍼의 칩을 쓴다

**Files:**
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListScreen.kt`
- Test: `feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GrouptagChipTypeTest.kt` (create)

**Interfaces:**
- Consumes: `NametagChipType`(Task 2) · `MyParfaitGroupVO.lastPlacedByNametagChip`(Task 2) · `YGGrouptagChipType.DEFAULT`(Task 4)
- Produces: 없음(화면 종단)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

같은 디렉토리의 `ToppingImageTest`가 화면 파일의 internal 변환 함수를 직접 테스트하는 선례다. 그 관용구를 따른다.

```kotlin
package com.teamyg.parfait.feature.groups.list.impl.route

import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.domain.model.group.NametagChipType
import kotlin.test.Test
import kotlin.test.assertEquals

class GrouptagChipTypeTest {
    @Test
    fun toGrouptagChipType_pairsTwelveNametagTypesIntoSix() {
        // Given Grouptag-Chip 은 Nametag 타입을 둘씩 묶은 6종이다
        val pairs = listOf(
            NametagChipType.TYPE1 to YGGrouptagChipType.TYPE_1_2,
            NametagChipType.TYPE2 to YGGrouptagChipType.TYPE_1_2,
            NametagChipType.TYPE3 to YGGrouptagChipType.TYPE_3_4,
            NametagChipType.TYPE4 to YGGrouptagChipType.TYPE_3_4,
            NametagChipType.TYPE5 to YGGrouptagChipType.TYPE_5_6,
            NametagChipType.TYPE6 to YGGrouptagChipType.TYPE_5_6,
            NametagChipType.TYPE7 to YGGrouptagChipType.TYPE_7_8,
            NametagChipType.TYPE8 to YGGrouptagChipType.TYPE_7_8,
            NametagChipType.TYPE9 to YGGrouptagChipType.TYPE_9_10,
            NametagChipType.TYPE10 to YGGrouptagChipType.TYPE_9_10,
            NametagChipType.TYPE11 to YGGrouptagChipType.TYPE_11_12,
            NametagChipType.TYPE12 to YGGrouptagChipType.TYPE_11_12,
        )

        // When/Then 짝이 그대로 맞는다
        pairs.forEach { (nametag, grouptag) ->
            assertEquals(grouptag, nametag.toGrouptagChipType())
        }
    }

    @Test
    fun toGrouptagChipType_releasedFallsBackToDefault() {
        // Given 마지막 토퍼가 그룹을 나갔다
        // When/Then 나간 사람 색을 계속 쓰지 않고 중립으로 간다
        assertEquals(YGGrouptagChipType.DEFAULT, NametagChipType.RELEASED.toGrouptagChipType())
    }

    @Test
    fun toGrouptagChipType_missingFallsBackToDefault() {
        // Given 아직 아무도 토핑을 올리지 않아 칩이 없다
        val missing: NametagChipType? = null

        // When/Then 목록 순서로 아무 색이나 돌리지 않는다
        assertEquals(YGGrouptagChipType.DEFAULT, missing.toGrouptagChipType())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.list.impl.route.GrouptagChipTypeTest"`
Expected: FAIL — 컴파일 에러, `toGrouptagChipType` 미해결.

- [ ] **Step 3: 변환 함수를 만들고 화면을 고친다**

`GroupListScreen.kt`에서 `CHIP_TYPES` 선언과 그 위 `TODO(칩 컬러)` 주석을 지우고, `toToppingImage` 옆에 변환 함수를 둔다(같은 파일의 `internal` 변환 관용구를 따른다).

```kotlin
/**
 * 마지막으로 그룹을 바꾼 사람의 칩을 Grouptag-Chip 색으로 옮긴다.
 *
 * Grouptag-Chip 6종은 Nametag 12종을 둘씩 묶은 타입이라 짝이 정해져 있다. 짝을 `ordinal`
 * 산술로 내지 않는 이유는 [NametagChipType.RELEASED] 가 그 범위 밖이어서다 — 분기로 갈라 둔다.
 *
 * 가리킬 사람이 없으면([NametagChipType.RELEASED] · `null`) 중립 색이다. 목록 순서로 돌리면
 * 그룹이 하나 빠질 때마다 남은 카드의 색이 밀린다.
 */
internal fun NametagChipType?.toGrouptagChipType(): YGGrouptagChipType = when (this) {
    NametagChipType.TYPE1, NametagChipType.TYPE2 -> YGGrouptagChipType.TYPE_1_2
    NametagChipType.TYPE3, NametagChipType.TYPE4 -> YGGrouptagChipType.TYPE_3_4
    NametagChipType.TYPE5, NametagChipType.TYPE6 -> YGGrouptagChipType.TYPE_5_6
    NametagChipType.TYPE7, NametagChipType.TYPE8 -> YGGrouptagChipType.TYPE_7_8
    NametagChipType.TYPE9, NametagChipType.TYPE10 -> YGGrouptagChipType.TYPE_9_10
    NametagChipType.TYPE11, NametagChipType.TYPE12 -> YGGrouptagChipType.TYPE_11_12
    NametagChipType.RELEASED, null -> YGGrouptagChipType.DEFAULT
}
```

`YGToppingGroup` 호출의 `chipType` 인자를 바꾼다.

```kotlin
                    chipType = group.lastPlacedByNametagChip.toGrouptagChipType(),
```

import에 `com.teamyg.parfait.domain.model.group.NametagChipType`을 더한다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest`
Expected: PASS. 같은 파일 아래 `GroupListScreenPreviewParameterProvider`가 `MyParfaitGroupVO(...)`를 세우므로 `lastPlacedByNametagChip` 인자를 채워야 컴파일된다 — 서로 다른 값을 주어 프리뷰에서 색이 갈리는 것을 보이게 한다.

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/list/impl/src/
git commit -m "feat(group-list): color the group chip by the last topper's nametag chip"
```

---

### Task 8: 전체 검증

**Files:** 없음(검증만)

- [ ] **Step 1: 전체 유닛 테스트를 돌린다**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. 깨지는 것이 있으면 새 필드를 세우지 않은 픽스처이므로 그 파일만 고친다.

- [ ] **Step 2: 정적 검사를 돌린다**

Run: `./gradlew ktlintCheck detekt`
Expected: BUILD SUCCESSFUL. 실패하면 지적된 자리만 고친다.

- [ ] **Step 3: 조립을 확인한다**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 남은 mock·TODO가 걷혔는지 확인한다**

```bash
git grep -n "MOCK_REMAINING_COUNT\|NAMETAG_CHIP_TYPES\|서버가 타입을 주면\|서버 응답 확장 대기\|TODO(칩 컬러)"
```
Expected: `feature/groups/canvas/impl`의 `NAMETAG_CHIP_PALETTE`만 남는다 — 서버가 `groupMembers`에 칩을 주지 않아 이 라운드의 범위 밖이다. 다른 자리가 남으면 걷는다.

- [ ] **Step 5: 커밋할 것이 남았으면 커밋**

```bash
git status --short
```
비어 있으면 아무것도 하지 않는다.
