package com.teamyg.parfait.feature.gallery.api

import kotlinx.serialization.Serializable

/** 갤러리 피커의 "최근" 줄에 실을 종류 */
@Serializable
enum class RecentImagePick {
    SOURCE,
    CUTOUT,
}
