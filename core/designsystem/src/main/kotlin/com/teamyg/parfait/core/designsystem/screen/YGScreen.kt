package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

@Composable
fun YGScreen(
    modifier: Modifier = Modifier,
    content: @Composable YGScreenScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier
            .clickableYGNoRipple {
                focusManager.clearFocus()
            },
        color = YGAtomicColors.Gray.Transparent,
    ) {
        val scope = remember { YGScreenScope() }
        scope.content()
    }
}
