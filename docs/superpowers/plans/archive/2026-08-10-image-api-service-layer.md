# image API Service·DataSource 레이어 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 image 도메인 2 엔드포인트(업로드 URL 발급 · 업로드 확인)를 TJYG-Android `:data`의 Retrofit Service와 remote DataSource로 배선하고 대응 domain VO를 만든다.

**Architecture:** `ImageService`(Retrofit·wire DTO) → `ImageRemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. 앞선 라운드([2026-08-03-data-api-service-layer](../../specs/archive/2026-08-03-data-api-service-layer.md))가 세운 관용구의 증분이라 계층·이름 규칙·타입 경계를 그대로 따른다. S3 PUT을 수행하는 앱 코드는 범위 밖이다.

**Tech Stack:** Kotlin · Retrofit2 · kotlinx-serialization · Hilt · MockK · kotlinx-coroutines-test · kotlin.test

**작업 저장소:** TJYG-Android (`mash-up-kr/TJYG-Android`). 로컬 절대경로는 private submodule `wiki/personal-private/project-paths.md` 참고.
**브랜치:** `feature/sync-backend-api-260810` (이미 존재하고 `develop`과 동일한 지점, 커밋 0개)
**스펙:** [specs/2026-08-10-image-api-service-layer.md](../../specs/archive/2026-08-10-image-api-service-layer.md)
**계약 정본:** [api/image.md](../../../api/image.md) (서버 기준선 `5bb2a3a`)

> **완료 — PR #230 `develop` 머지(2026-08-12).** Task 1~3 전량 반영. 뒤이은
> [member·parfait-image 계획](2026-08-11-member-parfait-image-api-service-layer.md)이 같은 브랜치
> (`feature/sync-backend-api-260810`) 위에 쌓여 **두 라운드가 한 PR로** 나갔고, PR #229는 별도로
> 머지되지 않았다. 아래 "최종 검증"의 `git log --oneline develop..HEAD` 커밋 3개 항목은 그래서 성립하지
> 않는다(합쳐진 브랜치의 커밋은 14개다). **산출물 계약의 정본은 스펙**이다 —
> [specs/archive](../../specs/archive/2026-08-10-image-api-service-layer.md).
> 계획 본문 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> **머지본이 계획과 갈린 곳 1건**: File Structure 표의
> `data/src/test/.../image/mapper/ImageVOMapperTest.kt`가 develop에 **없다.** 다음 라운드 Task 1이
> 매퍼 단독 테스트 금지 규약(2026-08-11 개정)에 따라 케이스를 `ImageRemoteDataSourceImplTest`로
> 옮기고 삭제했다. 브랜치 안에서 생겼다 사라져 머지 diff에는 나타나지 않는다.

## Global Constraints

- **커밋은 Task 경계마다 한다.** TJYG-Android 저장소에 로컬 커밋만 하고 **push·PR은 하지 않는다** — 사용자 승인 게이트다.
- **ktlint**: `max_line_length = 120`, `ktlint_code_style = android_studio`. 각 Task의 마지막 검증에 `./gradlew ktlintCheck`가 포함된다.
- **테스트 함수명에 백틱을 쓰지 않는다.** 형식은 `메서드명_조건_기대결과()`. 근거는 저장소 관용구다 — `PolicyRemoteDataSourceImplTest`·`PolicyVOMapperTest`가 그렇게 쓰고, `unit-test-infrastructure` 라운드가 백틱 한글 명명을 명시적으로 폐기했다. (이번 테스트는 전부 JVM local unit test라 dex를 거치지 않으므로 `minSdk`와는 무관하다.)
- **Given/When/Then 주석은 한국어**로 단다(`PolicyRemoteDataSourceImplTest`·`PolicyVOMapperTest` 관용구).
- **DTO에 value class·enum·`Duration`을 넣지 않는다.** wire 형태는 raw 타입(`Long`·`String`)만 쓰고 감싸고 벗기는 일은 mapper가 한다.
- **`@SerialName`을 전 필드에 명시한다.** 이름이 그대로여도 붙이는 것이 기존 관용구다.
- **suspend 함수는 `coEvery`/`coVerify`로 stub·verify한다.** `every`/`verify`를 쓰면 `Continuation` 인자가 매칭되지 않아 `MockKException`이 난다.
- **`@NoAuth`를 붙이지 않는다.** 두 엔드포인트 모두 서버 화이트리스트 밖이라 access token이 필요하다.
- **범위 밖**: S3 PUT을 수행하는 앱 코드, Repository·UseCase·화면 결선, 에러 코드의 도메인 예외 번역, 기존 `RecentImageLocalDataSource` 수정.

---

## File Structure

| 파일 | 책임 | Task |
|---|---|---|
| `domain/src/main/java/com/teamyg/parfait/domain/model/id/ImageId.kt` | 이미지 식별자 value class | 1 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageType.kt` | 업로드 종류 enum(앱이 보냄, 폴백 없음) | 1 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageStatus.kt` | 업로드 상태 enum(서버가 줌, `UNKNOWN` 폴백) | 1 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageUploadUrlVO.kt` | 발급 결과 VO | 1 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/image/ConfirmedImageVO.kt` | 확인 결과 VO | 1 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/request/image/IssueImageUploadUrlRequest.kt` | 발급 요청 wire DTO | 1 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/image/IssueImageUploadUrlResponse.kt` | 발급 응답 wire DTO | 1 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/image/ConfirmImageUploadResponse.kt` | 확인 응답 wire DTO | 1 |
| `data/src/main/java/com/teamyg/parfait/data/source/image/mapper/VOMapper.kt` | DTO → VO 변환(결정: status 파싱·초→Duration) | 1 |
| `data/src/test/java/com/teamyg/parfait/data/source/image/mapper/ImageVOMapperTest.kt` | mapper 결정 검증 | 1 |
| `data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt` | Retrofit 인터페이스 | 2 |
| `data/src/main/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSource.kt` | 의미 기반 인터페이스 | 2 |
| `data/src/main/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImpl.kt` | `ApiCaller` 경유 구현 | 2 |
| `data/src/test/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImplTest.kt` | 성공·실패 경로 + 요청 바디 조립 검증 | 2 |
| `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt` | `provideImageService` 추가 | 2 |
| `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt` | `bindImageRemoteDataSource` 추가 | 2 |
| `http/images.http` | 사람이 손으로 쏘는 계약 확인 요청 모음 | 3 |

