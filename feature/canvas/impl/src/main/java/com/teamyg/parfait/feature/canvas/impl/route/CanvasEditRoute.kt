package com.teamyg.parfait.feature.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.feature.canvas.impl.screen.CanvasEditScreen
import com.teamyg.navigation.Navigator

@Composable
internal fun CanvasEditRoute(
    imageUri: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    CanvasEditScreen(
        onBack = navigator::onBack,
        modifier = modifier,
    )
}
