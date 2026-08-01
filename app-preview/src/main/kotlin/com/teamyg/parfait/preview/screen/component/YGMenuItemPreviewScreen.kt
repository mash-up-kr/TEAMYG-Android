package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygmenuitem.YGMenuItem
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGMenuItemPreviewScreen(
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
                PreviewSection("default (pressed는 실기기에서 눌러 확인)") {
                    YGMenuItem(
                        text = "카메라로 촬영",
                        onClick = {},
                    )
                    YGMenuItem(
                        text = "갤러리에서 선택",
                        onClick = {},
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGMenuItemPreviewScreen() = PreviewBox {
    YGMenuItemPreviewScreen(
        onBack = {},
    )
}
