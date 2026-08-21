package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.PresignedUploadException
import com.teamyg.parfait.data.model.qualifier.UploadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class PresignedUploadDataSourceImpl @Inject constructor(
    @UploadClient private val okHttpClient: OkHttpClient,
) : PresignedUploadDataSource {
    override suspend fun put(
        uploadUrl: String,
        contentType: String,
        file: File,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // asRequestBody 는 파일을 스트리밍으로 읽는다. 바이트를 미리 배열에 담으면 원본
            // 해상도 이미지가 통째로 힙에 올라간다
            val request = Request
                .Builder()
                .url(uploadUrl)
                .put(file.asRequestBody(contentType.toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(ApiException.Unknown(PresignedUploadException(response.code)))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(ApiException.Network(e))
        } catch (e: Exception) {
            // uploadUrl·contentType 은 서버가 준 값이라 Request 조립 단계에서 예외가 날 수 있다.
            // 여기서 안 잡으면 Result 를 돌려주기로 한 계약이 깨진 채 호출부까지 올라간다
            Result.failure(ApiException.Unknown(e))
        }
    }
}
