package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationEdit
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationConfirmScreen
import java.io.File

@Composable
internal fun SegmentationConfirmRoute(
    navigator: Navigator,
    key: NavKeySegmentationConfirm,
    modifier: Modifier = Modifier,
) {
    SegmentationConfirmScreen(
        subjectImagePath = key.subjectImagePath,
        onClickBack = { navigator.onBack() },
        onClickClose = { }, // TODO: 편집 플로우 종료 후 이동할 화면 연결 필요
        onClickEditPhoto = {
            navigator.goTo(
                NavKeySegmentationEdit(
                    sourceImageUri = key.sourceImageUri,
                    // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                    segmentationImageUri = File(key.subjectImagePath).toUri().toString(),
                ),
            )
        },
        onClickNext = { navigator.goTo(NavKeyCanvasMove(imageUri = key.subjectImagePath)) },
        modifier = modifier,
    )
}
