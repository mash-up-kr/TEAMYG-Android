package com.teamyg.parfait.domain.model.image

import com.teamyg.parfait.domain.model.id.ImageId
import kotlin.time.Duration

/**
 * @param uploadUrl S3 presigned PUT URL. 한 번 쓰고 버리며 [expiresIn] 동안만 유효하다.
 * @param imageUrl 업로드 후 접근할 공개 주소. 오래 보관한다.
 *   [uploadUrl] 과 둘 다 String 이라 바꿔 넣어도 컴파일러가 막지 못한다.
 */
data class ImageUploadUrlVO(
    val imageId: ImageId,
    val uploadUrl: String,
    val imageUrl: String,
    val expiresIn: Duration,
)
