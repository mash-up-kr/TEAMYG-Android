package com.teamyg.parfait.feature.app.notice.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.notice.api.NavKeyNoticeDetail
import com.teamyg.parfait.feature.app.notice.impl.R
import com.teamyg.parfait.feature.app.notice.impl.screen.NoticeListScreen
import com.teamyg.parfait.feature.app.notice.impl.util.toNoticeDateText
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeListIntent
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeListSideEffect
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeListViewModel

@Composable
internal fun NoticeListRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: NoticeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val loadErrorMessage = stringResource(R.string.notice_load_error)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NoticeListSideEffect.NavigateBack -> navigator.onBack()
                is NoticeListSideEffect.NavigateToDetail -> navigator.goTo(
                    NavKeyNoticeDetail(
                        noticeId = effect.notice.id,
                        title = effect.notice.title,
                        date = effect.notice.publishedDate.toNoticeDateText(),
                        content = effect.notice.content,
                    ),
                )
                NoticeListSideEffect.ShowLoadError -> toastPolicy.showError(loadErrorMessage)
            }
        }
    }

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        NoticeListScreen(
            state = state,
            toastPolicy = toastPolicy,
            onClickBack = { viewModel.processIntent(NoticeListIntent.ClickBack) },
            onClickNotice = { viewModel.processIntent(NoticeListIntent.ClickNotice(it)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
