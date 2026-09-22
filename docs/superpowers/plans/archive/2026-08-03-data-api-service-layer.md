# :data API Service·remote DataSource 레이어 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 계약 14개 엔드포인트 전량을 `:data`의 Retrofit Service·remote DataSource와 `:domain` VO로 구현한다.

**Architecture:** `Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. Service 4개는 서버 도메인 계약 문서와 1:1이고 함수명은 경로를 그대로 비춘다. 의미 기반 이름과 value class 경계는 DataSource·domain이 갖는다. Repository·UseCase·화면 결선은 이 계획 범위 밖이다.

**Tech Stack:** Retrofit · kotlinx-serialization · kotlinx-datetime · Hilt · Kotlin `@JvmInline value class`

**스펙:** [specs/archive/2026-08-03-data-api-service-layer.md](../../specs/archive/2026-08-03-data-api-service-layer.md)
**계약 정본:** [api/](../../../api/README.md) (서버 `main` `69654bc`)

## Global Constraints

- **테스트를 작성하지 않는다.** repo 전체에 `test`·`androidTest` 소스셋이 0개이고 이번 라운드에 테스트 인프라를 세우지 않기로 했다. TDD 대신 각 Task의 검증은 **컴파일 + ktlint**다. 이 계획은 그 점에서 의도적으로 TDD를 벗어난다.
- **TJYG-Android는 커밋하지 않는다.** 작업 트리 변경만 남기고 보고한다. 각 Task의 마지막 단계는 commit이 아니라 검증이다.
- 패키지 루트는 `com.teamyg.parfait`. `:data`의 소스 디렉토리는 `data/src/main/java/`, `:domain`은 `domain/src/main/java/`다(둘 다 `java/` 경로에 Kotlin을 둔다 — 기존 관례).
- **DTO·Retrofit 시그니처에 value class를 쓰지 않는다.** raw 타입(`Long`·`String`·`Int`)만 쓰고 mapper가 감싼다.
- **Retrofit 경로에 앞 슬래시를 붙이지 않는다**(`"api/v1/auth/kakao"`). 기존 `TempService`와 같은 방식이며 `BuildConfig.BASE_URL`이 `/`로 끝난다는 전제다.
- `@NoAuth`는 `postAuthKakao`·`postAuthSignup`·`postAuthReissue`·`getPolicies` **4곳에만** 붙인다. `postAuthLogout`에는 붙이지 않는다.
- 파르페 규율: 라인번호·색 hex·변동 수치를 문서에 적지 않는다. 근거는 파일명 + 심볼명.
- 검증 명령 3종:
  - `./gradlew :domain:compileKotlin` (`:domain`은 Kotlin JVM 모듈이라 `compileDebugKotlin`이 없다)
  - `./gradlew :data:compileDebugKotlin`
  - `./gradlew :domain:ktlintCheck :data:ktlintCheck`

---

### Task 1: domain value class · VO

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/id/Ids.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/auth/AuthTokens.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/auth/KakaoLoginVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/auth/AuthSessionVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/auth/TermsAgreement.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/policy/PolicyVO.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/group/GroupValues.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupVO.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/KakaoLoginResult.kt` (KDoc 한 줄 추가)

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `GroupId`·`MemberId`·`TermsId`·`ReportId`(각 `value: Long`), `AccessToken`·`RefreshToken`·`RegistrationToken`·`InviteCode`·`GroupName`·`GroupNickname`(각 `value: String`), `AuthSessionVO(accessToken: AccessToken, refreshToken: RefreshToken, expiresIn: Duration)`, `KakaoLoginVO.ExistingMember(session: AuthSessionVO)`·`KakaoLoginVO.NewUser(registrationToken: RegistrationToken)`, `TermsAgreement(termsId: TermsId, agreed: Boolean)`, `PolicyVO`·`PolicyType`, `MyParfaitGroupVO`·`ParfaitGroupDetailVO`·`ParfaitGroupMemberVO`·`JoinedGroupVO`·`CreatedGroupVO`·`GroupNicknameVO`·`ReportedGroupVO`

- [ ] **Step 1: ID value class 4종 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/id/Ids.kt`

```kotlin
package com.teamyg.parfait.domain.model.id

@JvmInline
value class GroupId(val value: Long)

@JvmInline
value class MemberId(val value: Long)

@JvmInline
value class TermsId(val value: Long)

@JvmInline
value class ReportId(val value: Long)
```

- [ ] **Step 2: 토큰 value class 3종 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/auth/AuthTokens.kt`

```kotlin
package com.teamyg.parfait.domain.model.auth

/**
 * 서버가 발급하는 토큰 3종. 전부 String 이지만 서로 대체할 수 없다.
 *
 * 재발급(`/api/v1/auth/reissue`)에 access token 을 넣으면 재발급 자체가 막힌다 —
 * 실제로 겪은 사고라 타입으로 가른다.
 */
@JvmInline
value class AccessToken(val value: String)

@JvmInline
value class RefreshToken(val value: String)

@JvmInline
value class RegistrationToken(val value: String)
```

- [ ] **Step 3: auth VO 3종 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/auth/AuthSessionVO.kt`

```kotlin
package com.teamyg.parfait.domain.model.auth

import kotlin.time.Duration

/**
 * 로그인·회원가입·재발급이 공통으로 돌려주는 세션.
 *
 * 서버는 `expiresIn` 을 **초 단위 Long** 으로 주고, mapper 가 [Duration] 으로 바꾼다.
 */
data class AuthSessionVO(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
    val expiresIn: Duration,
)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/auth/KakaoLoginVO.kt`

```kotlin
package com.teamyg.parfait.domain.model.auth

/**
 * 카카오 로그인 **서버 응답**. 카카오 SDK 로그인 결과는
 * [com.teamyg.parfait.domain.model.KakaoLoginResult] 로 별개다.
 *
 * 서버는 판별자 하나(`newUser`)로 두 묶음을 갈라 보내고 나머지 4필드가 전부 nullable 이다.
 * mapper 가 그 분기를 흡수하므로 여기서는 각 갈래가 필요한 값을 반드시 갖는다.
 */
