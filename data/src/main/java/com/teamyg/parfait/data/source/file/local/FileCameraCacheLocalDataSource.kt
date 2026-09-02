package com.teamyg.parfait.data.source.file.local

import android.net.Uri
import java.io.File

interface FileCameraCacheLocalDataSource {
    fun mkdirs(): Boolean

    fun createFile(): File

    fun getUriForFile(file: File): Uri
}
