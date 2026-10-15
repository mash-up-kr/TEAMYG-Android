---
id: segmentation-module-install
title: 세그멘테이션 optional module 설치 대기·실패 처리 구현 계획
status: done
type: work-order
created: 2026-09-02
updated: 2026-09-03
archived_reason: 구현 완료·develop 머지(2026-09-03, PR #438 24679f8c). 브랜치 feature/ml-kit-model-load-exception, 7 Task 전량 수행, 신규 테스트 10건.
platforms: android
owner: Parfait 팀
related_adr: ADR-0012
related_spec: segmentation-module-install, c103-multi-subject-selection
related_code:
  - SegmentationModuleInstaller.kt#ensureInstalled
  - ModuleInstallGateway.kt
  - PlayServicesModuleInstallGateway.kt
  - ModuleInstallModule.kt
  - PrepareSegmentationModuleUseCase.kt
  - PictureConfirmViewModel.kt
  - ImageSegmentationRepository.kt#prepareSegmentationModule
  - SegmentationViewModel.kt#SegmentationErrorKind
  - SegmentationErrorScreen.kt#SegmentationErrorScreen
tags: [plan, parfait]
---

# 세그멘테이션 모듈 설치 대기·실패 처리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ML Kit 세그멘테이션 optional module 설치가 끝났는지를 실제로 알아내고, 못 받았으면 사용자에게 맞는 안내와 재시도를 준다.

**Architecture:** 모듈 준비의 단일 소유자 `SegmentationModuleInstaller`를 `:data`에 두고, `Mutex` + `CompletableDeferred`로 진행 중 설치를 여러 호출자가 공유한다. GMS 타입은 `ModuleInstallGateway` 이음매 뒤로 숨겨 JVM 테스트가 닿게 한다. 사전 설치는 카메라·갤러리 두 진입 경로의 합류점인 사진 확인 화면에서 건다.

**Tech Stack:** Kotlin, Coroutines(`kotlinx-coroutines-test`), Hilt, Jetpack Compose, Play services `play-services-base` 18.5.0, ML Kit `play-services-mlkit-subject-segmentation` 16.0.0-beta1, MockK, Turbine, JUnit4.

**Spec:** `parfait/specs/archive/2026-09-02-segmentation-module-install.md` (AI 스킬·위키 repo. 코드 작업 대상은 별도 repo `TJYG-Android`)

**작업 대상 저장소:** `TJYG-Android`, 브랜치 `feature/ml-kit-model-load-exception`.
⚠️ 이 브랜치 작업 트리에는 **조사용 진단 코드가 미커밋 상태로 남아 있다.** Task 3이 그것을 최종
형태로 정리한다. 그 전까지는 건드리지 않는다.

## Global Constraints

- **커밋만 하고 push·PR은 하지 않는다.** 사용자가 명시적으로 승인하기 전에는 리모트로 내보내지 않는다.
- **코드 주석 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다 — 낡는다. 써야 하면 근거 문서를 가리킨다.
  - 아키텍처 결정은 코드가 아니라 `parfait/adr/`·`parfait/architecture/`에 쓰고 코드엔 포인터 한 줄만 둔다.
- **문구는 한국어**, 기존 `strings.xml`의 어투(`~어요`)를 따른다.
- **매퍼 단독 테스트를 만들지 않는다.** 판단이 든 변환은 그것을 쓰는 쪽 테스트의 케이스로 넣는다.
- `INSTALL_TIMEOUT` = **20초**.
- 진단 로그 접두사는 `[MLKIT-MODULE]`로 유지한다. 최종 코드에 남긴다.
- 새 테스트 하니스·빌드 설정을 신설하지 않는다. 필요하면 멈추고 물어본다.

---

## 파일 구성

| 파일 | 책임 |
|---|---|
| `data/.../repository/image/ModuleInstallGateway.kt` | GMS 이음매 — 인터페이스 + 신호 타입 |
| `data/.../repository/image/PlayServicesModuleInstallGateway.kt` | 위 인터페이스의 GMS 구현 |
| `data/.../repository/image/SegmentationModuleInstaller.kt` | 공유 대기·종료 판정 |
| `data/.../repository/image/ImageSegmentationRepositoryImpl.kt` | 설치기 사용으로 교체, 준비 함수 추가 |
| `domain/.../repository/image/ImageSegmentationRepository.kt` | 준비 함수 선언 |
| `domain/.../usecase/image/PrepareSegmentationModuleUseCase.kt` | 화면이 쓰는 통로 |
| `data/.../di/RepositoryModule.kt` | 게이트웨이 결선 |
| `feature/segmentation/.../viewmodel/SegmentationViewModel.kt` | `errorKind`·재시도·실패 로깅 |
| `feature/segmentation/.../screen/SegmentationErrorScreen.kt` | 문구·재시도 버튼을 받는다 |
| `feature/segmentation/.../route/SegmentationRoute.kt` | `errorKind`로 문구를 고른다 |
| `feature/segmentation/impl/src/main/res/values/strings.xml` | 문구 3개 추가 |
| `feature/camera/.../viewmodel/PictureConfirmViewModel.kt` | 사전 설치 훅 |
| `feature/camera/.../route/PictureConfirmRoute.kt` | 위 ViewModel 연결 |

---

### Task 1: 모듈 설치기와 게이트웨이 이음매

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/ModuleInstallGateway.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationModuleInstaller.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationModuleInstallerTest.kt`

**Interfaces:**
- Consumes: 없음(첫 태스크).
- Produces:
  - `interface ModuleInstallGateway { suspend fun isAvailable(): Boolean; fun install(onSignal: (ModuleInstallSignal) -> Unit) }`
  - `sealed interface ModuleInstallSignal` — `AlreadyInstalled`, `Completed`, `Failed(installState: Int, errorCode: Int)`
  - `sealed interface ModuleInstallOutcome` — `Ready`, `Failed(installState: Int, errorCode: Int)`, `TimedOut`
  - `class SegmentationModuleInstaller @Inject constructor(gateway: ModuleInstallGateway)` — `suspend fun ensureInstalled(): ModuleInstallOutcome`

⚠️ **가시성은 public 이다.** `:data` 에는 `internal` Hilt 모듈도 `internal` Impl 클래스도 없다 —
public `RepositoryModule` 이 `internal` 타입을 `@Binds` 파라미터로 받으면 `EXPOSED_PARAMETER_TYPE`
으로 컴파일이 깨진다. 기존 관례를 따른다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationModuleInstallerTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeModuleInstallGateway(private var available: Boolean = false) : ModuleInstallGateway {
    var installCount: Int = 0
        private set

    private var listener: ((ModuleInstallSignal) -> Unit)? = null

    override suspend fun isAvailable(): Boolean = available

    override fun install(onSignal: (ModuleInstallSignal) -> Unit) {
        installCount++
        listener = onSignal
    }

    /** 게이트웨이가 신호를 흘리는 순간을 테스트가 정한다 */
    fun emit(signal: ModuleInstallSignal, becomesAvailable: Boolean = false) {
        available = becomesAvailable
        listener?.invoke(signal)
    }
}

class SegmentationModuleInstallerTest {
    @Test
    fun ensureInstalled_alreadyAvailable_doesNotRequestInstall() = runTest {
        val gateway = FakeModuleInstallGateway(available = true)
        val installer = SegmentationModuleInstaller(gateway)

        val outcome = installer.ensureInstalled()

        assertEquals(ModuleInstallOutcome.Ready, outcome)
        assertEquals(0, gateway.installCount)
    }

    @Test
    fun ensureInstalled_twoCallers_requestsInstallOnce() = runTest {
        val gateway = FakeModuleInstallGateway()
        val installer = SegmentationModuleInstaller(gateway)

        val first = async { installer.ensureInstalled() }
        val second = async { installer.ensureInstalled() }
        runCurrent()

        gateway.emit(ModuleInstallSignal.Completed, becomesAvailable = true)

        assertEquals(ModuleInstallOutcome.Ready, first.await())
        assertEquals(ModuleInstallOutcome.Ready, second.await())
        assertEquals(1, gateway.installCount)
    }

    @Test
    fun ensureInstalled_firstCallerCancelled_installSurvivesForSecondCaller() = runTest {
        val gateway = FakeModuleInstallGateway()
        val installer = SegmentationModuleInstaller(gateway)

        val first = async { installer.ensureInstalled() }
        runCurrent()
        first.cancel()
        runCurrent()

        val second = async { installer.ensureInstalled() }
        runCurrent()
        gateway.emit(ModuleInstallSignal.Completed, becomesAvailable = true)

        assertEquals(ModuleInstallOutcome.Ready, second.await())
        assertEquals(1, gateway.installCount)
    }

    @Test
    fun ensureInstalled_failedSignal_carriesStateAndErrorCode() = runTest {
        val gateway = FakeModuleInstallGateway()
        val installer = SegmentationModuleInstaller(gateway)

        val outcome = async { installer.ensureInstalled() }
        runCurrent()
        gateway.emit(ModuleInstallSignal.Failed(installState = 5, errorCode = 8))

        assertEquals(ModuleInstallOutcome.Failed(installState = 5, errorCode = 8), outcome.await())
    }

    @Test
    fun ensureInstalled_completedButStillUnavailable_isFailure() = runTest {
        val gateway = FakeModuleInstallGateway()
        val installer = SegmentationModuleInstaller(gateway)

        val outcome = async { installer.ensureInstalled() }
        runCurrent()
        gateway.emit(ModuleInstallSignal.Completed, becomesAvailable = false)

        val failed = assertIs<ModuleInstallOutcome.Failed>(outcome.await())
        assertEquals(STATE_COMPLETED, failed.installState)
    }

    @Test
    fun ensureInstalled_timeout_lettingNextCallerStartAnotherInstall() = runTest {
        val gateway = FakeModuleInstallGateway()
        val installer = SegmentationModuleInstaller(gateway)

        val first = async { installer.ensureInstalled() }
        advanceTimeBy(INSTALL_TIMEOUT_MS + 1)
        advanceUntilIdle()

        assertEquals(ModuleInstallOutcome.TimedOut, first.await())

        val second = async { installer.ensureInstalled() }
        runCurrent()

        assertEquals(2, gateway.installCount)
        gateway.emit(ModuleInstallSignal.Completed, becomesAvailable = true)
        assertEquals(ModuleInstallOutcome.Ready, second.await())
    }

    @Test
    fun ensureInstalled_afterFailure_startsAnotherInstall() = runTest {
        val gateway = FakeModuleInstallGateway()
        val installer = SegmentationModuleInstaller(gateway)

        val first = async { installer.ensureInstalled() }
        runCurrent()
        gateway.emit(ModuleInstallSignal.Failed(installState = 5, errorCode = 8))
        first.await()

        val second = async { installer.ensureInstalled() }
        runCurrent()

        // 끝난 대기를 재사용하면 재시도가 영영 옛 실패만 돌려준다
        assertEquals(2, gateway.installCount)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*SegmentationModuleInstallerTest*'`
Expected: 컴파일 실패 — `ModuleInstallGateway`·`SegmentationModuleInstaller`·`ModuleInstallOutcome`·`INSTALL_TIMEOUT_MS`·`STATE_COMPLETED` 미해결.

- [ ] **Step 3: 이음매를 만든다**

`ModuleInstallGateway.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

/**
 * 모듈 설치의 GMS 쪽 표면. 이 뒤로 Play 서비스 타입이 하나도 새지 않아야 JVM 테스트가 닿는다.
 */
interface ModuleInstallGateway {
    suspend fun isAvailable(): Boolean

    /**
     * ⚠️ [onSignal] 은 GMS 리스너 스레드에서 불린다 — **정지 함수가 아니다.**
     * 받는 쪽은 락을 잡거나 정지 함수를 부를 수 없다.
     */
    fun install(onSignal: (ModuleInstallSignal) -> Unit)
}

sealed interface ModuleInstallSignal {
    data object AlreadyInstalled : ModuleInstallSignal

    data object Completed : ModuleInstallSignal

    /** 취소는 `errorCode` 가 0이라 코드만으로는 실패와 안 갈린다 — 상태를 함께 싣는다 */
    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallSignal
}

sealed interface ModuleInstallOutcome {
    data object Ready : ModuleInstallOutcome

    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallOutcome

    data object TimedOut : ModuleInstallOutcome
}
```

- [ ] **Step 4: 설치기를 만든다**

`SegmentationModuleInstaller.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.utils.repositoryLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

const val INSTALL_TIMEOUT_MS = 20_000L

/** `ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED` 와 같은 값 */
const val STATE_COMPLETED = 4

private const val LOG_PREFIX = "[MLKIT-MODULE]"

/**
 * 모듈 준비의 단일 소유자.
 *
 * ⚠️ **코루틴 스코프를 들지 않는다.** 스코프를 소유하면 취소·에러·재시작을 이 클래스가 증명해야
 * 하는데 그럴 수 없다. 대기는 호출자의 스코프에서 일어나고, 진행 중인 설치만 여기서 공유한다.
 */
@Singleton
class SegmentationModuleInstaller
@Inject
constructor(
    private val gateway: ModuleInstallGateway,
) {
    private val mutex = Mutex()
    private var inFlight: CompletableDeferred<ModuleInstallSignal>? = null

    suspend fun ensureInstalled(): ModuleInstallOutcome {
        if (gateway.isAvailable()) return ModuleInstallOutcome.Ready

        val pending = mutex.withLock {
            // 끝난 대기를 재사용하면 한 번 실패한 뒤 재시도가 영영 그 실패만 돌려준다
            inFlight?.takeIf { !it.isCompleted } ?: startInstall().also { inFlight = it }
        }

        val signal = withTimeoutOrNull(INSTALL_TIMEOUT_MS) { pending.await() }
            ?: return timedOut(pending)

        return signal.toOutcome()
    }

    /**
     * 정지 함수가 아니다 — 콜백이 채울 대기만 만들어 돌려준다.
     */
    private fun startInstall(): CompletableDeferred<ModuleInstallSignal> {
        val deferred = CompletableDeferred<ModuleInstallSignal>()

        gateway.install { signal ->
            repositoryLogger.i { "$LOG_PREFIX 설치 신호 $signal" }
            deferred.complete(signal)
        }

        return deferred
    }

    private suspend fun timedOut(pending: CompletableDeferred<ModuleInstallSignal>): ModuleInstallOutcome {
        repositoryLogger.w { "$LOG_PREFIX 설치 대기가 ${INSTALL_TIMEOUT_MS}ms 를 넘겨 포기한다" }
        mutex.withLock { if (inFlight === pending) inFlight = null }

        return ModuleInstallOutcome.TimedOut
    }

    /**
     * 완료 신호에도 가용 여부를 다시 묻는 이유: 모듈이 깔렸는데 ML Kit 이 못 읽는 상태가 보고된
     * 적이 있다(스펙 「종료 판정」 절). 성공으로 접으면 곧바로 process 가 죽고 로그에는 설치가
     * 성공했다고 남아 원인이 가려진다.
     */
    private suspend fun ModuleInstallSignal.toOutcome(): ModuleInstallOutcome = when (this) {
        is ModuleInstallSignal.Failed -> ModuleInstallOutcome.Failed(installState, errorCode)

        ModuleInstallSignal.AlreadyInstalled,
        ModuleInstallSignal.Completed,
        -> if (gateway.isAvailable()) {
            ModuleInstallOutcome.Ready
        } else {
            ModuleInstallOutcome.Failed(installState = STATE_COMPLETED, errorCode = 0)
        }
    }
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*SegmentationModuleInstallerTest*'`
Expected: PASS (7건).

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/ModuleInstallGateway.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationModuleInstaller.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationModuleInstallerTest.kt
git commit -m "feat: 세그멘테이션 모듈 설치기를 만든다

진행 중인 설치를 여러 호출자가 공유하고, 끝난 대기는 재사용하지 않는다.
GMS 타입은 ModuleInstallGateway 뒤로 숨겨 JVM 테스트가 닿게 한다."
```

---

### Task 2: GMS 게이트웨이 구현과 Hilt 결선

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/PlayServicesModuleInstallGateway.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: Task 1의 `ModuleInstallGateway`·`ModuleInstallSignal`.
- Produces: `class PlayServicesModuleInstallGateway @Inject constructor(context: Context) : ModuleInstallGateway` — Hilt가 `@Binds`로 인터페이스에 묶는다.

⚠️ **이 태스크에는 단위 테스트가 없다.** GMS 설치는 JVM에서 재현할 수 없다. 검증은 컴파일과
Task 7 이후의 실기기 확인이다. 이 사실을 커밋 메시지에 적는다.

- [ ] **Step 1: 게이트웨이 구현을 만든다**

```kotlin
package com.teamyg.parfait.data.repository.image

import android.content.Context
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.teamyg.parfait.data.utils.repositoryLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 모듈 식별자는 세그멘터 옵션과 무관하다 — `SubjectSegmenter` 구현이 옵션이 뭐든 세그멘테이션
 * feature 하나만 내놓는다. 그래서 판정용 세그멘터를 기본 옵션으로 따로 열어도 결과가 같다.
 */
class PlayServicesModuleInstallGateway
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ModuleInstallGateway {
    private val client = ModuleInstall.getClient(context)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            probeSegmenter().use { Tasks.await(client.areModulesAvailable(it)).areModulesAvailable() }
        }.getOrElse { throwable ->
            repositoryLogger.w(throwable) { "[MLKIT-MODULE] 가용 여부 확인이 실패했다 — 없는 것으로 본다" }
            false
        }
    }

    override fun install(onSignal: (ModuleInstallSignal) -> Unit) {
        val segmenter = probeSegmenter()

        lateinit var listener: InstallStatusListener
        listener = InstallStatusListener { update ->
            repositoryLogger.i {
                "[MLKIT-MODULE] 설치 상태 ${update.installState}, 오류 코드 ${update.errorCode}, " +
                    "세션 ${update.sessionId}"
            }

            // STATE_COMPLETED 는 같은 패키지 SegmentationModuleInstaller.kt 의 상수다
            // (GMS 상수와 값이 같고, 설치기가 재확인 실패를 표시할 때도 쓴다)
            when (update.installState) {
                STATE_COMPLETED -> onSignal(ModuleInstallSignal.Completed)
                STATE_FAILED, STATE_CANCELED ->
                    onSignal(ModuleInstallSignal.Failed(update.installState, update.errorCode))
                else -> return@InstallStatusListener
            }

            client.unregisterListener(listener)
            segmenter.close()
        }

        val request = ModuleInstallRequest
            .newBuilder()
            .addApi(segmenter)
            .setListener(listener)
            .build()

        client
            .installModules(request)
            .addOnSuccessListener { response ->
                if (response.areModulesAlreadyInstalled()) {
                    client.unregisterListener(listener)
                    segmenter.close()
                    onSignal(ModuleInstallSignal.AlreadyInstalled)
                }
            }.addOnFailureListener { throwable ->
                repositoryLogger.w(throwable) { "[MLKIT-MODULE] 설치 요청 자체가 실패했다" }
                client.unregisterListener(listener)
                segmenter.close()
                onSignal(ModuleInstallSignal.Failed(installState = STATE_FAILED, errorCode = 0))
            }
    }

    /** 모듈 판정에만 쓰고 process 에 넘기지 않으므로 이 게이트웨이가 열고 닫는다 */
    private fun probeSegmenter() = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().build())
}
```

- [ ] **Step 2: Hilt에 묶는다**

`RepositoryModule.kt`의 `interface RepositoryModule` 안에 다음을 추가한다(다른 `@Binds`와 같은 모양).

```kotlin
    @Binds
    @Singleton
    fun bindModuleInstallGateway(gateway: PlayServicesModuleInstallGateway): ModuleInstallGateway
