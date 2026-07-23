package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.etc.YGListItem
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGListItemPreviewScreen(
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
                PreviewSection("trailing icon overload") {
                    YGListItem(
                        text = "계정 정보",
                        trailingIcon = R.drawable.ic_caret_right,
                        onClickTrailingIcon = {},
                    )
                }
            }
            item {
                PreviewSection("subText overload") {
                    YGListItem(
                        text = "버전 정보",
                        subText = "1.0.0",
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGListItemPreviewScreen() = PreviewBox {
    YGListItemPreviewScreen(
        onBack = {},
    )
}
