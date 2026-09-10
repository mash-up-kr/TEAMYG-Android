package com.teamyg.parfait.data.model.image

import java.io.File

/**
 * @param isTemporary 전처리가 새로 만든 파일이라 부른 쪽이 지워야 한다. 거짓이면 넘겨받은
 *   파일 그대로라 수명은 부른 쪽 밖에 있다.
 */
data class PreparedUploadImage(
    val file: File,
    val format: UploadImageFormat,
    val isTemporary: Boolean,
)
