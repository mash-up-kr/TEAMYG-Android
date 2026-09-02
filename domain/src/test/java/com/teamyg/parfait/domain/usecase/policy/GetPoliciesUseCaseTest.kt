package com.teamyg.parfait.domain.usecase.policy

import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetPoliciesUseCaseTest {
    private class FakePolicyRepository(
        private val result: Result<List<PolicyVO>>,
    ) : PolicyRepository {
        var callCount = 0
            private set

        override suspend fun getPolicies(): Result<List<PolicyVO>> {
            callCount++
            return result
        }
    }

    private fun policy(
        id: Long,
        required: Boolean,
    ) = PolicyVO(
        termsId = TermsId(id),
        type = PolicyType.TERMS_OF_SERVICE,
        title = "약관 $id",
        url = "https://example.com/$id",
        required = required,
    )

    @Test
    fun invoke_repositoryReturnsSuccess_returnsPolicies() = runTest {
        // Given 약관 두 건을 주는 저장소
        val policies = listOf(policy(id = 1L, required = true), policy(id = 2L, required = true))
        val useCase = GetPoliciesUseCase(FakePolicyRepository(Result.success(policies)))

        // When 약관 조회
        val result = useCase()

        // Then 저장소 결과가 그대로 전달된다
        assertEquals(policies, result.getOrThrow())
    }

    @Test
    fun invoke_optionalPolicyComesFirst_keepsServerOrder() = runTest {
        // Given 선택 약관이 앞에 오는 응답
        val optional = policy(id = 1L, required = false)
        val required = policy(id = 2L, required = true)
        val useCase = GetPoliciesUseCase(FakePolicyRepository(Result.success(listOf(optional, required))))

        // When 약관 조회
        val result = useCase().getOrThrow()

        // Then 순서를 바꾸지 않고 서버가 내려준 그대로 전달한다
        assertEquals(listOf(optional, required), result)
    }

    @Test
    fun invoke_emptyPolicies_returnsEmptyList() = runTest {
        // Given 약관이 없는 응답
        val useCase = GetPoliciesUseCase(FakePolicyRepository(Result.success(emptyList())))

        // When 약관 조회
        val result = useCase()

        // Then 빈 목록을 성공으로 돌려준다
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun invoke_repositoryFails_propagatesFailure() = runTest {
        // Given 저장소가 실패를 준다
        val useCase = GetPoliciesUseCase(FakePolicyRepository(Result.failure(IOException("network"))))

        // When 약관 조회
        val result = useCase()

        // Then 실패가 그대로 전달된다
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
    }

    @Test
    fun invoke_calledOnce_hitsRepositoryOnce() = runTest {
        // Given 성공 응답
        val repository = FakePolicyRepository(Result.success(emptyList()))
        val useCase = GetPoliciesUseCase(repository)

        // When 약관 조회
        useCase()

        // Then 저장소를 한 번만 호출한다
        assertEquals(1, repository.callCount)
    }
}