sealed interface KakaoLoginVO {
    data class ExistingMember(val session: AuthSessionVO) : KakaoLoginVO

    data class NewUser(val registrationToken: RegistrationToken) : KakaoLoginVO
}
```

`domain/src/main/java/com/teamyg/parfait/domain/model/auth/TermsAgreement.kt`

```kotlin
package com.teamyg.parfait.domain.model.auth

import com.teamyg.parfait.domain.model.id.TermsId

/**
 * 회원가입 시 보내는 약관 동의 항목. `termsId` 는 `GET /api/v1/policies` 가 준 값이다.
 */
data class TermsAgreement(
    val termsId: TermsId,
    val agreed: Boolean,
)
```

- [ ] **Step 4: policy VO 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/policy/PolicyVO.kt`

```kotlin
package com.teamyg.parfait.domain.model.policy

import com.teamyg.parfait.domain.model.id.TermsId

data class PolicyVO(
    val termsId: TermsId,
    val type: PolicyType,
    val title: String,
    val url: String,
    val required: Boolean,
)

/**
 * 서버는 현재 2종만 준다. [UNKNOWN] 은 서버가 종류를 늘렸을 때
 * 앱이 파싱에서 죽지 않게 하는 폴백이다.
 */
enum class PolicyType {
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    UNKNOWN,
}
```

- [ ] **Step 5: group value class 3종 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/group/GroupValues.kt`

```kotlin
package com.teamyg.parfait.domain.model.group

@JvmInline
value class InviteCode(val value: String)

@JvmInline
value class GroupName(val value: String)

@JvmInline
value class GroupNickname(val value: String)
```

- [ ] **Step 6: group VO 7종 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/group/ParfaitGroupVO.kt`

```kotlin
package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.id.ReportId
import kotlinx.datetime.LocalDateTime

/**
 * @param recentImageUploadedAt 오프셋 없는 로컬 시각이며 기준은 **Asia/Seoul 벽시계**다.
 *   UTC 로 취급하면 시각이 어긋난다.
 */
data class MyParfaitGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime?,
)

data class ParfaitGroupDetailVO(
    val groupId: GroupId,
    val groupNickname: GroupNickname,
    val inviteCode: InviteCode,
    val members: List<ParfaitGroupMemberVO>,
)

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
)

data class JoinedGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
)

data class CreatedGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val inviteCode: InviteCode,
    val memberLimit: Int,
)

data class GroupNicknameVO(
    val groupId: GroupId,
    val groupNickname: GroupNickname,
)

data class ReportedGroupVO(
    val groupId: GroupId,
    val reportId: ReportId,
)
```

- [ ] **Step 7: 기존 `KakaoLoginResult`에 상호 참조 KDoc 추가**

`domain/src/main/java/com/teamyg/parfait/domain/model/KakaoLoginResult.kt` — 기존 내용은 그대로 두고 선언 위에 KDoc만 붙인다.

```kotlin
package com.teamyg.parfait.domain.model

/**
 * 카카오 **SDK** 로그인 결과. 서버 로그인 응답은
 * [com.teamyg.parfait.domain.model.auth.KakaoLoginVO] 로 별개다.
 */
sealed interface KakaoLoginResult {
    data class Success(val token: String) : KakaoLoginResult

    data class Cancel(val throwable: Throwable?) : KakaoLoginResult

    data class Failure(val throwable: Throwable?) : KakaoLoginResult
}
```

- [ ] **Step 8: 컴파일·ktlint 검증**

```bash
./gradlew :domain:compileKotlin
./gradlew :domain:ktlintCheck
```

Expected: 둘 다 BUILD SUCCESSFUL. 실패 시 `kotlinx.datetime.LocalDateTime`·`kotlin.time.Duration` import 경로부터 확인한다(`:domain`은 `parfait.kotlin.jvm` 컨벤션 플러그인이 `kotlinx-datetime`을 넣어 준다).

---

### Task 2: Auth Service·DataSource

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/AuthService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/auth/AuthRequests.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/auth/AuthResponses.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/auth/mapper/VOMapper.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/auth/remote/AuthRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/auth/remote/AuthRemoteDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 1의 `AccessToken`·`RefreshToken`·`RegistrationToken`·`AuthSessionVO`·`KakaoLoginVO`·`TermsAgreement`·`TermsId`. 기존 `ApiCaller.safeApiCall`·`ApiCaller.safeApiCallNoContent`, `ApiResponse<T>`, `@NoAuth`.
- Produces: `AuthRemoteDataSource.loginWithKakao(idToken: String, nonce: String): Result<KakaoLoginVO>` · `signup(registrationToken: RegistrationToken, agreements: List<TermsAgreement>): Result<AuthSessionVO>` · `reissue(refreshToken: RefreshToken): Result<AuthSessionVO>` · `logout(refreshToken: RefreshToken): Result<Unit>`

- [ ] **Step 1: 요청 DTO 작성**

`data/src/main/java/com/teamyg/parfait/data/service/model/request/auth/AuthRequests.kt`

```kotlin
package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    val idToken: String,
    val nonce: String,
)

@Serializable
data class SignupRequest(
    val registrationToken: String,
    val agreements: List<TermsAgreementRequest>,
)

@Serializable
data class TermsAgreementRequest(
    val termsId: Long,
    val agreed: Boolean,
)

@Serializable
data class ReissueRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)
```

- [ ] **Step 2: 응답 DTO 작성**

`data/src/main/java/com/teamyg/parfait/data/service/model/response/auth/AuthResponses.kt`

`SignupResponse`와 `ReissueResponse`는 필드가 같지만 **서버 타입명을 그대로 미러링**한다. 전송 계층은 계약을 비추는 층이고, 서버가 둘 중 하나만 바꿔도 이쪽을 쪼갤 필요가 없다.

```kotlin
package com.teamyg.parfait.data.service.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ⚠️ 판별자의 JSON 키는 `newUser` 다. 서버 Kotlin 은 `val isNewUser` 지만
 * Jackson 이 `is` 접두사를 떼고 직렬화한다.
 *
 * `@SerialName` 을 빼면 예외가 나지 않는다 — `provideRemoteJson` 이 `coerceInputValues = true`
 * 라서 값이 조용히 false 로 떨어지고, 신규 유저가 기존 회원으로 분기한다.
 */
@Serializable
data class KakaoLoginResponse(
    @SerialName("newUser")
    val isNewUser: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val registrationToken: String? = null,
)

@Serializable
data class SignupResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class ReissueResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
```

