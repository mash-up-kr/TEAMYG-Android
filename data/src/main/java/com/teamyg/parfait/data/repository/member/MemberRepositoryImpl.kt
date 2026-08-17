package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
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
        .fold(
            onSuccess = { account -> saveLocally(account).map { account } },
            onFailure = { Result.failure(it) },
        ).mapErrorToAppError()

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
        .fold(
            onSuccess = { changed -> applyChangedNicknameLocally(changed) },
            onFailure = { Result.failure(it) },
        ).mapErrorToAppError()

    /**
     * `localDataSource.myAccount.first()` 도 `DataStore.data` 를 읽는 suspend 호출이라
     * [saveLocally] 와 똑같이 IOException 을 던질 수 있다. 이 함수 전체를
     * [runSuspendCatching] 으로 감싸야 읽기 실패도 [saveLocally] 의 쓰기 실패와
     * 마찬가지로 `Result` 로 되돌아온다 — 어느 한쪽만 감싸면 감싸지 않은 쪽이 Repository
     * 경계를 뚫고 나가는 예외가 된다(ADR-0020).
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
     * `DataStore.edit` 는 IOException 을 던질 수 있는데, 이 값이 원격 응답의
     * `Result.onSuccess` 체인 안에서 무방비로 던져지면 [mapErrorToAppError] 를 거치지
     * 않고 Repository 경계를 그대로 뚫고 나간다 — 소비자(로그인·가입·닉네임 변경)가
     * `Result` 만 보고 있다가 미포착 예외로 크래시한다. 여기서 [runSuspendCatching] 으로
     * 잡아 선언된 실패 채널로 되돌린다. 취소는 [runSuspendCatching] 이 걸러 재던진다.
     */
    private suspend fun saveLocally(account: MyAccountVO): Result<Unit> =
        runSuspendCatching { localDataSource.save(account) }
}
