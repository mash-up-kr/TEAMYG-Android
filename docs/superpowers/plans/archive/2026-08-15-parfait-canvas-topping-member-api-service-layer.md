# 캔버스 조회·토핑 테두리/삭제·회원 탈퇴 API Service·remote DataSource 구현 계획

> ✅ **완료 — develop 머지(PR #250, 2026-08-15).** 브랜치 `feature/canvas-topping-member-api-260815`.
> Task 0~6 전 Step 완료. 머지본 재대조에서 **설계와 갈린 곳 0건**이고, 계획이 예고한 대로 DI 줄은
> 한 줄도 늘지 않았다.
>
> **계획 밖 동반 변경 2건**(같은 PR, 커밋 `1a6a5577`) — 같은 날 2차 서버 delta(`e4ff23f`)의 규칙 변경을
> 앱에 반영했다: ① `CheckNameValidUseCase` 문자 집합에 자모 범위 추가, ② `GROUP_NICKNAME_ALREADY_USED`
> 계열(상수·`GroupNickNameError.ALREADY_USED`·문구·매핑 분기) 제거. 상세는
> [스펙](../../specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md) 머지 블록.
>
> 마지막 Step("위키 repo 문서 갱신은 develop 머지 후로 미룬다")은 이 문서를 아카이브하는 이 라운드에서 수행했다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 기준선 `36ecd1c`가 들여온 5 엔드포인트(캔버스 오늘 조회·과거 목록, 토핑 테두리 수정·삭제, 회원 탈퇴)를 TJYG-Android `:data`의 Retrofit Service와 remote DataSource로 배선하고 대응 domain VO를 만든다.

**Architecture:** `Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. 기존 6라운드가 세운 관용구의 증분이고, 새 결정은 `today` 응답의 3층 중첩을 domain에서 어떻게 접느냐에 몰려 있다. Repository·UseCase·화면은 범위 밖이다.

**Tech Stack:** Kotlin · Retrofit2 · kotlinx-serialization · kotlinx-datetime · Hilt · MockK · kotlin-test · kotlinx-coroutines-test

**Spec:** [`parfait/specs/2026-08-15-parfait-canvas-topping-member-api-service-layer.md`](../../specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md)

**계약 정본:** [`parfait/api/parfait.md`](../../../api/parfait.md) · [`parfait/api/parfait-image.md`](../../../api/parfait-image.md) · [`parfait/api/member.md`](../../../api/member.md)

## Global Constraints

- **작업 저장소는 `TJYG-Android`**(로컬 절대경로는 `wiki/personal-private/project-paths.md`). 이 계획서가 사는 위키 repo가 아니다.
- **브랜치**: `feature/canvas-topping-member-api-260815`를 `origin/develop`에서 판다. **Task마다 커밋한다**(사용자 지시, 2026-08-15). `push`·PR 생성은 **하지 않는다** — 별도 확인 대상이다.
- **DI 등록 줄을 추가하지 않는다.** 세 Service(`ParfaitService`·`ParfaitImageService`·`MemberService`)와 세 remote DataSource가 이미 `ServiceModule`·`RemoteDataSourceModule`에 등록돼 있다. 기존 인터페이스에 함수를 더하는 라운드다.
- **wire DTO는 전 프로퍼티에 `@SerialName`을 명시한다.** 값은 서버 DTO 프로퍼티명 그대로.
- **매퍼 단독 테스트 파일(`*VOMapperTest`)을 만들지 않는다.** 판단이 든 변환은 그 매퍼를 통과시키는 DataSource 테스트 케이스로 잠근다.
- **Service 함수명 규칙**: `<method><PathSegmentsCamelCase>`. 길어도 규칙의 답을 쓴다.
- **`domain`은 제품 언어**(`Canvas`·`Topping`), **`data`는 서버 언어**(`Parfait`·`ParfaitImage`), **id 타입은 서버 언어 유지**(`ParfaitId`·`ParfaitImageId`).
- **날짜·시각 타입은 `kotlinx.datetime`**(`LocalDate`·`LocalDateTime`). 서버는 ISO-8601 문자열을 준다.
- 테스트 함수명은 `대상_조건_기대` 3세그먼트 snake 혼합(`getMyAccount_serviceReturnsSuccess_returnsMappedVo` 선례). 본문은 `// Given` `// When` `// Then` 주석으로 가른다.
- 검증 명령: `./gradlew test` · `./gradlew ktlintCheck` · `./gradlew :app:assembleDebug`.

---

## 파일 구조

**생성**

| 경로 | 책임 |
|---|---|
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt` | today 응답 wire DTO 5개(상위 + 중첩 4) |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/PastParfaitsResponse.kt` | 과거 목록 wire DTO 2개 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageBorderRequest.kt` | 테두리 수정 요청 DTO |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImageBorderResponse.kt` | 테두리 수정 응답 DTO |
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt` | parfait 응답 → canvas VO 변환(이 도메인 첫 매퍼) |
| `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasStatus.kt` | 캔버스 상태 enum(+`UNKNOWN` 폴백) |
| `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasBackground.kt` | 배경 sealed(Color/Image) |
| `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasMemberVO.kt` | 캔버스에 이름이 걸리는 그룹 멤버 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasToppingVO.kt` | 캔버스에 배치된 토핑(테두리·생성시각 포함) |
| `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/TodayCanvasVO.kt` | 오늘의 캔버스 전체 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/PastCanvasVO.kt` | 과거 캔버스 요약 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/topping/UpdatedToppingBorderVO.kt` | 테두리 수정 결과 |
| `data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt` | parfait DataSource 테스트(신규) |

**수정**

| 경로 | 무엇 |
|---|---|
| `data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt` | 함수 2개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt` | 함수 2개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/service/MemberService.kt` | 함수 1개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSource.kt`(+`Impl`) | 함수 2개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt`(+`Impl`) | 함수 2개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt` | 테두리 요청/응답 변환 2개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSource.kt`(+`Impl`) | 함수 1개 추가 |
| `data/src/test/.../parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt` | 테두리·삭제 케이스 보강 |
| `data/src/test/.../member/remote/MemberRemoteDataSourceImplTest.kt` | 탈퇴 케이스 보강 |
| `http/parfait.http` · `http/parfait-image.http` · `http/users.http` · `http/README.md` | 요청 5개 추가·설명 갱신 |

**Task 경계**: domain 모델(Task 1) → parfait 배선(Task 2·3) → parfait-image(Task 4) → member(Task 5) → `http/`(Task 6). Task 2는 DTO+Service+매퍼까지, Task 3은 DataSource+테스트다 — today 매핑이 이 라운드에서 가장 큰 덩어리라 "변환 규칙"과 "그 규칙을 잠그는 테스트"를 나눈다.

---

### Task 0: 브랜치 준비

**Files:** 없음(git 조작만)

**Interfaces:**
- Consumes: 없음
- Produces: 작업 브랜치 `feature/canvas-topping-member-api-260815`

- [x] **Step 1: develop 최신화 후 브랜치 생성**

```bash
cd <TJYG-Android 로컬 경로>
git fetch origin develop
git switch -c feature/canvas-topping-member-api-260815 origin/develop
git log --oneline -1
```

Expected: `80895eb1 Merge pull request #241 ...` 또는 그 이후 develop HEAD.

- [x] **Step 2: 기준 빌드가 깨지지 않았는지 확인**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL. 여기서 실패하면 이 계획의 문제가 아니므로 **멈추고 보고한다**(직전 라운드에서 다른 브랜치 빌드 잔재로 Hilt 컴파일이 깨진 선례가 있다 — 그때는 `./gradlew clean`으로 풀렸다).

---

### Task 1: domain 모델 7종

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasStatus.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasBackground.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasMemberVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasToppingVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/TodayCanvasVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/PastCanvasVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/UpdatedToppingBorderVO.kt`

**Interfaces:**
- Consumes: 기존 `ParfaitId`·`ParfaitImageId`·`ImageId`·`GroupMemberId`(`domain/model/id/`), `GroupNickname`(`domain/model/group/`), `ToppingTransform`·`ToppingBorder`·`ToppingPlacerVO`(`domain/model/topping/`)
- Produces: `CanvasStatus`(enum `ACTIVE`·`CLOSED`·`EMPTY`·`UNKNOWN`), `CanvasBackground`(sealed `Color(value: String)`·`Image(url: String)`), `CanvasMemberVO(groupMemberId: GroupMemberId, nickname: GroupNickname)`, `CanvasToppingVO(parfaitImageId, imageId, imageUrl, transform, border, placedBy, createdAt: LocalDateTime)`, `TodayCanvasVO(parfaitId, date: LocalDate, status, lastClosedDate: LocalDate?, members: List<CanvasMemberVO>, background: CanvasBackground?, toppings: List<CanvasToppingVO>)`, `PastCanvasVO(parfaitId, date: LocalDate, thumbnailUrl: String?, toppingCount: Int)`, `UpdatedToppingBorderVO(parfaitImageId, border: ToppingBorder)`

> 이 Task에는 테스트가 없다. **데이터 홀더뿐이고 로직이 0줄**이라 잠글 동작이 없다 — 이 모델들의 계약은 Task 3·4의 DataSource 테스트가 매핑을 통해 잠근다. 컴파일이 게이트다.

- [x] **Step 1: `CanvasStatus` 작성**

```kotlin
package com.teamyg.parfait.domain.model.canvas

/**
 * 캔버스 상태.
 *
 * EMPTY 는 "비어 있음"이 아니라 "빈 채로 마감됨"이다 — 03시 회전 배치가 토핑 0건인 캔버스를
 * 이 상태로 닫는다(`api/parfait.md`). ACTIVE 가 아니면 더 올릴 수 없다는 뜻이지만
 * 서버가 그것을 강제하지 않는다 — 마감된 캔버스에도 배치·수정·삭제가 통과한다.
 *
 * UNKNOWN 은 서버가 상태를 늘렸을 때 앱이 크래시하지 않게 하는 폴백이다.
 */
enum class CanvasStatus {
    ACTIVE,
    CLOSED,
    EMPTY,
    UNKNOWN,
}
```

- [x] **Step 2: `CanvasBackground` 작성**

```kotlin
package com.teamyg.parfait.domain.model.canvas

/**
 * 캔버스 배경.
 *
 * 서버는 { type, value } 평면인데 value 의 뜻이 type 에 따라 갈린다(색 문자열 vs 이미지 URL).
 * sealed 로 가르면 "색인 줄 알고 URL 을 넣는" 실수가 컴파일에서 막힌다.
 *
 * 서버에 배경을 설정하는 API 가 아직 없어 현재는 항상 null 이 온다(`api/parfait.md`).
 * 미지 type 도 null 로 접는다 — 그려달라는 뜻을 모르는 것과 미설정은 화면에서 같은 처리다.
 */
sealed interface CanvasBackground {
    @JvmInline
    value class Color(val value: String) : CanvasBackground

    @JvmInline
    value class Image(val url: String) : CanvasBackground
}
```

- [x] **Step 3: `CanvasMemberVO` 작성**

```kotlin
package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId

/**
 * 캔버스 응답이 함께 주는 그룹 멤버.
 *
 * 서버 응답의 id 는 계정(MemberId)이 아니라 그룹 멤버십 행(GroupMemberId)이다 —
 * 토핑의 placedBy.groupMemberId 와 같은 축이라 그 둘로 조인할 수 있다.
 * 다만 탈퇴한 멤버는 이 목록에서 빠지는데 그 토핑은 남으므로, 조인이 항상 성립하지는 않는다.
 */
data class CanvasMemberVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
)
```

- [x] **Step 4: `CanvasToppingVO` 작성**

```kotlin
package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.datetime.LocalDateTime

