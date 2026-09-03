package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.member.UserConfigVO
import kotlinx.serialization.Serializable

@Serializable
internal data class UserConfigEntity(
    val isShowCanvasTutorial: Boolean,
)

internal fun UserConfigVO.toEntity(): UserConfigEntity = UserConfigEntity(
    isShowCanvasTutorial = isShowCanvasTutorial,
)

internal fun UserConfigEntity.toVO(): UserConfigVO = UserConfigVO(
    isShowCanvasTutorial = isShowCanvasTutorial,
)
