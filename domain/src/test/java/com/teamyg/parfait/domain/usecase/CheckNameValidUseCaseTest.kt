package com.teamyg.parfait.domain.usecase

import com.teamyg.parfait.domain.model.NameValidResult
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckNameValidUseCaseTest {
    private val checkNameValid = CheckNameValidUseCase()

    @Test
    fun invoke_plainKoreanName_returnsSuccess() {
        // Given 공백 없는 한글 이름
        val name = "파르페"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 통과
        assertEquals(NameValidResult.Success, result)
    }

    @Test
    fun invoke_nameWithSingleInnerSpace_returnsSuccess() {
        assertEquals(NameValidResult.Success, checkNameValid("우리 그룹"))
    }

    @Test
    fun invoke_alphanumericName_returnsSuccess() {
        assertEquals(NameValidResult.Success, checkNameValid("Team1"))
    }

    @Test
    fun invoke_leadingSpace_returnsSpaceAtEdge() {
        // Given 앞에 공백
        val name = " 파르페"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 가장자리 공백 오류
        assertEquals(NameValidResult.Error.SpaceAtEdge, result)
    }

    @Test
    fun invoke_trailingSpace_returnsSpaceAtEdge() {
        assertEquals(NameValidResult.Error.SpaceAtEdge, checkNameValid("파르페 "))
    }

    @Test
    fun invoke_consecutiveSpaces_returnsDuplicatedSpace() {
        // Given 연속 공백
        val name = "우리  그룹"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 연속 공백 오류
        assertEquals(NameValidResult.Error.DuplicatedSpace, result)
    }

    @Test
    fun invoke_emojiIncluded_returnsInvalidCharacter() {
        // Given 허용 문자 집합 밖의 문자
        val name = "파르페🍨"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 문자 오류
        assertEquals(NameValidResult.Error.InvalidCharacter, result)
    }

    @Test
    fun invoke_symbolIncluded_returnsInvalidCharacter() {
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("파르페!"))
    }

    // 아래 넷은 서버 정규식(`^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$`)보다 넓게
    // 통과시켜 서버에서만 400 으로 튕기던 입력들이다. 규칙이 다시 벌어지면 여기서 깨진다.

    @Test
    fun invoke_standaloneJamo_returnsSuccess() {
        // Given 자모만 있는 이름 — 2026-08-15 서버 변경으로 허용 대상이다
        val name = "ㅋㅋㅋ"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 통과한다 — 모음 단독·완성형 혼용도 같다
        assertEquals(NameValidResult.Success, result)
        assertEquals(NameValidResult.Success, checkNameValid("ㅠㅠ"))
        assertEquals(NameValidResult.Success, checkNameValid("파르페ㅎㅎ"))
    }

    @Test
    fun invoke_nonKoreanLetter_returnsInvalidCharacter() {
        // Given 한글·영문이 아닌 문자(isLetter() 로는 전부 통과하던 것들)
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("さくら"))
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("中文"))
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("Привет"))
    }

    @Test
    fun invoke_accentedLatinLetter_returnsInvalidCharacter() {
        // Given 라틴 확장 문자 — 'A'..'Z'·'a'..'z' 범위 밖이다
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("Café"))
    }

    @Test
    fun invoke_nonAsciiDigit_returnsInvalidCharacter() {
        // Given 아랍-인도 숫자 — isDigit() 은 통과시키지만 서버는 '0'..'9' 만 받는다
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("٣٤"))
    }

    @Test
    fun invoke_nonBreakingSpace_returnsInvalidCharacter() {
        // Given 붙여넣기로 딸려 오기 쉬운 non-breaking space — 서버 정규식은 U+0020 만 받는다
        val name = "우리\u00A0그룹"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 문자 오류
        assertEquals(NameValidResult.Error.InvalidCharacter, result)
    }

    @Test
    fun invoke_emptyString_returnsEmptyString() {
        // Given 빈 문자열
        val name = ""

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 빈 문자열 오류
        assertEquals(NameValidResult.Error.EmptyString, result)
    }

    @Test
    fun invoke_singleSpaceOnly_returnsSpaceAtEdge() {
        // Given 공백 한 칸만 입력 — " ".isNotEmpty() 는 true 이므로 EmptyString 에는 걸리지 않는다
        val name = " "

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 빈 문자열이 아니라 가장자리 공백으로 분류된다
        assertEquals(NameValidResult.Error.SpaceAtEdge, result)
    }

    @Test
    fun invoke_twoSpacesOnly_returnsSpaceAtEdgeNotDuplicatedSpace() {
        // Given 공백 두 칸 — CheckSpaceStartOrEnd(가장자리 공백)와 CheckDuplicatedSpace(연속 공백)
        // 둘 다 실패하는 입력이라, 두 규칙의 선언 순서를 고정하는 회귀 방어다
        val name = "  "

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then CheckSpaceStartOrEnd 가 먼저 선언되어 있어 DuplicatedSpace 가 아니라 SpaceAtEdge 로 걸린다
        assertEquals(NameValidResult.Error.SpaceAtEdge, result)
    }
}