Task 1은 **변환 규칙**이 산출물이고 Task 2는 **호출 경로**가 산출물이다. 리뷰어가 한쪽을 반려하면서 다른 쪽을 승인할 수 있어 경계를 여기 둔다. Task 3은 코드가 아니라 수동 확인 수단이라 따로 뗀다.

---

## Task 1: DTO · domain 타입 · VO mapper

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/id/ImageId.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageType.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageStatus.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageUploadUrlVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/image/ConfirmedImageVO.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/image/IssueImageUploadUrlRequest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/image/IssueImageUploadUrlResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/image/ConfirmImageUploadResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/mapper/VOMapper.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/image/mapper/ImageVOMapperTest.kt`

**Interfaces:**
- Consumes: 없음(첫 Task).
- Produces:
  - `ImageId(value: Long)` — `@JvmInline value class`, 패키지 `com.teamyg.parfait.domain.model.id`
  - `ImageType { NUKKI, BACKGROUND }`, `ImageStatus { PENDING, COMPLETED, UNKNOWN }` — 패키지 `com.teamyg.parfait.domain.model.image`
  - `ImageUploadUrlVO(imageId: ImageId, uploadUrl: String, imageUrl: String, expiresIn: Duration)`
  - `ConfirmedImageVO(imageId: ImageId, imageUrl: String, status: ImageStatus)`
  - `IssueImageUploadUrlRequest(fileName: String, contentType: String, imageType: String)` — 패키지 `com.teamyg.parfait.data.service.model.request.image`
  - `IssueImageUploadUrlResponse(imageId: Long, uploadUrl: String, imageUrl: String, expiresIn: Long)`, `ConfirmImageUploadResponse(imageId: Long, imageUrl: String, status: String)` — 패키지 `com.teamyg.parfait.data.service.model.response.image`
  - `internal fun IssueImageUploadUrlResponse.toImageUploadUrlVO(): ImageUploadUrlVO`
  - `internal fun ConfirmImageUploadResponse.toConfirmedImageVO(): ConfirmedImageVO`
  - 두 확장 함수 모두 패키지 `com.teamyg.parfait.data.source.image.mapper`

- [ ] **Step 1: domain 타입 5개 생성**

`domain/src/main/java/com/teamyg/parfait/domain/model/id/ImageId.kt`:

```kotlin
package com.teamyg.parfait.domain.model.id

@JvmInline
value class ImageId(val value: Long)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageType.kt`:

```kotlin
package com.teamyg.parfait.domain.model.image

/**
 * 업로드할 이미지의 용도. 앱이 서버로 보내는 값이라 알 수 없는 값이 생길 수 없어
 * UNKNOWN 폴백을 두지 않는다(서버가 주는 [ImageStatus] 와 다른 점).
 *
 * 서버는 이 이름의 소문자를 S3 키 접두사로 쓴다.
 */
enum class ImageType {
    NUKKI,
    BACKGROUND,
}
```

`domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageStatus.kt`:

```kotlin
package com.teamyg.parfait.domain.model.image

/**
 * 업로드 상태. 서버가 주는 값이라 늘어날 수 있어 UNKNOWN 폴백을 둔다.
 *
 * 확인 API 의 성공 응답은 현재 항상 COMPLETED 다 — 서버가 PENDING 인 것만 통과시켜
 * COMPLETED 로 전이시키고 이미 COMPLETED 인 것은 409 로 거르기 때문이다.
 * 그건 서버 구현의 성질이지 계약의 보장이 아니다.
 */
enum class ImageStatus {
    PENDING,
    COMPLETED,
    UNKNOWN,
}
```

`domain/src/main/java/com/teamyg/parfait/domain/model/image/ImageUploadUrlVO.kt`:

```kotlin
package com.teamyg.parfait.domain.model.image

import com.teamyg.parfait.domain.model.id.ImageId
import kotlin.time.Duration

/**
 * @param uploadUrl S3 presigned PUT URL. 한 번 쓰고 버리며 [expiresIn] 동안만 유효하다.
 * @param imageUrl 업로드 후 접근할 공개 주소. 오래 보관한다.
 *   [uploadUrl] 과 둘 다 String 이라 바꿔 넣어도 컴파일러가 막지 못한다.
 */
data class ImageUploadUrlVO(
    val imageId: ImageId,
    val uploadUrl: String,
    val imageUrl: String,
    val expiresIn: Duration,
)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/image/ConfirmedImageVO.kt`:

```kotlin
package com.teamyg.parfait.domain.model.image

import com.teamyg.parfait.domain.model.id.ImageId

data class ConfirmedImageVO(
    val imageId: ImageId,
    val imageUrl: String,
    val status: ImageStatus,
)
```

- [ ] **Step 2: wire DTO 3개 생성**

`data/src/main/java/com/teamyg/parfait/data/service/model/request/image/IssueImageUploadUrlRequest.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.request.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param fileName 서버가 현재 이 값을 쓰지 않는다 — S3 키는 UUID 로 만들고 확장자는
 *   contentType 에서 유도한다(계약 문서 `api/image.md` 미결). 서버가 쓰기 시작할 때
 *   값이 맞도록 실제 파일명을 보낸다. 빈 문자열은 서버 @NotBlank 에 걸려 400 이다.
 * @param contentType image/png 또는 image/jpeg 만 서버가 받는다. 그 외는 400 INVALID_CONTENT_TYPE.
 * @param imageType 서버 OpenAPI 의 required 목록에는 없지만 Kotlin 비널 타입이라 누락하면 400 이다.
 *   springdoc 이 required 를 Bean Validation 애노테이션에서만 유도하기 때문 —
 *   nullable 로 만들지 않는다(`api/conventions.md`).
 */
