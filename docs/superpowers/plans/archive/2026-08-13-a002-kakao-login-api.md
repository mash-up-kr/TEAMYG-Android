---
id: a002-kakao-login-api
title: A-002 카카오 로그인 API 결선 구현 계획
status: done
type: work-order
created: 2026-08-13
updated: 2026-08-15
platforms: android
owner: Android
related_adr: ADR-0005, ADR-0009, ADR-0017, ADR-0019, ADR-0020
related_spec: a002-kakao-login-api, mvi-error-infrastructure
related_code: KakaoLoginHelper, KakaoLoginResult, NonceGenerator, SecureRandomNonceGenerator, AuthRepository, AuthRepositoryImpl, LoginWithKakaoUseCase, LoginViewModel, LoginRoute, LoginScreen, KakaoSignInButton, KakaoLoginResponse, NavKeyTermAgree, NavKeyGroupList, TermAgreeRoute, RepositoryModule, ServerErrorCode, runSuspendCatching
archived_reason: PR #241 develop 머지 완료(2026-08-15, `80895eb1`). Step 6 실기기 검증만 이월 → OQ-P-146
tags: [plan, parfait, login, a002, auth]
---

# A-002 카카오 로그인 API 결선 Implementation Plan

> ✅ **완료 · develop 머지**(2026-08-15, PR #241 `80895eb1`). SDD 8 Task(5·6 통합 디스패치),
> fix 라운드 0. **Step 6(실기기 검증)만 미수행 상태로 머지됐다** — 아래 체크박스가 유일하게
> 비어 있는 자리이고 이월분은 [OQ-P-146](../../../synthesis/open-questions.md)이 추적한다.
> 계획 이후 추가된 것: `ServerErrorCode`(서버 코드 문자열을 도메인으로) · `KEY_KAKAO_LOGIN`을
> companion 으로 · `runSuspendCatching` 도입. `NonceGenerator` 바인딩 위치는 계획의
> `SingletonInjectModule`이 아니라 `RepositoryModule`이다(`@Binds`는 `interface` 모듈에만 된다).

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 카카오 SDK에서 `idToken`·`nonce`를 얻어 `POST /api/v1/auth/kakao`를 호출하고, `isNewUser`로 갈라 기존 회원은 세션 저장 후 그룹 목록, 신규는 약관 화면으로 보낸다.

**Architecture:** SDK 계층(`KakaoLoginHelper`)이 nonce를 만들어 SDK 호출과 결과에 함께 싣는다. `AuthRepository`가 원격 DataSource를 감싸며 `ApiException`을 `AppError`로 바꾸고 토큰을 저장한다. `LoginWithKakaoUseCase`가 "기존 회원이면 세션 저장"이라는 도메인 규칙을 갖는다. `LoginViewModel`이 분기·로딩·실패 로그를 담당한다.

**Tech Stack:** Kotlin, Kakao SDK v2-user 2.23.2(OpenID Connect), Retrofit + kotlinx-serialization, Hilt, Navigation3, JUnit4 + kotlin.test + Turbine + MockK

**Spec:** [`parfait/specs/2026-08-13-a002-kakao-login-api.md`](../../specs/archive/2026-08-13-a002-kakao-login-api.md) · 서버 계약 [api/auth.md](../../../api/auth.md)

**작업 대상 저장소:** `TJYG-Android`(별도 repo). 경로는 그 repo 루트 기준이다.

## 선행 조건

- **[mvi-error-infrastructure 계획](2026-08-13-mvi-error-infrastructure.md)이 먼저 끝나 있어야 한다.** 이 계획은 `AppError`·`BaseViewModel.launch`·`CollectAppError`를 전제한다.
- 카카오 개발자 콘솔의 **OpenID Connect가 활성**이어야 한다(2026-08-13 확인). 꺼지면 `OAuthToken.idToken`이 null이라 로그인이 성립하지 않는다.
- Task 8의 실기기 검증은 **개발 서버가 평문 HTTP면 차단**된다. 막히면 고치려 들지 말고 보고한다.

## Global Constraints

- 서버 요청 필드는 `idToken`·`nonce` 두 개다. **카카오 access token을 보내지 않는다.**
- **SDK에 넘긴 nonce와 서버에 보내는 nonce는 같은 값이어야 한다.** 서버가 ID 토큰의 `nonce` 클레임과 대조한다.
- 응답 판별자 JSON 키는 **`isNewUser`**다(`newUser` 아님).
- 실패는 **로그 + TODO만** 남긴다. 토스트·에러 문구·재시도 UI를 만들지 않는다(디자인 미확정).
- 사용자 취소는 에러가 아니다 — `d` 레벨 로그, TODO 없음.
- **커밋은 Task 단위 로컬 커밋만.** `git push`·PR 생성은 하지 않는다.
- 테스트는 Given/When/Then 한국어 주석 + `kotlin.test` 단언. ViewModel 테스트는 `runTest(mainDispatcherRule.dispatcher)`.
- 매퍼 단독 테스트(`XxxVOMapperTest`)를 만들지 않는다.
- 새 DI 모듈 파일을 만들지 않는다.
- ktlint 통과 필수: `./gradlew ktlintCheck`.

## File Structure

| 파일 | 책임 |
|---|---|
| `domain/…/util/NonceGenerator.kt` | nonce 생성 인터페이스. 신설 |
| `data/…/util/SecureRandomNonceGenerator.kt` | `SecureRandom` 구현. 신설 |
| `domain/…/model/KakaoLoginResult.kt` | SDK 결과. `Success(idToken, nonce)`로 수정 |
| `domain/…/repository/auth/AuthRepository.kt` | 인증 Repository 인터페이스. 신설 |
| `data/…/repository/auth/AuthRepositoryImpl.kt` | 원격 호출 + 에러 변환 + 토큰 저장. 신설 |
| `domain/…/usecase/auth/LoginWithKakaoUseCase.kt` | 로그인 흐름 + 세션 저장 규칙. 신설 |
| `data/…/service/model/response/auth/KakaoLoginResponse.kt` | `@SerialName` 정정. 수정 |
| `feature/login/impl/…/util/KakaoLoginHelper.kt` | nonce 전달·`idToken` 취득. 수정 |
| `feature/login/impl/…/viewmodel/LoginViewModel.kt` | 분기·로딩·실패 로그. 수정 |
| `feature/login/impl/…/route/LoginRoute.kt` | 두 목적지 배선 + 에러 수집. 수정 |
| `feature/login/impl/…/screen/LoginScreen.kt` | 로딩 중 버튼 비활성. 수정 |
| `feature/intro/api/…/NavKeyTermAgree.kt` | `data object` → `data class`. 수정 |
| `feature/intro/impl/…/EntryBuilder.kt` · `termagree/TermAgreeRoute.kt` | 토큰 전달(미사용, TODO). 수정 |

---

### Task 1: NonceGenerator

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/util/NonceGenerator.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/util/SecureRandomNonceGenerator.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/util/SecureRandomNonceGeneratorTest.kt`

**Interfaces:**
- Produces: `fun interface NonceGenerator { fun generate(): String }`, `class SecureRandomNonceGenerator @Inject constructor() : NonceGenerator`

- [x] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/util/SecureRandomNonceGeneratorTest.kt`

```kotlin
package com.teamyg.parfait.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecureRandomNonceGeneratorTest {
    private val generator = SecureRandomNonceGenerator()

    @Test
    fun generate_returnsUrlSafeBase64WithoutPadding() {
        // Given URL-safe Base64 문자 집합
        val allowed = Regex("^[A-Za-z0-9_-]+$")

        // When nonce 를 만든다
        val nonce = generator.generate()

        // Then 패딩(=) 없는 URL-safe 문자만 들어 있다
        assertTrue(allowed.matches(nonce), "URL-safe Base64 가 아니다: $nonce")
    }

    @Test
    fun generate_returns32ByteEntropy() {
        // Given 32바이트를 패딩 없이 Base64 로 인코딩하면 43자다
        // When nonce 를 만든다
        val nonce = generator.generate()

        // Then 길이가 43이다
        assertEquals(43, nonce.length)
    }

    @Test
    fun generate_calledRepeatedly_producesDistinctValues() {
        // Given 반복 호출
        // When 100번 만든다
        val nonces = List(100) { generator.generate() }

        // Then 전부 다르다(재생 공격 방어의 전제)
        assertEquals(100, nonces.toSet().size)
    }
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*SecureRandomNonceGeneratorTest*"`
Expected: FAIL — `Unresolved reference: SecureRandomNonceGenerator`

- [x] **Step 3: 인터페이스와 구현 작성**

`domain/src/main/java/com/teamyg/parfait/domain/util/NonceGenerator.kt`

```kotlin
package com.teamyg.parfait.domain.util

/**
 * 로그인 1회분 nonce 를 만든다.
 *
 * 카카오 SDK 요청과 서버 로그인 요청에 **같은 값**을 보내야 한다 — 서버가 ID 토큰의
 * `nonce` 클레임과 대조해 재생 공격을 막는다.
 *
 * 인터페이스로 두는 이유는 테스트에서 값을 고정하기 위해서다.
 */
fun interface NonceGenerator {
    fun generate(): String
}
```

`data/src/main/java/com/teamyg/parfait/data/util/SecureRandomNonceGenerator.kt`

```kotlin
package com.teamyg.parfait.data.util

import com.teamyg.parfait.domain.util.NonceGenerator
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

private const val NONCE_BYTE_SIZE = 32

/** [SecureRandom] 32바이트를 패딩 없는 URL-safe Base64(43자)로 인코딩한다 */
class SecureRandomNonceGenerator @Inject constructor() : NonceGenerator {
    private val secureRandom = SecureRandom()

    override fun generate(): String {
        val bytes = ByteArray(NONCE_BYTE_SIZE).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
```

- [x] **Step 4: DI 바인딩 추가**

`@Binds`는 `interface`/`abstract class` 모듈에만 쓸 수 있다. `SingletonInjectModule`은 `object`(=`@Provides` 전용)이므로 **이미 `interface`인 `RepositoryModule`에 넣는다** — 새 모듈 파일을 만들지 않는다는 규약을 지키면서 `@Binds`가 가능한 자리다.

`data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`에 추가:

```kotlin
    @Binds
    @Singleton
    fun bindNonceGenerator(secureRandomNonceGenerator: SecureRandomNonceGenerator): NonceGenerator
```

필요한 import:

```kotlin
import com.teamyg.parfait.data.util.SecureRandomNonceGenerator
import com.teamyg.parfait.domain.util.NonceGenerator
```

- [x] **Step 5: 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*SecureRandomNonceGeneratorTest*"`
Expected: PASS (3 tests)

- [x] **Step 6: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/util/NonceGenerator.kt \
        data/src/main/java/com/teamyg/parfait/data/util/SecureRandomNonceGenerator.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt \
        data/src/test/java/com/teamyg/parfait/data/util/SecureRandomNonceGeneratorTest.kt
git commit -m "feat(data): 로그인 nonce 생성기 추가"
```

---

### Task 2: KakaoLoginResponse 직렬화 키 정정

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/auth/KakaoLoginResponse.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/service/model/response/auth/KakaoLoginResponseSerializationTest.kt`

**Interfaces:**
- Produces: `KakaoLoginResponse(isNewUser, accessToken, refreshToken, expiresIn, registrationToken)` — 프로퍼티명 불변, JSON 키만 `isNewUser`로 정정

> 이 테스트는 **매퍼 테스트가 아니라 와이어 계약 테스트**다. DataSource 테스트는 서비스를
> MockK로 대체해 JSON을 지나지 않으므로 `@SerialName` 오류를 절대 못 잡는다. 실제 문자열을
> 디코딩하는 이 테스트만이 잡는다.

- [x] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/service/model/response/auth/KakaoLoginResponseSerializationTest.kt`

```kotlin
package com.teamyg.parfait.data.service.model.response.auth

import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 서버가 내려주는 판별자 키는 `isNewUser` 다(`newUser` 아님). Jackson 코틀린 모듈이
 * 붙은 서버는 주 생성자 파라미터명으로 직렬화해 `is` 접두사가 살아남는다.
 */
class KakaoLoginResponseSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_newUserResponse_readsIsNewUserKeyAndRegistrationToken() {
        // Given 서버 신규 회원 응답 본문
        val body = """
            {
              "success": true,
              "code": "OK",
              "message": "성공",
              "data": {
                "isNewUser": true,
                "accessToken": null,
                "refreshToken": null,
                "expiresIn": null,
                "registrationToken": "reg-token-1"
              }
            }
        """.trimIndent()

        // When 디코딩
        val response = json.decodeFromString<ApiResponse<KakaoLoginResponse>>(body).data

        // Then 판별자가 true 이고 가입 토큰이 실린다
        assertTrue(response!!.isNewUser)
        assertEquals("reg-token-1", response.registrationToken)
        assertNull(response.accessToken)
    }

    @Test
    fun decode_existingMemberResponse_readsSessionFields() {
        // Given 서버 기존 회원 응답 본문
        val body = """
            {
              "success": true,
              "code": "OK",
              "message": "성공",
              "data": {
                "isNewUser": false,
                "accessToken": "access-1",
                "refreshToken": "refresh-1",
                "expiresIn": 3600,
                "registrationToken": null
              }
            }
        """.trimIndent()

        // When 디코딩
        val response = json.decodeFromString<ApiResponse<KakaoLoginResponse>>(body).data

        // Then 세션 3종이 실리고 가입 토큰은 없다
        assertEquals(false, response!!.isNewUser)
        assertEquals("access-1", response.accessToken)
        assertEquals("refresh-1", response.refreshToken)
        assertEquals(3600L, response.expiresIn)
        assertNull(response.registrationToken)
    }
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*KakaoLoginResponseSerializationTest*"`
Expected: FAIL — `MissingFieldException: Field 'newUser' is required`. **이것이 실서버에서 로그인이 통째로 실패하는 바로 그 예외다.**

- [x] **Step 3: 키 정정**

`KakaoLoginResponse.kt`의 첫 `@SerialName`만 바꾼다.

```kotlin
@Serializable
data class KakaoLoginResponse(
    @SerialName("isNewUser")
    val isNewUser: Boolean,
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Long? = null,
    @SerialName("registrationToken")
    val registrationToken: String? = null,
)
```

- [x] **Step 4: 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*KakaoLoginResponseSerializationTest*"`
Expected: PASS (2 tests)

- [x] **Step 5: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/service/model/response/auth/KakaoLoginResponse.kt \
        data/src/test/java/com/teamyg/parfait/data/service/model/response/auth/KakaoLoginResponseSerializationTest.kt
git commit -m "fix(data): 카카오 로그인 응답 판별자 키를 isNewUser 로 정정"
```

---

### Task 3: AuthRepository

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/auth/AuthRepository.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImplTest.kt`

**Interfaces:**
- Consumes: 기존 `AuthRemoteDataSource#loginWithKakao(idToken, nonce): Result<KakaoLoginVO>`, 기존 `TokenStore#save(accessToken, refreshToken)`, 인프라 계획의 `Result<T>.mapErrorToAppError()`
- Produces: `AuthRepository#loginWithKakao(idToken: String, nonce: String): Result<KakaoLoginVO>`, `AuthRepository#saveSession(session: AuthSessionVO)`

- [x] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImplTest.kt`

```kotlin
package com.teamyg.parfait.data.repository.auth

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSource
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class AuthRepositoryImplTest {
    private val remoteDataSource: AuthRemoteDataSource = mockk()
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val repository = AuthRepositoryImpl(
        authRemoteDataSource = remoteDataSource,
        tokenStore = tokenStore,
    )

    @Test
    fun loginWithKakao_remoteSucceeds_returnsVoUnchanged() = runTest {
        // Given 원격이 신규 회원으로 응답
        val vo = KakaoLoginVO.NewUser(RegistrationToken("reg-1"))
        coEvery { remoteDataSource.loginWithKakao("id-1", "nonce-1") } returns Result.success(vo)

        // When 로그인
        val result = repository.loginWithKakao(idToken = "id-1", nonce = "nonce-1")

        // Then VO 가 그대로 나온다
        assertEquals(vo, result.getOrNull())
    }

    @Test
    fun loginWithKakao_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 원격이 401 INVALID_ID_TOKEN 으로 실패
        coEvery { remoteDataSource.loginWithKakao(any(), any()) } returns Result.failure(
            ApiException.Business(
                code = "INVALID_ID_TOKEN",
                serverMessage = "유효하지 않은 ID 토큰입니다",
                statusCode = 401,
                errorDetail = null,
            ),
        )

        // When 로그인
        val result = repository.loginWithKakao(idToken = "id-1", nonce = "nonce-1")

        // Then 도메인 에러로 바뀌어 나온다(호출부가 :data 를 보지 않아도 된다)
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("INVALID_ID_TOKEN", error.code)
        assertEquals(401, error.statusCode)
    }

    @Test
    fun loginWithKakao_remoteFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { remoteDataSource.loginWithKakao(any(), any()) } returns
            Result.failure(ApiException.Network(IOException("offline")))

        // When 로그인
        val result = repository.loginWithKakao(idToken = "id-1", nonce = "nonce-1")

        // Then Network 갈래다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun saveSession_delegatesRawTokenValuesToTokenStore() = runTest {
        // Given 세션 VO
        val session = AuthSessionVO(
            accessToken = AccessToken("access-1"),
            refreshToken = RefreshToken("refresh-1"),
            expiresIn = 3600.seconds,
        )

        // When 저장
        repository.saveSession(session)

        // Then value class 를 벗겨 원시 문자열로 저장한다
        coVerify(exactly = 1) {
            tokenStore.save(accessToken = "access-1", refreshToken = "refresh-1")
        }
    }
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*AuthRepositoryImplTest*"`
Expected: FAIL — `Unresolved reference: AuthRepositoryImpl`

