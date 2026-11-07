package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import javax.inject.Inject

class GetNoticesUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository,
) {
    suspend operator fun invoke(): Result<List<NoticeVO>> = noticeRepository.getNotices()
}
