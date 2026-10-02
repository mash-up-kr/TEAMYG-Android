package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.modal.YGModalQuitEdit
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasToppingArrangeScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeError
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeViewModel
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.toStringResource

@Composable
internal fun CanvasToppingArrangeRoute(
    groupId: Long,
    parfaitId: Long,
    initialToppingId: Long,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasToppingArrangeViewModel = hiltViewModel(
        creationCallback = { factory: CanvasToppingArrangeViewModel.Factory ->
            factory.create(
                groupId = groupId,
                parfaitId = parfaitId,
                initialToppingId = initialToppingId,
            )
        },
    ),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()

    // 이펙트 수집은 컴포지션이 아니라 코루틴이라 그 안에서 `stringResource` 를 부를 수 없다.
    // 문구를 여기서 미리 뽑아 두고 이펙트는 고르기만 한다
    val errorMessages = CanvasToppingArrangeError.entries.associateWith { it.toStringResource() }
    val othersNotEditableMessage = stringResource(R.string.canvas_topping_arrange_others_not_editable)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CanvasToppingArrangeEffect.NavigateBack -> navigator.onBack()

                // 실패가 아니라 안내라 오류 색을 쓰지 않는다
                CanvasToppingArrangeEffect.ShowOthersToppingNotEditable ->
                    toastPolicy.show(YGToastType.Edit(othersNotEditableMessage))

                is CanvasToppingArrangeEffect.ShowError ->
                    toastPolicy.showError(errorMessages.getValue(effect.error))
            }
        }
    }

    BackHandler { viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack) }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = uiState.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        CanvasToppingArrangeScreen(
            uiState = uiState,
            onClickClose = { viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose) },
            onClickConfirm = { viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm) },
            onClickTopping = { topping ->
                viewModel.processIntent(CanvasToppingArrangeIntent.OnClickTopping(topping))
            },
            onClickEmptyCanvas = { viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas) },
            onToggleBorderPanel = { viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel) },
            onDismissBorderPanel = { viewModel.processIntent(CanvasToppingArrangeIntent.OnDismissBorderPanel) },
            onSelectBorderColor = { colorArgb ->
                viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(colorArgb))
            },
            onChangeBorderWidth = { widthDp ->
                viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(widthDp))
            },
            onToppingTransform = { panX, panY, zoom, rotationDelta ->
                viewModel.processIntent(
                    CanvasToppingArrangeIntent.OnToppingTransform(
                        panX = panX,
                        panY = panY,
                        zoom = zoom,
                        rotationDelta = rotationDelta,
                    ),
                )
            },
            onClickDeleteTopping = {
                viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
            },
            onDeleteToppingDialogConfirm = {
                viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)
            },
            onDeleteToppingDialogCancel = {
                viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogCancel)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }

    if (uiState.showQuitDialog) {
        YGModalQuitEdit(
            onConfirmQuit = { viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogConfirm) },
            onDismiss = { viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogCancel) },
        )
    }
}
