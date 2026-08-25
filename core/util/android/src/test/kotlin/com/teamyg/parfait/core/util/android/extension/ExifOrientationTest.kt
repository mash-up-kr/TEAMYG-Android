package com.teamyg.parfait.core.util.android.extension

import androidx.exifinterface.media.ExifInterface
import kotlin.test.Test
import kotlin.test.assertEquals

class ExifOrientationTest {
    @Test
    fun exifOrientationToDegrees_rotateTags_mapToTheirAngles() {
        // Given 회전만 나타내는 태그 셋
        // Then 각각의 각도가 된다
        assertEquals(90, exifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, exifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, exifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test
    fun exifOrientationToDegrees_normalOrUndefined_isZero() {
        // Given 돌릴 필요가 없거나 태그가 없는 경우
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_UNDEFINED))
    }

    @Test
    fun exifOrientationToDegrees_mirrorTags_areZero() {
        // Given 좌우 반전이 섞인 태그 넷
        // Then 0 이다. 반전을 적용하면 뒤집힌 누끼가 나오고 정확도에는 기여하지 않는다
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_FLIP_VERTICAL))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_TRANSVERSE))
    }

    @Test
    fun exifOrientationToDegrees_unknownValue_isZero() {
        // 깨진 파일이 범위 밖 값을 주는 일이 있다
        assertEquals(0, exifOrientationToDegrees(-1))
        assertEquals(0, exifOrientationToDegrees(99))
    }
}