- [ ] **Step 3: `AuthService` 작성**

`data/src/main/java/com/teamyg/parfait/data/service/AuthService.kt`

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoAuth
import com.teamyg.parfait.data.service.model.request.auth.KakaoLoginRequest
import com.teamyg.parfait.data.service.model.request.auth.LogoutRequest
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.service.model.request.auth.SignupRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.auth.KakaoLoginResponse
import com.teamyg.parfait.data.service.model.response.auth.ReissueResponse
import com.teamyg.parfait.data.service.model.response.auth.SignupResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @NoAuth
    @POST("api/v1/auth/kakao")
    suspend fun postAuthKakao(
        @Body request: KakaoLoginRequest,
    ): ApiResponse<KakaoLoginResponse>

    @NoAuth
    @POST("api/v1/auth/signup")
    suspend fun postAuthSignup(
        @Body request: SignupRequest,
    ): ApiResponse<SignupResponse>

    @NoAuth
    @POST("api/v1/auth/reissue")
    suspend fun postAuthReissue(
        @Body request: ReissueRequest,
    ): ApiResponse<ReissueResponse>

    /**
     * 화이트리스트 **밖**이라 `@NoAuth` 를 붙이지 않는다 — access token 이 필요하다.
     *
     * 204 라 응답 본문이 없다. envelope 조차 오지 않으므로 반환 타입이 `Unit` 이고
     * 호출부는 `ApiCaller.safeApiCallNoContent` 를 쓴다.
     */
    @POST("api/v1/auth/logout")
    suspend fun postAuthLogout(
        @Body request: LogoutRequest,
    )
}
```

- [ ] **Step 4: auth mapper 작성**

`data/src/main/java/com/teamyg/parfait/data/source/auth/mapper/VOMapper.kt`

`requireNotNull`이 던지는 `IllegalArgumentException`은 `ApiCaller`가 `ApiException.Unknown`으로 잡는다. 서버가 약속을 깨면 여기서 드러난다.

```kotlin
package com.teamyg.parfait.data.source.auth.mapper

import com.teamyg.parfait.data.service.model.request.auth.TermsAgreementRequest
import com.teamyg.parfait.data.service.model.response.auth.KakaoLoginResponse
import com.teamyg.parfait.data.service.model.response.auth.ReissueResponse
import com.teamyg.parfait.data.service.model.response.auth.SignupResponse
import com.teamyg.parfait.domain.model.auth.AccessToken
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import kotlin.time.Duration.Companion.seconds

internal fun KakaoLoginResponse.toKakaoLoginVO(): KakaoLoginVO = when (isNewUser) {
    true -> KakaoLoginVO.NewUser(
        registrationToken = RegistrationToken(
            requireNotNull(registrationToken) { "newUser=true 인데 registrationToken 이 없다" },
        ),
    )

    false -> KakaoLoginVO.ExistingMember(
        session = AuthSessionVO(
            accessToken = AccessToken(
                requireNotNull(accessToken) { "newUser=false 인데 accessToken 이 없다" },
            ),
            refreshToken = RefreshToken(
                requireNotNull(refreshToken) { "newUser=false 인데 refreshToken 이 없다" },
            ),
            expiresIn = requireNotNull(expiresIn) { "newUser=false 인데 expiresIn 이 없다" }.seconds,
        ),
    )
}

internal fun SignupResponse.toAuthSessionVO(): AuthSessionVO = AuthSessionVO(
    accessToken = AccessToken(accessToken),
    refreshToken = RefreshToken(refreshToken),
    expiresIn = expiresIn.seconds,
)

internal fun ReissueResponse.toAuthSessionVO(): AuthSessionVO = AuthSessionVO(
    accessToken = AccessToken(accessToken),
    refreshToken = RefreshToken(refreshToken),
    expiresIn = expiresIn.seconds,
)

internal fun TermsAgreement.toRequest(): TermsAgreementRequest = TermsAgreementRequest(
    termsId = termsId.value,
    agreed = agreed,
)
```

- [ ] **Step 5: `AuthRemoteDataSource` 인터페이스 작성**

`data/src/main/java/com/teamyg/parfait/data/source/auth/remote/AuthRemoteDataSource.kt`

```kotlin
package com.teamyg.parfait.data.source.auth.remote

import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement

interface AuthRemoteDataSource {
    /**
     * @param idToken 카카오 SDK 가 발급한 ID 토큰(서버 발급 토큰이 아니라 raw String 이다)
     * @param nonce 앱이 생성해 카카오 SDK 요청과 **같은 값**을 보내야 한다
     */
    suspend fun loginWithKakao(idToken: String, nonce: String): Result<KakaoLoginVO>

    suspend fun signup(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO>

    suspend fun reissue(refreshToken: RefreshToken): Result<AuthSessionVO>

    suspend fun logout(refreshToken: RefreshToken): Result<Unit>
}
```

- [ ] **Step 6: `AuthRemoteDataSourceImpl` 작성**

`data/src/main/java/com/teamyg/parfait/data/source/auth/remote/AuthRemoteDataSourceImpl.kt`

```kotlin
package com.teamyg.parfait.data.source.auth.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.KakaoLoginRequest
import com.teamyg.parfait.data.service.model.request.auth.LogoutRequest
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.service.model.request.auth.SignupRequest
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.auth.mapper.toKakaoLoginVO
import com.teamyg.parfait.data.source.auth.mapper.toRequest
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authService: AuthService,
    private val apiCaller: ApiCaller,
) : AuthRemoteDataSource {
    override suspend fun loginWithKakao(idToken: String, nonce: String): Result<KakaoLoginVO> = apiCaller
        .safeApiCall {
            authService.postAuthKakao(KakaoLoginRequest(idToken = idToken, nonce = nonce))
        }.map { it.toKakaoLoginVO() }

    override suspend fun signup(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO> = apiCaller
        .safeApiCall {
            authService.postAuthSignup(
                SignupRequest(
                    registrationToken = registrationToken.value,
                    agreements = agreements.map { it.toRequest() },
                ),
            )
        }.map { it.toAuthSessionVO() }

    override suspend fun reissue(refreshToken: RefreshToken): Result<AuthSessionVO> = apiCaller
        .safeApiCall {
            authService.postAuthReissue(ReissueRequest(refreshToken = refreshToken.value))
        }.map { it.toAuthSessionVO() }

    override suspend fun logout(refreshToken: RefreshToken): Result<Unit> = apiCaller
        .safeApiCallNoContent {
            authService.postAuthLogout(LogoutRequest(refreshToken = refreshToken.value))
        }
}
```

- [ ] **Step 7: DI 등록**

`ServiceModule.kt`의 `object ServiceModule` 본문에 아래 함수를 **추가**한다(기존 `provideTempService`는 Task 6에서 지운다).

```kotlin
    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)
