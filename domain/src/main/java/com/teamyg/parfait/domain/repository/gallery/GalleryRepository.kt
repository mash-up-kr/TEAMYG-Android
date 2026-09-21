package com.teamyg.parfait.domain.repository.gallery

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import kotlinx.datetime.LocalDate

interface GalleryRepository {
    /**
     * 전체 이미지를 가져와서 날짜별로 그룹핑
     * 대신 당일 새벽 3시 부터 익일 새벽 2시 59분까지
     * e.g. 6일 03:00 ~ 7일 02:59
     */
    suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>>

    /** [bitmap] 을 기기 갤러리에 새 이미지로 저장한다. */
    suspend fun saveImageToGallery(
        bitmap: BitmapWrapper,
        displayName: String,
    ): Result<Unit>

    /**
     * [videoFilePath] 의 mp4 를 기기 갤러리에 새 영상으로 저장한다.
     *
     * 비트맵이 아니라 경로를 받는 이유: 영상은 인코더가 이미 파일로 완성해 둔다. 바이트를 메모리로
     * 올려 나르면 수십 MB 를 한 번 더 들고 있게 된다.
     */
    suspend fun saveVideoToGallery(
        videoFilePath: String,
        displayName: String,
    ): Result<Unit>
}
