package com.teamyg.parfait.data.network

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val skipAuth = originalRequest
            .tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoAuth::class.java) == true

        val token = tokenProvider.getToken()
        val request = originalRequest
            .newBuilder()
            .apply {
                if (token != null && skipAuth.not()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }.build()
        return chain.proceed(request)
    }
}