@Serializable
data class IssueImageUploadUrlRequest(
    @SerialName("fileName")
    val fileName: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("imageType")
    val imageType: String,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/response/image/IssueImageUploadUrlResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param uploadUrl S3 presigned PUT URL. 이 주소로 앱이 직접 PUT 한다(서버를 지나지 않는다).
 * @param imageUrl 업로드 후 접근할 공개 주소.
 * @param expiresIn uploadUrl 유효 시간, 초 단위. 매퍼가 Duration 으로 바꾼다.
 */
@Serializable
data class IssueImageUploadUrlResponse(
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("uploadUrl")
    val uploadUrl: String,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/response/image/ConfirmImageUploadResponse.kt`:

```kotlin
package com.teamyg.parfait.data.service.model.response.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param status ImageStatus enum 이름 문자열. 성공 응답이면 항상 COMPLETED 다 —
 *   서버는 PENDING 인 것만 통과시켜 COMPLETED 로 전이시키고, 이미 COMPLETED 인 것은
 *   409 IMAGE_ALREADY_CONFIRMED 로 거른다.
 */
@Serializable
data class ConfirmImageUploadResponse(
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("status")
    val status: String,
)
```

- [ ] **Step 3: 실패하는 mapper 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/image/mapper/ImageVOMapperTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.mapper

import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * 매퍼 테스트는 결정이 있는 곳만 다룬다. 필드를 그대로 옮기기만 하는 매퍼는 컴파일러가
 * 막아주니 테스트하지 않는다.
 *
 * 이 매퍼가 내리는 결정은 둘이다. ① status 문자열을 ImageStatus 로 옮기는 규칙 —
 * 모르는 값이 오면 예외를 던지지 않고 UNKNOWN 으로 떨어뜨린다. 누가 enumValueOf 로 바꾸면
 * 서버가 상태 하나 추가하는 순간 크래시가 난다. ② expiresIn 이 초 단위 Long 인데 VO 는
 * Duration 이다. 단위를 잘못 읽으면 밀리초로 해석돼 만료 판정이 1000배 어긋난다.
 *
 * 필드 배선도 컴파일러에 다 맡길 수 없다. uploadUrl 과 imageUrl 이 둘 다 String 이라
 * 뒤바꿔도 통과하는데, 이 둘은 의미가 정반대다(1회용 서명 URL vs 장기 공개 주소).
 */
class ImageVOMapperTest {
    private fun issueResponse(
        imageId: Long = 1L,
        uploadUrl: String = "https://bucket.s3.region.amazonaws.com/nukki/user1/key.png?sig",
        imageUrl: String = "https://bucket.s3.region.amazonaws.com/nukki/user1/key.png",
        expiresIn: Long = 900L,
    ) = IssueImageUploadUrlResponse(
        imageId = imageId,
        uploadUrl = uploadUrl,
        imageUrl = imageUrl,
        expiresIn = expiresIn,
    )

    private fun confirmResponse(
        imageId: Long = 1L,
        imageUrl: String = "https://bucket.s3.region.amazonaws.com/nukki/user1/key.png",
        status: String = "COMPLETED",
    ) = ConfirmImageUploadResponse(
        imageId = imageId,
        imageUrl = imageUrl,
        status = status,
    )

    @Test
    fun toImageUploadUrlVO_mapsEveryField() {
        // Given 서명 URL 과 공개 URL 이 서로 다른 발급 응답
        val response = issueResponse(
            imageId = 7L,
            uploadUrl = "https://example.com/upload",
            imageUrl = "https://example.com/image",
        )

        // When VO 로 변환
        val vo = response.toImageUploadUrlVO()

        // Then 두 URL 이 뒤바뀌지 않고 제자리에 들어간다 (둘 다 String 이라 컴파일러가 못 막는다)
        assertEquals(ImageId(7L), vo.imageId)
        assertEquals("https://example.com/upload", vo.uploadUrl)
        assertEquals("https://example.com/image", vo.imageUrl)
    }

    @Test
    fun toImageUploadUrlVO_expiresInIsReadAsSeconds() {
        // Given 서버가 초 단위로 준 만료 시간
        val response = issueResponse(expiresIn = 900L)

        // When VO 로 변환
        val vo = response.toImageUploadUrlVO()

        // Then 초로 해석된다 (밀리초로 읽으면 900밀리초가 돼 1000배 어긋난다)
        assertEquals(900.seconds, vo.expiresIn)
    }

    @Test
    fun toConfirmedImageVO_mapsKnownStatus() {
        // Given 서버가 아는 상태 문자열을 준다
        val completed = confirmResponse(status = "COMPLETED")
        val pending = confirmResponse(status = "PENDING")

        // When VO 로 변환
        // Then 각각 대응 enum 으로 떨어진다
        assertEquals(ImageStatus.COMPLETED, completed.toConfirmedImageVO().status)
        assertEquals(ImageStatus.PENDING, pending.toConfirmedImageVO().status)
    }

    @Test
    fun toConfirmedImageVO_unknownStatus_fallsBackToUnknown() {
        // Given 클라이언트가 모르는 상태 문자열
        val response = confirmResponse(status = "FAILED")

        // When VO 로 변환
        val vo = response.toConfirmedImageVO()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(ImageStatus.UNKNOWN, vo.status)
    }

    @Test
    fun toConfirmedImageVO_statusMatchIsCaseSensitive() {
        // Given 값은 맞지만 대소문자가 다른 상태
        val response = confirmResponse(status = "completed")

        // When VO 로 변환
        val vo = response.toConfirmedImageVO()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(ImageStatus.UNKNOWN, vo.status)
    }

    @Test
    fun toConfirmedImageVO_mapsIdAndUrl() {
        // Given 확인 응답
        val response = confirmResponse(imageId = 42L, imageUrl = "https://example.com/image")

        // When VO 로 변환
        val vo = response.toConfirmedImageVO()

        // Then id 는 value class 로 감싸이고 URL 은 그대로다
        assertEquals(ImageId(42L), vo.imageId)
        assertEquals("https://example.com/image", vo.imageUrl)
    }
}
```

- [ ] **Step 4: 테스트가 실패(컴파일 실패)하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.image.mapper.ImageVOMapperTest"`

Expected: **컴파일 실패**. `Unresolved reference: toImageUploadUrlVO` / `Unresolved reference: toConfirmedImageVO`.

Kotlin에서 아직 없는 함수를 부르는 테스트는 실행 실패가 아니라 컴파일 실패로 RED를 낸다. 이 저장소의 기존 라운드도 같은 형태다.

- [ ] **Step 5: mapper 구현**

`data/src/main/java/com/teamyg/parfait/data/source/image/mapper/VOMapper.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.mapper

import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageStatus
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO
import kotlin.time.Duration.Companion.seconds

internal fun IssueImageUploadUrlResponse.toImageUploadUrlVO(): ImageUploadUrlVO = ImageUploadUrlVO(
    imageId = ImageId(imageId),
    uploadUrl = uploadUrl,
    imageUrl = imageUrl,
    expiresIn = expiresIn.seconds,
)

internal fun ConfirmImageUploadResponse.toConfirmedImageVO(): ConfirmedImageVO = ConfirmedImageVO(
    imageId = ImageId(imageId),
    imageUrl = imageUrl,
    status = status.toImageStatus(),
)

private fun String.toImageStatus(): ImageStatus = when (this) {
    ImageStatus.PENDING.name -> ImageStatus.PENDING
    ImageStatus.COMPLETED.name -> ImageStatus.COMPLETED
    else -> ImageStatus.UNKNOWN
}
```

- [ ] **Step 6: 테스트가 통과하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.image.mapper.ImageVOMapperTest"`

Expected: PASS, 6개 테스트 전부.

- [ ] **Step 7: 테스트가 실제로 구현을 호출하는지 증명**

`toImageUploadUrlVO`의 `expiresIn = expiresIn.seconds`를 일시적으로 `expiresIn.milliseconds`로 바꾼다(import도 함께).

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.image.mapper.ImageVOMapperTest"`

Expected: `toImageUploadUrlVO_expiresInIsReadAsSeconds` FAIL.

확인 후 **원복한다**. 기존 코드 특성화 테스트는 논리적 RED가 약해 이 절차로 테스트가 살아 있음을 증명한다(`unit-test-infrastructure` 라운드 관례).

- [ ] **Step 8: ktlint 통과 확인**

Run: `./gradlew :data:ktlintCheck :domain:ktlintCheck`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/id/ImageId.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/image/ \
        data/src/main/java/com/teamyg/parfait/data/service/model/request/image/ \
        data/src/main/java/com/teamyg/parfait/data/service/model/response/image/ \
        data/src/main/java/com/teamyg/parfait/data/source/image/mapper/ \
        data/src/test/java/com/teamyg/parfait/data/source/image/mapper/
git commit -m "feat(data): image 도메인 DTO·VO·매퍼 추가

서버 image 도메인 2 엔드포인트의 wire DTO와 대응 domain VO, 변환 매퍼를 만든다.

ImageType은 앱이 보내는 값이라 UNKNOWN 폴백이 없고, ImageStatus는 서버가 주는
값이라 UNKNOWN으로 떨어뜨린다. expiresIn은 초 단위 Long을 Duration으로 바꾼다."
```

---

## Task 2: Service · RemoteDataSource · DI

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImplTest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes (Task 1이 만든 것):
  - `IssueImageUploadUrlRequest(fileName: String, contentType: String, imageType: String)`
  - `IssueImageUploadUrlResponse`, `ConfirmImageUploadResponse`
  - `ImageId(value: Long)`, `ImageType`, `ImageUploadUrlVO`, `ConfirmedImageVO`
  - `IssueImageUploadUrlResponse.toImageUploadUrlVO()`, `ConfirmImageUploadResponse.toConfirmedImageVO()`
- Consumes (기존 코드):
  - `ApiCaller.safeApiCall(block: suspend () -> ApiResponse<T>, transform: (T) -> R): Result<R>`
  - `ApiResponse<T>(success: Boolean, code: String, message: String, data: T? = null, errorDetail: Map<String, String>? = null)`
  - `ApiException.Business(code, serverMessage, statusCode, errorDetail)` · `ApiException.EmptyBody(code, serverMessage)` · `ApiException.Network(cause)` · `ApiException.Unknown(cause)`
    - ⚠️ `EmptyBody`의 두 번째 프로퍼티는 `message`가 아니라 **`serverMessage`**다. 단언을 추가할 일이 있으면 이 이름을 쓴다.
- Produces:
  - `ImageService.postImages(request: IssueImageUploadUrlRequest): ApiResponse<IssueImageUploadUrlResponse>` (suspend)
  - `ImageService.postImagesByImageIdConfirm(imageId: Long): ApiResponse<ConfirmImageUploadResponse>` (suspend)
  - `ImageRemoteDataSource.issueUploadUrl(fileName: String, contentType: String, imageType: ImageType): Result<ImageUploadUrlVO>` (suspend)
  - `ImageRemoteDataSource.confirmUpload(imageId: ImageId): Result<ConfirmedImageVO>` (suspend)

- [ ] **Step 1: `ImageService` 인터페이스 생성**

테스트가 이 인터페이스를 `mockk()`로 만들기 때문에 테스트보다 먼저 있어야 한다.

`data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt`:

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 두 엔드포인트 모두 서버 화이트리스트 밖이라 access token 이 필요하다 — @NoAuth 를 붙이지 않는다.
 *
 * 발급은 리소스를 만드는 POST 인데 201 이 아니라 200 이다(서버가 ApiResponse.ok 를 쓴다).
 * 성공 판정이 success 필드 기반이라 앱에 추가 작업은 없다.
 */
interface ImageService {
    @POST("api/v1/images")
    suspend fun postImages(@Body request: IssueImageUploadUrlRequest): ApiResponse<IssueImageUploadUrlResponse>

    @POST("api/v1/images/{imageId}/confirm")
    suspend fun postImagesByImageIdConfirm(@Path("imageId") imageId: Long): ApiResponse<ConfirmImageUploadResponse>
}
```

⚠️ **두 시그니처를 여러 줄로 쪼개면 ktlint가 실패한다.** `.editorconfig`가
`ktlint_standard_function-signature = enabled` +
`ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`라
**파라미터가 1개면 `max_line_length = 120` 안에 드는 한 한 줄로 강제**된다. 위 두 줄은 각각 111자·115자다.
같은 파일에서 `ParfaitGroupService.getParfaitGroupsByGroupId`(116자)는 한 줄이고
`deleteParfaitGroupsByGroupIdMembersMe`(125자)는 래핑돼 있다 — 경계가 정확히 120이다.

- [ ] **Step 2: 실패하는 DataSource 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ImageService
import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageStatus
import com.teamyg.parfait.domain.model.image.ImageType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ImageRemoteDataSourceImplTest {
    private val imageService: ImageService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ImageRemoteDataSourceImpl(
        imageService = imageService,
        apiCaller = apiCaller,
    )

    private fun issueSuccess(
        imageId: Long = 7L,
        expiresIn: Long = 900L,
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = IssueImageUploadUrlResponse(
            imageId = imageId,
            uploadUrl = "https://example.com/upload",
            imageUrl = "https://example.com/image",
            expiresIn = expiresIn,
        ),
    )

    private fun confirmSuccess(
        imageId: Long = 7L,
        status: String = "COMPLETED",
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = ConfirmImageUploadResponse(
            imageId = imageId,
            imageUrl = "https://example.com/image",
            status = status,
        ),
    )

    @Test
    fun issueUploadUrl_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 발급 성공 응답을 준다
        coEvery { imageService.postImages(any()) } returns issueSuccess(imageId = 7L, expiresIn = 900L)

        // When 업로드 URL 발급
        val result = dataSource.issueUploadUrl(
            fileName = "photo.png",
            contentType = "image/png",
            imageType = ImageType.NUKKI,
        )

        // Then VO 로 매핑된 성공 결과
        val vo = result.getOrThrow()
        assertEquals(ImageId(7L), vo.imageId)
        assertEquals("https://example.com/upload", vo.uploadUrl)
        assertEquals("https://example.com/image", vo.imageUrl)
        assertEquals(900.seconds, vo.expiresIn)
    }

    @Test
    fun issueUploadUrl_buildsRequestBodyFromArguments() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<IssueImageUploadUrlRequest>()
        coEvery { imageService.postImages(capture(request)) } returns issueSuccess()

        // When 파일명·MIME·용도를 넘겨 발급
        dataSource.issueUploadUrl(
            fileName = "photo.png",
            contentType = "image/png",
            imageType = ImageType.BACKGROUND,
        )

        // Then 세 인자가 그대로 실린다. imageType 은 enum 이름 문자열이다
        assertEquals("photo.png", request.captured.fileName)
        assertEquals("image/png", request.captured.contentType)
        assertEquals("BACKGROUND", request.captured.imageType)
    }

    @Test
    fun issueUploadUrl_businessFailure_returnsBusinessException() = runTest {
        // Given 지원하지 않는 MIME 이라 서버가 success=false 로 응답
        coEvery { imageService.postImages(any()) } returns ApiResponse(
            success = false,
            code = "INVALID_CONTENT_TYPE",
            message = "지원하지 않는 이미지 형식입니다",
            data = null,
        )

        // When 업로드 URL 발급
        val result = dataSource.issueUploadUrl(
            fileName = "photo.gif",
            contentType = "image/gif",
            imageType = ImageType.NUKKI,
        )

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_CONTENT_TYPE", error.code)
    }

    @Test
    fun issueUploadUrl_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { imageService.postImages(any()) } throws IOException("connection reset")

        // When 업로드 URL 발급
        val result = dataSource.issueUploadUrl(
            fileName = "photo.png",
            contentType = "image/png",
            imageType = ImageType.NUKKI,
        )

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun confirmUpload_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 확인 성공 응답을 준다
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(imageId = 7L)

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then VO 로 매핑된 성공 결과
        val vo = result.getOrThrow()
        assertEquals(ImageId(7L), vo.imageId)
        assertEquals(ImageStatus.COMPLETED, vo.status)
    }

    @Test
    fun confirmUpload_unwrapsImageIdForPathVariable() = runTest {
        // Given 성공 응답
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess()

        // When value class 로 감싼 id 로 확인 호출
        dataSource.confirmUpload(ImageId(42L))

        // Then 경로 변수에는 raw Long 이 들어간다 (Retrofit 경계에서 벗긴다)
        coVerify(exactly = 1) { imageService.postImagesByImageIdConfirm(42L) }
    }

    @Test
    fun confirmUpload_alreadyConfirmed_returnsBusinessException() = runTest {
        // Given 이미 확정된 이미지라 서버가 409 로 응답
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns ApiResponse(
            success = false,
            code = "IMAGE_ALREADY_CONFIRMED",
            message = "이미 확인된 이미지입니다",
            data = null,
        )

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then 성공으로 번역하지 않고 Business 예외로 남긴다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("IMAGE_ALREADY_CONFIRMED", error.code)
    }

    @Test
    fun confirmUpload_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
    }
}
```

- [ ] **Step 3: 테스트가 실패(컴파일 실패)하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSourceImplTest"`

