package com.teamyg.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.camera.api.NavKeyCameraCapture
import com.teamyg.canvas.impl.screen.CanvasImageAddScreen
import com.teamyg.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.navigation.Navigator
import com.teamyg.segmentation.api.NavKeySegmentation

@Composable
internal fun CanvasImageAddRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    ResultEffect<String> { imageUri ->
        // 카메라/갤러리에서 돌아온 이미지 URI를 받아 누끼 화면으로 분기
        navigator.goTo(NavKeySegmentation(sourceImageUri = imageUri))
    }

    CanvasImageAddScreen(
        modifier = modifier,
        onClickCamera = { navigator.goTo(NavKeyCameraCapture) },
        onClickGallery = { navigator.goTo(NavKeySystemGalleryPicker) },
    )
}
