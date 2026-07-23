package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDefault
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGTopBarPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewSection("YGTopBarBack") {
                    YGTopBarBack(onIconClick = {})
                }
            }
            item {
                PreviewSection("YGTopBarDetail") {
                    YGTopBarDetail(
                        title = "상세 화면",
                        onIconClick = {},
                    )
                }
            }
            item {
                PreviewSection("YGTopBarEmpty") {
                    YGTopBarEmpty(onIconClick = {})
                }
            }
            item {
                PreviewSection("YGTopBarDefault") {
                    YGTopBarDefault(
                        onIconClick = {},
                        onChipClick = {},
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGTopBarPreviewScreen() = PreviewBox {
    YGTopBarPreviewScreen(
        onBack = {},
    )
}
