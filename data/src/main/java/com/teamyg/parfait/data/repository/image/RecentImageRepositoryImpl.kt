package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.local.RecentImageEntity
import com.teamyg.parfait.data.model.local.RecentImageKindEntity
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val fileRecentImageLocalDataSource: FileRecentImageLocalDataSource,
) : RecentImageRepository {
    init {
        repositoryLogger.i { "RecentImageRepositoryImpl::init" }
    }

    override val recentCacheImages: Flow<List<String>> = recentImageLocalDataSource.values
        .map { entities -> entities.map { it.uri } }

    override suspend fun addAndGetEvictedCacheFileName(value: String): List<String> {
        var evicted: List<String> = emptyList()

        recentImageLocalDataSource.edit { prefs ->
            val current: List<RecentImageEntity> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<RecentImageEntity> = (
                current.filterNot { it.uri == value } +
                    listOf(RecentImageEntity(uri = value, kind = RecentImageKindEntity.SOURCE))
                ).takeLast(MAX_SIZE)

            evicted = current.filterNot { it.uri in updated.map(RecentImageEntity::uri) }.map(RecentImageEntity::uri)
            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }

        return evicted
    }

    override suspend fun removeCacheFileName(values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        recentImageLocalDataSource.edit { prefs ->
            val current: List<RecentImageEntity> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<RecentImageEntity> = current.filterNot { it.uri in values }

            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }
    }

    override suspend fun storeRecentImageInInternalStorage(
        source: String,
        kind: RecentImageKind,
    ): String = withContext(Dispatchers.IO) {
        fileRecentImageLocalDataSource.mkdirs()

        val bytes: ByteArray = when (kind) {
            // 갤러리·카메라가 주는 것은 uri 이고, 초안이 주는 것은 스킴 없는 절대경로다
            RecentImageKind.SOURCE -> fileRecentImageLocalDataSource.readBytes(source)

            RecentImageKind.CUTOUT -> fileRecentImageLocalDataSource.readFileBytes(source)
        }
        val target: File = fileRecentImageLocalDataSource.getTargetFile(bytes, kind.fileExtension())

        if (target.exists().not()) {
            target
                .outputStream()
                .use { output -> output.write(bytes) }
        }

        target.setLastModified(Clock.System.now().toEpochMilliseconds())

        return@withContext fileRecentImageLocalDataSource.getUriStringForFile(target)
    }

    override suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean = withContext(Dispatchers.IO) {
        val file: File = fileRecentImageLocalDataSource.getTargetFileFromUri(sourceUri) ?: return@withContext false

        return@withContext file.delete()
    }

    override suspend fun getLastModifiedCacheFile(sourceUri: String): Long? = withContext(Dispatchers.IO) {
        val file: File = fileRecentImageLocalDataSource.getTargetFileFromUri(sourceUri) ?: return@withContext null

        return@withContext when (file.exists()) {
            true -> file.lastModified()
            false -> null
        }
    }

    /** 알맹이는 투명 PNG 다. 이름이 거짓이면 업로드가 content type 을 잘못 정한다 */
    private fun RecentImageKind.fileExtension(): String = when (this) {
        RecentImageKind.SOURCE -> "jpg"
        RecentImageKind.CUTOUT -> "png"
    }

    companion object {
        private const val MAX_SIZE: Int = 9
    }
}
