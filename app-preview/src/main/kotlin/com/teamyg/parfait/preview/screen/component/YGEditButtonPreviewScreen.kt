package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygeditbutton.YGEditButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGEditButtonPreviewScreen(
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
                var selected by remember { mutableStateOf(false) }
                PreviewSection("interactive (tap to toggle)") {
                    YGEditButton(
                        text = "편집",
                        isSelected = selected,
                        onClick = { selected = !selected },
                        iconResource = R.drawable.ic_minus_round,
                    )
                }
            }
            item {
                PreviewSection("default / selected (static) + no icon") {
                    YGEditButton(
                        text = "편집",
                        isSelected = false,
                        onClick = {},
                        iconResource = R.drawable.ic_minus_round,
                    )
                    YGEditButton(
                        text = "편집",
                        isSelected = true,
                        onClick = {},
                        iconResource = R.drawable.ic_minus_round,
                    )
                    YGEditButton(
                        text = "아이콘 없음",
                        isSelected = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGEditButtonPreviewScreen() = PreviewBox {
    YGEditButtonPreviewScreen(
        onBack = {},
    )
}