```

import 추가: `com.teamyg.parfait.data.service.AuthService`

`RemoteDataSourceModule.kt`의 `interface RemoteDataSourceModule` 본문에 아래를 **추가**한다.

```kotlin
    @Binds
    @Singleton
    fun bindAuthRemoteDataSource(authRemoteDataSourceImpl: AuthRemoteDataSourceImpl): AuthRemoteDataSource
```

import 추가: `com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSource`, `com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSourceImpl`

- [ ] **Step 8: 컴파일·ktlint 검증**

```bash
./gradlew :data:compileDebugKotlin
./gradlew :data:ktlintCheck
```

Expected: 둘 다 BUILD SUCCESSFUL.

- [ ] **Step 9: `@SerialName` 육안 확인**

`AuthResponses.kt`를 다시 열어 `KakaoLoginResponse.isNewUser` 위에 `@SerialName("newUser")`가 실제로 붙어 있는지 확인한다. **이 한 줄은 컴파일로도 ktlint로도 잡히지 않고 런타임에도 예외를 내지 않는다** — 빠지면 신규 유저 분기가 조용히 뒤집힌다.

---

### Task 3: Policy Service·DataSource

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/PolicyService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/policy/PolicyResponses.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/policy/mapper/VOMapper.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 1의 `PolicyVO`·`PolicyType`·`TermsId`. 기존 `ApiCaller.safeApiCall`, `ApiResponse<T>`, `@NoAuth`.
- Produces: `PolicyRemoteDataSource.getPolicies(): Result<List<PolicyVO>>`

- [ ] **Step 1: 응답 DTO 작성**

`data/src/main/java/com/teamyg/parfait/data/service/model/response/policy/PolicyResponses.kt`

```kotlin
package com.teamyg.parfait.data.service.model.response.policy

import kotlinx.serialization.Serializable

@Serializable
data class PolicyResponse(
    val policies: List<PolicyItemResponse>,
)

/**
 * @param type 서버가 enum 이름 문자열로 준다(`TERMS_OF_SERVICE`·`PRIVACY_POLICY`).
 * @param url 서버가 URL 전용 컬럼이 아니라 약관 본문 컬럼을 그대로 매핑한 값이라
 *   링크가 아닐 수 있다(계약 문서 `api/policy.md` 미결).
 */
@Serializable
data class PolicyItemResponse(
    val termsId: Long,
    val type: String,
    val title: String,
    val url: String,
    val required: Boolean,
)
```

- [ ] **Step 2: `PolicyService` 작성**

`data/src/main/java/com/teamyg/parfait/data/service/PolicyService.kt`

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoAuth
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import retrofit2.http.GET

/**
 * 서버에서는 `PolicyController` 가 `http/auth` 패키지에 있고 OpenAPI 태그도 `Auth` 지만,
 * 경로가 `/api/v1/auth` 하위가 아니라 최상위 `/api/v1/policies` 라 별도 서비스로 둔다.
 */
interface PolicyService {
    @NoAuth
    @GET("api/v1/policies")
    suspend fun getPolicies(): ApiResponse<PolicyResponse>
}
```

- [ ] **Step 3: policy mapper 작성**

`data/src/main/java/com/teamyg/parfait/data/source/policy/mapper/VOMapper.kt`

```kotlin
package com.teamyg.parfait.data.source.policy.mapper

import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO

internal fun PolicyResponse.toPolicyVOList(): List<PolicyVO> = policies.map { it.toPolicyVO() }

internal fun PolicyItemResponse.toPolicyVO(): PolicyVO = PolicyVO(
    termsId = TermsId(termsId),
    type = type.toPolicyType(),
    title = title,
    url = url,
    required = required,
)

/** 서버가 종류를 늘려도 앱이 죽지 않도록 모르는 값은 [PolicyType.UNKNOWN] 으로 떨어뜨린다. */
private fun String.toPolicyType(): PolicyType = when (this) {
    PolicyType.TERMS_OF_SERVICE.name -> PolicyType.TERMS_OF_SERVICE
    PolicyType.PRIVACY_POLICY.name -> PolicyType.PRIVACY_POLICY
    else -> PolicyType.UNKNOWN
}
```

- [ ] **Step 4: `PolicyRemoteDataSource` 인터페이스 작성**

`data/src/main/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSource.kt`

```kotlin
package com.teamyg.parfait.data.source.policy.remote

import com.teamyg.parfait.domain.model.policy.PolicyVO

interface PolicyRemoteDataSource {
    /**
     * 현재 유효한 약관 목록. 타입당 최신 1건씩이라 길이는 0~2다.
     *
     * **빈 리스트가 정상 응답이다** — 서버가 200 에 빈 배열을 준다. 소비 측이 길이를 확인하지 않고
     * 회원가입으로 넘어가면 `REQUIRED_TERMS_NOT_AGREED` 400 을 받는다.
     */
    suspend fun getPolicies(): Result<List<PolicyVO>>
}
```

- [ ] **Step 5: `PolicyRemoteDataSourceImpl` 작성**

`data/src/main/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSourceImpl.kt`

