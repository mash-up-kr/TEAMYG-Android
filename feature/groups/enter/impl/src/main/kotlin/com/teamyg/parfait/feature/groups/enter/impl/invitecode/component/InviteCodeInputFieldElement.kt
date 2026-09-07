package com.teamyg.parfait.feature.groups.enter.impl.invitecode.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * 초대코드 한 글자를 그리는 칸.
 *
 * 입력은 [InviteCodeInputField] 의 텍스트 필드 하나가 받는다.
 */
@Composable
internal fun InviteCodeInputFieldElement(
    word: String,
    isFocus: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clickableYGNoRipple(onClick = onClick)) {
        Text(
            text = word,
            color = YGAtomicColors.Gray.Black,
            style = YGTheme.typography.title.t02SB,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height((2.2).dp)
                .background(
                    color = when {
                        isError -> YGAtomicColors.Cherry.Cherry600
                        isFocus -> YGAtomicColors.Cherry.Cherry200
                        else -> YGAtomicColors.Gray.Gray100
                    },
                ).align(Alignment.BottomCenter),
        )
    }
}

private data class InviteCodeInputFieldElementParam(
    val word: String,
    val isFocus: Boolean,
    val isError: Boolean,
)

private class InviteCodeInputFieldElementPreviewParameterProvider :
    PreviewParameterProvider<InviteCodeInputFieldElementParam> {
    override val values: Sequence<InviteCodeInputFieldElementParam>
        get() = sequenceOf(
            InviteCodeInputFieldElementParam("", true, false),
            InviteCodeInputFieldElementParam("h", true, false),
            InviteCodeInputFieldElementParam("h", false, false),
            InviteCodeInputFieldElementParam("h", true, true),
            InviteCodeInputFieldElementParam("h", false, true),
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
        isError = param.isError,
        onClick = {},
        modifier = Modifier.size(100.dp, 150.dp),
    )
}
