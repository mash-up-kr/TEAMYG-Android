---
id: topping-upload-source-scaled
title: 토핑 업로드 원본 기준 축소 (Topping upload scaled by source photo)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-10
related_code:
  - UploadImagePlan#of
  - UploadImagePreprocessor
  - UploadImagePreprocessorImpl
  - ImageUploadRepository#upload
  - ImageUploadRepositoryImpl#upload
  - UploadImageUseCase
  - AddToppingUseCase
  - SegmentationResult
  - SegmentationCandidate
  - ImageSegmentationRepositoryImpl#persistSubject
  - SegmentationViewModel
  - ToppingEditViewModel
  - ToppingEditResult
  - SegmentationConfirmViewModel
  - ToppingDraft
  - ToppingDraftEntity
  - ToppingDraftRepository#record
  - CanvasToppingPlaceViewModel
related_adr: ADR-0032
related_spec: upload-image-downscale
related_architecture:
  - data-layer.md
supersedes:
superseded_by:
tags: [spec, parfait, image, upload, topping]
---

# Spec: 토핑 업로드 원본 기준 축소

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

서버로 나가는 누끼 토핑의 픽셀 수와 바이트를 줄인다. 상한을 잘린 판에 거는 대신 **누끼를 오려낸
사진 전체를 기준으로 배율을 정해** 그 배율을 잘린 판에 적용한다.

## 배경 — 관찰된 사실

iOS 팀이 토핑 로딩이 느리다고 요청해 왔다(2026-09-09). 서버에 올라간 파일 3건의 실측이다.

| 바이트 | 로딩 | 치수 |
|---|---|---|
| 41 KB | 0.10s | 167x167 |
| 94 KB | 0.11s | 860x534 |
| 633 KB | 0.37s | 925x450 |

로딩 시간이 바이트를 그대로 따라간다. 그런데 **세 건 모두 긴 변이 현행 상한
`NUKKI_LONG_SIDE_LIMIT`(1500) 아래**라 지금 규칙은 이 파일들에 아무 일도 하지 않는다.
[upload-image-downscale](2026-09-08-upload-image-downscale.md)이 상한을 잘린 판에 걸었기
때문이다. 그 스펙을 통째로 대체하지는 않는다 — 배경 규칙은 거기가 그대로 정본이고, 이 스펙은
누끼 갈래만 갈아 끼운다. 잘린 판은 알파 bbox와 같아서(`postProcess`가 `require`로 강제한다) 피사체가 프레임의
일부만 차지하면 원본이 아무리 커도 상한에 닿지 않는다.

기준을 원본으로 옮기면 그 3건이 전부 걸린다. 원본 치수는 기록되지 않아 모르지만, 12MP 카메라
사진(긴 변 4032)에서 오려냈다고 가정하면 925x450 알맹이는 배율 `1280 / 4032`를 맞아 294x143이 되고
바이트는 대략 그 제곱만큼 준다. **가정이므로 구현 후 로그로 실측한다**(아래 「주의」).

## 결정한 규칙

```
배율 = 1280 / 원본 긴 변
업로드 토핑 = 잘린 판 × 배율
```

**원본**은 세그멘테이션에 들어간 판 전체다. 카메라 경로는 뷰파인더로 잘라낸 뒤가 원본이다
(`CameraCrop`이 자른 JPEG를 파일로 떨구고 그 파일이 `decodeImage`로 들어간다). 갤러리 경로는
EXIF 회전을 보정한 판이 원본이다.

**1280을 고른 근거는 둘이다.**

- `BitmapFactory`의 `inSampleSize`가 2의 거듭제곱 계단이라 `1280 = 2^8 × 5` 쪽이 정합이 낫다.
  `UploadImagePlan#sampleSizeOf`가 고른 배수로 정확히 떨어지면 `decodeDownscaled`의 밀도 보정
  단계를 타지 않는다.
- 커스텀 카메라가 9:16 고정이라 카메라 경로가 항상 720x1280으로 떨어진다. 720p 계열이라
  팀 안에서 설명하기 쉽다.

