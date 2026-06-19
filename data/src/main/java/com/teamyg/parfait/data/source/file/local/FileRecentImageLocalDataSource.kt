package com.teamyg.parfait.data.source.file.local

import android.net.Uri
import java.io.File

interface FileRecentImageLocalDataSource {
    fun mkdirs(): Boolean

    fun readBytes(sourceUri: String): ByteArray

    fun getTargetFile(name: String): File

    fun getTargetFile(bytes: ByteArray): File

    fun getUriForFile(target: File): Uri
}
