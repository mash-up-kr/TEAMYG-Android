package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMoveScreen

@Composable
internal fun CanvasMoveRoute(
    image: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    CanvasMoveScreen(
        modifier = modifier,
        image = image,
    )
}
