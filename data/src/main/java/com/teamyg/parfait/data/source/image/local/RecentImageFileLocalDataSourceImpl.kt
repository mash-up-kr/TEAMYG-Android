package com.teamyg.parfait.data.source.image.local

import android.content.Context
import androidx.core.content.FileProvider
import com.teamyg.parfait.core.util.sha256
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
    private val dir: File
        get() = File(
            context.filesDir,
            DIR_NAME,
        ).apply {
            mkdirs()
        }

    override suspend fun copy(sourceUri: String): String = withContext(Dispatchers.IO) {
        val target = File(dir, fileName(sourceUri))

        if (!target.exists()) {
            context.contentResolver
                .openInputStream(sourceUri.toUri())
                .use { input ->
                    requireNotNull(input) { "Cannot open input stream for $sourceUri" }

                    target.outputStream().use { output -> input.copyTo(output) }
                }
        }

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

    private fun fileName(sourceUri: String): String = sourceUri.sha256() + FILE_EXTENSION

    companion object {
        private const val DIR_NAME = "recent_images"
        private const val AUTHORITY_SUFFIX = ".recent.fileprovider"
        private const val FILE_EXTENSION = ".jpg"
    }
}
