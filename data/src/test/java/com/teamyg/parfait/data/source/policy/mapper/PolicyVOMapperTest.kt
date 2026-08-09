package com.teamyg.parfait.data.source.policy.mapper

import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 매퍼 테스트는 결정이 있는 곳만 다룬다. 필드를 그대로 옮기기만 하는 매퍼는 컴파일러가
 * 막아주니 테스트하지 않는다.
 *
 * 이 매퍼가 내리는 결정은 type 문자열을 PolicyType 으로 옮기는 규칙이다. 모르는 값이 오면
 * 예외를 던지지 않고 UNKNOWN 으로 떨어뜨린다. 누가 enumValueOf 로 바꾸면 서버가 타입 하나
 * 추가하는 순간 약관 화면에서 크래시가 난다. 필드 배선도 컴파일러에 다 맡길 수 없다.
 * title 과 url 이 둘 다 String 이라 뒤바꿔도 통과한다.
 */
class PolicyVOMapperTest {
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
    fun toPolicyVO_mapsEveryField() {
        // Given 서버가 준 약관 항목
        val response = itemResponse(termsId = 7L, required = false)

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then 모든 필드가 제자리에 들어간다 (title 과 url 은 같은 String 이라 뒤바뀔 수 있다)
        assertEquals(TermsId(7L), vo.termsId)
        assertEquals(PolicyType.TERMS_OF_SERVICE, vo.type)
        assertEquals("이용약관", vo.title)
        assertEquals("https://example.com/terms", vo.url)
        assertEquals(false, vo.required)
    }

    @Test
    fun toPolicyVO_unknownType_fallsBackToUnknown() {
        // Given 클라이언트가 모르는 타입 문자열
        val response = itemResponse(type = "MARKETING_CONSENT")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(PolicyType.UNKNOWN, vo.type)
    }

    @Test
    fun toPolicyVO_typeMatchIsCaseSensitive() {
        // Given 값은 맞지만 대소문자가 다른 타입
        val response = itemResponse(type = "terms_of_service")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(PolicyType.UNKNOWN, vo.type)
    }

    @Test
    fun toPolicyVOList_mapsEachItemInOrder() {
        // Given 타입이 서로 다른 두 건
        val response = PolicyResponse(
            policies = listOf(
                itemResponse(termsId = 1L, type = "TERMS_OF_SERVICE"),
                itemResponse(termsId = 2L, type = "PRIVACY_POLICY"),
            ),
        )

        // When 리스트로 변환
        val vos = response.toPolicyVOList()

        // Then 각 항목이 자기 타입으로 변환되고 순서도 그대로다
        assertEquals(2, vos.size)
        assertEquals(TermsId(1L) to PolicyType.TERMS_OF_SERVICE, vos[0].termsId to vos[0].type)
        assertEquals(TermsId(2L) to PolicyType.PRIVACY_POLICY, vos[1].termsId to vos[1].type)
    }
}
