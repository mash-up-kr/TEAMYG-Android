package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun TermAgreeScreen(
    state: TermAgreeState,
    onClickTermAgree: (index: Int, newSelected: Boolean) -> Unit,
    onClickTermLandingUrl: (landingUrl: String?) -> Unit,
    onClickAgreeAllTerm: (newSelected: Boolean) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // Todo : topbar component 로 변경
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            // Todo : ic_caret_right 아이콘으로 교체
            Spacer(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.Red)
                    .clickable { onClickBackButton() },
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 33.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item {
                Text(
                    text = "서비스 이용 약관에\n동의해 주세요",
                    color = Color(0xFF7A7D82),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFECECEE), shape = RoundedCornerShape(4.dp))
                        .clip(shape = RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .clickable { onClickAgreeAllTerm(state.isAllSelected.not()) },
                ) {
                    // Todo : ic_check_button 아이콘으로 교체
                    Spacer(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                            .background(Color.Red),
                    )
                    Text(
                        text = "모두 동의하기",
                        color = Color(0xFF29292C),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            itemsIndexed(state.termContentList) { index, termContent ->
                if (index == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Todo : ic_check 아이콘으로 교체
                    Spacer(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color.Red)
                            .clickable { onClickTermAgree(index, state.selectedList[index].not()) },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = termContent.visibleText,
                        color = Color(0xFF7A7D82),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClickTermAgree(index, state.selectedList[index].not()) },

                        )
                    // Todo : ic_caret_right 아이콘으로 교체
                    Spacer(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color.Red)
                            .clickable { onClickTermLandingUrl(termContent.landingUrl) },
                    )
                }
            }
        }

        // Todo : large button component 로 변경
        Text(
            text = "확인",
            textAlign = TextAlign.Center,
            color = Color(if (state.isAvailable) 0xFFFAFAFA else 0xFF7A7D82),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 2.dp)
                .background(
                    color = Color(if (state.isAvailable) 0xFF29292C else 0xFFDDDEE0),
                    shape = RoundedCornerShape(99.dp),
                )
                .clip(shape = RoundedCornerShape(99.dp))
                .padding(vertical = 12.dp)
                .clickable { onClickNextButton() },
        )
    }
}

@YGPreview
@Composable
private fun TermAgreeScreenPreview() = PreviewBox {
    TermAgreeScreen(
        state = TermAgreeState(),
        onClickTermAgree = { _, _ -> },
        onClickTermLandingUrl = {},
        onClickAgreeAllTerm = {},
        onClickNextButton = {},
        onClickBackButton = {},
        modifier = Modifier.fillMaxSize(),
    )
}
