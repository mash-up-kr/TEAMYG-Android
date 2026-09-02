package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygdatebutton.YGDateButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGDateButtonPreviewScreen(
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
                PreviewSection("default") {
                    YGDateButton(
                        text = "15",
                        isSelected = false,
                        isToday = false,
                        isEnabled = true,
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            item {
                PreviewSection("selected") {
                    YGDateButton(
                        text = "15",
                        isSelected = true,
                        isToday = false,
                        isEnabled = true,
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            item {
                PreviewSection("today") {
                    YGDateButton(
                        text = "15",
                        isSelected = false,
                        isToday = true,
                        isEnabled = true,
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            item {
                PreviewSection("disabled") {
                    YGDateButton(
                        text = "15",
                        isSelected = false,
                        isToday = false,
                        isEnabled = false,
                        onClick = {},
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGDateButtonPreviewScreen() = PreviewBox {
    YGDateButtonPreviewScreen(
        onBack = {},
    )
}
