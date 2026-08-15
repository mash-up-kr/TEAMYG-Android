package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.member.remote.MemberRemoteDataSource
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 원격 계정 정보와 로컬 SSoT 를 조율한다 — [mapErrorToAppError] 로 `ApiException` 을
 * `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 */
class MemberRepositoryImpl @Inject constructor(
    private val remoteDataSource: MemberRemoteDataSource,
    private val localDataSource: UserInfoLocalDataSource,
) : MemberRepository {
    override val myAccount: Flow<MyAccountVO?> = localDataSource.myAccount

    override suspend fun refreshMyAccount(): Result<MyAccountVO> = remoteDataSource
        .getMyAccount()
        .onSuccess { account -> localDataSource.save(account) }
        .mapErrorToAppError()

    /**
     * 성공 응답을 받은 뒤에 로컬을 갱신한다(낙관적 갱신 안 함) — 실패했는데 다른 화면에
     * 새 닉네임이 보이는 것이 되돌리는 것보다 나쁘다.
     */
    override suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname> = remoteDataSource
        .changeGlobalNickname(nickname)
        .onSuccess { changed ->
            localDataSource.myAccount.first()?.let { current ->
                localDataSource.save(current.copy(nickname = changed))
            }
        }.mapErrorToAppError()

    override suspend fun clearMyAccount() = localDataSource.clear()
}
