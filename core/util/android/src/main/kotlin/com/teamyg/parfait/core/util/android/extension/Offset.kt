package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import android.graphics.Path as AndroidPath

/**
 * 점을 순서대로 이어 선으로 만든다.
 *
 * 점이 하나뿐이면 이을 곳이 없어 아무것도 그려지지 않으므로, 제자리에 길이 없는 선을 그어 둔다.
 * 둥근 [androidx.compose.ui.graphics.StrokeCap] 으로 그으면 그 자리가 점으로 찍힌다.
 */
fun List<Offset>.toPath(): Path = Path().apply {
    val first = firstOrNull() ?: return@apply

    moveTo(first.x, first.y)
    if (size == 1) {
        lineTo(first.x, first.y)
        return@apply
    }
    for (index in 1..lastIndex) {
        val point = this@toPath[index]
        lineTo(point.x, point.y)
    }
}

/** [toPath] 와 같지만 [android.graphics.Canvas] 로 그릴 수 있는 형태다 */
fun List<Offset>.toAndroidPath(): AndroidPath = AndroidPath().apply {
    val first = firstOrNull() ?: return@apply

    moveTo(first.x, first.y)
    if (size == 1) {
        lineTo(first.x, first.y)
        return@apply
    }
    for (index in 1..lastIndex) {
        val point = this@toAndroidPath[index]
        lineTo(point.x, point.y)
    }
}