```kotlin
package com.teamyg.parfait.data.source.policy.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.PolicyService
import com.teamyg.parfait.data.source.policy.mapper.toPolicyVOList
import com.teamyg.parfait.domain.model.policy.PolicyVO
import javax.inject.Inject

class PolicyRemoteDataSourceImpl @Inject constructor(
    private val policyService: PolicyService,
    private val apiCaller: ApiCaller,
) : PolicyRemoteDataSource {
    override suspend fun getPolicies(): Result<List<PolicyVO>> = apiCaller
        .safeApiCall { policyService.getPolicies() }
        .map { it.toPolicyVOList() }
}
```

- [ ] **Step 6: DI 등록**

`ServiceModule.kt`에 추가:

```kotlin
    @Provides
    @Singleton
    fun providePolicyService(retrofit: Retrofit): PolicyService = retrofit.create(PolicyService::class.java)
```

`RemoteDataSourceModule.kt`에 추가:

```kotlin
    @Binds
    @Singleton
    fun bindPolicyRemoteDataSource(policyRemoteDataSourceImpl: PolicyRemoteDataSourceImpl): PolicyRemoteDataSource
```

import 추가 — `ServiceModule.kt`에 `com.teamyg.parfait.data.service.PolicyService`,
`RemoteDataSourceModule.kt`에 `com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSource`와
`com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSourceImpl`.

- [ ] **Step 7: 컴파일·ktlint 검증**

```bash
./gradlew :data:compileDebugKotlin
./gradlew :data:ktlintCheck
```

Expected: 둘 다 BUILD SUCCESSFUL.

---

### Task 4: ParfaitGroup Service·DataSource

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitGroupService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/group/ParfaitGroupRequests.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/group/ParfaitGroupResponses.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 1의 `GroupId`·`MemberId`·`ReportId`·`InviteCode`·`GroupName`·`GroupNickname`과 group VO 7종. 기존 `ApiCaller.safeApiCall`, `ApiResponse<T>`.
- Produces: `ParfaitGroupRemoteDataSource`의 8개 함수(아래 Step 5 시그니처 그대로)

- [ ] **Step 1: 요청 DTO 작성**

`data/src/main/java/com/teamyg/parfait/data/service/model/request/group/ParfaitGroupRequests.kt`

```kotlin
package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.Serializable

@Serializable
data class JoinParfaitGroupRequest(
    val inviteCode: String,
)

@Serializable
data class CreateParfaitGroupRequest(
    val groupName: String,
    val groupNickname: String,
    val memberLimit: Int,
)

@Serializable
data class ChangeMyParfaitGroupNicknameRequest(
    val groupNickname: String,
)

@Serializable
data class ReportParfaitGroupRequest(
    val reason: String,
)
```

- [ ] **Step 2: 응답 DTO 작성**

`data/src/main/java/com/teamyg/parfait/data/service/model/response/group/ParfaitGroupResponses.kt`

```kotlin
package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.Serializable

/**
 * @param recentImageUploadedAt 오프셋 없는 ISO 로컬 날짜시간 문자열(`2026-08-01T12:00:00`).
 *   기준은 **Asia/Seoul 벽시계**다 — UTC 로 취급하면 시각이 어긋난다.
 *   최근 업로드 이미지가 없으면 `recentImageUrl` 과 함께 null 이다(생략이 아니라 명시적 null).
 */
@Serializable
data class MyParfaitGroupResponse(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String? = null,
    val recentImageUploadedAt: String? = null,
)

@Serializable
data class MyParfaitGroupDetailResponse(
    val groupId: Long,
    val groupNickname: String,
    val inviteCode: String,
    val members: List<ParfaitGroupMemberResponse>,
)

@Serializable
data class ParfaitGroupMemberResponse(
    val memberId: Long,
    val groupNickname: String,
)

@Serializable
data class PreviewParfaitGroupJoinResponse(
    val groupName: String,
)

@Serializable
data class JoinParfaitGroupResponse(
    val groupId: Long,
    val groupName: String,
)

@Serializable
data class CreateParfaitGroupResponse(
    val groupId: Long,
    val groupName: String,
    val inviteCode: String,
    val memberLimit: Int,
)

@Serializable
data class ChangeMyParfaitGroupNicknameResponse(
    val groupId: Long,
    val groupNickname: String,
)

@Serializable
data class LeaveParfaitGroupResponse(
    val groupId: Long,
)

@Serializable
data class ReportParfaitGroupResponse(
    val groupId: Long,
    val reportId: Long,
)
```

- [ ] **Step 3: `ParfaitGroupService` 작성**

`data/src/main/java/com/teamyg/parfait/data/service/ParfaitGroupService.kt`

