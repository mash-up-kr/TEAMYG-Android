package com.teamyg.parfait.feature.segmentation.impl.route

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.api.TOPPING_EDIT_RESULT_KEY
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.screen.ToppingEditScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditIntent
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditViewModel

@Composable
internal fun ToppingEditRoute(
    navigator: Navigator,
    key: NavKeyToppingEdit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ToppingEditViewModel, ToppingEditViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                sourceImageUri = key.sourceImageUri,
                segmentationImageUri = key.segmentationImageUri,
                borderLayers = key.borderLayers,
            )
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resultEventBus = LocalResultEventBus.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ToppingEditEffect.LoadFailed -> {
                    val message = context.getString(R.string.topping_edit_load_failed)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    navigator.onBack()
                }

                is ToppingEditEffect.SaveFailed -> {
                    val message = context.getString(R.string.topping_edit_save_failed)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                is ToppingEditEffect.EditCompleted -> {
                    resultEventBus.sendResult(TOPPING_EDIT_RESULT_KEY, effect.result)
                    navigator.onBack()
                }
            }
        }
    }

    ToppingEditScreen(
        state = state,
        onChangeTab = { tab -> viewModel.processIntent(ToppingEditIntent.ChangeTab(tab)) },
        onChangeMode = { mode -> viewModel.processIntent(ToppingEditIntent.ChangeMode(mode)) },
        onChangeBrushWidth = { width -> viewModel.processIntent(ToppingEditIntent.ChangeBrushWidth(width)) },
        onAddStroke = { stroke -> viewModel.processIntent(ToppingEditIntent.AddStroke(stroke)) },
        onClickUndoArea = { viewModel.processIntent(ToppingEditIntent.UndoArea) },
        onClickRedoArea = { viewModel.processIntent(ToppingEditIntent.RedoArea) },
        onSelectBorderColor = { color -> viewModel.processIntent(ToppingEditIntent.SelectBorderColor(color)) },
        onChangeBorderWidth = { width -> viewModel.processIntent(ToppingEditIntent.ChangeBorderWidth(width)) },
        onClickUndoBorder = { viewModel.processIntent(ToppingEditIntent.UndoBorder) },
        onClickRedoBorder = { viewModel.processIntent(ToppingEditIntent.RedoBorder) },
        onClickDone = { viewModel.processIntent(ToppingEditIntent.ClickDone) },
        onClickBack = { navigator.onBack() },
        modifier = modifier,
    )
}
