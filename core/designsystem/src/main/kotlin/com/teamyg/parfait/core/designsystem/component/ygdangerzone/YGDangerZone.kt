package com.teamyg.parfait.core.designsystem.component.ygdangerzone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGDangerZone(
    topZone: @Composable () -> Unit,
    bottomZone: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .background(
                color = YGAtomicColors.Transparency.Black5,
                shape = YGTheme.shapes.radius.medium1,
            ),
    ) {
        topZone()

        // Todo : YGDivider
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = YGTheme.layout.padding.padding6)
                .background(YGAtomicColors.Transparency.White25),
        )

        bottomZone()
    }
}

@Preview
@Composable
private fun YGDangerZonePreview() {
    YGCustomTheme {
        Box(
            modifier = Modifier
                .background(Color.Black),
        ) {
            YGDangerZone(
                topZone = {
                    YGActionItem(
                        text = "로그아웃",
                        onClick = {},
                    )
                },
                bottomZone = {
                    YGActionItem(
                        text = "서비스 탈퇴하기",
                        onClick = {},
                        modifier = Modifier,
                    )
                },
                modifier = Modifier,
            )
        }
    }
}
