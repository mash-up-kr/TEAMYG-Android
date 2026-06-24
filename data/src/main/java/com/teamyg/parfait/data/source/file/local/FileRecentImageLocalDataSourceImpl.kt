package com.teamyg.parfait.data.source.file.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.teamyg.parfait.core.util.extensions.sha256
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

    override fun readBytes(sourceUri: String): ByteArray = context.contentResolver
        .openInputStream(sourceUri.toUri())
        .use { input ->
            requireNotNull(input) { "Cannot open input stream for $sourceUri" }

            input.readBytes()
        }

    override fun getTargetFile(name: String): File = File(
        dir,
        name,
    )

    override fun getTargetFile(bytes: ByteArray): File = File(
        dir,
        fileName(bytes),
    )

    override fun getUriForFile(target: File): Uri = FileProvider.getUriForFile(
        context,
        authority,
        target,
    )

    private fun fileName(bytes: ByteArray): String = bytes.sha256() + FILE_EXTENSION

    companion object {
        private const val DIR_NAME = "recent_images"
        private const val AUTHORITY_SUFFIX = ".recent.fileprovider"
        private const val FILE_EXTENSION = ".jpg"
    }
}
