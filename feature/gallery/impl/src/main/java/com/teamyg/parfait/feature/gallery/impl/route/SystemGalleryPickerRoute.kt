package com.teamyg.parfait.feature.gallery.impl.route

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.feature.gallery.impl.screen.SystemGalleryPickerScreen
import com.teamyg.parfait.feature.gallery.impl.viewmodel.SystemGalleryIntent
import com.teamyg.parfait.feature.gallery.impl.viewmodel.SystemGalleryPickerViewModel
import com.teamyg.parfait.feature.gallery.impl.viewmodel.SystemGallerySideEffect
import com.teamyg.parfait.core.navigation.Navigator

@Composable
internal fun SystemGalleryPickerRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SystemGalleryPickerViewModel = hiltViewModel(),
) {
    val resultEventBus = LocalResultEventBus.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        viewModel.processIntent(SystemGalleryIntent.PickPhoto(uri.toString()))
    }

    LaunchedEffect(Unit) {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SystemGallerySideEffect.NavigateToBack -> {
                    if (effect.imageUri != null) {
                        resultEventBus.sendResult(effect.imageUri)
                    }
                    navigator.onBack()
                }
            }
        }
    }

    YGScaffoldV2 { innerPadding ->
        SystemGalleryPickerScreen(
            state = state,
            modifier = modifier.padding(innerPadding),
            onClickConfirm = {
                viewModel.processIntent(SystemGalleryIntent.ConfirmPhoto(state.imageUri))
            },
        )
    }
}