/**
 * 캔버스 조회가 돌려주는 배치 토핑.
 *
 * 배치 확정 응답(PlacedToppingVO)과 필드 집합이 다르다 — 이쪽에만 테두리와 생성시각이 있다.
 * 두 타입을 합치면 POST 응답에 없는 값을 지어내거나 nullable 로 "모른다"와 "없다"를 뭉갠다.
 * 공통 조각(ToppingTransform·ToppingBorder·ToppingPlacerVO)은 그대로 재사용한다.
 *
 * placedBy 의 groupMemberId 가 같은 응답의 members 에 없을 수 있다 — 탈퇴·이탈한 멤버의
 * 토핑은 남고 닉네임이 "(알수없음)"으로 온다(`api/parfait.md`).
 */
data class CanvasToppingVO(
    val parfaitImageId: ParfaitImageId,
    val imageId: ImageId,
    val imageUrl: String,
    val transform: ToppingTransform,
    val border: ToppingBorder,
    val placedBy: ToppingPlacerVO,
    val createdAt: LocalDateTime,
)
```

- [x] **Step 5: `TodayCanvasVO` 작성**

```kotlin
package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

/**
 * 오늘의 캔버스 전체.
 *
 * ⚠️ 이 값을 얻는 조회는 서버에서 캔버스 행을 만든다 — 해당 날짜 파르페가 없으면 생성해
 * 저장한다(`api/parfait.md`). 화면이 이 호출을 남발하면 빈 캔버스가 양산된다.
 *
 * lastClosedDate 는 CLOSED 만 센다(EMPTY 제외) — "마지막 마감일"이 아니라
 * "마지막으로 토핑이 있던 날"이다.
 *
 * toppings 는 서버가 0건일 때 null 을 주지만 여기서는 빈 목록으로 접는다 — 0건과 빈 목록은
 * 같은 뜻이라 소비처가 널 분기를 반복할 이유가 없다. background·lastClosedDate 의 null 은
 * "미설정"·"이력 없음"이라는 의미가 있어 그대로 둔다.
 */
data class TodayCanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val status: CanvasStatus,
    val lastClosedDate: LocalDate?,
    val members: List<CanvasMemberVO>,
    val background: CanvasBackground?,
    val toppings: List<CanvasToppingVO>,
)
```

- [x] **Step 6: `PastCanvasVO` 작성**

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
    val thumbnailUrl: String?,
    val toppingCount: Int,
)
```

- [x] **Step 7: `UpdatedToppingBorderVO` 작성**

```kotlin
package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 테두리 수정 결과.
 *
 * 앱이 서버로부터 테두리를 되받는 첫 자리다 — 배치 확정·위치 수정 두 응답은 테두리를
 * 저장만 하고 돌려주지 않는다(`api/parfait-image.md`).
 */
data class UpdatedToppingBorderVO(
    val parfaitImageId: ParfaitImageId,
    val border: ToppingBorder,
)
```

- [x] **Step 8: 컴파일 확인**

Run: `./gradlew :domain:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 9: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/canvas domain/src/main/java/com/teamyg/parfait/domain/model/topping/UpdatedToppingBorderVO.kt
git commit -m "feat(domain): 캔버스 조회·테두리 수정 도메인 모델 추가"
```

---

### Task 2: parfait wire DTO · Service · 매퍼

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/GetTodayParfaitResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/PastParfaitsResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt`

**Interfaces:**
- Consumes: Task 1의 `TodayCanvasVO`·`PastCanvasVO`·`CanvasStatus`·`CanvasBackground`·`CanvasMemberVO`·`CanvasToppingVO`
- Produces:
  - `ParfaitService.getGroupsByGroupIdParfaitsToday(groupId: Long): ApiResponse<GetTodayParfaitResponse>`
  - `ParfaitService.getGroupsByGroupIdParfaits(groupId: Long, from: String?, to: String?): ApiResponse<PastParfaitsResponse>`
  - `internal fun GetTodayParfaitResponse.toTodayCanvasVO(): TodayCanvasVO`
  - `internal fun PastParfaitsResponse.toPastCanvasVOList(): List<PastCanvasVO>`

- [x] **Step 1: today 응답 DTO 작성**

```kotlin
package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 오늘의 캔버스 조회 응답.
 *
 * images 는 배치가 0건이면 빈 배열이 아니라 null 이다. background 도 type·value 중 하나라도
 * 없으면 통째로 null 이다. 서버가 default-property-inclusion: always 라 키 자체는 실려 오므로
 * 키 존재가 아니라 값이 null 인지로 갈라야 한다(`api/parfait.md`).
 */
