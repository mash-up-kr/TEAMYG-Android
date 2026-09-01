package com.teamyg.parfait.core.ui.reveal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * 한 묶음을 한꺼번에 내지 않고 [stepMillis] 간격으로 하나씩 드러낸다.
 * [rememberBatchReveal] 로 다 모은 뒤 이어서 쓴다.
 *
 * 드러난 개수만 돌려주고 어느 자리가 그 개수에 드는지는 [isStaggerRevealed] 가 정한다.
 * 개수는 줄지 않는다 — 항목이 늘면 이미 나온 것은 두고 새 것만 이어서 쌓인다.
 */
@Composable
fun rememberStaggeredReveal(
    total: Int,
    started: Boolean,
    stepMillis: Long,
): Int {
    var revealedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(started, total) {
        if (!started) return@LaunchedEffect

        // 첫 자리는 기다리지 않고 낸다 — 덮개가 걷힌 뒤 한 박자 비면 멈춘 것처럼 보인다
        while (revealedCount < total) {
            revealedCount++
            if (revealedCount < total) delay(stepMillis)
        }
    }

    return revealedCount
}

/**
 * [index] 자리가 드러날 차례를 지났는가. **아래에서 위로 쌓인다** — 목록의 마지막 자리가
 * 화면 맨 아래라 그것부터 나온다.
 */
fun isStaggerRevealed(
    index: Int,
    total: Int,
    revealedCount: Int,
): Boolean = total - index <= revealedCount
