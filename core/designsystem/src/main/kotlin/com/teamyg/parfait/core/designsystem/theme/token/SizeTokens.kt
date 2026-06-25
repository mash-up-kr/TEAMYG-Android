package com.teamyg.parfait.core.designsystem.theme.token

import androidx.compose.ui.unit.dp

@JvmInline
value class SizeToken(val size: Int) {
    fun getDp() = size.dp
}

object SizeTokens {
    val Size2 = SizeToken(2)
    val Size4 = SizeToken(4)
    val Size6 = SizeToken(6)
    val Size8 = SizeToken(8)
    val Size10 = SizeToken(10)
    val Size12 = SizeToken(12)
}