Expected: **컴파일 실패**. `Unresolved reference: ImageRemoteDataSourceImpl`.

- [ ] **Step 4: DataSource 인터페이스와 구현 작성**

`data/src/main/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO

interface ImageRemoteDataSource {
    /**
     * 업로드용 presigned URL 을 발급받는다. 실제 바이트 전송은 이 함수가 하지 않는다 —
     * 호출부가 응답의 uploadUrl 로 직접 PUT 해야 하고, 그 경로는 아직 없다.
     *
     * @param fileName 서버가 현재 이 값을 쓰지 않지만(`api/image.md` 미결) 실제 파일명을 넘긴다.
     *   서버가 쓰기 시작해도 값이 맞고, 빈 문자열은 400 이다.
     * @param contentType image/png 또는 image/jpeg. 그 외는 INVALID_CONTENT_TYPE 실패다.
     */
    suspend fun issueUploadUrl(
        fileName: String,
        contentType: String,
        imageType: ImageType,
    ): Result<ImageUploadUrlVO>

    /**
     * 업로드 완료를 서버에 알려 상태를 COMPLETED 로 올린다.
     *
     * 서버는 S3 에 객체가 실제로 있는지 확인하지 않는다 — 상태 전이만 한다.
     * 이미 확정된 이미지면 IMAGE_ALREADY_CONFIRMED 로 실패하며, 이 코드를 성공으로
     * 번역하지 않는다(서버가 소유자를 검증하지 않아 "내가 이미 했다"와 "남이 했다"가
     * 구분되지 않는다 — `api/image.md`).
     */
    suspend fun confirmUpload(imageId: ImageId): Result<ConfirmedImageVO>
}
```

