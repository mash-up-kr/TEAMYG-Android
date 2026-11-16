package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.model.notice.NoticeItemVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘은 방출할 때마다 새로 구한다. 자정을 넘겨도 스스로 다시 방출하지는 않고, 다음 읽음
 * 변화나 재구독 때 갱신된다.
 */
class GetNoticeItemsUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<NoticeItemVO>?> = combine(
        noticeRepository.notices,
        noticeRepository.readNoticeIds,
    ) { notices, readIds ->
        val today = clock.now().toLocalDateTime(SEOUL).date

        notices?.map { notice ->
            NoticeItemVO(
                notice = notice,
                isNew = notice.publishedDate.daysUntil(today) < NEW_DAYS && notice.id !in readIds,
            )
        }
    }

    private companion object {
        const val NEW_DAYS = 14

        // 서버 게시일이 KST 기준이라 같은 시간대로 "며칠 지났는지"를 센다
        val SEOUL: TimeZone = TimeZone.of("Asia/Seoul")
    }
}
