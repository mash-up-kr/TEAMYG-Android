---
id: image
title: 이미지(업로드 URL 발급·업로드 확인)
server_module: http/image
server_commit: aa9cc9b
verified: 2026-09-04
android_status: done
related_spec:
related_adr: ADR-0017
tags: [api, parfait, server-contract, image]
---

# 이미지(업로드 URL 발급·업로드 확인) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

`feat: 이미지 업로드 URL 발급 API 구현`(PR #71)과 `feat: 이미지 업로드 확인 API 구현`(PR #73)으로
신설됐다. 토핑 누끼 PNG와 캔버스 배경 이미지가 이 경로로 올라간다.

**서버를 경유하지 않는 2단계 업로드다.** 앱이 ① 서버에서 presigned PUT URL을 받고 ② 그 URL로
S3에 직접 PUT한 뒤 ③ 서버에 업로드 완료를 알린다. 이미지 바이트가 서버 프로세스를 지나가지
않으므로, 업로드 실패·재시도·진행률은 전부 앱 쪽 책임이다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| POST | `/api/v1/images` | 필요 | `IssueImageUploadUrlRequest` | `IssueImageUploadUrlResponse` | 구현됨 |
| POST | `/api/v1/images/{imageId}/confirm` | 필요 | 없음 | `ConfirmImageUploadResponse` | 구현됨 |

두 엔드포인트 모두 `SecurityConfig.WHITELIST_PATHS`에 없어 **access token이 필요하다**.

## 엔드포인트 상세

### POST /api/v1/images

업로드용 presigned URL을 발급하고 `PENDING` 상태의 이미지 메타를 만든다.

- **인증**: 필요. `IssueImageUploadUrlController`가 `Authentication.memberId(): Long = name.toLong()`
  확장으로 memberId를 꺼내 command에 싣는다(전역 규약과 동일, [conventions.md](conventions.md) "인증").
- **성공**: HTTP 200 · envelope `code` = `"OK"`.
  ⚠️ **리소스를 만드는 POST인데 `CREATED`가 아니다.** `@ResponseStatus`가 없고 컨트롤러가
  `ApiResponse.ok(...)`를 반환한다. signup(`ResponseEntity.status(HttpStatus.CREATED)` → 201·`"CREATED"`)과
  다르다 — 성공 판정을 `code` 문자열로 하면 두 API가 갈린다.

- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `fileName` | String | 필수(`@NotBlank`) | ⚠️ **서버가 쓰지 않는다** — 아래 참고 |
| `contentType` | String | 필수(`@NotBlank`) | MIME 타입. **`image/png`·`image/jpeg` 2종만 허용** |
| `imageType` | String(enum) | 필수(non-null 타입) | `NUKKI`(토핑 누끼) · `BACKGROUND`(배경). 검증 애노테이션 없음 — 아래 참고 |

  ⚠️ **`imageType`은 OpenAPI 스키마의 `required` 목록에 없다.** springdoc이 `required`를 Bean Validation
  애노테이션에서만 유도하기 때문이다(`fileName`·`contentType`만 `@NotBlank`가 붙어 있다). Kotlin 비널
  타입이라 **실제로는 빼면 400**인데 스키마는 선택 필드로 광고한다 — 스키마에서 클라이언트 코드를
  생성하면 nullable로 떨어진다. 전역 규칙은 [conventions.md](conventions.md) "OpenAPI" 참고.

  ⚠️ **`fileName`은 요청 계약에만 있고 서버 로직에 닿지 않는다.** `IssueImageUploadUrlRequest.toCommand`가
  `memberId`·`contentType`·`imageType`만 `IssueImageUploadUrlCommand`에 싣는다. S3 키의 파일명 부분은
  `ImageKeyGenerator`가 UUID로 만들고 확장자는 `contentType`에서 유도하므로, 원본 파일명은 어디에도
  남지 않는다. 그런데 `@NotBlank`라 **빈 문자열을 보내면 400이 난다** — 앱은 쓰이지 않을 값을
  반드시 채워야 한다 → [미결](#미결).

  ⚠️ **`imageType`이 enum 밖 값이면 Jackson 역직렬화가 먼저 깨진다.** `@NotNull` 같은 검증
  애노테이션이 아니라 `HttpMessageNotReadableException`을 타고 `GlobalExceptionHandler`의
  bad-request 핸들러로 떨어져 `INVALID_REQUEST`(400)가 나간다(`IssueImageUploadUrlControllerTest`가
  이 케이스를 직접 검증한다). 도메인 에러 코드가 아니라 공통 코드라는 뜻이다.

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `imageId` | Long | 아니오 | 저장된 `image_meta` 행의 id. confirm 호출과 이후 토핑·캔버스 참조에 쓰는 키 |
| `uploadUrl` | String | 아니오 | **S3 presigned PUT URL.** 여기로 앱이 직접 PUT한다 |
| `imageUrl` | String | 아니오 | 업로드 후 접근할 공개 URL. `https://<bucket>.s3.<region>.amazonaws.com/<key>` 형태로 어댑터가 문자열 조립한다 |
| `expiresIn` | Long | 아니오 | `uploadUrl` 유효 시간(초). 설정 `aws.s3.presigned-url-expiration-seconds` 값이 그대로 내려온다 |

  **S3 키 규칙**은 `ImageKeyGenerator.generate`가 정한다 — `<imageType 소문자>/user<memberId>/<UUID>.<확장자>`.
  확장자는 `contentType` 분기(`image/png`→`png`, `image/jpeg`→`jpg`)에서 나오고, 그 외 값은
  `INVALID_CONTENT_TYPE`을 던진다. **지원 이미지 형식이 2종으로 못박혀 있다** — WebP·HEIC는 400이다.

  ⚠️ **PUT 요청의 `Content-Type` 헤더가 발급 때 보낸 `contentType`과 같아야 한다.**
  `ImageUploadUrlIssueAdapter`가 `PutObjectRequest.builder().contentType(contentType)`으로 만든 요청을
  presign하므로 그 헤더가 서명 대상에 들어간다. 앱이 다른 값으로 PUT하면 S3가 서명 불일치로 거절하고,
  그 실패는 **서버 로그에 남지 않는다**(서버를 지나지 않는 요청이다).

  **저장 시점**: presign 발급 직후 `ImageMeta.createPending`이 `status = PENDING`·`referenceCount = 0`으로
  저장된다. 즉 **실제 업로드 성공 여부와 무관하게 행이 먼저 생긴다.**

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · `imageType` enum 밖 값 · `fileName`/`contentType` 공백(`CommonErrorCode`) |
| 400 | `INVALID_CONTENT_TYPE` | `image/png`·`image/jpeg` 외 MIME(`ImageErrorCode`) |
| 404 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원(`ImageErrorCode`) |
| 401 | `UNAUTHORIZED` | 인증 실패(`AuthErrorCode`, 전역) |

  ⚠️ **`MEMBER_NOT_FOUND`가 세 번째 enum에 등장했다.** `AuthErrorCode`는 **401**,
  `ParfaitGroupApiErrorCode`와 `ImageErrorCode`는 **404**다. `code` 문자열 단독 분기가
  세 상황을 한 브랜치로 뭉갠다 → [conventions.md](conventions.md) "코드 문자열은 enum 간 유일하지 않다".

### POST /api/v1/images/{imageId}/confirm

업로드 완료를 서버에 알려 상태를 `PENDING` → `COMPLETED`로 올린다.

- **인증**: 필요(화이트리스트 밖).
  ⚠️ **그런데 컨트롤러가 `Authentication`을 받지 않는다.** `ConfirmImageUploadController.confirm`의
  파라미터는 `@PathVariable imageId`뿐이고, `ConfirmImageUploadCommand`에도 memberId가 없다.
  `ConfirmImageUploadService`는 `imageMetaQueryPort.findById`로 찾은 뒤 소유자(`uploadedByMemberId`)를
  대조하지 않는다 — **토큰만 유효하면 누구든 남의 `imageId`를 확정할 수 있다** → [미결](#미결).
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음).
- **요청 필드**: 바디 없음. 경로 변수 `imageId`(Long)뿐이다.

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `imageId` | Long | 아니오 | 확정된 이미지 id |
| `imageUrl` | String | 아니오 | 발급 때 내려준 `imageUrl`과 같은 값(`ImageMeta.url` 그대로) |
| `status` | String | 아니오 | `ImageStatus` 이름 문자열. **성공 응답이면 항상 `"COMPLETED"`** — 아래 참고 |

  **성공 조건은 `PENDING`이다.** `ImageMeta.confirm`이 `status != PENDING`이면
  `IMAGE_ALREADY_CONFIRMED`를 던진다 — 즉 **걸러지는 쪽은 이미 `COMPLETED`인 이미지**이고,
  통과한 `PENDING`이 `COMPLETED`로 전이돼 나간다. 그래서 성공 응답의 `status`는 항상 `COMPLETED`다.

  ⚠️ **서버는 S3에 객체가 실제로 있는지 확인하지 않는다.** `ConfirmImageUploadService`는 상태 전이만
  한다(`ImageMeta.confirm`). 앱이 PUT을 건너뛰고 confirm만 불러도 `COMPLETED` 행이 남고, 그 `imageUrl`은
  404를 뱉는 주소다. **업로드 성공을 보증하는 것은 앱의 PUT 응답 확인뿐이다.**

  🔁 **2026-08-15 — `referenceCount`에 증감 경로가 생겼다.** 이 도메인의 두 API는 여전히 건드리지 않고
  응답에도 내보내지 않지만, [parfait-image.md](parfait-image.md)의 **토핑 배치(POST)가 +1**,
  **토핑 삭제(DELETE)가 -1**을 한다. 그리고 **0이 되는 순간 S3 객체가 지워진다**(`ImageDeleteAdapter`).
  이전 판본의 "쓰일 자리로 보인다"는 이 라운드로 채워졌다.

  ⚠️ **`image_meta` 행은 그때도 남는다** — 카운트가 0이어도 상태는 `COMPLETED` 그대로이고 행은 삭제되지
  않는다. 즉 **S3 객체 없이 `COMPLETED`인 메타**가 생길 수 있고, 그 `imageId`는 배치 API의 상태 검사를
  통과한다(`IMAGE_NOT_CONFIRMED`가 나지 않는다) → [미결](#미결).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | `imageId`가 Long으로 변환되지 않음(`MethodArgumentTypeMismatchException` → `CommonErrorCode`) |
| 404 | `IMAGE_NOT_FOUND` | 존재하지 않는 `imageId`(`ImageErrorCode`) |
| 409 | `IMAGE_ALREADY_CONFIRMED` | 이미 `COMPLETED`인 이미지(`ImageErrorCode`) |
| 401 | `UNAUTHORIZED` | 인증 실패(`AuthErrorCode`, 전역) |

  **409는 재시도 안전장치가 아니라 실패다.** 네트워크 타임아웃 후 앱이 confirm을 재시도하면 첫 호출이
  이미 성공했을 때 409가 돌아온다. 앱은 이 코드를 "성공으로 간주"로 매핑할지 정해야 한다 → [미결](#미결).

## 도메인 에러 코드 전수

`ImageErrorCode`(`core/image/exception`) 4종 전부.

| HTTP | code | message |
|---|---|---|
| 400 | `INVALID_CONTENT_TYPE` | 지원하지 않는 이미지 형식입니다 |
| 404 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |
| 404 | `IMAGE_NOT_FOUND` | 존재하지 않는 이미지입니다 |
| 409 | `IMAGE_ALREADY_CONFIRMED` | 이미 확인된 이미지입니다 |

## Android 매핑

**표면 있음, 소비처 0**(2026-08-12, PR #230 develop 머지).

| 계약 | Android 심볼 |
|---|---|
| `POST /api/v1/images` | `ImageService.postImages` → `ImageRemoteDataSource.issueUploadUrl(fileName, contentType, imageType)` |
| `POST /api/v1/images/{imageId}/confirm` | `ImageService.postImagesByImageIdConfirm` → `ImageRemoteDataSource.confirmUpload(imageId)` |

wire DTO는 `service/model/{request,response}/image/`(`IssueImageUploadUrlRequest`·`IssueImageUploadUrlResponse`·
`ConfirmImageUploadResponse`), 변환은 `source/image/mapper/VOMapper.kt`, domain은
`domain/model/image/`(`ImageType`·`ImageStatus`·`ImageUploadUrlVO`·`ConfirmedImageVO`)와
`domain/model/id/ImageId`다. 설계 근거는
[specs/archive/2026-08-10-image-api-service-layer](../superpowers/specs/archive/2026-08-10-image-api-service-layer.md).

계약 대조에서 갈린 곳은 없다 — 함수명 규칙 2/2, `@NoAuth` 미부착(화이트리스트 밖), 전 프로퍼티 `@SerialName`,
`imageType`이 스키마 `required` 밖인데도 비널, `expiresIn` 초 → `Duration`, `status` 미지값 → `UNKNOWN` 폴백.
`ImageType`은 앱이 보내는 값이라 폴백을 두지 않았다.

**`data/source/image/local/RecentImageLocalDataSource`와 `data/source/file/local/FileRecentImageLocalDataSource`는
여전히 이 API와 무관하다** — 기기 갤러리 조회용 로컬 소스다. 같은 폴더에 성격이 다른 둘이 공존하게 됐고,
`domain` 쪽은 `image`라는 이름이 이미 기기 이미지 뜻으로 선점돼 있다
→ [open-questions](../synthesis/open-questions.md).

✅ **`android_status`가 `done`이 됐다**(2026-08-22 develop 머지, PR #334). 두 엔드포인트는
`ImageUploadRepositoryImpl`(발급 → PUT → confirm)을 거쳐 `AddToppingUseCase`에 닿고,
`CanvasToppingPlaceViewModel`이 C-106 확인 버튼에서 그 UseCase를 부른다 — **2/2 전부 화면까지
이어졌다.** `done`은 이 저장소에서 **소비 여부만 뜻한다**([README](README.md) 규약) —
⚠️ **실서버 요청 검증은 여전히 0건**(실기기 미수행)이고 그 추적은
[open-questions](../synthesis/open-questions.md) OQ-P-146이 쥔다.

✅ **3단계가 처음으로 이어졌다**(2026-08-20 develop 머지, PR #322).
`data/source/image/remote/PresignedUploadDataSource`가 S3 PUT을 수행하고,
`domain/repository/image/ImageUploadRepository`가 발급 → PUT → confirm 셋을 하나로 닫아 확정된
`ImageId`를 돌려준다. 이전 판의 "S3 PUT을 수행하는 앱 코드가 통째로 없다"는 그것으로 닫혔다.

그 경로가 지키는 것 셋:

- **전용 클라이언트 분리는 성능 선택이 아니라 기능 전제**다 — `AuthInterceptor`의 `@NoAuth` 판정이
  Retrofit `Invocation` 태그를 읽어서, 태그가 없는 raw OkHttp 요청에는 `Authorization`이 붙고
  presigned URL을 S3가 거절한다. `@UploadClient`가 그 표면이고 **로깅 인터셉터를 아예 달지 않는다**
  (presigned URL은 서명을 쿼리 스트링에 싣는 방식이라 요청 라인만 남겨도 자격증명이 샌다).
- **`contentType`을 Repository가 한 번만 정해 발급 요청과 PUT 헤더 양쪽에 넘긴다** — 위 ⚠️의
  서명 불일치를 구조적으로 불가능하게 만든다.
- **`expiresIn` 만료를 판정하지 않는다** — 만료는 실패 후 발급부터 전량 재시도로만 풀린다.

설계 근거는 [specs/2026-08-20-c106-topping-place-api](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md),
선행 결정의 판정은 [open-questions](../synthesis/open-questions.md) `OQ-P-030`·`OQ-P-110`(둘 다 해소).

✅ **소비자가 붙으면서 살아날 뻔한 결함 둘을 PR5가 미리 닫았다**(2026-08-22 develop 머지,
PR #334) — 메인 클라이언트가 발급 **응답 본문**을
`Level.BODY`로 찍어 `uploadUrl`(=자격증명)이 debug logcat에 남던 것은 `@NoBodyLog` +
`SelectiveLoggingInterceptor`로 발급 엔드포인트만 `Level.HEADERS`로 낮춰 닫았다(OQ-P-109 해소). PUT이
블로킹 `execute()`라 코루틴 취소를 따라가지 않던 것은 `enqueue` + `suspendCancellableCoroutine`으로
바꿔 닫았다(OQ-P-246 해소).

✅ **두 번째 소비자가 붙으면서 `imageType`의 두 값이 모두 쓰이게 됐다**(2026-08-22 develop 머지,
PR #329). C-301 배경 편집이 고른 사진을 `ImageType.BACKGROUND`로 올린다 — 그전까지 실제로 나가는
값은 `NUKKI` 하나뿐이었다(`AddToppingUseCase`가 스스로 정한다). 배경 쪽은 반대로 **UseCase가
`imageType`을 인자로 받는다**(`UploadImageUseCase(uri, imageType)`) — 흐름이 정해진 토핑과 달리
이쪽은 화면이 용도를 안다.

그 UseCase가 메우는 것은 **단위 차이**다. 화면이 쥔 것은 `content://` 같은 uri이고
`ImageUploadRepository.upload`는 파일 절대경로만 받으므로, `ImageFileRepository.copyToCache`로 캐시에
한 번 떨군 뒤 그 경로를 넘긴다. 두 단계 순서를 화면마다 다시 세우지 않으려는 자리다.

⚠️ **형식 판정이 이 라운드에 한 자리로 모였다** — `ImageUploadRepositoryImpl`이 확장자로 contentType을
정하던 `when` 분기가 `UploadImageFormat` enum(확장자·contentType·파일 시그니처)으로 흡수됐고, 캐시
복사 단계가 **시스템 MIME → 바이트 앞머리** 순으로 형식을 판정해 파일명 확장자를 붙인다. 확장자와
실제 내용이 어긋난 파일을 그대로 올리면 잘못 실린 contentType이 S3를 통과해 다른 기기에서 깨져
보이기 때문이다. 서버가 받지 않는 형식은 발급 요청을 보내기 전에 걸러진다(400 `INVALID_CONTENT_TYPE`
왕복이 줄었다).

📌 **판정이 한 자리 앞으로 더 갔다**(2026-08-25, PR #349) — 최근 이미지 저장
(`RecentImageRepositoryImpl#extensionOf`)이 `SOURCE`의 확장자를 종류가 아니라 **바이트**로 정한다.
`UploadImageFormat.ofBytes`를 업로드 경로 밖에서 처음 쓰는 자리이고, 여기가 `.jpg`로 못박고 있던
동안에는 갤러리에서 고른 PNG가 그 이름으로 앉아 나중에 배경으로 고를 때
`ImageFileLocalDataSourceImpl#formatOf`의 **확장자 우선** 판정이 `image/jpeg`를 실었다.
⚠️ **이미 `.jpg`로 앉은 파일은 그대로 남는다** → [open-questions](../synthesis/open-questions.md) OQ-P-283.

⚠️ **캐시가 쌓이기만 한다** — 복사본은 `cacheDir/upload`에 UUID 이름으로 남고 지우는 코드가 없다
(세그멘테이션 캐시와 달리 정리 경로가 아직 없다) → [open-questions](../synthesis/open-questions.md) OQ-P-262.
📌 **같은 디렉토리에 수명 정책이 있는 파일이 생겼다**(2026-09-09, PR #473) — 아래 전처리가 만드는
축소본은 업로드가 끝나면 `finally`에서 지운다. 복사본만 남는다.

📌 **판정 자리가 발급 직전으로 한 번 더 옮겨 갔다**(2026-09-09, PR #473 develop 머지) —
`ImageUploadRepositoryImpl#upload`이 `UploadImagePreprocessor.prepare(file, imageType)`를 먼저 부르고,
돌려받은 `PreparedUploadImage`의 **파일과 포맷을 쌍으로** 발급(`fileName`·`contentType`)과 S3 PUT에
넘긴다. 위 문단이 말한 "발급과 PUT이 같은 값을 쓴다"는 성질은 그대로이고 정하는 자리만 바뀌었다.
서버가 받지 않는 확장자를 발급 전에 끊는 일도 전처리기로 옮겨 갔다(`UnsupportedImageException`).

이 라운드가 계약 표면에 남기는 사실 둘이다.

- **나가는 `fileName`이 원본 이름이 아닐 수 있다.** 축소·재인코딩을 탄 이미지는 `cacheDir/upload`의
  새 UUID 파일이라 그 이름으로 발급을 부른다. 그대로 통과한 이미지만 종전대로다.
- **`contentType`이 원본 확장자를 따라가지 않는 갈래가 생겼다.** `ImageType.BACKGROUND`는 출력 포맷을
  **JPEG로 고정**하므로 PNG 배경은 `image/jpeg`로 발급·PUT된다(투명 영역은 흰색 합성). `NUKKI`는 알파가
  필요해 입력 포맷을 그대로 따른다. 서버가 받는 형식이 `image/png`·`image/jpeg` 둘뿐이라는 계약은
  양쪽 갈래 모두 지킨다 → [spec](../superpowers/specs/archive/2026-09-08-upload-image-downscale.md).

`http/images.http`가 두 요청 + S3 PUT을 덮는다(요청 모음 20/20 회복).

## 미결

- `fileName`이 `@NotBlank` 필수인데 서버가 쓰지 않는다 — 계약에서 뺄지, 아니면 S3 키·메타에 반영할지
  → [open-questions](../synthesis/open-questions.md)
- confirm에 소유자 검증이 없어 임의 회원이 남의 `imageId`를 확정할 수 있다
  → [open-questions](../synthesis/open-questions.md)
- 확정되지 않은 `PENDING` 이미지·업로드되지 않은 S3 키를 정리하는 경로가 여전히 없다. 2026-08-15에
  스케줄러가 서버에 처음 들어왔지만(캔버스 회전, [parfait.md](parfait.md)) **이미지 정리는 그 대상이
  아니다** → [open-questions](../synthesis/open-questions.md)
- `referenceCount`가 0이 되면 S3 객체만 지워지고 `COMPLETED` 메타 행은 남는다 — 그 `imageId` 재배치가
  깨진 이미지를 만든다 → [open-questions](../synthesis/open-questions.md)
- confirm 재시도 시의 409를 앱이 성공으로 볼지 → 위 open-questions 항목에 함께 적는다
