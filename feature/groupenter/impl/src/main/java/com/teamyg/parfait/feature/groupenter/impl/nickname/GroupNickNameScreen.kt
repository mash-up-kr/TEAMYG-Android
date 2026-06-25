package com.teamyg.parfait.feature.groupenter.impl.nickname

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.feature.groupenter.impl.R

@Composable
internal fun GroupNickNameScreen(
    uiState: GroupNickNameUiState,
    onValueChanged: (word: String) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(all = 21.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_chevron_left),
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
                        text = "3/3",
                        color = Color(0x80333333),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "“그룹이름”에서 사용할\n닉네임을 입력해 주세요",
                        color = Color(0xFF333333),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "“그룹이름”에서만 공유되는 닉네임이에요\n0자까지 입력 가능해요",
                        color = Color(0xFF737373),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "닉네임",
                        color = Color(0x80000000),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                            .border(width = 1.dp, color = Color(0xFFDFDFDF), shape = RoundedCornerShape(8.dp))
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 11.dp)
                    )
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
