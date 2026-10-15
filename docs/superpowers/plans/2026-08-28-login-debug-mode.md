---
id: login-debug-mode
title: 로그인 화면 디버그 모드
status: draft
type: work-order
created: 2026-08-28
updated: 2026-08-28
platforms: android
owner: Parfait 팀
related_adr:
related_spec: login-debug-mode
related_code: LoginRoute, LoginViewModel, LoginState, LoginIntent, LoginSideEffect, KakaoLoginHelper, DebugModeRepository, DebugModeLocalDataSource, LocalDataSourceModule, RepositoryModule
archived_reason:
tags: [plan, parfait, login, debug]
---

# 로그인 화면 디버그 모드 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> 📌 **구현은 끝났고, 들어간 곳이 `develop` 이 아니다(2026-08-30 확인)** — `origin/feature/debug-mode`
> 커밋 6개가 `release/version-0.1.0-4` 에 머지됐다. `develop` 에는 `DebugMode*` 심볼이 0건이라
> 아카이브 판정(= `develop` 머지)이 서지 않아 이 계획은 `draft` 에 남는다 →
> [open-questions](../../synthesis/open-questions.md) OQ-P-311 ③.

**Goal:** 로그인 화면 빈 영역의 더블탭 7회 + 롱프레스로 디버그 모드를 켜고, 켜져 있으면 카카오 로그인이 카카오톡을 건너뛰고 웹 로그인으로 들어가게 한다.

**Architecture:** 플래그는 기존 `parfait_preferences` DataStore의 불린 하나이고, `DebugModeLocalDataSource` → `DebugModeRepository` 얇은 슬라이스로 노출한다(UseCase 없음). `LoginViewModel`이 그 저장소를 구독해 `LoginState.isDebugMode`로 투영하고, 배지 표시와 카카오 로그인 분기가 그 상태 하나를 읽는다. `KakaoLoginHelper`는 저장소를 모르고 `forceAccountLogin` 불린만 받는다.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, AndroidX DataStore Preferences, Kotlin Coroutines/Flow, 자체 MVI(`BaseViewModel`), 테스트는 kotlin-test + MockK + Turbine + kotlinx-coroutines-test.

**Spec:** [`parfait/specs/2026-08-28-login-debug-mode.md`](../specs/2026-08-28-login-debug-mode.md)

**작업 저장소:** `TJYG-Android` (remote `mash-up-kr/TEAMYG-Android`). 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다. 브랜치 `feature/debug-mode` 위에서 작업한다 — 그 브랜치는 지금 `develop`과 같고 작업 트리는 깨끗하다.

## Global Constraints

- **작업 위치**: `TJYG-Android` 저장소의 **본 체크아웃**, 현재 브랜치 `feature/debug-mode`. **git worktree를 만들지 않는다.**
- **커밋하지 않는다.** 사용자가 커밋을 요청하지 않았다. 각 Task는 코드 편집 + 검증까지만 하고 멈춘다. `git add`·`git commit`·`git push` 모두 금지.
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md` 요지):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다). 써야 하면 근거 문서를 가리킨다.
  - 주석 분량은 그 코드의 **어려움**에 비례한다. 중요하지만 단순한 코드에 긴 주석을 달지 않는다.
- **주석·KDoc·문자열 리소스는 한국어**로 쓴다. 기존 파일들의 어투를 따른다.
- **더블탭 판정은 "7 이상"이 아니라 "정확히 7"이다.** 롱프레스는 성공 여부와 무관하게 카운터를 0으로 되돌린다.
- **`BuildConfig.DEBUG` 게이트를 넣지 않는다.** 릴리즈 빌드에서도 동작하는 것이 확정된 요구다.
- **매퍼 단독 테스트를 만들지 않는다.** 변환 검증이 필요하면 데이터 소스 테스트의 케이스로 넣는다.
- ktlint가 CI 게이트다(`.github/workflows/ktlint.yml`이 `./gradlew ktlintCheck`를 돈다). 각 Task 끝에 해당 모듈의 `ktlintCheck`를 돌린다.
- 새 의존성을 추가하지 않는다. 이 계획에 쓰는 테스트 라이브러리는 모두 `parfait.test.unit` 플러그인이 이미 붙여 준다.

---

## File Structure

**Create**

| 파일 | 역할 |
|------|------|
| `domain/src/main/java/com/teamyg/parfait/domain/repository/debug/DebugModeRepository.kt` | 디버그 플래그 도메인 인터페이스 |
| `data/src/main/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSource.kt` | 로컬 소스 인터페이스 |
| `data/src/main/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSourceImpl.kt` | DataStore 구현 |
| `data/src/main/java/com/teamyg/parfait/data/repository/debug/DebugModeRepositoryImpl.kt` | 위임 구현 |
| `data/src/test/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSourceImplTest.kt` | 로컬 소스 테스트 |
| `feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelperTest.kt` | 웹 로그인 강제 분기 테스트 |

**Modify**

| 파일 | 변경 |
|------|------|
| `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt` | `@Binds` 1개 추가 |
| `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt` | `@Binds` 1개 추가 |
| `feature/login/impl/.../util/KakaoLoginHelper.kt` | `forceAccountLogin` 파라미터 |
| `feature/login/impl/.../viewmodel/LoginViewModel.kt` | 상태·Intent·SideEffect·저장소 주입 |
| `feature/login/impl/.../route/LoginRoute.kt` | `Box` 감싸기·제스처·배지·플래그 전달 |
| `feature/login/impl/src/main/res/values/strings.xml` | 배지 문구 |
| `feature/login/impl/src/test/.../viewmodel/LoginViewModelTest.kt` | 생성자 변경 반영 + 새 케이스 6개 |

---

### Task 1: 디버그 플래그 저장 슬라이스

**Files:**
- Create: `data/src/test/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSourceImplTest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSourceImpl.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/debug/DebugModeRepository.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/debug/DebugModeRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: 기존 `DataStore<Preferences>`(Hilt가 `DataStoreModule.provideParfaitPreferencesDataStore`로 제공), 테스트 대역 `com.teamyg.parfait.data.datastore.FakePreferencesDataStore`.
- Produces: `com.teamyg.parfait.domain.repository.debug.DebugModeRepository` — `val isEnabled: Flow<Boolean>`, `suspend fun setEnabled(enabled: Boolean)`. Task 3의 `LoginViewModel`이 이것을 주입받는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/source/debug/local/DebugModeLocalDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.debug.local

