package com.teamyg.parfait.core.ui.reveal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * 한 묶음을 한꺼번에 내지 않고 [stepMillis] 간격으로 하나씩 드러낸다.
 */
@Stable
class StaggeredRevealState(
    private val stepMillis: Long,
) : RevealState {
    private var revealedCount by mutableIntStateOf(0)
    private var allRevealed by mutableStateOf(false)

    override fun isRevealed(index: Int): Boolean = allRevealed || index < revealedCount

    /** 다시 불러도 처음부터 세지 않는다 — 늘어난 만큼만 이어서 쌓인다 */
    suspend fun reveal(total: Int) {
        // 첫 자리는 기다리지 않고 낸다 — 덮개가 걷힌 뒤 한 박자 비면 멈춘 것처럼 보인다
        while (revealedCount < total) {
            revealedCount++
            if (revealedCount < total) delay(stepMillis)
        }
    }

    fun revealAll() {
        allRevealed = true
    }
}

/**
 * @param staggered 끄면 대기 코루틴 없이 통째로 낸다. 재진입처럼 이미 본 목록을 다시
 *   쌓지 않는 자리에 쓴다
 */
@Composable
fun rememberStaggeredRevealState(
    total: Int,
    started: Boolean,
    stepMillis: Long,
    staggered: Boolean = true,
): StaggeredRevealState {
    val state = remember(stepMillis) { StaggeredRevealState(stepMillis) }

    LaunchedEffect(state, started, total, staggered) {
        when {
            !staggered -> state.revealAll()
            started -> state.reveal(total)
        }
    }

    return state
}
