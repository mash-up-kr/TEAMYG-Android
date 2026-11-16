package com.teamyg.parfait.data.source.notice.local

import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoticeLocalDataSourceImpl @Inject constructor() : NoticeLocalDataSource {
    private val _notices = MutableStateFlow<List<NoticeVO>?>(null)
    override val notices: StateFlow<List<NoticeVO>?> = _notices.asStateFlow()

    override fun saveNotices(notices: List<NoticeVO>) {
        _notices.value = notices
    }

    override fun clear() {
        _notices.value = null
    }
}
