package com.teamyg.parfait.core.designsystem.component.ygtopbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object YGTopBarDefaults {
    val BackdropBlurRadius: Dp = 4.dp

    /**
     * 탑바가 직접 흡수하는 시스템 인셋.
     * 화면(Screen)이 edgeToEdge 대응을 하지 않고, 상단 인셋만큼을 탑바가 자기 패딩으로 가진다.
     */
    val windowInsets: WindowInsets
        @Composable
        get() = WindowInsets.statusBars
}