import app.cash.turbine.test
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSourceImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugModeLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    private val dataSource = DebugModeLocalDataSourceImpl(dataStore = dataStore)

    @Test
    fun isEnabled_neverStored_isFalse() = runTest {
        // Given 한 번도 저장한 적이 없다

        // When 값을 읽는다
        // Then 켜지지 않은 것으로 본다 — 없는 값이 디버그 모드를 열면 안 된다
        assertFalse(dataSource.isEnabled.first())
    }

    @Test
    fun setEnabled_true_thenRead_isTrue() = runTest {
        // Given 디버그 모드를 켠다
        dataSource.setEnabled(true)

        // When 다시 읽는다
        // Then 켜진 채로 남는다
        assertTrue(dataSource.isEnabled.first())
    }

    @Test
    fun setEnabled_false_afterTrue_isFalse() = runTest {
        // Given 켜 둔 상태
        dataSource.setEnabled(true)

        // When 끈다
        dataSource.setEnabled(false)

        // Then 꺼진 채로 남는다 — 배지 탭이 유일한 회복 경로라 이 왕복이 끊기면 안 된다
        assertFalse(dataSource.isEnabled.first())
    }

    @Test
    fun isEnabled_otherKeyChanges_doesNotReemit() = runTest {
        // Given 디버그 플래그를 구독하고 있다
        dataSource.isEnabled.test {
            assertFalse(awaitItem())

            // When 같은 DataStore 파일의 다른 키가 바뀐다
            dataStore.putRaw(key = ToppingDraftLocalDataSourceImpl.TOPPING_DRAFT_KEY_NAME, value = "{}")
            // `expectNoEvents` 는 그 시점 채널만 본다 — 수집 코루틴을 먼저 재개시키지 않으면
            // `distinctUntilChanged` 가 없어도 통과해 이 테스트가 아무것도 지키지 못한다
            runCurrent()

            // Then 디버그 플래그 구독자는 흔들리지 않는다
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*DebugModeLocalDataSourceImplTest*"
```

Expected: 컴파일 실패 — `Unresolved reference: DebugModeLocalDataSourceImpl`.

- [ ] **Step 3: 로컬 소스 인터페이스와 구현을 쓴다**

`DebugModeLocalDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.debug.local

import kotlinx.coroutines.flow.Flow

interface DebugModeLocalDataSource {
    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}
```

`DebugModeLocalDataSourceImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.debug.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugModeLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) : DebugModeLocalDataSource {
    // 이 파일을 공유하는 다른 키가 바뀌어도 `data` 는 재방출한다 — 여기서 먼저 dedupe 하지
    // 않으면 무관한 쓰기마다 구독자가 흔들린다(`ToppingDraftLocalDataSourceImpl` 과 같은 이유)
    override val isEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[DEBUG_MODE_KEY] == true }
        .distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DEBUG_MODE_KEY] = enabled }
    }

    private companion object {
        val DEBUG_MODE_KEY = booleanPreferencesKey("debug_mode")
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*DebugModeLocalDataSourceImplTest*"
```

Expected: PASS (4개 케이스).

- [ ] **Step 5: 도메인 인터페이스와 위임 구현을 쓴다**

`domain/.../repository/debug/DebugModeRepository.kt`:

```kotlin
package com.teamyg.parfait.domain.repository.debug

import kotlinx.coroutines.flow.Flow

/**
 * 개발·QA 편의를 위한 디버그 모드 플래그. 저장소가 단일 진실이고 화면 상태는 그 투영이다.
 *
 * 정책은 `specs/2026-08-28-login-debug-mode.md` 가 정본이다.
 */
interface DebugModeRepository {
    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}
```

`data/.../repository/debug/DebugModeRepositoryImpl.kt`:

```kotlin
package com.teamyg.parfait.data.repository.debug

import com.teamyg.parfait.data.source.debug.local.DebugModeLocalDataSource
import com.teamyg.parfait.domain.repository.debug.DebugModeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DebugModeRepositoryImpl @Inject constructor(
    private val debugModeLocalDataSource: DebugModeLocalDataSource,
) : DebugModeRepository {
    override val isEnabled: Flow<Boolean> = debugModeLocalDataSource.isEnabled

    override suspend fun setEnabled(enabled: Boolean) = debugModeLocalDataSource.setEnabled(enabled)
}
```

- [ ] **Step 6: DI를 배선한다**

`LocalDataSourceModule.kt`의 `interface LocalDataSourceModule` 본문 끝에 추가하고, 파일 상단 import 블록에 두 줄(`...source.debug.local.DebugModeLocalDataSource`, `...source.debug.local.DebugModeLocalDataSourceImpl`)을 더한다. `.editorconfig`가 `ktlint_standard_import-ordering`을 꺼 두었고 기존 파일도 알파벳 순이 아니므로 정렬을 맞추려 애쓰지 않는다:

```kotlin
    @Binds
    @Singleton
    fun bindDebugModeLocalDataSource(
        debugModeLocalDataSourceImpl: DebugModeLocalDataSourceImpl,
    ): DebugModeLocalDataSource
```

`RepositoryModule.kt`의 `interface RepositoryModule` 본문 끝에 추가하고, import 블록에 두 줄(`...data.repository.debug.DebugModeRepositoryImpl`, `...domain.repository.debug.DebugModeRepository`)을 더한다:

```kotlin
    @Binds
    @Singleton
    fun bindDebugModeRepository(debugModeRepositoryImpl: DebugModeRepositoryImpl): DebugModeRepository
```

- [ ] **Step 7: Hilt 그래프와 스타일을 검증한다**

```bash
./gradlew :app:kspDebugKotlin :data:ktlintCheck :domain:ktlintCheck
```

Expected: BUILD SUCCESSFUL. Hilt 그래프 검증은 `:app`에서 도므로 여기서 누락 바인딩이 잡힌다.

---

### Task 2: `KakaoLoginHelper` 웹 로그인 강제

**Files:**
- Create: `feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelperTest.kt`
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelper.kt`

**Interfaces:**
- Consumes: 없음(Task 1과 독립이다 — Helper는 저장소를 모른다).
- Produces: `suspend fun KakaoLoginHelper.login(activity: Activity, forceAccountLogin: Boolean = false): KakaoLoginResult`. Task 4의 `LoginRoute`가 이 시그니처로 호출한다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/util/KakaoLoginHelperTest.kt`:

```kotlin
package com.teamyg.parfait.feature.login.impl.util

import android.app.Activity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.util.NonceGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class KakaoLoginHelperTest {
    private val userApiClient: UserApiClient = mockk(relaxed = true)
    private val activity: Activity = mockk(relaxed = true)

    private val helper = KakaoLoginHelper(
        userApiClient = userApiClient,
        // `NonceGenerator` 는 `fun interface` 라 람다로 고정값을 준다
        nonceGenerator = NonceGenerator { "nonce-1" },
    )

    /**
     * SDK 호출은 콜백이 돌아와야 재개된다. 스텁이 아무것도 안 하면 `login` 이 영영 매달리므로
     * 실패 콜백을 즉시 되돌려 준다 — 이 테스트가 보는 것은 결과가 아니라 **어느 경로로 갔는가**다.
     */
    private fun stubAccountLoginWithFailure() {
        every {
            userApiClient.loginWithKakaoAccount(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val callback = lastArg<(OAuthToken?, Throwable?) -> Unit>()
            callback(null, IllegalStateException("stub"))
        }
    }

    @Test
    fun login_forceAccountLogin_skipsKakaoTalkEntirely() = runTest {
        // Given 카카오톡으로 로그인할 수 있는 기기
        every { userApiClient.isKakaoTalkLoginAvailable(any()) } returns true
        stubAccountLoginWithFailure()

        // When 웹 로그인을 강제한다
        val result = helper.login(activity = activity, forceAccountLogin = true)

        // Then 설치 여부를 묻지도 않고 계정 로그인으로 간다
        verify(exactly = 0) { userApiClient.isKakaoTalkLoginAvailable(any()) }
        verify(exactly = 1) {
            userApiClient.loginWithKakaoAccount(any(), any(), any(), any(), any(), any(), any())
        }
        assertIs<KakaoLoginResult.Failure>(result)
    }

    @Test
    fun login_default_checksKakaoTalkAvailability() = runTest {
        // Given 카카오톡이 없는 기기
        every { userApiClient.isKakaoTalkLoginAvailable(any()) } returns false
        stubAccountLoginWithFailure()

        // When 기본값으로 로그인한다
        helper.login(activity = activity)

        // Then 기존 경로 그대로 설치 여부를 먼저 묻는다
        verify(exactly = 1) { userApiClient.isKakaoTalkLoginAvailable(any()) }
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :feature:login:impl:testDebugUnitTest --tests "*KakaoLoginHelperTest*"
```

Expected: 컴파일 실패 — `login` 에 `forceAccountLogin` 파라미터가 없다(`Cannot find a parameter with this name`).

⚠️ 여기서 **`forceAccountLogin` 파라미터 부재 말고 다른 실패가 나오면 멈추고 보고한다.** 특히 `mockk<Activity>(relaxed = true)`는 이 저장소에 선례가 없다 — JVM 유닛 테스트에서 `android.*` 프레임워크 클래스를 목으로 만든 테스트가 한 건도 없고 `testOptions.unitTests.returnDefaultValues`도 설정돼 있지 않다. 목 **생성 단계**에서 터지면 프로덕션 코드를 비틀어 우회하지 말고, 이 테스트 파일을 지운 채 Task 3으로 넘어가고 그 사실을 보고한다. 분기 계약은 그때 수동 검증(Task 4 Step 6)이 대신 지킨다.

- [ ] **Step 3: 최소 구현을 넣는다**

`KakaoLoginHelper.kt`의 `login`을 이렇게 고친다. KDoc의 기존 두 문단은 그대로 두고 `forceAccountLogin` 설명만 덧붙인다:

```kotlin
    /**
     * 로그인 1회분 nonce 를 만들어 SDK 요청에 넘기고, 성공 결과에 같은 값을 실어 돌려준다.
     * 카카오톡 로그인이 실패해 계정 로그인으로 넘어가도 **nonce 는 그대로 재사용**한다 —
     * 최종 성공한 로그인이 그 nonce 로 발급받은 ID 토큰을 주므로 서버 대조가 맞는다.
     *
     * @param forceAccountLogin 참이면 카카오톡 설치 여부를 묻지 않고 계정(웹) 로그인으로 간다.
     *   디버그 모드가 이 값을 세운다(`specs/2026-08-28-login-debug-mode.md`).
     */
    suspend fun login(
        activity: Activity,
        forceAccountLogin: Boolean = false,
    ): KakaoLoginResult {
        val nonce = nonceGenerator.generate()

        return if (!forceAccountLogin && isKakaoTalkLoginAvailable(activity)) {
            when (val result = loginWithKakaoTalk(activity, nonce)) {
                is KakaoLoginResult.Success -> result
                is KakaoLoginResult.Cancel -> result
                is KakaoLoginResult.Failure -> loginWithKakaoAccount(activity, nonce)
            }
        } else {
            loginWithKakaoAccount(activity, nonce)
        }
    }
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:login:impl:testDebugUnitTest --tests "*KakaoLoginHelperTest*" && ./gradlew :feature:login:impl:ktlintCheck
```

Expected: PASS (2개 케이스) + ktlint 통과.

`loginWithKakaoAccount`의 Kotlin 선언은 7-파라미터 하나뿐이고(바이트코드의 인자 수 다른 오버로드들은 `@JvmOverloads`가 만든 산물이다) 콜백이 마지막 인자다. 따라서 `any()` 일곱 개가 그 선언에 정확히 붙고, 프로덕션의 명명인자 호출도 `$default` 다리를 거쳐 같은 목에 걸린다. 이 스텁이 SDK 버전 업으로 깨지면 컴파일 에러로 즉시 드러난다.

---

### Task 3: `LoginViewModel` 디버그 상태와 제스처 판정

**Files:**
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModel.kt`
- Modify: `feature/login/impl/src/test/java/com/teamyg/parfait/feature/login/impl/viewmodel/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `DebugModeRepository`(`isEnabled: Flow<Boolean>`, `suspend setEnabled(Boolean)`).
- Produces:
  - `LoginState(isLoading: Boolean = false, isDebugMode: Boolean = false)`
  - `LoginIntent.DebugDoubleTap`, `LoginIntent.DebugLongPress`, `LoginIntent.DisableDebugMode` (모두 `data object`)
  - `LoginSideEffect.RequestLoginWithKakao(val forceAccountLogin: Boolean)` (`data object` → `data class`)
  - Task 4의 `LoginRoute`가 이 셋을 모두 쓴다.

- [ ] **Step 1: 기존 테스트를 새 생성자·새 이펙트 타입에 맞춘다**

`LoginViewModelTest.kt`에서 세 곳을 고친다.

첫째, 파일 안에 테스트용 대역을 추가한다(파일 맨 아래, 클래스 밖):

```kotlin
private class FakeDebugModeRepository(initial: Boolean = false) : DebugModeRepository {
    private val state = MutableStateFlow(initial)

    override val isEnabled: Flow<Boolean> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
```

둘째, 필드와 팩토리를 고친다:

```kotlin
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase = mockk()

    private fun viewModel(debugModeRepository: DebugModeRepository = FakeDebugModeRepository()) =
        LoginViewModel(
            loginWithKakaoUseCase = loginWithKakaoUseCase,
            debugModeRepository = debugModeRepository,
        )
```

셋째, `LoginSideEffect.RequestLoginWithKakao`를 비교하는 **모든** 단언을 인자 있는 형태로 바꾼다. 이 파일에 다섯 곳(테스트 `loginWithKakao_firstClick_...`, `loginWithKakao_clickedWhileLoading_...`, `loginFailure_useCaseThrows_...`, `sdkCancel_...`, `sdkFailure_...`)이 있다:

```kotlin
assertEquals(LoginSideEffect.RequestLoginWithKakao(forceAccountLogin = false), awaitItem())
```

추가 import: `com.teamyg.parfait.domain.repository.debug.DebugModeRepository`, `kotlinx.coroutines.flow.Flow`, `kotlinx.coroutines.flow.MutableStateFlow`.

- [ ] **Step 2: 새 케이스 6개를 쓴다**

`LoginViewModelTest` 클래스 안, 기존 테스트들 뒤에 넣는다:

```kotlin
    @Test
    fun debugGesture_sevenDoubleTapsThenLongPress_enablesDebugMode() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아직 꺼져 있다
        val repository = FakeDebugModeRepository()
        val viewModel = viewModel(repository)

        // When 더블탭 7회 뒤 롱프레스
        repeat(7) { viewModel.processIntent(LoginIntent.DebugDoubleTap) }
        viewModel.processIntent(LoginIntent.DebugLongPress)
        advanceUntilIdle()

        // Then 저장소에 켜진 것으로 남고 화면 상태가 그것을 따라온다
        assertTrue(repository.isEnabled.first())
        assertTrue(viewModel.state.value.isDebugMode)
    }

    @Test
    fun debugGesture_sixDoubleTapsThenLongPress_doesNotEnableDebugMode() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아직 꺼져 있다
        val repository = FakeDebugModeRepository()
        val viewModel = viewModel(repository)

        // When 한 번 모자란 채로 롱프레스
        repeat(6) { viewModel.processIntent(LoginIntent.DebugDoubleTap) }
        viewModel.processIntent(LoginIntent.DebugLongPress)
        advanceUntilIdle()

        // Then 켜지지 않는다
        assertFalse(repository.isEnabled.first())
        assertFalse(viewModel.state.value.isDebugMode)
    }

    @Test
    fun debugGesture_eightDoubleTapsThenLongPress_doesNotEnableDebugMode() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아직 꺼져 있다
        val repository = FakeDebugModeRepository()
        val viewModel = viewModel(repository)

        // When 한 번 더 밟고 롱프레스 — 판정은 "7 이상"이 아니라 "정확히 7"이다
        repeat(8) { viewModel.processIntent(LoginIntent.DebugDoubleTap) }
        viewModel.processIntent(LoginIntent.DebugLongPress)
        advanceUntilIdle()

        // Then 켜지지 않는다
        assertFalse(repository.isEnabled.first())
    }

    @Test
    fun debugGesture_longPressResetsCount_soFailedAttemptDoesNotAccumulate() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 4회를 밟고 롱프레스로 한 번 실패했다
            val repository = FakeDebugModeRepository()
            val viewModel = viewModel(repository)
            repeat(4) { viewModel.processIntent(LoginIntent.DebugDoubleTap) }
            viewModel.processIntent(LoginIntent.DebugLongPress)
            advanceUntilIdle()

            // When 3회만 더 밟고 롱프레스한다(누적이면 7이 된다)
            repeat(3) { viewModel.processIntent(LoginIntent.DebugDoubleTap) }
            viewModel.processIntent(LoginIntent.DebugLongPress)
            advanceUntilIdle()

            // Then 켜지지 않는다 — 실패한 시도는 다음 시도에 남지 않는다
            assertFalse(repository.isEnabled.first())
        }

    @Test
    fun disableDebugMode_turnsFlagOff() = runTest(mainDispatcherRule.dispatcher) {
        // Given 켜져 있다
        val repository = FakeDebugModeRepository(initial = true)
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isDebugMode)

        // When 배지를 탭한다
        viewModel.processIntent(LoginIntent.DisableDebugMode)
        advanceUntilIdle()

        // Then 꺼진다 — 이것이 유일한 회복 경로다
        assertFalse(repository.isEnabled.first())
        assertFalse(viewModel.state.value.isDebugMode)
    }

    @Test
    fun loginWithKakao_debugModeOn_requestsAccountLogin() = runTest(mainDispatcherRule.dispatcher) {
        // Given 디버그 모드가 켜져 있다
        val viewModel = viewModel(FakeDebugModeRepository(initial = true))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 카카오 버튼을 누른다
            viewModel.processIntent(LoginIntent.LoginWithKakao)
            runCurrent()

            // Then 카카오톡을 건너뛰라는 신호가 함께 나간다
            assertEquals(LoginSideEffect.RequestLoginWithKakao(forceAccountLogin = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
```

추가 import: `kotlinx.coroutines.flow.first`.

- [ ] **Step 3: 실패를 확인한다**

```bash
./gradlew :feature:login:impl:testDebugUnitTest --tests "*LoginViewModelTest*"
```

Expected: 컴파일 실패 — `LoginIntent.DebugDoubleTap` 등이 없다.

- [ ] **Step 4: `LoginViewModel`을 고친다**

상태에 필드를 더한다:

```kotlin
data class LoginState(
    val isLoading: Boolean = false,
    val isDebugMode: Boolean = false,
) : UiState
```

Intent에 세 갈래를 더한다(`sealed interface LoginIntent` 본문 끝):

```kotlin
    /** 로그인 화면 빈 영역 더블탭. 7회 뒤 롱프레스가 디버그 모드를 연다 */
    data object DebugDoubleTap : LoginIntent

    data object DebugLongPress : LoginIntent

    data object DisableDebugMode : LoginIntent
```

SideEffect를 인자 있는 형태로 바꾼다:

```kotlin
    /** @param forceAccountLogin 참이면 카카오톡을 건너뛰고 계정(웹) 로그인으로 간다 */
    data class RequestLoginWithKakao(val forceAccountLogin: Boolean) : LoginSideEffect
```

생성자와 `init`:

```kotlin
@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
    private val debugModeRepository: DebugModeRepository,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    /** 화면을 벗어나면 사라져도 되는 값이라 상태에 두지 않는다 — 탭마다 리컴포지션을 돌릴 이유가 없다 */
    private var debugDoubleTapCount = 0

    init {
        viewModelLogger.i { "LoginViewModel::init" }
        observeDebugMode()
    }
```

`processIntent`의 `when`에 세 갈래를 더한다:

```kotlin
            is LoginIntent.DebugDoubleTap -> debugDoubleTapCount++

            is LoginIntent.DebugLongPress -> enableDebugModeIfGestureMatched()

            is LoginIntent.DisableDebugMode -> setDebugMode(enabled = false)
```

private 함수 셋을 더한다(`requestSdkLogin` 위나 아래, 파일의 기존 배치 감각을 따른다):

```kotlin
    private fun observeDebugMode() {
        launch {
            debugModeRepository.isEnabled.collect { enabled ->
                updateState { copy(isDebugMode = enabled) }
            }
        }
    }

    /**
     * 롱프레스는 성공하든 못 하든 카운터를 되돌린다 — 실패한 시도가 다음 시도에 누적되면
     * 아무 데나 눌러도 열리는 제스처가 된다.
     */
    private fun enableDebugModeIfGestureMatched() {
        val matched = debugDoubleTapCount == DEBUG_GESTURE_DOUBLE_TAP_COUNT
        debugDoubleTapCount = 0

        if (!matched) {
            return
        }

        viewModelLogger.i { "디버그 모드를 켠다" }
        setDebugMode(enabled = true)
    }

    private fun setDebugMode(enabled: Boolean) {
        launch { debugModeRepository.setEnabled(enabled) }
    }
```

`requestSdkLogin`의 이펙트 발행을 고친다:

```kotlin
        postSideEffect(LoginSideEffect.RequestLoginWithKakao(forceAccountLogin = state.value.isDebugMode))
```

companion에 상수를 더한다:

```kotlin
    private companion object {
        /** [launch] 중복 실행 가드 키 — 이 ViewModel 의 서버 로그인 job 하나를 가리킨다 */
        const val KEY_KAKAO_LOGIN = "kakaoLogin"

        const val DEBUG_GESTURE_DOUBLE_TAP_COUNT = 7
    }
```

추가 import: `com.teamyg.parfait.domain.repository.debug.DebugModeRepository`.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:login:impl:testDebugUnitTest --tests "*LoginViewModelTest*" && ./gradlew :feature:login:impl:ktlintCheck
```

Expected: PASS (기존 11개 + 신규 6개) + ktlint 통과.

⚠️ **이 시점에 기능은 아직 동작하지 않는다.** `LoginRoute`가 `effect.forceAccountLogin`을 읽지 않아 디버그 모드를 켜도 카카오톡 로그인으로 간다. 배선은 Task 4 Step 2이고, 기능 확인은 Task 4를 마친 뒤에 한다. 컴파일과 테스트가 통과하는 것을 기능 완성으로 오인하지 않는다.

---

### Task 4: 로그인 화면 제스처와 배지

**Files:**
- Modify: `feature/login/impl/src/main/java/com/teamyg/parfait/feature/login/impl/route/LoginRoute.kt`
- Modify: `feature/login/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 2의 `KakaoLoginHelper.login(activity, forceAccountLogin)`, Task 3의 `LoginState.isDebugMode`·`LoginIntent.DebugDoubleTap`·`LoginIntent.DebugLongPress`·`LoginIntent.DisableDebugMode`·`LoginSideEffect.RequestLoginWithKakao(forceAccountLogin)`.
- Produces: 없음(화면 말단).

- [ ] **Step 1: 문자열 리소스를 더한다**

`feature/login/impl/src/main/res/values/strings.xml`의 `<resources>` 안에 추가한다:

```xml
    <string name="login_debug_mode_badge">디버그 모드</string>
    <string name="login_debug_mode_disable_label">디버그 모드 끄기</string>
```

- [ ] **Step 2: 이펙트 처리에서 플래그를 실어 나른다**

`LoginRoute.kt`의 `is LoginSideEffect.RequestLoginWithKakao -> { … }` 블록에서 Helper 호출 한 줄만 고친다. `activity` 널 가드와 `try`/`catch`는 그대로 둔다:

```kotlin
                        when (
                            val result = kakaoLoginHelper.login(
                                activity = currentActivity,
                                forceAccountLogin = effect.forceAccountLogin,
                            )
                        ) {
```

- [ ] **Step 3: 제스처와 배지를 단다**

`YGScaffoldV2`의 콘텐츠 람다를 이렇게 바꾼다:

```kotlin
    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        // 제스처를 콘텐츠 **위에** 덮는 오버레이로 달면 카카오 버튼 탭까지 이 레이어가 먹는다.
        // 부모에 달면 자식이 먼저 히트 테스트를 받으므로, 소비자가 없는 빈 영역의 탭만 온다
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { viewModel.processIntent(LoginIntent.DebugDoubleTap) },
                        onLongPress = { viewModel.processIntent(LoginIntent.DebugLongPress) },
                    )
                },
        ) {
            LoginScreen(
                pages = tempPages,
                isLoading = state.isLoading,
                onClickKakaoButton = {
                    viewModel.processIntent(LoginIntent.LoginWithKakao)
                },
                // `fillMaxSize` 는 취향이 아니라 하중이다 — LoginScreen 의 Column 안에서
                // OnboardingPager 가 weight(1f) 을 쓰므로 높이가 안 잡히면 레이아웃이 접힌다
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isDebugMode) {
                Text(
                    text = stringResource(R.string.login_debug_mode_badge),
                    style = YGTheme.typography.caption.c01R,
                    color = YGAtomicColors.Gray.Gray300,
                    // `clickable` 을 `padding` 앞에 둬야 여백까지 탭 영역이 된다. 12sp 글자에
                    // 사방 16dp 를 붙여야 최소 터치 타깃(48dp)에 닿는다 — 이 배지가 디버그
                    // 모드를 끄는 유일한 경로다
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClickLabel = stringResource(R.string.login_debug_mode_disable_label)) {
                            viewModel.processIntent(LoginIntent.DisableDebugMode)
                        }
                        .padding(YGTheme.layout.padding.padding6),
                )
            }
        }
    }
