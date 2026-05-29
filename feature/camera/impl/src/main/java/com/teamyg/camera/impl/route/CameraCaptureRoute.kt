package com.teamyg.camera.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.camera.impl.screen.CameraCaptureScreen
import com.teamyg.navigation.Navigator

@Composable
internal fun CameraCaptureRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    CameraCaptureScreen(
        modifier = modifier,
        onClickConfirm = {
            resultEventBus.sendResult("content://placeholder/camera/captured.jpg")
            navigator.onBack()
        },
    )
}
