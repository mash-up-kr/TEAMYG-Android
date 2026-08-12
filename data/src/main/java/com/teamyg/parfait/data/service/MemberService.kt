package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * 두 엔드포인트 모두 서버 화이트리스트 밖이라 access token 이 필요하다. @NoAuth 를 붙이지 않는다.
 * 대상 회원은 요청이 아니라 토큰에서 정해지므로 경로 변수도 바디 필드도 없다.
 */
interface MemberService {
    @GET("api/v1/users/me")
    suspend fun getUsersMe(): ApiResponse<MyAccountResponse>

    @PATCH("api/v1/users/me/nickname")
    suspend fun patchUsersMeNickname(
        @Body request: ChangeGlobalNicknameRequest,
    ): ApiResponse<ChangeGlobalNicknameResponse>
}
