package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.PresignedUploadException
import com.teamyg.parfait.data.model.qualifier.UploadClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

class PresignedUploadDataSourceImpl @Inject constructor(
    @UploadClient private val okHttpClient: OkHttpClient,
) : PresignedUploadDataSource {
    /**
     * `execute()` 가 아니라 `enqueue` 를 쓰는 것이 취소 전파의 전부다 — 블로킹 호출은
     * 코루틴이 취소돼도 스스로 멈추지 않아 `callTimeout` 까지 돈다. 자체 디스패처 위에서
     * 돌므로 `withContext(Dispatchers.IO)` 도 필요 없다.
     */
    override suspend fun put(
        uploadUrl: String,
        contentType: String,
        file: File,
    ): Result<Unit> {
        val call = try {
            // asRequestBody 는 파일을 스트리밍으로 읽는다. 바이트를 미리 배열에 담으면 원본
            // 해상도 이미지가 통째로 힙에 올라간다
            val request = Request
                .Builder()
                .url(uploadUrl)
                .put(file.asRequestBody(contentType.toMediaType()))
                .build()

            okHttpClient.newCall(request)
        } catch (e: Exception) {
            // uploadUrl·contentType 은 서버가 준 값이라 Request 조립 단계에서 예외가 날 수 있다.
            // 여기서 안 잡으면 Result 를 돌려주기로 한 계약이 깨진 채 호출부까지 올라간다.
            // 이 블록에는 suspend 호출이 없어 CancellationException 재던지기가 필요 없다 —
            // 한 줄이라도 들어오면 그때 가드를 붙인다
            return Result.failure(ApiException.Unknown(e))
        }

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(
                object : Callback {
                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            val result = if (it.isSuccessful) {
                                Result.success(Unit)
                            } else {
                                Result.failure(ApiException.Unknown(PresignedUploadException(it.code)))
                            }
                            if (continuation.isActive) continuation.resume(result)
                        }
                    }

                    // 취소로 끊긴 호출도 IOException("Canceled") 로 여기 들어온다.
                    // 이미 취소된 continuation 은 resume 을 버리므로 굳이 부르지 않는다
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) continuation.resume(Result.failure(ApiException.Network(e)))
                    }
                },
            )
        }
    }
}
