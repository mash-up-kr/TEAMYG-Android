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
import com.teamyg.parfait.core.designsystem.component.ygtogglebutton.YGToggleButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGToggleButtonPreviewScreen(
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
                    YGToggleButton(
                        text = "토글",
                        isSelected = selected,
                        onClick = { selected = !selected },
                    )
                }
            }
            item {
                PreviewSection("selected = false / true (static)") {
                    YGToggleButton(
                        text = "토글",
                        isSelected = false,
                        onClick = {},
                    )
                    YGToggleButton(
                        text = "토글",
                        isSelected = true,
                        onClick = {},
                    )
                }
            }
            item {
                var selected by remember { mutableStateOf(true) }
                PreviewSection("with icon") {
                    YGToggleButton(
                        text = "토글",
                        isSelected = selected,
                        onClick = { selected = !selected },
                        iconResource = R.drawable.ic_plus,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGToggleButtonPreviewScreen() = PreviewBox {
    YGToggleButtonPreviewScreen(
        onBack = {},
    )
}
