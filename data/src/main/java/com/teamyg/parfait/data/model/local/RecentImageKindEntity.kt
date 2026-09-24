package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.image.RecentImageKind
import kotlinx.serialization.Serializable

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
