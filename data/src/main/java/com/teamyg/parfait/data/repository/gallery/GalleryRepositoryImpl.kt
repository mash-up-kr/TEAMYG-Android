package com.teamyg.parfait.data.repository.gallery

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.teamyg.parfait.domain.model.DayWindow
import com.teamyg.parfait.data.utils.GalleryMediaProvider
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class GalleryRepositoryImpl
@Inject
constructor(
    private val galleryMediaProvider: GalleryMediaProvider,
) : GalleryRepository {
    init {
        repositoryLogger.i { "GalleryRepositoryImpl::init" }
    }

    override suspend fun loadAllGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>> =
        withContext(Dispatchers.IO) {
            val uri: Uri = galleryMediaProvider
                .collectionUri
                ?: return@withContext LinkedHashMap<LocalDate, MutableList<String>>()
            val timeZone: TimeZone = TimeZone.currentSystemDefault()

            val grouped = linkedMapOf<LocalDate, MutableList<String>>()

            galleryMediaProvider
                .query(
                    uri = uri,
                    projection = galleryMediaProvider.projection,
                    selection = null,
                    selectionArgs = null,
                    sortOrder = galleryMediaProvider.sortOrder,
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id: Long = cursor.getLong(idColumn)

                        val timestampMs: Long = galleryMediaProvider.resolveTimestampMs(
                            cursor = cursor,
                            takenColumn = takenColumn,
                            addedColumn = addedColumn,
                        )

                        val dateKey: LocalDate = Instant
                            .fromEpochMilliseconds(timestampMs)
                            .toLocalDateTime(timeZone)
                            .date

                        val imageUri: String = ContentUris
                            .withAppendedId(uri, id)
                            .toString()

                        grouped
                            .getOrPut(dateKey) { mutableListOf() }
                            .add(imageUri)
                    }
                }

            return@withContext grouped
        }

    override suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>> =
        withContext(Dispatchers.IO) {
            val uri: Uri = galleryMediaProvider
                .collectionUri
                ?: return@withContext LinkedHashMap<LocalDate, MutableList<String>>()
            val timeZone: TimeZone = TimeZone.currentSystemDefault()
            val window: DayWindow = DayWindow.current(timeZone)
            val selectionArgs: Array<String> = arrayOf(
                window.startMs.toString(),
                window.endMs.toString(),
            )

            val grouped = linkedMapOf<LocalDate, MutableList<String>>()

            galleryMediaProvider
                .query(
                    uri = uri,
                    projection = galleryMediaProvider.projection,
                    selection = galleryMediaProvider.selection,
                    selectionArgs = selectionArgs,
                    sortOrder = galleryMediaProvider.sortOrder,
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id: Long = cursor.getLong(idColumn)

                        val timestampMs: Long = galleryMediaProvider.resolveTimestampMs(
                            cursor = cursor,
                            takenColumn = takenColumn,
                            addedColumn = addedColumn,
                        )

                        if (timestampMs !in window) {
                            continue
                        }

                        val dateKey: LocalDate = Instant
                            .fromEpochMilliseconds(timestampMs)
                            .minus(DayWindow.DAY_BOUNDARY_HOUR.hours)
                            .toLocalDateTime(timeZone)
                            .date

                        val imageUri: String = ContentUris
                            .withAppendedId(uri, id)
                            .toString()

                        grouped
                            .getOrPut(dateKey) { mutableListOf() }
                            .add(imageUri)
                    }
                }

            return@withContext grouped
        }
}
