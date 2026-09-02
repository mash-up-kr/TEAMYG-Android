package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChip
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGGrouptagChipPreviewScreen(
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
                PreviewSection("Type 6종 (타임스탬프 색만 다름)") {
                    YGGrouptagChipType.entries.forEach { type ->
                        YGGrouptagChip(
                            name = "잠탈감금",
                            timestamp = "3분전",
                            type = type,
                        )
                    }
                }
            }
            item {
                PreviewSection("이름 말줄임 (80dp 초과)") {
                    YGGrouptagChip(
                        name = "팀장은 진짜 연경이야",
                        timestamp = "3분전",
                        type = YGGrouptagChipType.TYPE_7_8,
                    )
                    YGGrouptagChip(
                        name = "다섯글자임",
                        timestamp = "오래 전",
                        type = YGGrouptagChipType.TYPE_9_10,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGGrouptagChipPreviewScreen() = PreviewBox {
    YGGrouptagChipPreviewScreen(
        onBack = {},
    )
}
