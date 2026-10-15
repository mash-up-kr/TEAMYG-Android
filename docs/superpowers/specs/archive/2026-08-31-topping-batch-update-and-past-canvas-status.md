---
id: topping-batch-update-and-past-canvas-status
title: 토핑 일괄 수정 전환 및 과거 캔버스 status 수용 (Topping Batch Update)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-01
related_code:
  - ParfaitImageService.kt#patchGroupsByGroupIdParfaitsByParfaitIdImages
  - UpdateParfaitImagesRequest.kt
  - UpdateParfaitImagesResponse.kt
  - ParfaitImageRemoteDataSource.kt#updateToppings
  - ParfaitImageRemoteDataSourceImpl.kt#updateToppings
  - VOMapper.kt#toUpdateRequest
  - VOMapper.kt#toUpdatedToppingVOList
  - ToppingTransformUpdate.kt
  - ToppingRepository.kt#updateAll
  - ToppingRepositoryImpl.kt
  - UpdateToppingsUseCase.kt
  - CanvasBGEditViewModel.kt#updateDirtyToppings
  - CanvasBGEditViewModel.kt#saveTransforms
  - PastParfaitsResponse.kt#PastParfaitResponse
  - PastCanvasVO.kt#PastCanvasVO
  - CanvasMainViewModel.kt#uploadedDates
related_adr:
related_spec: c301-topping-edit-tab, parfait-api-contract-docs
related_architecture:
  - data-layer.md
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, topping, canvas, server-contract]
---

# Spec: 토핑 일괄 수정 전환 및 과거 캔버스 status 수용

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

> ✅ **구현이 develop에 들어왔다(2026-09-01, PR #428 `e870fb87` — 트리가 브랜치 팁 `a8d2efd5`와 같아
> 충돌 해소 편집이 0건이다).** 브랜치에 있던 커밋 5개가 그대로 올라왔고 문서 커밋 둘(`850a5be5`·`a8d2efd5`)이
> 뒤따랐다. **as-built가 이 설계와 갈린 자리는 없다** — 아래 설계의 심볼·계약·저장 흐름이 머지된 코드와
> 일치한다. 실행 결과가 계획과 갈린 것은 검증 명령 표기 정정과 ktlint 재포맷뿐이다.

## 목표

서버 `main` `de3a99a` delta 두 건을 앱에 반영한다.

**① 토핑 일괄 수정 PATCH로 갈아탄다.** 서버가 `PATCH .../parfaits/{parfaitId}/images`를 신설했고
([api/parfait-image.md](../../../api/parfait-image.md)), 그 커밋 본문이 밝힌 동기가 "클라이언트가 단건 수정
API를 개수만큼 반복 호출해야 했다"이다. C-301 편집 탭의 확인 버튼이 정확히 그렇게 동작한다 —
`updateDirtyToppings`가 `async` + `awaitAll`로 바뀐 토핑 수만큼 요청을 낸다. 그 자리를 요청 한 번으로
접는다.

**② 과거 캔버스 목록의 `status`를 받는다.** 서버가 목록 원소에 `ACTIVE`·`CLOSED`·`EMPTY`를 실어
주는데 앱 DTO에 필드가 없다. 오늘 조회·상세가 이미 쓰는 `CanvasStatus`를 그대로 재사용해 VO까지
올린다.

## 범위

- 포함:
  - 일괄 PATCH의 `:data` 표면(Service·요청/응답 DTO·매퍼·DataSource) 신설.
  - 단건 위치 PATCH 경로 **제거**(Service 메서드·DataSource·Repository·UseCase).
  - `ToppingRepository.updateAll`·`UpdateToppingsUseCase` 신설.
  - `CanvasBGEditViewModel` 확인 버튼의 저장 흐름 재구성.
  - 과거 목록 응답 DTO·`PastCanvasVO`에 `status` 추가.
  - 위 변경에 걸리는 계약 문서(`api/`)와 미결 문서의 갱신.
