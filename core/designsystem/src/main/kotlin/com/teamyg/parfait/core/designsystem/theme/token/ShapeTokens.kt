package com.teamyg.parfait.core.designsystem.theme.token

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

object ShapeTokens {
    val RadiusXSmall: Shape = RoundedCornerShape(SizeTokens.Size2.getDp())
    val RadiusSmall: Shape = RoundedCornerShape(SizeTokens.Size4.getDp())
    val RadiusMedium1: Shape = RoundedCornerShape(SizeTokens.Size6.getDp())
    val RadiusMedium2: Shape = RoundedCornerShape(SizeTokens.Size8.getDp())
    val RadiusLarge: Shape = RoundedCornerShape(SizeTokens.Size10.getDp())
    val RadiusXLarge: Shape = RoundedCornerShape(SizeTokens.Size12.getDp())
}
