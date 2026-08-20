package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToppingDraftRepositoryImplTest {
    private val toppingDraftLocalDataSource: ToppingDraftLocalDataSource = mockk(relaxUnitFun = true)

    /**
     * `draft` 는 구현의 생성자 초기화식이 곧바로 읽으므로 저장소를 만들기 전에 답이 있어야 한다.
     * `relaxUnitFun` 은 Unit 을 돌려주는 함수만 채워 주고 이 프로퍼티는 채우지 않는다.
     */
    @Before
    fun stubEmptyStore() {
        givenStoredDraft(null)
    }

    private fun repository() = ToppingDraftRepositoryImpl(toppingDraftLocalDataSource)

    private fun givenStoredDraft(draft: ToppingDraft?) {
        every { toppingDraftLocalDataSource.draft } returns flowOf(draft)
    }

    private fun draft(
        subjectImagePath: String?,
        cutoutImagePath: String?,
    ) = ToppingDraft(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        nextPositionZ = 4,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
    )

    @Test
    fun start_writesAFreshDraft_withNoImageOrBorder() = runTest {
        // Given 흐름에 들어선다
        val saved = slot<ToppingDraft>()
        coEvery { toppingDraftLocalDataSource.save(capture(saved)) } returns Unit

        // When 캔버스 식별값으로 흐름을 연다
        repository().start(groupId = GROUP_ID, parfaitId = PARFAIT_ID, nextPositionZ = 4)

        // Then 이미지와 테두리는 비어 있다 — 진입 시 덮어쓰는 이 규칙 하나가 지난 흐름의
        // 이미지가 따라붙는 것을 막는다
        assertEquals(
            ToppingDraft(groupId = GROUP_ID, parfaitId = PARFAIT_ID, nextPositionZ = 4),
            saved.captured,
        )
    }

    @Test
    fun draft_pathsPointToMissingFiles_areBlankedOut() = runTest {
        // Given 저장된 초안이 이미 지워진 캐시 파일을 가리킨다
        givenStoredDraft(
            draft(
                subjectImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/gone.png",
                cutoutImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/gone-cutout.png",
            ),
        )

        // When 초안을 읽는다
        val read = repository().draft.first()

        // Then 경로를 흘리지 않는다 — 세그멘테이션 진입이 그 디렉토리를 통째로 비우므로
        // 그대로 두면 읽는 쪽이 있지도 않은 파일을 올리려 든다
        assertNull(read?.subjectImagePath)
        assertNull(read?.cutoutImagePath)
    }

    @Test
    fun draft_pathsPointToRealFiles_areKept() = runTest {
        // Given 파일이 아직 살아 있는 초안
        val subject = File.createTempFile("subject", ".png").apply { deleteOnExit() }
        val cutout = File.createTempFile("cutout", ".png").apply { deleteOnExit() }
        givenStoredDraft(
            draft(subjectImagePath = subject.absolutePath, cutoutImagePath = cutout.absolutePath),
        )

        // When 초안을 읽는다
        val read = repository().draft.first()

        // Then 살아 있는 경로까지 지우지 않는다
        assertEquals(subject.absolutePath, read?.subjectImagePath)
        assertEquals(cutout.absolutePath, read?.cutoutImagePath)
    }

    @Test
    fun draft_nothingStored_isNull() = runTest {
        // Given 흐름 밖이다
        givenStoredDraft(null)

        // Then 빈 초안을 지어내지 않는다
        assertNull(repository().draft.first())
    }

    @Test
    fun clear_delegatesToTheStore() = runTest {
        // Given, When 초안을 비운다
        repository().clear()

        // Then 저장소에서 지운다 — 지우지 않으면 다음 흐름의 진입 전까지 낡은 초안이 읽힌다
        coVerify(exactly = 1) { toppingDraftLocalDataSource.clear() }
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
    }
}
