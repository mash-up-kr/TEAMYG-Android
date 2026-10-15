---
id: parfait-canvas-topping-member-api-service-layer
title: ":data 캔버스 조회·토핑 테두리/삭제·회원 탈퇴 API Service·remote DataSource 레이어 (5 엔드포인트)"
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-15
related_code: ParfaitService, ParfaitImageService, MemberService, ParfaitRemoteDataSource, ParfaitImageRemoteDataSource, MemberRemoteDataSource, ApiCaller, ToppingBorder, ToppingTransform, GroupNickname, CheckNameValidUseCase, ServerErrorCode, GroupNickNameError
related_adr: ADR-0017
related_spec: 2026-08-11-member-parfait-image-api-service-layer, 2026-08-10-image-api-service-layer, 2026-08-03-data-api-service-layer, 2026-08-06-unit-test-infrastructure
related_architecture: data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, data, network, api, canvas, topping, member]
---

# :data 캔버스 조회·토핑 테두리/삭제·회원 탈퇴 API Service·remote DataSource 레이어

> ✅ **develop 머지 완료(PR #250, 2026-08-15)** — 브랜치 `feature/canvas-topping-member-api-260815`.
> 머지본 재대조에서 **설계와 갈린 곳 0건**이다: Service 5함수의 이름·시그니처·`@Query` 기본값,
> wire DTO 9(전 프로퍼티 `@SerialName`), domain VO 7, DataSource 5함수, 매퍼의 네 폴백
> (`images` null → 빈 목록 · 미지 배경 type → `null` · 미지 status → `UNKNOWN` · `SOLID` 불완전 → `None`),
> 결정 ⑧의 진입점 갈림(탈퇴 `safeApiCallNoContent` / 토핑 삭제 `safeApiCallWithoutData`)이 본문 그대로다.
> 예고대로 **DI 등록 줄은 한 줄도 늘지 않았다**.
>
> **이로써 Android가 쓰기로 한 서버 엔드포인트 전량에 `:data` 표면이 있다**(서버 26 − 애플 로그인 1 −
> 테스트 전용 회전 1). 다만 **소비처(Repository·UseCase·화면)는 여전히 0건**이다
> → [open-questions](../../../synthesis/open-questions.md).
>
> **as-built 확정 2건**(본문이 형태를 안 적었던 자리):
> ① 테두리 평탄화가 `toPlaceRequest`와 `toUpdateBorderRequest` 두 곳에서 필요해져
> `private fun ToppingBorder.flatten(): Triple<String, String?, Double?>`로 뽑혔다 — 배치 쪽의 기존
> `as? ToppingBorder.Solid` 캐스팅은 사라졌다. 본문은 "같은 매퍼 파일에 둔다"까지만 적었다.
> ② `http/` 보강에 `http-client.env.json`·`_reset.http`의 `parfait_id` 변수 등재가 함께 갔다
> (본문은 `today` 응답이 `parfaitId`를 준다는 것까지만 적었다) — 형제 변수가 전부 등재돼 있는 관행을 따른다.
>
> **범위 밖 동반 변경 2건**(같은 PR, 커밋 `1a6a5577`) — 같은 날 2차 서버 delta(`e4ff23f`)가 바꾼 규칙을
> 앱에 반영했다. 아래 "서버 규칙 delta 반영" 절에 적는다.
>
> 🔁 **후속 라운드가 이름 둘을 바꿨다(2026-08-16, PR #266)** — 이 스펙의 `TodayCanvasVO`는 **`CanvasVO`**,
> 매퍼 `toTodayCanvasVO()`는 **`toCanvasVO()`**다. 서버가 캔버스 상세 조회에 **같은 응답 클래스**를 쓰면서
> 한 타입이 오늘과 특정 날짜 양쪽을 담게 됐기 때문이다. 아래 본문의 결정 ①~⑧은 그대로 유효하고 이름만
> 갈렸다. 그와 함께 **"이 조회가 캔버스 행을 만든다"는 경고가 타입 KDoc에서 함수 KDoc으로 내려갔다**
> (오늘 조회만 만든다) → [후속 스펙](2026-08-16-canvas-detail-background-api-service-layer.md).

## 서버 규칙 delta 반영 (범위 밖 동반 변경)

이 스펙이 겨냥한 delta는 `36ecd1c`(엔드포인트 5건 신설)인데, 같은 PR이 그 뒤 서버 `e4ff23f`의 **규칙
변경 2건**까지 함께 반영했다. 엔드포인트 증감은 없다.

**① 자모 단독 허용 — 앱이 좁던 쪽을 넓혔다.** 서버 닉네임·그룹명 정규식이 자모 범위를 얻었으므로
`CheckNameValidUseCase`의 `CheckValidCharacter`에 `'ㄱ'..'ㅎ'`·`'ㅏ'..'ㅣ'`를 더했다. 그대로 뒀다면
**서버가 받는 이름을 앱이 먼저 막는다.** KDoc의 "서버보다 느슨하면 안 된다"에 "좁아도 안 된다"가
나란히 붙었고, `CheckNameValidUseCaseTest`의 자모 케이스가 `InvalidCharacter` 기대에서 `Success`
기대로 뒤집히며 모음 단독·완성형 혼용 단언이 붙었다.

**② 그룹 내 닉네임 중복 허용 — 死코드 제거.** 서버가 `GROUP_NICKNAME_ALREADY_USED`를 통째로 지워
앱의 대응 경로가 전부 도달 불가가 됐다. 제거 대상은 넷이다 — `ServerErrorCode.ParfaitGroup` 상수,
`GroupNickNameError.ALREADY_USED`(+`toStringResource` 분기), `feature/groups/enter/impl` `strings.xml`
문구 1건, `GroupNickNameViewModel`의 매핑 분기. `GroupNickNameViewModelTest`·
`ParfaitGroupRepositoryImplTest`의 해당 케이스와 화면 프리뷰 provider의 에러 케이스는 삭제가 아니라
`INVALID_GROUP_NICKNAME`(400)으로 **바꿔 살렸다**. `ServerErrorCode.ParfaitGroup.INVALID_GROUP_NAME`의
KDoc 정규식도 자모 포함본으로 정정됐다.

> ⚠️ **둘 다 정책 근거가 서버 커밋 메시지뿐이다.** 위키 [[이름-입력-규칙]]·[[그룹]]에는 자모 허용도
> 그룹 내 닉네임 중복 허용도 항목이 없다 → [open-questions](../../../synthesis/open-questions.md).

서버 기준선 `36ecd1c`가 들여온 5 엔드포인트를 `:data`의 Retrofit Service와 remote DataSource로
구현하고 대응 domain VO를 만든다. 계약 정본은 [api/parfait.md](../../../api/parfait.md)·
[api/parfait-image.md](../../../api/parfait-image.md)·[api/member.md](../../../api/member.md).

| 엔드포인트 | 도메인 |
|---|---|
| `GET /api/v1/groups/{groupId}/parfaits/today` | parfait |
| `GET /api/v1/groups/{groupId}/parfaits` | parfait |
| `PATCH .../parfaits/{parfaitId}/images/{parfaitImageId}/border` | parfait-image |
| `DELETE .../parfaits/{parfaitId}/images/{parfaitImageId}` | parfait-image |
| `DELETE /api/v1/users/me` | member |

**앞선 라운드들이 세운 관용구의 증분이다.** 계층·이름 규칙·타입 경계의 정본은
[2026-08-03-data-api-service-layer](2026-08-03-data-api-service-layer.md)이고,
[2026-08-11-member-parfait-image-api-service-layer](2026-08-11-member-parfait-image-api-service-layer.md)가
직전 적용례다. 이 스펙은 **규칙이 답하지 않는 지점만** 새로 결정한다 — 이번엔 `today` 응답이다.
한 응답 안에 캔버스 상태·멤버 목록·배경·배치된 토핑 전량이 3층으로 중첩돼 있고, 지금까지의
응답은 전부 평면이었다.

## 범위

**포함** — Service 5함수 · 요청/응답 DTO 9(`GetTodayParfaitResponse` 계열 5 + `PastParfaitsResponse`
계열 2 + 테두리 요청/응답 2) · `ParfaitRemoteDataSource`(+2)·`ParfaitImageRemoteDataSource`(+2)·
`MemberRemoteDataSource`(+1) · `source/parfait/mapper/VOMapper.kt` 신설 · domain VO 7 ·
유닛 테스트(신규 1 + 보강 2) · `http/` 요청 3파일 보강.

**제외** — Repository·`domain/repository` 인터페이스, UseCase, 화면 결선, 서버 에러 코드의 도메인
예외 번역, 요청 전 클라이언트 측 유효성 검사. 기존 5라운드와 같은 경계다.

**DI 등록은 줄이 늘지 않는다.** 세 Service와 세 remote DataSource가 이미 `ServiceModule`·
`RemoteDataSourceModule`에 등록돼 있다 — 기존 인터페이스에 함수를 더하는 라운드라서다.
이전 다섯 라운드와 다른 점이고, 그만큼 `:app:assembleDebug` 게이트의 의미는 줄어든다(그래도 돌린다).

**테스트 전용 회전 엔드포인트(`POST /api/v1/test/parfait-canvas/rotate`)는 범위 밖이고 앞으로도
아니다.** 서버가 컨트롤러와 화이트리스트 양쪽에 프로덕션 오픈 전 제거 TODO를 달아 둔 임시 경로다
([api/parfait.md](../../../api/parfait.md), OQ-P-159). 앱 대응 심볼도 `http/` 요청도 만들지 않는다.

## 계층과 배치

`Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. 기존과 동일하다.

| 위치 | 신규 |
|---|---|
| `data/service/model/response/parfait/` | `GetTodayParfaitResponse`(+`GroupMemberResponse`·`BackgroundResponse`·`TodayParfaitImageResponse`·`PlacedByResponse`) · `PastParfaitsResponse`(+`PastParfaitResponse`) |
| `data/service/model/request/parfaitimage/` | `UpdateParfaitImageBorderRequest` |
| `data/service/model/response/parfaitimage/` | `UpdateParfaitImageBorderResponse` |
| `data/source/parfait/mapper/` | `VOMapper.kt` (이 도메인 첫 매퍼 — 지금까지 응답이 `years` 한 필드라 없었다) |
| `domain/model/canvas/` | `TodayCanvasVO` · `CanvasStatus` · `CanvasBackground` · `CanvasMemberVO` · `CanvasToppingVO` · `PastCanvasVO` |
| `domain/model/topping/` | `UpdatedToppingBorderVO` |

Service 함수명은 규칙이 기계적이라 결정거리가 아니다 — `<method><PathSegmentsCamelCase>`.
`patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder`는 길지만 규칙의 답이고,
직전 라운드의 `patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId`가 이미 같은 길이다.

**중첩 응답 DTO는 상위 응답 파일 안에 함께 둔다.** `PlaceParfaitImageResponse.kt`가
`PlacedByResponse`를 같은 파일에 담은 선례를 따른다 — 서버가 한 파일에 담은 것을 앱도 한 파일에 담아야
계약 문서와 눈으로 대조된다.

⚠️ **`PlacedByResponse`라는 이름이 두 패키지에 생긴다**(`response/parfait`·`response/parfaitimage`).
서버도 같은 이름을 두 DTO 파일에 두었고, **wire DTO는 서버의 거울**이라는 규약이 이름을 바꾸지 말라고
답한다. 두 곳을 동시에 import하는 자리는 생기지 않는다(매퍼가 도메인별로 갈려 있다).

## parfait

### Service

```kotlin
@GET("api/v1/groups/{groupId}/parfaits/today")
suspend fun getGroupsByGroupIdParfaitsToday(
    @Path("groupId") groupId: Long,
): ApiResponse<GetTodayParfaitResponse>

@GET("api/v1/groups/{groupId}/parfaits")
suspend fun getGroupsByGroupIdParfaits(
    @Path("groupId") groupId: Long,
    @Query("from") from: String? = null,
    @Query("to") to: String? = null,
): ApiResponse<PastParfaitsResponse>
```

둘 다 서버 화이트리스트 밖이라 access token이 필요하다 — `@NoAuth`를 붙이지 않는다.

**`@Query`가 `null`이면 Retrofit이 파라미터를 URL에서 통째로 뺀다.** 그래서 서버 기본값
(`to`=오늘, `from`=`to − 30일`)이 그대로 산다. `kotlinx.datetime.LocalDate.toString()`이 ISO-8601이라
별도 포맷터를 두지 않는다 — 문자열 변환은 DataSource가 한다(DTO는 서버가 보는 문자열 그대로).

### domain

```kotlin
enum class CanvasStatus { ACTIVE, CLOSED, EMPTY, UNKNOWN }

sealed interface CanvasBackground {
    @JvmInline value class Color(val value: String) : CanvasBackground
    @JvmInline value class Image(val url: String) : CanvasBackground
}

data class CanvasMemberVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
)

data class CanvasToppingVO(
    val parfaitImageId: ParfaitImageId,
    val imageId: ImageId,
    val imageUrl: String,
    val transform: ToppingTransform,
    val border: ToppingBorder,
    val placedBy: ToppingPlacerVO,
    val createdAt: LocalDateTime,
)

data class TodayCanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val status: CanvasStatus,
    val lastClosedDate: LocalDate?,
    val members: List<CanvasMemberVO>,
    val background: CanvasBackground?,
    val toppings: List<CanvasToppingVO>,
)

