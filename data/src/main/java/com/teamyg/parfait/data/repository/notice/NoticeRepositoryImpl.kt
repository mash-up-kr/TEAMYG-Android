package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 연동 전 임시 구현이다. 고정된 공지를 메모리에서 내려주고, 서버 API 가 정해지면 이 클래스를
 * 원격 데이터소스 호출로 교체한다.
 */
@Singleton
class NoticeRepositoryImpl @Inject constructor() : NoticeRepository {
    private val notices = listOf(
        NoticeVO(
            id = 1L,
            title = "이용약관 및 개인정보처리방침 개정 안내",
            date = "2026.11.17",
            content = "이용약관 및 개인정보처리방침이 개정되었어요.",
            isNew = true,
        ),
        NoticeVO(
            id = 2L,
            title = "파르페 v1.5.0 업데이트 안내",
            date = "2026.11.12",
            content = """
                - 알림 설정이 생겼어요
                - 오늘의 캔버스 알림을 켜고 끌 수 있어요
                - 그룹원 활동 알림을 모아서 받을 수 있어요
                - 사이드 메뉴에 공지사항이 생겼어요
            """.trimIndent(),
            isNew = false,
        ),
        NoticeVO(
            id = 3L,
            title = "파르페 v1.4.0 업데이트 안내",
            date = "2026.11.09",
            content = "파르페 v1.4.0 업데이트 내용이에요.",
            isNew = false,
        ),
        NoticeVO(
            id = 4L,
            title = "파르페 v1.3.0 업데이트 안내",
            date = "2026.11.02",
            content = "파르페 v1.3.0 업데이트 내용이에요.",
            isNew = false,
        ),
        NoticeVO(
            id = 5L,
            title = "개인정보처리방침 개정 안내",
            date = "2026.10.29",
            content = "개인정보처리방침이 개정되었어요.",
            isNew = false,
        ),
    )

    override suspend fun getNotices(): Result<List<NoticeVO>> = Result.success(notices)

    override suspend fun getNotice(id: Long): Result<NoticeVO> {
        val notice = notices.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("공지사항을 찾을 수 없다: $id"))

        return Result.success(notice)
    }
}
