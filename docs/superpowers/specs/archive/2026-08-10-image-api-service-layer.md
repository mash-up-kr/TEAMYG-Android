---
id: image-api-service-layer
title: :data image API Service·remote DataSource 레이어 (2 엔드포인트)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-12
related_code: ImageService, ImageRemoteDataSource, ApiCaller, ServiceModule, RemoteDataSourceModule, PolicyService, PolicyRemoteDataSourceImpl, RecentImageLocalDataSource
related_adr: ADR-0017
related_spec: 2026-08-03-data-api-service-layer, 2026-08-02-network-envelope-token-storage
related_architecture: data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, data, network, api, image]
---

# :data image API Service·remote DataSource 레이어

서버 image 도메인 2 엔드포인트([api/image.md](../../../api/image.md), 기준선 `5bb2a3a`)를 `:data`의
Retrofit Service와 remote DataSource로 구현하고 대응 domain VO를 만든다.

**앞선 라운드가 세운 관용구를 그대로 따르는 증분이다.** 계층·이름 규칙·타입 경계는
[2026-08-03-data-api-service-layer](2026-08-03-data-api-service-layer.md)가 정본이고,
이 스펙은 그 규칙을 image 도메인에 적용하며 **규칙이 답하지 않는 지점만** 새로 결정한다.

작업 브랜치는 `feature/sync-backend-api-260810`(develop 기준).

