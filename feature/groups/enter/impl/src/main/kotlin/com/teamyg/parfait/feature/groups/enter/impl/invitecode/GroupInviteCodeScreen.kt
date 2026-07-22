package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputField
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputFieldElement
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun GroupInviteCodeScreen(
    uiState: GroupInviteCodeUiState,
    onValueChanged: (index: Int, word: String) -> Unit,
    onClickTextFieldElement: (index: Int) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(all = 21.dp)) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.ic_caret_left),
                    contentDescription = "뒤로가기",
                    modifier = Modifier
                        .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                        .clickable { onClickBackButton() },
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 21.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                item {
                    Text(
                        text = "2/3",
                        color = Color(0x80333333),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "기존 그룹에 참여하기 위해\n초대코드를 입력해 주세요",
                        color = Color(0xFF333333),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "초대코드는 그룹원에게 직접 받을 수 있어요",
                        color = Color(0xFF737373),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    InviteCodeInputField(
                        text = uiState.text,
                        maxLength = uiState.codeLength,
                        horizontalSpace = 6.dp,
                        modifier = Modifier.fillMaxWidth(),
                        elementContent = { word, index ->
                            InviteCodeInputFieldElement(
                                word = word,
                                isFocus = index == uiState.focusedIndex,
                                onValueChanged = { changed -> onValueChanged(index, changed) },
                                onClickTextFieldElement = { onClickTextFieldElement(index) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(62 / 76f),
                            )
                        },
                    )
                }
                if (uiState.errorText != null) {
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = uiState.errorText,
                            color = Color(0xFFFF0000),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
        Text(
            text = "다음",
            color = Color(0xFFFFFFFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 27.dp)
                .background(color = Color(0xFF000000), shape = RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = Color(0xFFDFDFDF), shape = RoundedCornerShape(8.dp))
                .clip(shape = RoundedCornerShape(8.dp))
                .clickable(enabled = true) { onClickNextButton() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
        )
    }
}

private class GroupInviteCodeScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupInviteCodeUiState> {
    override val values: Sequence<GroupInviteCodeUiState>
        get() = sequenceOf(
            GroupInviteCodeUiState(""),
            GroupInviteCodeUiState(text = "he"),
            GroupInviteCodeUiState(text = "hello"),
            GroupInviteCodeUiState(text = "", errorText = "이미 최대 인원이 모두 참여한 그룹이에요"),
        )
}

@YGPreview
@Composable
private fun GroupInviteCodeScreenPreview(
    @PreviewParameter(GroupInviteCodeScreenPreviewParameterProvider::class) uiState: GroupInviteCodeUiState,
) = PreviewBox {
    GroupInviteCodeScreen(
        uiState = uiState,
        onValueChanged = { _, _ -> },
        onClickTextFieldElement = {},
        onClickNextButton = {},
        onClickBackButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