**모델 입력은 건드리지 않는다.** 세그멘테이션은 지금처럼 원본 해상도에서 돌고, 축소는 업로드
경계에서만 일어난다. 따라서
[segmentation-preprocessing](../2026-08-23-segmentation-preprocessing.md)이 제외 항목으로 적은
「원본 다운샘플」과 충돌하지 않고, 같은 문서의 짧은 변 512 하한과도 무관하다.

**로컬 파일은 원본 해상도로 남는다.** 수동 편집(C-104)이 읽는 것은 원본 크기 캔버스 판이고
업로드되는 것은 잘린 판이다. 두 파일이 다르므로 업로드 경계에서 줄여도 편집 품질이 내려가지
않는다.

## 범위

**포함**

- 누끼 업로드의 목표 치수 판정을 원본 긴 변 기준으로 바꾼다.
- 원본 긴 변을 세그멘테이션에서 업로드 경계까지 나르는 경로를 만든다.
- 잘린 판의 하한을 둔다.
- 배경 업로드는 그대로 둔다(긴 변 2048, JPEG 품질 70).

**제외**

- **기존 업로드분 재처리** — 앞으로 올라가는 토핑에만 적용한다. 캔버스가 매일 03시에 마감·재생성
  되므로 시간이 지나면 교체된다. 지난 캔버스 조회는 여전히 옛 파일을 받는다.
- **읽기 쪽 디코딩 크기 제한** — `rememberReloadableImageRequest`가 `.size()`를 주지 않아 Coil이
  `SizeResolver.ORIGINAL`로 떨어지는 문제는 별개 라운드다. 이 스펙은 서버로 나가는 바이트만 다룬다.
- **WebP 전환** — 서버가 `image/png`·`image/jpeg` 2종만 받는다. 누끼는 알파가 필요해 PNG를 유지한다.
- **iOS 정합** — 아래 「iOS와의 관계」 참고.

## iOS와의 관계

**Android 독자 규격이다.** iOS `ToppingImageEncoder.maximumLongEdge`는 **잘린 판**에 거는 상한이고
이 스펙의 1280은 **원본**에 거는 기준이라, 두 숫자는 같은 것을 재지 않는다. 값을 맞춘다는 말 자체가
성립하지 않으므로 정합을 시도하지 않는다. 근거는 ADR-0032.

배경 상한 2048과 JPEG 품질 70은 지금처럼 iOS와 같은 값을 유지한다.

⚠️ 코드 주석과 archive 스펙에 남아 있는 「iOS 와 맞춘 값」 문장은 누끼에 한해 사실이 아니게 된다.
`UploadImagePlan`의 해당 주석을 걷고 ADR 포인터로 바꾼다.

## API / 인터페이스

원본 긴 변은 값 클래스로 나른다. `ToppingDraftRepository#record`의 인자 목록에 이미
`borderColorArgb: Int?`가 있어 벌거벗은 `Int?`를 하나 더 붙이면 인접한 두 값이 서로 바뀔 수 있다.

```kotlin
// domain/model/image/
/** 누끼를 오려낸 사진 전체의 긴 변(픽셀). 카메라는 뷰파인더로 자른 뒤가 기준이다. */
@JvmInline
value class SourceLongSide(val px: Int)
```

값이 지나는 자리와 각 자리가 아는 출처다.

| 자리 | 변경 | 값의 출처 |
|---|---|---|
| `SegmentationResult` | `sourceLongSide` 추가 | `SegmentationCandidate`의 `canvasWidth`·`canvasHeight` 중 큰 값 |
| `ToppingEditResult` | `sourceLongSide: Int?` 추가 | 편집 결과 cutout(원본 크기 판)의 긴 변 |
| `ToppingDraft` | 널 가능 필드 추가 | 위 둘, 그리고 `SegmentationViewModel#useOriginal`은 원본 비트맵 치수 |
| `ToppingDraftEntity` | 널 가능 `Int?` 필드 추가 | 매퍼가 감싸고 푼다 |
| `ToppingDraftRepository#record` | 인자 추가 | 호출부 |
| `AddToppingUseCase` | 인자 추가 | 초안 |
| `ImageUploadRepository#upload` | 인자 추가 | 위 |
| `UploadImagePreprocessor#prepare` | 인자 추가 | 위 |