@Serializable
data class GetTodayParfaitResponse(
    @SerialName("parfaitId")
    val parfaitId: Long,
    @SerialName("date")
    val date: String,
    @SerialName("status")
    val status: String,
    @SerialName("lastClosedDate")
    val lastClosedDate: String? = null,
    @SerialName("groupMembers")
    val groupMembers: List<GroupMemberResponse>,
    @SerialName("background")
    val background: BackgroundResponse? = null,
    @SerialName("images")
    val images: List<TodayParfaitImageResponse>? = null,
)

/**
 * @param id 계정 id 가 아니라 그룹 멤버십 행 id 다.
 */
@Serializable
data class GroupMemberResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("nickname")
    val nickname: String,
)

/**
 * @param value type 이 COLOR 면 색 문자열, IMAGE 면 URL 이다.
 */
@Serializable
data class BackgroundResponse(
    @SerialName("type")
    val type: String,
    @SerialName("value")
    val value: String,
)

@Serializable
data class TodayParfaitImageResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("positionX")
    val positionX: Double,
    @SerialName("positionY")
    val positionY: Double,
    @SerialName("positionZ")
    val positionZ: Int,
    @SerialName("scale")
    val scale: Double,
    @SerialName("rotation")
    val rotation: Double,
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
    @SerialName("placedBy")
    val placedBy: PlacedByResponse,
    @SerialName("createdAt")
    val createdAt: String,
)

/**
 * 배치자. 같은 이름의 DTO 가 response/parfaitimage 에도 있다 — 서버가 두 응답에 같은 이름을
 * 썼고 wire DTO 는 서버의 거울이라 이름을 바꾸지 않는다.
 *
 * @param nickname 그룹 닉네임이다. 탈퇴·이탈한 멤버면 "(알수없음)"이 온다.
 */
@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
)
```

- [x] **Step 2: 과거 목록 응답 DTO 작성**

```kotlin
package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 과거 캔버스 목록 응답. 0건이면 빈 배열이다 — today 의 images 가 null 인 것과 반대다.
 */
@Serializable
data class PastParfaitsResponse(
    @SerialName("parfaits")
    val parfaits: List<PastParfaitResponse>,
)

/**
 * @param thumbnailUrl 서버가 항상 null 을 넣는다. 채우는 코드가 없다(`api/parfait.md`).
 */
@Serializable
data class PastParfaitResponse(
    @SerialName("parfaitId")
    val parfaitId: Long,
    @SerialName("date")
    val date: String,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    @SerialName("imageCount")
    val imageCount: Int,
)
```

- [x] **Step 3: `ParfaitService`에 함수 2개 추가**

파일 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.ParfaitYearsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ParfaitService {
    @GET("api/v1/groups/{groupId}/parfaits/year")
    suspend fun getGroupsByGroupIdParfaitsYear(@Path("groupId") groupId: Long): ApiResponse<ParfaitYearsResponse>

    /**
     * ⚠️ 조회인데 서버가 캔버스 행을 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다.
     */
    @GET("api/v1/groups/{groupId}/parfaits/today")
    suspend fun getGroupsByGroupIdParfaitsToday(
        @Path("groupId") groupId: Long,
    ): ApiResponse<GetTodayParfaitResponse>

    /**
     * from·to 가 null 이면 Retrofit 이 쿼리 파라미터를 URL 에서 빼므로 서버 기본값
     * (to = 오늘, from = to - 30일)이 그대로 산다.
     */
    @GET("api/v1/groups/{groupId}/parfaits")
    suspend fun getGroupsByGroupIdParfaits(
        @Path("groupId") groupId: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): ApiResponse<PastParfaitsResponse>
}
```

- [x] **Step 4: parfait 매퍼 작성**

```kotlin
package com.teamyg.parfait.data.source.parfait.mapper

import com.teamyg.parfait.data.service.model.response.parfait.BackgroundResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.GroupMemberResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitImageResponse
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.canvas.TodayCanvasVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

private const val BACKGROUND_TYPE_COLOR = "COLOR"
private const val BACKGROUND_TYPE_IMAGE = "IMAGE"
private const val BORDER_TYPE_SOLID = "SOLID"

internal fun GetTodayParfaitResponse.toTodayCanvasVO(): TodayCanvasVO = TodayCanvasVO(
    parfaitId = ParfaitId(parfaitId),
    date = LocalDate.parse(date),
    status = status.toCanvasStatus(),
    lastClosedDate = lastClosedDate?.let(LocalDate::parse),
    members = groupMembers.map { it.toCanvasMemberVO() },
    background = background?.toCanvasBackground(),
    toppings = images.orEmpty().map { it.toCanvasToppingVO() },
)

internal fun PastParfaitsResponse.toPastCanvasVOList(): List<PastCanvasVO> = parfaits.map {
    PastCanvasVO(
        parfaitId = ParfaitId(it.parfaitId),
        date = LocalDate.parse(it.date),
        thumbnailUrl = it.thumbnailUrl,
        toppingCount = it.imageCount,
    )
}

private fun String.toCanvasStatus(): CanvasStatus = when (this) {
    CanvasStatus.ACTIVE.name -> CanvasStatus.ACTIVE
    CanvasStatus.CLOSED.name -> CanvasStatus.CLOSED
    CanvasStatus.EMPTY.name -> CanvasStatus.EMPTY
    else -> CanvasStatus.UNKNOWN
}

/**
 * 미지 type 은 null 로 접는다 — 그리라는 뜻을 모르는 것과 배경 미설정은 화면에서 같다.
 */
private fun BackgroundResponse.toCanvasBackground(): CanvasBackground? = when (type) {
    BACKGROUND_TYPE_COLOR -> CanvasBackground.Color(value)
    BACKGROUND_TYPE_IMAGE -> CanvasBackground.Image(value)
    else -> null
}

private fun GroupMemberResponse.toCanvasMemberVO(): CanvasMemberVO = CanvasMemberVO(
    groupMemberId = GroupMemberId(id),
    nickname = GroupNickname(nickname),
)

private fun TodayParfaitImageResponse.toCanvasToppingVO(): CanvasToppingVO = CanvasToppingVO(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    imageId = ImageId(imageId),
    imageUrl = imageUrl,
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
    border = toToppingBorder(),
    placedBy = placedBy.toToppingPlacerVO(),
    createdAt = LocalDateTime.parse(createdAt),
)

/**
 * SOLID 인데 색이나 두께가 없으면 Solid 를 만들 수 없으므로 None 으로 떨어뜨린다.
 * 서버는 그 조합을 저장 시점에 막지만(INVALID_BORDER) 이미 저장된 행이 있을 수 있고,
 * 앱이 크래시하는 것보다 테두리를 안 그리는 편이 낫다.
 */
private fun TodayParfaitImageResponse.toToppingBorder(): ToppingBorder {
    if (borderType != BORDER_TYPE_SOLID) return ToppingBorder.None
    val color = borderColor ?: return ToppingBorder.None
    val width = borderWidth ?: return ToppingBorder.None
    return ToppingBorder.Solid(color = color, width = width)
}

private fun PlacedByResponse.toToppingPlacerVO(): ToppingPlacerVO = ToppingPlacerVO(
    groupMemberId = GroupMemberId(groupMemberId),
    nickname = GroupNickname(nickname),
)
```

- [x] **Step 5: 컴파일·린트 확인**

Run: `./gradlew :data:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 6: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait \
        data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper
git commit -m "feat(data): 캔버스 조회 2건 Service·DTO·매퍼 추가"
```

---

