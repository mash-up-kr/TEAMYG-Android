package com.teamyg.parfait.data.source.image.local

import android.content.Context
import androidx.core.net.toUri
import com.teamyg.parfait.data.utils.sourceLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageFileLocalDataSourceImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ImageFileLocalDataSource {
    init {
        sourceLogger.i { "ImageFileLocalDataSourceImpl::init" }
    }

    private val dir: File by lazy { File(context.cacheDir, UPLOAD_DIR_NAME) }

    override fun copyToCache(uri: String): File {
        val bytes = context.contentResolver
            .openInputStream(uri.toUri())
            .use { input ->
                requireNotNull(input) { "이미지를 열 수 없다 - uri: $uri" }

                input.readBytes()
            }

        dir.mkdirs()
        // 원래 이름을 쓰지 않는다 — 갤러리 `content://` 의 마지막 조각은 확장자 없는 숫자
        // id 이고, 같은 사진을 두 번 고르면 앞 파일을 덮어쓴다
        val file = File(dir, "${UUID.randomUUID()}.${extensionOf(uri = uri, bytes = bytes)}")
        file.writeBytes(bytes)

        return file
    }

    private fun extensionOf(
        uri: String,
        bytes: ByteArray,
    ): String {
        val declared = context.contentResolver.getType(uri.toUri())
        EXTENSION_BY_MIME[declared]?.let { return it }

        val sniffed = bytes.sniffExtension()
        require(sniffed != null) { "서버가 받지 않는 이미지 형식이다 - mimeType: $declared" }
        sourceLogger.i { "확장자를 바이트로 판정했다 - declared: $declared, sniffed: $sniffed" }

        return sniffed
    }

    private fun ByteArray.sniffExtension(): String? = when {
        startsWith(PNG_SIGNATURE) -> EXTENSION_PNG
        startsWith(JPEG_SIGNATURE) -> EXTENSION_JPEG
        else -> null
    }

    private fun ByteArray.startsWith(signature: ByteArray): Boolean =
        size >= signature.size && signature.indices.all { index -> this[index] == signature[index] }

    private companion object {
        const val UPLOAD_DIR_NAME = "upload"

        const val EXTENSION_PNG = "png"
        const val EXTENSION_JPEG = "jpg"

        /** 서버가 받는 두 가지. 그 외는 400 INVALID_CONTENT_TYPE 이다(`http/images.http`) */
        val EXTENSION_BY_MIME = mapOf(
            "image/png" to EXTENSION_PNG,
            "image/jpeg" to EXTENSION_JPEG,
        )

        val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    }
}
