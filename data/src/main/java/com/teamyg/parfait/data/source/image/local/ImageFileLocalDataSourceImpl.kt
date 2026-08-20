package com.teamyg.parfait.data.source.image.local

import android.content.Context
import androidx.core.net.toUri
import com.teamyg.parfait.core.util.android.extension.readBytes
import com.teamyg.parfait.data.model.image.UploadImageFormat
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
        val bytes = context.contentResolver.readBytes(uri.toUri())

        dir.mkdirs()
        // 원래 이름을 쓰지 않는다 — 갤러리 `content://` 의 마지막 조각은 확장자 없는 숫자
        // id 이고, 같은 사진을 두 번 고르면 앞 파일을 덮어쓴다
        val file = File(dir, "${UUID.randomUUID()}.${formatOf(uri = uri, bytes = bytes).extension}")
        file.writeBytes(bytes)

        return file
    }

    private fun formatOf(
        uri: String,
        bytes: ByteArray,
    ): UploadImageFormat {
        val declared = context.contentResolver.getType(uri.toUri())
        UploadImageFormat.ofContentType(declared)?.let { return it }

        val sniffed = UploadImageFormat.ofBytes(bytes)
        requireNotNull(sniffed) { "서버가 받지 않는 이미지 형식이다 - mimeType: $declared" }
        sourceLogger.i { "형식을 바이트로 판정했다 - declared: $declared, sniffed: ${sniffed.contentType}" }

        return sniffed
    }

    private companion object {
        const val UPLOAD_DIR_NAME = "upload"
    }
}
