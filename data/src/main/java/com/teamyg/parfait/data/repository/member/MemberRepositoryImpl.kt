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
     *
     * 로컬이 비어 있으면(`current == null`) `memberId`·`provider` 를 몰라 닉네임만으로
     * VO 를 만들 수 없으므로 [refreshMyAccount] 로 폴백해 SSoT 를 채운다. 그 결과는
     * 무시한다 — 닉네임 변경 자체는 이미 성공했으니, 폴백(재조회)이 실패한다고 이 함수의
     * 결과까지 실패로 되돌리면 안 된다.
     */
    override suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname> = remoteDataSource
        .changeGlobalNickname(nickname)
        .onSuccess { changed ->
            val current = localDataSource.myAccount.first()
            if (current != null) {
                localDataSource.save(current.copy(nickname = changed))
            } else {
                refreshMyAccount()
            }
        }.mapErrorToAppError()

    override suspend fun clearMyAccount() = localDataSource.clear()
}