```

같은 파일 상단에 import 두 줄을 더한다.

```kotlin
import com.teamyg.parfait.data.repository.image.ModuleInstallGateway
import com.teamyg.parfait.data.repository.image.PlayServicesModuleInstallGateway
```

- [ ] **Step 3: 컴파일과 기존 테스트를 확인한다**

Run: `./gradlew :data:compileDebugKotlin :data:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Task 1의 7건을 포함해 기존 테스트가 전부 통과한다.

- [ ] **Step 4: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/PlayServicesModuleInstallGateway.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt
git commit -m "feat: 모듈 설치 게이트웨이의 GMS 구현을 붙인다

installModules 의 Task 성공은 요청 접수일 뿐이라 종료는 InstallStatusListener
로 받는다. GMS 설치는 JVM 에서 재현할 수 없어 이 구현에는 단위 테스트가 없다 —
실기기 확인으로 검증한다."
```

---

### Task 3: 리포지토리를 설치기로 바꾸고 준비 함수를 연다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`

**Interfaces:**
- Consumes: Task 1의 `SegmentationModuleInstaller`·`ModuleInstallOutcome`.
- Produces: `ImageSegmentationRepository.prepareSegmentationModule(): Unit` (suspend).

⚠️ 이 태스크가 **조사용 진단 코드를 정리한다.** 작업 트리에 남아 있는 30초 폴링(`awaitModuleAvailable`,
`MODULE_AVAILABILITY_POLL_COUNT`, `MODULE_AVAILABILITY_POLL_INTERVAL_MS`, `Long.elapsedMs`)과 기존
`ensureModuleInstalled`를 전부 지운다. 상태·실패 코드 로그는 Task 1·2로 옮겨 갔으므로 남는다.

