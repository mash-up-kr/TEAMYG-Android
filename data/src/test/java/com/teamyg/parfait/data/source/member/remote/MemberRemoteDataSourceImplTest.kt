package com.teamyg.parfait.data.source.member.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.MemberService
import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
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

class MemberRemoteDataSourceImplTest {
    private val memberService: MemberService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = MemberRemoteDataSourceImpl(
        memberService = memberService,
        apiCaller = apiCaller,
    )

    private fun accountSuccess(
        memberId: Long = 42L,
        provider: String = "KAKAO",
        nickname: String = "행복한 판다",
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = MyAccountResponse(memberId = memberId, provider = provider, nickname = nickname),
    )

    private fun nicknameSuccess(nickname: String = "부지런한 수달") = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = ChangeGlobalNicknameResponse(nickname = nickname),
    )

    @Test
    fun getMyAccount_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 계정 정보를 준다
        coEvery { memberService.getUsersMe() } returns
            accountSuccess(memberId = 42L, provider = "KAKAO", nickname = "행복한 판다")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then 모든 필드가 제자리에 들어간다
        assertEquals(MemberId(42L), vo.memberId)
        assertEquals(LoginProvider.KAKAO, vo.provider)
        assertEquals(GlobalNickname("행복한 판다"), vo.nickname)
    }

    @Test
    fun getMyAccount_appleProvider_mapsToApple() = runTest {
        // Given 서버가 애플 회원을 준다
        coEvery { memberService.getUsersMe() } returns accountSuccess(provider = "APPLE")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then APPLE enum 으로 떨어진다
        assertEquals(LoginProvider.APPLE, vo.provider)
    }

    @Test
    fun getMyAccount_unknownProvider_fallsBackToUnknown() = runTest {
        // Given 클라이언트가 모르는 provider 문자열 (서버 영속 계층에는 GOOGLE 이 있다)
        coEvery { memberService.getUsersMe() } returns accountSuccess(provider = "GOOGLE")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(LoginProvider.UNKNOWN, vo.provider)
    }

    @Test
    fun getMyAccount_providerMatchIsCaseSensitive() = runTest {
        // Given 값은 맞지만 대소문자가 다른 provider
        coEvery { memberService.getUsersMe() } returns accountSuccess(provider = "kakao")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(LoginProvider.UNKNOWN, vo.provider)
    }

    @Test
    fun getMyAccount_memberNotFound_returnsBusinessException() = runTest {
        // Given envelope 의 success=false 응답
        coEvery { memberService.getUsersMe() } returns ApiResponse(
            success = false,
            code = "MEMBER_NOT_FOUND",
            message = "존재하지 않는 회원입니다",
            data = null,
        )

        // When 계정 조회
        val result = dataSource.getMyAccount()

        // Then Business 예외로 실패한다 (401 인지 404 인지는 이 계층이 판정하지 않는다)
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("MEMBER_NOT_FOUND", error.code)
    }

    @Test
    fun getMyAccount_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { memberService.getUsersMe() } throws IOException("connection reset")

        // When 계정 조회
        val result = dataSource.getMyAccount()

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun changeGlobalNickname_serviceReturnsSuccess_returnsSavedNickname() = runTest {
        // Given 서비스가 저장된 닉네임을 준다
        coEvery { memberService.patchUsersMeNickname(any()) } returns nicknameSuccess("부지런한 수달")

        // When 닉네임 변경
        val result = dataSource.changeGlobalNickname(GlobalNickname("부지런한 수달"))

        // Then 저장된 값이 GlobalNickname 으로 돌아온다
        assertEquals(GlobalNickname("부지런한 수달"), result.getOrThrow())
    }

    @Test
    fun changeGlobalNickname_unwrapsValueClassForRequestBody() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<ChangeGlobalNicknameRequest>()
        coEvery { memberService.patchUsersMeNickname(capture(request)) } returns nicknameSuccess()

        // When value class 로 감싼 닉네임으로 변경 호출
        dataSource.changeGlobalNickname(GlobalNickname("부지런한 수달"))

        // Then 바디에는 raw String 이 들어간다 (Retrofit 경계에서 벗긴다)
        assertEquals("부지런한 수달", request.captured.nickname)
        coVerify(exactly = 1) { memberService.patchUsersMeNickname(any()) }
    }

    @Test
    fun changeGlobalNickname_invalidNickname_returnsBusinessException() = runTest {
        // Given 형식 위반 응답
        coEvery { memberService.patchUsersMeNickname(any()) } returns ApiResponse(
            success = false,
            code = "INVALID_NICKNAME",
            message = "닉네임 형식이 올바르지 않습니다",
            data = null,
        )

        // When 닉네임 변경
        val result = dataSource.changeGlobalNickname(GlobalNickname("연속  공백"))

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_NICKNAME", error.code)
    }

    @Test
    fun changeGlobalNickname_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { memberService.patchUsersMeNickname(any()) } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 닉네임 변경
        val result = dataSource.changeGlobalNickname(GlobalNickname("부지런한 수달"))

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }

    @Test
    fun withdraw_serviceReturnsNoContent_returnsSuccess() = runTest {
        // Given 서버가 204 를 준다(본문 없음 — envelope 자체가 오지 않는다)
        coJustRun { memberService.deleteUsersMe() }

        // When 탈퇴한다
        val result = dataSource.withdraw()

        // Then 파싱할 본문이 없어도 성공이다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun withdraw_serviceThrowsHttpException_returnsFailure() = runTest {
        // Given 서버가 401 을 준다
        coEvery { memberService.deleteUsersMe() } throws HttpException(
            Response.error<Unit>(401, "".toResponseBody(null)),
        )

        // When 탈퇴한다
        val result = dataSource.withdraw()

        // Then ApiException.Http 로 번역되고 상태 코드가 보존된다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Http>(result.exceptionOrNull())
        assertEquals(401, error.statusCode)
    }
}
