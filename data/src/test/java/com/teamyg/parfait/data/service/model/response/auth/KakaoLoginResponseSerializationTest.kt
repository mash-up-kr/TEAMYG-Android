package com.teamyg.parfait.data.service.model.response.auth

import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 서버가 내려주는 판별자 키는 `isNewUser` 다(`newUser` 아님). Jackson 코틀린 모듈이
 * 붙은 서버는 주 생성자 파라미터명으로 직렬화해 `is` 접두사가 살아남는다.
 */
class KakaoLoginResponseSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_newUserResponse_readsIsNewUserKeyAndRegistrationToken() {
        // Given 서버 신규 회원 응답 본문
        val body = """
            {
              "success": true,
              "code": "OK",
              "message": "성공",
              "data": {
                "isNewUser": true,
                "accessToken": null,
                "refreshToken": null,
                "expiresIn": null,
                "registrationToken": "reg-token-1"
              }
            }
        """.trimIndent()

        // When 디코딩
        val response = json.decodeFromString<ApiResponse<KakaoLoginResponse>>(body).data

        // Then 판별자가 true 이고 가입 토큰이 실린다
        assertTrue(response!!.isNewUser)
        assertEquals("reg-token-1", response.registrationToken)
        assertNull(response.accessToken)
    }

    @Test
    fun decode_existingMemberResponse_readsSessionFields() {
        // Given 서버 기존 회원 응답 본문
        val body = """
            {
              "success": true,
              "code": "OK",
              "message": "성공",
              "data": {
                "isNewUser": false,
                "accessToken": "access-1",
                "refreshToken": "refresh-1",
                "expiresIn": 3600,
                "registrationToken": null
              }
            }
        """.trimIndent()

        // When 디코딩
        val response = json.decodeFromString<ApiResponse<KakaoLoginResponse>>(body).data

        // Then 세션 3종이 실리고 가입 토큰은 없다
        assertEquals(false, response!!.isNewUser)
        assertEquals("access-1", response.accessToken)
        assertEquals("refresh-1", response.refreshToken)
        assertEquals(3600L, response.expiresIn)
        assertNull(response.registrationToken)
    }
}