```kotlin
suspend fun upload(
    filePath: String,
    imageType: ImageType,
    /** 배경은 이 값을 보지 않는다. 모르면 null — 잘린 판 상한만 걸린다. */
    sourceLongSide: SourceLongSide?,
): Result<ImageId>
```

**기본값을 두지 않는다.** 새 호출부가 생겼을 때 빠뜨린 것이 컴파일 단계에서 드러나야 한다. 배경
경로(`UploadImageUseCase`)는 `null`을 명시적으로 적는다.

`ImageType`을 새 축으로 가르지 않는다. 누끼만 이 값을 쓰는 것은 사실이나, 그 차이를 sealed 타입으로
표현하면 `ImageKeyGenerator`의 S3 키 규칙까지 따라 움직인다. 기존 분기를 재사용한다.

## 동작 / 상태

`UploadImagePlan#of`가 인자를 하나 더 받고, 현재의 `sourceSize`는 이름을 `fileSize`로 바꾼다.
그대로 두면 「원본 사진」과 「올릴 파일」이 둘 다 `source`가 되어 반드시 헷갈린다.

```kotlin
fun of(
    fileSize: UploadImageSize,          // 올릴 파일(누끼면 잘린 판)의 치수
    imageType: ImageType,
    sourceFormat: UploadImageFormat,
    sourceLongSide: SourceLongSide?,    // 누끼를 오려낸 사진 전체의 긴 변
): UploadImagePlan
```

상수는 `UploadImagePlan`이 소유한다. 두 값은 같은 숫자지만 역할이 다르다 — 하나는 배율의 분자이고
다른 하나는 결과물의 상한이다.

| 상수 | 값 | 역할 |
|---|---|---|
| `NUKKI_SOURCE_LONG_SIDE` | 1280 | 배율의 분자. 원본 긴 변을 이 값으로 본다 |
| `NUKKI_LONG_SIDE_LIMIT` | 1280 | 잘린 판 결과물의 상한. 원본을 모를 때의 방어선 |
| `NUKKI_MIN_LONG_SIDE` | 256 | 축소 **결과**의 하한. 되돌림의 상한이 `fileSize` 자신이라 확대가 되지 않는다 |
| `BACKGROUND_LONG_SIDE_LIMIT` | 2048 | 그대로 |

목표 치수의 갈래다.

| imageType | sourceLongSide | 목표 치수 |
|---|---|---|
| BACKGROUND | 무관 | `scaledSize(fileSize, 2048)` — 변경 없음 |
| NUKKI | null | `scaledSize(fileSize, 1280)` — 방어선만 |
| NUKKI | ≤ 1280 | `scaledSize(fileSize, 1280)` — 배율 갈래를 건너뛰고 방어선만 |
| NUKKI | > 1280 | 아래 배율 갈래를 지난 뒤 `scaledSize(…, 1280)`을 겹친다 |

배율 갈래는 긴 변만 계산한 뒤 그 비로 두 축을 함께 줄인다.

```
축소된 긴 변 = 잘린 판 긴 변 × (1280 ÷ 원본 긴 변)
목표 긴 변   = 축소된 긴 변을 256 까지 되돌리되, 잘린 판 긴 변을 넘지 않는다
```

**되돌림의 상한이 `fileSize` 자신이라 이 식은 확대를 만들 수 없다.** 원본 긴 변이 1280 이하인
갈래도 `fileSize`를 그대로 돌려주므로 같다. 뒤에 방어선을 겹치는 이유는 `sourceLongSide`가 잘린
판보다 작다고 주장하는 망가진 입력 때문이다.

포맷 규칙과 `sampleSize` 계산은 바꾸지 않는다. 치수·포맷이 둘 다 그대로면 `Passthrough`라는 성질도
유지된다.

### 하한

배율은 피사체 크기를 보지 않으므로 작게 찍힌 피사체가 극단적으로 줄어든다. 4032px 사진에서 오려낸
100x80 알맹이는 배율 `1280/4032`를 맞아 32x25가 된다. 캔버스 기본 배치에서 열 배 넘게 확대되어
형태를 알아볼 수 없다.

