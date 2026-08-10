package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO

interface ImageRemoteDataSource {
    /**
     * 업로드용 presigned URL 을 발급받는다. 실제 바이트 전송은 이 함수가 하지 않는다 —
     * 호출부가 응답의 uploadUrl 로 직접 PUT 해야 하고, 그 경로는 아직 없다.
     *
     * @param fileName 서버가 현재 이 값을 쓰지 않지만(`api/image.md` 미결) 실제 파일명을 넘긴다.
     *   서버가 쓰기 시작해도 값이 맞고, 빈 문자열은 400 이다.
     * @param contentType image/png 또는 image/jpeg. 그 외는 INVALID_CONTENT_TYPE 실패다.
     */
    suspend fun issueUploadUrl(
        fileName: String,
        contentType: String,
        imageType: ImageType,
    ): Result<ImageUploadUrlVO>

    /**
     * 업로드 완료를 서버에 알려 상태를 COMPLETED 로 올린다.
     *
     * 서버는 S3 에 객체가 실제로 있는지 확인하지 않는다 — 상태 전이만 한다.
     * 이미 확정된 이미지면 IMAGE_ALREADY_CONFIRMED 로 실패하며, 이 코드를 성공으로
     * 번역하지 않는다(서버가 소유자를 검증하지 않아 "내가 이미 했다"와 "남이 했다"가
     * 구분되지 않는다 — `api/image.md`).
     */
    suspend fun confirmUpload(imageId: ImageId): Result<ConfirmedImageVO>
}
