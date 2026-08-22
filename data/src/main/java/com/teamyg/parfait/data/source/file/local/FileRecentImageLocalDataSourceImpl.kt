package com.teamyg.parfait.data.source.file.local

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.teamyg.parfait.core.util.android.extension.readBytes
import com.teamyg.parfait.core.util.jvm.extension.sha256
import com.teamyg.parfait.data.utils.sourceLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRecentImageLocalDataSourceImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : FileRecentImageLocalDataSource {
    init {
        sourceLogger.i { "FileRecentImageLocalDataSourceImpl::init" }
    }

    private val dir: File by lazy {
        File(
            context.filesDir,
            DIR_NAME,
        )
    }

    private val authority: String = context.packageName + AUTHORITY_SUFFIX

    override fun mkdirs(): Boolean = dir.mkdirs()

    override fun readBytes(sourceUri: String): ByteArray = context.contentResolver.readBytes(sourceUri.toUri())

    override fun readFileBytes(filePath: String): ByteArray = File(filePath).readBytes()

    override fun getTargetFile(
        bytes: ByteArray,
        extension: String,
    ): File = File(
        dir,
        bytes.sha256() + "." + extension,
    )

    override fun getTargetFileFromUri(uri: String): File? = uri
        .toUri()
        .lastPathSegment
        ?.let { name -> File(dir, name) }

    override fun getUriStringForFile(target: File): String = FileProvider
        .getUriForFile(
            context,
            authority,
            target,
        ).toString()

    companion object {
        private const val DIR_NAME = "recent_images"
        private const val AUTHORITY_SUFFIX = ".recent.fileprovider"
    }
}
