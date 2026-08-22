package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.image.local.ImageFileLocalDataSource
import com.teamyg.parfait.domain.repository.image.ImageFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageFileRepositoryImpl
@Inject
constructor(
    private val imageFileLocalDataSource: ImageFileLocalDataSource,
) : ImageFileRepository {
    override suspend fun copyToCache(uri: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching { imageFileLocalDataSource.copyToCache(uri).absolutePath }
    }.mapErrorToAppError()
}
