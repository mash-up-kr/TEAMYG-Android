package com.teamyg.parfait.feature.gallery.impl.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.teamyg.parfait.core.util.model.DayWindow
import com.teamyg.parfait.feature.gallery.impl.model.GalleryImageGroup
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
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

    private const val SELECTION: String =
        "(${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?) " +
            "OR ${MediaStore.Images.Media.DATE_TAKEN} IS NULL"

    private val dateFormat = LocalDateTime.Format {
        year()
        char('.')
        monthNumber()
        char('.')
        this@Format.day(padding = Padding.ZERO)
    }

    /**
     * 전체 이미지를 가져와서 날짜별로 그룹핑
     */
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

    /**
     * 전체 이미지를 가져와서 날짜별로 그룹핑
     * 대신 당일 새벽 3시 부터 익일 새벽 2시 59분까지
     * e.g. 6일 03:00 ~ 7일 02:59
     */
    fun loadImageGroupsByYG(context: Context): List<GalleryImageGroup> {
        val uri = collectionUri ?: return emptyList()

        val resolver = context.contentResolver
        val timeZone = TimeZone.currentSystemDefault()

        val window: DayWindow = DayWindow.current(timeZone)

        val selectionArgs: Array<String> = arrayOf(
            window.startMs.toString(),
            window.endMs.toString(),
        )

        val grouped = linkedMapOf<String, MutableList<String>>()

        resolver
            .query(
                uri,
                projection,
                SELECTION,
                selectionArgs,
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

                    if (timestampMs !in window) {
                        continue
                    }

                    val dateKey: String = Instant
                        .fromEpochMilliseconds(timestampMs)
                        .minus(DayWindow.DAY_BOUNDARY_HOUR.hours)
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
