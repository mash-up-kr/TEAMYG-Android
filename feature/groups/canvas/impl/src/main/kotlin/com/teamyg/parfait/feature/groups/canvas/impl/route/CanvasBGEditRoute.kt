package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasBGEditScreen

@Composable
internal fun CanvasBGEditRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    var backgroundImageUri by remember { mutableStateOf<String?>(null) }

    ResultEffect<String> { uri ->
        backgroundImageUri = uri
    }

    CanvasBGEditScreen(
        backgroundImageUri = backgroundImageUri,
        onClickClose = { navigator.onBack() },
        onClickConfirm = {
            // TODO: 선택한 배경색을 이전 화면으로 전달하는 방식 연동 필요
            navigator.onBack()
        },
        onClickCamera = { navigator.goTo(destination = NavKeyCameraCustom) },
        onClickGallery = { navigator.goTo(destination = NavKeyCustomGalleryPicker) },
        modifier = modifier,
    )
}
