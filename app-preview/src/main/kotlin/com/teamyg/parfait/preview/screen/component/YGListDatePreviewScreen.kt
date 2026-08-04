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
import com.teamyg.parfait.core.designsystem.component.yglistdate.YGListDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGListDatePreviewScreen(
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
                PreviewSection("upload = true") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = true,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = true,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = true,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = true,
                            isEnabled = true,
                            isUploaded = true,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = false,
                            isUploaded = true,
                            onClick = {},
                        )
                    }
                }
            }
            item {
                PreviewSection("upload = false") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = false,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = true,
                            isToday = false,
                            isEnabled = true,
                            isUploaded = false,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = true,
                            isEnabled = true,
                            isUploaded = false,
                            onClick = {},
                        )
                        YGListDate(
                            text = "31",
                            isSelected = false,
                            isToday = false,
                            isEnabled = false,
                            isUploaded = false,
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
private fun PreviewYGListDatePreviewScreen() = PreviewBox {
    YGListDatePreviewScreen(onBack = {})
}
