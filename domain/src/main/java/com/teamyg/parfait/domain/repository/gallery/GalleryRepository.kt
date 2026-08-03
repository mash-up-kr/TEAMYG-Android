package com.teamyg.parfait.domain.repository.gallery

import kotlinx.datetime.LocalDate

interface GalleryRepository {
    /**
     * 전체 이미지를 가져와서 날짜별로 그룹핑
     */
    suspend fun loadAllGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>>

    /**
     * 전체 이미지를 가져와서 날짜별로 그룹핑
     * 대신 당일 새벽 3시 부터 익일 새벽 2시 59분까지
     * e.g. 6일 03:00 ~ 7일 02:59
     */
    suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>>
}