- [ ] **Step 1: 도메인 인터페이스에 준비 함수를 더한다**

`ImageSegmentationRepository`의 `decodeImage` 위에 추가한다.

```kotlin
    /**
     * 세그멘테이션 모델을 미리 받아 둔다. 결과를 돌려주지 않는 이유는 부르는 화면이 그 결과로
     * 할 일이 없어서다 — 실패는 실제로 세그멘테이션을 시도하는 화면이 받는다.
     */
    suspend fun prepareSegmentationModule()
```

- [ ] **Step 2: 리포지토리에서 옛 설치 경로를 걷어낸다**

`ImageSegmentationRepositoryImpl.kt`에서 다음을 **삭제**한다.

- `ensureModuleInstalled` 함수 전체
- `awaitModuleAvailable` 함수 전체
- `elapsedMs` 확장 함수
- 파일 상단 상수 `MODULE_DIAGNOSTIC_PREFIX`·`MODULE_AVAILABILITY_POLL_COUNT`·`MODULE_AVAILABILITY_POLL_INTERVAL_MS`
- import: `InstallStatusListener`, `ModuleInstall`, `ModuleInstallClient`, `ModuleInstallRequest`, `kotlinx.coroutines.delay`

- [ ] **Step 3: 설치기를 주입한다**

