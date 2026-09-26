---
id: member-parfait-image-api-service-layer
title: ":data member·parfait-image API Service·remote DataSource 레이어 (4 엔드포인트)"
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-12
related_code: MemberService, ParfaitImageService, MemberRemoteDataSource, ParfaitImageRemoteDataSource, ApiCaller, ServiceModule, RemoteDataSourceModule, ImageService, PolicyRemoteDataSourceImpl, GroupNickname
related_adr: ADR-0017
related_spec: 2026-08-10-image-api-service-layer, 2026-08-03-data-api-service-layer, 2026-08-06-unit-test-infrastructure
related_architecture: data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, data, network, api, member, parfait-image]
---

# :data member·parfait-image API Service·remote DataSource 레이어

서버 delta `2c5499a`가 들여온 두 도메인 4 엔드포인트([api/member.md](../../../api/member.md)·
[api/parfait-image.md](../../../api/parfait-image.md))를 `:data`의 Retrofit Service와 remote DataSource로
구현하고 대응 domain VO를 만든다.

> ✅ **develop 머지 완료(PR #230, 2026-08-12)** — 아래 ⚠️가 예고한 대로 **앞 라운드(PR #229)와 한 PR로 합쳐졌다.**
> 최종 머지 브랜치는 `feature/sync-backend-api-260810`이고, image 2 + member 2 + parfait-image 2가 함께 develop에 들어갔다.
> 머지본 재대조에서 **설계와 갈린 곳 0건** — Service 함수명 4건(`patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId` 포함)·
> DTO 6건 전 프로퍼티 `@SerialName`·domain 타입 11건·DataSource 시그니처(POST는 `ToppingTransform` 통째, PATCH는 nullable 5)·
> DI 4줄이 본문 그대로다. `*VOMapperTest` 0건도 `find` 기대대로다.
>
> **as-built 확정 2건**(본문이 위치를 안 적었던 자리):
> ① `ToppingPlacerVO`는 자기 파일이 아니라 `domain/model/topping/PlacedToppingVO.kt`에 함께 산다
> (`PlacedByResponse`가 `PlaceParfaitImageResponse.kt`에 있는 것과 같은 형태 — 본문 배치 표는 그쪽만 병기했다).
> ② 요청 방향 변환(`ToppingTransform.toPlaceRequest`)도 응답 매퍼와 같은 `source/parfaitimage/mapper/VOMapper.kt`에 있다.
> sealed → 평면 3필드를 펴는 곳이 여기다.
>
> **본문 결정 ③의 기술이 코드에서 정밀해졌다.** 본문은 `null`이 실려 나가는 원인을 `explicitNulls`로 적었는데,
> `UpdateParfaitImageRequest` KDoc은 **실제 결정 인자가 `encodeDefaults = true`**임을 짚는다 — 5필드가 전부
> `= null` 기본값이라 `encodeDefaults = false`였다면 `explicitNulls`와 무관하게 통째로 생략됐을 것이다.
> 결론(키 부재와 명시적 null이 서버에 동치)은 같고 근거만 정확해졌다.
>
> 🔁 **2026-08-11 구현 완료(당시 미머지·미푸시)** — SDD 5 Task, 브랜치 `feature/member-parfait-image-api-260811`,
> 커밋 7개(`40e8e5a6`~`24dd7c11`). **설계에서 뒤집힌 결정 0건** — 본문이 그대로 as-built다.
> Task 리뷰 5회 중 fix 라운드는 1회뿐(Task 5, README 파일 목록이 두 곳인데 한쪽만 갱신됨).
> 유닛 테스트 신규 25건(member 10 + parfait-image 15) + 이관 6건, `test`·`ktlintCheck`·`:app:assembleDebug` 통과.
>
> **최종 전체 리뷰(opus)가 Important 1건**을 잡았고 그것도 코드가 아니라 **주석의 자기모순**이었다 —
> `http/parfait-image.http`가 4줄 위에서 "업로드와 **다른 도메인**"이라 경고해 놓고 아래에서 "**같은
> 도메인**의 이미지 발급이 200"이라 적어, 이 파일이 가르치려는 구분을 반대로 알려주고 있었다.
> fix 웨이브 1회로 7건(Important 1 + Minor 6) 반영, 재검수 전부 ADDRESSED·새 breakage 0.
>
> **실행 중 드러난 것 하나가 계획서 밖이었다.** Task 1 리뷰어가 `:app` Hilt 컴파일 에러를 올렸는데,
> 파 보니 `CreateGroupUseCase`·`EnterGroupUseCase`를 못 찾는 에러였고 **두 클래스는 추적 소스에 0건**
> (develop에도 PR #229에도 없다)이었다 — 다른 브랜치 빌드 잔재였다. `clean` 후 통과. 이걸 안 잡았으면
> Task 2~4의 `:app:assembleDebug` DI 게이트가 통째로 무의미했다.
>
> **이월 minor 5건은 최종 리뷰가 전부 "이월 가능"으로 판정**했다. 그중 테스트명 2세그먼트 건은
> 규약 위반이 아니라 **기존 관행과 일치**함이 확인됐다(merge base에 이미 같은 형태가 있다).
> **계획의 "변수를 더하지 않는다"는 뒤집혔다(2026-08-11, 사용자 결정, 커밋 `70203174`).** 최종 리뷰가
> `parfait_image_id`만 `http-client.env.json`·`_reset.http`에 없다고 지적했고, 형제 변수
> (`terms_id_*`·`group_id`·`image_*`)가 **전부 스크립트가 채우는 값인데도 등록돼 있다**는 것이 근거였다.
> 계획의 논거("런타임에 채우므로 선언 불필요")가 그 관행과 어긋났다. 등재하면서 `_reset.http`에
> 도메인별 비우기 항목(`0-1-3`)도 짝을 맞췄고, **`http/README.md` 샘플 블록이 `image_*` 2건까지
> 이미 어긋나 있던 것**(PR #229 때부터)도 함께 정정했다. 동작 변화는 없다 — 목적은 처음 쓰는 사람이
> 미해결 `{{parfait_image_id}}`를 설정 누락으로 오해하지 않게 하는 것이다.

**앞선 라운드들이 세운 관용구의 증분이다.** 계층·이름 규칙·타입 경계의 정본은
[2026-08-03-data-api-service-layer](2026-08-03-data-api-service-layer.md)이고,
[2026-08-10-image-api-service-layer](2026-08-10-image-api-service-layer.md)가 그 관용구를 한 번 더
적용했다. 이 스펙은 **규칙이 답하지 않는 지점만** 새로 결정한다 — 그 지점이 이번엔 하나 크다
(`parfait-image`의 요청 필드 3개가 서로 얽혀 있다).

작업 브랜치는 **`feature/sync-backend-api-260810`(PR #229) 위**에 판다. 그 PR이 `ImageId`를 만들고
있는데 `parfait-image`의 배치 요청이 그 타입을 받기 때문이다. develop 기준으로 파면 `ImageId`를
중복 정의하게 되고 `ServiceModule`·`RemoteDataSourceModule`·`http/` 파일이 양쪽에서 충돌한다.

> ⚠️ **PR #229는 아직 리뷰 중이다.** 이 브랜치는 그 위에 쌓이므로 #229가 리뷰에서 바뀌면 이쪽도
> 따라온다. develop이 #229보다 18커밋 앞서 있어 **#229의 rebase가 선행**한다.
>
> 📌 **결말: 두 브랜치가 합쳐져 PR #230 하나로 머지됐다(2026-08-12).** #229는 별도로 머지되지 않았고
> 이 라운드의 커밋이 그 위에 얹힌 채 `feature/sync-backend-api-260810`으로 함께 나갔다.

## 범위

**포함** — `MemberService`·`ParfaitImageService` · 요청/응답 DTO 6개 · `MemberRemoteDataSource`(+`Impl`) ·
`ParfaitImageRemoteDataSource`(+`Impl`) · VO mapper 2개 · domain VO·value class·enum · DI 등록 4줄 ·
유닛 테스트(신규 2 + 보강 2) · 매퍼 테스트 2건 삭제 · `http/users.http`·`http/parfait-image.http`.

**제외** — Repository·`domain/repository` 인터페이스, UseCase, 화면 결선, 서버 에러 코드의 도메인 예외
번역, 요청 전 클라이언트 측 유효성 검사. 기존 4라운드와 같은 경계다.

**애플 로그인(`POST /api/v1/auth/apple`)은 범위 밖이고 앞으로도 아니다.** Android에서 이 로그인을
쓰지 않기로 결정했다(2026-08-11). 서버 delta가 함께 들여온 엔드포인트지만 앱 대응 심볼을 만들지
않으며, [api/README.md](../../../api/README.md) 도메인 표와 `open-questions` OQ-P-117 ②를 이 결정으로 닫는다.
`http/auth.http`에도 애플 요청을 넣지 않는다.

## 계층과 배치

`Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. 기존과 동일하다.

| 위치 | 신규 |
|---|---|
| `data/service/` | `MemberService`·`ParfaitImageService` |
| `data/service/model/request/member/` | `ChangeGlobalNicknameRequest` |
| `data/service/model/response/member/` | `MyAccountResponse`·`ChangeGlobalNicknameResponse` |
| `data/service/model/request/parfaitimage/` | `PlaceParfaitImageRequest`·`UpdateParfaitImageRequest` |
| `data/service/model/response/parfaitimage/` | `PlaceParfaitImageResponse`(+`PlacedByResponse`)·`UpdateParfaitImageResponse` |
| `data/source/member/{mapper,remote}/` | `VOMapper.kt` · `MemberRemoteDataSource`(+`Impl`) |
| `data/source/parfaitimage/{mapper,remote}/` | `VOMapper.kt` · `ParfaitImageRemoteDataSource`(+`Impl`) |
| `domain/model/id/` | `ParfaitId`·`ParfaitImageId`·`GroupMemberId` |
| `domain/model/member/` | `MyAccountVO`·`GlobalNickname`·`LoginProvider` |
| `domain/model/topping/` | `ToppingTransform`·`ToppingBorder`·`PlacedToppingVO`·`ToppingPlacerVO`·`UpdatedToppingVO` |

Service 함수명은 규칙이 기계적이라 결정거리가 아니다 — `<method><PathSegmentsCamelCase>`.
`patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId`는 길지만 규칙의 답이고,
`deleteParfaitGroupsByGroupIdMembersMe` 선례가 이미 같은 길이다.

## member

### Service

```kotlin
interface MemberService {
    @GET("api/v1/users/me")
    suspend fun getUsersMe(): ApiResponse<MyAccountResponse>

    @PATCH("api/v1/users/me/nickname")
    suspend fun patchUsersMeNickname(
        @Body request: ChangeGlobalNicknameRequest,
    ): ApiResponse<ChangeGlobalNicknameResponse>
}
```

둘 다 서버 화이트리스트 밖이라 access token이 필요하다 — `@NoAuth`를 붙이지 않는다.
memberId는 요청이 아니라 토큰에서 나오므로 경로 변수도 바디 필드도 없다.

### domain

```kotlin
@JvmInline value class GlobalNickname(val value: String)
enum class LoginProvider { KAKAO, APPLE, UNKNOWN }
data class MyAccountVO(
    val memberId: MemberId,
    val provider: LoginProvider,
    val nickname: GlobalNickname,
)
```

### DataSource

```kotlin
suspend fun getMyAccount(): Result<MyAccountVO>
suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>
```

### 결정 3건

**`GlobalNickname`을 `GroupNickname`과 합치지 않는다.** 서버 대조 결과 두 값 객체의 규칙은 문자
그대로 같다(1~15자, `^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$`). 그래도 타입을 나눈다 — 저장
위치(계정 vs 그룹 멤버십)와 서버가 던지는 코드(`INVALID_NICKNAME` vs `INVALID_GROUP_NICKNAME`)가
다르고, 합치면 **전역 닉네임을 그룹 API에 그대로 넘기는 실수가 컴파일을 통과한다.** 두 타입 다
`value class`라 런타임 비용은 없다.

**`LoginProvider`에 `UNKNOWN`을 둔다.** `PolicyType`·`ImageStatus` 선례와 같다. 서버 영속 계층에는
`GOOGLE`이 있는데 core enum에는 없어 그 회원 조회가 500이 나는 상태다
([api/member.md](../../../api/member.md), open-questions OQ-P-121) — 앱이 `enumValueOf`를 쓰면 서버가
provider를 하나 늘리는 순간 크래시한다.

**닉네임 변경 반환은 `Result<GlobalNickname>`이고 VO를 만들지 않는다.** 응답이 필드 하나뿐이라
감쌀 것이 없다. `GroupNicknameVO`가 VO였던 건 필드가 둘(`groupId` + 닉네임)이었기 때문이다.

## parfait-image

### wire DTO는 서버 그대로 평면이다

```kotlin
@Serializable
data class PlaceParfaitImageRequest(
    @SerialName("imageId") val imageId: Long,
    @SerialName("positionX") val positionX: Double,
    @SerialName("positionY") val positionY: Double,
    @SerialName("positionZ") val positionZ: Int,
    @SerialName("scale") val scale: Double,
    @SerialName("rotation") val rotation: Double,
    @SerialName("borderType") val borderType: String,
    @SerialName("borderColor") val borderColor: String? = null,
    @SerialName("borderWidth") val borderWidth: Double? = null,
)
```

sealed 타입은 **domain 쪽에만** 산다. DTO는 서버 JSON의 거울이라 계약 문서와 눈으로 대조돼야 한다.
전 프로퍼티 `@SerialName` 명시는 기존 규약 그대로다([architecture/data-layer](../../../architecture/data-layer.md)).

### domain

```kotlin
@JvmInline value class ParfaitId(val value: Long)
@JvmInline value class ParfaitImageId(val value: Long)
@JvmInline value class GroupMemberId(val value: Long)

data class ToppingTransform(
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
)

sealed interface ToppingBorder {
    data object None : ToppingBorder
    data class Solid(val color: String, val width: Double) : ToppingBorder
}

data class PlacedToppingVO(
    val parfaitImageId: ParfaitImageId,
    val imageId: ImageId,
    val imageUrl: String,
    val transform: ToppingTransform,
    val placedBy: ToppingPlacerVO,
)
data class ToppingPlacerVO(val groupMemberId: GroupMemberId, val nickname: GroupNickname)
data class UpdatedToppingVO(val parfaitImageId: ParfaitImageId, val transform: ToppingTransform)
```

### DataSource

```kotlin
suspend fun placeTopping(
    groupId: GroupId,
    parfaitId: ParfaitId,
    imageId: ImageId,
    transform: ToppingTransform,
    border: ToppingBorder,
): Result<PlacedToppingVO>

suspend fun updateTopping(
    groupId: GroupId,
    parfaitId: ParfaitId,
    parfaitImageId: ParfaitImageId,
    positionX: Double? = null,
    positionY: Double? = null,
    positionZ: Int? = null,
    scale: Double? = null,
    rotation: Double? = null,
): Result<UpdatedToppingVO>
```

### 결정 5건

**① 얽힌 필드만 sealed로 묶는다.** 서버는 `borderType = SOLID`면 `borderColor`·`borderWidth`가
필수이고 아니면 400 `INVALID_BORDER`다. `ToppingBorder` sealed로 모델링하면 **그 실패가 표현
불가능한 상태**가 된다 — 서버가 말로 적어 둔 제약을 타입이 강제한다. `ToppingTransform`도 같은
이유로 묶는다: 평면으로 두면 `Double` 넷이 연속해서 호출부가 순서를 뒤바꿔도 컴파일이 통과한다.

기존 라운드들이 평면이었던 건 필드 간 의존이 없었기 때문이다(`IssueImageUploadUrlRequest`의
`fileName`·`contentType`·`imageType`은 서로 독립이다). **여기서 처음 의존이 생겼고, 그래서
관용구가 답하지 않는다.**

전부를 `PlaceToppingCommand` 하나로 감싸는 안은 기각했다 — data 전용 중간 모델을 만드는 셈이라
[architecture/data-layer](../../../architecture/data-layer.md)가 명시적으로 기각한 `model.dto` 복제본에
가깝다.

`Solid.color`는 **raw `String`이고 앱이 형식을 규정하지 않는다.** 서버 계약이 타입만 정하고 형식을
말하지 않아 지금 좁힐 근거가 없다. 색을 실제로 만드는 화면 라운드가 형식을 정하고 그때 타입을
좁힐지 판단한다(`MyParfaitGroupVO.recentImageUrl`이 raw `String`인 선례와 같은 이유다).

**② PATCH는 부분 수정을 그대로 노출한다.** 팀 명세가 `partial update`로 명시하고, 관련 화면
설명(C-305)이 **위치만 바뀌는 조작**을 이름으로 든다("캔버스 영역 이탈 시 자동 보정"). z-order
변경도 위치를 안 건드린다. 코드가 허용하는 것과 팀이 의도한 것이 같은 방향이면 미러링이 기본값이다.

`ToppingTransform`은 5필드가 다 채워진 타입이라 "positionZ만"을 표현하지 못한다. **POST는
`ToppingTransform` 통째, PATCH는 nullable 5파라미터**로 간다 — 비대칭이지만 그 비대칭은 **서버
계약의 비대칭**이지 앱이 만든 것이 아니다. `ToppingTransformPatch`(5필드 nullable) 타입을 새로 두는
안은 기각했다: nullable 필드를 감싸기만 해서 `INVALID_BORDER` 같은 실제 제약을 잠그지 못한다 —
①에서 sealed를 채택한 이유가 여기엔 없다.

**③ "부분 수정"은 실제로 `null`을 명시 전송한다.** `@RemoteJson` `Json`이 `encodeDefaults = true`고
kotlinx-serialization 기본이 `explicitNulls = true`라, 안 바꾸는 필드도 `"positionX": null`로 나간다.
서버 `ParfaitImage.update`가 `positionX ?: this.positionX`이므로 **키 부재와 명시적 null이 같은 뜻**이라
동작은 정확하다. 이 하나 때문에 전역 `Json` 설정을 바꾸지 않는다 — 다른 API가 전부 영향받는다.

**④ 이름이 계층마다 갈린다.** `data`는 서버 언어(`ParfaitImageService`·`PlaceParfaitImageRequest`),
`domain`은 제품 언어(`PlacedToppingVO`·`ToppingTransform`·`ToppingBorder`). 기존 `ParfaitGroupVO`는
서버 이름을 domain까지 가져왔지만 그건 제품도 "그룹"이라 충돌이 없었다. **제품 어디에도 "parfait
image"라는 말이 없다** — 위키 개념도 화면 기획도 전부 "토핑"이다. 매퍼가 그 번역 지점이 된다.

`placedBy.nickname`은 전역 닉네임이 아니라 **그룹 닉네임**이므로 `GroupNickname`을 재사용한다 —
member 절에서 두 닉네임 타입을 안 합친 결정이 여기서 값을 한다.

**⑤ 응답 VO에 테두리 필드를 두지 않는다.** 서버가 저장은 하는데 `PlaceParfaitImageResponse`·
`UpdateParfaitImageResponse` 어디에도 돌려주지 않는다([api/parfait-image.md](../../../api/parfait-image.md),
open-questions OQ-P-119 ③). 없는 것을 지어내지 않는다 — 앱은 자기가 보낸 값을 기억해야 하고,
그 사실이 VO 모양에 드러나야 다음 라운드가 착각하지 않는다.

## 계약 함정

구현자가 서버를 다시 훑지 않아도 되도록 옮겨 적는다. 근거는 [api/member.md](../../../api/member.md)·
[api/parfait-image.md](../../../api/parfait-image.md).

1. **`GET /users/me`가 `MEMBER_NOT_FOUND`를 401과 404 둘 다로 낸다.** 앞의 것은 전역 `JwtAuthFilter`,
   뒤의 것은 `MemberService`다. `code` 문자열만으로 분기하면 두 상황이 뭉개진다. 이번 범위에서는
   에러 코드를 도메인 예외로 번역하지 않으므로 `ApiException.Business`로 그대로 흐른다 — **앱이
   코드 문자열로 분기하는 자리를 만들지 않는다**는 뜻이기도 하다.
2. **PATCH 닉네임에서 빈 문자열과 형식 위반이 다른 코드로 갈린다.** `""`는 `@NotBlank`에 걸려
   `INVALID_REQUEST`, `"연속  공백"`은 `INVALID_NICKNAME`이다.
3. **배치 POST는 upsert다.** 같은 `(parfaitId, imageId)`로 다시 POST하면 새 행이 생기지 않고 기존
   배치가 이동하며 **소유자가 호출자로 바뀐다**(open-questions OQ-P-118). 앱은 같은 이미지를 두 번
   배치할 수 없다.
4. **PATCH는 그룹 미참여도 `PARFAIT_IMAGE_NOT_OWNED`(403)로 낸다.** POST가 미참여를
   `GROUP_NOT_JOINED`로 구분하는 것과 다르다.
5. **배치 요청에 서버 검증이 없다.** `scale`이 음수여도, 좌표가 캔버스 밖이어도 저장된다.
   팀 명세는 캔버스 이탈 보정을 **앱 책임**으로 적었다(C-305) — 다만 그 보정은 화면 계층 일이라
   이번 범위 밖이다.
6. **배치 목록 조회·삭제 API가 서버에 없다.** 배치는 되지만 캔버스를 다시 그릴 수 없다
   (open-questions OQ-P-119 ①②). 이 라운드가 Repository까지 가지 않는 이유이기도 하다.
7. **`borderType` enum 밖 값은 Jackson 역직렬화가 먼저 깨져** `INVALID_REQUEST`(400)가 된다.
   도메인 코드가 아니라 공통 코드다. 앱은 sealed로 보내므로 도달하지 않는다.

## 테스트

**매퍼 단독 테스트를 만들지 않는다**([unit-test-infrastructure](2026-08-06-unit-test-infrastructure.md)
"테스트 규약" 11, 2026-08-11 개정). 판단이 든 변환은 그 매퍼를 통과시키는 DataSource 테스트의
케이스로 잠근다.

| 파일 | 잠그는 것 |
|---|---|
| `MemberRemoteDataSourceImplTest` (신규) | provider 미지값 → `UNKNOWN`, 대소문자 민감성, 성공 매핑, 요청 바디 배선, Business·Network 실패 경로 |
| `ParfaitImageRemoteDataSourceImplTest` (신규) | `ToppingBorder.None` → `borderColor`·`borderWidth`가 `null`, `Solid` → 둘 다 실림, `ToppingTransform` 5필드 배선, PATCH가 지정 안 한 필드를 `null`로 보냄, `placedBy` 중첩 매핑, `GROUP_NOT_JOINED`·`IMAGE_NOT_CONFIRMED`·`PARFAIT_IMAGE_NOT_OWNED`[^errcase] |

[^errcase]: **2026-08-11 as-built 정정.** 이 표의 초안은 `INVALID_BORDER`를 잠근다고 적었다. 구현이
`IMAGE_NOT_CONFIRMED`로 바꿨고 **그쪽이 맞다** — `INVALID_BORDER`는 `ToppingBorder` sealed 때문에
앱에서 도달 불가능해서 envelope 실패로 흉내 내는 테스트의 가치가 낮고, `IMAGE_NOT_CONFIRMED`는
앱이 실제로 만나는 실패다(업로드 confirm 누락). 최종 브랜치 리뷰가 같은 판정을 냈다. sealed가
`INVALID_BORDER`를 막는다는 사실은 `placeTopping_solidBorder_sendsColorAndWidth` 주석이 문서화한다.
| `PolicyRemoteDataSourceImplTest` (보강) | `PolicyVOMapperTest`에서 옮겨오는 `UNKNOWN` 폴백·대소문자 민감성 |
| `ImageRemoteDataSourceImplTest` (보강) | `ImageVOMapperTest`에서 옮겨오는 status 폴백·대소문자·`expiresIn` 초 해석·두 URL 배선 |
| 삭제 | `PolicyVOMapperTest` · `ImageVOMapperTest` |

**삭제 전에 이관이 먼저다.** 규약이 "검증을 줄이자"가 아니라 "한 곳에서 하자"이므로, 옮기지 않고
지우면 규약 위반이다. `ImageVOMapperTest`는 PR #229가 방금 추가한 파일인데 이 브랜치가 그 위에
쌓이므로 여기서 함께 정리한다.

## http/ 요청 모음

`http/users.http`·`http/parfait-image.http` 신설 + `http/README.md` 파일 목록 갱신.
이번 구현분 4 엔드포인트만 덮는다 — 애플 로그인은 위 범위 절대로 앱이 쓰지 않으므로 넣지 않는다.

`parfait-image.http`는 **선행 요청이 있다**는 점이 기존 파일과 다르다. 배치 확정은 `COMPLETED` 상태
`imageId`를 요구하므로 `images.http`의 발급 → S3 PUT → confirm을 먼저 돌려야 한다. 그 의존을 파일
주석에 적는다.

## 검증

- `./gradlew test` — 유닛 테스트 전량
- `./gradlew ktlintCheck`
- `./gradlew :app:assembleDebug` — Hilt 그래프까지 확인(DI 4줄이 실제로 물리는지)

**실서버 호출은 이번에도 0건이다.** 개발 서버 평문 HTTP 차단이 그대로고(open-questions
`[2026-08-02]`), 소비처가 없어 요청을 만들 자리도 없다. `http/` 요청 모음이 계약 해석을 사람이
확인할 유일한 수단이다.

## 미결

- 이 표면을 소비하는 Repository·UseCase·화면이 0건이다. **이 라운드가 끝나면 앱 표면이 20이 되어
  Android가 쓰기로 한 엔드포인트를 전량 덮는다**(서버 21 − 애플 1 = 20; develop 14 + PR #229의
  image 2 + 이번 4). 즉 **"덮을 게 남아서 소비를 미룬다"는 말이 이번 라운드로 끝난다** — 다음
  라운드부터는 표면을 더 만들 것이 없고 소비처를 붙이는 일만 남는다
  → [open-questions](../../../synthesis/open-questions.md)
  > 📌 **2026-08-12 확정** — PR #230 머지로 develop 표면이 **20/20**이 됐다(Service 7·remote DataSource 7쌍).
  > 소비처는 여전히 **0건**이다. 표면 공백이라는 사유가 사라졌으므로 다음 라운드의 대상은 소비처다.
- 배치 목록 조회·삭제 API 부재로 `parfait-image`는 Repository까지 갈 수 없다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-119
- 팀 명세 원문(`parfait-image` PATCH·POST)이 `parfait/api/spec/`에 없다. C-305 자동 보정 같은
  **코드에서 못 읽는 클라이언트 책임**이 거기 있으므로 별도 라운드에서 수집한다
  → [open-questions](../../../synthesis/open-questions.md)
