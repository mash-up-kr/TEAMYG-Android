package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.usecase.notice.GetNoticeItemsUseCase
import com.teamyg.parfait.domain.usecase.notice.MarkNoticeReadUseCase
import com.teamyg.parfait.domain.usecase.notice.RefreshNoticesUseCase
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
    private val getNoticeItems: GetNoticeItemsUseCase,
    private val refreshNotices: RefreshNoticesUseCase,
    private val markNoticeRead: MarkNoticeReadUseCase,
) : BaseViewModel<NoticeDetailState, NoticeDetailIntent, NoticeDetailSideEffect>(
    initialState = NoticeDetailState(),
) {
    // 캐시가 다시 방출해도 읽음 저장은 이 화면에서 처음 찾았을 때 한 번이면 된다
    private var hasMarkedRead = false

    init {
        viewModelLogger.i { "NoticeDetailViewModel::init" }

        observeNotice()
    }

    override fun processIntent(intent: NoticeDetailIntent) {
        when (intent) {
            NoticeDetailIntent.ClickBack -> postSideEffect(NoticeDetailSideEffect.NavigateBack)
        }
    }

    private fun observeNotice() {
        launch(key = KEY_OBSERVE) {
            getNoticeItems().collect { items ->
                if (items == null) {
                    // 목록 화면을 거치지 않고 열린 경우(프로세스 복원 직후 등)에만 직접 채운다
                    refresh()
                } else {
                    showNotice(items.firstOrNull { it.notice.id == key.noticeId }?.notice)
                }
            }
        }
    }

    // 실패는 화면에 남는 안내 문구로 알린다. 토스트를 더하면 같은 말을 두 번 하게 된다
    private suspend fun refresh() {
        refreshNotices().onFailure {
            viewModelLogger.e(it) { "공지 상세를 위한 목록 조회에 실패했다: ${key.noticeId}" }
            updateState { copy(isUnavailable = true) }
        }
    }

    // 목록을 받았는데 없으면 서버에서 내려간 공지다. 없는 id 는 읽음으로 저장하지 않는다
    private fun showNotice(notice: NoticeVO?) {
        if (notice == null) {
            updateState { copy(notice = null, isUnavailable = true) }
            return
        }

        updateState { copy(notice = notice, isUnavailable = false) }
        markRead()
    }

    // 읽음 저장이 실패해도 화면은 열려 있어야 한다. 다음에 NEW 칩이 한 번 더 보일 뿐이다
    private fun markRead() {
        if (hasMarkedRead) return

        hasMarkedRead = true
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
        const val KEY_OBSERVE = "observeNotice"
        const val KEY_MARK_READ = "markNoticeRead"
    }
}
