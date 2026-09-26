package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH

interface MemberService {
    @GET("api/v1/users/me")
    suspend fun getUsersMe(): ApiResponse<MyAccountResponse>

    @PATCH("api/v1/users/me/nickname")
    suspend fun patchUsersMeNickname(
        @Body request: ChangeGlobalNicknameRequest,
    ): ApiResponse<ChangeGlobalNicknameResponse>

    /**
     * 회원 탈퇴. 성공이 204 본문 없음이라 ApiResponse 를 반환하지 않는다. 회원이 없어도 204 라 멱등이다.
     */
    @DELETE("api/v1/users/me")
    suspend fun deleteUsersMe()
}
