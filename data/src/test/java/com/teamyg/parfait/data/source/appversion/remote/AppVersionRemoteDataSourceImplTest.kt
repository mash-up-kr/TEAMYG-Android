package com.teamyg.parfait.data.source.appversion.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.AppVersionService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.appversion.AppVersionCheckResponse
import com.teamyg.parfait.domain.model.appversion.AppUpdateType
import com.teamyg.parfait.domain.model.appversion.AppVersionInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppVersionRemoteDataSourceImplTest {
    private val appVersionService: AppVersionService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = AppVersionRemoteDataSourceImpl(
        appVersionService = appVersionService,
        apiCaller = apiCaller,
    )

    private fun response(
        updateType: String,
        storeUrl: String? = null,
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = AppVersionCheckResponse(
            updateType = updateType,
            minimumVersion = "1.0.0",
            latestVersion = "1.5.0",
            storeUrl = storeUrl,
        ),
    )

    @Test
    fun checkVersion_force_mapsToForceWithStoreUrl() = runTest {
        // Given 서버가 FORCE 와 스토어 주소를 준다
        coEvery { appVersionService.checkVersion(any(), any()) } returns
            response("FORCE", "market://details?id=x")

        // When 버전을 확인한다
        val info = dataSource.checkVersion("1.4.0").getOrThrow()

        // Then FORCE 와 스토어 주소로 매핑한다
        assertEquals(AppVersionInfo(AppUpdateType.FORCE, "market://details?id=x"), info)
    }

    @Test
    fun checkVersion_optionalAndNone_mapToEnum() = runTest {
        // Given 서버가 OPTIONAL, NONE 을 차례로 준다
        coEvery { appVersionService.checkVersion(any(), any()) } returnsMany
            listOf(response("OPTIONAL"), response("NONE"))

        // When 버전을 두 번 확인한다
        val optional = dataSource.checkVersion("1.4.0").getOrThrow()
        val none = dataSource.checkVersion("1.4.0").getOrThrow()

        // Then 각각 enum 으로 매핑한다
        assertEquals(AppUpdateType.OPTIONAL, optional.updateType)
        assertEquals(AppUpdateType.NONE, none.updateType)
    }

    @Test
    fun checkVersion_unknownUpdateType_mapsToUnknown() = runTest {
        // Given 서버가 모르는 값 SOFT 를 준다
        coEvery { appVersionService.checkVersion(any(), any()) } returns response("SOFT")

        // When 버전을 확인한다
        val info = dataSource.checkVersion("1.4.0").getOrThrow()

        // Then UNKNOWN 으로 매핑한다
        assertEquals(AppUpdateType.UNKNOWN, info.updateType)
    }

    @Test
    fun checkVersion_always_requestsAndroidWithVersion() = runTest {
        // Given NONE 응답
        coEvery { appVersionService.checkVersion(any(), any()) } returns response("NONE")

        // When 버전 1.4.0 을 확인한다
        dataSource.checkVersion("1.4.0")

        // Then 플랫폼 ANDROID 와 그 버전으로 한 번만 호출한다
        coVerify(exactly = 1) { appVersionService.checkVersion("ANDROID", "1.4.0") }
    }

    @Test
    fun checkVersion_businessFailure_returnsFailure() = runTest {
        // Given 서버가 success=false 로 응답한다
        coEvery { appVersionService.checkVersion(any(), any()) } returns ApiResponse(
            success = false,
            code = "INVALID_VERSION_FORMAT",
            message = "잘못된 버전 형식",
            data = null,
        )

        // When 버전을 확인한다
        val result = dataSource.checkVersion("abc")

        // Then Business 예외로 실패한다
        assertIs<ApiException.Business>(result.exceptionOrNull())
    }

    @Test
    fun checkVersion_http404_returnsFailure() = runTest {
        // Given 서버가 404 를 준다
        coEvery { appVersionService.checkVersion(any(), any()) } throws
            HttpException(Response.error<Unit>(404, "".toResponseBody(null)))

        // When 버전을 확인한다
        val result = dataSource.checkVersion("1.4.0")

        // Then 실패로 돌려준다
        assertTrue(result.isFailure)
    }

    @Test
    fun checkVersion_http500_returnsFailure() = runTest {
        // Given 서버가 500 을 준다
        coEvery { appVersionService.checkVersion(any(), any()) } throws
            HttpException(Response.error<Unit>(500, "".toResponseBody(null)))

        // When 버전을 확인한다
        val result = dataSource.checkVersion("1.4.0")

        // Then 실패로 돌려준다
        assertTrue(result.isFailure)
    }

    @Test
    fun checkVersion_ioException_returnsFailure() = runTest {
        // Given 네트워크가 끊겼다
        coEvery { appVersionService.checkVersion(any(), any()) } throws IOException("reset")

        // When 버전을 확인한다
        val result = dataSource.checkVersion("1.4.0")

        // Then Network 예외로 감싼다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
}
