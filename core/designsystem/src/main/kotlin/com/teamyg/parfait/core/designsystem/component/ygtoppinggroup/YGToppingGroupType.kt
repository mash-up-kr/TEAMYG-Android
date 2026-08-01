package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Figma Topping-Group Type
 */
enum class YGToppingGroupType(
    val rotation: Float,
    val imageOffset: DpOffset,
    val chipOffset: DpOffset,
) {
    TYPE_1_LEFT(
        rotation = -6f,
        imageOffset = DpOffset(
            x = (-1.25).dp,
            y = (-11.25).dp,
        ),
        chipOffset = DpOffset(
            x = (-0.5).dp,
            y = 49.23.dp,
        ),
    ),
    TYPE_1_RIGHT(
        rotation = 6f,
        imageOffset = DpOffset(
            x = (-1.25).dp,
            y = (-11.25).dp,
        ),
        chipOffset = DpOffset(
            x = (-0.5).dp,
            y = 49.23.dp,
        ),
    ),
    TYPE_2_LEFT(
        rotation = -12f,
        imageOffset = DpOffset(
            x = 1.06.dp,
            y = (-12.07).dp,
        ),
        chipOffset = DpOffset(
            x = 0.13.dp,
            y = 54.69.dp,
        ),
    ),
    TYPE_2_RIGHT(
        rotation = 16f,
        imageOffset = DpOffset(
            x = 1.5.dp,
            y = (-12.63).dp,
        ),
        chipOffset = DpOffset(
            x = 0.13.dp,
            y = 58.13.dp,
        ),
    ),
    TYPE_3_LEFT(
        rotation = 8f,
        imageOffset = DpOffset(
            x = (-0.79).dp,
            y = (-10.79).dp,
        ),
        chipOffset = DpOffset(
            x = (-0.5).dp,
            y = 49.5.dp,
        ),
    ),
    TYPE_3_RIGHT(
        rotation = 8f,
        imageOffset = DpOffset(
            x = (-0.79).dp,
            y = (-10.79).dp,
        ),
        chipOffset = DpOffset(
            x = (-0.5).dp,
            y = 49.5.dp,
        ),
    ),
    TEMPLATE(
        rotation = 0f,
        imageOffset = DpOffset(
            x = (-0.79).dp,
            y = (-10.79).dp,
        ),
        chipOffset = DpOffset(
            x = (-0.5).dp,
            y = 49.5.dp,
        ),
    ),
}
