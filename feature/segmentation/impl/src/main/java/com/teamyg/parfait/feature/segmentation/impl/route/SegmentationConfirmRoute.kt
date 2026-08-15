package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.api.TOPPING_EDIT_RESULT_KEY
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationConfirmScreen
import java.io.File

/** 겹은 화면 회전에도 살아남아야 하는데 [ToppingBorderLayer] 를 그대로 담지 못하므로 두 값으로 펼친다 */
private val BorderLayersSaver = listSaver<List<ToppingBorderLayer>, Any>(
    save = { layers -> layers.flatMap { layer -> listOf(layer.colorArgb, layer.widthDp) } },
    restore = { values ->
        values.chunked(2) { (colorArgb, widthDp) ->
            ToppingBorderLayer(colorArgb = colorArgb as Int, widthDp = widthDp as Float)
        }
    },
)

@Composable
internal fun SegmentationConfirmRoute(
    navigator: Navigator,
    key: NavKeySegmentationConfirm,
    modifier: Modifier = Modifier,
) {
    // NavKey 는 화면을 처음 열 때의 인자라 편집 결과를 담지 못한다. 편집 후 결과는 화면이 들고 있는다
    var subjectImagePath by rememberSaveable { mutableStateOf(key.subjectImagePath) }

    // 다시 편집할 때 넘길 재료. 테두리를 구운 [subjectImagePath] 를 마스크로 넘기면 그 색이
    // 원본 픽셀로 덮여 테두리가 사라지므로, 두르기 전 알맹이와 겹 목록을 따로 들고 간다
    var cutoutImagePath by rememberSaveable { mutableStateOf(key.subjectImagePath) }
    var borderLayers by rememberSaveable(stateSaver = BorderLayersSaver) {
        mutableStateOf(emptyList<ToppingBorderLayer>())
    }

    ResultEffect<ToppingEditResult>(resultKey = TOPPING_EDIT_RESULT_KEY) { result ->
        subjectImagePath = result.editedImagePath
        cutoutImagePath = result.cutoutImagePath
        borderLayers = result.borderLayers
    }

    SegmentationConfirmScreen(
        subjectImagePath = subjectImagePath,
        onClickBack = { navigator.onBack() },
        onClickClose = { }, // TODO: 편집 플로우 종료 후 이동할 화면 연결 필요
        onClickEditPhoto = {
            navigator.goTo(
                NavKeyToppingEdit(
                    sourceImageUri = key.sourceImageUri,
                    // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                    segmentationImageUri = File(cutoutImagePath).toUri().toString(),
                    borderLayers = borderLayers,
                ),
            )
        },
        onClickNext = { navigator.goTo(NavKeyCanvasMove(imageUri = subjectImagePath)) },
        modifier = modifier,
    )
}
