---
id: c106-pr1-image-upload-transport
title: C-106 결선 PR1 — 이미지 업로드 전송 계층 (presigned 발급·S3 PUT·확인)
status: done
type: work-order
created: 2026-08-20
updated: 2026-08-20
platforms: android
owner: Parfait 팀
related_adr: ADR-0017, ADR-0025, ADR-0026
related_spec: c106-topping-place-api
related_code:
  - NetworkModule.kt#provideUnauthenticatedOkHttpClient
  - NetworkModule.kt#loggingInterceptor
  - AuthInterceptor.kt#intercept
  - ApiCaller.kt#safeApiCallNoContent
  - ImageRemoteDataSource.kt#issueUploadUrl
  - ImageRemoteDataSource.kt#confirmUpload
  - ImageRemoteDataSourceImpl.kt
  - ImageUploadUrlVO.kt
  - ImageStatus.kt
  - UnauthenticatedClient.kt
  - ApiException.kt
  - AppErrorMapper.kt#toAppError
  - AppErrorMapper.kt#mapErrorToAppError
  - RemoteDataSourceModule.kt
  - RepositoryModule.kt
archived_reason: 구현 완료·develop 머지(PR #322 da03c9b0, 2026-08-20). PR2 브랜치가 이 커밋들을 업고 한 PR로 올라갔다.
tags: [plan, parfait, image, upload, network]
---

# C-106 결선 PR1 — 이미지 업로드 전송 계층 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** presigned URL 발급 → S3 PUT → 업로드 확인 3단계를 하나로 닫는 `ImageUploadRepository`를 만든다. 돌려주는 `ImageId`는 이미 서버에서 확정된 것이라 곧바로 토핑 배치에 쓸 수 있다.

**Architecture:** 저장소에 **S3로 바이트를 보내는 경로가 아예 없다.** 그 요청은 Retrofit이 아니라 raw OkHttp라 전용 클라이언트가 필요하고, 그 분리는 성능 선택이 아니라 **기능 전제**다 — 공유 클라이언트를 쓰면 `Authorization`이 붙어 S3가 거절한다. 아래에서 위로 세 층을 쌓는다: 전용 OkHttp 클라이언트 → PUT 전송 DataSource → 3단계를 묶는 Repository. **이 PR에는 소비자가 없다** — 화면 결선은 PR5다.

**Tech Stack:** Kotlin · OkHttp 5 · Hilt · kotlinx-coroutines-test · MockWebServer 3 · MockK · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-20-c106-topping-place-api.md`](../../specs/archive/2026-08-20-c106-topping-place-api.md)

> ✅ **실행 완료·미머지(2026-08-20)** — subagent-driven-development 로 3 태스크 + fix 라운드 2회 +
> 브랜치 최종 리뷰 1회 + fix 웨이브 1회. 신규 테스트 **22건**(계획 예상 20건 + fix 웨이브 2건),
> `:domain:test` + `:data:testDebugUnitTest` **334건 전부 통과**.
>
> ⚠️ **아래 Global Constraints 의 브랜치 항목은 낡았다.** 실제 작업은
> `feature/#270-c-106-topping-add-api`(팁 `34e1a803`) **위에 새로 만든 `feature/#270-image-upload-transport`**
> 에서 했고 커밋 6개였다. **머지·push 모두 안 했다** — 다음 PR 은 이 브랜치 위에 쌓는다.
>
> 🔁 **2026-08-20 저녁, develop(`36719e8e`) 위로 리베이스했다.** 커밋 6개는
> `3467ffe5..496d55f1` 이 됐고 충돌은 0건이었다. **얹혀 있던 문서 커밋 넷은 리베이스가 걷어냈다** —
> `PARFAIT_ALREADY_CLOSED` 상수·`http/` 문서가 그사이 develop 에 같은 내용으로 들어가(PR #318)
> git 이 이미 적용된 것으로 판정했다. 그래서 이 브랜치는 이제 업로드 전송 계층 커밋만 담는다.
>
> 실행 중 계획을 뒤집은 판정 둘: ① 업로드 클라이언트의 로깅 인터셉터를 통째로 제거(Task 1 각주 참고)
> ② `ImageUploadRepositoryImpl` 주석에서 세그멘테이션 캐시 정리 시점을 단정하던 절을 삭제.
> 미룬 것 둘은 **PR5 선행**으로 [OQ-P-109·OQ-P-246](../../../synthesis/open-questions.md)에 등록했다.

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**이고 이 문서가 사는 저장소가 아니다. 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다.
- **브랜치는 `feature/#270-c-106-topping-add-api`** 위에서 이어 간다. 그 브랜치에는 이미 사전 커밋 넷(`PARFAIT_ALREADY_CLOSED` 상수·`http/` 문서)이 있고 그대로 둔다.
- **커밋은 태스크마다 한다.** `git push`·`gh pr create`·`gh pr merge`는 **하지 않는다** — 사용자 확인이 필요한 작업이다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 선언에 멀티라인을 강제한다**(`.editorconfig`의 `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`). 이 계획의 코드 블록은 이미 그 형태로 적혀 있으니 **한 줄로 줄이지 말 것.**
- **주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 근거는 문서를 가리킨다. 함정과 의도는 쓴다.
  - 아키텍처 결정 설명을 코드에 복사하지 않는다. 포인터 한 줄만 둔다.
- **`contentType`은 한 곳에서만 정한다.** 발급 요청과 PUT 헤더 양쪽이 같은 값을 써야 한다 — 둘 다 S3 서명 대상이고, 어긋난 실패는 서버 로그에 남지 않는다.
- **`filePath`는 파일 시스템 절대경로**다. `file://` uri가 아니다.
- 매퍼 단독 테스트(`XxxVOMapperTest`)는 만들지 않는다. 변환 판단은 DataSource·Repository 테스트 케이스로 잠근다.
- 검증 명령(태스크마다 전부 통과해야 한다):
  ```bash
  ./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
  ```
  마지막 태스크에서만 `./gradlew :app:assembleDebug`까지 돌린다.

## 파일 구성

| 파일 | 책임 |
|---|---|
| `data/model/qualifier/UploadClient.kt` (신규) | S3 전송 표면을 가리키는 Hilt 한정자 |
| `data/di/NetworkModule.kt` (수정) | 업로드 전용 `OkHttpClient` 제공 |
| `data/model/exception/PresignedUploadException.kt` (신규) | S3가 준 비-2xx 상태 코드를 실어 나른다 |
| `data/source/image/remote/PresignedUploadDataSource.kt` (신규) | PUT 전송 계약 |
| `data/source/image/remote/PresignedUploadDataSourceImpl.kt` (신규) | raw OkHttp로 파일을 스트리밍 전송 |
| `data/di/RemoteDataSourceModule.kt` (수정) | 위 구현 바인딩 |
| `domain/repository/image/ImageUploadRepository.kt` (신규) | 3단계를 하나로 닫는 도메인 계약 |
| `data/repository/image/ImageUploadRepositoryImpl.kt` (신규) | 발급 → PUT → 확인 조율 + `AppError` 변환 |
| `data/di/RepositoryModule.kt` (수정) | 위 구현 바인딩 |

---

### Task 1: 업로드 전용 OkHttp 클라이언트

> 🔁 **as-built가 이 태스크 텍스트를 뒤집었다(fix round 1).** 아래 Step 4는 디버그 빌드에서
> `HttpLoggingInterceptor.Level.HEADERS`를 켜라고 지시하지만, 태스크 리뷰가 "`HEADERS` 이상은
> 요청 라인을 남기는데 presigned URL은 쿼리 스트링이 곧 자격증명"이라고 지적해 **로깅 인터셉터를
> 통째로 제거**하는 것으로 판정됐다. 테스트도 `hasNoLoggingInterceptor`로 강화됐다.
> 스펙 "업로드 전송" 절이 정본이다.

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/model/qualifier/UploadClient.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/di/UploadOkHttpClientTest.kt`

**Interfaces:**
- Consumes: 없음(이 PR의 첫 태스크)
- Produces: `@UploadClient` 한정자 · `NetworkModule.provideUploadOkHttpClient(): OkHttpClient`

- [ ] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/di/UploadOkHttpClientTest.kt`:

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.network.AuthInterceptor
import okhttp3.Authenticator
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadOkHttpClientTest {
    @Test
    fun provideUploadOkHttpClient_hasNoAuthInterceptor() {
        // Given·When 업로드 전용 클라이언트를 만든다
        val client = NetworkModule.provideUploadOkHttpClient()

        // Then 자격증명을 붙이는 인터셉터가 없다 — 붙으면 presigned URL 을 S3 가 거절한다
        assertFalse(client.interceptors.any { it is AuthInterceptor })
    }

    @Test
    fun provideUploadOkHttpClient_hasNoAuthenticator() {
        // Given·When 업로드 전용 클라이언트를 만든다
        val client = NetworkModule.provideUploadOkHttpClient()

        // Then 401 을 만나도 재발급을 시도하지 않는다
        assertEquals(Authenticator.NONE, client.authenticator)
    }

    @Test
    fun provideUploadOkHttpClient_doesNotLogRequestBody() {
        // Given·When 업로드 전용 클라이언트를 만든다
        val client = NetworkModule.provideUploadOkHttpClient()

        // Then 본문 로깅이 없다 — 원본 해상도 이미지가 매 업로드마다 문자열로 힙에 올라간다
        val logging = client.interceptors.filterIsInstance<HttpLoggingInterceptor>()
        assertTrue(logging.none { it.level == HttpLoggingInterceptor.Level.BODY })
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.di.UploadOkHttpClientTest"
```

Expected: 컴파일 실패 — `Unresolved reference: provideUploadOkHttpClient`

- [ ] **Step 3: 한정자를 만든다**

`data/src/main/java/com/teamyg/parfait/data/model/qualifier/UploadClient.kt`:

```kotlin
package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * S3 presigned URL 로 파일 바이트를 보내는 표면.
 *
 * 자격증명을 붙이지 않고, 재발급 표면과 `Dispatcher` 를 공유하지 않으며, 본문을 로깅하지 않는다.
 * 이름은 사용처가 아니라 이 표면의 성질을 가리킨다.
 *
 * 이웃 [UnauthenticatedClient] 와 달리 리텐션이 `RUNTIME` 이다 — 이 한정자가 주입 자리에서
 * 빠지는 것이 이 라운드의 핵심 실패 모드인데, 이 PR 에는 소비자가 없어 Dagger 가 그래프를
 * 검증하지 않는다. 리플렉션 테스트가 유일한 감지선이라 런타임까지 남겨 둔다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UploadClient
```

- [ ] **Step 4: 제공자를 만든다**

`NetworkModule.kt`에 import를 더한다:

```kotlin
import com.teamyg.parfait.data.model.qualifier.UploadClient
```

`provideUnauthenticatedOkHttpClient` 바로 아래에 제공자를 더한다:

```kotlin
    /**
     * S3 presigned PUT 전용. **자격증명을 붙이지 않는 것이 이 클라이언트의 존재 이유다** —
     * presigned URL 에 `Authorization` 이 실리면 S3 가 거절해 업로드가 아예 동작하지 않는다.
     * 재발급 표면([provideUnauthenticatedOkHttpClient])을 재사용하지 않는 이유를 포함한 근거는
     * `specs/archive/2026-08-20-c106-topping-place-api.md` 업로드 전송 절에 있다.
     *
     * ⚠️ `newBuilder()` 로 파생하면 부모의 [Dispatcher] 를 물려받아 격리가 사라진다.
     * 반드시 새 [OkHttpClient.Builder] 로 만든다.
     *
     * 본문 로깅을 붙이지 않는 것과 이 표면만 `callTimeout` 을 두는 것이 의도다 —
     * `writeTimeout` 은 바이트 사이 유휴 상한이라 전송 전체가 느린 것을 잡지 못한다.
     */
    @Provides
    @Singleton
    @UploadClient
    fun provideUploadOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .dispatcher(Dispatcher())
        .addInterceptor(uploadLoggingInterceptor())
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPLOAD_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(UPLOAD_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
```

`loggingInterceptor()` 바로 아래에 더한다:

```kotlin
    private fun uploadLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
    }
```

기존 상수 옆에 둘을 더한다:

```kotlin
    private const val UPLOAD_WRITE_TIMEOUT_SECONDS = 60L
    private const val UPLOAD_CALL_TIMEOUT_SECONDS = 120L
```

- [ ] **Step 5: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
```

Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/model/qualifier/UploadClient.kt \
        data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt \
        data/src/test/java/com/teamyg/parfait/data/di/UploadOkHttpClientTest.kt
git commit -m "feat(network): 업로드 전용 OkHttpClient 를 분리한다

presigned URL 에 Authorization 이 실리면 S3 가 거절한다. 재발급 전용
클라이언트를 재사용하지 않는 이유는 그쪽 Dispatcher 를 업로드가 오래
점유하면 재발급이 다시 굶기 때문이다."
```

---

### Task 2: presigned PUT 전송 DataSource

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/model/exception/PresignedUploadException.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `@UploadClient` 한정자 · `NetworkModule.provideUploadOkHttpClient()`
- Produces:
  - `PresignedUploadDataSource.put(uploadUrl: String, contentType: String, file: File): Result<Unit>`
  - `PresignedUploadException(val statusCode: Int)`

- [ ] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.di.NetworkModule
import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.PresignedUploadException
import com.teamyg.parfait.data.model.qualifier.UploadClient
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresignedUploadDataSourceImplTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: PresignedUploadDataSource
    private lateinit var file: File

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        dataSource = PresignedUploadDataSourceImpl(NetworkModule.provideUploadOkHttpClient())
        file = File.createTempFile("topping", ".png")
        file.writeBytes(ByteArray(FILE_SIZE) { index -> index.toByte() })
    }

    @AfterTest
    fun tearDown() {
        server.close()
        file.delete()
    }

    @Test
    fun impl_injectsUploadQualifiedClient() {
        // Given·When 생성자에 붙은 한정자를 본다
        val qualifiers = PresignedUploadDataSourceImpl::class.java
            .declaredConstructors
            .single()
            .parameterAnnotations
            .single()
            .map { annotation -> annotation.annotationClass }

        // Then @UploadClient 다. 빠지면 공유 클라이언트가 주입돼 Authorization 이 붙고 업로드가
        // 통째로 죽는데, 이 PR 은 소비자가 0 이라 컴파일도 assembleDebug 도 그것을 못 잡는다
        assertTrue(UploadClient::class in qualifiers)
    }

    @Test
    fun put_serverAccepts_sendsPutWithGivenContentType() = runTest {
        // Given S3 가 200 을 준다
        server.enqueue(MockResponse.Builder().code(200).build())

        // When 발급 때 쓴 contentType 으로 올린다
        val result = dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then 성공하고 PUT 으로 그 타입이 그대로 나간다 — 발급 값과 어긋나면 S3 가 거절한다
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("image/png", recorded.headers["Content-Type"])
    }

    @Test
    fun put_serverAccepts_doesNotSendAuthorizationHeader() = runTest {
        // Given S3 가 200 을 준다
        server.enqueue(MockResponse.Builder().code(200).build())

        // When 올린다
        dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then Authorization 이 없다. 붙으면 S3 가 서명 수단 중복으로 거절해 업로드가 아예 안 된다
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun put_serverAccepts_sendsWholeFile() = runTest {
        // Given S3 가 200 을 준다
        server.enqueue(MockResponse.Builder().code(200).build())

        // When 올린다
        dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then 파일 전체가 나간다
        val recorded = server.takeRequest()
        assertEquals(file.length().toString(), recorded.headers["Content-Length"])
    }

    @Test
    fun put_serverRejects_failsWithStatusCode() = runTest {
        // Given S3 가 403 으로 거절한다(서명 불일치·만료가 이 모양으로 온다)
        server.enqueue(MockResponse.Builder().code(403).build())

        // When 올린다
        val result = dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then 상태 코드를 실은 실패다 — S3 거절은 서버 로그에 안 남아 이 값이 유일한 단서다
        val unknown = assertIs<ApiException.Unknown>(result.exceptionOrNull())
        val cause = assertIs<PresignedUploadException>(unknown.cause)
        assertEquals(403, cause.statusCode)
    }

    @Test
    fun put_malformedUploadUrl_failsInsteadOfThrowing() = runTest {
        // Given 서버가 준 uploadUrl 이 http/https 가 아니다

        // When 올린다
        val result = dataSource.put(
            uploadUrl = "not a url",
            contentType = "image/png",
            file = file,
        )

        // Then 예외로 새지 않고 Result 로 돌아온다. uploadUrl 은 서버가 주는 값이라 앱이 통제 못 한다
        assertIs<ApiException.Unknown>(result.exceptionOrNull())
    }

    @Test
    fun put_connectionFails_failsAsNetwork() = runTest {
        // Given 아무도 듣지 않는 포트다

        // When 올린다
        val result = dataSource.put(
            uploadUrl = UNREACHABLE_URL,
            contentType = "image/png",
            file = file,
        )

        // Then 재시도가 의미 있는 갈래로 분류된다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    private companion object {
        const val FILE_SIZE = 1024

        /** 특권 포트 1 은 어떤 서버도 듣지 않아 연결이 즉시 거절된다 */
        const val UNREACHABLE_URL = "http://127.0.0.1:1/upload"
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSourceImplTest"
```

Expected: 컴파일 실패 — `Unresolved reference: PresignedUploadDataSourceImpl`

- [ ] **Step 3: 예외 타입을 만든다**

`data/src/main/java/com/teamyg/parfait/data/model/exception/PresignedUploadException.kt`:

```kotlin
package com.teamyg.parfait.data.model.exception

/**
 * S3 가 presigned PUT 을 거절했다.
 *
 * 이 실패는 우리 서버를 거치지 않아 서버 로그에 아무것도 남지 않는다. 그래서 상태 코드를
 * 여기 실어 둔다 — 원인 추적의 유일한 단서다.
 */
class PresignedUploadException(
    val statusCode: Int,
) : Exception("presigned PUT 거절 - statusCode: $statusCode")
```

- [ ] **Step 4: 계약을 만든다**

`data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import java.io.File

interface PresignedUploadDataSource {
    /**
     * 발급받은 presigned URL 로 파일 바이트를 그대로 올린다. 우리 서버가 아니라 S3 로 나가는
     * 유일한 요청이다.
     *
     * @param contentType URL 을 발급받을 때 보낸 값과 **반드시 같아야 한다.** 서명 대상이라
     *   어긋나면 S3 가 거절하고, 그 실패는 서버 로그에 남지 않는다.
     */
    suspend fun put(
        uploadUrl: String,
        contentType: String,
        file: File,
    ): Result<Unit>
}
```

- [ ] **Step 5: 구현을 만든다**

`data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.PresignedUploadException
import com.teamyg.parfait.data.model.qualifier.UploadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class PresignedUploadDataSourceImpl @Inject constructor(
    @UploadClient private val okHttpClient: OkHttpClient,
) : PresignedUploadDataSource {
    override suspend fun put(
        uploadUrl: String,
        contentType: String,
        file: File,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // asRequestBody 는 파일을 스트리밍으로 읽는다. 바이트를 미리 배열에 담으면 원본
            // 해상도 이미지가 통째로 힙에 올라간다
            val request = Request
                .Builder()
                .url(uploadUrl)
                .put(file.asRequestBody(contentType.toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(ApiException.Unknown(PresignedUploadException(response.code)))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(ApiException.Network(e))
        } catch (e: Exception) {
            // uploadUrl·contentType 은 서버가 준 값이라 Request 조립 단계에서 예외가 날 수 있다.
            // 여기서 안 잡으면 Result 를 돌려주기로 한 계약이 깨진 채 호출부까지 올라간다
            Result.failure(ApiException.Unknown(e))
        }
    }
}
```

> `Request` 조립을 `try` **안에** 둔다. 밖에 두면 잘못된 `uploadUrl`이 그대로 예외로 새어 나간다. `ApiCaller#safeApiCallNoContent`가 같은 모양의 폴백을 이미 쓴다.

- [ ] **Step 6: DI 바인딩을 더한다**

`RemoteDataSourceModule.kt`에 import를 더한다:

```kotlin
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSource
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSourceImpl
```

인터페이스 본문에 바인딩을 더한다:

```kotlin
    @Binds
    @Singleton
    fun bindPresignedUploadDataSource(
        presignedUploadDataSourceImpl: PresignedUploadDataSourceImpl,
    ): PresignedUploadDataSource
```

- [ ] **Step 7: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
```

Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/model/exception/PresignedUploadException.kt \
        data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSource.kt \
        data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt \
        data/src/test/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImplTest.kt
git commit -m "feat(image): S3 presigned PUT 전송 경로를 추가한다

파일을 스트리밍으로 태워 원본 해상도 이미지가 힙에 통째로 올라가지 않게 한다.
S3 거절은 우리 서버를 거치지 않아 로그가 없으므로 상태 코드를 예외에 싣는다.
uploadUrl 은 서버가 주는 값이라 조립 실패도 Result 로 접는다."
```

---

### Task 3: 3단계를 하나로 닫는 ImageUploadRepository

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageUploadRepository.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt`

**Interfaces:**
- Consumes:
  - `PresignedUploadDataSource.put(uploadUrl: String, contentType: String, file: File): Result<Unit>`
  - `ImageRemoteDataSource.issueUploadUrl(fileName: String, contentType: String, imageType: ImageType): Result<ImageUploadUrlVO>`
  - `ImageRemoteDataSource.confirmUpload(imageId: ImageId): Result<ConfirmedImageVO>`
- Produces: `ImageUploadRepository.upload(filePath: String, imageType: ImageType): Result<ImageId>`

- [ ] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageStatus
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class ImageUploadRepositoryImplTest {
    private val imageRemoteDataSource: ImageRemoteDataSource = mockk()
    private val presignedUploadDataSource: PresignedUploadDataSource = mockk()
    private val repository = ImageUploadRepositoryImpl(
        imageRemoteDataSource = imageRemoteDataSource,
        presignedUploadDataSource = presignedUploadDataSource,
    )

    private lateinit var file: File

    private val issued = ImageUploadUrlVO(
        imageId = ISSUED_IMAGE_ID,
        uploadUrl = "https://s3.example.com/upload",
        imageUrl = "https://cdn.example.com/image.png",
        expiresIn = 900.seconds,
    )

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("topping", ".png")
        file.writeBytes(ByteArray(FILE_SIZE))
    }

    @AfterTest
    fun tearDown() {
        file.delete()
    }

    private fun givenAllStepsSucceed() {
        coEvery { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) } returns Result.success(issued)
        coEvery { presignedUploadDataSource.put(any(), any(), any()) } returns Result.success(Unit)
        // 확인 응답의 id 를 발급 id 와 다르게 둔다 — 같은 값이면 확인을 건너뛴 구현도 통과한다
        coEvery { imageRemoteDataSource.confirmUpload(any()) } returns Result.success(
            ConfirmedImageVO(
                imageId = CONFIRMED_IMAGE_ID,
                imageUrl = "https://cdn.example.com/image.png",
                status = ImageStatus.COMPLETED,
            ),
        )
    }

    @Test
    fun upload_allStepsSucceed_returnsConfirmedImageId() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 발급 id 가 아니라 확인까지 마친 id 가 나온다
        assertEquals(CONFIRMED_IMAGE_ID, result.getOrNull())
        coVerify(exactly = 1) { imageRemoteDataSource.confirmUpload(ISSUED_IMAGE_ID) }
    }

    @Test
    fun upload_allStepsSucceed_callsIssueThenPutThenConfirm() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 서버 계약이 정한 순서 그대로다 — 발급 전 PUT 은 서명이 없고, 전송 전 확인은 빈 객체를 굳힌다
        coVerifyOrder {
            imageRemoteDataSource.issueUploadUrl(any(), any(), any())
            presignedUploadDataSource.put(any(), any(), any())
            imageRemoteDataSource.confirmUpload(any())
        }
    }

    @Test
    fun upload_allStepsSucceed_putsToIssuedUploadUrl() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val putUrl = slot<String>()
        coEvery { presignedUploadDataSource.put(capture(putUrl), any(), any()) } returns Result.success(Unit)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 표시용 imageUrl 이 아니라 서명된 uploadUrl 로 나간다. 둘 다 String 이라 컴파일러가 안 막는다
        assertEquals(issued.uploadUrl, putUrl.captured)
    }

    @Test
    fun upload_allStepsSucceed_usesSameContentTypeForIssueAndPut() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val issuedContentType = slot<String>()
        val putContentType = slot<String>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(any(), capture(issuedContentType), any())
        } returns Result.success(issued)
        coEvery {
            presignedUploadDataSource.put(any(), capture(putContentType), any())
        } returns Result.success(Unit)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 두 값이 같다. 어긋나면 S3 가 서명 불일치로 거절하고 서버 로그에 안 남는다
        assertEquals("image/png", issuedContentType.captured)
        assertEquals(issuedContentType.captured, putContentType.captured)
    }

    @Test
    fun upload_allStepsSucceed_sendsRealFileName() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val fileName = slot<String>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(capture(fileName), any(), any())
        } returns Result.success(issued)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 더미가 아니라 실제 파일명을 보낸다
        assertEquals(file.name, fileName.captured)
    }

    @Test
    fun upload_issueFails_doesNotPutOrConfirm() = runTest {
        // Given 발급이 실패한다
        coEvery { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) } returns Result.failure(
            ApiException.Network(IOException("connection reset")),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 다음 단계로 넘어가지 않고 도메인 에러로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { presignedUploadDataSource.put(any(), any(), any()) }
        coVerify(exactly = 0) { imageRemoteDataSource.confirmUpload(any()) }
    }

    @Test
    fun upload_putFails_doesNotConfirm() = runTest {
        // Given 발급은 되고 전송이 실패한다
        coEvery { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) } returns Result.success(issued)
        coEvery { presignedUploadDataSource.put(any(), any(), any()) } returns Result.failure(
            ApiException.Network(IOException("broken pipe")),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then 확인을 부르지 않는다. 부르면 S3 에 없는 객체가 COMPLETED 로 굳는다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { imageRemoteDataSource.confirmUpload(any()) }
    }

    @Test
    fun upload_confirmFails_mapsToDomainError() = runTest {
        // Given 발급·전송은 되고 확인이 업무 에러로 실패한다
        givenAllStepsSucceed()
        coEvery { imageRemoteDataSource.confirmUpload(any()) } returns Result.failure(
            ApiException.Business(
                code = "IMAGE_ALREADY_CONFIRMED",
                serverMessage = "이미 확정된 이미지입니다",
                statusCode = 409,
                errorDetail = null,
            ),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

        // Then ApiException 이 도메인까지 새지 않는다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("IMAGE_ALREADY_CONFIRMED", error.code)
    }

    @Test
    fun upload_fileMissing_failsWithoutCallingServer() = runTest {
        // Given 초안이 가리키는 캐시 파일이 이미 지워졌다
        val missing = File(file.parentFile, "gone.png")

        // When 업로드한다
        val result = repository.upload(filePath = missing.absolutePath, imageType = ImageType.NUKKI)

        // Then 발급을 부르지 않는다 — 부르면 올릴 것도 없는데 PENDING 행과 S3 키만 남는다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
        coVerify(exactly = 0) { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) }
    }

    @Test
    fun upload_unsupportedExtension_failsWithoutCallingServer() = runTest {
        // Given 서버가 받지 않는 확장자다
        val gif = File.createTempFile("topping", ".gif")

        // When 업로드한다
        val result = repository.upload(filePath = gif.absolutePath, imageType = ImageType.NUKKI)

        // Then 서버를 부르기 전에 끊는다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
        coVerify(exactly = 0) { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) }
        gif.delete()
    }

    private companion object {
        const val FILE_SIZE = 16
        val ISSUED_IMAGE_ID = ImageId(7L)
        val CONFIRMED_IMAGE_ID = ImageId(99L)
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.repository.image.ImageUploadRepositoryImplTest"
```

Expected: 컴파일 실패 — `Unresolved reference: ImageUploadRepositoryImpl`

- [ ] **Step 3: 도메인 계약을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageUploadRepository.kt`:

```kotlin
package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType

interface ImageUploadRepository {
    /**
     * 발급·전송·확인 3단계를 하나로 닫는다. 돌려주는 [ImageId] 는 서버에서 확정까지 마친 것이라
     * 곧바로 배치에 쓸 수 있다.
     *
     * 중간에서 실패하면 그 지점의 실패가 그대로 올라오고 **되돌리지 않는다** — 서버에 정리
     * 경로가 없다(`api/image.md`). 다시 부르면 발급부터 전부 다시 탄다.
     *
     * @param filePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
     */
    suspend fun upload(
        filePath: String,
        imageType: ImageType,
    ): Result<ImageId>
}
```

- [ ] **Step 4: 구현을 만든다**

`data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.model.error.toAppError
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSource
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import java.io.File
import javax.inject.Inject

class ImageUploadRepositoryImpl @Inject constructor(
    private val imageRemoteDataSource: ImageRemoteDataSource,
    private val presignedUploadDataSource: PresignedUploadDataSource,
) : ImageUploadRepository {
    override suspend fun upload(
        filePath: String,
        imageType: ImageType,
    ): Result<ImageId> {
        val file = File(filePath)
        // 발급을 먼저 부르면 올릴 것도 없는데 PENDING 행과 S3 키만 남고, 재시도해도 영원히
        // 같은 자리에서 실패한다. 캐시 파일은 다음 흐름이 시작될 때 지워진다
        if (file.isFile.not()) {
            return Result.failure(IllegalStateException("업로드할 파일이 없다 - $filePath").toAppError())
        }
        // 발급 요청과 PUT 헤더가 같은 값을 써야 한다 — 둘 다 S3 서명 대상이고 어긋난 실패는
        // 서버 로그에 남지 않는다. 그래서 여기서 한 번만 정해 양쪽에 넘긴다
        val contentType = contentTypeOf(file) ?: return Result.failure(
            IllegalArgumentException("서버가 받지 않는 확장자다 - ${file.extension}").toAppError(),
        )

        val issued = imageRemoteDataSource
            .issueUploadUrl(fileName = file.name, contentType = contentType, imageType = imageType)
            .getOrElse { return Result.failure(it.toAppError()) }

        presignedUploadDataSource
            .put(uploadUrl = issued.uploadUrl, contentType = contentType, file = file)
            .getOrElse { return Result.failure(it.toAppError()) }

        return imageRemoteDataSource
            .confirmUpload(issued.imageId)
            .map { confirmed -> confirmed.imageId }
            .mapErrorToAppError()
    }

    private fun contentTypeOf(file: File): String? = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> null
    }
}
```

- [ ] **Step 5: DI 바인딩을 더한다**

`RepositoryModule.kt`에 import를 더한다:

```kotlin
import com.teamyg.parfait.data.repository.image.ImageUploadRepositoryImpl
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
```

인터페이스 본문에 바인딩을 더한다:

```kotlin
    @Binds
    @Singleton
    fun bindImageUploadRepository(
        imageUploadRepositoryImpl: ImageUploadRepositoryImpl,
    ): ImageUploadRepository
```

- [ ] **Step 6: 전체 검증**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck :app:assembleDebug
```

Expected: 전부 PASS.

> ⚠️ **`:app:assembleDebug`는 이 PR의 DI 안전망이 아니다.** 저장소에 `dagger.fullBindingGraphValidation` 설정이 없고 Dagger는 기본값에서 **엔트리포인트로부터 도달 가능한 바인딩만** 검증한다. 이 PR에는 소비자가 0이라 `@UploadClient` 제공자가 없어도, `@Binds`를 빠뜨려도 통과한다. 그 구멍은 Task 2의 `impl_injectsUploadQualifiedClient`가 좁힌다. `assembleDebug`가 보는 것은 KSP·Hilt 코드 생성이 깨지지 않는지까지다.

- [ ] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageUploadRepository.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt
git commit -m "feat(image): 발급·전송·확인을 하나로 닫는 ImageUploadRepository 를 추가한다

contentType 을 한 곳에서 정해 발급 요청과 PUT 헤더가 어긋날 수 없게 한다.
파일이 없거나 서버가 안 받는 확장자면 발급 전에 끊는다 — 부르면 올릴 것도
없는데 PENDING 행과 S3 키만 남는다."
```

---

## 완료 조건

- `./gradlew :domain:test :data:testDebugUnitTest ktlintCheck :app:assembleDebug` 전부 통과
- 신규 테스트 **20건**(Task 1: 3 · Task 2: 7 · Task 3: 10)
- 커밋 3개, push·PR 없음
- 기존 파일 변경은 DI 모듈 셋(`NetworkModule`·`RemoteDataSourceModule`·`RepositoryModule`)뿐이고 **기존 동작은 한 줄도 바뀌지 않는다**
- **스트리밍 전송 여부는 테스트가 아니라 리뷰가 본다.** `Content-Length` 단언은 `readBytes().toRequestBody()`로 바꿔도 그대로 통과한다 — `asRequestBody`를 쓰는지는 코드를 눈으로 확인한다.

## 이 PR에서 하지 않는 것

- 화면 결선(PR5) · 배치 Repository와 UseCase(PR2) · 토핑 초안(PR3) · 테두리 계약 전환(PR4)
- presigned URL 만료 판정 — 만료는 실패 후 전량 재시도로만 풀린다
- 고아 `PENDING` 이미지·S3 객체 정리 — 서버에 경로가 없다
- **`confirmUpload` 응답의 `status` 판정** — 서버가 `PENDING`인 것만 통과시키므로 성공 응답은 곧 `COMPLETED`다. 계약의 보장은 아니지만(`ImageStatus` KDoc) 앱이 달리 할 처분이 없다
- 실기기·실서버 확인 — 이 PR에는 소비자가 없어 손으로 밟을 화면이 없다
