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
    fun emit(
        signal: ModuleInstallSignal,
        becomesAvailable: Boolean = false,
    ) {
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
        assertEquals(STATE_COMPLETED_BUT_UNAVAILABLE, failed.installState)
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
