package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation3.runtime.result.ResultEffect
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
    // NavKey 는 화면을 처음 열 때의 인자라 편집 결과를 담지 못한다. 편집 후 경로는 화면이 들고 있는다
    var subjectImagePath by rememberSaveable { mutableStateOf(key.subjectImagePath) }

    ResultEffect<String> { editedImagePath -> subjectImagePath = editedImagePath }

    SegmentationConfirmScreen(
        subjectImagePath = subjectImagePath,
        onClickBack = { navigator.onBack() },
        onClickClose = { }, // TODO: 편집 플로우 종료 후 이동할 화면 연결 필요
        onClickEditPhoto = {
            navigator.goTo(
                NavKeySegmentationEdit(
                    sourceImageUri = key.sourceImageUri,
                    // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                    segmentationImageUri = File(subjectImagePath).toUri().toString(),
                ),
            )
        },
        onClickNext = { navigator.goTo(NavKeyCanvasMove(imageUri = subjectImagePath)) },
        modifier = modifier,
    )
}
