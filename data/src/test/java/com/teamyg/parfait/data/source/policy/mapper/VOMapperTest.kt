package com.teamyg.parfait.data.source.policy.mapper

import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VOMapperTest {
    private fun itemResponse(
        termsId: Long = 1L,
        type: String = "TERMS_OF_SERVICE",
        title: String = "이용약관",
        url: String = "https://example.com/terms",
        required: Boolean = true,
    ) = PolicyItemResponse(
        termsId = termsId,
        type = type,
        title = title,
        url = url,
        required = required,
    )

    @Test
    fun toPolicyVO_termsOfServiceType_mapsAllFields() {
        // Given 서버가 준 약관 항목
        val response = itemResponse(termsId = 7L, required = false)

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then 모든 필드가 그대로 옮겨진다
        assertEquals(TermsId(7L), vo.termsId)
        assertEquals(PolicyType.TERMS_OF_SERVICE, vo.type)
        assertEquals("이용약관", vo.title)
        assertEquals("https://example.com/terms", vo.url)
        assertEquals(false, vo.required)
    }

    @Test
    fun toPolicyVO_privacyPolicyType_mapsToPrivacyPolicy() {
        // Given 개인정보 처리방침 타입
        val response = itemResponse(type = "PRIVACY_POLICY")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then 대응 enum 으로 매핑된다
        assertEquals(PolicyType.PRIVACY_POLICY, vo.type)
    }

    @Test
    fun toPolicyVO_unknownType_mapsToUnknown() {
        // Given 클라이언트가 모르는 타입 문자열
        val response = itemResponse(type = "MARKETING_CONSENT")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then UNKNOWN 으로 떨어진다 — 예외를 던지지 않는다
        assertEquals(PolicyType.UNKNOWN, vo.type)
    }

    @Test
    fun toPolicyVO_lowercaseType_mapsToUnknown() {
        // Given 대소문자가 다른 타입 (매핑은 정확히 일치할 때만 성립한다)
        val response = itemResponse(type = "terms_of_service")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then UNKNOWN
        assertEquals(PolicyType.UNKNOWN, vo.type)
    }

    @Test
    fun toPolicyVOList_multipleItems_preservesOrder() {
        // Given 두 건이 담긴 응답
        val response = PolicyResponse(
            policies = listOf(
                itemResponse(termsId = 1L, type = "TERMS_OF_SERVICE"),
                itemResponse(termsId = 2L, type = "PRIVACY_POLICY"),
            ),
        )

        // When 리스트로 변환
        val vos = response.toPolicyVOList()

        // Then 순서와 개수가 유지된다
        assertEquals(2, vos.size)
        assertEquals(TermsId(1L), vos[0].termsId)
        assertEquals(TermsId(2L), vos[1].termsId)
    }

    @Test
    fun toPolicyVOList_emptyPolicies_returnsEmptyList() {
        // Given 빈 응답
        val response = PolicyResponse(policies = emptyList())

        // When 리스트로 변환
        val vos = response.toPolicyVOList()

        // Then 빈 리스트
        assertTrue(vos.isEmpty())
    }
}