**하한은 축소 결과에 건다.** 입력에 걸면 자기 목적을 이행하지 못한다. 원본 4032 기준으로 잘린 판
640은 그대로 640으로, 641은 204로 올라가 1px 차이가 결과를 3배 가르고 **더 큰 알맹이가 더 작게**
올라간다. 결과에 걸면 두 경우가 모두 256이 되어 단조성이 회복된다.

값이 256인 근거는 앱 자신에게 있다. `ToppingOutlineCache`가 토핑의 실루엣과 터치 판정을 긴 변
256px 거리장에서 계산한다. 그 아래로는 앱이 형태를 구분하지 못하므로 더 줄일 이유가 없다.

640이 아니라 256인 이유는 이 스펙의 목표 때문이다. 640으로 두면 위 측정표의 925x450이 294x143이
아니라 640x311로 올라가 픽셀 수가 4.7배가 되고, 줄이려던 바이트의 대부분이 되돌아온다.

### 회전

`SegmentationCandidate`의 캔버스 치수는 EXIF 보정을 마친 판에서 나오고 잘린 판도 같은 좌표계라,
배율은 회전과 무관하게 일관된다. 업로드 경계의 기존 회전 굽기 로직은 그대로 둔다.

### "편집 없이 사용" 경로

`SegmentationViewModel#useOriginal`은 한 파일을 `subjectImagePath`와 `cutoutImagePath` 두 자리에
싣는다. 축소가 업로드 경계에서 일어나므로 로컬은 원본으로 남고, 배율을 적용하면 결과가 정확히 긴 변
1280이 된다. **특별 취급이 필요 없다.** 지금 이 경로는 트리밍이 없어 원본이 그대로 올라가는
유일한 구멍인데, 이 스펙이 그 구멍도 함께 막는다.

### `borderOnly` 진입

`ToppingEditResult.sourceLongSide`는 **널 가능**이고, 값 클래스가 아니라 **벌거벗은 `Int?`**다.
`ToppingEditResult`는 내비게이션 계약이고 `feature/segmentation/api`는 `:domain`을 의존하지 않는다
(ADR-0002가 그 제한을 의도된 방어기제라고 적었고, 같은 타입이 담은 `ToppingBorderLayer`가 이미 같은
이유로 색을 ARGB 정수로 내리고 있다). `SourceLongSide`로 감싸는 것은 소비처인
`SegmentationConfirmViewModel`이 한다 — 그 모듈은 이미 domain을 의존한다. 이 편집 화면은 두 방향에서 열리는데, 최근
목록에서 되살린 알맹이의 테두리만 고치는 진입에서는 `cutout`이 사진이 아니라 알맹이 자신이다. 그
긴 변을 분모로 쓰면 배율이 1에 가까워져 규칙이 무력해진다. 상태의 `isBorderOnly`로 가르고 그
갈래는 `null`을 싣는다.

같은 이유로 `SegmentationConfirmViewModel`의 재사용 진입(`isReuseEntry`)도 `null`을 적는다. 다만
**이유가 「이미 축소된 파일이라서」가 아니다** — 최근 목록에 남는 것은 업로드 전 원본 해상도 판이다
(`addRecentImageUseCase`가 초안의 `subjectImagePath`를 그대로 넘긴다). 진짜 이유는 그 알맹이를
오려낸 사진의 치수를 알 방법이 없다는 것이다.

### 로깅

`UploadImagePreprocessorImpl`이 이미 남기는 축소 전후 줄에 원본 긴 변과 배율을 덧붙인다. 전후 비교의
근거가 이 로그다.

## 파일 구성

