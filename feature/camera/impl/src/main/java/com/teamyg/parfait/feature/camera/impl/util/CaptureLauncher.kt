package com.teamyg.parfait.feature.camera.impl.util

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.feature.camera.impl.model.CaptureOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** [capture]를 띄우고 결과를 [onOutcome]에 정확히 한 번 알린다. */
internal fun CoroutineScope.launchCapture(
    capture: suspend () -> Unit,
    onOutcome: (CaptureOutcome) -> Unit,
): Job {
    val job = launch {
        val isSaved = runSuspendCatching { capture() }.isSuccess
        onOutcome(if (isSaved) CaptureOutcome.SAVED else CaptureOutcome.FAILED)
    }
    job.invokeOnCompletion { cause ->
        // 호출부만 사라지고 결과를 받는 쪽은 남는 경우(재생성 등) 셔터가 잠긴 채 남지 않게 한다.
        // 본문 시작 전에 취소된 잡도 여기서 잡힌다
        if (cause is CancellationException) onOutcome(CaptureOutcome.CANCELLED)
    }
    return job
}
