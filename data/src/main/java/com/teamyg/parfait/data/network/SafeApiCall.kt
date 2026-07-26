package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.service.model.response.ApiResponse
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> = try {
    val response = block()
    val data = response.data
    if (response.isSuccess && data != null) {
        Result.success(data)
    } else {
        Result.failure(ApiException.Business(response.code, response.message))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Result.failure(ApiException.Http(e.code(), e))
} catch (e: IOException) {
    Result.failure(ApiException.Network(e))
} catch (e: Exception) {
    Result.failure(ApiException.Unknown(e))
}
