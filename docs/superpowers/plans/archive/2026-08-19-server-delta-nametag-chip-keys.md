# 서버 delta 57529ec 반영 Implementation Plan

> ✅ **완료·develop 머지(PR #310 `feature/#300-sync-backend-api-250819` → `750cc2dd`, 2026-08-20)** —
> 7 Task 전량이 develop에 있다. 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> 스택 셋(#307 → #308 → #310)이 순서대로 머지돼 develop HEAD가 이 브랜치 팁과 같아졌고, 머지
> 커밋에 충돌 해소 편집이 0건이다. **머지 전 코드리뷰가 Task 3의 결론을 한 번 더 뒤집었다** —
> 모르는 칩 문자열과 값 없음을 모두 `NametagChipType.DEFAULT`로 접어 이 축의 널 허용을 없앴고
> (매퍼 · VO 셋 · 색 변환 셋), 근거는 [ADR-0024](../../../adr/0024-nametag-chip-unknown-fold.md)에 있다.
> 같은 리뷰가 `:data`의 칩 매퍼 두 사본을 `source/common/mapper`의 `internal` 하나로 합쳤다.
> 유닛 532 → 538건.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 `57529ec`가 바꾼 응답 JSON 키·필드·값 이름을 앱에 반영해, 직전 라운드가 붙인 Nametag-Chip 결선을 실제로 동작시키고 G-001 목록을 깨뜨리는 시각 파싱을 고친다.

**Architecture:** 변경은 `:data`의 wire DTO·매퍼에 집중된다. `:domain`은 enum 값 하나(`RELEASED` → `DEFAULT`)와 VO 필드 하나(`CanvasMemberVO.nametagChip`)만 늘고, feature는 C-001 상단 칩이 팔레트 인덱스에서 서버 값으로 바뀐다. 키 정정은 동작이 아니라 이름만 바꾸므로 **컴파일과 기존 테스트가 검증 수단**이고, 그 한계는 스펙이 명시적으로 감수한 것이다.

**Tech Stack:** Kotlin, kotlinx-serialization, kotlinx-datetime 0.8.0(`kotlin.time.Instant`), Hilt, MockK, kotlin.test, Gradle

**Spec:** [parfait/specs/2026-08-19-server-delta-nametag-chip-keys.md](../../specs/archive/2026-08-19-server-delta-nametag-chip-keys.md)

## Global Constraints

- **작업 브랜치는 `feature/#300-sync-backend-api-250819`** (TJYG-Android 저장소, 이미 체크아웃돼 있다). 새 브랜치를 만들지 않는다.
- **TJYG-Android 저장소는 기본적으로 커밋하지 않는다.** 단 이 브랜치는 사용자 소유 PR 브랜치이고 직전 라운드가 태스크마다 커밋하는 방식으로 진행됐으므로 **이번에도 태스크마다 커밋한다.** push·PR은 하지 않는다.
- **`:domain`은 Kotlin JVM 모듈이다** — 테스트 태스크는 `:domain:test`다. `:domain:testDebugUnitTest`는 존재하지 않는다(직전 계획의 오기).
- **이 저장소에 detekt는 없다.** 린트는 `./gradlew ktlintCheck` 하나다.
- **매퍼 단독 테스트를 만들지 않는다**(저장소 규약). 판단이 든 변환은 DataSource 테스트 케이스로 잠근다.
- **파르페 문서 규율**: 라인번호·변동 수치·색 hex를 문서에 적지 않는다. 근거는 파일명 + 심볼명.
- **개명은 `:data`의 wire DTO에서 멈춘다.** `:domain`의 `NametagChipType`·`MyParfaitGroupVO.lastPlacedByNametagChip`·`ParfaitGroupMemberVO.nametagChip` 표기는 그대로 둔다(도메인은 거울이 아니라 제품 언어이고 매퍼가 번역 지점이다).
- **DTO의 널 허용(`String? = null`)을 비널로 좁히지 않는다.** 구버전 서버를 만나도 화면이 통째로 실패하지 않게 하려는 결정이다.
- 앱 저장소의 로컬 절대경로는 개인정보라 `wiki/personal-private/project-paths.md`의 `TJYG-Android` 행을 본다. 아래 파일 경로는 전부 그 저장소 루트 기준이다.

---

### Task 1: 업로드 시각 파싱 복구 + 매퍼 단독 테스트 삭제

지금 G-001 목록이 **그룹이 하나라도 있으면 통째로 실패한다.** 서버가 오프셋 없는 로컬 날짜시각을 주는데 매퍼가 `Instant::parse`(오프셋 필수)로 읽고, 서버가 이 필드를 `COALESCE`로 비널화하면서 "값이 `null`이면 파싱을 건너뛴다"는 마지막 우회로가 사라졌다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt`
- Delete: `data/src/test/java/com/teamyg/parfait/data/source/group/mapper/MyParfaitGroupVOMapperTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `PARFAIT_TIME_ZONE`(`domain/src/main/java/com/teamyg/parfait/domain/model/ParfaitDay.kt`의 `val PARFAIT_TIME_ZONE: TimeZone`)
- Produces: `MyParfaitGroupVO.recentImageUploadedAt`가 계속 `kotlin.time.Instant?`다 — 타입은 안 바뀌므로 뒤 태스크·화면이 영향받지 않는다

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ParfaitGroupRemoteDataSourceImplTest.kt`의 `groupResponse` 픽스처가 `recentImageUploadedAt`을 못 받으므로 먼저 파라미터를 연다. 기존 호출부 넷은 기본값으로 그대로 컴파일된다.

```kotlin
    private fun groupResponse(
        lastPlacedByNametagChip: String?,
        recentImageUploadedAt: String? = null,
    ) = MyParfaitGroupResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        recentImageUrl = null,
        recentImageUploadedAt = recentImageUploadedAt,
        lastPlacedByNametagChip = lastPlacedByNametagChip,
    )
```

같은 파일에 테스트 둘을 추가한다.

```kotlin
    @Test
    fun getMyGroups_offsetlessUploadedAt_isReadAsSeoulWallClock() = runTest {
        // Given 서버가 오프셋 없는 로컬 날짜시각을 준다 — 그 벽시계는 Asia/Seoul 기준이다
        coEvery { parfaitGroupService.getParfaitGroups() } returns
            success(listOf(groupResponse(null, recentImageUploadedAt = "2026-08-01T12:00:00")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then KST 정오는 UTC 오전 3시와 같은 시점이다
        assertEquals(
            Instant.parse("2026-08-01T03:00:00Z"),
            result.getOrNull()?.single()?.recentImageUploadedAt,
        )
    }

    @Test
    fun getMyGroups_uploadedAtAcrossMidnight_staysOnItsOwnSeoulDay() = runTest {
        // Given 자정 직후 값이다 — UTC 로 읽으면 전날로 밀린다
        coEvery { parfaitGroupService.getParfaitGroups() } returns
            success(listOf(groupResponse(null, recentImageUploadedAt = "2026-08-02T00:30:00")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then 서울 벽시계 8월 2일 00:30 = UTC 8월 1일 15:30
        assertEquals(
            Instant.parse("2026-08-01T15:30:00Z"),
            result.getOrNull()?.single()?.recentImageUploadedAt,
        )
    }
```

import 한 줄을 파일 상단 import 블록에 추가한다(`runTest`·`coEvery`·`assertEquals`는 이미 있다).

```kotlin
import kotlin.time.Instant
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitGroupRemoteDataSourceImplTest*"`

Expected: FAIL. 두 신규 테스트가 `IllegalArgumentException`(또는 `DateTimeFormatException`) 계열로 깨진다 — `Instant.parse`가 오프셋 없는 문자열을 거부하고, 그 예외를 `ApiCaller`가 접어 `result`가 실패가 되므로 `getOrNull()`이 `null`이다.

- [ ] **Step 3: 매퍼를 고친다**

`data/.../source/group/mapper/VOMapper.kt`에서 `toMyParfaitGroupVO`의 시각 변환 한 줄과 그 위 주석을 바꾼다.

```kotlin
internal fun MyParfaitGroupResponse.toMyParfaitGroupVO(): MyParfaitGroupVO = MyParfaitGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    recentImageUrl = recentImageUrl,
    // 서버는 오프셋 없는 로컬 날짜시각을 준다. 그 벽시계가 Asia/Seoul 기준이라는 것이 계약 사실이다 —
    // 서버 DB 커넥션이 dev·local·prod 세 환경 전부 serverTimezone=Asia/Seoul 이고
    // hibernate.jdbc.time_zone 도 같다(api/parfait-group.md 타임존 절). 그래서 여기서 시간대를
    // 부여해 절대 시점으로 만든다 — 벽시계 숫자를 그대로 들면 기기 타임존에 따라 다른 시점이 된다.
    recentImageUploadedAt = recentImageUploadedAt
        ?.let(LocalDateTime::parse)
        ?.toInstant(PARFAIT_TIME_ZONE),
    lastPlacedByNametagChip = lastPlacedByNametagChip.toNametagChipType(),
)
```

import을 갈아 끼운다 — `kotlin.time.Instant`를 지우고 셋을 넣는다. **ktlint가 사전순을 요구하므로** `...domain.model.PARFAIT_TIME_ZONE`이 `...domain.model.group.*`보다 앞이다.

```kotlin
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitGroupRemoteDataSourceImplTest*"`

Expected: PASS (신규 2건 포함 7건)

- [ ] **Step 5: 매퍼 단독 테스트를 지운다**

`MyParfaitGroupVOMapperTest`는 **이 버그를 초록으로 지켜 온 파일**이다 — 오프셋이 붙은 문자열을 자기가 지어 넣고(`"...Z"`·`"...+09:00"`) 단언하는데, 그 Given 주석("서버가 UTC 오프셋(`Z`)을 붙여 보낸다")이 방금 고친 매퍼 주석과 같은 허구다. 고쳐서 살릴 수 없다 — 살리려면 다시 오프셋 있는 입력을 지어내야 하고 그것이 병의 본체다. 셋째 테스트가 검증하는 `null` 입력도 이제 계약상 발생하지 않는다.

```bash
git rm data/src/test/java/com/teamyg/parfait/data/source/group/mapper/MyParfaitGroupVOMapperTest.kt
```

커버리지는 Step 1에서 `ParfaitGroupRemoteDataSourceImplTest`로 옮겼다. 매퍼 단독 테스트를 만들지 않는 저장소 규약과 이로써 일관된다(이 파일이 그 규약의 마지막 예외였다).

- [ ] **Step 6: 모듈 테스트와 린트를 돌린다**

Run: `./gradlew :data:testDebugUnitTest ktlintCheck`

Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt \
        data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt
git commit -m "fix(group): read recentImageUploadedAt as a Seoul wall clock

서버는 오프셋 없는 로컬 날짜시각을 주는데 매퍼가 Instant::parse 로 읽고 있었다.
서버가 이 필드를 COALESCE 로 비널화하면서 '널이면 건너뛴다'는 우회로가 사라져
그룹이 하나라도 있으면 G-001 목록이 통째로 실패한다.

MyParfaitGroupVOMapperTest 는 오프셋 붙은 입력을 스스로 지어 넣어 이 버그를
초록으로 지켜 왔다. 삭제하고 커버리지를 DataSource 테스트로 옮겼다."
```

---

### Task 2: 칩 JSON 키 세 자리를 서버에 맞춘다

서버가 응답 DTO 경계에서만 `nametagChip` → `nameTagChip`, `lastPlacedByNametagChip` → `lastPlacedByNameTagChip`으로 바꿨다. 앱은 옛 키를 들고 있고 세 필드가 전부 `String? = null`이라 **예외 없이 값이 전부 `null`이 된다** — 직전 라운드가 붙인 칩 결선이 통째로 무효인데 화면은 그럴듯하게 뜬다.

⚠️ **이 태스크는 동작이 아니라 이름만 바꾸므로 새 테스트가 이 변경을 잡지 못한다.** 앱 테스트는 자기 DTO 객체를 자기가 만들어 넣으므로 `@SerialName` 문자열에 어떤 단언도 걸리지 않는다. 검증 수단은 **컴파일과 기존 테스트**이고, 이 한계는 스펙이 명시적으로 감수한 결정이다(와이어 계약 테스트를 붙이지 않기로 했다).

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/MyParfaitGroupResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/ParfaitGroupMemberResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt`

**Interfaces:**
- Produces: DTO 프로퍼티명 `MyParfaitGroupResponse.lastPlacedByNameTagChip`·`ParfaitGroupMemberResponse.nameTagChip`·`PlacedByResponse.nameTagChip`. 이후 태스크가 이 이름으로 참조한다.

**고칠 자리는 정확히 셋이다.** 세 번째(캔버스 `GetTodayParfaitResponse.kt` 안의 `PlacedByResponse`)를 빠뜨리기 쉽다. 앱 `CreateParfaitGroupResponse`에는 아직 이 필드가 없으므로(Task 6이 새로 넣는다) 여기 안 들어간다.

- [ ] **Step 1: 그룹 목록 DTO를 고친다**

`MyParfaitGroupResponse.kt` — 프로퍼티명과 `@SerialName`을 **둘 다** 바꾸고 KDoc의 반납 값 이름도 고친다.

```kotlin
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
    /** 마지막 토퍼가 이미 그룹을 나갔으면 `"DEFAULT"` 가 온다 */
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
)
```

- [ ] **Step 2: 그룹 상세 멤버 DTO를 고친다**

`ParfaitGroupMemberResponse.kt`

```kotlin
@Serializable
data class ParfaitGroupMemberResponse(
    @SerialName("memberId")
    val memberId: Long,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
```

- [ ] **Step 3: 캔버스 `PlacedByResponse`를 고친다**

`GetTodayParfaitResponse.kt` 맨 아래 `PlacedByResponse`. **클래스 이름은 그대로 두고**(서버가 이쪽은 안 바꿨다) 필드 키만 바꾼다. KDoc 두 문장도 이번 delta로 거짓이 됐으므로 함께 고친다.

```kotlin
/**
 * 배치자. 같은 이름의 DTO 가 response/parfaitimage 에도 있었으나 서버가 그쪽을
 * PlaceParfaitImagePlacedByResponse 로 개명했다(springdoc 이 두 스키마를 같은 것으로 취급해
 * 이쪽에 추가한 칩이 스웨거에 안 보이던 문제 때문이다). 이 클래스는 서버가 이름을 안 바꿨다.
 *
 * @param nickname 그룹 닉네임이다. 탈퇴·이탈한 멤버면 "(알수없음)"이 온다.
 * @param nameTagChip 그 사람의 칩. 탈퇴했으면 `"DEFAULT"` 다. **아직 도메인으로 올리지 않는다** —
 *  서버는 이제 배치 확정 응답에도 이 값을 주므로 [com.teamyg.parfait.domain.model.topping.ToppingPlacerVO]
 *  를 채울 수 있게 됐지만, `placedBy` 를 읽는 화면이 0건이다. C-202 Spotlight 는 이 값이 아니라
 *  groupMembers 를 GroupMemberId 로 조인해 찾으므로 이 보류에 물리지 않는다.
 */
@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
```

- [ ] **Step 4: 매퍼 참조를 고친다**

`data/.../source/group/mapper/VOMapper.kt` — 도메인 쪽 이름은 그대로이고 **DTO 쪽 이름만** 바뀐다. 두 줄이다.

```kotlin
    lastPlacedByNametagChip = lastPlacedByNameTagChip.toNametagChipType(),
```

```kotlin
internal fun ParfaitGroupMemberResponse.toParfaitGroupMemberVO(): ParfaitGroupMemberVO = ParfaitGroupMemberVO(
    memberId = MemberId(memberId),
    groupNickname = GroupNickname(groupNickname),
    nametagChip = nameTagChip.toNametagChipType(),
)
```

- [ ] **Step 5: 테스트 픽스처의 명명 인자를 고친다**

`ParfaitGroupRemoteDataSourceImplTest.kt`의 픽스처 둘이 명명 인자로 옛 이름을 쓴다. 파라미터 이름(테스트 안의 지역 이름)은 그대로 두고 **DTO에 넘기는 인자 이름만** 바꾼다.

```kotlin
    private fun groupResponse(
        lastPlacedByNametagChip: String?,
        recentImageUploadedAt: String? = null,
    ) = MyParfaitGroupResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        recentImageUrl = null,
        recentImageUploadedAt = recentImageUploadedAt,
        lastPlacedByNameTagChip = lastPlacedByNametagChip,
    )
```

```kotlin
            ParfaitGroupMemberResponse(
                memberId = 42L,
                groupNickname = "모카",
                nameTagChip = memberChip,
            ),
```

- [ ] **Step 6: 컴파일과 테스트로 검증한다**

Run: `./gradlew :data:testDebugUnitTest ktlintCheck`

Expected: PASS. 이 태스크의 실질 검증은 **컴파일**이다 — 세 DTO 중 하나라도 프로퍼티명을 안 바꾸면 매퍼·픽스처가 안 붙는다. 기존 칩 테스트 넷이 계속 통과하는 것은 매핑 로직이 안 바뀌었음을 뜻한다.

- [ ] **Step 7: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/model/response/group/MyParfaitGroupResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/group/ParfaitGroupMemberResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt \
        data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt
git commit -m "fix(data): follow the server rename to nameTagChip keys

서버가 HTTP 응답 DTO 경계에서만 nametagChip -> nameTagChip,
lastPlacedByNametagChip -> lastPlacedByNameTagChip 으로 바꿨다. 앱은 옛 키를
들고 있었고 세 필드가 전부 널 허용이라 예외 없이 값이 전부 null 이 됐다 —
직전 라운드의 칩 결선이 통째로 무효인 채 화면만 그럴듯했다.

개명은 :data 의 wire DTO 에서 멈춘다. 도메인은 거울이 아니라 제품 언어이고
매퍼가 번역 지점이다."
```

---

### Task 3: 반납 값 `RELEASED`를 `DEFAULT`로 바꾼다

서버가 마이그레이션 V15로 반납 값 이름을 바꿨다(`TYPE1`~`TYPE12`와 달리 **유일성 제약이 없어 한 그룹에 여럿이 동시에 가질 수 있다**). 지금 결과가 맞는 것은 우연이 두 겹이다 — 키가 어긋나 매퍼에 도달조차 못 하고(Task 2가 그것을 고쳤다), 도달해도 `"DEFAULT"`가 "모르는 문자열" 갈래로 빠져 `null`이 되며, 두 경우 다 화면 표현이 반납 값과 같아 안 드러난다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/NametagChipType.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/MyParfaitGroupVO.kt` (KDoc 링크만)
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupMemberVO.kt` (KDoc 링크만)
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/util/ColorChipType.kt`
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/GrouptagChipType.kt`
- Test: `feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/util/ColorChipTypeTest.kt`
- Test: `feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/GrouptagChipTypeTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: Task 2가 고친 DTO 프로퍼티명
- Produces: `NametagChipType.DEFAULT`(`RELEASED`는 사라진다). Task 5의 canvas util이 이 값을 분기한다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

세 테스트 파일에서 `RELEASED`를 `DEFAULT`로 바꾼다. enum 상수가 아직 없으므로 **컴파일이 깨진다** — 그것이 이 태스크의 red다.

`ColorChipTypeTest.kt`

```kotlin
    @Test
    fun toColorChipType_defaultFallsBackToDefault() {
        // Given 마지막 토퍼가 그룹을 나가 자리가 반납됐다
        // When/Then 나간 사람 색을 계속 쓰지 않고 중립으로 간다
        assertEquals(YGColorChipType.Default, NametagChipType.DEFAULT.toColorChipType())
    }
```

`GrouptagChipTypeTest.kt` — 테스트 이름까지 함께 바꾼다.

```kotlin
    @Test
    fun toGrouptagChipType_defaultFallsBackToDefault() {
        // Given 마지막 토퍼가 그룹을 나갔다
        // When/Then 나간 사람 색을 계속 쓰지 않고 중립으로 간다
        assertEquals(YGGrouptagChipType.DEFAULT, NametagChipType.DEFAULT.toGrouptagChipType())
    }
```

`ParfaitGroupRemoteDataSourceImplTest.kt`

```kotlin
    @Test
    fun getMyGroups_defaultChip_isKeptNotFolded() = runTest {
        // Given 마지막 토퍼가 그룹을 나가 서버가 반납 표식을 준다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("DEFAULT")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then null 로 접지 않는다 — "나간 사람"과 "값이 없다"는 뜻이 다르다
        assertEquals(NametagChipType.DEFAULT, result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }
```

(`getMyGroups_releasedChip_isKeptNotFolded`를 이것으로 대체한다.)

- [ ] **Step 2: 컴파일이 깨지는 것을 확인한다**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest`

Expected: FAIL — `Unresolved reference: DEFAULT`

- [ ] **Step 3: 도메인 enum을 고친다**

`NametagChipType.kt` — 값 이름과 KDoc 두 자리를 바꾸고, 서버가 이번에 명시한 성질을 싣는다.

```kotlin
/**
 * 그룹 안에서 사람을 가리키는 칩 타입. 배정 주체는 **서버**다 — 참여·생성 시 그 그룹의 활동
 * 멤버가 안 쓰는 값 중 하나를 받고, 그룹을 나가면 [DEFAULT] 로 반납된다. 다시 뽑는 경로는 없다.
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
     * 그룹을 나간 사람이 반납한 자리. 12종과 달리 **유일성 제약이 없어** 한 그룹 안에서 여럿이
     * 동시에 가질 수 있다(서버 NameTagChipType 이 이 성질을 명시한다).
     *
     * "값이 없다"(`null`)와 뜻이 다르다 — 지금은 화면 표현이 같지만 계약이 갈라 주는 것을
     * 매퍼가 뭉개면 되돌릴 수 없다.
     */
    DEFAULT,
}
```

- [ ] **Step 4: 두 변환의 분기를 고친다**

`setting/impl/util/ColorChipType.kt`의 마지막 arm과 `list/impl/util/GrouptagChipType.kt`의 마지막 arm.

```kotlin
    NametagChipType.DEFAULT, null -> YGColorChipType.Default
```

```kotlin
    NametagChipType.DEFAULT, null -> YGGrouptagChipType.DEFAULT
```

`GrouptagChipType.kt`의 KDoc이 `[NametagChipType.RELEASED]`를 두 번 가리키므로 그것도 `[NametagChipType.DEFAULT]`로 바꾼다.

- [ ] **Step 5: 매퍼 KDoc의 예시 문자열을 고친다**

`data/.../source/group/mapper/VOMapper.kt`의 `toNametagChipType` KDoc이 `"RELEASED"` 를 예로 든다.

```kotlin
/**
 * 서버가 주는 칩 이름을 도메인 값으로 바꾼다.
 *
 * 열린 입력이라 모르는 문자열은 `null` 로 접는다 — 새 타입이 서버에 먼저 들어와도 목록 조회가
 * 통째로 실패하지 않아야 한다. `"DEFAULT"` 는 접지 않고 그대로 남긴다.
 */
```

- [ ] **Step 6: 도메인 VO 두 곳의 KDoc 링크를 바꾼다**

`[NametagChipType.RELEASED]`가 두 VO의 KDoc에 남아 있다. **KDoc 링크라 컴파일은 안 깨지지만 Step 8의 게이트가 반드시 걸린다.** 여기서는 **링크 이름만** 바꾼다 — 이 두 KDoc의 서술 자체가 이번 delta로 거짓이 된 것은 Task 7이 문장째 다시 쓴다.

`MyParfaitGroupVO.kt`

```kotlin
     * 그 사람이 이미 그룹을 나갔으면 [NametagChipType.DEFAULT], 토핑이 하나도 없으면
```

`ParfaitGroupMemberVO.kt`

```kotlin
     * 상세 응답은 탈퇴자를 빼고 주므로 실제로는 [NametagChipType.DEFAULT] 도 `null` 도
```

- [ ] **Step 7: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :domain:test :data:testDebugUnitTest :feature:groups:setting:impl:testDebugUnitTest :feature:groups:list:impl:testDebugUnitTest`

Expected: PASS

- [ ] **Step 8: 남은 `RELEASED` 참조가 없는지 확인한다**

Run: `git grep -n "RELEASED"`

Expected: 결과 0건. 하나라도 남으면 그 파일을 고치고 **Files 목록에 없던 파일이면 아래 `git add`에도 더한 뒤** Step 7을 다시 돌린다.

- [ ] **Step 9: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/group/NametagChipType.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/group/MyParfaitGroupVO.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupMemberVO.kt \
        data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt \
        data/src/test/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImplTest.kt \
        feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/util/ColorChipType.kt \
        feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/util/ColorChipTypeTest.kt \
        feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/GrouptagChipType.kt \
        feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/GrouptagChipTypeTest.kt
git commit -m "refactor(group): rename the returned chip slot to DEFAULT

서버가 마이그레이션 V15 로 반납 값 이름을 RELEASED 에서 DEFAULT 로 바꿨다.
enum·KDoc·두 변환 분기가 존재하지 않는 계약 값을 가리키고 있었다.

12종과 달리 유일성 제약이 없다는 성질도 KDoc 에 실었다."
```

---

### Task 4: 캔버스 `groupMembers`의 칩을 도메인까지 올린다

서버가 `groupMembers[]`에 `nameTagChip`을 실었다 — 직전 라운드가 C-001 상단 칩을 범위 밖으로 둔 **유일한 사유**가 사라졌다. 여기서는 계약을 VO까지 올리고, 화면 결선은 Task 5가 한다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasMemberVO.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt`
- Modify (픽스처): `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt` — `CanvasMemberVO`에 필수 파라미터가 늘어 이 파일의 `member()` 헬퍼가 깨진다(Step 5가 처리)

**Interfaces:**
- Consumes: `NametagChipType.DEFAULT`(Task 3), `toNametagChipType()`은 group 매퍼의 **private** 함수라 여기서 쓸 수 없다 — 아래 Step 3이 parfait 매퍼에 자기 것을 둔다
- Produces: `CanvasMemberVO.nametagChip: NametagChipType?`. Task 5가 이 값을 읽는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ParfaitRemoteDataSourceImplTest.kt`의 `todaySuccess` 픽스처가 `groupMembers`를 리터럴로 들고 있다. 칩을 파라미터로 열어 기본값을 준다(기존 호출부는 전부 명명 인자라 그대로 컴파일된다).

```kotlin
    private fun todaySuccess(
        status: String = "ACTIVE",
        lastClosedDate: String? = "2026-08-14",
        background: BackgroundResponse? = BackgroundResponse(type = "COLOR", value = "#FFEEDD"),
        images: List<TodayParfaitImageResponse>? = listOf(toppingResponse()),
        memberChip: String? = "TYPE6",
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = GetTodayParfaitResponse(
            parfaitId = 100L,
            date = "2026-08-15",
            status = status,
            lastClosedDate = lastClosedDate,
            groupMembers = listOf(
                GroupMemberResponse(id = 5L, nickname = "행복한 판다", nameTagChip = memberChip),
            ),
            background = background,
            images = images,
        ),
    )
```

> ⚠️ 위 픽스처의 `status`·`lastClosedDate` 기본값은 **파일에 이미 있는 값을 그대로 옮긴 것**이다. 파라미터 목록 앞부분을 바꾸지 말고 `memberChip` 하나만 더한다.

같은 파일에 테스트 둘을 추가한다.

```kotlin
    @Test
    fun getTodayCanvas_memberChip_becomesThatType() = runTest {
        // Given 서버가 그룹 멤버마다 배정된 칩을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess()

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 도메인 enum 으로 넘어온다 — 상단 멤버 칩을 계약으로 그릴 수 있다
        assertEquals(NametagChipType.TYPE6, canvas.members.single().nametagChip)
    }

    @Test
    fun getTodayCanvas_unknownMemberChip_foldsToNull() = runTest {
        // Given 서버가 앱이 모르는 값을 준다 — 열린 입력이다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(memberChip = "TYPE99")

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 던지지 않고 null 로 접는다 — 모르는 색은 그리지 못할 뿐이다
        assertNull(canvas.members.single().nametagChip)
    }
```

import을 추가한다(`assertNull`은 이 파일에 이미 있다).

```kotlin
import com.teamyg.parfait.domain.model.group.NametagChipType
```

- [ ] **Step 2: 컴파일이 깨지는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitRemoteDataSourceImplTest*"`

Expected: FAIL — `GroupMemberResponse`에 `nameTagChip` 파라미터가 없고 `CanvasMemberVO`에 `nametagChip` 프로퍼티가 없다

- [ ] **Step 3: DTO·VO·매퍼를 고친다**

`GetTodayParfaitResponse.kt`의 `GroupMemberResponse`

```kotlin
/**
 * @param id 계정 id 가 아니라 그룹 멤버십 행 id 다.
 * @param nameTagChip 서버가 그 그룹 안에서 배정한 칩. 탈퇴자는 이 목록에서 빠지므로 `"DEFAULT"` 는
 *  오지 않는다.
 */
@Serializable
data class GroupMemberResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
```

`CanvasMemberVO.kt`

```kotlin
data class CanvasMemberVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
    /** 서버가 배정한 칩. 모르는 값이면 `null` 이고 화면은 중립 색으로 떨어뜨린다. */
    val nametagChip: NametagChipType?,
)
```

import을 추가한다: `import com.teamyg.parfait.domain.model.group.NametagChipType`

`data/.../source/parfait/mapper/VOMapper.kt` — 변환 함수를 이 파일에 **새로 추가**한다. group 매퍼의 같은 함수는 `private`이라 재사용할 수 없고, 그것을 `internal`로 넓히면 두 도메인 매퍼가 서로를 보게 된다.

```kotlin
/**
 * 서버가 주는 칩 이름을 도메인 값으로 바꾼다. 열린 입력이라 모르는 문자열은 `null` 로 접는다 —
 * 새 타입이 서버에 먼저 들어와도 캔버스 조회가 통째로 실패하지 않아야 한다.
 *
 * group 매퍼에도 같은 함수가 있다. 서로 private 이라 공유하지 않는다 — 두 도메인 매퍼가 서로를
 * 보게 만드는 것보다 낫다고 판단했다.
 */
private fun String?.toNametagChipType(): NametagChipType? =
    this?.let { raw -> NametagChipType.entries.firstOrNull { it.name == raw } }
```

⚠️ **`toCanvasMemberVO`는 이 파일에 이미 있다. 새로 선언하지 말고 본문에 한 줄을 더하라** — 그대로 붙여 넣으면 `Conflicting overloads`로 깨진다.

```kotlin
private fun GroupMemberResponse.toCanvasMemberVO(): CanvasMemberVO = CanvasMemberVO(
    groupMemberId = GroupMemberId(id),
    nickname = GroupNickname(nickname),
    nametagChip = nameTagChip.toNametagChipType(),
)
```

import을 추가한다: `import com.teamyg.parfait.domain.model.group.NametagChipType`

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest :domain:test`

Expected: PASS. `CanvasMemberVO`에 필수 파라미터가 늘었으므로 **다른 모듈의 픽스처가 깨질 수 있다** — 다음 스텝에서 처리한다.

- [ ] **Step 5: 깨진 픽스처를 모두 고친다**

Run: `git grep -ln "CanvasMemberVO"`

각 파일에서 `CanvasMemberVO(...)` 생성자 호출에 `nametagChip = null`을 더한다(테스트가 칩을 검증하지 않는 자리는 `null`이 맞다 — Task 5가 검증하는 자리만 값을 넣는다).

Run: `./gradlew testDebugUnitTest ktlintCheck`

Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasMemberVO.kt \
        data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt
git add -u
git commit -m "feat(canvas): carry the server nametag chip on group members

서버가 캔버스 응답의 groupMembers 에 칩을 실었다 — 직전 라운드가 C-001 상단
칩을 범위 밖으로 둔 유일한 사유가 사라졌다. 계약을 CanvasMemberVO 까지 올린다.

화면 결선은 다음 커밋."
```

---

### Task 5: C-001 상단 멤버 칩을 서버 값으로 그린다

`CanvasMainViewModel`이 목록 순서로 7종 팔레트를 돌고 있다 — 멤버가 나가면 뒤 순서가 밀려 남은 사람 색이 바뀌고, **같은 사람이 S-101과 C-001에서 다른 색**으로 보인다. 서버가 값을 주므로 팔레트라는 개념 자체가 사라진다(근거 없는 7종 문제도 함께 닫힌다).

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ColorChipType.kt`
- Create: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ColorChipTypeTest.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/util/ColorChipType.kt`
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/GrouptagChipType.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: `CanvasMemberVO.nametagChip`(Task 4), `NametagChipType.DEFAULT`(Task 3)
- Produces: `internal fun NametagChipType?.toColorChipType(): YGColorChipType` (canvas impl `util` 패키지)

- [ ] **Step 1: 변환 테스트를 쓴다**

새 파일 `feature/groups/canvas/impl/src/test/kotlin/.../util/ColorChipTypeTest.kt`. S-101의 같은 이름 테스트와 내용이 같다 — 규칙이 같아서다.

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.domain.model.group.NametagChipType
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorChipTypeTest {
    @Test
    fun toColorChipType_pairsAllTwelveNametagTypesOneToOne() {
        // Given Nametag 타입 12종은 각각 자기 자리의 색 칩 하나에만 대응한다
        val pairs = listOf(
            NametagChipType.TYPE1 to YGColorChipType.NametagChip1,
            NametagChipType.TYPE2 to YGColorChipType.NametagChip2,
            NametagChipType.TYPE3 to YGColorChipType.NametagChip3,
            NametagChipType.TYPE4 to YGColorChipType.NametagChip4,
            NametagChipType.TYPE5 to YGColorChipType.NametagChip5,
            NametagChipType.TYPE6 to YGColorChipType.NametagChip6,
            NametagChipType.TYPE7 to YGColorChipType.NametagChip7,
            NametagChipType.TYPE8 to YGColorChipType.NametagChip8,
            NametagChipType.TYPE9 to YGColorChipType.NametagChip9,
            NametagChipType.TYPE10 to YGColorChipType.NametagChip10,
            NametagChipType.TYPE11 to YGColorChipType.NametagChip11,
            NametagChipType.TYPE12 to YGColorChipType.NametagChip12,
        )

        // When/Then 짝이 그대로 맞는다
        pairs.forEach { (nametag, colorChip) ->
            assertEquals(colorChip, nametag.toColorChipType())
        }
    }

    @Test
    fun toColorChipType_defaultFallsBackToDefault() {
        // Given 반납된 자리다
        // When/Then 나간 사람 색을 계속 쓰지 않고 중립으로 간다
        assertEquals(YGColorChipType.Default, NametagChipType.DEFAULT.toColorChipType())
    }

    @Test
    fun toColorChipType_missingFallsBackToDefault() {
        // Given 서버가 모르는 값을 줘 매퍼가 접었다
        val missing: NametagChipType? = null

        // When/Then 아무 색이나 돌리지 않는다
        assertEquals(YGColorChipType.Default, missing.toColorChipType())
    }
}
```

- [ ] **Step 2: 컴파일이 깨지는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`

Expected: FAIL — `Unresolved reference: toColorChipType`

- [ ] **Step 3: 변환을 만든다**

새 파일 `feature/groups/canvas/impl/src/main/kotlin/.../util/ColorChipType.kt`.

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 서버가 배정한 칩을 화면 색으로 옮긴다.
 *
 * 값이 없거나 반납된 자리는 [YGColorChipType.Default] 다 — 색이 "그룹 안의 이 사람"을 가리키는
 * 신호라, 가리킬 사람이 없을 때 아무 색이나 돌리면 신호가 거짓이 된다.
 *
 * **같은 규칙의 변환이 저장소에 셋이다** — S-101 그룹 설정(12종 1:1, 이 파일과 글자까지 같다)과
 * G-001 목록(12종을 6종으로 짝지어 접는다). 공용화하지 않은 이유는 자리가 없어서가 아니라
 * (`core:ui` 가 `:domain` 을 이미 보고 `:core:designsystem` 을 더하면 된다) 그 모듈의
 * `implementation`/`api` 가시성이 팀 결정 대상으로 열려 있어서다.
 *
 * ⚠️ **컴파일러가 잡아 주는 것은 앱이 [NametagChipType] 에 상수를 더할 때의 arm 누락뿐이다.**
 * 서버에 새 타입이 생기면 매퍼가 모르는 문자열을 `null` 로 접으므로 컴파일은 안 깨지고,
 * 세 변환 중 하나에서 색만 바꾸는 것도 못 잡는다. 색을 고칠 때는 셋을 함께 본다.
 */
internal fun NametagChipType?.toColorChipType(): YGColorChipType = when (this) {
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
    NametagChipType.DEFAULT, null -> YGColorChipType.Default
}
```

- [ ] **Step 4: 변환 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ColorChipTypeTest*"`

Expected: PASS

- [ ] **Step 5: ViewModel 동작을 잠그는 테스트를 쓴다**

`CanvasMainViewModelTest.kt`. companion object의 `member(nickname)` 헬퍼가 칩을 못 받으므로 파라미터를 연다(기존 호출부는 기본값으로 그대로 컴파일된다).

```kotlin
        fun member(nickname: String, nametagChip: NametagChipType? = null) = CanvasMemberVO(
            groupMemberId = GroupMemberId(1L),
            nickname = GroupNickname(nickname),
            nametagChip = nametagChip,
        )
```

테스트 둘을 추가한다. 이 파일의 관용구는 `runTest(mainDispatcherRule.dispatcher)` + `enteredViewModel()` + `coEvery { getTodayParfait(any()) }`이고, mock 이름은 `getTodayParfait`다(`...UseCase`가 아니다).

```kotlin
    @Test
    fun enter_memberChips_followTheServerAssignedChip() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 두 멤버에게 12종 중 서로 다른 값을 배정했다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(
                TODAY_PARFAIT_ID,
                today,
                members = listOf(
                    member("모카", NametagChipType.TYPE7),
                    member("판다", NametagChipType.TYPE2),
                ),
            ),
        )

        // When 화면에 들어간다
        val viewModel = enteredViewModel()

        // Then 목록 순서가 아니라 배정된 값이 색을 정한다
        assertEquals(
            listOf(YGColorChipType.NametagChip7, YGColorChipType.NametagChip2),
            viewModel.state.value.memberChips.map(GroupMemberChip::colorChipType),
        )
    }

    @Test
    fun enter_memberChips_doNotShiftWhenAnEarlierMemberLeaves() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앞자리 멤버가 빠지고 뒤의 멤버만 남았다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(TODAY_PARFAIT_ID, today, members = listOf(member("판다", NametagChipType.TYPE2))),
        )

        // When 화면에 들어간다
        val viewModel = enteredViewModel()

        // Then 남은 사람 색이 밀리지 않는다 — 인덱스 규칙이었다면 첫 칸 색이 됐다
        assertEquals(
            listOf(YGColorChipType.NametagChip2),
            viewModel.state.value.memberChips.map(GroupMemberChip::colorChipType),
        )
    }
```

```kotlin
    @Test
    fun enter_memberWithoutAChip_getsTheNeutralColour() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 앱이 모르는 값을 줘 매퍼가 접었다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(TODAY_PARFAIT_ID, today, members = listOf(member("모카", nametagChip = null))),
        )

        // When 화면에 들어간다
        val viewModel = enteredViewModel()

        // Then 아무 색이나 돌리지 않는다
        assertEquals(
            listOf(YGColorChipType.Default),
            viewModel.state.value.memberChips.map(GroupMemberChip::colorChipType),
        )
    }
```

> ⚠️ **두 번째 테스트가 이 태스크의 핵심이다.** "앞사람이 빠져도 남은 사람 색이 그대로"가 인덱스 규칙과 서버 값을 가르는 **유일한 관찰**이다. 첫 테스트만 있으면 팔레트 순서와 우연히 맞는 배정에서 통과할 수 있다. 세 번째는 변환 함수가 아니라 **ViewModel 결선**이 널을 중립으로 흘리는지를 잠근다(스펙 테스트 표의 항목이다).

`enteredViewModel()`이 `getTodayParfait`을 부르는 시점보다 `coEvery`가 **먼저** 와야 한다 — 위 순서를 지켜라. `GroupMemberChip`은 이 파일이 이미 import하고 있다.

import 둘을 추가한다.

```kotlin
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.domain.model.group.NametagChipType
```

- [ ] **Step 6: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasMainViewModelTest*"`

Expected: FAIL — 팔레트가 인덱스로 색을 고르므로 첫 테스트가 `NametagChip1`·`NametagChip2`를 내놓는다

- [ ] **Step 7: ViewModel을 고친다**

`CanvasMainViewModel.kt` — `toMemberChips`의 본문과 KDoc을 바꾸고 팔레트 상수를 지운다.

```kotlin
    /**
     * 색은 서버가 그룹 안에서 배정한 값을 그대로 쓴다 — 목록 순서를 쓰면 멤버가 빠질 때 남은
     * 사람 색이 밀리고, 같은 사람이 S-101 그룹 설정과 다른 색으로 보인다.
     */
    private fun List<CanvasMemberVO>.toMemberChips(): List<GroupMemberChip> = map { member ->
        GroupMemberChip(
            nickname = member.nickname.value,
            colorChipType = member.nametagChip.toColorChipType(),
        )
    }
```

companion object에서 `NAMETAG_CHIP_PALETTE` 선언을 통째로 지운다.

⚠️ **`YGColorChipType` import은 지우지 마라.** 같은 파일의 톱레벨 `data class GroupMemberChip`이 `colorChipType: YGColorChipType`를 들고 있어 계속 쓰인다.

`toColorChipType` import을 추가한다.

```kotlin
import com.teamyg.parfait.feature.groups.canvas.impl.util.toColorChipType
```

- [ ] **Step 8: 칩 안의 글자는 건드리지 않는다**

`Default`가 떠도 `nickname.take(1)` 그대로 둔다(S-101 `GroupMemberList`와 같다). Figma의 `Default` 변형은 글자가 `-`지만, 여기서 `Default`가 뜨는 것은 **계약이 어긋났다는 뜻**이라 첫 글자가 오히려 단서가 된다. 이 스텝은 **확인만 하고 코드를 바꾸지 않는다** — `CanvasMainScreen`의 칩 호출부가 닉네임 첫 글자를 넘기는지 눈으로 보고 넘어간다.

- [ ] **Step 9: 테스트와 린트를 돌린다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck`

Expected: PASS

- [ ] **Step 10: 기존 두 변환의 KDoc을 갱신한다**

두 파일이 서로를 "짝이 되는 변환"(단수)으로 부른다. 이제 셋이므로 각각 나머지 둘을 가리키게 하고, Step 3이 적은 컴파일러 한계를 같은 문장으로 담는다.

`setting/impl/util/ColorChipType.kt` — "G-001 목록에도 짝이 되는 변환이 있지만…"으로 시작하는 문단을 아래로 바꾼다.

```kotlin
 * **같은 규칙의 변환이 저장소에 셋이다** — C-001 캔버스 상단 칩(12종 1:1, 이 파일과 글자까지
 * 같다)과 G-001 목록(12종을 6종으로 짝지어 접는다). 공용화하지 않은 이유는 자리가 없어서가
 * 아니라 `core:ui` 의 `implementation`/`api` 가시성이 팀 결정 대상으로 열려 있어서다.
 *
 * ⚠️ **컴파일러가 잡아 주는 것은 앱이 [NametagChipType] 에 상수를 더할 때의 arm 누락뿐이다.**
 * 서버에 새 타입이 생기면 매퍼가 `null` 로 접어 컴파일이 안 깨지고, 셋 중 하나에서 색만
 * 바꾸는 것도 못 잡는다. 색을 고칠 때는 셋을 함께 본다.
```

`list/impl/util/GrouptagChipType.kt` — "S-101 그룹 설정에도 짝이 되는 변환이 있지만…"으로 시작하는 문단을 아래로 바꾼다.

```kotlin
 * **같은 규칙의 변환이 저장소에 셋이다** — S-101 그룹 설정과 C-001 캔버스 상단 칩은 12종을
 * 1:1로 옮기고(둘은 서로 글자까지 같다) 이 파일만 6종으로 접는다. 공용화하지 않은 이유는
 * 자리가 없어서가 아니라 `core:ui` 의 `implementation`/`api` 가시성이 팀 결정 대상이어서다.
 *
 * ⚠️ **컴파일러가 잡아 주는 것은 앱이 [NametagChipType] 에 상수를 더할 때의 arm 누락뿐이다.**
 * 서버에 새 타입이 생기면 매퍼가 `null` 로 접어 컴파일이 안 깨진다.
```

- [ ] **Step 11: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ColorChipType.kt \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ColorChipTypeTest.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt \
        feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/util/ColorChipType.kt \
        feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/GrouptagChipType.kt
git commit -m "feat(canvas): colour the top member chips from the server value

목록 순서로 7종 팔레트를 돌던 자리다 — 멤버가 나가면 남은 사람 색이 밀리고
같은 사람이 S-101 과 다른 색으로 보였다. 서버가 groupMembers 에 칩을 주므로
팔레트 개념 자체가 사라진다(근거 없던 7종 문제도 함께 닫힌다).

변환은 S-101 과 규칙이 같지만 복제한다 — 공용 자리는 있으나 core:ui 의
implementation/api 가시성이 팀 결정 대상으로 열려 있다."
```

---

### Task 6: 소비처 없는 계약을 DTO에만 미러링한다

서버가 늘린 나머지 둘이다. **읽는 화면이 0건이라 VO로 올리지 않는다** — 소비자 없이 도메인 모양을 굳히면 화면이 붙을 때 되돌려야 한다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/PlaceParfaitImageResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/CreateParfaitGroupResponse.kt`

⚠️ **`PlacedByResponse`(parfaitimage)를 참조하는 곳은 셋이다** — 선언 파일, 매퍼, **그리고 테스트**다. 셋을 한 커밋에 고쳐야 `:data`가 붙는다.

**Interfaces:**
- Produces: 없음(도메인·화면에 노출되는 변화가 없다)

- [ ] **Step 1: 토핑 배치 응답의 중첩 타입을 개명하고 칩을 더한다**

`PlaceParfaitImageResponse.kt`. 서버가 `PlacedByResponse` → `PlaceParfaitImagePlacedByResponse`로 개명했다(springdoc이 캔버스 응답의 동명 클래스와 한 스키마로 취급해, 캔버스 쪽에 추가한 칩이 스웨거에 안 보이던 문제 때문이다). 앱이 두 패키지에 같은 이름을 둔 근거가 "서버가 그렇다"였으므로 거울을 유지한다.

```kotlin
/**
 * 배치자. 서버가 캔버스 응답의 동명 클래스와 스키마 충돌을 없애려고 이쪽만 개명했다.
 *
 * @param nameTagChip **아직 도메인으로 올리지 않는다** — 이 값을 읽는 화면이 0건이다.
 */
@Serializable
data class PlaceParfaitImagePlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
```

`PlaceParfaitImageResponse`의 `placedBy` 타입도 새 이름으로 바꾼다.

```kotlin
    @SerialName("placedBy")
    val placedBy: PlaceParfaitImagePlacedByResponse,
```

- [ ] **Step 2: parfaitimage 매퍼의 리시버 타입을 고친다**

`data/.../source/parfaitimage/mapper/VOMapper.kt`의 `toToppingPlacerVO` 확장 함수 리시버가 `PlacedByResponse`다. 새 이름으로 바꾼다 — **본문은 그대로**다(칩을 안 올리므로).

```kotlin
private fun PlaceParfaitImagePlacedByResponse.toToppingPlacerVO(): ToppingPlacerVO = ToppingPlacerVO(
    groupMemberId = GroupMemberId(groupMemberId),
    nickname = GroupNickname(nickname),
)
```

import도 새 이름으로 바꾼다.

- [ ] **Step 3: 테스트의 참조를 고친다**

`data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt`가 이 클래스를 import하고 픽스처에서 생성한다. **이걸 빼면 `:data` 테스트 컴파일이 깨진다.**

import을 바꾼다.

```kotlin
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImagePlacedByResponse
```

픽스처의 생성자 호출을 바꾼다(칩은 안 넘긴다 — 기본값 `null`이고 이 테스트는 칩을 검증하지 않는다).

```kotlin
            placedBy = PlaceParfaitImagePlacedByResponse(groupMemberId = 10L, nickname = "연경이"),
```

- [ ] **Step 4: 그룹 생성 응답에 3필드를 더한다**

`CreateParfaitGroupResponse.kt`

```kotlin
    /** 갓 만든 그룹이라 서버가 항상 `null` 을 넣는다 */
    @SerialName("recentImageUrl")
    val recentImageUrl: String? = null,
    /** 방금 저장한 그룹의 updatedAt 이다 — 목록 응답의 같은 필드는 created_at 이라 출처가 다르다 */
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    /** 생성자에게 방금 배정된 칩 */
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
```

`CreatedGroupVO`와 `toCreatedGroupVO`는 **건드리지 않는다** — A-005는 생성 직후 목록으로 돌아가며 목록을 다시 읽으므로 소비할 값이 없다.

- [ ] **Step 5: 테스트와 린트를 돌린다**

Run: `./gradlew :data:testDebugUnitTest ktlintCheck`

Expected: PASS. 새 필드는 전부 기본값이 있어 기존 픽스처가 그대로 컴파일된다. 개명은 컴파일이 검증한다 — Step 1~3 중 하나라도 빠지면 여기서 깨진다.

- [ ] **Step 6: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/PlaceParfaitImageResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt \
        data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/group/CreateParfaitGroupResponse.kt
git commit -m "chore(data): mirror the remaining server response fields

토핑 배치 응답의 중첩 타입 개명(서버가 스키마 충돌 해소로 이쪽만 바꿨다) +
칩 필드, 그룹 생성 응답 3필드. 읽는 화면이 0건이라 VO 로는 올리지 않는다 —
소비자 없이 도메인 모양을 굳히면 화면이 붙을 때 되돌려야 한다."
```

---

### Task 7: 거짓이 된 KDoc을 정리하고 전체를 검증한다

이번 delta로 **사실이 아니게 된 서술**들이다. 코드가 맞아도 주석이 틀리면 다음 사람이 같은 실수를 되풀이한다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/MyParfaitGroupVO.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupMemberVO.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt`

- [ ] **Step 1: `MyParfaitGroupVO`의 두 KDoc을 고친다**

`recentImageUploadedAt` — "오프셋이 붙은 절대 시점"이 거짓이다. 서버는 오프셋 없는 로컬 날짜시각을 주고 오프셋은 앱 매퍼가 부여한다(Task 1). 토핑이 없으면 그 값은 그룹 생성 시각이라 "마지막 활동"도 아니다.

```kotlin
    /**
     * 마지막으로 토핑이 올라온 시각. **토핑이 하나도 없으면 그룹이 만들어진 시각**이 오므로
     * "활동이 있었다"는 뜻이 아니다 — 그것을 가르려면 [recentImageUrl] 이 `null` 인지 함께 본다.
     *
     * 서버는 오프셋 없는 로컬 날짜시각을 주고 매퍼가 Asia/Seoul 을 부여해 절대 시점으로 만든다.
     */
    val recentImageUploadedAt: Instant?,
```

`lastPlacedByNametagChip` — "토핑이 하나도 없으면 `null`"이 거짓이다. 서버가 `COALESCE`로 **그룹 생성자의 칩**을 넣어, 토핑이 0건인 그룹도 중립이 아니라 사람 색을 얻는다.

```kotlin
    /**
     * 마지막으로 토핑을 올린 사람의 칩. **토핑이 하나도 없으면 그룹을 만든 사람의 칩**이 온다 —
     * 그 경우 이 값은 "마지막으로 바꾼 사람"이 아니라 "만든 사람"을 가리킨다.
     * 그 사람이 이미 그룹을 나갔으면 [NametagChipType.DEFAULT] 다.
     *
     * `null` 은 앱이 모르는 타입 문자열이 와서 매퍼가 접었다는 뜻이다.
     */
    val lastPlacedByNametagChip: NametagChipType?,
```

- [ ] **Step 2: `ParfaitGroupMemberVO`의 KDoc을 고친다**

"계약 타입이 널 허용이라"가 거짓이다. 서버는 비널로 좁혔고 **앱만** 널 허용을 유지한다.

```kotlin
    /**
     * 서버가 이 그룹 안에서 배정한 칩. 상세 응답은 탈퇴자를 빼고 주므로
     * [NametagChipType.DEFAULT] 는 오지 않는다.
     *
     * **서버 계약은 비널인데 여기가 널 허용인 것은 의도다** — 구버전 서버를 만나거나 앱이 모르는
     * 타입 문자열이 왔을 때 매퍼가 `null` 로 접어, 화면이 통째로 실패하는 대신 중립 색으로 그린다.
     */
    val nametagChip: NametagChipType?,
```

- [ ] **Step 3: `ParfaitService`의 과거 목록 KDoc을 고친다**

`getGroupsByGroupIdParfaits`의 KDoc이 서버 기본값의 "오늘"을 자정 기준으로 기술한다. 그 "오늘"이 이제 **03시 경계**(`ParfaitDay.current()`)다. 파일을 열어 그 문장을 찾아 아래 취지로 고친다 — **동작은 안 바뀐다**(유일한 프로덕션 호출부 `GetParfaitHistoriesUseCase`가 항상 범위를 명시한다). 기술만 낡았다.

```
`from`·`to` 를 비우면 서버 기본값이 산다 — `to` 는 서버 기준 오늘, `from` 은 그로부터 30일 전이다.
그 "오늘"은 자정이 아니라 **03시에 넘어간다**(서버 `ParfaitDay.current()`). 앱은 항상 범위를
명시해 부르므로 이 기본값에 물리지 않는다.
```

- [ ] **Step 4: 남은 옛 키 참조가 없는지 확인한다**

Run: `git grep -n "nametagChip\|NametagChip" -- 'data/src/main/java/com/teamyg/parfait/data/service/model'`

Expected: 결과 0건. `:data`의 wire DTO에는 옛 표기가 남으면 안 된다(도메인·매퍼 변수명에는 남는 것이 정상이다 — 개명 경계가 거기다).

- [ ] **Step 5: 전체 빌드와 테스트를 돌린다**

Run: `./gradlew testDebugUnitTest :domain:test ktlintCheck assembleDebug`

Expected: PASS.
⚠️ **`:domain:test`를 빼면 안 된다** — `:domain`은 Kotlin JVM 모듈이라 `testDebugUnitTest`가 없어서, 그것만 돌리면 `domain/src/test`가 최종 검증에서 통째로 빠진다.

- [ ] **Step 6: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/group/MyParfaitGroupVO.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupMemberVO.kt \
        data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt
git commit -m "docs(group,parfait): correct KDoc the server delta made false

- lastPlacedByNametagChip: 토핑 0건이어도 null 이 아니라 생성자 칩이 온다
- recentImageUploadedAt: 오프셋은 서버가 아니라 앱이 부여한다. 토핑이 없으면
  그 값은 그룹 생성 시각이라 '마지막 활동'이 아니다
- ParfaitGroupMemberVO.nametagChip: 서버는 비널로 좁혔고 앱만 널을 유지한다
- 과거 목록 to 기본값의 '오늘'이 자정이 아니라 03시 경계다(동작 무변경)"
```

---

## 실행 후 확인

- [ ] `git grep -n "RELEASED"` → 0건
- [ ] `./gradlew testDebugUnitTest :domain:test ktlintCheck assembleDebug` → PASS
- [ ] `git log --oneline origin/develop..HEAD` → 이 계획의 커밋 7개가 브랜치 위에 얹혀 있다

**실기기·실서버 확인은 이 계획의 범위 밖이다**(이 저장소의 모든 계약 라운드와 같이 코드 대조까지). 다만 Task 1이 고치는 버그는 **실서버에 한 번만 쏴 봤으면 즉시 드러났을 종류**라는 점은 기록해 둔다 — `http/parfait-group.http`의 목록 요청이 그 수단이다.

**이 계획이 잡지 못하는 것**: 어떤 테스트도 `@SerialName` 문자열이 서버와 같은지 검증하지 않는다(전부 DTO 객체를 직접 만들어 넣는다). 다음 서버 키 변경도 `sync-teamyg-server-api` 감사로만 잡힌다 — 스펙이 감수한 결정이고 열린 질문으로 남아 있다.
