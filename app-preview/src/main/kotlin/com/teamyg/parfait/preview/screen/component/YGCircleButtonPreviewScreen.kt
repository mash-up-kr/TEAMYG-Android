package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGCircleButtonPreviewScreen(
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
                PreviewSection("Default / Secondary / Small") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        YGCircleButton(
                            iconResource = R.drawable.ic_caret_left,
                            type = YGCircleButtonType.Default,
                            contentDescription = "뒤로",
                            onClick = {},
                        )
                        YGCircleButton(
                            iconResource = R.drawable.ic_plus,
                            type = YGCircleButtonType.Secondary,
                            contentDescription = "추가",
                            onClick = {},
                        )
                        YGCircleButton(
                            iconResource = R.drawable.ic_rotate,
                            type = YGCircleButtonType.Small,
                            contentDescription = "전환",
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGCircleButtonPreviewScreen() = PreviewBox {
    YGCircleButtonPreviewScreen(
        onBack = {},
    )
}
