package com.teamyg.parfait.domain.usecase.camera

import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import java.io.File
import javax.inject.Inject

class CreateCameraCacheFileUseCase
@Inject
constructor(
    private val cameraCacheFileRepository: CameraCacheFileRepository,
) {
    operator fun invoke(): File {
        cameraCacheFileRepository.makeCameraCacheFileDirs()
        return cameraCacheFileRepository.createCameraCacheFile()
    }
}
