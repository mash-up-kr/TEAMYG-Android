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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

        // Then 경로만 비운다 — 세그멘테이션 진입이 그 디렉토리를 통째로 비우므로 그대로
        // 두면 읽는 쪽이 있지도 않은 파일을 올리려 들지만, 캔버스 식별값(`parfaitId` 등)까지
        // 잃으면 흐름 진입 때 못 박은 값이 사라진다
        assertEquals(draft(subjectImagePath = null, cutoutImagePath = null), read)
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

    @Test
    fun record_keepsCanvasIdentity_andFillsImages() = runTest {
        // Given 흐름 진입만 마친 초안(이미지·테두리가 비어 있다)
        givenStoredDraft(draft(subjectImagePath = null, cutoutImagePath = null))
        val saved = slot<ToppingDraft>()
        coEvery { toppingDraftLocalDataSource.save(capture(saved)) } returns Unit

        // When 세그멘테이션 결과를 적는다
        val recorded = repository().record(
            subjectImagePath = "/cache/segmentation/subject.png",
            cutoutImagePath = "/cache/segmentation/cutout.png",
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 진입 때 못 박은 캔버스 식별값은 건드리지 않는다 — 그것이 이 배치의 전제다
        assertTrue(recorded)
        assertEquals(
            ToppingDraft(
                groupId = GROUP_ID,
                parfaitId = PARFAIT_ID,
                nextPositionZ = 4,
                subjectImagePath = "/cache/segmentation/subject.png",
                cutoutImagePath = "/cache/segmentation/cutout.png",
            ),
            saved.captured,
        )
    }

    @Test
    fun record_withoutBorder_dropsThePreviousOne() = runTest {
        // Given 테두리까지 적혀 있던 초안
        givenStoredDraft(
            draft(
                subjectImagePath = "/cache/segmentation/old.png",
                cutoutImagePath = "/cache/segmentation/old-cutout.png",
            ).copy(borderColorArgb = 0xFFFF0000.toInt(), borderWidthDp = 10f),
        )
        val saved = slot<ToppingDraft>()
        coEvery { toppingDraftLocalDataSource.save(capture(saved)) } returns Unit

        // When 테두리를 벗긴 편집 결과를 적는다
        repository().record(
            subjectImagePath = "/cache/segmentation/new.png",
            cutoutImagePath = "/cache/segmentation/new-cutout.png",
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 지난 테두리가 살아남지 않는다 — 병합하면 방금 벗긴 테두리가 배치까지 따라간다
        assertNull(saved.captured.borderColorArgb)
        assertNull(saved.captured.borderWidthDp)
    }

    @Test
    fun record_withNullCutoutPath_keepsDraftWritable() = runTest {
        // Given 흐름이 열려 있다
        givenStoredDraft(draft(subjectImagePath = null, cutoutImagePath = null))
        val repository = repository()

        // When 재편집 마스크 없이 알맹이만 적는다
        val recorded = repository.record(
            subjectImagePath = "/data/files/recent_images/b.png",
            cutoutImagePath = null,
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 적힌다 — 최근 목록에서 되살린 알맹이에는 마스크가 없다
        assertTrue(recorded)

        val saved = slot<ToppingDraft>()
        coVerify { toppingDraftLocalDataSource.save(capture(saved)) }
        assertEquals("/data/files/recent_images/b.png", saved.captured.subjectImagePath)
        assertNull(saved.captured.cutoutImagePath)
    }

    @Test
    fun record_withNoOpenFlow_writesNothing() = runTest {
        // Given 흐름 밖이다(진입이 초안을 쓰지 못했거나 이미 비워졌다)
        givenStoredDraft(null)

        // When 결과를 적으려 한다
        val recorded = repository().record(
            subjectImagePath = "/cache/segmentation/subject.png",
            cutoutImagePath = "/cache/segmentation/cutout.png",
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 캔버스 식별값 없는 초안을 지어내지 않는다 — 그걸 만들면 배치까지 가서야 올릴 데가
        // 없다는 것을 알게 된다
        assertFalse(recorded)
        coVerify(exactly = 0) { toppingDraftLocalDataSource.save(any()) }
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
    }
}