- [x] **Step 3: 인터페이스 작성**

`domain/src/main/java/com/teamyg/parfait/domain/repository/auth/AuthRepository.kt`

```kotlin
package com.teamyg.parfait.domain.repository.auth

import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO

interface AuthRepository {
    /**
     * @param idToken 카카오 SDK 가 발급한 **ID 토큰**(access token 이 아니다)
     * @param nonce SDK 요청에 넘긴 것과 **같은 값**
     */
    suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO>

    /** 발급받은 세션을 암호화 저장소에 넣는다 */
    suspend fun saveSession(session: AuthSessionVO)
}
```

- [x] **Step 4: 구현 작성**

`data/src/main/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImpl.kt`

```kotlin
package com.teamyg.parfait.data.repository.auth

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSource
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 원격 인증 호출과 토큰 저장을 묶는다.
 *
 * 실패 원인을 여기서 [com.teamyg.parfait.domain.model.error.AppError] 로 바꾼다 —
 * 이 경계가 있어야 feature 모듈이 `:data` 의 `ApiException` 을 보지 않는다.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val tokenStore: TokenStore,
) : AuthRepository {
    override suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO> = authRemoteDataSource
        .loginWithKakao(idToken = idToken, nonce = nonce)
        .mapErrorToAppError()

    override suspend fun saveSession(session: AuthSessionVO) {
        tokenStore.save(
            accessToken = session.accessToken.value,
            refreshToken = session.refreshToken.value,
        )
    }
}
```

