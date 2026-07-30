package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygeditactionbutton.YGEditActionButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGEditActionButtonPreviewScreen(
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
                PreviewSection("enabled / disabled (dark backdrop)") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .background(YGAtomicColors.Gray.Gray900)
                            .padding(16.dp),
                    ) {
                        YGEditActionButton(
                            iconResource = R.drawable.ic_arrow_left,
                            contentDescription = "이전",
                            onClick = {},
                        )
                        YGEditActionButton(
                            iconResource = R.drawable.ic_arrow_left,
                            contentDescription = "이전",
                            onClick = {},
                            isEnabled = false,
                        )
                    }
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGEditActionButtonPreviewScreen() = PreviewBox {
    YGEditActionButtonPreviewScreen(
        onBack = {},
    )
}
