package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.util.toSpotlightTimeLabel
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainIntent
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting

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
    val context = LocalContext.current
    // 토스트 정책을 둘로 나눈다 — 토핑 작성자 알림은 캔버스 영역 안에 뜨고(Spotlight 디자인),
    // 조회 실패는 화면 최상단에 뜬다. 하나를 두 호스트에 넘기면 같은 토스트가 두 곳에 그려진다
    val spotlightToastPolicy = rememberYGToastPolicy()
    val errorToastPolicy = rememberYGToastPolicy()
    val todayCanvasErrorMessage = stringResource(R.string.canvas_main_today_canvas_error)
    val toppingFlowStartErrorMessage = stringResource(R.string.canvas_main_topping_flow_start_error)

    // 백스택 아래에 깔린 엔트리는 컴포지션에서 빠지므로 다시 앞에 설 때 한 번 더 돈다.
    // 매번 다시 묻는 이유는 CanvasMainIntent.Enter 에 있다
    LifecycleResumeEffect(viewModel) {
        viewModel.processIntent(CanvasMainIntent.Enter)
        onPauseOrDispose { }
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

                is CanvasMainEffect.ShowSpotlightToast -> spotlightToastPolicy.show(
                    YGToastType.Record(
                        userName = effect.nickname,
                        time = effect.elapsed.toSpotlightTimeLabel(context),
                        userNameColor = effect.nicknameColor,
                    ),
                )

                is CanvasMainEffect.ShowTodayCanvasError -> errorToastPolicy.showError(todayCanvasErrorMessage)

                is CanvasMainEffect.ShowToppingFlowStartError ->
                    errorToastPolicy.showError(toppingFlowStartErrorMessage)
            }
        }
    }

    // Spotlight 상태에서 앱이 백그라운드로 이동했다가 돌아오면 Default 로 복귀한다
    LifecycleStartEffect(viewModel) {
        viewModel.processIntent(CanvasMainIntent.OnAppReturnedFromBackground)
        onStopOrDispose { }
    }

    YGScaffoldV2(
        modifier = modifier,
        toastPolicy = errorToastPolicy,
    ) { innerPadding ->
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
            onClickTopping = { viewModel.processIntent(CanvasMainIntent.OnClickTopping(it)) },
            onClickSpotlightDim = { viewModel.processIntent(CanvasMainIntent.OnClickSpotlightDim) },
            toastPolicy = spotlightToastPolicy,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
