package com.teamyg.parfait.core.designsystem.theme.token

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

object ShapeTokens {
    val RadiusXSmall: Shape = RoundedCornerShape(SizeTokens.Size4.getDp())
    val RadiusSmall: Shape = RoundedCornerShape(SizeTokens.Size8.getDp())
    val RadiusMedium1: Shape = RoundedCornerShape(SizeTokens.Size12.getDp())
    val RadiusMedium2: Shape = RoundedCornerShape(SizeTokens.Size16.getDp())
    val RadiusLarge: Shape = RoundedCornerShape(SizeTokens.Size24.getDp())
    val RadiusXLarge1: Shape = RoundedCornerShape(SizeTokens.Size32.getDp())
    val RadiusXLarge2: Shape = RoundedCornerShape(SizeTokens.Size48.getDp())
    val RadiusRound: Shape = CircleShape
}
