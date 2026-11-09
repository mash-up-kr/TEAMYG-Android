package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingHitTarget

/**
 * 텍스트는 실루엣이 없어 박스 전체가 판정 영역이다. [boxWidthPx]·[boxHeightPx] 는 [scale] 을
 * 곱하기 전, 렌더에서 잰 박스 크기다.
 */
fun textToppingHitTarget(
    centerXPx: Float,
    centerYPx: Float,
    boxWidthPx: Float,
    boxHeightPx: Float,
    scale: Float,
    rotationDegrees: Float,
): ToppingHitTarget = ToppingHitTarget(
    centerXPx = centerXPx,
    centerYPx = centerYPx,
    imageWidthPx = boxWidthPx * scale,
    imageHeightPx = boxHeightPx * scale,
    rotationDegrees = rotationDegrees,
    borderWidthPx = 0f,
    outline = null,
)

/**
 * 겹친 것들 중 [xPx]·[yPx] 를 처음 받는 대상. 위 토핑의 투명한 자리는 통과해 아래가 잡힌다.
 *
 * @param entries 겹침 순서가 **아래에서 위**인 목록. 그리는 순서 그대로 넘기면 된다
 */
fun <T> pickToppingHit(
    entries: List<Pair<T, ToppingHitTarget>>,
    xPx: Float,
    yPx: Float,
): T? = entries
    .asReversed()
    .firstOrNull { (_, target) -> target.containsPoint(xPx, yPx) }
    ?.first
