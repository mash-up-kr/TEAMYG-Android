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
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGIconButtonPreviewScreen(
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
                PreviewSection("SIZE_44 (enabled / disabled)") {
                    YGIconButton(
                        iconResource = R.drawable.ic_close_round,
                        size = YGIconButtonSize.SIZE_44,
                        contentDescription = "닫기",
                        onClick = {},
                    )
                    YGIconButton(
                        iconResource = R.drawable.ic_close_round,
                        size = YGIconButtonSize.SIZE_44,
                        contentDescription = "닫기",
                        onClick = {},
                        isEnabled = false,
                    )
                }
            }
            item {
                PreviewSection("SIZE_48 (enabled / disabled)") {
                    YGIconButton(
                        iconResource = R.drawable.ic_caret_right,
                        size = YGIconButtonSize.SIZE_48,
                        contentDescription = "다음",
                        onClick = {},
                    )
                    YGIconButton(
                        iconResource = R.drawable.ic_caret_right,
                        size = YGIconButtonSize.SIZE_48,
                        contentDescription = "다음",
                        onClick = {},
                        isEnabled = false,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGIconButtonPreviewScreen() = PreviewBox {
    YGIconButtonPreviewScreen(
        onBack = {},
    )
}
