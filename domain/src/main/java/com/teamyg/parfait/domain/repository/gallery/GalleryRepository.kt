package com.teamyg.parfait.domain.repository.gallery

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import kotlinx.datetime.LocalDate

interface GalleryRepository {
    /**
     * 기기의 전체 이미지를 기기 시간대의 달력 날짜별로 그룹핑한다.
     */
    suspend fun loadGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>>

    /** [bitmap] 을 기기 갤러리에 새 이미지로 저장한다. */
    suspend fun saveImageToGallery(
        bitmap: BitmapWrapper,
        displayName: String,
    ): Result<Unit>
}
