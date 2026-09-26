---
id: network-envelope-token-storage
title: 서버 계약 정합 — envelope·에러 파싱·암호화 토큰 저장 구현
status: done
type: work-order
created: 2026-08-02
updated: 2026-08-04
platforms: android
owner:
related_adr: ADR-0017, ADR-0008, ADR-0004
related_spec: network-envelope-token-storage
related_code: ApiResponse.kt, ApiCaller.kt, ApiException.kt#Business, CryptoManager.kt, TokenStore.kt, EncryptedTokenStore.kt, TokenStoreTokenProvider.kt, NetworkModule.kt, LocalDataSourceModule.kt
archived_reason: PR #190(feature/set-up-backend-api) develop 머지 완료 — Task 1~6 전량 반영. Task 5 Step 7(실기기 암복호화 왕복)은 `TokenStore.save()` 호출부 0건이라 수행 불가로 이월.
tags: [plan, parfait, network, data, auth, security]
---

# 서버 계약 정합 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `:data`가 서버 계약(`TEAMYG-SERVER` `main` `6f5bffc`)대로 응답을 해석하고, 인증이 필요한 API를 호출할 수 있는 상태로 만든다.

**Architecture:** envelope 필드를 서버와 맞추고 성공 판정을 `success` 필드로 교체한다. `SafeApiCall.kt`의 top-level 함수들을 `ApiCaller` 클래스로 승격해 `@RemoteJson`을 주입받고, `HttpException` 바디를 envelope로 역직렬화해 `code`·`errorDetail`에 도달한다. 토큰은 Android Keystore AES/GCM으로 암호화해 Preferences DataStore에 저장하고, 기존 `TokenProvider` 동기 추상화는 유지한 채 구현만 교체한다.

**Tech Stack:** Kotlin, Retrofit, OkHttp, kotlinx-serialization, Hilt, Jetpack DataStore(Preferences), Android Keystore

## Global Constraints