- [x] **Step 5: DI 바인딩 추가**

`RepositoryModule.kt`에 추가:

```kotlin
    @Binds
    @Singleton
    fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository
```

필요한 import:

```kotlin
import com.teamyg.parfait.data.repository.auth.AuthRepositoryImpl
import com.teamyg.parfait.domain.repository.auth.AuthRepository
```

- [x] **Step 6: 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*AuthRepositoryImplTest*"`
Expected: PASS (4 tests)

- [x] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/auth/AuthRepository.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImplTest.kt
git commit -m "feat(data): AuthRepository 추가 — 카카오 로그인 호출과 세션 저장"
```

---

### Task 4: LoginWithKakaoUseCase

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 3의 `AuthRepository`
- Produces: `class LoginWithKakaoUseCase @Inject constructor(authRepository: AuthRepository)` with `suspend operator fun invoke(idToken: String, nonce: String): Result<KakaoLoginVO>`

- [x] **Step 1: 실패 테스트 작성**

`domain/src/test/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCaseTest.kt`

```kotlin
package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class LoginWithKakaoUseCaseTest {
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val useCase = LoginWithKakaoUseCase(authRepository)

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-1"),
        refreshToken = RefreshToken("refresh-1"),
        expiresIn = 3600.seconds,
    )

    @Test
    fun invoke_existingMember_savesSession() = runTest {
        // Given 기존 회원 응답
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))

        // When 로그인
        useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 세션이 저장된다 — 화면이 잊을 수 없도록 여기서 한다
        coVerify(exactly = 1) { authRepository.saveSession(session) }
    }

    @Test
    fun invoke_newUser_doesNotSaveSession() = runTest {
        // Given 신규 회원 응답(세션이 아직 없다)
        coEvery { authRepository.loginWithKakao(any(), any()) } returns
            Result.success(KakaoLoginVO.NewUser(RegistrationToken("reg-1")))

        // When 로그인
        useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 저장 호출이 없다
        coVerify(exactly = 0) { authRepository.saveSession(any()) }
    }

    @Test
    fun invoke_failure_propagatesErrorAndSkipsSave() = runTest {
        // Given 서버가 401 로 실패
        coEvery { authRepository.loginWithKakao(any(), any()) } returns Result.failure(
            AppError.Server(code = "INVALID_ID_TOKEN", statusCode = 401, serverMessage = "…"),
        )

        // When 로그인
        val result = useCase(idToken = "id-1", nonce = "nonce-1")

        // Then 실패가 그대로 전달되고 저장하지 않는다
        assertIs<AppError.Server>(result.exceptionOrNull())
        coVerify(exactly = 0) { authRepository.saveSession(any()) }
    }
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :domain:test --tests "*LoginWithKakaoUseCaseTest*"`
Expected: FAIL — `Unresolved reference: LoginWithKakaoUseCase`

- [x] **Step 3: UseCase 작성**

`domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCase.kt`

```kotlin
package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 카카오 ID 토큰으로 서버 로그인을 하고, **기존 회원이면 세션을 저장한다.**
 *
 * 저장을 화면이 아니라 여기서 하는 이유: 로그인 진입점이 늘어날 때마다 잊을 수 있고,
 * 저장 전에 내비게이션이 나가면 다음 화면의 첫 API 호출이 토큰 없이 나간다.
 */
class LoginWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO> = authRepository
        .loginWithKakao(idToken = idToken, nonce = nonce)
        .onSuccess { result ->
            if (result is KakaoLoginVO.ExistingMember) {
                authRepository.saveSession(result.session)
            }
        }
}
```

- [x] **Step 4: 통과 확인**

Run: `./gradlew :domain:test --tests "*LoginWithKakaoUseCaseTest*"`
Expected: PASS (3 tests)

- [x] **Step 5: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCase.kt \
        domain/src/test/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCaseTest.kt
git commit -m "feat(domain): LoginWithKakaoUseCase 추가 — 기존 회원 세션 저장 규칙 포함"
```

---

### Task 5: KakaoLoginHelper — nonce 전달과 idToken 취득

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/KakaoLoginResult.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/auth/KakaoLoginVO.kt`
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelper.kt`

**Interfaces:**
- Consumes: Task 1의 `NonceGenerator`
- Produces: `KakaoLoginResult.Success(idToken: String, nonce: String)`, `KakaoLoginHelper#login(activity: Activity): KakaoLoginResult`(시그니처 불변)

> 이 Task는 자동 테스트가 없다. `Activity`와 카카오 SDK 정적 클라이언트에 묶여 있어 JVM
> 유닛 테스트로 덮을 수 없다. 검증은 컴파일 + Task 8의 실기기 확인이다.

- [x] **Step 1: KakaoLoginResult 교체**

`domain/src/main/java/com/teamyg/parfait/domain/model/KakaoLoginResult.kt`

```kotlin
package com.teamyg.parfait.domain.model

/**
 * 카카오 **SDK** 로그인 결과.
 *
 * 서버 로그인 응답은 [com.teamyg.parfait.domain.model.auth.KakaoLoginVO] 다 — 이름이 닮았지만
 * 다른 것이다. 이쪽은 SDK 가 준 ID 토큰, 저쪽은 우리 서버가 준 세션/가입 토큰이다.
 */
sealed interface KakaoLoginResult {
    /**
     * @param idToken 서버 `POST /api/v1/auth/kakao` 에 보낼 ID 토큰
     * @param nonce SDK 요청에 넘긴 값. **서버 요청에도 같은 값을 보내야 한다**
     */
    data class Success(
        val idToken: String,
        val nonce: String,
    ) : KakaoLoginResult

    data class Cancel(val throwable: Throwable?) : KakaoLoginResult

    data class Failure(val throwable: Throwable?) : KakaoLoginResult
}
```

- [x] **Step 2: KakaoLoginVO 에 상호 참조 KDoc 추가**

`domain/src/main/java/com/teamyg/parfait/domain/model/auth/KakaoLoginVO.kt`

