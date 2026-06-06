package com.teamyg.gallery.impl.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.teamyg.gallery.impl.model.GalleryImageGroup
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object GalleryMediaProvider {
    private val collectionUri: Uri? = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private val projection: Array<String> = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
    )

    private const val SORT_ORDER: String =
        "COALESCE(${MediaStore.Images.Media.DATE_TAKEN}, ${MediaStore.Images.Media.DATE_ADDED} * 1000) DESC"

    private val dateFormat = LocalDateTime.Format {
        year()
        char('.')
        monthNumber()
        char('.')
        this@Format.day(padding = Padding.ZERO)
    }

    fun loadImageGroups(context: Context): List<GalleryImageGroup> {
        val uri = collectionUri ?: return emptyList()

        val resolver = context.contentResolver
        val timeZone = TimeZone.currentSystemDefault()
        val grouped = linkedMapOf<String, MutableList<String>>()

        resolver
            .query(
                uri,
                projection,
                null,
                null,
                SORT_ORDER,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id: Long = cursor.getLong(idColumn)

                    val timestampMs: Long = resolveTimestampMs(
                        cursor = cursor,
                        takenColumn = takenColumn,
                        addedColumn = addedColumn,
                    )

                    val dateKey: String = Instant
                        .fromEpochMilliseconds(timestampMs)
                        .toLocalDateTime(timeZone)
                        .format(dateFormat)

                    val imageUri: String = ContentUris
                        .withAppendedId(uri, id)
                        .toString()

                    grouped
                        .getOrPut(dateKey) { mutableListOf() }
                        .add(imageUri)
                }
            }

        return grouped.map { (date, uris) ->
            GalleryImageGroup(
                date = date,
                images = uris.toList(),
            )
        }
    }

    private fun resolveTimestampMs(
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