- **대상 저장소는 `TJYG-Android`다.** 로컬 경로는 `wiki/personal-private/project-paths.md` 참고(이하 `<A>`).
- **커밋하지 않는다.** 사용자 지시상 `TJYG-Android`는 구현이 끝나도 커밋하지 않는다 — 작업 트리 변경만 남기고 보고한다. 브랜치는 현재 `feature/set-up-backend-api`를 그대로 쓴다.
  > **as-built** — 이후 사용자가 같은 브랜치를 직접 커밋·PR(#190)해 2026-08-04 develop에 머지했다. 아래 "완료 기준"의 무커밋 조항도 이 사실로 대체된다.
- **테스트를 새로 만들지 않는다.** 코드베이스에 `test`·`androidTest` 디렉토리가 0건이다(무테스트 관례). 검증은 `assembleDebug` + ktlint + 실기기 육안이다.
- minSdk **26**. `java.util.Base64`(API 26+)가 아니라 **`android.util.Base64`**를 쓴다 — 플랫폼 API 레벨과 무관하다.
- 파르페 규율: 라인번호·변동 수치·색 hex를 문서에 적지 않는다.
- DI는 [ADR-0017](../../../adr/0017-remote-network-datasource.md)의 **역할당 1파일 평면 배치**를 따른다. 새 DI 파일을 만들지 않는다.
- ktlint 규칙이 강제된다(`./gradlew ktlintFormat`으로 자동 수정 가능).
- 서버 조회가 필요하면 `TEAMYG-SERVER`를 **read-only**로만 본다(`git -C <S> show origin/main:<path>`).

---

## File Structure

| 파일 | 책임 | 작업 |
|---|---|---|
| `data/.../service/model/response/ApiResponse.kt` | 서버 envelope 미러 | 수정 — 필드 2개 추가, `isSuccess` 제거 |
| `data/.../network/ApiCaller.kt` | 서비스 호출을 `Result`로 감싸고 예외를 `ApiException`으로 매핑 | **신규** |
| `data/.../network/SafeApiCall.kt` | (구) top-level 진입점 | **삭제** |
| `data/.../model/exception/ApiException.kt` | 실패 분류 | 수정 — `Business` 확장 |
| `data/.../source/temp/remote/TempRemoteDataSourceImpl.kt` | 예시 remote DataSource | 수정 — `ApiCaller` 주입으로 교체 |
| `data/.../security/CryptoManager.kt` | Keystore AES/GCM 암복호화. 저장 매체를 모른다 | **신규** |
| `data/.../source/token/local/TokenStore.kt` | 토큰 저장소 계약 | **신규** |
| `data/.../source/token/local/EncryptedTokenStore.kt` | 암호화 + DataStore 저장 | **신규** |
| `data/.../network/TokenStoreTokenProvider.kt` | 동기 `TokenProvider` 구현 | **신규** |
| `data/.../network/EmptyTokenProvider.kt` | stub 구현 | **삭제** |
| `data/.../di/NetworkModule.kt` | 네트워크 설정 DI | 수정 — `provideTokenProvider` 교체 |
| `data/.../di/LocalDataSourceModule.kt` | 로컬 저장소 바인딩 | 수정 — `TokenStore` `@Binds` 추가 |

패키지 루트는 `data/src/main/java/com/teamyg/parfait/data/`다.

---

### Task 1: `ApiResponse`를 서버 envelope와 맞추기

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/model/response/ApiResponse.kt`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `ApiResponse<T>(success: Boolean, code: String, message: String, data: T?, errorDetail: Map<String, String>?)`. **`isSuccess` 프로퍼티는 없다** — 소비자가 `response.success`를 직접 읽는다. Task 2가 이 형태에 의존한다.

- [x] **Step 1: 파일 전체를 아래로 교체**

`SUCCESS_CODE` 상수와 `isSuccess` 프로퍼티를 **함께 제거**한다. `isSuccess`가 `success`를 그대로 반환하는 껍데기가 되므로 남길 이유가 없다.

```kotlin
package com.teamyg.parfait.data.service.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T? = null,
    val errorDetail: Map<String, String>? = null,
)
```

- [x] **Step 2: 컴파일이 깨지는 지점 확인**

Run: `./gradlew :data:compileDebugKotlin`
Expected: **FAIL** — `SafeApiCall.kt`가 `response.isSuccess`를 참조하므로 `Unresolved reference: isSuccess`가 난다. 이는 Task 2가 `SafeApiCall.kt`를 삭제하며 해소된다. **여기서 고치지 마라.**

- [x] **Step 3: 필드가 서버와 일치하는지 대조**

Run: `git -C <S> show origin/main:common/src/main/kotlin/parfait/common/response/ApiResponse.kt`
Expected: `success`·`code`·`message`·`data`·`errorDetail` 5필드. 위 코드와 이름·널 허용이 같은지 확인한다.

---

### Task 2: `ApiCaller` 승격 + 에러 envelope 파싱

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/network/ApiCaller.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/network/SafeApiCall.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/model/exception/ApiException.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/temp/remote/TempRemoteDataSourceImpl.kt`

**Interfaces:**
- Consumes: Task 1의 `ApiResponse<T>`(`success` 프로퍼티, `isSuccess` 없음)
- Produces:
  - `ApiCaller` — `@Singleton class ApiCaller @Inject constructor(@RemoteJson json: Json)`. 메서드 셋:
    `suspend fun <T : Any> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T>` ·
    `suspend fun safeApiCallWithoutData(block: suspend () -> ApiResponse<Unit>): Result<Unit>` ·
    `suspend fun safeApiCallNoContent(block: suspend () -> Unit): Result<Unit>`
  - `ApiException.Business(code: String, serverMessage: String, statusCode: Int?, errorDetail: Map<String, String>?)`
  - remote DataSource는 `ApiCaller`를 **생성자 주입**으로 받는다(Task 5의 Hilt 그래프 검증이 이 배선을 확인한다)

- [x] **Step 1: `ApiException.Business` 확장**

`ApiException.kt`에서 `Business`만 아래로 교체한다. 나머지(`EmptyBody`·`Http`·`Network`·`Unknown`)는 건드리지 않는다.

```kotlin
    data class Business(
        val code: String,
        val serverMessage: String,
        val statusCode: Int?,
        val errorDetail: Map<String, String>?,
    ) : ApiException(serverMessage, null)
```

`statusCode`가 **nullable인 이유**: HTTP 상태를 알 수 있는 경로(`HttpException`)와 알 수 없는 경로(2xx 응답인데 envelope `success=false`)가 둘 다 있다. 후자는 현재 서버에 없는 경로지만 계약상 가능하므로 `null`로 정직하게 표현한다.

