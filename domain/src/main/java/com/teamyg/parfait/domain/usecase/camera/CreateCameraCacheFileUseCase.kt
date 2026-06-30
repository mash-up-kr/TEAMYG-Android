package com.teamyg.parfait.domain.usecase.camera

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import java.io.File
import javax.inject.Inject

class CreateCameraCacheFileUseCase
@Inject
constructor(
    private val cameraCacheFileRepository: CameraCacheFileRepository,
) {
    init {
        useCaseLogger.i { "CreateCameraCacheFileUseCase::init" }
    }

    operator fun invoke(): File {
        val makeCondition: Boolean = cameraCacheFileRepository.makeCameraCacheFileDirs()
        useCaseLogger.d { "CreateCameraCacheFileUseCase - makeCondition: $makeCondition" }

        return cameraCacheFileRepository.createCameraCacheFile()
    }
}