생성자에 설치기를 더한다. **가시성은 그대로 public 이다** — `:data` 의 다른 클래스와 같다.

```kotlin
@Singleton
class ImageSegmentationRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val remoteImageDownloadDataSource: RemoteImageDownloadDataSource,
    private val moduleInstaller: SegmentationModuleInstaller,
) : ImageSegmentationRepository {
```

- [ ] **Step 4: 준비 함수와 `runSegmenter`를 잇는다**

`decodeImage` 위에 준비 함수를 더한다.

```kotlin
    override suspend fun prepareSegmentationModule() {
        moduleInstaller.ensureInstalled()
    }
```

`runSegmenter`의 모듈 확인 부분을 바꾼다. 기존의 `if (!ensureModuleInstalled(segmenter))` 블록을
아래로 교체한다.

```kotlin
        val outcome = moduleInstaller.ensureInstalled()
        if (outcome != ModuleInstallOutcome.Ready) {
            repositoryLogger.w { "[MLKIT-MODULE] 모듈 미준비($outcome)로 process 를 건너뛴다" }
            return Result.failure(SegmentationException.ModuleNotReady(null))
        }
```

⚠️ 이 검사는 `segmenter.use { }` **바깥**으로 나온다. 설치기가 자기 세그멘터를 따로 열기 때문에
`process`용 세그멘터를 넘길 이유가 없어졌다.

