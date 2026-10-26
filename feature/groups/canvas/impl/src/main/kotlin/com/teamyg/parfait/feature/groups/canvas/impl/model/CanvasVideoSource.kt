package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/**
 * 타임랩스 영상을 다시 그리는 데 필요한 캔버스 한 벌. 미리보기를 연 순간의 값이다.
 * 각 값이 캔버스 메인과 같아야 하는 이유는 `CanvasVideoRecordLayer` 에 있다
 */
internal data class CanvasVideoSource(
    val toppings: List<CanvasToppingVO>,
    val texts: List<CanvasTextToppingVO>,
    val background: YGCanvasBackground?,
    val canvasWidth: Dp,
    val canvasHeight: Dp,
    val retryKey: Int,
)
