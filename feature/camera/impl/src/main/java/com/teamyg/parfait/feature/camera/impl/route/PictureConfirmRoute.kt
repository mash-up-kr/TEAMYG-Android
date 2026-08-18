package com.teamyg.parfait.feature.camera.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.screen.PictureConfirmScreen
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun PictureConfirmRoute(
    uri: String,
    source: PictureConfirmSource,
    returnResultOnly: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    YGScaffoldV2 { innerPadding ->
        PictureConfirmScreen(
            uri = uri,
            source = source,
            onClickReCapture = { navigator.onBack() },
            onClickConfirm = {
                if (returnResultOnly) {
                    resultEventBus.sendResult(PictureConfirmResult(uri = uri, source = source))
                    navigator.onBack() // PictureConfirm
                    navigator.onBack() // Camera/Gallery
                } else {
                    navigator.goToAndPopCurrent(
                        destination = NavKeySegmentation(
                            sourceImageUri = uri,
                        ),
                    )
                }
            },
            // 배경 편집에서 들어온 경우 캔버스까지 튀면 편집 중이던 배경이 날아간다.
            // 그 경로의 닫기는 부른 화면으로 돌아가는 것이고, 확인 버튼과 같은 백 처리다
            onClickClose = {
                if (returnResultOnly) {
                    navigator.onBack() // PictureConfirm
                    navigator.onBack() // Camera/Gallery
                } else {
                    navigator.popUpTo<NavKeyCanvasMain>()
                }
            },
            modifier = modifier.padding(innerPadding),
        )
    }
}
