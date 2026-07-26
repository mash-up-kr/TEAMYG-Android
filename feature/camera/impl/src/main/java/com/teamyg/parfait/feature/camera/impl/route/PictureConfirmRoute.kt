package com.teamyg.parfait.feature.camera.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.impl.screen.PictureConfirmScreen

@Composable
internal fun PictureConfirmRoute(
    uri: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    PictureConfirmScreen(
        uri = uri,
        onClickConfirm = { navigator.onBack() },
        modifier = modifier,
    )
}
