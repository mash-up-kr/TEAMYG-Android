package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.model.local.RecentImageEntity
import com.teamyg.parfait.data.model.local.toEntity
import com.teamyg.parfait.data.model.local.toVO
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.image.RecentImage
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

    /**
     * 빠진 항목은 만료 정리의 시야에서도 사라져 저장소에 영영 남을 수 있다. 저장값이 항상
     * FileProvider content uri라 발생 조건이 거의 없어 감수한다.
     */
    override val recentCacheImages: Flow<List<RecentImage>> = recentImageLocalDataSource.values
        .map { entities ->
            entities.mapNotNull { entity ->
                val file: File = fileRecentImageLocalDataSource.getTargetFileFromUri(entity.uri)
                    ?: return@mapNotNull null

                RecentImage(
                    uri = entity.uri,
                    filePath = file.absolutePath,
                    kind = entity.kind.toVO(),
                )
            }
        }

    override suspend fun addAndGetEvictedCacheFileName(
        uri: String,
        kind: RecentImageKind,
    ): List<String> {
        var evicted: List<String> = emptyList()

        recentImageLocalDataSource.edit { prefs ->
            val current: List<RecentImageEntity> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<RecentImageEntity> = (
                current.filterNot { it.uri == uri } +
                    listOf(RecentImageEntity(uri = uri, kind = kind.toEntity()))
                ).takeLast(MAX_SIZE)
            val keptUris: List<String> = updated.map(RecentImageEntity::uri)

            evicted = current.filterNot { it.uri in keptUris }.map(RecentImageEntity::uri)
            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }

        return evicted
    }

    override suspend fun removeCacheFileName(values: List<String>) {
        recentImageLocalDataSource.remove(values)
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
        val target: File = fileRecentImageLocalDataSource.getTargetFile(bytes, extensionOf(kind, bytes))

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

    /**
     * 알맹이는 언제나 투명 PNG 라 종류로 정해지지만, 원본은 사용자가 고른 파일이라 내용을 봐야 한다.
     * 이름이 거짓이면 업로드가 content type 을 잘못 정한다.
     */
    private fun extensionOf(
        kind: RecentImageKind,
        bytes: ByteArray,
    ): String = when (kind) {
        RecentImageKind.SOURCE -> UploadImageFormat.ofBytes(bytes)?.extension ?: UploadImageFormat.JPEG.extension
        RecentImageKind.CUTOUT -> UploadImageFormat.PNG.extension
    }

    companion object {
        private const val MAX_SIZE: Int = 9
    }
}
