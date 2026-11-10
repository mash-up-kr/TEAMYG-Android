package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.notice.GetNoticeItemsUseCase
import com.teamyg.parfait.domain.usecase.notice.RefreshNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoticeListViewModel
@Inject
constructor(
    private val getNoticeItems: GetNoticeItemsUseCase,
    private val refreshNotices: RefreshNoticesUseCase,
    private val analyticsLogger: AnalyticsLogger,
) : BaseViewModel<NoticeListState, NoticeListIntent, NoticeListSideEffect>(initialState = NoticeListState()) {
    init {
        viewModelLogger.i { "NoticeListViewModel::init" }

        observeNotices()
    }

    override fun processIntent(intent: NoticeListIntent) {
        when (intent) {
            NoticeListIntent.ClickBack -> postSideEffect(NoticeListSideEffect.NavigateBack)
            is NoticeListIntent.ClickNotice -> handleClickNotice(intent.id)
        }
    }

    private fun handleClickNotice(id: Long) {
        // 탭한 시점에 NEW 칩이 붙어 있었는지가 필요해서 이미 받아 둔 목록에서 찾는다.
        // 목록에 없는 id 는 화면이 그린 적 없는 공지라 로그도 이동도 하지 않는다
        val item = state.value.notices.firstOrNull { it.notice.id == id } ?: return

        analyticsLogger.logEvent(
            AnalyticsEvent(
                type = AnalyticsEventType.APP_NOTICE_ITEM_TAPPED,
                params = mapOf(PARAM_NOTICE_ID to id.toString(), PARAM_IS_NEW to item.isNew.toString()),
            ),
        )
        postSideEffect(NoticeListSideEffect.NavigateToDetail(item.notice))
    }

    private fun observeNotices() {
        launch(key = KEY_OBSERVE) {
            getNoticeItems().collect { items ->
                if (items == null) {
                    // 설정 화면을 거치지 않고 열린 경우(프로세스 복원 직후 등)에만 직접 채운다
                    refresh()
                } else {
                    updateState { copy(notices = items) }
                }
            }
        }
    }

    private suspend fun refresh() {
        refreshNotices().onFailure {
            viewModelLogger.e(it) { "공지사항 목록 조회에 실패했다" }
            postSideEffect(NoticeListSideEffect.ShowLoadError)
        }
    }

    private companion object {
        const val KEY_OBSERVE = "observeNotices"
        const val PARAM_NOTICE_ID = "notice_id"
        const val PARAM_IS_NEW = "is_new"
    }
}
