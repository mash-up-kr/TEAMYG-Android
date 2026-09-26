---
id: c106-topping-place-api
title: C-106 토핑 배치 API 결선 — 업로드 전송·초안 SSOT·테두리 계약 전환 (Topping Place API)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-22
related_code:
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceUiState
  - CanvasToppingPlaceRoute.kt#CanvasToppingPlaceRoute
  - CanvasToppingPlaceScreen.kt#CanvasToppingPlaceScreen
  - CanvasToppingLayer.kt#TOPPING_BASE_LONG_SIDE_RATIO
  - CanvasToppingLayer.kt#CanvasTopping
  - CanvasToppingLayer.kt#ToppingOutline
  - ToppingHandleComponents.kt#rememberToppingBaseSize
  - YGCanvas.kt#CANVAS_AREA_ASPECT_RATIO
  - YGCanvas.kt#CanvasArea
  - YGToppingCutoutImage.kt#YGToppingCutoutImage
  - ImageRemoteDataSource.kt#issueUploadUrl
  - ImageRemoteDataSource.kt#confirmUpload
  - ParfaitImageRemoteDataSource.kt#placeTopping
  - NetworkModule.kt#provideUnauthenticatedOkHttpClient
  - NetworkModule.kt#loggingInterceptor
  - AuthInterceptor.kt#intercept
  - SegmentationConfirmRoute.kt#SegmentationConfirmRoute
  - CanvasBGEditRoute.kt#CanvasBGEditRoute
  - ToppingEditViewModel.kt#completeEdit
  - ToppingEditMask.kt#trimTransparentBounds
  - SegmentationCacheDir.kt#clearFiles
  - CanvasMainViewModel.kt#loadTodayCanvas
  - CanvasMainScreen.kt#addAction
  - ServerErrorCode.kt#Parfait
  - String.kt#toColorOrNull
  - RecentImageLocalDataSourceImpl.kt#decode
  - FileRecentImageLocalDataSourceImpl.kt#readBytes
  - RecentImageRepositoryImpl.kt#storeRecentImageInInternalStorage
  - AddRecentImageUseCase.kt#invoke
  - GetRecentCacheImagesUseCase.kt#clearOutsideDayWindow
  - CustomGalleryPickerViewModel.kt#CustomGalleryPickerState
  - GalleryImageGridComponent.kt#GalleryImageGridComponent
  - NavKeySegmentationConfirm.kt#NavKeySegmentationConfirm
  - SegmentationConfirmViewModel.kt#observeDraft
  - ToppingDraftRepository.kt#record
related_adr: ADR-0025, ADR-0026, ADR-0017, ADR-0020
related_spec: c106-topping-place, image-api-service-layer, parfait-canvas-topping-member-api-service-layer, ygscaffold-v2-common-loading-error, c103-segmentation-topping-edit, segmentation-pipeline-hardening, screen-resume-refetch
related_architecture: data-layer, state-management, navigation-flow, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, topping, c-106, api]
---

# Spec: C-106 토핑 배치 API 결선

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

C-106 확인 버튼을 누르면 토핑이 **실제로 서버에 올라가게 한다.** 지금은
`CanvasToppingPlaceViewModel#handleOnClickConfirm`이 이펙트만 쏘고 Route가 `// TODO` 뒤에서
캔버스로 되감는다([c106-topping-place](2026-08-19-c106-topping-place.md) 드리프트 ①).

## 범위

- **포함**
  - S3 presigned PUT 전송 경로 신설 + 업로드 전용 OkHttp 클라이언트 분리
  - `ImageUploadRepository`(발급·전송·확인 3단계를 하나로) · `ToppingRepository`(배치) ·
    `AddToppingUseCase`(둘을 조율)
  - 토핑 만들기 흐름 상태를 `:data` DataStore 한 곳으로 모으는 **토핑 초안 SSOT**([ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md))
  - **테두리를 픽셀에 굽지 않고 서버 필드로 보내는 계약 전환**([ADR-0025](../../../adr/0025-topping-border-as-server-field.md))
  - 화면 좌표 → 서버 좌표 변환 + **종횡비 상수 통일**
  - 로딩 오버레이 · 실패 토스트 · 영구 실패 코드 판정(다섯 코드, 알린 뒤 화면에 남는다)
  - **C-001 `YGScaffoldV2` 이관 + 오늘 캔버스 조회 실패 표현**
