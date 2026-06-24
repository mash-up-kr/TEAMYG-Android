package com.teamyg.parfait.data.repository.camera

import android.net.Uri
import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSource
import com.teamyg.parfait.data.utils.repoLogger
import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.toString

@Singleton
class CameraCacheFileRepositoryImpl
@Inject
constructor(
    private val fileCameraCacheLocalDataSource: FileCameraCacheLocalDataSource,
) : CameraCacheFileRepository {
    init {
        repoLogger.i { "CameraCacheFileRepositoryImpl::init" }
    }

    override fun makeCameraCacheFileDirs(): Boolean = fileCameraCacheLocalDataSource.mkdirs()

    override fun createCameraCacheFile(): File = fileCameraCacheLocalDataSource.createFile()

    override fun getCameraCacheUri(file: File): String {
        val uri: Uri = fileCameraCacheLocalDataSource.getUriForFile(file)

        return uri.toString()
    }
}