- [ ] **Step 5: 컴파일과 기존 테스트를 확인한다**

Run: `./gradlew :data:compileDebugKotlin :domain:compileDebugKotlin :data:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt \
        domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt
git commit -m "refactor: 세그멘테이션 모듈 준비를 설치기로 옮긴다

조사용 30초 폴링을 걷고 설치기의 공유 대기를 쓴다. 화면이 미리 부를 수 있게
prepareSegmentationModule 을 계약에 연다."
```

---

### Task 4: 준비 유스케이스

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/PrepareSegmentationModuleUseCase.kt`

**Interfaces:**
- Consumes: Task 3의 `ImageSegmentationRepository.prepareSegmentationModule()`.
- Produces: `class PrepareSegmentationModuleUseCase @Inject constructor(repository: ImageSegmentationRepository)` — `suspend operator fun invoke()`.

- [ ] **Step 1: 유스케이스를 만든다**

같은 디렉토리의 `SegmentImageUseCase`와 같은 모양을 따른다.

```kotlin
package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class PrepareSegmentationModuleUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "PrepareSegmentationModuleUseCase::init" }
    }

    suspend operator fun invoke() = repository.prepareSegmentationModule()
}
```

- [ ] **Step 2: 컴파일을 확인한다**

Run: `./gradlew :domain:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋한다**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/usecase/image/PrepareSegmentationModuleUseCase.kt
git commit -m "feat: 세그멘테이션 모듈 준비 유스케이스를 만든다"
```

---

### Task 5: 실패 원인을 상태로 가르고 재시도를 받는다

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: 없음(도메인 변경에 의존하지 않는다).
- Produces:
  - `enum class SegmentationErrorKind { SubjectNotFound, ModuleNotReady }`
  - `SegmentationState.errorKind: SegmentationErrorKind?` (기존 `isError: Boolean` 대체)
  - `SegmentationIntent.Retry`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

기존 `SegmentationViewModelTest.kt`에서 `assertTrue(viewModel.state.value.isError)` 3곳을 바꾸고
재시도 테스트 둘을 더한다.

바꾸는 3곳(`init_decodeFails_...`, `init_segmentationFails_...`, `init_noSubjectDetected_...`):

```kotlin
        assertEquals(SegmentationErrorKind.SubjectNotFound, viewModel.state.value.errorKind)
