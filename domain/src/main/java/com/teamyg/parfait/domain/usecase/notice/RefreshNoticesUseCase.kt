package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import javax.inject.Inject

class RefreshNoticesUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository,
) {
    suspend operator fun invoke(): Result<Unit> = noticeRepository.refreshNotices()
}
