package com.teamyg.parfait.data.repository.deeplink

import com.teamyg.parfait.data.installer.deeplink.AppLinkReferrerGateway
import com.teamyg.parfait.data.installer.deeplink.ReferrerLookup
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
            coEvery { getReferrer() } returns ReferrerLookup.Found("inviteCode=ABC123&utm_source=kakao")
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
        coVerify(exactly = 0) { localDataSource.markChecked() }
    }

    @Test
    fun consumeDeepLinkOnce_referrerWithoutInviteCode_returnsNull() = runTest {
        // Given 스토어를 거치지 않은 설치 등 inviteCode 가 없는 referrer
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns ReferrerLookup.Found("utm_source=organic")
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then 값은 받았으니 확인한 것으로 남긴다
        assertNull(result)
        coVerify(exactly = 1) { localDataSource.markChecked() }
    }

    @Test
    fun consumeDeepLinkOnce_urlEncodedInviteCode_returnsDecodedOpenGroupInvite() = runTest {
        // Given referrer 는 URL 인코딩된 채로 온다(%43 = C)
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns ReferrerLookup.Found("inviteCode=AB%43123&utm_source=kakao")
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then
        assertEquals(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"), result)
    }

    @Test
    fun consumeDeepLinkOnce_malformedInviteCode_returnsNull() = runTest {
        // Given 초대코드 규칙(6자 영숫자)에 맞지 않는 값
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns ReferrerLookup.Found("inviteCode=abc")
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then
        assertNull(result)
    }

    @Test
    fun consumeDeepLinkOnce_emptyInviteCode_returnsNull() = runTest {
        // Given 키만 있고 값이 빈 referrer
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns ReferrerLookup.Found("inviteCode=&utm_source=kakao")
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then
        assertNull(result)
    }

    @Test
    fun consumeDeepLinkOnce_notAvailable_returnsNullAndMarksChecked() = runTest {
        // Given 이 기기에서는 다시 물어도 값이 없다는 확정 응답
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns ReferrerLookup.NotAvailable
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then 다시 물을 이유가 없으니 확인한 것으로 남긴다
        assertNull(result)
        coVerify(exactly = 1) { localDataSource.markChecked() }
    }

    @Test
    fun consumeDeepLinkOnce_transientFailure_returnsNullAndKeepsRetryChance() = runTest {
        // Given Play 서비스 연결이 일시적으로 실패함
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway> {
            coEvery { getReferrer() } returns ReferrerLookup.TransientFailure
        }
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        val result = repository.consumeDeepLinkOnce()

        // Then 확인한 것으로 남기면 복원 기회가 영구히 사라지므로 다음 실행에서 다시 시도한다
        assertNull(result)
        coVerify(exactly = 0) { localDataSource.markChecked() }
    }

    @Test
    fun skipLookup_marksCheckedWithoutQueryingGateway() = runTest {
        // Given
        val localDataSource = fakeLocalDataSource(hasChecked = false)
        val gateway = mockk<AppLinkReferrerGateway>()
        val repository = AppLinkReferrerRepositoryImpl(gateway, localDataSource)

        // When
        repository.skipLookup()

        // Then
        coVerify(exactly = 1) { localDataSource.markChecked() }
        coVerify(exactly = 0) { gateway.getReferrer() }
    }

    private fun fakeLocalDataSource(hasChecked: Boolean): AppLinkReferrerLocalDataSource = mockk {
        coEvery { this@mockk.hasChecked() } returns hasChecked
        coEvery { markChecked() } returns Unit
    }
}
