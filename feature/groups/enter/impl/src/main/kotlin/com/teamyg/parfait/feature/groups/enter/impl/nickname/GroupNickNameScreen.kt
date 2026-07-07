package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.typography.YGTypography
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun GroupNickNameScreen(
    uiState: GroupNickNameUiState,
    onValueChanged: (word: String) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Todo : DesignSystem TopBar component 적용하기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Image(
                painter = painterResource(DesignSystemR.drawable.ic_caret_left),
                contentDescription = "뒤로가기",
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onClickBackButton() },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "그룹 참여하기",
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item {
                Text(
                    text = "그룹이름에서 사용할\n닉네임을 입력해 주세요",
                    color = YGAtomicColors.Gray.Gray900,
                    style = YGTypography.Title.T02_B,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "그룹이름에서만 공유되는 닉네임이에요",
                    color = YGAtomicColors.Gray.Gray500,
                    style = YGTypography.Body.B02_R,
                )
            }
            item {
                Spacer(modifier = Modifier.height(49.dp))
                // Todo : DesignSystem Input component 적용하기
                BasicTextField(
                    value = uiState.nickName,
                    textStyle = TextStyle(
                        color = Color(0xFF000000),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    onValueChange = { value -> onValueChanged(value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(8.dp))
                        .border(width = 1.dp, color = Color(0xFFDFDFDF), shape = RoundedCornerShape(8.dp))
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 11.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Todo : DesignSystem Button component 적용하기
            Text(
                text = "확인",
                color = Color(0xFFFFFFFF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFF000000), shape = CircleShape)
                    .border(width = 1.dp, color = Color(0xFFDFDFDF), shape = CircleShape)
                    .clip(shape = CircleShape)
                    .clickable(enabled = true) { onClickNextButton() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

private class GroupNickNameScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupNickNameUiState> {
    override val values: Sequence<GroupNickNameUiState>
        get() = sequenceOf(
            GroupNickNameUiState(""),
            GroupNickNameUiState(nickName = "he"),
            GroupNickNameUiState(nickName = "hello"),
        )
}

@YGPreview
@Composable
private fun GroupNickNameScreenPreview(
    @PreviewParameter(GroupNickNameScreenPreviewParameterProvider::class) uiState: GroupNickNameUiState,
) = PreviewBox {
    GroupNickNameScreen(
        uiState = uiState,
        onValueChanged = { },
        onClickNextButton = {},
        onClickBackButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
