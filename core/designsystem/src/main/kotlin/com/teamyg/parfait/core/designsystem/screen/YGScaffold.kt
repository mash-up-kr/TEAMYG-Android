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
    message = "공통 로딩·에러 토스트 처리가 없는 구판이다. 이관은 이름 교체가 아니다 — " +
        "스캐폴드를 EntryBuilder 에서 Route 안으로 내리고 isLoading·toastPolicy 를 넘겨야 " +
        "실제로 로딩·실패 표현을 얻는다.",
    replaceWith = ReplaceWith(
        "YGScaffoldV2(modifier = modifier, containerColor = containerColor, " +
            "contentWindowInsets = contentWindowInsets, content = content)",
        "com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2",
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
