package com.teamyg.parfait.domain.usecase.deeplink

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import com.teamyg.parfait.domain.repository.deeplink.AppLinkReferrerRepository
import com.teamyg.parfait.domain.usecase.session.HasActiveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsumeInstallReferrerDeepLinkUseCaseTest {
    private val repository = mockk<AppLinkReferrerRepository>(relaxed = true)
    private val hasActiveSession = mockk<HasActiveSessionUseCase>()
    private val useCase = ConsumeInstallReferrerDeepLinkUseCase(repository, hasActiveSession)

    @Test
    fun invoke_noSession_returnsDeepLinkFromRepository() = runTest {
        // Given 세션이 없는 첫 실행 기기
        val deepLink = AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123")
        coEvery { hasActiveSession() } returns false
        coEvery { repository.consumeDeepLinkOnce() } returns deepLink

        // When
        val result = useCase()

        // Then
        assertEquals(deepLink, result)
        coVerify(exactly = 0) { repository.skipLookup() }
    }

    @Test
    fun invoke_existingSession_skipsLookupAndReturnsNull() = runTest {
        // Given 이 기능이 들어간 버전으로 업데이트한 기존 유저
        coEvery { hasActiveSession() } returns true

        // When
        val result = useCase()

        // Then 설치 전 링크가 없으니 조회하지 않고 확인함만 남긴다
        assertNull(result)
        coVerify(exactly = 1) { repository.skipLookup() }
        coVerify(exactly = 0) { repository.consumeDeepLinkOnce() }
    }
}
