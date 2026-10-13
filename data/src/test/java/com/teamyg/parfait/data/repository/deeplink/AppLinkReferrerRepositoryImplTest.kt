package com.teamyg.parfait.data.repository.deeplink

import com.teamyg.parfait.data.installer.deeplink.AppLinkReferrerGateway
import com.teamyg.parfait.data.source.deeplink.local.AppLinkReferrerLocalDataSource
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinkReferrerRepositoryImplTest {
    @Test
    fun consumeDeepLinkOnce_firstCheckWithInviteCodeReferrer_returnsOpenGroupInvite() = runTest {
        // Given 아직 확인한 적 없고, 스토어가 inviteCode 를 실어 보낸 상황
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns "inviteCode=ABC123&utm_source=kakao"
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then
        assertEquals(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"), result)
        coVerify(exactly = 1) { localDataSource.markChecked() }
    }

    @Test
    fun consumeDeepLinkOnce_alreadyChecked_returnsNullWithoutQueryingGateway() = runTest {
        // Given 이 기기에서 이미 한 번 확인함
        val localDataSource = fakeLocalDataSource(hasChecked = true)
        val gateway = mockk<AppLinkReferrerGateway>()
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then 앱을 열 때마다 같은 화면으로 되돌리지 않아야 하므로 게이트웨이 자체를 안 부른다
        assertNull(result)
        coVerify(exactly = 0) { gateway.getReferrer() }
    }

    @Test
    fun consumeDeepLinkOnce_referrerWithoutInviteCode_returnsNull() = runTest {
        // Given 스토어를 거치지 않은 설치 등 inviteCode 가 없는 referrer
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns "utm_source=organic"
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then
        assertNull(result)
    }

    @Test
    fun consumeDeepLinkOnce_gatewayReturnsNull_returnsNull() = runTest {
        // Given 조회 자체가 실패함
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns null
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then
        assertNull(result)
    }

    private fun fakeLocalDataSource(hasChecked: Boolean): AppLinkReferrerLocalDataSource = mockk {
        coEvery { this@mockk.hasChecked() } returns hasChecked
        coEvery { markChecked() } returns Unit
    }
}