```

⚠️ 세 곳 모두 `SubjectNotFound`다. `ModuleNotReady`는 도메인 예외를 보고 가르는데, 이 ViewModel은
`Result.failure`의 예외 타입으로 판정한다. 아래 Step 3의 매핑을 보라.

파일 끝에 추가:

```kotlin
    @Test
    fun retry_afterFailure_runsTheFlowAgainAndClearsTheError() = runTest {
        // Given 첫 시도가 실패한 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(SegmentationErrorKind.SubjectNotFound, viewModel.state.value.errorKind)

        // When 다음 시도는 성공하도록 바꾸고 재시도를 누른다
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate))
        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // Then 실패 표시가 걷히고 후보가 실린다 — 안 걷으면 성공해도 에러 화면이 남는다
        val state = viewModel.state.value
        assertNull(state.errorKind)
        assertEquals(listOf(candidate), state.candidates)
        assertFalse(state.isLoading)
    }

    @Test
    fun retry_pressedTwiceWhileRunning_runsOnce() = runTest {
        // Given 세그멘테이션이 오래 걸리는 상황
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 연달아 두 번 누른다
        viewModel.processIntent(SegmentationIntent.Retry)
        runCurrent()
        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // Then 흐름은 진입 1회 + 재시도 1회로 끝난다 — 두 번째 누름은 버려진다
        coVerify(exactly = 2) { segmentImage(bitmapWrapper) }
    }

    @Test
    fun init_moduleNotReady_marksModuleError() = runTest {
        // Given 모듈을 못 받아 실패한 상황
        coEvery { segmentImage(bitmapWrapper) } returns
            Result.failure(SegmentationException.ModuleNotReady(null))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 대상 못 찾음과 다른 안내를 줄 수 있게 갈라 둔다
        assertEquals(SegmentationErrorKind.ModuleNotReady, viewModel.state.value.errorKind)
    }
```

import 두 줄을 파일 상단에 더한다.

```kotlin
import com.teamyg.parfait.domain.exception.SegmentationException
import kotlin.test.assertNull
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*SegmentationViewModelTest*'`
Expected: 컴파일 실패 — `errorKind`·`SegmentationErrorKind`·`SegmentationIntent.Retry` 미해결.

- [ ] **Step 3: ViewModel을 고친다**

상태와 어휘를 바꾼다.

```kotlin
/** 실패 화면이 무엇을 말할지 가른다 */
enum class SegmentationErrorKind {
    /** 후보도 폴백도 못 얻었다 */
    SubjectNotFound,

    /** 세그멘테이션 모델을 못 받았다 */
    ModuleNotReady,
}

data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    /** 널이 아니면 화면 전체가 `C-103-Error` 로 바뀐다 */
    val errorKind: SegmentationErrorKind? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent

    data object Retry : SegmentationIntent
}
```

`init` 블록을 함수 호출로 바꾸고 흐름을 함수로 뺀다.

```kotlin
    init {
        loadCandidates()
    }

    /**
     * ⚠️ 진입과 재시도가 **같은 키**를 쓴다. 진입만 다른 경로로 띄우면 진입 흐름이 도는 중에
     * 누른 재시도를 막지 못한다.
     */
    private fun loadCandidates() {
        launch(key = LOAD_CANDIDATES_KEY) {
            // 실패 표시를 걷지 않으면 재시도가 성공해도 에러 화면이 그대로 남는다
            updateState { copy(isLoading = true, errorKind = null, candidates = emptyList()) }

            runSuspendCatching { clearSegmentationCacheUseCase() }

            val bitmapWrapper = decodeImageUseCase(sourceImageUri).getOrNull()

            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false, errorKind = SegmentationErrorKind.SubjectNotFound) }
                return@launch
            }

            runSuspendCatching { addRecentImageUseCase(source = sourceImageUri, kind = RecentImageKind.SOURCE) }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        updateState { copy(errorKind = SegmentationErrorKind.SubjectNotFound) }
                        return@onSuccess
                    }

                    updateState { copy(candidates = candidates) }
                }.onFailure { throwable ->
                    // 원인을 삼키면 모듈 미설치와 처리 실패가 화면에서 똑같아 보인다
                    viewModelLogger.e(throwable) {
                        "세그멘테이션 실패 ${throwable::class.simpleName}, 원인 ${throwable.cause}"
                    }
                    updateState { copy(errorKind = throwable.toErrorKind()) }
                }

            updateState { copy(isLoading = false) }
        }
    }

    private fun Throwable.toErrorKind(): SegmentationErrorKind = when (this) {
        is SegmentationException.ModuleNotReady -> SegmentationErrorKind.ModuleNotReady
        else -> SegmentationErrorKind.SubjectNotFound
    }
```

