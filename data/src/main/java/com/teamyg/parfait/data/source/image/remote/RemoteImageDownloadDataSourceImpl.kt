package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.qualifier.DownloadClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RemoteImageDownloadDataSourceImpl
@Inject
constructor(
    @DownloadClient private val okHttpClient: OkHttpClient,
) : RemoteImageDownloadDataSource {
    /**
     * `execute()` 가 아니라 `enqueue` 를 쓰는 것이 취소 전파의 전부다 — 블로킹 호출은
     * 코루틴이 취소돼도 스스로 멈추지 않는다. 자체 디스패처 위에서 돌므로
     * `withContext(Dispatchers.IO)` 도 필요 없다.
     */
    override suspend fun download(url: String): ByteArray {
        val call = okHttpClient.newCall(Request.Builder().url(url).build())

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(
                object : Callback {
                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            if (!continuation.isActive) return@use

                            if (!it.isSuccessful) {
                                continuation.resumeWithException(
                                    IOException("이미지 다운로드 실패 - code: ${it.code}, url: $url"),
                                )
                                return@use
                            }

                            val bytes = it.body?.bytes()
                            if (bytes != null) {
                                continuation.resume(bytes)
                            } else {
                                continuation.resumeWithException(IOException("응답 본문이 비었다 - url: $url"))
                            }
                        }
                    }

                    // 취소로 끊긴 호출도 IOException("Canceled") 로 여기 들어온다.
                    // 이미 취소된 continuation 은 resume 을 버리므로 굳이 부르지 않는다
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                },
            )
        }
    }
}
