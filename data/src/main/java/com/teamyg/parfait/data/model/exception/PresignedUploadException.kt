package com.teamyg.parfait.data.model.exception

/**
 * S3 가 presigned PUT 을 거절했다. 우리 서버를 거치지 않아 서버 로그에 남지 않으므로,
 * [statusCode] 가 원인 추적의 유일한 단서다.
 */
class PresignedUploadException(
    val statusCode: Int,
) : Exception("presigned PUT 거절 - statusCode: $statusCode")
