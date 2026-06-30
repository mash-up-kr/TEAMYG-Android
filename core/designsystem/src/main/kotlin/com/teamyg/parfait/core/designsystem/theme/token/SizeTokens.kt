package com.teamyg.parfait.core.designsystem.theme.token

import androidx.compose.ui.unit.dp

@JvmInline
value class SizeToken(val size: Int) {
    fun getDp() = size.dp
}

object SizeTokens {
    val Size2: SizeToken = SizeToken(2)
    val Size4: SizeToken = SizeToken(4)
    val Size6: SizeToken = SizeToken(6)
    val Size8: SizeToken = SizeToken(8)
    val Size10: SizeToken = SizeToken(10)
    val Size12: SizeToken = SizeToken(12)
    val Size16: SizeToken = SizeToken(16)
    val Size20: SizeToken = SizeToken(20)
    val Size24: SizeToken = SizeToken(24)
    val Size32: SizeToken = SizeToken(32)
    val Size40: SizeToken = SizeToken(40)
    val Size48: SizeToken = SizeToken(48)
    val Size64: SizeToken = SizeToken(64)
    val Size80: SizeToken = SizeToken(80)
}