`data/src/main/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ImageService
import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.source.image.mapper.toConfirmedImageVO
import com.teamyg.parfait.data.source.image.mapper.toImageUploadUrlVO
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO
import javax.inject.Inject

class ImageRemoteDataSourceImpl @Inject constructor(
    private val imageService: ImageService,
    private val apiCaller: ApiCaller,
) : ImageRemoteDataSource {
    override suspend fun issueUploadUrl(
        fileName: String,
        contentType: String,
        imageType: ImageType,
    ): Result<ImageUploadUrlVO> = apiCaller.safeApiCall(
        block = {
            imageService.postImages(
                IssueImageUploadUrlRequest(
                    fileName = fileName,
                    contentType = contentType,
                    imageType = imageType.name,
                ),
            )
        },
        transform = { it.toImageUploadUrlVO() },
    )

    override suspend fun confirmUpload(imageId: ImageId): Result<ConfirmedImageVO> = apiCaller
        .safeApiCall(
            block = { imageService.postImagesByImageIdConfirm(imageId.value) },
            transform = { it.toConfirmedImageVO() },
        )
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSourceImplTest"`

Expected: PASS, 8개 테스트 전부.

- [ ] **Step 6: DI에 Service provider 추가**

`data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`의 import 목록에 `com.teamyg.parfait.data.service.ImageService`를 알파벳 순서에 맞게 넣고(`AuthService` 다음), `provideParfaitService` 뒤에 아래를 추가한다:

