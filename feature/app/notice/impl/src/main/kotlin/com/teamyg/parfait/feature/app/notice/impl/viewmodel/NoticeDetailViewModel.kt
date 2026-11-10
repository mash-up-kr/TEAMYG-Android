package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.usecase.notice.MarkNoticeReadUseCase
import com.teamyg.parfait.feature.app.notice.api.NavKeyNoticeDetail
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = NoticeDetailViewModel.Factory::class)
class NoticeDetailViewModel
@AssistedInject
constructor(
    @Assisted private val key: NavKeyNoticeDetail,
    private val markNoticeRead: MarkNoticeReadUseCase,
) : BaseViewModel<NoticeDetailState, NoticeDetailIntent, NoticeDetailSideEffect>(
    initialState = NoticeDetailState(title = key.title, date = key.date, content = key.content),
) {
    init {
        viewModelLogger.i { "NoticeDetailViewModel::init" }

        markRead()
    }

    override fun processIntent(intent: NoticeDetailIntent) {
        when (intent) {
            NoticeDetailIntent.ClickBack -> postSideEffect(NoticeDetailSideEffect.NavigateBack)
        }
    }

    // 읽음 저장이 실패해도 화면은 열려 있어야 한다. 다음에 NEW 칩이 한 번 더 보일 뿐이다
    private fun markRead() {
        launch(key = KEY_MARK_READ) {
            runSuspendCatching { markNoticeRead(key.noticeId) }
                .onFailure { viewModelLogger.e(it) { "공지 읽음 처리에 실패했다: ${key.noticeId}" } }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(key: NavKeyNoticeDetail): NoticeDetailViewModel
    }

    private companion object {
        const val KEY_MARK_READ = "markNoticeRead"
    }
}
