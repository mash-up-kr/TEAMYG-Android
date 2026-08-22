package com.teamyg.parfait.data.repository.gallery

import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
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

    /**
     * IS_PENDING 으로 등록해 두고 바이트를 다 쓴 뒤에야 내린다 — 쓰다 만 파일이 갤러리에
     * 잠깐이라도 온전한 것처럼 보이지 않게 하려는 것이다(API 29+). 어느 단계에서든 실패하면
     * 등록 자체를 지워 빈 항목이 남지 않게 한다.
     */
    override suspend fun saveImageToGallery(
        bitmap: BitmapWrapper,
        displayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runSuspendCatching {
            val rawBitmap: Bitmap = (bitmap as? AndroidBitmap)?.getRawData()
                ?: error("갤러리에 저장할 비트맵을 읽지 못했다")
            val uri = galleryMediaProvider.insertPendingImage(displayName)
                ?: error("갤러리에 이미지를 등록하지 못했다")

            try {
                galleryMediaProvider.openOutputStream(uri)?.use { output ->
                    rawBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } ?: error("갤러리 이미지의 출력 스트림을 열지 못했다")

                galleryMediaProvider.finalizePendingImage(uri)
            } catch (throwable: Throwable) {
                galleryMediaProvider.deleteImage(uri)
                throw throwable
            }
        }
    }
}
