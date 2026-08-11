package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasBGEditScreen

private val PictureConfirmSourceSaver = Saver<PictureConfirmSource?, String>(
    save = { it?.name.orEmpty() },
    restore = { if (it.isEmpty()) null else PictureConfirmSource.valueOf(it) },
)

@Composable
internal fun CanvasBGEditRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    var backgroundImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var backgroundImageSource by rememberSaveable(stateSaver = PictureConfirmSourceSaver) {
        mutableStateOf<PictureConfirmSource?>(null)
    }

    ResultEffect<PictureConfirmResult> { result ->
        backgroundImageUri = result.uri
        backgroundImageSource = result.source
    }

    CanvasBGEditScreen(
        backgroundImageUri = backgroundImageUri,
        backgroundImageSource = backgroundImageSource,
        onClickClose = { navigator.onBack() },
        onClickConfirm = {
            // TODO: 선택한 배경색을 이전 화면으로 전달하는 방식 연동 필요
            navigator.onBack()
        },
        onClickCamera = {
            navigator.goTo(destination = NavKeyCameraCustom(showGuideToast = false, returnResultOnly = true))
        },
        onClickGallery = {
            navigator.goTo(destination = NavKeyCustomGalleryPicker(showGuideToast = false, returnResultOnly = true))
        },
        modifier = modifier,
    )
}
