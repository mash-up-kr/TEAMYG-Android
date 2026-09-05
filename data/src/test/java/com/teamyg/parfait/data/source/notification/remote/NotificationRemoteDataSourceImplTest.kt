package com.teamyg.parfait.data.source.notification.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NotificationService
import com.teamyg.parfait.data.service.model.request.notification.RegisterDeviceTokenRequest
import com.teamyg.parfait.domain.model.notification.DeviceToken
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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

class NotificationRemoteDataSourceImplTest {
    private val notificationService: NotificationService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = NotificationRemoteDataSourceImpl(
        notificationService = notificationService,
        apiCaller = apiCaller,
    )

    @Test
    fun registerDeviceToken_serviceReturnsNoContent_returnsSuccess() = runTest {
        // Given 서버가 204 를 준다(본문 없음 — envelope 자체가 오지 않는다)
        coJustRun { notificationService.postNotificationsDevices(any()) }

        // When 기기 토큰을 등록한다
        val result = dataSource.registerDeviceToken(DeviceToken("fcm-token"))

        // Then 파싱할 본문이 없어도 성공이다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun registerDeviceToken_sendsAndroidPlatformAndUnwrapsValueClass() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<RegisterDeviceTokenRequest>()
        coJustRun { notificationService.postNotificationsDevices(capture(request)) }

        // When value class 로 감싼 토큰으로 등록 호출
        dataSource.registerDeviceToken(DeviceToken("fcm-token"))

        // Then 바디에는 raw String 이 들어가고 플랫폼은 호출자가 아니라 이 계층이 채운다
        assertEquals("fcm-token", request.captured.token)
        assertEquals("ANDROID", request.captured.platform)
        coVerify(exactly = 1) { notificationService.postNotificationsDevices(any()) }
    }

    @Test
    fun registerDeviceToken_serviceThrowsHttpException_returnsFailure() = runTest {
        // Given 서버가 401 을 준다
        coEvery { notificationService.postNotificationsDevices(any()) } throws HttpException(
            Response.error<Unit>(401, "".toResponseBody(null)),
        )

        // When 기기 토큰을 등록한다
        val result = dataSource.registerDeviceToken(DeviceToken("fcm-token"))

        // Then ApiException.Http 로 번역되고 상태 코드가 보존된다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Http>(result.exceptionOrNull())
        assertEquals(401, error.statusCode)
    }

    @Test
    fun registerDeviceToken_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { notificationService.postNotificationsDevices(any()) } throws IOException("connection reset")

        // When 기기 토큰을 등록한다
        val result = dataSource.registerDeviceToken(DeviceToken("fcm-token"))

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
}