| 파일 | 역할 | 조건 |
|---|---|---|
| `domain/model/image/SourceLongSide.kt` | 값 클래스 | 신규 |
| `domain/model/SegmentationResult.kt` | 필드 추가 | 수정 |
| `domain/model/topping/ToppingDraft.kt` | 필드 추가 | 수정 |
| `domain/repository/topping/ToppingDraftRepository.kt` | `record` 인자 추가 | 수정 |
| `domain/repository/image/ImageUploadRepository.kt` | `upload` 인자 추가 | 수정 |
| `domain/usecase/topping/AddToppingUseCase.kt` | 인자 추가·전달 | 수정 |
| `domain/usecase/image/UploadImageUseCase.kt` | `null` 명시 | 수정 |
| `data/model/local/ToppingDraftEntity.kt` | `Int?` 필드 + 매퍼 | 수정 |
| `data/model/image/UploadImagePlan.kt` | 판정 로직·상수 | 수정 |
| `data/utils/image/UploadImagePreprocessor.kt` | `prepare` 인자 추가 | 수정 |
| `data/utils/image/UploadImagePreprocessorImpl.kt` | 인자 전달·로깅 | 수정 |
| `data/repository/image/ImageUploadRepositoryImpl.kt` | 인자 전달 | 수정 |
| `data/repository/image/ImageSegmentationRepositoryImpl.kt` | `persistSubject`가 값 채움 | 수정 |
| `feature/segmentation/impl/.../SegmentationViewModel.kt` | 자동 누끼·「편집 없이 사용」 두 경로에서 `record` | 수정 |
| `feature/segmentation/impl/.../ToppingEditViewModel.kt` | 결과에 값 실음 | 수정 |
| `feature/segmentation/api/.../NavKeyToppingEdit.kt` | `ToppingEditResult`에 필드 추가 | 수정 |
| `feature/segmentation/impl/.../SegmentationConfirmViewModel.kt` | 편집 결과를 초안에 반영 | 수정 |
| `feature/groups/canvas/impl/.../CanvasToppingPlaceViewModel.kt` | 초안 값을 UseCase로 | 수정 |

## 테스트

- `UploadImagePlanTest`(JVM 순수)를 넓힌다. 배율 갈래, `sourceLongSide == null` 방어선 갈래, 256 하한 되돌림·단조성·확대 안 만듦,
  확대 금지, 배경 무영향, 그리고 원본 긴 변이 잘린 판보다 작다고 주장하는 망가진 입력.
- 초안 왕복은 `ToppingDraftLocalDataSourceImplTest`에 넣는다. **필드가 없는 기존 저장분을 읽으면
  `null`이 나오는지**가 핵심 케이스다.
- `SegmentationViewModelTest`는 자동 누끼와 「편집 없이 사용」 **두 경로**가 `record`에 실은 값을 본다. 세 번째 경로(`SegmentationConfirmViewModel`의 재사용 진입)는 그 화면의 테스트가 `null`을 단언한다.
- 매퍼 단독 테스트는 만들지 않는다. 판단이 든 변환은 DataSource 테스트 케이스로 덮는다.

## 주의 / 열린 질문

- ⚠️ **[topping-draft-usecase-extraction](2026-09-09-topping-draft-usecase-extraction.md)과 같은 자리를
  건드린다.** 그 스펙은 `status: implemented`지만 **코드는 아직 `develop`에 없다** — `usecase/topping/`에
  초안 UseCase 5종이 없고 `SegmentationViewModel`·`SegmentationConfirmViewModel`·
  `CanvasToppingPlaceViewModel`·`CanvasMainViewModel` 넷이 여전히 `ToppingDraftRepository`를 직접 받는다.
  이 스펙의 「파일 구성」은 **현재 develop 기준**으로 적었다. 그 PR이 먼저 머지되면 `record` 호출부가
  `RecordToppingDraftUseCase` 뒤로 옮겨가므로 인자 추가 지점이 한 겹 늘어난다. 구현 착수 시점에
  `develop`을 다시 확인하고, 이미 머지됐으면 UseCase 쪽에 인자를 얹는다.
  > ✅ **그렇게 됐다**(2026-09-09) — PR #479가 먼저 머지돼 UseCase 다섯이 이미 서 있었고, 인자는
  > `RecordToppingDraftUseCase`·`EnsureDraftSubjectRecordedUseCase` 쪽에 얹혔다. `record` 직접 호출은
  > 남지 않았다.

