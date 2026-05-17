package com.tjyg.core.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.designsystem.theme.TempYGMaterialTheme

@Composable
fun PreviewBox(content: @Composable BoxScope.() -> Unit) {
    TempYGMaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }
}
