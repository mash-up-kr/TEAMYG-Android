package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.RecentImageKindEntity
import com.teamyg.parfait.domain.model.image.RecentImageKind

internal fun RecentImageKindEntity.toVO(): RecentImageKind = when (this) {
    RecentImageKindEntity.SOURCE -> RecentImageKind.SOURCE
    RecentImageKindEntity.CUTOUT -> RecentImageKind.CUTOUT
}

internal fun RecentImageKind.toEntity(): RecentImageKindEntity = when (this) {
    RecentImageKind.SOURCE -> RecentImageKindEntity.SOURCE
    RecentImageKind.CUTOUT -> RecentImageKindEntity.CUTOUT
}
