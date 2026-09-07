package com.teamyg.parfait.feature.groups.enter.impl.invitecode.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * 초대코드 입력칸.
 *
 * 칸이 [maxLength] 개로 보이지만 입력을 받는 것은 [BasicTextField] 하나다. 그래서 삭제와 중간 수정,
 * 뒤 글자 당김이 텍스트 필드의 기본 동작으로 따라온다.
 *
 * `innerTextField` 는 API 계약상 정확히 한 번 불러야 한다 — 안 부르면 텍스트 레이아웃이 잡히지 않아
 * 키보드가 올라올 때 입력줄로 스크롤되지 않는다.
 */
@Composable
internal fun InviteCodeInputField(
    text: String,
    cursor: Int,
    isFocused: Boolean,
    maxLength: Int,
    horizontalSpace: Dp,
    onTextChanged: (text: String, cursor: Int) -> Unit,
    onFocusChanged: (isFocused: Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    elementContent: @Composable (word: String, index: Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    BasicTextField(
        value = TextFieldValue(text = text, selection = TextRange(cursor)),
        onValueChange = { value -> onTextChanged(value.text, value.selection.start) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        // 한 줄임을 알려야 IME 가 Done 대신 개행 키를 내놓지 않는다
        singleLine = true,
        // 커서 자리는 칸의 밑줄로 나타낸다
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState -> onFocusChanged(focusState.isFocused) },
        decorationBox = { innerTextField ->
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpace)) {
                    repeat(maxLength) { index ->
                        elementContent(text.getOrNull(index)?.toString().orEmpty(), index)
                    }
                }
                Box(modifier = Modifier.alpha(0f)) { innerTextField() }
            }
        },
    )
}

private data class InviteCodeInputFieldParam(
    val text: String,
    val cursor: Int,
    val maxLength: Int,
)

private class InviteCodeInputFieldPreviewParameterProvider :
    PreviewParameterProvider<InviteCodeInputFieldParam> {
    override val values: Sequence<InviteCodeInputFieldParam>
        get() = sequenceOf(
            InviteCodeInputFieldParam(text = "", cursor = 0, maxLength = 3),
            InviteCodeInputFieldParam(text = "h", cursor = 1, maxLength = 3),
            InviteCodeInputFieldParam(text = "hello", cursor = 5, maxLength = 5),
        )
}

@YGPreview
@Composable
private fun InviteCodeInputFieldPreview(
    @PreviewParameter(InviteCodeInputFieldPreviewParameterProvider::class) param: InviteCodeInputFieldParam,
) = PreviewBox {
    InviteCodeInputField(
        text = param.text,
        cursor = param.cursor,
        isFocused = false,
        maxLength = param.maxLength,
        horizontalSpace = 6.dp,
        onTextChanged = { _, _ -> },
        onFocusChanged = { },
        onDone = { },
        modifier = Modifier.fillMaxWidth(),
        elementContent = { word, index ->
            InviteCodeInputFieldElement(
                word = word,
                isFocus = index == param.cursor,
                isError = false,
                onClick = { },
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(62 / 76f),
            )
        },
    )
}
