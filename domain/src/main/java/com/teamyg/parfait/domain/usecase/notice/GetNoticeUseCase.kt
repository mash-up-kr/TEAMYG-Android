package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import javax.inject.Inject

class GetNoticeUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository,
) {
    suspend operator fun invoke(id: Long): Result<NoticeVO> = noticeRepository.getNotice(id)
}
