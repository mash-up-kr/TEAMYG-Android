package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.ParfaitYearsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ParfaitService {
    @GET("api/v1/groups/{groupId}/parfaits/year")
    suspend fun getGroupsByGroupIdParfaitsYear(@Path("groupId") groupId: Long): ApiResponse<ParfaitYearsResponse>
}