`processIntent`에 갈래를 더한다.

```kotlin
    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)
            SegmentationIntent.Retry -> loadCandidates()
        }
    }
```

파일 하단 상수에 키를 더한다(기존 `SELECT_CANDIDATE_KEY` 옆).

```kotlin
private const val LOAD_CANDIDATES_KEY = "loadCandidates"
```

import를 더한다.

```kotlin
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.core.ui.viewModelLogger
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest`
Expected: PASS. 기존 테스트도 전부 통과한다(`isError` 단언 3건이 `errorKind`로 바뀐 상태).

- [ ] **Step 5: 커밋한다**

```bash
git add feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt \
        feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt
git commit -m "feat: 세그멘테이션 실패 원인을 상태로 가르고 재시도를 받는다

모듈 미준비와 대상 못 찾음은 사용자가 할 일이 달라 안내를 갈라야 한다.
재시도는 진입과 같은 흐름을 같은 키로 다시 태우고 실패 표시를 되돌린다."
```

---

### Task 6: 실패 화면 문구와 재시도 버튼

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/SegmentationErrorScreen.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 5의 `SegmentationErrorKind`·`SegmentationIntent.Retry`.
- Produces: `SegmentationErrorScreen(title: String, description: String, onClickRetry: () -> Unit, onClickClose: () -> Unit, modifier: Modifier)`.

⚠️ **재시도 버튼은 디자인 검토를 받으려고 먼저 놓는 시안이다.** 새 컴포넌트를 만들지 않는다.

- [ ] **Step 1: 문구를 더한다**

`strings.xml`에 추가한다.

```xml
    <string name="segmentation_module_error_title">사진 편집 기능을 준비하지 못했어요</string>
    <string name="segmentation_module_error_description">네트워크 상태를 확인하고 잠시 후 다시 시도해 주세요</string>
    <string name="segmentation_error_retry">다시 시도</string>
```

- [ ] **Step 2: 실패 화면이 문구와 재시도를 받게 연다**

`SegmentationErrorScreen`의 시그니처를 바꾸고, `stringResource` 직접 호출을 파라미터로 교체한다.

```kotlin
@Composable
internal fun SegmentationErrorScreen(
    title: String,
    description: String,
    onClickRetry: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

안쪽 `Text` 둘의 `text =`를 `title`·`description`으로 바꾼다. 설명 `Text` 아래, 같은 `Column` 안에
버튼을 더한다.

```kotlin
                    YGButton(
                        text = stringResource(R.string.segmentation_error_retry),
                        buttonType = YGButtonType.Medium.Primary,
                        isEnabled = true,
                        onClick = onClickRetry,
                    )
