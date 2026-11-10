package com.teamyg.parfait.domain.usecase.notice

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetNoticeItemsUseCaseTest {
    // 한국 시간 2026-11-17 12:00
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-17T03:00:00Z")
    }

    private fun notice(
        id: Long,
        publishedDate: String,
    ) = NoticeVO(
        id = id,
        title = "공지 $id",
        publishedDate = LocalDate.parse(publishedDate),
        content = "본문 $id",
    )

    private fun useCase(
        notices: Flow<List<NoticeVO>?>,
        readIds: Flow<Set<Long>> = flowOf(emptySet()),
    ): GetNoticeItemsUseCase {
        val repository: NoticeRepository = mockk()
        every { repository.notices } returns notices
        every { repository.readNoticeIds } returns readIds
        return GetNoticeItemsUseCase(repository, clock)
    }

    @Test
    fun invoke_publishedThirteenDaysAgo_isNew() = runTest {
        // Given 13일 전에 게시된 안 읽은 공지
        val useCase = useCase(flowOf(listOf(notice(1L, "2026-11-04"))))

        // When 항목을 조회한다
        useCase().test {
            // Then NEW 다
            assertTrue(awaitItem()!!.single().isNew)
            awaitComplete()
        }
    }

    @Test
    fun invoke_publishedFourteenDaysAgo_isNotNew() = runTest {
        // Given 14일 전에 게시된 공지 — 경계값이라 NEW 가 아니다
        val useCase = useCase(flowOf(listOf(notice(1L, "2026-11-03"))))

        useCase().test {
            assertFalse(awaitItem()!!.single().isNew)
            awaitComplete()
        }
    }

    @Test
    fun invoke_readNotice_isNotNew() = runTest {
        // Given 13일 전 공지인데 이미 읽었다
        val useCase = useCase(flowOf(listOf(notice(1L, "2026-11-04"))), flowOf(setOf(1L)))

        useCase().test {
            assertFalse(awaitItem()!!.single().isNew)
            awaitComplete()
        }
    }

    @Test
    fun invoke_cacheNotLoaded_emitsNull() = runTest {
        // Given 아직 서버에서 받지 못했다
        val useCase = useCase(flowOf(null))

        useCase().test {
            // Then 빈 목록과 구분되도록 null 을 그대로 낸다
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_emptyList_emitsEmptyList() = runTest {
        // Given 공지가 0건이다
        val useCase = useCase(flowOf(emptyList()))

        useCase().test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_readIdsNotInList_areIgnored() = runTest {
        // Given 목록에 없는 id 만 읽음 집합에 있다
        val useCase = useCase(flowOf(listOf(notice(1L, "2026-11-04"))), flowOf(setOf(99L)))

        useCase().test {
            // Then 목록의 공지는 그대로 NEW 다
            assertTrue(awaitItem()!!.single().isNew)
            awaitComplete()
        }
    }

    @Test
    fun invoke_serverOrderIsKept() = runTest {
        // Given 서버가 최신순 3, 2, 1 로 준다
        val notices = listOf(notice(3L, "2026-11-16"), notice(2L, "2026-11-15"), notice(1L, "2026-11-14"))
        val useCase = useCase(flowOf(notices))

        useCase().test {
            // Then 순서를 바꾸지 않는다
            assertEquals(listOf(3L, 2L, 1L), awaitItem()!!.map { it.notice.id })
            awaitComplete()
        }
    }

    @Test
    fun invoke_markedReadLater_flipsToNotNew() = runTest {
        // Given 안 읽은 새 공지를 구독 중이다
        val readIds = MutableStateFlow(emptySet<Long>())
        val useCase = useCase(flowOf(listOf(notice(1L, "2026-11-04"))), readIds)

        useCase().test {
            assertTrue(awaitItem()!!.single().isNew)

            // When 그 공지를 읽는다
            readIds.value = setOf(1L)

            // Then 같은 항목이 NEW 에서 내려간다
            assertFalse(awaitItem()!!.single().isNew)
        }
    }
}
