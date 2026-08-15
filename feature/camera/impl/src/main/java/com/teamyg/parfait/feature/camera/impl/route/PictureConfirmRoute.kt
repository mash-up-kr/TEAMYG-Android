package com.teamyg.parfait.feature.camera.impl.route

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.screen.PictureConfirmScreen
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun PictureConfirmRoute(
    uri: String,
    source: PictureConfirmSource,
    returnResultOnly: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    PictureConfirmScreen(
        uri = uri,
        source = source,
        onClickReCapture = { navigator.onBack() },
        onClickConfirm = {
            if (returnResultOnly) {
                resultEventBus.sendResult(PictureConfirmResult(uri = uri, source = source))
                navigator.onBack() // PictureConfirm
                navigator.onBack() // Camera/Gallery
            } else {
                navigator.goToAndPopCurrent(
                    destination = NavKeySegmentation(
                        sourceImageUri = uri,
                    ),
                )
            }
        },
        onClickClose = {}, // TODO("c001-캔버스메인으로 넘어가야함")
        modifier = modifier,
    )
}