data class PastCanvasVO(
    val parfaitId: ParfaitId,
    val date: LocalDate,
    val thumbnailUrl: String?,
    val toppingCount: Int,
)
```

`LocalDate`·`LocalDateTime`은 `kotlinx.datetime`이다 — `MyParfaitGroupVO.recentImageUploadedAt`이
같은 타입이고 매퍼가 `LocalDateTime::parse`를 쓰는 선례가 있다.

### DataSource

```kotlin
suspend fun getTodayCanvas(groupId: GroupId): Result<TodayCanvasVO>

suspend fun getPastCanvases(
    groupId: GroupId,
    from: LocalDate? = null,
    to: LocalDate? = null,
): Result<List<PastCanvasVO>>
```

### 결정 5건

**① 배치 토핑에 상태 전용 타입을 신설한다.** `CanvasToppingVO`는 기존 `PlacedToppingVO`와 필드가
겹치지만 **테두리와 생성시각이 더 있다.** 하나로 합치면 둘 중 하나를 치른다 — POST 응답에 없는 값을
요청 값으로 지어내거나, 두 필드를 nullable로 만들어 "모른다"와 "없다"를 같은 `null`로 뭉갠다.
직전 라운드 결정 ⑤("서버가 안 돌려주는 것을 지어내지 않는다")의 연장이다. **공통 조각
(`ToppingTransform`·`ToppingBorder`·`ToppingPlacerVO`)은 그대로 재사용**하므로 중복은 필드 목록뿐이고
불변식은 한 곳에 있다.

**② `toppings`만 `emptyList()`로 접고 나머지 `null`은 유지한다.** 서버는 `null`로 세 가지 다른 것을
말한다 — `images`=0건, `background`=미설정, `lastClosedDate`=마감 이력 없음. **0건과 빈 목록은 같은
뜻**이라 소비처가 `?: emptyList()`를 반복할 이유가 없다. 나머지 둘은 "없음"이 의미 있는 상태라
nullable을 유지한다. 서버가 나중에 빈 배열로 바꿔도 이 매핑은 그대로 맞는다.

**③ `CanvasBackground`는 sealed이고 미지 type은 `null`로 접는다.** `value`의 뜻이 `type`에 따라
갈린다(색 문자열 vs 이미지 URL) — `ToppingBorder`에서 sealed를 택한 것과 같은 이유다. 미지 type을
`Unknown` 케이스로 보관하는 안은 기각했다: 소비처가 쓸 수 없는 분기를 계속 만난다. **미지 type과
미설정은 화면에서 같은 처리**(배경 없음)라 같은 값으로 접는 것이 정확하다.

기존 enum 폴백 관용구(`PolicyType`·`ImageStatus`·`LoginProvider`의 `UNKNOWN`)는 **`CanvasStatus`에만**
적용한다 — 그쪽은 값 자체가 상태라 버릴 수 없다.

**④ domain 이름은 제품 언어를 따른다.** 서버 `parfait`(캔버스)는 domain에서 `Canvas`다 —
직전 라운드 결정 ④(`ParfaitImage` → `Topping`)와 같은 축이다. 위키·화면 기획이 C-001을 "캔버스"로
부르고 "파르페"는 그룹 목록의 그래픽 메타포를 가리킨다. **다만 `ParfaitId`는 그대로 쓴다** — 이미
`domain/model/id/`에 있고 토핑 배치 API가 그 타입을 받는다. 즉 **id 타입은 서버 언어, VO는 제품
언어**이고, 그 경계는 직전 라운드가 이미 그은 것이다(`ParfaitImageId` × `PlacedToppingVO`).

**⑤ 과거 목록의 `thumbnailUrl`을 그대로 노출한다.** 서버가 항상 `null`을 넣는다
([api/parfait.md](../../../api/parfait.md), OQ-P-161). 필드를 빼면 서버가 채우기 시작할 때 계약이 갈리고,
가짜 값을 넣으면 없는 것을 지어내는 것이다. **서버 응답 필드 이름 `imageCount`는 domain에서
`toppingCount`로 바꾼다** — ④의 이름 축을 따른다.

## parfait-image

### Service

```kotlin
@PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}/border")
suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
    @Path("groupId") groupId: Long,
    @Path("parfaitId") parfaitId: Long,
    @Path("parfaitImageId") parfaitImageId: Long,
    @Body request: UpdateParfaitImageBorderRequest,
): ApiResponse<UpdateParfaitImageBorderResponse>

