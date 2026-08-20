package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType

interface ImageUploadRepository {
    /**
     * 발급·전송·확인 3단계를 하나로 닫는다. 돌려주는 [ImageId] 는 서버에서 확정까지 마친 것이라
     * 곧바로 배치에 쓸 수 있다.
     *
     * 중간에서 실패하면 그 지점의 실패가 그대로 올라오고 **되돌리지 않는다** — 서버에 정리
     * 경로가 없다(`parfait/api/image.md`). 다시 부르면 발급부터 전부 다시 탄다.
     *
     * @param filePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
     */
    suspend fun upload(
        filePath: String,
        imageType: ImageType,
    ): Result<ImageId>
}