### Task 3: parfait DataSource + 테스트

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSource.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: Task 2의 `ParfaitService` 함수 2개·매퍼 2개, Task 1의 VO
- Produces:
  - `ParfaitRemoteDataSource.getTodayCanvas(groupId: GroupId): Result<TodayCanvasVO>`
  - `ParfaitRemoteDataSource.getPastCanvases(groupId: GroupId, from: LocalDate? = null, to: LocalDate? = null): Result<List<PastCanvasVO>>`

- [x] **Step 1: 실패하는 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt`를 새로 만든다.

```kotlin
package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.BackgroundResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.GroupMemberResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitImageResponse
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParfaitRemoteDataSourceImplTest {
    private val parfaitService: ParfaitService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitRemoteDataSourceImpl(
        parfaitService = parfaitService,
        apiCaller = apiCaller,
    )

    private fun toppingResponse(
        borderType: String = "SOLID",
        borderColor: String? = "#FF0000",
        borderWidth: Double? = 4.0,
    ) = TodayParfaitImageResponse(
        parfaitImageId = 7L,
        imageId = 11L,
        imageUrl = "https://example.com/topping.png",
        positionX = 10.5,
        positionY = 20.5,
        positionZ = 3,
        scale = 1.5,
        rotation = 30.0,
        borderType = borderType,
        borderColor = borderColor,
        borderWidth = borderWidth,
        placedBy = PlacedByResponse(groupMemberId = 5L, nickname = "행복한 판다"),
        createdAt = "2026-08-15T09:30:00",
    )

    private fun todaySuccess(
        status: String = "ACTIVE",
        lastClosedDate: String? = "2026-08-14",
        background: BackgroundResponse? = BackgroundResponse(type = "COLOR", value = "#FFEEDD"),
        images: List<TodayParfaitImageResponse>? = listOf(toppingResponse()),
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = GetTodayParfaitResponse(
            parfaitId = 100L,
            date = "2026-08-15",
            status = status,
            lastClosedDate = lastClosedDate,
            groupMembers = listOf(GroupMemberResponse(id = 5L, nickname = "행복한 판다")),
            background = background,
            images = images,
        ),
    )

    private fun pastSuccess(parfaits: List<PastParfaitResponse>) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = PastParfaitsResponse(parfaits = parfaits),
    )

    private fun <T : Any> businessFailure(code: String) = ApiResponse<T>(
        success = false,
        code = code,
        message = "실패",
        data = null,
    )

    @Test
    fun getTodayCanvas_serviceReturnsFullCanvas_mapsEveryLayer() = runTest {
        // Given 서버가 멤버·배경·토핑이 모두 있는 캔버스를 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess()

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 3층 중첩이 전부 제자리에 들어간다
        assertEquals(ParfaitId(100L), canvas.parfaitId)
        assertEquals(LocalDate.parse("2026-08-15"), canvas.date)
        assertEquals(CanvasStatus.ACTIVE, canvas.status)
        assertEquals(LocalDate.parse("2026-08-14"), canvas.lastClosedDate)
        assertEquals(GroupMemberId(5L), canvas.members.single().groupMemberId)
        assertEquals(GroupNickname("행복한 판다"), canvas.members.single().nickname)
        assertEquals(CanvasBackground.Color("#FFEEDD"), canvas.background)

        val topping = canvas.toppings.single()
        assertEquals(ParfaitImageId(7L), topping.parfaitImageId)
        assertEquals(ImageId(11L), topping.imageId)
        assertEquals(3, topping.transform.positionZ)
        assertEquals(ToppingBorder.Solid(color = "#FF0000", width = 4.0), topping.border)
        assertEquals(GroupMemberId(5L), topping.placedBy.groupMemberId)
        assertEquals(LocalDateTime.parse("2026-08-15T09:30:00"), topping.createdAt)
    }

    @Test
    fun getTodayCanvas_imagesNull_foldsToEmptyList() = runTest {
        // Given 배치가 0건이라 서버가 images 를 null 로 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(images = null)

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 널이 아니라 빈 목록으로 접힌다
        assertEquals(emptyList(), canvas.toppings)
    }

    @Test
    fun getTodayCanvas_backgroundNull_staysNull() = runTest {
        // Given 배경이 설정되지 않았다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(background = null)

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 미설정은 의미 있는 상태라 널로 남는다
        assertNull(canvas.background)
    }

    @Test
    fun getTodayCanvas_unknownBackgroundType_foldsToNull() = runTest {
        // Given 서버가 앱이 모르는 배경 타입을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(background = BackgroundResponse(type = "GRADIENT", value = "x"))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 미설정과 같은 값으로 접힌다
        assertNull(canvas.background)
    }

    @Test
    fun getTodayCanvas_imageBackground_mapsToImageCase() = runTest {
        // Given 배경이 이미지다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(background = BackgroundResponse(type = "IMAGE", value = "https://example.com/bg.png"))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then value 가 url 자리로 간다
        assertEquals(CanvasBackground.Image("https://example.com/bg.png"), canvas.background)
    }

    @Test
    fun getTodayCanvas_unknownStatus_fallsBackToUnknown() = runTest {
        // Given 서버가 상태를 하나 늘렸다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(status = "ARCHIVED")

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 크래시하지 않고 UNKNOWN 으로 떨어진다
        assertEquals(CanvasStatus.UNKNOWN, canvas.status)
    }

    @Test
    fun getTodayCanvas_lowercaseStatus_fallsBackToUnknown() = runTest {
        // Given 서버가 소문자로 준다(대소문자 민감성 확인)
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(status = "active")

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 매핑은 정확히 일치할 때만 성립한다
        assertEquals(CanvasStatus.UNKNOWN, canvas.status)
    }

    @Test
    fun getTodayCanvas_borderTypeNone_mapsToNoneIgnoringColor() = runTest {
        // Given 테두리가 NONE 인데 색·두께가 함께 실려 왔다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(images = listOf(toppingResponse(borderType = "NONE")))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 값은 무시되고 None 이 된다
        assertEquals(ToppingBorder.None, canvas.toppings.single().border)
    }

    @Test
    fun getTodayCanvas_solidBorderMissingWidth_fallsBackToNone() = runTest {
        // Given SOLID 인데 두께가 없다(서버가 막는 조합이지만 이미 저장된 행일 수 있다)
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(images = listOf(toppingResponse(borderWidth = null)))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 크래시 대신 테두리를 그리지 않는다
        assertEquals(ToppingBorder.None, canvas.toppings.single().border)
    }

    @Test
    fun getTodayCanvas_groupNotJoined_returnsBusinessFailure() = runTest {
        // Given 참여하지 않은 그룹이다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            businessFailure<GetTodayParfaitResponse>("GROUP_NOT_JOINED")

        // When 오늘의 캔버스 조회
        val result = dataSource.getTodayCanvas(GroupId(1L))

        // Then 번역하지 않고 Business 로 흐른다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("GROUP_NOT_JOINED", error.code)
    }

    @Test
    fun getPastCanvases_serviceReturnsList_mapsCountAndThumbnail() = runTest {
        // Given 서버가 과거 캔버스 둘을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(
            listOf(
                PastParfaitResponse(parfaitId = 3L, date = "2026-08-14", thumbnailUrl = null, imageCount = 2),
                PastParfaitResponse(parfaitId = 2L, date = "2026-08-13", thumbnailUrl = null, imageCount = 0),
            ),
        )

        // When 과거 목록 조회
        val canvases = dataSource.getPastCanvases(GroupId(1L)).getOrThrow()

        // Then 서버 순서를 유지하고 imageCount 가 toppingCount 로 간다
        assertEquals(listOf(ParfaitId(3L), ParfaitId(2L)), canvases.map { it.parfaitId })
        assertEquals(listOf(2, 0), canvases.map { it.toppingCount })
        assertNull(canvases.first().thumbnailUrl)
    }

    @Test
    fun getPastCanvases_rangeOmitted_passesNullQueries() = runTest {
        // Given 범위를 넘기지 않는다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(emptyList())

        // When 과거 목록 조회
        dataSource.getPastCanvases(GroupId(1L)).getOrThrow()

        // Then 서버 기본값(오늘 - 30일)이 살도록 쿼리를 비워 보낸다
        coVerify { parfaitService.getGroupsByGroupIdParfaits(groupId = 1L, from = null, to = null) }
    }

    @Test
    fun getPastCanvases_rangeGiven_sendsIsoStrings() = runTest {
        // Given 범위를 지정한다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, "2026-08-01", "2026-08-15") } returns
            pastSuccess(emptyList())

        // When 과거 목록 조회
        dataSource.getPastCanvases(
            groupId = GroupId(1L),
            from = LocalDate.parse("2026-08-01"),
            to = LocalDate.parse("2026-08-15"),
        ).getOrThrow()

        // Then ISO-8601 문자열로 실린다
        coVerify {
            parfaitService.getGroupsByGroupIdParfaits(groupId = 1L, from = "2026-08-01", to = "2026-08-15")
        }
    }

    @Test
    fun getPastCanvases_invalidDateRange_returnsBusinessFailure() = runTest {
        // Given from 이 to 보다 늦다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, "2026-08-20", "2026-08-15") } returns
            businessFailure<PastParfaitsResponse>("INVALID_DATE_RANGE")

        // When 과거 목록 조회
        val result = dataSource.getPastCanvases(
            groupId = GroupId(1L),
            from = LocalDate.parse("2026-08-20"),
            to = LocalDate.parse("2026-08-15"),
        )

        // Then 서버 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_DATE_RANGE", error.code)
    }
}
```

- [x] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitRemoteDataSourceImplTest*"`
Expected: **컴파일 실패** — `getTodayCanvas`/`getPastCanvases`가 `ParfaitRemoteDataSourceImpl`에 없다(`Unresolved reference`).

