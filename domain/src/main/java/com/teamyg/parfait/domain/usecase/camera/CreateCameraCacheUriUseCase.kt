package com.teamyg.parfait.domain.usecase.camera

import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import java.io.File
import javax.inject.Inject

class CreateCameraCacheUriUseCase
@Inject
constructor(
    private val cameraCacheFileRepository: CameraCacheFileRepository,
) {
    operator fun invoke(): String {
        cameraCacheFileRepository.makeCameraCacheFileDirs()

        val file = cameraCacheFileRepository.createCameraCacheFile()

        return cameraCacheFileRepository.getCameraCacheUri(file)
    }

    operator fun invoke(file: File): String = cameraCacheFileRepository.getCameraCacheUri(file)
}
