package com.teamyg.parfait.feature.camera.impl.util

import com.teamyg.parfait.feature.camera.impl.model.CaptureOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class CaptureLauncherTest {
    @Test
    fun launchCapture_success_reportsSavedOnce() = runTest {
        val outcomes = mutableListOf<CaptureOutcome>()

        launchCapture(capture = {}, onOutcome = outcomes::add).join()

        assertEquals(listOf(CaptureOutcome.SAVED), outcomes)
    }

    @Test
    fun launchCapture_failure_reportsFailedOnce() = runTest {
        val outcomes = mutableListOf<CaptureOutcome>()

        launchCapture(capture = { error("촬영 실패") }, onOutcome = outcomes::add).join()

        assertEquals(listOf(CaptureOutcome.FAILED), outcomes)
    }

    @Test
    fun launchCapture_cancelledWhileRunning_reportsCancelledOnce() = runTest {
        val outcomes = mutableListOf<CaptureOutcome>()
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

        val job = scope.launchCapture(capture = { awaitCancellation() }, onOutcome = outcomes::add)
        job.cancel()
        job.join()

        assertEquals(listOf(CaptureOutcome.CANCELLED), outcomes)
    }

    @Test
    fun launchCapture_cancelledBeforeStart_reportsCancelledOnce() = runTest {
        val outcomes = mutableListOf<CaptureOutcome>()
        var hasStarted = false
        val scope = TestScope(StandardTestDispatcher(testScheduler))

        val job = scope.launchCapture(capture = { hasStarted = true }, onOutcome = outcomes::add)
        job.cancel()
        scope.runCurrent()
        job.join()

        assertFalse(hasStarted)
        assertEquals(listOf(CaptureOutcome.CANCELLED), outcomes)
    }
}
