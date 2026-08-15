package com.teamyg.parfait.domain.repository.member

import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.coroutines.flow.Flow

/**
 * `GET /api/v1/users/me` 계정 정보의 local SSoT 를 조율한다. 실패는 모두
 * [com.teamyg.parfait.domain.model.error.AppError] 로 온다.
 */
interface MemberRepository {
    /** 로컬에 저장된 계정 정보. 없거나 복호화에 실패하면 `null` */
    val myAccount: Flow<MyAccountVO?>

    /** 원격에서 계정 정보를 다시 읽어 로컬을 갱신한다 */
    suspend fun refreshMyAccount(): Result<MyAccountVO>

    suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>

    suspend fun clearMyAccount()
}
