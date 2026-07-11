package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGTextFormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLength: Int? = null,
    description: String? = null,
    colors: YGTextFieldColors = YGTextFieldDefaults.colors(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
    ) {
        YGTextFieldImpl(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder,
            enabled = enabled,
            isError = isError,
            maxLength = maxLength,
            colors = colors,
        )
        if (description != null) {
            Text(
                text = description,
                style = YGTheme.typography.caption.c01R,
                color = colors.counterColor(isError = isError),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGTextFormFieldPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("with description")
        YGTextFormField(
            value = "Text",
            onValueChange = {},
            maxLength = 15,
            description = "닉네임은 15자까지만 입력 가능해요",
            modifier = Modifier.fillMaxWidth(),
        )
        Text("error with description")
        YGTextFormField(
            value = "Text",
            onValueChange = {},
            isError = true,
            maxLength = 15,
            description = "닉네임은 15자까지만 입력 가능해요",
            modifier = Modifier.fillMaxWidth(),
        )
        Text("no description")
        YGTextFormField(
            value = "",
            onValueChange = {},
            placeholder = "Text를 입력해 주세요",
            maxLength = 15,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