- ⚠️ **화질 손해를 받아들인 결정이다.** 작게 찍힌 피사체일수록 캔버스 확대율이 커지는데 이 규칙은
  바로 그 경우에 픽셀을 덜 준다. 256 하한이 최악값만 막는다. 로딩 개선이 목표라 감수한다.
- ⚠️ **로딩 개선의 절반은 이 스펙 밖에 있다.** Android 읽기 쪽은 `rememberAsyncImagePainter`가
  `SizeResolver.ORIGINAL`로 떨어져 표시 크기와 무관하게 원본 해상도로 디코드한다. iOS는 버킷
  계단으로 막고 있다. 별도 라운드로 민다.
- 배율 적용 전후 바이트를 실측해 기록하지 않았다. 위 배경의 3건을 새 규칙으로 다시 올려 로그로
  확인한다.
- ✅ archive의 [upload-image-downscale](2026-09-08-upload-image-downscale.md) 「결정 표」는
  **정정을 마쳤다** — JPEG 품질 90을 70으로 고치고, 누끼 행이 이 스펙으로 대체됐다는 🔁 표시와 iOS
  값 인용이 낡았다는 경고를 달았다. 코드의 품질 상수도 이미 70이다.
- ⚠️ **최근 업로드 재사용 경로는 이 규칙의 이득을 받지 못한다.** 그 알맹이는 원본 해상도 그대로
  보관되는데 오려낸 사진의 치수가 남지 않아 `null`이 흐르고, 방어선 1280만 걸린다. 이 경로가 자주
  쓰이면 개선폭이 그만큼 줄어든다. 원본 긴 변을 최근 목록에 함께 저장하면 풀리지만 별도 라운드다.

## as-built (2026-09-09, PR #480 `93cb002b5`)

설계대로 들어왔다. `SourceLongSide` 값 클래스와 값이 지나는 자리 여덟, `UploadImagePlan#of`의
인자·상수 넷, 배율·하한·방어선의 겹침 순서가 위 표와 같다. `sourceSize` → `fileSize` 개명도
그대로다. 다른 것은 넷이다.

- **`UploadImagePlan.ruleScaleOf`가 생겼다.** 「로깅」 절이 "원본 긴 변과 배율을 덧붙인다"고만
  적은 자리다. 로그가 **규칙 배율**(`1280 ÷ 원본 긴 변`)과 **실효 배율**(목표 긴 변 ÷ 잘린 판 긴
  변) 둘을 함께 남기게 되면서, 앞엣것을 내는 공개 함수가 필요해졌다. KDoc이 "축소 판정에는 쓰이지
  않는다"고 못 박는다 — 판정은 `scaledBySource` 안에서 따로 계산한다.
- **`Passthrough` 갈래에도 로그가 붙었다.** 줄이지 않았다는 사실과 그때의 원본 긴 변을 남긴다.
  실측이 목적인 라운드에서 "안 줄었다"가 침묵으로 나타나면 규칙이 안 걸린 것인지 로그가 빠진
  것인지 갈리지 않는다.
- **`ToppingEditResult`에 `domain` 의존이 잠시 들어왔다가 걷혔다**(`d72f4b06c`). 스펙이 벌거벗은
  `Int?`를 고른 이유가 바로 `feature/segmentation/api`의 의존 제한인데, 구현 중간에 그 제한이 한 번
  깨졌다가 되돌려졌다. 최종 형태는 스펙대로 `Int?`이고 감싸는 곳은 `SegmentationConfirmViewModel`이다.
- **배경 JPEG 품질을 70으로 내리는 커밋이 같은 PR에 실렸다**(`95291eda9`). 스펙 「주의」가 코드 상수도
  이미 70이라고 적었으나 실제로는 이 PR이 내린 것이다. 값·근거(iOS 정합)는 그대로다.

`ToppingEditViewModel`의 `borderOnly` 갈래는 설계대로 `null`을 싣는다. 그 밖의 진입은 편집 결과
`cutout`의 긴 변을 그대로 쓴다 — 그 판이 원본 좌표계를 유지하기 때문이다.
