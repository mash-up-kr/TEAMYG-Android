package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.image.RecentImageKind
import kotlinx.serialization.Serializable

/** 최근 이미지의 저장 형태. 절대경로는 저장하지 않는다 — uri 로부터 매번 되짚는다 */
@Serializable
data class RecentImageEntity(
    val uri: String,
    /** 기본값이 있어야 모르는 종류값 하나가 목록 전체를 못 날린다(`coerceInputValues`) */
    val kind: RecentImageKindEntity = RecentImageKindEntity.SOURCE,
)

@Serializable
enum class RecentImageKindEntity {
    SOURCE,
    CUTOUT,
}

fun RecentImageKindEntity.toVO(): RecentImageKind = when (this) {
    RecentImageKindEntity.SOURCE -> RecentImageKind.SOURCE
    RecentImageKindEntity.CUTOUT -> RecentImageKind.CUTOUT
}

fun RecentImageKind.toEntity(): RecentImageKindEntity = when (this) {
    RecentImageKind.SOURCE -> RecentImageKindEntity.SOURCE
    RecentImageKind.CUTOUT -> RecentImageKindEntity.CUTOUT
}
