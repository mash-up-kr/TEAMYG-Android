package com.teamyg.parfait.data.source.policy.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.PolicyService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PolicyRemoteDataSourceImplTest {
    private val policyService: PolicyService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = PolicyRemoteDataSourceImpl(
        policyService = policyService,
        apiCaller = apiCaller,
    )

    private fun successResponse(vararg types: String) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = PolicyResponse(
            policies = types.mapIndexed { index, type ->
                PolicyItemResponse(
                    termsId = index.toLong(),
                    type = type,
                    title = "약관 $index",
                    url = "https://example.com/$index",
                    required = true,
                )
            },
        ),
    )

    @Test
    fun getPolicies_serviceReturnsSuccess_returnsMappedVoList() = runTest {
        // Given 서비스가 약관 두 건을 성공 응답으로 준다
        coEvery { policyService.getPolicies() } returns
            successResponse("TERMS_OF_SERVICE", "PRIVACY_POLICY")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then VO 리스트로 매핑된 성공 결과
        val policies = result.getOrThrow()
        assertEquals(2, policies.size)
        assertEquals(PolicyType.TERMS_OF_SERVICE, policies[0].type)
        assertEquals(PolicyType.PRIVACY_POLICY, policies[1].type)
    }

    @Test
    fun getPolicies_onceCalled_delegatesToServiceExactlyOnce() = runTest {
        // Given 성공 응답
        coEvery { policyService.getPolicies() } returns successResponse("TERMS_OF_SERVICE")

        // When 정책 조회
        dataSource.getPolicies()

        // Then 서비스를 정확히 한 번만 호출한다 (중복 호출 회귀 방어)
        coVerify(exactly = 1) { policyService.getPolicies() }
    }

    @Test
    fun getPolicies_businessFailure_returnsBusinessException() = runTest {
        // Given 서버가 success=false 로 응답
        coEvery { policyService.getPolicies() } returns ApiResponse(
            success = false,
            code = "POLICY_NOT_FOUND",
            message = "약관을 찾을 수 없습니다",
            data = null,
        )

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("POLICY_NOT_FOUND", error.code)
        assertEquals("약관을 찾을 수 없습니다", error.serverMessage)
    }

    @Test
    fun getPolicies_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { policyService.getPolicies() } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }

    @Test
    fun getPolicies_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { policyService.getPolicies() } throws IOException("connection reset")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun getPolicies_unexpectedException_returnsUnknownException() = runTest {
        // Given 예상 못 한 예외
        coEvery { policyService.getPolicies() } throws IllegalStateException("boom")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then Unknown 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Unknown>(result.exceptionOrNull())
    }

    @Test
    fun getPolicies_unknownType_fallsBackToUnknown() = runTest {
        // Given 클라이언트가 모르는 타입 문자열
        coEvery { policyService.getPolicies() } returns successResponse("MARKETING_CONSENT")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(PolicyType.UNKNOWN, result.getOrThrow().single().type)
    }

    @Test
    fun getPolicies_typeMatchIsCaseSensitive() = runTest {
        // Given 값은 맞지만 대소문자가 다른 타입
        coEvery { policyService.getPolicies() } returns successResponse("terms_of_service")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(PolicyType.UNKNOWN, result.getOrThrow().single().type)
    }

    @Test
    fun getPolicies_mapsEveryFieldOfEachItem() = runTest {
        // Given title 과 url 이 서로 다른 값인 약관 한 건
        coEvery { policyService.getPolicies() } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = PolicyResponse(
                policies = listOf(
                    PolicyItemResponse(
                        termsId = 7L,
                        type = "PRIVACY_POLICY",
                        title = "개인정보 처리방침",
                        url = "https://example.com/privacy",
                        required = false,
                    ),
                ),
            ),
        )

        // When 정책 조회
        val vo = dataSource.getPolicies().getOrThrow().single()

        // Then 모든 필드가 제자리에 들어간다 (title 과 url 은 둘 다 String 이라 뒤바뀌어도 컴파일된다)
        assertEquals(TermsId(7L), vo.termsId)
        assertEquals(PolicyType.PRIVACY_POLICY, vo.type)
        assertEquals("개인정보 처리방침", vo.title)
        assertEquals("https://example.com/privacy", vo.url)
        assertEquals(false, vo.required)
    }
}
