package com.teamyg.parfait.feature.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.feature.canvas.api.NavKeyCanvasEdit
import com.teamyg.parfait.feature.canvas.impl.screen.CanvasImageSelectScreen
import com.teamyg.navigation.Navigator

@Composable
internal fun CanvasImageSelectRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    CanvasImageSelectScreen(
        onClickImage = { navigator.goTo(NavKeyCanvasEdit(imageUri = "nukkiii")) },
        modifier = modifier,
    )
}
