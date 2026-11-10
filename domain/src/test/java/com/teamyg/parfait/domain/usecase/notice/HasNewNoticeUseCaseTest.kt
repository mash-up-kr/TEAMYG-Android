package com.teamyg.parfait.domain.usecase.notice

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.notice.NoticeItemVO
import com.teamyg.parfait.domain.model.notice.NoticeVO
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasNewNoticeUseCaseTest {
    private fun item(id: Long, isNew: Boolean) = NoticeItemVO(
        notice = NoticeVO(id = id, title = "공지", publishedDate = LocalDate(2026, 11, 17), content = "본문"),
        isNew = isNew,
    )

    private fun useCase(items: Flow<List<NoticeItemVO>?>): HasNewNoticeUseCase {
        val getNoticeItems: GetNoticeItemsUseCase = mockk()
        every { getNoticeItems() } returns items
        return HasNewNoticeUseCase(getNoticeItems)
    }

    @Test
    fun invoke_allRead_emitsFalse() = runTest {
        useCase(flowOf(listOf(item(1L, false), item(2L, false)))).invoke().test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_someNewNotice_emitsTrue() = runTest {
        useCase(flowOf(listOf(item(1L, false), item(2L, true)))).invoke().test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_cacheNotLoaded_emitsFalse() = runTest {
        useCase(flowOf(null)).invoke().test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_emptyList_emitsFalse() = runTest {
        useCase(flowOf(emptyList())).invoke().test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }
}