경로에 버전 프리픽스가 없다(`api/parfait-groups`). 파르페 연도 조회만 `api/v1/groups/...`를 쓴다 — 서버 URL 규약이 갈린 상태라 줄여 쓰지 않는다.

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.group.ChangeMyParfaitGroupNicknameRequest
import com.teamyg.parfait.data.service.model.request.group.CreateParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.JoinParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.ReportParfaitGroupRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.group.ChangeMyParfaitGroupNicknameResponse
import com.teamyg.parfait.data.service.model.response.group.CreateParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.JoinParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.LeaveParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupDetailResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.PreviewParfaitGroupJoinResponse
import com.teamyg.parfait.data.service.model.response.group.ReportParfaitGroupResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ParfaitGroupService {
    @GET("api/parfait-groups")
    suspend fun getParfaitGroups(): ApiResponse<List<MyParfaitGroupResponse>>

    @GET("api/parfait-groups/{groupId}")
    suspend fun getParfaitGroupsByGroupId(
        @Path("groupId") groupId: Long,
    ): ApiResponse<MyParfaitGroupDetailResponse>

    @GET("api/parfait-groups/join-preview")
    suspend fun getParfaitGroupsJoinPreview(
        @Query("inviteCode") inviteCode: String,
    ): ApiResponse<PreviewParfaitGroupJoinResponse>

    @POST("api/parfait-groups/join")
    suspend fun postParfaitGroupsJoin(
        @Body request: JoinParfaitGroupRequest,
    ): ApiResponse<JoinParfaitGroupResponse>

    @POST("api/parfait-groups")
    suspend fun postParfaitGroups(
        @Body request: CreateParfaitGroupRequest,
    ): ApiResponse<CreateParfaitGroupResponse>

    @PATCH("api/parfait-groups/{groupId}/nickname")
    suspend fun patchParfaitGroupsByGroupIdNickname(
        @Path("groupId") groupId: Long,
        @Body request: ChangeMyParfaitGroupNicknameRequest,
    ): ApiResponse<ChangeMyParfaitGroupNicknameResponse>

    /** 탈퇴는 204 가 아니라 **200** 이고 envelope 로 `groupId` 를 돌려준다. */
    @DELETE("api/parfait-groups/{groupId}/members/me")
    suspend fun deleteParfaitGroupsByGroupIdMembersMe(
        @Path("groupId") groupId: Long,
    ): ApiResponse<LeaveParfaitGroupResponse>

    /** 신고가 성공하면 서버가 같은 트랜잭션에서 신고자를 **탈퇴 처리**한다. */
    @POST("api/parfait-groups/{groupId}/reports")
    suspend fun postParfaitGroupsByGroupIdReports(
        @Path("groupId") groupId: Long,
        @Body request: ReportParfaitGroupRequest,
    ): ApiResponse<ReportParfaitGroupResponse>
}
```

- [ ] **Step 4: group mapper 작성**

`data/src/main/java/com/teamyg/parfait/data/source/group/mapper/VOMapper.kt`

```kotlin
package com.teamyg.parfait.data.source.group.mapper

import com.teamyg.parfait.data.service.model.response.group.ChangeMyParfaitGroupNicknameResponse
import com.teamyg.parfait.data.service.model.response.group.CreateParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.JoinParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupDetailResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.ParfaitGroupMemberResponse
import com.teamyg.parfait.data.service.model.response.group.ReportParfaitGroupResponse
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.id.ReportId
import kotlinx.datetime.LocalDateTime

/**
 * `recentImageUploadedAt` 은 오프셋이 없는 ISO 로컬 날짜시간이고 기준은 **Asia/Seoul 벽시계**다
 * (서버 DB 커넥션·Hibernate 가 세 환경 모두 그 타임존으로 맞춰져 있다).
 * 파싱 실패는 삼키지 않는다 — `ApiCaller` 가 `ApiException.Unknown` 으로 잡는다.
 */
internal fun MyParfaitGroupResponse.toMyParfaitGroupVO(): MyParfaitGroupVO = MyParfaitGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    recentImageUrl = recentImageUrl,
    recentImageUploadedAt = recentImageUploadedAt?.let(LocalDateTime::parse),
)

internal fun MyParfaitGroupDetailResponse.toParfaitGroupDetailVO(): ParfaitGroupDetailVO = ParfaitGroupDetailVO(
    groupId = GroupId(groupId),
    groupNickname = GroupNickname(groupNickname),
    inviteCode = InviteCode(inviteCode),
    members = members.map { it.toParfaitGroupMemberVO() },
)

internal fun ParfaitGroupMemberResponse.toParfaitGroupMemberVO(): ParfaitGroupMemberVO = ParfaitGroupMemberVO(
    memberId = MemberId(memberId),
    groupNickname = GroupNickname(groupNickname),
)

internal fun JoinParfaitGroupResponse.toJoinedGroupVO(): JoinedGroupVO = JoinedGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
)

internal fun CreateParfaitGroupResponse.toCreatedGroupVO(): CreatedGroupVO = CreatedGroupVO(
    groupId = GroupId(groupId),
    groupName = GroupName(groupName),
    inviteCode = InviteCode(inviteCode),
    memberLimit = memberLimit,
)

internal fun ChangeMyParfaitGroupNicknameResponse.toGroupNicknameVO(): GroupNicknameVO = GroupNicknameVO(
    groupId = GroupId(groupId),
    groupNickname = GroupNickname(groupNickname),
)

internal fun ReportParfaitGroupResponse.toReportedGroupVO(): ReportedGroupVO = ReportedGroupVO(
    groupId = GroupId(groupId),
    reportId = ReportId(reportId),
)
```

- [ ] **Step 5: `ParfaitGroupRemoteDataSource` 인터페이스 작성**

`data/src/main/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSource.kt`

```kotlin
package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId

interface ParfaitGroupRemoteDataSource {
    suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>>

    suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO>

    /** 응답이 `groupName` 한 필드뿐이라 래퍼 VO 없이 값 자체를 돌려준다. */
    suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName>

    suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO>

    suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO>

    suspend fun changeMyNickname(groupId: GroupId, groupNickname: GroupNickname): Result<GroupNicknameVO>

    /** 응답이 `groupId` 한 필드뿐이라 래퍼 VO 없이 값 자체를 돌려준다. */
    suspend fun leaveGroup(groupId: GroupId): Result<GroupId>

    /** 성공하면 서버가 같은 트랜잭션에서 신고자를 탈퇴 처리한다 — 신고는 탈퇴를 동반한다. */
    suspend fun reportGroup(groupId: GroupId, reason: String): Result<ReportedGroupVO>
}
```

- [ ] **Step 6: `ParfaitGroupRemoteDataSourceImpl` 작성**

`data/src/main/java/com/teamyg/parfait/data/source/group/remote/ParfaitGroupRemoteDataSourceImpl.kt`

```kotlin
package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.model.request.group.ChangeMyParfaitGroupNicknameRequest
import com.teamyg.parfait.data.service.model.request.group.CreateParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.JoinParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.ReportParfaitGroupRequest
import com.teamyg.parfait.data.source.group.mapper.toCreatedGroupVO
import com.teamyg.parfait.data.source.group.mapper.toGroupNicknameVO
import com.teamyg.parfait.data.source.group.mapper.toJoinedGroupVO
import com.teamyg.parfait.data.source.group.mapper.toMyParfaitGroupVO
import com.teamyg.parfait.data.source.group.mapper.toParfaitGroupDetailVO
import com.teamyg.parfait.data.source.group.mapper.toReportedGroupVO
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import javax.inject.Inject