```kotlin
    @Provides
    @Singleton
    fun provideImageService(retrofit: Retrofit): ImageService = retrofit.create(ImageService::class.java)
```

- [ ] **Step 7: DI에 DataSource 바인딩 추가**

`data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`의 import 목록에 아래 둘을 알파벳 순서에 맞게 넣고(`group` 다음, `parfait` 앞):

```kotlin
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSourceImpl
```

`bindParfaitRemoteDataSource` 뒤에 아래를 추가한다:

```kotlin
    @Binds
    @Singleton
    fun bindImageRemoteDataSource(imageRemoteDataSourceImpl: ImageRemoteDataSourceImpl): ImageRemoteDataSource
```

- [ ] **Step 8: 전체 빌드가 통과하는지 확인**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

⚠️ **이 스텝은 DI 오배선을 잡아주지 못한다.** `ImageRemoteDataSource`를 주입받는 소비자가 이 라운드에
0건이고(Repository·UseCase·화면 전부 범위 밖) Dagger는 기본값 `dagger.fullBindingGraphValidation = NONE`에서
**엔트리포인트로부터 도달 불가능한 바인딩을 검증하지 않는다.** `provideImageService`나
`bindImageRemoteDataSource`를 통째로 빠뜨려도 빌드는 성공한다.

이 스텝이 실제로 잡는 것은 **`@Binds` 파라미터 타입 불일치·`@Provides` 반환 타입 오류 같은 컴파일 오류**뿐이다.
바인딩 존재 자체는 **소비자가 생기는 다음 라운드에서 처음 검증된다** — Step 6·7의 코드를 눈으로 대조하는
것이 이번 라운드의 유일한 그물이다.

- [ ] **Step 9: 전체 유닛 테스트와 ktlint 통과 확인**

Run: `./gradlew test ktlintCheck`

Expected: BUILD SUCCESSFUL. Task 1의 6개 + Task 2의 8개가 기존 테스트와 함께 통과한다.

- [ ] **Step 10: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt \
        data/src/main/java/com/teamyg/parfait/data/source/image/remote/ \
        data/src/test/java/com/teamyg/parfait/data/source/image/remote/ \
        data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt
git commit -m "feat(data): image API Service·RemoteDataSource 배선

POST /api/v1/images(업로드 URL 발급)와 POST /api/v1/images/{imageId}/confirm
(업로드 확인)을 Retrofit Service와 remote DataSource로 잇고 DI에 등록한다.

둘 다 서버 화이트리스트 밖이라 @NoAuth를 붙이지 않는다. 409
IMAGE_ALREADY_CONFIRMED는 성공으로 번역하지 않는다."
```

---

## Task 3: `http/images.http` 요청 모음

**Files:**
- Create: `http/images.http`

**Interfaces:**
- Consumes: 없음(코드 의존 없음). 기존 `http/http-client.env.json`의 `base_url`과 로그인 요청이 저장하는 `access_token` 전역 변수를 쓴다.
- Produces: 없음.

기존 요청 모음이 14 엔드포인트를 덮고 있었는데 서버가 16이 되며 깨졌다. 이 Task가 그 둘을 메운다.

- [ ] **Step 1: 요청 파일 작성**

`http/images.http`:

```
### 이미지 API — 업로드 URL 발급 / 업로드 확인
#
# 서버: mash-up-kr/TEAMYG-SERVER (main)
# 실행: 요청 왼쪽 ▶️ → 환경 `dev` 선택
#
# 업로드는 3단계다. ① 서버에서 presigned PUT URL 을 받고 ② 그 URL 로 S3 에 직접 PUT 하고
# ③ 서버에 완료를 알린다. ②는 서버를 지나지 않으므로 실패해도 서버 로그에 안 남는다.
#
# 두 서버 요청 모두 인증이 필요하다(화이트리스트 밖). auth.http 로 로그인해
# access_token 을 먼저 채운다.