- [x] **Step 3: 인터페이스에 함수 2개 추가**

`ParfaitRemoteDataSource.kt` 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.canvas.TodayCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.datetime.LocalDate

interface ParfaitRemoteDataSource {
    suspend fun getYears(groupId: GroupId): Result<List<Int>>

    /**
     * 오늘의 캔버스를 상태·멤버·배경·배치 토핑까지 한 번에 읽는다.
     *
     * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
     * (`api/parfait.md`). 화면이 반복 호출하면 빈 캔버스가 양산되므로 호출 지점을 아껴야 한다.
     *
     * 오늘 날짜가 이미 마감돼 있으면 그것을 그대로 돌려준다 — status 가 ACTIVE 가 아닐 수 있고,
     * 서버는 마감된 캔버스의 편집도 막지 않으므로 잠그는 것은 화면 책임이다.
     */
    suspend fun getTodayCanvas(groupId: GroupId): Result<TodayCanvasVO>

    /**
     * 과거 캔버스 목록. 범위를 생략하면 서버 기본값(오늘 - 30일 ~ 오늘)이다.
     *
     * 페이지네이션도 범위 상한도 없다 — 넓게 주면 그만큼 전량이 내려온다.
     * from 이 to 보다 늦으면 400 INVALID_DATE_RANGE 다.
     */
    suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<List<PastCanvasVO>>
}
```

- [x] **Step 4: 구현체에 함수 2개 추가**

`ParfaitRemoteDataSourceImpl.kt` 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.source.parfait.mapper.toPastCanvasVOList
import com.teamyg.parfait.data.source.parfait.mapper.toTodayCanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.canvas.TodayCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class ParfaitRemoteDataSourceImpl @Inject constructor(
    private val parfaitService: ParfaitService,
    private val apiCaller: ApiCaller,
) : ParfaitRemoteDataSource {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = apiCaller
        .safeApiCall(
            block = { parfaitService.getGroupsByGroupIdParfaitsYear(groupId.value) },
            transform = { it.years },
        )

    override suspend fun getTodayCanvas(groupId: GroupId): Result<TodayCanvasVO> = apiCaller.safeApiCall(
        block = { parfaitService.getGroupsByGroupIdParfaitsToday(groupId.value) },
        transform = { it.toTodayCanvasVO() },
    )

    override suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate?,
        to: LocalDate?,
    ): Result<List<PastCanvasVO>> = apiCaller.safeApiCall(
        block = {
            parfaitService.getGroupsByGroupIdParfaits(
                groupId = groupId.value,
                from = from?.toString(),
                to = to?.toString(),
            )
        },
        transform = { it.toPastCanvasVOList() },
    )
}
```

- [x] **Step 5: 테스트 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitRemoteDataSourceImplTest*"`
Expected: PASS (14케이스)

- [x] **Step 6: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/remote \
        data/src/test/java/com/teamyg/parfait/data/source/parfait/remote
git commit -m "feat(data): 캔버스 조회 remote DataSource 추가"
```

---

### Task 4: 토핑 테두리 수정·삭제

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageBorderRequest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImageBorderResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt`(+`Impl`)
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 `UpdatedToppingBorderVO`, 기존 `ToppingBorder`
- Produces:
  - `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(groupId: Long, parfaitId: Long, parfaitImageId: Long, request: UpdateParfaitImageBorderRequest): ApiResponse<UpdateParfaitImageBorderResponse>`
  - `ParfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(groupId: Long, parfaitId: Long, parfaitImageId: Long): ApiResponse<Unit>`
  - `internal fun ToppingBorder.toUpdateBorderRequest(): UpdateParfaitImageBorderRequest`
  - `internal fun UpdateParfaitImageBorderResponse.toUpdatedToppingBorderVO(): UpdatedToppingBorderVO`
  - `ParfaitImageRemoteDataSource.updateToppingBorder(groupId: GroupId, parfaitId: ParfaitId, parfaitImageId: ParfaitImageId, border: ToppingBorder): Result<UpdatedToppingBorderVO>`
  - `ParfaitImageRemoteDataSource.deleteTopping(groupId: GroupId, parfaitId: ParfaitId, parfaitImageId: ParfaitImageId): Result<Unit>`

- [x] **Step 1: 실패하는 테스트를 기존 파일 끝에 추가**

`ParfaitImageRemoteDataSourceImplTest.kt`의 클래스 본문 끝(마지막 `}` 바로 위)에 아래 테스트 6개를 붙인다.
**추가할 import는 정확히 둘**이다(나머지 — `slot`·`coEvery`·`assertEquals`·`assertIs`·`assertNull`·`assertTrue` —
는 이미 파일에 있다):

```kotlin
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageBorderRequest
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageBorderResponse
```

