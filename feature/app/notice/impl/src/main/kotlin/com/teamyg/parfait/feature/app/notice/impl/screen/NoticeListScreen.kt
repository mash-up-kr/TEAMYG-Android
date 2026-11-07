package com.teamyg.parfait.feature.app.notice.impl.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.feature.app.notice.impl.R
import com.teamyg.parfait.feature.app.notice.impl.component.NoticeListItem
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeListState

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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.notices, key = { it.id }) { notice ->
                        NoticeListItem(
                            notice = notice,
                            onClick = { onClickNotice(notice.id) },
                        )
                    }
                }

                YGToastHost(
                    policy = toastPolicy,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )
            }
        }

        OnBack { onClickBack() }
    }
}

private class NoticeListPreviewParameterProvider : PreviewParameterProvider<NoticeListState> {
    override val values: Sequence<NoticeListState>
        get() {
            val notice = NoticeVO(
                id = 1L,
                title = "파르페 v1.5.0 업데이트 안내",
                date = "2026.11.12",
                content = "",
                isNew = false,
            )

            return sequenceOf(
                NoticeListState(
                    notices = listOf(
                        notice.copy(id = 1L, isNew = true),
                        notice.copy(id = 2L),
                        notice.copy(id = 3L, title = "아주 긴 제목의 공지사항이 한 줄을 넘어가는 경우를 확인하기 위한 예시 제목"),
                    ),
                ),
                NoticeListState(),
            )
        }
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
