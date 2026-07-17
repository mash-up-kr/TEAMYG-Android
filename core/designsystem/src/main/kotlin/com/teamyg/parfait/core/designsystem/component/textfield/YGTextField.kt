package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLength: Int? = null,
    colors: YGTextFieldColors = YGTextFieldDefaults.colors(),
) {
    YGTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        isError = isError,
        maxLength = maxLength,
        colors = colors,
    )
}

@YGPreview
@Composable
private fun YGTextFieldPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("idle")
        YGTextField(
            value = "",
            onValueChange = {},
            placeholder = "Text를 입력해 주세요",
            maxLength = 15,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("filled")
        YGTextField(
            value = "Text",
            onValueChange = {},
            maxLength = 15,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("error")
        YGTextField(
            value = "Text",
            onValueChange = {},
            isError = true,
            maxLength = 15,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("disabled")
        YGTextField(
            value = "Text",
            onValueChange = {},
            enabled = false,
            maxLength = 15,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
