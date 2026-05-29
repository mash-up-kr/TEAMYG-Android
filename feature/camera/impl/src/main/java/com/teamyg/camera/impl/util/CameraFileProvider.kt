package com.teamyg.camera.impl.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object CameraFileProvider {
    private const val AUTHORITY_SUFFIX = ".camera.fileprovider"
    private const val CAMERA_DIR_NAME = "camera"
    private const val FILE_NAME_PATTERN = "yyyyMMdd_HHmmss"

    fun createImageFile(context: Context): File {
        val cameraDir = File(context.cacheDir, CAMERA_DIR_NAME).apply { mkdirs() }
        val timestamp = SimpleDateFormat(FILE_NAME_PATTERN, Locale.US).format(Date())
        return File(cameraDir, "IMG_$timestamp.jpg")
    }

    fun toContentUri(
        context: Context,
        file: File,
    ): Uri =
        FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file,
        )

    fun createImageUri(context: Context): Uri =
        toContentUri(
            context = context,
            file = createImageFile(context),
        )
}
