package com.teamyg.parfait.domain.model.solo

import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/**
 * 혼자 공간 캔버스. 원소 타입은 그룹 캔버스와 같아 화면 매퍼(`EditableToppingMapper`)를 그대로 쓴다.
 *
 * 모든 토핑·텍스트는 `isMine = true`이고 `placedBy`는 닉네임 "나" 하나로 고정한다.
 * `createdAt`은 실제 배치 시각이며 영상 저장(타임랩스)이 이 순서로 그린다.
 */
data class SoloCanvasVO(
    val background: CanvasBackground?,
    val toppings: List<CanvasToppingVO>,
    val texts: List<CanvasTextToppingVO>,
)
