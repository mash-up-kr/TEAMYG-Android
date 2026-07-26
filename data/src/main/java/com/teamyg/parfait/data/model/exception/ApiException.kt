package com.teamyg.parfait.data.model.exception

import retrofit2.HttpException
import java.io.IOException

sealed class ApiException(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    data class Business(
        val code: String,
        val serverMessage: String,
    ) : ApiException(serverMessage, null)

    data class Http(
        val statusCode: Int,
        override val cause: HttpException,
    ) : ApiException(cause.message, cause)

    data class Network(
        override val cause: IOException,
    ) : ApiException(cause.message, cause)

    data class Unknown(
        override val cause: Throwable,
    ) : ApiException(cause.message, cause)
}
