package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class ApiCaller @Inject constructor(
    @RemoteJson private val json: Json,
) {
    suspend fun <T : Any> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> =
        runCatchingApi(block) { response ->
            response.data?.let { Result.success(it) }
                ?: Result.failure(ApiException.EmptyBody(response.code, response.message))
        }

    suspend fun <T : Any, R> safeApiCall(
        block: suspend () -> ApiResponse<T>,
        transform: (T) -> R,
    ): Result<R> = runCatchingApi(block) { response ->
        response.data?.let { Result.success(transform(it)) }
            ?: Result.failure(ApiException.EmptyBody(response.code, response.message))
    }

    suspend fun safeApiCallWithoutData(block: suspend () -> ApiResponse<Unit>): Result<Unit> =
        runCatchingApi(block) { Result.success(Unit) }

    suspend fun safeApiCallNoContent(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(toApiException(e))
    } catch (e: IOException) {
        Result.failure(ApiException.Network(e))
    } catch (e: Exception) {
        Result.failure(ApiException.Unknown(e))
    }

    private suspend fun <T, R> runCatchingApi(
        block: suspend () -> ApiResponse<T>,
        onSuccess: (ApiResponse<T>) -> Result<R>,
    ): Result<R> = try {
        val response = block()
        if (response.success) {
            onSuccess(response)
        } else {
            Result.failure(
                ApiException.Business(
                    code = response.code,
                    serverMessage = response.message,
                    statusCode = null,
                    errorDetail = response.errorDetail,
                ),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(toApiException(e))
    } catch (e: IOException) {
        Result.failure(ApiException.Network(e))
    } catch (e: Exception) {
        Result.failure(ApiException.Unknown(e))
    }

    private fun toApiException(e: HttpException): ApiException {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        if (body.isNullOrBlank()) return ApiException.Http(e.code(), e)
        val envelope = runCatching {
            json.decodeFromString(ApiResponse.serializer(Unit.serializer()), body)
        }.getOrNull() ?: return ApiException.Http(e.code(), e)
        return ApiException.Business(
            code = envelope.code,
            serverMessage = envelope.message,
            statusCode = e.code(),
            errorDetail = envelope.errorDetail,
        )
    }
}
