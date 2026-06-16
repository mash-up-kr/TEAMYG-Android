package com.teamyg.parfait.data.repository.image

import android.net.Uri
import androidx.core.net.toUri
import com.teamyg.parfait.data.source.image.local.RecentImageFileLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class RecentImageRepositoryImpl
@Inject
constructor(
    private val recentImageLocalDataSource: RecentImageLocalDataSource,
    private val recentImageFileLocalDataSource: RecentImageFileLocalDataSource,
) : RecentImageRepository {
    override val recentCacheImages: Flow<List<String>> = recentImageLocalDataSource.values

    override suspend fun addAndGetEvictedCacheFileName(value: String): List<String> {
        var evicted: List<String> = emptyList()

        recentImageLocalDataSource.edit { prefs ->
            val current: List<String> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<String> = (current.filterNot { it == value } + listOf(value)).takeLast(MAX_SIZE)

            evicted = current.filterNot { it in updated }
            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }

        return evicted
    }

    override suspend fun removeCacheFileName(values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        recentImageLocalDataSource.edit { prefs ->
            val current: List<String> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<String> = current.filterNot { it in values }

            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }
    }

    override suspend fun storeRecentImageInInternalStorage(sourceUri: String): String = withContext(Dispatchers.IO) {
        recentImageFileLocalDataSource.mkdirs()

        val bytes: ByteArray = recentImageFileLocalDataSource.readBytes(sourceUri)
        val target: File = recentImageFileLocalDataSource.getTargetFile(bytes)

        if (target.exists().not()) {
            target
                .outputStream()
                .use { output -> output.write(bytes) }
        }

        target.setLastModified(Clock.System.now().toEpochMilliseconds())

        val uri: Uri = recentImageFileLocalDataSource.getUriForFile(target)

        return@withContext uri.toString()
    }

    override suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean = withContext(Dispatchers.IO) {
        val name = sourceUri.toUri().lastPathSegment ?: return@withContext false

        val file: File = recentImageFileLocalDataSource.getTargetFile(name)

        return@withContext file.delete()
    }

    override suspend fun getLastModifiedCacheFile(sourceUri: String): Long? = withContext(Dispatchers.IO) {
        val name = sourceUri.toUri().lastPathSegment ?: return@withContext null
        val file: File = recentImageFileLocalDataSource.getTargetFile(name)

        return@withContext when (file.exists()) {
            true -> file.lastModified()
            false -> null
        }
    }

    companion object {
        private const val MAX_SIZE: Int = 9
    }
}