```kotlin
    @Test
    fun updateToppingBorder_solid_sendsColorAndWidth() = runTest {
        // Given 서버가 저장된 테두리를 돌려준다
        val request = slot<UpdateParfaitImageBorderRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = capture(request),
            )
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = UpdateParfaitImageBorderResponse(
                parfaitImageId = 3L,
                borderType = "SOLID",
                borderColor = "#FF0000",
                borderWidth = 4.0,
            ),
        )

        // When SOLID 테두리로 바꾼다
        val vo = dataSource.updateToppingBorder(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
            border = ToppingBorder.Solid(color = "#FF0000", width = 4.0),
        ).getOrThrow()

        // Then sealed 가 평면 3필드로 펴져 나가고 응답이 sealed 로 복원된다
        assertEquals("SOLID", request.captured.borderType)
        assertEquals("#FF0000", request.captured.borderColor)
        assertEquals(4.0, request.captured.borderWidth)
        assertEquals(ParfaitImageId(3L), vo.parfaitImageId)
        assertEquals(ToppingBorder.Solid(color = "#FF0000", width = 4.0), vo.border)
    }

    @Test
    fun updateToppingBorder_none_sendsNullColorAndWidth() = runTest {
        // Given 테두리를 없앤다
        val request = slot<UpdateParfaitImageBorderRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = capture(request),
            )
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = UpdateParfaitImageBorderResponse(
                parfaitImageId = 3L,
                borderType = "NONE",
                borderColor = null,
                borderWidth = null,
            ),
        )

        // When NONE 으로 바꾼다
        val vo = dataSource.updateToppingBorder(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
            border = ToppingBorder.None,
        ).getOrThrow()

        // Then 색·두께를 보내지 않는다
        assertEquals("NONE", request.captured.borderType)
        assertNull(request.captured.borderColor)
        assertNull(request.captured.borderWidth)
        assertEquals(ToppingBorder.None, vo.border)
    }

    @Test
    fun updateToppingBorder_solidResponseMissingWidth_fallsBackToNone() = runTest {
        // Given 서버가 SOLID 라면서 두께를 빠뜨렸다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = any(),
            )
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = UpdateParfaitImageBorderResponse(
                parfaitImageId = 3L,
                borderType = "SOLID",
                borderColor = "#FF0000",
                borderWidth = null,
            ),
        )

        // When 테두리를 바꾼다
        val vo = dataSource.updateToppingBorder(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
            border = ToppingBorder.Solid(color = "#FF0000", width = 4.0),
        ).getOrThrow()

        // Then Solid 를 만들 수 없으므로 None 으로 떨어진다
        assertEquals(ToppingBorder.None, vo.border)
    }

    @Test
    fun updateToppingBorder_notOwned_returnsBusinessFailure() = runTest {
        // Given 본인이 배치한 토핑이 아니다(그룹 미참여도 같은 코드다)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = any(),
            )
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_OWNED",
            message = "본인이 배치한 토핑이 아닙니다",
            data = null,
        )

        // When 테두리를 바꾼다
        val result = dataSource.updateToppingBorder(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
            border = ToppingBorder.None,
        )

        // Then 서버 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("PARFAIT_IMAGE_NOT_OWNED", error.code)
    }

    @Test
    fun deleteTopping_serviceReturnsSuccess_returnsUnit() = runTest {
        // Given 서버가 200 과 빈 data 를 준다
        coEvery {
            parfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
            )
        } returns ApiResponse(success = true, code = "SUCCESS", message = "성공", data = null)

        // When 토핑을 지운다
        val result = dataSource.deleteTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
        )

        // Then data 가 없어도 성공이다 — envelope 는 오지만 payload 가 없는 경로다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun deleteTopping_alreadyDeleted_returnsBusinessFailure() = runTest {
        // Given 이미 지운 배치를 다시 지운다(삭제는 멱등이 아니다)
        coEvery {
            parfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
            )
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_FOUND",
            message = "존재하지 않는 배치입니다",
            data = null,
        )

        // When 토핑을 지운다
        val result = dataSource.deleteTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
        )

        // Then 두 번째 호출은 404 다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("PARFAIT_IMAGE_NOT_FOUND", error.code)
    }
```

- [x] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitImageRemoteDataSourceImplTest*"`
Expected: **컴파일 실패** — `UpdateParfaitImageBorderRequest`·`updateToppingBorder`·`deleteTopping`이 없다.

- [x] **Step 3: 요청·응답 DTO 작성**

`UpdateParfaitImageBorderRequest.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 테두리 수정 요청.
 *
 * 위치 수정(UpdateParfaitImageRequest)과 달리 부분 병합이 아니다 — 서버가 세 필드를
 * 통째로 덮는다. borderType 이 SOLID 인데 색·두께가 없으면 400 INVALID_BORDER 다.
 */
@Serializable
data class UpdateParfaitImageBorderRequest(
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
)
```

`UpdateParfaitImageBorderResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 테두리 수정 응답. 이 도메인에서 테두리를 되돌려주는 유일한 응답이다 —
 * 배치 확정·위치 수정 둘 다 테두리 필드가 없다(`api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImageBorderResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
)
```

- [x] **Step 4: `ParfaitImageService`에 함수 2개 추가**

기존 두 함수는 그대로 두고 인터페이스 본문 끝에 아래를 더한다. import에 `UpdateParfaitImageBorderRequest`·`UpdateParfaitImageBorderResponse`·`retrofit2.http.DELETE`를 추가한다.

```kotlin
    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}/border")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitImageId") parfaitImageId: Long,
        @Body request: UpdateParfaitImageBorderRequest,
    ): ApiResponse<UpdateParfaitImageBorderResponse>

    /**
     * 성공이 204 가 아니라 200 + data: null 이다 — 회원 탈퇴(DELETE /users/me)와 달리
     * envelope 가 온다(`api/conventions.md`).
     */
    @DELETE("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}")
    suspend fun deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitImageId") parfaitImageId: Long,
    ): ApiResponse<Unit>
```

- [x] **Step 5: 매퍼 2개 추가**

`data/source/parfaitimage/mapper/VOMapper.kt` 끝에 아래를 더한다. import에 `UpdateParfaitImageBorderRequest`·`UpdateParfaitImageBorderResponse`·`UpdatedToppingBorderVO`를 추가한다.

```kotlin
internal fun ToppingBorder.toUpdateBorderRequest(): UpdateParfaitImageBorderRequest {
    val solid = this as? ToppingBorder.Solid
    return UpdateParfaitImageBorderRequest(
        borderType = when (this) {
            ToppingBorder.None -> BORDER_TYPE_NONE
            is ToppingBorder.Solid -> BORDER_TYPE_SOLID
        },
        borderColor = solid?.color,
        borderWidth = solid?.width,
    )
}

internal fun UpdateParfaitImageBorderResponse.toUpdatedToppingBorderVO(): UpdatedToppingBorderVO =
    UpdatedToppingBorderVO(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        border = toToppingBorder(),
    )

/**
 * SOLID 인데 색이나 두께가 비어 있으면 Solid 를 만들 수 없으므로 None 으로 떨어뜨린다.
 */
private fun UpdateParfaitImageBorderResponse.toToppingBorder(): ToppingBorder {
    if (borderType != BORDER_TYPE_SOLID) return ToppingBorder.None
    val color = borderColor ?: return ToppingBorder.None
    val width = borderWidth ?: return ToppingBorder.None
    return ToppingBorder.Solid(color = color, width = width)
}
```

- [x] **Step 6: DataSource 인터페이스에 함수 2개 추가**

`ParfaitImageRemoteDataSource.kt`의 인터페이스 본문 끝에 아래를 더한다. import에 `UpdatedToppingBorderVO`를 추가한다.

```kotlin
    /**
     * 배치된 토핑의 테두리를 바꾼다.
     *
     * 위치 수정과 달리 부분 병합이 아니라 통째 덮기다 — 그래서 nullable 파라미터가 아니라
     * ToppingBorder 하나를 받는다. sealed 라 SOLID 인데 색·두께가 빠지는 조합을 만들 수 없고,
     * 그래서 400 INVALID_BORDER 는 앱에서 도달 불가다.
     *
     * 그룹 미참여도 본인 배치가 아닐 때와 같은 코드(PARFAIT_IMAGE_NOT_OWNED, 403)가 온다.
     */
    suspend fun updateToppingBorder(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO>

    /**
     * 배치된 토핑을 지운다. 되돌릴 수 없다.
     *
     * 서버가 배치 행을 지우면서 이미지 참조 수를 줄이고, 그것이 0이 되면 S3 객체까지 지운다
     * (`api/parfait-image.md`). 멱등이 아니라 같은 배치를 두 번 지우면 404 다.
     */
    suspend fun deleteTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit>
```

- [x] **Step 7: 구현체에 함수 2개 추가**

`ParfaitImageRemoteDataSourceImpl.kt`의 클래스 본문 끝에 아래를 더한다. import에 `toUpdateBorderRequest`·`toUpdatedToppingBorderVO`·`UpdatedToppingBorderVO`를 추가한다.

```kotlin
    override suspend fun updateToppingBorder(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                parfaitImageId = parfaitImageId.value,
                request = border.toUpdateBorderRequest(),
            )
        },
        transform = { it.toUpdatedToppingBorderVO() },
    )

    override suspend fun deleteTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit> = apiCaller.safeApiCallWithoutData {
        parfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
            groupId = groupId.value,
            parfaitId = parfaitId.value,
            parfaitImageId = parfaitImageId.value,
        )
    }
