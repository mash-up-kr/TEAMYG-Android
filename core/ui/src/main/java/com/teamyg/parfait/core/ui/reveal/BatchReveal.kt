package com.teamyg.parfait.core.ui.reveal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 여러 원격 이미지를 한 묶음으로 보고, 전부 결말날 때까지 가렸다가 한 번에 드러낸다.
 * [settled] 에는 성공과 실패를 모두 결말로 넣어야 한다.
 *
 * @param resetKey 바뀌면 처음부터 다시 모은다. 화면은 그대로인데 보여 줄 대상만 통째로
 *   바뀌는 자리에 넘긴다 — 안 넘기면 빗장이 풀린 채라 다음 대상이 하나씩 따로 뜬다
 */
@Composable
fun rememberBatchReveal(
    settled: List<Boolean>,
    resetKey: Any? = Unit,
): Boolean {
    var ready by remember(resetKey) { mutableStateOf(false) }
    val batchReady = isBatchReady(settled)

    LaunchedEffect(resetKey, batchReady) {
        if (batchReady) ready = true
    }

    return isBatchShown(settled = settled, ready = ready)
}

/**
 * 빗장을 풀 때가 됐는가. **빈 목록은 완료가 아니다** — 조회가 오기 전에는 기다릴 대상
 * 자체가 없는데, 그 순간을 완료로 세면 빗장이 먼저 풀려 뒤늦게 온 것들이 하나씩 뜬다.
 */
fun isBatchReady(settled: List<Boolean>): Boolean = settled.isNotEmpty() && settled.all { it }

/**
 * 지금 보여야 하는가. 한 번 [ready] 가 된 뒤에는 다시 가리지 않는다 — 항목이 하나
 * 추가될 때마다 화면 전체가 사라졌다 나타나면 더 거슬린다.
 */
fun isBatchShown(
    settled: List<Boolean>,
    ready: Boolean,
): Boolean = ready || settled.isEmpty()
