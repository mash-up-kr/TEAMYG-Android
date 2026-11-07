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
import com.teamyg.parfait.feature.app.notice.impl.screen.NoticeDetailScreen
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeDetailIntent
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeDetailSideEffect
import com.teamyg.parfait.feature.app.notice.impl.viewmodel.NoticeDetailViewModel

@Composable
internal fun NoticeDetailRoute(
    navigator: Navigator,
    key: NavKeyNoticeDetail,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<NoticeDetailViewModel, NoticeDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(key.noticeId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val loadErrorMessage = stringResource(R.string.notice_load_error)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NoticeDetailSideEffect.NavigateBack -> navigator.onBack()
                NoticeDetailSideEffect.ShowLoadError -> toastPolicy.showError(loadErrorMessage)
            }
        }
    }

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        NoticeDetailScreen(
            state = state,
            toastPolicy = toastPolicy,
            onClickBack = { viewModel.processIntent(NoticeDetailIntent.ClickBack) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
