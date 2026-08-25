package com.teamyg.parfait.core.util.android.extension

import androidx.exifinterface.media.ExifInterface

/**
 * EXIF orientation 태그를 시계 방향 회전 각도로 바꾼다.
 *
 * 미러링(`FLIP_*`·`TRANSPOSE`·`TRANSVERSE`)이 0 인 이유는
 * `parfait/specs/2026-08-23-segmentation-preprocessing.md` 「범위 제외」에 있다.
 */
internal fun exifOrientationToDegrees(orientation: Int): Int = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}