- [x] **Step 2: `ApiCaller.kt` 신규 작성**

```kotlin
package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class ApiCaller @Inject constructor(
    @RemoteJson private val json: Json,
) {
    suspend fun <T : Any> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> =
        runCatchingApi(block) { response ->
            response.data?.let { Result.success(it) }
                ?: Result.failure(ApiException.EmptyBody(response.code, response.message))
        }

    suspend fun safeApiCallWithoutData(block: suspend () -> ApiResponse<Unit>): Result<Unit> =
        runCatchingApi(block) { Result.success(Unit) }

    suspend fun safeApiCallNoContent(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(toApiException(e))
    } catch (e: IOException) {
        Result.failure(ApiException.Network(e))
    } catch (e: Exception) {
        Result.failure(ApiException.Unknown(e))
    }

    private suspend fun <T, R> runCatchingApi(
        block: suspend () -> ApiResponse<T>,
        onSuccess: (ApiResponse<T>) -> Result<R>,
    ): Result<R> = try {
        val response = block()
        if (response.success) {
            onSuccess(response)
        } else {
            Result.failure(
                ApiException.Business(
                    code = response.code,
                    serverMessage = response.message,
                    statusCode = null,
                    errorDetail = response.errorDetail,
                ),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(toApiException(e))
    } catch (e: IOException) {
        Result.failure(ApiException.Network(e))
    } catch (e: Exception) {
        Result.failure(ApiException.Unknown(e))
    }

    private fun toApiException(e: HttpException): ApiException {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        if (body.isNullOrBlank()) return ApiException.Http(e.code(), e)
        val envelope = runCatching {
            json.decodeFromString(ApiResponse.serializer(Unit.serializer()), body)
        }.getOrNull() ?: return ApiException.Http(e.code(), e)
        return ApiException.Business(
            code = envelope.code,
            serverMessage = envelope.message,
            statusCode = e.code(),
            errorDetail = envelope.errorDetail,
        )
    }
}
```

주의할 점 셋:

1. **`errorBody()`는 일회성 스트림이다.** `string()`을 두 번 호출하면 두 번째는 빈 값이다. 위 코드는 한 번만 읽어 `body` 지역 변수에 담는다.
2. **파싱 실패 폴백이 필수다.** 인프라 계층(게이트웨이·WAF)이 내는 429·502는 envelope 없이 올 수 있다. 서버가 항상 envelope를 준다고 가정하지 않는다.
3. `Unit.serializer()`는 `kotlinx.serialization.builtins`에서 온다. 에러 응답의 `data`는 항상 `null`이므로 타입 인자는 무엇이든 무방하고 `Unit`이 가장 가볍다.

- [x] **Step 3: `SafeApiCall.kt` 삭제**

```bash
rm <A>/data/src/main/java/com/teamyg/parfait/data/network/SafeApiCall.kt
```

- [x] **Step 4: `TempRemoteDataSourceImpl` 호출부 교체**

파일 전체를 아래로 교체한다. `import com.teamyg.parfait.data.network.safeApiCall`(top-level 함수 import)가 사라지고 `ApiCaller` 주입이 들어간다.

```kotlin
package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.TempService
import com.teamyg.parfait.data.source.temp.mapper.toTempVO
import com.teamyg.parfait.domain.model.TempVO
import javax.inject.Inject

class TempRemoteDataSourceImpl @Inject constructor(
    private val tempService: TempService,
    private val apiCaller: ApiCaller,
) : TempRemoteDataSource {
    override suspend fun getTemp(id: String): Result<TempVO> = apiCaller
        .safeApiCall { tempService.getTemp(id) }
        .map { it.toTempVO() }
}
```

- [x] **Step 5: 컴파일 + 린트**

Run:
```bash
cd <A> && ./gradlew :data:compileDebugKotlin ktlintCheck
```
Expected: BUILD SUCCESSFUL. Task 1에서 났던 `Unresolved reference: isSuccess`가 `SafeApiCall.kt` 삭제로 해소된다.

실패 시: `safeApiCall`을 top-level로 import하는 다른 파일이 남아 있는지 확인한다.
```bash
grep -rn "network.safeApiCall\|safeApiCallWithoutData" <A>/data/src/main --include=*.kt
```

