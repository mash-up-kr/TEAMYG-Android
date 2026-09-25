package com.teamyg.parfait.data.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

private const val IMAGE_MIME_TYPE = "image/png"
private const val SAVE_SUBDIRECTORY = "Parfait"

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

    /**
     * 새 이미지를 [MediaStore.MediaColumns.IS_PENDING] 상태로 갤러리에 등록한다. 바이트는 돌려받은
     * [Uri] 로 [openOutputStream] 을 열어 쓴다. API 29 미만은 IS_PENDING 을 쓰지 않는다.
     */
    fun insertPendingImage(displayName: String): Uri? {
        val collection = collectionUri ?: return null
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, IMAGE_MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SAVE_SUBDIRECTORY")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver?.insert(collection, values)
    }

    fun openOutputStream(uri: Uri): OutputStream? = context.contentResolver?.openOutputStream(uri)

    /** [insertPendingImage] 가 건 IS_PENDING 을 내려 갤러리에 보이게 한다 */
    fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val values = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        context.contentResolver?.update(uri, values, null, null)
    }

    /** 바이트를 다 못 썼을 때 갤러리에 빈 파일이 남지 않게 등록을 되돌린다 */
    fun deleteImage(uri: Uri) {
        context.contentResolver?.delete(uri, null, null)
    }
}
