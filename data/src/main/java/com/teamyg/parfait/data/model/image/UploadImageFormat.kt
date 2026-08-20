package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.core.util.jvm.extension.startsWith

/**
 * 업로드가 다룰 수 있는 이미지 형식. 그 외를 보내면 400 INVALID_CONTENT_TYPE 이다
 * (`http/images.http`).
 *
 * 확장자·contentType·시그니처를 한 자리에 묶는 이유: 발급 요청과 S3 PUT 헤더가 같은
 * contentType 을 써야 하고, 파일명 확장자는 그 contentType 을 되짚는 유일한 단서다.
 * 셋이 갈라져 있으면 서버가 받는 형식이 늘어날 때 한쪽만 고쳐도 아무 실패가 드러나지 않는다.
 */
enum class UploadImageFormat(
    val contentType: String,
    private val extensions: List<String>,
    private val signature: ByteArray,
) {
    PNG(
        contentType = "image/png",
        extensions = listOf("png"),
        signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
    ),

    /** `image/jpg` 가 아니라 `image/jpeg` 다 — 앞의 것은 서버가 거절한다 */
    JPEG(
        contentType = "image/jpeg",
        extensions = listOf("jpg", "jpeg"),
        signature = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
    ),
    ;

    /** 파일명을 지을 때 붙일 확장자. 읽을 때는 [ofExtension] 이 다른 표기도 받는다 */
    val extension: String get() = extensions.first()

    companion object {
        fun ofExtension(extension: String): UploadImageFormat? =
            entries.find { format -> extension.lowercase() in format.extensions }

        fun ofContentType(contentType: String?): UploadImageFormat? =
            entries.find { format -> format.contentType == contentType }

        /**
         * 앞머리 바이트로 판정한다. 확장자나 시스템 MIME 과 실제 내용이 어긋난 파일이 드물지
         * 않은데, 잘못 실린 contentType 은 S3 를 통과해 다른 기기에서 깨져 보인다.
         */
        fun ofBytes(bytes: ByteArray): UploadImageFormat? =
            entries.find { format -> bytes.startsWith(format.signature) }
    }
}
