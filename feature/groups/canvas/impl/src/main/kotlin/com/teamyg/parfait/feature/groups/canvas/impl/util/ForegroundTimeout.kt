package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val FOREGROUND_TICK_MILLIS = 250L

/**
 * [block] 을 돌리되 화면이 STARTED 인 동안 흐른 시간만 세어 [timeoutMillis] 를 넘기면 취소하고 null 을 돌려준다.
 *
 * 벽시계 `withTimeoutOrNull` 을 쓰지 않는 이유: 백그라운드에서는 프레임이 오지 않아 녹화가 멈춰 있는데
 * 시계만 흘러, 돌아오면 기다리면 됐을 녹화가 실패로 끝난다.
 *
 * 바깥이 취소된 것은 null 로 접지 않고 그대로 던진다
 */
internal suspend fun <T> withForegroundTimeoutOrNull(
    lifecycle: Lifecycle,
    timeoutMillis: Long,
    block: suspend () -> T,
): T? = coroutineScope {
    val work = async { block() }
    val watchdog = launch {
        var foregroundMillis = 0L
        while (foregroundMillis < timeoutMillis) {
            // 백그라운드면 여기서 멈춰 시간을 세지 않는다
            lifecycle.withStarted {}
            delay(FOREGROUND_TICK_MILLIS)
            foregroundMillis += FOREGROUND_TICK_MILLIS
        }
        work.cancel()
    }
    try {
        work.await()
    } catch (e: CancellationException) {
        currentCoroutineContext().ensureActive()
        null
    } finally {
        watchdog.cancel()
    }
}
