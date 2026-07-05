package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasEditScreen
import com.teamyg.parfait.core.navigation.Navigator

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
