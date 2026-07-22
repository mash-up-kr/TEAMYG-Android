package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGLabelPreviewScreen(
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
                PreviewSection("label") {
                    YGLabel(text = "레이블 텍스트")
                }
            }
            item {
                PreviewSection("long label") {
                    YGLabel(text = "조금 더 긴 보조 설명 레이블 텍스트입니다")
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGLabelPreviewScreen() = PreviewBox {
    YGLabelPreviewScreen(
        onBack = {},
    )
}
