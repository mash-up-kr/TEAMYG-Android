package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import javax.inject.Inject

class MarkNoticeReadUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository,
) {
    suspend operator fun invoke(id: Long) = noticeRepository.markRead(id)
}
