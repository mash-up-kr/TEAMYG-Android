package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.data.source.member.local.UserConfigLocalDataSource
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
    private val repository = UserConfigRepositoryImpl(localDataSource)

    @Test
    fun updateIsShowCanvasTutorial_withNothingStored_stillWrites() = runTest {
        // Given 앱을 막 설치해 저장분이 없다
        coEvery { localDataSource.save(any()) } returns Unit

        // When 튜토리얼을 끝까지 본 것으로 남긴다
        repository.updateIsShowCanvasTutorial(false)

        // Then 저장분이 없어도 써야 한다 — 안 쓰면 다음 진입에서 튜토리얼이 다시 뜬다
        coVerify(exactly = 1) { localDataSource.save(UserConfigVO(isShowCanvasTutorial = false)) }
    }

    @Test
    fun updateIsShowCanvasTutorial_withStoredConfig_keepsTheRestOfIt() = runTest {
        // Given 이미 저장된 설정이 있다
        every { localDataSource.userConfig } returns flowOf(UserConfigVO(isShowCanvasTutorial = true))
        val repository = UserConfigRepositoryImpl(localDataSource)
        coEvery { localDataSource.save(any()) } returns Unit

        // When 튜토리얼 항목만 끈다
        repository.updateIsShowCanvasTutorial(false)

        // Then 나머지 항목이 늘어나도 이 갱신이 그것들을 초기화하지 않는다
        coVerify(exactly = 1) {
            localDataSource.save(UserConfigVO(isShowCanvasTutorial = true).copy(isShowCanvasTutorial = false))
        }
    }
}
