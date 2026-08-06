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
        // Given 가운데 공백 한 칸은 허용된다
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

    @Test
    fun invoke_emptyString_returnsEmptyString() {
        // Given 빈 문자열

        // When 유효성 검사
        val result = checkNameValid("")

        // Then 빈 문자열 오류
        assertEquals(NameValidResult.Error.EmptyString, result)
    }

    @Test
    fun invoke_singleSpaceOnly_returnsSpaceAtEdge() {
        // Given 공백 한 칸만 입력
        // 검사 순서상 가장자리 공백이 빈 문자열보다 먼저 걸린다
        val result = checkNameValid(" ")

        // Then EmptyString 이 아니라 SpaceAtEdge
        assertEquals(NameValidResult.Error.SpaceAtEdge, result)
    }
}
