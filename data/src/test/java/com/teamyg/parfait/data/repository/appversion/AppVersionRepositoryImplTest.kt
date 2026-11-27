package com.teamyg.parfait.data.repository.appversion

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.appversion.remote.AppVersionRemoteDataSource
import com.teamyg.parfait.domain.model.appversion.AppUpdateType
import com.teamyg.parfait.domain.model.appversion.AppVersionInfo
import com.teamyg.parfait.domain.model.error.AppError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppVersionRepositoryImplTest {
    private val remote: AppVersionRemoteDataSource = mockk()
    private val repository = AppVersionRepositoryImpl(remote)

    @Test
    fun checkVersion_success_passesInfoThrough() = runTest {
        // Given 원격이 FORCE 를 준다
        val info = AppVersionInfo(AppUpdateType.FORCE, "market://details?id=x")
        coEvery { remote.checkVersion("1.4.0") } returns Result.success(info)

        // When 버전을 확인한다
        val result = repository.checkVersion("1.4.0")

        // Then 그대로 돌려준다
        assertEquals(info, result.getOrThrow())
    }

    @Test
    fun checkVersion_failure_mapsToAppError() = runTest {
        // Given 원격이 네트워크 오류로 실패한다
        coEvery { remote.checkVersion(any()) } returns Result.failure(ApiException.Network(IOException()))

        // When 버전을 확인한다
        val result = repository.checkVersion("1.4.0")

        // Then AppError 로 바꿔 돌려준다
        assertIs<AppError>(result.exceptionOrNull())
    }
}
