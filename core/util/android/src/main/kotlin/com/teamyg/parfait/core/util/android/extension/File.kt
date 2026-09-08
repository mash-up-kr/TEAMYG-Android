package com.teamyg.parfait.core.util.android.extension

import androidx.exifinterface.media.ExifInterface
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger
import java.io.File

/**
 * 로컬 파일의 EXIF orientation 태그를 시계 방향 회전 각도로 읽는다.
 *
 * [ContentResolver.readExifDegrees] 와 같은 규약(못 읽으면 0, 미러링도 0)이다 —
 * 소스가 `content://` 가 아니라 이미 디스크에 있는 [File]인 호출부(업로드 전처리 등)를 위한
 * 것이다. 태그가 깨진 것과 파일을 못 여는 것은 다른 사건이라 호출부의 디코드를 막지 않는다.
 */
fun File.readExifDegrees(): Int = try {
    exifOrientationToDegrees(
        ExifInterface(this).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        ),
    )
} catch (throwable: Exception) {
    coreUtilAndroidLogger.w(throwable) { "EXIF 를 읽지 못해 회전 보정을 건너뛴다 - file: $name" }
    0
}
