package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasImageAddScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddIntent
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun CanvasImageAddRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasImageAddViewModel = hiltViewModel(),
) {
    ResultEffect<String> { imageUri ->
        viewModel.processIntent(CanvasImageAddIntent.CacheImage(imageUri))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CanvasImageAddEffect.NavigateToCamera -> navigator.goTo(
                    destination = NavKeyCameraCustom,
                )

                is CanvasImageAddEffect.NavigateToCanvas -> navigator.goTo(
                    destination = NavKeyCustomGalleryPicker,
                )

                is CanvasImageAddEffect.NavigateToSegmentation -> navigator.goTo(
                    destination = NavKeySegmentation(
                        sourceImageUri = effect.uri,
                    ),
                )
            }
        }
    }

    CanvasImageAddScreen(
        modifier = modifier,
        onClickCamera = { viewModel.processIntent(CanvasImageAddIntent.OnClickCamera()) },
        onClickGallery = { viewModel.processIntent(CanvasImageAddIntent.OnClickCanvas()) },
    )
}
