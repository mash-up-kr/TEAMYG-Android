package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.group.ChangeMyParfaitGroupNicknameRequest
import com.teamyg.parfait.data.service.model.request.group.CreateParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.JoinParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.ReportParfaitGroupRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.group.ChangeMyParfaitGroupNicknameResponse
import com.teamyg.parfait.data.service.model.response.group.CreateParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.JoinParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.LeaveParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupDetailResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.PreviewParfaitGroupJoinResponse
import com.teamyg.parfait.data.service.model.response.group.ReportParfaitGroupResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ParfaitGroupService {
    @GET("api/parfait-groups")
    suspend fun getParfaitGroups(): ApiResponse<List<MyParfaitGroupResponse>>

    @GET("api/parfait-groups/{groupId}")
    suspend fun getParfaitGroupsByGroupId(@Path("groupId") groupId: Long): ApiResponse<MyParfaitGroupDetailResponse>

    @GET("api/parfait-groups/join-preview")
    suspend fun getParfaitGroupsJoinPreview(
        @Query("inviteCode") inviteCode: String,
    ): ApiResponse<PreviewParfaitGroupJoinResponse>

    @POST("api/parfait-groups/join")
    suspend fun postParfaitGroupsJoin(@Body request: JoinParfaitGroupRequest): ApiResponse<JoinParfaitGroupResponse>

    @POST("api/parfait-groups")
    suspend fun postParfaitGroups(@Body request: CreateParfaitGroupRequest): ApiResponse<CreateParfaitGroupResponse>

    @PATCH("api/parfait-groups/{groupId}/nickname")
    suspend fun patchParfaitGroupsByGroupIdNickname(
        @Path("groupId") groupId: Long,
        @Body request: ChangeMyParfaitGroupNicknameRequest,
    ): ApiResponse<ChangeMyParfaitGroupNicknameResponse>

    @DELETE("api/parfait-groups/{groupId}/members/me")
    suspend fun deleteParfaitGroupsByGroupIdMembersMe(
        @Path("groupId") groupId: Long,
    ): ApiResponse<LeaveParfaitGroupResponse>

    @POST("api/parfait-groups/{groupId}/reports")
    suspend fun postParfaitGroupsByGroupIdReports(
        @Path("groupId") groupId: Long,
        @Body request: ReportParfaitGroupRequest,
    ): ApiResponse<ReportParfaitGroupResponse>
}