### 1. 업로드 URL 발급
# 리소스를 만드는 POST 인데 201 이 아니라 200 이다(서버가 ApiResponse.ok 를 쓴다).
#
# ⚠️ fileName 은 @NotBlank 필수인데 서버가 쓰지 않는다 — S3 키는 UUID 로 만들고
#    확장자는 contentType 에서 유도한다. 빈 문자열을 보내면 400 이다.
# ⚠️ imageType 은 스웨거 required 목록에 없지만 빼면 400 이다(springdoc 이 required 를
#    Bean Validation 애노테이션에서만 유도한다). NUKKI 또는 BACKGROUND 만 유효하고
#    그 외 문자열은 400 INVALID_REQUEST 다(INVALID_CONTENT_TYPE 이 아니다).
# ⚠️ contentType 은 image/png · image/jpeg 2종만 받는다. 그 외는 400 INVALID_CONTENT_TYPE.
POST {{base_url}}/api/v1/images
Content-Type: application/json
Authorization: Bearer {{access_token}}

{
  "fileName": "photo.png",
  "contentType": "image/png",
  "imageType": "NUKKI"
}

> {%
    client.test("200 성공", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.success === true, "code: " + response.body.code);
    });

    // 실패했으면 전역 변수를 지운다. 그냥 두면 이전 실행의 image_id 가 남아
    // 3·4번이 엉뚱한 이미지를 확정하는데, 서버가 소유자를 검증하지 않아 200 으로 조용히 성공한다.
    const data = response.body ? response.body.data : null;
    if (!data) {
        client.global.clear("image_id");
        client.global.clear("image_upload_url");
        client.log("⚠️ 발급 실패 — image_id/image_upload_url 을 비웠다. status=" + response.status);
    } else {
        client.global.set("image_id", data.imageId);
        client.global.set("image_upload_url", data.uploadUrl);
        client.log("imageId=" + data.imageId + " expiresIn=" + data.expiresIn + "초");
        client.log("uploadUrl 저장 완료 → 다음은 '2. S3 에 직접 PUT'");
        client.log("imageUrl=" + data.imageUrl);
    }
%}

### 2. S3 에 직접 PUT (서버 계약이 아니라 AWS 계약)
# 이 요청만 {{base_url}} 이 아니라 발급받은 uploadUrl 로 나간다. 서버를 지나지 않는다.
#
# ⚠️ Content-Type 이 1번에서 보낸 contentType 과 달라야 하면 S3 가 서명 불일치로 거절한다.
#    발급 때 PutObjectRequest 에 contentType 이 실려 서명 대상에 들어가기 때문이다.
#    이 실패는 서버 로그에 남지 않는다 — 여기서만 재현할 수 있다.
# ⚠️ uploadUrl 은 발급 응답의 expiresIn(초) 동안만 유효하다. 만료됐으면 1번을 다시 실행한다.
# 바디는 아무 png 파일이면 된다. 아래는 같은 디렉토리의 sample.png 를 보낸다는 뜻이고,
# 파일이 없으면 이 요청만 실패한다(1·3번과 무관).
PUT {{image_upload_url}}
Content-Type: image/png

< ./sample.png

> {%
    client.test("200 업로드 성공", function() {
        client.assert(response.status === 200, "status: " + response.status +
            " — 403 이면 Content-Type 이 발급 때와 다르거나 URL 이 만료된 것이다");
    });
%}

### 3. 업로드 확인
# 상태를 PENDING → COMPLETED 로 올린다. 성공이면 status 는 항상 COMPLETED 다.
#
# ⚠️ 서버는 S3 에 객체가 실제로 있는지 확인하지 않는다 — 상태 전이만 한다.
#    2번을 건너뛰고 이것만 불러도 200 이 나오고, 그 imageUrl 은 404 를 뱉는 주소다.
# ⚠️ 두 번 부르면 409 IMAGE_ALREADY_CONFIRMED 다. 재시도 안전장치가 아니다.
# ⚠️ 서버가 소유자를 검증하지 않는다 — 다른 계정 토큰으로도 이 imageId 가 확정된다.
#    (서버 소관 미결. 5번에서 재현할 수 있다.)
POST {{base_url}}/api/v1/images/{{image_id}}/confirm
Authorization: Bearer {{access_token}}

> {%
    client.test("200 성공", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.success === true, "code: " + response.body.code);
    });

    client.test("status 는 COMPLETED", function() {
        client.assert(response.body.data.status === "COMPLETED",
            "status: " + response.body.data.status);
    });
%}

### 4. (대조용) 같은 이미지를 다시 확인 → 409
# 3번을 실행한 뒤 이 요청을 쏘면 409 IMAGE_ALREADY_CONFIRMED 가 난다.
POST {{base_url}}/api/v1/images/{{image_id}}/confirm
Authorization: Bearer {{access_token}}

> {%
    client.log("status=" + response.status + " code=" + (response.body ? response.body.code : "(본문 없음)"));
%}

### 5. (대조용) 존재하지 않는 imageId 확인 → 404
POST {{base_url}}/api/v1/images/999999999/confirm
Authorization: Bearer {{access_token}}

> {%
    client.log("status=" + response.status + " code=" + (response.body ? response.body.code : "(본문 없음)"));
%}

### 6. (대조용) 지원하지 않는 MIME → 400 INVALID_CONTENT_TYPE
POST {{base_url}}/api/v1/images
Content-Type: application/json
Authorization: Bearer {{access_token}}

{
  "fileName": "photo.gif",
  "contentType": "image/gif",
  "imageType": "NUKKI"
}

> {%
    client.log("status=" + response.status + " code=" + (response.body ? response.body.code : "(본문 없음)"));
%}

### 7. (대조용) imageType 누락 → 400 INVALID_REQUEST
# 스웨거 required 목록에 없어서 선택 필드처럼 보이지만 실제로는 필수다.
POST {{base_url}}/api/v1/images
Content-Type: application/json
Authorization: Bearer {{access_token}}

{
  "fileName": "photo.png",
  "contentType": "image/png"
}

