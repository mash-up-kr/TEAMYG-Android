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
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGTextFormFieldPreviewScreen(
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
                    YGTextFormField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "닉네임",
                    )
                }
            }
            item {
                var text by remember { mutableStateOf("bad") }
                PreviewSection("error + errorDescription") {
                    YGTextFormField(
                        value = text,
                        onValueChange = { text = it },
                        isError = true,
                        errorDescription = "사용할 수 없는 값입니다",
                    )
                }
            }
            item {
                PreviewSection("disabled") {
                    YGTextFormField(
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
private fun PreviewYGTextFormFieldPreviewScreen() = PreviewBox {
    YGTextFormFieldPreviewScreen(
        onBack = {},
    )
}
