package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygstrokebutton.YGStrokeButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGStrokeButtonPreviewScreen(
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
                PreviewSection("interactive (tap to toggle selected)") {
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = { selected = !selected },
                        iconResource = R.drawable.ic_plus,
                        isSelected = selected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("default / selected / disabled") {
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                        isSelected = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    YGStrokeButton(
                        text = "토핑 추가",
                        onClick = {},
                        iconResource = R.drawable.ic_plus,
                        isEnabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PreviewSection("no icon / half width pair") {
                    YGStrokeButton(
                        text = "아이콘 없음",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        YGStrokeButton(
                            text = "토핑 추가",
                            onClick = {},
                            iconResource = R.drawable.ic_plus,
                            modifier = Modifier.weight(1f),
                        )
                        YGStrokeButton(
                            text = "캔버스 편집",
                            onClick = {},
                            iconResource = R.drawable.ic_caret_right,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGStrokeButtonPreviewScreen() = PreviewBox {
    YGStrokeButtonPreviewScreen(
        onBack = {},
    )
}
