package com.teamyg.parfait.data.source.file.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class FileCameraCacheLocalDataSourceImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : FileCameraCacheLocalDataSource {
    private val dir: File by lazy {
        File(
            context.cacheDir,
            CAMERA_DIR_NAME,
        )
    }

    private val authority: String = context.packageName + AUTHORITY_SUFFIX

    override fun mkdirs(): Boolean = dir.mkdirs()

    @OptIn(FormatStringsInDatetimeFormats::class)
    override fun createFile(): File {
        val timestamp = Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format { byUnicodePattern(FILE_NAME_PATTERN) })

        return File(
            dir,
            "IMG_$timestamp.jpg",
        )
    }

    override fun getUriForFile(file: File): Uri = FileProvider.getUriForFile(
        context,
        authority,
        file,
    )

    private companion object {
        private const val AUTHORITY_SUFFIX = ".camera.fileprovider"
        private const val CAMERA_DIR_NAME = "camera"
        private const val FILE_NAME_PATTERN = "yyyyMMdd_HHmmss"
    }
}