- 제외:
  - **달력 점 기준의 변경.** `uploadedDates`는 `PastCanvasVO.isEmpty`(토핑 개수)를 계속 쓴다.
    아래 [과거 목록 status](#과거-목록-status) 참고 — 위키 정본이 개수 기준이다.
  - **`status`의 화면 소비처.** VO까지만 올리고 읽는 자리는 만들지 않는다.
  - **테두리 PATCH의 일괄화.** 서버 일괄 API에 테두리 필드가 없다. 서버가 넓히면 그때 다룬다.
  - **`positionZ` 전송.** 앱에 z 조작 경로가 없고 서버가 부분 병합으로 기존 값을 유지한다.
  - 일괄 실패 시 단건으로 되짚어 실패 항목을 가려내는 폴백. 아래 [실패 처분](#실패-처분) 참고.

## API / 인터페이스

### 서버 계약 (정본: [api/parfait-image.md](../../../api/parfait-image.md))

`PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/images` — 성공 200 · envelope `code` = `"OK"`.

요청은 `items` 배열이고 항목은 `parfaitImageId`(비널) + 위치·크기·각도 다섯 필드(전부 널 허용,
`null`이면 서버가 기존 값 유지)다. 응답은 `images` 배열이고 원소 필드 집합이 **단건 응답과 같다**
(서버가 `UpdateParfaitImageResponse`를 그대로 재사용한다).

계약에서 이 설계에 걸리는 성질이 넷이다.

- **부분 성공이 없다.** 트랜잭션 하나라 항목 하나가 걸리면 전부 롤백된다.
- **실패 항목을 알려주지 않는다.** 에러 코드만 오고 `parfaitImageId`는 응답에 없다.
- **검사 순서가 단건과 반대다.** 정확히는 **항목별 소유권**이 마감 검사보다 뒤로 밀린 것이다 —
  그룹 소속 검사는 일괄에서도 여전히 마감보다 앞이라 미참여는 `PARFAIT_IMAGE_NOT_OWNED`(403)다.
  결과로 마감된 캔버스의 남의 토핑에 단건은 403 `PARFAIT_IMAGE_NOT_OWNED`, 일괄은 409
  `PARFAIT_ALREADY_CLOSED`가 온다(호출자가 그룹 멤버일 때 — 이 화면은 항상 그렇다). 다만
  `toCanvasBGEditError`가 네트워크·이미지 외 전부를 `unknown`으로 접으므로 **사용자에게 보이는
  문구는 안 바뀐다.**
- **응답 순서를 계약이 보장하지 않는다.** 소비 측이 응답 원소를 읽는다면 `parfaitImageId`로 맞춰야
  한다. 지금 화면은 성공·실패만 보고 `UpdatedToppingVO`를 버리므로 이 성질에 걸리지 않는다 —
  읽는 소비처가 생길 때를 위한 기록이다.

### `:data`

신설 DTO 둘:

```kotlin
@Serializable
data class UpdateParfaitImagesRequest(
    @SerialName("items") val items: List<UpdateParfaitImageItemRequest>,
)

@Serializable
data class UpdateParfaitImageItemRequest(
    @SerialName("parfaitImageId") val parfaitImageId: Long,
    @SerialName("positionX") val positionX: Double? = null,
    @SerialName("positionY") val positionY: Double? = null,
    @SerialName("positionZ") val positionZ: Int? = null,
    @SerialName("scale") val scale: Double? = null,
    @SerialName("rotation") val rotation: Double? = null,
)

@Serializable
data class UpdateParfaitImagesResponse(
    @SerialName("images") val images: List<UpdateParfaitImageResponse>,
)
```

응답 원소는 **기존 `UpdateParfaitImageResponse`를 재사용한다** — 서버가 같은 DTO를 재사용하므로
"wire DTO는 서버의 거울"이라는 규약과 맞는다. 기존 파일을 지우지 않고 그대로 둔다.

`@RemoteJson`이 `encodeDefaults = true`라 안 바꾸는 필드도 `"positionX": null`로 실려 나간다.
서버에게 키 부재와 명시적 `null`이 같은 뜻이라 동작은 정확하다 — 기존 단건 요청 DTO의 KDoc이 적어
둔 것과 같은 사정이고, 그 설명은 항목 DTO로 옮긴다.

`ParfaitImageService`:

```kotlin
@PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImages(
    @Path("groupId") groupId: Long,
    @Path("parfaitId") parfaitId: Long,
    @Body request: UpdateParfaitImagesRequest,
): ApiResponse<UpdateParfaitImagesResponse>
```

단건 `patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId`는 제거한다. POST와 경로가 같고
메서드만 다른 모양이 되는데, 이 파일의 이름 규약(경로를 그대로 옮긴다)을 따르면 자연히 그렇게 된다.

### `:domain`

부분 수정 항목을 나를 타입을 새로 둔다. 기존 `ToppingTransform`은 다섯 필드가 전부 non-null이라
"이 축은 안 바꾼다"를 표현하지 못한다.

```kotlin
data class ToppingTransformUpdate(
    val parfaitImageId: ParfaitImageId,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
)
```

`ToppingRepository`에서 `update`를 걷고 `updateAll`을 넣는다. DataSource도 같은 모양이다.

```kotlin
suspend fun updateAll(
    groupId: GroupId,
    parfaitId: ParfaitId,
    updates: List<ToppingTransformUpdate>,
): Result<List<UpdatedToppingVO>>
```

`UpdateToppingUseCase`는 `UpdateToppingsUseCase`로 교체한다. 소비처가
`CanvasBGEditViewModel` 하나뿐이라 단건 경로를 남기지 않는다 — 쓰지 않는 갈래를 열어 두면 계약이
바뀌어도 아무도 고치지 않는다는 이 저장소의 규약을 따른다. 단건이 다시 필요해지면
[api/parfait-image.md](../../../api/parfait-image.md)에 스펙이 그대로 남아 있으니 그것을 보고 되살린다.

**빈 목록은 요청을 보내지 않는다.** 서버가 빈 `items`를 200으로 받아 주기는 하지만
([api/parfait-image.md](../../../api/parfait-image.md) — 검증을 하나도 안 돌고 빈 배열을 준다) 보낼 이유가
없다. Repository가 `updates`가 비면 `Result.success(emptyList())`로 곧장 끊는다.

## 동작 / 상태

### 확인 버튼의 저장 흐름

지금 `updateDirtyToppings`는 dirty 토핑마다 코루틴을 띄우고 그 안에서 변형→테두리를 순차로 보낸다.
바뀐 뒤에는 **축으로 먼저 가른다.**

1. dirty 토핑을 `serverToppings` 스냅샷과 대조해 **변형이 바뀐 것**과 **테두리가 바뀐 것**으로
   나눈다. 대조를 dirty 안에서만 하는 것은 지금과 같다 — 목록 전체를 스냅샷과 견주면 갱신이 들여온
   남의 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡힌다.
2. 변형이 바뀐 게 하나라도 있으면 **`UpdateToppingsUseCase`를 1회** 부른다.
3. 테두리가 바뀐 토핑은 **지금처럼 병렬 N회**다. 일괄 API에 테두리가 없어서 다른 수가 없다.
4. 두 실패 집합을 합쳐 `dirtyToppingIds`에 남긴다. 그 뒤 배경 저장·토스트·화면 닫기 판정은
   **지금 그대로**다.

변형 일괄과 테두리 N회의 순서는 서로 다른 필드를 건드리므로 무관하다. 변형 일괄을 먼저 보내고
그것이 끝난 뒤 테두리를 병렬로 보낸다 — 실패 집합을 합치는 자리가 하나로 모인다.

### 실패 처분

- **일괄이 실패하면 변형을 보낸 토핑 id를 전부** 실패 집합에 넣는다. 서버가 전부 롤백했으므로 이
  판정이 실제 서버 상태와 정확히 맞는다. 재시도 입도가 토핑 단위에서 요청 단위로 거칠어지는 것이
  대가이고, 실패 항목을 응답이 안 알려주므로 더 잘게 나눌 방법이 없다.
- **테두리는 지금처럼 토핑 단위**다. 한 토핑이 실패해도 나머지는 저장된다.
- 한 토핑이 두 축 다 바뀌었는데 한쪽만 실패하면 그 토핑이 dirty로 남아 다음 확인에서 둘 다 다시
  나간다. 지금도 같은 성질이고(`transformSaved && borderSaved`), 서버가 두 축을 각각 통째로 덮으므로
  다시 보내도 결과가 같다.
- 실패 토스트는 기존 `CanvasBGEditError.TOPPING_SAVE_UNKNOWN`을 그대로 쓴다. 서버가 403 대신 409를
  주게 되는 경우가 생기지만 `toCanvasBGEditError`가 둘 다 `unknown`으로 접으므로 문구가 안 바뀐다.

### 과거 목록 status

`PastParfaitResponse`에 `status: String`을 더하고, `PastCanvasVO`에 `status: CanvasStatus`를 더한다.
매퍼는 이미 있는 `toCanvasStatus()` 확장을 재사용한다(미지 값은 `CanvasStatus.UNKNOWN`).

⚠️ **`isEmpty`는 `toppingCount == 0` 그대로 둔다.** 위키 [[C-201-캘린더-정책-v0.1]]이 인디케이터를
"해당 날짜 토핑 1개 이상 = True, 0개 = False"로 규정하고, 지금 판정이 그 정본과 일치한다. 서버
`EMPTY`는 **토핑 0건으로 마감된 날**이라 뜻이 더 좁아서, 판정을 그쪽으로 옮기면 오늘의 빈
캔버스(`ACTIVE` + 0건)에 점이 찍혀 정본을 어긴다.

두 값이 같은 것을 뜻하지 않는다는 사실을 `PastCanvasVO`의 KDoc에 적어, 다음 사람이 "서버가 주는
값이 있는데 왜 개수로 세나"를 다시 묻지 않게 한다.

## 파일 구성

**`:data`**

- 신설: `service/model/request/parfaitimage/UpdateParfaitImagesRequest.kt`
  (`UpdateParfaitImagesRequest` + `UpdateParfaitImageItemRequest`)
- 신설: `service/model/response/parfaitimage/UpdateParfaitImagesResponse.kt`
- 삭제: `service/model/request/parfaitimage/UpdateParfaitImageRequest.kt`
  (단건 전용 — 소비처가 사라진다. KDoc의 `encodeDefaults` 설명은 새 항목 DTO로 옮긴다)
- 수정: `service/ParfaitImageService.kt` — 단건 PATCH 제거, 컬렉션 PATCH 추가
- 수정: `source/parfaitimage/mapper/VOMapper.kt` — `toUpdateRequest()`(도메인 항목 → wire 항목),
  `toUpdatedToppingVOList()` 추가. 기존 `toUpdatedToppingVO()`는 원소 매핑으로 계속 쓴다
- 수정: `source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt` + `Impl` — `updateTopping` →
  `updateToppings`
- 수정: `repository/topping/ToppingRepositoryImpl.kt` — `update` → `updateAll`, 빈 목록 단축
- 수정: `service/model/response/parfait/PastParfaitsResponse.kt` — `status` 추가
- 수정: `source/parfait/mapper/VOMapper.kt` — `toPastCanvasVOList`가 `status`를 채운다

**`:domain`**

- 신설: `model/topping/ToppingTransformUpdate.kt`
- 신설: `usecase/topping/UpdateToppingsUseCase.kt`
- 삭제: `usecase/topping/UpdateToppingUseCase.kt`
- 수정: `repository/topping/ToppingRepository.kt`
- 수정: `model/canvas/PastCanvasVO.kt` — `status` 추가, `isEmpty` KDoc 보강

**`:feature:groups:canvas:impl`**

- 수정: `viewmodel/CanvasBGEditViewModel.kt` — `updateDirtyToppings` 재구성,
  `updateToppingIfChanged`를 축별로 가르는 구조로 대체

**테스트**

- 수정: `data/.../parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt` — 단건 케이스를 일괄
  케이스로 옮긴다(성공 매핑 · 항목 배열 조립 · 생략 필드가 `null`로 실림 · 경로 변수 언랩 · 에러 ·
  빈 응답)
- 수정: `data/.../parfait/remote/ParfaitRemoteDataSourceImplTest.kt` — 과거 목록 `status` 매핑과
  미지 값 폴백
- 수정: `data/.../repository/topping/ToppingRepositoryImplTest.kt` — `update_` 케이스 둘을 `updateAll_`
  케이스로 옮기고 **빈 목록 단축**을 잠근다(매퍼 단독 테스트를 안 만드는 규약상 그 판단의 유일한 서식지다)
- 수정: `feature/.../CanvasBGEditViewModelTest.kt` — 변형이 바뀐 토핑들이 **한 번의 호출**로 나감 ·
  테두리만 바뀐 토핑은 일괄에 안 실림 · 일괄 실패 시 변형을 보낸 토핑 전부가 dirty로 남음 ·
  테두리 실패는 그 토핑만 남음. `updateTopping` 목을 참조하는 기존 케이스 **여섯**을 전부 처리해야
  컴파일된다
- 수정: `CanvasMainViewModelTest.kt`(생성자 호출 셋) · `GetParfaitHistoriesUseCaseTest.kt`(하나) —
  `PastCanvasVO` 인자 추가. ⚠️ `GetParfaitYearsUseCaseTest`·`GetTodayParfaitFlowUseCaseTest`는 타입만
  참조하므로 건드리지 않는다
- 매퍼 단독 테스트는 만들지 않는다. 판단이 든 변환은 DataSource 테스트의 케이스로 잠근다.

**문서** (코드가 `develop`에 들어간 뒤가 아니라 이 라운드에서 함께 고친다 — 근거는 아래 주의)

- `api/parfait-image.md` — 엔드포인트 표 Android 열 뒤집기(단건 → 표면 없음, 일괄 → 구현됨),
  Android 매핑 표·서술 갱신
- `api/parfait.md` — 과거 목록 `status` 수용 반영
- `api/README.md`·`api/conventions.md` — 표면 셈
- `architecture/data-layer.md` — Repository 표의 `ToppingRepository` 행이 지워지는 `update` 시그니처와
  `UpdateToppingUseCase`를 철자 그대로 싣고 있다
- `synthesis/open-questions.md` — OQ-P-334 부분 해소, OQ-P-333 부분 해소

## 주의 / 열린 질문

⚠️ **계약 문서에 낡은 서술이 있다.** `api/parfait-image.md`의 PR #336·#335 항목이 위치 PATCH와
삭제의 실패를 "로그 한 줄로 접힌다 · 화면에 닿지 않는다"로 적는데 둘 다 틀렸다. **정정문은 서로
다르다** — 위치 PATCH 실패는 실패 id를 `dirtyToppingIds`에 남기고 `TOPPING_SAVE_UNKNOWN` 토스트를
내며 화면을 닫지 않고(`handleOnClickConfirm`), 삭제 실패는 `TOPPING_DELETE_UNKNOWN` 토스트를 내고
로딩만 내린다(`failToDeleteTopping`, dirty 축과 무관하다). 2026-08-31 서버 라운드에서 등록한
OQ-P-334도 그 낡은 서술을 그대로 옮겼다. 이 라운드에서 함께 정정한다 — 서버 delta 반영과 별개로
**지금 develop에 대해 이미 틀린 서술**이라, 다음 `sync-tjyg-develop-baseline` 회차까지 미룰 이유가
없다.

같은 절의 PR #336 항목이 스냅샷 필드를 `confirmedToppings`로 적는데 실제 이름은 `serverToppings`다.
함께 고친다.

⚠️ **재시도 입도가 거칠어진다.** 변형 저장이 실패하면 그 회차에 변형을 보낸 토핑이 전부 dirty로
남는다. 지금은 실패한 토핑만 남는다. 서버가 실패 항목을 응답에 실어 주면 되돌릴 수 있는 자리다 —
서버 쪽 개선 요청으로 남길지는 이 라운드에서 정하지 않는다.

⚠️ **실패가 반복 가능하면 그 화면에서 위치 저장이 통째로 막힌다.** dirty 집합에 다시 시도해도 계속
실패할 항목이 하나라도 섞이면(예: 남이 같은 `imageId`로 다시 배치해 소유자가 넘어간 토핑) 같은
회차에 변형을 보낸 나머지 토핑까지 롤백되고, 그 토핑들이 dirty로 남아 다음 확인도 같은 요청을
만들어 또 전부 실패한다. 단건일 때는 막힌 토핑 하나만 실패했다.

**그럼에도 폴백을 넣지 않는다.** 발생 조건이 좁다 — `handleOnClickTopping`이 `isMine`으로 선택을
막아 남의 토핑은 애초에 dirty에 들어가지 않고, 소유자가 넘어가려면 그 사이 남이 같은 `imageId`로
재배치해야 한다. 폴백(일괄 실패 시 단건 N회)은 지운 경로를 되살려 두 방식을 영구히 유지하게
만드는데, 그 비용이 이 시나리오의 확률보다 크다. 실사용에서 이 봉쇄가 관측되면 그때 다시 연다.

⚠️ **요청 수가 절반만 준다.** 변형은 1회로 접히지만 테두리는 여전히 토핑마다 나간다. 확인 한 번에
두 방식이 섞이는 모양이 남는다.

⚠️ **`scale`·`rotation`의 범위 검증은 여전히 양쪽 어디에도 없다**(OQ-P-271). 일괄로 바뀌어도
그대로다 — 서버에 검증 애노테이션이 없고 `items` 개수 상한도 없다.

⚠️ **실서버 요청 검증은 이 라운드에서도 0건이다.** 일괄 엔드포인트는 `http/parfait-image.http`
요청 모음에도 없다. 요청을 하나 추가할지는 구현 계획에서 정한다.
