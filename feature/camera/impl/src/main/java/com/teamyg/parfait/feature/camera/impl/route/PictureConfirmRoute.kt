package com.teamyg.parfait.feature.camera.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.R
import com.teamyg.parfait.feature.camera.impl.screen.PictureConfirmScreen
import com.teamyg.parfait.feature.camera.impl.viewmodel.PictureConfirmViewModel
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun PictureConfirmRoute(
    uri: String,
    source: PictureConfirmSource,
    returnResultOnly: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current
    val viewModel: PictureConfirmViewModel = hiltViewModel()

    // 배경 편집에서 온 경로는 세그멘테이션으로 가지 않으므로 헛일을 안 한다
    LaunchedEffect(returnResultOnly) {
        if (!returnResultOnly) viewModel.prepareSegmentationModule()
    }

    var showQuitDialog by rememberSaveable { mutableStateOf(false) }

    YGScaffoldV2 { innerPadding ->
        PictureConfirmScreen(
            uri = uri,
            source = source,
            onClickReCapture = { navigator.onBack() },
            onClickConfirm = {
                if (returnResultOnly) {
                    resultEventBus.sendResult(PictureConfirmResult(uri = uri, source = source))
                    // 사이에 낀 촬영·선택 화면을 몇 장이든 걷고 부른 화면으로 되감는다.
                    // 뒤로가기를 세어 돌아가면 그 경로에 화면이 하나 끼는 날 조용히 어긋난다
                    navigator.popUpTo<NavKeyCanvasBGEdit>()
                } else {
                    // 세그멘테이션 로딩이 이 화면 위에서 돈다. 실패·뒤로가기 때 돌아올 수 있게 백스택에 남기고,
                    // 연타로 같은 키가 겹쳐 쌓이지 않게 한다
                    navigator.goToSingleClearTop(
                        destination = NavKeySegmentation(
                            sourceImageUri = uri,
                        ),
                    )
                }
            },
            // 배경 편집에서 들어온 경우 캔버스까지 튀면 편집 중이던 배경이 날아간다.
            // 그 경로의 닫기는 부른 화면으로 돌아가는 것이고, 확인 버튼과 같은 처리다.
            // 그 외 경로는 편집 내용이 버려지므로 그만둘지 먼저 묻는다
            onClickClose = {
                if (returnResultOnly) {
                    navigator.popUpTo<NavKeyCanvasBGEdit>()
                } else {
                    showQuitDialog = true
                }
            },
            modifier = modifier.padding(innerPadding),
        )
    }

    if (showQuitDialog) {
        YGModalPopup(
            title = stringResource(R.string.camera_picture_confirm_quit_dialog_title),
            body = stringResource(R.string.camera_picture_confirm_quit_dialog_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.camera_picture_confirm_quit_dialog_confirm),
            onSecondaryClick = {
                showQuitDialog = false
                navigator.popUpTo<NavKeyCanvasMain>()
            },
            primaryText = stringResource(R.string.camera_picture_confirm_quit_dialog_cancel),
            onPrimaryClick = { showQuitDialog = false },
            onDismissRequest = { showQuitDialog = false },
        )
    }
}
