package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SUBJECT_PATH = "/files/recent_images/a.png"
private const val OTHER_PATH = "/files/recent_images/b.png"

class EnsureDraftSubjectRecordedUseCaseTest {
    private val repository: ToppingDraftRepository = mockk()
    private val ensureDraftSubjectRecorded = EnsureDraftSubjectRecordedUseCase(repository)

    private fun givenDraft(subjectImagePath: String?) {
        every { repository.draft } returns flowOf(
            ToppingDraft(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(2L),
                nextPositionZ = 0,
                subjectImagePath = subjectImagePath,
                cutoutImagePath = null,
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = null,
            ),
        )
    }

    @Test
    fun whenDraftAlreadyPointsToTheSubject_doesNotRecordAgain() = runTest {
        givenDraft(SUBJECT_PATH)

        assertTrue(ensureDraftSubjectRecorded(SUBJECT_PATH))

        coVerify(exactly = 0) { repository.record(any(), any(), any(), any(), any()) }
    }

    @Test
    fun whenDraftPointsElsewhere_recordsTheSubjectOnly() = runTest {
        givenDraft(OTHER_PATH)
        coEvery { repository.record(any(), any(), any(), any(), any()) } returns true

        assertTrue(ensureDraftSubjectRecorded(SUBJECT_PATH))

        // 테두리까지 비우는 것이 규약이다 — 알맹이가 바뀌면 그 전 테두리는 설 자리가 없다
        coVerify(exactly = 1) {
            repository.record(
                subjectImagePath = SUBJECT_PATH,
                cutoutImagePath = null,
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = null,
            )
        }
    }

    @Test
    fun whenRecordFails_returnsFalse() = runTest {
        givenDraft(null)
        coEvery { repository.record(any(), any(), any(), any(), any()) } returns false

        assertFalse(ensureDraftSubjectRecorded(SUBJECT_PATH))
    }
}
