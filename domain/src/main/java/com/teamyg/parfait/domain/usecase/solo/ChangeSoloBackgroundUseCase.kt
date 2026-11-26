package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.solo.SoloBackgroundEdit
import com.teamyg.parfait.domain.repository.image.ImageFileRepository
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class ChangeSoloBackgroundUseCase @Inject constructor(
    private val imageFileRepository: ImageFileRepository,
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend fun color(hex: String): Result<Unit> = soloSpaceRepository.changeBackground(SoloBackgroundEdit.Color(hex))

    /** 복사에 실패하면 저장소를 부르지 않고 그 실패를 그대로 돌려준다 */
    suspend fun image(uri: String): Result<Unit> {
        val cachedPath = imageFileRepository
            .copyToCache(uri)
            .getOrElse { throwable -> return Result.failure(throwable) }
        return soloSpaceRepository.changeBackground(SoloBackgroundEdit.Image(cachedPath))
    }
}