```

추가 import:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
```

⚠️ `Text`와 `YGAtomicColors`의 import 경로는 같은 모듈의 `component/OnboardingPager.kt`가 쓰는 것을 그대로 따른다. 그 파일을 열어 확인한 뒤 베낀다 — 추측해서 쓰지 않는다.

- [ ] **Step 4: 컴파일과 스타일을 검증한다**

```bash
./gradlew :feature:login:impl:compileDebugKotlin :feature:login:impl:ktlintCheck
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 모듈 전체 테스트를 돌린다**

```bash
./gradlew :feature:login:impl:testDebugUnitTest :data:testDebugUnitTest
```

Expected: 전부 PASS. 여기서 실패하면 앞 Task가 깬 것이므로 그 Task로 돌아간다.

- [ ] **Step 6: 수동 검증 절차를 보고한다**

코드를 고치지 않고, 사용자가 기기에서 확인할 절차를 정리해 보고한다. 에뮬레이터·기기 실행은 이 계획의 범위 밖이다.

1. 로그인 화면 온보딩 이미지 영역(버튼이 아닌 빈 곳)을 더블탭 7회 뒤 길게 누른다 → 우측 상단에 "디버그 모드"가 뜬다.
   - **더블탭 쌍 사이에는 잠깐 쉰다.** `detectTapGestures`는 탭 둘을 한 쌍으로 묶고 쌍 사이에 더블탭 타임아웃 만료를 기다리므로, 14회를 균등한 간격으로 빠르게 치면 쌍 경계가 어긋나 카운트가 7에 도달하지 못한다.
   - **롱프레스는 손가락을 완전히 멈춘 채** 0.5초 이상 누른다. 터치 슬롭을 넘기면 페이저가 이동을 소비해 롱프레스가 취소된다.
   - 온보딩을 스와이프한 직후의 탭은 페이저가 삼킬 수 있다. 스크롤이 멎은 뒤에 시작한다.
2. 앱을 완전히 종료하고 다시 켠다 → 배지가 그대로 있다(DataStore 영속 확인).
3. 카카오톡이 설치된 기기에서 카카오 로그인 버튼을 누른다 → 카카오톡 앱이 아니라 웹 로그인 화면이 뜬다.
4. 배지를 탭한다 → 배지가 사라지고, 다시 로그인을 시도하면 카카오톡 앱으로 간다.
5. 카카오 버튼 자체를 더블탭·롱프레스해 본다 → 카운터가 올라가지 않는다(버튼이 탭을 소비한다).

⚠️ **이 다섯 절차가 제스처와 배지를 지키는 유일한 수단이다.** `feature/login/impl`에는 `androidTest` 소스셋이 아예 없고, 이 계획은 "새 의존성을 추가하지 않는다" 제약 때문에 Compose UI 테스트 하니스를 신설하지 않는다. 하니스가 필요하다고 판단되면 만들지 말고 먼저 보고한다.

---

## 검증 요약

| 게이트 | 명령 |
|--------|------|
| 데이터 슬라이스 | `./gradlew :data:testDebugUnitTest --tests "*DebugModeLocalDataSourceImplTest*"` |
| Hilt 그래프 | `./gradlew :app:kspDebugKotlin` |
| 로그인 모듈 | `./gradlew :feature:login:impl:testDebugUnitTest` |
| 스타일(모듈 한정) | `./gradlew :data:ktlintCheck :domain:ktlintCheck :feature:login:impl:ktlintCheck` |
| CI가 실제로 도는 것 | `./gradlew test ktlintCheck` (마지막 Task를 마친 뒤 한 번) |

CI는 전 모듈 `test`와 전 모듈 `ktlintCheck`를 돈다(`.github/workflows/test.yml`·`ktlint.yml`). 모듈 한정 태스크만으로는 다른 모듈로 새어 나간 회귀를 못 잡으므로 마지막에 전체를 한 번 돌린다.