---

### Task 3: `CryptoManager` — Keystore AES/GCM

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/security/CryptoManager.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `@Singleton class CryptoManager @Inject constructor()` — `fun encrypt(plainText: String): String` · `fun decrypt(encoded: String): String`. 두 메서드 모두 실패 시 **예외를 던진다**(Task 4가 잡는다). 반환·입력은 `IV + 암호문`을 이어붙인 Base64 문자열이다.

- [x] **Step 1: `CryptoManager.kt` 작성**

```kotlin
package com.teamyg.parfait.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val cipherText = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "parfait_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
    }
}
```

설계 근거 셋:

1. **GCM은 IV를 매번 새로 만들어야 한다.** 같은 키로 IV를 재사용하면 GCM의 보안이 무너진다. `Cipher`가 생성한 IV를 암호문 앞에 붙여 함께 저장하고, 복호화 때 잘라 쓴다. IV 자체는 비밀이 아니다.
2. **`ENCRYPTION_PADDING_NONE`** — GCM은 스트림 모드라 패딩이 필요 없다. `PKCS7`을 지정하면 `InvalidAlgorithmParameterException`이 난다.
3. **`android.util.Base64`**를 쓴다. `java.util.Base64`는 API 26+인데, 여기 minSdk가 26이라 쓸 수는 있으나 Android 표준 API를 따른다. `NO_WRAP`은 개행을 넣지 않아 한 줄 문자열로 저장된다.

- [x] **Step 2: 컴파일 + 린트**

Run: `cd <A> && ./gradlew :data:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 4: `TokenStore` + `EncryptedTokenStore`

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/token/local/TokenStore.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/token/local/EncryptedTokenStore.kt`

**Interfaces:**
- Consumes: Task 3의 `CryptoManager`(`encrypt`/`decrypt`, 실패 시 예외를 던짐), 기존 `DataStoreModule`이 제공하는 `DataStore<Preferences>`
- Produces: `interface TokenStore` — `suspend fun getAccessToken(): String?` · `suspend fun getRefreshToken(): String?` · `suspend fun save(accessToken: String, refreshToken: String)` · `suspend fun clear()`. 구현은 `EncryptedTokenStore`(`@Inject constructor`). Task 5가 `@Binds`로 묶고 `TokenStoreTokenProvider`가 소비한다.

- [x] **Step 1: `TokenStore.kt` 작성**

```kotlin
package com.teamyg.parfait.data.source.token.local

interface TokenStore {
    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun save(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clear()
}
```

- [x] **Step 2: `EncryptedTokenStore.kt` 작성**

```kotlin
package com.teamyg.parfait.data.source.token.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.security.CryptoManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EncryptedTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager,
) : TokenStore {
    override suspend fun getAccessToken(): String? = read(ACCESS_TOKEN_KEY)

    override suspend fun getRefreshToken(): String? = read(REFRESH_TOKEN_KEY)

    override suspend fun save(
        accessToken: String,
        refreshToken: String,
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = cryptoManager.encrypt(accessToken)
            preferences[REFRESH_TOKEN_KEY] = cryptoManager.encrypt(refreshToken)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }

    private suspend fun read(key: Preferences.Key<String>): String? {
        val encrypted = dataStore.data.first()[key] ?: return null
        return runCatching { cryptoManager.decrypt(encrypted) }
            .getOrElse {
                clear()
                null
            }
    }

    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
}
```

**`read`의 `getOrElse`가 이 Task의 핵심이다.** 기기 복원·잠금 자격증명 변경으로 Keystore 키가 무효화되면 `decrypt`가 예외를 던진다. 이걸 그대로 올려보내면 `TokenProvider.getToken()` → `AuthInterceptor.intercept`에서 터져 **모든 네트워크 요청이 죽고, 사용자는 앱을 삭제하기 전까지 복구할 수 없다.** 대신 저장된 값을 지우고 `null`을 반환해 "토큰 없음" 상태로 만들면 앱이 자연스럽게 재로그인 경로로 간다.

- [x] **Step 3: 컴파일 + 린트**

