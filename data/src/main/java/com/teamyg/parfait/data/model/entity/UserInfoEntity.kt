package com.teamyg.parfait.data.model.entity

import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.serialization.Serializable

/**
 * [MyAccountVO] 를 그대로 직렬화하지 않는 이유: 값 클래스 둘과 enum 하나를 품고 있어
 * 직렬화기가 다루지 못하고, domain 이 kotlinx.serialization 을 알게 되면 단방향 의존이
 * 깨진다(`docs/adr/0001-layered-multi-module.md`).
 */
@Serializable
internal data class UserInfoEntity(
    val memberId: Long,
    val provider: String,
    val nickname: String,
)
