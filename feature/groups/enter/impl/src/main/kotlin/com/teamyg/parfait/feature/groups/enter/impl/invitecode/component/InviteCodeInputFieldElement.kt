package com.teamyg.parfait.feature.groups.enter.impl.invitecode.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun InviteCodeInputFieldElement(
    word: String,
    isFocus: Boolean,
    onValueChanged: (String) -> Unit,
    onClickTextFieldElement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocus) {
        if (isFocus) {
            focusRequester.requestFocus()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color = Color(0x0F000000))
            .then(
                if (isFocus) {
                    Modifier.border(width = 1.dp, color = Color(0x4D000000), shape = RoundedCornerShape(4.dp))
                } else {
                    Modifier
                },
            ),
    ) {
        BasicTextField(
            value = word,
            textStyle = TextStyle(
                color = Color(0xFF000000),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(Color.Transparent),
            onValueChange = { value -> onValueChanged(value) },
            modifier = Modifier.focusRequester(focusRequester),
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClickTextFieldElement() },
        )
    }
}

private data class InviteCodeInputFieldElementParam(
    val word: String,
    val isFocus: Boolean,
)

private class InviteCodeInputFieldElementPreviewParameterProvider :
    PreviewParameterProvider<InviteCodeInputFieldElementParam> {
    override val values: Sequence<InviteCodeInputFieldElementParam>
        get() = sequenceOf(
            InviteCodeInputFieldElementParam("", true),
            InviteCodeInputFieldElementParam("h", true),
            InviteCodeInputFieldElementParam("h", false),
        )
}

@YGPreview
@Composable
private fun InviteCodeInputFieldElementPreview(
    @PreviewParameter(InviteCodeInputFieldElementPreviewParameterProvider::class) param:
    InviteCodeInputFieldElementParam,
) = PreviewBox {
    InviteCodeInputFieldElement(
        word = param.word,
        isFocus = param.isFocus,
        onValueChanged = {},
        onClickTextFieldElement = {},
        modifier = Modifier.size(100.dp, 150.dp),
    )
}
