package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
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
    private val analyticsLogger: AnalyticsLogger,
) : BaseViewModel<NoticeListState, NoticeListIntent, NoticeListSideEffect>(initialState = NoticeListState()) {
    init {
        viewModelLogger.i { "NoticeListViewModel::init" }

        loadNotices()
    }

    override fun processIntent(intent: NoticeListIntent) {
        when (intent) {
            NoticeListIntent.ClickBack -> postSideEffect(NoticeListSideEffect.NavigateBack)
            is NoticeListIntent.ClickNotice -> handleClickNotice(intent.id)
        }
    }

    private fun handleClickNotice(id: Long) {
        // 탭한 시점에 NEW 칩이 붙어 있었는지가 필요해서 이미 받아 둔 목록에서 찾는다.
        // 목록에 없는 id 는 화면이 그린 적 없는 공지라 로그는 건너뛰고 이동만 한다
        val notice = state.value.notices.firstOrNull { it.id == id }
        if (notice != null) {
            analyticsLogger.logEvent(
                AnalyticsEvent(
                    type = AnalyticsEventType.APP_NOTICE_ITEM_TAPPED,
                    params = mapOf(PARAM_NOTICE_ID to id.toString(), PARAM_IS_NEW to notice.isNew.toString()),
                ),
            )
        }
        postSideEffect(NoticeListSideEffect.NavigateToDetail(id))
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
        const val PARAM_NOTICE_ID = "notice_id"
        const val PARAM_IS_NEW = "is_new"
    }
}
