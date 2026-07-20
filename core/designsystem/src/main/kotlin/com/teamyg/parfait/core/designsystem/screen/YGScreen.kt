package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGScreen(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = YGAtomicColors.Gray.White,
    content: @Composable YGScreenScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
    ) {
        val scope = remember { YGScreenScope() }
        scope.content()
    }
}
