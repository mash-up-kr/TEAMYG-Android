package com.teamyg.parfait.domain.model.group

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InviteCodeTest {
    private val messageTemplate = $$"친구가 파르페에 초대했어요.\n체리 올리러 가볼까요? %1$s"

    private fun message(code: String) = messageTemplate.format(code)

    @Test
    fun parseFromMessageOrNull_messageEndingWithCode_readsTheLastSixChars() {
        // Given 코드로 끝나는 초대 메시지
        val text = message("E54W1A")

        // When
        val inviteCode = InviteCode.parseFromMessageOrNull(text)

        // Then 맨 뒤 6자가 코드다
        assertEquals(InviteCode("E54W1A"), inviteCode)
    }

    @Test
    fun parseFromMessageOrNull_messageWithLinkLine_readsTheCodeAtTheEnd() {
        // Given 링크 줄이 붙어 코드가 두 번 나오는 메시지
        val text = message("E54W1A") + "\n바로가기: https://parfait-app.store/link?inviteCode=E54W1A"

        // When
        val inviteCode = InviteCode.parseFromMessageOrNull(text)

        // Then 끝의 코드를 읽는다
        assertEquals(InviteCode("E54W1A"), inviteCode)
    }

    @Test
    fun parseFromMessageOrNull_codeShapedTokenBeforeTheEnd_ignoresIt() {
        // Given 앞에 코드처럼 생긴 토큰이 섞인 메시지
        val text = "ABC123 " + message("E54W1A")

        // When
        val inviteCode = InviteCode.parseFromMessageOrNull(text)

        // Then 맨 뒤 6자만 본다
        assertEquals(InviteCode("E54W1A"), inviteCode)
    }

    @Test
    fun parseFromMessageOrNull_trailingWhitespace_isTrimmedFirst() {
        assertEquals(InviteCode("E54W1A"), InviteCode.parseFromMessageOrNull("초대 E54W1A \n"))
    }

    @Test
    fun parseFromMessageOrNull_codeOnlyText_returnsTheCode() {
        assertEquals(InviteCode("E54W1A"), InviteCode.parseFromMessageOrNull("E54W1A"))
    }

    @Test
    fun parseFromMessageOrNull_tokenLongerThanCodeLength_readsTheLastSixChars() {
        // Given 6자보다 긴 영숫자로 끝나는 텍스트
        val text = "ABCDEFGH"

        // When
        val inviteCode = InviteCode.parseFromMessageOrNull(text)

        // Then iOS 와 같이 뒤 6자를 읽는다 — 엄격한 전체 일치는 parseOrNull 몫이다
        assertEquals(InviteCode("CDEFGH"), inviteCode)
    }

    @Test
    fun parseFromMessageOrNull_lastSixCharsNotAllCodeChars_returnsNull() {
        // 끝이 한글, 기호, 6자 미만인 텍스트
        assertNull(InviteCode.parseFromMessageOrNull("초대코드가 없는 문구예요"))
        assertNull(InviteCode.parseFromMessageOrNull("E54W1!"))
        assertNull(InviteCode.parseFromMessageOrNull("E54W1"))
    }

    @Test
    fun parseFromMessageOrNull_blankOrNullOrTooLong_returnsNull() {
        assertNull(InviteCode.parseFromMessageOrNull("   "))
        assertNull(InviteCode.parseFromMessageOrNull(null))
        assertNull(InviteCode.parseFromMessageOrNull("가".repeat(200) + " E54W1A"))
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
        val inviteCode = InviteCode.parseOrNull(text)

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

    @Test
    fun isCodeChar_asciiLetterOrDigit_returnsTrue() {
        // Given 초대코드에 쓰이는 글자들
        val chars = listOf('A', 'z', '0', '9')

        // When·Then 전부 코드 글자로 인정한다
        chars.forEach { char -> assertTrue(InviteCode.isCodeChar(char), "$char") }
    }

    @Test
    fun isCodeChar_koreanOrSymbolOrWhitespace_returnsFalse() {
        // Given 초대코드에 들어올 수 없는 글자들
        val chars = listOf('가', 'ㄱ', '!', '-', ' ', '\n', 'é')

        // When·Then 전부 걸러낸다 — 한글은 isLetterOrDigit 로는 통과하므로 별도 판정이 필요하다
        chars.forEach { char -> assertFalse(InviteCode.isCodeChar(char), "$char") }
    }
}
