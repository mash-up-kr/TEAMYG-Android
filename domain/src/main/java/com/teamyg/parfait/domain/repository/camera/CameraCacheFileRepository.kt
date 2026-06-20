package com.teamyg.parfait.domain.repository.camera

import java.io.File

interface CameraCacheFileRepository {
    fun makeCameraCacheFileDirs(): Boolean

    fun createCameraCacheFile(): File

    fun getCameraCacheUri(file: File): String
}
