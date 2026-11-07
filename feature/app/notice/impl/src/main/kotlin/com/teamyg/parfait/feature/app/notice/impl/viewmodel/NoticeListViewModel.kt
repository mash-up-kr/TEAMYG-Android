package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.notice.GetNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoticeListViewModel
@Inject
constructor(
    private val getNotices: GetNoticesUseCase,
) : BaseViewModel<NoticeListState, NoticeListIntent, NoticeListSideEffect>(initialState = NoticeListState()) {
    init {
        viewModelLogger.i { "NoticeListViewModel::init" }

        loadNotices()
    }

    override fun processIntent(intent: NoticeListIntent) {
        when (intent) {
            NoticeListIntent.ClickBack -> postSideEffect(NoticeListSideEffect.NavigateBack)
            is NoticeListIntent.ClickNotice -> postSideEffect(NoticeListSideEffect.NavigateToDetail(intent.id))
        }
    }

    private fun loadNotices() {
        launch(key = KEY_LOAD) {
            getNotices()
                .onSuccess { notices -> updateState { copy(notices = notices) } }
                .onFailure {
                    viewModelLogger.e(it) { "공지사항 목록 조회에 실패했다" }
                    postSideEffect(NoticeListSideEffect.ShowLoadError)
                }
        }
    }

    private companion object {
        const val KEY_LOAD = "loadNotices"
    }
}
