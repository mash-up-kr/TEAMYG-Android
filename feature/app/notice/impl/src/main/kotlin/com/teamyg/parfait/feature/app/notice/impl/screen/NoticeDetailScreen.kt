package com.teamyg.parfait.feature.app.notice.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.feature.app.notice.impl.R
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeDetailState

private val ToastTopOffset = 13.dp

@Composable
internal fun NoticeDetailScreen(
    state: NoticeDetailState,
    toastPolicy: YGToastPolicy,
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            ) {
                state.notice?.let { notice ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                                text = notice.title,
                                style = YGTheme.typography.body.b02SB,
                                color = YGAtomicColors.Gray.Gray900,
                            )

                            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap2))

                            Text(
                                text = notice.date,
                                style = YGTheme.typography.caption.c01R,
                                color = YGAtomicColors.Gray.Gray500,
                            )

                            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                            YGHorizontalDivider()

                            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                            Text(
                                text = notice.content,
                                style = YGTheme.typography.body.b02R,
                                color = YGAtomicColors.Gray.Gray800,
                            )
                        }
                    }
                }

                YGToastHost(
                    policy = toastPolicy,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ToastTopOffset)
                        .fillMaxWidth(),
                )
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
            notice = NoticeVO(
                id = 2L,
                title = "파르페 v1.5.0 업데이트 안내",
                date = "2026.11.12",
                content = "- 알림 설정이 생겼어요\n- 오늘의 캔버스 알림을 켜고 끌 수 있어요",
                isNew = false,
            ),
        ),
        toastPolicy = rememberYGToastPolicy(),
        onClickBack = {},
    )
}
