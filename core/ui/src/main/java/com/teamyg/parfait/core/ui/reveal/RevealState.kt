package com.teamyg.parfait.core.ui.reveal

import androidx.compose.runtime.Stable

/**
 * 드러내는 방식(한꺼번에·순차)이 달라도 그리는 쪽과 배치하는 쪽은 이 답만 보면 된다.
 */
@Stable
interface RevealState {
    fun isRevealed(index: Int): Boolean

    companion object {
        val AllRevealed: RevealState = object : RevealState {
            override fun isRevealed(index: Int): Boolean = true
        }
    }
}
