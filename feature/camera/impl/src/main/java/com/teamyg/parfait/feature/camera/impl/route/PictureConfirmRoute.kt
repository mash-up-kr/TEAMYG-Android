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
        onClickReCapture = { navigator.onBack() },
        onClickConfirm = { navigator.onBack() }, // 로딩페이지로 넘어가야댐
        modifier = modifier,
    )
}
