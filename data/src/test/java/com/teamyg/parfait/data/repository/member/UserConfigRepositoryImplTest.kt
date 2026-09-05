package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.data.source.member.local.UserConfigLocalDataSource
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.member.UserConfigVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UserConfigRepositoryImplTest {
    /**
     * [UserConfigRepositoryImpl.userConfig] 가 생성자에서 곧바로 `localDataSource.userConfig` 를
     * 읽으므로, 기본 스텁이 없으면 strict mock 이 생성 시점에 MockKException 을 던진다.
     */
    private val localDataSource: UserConfigLocalDataSource = mockk {
        every { userConfig } returns flowOf(null)
    }

    @Test
    fun markTutorialSeen_withNothingStored_stillWrites() = runTest {
        // Given 앱을 막 설치해 저장분이 없다
        val repository = UserConfigRepositoryImpl(localDataSource)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 캔버스 튜토리얼을 끝까지 본 것으로 남긴다
        repository.markTutorialSeen(TutorialKind.CANVAS)

        // Then 저장분이 없어도 써야 한다 — 안 쓰면 다음 진입에서 튜토리얼이 다시 뜬다
        coVerify(exactly = 1) {
            localDataSource.save(UserConfigVO(seenTutorials = setOf(TutorialKind.CANVAS)))
        }
    }

    @Test
    fun markTutorialSeen_keepsTutorialsSeenEarlier() = runTest {
        // Given 캔버스 튜토리얼은 이미 봤다
        every { localDataSource.userConfig } returns
            flowOf(UserConfigVO(seenTutorials = setOf(TutorialKind.CANVAS)))
        val repository = UserConfigRepositoryImpl(localDataSource)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 업로드 튜토리얼을 마친다
        repository.markTutorialSeen(TutorialKind.UPLOAD)

        // Then 먼저 끝낸 기록을 덮지 않는다 — 덮으면 이미 본 튜토리얼이 되살아난다
        coVerify(exactly = 1) {
            localDataSource.save(
                UserConfigVO(seenTutorials = setOf(TutorialKind.CANVAS, TutorialKind.UPLOAD)),
            )
        }
    }
}
