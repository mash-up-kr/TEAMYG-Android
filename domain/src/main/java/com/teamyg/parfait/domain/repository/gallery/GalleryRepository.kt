package com.teamyg.parfait.domain.repository.gallery

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import kotlinx.datetime.LocalDate

interface GalleryRepository {
    /**
     * 전체 이미지를 가져와서 날짜별로 그룹핑
     * 대신 당일 새벽 3시 부터 익일 새벽 2시 59분까지
     * e.g. 6일 03:00 ~ 7일 02:59
     *
     * 지금 화면은 쓰지 않는다 — 내부 배포 반응을 본 뒤 제한 해제가 확정되면 지운다.
     */
    suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>>

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
