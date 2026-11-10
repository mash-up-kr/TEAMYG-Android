package com.teamyg.parfait.feature.app.notice.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.notice.impl.R
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeDetailState

@Composable
internal fun NoticeDetailScreen(
    state: NoticeDetailState,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.notice_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = YGTheme.layout.padding.padding7),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(YGAtomicColors.Transparency.White75)
                        .border(width = 1.dp, color = YGAtomicColors.Cherry.Cherry100)
                        .padding(YGTheme.layout.padding.padding6),
                ) {
                    Text(
                        text = state.title,
                        style = YGTheme.typography.body.b02SB,
                        color = YGAtomicColors.Gray.Gray900,
                    )

                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap2))

                    Text(
                        text = state.date,
                        style = YGTheme.typography.caption.c01R,
                        color = YGAtomicColors.Gray.Gray500,
                    )

                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                    YGHorizontalDivider()

                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                    Text(
                        text = state.content,
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray800,
                    )
                }
            }
        }

        OnBack { onClickBack() }
    }
}

@YGPreview
@Composable
private fun NoticeDetailScreenPreview() = PreviewBox {
    NoticeDetailScreen(
        state = NoticeDetailState(
            title = "파르페 v1.5.0 업데이트 안내",
            date = "2026.11.12",
            content = "- 알림 설정이 생겼어요\n- 오늘의 캔버스 알림을 켜고 끌 수 있어요",
        ),
        onClickBack = {},
    )
}