```

> `safeApiCallWithoutData`는 여기가 **첫 프로덕션 소비처**다. 성공 응답에 `data`가 없으므로
> `safeApiCall`을 쓰면 `ApiException.EmptyBody`로 실패 처리된다 — 진입점을 바꾸면 안 된다.

- [x] **Step 8: 테스트 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitImageRemoteDataSourceImplTest*"`
Expected: PASS (기존 케이스 + 신규 6케이스)

- [x] **Step 9: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt \
        data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageBorderRequest.kt \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImageBorderResponse.kt \
        data/src/main/java/com/teamyg/parfait/data/source/parfaitimage \
        data/src/test/java/com/teamyg/parfait/data/source/parfaitimage
git commit -m "feat(data): 토핑 테두리 수정·삭제 API 배선"
```

---

### Task 5: 회원 탈퇴

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/MemberService.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSource.kt`(+`Impl`)
- Test: `data/src/test/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: 없음(기존 `ApiCaller.safeApiCallNoContent`)
- Produces:
  - `MemberService.deleteUsersMe()` — 반환 타입 없음(`Unit`)
  - `MemberRemoteDataSource.withdraw(): Result<Unit>`

- [x] **Step 1: 실패하는 테스트를 기존 파일 끝에 추가**

`MemberRemoteDataSourceImplTest.kt`의 클래스 본문 끝에 아래를 붙인다. import에 `io.mockk.coJustRun`·`retrofit2.HttpException`·`retrofit2.Response`·`okhttp3.ResponseBody.Companion.toResponseBody`가 없으면 추가한다.

```kotlin
    @Test
    fun withdraw_serviceReturnsNoContent_returnsSuccess() = runTest {
        // Given 서버가 204 를 준다(본문 없음 — envelope 자체가 오지 않는다)
        coJustRun { memberService.deleteUsersMe() }

        // When 탈퇴한다
        val result = dataSource.withdraw()

        // Then 파싱할 본문이 없어도 성공이다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun withdraw_serviceThrowsHttpException_returnsFailure() = runTest {
        // Given 서버가 401 을 준다
        coEvery { memberService.deleteUsersMe() } throws HttpException(
            Response.error<Unit>(401, "".toResponseBody(null)),
        )

        // When 탈퇴한다
        val result = dataSource.withdraw()

        // Then ApiException 으로 번역돼 흐른다
        assertTrue(result.isFailure)
        assertIs<ApiException>(result.exceptionOrNull())
    }
```

- [x] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*MemberRemoteDataSourceImplTest*"`
Expected: **컴파일 실패** — `deleteUsersMe`·`withdraw`가 없다.

- [x] **Step 3: `MemberService`에 함수 추가**

인터페이스 본문 끝에 아래를 더한다. import에 `retrofit2.http.DELETE`를 추가한다.

```kotlin
    /**
     * 회원 탈퇴. 성공이 204 이고 본문이 없어 ApiResponse 를 반환하지 않는다 —
     * logout 과 같은 모양이다. 회원이 없어도 204 라 멱등이고 도메인 에러가 없다.
     */
    @DELETE("api/v1/users/me")
    suspend fun deleteUsersMe()
```

- [x] **Step 4: DataSource 인터페이스에 함수 추가**

`MemberRemoteDataSource.kt`의 인터페이스 본문 끝에 아래를 더한다.

```kotlin
    /**
     * 회원 탈퇴. 되돌릴 수 없다.
     *
     * 서버가 회원 행을 지우고 참여 중인 모든 그룹 멤버십을 탈퇴 처리하며(그룹 닉네임이
     * "(알수없음)"으로 바뀐다) 커밋 후 refresh token 을 정리한다. 다만 그 회원이 올린
     * 토핑은 캔버스에 남는다(`api/member.md`).
     *
     * 성공 응답에 envelope 가 없다(204) — 서버 전체에서 logout 과 이 API 둘뿐이다.
     */
    suspend fun withdraw(): Result<Unit>
```

- [x] **Step 5: 구현체에 함수 추가**

`MemberRemoteDataSourceImpl.kt`의 클래스 본문 끝에 아래를 더한다.

```kotlin
    override suspend fun withdraw(): Result<Unit> = apiCaller.safeApiCallNoContent {
        memberService.deleteUsersMe()
    }
```

- [x] **Step 6: 테스트 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*MemberRemoteDataSourceImplTest*"`
Expected: PASS (기존 케이스 + 신규 2케이스)

- [x] **Step 7: 전체 검증**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: BUILD SUCCESSFUL. 실패하면 **먼저 `./gradlew clean`을 한 번 시도**한다(직전 라운드에서 다른 브랜치 빌드 잔재로 Hilt 컴파일이 깨진 선례가 있다).

- [x] **Step 8: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/MemberService.kt \
        data/src/main/java/com/teamyg/parfait/data/source/member \
        data/src/test/java/com/teamyg/parfait/data/source/member
git commit -m "feat(data): 회원 탈퇴 API 배선"
```

---

### Task 6: `http/` 요청 모음 보강

**Files:**
- Modify: `http/parfait.http`
- Modify: `http/parfait-image.http`
- Modify: `http/users.http`
- Modify: `http/http-client.env.json`
- Modify: `http/README.md`

**Interfaces:**
- Consumes: 없음(코드와 독립)
- Produces: 없음

> 이 Task는 코드가 아니라 사람이 서버에 직접 쏘는 요청 모음이다. 자동 테스트가 없고
> **검증은 파일 내 변수명이 `http-client.env.json`에 실재하는지 대조**로 한다.

- [x] **Step 1: `http/parfait.http`에 요청 2개 추가**

파일 끝에 아래를 붙인다.

```
### 오늘의 캔버스 조회
# C-001 캔버스 메인이 그릴 것 전부가 한 응답에 있다 — 상태·멤버 목록·배경·배치 토핑.
# 배치 목록 전용 API 는 없고 이 엔드포인트가 그 자리를 대신한다.
#
# ⚠️ 조회인데 서버가 캔버스를 만든다. 오늘 날짜 파르페가 없으면 생성해 저장하므로,
#    이 요청을 한 번 쏘면 연도 목록·과거 목록에도 그날이 나타난다.
# ⚠️ images 는 0건일 때 빈 배열이 아니라 null 이다. background 도 미설정이면 null 이다.
# ⚠️ lastClosedDate 는 CLOSED 만 센다 — 토핑 0건으로 마감된 날(EMPTY)은 잡히지 않는다.
GET {{base_url}}/api/v1/groups/{{group_id}}/parfaits/today
Authorization: Bearer {{access_token}}

> {%
    client.test("200", function() {
        client.assert(response.status === 200, "status: " + response.status);
    });
    client.global.set("parfait_id", response.body.data.parfaitId);
    client.log("status=" + response.body.data.status +
        " toppings=" + (response.body.data.images === null ? "null(0건)" : response.body.data.images.length));
%}

### 과거 캔버스 목록
# 범위를 생략하면 서버 기본값(오늘 - 30일 ~ 오늘)이다. 페이지네이션도 상한도 없다.
#
# ⚠️ thumbnailUrl 은 항상 null 이다 — 서버에 채우는 코드가 없다.
# ⚠️ 0건이면 빈 배열이다. 위 today 의 images 가 null 인 것과 반대다.
GET {{base_url}}/api/v1/groups/{{group_id}}/parfaits
Authorization: Bearer {{access_token}}

> {%
    client.test("200", function() {
        client.assert(response.status === 200, "status: " + response.status);
    });
    client.log("count=" + response.body.data.parfaits.length);
%}

### (대조용) from 이 to 보다 늦으면 400 INVALID_DATE_RANGE
GET {{base_url}}/api/v1/groups/{{group_id}}/parfaits?from=2026-08-20&to=2026-08-15
Authorization: Bearer {{access_token}}