Run: `cd <A> && ./gradlew :data:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 5: `TokenStoreTokenProvider` + DI 배선 + Hilt 그래프 검증

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/network/TokenStoreTokenProvider.kt`
- Delete: `data/src/main/java/com/teamyg/parfait/data/network/EmptyTokenProvider.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 4의 `TokenStore`·`EncryptedTokenStore`, 기존 `TokenProvider` 인터페이스(`fun getToken(): String?`)
- Produces: 없음(마지막 코드 Task)

- [x] **Step 1: `TokenStoreTokenProvider.kt` 작성**

```kotlin
package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.source.token.local.TokenStore
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class TokenStoreTokenProvider @Inject constructor(
    private val tokenStore: TokenStore,
) : TokenProvider {
    override fun getToken(): String? = runBlocking { tokenStore.getAccessToken() }
}
```

**`runBlocking` 사용 근거(리뷰 대비 — 코드 주석이 아니라 이 계획서와 ADR에 남긴다):** OkHttp `Interceptor.intercept`는 동기 API라 suspend를 직접 호출할 수 없다. 이 코드는 OkHttp dispatcher 스레드에서 실행되므로 **메인 스레드를 막지 않는다.** 대안이던 "메모리 캐시(StateFlow) + 동기 읽기"는 앱 시작 직후 캐시가 비어 있는 창에서 첫 요청이 토큰 없이 나가는 타이밍 문제를 새로 만든다. `TokenProvider` 인터페이스를 유지하는 이유는 인터셉터 테스트 시 fake를 끼울 자리를 남기기 위해서다.

- [x] **Step 2: `EmptyTokenProvider.kt` 삭제**

```bash
rm <A>/data/src/main/java/com/teamyg/parfait/data/network/EmptyTokenProvider.kt
```

- [x] **Step 3: `NetworkModule`의 `provideTokenProvider` 교체**

`import com.teamyg.parfait.data.network.EmptyTokenProvider`를 `import com.teamyg.parfait.data.network.TokenStoreTokenProvider`로 바꾸고, 아래 함수를 교체한다. **나머지 provider 4개는 건드리지 않는다.**

교체 전:
```kotlin
    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = EmptyTokenProvider()
```

교체 후:
```kotlin
    @Provides
    @Singleton
    fun provideTokenProvider(tokenStoreTokenProvider: TokenStoreTokenProvider): TokenProvider = tokenStoreTokenProvider
```

`NetworkModule`은 `object`라 `@Binds`를 쓸 수 없다(`@Binds`는 추상 메서드가 필요하다). 그래서 `@Provides`가 구현체를 주입받아 그대로 반환한다.

- [x] **Step 4: `LocalDataSourceModule`에 `TokenStore` 바인딩 추가**

기존 `@Binds` 메서드들 **뒤에** 아래를 추가한다. import 2줄도 함께 넣는다(ktlint가 알파벳 순 정렬을 강제하지 않는다 — `.editorconfig`에서 import-ordering이 꺼져 있으나, 기존 파일 순서를 따라 넣는 편이 읽기 좋다).

```kotlin
import com.teamyg.parfait.data.source.token.local.EncryptedTokenStore
import com.teamyg.parfait.data.source.token.local.TokenStore
```

```kotlin
    @Binds
    @Singleton
    fun bindTokenStore(encryptedTokenStore: EncryptedTokenStore): TokenStore
```

- [x] **Step 5: `:data` 컴파일 + 린트**

Run: `cd <A> && ./gradlew :data:compileDebugKotlin ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 6: Hilt 그래프 전체 해소 검증**

