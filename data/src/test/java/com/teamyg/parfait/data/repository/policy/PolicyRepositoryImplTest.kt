package com.teamyg.parfait.data.repository.policy

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PolicyRepositoryImplTest {
    private val policyRemoteDataSource: PolicyRemoteDataSource = mockk()
    private val repository = PolicyRepositoryImpl(policyRemoteDataSource)

    private val policy = PolicyVO(
        termsId = TermsId(1L),
        type = PolicyType.TERMS_OF_SERVICE,
        title = "서비스 이용약관",
        url = "https://example.com/terms",
        required = true,
    )

    @Test
    fun getPolicies_dataSourceReturnsSuccess_returnsSameValue() = runTest {
        // Given 원격 데이터소스가 약관을 준다
        coEvery { policyRemoteDataSource.getPolicies() } returns Result.success(listOf(policy))

        // When 약관 조회
        val result = repository.getPolicies()

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(listOf(policy), result.getOrThrow())
    }

    @Test
    fun getPolicies_onceCalled_delegatesToDataSourceExactlyOnce() = runTest {
        // Given 성공 응답
        coEvery { policyRemoteDataSource.getPolicies() } returns Result.success(listOf(policy))

        // When 약관 조회
        repository.getPolicies()

        // Then 원격 데이터소스를 한 번만 호출한다
        coVerify(exactly = 1) { policyRemoteDataSource.getPolicies() }
    }

    @Test
    fun getPolicies_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { policyRemoteDataSource.getPolicies() } returns
            Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 약관 조회
        val result = repository.getPolicies()

        // Then Network 갈래로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun getPolicies_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 서버가 에러 envelope 로 응답
        coEvery { policyRemoteDataSource.getPolicies() } returns Result.failure(
            ApiException.Business(
                code = "POLICY_NOT_FOUND",
                serverMessage = "약관을 찾을 수 없습니다",
                statusCode = 404,
                errorDetail = null,
            ),
        )

        // When 약관 조회
        val result = repository.getPolicies()

        // Then 도메인 에러로 바뀌어 나온다(호출부가 :data 를 보지 않아도 된다)
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("POLICY_NOT_FOUND", error.code)
        assertEquals(404, error.statusCode)
    }
}
