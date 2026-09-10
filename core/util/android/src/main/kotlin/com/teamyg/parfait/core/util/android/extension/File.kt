package com.teamyg.parfait.core.util.android.extension

import androidx.exifinterface.media.ExifInterface
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger
import java.io.File

/**
 * EXIF orientation 태그를 시계 방향 회전 각도로 읽는다. 같은 파일의 `ContentResolver` 판과
 * 규약이 같다 — 못 읽으면 0, 미러링도 0.
 *
 * 못 읽는 것을 실패로 올리지 않는 이유: 태그가 깨진 것과 파일을 못 여는 것은 다른 사건이라
 * 호출부의 디코드까지 막을 이유가 없다.
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