class ParfaitGroupRemoteDataSourceImpl @Inject constructor(
    private val parfaitGroupService: ParfaitGroupService,
    private val apiCaller: ApiCaller,
) : ParfaitGroupRemoteDataSource {
    override suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>> = apiCaller
        .safeApiCall { parfaitGroupService.getParfaitGroups() }
        .map { responses -> responses.map { it.toMyParfaitGroupVO() } }

    override suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO> = apiCaller
        .safeApiCall { parfaitGroupService.getParfaitGroupsByGroupId(groupId.value) }
        .map { it.toParfaitGroupDetailVO() }

    override suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName> = apiCaller
        .safeApiCall { parfaitGroupService.getParfaitGroupsJoinPreview(inviteCode.value) }
        .map { GroupName(it.groupName) }

    override suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO> = apiCaller
        .safeApiCall {
            parfaitGroupService.postParfaitGroupsJoin(JoinParfaitGroupRequest(inviteCode = inviteCode.value))
        }.map { it.toJoinedGroupVO() }

    override suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO> = apiCaller
        .safeApiCall {
            parfaitGroupService.postParfaitGroups(
                CreateParfaitGroupRequest(
                    groupName = groupName.value,
                    groupNickname = groupNickname.value,
                    memberLimit = memberLimit,
                ),
            )
        }.map { it.toCreatedGroupVO() }

    override suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO> = apiCaller
        .safeApiCall {
            parfaitGroupService.patchParfaitGroupsByGroupIdNickname(
                groupId = groupId.value,
                request = ChangeMyParfaitGroupNicknameRequest(groupNickname = groupNickname.value),
            )
        }.map { it.toGroupNicknameVO() }

    override suspend fun leaveGroup(groupId: GroupId): Result<GroupId> = apiCaller
        .safeApiCall { parfaitGroupService.deleteParfaitGroupsByGroupIdMembersMe(groupId.value) }
        .map { GroupId(it.groupId) }

    override suspend fun reportGroup(groupId: GroupId, reason: String): Result<ReportedGroupVO> = apiCaller
        .safeApiCall {
            parfaitGroupService.postParfaitGroupsByGroupIdReports(
                groupId = groupId.value,
                request = ReportParfaitGroupRequest(reason = reason),
            )
        }.map { it.toReportedGroupVO() }
}
```

- [ ] **Step 7: DI 등록**

`ServiceModule.kt`에 추가:

```kotlin
    @Provides
    @Singleton
    fun provideParfaitGroupService(retrofit: Retrofit): ParfaitGroupService =
        retrofit.create(ParfaitGroupService::class.java)
```

`RemoteDataSourceModule.kt`에 추가:

```kotlin
    @Binds
    @Singleton
    fun bindParfaitGroupRemoteDataSource(
        parfaitGroupRemoteDataSourceImpl: ParfaitGroupRemoteDataSourceImpl,
    ): ParfaitGroupRemoteDataSource
```

import 추가 — `ServiceModule.kt`에 `com.teamyg.parfait.data.service.ParfaitGroupService`,
`RemoteDataSourceModule.kt`에 `com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSource`와
`com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSourceImpl`.

- [ ] **Step 8: 컴파일·ktlint 검증**

```bash
./gradlew :data:compileDebugKotlin
./gradlew :data:ktlintCheck
```

Expected: 둘 다 BUILD SUCCESSFUL.

---

### Task 5: Parfait Service·DataSource

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/ParfaitResponses.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 1의 `GroupId`. 기존 `ApiCaller.safeApiCall`, `ApiResponse<T>`.
- Produces: `ParfaitRemoteDataSource.getYears(groupId: GroupId): Result<List<Int>>`

mapper 파일이 없다 — 응답이 `List<Int>` 하나라 감쌀 VO가 없다.

- [ ] **Step 1: 응답 DTO 작성**

`data/src/main/java/com/teamyg/parfait/data/service/model/response/parfait/ParfaitResponses.kt`

```kotlin
package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.Serializable

/** 경로 세그먼트는 단수 `year` 인데 응답 필드는 복수 `years` 다. */
@Serializable
data class ParfaitYearsResponse(
    val years: List<Int>,
)
```

- [ ] **Step 2: `ParfaitService` 작성**

`data/src/main/java/com/teamyg/parfait/data/service/ParfaitService.kt`

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.ParfaitYearsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ParfaitService {
    /**
     * 그룹을 가리키는 경로가 여기만 `groups` 이고 버전 프리픽스가 붙는다 —
     * 다른 그룹 API 는 전부 `api/parfait-groups` 다.
     *
     * 그룹 존재 여부를 확인하지 않는다. 없는 `groupId` 를 넣어도
     * `GROUP_NOT_FOUND` 404 가 아니라 `GROUP_NOT_JOINED` 403 이 온다.
     */
    @GET("api/v1/groups/{groupId}/parfaits/year")
    suspend fun getGroupsByGroupIdParfaitsYear(
        @Path("groupId") groupId: Long,
    ): ApiResponse<ParfaitYearsResponse>
}
```

- [ ] **Step 3: `ParfaitRemoteDataSource` 인터페이스 작성**

`data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSource.kt`

```kotlin
package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.id.GroupId

interface ParfaitRemoteDataSource {
    /**
     * 그룹 캘린더가 그릴 연도 선택지.
     *
     * `Year` value class 를 두지 않는다 — 리스트 원소가 전부 박싱되는데 얻는 구분이 약하다.
     */
    suspend fun getYears(groupId: GroupId): Result<List<Int>>
}
```

- [ ] **Step 4: `ParfaitRemoteDataSourceImpl` 작성**

`data/src/main/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImpl.kt`

```kotlin
package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.domain.model.id.GroupId
import javax.inject.Inject

class ParfaitRemoteDataSourceImpl @Inject constructor(
    private val parfaitService: ParfaitService,
    private val apiCaller: ApiCaller,
) : ParfaitRemoteDataSource {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = apiCaller
        .safeApiCall { parfaitService.getGroupsByGroupIdParfaitsYear(groupId.value) }
        .map { it.years }
}
```

- [ ] **Step 5: DI 등록**

