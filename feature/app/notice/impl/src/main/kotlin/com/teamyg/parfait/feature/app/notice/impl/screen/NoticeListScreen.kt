package com.teamyg.parfait.feature.app.notice.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.notice.NoticeItemVO
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.feature.app.notice.impl.R
import com.teamyg.parfait.feature.app.notice.impl.component.ListItemMenuSub
import com.teamyg.parfait.feature.app.notice.impl.util.toNoticeDateText
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeListState
import kotlinx.datetime.LocalDate

private val ToastTopOffset = 13.dp

// 디자인 값 그대로 쓴다 — 가까운 레이아웃 토큰이 없다
private val ContentHorizontalPadding = 19.dp

@Composable
internal fun NoticeListScreen(
    state: NoticeListState,
    toastPolicy: YGToastPolicy,
    onClickBack: () -> Unit,
    onClickNotice: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.notice_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )

            // 토스트는 상단바 아래 영역 안에서만 그린다 — 슬라이드 인이 상단바를 덮지 않게 한다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.notices.forEachIndexed { index, item ->
                        if (index > 0) {
                            item(key = "divider-${item.notice.id}") {
                                YGHorizontalDivider(
                                    modifier = Modifier.padding(horizontal = ContentHorizontalPadding),
                                )
                            }
                        }

                        item(key = item.notice.id) {
                            ListItemMenuSub(
                                title = item.notice.title,
                                subText = item.notice.publishedDate.toNoticeDateText(),
                                isNew = item.isNew,
                                onClick = { onClickNotice(item.notice.id) },
                                modifier = Modifier.padding(horizontal = ContentHorizontalPadding),
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

private val PREVIEW_ITEM = NoticeItemVO(
    notice = NoticeVO(
        id = 1L,
        title = "파르페 v1.5.0 업데이트 안내",
        publishedDate = LocalDate(2026, 11, 12),
        content = "",
    ),
    isNew = false,
)

private class NoticeListPreviewParameterProvider : NamedPreviewParameterProvider<NoticeListState>() {
    override val entries = listOf(
        "notices" to NoticeListState(
            notices = listOf(
                PREVIEW_ITEM.copy(isNew = true),
                PREVIEW_ITEM.copy(notice = PREVIEW_ITEM.notice.copy(id = 2L)),
                PREVIEW_ITEM.copy(
                    notice = PREVIEW_ITEM.notice.copy(
                        id = 3L,
                        title = "아주 긴 제목의 공지사항이 한 줄을 넘어가는 경우를 확인하기 위한 예시 제목",
                    ),
                ),
            ),
        ),
        "empty" to NoticeListState(),
    )
}

@YGPreview
@Composable
private fun NoticeListScreenPreview(
    @PreviewParameter(NoticeListPreviewParameterProvider::class) state: NoticeListState,
) = PreviewBox {
    NoticeListScreen(
        state = state,
        toastPolicy = rememberYGToastPolicy(),
        onClickBack = {},
        onClickNotice = {},
    )
}
