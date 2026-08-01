package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationConfirmScreen

@Composable
internal fun SegmentationConfirmRoute(
    navigator: Navigator,
    key: NavKeySegmentationConfirm,
    modifier: Modifier = Modifier,
) {
    SegmentationConfirmScreen(
        subjectImagePath = key.subjectImagePath,
        onClickBack = { navigator.onBack() },
        onClickEditPhoto = { }, // TODO: 사진 편집 화면 연결 필요
        onClickNext = { navigator.goTo(NavKeyCanvasMove(imageUri = key.subjectImagePath)) },
        modifier = modifier,
    )
}
