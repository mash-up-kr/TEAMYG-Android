package com.teamyg.parfait.data.source.image.local

import android.net.Uri
import java.io.File

interface RecentImageFileLocalDataSource {
    fun mkdirs(): Boolean

    fun readBytes(sourceUri: String): ByteArray

    fun getTargetFile(name: String): File

    fun getTargetFile(bytes: ByteArray): File

    fun getUriForFile(target: File): Uri
}
