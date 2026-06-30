package com.teamyg.parfait.domain.usecase.camera

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import java.io.File
import javax.inject.Inject

class CreateCameraCacheUriUseCase
@Inject
constructor(
    private val createCameraCacheFileUseCase: CreateCameraCacheFileUseCase,
    private val cameraCacheFileRepository: CameraCacheFileRepository,
) {
    init {
        useCaseLogger.i { "CreateCameraCacheUriUseCase::init" }
    }

    operator fun invoke(): String {
        val file: File = createCameraCacheFileUseCase()

        return cameraCacheFileRepository.getCameraCacheUri(file)
    }

    operator fun invoke(file: File): String = cameraCacheFileRepository.getCameraCacheUri(file)
}
