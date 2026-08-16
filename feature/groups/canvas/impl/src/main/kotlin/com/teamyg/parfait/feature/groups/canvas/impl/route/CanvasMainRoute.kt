package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainIntent
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun CanvasMainRoute(
    groupId: Long,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasMainViewModel = hiltViewModel(
        creationCallback = { factory: CanvasMainViewModel.Factory ->
            factory.create(groupIdValue = groupId)
        },
    ),
) {
    val canvasState by viewModel.state.collectAsStateWithLifecycle()

    ResultEffect<String> { imageUri ->
        viewModel.processIntent(CanvasMainIntent.CacheImage(imageUri))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CanvasMainEffect.NavigateToCamera -> navigator.goTo(
                    destination = NavKeyCameraCustom(),
                )

                is CanvasMainEffect.NavigateToCanvas -> navigator.goTo(
                    destination = NavKeyCustomGalleryPicker(),
                )

                is CanvasMainEffect.NavigateToCanvasBGEdit -> navigator.goTo(
                    destination = NavKeyCanvasBGEdit,
                )

                is CanvasMainEffect.NavigateToGroupSetting -> navigator.goTo(
                    destination = NavKeyGroupSetting(groupId = effect.groupId.value),
                )

                is CanvasMainEffect.NavigateToSegmentation -> navigator.goTo(
                    destination = NavKeySegmentation(
                        sourceImageUri = effect.uri,
                    ),
                )
            }
        }
    }

    CanvasMainScreen(
        canvasState = canvasState,
        onClickBack = { navigator.onBack() },
        onClickDateSelect = { viewModel.processIntent(CanvasMainIntent.OnClickDateSelect) },
        onClickMenu = { viewModel.processIntent(CanvasMainIntent.OnClickGroupSetting) },
        onClickCamera = { viewModel.processIntent(CanvasMainIntent.OnClickCamera()) },
        onClickGallery = { viewModel.processIntent(CanvasMainIntent.OnClickCanvas()) },
        onClickEditCanvasBG = { viewModel.processIntent(CanvasMainIntent.OnClickCanvasEdit()) },
        onClickSaveToGallery = { viewModel.processIntent(CanvasMainIntent.OnClickSaveToGallery) },
        onClickGoToToday = { viewModel.processIntent(CanvasMainIntent.OnClickGoToToday) },
        onDismissCalendar = { viewModel.processIntent(CanvasMainIntent.DismissCalendar) },
        onSelectYear = { viewModel.processIntent(CanvasMainIntent.SelectYear(it)) },
        onSelectMonth = { viewModel.processIntent(CanvasMainIntent.SelectMonth(it)) },
        onClickDate = { viewModel.processIntent(CanvasMainIntent.ClickDate(it)) },
        modifier = modifier,
    )
}
