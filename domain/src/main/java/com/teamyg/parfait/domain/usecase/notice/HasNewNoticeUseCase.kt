package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.model.notice.NoticeItemVO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HasNewNoticeUseCase @Inject constructor(
    private val getNoticeItems: GetNoticeItemsUseCase,
) {
    operator fun invoke(): Flow<Boolean> = getNoticeItems()
        .map { items -> items?.any(NoticeItemVO::isNew) == true }
        .distinctUntilChanged()
}
