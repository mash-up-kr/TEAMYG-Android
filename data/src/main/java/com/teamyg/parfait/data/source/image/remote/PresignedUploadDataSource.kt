package com.teamyg.parfait.data.source.image.remote

import java.io.File

interface PresignedUploadDataSource {
    /**
     * 발급받은 presigned URL 로 파일 바이트를 그대로 올린다. 우리 서버가 아니라 S3 로 나가는
     * 유일한 요청이다.
     *
     * @param contentType URL 을 발급받을 때 보낸 값과 **반드시 같아야 한다.** 서명 대상이라
     *   어긋나면 S3 가 거절하고, 그 실패는 서버 로그에 남지 않는다.
     */
    suspend fun put(
        uploadUrl: String,
        contentType: String,
        file: File,
    ): Result<Unit>
}