Run: `cd <A> && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

이 단계가 확인하는 체인:
`CryptoManager` → `EncryptedTokenStore`(+ `DataStore<Preferences>`) → `TokenStore` → `TokenStoreTokenProvider` → `TokenProvider` → `AuthInterceptor` → `OkHttpClient` → `Retrofit`, 그리고 `@RemoteJson Json` → `ApiCaller` → `TempRemoteDataSourceImpl`.

실패 시 흔한 원인: `LocalDataSourceModule`의 `@Binds` 반환 타입이 인터페이스가 아님, `EncryptedTokenStore`에 `@Inject constructor`가 빠짐, `NetworkModule`에 `EmptyTokenProvider` import가 남음.

- [ ] **Step 7: 실기기 암복호화 왕복 검증 (사람이 수행)** — **미수행·이월**. `TokenStore.save()` 호출부가
  develop 기준 0건이라 아래 2번(저장 트리거)을 실행할 방법이 없다. 로그인 연동 라운드로 넘긴다
  → [open-questions](../../../synthesis/open-questions.md).

자동 테스트로 대체할 수 없다 — Android Keystore는 JVM 유닛 테스트에서 동작하지 않고, 이 저장소에는 계측 테스트 인프라가 없다.

절차:
1. 앱을 실기기에 설치·실행한다.
2. 디버거나 임시 진입점에서 `tokenStore.save("test-access", "test-refresh")`를 한 번 호출한다.
3. **앱을 완전히 종료**한다(최근 앱에서 스와이프 제거 — 프로세스가 살아 있으면 메모리 값을 읽어 검증이 무의미하다).
4. 재실행 후 `tokenStore.getAccessToken()`이 `"test-access"`를 돌려주는지 확인한다.
5. 확인 후 임시 진입점을 제거한다.

DataStore 파일을 직접 열어 값이 **평문이 아닌지**도 함께 본다(`adb shell run-as <패키지> cat /data/data/<패키지>/files/datastore/parfait_preferences.preferences_pb`). Base64 암호문이 보여야 한다.

**미검증으로 남는 것**: 키 유실 경로(`getOrElse` 분기). 기기 복원·잠금 변경을 재현해야 하는데 실기기에서 안전하게 만들기 어렵다. 다음 라운드로 넘긴다.

---

### Task 6: 문서 — ADR-0019 신설 · ADR-0017 갱신 · data-layer 반영

**Files:**
- Create: `parfait/adr/0019-encrypted-token-storage.md`
- Modify: `parfait/adr/0017-remote-network-datasource.md`
- Modify: `parfait/architecture/data-layer.md`
- Modify: `parfait/adr/README.md` (인덱스 1행)

**Interfaces:**
- Consumes: Task 1~5의 실제 산출물(as-built로 적는다 — 계획과 코드가 갈렸으면 **코드가 정답**이다)
- Produces: 없음(마지막 Task)

이 Task는 **문서 저장소(`team-yg-pesonal-agent`)에서** 수행한다. `TJYG-Android`가 아니다. 이 저장소의 커밋은 사용자가 승인했다.

- [x] **Step 1: ADR-0019 작성**

`parfait/adr/template.md` 형식을 따른다. 담을 것:

- **맥락**: `TokenProvider`가 stub이라 인증 API를 못 부른다. 토큰은 장기 자격증명(refresh 2주)이라 평문 저장이 부적절하다.
- **결정**: Android Keystore에 AES/GCM 키를 만들어(`CryptoManager`) 암호화하고, `IV + 암호문`을 Base64 한 문자열로 Preferences DataStore에 저장한다(`EncryptedTokenStore`). 키 별칭 1개. GCM IV는 매 암호화마다 새로 만든다.
- **키 유실 시 정책**: 복호화 예외를 전파하지 않고 `clear()` 후 `null` 반환 → 재로그인 유도. 전파하면 인터셉터에서 터져 전 요청이 죽는다.
- **대안 A — Tink**: 구글이 유지보수하는 암호 라이브러리라 직접 구현 실수를 피한다. **기각** — 의존성·APK 크기가 늘고 버전 카탈로그·컨벤션 플러그인 작업이 따른다. 현재 필요한 것은 문자열 두 개의 대칭 암호화뿐이다.
- **대안 B — EncryptedSharedPreferences**: 가장 적게 쓴다. **기각** — `androidx.security-crypto` 1.1.0이 alpha에서 오래 정체돼 있고, SharedPreferences라 [ADR-0008](../../../adr/0008-datastore-local-persistence.md)의 DataStore 관용과 어긋난다.
- **트레이드오프**: 암호화 로직을 직접 들고 있어야 한다. 키 유실 경로가 미검증으로 남는다(테스트 인프라 부재).

- [x] **Step 2: ADR-0017 갱신**

아래 4곳을 as-built로 고친다. **원문을 지우지 말고** 바뀐 부분에 갱신 표기를 남긴다(이 저장소의 ADR 관용).

1. **응답 계약** — 성공 판정 근거가 `code == SUCCESS_CODE`에서 **`success` 필드**로 바뀌었다. 서버가 성공 코드 2종(`"OK"`·`"CREATED"`)을 쓰므로 단일 상수 비교가 불가능했다. `isSuccess` 프로퍼티는 제거됐다.
2. **진입점** — 2개에서 **3개**로 늘었다(`safeApiCallNoContent` 신설). 근거: `logout`이 204라 응답 본문 자체가 없어 `safeApiCallWithoutData`로 처리할 수 없다.
3. **`safeApiCall`이 top-level 함수에서 `ApiCaller` 클래스로 승격**됐다. 근거: 에러 envelope 역직렬화에 `@RemoteJson`이 필요한데, top-level이면 호출부마다 `Json`을 넘겨야 하고 파일 내 `private val`은 `@LocalJson`/`@RemoteJson` 분리를 무의미하게 만든다.
4. **에러 타입 계층** — `Business`에 `statusCode: Int?`·`errorDetail`이 추가됐다. 근거: 코드 문자열이 enum 간 유일하지 않다(`MEMBER_NOT_FOUND`가 401/404 둘 다). 그리고 **에러가 HTTP 4xx/5xx로 오므로 `HttpException` 바디를 파싱해야 envelope에 도달한다** — 이걸 안 하면 `Business`가 죽은 분기가 된다.
5. **인증** — `EmptyTokenProvider`가 `TokenStoreTokenProvider`로 교체됐다. 상세는 ADR-0019.

- [x] **Step 3: `architecture/data-layer.md` 반영**

- 토큰 저장 경로 추가: `CryptoManager` → `EncryptedTokenStore` → `TokenStore` → `TokenStoreTokenProvider` → `AuthInterceptor`
- remote DataSource 작성 체크리스트에 **`ApiCaller` 생성자 주입**을 명시(top-level `safeApiCall` import는 더 이상 없다)
- 진입점 3개와 각각을 언제 쓰는지(payload 필요 / envelope만 / 본문 없음)

- [x] **Step 4: `parfait/adr/README.md` 인덱스에 ADR-0019 한 줄 등록**

- [x] **Step 5: 링크 검증**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
for f in parfait/adr/0019-encrypted-token-storage.md parfait/adr/0017-remote-network-datasource.md parfait/architecture/data-layer.md; do
  d=$(dirname "$f")
  grep -oE '\]\(([^)]+\.md)[^)]*\)' "$f" | sed -E 's/^\]\(//; s/[)#].*$//' | while read -r l; do
    case "$l" in http*) continue;; esac
    [ -e "$d/$l" ] || echo "BROKEN: $f -> $l"
  done
done
```
Expected: 출력 없음

