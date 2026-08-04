package com.teamyg.parfait.data.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class GalleryMediaProvider(
    private val context: Context,
) {
    val collectionUri: Uri? = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection: Array<String> = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
    )

    val sortOrder: String =
        "COALESCE(${MediaStore.Images.Media.DATE_TAKEN}, ${MediaStore.Images.Media.DATE_ADDED} * 1000) DESC"

    val selection: String =
        "(${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?) " +
            "OR ${MediaStore.Images.Media.DATE_TAKEN} IS NULL"

    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        val resolver: ContentResolver = context.contentResolver ?: return null

        return resolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )
    }

    fun resolveTimestampMs(
        cursor: Cursor,
        takenColumn: Int,
        addedColumn: Int,
    ): Long {
        val takenMs = when (!cursor.isNull(takenColumn)) {
            true -> cursor.getLong(takenColumn)
            false -> 0L
        }

        if (takenMs > 0L) {
            return takenMs
        }

        val addedSeconds = cursor.getLong(addedColumn)
        return addedSeconds * 1000L
    }
}
