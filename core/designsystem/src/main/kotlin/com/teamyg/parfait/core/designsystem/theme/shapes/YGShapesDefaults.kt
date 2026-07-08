package com.teamyg.parfait.core.designsystem.theme.shapes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

internal object YGShapesDefaults {
    //region radius
    private val radius4: Shape = RoundedCornerShape(SizeTokens.Size4.getDp())
    private val radius8: Shape = RoundedCornerShape(SizeTokens.Size8.getDp())
    private val radius12: Shape = RoundedCornerShape(SizeTokens.Size12.getDp())
    private val radius16: Shape = RoundedCornerShape(SizeTokens.Size16.getDp())
    private val radius24: Shape = RoundedCornerShape(SizeTokens.Size24.getDp())
    private val radius32: Shape = RoundedCornerShape(SizeTokens.Size32.getDp())
    private val radius48: Shape = RoundedCornerShape(SizeTokens.Size48.getDp())
    private val radiusMax: Shape = CircleShape
    //endregion

    private val YGDefaultShapeRadius: YGShapeRadius = YGShapeRadius(
        xSmall = radius4,
        small = radius8,
        medium1 = radius12,
        medium2 = radius16,
        large = radius24,
        xLarge1 = radius32,
        xLarge2 = radius48,
        round = radiusMax,
    )

    val YGDefaultShapes: YGShapes = YGShapes(
        radius = YGDefaultShapeRadius,
    )
}
