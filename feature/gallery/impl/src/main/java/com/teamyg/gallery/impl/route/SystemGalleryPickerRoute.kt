package com.teamyg.gallery.impl.route

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.gallery.impl.screen.SystemGalleryPickerScreen
import com.teamyg.gallery.impl.viewmodel.SystemGalleryIntent
import com.teamyg.gallery.impl.viewmodel.SystemGalleryPickerViewModel
import com.teamyg.navigation.Navigator

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
        viewModel.processIntent(SystemGalleryIntent.PickPhoto(uri))
    }

    LaunchedEffect(Unit) {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    SystemGalleryPickerScreen(
        state = state,
        modifier = modifier,
        onClickConfirm = {
            resultEventBus.sendResult(state.imageUri)
            navigator.onBack()
        },
    )
}