> {%
    client.test("400 INVALID_DATE_RANGE", function() {
        client.assert(response.status === 400, "status: " + response.status);
        client.assert(response.body.code === "INVALID_DATE_RANGE", "code: " + response.body.code);
    });
%}
```

- [x] **Step 2: `http/parfait-image.http`에 요청 2개 추가**

파일 끝에 아래를 붙인다.

```
### 토핑 테두리 두께/색깔 수정
# 위치 수정과 달리 부분 병합이 아니다 — 세 필드를 통째로 덮는다.
# 이 응답이 서버가 테두리를 돌려주는 유일한 자리다(배치 확정·위치 수정 응답에는 없다).
#
# ⚠️ borderType 이 SOLID 인데 borderColor 나 borderWidth 가 없으면 400 INVALID_BORDER 다.
PATCH {{base_url}}/api/v1/groups/{{group_id}}/parfaits/{{parfait_id}}/images/{{parfait_image_id}}/border
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "borderType": "SOLID",
  "borderColor": "#FF0000",
  "borderWidth": 4.0
}

> {%
    client.test("200", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.data.borderType === "SOLID", "borderType: " + response.body.data.borderType);
    });
%}

### 토핑 삭제
# 되돌릴 수 없다. 서버가 배치 행을 지우면서 이미지 참조 수를 줄이고,
# 그것이 0이 되면 S3 객체까지 지운다.
#
# ⚠️ 성공이 204 가 아니라 200 + data: null 이다. 회원 탈퇴(users.http)는 204 라 서로 다르다.
# ⚠️ 멱등이 아니다 — 같은 요청을 두 번 보내면 두 번째는 404 PARFAIT_IMAGE_NOT_FOUND 다.
DELETE {{base_url}}/api/v1/groups/{{group_id}}/parfaits/{{parfait_id}}/images/{{parfait_image_id}}
Authorization: Bearer {{access_token}}

> {%
    client.test("200 + data null", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.data === null, "data: " + JSON.stringify(response.body.data));
    });
%}
```

- [x] **Step 3: `http/parfait-image.http` 머리말과 기존 요청의 `parfaitId` 리터럴 교체**

파일 상단 주석의 아래 두 줄을

```
# ⚠️ parfaitId 를 얻을 조회 API 가 서버에 없다. 아래 요청은 리터럴 1 을 쓴다 -
#    실제 값은 DB 나 서버팀에서 받아 손으로 바꾼다.
```

아래로 바꾼다(선행 목록의 3번 다음에 4번이 생긴다).

```
#    4) parfait.http 의 "오늘의 캔버스 조회"를 돌려 parfait_id 를 채운다
#       (2026-08-15 이전에는 조회 API 가 없어 리터럴 1 을 손으로 바꿔야 했다)
```

그리고 **이 파일의 모든 요청 URL에서 `/parfaits/1/`을 `/parfaits/{{parfait_id}}/`로 바꾼다.**
기존 배치·수정 요청 4개가 리터럴 `1`을 쓰고 있다.

Run: `grep -n "parfaits/1/" http/parfait-image.http`
Expected: 교체 후 결과 0줄.

- [x] **Step 4: `http/users.http`에 탈퇴 요청 추가**

파일 끝에 아래를 붙인다.

```
### 7. 회원 탈퇴
# ⚠️ 되돌릴 수 없다. 이 요청을 보내면 계정이 삭제되고 참여 중인 모든 그룹에서 탈퇴 처리된다.
#    이후 access_token 이 무효가 되므로 auth.http 로 다시 로그인해야 한다.
#
# ⚠️ 성공이 204 이고 본문이 없다 — 서버 전체에서 envelope 없는 응답은 logout 과 이것뿐이다.
# ⚠️ 회원이 이미 없어도 204 다(멱등). 도메인 에러가 없다.
# ⚠️ 올려 둔 토핑은 캔버스에 남고 배치자 이름이 "(알수없음)"으로 바뀐다.
DELETE {{base_url}}/api/v1/users/me
Authorization: Bearer {{access_token}}

> {%
    client.test("204 본문 없음", function() {
        client.assert(response.status === 204, "status: " + response.status);
    });
%}
```

- [x] **Step 5: `http/http-client.env.json`에 `parfait_id` 등재**

`parfait_image_id` 옆에 빈 값으로 더한다. 런타임에 스크립트가 채우는 값이지만 **형제 변수
(`group_id`·`image_*`·`parfait_image_id`)가 전부 같은 이유로 등재돼 있다** — 처음 쓰는 사람이
미해결 `{{parfait_id}}`를 설정 누락으로 오해하지 않게 하는 것이 목적이다(직전 라운드 결정).

```json
    "parfait_image_id": "",
    "parfait_id": ""
```

`_reset.http`의 도메인별 비우기 항목에도 `parfait_id`를 짝으로 더한다(파일에서 `parfait_image_id`를
비우는 항목을 찾아 같은 자리에 넣는다).

- [x] **Step 6: `http/README.md` 세 줄 갱신**

① 파일 목록 표의 세 줄을 아래로 바꾼다.

```
| `parfait.http` | 그룹 캘린더 연도 리스트 · **오늘의 캔버스** · **과거 캔버스 목록** |
| `users.http` | 내 계정 조회 · 전역 닉네임 변경 · **탈퇴**(선행: `auth.http`만) |
| `parfait-image.http` | 토핑 배치 확정 · 위치/크기/각도 수정 · **테두리 수정** · **삭제**(**선행이 넷** — `auth.http` → `parfait-group.http` → `images.http` → `parfait.http`) |
```

② 준비 순서를 설명하는 문단에서 `parfaitId`를 손으로 바꾼다는 문장을 아래로 바꾼다.

```
`parfaitId`는 `parfait.http`의 "오늘의 캔버스 조회"가 응답 핸들러로 `parfait_id`에 채워 준다.
```

③ 변수 구조 블록(`parfait_image_id`가 있는 JSON)과 그 아래 "손으로 채우는 값이 아니다" 문장의
변수 열거에 `parfait_id`를 더한다.

- [x] **Step 7: 변수 실재 확인**

Run: `grep -oh "{{[a-z_]*}}" http/*.http | sort -u`
Expected: 출력된 변수 전부가 `http/http-client.env.json`에 있어야 한다. 특히 `{{parfait_id}}`가
Step 5로 등재됐는지 확인한다.

- [x] **Step 8: 커밋**

```bash
git add http/
git commit -m "docs(http): 캔버스 조회·토핑 테두리/삭제·탈퇴 요청 추가"
```

---

## 완료 후

- [x] **전체 검증 재실행**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **보고**

`git log --oneline origin/develop..HEAD`로 커밋 목록을 보고한다. **push·PR은 하지 않는다** — 사용자 확인 대상이다.

- [x] **위키 repo 문서 갱신은 develop 머지 후로 미룬다**

스펙의 `status: draft` → `implemented` 전환, `specs/archive/` 이동, `api/README.md`의 Android 열·표면 개수(20/25 → 25/25), `parfait/index.md` "지금 상태", open-questions OQ-P-108·OQ-P-132·OQ-P-158 갱신은 **develop 머지 후**에 한다.

## 이 계획이 다루지 않는 것

- Repository·UseCase·화면 결선 — 다음 라운드다. `today`가 배치 전량을 주므로 C-001 캔버스 결선의 서버 측 선행 조건은 이 라운드로 사라진다.
- 마감된 캔버스의 편집 차단 — 서버가 막지 않으므로 앱이 `status`로 잠가야 하는데, 어느 조작을 어디까지 잠글지 정책 소스가 없다(OQ-P-160).
- 탈퇴자 토핑(`(알수없음)`)의 표시 정책(OQ-P-163).
- 실서버 호출 — 소비처가 없어 요청을 만들 자리가 없다. `http/`가 사람이 계약을 확인하는 유일한 수단이다.
