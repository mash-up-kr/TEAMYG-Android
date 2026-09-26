package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.member.remote.MemberRemoteDataSource
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 원격 계정 정보와 로컬 SSoT 를 조율한다 */
class MemberRepositoryImpl @Inject constructor(
    private val remoteDataSource: MemberRemoteDataSource,
    private val localDataSource: UserInfoLocalDataSource,
) : MemberRepository {
    override val myAccount: Flow<MyAccountVO?> = localDataSource.myAccount

    override suspend fun refreshMyAccount(): Result<MyAccountVO> = remoteDataSource
        .getMyAccount()
        .fold(
            onSuccess = { account -> saveLocally(account).map { account } },
            onFailure = { Result.failure(it) },
        ).mapErrorToAppError()

    /**
     * 성공 응답을 받은 뒤에 로컬을 갱신한다(낙관적 갱신 안 함) — 실패했는데 다른 화면에
     * 새 닉네임이 보이는 것이 되돌리는 것보다 나쁘다.
     *
     * 로컬이 비어 있으면 닉네임만으로 VO 를 만들 수 없어 [refreshMyAccount] 로 폴백한다.
     * 닉네임 변경은 이미 성공했으므로 폴백 결과는 무시한다.
     */
    override suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname> = remoteDataSource
        .changeGlobalNickname(nickname)
        .fold(
            onSuccess = { changed -> applyChangedNicknameLocally(changed) },
            onFailure = { Result.failure(it) },
        ).mapErrorToAppError()

    /**
     * 로컬 읽기도 [saveLocally] 처럼 IOException 을 던질 수 있어 함수 전체를 [runSuspendCatching]
     * 으로 감싼다. 한쪽만 감싸면 다른 쪽이 Repository 경계를 뚫고 나간다(ADR-0020).
     */
    private suspend fun applyChangedNicknameLocally(changed: GlobalNickname): Result<GlobalNickname> =
        runSuspendCatching {
            val current = localDataSource.myAccount.first()
            if (current != null) {
                saveLocally(current.copy(nickname = changed)).getOrThrow()
            } else {
                refreshMyAccount()
            }
            changed
        }

    override suspend fun clearMyAccount() = localDataSource.clear()

    override suspend fun withdraw(): Result<Unit> = remoteDataSource.withdraw().mapErrorToAppError()

    /**
     * `DataStore.edit` 의 IOException 이 `Result.onSuccess` 체인 안에서 던져지면 Repository
     * 경계를 뚫고 나가 `Result` 만 보는 소비자가 크래시한다. [runSuspendCatching] 으로 잡아
     * `Result` 로 되돌린다.
     */
    private suspend fun saveLocally(account: MyAccountVO): Result<Unit> =
        runSuspendCatching { localDataSource.save(account) }
}
