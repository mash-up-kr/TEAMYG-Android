package com.teamyg.gallery.impl.route

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.gallery.impl.model.GalleryAccessLevel
import com.teamyg.gallery.impl.screen.CustomGalleryPickerScreen
import com.teamyg.gallery.impl.utils.GalleryMediaProvider
import com.teamyg.gallery.impl.utils.GalleryPermissionManager
import com.teamyg.gallery.impl.utils.extension.buildAppSettingsIntent
import com.teamyg.gallery.impl.viewmodel.CustomGalleryPickerEffect
import com.teamyg.gallery.impl.viewmodel.CustomGalleryPickerIntent
import com.teamyg.gallery.impl.viewmodel.CustomGalleryPickerViewModel
import com.teamyg.navigation.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CustomGalleryPickerRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CustomGalleryPickerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resultEventBus = LocalResultEventBus.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val access = GalleryAccessLevel.resolveAccessLevelAfterRequest(context, activity)
        viewModel.processIntent(CustomGalleryPickerIntent.OnPermissionResult(access))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CustomGalleryPickerEffect.RequestPermission -> {
                    permissionLauncher.launch(GalleryPermissionManager.requiredPermissions)
                }

                is CustomGalleryPickerEffect.OpenAppSettings -> {
                    context.startActivity(context.buildAppSettingsIntent())
                }

                is CustomGalleryPickerEffect.LoadImages -> {
                    val groups = withContext(Dispatchers.IO) {
                        GalleryMediaProvider.loadImageGroupsByYG(context)
                    }
                    viewModel.processIntent(CustomGalleryPickerIntent.OnImagesLoaded(groups))
                }

                is CustomGalleryPickerEffect.ReturnResult -> {
                    resultEventBus.sendResult(effect.uri)
                    navigator.onBack()
                }

                is CustomGalleryPickerEffect.NavigateToBack -> navigator.onBack()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.processIntent(
                    CustomGalleryPickerIntent.OnPermissionResult(
                        access = GalleryAccessLevel.resolveAccessLevelOnEnter(context),
                    ),
                )
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CustomGalleryPickerScreen(
        state = state,
        onClickGrantPermission = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestPermission) },
        onClickOpenSettings = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestOpenSettings) },
        onClickManageMedia = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestManageMedia) },
        onClickImage = { viewModel.processIntent(CustomGalleryPickerIntent.OnClickImage(it)) },
        onClickCancel = { viewModel.processIntent(CustomGalleryPickerIntent.OnCancel) },
        modifier = modifier,
    )
}