- [x] **Step 6: 문서 저장소 커밋 (사용자 확인 후)**

```bash
git add parfait/adr parfait/architecture/data-layer.md parfait/specs parfait/plans
git commit -m "docs(parfait): ADR-0019 토큰 암호화 저장 + ADR-0017 갱신"
```

**`TJYG-Android`는 커밋하지 않는다**(Global Constraints).

---

## 완료 기준

- `:data:compileDebugKotlin` · `ktlintCheck` · `:app:assembleDebug` 전부 통과
- `SafeApiCall.kt`·`EmptyTokenProvider.kt`가 삭제되고 참조가 0건
- ~~실기기에서 암복호화 왕복 확인 + DataStore 파일에 평문이 없음을 확인~~ — **미수행·이월**(Step 7 참고)
- ADR-0019 신설, ADR-0017 5개 항목 갱신, data-layer 반영, README 등록
- ~~**`TJYG-Android`에 커밋이 없다** — 작업 트리 변경만 남는다~~ — **as-built: 사용자가 직접 커밋·PR #190으로 develop 머지**(2026-08-04)

## 이 계획이 하지 않는 것

- auth 도메인의 Service·Response·RemoteDataSource·Repository 구현
- 401 자동 재발급(`Authenticator`) — 토큰 저장이 선행돼야 설계 가능하고, `reissue`가 화이트리스트라 인증 헤더를 받지 않는다는 점이 설계에 영향을 준다
- 화면·카카오 SDK·`nonce` 생성
- `TempService`·`TempVO` 등 예시 세트 제거 — 실제 API가 들어올 때 함께 정리
- 키 유실 경로 검증
