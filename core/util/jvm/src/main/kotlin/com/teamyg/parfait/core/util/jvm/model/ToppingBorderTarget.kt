package com.teamyg.parfait.core.util.jvm.model

/**
 * 띠를 칠할 판과, 그 판 안에서 알맹이가 놓이는 자리.
 *
 * 알맹이 자리를 따로 받는 것이 핵심이다 — 판은 알맹이보다 사방으로 넓고, 실루엣은 판 전체가
 * 아니라 그 안쪽 사각형에 대응한다. 이 값이 없으면 실루엣이 여백까지 채우도록 늘어난다.
 */
data class ToppingBorderTarget(
    val width: Int,
    val height: Int,
    val subjectLeft: Int,
    val subjectTop: Int,
    val subjectWidth: Int,
    val subjectHeight: Int,
) {
    internal val isUsable: Boolean
        get() = width > 0 && height > 0 && subjectWidth > 0 && subjectHeight > 0
}
