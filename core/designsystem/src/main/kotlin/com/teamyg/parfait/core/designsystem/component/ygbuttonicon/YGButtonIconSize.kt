package com.teamyg.parfait.core.designsystem.component.ygbuttonicon

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
enum class YGButtonIconSize(val containerSize: Dp, val iconSize: Dp) {
    SIZE_44(containerSize = 44.dp, iconSize = 24.dp),
    SIZE_48(containerSize = 48.dp, iconSize = 28.dp),
}
