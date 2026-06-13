package com.teamyg.parfait.data.source.image.local

import android.content.Context
import androidx.core.content.FileProvider
import com.teamyg.parfait.core.util.extensions.sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RecentImageFileLocalDataSourceImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : RecentImageFileLocalDataSource {
    private val dir: File by lazy {
        File(
            context.filesDir,
            DIR_NAME,
        )
    }

    override suspend fun store(sourceUri: String): String = withContext(Dispatchers.IO) {
        dir.mkdirs()

        val bytes = context.contentResolver
            .openInputStream(sourceUri.toUri())
            .use { input ->
                requireNotNull(input) { "Cannot open input stream for $sourceUri" }

                input.readBytes()
            }

        val target = File(dir, fileName(bytes))

        if (!target.exists()) {
            target.outputStream().use { output -> output.write(bytes) }
        }

        target.setLastModified(System.currentTimeMillis())

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            target,
        )

        return@withContext uri.toString()
    }

    override suspend fun delete(cachedUri: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val name = cachedUri.toUri().lastPathSegment ?: return@runCatching

                File(
                    dir,
                    name,
                ).delete()
            }
        }
    }

    override suspend fun lastModified(cachedUri: String): Long = withContext(Dispatchers.IO) {
        runCatching {
            val name = cachedUri.toUri().lastPathSegment ?: return@runCatching 0L

            File(dir, name).let { file ->
                when (file.exists()) {
                    true -> file.lastModified()
                    false -> 0L
                }
            }
        }.getOrDefault(0L)
    }

    private fun fileName(bytes: ByteArray): String = bytes.sha256() + FILE_EXTENSION

    companion object {
        private const val DIR_NAME = "recent_images"
        private const val AUTHORITY_SUFFIX = ".recent.fileprovider"
        private const val FILE_EXTENSION = ".jpg"
    }
}
