package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.Headers
import retrofit2.HttpException
import retrofit2.Response
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

    /** 304 는 2xx 가 아니라 Retrofit 이 예외 없이 [Response] 로 돌려주므로 여기서 [notModified] 로 바꾼다 */
    suspend fun <T : Any, R> safeConditionalApiCall(
        block: suspend () -> Response<ApiResponse<T>>,
        notModified: R,
        transform: (data: T, headers: Headers) -> R,
    ): Result<R> = catchingApi {
        val response = block()
        when {
            response.code() == HTTP_NOT_MODIFIED -> Result.success(notModified)

            !response.isSuccessful -> throw HttpException(response)

            else -> {
                val body = checkNotNull(response.body()) { "본문 없는 ${response.code()} 응답" }
                if (body.success) {
                    body.data?.let { Result.success(transform(it, response.headers())) }
                        ?: Result.failure(ApiException.EmptyBody(body.code, body.message))
                } else {
                    Result.failure(body.toBusinessException())
                }
            }
        }
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
    ): Result<R> = catchingApi {
        val response = block()
        if (response.success) onSuccess(response) else Result.failure(response.toBusinessException())
    }

    private fun ApiResponse<*>.toBusinessException() = ApiException.Business(
        code = code,
        serverMessage = message,
        statusCode = null,
        errorDetail = errorDetail,
    )

    private suspend fun <R> catchingApi(body: suspend () -> Result<R>): Result<R> = try {
        body()
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

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
    }
}