```kotlin
package com.teamyg.parfait.domain.model.auth

/**
 * 서버 `POST /api/v1/auth/kakao` 응답.
 *
 * 카카오 **SDK** 로그인 결과는 [com.teamyg.parfait.domain.model.KakaoLoginResult] 다 —
 * 이름이 닮았지만 다른 것이다.
 */
sealed interface KakaoLoginVO {
    data class ExistingMember(val session: AuthSessionVO) : KakaoLoginVO

    data class NewUser(val registrationToken: RegistrationToken) : KakaoLoginVO
}
```

- [x] **Step 3: KakaoLoginHelper 교체**

`feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelper.kt`

```kotlin
package com.teamyg.parfait.feature.login.impl.util

import android.app.Activity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.util.NonceGenerator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class KakaoLoginHelper
@Inject
constructor(
    private val userApiClient: UserApiClient,
    private val nonceGenerator: NonceGenerator,
) {
    /**
     * 로그인 1회분 nonce 를 만들어 SDK 요청에 넘기고, 성공 결과에 같은 값을 실어 돌려준다.
     * 카카오톡 로그인이 실패해 계정 로그인으로 넘어가도 **nonce 는 그대로 재사용**한다 —
     * 최종 성공한 로그인이 그 nonce 로 발급받은 ID 토큰을 주므로 서버 대조가 맞는다.
     */
    suspend fun login(activity: Activity): KakaoLoginResult {
        val nonce = nonceGenerator.generate()

        return if (isKakaoTalkLoginAvailable(activity)) {
            when (val result = loginWithKakaoTalk(activity, nonce)) {
                is KakaoLoginResult.Success -> result
                is KakaoLoginResult.Cancel -> result
                is KakaoLoginResult.Failure -> loginWithKakaoAccount(activity, nonce)
            }
        } else {
            loginWithKakaoAccount(activity, nonce)
        }
    }

    private fun isKakaoTalkLoginAvailable(activity: Activity): Boolean =
        userApiClient.isKakaoTalkLoginAvailable(activity)

    private suspend fun loginWithKakaoTalk(
        activity: Activity,
        nonce: String,
    ): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        userApiClient.loginWithKakaoTalk(
            context = activity,
            nonce = nonce,
            callback = { token, error -> continuation.resumeWithLoginResult(nonce, token, error) },
        )
    }

    private suspend fun loginWithKakaoAccount(
        activity: Activity,
        nonce: String,
    ): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        userApiClient.loginWithKakaoAccount(
            context = activity,
            nonce = nonce,
            callback = { token, error -> continuation.resumeWithLoginResult(nonce, token, error) },
        )
    }

    private fun CancellableContinuation<KakaoLoginResult>.resumeWithLoginResult(
        nonce: String,
        token: OAuthToken?,
        error: Throwable?,
    ) {
        val result = when {
            token != null -> token.toLoginResult(nonce)
            error == null -> KakaoLoginResult.Failure(IllegalStateException("token 과 error 가 모두 null 이다"))
            error is ClientError && error.reason == ClientErrorCause.Cancelled -> KakaoLoginResult.Cancel(error)
            else -> KakaoLoginResult.Failure(error)
        }
        resume(value = result, onCancellation = null)
    }

    /**
     * `idToken` 은 nullable 이다 — 카카오 개발자 콘솔에서 **OpenID Connect 가 꺼져 있으면
     * null** 이다. 서버는 이 값을 요구하므로 없으면 로그인 자체가 성립하지 않는다.
     */
    private fun OAuthToken.toLoginResult(nonce: String): KakaoLoginResult {
        val idToken = idToken
            ?: return KakaoLoginResult.Failure(
                IllegalStateException("idToken 이 null 이다 — 카카오 콘솔 OpenID Connect 활성화를 확인한다"),
            )
        return KakaoLoginResult.Success(idToken = idToken, nonce = nonce)
    }
}
```

- [x] **Step 4: 컴파일 확인**

Run: `./gradlew :feature:login:impl:compileDebugKotlin`
Expected: FAIL — `LoginViewModel`/`LoginRoute`가 아직 `KakaoLoginResult.Success(token)`을 쓰고 있어 깨진다. **이 실패는 예상된 것이며 Task 6·8에서 닫힌다.** 오류가 그 두 파일에서만 나는지 확인한다.

- [x] **Step 5: 커밋하지 않는다**

Task 5는 단독으로 컴파일되지 않는다. Task 6과 함께 커밋한다.

---

### Task 6: LoginViewModel 분기·로딩·실패 로그

**Files:**
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModel.kt`
- Modify: `feature/login/impl/build.gradle.kts`
- Test: `feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: Task 4의 `LoginWithKakaoUseCase`, Task 5의 `KakaoLoginResult`, 인프라 계획의 `BaseViewModel.launch`
- Produces: `LoginState(isLoading)`, `LoginIntent.LoginWithKakaoSuccess(idToken, nonce)`, `LoginSideEffect.NavigateToTermAgree(registrationToken)`·`NavigateToGroupList`·`RequestLoginWithKakao`

- [x] **Step 1: 모듈에 테스트 플러그인 추가**

`feature/login/impl/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.login.impl"
}

dependencies {
    implementation(projects.feature.login.api)
    implementation(projects.feature.intro.api)
    implementation(projects.feature.groups.list.api)
    implementation(libs.kakao.sdk.user)
}
```

- [x] **Step 2: 실패 테스트 작성**

`feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModelTest.kt`

