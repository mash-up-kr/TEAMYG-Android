package com.teamyg.parfait.domain.model.group

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InviteCodeTest {
    private val messageTemplate = "친구가 파르페에 초대했어요.\n체리 올리러 가볼까요? %1\$s"

    private fun message(code: String) = messageTemplate.format(code)

    @Test
    fun parseOrNull_withTemplate_extractsCodeAtPlaceholderPosition() {
        // Given 템플릿으로 만들어진 초대 메시지
        val text = message("E54W1A")

        // When 템플릿을 함께 넘겨 추출
        val inviteCode = InviteCode.parseOrNull(text, messageTemplate)

        // Then 자리 표시자 위치의 값이 추출된다
        assertEquals(InviteCode("E54W1A"), inviteCode)
    }

    @Test
    fun parseOrNull_withTemplate_ignoresCodeShapedTokenInSurroundingText() {
        // Given 앞부분에 코드처럼 생긴 토큰이 섞인 초대 메시지
        val text = "ABC123 " + message("E54W1A")

        // When 템플릿을 함께 넘겨 추출
        val inviteCode = InviteCode.parseOrNull(text, messageTemplate)

        // Then 템플릿의 코드 자리 값만 추출된다
        assertEquals(InviteCode("E54W1A"), inviteCode)
    }

    @Test
    fun parseOrNull_withTemplate_codeOnlyText_fallsBackToPatternMatching() {
        // Given 코드만 복사한 텍스트
        val text = "E54W1A"

        // When 템플릿을 함께 넘겨 추출
        val inviteCode = InviteCode.parseOrNull(text, messageTemplate)

        // Then 템플릿과 형태가 달라도 코드를 찾아낸다
        assertEquals(InviteCode("E54W1A"), inviteCode)
    }

    @Test
    fun parseOrNull_withTemplateWithoutPlaceholder_fallsBackToWholeTextMatching() {
        // Given 자리 표시자가 없는 템플릿
        val brokenTemplate = "친구가 파르페에 초대했어요."

        // When 그 템플릿으로 메시지와 코드를 각각 추출
        val fromMessage = InviteCode.parseOrNull(message("E54W1A"), brokenTemplate)
        val fromCodeOnly = InviteCode.parseOrNull("E54W1A", brokenTemplate)

        // Then 전체가 코드인 경우만 인정한다
        assertNull(fromMessage)
        assertEquals(InviteCode("E54W1A"), fromCodeOnly)
    }

    @Test
    fun parseOrNull_withTemplate_koreanOnlyText_returnsNull() {
        assertNull(InviteCode.parseOrNull("초대코드가 없는 문구예요", messageTemplate))
    }

    @Test
    fun parseOrNull_codeOnly_extractsCode() {
        assertEquals(InviteCode("WDIDCJ"), InviteCode.parseOrNull("WDIDCJ"))
    }

    @Test
    fun parseOrNull_codeWithSurroundingWhitespace_extractsCode() {
        assertEquals(InviteCode("WDIDCJ"), InviteCode.parseOrNull("  WDIDCJ\n"))
    }

    @Test
    fun parseOrNull_lowerCaseCode_extractsCodeAsIs() {
        assertEquals(InviteCode("e54w1a"), InviteCode.parseOrNull("e54w1a"))
    }

    @Test
    fun parseOrNull_codeShapedTokenInsideSentence_returnsNull() {
        // Given 초대 메시지가 아닌 문장에 6자 토큰이 섞여 있는 텍스트
        val text = "ABC123 로 들어와 아니다 XYZ789"

        // When 초대코드 추출
        val inviteCode = InviteCode.parseOrNull(text)

        // Then 문장에서 토큰을 주워오지 않는다
        assertNull(inviteCode)
    }

    @Test
    fun parseOrNull_identifierContainingSixLetterToken_returnsNull() {
        // Given `_` 로 끊긴 6자 토큰이 들어 있는 식별자 (group_invite_message 의 invite)
        val text = "core/ui 의 group_invite_message 를 공유한다"

        // When 초대코드 추출
        val inviteCode = InviteCode.parseOrNull(text, messageTemplate)

        // Then 초대와 무관한 텍스트에서는 코드를 만들지 않는다
        assertNull(inviteCode)
    }

    @Test
    fun parseOrNull_tokenLongerThanCodeLength_returnsNull() {
        // Given 6자보다 긴 영숫자 토큰만 있는 텍스트
        val text = "ABCDEFGH"

        // When 초대코드 추출
        val inviteCode = InviteCode.parseOrNull(text)

        // Then 잘라내서 오탐하지 않는다
        assertNull(inviteCode)
    }

    @Test
    fun parseOrNull_urlContainingLongToken_returnsNull() {
        assertNull(InviteCode.parseOrNull("https://parfait.app/invitation"))
    }

    @Test
    fun parseOrNull_koreanOnlyText_returnsNull() {
        assertNull(InviteCode.parseOrNull("초대코드가 없는 문구예요"))
    }

    @Test
    fun parseOrNull_blankText_returnsNull() {
        assertNull(InviteCode.parseOrNull("   "))
    }

    @Test
    fun parseOrNull_nullText_returnsNull() {
        assertNull(InviteCode.parseOrNull(null))
    }

    @Test
    fun parseOrNull_tooLongText_returnsNull() {
        // Given 초대 메시지로 보기 어려운 긴 텍스트
        val text = "가".repeat(200) + " E54W1A"

        // When 초대코드 추출
        val inviteCode = InviteCode.parseOrNull(text)

        // Then 임의의 문서에서 코드를 주워오지 않는다
        assertNull(inviteCode)
    }
}