> {%
    client.log("status=" + response.status + " code=" + (response.body ? response.body.code : "(본문 없음)"));
%}
```

- [ ] **Step 2: `http/README.md` 다섯 곳 갱신**

(c)~(e)는 **이번 변경으로 기존 서술이 틀리게 되는 곳**이다. 빠뜨리면 README에 사실과 다른 문장이 남는다.

**(a) 파일 목록 표** — `` | `health.http` | 헬스체크(인증 유무 대조용) | `` 줄 **뒤에** 추가
(파일명이 백틱으로 감싸여 있다):

```markdown
| `images.http` | 이미지 업로드 URL 발급 · 업로드 확인(**2번 요청만 서버가 아니라 S3로 나간다**) |
```

**(b) 디렉토리 트리 블록** — `├── health.http                   # 헬스체크` 줄 **뒤에** 추가:

```
├── images.http                   # 이미지 업로드 2종 (+ S3 PUT)
```

**(c) 에러 코드 문단** — "스웨거에 없는 에러 코드가 많다" 절의 본문에서 enum 나열에 `ImageErrorCode`를 더한다. 아래 문장으로 통째로 교체:

```markdown
스웨거는 성공 응답만 열거한다. 실제 에러 코드는 `AuthErrorCode`(12종)·`ParfaitGroupApiErrorCode`(11종)·`ImageErrorCode`(4종)·`CommonErrorCode`(2종)에 있고, 각 `.http` 파일 주석에 엔드포인트별로 적어뒀다.
```

**(d) §7 "자주 나오는 에러" 표** — image 에러 3종이 없다. `| 409 | \`ALREADY_REGISTERED\` | 이미 가입된 회원인데 signup 호출 |` 줄 **앞에** 400·404 두 줄을, **뒤에** 409 한 줄을 status 순서에 맞게 넣는다:

```markdown
| 400 | `INVALID_CONTENT_TYPE` | 이미지 MIME이 `image/png`·`image/jpeg`가 아님 |
| 404 | `IMAGE_NOT_FOUND` | 없는 `imageId`로 업로드 확인 |
| 409 | `IMAGE_ALREADY_CONFIRMED` | 이미 확정된 이미지를 다시 확인 (재시도 안전장치가 아니다) |
```

**(e) `MEMBER_NOT_FOUND` 중복 인용문** — 이번 변경으로 **enum이 둘에서 셋이 돼** 기존 문장이 틀리게 된다. 아래 문장으로 통째로 교체:

```markdown
> `MEMBER_NOT_FOUND`는 **`AuthErrorCode`에서 401, `ParfaitGroupApiErrorCode`와 `ImageErrorCode`에서 404**로 중복 정의돼 있다. 코드 문자열만으로 분기하면 세 상황이 뭉개진다 — HTTP status와 함께 봐야 한다.
```

같은 §7 표의 `| 401 | \`MEMBER_NOT_FOUND\` | … (그룹 API의 404 \`MEMBER_NOT_FOUND\`와 **다른 코드다**) |` 줄도 "그룹·이미지 API의 404"로 고친다.

`권장 순서` 줄은 고치지 않는다 — 이미지 업로드는 그룹 생성·로그아웃 사이 어디서든 독립적으로 돌릴 수 있고, 순서를 강제하면 실제보다 의존이 있는 것처럼 읽힌다.

- [ ] **Step 3: 커밋**

```bash
git add http/images.http http/README.md
git commit -m "docs(http): 이미지 업로드 요청 모음 추가

서버가 16 엔드포인트가 되며 요청 모음이 14에서 멈춰 있던 공백을 메운다.

S3 PUT 요청도 함께 둔다. Content-Type이 발급 때와 다르면 S3가 서명 불일치로
거절하는데 그 실패는 서버 로그에 남지 않아 이 파일이 재현할 유일한 자리다."
```

---

## 최종 검증

- [ ] `./gradlew test` — 신규 14개(Task 1의 6 + Task 2의 8) 포함 전량 통과
- [ ] `./gradlew ktlintCheck` — 통과
- [ ] `./gradlew :app:assembleDebug` — Hilt 그래프 해석 통과
- [ ] `git log --oneline develop..HEAD` — 커밋 3개
- [ ] **push·PR은 하지 않는다.** 사용자 승인 게이트다.

### 이 라운드가 검증하지 못하는 것

**DI 바인딩 존재** — 소비자가 0건이라 Dagger가 도달 불가능한 바인딩을 검증하지 않는다(Task 2 Step 8 참고).

**`ApiException.Business.statusCode`** — 실패 경로 테스트는 서비스가 `ApiResponse(success = false)`를
**반환**하도록 mocking하므로 `ApiCaller`의 `success == false` 분기를 타고 `statusCode`가 항상 `null`이다.
실제 400·404·409는 Retrofit이 `HttpException`을 던져 `toApiException`의 `statusCode = e.code()` 경로를 탄다.
스펙이 "`code` 단독이 아니라 `statusCode`와 함께 판정해야 한다"고 못 박은 그 축은 **이번 테스트가 잡지
않는다.** 기존 `PolicyRemoteDataSourceImplTest`도 같은 형태라 관용구 이탈은 아니고, 번역이 실제로 필요해지는
라운드(소비자 등장)에서 `HttpException` 경로 테스트를 함께 세운다.

개발 서버가 평문 HTTP인데 `usesCleartextTraffic`도 `networkSecurityConfig`도 없고 `local.properties`의
`YG_BASE_URL`이 비어 있다. **앱에서 이 API를 실제로 호출한 적은 이번 라운드 이후에도 0건이다.**
`@SerialName` 오타나 `expiresIn` 단위 오해처럼 조용히 틀리는 결함은 유닛 테스트가 매퍼까지만 잡고,
wire 형태와 서버 실응답의 일치는 Task 3의 `http/images.http`로 사람이 대조하는 것이 유일한 그물이다.

`http/images.http`를 실제로 쏘는 것은 이 계획의 필수 단계가 아니다 — 개발 서버 접근과 유효한 계정이
필요하고, 그 확인은 언제든 나중에 할 수 있다. 다만 **쏘기 전까지는 계약 일치가 미검증**이라는 것을
Task 3 완료 보고에 명시한다.
