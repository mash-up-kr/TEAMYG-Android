package com.teamyg.parfait.feature.canvas.impl.route

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.feature.canvas.impl.screen.CanvasImageAddScreen
import com.teamyg.parfait.feature.canvas.impl.viewmodel.CanvasImageAddViewModel
import com.teamyg.parfait.core.navigation.Navigator
import androidx.core.net.toUri
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.canvas.impl.viewmodel.CanvasImageAddEffect
import com.teamyg.parfait.feature.canvas.impl.viewmodel.CanvasImageAddIntent
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun CanvasImageAddRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasImageAddViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    ResultEffect<String> { imageUri ->
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                imageUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

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
