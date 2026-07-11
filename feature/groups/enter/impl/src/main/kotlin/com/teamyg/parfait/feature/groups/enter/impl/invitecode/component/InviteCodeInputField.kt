package com.teamyg.parfait.feature.groups.enter.impl.invitecode.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun InviteCodeInputField(
    text: String,
    maxLength: Int,
    horizontalSpace: Dp,
    modifier: Modifier = Modifier,
    elementContent: @Composable (word: String, index: Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(horizontalSpace),
        modifier = modifier,
    ) {
        repeat(maxLength) { index ->
            elementContent(text.getOrNull(index)?.toString().orEmpty(), index)
        }
    }
}

private data class InviteCodeInputFieldParam(
    val text: String,
    val maxLength: Int,
)

private class InviteCodeInputFieldPreviewParameterProvider :
    PreviewParameterProvider<InviteCodeInputFieldParam> {
    override val values: Sequence<InviteCodeInputFieldParam>
        get() = sequenceOf(
            InviteCodeInputFieldParam("", 3),
            InviteCodeInputFieldParam("h", 3),
            InviteCodeInputFieldParam("hello", 5),
        )
}

@YGPreview
@Composable
private fun InviteCodeInputFieldPreview(
    @PreviewParameter(InviteCodeInputFieldPreviewParameterProvider::class) param: InviteCodeInputFieldParam,
) = PreviewBox {
    InviteCodeInputField(
        text = "",
        maxLength = 5,
        horizontalSpace = 6.dp,
        modifier = Modifier.fillMaxWidth(),
        elementContent = { word, index ->
            InviteCodeInputFieldElement(
                word = word,
                isFocus = index == 0,
                onValueChanged = { },
                onClickTextFieldElement = { },
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(62 / 76f),
            )
        },
    )
}
