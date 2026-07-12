package com.teamyg.parfait.core.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme

@Composable
fun PreviewBox(content: @Composable BoxScope.() -> Unit) {
    YGCustomTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }
}