```kotlin
package com.teamyg.parfait.feature.login.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.usecase.auth.LoginWithKakaoUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginWithKakaoUseCase: LoginWithKakaoUseCase = mockk()

    private fun viewModel() = LoginViewModel(loginWithKakaoUseCase)

    private val session = AuthSessionVO(
        accessToken = AccessToken("access-1"),
        refreshToken = RefreshToken("refresh-1"),
        expiresIn = 3600.seconds,
    )

    @Test
    fun loginWithKakao_firstClick_requestsSdkLoginAndTurnsOnLoading() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 초기 상태
            val viewModel = viewModel()

            viewModel.effect.test {
                // When 카카오 버튼을 누른다
                viewModel.processIntent(LoginIntent.LoginWithKakao)
                runCurrent()

                // Then SDK 로그인 요청이 나가고 로딩이 켜진다
                assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())
                assertTrue(viewModel.state.value.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun loginWithKakao_clickedWhileLoading_doesNotRequestSdkLoginAgain() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 이미 로그인이 진행 중
            val viewModel = viewModel()

            viewModel.effect.test {
                viewModel.processIntent(LoginIntent.LoginWithKakao)
                runCurrent()
                assertEquals(LoginSideEffect.RequestLoginWithKakao, awaitItem())

                // When 한 번 더 누른다(연타)
                viewModel.processIntent(LoginIntent.LoginWithKakao)
                runCurrent()

                // Then 두 번째 요청은 나가지 않는다 — 카카오 창이 두 번 뜨면 안 된다
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun loginSuccess_existingMember_navigatesToGroupList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 기존 회원으로 응답
        coEvery { loginWithKakaoUseCase(any(), any()) } returns
            Result.success(KakaoLoginVO.ExistingMember(session))
        val viewModel = viewModel()

        viewModel.effect.test {
            // When SDK 성공 결과를 전달
            viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
            advanceUntilIdle()

            // Then 그룹 목록으로 간다
            assertEquals(LoginSideEffect.NavigateToGroupList, awaitItem())
            assertFalse(viewModel.state.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loginSuccess_newUser_navigatesToTermAgreeWithRegistrationToken() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 서버가 신규 회원으로 응답
            coEvery { loginWithKakaoUseCase(any(), any()) } returns
                Result.success(KakaoLoginVO.NewUser(RegistrationToken("reg-1")))
            val viewModel = viewModel()

            viewModel.effect.test {
                // When SDK 성공 결과를 전달
                viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
                advanceUntilIdle()

                // Then 가입 토큰을 들고 약관 화면으로 간다
                assertEquals(LoginSideEffect.NavigateToTermAgree("reg-1"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun loginFailure_serverError_turnsOffLoadingAndDoesNotNavigate() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 서버가 401 로 실패
            coEvery { loginWithKakaoUseCase(any(), any()) } returns Result.failure(
                AppError.Server(code = "INVALID_ID_TOKEN", statusCode = 401, serverMessage = "…"),
            )
            val viewModel = viewModel()

            viewModel.effect.test {
                // When SDK 성공 결과를 전달
                viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
                advanceUntilIdle()

                // Then 내비게이션 없이 로딩만 풀린다(에러 UX 는 아직 없다)
                expectNoEvents()
                assertFalse(viewModel.state.value.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun loginFailure_networkError_turnsOffLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 네트워크 단절
        coEvery { loginWithKakaoUseCase(any(), any()) } returns
            Result.failure(AppError.Network(null))
        val viewModel = viewModel()

        // When SDK 성공 결과를 전달
        viewModel.processIntent(LoginIntent.LoginWithKakaoSuccess(idToken = "id-1", nonce = "nonce-1"))
        advanceUntilIdle()

        // Then 로딩이 풀려 재시도할 수 있다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun sdkCancel_turnsOffLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그인 진행 중
        val viewModel = viewModel()
        viewModel.processIntent(LoginIntent.LoginWithKakao)
        runCurrent()

        // When 사용자가 카카오 화면에서 취소
        viewModel.processIntent(LoginIntent.LoginWithKakaoCancel)
        runCurrent()

        // Then 로딩이 풀린다(취소는 에러가 아니다)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun sdkFailure_turnsOffLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그인 진행 중
        val viewModel = viewModel()
        viewModel.processIntent(LoginIntent.LoginWithKakao)
        runCurrent()

        // When SDK 가 실패를 돌려준다(idToken null 포함)
        viewModel.processIntent(LoginIntent.LoginWithKakaoFailure(IllegalStateException("idToken null")))
        runCurrent()

        // Then 로딩이 풀린다
        assertFalse(viewModel.state.value.isLoading)
    }
}
```

- [x] **Step 3: 실패 확인**

Run: `./gradlew :feature:login:impl:testDebugUnitTest --tests "*LoginViewModelTest*"`
Expected: FAIL — `LoginViewModel` 생성자 인자 없음 / `NavigateToGroupList` 미정의

- [x] **Step 4: LoginViewModel 교체**

`feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModel.kt`

```kotlin
package com.teamyg.parfait.feature.login.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.usecase.auth.LoginWithKakaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val KEY_KAKAO_LOGIN = "kakaoLogin"

private const val CODE_INVALID_ID_TOKEN = "INVALID_ID_TOKEN"
private const val CODE_KAKAO_JWKS_FETCH_FAILED = "KAKAO_JWKS_FETCH_FAILED"
private const val CODE_KAKAO_SERVER_UNAVAILABLE = "KAKAO_SERVER_UNAVAILABLE"

data class LoginState(
    val isLoading: Boolean = false,
) : UiState

sealed interface LoginIntent : UiIntent {
    data object LoginWithKakao : LoginIntent

    /**
     * @param idToken 카카오 SDK 가 준 ID 토큰
     * @param nonce SDK 요청에 쓴 값. 서버에도 같은 값을 보낸다
     */
    data class LoginWithKakaoSuccess(
        val idToken: String,
        val nonce: String,
    ) : LoginIntent

    data class LoginWithKakaoFailure(val throwable: Throwable?) : LoginIntent

    data object LoginWithKakaoCancel : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    data object RequestLoginWithKakao : LoginSideEffect

    /** 신규 회원 — 약관 동의로 보낸다. 뒤로가기가 로그인으로 와야 하므로 백스택을 지우지 않는다 */
    data class NavigateToTermAgree(val registrationToken: String) : LoginSideEffect

    /** 기존 회원 — 세션 저장이 끝났다. 백스택을 지우고 그룹 목록으로 간다 */
    data object NavigateToGroupList : LoginSideEffect
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    init {
        viewModelLogger.i { "LoginViewModel::init" }
    }

    override fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.LoginWithKakao -> requestSdkLogin()

            is LoginIntent.LoginWithKakaoSuccess -> requestServerLogin(
                idToken = intent.idToken,
                nonce = intent.nonce,
            )

            is LoginIntent.LoginWithKakaoFailure -> {
                updateState { copy(isLoading = false) }
                // TODO(에러 UX 미정): 실패 안내 노출. idToken 이 null 이면 콘솔 OIDC 설정 문제다
                viewModelLogger.e(intent.throwable) { "카카오 SDK 로그인 실패" }
            }

            is LoginIntent.LoginWithKakaoCancel -> {
                updateState { copy(isLoading = false) }
                viewModelLogger.d { "사용자가 카카오 로그인을 취소했다" }
            }
        }
    }

    /**
     * SDK 다이얼로그는 [launch] 가드보다 앞에서 뜨므로 로딩 플래그로 한 겹 더 막는다.
     * 안 그러면 연타에 카카오 로그인 창이 두 번 뜬다.
     */
    private fun requestSdkLogin() {
        if (state.value.isLoading) {
            viewModelLogger.d { "로그인 진행 중이라 중복 요청을 무시한다" }
            return
        }
        updateState { copy(isLoading = true) }
        postSideEffect(LoginSideEffect.RequestLoginWithKakao)
    }

    private fun requestServerLogin(
        idToken: String,
        nonce: String,
    ) {
        launch(key = KEY_KAKAO_LOGIN) {
            loginWithKakaoUseCase(idToken = idToken, nonce = nonce)
                .onSuccess(::navigateByMemberType)
                .onFailure(::logServerLoginFailure)

            updateState { copy(isLoading = false) }
        }
    }

    private fun navigateByMemberType(result: KakaoLoginVO) {
        when (result) {
            is KakaoLoginVO.ExistingMember -> postSideEffect(LoginSideEffect.NavigateToGroupList)

            is KakaoLoginVO.NewUser -> postSideEffect(
                LoginSideEffect.NavigateToTermAgree(registrationToken = result.registrationToken.value),
            )
        }
    }

    /**
     * 실패 갈래를 전부 열거해 둔다. 지금은 로그뿐이지만, UX 가 정해지면 각 자리를 문구로
     * 바꾸면 되고 분기를 다시 발굴할 필요가 없다.
     */
    private fun logServerLoginFailure(throwable: Throwable) {
        when (throwable) {
            is AppError.Network ->
                // TODO(에러 UX 미정): "네트워크 연결을 확인해 주세요" + 재시도 안내
                viewModelLogger.e(throwable) { "로그인 실패 — 네트워크 단절" }

            is AppError.Server -> when (throwable.code) {
                CODE_INVALID_ID_TOKEN ->
                    // TODO(에러 UX 미정): 다시 로그인 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — ID 토큰 검증 실패(401)" }

                CODE_KAKAO_JWKS_FETCH_FAILED ->
                    // TODO(에러 UX 미정): 잠시 후 재시도 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — 카카오 공개키 조회 실패(502)" }

                CODE_KAKAO_SERVER_UNAVAILABLE ->
                    // TODO(에러 UX 미정): 잠시 후 재시도 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — 카카오 서버 연결 불가(503)" }

                else ->
                    // TODO(에러 UX 미정): 알 수 없는 서버 에러 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — 미분류 서버 에러 ${throwable.code}" }
            }

            else ->
                // TODO(에러 UX 미정): 알 수 없는 오류 안내. 매퍼 실패·파싱 실패가 여기로 온다
                viewModelLogger.e(throwable) { "로그인 실패 — 예상하지 못한 오류" }
        }
    }
}
```

