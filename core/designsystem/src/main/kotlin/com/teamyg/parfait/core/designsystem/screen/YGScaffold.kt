package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Deprecated(
    message = "공통 로딩·에러 토스트 처리가 없는 구판이다. YGScaffoldV2 로 이관한다.",
    replaceWith = ReplaceWith(
        "YGScaffoldV2(modifier = modifier, containerColor = containerColor, " +
            "contentWindowInsets = contentWindowInsets, content = content)",
    ),
    level = DeprecationLevel.WARNING,
)
@Composable
fun YGScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = YGAtomicColors.Gray.White,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
