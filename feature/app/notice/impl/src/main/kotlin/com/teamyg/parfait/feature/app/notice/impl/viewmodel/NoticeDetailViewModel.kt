package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.notice.GetNoticeUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = NoticeDetailViewModel.Factory::class)
class NoticeDetailViewModel
@AssistedInject
constructor(
    @Assisted private val noticeId: Long,
    private val getNotice: GetNoticeUseCase,
) : BaseViewModel<NoticeDetailState, NoticeDetailIntent, NoticeDetailSideEffect>(initialState = NoticeDetailState()) {
    init {
        viewModelLogger.i { "NoticeDetailViewModel::init" }

        loadNotice()
    }

    override fun processIntent(intent: NoticeDetailIntent) {
        when (intent) {
            NoticeDetailIntent.ClickBack -> postSideEffect(NoticeDetailSideEffect.NavigateBack)
        }
    }

    private fun loadNotice() {
        launch(key = KEY_LOAD) {
            getNotice(noticeId)
                .onSuccess { notice -> updateState { copy(notice = notice) } }
                .onFailure {
                    viewModelLogger.e(it) { "공지사항 상세 조회에 실패했다: $noticeId" }
                    postSideEffect(NoticeDetailSideEffect.ShowLoadError)
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(noticeId: Long): NoticeDetailViewModel
    }

    private companion object {
        const val KEY_LOAD = "loadNotice"
    }
}