> ✅ **develop 머지 완료(PR #230, 2026-08-12)** — 뒤이은 [member·parfait-image 라운드](2026-08-11-member-parfait-image-api-service-layer.md)가
> 이 브랜치 위에 쌓여 **두 라운드가 한 PR로** 들어갔다. 머지본 재대조에서 **설계와 갈린 곳 0건** —
> Service 함수명 2건·DTO 3건·domain 타입 5건·DataSource 시그니처·DI 2줄이 본문 그대로다.
> **본문 중 한 곳만 뒤 라운드가 덮었다**: 아래 [검증](#유닛-테스트-2건)의 `ImageVOMapperTest`는
> **머지본에 없다** — 매퍼 단독 테스트 금지 규약(2026-08-11 개정)에 따라 케이스가
> `ImageRemoteDataSourceImplTest`로 이관된 뒤 삭제됐고, 브랜치 안에서 생겼다 사라져 develop에는 흔적이 없다.
>
> 🔁 **2026-08-10 구현 완료(당시 미머지·미푸시)** — SDD 3 Task, 커밋 4개(`abd4b99a`·`d1ff627d`·`7a2f9488` + fix `f6f76813`).
> **설계에서 뒤집힌 결정 0건** — 본문이 그대로 as-built다. Task별 리뷰 3회 전부 fix 라운드 0으로 통과했고,
> 유닛 테스트 14개(매퍼 6 + DataSource 8)·`ktlintCheck`·`:app:assembleDebug` 통과.
>
> **최종 전체 리뷰(opus)가 Important 2건을 잡았고 둘 다 이 스펙이 아니라 계획서의 사실 오류였다** —
> `http/images.http` 주석이 ① 존재하지 않는 재현 절차("5번에서 재현할 수 있다"인데 5번은 404 테스트다)를
> 가리키고 ② "Content-Type이 **달라야 하면** 거절한다"로 뜻이 뒤집혀, 이 파일이 잡으라고 만들어진 함정으로
> 사람을 밀어 넣고 있었다. fix 웨이브 1회로 해소(재검수 전부 ADDRESSED).
>
> **다음 라운드로 넘긴 발견 4건**은 [open-questions](../../../synthesis/open-questions.md)에 등록했다.
> 가장 큰 것은 **`AuthInterceptor`가 S3 PUT에 Bearer를 붙인다**는 것 — `@NoAuth` 판정이 Retrofit `Invocation`
> 태그를 읽는 방식이라 raw OkHttp 요청에는 태그가 없어 `skipAuth = false`가 되고, presigned URL에
> `Authorization`이 실리면 S3가 거절한다. 즉 **업로드 전용 `OkHttpClient` 분리는 성능 선택이 아니라 기능 전제**이고,
> 이 스펙이 "타임아웃 미결 때문에 S3 PUT을 뺀다"고 적은 것보다 강한 사유가 뒤에 있었다.

## 범위

**포함** — `ImageService` · 요청/응답 DTO 3개 · `ImageRemoteDataSource`(+`Impl`) · VO mapper ·
domain VO 2개·enum 2개·value class 1개 · DI 등록 2줄 · 유닛 테스트 2건 · `http/images.http`.

**제외** — **S3 PUT을 수행하는 앱 코드 전량**, Repository·`domain/repository` 인터페이스, UseCase,
화면 결선, 업로드 타임아웃·재시도·진행률 정책, 업로드 전용 `OkHttpClient` 분리, 에러 코드의 도메인
예외 번역. (`http/images.http`의 PUT 요청은 사람이 손으로 쏘는 확인 수단이라 별개다 — 아래 [검증](#httpimageshttp) 참고.)

S3 PUT을 뺀 이유는 시간이 아니라 **선행 결정이 미결**이라서다. 업로드 요청은 서버 계약이 아니라 AWS
계약이라 `ApiCaller`·`ApiResponse` envelope가 통하지 않고, 타임아웃·재시도·전용 클라이언트 분리가
[open-questions](../../../synthesis/open-questions.md) `[2026-07-30]`에 걸려 있다. 그 항목의 보류 사유였던
"업로드 API 미구현"은 이번 서버 delta로 사라졌으므로 **다음 라운드의 첫 작업이 그 결정이다.**

## 계층과 배치

`Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. 기존과 동일하다.

```
data/service/
├── ImageService.kt
└── model/
    ├── request/image/IssueImageUploadUrlRequest.kt
    └── response/image/IssueImageUploadUrlResponse.kt · ConfirmImageUploadResponse.kt

data/source/image/
├── local/   RecentImageLocalDataSource(+Impl)   (기존 · 무수정)
├── remote/  ImageRemoteDataSource(+Impl)        (신규)
└── mapper/  VOMapper.kt                          (신규)

domain/model/image/  ImageType · ImageStatus · ImageUploadUrlVO · ConfirmedImageVO
domain/model/id/     ImageId
```

⚠️ **`data/source/image/`는 이미 존재하고, 그 안의 `local/`은 서버와 무관하다.**
`RecentImageLocalDataSource`는 **기기 갤러리의 최근 이미지**를 읽는다. 같은 폴더에 서버 업로드가
들어오지만 규칙 위반이 아니다 — 이 저장소의 규칙은 **폴더 = 도메인, 하위 = 출처**(`remote`/`local`)이고,
`auth`는 remote만 `token`은 local만 갖는 식이다. 어긋난 것은 배치가 아니라 **기존 이름**이다:
그 클래스가 다루는 것은 서버 이미지가 아니라 갤러리다. 개명은 카메라·갤러리 feature가 소비 중이라
이번 범위 밖이고 [미결](#미결)로 남긴다.

`domain/model/`에 `image` 패키지를 새로 만든다. 갤러리 쪽 `GalleryImageGroup`은 루트 평면에 있고
이 패키지로 옮기지 않는다 — 같은 이유로 다루는 대상이 다르다.

## Service 함수 이름

규칙은 **`<method><경로 세그먼트 PascalCase>`**, 경로 변수는 `By<파라미터명>`, `/api`·`/v1` 접두사 생략.

| Service 함수 | HTTP | 경로 |
|---|---|---|
| `ImageService.postImages` | POST | `/api/v1/images` |
| `ImageService.postImagesByImageIdConfirm` | POST | `/api/v1/images/{imageId}/confirm` |

**`@NoAuth`를 붙이지 않는다.** 두 엔드포인트 모두 서버 `SecurityConfig.WHITELIST_PATHS` 밖이라 access
token이 필요하다([api/conventions.md](../../../api/conventions.md) "인증"). 현재 `@NoAuth`가 붙은 곳은
화이트리스트 4경로뿐이고 이번에 늘어나지 않는다.

## 요청·응답 DTO

DTO는 **wire 형태를 그대로 비춘다** — raw 타입만 쓰고 value class·enum·`Duration`을 넣지 않는다.
`@Serializable` DTO에 value class를 쓰면 인코딩 형태가 바뀌어 계약을 흔들고, enum을 쓰면 직렬화 이름
관리 책임이 data 계층으로 샌다. 감싸고 벗기는 일은 mapper가 한다(기존 규칙, `PolicyItemResponse.type`이
같은 이유로 `String`이다).

**`IssueImageUploadUrlRequest`** — `fileName: String` · `contentType: String` · `imageType: String`

**`IssueImageUploadUrlResponse`** — `imageId: Long` · `uploadUrl: String` · `imageUrl: String` · `expiresIn: Long`

**`ConfirmImageUploadResponse`** — `imageId: Long` · `imageUrl: String` · `status: String`

confirm은 요청 바디가 없다. 경로 변수 하나뿐이라 요청 DTO를 만들지 않는다.

전 필드에 `@SerialName`을 명시한다. 이름이 그대로여도 붙이는 것이 이 저장소의 기존 관용구이고,
`isNewUser` → `newUser` 사고 이후 "이름은 우연히 같을 뿐"이라는 전제로 쓰고 있다.

## domain 타입

### value class

```kotlin
// domain/model/id/
@JvmInline value class ImageId(val value: Long)
```

**URL 2종은 raw `String`이다.** 선례가 그렇다 — `MyParfaitGroupVO.recentImageUrl`이 이미 raw다.
다만 `uploadUrl`과 `imageUrl`은 **의미가 정반대**라는 점을 기록해 둔다. `uploadUrl`은 한 번 쓰고 버리는
서명 URL(설정값 `aws.s3.presigned-url-expiration-seconds`만큼만 유효)이고 `imageUrl`은 오래 보관하는
공개 주소다. 바꿔 넣으면 서명과 만료시각이 DB·화면으로 새고, 컴파일러가 막지 못한다.
**S3 PUT을 붙이는 다음 라운드에서 이 둘을 타입으로 가를지 재검토한다** → [미결](#미결).

### enum

```kotlin
// domain/model/image/
enum class ImageType { NUKKI, BACKGROUND }
enum class ImageStatus { PENDING, COMPLETED, UNKNOWN }
```

**두 enum의 폴백 유무가 다르고, 그게 의도다.**

- `ImageType`은 **앱이 보내는 값**이라 미지 값이 생길 수 없다. `UNKNOWN`을 두면 "보낼 수 없는 값"이
  타입에 들어와 mapper가 방어 분기를 갖게 된다.
- `ImageStatus`는 **서버가 주는 값**이라 늘어날 수 있다. `PolicyType` 선례대로 `UNKNOWN` 폴백을 둬
  서버가 상태를 추가해도 역직렬화가 깨지지 않게 한다.

`NUKKI`는 토핑 누끼(위키 [[누끼-따기]]), `BACKGROUND`는 캔버스 배경이다. 서버 `ImageKeyGenerator`가
이 값의 소문자를 S3 키 접두사로 쓴다.

### VO

```kotlin
// domain/model/image/
data class ImageUploadUrlVO(
    val imageId: ImageId,
    val uploadUrl: String,
    val imageUrl: String,
    val expiresIn: Duration,
)

data class ConfirmedImageVO(
    val imageId: ImageId,
    val imageUrl: String,
    val status: ImageStatus,
)
```

`expiresIn`은 서버가 **초 단위 `Long`**을 주고 mapper가 `.seconds`로 `Duration`을 만든다
(`AuthSessionVO.expiresIn` 선례). 단위가 타입에 실려 소비 측이 밀리초로 오해할 여지가 없어진다.

`ConfirmedImageVO`를 축약하지 않은 이유: **성공 시 `status`가 항상 `COMPLETED`인 것은 현재 서버 구현의
성질이지 계약의 보장이 아니다**(`ImageMeta.confirm`이 `status != PENDING`이면 409를 던지므로, 통과하는
것은 `PENDING`뿐이고 그것이 `COMPLETED`로 전이돼 나간다). 값으로 축약하면 서버가 상태를 늘릴 때
시그니처를 되돌려야 한다. `leaveGroup`·`previewJoin`이 값으로 축약된 것과 다른 판단이며, 그 둘은
응답 필드가 실제로 하나였다.

## DataSource 시그니처

Service와 달리 **의미 기반 이름**이다.

```kotlin
ImageRemoteDataSource
  issueUploadUrl(fileName: String, contentType: String, imageType: ImageType): Result<ImageUploadUrlVO>
  confirmUpload(imageId: ImageId): Result<ConfirmedImageVO>
```

`Impl`은 기존과 같이 `ApiCaller.safeApiCall(block, transform)` 한 줄이다.

**`fileName`을 시그니처에 노출한다.** 서버가 현재 이 값을 쓰지 않지만(아래 함정 ①) 호출부는 실제
파일명을 넘긴다. 서버가 나중에 쓰기 시작해도 값이 맞고, 그때 호출부를 전부 고치지 않아도 된다.
KDoc에 "서버가 현재 쓰지 않는다"를 근거(`api/image.md` 미결)와 함께 남긴다.

`contentType`은 raw `String`이다. `image/png`·`image/jpeg` 2종만 서버가 받지만 열거형으로 좁히는 것은
이번 범위 밖이다 — 좁히면 확장자 유도(서버 `ImageKeyGenerator`)와 앱의 MIME 조회 경로가 한 타입에
묶이는데, 그 경로는 S3 PUT 라운드에서 생긴다.

## 계약이 던지는 함정

기계적 매핑으로 풀리지 않는 다섯 지점. 근거는 [api/image.md](../../../api/image.md)다.

### ① `fileName`은 필수인데 서버가 쓰지 않는다

`@NotBlank`라 **빈 문자열을 보내면 400**이고, `toCommand`가 싣지 않아 **보낸 값은 어디에도 안 남는다**.
S3 키는 UUID이고 확장자는 `contentType`에서 유도된다. 위 결정대로 실제 파일명을 넘긴다.

### ② `imageType`은 스키마 `required`에 없지만 빼면 400이다

서버가 발행한 OpenAPI의 `required`는 `fileName`·`contentType`뿐이다. springdoc이 `required`를 Bean
Validation 애노테이션에서만 유도하기 때문이고, `imageType`은 Kotlin 비널 타입이라 애노테이션이 없다
([api/conventions.md](../../../api/conventions.md) "스키마 `required`는 Bean Validation 애노테이션만 반영한다").

**결론: `IssueImageUploadUrlRequest`의 `imageType`은 nullable이 아니다.** 스키마를 근거로 삼으면
반대로 만들게 되는 자리라 명시한다.

### ③ 리소스를 만드는 POST인데 200이다

발급은 `@ResponseStatus` 없이 `ApiResponse.ok`를 반환해 **200·`code = "OK"`**다. 같은 저장소의
`signup`은 201·`"CREATED"`다. 성공 판정이 `success` 필드 기반이라([2026-08-02 라운드](2026-08-02-network-envelope-token-storage.md))
**앱에 추가 작업은 없다.** 코드 문자열로 판정했다면 갈릴 자리였다는 사실만 남긴다.

### ④ confirm은 소유자를 검증하지 않는다

서버 `ConfirmImageUploadController`가 `Authentication`을 받지 않아 **유효한 토큰이면 남의 `imageId`도
확정된다**. 서버 소관이고 [open-questions](../../../synthesis/open-questions.md) `[2026-08-10]`에 등록돼 있다.
**앱은 이번 범위에서 아무 방어도 하지 않는다** — 클라이언트가 막을 수 있는 종류가 아니다.

### ⑤ 409는 재시도 안전장치가 아니다

confirm 재시도 시 첫 호출이 이미 성공했다면 `IMAGE_ALREADY_CONFIRMED`(409)가 돌아온다. 이번 범위에서
이 코드를 성공으로 번역하지 **않는다** — ④ 때문에 409가 "내가 이미 했다"인지 "남이 했다"인지 구분되지
않아서다. 재시도 정책이 생기는 라운드(S3 PUT)에서 정한다.

### 에러 번역은 하지 않는다

`INVALID_CONTENT_TYPE`·`MEMBER_NOT_FOUND`·`IMAGE_NOT_FOUND`·`IMAGE_ALREADY_CONFIRMED` 전부
`ApiCaller`가 `ApiException.Business(code, statusCode)`로 만든다. 도메인 예외로 번역하지 않는 것은
앞선 라운드가 `MEMBER_NOT_FOUND` 중복에 대해 내린 결정과 같다 — **소비자가 생길 때 번역한다.**
`MEMBER_NOT_FOUND`가 이제 세 enum에 있고 status가 401/404/404로 갈리므로, 번역 시점에 `code` 단독이
아니라 `statusCode`와 함께 판정해야 한다(`ApiException.Business`가 둘 다 갖고 있다).

## DI

`ServiceModule`에 `provideImageService`, `RemoteDataSourceModule`에 `bindImageRemoteDataSource`를
**추가**한다. 역할 파일 분할은 하지 않는다(ADR-0017).

## 검증

### 유닛 테스트 2건

선례는 `PolicyVOMapperTest`·`PolicyRemoteDataSourceImplTest`다. "매퍼는 결정이 있는 곳만" 규약에 따라
매퍼 테스트는 **분기가 있는 변환만** 잠근다.

- **`ImageVOMapperTest`** — ① `status` 문자열이 `PENDING`·`COMPLETED`로 매핑되고 ② **미지 문자열이
  `UNKNOWN`으로 떨어지며** ③ `expiresIn` 초가 `Duration`으로 변환된다. 이 셋이 결정이 있는 자리다.
- **`ImageRemoteDataSourceImplTest`** — 두 함수의 성공 경로와 실패 경로(`ApiException.Business`)가
  `Result`로 올바르게 나오는지.

`ImageType`은 `.name` 직행이라 별도 테스트를 만들지 않는다(분기 없음).

> 📌 **as-built(2026-08-12 머지본)** — `ImageVOMapperTest`는 **develop에 없다.** 위 세 케이스는
> `ImageRemoteDataSourceImplTest`가 흡수했고(status 폴백·대소문자·`expiresIn` 초 해석·두 URL 배선),
> 파일은 다음 라운드 Task 1이 삭제했다 → [member·parfait-image 스펙](2026-08-11-member-parfait-image-api-service-layer.md) "테스트".
> develop의 `data` 유닛 테스트는 `ImageRemoteDataSourceImplTest`·`MemberRemoteDataSourceImplTest`·
> `ParfaitImageRemoteDataSourceImplTest`·`PolicyRemoteDataSourceImplTest`·`ApiCallerTest`·`AuthInterceptorTest`이고
> **`*VOMapperTest`는 0건**이다.

### 컴파일·그래프

`:data`·`:domain` 빌드 + Hilt 그래프 해석(`assembleDebug`).

### `http/images.http`

TJYG-Android 루트 `http/`에 요청 파일을 추가한다. 기존 요청 모음이 14 엔드포인트를 덮고 있었는데
서버가 16이 되며 깨진 상태다([open-questions](../../../synthesis/open-questions.md) `[2026-08-10]`).
발급·확인 두 요청과, **발급 응답의 `imageId`를 확인 요청이 쓰도록 변수 추출**을 넣는다.

**`http/README.md`도 함께 고친다** — 파일 목록·디렉토리 트리뿐 아니라 **에러 코드 서술 2곳**이 이번
변경으로 틀리게 된다. `MEMBER_NOT_FOUND`가 "`AuthErrorCode` 401 / `ParfaitGroupApiErrorCode` 404
**두** enum에 중복"이라고 적혀 있는데 이제 셋이고, "자주 나오는 에러" 표에 image 에러 3종이 없다.

**S3 PUT 요청도 이 파일에 함께 둔다.** 범위 제외인 "S3 PUT"은 **앱 코드**를 말하고, `http/`는 사람이
손으로 쏘는 확인 수단이라 성격이 다르다. 오히려 여기 있어야 하는 이유가 있다 — PUT 요청 헤더의
`Content-Type`이 발급 때 보낸 값과 달라 S3가 서명 불일치로 거절하는 실패는 **서버 로그에 남지 않으므로**
(서버를 지나지 않는 요청이다) 이 파일이 그 함정을 재현할 수 있는 유일한 자리다. 서버 계약이 아니라
AWS 계약이라는 사실을 요청 주석에 명시한다.

### 여전히 런타임 검증은 안 된다

개발 서버가 평문 HTTP인데 `usesCleartextTraffic`도 `networkSecurityConfig`도 없고 `local.properties`의
`YG_BASE_URL`이 비어 있다([api/conventions.md](../../../api/conventions.md)). 앱에서 이 API를 실제로 호출한
적은 이번 라운드 이후에도 0건이다. **조용히 틀리는 종류의 결함은 이번에도 발견되지 않는다** —
`@SerialName` 오타나 `expiresIn` 단위 오해가 그 부류다. 유닛 테스트가 매퍼까지는 잠그지만 wire 형태와
서버 실응답의 일치는 `http/` 파일로 사람이 대조하는 것이 유일한 그물이다.

## 미결

- **`uploadUrl`·`imageUrl`을 value class로 가를지** — 이번엔 raw `String`(선례 일치). 소비자가 0건이라
  섞일 자리가 없기 때문이고, S3 PUT을 붙이면 둘을 실제로 다루는 코드가 생긴다. 그때 재검토
  → [open-questions](../../../synthesis/open-questions.md)
- **`RecentImageLocalDataSource`가 다루는 것은 서버 이미지가 아니라 기기 갤러리다** — 이름이 부정확해
  `data/source/image/`에 성격이 다른 둘이 공존한다. 개명·이동은 카메라·갤러리 feature가 소비 중이라
  범위 밖 → [open-questions](../../../synthesis/open-questions.md)
- **`contentType`을 열거형으로 좁힐지** — 서버는 2종만 받는다. 앱의 MIME 조회 경로가 생기는 S3 PUT
  라운드에서 함께 정한다
- **다음 라운드의 선행 결정** — 업로드 전용 `OkHttpClient` 분리 여부·`callTimeout` 유무·`expiresIn`
  만료 시 URL 재발급 흐름. [open-questions](../../../synthesis/open-questions.md) `[2026-07-30]` 항목이
  이번 서버 delta로 보류 사유를 잃었다
