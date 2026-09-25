package com.teamyg.parfait.data.model.entity

import kotlinx.serialization.Serializable

/** 최근 이미지의 저장 형태. 절대경로는 저장하지 않는다 — uri 로부터 매번 되짚는다 */
@Serializable
data class RecentImageEntity(
    val uri: String,
    /** 기본값이 있어야 모르는 종류값 하나가 목록 전체를 못 날린다(`coerceInputValues`) */
    val kind: RecentImageKindEntity = RecentImageKindEntity.SOURCE,
)
