package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGDangerZonePreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PreviewSection("top + bottom zones") {
                    YGDangerZone(
                        topZone = {
                            YGActionItem(
                                text = "로그아웃",
                                onClick = {},
                            )
                        },
                        bottomZone = {
                            YGActionItem(
                                text = "회원 탈퇴",
                                onClick = {},
                            )
                        },
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGDangerZonePreviewScreen() = PreviewBox {
    YGDangerZonePreviewScreen(
        onBack = {},
    )
}
