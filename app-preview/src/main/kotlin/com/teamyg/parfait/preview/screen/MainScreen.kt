package com.teamyg.parfait.preview.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun MainScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Preview Application",
        )
    }
}

@YGPreview
@Composable
private fun PreviewMainScreen() = PreviewBox {
    MainScreen(
        modifier = Modifier.fillMaxSize(),
    )
}
