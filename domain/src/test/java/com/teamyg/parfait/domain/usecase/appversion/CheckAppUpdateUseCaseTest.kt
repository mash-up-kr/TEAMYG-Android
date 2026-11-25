package com.teamyg.parfait.domain.usecase.appversion

import com.teamyg.parfait.domain.model.appversion.AppUpdateCheck
import com.teamyg.parfait.domain.model.appversion.AppUpdateType
import com.teamyg.parfait.domain.model.appversion.AppVersionInfo
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.repository.appversion.AppVersionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckAppUpdateUseCaseTest {
    private val repository: AppVersionRepository = mockk()
    private val useCase = CheckAppUpdateUseCase(repository)

    private fun givenVersionInfo(
        type: AppUpdateType,
        storeUrl: String? = null,
    ) {
        coEvery { repository.checkVersion(any()) } returns Result.success(AppVersionInfo(type, storeUrl))
    }

    @Test
    fun force_returnsRequiredWithStoreUrl() = runTest {
        // Given 서버가 강제 업데이트와 스토어 주소를 준다
        givenVersionInfo(AppUpdateType.FORCE, "market://details?id=x")

        // When 판정한다
        val result = useCase("1.0.0")

        // Then 스토어 주소를 담아 필수로 판정한다
        assertEquals(AppUpdateCheck.Required("market://details?id=x"), result)
    }

    @Test
    fun force_nullStoreUrl_returnsRequiredWithNull() = runTest {
        // Given 강제 업데이트인데 스토어 주소가 없다
        givenVersionInfo(AppUpdateType.FORCE, null)

        // Then 주소 없이 필수로 판정한다
        assertEquals(AppUpdateCheck.Required(null), useCase("1.0.0"))
    }

    @Test
    fun optional_returnsOptionalWithStoreUrl() = runTest {
        // Given 선택 업데이트와 스토어 주소를 준다
        givenVersionInfo(AppUpdateType.OPTIONAL, "market://details?id=x")

        // Then 스토어 주소를 담아 선택으로 판정한다
        assertEquals(AppUpdateCheck.Optional("market://details?id=x"), useCase("1.0.0"))
    }

    @Test
    fun none_returnsNotRequired() = runTest {
        // Given 업데이트가 없다
        givenVersionInfo(AppUpdateType.NONE)

        assertEquals(AppUpdateCheck.NotRequired, useCase("1.0.0"))
    }

    @Test
    fun unknown_returnsNotRequired() = runTest {
        // Given 알 수 없는 유형이다
        givenVersionInfo(AppUpdateType.UNKNOWN)

        assertEquals(AppUpdateCheck.NotRequired, useCase("1.0.0"))
    }

    @Test
    fun failure_returnsNotRequired() = runTest {
        // Given 조회가 네트워크 오류로 실패한다
        coEvery { repository.checkVersion(any()) } returns Result.failure(AppError.Network(IOException()))

        // Then 앱 진입을 막지 않는다
        assertEquals(AppUpdateCheck.NotRequired, useCase("1.0.0"))
    }

    @Test
    fun passesVersionToRepository() = runTest {
        givenVersionInfo(AppUpdateType.NONE)

        // When 버전을 넘겨 판정한다
        useCase("1.4.0")

        // Then 그 버전으로 한 번 조회한다
        coVerify(exactly = 1) { repository.checkVersion("1.4.0") }
    }
}
