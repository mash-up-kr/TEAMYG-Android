package com.teamyg.parfait.data.network

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

/**
 * [NoBodyLog] 가 붙은 엔드포인트만 [redacted] 로, 나머지는 [full] 로 로깅한다.
 *
 * 인스턴스를 둘 두는 이유: `HttpLoggingInterceptor.level` 은 가변 필드라 요청마다 바꾸면
 * 동시 요청끼리 서로의 레벨을 덮어쓴다.
 */
class SelectiveLoggingInterceptor(
    private val full: Interceptor,
    private val redacted: Interceptor,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val suppressBody = chain
            .request()
            .tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoBodyLog::class.java) == true

        return if (suppressBody) redacted.intercept(chain) else full.intercept(chain)
    }
}
