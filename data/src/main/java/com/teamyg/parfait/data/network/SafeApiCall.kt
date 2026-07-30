package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.service.model.response.ApiResponse
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T : Any> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> = runCatchingApi(block) { response ->
    response.data?.let { Result.success(it) }
        ?: Result.failure(ApiException.EmptyBody(response.code, response.message))
}

suspend fun safeApiCallWithoutData(block: suspend () -> ApiResponse<Unit>): Result<Unit> =
    runCatchingApi(block) { Result.success(Unit) }

private suspend fun <T, R> runCatchingApi(
    block: suspend () -> ApiResponse<T>,
    onSuccessCode: (ApiResponse<T>) -> Result<R>,
): Result<R> = try {
    val response = block()
    if (response.isSuccess) {
        onSuccessCode(response)
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
