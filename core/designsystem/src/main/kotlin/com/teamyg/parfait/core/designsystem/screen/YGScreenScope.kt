package com.teamyg.parfait.core.designsystem.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class YGScreenScope {
    @Composable
    fun OnBack(
        enabled: Boolean = true,
        handler: () -> Unit,
    ) {
        BackHandler(enabled = enabled, onBack = handler)
    }
}
