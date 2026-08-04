package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoAuth
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import retrofit2.http.GET

interface PolicyService {
    @NoAuth
    @GET("api/v1/policies")
    suspend fun getPolicies(): ApiResponse<PolicyResponse>
}
