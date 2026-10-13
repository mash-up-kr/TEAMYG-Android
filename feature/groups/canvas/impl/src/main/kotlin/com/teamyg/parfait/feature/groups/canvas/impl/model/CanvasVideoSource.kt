package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/**
 * 타임랩스 영상을 다시 그리는 데 필요한 캔버스 한 벌. 미리보기를 연 순간의 값이다.
 *
 * @param toppings 그리는 순서(positionZ 오름차순). 이 순서가 곧 등장 순서다
 * @param background 캔버스 메인이 `YGCanvas` 에 넘긴 것과 같은 값
 * @param canvasWidth 캔버스 메인의 Canvas-Area dp 폭. 토핑 테두리 굵기가 화면 기준 dp 고정이라
 *   (ADR-0025) 다른 폭으로 그리면 영상의 테두리가 이미지 저장물과 다른 두께로 앉는다
 * @param retryKey 캔버스 메인의 재시도 키. 0 으로 두면 재시도 뒤에도 캐시에 앉은 옛 실패 바이트를 읽는다
 */
internal data class CanvasVideoSource(
    val toppings: List<CanvasToppingVO>,
    val background: YGCanvasBackground?,
    val canvasWidth: Dp,
    val canvasHeight: Dp,
    val retryKey: Int,
)
