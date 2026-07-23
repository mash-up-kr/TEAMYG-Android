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
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextField
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGTextFieldPreviewScreen(
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
                var text by remember { mutableStateOf("") }
                PreviewSection("default (placeholder)") {
                    YGTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "입력하세요",
                    )
                }
            }
            item {
                var text by remember { mutableStateOf("입력값") }
                PreviewSection("filled + maxLength 10") {
                    YGTextField(
                        value = text,
                        onValueChange = { text = it },
                        maxLength = 10,
                    )
                }
            }
            item {
                var text by remember { mutableStateOf("잘못된 값") }
                PreviewSection("error") {
                    YGTextField(
                        value = text,
                        onValueChange = { text = it },
                        isError = true,
                    )
                }
            }
            item {
                PreviewSection("disabled") {
                    YGTextField(
                        value = "비활성",
                        onValueChange = {},
                        enabled = false,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGTextFieldPreviewScreen() = PreviewBox {
    YGTextFieldPreviewScreen(
        onBack = {},
    )
}