`ServiceModule.kt`에 추가:

```kotlin
    @Provides
    @Singleton
    fun provideParfaitService(retrofit: Retrofit): ParfaitService = retrofit.create(ParfaitService::class.java)
```

`RemoteDataSourceModule.kt`에 추가:

```kotlin
    @Binds
    @Singleton
    fun bindParfaitRemoteDataSource(
        parfaitRemoteDataSourceImpl: ParfaitRemoteDataSourceImpl,
    ): ParfaitRemoteDataSource
```

import 추가 — `ServiceModule.kt`에 `com.teamyg.parfait.data.service.ParfaitService`,
`RemoteDataSourceModule.kt`에 `com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource`와
`com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSourceImpl`.

- [ ] **Step 6: 컴파일·ktlint 검증**

```bash
./gradlew :data:compileDebugKotlin
./gradlew :data:ktlintCheck
```

Expected: 둘 다 BUILD SUCCESSFUL.

---

### Task 6: Temp 스텁 제거·통합 검증

**Files:**
- Delete: `data/src/main/java/com/teamyg/parfait/data/service/TempService.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/service/model/request/TempRequest.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/service/model/response/TempResponse.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/source/temp/remote/TempRemoteDataSource.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/source/temp/remote/TempRemoteDataSourceImpl.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/source/temp/mapper/VOMapper.kt`
- Delete: `domain/src/main/java/com/teamyg/parfait/domain/model/TempVO.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 2~5가 등록한 DI 바인딩(삭제 후에도 두 모듈이 비지 않는다)
- Produces: 없음(정리 Task)

- [ ] **Step 1: 소비처가 없는지 재확인**

```bash
grep -rn "TempService\|TempRequest\|TempResponse\|TempVO\|TempRemoteDataSource" --include="*.kt" .
```

Expected: 위 Files 목록의 파일과 `ServiceModule.kt`·`RemoteDataSourceModule.kt`에서만 나온다. **다른 모듈에서 나오면 삭제를 멈추고 보고한다.**
(`core:designsystem`의 `YGToppingTemplate`은 `Temp`로 시작하지 않으므로 이 grep에 걸리지 않는다.)

- [ ] **Step 2: 7개 파일 삭제**

```bash
rm data/src/main/java/com/teamyg/parfait/data/service/TempService.kt
rm data/src/main/java/com/teamyg/parfait/data/service/model/request/TempRequest.kt
rm data/src/main/java/com/teamyg/parfait/data/service/model/response/TempResponse.kt
rm -r data/src/main/java/com/teamyg/parfait/data/source/temp
rm domain/src/main/java/com/teamyg/parfait/domain/model/TempVO.kt
```

- [ ] **Step 3: DI 바인딩 제거**

`ServiceModule.kt`에서 `provideTempService` 함수와 `com.teamyg.parfait.data.service.TempService` import를 지운다.

`RemoteDataSourceModule.kt`에서 `bindTempRemoteDataSource` 함수와 `TempRemoteDataSource`·`TempRemoteDataSourceImpl` import를 지운다.

두 모듈에는 Task 2~5가 넣은 바인딩이 남으므로 빈 모듈이 되지 않는다.

- [ ] **Step 4: 컴파일·ktlint 검증**

```bash
./gradlew :domain:compileKotlin
./gradlew :data:compileDebugKotlin
./gradlew :domain:ktlintCheck :data:ktlintCheck
```

Expected: 전부 BUILD SUCCESSFUL.

- [ ] **Step 5: Hilt 그래프 검증**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. 이 단계가 DI 배선을 실제로 해석한다 — 앞선 Task의 `compileDebugKotlin`만으로는 바인딩 누락이 드러나지 않는다.

- [ ] **Step 6: 서비스 함수 이름·경로 대조**

`AuthService`·`PolicyService`·`ParfaitGroupService`·`ParfaitService` 4파일을 열어 함수명과 애노테이션 경로가 스펙의 표와 일치하는지 대조한다. 특히:
- `@NoAuth`가 정확히 4곳(`postAuthKakao`·`postAuthSignup`·`postAuthReissue`·`getPolicies`)에만 있는지
- `postAuthLogout`에 `@NoAuth`가 **없는지**
- 경로에 앞 슬래시가 없는지

- [ ] **Step 7: 문서 갱신**

문서 저장소(`team-yg-pesonal-agent`)에서:
- `parfait/api/auth.md`·`policy.md`·`parfait-group.md`·`parfait.md`의 **엔드포인트 표 Android 열**을 `미구현` → `구현됨`으로 바꾸고, 각 문서의 `## Android 매핑` 절에 대응 심볼(`AuthService#postAuthKakao`·`AuthRemoteDataSource#loginWithKakao` 형식)을 적는다. `android_status`를 `partial`로 올린다(Repository·화면이 아직 없다).
- `parfait/specs/2026-08-03-data-api-service-layer.md`의 `status`를 `implemented`로 바꾸고 `specs/archive/`로 옮긴다. `specs/README.md`의 인덱스 줄도 아카이브 표로 옮긴다.
- 이 계획서를 `plans/archive/`로 옮기고 `plans/README.md` 인덱스를 갱신한다.
- `parfait/synthesis/open-questions.md`에 남길 것 — `@NoAuth`의 R8 유지 미검증 사용처가 4곳으로 늘어난 사실, 이 레이어 전체가 런타임 미검증이라는 사실.

**TJYG-Android는 커밋하지 않는다.** 문서 저장소 커밋은 사용자 확인 후 별도로 진행한다.

---

## 실행 순서 메모

Task 2~5는 서로 의존하지 않는다(각자 다른 Service·DataSource 파일). 공유 파일은 `ServiceModule.kt`·`RemoteDataSourceModule.kt` 둘뿐이고 **추가만** 하므로 순서를 바꿔도 되고, 병렬로 진행하면 이 두 파일에서만 충돌이 난다.

Task 1은 나머지 전부의 선행이다. Task 6은 마지막이어야 한다 — 앞 Task가 DI 바인딩을 넣기 전에 `Temp*`를 지우면 중간 상태에서 모듈이 빌 수 있다.