- [x] **Step 5: 통과 확인**

Run: `./gradlew :feature:login:impl:testDebugUnitTest --tests "*LoginViewModelTest*"`
Expected: PASS (8 tests). `LoginRoute`는 아직 깨져 있으므로 `compileDebugKotlin`은 실패할 수 있다 — Task 8에서 닫는다.

- [x] **Step 6: 커밋(Task 5 포함)**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/KakaoLoginResult.kt \
        domain/src/main/java/com/teamyg/parfait/domain/model/auth/KakaoLoginVO.kt \
        feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelper.kt \
        feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModel.kt \
        feature/login/impl/build.gradle.kts \
        feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModelTest.kt
git commit -m "feat(login): 카카오 SDK idToken·nonce 취득과 신규/기존 회원 분기"
```

---

### Task 7: NavKeyTermAgree 에 registrationToken 싣기

**Files:**
- Modify: `feature/intro/api/src/main/java/com/teamyg/parfait/feature/intro/api/NavKeyTermAgree.kt`
- Modify: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/EntryBuilder.kt`
- Modify: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/termagree/TermAgreeRoute.kt`

**Interfaces:**
- Produces: `NavKeyTermAgree(registrationToken: String)`, `TermAgreeRoute(navigator, registrationToken, modifier, viewModel)`

- [x] **Step 1: NavKey 를 data class 로**

`feature/intro/api/src/main/java/com/teamyg/parfait/feature/intro/api/NavKeyTermAgree.kt`

```kotlin
package com.teamyg.parfait.feature.intro.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param registrationToken 카카오 로그인이 신규 회원으로 판정하며 내려준 가입 토큰.
 *   약관 동의 후 `POST /api/v1/auth/signup` 에 넘긴다.
 */
