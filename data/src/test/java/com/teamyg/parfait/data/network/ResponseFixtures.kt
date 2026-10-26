package com.teamyg.parfait.data.network

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** Retrofit 은 2xx 가 아닌 응답을 본문이 비어도 [Response.error] 로 만든다 — 304 도 그 모양으로 온다 */
internal fun <T> notModifiedResponse(): Response<T> {
    val request = Request
        .Builder()
        .url("http://localhost/")
        .build()
    val raw = okhttp3.Response
        .Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(304)
        .message("Not Modified")
        .build()
    return Response.error("".toResponseBody(null), raw)
}
