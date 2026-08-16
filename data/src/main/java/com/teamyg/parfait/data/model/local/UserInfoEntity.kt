package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.serialization.Serializable

/**
 * 계정 정보의 저장 형태.
 *
 * [MyAccountVO] 를 그대로 직렬화하지 않는 이유: 값 클래스 둘과 enum 하나를 품고 있어
 * 직렬화기가 다루지 못하고, domain 이 kotlinx.serialization 을 알게 되면 ADR-0001 의
 * 단방향 의존이 깨진다.
 */
@Serializable
internal data class UserInfoEntity(
    val memberId: Long,
    val provider: String,
    val nickname: String,
)

internal fun MyAccountVO.toEntity(): UserInfoEntity = UserInfoEntity(
    memberId = memberId.value,
    provider = provider.name,
    nickname = nickname.value,
)

/** 저장 당시와 앱의 [LoginProvider] 목록이 다를 수 있어 알 수 없는 값은 UNKNOWN 으로 떨어뜨린다 */
internal fun UserInfoEntity.toVO(): MyAccountVO = MyAccountVO(
    memberId = MemberId(memberId),
    provider = LoginProvider.entries.firstOrNull { it.name == provider } ?: LoginProvider.UNKNOWN,
    nickname = GlobalNickname(nickname),
)