@DELETE("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}")
suspend fun deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
    @Path("groupId") groupId: Long,
    @Path("parfaitId") parfaitId: Long,
    @Path("parfaitImageId") parfaitImageId: Long,
): ApiResponse<Unit>
```

### domain

```kotlin
data class UpdatedToppingBorderVO(
    val parfaitImageId: ParfaitImageId,
    val border: ToppingBorder,
)
```

### DataSource

```kotlin
suspend fun updateToppingBorder(
    groupId: GroupId,
    parfaitId: ParfaitId,
    parfaitImageId: ParfaitImageId,
    border: ToppingBorder,
): Result<UpdatedToppingBorderVO>

suspend fun deleteTopping(
    groupId: GroupId,
    parfaitId: ParfaitId,
    parfaitImageId: ParfaitImageId,
): Result<Unit>
```

### 결정 2건

**⑥ 테두리 수정은 `ToppingBorder` 하나를 받는다 — nullable 3파라미터가 아니다.** 서버가 세 필드를
**통째로 덮기 때문**이다(부분 병합이 아니다). 위치 PATCH가 nullable 5파라미터인 것과 비대칭인데,
그 비대칭은 **서버 계약의 비대칭**이지 앱이 만든 것이 아니다 — 직전 라운드 결정 ②와 같은 판단
기준이다. sealed로 받으면 `SOLID`인데 색·두께가 빠지는 조합이 표현 불가능해져 400 `INVALID_BORDER`가
앱에서 도달 불가가 된다.

sealed → 평면 3필드를 펴는 자리는 기존 `source/parfaitimage/mapper/VOMapper.kt`다(`toPlaceRequest`
선례). `None` → `("NONE", null, null)`, `Solid` → 셋 다 실린다. **응답을 sealed로 되돌리는 변환은
이 라운드에서 처음 생긴다** — 지금까지는 보내기만 했다.

**⑦ 응답 VO를 만든다(`UpdatedToppingBorderVO`).** 응답 필드가 `parfaitImageId` + 테두리 3필드라
감쌀 것이 있다 — 필드 하나였던 `changeGlobalNickname`이 VO 없이 값 타입을 반환한 것과 갈린다.
기존 `UpdatedToppingVO(parfaitImageId, transform)`와 모양이 대칭이다. **이 응답이 앱이 테두리를
되받는 첫 자리다**(POST·위치 PATCH는 저장만 하고 돌려주지 않는다).

## member

### Service

```kotlin
@DELETE("api/v1/users/me")
suspend fun deleteUsersMe()
```

### DataSource

```kotlin
suspend fun withdraw(): Result<Unit>
```

## 결정 ⑧ — 두 DELETE가 `ApiCaller` 진입점을 달리 쓴다

서버가 성공 표현을 달리해서 앱도 갈라야 한다.

| 엔드포인트 | 서버 성공 | Service 반환 | 진입점 |
|---|---|---|---|
| 회원 탈퇴 | **204 · 본문 없음** | `Unit`(비-`ApiResponse`) | `safeApiCallNoContent` |
| 토핑 삭제 | 200 · envelope `data: null` | `ApiResponse<Unit>` | `safeApiCallWithoutData` |

탈퇴 쪽은 `postAuthLogout`이 이미 같은 모양이라 선례가 있다 — envelope가 아예 없으므로 Service
함수가 `ApiResponse`를 반환하면 역직렬화가 빈 본문에서 깨진다.

**`safeApiCallWithoutData`는 지금 死코드다**(선언과 `ApiCallerTest`의 자기 테스트뿐, OQ-P-132).
토핑 삭제가 **그 진입점의 첫 프로덕션 소비처**가 된다 — "Android가 붙일 엔드포인트가 더 없어서 죽은
코드로 확정됐다"던 판정이 서버 delta로 뒤집혔다. 진입점 넷이 전부 쓰이게 되므로 그 항목이 닫힌다.

## 계약 함정

구현자가 서버를 다시 훑지 않아도 되도록 옮겨 적는다. 근거는 [api/parfait.md](../../../api/parfait.md)·
[api/parfait-image.md](../../../api/parfait-image.md)·[api/member.md](../../../api/member.md).

1. **`GET .../today`는 조회인데 캔버스 행을 만든다.** 해당 날짜 파르페가 없으면 서버가 생성해 저장한다
   (`EnsureActiveCanvasUseCase`). 화면이 이 GET을 남발하면 빈 캔버스가 양산되고 연도·과거 목록에도
   즉시 나타난다(OQ-P-160).
2. **오늘 날짜 캔버스가 이미 마감돼 있으면 그것을 그대로 돌려준다.** `status`가 `ACTIVE`가 아닐 수 있다.
   그리고 **서버는 마감된 캔버스의 편집을 막지 않는다** — 배치·위치·테두리·삭제 넷 다 `status`를 보지
   않는다. 마감 이후를 잠그는 것은 현재 앱 책임이고 그 규칙이 아직 어디에도 없다.
3. **`lastClosedDate`는 `CLOSED`만 센다**(`EMPTY` 제외). "마지막 마감일"이 아니라 **"마지막으로 토핑이
   있던 날"**이다.
4. **`toppings[].placedBy.groupMemberId`가 `members`에 없을 수 있다.** 탈퇴·그룹 이탈 멤버의 토핑은
   남고, 그 닉네임은 `(알수없음)`으로 내려온다(서버 `GroupNickname.unknown()`). 두 목록을 조인해
   그리는 화면은 이 케이스에서 깨진다(OQ-P-163).
5. **과거 목록**: `thumbnailUrl` 항상 `null` · 기본 30일 · **상한도 페이지네이션도 없다**(범위를 크게
   주면 전량이 내려온다) · `from > to`면 400 `INVALID_DATE_RANGE` · 0건은 **빈 배열**(`today`의 `null`과
   반대다) · `ACTIVE`인 오늘 캔버스도 범위에 들면 포함된다.
6. **토핑 삭제는 멱등이 아니다** — 두 번째 호출은 404 `PARFAIT_IMAGE_NOT_FOUND`다. 그리고 **그룹
   미참여도 403 `PARFAIT_IMAGE_NOT_OWNED`**로 뭉개진다(테두리 PATCH도 같다) — "그룹에서 나갔다"와
   "남의 토핑이다"를 코드로 구분할 수 없다.
7. **삭제는 서버에서 S3 객체까지 지운다**(참조 카운트가 0이 될 때). 되돌릴 수 없고, 같은 `imageId`로
   다시 배치하면 깨진 이미지가 걸린다(OQ-P-107 ③).
8. **탈퇴는 회원이 없어도 204다**(멱등) — 도메인 에러가 없어 "이미 탈퇴됨"을 구분할 수단이 없다.
   탈퇴는 그 회원의 **모든 그룹 멤버십을 함께 정리**하고 refresh token을 지운다.
9. **테두리 PATCH에 서버 검증이 없다** — `borderWidth` 음수·과대값이 그대로 저장된다. 요청 DTO에
   Bean Validation이 없어 OpenAPI `required`도 비어 있다.

## 테스트

**매퍼 단독 테스트를 만들지 않는다**([unit-test-infrastructure](2026-08-06-unit-test-infrastructure.md)
"테스트 규약" 11). 판단이 든 변환은 그 매퍼를 통과시키는 DataSource 테스트의 케이스로 잠근다.

| 파일 | 잠그는 것 |
|---|---|
| `ParfaitRemoteDataSourceImplTest` (신규) | today 3층 중첩 매핑 · `images: null` → `emptyList()` · `background` 미지 type → `null` · `background` `COLOR`/`IMAGE` → sealed 두 갈래 · `status` 미지값 → `UNKNOWN`(대소문자 민감성 포함) · 토핑 `borderType` `NONE`/`SOLID` → sealed 복원 · 날짜·시각 파싱 · 과거 목록 `from`/`to` 생략 시 **쿼리 인자가 `null`로 전달됨** · `INVALID_DATE_RANGE`·`GROUP_NOT_JOINED` 실패 경로 |
| `ParfaitImageRemoteDataSourceImplTest` (보강) | `ToppingBorder.None` → `("NONE", null, null)` 전송 · `Solid` → 색·두께 실림 · 응답 → sealed 복원 · 삭제 성공이 `Result<Unit>` · `PARFAIT_IMAGE_NOT_FOUND`·`PARFAIT_IMAGE_NOT_OWNED` |
| `MemberRemoteDataSourceImplTest` (보강) | 탈퇴 성공(`safeApiCallNoContent` 경로) · `HttpException` → `ApiException` 변환 |

**`safeApiCallWithoutData`의 첫 소비처가 생기므로 그 경로를 도메인 테스트가 한 번 잠근다** —
지금까지는 `ApiCallerTest`만 그 진입점을 통과시켰다.

> **as-built** — 위 표대로 머지됐고 `*VOMapperTest`는 이 라운드에서 하나도 생기지 않았다. 이로써
> develop의 `XxxRemoteDataSourceImplTest`는 image·member·parfait·parfaitimage·policy 다섯이다.
> ⚠️ **`ParfaitGroupRemoteDataSourceImplTest`는 여전히 없다** — 그룹 도메인만 Repository 테스트로
> 대신하고 있어, "매퍼 테스트 케이스를 DataSource 테스트로 옮긴다"는 규약의 이행 대상 파일이
> 그 도메인에는 존재하지 않는다 → [open-questions](../../../synthesis/open-questions.md).

## http/ 요청 모음

`http/parfait.http`(+2)·`http/parfait-image.http`(+2)·`http/users.http`(+1) 보강 +
`http/README.md` 설명 갱신. 이번 구현분 5 엔드포인트를 덮으면 **25/25로 회복**된다(OQ-P-108).

`today` 요청이 `parfaitId`를 응답으로 주므로 **`parfait-image.http`가 손으로 바꾸던 `parfaitId`
리터럴을 이 응답에서 뽑아 쓴다** — 그 파일의 오래된 주석("조회 API가 없어 직접 바꿔야 함")도 함께
정정한다. 테스트 전용 회전 엔드포인트는 넣지 않는다(위 범위 참고).

## 검증

- `./gradlew test` — 유닛 테스트 전량
- `./gradlew ktlintCheck`
- `./gradlew :app:assembleDebug` — Hilt 그래프(이번엔 DI 줄이 늘지 않아 회귀 확인용)

**실서버 호출은 이번에도 0건이다.** 소비처가 없어 요청을 만들 자리가 없다. `http/` 요청 모음이 계약
해석을 사람이 확인할 유일한 수단이다.

## 미결

- 이 표면을 소비하는 Repository·UseCase·화면이 여전히 0건이다. **다만 이번 라운드로 C-001 캔버스
  결선의 서버 측 선행 조건이 사라진다** — `today`가 배치 전량을 주므로 "다시 그릴 수 없다"는 사유가
  없어진다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-158
- **마감된 캔버스의 편집 차단 규칙이 없다.** 서버가 막지 않으므로 앱이 `status`로 잠가야 하는데,
  어느 조작을 어디까지 잠글지 정책 소스가 없다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-160
- **`placedBy`가 `members`에 없는 케이스의 표시 정책이 없다**(탈퇴자 `(알수없음)`). 네임태그 칩이
  닉네임 첫 글자로 색을 정하는데 그 값의 첫 글자는 괄호다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-163
- 팀 명세 원문(`parfait` 조회·`parfait-image` 테두리/삭제·member 탈퇴)이 `parfait/api/spec/`에 없다.
  코드에서 못 읽는 클라이언트 책임이 거기 있을 수 있다 → [open-questions](../../../synthesis/open-questions.md)