@Serializable
data class NavKeyTermAgree(val registrationToken: String) : NavKey
```

- [x] **Step 2: EntryBuilder 에서 값 꺼내 전달**

`feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/EntryBuilder.kt`의 `featureTermAgreeEntryBuilder`만 바꾼다.

```kotlin
fun EntryProviderScope<NavKey>.featureTermAgreeEntryBuilder(navigator: Navigator) {
    entry<NavKeyTermAgree> { navKey ->
        YGScaffold { innerPadding ->
            TermAgreeRoute(
                navigator = navigator,
                registrationToken = navKey.registrationToken,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = YGTheme.colorScheme.grayScale.white)
                    .padding(innerPadding),
            )
        }
    }
}
```

- [x] **Step 3: TermAgreeRoute 가 값을 받는다**

`TermAgreeRoute.kt`의 시그니처만 바꾼다. **이번 라운드에서는 쓰지 않는다.**

```kotlin
@Composable
fun TermAgreeRoute(
    navigator: Navigator,
    registrationToken: String,
    modifier: Modifier = Modifier,
    viewModel: TermAgreeViewModel = hiltViewModel(),
) {
    // TODO(signup 라운드): 이 토큰으로 POST /api/v1/auth/signup 을 호출한다.
    //  ViewModel 주입은 GroupCreateViewModel 과 같은 assisted Factory 패턴으로 붙인다.
```

본문(나머지)은 그대로 둔다.

- [x] **Step 4: 컴파일 확인**

Run: `./gradlew :feature:intro:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [x] **Step 5: 커밋**

```bash
git add feature/intro/api/src/main/java/com/teamyg/parfait/feature/intro/api/NavKeyTermAgree.kt \
        feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/EntryBuilder.kt \
        feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/termagree/TermAgreeRoute.kt
git commit -m "feat(intro): 약관 화면 NavKey 에 registrationToken 전달"
```

---

### Task 8: LoginRoute·LoginScreen 배선과 통합 검증

**Files:**
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/route/LoginRoute.kt`
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/screen/LoginScreen.kt`

**Interfaces:**
- Consumes: Task 6의 `LoginSideEffect` 3종·`LoginState.isLoading`, Task 7의 `NavKeyTermAgree(registrationToken)`, 인프라 계획의 `CollectAppError`
- Produces: 없음(최종 배선)

- [x] **Step 1: LoginScreen 이 로딩을 받는다**

`LoginScreen.kt` — 시그니처에 `isLoading`을 더하고 버튼에 넘긴다.

```kotlin
@Composable
internal fun LoginScreen(
    pages: List<OnboardingPage>,
    isLoading: Boolean,
    onClickKakaoButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

`KakaoSignInButton` 호출에 한 줄 추가:

```kotlin
        KakaoSignInButton(
            onClick = onClickKakaoButton,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        )
```

프리뷰도 인자를 채운다:

```kotlin
    LoginScreen(
        pages = pages,
        isLoading = false,
        onClickKakaoButton = {},
        modifier = Modifier.fillMaxSize(),
    )
```

- [x] **Step 2: LoginRoute 배선**

`LoginRoute.kt` — 상태 수집·에러 수집·두 목적지·SDK 결과 전달을 고친다.

추가/변경 import:

```kotlin
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.ui.CollectAppError
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
```

본문 변경 — `LaunchedEffect` 블록과 `LoginScreen` 호출:

```kotlin
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectAppError(viewModel)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToGroupList -> {
                    navigator.clearBackStack()
                    navigator.goTo(destination = NavKeyGroupList)
                }

                is LoginSideEffect.NavigateToTermAgree -> {
                    navigator.goTo(
                        destination = NavKeyTermAgree(registrationToken = effect.registrationToken),
                    )
                }

                is LoginSideEffect.RequestLoginWithKakao -> {
                    // activity 가 null 이면 로딩이 켜진 채 영영 남는다 — 실패로 닫는다
                    val currentActivity = activity
                    if (currentActivity == null) {
                        viewModel.processIntent(
                            LoginIntent.LoginWithKakaoFailure(IllegalStateException("Activity 가 없다")),
                        )
                        return@collect
                    }

                    when (val result = kakaoLoginHelper.login(currentActivity)) {
                        is KakaoLoginResult.Success ->
                            viewModel.processIntent(
                                LoginIntent.LoginWithKakaoSuccess(
                                    idToken = result.idToken,
                                    nonce = result.nonce,
                                ),
                            )

                        is KakaoLoginResult.Failure ->
                            viewModel.processIntent(LoginIntent.LoginWithKakaoFailure(result.throwable))

                        is KakaoLoginResult.Cancel ->
                            viewModel.processIntent(LoginIntent.LoginWithKakaoCancel)
                    }
                }
            }
        }
    }

    LoginScreen(
        pages = tempPages,
        isLoading = state.isLoading,
        onClickKakaoButton = {
            viewModel.processIntent(LoginIntent.LoginWithKakao)
        },
        modifier = modifier,
    )
```

- [x] **Step 3: 컴파일·테스트**

Run: `./gradlew ktlintCheck :feature:login:impl:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: 전체 검증**

Run: `./gradlew ktlintCheck test :app:assembleDebug`
Expected: BUILD SUCCESSFUL — 이 계획의 신규 테스트 20건 + 인프라 계획 15건 + 기존 테스트 전부 통과

- [x] **Step 5: 커밋**

```bash
git add feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/route/LoginRoute.kt \
        feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/screen/LoginScreen.kt
git commit -m "feat(login): 로그인 결과에 따른 목적지 분기와 로딩 배선"
```

- [ ] **Step 6: 실기기 검증 — 결과를 보고한다(코드 수정 금지)**

앱 최초의 실서버 호출이다. 아래를 확인하고 **결과만 보고한다.** 실패해도 그 자리에서 고치지 않는다.

1. 개발 서버에 요청이 나가는가 — **평문 HTTP면 차단된다.** 차단이면 여기서 멈추고 보고한다.
2. 신규 계정으로 로그인 → 약관 화면으로 이동하는가. 로그에서 응답 판별자 키가 **`isNewUser`**인지 확인한다(`MissingFieldException`이 나면 서버가 `newUser`를 쓰는 것이므로 계약 문서가 틀린 것 — 보고 대상).
3. 기존 계정으로 로그인 → 그룹 목록으로 이동하고 백스택이 비는가(뒤로가기로 로그인에 돌아오지 않는가).
4. 로그인 후 **앱 종료 → 재시작 → 토큰이 읽히는가**. DataStore 파일에 토큰 평문이 없는지 함께 확인한다.
5. 카카오 로그인 창에서 **뒤로가기(취소)** → 로딩이 풀리고 버튼이 다시 눌리는가.
6. 버튼 **연타** → 카카오 창이 한 번만 뜨는가.
7. 비행기 모드에서 로그인 → 로딩이 풀리고 `AppError.Network` 로그가 남는가.
8. `TokenStoreTokenProvider`의 `runBlocking`이 체감되는 지연을 만드는가(첫 인증 요청 시점).

---

## 완료 조건

- [x] `./gradlew ktlintCheck test :app:assembleDebug` 통과
- [x] 신규 테스트 20건 통과(nonce 3 · 직렬화 2 · Repository 4 · UseCase 3 · ViewModel 8)
- [ ] 실기기 8항목 결과 보고(막힌 항목은 사유와 함께)
- [x] push·PR 없음(로컬 커밋 6개)

## 함정

- **`nonce`를 두 번 만들지 않는다.** SDK에 넘긴 값과 서버에 보내는 값이 갈리면 서버가 401 `INVALID_ID_TOKEN`을 준다. 원인이 nonce라는 단서가 응답에 없어 진단이 오래 걸린다.
- **`token.accessToken`을 쓰지 않는다.** 그것이 지금 코드가 틀린 지점이다. 서버가 요구하는 것은 `idToken`이다.
- **`idToken`은 nullable이다.** null 분기를 지우면 `!!`가 되고, 콘솔 설정이 바뀌는 순간 크래시한다.
- **기존 회원 분기에서만 백스택을 지운다.** 신규 회원은 약관에서 뒤로가기로 로그인에 돌아와야 한다.
- **Task 5는 단독으로 컴파일되지 않는다.** Task 6과 한 커밋이다. 중간에서 빌드가 깨지는 것은 예상된 상태다.
- **`@SerialName("isNewUser")`를 지우면 안 된다.** DTO 전 프로퍼티에 `@SerialName`을 명시하는 것이 이 저장소 규약이고, 값이 옳을 때만 방어가 된다.
