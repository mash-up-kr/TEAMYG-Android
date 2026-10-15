# 유저 정보 로컬 SSoT · 자동로그인 부트스트랩 Implementation Plan

> ✅ **완료·develop 머지(PR #263 `2143c229`, 2026-08-16)** — 8 Task 전량이 develop에 있다.
> 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> **계획이 코드와 갈린 곳 4건**(구현 중 확정분은 각 Task 안의 ✅ 블록에 이미 적혀 있다):
> ① 계획의 `ObserveMyAccountUseCase`가 **`GetMyAccountFlowUseCase`**로 개명됐다 — 호출이 구독하지 않고
> `Flow`만 넘기므로 같은 형태의 선례(`GetRecentCacheImagesUseCase`)와 접두사를 맞추고, 일회성
> `RefreshMyAccountUseCase`와 구분하려 `Flow`를 이름에 남겼다.
> ② 계획에 없던 **`EncryptedPreferences`(`data/datastore/`)를 뽑았다.** Task 1이 그리던 "저장소가
> `CryptoManager`+DataStore를 직접 감싼다"를 두 저장소가 그대로 반복하게 두지 않고, 암호화·복호화·
> 손상분 폐기를 프록시에 모으고 `EncryptedTokenStore`까지 옮겨 태웠다. 프록시가 **암호문 상태에서
> `distinctUntilChanged`**를 걸어, 토큰 재발급 저장이 계정 정보 구독자(편집 중 입력 필드)를 흔들던
> 경로를 막는다 — 계획에 없던 성질이고 Task 8의 "편집 중 되돌림" 방어의 실제 근거다.
> ③ Task 2의 `changeGlobalNickname`이 **로컬이 비어 있을 때 재조회로 폴백**한다. 계획은 "응답 값으로
> 로컬 닉네임만 갱신"이었으나 로컬이 `null`이면 `memberId`·provider가 없어 VO를 세울 수 없다.
> 폴백 결과는 무시한다(변경 자체는 이미 성공했다).
> ④ Task 5의 부트스트랩이 토큰·계정 정보를 **직접 지우지 않고 `LogoutUseCase`에 위임**한다. 지울
> 대상이 사용자 로그아웃과 같아 "무엇을 지우는가"를 한 자리에 둔다.
>
> **테스트**: 저장소 전역 358 → **415건**(테스트 파일 40 → 47). 계획이 예상한 약 20건보다 크게
> 늘었다 — `EncryptedPreferences`·`TokenAuthenticator` 회귀와 `AccountInfoViewModelTest`의 편집 세션
> 케이스가 계획 밖에서 붙었다. **수동 확인 7항목은 미수행**이다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `users/me`가 주는 계정 정보를 로컬에 한 벌만 두고 화면이 그것을 구독하게 하며, 스플래시가 토큰으로 세션을 복원해 자동로그인시킨다.

**Architecture:** `:data`의 암호화 DataStore가 계정 정보를 소유하고 `MemberRepository`가 remote↔local을 조율한다. 화면은 `Flow`를 구독하기만 하고 조회 API를 직접 부르지 않는다. 서버 조회는 로그인 직후·앱 진입·닉네임 변경 성공 세 시점뿐이다. 계정 정보는 토큰과 수명을 같이 해 두 로그아웃 경로에서 함께 지워진다.

**Tech Stack:** Kotlin, Preferences DataStore, kotlinx.serialization(`@LocalJson`), `CryptoManager`(Android Keystore AES/GCM), Hilt, Coroutines Flow, MockK, Turbine, kotlin.test

**Spec:** [`parfait/specs/2026-08-15-user-info-ssot.md`](../../specs/archive/2026-08-15-user-info-ssot.md) · 대응 ADR [`parfait/adr/0022-user-info-local-ssot.md`](../../../adr/0022-user-info-local-ssot.md)

## Global Constraints

- **작업 저장소는 `TJYG-Android`**(이 문서가 있는 repo가 아니다).
- **선행 필수** — 이 계획은 `feature/session-token-refresh-infra`(세션 인프라)가 만든 `LogoutUseCase`·`TokenAuthenticator`·`SessionEventBus`에 의존한다. 그 브랜치가 `develop`에 머지된 뒤 `develop`에서 브랜치를 딴다. 아직 머지 전이면 그 브랜치 위에 쌓는다.
- **워크트리를 만들지 않는다.** 본 체크아웃에서 `git checkout -b`로 브랜치를 만들어 작업한다.
- **커밋은 사용자 지시가 있을 때만 한다.** 각 Task의 커밋 스텝은 지시가 있을 때 실행한다. `git push`·PR 생성은 별도 승인 사항이며 이 계획에 포함되지 않는다.
- `:domain`은 Android·kotlinx.serialization·DataStore를 참조하지 않는다(ADR-0001·0011). 저장 모델과 매퍼는 `:data`에 둔다.
- 실패는 Repository 경계에서 `AppError`로 바꾼다(ADR-0020) — `mapErrorToAppError()` 사용.
- 표시 문자열 매핑은 `core:ui`가 소유한다(ADR-0016). domain은 의미만 돌려준다.
- 테스트는 Given-When-Then 주석을 단다. 테스트 함수명은 `대상_조건_기대` 형식.
- 검증 명령: `./gradlew :data:testDebugUnitTest`, `:domain:testDebugUnitTest`, `:feature:app:setting:impl:testDebugUnitTest`, `:feature:intro:impl:testDebugUnitTest`. 전체는 `./gradlew testDebugUnitTest`. DI 확인은 `./gradlew :app:kspDebugKotlin`, 최종은 `:app:assembleDebug`.
- ktlint: `./gradlew ktlintCheck`.
- **매퍼 단독 테스트를 만들지 않는다**(저장소 규약). 변환 판단은 그것을 쓰는 DataSource·Repository 테스트의 케이스로 넣는다.

---

## 파일 구성

| 파일 | 책임 | 신규/수정 |
|---|---|---|
| `data/model/local/UserInfoEntity.kt` | `@Serializable` 저장 모델 + `MyAccountVO` 양방향 매퍼 | 신규 |
| `data/source/member/local/UserInfoLocalDataSource.kt` | 인터페이스 | 신규 |
| `data/source/member/local/UserInfoLocalDataSourceImpl.kt` | DataStore·암호화·`Flow` | 신규 |
| `data/repository/member/MemberRepositoryImpl.kt` | remote↔local 조율, `AppError` 변환 | 신규 |
| `domain/repository/member/MemberRepository.kt` | 읽기·갱신·변경·정리 | 신규 |
| `domain/usecase/member/ObserveMyAccountUseCase.kt` | 신규 |
| `domain/usecase/member/RefreshMyAccountUseCase.kt` | 신규 |
| `domain/usecase/member/ChangeGlobalNicknameUseCase.kt` | 신규 |
| `domain/model/session/SessionBootstrap.kt` | 부트스트랩 결과 sealed | 신규 |
| `domain/usecase/session/BootstrapSessionUseCase.kt` | 토큰 유무 → 조회 → 목적지 | 신규 |
| `domain/repository/auth/AuthRepository.kt` | `hasSession()` 추가 | 수정 |
| `data/repository/auth/AuthRepositoryImpl.kt` | `hasSession()` 구현 | 수정 |
| `domain/usecase/auth/LogoutUseCase.kt` | `clearMyAccount()` 추가 | 수정 |
| `data/network/TokenAuthenticator.kt` | 세션 폐기 시 userInfo clear | 수정 |
| `domain/usecase/auth/LoginWithKakaoUseCase.kt`·`SignUpUseCase.kt` | 성공 직후 refresh | 수정 |
| `core/ui/.../text/LoginProviderUiText.kt` + `strings.xml` | provider 표시 문구 | 신규/수정 |
| `feature/intro/impl/.../splash/SplashViewModel.kt`·`SplashRoute.kt` | 부트스트랩 분기 | 수정 |
| `feature/app/setting/impl/.../AppSettingViewModel.kt` | mock 제거·구독 | 수정 |
| `feature/app/setting/impl/.../AccountInfoViewModel.kt` | mock 제거·구독·변경 결선 | 수정 |
| `feature/app/setting/impl/.../screen/*.kt` | `null`(로딩) 상태 처리 | 수정 |

`MemberRemoteDataSource.getMyAccount()`·`changeGlobalNickname()`은 **이미 있다** — 새로 만들지 않는다.

**Task 순서의 이유** — 저장(1) → 조율(2) → 도메인 진입점(3)까지가 아래층이고, 그 위에 세션 정리(4)·부트스트랩(5)·화면(6~8)이 얹힌다. 4를 5보다 먼저 두는 이유는 부트스트랩 실패 경로가 정리 동작을 호출하기 때문이다.

---

### Task 1: 저장 모델과 로컬 저장소

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/model/local/UserInfoEntity.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/member/local/UserInfoLocalDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/member/local/UserInfoLocalDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/DataSourceModule.kt` (바인딩 추가 — 실제 모듈 파일명은 저장소에서 확인)
- Test: `data/src/test/java/com/teamyg/parfait/data/source/member/local/UserInfoLocalDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `MyAccountVO(memberId: MemberId, provider: LoginProvider, nickname: GlobalNickname)`, `CryptoManager.encrypt(String): String` / `.decrypt(String): String`, `DataStore<Preferences>`, `@LocalJson Json`
- Produces: `UserInfoLocalDataSource` — `val myAccount: Flow<MyAccountVO?>`, `suspend fun save(account: MyAccountVO)`, `suspend fun clear()`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`UserInfoLocalDataSourceImplTest.kt`. `EncryptedTokenStoreTest`가 `CryptoManager`를 어떻게 다루는지 먼저 읽고 같은 방식을 따른다(Keystore가 JVM에서 안 돌아 페이크·목이 필요하다).

```kotlin
package com.teamyg.parfait.data.source.member.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.teamyg.parfait.data.security.CryptoManager
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserInfoLocalDataSourceImplTest {
    /** Keystore 를 요구하지 않는 통과 암호화 — 저장 왕복만 검증하면 되므로 변환하지 않는다 */
    private val passthroughCrypto: CryptoManager = mockk {
        every { encrypt(any()) } answers { firstArg() }
        every { decrypt(any()) } answers { firstArg() }
    }

    private fun dataSource(
        dataStore: DataStore<Preferences>,
        crypto: CryptoManager = passthroughCrypto,
    ) = UserInfoLocalDataSourceImpl(
        dataStore = dataStore,
        json = Json { ignoreUnknownKeys = true },
        cryptoManager = crypto,
    )

    @Test
    fun save_thenRead_roundTripsEveryField() = runTest {
        // Given 값 클래스와 enum 을 품은 계정 정보
        val dataStore = FakePreferencesDataStore()
        val source = dataSource(dataStore)
        val account = MyAccountVO(
            memberId = MemberId(7L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )

        // When 저장하고 다시 읽는다
        source.save(account)

        // Then 필드가 하나도 뒤바뀌지 않는다 — memberId 와 nickname 은 타입이 달라도
        // 매퍼가 뒤집히면 컴파일러가 막지 못한다
        assertEquals(account, source.myAccount.first())
    }

    @Test
    fun myAccount_nothingSaved_isNull() = runTest {
        // Given 저장분이 없는 상태
        val source = dataSource(FakePreferencesDataStore())

        // When 읽는다
        // Then 빈 값이 아니라 null 이다 — 화면이 "아직 없음"을 로딩으로 구분해야 한다
        assertNull(source.myAccount.first())
    }

    @Test
    fun myAccount_decryptFails_isNullAndDiscardsStoredValue() = runTest {
        // Given 저장 후 키가 바뀌어 복호화가 실패하는 상태
        val dataStore = FakePreferencesDataStore()
        val failingCrypto: CryptoManager = mockk {
            every { encrypt(any()) } answers { firstArg() }
            every { decrypt(any()) } throws IllegalStateException("키 유실")
        }
        dataSource(dataStore).save(
            MyAccountVO(MemberId(7L), LoginProvider.KAKAO, GlobalNickname("모카")),
        )

        // When 복호화가 실패하는 저장소로 읽는다
        val read = dataSource(dataStore, failingCrypto).myAccount.first()

        // Then null 이고 저장분은 버려진다 — 영구히 못 읽는 값을 들고 있지 않는다
        assertNull(read)
        assertNull(dataSource(dataStore).myAccount.first())
    }

    @Test
    fun myAccount_storedProviderUnknownToApp_fallsBackToUnknown() = runTest {
        // Given 앱이 모르는 provider 문자열이 저장돼 있다(서버가 provider 를 늘린 뒤)
        val dataStore = FakePreferencesDataStore()
        dataStore.putRaw(
            key = UserInfoLocalDataSourceImpl.USER_INFO_KEY_NAME,
            value = """{"memberId":7,"provider":"GOOGLE","nickname":"모카"}""",
        )

        // When 읽는다
        val read = dataSource(dataStore).myAccount.first()

        // Then 크래시하지 않고 UNKNOWN 으로 떨어진다
        assertEquals(LoginProvider.UNKNOWN, read?.provider)
    }
}
```

`FakePreferencesDataStore`는 이 파일 안에 둔다 — 메모리 `MutableStateFlow<Preferences>`로 `DataStore<Preferences>`를 구현하고, `putRaw`는 테스트가 저장 형태를 직접 심기 위한 헬퍼다. `EncryptedTokenStoreTest`가 이미 같은 필요를 어떻게 풀었는지 먼저 보고, **선례가 있으면 그것을 재사용한다.**

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*UserInfoLocalDataSourceImplTest*'`
Expected: 컴파일 실패 — `Unresolved reference: UserInfoLocalDataSourceImpl`

- [ ] **Step 3: 저장 모델·매퍼를 쓴다**

`UserInfoEntity.kt`:

```kotlin
package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.serialization.Serializable

/**
 * 계정 정보의 저장 형태.
 *
 * [MyAccountVO] 를 그대로 직렬화하지 않는 이유: 값 클래스 둘과 enum 하나를 품고 있어
 * 직렬화기가 다루지 못하고, domain 이 kotlinx.serialization 을 알게 되면 ADR-0001 의
 * 단방향 의존이 깨진다.
 */
@Serializable
internal data class UserInfoEntity(
    val memberId: Long,
    val provider: String,
    val nickname: String,
)

internal fun MyAccountVO.toEntity(): UserInfoEntity = UserInfoEntity(
    memberId = memberId.value,
    provider = provider.name,
    nickname = nickname.value,
)

/** 저장 당시와 앱의 [LoginProvider] 목록이 다를 수 있어 알 수 없는 값은 UNKNOWN 으로 떨어뜨린다 */
internal fun UserInfoEntity.toVO(): MyAccountVO = MyAccountVO(
    memberId = MemberId(memberId),
    provider = LoginProvider.entries.firstOrNull { it.name == provider } ?: LoginProvider.UNKNOWN,
    nickname = GlobalNickname(nickname),
)
```

`UserInfoLocalDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.member.local

import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.coroutines.flow.Flow

interface UserInfoLocalDataSource {
    /** 저장된 계정 정보. 없거나 복호화에 실패하면 `null` */
    val myAccount: Flow<MyAccountVO?>

    suspend fun save(account: MyAccountVO)

    suspend fun clear()
}
```

`UserInfoLocalDataSourceImpl.kt` — `EncryptedTokenStore`의 구조를 따른다. 특히 **`runCatching`이 아니라 `runSuspendCatching`**을 쓴다(stdlib 판으로 감싸면 취소가 `null`로 둔갑해 "저장분 없음"으로 보고된다). 읽기가 실패하면 `clear()` 후 `null`.

```kotlin
@Singleton
class UserInfoLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @LocalJson private val json: Json,
    private val cryptoManager: CryptoManager,
) : UserInfoLocalDataSource {
    override val myAccount: Flow<MyAccountVO?> = dataStore.data.map { preferences ->
        decode(preferences[USER_INFO_KEY])
    }

    override suspend fun save(account: MyAccountVO) {
        val encoded = json.encodeToString(account.toEntity())
        dataStore.edit { preferences ->
            preferences[USER_INFO_KEY] = cryptoManager.encrypt(encoded)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(USER_INFO_KEY) }
    }

    private suspend fun decode(stored: String?): MyAccountVO? { /* 아래 규칙대로 */ }

    internal companion object {
        const val USER_INFO_KEY_NAME = "user_info"
        val USER_INFO_KEY = stringPreferencesKey(USER_INFO_KEY_NAME)
    }
}
```

`decode`가 지켜야 할 것 — 저장분이 없으면 `null`, 복호화·역직렬화 실패면 `clear()` 후 `null`.
`myAccount`가 `Flow`이므로 `clear()`를 어디서 부를지 주의한다: `map` 안에서 suspend 정리를 호출할 수 없으면 읽기 전용 폴백만 하고, 폐기는 `save`/`refresh` 경로에서 처리하는 형태로 바꿔도 된다 — **테스트가 요구하는 것은 "실패 후 다시 읽어도 `null`"이므로 그 조건만 만족하면 형태는 구현자가 정한다.** 정한 형태를 report에 남긴다.

DI 바인딩은 저장소의 기존 DataSource 바인딩 모듈 관례를 따른다(`@Binds` 인터페이스 모듈).

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*UserInfoLocalDataSourceImplTest*'`
Expected: PASS 4건

- [ ] **Step 5: 그물을 확인한다**

`toVO()`의 `memberId`와 `nickname`을 서로 바꿔 넣어 보고 `save_thenRead_roundTripsEveryField`가 실패하는지 본다. 확인 후 되돌린다. 실패 출력을 report에 남긴다.

- [ ] **Step 6: 커밋** (지시가 있을 때만)

```bash
git add data/src/main/java/com/teamyg/parfait/data/model/local/UserInfoEntity.kt \
        data/src/main/java/com/teamyg/parfait/data/source/member/local/ \
        data/src/main/java/com/teamyg/parfait/data/di/ \
        data/src/test/java/com/teamyg/parfait/data/source/member/local/
git commit -m "feat(member): 계정 정보 로컬 저장소 — 암호화 DataStore"
```

---

### Task 2: MemberRepository

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/member/MemberRepository.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/member/MemberRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/member/MemberRepositoryImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 `UserInfoLocalDataSource`, 기존 `MemberRemoteDataSource.getMyAccount(): Result<MyAccountVO>` / `.changeGlobalNickname(GlobalNickname): Result<GlobalNickname>`, `mapErrorToAppError()`
- Produces: `MemberRepository` — `val myAccount: Flow<MyAccountVO?>`, `suspend fun refreshMyAccount(): Result<MyAccountVO>`, `suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>`, `suspend fun clearMyAccount()`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`MemberRepositoryImplTest.kt` — 기존 `ParfaitGroupRepositoryImplTest`의 목 구성 방식을 따른다.

```kotlin
    @Test
    fun refreshMyAccount_succeeds_savesToLocal() = runTest {
        // Given 서버가 계정 정보를 준다
        coEvery { remoteDataSource.getMyAccount() } returns Result.success(ACCOUNT)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 갱신한다
        val result = repository.refreshMyAccount()

        // Then 반환값과 저장값이 같다
        assertEquals(ACCOUNT, result.getOrNull())
        coVerify(exactly = 1) { localDataSource.save(ACCOUNT) }
    }

    @Test
    fun refreshMyAccount_fails_keepsLocalUntouched() = runTest {
        // Given 서버 조회가 실패한다
        coEvery { remoteDataSource.getMyAccount() } returns
            Result.failure(ApiException.Network(IOException("연결 실패")))

        // When 갱신한다
        val result = repository.refreshMyAccount()

        // Then 저장소를 건드리지 않는다 — 낡은 값이라도 지우지 않는다.
        // 지우면 오프라인에서 화면이 빈다
        assertTrue(result.isFailure)
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { localDataSource.save(any()) }
        coVerify(exactly = 0) { localDataSource.clear() }
    }

    @Test
    fun changeGlobalNickname_succeeds_updatesOnlyNickname() = runTest {
        // Given 저장된 계정 정보가 있고 서버가 새 닉네임을 확인해 준다
        every { localDataSource.myAccount } returns flowOf(ACCOUNT)
        coEvery { remoteDataSource.changeGlobalNickname(NEW_NICKNAME) } returns
            Result.success(NEW_NICKNAME)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 닉네임을 바꾼다
        val result = repository.changeGlobalNickname(NEW_NICKNAME)

        // Then 닉네임만 바뀌고 memberId·provider 는 그대로다
        assertEquals(NEW_NICKNAME, result.getOrNull())
        coVerify(exactly = 1) {
            localDataSource.save(ACCOUNT.copy(nickname = NEW_NICKNAME))
        }
    }

    @Test
    fun changeGlobalNickname_fails_leavesLocalUntouched() = runTest {
        // Given 서버가 거절한다
        every { localDataSource.myAccount } returns flowOf(ACCOUNT)
        coEvery { remoteDataSource.changeGlobalNickname(any()) } returns
            Result.failure(ApiException.Business(code = "INVALID_NICKNAME", statusCode = 400, serverMessage = "…", errorDetail = null))

        // When 닉네임을 바꾼다
        val result = repository.changeGlobalNickname(NEW_NICKNAME)

        // Then 낙관적 갱신을 하지 않는다 — 실패했는데 다른 화면에 새 이름이 보이면 안 된다
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { localDataSource.save(any()) }
    }
```

`ApiException.Business`의 실제 생성자 파라미터는 저장소에서 확인해 맞춘다.

- [ ] **Step 2: 실패 확인** — `./gradlew :data:testDebugUnitTest --tests '*MemberRepositoryImplTest*'`

- [ ] **Step 3: 구현한다**

```kotlin
// domain
interface MemberRepository {
    val myAccount: Flow<MyAccountVO?>
    suspend fun refreshMyAccount(): Result<MyAccountVO>
    suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>
    suspend fun clearMyAccount()
}
```

```kotlin
// data
class MemberRepositoryImpl @Inject constructor(
    private val remoteDataSource: MemberRemoteDataSource,
    private val localDataSource: UserInfoLocalDataSource,
) : MemberRepository {
    override val myAccount: Flow<MyAccountVO?> = localDataSource.myAccount

    override suspend fun refreshMyAccount(): Result<MyAccountVO> = remoteDataSource
        .getMyAccount()
        .onSuccess { account -> localDataSource.save(account) }
        .mapErrorToAppError()

    /**
     * 성공 응답을 받은 뒤에 로컬을 갱신한다(낙관적 갱신 안 함) — 실패했는데 다른 화면에
     * 새 닉네임이 보이는 것이 되돌리는 것보다 나쁘다.
     */
    override suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname> =
        remoteDataSource
            .changeGlobalNickname(nickname)
            .onSuccess { changed ->
                localDataSource.myAccount.first()?.let { current ->
                    localDataSource.save(current.copy(nickname = changed))
                }
            }.mapErrorToAppError()

    override suspend fun clearMyAccount() = localDataSource.clear()
}
```

- [ ] **Step 4: 통과 확인** — PASS 4건
- [ ] **Step 5: 커밋** (지시가 있을 때만) — `feat(member): MemberRepository — remote·local 조율`

---

### Task 3: UseCase 3종

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/member/ObserveMyAccountUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/member/RefreshMyAccountUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/member/ChangeGlobalNicknameUseCase.kt`

**Interfaces:**
- Consumes: Task 2의 `MemberRepository`
- Produces: `ObserveMyAccountUseCase.invoke(): Flow<MyAccountVO?>`, `RefreshMyAccountUseCase.invoke(): Result<MyAccountVO>`, `ChangeGlobalNicknameUseCase.invoke(nickname: GlobalNickname): Result<GlobalNickname>`

세 파일 모두 저장소의 UseCase 관례(ADR-0009: 주입 클래스 + `operator invoke`, 인터페이스 없음)를 따르는 위임 한 줄이다. **자체 테스트를 만들지 않는다** — 판단이 없는 위임이고, 소비처 테스트가 덮는다.

- [ ] **Step 1: 세 파일을 쓴다**

```kotlin
class ObserveMyAccountUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
) {
    operator fun invoke(): Flow<MyAccountVO?> = memberRepository.myAccount
}
```

나머지 둘도 같은 형태로, `RefreshMyAccountUseCase`는 `suspend operator fun invoke(): Result<MyAccountVO>`, `ChangeGlobalNicknameUseCase`는 `suspend operator fun invoke(nickname: GlobalNickname): Result<GlobalNickname>`.

- [ ] **Step 2: 컴파일 확인** — `./gradlew :domain:compileDebugKotlin`
- [ ] **Step 3: 커밋** (지시가 있을 때만) — `feat(member): 계정 정보 UseCase 3종`

---

### Task 4: 세션 정리에 userInfo 포함

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCaseTest.kt` (신규)
- Test: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt` (기존에 추가)

**Interfaces:**
- Consumes: Task 2의 `MemberRepository.clearMyAccount()`, Task 1의 `UserInfoLocalDataSource.clear()`, 세션 인프라의 `LogoutUseCase`·`TokenAuthenticator`
- Produces: 없음

**두 경로가 있고 지우는 주체가 다르다.** 사용자 로그아웃은 UseCase가 조율하고, 강제 로그아웃은 `TokenAuthenticator`가 토큰을 지우는 그 자리에서 함께 지운다. 후자를 `:data` 안에서 끝내는 이유는 이벤트가 유실돼도 토큰과 userInfo가 갈라지지 않게 하기 위해서다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`LogoutUseCaseTest.kt`:

```kotlin
    @Test
    fun invoke_always_clearsTokensAndAccount() = runTest {
        // Given 로그인 상태
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { memberRepository.clearMyAccount() } returns Unit

        // When 로그아웃한다
        logout()

        // Then 토큰과 계정 정보가 둘 다 지워진다 — 하나만 지우면 계정 전환 시 이전
        // 사용자 정보가 남는다
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }

    @Test
    fun invoke_serverLogoutFails_stillClearsAccount() = runTest {
        // Given 서버 로그아웃이 실패한다(AuthRepository 는 그래도 성공을 돌려준다)
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { memberRepository.clearMyAccount() } returns Unit

        // When 로그아웃한다
        logout()

        // Then 계정 정보 정리는 서버 결과와 무관하게 일어난다
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }
```

`TokenAuthenticatorTest`에 추가:

```kotlin
    @Test
    fun authenticate_reissueRejected_clearsUserInfoTogether() = runTest {
        // Given 서버가 refresh token 을 거절한다
        server.enqueue(
            MockResponse.Builder().code(401)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"…","data":null}""")
                .build(),
        )

        // When 인증기가 응답을 받는다
        authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 토큰과 함께 계정 정보도 지워진다 — 이벤트가 유실돼도 둘이 갈라지면 안 된다
        assertEquals(1, tokenStore.clearCount)
        coVerify(exactly = 1) { userInfoLocalDataSource.clear() }
    }
```

기존 `TokenAuthenticatorTest`의 생성자 호출부 전부에 새 의존을 넘겨야 하므로 헬퍼를 고친다. **기존 9건이 전부 통과해야 한다.**

- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현한다** — `LogoutUseCase`에 `MemberRepository` 주입 후 `clearMyAccount()` 호출, `TokenAuthenticator`에 `UserInfoLocalDataSource` 주입 후 세션 폐기 분기에서 `clear()` 호출
- [ ] **Step 4: 통과 확인** — `:domain` 신규 2건 + `:data` `TokenAuthenticatorTest` 10건
- [ ] **Step 5: 커밋** (지시가 있을 때만) — `feat(session): 세션 정리에 계정 정보 포함`

---

### Task 5: 부트스트랩

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/session/SessionBootstrap.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/session/BootstrapSessionUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/auth/AuthRepository.kt` (`hasSession()`)
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImpl.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/session/BootstrapSessionUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 2의 `MemberRepository.refreshMyAccount()`·`clearMyAccount()`, `AuthRepository.hasSession()`·`logout()`
- Produces: `SessionBootstrap.{ToGroupList, ToLogin}`, `BootstrapSessionUseCase.invoke(): SessionBootstrap`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun invoke_noToken_goesToLoginWithoutServerCall() = runTest {
        // Given 저장된 토큰이 없다
        coEvery { authRepository.hasSession() } returns false

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 서버를 부르지 않고 로그인으로 간다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 0) { memberRepository.refreshMyAccount() }
    }

    @Test
    fun invoke_tokenAndRefreshSucceeds_goesToGroupList() = runTest {
        // Given 토큰이 있고 조회가 성공한다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns Result.success(ACCOUNT)

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 그룹 목록으로 간다 — SSoT 가 채워진 상태다
        assertEquals(SessionBootstrap.ToGroupList, result)
    }

    @Test
    fun invoke_tokenButRefreshFails_clearsSessionAndGoesToLogin() = runTest {
        // Given 토큰은 있으나 조회가 실패한다
        coEvery { authRepository.hasSession() } returns true
        coEvery { memberRepository.refreshMyAccount() } returns
            Result.failure(AppError.Network(cause = null))
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { memberRepository.clearMyAccount() } returns Unit

        // When 부트스트랩한다
        val result = bootstrap()

        // Then 로그인으로 보내고 정리한다
        assertEquals(SessionBootstrap.ToLogin, result)
        coVerify(exactly = 1) { memberRepository.clearMyAccount() }
    }
```

> ⚠️ 세 번째 테스트는 **스펙의 열린 질문과 맞닿아 있다.** 스펙은 네트워크 실패도 `ToLogin`으로
> 보내되 **토큰은 지우지 않는다**고 적었다(연결이 돌아오면 자동로그인이 성립해야 하므로).
> 위 테스트는 `clearMyAccount()`만 단언하고 토큰 정리는 단언하지 않는다. 구현자는 스펙 본문의
> 부트스트랩 절을 읽고 **실패 종류에 따라 정리 범위가 갈리는지**를 확정한 뒤, 확정한 규칙을
> 테스트로 고정하고 report에 적는다. 판단이 서지 않으면 BLOCKED로 보고한다.
>
> ✅ **확정됨(2026-08-15, 구현 중)** — 세션을 파기하는 것은 **인증 거절(HTTP 401 ·
> `MEMBER_NOT_FOUND`)뿐**이다. 네트워크 실패·5xx·로컬 저장 실패는 아무것도 지우지 않고
> `ToLogin` 라우팅만 한다. 처음엔 "네트워크 실패만 예외"로 좁혔다가, 최종 리뷰에서 5xx가
> `AppError.Unexpected`로 떨어져 **서버 배포 중에 전 사용자가 로그아웃**되고 서버가 200을 준 뒤
> 로컬 쓰기가 실패해도 멀쩡한 세션이 죽는다는 것이 드러나 인증 거절로 더 좁혔다. 그래서
> 이 Task의 테스트는 3건이 아니라 4건이다(인증 거절 / 네트워크 / 5xx·예상 밖 / 토큰 없음).
> 스펙 본문의 부트스트랩 절이 이 규칙으로 갱신됐다.

- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현한다** — `AuthRepositoryImpl.hasSession()`은 `tokenStore.getRefreshToken() != null`
- [ ] **Step 4: 통과 확인** — PASS 3건
- [ ] **Step 5: 커밋** (지시가 있을 때만) — `feat(session): 부트스트랩 — 토큰으로 세션 복원`

---

### Task 6: 스플래시 자동로그인 라우팅

**Files:**
- Modify: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/splash/SplashViewModel.kt`
- Modify: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/splash/SplashRoute.kt`
- Modify: `feature/intro/impl/build.gradle.kts` (필요 시 `feature.groups.list.api` 의존)
- Test: `feature/intro/impl/src/test/.../splash/SplashViewModelTest.kt` (신규)

**Interfaces:**
- Consumes: Task 5의 `BootstrapSessionUseCase`, `SessionBootstrap`
- Produces: `SplashSideEffect.{NavigateToLogin, NavigateToGroupList}`

지금 `SplashViewModel`은 `SplashInitialUseCase()`(mock `delay`)를 부르고 `LoadingStatus.Success`만 내며, `SplashRoute`가 **무조건** `NavKeyLogin`으로 보낸다. 이 Task가 그 분기를 만든다.

- [ ] **Step 1: 실패하는 테스트를 쓴다** — 부트스트랩 결과에 따라 side effect가 갈리는지, 그리고 `launch(key)`로 중복 실행이 막히는지
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현한다** — `SplashSideEffect` 신설, `Navigator.replaceAll(destination)`으로 이동(`clearBackStack` + `goTo` 손조합을 새로 만들지 않는다)
- [ ] **Step 4: 통과 확인**
- [ ] **Step 5: 커밋** (지시가 있을 때만) — `feat(splash): 자동로그인 라우팅`

**주의** — `SplashInitialUseCase`(mock `delay(1000)`)를 지울지 남길지 정한다. 스플래시 최소 노출 시간이 필요하면 남기고, 아니면 지운다. 정한 이유를 report에 적는다.

✅ **확정됨(2026-08-15, 구현 중)** — `SplashInitialUseCase`는 **삭제**했다. 최소 노출 시간 요구가 스펙에 없어 자리채움 mock을 남길 이유가 없다. 부트스트랩 진입은 `SplashIntent.Init` 하나이고 `init`이 그것을 보낸다 — 처음엔 `bootstrap()`을 `internal`로 열어 테스트가 중복 실행 가드를 두 번 트리거하게 했으나, 그 화면만 MVI 관용구 밖으로 나가는 대가라 의도를 채우는 쪽으로 되돌렸다(`SplashIntent`는 비어 있었고 `processIntent`는 `= Unit`이었다).

---

### Task 7: 로그인·회원가입 직후 갱신

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LoginWithKakaoUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/SignUpUseCase.kt`
- Test: 기존 `LoginWithKakaoUseCaseTest`·`SignUpUseCaseTest`에 추가

**Interfaces:**
- Consumes: Task 3의 `RefreshMyAccountUseCase`(또는 `MemberRepository` 직접 — UseCase가 UseCase를 부르는 것이 저장소 관례에 맞는지 확인해 맞춘다)
- Produces: 없음

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun invoke_loginSucceeds_refreshesAccount() = runTest {
        // Given 기존 회원 로그인이 성공한다
        // When 로그인한다
        // Then 세션 저장과 함께 계정 정보를 한 번 당겨온다
        coVerify(exactly = 1) { refreshMyAccount() }
    }

    @Test
    fun invoke_accountRefreshFails_loginStillSucceeds() = runTest {
        // Given 로그인은 성공하나 계정 조회가 실패한다
        // When 로그인한다
        // Then 로그인 결과는 성공이다 — 그 시점에 되돌릴 곳이 없고 값은 다음 진입에서 채워진다
        assertTrue(result.isSuccess)
    }
```

- [ ] **Step 2~4**: 실패 확인 → 구현 → 통과 확인. 기존 테스트가 전부 통과해야 한다
- [ ] **Step 5: 커밋** (지시가 있을 때만) — `feat(auth): 로그인 직후 계정 정보 갱신`

---

### Task 8: S-001·S-002 화면 결선

**Files:**
- Create: `core/ui/src/main/kotlin/com/teamyg/parfait/core/ui/text/LoginProviderUiText.kt`
- Modify: `core/ui/src/main/res/values/strings.xml`
- Modify: `feature/app/setting/impl/.../viewmodel/AppSettingViewModel.kt`
- Modify: `feature/app/setting/impl/.../viewmodel/AccountInfoViewModel.kt`
- Modify: `feature/app/setting/impl/.../screen/AppSettingScreen.kt`·`AccountInfoScreen.kt`
- Test: 기존 `AppSettingViewModelTest`·`AccountInfoViewModelTest`에 추가

**Interfaces:**
- Consumes: Task 3의 `ObserveMyAccountUseCase`·`ChangeGlobalNicknameUseCase`, 기존 `CheckNameValidUseCase`
- Produces: 없음(마지막 Task)

**mock을 지우면 기본값이 사라진다.** `AppSettingState.nickname`·`loginProvider`, `AccountInfoUiState.nickname`이 `null`을 가질 수 있게 되고, 화면은 **빈 문자열이 아니라 로딩**을 보여야 한다.

`LoginProvider` 표시 매핑은 `core:ui`가 소유한다(ADR-0016) — `UNKNOWN`도 문구를 가져야 한다.

S-002 닉네임 변경은 기존 `CheckNameValidUseCase`로 먼저 거르고(형식), 서버 실패는 별도 갈래로 표시한다. `session-token-refresh-infra`의 `GroupNickNameError` 형태를 참고하되 **전역 닉네임용 에러 타입을 새로 만들지, 기존 것을 재사용할지 정하고 report에 적는다.**

✅ **확정됨(2026-08-15, 구현 중)** — feature 로컬 `GlobalNicknameError`를 **새로 만들었다.** `GroupNickNameError`는 `feature/groups/enter/impl` 안에 있어 재사용하려면 leaf 모듈 사이에 없던 의존을 새로 만들어야 하고, 서버 에러 어휘도 다르다(전역 닉네임엔 `ALREADY_USED`가 없다). 소비처가 하나면 feature 로컬이 맞다는 [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md) 애드덤을 따랐다. 문구가 모듈마다 겹치기 시작한 것은 [OQ-P-167](../../../synthesis/open-questions.md)이 이미 추적 중이다.

**추가 확정(2026-08-15, 디자인 대조 후)** — 이 화면은 **읽기가 기본이고 편집은 세션**이다. 확인 버튼은 포커스 중에만 하단(`imePadding`)에 뜨고, 활성 조건은 서버 값과 다를 것이다. 그래서 상태가 `savedNickname`(SSoT)과 `nickname`(입력 버퍼)을 나눠 갖는다 — 최종 리뷰가 후속으로 미뤄 뒀던 분리가 dirty 판정 때문에 여기서 필요해졌다. 뒤로가기는 고친 게 있을 때만 `YGModalPopup`으로 확인을 묻는다(`그만두기`=버리고 나가기 / `취소하기`=닫고 계속). 로딩은 자리를 바꾸지 않고 입력 필드를 비활성으로만 둔다. 형제 화면 S-102(`GroupSettingScreen`)가 버튼 스트립 관용구의 선례다.

- [ ] **Step 1: 실패하는 테스트를 쓴다** — SSoT 값이 상태로 흐르는지, `null`이면 로딩인지, 변경 성공이 SSoT를 통해 되돌아오는지, 변경 중 버튼 비활성·연타 1회
- [ ] **Step 2: 실패 확인**
- [ ] **Step 3: 구현한다**
- [ ] **Step 4: 통과 확인**
- [ ] **Step 5: 전체 검증** — `./gradlew :app:kspDebugKotlin`, `./gradlew testDebugUnitTest`, `./gradlew ktlintCheck`, `./gradlew :app:assembleDebug`
- [ ] **Step 6: 커밋** (지시가 있을 때만) — `feat(setting): S-001·S-002 계정 정보 결선`

---

## 수동 확인 (구현 후)

1. **자동로그인** — 로그인 후 앱 강제 종료 → 재실행. 로그인 화면을 거치지 않고 그룹 목록으로 간다
2. **첫 프레임** — 재실행 직후 설정 화면 진입 시 닉네임이 **깜빡임 없이** 보인다(영속의 목적)
3. **닉네임 전파** — S-002에서 닉네임 변경 → 뒤로 → S-001에 새 이름이 보인다
4. **계정 전환** — 로그아웃 후 다른 계정으로 로그인. 이전 사용자 닉네임이 한 순간도 보이지 않는다
5. **강제 로그아웃** — refresh token 무효화 후 API 호출. 로그인 화면 도착 후 다시 로그인하면 이전 계정 정보가 남아 있지 않다
6. **오프라인 재실행** — 비행기 모드로 앱 실행. 스펙대로 로그인 화면으로 가되 **토큰이 남아** 연결 복구 후 재실행하면 자동로그인된다
7. **provider 표시** — 카카오 로그인 계정에서 설정 화면의 provider 문구가 "카카오"로 보인다
