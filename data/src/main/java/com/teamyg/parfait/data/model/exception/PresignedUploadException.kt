package com.teamyg.parfait.data.model.exception

/**
 * S3 가 presigned PUT 을 거절했다.
 *
 * 이 실패는 우리 서버를 거치지 않아 서버 로그에 아무것도 남지 않는다. 그래서 상태 코드를
 * 여기 실어 둔다 — 원인 추적의 유일한 단서다.
 */
class PresignedUploadException(
    val statusCode: Int,
) : Exception("presigned PUT 거절 - statusCode: $statusCode")
