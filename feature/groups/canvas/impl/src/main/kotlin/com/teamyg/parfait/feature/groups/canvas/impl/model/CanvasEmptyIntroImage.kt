package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.canvas.impl.R

/** 배치 비율은 캔버스 크기에 대한 이미지 **중심**의 위치다 */
internal data class CanvasEmptyIntroImage(
    @DrawableRes val res: Int,
    val width: Dp,
    val height: Dp,
    val centerXFraction: Float,
    val centerYFraction: Float,
    val rotationDegrees: Float = 0f,
)

internal val CANVAS_EMPTY_INTRO_IMAGES = listOf(
    CanvasEmptyIntroImage(
        res = R.drawable.canvas_empty_intro_1,
        width = 134.dp,
        height = 134.dp,
        centerXFraction = 0.3164f,
        centerYFraction = 0.3658f,
    ),
    CanvasEmptyIntroImage(
        res = R.drawable.canvas_empty_intro_2,
        width = 180.dp,
        height = 193.dp,
        centerXFraction = 0.7834f,
        centerYFraction = 0.6015f,
    ),
    CanvasEmptyIntroImage(
        res = R.drawable.canvas_empty_intro_3,
        width = 248.dp,
        height = 254.dp,
        centerXFraction = 0.3149f,
        centerYFraction = 0.6904f,
    ),
)