```

import를 더한다.

```kotlin
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
```

프리뷰도 새 시그니처에 맞춘다.

```kotlin
@YGPreview
@Composable
private fun PreviewSegmentationErrorScreen() = PreviewBox {
    SegmentationErrorScreen(
        title = stringResource(R.string.segmentation_error_title),
        description = stringResource(R.string.segmentation_error_description),
        onClickRetry = {},
        onClickClose = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

- [ ] **Step 3: Route가 원인으로 문구를 고르게 한다**

`SegmentationRoute`의 `if (state.isError)` 블록을 바꾼다.

```kotlin
        val errorKind = state.errorKind

        if (errorKind != null) {
            SegmentationErrorScreen(
                title = stringResource(errorKind.titleRes()),
                description = stringResource(errorKind.descriptionRes()),
                onClickRetry = { viewModel.processIntent(SegmentationIntent.Retry) },
                onClickClose = onClickClose,
                modifier = modifier.padding(innerPadding),
            )
        } else {
```

파일 하단에 매핑을 더한다.

```kotlin
@StringRes
private fun SegmentationErrorKind.titleRes(): Int = when (this) {
    SegmentationErrorKind.SubjectNotFound -> R.string.segmentation_error_title
    SegmentationErrorKind.ModuleNotReady -> R.string.segmentation_module_error_title
}

@StringRes
private fun SegmentationErrorKind.descriptionRes(): Int = when (this) {
    SegmentationErrorKind.SubjectNotFound -> R.string.segmentation_error_description
    SegmentationErrorKind.ModuleNotReady -> R.string.segmentation_module_error_description
}
```

import를 더한다.

```kotlin
import androidx.annotation.StringRes
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationErrorKind
```

- [ ] **Step 4: 컴파일과 테스트를 확인한다**

Run: `./gradlew :feature:segmentation:impl:assembleDebug :feature:segmentation:impl:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋한다**

```bash
git add feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/SegmentationErrorScreen.kt \
        feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt \
        feature/segmentation/impl/src/main/res/values/strings.xml
git commit -m "feat: 실패 원인별 문구와 재시도 버튼을 실패 화면에 놓는다

모듈을 못 받은 상황에서 '다른 사진을 선택하세요'는 틀린 지시다.
재시도 버튼은 디자인 검토를 받으려고 먼저 놓는 시안이라 새 컴포넌트를
만들지 않고 YGButton 을 그대로 쓴다."
```

---

### Task 7: 사진 확인 화면에서 모듈을 미리 받는다

**Files:**
- Create: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/viewmodel/PictureConfirmViewModel.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/PictureConfirmRoute.kt`

**Interfaces:**
- Consumes: Task 4의 `PrepareSegmentationModuleUseCase`.
- Produces: `PictureConfirmViewModel` — 상태 없음. `hiltViewModel()`로 얻는다.

⚠️ **이 태스크에는 단위 테스트가 없다.** `feature/camera/impl`에는 `parfait.test.unit` 플러그인도
`src/test`도 없고, 이 계획은 새 테스트 하니스를 만들지 않는다(Global Constraints). 검증은 컴파일과
Step 4의 실기기 확인이다.

- [ ] **Step 1: 준비만 거는 ViewModel을 만든다**

```kotlin
package com.teamyg.parfait.feature.camera.impl.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.usecase.image.PrepareSegmentationModuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 사진 확인 화면이 세그멘테이션 모델을 미리 받아 두는 자리.
 *
 * ⚠️ **카메라 화면이 아니라 여기인 이유**는 진입 경로가 둘이기 때문이다 — 촬영과 갤러리 선택이
 * 이 화면에서 합쳐지고, 세그멘테이션으로 가는 길은 여기 하나다. 근거는
 * `parfait/specs/2026-09-02-segmentation-module-install.md`.
 */
@HiltViewModel
class PictureConfirmViewModel
@Inject
constructor(
    private val prepareSegmentationModuleUseCase: PrepareSegmentationModuleUseCase,
) : ViewModel() {
    /**
     * 결과를 안 본다. 실패는 실제로 세그멘테이션을 시도하는 화면이 받고, 이 화면은 사용자를
     * 붙잡지 않는다.
     */
    fun prepareSegmentationModule() {
        viewModelScope.launch {
            runSuspendCatching { prepareSegmentationModuleUseCase() }
                .onFailure { viewModelLogger.w(it) { "세그멘테이션 모듈 사전 준비가 실패했다" } }
        }
    }
}
```

- [ ] **Step 2: Route에 붙인다**

`PictureConfirmRoute`의 `YGScaffoldV2` 위에 다음을 더한다.

```kotlin
    val viewModel: PictureConfirmViewModel = hiltViewModel()

    // 배경 편집에서 온 경로는 세그멘테이션으로 가지 않으므로 헛일을 안 한다
    LaunchedEffect(returnResultOnly) {
        if (!returnResultOnly) viewModel.prepareSegmentationModule()
    }
```

import를 더한다.

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.feature.camera.impl.viewmodel.PictureConfirmViewModel
```

- [ ] **Step 3: 빌드하고 설치한다**

Run: `./gradlew :app:installDebug`
Expected: `Installed on 1 device.`

- [ ] **Step 4: 실기기로 확인한다**

두 진입 경로를 각각 한 번씩 탄다. ① 카메라로 촬영 → 확인 → 편집, ② 갤러리에서 선택 → 확인 → 편집.

Run: `adb logcat -b all -v time | grep -E 'MLKIT-MODULE|세그멘테이션 실패'`

Expected(모듈이 없는 기기): 사진 확인 화면 진입 시점에 `[MLKIT-MODULE] 설치 신호 ...`가 찍히고,
편집 화면이 같은 대기에 붙어 **설치 요청이 한 번만** 나간다. 실패하면 실패 화면에 "사진 편집
기능을 준비하지 못했어요"와 재시도 버튼이 뜬다. 재시도를 누르면 새 설치 요청이 나간다.

Expected(모듈이 있는 기기): 두 경로 모두 평소처럼 후보 화면으로 넘어간다.

- [ ] **Step 5: 커밋한다**

```bash
git add feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/viewmodel/PictureConfirmViewModel.kt \
        feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/PictureConfirmRoute.kt
git commit -m "feat: 사진 확인 화면에서 세그멘테이션 모듈을 미리 받는다

촬영과 갤러리 선택이 합쳐지는 유일한 지점이라, 카메라에만 걸면 갤러리로
들어온 사용자가 사전 준비를 못 탄다. 배경 편집 경로는 세그멘테이션으로
가지 않으므로 걸지 않는다."
```

---

## 마무리 확인

- [ ] `./gradlew :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest` 전부 통과
- [ ] `./gradlew :app:assembleDebug` 성공
- [ ] Task 7 Step 4의 두 경로 실기기 확인 완료
- [ ] 작업 트리에 조사용 진단 코드(30초 폴링)가 남아 있지 않다
- [ ] push·PR은 하지 않았다
