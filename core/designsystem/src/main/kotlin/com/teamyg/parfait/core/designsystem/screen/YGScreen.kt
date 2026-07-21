package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun YGScreen(
    modifier: Modifier = Modifier,
    content: @Composable YGScreenScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
    ) {
        val scope = remember { YGScreenScope() }
        scope.content()
    }
}
