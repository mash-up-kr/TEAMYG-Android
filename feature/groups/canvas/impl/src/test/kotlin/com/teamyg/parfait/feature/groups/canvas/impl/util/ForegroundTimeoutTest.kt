package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.teamyg.parfait.core.testing.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForegroundTimeoutTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val owner = object : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun finishesInTime_returnsResult() = runTest(mainDispatcherRule.dispatcher) {
        owner.registry.currentState = Lifecycle.State.RESUMED

        val result = withForegroundTimeoutOrNull(owner.lifecycle, timeoutMillis = 1_000L) {
            delay(500L)
            "done"
        }

        assertEquals("done", result)
    }

    @Test
    fun exceedsForegroundTime_cancelsBlockAndReturnsNull() = runTest(mainDispatcherRule.dispatcher) {
        owner.registry.currentState = Lifecycle.State.RESUMED
        var blockCancelled = false

        val result = withForegroundTimeoutOrNull(owner.lifecycle, timeoutMillis = 1_000L) {
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                // 녹화라면 여기서 부분 파일을 지운다
                blockCancelled = true
                throw e
            }
        }

        assertNull(result)
        assertTrue(blockCancelled)
    }

    @Test
    fun backgroundTime_isNotCounted() = runTest(mainDispatcherRule.dispatcher) {
        owner.registry.currentState = Lifecycle.State.CREATED

        val deferred = async {
            withForegroundTimeoutOrNull(owner.lifecycle, timeoutMillis = 1_000L) {
                delay(5_000L)
                "done"
            }
        }

        // 백그라운드에서 5초가 지나도 시간 초과로 끊지 않는다
        advanceTimeBy(4_900L)
        runCurrent()
        assertFalse(deferred.isCompleted)

        advanceTimeBy(200L)
        runCurrent()
        assertEquals("done", deferred.await())
    }

    @Test
    fun outerCancellation_isRethrownNotSwallowed() = runTest(mainDispatcherRule.dispatcher) {
        owner.registry.currentState = Lifecycle.State.RESUMED
        var swallowed = false

        val job = launch {
            withForegroundTimeoutOrNull(owner.lifecycle, timeoutMillis = 10_000L) { awaitCancellation() }
            // 바깥 취소를 null 로 접으면 여기까지 온다
            swallowed = true
        }
        advanceTimeBy(100L)
        job.cancel()
        runCurrent()

        assertTrue(job.isCancelled)
        assertFalse(swallowed)
    }
}