- **제외**
  - **토핑 수정·삭제·테두리 재편집 API** — DataSource에 넷 다 있으나 소비 화면이 C-301 라운드다.
    쓰지 않는 것을 Repository로 올리면 계약이 바뀌어도 아무도 고치지 않는다.
  - **배경 이미지 업로드**(C-301) — 같은 `ImageUploadRepository`를 쓰지만 결선은 그 라운드 몫이다.
  - **배치 화면의 실제 배경·기존 토핑 미리보기** — OQ-P-240을 연 채로 둔다.
  - 고아 `PENDING` 이미지 정리 · 회전·리사이즈 한계의 정책 근거(OQ-P-241) ·
    드래그 핸들 접근성(OQ-P-202 ③) · `60.dp`/`14.dp` 리터럴(OQ-P-203 ③)
  - **누끼 알맹이의 최근 이미지 재사용** — PR6로 분리했다. 결정과 선행 결함 넷의 처방은
    아래 [누끼 알맹이 재사용 (PR6)](#누끼-알맹이-재사용-pr6) 절에 있다(미결 원문은 OQ-P-255).

## 결정된 것

| 쟁점 | 결정 | 근거 |
|---|---|---|
| 업로드 전송 포함 여부 | 전체 체인 한 라운드 | `imageId`를 얻을 경로가 없으면 배치만 붙여도 동작하지 않는다 |
| 조율 위치 | `AddToppingUseCase` 하나 | 4단계 순서는 서버 계약이 정한 도메인 규칙이지 화면 관심사가 아니다. C-301 배경이 앞 3단계를 재사용한다 |
| `positionZ` | 흐름 진입 시 캔버스의 최대 z + 1, **토핑이 없으면 1** | 새 토핑이 항상 맨 위. 목록 크기로 세면 지워진 토핑이 있는 캔버스에서 겹친다. 서버 요청 DTO에 검증 애노테이션이 없고 유일성 제약도 없어 남과 겹쳐도 거부되지 않는다 |
| 재시도 | 실패하면 토스트 후 **발급부터 전부 다시** | 만료된 presigned URL 문제가 자동으로 풀리고 상태 기계가 단순하다. 대가는 고아 S3 객체 |
| 테두리 | 굽지 않고 서버 필드로 | [ADR-0025](../../../adr/0025-topping-border-as-server-field.md) |
| 흐름 상태 위치 | DataStore 초안 SSOT | [ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md) |
| 업로드 Repository 이름 | `ImageUploadRepository` | 요청의 `ImageType`이 `NUKKI`·`BACKGROUND` 둘이라 토핑 전용이 아니다 |
| 배치 Repository 이름 | `ToppingRepository` | `domain/model/topping/`과 이름이 맞는다 |

## API / 인터페이스

```kotlin
// domain/repository/image/
interface ImageUploadRepository {
    /** 발급·전송·확인 3단계를 하나로 닫는다. 돌려주는 imageId 는 이미 COMPLETED 다. */
    suspend fun upload(
        filePath: String,
        imageType: ImageType,
    ): Result<ImageId>
}

// domain/repository/topping/
interface ToppingRepository {
    suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO>
}

// domain/usecase/topping/
class AddToppingUseCase(
    private val imageUploadRepository: ImageUploadRepository,
    private val toppingRepository: ToppingRepository,
)
```

`upload`가 받는 것은 **파일 시스템 절대경로**이지 `file://` URI가 아니다. 초안이 담는 것도 같은
형태다 — `ImageSegmentationRepositoryImpl`이 돌려주는 것이 절대경로이고, URI 변환은 지금도 화면이
필요할 때만 한다. 두 형태가 섞이면 어느 쪽이 계약인지 호출부마다 달라진다.

`contentType`을 파라미터로 받지 않는 것이 설계다. **구현이 한 번 정해 발급 요청과 PUT 헤더 양쪽에
같은 값을 쓴다.** 두 값은 S3 서명 대상이라 어긋나면 실패하는데 그 실패는 서버 로그에 남지 않는다.
한 곳에서만 나오면 어긋날 수가 없다.

**테두리 색의 직렬화 형식은 `#RRGGBB` 6자리다.** 서버 계약은 타입만 정하고 형식을 말하지 않으며
(`ToppingBorder.Solid`의 KDoc이 "색을 실제로 만드는 화면 라운드가 정한다"고 미뤄 둔 자리),
읽기 쪽 `String#toColorOrNull`은 `#` 유무와 6·8자리 hex만 받는다. 형식이 어긋나면
`CanvasToppingLayer#ToppingOutline`이 **테두리를 그냥 안 그리고** 서버는 200을 준다 — 어디에도
로그가 남지 않는 무증상 실패다. 그래서 형식을 여기서 못 박고 왕복을 테스트한다.
(PR5 브랜치 리뷰에서 8자리 → 6자리로 정정됐다 — 8자리는 ARGB·RGBA 두 관례가 공존해 iOS·CSS
파서가 `#RRGGBBAA`로 읽고, 서버가 검증 없이 저장·반환해 어긋나도 드러나지 않는다.)

## 업로드 전송 — 전용 클라이언트가 기능 전제다

`AuthInterceptor#intercept`는 Retrofit `Invocation` 태그로 `@NoAuth`를 판정한다. 발급받은
`uploadUrl`로 raw OkHttp `Request`를 만들어 쏘면 그 태그가 없어 **`Authorization`이 무조건 붙고**,
presigned URL에 그 헤더가 실리면 S3가 거절한다. 공유 클라이언트를 쓰면 업로드가 아예 동작하지 않는다.

기존 `@UnauthenticatedClient`를 재사용하지 않는다. 그 클라이언트는 KDoc이 존재 이유를 **재발급
교착 회피**로 못박아 뒀고 전용 `Dispatcher`를 그 목적으로 들고 있다. 이미지 업로드가 그 슬롯을
오래 점유하면 401이 몰리는 순간 재발급이 다시 굶는다. `@UploadClient`를 따로 만들고 전송 성격에
맞는 타임아웃을 준다(수치는 코드가 정한다).

두 가지를 기존 클라이언트에서 **복사하지 않는다.**

- **로깅 인터셉터를 아예 달지 않는다.** presigned URL은 서명(`X-Amz-Signature`)과 자격 정보를
  **쿼리 스트링**에 싣는 방식이라 **URL 자체가 유효한 업로드 자격증명**이고, `HttpLoggingInterceptor`는
  `HEADERS` 이상 모든 레벨에서 요청 라인(`--> PUT <url>`)을 남긴다. `redactHeader`는 헤더만 가려
  이 URL을 가리지 못한다. 이 클라이언트가 보내는 요청은 그 PUT 하나뿐이므로 로깅으로 얻는 값이
  자격증명 노출을 감수할 만큼 크지 않다. 실패 원인은 `PresignedUploadException`이 상태 코드로 싣는다.
- **바이트를 메모리에 통째로 올리지 않는다.** 파일을 스트리밍 `RequestBody`로 태운다. 본문을
  로깅했다면 원본 해상도 이미지가 매 업로드마다 문자열로 힙에 올라갔을 것이다(OQ-P-228과 같은 축).

## 토핑 초안 SSOT

`:data`의 DataStore에 **흐름당 하나**만 산다.

```kotlin
data class ToppingDraft(
    val groupId: GroupId,
    val parfaitId: ParfaitId,
    val nextPositionZ: Int,
    val subjectImagePath: String?,   // 업로드·표시용 알맹이(테두리 없음, 여백 걷힌 것)
    val cutoutImagePath: String?,    // 재편집 시작 마스크. 좌표계를 지켜야 해 트리밍하지 않는다
    val borderColorArgb: Int?,       // null 이면 테두리 없음
    val borderWidthDp: Float?,
)
```

| 시점 | 하는 일 |
|---|---|
| `CanvasMain`이 카메라·갤러리로 떠날 때 | 캔버스 식별값 셋으로 **새로 덮어쓴다**. 이미지·테두리는 비운다 |
| 세그멘테이션 완료 | `SegmentationViewModel`이 알맹이·cutout 경로 기록. **화면 진입이 아니라 세그멘테이션 성공 사건에 건다** — 진입에 걸면 프로세스 사망 복원 때 진입 인자가 편집 결과를 덮어써, [ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md)이 영속을 고른 이유를 스스로 깬다 |
| 토핑 편집 완료 | 알맹이·cutout·테두리 기록(덮어쓰기) |
| 배치 확정 성공 | 비운다 |

낡은 초안이 다음 흐름에 따라붙는 문제는 **진입 시 덮어쓰기 규칙 하나로** 닫힌다. 별도 만료·정리
경로를 두지 않는다.

**진입에서 초안을 쓰지 못하면 화면을 옮기지 않고 알린다.** 초안 없이 흐름에 들어가면 촬영·누끼·
편집을 다 마친 뒤에야 올릴 데가 없다는 것을 알게 된다 — 토핑 추가 버튼 가드가 막으려는 것과 같은
실패다. 되돌릴 것이 아직 없는 지점이라 알리고 제자리에 두는 것으로 끝난다.

⚠️ **`TOPPING_EDIT_RESULT_KEY`는 걷지 않는다.** 그 결과 키의 소비자가 둘이다 —
`SegmentationConfirmRoute`(토핑 만들기)와 **`CanvasBGEditRoute`**(C-301에서 이미 놓인 토핑을
`borderOnly`로 재편집하는 경로). 편집 화면이 결과 키 대신 초안에 쓰도록 바꾸면 배경 편집 쪽은
편집을 마쳐도 아무것도 반영되지 않고 **컴파일은 통과한다.** 결과 키는 그대로 두고, 그것을 받은
`SegmentationConfirmRoute`가 **초안에 옮겨 적는다.** 걷는 것은 그 Route의 `rememberSaveable`
셋뿐이다.

`NavKeySegmentationConfirm`의 경로 셋도 그대로 둔다. 그것은 **화면을 여는 인자**이고 초안은
**흐름의 결과물**이다. 두 값이 겹치는 구간에서는 **초안이 정본**이다 — 편집을 거치면 NavKey의
값은 낡은다.

**`NavKeyCanvasToppingPlace`는 인자가 없어진다.** `camera`·`segmentation` 모듈이 캔버스 개념을
떠안지 않는 것이 이 배치의 실익이다.

**초안이 가리키는 파일이 이미 없을 수 있다.** 초안은 DataStore에 영속되지만 그것이 가리키는 것은
`cacheDir` 하위 파일이고, 세그멘테이션 진입이 그 디렉토리를 통째로 비운다
(`SegmentationCacheDir#clearFiles`). OS도 저장 공간 압박 시 회수한다. 그래서 **초안을 읽을 때
"경로는 있는데 파일이 없다"를 그 경로가 처음부터 없었던 것과 같이 취급한다** — 비우는 것은
**이미지 경로 둘뿐**이고 캔버스 식별값과 테두리는 남긴다. 초안 전체를 버리면 흐름 진입 때 못 박은
`parfaitId`까지 잃어, 이 배치가 지키려던 "진입 캔버스가 못 박힌다"가 함께 깨진다.

## 표시·제어 규칙

| 대상 | 조건 |
|---|---|
| C-001 토핑 추가 버튼 | `isViewingToday`가 참이고 **`todayCanvas`가 있을 때만** 활성 |
| C-106 확인 버튼 | 캔버스 실측이 있고, 토핑 이미지 painter가 `Success`이고, 초안이 유효할 때만 활성 |

**토핑 추가 버튼 가드** — `todayCanvas`가 없으면 `parfaitId`도 `nextPositionZ`도 없다. 서버는 오늘
조회 때 캔버스를 만들어 주므로 "서버에 없다"는 경우는 없고, 없는 것은 **앱이 아직 못 받은 경우**다
(조회 전 로딩 구간, 그리고 `loadTodayCanvas`의 실패). 열어 두면 촬영·누끼·편집을 다 마친 뒤
마지막에야 올릴 데가 없다는 것을 알게 된다.

**확인 버튼 가드의 판정 근거가 `toppingBaseSize != null`이면 안 된다.**
`ToppingHandleComponents#rememberToppingBaseSize`는 `intrinsicSize`가 아직 없거나 로드에 실패해도
`null`이 아니라 **고정 폴백 크기**를 돌려주고 화면이 그것을 곧바로 ViewModel에 밀어 넣는다. 그래서
그 조건은 첫 프레임부터 참이고, 큰 PNG를 디코드하는 동안 확인을 누르면 폴백 크기로 계산된 배율이
서버에 올라간다. 읽기 쪽 `CanvasToppingLayer#ToppingImage`가 이미 같은 이유로 painter 상태를 보므로
쓰기 쪽도 그것을 판정 근거로 삼는다.

## C-001 오늘 캔버스 조회 실패 표현

지금 `CanvasMainViewModel#loadTodayCanvas`의 `onFailure`는 로그만 남긴다. 화면에는 아무 표현이
없어 사용자는 빈 캔버스와 조회 실패를 구분하지 못한다(OQ-P가 이미 지적한 자리). 토핑 추가 버튼
가드가 그 위에 얹히면 "버튼이 왜 안 눌리는지"까지 안 보이게 되므로 함께 연다.

- `CanvasMain`을 `YGScaffold`(V1, `EntryBuilder` 소유)에서 **`YGScaffoldV2`(Route 소유)**로 옮긴다.
  [ygscaffold-v2 스펙](2026-08-16-ygscaffold-v2-common-loading-error.md)이 이관을
  "화면별 API 결선 라운드에 묶어 점진 진행"으로 정해 둔 그 경우다.
- ⚠️ **토스트 자리는 `YGScaffoldV2`의 것이 아니다**(2026-08-20, PR #298 머지 후 확정). 같은 화면의
  Spotlight 작성자 토스트가 `YGCanvas`의 `overlayContent`에 호스트를 못 박아 두었고, 큐를 둘로
  나누면 [[toast]] 공통 정책의 스택이 큐마다 따로 논다. 조회 실패 토스트도 그 호스트로 보내고
  스캐폴드는 **로딩 오버레이 자리로만** 쓴다(OQ-P-167 ·
  [c202 스펙](2026-08-20-c202-canvas-spotlight.md)).
- **실패를 매번 알리지 않는다.** `screen-resume-refetch`가 화면이 앞에 설 때마다 재조회하게
  만들어 조회 빈도가 높다. 이미 받아 둔 `todayCanvas`가 있으면 화면을 유지하고 조용히 로그만 남기고,
  **보여 줄 캔버스가 없을 때만** 토스트로 알린다. G-001이 같은 스펙에서 정한 규칙의 C-001 대응물이다.

## 좌표 변환

`CanvasToppingPlaceViewModel`이 캔버스 실측과 토핑 원본 크기를 둘 다 아는 유일한 자리라 여기서
바꾼다. 순수 함수로 뽑아 단위 테스트한다.

```
positionX = (offsetX + baseWidth  / 2) / canvasWidth
positionY = (offsetY + baseHeight / 2) / canvasHeight
scale     = max(baseWidth, baseHeight) * scale / (canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO)
rotation  = rotationDegrees
positionZ = draft.nextPositionZ
```

⚠️ **쓰기 쪽 `scale`과 읽기 쪽 `scale`은 기준이 다른 다른 수다.** 그대로 보내면 안 된다.
읽기 쪽 `CanvasToppingLayer#CanvasTopping`은 한 변이
`canvasWidth × TOPPING_BASE_LONG_SIDE_RATIO × scale`인 **정사각 박스**에 `ContentScale.Fit`으로
담아 긴 변을 꽉 채운다. 배치 화면의 긴 변은 `max(baseWidth, baseHeight) × scale`이다. 둘을 같게
놓으면 위 식이 나온다.

정규화가 성립하려면 두 화면의 Canvas-Area 종횡비가 같아야 한다. **이 스펙을 쓸 때 그 값이 두 상수로
갈려 있었다** — 배치 화면은 `domain`의 `CANVAS_ASPECT_RATIO`를, 읽기 쪽 `YGCanvas#CanvasArea`는
자기 모듈의 private 상수를 썼다. 값만 같을 뿐 **동일성을 강제하는 것이 없어 하나를 바꿔도 컴파일이
깨지지 않았다.** 어긋나는 순간 이미 저장된 모든 토핑의 `positionY`가 틀어지고 증상은 "토핑이 조금씩
위아래로 밀린다"라 원인 추적이 어렵다. **`domain`의 `CANVAS_ASPECT_RATIO`를 지우고
`core:designsystem`의 `CANVAS_AREA_ASPECT_RATIO` 하나로 통일한다.** 반대 방향으로 모으면
`core:designsystem` → `:domain` 간선이 새로 생기는데, 캔버스 비율은 도메인 규칙이 아니라 **표시
규격**이라 소유가 디자인시스템 쪽이다(OQ-P-177 ①).

## 실패 처리

로딩은 `launch(key = …)`로 연타를 막고 `isLoading`을 세워 `YGScaffoldV2`의 오버레이가 터치를
삼킨다. 4단계 전체가 한 덩어리라 단계별 진행률은 표시하지 않는다.

분기는 `AppError.Server`의 `code` 하나로 한다. `statusCode`를 조건에 넣지 않는 것이 결정이다 —
아래 다섯 코드에는 status로 갈려야 하는 동명 코드가 없고, 서버가 HTTP 200에 실패 봉투를
실으면 `ApiCaller`가 그 값을 `null`로 채워 조건에 넣는 순간 판정이 사라진다(OQ-P-247 해소).

| 실패 | 화면 |
|---|---|
| `PARFAIT_ALREADY_CLOSED`(409) · `GROUP_NOT_JOINED`(403) · `PARFAIT_NOT_FOUND`(404) · `INVALID_REQUEST`(400) · `INVALID_BORDER`(400) | 토스트 후 **화면에 남는다**(닫기 버튼이 이미 있어 막다른 곳이 아니다) |
| 그 외 전부 | 토스트만. 배치 화면에 머문다 |

앞의 셋에 400 둘(`INVALID_REQUEST`·`INVALID_BORDER`)이 최종 리뷰로 늘었다 — 재시도가 발급부터
4단계를 전부 다시 태워도 서버 응답은 항상 같은 400이라, 확인을 누를 때마다 참조되지 않는
`PENDING` 이미지만 쌓인다.

⚠️ **마감만 판정하면 안 된다.** 배치 POST의 검사 순서가 그룹 참여 → 파르페 존재 → 파르페 상태라,
그룹에서 빠졌거나 파르페가 사라지면 **마감된 캔버스여도 409가 오지 않는다**
(`ServerErrorCode.Parfait`의 KDoc이 명시적으로 경고하는 함정). 다섯 코드 모두 재시도가 영원히
실패하므로, 알리지 않으면 사용자가 실패 사실 자체를 모른 채 남는다.

**되감지 않는다 — 알린 뒤 화면에 남는다.** 애초 설계는 이 코드들에서 `popUpTo`로 캔버스까지
되감는 것이었다. 최종 브랜치 리뷰가 그것을 Critical로 잡았다 — `CanvasToppingPlaceRoute`의
`toastPolicy`가 `rememberYGToastPolicy()`로 그 Route 컴포지션에 매달려 있어, `popUpTo`가 Route를
접는 **같은 프레임에 안내(토스트)까지 함께 폐기된다.** 되감으면 사용자는 실패했다는 말을 한마디도
못 듣고 캔버스로 돌아간다. 같은 파일이 `DraftMissing`을 안 되감는 이유로 이미 이 함정을 주석에
적어 두고, 바로 아래 영구 실패 갈래에서 같은 실수를 반복하고 있었다. 그래서 영구 실패도
`DraftMissing`과 같은 처분으로 맞췄다 — 알리고 화면에 남긴다(닫기 버튼이 이미 있어 막다른 곳이
아니다). **진짜 처방은 안내를 캔버스 쪽 토스트 호스트로 보내는 것이고, 그 자리는 OQ-P-167(서버
실패를 화면이 표현하는 방식) 소관이라 이 라운드 밖으로 미뤘다.**

업로드된 이미지는 여전히 롤백하지 않는다 — `ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED`의
KDoc이 "되돌리지 않고 알린다"로 처분을 미리 적어 뒀다(막 만든 토핑을 들고 갈 곳이 없어지면
사용자가 작업을 통째로 잃는다). **성공 경로의 `popUpTo`는 그대로다** — 되감기를 걷은 것은 영구
실패 갈래뿐이고, 성공하면 여전히 캔버스로 되감는다(아래 참고).

그 외에는 확인을 다시 눌러 **발급부터 전부 다시** 탄다.

`AppError` → 문구 공통 매핑은 아직 없다([ygscaffold-v2 스펙](2026-08-16-ygscaffold-v2-common-loading-error.md)이
명시적으로 제외한 항목). 이 화면이 자기 문구를 가진다.

성공하면 초안을 비우고 `popUpTo<NavKeyCanvasMain>()`으로 되감는다. `CanvasMainViewModel`에 이미
`handleEnter()` → `loadTodayCanvas()`가 있어 되돌아온 순간 새 토핑이 함께 내려온다.

## PR 분할 (스택)

아래에서 위로 쌓는다. 각 PR은 **그 시점에 develop이 깨지지 않는다.**

| # | 브랜치 성격 | 내용 | 사용자에게 보이는 변화 |
|---|---|---|---|
| 1 | 업로드 전송 계층 | `@UploadClient` · `PresignedUploadDataSource` · `ImageUploadRepository`/Impl · DI | **없음**(소비자 0) — ✅ **develop 머지**(PR #322, 2026-08-20 `da03c9b0`) |
| 2 | 배치 계층 | `ToppingRepository`/Impl(`place`만) · `AddToppingUseCase` | **없음**(소비자 0) — ✅ **develop 머지**(PR #322와 같은 머지 — PR2 브랜치가 PR1 커밋을 업고 올라갔다) |
| 3 | 초안 SSOT + C-001 정비 | `ToppingDraft` + DataStore + Repository · `CanvasMain`이 흐름 진입 시 초안 쓰기 · 토핑 추가 버튼 가드 · `YGScaffoldV2` 이관 + 조회 실패 토스트 | 버튼 가드 · 조회 실패가 보인다 · 초안 쓰기 실패도 알린다 — ✅ **develop 머지**(PR #334, 2026-08-22 `19cb5299` — PR3~PR6 넷이 이 머지 하나로 함께 들어왔다) |
| 4 | 테두리 계약 전환 | 트리밍된 알맹이 생성 · 굽기 중단 · 확인·배치 화면이 초안을 읽고 같은 스탬프로 그리기 · `rememberSaveable` 걷기 · `NavKeyCanvasToppingPlace` 인자 제거 · 종횡비 상수 통일 | **테두리 렌더 방식이 바뀐다** — ✅ **develop 머지**(PR #334와 같은 머지)([계획](../../plans/archive/2026-08-21-c106-pr4-topping-border-contract.md)) |
| 5 | 결선 | 좌표 변환 · `AddToppingUseCase` 호출 · 로딩·토스트·성공 시 되감기 · 성공 시 초안 비우기 · **아래 선행 미결 둘** | **토핑이 서버에 올라간다** — ✅ **develop 머지**(PR #334와 같은 머지)([계획](../../plans/archive/2026-08-21-c106-pr5-topping-place-wiring.md)) |
| 6 | 누끼 알맹이 재사용 | 배치 성공 시 **테두리 없는 알맹이**를 최근 이미지에 저장 · 최근 목록에 종류 축 신설 · 알맹이를 고르면 누끼 확인 화면으로 직행 · 선행 결함 넷(OQ-P-255) 처방 | 갤러리 "최근"에서 이미 만든 누끼를 다시 쓸 수 있다 — ✅ **develop 머지**(PR #334와 같은 머지. 브랜치 커밋 10개 `67ee0d6a..656cbf2e`, 신규 테스트 24건, 30파일 964/145). 상세는 [누끼 알맹이 재사용 (PR6)](#누끼-알맹이-재사용-pr6) |

1과 2는 소비자가 없어 리뷰가 각각 **S3 서명**과 **계약 매핑** 한 가지에만 집중할 수 있다.

✅ **PR3~PR6 넷이 머지 커밋 하나로 develop에 들어왔다**(2026-08-22, PR #334 `19cb5299`). 스택을
`ef55a58c` 위로 다시 쌓아 둔 상태에서 최상단 하나만 머지했고(PR 베이스 구조 #327→develop,
#331→#327, #333→#331, #334→#333), **머지 커밋의 트리가 PR6 브랜치 팁 `656cbf2e`와 같다** — 충돌
해소 편집이 0건이라 네 브랜치에 붙여 둔 as-built 기록을 **재측정 없이 develop 사실로 승격**할 수
있다. 리베이스 때 충돌은 `SegmentationViewModel` 한 자리뿐이었고 스택의 초안 기록은 그대로 둔 채
실패 표현만 develop의 `SegmentationEffect.ShowError`를 따르게 했다 — 이 스펙이 정한 동작은 바뀌지
않았다. 상세는 [PR3 계획서](../../plans/archive/2026-08-20-c106-pr3-topping-draft-ssot.md).

✅ **PR5가 선행 미결 둘을 함께 닫았다.** PR1이 만든 계층에 **처음으로 소비자가 붙는 라운드**라
그때까지 잠들어 있던 두 결함이 동시에 살아날 뻔했다.

- [**OQ-P-109**](../../../synthesis/open-questions.md) — 메인 클라이언트가 발급 **응답 본문**을
  `Level.BODY`로 찍던 것. `@NoBodyLog` + `SelectiveLoggingInterceptor`로 발급 엔드포인트만
  `Level.HEADERS`로 낮춰 닫았다.
- [**OQ-P-246**](../../../synthesis/open-questions.md) — `PresignedUploadDataSourceImpl#put`이 블로킹
  `execute()`를 쓰고 `Call.cancel()`을 코루틴 취소에 잇지 않던 것. `enqueue` +
  `suspendCancellableCoroutine`·`invokeOnCancellation { call.cancel() }`로 바꿔 닫았다.

**3과 4의 경계에 주의한다.** 초안에 이미지를 채우는 것과 굽기를 그만두는 것은 **떼면 안 된다** —
3에서 확인 화면이 초안을 읽게 하면서 4에서야 굽기를 멈추면, 그사이 초안의 `subjectImagePath`가
"테두리 없음"이라는 자기 정의와 어긋나거나 사용자가 방금 두른 테두리가 확인 화면에서 사라진다.
그래서 3은 **캔버스 식별값까지만** 쓰고 이미지·테두리는 4에서 함께 들어간다.

4가 시각 회귀 위험이 유일하게 몰리는 자리다. 실기기 확인을 여기에 붙이고 **누끼 확인 화면까지**
본다.

## 누끼 알맹이 재사용 (PR6)

배치에 성공한 알맹이를 갤러리 "최근"에 남겨, 다음번에 같은 사진을 다시 누끼 따지 않고 바로 쓰게
한다. 재사용 항목을 고르면 카메라·갤러리 확인과 세그멘테이션을 건너뛰고 **누끼 확인 화면(C-103)으로
직행**한다.

### 결정된 것

| 쟁점 | 결정 | 근거 |
|---|---|---|
| 저장 대상 | **테두리 없는 트리밍 알맹이 1장만** | 원본·마스크까지 함께 두면 내부 저장소 사용량이 3배가 되면서 상한 `MAX_SIZE = 9`와 정면으로 부딪힌다. 다시 편집하고 싶으면 갤러리의 원본에서 새로 시작하는 길이 이미 있다 |
| 종류 축 | 기존 목록 하나를 `List<RecentImage>`로 넓힌다 | 상한·데이 윈도우 정리·정렬이 전부 한 곳에 남는다. 목록을 둘로 가르면 상한이 목록마다 따로 걸려 최대 18장이 되고 시간순 병합이 이중으로 생긴다 |
| 구 스키마 처분 | 디코드 **2단 폴백** | 신 스키마 디코드가 실패하면 `List<String>`으로 한 번 더 시도해 종류를 `SOURCE`로 올려받는다. 지금의 `getOrDefault(emptyList())` 하나로는 기존 목록이 통째로 날아가고 파일만 고아로 남는다(OQ-P-255 ③) |
| 저장 시점 | 배치 성공 직후, **성공을 알리기 전** | 알맹이 경로를 초안에서 읽으므로 `clear()`가 먼저면 경로를 잃고, 성공 이펙트가 먼저면 `popUpTo`가 화면과 함께 `viewModelScope`를 걷어 저장이 중간에 끊긴다 |
| 저장 실패 처분 | 삼키고 로그만 남긴다 | 배치는 이미 성공했다. 재사용 편의 하나 때문에 성공한 흐름을 실패로 보이게 하지 않는다 |
| 노출 범위 | `returnResultOnly = false`인 토핑 만들기 진입에서만 | 배경 선택(C-301)에서 투명 알맹이가 골라지는 사고를 막는다. 경로마다 직행 대상 화면이 다른 문제도 함께 사라진다 |
| 셀 표시 | 같은 "최근 업로드" 섹션에 시간순으로 섞고 알맹이만 `ContentScale.Fit` | 알맹이는 투명 여백을 걷어낸 객체라 지금의 `Crop`으로는 잘린다. 종류를 알리는 시각 장치(뱃지·배경 구분)는 시안이 없어 두지 않는다 |
| 재업로드 | 재사용해도 **다시 올린다** | 서버가 준 `imageId`를 로컬에 붙들어 두는 경로가 없다. 업로드를 아끼는 것은 이 라운드의 목표가 아니다 |

### 선행 결함 넷의 처방 (OQ-P-255)

| # | 결함 | 처방 |
|---|---|---|
| ① | `FileRecentImageLocalDataSourceImpl#readBytes`가 `contentResolver` 전용이라 스킴 없는 절대경로를 못 읽는다 | 절대경로를 읽는 `readFileBytes(path)`를 따로 둔다. 어느 쪽으로 읽을지는 `kind`가 가른다 |
| ② | 확장자를 `.jpg`로 하드코딩한다 | `getTargetFile(bytes, extension)`으로 확장자를 인자화하고 알맹이는 `.png`를 받는다. 이름이 거짓이면 `ImageUploadRepositoryImpl#contentTypeOf`가 투명 PNG를 `image/jpeg`로 올린다 |
| ③ | 목록 스키마를 넓히면 구 스키마 디코드 실패를 `runCatching { … }.getOrDefault(emptyList())`가 삼킨다 | 위 2단 폴백. 폴백이 없으면 기존 목록이 사라지고 `clearOutsideDayWindow`가 목록 기준이라 파일을 못 지운다 |
| ④ | `NavKeySegmentationConfirm`이 인자 셋을 요구하고 "사진 편집"이 원본·마스크를 둘 다 쓴다 | `sourceImageUri`·`cutoutImagePath`를 nullable로 넓히고 트리밍 알맹이 경로만 필수로 남긴다. 재사용 항목에서는 편집 버튼이 잠긴다(🔁 **2026-09-01 develop 머지로 뒤집힘 — 이슈 #424 · PR #425**. 잠그는 대신 테두리 편집만 열고, 원본 자리에는 알맹이를 같이 넣는다 → OQ-P-338) |

### 초안을 쓰는 주체가 새로 필요하다

확인 화면은 **초안에 알맹이가 적혀 있어야** 다음 버튼이 열린다
(`SegmentationConfirmViewModel#observeDraft`의 `isDraftReady`). 지금 그것을 적는 것은 세그멘테이션
화면과 편집 결과뿐인데, 재사용 진입은 둘 다 타지 않는다. 그래서 확인 화면이 재사용 인자를 받았고
**초안이 이번 알맹이를 가리키지 않으면** 스스로 `record`를 먼저 마친 뒤 구독을 연다. 순서를 뒤집으면
첫 방출의 `null`이 `DraftMissing` 토스트를 쏘고 사용자가 없는 실패를 듣는다.
`ToppingDraftRepository#record`의 `cutoutImagePath`도 nullable로 넓힌다 — 재사용 초안에는 재편집
마스크가 없다.

판정을 "초안이 비어 있는가"로 두면 안 된다. 갤러리는 백스택에 남으므로 알맹이 A를 고른 뒤 뒤로 가
B를 고르는 경로가 실재하고, 그때 초안에는 A가 적혀 있어 B를 적지 않은 채 **A가 배치된다.** 경로
비교로 두면 프로세스 사망 복원(경로가 같다)은 여전히 건너뛰므로, 사용자가 두른 테두리를 덮어쓰지
않는다는 근거도 함께 지켜진다.

파일이 이미 지워진 항목을 골랐을 때의 안전망은 새로 만들지 않는다. `draft`가
`withExistingFilesOnly`로 걸러 빈 초안이 되고, 그때 뜨는 `DraftMissing`이 그대로 맞는 안내다.

### 범위 밖

- 알맹이 셀의 최종 디자인(뱃지·배경·섹션 분리) — 시안이 없다(OQ-P-257).
- 배경 이미지(C-301)의 재사용.
- 재사용 시 서버 재업로드 회피.

### 구현이 이 절과 갈린 자리 (as-built)

- **재사용 판정을 "초안이 비어 있는가"가 아니라 경로 비교로 바꿨다.** 위 절이 그 이유를 담고 있고,
  최초 계획은 "비어 있는가"였다. 최종 브랜치 리뷰가 A를 고른 뒤 뒤로 가 B를 고르면 A가 배치되는
  경로를 찾아 뒤집었다.
- **갤러리 클릭 인텐트를 종류별로 둘로 갈랐다**(`OnClickImage` · `OnClickCutoutImage`). 최초 계획은
  인텐트 하나가 `(uri, kind)`를 싣고 ViewModel이 uri로 목록을 되짚어 절대경로를 찾는 형태였는데,
  그 되짚기가 실패하면 탭이 로그 없이 죽었다. 지금은 알맹이 인텐트가 `RecentImage`를 통째로 나른다.
  그 덕에 날짜 그룹 사진이 확인 화면으로 새는 경로가 **타입 수준에서** 막힌다.
- **`AddRecentImageUseCase`의 `kind` 기본값을 없앴다.** 기본값이 있으면 새 호출부가 종류를 조용히
  잃고, 그 증상이 "알맹이 셀이 잘려 그려지고 누르면 누끼를 다시 딴다"로만 나타난다.
- ⚠️ **상한 `MAX_SIZE = 9`를 토핑 흐름 하나가 두 칸씩 먹는다** — 진입할 때 원본 사진이 한 칸,
  배치에 성공하면 알맹이가 한 칸이다. 최근 사진이 전보다 두 배 속도로 밀려난다. 감수한다(상한을
  종류별로 나누면 목록이 둘로 갈려 시간순 정렬이 이중이 된다). 미결은 OQ-P-258.
- **마지막 커밋은 주석 정리다**(`656cbf2e`). 브랜치가 심은 테스트 주석 넷을 걷었다 — 이미 삭제된
  임시 작업 파일을 가리키던 포인터 하나, 7줄짜리 `Then` 설명 하나, 갤러리 화면의 현재 동작을
  단정해 곧 낡을 문장 하나, 3줄짜리 스텁 함정 하나. 비주석 변경은 0줄이다.

## 파일 구성

| 자리 | 역할 |
|---|---|
| `data/di/NetworkModule.kt` | `@UploadClient` OkHttpClient 추가 |
| `data/source/image/remote/PresignedUploadDataSource` | S3 PUT 전송. raw OkHttp를 쓰는 유일한 자리 |
| `data/repository/image/ImageUploadRepositoryImpl` | 발급 → PUT → confirm |
| `data/repository/topping/ToppingRepositoryImpl` | 배치 |
| `data/source/toppingdraft/local/` | 초안 DataStore |
| `domain/repository/image/ImageUploadRepository` | 위 계약 |
| `domain/repository/topping/ToppingRepository` | 위 계약 |
| `domain/usecase/topping/AddToppingUseCase` | 업로드 → 배치 조율 |
| `core/designsystem/.../ygcanvas/YGCanvas.kt` | private 종횡비 상수를 public으로 올려 저장소의 유일한 정본으로 둔다 |
| `feature/groups/canvas/impl/.../CanvasToppingPlaceViewModel` | 좌표 변환·확정·실패 표현 |
| `core/designsystem/.../ygtoppingcutout/YGToppingCutoutImage` | 8방향 테두리 스탬프. 나눠 쓰는 화면 셋이 **모듈 둘**(`segmentation`·`groups/canvas`)에 걸쳐 있어 feature `component/`가 아니라 여기가 소유한다 |
| `domain/model/image/RecentImage`(PR6) | 최근 이미지 한 항목. uri·절대경로·종류를 함께 든다 |
| `data/source/image/local/RecentImageLocalDataSourceImpl`(PR6) | 목록 스키마 확장과 2단 폴백 디코드 |
| `data/source/file/local/FileRecentImageLocalDataSourceImpl`(PR6) | 절대경로 읽기·확장자 인자화 |
| `feature/gallery/impl/.../GalleryImageGridComponent`(PR6) | 알맹이 셀을 `Fit`으로 그린다 |
| `feature/segmentation/api/NavKeySegmentationConfirm`(PR6) | 원본·마스크 인자를 nullable로 넓힌다 |

## 테스트

| 대상 | 확인할 것 |
|---|---|
| `PresignedUploadDataSource` | PUT에 **`Authorization`이 붙지 않는다** · `Content-Type`이 발급 때 쓴 값과 같다 |
| `ImageUploadRepositoryImpl` | 3단계 순서 · 중간 실패가 그대로 올라온다 · 실패 후 다음 단계를 부르지 않는다 |
| `AddToppingUseCase` | 업로드 실패 시 배치를 부르지 않는다 |
| 좌표 변환(순수 함수) | 정중앙·모서리·회전·스케일에서 읽기 쪽 식으로 되돌리면 원래 화면 좌표가 나온다(왕복) |
| 종횡비 상수 | 상수가 하나뿐이라 컴파일이 보증한다(단언 없음) |
| 테두리 색 | 초안 ARGB → 서버 문자열 → `String#toColorOrNull` 왕복이 원래 색을 낸다 |
| `CanvasToppingPlaceViewModel` | painter 미완료 시 확인 비활성 · 연타 차단 · 영구 실패 다섯 코드는 전용 문구로 알리고 화면에 남는다, 그 외도 잔류(문구만 다르다) |
| 초안 DataStore | 흐름 진입 시 덮어쓰기 · 성공 시 비우기 · **경로는 있는데 파일이 없으면 빈 초안 취급** |
| 최근 목록 디코드(PR6) | 신 스키마·구 `List<String>`·깨진 값 셋 다에서 **기존 항목이 사라지지 않는다** |
| 최근 이미지 저장(PR6) | 종류가 읽기 경로와 확장자를 가른다 · 알맹이 저장이 성공 이펙트·`clear()`보다 **먼저** 일어난다 · 저장 실패가 배치 성공을 뒤집지 않는다 |
| 갤러리(PR6) | `returnResultOnly = true`면 알맹이가 목록에 없다 · 알맹이 클릭이 확인 화면으로 간다 |
| 재사용 진입(PR6) | 초안 `record`가 구독보다 **먼저** 끝난다(없으면 `DraftMissing`이 잘못 뜬다) · 편집 버튼이 잠긴다(🔁 2026-09-01 PR #425로 뒤집혀 테두리 편집만 연다. 초안 재기록 가드는 `SavedStateHandle` 표시로 옮겨 프로세스 사망 복원을 견딘다) |

`Authorization` 부재 검증은 형식이 아니다. 그것이 붙으면 업로드가 **아예 동작하지 않는** 이
라운드의 핵심 실패 모드다.

## 주의 / 열린 질문

- ⚠️ **편집 화면에서 본 테두리 굵기와 캔버스에서 보이는 굵기가 다를 수 있다**(OQ-P-245). 편집
  화면은 dp를 원본 픽셀 좌표계에 환산해 그리고, 캔버스는 `borderWidth`를 화면 dp로 고정해 그린다
  (토핑을 키워도 굵기가 그대로다). 어느 쪽이 정책인지 위키에 근거가 없다.
- **배치 화면과 캔버스의 클립 모양이 다르다.** 배치 화면은 사각으로 자르고 캔버스는 모서리가 잘린
  모양(`YGCanvas#CanvasArea`)으로 자른다. 모서리에 걸쳐 놓은 토핑은 캔버스에서 더 잘린다.
  "본 대로 올라간다"가 모서리에서만 성립하지 않는다.
- 고아 `PENDING` 이미지·S3 객체가 재시도마다 늘어난다. 서버에 정리 경로가 없는 것은 기존 미결이고
  이 라운드가 그 발생률을 처음으로 실제화한다.
- ✅ **판정 근거 문제는 PR5가 판정했다**(OQ-P-247 해소, ①안). 서버가 HTTP 200에 실패
  봉투를 싣는 경로가 `api/` 계약 스냅샷에 없고, 영구 실패 판정 대상 다섯 코드에 status로 갈려야
  하는 동명 코드도 없어 위 실패 처리 절이 `code` 하나로 판정하도록 바뀌었다 — `statusCode`는
  조건에 넣지 않는다.
- ✅ **업로드 확정과 배치 사이 취소는 감수로 닫혔다**(OQ-P-248 해소, 감수). 서버에 확정된 이미지만
  남고 `mapErrorToAppError`가 `CancellationException`을 재던져 그 취소가 실패 `Result`가 아니라
  예외로 화면까지 올라가는 것은 정상 계약으로 둔다 — 재시도 결정이 이미 고아 S3 객체를 감수하기로
  한 것과 같은 처분이다. 코드 변경 없이 `AddToppingUseCase` KDoc 한 줄로 남겼다.
- ✅ **PR6의 선행 결함 넷은 설계로 닫혔다**(OQ-P-255 해소). 처방은 위 [누끼 알맹이 재사용
  (PR6)](#누끼-알맹이-재사용-pr6) 절이 정본이고, 그 과정에서 **다섯 번째 결함**이 드러났다 — 재사용
  진입은 초안에 알맹이를 적는 두 경로를 모두 타지 않아 확인 화면의 다음 버튼이 열리지 않는다.
- **알맹이 셀을 무엇으로 구별할지 시안이 없다**(OQ-P-257). PR6는 잘림을 막는 `ContentScale.Fit`만
  적용하고 뱃지·배경 구분·섹션 분리는 두지 않는다.
- presigned URL 만료를 판정하지 않는다. 만료는 실패 후 전량 재시도로만 풀린다.
- `positionZ`가 흐름 진입 시점에 못 박히므로, 그 사이 남이 올린 토핑과 z가 겹칠 수 있다.
  서버가 유일성을 요구하지 않아 거부되지는 않는다.
- 배치 화면 미리보기와 캔버스 렌더가 실제로 같은 그림인지는 사람 눈으로만 확인한다
  (스크린샷 테스트 없음).
